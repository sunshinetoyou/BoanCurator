"""분석 엔진 팩토리 + 플러그인 테스트"""
import sys
import os
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from analyzers.base import BaseAnalyzer
from config import settings


def test_create_analyzer_gemini():
    """engine=gemini → GeminiAnalyzer 반환"""
    from analyzers import create_analyzer
    from analyzers.gemini_analyzer import GeminiAnalyzer

    settings.analysis_engine = "gemini"
    analyzer = create_analyzer()
    assert isinstance(analyzer, GeminiAnalyzer), f"Expected GeminiAnalyzer, got {type(analyzer)}"
    print("[1] create_analyzer(gemini) OK")


def test_create_analyzer_claude():
    """engine=claude → ClaudeAnalyzer 반환"""
    from analyzers import create_analyzer
    from analyzers.claude_analyzer import ClaudeAnalyzer

    settings.analysis_engine = "claude"
    analyzer = create_analyzer()
    assert isinstance(analyzer, ClaudeAnalyzer), f"Expected ClaudeAnalyzer, got {type(analyzer)}"
    print("[2] create_analyzer(claude) OK")


def test_create_analyzer_ollama():
    """engine=ollama → OllamaAnalyzer 반환"""
    from analyzers import create_analyzer
    from analyzers.ollama_analyzer import OllamaAnalyzer

    settings.analysis_engine = "ollama"
    analyzer = create_analyzer()
    assert isinstance(analyzer, OllamaAnalyzer), f"Expected OllamaAnalyzer, got {type(analyzer)}"
    print("[3] create_analyzer(ollama) OK")


def test_create_analyzer_unknown():
    """잘못된 엔진 → ValueError"""
    from analyzers import create_analyzer

    settings.analysis_engine = "unknown_engine"
    try:
        create_analyzer()
        assert False, "ValueError가 발생해야 함"
    except ValueError as e:
        print(f"[4] unknown engine ValueError OK: {e}")
    finally:
        settings.analysis_engine = "gemini"


def test_all_analyzers_inherit_base():
    """모든 분석기가 BaseAnalyzer를 상속하는지 확인"""
    from analyzers.gemini_analyzer import GeminiAnalyzer
    from analyzers.claude_analyzer import ClaudeAnalyzer
    from analyzers.ollama_analyzer import OllamaAnalyzer

    for cls in [GeminiAnalyzer, ClaudeAnalyzer, OllamaAnalyzer]:
        assert issubclass(cls, BaseAnalyzer), f"{cls.__name__}이 BaseAnalyzer를 상속하지 않음"
    print("[5] 모든 분석기 BaseAnalyzer 상속 OK")


if __name__ == "__main__":
    tests = [
        test_create_analyzer_gemini,
        test_create_analyzer_claude,
        test_create_analyzer_ollama,
        test_create_analyzer_unknown,
        test_all_analyzers_inherit_base,
    ]
    passed = 0
    for test in tests:
        try:
            test()
            passed += 1
        except Exception as e:
            print(f"FAIL {test.__name__}: {e}")
    print(f"\n결과: {passed}/{len(tests)} 통과")
