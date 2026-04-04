from db.models import SECURITY_DOMAINS, RelativeDifficulty

LEVEL_SCORE = {"Low": 1, "Medium": 3, "High": 5}

# Elo Rating 파라미터
ELO_K = 0.64  # K=16을 1~5 스케일에 맞춤 (16/25=0.64)
ELO_SCALE = 4.0


def calculate_relative_difficulty(
    level: str,
    domain_scores: dict,
    user_expertise: dict,
    level_preference: float = 3.0,
) -> str:
    """기사의 절대 난이도 + 유저 도메인 전문성 + Elo 난이도 선호를 결합.

    combined_gap = 0.4 * domain_gap + 0.6 * elo_gap
    """
    level_score = LEVEL_SCORE.get(level, 3)

    # 도메인 기반 gap
    relevant = [
        user_expertise.get(d, 2)
        for d in SECURITY_DOMAINS
        if domain_scores.get(d, 0) > 0
    ]
    domain_avg = sum(relevant) / len(relevant) if relevant else 2.0
    domain_gap = level_score - domain_avg

    # Elo 기반 gap
    elo_gap = level_score - (level_preference or 3.0)

    # 결합 (Elo 비중 60%)
    combined_gap = 0.4 * domain_gap + 0.6 * elo_gap

    if combined_gap <= -1.5:
        return RelativeDifficulty.Easy.value
    elif combined_gap >= 1.5:
        return RelativeDifficulty.Hard.value
    return RelativeDifficulty.Medium.value


def update_level_preference_elo(
    current_pref: float,
    article_level: str,
    liked: bool,
) -> float:
    """Elo Rating으로 유저 난이도 선호 업데이트.

    - liked=True (좋아요): actual=1
    - liked=False (싫어요): actual=0
    - expected: 유저가 이 난이도를 소화할 확률
    """
    level_score = LEVEL_SCORE.get(article_level, 3)
    expected = 1 / (1 + 10 ** ((level_score - current_pref) / ELO_SCALE))
    actual = 1.0 if liked else 0.0
    new_pref = current_pref + ELO_K * (actual - expected)
    return round(max(1.0, min(5.0, new_pref)), 2)


def update_expertise_on_rating(
    expertise: dict,
    domain_scores: dict,
    relative_difficulty: str,
    liked: bool,
) -> dict:
    """평가에 따른 expertise 업데이트 (방법 B).

    좋아요: 체감 난이도에 비례한 alpha로 EMA
    싫어요+Easy: 기본값(2) 방향으로 감쇠 (관심 없음)
    싫어요+Medium/Hard: 미변동 (난이도 문제이지 관심 문제가 아님)
    """
    updated = dict(expertise)

    if liked:
        alpha_map = {"Easy": 0.03, "Medium": 0.06, "Hard": 0.08}
        alpha = alpha_map.get(relative_difficulty, 0.05)
        for domain in SECURITY_DOMAINS:
            a_score = domain_scores.get(domain, 0)
            if a_score == 0:
                continue
            old = updated.get(domain, 2)
            new_val = alpha * a_score + (1 - alpha) * old
            updated[domain] = round(max(0, min(5, new_val)), 2)
    else:
        if relative_difficulty == "Easy":
            # 관심 없음 → 기본값(2) 방향으로 감쇠
            decay = 0.05
            for domain in SECURITY_DOMAINS:
                a_score = domain_scores.get(domain, 0)
                if a_score == 0:
                    continue
                old = updated.get(domain, 2)
                new_val = old + decay * (2.0 - old)  # 2.0 방향으로 이동
                updated[domain] = round(max(0, min(5, new_val)), 2)
        # Medium/Hard 싫어요 → 미변동

    return updated


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
