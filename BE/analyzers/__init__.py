from .gemini_analyzer import GeminiAnalyzer
from .base import BaseAnalyzer


def create_analyzer() -> BaseAnalyzer:
    return GeminiAnalyzer()


__all__ = ["GeminiAnalyzer", "BaseAnalyzer", "create_analyzer"]
