"""Kiểm header token nội bộ — AI Worker chỉ nhận request từ backend trong mạng Compose."""

import secrets

from fastapi import Header, HTTPException, status

from app.core.config import get_settings


def require_internal_token(x_internal_token: str = Header(default="")) -> None:
    expected = get_settings().internal_token
    if not expected or not secrets.compare_digest(x_internal_token, expected):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="invalid internal token")
