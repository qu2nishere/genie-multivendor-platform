"""Cấu hình log JSON (python-json-logger). Không bao giờ ghi số CCCD, họ tên, URL ký sẵn, vector mặt (N4)."""

import logging

try:
    from pythonjsonlogger.json import JsonFormatter
except ImportError:  # python-json-logger < 3
    from pythonjsonlogger.jsonlogger import JsonFormatter


#: Thư viện tự ghi URL request ở mức INFO — URL ảnh KYC là link ký sẵn (bí mật tạm thời) nên phải tắt (N4)
NOISY_LOGGERS = ("httpx", "httpcore")


def quiet_third_party() -> None:
    for name in NOISY_LOGGERS:
        logging.getLogger(name).setLevel(logging.WARNING)


def configure_logging(level: int = logging.INFO) -> None:
    quiet_third_party()
    root = logging.getLogger()
    if any(getattr(h, "_genie", False) for h in root.handlers):
        return
    handler = logging.StreamHandler()
    handler.setFormatter(JsonFormatter("%(asctime)s %(levelname)s %(name)s %(message)s"))
    handler._genie = True  # type: ignore[attr-defined]
    root.addHandler(handler)
    root.setLevel(level)
