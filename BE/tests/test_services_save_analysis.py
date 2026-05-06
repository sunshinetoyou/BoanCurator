"""save_analysis 트랜잭션 계약 회귀 테스트.

배경:
  workers/analysis_bot.process_one_article 은 save_analysis 호출을
  try/except 로 감싸고, except 블록에서 record_analysis_failure를 호출해
  AnalysisFailure.attempt_count 를 증가시킨다.

  과거 save_analysis 가 commit 예외를 swallow 하고 None 을 반환했기 때문에
  caller 의 except 가 발동하지 않았고, 결과적으로 attempt_count 가 증가하지
  않아 같은 기사가 영원히 재시도되는 무한 루프가 발생할 수 있었다.

  계약: save_analysis 는 commit 실패 시 rollback 후 예외를 그대로 raise 해야 한다.
"""
import os
import sys
from pathlib import Path
from unittest.mock import MagicMock

# config.Settings 가 import 시점에 .env 를 읽으므로 단위 테스트에서도 환경 변수 보장
os.environ.setdefault("DATABASE_URL", "postgresql+psycopg2://x:x@localhost:5432/x")

BE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BE_DIR))

import pytest

from db import services
from db.models import AnalysisData, Category, Level


def _valid_analysis_data() -> AnalysisData:
    return AnalysisData(
        category=Category.TECH,
        themes=["Security"],
        summary="ok",
        level=Level.Low,
        domain_scores={
            "network_infra": 1, "malware_vuln": 0, "cloud_devsecops": 0,
            "crypto_auth": 0, "policy_compliance": 0, "general_it": 2,
        },
        prompt_version="test",
        model="fake",
    )


def test_save_analysis_raises_on_commit_failure():
    """commit 실패 시 예외를 raise 하고 rollback 해야 한다 (silent fail 금지)."""
    session = MagicMock()
    session.commit.side_effect = RuntimeError("DB unavailable")

    with pytest.raises(RuntimeError, match="DB unavailable"):
        services.save_analysis(session, article_id=1, analysis_data=_valid_analysis_data())

    session.rollback.assert_called_once()


def test_save_analysis_success_returns_analysis():
    """정상 케이스: add → commit → refresh 호출되고 article_id 가 채워진 객체를 반환."""
    session = MagicMock()

    result = services.save_analysis(session, article_id=42, analysis_data=_valid_analysis_data())

    assert result is not None
    assert result.article_id == 42
    session.add.assert_called_once()
    session.commit.assert_called_once()
    session.refresh.assert_called_once()


def test_save_analysis_raises_on_validation_failure():
    """model_validate 단계에서 예외가 나도 raise 해야 한다 (rollback은 commit 전이라 호출 안 됨)."""
    session = MagicMock()
    # model_validate 가 실패하도록 부적절한 article_id 타입을 강제하기는 어려우니
    # session.add 가 raise 하는 케이스로 검증 (add 시점도 commit 전 단계임)
    session.add.side_effect = RuntimeError("integrity error")

    with pytest.raises(RuntimeError, match="integrity error"):
        services.save_analysis(session, article_id=1, analysis_data=_valid_analysis_data())

    session.rollback.assert_called_once()
