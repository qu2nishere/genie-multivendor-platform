"""Chuẩn hóa chuỗi — cùng quy tắc ``NameNormalizer`` của backend (bỏ dấu kể cả đ/Đ, viết hoa, gộp khoảng trắng)."""

import re
import unicodedata


def normalize_name(name: str | None) -> str:
    if not name:
        return ""
    no_marks = "".join(c for c in unicodedata.normalize("NFD", name) if unicodedata.category(c) != "Mn")
    no_marks = no_marks.replace("đ", "d").replace("Đ", "D")
    return re.sub(r"\s+", " ", no_marks.upper()).strip()
