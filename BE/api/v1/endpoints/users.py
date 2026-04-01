from fastapi import APIRouter, Depends

from api.deps import get_current_user
from db.models import User

router = APIRouter()


@router.get("/users/me")
def get_me(user: User = Depends(get_current_user)):
    """현재 로그인한 유저 정보 조회"""
    return {
        "id": user.id,
        "username": user.username,
        "email": user.email,
        "profile_image": user.profile_image,
        "expertise_level": user.expertise_level,
    }
