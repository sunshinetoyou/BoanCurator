from typing import List, Optional
from datetime import datetime
from sqlmodel import SQLModel, Field, Relationship, Column
from sqlalchemy.dialects.postgresql import ARRAY
from sqlalchemy import String, JSON
from enum import StrEnum


# ── 6축 도메인 정의 ──

SECURITY_DOMAINS = [
    "network_infra",
    "malware_vuln",
    "cloud_devsecops",
    "crypto_auth",
    "policy_compliance",
    "general_it",
]

DEFAULT_EXPERTISE = {d: 2 for d in SECURITY_DOMAINS}


class Level(StrEnum):
    Low = "Low"
    Medium = "Medium"
    High = "High"


class RelativeDifficulty(StrEnum):
    Easy = "Easy"
    Medium = "Medium"
    Hard = "Hard"

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
    domain_scores: Optional[dict] = Field(default=None, sa_column=Column(JSON))
    prompt_version: str
    model: str


class Analysis(AnalysisData, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    created_at: datetime = Field(default_factory=datetime.now)

    article_id: int = Field(foreign_key="article.id", unique=True)
    article: Optional[Article] = Relationship(back_populates="analysis")


# VIEW 설정
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


# ── 커스텀 소스 ──

class CustomSource(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    url: str = Field(index=True, unique=True)
    source_name: str
    content_selector: Optional[str] = None
    has_full_content: bool = True
    period: int = 10800
    enabled: bool = True
    last_error: Optional[str] = None
    last_scraped_at: Optional[datetime] = None
    created_at: datetime = Field(default_factory=datetime.now)


# ── 사용자 및 북마크 ──

class User(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    google_id: str = Field(index=True, unique=True)
    email: str = Field(index=True, unique=True)
    username: str
    profile_image: Optional[str] = None
    expertise: dict = Field(default_factory=lambda: dict(DEFAULT_EXPERTISE), sa_column=Column(JSON))
    level_preference: float = Field(default=3.0)
    created_at: datetime = Field(default_factory=datetime.now)
    bookmarks: List["Bookmark"] = Relationship(back_populates="user")


class Bookmark(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id", index=True)
    article_id: int = Field(foreign_key="article.id", index=True)
    created_at: datetime = Field(default_factory=datetime.now)

    user: Optional[User] = Relationship(back_populates="bookmarks")
    article: Optional[Article] = Relationship()


class ArticleRating(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id", index=True)
    article_id: int = Field(foreign_key="article.id", index=True)
    rating: int  # 1=좋아요, -1=싫어요
    created_at: datetime = Field(default_factory=datetime.now)


# ── 키워드 알림 ──

class KeywordAlert(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id", index=True)
    keyword: str
    embedding_id: str
    created_at: datetime = Field(default_factory=datetime.now)


class NotificationLog(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id", index=True)
    article_id: int = Field(foreign_key="article.id", index=True)
    keyword_alert_id: int = Field(foreign_key="keywordalert.id")
    sent_at: datetime = Field(default_factory=datetime.now)


class UserNotificationSettings(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id", unique=True, index=True)
    match_preset: str = Field(default="normal")  # strict/normal/loose
    top_n: int = Field(default=3, ge=1, le=5)
    daily_limit: int = Field(default=5, ge=1, le=20)
    mode: str = Field(default="realtime")  # realtime/daily
    fcm_token: Optional[str] = None


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
