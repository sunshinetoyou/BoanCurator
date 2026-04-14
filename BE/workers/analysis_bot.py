import logging
import time
from sqlmodel import Session
from db import services, engine, init_db

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


IDLE_SLEEP_SECONDS = 30
FAILURE_SLEEP_SECONDS = 30


def process_one_article(session: Session, analyzer, store_embedding_fn) -> str:
    """한 기사를 분석하고 상태를 전이시킨다.

    반환값:
      - "idle"    : 분석할 기사가 없음
      - "success" : 분석 저장 완료
      - "failed"  : 실패 기록됨 (AnalysisFailure 증가)
    """
    target = services.get_next_article_to_analyze(session)
    if not target:
        return "idle"

    logger.info(f"분석 시작: {target.title}")
    try:
        analysis_data = analyzer.analyze(target)
    except Exception as e:
        logger.error(f"분석기 예외: {e}")
        services.record_analysis_failure(session, target.id, f"analyzer exception: {e}")
        return "failed"

    if not analysis_data:
        logger.warning(f"분석 결과가 비어있습니다 (article_id={target.id})")
        services.record_analysis_failure(session, target.id, "analyzer returned None")
        return "failed"

    try:
        services.save_analysis(session, target.id, analysis_data)
    except Exception as e:
        logger.error(f"save_analysis 실패: {e}")
        services.record_analysis_failure(session, target.id, f"save_analysis exception: {e}")
        return "failed"

    # 임베딩 저장은 실패로 치지 않음 (분석은 이미 성공)
    try:
        primary_domain = max(
            analysis_data.domain_scores,
            key=analysis_data.domain_scores.get,
        ) if analysis_data.domain_scores else "general_it"

        store_embedding_fn(
            article_id=target.id,
            title=target.title,
            summary=analysis_data.summary,
            metadata={
                "source": target.source,
                "category": analysis_data.category,
                "level": analysis_data.level,
                "primary_domain": primary_domain,
            },
            image_urls=target.image_urls,
        )
    except Exception as e:
        logger.warning(f"임베딩 저장 실패 (분석은 유지): {e}")

    logger.info("분석 완료 및 저장 성공")
    return "success"


def run_analysis_bot():
    # 지연 import: 테스트 환경에서 chromadb/analyzer 의존성을 피하기 위해
    from db.vector_store import store_embedding
    from analyzers import create_analyzer

    init_db()
    analyzer = create_analyzer()
    logger.info(f"Analysis Bot 가동 중 ({type(analyzer).__name__})...")

    while True:
        with Session(engine) as session:
            status = process_one_article(session, analyzer, store_embedding)

        if status == "idle":
            time.sleep(IDLE_SLEEP_SECONDS)
        elif status == "failed":
            time.sleep(FAILURE_SLEEP_SECONDS)


if __name__ == "__main__":
    run_analysis_bot()
