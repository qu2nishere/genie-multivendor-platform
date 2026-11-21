# Ảnh sản phẩm cho seed từ Excel (plan 2026-11-21 E03; D1, D2, X6).
#   Nguồn (D2): (1) tên tệp trong folder anh/ cạnh file Excel — cách chính · (2) link https:// — ghi thẳng vào SQL
#               (3) ảnh chèn trong Excel: "Place over Cells" (ảnh nổi, openpyxl đọc ô neo) và "Place in Cell"
#                   (Excel 365 — đọc xl/metadata.xml + xl/richData/*, E03.05)
#   Chuẩn hóa (E03.04): xoay theo EXIF, nền trắng thay nền trong suốt, cạnh dài ≤ 1200px, JPEG q85,
#               tên <sha256[:8]>.jpg theo nội dung SAU chuẩn hóa → assets/ (commit) + assets/manifest.json.
#   manifest.json: khóa nguồn (vd `anh/tai-nghe-1.jpg`) → tên ảnh thu nhỏ. Ảnh gốc vắng (anh/ không lên git) mà manifest
#               đã có bản thu nhỏ → dùng lại, nên người khác sửa Excel không cần ảnh gốc (E03.01).
from __future__ import annotations

import hashlib
import io
import json
import posixpath
import re
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Optional
from xml.etree import ElementTree

from openpyxl.utils import column_index_from_string

from . import paths
from .model import ImageSource, Product, Report, ResolvedImage, Variant
from .reader import PRODUCT_IMAGE_COLUMNS, SHEET_PRODUCTS, SHEET_VARIANTS, Workbook

MAX_SIDE = 1200
JPEG_QUALITY = 85
MAX_SOURCE_BYTES = 25 * 1024 * 1024
SMALL_SIDE = 300
ALLOWED_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp"}
MANIFEST_VERSION = 1


# ---------- ảnh chèn trong Excel ----------

def _local(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _children(el: ElementTree.Element, name: str) -> list[ElementTree.Element]:
    return [c for c in el if _local(c.tag) == name]


def _first(el: ElementTree.Element, name: str) -> Optional[ElementTree.Element]:
    for c in el.iter():
        if _local(c.tag) == name:
            return c
    return None


def _attr(el: ElementTree.Element, name: str) -> Optional[str]:
    for k, v in el.attrib.items():
        if _local(k) == name:
            return v
    return None


def _rels(z: zipfile.ZipFile, rels_path: str, base_dir: str) -> dict[str, str]:
    """Id → đường dẫn tệp trong zip, đã ghép với thư mục của tệp nguồn."""
    root = ElementTree.fromstring(z.read(rels_path))
    out: dict[str, str] = {}
    for rel in root:
        target = rel.attrib.get("Target", "")
        out[rel.attrib.get("Id", "")] = target.lstrip("/") if target.startswith("/") else posixpath.normpath(
            posixpath.join(base_dir, target))
    return out


def read_in_cell_images(path: Path) -> dict[tuple[str, str], bytes]:
    """Ảnh kiểu "Place in Cell": (tên sheet, ô) → nội dung ảnh. Không có rich data → {}.

    Chuỗi tra: ô <c vm="N"> → metadata.xml valueMetadata[N-1] → rc(t = XLRICHVALUE, v) → futureMetadata[v] rvb i
    → richData/rdrichvalue.xml rv[i] (+ cấu trúc ở rdrichvaluestructure.xml, khóa _rvRel:LocalImageIdentifier)
    → richData/richValueRel.xml rel[k] r:id → _rels/richValueRel.xml.rels → ../media/imageX.png
    """
    with zipfile.ZipFile(path) as z:
        names = set(z.namelist())
        if "xl/metadata.xml" not in names or "xl/richData/rdrichvalue.xml" not in names:
            return {}
        meta = ElementTree.fromstring(z.read("xl/metadata.xml"))
        types = [_attr(t, "name") for t in _children(_first(meta, "metadataTypes"), "metadataType")]
        rich_type = types.index("XLRICHVALUE") + 1
        future = next(f for f in _children(meta, "futureMetadata") if _attr(f, "name") == "XLRICHVALUE")
        future_rv = [int(_attr(_first(bk, "rvb"), "i") or 0) for bk in _children(future, "bk")]
        value_meta: list[Optional[int]] = []
        for bk in _children(_first(meta, "valueMetadata"), "bk"):
            rc = _first(bk, "rc")
            ok = rc is not None and int(_attr(rc, "t") or 0) == rich_type
            value_meta.append(future_rv[int(_attr(rc, "v") or 0)] if ok else None)

        structures: list[list[str]] = []
        if "xl/richData/rdrichvaluestructure.xml" in names:
            sroot = ElementTree.fromstring(z.read("xl/richData/rdrichvaluestructure.xml"))
            structures = [[_attr(k, "n") or "" for k in _children(s, "k")] for s in _children(sroot, "s")]
        rich_values: list[Optional[int]] = []
        for rv in _children(ElementTree.fromstring(z.read("xl/richData/rdrichvalue.xml")), "rv"):
            values = [v.text or "" for v in _children(rv, "v")]
            keys = structures[int(_attr(rv, "s") or 0)] if structures else []
            idx = keys.index("_rvRel:LocalImageIdentifier") if "_rvRel:LocalImageIdentifier" in keys else 0
            rich_values.append(int(values[idx]) if values and values[idx].isdigit() else None)

        rel_root = ElementTree.fromstring(z.read("xl/richData/richValueRel.xml"))
        rel_ids = [_attr(r, "id") for r in _children(rel_root, "rel")]
        rel_targets = _rels(z, "xl/richData/_rels/richValueRel.xml.rels", "xl/richData")

        wb = ElementTree.fromstring(z.read("xl/workbook.xml"))
        wb_rels = _rels(z, "xl/_rels/workbook.xml.rels", "xl")
        result: dict[tuple[str, str], bytes] = {}
        for sheet in _children(_first(wb, "sheets"), "sheet"):
            target = wb_rels.get(_attr(sheet, "id") or "")
            if not target or target not in names:
                continue
            for c in ElementTree.fromstring(z.read(target)).iter():
                if _local(c.tag) != "c" or "vm" not in c.attrib:
                    continue
                vm = int(c.attrib["vm"]) - 1
                rv_index = value_meta[vm] if 0 <= vm < len(value_meta) else None
                rel_index = rich_values[rv_index] if rv_index is not None and rv_index < len(rich_values) else None
                if rel_index is None or rel_index >= len(rel_ids):
                    continue
                media = rel_targets.get(rel_ids[rel_index] or "")
                if media and media in names:
                    result[(sheet.attrib.get("name", ""), c.attrib.get("r", ""))] = z.read(media)
        return result


def _has_rich_data(path: Path) -> bool:
    try:
        with zipfile.ZipFile(path) as z:
            return any(n.startswith("xl/richData/") for n in z.namelist())
    except zipfile.BadZipFile:
        return False


def collect_embedded(book: Workbook, products_by_row: dict[int, Product], variants_by_row: dict[int, Variant],
                     report: Report) -> None:
    """Gắn ảnh chèn trong Excel vào sản phẩm / biến thể theo ô neo (E03.03, E03.05)."""
    found: list[tuple[str, int, int, tuple, bytes]] = []   # (sheet, hàng, cột, vị trí trong ô, ảnh)
    for sheet_name in (SHEET_PRODUCTS, SHEET_VARIANTS):
        ws = book.sheets.get(sheet_name)
        if ws is None:
            continue
        for img in getattr(ws, "_images", []):
            marker = getattr(img.anchor, "_from", None)
            if marker is None:
                report.error(sheet_name, "có ảnh không neo vào ô nào — chèn lại bằng Insert → Pictures → Place over Cells")
                continue
            found.append((sheet_name, marker.row + 1, marker.col + 1, (marker.colOff, marker.rowOff), img._data()))

    if _has_rich_data(book.path):
        try:
            for (sheet_name, coord), data in read_in_cell_images(book.path).items():
                m = re.match(r"^([A-Z]+)(\d+)$", coord)
                if m and sheet_name in (SHEET_PRODUCTS, SHEET_VARIANTS):
                    found.append((sheet_name, int(m.group(2)), column_index_from_string(m.group(1)), (0, 0), data))
        except Exception as e:  # cấu trúc rich data lạ
            report.error(book.path.name, "không đọc được ảnh kiểu 'Place in Cell' "
                         f"({e.__class__.__name__}) — chèn lại bằng Insert → Pictures → Place over Cells hoặc ghi tên tệp")

    product_cols = {book.columns.get(SHEET_PRODUCTS, {}).get(k): k for k in PRODUCT_IMAGE_COLUMNS}
    product_cols.pop(None, None)
    variant_col = book.columns.get(SHEET_VARIANTS, {}).get("anh")
    product_data_rows = {r.row for r in book.product_rows}
    variant_data_rows = {r.row for r in book.variant_rows}
    for sheet_name, row, col, offset, data in found:
        ws = book.sheets[sheet_name]
        where = f"{sheet_name}!{ws.cell(row=row, column=col).coordinate}"
        # dòng có dữ liệu nhưng lỗi (không thành sản phẩm / biến thể) → bỏ qua, lỗi của dòng đã được báo
        if sheet_name == SHEET_PRODUCTS:
            if col not in product_cols or row not in product_data_rows:
                report.error(where, "ảnh chèn không nằm trong cột ảnh (anh, anh_2 … anh_8) của dòng sản phẩm nào")
                continue
            product = products_by_row.get(row)
            if product is not None:
                product.images.append(ImageSource("embedded", where, data=data, order=(col, 1, *offset)))
        else:
            if col != variant_col or row not in variant_data_rows:
                report.error(where, "ảnh chèn không nằm trong cột 'anh' của dòng biến thể nào")
                continue
            variant = variants_by_row.get(row)
            if variant is None:
                continue
            if variant.image is not None:
                report.error(where, "mỗi biến thể chỉ một ảnh")
                continue
            variant.image = ImageSource("embedded", where, data=data, order=(col, 1, *offset))


# ---------- chuẩn hóa + kho assets/ ----------

def normalize(raw: bytes) -> tuple[bytes, int, int]:
    """Ảnh bất kỳ → JPEG chuẩn hóa (X6). Lỗi đọc ảnh → ValueError."""
    from PIL import Image, ImageOps, UnidentifiedImageError

    try:
        img = Image.open(io.BytesIO(raw))
        img.load()
    except (UnidentifiedImageError, OSError) as e:
        raise ValueError(f"không đọc được ảnh ({e.__class__.__name__})") from e
    img = ImageOps.exif_transpose(img)
    if img.mode in ("RGBA", "LA") or (img.mode == "P" and "transparency" in img.info):
        rgba = img.convert("RGBA")
        img = Image.new("RGB", rgba.size, (255, 255, 255))
        img.paste(rgba, mask=rgba.getchannel("A"))
    else:
        img = img.convert("RGB")
    img.thumbnail((MAX_SIDE, MAX_SIDE), Image.Resampling.LANCZOS)
    out = io.BytesIO()
    img.save(out, "JPEG", quality=JPEG_QUALITY, optimize=True)
    return out.getvalue(), img.width, img.height


@dataclass
class _Entry:
    source_sha256: str
    asset: str
    width: int
    height: int
    bytes: int

    def resolved(self) -> ResolvedImage:
        return ResolvedImage(image_url=paths.OBJECT_KEY_PREFIX + self.asset, width=self.width, height=self.height,
                             file_size=self.bytes, mime_type="image/jpeg")


class AssetStore:
    """assets/ + manifest.json. write=False (lệnh check): chỉ kiểm, không ghi tệp nào."""

    def __init__(self, assets_dir: Path = paths.ASSETS_DIR, write: bool = True):
        self.dir = assets_dir
        self.manifest_path = assets_dir / "manifest.json"
        self.write = write
        self.entries: dict[str, _Entry] = {}
        self.used: set[str] = set()
        if self.manifest_path.exists():
            data = json.loads(self.manifest_path.read_text(encoding="utf-8"))
            for key, e in data.get("images", {}).items():
                self.entries[key] = _Entry(**e)

    def reuse(self, key: str) -> Optional[ResolvedImage]:
        entry = self.entries.get(key)
        if entry is None or not (self.dir / entry.asset).exists():
            return None
        self.used.add(key)
        return entry.resolved()

    def put(self, key: str, raw: bytes) -> ResolvedImage:
        sha = hashlib.sha256(raw).hexdigest()
        entry = self.entries.get(key)
        if entry is not None and entry.source_sha256 == sha and (self.dir / entry.asset).exists():
            self.used.add(key)
            return entry.resolved()   # ảnh gốc không đổi → giữ bản thu nhỏ cũ, không mã hóa lại (X3)
        data, width, height = normalize(raw)
        asset = hashlib.sha256(data).hexdigest()[:8] + ".jpg"
        target = self.dir / asset
        if self.write:
            self.dir.mkdir(parents=True, exist_ok=True)
            if target.exists() and target.read_bytes() != data:
                raise ValueError(f"trùng tên băm {asset} với ảnh khác — đổi nhẹ ảnh rồi chạy lại")
            if not target.exists():
                target.write_bytes(data)
        self.entries[key] = _Entry(sha, asset, width, height, len(data))
        self.used.add(key)
        return self.entries[key].resolved()

    def save(self) -> list[str]:
        """Ghi manifest (khóa sắp xếp — tất định), xóa mục + ảnh mồ côi. Trả danh sách ảnh đã xóa."""
        if not self.write:
            return []
        kept = {k: self.entries[k] for k in sorted(self.used)}
        self.dir.mkdir(parents=True, exist_ok=True)
        body = {"version": MANIFEST_VERSION,
                "note": "Sinh tự động bởi scripts/seed/excel/excel_to_seed.py — không sửa tay",
                "images": {k: vars(e) for k, e in kept.items()}}
        self.manifest_path.write_text(json.dumps(body, ensure_ascii=False, indent=2) + "\n", encoding="utf-8",
                                      newline="\n")
        live = {e.asset for e in kept.values()}
        removed = []
        for f in sorted(self.dir.glob("*.jpg")):
            if f.name not in live:
                f.unlink()
                removed.append(f.name)
        return removed


class _SourceIndex:
    """Tra tệp trong anh/ không phân biệt hoa thường (Windows / macOS / Linux như nhau)."""

    def __init__(self, image_dir: Path):
        self.dir = image_dir
        self.files: dict[str, Path] = {}
        if image_dir.is_dir():
            for f in image_dir.rglob("*"):
                if f.is_file():
                    self.files[f.relative_to(image_dir).as_posix().lower()] = f

    def find(self, name: str) -> Optional[Path]:
        return self.files.get(name.lower())


def _resolve_one(src: ImageSource, store: AssetStore, index: _SourceIndex, report: Report) -> Optional[ResolvedImage]:
    if src.kind == "url":
        report.warn(src.where, f"ảnh là link ngoài — phụ thuộc web đó còn sống: {src.name}")
        return ResolvedImage(image_url=src.name)
    try:
        if src.kind == "embedded":
            raw = src.data or b""
            key = "embedded:" + hashlib.sha256(raw).hexdigest()[:16]
        else:
            if Path(src.name).suffix.lower() not in ALLOWED_SUFFIXES:
                report.error(src.where, f"'{src.name}': chỉ nhận ảnh {', '.join(sorted(ALLOWED_SUFFIXES))}")
                return None
            key = "anh/" + src.name.lower()
            path = index.find(src.name)
            if path is None:
                reused = store.reuse(key)
                if reused is None:
                    report.error(src.where, f"không thấy tệp '{src.name}' trong {paths.rel(index.dir)}/ "
                                 "(và chưa có bản thu nhỏ trong assets/manifest.json)")
                return reused
            if path.stat().st_size > MAX_SOURCE_BYTES:
                report.error(src.where, f"'{src.name}' lớn quá {MAX_SOURCE_BYTES // 1024 // 1024} MB")
                return None
            raw = path.read_bytes()
        image = store.put(key, raw)
    except ValueError as e:
        report.error(src.where, f"{src.name or 'ảnh chèn'}: {e}")
        return None
    if image.width and image.height and max(image.width, image.height) < SMALL_SIDE:
        report.warn(src.where, f"ảnh nhỏ ({image.width}×{image.height}px) — hiển thị sẽ mờ")
    return image


def resolve(products: list[Product], image_dir: Path, store: AssetStore, report: Report) -> None:
    """Xử lý mọi ảnh của sản phẩm + biến thể; lỗi gom vào report."""
    index = _SourceIndex(image_dir)
    for product in products:
        product.resolved_images = [r for r in (_resolve_one(s, store, index, report) for s in product.images) if r]
        for variant in product.variants:
            if variant.image is not None:
                variant.resolved_image = _resolve_one(variant.image, store, index, report)
