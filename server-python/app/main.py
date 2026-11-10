"""FastAPI AI Worker — lifespan nạp mô hình một lần khi khởi động."""

from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI

from app import health
from app.core.security import require_internal_token
from app.kyc import router as kyc_router
from app.sentiment import router as sentiment_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    # TODO: nạp mô hình KYC (YuNet, SFace) và PhoBERT ONNX; đặt health.state.models_loaded = True khi xong
    yield


app = FastAPI(title="Genie AI Worker", lifespan=lifespan)

app.include_router(health.router)
for module in (kyc_router, sentiment_router):
    if hasattr(module, "router"):
        app.include_router(module.router, dependencies=[Depends(require_internal_token)])
