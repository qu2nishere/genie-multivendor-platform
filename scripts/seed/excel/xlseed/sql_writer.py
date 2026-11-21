# Sinh file seed SQL từ dữ liệu Excel (plan 2026-11-21 E04; X3–X5, D3, D4, D6) — khuôn theo R__seed_demo_40_products.sql.
#   - SQL thuần, chạy lại được, không ghi cứng id (plan seed N1–N3); chạy được bằng Flyway lẫn psql.
#   - Sản phẩm khớp theo (chủ shop, tên) → cập nhật; chưa có → thêm. SKU 'XL-<ma_sp>-<ma_bien_the>' → ON CONFLICT (sku).
#   - Sản phẩm Excel (có SKU 'XL-%') không còn trong file → HIDDEN; biến thể 'XL-%' không còn → xóa mềm (deleted_at).
#   - Ảnh của sản phẩm Excel: xóa rồi chèn lại (chỉ dòng 'seed/excel/%' hoặc link https).
#   - Khối DO kiểm chủ shop / danh mục TRƯỚC khi ghi → sai thì RAISE EXCEPTION (X5), transaction rollback.
#   - Đầu ra tất định: cùng dữ liệu → cùng nội dung tệp (LF, UTF-8 không BOM, không mốc thời gian).
from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Iterable

from .model import Product

HEADER = """\
-- Dữ liệu mẫu 95 — sản phẩm nhập từ file Excel (plan 2026-11-21 db/plan_seed_tu_excel.md).
-- FILE SINH TỰ ĐỘNG bởi scripts/seed/excel/excel_to_seed.py — KHÔNG SỬA TAY: sửa file Excel rồi chạy
--   python scripts/seed/excel/excel_to_seed.py build <file.xlsx>      (hướng dẫn: scripts/seed/excel/README.md)
-- SQL thuần, chạy lại được, không ghi cứng id: sản phẩm khớp theo (email chủ shop, tên); SKU 'XL-<ma_sp>-<ma_bien_the>'.
-- Ảnh lưu khóa đối tượng 'seed/excel/<băm8>.jpg' trong bucket product-media — ảnh ở scripts/seed/excel/assets/,
-- đẩy lên Silo bằng scripts/seed/upload-assets.sh | .ps1. Ô ảnh là link https thì lưu nguyên link.
"""


def lit(value: Any) -> str:
    """Giá trị Python → hằng SQL."""
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "TRUE" if value else "FALSE"
    if isinstance(value, int):
        return str(value)
    if isinstance(value, float):
        return repr(value)
    text = str(value).replace("\x00", "")
    return "'" + text.replace("'", "''") + "'"


def jsonb(obj: dict | None) -> str:
    if not obj:
        return "NULL"
    return lit(json.dumps(obj, ensure_ascii=False, sort_keys=True)) + "::jsonb"


def _values(rows: Iterable[list[str]]) -> str:
    return ",\n".join("    (" + ", ".join(r) + ")" for r in rows)


def render(products: list[Product], source_name: str) -> str:
    products = sorted(products, key=lambda p: p.code)
    product_rows, variant_rows, image_rows = [], [], []
    for p in products:
        product_rows.append([
            lit(p.code), lit(p.owner_email), lit(p.category_slug), lit(p.name), lit(p.description), lit(p.brand),
            lit(p.item_condition), lit(p.origin), jsonb(p.specs), lit(float(p.weight_gram)), lit(p.length_cm),
            lit(p.width_cm), lit(p.height_cm), lit(p.free_ship), lit(p.pre_order_days), lit(p.status), lit(p.sold),
            lit(p.rating), lit(p.price)])
        order = 0
        for i, img in enumerate(p.resolved_images):
            image_rows.append([lit(p.code), "NULL", lit(img.image_url), lit(img.width), lit(img.height),
                               lit(img.file_size), lit(img.mime_type), lit(i == 0), lit(order)])
            order += 1
        for v in p.variants:
            sku = v.sku(p.code)
            img = v.resolved_image
            variant_rows.append([lit(p.code), lit(sku), lit(v.name), jsonb(v.attributes), lit(v.price), lit(v.stock),
                                 lit(img.image_url if img else None)])
            if img is not None:
                image_rows.append([lit(p.code), lit(sku), lit(img.image_url), lit(img.width), lit(img.height),
                                   lit(img.file_size), lit(img.mime_type), lit(False), lit(order)])
                order += 1

    summary = (f"-- Nguồn: {source_name} · {len(products)} sản phẩm, {len(variant_rows)} biến thể, "
               f"{len(image_rows)} ảnh.\n")
    return HEADER + summary + f"""
DROP TABLE IF EXISTS xl_product, xl_variant, xl_image, xl_product_id;

CREATE TEMP TABLE xl_product (
    code TEXT PRIMARY KEY, owner_email TEXT NOT NULL, category_slug TEXT NOT NULL, name TEXT NOT NULL, description TEXT,
    brand TEXT, item_condition TEXT NOT NULL, origin TEXT, specs JSONB, weight_gram DOUBLE PRECISION NOT NULL,
    length_cm DOUBLE PRECISION, width_cm DOUBLE PRECISION, height_cm DOUBLE PRECISION, free_ship BOOLEAN NOT NULL,
    pre_order_days INT, status TEXT NOT NULL, sold INT NOT NULL, rating DOUBLE PRECISION, base_price NUMERIC(19,2) NOT NULL
);
INSERT INTO xl_product VALUES
{_values(product_rows)};

CREATE TEMP TABLE xl_variant (
    product_code TEXT NOT NULL, sku TEXT PRIMARY KEY, variant_name TEXT, attributes JSONB, price NUMERIC(19,2) NOT NULL,
    stock INT NOT NULL, image_url TEXT
);
INSERT INTO xl_variant VALUES
{_values(variant_rows)};

CREATE TEMP TABLE xl_image (
    product_code TEXT NOT NULL, sku TEXT, image_url TEXT NOT NULL, width INT, height INT, file_size BIGINT,
    mime_type TEXT, is_primary BOOLEAN NOT NULL, display_order INT NOT NULL
);
INSERT INTO xl_image VALUES
{_values(image_rows)};

-- ===== Kiểm tham chiếu trước khi ghi (X5): chủ shop phải có shop VERIFIED, danh mục phải tồn tại =====
DO $$
DECLARE bad TEXT;
BEGIN
    SELECT string_agg(x.code || ' (' || x.owner_email || ')', ', ' ORDER BY x.code) INTO bad
    FROM xl_product x
    WHERE NOT EXISTS (SELECT 1 FROM users u JOIN vendor_profiles vp ON vp.user_id = u.id
                      WHERE lower(u.email) = x.owner_email AND vp.kyc_status = 'VERIFIED');
    IF bad IS NOT NULL THEN
        RAISE EXCEPTION 'Seed Excel: chưa có shop VERIFIED của chủ shop: %', bad;
    END IF;
    SELECT string_agg(x.code || ' (' || x.category_slug || ')', ', ' ORDER BY x.code) INTO bad
    FROM xl_product x WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.slug = x.category_slug);
    IF bad IS NOT NULL THEN
        RAISE EXCEPTION 'Seed Excel: không có danh mục: %', bad;
    END IF;
END $$;

-- ===== 1) Sản phẩm Excel đã bỏ khỏi file (xóa dòng / đổi tên) → ẩn (D4) =====
UPDATE products p
SET status = 'HIDDEN', is_published = FALSE, version = p.version + 1, updated_at = now()
WHERE p.status <> 'HIDDEN'
  AND EXISTS (SELECT 1 FROM product_variants v WHERE v.product_id = p.id AND v.sku LIKE 'XL-%')
  AND NOT EXISTS (SELECT 1 FROM xl_product x JOIN users u ON lower(u.email) = x.owner_email
                  WHERE p.vendor_id = u.id AND p.name = x.name);

-- ===== 2) Cập nhật sản phẩm đã có (khớp chủ shop + tên) =====
UPDATE products p
SET category_id = (SELECT min(c.id) FROM categories c WHERE c.slug = x.category_slug),
    description = x.description, brand = x.brand,
    brand_id = (SELECT min(b.id) FROM brands b WHERE lower(b.name) = lower(x.brand)),
    item_condition = x.item_condition, origin = x.origin, specs_json = x.specs,
    weight = x.weight_gram, length = x.length_cm, width = x.width_cm, height = x.height_cm,
    ships_from = vp.pickup_province, shipping_payer = CASE WHEN x.free_ship THEN 'SELLER' ELSE 'BUYER' END,
    base_price = x.base_price, status = x.status, is_published = (x.status = 'PUBLISHED'),
    sold = x.sold, rating = x.rating, is_pre_order = x.pre_order_days IS NOT NULL, pre_order_days = x.pre_order_days,
    version = p.version + 1, updated_at = now()
FROM xl_product x
JOIN users u ON lower(u.email) = x.owner_email
JOIN vendor_profiles vp ON vp.user_id = u.id
WHERE p.vendor_id = u.id AND p.name = x.name;

-- ===== 3) Thêm sản phẩm mới =====
INSERT INTO products (vendor_id, category_id, name, description, brand, brand_id, item_condition, origin, specs_json,
                      weight, length, width, height, ships_from, shipping_payer, base_price, status, is_published,
                      version, created_at, updated_at, sold, rating, is_pre_order, pre_order_days, view_count)
SELECT u.id, (SELECT min(c.id) FROM categories c WHERE c.slug = x.category_slug), x.name, x.description, x.brand,
       (SELECT min(b.id) FROM brands b WHERE lower(b.name) = lower(x.brand)), x.item_condition, x.origin, x.specs,
       x.weight_gram, x.length_cm, x.width_cm, x.height_cm, vp.pickup_province,
       CASE WHEN x.free_ship THEN 'SELLER' ELSE 'BUYER' END, x.base_price, x.status, x.status = 'PUBLISHED',
       0, now(), now(), x.sold, x.rating, x.pre_order_days IS NOT NULL, x.pre_order_days, 0
FROM xl_product x
JOIN users u ON lower(u.email) = x.owner_email
JOIN vendor_profiles vp ON vp.user_id = u.id
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.vendor_id = u.id AND p.name = x.name)
ORDER BY x.code;

CREATE TEMP TABLE xl_product_id AS
SELECT x.code, (SELECT min(p.id) FROM products p WHERE p.vendor_id = u.id AND p.name = x.name) AS product_id
FROM xl_product x
JOIN users u ON lower(u.email) = x.owner_email;

-- ===== 4) Biến thể: 'XL-%' không còn trong file → xóa mềm; còn lại upsert theo SKU (D6) =====
UPDATE product_variants v
SET deleted_at = now(), version = v.version + 1
WHERE v.sku LIKE 'XL-%' AND v.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM xl_variant x WHERE x.sku = v.sku);

INSERT INTO product_variants (product_id, sku, variant_name, attributes_json, price, stock_count, reserved_count,
                              version, image_url)
SELECT m.product_id, x.sku, x.variant_name, x.attributes, x.price, x.stock, 0, 0, x.image_url
FROM xl_variant x
JOIN xl_product_id m ON m.code = x.product_code
ORDER BY x.sku
ON CONFLICT (sku) DO UPDATE
SET product_id = EXCLUDED.product_id, variant_name = EXCLUDED.variant_name, attributes_json = EXCLUDED.attributes_json,
    price = EXCLUDED.price, stock_count = EXCLUDED.stock_count, image_url = EXCLUDED.image_url, deleted_at = NULL,
    version = product_variants.version + 1;

-- ===== 5) Ảnh: xóa ảnh do công cụ tạo của các sản phẩm này rồi chèn lại theo file =====
DELETE FROM product_images i
USING xl_product_id m
WHERE i.product_id = m.product_id AND (i.image_url LIKE 'seed/excel/%' OR i.image_url LIKE 'https://%');

INSERT INTO product_images (product_id, variant_id, image_url, width, height, file_size, alt_text, is_primary,
                            mime_type, display_order)
SELECT m.product_id, v.id, x.image_url, x.width, x.height, x.file_size,
       left(CASE WHEN v.id IS NULL THEN p.name ELSE p.name || ' — ' || v.variant_name END, 500),
       x.is_primary, x.mime_type, x.display_order
FROM xl_image x
JOIN xl_product_id m ON m.code = x.product_code
JOIN xl_product p ON p.code = x.product_code
LEFT JOIN product_variants v ON v.sku = x.sku
ORDER BY x.product_code, x.display_order;

-- ===== 6) Đồng bộ bộ đếm sản phẩm đang bán của shop (như file 40) =====
UPDATE vendor_profiles vp
SET active_product_count = (SELECT count(*) FROM products p WHERE p.vendor_id = vp.user_id AND p.status = 'PUBLISHED')
WHERE vp.kyc_status = 'VERIFIED';

DROP TABLE xl_product_id;
DROP TABLE xl_image;
DROP TABLE xl_variant;
DROP TABLE xl_product;
"""


def write(path: Path, content: str) -> bool:
    """Ghi tệp nếu nội dung đổi. Trả True khi có thay đổi."""
    if path.exists() and path.read_text(encoding="utf-8") == content:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8", newline="\n")
    return True
