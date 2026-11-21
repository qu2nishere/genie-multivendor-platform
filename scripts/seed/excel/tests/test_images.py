# Ảnh cho seed Excel (plan 2026-11-21 E03, E06.01): tên tệp, link, ảnh nổi, Place in Cell, manifest, dọn mồ côi.
from __future__ import annotations

import io
import json
import re
import zipfile
from pathlib import Path

from conftest import make_book, png_bytes, product_row
from openpyxl import load_workbook
from openpyxl.drawing.image import Image as XlImage
from PIL import Image

from xlseed import images, reader
from xlseed.model import Report


def _read(path: Path, ref, report: Report):
    return reader.read(path, ref, report, attach_embedded=images.collect_embedded)


def _jpeg_sizes(path: Path) -> tuple[int, int]:
    with Image.open(path) as img:
        assert img.format == "JPEG"
        return img.size


def test_file_images_are_normalized_and_reused_from_manifest(tmp_path, ref):
    anh = tmp_path / "anh"
    anh.mkdir()
    (anh / "Big.PNG").write_bytes(png_bytes(size=(2400, 1200), mode="RGBA"))
    (anh / "nho.jpg").write_bytes(png_bytes(size=(100, 80)))   # đuôi .jpg nhưng nội dung PNG — Pillow vẫn đọc
    f = tmp_path / "p.xlsx"
    make_book(f, [product_row("TN01", anh="big.png; NHO.jpg; https://cdn.example.com/x.jpg")])
    assets = tmp_path / "assets"

    report = Report()
    [p] = _read(f, ref, report)
    store = images.AssetStore(assets)
    images.resolve([p], anh, store, report)
    store.save()
    assert report.ok, [str(e) for e in report.errors]
    big, small, link = p.resolved_images
    assert re.fullmatch(r"seed/excel/[0-9a-f]{8}\.jpg", big.image_url)
    assert (big.width, big.height, big.mime_type) == (1200, 600, "image/jpeg")
    assert _jpeg_sizes(assets / big.image_url.split("/")[-1]) == (1200, 600)
    assert link.image_url == "https://cdn.example.com/x.jpg" and link.width is None
    assert any("ảnh nhỏ" in str(w) for w in report.warnings)
    assert any("link ngoài" in str(w) for w in report.warnings)
    manifest = json.loads((assets / "manifest.json").read_text(encoding="utf-8"))
    assert sorted(manifest["images"]) == ["anh/big.png", "anh/nho.jpg"]

    # người khác: không có folder anh/ → dùng lại bản thu nhỏ trong manifest
    report2 = Report()
    [p2] = _read(f, ref, report2)
    images.resolve([p2], tmp_path / "khong-co", images.AssetStore(assets), report2)
    assert report2.ok, [str(e) for e in report2.errors]
    assert [i.image_url for i in p2.resolved_images] == [i.image_url for i in p.resolved_images]


def test_missing_file_and_bad_image_are_errors(tmp_path, ref):
    anh = tmp_path / "anh"
    anh.mkdir()
    (anh / "hong.jpg").write_bytes(b"khong phai anh")
    f = tmp_path / "p.xlsx"
    make_book(f, [product_row("TN01", anh="thieu.jpg; hong.jpg; a.gif")])
    report = Report()
    [p] = _read(f, ref, report)
    images.resolve([p], anh, images.AssetStore(tmp_path / "assets", write=False), report)
    errors = "\n".join(str(e) for e in report.errors)
    assert "SanPham!H2: không thấy tệp 'thieu.jpg'" in errors
    assert "SanPham!H2: hong.jpg: không đọc được ảnh" in errors
    assert "SanPham!H2: 'a.gif': chỉ nhận ảnh" in errors
    assert not (tmp_path / "assets").exists()   # write=False không ghi gì


def test_orphans_are_removed_and_unchanged_source_keeps_asset(tmp_path, ref):
    anh = tmp_path / "anh"
    anh.mkdir()
    (anh / "a.jpg").write_bytes(png_bytes((10, 200, 10), (500, 500)))
    (anh / "b.jpg").write_bytes(png_bytes((10, 10, 200), (500, 500)))
    f = tmp_path / "p.xlsx"
    assets = tmp_path / "assets"
    make_book(f, [product_row("TN01", anh="a.jpg; b.jpg")])
    report = Report()
    [p] = _read(f, ref, report)
    store = images.AssetStore(assets)
    images.resolve([p], anh, store, report)
    store.save()
    first = sorted(x.name for x in assets.glob("*.jpg"))
    assert len(first) == 2

    make_book(f, [product_row("TN01", anh="a.jpg")])
    report = Report()
    [p] = _read(f, ref, report)
    store = images.AssetStore(assets)
    images.resolve([p], anh, store, report)
    _, removed = store.save()
    assert len(removed) == 1 and sorted(x.name for x in assets.glob("*.jpg")) == [p.resolved_images[0].image_url[-12:]]


def test_floating_images_follow_anchor_cell(tmp_path, ref):
    f = tmp_path / "p.xlsx"
    header = ["ma_sp", "email_chu_shop", "slug_danh_muc", "ten", "gia", "ton_kho", "can_nang_g", "anh", "anh_2"]
    wb = make_book(f, [["TN01", "mai.tran@gmail.com", "thiet-bi-am-thanh", "Tai nghe", 389000, 3, 350, None, None]],
                   product_header=header, variants=[["TN01", "BLK", None, "color=Đen", None, 2, None]])
    ws = wb["SanPham"]
    ws.add_image(XlImage(io.BytesIO(png_bytes((1, 2, 3), (400, 400)))), "I2")   # anh_2
    ws.add_image(XlImage(io.BytesIO(png_bytes((4, 5, 6), (400, 400)))), "H2")   # anh → ảnh chính
    ws.add_image(XlImage(io.BytesIO(png_bytes((7, 8, 9), (400, 400)))), "D5")   # ngoài cột ảnh
    wb["BienThe"].add_image(XlImage(io.BytesIO(png_bytes((9, 9, 9), (400, 400)))), "G2")
    wb.save(f)

    report = Report()
    [p] = _read(f, ref, report)
    assert [i.where for i in p.images] == ["SanPham!H2", "SanPham!I2"]
    assert p.variants[0].image is not None and p.variants[0].image.kind == "embedded"
    assert [str(e) for e in report.errors] == [
        "SanPham!D5: ảnh chèn không nằm trong cột ảnh (anh, anh_2 … anh_8) của dòng sản phẩm nào"]


def _add_place_in_cell(path: Path, sheet_xml: str, cell: str, media: bytes) -> None:
    """Biến ô `cell` thành ảnh "Place in Cell" đúng cấu trúc Excel 365 (metadata.xml + xl/richData/*)."""
    src = zipfile.ZipFile(path)
    parts = {n: src.read(n) for n in src.namelist()}
    src.close()
    xml = parts[sheet_xml].decode("utf-8")
    xml, n = re.subn(rf'<c r="{cell}"[^>]*?(/>|>.*?</c>)', f'<c r="{cell}" t="e" vm="1"><v>#VALUE!</v></c>', xml)
    assert n == 1
    parts[sheet_xml] = xml.encode("utf-8")
    parts["xl/metadata.xml"] = b'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<metadata xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:xlrd="http://schemas.microsoft.com/office/spreadsheetml/2017/richdata">
<metadataTypes count="1"><metadataType name="XLRICHVALUE" minSupportedVersion="120000"/></metadataTypes>
<futureMetadata name="XLRICHVALUE" count="1"><bk><extLst><ext uri="{3e2802c4-a4d2-4d8b-9148-e3be6c30e623}"><xlrd:rvb i="0"/></ext></extLst></bk></futureMetadata>
<valueMetadata count="1"><bk><rc t="1" v="0"/></bk></valueMetadata></metadata>'''
    parts["xl/richData/rdrichvalue.xml"] = b'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<rvData xmlns="http://schemas.microsoft.com/office/spreadsheetml/2017/richdata" count="1"><rv s="0"><v>0</v><v>5</v></rv></rvData>'''
    parts["xl/richData/rdrichvaluestructure.xml"] = b'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<rvStructures xmlns="http://schemas.microsoft.com/office/spreadsheetml/2017/richdata" count="1"><s t="_localImage"><k n="_rvRel:LocalImageIdentifier" t="i"/><k n="CalcOrigin" t="i"/></s></rvStructures>'''
    parts["xl/richData/richValueRel.xml"] = b'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<richValueRels xmlns="http://schemas.microsoft.com/office/spreadsheetml/2022/richvaluerel" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><rel r:id="rId1"/></richValueRels>'''
    parts["xl/richData/_rels/richValueRel.xml.rels"] = b'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/image1.png"/></Relationships>'''
    parts["xl/media/image1.png"] = media
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as out:
        for name, data in parts.items():
            out.writestr(name, data)


def test_place_in_cell_images_are_read(tmp_path, ref):
    f = tmp_path / "p.xlsx"
    make_book(f, [product_row("TN01", anh="PLACEHOLDER")])
    wb = load_workbook(f)
    sheet_index = wb.sheetnames.index("SanPham") + 1
    _add_place_in_cell(f, f"xl/worksheets/sheet{sheet_index}.xml", "H2", png_bytes((50, 60, 70), (600, 400)))

    report = Report()
    [p] = _read(f, ref, report)
    assert report.ok, [str(e) for e in report.errors]
    [img] = p.images
    assert img.kind == "embedded" and img.where == "SanPham!H2"
    with Image.open(io.BytesIO(img.data or b"")) as decoded:
        assert decoded.size == (600, 400)
