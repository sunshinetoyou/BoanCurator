"""Bookmark 도메인: 생성/삭제/목록 조회."""
import logging

from sqlmodel import Session, select

from ..models import Bookmark, Article, Analysis
from schemas import BookmarkView
from . import users

logger = logging.getLogger(__name__)


def create_bookmark(session: Session, user_id: int, article_id: int) -> "Bookmark":
    """북마크 생성 (중복 시 기존 반환) + 유저 expertise 자동 업데이트"""
    existing = session.exec(
        select(Bookmark).where(
            Bookmark.user_id == user_id,
            Bookmark.article_id == article_id,
        )
    ).first()
    if existing:
        return existing

    bookmark = Bookmark(user_id=user_id, article_id=article_id)
    session.add(bookmark)

    # cross-domain: users 모듈을 통해 명시적 호출 (mock 경로 명확화)
    users._update_expertise_on_action(session, user_id, article_id, "bookmark")

    session.commit()
    session.refresh(bookmark)
    return bookmark


def delete_bookmark(session: Session, bookmark_id: int, user_id: int) -> bool:
    """북마크 삭제 (소유자 확인)"""
    bookmark = session.exec(
        select(Bookmark).where(
            Bookmark.id == bookmark_id,
            Bookmark.user_id == user_id,
        )
    ).first()
    if not bookmark:
        return False
    session.delete(bookmark)
    session.commit()
    return True


def get_user_bookmarks(
    session: Session,
    user_id: int,
    offset: int = 0,
    limit: int = 20,
) -> list[dict]:
    """사용자의 북마크 목록 조회"""
    statement = (
        select(
            Bookmark.id.label("bookmark_id"),
            Article.id.label("article_id"),
            Article.source, Article.url, Article.title,
            Article.published_at, Article.image_urls,
            Analysis.summary, Analysis.themes, Analysis.level, Analysis.category,
            Analysis.domain_scores,
            Bookmark.created_at.label("bookmarked_at"),
        )
        .join(Article, Bookmark.article_id == Article.id)
        .join(Analysis, Article.id == Analysis.article_id)
        .where(Bookmark.user_id == user_id)
        .order_by(Bookmark.created_at.desc())
        .offset(offset)
        .limit(limit)
    )
    rows = session.exec(statement).all()
    return [BookmarkView.model_validate(row) for row in rows]
