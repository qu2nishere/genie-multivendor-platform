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

## Quy trình nhóm

- Nhánh: `main` (bản phát hành), `develop` (tích hợp), `feature/GNE-<số>-<mo-ta>`, `bugfix/…`, `release/vX.Y.Z`, `hotfix/…`.
- Commit: `<type>(<scope>): <subject> [#GNE-<số>]` — commitlint chặn khi sai.
- PR vào `develop`: CI xanh, ≥ 2 duyệt, squash merge.
- Release: tag `vX.Y.Z` trên `main` → `release.yml` build 3 image → `deploy.yml` triển khai VPS.
