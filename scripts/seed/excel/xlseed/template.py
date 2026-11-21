# Sinh file Excel mẫu (plan 2026-11-21 D7; mục 3 cấu trúc Excel): 3 sheet nhập liệu + HuongDan + sheet ẩn
# _DanhMuc, _Shop làm nguồn cho Data Validation (danh sách chọn). Không ghi đè file đã có trừ khi --force.
from __future__ import annotations

from pathlib import Path

from openpyxl import Workbook
from openpyxl.comments import Comment
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.datavalidation import DataValidation

from .model import ITEM_CONDITIONS, PRODUCT_STATUSES
from .reader import (PRODUCT_COLUMNS, PRODUCT_REQUIRED, SHEET_PRODUCTS, SHEET_SPECS, SHEET_VARIANTS, SPEC_COLUMNS,
                     VARIANT_COLUMNS, VARIANT_REQUIRED)
from .refdata import RefData

DATA_ROWS = 500   # số dòng được gắn danh sách chọn

HELP = {
    "ma_sp": "Mã sản phẩm do bạn tự đặt, duy nhất trong file (chữ không dấu + số, vd TN01). Dùng để nối sheet BienThe / ThongSo và tạo SKU XL-<ma_sp>-<ma_bien_the>.",
    "email_chu_shop": "Email chủ shop đã có trong seed — chọn trong danh sách.",
    "slug_danh_muc": "Slug danh mục (nên chọn danh mục con) — chọn trong danh sách.",
    "ten": "Tên sản phẩm (≤ 255 ký tự). Tên + shop là khóa nhận diện: ĐỔI TÊN = sản phẩm mới, bản cũ bị ẩn.",
    "mo_ta": "Mô tả, được xuống dòng (Alt+Enter).",
    "thuong_hieu": "Tên thương hiệu. Trùng bảng brands (Sony, Samsung…) thì được gắn brand_id để lọc.",
    "tinh_trang": "NEW (mặc định) / USED / REFURBISHED.",
    "xuat_xu": "Nơi sản xuất, vd Việt Nam.",
    "gia": "Giá bán, số nguyên VND (vd 389000).",
    "ton_kho": "Tồn kho khi KHÔNG khai biến thể ở sheet BienThe.",
    "can_nang_g": "Cân nặng (gram, số nguyên) — GHN tính cước.",
    "dai_cm": "Kích thước đóng gói (cm).",
    "rong_cm": "Kích thước đóng gói (cm).",
    "cao_cm": "Kích thước đóng gói (cm).",
    "mien_phi_ship": "Ghi x nếu shop trả phí ship, để trống nếu người mua trả.",
    "dat_truoc_ngay": "Số ngày chuẩn bị hàng nếu là hàng đặt trước.",
    "trang_thai": "PUBLISHED (mặc định, hiện trên sàn) / DRAFT / HIDDEN.",
    "da_ban": "Số đã bán (để thử sắp xếp).",
    "diem_sao": "Điểm đánh giá 0–5.",
    "anh": "Ảnh — ảnh đầu là ảnh chính. Cách chính: tên tệp trong folder anh/ cạnh file Excel, nhiều ảnh cách nhau dấu ; (vd tai-nghe-1.jpg; tai-nghe-2.jpg). Cũng nhận link https:// hoặc ảnh chèn bằng Insert → Pictures → Place over Cells.",
    "ma_bien_the": "Mã biến thể (chữ không dấu + số, vd BLK, M, 512).",
    "ten_bien_the": "Tên hiển thị, vd 'Đen' hoặc 'Size M'. Bỏ trống thì lấy từ thuoc_tinh.",
    "thuoc_tinh": "Dạng khoa=gia tri; khoa=gia tri — vd color=Đen; size=M.",
    "khoa": "Mã thông số, nên trùng thuộc tính lọc của sàn (warranty, material, skin_type, color, size).",
    "gia_tri": "Giá trị thông số, vd 12 tháng.",
}

GUIDE = [
    "HƯỚNG DẪN NHẬP SẢN PHẨM SEED (chi tiết: scripts/seed/excel/README.md)",
    "",
    "1. Sheet SanPham: mỗi dòng một sản phẩm. Cột tô vàng là bắt buộc. Rê chuột vào tiêu đề để xem giải thích.",
    "2. Sheet BienThe (tùy chọn): mỗi dòng một biến thể (màu, size…). Không khai thì sản phẩm có 1 biến thể 'Mặc định' với tồn = ton_kho.",
    "3. Sheet ThongSo (tùy chọn): thông số kỹ thuật, mỗi dòng một cặp khoa / gia_tri.",
    "4. Ảnh: bỏ ảnh vào folder anh/ đặt cạnh file Excel này, rồi ghi tên tệp vào cột anh (nhiều ảnh cách nhau dấu ;).",
    "   - Cũng nhận link https://… (ảnh phụ thuộc web ngoài) hoặc ảnh chèn bằng Insert → Pictures → Place over Cells.",
    "   - KHÔNG dùng 'Place in Cell'. Nếu chèn ảnh vào Excel: File → Options → Advanced → tick 'Do not compress images in file'.",
    "5. Kiểm:  python scripts/seed/excel/excel_to_seed.py check <file.xlsx>",
    "6. Tạo SQL + ảnh, nạp vào máy dev:  python scripts/seed/excel/excel_to_seed.py build <file.xlsx> --apply",
    "7. Commit: file SQL R__seed_demo_95_excel_products.sql + folder scripts/seed/excel/assets/ + file Excel. KHÔNG commit folder anh/.",
]

_HEADER_FONT = Font(bold=True)
_REQUIRED_FILL = PatternFill("solid", fgColor="FFF2CC")
_OPTIONAL_FILL = PatternFill("solid", fgColor="E7E6E6")


def _sheet(wb: Workbook, title: str, columns: list[str], required: list[str]):
    ws = wb.create_sheet(title)
    for i, key in enumerate(columns, start=1):
        cell = ws.cell(row=1, column=i, value=key)
        cell.font = _HEADER_FONT
        cell.fill = _REQUIRED_FILL if key in required else _OPTIONAL_FILL
        help_text = ("Chỉ dùng khi CHÈN ẢNH vào Excel (mỗi ô một ảnh). Dùng tên tệp thì ghi hết vào cột anh."
                     if key.startswith("anh_") else HELP.get(key))
        if help_text:
            cell.comment = Comment(help_text, "Genie seed", width=320, height=140)
        ws.column_dimensions[get_column_letter(i)].width = 34 if key in ("ten", "mo_ta", "anh", "thuoc_tinh") else 16
    ws.freeze_panes = "A2"
    return ws


def _list_validation(ws, columns: list[str], key: str, formula: str) -> None:
    if key not in columns:
        return
    letter = get_column_letter(columns.index(key) + 1)
    dv = DataValidation(type="list", formula1=formula, allow_blank=True, showErrorMessage=True,
                        errorTitle="Giá trị không hợp lệ", error="Chọn một giá trị trong danh sách.")
    dv.add(f"{letter}2:{letter}{DATA_ROWS + 1}")
    ws.add_data_validation(dv)


def build(path: Path, ref: RefData) -> None:
    wb = Workbook()
    guide = wb.active
    guide.title = "HuongDan"
    for i, line in enumerate(GUIDE, start=1):
        guide.cell(row=i, column=1, value=line).alignment = Alignment(wrap_text=False)
    guide["A1"].font = Font(bold=True, size=13)
    guide.column_dimensions["A"].width = 140

    products = _sheet(wb, SHEET_PRODUCTS, PRODUCT_COLUMNS, PRODUCT_REQUIRED)
    _sheet(wb, SHEET_VARIANTS, VARIANT_COLUMNS, VARIANT_REQUIRED)
    _sheet(wb, SHEET_SPECS, SPEC_COLUMNS, SPEC_COLUMNS)

    cats = wb.create_sheet("_DanhMuc")
    cats.append(["slug", "ten"])
    for slug, name in ref.categories.items():
        cats.append([slug, name])
    shops = wb.create_sheet("_Shop")
    shops.append(["email_chu_shop", "ten_shop"])
    for email, name in sorted(ref.shops.items()):
        shops.append([email, name])
    for ws in (cats, shops):
        ws.sheet_state = "hidden"

    if ref.shops:
        _list_validation(products, PRODUCT_COLUMNS, "email_chu_shop", f"=_Shop!$A$2:$A${len(ref.shops) + 1}")
    if ref.categories:
        _list_validation(products, PRODUCT_COLUMNS, "slug_danh_muc", f"=_DanhMuc!$A$2:$A${len(ref.categories) + 1}")
    _list_validation(products, PRODUCT_COLUMNS, "tinh_trang", '"' + ",".join(ITEM_CONDITIONS) + '"')
    _list_validation(products, PRODUCT_COLUMNS, "trang_thai", '"' + ",".join(PRODUCT_STATUSES) + '"')
    _list_validation(products, PRODUCT_COLUMNS, "mien_phi_ship", '"x"')

    wb.active = 1   # mở vào sheet SanPham
    path.parent.mkdir(parents=True, exist_ok=True)
    wb.save(path)
