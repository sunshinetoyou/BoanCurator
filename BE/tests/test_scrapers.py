"""각 스크래퍼의 RSS 파싱 + 본문 스크래핑을 DB 없이 테스트합니다."""
import sys
import os
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import db.services as svc
svc.is_article_exists = lambda session, url: False

from scrapers.sser import BoanNewsScraper, GeekNewsScraper, S2WScraper
from scrapers.rsser import RSSGenericScraper


def test_scraper(scraper, max_articles=3):
    name = getattr(scraper, 'source_name', scraper.__class__.__name__)
    print(f"\n{'='*60}")
    print(f"  {name}")
    print(f"{'='*60}")

    try:
        articles = scraper.collect(session=None)
    except Exception as e:
        print(f"  [FAIL] collect error: {e}")
        return

    if not articles:
        print(f"  [WARN] 0 articles collected")
        return

    print(f"  [OK] {len(articles)} articles (showing top {min(max_articles, len(articles))})\n")

    for i, a in enumerate(articles[:max_articles]):
        img_count = len(a.image_paths) if a.image_paths else 0
        print(f"  [{i+1}] {a.title[:60]}")
        print(f"      source={a.source} | content={len(a.content)} chars | images={img_count}")
        if a.image_paths:
            for p in a.image_paths[:3]:
                print(f"        IMG: {p}")
        print(f"      published_at={a.published_at}")
        print()


if __name__ == "__main__":
    scrapers = [
        ("BoanNews", BoanNewsScraper("http://www.boannews.com/media/news_rss.xml?skind=1", 3600)),
        ("GeekNews", GeekNewsScraper("https://news.hada.io/rss/news", 3600)),
        ("S2W", S2WScraper("https://medium.com/feed/s2wblog", 86400)),
        ("Krebs", RSSGenericScraper(
            url="https://krebsonsecurity.com/feed/",
            period=21600,
            source_name="Krebs on Security",
            has_full_content=True,
        )),
    ]

    target = sys.argv[1] if len(sys.argv) > 1 else None

    for name, scraper in scrapers:
        if target and target.lower() not in name.lower():
            continue
        test_scraper(scraper)
