#!/bin/sh
# Container chạy một lần: tạo 9 bucket, chính sách công khai, lifecycle tmp/ 1 ngày,
# tài khoản ứng dụng riêng (không dùng root) — TECH_STACK mục 16.5, LUU_TRU_DU_LIEU.md mục 5
set -eu

mc alias set genie http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"

PUBLIC_BUCKETS="product-media avatar-assets review-media content-assets"
PRIVATE_BUCKETS="chat-media product-docs refund-evidence kyc-dossiers ml-models"

for b in $PUBLIC_BUCKETS $PRIVATE_BUCKETS; do
  mc mb --ignore-existing "genie/$b"
  # Xóa tệp tạm tmp/ sau 1 ngày
  mc ilm rule add --prefix "tmp/" --expire-days 1 "genie/$b" 2>/dev/null || true
done

for b in $PUBLIC_BUCKETS; do
  mc anonymous set download "genie/$b"
done

# Tài khoản ứng dụng (backend / ai-worker)
mc admin user add genie "$MINIO_APP_ACCESS_KEY" "$MINIO_APP_SECRET_KEY" || true
mc admin policy attach genie readwrite --user "$MINIO_APP_ACCESS_KEY" || true

echo "MinIO đã khởi tạo xong."
