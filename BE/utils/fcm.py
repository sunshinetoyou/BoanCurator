import logging
import requests
from config import settings

logger = logging.getLogger(__name__)


def send_fcm_notification(
    token: str,
    title: str,
    body: str,
    data: dict = None,
) -> bool:
    """FCM 푸시 알림 발송. FCM 키 미설정 시 무시."""
    if not settings.fcm_server_key:
        logger.debug("FCM 서버 키 미설정 — 알림 스킵")
        return False

    try:
        payload = {
            "to": token,
            "notification": {"title": title, "body": body},
        }
        if data:
            payload["data"] = data

        resp = requests.post(
            "https://fcm.googleapis.com/fcm/send",
            headers={
                "Authorization": f"key={settings.fcm_server_key}",
                "Content-Type": "application/json",
            },
            json=payload,
            timeout=10,
        )
        if resp.status_code == 200:
            logger.info(f"FCM 발송 성공: {title}")
            return True
        else:
            logger.warning(f"FCM 발송 실패 ({resp.status_code}): {resp.text}")
            return False
    except Exception as e:
        logger.error(f"FCM 발송 에러: {e}")
        return False
