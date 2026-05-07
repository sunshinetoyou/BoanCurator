"""services.py 트랜잭션 함수 단위 테스트.

대상:
  - save_article: 정상 / commit 실패 시 rollback + raise
  - record_analysis_failure: 첫 실패 / 재시도 increment / 에러 메시지 500자 truncate
  - create_bookmark: 기존 존재 시 재사용 / 새 생성 시 expertise 업데이트
  - delete_bookmark: 소유자 일치 / 불일치
  - get_or_create_user_by_google: 신규 생성 / 기존 갱신 / profile_image None 보존
  - _update_expertise_on_action: user/analysis/domain_scores 누락 시 silent return / 정상 갱신
  - rate_article: 신규 / 덮어쓰기 / 평가 부가 효과(Elo + expertise) 분기

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
from db.models import (
    Analysis,
    AnalysisFailure,
    Article,
    ArticleRating,
    Bookmark,
    User,
)


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

    with patch.object(services.users, "_update_expertise_on_action") as mock_update:
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


# ── get_or_create_user_by_google ────────────────────────────────────────

def test_get_or_create_user_creates_new_when_not_found():
    """google_id 로 기존 유저가 없으면 새 User 를 만들고 add/commit/refresh."""
    session = MagicMock()
    session.exec.return_value = _exec_returns_first(None)

    result = services.get_or_create_user_by_google(
        session,
        google_id="g1",
        email="a@x.com",
        username="alice",
        profile_image="http://img/a.png",
    )

    assert result.google_id == "g1"
    assert result.email == "a@x.com"
    assert result.username == "alice"
    assert result.profile_image == "http://img/a.png"
    session.add.assert_called_once()
    session.commit.assert_called_once()
    session.refresh.assert_called_once()


def test_get_or_create_user_updates_existing_username_and_email():
    """기존 유저 발견 시 username/email 을 새 값으로 갱신하고 commit."""
    existing = User(id=1, google_id="g1", email="old@x.com", username="old")
    session = MagicMock()
    session.exec.return_value = _exec_returns_first(existing)

    result = services.get_or_create_user_by_google(
        session,
        google_id="g1",
        email="new@x.com",
        username="new",
    )

    assert result is existing
    assert result.email == "new@x.com"
    assert result.username == "new"
    session.commit.assert_called_once()


def test_get_or_create_user_preserves_profile_image_when_none_passed():
    """profile_image=None 이면 기존 값 유지 (회귀 방지: 빈 OAuth 응답이 프로필을 지우면 안 됨)."""
    existing = User(
        id=1, google_id="g1", email="a@x.com", username="alice",
        profile_image="http://img/old.png",
    )
    session = MagicMock()
    session.exec.return_value = _exec_returns_first(existing)

    result = services.get_or_create_user_by_google(
        session, google_id="g1", email="a@x.com", username="alice",
        profile_image=None,
    )

    assert result.profile_image == "http://img/old.png"


def test_get_or_create_user_overwrites_profile_image_when_passed():
    """profile_image 가 truthy 면 기존 값을 새 값으로 갱신."""
    existing = User(
        id=1, google_id="g1", email="a@x.com", username="alice",
        profile_image="http://img/old.png",
    )
    session = MagicMock()
    session.exec.return_value = _exec_returns_first(existing)

    result = services.get_or_create_user_by_google(
        session, google_id="g1", email="a@x.com", username="alice",
        profile_image="http://img/new.png",
    )

    assert result.profile_image == "http://img/new.png"


# ── _update_expertise_on_action ────────────────────────────────────────

def _basic_user() -> User:
    return User(id=1, google_id="g", email="a@x.com", username="u")


def test_update_expertise_returns_silently_when_user_missing():
    """session.get(User) → None 이면 분석 조회 결과와 무관하게 즉시 종료, add 안 함."""
    session = MagicMock()
    session.get.return_value = None
    session.exec.return_value = _exec_returns_first(None)

    services._update_expertise_on_action(session, user_id=1, article_id=10, action="bookmark")

    session.add.assert_not_called()


def test_update_expertise_returns_silently_when_analysis_missing():
    """analysis 가 없으면 silent return — 외부에서 user expertise 가 변하지 않아야 한다."""
    session = MagicMock()
    session.get.return_value = _basic_user()
    session.exec.return_value = _exec_returns_first(None)

    services._update_expertise_on_action(session, user_id=1, article_id=10, action="bookmark")

    session.add.assert_not_called()


def test_update_expertise_returns_silently_when_domain_scores_empty():
    """analysis.domain_scores 가 None/빈값이면 silent return."""
    session = MagicMock()
    session.get.return_value = _basic_user()
    analysis = MagicMock()
    analysis.domain_scores = None
    session.exec.return_value = _exec_returns_first(analysis)

    services._update_expertise_on_action(session, user_id=1, article_id=10, action="bookmark")

    session.add.assert_not_called()


def test_update_expertise_calls_update_user_expertise_and_session_add():
    """정상 케이스: update_user_expertise(현 expertise, domain_scores, action) 호출 + user 갱신 후 add."""
    user = _basic_user()
    user.expertise = {"network_infra": 2}
    analysis = MagicMock()
    analysis.domain_scores = {"network_infra": 3}
    session = MagicMock()
    session.get.return_value = user
    session.exec.return_value = _exec_returns_first(analysis)

    new_expertise = {"network_infra": 2.5}
    with patch.object(services.users, "update_user_expertise", return_value=new_expertise) as mock_upd:
        services._update_expertise_on_action(session, user_id=1, article_id=10, action="bookmark")

    mock_upd.assert_called_once_with({"network_infra": 2}, {"network_infra": 3}, "bookmark")
    assert user.expertise == new_expertise
    session.add.assert_called_once_with(user)
    # commit 은 caller (create_bookmark 등) 책임이므로 여기서 호출되면 안 된다
    session.commit.assert_not_called()


# ── rate_article ────────────────────────────────────────

def _full_user() -> User:
    user = _basic_user()
    user.expertise = {"network_infra": 2}
    user.level_preference = 3.0
    return user


def _exec_side_effect_for_rate(rating_value, analysis_value):
    """rate_article 은 session.exec 를 두 번 호출 (ArticleRating → Analysis)."""
    return [_exec_returns_first(rating_value), _exec_returns_first(analysis_value)]


def test_rate_article_creates_new_when_no_existing():
    """기존 ArticleRating 이 없으면 새 객체 생성 후 add + commit."""
    session = MagicMock()
    session.exec.side_effect = _exec_side_effect_for_rate(None, None)
    session.get.return_value = None  # user 없음 → 부가 효과 스킵

    result = services.rate_article(session, user_id=1, article_id=10, rating=1)

    assert isinstance(result, ArticleRating)
    assert result.user_id == 1
    assert result.article_id == 10
    assert result.rating == 1
    session.add.assert_called_once_with(result)
    session.commit.assert_called_once()
    session.refresh.assert_called_once_with(result)


def test_rate_article_overwrites_existing_rating():
    """기존 평가가 있으면 새 객체 생성 없이 rating 만 덮어쓴다 (회귀 방지)."""
    existing = ArticleRating(id=5, user_id=1, article_id=10, rating=-1)
    session = MagicMock()
    session.exec.side_effect = _exec_side_effect_for_rate(existing, None)
    session.get.return_value = None

    result = services.rate_article(session, user_id=1, article_id=10, rating=1)

    assert result is existing
    assert result.rating == 1
    session.add.assert_called_once_with(existing)
    session.commit.assert_called_once()


def test_rate_article_skips_user_updates_when_user_missing():
    """user 없으면 Elo/expertise 헬퍼는 호출되지 않고 평가만 저장."""
    analysis = MagicMock()
    analysis.domain_scores = {"network_infra": 3}
    session = MagicMock()
    session.exec.side_effect = _exec_side_effect_for_rate(None, analysis)
    session.get.return_value = None

    with patch.object(services.ratings, "update_level_preference_elo") as elo, \
         patch.object(services.ratings, "update_expertise_on_rating") as exp:
        services.rate_article(session, user_id=1, article_id=10, rating=1)

    elo.assert_not_called()
    exp.assert_not_called()
    session.commit.assert_called_once()


def test_rate_article_skips_user_updates_when_analysis_missing():
    """analysis 없으면 부가 업데이트 스킵."""
    session = MagicMock()
    session.exec.side_effect = _exec_side_effect_for_rate(None, None)
    session.get.return_value = _full_user()

    with patch.object(services.ratings, "update_level_preference_elo") as elo, \
         patch.object(services.ratings, "update_expertise_on_rating") as exp:
        services.rate_article(session, user_id=1, article_id=10, rating=1)

    elo.assert_not_called()
    exp.assert_not_called()


def test_rate_article_skips_user_updates_when_domain_scores_empty():
    """analysis.domain_scores 가 None 이면 부가 업데이트 스킵."""
    analysis = MagicMock()
    analysis.domain_scores = None
    session = MagicMock()
    session.exec.side_effect = _exec_side_effect_for_rate(None, analysis)
    session.get.return_value = _full_user()

    with patch.object(services.ratings, "update_level_preference_elo") as elo, \
         patch.object(services.ratings, "update_expertise_on_rating") as exp:
        services.rate_article(session, user_id=1, article_id=10, rating=1)

    elo.assert_not_called()
    exp.assert_not_called()


def test_rate_article_liked_true_when_rating_is_1():
    """rating=1 → liked=True 가 Elo + expertise 헬퍼에 전달."""
    user = _full_user()
    analysis = MagicMock()
    analysis.domain_scores = {"network_infra": 3}
    analysis.level = "Medium"
    session = MagicMock()
    session.exec.side_effect = _exec_side_effect_for_rate(None, analysis)
    session.get.return_value = user

    with patch.object(services.ratings, "update_level_preference_elo", return_value=3.2) as elo, \
         patch.object(services.ratings, "calculate_relative_difficulty", return_value="Medium"), \
         patch.object(services.ratings, "update_expertise_on_rating", return_value=user.expertise) as exp:
        services.rate_article(session, user_id=1, article_id=10, rating=1)

    elo.assert_called_once_with(3.0, "Medium", True)
    assert exp.call_args.args[3] is True


def test_rate_article_liked_false_when_rating_is_negative_1():
    """rating=-1 → liked=False 가 Elo + expertise 헬퍼에 전달."""
    user = _full_user()
    analysis = MagicMock()
    analysis.domain_scores = {"network_infra": 3}
    analysis.level = "Medium"
    session = MagicMock()
    session.exec.side_effect = _exec_side_effect_for_rate(None, analysis)
    session.get.return_value = user

    with patch.object(services.ratings, "update_level_preference_elo", return_value=2.8) as elo, \
         patch.object(services.ratings, "calculate_relative_difficulty", return_value="Easy"), \
         patch.object(services.ratings, "update_expertise_on_rating", return_value=user.expertise) as exp:
        services.rate_article(session, user_id=1, article_id=10, rating=-1)

    elo.assert_called_once_with(3.0, "Medium", False)
    assert exp.call_args.args[3] is False


def test_rate_article_full_flow_updates_user_state():
    """정상 케이스: level_preference + expertise 가 헬퍼 반환값으로 갱신되고 user 가 add."""
    user = _full_user()
    analysis = MagicMock()
    analysis.domain_scores = {"network_infra": 3}
    analysis.level = "Hard"
    session = MagicMock()
    session.exec.side_effect = _exec_side_effect_for_rate(None, analysis)
    session.get.return_value = user

    new_expertise = {"network_infra": 2.6}
    with patch.object(services.ratings, "update_level_preference_elo", return_value=3.5), \
         patch.object(services.ratings, "calculate_relative_difficulty", return_value="Medium") as rel, \
         patch.object(services.ratings, "update_expertise_on_rating", return_value=new_expertise):
        services.rate_article(session, user_id=1, article_id=10, rating=1)

    assert user.level_preference == 3.5
    assert user.expertise == new_expertise
    # calculate_relative_difficulty 는 갱신된 level_preference(3.5) 를 받아야 한다
    rel.assert_called_once_with("Hard", {"network_infra": 3}, {"network_infra": 2}, 3.5)
    # add 호출: 신규 ArticleRating + user → 총 2번
    assert session.add.call_count == 2
    session.commit.assert_called_once()
