from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException
from jose import jwt, JWTError
from sqlmodel import Session
from pydantic import BaseModel
import requests

from config import settings
from db.connection import get_session
from db import services

router = APIRouter()


def _create_access_token(user_id: int, email: str) -> str:
    """Access token 생성 (짧은 수명)."""
    expire = datetime.now(timezone.utc) + timedelta(hours=settings.jwt_expire_hours)
    payload = {"user_id": user_id, "email": email, "exp": expire, "type": "access"}
    return jwt.encode(payload, settings.jwt_secret, algorithm="HS256")


def _create_refresh_token(user_id: int) -> str:
    """Refresh token 생성 (긴 수명)."""
    expire = datetime.now(timezone.utc) + timedelta(days=settings.jwt_refresh_expire_days)
    payload = {"user_id": user_id, "exp": expire, "type": "refresh"}
    return jwt.encode(payload, settings.jwt_secret, algorithm="HS256")


class GoogleLoginRequest(BaseModel):
    token: str


class RefreshRequest(BaseModel):
    refresh_token: str


class AuthResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    user: dict


class RefreshResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


@router.post("/auth/google", response_model=AuthResponse)
def google_login(
    req: GoogleLoginRequest,
    session: Session = Depends(get_session),
):
    """Google ID Token을 검증하고 JWT를 발급합니다."""

    # Google ID Token 검증 (Google tokeninfo 엔드포인트 사용)
    google_resp = requests.get(
        "https://oauth2.googleapis.com/tokeninfo",
        params={"id_token": req.token},
        timeout=5,
    )

    if google_resp.status_code != 200:
        raise HTTPException(status_code=401, detail="Invalid Google token")

    google_data = google_resp.json()

    # Client ID 일치 확인
    if google_data.get("aud") != settings.google_client_id:
        raise HTTPException(status_code=401, detail="Token not intended for this app")

    google_id = google_data["sub"]
    email = google_data.get("email", "")
    username = google_data.get("name", email.split("@")[0])
    profile_image = google_data.get("picture")

    # 유저 조회/생성
    user = services.get_or_create_user_by_google(
        session=session,
        google_id=google_id,
        email=email,
        username=username,
        profile_image=profile_image,
    )

    # 토큰 생성
    access_token = _create_access_token(user.id, user.email)
    refresh_token = _create_refresh_token(user.id)

    return AuthResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        user={
            "id": user.id,
            "username": user.username,
            "email": user.email,
            "profile_image": user.profile_image,
            "expertise": user.expertise,
        },
    )


@router.post("/auth/refresh", response_model=RefreshResponse)
def refresh_token(
    req: RefreshRequest,
    session: Session = Depends(get_session),
):
    """Refresh token으로 새 access token + refresh token을 발급합니다."""

    try:
        payload = jwt.decode(req.refresh_token, settings.jwt_secret, algorithms=["HS256"])
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid or expired refresh token")

    if payload.get("type") != "refresh":
        raise HTTPException(status_code=401, detail="Invalid token type")

    user_id = payload.get("user_id")
    if user_id is None:
        raise HTTPException(status_code=401, detail="Invalid refresh token")

    user = services.get_user_by_id(session, user_id)
    if not user:
        raise HTTPException(status_code=401, detail="User not found")

    # 새 토큰 쌍 발급 (refresh token rotation)
    new_access = _create_access_token(user.id, user.email)
    new_refresh = _create_refresh_token(user.id)

    return RefreshResponse(
        access_token=new_access,
        refresh_token=new_refresh,
    )
