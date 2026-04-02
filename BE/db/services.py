import logging
from sqlmodel import Session, select, func
from sqlalchemy import or_, and_, any_

from .models import *
from .difficulty import calculate_relative_difficulty, update_user_expertise

from datetime import datetime
from typing import Optional

logger = logging.getLogger(__name__)


def is_article_exists(session: Session, url: str) -> bool:
    """URL 기준으로 기사 중복 체크"""
    statement = select(Article).where(Article.url == url)
    results = session.exec(statement).first()
    return results is not None


def save_article(session: Session, scraped_item: Article) -> Article:
    """기사 정보를 저장하고 즉시 커밋하여 독립적인 원자성을 보장합니다."""
    try:
        db_article = Article.model_validate(scraped_item)
        session.add(db_article)
        session.commit()
        session.refresh(db_article)
        return db_article
    except Exception as e:
        session.rollback()
        logger.error(f"기사 저장 실패: {e}")
        raise


def save_analysis(session: Session, article_id: int, analysis_data: AnalysisData) -> Analysis:
    """분석 결과만 별도의 트랜잭션으로 저장합니다."""
    try:
        db_analysis = Analysis.model_validate(
            analysis_data,
            update={
                "article_id": article_id,
                "created_at": datetime.now()
            }
        )
        session.add(db_analysis)
        session.commit()
        session.refresh(db_analysis)
        return db_analysis
    except Exception as e:
        session.rollback()
        logger.error(f"분석 결과 저장 실패 (기사 ID {article_id}): {e}")
        return None


def is_already_analyzed(session: Session, url: str) -> bool:
    statement = select(Article).where(Article.url == url)
    article = session.exec(statement).first()
    if article and article.analysis:
        return True
    return False


def get_article_by_url(session: Session, url: str):
    return session.exec(select(Article).where(Article.url == url)).first()


def get_next_article_to_analyze(session: Session) -> Optional[Article]:
    """분석(Analysis) 데이터가 없는 가장 오래된 기사 하나를 가져옵니다."""
    statement = (
        select(Article)
        .where(~select(Analysis).where(Analysis.article_id == Article.id).exists())
        .order_by(Article.published_at.asc())
        .limit(1)
    )
    return session.exec(statement).first()


# ── 카드뉴스 조회 ──

def _build_card_view_query(
    category: Optional[Category] = None,
    level: Optional[Level] = None,
):
    """카드뉴스 공통 쿼리 빌더 (데이터 조회 + 카운트에서 공유)"""
    statement = select(
        Article.id.label("article_id"),
        Article.source, Article.url, Article.title, Article.published_at, Article.image_urls,
        Analysis.summary, Analysis.themes, Analysis.level, Analysis.category,
        Analysis.domain_scores,
    ).join(Analysis, Article.id == Analysis.article_id)

    if category:
        statement = statement.where(Analysis.category == category)
    if level:
        statement = statement.where(Analysis.level == level)

    return statement


def get_card_view_list(
    session: Session,
    category: Optional[Category] = None,
    level: Optional[Level] = None,
    offset: int = 0,
    limit: int = 20,
    user_expertise: Optional[dict] = None,
) -> PaginatedResponse:
    """필터링된 카드 뉴스를 페이지네이션 메타데이터와 함께 반환합니다."""
    base = _build_card_view_query(category, level)

    count_stmt = select(func.count()).select_from(base.subquery())
    total = session.exec(count_stmt).one()

    data_stmt = base.order_by(Analysis.created_at.desc()).offset(offset).limit(limit)
    rows = session.exec(data_stmt).all()
    items = [CardView.model_validate(row) for row in rows]

    if user_expertise:
        for item in items:
            if item.domain_scores:
                item.relative_difficulty = calculate_relative_difficulty(
                    item.domain_scores, user_expertise
                )

    return PaginatedResponse(
        items=items,
        total=total,
        offset=offset,
        limit=limit,
        has_more=(offset + limit) < total,
    )


# ── 테마 검색 ──

def _build_theme_search_query(req: ThemeSearchRequest, mode: str):
    """테마 검색 공통 쿼리 빌더 (ARRAY 연산자 사용)"""
    statement = select(
        Article.id.label("article_id"),
        Article.source, Article.url, Article.title, Article.published_at, Article.image_urls,
        Analysis.summary, Analysis.themes, Analysis.level, Analysis.category,
        Analysis.domain_scores,
    ).join(Analysis, Article.id == Analysis.article_id)

    if req.themes:
        theme_values = [t.value if hasattr(t, "value") else t for t in req.themes]
        if mode == "any":
            # ANY: themes 배열에 요청된 테마 중 하나라도 포함
            filters = [Analysis.themes.any(tv) for tv in theme_values]
            statement = statement.where(or_(*filters))
        else:
            # ALL: themes 배열에 요청된 테마가 모두 포함
            filters = [Analysis.themes.any(tv) for tv in theme_values]
            statement = statement.where(and_(*filters))

    return statement


def search_articles_by_any_themes(session: Session, req: ThemeSearchRequest) -> PaginatedResponse:
    """입력된 테마 중 하나라도 포함되는 기사를 반환합니다."""
    base = _build_theme_search_query(req, "any")

    count_stmt = select(func.count()).select_from(base.subquery())
    total = session.exec(count_stmt).one()

    data_stmt = base.order_by(Analysis.created_at.desc()).offset(req.offset).limit(req.limit)
    rows = session.exec(data_stmt).all()
    items = [CardView.model_validate(row) for row in rows]

    return PaginatedResponse(
        items=items,
        total=total,
        offset=req.offset,
        limit=req.limit,
        has_more=(req.offset + req.limit) < total,
    )


def search_articles_by_all_themes(session: Session, req: ThemeSearchRequest) -> PaginatedResponse:
    """입력된 모든 테마가 포함된 기사만 반환합니다."""
    base = _build_theme_search_query(req, "all")

    count_stmt = select(func.count()).select_from(base.subquery())
    total = session.exec(count_stmt).one()

    data_stmt = base.order_by(Analysis.created_at.desc()).offset(req.offset).limit(req.limit)
    rows = session.exec(data_stmt).all()
    items = [CardView.model_validate(row) for row in rows]

    return PaginatedResponse(
        items=items,
        total=total,
        offset=req.offset,
        limit=req.limit,
        has_more=(req.offset + req.limit) < total,
    )


def get_active_themes(session: Session) -> List[str]:
    """DB에 존재하는 테마를 반환합니다 (ARRAY unnest 사용)."""
    from sqlalchemy import func as sa_func, literal_column
    query = (
        select(sa_func.unnest(Analysis.themes).label("theme"))
        .where(Analysis.themes.isnot(None))
        .distinct()
        .order_by(literal_column("theme"))
    )
    results = session.execute(query).all()
    return [row[0] for row in results]


# ── 사용자 ──

def get_or_create_user_by_google(
    session: Session,
    google_id: str,
    email: str,
    username: str,
    profile_image: Optional[str] = None,
) -> "User":
    """Google 계정으로 사용자 조회 또는 생성"""
    user = session.exec(select(User).where(User.google_id == google_id)).first()
    if user:
        # 프로필 정보 업데이트
        user.username = username
        user.email = email
        if profile_image:
            user.profile_image = profile_image
        session.add(user)
        session.commit()
        session.refresh(user)
        return user

    user = User(
        google_id=google_id,
        email=email,
        username=username,
        profile_image=profile_image,
    )
    session.add(user)
    session.commit()
    session.refresh(user)
    return user


def get_user_by_id(session: Session, user_id: int) -> Optional["User"]:
    return session.get(User, user_id)


# ── 북마크 ──

def create_bookmark(session: Session, user_id: int, article_id: int) -> "Bookmark":
    """북마크 생성 (중복 시 기존 반환) + 유저 expertise 자동 업데이트"""
    existing = session.exec(
        select(Bookmark).where(
            Bookmark.user_id == user_id,
            Bookmark.article_id == article_id,
        )
    ).first()
    if existing:
        return existing

    bookmark = Bookmark(user_id=user_id, article_id=article_id)
    session.add(bookmark)

    # 유저 expertise 자동 업데이트 (EMA)
    _update_expertise_on_action(session, user_id, article_id, "bookmark")

    session.commit()
    session.refresh(bookmark)
    return bookmark


def _update_expertise_on_action(
    session: Session, user_id: int, article_id: int, action: str
):
    """기사의 domain_scores를 기반으로 유저 expertise를 EMA 업데이트"""
    user = session.get(User, user_id)
    analysis = session.exec(
        select(Analysis).where(Analysis.article_id == article_id)
    ).first()

    if not user or not analysis or not analysis.domain_scores:
        return

    user.expertise = update_user_expertise(
        user.expertise, analysis.domain_scores, action
    )
    session.add(user)


def delete_bookmark(session: Session, bookmark_id: int, user_id: int) -> bool:
    """북마크 삭제 (소유자 확인)"""
    bookmark = session.exec(
        select(Bookmark).where(
            Bookmark.id == bookmark_id,
            Bookmark.user_id == user_id,
        )
    ).first()
    if not bookmark:
        return False
    session.delete(bookmark)
    session.commit()
    return True


def get_user_bookmarks(
    session: Session,
    user_id: int,
    offset: int = 0,
    limit: int = 20,
) -> list[dict]:
    """사용자의 북마크 목록 조회"""
    statement = (
        select(
            Bookmark.id.label("bookmark_id"),
            Article.id.label("article_id"),
            Article.source, Article.url, Article.title,
            Article.published_at, Article.image_urls,
            Analysis.summary, Analysis.themes, Analysis.level, Analysis.category,
            Analysis.domain_scores,
            Bookmark.created_at.label("bookmarked_at"),
        )
        .join(Article, Bookmark.article_id == Article.id)
        .join(Analysis, Article.id == Analysis.article_id)
        .where(Bookmark.user_id == user_id)
        .order_by(Bookmark.created_at.desc())
        .offset(offset)
        .limit(limit)
    )
    rows = session.exec(statement).all()
    return [BookmarkView.model_validate(row) for row in rows]


def record_article_read(session: Session, user_id: int, article_id: int):
    """기사 읽음 이벤트 → 유저 expertise 자동 업데이트"""
    _update_expertise_on_action(session, user_id, article_id, "read")
    session.commit()


def get_user_stats(session: Session, user_id: int) -> dict:
    """유저 활동 통계: 북마크 수 + 도메인별 관심 분포"""
    bookmark_count = session.exec(
        select(func.count()).where(Bookmark.user_id == user_id)
    ).one()

    # 북마크한 기사들의 domain_scores 조회
    rows = session.exec(
        select(Analysis.domain_scores)
        .join(Bookmark, Bookmark.article_id == Analysis.article_id)
        .where(Bookmark.user_id == user_id)
        .where(Analysis.domain_scores.isnot(None))
    ).all()

    domain_distribution = {d: 0 for d in SECURITY_DOMAINS}
    for scores in rows:
        if not scores:
            continue
        primary = max(scores, key=scores.get)
        domain_distribution[primary] += 1

    return {
        "bookmark_count": bookmark_count,
        "domain_distribution": domain_distribution,
    }
