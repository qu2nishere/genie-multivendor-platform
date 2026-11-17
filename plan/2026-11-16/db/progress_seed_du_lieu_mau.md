# PROGRESS — Seed dữ liệu mẫu chạy được trên mọi CSDL (danh mục, shop, sản phẩm, voucher, banner…)

| | |
|---|---|
| **Plan** | [plan_seed_du_lieu_mau.md](./plan_seed_du_lieu_mau.md) — chi tiết từng task tra theo mã `Sxx.yy` |
| **Ngày lập** | 2026-11-16 |
| **Tổng** | 58 task + 10 commit, 9 giai đoạn |
| **Nhánh** | `feature/db-seed-demo` |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-16 — mới lập plan, chưa bắt đầu (0 / 68) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục Ghi chú.
- Task `COMMIT` chỉ tick sau khi qua Cổng kiểm tra (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`.
- Thông điệp commit ghi sẵn bên dưới, thêm ` [#GNE-<số>]` vào cuối khi đã có mã Jira.
- Cập nhật cột "Xong" của bảng Tổng quan và dòng "Cập nhật gần nhất" sau mỗi buổi làm; ghi một dòng vào Nhật ký cuối trang.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | Đặt file seed mới ở đâu để vừa Flyway chạy được vừa `psql` chạy được | ☐ | |
| D2 | Thứ tự chạy giữa `R__seed_demo` (tài khoản) và file mới | ☐ | |
| D3 | Script nạp cho CSDL bất kỳ xử lý placeholder của `R__seed_demo.sql` thế nào | ☐ | |
| D4 | Số lượng dữ liệu: mock web có 6 shop / 23 danh mục / 20 sản phẩm; implement 3.1 ghi 5 shop / 30 danh mục / ~200 sản phẩm | ☐ | |
| D5 | Chủ shop TechZone | ☐ | |
| D6 | Ảnh sản phẩm / banner / logo | ☐ | |
| D7 | Kiểm seed không bị hỏng khi schema đổi | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong | Mã Jira | Nhánh |
|---|---|---:|---:|---:|---|---|
| S00 | Chuẩn bị | 6 | 0 | 0 / 6 | — | `feature/db-seed-demo` |
| S01 | Khung nạp seed cho CSDL bất kỳ | 7 | 1 | 0 / 8 | — | `feature/db-seed-demo` |
| S02 | Danh mục, thương hiệu, thuộc tính | 6 | 1 | 0 / 7 | — | `feature/db-seed-demo` |
| S03 | Shop đã KYC | 9 | 1 | 0 / 10 | — | `feature/db-seed-demo` |
| S04 | Sản phẩm, SKU, ảnh | 7 | 2 | 0 / 9 | — | `feature/db-seed-demo` |
| S05 | Voucher & banner | 5 | 1 | 0 / 6 | — | `feature/db-seed-demo` |
| S06 | Hoạt động của khách mẫu & hàng chờ KYC | 8 | 1 | 0 / 9 | — | `feature/db-seed-demo` |
| S07 | Kiểm thử tự động & tài liệu | 7 | 2 | 0 / 9 | — | `feature/db-seed-demo` |
| S08 | (Tùy chọn — D4) Mở rộng lên ~200 sản phẩm | 3 | 1 | 0 / 4 | — | `feature/db-seed-demo` |
| **Tổng** | | **58** | **10** | **0 / 68** | | |

## S00 — Chuẩn bị

- [ ] **S00.01** Kiểm danh tính git
- [ ] **S00.02** Chốt quyết định D1–D7
- [ ] **S00.03** Tạo nhánh
- [ ] **S00.04** Dựng môi trường
- [ ] **S00.05** Tái hiện lỗi script cũ
- [ ] **S00.06** Lập bảng ánh xạ cột

## S01 — Khung nạp seed cho CSDL bất kỳ

- [ ] **S01.01** Tạo thư mục `server-spring/src/main/resources/db/seed/` + tệp `R__seed_demo_10_categories.sql` rỗng có chú thích đầu file (mục đích, nguyên tắc N1–N3, cách chạy).
- [ ] **S01.02** `application-local.yml`
- [ ] **S01.03** `scripts/seed/seed.sh`
- [ ] **S01.04** `scripts/seed/seed.ps1`
- [ ] **S01.05** `scripts/seed-local.sh`
- [ ] **S01.06** `scripts/seed/README.md`
- [ ] **S01.07** Thử khung
- [ ] **S01.C1** 🔖 COMMIT `feat(db): add seed runner that works against any postgres database` · hash: `…`

## S02 — Danh mục, thương hiệu, thuộc tính

- [ ] **S02.01** `R__seed_demo_10_categories.sql`
- [ ] **S02.02** 13 danh mục con từ `SEED_SUB_CATEGORIES`
- [ ] **S02.03** Tỷ lệ hoa hồng theo ngành
- [ ] **S02.04** `R__seed_demo_20_brands_attributes.sql`
- [ ] **S02.05** Thuộc tính ngành
- [ ] **S02.06** Kiểm
- [ ] **S02.C1** 🔖 COMMIT `feat(db): seed categories, brands and category attributes` · hash: `…`

## S03 — Shop đã KYC

- [ ] **S03.01** `R__seed_demo_30_shops.sql`
- [ ] **S03.02** Chủ shop TechZone (D5)
- [ ] **S03.03** `vendor_profiles`
- [ ] **S03.04** Kho lấy hàng
- [ ] **S03.05** Tài khoản nhận tiền
- [ ] **S03.06** `vendor_wallets`
- [ ] **S03.07** `vendor_shipping_config`
- [ ] **S03.08** Shop đang nghỉ
- [ ] **S03.09** Kiểm
- [ ] **S03.C1** 🔖 COMMIT `feat(db): seed verified shops with wallets and shipping config` · hash: `…`

## S04 — Sản phẩm, SKU, ảnh

- [ ] **S04.01** `scripts/seed/upload-assets.sh` + `.ps1` (D6)
- [ ] **S04.02** `R__seed_demo_40_products.sql`
- [ ] **S04.03** `product_variants`
- [ ] **S04.04** `product_images`
- [ ] **S04.05** Cột tìm kiếm
- [ ] **S04.06** Đồng bộ bộ đếm shop
- [ ] **S04.07** Kiểm
- [ ] **S04.C1** 🔖 COMMIT `feat(db): seed published products with variants and images` · hash: `…`
- [ ] **S04.C2** 🔖 COMMIT `feat(db): add script to upload seed images to object storage` · hash: `…`

## S05 — Voucher & banner

- [ ] **S05.01** `R__seed_demo_50_vouchers.sql`
- [ ] **S05.02** Voucher đổi Điểm
- [ ] **S05.03** `R__seed_demo_60_banners.sql`
- [ ] **S05.04** Trạng thái banner đa dạng
- [ ] **S05.05** Kiểm
- [ ] **S05.C1** 🔖 COMMIT `feat(db): seed vouchers, coin reward vouchers and home banners` · hash: `…`

## S06 — Hoạt động của khách mẫu & hàng chờ KYC

- [ ] **S06.01** `R__seed_demo_70_customer_activity.sql`
- [ ] **S06.02** `coin_transactions`
- [ ] **S06.03** `daily_checkins`
- [ ] **S06.04** `user_wishlists`
- [ ] **S06.05** `R__seed_demo_80_kyc_queue.sql`
- [ ] **S06.06** `kyc_results` cho 3 hồ sơ chờ duyệt
- [ ] **S06.07** Nếu plan Mở shop & KYC đã thêm cột `V15` (`kyc_supplement_deadline_at`…) thì điền trong file này bằng khối `DO $$ … IF EXISTS (cột) …` để file chạy được cả trước và sau `V15`.
- [ ] **S06.08** Kiểm
- [ ] **S06.C1** 🔖 COMMIT `feat(db): seed sample customer activity and kyc review queue` · hash: `…`

## S07 — Kiểm thử tự động & tài liệu

- [ ] **S07.01** `SeedDataIT` (D7)
- [ ] **S07.02** Kiểm idempotent trong `SeedDataIT`
- [ ] **S07.03** Kiểm enum / `CHECK`
- [ ] **S07.04** Tài liệu trong repo
- [ ] **S07.05** Tài liệu nguồn
- [ ] **S07.06** Thử trên máy "sạch"
- [ ] **S07.C1** 🔖 COMMIT `test(db): verify demo seed is complete and idempotent` · hash: `…`
- [ ] **S07.C2** 🔖 COMMIT `docs(db): document demo seed files and seed scripts` · hash: `…`
- [ ] **S07.07** Mở PR vào `develop`

## S08 — (Tùy chọn — D4) Mở rộng lên ~200 sản phẩm

- [ ] **S08.01** `R__seed_demo_90_bulk_products.sql`
- [ ] **S08.02** Kiểm hiệu năng tìm kiếm trên ~200 sản phẩm (`EXPLAIN ANALYZE` truy vấn full-text dùng index GIN)
- [ ] **S08.03** Bổ sung khẳng định trong `SeedDataIT` (tổng ≥ 200 khi có file 90).
- [ ] **S08.C1** 🔖 COMMIT `feat(db): seed bulk products for search and paging tests` · hash: `…`

---

## Chốt plan — tick tiến độ use case

Plan này không gắn use case nào nên **không tick** ở checklist use case; chỉ:

- [ ] Tick dòng plan này ở bảng *Tiến độ các plan* trong [README](../README.md) mục 6
- [ ] Thêm một dòng vào mục 5 *Nhật ký cập nhật* của [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../../docs/Document/CHECKLIST_TIEN_DO_UC.md) (đã có bộ seed, cách chạy)

## Ghi chú

Ghi ở đây: lý do bỏ qua task (`[-]`), chỗ làm khác plan, số đo, việc tồn chuyển sang plan khác.

| Task | Ghi chú |
|---|---|
| | |

## Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-16 | Lập plan + progress (58 task, 10 commit, 9 giai đoạn) — chưa bắt đầu làm | — |
