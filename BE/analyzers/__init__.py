from .gemini_analyzer import GeminiAnalyzer
from .claude_analyzer import ClaudeAnalyzer
from .ollama_analyzer import OllamaAnalyzer
from .base import BaseAnalyzer
from config import settings


def create_analyzer() -> BaseAnalyzer:
    """설정된 분석 엔진에 맞는 Analyzer 인스턴스 생성"""
    engine = settings.analysis_engine
    if engine == "gemini":
        return GeminiAnalyzer()
    elif engine == "claude":
        return ClaudeAnalyzer()
    elif engine == "ollama":
        return OllamaAnalyzer()
    raise ValueError(f"Unknown analysis engine: {engine}")


__all__ = [
    "GeminiAnalyzer",
    "ClaudeAnalyzer",
    "OllamaAnalyzer",
    "create_analyzer",
]
