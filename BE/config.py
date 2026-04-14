from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    database_url: str
    database_user: str = ""
    database_pw: str = ""

    # Gemini
    gemini_api_keys: str = ""
    gemini_model: str = ""
    max_content_length: int = 16000

    # 이미지 수집
    max_images_per_article: int = 5

    # 임베딩
    embedding_model: str = ""

    # FCM 푸시 알림 (없으면 알림 비활성화)
    fcm_server_key: str = ""

    # Google OAuth (API 서버만 필요. 워커는 빈 값 허용)
    google_client_id: str = ""
    jwt_secret: str = ""
    jwt_expire_hours: int = 1  # Access token: 1시간
    jwt_refresh_expire_days: int = 30  # Refresh token: 30일

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
