from fastapi import APIRouter, Query
from typing import Optional

from db.vector_store import search_similar, search_similar_by_id

router = APIRouter()


@router.get("/search/semantic")
def semantic_search(
    q: str = Query(..., min_length=2, description="검색 쿼리"),
    n: int = Query(default=10, le=50),
    level: Optional[str] = Query(default=None),
    category: Optional[str] = Query(default=None),
):
    """ChromaDB 기반 시맨틱 유사도 검색"""
    where = {}
    if level:
        where["level"] = level
    if category:
        where["category"] = category

    return search_similar(
        query_text=q,
        n_results=n,
        where=where if where else None,
    )


@router.get("/search/similar/{article_id}")
def find_similar_articles(
    article_id: int,
    n: int = Query(default=10, le=50),
):
    """특정 기사와 유사한 기사 조회"""
    return search_similar_by_id(article_id=article_id, n_results=n)
