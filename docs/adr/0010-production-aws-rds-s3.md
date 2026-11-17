# 0010 — Production cố định AWS RDS + AWS S3; máy dev / test dùng PostgreSQL (Docker hoặc cài máy) + Silo

- **Trạng thái:** Chấp nhận (2026-11-17)
- **Phạm vi:** `server-spring` (`common/storage`, `application*.yml`), `server-python/app/core/config.py`, `deploy/`
  (compose VPS, nginx, sao lưu, thư mục mới `deploy/aws/`), `.env.example`, `docs/cau-hinh-moi-truong.md`, plan seed
- **Liên quan:** [0009](0009-kho-tep-silo-thay-image-minio.md) (Silo cho máy dev — vẫn giữ)

## Bối cảnh

Bản VPS cũ tự chạy PostgreSQL và Silo trong Docker trên một máy 8 GB: tự lo sao lưu (`pg_dump`, `mcli mirror`),
vá lỗi, dung lượng ổ, và dữ liệu KYC nằm chung máy với ứng dụng. Nhóm muốn production ổn định, có sao lưu tự động và
không phụ thuộc một máy, trong khi máy dev vẫn chạy được không cần tài khoản đám mây.

## Quyết định

| Môi trường | CSDL | Kho tệp | Cách bật |
|---|---|---|---|
| Máy dev / test chạy tay | PostgreSQL 16 **Docker** (mặc định) hoặc **cài thẳng máy** | **Silo** Docker | `.env` gốc — khối CSDL [1] / [2]; `deploy/docker-compose.local.yml` |
| Test tự động (`*IT`) | Testcontainers PostgreSQL | Testcontainers Silo | Không đổi |
| **Production** | **AWS RDS PostgreSQL 16** | **AWS S3** | `deploy/.env` + `deploy/aws/` |

- **Một bộ code cho cả Silo và S3:** cả hai cùng giao thức S3, SDK `io.minio` dùng được cho cả hai. Lớp cài đặt đổi tên
  `SiloObjectStorage` → **`S3ObjectStorage`** và thêm:
  - `genie.storage.bucket-prefix` (`STORAGE_BUCKET_PREFIX`): tên bucket S3 là duy nhất toàn cầu → tên thật =
    tiền tố + tên logic (`genie-prod-avatar-assets`). Mã nghiệp vụ vẫn dùng tên logic (`UploadPurpose.bucket()`).
    Máy dev để trống → tên như cũ.
  - `genie.storage.region` (`STORAGE_REGION`): AWS mặc định `ap-southeast-1` (Singapore).
  - URL công khai: host `*.amazonaws.com` → kiểu virtual-host `https://<bucket>.s3.<vùng>.amazonaws.com/<key>`;
    Silo giữ `<public-url>/<bucket>/<key>`.
- **Giữ tên biến `MINIO_*`** (`MINIO_ENDPOINT`, `MINIO_APP_ACCESS_KEY`, `MINIO_APP_SECRET_KEY`) như ADR 0009 để không
  đổi code / tài liệu hàng loạt — ở production chúng chứa endpoint S3 và khóa IAM.
- **`application-prod.yml` không có mặc định** cho `MINIO_ENDPOINT`, `STORAGE_BUCKET_PREFIX`, `DB_URL`, khóa → thiếu là
  backend dừng, không lặng lẽ trỏ về Silo / container.
- **Compose VPS bỏ** `postgres`, `silo`, `silo-init`, volume `pgdata`, `silodata`, nginx `s3.genie.vn`
  (`conf.d/s3.conf`), `deploy/postgres/postgresql.conf`. Redis vẫn là container. CSP cho phép `*.s3.ap-southeast-1.amazonaws.com`.
- **Khởi tạo S3** bằng `deploy/aws/s3-init.sh` (tương đương `silo/init.sh`): 9 bucket, đọc công khai trừ `tmp/` cho 4
  bucket, chặn công khai 5 bucket, lifecycle `tmp/` 1 ngày, CORS cho PUT từ trình duyệt, SSE-S3, versioning `kyc-dossiers`.
  Quyền ứng dụng: IAM user theo `deploy/aws/iam-policy.json` (không dùng khóa root).
- **Sao lưu:** RDS automated backups 7 ngày + snapshot trước release; `backup.sh` chỉ còn `pg_dump` logic từ RDS
  (qua image `postgres:16-alpine`), bỏ `backup.sh silo`.
- **Seed không bao giờ chạy trên production:** `GENIE_SEED_DEMO=false`; script seed chỉ chạy với host local và từ chối
  mọi host khác (plan seed N10).
- **`.env` máy dev chỉ có CSDL local** (khối [1] Docker, [2] cài máy) — bỏ khối "CSDL cloud dùng chung"; CSDL production
  chỉ khai trong `deploy/.env` trên máy chủ.

## Hệ quả

- Có chi phí AWS (Free Tier 12 tháng cho RDS `db.t4g.micro` + S3 vài GB; sau đó ~15–20 USD / tháng) — bật AWS Budgets.
- Không cần CDN ở bản TLCN; CloudFront trước S3 và IAM role thay khóa tĩnh là việc của DATN.
- Đổi vùng AWS phải đổi đồng bộ: `STORAGE_REGION`, `MINIO_ENDPOINT`, `PUBLIC_S3_URL`, tham số `s3-init.sh`, CSP nginx.
- Tài liệu TLCN (TECH_STACK 16, LUU_TRU 5) cập nhật theo; sơ đồ triển khai trong Document_Vi / HLD còn vẽ Silo +
  PostgreSQL trên VPS — sửa ở đợt đồng bộ tài liệu kế tiếp.
- ADR hợp đồng KYC Backend ↔ AI Worker mà plan 2026-11-16 định tạo dời sang số **0011**.
