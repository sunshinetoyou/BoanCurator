from fastapi import APIRouter, Depends, Query
from sqlmodel import Session

from api.deps import get_current_user
from db.connection import get_session
from db.models import User
from db import services
from db.vector_store import search_similar

router = APIRouter()


@router.get("/recommendations")
def get_recommendations(
    n: int = Query(default=10, le=50),
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """사용자 북마크 기반 개인화 추천."""
    bookmarks = services.get_user_bookmarks(session, user.id, offset=0, limit=10)

    if not bookmarks:
        return []

    bookmark_texts = [f"{b.title} {b.summary}" for b in bookmarks]
    combined_query = " ".join(bookmark_texts)[:2000]

    bookmarked_ids = {b.article_id for b in bookmarks}

    results = search_similar(query_text=combined_query, n_results=n + len(bookmarked_ids))

    filtered = [r for r in results if r["article_id"] not in bookmarked_ids]
    return filtered[:n]
