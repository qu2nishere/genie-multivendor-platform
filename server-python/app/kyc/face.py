"""Tầng 2 — khớp mặt ảnh thẻ ↔ selfie bằng YuNet + SFace, chất lượng selfie (plan AI Worker Y-D6, Y-D7, Y05).

Vector khuôn mặt chỉ tồn tại trong bộ nhớ của lần xử lý, không lưu / không log (N4). Không đồng ý sinh trắc → không chạy
bước này (N5). Thiếu mô hình / lỗi suy luận → các cờ ``None`` + ``errors`` (không làm hỏng pipeline).
"""

from dataclasses import dataclass, field

import cv2
import numpy as np

from app.core.config import Settings
from app.core.images import sharpness
from app.kyc import signals
from app.kyc.models import FaceModels

NO_CONSENT = "BIOMETRIC_CONSENT_NOT_GIVEN"
MODEL_UNAVAILABLE = "FACE_MODEL_UNAVAILABLE"
NO_CARD_FACE = "CARD_FACE_NOT_FOUND"
NO_SELFIE_FACE = "SELFIE_FACE_NOT_FOUND"
MANY_SELFIE_FACES = "SELFIE_MULTIPLE_FACES"
MISSING_IMAGE = "FACE_IMAGE_MISSING"


@dataclass
class FaceResult:
    face_match: bool | None = None
    face_match_score: float | None = None
    liveness_score: float | None = None
    signals: list[str] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)


def detect(models: FaceModels, image: np.ndarray) -> np.ndarray:
    """Khuôn mặt (mỗi dòng: x, y, w, h, 5 điểm mốc, độ tin cậy) sắp theo diện tích giảm dần."""
    h, w = image.shape[:2]
    models.detector.setInputSize((w, h))
    _, faces = models.detector.detect(image)
    if faces is None:
        return np.empty((0, 15), np.float32)
    return faces[np.argsort(-(faces[:, 2] * faces[:, 3]))]


def embed(models: FaceModels, image: np.ndarray, face: np.ndarray) -> np.ndarray:
    aligned = models.recognizer.alignCrop(image, face)
    vec = models.recognizer.feature(aligned).flatten().astype(np.float32)
    return vec / max(float(np.linalg.norm(vec)), 1e-9)


def cosine(a: np.ndarray, b: np.ndarray) -> float:
    return float(np.clip(np.dot(a, b), -1.0, 1.0))


def classify(score: float, settings: Settings) -> tuple[bool, str | None]:
    """≥ ngưỡng khớp → ``True``; giữa hai ngưỡng → ``FACE_SCORE_BORDERLINE``; dưới ngưỡng thấp → ``FACE_SCORE_LOW``."""
    if score >= settings.kyc_face_match_threshold:
        return True, None
    if score >= settings.kyc_face_low_threshold:
        return False, signals.FACE_SCORE_BORDERLINE
    return False, signals.FACE_SCORE_LOW


def moire_index(gray: np.ndarray) -> float:
    """Tỷ lệ năng lượng tần số cao có đỉnh nhọn (dấu hiệu chụp lại màn hình), 0 → 1."""
    g = cv2.resize(gray, (256, 256)).astype(np.float32)
    spec = np.abs(np.fft.fftshift(np.fft.fft2(g - g.mean())))
    yy, xx = np.ogrid[-128:128, -128:128]
    r = np.sqrt(xx * xx + yy * yy)
    high = spec[(r > 60) & (r < 120)]
    if high.size == 0 or high.mean() <= 0:
        return 0.0
    peakiness = float(high.max() / (high.mean() + 1e-9))  # ảnh tự nhiên ~5–15; vân lưới màn hình hàng trăm
    return float(np.clip((peakiness - 25) / 75, 0, 1))


def selfie_quality(image: np.ndarray, face: np.ndarray, face_count: int) -> float:
    """Liveness thụ động 0–1 (Y-D7): độ nét vùng mặt, tỷ lệ diện tích mặt, đúng 1 mặt, không moiré."""
    if face_count != 1:
        return 0.0
    x, y, w, h = (int(max(0, v)) for v in face[:4])
    crop = image[y : y + h, x : x + w]
    if crop.size == 0:
        return 0.0
    gray = cv2.cvtColor(crop, cv2.COLOR_BGR2GRAY)
    sharp = min(1.0, sharpness(gray) / 150.0)
    ratio = (w * h) / float(image.shape[0] * image.shape[1])
    size = 1.0 if 0.04 <= ratio <= 0.7 else max(0.0, 1 - abs(ratio - 0.2) * 4)
    return round(0.45 * sharp + 0.3 * size + 0.25 * (1 - moire_index(gray)), 4)


def run(
    models: FaceModels | None,
    card: np.ndarray | None,
    selfie: np.ndarray | None,
    consent: bool,
    settings: Settings,
) -> FaceResult:
    out = FaceResult()
    if not consent:
        out.errors.append(NO_CONSENT)
        return out
    if models is None:
        out.errors.append(MODEL_UNAVAILABLE)
        return out
    if card is None or selfie is None:
        out.errors.append(MISSING_IMAGE)
        return out
    try:
        card_faces = detect(models, card)
        selfie_faces = detect(models, selfie)
        if len(selfie_faces) == 0:
            out.errors.append(NO_SELFIE_FACE)
            out.liveness_score = 0.0
            out.signals.append(signals.LIVENESS_SUSPECT)
            return out
        if len(selfie_faces) > 1:
            out.errors.append(MANY_SELFIE_FACES)
        out.liveness_score = selfie_quality(selfie, selfie_faces[0], len(selfie_faces))
        if out.liveness_score < settings.kyc_liveness_threshold:
            out.signals.append(signals.LIVENESS_SUSPECT)
        if len(card_faces) == 0:
            out.errors.append(NO_CARD_FACE)
            return out
        score = cosine(embed(models, card, card_faces[0]), embed(models, selfie, selfie_faces[0]))
        out.face_match_score = round(max(0.0, score), 4)
        out.face_match, sig = classify(score, settings)
        if sig:
            out.signals.append(sig)
    except cv2.error:
        out.errors.append(MODEL_UNAVAILABLE)
        out.face_match = out.face_match_score = None
    return out
