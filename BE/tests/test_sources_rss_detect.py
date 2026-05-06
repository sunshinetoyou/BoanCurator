"""_detect_rss_feed 의 silent fail 회귀 방지 테스트.

배경:
  과거 sources._detect_rss_feed 는 HTML fetch/parse 단계의 모든 예외를
  `except: pass` 로 흡수하고 None 을 반환했다. 사용자에게는 "RSS 피드를
  찾을 수 없습니다" 로 응답되지만 실제 원인(timeout/DNS/4xx/파싱오류)이
  운영자/디버거에게 전혀 보이지 않았다.

  계약: HTML fetch/parse 예외 시 logger.warning 으로 원인을 남기고 None 을 반환.
"""
import os
import sys
from pathlib import Path
from unittest.mock import patch, MagicMock

os.environ.setdefault("DATABASE_URL", "postgresql+psycopg2://x:x@localhost:5432/x")

BE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BE_DIR))

import pytest
import requests

from api.v1.endpoints import sources


def _empty_feed():
    """feedparser.parse 가 RSS 가 아닌 URL 에 대해 반환하는 형태"""
    feed = MagicMock()
    feed.entries = []
    return feed


def test_detect_rss_logs_warning_on_connection_error(caplog):
    """requests.get 이 ConnectionError 일 때 warning 로그 + None 반환."""
    with patch.object(sources, "feedparser") as mock_feedparser, \
         patch.object(sources, "requests") as mock_requests:
        mock_feedparser.parse.return_value = _empty_feed()
        mock_requests.get.side_effect = requests.exceptions.ConnectionError("name resolution failed")

        with caplog.at_level("WARNING", logger="api.v1.endpoints.sources"):
            result = sources._detect_rss_feed("https://example.invalid")

    assert result is None
    assert any(
        "RSS 탐지" in record.message and "name resolution failed" in record.message
        for record in caplog.records
    ), f"expected warning log with cause, got: {[r.message for r in caplog.records]}"


def test_detect_rss_logs_warning_on_timeout(caplog):
    """Timeout 케이스도 로그가 남아야 한다."""
    with patch.object(sources, "feedparser") as mock_feedparser, \
         patch.object(sources, "requests") as mock_requests:
        mock_feedparser.parse.return_value = _empty_feed()
        mock_requests.get.side_effect = requests.exceptions.Timeout("read timeout")

        with caplog.at_level("WARNING", logger="api.v1.endpoints.sources"):
            result = sources._detect_rss_feed("https://slow.example")

    assert result is None
    assert any("read timeout" in record.message for record in caplog.records)


def test_detect_rss_returns_direct_feed_without_fetch():
    """직접 RSS 인 경우 HTML fetch 단계로 진입하지 않는다 (회귀 방지)."""
    feed = MagicMock()
    entry = MagicMock()
    entry.get = MagicMock(return_value="sample title")
    feed.entries = [entry]
    feed.feed.get = MagicMock(return_value="Test Feed")

    with patch.object(sources, "feedparser") as mock_feedparser, \
         patch.object(sources, "requests") as mock_requests:
        mock_feedparser.parse.return_value = feed

        result = sources._detect_rss_feed("https://example.com/feed.xml")

        assert result is not None
        assert result["feed_url"] == "https://example.com/feed.xml"
        mock_requests.get.assert_not_called()
