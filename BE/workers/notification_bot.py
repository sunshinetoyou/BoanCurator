import logging
import time
from datetime import datetime, timedelta

from sqlmodel import Session, select, func
from db import engine, init_db
from db.models import (
    Analysis, Article, KeywordAlert, NotificationLog,
    UserNotificationSettings,
)
from db.vector_store import match_article_to_keywords
from utils.fcm import send_fcm_notification

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

MATCH_PRESETS = {"strict": 0.4, "normal": 0.7, "loose": 1.0}


def _get_user_settings(session: Session, user_id: int) -> dict:
    """유저 알림 설정 조회 (없으면 기본값)"""
    s = session.exec(
        select(UserNotificationSettings).where(
            UserNotificationSettings.user_id == user_id
        )
    ).first()
    if not s:
        return {
            "match_preset": "normal",
            "top_n": 3,
            "daily_limit": 5,
            "mode": "realtime",
            "fcm_token": None,
        }
    return {
        "match_preset": s.match_preset,
        "top_n": s.top_n,
        "daily_limit": s.daily_limit,
        "mode": s.mode,
        "fcm_token": s.fcm_token,
    }


def _get_today_notification_count(session: Session, user_id: int) -> int:
    """오늘 발송된 알림 수"""
    today_start = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
    return session.exec(
        select(func.count()).where(
            NotificationLog.user_id == user_id,
            NotificationLog.sent_at >= today_start,
        )
    ).one()


def _is_duplicate(session: Session, user_id: int, article_id: int, keyword_alert_id: int) -> bool:
    """이미 발송된 알림인지 확인"""
    return session.exec(
        select(NotificationLog).where(
            NotificationLog.user_id == user_id,
            NotificationLog.article_id == article_id,
            NotificationLog.keyword_alert_id == keyword_alert_id,
        )
    ).first() is not None


def _process_article(session: Session, article: Article, analysis: Analysis):
    """새 기사에 대해 모든 유저의 키워드 매칭 + 알림 발송"""
    # 키워드 등록된 유저 목록
    user_ids = session.exec(
        select(KeywordAlert.user_id).distinct()
    ).all()

    article_text = f"{article.title} {analysis.summary}"

    for user_id in user_ids:
        settings = _get_user_settings(session, user_id)

        if settings["mode"] != "realtime":
            continue
        if not settings["fcm_token"]:
            continue

        # 일일 한도 체크
        today_count = _get_today_notification_count(session, user_id)
        if today_count >= settings["daily_limit"]:
            continue

        # 키워드 매칭
        threshold = MATCH_PRESETS.get(settings["match_preset"], 0.7)
        matches = match_article_to_keywords(
            query_text=article_text,
            user_id=user_id,
            top_n=settings["top_n"],
        )

        for match in matches:
            if match["distance"] >= threshold:
                continue

            # 키워드 ID 추출
            kw_id_str = match["keyword_id"].replace("kw_", "")
            try:
                keyword_alert_id = int(kw_id_str)
            except ValueError:
                continue

            # 중복 체크
            if _is_duplicate(session, user_id, article.id, keyword_alert_id):
                continue

            # FCM 발송
            sent = send_fcm_notification(
                token=settings["fcm_token"],
                title=f"'{match['keyword']}' 관련 새 기사",
                body=article.title[:100],
                data={"article_id": str(article.id)},
            )

            # 로그 기록 (발송 성공 여부와 관계없이)
            log = NotificationLog(
                user_id=user_id,
                article_id=article.id,
                keyword_alert_id=keyword_alert_id,
            )
            session.add(log)

            # 일일 한도 재체크
            today_count += 1
            if today_count >= settings["daily_limit"]:
                break

    session.commit()


def run_notification_bot():
    init_db()
    logger.info("Notification Bot 가동 중...")

    last_check_id = 0

    while True:
        with Session(engine) as session:
            # 마지막 체크 이후 새로 분석된 기사 조회
            new_analyses = session.exec(
                select(Analysis)
                .where(Analysis.id > last_check_id)
                .order_by(Analysis.id.asc())
            ).all()

            for analysis in new_analyses:
                article = session.exec(
                    select(Article).where(Article.id == analysis.article_id)
                ).first()
                if article:
                    try:
                        _process_article(session, article, analysis)
                    except Exception as e:
                        logger.error(f"알림 처리 실패 (article_id={analysis.article_id}): {e}")

                last_check_id = analysis.id

        time.sleep(60)  # 1분마다 체크


if __name__ == "__main__":
    run_notification_bot()
