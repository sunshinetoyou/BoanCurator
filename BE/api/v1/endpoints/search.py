from fastapi import APIRouter, Depends, Query
from sqlmodel import Session
from typing import Optional, List

from db.connection import get_session
from db.models import CardView, User
from db.vector_store import search_similar, search_similar_by_id
from db.services import get_card_views_by_ids
from api.deps import get_optional_user

router = APIRouter()


@router.get("/search/semantic", response_model=List[CardView])
def semantic_search(
    q: str = Query(..., min_length=2, max_length=500, description="검색 쿼리"),
    n: int = Query(default=10, ge=1, le=50),
    level: Optional[str] = Query(default=None),
    category: Optional[str] = Query(default=None),
    session: Session = Depends(get_session),
    user: Optional[User] = Depends(get_optional_user),
):
    """ChromaDB 기반 시맨틱 유사도 검색 → CardView로 반환"""
    where = {}
    if level:
        where["level"] = level
    if category:
        where["category"] = category

    results = search_similar(
        query_text=q,
        n_results=n,
        where=where if where else None,
    )

    article_ids = [r["article_id"] for r in results]
    return get_card_views_by_ids(
        session, article_ids,
        user_expertise=user.expertise if user else None,
    )


@router.get("/search/similar/{article_id}", response_model=List[CardView])
def find_similar_articles(
    article_id: int,
    n: int = Query(default=10, le=50),
    session: Session = Depends(get_session),
    user: Optional[User] = Depends(get_optional_user),
):
    """특정 기사와 유사한 기사 조회 → CardView로 반환"""
    results = search_similar_by_id(article_id=article_id, n_results=n)

    article_ids = [r["article_id"] for r in results]
    return get_card_views_by_ids(
        session, article_ids,
        user_expertise=user.expertise if user else None,
    )
