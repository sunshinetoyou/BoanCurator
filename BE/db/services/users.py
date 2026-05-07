"""User 도메인: Google 인증 / 프로필 / expertise / 통계."""
import logging
from typing import Optional

from sqlmodel import Session, select, func

from ..models import User, Analysis, Bookmark, SECURITY_DOMAINS
from ..difficulty import update_user_expertise

logger = logging.getLogger(__name__)


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


def _update_expertise_on_action(
    session: Session, user_id: int, article_id: int, action: str
):
    """기사의 domain_scores를 기반으로 유저 expertise를 EMA 업데이트.

    Bookmark/read/rating 등 cross-domain 동작에서 호출되는 헬퍼라 users 모듈에 둠.
    호출자(bookmarks, ratings)는 `from . import users` 후 `users._update_expertise_on_action(...)`
    로 명시적 lookup → mock 경로(`patch.object(services.users, ...)`)도 명확.
    """
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


def record_article_read(session: Session, user_id: int, article_id: int):
    """기사 읽음 이벤트 → 유저 expertise 자동 업데이트"""
    _update_expertise_on_action(session, user_id, article_id, "read")
    session.commit()


def get_user_stats(session: Session, user_id: int) -> dict:
    """유저 활동 통계: 북마크 수 + 도메인별 관심 분포"""
    bookmark_count = session.exec(
        select(func.count()).where(Bookmark.user_id == user_id)
    ).one()

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
