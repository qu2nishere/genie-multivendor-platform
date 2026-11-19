# Dữ liệu mẫu (seed) cho máy dev

> Plan: [`plan/2026-11-16/db/plan_seed_du_lieu_mau.md`](../../plan/2026-11-16/db/plan_seed_du_lieu_mau.md) · ENTITY 13.2 · ADR 0010.
> **Chỉ cho CSDL local** (PostgreSQL Docker hoặc cài thẳng máy) và **Silo** Docker. Production (AWS RDS + S3) **không bao giờ** seed — script từ chối mọi host không phải local.

## 1. Có gì

| File (chạy theo thứ tự tên) | Nội dung |
|---|---|
| `server-spring/src/main/resources/db/migration/R__seed_demo.sql` | 5 tài khoản: khách `mai.tran`, Admin, 3 nhân viên (có sẵn từ plan AUTH) |
| `db/seed/R__seed_demo_10_categories.sql` | 10 danh mục gốc + 13 danh mục con, hoa hồng theo ngành |
| `db/seed/R__seed_demo_20_brands_attributes.sql` | Thương hiệu (cờ xác minh), thuộc tính lọc theo ngành |
| `db/seed/R__seed_demo_30_shops.sql` | 6 shop `VERIFIED` + chủ shop, ví, cấu hình vận chuyển, kho lấy hàng GHN |
| `db/seed/R__seed_demo_40_products.sql` | 20 sản phẩm `PUBLISHED` + SKU + ảnh (khóa đối tượng `seed/product/…`) |
| `db/seed/R__seed_demo_50_vouchers.sql` | Voucher sàn / shop / freeship / hoàn Điểm + 3 voucher đổi Điểm `COIN*` |
| `db/seed/R__seed_demo_60_banners.sql` | Banner trang chủ đủ vị trí và 4 trạng thái hiển thị |
| `db/seed/R__seed_demo_70_customer_activity.sql` | Điểm Genie, điểm danh, yêu thích, theo dõi shop của `mai.tran` |
| `db/seed/R__seed_demo_80_kyc_queue.sql` | 5 hồ sơ KYC cho hàng chờ Admin (chờ duyệt / bổ sung / từ chối) |
| `db/seed/R__seed_demo_90_bulk_products.sql` | ~180 sản phẩm sinh tự động (tìm kiếm, phân trang) |

Thư mục `db/seed` là migration lặp lại của Flyway (`R__…`), SQL thuần (không placeholder, không lệnh meta `psql`),
chạy lại bao nhiêu lần cũng không trùng dòng; khóa chính không ghi cứng, quan hệ tra bằng khóa tự nhiên (email, slug, SKU, mã voucher).

## 2. Ba cách nạp

1. **Tự động khi chạy backend** (mặc định máy dev): profile `local` + `GENIE_SEED_DEMO=true` (mặc định) → Flyway chạy
   `db/migration` rồi `db/seed`. Không muốn dữ liệu mẫu: đặt `GENIE_SEED_DEMO=false` trong `.env`.
2. **PostgreSQL Docker** (`deploy/docker-compose.local.yml`), không cần cài `psql`:
   ```bash
   scripts/seed/seed.sh --docker          # Linux / macOS / Git Bash  (= scripts/seed-local.sh)
   ```
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\seed\seed.ps1 -Docker
   ```
3. **PostgreSQL cài thẳng máy** (khối CSDL [2] trong `.env`, cần `psql` trên máy): bật phần còn lại bằng Docker
   `docker compose --env-file .env -f deploy/docker-compose.local.yml up -d redis silo silo-init mailpit`, rồi
   ```bash
   scripts/seed/seed.sh                   # hoặc seed.ps1 không có -Docker
   ```

Script đọc **cùng biến với backend**: `DATABASE_URL` (`postgresql://user:pass@host:port/db`) nếu có, không thì
`DB_URL` (JDBC, giữ tham số như `sslmode`) + `DB_USER` + `DB_PASSWORD`; thứ tự lấy: biến môi trường → `.env` gốc repo → mặc định
Docker local (`genie` / `changeme`). CSDL phải đã migrate tới `V14` (chạy backend một lần). Tùy chọn: `--only <NN>` (`00` = tài khoản),
`--dry-run`. Mỗi file chạy trong một transaction với `ON_ERROR_STOP`; script in bảng đếm số dòng trước / sau.

**Ảnh mẫu** (sản phẩm, banner) lên Silo — chạy sau khi Silo đã bật:

```bash
scripts/seed/upload-assets.sh            # hoặc: powershell -ExecutionPolicy Bypass -File scripts\seed\upload-assets.ps1
```

## 3. Tài khoản mẫu (mật khẩu chung `Genie@123`, OTP mock `123456`)

| Email | Vai trò | Ghi chú |
|---|---|---|
| `mai.tran@gmail.com` | VENDOR (vừa mua vừa bán) | Chủ shop **TechZone Official**; 1.250 Điểm, chuỗi điểm danh 3 ngày, thích 2 sản phẩm, theo dõi 3 shop |
| `yen.dh@genie.example` | ADMIN | Đủ 13 quyền |
| `phat.nt@genie.example` · `linh.tm@genie.example` · `truc.ht@genie.example` | STAFF | MODERATOR · FINANCE_OFFICER · CS_SPECIALIST |
| `owner.shoe@genie.example` · `owner.boutique@…` · `owner.home@…` · `owner.beauty@…` · `owner.travel@…` | VENDOR | Chủ 5 shop còn lại |
| `kyc.<mã>@genie.example` | VENDOR | 5 hồ sơ ở hàng chờ KYC (`kyc.nhasachmoc`, `kyc.gombattrang`, `kyc.mebehappy`, `kyc.travelgear`, `kyc.cheapelec`) |

## 4. Băm số dư (`balance_hash`)

Ví shop và ví Điểm có cột `balance_hash` = HMAC-SHA256 (hex chữ thường) với khóa `WALLET_HMAC_SECRET`
(mặc định máy dev `local-wallet-secret` trong `application-local.yml`):

- Ví Điểm (`user_coins`): thông điệp `coin:<user_id>:<balance>` (plan Điểm Genie D2).
- Ví shop (`vendor_wallets`): thông điệp `wallet:<vendor_id>:<balance>:<pending_withdrawal>` (số dạng `0.00`).

Seed tính băm bằng `sha256()` sẵn có của PostgreSQL với khóa máy dev. **Đặt `WALLET_HMAC_SECRET` khác** trong `.env` thì
băm seed sẽ lệch — xóa dữ liệu và để backend tạo ví mới, hoặc dùng lại khóa mặc định trên máy dev.

## 5. Xóa dữ liệu mẫu

- Docker: `docker compose --env-file .env -f deploy/docker-compose.local.yml down -v` (xóa cả volume) rồi chạy lại backend.
- Cài máy: `DROP DATABASE genie; CREATE DATABASE genie;` rồi chạy lại backend.

**Không** chạy script trên production — host khác local sẽ bị từ chối; `deploy/.env` luôn đặt `GENIE_SEED_DEMO=false`.
