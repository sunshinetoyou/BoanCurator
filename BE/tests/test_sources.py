"""커스텀 소스 등록 + RSS 탐지 테스트"""
import sys
import os
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import feedparser
import requests
from bs4 import BeautifulSoup


def test_rss_auto_detect_direct_feed():
    """RSS 피드 URL을 직접 입력하면 feedparser로 파싱 가능"""
    url = "https://krebsonsecurity.com/feed/"
    feed = feedparser.parse(url)
    assert feed.entries, f"RSS 피드에서 기사를 찾을 수 없음: {url}"
    print(f"[1] RSS 직접 파싱 OK: {len(feed.entries)}건 ({feed.feed.get('title', 'N/A')})")


def test_rss_auto_detect_from_html():
    """일반 웹사이트 URL에서 RSS 링크 자동 탐지"""
    url = "https://krebsonsecurity.com"
    resp = requests.get(url, timeout=10, headers={
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    })
    soup = BeautifulSoup(resp.text, "html.parser")

    rss_links = soup.find_all("link", attrs={"type": ["application/rss+xml", "application/atom+xml"]})
    assert rss_links, f"RSS 링크를 찾을 수 없음: {url}"

    rss_url = rss_links[0].get("href", "")
    print(f"[2] RSS 자동 탐지 OK: {rss_url}")

    feed = feedparser.parse(rss_url)
    assert feed.entries, "탐지된 RSS 피드에서 기사를 찾을 수 없음"
    print(f"[2] 탐지된 RSS 파싱 OK: {len(feed.entries)}건")


def test_rss_detect_no_feed():
    """RSS 피드가 없는 사이트는 빈 결과"""
    url = "https://www.google.com"
    resp = requests.get(url, timeout=10, headers={
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    })
    soup = BeautifulSoup(resp.text, "html.parser")
    rss_links = soup.find_all("link", attrs={"type": ["application/rss+xml", "application/atom+xml"]})
    assert not rss_links, "Google에서 RSS 링크가 발견됨 (예상 외)"
    print("[3] RSS 미탐지 OK: RSS 피드 없는 사이트 정상 처리")


def test_rss_sample_articles():
    """RSS 피드에서 샘플 기사 추출 (title, link, published)"""
    url = "https://www.schneier.com/feed/"
    feed = feedparser.parse(url)
    assert feed.entries, "Schneier 피드에서 기사를 찾을 수 없음"

    sample = feed.entries[0]
    assert sample.get("title"), "기사 제목 없음"
    assert sample.get("link"), "기사 링크 없음"
    print(f"[4] 샘플 기사 OK: '{sample.title}' → {sample.link}")


if __name__ == "__main__":
    tests = [
        test_rss_auto_detect_direct_feed,
        test_rss_auto_detect_from_html,
        test_rss_detect_no_feed,
        test_rss_sample_articles,
    ]
    passed = 0
    for test in tests:
        try:
            test()
            passed += 1
        except Exception as e:
            print(f"FAIL {test.__name__}: {e}")
    print(f"\n결과: {passed}/{len(tests)} 통과")
