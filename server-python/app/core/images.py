"""Tải ảnh KYC an toàn + giải mã trong bộ nhớ + đánh giá chất lượng (plan AI Worker N3, N4, Y-D8, Y02.01–Y02.02).

- Chỉ tải từ host thuộc ``STORAGE_ALLOWED_HOSTS`` (chống SSRF), chỉ ``image/jpeg`` / ``image/png``,
  tối đa 12 MB, 5 giây.
- Không ghi ảnh xuống đĩa; không log URL (URL ký sẵn là bí mật tạm thời).
"""

from dataclasses import dataclass
from fnmatch import fnmatch
from io import BytesIO
from urllib.parse import urlsplit

import cv2
import httpx
import numpy as np
from PIL import Image, ImageOps

from app.core.config import Settings

ALLOWED_TYPES = ("image/jpeg", "image/png")
MAX_SIDE = 2000


class ImageError(Exception):
    """Ảnh không tải / không giải mã được — pipeline coi như ảnh không đọc được (UNREADABLE_IMAGE)."""


def host_allowed(url: str, settings: Settings) -> bool:
    parts = urlsplit(url)
    if parts.scheme not in ("http", "https") or not parts.hostname:
        return False
    host = parts.hostname.lower()
    netloc = f"{host}:{parts.port}" if parts.port else host
    return any(fnmatch(netloc, pattern) or fnmatch(host, pattern) for pattern in settings.allowed_hosts())


async def fetch_bytes(client: httpx.AsyncClient, url: str, settings: Settings) -> bytes:
    if not host_allowed(url, settings):
        raise ImageError("HOST_NOT_ALLOWED")
    try:
        async with client.stream("GET", url, timeout=settings.kyc_download_timeout_s) as r:
            if r.status_code != 200:
                raise ImageError(f"HTTP_{r.status_code}")
            ctype = r.headers.get("content-type", "").split(";")[0].strip().lower()
            if ctype not in ALLOWED_TYPES:
                raise ImageError("UNSUPPORTED_TYPE")
            buf = bytearray()
            async for chunk in r.aiter_bytes():
                buf.extend(chunk)
                if len(buf) > settings.kyc_download_max_bytes:
                    raise ImageError("TOO_LARGE")
            return bytes(buf)
    except httpx.HTTPError as e:
        raise ImageError("DOWNLOAD_FAILED") from e


def decode(data: bytes) -> np.ndarray:
    """Giải mã → ảnh BGR, đã dựng thẳng theo EXIF, cạnh dài ≤ 2000 px."""
    try:
        with Image.open(BytesIO(data)) as im:
            im = ImageOps.exif_transpose(im).convert("RGB")
            arr = cv2.cvtColor(np.asarray(im), cv2.COLOR_RGB2BGR)
    except Exception as e:  # noqa: BLE001 — mọi lỗi giải mã đều là ảnh hỏng
        raise ImageError("NOT_AN_IMAGE") from e
    h, w = arr.shape[:2]
    if max(h, w) > MAX_SIDE:
        scale = MAX_SIDE / max(h, w)
        arr = cv2.resize(arr, (round(w * scale), round(h * scale)), interpolation=cv2.INTER_AREA)
    return arr


@dataclass(frozen=True)
class Quality:
    ok: bool
    short_side: int
    sharpness: float


def sharpness(image: np.ndarray) -> float:
    gray = image if image.ndim == 2 else cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    return float(cv2.Laplacian(gray, cv2.CV_64F).var())


def document_sharpness(image: np.ndarray, side: int = 1000, grid: int = 8, pct: float = 75) -> float:
    """Độ nét ảnh giấy tờ: chuẩn về cạnh dài ``side`` px, chia lưới ``grid``×``grid``, lấy phân vị ``pct`` của
    phương sai Laplacian từng ô. Đo cả khung (cách cũ) bị nền bàn / bao nhựa / vùng trống kéo xuống — ảnh điện thoại
    rõ nét vẫn bị coi là mờ (plan sửa KYC ảnh thật L1); đo theo ô thì vùng chữ quyết định, ảnh mờ toàn phần vẫn thấp."""
    gray = image if image.ndim == 2 else cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    h, w = gray.shape
    scale = side / max(h, w)
    gray = cv2.resize(gray, (max(1, round(w * scale)), max(1, round(h * scale))), interpolation=cv2.INTER_AREA)
    lap = cv2.Laplacian(gray, cv2.CV_64F)
    gh, gw = lap.shape
    tiles = [
        lap[i * gh // grid : (i + 1) * gh // grid, j * gw // grid : (j + 1) * gw // grid].var()
        for i in range(grid)
        for j in range(grid)
    ]
    return float(np.percentile(tiles, pct))


def assess_quality(image: np.ndarray, settings: Settings) -> Quality:
    short = min(image.shape[:2])
    sharp = document_sharpness(image)
    return Quality(short >= settings.kyc_min_image_side and sharp >= settings.kyc_blur_threshold, short, sharp)
