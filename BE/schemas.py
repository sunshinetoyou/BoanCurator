"""API DTO 계층.

DB 테이블 모델(db/models.py)과 API 응답/요청 계약을 분리한다.
엔드포인트는 여기의 모델만 response_model로 노출해야 하며,
ChromaDB/raw SQL 결과를 직접 반환하면 안 된다.
"""

from datetime import datetime
from typing import List, Optional

from sqlmodel import SQLModel

from db.models import Category, Level, RelativeDifficulty, Theme


class CardView(SQLModel):
    article_id: Optional[int] = None
    source: str
    url: str
    title: str
    published_at: Optional[datetime] = None
    image_urls: Optional[List[str]] = None

    summary: str
    themes: List[str]
    level: Level
    category: Category
    domain_scores: Optional[dict] = None
    relative_difficulty: Optional[RelativeDifficulty] = None

    class Config:
        from_attributes = True


class BookmarkView(SQLModel):
    bookmark_id: int
    article_id: int
    source: str
    url: str
    title: str
    published_at: Optional[datetime] = None
    image_urls: Optional[List[str]] = None
    summary: str
    themes: List[str]
    level: Level
    category: Category
    domain_scores: Optional[dict] = None
    relative_difficulty: Optional[RelativeDifficulty] = None
    bookmarked_at: datetime

    class Config:
        from_attributes = True


class PaginatedResponse(SQLModel):
    items: List[CardView]
    total: int
    offset: int
    limit: int
    has_more: bool


class ThemeSearchRequest(SQLModel):
    search_type: int = 1
    themes: List[Theme]
    offset: int = 0
    limit: int = 20
