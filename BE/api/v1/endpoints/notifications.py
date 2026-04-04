from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel, Field
from sqlmodel import Session, select
from typing import Optional

from api.deps import get_current_user
from db.connection import get_session
from db.models import User, UserNotificationSettings, NotificationLog

router = APIRouter()


class NotificationSettingsUpdate(BaseModel):
    match_preset: Optional[str] = Field(None, pattern=r'^(strict|normal|loose)$')
    top_n: Optional[int] = Field(None, ge=1, le=5)
    daily_limit: Optional[int] = Field(None, ge=1, le=20)
    mode: Optional[str] = Field(None, pattern=r'^(realtime|daily)$')


class FCMTokenRequest(BaseModel):
    token: str = Field(..., min_length=10, max_length=500)


def _get_or_create_settings(session: Session, user_id: int) -> UserNotificationSettings:
    settings = session.exec(
        select(UserNotificationSettings).where(
            UserNotificationSettings.user_id == user_id
        )
    ).first()
    if not settings:
        settings = UserNotificationSettings(user_id=user_id)
        session.add(settings)
        session.commit()
        session.refresh(settings)
    return settings


@router.get("/notifications/settings")
def get_notification_settings(
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """알림 설정 조회"""
    s = _get_or_create_settings(session, user.id)
    return {
        "match_preset": s.match_preset,
        "top_n": s.top_n,
        "daily_limit": s.daily_limit,
        "mode": s.mode,
        "has_fcm_token": s.fcm_token is not None,
    }


@router.put("/notifications/settings")
def update_notification_settings(
    req: NotificationSettingsUpdate,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """알림 설정 변경"""
    s = _get_or_create_settings(session, user.id)
    if req.match_preset is not None:
        s.match_preset = req.match_preset
    if req.top_n is not None:
        s.top_n = req.top_n
    if req.daily_limit is not None:
        s.daily_limit = req.daily_limit
    if req.mode is not None:
        s.mode = req.mode
    session.add(s)
    session.commit()
    session.refresh(s)
    return {
        "match_preset": s.match_preset,
        "top_n": s.top_n,
        "daily_limit": s.daily_limit,
        "mode": s.mode,
    }


@router.post("/notifications/fcm-token")
def register_fcm_token(
    req: FCMTokenRequest,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
):
    """FCM 토큰 등록/갱신"""
    s = _get_or_create_settings(session, user.id)
    s.fcm_token = req.token
    session.add(s)
    session.commit()
    return {"status": "registered"}


@router.get("/notifications/log")
def get_notification_log(
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
    offset: int = Query(default=0, ge=0, le=10000),
    limit: int = Query(default=20, ge=1, le=100),
):
    """알림 이력 조회"""
    logs = session.exec(
        select(NotificationLog)
        .where(NotificationLog.user_id == user.id)
        .order_by(NotificationLog.sent_at.desc())
        .offset(offset)
        .limit(limit)
    ).all()
    return [
        {
            "id": l.id,
            "article_id": l.article_id,
            "keyword_alert_id": l.keyword_alert_id,
            "sent_at": l.sent_at,
        }
        for l in logs
    ]
