# Danh sách tham chiếu đọc từ file seed đã có (plan 2026-11-21 D7, E02.03) — không cần kết nối CSDL.
#   R__seed_demo_10_categories.sql → slug + tên danh mục
#   R__seed_demo_30_shops.sql      → email chủ shop + tên shop (6 shop VERIFIED)
#   R__seed_demo_20_brands_attributes.sql → tên thương hiệu, mã thuộc tính lọc
# Đọc bằng biểu thức chính quy trên các dòng VALUES; đọc không ra (file seed đổi khuôn) → danh sách rỗng,
# bỏ kiểm offline và chỉ còn lớp kiểm khi nạp (khối DO trong SQL — X5).
from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path

from . import paths

_SQL_STR = r"'((?:[^']|'')*)'"
_CATEGORY_ROW = re.compile(r"^\s*\('([a-z0-9-]+)',\s*" + _SQL_STR, re.M)
_SHOP_ROW = re.compile(r"^\s*\('([^'\s@]+@[^'\s]+)',\s*" + _SQL_STR + r",\s*" + _SQL_STR, re.M)
_BRAND_ROW = re.compile(r"\(" + _SQL_STR + r",\s*'([a-z0-9-]+)',\s*(?:TRUE|FALSE)\)")
_ATTRIBUTE_ROW = re.compile(r"^\s*\('[a-z0-9-]+',\s*'([a-z_]+)',\s*" + _SQL_STR + r",\s*'(?:ENUM|TEXT|NUMBER|BOOLEAN|MULTI_ENUM|DATE)'", re.M)


def _unquote(s: str) -> str:
    return s.replace("''", "'")


@dataclass
class RefData:
    categories: dict[str, str] = field(default_factory=dict)    # slug → tên
    shops: dict[str, str] = field(default_factory=dict)         # email chủ shop → tên shop
    brands: dict[str, str] = field(default_factory=dict)        # tên viết thường → tên chuẩn
    attributes: dict[str, str] = field(default_factory=dict)    # mã → tên
    missing: list[str] = field(default_factory=list)            # file / nhóm đọc không ra

    def brand_name(self, raw: str) -> str | None:
        return self.brands.get(raw.strip().lower())


def _read(path: Path) -> str | None:
    try:
        return path.read_text(encoding="utf-8")
    except OSError:
        return None


def load(seed_dir: Path = paths.SEED_DIR) -> RefData:
    ref = RefData()
    cat = _read(seed_dir / "R__seed_demo_10_categories.sql")
    if cat:
        for slug, name in _CATEGORY_ROW.findall(cat):
            ref.categories.setdefault(slug, _unquote(name))
    if not ref.categories:
        ref.missing.append("danh mục (R__seed_demo_10_categories.sql)")

    shops = _read(seed_dir / "R__seed_demo_30_shops.sql")
    if shops:
        for email, _owner, shop in _SHOP_ROW.findall(shops):
            ref.shops.setdefault(email.lower(), _unquote(shop))
    if not ref.shops:
        ref.missing.append("shop (R__seed_demo_30_shops.sql)")

    brands = _read(seed_dir / "R__seed_demo_20_brands_attributes.sql")
    if brands:
        for name, _slug in _BRAND_ROW.findall(brands):
            ref.brands.setdefault(_unquote(name).lower(), _unquote(name))
        for code, name in _ATTRIBUTE_ROW.findall(brands):
            ref.attributes.setdefault(code, _unquote(name))
    if not ref.brands:
        ref.missing.append("thương hiệu (R__seed_demo_20_brands_attributes.sql)")
    return ref
