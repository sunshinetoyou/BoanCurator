"""Google OAuth + JWT 인증 단위 테스트 (DB 불필요)"""
import sys
import os
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from datetime import datetime, timedelta, timezone
from jose import jwt, JWTError
from config import settings


def test_jwt_create_and_decode():
    """JWT 생성 → 디코딩 → 페이로드 검증"""
    payload = {
        "user_id": 42,
        "email": "test@gmail.com",
        "exp": datetime.now(timezone.utc) + timedelta(hours=settings.jwt_expire_hours),
    }
    token = jwt.encode(payload, settings.jwt_secret, algorithm="HS256")
    print(f"[1] JWT 생성 OK: {token[:50]}...")

    decoded = jwt.decode(token, settings.jwt_secret, algorithms=["HS256"])
    assert decoded["user_id"] == 42
    assert decoded["email"] == "test@gmail.com"
    print(f"[1] JWT 디코딩 OK: user_id={decoded['user_id']}, email={decoded['email']}")


def test_jwt_expired():
    """만료된 JWT 검증 시 에러 발생"""
    payload = {
        "user_id": 1,
        "email": "expired@gmail.com",
        "exp": datetime.now(timezone.utc) - timedelta(hours=1),  # 1시간 전 만료
    }
    token = jwt.encode(payload, settings.jwt_secret, algorithm="HS256")

    try:
        jwt.decode(token, settings.jwt_secret, algorithms=["HS256"])
        print("[2] FAIL: 만료된 토큰이 통과됨")
    except JWTError as e:
        print(f"[2] 만료 토큰 거부 OK: {e}")


def test_jwt_wrong_secret():
    """잘못된 시크릿으로 서명된 JWT 거부"""
    payload = {
        "user_id": 1,
        "email": "fake@gmail.com",
        "exp": datetime.now(timezone.utc) + timedelta(hours=1),
    }
    token = jwt.encode(payload, "wrong-secret-key", algorithm="HS256")

    try:
        jwt.decode(token, settings.jwt_secret, algorithms=["HS256"])
        print("[3] FAIL: 잘못된 시크릿 토큰이 통과됨")
    except JWTError as e:
        print(f"[3] 잘못된 시크릿 거부 OK: {e}")


def test_jwt_tampered():
    """변조된 JWT 거부"""
    payload = {
        "user_id": 1,
        "email": "real@gmail.com",
        "exp": datetime.now(timezone.utc) + timedelta(hours=1),
    }
    token = jwt.encode(payload, settings.jwt_secret, algorithm="HS256")

    # 토큰 페이로드 부분을 변조
    parts = token.split(".")
    parts[1] = parts[1][:-3] + "abc"
    tampered = ".".join(parts)

    try:
        jwt.decode(tampered, settings.jwt_secret, algorithms=["HS256"])
        print("[4] FAIL: 변조된 토큰이 통과됨")
    except JWTError as e:
        print(f"[4] 변조 토큰 거부 OK: {e}")


def test_google_client_id_loaded():
    """Google Client ID가 .env에서 로드되는지 확인"""
    assert settings.google_client_id, "GOOGLE_CLIENT_ID가 비어있음"
    assert settings.google_client_id.endswith(".apps.googleusercontent.com")
    print(f"[5] Google Client ID 로드 OK: {settings.google_client_id[:20]}...")


def test_jwt_settings():
    """JWT 관련 설정값 확인"""
    assert settings.jwt_secret, "JWT_SECRET이 비어있음"
    assert len(settings.jwt_secret) >= 32, "JWT_SECRET이 너무 짧음 (32자 이상 권장)"
    assert settings.jwt_expire_hours > 0, "JWT_EXPIRE_HOURS가 0 이하"
    print(f"[6] JWT 설정 OK: secret={len(settings.jwt_secret)}자, expire={settings.jwt_expire_hours}h")


if __name__ == "__main__":
    tests = [
        test_jwt_create_and_decode,
        test_jwt_expired,
        test_jwt_wrong_secret,
        test_jwt_tampered,
        test_google_client_id_loaded,
        test_jwt_settings,
    ]

    passed = 0
    failed = 0
    for t in tests:
        try:
            t()
            passed += 1
        except Exception as e:
            print(f"[FAIL] {t.__name__}: {e}")
            failed += 1

    print(f"\n{'='*40}")
    print(f"  RESULT: {passed} passed, {failed} failed")
    print(f"{'='*40}")
