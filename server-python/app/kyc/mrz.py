"""Tầng 1 — MRZ mặt sau CCCD, định dạng TD1 ICAO 9303: 3 dòng × 30 ký tự (plan AI Worker Y-D2, Y04.02–Y04.04).

Dòng 1: ``ID`` + ``VNM`` + số giấy tờ (9 số cuối CCCD) + số kiểm tra + trường tùy chọn (CCCD 12 số).
Dòng 2: ngày sinh YYMMDD + KT, giới tính, ngày hết hạn YYMMDD + KT, quốc tịch, tùy chọn, KT tổng.
Dòng 3: HỌ<<TÊN<ĐỆM (không dấu).
"""

import re
from dataclasses import dataclass

import cv2
import numpy as np

LINE = 30
_WEIGHTS = (7, 3, 1)


def _value(c: str) -> int:
    if c.isdigit():
        return int(c)
    if "A" <= c <= "Z":
        return ord(c) - 55
    return 0  # "<"


def check_digit(field: str) -> str:
    """Số kiểm tra trọng số 7-3-1 (ICAO 9303 phần 3, mục 4.9)."""
    return str(sum(_value(c) * _WEIGHTS[i % 3] for i, c in enumerate(field)) % 10)


def _pad(s: str, n: int = LINE) -> str:
    return (s + "<" * n)[:n]


def build(identity_number: str, dob_yymmdd: str, sex: str, expiry_yymmdd: str, surname: str, given: str) -> list[str]:
    """Dựng 3 dòng MRZ hợp lệ (dùng cho thẻ mẫu trong test)."""
    doc = identity_number[-9:]
    l1 = _pad("IDVNM" + doc + check_digit(doc) + identity_number)
    l2_body = dob_yymmdd + check_digit(dob_yymmdd) + sex + expiry_yymmdd + check_digit(expiry_yymmdd) + "VNM"
    l2 = _pad(l2_body, 29)
    composite = l1[5:30] + l2[0:7] + l2[8:15] + l2[18:29]
    l2 = l2 + check_digit(composite)
    l3 = _pad(surname.replace(" ", "<") + "<<" + given.replace(" ", "<"))
    return [l1, l2, l3]


@dataclass(frozen=True)
class MrzData:
    document_number: str
    identity_number: str | None  # trường tùy chọn dòng 1
    date_of_birth: str  # YYMMDD
    sex: str  # M / F / <
    expiry_date: str  # YYMMDD
    nationality: str
    full_name: str  # "HO TEN DEM" không dấu
    valid: bool  # mọi số kiểm tra đúng


def parse(lines: list[str]) -> MrzData | None:
    """Phân tích 3 dòng; sai hình dạng → ``None``; sai số kiểm tra → ``valid = False``."""
    if len(lines) != 3 or any(len(x) != LINE or not re.fullmatch(r"[A-Z0-9<]{30}", x) for x in lines):
        return None
    l1, l2, l3 = lines
    if not l1.startswith("ID"):
        return None
    doc, doc_cd = l1[5:14], l1[14]
    optional = l1[15:30].replace("<", "")
    dob, dob_cd, sex, exp, exp_cd, nat = l2[0:6], l2[6], l2[7], l2[8:14], l2[14], l2[15:18]
    composite = l1[5:30] + l2[0:7] + l2[8:15] + l2[18:29]
    valid = (
        check_digit(doc) == doc_cd
        and check_digit(dob) == dob_cd
        and check_digit(exp) == exp_cd
        and check_digit(composite) == l2[29]
        and dob.isdigit()
        and exp.isdigit()
    )
    surname, _, given = l3.partition("<<")
    name = " ".join(p for p in (surname.replace("<", " ") + " " + given.replace("<", " ")).split())
    return MrzData(doc, optional if re.fullmatch(r"\d{12}", optional) else None, dob, sex, exp, nat, name, valid)


# ===== Đọc ảnh: tìm dải MRZ + OCR =====

_FIX_DIGIT = str.maketrans({"O": "0", "Q": "0", "D": "0", "I": "1", "L": "1", "Z": "2", "S": "5", "B": "8", "G": "6"})


def locate(image: np.ndarray) -> np.ndarray | None:
    """Cắt vùng chữ dày đặc ở nửa dưới ảnh (blackhat + gradient + đóng hình thái), ``None`` nếu không thấy."""
    gray = image if image.ndim == 2 else cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    h, w = gray.shape
    scale = 1000 / w
    small = cv2.resize(gray, (1000, max(1, round(h * scale))))
    rect = cv2.getStructuringElement(cv2.MORPH_RECT, (25, 7))
    blackhat = cv2.morphologyEx(cv2.GaussianBlur(small, (3, 3), 0), cv2.MORPH_BLACKHAT, rect)
    grad = cv2.Sobel(blackhat, cv2.CV_32F, 1, 0, ksize=-1)
    grad = np.absolute(grad)
    grad = (255 * (grad - grad.min()) / max(1e-6, grad.max() - grad.min())).astype("uint8")
    grad = cv2.morphologyEx(grad, cv2.MORPH_CLOSE, rect)
    _, th = cv2.threshold(grad, 0, 255, cv2.THRESH_BINARY | cv2.THRESH_OTSU)
    th = cv2.morphologyEx(th, cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_RECT, (35, 25)))
    th = cv2.erode(th, None, iterations=2)
    contours, _ = cv2.findContours(th, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    best = None
    for c in sorted(contours, key=cv2.contourArea, reverse=True):
        x, y, cw, ch = cv2.boundingRect(c)
        if cw / small.shape[1] > 0.6 and cw / max(ch, 1) > 3 and y > small.shape[0] * 0.3:
            best = (x, y, cw, ch)
            break
    if best is None:
        return None
    x, y, cw, ch = best
    pad = int(0.03 * small.shape[1])
    x0, y0 = max(0, x - pad), max(0, y - pad)
    x1, y1 = min(small.shape[1], x + cw + pad), min(small.shape[0], y + ch + pad)
    return gray[int(y0 / scale) : int(y1 / scale), int(x0 / scale) : int(x1 / scale)]


def _fix_line(line: str, kind: int) -> str:
    """Sửa nhầm thường gặp theo vị trí ô số (O↔0, I↔1, B↔8…)."""
    chars = list(line)
    if kind == 1:
        digit_pos = list(range(5, 15))
    elif kind == 2:
        digit_pos = [*range(0, 7), *range(8, 15), 29]
    else:
        digit_pos = []
    for i in digit_pos:
        if i < len(chars):
            chars[i] = chars[i].translate(_FIX_DIGIT)
    return "".join(chars)


def _rows(text: str) -> list[str] | None:
    rows = [re.sub(r"[^A-Z0-9<]", "", r.upper()) for r in text.splitlines()]
    rows = [r for r in rows if len(r) >= 20]
    for i, r in enumerate(rows):
        if r.startswith("ID") and i + 2 < len(rows):
            return [_fix_line(_pad(x), k + 1) for k, x in enumerate(rows[i : i + 3])]
    return None


def read(region: np.ndarray) -> list[str] | None:
    """OCR vùng MRZ (ảnh xám — nhị phân hóa dễ mất ký tự ``<``) → 3 dòng 30 ký tự đã sửa vị trí, hoặc ``None``."""
    import pytesseract  # nhập muộn: test không cần Tesseract vẫn chạy phần phân tích

    gray = region if region.ndim == 2 else cv2.cvtColor(region, cv2.COLOR_BGR2GRAY)
    config = "--psm 6 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<"
    candidates = [gray]
    if gray.shape[1] < 900:
        candidates.append(cv2.resize(gray, None, fx=2, fy=2, interpolation=cv2.INTER_CUBIC))
    for c in candidates:
        try:
            text = pytesseract.image_to_string(c, lang="eng", config=config)
        except pytesseract.TesseractError:
            return None
        rows = _rows(text)
        if rows:
            return rows
    return None


def read_image(image: np.ndarray) -> MrzData | None:
    """Thử vùng đã định vị rồi nửa dưới ảnh, ở 4 hướng; trả kết quả hợp lệ đầu tiên (hoặc kết quả đọc được)."""
    base = image if image.ndim == 2 else cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    found: MrzData | None = None
    for rot in (None, cv2.ROTATE_180, cv2.ROTATE_90_CLOCKWISE, cv2.ROTATE_90_COUNTERCLOCKWISE):
        gray = base if rot is None else cv2.rotate(base, rot)
        h = gray.shape[0]
        for r in (locate(gray), gray[int(h * 0.45) :, :]):
            if r is None or not r.size:
                continue
            lines = read(r)
            data = parse(lines) if lines else None
            if data and data.valid:
                return data
            found = found or data
        if found:
            return found
    return found
