"""POST /kyc/verify (plan AI Worker Y06.06)."""

import json
from pathlib import Path

import pytest
import respx
from fastapi.testclient import TestClient

from app.core.config import get_settings
from app.kyc.schemas import KycResultPayload
from app.main import app
from tests.fixtures import build_card as card

CONTRACT = Path(__file__).resolve().parents[3] / "docs" / "api-tests" / "kyc-contract"
BACKEND = "http://backend:8080/api/v1/internal/kyc/1042/result"


@pytest.fixture
def worker(monkeypatch):
    monkeypatch.setenv("INTERNAL_TOKEN", "t0k")
    monkeypatch.setenv("BACKEND_URL", "http://backend:8080")
    monkeypatch.setenv("STORAGE_ALLOWED_HOSTS", "localhost:9000")
    get_settings.cache_clear()
    with TestClient(app) as c:
        yield c
    get_settings.cache_clear()


def _body() -> dict:
    return json.loads((CONTRACT / "verify-request.json").read_text(encoding="utf-8"))


def test_requires_internal_token(worker):
    assert worker.post("/kyc/verify", json=_body()).status_code == 401
    assert worker.post("/kyc/verify", json=_body(), headers={"X-Internal-Token": "sai"}).status_code == 401


def test_rejects_invalid_body(worker):
    r = worker.post("/kyc/verify", json={**_body(), "kyc_id": -1}, headers={"X-Internal-Token": "t0k"})
    assert r.status_code == 422


@respx.mock
def test_accepts_and_calls_back_once_with_a_valid_payload(worker):
    jpg = {"content-type": "image/jpeg"}
    body = _body()
    respx.get(url__startswith=str(body["documents"]["id_front"]).split("?")[0]).respond(
        200, content=card.jpeg(card.front()), headers=jpg
    )
    respx.get(url__startswith=str(body["documents"]["id_back"]).split("?")[0]).respond(
        200, content=card.jpeg(card.back()), headers=jpg
    )
    respx.get(url__startswith=str(body["documents"]["selfie"]).split("?")[0]).respond(
        200, content=card.jpeg(card.front()), headers=jpg
    )
    cb = respx.patch(BACKEND).respond(200, json={"data": {"kyc_status": "VERIFIED"}})
    # callback_url trỏ host lạ — worker vẫn chỉ gọi BACKEND_URL cấu hình
    body["callback_url"] = "http://evil.example/steal"
    r = worker.post("/kyc/verify", json=body, headers={"X-Internal-Token": "t0k"})
    assert r.status_code == 202
    assert r.json() == {"kyc_id": 1042, "accepted": True}
    assert cb.call_count == 1
    sent = json.loads(cb.calls[0].request.content)
    KycResultPayload.model_validate(sent)
    assert sent["checks"]["qr_code"] is True
    assert cb.calls[0].request.headers["X-Internal-Token"] == "t0k"
