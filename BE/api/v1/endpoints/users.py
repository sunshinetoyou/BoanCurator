from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field, field_validator
from sqlmodel import Session, select
from typing import List, Optional

from api.deps import get_current_user
from db.connection import get_session
from db.models import User, SECURITY_DOMAINS, Theme, KeywordAlert, UserNotificationSettings
from db.services import get_user_stats

router = APIRouter()


class ExpertiseUpdateRequest(BaseModel):
    network_infra: int | None = None
    malware_vuln: int | None = None
    cloud_devsecops: int | None = None
    crypto_auth: int | None = None
    policy_compliance: int | None = None
    general_it: int | None = None

    @field_validator("*", mode="before")
    @classmethod
    def validate_range(cls, v):
        if v is not None and not (0 <= v <= 5):
            raise ValueError("각 축의 값은 0~5 범위여야 합니다")
        return v


class ProfileUpdateRequest(BaseModel):
    username: str | None = Field(None, min_length=1, max_length=100)
    profile_image: str | None = Field(None, max_length=2048, pattern=r'^https?://.+$')


class ThemesUpdateRequest(BaseModel):
    themes: List[str] = Field(..., max_length=10)

    @field_validator("themes")
    @classmethod
    def validate_themes(cls, v):
        valid = [t.value for t in Theme]
        for t in v:
            if t not in valid:
                raise ValueError(f"유효하지 않은 테마: {t}. 허용: {valid}")
        return v


# ── 프로필 통합 조회 ──

@router.get("/users/me")
def get_me(user: User = Depends(get_current_user)):
    """현재 로그인한 유저 정보 조회"""
    return {
        "id": user.id,
        "username": user.username,
        "email": user.email,
        "profile_image": user.profile_image,
        "expertise": user.expertise,
        "preferred_themes": user.preferred_themes or [],
        "level_preference": user.level_preference,
    }


@router.get("/users/me/profile")
def get_full_profile(
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """프로필 전체 조회: 기본정보 + expertise + 테마 + 키워드 + 알림설정 + 통계"""
    keywords = session.exec(
        select(KeywordAlert)
        .where(KeywordAlert.user_id == user.id)
        .order_by(KeywordAlert.created_at.desc())
    ).all()

    notif_settings = session.exec(
        select(UserNotificationSettings)
        .where(UserNotificationSettings.user_id == user.id)
    ).first()

    stats = get_user_stats(session, user.id)

    return {
        "user": {
            "id": user.id,
            "username": user.username,
            "email": user.email,
            "profile_image": user.profile_image,
        },
        "expertise": user.expertise,
        "preferred_themes": user.preferred_themes or [],
        "level_preference": user.level_preference,
        "keywords": [
            {"id": k.id, "keyword": k.keyword, "created_at": k.created_at}
            for k in keywords
        ],
        "notification_settings": {
            "match_preset": notif_settings.match_preset if notif_settings else "normal",
            "top_n": notif_settings.top_n if notif_settings else 3,
            "daily_limit": notif_settings.daily_limit if notif_settings else 5,
            "mode": notif_settings.mode if notif_settings else "realtime",
            "has_fcm_token": bool(notif_settings and notif_settings.fcm_token),
        },
        "stats": stats,
    }


@router.put("/users/me")
def update_profile(
    req: ProfileUpdateRequest,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """프로필 수정 (username, profile_image)"""
    if req.username is not None:
        user.username = req.username
    if req.profile_image is not None:
        user.profile_image = req.profile_image
    session.add(user)
    session.commit()
    session.refresh(user)
    return {
        "id": user.id,
        "username": user.username,
        "email": user.email,
        "profile_image": user.profile_image,
        "expertise": user.expertise,
    }


# ── Expertise (도메인 6축) ──

@router.get("/users/me/expertise")
def get_expertise(user: User = Depends(get_current_user)):
    """유저 6축 전문성 프로필 조회"""
    return user.expertise


@router.put("/users/me/expertise")
def update_expertise(
    req: ExpertiseUpdateRequest,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """유저 6축 전문성 프로필 수동 조정 (부분 업데이트 가능)"""
    updated = dict(user.expertise)
    for domain in SECURITY_DOMAINS:
        val = getattr(req, domain, None)
        if val is not None:
            updated[domain] = val
    user.expertise = updated
    session.add(user)
    session.commit()
    session.refresh(user)
    return user.expertise


# ── 테마 (preferred_themes) ──

@router.get("/users/me/themes")
def get_preferred_themes(user: User = Depends(get_current_user)):
    """관심 테마 목록 조회"""
    return {"themes": user.preferred_themes or []}


@router.put("/users/me/themes")
def update_preferred_themes(
    req: ThemesUpdateRequest,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """관심 테마 설정 (전체 덮어쓰기)"""
    user.preferred_themes = req.themes
    session.add(user)
    session.commit()
    session.refresh(user)
    return {"themes": user.preferred_themes}


@router.delete("/users/me/themes")
def clear_preferred_themes(
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """관심 테마 전체 초기화 (필터 해제)"""
    user.preferred_themes = None
    session.add(user)
    session.commit()
    return {"themes": []}


# ── 통계 ──

@router.get("/users/me/stats")
def get_stats(
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """유저 활동 통계 (북마크 수, 도메인별 관심 분포)"""
    return get_user_stats(session, user.id)
