from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    database_url: str
    database_user: str = ""
    database_pw: str = ""

    # 분석 엔진 선택
    analysis_engine: str = "gemini"  # "gemini" | "claude" | "ollama"

    # Gemini
    gemini_api_keys: str = ""
    gemini_model: str = ""
    max_content_length: int = 16000

    # Claude (Anthropic)
    anthropic_api_key: str = ""
    claude_model: str = "claude-sonnet-4-6"

    # Ollama (로컬 LLM)
    ollama_url: str = "http://localhost:11434"
    ollama_model: str = "llama3"

    # 이미지 수집
    max_images_per_article: int = 5

    # 임베딩
    embedding_model: str = ""

    # FCM 푸시 알림 (없으면 알림 비활성화)
    fcm_server_key: str = ""

    # Google OAuth
    google_client_id: str
    jwt_secret: str
    jwt_expire_hours: int

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
