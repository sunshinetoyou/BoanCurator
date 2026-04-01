"""스크래퍼 중앙 레지스트리. Worker에서 이 목록을 참조하여 스크래퍼를 생성합니다."""

from .sser import BoanNewsScraper, GeekNewsScraper, S2WScraper
from .rsser import RSSGenericScraper


def get_all_scrapers():
    """활성 상태의 모든 스크래퍼 인스턴스를 반환합니다."""
    return [
        # ── 특화 스크래퍼 (sser) ──
        BoanNewsScraper(
            url="http://www.boannews.com/media/news_rss.xml?skind=1",
            period=10800,
        ),
        BoanNewsScraper(
            url="http://www.boannews.com/media/news_rss.xml?skind=2",
            period=10800,
        ),
        GeekNewsScraper(
            url="https://news.hada.io/rss/news",
            period=10800,
        ),
        S2WScraper(
            url="https://medium.com/feed/s2wblog",
            period=86400,
        ),

        # ── RSS 전문 포함 (rsser) ──
        RSSGenericScraper(
            url="https://krebsonsecurity.com/feed/",
            period=21600,
            source_name="Krebs on Security",
            has_full_content=True,
        ),
        RSSGenericScraper(
            url="https://www.schneier.com/feed/",
            period=21600,
            source_name="Schneier on Security",
            has_full_content=True,
        ),
        RSSGenericScraper(
            url="https://www.cisa.gov/cybersecurity-advisories/all.xml",
            period=43200,
            source_name="CISA Alerts",
            has_full_content=True,
        ),
        RSSGenericScraper(
            url="https://feeds.feedburner.com/threatintelligence/pvexyqv7v0v",
            period=43200,
            source_name="Mandiant",
            has_full_content=True,
        ),

        # ── RSS 요약 + 본문 스크래핑 (rsser) ──
        RSSGenericScraper(
            url="https://www.bleepingcomputer.com/feed/",
            period=10800,
            source_name="BleepingComputer",
            content_selector="div.articleBody",
        ),
        RSSGenericScraper(
            url="https://www.dailysecu.com/rss/allArticle.xml",
            period=10800,
            source_name="데일리시큐",
            content_selector="div#article-view-content-div",
        ),
        RSSGenericScraper(
            url="https://asec.ahnlab.com/ko/feed/",
            period=21600,
            source_name="AhnLab ASEC",
            content_selector="div.entry-content",
        ),
        RSSGenericScraper(
            url="https://unit42.paloaltonetworks.com/feed/",
            period=43200,
            source_name="Unit 42",
            content_selector="div.be__contents",
        ),
    ]
