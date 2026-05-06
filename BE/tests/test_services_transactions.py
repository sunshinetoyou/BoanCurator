"""services.py 트랜잭션 함수 단위 테스트.

대상:
  - save_article: 정상 / commit 실패 시 rollback + raise
  - record_analysis_failure: 첫 실패 / 재시도 increment / 에러 메시지 500자 truncate
  - create_bookmark: 기존 존재 시 재사용 / 새 생성 시 expertise 업데이트
  - delete_bookmark: 소유자 일치 / 불일치

mock session 패턴은 test_services_save_analysis.py 와 동일.
"""
import os
import sys
from pathlib import Path
from unittest.mock import MagicMock, patch

os.environ.setdefault("DATABASE_URL", "postgresql+psycopg2://x:x@localhost:5432/x")

BE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BE_DIR))

import pytest

from db import services
from db.models import Article, AnalysisFailure, Bookmark


# ── save_article ────────────────────────────────────────

def _valid_article() -> Article:
    return Article(
        title="t", url="https://example.com/a", source="test", content="c"
    )


def test_save_article_success_returns_persisted():
    session = MagicMock()
    result = services.save_article(session, _valid_article())
    assert result is not None
    session.add.assert_called_once()
    session.commit.assert_called_once()
    session.refresh.assert_called_once()


def test_save_article_raises_on_commit_failure():
    """save_analysis 와 동일 계약 — 호출자(스크래퍼)가 try/except 로 처리 가정."""
    session = MagicMock()
    session.commit.side_effect = RuntimeError("constraint violation")
    with pytest.raises(RuntimeError, match="constraint violation"):
        services.save_article(session, _valid_article())
    session.rollback.assert_called_once()


# ── record_analysis_failure ────────────────────────────────────────

def _exec_returns_first(value):
    """session.exec(stmt).first() == value 가 되도록 MagicMock 체인 구성."""
    exec_result = MagicMock()
    exec_result.first.return_value = value
    return exec_result


def test_record_analysis_failure_first_time_creates_with_attempt_1():
    session = MagicMock()
    session.exec.return_value = _exec_returns_first(None)  # 기존 실패 없음

    result = services.record_analysis_failure(session, article_id=1, error="boom")

    assert result.attempt_count == 1
    assert result.article_id == 1
    assert result.last_error == "boom"
    session.add.assert_called_once()
    session.commit.assert_called_once()


def test_record_analysis_failure_increments_existing_attempt_count():
    """동일 article_id 재시도 시 attempt_count 만 증가 (회귀 방지: 무한 재시도 차단의 핵심)."""
    existing = AnalysisFailure(article_id=1, attempt_count=1, last_error="prev")
    session = MagicMock()
    session.exec.return_value = _exec_returns_first(existing)

    result = services.record_analysis_failure(session, article_id=1, error="again")

    assert result is existing
    assert result.attempt_count == 2
    assert result.last_error == "again"
    session.commit.assert_called_once()


def test_record_analysis_failure_truncates_long_error():
    session = MagicMock()
    session.exec.return_value = _exec_returns_first(None)
    long_err = "x" * 1000

    result = services.record_analysis_failure(session, article_id=1, error=long_err)

    assert len(result.last_error) == 500


def test_record_analysis_failure_handles_none_error():
    session = MagicMock()
    session.exec.return_value = _exec_returns_first(None)
    result = services.record_analysis_failure(session, article_id=1, error=None)
    assert result.last_error == ""


# ── create_bookmark ────────────────────────────────────────

def test_create_bookmark_returns_existing_when_present():
    """이미 같은 (user_id, article_id) 북마크가 있으면 새로 만들지 않고 반환."""
    existing = Bookmark(id=7, user_id=1, article_id=10)
    session = MagicMock()
    session.exec.return_value = _exec_returns_first(existing)

    result = services.create_bookmark(session, user_id=1, article_id=10)

    assert result is existing
    # 새 Bookmark 추가도 expertise 업데이트도 일어나면 안 됨
    session.add.assert_not_called()
    session.commit.assert_not_called()


def test_create_bookmark_creates_new_and_calls_expertise_update():
    """신규 북마크 생성 시 _update_expertise_on_action 이 호출되고 commit 된다."""
    session = MagicMock()
    session.exec.return_value = _exec_returns_first(None)  # 기존 북마크 없음

    with patch.object(services, "_update_expertise_on_action") as mock_update:
        result = services.create_bookmark(session, user_id=1, article_id=10)

    assert result.user_id == 1
    assert result.article_id == 10
    mock_update.assert_called_once_with(session, 1, 10, "bookmark")
    session.commit.assert_called_once()


# ── delete_bookmark ────────────────────────────────────────

def test_delete_bookmark_owner_match_deletes_and_returns_true():
    bookmark = Bookmark(id=5, user_id=1, article_id=10)
    session = MagicMock()
    session.exec.return_value = _exec_returns_first(bookmark)

    result = services.delete_bookmark(session, bookmark_id=5, user_id=1)

    assert result is True
    session.delete.assert_called_once_with(bookmark)
    session.commit.assert_called_once()


def test_delete_bookmark_owner_mismatch_returns_false():
    """다른 유저의 북마크는 삭제하지 않고 False 반환 (소유자 검증 회귀 방지)."""
    session = MagicMock()
    session.exec.return_value = _exec_returns_first(None)  # 쿼리에 user_id 조건 → 결과 없음

    result = services.delete_bookmark(session, bookmark_id=5, user_id=999)

    assert result is False
    session.delete.assert_not_called()
    session.commit.assert_not_called()
