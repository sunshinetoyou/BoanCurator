from fastapi import APIRouter, Depends, Query, HTTPException
from sqlmodel import Session

from api.deps import get_current_user
from db.connection import get_session
from db.models import User
from db import services

router = APIRouter()


@router.post("/bookmarks")
def create_bookmark(
    article_id: int = Query(...),
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """북마크 생성"""
    bookmark = services.create_bookmark(session, user.id, article_id)
    return {"bookmark_id": bookmark.id, "article_id": bookmark.article_id}


@router.get("/bookmarks")
def get_bookmarks(
    offset: int = Query(default=0, ge=0),
    limit: int = Query(default=20, le=100),
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """현재 유저의 북마크 목록 조회"""
    return services.get_user_bookmarks(session, user.id, offset, limit)


@router.delete("/bookmarks/{bookmark_id}")
def delete_bookmark(
    bookmark_id: int,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """북마크 삭제"""
    deleted = services.delete_bookmark(session, bookmark_id, user.id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Bookmark not found")
    return {"detail": "Bookmark deleted"}
