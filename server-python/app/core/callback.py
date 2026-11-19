"""Gọi lại backend ``PATCH /api/v1/internal/kyc/{id}/result`` kèm ``X-Internal-Token`` (plan AI Worker N3, N7, Y02.04).

Luôn gọi đúng ``BACKEND_URL`` cấu hình (không dùng host trong ``callback_url``). Thử lại tối đa ``CALLBACK_RETRIES`` lần
(1 s / 2 s / 4 s) với lỗi mạng và 5xx; 4xx không thử lại. Không ném lỗi ra luồng nền, không log nội dung thân.
"""

import asyncio
import logging
from collections.abc import Awaitable, Callable

import httpx

from app.core.config import Settings

log = logging.getLogger(__name__)


async def send_result(
    kyc_id: int,
    payload: dict,
    settings: Settings,
    client: httpx.AsyncClient | None = None,
    sleep: Callable[[float], Awaitable[None]] = asyncio.sleep,
) -> bool:
    url = f"{settings.backend_url.rstrip('/')}/api/v1/internal/kyc/{kyc_id}/result"
    headers = {"X-Internal-Token": settings.internal_token}
    own = client is None
    client = client or httpx.AsyncClient(timeout=settings.callback_timeout_s)
    try:
        for attempt in range(settings.callback_retries + 1):
            try:
                r = await client.patch(url, json=payload, headers=headers, timeout=settings.callback_timeout_s)
                if r.status_code < 400:
                    extra = {"kyc_id": kyc_id, "status": r.status_code, "attempt": attempt + 1}
                    log.info("kyc callback ok", extra=extra)
                    return True
                if r.status_code < 500:
                    log.error("kyc callback bị từ chối", extra={"kyc_id": kyc_id, "status": r.status_code})
                    return False
                reason = f"HTTP_{r.status_code}"
            except httpx.HTTPError as e:
                reason = type(e).__name__
            if attempt < settings.callback_retries:
                await sleep(2**attempt)
        log.error("kyc callback thất bại sau khi thử lại", extra={"kyc_id": kyc_id, "reason": reason})
        return False
    finally:
        if own:
            await client.aclose()
