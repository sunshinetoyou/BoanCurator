import logging
import time
from sqlmodel import Session
from db import services, engine, init_db
from db.vector_store import store_embedding
from analyzers import GeminiAnalyzer

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


def run_analysis_bot():
    init_db()
    analyzer = GeminiAnalyzer()
    logger.info("Analysis Bot 가동 중 (Gemini API)...")

    while True:
        with Session(engine) as session:
            target_article = services.get_next_article_to_analyze(session)

            if not target_article:
                logger.debug("분석할 기사가 없습니다. 대기 중...")
                time.sleep(30)
                continue

            logger.info(f"분석 시작: {target_article.title}")
            try:
                analysis_data = analyzer.analyze(target_article)

                if analysis_data:
                    services.save_analysis(session, target_article.id, analysis_data)

                    # ChromaDB에 멀티모달 임베딩 저장
                    store_embedding(
                        article_id=target_article.id,
                        title=target_article.title,
                        summary=analysis_data.summary,
                        metadata={
                            "source": target_article.source,
                            "category": analysis_data.category,
                            "level": analysis_data.level,
                        },
                        image_urls=target_article.image_urls,
                    )
                    logger.info("분석 완료 및 저장 성공")
                else:
                    logger.warning("분석 결과가 비어있습니다")
            except Exception as e:
                logger.error(f"분석 실패: {e}")
                time.sleep(10)


if __name__ == "__main__":
    run_analysis_bot()
