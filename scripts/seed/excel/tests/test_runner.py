# Đọc biến cho --apply / --flush-cache (plan 2026-11-21 E05): môi trường → .env (dòng sau thắng) → mặc định.
from __future__ import annotations

from xlseed import paths, runner


def test_env_value_order(tmp_path, monkeypatch):
    env = tmp_path / ".env"
    env.write_text("REDIS_PASSWORD=a\nexport REDIS_PASSWORD=\"b\"  # ghi chú\nOTHER=x\n", encoding="utf-8")
    monkeypatch.setattr(paths, "ENV_FILE", env)
    monkeypatch.delenv("REDIS_PASSWORD", raising=False)
    assert runner.env_value("REDIS_PASSWORD", "changeme") == "b"
    assert runner.env_value("KHONG_CO", "changeme") == "changeme"
    monkeypatch.setenv("REDIS_PASSWORD", "tu-moi-truong")
    assert runner.env_value("REDIS_PASSWORD", "changeme") == "tu-moi-truong"
