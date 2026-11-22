#!/usr/bin/env python3
# Công cụ seed sản phẩm từ Excel — CHỈ cho máy dev (plan 2026-11-21 db/plan_seed_tu_excel.md; ADR 0010).
#   template <file.xlsx> [--force]   tạo file Excel mẫu có danh sách chọn shop / danh mục
#   check    <file.xlsx>             chỉ đọc + kiểm, không ghi gì
#   build    <file.xlsx>             kiểm rồi ghi ảnh thu nhỏ vào assets/ + db/seed/R__seed_demo_95_excel_products.sql
#            [--apply]               + upload-assets (Silo) + seed.sh --docker --only 95
#            [--flush-cache]         + xóa cache catalog trong Redis Docker
# Mã thoát: 0 ổn · 1 lỗi dữ liệu · 2 dùng lệnh sai · 3 lỗi khi nạp. Hướng dẫn: scripts/seed/excel/README.md
from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from xlseed import images, paths, refdata, reader, runner, sql_writer, template  # noqa: E402
from xlseed.model import Report  # noqa: E402

MAX_PRINTED = 50


def _utf8_console() -> None:
    # Console Windows mặc định cp1252 → in tiếng Việt lỗi UnicodeEncodeError
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8")  # type: ignore[attr-defined]
        except (AttributeError, ValueError):
            pass


def print_report(report: Report) -> None:
    for title, issues in (("Cảnh báo", report.warnings), ("Lỗi", report.errors)):
        if not issues:
            continue
        print(f"\n{title} ({len(issues)}):", file=sys.stderr if title == "Lỗi" else sys.stdout)
        for issue in issues[:MAX_PRINTED]:
            print(f"  - {issue}", file=sys.stderr if title == "Lỗi" else sys.stdout)
        if len(issues) > MAX_PRINTED:
            print(f"  … và {len(issues) - MAX_PRINTED} mục nữa", file=sys.stderr if title == "Lỗi" else sys.stdout)


def cmd_template(args: argparse.Namespace) -> int:
    target = Path(args.file)
    if target.exists() and not args.force:
        print(f"Đã có {target} — không ghi đè (thêm --force nếu muốn tạo lại).", file=sys.stderr)
        return 1
    ref = refdata.load()
    template.build(target, ref)
    print(f"Đã tạo {target}: {len(ref.shops)} shop, {len(ref.categories)} danh mục trong danh sách chọn.")
    print(f"Bỏ ảnh vào folder {paths.image_dir_for(target)} rồi ghi tên tệp vào cột 'anh'.")
    return 0


def read_checked(path: Path, *, write: bool) -> tuple[list, Report, images.AssetStore]:
    """Đọc + kiểm Excel rồi xử lý ảnh. write=False: không ghi assets/ (lệnh check)."""
    report = Report()
    products = reader.read(path, refdata.load(), report, attach_embedded=images.collect_embedded)
    store = images.AssetStore(write=write)
    if report.ok:
        images.resolve(products, paths.image_dir_for(path), store, report)
    return products, report, store


def cmd_check(args: argparse.Namespace) -> int:
    products, report, _ = read_checked(Path(args.file), write=False)
    print_report(report)
    if not report.ok:
        print(f"\nCó {len(report.errors)} lỗi — sửa file Excel rồi chạy lại.", file=sys.stderr)
        return 1
    variants = sum(len(p.variants) for p in products)
    pictures = sum(len(p.resolved_images) + sum(1 for v in p.variants if v.resolved_image) for p in products)
    print(f"\nỔn: {len(products)} sản phẩm, {variants} biến thể, {pictures} ảnh.")
    return 0


def cmd_build(args: argparse.Namespace) -> int:
    source = Path(args.file)
    products, report, store = read_checked(source, write=True)
    print_report(report)
    if not report.ok:
        print(f"\nCó {len(report.errors)} lỗi — chưa ghi file nào. Sửa file Excel rồi chạy lại.", file=sys.stderr)
        return 1
    changed = sql_writer.write(paths.SQL_FILE, sql_writer.render(products, source.name))
    added, removed = store.save()
    variants = sum(len(p.variants) for p in products)
    print(f"\nĐã xử lý {len(products)} sản phẩm, {variants} biến thể.")
    print(f"  SQL : {paths.rel(paths.SQL_FILE)} ({'đã cập nhật' if changed else 'không đổi'})")
    print(f"  Ảnh : {paths.rel(paths.ASSETS_DIR)}/ (+{len(added)} mới, -{len(removed)} xóa)")
    print("Commit: file SQL + folder assets/ + file Excel — KHÔNG commit folder anh/.")
    if args.apply:
        code = runner.apply()
        if code != 0:
            return 3
    if args.flush_cache:
        if runner.flush_cache() != 0:
            return 3
    elif args.apply:
        runner.remind_cache()
    return 0


def main(argv: list[str] | None = None) -> int:
    _utf8_console()
    parser = argparse.ArgumentParser(prog="excel_to_seed.py", description="Seed sản phẩm từ file Excel (máy dev).")
    sub = parser.add_subparsers(dest="command", required=True)

    p = sub.add_parser("template", help="tạo file Excel mẫu")
    p.add_argument("file")
    p.add_argument("--force", action="store_true", help="ghi đè nếu file đã có")
    p.set_defaults(func=cmd_template)

    p = sub.add_parser("check", help="chỉ kiểm file Excel, không ghi gì")
    p.add_argument("file")
    p.set_defaults(func=cmd_check)

    p = sub.add_parser("build", help="kiểm rồi ghi ảnh vào assets/ + file SQL seed 95")
    p.add_argument("file")
    p.add_argument("--apply", action="store_true",
                   help="sau khi ghi: đẩy ảnh lên Silo (upload-assets) và nạp seed 95 vào PostgreSQL Docker")
    p.add_argument("--flush-cache", action="store_true", help="xóa cache catalog trong Redis Docker")
    p.set_defaults(func=cmd_build)

    args = parser.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
