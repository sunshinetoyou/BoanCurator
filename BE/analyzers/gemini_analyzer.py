import json
import time
import logging
from google import genai
from google.genai import types

from .base import BaseAnalyzer
from config import settings
from db.models import Article, AnalysisData, Category, Theme, Level, SECURITY_DOMAINS, DOMAIN_WEIGHTS

logger = logging.getLogger(__name__)


class GeminiAnalyzer(BaseAnalyzer):
    def __init__(self, prompt_version: str = "v3"):
        api_keys = [k.strip() for k in settings.gemini_api_keys.split(",") if k.strip()]
        self.clients = [genai.Client(api_key=k) for k in api_keys]
        self._key_index = 0
        self.model_name = settings.gemini_model
        self.prompt_version = prompt_version

    @property
    def client(self) -> genai.Client:
        return self.clients[self._key_index]

    def _rotate_key(self, base_delay: float = 3.0):
        """다음 API 키로 전환 (키 간 간격을 두어 동시 호출 패턴 방지)"""
        prev = self._key_index
        self._key_index = (self._key_index + 1) % len(self.clients)
        # 키 순번에 비례한 지수적 텀: 1번째 키→3초, 2번째→6초, 3번째→12초...
        delay = base_delay * (2 ** (self._key_index % len(self.clients)))
        logger.info(f"API 키 전환: {prev} → {self._key_index} (대기 {delay:.0f}초)")
        time.sleep(delay)

    def analyze(self, article: Article) -> AnalysisData | None:
        categories = [c.value for c in Category]
        themes = [t.value for t in Theme]

        system_prompt = f"""당신은 IT/보안 전문 뉴스 분석가입니다. 아래 지침에 따라 뉴스 본문을 분석하여 오직 유효한 JSON만 출력하세요.

        [지침]
        1. Category: 반드시 다음 중 하나만 선택하세요: {categories}
        2. Themes: 다음 리스트 중 관련 있는 항목을 모두 선택하세요(최대 3개): {themes}
        3. Summary: 뉴스 본문을 한국어 1문장으로 핵심만 요약하세요.
        4. Technical Keywords: 핵심 기술 용어를 추출하세요(최대 3개).
        5. Domain Scores: 기사의 기술적 깊이를 아래 6개 도메인 축에 대해 0~5 정수로 평가하세요.
           - network_infra: 네트워크/인프라 보안 (방화벽, IDS/IPS, 네트워크 포렌식 등)
           - malware_vuln: 악성코드/취약점 분석 (CVE, 익스플로잇, 리버스엔지니어링 등)
           - cloud_devsecops: 클라우드/DevSecOps (AWS/Azure/GCP 보안, CI/CD, 컨테이너 보안 등)
           - crypto_auth: 암호학/인증 (TLS, PKI, OAuth, 암호 프로토콜 등)
           - policy_compliance: 정책/컴플라이언스 (개인정보보호법, ISMS, 규제, 거버넌스 등)
           - general_it: 일반 IT/개발 (프로그래밍, OS, DB, 일반 IT 뉴스 등)
           0 = 해당 도메인과 무관, 5 = 해당 도메인의 최고 수준 전문 지식 필요

        [출력 JSON 양식]
        {{
            "category": "선택한 카테고리",
            "themes": ["테마1", "테마2"],
            "summary": "한국어 요약",
            "technical_keywords": ["CVE-2024-...", "Buffer Overflow", "ROP chain"],
            "domain_scores": {{
                "network_infra": 0,
                "malware_vuln": 0,
                "cloud_devsecops": 0,
                "crypto_auth": 0,
                "policy_compliance": 0,
                "general_it": 0
            }}
        }}"""

        user_content = f"제목: {article.title}\n본문: {article.content[:8000]}"
        result = self._call_gemini_json(system_prompt, user_content)
        if not result:
            return None

        # domain_scores 검증 및 정규화
        domain_scores = result.get("domain_scores", {})
        domain_scores = {
            d: max(0, min(5, int(domain_scores.get(d, 0))))
            for d in SECURITY_DOMAINS
        }

        # 절대 난이도 도출 (보안 축 가중 평균)
        weighted_sum = 0.0
        weight_total = 0.0
        for d in SECURITY_DOMAINS:
            score = domain_scores.get(d, 0)
            if score > 0:
                w = DOMAIN_WEIGHTS.get(d, 1.0)
                weighted_sum += score * w
                weight_total += w
        weighted_avg = weighted_sum / weight_total if weight_total > 0 else 0

        if weighted_avg >= 3.5:
            level = Level.High.value
        elif weighted_avg >= 2.0:
            level = Level.Medium.value
        else:
            level = Level.Low.value

        return AnalysisData(
            category=result.get("category", Category.TECH.value),
            themes=result.get("themes", []),
            summary=result.get("summary", ""),
            level=level,
            domain_scores=domain_scores,
            prompt_version=self.prompt_version,
            model=self.model_name,
        )

    def _call_gemini_json(self, system_prompt: str, user_content: str, max_retries: int = 3) -> dict | None:
        """Gemini API 호출 (JSON 모드) + 키 로테이션 + 재시도"""
        keys_tried = 0
        for attempt in range(max_retries * len(self.clients)):
            try:
                response = self.client.models.generate_content(
                    model=self.model_name,
                    contents=user_content,
                    config=types.GenerateContentConfig(
                        system_instruction=system_prompt,
                        response_mime_type="application/json",
                    ),
                )
                return json.loads(response.text)
            except Exception as e:
                err_str = str(e)
                if "429" in err_str or "RESOURCE_EXHAUSTED" in err_str:
                    self._rotate_key()
                    keys_tried += 1
                    if keys_tried >= len(self.clients):
                        logger.warning(f"모든 키 소진, backoff 대기 (시도 {attempt + 1})")
                        keys_tried = 0
                        time.sleep(2 ** min(attempt, 4))
                else:
                    logger.warning(f"Gemini JSON 호출 실패 (시도 {attempt + 1}): {e}")
                    time.sleep(2 ** min(attempt, 4))
        return None
