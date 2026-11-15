# 0009 — Kho tệp: thay MinIO bằng Silo

- **Trạng thái:** Chấp nhận (2026-11-15)
- **Phạm vi:** `deploy/` (compose local + VPS, `minio/init.sh`, `backup/backup.sh`), test tích hợp `server-spring`

## Bối cảnh

MinIO Inc. ngừng phân phối bản cộng đồng: repo GitHub `minio/minio` bị lưu trữ (04/2026), image `minio/minio` và
`minio/mc` bị xóa khỏi Docker Hub (09/2026), `quay.io/minio/*` chặn tải ẩn danh. Hậu quả với Genie:

- máy chưa có sẵn image (máy thành viên mới, runner CI, VPS mới) không chạy được `minio` / `minio-init`;
- Testcontainers không kéo được image → `IntegrationTestBase` lỗi khởi tạo → toàn bộ test `*IT` đỏ trên CI (PR #53);
- không còn bản vá bảo mật cho MinIO bản cộng đồng.

## Quyết định

- Dùng **`docker.io/pgsty/silo:RELEASE.2026-10-26T00-00-00Z`** (Silo — fork cộng đồng của MinIO do Pigsty duy trì,
  AGPL-3.0, có tag theo phiên bản, bản amd64 + arm64) cho cả dịch vụ kho tệp và dịch vụ khởi tạo bucket, ở compose local, compose
  VPS và Testcontainers (`MinIOContainer` + `asCompatibleSubstituteFor("minio/minio")`).
- **Bỏ hẳn MinIO khỏi Docker**: dịch vụ `minio` → `silo`, `minio-init` → `silo-init`, volume `miniodata` → `silodata`,
  script `deploy/minio/init.sh` → `deploy/silo/init.sh`, nginx `s3.conf` trỏ `silo:9000`, `backup.sh silo`,
  tài khoản root mặc định `siloadmin`.
- Silo giữ nguyên API S3 / admin, đường dẫn `/minio/*`, biến `MINIO_*`, định dạng dữ liệu `.minio.sys` → code Java chỉ
  đổi tên: `MinioObjectStorage` → `SiloObjectStorage` (2026-11-15; vẫn gọi qua SDK S3 `io.minio` vì Silo giữ API S3),
  không đổi logic và cấu hình `genie.storage.*`. Tên lớp thư viện (`MinioClient`, Testcontainers `MinIOContainer`) và
  mã lỗi `GNE-MINIO-5003` (API_SPEC) giữ nguyên.
  Các biến `MINIO_ROOT_*` (Silo bắt buộc) và `MINIO_ENDPOINT`, `MINIO_APP_*` (ứng dụng đọc trong `application*.yml`)
  giữ tên. VPS: `application-prod.yml` ghi cứng `genie.storage.endpoint: http://minio:9000` → compose đặt
  `GENIE_STORAGE_ENDPOINT=http://silo:9000` để ghi đè (không sửa yml).
- Client đổi tên thành `mcli` (vẫn có tên `mc` để tương thích): `init.sh`, `backup.sh` dùng
  `$(command -v mcli || command -v mc)`. `minio-init` dùng chính image Silo (có `/bin/sh` + `mcli`), bỏ image `mc` riêng.
- Healthcheck: `silo healthcheck ready || mcli ready local || mc ready local`.

## Phương án đã cân nhắc

| Phương án | Lý do không chọn |
|---|---|
| Image MinIO do Chainguard build (`cgr.dev/chainguard/minio`) | Gói miễn phí chỉ có `latest` (digest đổi liên tục), chạy user 65532 phải `chown` volume, không có `curl`/`wget` cho healthcheck |
| Tự build MinIO từ mã nguồn, đẩy lên GHCR | Thêm việc build Go + quản lý registry; vẫn không có bản vá mới |
| Đổi hẳn sang RustFS / Garage | Khác API admin / lifecycle, phải viết lại `init.sh`; để dành nếu Silo ngừng duy trì |

## Hệ quả

- Bất kỳ máy nào cũng `docker compose up` được; CI chạy lại được test MinIO.
- Phụ thuộc vào một bên thứ ba (Pigsty). Ghim theo tag; khi nâng cấp đọc release note Silo. Nếu Silo ngừng, nhờ cổng
  `ObjectStorage` (giao thức S3) chỉ cần đổi image + `init.sh`.
- Volume dữ liệu đổi tên: máy / VPS nào đã có dữ liệu trong `miniodata` phải chép sang `silodata` (hiện chưa deploy thật nên không ảnh hưởng).
- Lệnh dev: `docker compose -f deploy/docker-compose.local.yml up -d redis silo silo-init mailpit`; console http://localhost:9001 (`siloadmin` / `changeme`).
