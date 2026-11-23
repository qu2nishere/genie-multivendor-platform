# Hướng dẫn chạy Genie trên máy local (Windows + PowerShell)

> Bản cho máy dev Windows: Postgres ở **cổng 5433** (5432 đã bị chiếm), email OTP qua **Mailpit**, AI KYC chạy **thật** (worker trong Docker) hoặc **stub**.
> Cấu hình chi tiết từng biến: [cau-hinh-moi-truong.md](cau-hinh-moi-truong.md) · kịch bản kiểm thử: [kiem-thu/](kiem-thu/README.md) · Redis: [redis-local.md](redis-local.md).
> Cập nhật: 2026-11-23 (sau B1, B2, seed Excel, KYC ảnh thật, migration tới `V24`).

## 0. Tóm tắt nhanh — bảng cổng & đường link

| Dịch vụ | Cổng | Link | Đăng nhập / ghi chú |
|---|---|---|---|
| **Web** (người mua) | 4321 | http://localhost:4321 | |
| Kênh người bán | 4321 | http://localhost:4321/seller | |
| Trang quản trị | 4321 | http://localhost:4321/admin | |
| **Backend** | 8080 | http://localhost:8080/actuator/health | `{"status":"UP"}` |
| Swagger (thử API) | 8080 | http://localhost:8080/swagger-ui.html | |
| **AI Worker** | 8000 | http://localhost:8000/health | `"models_loaded": true` |
| **Postgres** | **5433** | `localhost:5433`, db `genie` | `genie` / `genie` (theo `.env`) — DataGrip / DBeaver |
| Redis | 6379 | `localhost:6379` | mật khẩu `changeme` |
| Silo API (ảnh) | 9000 | http://localhost:9000 | thử: http://localhost:9000/product-media/seed/product/headphones.jpg |
| Silo console | 9001 | http://localhost:9001 | `siloadmin` / `changeme` |
| Mailpit (xem OTP) | 8025 | http://localhost:8025 | |
| Mailpit SMTP | 1025 | — | backend gửi mail vào đây |

**Thứ tự bật mỗi ngày:** Docker → AI Worker → Backend → Web (mỗi thứ một cửa sổ PowerShell, đứng ở thư mục gốc repo `D:\TLCN-KLTN\TLCN\genie-multivendor-platform`).

```powershell
# 1. Docker (Postgres, Redis, Silo, Mailpit)
docker compose --env-file .env -f deploy/docker-compose.local.yml up -d

# 2. AI Worker (để nguyên cửa sổ cho hiện log) — bỏ qua nếu dùng stub
docker run --rm --name genie-ai-worker -p 8000:8000 -e INTERNAL_TOKEN=local-internal-token -e BACKEND_URL=http://host.docker.internal:8080 -e STORAGE_ALLOWED_HOSTS=host.docker.internal:9000 -v "${PWD}\server-python\models:/app/models:ro" genie-ai-worker:local

# 3. Backend
cd server-spring
$env:JAVA_HOME = "C:\Users\MSII\.jdks\ms-21.0.10"
.\mvnw.cmd spring-boot:run

# 4. Web
cd web
npm run dev
```

## 1. Cần cài sẵn

| Công cụ | Phiên bản | Ghi chú |
|---|---|---|
| JDK | **21** (`C:\Users\MSII\.jdks\ms-21.0.10`) | Máy mặc định JDK 24 → Lombok lỗi `TypeTag :: UNKNOWN`, nên phải trỏ `JAVA_HOME` về 21 |
| Docker Desktop | bản mới | Postgres, Redis, Silo, Mailpit, AI Worker |
| Node.js | 22 (kèm npm) | Web Astro |
| Python | 3.12 (không bắt buộc) | Chỉ cần khi **sửa dữ liệu** bằng tool seed Excel (mục 4.3). AI Worker chạy bằng Docker |

**Dòng `$env:JAVA_HOME = ...`** gán JDK 21 **chỉ cho cửa sổ PowerShell hiện tại** (đóng là mất). Muốn cố định: *Start → "Edit the system environment
variables" → Environment Variables* → `JAVA_HOME = C:\Users\MSII\.jdks\ms-21.0.10` và đưa `%JAVA_HOME%\bin` lên đầu `Path`.
Kiểm tra: `& "$env:JAVA_HOME\bin\java" -version` → `21.0.10`. Gõ trong **cmd** (không phải PowerShell) thì dùng `set "JAVA_HOME=C:\Users\MSII\.jdks\ms-21.0.10"` và `mvnw.cmd`.

## 2. File `.env` (làm một lần)

> ⚠ `.env` của backend **không** hiểu chú thích cuối dòng: `KHOA=giá_trị   # ghi chú` → phần ghi chú bị tính vào giá trị (từng làm backend
> lỗi `invalid hostname`). Chú thích luôn đặt ở **dòng riêng** bắt đầu bằng `#`. Không commit `.env` (đã `.gitignore`).

### 2.1. `.env` ở thư mục gốc repo (cấu hình đang dùng)

```dotenv
SPRING_PROFILES_ACTIVE=local

# ===== Postgres ở cổng 5433 (POSTGRES_PORT cho Docker, DB_URL cho backend — phải khớp nhau) =====
POSTGRES_PORT=5433
DB_URL=jdbc:postgresql://localhost:5433/genie
DB_USER=genie
DB_PASSWORD=genie

# ===== Email OTP qua Mailpit (không gửi mail thật ra ngoài) =====
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_USER=
SMTP_PASSWORD=
SMTP_AUTH=false
SMTP_STARTTLS=false
MAIL_FROM=no-reply@genie.local

# ===== Đăng nhập Google / Zalo (giữ giá trị riêng của bạn — không ghi vào tài liệu) =====
GOOGLE_CLIENT_ID=<của bạn>
GOOGLE_CLIENT_SECRET=<của bạn>
ZALO_APP_ID=<của bạn>
ZALO_APP_SECRET=<của bạn>

# ===== AI Worker thật (mục 6). Dùng stub: thêm # đầu dòng AI_WORKER_MODE =====
AI_WORKER_MODE=http
# backend chạy trên Windows nên gọi worker qua localhost là được
AI_WORKER_URL=http://localhost:8000
INTERNAL_TOKEN=local-internal-token
BACKEND_INTERNAL_URL=http://host.docker.internal:8080
# link ảnh ký bằng host này để worker (trong Docker) tải được — trình duyệt cũng phân giải được
PUBLIC_S3_URL=http://host.docker.internal:9000
```

Biến **tùy chọn** hay dùng (mặc định đã hợp máy dev, chỉ thêm khi cần — đủ danh sách ở `.env.example`):

| Biến | Mặc định | Khi nào đổi |
|---|---|---|
| `GENIE_SEED_DEMO` | `true` | `false` = không nạp dữ liệu mẫu |
| `STAFF_EMAIL_DOMAINS` | trống (mọi email) | Giới hạn tên miền email khi mời nhân viên (B1) |
| `REDIS_PASSWORD` | `changeme` | Đổi mật khẩu Redis Docker |
| `MINIO_ENDPOINT`, `MINIO_APP_ACCESS_KEY`, `MINIO_APP_SECRET_KEY` | `http://localhost:9000`, `genie-app`, `changeme` | Đổi cổng / tài khoản Silo |
| `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD` | `siloadmin`, `changeme` | Tài khoản Silo console |
| `SILO_PORT`, `SILO_CONSOLE_PORT`, `MAILPIT_SMTP_PORT`, `MAILPIT_UI_PORT` | 9000, 9001, 1025, 8025 | Cổng bị app khác chiếm |
| `LOG_LEVEL_GENIE` | `DEBUG` | Bớt log |
| `GENIE_SCHEDULING_ENABLED` | `true` | `false` = tắt mọi cron (banner, Điểm hết hạn, KYC…) |

### 2.2. `web/.env`

```dotenv
PUBLIC_WEB_URL=http://localhost:4321
PUBLIC_API_URL=http://localhost:8080/api/v1
INTERNAL_API_URL=http://localhost:8080/api/v1
PUBLIC_S3_URL=http://localhost:9000
PUBLIC_USE_MOCK=true
PUBLIC_REAL_API=auth,upload,profile,security,address,audit,banner,coins,wishlist,follow,vendor,rbac,users,catalog,product,search,shop,admin-catalog,commission
```

**Mock hay dữ liệu thật?** Một chức năng gọi backend khi tên cờ của nó có trong `PUBLIC_REAL_API`; không có thì dùng dữ liệu giả.
Chức năng **chưa có cờ** (giỏ hàng, thanh toán, đơn hàng, chat, đánh giá, ví shop, flash sale, voucher…) luôn chạy mock vì chưa có backend.
`PUBLIC_USE_MOCK=false` = mọi thứ gọi backend (màn chưa có API sẽ lỗi 404). Không cần xóa code mock.

| Cờ | Chức năng | Backend |
|---|---|---|
| `auth` `profile` `security` `address` `upload` | Đăng nhập / đăng ký / OTP, hồ sơ, bảo mật, sổ địa chỉ, tải tệp | ✅ |
| `rbac` `users` | Nhân viên, quyền, người dùng (Admin) | ✅ |
| `audit` `banner` | Nhật ký hệ thống, banner | ✅ |
| `coins` `wishlist` `follow` | Điểm Genie, yêu thích, theo dõi shop | ✅ |
| `vendor` | Mở shop, KYC, duyệt KYC, tài khoản nhận tiền, cài đặt shop | ✅ |
| `catalog` `product` `search` `shop` | Cây danh mục, sản phẩm, tìm kiếm, trang shop | ✅ |
| `admin-catalog` `commission` | Quản lý danh mục, thuộc tính, hoa hồng (Admin) | ✅ |
| `pickup` `vacation` `seller-search` | Kho lấy hàng GHN, chế độ nghỉ, tìm trong Seller Center | ❌ chưa có — **đừng thêm** |
| `catalog-proposals` `catalog-products` `attribute-filters` | Duyệt đề xuất danh mục, kiểm duyệt sản phẩm, bộ lọc thuộc tính | ❌ chưa có — **đừng thêm** |

Đổi `web/.env` xong phải **tắt và chạy lại** `npm run dev`. Kiểm tra một màn đang dùng gì: DevTools → Network, thấy request `localhost:8080/api/v1/...` là dữ liệu thật.

## 3. Bật Docker

```powershell
docker compose --env-file .env -f deploy/docker-compose.local.yml up -d
docker compose --env-file .env -f deploy/docker-compose.local.yml ps
```

| Container | Image | Vai trò |
|---|---|---|
| `genie-local-postgres-1` | `postgres:16-alpine` | CSDL |
| `genie-local-redis-1` | `redis:7-alpine` | OTP, rate limit, cache |
| `genie-local-silo-1` | `pgsty/silo:RELEASE.2026-10-26T00-00-00Z` | Kho tệp kiểu S3 (ảnh, hồ sơ KYC mã hóa) |
| `genie-local-silo-init-1` | (cùng image Silo) | Tạo bucket rồi **tự tắt** — `Exited (0)` là đúng |
| `genie-local-mailpit-1` | `axllent/mailpit:v1.20` | Hộp thư giả |
| `genie-ai-worker` (chạy tay) | `genie-ai-worker:local` | AI KYC (mục 6) |

Docker Desktop tự tắt (hết RAM, khởi động lại máy) → các container dừng theo → mở lại Docker Desktop rồi chạy lại lệnh `up -d`.

## 4. Backend & dữ liệu mẫu (seed)

### 4.1. Chạy backend

```powershell
cd server-spring
$env:JAVA_HOME = "C:\Users\MSII\.jdks\ms-21.0.10"
.\mvnw.cmd spring-boot:run
```

- Lần đầu trên DB trống: Flyway chạy mọi migration (`V1` → `V24`) rồi các file seed `R__seed_*` — log `Successfully applied … migrations`, `Started GenieApplication`.
- Vừa đổi nhánh / đổi số migration / lỗi lạ kiểu `Found more than one migration with version …`, `ClassNotFoundException` → chạy `.\mvnw.cmd clean spring-boot:run`.
- Lỗi Flyway *checksum / description mismatch* trên DB đang có dữ liệu → DB từng chạy migration của nhánh khác; dễ nhất là làm sạch (mục 8).

### 4.2. Seed tự động (không cần làm gì)

| Phần | Nằm ở đâu | Cách nạp |
|---|---|---|
| Dữ liệu trong DB: 15 tài khoản, danh mục, 11 shop (6 VERIFIED + 5 hồ sơ KYC), ~250 sản phẩm (gồm sản phẩm Excel), voucher, 19 banner, Điểm / yêu thích / theo dõi của `mai.tran` | `server-spring/src/main/resources/db/migration/R__seed_demo.sql` + `db/seed/R__seed_demo_*.sql` (10 → 95) | **Tự động** khi chạy backend (profile `local`, `GENIE_SEED_DEMO=true`) |
| Tệp ảnh sản phẩm / banner | `web/public/img/products`, `web/public/img/banners`, `scripts/seed/excel/assets/` | Chạy tay `upload-assets.ps1` (dưới) |

DB chỉ lưu **đường dẫn ảnh**; tệp ảnh nằm trên Silo. Chưa đẩy ảnh → dữ liệu đúng nhưng ảnh vỡ. **Đẩy ảnh** (sau khi Docker chạy; chạy lại không sao):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\seed\upload-assets.ps1
```

Script đẩy ảnh mẫu + ảnh sản phẩm Excel lên Silo và **kiểm đủ ảnh** (in `Ảnh seed Excel: đủ 54 ảnh trên Silo`). Nạp lại riêng dữ liệu DB không cần
backend: `powershell -ExecutionPolicy Bypass -File scripts\seed\seed.ps1 -Docker` (thêm `-Only 95` = chỉ sản phẩm Excel). Chi tiết: [scripts/seed/README.md](../scripts/seed/README.md).

### 4.3. Tool seed sản phẩm từ Excel (`scripts/seed/excel/`) — từng bước

> Tool của q2nishere (plan 2026-11-21). Tài liệu gốc đầy đủ: [scripts/seed/excel/README.md](../scripts/seed/excel/README.md).
> **Chỉ dùng cho máy dev** (Postgres + Silo chạy Docker). Không bao giờ trỏ `.env` vào RDS / S3 production rồi chạy.

#### 4.3.1. Tool này làm gì (hiểu trước 1 phút)

Bạn điền sản phẩm vào **một file Excel** → tool Python đọc file → sinh ra 2 thứ → 2 thứ đó được nạp vào máy bạn:

```
scripts/seed/excel/products.xlsx  ─┐
scripts/seed/excel/anh/  (ảnh gốc) ─┴─► excel_to_seed.py build ─► assets/*.jpg + manifest.json  ──► upload-assets ──► Silo (ảnh)
                                                                └► R__seed_demo_95_excel_products.sql ──► seed ──► PostgreSQL (dữ liệu)
```

| Thứ | Ở đâu | Commit lên git? |
|---|---|---|
| File Excel (nguồn dữ liệu) | `scripts/seed/excel/products.xlsx` — **đã có sẵn 54 sản phẩm, 89 biến thể** | ✅ Có |
| Ảnh gốc bạn bỏ vào | `scripts/seed/excel/anh/` (đang trống trên máy bạn — bình thường) | ❌ Không (`.gitignore`) |
| Ảnh thu nhỏ tool sinh | `scripts/seed/excel/assets/` (54 `.jpg` + `manifest.json`) | ✅ Có |
| File SQL tool sinh | `server-spring/src/main/resources/db/seed/R__seed_demo_95_excel_products.sql` | ✅ Có — **không sửa tay** |

Vì `assets/` + file SQL đã nằm trên git, nên có **2 trường hợp**:

| Bạn muốn | Làm theo |
|---|---|
| Chỉ cần có sản phẩm để test (không sửa gì) | **4.3.2** — không cần Python |
| Thêm / sửa / xóa sản phẩm | **4.3.3 → 4.3.9** |

#### 4.3.2. Trường hợp A — chỉ cần dữ liệu (không cần Python)

Mở **PowerShell** tại gốc repo (`D:\TLCN-KLTN\TLCN\genie-multivendor-platform`), Docker đã `up -d` (mục 3):

```powershell
git pull
# 1) đẩy 54 ảnh trong scripts\seed\excel\assets\ lên Silo máy mình — cuối phải in "đủ 54 ảnh trên Silo"
powershell -ExecutionPolicy Bypass -File scripts\seed\upload-assets.ps1
# 2) nạp dữ liệu: chạy backend là tự nạp (GENIE_SEED_DEMO=true)
cd server-spring; .\mvnw.cmd spring-boot:run
```

Quên bước 1 → sản phẩm vẫn hiện nhưng **ảnh vỡ**; chạy lại `upload-assets.ps1` là có ảnh.

#### 4.3.3. Trường hợp B — bước 1: chuẩn bị máy (làm một lần)

**a) Kiểm Python.** Mở PowerShell gõ:

```powershell
python --version        # máy bạn: Python 3.13.7 → dùng được (README ghi 3.12, nhưng 3.13 đã chạy thử OK)
py -0p                  # (tuỳ chọn) liệt kê mọi bản Python đã cài
```

Báo `'python' is not recognized` → cài Python từ python.org, lúc cài **tick "Add python.exe to PATH"**, mở lại PowerShell.

**b) Cài thư viện của tool** (đứng ở **gốc repo**, không phải trong `scripts\seed\excel`):

```powershell
cd D:\TLCN-KLTN\TLCN\genie-multivendor-platform
pip install -r scripts\seed\excel\requirements.txt     # openpyxl (đọc Excel), Pillow (xử lý ảnh), pytest
```

Kiểm cài xong: `python -c "import openpyxl, PIL; print('ok')"` → in `ok`.

**c) Điều kiện trước khi nạp:**
1. Docker đang chạy: `docker ps` thấy `genie-postgres`, `genie-redis`, `genie-silo` (mục 3).
2. Backend **đã chạy ít nhất một lần** trên CSDL này (để Flyway tạo bảng + nạp shop, danh mục gốc). Sản phẩm Excel gắn vào shop / danh mục đó, thiếu thì nạp báo lỗi `chưa có shop VERIFIED` / `không có danh mục`.

**d) Web xem được kết quả** khi `web/.env` có `product,search,shop` trong `PUBLIC_REAL_API` (đã có — mục 2.2).

#### 4.3.4. Bước 2: mở file Excel

Mở `scripts\seed\excel\products.xlsx` bằng Excel. **File đã có sẵn — đừng chạy lệnh `template`** (lệnh đó chỉ dùng khi chưa có file; thêm `--force` sẽ **ghi đè mất 54 sản phẩm**).

File có 6 sheet:

| Sheet | Dùng để | Bạn có sửa không |
|---|---|---|
| `HuongDan` | Tóm tắt cách nhập | Chỉ đọc |
| `SanPham` | **Mỗi dòng = 1 sản phẩm** | ✅ |
| `BienThe` | Mỗi dòng = 1 biến thể (màu, size…) của sản phẩm | ✅ (tuỳ chọn) |
| `ThongSo` | Mỗi dòng = 1 thông số (bảo hành, chất liệu…) | ✅ (tuỳ chọn) |
| `_DanhMuc` | Danh sách slug danh mục hợp lệ (24 dòng) | ❌ Chỉ để tra |
| `_Shop` | Danh sách email chủ shop hợp lệ (6 shop) | ❌ Chỉ để tra |

Mẹo: **cột tô vàng là bắt buộc**; rê chuột vào ô tiêu đề để xem giải thích. Tool đọc theo **tên cột** ở dòng 1, nên đừng đổi tên cột.

#### 4.3.5. Bước 3: điền một sản phẩm (ví dụ thật)

Thêm một dòng mới ở cuối sheet **`SanPham`**:

| Cột | Ví dụ | Ghi chú |
|---|---|---|
| `ma_sp` ✔ | `XL55` | Mã tự đặt, **không trùng** dòng khác, chữ không dấu + số, ≤ 20 ký tự. File đang dùng `XL01…XL54` |
| `email_chu_shop` ✔ | `mai.tran@gmail.com` | Chọn trong danh sách thả xuống (= sheet `_Shop`). `mai.tran` = shop TechZone |
| `slug_danh_muc` ✔ | `thiet-bi-am-thanh` | Chọn trong danh sách (= sheet `_DanhMuc`), nên chọn **danh mục con** |
| `ten` ✔ | `Loa Bluetooth JBL Flip 6` | ≤ 255 ký tự. **Tên + email chủ shop là "khóa" của sản phẩm** (xem 4.3.9) |
| `mo_ta` | `Loa chống nước IP67…` | Xuống dòng trong ô: **Alt+Enter** |
| `thuong_hieu` | `JBL` | Trùng bảng brands (Sony, Samsung…) thì lọc theo hãng được |
| `tinh_trang` | `NEW` | `NEW` (mặc định) / `USED` / `REFURBISHED` |
| `xuat_xu` | `Trung Quốc` | |
| `gia` ✔ | `2490000` | Số nguyên VND; gõ `2.490.000` hay `2,490,000` cũng nhận |
| `ton_kho` | `100` | **Bắt buộc nếu không có biến thể** ở sheet `BienThe` |
| `can_nang_g` ✔ | `550` | Gram |
| `dai_cm` `rong_cm` `cao_cm` | `18` `7` `7` | Kích thước đóng gói (tuỳ chọn) |
| `mien_phi_ship` | `x` | `x` = shop trả ship; trống = người mua trả |
| `dat_truoc_ngay` | (trống) | Số ngày chuẩn bị nếu là hàng đặt trước |
| `trang_thai` | `PUBLISHED` | `PUBLISHED` (mặc định, hiện trên web) / `DRAFT` / `HIDDEN` |
| `da_ban` · `diem_sao` | `120` · `4.7` | Giả số đã bán / điểm 0–5 để thử sắp xếp |
| `anh` ✔ | `jbl-flip6-1.jpg; jbl-flip6-2.jpg` | Ảnh — xem bước 4. **Ảnh đầu = ảnh chính**, tối đa 8 |

**Biến thể (tuỳ chọn)** — sheet **`BienThe`**, mỗi màu / size một dòng, `ma_sp` trùng với sheet `SanPham`:

| `ma_sp` | `ma_bien_the` | `ten_bien_the` | `thuoc_tinh` | `gia` | `ton_kho` | `anh` |
|---|---|---|---|---|---|---|
| `XL55` | `BLK` | `Đen` | `color=Đen` | (trống = giá SP) | `40` | (tuỳ chọn) |
| `XL55` | `BLU` | `Xanh` | `color=Xanh dương` | `2590000` | `25` | `jbl-flip6-blue.jpg` |

- SKU tự sinh: `XL-<ma_sp>-<ma_bien_the>` → `XL-XL55-BLK`.
- Nhiều thuộc tính: `color=Đen; size=M` (cách nhau bằng `;`).
- Không có dòng biến thể nào → tool tự tạo 1 biến thể `Mặc định` (SKU `XL-XL55-STD`) với tồn kho = cột `ton_kho`.

**Thông số (tuỳ chọn)** — sheet **`ThongSo`**: `XL55 · warranty · 12 tháng`. Khóa nên dùng `warranty`, `color`, `material`, `size`, `skin_type` để bộ lọc trên web dùng được; khóa khác vẫn lưu, tool chỉ cảnh báo.

Lưu file Excel (**Ctrl+S**) và **đóng Excel** trước khi chạy tool (Excel đang mở có thể khóa file).

#### 4.3.6. Bước 4: đưa ảnh vào (chọn 1 trong 3 cách)

| Cách | Làm thế nào | Khi nào dùng |
|---|---|---|
| **1. Tên tệp** (khuyên dùng) | Copy ảnh vào `scripts\seed\excel\anh\` (tạo folder nếu chưa có), rồi ghi tên tệp vào ô `anh`, nhiều ảnh cách nhau `;` — vd `jbl-flip6-1.jpg; jbl-flip6-2.jpg` | Mặc định. Giữ chất lượng, file Excel nhẹ. Tên không phân biệt hoa thường |
| **2. Link** | Ghi thẳng `https://…/anh.jpg` vào ô `anh` | Thử nhanh; web nguồn chết / chặn thì lỗi (tool cảnh báo) |
| **3. Chèn ảnh vào Excel** | **Insert → Pictures → Place over Cells**, kéo ảnh sao cho **góc trên-trái** nằm trong ô `anh` (ảnh 2 → ô `anh_2` … `anh_8`) của đúng dòng | Muốn nhìn thấy ảnh ngay trong bảng |

Với cách 3, **phải tắt nén ảnh** trước khi lưu: **File → Options → Advanced → Image Size and Quality → tick "Do not compress images in file"** (không thì Excel làm mờ ảnh).

Ảnh nhận: `.jpg` `.jpeg` `.png` `.webp`, ≤ 25 MB. Tool tự xoay đúng chiều, nền trong suốt → trắng, thu cạnh dài về ≤ 1200px, lưu JPEG.

> **Vì sao 54 sản phẩm cũ vẫn chạy dù `anh\` trống?** `assets\manifest.json` nhớ ảnh gốc nào ứng với ảnh thu nhỏ nào, nên sản phẩm cũ không cần ảnh gốc. Chỉ **sản phẩm / ảnh mới** mới cần file trong `anh\`.

#### 4.3.7. Bước 5: kiểm file (không ghi gì, chạy bao nhiêu lần cũng được)

PowerShell, đứng ở **gốc repo**:

```powershell
python scripts\seed\excel\excel_to_seed.py check scripts\seed\excel\products.xlsx
```

| Kết quả in ra | Nghĩa | Làm gì |
|---|---|---|
| `Ổn: 55 sản phẩm, 91 biến thể, 56 ảnh.` | Hợp lệ (số tăng theo dòng bạn thêm) | Sang bước 6 |
| `SanPham!I56: phải là số nguyên VND` | Lỗi ở **sheet SanPham, ô I56** (cột I, dòng 56) | Mở Excel, sửa đúng ô đó, lưu, chạy lại `check` |
| `không thấy tệp 'x.jpg' trong scripts/seed/excel/anh/` | Chưa copy ảnh hoặc gõ sai tên | Copy ảnh vào `anh\` / sửa tên |
| Dòng bắt đầu bằng cảnh báo (khóa thông số lạ, link ảnh…) | Vẫn chạy được | Xem có cần sửa không |

Hiện tại máy bạn chạy `check` ra `Ổn: 54 sản phẩm, 89 biến thể, 54 ảnh.`

#### 4.3.8. Bước 6: build + nạp vào máy (1 lệnh)

```powershell
python scripts\seed\excel\excel_to_seed.py build scripts\seed\excel\products.xlsx --apply --flush-cache
```

Lệnh này làm lần lượt:

| Phần | Làm gì | Bỏ phần này thì sao |
|---|---|---|
| `build` | Kiểm lại → thu nhỏ ảnh vào `assets\` → ghi file SQL 95. **Có lỗi thì không ghi gì** | — |
| `--apply` | Chạy `upload-assets` (đẩy ảnh lên Silo) + `seed --docker --only 95` (nạp SQL vào Postgres Docker) | Chỉ sinh file; phải tự chạy `upload-assets.ps1` + `seed.ps1 -Docker -Only 95` (hoặc khởi động lại backend) |
| `--flush-cache` | Xóa cache sản phẩm / shop trong Redis | Web có thể hiện dữ liệu **cũ 5–10 phút** |

**Cách không cần gõ lệnh** (Windows): mở File Explorer tới `scripts\seed\excel\`, **nhấp đúp `excel_to_seed.bat`** (dùng `products.xlsx`) hoặc **kéo thả một file `.xlsx` khác lên `excel_to_seed.bat`**. Cửa sổ đen chạy đúng lệnh trên, cuối in `Exit code: …` và chờ bấm phím.

| `Exit code` | Nghĩa |
|---|---|
| `0` | Thành công |
| `1` | Lỗi dữ liệu Excel (đọc dòng lỗi phía trên, sửa ô đó) |
| `2` | Gõ lệnh sai |
| `3` | Lỗi khi đẩy ảnh lên Silo / nạp CSDL → kiểm Docker có chạy không, backend đã chạy lần nào chưa |

#### 4.3.9. Bước 7: kiểm tra kết quả

1. **Web** (`npm run dev` trong `web/`): mở `http://localhost:4321/search?q=JBL` → thấy sản phẩm, bấm vào xem ảnh + biến thể.
2. **DB** (Adminer / psql — mục 9):
   ```sql
   SELECT p.id, p.name, v.sku, v.price, v.stock_count
   FROM product_variants v JOIN products p ON p.id = v.product_id
   WHERE v.sku LIKE 'XL-XL55-%' AND v.deleted_at IS NULL;
   ```
   (`XL-%` = mọi sản phẩm đến từ Excel.)
3. **Ảnh trên Silo:** `http://localhost:9001` → bucket `product-media` → `seed/excel/`.

**Sửa lần sau** = sửa Excel → `check` → `build --apply --flush-cache`. Tool so theo **tên sản phẩm + email chủ shop**:

| Bạn làm gì trong Excel | Kết quả sau khi nạp |
|---|---|
| Sửa giá, mô tả, danh mục, thông số, trạng thái | Sản phẩm được **cập nhật** |
| Sửa biến thể (giá, tồn, tên, ảnh) | Cập nhật theo SKU |
| Xóa một dòng biến thể | Biến thể bị **xóa mềm** (ẩn) |
| **Đổi tên** sản phẩm | Coi là **sản phẩm mới**; bản tên cũ chuyển `HIDDEN` |
| Xóa một dòng sản phẩm | Sản phẩm chuyển `HIDDEN` |
| Đổi `ma_sp` | SKU đổi: biến thể cũ xóa mềm, biến thể mới tạo |
| Thay ảnh (cùng tên, nội dung khác) | Sinh ảnh thu nhỏ mới, ảnh cũ trong `assets\` bị xóa |

Muốn xóa sạch hẳn bản cũ: làm sạch dữ liệu (mục 8.1) rồi chạy lại backend.

#### 4.3.10. Bước 8: commit (khi muốn chia sẻ cho nhóm)

| File | Commit? |
|---|---|
| `server-spring/src/main/resources/db/seed/R__seed_demo_95_excel_products.sql` | ✅ **Bắt buộc** |
| `scripts/seed/excel/assets/` (mọi `.jpg` + `manifest.json`, **kể cả file bị xóa**) | ✅ **Bắt buộc** |
| `scripts/seed/excel/products.xlsx` | ✅ **Bắt buộc** (để người khác sửa tiếp) |
| `scripts/seed/excel/anh/` | ❌ Không — tự giữ ảnh gốc |

```powershell
git status                                      # xem đúng 3 nhóm file trên thay đổi
git add server-spring/src/main/resources/db/seed/R__seed_demo_95_excel_products.sql scripts/seed/excel/assets scripts/seed/excel/products.xlsx
git commit -m "chore(db): add excel seed products"
```

Người khác `git pull` xong chỉ cần làm 4.3.2.

#### 4.3.11. Lỗi hay gặp

| Hiện tượng | Cách sửa |
|---|---|
| `pip` / `python` không nhận | Cài Python có tick "Add to PATH"; hoặc thay `python` bằng `py -3`, `pip` bằng `py -3 -m pip` |
| `ModuleNotFoundError: openpyxl` | Chưa chạy bước 4.3.3 b |
| `PermissionError` khi đọc `products.xlsx` | Excel đang mở file → đóng Excel |
| `UnicodeEncodeError` khi in | Chạy trong PowerShell / Windows Terminal; hoặc `$env:PYTHONIOENCODING="utf-8"` rồi chạy lại |
| Ảnh vỡ trên web | Chưa `upload-assets`; nếu báo `THIẾU ảnh trên Silo` → kiểm `MINIO_ENDPOINT` / `STORAGE_BUCKET_PREFIX` trong `.env` |
| Sửa giá mà web vẫn giá cũ | Thiếu `--flush-cache`, hoặc đợi 5–10 phút |
| `chưa có shop VERIFIED` / `không có danh mục` | CSDL chưa có dữ liệu gốc → chạy backend một lần (hoặc `seed.ps1 -Docker` không `-Only`) |
| Backend báo lỗi Flyway `seed demo 95 excel products` | File 95 bị sửa tay / tham chiếu shop không có → `build` lại |
| `không đọc được ảnh kiểu 'Place in Cell'` | Chèn lại bằng **Place over Cells**, hoặc dùng cách tên tệp |

**Không bao giờ:** sửa tay file SQL 95 (lần build sau ghi đè), commit `anh\`, quên commit `assets\`, chạy `template --force` lên file đang có dữ liệu.

## 5. Web Astro

```powershell
cd web
npm install        # lần đầu / khi package.json đổi
npm run dev
```

| Khu vực | URL |
|---|---|
| Trang chủ, danh mục, sản phẩm, tìm kiếm | `/`, `/c/{slug}`, `/p/{slug}-{id}`, `/search?q=…` |
| Đăng nhập / đăng ký | `/auth/login`, `/auth/register` |
| Tài khoản | `/account/profile`, `/account/address`, `/account/points`, `/account/vouchers`, `/account/wishlist`, `/account/following` |
| Mở shop | `/become-seller` (email shop = email tài khoản, ô khóa) |
| Seller Center | `/seller` (`/seller/kyc`, `/seller/settings`) |
| Admin | `/admin` (`/admin/audit-logs`, `/admin/banners`, `/admin/kyc`, `/admin/staff`, `/admin/users`, `/admin/catalog`…) |

Nhân viên / Admin đăng nhập xong vào thẳng `/admin`; **Đăng xuất** ở menu tài khoản góc phải thanh trên.

## 6. AI KYC

| Chế độ | `.env` gốc | Cần chạy worker? | Kết quả |
|---|---|---|---|
| **http** (đang dùng) | `AI_WORKER_MODE=http` + khối AI ở mục 2.1 | Có (bước 2 mục 0) | AI thật đọc QR, MRZ, so khuôn mặt |
| **stub** | thêm `#` đầu dòng `AI_WORKER_MODE=http` | Không | Theo **2 số cuối CCCD**: `00–29` tự duyệt · `30–69` nhân viên duyệt · `70–99` từ chối |

Đổi chế độ → **khởi động lại backend**.

**Build image worker** (lần đầu ~5–10 phút, ~1,7 GB; làm lại khi code `server-python/` đổi):

```powershell
docker build -t genie-ai-worker:local server-python
```

**Model AI** nằm ở `server-python\models\` (`face_detection_yunet_2023mar.onnx`, `face_recognition_sface_2021dec.onnx`) — **máy bạn đã có, không cần làm bước này**.
Chỉ khi thiếu (clone repo mới, lỡ xóa) mới tải lại bằng script `scripts/fetch-models.sh`. Đây là script **bash**, nên:

| Gõ ở đâu | Lệnh (đứng ở **gốc repo**) |
|---|---|
| **Git Bash** (chuột phải trong thư mục repo → *Open Git Bash here*) | `SKIP_PHOBERT=1 bash scripts/fetch-models.sh` |
| **PowerShell** | `$env:SKIP_PHOBERT="1"; bash scripts/fetch-models.sh` |

`SKIP_PHOBERT=1` = bỏ qua model PhoBERT (phân tích cảm xúc, chưa phát hành) — thiếu biến này script sẽ báo lỗi tải.
Script tải 2 file `.onnx` từ opencv_zoo và kiểm SHA-256; xong thì build lại image worker. Kiểm: `dir server-python\models` thấy 2 file `.onnx`.

Worker chạy trong Docker nên `localhost` trong container là chính nó → backend ký link ảnh bằng `host.docker.internal:9000` và worker gọi lại
`host.docker.internal:8080` (`ping host.docker.internal` phải ra IP máy). Ảnh nên chụp rõ, cạnh ngắn ≥ 600 px; ảnh **nhòe** bị báo `UNREADABLE_IMAGE` →
nhân viên duyệt hoặc từ chối. Giải thích đầy đủ: [kyc-giai-thich-cho-nguoi-moi.md](kyc-giai-thich-cho-nguoi-moi.md).

| Triệu chứng | Cách xử lý |
|---|---|
| `pull access denied for genie-ai-worker` | Chưa build image → lệnh `docker build` ở trên |
| `/health` → `models_loaded: false` | Thiếu `.onnx` hoặc chạy `docker run` không đứng ở gốc repo (`${PWD}` sai) |
| Hồ sơ đứng ở "Đang xác minh", worker không có log | Backend vẫn `stub` hoặc `AI_WORKER_URL` sai → sửa `.env`, restart backend |
| Backend lỗi `invalid hostname http://…  # …` | Có chú thích cuối dòng trong `.env` → chuyển lên dòng riêng |
| 401 / lỗi tải ảnh / lỗi gọi lại | `INTERNAL_TOKEN` lệch · `PUBLIC_S3_URL` còn `localhost:9000` · Firewall chặn Java cổng 8080 |

## 7. Tài khoản seed (mật khẩu chung `Genie@123`)

| Email | Vai trò | Dùng để test |
|---|---|---|
| `mai.tran@gmail.com` | Người mua + chủ shop **TechZone Official** (VERIFIED) | Điểm, yêu thích, theo dõi, cài đặt shop, tài khoản nhận tiền |
| `yen.dh@genie.example` | Admin | Mọi màn Admin, duyệt KYC + hoa hồng |
| `phat.nt@genie.example` | Moderator | Duyệt KYC (không sửa hoa hồng) |
| `linh.tm@genie.example` | Tài chính | Banner, hoa hồng (không xem KYC) |
| `truc.ht@genie.example` | CSKH | Kiểm tra bị chặn 403 |
| `owner.<shoe/boutique/home/beauty/travel>@genie.example` | Chủ 5 shop còn lại | Theo dõi shop, sản phẩm |
| `kyc.<nhasachmoc/gombattrang/mebehappy/travelgear/cheapelec>@genie.example` | Chủ 5 hồ sơ KYC | Hàng chờ KYC phía người bán |

Tài khoản mới: `/auth/register` bằng email bất kỳ → OTP ở Mailpit http://localhost:8025.

## 8. Tắt, bật lại & làm sạch

| Việc | Lệnh |
|---|---|
| Tắt backend / web / worker | `Ctrl + C` trong cửa sổ tương ứng |
| Tắt Docker (giữ dữ liệu) | `docker compose --env-file .env -f deploy/docker-compose.local.yml stop` |
| Bật lại | `docker compose --env-file .env -f deploy/docker-compose.local.yml up -d` |

### 8.1. Làm sạch **dữ liệu** (giữ image, nhanh nhất)

Xóa DB, ảnh Silo, Redis, mail → chạy lại là đúng trạng thái seed ban đầu (tài khoản tự tạo, KYC, banner, nhật ký… test đều mất):

```powershell
cd D:\TLCN-KLTN\TLCN\genie-multivendor-platform
docker compose --env-file .env -f deploy/docker-compose.local.yml down -v --remove-orphans
docker compose --env-file .env -f deploy/docker-compose.local.yml up -d
# chạy backend (tự migrate + seed) rồi đẩy lại ảnh:
powershell -ExecutionPolicy Bypass -File scripts\seed\upload-assets.ps1
```

### 8.2. Làm sạch **hoàn toàn** mọi thứ của Genie (container + volume + network + image)

Dùng khi muốn giải phóng ổ đĩa hoặc làm lại từ đầu. Chỉ xóa thứ của Genie; **giữ** `postgres:16-alpine` vì project khác (`dove-postgres`) cũng dùng.

```powershell
cd D:\TLCN-KLTN\TLCN\genie-multivendor-platform
# 1) tắt AI Worker nếu đang chạy
docker rm -f genie-ai-worker 2>$null
# 2) container + volume (dữ liệu) + network của Genie
docker compose --env-file .env -f deploy/docker-compose.local.yml down -v --remove-orphans
# 3) image riêng của Genie (lần sau tự tải / build lại)
docker rmi redis:7-alpine pgsty/silo:RELEASE.2026-10-26T00-00-00Z axllent/mailpit:v1.20 genie-ai-worker:local genie-ai-worker:test
# 4) (tùy chọn) dọn cache build — ảnh hưởng cache build của MỌI project, chỉ làm cho chậm lần build sau
docker builder prune -f
# 5) kiểm tra còn gì của genie
docker ps -a --filter "name=genie"; docker volume ls --filter "name=genie"; docker images | Select-String "genie|silo|mailpit"
```

Lệnh `docker rmi` báo *No such image* cho image không có là bình thường. Không dùng `docker system prune -a` (xóa luôn image / volume của project khác).

**Chạy lại sau khi làm sạch hoàn toàn:**

```powershell
docker compose --env-file .env -f deploy/docker-compose.local.yml up -d   # tự tải lại image (~350 MB)
docker build -t genie-ai-worker:local server-python                     # nếu dùng AI thật
# chạy backend (mục 4.1) → seed tự nạp
powershell -ExecutionPolicy Bypass -File scripts\seed\upload-assets.ps1
# chạy web (mục 5); trình duyệt báo 401 → đăng xuất / xóa cookie localhost:4321 rồi đăng nhập lại
```

**Ổ C vẫn đầy sau khi xóa?** Docker trên Windows lưu mọi thứ trong file `C:\Users\MSII\AppData\Local\Docker\wsl\disk\docker_data.vhdx`, file này
**không tự co**. Muốn lấy lại dung lượng: *Quit Docker Desktop* → PowerShell **Admin**: `wsl --shutdown` → `diskpart` rồi gõ
`select vdisk file="C:\Users\MSII\AppData\Local\Docker\wsl\disk\docker_data.vhdx"` · `attach vdisk readonly` · `compact vdisk` · `detach vdisk` · `exit`.

## 9. Xem dữ liệu trong DB

```powershell
docker exec -it genie-local-postgres-1 psql -U genie -d genie
```

```sql
SELECT version, description FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
SELECT id, name, status FROM products ORDER BY id LIMIT 10;
SELECT id, shop_name, user_id, kyc_status FROM vendor_profiles ORDER BY id;   -- shop_id trên API = vendor_profiles.id
SELECT created_at, user_email, action_type FROM system_logs ORDER BY created_at DESC LIMIT 10;
\q
```

DataGrip / DBeaver: host `localhost`, port `5433`, db `genie`, user `genie`. Không thấy bảng → bật hiển thị schema `public` (chuột phải DB → *Manage Shown Schemas*).

## 10. Lỗi hay gặp

| Triệu chứng | Nguyên nhân / cách sửa |
|---|---|
| `TypeTag :: UNKNOWN` | Đang dùng JDK 24 → `$env:JAVA_HOME = ...` (mục 1) |
| `The filename, directory name, or volume label syntax is incorrect` khi gõ `$env:` | Đang ở **cmd**, không phải PowerShell (mục 1) |
| `Connection to localhost:5432 refused` | `DB_URL` còn 5432 → sửa 5433 |
| Backend thoát `exit code 1` ngay khi khởi động | Docker / Postgres đang tắt → `up -d`; xem dòng `Caused by:` trong log |
| `port is already allocated` | Cổng bị app khác chiếm → đổi `*_PORT` trong `.env` hoặc tắt container khác (`docker ps`) |
| Không nhận OTP | SMTP chưa trỏ Mailpit (mục 2.1), restart backend, xem http://localhost:8025 |
| Màn hiện dữ liệu giả dù backend có | Thiếu cờ trong `PUBLIC_REAL_API` hoặc chưa restart `npm run dev` |
| Ảnh sản phẩm / banner vỡ | Chưa `upload-assets.ps1` hoặc Silo tắt |
| Lỗi Flyway checksum / trùng version | `.\mvnw.cmd clean spring-boot:run`; vẫn lỗi → làm sạch dữ liệu (mục 8.1) |
| Web báo 401 liên tục | Token cũ sau khi reset DB → đăng xuất / xóa cookie `localhost:4321` |
| Máy hết RAM, Docker tự tắt | Đóng bớt cửa sổ VS Code / tab; mở lại Docker Desktop rồi `up -d` |
