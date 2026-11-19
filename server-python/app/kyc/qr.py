"""Tầng 1 — mã QR mặt trước CCCD (plan AI Worker Y-D3, Y03.02–Y03.04).

Chuỗi QR CCCD gắn chip: ``số CCCD|số CMND cũ|họ tên|ngày sinh ddMMyyyy|giới tính|nơi thường trú|ngày cấp ddMMyyyy``.
Giải mã bằng ``cv2.QRCodeDetector`` với chuỗi tiền xử lý tăng dần (dừng ngay khi đọc được).
"""

import re
from collections.abc import Iterator
from dataclasses import dataclass
from datetime import date

import cv2
import numpy as np


@dataclass(frozen=True)
class QrData:
    identity_number: str
    legacy_identity_number: str | None
    full_name: str
    date_of_birth: str  # ddMMyyyy
    gender: str  # "Nam" / "Nữ"
    address: str
    issue_date: str  # ddMMyyyy


def _variants(image: np.ndarray) -> Iterator[np.ndarray]:
    gray = image if image.ndim == 2 else cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    yield gray
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8)).apply(gray)
    yield clahe
    yield cv2.adaptiveThreshold(clahe, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 31, 5)
    up = cv2.resize(gray, None, fx=2, fy=2, interpolation=cv2.INTER_CUBIC)
    yield up
    for rot in (cv2.ROTATE_90_CLOCKWISE, cv2.ROTATE_180, cv2.ROTATE_90_COUNTERCLOCKWISE):
        yield cv2.rotate(clahe, rot)


def decode(image: np.ndarray) -> str | None:
    """Chuỗi thô của mã QR hoặc ``None`` nếu không tìm / không giải được."""
    detector = cv2.QRCodeDetector()
    for v in _variants(image):
        try:
            text, points, _ = detector.detectAndDecode(v)
        except cv2.error:
            continue
        if text:
            return text
    return None


def _valid_date(s: str) -> bool:
    if not re.fullmatch(r"\d{8}", s):
        return False
    try:
        date(int(s[4:]), int(s[2:4]), int(s[:2]))
    except ValueError:
        return False
    return True


def parse(raw: str | None) -> QrData | None:
    """Tách 7 phần; sai số phần / sai định dạng → ``None``."""
    if not raw:
        return None
    parts = raw.strip().split("|")
    if len(parts) != 7:
        return None
    cccd, cmnd, name, dob, gender, address, issued = (p.strip() for p in parts)
    if not re.fullmatch(r"\d{12}", cccd) or (cmnd and not re.fullmatch(r"\d{9}", cmnd)):
        return None
    if not name or not _valid_date(dob) or not _valid_date(issued) or gender not in ("Nam", "Nữ"):
        return None
    return QrData(cccd, cmnd or None, name, dob, gender, address, issued)


@dataclass(frozen=True)
class QrCheck:
    data: QrData | None
    flag: bool | None  # checks.qr_code
    signal: str | None  # LEGACY_CARD / UNREADABLE_IMAGE / None


def check(image: np.ndarray | None, quality_ok: bool) -> QrCheck:
    """Đọc được + hợp lệ → ``True``; không thấy QR trên ảnh đạt chất lượng → ``LEGACY_CARD`` (``False``);
    ảnh không đạt / không có ảnh → ``UNREADABLE_IMAGE`` (``None``)."""
    from app.kyc import signals

    if image is None:
        return QrCheck(None, None, signals.UNREADABLE_IMAGE)
    data = parse(decode(image))
    if data:
        return QrCheck(data, True, None)
    if not quality_ok:
        return QrCheck(None, None, signals.UNREADABLE_IMAGE)
    return QrCheck(None, False, signals.LEGACY_CARD)
