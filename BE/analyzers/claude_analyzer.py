import json
import shutil
import subprocess
import time
import logging

from .base import BaseAnalyzer
from config import settings
from db.models import Article, AnalysisData, Category, Theme, Level, SECURITY_DOMAINS

logger = logging.getLogger(__name__)


def _query_claude(prompt: str, model: str = "sonnet") -> str:
    """claude CLI(--print)로 응답을 받아옵니다."""
    if not shutil.which("claude"):
        raise RuntimeError("claude CLI가 설치되어 있지 않습니다.")

    result = subprocess.run(
        ["claude", "--print", "--model", model, prompt],
        capture_output=True,
        text=True,
        timeout=120,
    )
    if result.returncode != 0:
        raise RuntimeError(f"claude CLI 호출 실패: {result.stderr}")
    return result.stdout.strip()


class ClaudeAnalyzer(BaseAnalyzer):
    def __init__(self, prompt_version: str = "v3"):
        self.model_name = "sonnet"
        self.prompt_version = prompt_version

    def analyze(self, article: Article) -> AnalysisData | None:
        categories = [c.value for c in Category]
        themes = [t.value for t in Theme]
        levels = [l.value for l in Level]

        prompt = f"""당신은 IT/보안 전문 뉴스 분석가입니다. 아래 지침에 따라 뉴스 본문을 분석하여 오직 유효한 JSON만 출력하세요.

[지침]
1. Category: 반드시 다음 중 하나만 선택하세요: {categories}
2. Themes: 다음 리스트 중 관련 있는 항목을 모두 선택하세요(최대 3개): {themes}
3. Summary: 뉴스 본문을 한국어 1문장으로 핵심만 요약하세요.
4. Technical Keywords: 핵심 기술 용어를 추출하세요(최대 3개).
5. Level: 기사의 기술적 깊이를 독자 수준 기준으로 판단하세요. 반드시 다음 중 하나: {levels}
   - Low: 보안/IT 비전문가도 이해 가능. 개념 소개, 뉴스 단신, 정책 변경, 이벤트 소식.
   - Medium: 실무 경험이 있는 중급자 대상. 설정 가이드, 사고 분석, 기술 비교, 모범 사례.
   - High: 깊은 전문 지식 필요. 취약점 PoC, exploit 코드, 리버싱 분석, 심층 연구 보고서.
6. Domain Scores: 기사가 다루는 분야를 아래 6개 도메인 축에 대해 0~5 정수로 평가하세요.
   - network_infra, malware_vuln, cloud_devsecops, crypto_auth, policy_compliance, general_it
   0 = 해당 도메인과 무관, 5 = 해당 도메인과 매우 깊이 관련됨

오직 JSON만 출력하세요. 다른 텍스트는 포함하지 마세요.

---
제목: {article.title}
본문: {article.content[:8000]}"""

        result = self._call_claude_json(prompt)
        if not result:
            return None

        domain_scores = result.get("domain_scores", {})
        domain_scores = {
            d: max(0, min(5, int(domain_scores.get(d, 0))))
            for d in SECURITY_DOMAINS
        }

        raw_level = result.get("level", "Medium")
        if raw_level not in [l.value for l in Level]:
            raw_level = Level.Medium.value

        return AnalysisData(
            category=result.get("category", Category.TECH.value),
            themes=result.get("themes", []),
            summary=result.get("summary", ""),
            level=raw_level,
            domain_scores=domain_scores,
            prompt_version=self.prompt_version,
            model=self.model_name,
        )

    def _call_claude_json(self, prompt: str, max_retries: int = 3) -> dict | None:
        for attempt in range(max_retries):
            try:
                text = _query_claude(prompt, model=self.model_name)
                # JSON 블록이 ```json ... ``` 으로 감싸져 있을 수 있음
                if text.startswith("```"):
                    text = text.split("\n", 1)[1].rsplit("```", 1)[0].strip()
                return json.loads(text)
            except Exception as e:
                logger.warning(f"Claude CLI 호출 실패 (시도 {attempt + 1}/{max_retries}): {e}")
                if attempt < max_retries - 1:
                    time.sleep(2 ** (attempt + 1))
        return None
