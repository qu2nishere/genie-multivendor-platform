"""Khớp mặt + chất lượng selfie (plan AI Worker Y05.07).

Repo KHÔNG có ảnh khuôn mặt người thật được phép dùng (N10) → phần so khớp dùng mô hình giả lập; phần cần mô hình thật
(``@pytest.mark.models``) chỉ kiểm nạp mô hình + ảnh không có mặt. Ca "cùng người / khác người" chạy khi có bộ ảnh
được phép dùng.
"""

from pathlib import Path
from unittest.mock import MagicMock

import numpy as np
import pytest

from app.core.config import Settings
from app.kyc import face, signals
from app.kyc import models as kyc_models
from tests.fixtures import build_card as card

S = Settings(kyc_face_match_threshold=0.363, kyc_face_low_threshold=0.25, kyc_liveness_threshold=0.5)
MODELS_DIR = Path(__file__).resolve().parents[2] / "models"
has_models = (MODELS_DIR / kyc_models.YUNET_FILE).is_file() and (MODELS_DIR / kyc_models.SFACE_FILE).is_file()


def _fake_models(card_faces: int = 1, selfie_faces: int = 1, vectors=((1.0, 0.0), (1.0, 0.0))) -> kyc_models.FaceModels:
    det = MagicMock()

    def detect(img):
        # selfie mẫu 480×480; mọi ảnh khác (thẻ ở các hướng thử) là ảnh thẻ
        n = selfie_faces if img.shape[:2] == (480, 480) else card_faces
        rows = [[10 + i, 10, 200 - i, 200, *([0] * 10), 0.99] for i in range(n)]
        return 1, (np.array(rows, np.float32) if n else None)

    det.detect.side_effect = detect
    rec = MagicMock()
    vecs = iter(np.array(v, np.float32) for v in vectors)
    rec.alignCrop.side_effect = lambda img, f: img
    rec.feature.side_effect = lambda img: next(vecs)
    return kyc_models.FaceModels(detector=det, recognizer=rec)


def _selfie() -> np.ndarray:
    rng = np.random.default_rng(3)
    return rng.integers(0, 255, (480, 480, 3)).astype(np.uint8)


@pytest.mark.parametrize(
    ("score", "match", "signal"),
    [
        (0.9, True, None),
        (0.363, True, None),
        (0.30, False, signals.FACE_SCORE_BORDERLINE),
        (0.1, False, signals.FACE_SCORE_LOW),
    ],
)
def test_classify_thresholds(score, match, signal):
    assert face.classify(score, S) == (match, signal)


def test_same_vectors_match_and_orthogonal_vectors_are_low():
    same = face.run(_fake_models(vectors=((1, 0), (1, 0))), card.front(), _selfie(), True, S)
    assert (same.face_match, same.face_match_score) == (True, 1.0)
    diff = face.run(_fake_models(vectors=((1, 0), (0, 1))), card.front(), _selfie(), True, S)
    assert diff.face_match is False
    assert signals.FACE_SCORE_LOW in diff.signals


def test_no_consent_never_touches_the_models():
    models = _fake_models()
    res = face.run(models, card.front(), _selfie(), False, S)
    assert res.errors == [face.NO_CONSENT]
    assert (res.face_match, res.face_match_score, res.liveness_score) == (None, None, None)
    models.detector.detect.assert_not_called()
    models.recognizer.feature.assert_not_called()


def test_missing_models_or_faces_are_reported_not_raised():
    assert face.run(None, card.front(), _selfie(), True, S).errors == [face.MODEL_UNAVAILABLE]
    no_selfie = face.run(_fake_models(selfie_faces=0), card.front(), _selfie(), True, S)
    assert face.NO_SELFIE_FACE in no_selfie.errors and signals.LIVENESS_SUSPECT in no_selfie.signals
    two = face.run(_fake_models(selfie_faces=2), card.front(), _selfie(), True, S)
    assert face.MANY_SELFIE_FACES in two.errors
    assert two.liveness_score == 0.0 and signals.LIVENESS_SUSPECT in two.signals
    no_card = face.run(_fake_models(card_faces=0), card.front(), _selfie(), True, S)
    assert face.NO_CARD_FACE in no_card.errors and no_card.face_match is None


def test_moire_index_flags_periodic_patterns():
    flat = np.full((256, 256), 128, np.uint8)
    xx = np.arange(256)
    stripes = (128 + 100 * np.sin(xx * 2 * np.pi / 3.2))[None, :].repeat(256, 0).astype(np.uint8)
    assert face.moire_index(flat) == 0.0
    assert face.moire_index(stripes) > 0.5


@pytest.mark.models
@pytest.mark.skipif(not has_models, reason="thiếu models/*.onnx — chạy scripts/fetch-models.sh")
def test_real_models_load_and_find_no_face_on_a_blank_selfie():
    models = kyc_models.load(str(MODELS_DIR))
    assert models is not None
    blank = np.full((480, 640, 3), 200, np.uint8)
    assert len(face.detect(models, blank)) == 0
    res = face.run(models, card.front(), blank, True, S)
    assert face.NO_SELFIE_FACE in res.errors


def test_card_is_turned_upright_before_matching():
    """Ảnh thẻ chụp dọc (xoay 90°): chỉ hướng dựng thẳng có mặt → vẫn so được (lỗi thật 2026-11-22)."""
    det = MagicMock()
    upright_shape = face.shrink(card.front()).shape[:2]

    def detect(img):
        has = img.shape[:2] == (480, 480) or img.shape[:2] == upright_shape
        return 1, (np.array([[10, 10, 200, 200, *([0] * 10), 0.95]], np.float32) if has else None)

    det.detect.side_effect = detect
    rec = MagicMock()
    rec.alignCrop.side_effect = lambda img, f: img
    rec.feature.side_effect = [np.array((1.0, 0.0), np.float32)] * 2
    import cv2

    sideways = cv2.rotate(card.front(), cv2.ROTATE_90_COUNTERCLOCKWISE)
    res = face.run(kyc_models.FaceModels(detector=det, recognizer=rec), sideways, _selfie(), True, S)
    assert face.NO_CARD_FACE not in res.errors
    assert res.face_match is True


def test_large_photos_are_shrunk_for_detection():
    big = np.zeros((2560, 1440, 3), np.uint8)
    out = face.shrink(big)
    assert max(out.shape[:2]) == face.WORK_SIDE
    small = np.zeros((480, 480, 3), np.uint8)
    assert face.shrink(small) is small
