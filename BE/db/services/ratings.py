"""Rating 도메인: 좋아요/싫어요 평가 + Elo/expertise 업데이트."""
import logging

from sqlmodel import Session, select

from ..models import ArticleRating, Analysis, User
from ..difficulty import (
    calculate_relative_difficulty,
    update_level_preference_elo,
    update_expertise_on_rating,
)

logger = logging.getLogger(__name__)


def rate_article(session: Session, user_id: int, article_id: int, rating: int) -> ArticleRating:
    """기사 평가 (좋아요=1, 싫어요=-1). 기존 평가 있으면 덮어쓰기.
    Elo로 level_preference 업데이트 + 방법B로 expertise 업데이트."""
    existing = session.exec(
        select(ArticleRating).where(
            ArticleRating.user_id == user_id,
            ArticleRating.article_id == article_id,
        )
    ).first()

    if existing:
        existing.rating = rating
        session.add(existing)
        article_rating = existing
    else:
        article_rating = ArticleRating(
            user_id=user_id, article_id=article_id, rating=rating
        )
        session.add(article_rating)

    analysis = session.exec(
        select(Analysis).where(Analysis.article_id == article_id)
    ).first()

    user = session.get(User, user_id)
    if user and analysis and analysis.domain_scores:
        liked = rating == 1

        # 1. Elo로 level_preference 업데이트
        user.level_preference = update_level_preference_elo(
            user.level_preference, analysis.level, liked
        )

        # 2. 체감 난이도 계산
        rel_diff = calculate_relative_difficulty(
            analysis.level, analysis.domain_scores,
            user.expertise, user.level_preference,
        )

        # 3. 방법B로 expertise 업데이트
        user.expertise = update_expertise_on_rating(
            user.expertise, analysis.domain_scores, rel_diff, liked
        )

        session.add(user)

    session.commit()
    session.refresh(article_rating)
    return article_rating


def get_user_ratings(session: Session, user_id: int) -> list[ArticleRating]:
    """유저의 평가 목록"""
    return list(session.exec(
        select(ArticleRating)
        .where(ArticleRating.user_id == user_id)
        .order_by(ArticleRating.created_at.desc())
    ).all())


def delete_rating(session: Session, user_id: int, article_id: int) -> bool:
    """평가 취소"""
    existing = session.exec(
        select(ArticleRating).where(
            ArticleRating.user_id == user_id,
            ArticleRating.article_id == article_id,
        )
    ).first()
    if not existing:
        return False
    session.delete(existing)
    session.commit()
    return True
