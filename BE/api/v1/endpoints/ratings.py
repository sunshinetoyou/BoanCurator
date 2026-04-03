from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel, Field
from sqlmodel import Session

from api.deps import get_current_user
from db.connection import get_session
from db.models import User
from db import services

router = APIRouter()


class RatingRequest(BaseModel):
    article_id: int
    rating: int = Field(..., ge=-1, le=1, description="1=좋아요, -1=싫어요")


@router.post("/ratings")
def rate_article(
    req: RatingRequest,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """기사 평가 (좋아요/싫어요). 기존 평가 있으면 덮어쓰기."""
    if req.rating == 0:
        raise HTTPException(status_code=400, detail="rating은 1 또는 -1이어야 합니다")
    result = services.rate_article(session, user.id, req.article_id, req.rating)
    return {
        "article_id": req.article_id,
        "rating": result.rating,
        "level_preference": user.level_preference,
    }


@router.get("/ratings")
def get_my_ratings(
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """내 평가 목록"""
    ratings = services.get_user_ratings(session, user.id)
    return [
        {"article_id": r.article_id, "rating": r.rating, "created_at": r.created_at}
        for r in ratings
    ]


@router.delete("/ratings/{article_id}")
def delete_my_rating(
    article_id: int,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """평가 취소"""
    if not services.delete_rating(session, user.id, article_id):
        raise HTTPException(status_code=404, detail="평가를 찾을 수 없습니다")
    return {"status": "deleted", "article_id": article_id}
