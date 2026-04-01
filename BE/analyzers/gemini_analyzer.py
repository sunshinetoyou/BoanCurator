import json
import time
import logging
from google import genai
from google.genai import types

from .base import BaseAnalyzer
from config import settings
from db.models import Article, AnalysisData, Category, Theme, Level

logger = logging.getLogger(__name__)


class GeminiAnalyzer(BaseAnalyzer):
    def __init__(self, prompt_version: str = "v2"):
        self.client = genai.Client(api_key=settings.gemini_api_key)
        self.model_name = settings.gemini_model
        self.prompt_version = prompt_version

    def analyze(self, article: Article) -> AnalysisData | None:
        # 1. [Agent 1] Analyst: 분류 및 요약
        categories = [c.value for c in Category]
        themes = [t.value for t in Theme]

        analyst_res = self._run_analyst(article, categories, themes)
        if not analyst_res:
            return None

        # 2. [Agent 2] Judge: 기술적 깊이(Level) 판별
        levels = [l.value for l in Level]
        final_level = self._run_judge(analyst_res, levels)

        # 3. 최종 AnalysisData 규격으로 병합
        return AnalysisData(
            category=analyst_res.get("category", Category.TECH.value),
            themes=analyst_res.get("themes", []),
            summary=analyst_res.get("summary", ""),
            level=final_level,
            prompt_version=self.prompt_version,
            model=self.model_name,
        )

    def _run_analyst(self, article: Article, categories: list, themes: list) -> dict | None:
        """Agent 1: 추출 및 분류 (DB 스키마 준수)"""
        system_prompt = f"""당신은 IT 전문 뉴스 분석가입니다. 아래 지침에 따라 뉴스 본문을 분석하여 오직 유효한 JSON만 출력하세요.

        [지침]
        1. Category: 반드시 다음 중 하나만 선택하세요: {categories}
        2. Themes: 다음 리스트 중 관련 있는 항목을 모두 선택하세요(최대 3개): {themes}
        3. Summary: 뉴스 본문을 한국어 1문장으로 핵심만 요약하세요.
        4. Technical Keywords: 핵심 기술 용어를 추출하세요(최대 3개)

        [출력 JSON 양식]
        {{
            "category": "선택한 카테고리",
            "themes": ["테마1", "테마2"],
            "summary": "한국어 요약",
            "technical_keywords": ["CVE-2024-...", "Buffer Overflow", "ROP chain"]
        }}"""

        user_content = f"제목: {article.title}\n본문: {article.content[:8000]}"

        return self._call_gemini_json(system_prompt, user_content)

    def _run_judge(self, analyst_data: dict, levels: list) -> str:
        """Agent 2: 난이도 판단 (기술적 깊이 기준)"""
        summary = analyst_data.get("summary", "")
        themes = analyst_data.get("themes", [])

        system_prompt = f"""당신은 보안 뉴스 에디터입니다. 분석 데이터의 기술적 깊이를 평가하여 중요도를 결정하세요.
        출력은 오직 다음 값 중 하나만 허용됩니다: {levels}

        [평가 기준]
        - High: 취약점 분석, Exploit 코드, 복잡한 아키텍처, 0-day, 심층 연구 보고서.
        - Medium: 가이드라인, 기술적 설정 방법, 일반적인 보안 사고 분석, 클라우드 인프라 운영.
        - Low: 단순 단신, 정책 변경, 일반 IT 뉴스, 인사 이동, 단순 이벤트 소식.

        [입력 데이터]
        - Themes: {themes}
        - Summary: {summary}"""

        response = self._call_gemini(system_prompt, "위 데이터를 기반으로 Level을 판단해주세요.")
        if not response:
            return Level.Low.value

        if "High" in response:
            return Level.High.value
        if "Medium" in response:
            return Level.Medium.value
        return Level.Low.value

    def _call_gemini_json(self, system_prompt: str, user_content: str, max_retries: int = 3) -> dict | None:
        """Gemini API 호출 (JSON 모드) + 재시도"""
        for attempt in range(max_retries):
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
                logger.warning(f"Gemini JSON 호출 실패 (시도 {attempt + 1}/{max_retries}): {e}")
                if attempt < max_retries - 1:
                    time.sleep(2 ** (attempt + 1))
        return None

    def _call_gemini(self, system_prompt: str, user_content: str, max_retries: int = 3) -> str | None:
        """Gemini API 호출 (텍스트 모드) + 재시도"""
        for attempt in range(max_retries):
            try:
                response = self.client.models.generate_content(
                    model=self.model_name,
                    contents=user_content,
                    config=types.GenerateContentConfig(
                        system_instruction=system_prompt,
                    ),
                )
                return response.text.strip()
            except Exception as e:
                logger.warning(f"Gemini 호출 실패 (시도 {attempt + 1}/{max_retries}): {e}")
                if attempt < max_retries - 1:
                    time.sleep(2 ** (attempt + 1))
        return None
