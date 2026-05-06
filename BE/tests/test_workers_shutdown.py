"""workers/_shutdown.py 단위 테스트.

검증:
  - install_shutdown_handler 가 SIGTERM/SIGINT 핸들러를 등록한다
  - 핸들러가 호출되면 반환된 Event 가 set 되고, event.wait() 가 즉시 깨어난다

배경:
  과거 3개 워커 모두 `while True: time.sleep(N)` 로 동작해 Docker SIGTERM
  수신 시에도 sleep 깨지 않아 stop_grace_period 초과 후 SIGKILL 강제 종료됐다.
  install_shutdown_handler() 가 반환한 Event 의 wait(N) 으로 sleep 을 대체하면
  signal 즉시 깨어나 graceful shutdown 가능.
"""
import os
import signal
import sys
import time
from pathlib import Path
from unittest.mock import patch

os.environ.setdefault("DATABASE_URL", "postgresql+psycopg2://x:x@localhost:5432/x")

BE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BE_DIR))

from workers import _shutdown


def test_install_registers_sigterm_and_sigint():
    """signal.signal 이 SIGTERM, SIGINT 둘 다에 호출된다."""
    with patch.object(_shutdown.signal, "signal") as mock_signal:
        event = _shutdown.install_shutdown_handler()

    registered_signums = {call.args[0] for call in mock_signal.call_args_list}
    assert signal.SIGTERM in registered_signums
    assert signal.SIGINT in registered_signums
    # 두 호출 모두 같은 핸들러 함수를 등록해야 한다 (Event 공유)
    handlers = {call.args[1] for call in mock_signal.call_args_list}
    assert len(handlers) == 1
    assert event.is_set() is False


def test_handler_sets_event():
    """등록된 핸들러를 직접 호출하면 Event 가 set 된다 (signal 발생 없이도 동작 검증)."""
    captured = {}

    def fake_signal(sig, handler):
        captured[sig] = handler

    with patch.object(_shutdown.signal, "signal", side_effect=fake_signal):
        event = _shutdown.install_shutdown_handler()

    assert event.is_set() is False
    captured[signal.SIGTERM](signal.SIGTERM, None)
    assert event.is_set() is True


def test_event_wait_returns_immediately_when_set():
    """Event 가 set 된 상태에서 wait(timeout) 은 timeout 을 기다리지 않고 즉시 True 반환.

    회귀 방지: 이 동작이 깨지면 워커들의 shutdown.wait(N) 이 N 초를 기다리게 됨.
    """
    captured = {}

    def fake_signal(sig, handler):
        captured[sig] = handler

    with patch.object(_shutdown.signal, "signal", side_effect=fake_signal):
        event = _shutdown.install_shutdown_handler()

    captured[signal.SIGTERM](signal.SIGTERM, None)

    start = time.monotonic()
    woke = event.wait(timeout=5)
    elapsed = time.monotonic() - start

    assert woke is True
    assert elapsed < 0.1, f"event.wait should return immediately, took {elapsed:.3f}s"
