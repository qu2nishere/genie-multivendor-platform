"""Điều phối pipeline KYC (plan AI Worker Y06.03–Y06.04): tải 3 ảnh song song → chất lượng → QR → MRZ → luật → mặt.

- Mỗi bước bọc ``try`` riêng — một bước hỏng không chặn bước khác (N2);
- phần CPU chạy trong thread pool, tối đa ``KYC_MAX_CONCURRENCY`` hồ sơ cùng lúc (N6);
- quá ``KYC_PIPELINE_TIMEOUT_S`` → trả phần đã có + ``PIPELINE_TIMEOUT`` (backend sẽ chuyển duyệt tay);
- log chỉ có ``kyc_id``, thời gian từng bước, mã tín hiệu (N4).
"""

import asyncio
import logging
import time

import httpx
import numpy as np

from app.core import images
from app.core.config import Settings
from app.core.logging import quiet_third_party
from app.kyc import face, mrz, qr, rules
from app.kyc.models import FaceModels
from app.kyc.schemas import KycResultPayload, ModelInfo, VerifyRequest
from app.kyc.scoring import Steps, assemble

log = logging.getLogger(__name__)
quiet_third_party()

PIPELINE_TIMEOUT = "PIPELINE_TIMEOUT"


_SEMAPHORES: dict[tuple[int, int], asyncio.Semaphore] = {}


def _semaphore(limit: int) -> asyncio.Semaphore:
    """Một semaphore cho mỗi event loop (uvicorn chỉ có 1 loop; test tạo loop mới mỗi ca)."""
    key = (id(asyncio.get_running_loop()), limit)
    if key not in _SEMAPHORES:
        _SEMAPHORES[key] = asyncio.Semaphore(limit)
    return _SEMAPHORES[key]


def model_info(models: FaceModels | None, settings: Settings) -> ModelInfo:
    return ModelInfo(
        yunet=models.yunet if models else None,
        sface=models.sface if models else None,
        pipeline_version=settings.pipeline_version,
    )


async def _load(client: httpx.AsyncClient, url: str, settings: Settings) -> np.ndarray:
    data = await images.fetch_bytes(client, url, settings)
    return await asyncio.to_thread(images.decode, data)


async def _run(
    req: VerifyRequest,
    models: FaceModels | None,
    settings: Settings,
    client: httpx.AsyncClient,
    steps: Steps,
    timings: dict[str, int],
) -> None:
    t = time.perf_counter()
    urls = (str(req.documents.id_front), str(req.documents.id_back), str(req.documents.selfie))
    loaded = await asyncio.gather(*(_load(client, u, settings) for u in urls), return_exceptions=True)
    front, back, selfie = (None if isinstance(x, BaseException) else x for x in loaded)
    for name, x in zip(("ID_FRONT", "ID_BACK", "SELFIE"), loaded, strict=True):
        if isinstance(x, BaseException):
            steps.unreadable = True
            steps.errors.append(f"{name}_{x.args[0] if isinstance(x, images.ImageError) else 'LOAD_FAILED'}")
    timings["download_ms"] = round((time.perf_counter() - t) * 1000)

    quality_ok = {}
    for name, img in (("front", front), ("back", back)):
        if img is not None:
            q = images.assess_quality(img, settings)
            quality_ok[name] = q.ok
            if not q.ok:
                steps.unreadable = True

    async def step(name: str, fn) -> None:
        t0 = time.perf_counter()
        try:
            await asyncio.to_thread(fn)
        except Exception:  # noqa: BLE001 — một bước hỏng không làm hỏng cả pipeline (N2)
            log.exception("kyc step failed", extra={"kyc_id": req.kyc_id, "step": name})
            steps.errors.append(f"{name.upper()}_FAILED")
        timings[f"{name}_ms"] = round((time.perf_counter() - t0) * 1000)

    def do_qr() -> None:
        steps.qr = qr.check(front, quality_ok.get("front", False))

    def do_mrz() -> None:
        steps.mrz_attempted = True
        steps.mrz = mrz.read_image(back) if back is not None else None

    def do_rules() -> None:
        q = steps.qr.data if steps.qr else None
        m = steps.mrz if steps.mrz and steps.mrz.valid else None
        steps.mrz_qr_match = rules.match_qr_mrz(q, m)
        identity = q.identity_number if q else (m.identity_number if m else None)
        dob = q.date_of_birth if q else None
        gender = q.gender if q else ({"M": "Nam", "F": "Nữ"}.get(m.sex) if m else None)
        steps.cccd_number_valid = rules.check_id_structure(identity, dob, gender)
        steps.expiry_rule_valid = rules.check_expiry(dob, q.issue_date if q else None, m.expiry_date if m else None)

    def do_face() -> None:
        steps.face = face.run(models, front, selfie, req.biometric_consent, settings)

    await step("qr", do_qr)
    await step("mrz", do_mrz)
    await step("rules", do_rules)
    await step("face", do_face)


async def verify(
    req: VerifyRequest, models: FaceModels | None, settings: Settings, client: httpx.AsyncClient | None = None
) -> KycResultPayload:
    steps = Steps()
    timings: dict[str, int] = {}
    started = time.perf_counter()
    own = client is None
    client = client or httpx.AsyncClient()
    try:
        async with _semaphore(settings.kyc_max_concurrency):
            try:
                await asyncio.wait_for(
                    _run(req, models, settings, client, steps, timings), settings.kyc_pipeline_timeout_s
                )
            except TimeoutError:
                steps.errors.append(PIPELINE_TIMEOUT)
    finally:
        if own:
            await client.aclose()
    payload = assemble(steps, model_info(models, settings))
    log.info(
        "kyc pipeline done",
        extra={
            "kyc_id": req.kyc_id,
            "total_ms": round((time.perf_counter() - started) * 1000),
            **timings,
            "signals": [s.code for s in payload.signals],
        },
    )
    return payload
