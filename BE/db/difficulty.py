from db.models import SECURITY_DOMAINS, RelativeDifficulty


def calculate_relative_difficulty(
    article_scores: dict,
    user_expertise: dict,
) -> str:
    """기사 도메인 점수와 유저 전문성을 비교하여 상대 난이도 반환.

    기사 점수 > 0인 축만 대상으로, 기사 점수를 가중치로 사용한 가중 평균 delta 계산.
    """
    total_weight = 0
    weighted_delta = 0.0

    for domain in SECURITY_DOMAINS:
        a_score = article_scores.get(domain, 0)
        if a_score == 0:
            continue
        u_score = user_expertise.get(domain, 2)
        weighted_delta += a_score * (a_score - u_score)
        total_weight += a_score

    if total_weight == 0:
        return RelativeDifficulty.Medium.value

    avg_delta = weighted_delta / total_weight

    if avg_delta <= -1.0:
        return RelativeDifficulty.Easy.value
    elif avg_delta > 1.0:
        return RelativeDifficulty.Hard.value
    return RelativeDifficulty.Medium.value


def update_user_expertise(
    current_expertise: dict,
    article_scores: dict,
    action: str = "read",
) -> dict:
    """EMA로 유저 전문성 업데이트. 기사 점수 > 0인 축만 반영.

    alpha: read=0.05, bookmark=0.1 (북마크가 더 강한 관심 신호)
    """
    alpha = 0.1 if action == "bookmark" else 0.05
    updated = dict(current_expertise)

    for domain in SECURITY_DOMAINS:
        a_score = article_scores.get(domain, 0)
        if a_score == 0:
            continue
        old = updated.get(domain, 2)
        new_val = alpha * a_score + (1 - alpha) * old
        updated[domain] = round(max(0, min(5, new_val)), 2)

    return updated
