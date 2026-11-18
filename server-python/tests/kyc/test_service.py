"""Pipeline đầy đủ trên thẻ mẫu (plan AI Worker Y06.07–Y06.08)."""

import asyncio
import logging

import httpx
import respx

from app.core.config import Settings
from app.kyc import qr, service
from app.kyc.schemas import VerifyRequest
from tests.fixtures import build_card as card

S = Settings(storage_allowed_hosts="silo:9000", kyc_max_concurrency=1, pipeline_version="test")
BASE = "http://silo:9000/kyc-dossiers/tmp/view"


def _req(kyc_id: int = 1, consent: bool = True) -> VerifyRequest:
    return VerifyRequest.model_validate(
        {
            "kyc_id": kyc_id,
            "documents": {
                "id_front": f"{BASE}/f.jpg?sig=a",
                "id_back": f"{BASE}/b.jpg?sig=b",
                "selfie": f"{BASE}/s.jpg?sig=c",
            },
            "declared": {"identity_number": "079190000012", "full_name": "Phạm Thị Lan"},
            "biometric_consent": consent,
        }
    )


def _serve(back_status: int = 200) -> None:
    jpg = {"content-type": "image/jpeg"}
    respx.get(f"{BASE}/f.jpg").respond(200, content=card.jpeg(card.front()), headers=jpg)
    respx.get(f"{BASE}/b.jpg").respond(back_status, content=card.jpeg(card.back()), headers=jpg)
    respx.get(f"{BASE}/s.jpg").respond(200, content=card.jpeg(card.front()), headers=jpg)


@respx.mock
async def test_sample_card_passes_tier_one_without_models():
    _serve()
    p = await service.verify(_req(), None, S)
    c = p.checks
    assert (c.qr_code, c.mrz, c.mrz_qr_match, c.cccd_number_valid, c.expiry_rule_valid) == (
        True,
        True,
        True,
        True,
        True,
    )
    assert c.face_match is None and "FACE_MODEL_UNAVAILABLE" in p.errors
    assert [s.code for s in p.signals] == []
    assert p.extracted.identity_number == "079190000012"
    assert p.model.pipeline_version == "test"


@respx.mock
async def test_failed_download_still_produces_a_result():
    _serve(back_status=404)
    p = await service.verify(_req(), None, S)
    assert "UNREADABLE_IMAGE" in [s.code for s in p.signals]
    assert p.checks.mrz is None
    assert p.checks.qr_code is True  # các bước khác vẫn chạy
    assert "ID_BACK_HTTP_404" in p.errors


@respx.mock
async def test_one_step_crashing_does_not_stop_the_others(monkeypatch):
    _serve()

    def boom(*_a, **_k):
        raise RuntimeError("bug")

    monkeypatch.setattr(qr, "check", boom)
    p = await service.verify(_req(), None, S)
    assert "QR_FAILED" in p.errors
    assert p.checks.mrz is True


@respx.mock
async def test_timeout_returns_partial_result(monkeypatch):
    _serve()
    monkeypatch.setattr(qr, "check", lambda *_a: __import__("time").sleep(1.5))
    p = await service.verify(_req(), None, Settings(storage_allowed_hosts="silo:9000", kyc_pipeline_timeout_s=0.5))
    assert "PIPELINE_TIMEOUT" in p.errors


@respx.mock
async def test_concurrent_requests_run_one_at_a_time(monkeypatch):
    _serve()
    running, peak = 0, 0
    real = service._run

    async def tracked(*a, **k):
        nonlocal running, peak
        running += 1
        peak = max(peak, running)
        try:
            await asyncio.sleep(0.05)
            await real(*a, **k)
        finally:
            running -= 1

    monkeypatch.setattr(service, "_run", tracked)
    async with httpx.AsyncClient() as client:
        await asyncio.gather(service.verify(_req(1), None, S, client), service.verify(_req(2), None, S, client))
    assert peak == 1


@respx.mock
async def test_logs_contain_no_personal_data(caplog):
    _serve()
    caplog.set_level(logging.DEBUG)
    await service.verify(_req(), None, S)
    text = " ".join(f"{r.getMessage()} {r.__dict__}" for r in caplog.records)
    for secret in ("079190000012", "Phạm", "PHAM THI LAN", "sig=a", "http://silo"):
        assert secret not in text
    assert any(getattr(r, "kyc_id", None) == 1 for r in caplog.records)
