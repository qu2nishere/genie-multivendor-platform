"""Nạp YuNet (phát hiện mặt) + SFace (vector mặt) một lần trong lifespan (plan AI Worker N6, Y01.05)."""

import logging
from dataclasses import dataclass
from pathlib import Path

import cv2

log = logging.getLogger(__name__)

YUNET_FILE = "face_detection_yunet_2023mar.onnx"
SFACE_FILE = "face_recognition_sface_2021dec.onnx"


@dataclass
class FaceModels:
    detector: "cv2.FaceDetectorYN"
    recognizer: "cv2.FaceRecognizerSF"
    yunet: str = YUNET_FILE.removesuffix(".onnx")
    sface: str = SFACE_FILE.removesuffix(".onnx")


def load(models_dir: str) -> FaceModels | None:
    """Thiếu tệp / lỗi nạp → ``None`` + cảnh báo; worker vẫn lên (bước khuôn mặt sẽ trả null)."""
    d = Path(models_dir)
    yunet, sface = d / YUNET_FILE, d / SFACE_FILE
    if not yunet.is_file() or not sface.is_file():
        log.warning("Thiếu mô hình khuôn mặt trong %s — bước khuôn mặt sẽ trả null", d)
        return None
    try:
        detector = cv2.FaceDetectorYN.create(str(yunet), "", (320, 320), 0.8, 0.3, 50)
        recognizer = cv2.FaceRecognizerSF.create(str(sface), "")
        return FaceModels(detector=detector, recognizer=recognizer)
    except cv2.error as e:  # pragma: no cover — tệp hỏng
        log.warning("Không nạp được mô hình khuôn mặt: %s", e)
        return None
