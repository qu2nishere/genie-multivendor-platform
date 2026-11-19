"""GET /health — worker sẵn sàng sau lifespan; ``models_loaded`` cho biết bước khuôn mặt có chạy được không.

Thiếu mô hình worker vẫn phục vụ tầng 1 (QR, MRZ, luật) — bước khuôn mặt trả ``null`` (plan AI Worker Y01.05–Y01.06).
"""

from dataclasses import dataclass

from fastapi import APIRouter, Response, status

from app.core.config import get_settings


@dataclass
class _State:
    started: bool = False
    models_loaded: bool = False


state = _State()
router = APIRouter()


@router.get("/health")
def health(response: Response) -> dict:
    body = {"models_loaded": state.models_loaded, "pipeline_version": get_settings().pipeline_version}
    if not state.started:
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
        return {"status": "LOADING", **body}
    return {"status": "UP", **body}
