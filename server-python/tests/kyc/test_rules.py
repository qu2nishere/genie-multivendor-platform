"""Luật đối chiếu số CCCD, hạn thẻ, QR ↔ MRZ (plan AI Worker Y04.10)."""

from dataclasses import replace
from datetime import date

import pytest

from app.kyc import mrz, qr, rules
from tests.fixtures import build_card as card


@pytest.mark.parametrize(
    ("number", "dob", "gender", "expected"),
    [
        ("079190000012", "12041990", "Nữ", True),
        ("079090000012", "12041990", "Nam", True),
        ("001203000123", "05062003", "Nam", True),
        ("001303000123", "05062003", "Nữ", True),
        ("003190000012", "12041990", "Nữ", False),  # 003 không phải mã tỉnh
        ("079090000012", "12041990", "Nữ", False),  # sai giới tính
        ("079191000012", "12041990", "Nữ", False),  # sai năm sinh
        ("079290000012", "12041990", "Nữ", False),  # sai thế kỷ
        ("079190000012", None, "Nữ", None),
        ("0791900", "12041990", "Nữ", None),
    ],
)
def test_id_structure(number, dob, gender, expected):
    assert rules.check_id_structure(number, dob, gender) is expected


@pytest.mark.parametrize(
    ("dob", "issued", "expected"),
    [
        (date(2005, 6, 1), date(2021, 3, 1), date(2030, 6, 1)),  # cấp lúc 15 tuổi → mốc 25
        (date(1999, 6, 1), date(2022, 7, 1), date(2039, 6, 1)),  # 23 tuổi 1 tháng (trong 2 năm trước 25) → mốc 40
        (date(1999, 6, 1), date(2022, 5, 31), date(2024, 6, 1)),  # ngày trước mốc 2 năm → vẫn 25
        (date(1990, 4, 12), date(2021, 5, 10), date(2030, 4, 12)),  # 31 tuổi → 40
        (date(1970, 1, 1), date(2021, 1, 1), date(2030, 1, 1)),  # 51 tuổi → 60
        (date(1960, 1, 1), date(2021, 1, 1), None),  # 61 tuổi → không thời hạn
    ],
)
def test_expected_expiry_milestones(dob, issued, expected):
    assert rules.expected_expiry(dob, issued) == expected


def test_check_expiry():
    assert rules.check_expiry("12041990", "10052021", "300412") is True
    assert rules.check_expiry("12041990", "10052021", "350412") is False
    assert rules.check_expiry("01011960", "01012021", "<<<<<<") is True
    assert rules.check_expiry(None, "10052021", "300412") is None


def test_qr_mrz_match():
    q = qr.parse(card.qr_text())
    m = mrz.parse(card.mrz_lines())
    assert rules.match_qr_mrz(q, m) is True
    assert rules.match_qr_mrz(replace(q, full_name="Phạm Thị Lâm"), m) is False
    assert rules.match_qr_mrz(replace(q, date_of_birth="13041990"), m) is False
    assert rules.match_qr_mrz(replace(q, gender="Nam"), m) is False
    assert rules.match_qr_mrz(q, replace(m, identity_number="079190000013")) is False
    assert rules.match_qr_mrz(None, m) is None
    assert rules.match_qr_mrz(q, None) is None
