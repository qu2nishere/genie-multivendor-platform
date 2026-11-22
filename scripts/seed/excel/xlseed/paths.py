# Đường dẫn cố định của công cụ seed từ Excel (plan 2026-11-21 db/plan_seed_tu_excel.md E01.02; D1, D3).
# Gốc repo dò từ vị trí file này (như scripts/seed/seed.sh) — chạy từ thư mục nào cũng ghi đúng chỗ.
from __future__ import annotations

from pathlib import Path

TOOL_DIR = Path(__file__).resolve().parent.parent          # scripts/seed/excel
ROOT = TOOL_DIR.parent.parent.parent                         # gốc repo
SEED_DIR = ROOT / "server-spring" / "src" / "main" / "resources" / "db" / "seed"
SQL_FILE_NAME = "R__seed_demo_95_excel_products.sql"
SQL_FILE = SEED_DIR / SQL_FILE_NAME
ASSETS_DIR = TOOL_DIR / "assets"                             # bản thu nhỏ — BẮT BUỘC commit
MANIFEST_FILE = ASSETS_DIR / "manifest.json"
SCRIPTS_SEED_DIR = ROOT / "scripts" / "seed"
COMPOSE_FILE = ROOT / "deploy" / "docker-compose.local.yml"
ENV_FILE = ROOT / ".env"

# Khóa đối tượng trong bucket product-media (LUU_TRU 5.5) — upload-assets mirror ASSETS_DIR vào đúng tiền tố này
OBJECT_KEY_PREFIX = "seed/excel/"


def image_dir_for(workbook: Path) -> Path:
    """Folder ảnh gốc `anh/` nằm cạnh file Excel (không commit — .gitignore)."""
    return workbook.resolve().parent / "anh"


def rel(path: Path) -> str:
    """Đường dẫn tương đối so với gốc repo để in ra cho dễ đọc."""
    try:
        return path.resolve().relative_to(ROOT).as_posix()
    except ValueError:
        return str(path)
