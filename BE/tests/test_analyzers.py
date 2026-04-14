"""분석 엔진 팩토리 테스트 (Gemini 단일 엔진)"""
import sys
import os
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from analyzers.base import BaseAnalyzer
from analyzers import create_analyzer
from analyzers.gemini_analyzer import GeminiAnalyzer


def test_create_analyzer_returns_gemini():
    analyzer = create_analyzer()
    assert isinstance(analyzer, GeminiAnalyzer)


def test_gemini_inherits_base():
    assert issubclass(GeminiAnalyzer, BaseAnalyzer)
