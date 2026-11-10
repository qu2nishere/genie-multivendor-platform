"""pydantic-settings: INTERNAL_TOKEN, BACKEND_URL, MINIO_*."""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    internal_token: str = ""
    backend_url: str = "http://localhost:8080"
    minio_endpoint: str = "localhost:9000"
    minio_access_key: str = ""
    minio_secret_key: str = ""
    minio_secure: bool = False
    models_dir: str = "models"


@lru_cache
def get_settings() -> Settings:
    return Settings()
