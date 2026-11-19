"""POST /kyc/verify — nhận hồ sơ, trả 202 ngay, xử lý nền rồi gọi lại backend (plan AI Worker N7, Y06.05).

Token nội bộ kiểm ở ``main.py`` (``require_internal_token`` gắn cho cả router). Gọi lại luôn tới ``BACKEND_URL``
cấu hình — ``callback_url`` trong yêu cầu chỉ để đối chiếu, không dùng làm đích (N3).
"""

import logging

from fastapi import APIRouter, BackgroundTasks, Request, status

from app.core import callback
from app.core.config import get_settings
from app.kyc import service
from app.kyc.schemas import Checks, KycResultPayload, VerifyAccepted, VerifyRequest

log = logging.getLogger(__name__)
router = APIRouter(prefix="/kyc", tags=["kyc"])

INTERNAL_ERROR = "INTERNAL_ERROR"


async def process(req: VerifyRequest, models) -> None:
    settings = get_settings()
    try:
        payload = await service.verify(req, models, settings)
    except Exception:  # noqa: BLE001 — vẫn báo backend để hồ sơ không kẹt
        log.exception("kyc pipeline crashed", extra={"kyc_id": req.kyc_id})
        payload = KycResultPayload(checks=Checks(), errors=[INTERNAL_ERROR], model=service.model_info(models, settings))
    await callback.send_result(req.kyc_id, payload.model_dump(mode="json"), settings)


@router.post("/verify", status_code=status.HTTP_202_ACCEPTED, response_model=VerifyAccepted)
async def verify(body: VerifyRequest, request: Request, tasks: BackgroundTasks) -> VerifyAccepted:
    tasks.add_task(process, body, getattr(request.app.state, "face_models", None))
    return VerifyAccepted(kyc_id=body.kyc_id)
