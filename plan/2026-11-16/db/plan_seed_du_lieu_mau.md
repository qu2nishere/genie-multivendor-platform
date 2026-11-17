# PLAN — Seed dữ liệu mẫu chạy được trên mọi CSDL (danh mục, shop, sản phẩm, voucher, banner…)

| | |
|---|---|
| **Ngày lập** | 2026-11-16 |
| **Thể loại** | db (dữ liệu mẫu PostgreSQL + script nạp) |
| **Mục tiêu** | Bất kỳ thành viên nào, cắm **bất kỳ PostgreSQL nào** (Docker local, Postgres cài máy, CSDL của bạn khác, VPS demo) đều chạy **một lệnh** là có đủ dữ liệu để test: tài khoản, danh mục, thương hiệu, shop đã KYC, sản phẩm + SKU + ảnh, voucher, banner, hàng chờ KYC |
| **Phục vụ plan** | [Sản phẩm yêu thích](../server-spring/plan_san_pham_yeu_thich.md) · [Theo dõi gian hàng](../server-spring/plan_theo_doi_gian_hang.md) · [Banner trang chủ](../server-spring/plan_banner_trang_chu.md) · [Điểm danh & Điểm Genie](../server-spring/plan_diem_danh_diem_genie.md) (phần Đổi quà) · [Mở shop & KYC](../server-spring/plan_mo_shop_kyc.md) |
| **Tài liệu chuẩn** | TS 2.6 (`scripts/`), 7.3 (migration), 16.5 (khởi tạo dữ liệu) · ENTITY 13.2 · LUU_TRU 5.5 (đặt tên khóa đối tượng) · implement 3.1 (bảng nội dung seed) — xem [README](../README.md) |
| **Theo dõi tiến độ** | [progress_seed_du_lieu_mau.md](./progress_seed_du_lieu_mau.md) — cùng mã task `Sxx.yy` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |

---

## 0. Hiện trạng đã khảo sát

| Phần | Có gì | Thiếu / lỗi |
|---|---|---|
| `db/migration/R__seed_demo.sql` | 5 tài khoản (`mai.tran@gmail.com` CUSTOMER, `yen.dh@genie.example` ADMIN, 3 STAFF) + hồ sơ + 2 địa chỉ + `admin_profiles` + `staff_permissions`; mọi câu lệnh có `WHERE '${seed_demo}' = 'true'` (placeholder Flyway) | Chưa có danh mục, thương hiệu, shop, sản phẩm, voucher, banner — các plan khác không có dữ liệu để test |
| `scripts/seed-local.sh` | Đổ thẳng `R__seed_demo.sql` vào `psql` của container `postgres` | **Lỗi:** `psql` không thay placeholder Flyway nên `'${seed_demo}' = 'true'` luôn sai → script chạy xong **không chèn dòng nào**; chỉ chạy được với Docker Compose local, không trỏ được CSDL khác; không có bản cho Windows (PowerShell) |
| Dữ liệu mock của web | `web/src/shared/api/mock/seed.ts`: 6 shop (`SEED_SHOPS`), 10 danh mục gốc + 13 danh mục con, 20 sản phẩm (`SEED_PRODUCTS`), bộ lọc thuộc tính, thương hiệu xác minh; `mock/banners.ts` (`BANNER_SEED`); `mock/admin.ts` (hàng chờ KYC `adminKycStore`); ảnh ở `web/public/img/{products,banners}` (22 + 15 ảnh) | Chỉ tồn tại ở trình duyệt; khi nối API thật giao diện sẽ trống nếu CSDL không có dữ liệu tương ứng |
| Schema | `V1…V14` đủ 65 bảng; khóa chính `GENERATED ALWAYS AS IDENTITY` (không chèn `id` tùy ý) | Seed phải tham chiếu bằng **khóa tự nhiên** (email, slug, mã SKU, mã voucher), không ghi cứng `id` |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | **Chạy lại được nhiều lần** (idempotent): mọi `INSERT` dùng `ON CONFLICT … DO NOTHING / DO UPDATE` hoặc `WHERE NOT EXISTS`; chạy lần 2 không tạo dòng trùng, không báo lỗi | ENTITY 13.2 |
| N2 | **Không ghi cứng `id`**: khóa chính là identity; quan hệ tra bằng khóa tự nhiên (`users.email`, `categories.slug`, `brands.name`, `product_variants.sku`, `vouchers.code`) qua `SELECT … FROM … WHERE` | V1…V12 |
| N3 | **SQL thuần, không phụ thuộc công cụ**: file seed mới **không** chứa placeholder Flyway (`${…}`) hay lệnh meta của `psql` — cùng một file chạy được bằng Flyway (khi backend khởi động ở profile `local`) **và** bằng `psql` trỏ vào CSDL bất kỳ | mục tiêu plan |
| N4 | Mỗi nhóm dữ liệu một file, đánh số theo thứ tự phụ thuộc; file sau chỉ dùng dữ liệu của file trước | TS 7.3 |
| N5 | Dữ liệu **khớp mock của web** (tên shop, tên / giá sản phẩm, slug, banner) để giao diện trước và sau khi nối API thật nhìn giống nhau; tài khoản dùng chung mật khẩu `Genie@123` (băm Argon2id có sẵn trong `R__seed_demo.sql`) | CLAUDE.md mục 3 |
| N6 | Đúng quy ước dữ liệu: tiền `NUMERIC(19,2)`; thời gian `TIMESTAMPTZ` (`now()`); enum là chuỗi đúng `CHECK`; từ ngữ thương hiệu "Genie", "Điểm", "Chính hãng" | TS 7.1, 11 |
| N7 | Ảnh **không lưu URL đầy đủ**: cột ảnh lưu khóa đối tượng (`seed/product/<tên>.jpg`…) — URL do backend ghép theo kho tệp đang dùng; ảnh mẫu chỉ được đẩy lên **Silo** của máy dev bằng script riêng (S3 production không có ảnh mẫu — N10) | LUU_TRU 5.5, 5.6 |
| N8 | Seed tự động **chỉ** chạy khi `GENIE_SEED_DEMO=true` (mặc định `true` ở profile `local`, `false` ở `prod`; profile `test` luôn tắt) và khi gọi script thủ công — **không** chạy ở `test` (test tự dựng dữ liệu bằng `TestDataBuilder`) và `prod`. Một công tắc cho cả `R__seed_demo.sql` lẫn thư mục `db/seed` — trỏ CSDL cloud dùng chung chỉ cần `GENIE_SEED_DEMO=false` trong `.env` ([docs/cau-hinh-moi-truong.md](../../../docs/cau-hinh-moi-truong.md) mục 5) | TS 16.1 |
| N9 | Không sửa `V1…V14`; không sửa nội dung `R__seed_demo.sql` đã merge (giữ nguyên để không đổi checksum trên máy mọi người) — dữ liệu mới nằm ở file mới | TS 7.3 |
| N10 | **Môi trường được seed:** chỉ máy dev / CSDL thử nghiệm — PostgreSQL chạy **Docker** hoặc **cài thẳng máy**, kho tệp **Silo** chạy Docker (`deploy/docker-compose.local.yml`). **Production = AWS RDS + AWS S3 — không bao giờ seed**: script từ chối khi host CSDL là `*.rds.amazonaws.com` hoặc kho tệp là `*.amazonaws.com`, kể cả có cờ ép | [ADR 0010](../../../docs/adr/0010-production-aws-rds-s3.md) |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Đề xuất | Chốt |
|---|---|---|---|
| D1 | Đặt file seed mới ở đâu để vừa Flyway chạy được vừa `psql` chạy được | Thư mục mới `server-spring/src/main/resources/db/seed/`, file dạng migration lặp lại `R__seed_demo_<NN>_<ten>.sql`. **Không** ghi cứng `spring.flyway.locations` theo profile: thêm `common/config/SeedDataFlywayCustomizer` (`FlywayConfigurationCustomizer`) nối `classpath:db/seed` khi `genie.seed.demo-data=true`; giá trị đọc `${GENIE_SEED_DEMO}` (mặc định `false` ở `application.yml`, `true` ở `application-local.yml`, `application-test.yml` đặt cứng `false`) — đúng quy ước mọi giá trị theo môi trường đọc từ biến ([docs/cau-hinh-moi-truong.md](../../../docs/cau-hinh-moi-truong.md)) | ☐ |
| D2 | Thứ tự chạy giữa `R__seed_demo` (tài khoản) và file mới | Flyway chạy migration lặp lại theo thứ tự chữ cái của phần mô tả: `seed demo` < `seed demo 10 categories` < `seed demo 20 …` → đặt tên đúng mẫu `R__seed_demo_<NN>_…` là tài khoản luôn chạy trước | ☐ |
| D3 | Script nạp cho CSDL bất kỳ xử lý placeholder của `R__seed_demo.sql` thế nào | Script thay `${seed_demo}` → `true` bằng `sed` (bash) / `-replace` (PowerShell) khi đổ file đó vào `psql`; các file `db/seed/*` đổ nguyên văn | ☐ |
| D4 | Số lượng dữ liệu: mock web có 6 shop / 23 danh mục / 20 sản phẩm; implement 3.1 ghi 5 shop / 30 danh mục / ~200 sản phẩm | **Hai mức:** mức *cơ bản* = đúng dữ liệu mock (đủ cho mọi plan ngày 16-11, giao diện khớp); mức *đầy* = thêm ~180 sản phẩm sinh bằng `generate_series` trong file riêng `R__seed_demo_90_bulk_products.sql` (làm sau, cắt được) | ☐ |
| D5 | Chủ shop TechZone | `mai.tran@gmail.com` đổi `role` thành `VENDOR` và sở hữu shop TechZone Official (khớp mock: "vừa là người mua vừa chủ shop TechZone") — làm trong file `30_shops` bằng `UPDATE … WHERE email = …` | ☐ |
| D6 | Ảnh sản phẩm / banner / logo | Đẩy ảnh từ `web/public/img/{products,banners}` lên Silo bằng `scripts/seed/upload-assets.{sh,ps1}` (client `mcli` / `mc` trong container `silo-init`) vào `product-media/seed/product/…`, `content-assets/seed/banner/…`; CSDL lưu khóa đối tượng | ☐ |
| D7 | Kiểm seed không bị hỏng khi schema đổi | Thêm `SeedDataIT` (Testcontainers PG): chạy Flyway với cả `db/migration` + `db/seed`, khẳng định số dòng tối thiểu và chạy lần 2 không đổi số dòng — nằm trong `./mvnw verify` | ☐ |

---

## 3. Cây file sẽ tạo / sửa

```
server-spring/src/main/resources/
├── application.yml · application-local.yml · application-test.yml   (sửa) genie.seed.demo-data: ${GENIE_SEED_DEMO:…} (D1)
│   (java) common/config/SeedDataFlywayCustomizer.java     (mới) thêm classpath:db/seed khi genie.seed.demo-data=true
└── db/seed/                                    (mới — SQL thuần, idempotent, không placeholder)
    ├── R__seed_demo_10_categories.sql          danh mục gốc + con, tỷ lệ hoa hồng theo ngành
    ├── R__seed_demo_20_brands_attributes.sql   thương hiệu (có cờ xác minh), thuộc tính lọc theo ngành
    ├── R__seed_demo_30_shops.sql               tài khoản chủ shop + vendor_profiles VERIFIED + ví + cấu hình vận chuyển
    ├── R__seed_demo_40_products.sql            sản phẩm PUBLISHED + biến thể SKU + ảnh
    ├── R__seed_demo_50_vouchers.sql            voucher sàn / shop / freeship + voucher đổi Điểm
    ├── R__seed_demo_60_banners.sql             banner trang chủ theo vị trí
    ├── R__seed_demo_70_customer_activity.sql   số dư Điểm, yêu thích, theo dõi shop của tài khoản mẫu
    ├── R__seed_demo_80_kyc_queue.sql           shop ở các trạng thái KYC cho hàng chờ Admin
    └── R__seed_demo_90_bulk_products.sql       (D4 — tùy chọn) ~180 sản phẩm sinh tự động
server-spring/src/test/java/com/genie/support/SeedDataIT.java      (mới — D7)
scripts/seed/
├── seed.sh                                     (mới) nạp seed vào CSDL bất kỳ (Linux / macOS / Git Bash)
├── seed.ps1                                    (mới) bản PowerShell cho Windows
├── upload-assets.sh · upload-assets.ps1        (mới) đẩy ảnh mẫu lên Silo
└── README.md                                   (mới) cách dùng, biến môi trường, tài khoản mẫu
scripts/seed-local.sh                           (sửa) gọi scripts/seed/seed.sh với tham số của Docker local
README.md · CLAUDE.md                           (sửa) mục chạy local, bẫy đã biết
docs/Document/…/ENTITY.md 13.2, TECH_STACK 2.6 + 16.5, implement 3.1   (sửa) mô tả seed mới
```

---

## 4. Bảng dữ liệu seed → bảng CSDL → nguồn

| File | Bảng ghi | Nội dung (mức cơ bản) | Nguồn dữ liệu |
|---|---|---|---|
| `R__seed_demo.sql` (đã có) | `users`, `customer_profiles`, `addresses`, `admin_profiles`, `staff_permissions` | 1 khách, 1 Admin, 3 nhân viên | plan AUTH A02.04 |
| `10_categories` | `categories` | 10 danh mục gốc + 13 danh mục con, `slug`, `icon`, `display_order`, `commission_rate` 3,5–8% theo ngành (mặc định 5%) | `SEED_ROOT_CATEGORIES`, `SEED_SUB_CATEGORIES`; CLAUDE.md mục 7 |
| `20_brands_attributes` | `brands`, `category_attributes` | Thương hiệu của 20 sản phẩm (cờ xác minh theo `SEED_VERIFIED_BRANDS`); thuộc tính lọc theo ngành | `SEED_PRODUCTS[].brand`, `SEED_ATTR_FILTERS` |
| `30_shops` | `users`, `customer_profiles`, `vendor_profiles`, `vendor_wallets`, `vendor_shipping_config` | 6 shop `VERIFIED` (TechZone Official thuộc `mai.tran@gmail.com`; 5 shop còn lại có tài khoản chủ shop `owner.<code>@genie.example`), `is_mall`, `rating_avg`, `follower_count`, kho lấy hàng + `ghn_shop_id` mẫu, ví số dư 0 có `balance_hash` hợp lệ, GHN bật | `SEED_SHOPS` |
| `40_products` | `products`, `product_variants`, `product_images` | 20 sản phẩm `PUBLISHED` (tên, slug, giá, giá gốc, ngành, thương hiệu, shop, đã bán, đánh giá), mỗi sản phẩm ≥ 1 SKU có tồn, ảnh chính | `SEED_PRODUCTS` |
| `50_vouchers` | `vouchers` | 6 voucher: 2 sàn, 2 shop (TechZone), 1 freeship, 1 hoàn Điểm; **3 voucher đổi Điểm** (mã `COIN500`, `COIN1000`, `COIN2000`) cho plan Điểm Genie | implement 3.1; API 3.13 |
| `60_banners` | `home_banners` | Banner đang hiển thị ở `HOME_TOP_CAROUSEL` (3), `HOME_TOP_SIDE` (2), `HOME_MIDDLE` (2), `FLASH_SALE_TOP` (1), `CATEGORY_TOP` (5), 1 banner đã hết hạn, 1 banner tắt | `BANNER_SEED`, ảnh `web/public/img/banners` |
| `70_customer_activity` | `user_coins`, `coin_transactions`, `daily_checkins`, `user_wishlists`, `shop_follows` | `mai.tran`: 1.250 Điểm (có lô sắp hết hạn 150 Điểm), chuỗi điểm danh 3 ngày, thích 2 sản phẩm, theo dõi 3 shop (cập nhật `follower_count`) | API 3.10, 3.11, 3.13 (ví dụ) |
| `80_kyc_queue` | `users`, `vendor_profiles`, `kyc_results` | 5 shop cho hàng chờ Admin: 3 `MANUAL_REVIEW` (có `kyc_results` mức rủi ro khác nhau, 1 hậu kiểm ngẫu nhiên), 1 `SUPPLEMENT_REQUESTED`, 1 `REJECTED` | `adminKycStore` |

---

## 5. Nhánh, commit, PR

- Nhánh: `feature/db-seed-demo` tách từ `develop` (có mã Jira thì `feature/GNE-<số>-db-seed-demo`).
- Scope commit: `db` (file SQL, script nạp), `deploy` (không dùng), `docs` (tài liệu). Thông điệp ghi sẵn ở từng task `COMMIT`.
- PR có thể tách 3: (1) khung + danh mục + thương hiệu, (2) shop + sản phẩm + ảnh, (3) phần còn lại — file SQL sinh dữ liệu không tính vào ngưỡng ~400 dòng nhưng nên tách để dễ duyệt.
- Quy ước chung: [README mục 5](../README.md).

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. **CSDL trống:** tạo database mới → chạy migration (`./mvnw -pl server-spring flyway:migrate` hoặc khởi động backend profile `local`) → `scripts/seed/seed.sh` chạy hết không lỗi (`ON_ERROR_STOP=1`).
2. **Chạy lại:** chạy `seed.sh` lần 2 → không lỗi, số dòng các bảng không đổi (script in bảng đếm trước / sau).
3. **Flyway:** khởi động backend profile `local` trên CSDL trống → Flyway áp dụng `db/seed` không lỗi; `SchemaValidationIT` vẫn xanh.
4. `cd server-spring && ./mvnw verify` — gồm `SeedDataIT` (từ S09).
5. Kiểm nhanh bằng mắt: `SELECT` vài bảng (ví dụ ở từng task) khớp dữ liệu mock.
6. Tick task ở progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Sxx.yy` = task; `Sxx.Cn` = task commit (làm sau khi qua mục 6).

### S00 — Chuẩn bị

- **[S00.01]** Kiểm danh tính git — `git config user.name`, `git config user.email` đúng tài khoản của mình; không đổi config.
- **[S00.02]** Chốt quyết định D1–D7 — ghi lựa chọn vào đầu file progress.
- **[S00.03]** Tạo nhánh — `git switch develop && git pull && git switch -c feature/db-seed-demo`.
- **[S00.04]** Dựng môi trường — `cp .env.example .env` nếu chưa có (khối CSDL [1] Docker — [docs/cau-hinh-moi-truong.md](../../../docs/cau-hinh-moi-truong.md)); `docker compose --env-file .env -f deploy/docker-compose.local.yml up -d postgres silo silo-init`; xác nhận `psql` vào được (`docker compose … exec postgres psql -U genie -d genie -c '\\dt'` ra 65 bảng sau khi backend chạy một lần).
- **[S00.05]** Tái hiện lỗi script cũ — chạy `scripts/seed-local.sh` trên CSDL trống, xác nhận `SELECT count(*) FROM users` = 0 (placeholder không được thay); ghi vào Ghi chú của progress.
- **[S00.06]** Lập bảng ánh xạ cột — với mỗi bảng ở mục 4, mở migration tương ứng (`V1`, `V2`, `V4`, `V6`, `V7`, `V10`…) ghi lại cột `NOT NULL` không có mặc định và các `CHECK` enum để seed không vi phạm (lưu vào Ghi chú progress).

### S01 — Khung nạp seed cho CSDL bất kỳ

- **[S01.01]** Tạo thư mục `server-spring/src/main/resources/db/seed/` + tệp `R__seed_demo_10_categories.sql` rỗng có chú thích đầu file (mục đích, nguyên tắc N1–N3, cách chạy).
- **[S01.02]** Công tắc seed (D1) — `genie.seed.demo-data: ${GENIE_SEED_DEMO:false}` ở `application.yml`, `${GENIE_SEED_DEMO:true}` ở `application-local.yml`, `false` cứng ở `application-test.yml`; placeholder `seed_demo` đọc cùng giá trị; `SeedDataFlywayCustomizer` nối `classpath:db/seed` khi `true` + unit test (bật → có `db/seed`, tắt → không). Thử: `GENIE_SEED_DEMO=false` trong `.env` → khởi động backend trên CSDL trống không có dòng seed nào.
- **[S01.03]** `scripts/seed/seed.sh` — **dùng chung biến CSDL với backend** để ai cắm CSDL nào thì seed vào đúng CSDL đó: đọc `DB_URL` (dạng JDBC — script tự đổi sang chuỗi kết nối `psql`, giữ tham số như `sslmode=require`), `DB_USER`, `DB_PASSWORD`; thứ tự lấy: biến môi trường → tệp `.env` gốc repo (khối CSDL đang bật) → mặc định Docker local (`genie` / `changeme`); `DATABASE_URL` (`postgresql://user:pass@host:port/db`) nếu đặt thì ưu tiên nhất; in ra host / tên CSDL (che mật khẩu) trước khi chạy; **bộ chặn môi trường (N10):** host là `localhost`, `127.0.0.1` hoặc tên dịch vụ Docker → chạy luôn; host khác (CSDL cloud thử nghiệm của nhóm — khối [3] trong `.env`) → bắt buộc cờ `--allow-remote` và gõ lại tên CSDL để xác nhận; host `*.rds.amazonaws.com` (production) → **luôn dừng** với thông báo, không có cờ nào vượt qua; tùy chọn `--docker` (dùng `docker compose exec -T postgres psql` khi máy không cài `psql`), `--only <NN>` (chạy một file), `--dry-run` (in danh sách file). Trình tự: kiểm kết nối → kiểm bảng `flyway_schema_history` có `V14` (chưa migrate thì dừng kèm hướng dẫn) → đổ `R__seed_demo.sql` sau khi thay `${seed_demo}` → `true` (D3) → đổ lần lượt `db/seed/R__seed_demo_*.sql` theo thứ tự tên → in bảng đếm số dòng các bảng chính. Luôn dùng `-v ON_ERROR_STOP=1`, mỗi file trong một transaction (`--single-transaction`).
- **[S01.04]** `scripts/seed/seed.ps1` — cùng hành vi cho Windows PowerShell 5.1 (không dùng `&&`, đọc file UTF-8 không BOM, thay placeholder bằng `-replace`, gọi `psql.exe` hoặc `docker compose exec -T`).
- **[S01.05]** `scripts/seed-local.sh` — rút gọn thành gọi `scripts/seed/seed.sh --docker` (giữ tên cũ để không gãy tài liệu); sửa chú thích đầu file cho đúng nội dung seed thật.
- **[S01.06]** `scripts/seed/README.md` — 4 cách chạy (Docker local, Postgres cài máy — khi đó vẫn bật Silo / Redis bằng `docker compose --env-file .env -f deploy/docker-compose.local.yml up -d redis silo silo-init mailpit`, CSDL của người khác / cloud — chỉ cần bật đúng khối CSDL trong `.env` như backend, tự động khi backend chạy với `GENIE_SEED_DEMO=true`); cập nhật [docs/cau-hinh-moi-truong.md](../../../docs/cau-hinh-moi-truong.md) (`GENIE_SEED_DEMO` điều khiển cả `db/seed`, cách seed CSDL cloud); danh sách file + thứ tự, bảng tài khoản mẫu (email / vai trò / mật khẩu `Genie@123`), cách xóa dữ liệu seed (xóa volume hoặc `DROP DATABASE`), lưu ý không chạy trên `prod`.
- **[S01.07]** Thử khung — chạy `seed.sh` và `seed.ps1` trên CSDL trống: 5 tài khoản của `R__seed_demo.sql` xuất hiện (lỗi S00.05 đã hết); chạy lần 2 không đổi. Thử thêm trên **cả PostgreSQL Docker lẫn PostgreSQL cài máy** (khối [1], [2] của `.env`); thử bộ chặn: `DB_URL=jdbc:postgresql://x.abc.ap-southeast-1.rds.amazonaws.com:5432/genie` → dừng kể cả có `--allow-remote`; host lạ không có cờ → dừng.
- **[S01.C1]** COMMIT — `feat(db): add seed runner that works against any postgres database`

### S02 — Danh mục, thương hiệu, thuộc tính

- **[S02.01]** `R__seed_demo_10_categories.sql` — 10 danh mục gốc từ `SEED_ROOT_CATEGORIES` (`name`, `slug`, `icon`, `display_order`, `is_active = true`), `ON CONFLICT (slug) DO UPDATE` các cột hiển thị (để sửa tên / thứ tự chạy lại được).
- **[S02.02]** 13 danh mục con từ `SEED_SUB_CATEGORIES` — `parent_id` tra theo `slug` cha; kiểm không tạo vòng.
- **[S02.03]** Tỷ lệ hoa hồng theo ngành — `commission_rate` từng danh mục gốc trong khoảng 3,5–8% (điện tử 3,5%, thời trang 8%…), danh mục con để `NULL` (kế thừa); đối chiếu bảng hoa hồng ở trang chính sách của web (`policies.data.ts`).
- **[S02.04]** `R__seed_demo_20_brands_attributes.sql` — thương hiệu: gom `brand` khác nhau của `SEED_PRODUCTS` + `SEED_VERIFIED_BRANDS` (cờ xác minh / chính hãng theo cột thật của bảng `brands`).
- **[S02.05]** Thuộc tính ngành — `category_attributes` từ `SEED_ATTR_FILTERS` (`code`, `name`, `data_type`, `options_json`, `is_filterable`, `is_required`, `display_order`), gắn đúng danh mục theo `slug`.
- **[S02.06]** Kiểm — `SELECT count(*) FROM categories` = 23, cây cha–con đúng (`WITH RECURSIVE`), không trùng slug; chạy lại không đổi.
- **[S02.C1]** COMMIT — `feat(db): seed categories, brands and category attributes`

### S03 — Shop đã KYC

- **[S03.01]** `R__seed_demo_30_shops.sql` — tài khoản chủ shop: 5 `users` mới `owner.<code>@genie.example` (role `VENDOR`, `ACTIVE`, đã xác minh email, băm mật khẩu `Genie@123` dùng lại chuỗi Argon2id của `R__seed_demo.sql`) + `customer_profiles` tương ứng.
- **[S03.02]** Chủ shop TechZone (D5) — `UPDATE users SET role = 'VENDOR' WHERE email = 'mai.tran@gmail.com' AND role = 'CUSTOMER'`.
- **[S03.03]** `vendor_profiles` — 6 dòng từ `SEED_SHOPS`: `shop_name`, `category_id` (tra slug), `business_type`, `shop_description`, `kyc_status = 'VERIFIED'`, `kyc_verification_method = 'MANUAL_STAFF'`, `kyc_submitted_at` / `kyc_verified_at`, `is_mall` (theo `is_authentic`), `rating_avg`, `rating_count`, `follower_count`, `chat_response_rate`, `avg_response_minutes`, `created_at` theo `joined_year`; `ON CONFLICT (user_id) DO NOTHING`.
- **[S03.04]** Kho lấy hàng — điền `pickup_province_id / pickup_province / pickup_district_id / pickup_district / pickup_ward_code / pickup_ward / pickup_address_line` bằng mã GHN thật (TP.HCM 202 · Quận 1 1442 · P. Bến Nghé 20308 như `R__seed_demo.sql`; shop Hà Nội dùng mã Hà Nội) và `ghn_shop_id` mẫu → `isPickupConfigured() = true`.
- **[S03.05]** Tài khoản nhận tiền — TechZone có `payout_bank_name`, `payout_bank_account_number`, `payout_bank_account_name` (không dấu, viết hoa, khớp tên người đại diện), `payout_account_verified_at`; shop khác để trống (test luồng chưa có tài khoản).
- **[S03.06]** `vendor_wallets` — mỗi shop một ví số dư 0; `balance_hash` tính đúng công thức HMAC của entity `VendorWallet` với khóa local mặc định (ghi công thức + khóa dùng trong chú thích; nếu khóa local khác thì hướng dẫn chạy lại lệnh cập nhật băm trong README).
- **[S03.07]** `vendor_shipping_config` — mỗi shop một dòng: GHN bật, hãng khác theo mock, `freeship_threshold` theo `free_ship_from`.
- **[S03.08]** Shop đang nghỉ — một shop có `vacation_starts_at / vacation_ends_at / vacation_message` (theo trường `vacation` của mock, nếu mock không có thì chọn shop thứ 6) để test "sản phẩm của shop đang nghỉ không mua được".
- **[S03.09]** Kiểm — `SELECT shop_name, kyc_status, is_mall FROM vendor_profiles` ra 6 dòng `VERIFIED`; `mai.tran` có role `VENDOR`; chạy lại không đổi.
- **[S03.C1]** COMMIT — `feat(db): seed verified shops with wallets and shipping config`

### S04 — Sản phẩm, SKU, ảnh

- **[S04.01]** `scripts/seed/upload-assets.sh` + `.ps1` (D6) — đẩy `web/public/img/products/*` → `product-media/seed/product/`, `web/public/img/banners/*` → `content-assets/seed/banner/` bằng `mcli` / `mc` của container `silo-init` (`docker compose run --rm -v <thư mục ảnh>:/assets --entrypoint /bin/sh silo-init -c "…mirror…"`); Silo không phải Docker local thì đọc **cùng biến với backend** `MINIO_ENDPOINT`, `MINIO_APP_ACCESS_KEY`, `MINIO_APP_SECRET_KEY` (biến môi trường → `.env` gốc → mặc định Docker); chạy lại không tải trùng (`mirror --overwrite`). **Chỉ đẩy lên Silo** — `MINIO_ENDPOINT` chứa `amazonaws.com` (S3 production) → dừng (N10).
- **[S04.02]** `R__seed_demo_40_products.sql` — 20 `products` từ `SEED_PRODUCTS`: `vendor_id` (= `users.id` của chủ shop, tra qua `vendor_profiles.shop_name`), `category_id` (slug), `brand_id` (tên), `name`, `slug`, `description` ngắn, `base_price`, `original_price`, `status = 'PUBLISHED'`, `item_condition = 'NEW'`, `sold_count`, `rating_avg`, `rating_count`, `is_pre_order` + số ngày (theo `preorder_days`), cờ freeship nếu có cột; chống trùng theo `slug`.
- **[S04.03]** `product_variants` — mỗi sản phẩm 1–3 SKU (màu / dung lượng): `sku` duy nhất dạng `SEED-<mã sp>-<mã biến thể>`, `price`, `stock_quantity` (một SKU để tồn 0 để test hết hàng), `attributes_json`; `ON CONFLICT (sku) DO NOTHING`.
- **[S04.04]** `product_images` — ảnh chính + 1–2 ảnh phụ; `image_url` lưu **khóa đối tượng** `seed/product/<file>` (N7), `display_order`, cờ ảnh chính.
- **[S04.05]** Cột tìm kiếm — xác nhận trigger `search_vector` của `V2__catalog.sql` tự điền khi `INSERT` (`SELECT count(*) FROM products WHERE search_vector IS NOT NULL` = 20); nếu trigger chỉ chạy khi `UPDATE` thì thêm `UPDATE products SET name = name` cuối file.
- **[S04.06]** Đồng bộ bộ đếm shop — `UPDATE vendor_profiles SET active_product_count = (SELECT count(*) FROM products WHERE vendor_id = vendor_profiles.user_id AND status = 'PUBLISHED')`.
- **[S04.07]** Kiểm — 20 sản phẩm, mỗi sản phẩm ≥ 1 SKU và ≥ 1 ảnh; ảnh mở được qua `http://localhost:9000/product-media/seed/product/<file>` sau khi chạy `upload-assets`; chạy lại không đổi.
- **[S04.C1]** COMMIT — `feat(db): seed published products with variants and images`
- **[S04.C2]** COMMIT — `feat(db): add script to upload seed images to object storage`

### S05 — Voucher & banner

- **[S05.01]** `R__seed_demo_50_vouchers.sql` — 2 voucher sàn (giảm %, giảm tiền, có trần), 2 voucher shop TechZone, 1 freeship, 1 hoàn Điểm; đủ cột bắt buộc (`code`, `type`, `discount_type`, `discount_value`, `min_order_value`, `max_discount`, `usage_limit`, `starts_at`, `ends_at`, người chịu chi phí…) theo `V4__promotion.sql`; thời hạn tương đối (`now() - interval '1 day'` → `now() + interval '60 days'`) để không hết hạn khi demo.
- **[S05.02]** Voucher đổi Điểm — 3 voucher sàn `COIN500` (giảm 5.000đ, đơn từ 50.000đ), `COIN1000`, `COIN2000` dùng cho `GET /public/coin-rewards` (plan Điểm Genie D3); ghi rõ trong chú thích là "quà đổi Điểm — không hiện ở danh sách voucher công khai" (cờ / phạm vi theo cột thật).
- **[S05.03]** `R__seed_demo_60_banners.sql` — banner theo mục 4: `title`, `image_url` = khóa `seed/banner/<file>` (N7), `target_url` trong miền sàn (`/c/<slug>`, `/flash-sale`, `/campaign/sieu-sale-12-12`…), `position`, `category_id` cho `CATEGORY_TOP`, `sort_order`, `starts_at / ends_at` tương đối, `is_active`, `created_by_admin_id` (id của `yen.dh@genie.example`); chống trùng theo (`title`, `position`).
- **[S05.04]** Trạng thái banner đa dạng — 1 banner đã kết thúc (`ends_at` quá khứ), 1 banner đã lên lịch (`starts_at` tương lai), 1 banner tắt (`is_active = false`) để màn Admin có đủ 4 trạng thái `LIVE / SCHEDULED / ENDED / OFF`.
- **[S05.05]** Kiểm — không vị trí nào quá 8 banner đang hiển thị; mọi `target_url` bắt đầu bằng `/` và không chứa `//`.
- **[S05.C1]** COMMIT — `feat(db): seed vouchers, coin reward vouchers and home banners`

### S06 — Hoạt động của khách mẫu & hàng chờ KYC

- **[S06.01]** `R__seed_demo_70_customer_activity.sql` — `user_coins` của `mai.tran`: `balance = 1250`, `balance_hash` đúng công thức entity `UserCoin` (ghi công thức + khóa local trong chú thích).
- **[S06.02]** `coin_transactions` — các lô cộng / trừ sao cho `Σ amount = 1250`: điểm danh, hoàn Điểm đơn, đánh giá có ảnh, dùng khi thanh toán (âm); một lô 150 Điểm có `expires_at = now() + interval '20 days'` (để hiện "150 Điểm sắp hết hạn trong 30 ngày").
- **[S06.03]** `daily_checkins` — `streak_count = 3`, `last_checkin_at = now() - interval '1 day'` (hôm nay điểm danh được ngày thứ 4).
- **[S06.04]** `user_wishlists` — `mai.tran` thích 2 sản phẩm (tra theo slug); `shop_follows` — theo dõi 3 shop **không phải** TechZone (không tự theo dõi shop mình), cộng `follower_count` tương ứng chỉ khi dòng mới được chèn (dùng CTE `INSERT … RETURNING`).
- **[S06.05]** `R__seed_demo_80_kyc_queue.sql` — 5 tài khoản `VENDOR` + `vendor_profiles` theo `adminKycStore`: Nhà Sách Mộc (HOUSEHOLD), Gốm Bát Tràng Xưa (BUSINESS), Mẹ & Bé Happy (INDIVIDUAL) ở `MANUAL_REVIEW`; TravelGear `SUPPLEMENT_REQUESTED` (kèm ghi chú); Cheap Electronics `REJECTED` (kèm lý do); `kyc_submitted_at` lệch nhau để kiểm sắp xếp.
- **[S06.06]** `kyc_results` cho 3 hồ sơ chờ duyệt — `risk_score` 12 (bị lấy mẫu, `sampled_for_review = true`), 42, 65; cờ kiểm tra + `risk_signals_json` theo bảng tín hiệu ENTITY 1.6 (vd `FACE_SCORE_BORDERLINE` +25, `UNREADABLE_IMAGE` +30); `overall_status = 'PARTIAL'`; trường `extracted_*` mẫu. Cột ảnh giấy tờ để `NULL` (ảnh thật mã hóa do luồng nộp hồ sơ tạo — ghi chú để người test tự nộp một hồ sơ nếu cần xem ảnh).
- **[S06.07]** Nếu plan Mở shop & KYC đã thêm cột `V15` (`kyc_supplement_deadline_at`…) thì điền trong file này bằng khối `DO $$ … IF EXISTS (cột) …` để file chạy được cả trước và sau `V15`.
- **[S06.08]** Kiểm — đăng nhập `mai.tran` trên web mock tắt (`PUBLIC_USE_MOCK=false` cho nhóm đã nối) thấy đúng số Điểm / yêu thích / theo dõi khi các plan tương ứng xong; `SELECT kyc_status, count(*) FROM vendor_profiles GROUP BY 1`.
- **[S06.C1]** COMMIT — `feat(db): seed sample customer activity and kyc review queue`

### S07 — Kiểm thử tự động & tài liệu

- **[S07.01]** `SeedDataIT` (D7) — Testcontainers PG 16, Flyway với `locations = db/migration, db/seed` và placeholder `seed_demo=true`; khẳng định: ≥ 23 danh mục, ≥ 6 shop `VERIFIED`, ≥ 20 sản phẩm `PUBLISHED` đều có SKU + ảnh, mọi `products.vendor_id` có `vendor_profiles`, mọi voucher `COIN*` tồn tại, `Σ coin_transactions.amount = user_coins.balance`.
- **[S07.02]** Kiểm idempotent trong `SeedDataIT` — chạy lại toàn bộ file `db/seed/*` bằng `ScriptUtils` lần 2, đếm lại số dòng các bảng: không đổi.
- **[S07.03]** Kiểm enum / `CHECK` — mọi giá trị enum trong seed nằm trong enum Java tương ứng (test đọc cột `status`, `kyc_status`, `position`, `type`… và `Enum.valueOf`).
- **[S07.04]** Tài liệu trong repo — `README.md` (mục chạy local: thêm bước seed + upload ảnh), `CLAUDE.md` mục 3 (lệnh) và mục 9 (bẫy: seed cũ không chèn được do placeholder — đã sửa), `scripts/seed/README.md` hoàn chỉnh.
- **[S07.05]** Tài liệu nguồn — `docs/Document`: ENTITY 13.2 (mô tả seed nhiều file + script), TS 2.6 (cây `scripts/seed/`), TS 16.5, implement 3.1 (bảng file seed); ghi lịch sử phiên bản.
- **[S07.06]** Thử trên máy "sạch" — nhờ một thành viên khác (hoặc máy / container mới) clone, `docker compose up`, chạy backend, `seed.sh` / `seed.ps1`, `upload-assets` theo đúng README; ghi vướng mắc và sửa README.
- **[S07.C1]** COMMIT — `test(db): verify demo seed is complete and idempotent`
- **[S07.C2]** COMMIT — `docs(db): document demo seed files and seed scripts`
- **[S07.07]** Mở PR vào `develop` — tiêu đề đúng chuẩn commit, điền mẫu `.github/pull_request_template.md` (Ảnh hưởng CSDL: không có migration `V` mới, chỉ dữ liệu mẫu ở profile local).

### S08 — (Tùy chọn — D4) Mở rộng lên ~200 sản phẩm

- **[S08.01]** `R__seed_demo_90_bulk_products.sql` — sinh ~180 sản phẩm bằng `generate_series` trên 6 shop × 23 danh mục: tên theo mẫu `<loại> <thương hiệu> <số>`, slug duy nhất `seed-bulk-<n>`, giá ngẫu nhiên ổn định (`setseed` / biểu thức theo `n` để chạy lại cho cùng kết quả), 1 SKU, ảnh dùng lại 22 ảnh mẫu xoay vòng.
- **[S08.02]** Kiểm hiệu năng tìm kiếm trên ~200 sản phẩm (`EXPLAIN ANALYZE` truy vấn full-text dùng index GIN) — ghi kết quả vào Ghi chú.
- **[S08.03]** Bổ sung khẳng định trong `SeedDataIT` (tổng ≥ 200 khi có file 90).
- **[S08.C1]** COMMIT — `feat(db): seed bulk products for search and paging tests`

---

## 8. Ngoài phạm vi

- Đơn hàng, thanh toán, ký quỹ, vận đơn, đánh giá, trả hàng mẫu — thuộc chuỗi nhóm C, seed cùng plan của chuỗi đó.
- Tin nhắn chat, thông báo — seed cùng cụm B6.
- Dữ liệu cho môi trường `prod`; ẩn danh / xóa dữ liệu thật.
- Sửa `V1…V14` hoặc nội dung `R__seed_demo.sql`.

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Seed vi phạm `NOT NULL` / `CHECK` khi schema đổi (migration `V15+` của plan khác) | `SeedDataIT` chạy trong CI; plan thêm cột `NOT NULL` phải cập nhật seed trong cùng PR |
| Thứ tự chạy migration lặp lại không như mong đợi | Đặt tên đúng mẫu D2; S01.07 và `SeedDataIT` kiểm trên CSDL trống |
| `balance_hash` của ví / Điểm sai khóa → backend báo lệch số dư | Ghi rõ công thức + khóa local; README có lệnh tính lại; `SeedDataIT` gọi `verifyHash()` |
| Máy Windows không có `psql` / `bash` | `seed.ps1` + tùy chọn `--docker` dùng `psql` trong container |
| Ảnh chưa đẩy lên Silo → giao diện vỡ ảnh | `upload-assets` là bước riêng có trong README; backend / web hiển thị ảnh giữ chỗ khi 404 (đã có ở web) |
| Chạy nhầm trên CSDL thật | Bộ chặn N10: host RDS / S3 production luôn bị từ chối; host lạ khác cần `--allow-remote` + gõ lại tên CSDL; production đặt `GENIE_SEED_DEMO=false` nên Flyway không nạp `db/seed`; README cảnh báo |
