# Đọc + kiểm Excel (plan 2026-11-21 E02, E06.01): mỗi loại lỗi báo đúng ô.
from __future__ import annotations

from pathlib import Path

from conftest import make_book, product_row

from xlseed import reader, refdata
from xlseed.model import Report


def _read(path: Path, ref) -> tuple[list, Report]:
    report = Report()
    return reader.read(path, ref, report), report


def _errors(report: Report) -> list[str]:
    return [str(e) for e in report.errors]


def test_reads_minimal_product_with_default_variant(tmp_path, ref):
    f = tmp_path / "p.xlsx"
    make_book(f, [product_row("tn01", anh="a.jpg; b.png\nhttps://cdn.example.com/c.jpg")])
    products, report = _read(f, ref)
    assert report.ok, _errors(report)
    [p] = products
    assert p.code == "TN01"
    assert p.status == "PUBLISHED" and p.item_condition == "NEW"
    assert [(i.kind, i.name) for i in p.images] == [("file", "a.jpg"), ("file", "b.png"),
                                                    ("url", "https://cdn.example.com/c.jpg")]
    [v] = p.variants
    assert (v.code, v.name, v.price, v.stock, v.sku(p.code)) == ("STD", "Mặc định", 389000, 10, "XL-TN01-STD")


def test_reports_errors_with_cell_reference(tmp_path, ref):
    f = tmp_path / "p.xlsx"
    make_book(f, [
        product_row("TN01", gia=12.5),
        product_row("TN 02", ten="Khác"),
        product_row("TN03", email_chu_shop="ai@la.vn", ten="Ba"),
        product_row("TN04", slug_danh_muc="khong-co", ten="Bốn"),
        product_row("TN05", anh="http://x.com/a.jpg", ten="Năm"),
        product_row("TN06", anh="../ngoai.jpg", ten="Sáu"),
        product_row("TN07", ten="Giá ${x}"),
        product_row("TN08", can_nang_g=None, ten="Tám"),
    ])
    _, report = _read(f, ref)
    errors = "\n".join(_errors(report))
    assert "SanPham!E2: phải là số nguyên VND" in errors
    assert "SanPham!A3: chỉ gồm chữ không dấu và số" in errors
    assert "SanPham!B4: không có shop của 'ai@la.vn'" in errors
    assert "SanPham!C5: không có danh mục 'khong-co'" in errors
    assert "SanPham!H6: link ảnh phải là https://" in errors
    assert "SanPham!H7: chỉ ghi tên tệp trong folder anh/" in errors
    assert "SanPham!D8: không được chứa chuỗi '${'" in errors
    assert "SanPham!G9: bắt buộc" in errors


def test_accepts_vietnamese_number_format_and_flags(tmp_path, ref):
    f = tmp_path / "p.xlsx"
    header = ["ma_sp", "email_chu_shop", "slug_danh_muc", "ten", "gia", "ton_kho", "can_nang_g", "anh",
              "mien_phi_ship", "trang_thai", "tinh_trang", "thuong_hieu", "diem_sao"]
    make_book(f, [["A1", "MAI.TRAN@gmail.com", "thiet-bi-am-thanh", "  Tai   nghe  ", "1.290.000", "5", 350, "a.jpg",
                   "x", "draft", "used", "SONY", 4.5]], product_header=header)
    [p], report = _read(f, ref)
    assert report.ok, _errors(report)
    assert (p.price, p.owner_email, p.name, p.free_ship, p.status, p.item_condition, p.brand, p.rating) == (
        1290000, "mai.tran@gmail.com", "Tai nghe", True, "DRAFT", "USED", "Sony", 4.5)


def test_duplicates_and_missing_required_column(tmp_path, ref):
    f = tmp_path / "p.xlsx"
    make_book(f, [product_row("TN01"), product_row("tn01", ten="Khác"), product_row("TN02", ten="Tai nghe TN01")])
    _, report = _read(f, ref)
    errors = "\n".join(_errors(report))
    assert "SanPham!A3: mã 'TN01' trùng với dòng 2" in errors
    assert "SanPham!D4: shop này đã có sản phẩm cùng tên" in errors

    g = tmp_path / "q.xlsx"
    make_book(g, [["TN01"]], product_header=["ma_sp"])
    _, report = _read(g, ref)
    assert "SanPham!1: thiếu cột bắt buộc 'gia'" in _errors(report)


def test_variants_and_specs(tmp_path, ref):
    f = tmp_path / "p.xlsx"
    make_book(f, [product_row("TN01", ton_kho=None)],
              variants=[["TN01", "blk", None, "color=Đen; size=M", None, 5, None],
                        ["TN01", "WHT", "Trắng", "color=Trắng", 399000, 0, "w.jpg"],
                        ["TN01", "BLK", "Lặp", None, None, 1, None],
                        ["XX99", "A", None, None, None, 1, None],
                        ["TN01", "BAD", None, "color Đen", None, 1, None]],
              specs=[["TN01", "warranty", "12 tháng"], ["TN01", "Mau_Sac", "x"], ["TN01", "khac", "y"],
                     ["TN01", "warranty", "24 tháng"]])
    [p], report = _read(f, ref)
    errors = "\n".join(_errors(report))
    assert "BienThe!B4: biến thể 'BLK' của TN01 bị lặp" in errors
    assert "BienThe!A5: mã sản phẩm 'XX99' không có trong sheet SanPham" in errors
    assert "BienThe!D6: viết dạng 'khoa=gia tri" in errors
    assert "ThongSo!B5: thông số 'warranty' của TN01 bị lặp" in errors
    assert any("ThongSo!B4" in str(w) and "không phải thuộc tính lọc" in str(w) for w in report.warnings)
    assert [(v.code, v.name, v.price, v.stock, v.attributes) for v in p.variants] == [
        ("BLK", "Đen / M", 389000, 5, {"color": "Đen", "size": "M"}),
        ("WHT", "Trắng", 399000, 0, {"color": "Trắng"})]
    assert p.variants[1].image is not None and p.variants[1].image.name == "w.jpg"
    assert p.specs == {"warranty": "12 tháng", "mau_sac": "x", "khac": "y"}


def test_needs_image_and_stock(tmp_path, ref):
    f = tmp_path / "p.xlsx"
    make_book(f, [product_row("TN01", anh=None, ton_kho=None)])
    _, report = _read(f, ref)
    errors = "\n".join(_errors(report))
    assert "SanPham!dòng 2: cần ít nhất 1 ảnh" in errors
    assert "SanPham!dòng 2: không có biến thể ở sheet BienThe thì phải điền 'ton_kho'" in errors


def test_refdata_reads_repo_seed_files():
    ref = refdata.load()
    assert "thiet-bi-am-thanh" in ref.categories and "dien-tu" in ref.categories
    assert ref.shops.get("mai.tran@gmail.com") == "TechZone Official"
    assert ref.brand_name("biti's") == "Biti's"
    assert "warranty" in ref.attributes and not ref.missing
