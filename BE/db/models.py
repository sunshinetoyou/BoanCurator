from typing import List, Optional
from datetime import datetime
from sqlmodel import SQLModel, Field, Relationship, Column
from sqlalchemy.dialects.postgresql import ARRAY
from sqlalchemy import String
from enum import StrEnum


class Level(StrEnum):
    Low = "Low"
    Medium = "Medium"
    High = "High"

class Category(StrEnum):
    TECH = "Tech"
    ECONOMY = "Economy"
    POLITICS = "Politics"
    SOCIETY = "Society"
    CULTURE = "Culture"
    WORLD = "World"

class Theme(StrEnum):
    SECURITY = "Security"
    AI_ML = "AI/ML"
    INFRA_CLOUD = "Infra/Cloud"
    DEV_STACK = "Development"
    BIZ_POLICY = "Business/Policy"
    GENERAL_IT = "General IT"


class Article(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    title: str
    url: str = Field(index=True, unique=True)
    source: str
    published_at: Optional[datetime] = None
    created_at: datetime = Field(default_factory=datetime.now)
    content: str
    image_urls: Optional[List[str]] = Field(default=None, sa_column=Column(ARRAY(String)))
    analysis: Optional["Analysis"] = Relationship(back_populates="article")


class AnalysisData(SQLModel):
    category: Category
    themes: List[str] = Field(sa_column=Column(ARRAY(String)))
    summary: str
    level: Level
    prompt_version: str
    model: str


class Analysis(AnalysisData, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    created_at: datetime = Field(default_factory=datetime.now)

    article_id: int = Field(foreign_key="article.id", unique=True)
    article: Optional[Article] = Relationship(back_populates="analysis")


# VIEW 설정
class CardView(SQLModel):
    source: str
    url: str
    title: str
    published_at: Optional[datetime] = None
    image_urls: Optional[List[str]] = None

    summary: str
    themes: List[str]
    level: Level
    category: Category

    class Config:
        from_attributes = True


class ThemeSearchRequest(SQLModel):
    search_type: int = 1
    themes: List[Theme]
    offset: int = 0
    limit: int = 20


class PaginatedResponse(SQLModel):
    items: List[CardView]
    total: int
    offset: int
    limit: int
    has_more: bool


# ── 사용자 및 북마크 ──

class User(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    google_id: str = Field(index=True, unique=True)
    email: str = Field(index=True, unique=True)
    username: str
    profile_image: Optional[str] = None
    expertise_level: Level = Field(default=Level.Low)
    created_at: datetime = Field(default_factory=datetime.now)
    bookmarks: List["Bookmark"] = Relationship(back_populates="user")


class Bookmark(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id", index=True)
    article_id: int = Field(foreign_key="article.id", index=True)
    created_at: datetime = Field(default_factory=datetime.now)

    user: Optional[User] = Relationship(back_populates="bookmarks")
    article: Optional[Article] = Relationship()


class BookmarkView(SQLModel):
    bookmark_id: int
    article_id: int
    source: str
    url: str
    title: str
    summary: str
    themes: List[str]
    level: Level
    category: Category
    bookmarked_at: datetime

    class Config:
        from_attributes = True
