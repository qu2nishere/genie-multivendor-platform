# Kiểu dữ liệu trung gian giữa đọc Excel → xử lý ảnh → sinh SQL (plan 2026-11-21 E02.01).
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Optional

# Giá trị CHECK của V2__catalog.sql
ITEM_CONDITIONS = ("NEW", "USED", "REFURBISHED")
# Trạng thái công cụ cho phép đặt (con của ck_products_status)
PRODUCT_STATUSES = ("PUBLISHED", "DRAFT", "HIDDEN")
MAX_IMAGES_PER_PRODUCT = 8


@dataclass
class Issue:
    """Một lỗi / cảnh báo gắn với ô Excel, vd `SanPham!D7`."""

    where: str
    message: str

    def __str__(self) -> str:
        return f"{self.where}: {self.message}"


@dataclass
class Report:
    errors: list[Issue] = field(default_factory=list)
    warnings: list[Issue] = field(default_factory=list)

    def error(self, where: str, message: str) -> None:
        self.errors.append(Issue(where, message))

    def warn(self, where: str, message: str) -> None:
        self.warnings.append(Issue(where, message))

    @property
    def ok(self) -> bool:
        return not self.errors


@dataclass
class ImageSource:
    """Một ảnh như người nhập khai — chưa xử lý.

    kind: `file` (tên tệp trong anh/), `url` (link https), `embedded` (ảnh chèn trong Excel).
    """

    kind: str
    where: str                       # ô Excel khai ảnh
    name: str = ""                   # file: đường dẫn tương đối trong anh/; url: link
    data: Optional[bytes] = None     # embedded: nội dung ảnh


@dataclass
class ResolvedImage:
    """Ảnh đã xử lý — giá trị ghi vào product_images / product_variants.image_url."""

    image_url: str                   # khóa đối tượng `seed/excel/<băm8>.jpg` hoặc link https
    width: Optional[int] = None
    height: Optional[int] = None
    file_size: Optional[int] = None
    mime_type: Optional[str] = None


@dataclass
class Variant:
    code: str                        # ma_bien_the (đã viết hoa)
    name: str
    attributes: dict[str, str]
    price: int
    stock: int
    image: Optional[ImageSource] = None
    resolved_image: Optional[ResolvedImage] = None
    where: str = ""

    def sku(self, product_code: str) -> str:
        return f"XL-{product_code}-{self.code}"


@dataclass
class Product:
    code: str                        # ma_sp (đã viết hoa)
    row: int
    owner_email: str
    category_slug: str
    name: str
    description: Optional[str]
    brand: Optional[str]
    item_condition: str
    origin: Optional[str]
    price: int
    stock: Optional[int]
    weight_gram: int
    length_cm: Optional[float]
    width_cm: Optional[float]
    height_cm: Optional[float]
    free_ship: bool
    pre_order_days: Optional[int]
    status: str
    sold: int
    rating: Optional[float]
    images: list[ImageSource] = field(default_factory=list)
    resolved_images: list[ResolvedImage] = field(default_factory=list)
    variants: list[Variant] = field(default_factory=list)
    specs: dict[str, str] = field(default_factory=dict)
