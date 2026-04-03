from fastapi import APIRouter, Depends, Query
from sqlmodel import Session

from api.deps import get_current_user
from db.connection import get_session
from db.models import User, SECURITY_DOMAINS
from db import services
from db.vector_store import search_similar
from db.difficulty import calculate_relative_difficulty

router = APIRouter()


def _get_target_levels(expertise: dict) -> list[str]:
    """유저 expertise 평균 → 적정 level 범위"""
    avg = sum(expertise.values()) / len(expertise) if expertise else 2
    if avg < 2:
        return ["Low", "Medium"]
    elif avg < 3.5:
        return ["Low", "Medium", "High"]
    return ["Medium", "High"]


def _domain_match_score(metadata: dict, expertise: dict) -> float:
    """기사의 primary_domain과 유저 expertise 매칭 점수 (높을수록 관심 분야)"""
    primary = metadata.get("primary_domain", "")
    return expertise.get(primary, 0)


@router.get("/recommendations")
def get_recommendations(
    n: int = Query(default=10, le=50),
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """사용자 expertise + 북마크 기반 개인화 추천."""
    bookmarks = services.get_user_bookmarks(session, user.id, offset=0, limit=10)

    # 적정 level 범위
    target_levels = _get_target_levels(user.expertise)

    if not bookmarks:
        # 북마크 없으면 expertise 높은 도메인 키워드로 검색
        top_domains = sorted(user.expertise.items(), key=lambda x: x[1], reverse=True)[:3]
        query = " ".join(d for d, _ in top_domains)
        results = search_similar(
            query_text=query,
            n_results=n,
            where={"level": {"$in": target_levels}} if len(target_levels) < 3 else None,
        )
    else:
        bookmark_texts = [f"{b.title} {b.summary}" for b in bookmarks]
        combined_query = " ".join(bookmark_texts)[:2000]
        bookmarked_ids = {b.article_id for b in bookmarks}

        results = search_similar(
            query_text=combined_query,
            n_results=n + len(bookmarked_ids),
            where={"level": {"$in": target_levels}} if len(target_levels) < 3 else None,
        )
        results = [r for r in results if r["article_id"] not in bookmarked_ids]

    # 도메인 매칭 점수로 재정렬 (관심 분야 우선)
    for r in results:
        r["domain_match"] = _domain_match_score(r.get("metadata", {}), user.expertise)
        level = r.get("metadata", {}).get("level", "Medium")
        domain_scores = {}  # ChromaDB 메타데이터에는 full domain_scores 없음
        primary = r.get("metadata", {}).get("primary_domain", "")
        if primary:
            domain_scores[primary] = 3  # 근사값
        r["relative_difficulty"] = calculate_relative_difficulty(
            level, domain_scores, user.expertise
        )

    results.sort(key=lambda r: r["domain_match"], reverse=True)
    return results[:n]
