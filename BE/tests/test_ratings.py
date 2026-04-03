"""기사 평가 + Elo Rating 기반 난이도 선호 적응 테스트"""
import sys
import os
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from db.difficulty import (
    update_level_preference_elo,
    update_expertise_on_rating,
    LEVEL_SCORE,
)


def test_elo_like_hard_article():
    """High 기사에 좋아요 → level_pref 큰 상승"""
    result = update_level_preference_elo(3.0, "High", liked=True)
    assert result > 3.3, f"Expected > 3.3, got {result}"
    print(f"[1] Elo 좋아요+High OK: 3.0 → {result}")


def test_elo_dislike_easy_article():
    """Low 기사에 싫어요 → level_pref 큰 하락"""
    result = update_level_preference_elo(3.0, "Low", liked=False)
    assert result < 2.7, f"Expected < 2.7, got {result}"
    print(f"[2] Elo 싫어요+Low OK: 3.0 → {result}")


def test_elo_like_easy_article():
    """Low 기사에 좋아요 → level_pref 작은 상승 (예상대로니까)"""
    result = update_level_preference_elo(3.0, "Low", liked=True)
    assert 3.0 < result < 3.2, f"Expected 3.0~3.2, got {result}"
    print(f"[3] Elo 좋아요+Low OK: 3.0 → {result} (작은 변동)")


def test_elo_dislike_hard_article():
    """High 기사에 싫어요 → level_pref 작은 하락 (예상대로니까)"""
    result = update_level_preference_elo(3.0, "High", liked=False)
    assert 2.8 < result < 3.0, f"Expected 2.8~3.0, got {result}"
    print(f"[4] Elo 싫어요+High OK: 3.0 → {result} (작은 변동)")


def test_elo_clamp_upper():
    """level_pref가 5.0을 초과하지 않음"""
    result = update_level_preference_elo(4.9, "High", liked=True)
    assert result <= 5.0, f"Expected <= 5.0, got {result}"
    print(f"[5] Elo 상한 클램프 OK: 4.9 → {result}")


def test_elo_clamp_lower():
    """level_pref가 1.0 미만으로 내려가지 않음"""
    result = update_level_preference_elo(1.1, "Low", liked=False)
    assert result >= 1.0, f"Expected >= 1.0, got {result}"
    print(f"[6] Elo 하한 클램프 OK: 1.1 → {result}")


def test_expertise_like_hard():
    """좋아요 + Hard → alpha=0.08로 expertise 강하게 업데이트"""
    expertise = {"network_infra": 2.0, "malware_vuln": 2.0, "cloud_devsecops": 2.0,
                 "crypto_auth": 2.0, "policy_compliance": 2.0, "general_it": 2.0}
    domain_scores = {"network_infra": 4, "malware_vuln": 3}
    result = update_expertise_on_rating(expertise, domain_scores, "Hard", liked=True)
    assert result["network_infra"] > 2.0, f"Expected > 2.0, got {result['network_infra']}"
    assert result["cloud_devsecops"] == 2.0, "무관한 축은 변동 없어야 함"
    print(f"[7] expertise 좋아요+Hard OK: network {expertise['network_infra']} → {result['network_infra']}")


def test_expertise_dislike_easy():
    """싫어요 + Easy → 기본값(2) 방향으로 감쇠"""
    expertise = {"network_infra": 4.0, "malware_vuln": 2.0, "cloud_devsecops": 2.0,
                 "crypto_auth": 2.0, "policy_compliance": 2.0, "general_it": 2.0}
    domain_scores = {"network_infra": 3}
    result = update_expertise_on_rating(expertise, domain_scores, "Easy", liked=False)
    assert result["network_infra"] < 4.0, f"Expected < 4.0, got {result['network_infra']}"
    print(f"[8] expertise 싫어요+Easy OK: network {expertise['network_infra']} → {result['network_infra']}")


def test_expertise_dislike_hard():
    """싫어요 + Hard → expertise 미변동 (Elo만 처리)"""
    expertise = {"network_infra": 2.0, "malware_vuln": 2.0, "cloud_devsecops": 2.0,
                 "crypto_auth": 2.0, "policy_compliance": 2.0, "general_it": 2.0}
    domain_scores = {"network_infra": 5}
    result = update_expertise_on_rating(expertise, domain_scores, "Hard", liked=False)
    assert result["network_infra"] == 2.0, f"Expected 2.0 (미변동), got {result['network_infra']}"
    print("[9] expertise 싫어요+Hard OK: 미변동")


def test_elo_convergence():
    """50건 시뮬레이션: Medium 좋아요/싫어요 반복 → level_pref 안정적 유지"""
    pref = 3.0
    for i in range(50):
        # Medium 기사에 좋아요 70%, 싫어요 30% (현실적 패턴)
        liked = i % 10 < 7
        pref = update_level_preference_elo(pref, "Medium", liked=liked)
    assert 2.0 < pref < 4.5, f"Expected stable range, got {pref}"
    print(f"[10] Elo 안정성 시뮬레이션 OK: 3.0 → {pref} (50건 혼합 평가)")


if __name__ == "__main__":
    tests = [
        test_elo_like_hard_article,
        test_elo_dislike_easy_article,
        test_elo_like_easy_article,
        test_elo_dislike_hard_article,
        test_elo_clamp_upper,
        test_elo_clamp_lower,
        test_expertise_like_hard,
        test_expertise_dislike_easy,
        test_expertise_dislike_hard,
        test_elo_convergence,
    ]
    passed = 0
    for test in tests:
        try:
            test()
            passed += 1
        except Exception as e:
            print(f"FAIL {test.__name__}: {e}")
    print(f"\n결과: {passed}/{len(tests)} 통과")
