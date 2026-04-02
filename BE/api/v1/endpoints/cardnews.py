from fastapi import APIRouter, Depends, Query
from sqlmodel import Session, select
from sqlalchemy import extract, distinct
from typing import List, Optional

from db.connection import get_session
from db.models import Article, User, PaginatedResponse, Category, Level
from db.services import get_card_view_list, record_article_read
from api.deps import get_optional_user

router = APIRouter()


@router.get("/cardnews", response_model=PaginatedResponse)
def read_dashboard(
    session: Session = Depends(get_session),
    user: Optional[User] = Depends(get_optional_user),
    category: Optional[Category] = Query(default=None),
    level: Optional[Level] = Query(default=None),
    offset: int = Query(default=0, ge=0),
    limit: int = Query(default=20, le=100),
):
    return get_card_view_list(
        session=session,
        category=category,
        level=level,
        offset=offset,
        limit=limit,
        user_expertise=user.expertise if user else None,
    )


@router.post("/articles/{article_id}/read")
def mark_article_read(
    article_id: int,
    session: Session = Depends(get_session),
    user: Optional[User] = Depends(get_optional_user),
):
    """기사 읽음 이벤트 — 로그인 유저의 expertise 자동 업데이트"""
    if not user:
        return {"status": "ok"}
    record_article_read(session, user.id, article_id)
    return {"status": "ok"}


@router.get("/cardnews/years", response_model=List[int])
def get_available_years(session: Session = Depends(get_session)):
    """기사가 존재하는 연도 목록을 내림차순으로 반환"""
    stmt = (
        select(distinct(extract("year", Article.published_at).cast(int)))
        .where(Article.published_at.is_not(None))
        .order_by(extract("year", Article.published_at).desc())
    )
    return list(session.exec(stmt).all())
