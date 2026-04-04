from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field
from sqlmodel import Session, select

from api.deps import get_current_user
from db.connection import get_session
from db.models import User, KeywordAlert
from db.vector_store import store_keyword_embedding, delete_keyword_embedding

router = APIRouter()

MAX_KEYWORDS = 20


class KeywordCreateRequest(BaseModel):
    keyword: str = Field(..., min_length=2, max_length=200)


@router.post("/keywords")
def create_keyword(
    req: KeywordCreateRequest,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """키워드 등록 (임베딩 후 ChromaDB 저장)"""
    # 개수 제한
    count = session.exec(
        select(KeywordAlert).where(KeywordAlert.user_id == user.id)
    ).all()
    if len(count) >= MAX_KEYWORDS:
        raise HTTPException(status_code=400, detail=f"키워드는 최대 {MAX_KEYWORDS}개까지 등록 가능합니다")

    # 중복 체크
    existing = session.exec(
        select(KeywordAlert).where(
            KeywordAlert.user_id == user.id,
            KeywordAlert.keyword == req.keyword,
        )
    ).first()
    if existing:
        raise HTTPException(status_code=400, detail="이미 등록된 키워드입니다")

    # DB 저장
    alert = KeywordAlert(user_id=user.id, keyword=req.keyword, embedding_id="")
    session.add(alert)
    session.commit()
    session.refresh(alert)

    # ChromaDB 임베딩 저장
    embedding_id = f"kw_{alert.id}"
    alert.embedding_id = embedding_id
    session.add(alert)
    session.commit()

    store_keyword_embedding(embedding_id, req.keyword, user_id=user.id)

    return {"id": alert.id, "keyword": alert.keyword, "embedding_id": embedding_id}


@router.get("/keywords")
def list_keywords(
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """내 키워드 목록"""
    alerts = session.exec(
        select(KeywordAlert)
        .where(KeywordAlert.user_id == user.id)
        .order_by(KeywordAlert.created_at.desc())
    ).all()
    return [
        {"id": a.id, "keyword": a.keyword, "created_at": a.created_at}
        for a in alerts
    ]


@router.delete("/keywords/{keyword_id}")
def delete_keyword(
    keyword_id: int,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """키워드 삭제 (ChromaDB에서도 제거)"""
    alert = session.exec(
        select(KeywordAlert).where(
            KeywordAlert.id == keyword_id,
            KeywordAlert.user_id == user.id,
        )
    ).first()
    if not alert:
        raise HTTPException(status_code=404, detail="키워드를 찾을 수 없습니다")

    delete_keyword_embedding(alert.embedding_id)
    session.delete(alert)
    session.commit()
    return {"status": "deleted", "id": keyword_id}
