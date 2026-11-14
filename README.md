# Genie Platform

Sàn thương mại điện tử đa người bán — bản TLCN. Monorepo gồm:

| Thư mục | Nội dung |
|---|---|
| `server-spring/` | Backend — Spring Boot 3.3 / Java 21, modular monolith 11 module |
| `server-python/` | AI Worker — FastAPI: KYC (ONNX + OpenCV) + PhoBERT cảm xúc đánh giá |
| `web/` | Frontend — Astro SSR + React 19 islands + Tailwind |
| `deploy/` | Docker Compose, Nginx, MinIO / Redis / Postgres, sao lưu |
| `scripts/` | Seed, tải mô hình, giả lập webhook, kiểm i18n |
| `docs/` | ADR (`docs/adr/`), OpenAPI xuất ra (`docs/openapi/`) |

Quy ước đầy đủ: `TECH_STACK_VA_QUY_UOC.md`.

## Chạy local trong 15 phút

Yêu cầu: Docker Desktop, JDK 21, Node.js 22, Python 3.12.

```bash
# 0. Công cụ gốc (husky + commitlint)
npm install

# 1. Hạ tầng: postgres, redis, minio (+ minio-init), mailpit
cp deploy/.env.example deploy/.env
docker compose -f deploy/docker-compose.local.yml --env-file deploy/.env up -d

# 2. Backend (Flyway tự tạo bảng, R__seed_demo.sql nạp dữ liệu demo)
cd server-spring && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 3. AI Worker
./scripts/fetch-models.sh
cd server-python && python -m venv .venv && .venv/bin/pip install -e ".[dev]"
.venv/bin/uvicorn app.main:app --port 8000

# 4. Frontend
cd web && npm ci && npm run dev
```

| Dịch vụ | URL |
|---|---|
| Storefront | http://localhost:4321 |
| API | http://localhost:8080/api/v1 · Swagger UI http://localhost:8080/swagger-ui.html |
| MinIO Console | http://localhost:9001 |
| Mailpit (xem email OTP) | http://localhost:8025 |

Webhook đối tác khi dev: `cloudflared tunnel --url http://localhost:8080` hoặc `scripts/webhooks/*.http`.

## Frontend `web/` — chạy riêng ở chế độ mock

Giao diện (storefront, Seller Center, Admin Portal) chạy được **không cần backend**: mọi hàm
`features/*/infrastructure/*.api.ts` trả dữ liệu mẫu khi `PUBLIC_USE_MOCK=true` (mặc định).

```bash
cd web
cp .env.example .env      # tùy chọn — thiếu biến thì dùng giá trị dev mặc định
npm ci
npm run dev               # http://localhost:4321
```

| Biến (`web/.env`) | Mặc định | Ý nghĩa |
|---|---|---|
| `PUBLIC_USE_MOCK` | `true` | `true`: dữ liệu mock trong trình duyệt / SSR; `false`: gọi API thật |
| `PUBLIC_API_URL` | `http://localhost:8080/api/v1` | API gọi từ trình duyệt |
| `INTERNAL_API_URL` | = `PUBLIC_API_URL` | API gọi khi SSR (trong Docker: `http://backend:8080/api/v1`) |
| `PUBLIC_WEB_URL` | `http://localhost:4321` | Tên miền công khai — canonical, sitemap, Open Graph |
| `PUBLIC_S3_URL` | — | Ảnh / tệp trên MinIO |

**Dữ liệu mẫu ở chế độ mock**

- Đăng nhập: `mai.tran@gmail.com` / mật khẩu `Genie@123`, mã OTP luôn là `123456`.
  Tài khoản này vừa là người mua vừa là chủ shop TechZone Official (`/seller`).
- `/admin` đăng nhập bằng Admin mẫu đủ 13 quyền. Muốn xem dưới quyền nhân viên: đặt
  `sessionStorage['genie.mock.staff_title']` = `MODERATOR` | `FINANCE_OFFICER` | `CS_SPECIALIST` rồi tải lại.
- Thay đổi (giỏ, đơn, địa chỉ, duyệt KYC…) giữ trong `sessionStorage` của tab (`genie.mock.*`) — đóng tab là về dữ liệu gốc.
- Theme storefront: `?theme=genie|classic|dongho|dongson|hoian|polymer|buudien|saigon|sleeve`.

**Thư viện giao diện `/dev/ui`** — mọi component `shared/ui` với đủ biến thể (nút, form, overlay,
thẻ sản phẩm, bộ thống kê). Chỉ có ở `npm run dev`; bản build trả 404.

**Lệnh kiểm tra**

| Lệnh | Việc |
|---|---|
| `npm run verify` | lint → `astro check` + `tsc` → Vitest → build → `check-i18n` (cổng trước mỗi commit) |
| `npm run test` | Vitest (`src/**/*.test.ts`) |
| `npm run test:e2e` | Playwright 360 px + 1440 px trên bản build production cổng 4322 (`E2E_DEV=1`: chạy trên dev server; `E2E_PORT`: đổi cổng) |

Danh sách hàm mock cần nối API thật: [`docs/web-backend-handoff.md`](docs/web-backend-handoff.md).
Quyết định kiến trúc của phần chuyển giao diện: [`docs/adr/`](docs/adr/).

## Quy trình nhóm

- Nhánh: `main` (bản phát hành), `develop` (tích hợp), `feature/GNE-<số>-<mo-ta>`, `bugfix/…`, `release/vX.Y.Z`, `hotfix/…`.
- Commit: `<type>(<scope>): <subject> [#GNE-<số>]` — commitlint chặn khi sai.
- PR vào `develop`: CI xanh, ≥ 2 duyệt, squash merge.
- Release: tag `vX.Y.Z` trên `main` → `release.yml` build 3 image → `deploy.yml` triển khai VPS.
