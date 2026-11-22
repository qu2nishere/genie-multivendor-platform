"""Tải ảnh an toàn (plan AI Worker Y02.03)."""

from io import BytesIO

import cv2
import httpx
import numpy as np
import pytest
import respx
from PIL import Image

from app.core import images
from app.core.config import Settings

S = Settings(storage_allowed_hosts="localhost:9000,*.s3.ap-southeast-1.amazonaws.com", kyc_download_max_bytes=50_000)


def _jpeg(w=800, h=600, exif_rotate=False, noise=True) -> bytes:
    arr = (np.random.default_rng(1).integers(0, 255, (h, w, 3)) if noise else np.full((h, w, 3), 128)).astype(np.uint8)
    im = Image.fromarray(arr)
    out = BytesIO()
    if exif_rotate:
        exif = Image.Exif()
        exif[0x0112] = 6  # xoay 90° khi hiển thị
        im.save(out, "JPEG", exif=exif)
    else:
        im.save(out, "JPEG")
    return out.getvalue()


def test_host_allow_list():
    assert images.host_allowed("http://localhost:9000/kyc-dossiers/x.jpg?sig=1", S)
    assert images.host_allowed("https://genie-prod-kyc-dossiers.s3.ap-southeast-1.amazonaws.com/x.jpg", S)
    assert not images.host_allowed("http://169.254.169.254/latest/meta-data", S)
    assert not images.host_allowed("http://localhost:8080/x.jpg", S)
    assert not images.host_allowed("file:///etc/passwd", S)


@respx.mock
async def test_fetch_rules():
    jpg, pdf = {"content-type": "image/jpeg"}, {"content-type": "application/pdf"}
    respx.get("http://localhost:9000/ok.jpg").respond(200, content=b"x" * 100, headers=jpg)
    respx.get("http://localhost:9000/big.jpg").respond(200, content=b"x" * 60_000, headers=jpg)
    respx.get("http://localhost:9000/doc.pdf").respond(200, content=b"%PDF", headers=pdf)
    respx.get("http://localhost:9000/gone.jpg").respond(403)
    async with httpx.AsyncClient() as c:
        assert len(await images.fetch_bytes(c, "http://localhost:9000/ok.jpg", S)) == 100
        for url, err in [
            ("http://localhost:9000/big.jpg", "TOO_LARGE"),
            ("http://localhost:9000/doc.pdf", "UNSUPPORTED_TYPE"),
            ("http://localhost:9000/gone.jpg", "HTTP_403"),
            ("http://evil.example/x.jpg", "HOST_NOT_ALLOWED"),
        ]:
            with pytest.raises(images.ImageError, match=err):
                await images.fetch_bytes(c, url, S)


def test_decode_fixes_exif_orientation_and_downscales():
    img = images.decode(_jpeg(800, 600, exif_rotate=True))
    assert img.shape[:2] == (800, 600)  # đã dựng thẳng
    big = images.decode(_jpeg(3000, 1500))
    assert max(big.shape[:2]) == 2000
    with pytest.raises(images.ImageError):
        images.decode(b"not an image")


def test_quality_flags_blurry_and_small_images():
    sharp = images.decode(_jpeg(900, 700))
    assert images.assess_quality(sharp, S).ok
    blurry = cv2.GaussianBlur(sharp, (51, 51), 20)
    assert not images.assess_quality(blurry, S).ok
    small = cv2.resize(sharp, (300, 200))
    assert not images.assess_quality(small, S).ok


def test_sharp_document_on_a_plain_background_is_not_flagged_blurry():
    """Ảnh điện thoại: thẻ nét nằm giữa nền trơn lớn — đo cả khung ra "mờ", đo theo ô thì đạt (lỗi thật 2026-11-22)."""
    sharp = images.decode(_jpeg(900, 700))
    canvas = np.full((2000, 1127, 3), 230, np.uint8)
    canvas[600:1300, 100:1000] = sharp
    assert images.assess_quality(canvas, S).ok
    blurry = cv2.GaussianBlur(canvas, (51, 51), 20)
    assert not images.assess_quality(blurry, S).ok
