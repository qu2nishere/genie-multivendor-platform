# Nạp seed Excel vào máy dev sau khi build (plan 2026-11-21 E05.03, E05.04; X1):
#   --apply        upload-assets (đẩy assets/ lên Silo + kiểm ảnh) → seed.sh --docker --only 95 (Windows: seed.ps1)
#   --flush-cache  xóa cache catalog trong Redis Docker (trang sản phẩm 5 phút, trang shop 10 phút)
# Công cụ KHÔNG tự nói chuyện với CSDL / S3: dùng lại hai script có sẵn để giữ lớp chặn host local / chặn AWS (ADR 0010).
from __future__ import annotations

import os
import re
import subprocess

from . import paths

# Khóa cache (JsonCache.PREFIX = "cache:") — ProductDetailService "catalog:product:", Similar / Reco / Trending
# "catalog:…", PublicShopService "vendor:shop:"
CACHE_PATTERNS = ("cache:catalog:*", "cache:vendor:shop:*")


def env_value(name: str, default: str) -> str:
    """Biến môi trường → .env gốc repo (dòng sau thắng) → mặc định — cùng thứ tự với seed.sh / upload-assets.sh."""
    value = os.environ.get(name)
    if value:
        return value
    if paths.ENV_FILE.exists():
        found = None
        for line in paths.ENV_FILE.read_text(encoding="utf-8").splitlines():
            m = re.match(rf"^\s*(?:export\s+)?{re.escape(name)}=(.*)$", line)
            if m:
                found = re.sub(r"\s+#.*$", "", m.group(1)).strip().strip('"').strip("'")
        if found:
            return found
    return default


def _run(cmd: list[str], title: str) -> int:
    print(f"\n→ {title}\n  {' '.join(cmd)}", flush=True)
    try:
        return subprocess.run(cmd, cwd=paths.ROOT).returncode
    except FileNotFoundError as e:
        print(f"Không chạy được lệnh ({e.filename}) — kiểm đã cài Docker / Git Bash / PowerShell.")
        return 127


def _script(name: str) -> list[str]:
    """Script trong scripts/seed: Windows → bản .ps1, còn lại → bản .sh."""
    if os.name == "nt":
        return ["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                str(paths.SCRIPTS_SEED_DIR / f"{name}.ps1")]
    return ["bash", str(paths.SCRIPTS_SEED_DIR / f"{name}.sh")]


def apply() -> int:
    code = _run(_script("upload-assets"), "Đẩy ảnh lên Silo máy dev + kiểm ảnh")
    if code != 0:
        print(f"Đẩy ảnh lỗi (mã {code}) — dừng, chưa nạp CSDL.")
        return code
    only = re.match(r"R__seed_demo_(\d+)_", paths.SQL_FILE_NAME).group(1)   # R__seed_demo_95_… → 95
    seed = _script("seed") + (["-Docker", "-Only", only] if os.name == "nt" else ["--docker", "--only", only])
    code = _run(seed, "Nạp file seed 95 vào PostgreSQL Docker")
    if code != 0:
        print(f"Nạp CSDL lỗi (mã {code}).")
    return code


def flush_cache() -> int:
    password = env_value("REDIS_PASSWORD", "changeme")
    shell = " ; ".join(
        f"redis-cli -a \"$P\" --no-auth-warning --scan --pattern '{p}' | xargs -r redis-cli -a \"$P\" --no-auth-warning DEL"
        for p in CACHE_PATTERNS)
    cmd = ["docker", "compose", "-f", str(paths.COMPOSE_FILE), "exec", "-T", "-e", f"P={password}", "redis", "sh", "-c",
           shell]
    print(f"\n→ Xóa cache catalog trong Redis Docker ({', '.join(CACHE_PATTERNS)})", flush=True)
    try:
        result = subprocess.run(cmd, cwd=paths.ROOT, capture_output=True, text=True, encoding="utf-8")
    except FileNotFoundError:
        print("Không tìm thấy docker.")
        return 127
    if result.returncode != 0:
        print((result.stderr or result.stdout).strip() or f"redis lỗi (mã {result.returncode})")
        return result.returncode
    # redis-cli không có TTY in mỗi lệnh DEL một số nguyên
    deleted = sum(int(token) for token in result.stdout.split() if token.isdigit())
    print(f"  Đã xóa {deleted} khóa.")
    return 0


def remind_cache() -> None:
    print("\nLưu ý: trang sản phẩm / trang shop có thể còn bản cũ tới 5–10 phút (cache Redis) — "
          "thêm --flush-cache để xóa ngay.")
