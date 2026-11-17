"""pydantic-settings: INTERNAL_TOKEN, BACKEND_URL, MINIO_*, STORAGE_* — dev: Silo, production: AWS S3 (ADR 0010)."""

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
    # Tên bucket thật = tiền tố + tên logic (production vd. "genie-prod-"); dev để trống
    storage_bucket_prefix: str = ""
    storage_region: str = "us-east-1"
    models_dir: str = "models"


@lru_cache
def get_settings() -> Settings:
    return Settings()
