"""FastAPI AI Worker — lifespan nạp mô hình một lần khi khởi động (plan AI Worker N6, Y01.05)."""

from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI

from app import health
from app.core.config import get_settings
from app.core.logging import configure_logging
from app.core.security import require_internal_token
from app.kyc import models as kyc_models
from app.kyc import router as kyc_router
from app.sentiment import router as sentiment_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    configure_logging()
    app.state.face_models = kyc_models.load(get_settings().models_dir)
    health.state.models_loaded = app.state.face_models is not None
    health.state.started = True
    # TODO (plan cảm xúc đánh giá): nạp PhoBERT ONNX
    yield
    health.state.started = False


app = FastAPI(title="Genie AI Worker", lifespan=lifespan)

app.include_router(health.router)
for module in (kyc_router, sentiment_router):
    if hasattr(module, "router"):
        app.include_router(module.router, dependencies=[Depends(require_internal_token)])
