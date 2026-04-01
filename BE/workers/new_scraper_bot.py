import logging
import threading
import time
from sqlmodel import Session
from db import services, engine, init_db
from scrapers import get_all_scrapers

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


def scraper_thread(scraper):
    """각 스크래퍼별 독립 루프"""
    name = getattr(scraper, "source_name", scraper.__class__.__name__)
    while True:
        logger.info(f"{name} 스크래핑 시작...")
        with Session(engine) as session:
            try:
                scraped_items = scraper.collect(session)
                for item in scraped_items:
                    if not services.get_article_by_url(session, item.url):
                        services.save_article(session, item)
                        logger.info(f"새 기사 저장: {item.title[:30]}...")
            except Exception as e:
                logger.error(f"{name} 에러: {e}")

        time.sleep(scraper.period)


def run_scraper_bot():
    init_db()
    scrapers = get_all_scrapers()

    for s in scrapers:
        threading.Thread(target=scraper_thread, args=(s,), daemon=True).start()

    logger.info(f"Scraper Bot 가동 중... ({len(scrapers)}개 소스)")
    while True:
        time.sleep(1)


if __name__ == "__main__":
    run_scraper_bot()
