"""Mã tín hiệu rủi ro AI Worker được phép sinh — đúng chính tả ENTITY 1.6 (plan AI Worker Y01.02).

Worker KHÔNG tính điểm (N1): backend quy đổi mã → điểm theo ``genie.kyc.risk.points``. ``DUPLICATE_ID`` và
``DECLARED_DATA_MISMATCH`` do backend tự sinh nên không có ở đây.
"""

QR_MRZ_MISMATCH = "QR_MRZ_MISMATCH"
ID_STRUCTURE_MISMATCH = "ID_STRUCTURE_MISMATCH"
EXPIRY_RULE_MISMATCH = "EXPIRY_RULE_MISMATCH"
FACE_DUPLICATE = "FACE_DUPLICATE"  # chưa sinh ở đợt này (Y-D4)
FACE_SCORE_BORDERLINE = "FACE_SCORE_BORDERLINE"
FACE_SCORE_LOW = "FACE_SCORE_LOW"
LIVENESS_SUSPECT = "LIVENESS_SUSPECT"
LEGACY_CARD = "LEGACY_CARD"
UNREADABLE_IMAGE = "UNREADABLE_IMAGE"

#: Thứ tự cố định khi trả về (scoring.assemble)
ORDER: tuple[str, ...] = (
    UNREADABLE_IMAGE,
    LEGACY_CARD,
    QR_MRZ_MISMATCH,
    ID_STRUCTURE_MISMATCH,
    EXPIRY_RULE_MISMATCH,
    FACE_SCORE_BORDERLINE,
    FACE_SCORE_LOW,
    LIVENESS_SUSPECT,
    FACE_DUPLICATE,
)

ALL: frozenset[str] = frozenset(ORDER)
