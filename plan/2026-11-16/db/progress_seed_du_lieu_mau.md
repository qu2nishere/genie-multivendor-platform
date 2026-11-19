# PROGRESS — Seed dữ liệu mẫu chạy được trên mọi CSDL (danh mục, shop, sản phẩm, voucher, banner…)

| | |
|---|---|
| **Plan** | [plan_seed_du_lieu_mau.md](./plan_seed_du_lieu_mau.md) — chi tiết từng task tra theo mã `Sxx.yy` |
| **Ngày lập** | 2026-11-16 |
| **Tổng** | 58 task + 10 commit, 9 giai đoạn |
| **Nhánh** | `feature/db-seed-demo` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-17 — xong 66 / 68 (S07.06 thử máy khác và S07.07 mở PR còn chờ người dùng) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục Ghi chú.
- Task `COMMIT` chỉ tick sau khi qua Cổng kiểm tra (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`.
- Thông điệp commit ghi sẵn bên dưới, thêm ` [#GNE-<số>]` vào cuối khi đã có mã Jira.
- Cập nhật cột "Xong" của bảng Tổng quan và dòng "Cập nhật gần nhất" sau mỗi buổi làm; ghi một dòng vào Nhật ký cuối trang.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | Đặt file seed mới ở đâu để vừa Flyway chạy được vừa `psql` chạy được | ☑ | Thư mục `db/seed/` + `SeedDataFlywayCustomizer` nối location khi `genie.seed.demo-data=true` (`GENIE_SEED_DEMO`); placeholder `seed_demo` đọc cùng giá trị; profile test giữ `seed_demo: true` cho tài khoản, `demo-data: false` · 2026-11-17 |
| D2 | Thứ tự chạy giữa `R__seed_demo` (tài khoản) và file mới | ☑ | Tên `R__seed_demo_<NN>_…` — Flyway chạy sau `R__seed_demo` (đã thử trên CSDL trống) · 2026-11-17 |
| D3 | Script nạp cho CSDL bất kỳ xử lý placeholder của `R__seed_demo.sql` thế nào | ☑ | `sed` / `-replace` thay placeholder `seed_demo` → `true`; file `db/seed` đổ nguyên văn · 2026-11-17 |
| D4 | Số lượng dữ liệu: mock web có 6 shop / 23 danh mục / 20 sản phẩm; implement 3.1 ghi 5 shop / 30 danh mục / ~200 sản phẩm | ☑ | Làm cả hai mức: 20 sản phẩm mock + 180 sản phẩm sinh tất định (`R__seed_demo_90_bulk_products.sql`) · 2026-11-17 |
| D5 | Chủ shop TechZone | ☑ | Như đề xuất — `mai.tran` thành `VENDOR`, chủ TechZone Official · 2026-11-17 |
| D6 | Ảnh sản phẩm / banner / logo | ☑ | Như đề xuất; khóa `seed/product/…` (product-media), `seed/banner/…` (content-assets) · 2026-11-17 |
| D7 | Kiểm seed không bị hỏng khi schema đổi | ☑ | `SeedDataIT` (Flyway trực tiếp trên PG 16 Testcontainers, 4 test) · 2026-11-17 |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong | Mã Jira | Nhánh |
|---|---|---:|---:|---:|---|---|
| S00 | Chuẩn bị | 6 | 0 | 6 / 6 | — | `feature/db-seed-demo` |
| S01 | Khung nạp seed cho CSDL bất kỳ | 7 | 1 | 8 / 8 | — | `feature/db-seed-demo` |
| S02 | Danh mục, thương hiệu, thuộc tính | 6 | 1 | 7 / 7 | — | `feature/db-seed-demo` |
| S03 | Shop đã KYC | 9 | 1 | 10 / 10 | — | `feature/db-seed-demo` |
| S04 | Sản phẩm, SKU, ảnh | 7 | 2 | 9 / 9 | — | `feature/db-seed-demo` |
| S05 | Voucher & banner | 5 | 1 | 6 / 6 | — | `feature/db-seed-demo` |
| S06 | Hoạt động của khách mẫu & hàng chờ KYC | 8 | 1 | 9 / 9 | — | `feature/db-seed-demo` |
| S07 | Kiểm thử tự động & tài liệu | 7 | 2 | 7 / 9 | — | `feature/db-seed-demo` |
| S08 | (Tùy chọn — D4) Mở rộng lên ~200 sản phẩm | 3 | 1 | 4 / 4 | — | `feature/db-seed-demo` |
| **Tổng** | | **58** | **10** | **66 / 68** | | |

## S00 — Chuẩn bị

- [x] **S00.01** Kiểm danh tính git
- [x] **S00.02** Chốt quyết định D1–D7
- [x] **S00.03** Tạo nhánh
- [x] **S00.04** Dựng môi trường
- [x] **S00.05** Tái hiện lỗi script cũ
- [x] **S00.06** Lập bảng ánh xạ cột

## S01 — Khung nạp seed cho CSDL bất kỳ

- [x] **S01.01** Tạo thư mục `server-spring/src/main/resources/db/seed/` + tệp `R__seed_demo_10_categories.sql` rỗng có chú thích đầu file (mục đích, nguyên tắc N1–N3, cách chạy).
- [x] **S01.02** Công tắc seed (D1)
- [x] **S01.03** `scripts/seed/seed.sh`
- [x] **S01.04** `scripts/seed/seed.ps1`
- [x] **S01.05** `scripts/seed-local.sh`
- [x] **S01.06** `scripts/seed/README.md`
- [x] **S01.07** Thử khung
- [x] **S01.C1** 🔖 COMMIT `feat(db): add seed runner that works against any postgres database` · hash: `3e99dc3`

## S02 — Danh mục, thương hiệu, thuộc tính

- [x] **S02.01** `R__seed_demo_10_categories.sql`
- [x] **S02.02** 13 danh mục con từ `SEED_SUB_CATEGORIES`
- [x] **S02.03** Tỷ lệ hoa hồng theo ngành
- [x] **S02.04** `R__seed_demo_20_brands_attributes.sql`
- [x] **S02.05** Thuộc tính ngành
- [x] **S02.06** Kiểm
- [x] **S02.C1** 🔖 COMMIT `feat(db): seed categories, brands and category attributes` · hash: `cfee56a`

## S03 — Shop đã KYC

- [x] **S03.01** `R__seed_demo_30_shops.sql`
- [x] **S03.02** Chủ shop TechZone (D5)
- [x] **S03.03** `vendor_profiles`
- [x] **S03.04** Kho lấy hàng
- [x] **S03.05** Tài khoản nhận tiền
- [x] **S03.06** `vendor_wallets`
- [x] **S03.07** `vendor_shipping_config`
- [x] **S03.08** Shop đang nghỉ
- [x] **S03.09** Kiểm
- [x] **S03.C1** 🔖 COMMIT `feat(db): seed verified shops with wallets and shipping config` · hash: `c949acf`

## S04 — Sản phẩm, SKU, ảnh

- [x] **S04.01** `scripts/seed/upload-assets.sh` + `.ps1` (D6)
- [x] **S04.02** `R__seed_demo_40_products.sql`
- [x] **S04.03** `product_variants`
- [x] **S04.04** `product_images`
- [x] **S04.05** Cột tìm kiếm
- [x] **S04.06** Đồng bộ bộ đếm shop
- [x] **S04.07** Kiểm
- [x] **S04.C1** 🔖 COMMIT `feat(db): seed published products with variants and images` · hash: `6cff29c`
- [x] **S04.C2** 🔖 COMMIT `feat(db): add script to upload seed images to object storage` · hash: `81cc245`

## S05 — Voucher & banner

- [x] **S05.01** `R__seed_demo_50_vouchers.sql`
- [x] **S05.02** Voucher đổi Điểm
- [x] **S05.03** `R__seed_demo_60_banners.sql`
- [x] **S05.04** Trạng thái banner đa dạng
- [x] **S05.05** Kiểm
- [x] **S05.C1** 🔖 COMMIT `feat(db): seed vouchers, coin reward vouchers and home banners` · hash: `6aade00`

## S06 — Hoạt động của khách mẫu & hàng chờ KYC

- [x] **S06.01** `R__seed_demo_70_customer_activity.sql`
- [x] **S06.02** `coin_transactions`
- [x] **S06.03** `daily_checkins`
- [x] **S06.04** `user_wishlists`
- [x] **S06.05** `R__seed_demo_80_kyc_queue.sql`
- [x] **S06.06** `kyc_results` cho 3 hồ sơ chờ duyệt
- [x] **S06.07** Nếu plan Mở shop & KYC đã thêm cột `V15` (`kyc_supplement_deadline_at`…) thì điền trong file này bằng khối `DO $$ … IF EXISTS (cột) …` để file chạy được cả trước và sau `V15`.
- [x] **S06.08** Kiểm
- [x] **S06.C1** 🔖 COMMIT `feat(db): seed sample customer activity and kyc review queue` · hash: `98ae7d2`

## S07 — Kiểm thử tự động & tài liệu

- [x] **S07.01** `SeedDataIT` (D7)
- [x] **S07.02** Kiểm idempotent trong `SeedDataIT`
- [x] **S07.03** Kiểm enum / `CHECK`
- [x] **S07.04** Tài liệu trong repo
- [x] **S07.05** Tài liệu nguồn
- [-] **S07.06** Thử trên máy "sạch"
- [x] **S07.C1** 🔖 COMMIT `test(db): verify demo seed is complete and idempotent` · hash: `d2531e5`
- [x] **S07.C2** 🔖 COMMIT `docs(db): document demo seed files and seed scripts` · hash: `(commit tài liệu cuối nhánh)`
- [-] **S07.07** Mở PR vào `develop`

## S08 — (Tùy chọn — D4) Mở rộng lên ~200 sản phẩm

- [x] **S08.01** `R__seed_demo_90_bulk_products.sql`
- [x] **S08.02** Kiểm hiệu năng tìm kiếm trên ~200 sản phẩm (`EXPLAIN ANALYZE` truy vấn full-text dùng index GIN)
- [x] **S08.03** Bổ sung khẳng định trong `SeedDataIT` (tổng ≥ 200 khi có file 90).
- [x] **S08.C1** 🔖 COMMIT `feat(db): seed bulk products for search and paging tests` · hash: `c9fc754`

---

## Chốt plan — tick tiến độ use case

Plan này không gắn use case nào nên **không tick** ở checklist use case; chỉ:

- [x] Tick dòng plan này ở bảng *Tiến độ các plan* trong [README](../README.md) mục 6
- [x] Thêm một dòng vào mục 5 *Nhật ký cập nhật* của [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../../docs/Document/CHECKLIST_TIEN_DO_UC.md) (đã có bộ seed, cách chạy)

## Ghi chú

Ghi ở đây: lý do bỏ qua task (`[-]`), chỗ làm khác plan, số đo, việc tồn chuyển sang plan khác.

| Task | Ghi chú |
|---|---|
| S00.04 | Máy dev có container `dove-postgres` (dự án khác) chiếm cổng 5432 → chạy Postgres Genie bằng `POSTGRES_PORT=5433` (chỉ ghi đè khi chạy lệnh, không sửa `.env`) |
| S00.05 | Tái hiện: placeholder `seed_demo` không được thay khi đổ qua `psql` → so sánh ra `f`, script cũ không chèn dòng nào; sau S01 `seed.sh --docker --only 00` chèn đủ 5 tài khoản vào CSDL mới |
| S00.06 | Khác giả định của plan: `categories.slug`, `brands.slug` **không UNIQUE** → dùng `NOT EXISTS`; `products` **không có** cột `slug` và giá gốc → chống trùng theo (`vendor_id`, `name`), `base_price` = giá bán; `product_variants.stock_count` (không phải `stock_quantity`); `vouchers.valid_from/valid_to`; `kyc_results.risk_signals_json` là TEXT |
| S01.07 | Đã thử `seed.sh` + `seed.ps1` (`--docker`), bộ chặn RDS / host lạ (thoát mã 3). Chưa thử PostgreSQL cài thẳng máy — máy này không có `psql` |
| S02.03 | Hoa hồng theo tài liệu nghiệp vụ 3,5–8% (điện tử 3,5%, thời trang 8%…). Trang chính sách web (`policies.data.ts`) đang ghi 1,5–7% — lệch, cần sửa web hoặc tài liệu ở plan khác |
| S03.04 | Chỉ mã GHN TP.HCM lấy từ API_SPEC; Hà Nội / Bình Dương / Đà Nẵng là mã mẫu (ghi rõ trong file) — đối chiếu master-data GHN khi nối GHN thật |
| S03.06, S06.01 | `balance_hash` = HMAC-SHA256 khóa máy dev `local-wallet-secret`; tính bằng `sha256()` + ipad/opad đệm sẵn (không cần `pgcrypto`). Ví shop: `wallet:<vendor_id>:<balance>:<pending>`, ví Điểm: `coin:<user_id>:<balance>` — plan Điểm Genie phải dùng đúng chuỗi này |
| S04.04 | Mỗi sản phẩm 1 ảnh chính (mock chỉ có 1 ảnh / sản phẩm — không mượn ảnh khác làm ảnh phụ) |
| S05.01 | Không seed voucher "hoàn Điểm" — `ck_vouchers_type` chỉ SHOP / PLATFORM / FREESHIP |
| S05.03 | 19 banner theo `BANNER_SEED`; banner "Tết 2026" (ENDED) mượn ảnh `camp-1212` vì ảnh gốc nằm ở thư mục products |
| S06.05 | Hồ sơ "TravelGear" của mock đổi thành "TravelGear Outdoor" — trùng tên shop đã KYC, V15 thêm UNIQUE `lower(shop_name)` |
| S06.08 | Kiểm bằng SQL; kiểm trên web thật làm khi các plan [4], [5], [6], [7] nối API |
| S07.06 | `[-]` Chưa nhờ được thành viên khác; đã thử tương đương: CSDL mới tinh → backend profile local tự nạp 10 file seed (200 sản phẩm, 11 shop, 19 banner) |
| S07.07 | `[-]` Chưa push / mở PR — chờ người dùng |
| S08.02 | 200 sản phẩm: `EXPLAIN ANALYZE` full-text chọn Seq Scan 0,06 ms (bảng nhỏ — planner chưa dùng GIN, đúng kỳ vọng) |

## Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-16 | Lập plan + progress (58 task, 10 commit, 9 giai đoạn) — chưa bắt đầu làm | — |
| 2026-11-17 | S00–S08: khung nạp seed (`seed.sh` / `seed.ps1`, công tắc `GENIE_SEED_DEMO`), 9 file `db/seed`, `upload-assets`, `SeedDataIT`; `./mvnw verify` BUILD SUCCESS | `3e99dc3` `cfee56a` `c949acf` `6cff29c` `81cc245` `6aade00` `98ae7d2` `d2531e5` `c9fc754` |
