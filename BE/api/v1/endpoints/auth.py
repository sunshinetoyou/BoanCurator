from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException
from jose import jwt
from sqlmodel import Session
from pydantic import BaseModel
import requests

from config import settings
from db.connection import get_session
from db import services

router = APIRouter()


class GoogleLoginRequest(BaseModel):
    token: str


class AuthResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: dict


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

    # JWT 생성
    expire = datetime.now(timezone.utc) + timedelta(hours=settings.jwt_expire_hours)
    payload = {
        "user_id": user.id,
        "email": user.email,
        "exp": expire,
    }
    access_token = jwt.encode(payload, settings.jwt_secret, algorithm="HS256")

    return AuthResponse(
        access_token=access_token,
        user={
            "id": user.id,
            "username": user.username,
            "email": user.email,
            "profile_image": user.profile_image,
            "expertise_level": user.expertise_level,
        },
    )
