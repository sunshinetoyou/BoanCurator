"""SIGTERM/SIGINT graceful shutdown 헬퍼.

Docker stop 은 SIGTERM 을 보낸 뒤 stop_grace_period (기본 10s) 이내에
프로세스가 종료하지 않으면 SIGKILL 한다. 각 워커는 install_shutdown_handler()
가 반환한 threading.Event 를 폴링/대기하여 즉시 루프를 빠져나가야 한다.

핵심: time.sleep(N) 대신 event.wait(N) 을 써야 SIGTERM 수신 시 즉시 깨어난다.
"""
import logging
import signal
import threading

logger = logging.getLogger(__name__)


def install_shutdown_handler() -> threading.Event:
    """SIGTERM/SIGINT 수신 시 set 되는 Event 를 반환한다.

    동일 프로세스에서 여러 번 호출하면 마지막 호출의 Event 가 활성화된다
    (signal.signal 의 표준 동작). 워커 진입점에서 한 번만 호출할 것.
    """
    event = threading.Event()

    def _handler(signum, frame):
        logger.info(f"Signal {signum} 수신 — graceful shutdown 시작")
        event.set()

    signal.signal(signal.SIGTERM, _handler)
    signal.signal(signal.SIGINT, _handler)
    return event
