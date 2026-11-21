# Sinh SQL seed (plan 2026-11-21 E04, E06.01): tất định, escape đúng, đủ biến thể / ảnh. Chạy thật trên PG: SeedDataIT.
from __future__ import annotations

from conftest import make_book, png_bytes, product_row

from xlseed import images, reader, sql_writer
from xlseed.model import Report


def _products(tmp_path, ref):
    anh = tmp_path / "anh"
    anh.mkdir(exist_ok=True)
    (anh / "a.jpg").write_bytes(png_bytes((10, 200, 10), (500, 500)))
    (anh / "w.jpg").write_bytes(png_bytes((250, 250, 250), (500, 500)))
    f = tmp_path / "p.xlsx"
    make_book(f, [product_row("TN02", ten="Áo 'basic'", slug_danh_muc="thoi-trang-nam", ton_kho=None),
                  product_row("TN01", anh="a.jpg; https://cdn.example.com/x.jpg")],
              variants=[["TN02", "WHT", "Trắng", "color=Trắng", 99000, 4, "w.jpg"]],
              specs=[["TN01", "warranty", "12 tháng"]])
    report = Report()
    products = reader.read(f, ref, report, attach_embedded=images.collect_embedded)
    images.resolve(products, anh, images.AssetStore(tmp_path / "assets"), report)
    assert report.ok, [str(e) for e in report.errors]
    return products


def test_literals():
    assert sql_writer.lit(None) == "NULL"
    assert sql_writer.lit(True) == "TRUE"
    assert sql_writer.lit(389000) == "389000"
    assert sql_writer.lit(4.5) == "4.5"
    assert sql_writer.lit("Biti's \\ x\x00") == "'Biti''s \\ x'"
    assert sql_writer.jsonb({"size": "M", "color": "Đen"}) == """'{"color": "Đen", "size": "M"}'::jsonb"""
    assert sql_writer.jsonb({}) == "NULL"


def test_render_is_deterministic_and_complete(tmp_path, ref):
    products = _products(tmp_path, ref)
    sql = sql_writer.render(products, "products.xlsx")
    assert sql == sql_writer.render(list(reversed(products)), "products.xlsx")
    assert sql.startswith("-- Dữ liệu mẫu 95")
    assert "FILE SINH TỰ ĐỘNG" in sql and "${" not in sql and "\r" not in sql
    assert "-- Nguồn: products.xlsx · 2 sản phẩm, 2 biến thể, 4 ảnh." in sql
    assert sql.index("'TN01'") < sql.index("'TN02'")
    assert "'XL-TN01-STD', 'Mặc định', NULL, 389000, 10, NULL" in sql
    assert "'XL-TN02-WHT', 'Trắng', '{\"color\": \"Trắng\"}'::jsonb, 99000, 4, 'seed/excel/" in sql
    assert "'Áo ''basic'''" in sql
    assert "'https://cdn.example.com/x.jpg', NULL, NULL, NULL, NULL, FALSE, 1" in sql
    assert "'{\"warranty\": \"12 tháng\"}'::jsonb" in sql
    assert "RAISE EXCEPTION 'Seed Excel: chưa có shop VERIFIED" in sql
    assert sql.rstrip().endswith("DROP TABLE xl_product;")


def test_write_only_when_changed(tmp_path):
    target = tmp_path / "out.sql"
    assert sql_writer.write(target, "SELECT 1;\n") is True
    assert sql_writer.write(target, "SELECT 1;\n") is False
    assert target.read_bytes() == b"SELECT 1;\n"
