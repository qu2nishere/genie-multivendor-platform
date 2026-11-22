# Tiện ích chung cho test công cụ seed Excel (plan 2026-11-21 E06.01) — dựng file Excel nhỏ ngay trong test.
from __future__ import annotations

import io
import sys
from pathlib import Path

import pytest
from openpyxl import Workbook
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from xlseed.refdata import RefData  # noqa: E402

PRODUCT_HEADER = ["ma_sp", "email_chu_shop", "slug_danh_muc", "ten", "gia", "ton_kho", "can_nang_g", "anh"]


def png_bytes(color: tuple[int, int, int] = (200, 30, 30), size: tuple[int, int] = (40, 30), mode: str = "RGB") -> bytes:
    buf = io.BytesIO()
    Image.new(mode, size, color if mode == "RGB" else (*color, 128)).save(buf, "PNG")
    return buf.getvalue()


def make_book(path: Path, products: list[list], *, product_header: list[str] | None = None,
              variants: list[list] | None = None, variant_header: list[str] | None = None,
              specs: list[list] | None = None) -> Workbook:
    wb = Workbook()
    ws = wb.active
    ws.title = "SanPham"
    ws.append(product_header or PRODUCT_HEADER)
    for row in products:
        ws.append(row)
    if variants is not None:
        v = wb.create_sheet("BienThe")
        v.append(variant_header or ["ma_sp", "ma_bien_the", "ten_bien_the", "thuoc_tinh", "gia", "ton_kho", "anh"])
        for row in variants:
            v.append(row)
    if specs is not None:
        s = wb.create_sheet("ThongSo")
        s.append(["ma_sp", "khoa", "gia_tri"])
        for row in specs:
            s.append(row)
    wb.save(path)
    return wb


@pytest.fixture
def ref() -> RefData:
    return RefData(categories={"thiet-bi-am-thanh": "Thiết bị âm thanh", "thoi-trang-nam": "Thời trang Nam"},
                   shops={"mai.tran@gmail.com": "TechZone Official", "owner.shoe@genie.example": "Shoe Republic"},
                   brands={"sony": "Sony"}, attributes={"warranty": "Bảo hành", "material": "Chất liệu"})


def product_row(code: str = "TN01", **over) -> list:
    base = {"ma_sp": code, "email_chu_shop": "mai.tran@gmail.com", "slug_danh_muc": "thiet-bi-am-thanh",
            "ten": f"Tai nghe {code}", "gia": 389000, "ton_kho": 10, "can_nang_g": 350, "anh": "a.jpg"}
    base.update(over)
    return [base[k] for k in PRODUCT_HEADER]
