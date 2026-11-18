"""Cấu hình log JSON (python-json-logger). Không bao giờ ghi số CCCD, họ tên, URL ký sẵn, vector mặt (N4)."""

import logging

try:
    from pythonjsonlogger.json import JsonFormatter
except ImportError:  # python-json-logger < 3
    from pythonjsonlogger.jsonlogger import JsonFormatter


def configure_logging(level: int = logging.INFO) -> None:
    root = logging.getLogger()
    if any(getattr(h, "_genie", False) for h in root.handlers):
        return
    handler = logging.StreamHandler()
    handler.setFormatter(JsonFormatter("%(asctime)s %(levelname)s %(name)s %(message)s"))
    handler._genie = True  # type: ignore[attr-defined]
    root.addHandler(handler)
    root.setLevel(level)
