"""pydantic-settings: INTERNAL_TOKEN, BACKEND_URL, MINIO_*, STORAGE_*, KYC_* — dev: Silo, production: AWS S3 (ADR 0010).

Mỗi trường đọc biến môi trường cùng tên viết hoa (vd. ``kyc_face_match_threshold`` ← ``KYC_FACE_MATCH_THRESHOLD``).
Ngưỡng KYC là cấu hình, không ghi cứng trong mã (plan AI Worker N8).
"""

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

    # ===== KYC (plan AI Worker Y01.01; Y-D6, Y-D7, Y-D8) =====
    # Host được phép tải ảnh (chống SSRF — N3), cách nhau dấu phẩy; "*.s3.ap-southeast-1.amazonaws.com" khớp mọi bucket
    storage_allowed_hosts: str = "localhost:9000"
    kyc_download_max_bytes: int = 12 * 1024 * 1024
    kyc_download_timeout_s: float = 5.0
    kyc_face_match_threshold: float = 0.363  # cosine SFace ≥ ngưỡng → khớp
    kyc_face_low_threshold: float = 0.25  # dưới ngưỡng → FACE_SCORE_LOW; giữa hai ngưỡng → FACE_SCORE_BORDERLINE
    kyc_liveness_threshold: float = 0.5  # liveness_score dưới ngưỡng → LIVENESS_SUSPECT
    kyc_min_image_side: int = 600  # cạnh ngắn tối thiểu (px) của ảnh giấy tờ
    kyc_blur_threshold: float = 60.0  # phương sai Laplacian tối thiểu
    kyc_max_concurrency: int = 1  # số hồ sơ xử lý cùng lúc (ngân sách RAM — TS 16.2)
    kyc_pipeline_timeout_s: float = 20.0
    callback_retries: int = 3
    callback_timeout_s: float = 5.0
    pipeline_version: str = "kyc-1.0.0"

    def allowed_hosts(self) -> list[str]:
        return [h.strip().lower() for h in self.storage_allowed_hosts.split(",") if h.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
