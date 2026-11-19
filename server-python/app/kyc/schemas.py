"""Lược đồ hợp đồng Backend ↔ AI Worker (ADR 0011 mục 5; plan AI Worker Y-D1, Y01.03) — snake_case, cấm trường lạ."""

from typing import Annotated

from pydantic import AnyHttpUrl, BaseModel, ConfigDict, Field, field_validator

from app.kyc import signals


class _Strict(BaseModel):
    model_config = ConfigDict(extra="forbid")


class Documents(_Strict):
    """URL ký sẵn 5 phút của bản xem tạm (backend giải mã từ kyc-dossiers)."""

    id_front: AnyHttpUrl
    id_back: AnyHttpUrl
    selfie: AnyHttpUrl


class Declared(_Strict):
    identity_number: Annotated[str, Field(pattern=r"^\d{12}$")]
    full_name: str | None = None


class VerifyRequest(_Strict):
    kyc_id: Annotated[int, Field(gt=0)]
    documents: Documents
    declared: Declared
    biometric_consent: bool
    callback_url: AnyHttpUrl | None = None


class VerifyAccepted(_Strict):
    kyc_id: int
    accepted: bool = True


class Checks(_Strict):
    """Mỗi cờ ``True / False / None`` (None = không kiểm được / không áp dụng)."""

    qr_code: bool | None = None
    mrz: bool | None = None
    mrz_qr_match: bool | None = None
    cccd_number_valid: bool | None = None
    expiry_rule_valid: bool | None = None
    face_match: bool | None = None
    face_duplicate: bool | None = None
    national_emblem: bool | None = None
    fingerprint: bool | None = None


class Signal(_Strict):
    code: str

    @field_validator("code")
    @classmethod
    def known(cls, v: str) -> str:
        if v not in signals.ALL:
            raise ValueError(f"tín hiệu không hợp lệ: {v}")
        return v


class Extracted(_Strict):
    """Dữ liệu đọc từ QR (ưu tiên) / MRZ; ngày dạng ddMMyyyy."""

    identity_number: str | None = None
    full_name: str | None = None
    date_of_birth: str | None = None
    gender: str | None = None
    address: str | None = None
    issue_date: str | None = None
    expiry_date: str | None = None
    legacy_identity_number: str | None = None


class ModelInfo(_Strict):
    yunet: str | None = None
    sface: str | None = None
    pipeline_version: str


class KycResultPayload(_Strict):
    """Thân callback ``PATCH {BACKEND_URL}/api/v1/internal/kyc/{kyc_id}/result``."""

    checks: Checks
    face_match_score: Annotated[float, Field(ge=0, le=1)] | None = None
    liveness_score: Annotated[float, Field(ge=0, le=1)] | None = None
    signals: list[Signal] = Field(default_factory=list)
    extracted: Extracted = Field(default_factory=Extracted)
    errors: list[str] = Field(default_factory=list)
    model: ModelInfo
