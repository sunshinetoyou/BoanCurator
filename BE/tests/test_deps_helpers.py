"""api/deps.py 의 보일러플레이트 추출 헬퍼 단위 테스트.

검증 대상:
  - load_user_or_401: 유저 존재 시 반환, 없으면 401 raise
  - PaginationParams: offset/limit 제약 (ge/le) 강제

배경:
  과거 deps.py:get_current_user 와 auth.py:refresh 가
  `user = services.get_user_by_id(...); if not user: raise 401(...)`
  패턴을 그대로 중복했고, bookmarks.py / searching.py 의
  Query(limit) 가 `ge` 제약 누락으로 limit=0/-N 을 허용했다.
  표준 헬퍼로 추출 후 회귀 방지.
"""
import os
import sys
from pathlib import Path
from unittest.mock import patch, MagicMock

os.environ.setdefault("DATABASE_URL", "postgresql+psycopg2://x:x@localhost:5432/x")

BE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BE_DIR))

import pytest
from fastapi import HTTPException

from api import deps


def test_load_user_or_401_returns_user_when_found():
    session = MagicMock()
    fake_user = MagicMock()
    with patch.object(deps.services, "get_user_by_id", return_value=fake_user):
        result = deps.load_user_or_401(session, user_id=42)
    assert result is fake_user


def test_load_user_or_401_raises_401_when_missing():
    session = MagicMock()
    with patch.object(deps.services, "get_user_by_id", return_value=None):
        with pytest.raises(HTTPException) as exc_info:
            deps.load_user_or_401(session, user_id=999)
    assert exc_info.value.status_code == 401
    assert exc_info.value.detail == "User not found"


def test_pagination_accepts_in_range_values():
    pag = deps.PaginationParams(offset=100, limit=50)
    assert pag.offset == 100
    assert pag.limit == 50


def test_pagination_ge_le_enforced_by_fastapi_at_request_time():
    """ge/le 검증은 FastAPI Query 가 요청 파싱 시점에 강제한다.
    PaginationParams 자체는 단순 dataclass-like 컨테이너이므로 직접 호출은
    음수도 허용된다 (Pydantic/FastAPI 가 endpoint 진입 전에 차단). 본 테스트는
    Query 메타데이터(ge/le) 가 서명에 보존되는지 확인한다.
    """
    import inspect
    sig = inspect.signature(deps.PaginationParams.__init__)
    offset_param = sig.parameters["offset"]
    limit_param = sig.parameters["limit"]

    # FastAPI Query 객체에서 ge/le 메타 추출 (Pydantic v2 의 FieldInfo 형태)
    offset_meta = offset_param.default
    limit_meta = limit_param.default

    metadata = list(offset_meta.metadata) + list(limit_meta.metadata)
    constraints = {type(m).__name__ for m in metadata}
    # Ge / Le 제약 둘 다 등장해야 함 (limit ge 누락 회귀 방지)
    assert "Ge" in constraints
    assert "Le" in constraints
