"""Dựng ảnh thẻ CCCD MẪU (dữ liệu giả, không phải giấy tờ thật — plan AI Worker N10, Y03.01).

Mặt trước: nền, khung, ô ảnh (xám), chữ, mã QR sinh từ chuỗi giả. Mặt sau: 3 dòng MRZ TD1 font đơn cách
(DejaVu Sans Mono — thay cho OCR-B). Tham số biến thể: xoay, làm mờ, giảm sáng, thu nhỏ.
"""

from pathlib import Path

import cv2
import numpy as np
import qrcode
from PIL import Image, ImageDraw, ImageFont

from app.kyc import mrz

W, H = 1012, 638  # tỷ lệ thẻ ID-1
FONT_DIRS = [Path("/usr/share/fonts/truetype/dejavu"), Path("C:/Windows/Fonts")]

SAMPLE = {
    "identity_number": "079190000012",
    "legacy": "025123456",
    "full_name": "Phạm Thị Lan",
    "dob": "12041990",
    "gender": "Nữ",
    "address": "Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
    "issue": "10052021",
    "expiry": "12042030",
}


def _font(mono: bool, size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    names = ["DejaVuSansMono-Bold.ttf", "consolab.ttf"] if mono else ["DejaVuSans.ttf", "arial.ttf"]
    for d in FONT_DIRS:
        for n in names:
            if (d / n).is_file():
                return ImageFont.truetype(str(d / n), size)
    return ImageFont.load_default()


def qr_text(d: dict = SAMPLE) -> str:
    return "|".join(
        [d["identity_number"], d["legacy"], d["full_name"], d["dob"], d["gender"], d["address"], d["issue"]]
    )


def mrz_lines(d: dict = SAMPLE) -> list[str]:
    dob = d["dob"][6:8] + d["dob"][2:4] + d["dob"][0:2]
    exp = d["expiry"][6:8] + d["expiry"][2:4] + d["expiry"][0:2]
    from app.kyc.text import normalize_name

    parts = normalize_name(d["full_name"]).split()
    return mrz.build(d["identity_number"], dob, "F" if d["gender"] == "Nữ" else "M", exp, parts[0], " ".join(parts[1:]))


def _to_bgr(im: Image.Image) -> np.ndarray:
    return cv2.cvtColor(np.asarray(im.convert("RGB")), cv2.COLOR_RGB2BGR)


def front(text: str | None = None, with_qr: bool = True) -> np.ndarray:
    im = Image.new("RGB", (W, H), (236, 242, 238))
    dr = ImageDraw.Draw(im)
    dr.rounded_rectangle((8, 8, W - 8, H - 8), 24, outline=(120, 140, 130), width=4)
    dr.text((300, 40), "CĂN CƯỚC CÔNG DÂN (MẪU THỬ)", fill=(150, 30, 30), font=_font(False, 34))
    dr.rectangle((40, 160, 260, 440), fill=(170, 170, 170))  # ô ảnh chân dung
    dr.text((300, 170), "Họ và tên: " + SAMPLE["full_name"], fill=(20, 20, 20), font=_font(False, 28))
    dr.text((300, 220), "Số: " + SAMPLE["identity_number"], fill=(20, 20, 20), font=_font(False, 28))
    if with_qr:
        q = qrcode.QRCode(border=2, box_size=6, error_correction=qrcode.constants.ERROR_CORRECT_M)
        q.add_data(text or qr_text())
        q.make(fit=True)
        qim = q.make_image(fill_color="black", back_color="white").convert("RGB").resize((260, 260), Image.NEAREST)
        im.paste(qim, (W - 300, 300))
    return _to_bgr(im)


def back(lines: list[str] | None = None) -> np.ndarray:
    im = Image.new("RGB", (W, H), (240, 240, 236))
    dr = ImageDraw.Draw(im)
    dr.rounded_rectangle((8, 8, W - 8, H - 8), 24, outline=(120, 120, 120), width=4)
    dr.text((60, 60), "Đặc điểm nhận dạng: (mẫu thử)", fill=(40, 40, 40), font=_font(False, 26))
    f = _font(True, 40)
    for i, line in enumerate(lines or mrz_lines()):
        dr.text((50, 420 + i * 62), line, fill=(10, 10, 10), font=f)
    return _to_bgr(im)


def vary(img: np.ndarray, rotate: int = 0, blur: int = 0, dark: float = 1.0, scale: float = 1.0) -> np.ndarray:
    out = img
    if scale != 1.0:
        out = cv2.resize(out, None, fx=scale, fy=scale, interpolation=cv2.INTER_AREA)
    if rotate:
        out = cv2.rotate(
            out, {90: cv2.ROTATE_90_CLOCKWISE, 180: cv2.ROTATE_180, 270: cv2.ROTATE_90_COUNTERCLOCKWISE}[rotate]
        )
    if blur:
        out = cv2.GaussianBlur(out, (blur | 1, blur | 1), 0)
    if dark != 1.0:
        out = np.clip(out.astype(np.float32) * dark, 0, 255).astype(np.uint8)
    return out


def jpeg(img: np.ndarray, quality: int = 90) -> bytes:
    ok, buf = cv2.imencode(".jpg", img, [cv2.IMWRITE_JPEG_QUALITY, quality])
    assert ok
    return buf.tobytes()
