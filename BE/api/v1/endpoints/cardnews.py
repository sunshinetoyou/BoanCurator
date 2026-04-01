from fastapi import APIRouter, Depends, Query
from sqlmodel import Session
from typing import Optional

from db.connection import get_session
from db.models import PaginatedResponse, Category, Level
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
