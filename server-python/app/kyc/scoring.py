"""Gom kết quả các bước → ``checks`` + ``signals`` + ``extracted`` + ``errors`` (plan AI Worker Y06.01).

KHÔNG tính điểm, KHÔNG kết luận (N1) — backend quy đổi tín hiệu → điểm.
"""

from dataclasses import dataclass, field

from app.kyc import signals
from app.kyc.face import FaceResult
from app.kyc.mrz import MrzData
from app.kyc.qr import QrCheck
from app.kyc.schemas import Checks, Extracted, KycResultPayload, ModelInfo, Signal


@dataclass
class Steps:
    """Trạng thái pipeline — mỗi bước ghi phần của mình; quá giờ vẫn gom được phần đã có."""

    unreadable: bool = False  # một ảnh tải / giải mã hỏng hoặc không đạt chất lượng
    qr: QrCheck | None = None
    mrz: MrzData | None = None
    mrz_attempted: bool = False
    cccd_number_valid: bool | None = None
    expiry_rule_valid: bool | None = None
    mrz_qr_match: bool | None = None
    face: FaceResult | None = None
    errors: list[str] = field(default_factory=list)


def _ddmmyyyy_from_yymmdd(s: str | None, birth: bool) -> str | None:
    if not s or len(s) != 6 or not s.isdigit():
        return None
    yy = int(s[:2])
    year = (1900 + yy if yy > 30 else 2000 + yy) if birth else 2000 + yy
    return f"{s[4:6]}{s[2:4]}{year}"


def extracted(steps: Steps) -> Extracted:
    q = steps.qr.data if steps.qr else None
    m = steps.mrz
    gender_from_mrz = {"M": "Nam", "F": "Nữ"}.get(m.sex) if m else None
    return Extracted(
        identity_number=q.identity_number if q else (m.identity_number if m else None),
        full_name=q.full_name if q else (m.full_name if m else None),
        date_of_birth=q.date_of_birth if q else (_ddmmyyyy_from_yymmdd(m.date_of_birth, True) if m else None),
        gender=q.gender if q else gender_from_mrz,
        address=q.address if q else None,
        issue_date=q.issue_date if q else None,
        expiry_date=_ddmmyyyy_from_yymmdd(m.expiry_date, False) if m else None,
        legacy_identity_number=q.legacy_identity_number if q else None,
    )


def assemble(steps: Steps, model: ModelInfo) -> KycResultPayload:
    found: set[str] = set()
    if steps.unreadable:
        found.add(signals.UNREADABLE_IMAGE)
    if steps.qr and steps.qr.signal:
        found.add(steps.qr.signal)
    mrz_flag: bool | None = None
    if steps.mrz is not None:
        mrz_flag = steps.mrz.valid
    elif steps.mrz_attempted:
        found.add(signals.UNREADABLE_IMAGE)
    if steps.mrz_qr_match is False:
        found.add(signals.QR_MRZ_MISMATCH)
    if steps.cccd_number_valid is False:
        found.add(signals.ID_STRUCTURE_MISMATCH)
    if steps.expiry_rule_valid is False:
        found.add(signals.EXPIRY_RULE_MISMATCH)
    face = steps.face or FaceResult()
    found.update(face.signals)
    errors = list(dict.fromkeys([*steps.errors, *face.errors]))
    return KycResultPayload(
        checks=Checks(
            qr_code=steps.qr.flag if steps.qr else None,
            mrz=mrz_flag,
            mrz_qr_match=steps.mrz_qr_match,
            cccd_number_valid=steps.cccd_number_valid,
            expiry_rule_valid=steps.expiry_rule_valid,
            face_match=face.face_match,
            face_duplicate=None,  # Y-D4: chưa làm
            national_emblem=None,  # Y-D5: chưa làm
            fingerprint=None,
        ),
        face_match_score=face.face_match_score,
        liveness_score=face.liveness_score,
        signals=[Signal(code=c) for c in signals.ORDER if c in found],
        extracted=extracted(steps),
        errors=errors,
        model=model,
    )
