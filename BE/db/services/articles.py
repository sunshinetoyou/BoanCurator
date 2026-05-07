"""Article 도메인: CRUD, 분석 저장/실패 기록, 카드뉴스/검색 조회."""
import logging
from datetime import datetime
from typing import Optional, List

from sqlmodel import Session, select, func
from sqlalchemy import or_, and_

from ..models import (
    Article, Analysis, AnalysisData, AnalysisFailure, Category, Level,
)
from schemas import CardView, PaginatedResponse, ThemeSearchRequest
from ..difficulty import calculate_relative_difficulty

logger = logging.getLogger(__name__)


MAX_ANALYSIS_ATTEMPTS = 2


# ── 기사 CRUD ──

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
        raise


def is_already_analyzed(session: Session, url: str) -> bool:
    statement = select(Article).where(Article.url == url)
    article = session.exec(statement).first()
    if article and article.analysis:
        return True
    return False


def get_article_by_url(session: Session, url: str):
    return session.exec(select(Article).where(Article.url == url)).first()


def get_next_article_to_analyze(session: Session) -> Optional[Article]:
    """분석(Analysis) 데이터가 없고, 실패 누적이 한계 미만인 기사 하나를 가져옵니다."""
    statement = (
        select(Article)
        .outerjoin(Analysis, Article.id == Analysis.article_id)
        .outerjoin(AnalysisFailure, Article.id == AnalysisFailure.article_id)
        .where(Analysis.id == None)  # noqa: E711
        .where(
            or_(
                AnalysisFailure.id == None,  # noqa: E711
                AnalysisFailure.attempt_count < MAX_ANALYSIS_ATTEMPTS,
            )
        )
        .order_by(Article.published_at.asc())
        .limit(1)
    )
    return session.exec(statement).first()


def record_analysis_failure(session: Session, article_id: int, error: str) -> AnalysisFailure:
    """기사 분석 실패를 영속화. 동일 기사에 대해서는 attempt_count만 증가."""
    existing = session.exec(
        select(AnalysisFailure).where(AnalysisFailure.article_id == article_id)
    ).first()
    if existing:
        existing.attempt_count += 1
        existing.last_attempted_at = datetime.now()
        existing.last_error = (error or "")[:500]
        session.add(existing)
        session.commit()
        session.refresh(existing)
        return existing

    failure = AnalysisFailure(
        article_id=article_id,
        attempt_count=1,
        last_error=(error or "")[:500],
    )
    session.add(failure)
    session.commit()
    session.refresh(failure)
    return failure


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
    level_preference: float = 3.0,
) -> PaginatedResponse:
    """필터링된 카드 뉴스를 페이지네이션 메타데이터와 함께 반환합니다."""
    base = _build_card_view_query(category, level)

    count_stmt = select(func.count()).select_from(base.subquery())
    total = session.exec(count_stmt).one()

    data_stmt = base.order_by(Article.published_at.desc()).offset(offset).limit(limit)
    rows = session.exec(data_stmt).all()
    items = [CardView.model_validate(row) for row in rows]

    if user_expertise:
        for item in items:
            if item.domain_scores:
                item.relative_difficulty = calculate_relative_difficulty(
                    item.level, item.domain_scores, user_expertise, level_preference
                )

    return PaginatedResponse(
        items=items,
        total=total,
        offset=offset,
        limit=limit,
        has_more=(offset + limit) < total,
    )


def get_card_views_by_ids(
    session: Session,
    article_ids: list[int],
    user_expertise: Optional[dict] = None,
    level_preference: float = 3.0,
) -> list[CardView]:
    """article_id 목록으로 CardView 조회 (입력 순서 유지)"""
    if not article_ids:
        return []

    statement = select(
        Article.id.label("article_id"),
        Article.source, Article.url, Article.title, Article.published_at, Article.image_urls,
        Analysis.summary, Analysis.themes, Analysis.level, Analysis.category,
        Analysis.domain_scores,
    ).join(Analysis, Article.id == Analysis.article_id).where(
        Article.id.in_(article_ids)
    )
    rows = session.exec(statement).all()

    card_map = {}
    for row in rows:
        card = CardView.model_validate(row)
        if user_expertise and card.domain_scores:
            card.relative_difficulty = calculate_relative_difficulty(
                card.level, card.domain_scores, user_expertise, level_preference
            )
        card_map[card.article_id] = card

    return [card_map[aid] for aid in article_ids if aid in card_map]


# ── 키워드 검색 (하이브리드용) ──

def search_articles_by_keyword(session: Session, query: str, limit: int = 10) -> list[int]:
    """title, summary에서 키워드 ILIKE 매칭 → article_id 리스트"""
    statement = (
        select(Article.id)
        .join(Analysis, Article.id == Analysis.article_id)
        .where(
            or_(
                Article.title.ilike(f"%{query}%"),
                Analysis.summary.ilike(f"%{query}%"),
            )
        )
        .order_by(Analysis.created_at.desc())
        .limit(limit)
    )
    return list(session.exec(statement).all())


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
            filters = [Analysis.themes.any(tv) for tv in theme_values]
            statement = statement.where(or_(*filters))
        else:
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
