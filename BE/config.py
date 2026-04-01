from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    database_url: str
    gemini_api_key: str
    gemini_model: str
    max_content_length: int

    # 이미지 수집
    max_images_per_article: int

    # 임베딩
    embedding_model: str

    # Google OAuth
    google_client_id: str
    jwt_secret: str
    jwt_expire_hours: int

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
