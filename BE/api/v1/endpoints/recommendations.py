from typing import List

from fastapi import APIRouter, Depends, Query
from sqlmodel import Session

from api.deps import get_current_user
from db.connection import get_session
from db.models import User
from db import services
from db.vector_store import search_similar
from schemas import CardView

router = APIRouter()


def _get_target_levels(level_preference: float) -> list[str]:
    """Elo 기반 level_preference → 적정 level 범위"""
    if level_preference < 2.0:
        return ["Low", "Medium"]
    elif level_preference < 4.0:
        return ["Low", "Medium", "High"]
    return ["Medium", "High"]


def _domain_match_score(metadata: dict, expertise: dict) -> float:
    """기사의 primary_domain과 유저 expertise 매칭 점수 (높을수록 관심 분야)"""
    primary = metadata.get("primary_domain", "")
    return expertise.get(primary, 0)


@router.get("/recommendations", response_model=List[CardView])
def get_recommendations(
    n: int = Query(default=10, le=50),
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """사용자 expertise + 북마크 기반 개인화 추천."""
    bookmarks = services.get_user_bookmarks(session, user.id, offset=0, limit=10)

    target_levels = _get_target_levels(user.level_preference or 3.0)
    where = {"level": {"$in": target_levels}} if len(target_levels) < 3 else None

    if not bookmarks:
        top_domains = sorted(user.expertise.items(), key=lambda x: x[1], reverse=True)[:3]
        query = " ".join(d for d, _ in top_domains) or "security"
        raw_results = search_similar(query_text=query, n_results=n, where=where)
    else:
        bookmark_texts = [f"{b.title} {b.summary}" for b in bookmarks]
        combined_query = " ".join(bookmark_texts)[:2000]
        bookmarked_ids = {b.article_id for b in bookmarks}
        raw_results = search_similar(
            query_text=combined_query,
            n_results=n + len(bookmarked_ids),
            where=where,
        )
        raw_results = [r for r in raw_results if r["article_id"] not in bookmarked_ids]

    # 도메인 매칭 점수로 재정렬 (관심 분야 우선)
    raw_results.sort(
        key=lambda r: _domain_match_score(r.get("metadata", {}), user.expertise),
        reverse=True,
    )
    ordered_ids = [r["article_id"] for r in raw_results[:n]]

    return services.get_card_views_by_ids(
        session,
        ordered_ids,
        user_expertise=user.expertise,
        level_preference=user.level_preference or 3.0,
    )
