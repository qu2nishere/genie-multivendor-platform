"""Lược đồ hợp đồng đọc đúng 4 tệp mẫu dùng chung với backend (plan AI Worker Y01.02, Y01.04)."""

import json
from pathlib import Path

import pytest
from pydantic import ValidationError

from app.kyc import signals
from app.kyc.schemas import KycResultPayload, VerifyRequest

CONTRACT = Path(__file__).resolve().parents[3] / "docs" / "api-tests" / "kyc-contract"


def _load(name: str) -> dict:
    return json.loads((CONTRACT / name).read_text(encoding="utf-8"))


def test_verify_request_sample_is_valid():
    req = VerifyRequest.model_validate(_load("verify-request.json"))
    assert req.kyc_id == 1042
    assert req.biometric_consent is True


@pytest.mark.parametrize("name", ["result-pass.json", "result-partial.json", "result-fail.json"])
def test_result_samples_round_trip_in_snake_case(name):
    raw = _load(name)
    payload = KycResultPayload.model_validate(raw)
    out = payload.model_dump(mode="json")
    assert out["checks"]["face_duplicate"] is None  # giữ null
    assert set(out) == set(raw)
    assert {s["code"] for s in out["signals"]} <= signals.ALL


def test_sample_signals_are_known_codes():
    codes = {s["code"] for n in ("result-partial.json", "result-fail.json") for s in _load(n)["signals"]}
    assert codes <= signals.ALL


def test_rejects_bad_requests():
    good = _load("verify-request.json")
    for broken in (
        {**good, "kyc_id": 0},
        {**good, "declared": {"identity_number": "123"}},
        {**good, "extra": 1},
        {k: v for k, v in good.items() if k != "documents"},
        {**good, "documents": {**good["documents"], "selfie": "ftp://x/y.jpg"}},
    ):
        with pytest.raises(ValidationError):
            VerifyRequest.model_validate(broken)
    with pytest.raises(ValidationError):
        KycResultPayload.model_validate({**_load("result-pass.json"), "signals": [{"code": "DUPLICATE_ID"}]})
