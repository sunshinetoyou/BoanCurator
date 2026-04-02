from fastapi import APIRouter, Depends, Query
from sqlmodel import Session, select
from sqlalchemy import extract, distinct
from typing import List, Optional

from db.connection import get_session
from db.models import Article, PaginatedResponse, Category, Level
from db.services import get_card_view_list

router = APIRouter()


@router.get("/cardnews", response_model=PaginatedResponse)
def read_dashboard(
    session: Session = Depends(get_session),
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
    )


@router.get("/cardnews/years", response_model=List[int])
def get_available_years(session: Session = Depends(get_session)):
    """기사가 존재하는 연도 목록을 내림차순으로 반환"""
    stmt = (
        select(distinct(extract("year", Article.published_at).cast(int)))
        .where(Article.published_at.is_not(None))
        .order_by(extract("year", Article.published_at).desc())
    )
    return list(session.exec(stmt).all())
