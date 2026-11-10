"""GET /health — chỉ báo sẵn sàng khi mô hình đã nạp xong."""

from dataclasses import dataclass

from fastapi import APIRouter, Response, status


@dataclass
class _State:
    models_loaded: bool = False


state = _State()
router = APIRouter()


@router.get("/health")
def health(response: Response) -> dict:
    if not state.models_loaded:
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
        return {"status": "LOADING"}
    return {"status": "UP"}
