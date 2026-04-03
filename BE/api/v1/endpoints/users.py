from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field, field_validator
from sqlmodel import Session

from api.deps import get_current_user
from db.connection import get_session
from db.models import User, SECURITY_DOMAINS
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


@router.get("/users/me")
def get_me(user: User = Depends(get_current_user)):
    """현재 로그인한 유저 정보 조회"""
    return {
        "id": user.id,
        "username": user.username,
        "email": user.email,
        "profile_image": user.profile_image,
        "expertise": user.expertise,
    }


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


@router.get("/users/me/stats")
def get_stats(
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """유저 활동 통계 (북마크 수, 도메인별 관심 분포)"""
    return get_user_stats(session, user.id)


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
