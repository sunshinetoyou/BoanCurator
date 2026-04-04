"""키워드 시맨틱 트래킹 테스트"""
import sys
import os
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from db.vector_store import (
    store_keyword_embedding,
    delete_keyword_embedding,
    match_article_to_keywords,
)

MATCH_PRESETS = {"strict": 0.4, "normal": 0.7, "loose": 1.0}


def test_keyword_embedding_store():
    """키워드 임베딩 저장 + 조회"""
    kid = "test_kw_1"
    store_keyword_embedding(kid, "랜섬웨어 공격 대응 전략", user_id=9999)
    print("[1] 키워드 임베딩 저장 OK")


def test_keyword_match_related():
    """관련 기사 → distance < normal 임계값(0.7)"""
    # 먼저 키워드 저장
    store_keyword_embedding("test_kw_2", "제로데이 취약점 분석", user_id=9998)

    # 관련 텍스트로 매칭 테스트
    results = match_article_to_keywords(
        query_text="최신 제로데이 취약점이 발견되어 긴급 패치가 배포되었습니다",
        user_id=9998,
        top_n=3,
    )
    assert len(results) > 0, "매칭 결과가 없음"
    assert results[0]["distance"] < MATCH_PRESETS["normal"], \
        f"관련 기사인데 distance가 너무 높음: {results[0]['distance']}"
    print(f"[2] 관련 기사 매칭 OK: distance={results[0]['distance']:.3f}")


def test_keyword_no_match():
    """무관한 텍스트 → distance > loose 임계값(1.0) 또는 결과 없음"""
    store_keyword_embedding("test_kw_3", "클라우드 보안 아키텍처", user_id=9997)

    results = match_article_to_keywords(
        query_text="오늘 날씨가 매우 좋습니다. 맛집 추천 부탁드립니다.",
        user_id=9997,
        top_n=3,
    )
    if results:
        assert results[0]["distance"] > MATCH_PRESETS["strict"], \
            f"무관한 텍스트인데 distance가 너무 낮음: {results[0]['distance']}"
    dist = f"{results[0]['distance']:.3f}" if results else "N/A"
    print(f"[3] 무관한 텍스트 필터 OK: distance={dist}")


def test_keyword_delete():
    """키워드 삭제 후 매칭 안 됨"""
    store_keyword_embedding("test_kw_del", "IoT 보안 취약점", user_id=9996)
    delete_keyword_embedding("test_kw_del")

    results = match_article_to_keywords(
        query_text="IoT 디바이스 보안 취약점 분석",
        user_id=9996,
        top_n=3,
    )
    assert len(results) == 0, f"삭제 후에도 매칭됨: {results}"
    print("[4] 키워드 삭제 후 매칭 안 됨 OK")


def _cleanup():
    """테스트 데이터 정리"""
    for kid in ["test_kw_1", "test_kw_2", "test_kw_3", "test_kw_del"]:
        try:
            delete_keyword_embedding(kid)
        except Exception:
            pass


if __name__ == "__main__":
    tests = [
        test_keyword_embedding_store,
        test_keyword_match_related,
        test_keyword_no_match,
        test_keyword_delete,
    ]
    passed = 0
    for test in tests:
        try:
            test()
            passed += 1
        except Exception as e:
            print(f"FAIL {test.__name__}: {e}")
    _cleanup()
    print(f"\n결과: {passed}/{len(tests)} 통과")
