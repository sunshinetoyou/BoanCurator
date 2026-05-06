from fastapi import Depends, HTTPException, Query, status
from fastapi.security import OAuth2PasswordBearer
from jose import JWTError, jwt
from sqlmodel import Session

from config import settings
from db.connection import get_session
from db.models import User
from db import services

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="", auto_error=False)


def load_user_or_401(session: Session, user_id: int) -> User:
    """user_id로 유저를 조회하고 없으면 401 raise."""
    user = services.get_user_by_id(session, user_id)
    if not user:
        raise HTTPException(status_code=401, detail="User not found")
    return user


def get_current_user(
    token: str = Depends(oauth2_scheme),
    session: Session = Depends(get_session),
) -> User:
    """Authorization 헤더의 JWT에서 유저를 식별합니다."""
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Not authenticated",
            headers={"WWW-Authenticate": "Bearer"},
        )

    try:
        payload = jwt.decode(token, settings.jwt_secret, algorithms=["HS256"])
        user_id: int = payload.get("user_id")
        if user_id is None:
            raise HTTPException(status_code=401, detail="Invalid token")
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")

    return load_user_or_401(session, user_id)


def get_optional_user(
    token: str = Depends(oauth2_scheme),
    session: Session = Depends(get_session),
) -> User | None:
    """인증 선택적 — 토큰 없으면 None, 있으면 유저 반환."""
    if not token:
        return None
    try:
        payload = jwt.decode(token, settings.jwt_secret, algorithms=["HS256"])
        user_id: int = payload.get("user_id")
        if user_id is None:
            return None
    except JWTError:
        return None
    return services.get_user_by_id(session, user_id)


class PaginationParams:
    """offset/limit 표준 페이지네이션 파라미터.

    표준 제약: offset >= 0, offset <= 10000, 1 <= limit <= 100.
    의도: 모든 페이지네이션 엔드포인트에서 일관된 제약을 강제하고, 과거
    bookmarks.py/searching.py 가 limit 에 ge 제약 누락으로 limit=0/-N 을
    허용했던 버그를 회귀 방지한다.
    """
    def __init__(
        self,
        offset: int = Query(default=0, ge=0, le=10000),
        limit: int = Query(default=20, ge=1, le=100),
    ):
        self.offset = offset
        self.limit = limit
