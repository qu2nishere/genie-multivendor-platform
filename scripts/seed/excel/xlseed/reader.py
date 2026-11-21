# Đọc + kiểm file Excel sản phẩm (plan 2026-11-21 E02.01–E02.03; mục 3 cấu trúc Excel).
#   Sheet SanPham (bắt buộc) · BienThe (tùy chọn) · ThongSo (tùy chọn)
#   Đọc cột theo TÊN TIÊU ĐỀ ở dòng 1 (không theo vị trí); bỏ dòng trống; chuẩn hóa khoảng trắng + Unicode NFC.
#   Mọi lỗi gom vào Report kèm `Sheet!Ô` (X2) — không dừng ở lỗi đầu tiên.
# Ảnh chèn trong Excel (cách phụ) được gom ở images.collect_embedded() rồi gắn vào đây qua `embedded`.
from __future__ import annotations

import re
import unicodedata
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Callable, Optional

from openpyxl import load_workbook
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.worksheet import Worksheet

from .model import (ITEM_CONDITIONS, MAX_IMAGES_PER_PRODUCT, PRODUCT_STATUSES, ImageSource, Product, Report,
                    Variant)
from .refdata import RefData

SHEET_PRODUCTS = "SanPham"
SHEET_VARIANTS = "BienThe"
SHEET_SPECS = "ThongSo"

PRODUCT_IMAGE_COLUMNS = ["anh"] + [f"anh_{i}" for i in range(2, MAX_IMAGES_PER_PRODUCT + 1)]
PRODUCT_COLUMNS = ["ma_sp", "email_chu_shop", "slug_danh_muc", "ten", "mo_ta", "thuong_hieu", "tinh_trang", "xuat_xu",
                   "gia", "ton_kho", "can_nang_g", "dai_cm", "rong_cm", "cao_cm", "mien_phi_ship", "dat_truoc_ngay",
                   "trang_thai", "da_ban", "diem_sao", *PRODUCT_IMAGE_COLUMNS]
PRODUCT_REQUIRED = ["ma_sp", "email_chu_shop", "slug_danh_muc", "ten", "gia", "can_nang_g", "anh"]
VARIANT_COLUMNS = ["ma_sp", "ma_bien_the", "ten_bien_the", "thuoc_tinh", "gia", "ton_kho", "anh"]
VARIANT_REQUIRED = ["ma_sp", "ma_bien_the", "ton_kho"]
SPEC_COLUMNS = ["ma_sp", "khoa", "gia_tri"]

# Độ dài tối đa theo cột DB (V2__catalog.sql)
MAX_NAME = 255
MAX_BRAND = 255
MAX_ORIGIN = 100
MAX_URL = 500
MAX_CODE = 20                      # ma_sp / ma_bien_the — SKU 'XL-<ma_sp>-<ma_bien_the>' ≤ 100

_CODE = re.compile(r"^[A-Za-z0-9]+$")
_KEY = re.compile(r"^[a-z0-9_]+$")
_TRUE = {"x", "có", "co", "yes", "y", "true", "1"}
_FALSE = {"", "không", "khong", "no", "n", "false", "0"}


def normalize_text(value: Any) -> Optional[str]:
    """Chuỗi đã NFC, gộp khoảng trắng thừa ở hai đầu; rỗng → None. Giữ xuống dòng bên trong (mô tả)."""
    if value is None:
        return None
    text = unicodedata.normalize("NFC", str(value)).replace("\r\n", "\n").replace("\r", "\n")
    text = "\n".join(re.sub(r"[ \t ]+", " ", line).strip() for line in text.split("\n")).strip()
    return text or None


def header_key(value: Any) -> Optional[str]:
    """Tiêu đề cột → khóa: chữ thường, bỏ dấu `*` và phần chú thích sau dấu cách / ngoặc (vd `gia * (VND)` → `gia`)."""
    text = normalize_text(value)
    if not text:
        return None
    return re.split(r"[\s(]", text, maxsplit=1)[0].strip("*").lower() or None


@dataclass
class SheetRow:
    sheet: str
    row: int
    cells: dict[str, Any]
    columns: dict[str, int]

    def where(self, key: Optional[str] = None) -> str:
        if key is None or key not in self.columns:
            return f"{self.sheet}!dòng {self.row}"
        return f"{self.sheet}!{get_column_letter(self.columns[key])}{self.row}"

    def text(self, key: str) -> Optional[str]:
        return normalize_text(self.cells.get(key))


@dataclass
class Workbook:
    """Kết quả đọc thô — dùng chung cho kiểm tra và cho images.collect_embedded()."""

    path: Path
    product_rows: list[SheetRow] = field(default_factory=list)
    variant_rows: list[SheetRow] = field(default_factory=list)
    spec_rows: list[SheetRow] = field(default_factory=list)
    columns: dict[str, dict[str, int]] = field(default_factory=dict)   # sheet → khóa → số cột
    sheets: dict[str, Worksheet] = field(default_factory=dict)


def _read_sheet(ws: Worksheet, known: list[str], report: Report) -> tuple[dict[str, int], list[SheetRow]]:
    columns: dict[str, int] = {}
    for cell in ws[1]:
        key = header_key(cell.value)
        if key is None:
            continue
        if key in columns:
            report.error(f"{ws.title}!{cell.coordinate}", f"cột '{key}' bị lặp")
            continue
        if key not in known:
            report.warn(f"{ws.title}!{cell.coordinate}", f"cột lạ '{key}' — bỏ qua")
            continue
        columns[key] = cell.column
    rows: list[SheetRow] = []
    for r in range(2, ws.max_row + 1):
        cells = {key: ws.cell(row=r, column=c).value for key, c in columns.items()}
        if all(normalize_text(v) is None for v in cells.values()):
            continue
        rows.append(SheetRow(ws.title, r, cells, columns))
    return columns, rows


def open_workbook(path: Path, report: Report) -> Optional[Workbook]:
    try:
        wb = load_workbook(path, data_only=True)
    except FileNotFoundError:
        report.error(str(path), "không tìm thấy file")
        return None
    except Exception as e:  # file hỏng / không phải xlsx
        report.error(str(path), f"không mở được file Excel ({e.__class__.__name__}: {e})")
        return None
    book = Workbook(path=path)
    specs = [(SHEET_PRODUCTS, PRODUCT_COLUMNS, book.product_rows), (SHEET_VARIANTS, VARIANT_COLUMNS, book.variant_rows),
             (SHEET_SPECS, SPEC_COLUMNS, book.spec_rows)]
    for name, known, target in specs:
        ws = wb[name] if name in wb.sheetnames else None
        if ws is None:
            if name == SHEET_PRODUCTS:
                report.error(str(path), f"thiếu sheet '{SHEET_PRODUCTS}'")
            continue
        book.sheets[name] = ws
        columns, rows = _read_sheet(ws, known, report)
        book.columns[name] = columns
        target.extend(rows)
    required = {SHEET_PRODUCTS: PRODUCT_REQUIRED, SHEET_VARIANTS: VARIANT_REQUIRED, SHEET_SPECS: SPEC_COLUMNS}
    for name, cols in book.columns.items():
        for key in required[name]:
            if key not in cols:
                report.error(f"{name}!1", f"thiếu cột bắt buộc '{key}'")
    return book


# ---------- chuyển kiểu ----------

def _int(row: SheetRow, key: str, report: Report, *, required: bool, minimum: int = 0,
         label: str = "số nguyên") -> Optional[int]:
    value = row.cells.get(key)
    if isinstance(value, bool):
        report.error(row.where(key), f"phải là {label}")
        return None
    if value is None or normalize_text(value) is None:
        if required:
            report.error(row.where(key), "bắt buộc")
        return None
    if isinstance(value, (int, float)):
        if isinstance(value, float) and not value.is_integer():
            report.error(row.where(key), f"phải là {label} (đang là {value})")
            return None
        number = int(value)
    else:
        # chấp nhận "389.000" / "389,000" / "389 000" — cột này không có phần thập phân
        cleaned = re.sub(r"[.,\s ]", "", str(value)).removesuffix("đ").removesuffix("₫")
        if not cleaned.isdigit():
            report.error(row.where(key), f"phải là {label} (đang là '{value}')")
            return None
        number = int(cleaned)
    if number < minimum:
        report.error(row.where(key), f"phải ≥ {minimum:,}".replace(",", "."))
        return None
    return number


def _float(row: SheetRow, key: str, report: Report, *, minimum: float = 0, maximum: Optional[float] = None
           ) -> Optional[float]:
    value = row.cells.get(key)
    if value is None or normalize_text(value) is None:
        return None
    if isinstance(value, bool):
        report.error(row.where(key), "phải là số")
        return None
    try:
        number = float(value) if isinstance(value, (int, float)) else float(str(value).strip().replace(",", "."))
    except ValueError:
        report.error(row.where(key), f"phải là số (đang là '{value}')")
        return None
    if number < minimum or (maximum is not None and number > maximum):
        bound = f"từ {minimum:g} đến {maximum:g}" if maximum is not None else f"≥ {minimum:g}"
        report.error(row.where(key), f"phải {bound}")
        return None
    return number


def _bool(row: SheetRow, key: str, report: Report) -> bool:
    value = row.cells.get(key)
    if isinstance(value, bool):
        return value
    text = (normalize_text(value) or "").lower()
    if text in _TRUE:
        return True
    if text not in _FALSE:
        report.error(row.where(key), f"ghi 'x' (có) hoặc để trống (không) — đang là '{value}'")
    return False


def _text(row: SheetRow, key: str, report: Report, *, required: bool = False, max_len: Optional[int] = None
          ) -> Optional[str]:
    text = row.text(key)
    if text is None:
        if required:
            report.error(row.where(key), "bắt buộc")
        return None
    if "${" in text:
        # Flyway thay placeholder ${…} trong file seed (plan seed N3) → cấm trong dữ liệu
        report.error(row.where(key), "không được chứa chuỗi '${'")
        return None
    if max_len is not None and len(text) > max_len:
        report.error(row.where(key), f"dài {len(text)} ký tự, tối đa {max_len}")
        return None
    return text


def _choice(row: SheetRow, key: str, report: Report, allowed: tuple[str, ...], default: str) -> str:
    text = row.text(key)
    if text is None:
        return default
    value = text.upper()
    if value not in allowed:
        report.error(row.where(key), f"chỉ nhận {' / '.join(allowed)} (đang là '{text}')")
        return default
    return value


def _code(row: SheetRow, key: str, report: Report) -> Optional[str]:
    text = row.text(key)
    if text is None:
        report.error(row.where(key), "bắt buộc")
        return None
    if not _CODE.match(text) or len(text) > MAX_CODE:
        report.error(row.where(key), f"chỉ gồm chữ không dấu và số, tối đa {MAX_CODE} ký tự (đang là '{text}')")
        return None
    return text.upper()


def parse_image_tokens(row: SheetRow, key: str, report: Report) -> list[ImageSource]:
    """Ô ảnh dạng chữ: tên tệp trong anh/ hoặc link https, cách nhau `;` hoặc xuống dòng (D2)."""
    text = row.text(key)
    if text is None:
        return []
    where = row.where(key)
    images: list[ImageSource] = []
    for token in (t.strip() for t in re.split(r"[;\n]", text)):
        if not token:
            continue
        low = token.lower()
        if low.startswith("https://"):
            if len(token) > MAX_URL:
                report.error(where, f"link ảnh dài quá {MAX_URL} ký tự")
                continue
            images.append(ImageSource("url", where, name=token))
        elif low.startswith("http://"):
            report.error(where, f"link ảnh phải là https:// (đang là '{token}')")
        else:
            name = token.replace("\\", "/")
            if name.startswith("/") or re.match(r"^[A-Za-z]:", name) or ".." in name.split("/"):
                report.error(where, f"chỉ ghi tên tệp trong folder anh/, không ghi đường dẫn tuyệt đối hay '..' ('{token}')")
                continue
            images.append(ImageSource("file", where, name=name))
    return images


def _attributes(row: SheetRow, report: Report) -> dict[str, str]:
    text = row.text("thuoc_tinh")
    result: dict[str, str] = {}
    if text is None:
        return result
    for part in (p.strip() for p in re.split(r"[;\n]", text)):
        if not part:
            continue
        if "=" not in part:
            report.error(row.where("thuoc_tinh"), f"viết dạng 'khoa=gia tri; khoa=gia tri' (lỗi ở '{part}')")
            continue
        k, v = (s.strip() for s in part.split("=", 1))
        k = k.lower()
        if not _KEY.match(k) or not v:
            report.error(row.where("thuoc_tinh"), f"khóa chỉ gồm a-z, 0-9, _ và phải có giá trị (lỗi ở '{part}')")
            continue
        if k in result:
            report.error(row.where("thuoc_tinh"), f"khóa '{k}' bị lặp")
            continue
        result[k] = v
    return result


# ---------- đọc từng sheet ----------

def _product(row: SheetRow, report: Report, ref: RefData) -> Optional[Product]:
    before = len(report.errors)
    code = _code(row, "ma_sp", report)
    email = _text(row, "email_chu_shop", report, required=True)
    if email:
        email = email.lower()
        if ref.shops and email not in ref.shops:
            report.error(row.where("email_chu_shop"),
                         f"không có shop của '{email}' trong seed — chọn một trong: {', '.join(sorted(ref.shops))}")
    slug = _text(row, "slug_danh_muc", report, required=True)
    if slug:
        slug = slug.lower()
        if ref.categories and slug not in ref.categories:
            report.error(row.where("slug_danh_muc"), f"không có danh mục '{slug}' — xem sheet _DanhMuc của file mẫu")
    name = _text(row, "ten", report, required=True, max_len=MAX_NAME)
    description = _text(row, "mo_ta", report)
    brand = _text(row, "thuong_hieu", report, max_len=MAX_BRAND)
    if brand and ref.brands:
        canonical = ref.brand_name(brand)
        if canonical:
            brand = canonical
        elif brand.lower() != "no brand":
            report.warn(row.where("thuong_hieu"), f"thương hiệu '{brand}' chưa có trong bảng brands — chỉ lưu tên, không gắn brand_id")
    condition = _choice(row, "tinh_trang", report, ITEM_CONDITIONS, "NEW")
    origin = _text(row, "xuat_xu", report, max_len=MAX_ORIGIN)
    price = _int(row, "gia", report, required=True, minimum=1000, label="số nguyên VND")
    stock = _int(row, "ton_kho", report, required=False, minimum=0)
    weight = _int(row, "can_nang_g", report, required=True, minimum=1, label="số nguyên gram")
    length = _float(row, "dai_cm", report)
    width = _float(row, "rong_cm", report)
    height = _float(row, "cao_cm", report)
    free_ship = _bool(row, "mien_phi_ship", report)
    pre_order = _int(row, "dat_truoc_ngay", report, required=False, minimum=1)
    status = _choice(row, "trang_thai", report, PRODUCT_STATUSES, "PUBLISHED")
    sold = _int(row, "da_ban", report, required=False, minimum=0) or 0
    rating = _float(row, "diem_sao", report, minimum=0, maximum=5)
    images: list[ImageSource] = []
    for key in PRODUCT_IMAGE_COLUMNS:
        images.extend(parse_image_tokens(row, key, report))
    if len(report.errors) > before or code is None:
        return None
    return Product(code=code, row=row.row, owner_email=email or "", category_slug=slug or "", name=name or "",
                   description=description, brand=brand, item_condition=condition, origin=origin, price=price or 0,
                   stock=stock, weight_gram=weight or 0, length_cm=length, width_cm=width, height_cm=height,
                   free_ship=free_ship, pre_order_days=pre_order, status=status, sold=sold, rating=rating,
                   images=images)


def _variant(row: SheetRow, report: Report, product: Product) -> Optional[Variant]:
    before = len(report.errors)
    code = _code(row, "ma_bien_the", report)
    attributes = _attributes(row, report)
    name = _text(row, "ten_bien_the", report, max_len=MAX_NAME)
    if name is None:
        name = " / ".join(attributes.values()) or (code or "")
    price = _int(row, "gia", report, required=False, minimum=1000, label="số nguyên VND")
    stock = _int(row, "ton_kho", report, required=True, minimum=0)
    images = parse_image_tokens(row, "anh", report)
    if len(images) > 1:
        report.error(row.where("anh"), "mỗi biến thể chỉ một ảnh")
    if len(report.errors) > before or code is None or stock is None:
        return None
    return Variant(code=code, name=name, attributes=attributes, price=price if price is not None else product.price,
                   stock=stock, image=images[0] if images else None, where=row.where())


def read(path: Path, ref: RefData, report: Report,
         attach_embedded: Optional[Callable[[Workbook, dict[int, Product], dict[int, Variant], Report], None]] = None
         ) -> list[Product]:
    """Đọc + kiểm toàn bộ file. Có lỗi thì vẫn trả danh sách đọc được — người gọi xem report.ok."""
    if ref.missing:
        report.warn("seed", "không đọc được danh sách " + ", ".join(ref.missing)
                    + " — bỏ kiểm offline, SQL vẫn kiểm khi nạp")
    book = open_workbook(path, report)
    if book is None:
        return []

    products: dict[str, Product] = {}
    by_row: dict[int, Product] = {}
    names: dict[tuple[str, str], str] = {}
    for row in book.product_rows:
        product = _product(row, report, ref)
        if product is None:
            continue
        if product.code in products:
            report.error(row.where("ma_sp"), f"mã '{product.code}' trùng với dòng {products[product.code].row}")
            continue
        key = (product.owner_email, product.name.lower())
        if key in names:
            report.error(row.where("ten"), f"shop này đã có sản phẩm cùng tên (mã {names[key]}) — tên là khóa nhận diện sản phẩm")
            continue
        names[key] = product.code
        products[product.code] = product
        by_row[row.row] = product

    variants_by_row: dict[int, Variant] = {}
    for row in book.variant_rows:
        code = row.text("ma_sp")
        product = products.get(code.upper()) if code else None
        if product is None:
            report.error(row.where("ma_sp"), f"mã sản phẩm '{code}' không có trong sheet {SHEET_PRODUCTS}"
                         if code else "bắt buộc")
            continue
        variant = _variant(row, report, product)
        if variant is None:
            continue
        if any(v.code == variant.code for v in product.variants):
            report.error(row.where("ma_bien_the"), f"biến thể '{variant.code}' của {product.code} bị lặp")
            continue
        product.variants.append(variant)
        variants_by_row[row.row] = variant

    for row in book.spec_rows:
        code = row.text("ma_sp")
        product = products.get(code.upper()) if code else None
        if product is None:
            report.error(row.where("ma_sp"), f"mã sản phẩm '{code}' không có trong sheet {SHEET_PRODUCTS}"
                         if code else "bắt buộc")
            continue
        key = (row.text("khoa") or "").lower()
        value = _text(row, "gia_tri", report, required=True)
        if not _KEY.match(key):
            report.error(row.where("khoa"), "khóa chỉ gồm a-z, 0-9, _ (vd warranty, material)")
            continue
        if value is None:
            continue
        if key in product.specs:
            report.error(row.where("khoa"), f"thông số '{key}' của {product.code} bị lặp")
            continue
        if ref.attributes and key not in ref.attributes:
            report.warn(row.where("khoa"), f"'{key}' không phải thuộc tính lọc của sàn ({', '.join(sorted(ref.attributes))}) — "
                        "vẫn lưu nhưng bộ lọc không dùng được")
        product.specs[key] = value

    if attach_embedded is not None:
        attach_embedded(book, by_row, variants_by_row, report)

    if not book.product_rows:
        report.error(f"{SHEET_PRODUCTS}!dòng 2", "chưa có sản phẩm nào")

    for product in products.values():
        where = f"{SHEET_PRODUCTS}!dòng {product.row}"
        if not product.images:
            report.error(where, "cần ít nhất 1 ảnh ở cột 'anh'")
        if len(product.images) > MAX_IMAGES_PER_PRODUCT:
            report.error(where, f"tối đa {MAX_IMAGES_PER_PRODUCT} ảnh / sản phẩm (đang có {len(product.images)})")
        if not product.variants:
            if product.stock is None:
                report.error(f"{SHEET_PRODUCTS}!dòng {product.row}",
                             "không có biến thể ở sheet BienThe thì phải điền 'ton_kho'")
            else:
                product.variants.append(Variant(code="STD", name="Mặc định", attributes={}, price=product.price,
                                                stock=product.stock, where=where))
        elif product.stock is not None:
            report.warn(where, "đã có biến thể ở sheet BienThe — bỏ qua 'ton_kho' của dòng sản phẩm")

    return sorted(products.values(), key=lambda p: p.code)
