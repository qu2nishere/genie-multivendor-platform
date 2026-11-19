"""Tầng 1 — luật đối chiếu: cấu trúc số CCCD, quy tắc hạn thẻ, QR ↔ MRZ (plan AI Worker Y04.07–Y04.09).

Mỗi hàm trả ``True / False / None`` — ``None`` = thiếu dữ liệu, KHÔNG sinh tín hiệu lệch (N2).
"""

from datetime import date
from functools import lru_cache
from pathlib import Path

from app.kyc.mrz import MrzData
from app.kyc.qr import QrData
from app.kyc.text import normalize_name

PROVINCE_FILE = Path(__file__).resolve().parents[2] / "resources" / "province_codes.txt"


@lru_cache
def province_codes() -> frozenset[str]:
    lines = PROVINCE_FILE.read_text(encoding="utf-8").splitlines()
    return frozenset(line.split()[0] for line in lines if line.strip() and not line.startswith("#"))


def _ddmmyyyy(s: str | None) -> date | None:
    if not s or len(s) != 8 or not s.isdigit():
        return None
    try:
        return date(int(s[4:]), int(s[2:4]), int(s[:2]))
    except ValueError:
        return None


def _yymmdd(s: str | None, pivot_year: int) -> date | None:
    """YYMMDD → date; thế kỷ chọn sao cho gần ``pivot_year`` nhất về phía sau (hạn thẻ) hoặc trước (ngày sinh)."""
    if not s or len(s) != 6 or not s.isdigit():
        return None
    yy = int(s[:2])
    year = 2000 + yy if 2000 + yy <= pivot_year + 50 else 1900 + yy
    try:
        return date(year, int(s[2:4]), int(s[4:]))
    except ValueError:
        return None


def check_id_structure(identity_number: str | None, date_of_birth: str | None, gender: str | None) -> bool | None:
    """3 số đầu = mã tỉnh; số thứ 4 = giới tính + thế kỷ (0/1: 1900s, 2/3: 2000s, 4/5: 2100s; chẵn = nam);
    số 5–6 = 2 số cuối năm sinh (Thông tư 07/2016/TT-BCA)."""
    dob = _ddmmyyyy(date_of_birth)
    if not identity_number or len(identity_number) != 12 or not identity_number.isdigit() or dob is None:
        return None
    if identity_number[:3] not in province_codes():
        return False
    century_code = (dob.year // 100 - 19) * 2
    if century_code < 0:
        return False
    expected = century_code + (0 if gender == "Nam" else 1 if gender == "Nữ" else -100)
    return int(identity_number[3]) == expected and identity_number[4:6] == f"{dob.year % 100:02d}"


def _birthday(dob: date, age: int) -> date:
    try:
        return dob.replace(year=dob.year + age)
    except ValueError:  # 29/02
        return date(dob.year + age, 3, 1)


def expected_expiry(dob: date, issued: date) -> date | None:
    """Hạn thẻ = sinh nhật mốc 25 / 40 / 60 kế tiếp sau ngày cấp; cấp trong 2 năm trước một mốc thì dùng mốc sau;
    cấp từ 58 tuổi trở lên → không thời hạn (``None``) — Luật Căn cước 2023, Điều 21."""
    for age in (25, 40, 60):
        milestone = _birthday(dob, age)
        if issued < _birthday(dob, age - 2):
            return milestone
    return None


def check_expiry(date_of_birth: str | None, issue_date: str | None, expiry_yymmdd: str | None) -> bool | None:
    dob, issued = _ddmmyyyy(date_of_birth), _ddmmyyyy(issue_date)
    if dob is None or issued is None or not expiry_yymmdd:
        return None
    expected = expected_expiry(dob, issued)
    if expected is None:
        # không thời hạn: MRZ thường ghi "<<<<<<" hoặc ngày rất xa
        return not expiry_yymmdd.isdigit() or (_yymmdd(expiry_yymmdd, issued.year) or date.max) >= date(2090, 1, 1)
    actual = _yymmdd(expiry_yymmdd, issued.year)
    return actual == expected


def match_qr_mrz(q: QrData | None, m: MrzData | None) -> bool | None:
    """So số CCCD, ngày sinh, giới tính, họ tên đã chuẩn hóa; thiếu một bên → ``None``."""
    if q is None or m is None:
        return None
    dob = _ddmmyyyy(q.date_of_birth)
    mrz_dob = _yymmdd(m.date_of_birth, 2000)
    same_dob = (
        dob is not None
        and mrz_dob is not None
        and (dob.month, dob.day, dob.year % 100)
        == (
            mrz_dob.month,
            mrz_dob.day,
            mrz_dob.year % 100,
        )
    )
    same_id = (
        m.identity_number == q.identity_number if m.identity_number else q.identity_number.endswith(m.document_number)
    )
    same_sex = (m.sex == "M" and q.gender == "Nam") or (m.sex == "F" and q.gender == "Nữ")
    same_name = normalize_name(q.full_name) == normalize_name(m.full_name)
    return same_id and same_dob and same_sex and same_name
