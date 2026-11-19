"""MRZ TD1 mặt sau (plan AI Worker Y04.05–Y04.06). Thẻ mẫu tự dựng — không phải giấy tờ thật."""

import shutil

import numpy as np
import pytest

from app.kyc import mrz
from tests.fixtures import build_card as card

needs_tesseract = pytest.mark.skipif(shutil.which("tesseract") is None, reason="cần tesseract-ocr")


@pytest.mark.parametrize(
    ("field", "digit"),
    [("520727", "3"), ("D23145890", "7"), ("L898902C3", "6"), ("740812", "2"), ("120415", "9"), ("<<<<<<", "0")],
)
def test_check_digits_follow_icao_9303(field, digit):
    assert mrz.check_digit(field) == digit


def test_build_and_parse_round_trip():
    lines = card.mrz_lines()
    assert all(len(x) == 30 for x in lines)
    data = mrz.parse(lines)
    assert data is not None and data.valid
    assert data.identity_number == "079190000012"
    assert data.document_number == "190000012"
    assert (data.date_of_birth, data.sex, data.expiry_date, data.nationality) == ("900412", "F", "300412", "VNM")
    assert data.full_name == "PHAM THI LAN"


def test_one_changed_character_breaks_the_check_digit():
    lines = card.mrz_lines()
    tampered = [lines[0], lines[1].replace("900412", "900413", 1), lines[2]]
    assert mrz.parse(tampered).valid is False
    assert mrz.parse(lines[:2]) is None
    assert mrz.parse([lines[0][:-1], lines[1], lines[2]]) is None


@needs_tesseract
def test_reads_sample_card_back():
    data = mrz.read_image(card.back())
    assert data is not None and data.valid
    assert data.full_name == "PHAM THI LAN"


@needs_tesseract
def test_image_without_mrz_returns_none():
    blank = np.full((638, 1012, 3), 235, np.uint8)
    assert mrz.read_image(blank) is None


@needs_tesseract
def test_read_rate_on_variants():
    base = card.back()
    variants = [
        card.vary(base, rotate=r, scale=s, dark=d, blur=b)
        for r in (0, 180)
        for s in (1.0, 0.8, 0.6)
        for d in (1.0, 0.7)
        for b in (0, 3, 5)
    ]
    assert len(variants) >= 30
    ok = 0
    for v in variants:
        data = mrz.read_image(v)
        ok += bool(data and data.valid)
    rate = ok / len(variants)
    print(f"MRZ read rate: {ok}/{len(variants)} = {rate:.0%}")
    assert rate >= 0.8
