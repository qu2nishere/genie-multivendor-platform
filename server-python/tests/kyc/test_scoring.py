"""Gom kết quả → checks + signals, không tính điểm (plan AI Worker Y06.02)."""

import json
from pathlib import Path

from app.kyc import mrz, qr, signals
from app.kyc.face import FaceResult
from app.kyc.schemas import ModelInfo
from app.kyc.scoring import Steps, assemble
from tests.fixtures import build_card as card

MODEL = ModelInfo(yunet="y", sface="s", pipeline_version="test")
CONTRACT = Path(__file__).resolve().parents[3] / "docs" / "api-tests" / "kyc-contract"


def _clean() -> Steps:
    q = qr.QrCheck(qr.parse(card.qr_text()), True, None)
    return Steps(
        qr=q,
        mrz=mrz.parse(card.mrz_lines()),
        mrz_attempted=True,
        cccd_number_valid=True,
        expiry_rule_valid=True,
        mrz_qr_match=True,
        face=FaceResult(True, 0.91, 0.95),
    )


def _codes(steps: Steps) -> list[str]:
    return [s.code for s in assemble(steps, MODEL).signals]


def test_clean_dossier_has_no_signals_and_full_extraction():
    p = assemble(_clean(), MODEL)
    assert p.signals == []
    assert p.checks.qr_code and p.checks.mrz and p.checks.mrz_qr_match
    assert p.extracted.identity_number == "079190000012"
    assert p.extracted.full_name == "Phạm Thị Lan"
    assert p.extracted.expiry_date == "12042030"
    assert p.checks.face_duplicate is None and p.checks.national_emblem is None


def test_each_signal_and_combinations_in_fixed_order():
    s = _clean()
    s.mrz_qr_match = False
    s.cccd_number_valid = False
    s.expiry_rule_valid = False
    s.face = FaceResult(False, 0.3, 0.2, [signals.FACE_SCORE_BORDERLINE, signals.LIVENESS_SUSPECT])
    assert _codes(s) == [
        signals.QR_MRZ_MISMATCH,
        signals.ID_STRUCTURE_MISMATCH,
        signals.EXPIRY_RULE_MISMATCH,
        signals.FACE_SCORE_BORDERLINE,
        signals.LIVENESS_SUSPECT,
    ]
    legacy = _clean()
    legacy.qr = qr.QrCheck(None, False, signals.LEGACY_CARD)
    assert _codes(legacy) == [signals.LEGACY_CARD]


def test_unreadable_image_appears_once_even_if_several_images_fail():
    s = _clean()
    s.unreadable = True
    s.qr = qr.QrCheck(None, None, signals.UNREADABLE_IMAGE)
    s.mrz = None
    assert _codes(s) == [signals.UNREADABLE_IMAGE]
    p = assemble(s, MODEL)
    assert p.checks.mrz is None and p.checks.qr_code is None
    assert p.extracted.identity_number is None


def test_extraction_falls_back_to_mrz_when_qr_missing():
    s = _clean()
    s.qr = qr.QrCheck(None, False, signals.LEGACY_CARD)
    p = assemble(s, MODEL)
    assert p.extracted.identity_number == "079190000012"
    assert p.extracted.full_name == "PHAM THI LAN"
    assert p.extracted.date_of_birth == "12041990"
    assert p.extracted.gender == "Nữ"


def test_output_has_the_same_shape_as_contract_samples():
    sample = json.loads((CONTRACT / "result-pass.json").read_text(encoding="utf-8"))
    out = assemble(_clean(), MODEL).model_dump(mode="json")
    assert set(out) == set(sample)
    assert set(out["checks"]) == set(sample["checks"])
    assert set(out["extracted"]) == set(sample["extracted"])
