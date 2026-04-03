import logging
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime

from sqlmodel import Session, select
from db import services, engine, init_db
from db.models import CustomSource
from scrapers.registry import get_system_scrapers, get_custom_scrapers

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


def scraper_thread(scraper, custom_source_id=None):
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

                # 커스텀 소스: last_scraped_at 업데이트, 에러 초기화
                if custom_source_id:
                    src = session.get(CustomSource, custom_source_id)
                    if src:
                        src.last_scraped_at = datetime.now()
                        src.last_error = None
                        session.add(src)
                        session.commit()

            except Exception as e:
                logger.error(f"{name} 에러: {e}")
                # 커스텀 소스: 에러 기록
                if custom_source_id:
                    with Session(engine) as err_session:
                        src = err_session.get(CustomSource, custom_source_id)
                        if src:
                            src.last_error = str(e)[:500]
                            err_session.add(src)
                            err_session.commit()

        time.sleep(scraper.period)


def _run_custom_scrapers(pool: ThreadPoolExecutor):
    """DB에서 커스텀 소스를 주기적으로 로드하고 실행"""
    active_urls = set()  # 이미 실행 중인 URL 추적

    while True:
        with Session(engine) as session:
            custom_scrapers = get_custom_scrapers(session)
            sources = session.exec(
                select(CustomSource).where(CustomSource.enabled == True)
            ).all()
            source_map = {s.url: s.id for s in sources}

        for scraper in custom_scrapers:
            if scraper.url not in active_urls:
                active_urls.add(scraper.url)
                source_id = source_map.get(scraper.url)
                pool.submit(scraper_thread, scraper, source_id)
                name = getattr(scraper, "source_name", scraper.url)
                logger.info(f"커스텀 소스 시작: {name}")

        time.sleep(300)  # 5분마다 새 소스 체크


def run_scraper_bot():
    init_db()

    # 시스템 스크래퍼: 상시 쓰레드
    system_scrapers = get_system_scrapers()
    for s in system_scrapers:
        threading.Thread(target=scraper_thread, args=(s,), daemon=True).start()
    logger.info(f"시스템 스크래퍼 가동: {len(system_scrapers)}개")

    # 커스텀 스크래퍼: ThreadPoolExecutor
    pool = ThreadPoolExecutor(max_workers=20)
    threading.Thread(target=_run_custom_scrapers, args=(pool,), daemon=True).start()
    logger.info("커스텀 소스 매니저 가동")

    while True:
        time.sleep(1)


if __name__ == "__main__":
    run_scraper_bot()
