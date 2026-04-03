from db.models import SECURITY_DOMAINS, RelativeDifficulty

LEVEL_SCORE = {"Low": 1, "Medium": 3, "High": 5}


def calculate_relative_difficulty(
    level: str,
    domain_scores: dict,
    user_expertise: dict,
) -> str:
    """기사의 절대 난이도(level) + 유저 도메인 전문성을 결합하여 체감 난이도 반환.

    - level: Gemini가 판단한 기사의 기술적 깊이 (Low/Medium/High)
    - domain_scores: 기사가 다루는 도메인별 관련도
    - user_expertise: 유저의 도메인별 전문성

    gap = 기사 난이도 점수 - 유저의 관련 분야 평균 전문성
    """
    level_score = LEVEL_SCORE.get(level, 3)

    # 기사가 다루는 도메인에서 유저 expertise 평균 계산
    relevant = [
        user_expertise.get(d, 2)
        for d in SECURITY_DOMAINS
        if domain_scores.get(d, 0) > 0
    ]
    if not relevant:
        return RelativeDifficulty.Medium.value

    user_avg = sum(relevant) / len(relevant)
    gap = level_score - user_avg

    if gap <= -1.5:
        return RelativeDifficulty.Easy.value
    elif gap >= 1.5:
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
