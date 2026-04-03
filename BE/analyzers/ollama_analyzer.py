import json
import time
import logging
import requests

from .base import BaseAnalyzer
from config import settings
from db.models import Article, AnalysisData, Category, Theme, Level, SECURITY_DOMAINS

logger = logging.getLogger(__name__)


class OllamaAnalyzer(BaseAnalyzer):
    def __init__(self, prompt_version: str = "v3"):
        self.base_url = settings.ollama_url
        self.model_name = settings.ollama_model
        self.prompt_version = prompt_version

    def analyze(self, article: Article) -> AnalysisData | None:
        categories = [c.value for c in Category]
        themes = [t.value for t in Theme]
        levels = [l.value for l in Level]

        prompt = f"""당신은 IT/보안 전문 뉴스 분석가입니다. 아래 뉴스를 분석하여 오직 유효한 JSON만 출력하세요.

[지침]
1. Category: {categories} 중 하나
2. Themes: {themes} 중 최대 3개
3. Summary: 한국어 1문장 요약
4. Technical Keywords: 핵심 기술 용어 최대 3개
5. Level: {levels} 중 하나 (Low=비전문가, Medium=중급 실무자, High=전문가)
6. Domain Scores: network_infra, malware_vuln, cloud_devsecops, crypto_auth, policy_compliance, general_it 각 0~5

[뉴스]
제목: {article.title}
본문: {article.content[:8000]}

오직 JSON만 출력:
{{"category":"","themes":[],"summary":"","technical_keywords":[],"level":"","domain_scores":{{"network_infra":0,"malware_vuln":0,"cloud_devsecops":0,"crypto_auth":0,"policy_compliance":0,"general_it":0}}}}"""

        result = self._call_ollama_json(prompt)
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

    def _call_ollama_json(self, prompt: str, max_retries: int = 3) -> dict | None:
        for attempt in range(max_retries):
            try:
                resp = requests.post(
                    f"{self.base_url}/api/generate",
                    json={
                        "model": self.model_name,
                        "prompt": prompt,
                        "format": "json",
                        "stream": False,
                    },
                    timeout=120,
                )
                resp.raise_for_status()
                text = resp.json().get("response", "")
                return json.loads(text)
            except Exception as e:
                logger.warning(f"Ollama 호출 실패 (시도 {attempt + 1}/{max_retries}): {e}")
                if attempt < max_retries - 1:
                    time.sleep(2 ** (attempt + 1))
        return None
