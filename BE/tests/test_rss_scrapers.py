"""RSSGenericScraper 기반 소스들을 개별 테스트합니다."""
import sys
import os
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import db.services as svc
svc.is_article_exists = lambda session, url: False

from scrapers.rsser import RSSGenericScraper


RSS_SOURCES = [
    # RSS 전문 포함
    {"source_name": "Krebs on Security",     "url": "https://krebsonsecurity.com/feed/",          "has_full_content": True},
    {"source_name": "Schneier on Security",  "url": "https://www.schneier.com/feed/",             "has_full_content": True},
    {"source_name": "CISA Alerts",           "url": "https://www.cisa.gov/cybersecurity-advisories/all.xml", "has_full_content": True},
    {"source_name": "Mandiant",              "url": "https://feeds.feedburner.com/threatintelligence/pvexyqv7v0v", "has_full_content": True},
    # RSS 요약 + 스크래핑
    {"source_name": "BleepingComputer",      "url": "https://www.bleepingcomputer.com/feed/",     "content_selector": "div.articleBody"},
    {"source_name": "DailySecu",             "url": "https://www.dailysecu.com/rss/allArticle.xml", "content_selector": "div#article-view-content-div"},
    {"source_name": "AhnLab ASEC",           "url": "https://asec.ahnlab.com/ko/feed/",           "content_selector": "div.entry-content"},
    {"source_name": "Unit 42",               "url": "https://unit42.paloaltonetworks.com/feed/",   "content_selector": "div.be__contents"},
]


def test_one(cfg, max_articles=2):
    name = cfg["source_name"]
    print(f"\n{'='*60}")
    print(f"  {name}")
    print(f"{'='*60}")

    scraper = RSSGenericScraper(
        url=cfg["url"],
        period=3600,
        source_name=name,
        content_selector=cfg.get("content_selector"),
        has_full_content=cfg.get("has_full_content", False),
    )

    try:
        articles = scraper.collect(session=None)
    except Exception as e:
        print(f"  [FAIL] {e}")
        return name, "FAIL", str(e)

    if not articles:
        print(f"  [WARN] 0 articles")
        return name, "EMPTY", ""

    print(f"  [OK] {len(articles)} articles\n")
    for i, a in enumerate(articles[:max_articles]):
        img_count = len(a.image_paths) if a.image_paths else 0
        print(f"  [{i+1}] {a.title[:70]}")
        print(f"      content={len(a.content)} chars | images={img_count} | date={a.published_at}")
        if a.content:
            print(f"      preview: {a.content[:100]}...")
        print()

    return name, "OK", f"{len(articles)} articles"


if __name__ == "__main__":
    target = sys.argv[1].lower() if len(sys.argv) > 1 else None

    results = []
    for cfg in RSS_SOURCES:
        if target and target not in cfg["source_name"].lower():
            continue
        results.append(test_one(cfg))

    print(f"\n{'='*60}")
    print(f"  SUMMARY")
    print(f"{'='*60}")
    for name, status, detail in results:
        print(f"  {status:5s} | {name:25s} | {detail}")
