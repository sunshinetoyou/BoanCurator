from fastapi import APIRouter, Depends, Query, HTTPException, status
from sqlmodel import Session
from typing import List

from db.connection import get_session
from db.models import Theme
from schemas import PaginatedResponse, ThemeSearchRequest
from db.services import search_articles_by_all_themes, search_articles_by_any_themes, get_active_themes

router = APIRouter()


@router.get("/search/theme", response_model=PaginatedResponse)
def search_by_themes(
    session: Session = Depends(get_session),
    themes: List[Theme] = Query(...),
    search_type: int = Query(default=1, ge=1, le=2, description="1=ANY, 2=ALL"),
    offset: int = Query(default=0, ge=0),
    limit: int = Query(default=20, le=100),
):
    """search_type: 1 → 하나라도 포함(OR), 2 → 모두 포함(AND)"""
    req = ThemeSearchRequest(
        search_type=search_type,
        themes=themes,
        offset=offset,
        limit=limit,
    )
    if search_type == 1:
        return search_articles_by_any_themes(session, req)
    return search_articles_by_all_themes(session, req)


@router.get("/themes")
def get_theme_list(
    session: Session = Depends(get_session),
):
    """DB 내에 존재하는 테마 리스트 반환"""
    return get_active_themes(session)
