# Cấu hình môi trường — `.env` & `application*.yml`

> Áp dụng từ 2026-11-17 (nhánh `feature/env-config-cleanup`). Bổ sung cho TECH_STACK mục 16.6.
> Đọc khi: mới clone repo, muốn đổi CSDL (Docker / cài trên máy), gửi email thật, hoặc chuẩn bị triển khai VPS.

## 1. Nguyên tắc

**Môi trường chạy ([ADR 0010](adr/0010-production-aws-rds-s3.md)):**

| Môi trường | CSDL | Kho tệp | Cấu hình ở |
|---|---|---|---|
| Máy dev / test chạy tay | PostgreSQL 16 **Docker** hoặc **cài thẳng máy** | **Silo** Docker | `.env` gốc repo |
| Test tự động (`*IT`) | Testcontainers PostgreSQL | Testcontainers Silo | `application-test.yml` |
| **Production** | **AWS RDS PostgreSQL 16** | **AWS S3** | `deploy/.env` — tạo hạ tầng theo `deploy/aws/README.md` |

Code chỉ có **một** cài đặt kho tệp (`S3ObjectStorage`) cho cả Silo và S3 — khác nhau ở biến môi trường (mục 6.2).

| # | Nguyên tắc |
|---|---|
| 1 | **Mọi giá trị thay đổi theo môi trường** (địa chỉ, tài khoản, mật khẩu, khóa bí mật, kích thước pool, mức log, bật / tắt seed và cron) **đọc từ biến môi trường** `${TEN_BIEN}`. Không ghi cứng trong `application*.yml`. |
| 2 | **Mật khẩu / khóa thật chỉ nằm trong tệp `.env`** (máy dev: gốc repo; VPS: `deploy/.env`). Hai tệp này bị `.gitignore` chặn. Tệp `*.env.example` và `application*.yml` có trên git → chỉ chứa giá trị mẫu. |
| 3 | **Máy dev chạy được không cần `.env`:** `application-local.yml` có giá trị dự phòng khớp `deploy/docker-compose.local.yml`. `.env` chỉ cần khi muốn khác mặc định. |
| 4 | **Prod an toàn khi thiếu biến:** bí mật (`DB_PASSWORD`, `JWT_SECRET`, …) không có mặc định ở `application.yml` / `application-prod.yml` → thiếu là backend dừng ngay, không lặng lẽ chạy bằng mật khẩu mẫu. |
| 5 | **Hằng số nghiệp vụ theo tài liệu** (access token 15 phút, refresh 7 ngày, TTL Presigned URL 15 / 10 / 5 phút…) giữ trong `application.yml` vì không đổi theo môi trường. |

## 2. Tệp nào làm gì

| Tệp | Có trên git | Vai trò |
|---|:---:|---|
| `server-spring/src/main/resources/application.yml` | ✔ | Cấu hình chung mọi môi trường. Toàn bộ giá trị dạng `${BIEN}` hoặc `${BIEN:mặc_định_không_bí_mật}` |
| `server-spring/src/main/resources/application-local.yml` | ✔ | Profile `local`: **chỉ** giá trị dự phòng cho máy dev (`genie` / `changeme`, Mailpit, khóa dev). Không sửa file này để đổi máy — sửa `.env` |
| `server-spring/src/main/resources/application-prod.yml` | ✔ | Profile `prod` (production AWS): **không** có mặc định cho `DB_URL` (RDS), `MINIO_ENDPOINT` (S3), `STORAGE_BUCKET_PREFIX`, khóa; mặc định vùng `ap-southeast-1`, Redis `redis`, AI Worker `ai-worker` (tên service Compose), SMTP bắt buộc TLS |
| `server-spring/src/test/resources/application-test.yml` | ✔ | Profile `test` cho `*IT` — CSDL / Redis do Testcontainers tạo, không đọc `.env` để chọn DB |
| `.env.example` → `.env` | mẫu ✔ / thật ✘ | Biến của **máy dev** — backend đọc tự động; Docker Compose local đọc qua `--env-file .env` |
| `deploy/.env.example` → `deploy/.env` | mẫu ✔ / thật ✘ | Biến của **production**: RDS, S3, IAM, bí mật — `docker compose` nạp qua `env_file` |
| `web/.env.example` → `web/.env` | mẫu ✔ / thật ✘ | Biến của **web** (Astro): `PUBLIC_USE_MOCK`, `PUBLIC_REAL_API`, `PUBLIC_API_URL`… |
| `server-python/.env` | ✘ | Biến của **AI Worker** (`app/core/config.py`): `INTERNAL_TOKEN`, `BACKEND_URL`, `MINIO_*`, `MODELS_DIR` |

### Backend đọc `.env` thế nào

`common/config/DotEnvEnvironmentPostProcessor` (đăng ký ở `META-INF/spring.factories`):

- Tìm `.env` ở thư mục chạy rồi đi ngược lên tối đa 3 cấp → chạy từ `server-spring/` (IntelliJ, `./mvnw`) vẫn thấy `.env` ở gốc repo.
- Chạy **trước** khi Spring đọc `application*.yml` và chọn profile → dòng `SPRING_PROFILES_ACTIVE=local` trong `.env` có tác dụng, **không cần** đặt profile trong IntelliJ.
- Độ ưu tiên thấp: biến môi trường thật của hệ điều hành, tham số dòng lệnh (`-Dspring-boot.run.profiles=…`), Run Configuration, `@ActiveProfiles` của test đều ghi đè được.
- Không có tệp → bỏ qua (CI, Docker, VPS dùng biến môi trường như cũ).
- Cú pháp: `KHOA=gia_tri` mỗi dòng; bỏ dòng trống, dòng `#`, dòng có giá trị rỗng; bỏ tiền tố `export`; giá trị có thể bọc `"…"` / `'…'`. Trùng tên: **dòng viết sau thắng**.

**Thứ tự ưu tiên** (cao → thấp): tham số dòng lệnh → biến môi trường hệ điều hành / Run Configuration → `.env` → `application-<profile>.yml` → `application.yml`.

## 3. Profile Spring — có mấy loại, chọn thế nào

Profile quyết định Spring nạp thêm file `application-<profile>.yml` nào **chồng lên** `application.yml`. Repo có **3 profile**:

| Profile | File | Dùng khi | Ai / cái gì bật nó | Đặc điểm |
|---|---|---|---|---|
| **`local`** | `application-local.yml` | Chạy backend trên máy dev (IntelliJ, `./mvnw`) | Dòng `SPRING_PROFILES_ACTIVE=local` trong `.env` gốc | Có mặc định cho **mọi** biến (khớp Docker local), log `DEBUG`, nạp dữ liệu mẫu, cookie không cần HTTPS, chi tiết `/actuator/health` |
| **`prod`** | `application-prod.yml` | Production: app chạy `deploy/docker-compose.yml` trên VPS / EC2, CSDL **RDS**, kho tệp **S3** | `docker-compose.yml` tự đặt `SPRING_PROFILES_ACTIVE: prod` | **Không** có mặc định cho bí mật (thiếu → dừng), host là tên service Docker, pool 30, SMTP bắt buộc TLS, không seed |
| **`test`** | `src/test/resources/application-test.yml` | `./mvnw test` / `verify` | Test tự bật bằng `@ActiveProfiles("test")` (`IntegrationTestBase`) — **không** đặt tay | CSDL / Redis do Testcontainers tạo, `FakeEmailSender`, tắt cron, khóa cố định |

- **Không đặt profile nào** → chỉ có `application.yml` → thiếu `DB_URL`, `JWT_SECRET`… → backend dừng với lỗi `Could not resolve placeholder`. Đây là chủ ý (an toàn), không phải hỏng.
- **`demo`**: TECH_STACK 16.1 có nhắc "profile `local` / `demo`" nhưng repo **chưa có** `application-demo.yml`. Demo dùng `prod` (production RDS + S3) hoặc chạy `local` trên máy.
- Có thể bật **nhiều profile** cách nhau dấu phẩy, ví dụ `local,debug-sql` — file sau ghi đè file trước. Hiện chưa cần.

### Cách chọn profile (cách trên ưu tiên hơn cách dưới)

| # | Cách | Ví dụ | Khi nào |
|---|---|---|---|
| 1 | Tham số dòng lệnh | `./mvnw spring-boot:run -Dspring-boot.run.profiles=prod` · `java -jar genie-backend.jar --spring.profiles.active=prod` | Thử nhanh một profile khác mà không sửa `.env` |
| 2 | IntelliJ → Run Configuration → **Active profiles** | `local` | Thích chọn trong IDE; để trống thì lấy từ `.env` |
| 3 | Biến môi trường hệ điều hành / Docker | `SPRING_PROFILES_ACTIVE=prod` (Compose đặt sẵn) | VPS, CI |
| 4 | Tệp `.env` gốc repo | `SPRING_PROFILES_ACTIVE=local` | **Mặc định cho máy dev** — `.env.example` đã có sẵn dòng này |

Test (`@ActiveProfiles("test")`) luôn thắng `.env`, nên `.env` của bạn ghi `local` cũng không làm test chạy nhầm cấu hình.

### Kiểm tra đang chạy profile nào

Dòng log đầu khi khởi động: `The following 1 profile is active: "local"`. Không thấy dòng này mà thấy `No active profile set, falling back to 1 default profile: "default"` → chưa đặt profile (xem cách 1–4).

### Lưu ý

- **Đừng chạy `prod` trên máy dev** trừ khi đã đặt đủ biến bí mật — profile `prod` không có mặc định nên sẽ dừng; và nếu trỏ vào CSDL thật thì mọi cron sẽ chạy thật.
- Đổi profile **không** đổi CSDL: CSDL do khối `DB_URL` trong `.env` quyết định (mục 5). Máy dev chỉ trỏ CSDL local (Docker / cài máy); CSDL production (RDS) chỉ khai trong `deploy/.env`.

## 4. Bắt đầu trên máy mới

```bash
cp .env.example .env                 # gốc repo — đã bật sẵn SPRING_PROFILES_ACTIVE=local + khối CSDL Docker
docker compose --env-file .env -f deploy/docker-compose.local.yml up -d   # postgres, redis, silo, mailpit (genie / changeme)
cd server-spring && ./mvnw spring-boot:run                   # profile lấy từ .env — không cần -Dspring-boot.run.profiles
```

- Cần **JDK 21**. JDK mới hơn (24, 25) làm Lombok lỗi `TypeTag :: UNKNOWN` khi biên dịch → đặt `JAVA_HOME` về JDK 21 hoặc chọn SDK 21 trong IntelliJ.
- **Luôn chạy compose với `--env-file .env`:** Docker Compose mặc định chỉ đọc `.env` cạnh file compose (`deploy/.env`), **không** đọc `.env` gốc repo. Có `--env-file .env` thì Docker và backend dùng **cùng một tệp** — đổi mật khẩu / cổng ở một chỗ là cả hai khớp. Đổi `DB_PASSWORD` sau khi Postgres đã tạo dữ liệu thì phải xóa volume `pgdata` (Postgres chỉ đặt mật khẩu lần tạo đầu tiên).

### Đổi cổng khi bị chiếm

Cổng mà Docker mở trên máy dev đọc từ biến, mặc định như bảng dưới. **Không đặt gì thì giữ nguyên.** Báo lỗi
`port is already allocated` / `Only one usage of each socket address` (ví dụ máy đã cài PostgreSQL thật chiếm 5432) thì
thêm biến vào `.env` rồi sửa biến của backend cho **cùng cổng**:

| Dịch vụ | Biến cổng Docker | Mặc định | Sửa thêm cho backend / trình duyệt |
|---|---|---|---|
| PostgreSQL | `POSTGRES_PORT` | 5432 | `DB_URL=jdbc:postgresql://localhost:<cổng>/genie` |
| Redis | `REDIS_PORT` | 6379 | Không — backend dùng chung biến `REDIS_PORT` |
| Silo (API S3) | `SILO_PORT` | 9000 | `MINIO_ENDPOINT` và `PUBLIC_S3_URL` = `http://localhost:<cổng>` (web: `PUBLIC_S3_URL` trong `web/.env`) |
| Silo console | `SILO_CONSOLE_PORT` | 9001 | Không — chỉ để mở http://localhost:<cổng> |
| Mailpit SMTP | `MAILPIT_SMTP_PORT` | 1025 | `SMTP_PORT=<cổng>` (khi gửi email vào Mailpit) |
| Mailpit giao diện | `MAILPIT_UI_PORT` | 8025 | Không — mở http://localhost:<cổng> xem email OTP |

Ví dụ máy đã có PostgreSQL thật ở 5432, cho Docker dùng 5433:

```properties
POSTGRES_PORT=5433
DB_URL=jdbc:postgresql://localhost:5433/genie
```

Rồi chạy lại `docker compose --env-file .env -f deploy/docker-compose.local.yml up -d`. Cổng **trong** container (số bên
phải dấu `:` trong file compose) không đổi. Kiểm tra cổng đang bị ai chiếm: `netstat -ano | findstr :5432` (Windows) /
`lsof -i :5432` (macOS, Linux).


## 5. Chọn CSDL máy dev — 2 cách

Trong `.env` gốc repo có 2 khối. **Chỉ bật một khối**, comment khối còn lại, chạy lại backend. CSDL production (AWS RDS) **không** có ở đây — nó nằm trong `deploy/.env` trên máy chủ (mục 7).

| Khối | Khi nào dùng | `DB_URL` | Lưu ý |
|---|---|---|---|
| **[1] Docker** (mặc định) | Bình thường | `jdbc:postgresql://localhost:5432/genie` | `genie` / `changeme` — khớp `docker-compose.local.yml`. Để comment cả khối cũng chạy |
| **[2] PostgreSQL cài trên máy** | Không muốn dùng Docker | `jdbc:postgresql://localhost:5433/genie` | Tạo DB trước: `CREATE DATABASE genie;`. Nếu chạy song song Docker thì một bên phải đổi cổng (ví dụ 5433). Kho tệp **vẫn là Silo Docker**: `docker compose --env-file .env -f deploy/docker-compose.local.yml up -d redis silo silo-init mailpit` |

Mọi cách đều để **Flyway tự tạo bảng** (`V1…`) lần chạy đầu — DB phải trống hoặc đúng lịch sử Flyway của repo.

**Không** trỏ máy dev vào **RDS production**: profile `local` nạp dữ liệu mẫu và chạy cron thật lên dữ liệu thật; script seed chỉ chạy với host local.

## 6. Danh mục biến

Cột **Mặc định dev** = giá trị khi không đặt biến và chạy profile `local`. `—` = không có mặc định (bắt buộc ở prod).

### 6.1 Profile & CSDL

| Biến | Mặc định dev | Ý nghĩa |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | — (ghi `local` trong `.env`) | `local` máy dev · `prod` VPS · `test` do test tự đặt — xem mục 3 |
| `DB_URL` | `jdbc:postgresql://localhost:5432/genie` | Chuỗi kết nối JDBC — prod: endpoint **RDS** + `?sslmode=require` |
| `DB_USER` / `DB_PASSWORD` | `genie` / `changeme` | Tài khoản CSDL |
| `DB_POOL_SIZE` | `20` (prod `30`) | Số kết nối tối đa HikariCP |
| `GENIE_SEED_DEMO` | `true` (prod `false`) | Nạp dữ liệu mẫu: `R__seed_demo.sql` (tài khoản) **và** thư mục `db/seed` (danh mục, shop, sản phẩm, voucher, banner… — `SeedDataFlywayCustomizer`). Nạp tay vào CSDL local: `scripts/seed/seed.sh` / `seed.ps1` ([scripts/seed/README.md](../scripts/seed/README.md)) |
| `FLYWAY_ENABLED` | `true` | `false` = không chạy migration |
| `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD_HASH` | `admin@genie.local` / rỗng | Admin đầu tiên (`V13__bootstrap_admin.sql`); băm bằng `python3 scripts/hash-password.py '<mật khẩu>'` |
| `STAFF_EMAIL_DOMAINS` | rỗng (prod `genie.vn`) | Tên miền email được phép khi mời nhân viên (UC-ADM-01, plan B1 D3), cách nhau dấu phẩy; rỗng = mọi email hợp lệ. Sai tên miền → `422 GNE-ADM-4221`. Đọc trực tiếp bằng `@Value` (không khai trong `application*.yml`) |

### 6.2 Redis, Silo, địa chỉ

| Biến | Mặc định dev | Ý nghĩa |
|---|---|---|
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | `localhost` / `6379` / `changeme` | Redis (prod: host `redis`) |
| `MINIO_ENDPOINT` | `http://localhost:9000` (prod: **bắt buộc**, `https://s3.ap-southeast-1.amazonaws.com`) | Backend gọi kho tệp — Silo ở dev, AWS S3 ở production. Tên `MINIO_*` giữ nguyên (ADR 0009, 0010) |
| `PUBLIC_S3_URL` | `http://localhost:9000` (prod: mặc định = `MINIO_ENDPOINT`) | Địa chỉ trình duyệt tải / xem tệp; host `*.amazonaws.com` → URL ảnh dạng `https://<bucket>.s3.<vùng>.amazonaws.com/<key>` |
| `MINIO_APP_ACCESS_KEY` / `MINIO_APP_SECRET_KEY` | `genie-app` / `changeme` | Dev: tài khoản ứng dụng do `silo-init` tạo; prod: khóa **IAM user** theo `deploy/aws/iam-policy.json` (không dùng khóa root AWS) |
| `STORAGE_REGION` | `us-east-1` (prod `ap-southeast-1`) | Vùng ký S3 — Silo bỏ qua, AWS phải đúng vùng của bucket |
| `STORAGE_BUCKET_PREFIX` | trống (prod: **bắt buộc**, vd. `genie-prod-`) | Tên bucket thật = tiền tố + tên logic (`genie-prod-avatar-assets`) — tên bucket S3 là duy nhất toàn cầu; khớp tham số `deploy/aws/s3-init.sh` |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | `siloadmin` / `changeme` | Chỉ cho container Silo máy dev; production không dùng |
| `SERVER_PORT` | `8080` | Cổng backend |
| `PUBLIC_WEB_URL` | `http://localhost:4321` | Địa chỉ web (link trong email, redirect OAuth) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4321` | Origin được gọi API kèm cookie, nhiều origin cách dấu phẩy |
| `AI_WORKER_URL` | `http://localhost:8000` (prod `http://ai-worker:8000`) | AI Worker |
| `AI_WORKER_MODE` | `stub` (profile local) / `http` (prod, mặc định `application.yml`) | `stub` = giả lập kết quả KYC theo 2 số cuối CCCD (`00–29` tự duyệt, `30–69` nhân viên duyệt, `70–99` từ chối), không cần AI Worker; `http` = gọi AI Worker thật (ADR 0011) |
| `BACKEND_INTERNAL_URL` | `http://localhost:8080` (prod `http://backend:8080`) | Địa chỉ backend AI Worker gọi lại `PATCH /api/v1/internal/kyc/{id}/result` |

### 6.3 Bảo mật

| Biến | Mặc định dev | Ý nghĩa |
|---|---|---|
| `JWT_SECRET` | khóa dev cố định | Ký JWT HS256 — prod ≥ 256 bit ngẫu nhiên |
| `OTP_PEPPER` | khóa dev | Trộn vào bản băm OTP / token tạm trong Redis |
| `INTERNAL_TOKEN` | `local-internal-token` | Header nội bộ backend ↔ AI Worker — **phải trùng** `server-python/.env` |
| `KYC_AES_KEY` | khóa dev | Mã hóa ảnh KYC AES-256-GCM (prod: base64 32 byte; mất khóa = không đọc lại được hồ sơ) |
| `WALLET_HMAC_SECRET` | khóa dev | HMAC `balanceHash` ví shop |
| `AUTH_COOKIE_SECURE` | `false` (prod `true`) | Cookie refresh token chỉ gửi qua HTTPS |

### 6.4 Email

| Biến | Mặc định dev | Ý nghĩa |
|---|---|---|
| `SMTP_HOST` / `SMTP_PORT` | `localhost` / `1025` (Mailpit) | Máy chủ SMTP |
| `SMTP_USER` / `SMTP_PASSWORD` | rỗng | Gmail: `SMTP_PASSWORD` là **App Password 16 ký tự**, không phải mật khẩu đăng nhập |
| `SMTP_AUTH` / `SMTP_STARTTLS` | `false` (prod `true`) | Gmail cần cả hai `true`, cổng 587 |
| `MAIL_FROM` | `no-reply@genie.local` | Gửi qua Gmail thì phải trùng `SMTP_USER` |

Để trống `SMTP_*` → email vào Mailpit, xem ở http://localhost:8025.

### 6.5 Đăng nhập mạng xã hội & tích hợp bên thứ ba

Để trống = tính năng tắt hoặc dùng bản giả lập.

| Nhóm | Biến |
|---|---|
| Google | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` (redirect URI `http://localhost:4321/auth/oauth-callback`) |
| Zalo | `ZALO_APP_ID`, `ZALO_APP_SECRET` |
| Thanh toán | `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_URL`, `VNPAY_RETURN_URL`, `SEPAY_API_KEY`, `SEPAY_ACCOUNT_NUMBER` |
| Vận chuyển | `GHN_TOKEN`, `GHN_SHOP_ID`, `GHTK_TOKEN`, `VTP_TOKEN`, `JT_KEY` |
| AI | `GEMINI_API_KEY` |

### 6.6 Vận hành

| Biến | Mặc định dev | Ý nghĩa |
|---|---|---|
| `LOG_LEVEL_GENIE` | `DEBUG` (prod `INFO`) | Mức log package `com.genie` |
| `LOG_LEVEL_ROOT` | `INFO` | Mức log chung |
| `GENIE_SCHEDULING_ENABLED` | `true` | `false` = tắt mọi `@Scheduled` (test luôn tắt) |
| `HEALTH_SHOW_DETAILS` | `always` (prod `when-authorized`) | Chi tiết `/actuator/health` |
| `VIRTUAL_THREADS_ENABLED` | `false` | Bật virtual thread Java 21 |
| `TRACING_SAMPLING` | `1.0` | Tỷ lệ lấy mẫu tracing |

### 6.7 Biến sẽ thêm theo plan 2026-11-16 (chưa có trong code)

Ghi trước để không ai đặt tên khác. Khi làm task tương ứng phải thêm vào `.env.example`, `deploy/.env.example` và bảng trên.

| Biến | Plan / task | Mặc định | Ý nghĩa |
|---|---|---|---|
| ~~`AI_WORKER_MODE`~~ | Mở shop & KYC — D7, K05.05 | — | **Đã có** — xem mục 6.2 |
| ~~`server-python/.env`~~ | AI Worker KYC — Y01.01 | — | **Đã có** — xem mục 6.8 |
| `E2E_REAL_API` (web, Playwright) | Mở shop & KYC — K11.09 | không đặt | `1` = chạy E2E với backend thật |

`KYC_AES_KEY` mặc định dev (`application-local.yml`) đã là khóa base64 32 byte hợp lệ — sẵn cho task K02.01 kiểm độ dài khóa.

## 7. Production — AWS RDS + S3 (profile `prod`)

1. Tạo hạ tầng AWS **một lần** theo [deploy/aws/README.md](../deploy/aws/README.md): RDS PostgreSQL 16, 9 bucket S3
   (`deploy/aws/s3-init.sh <tiền tố> <vùng> <origin web>`), IAM user cho ứng dụng (`deploy/aws/iam-policy.json`).
2. Trên VPS / EC2 chạy app:

```bash
cd deploy && cp .env.example .env && chmod 600 .env     # điền DB_URL (RDS), MINIO_* (S3 + IAM), STORAGE_*, bí mật
docker compose up -d                                     # nginx, frontend, backend, ai-worker, redis
```

- `docker-compose.yml` **không** còn container `postgres`, `silo`: CSDL là RDS, tệp nằm trên S3. Compose tự đặt
  `SPRING_PROFILES_ACTIVE=prod`, `REDIS_HOST=redis`, `AI_WORKER_URL=http://ai-worker:8000`.
- Redis, AI Worker **không mở cổng ra ngoài**; RDS chỉ cho security group của máy chạy backend vào cổng 5432.
- `GENIE_SEED_DEMO=false` — production không bao giờ có dữ liệu mẫu.
- Sao lưu: RDS automated backups 7 ngày + snapshot trước release; `deploy/backup/backup.sh` thêm bản `pg_dump`.
- Đổi vùng AWS → đổi đồng bộ `STORAGE_REGION`, `MINIO_ENDPOINT`, `PUBLIC_S3_URL`, tham số `s3-init.sh`, CSP trong
  `deploy/nginx/conf.d/web.conf`.
- Danh sách biến: `deploy/.env.example`, TECH_STACK mục 16.6.

## 8. Bảo mật — điều cần nhớ

- `genie` / `changeme` trên git **không phải lộ bí mật**: đó là mật khẩu mẫu dùng chung cho máy dev. Chỉ nguy hiểm nếu đem lên VPS mà không đổi.
- `.env` của từng người chứa khóa thật (Gmail App Password, Google client secret…) → **không** gửi qua chat, không dán vào issue / PR, không commit (`.gitignore` đã chặn `*.env` / `.env`).
- Máy dev ngồi mạng công cộng: `docker-compose.local.yml` mở cổng 5432 / 6379 / 9000 trên mọi card mạng — người cùng wifi có thể kết nối bằng mật khẩu mẫu. Dữ liệu chỉ là dữ liệu mẫu nên rủi ro thấp; muốn chặn thì đổi thành `"127.0.0.1:5432:5432"`.
- Lộ khóa thật: thu hồi ngay ở nơi cấp (Google Cloud, Gmail App Password, VNPay…), tạo khóa mới, cập nhật `.env`.

## 9. Lỗi thường gặp

| Hiện tượng | Nguyên nhân | Cách sửa |
|---|---|---|
| `Could not resolve placeholder 'DB_URL'` / `'JWT_SECRET'` | Chạy không có profile `local` (mục 3) | Thêm `SPRING_PROFILES_ACTIVE=local` vào `.env` (hoặc `-Dspring-boot.run.profiles=local`) |
| Production: trình duyệt báo lỗi CORS khi tải ảnh lên | Bucket S3 chưa có CORS cho origin web | Chạy lại `deploy/aws/s3-init.sh` với đúng origin |
| Production: ảnh trả `403 AccessDenied` | Bucket công khai chưa có bucket policy, hoặc đang xem tệp trong `tmp/` / bucket riêng tư bằng URL không ký | Chạy lại `s3-init.sh`; tệp riêng tư phải xem qua link ký |
| `port is already allocated` khi `docker compose up` | Cổng đã có chương trình khác dùng (thường PostgreSQL cài sẵn) | Đổi cổng qua `.env` — mục 4 *Đổi cổng khi bị chiếm* |
| Log lặp `HikariPool-1 - Starting...` rồi lỗi kết nối | CSDL chưa chạy / sai cổng | Bật Docker Desktop + `docker compose … up -d`, hoặc kiểm khối CSDL đang bật trong `.env` |
| `password authentication failed for user "genie"` | Mật khẩu `.env` khác mật khẩu lúc tạo volume Postgres | Sửa `.env` cho khớp, hoặc xóa volume `pgdata` để tạo lại (mất dữ liệu local) |
| `TypeTag :: UNKNOWN` khi `./mvnw` | Đang dùng JDK > 21 | Đặt `JAVA_HOME` về JDK 21 |
| OTP không tới Gmail | Thiếu / sai `SMTP_*`, dùng mật khẩu Gmail thay App Password, `MAIL_FROM` khác `SMTP_USER` | Xem mục 6.4; muốn test nhanh thì để trống `SMTP_*` và xem Mailpit |
| AI Worker trả `401` | `INTERNAL_TOKEN` hai bên khác nhau / `server-python/.env` chưa có | Đặt cùng một giá trị ở `.env` gốc và `server-python/.env` |

### 6.8 AI Worker (`server-python/.env` — mẫu `server-python/.env.example`)

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `INTERNAL_TOKEN` | trống (dev: `local-internal-token`) | **Trùng** `INTERNAL_TOKEN` của backend — header `X-Internal-Token` hai chiều |
| `BACKEND_URL` | `http://localhost:8080` (prod `http://backend:8080`) | Worker chỉ gọi lại đúng địa chỉ này (bỏ qua host trong `callback_url` — chống SSRF) |
| `MODELS_DIR` | `models` | Chứa `face_detection_yunet_2023mar.onnx`, `face_recognition_sface_2021dec.onnx` (`scripts/fetch-models.sh`) |
| `STORAGE_ALLOWED_HOSTS` | `localhost:9000` (prod `*.s3.ap-southeast-1.amazonaws.com`) | Host được tải ảnh KYC, cách nhau dấu phẩy |
| `KYC_FACE_MATCH_THRESHOLD` / `KYC_FACE_LOW_THRESHOLD` | `0.363` / `0.25` | Ngưỡng cosine SFace: khớp / dưới mức này `FACE_SCORE_LOW`, ở giữa `FACE_SCORE_BORDERLINE` |
| `KYC_LIVENESS_THRESHOLD` | `0.5` | `liveness_score` dưới ngưỡng → `LIVENESS_SUSPECT` |
| `KYC_MIN_IMAGE_SIDE` / `KYC_BLUR_THRESHOLD` | `600` / `60` | Ảnh giấy tờ nhỏ hơn / mờ hơn → `UNREADABLE_IMAGE` |
| `KYC_DOWNLOAD_MAX_BYTES` / `KYC_DOWNLOAD_TIMEOUT_S` | `12582912` / `5` | Giới hạn tải một ảnh |
| `KYC_MAX_CONCURRENCY` / `KYC_PIPELINE_TIMEOUT_S` | `1` / `20` | Số hồ sơ xử lý cùng lúc; quá thời gian → gửi phần đã có + `PIPELINE_TIMEOUT` |
| `CALLBACK_RETRIES` | `3` | Số lần thử lại callback (1 s / 2 s / 4 s) khi lỗi mạng / 5xx |
