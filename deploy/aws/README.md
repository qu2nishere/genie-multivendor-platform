# Hạ tầng AWS cho production — RDS PostgreSQL + S3 (ADR 0010)

> Máy dev / test **không** dùng AWS: CSDL là PostgreSQL chạy Docker hoặc cài thẳng máy, kho tệp là Silo chạy Docker
> (`deploy/docker-compose.local.yml`). Production cố định **RDS + S3**; app (nginx, frontend, backend, ai-worker, redis)
> vẫn chạy bằng `deploy/docker-compose.yml` trên VPS / EC2. Biến môi trường: `docs/cau-hinh-moi-truong.md` mục 7.

Vùng mặc định: **`ap-southeast-1` (Singapore)** — gần Việt Nam nhất. Đổi vùng thì đổi đồng bộ `STORAGE_REGION`,
`MINIO_ENDPOINT`, `PUBLIC_S3_URL` trong `deploy/.env`, tham số của `s3-init.sh` và CSP trong `deploy/nginx/conf.d/web.conf`.

## 1. RDS PostgreSQL

| Mục | Giá trị |
|---|---|
| Engine | PostgreSQL **16** (khớp `postgres:16-alpine` của máy dev) |
| Lớp máy | `db.t4g.micro` (Free Tier) — nâng khi cần |
| Tên CSDL ban đầu | `genie` |
| Tài khoản | `genie` + mật khẩu mạnh → `DB_USER`, `DB_PASSWORD` |
| Truy cập công khai | **Không**. Security group chỉ mở cổng 5432 cho security group / IP của máy chạy backend |
| SSL | Bắt buộc — `DB_URL` có `?sslmode=require` (parameter group: `rds.force_ssl = 1`) |
| Sao lưu | Automated backups **7 ngày** + snapshot thủ công trước mỗi release (thay `pg_dump` hằng ngày của bản VPS cũ) |
| Múi giờ | Để mặc định UTC — ứng dụng đã ghi `TIMESTAMPTZ` / `jdbc.time_zone: UTC` |

`DB_URL` = `jdbc:postgresql://<endpoint RDS>:5432/genie?sslmode=require`. Bảng do **Flyway** tạo khi backend khởi động
lần đầu (`V1…`); `GENIE_SEED_DEMO=false` — **không bao giờ** nạp dữ liệu mẫu lên RDS production.

## 2. S3

```bash
./deploy/aws/s3-init.sh genie-prod- ap-southeast-1 https://genie.vn,https://www.genie.vn
```

Script tạo 9 bucket `<tiền tố><tên>` (LUU_TRU_DU_LIEU.md mục 5) và cấu hình giống `deploy/silo/init.sh`:

| Cấu hình | Bucket |
|---|---|
| Đọc công khai (trừ `tmp/`) qua bucket policy | `product-media`, `avatar-assets`, `review-media`, `content-assets` |
| Chặn công khai hoàn toàn, chỉ xem qua link ký 5 phút | `chat-media`, `product-docs`, `refund-evidence`, `kyc-dossiers`, `ml-models` |
| Xóa `tmp/` sau 1 ngày (lifecycle) | tất cả |
| CORS cho trình duyệt PUT / GET từ web | tất cả |
| Mã hóa SSE-S3 mặc định | tất cả (ảnh KYC còn được backend mã hóa AES-256-GCM trước khi lưu) |
| Lưu phiên bản (versioning) | `kyc-dossiers` |

Tên bucket S3 là **duy nhất toàn cầu** — trùng thì đổi tiền tố (vd. `genie-tlcn-2026-`) và sửa `STORAGE_BUCKET_PREFIX`.

## 3. IAM user cho ứng dụng

1. IAM → Users → tạo `genie-app` (không cấp quyền console).
2. Gắn policy từ [`iam-policy.json`](iam-policy.json) — **thay `genie-prod-` bằng tiền tố thật** nếu khác.
3. Tạo access key → điền `MINIO_APP_ACCESS_KEY`, `MINIO_APP_SECRET_KEY` trong `deploy/.env` (tên biến giữ `MINIO_*`).
4. Không dùng khóa root của tài khoản AWS. Lộ khóa → vô hiệu hóa ngay ở IAM và tạo khóa mới.

Chạy trên EC2 có thể thay access key bằng IAM role gắn vào máy — khi đó vẫn cần đặt hai biến trên (SDK hiện đọc khóa
tĩnh); chuyển sang role là việc của DATN.

## 4. Kiểm tra sau khi cấu hình

- Backend khởi động với profile `prod`: log Flyway `Successfully applied … migrations`, `/actuator/health` = `UP`.
- Tải ảnh đại diện trên web → tệp xuất hiện ở `genie-prod-avatar-assets/user/…`, ảnh mở được bằng URL
  `https://genie-prod-avatar-assets.s3.ap-southeast-1.amazonaws.com/…`.
- Mở thử một tệp trong `genie-prod-kyc-dossiers` bằng URL không ký → **403** (đúng — bucket riêng tư).

## 5. Chi phí (ước lượng, giá 2026 — kiểm lại trước khi tạo)

RDS `db.t4g.micro` + 20 GB gp3 và S3 vài GB nằm trong Free Tier 12 tháng của tài khoản mới; hết Free Tier khoảng
15–20 USD / tháng cho RDS. Bật AWS Budgets cảnh báo ở 5 USD để không bị bất ngờ.
