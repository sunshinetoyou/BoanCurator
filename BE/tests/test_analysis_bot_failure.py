"""Analysis worker 무한 루프 재발 방지 회귀 테스트.

검증:
1. 분석기가 None을 반환하면 AnalysisFailure row가 생성된다.
2. 같은 기사에 대한 반복 실패는 attempt_count를 증가시킨다.
3. attempt_count가 MAX_ANALYSIS_ATTEMPTS에 도달하면 그 기사는 더 이상 뽑히지 않는다.
4. 분석기가 예외를 던져도 동일하게 실패로 기록된다.
5. 성공 케이스는 Analysis만 저장되고 AnalysisFailure는 생기지 않는다.
6. 임베딩 저장 실패는 실패로 치지 않는다.
"""
import os
import sys
from datetime import datetime, timedelta
from pathlib import Path

# 환경 설정: config.Settings가 import 시점에 읽음
os.environ["DATABASE_URL"] = "postgresql+psycopg2://boan:boan@localhost:5432/boan_test"

BE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BE_DIR))

import pytest
from sqlmodel import Session, SQLModel, select

from db import engine
from db.models import Article, Analysis, AnalysisFailure, AnalysisData, Category, Level
from db import services
from workers.analysis_bot import process_one_article


@pytest.fixture(scope="module", autouse=True)
def setup_schema():
    # 이 테스트용으로 모든 테이블 재생성
    SQLModel.metadata.drop_all(engine)
    SQLModel.metadata.create_all(engine)
    yield
    SQLModel.metadata.drop_all(engine)


@pytest.fixture
def clean_session():
    with Session(engine) as session:
        # 테스트 간 격리: Article/Analysis/AnalysisFailure 비우기
        session.exec(AnalysisFailure.__table__.delete())
        session.exec(Analysis.__table__.delete())
        session.exec(Article.__table__.delete())
        session.commit()
        yield session


def _make_article(session, title: str, offset_minutes: int = 0) -> Article:
    art = Article(
        title=title,
        url=f"https://example.com/{title}",
        source="test",
        content="lorem ipsum " * 20,
        published_at=datetime.now() - timedelta(minutes=offset_minutes),
    )
    session.add(art)
    session.commit()
    session.refresh(art)
    return art


class NoneAnalyzer:
    def analyze(self, article):
        return None


class RaisingAnalyzer:
    def analyze(self, article):
        raise RuntimeError("boom")


class GoodAnalyzer:
    def analyze(self, article):
        return AnalysisData(
            category=Category.TECH,
            themes=["Security"],
            summary="ok",
            level=Level.Low,
            domain_scores={"network_infra": 1, "malware_vuln": 0, "cloud_devsecops": 0,
                           "crypto_auth": 0, "policy_compliance": 0, "general_it": 2},
            prompt_version="test",
            model="fake",
        )


def _noop_embed(**kwargs):
    pass


def _failing_embed(**kwargs):
    raise RuntimeError("chroma down")


def test_none_result_records_failure(clean_session):
    art = _make_article(clean_session, "a1")
    status = process_one_article(clean_session, NoneAnalyzer(), _noop_embed)
    assert status == "failed"

    failure = clean_session.exec(
        select(AnalysisFailure).where(AnalysisFailure.article_id == art.id)
    ).first()
    assert failure is not None
    assert failure.attempt_count == 1
    assert "None" in (failure.last_error or "")


def test_repeated_failures_increment_attempt_count(clean_session):
    art = _make_article(clean_session, "a2")
    process_one_article(clean_session, NoneAnalyzer(), _noop_embed)
    process_one_article(clean_session, NoneAnalyzer(), _noop_embed)

    failure = clean_session.exec(
        select(AnalysisFailure).where(AnalysisFailure.article_id == art.id)
    ).first()
    assert failure.attempt_count == 2


def test_max_attempts_skips_article(clean_session):
    """MAX_ANALYSIS_ATTEMPTS 도달 후에는 같은 기사를 다시 뽑지 않고
    다음 기사로 넘어가야 한다."""
    a1 = _make_article(clean_session, "stuck", offset_minutes=10)   # 더 오래됨 = 우선순위 높음
    a2 = _make_article(clean_session, "next", offset_minutes=5)

    # MAX=2 이므로 2번 실패시키면 a1은 스킵되어야 함
    assert process_one_article(clean_session, NoneAnalyzer(), _noop_embed) == "failed"
    assert process_one_article(clean_session, NoneAnalyzer(), _noop_embed) == "failed"

    # 이제 a1은 스킵되고 a2가 뽑혀야 함
    next_target = services.get_next_article_to_analyze(clean_session)
    assert next_target is not None
    assert next_target.id == a2.id, f"expected a2, got {next_target.title}"

    # 세 번째 호출은 a2를 처리 (성공 케이스)
    status = process_one_article(clean_session, GoodAnalyzer(), _noop_embed)
    assert status == "success"

    # a1의 attempt_count는 2에서 멈춰야 함
    failure = clean_session.exec(
        select(AnalysisFailure).where(AnalysisFailure.article_id == a1.id)
    ).first()
    assert failure.attempt_count == 2


def test_exception_also_records_failure(clean_session):
    art = _make_article(clean_session, "a3")
    status = process_one_article(clean_session, RaisingAnalyzer(), _noop_embed)
    assert status == "failed"

    failure = clean_session.exec(
        select(AnalysisFailure).where(AnalysisFailure.article_id == art.id)
    ).first()
    assert failure is not None
    assert failure.attempt_count == 1
    assert "boom" in (failure.last_error or "")


def test_success_path_creates_analysis_only(clean_session):
    art = _make_article(clean_session, "ok")
    status = process_one_article(clean_session, GoodAnalyzer(), _noop_embed)
    assert status == "success"

    analysis = clean_session.exec(
        select(Analysis).where(Analysis.article_id == art.id)
    ).first()
    assert analysis is not None
    assert analysis.summary == "ok"

    failure = clean_session.exec(
        select(AnalysisFailure).where(AnalysisFailure.article_id == art.id)
    ).first()
    assert failure is None


def test_embedding_failure_does_not_count_as_failure(clean_session):
    art = _make_article(clean_session, "embed_fail")
    status = process_one_article(clean_session, GoodAnalyzer(), _failing_embed)
    assert status == "success"

    # Analysis는 저장되어야 하고
    analysis = clean_session.exec(
        select(Analysis).where(Analysis.article_id == art.id)
    ).first()
    assert analysis is not None

    # Failure는 생기지 않아야 함
    failure = clean_session.exec(
        select(AnalysisFailure).where(AnalysisFailure.article_id == art.id)
    ).first()
    assert failure is None


def test_idle_when_no_articles(clean_session):
    status = process_one_article(clean_session, NoneAnalyzer(), _noop_embed)
    assert status == "idle"
