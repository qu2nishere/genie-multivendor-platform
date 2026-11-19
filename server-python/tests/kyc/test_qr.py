"""Mã QR mặt trước (plan AI Worker Y03.05–Y03.07). Thẻ mẫu tự dựng — không phải giấy tờ thật."""

import cv2
import pytest

from app.kyc import qr, signals
from app.kyc.text import normalize_name
from tests.fixtures import build_card as card


@pytest.mark.parametrize("kw", [{}, {"rotate": 90}, {"rotate": 180}, {"rotate": 270}, {"scale": 0.6}, {"dark": 0.6}])
def test_sample_card_variants_are_decoded(kw):
    data = qr.parse(qr.decode(card.vary(card.front(), **kw)))
    assert data is not None
    assert data.identity_number == "079190000012"
    assert data.full_name == "Phạm Thị Lan"
    assert data.date_of_birth == "12041990"
    assert data.legacy_identity_number == "025123456"


def test_parse_rejects_bad_strings():
    good = card.qr_text()
    assert qr.parse(good) is not None
    assert qr.parse(None) is None
    assert qr.parse("079190000012|x|y") is None  # thiếu trường
    assert qr.parse(good.replace("12041990", "31021990")) is None  # 31/02 không tồn tại
    assert qr.parse(good.replace("079190000012", "07919000001")) is None  # 11 số
    assert qr.parse(good.replace("|Nữ|", "|Khác|")) is None
    assert qr.parse(good.replace("025123456", "")).legacy_identity_number is None


def test_check_classifies_missing_or_unreadable_qr():
    assert qr.check(card.front(), True).flag is True
    no_qr = qr.check(card.front(with_qr=False), True)
    assert (no_qr.flag, no_qr.signal) == (False, signals.LEGACY_CARD)
    blurry = cv2.GaussianBlur(card.front(), (41, 41), 15)
    res = qr.check(blurry, quality_ok=False)
    assert (res.flag, res.signal) == (None, signals.UNREADABLE_IMAGE)
    assert qr.check(None, True).signal == signals.UNREADABLE_IMAGE


@pytest.mark.parametrize(
    ("raw", "expected"),
    [
        ("Phạm Thị Lan", "PHAM THI LAN"),
        ("  Đặng   Quốc  Huy ", "DANG QUOC HUY"),
        ("nguyễn văn a", "NGUYEN VAN A"),
        ("", ""),
    ],
)
def test_name_normalization_matches_backend(raw, expected):
    assert normalize_name(raw) == expected


def test_decode_rate_on_variants_is_at_least_90_percent():
    base = card.front()
    variants = [
        card.vary(base, rotate=r, scale=s, dark=d, blur=b)
        for r in (0, 90, 180, 270)
        for s in (1.0, 0.75)
        for d in (1.0, 0.7)
        for b in (0, 3)
    ]
    assert len(variants) >= 30
    ok = sum(qr.parse(qr.decode(v)) is not None for v in variants)
    rate = ok / len(variants)
    print(f"QR decode rate: {ok}/{len(variants)} = {rate:.0%}")
    assert rate >= 0.9
