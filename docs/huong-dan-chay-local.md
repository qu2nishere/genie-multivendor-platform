# Hướng dẫn chạy Genie trên máy local (Windows + PowerShell)

> Bản dành riêng cho máy dev Windows: Postgres chạy ở **cổng 5433** (5432 đã bị chiếm), email OTP đi qua **Mailpit**, AI KYC chạy chế độ **stub**.
> Cấu hình chi tiết từng biến xem thêm [cau-hinh-moi-truong.md](cau-hinh-moi-truong.md).

## 0. Cần cài sẵn

| Công cụ | Phiên bản | Ghi chú |
|---|---|---|
| JDK | **21** (`C:\Users\MSII\.jdks\ms-21.0.10`) | Máy đang có JDK 24 mặc định → Lombok lỗi `TypeTag :: UNKNOWN`, nên phải trỏ JAVA_HOME về 21 |
| Docker Desktop | bản mới | Chạy Postgres, Redis, Silo (S3), Mailpit |
| Node.js | 22 (kèm npm) | Chạy web Astro |
| Python | 3.12 (không bắt buộc) | AI worker chạy bằng Docker nên không cần cài (mục 6.2) |

### Về dòng `$env:JAVA_HOME = ...`

```powershell
$env:JAVA_HOME = "C:\Users\MSII\.jdks\ms-21.0.10"
```

- Gán biến môi trường `JAVA_HOME` **chỉ cho cửa sổ PowerShell hiện tại**. `mvnw.cmd` đọc `JAVA_HOME` để chọn JDK → backend được build/chạy bằng JDK 21.
- Đóng cửa sổ là mất, mở terminal mới phải gõ lại.
- Muốn cố định: *Start → "Edit the system environment variables" → Environment Variables* → đặt `JAVA_HOME = C:\Users\MSII\.jdks\ms-21.0.10` và đưa `%JAVA_HOME%\bin` lên đầu `Path`. (Hoặc chạy bằng IntelliJ với Project SDK = 21.)
- Kiểm tra: `& "$env:JAVA_HOME\bin\java" -version` → phải ra `21.0.10`.

## 1. Chuẩn bị file `.env` (một lần)

### 1.1. `.env` ở thư mục gốc repo

Mở `.env` và đảm bảo có các dòng sau (giữ nguyên các secret khác đang có):

```dotenv
SPRING_PROFILES_ACTIVE=local

# Postgres ở cổng 5433
POSTGRES_PORT=5433
DB_URL=jdbc:postgresql://localhost:5433/genie
DB_USER=genie
DB_PASSWORD=genie

# Email OTP đi qua Mailpit (KHÔNG dùng Gmail khi test – tránh gửi mail thật ra ngoài)
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_USER=
SMTP_PASSWORD=
SMTP_AUTH=false
SMTP_STARTTLS=false
MAIL_FROM=no-reply@genie.local
```

> Nếu muốn quay lại Gmail thì comment khối Mailpit và mở lại khối Gmail cũ. Khi test bằng tài khoản seed (`@genie.example`) **luôn dùng Mailpit**.

`POSTGRES_PORT` được `docker-compose.local.yml` dùng để map cổng container 5432 → máy 5433; `DB_URL` là cái Spring Boot dùng để kết nối. Hai giá trị phải khớp nhau.

### 1.2. `web/.env`

Tạo file `web/.env`:

```dotenv
PUBLIC_WEB_URL=http://localhost:4321
PUBLIC_API_URL=http://localhost:8080/api/v1
INTERNAL_API_URL=http://localhost:8080/api/v1
PUBLIC_S3_URL=http://localhost:9000
PUBLIC_USE_MOCK=true
PUBLIC_REAL_API=auth,upload,profile,security,address,audit,banner,coins,wishlist,follow,vendor
```

- `PUBLIC_USE_MOCK=true`: những phần chưa có backend (catalog, giỏ hàng, đơn hàng, …) vẫn chạy bằng dữ liệu giả.
- `PUBLIC_REAL_API`: danh sách tính năng **gọi backend thật**. Các cờ có thể dùng: `address, audit, auth, banner, coins, follow, pickup, profile, security, seller-search, upload, vacation, vendor, wishlist`.
- Đổi `web/.env` xong phải **tắt và chạy lại** `npm run dev`.

## 2. Bật hạ tầng Docker

```powershell
cd D:\TLCN-KLTN\TLCN\genie-multivendor-platform
docker compose --env-file .env -f deploy/docker-compose.local.yml up -d
docker compose --env-file .env -f deploy/docker-compose.local.yml ps
```

| Dịch vụ | Cổng máy | Dùng để |
|---|---|---|
| postgres | **5433** | CSDL `genie` |
| redis | 6379 | Cache, OTP, rate-limit |
| silo (S3) | 9000 (API), 9001 (console) | Lưu ảnh, hồ sơ KYC |
| silo-init | – | Tạo bucket rồi tự thoát (trạng thái `Exited (0)` là bình thường) |
| mailpit | 1025 (SMTP), **8025** (web) | Hộp thư giả – xem OTP tại http://localhost:8025 |

Nếu Docker Desktop đang tắt: mở *Docker Desktop* và đợi biểu tượng cá voi chuyển xanh rồi chạy lại lệnh `up -d`.

## 3. Chạy backend Spring Boot

```powershell
cd D:\TLCN-KLTN\TLCN\genie-multivendor-platform\server-spring
$env:JAVA_HOME = "C:\Users\MSII\.jdks\ms-21.0.10"
.\mvnw.cmd spring-boot:run
```

- Lần đầu: Flyway tự chạy migration (V1 → V20) và nạp dữ liệu seed (`R__seed_*`).
- Thấy log `Started GenieApplication in ... seconds` là xong. Kiểm tra:
  - http://localhost:8080/actuator/health → `{"status":"UP"}`
  - Swagger: http://localhost:8080/swagger-ui.html
- Nếu từng build bằng mã cũ và gặp lỗi Flyway kiểu *"Detected resolved migration not applied"* / checksum: chạy `.\mvnw.cmd clean spring-boot:run`.

### 3.1. Dữ liệu mẫu (seed)

Seed gồm **2 phần tách biệt**:

| Phần | Nằm ở đâu | Cách nạp |
|---|---|---|
| **Dữ liệu trong DB** (tài khoản, danh mục, 6 shop, ~200 sản phẩm, voucher, banner, Điểm / yêu thích / theo dõi của `mai.tran`, 5 hồ sơ KYC) | `server-spring/src/main/resources/db/seed/R__seed_demo_*.sql` | **Tự động** khi chạy backend (profile `local`, `GENIE_SEED_DEMO=true` là mặc định) — không cần làm gì |
| **Tệp ảnh** sản phẩm / banner | `web/public/img/products`, `web/public/img/banners` | Chạy tay `upload-assets.ps1` để đẩy lên Silo |

Trong DB, sản phẩm và banner chỉ lưu **đường dẫn ảnh** (`seed/product/headphones.jpg`…), còn tệp ảnh nằm trên Silo (S3 giả lập). Nếu không đẩy ảnh lên thì dữ liệu vẫn đúng nhưng ảnh sẽ bị vỡ.

**Đẩy ảnh lên Silo** (một lần, khi Docker đang chạy; chạy lại cũng không sao, tệp trùng sẽ bị ghi đè):

```powershell
cd D:\TLCN-KLTN\TLCN\genie-multivendor-platform
powershell -ExecutionPolicy Bypass -File scripts\seed\upload-assets.ps1
```

- `powershell -ExecutionPolicy Bypass -File …`: chạy file script `.ps1` và **chỉ lần này** bỏ qua chặn chạy script của Windows (mặc định Windows không cho chạy `.ps1`). Không thay đổi cài đặt máy.
- Script dùng container `silo-init` (có sẵn công cụ `mc`) để chép `web\public\img\products\*` → bucket `product-media/seed/product/` và `web\public\img\banners\*` → `content-assets/seed/banner/`.
- Script đọc thông tin đăng nhập Silo từ `.env` (giống backend). Nếu `MINIO_ENDPOINT` trỏ AWS S3, script sẽ dừng (không đẩy lên production).
- Xong thì mở thử http://localhost:9000/product-media/seed/product/headphones.jpg để kiểm tra.

**Nạp lại dữ liệu DB bằng tay** (chỉ khi cần, ví dụ đã sửa file seed hoặc xóa bớt dữ liệu mẫu mà không muốn reset DB):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\seed\seed.ps1 -Docker
```

Lệnh này chạy lại các file `R__seed_*` vào Postgres trong Docker. Chạy nhiều lần cũng không bị trùng dữ liệu, và script in bảng số dòng trước / sau. DB phải được backend migrate trước ít nhất một lần.

- Không muốn có dữ liệu mẫu: thêm `GENIE_SEED_DEMO=false` vào `.env`.
- Muốn seed lại từ đầu: `down -v` (mục 7) rồi chạy lại backend, sau đó chạy `upload-assets.ps1`.
- Chi tiết từng file seed và giải thích `balance_hash`: [scripts/seed/README.md](../scripts/seed/README.md).

## 4. Chạy web Astro

```powershell
cd D:\TLCN-KLTN\TLCN\genie-multivendor-platform\web
npm install        # chỉ lần đầu hoặc khi package.json đổi
npm run dev
```

Mở http://localhost:4321.

| Khu vực | URL |
|---|---|
| Trang chủ người mua | http://localhost:4321/ |
| Đăng nhập / đăng ký | `/auth/login`, `/auth/register` |
| Tài khoản | `/account/profile`, `/account/address`, `/account/points`, `/account/vouchers`, `/account/wishlist`, `/account/following` |
| Đăng ký mở shop | `/become-seller` |
| Kênh người bán | `/seller` (KYC: `/seller/kyc`, cài đặt: `/seller/settings`) |
| Trang quản trị | `/admin` (`/admin/audit-logs`, `/admin/banners`, `/admin/kyc`) |

## 5. Tài khoản seed (mật khẩu chung `Genie@123`)

| Email | Vai trò | Dùng để test |
|---|---|---|
| `mai.tran@gmail.com` | Người mua + chủ shop **TechZone Official** (đã VERIFIED) | Coin, wishlist, follow, cài đặt shop, tài khoản nhận tiền |
| `yen.dh@genie.example` | Admin (đủ quyền) | Audit log, banner, duyệt KYC, sửa hoa hồng |
| `phat.nt@genie.example` | Moderator | Duyệt KYC (không được sửa hoa hồng) |
| `linh.tm@genie.example` | Finance Officer | Quản lý banner (không xem được KYC) |
| `truc.ht@genie.example` | CS Specialist | Kiểm tra bị chặn 403 |
| `kyc.*@genie.example` | Chủ các shop trong hàng đợi KYC | Xem trạng thái KYC phía người bán |

Hàng đợi KYC seed: *Nhà Sách Mộc*, *Gốm Bát Tràng Xưa*, *Mẹ & Bé Happy* (MANUAL_REVIEW), *TravelGear Outdoor* (SUPPLEMENT_REQUESTED), *Cheap Electronics* (REJECTED).

Muốn có tài khoản mới tinh: đăng ký ở `/auth/register` bằng email bất kỳ (ví dụ `test1@genie.test`) → mã OTP xem ở Mailpit http://localhost:8025.

## 6. AI KYC

### 6.1. Mặc định: chế độ stub (không cần làm gì)

`AI_WORKER_MODE=stub` – backend tự “giả lập” kết quả sau ~2 giây dựa vào **2 số cuối CCCD**:

| 2 số cuối CCCD | Kết quả |
|---|---|
| `00`–`29` | VERIFIED (tự duyệt) |
| `30`–`69` | MANUAL_REVIEW (vào hàng đợi admin) |
| `70`–`99` | REJECTED |

Ví dụ: `079203000123` → VERIFIED, `079203000145` → MANUAL_REVIEW, `079203000188` → REJECTED.

### 6.2. AI worker thật (chế độ `http`)

Worker là service Python (FastAPI) ở `server-python/`. Nó tải 3 ảnh hồ sơ, đọc **QR** mặt trước, **MRZ** mặt sau (Tesseract) và **so khớp khuôn mặt** (YuNet + SFace), rồi gửi các tín hiệu về backend. **Backend** cộng điểm rủi ro từ các tín hiệu đó: dưới 30 tự duyệt, 30–69 chuyển người duyệt, từ 70 trở lên từ chối.

```
Trình duyệt ──nộp KYC──► Backend :8080 ──POST /kyc/verify (kèm 3 link ảnh ký sẵn 5 phút)──► Worker :8000
                                                                                            │ tải ảnh từ Silo :9000
Backend ◄──PATCH /api/v1/internal/kyc/{id}/result (checks, signals, extracted)──────────────┘
```

Máy bạn không có Python 3.12 nên **chạy worker bằng Docker**.

#### Vấn đề `localhost` khi worker chạy trong Docker

Trong container, `localhost` là **chính container đó**, không phải máy Windows. Vì vậy:

- Link ảnh backend ký có dạng `http://localhost:9000/...` → worker không tải được.
- Worker gọi lại `http://localhost:8080` → không tới được backend.

Cách sửa: dùng tên **`host.docker.internal`**. Docker Desktop đã ghi tên này vào file `hosts` của Windows (trỏ về IP máy bạn), nên **cả container lẫn trình duyệt** đều phân giải được. Kiểm tra: `ping host.docker.internal` phải ra IP máy (vd `192.168.1.151`).

#### Bước 1: model (đã có sẵn thì bỏ qua)

Kiểm tra `server-python\models\` đã có 2 file `face_detection_yunet_2023mar.onnx` và `face_recognition_sface_2021dec.onnx`. Thiếu thì tải bằng Git Bash:

```bash
SKIP_PHOBERT=1 bash scripts/fetch-models.sh
```

#### Bước 2: build image worker (lần đầu khoảng 5–10 phút, image khoảng 1,7 GB)

```powershell
cd D:\TLCN-KLTN\TLCN\genie-multivendor-platform
docker build -t genie-ai-worker:local server-python
```

Chỉ cần build lại khi code trong `server-python/` thay đổi.

#### Bước 3: chạy worker

```powershell
cd D:\TLCN-KLTN\TLCN\genie-multivendor-platform
docker run --rm --name genie-ai-worker -p 8000:8000 `
  -e INTERNAL_TOKEN=local-internal-token `
  -e BACKEND_URL=http://host.docker.internal:8080 `
  -e STORAGE_ALLOWED_HOSTS=host.docker.internal:9000 `
  -v "${PWD}\server-python\models:/app/models:ro" `
  genie-ai-worker:local
```

| Biến | Ý nghĩa |
|---|---|
| `INTERNAL_TOKEN` | Mật khẩu nội bộ giữa backend và worker. **Phải trùng** `INTERNAL_TOKEN` của backend (mặc định máy dev `local-internal-token`) |
| `BACKEND_URL` | Địa chỉ worker gọi lại để trả kết quả. Worker **chỉ** gọi đúng địa chỉ này (chống giả mạo) |
| `STORAGE_ALLOWED_HOSTS` | Host duy nhất worker được phép tải ảnh từ đó (chống SSRF). Phải khớp host trong link ảnh |
| `-v ...models:/app/models:ro` | Gắn thư mục model vào container (chỉ đọc) |

Cửa sổ này giữ để xem log, `Ctrl+C` để tắt (`--rm` tự xóa container). Kiểm tra http://localhost:8000/health phải trả `"status":"UP"` và `"models_loaded":true`.

#### Bước 4: chuyển backend sang `http`

Thêm hoặc sửa trong `.env` gốc:

```dotenv
# ===== AI Worker thật (mục 6.2) =====
AI_WORKER_MODE=http
# backend chạy trên Windows nên gọi worker qua localhost là được
AI_WORKER_URL=http://localhost:8000
INTERNAL_TOKEN=local-internal-token
BACKEND_INTERNAL_URL=http://host.docker.internal:8080
# link ảnh ký bằng host này để worker (trong Docker) tải được
PUBLIC_S3_URL=http://host.docker.internal:9000
```

> ⚠ `.env` của backend **không** hiểu chú thích nằm cuối dòng (`KHOA=giá_trị   # ghi chú` → ghi chú bị tính vào giá trị).
> Chú thích luôn đặt ở **dòng riêng** bắt đầu bằng `#`.

Rồi **khởi động lại backend** (`Ctrl+C`, chạy lại `.\mvnw.cmd spring-boot:run`).

> `PUBLIC_S3_URL` của backend cũng là địa chỉ trình duyệt dùng để xem và upload ảnh. Vì `host.docker.internal` phân giải được trên Windows nên web vẫn chạy bình thường. Nếu ảnh không hiện hoặc upload lỗi: kiểm tra `ping host.docker.internal`, và cho phép Docker / Java qua Windows Firewall khi được hỏi. `web/.env` giữ nguyên.

#### Bước 5: thử

1. Nộp KYC theo giai đoạn **C** / **K** trong [kiem-thu/kich-ban-2026-11-16-mo-shop-kyc.md](kiem-thu/kich-ban-2026-11-16-mo-shop-kyc.md).
2. Cửa sổ worker hiện log nhận `POST /kyc/verify`, xử lý khoảng 1–3 giây, rồi gọi lại backend.
3. Backend log nhận `PATCH /internal/kyc/{id}/result`. Hồ sơ đổi trạng thái; admin mở `/admin/kyc` thấy các cờ QR / MRZ / khớp mặt, điểm khớp mặt và danh sách tín hiệu.

**Kết quả thường gặp:**

| Ảnh nộp | Tín hiệu | Thường ra |
|---|---|---|
| Ảnh bất kỳ (không phải CCCD), ảnh nhỏ hơn 600 px hoặc mờ | `UNREADABLE_IMAGE` (30 điểm) | MANUAL_REVIEW |
| CCCD thật, rõ nét, selfie đúng người | Có thể không có tín hiệu nào | VERIFIED |
| Selfie người khác | `FACE_SCORE_LOW` (60 điểm) | MANUAL_REVIEW, cộng thêm tín hiệu khác thì REJECTED |
| CCCD đời cũ không có QR | `LEGACY_CARD` (30 điểm) | MANUAL_REVIEW |

Với AI thật, kết quả **không phụ thuộc 2 số cuối CCCD** như stub. Ảnh nên chụp thẳng, đủ sáng, cạnh ngắn từ 600 px trở lên, tối đa 12 MB, định dạng JPG/PNG. Đừng commit ảnh giấy tờ thật vào repo.

#### Lỗi hay gặp với worker

| Triệu chứng | Cách xử lý |
|---|---|
| `/health` trả `models_loaded: false` | Thiếu file `.onnx` hoặc đường dẫn `-v` sai, xem lại bước 1 |
| Hồ sơ đứng mãi ở AI_PROCESSING, worker không có log | Backend vẫn ở `stub` hoặc `AI_WORKER_URL` sai, xem bước 4, nhớ restart backend |
| Worker log 401/403 khi gọi lại, hoặc backend log 401 trên `/internal/...` | `INTERNAL_TOKEN` hai bên không trùng |
| Worker log từ chối host / tải ảnh lỗi | `PUBLIC_S3_URL` của backend vẫn là `localhost:9000`, hoặc `STORAGE_ALLOWED_HOSTS` không khớp |
| Worker log lỗi kết nối khi gọi lại backend | `BACKEND_URL` của worker sai, hoặc Windows Firewall chặn Java trên cổng 8080 |
| Tắt worker giữa chừng | Hồ sơ không mất. Bật worker lại, rồi admin dùng nút đưa lại hàng đợi (requeue) |

### 6.3. Chuyển qua lại giữa stub và http

| Muốn | Làm |
|---|---|
| Test nhanh luồng nghiệp vụ (duyệt / từ chối theo ý) | `.env`: `AI_WORKER_MODE=stub` (hoặc xóa dòng), không cần chạy worker. `PUBLIC_S3_URL` để `localhost:9000` hay `host.docker.internal:9000` đều được |
| Test AI thật | Bước 3 và 4 ở mục 6.2 |

Mỗi lần đổi `.env` phải **khởi động lại backend**. Hồ sơ nộp lúc đang ở chế độ nào thì được xử lý theo chế độ đó.

## 7. Tắt / bật lại

| Việc | Lệnh |
|---|---|
| Tắt backend / web | `Ctrl + C` trong cửa sổ tương ứng |
| Tắt Docker (giữ dữ liệu) | `docker compose --env-file .env -f deploy/docker-compose.local.yml stop` |
| Bật lại Docker | `... up -d` như bước 2 |
| **Xóa sạch dữ liệu** (DB, ảnh, mail) | `docker compose --env-file .env -f deploy/docker-compose.local.yml down -v` → lần chạy backend sau sẽ seed lại từ đầu |

Thứ tự bật mỗi ngày: **Docker → backend → web**.

## 8. Xem dữ liệu trong DB

```powershell
docker compose --env-file .env -f deploy/docker-compose.local.yml exec postgres psql -U genie -d genie
```

Một số câu hữu ích:

```sql
SELECT version, description FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
SELECT id, name, status FROM products ORDER BY id LIMIT 10;          -- lấy id sản phẩm thật
SELECT user_id, shop_name, kyc_status FROM vendor_profiles;         -- lấy id shop / trạng thái KYC
SELECT action_type, user_email, status, created_at FROM system_logs ORDER BY created_at DESC LIMIT 10;
\q
```

(Hoặc dùng DBeaver / pgAdmin: host `localhost`, port `5433`, db `genie`, user/pass như `.env`.)

## 9. Lỗi hay gặp

| Triệu chứng | Nguyên nhân / cách sửa |
|---|---|
| `java: ... TypeTag :: UNKNOWN` | Đang dùng JDK 24 → gõ lại `$env:JAVA_HOME = ...` (mục 0) |
| `Connection to localhost:5432 refused` | `DB_URL` còn 5432 → sửa thành 5433 |
| `port is already allocated` khi `up -d` | Cổng đã bị app khác chiếm → đổi `POSTGRES_PORT` / dừng container khác (`docker ps`) |
| Không nhận được OTP | SMTP chưa trỏ Mailpit → xem mục 1.1, khởi động lại backend; mở http://localhost:8025 |
| Web vẫn hiện dữ liệu giả ở phần đã có backend | Thiếu cờ trong `PUBLIC_REAL_API` hoặc chưa restart `npm run dev` |
| Ảnh banner/sản phẩm bị vỡ | Chưa chạy `upload-assets.ps1` (mục 3.1) hoặc Silo chưa chạy |
| Lỗi Flyway checksum / migration lạ | `.\mvnw.cmd clean spring-boot:run`; nếu vẫn lỗi và chấp nhận mất dữ liệu: `down -v` rồi chạy lại |
| `401` liên tục trên web | Token hết hạn sau khi reset DB → đăng xuất, xóa cookie localhost:4321, đăng nhập lại |
| Docker Desktop tự tắt | Mở lại Docker Desktop, đợi xanh, `up -d` |
