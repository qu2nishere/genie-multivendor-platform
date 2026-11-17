#!/usr/bin/env bash
# Sao lưu bổ sung cho production (TECH_STACK mục 16.9, ADR 0010).
# Lớp chính: RDS automated backups 7 ngày + snapshot trước mỗi release (AWS lo); S3 có SSE + versioning cho kyc-dossiers.
# Script này thêm một bản pg_dump logic ra ổ VPS / EC2 — dễ khôi phục sang máy khác hoặc sang PostgreSQL máy dev:
#   cron: 0 2 * * *  /home/deploy/genie/deploy/backup/backup.sh      → giữ 7 bản ở $BACKUP_DIR/pg
# Dùng image postgres:16-alpine để có pg_dump đúng phiên bản (không cần cài client trên máy chủ).
set -euo pipefail
cd "$(dirname "$0")/.."
# shellcheck disable=SC1091
source .env

BACKUP_DIR=${BACKUP_DIR:-/backup}
TODAY=$(date +%F)

# jdbc:postgresql://host:port/db?sslmode=require  →  host, port, db
url=${DB_URL#jdbc:postgresql://}
hostport=${url%%/*}
dbname=${url#*/}; dbname=${dbname%%\?*}
host=${hostport%%:*}
port=${hostport##*:}; [ "$port" = "$hostport" ] && port=5432

mkdir -p "$BACKUP_DIR/pg"
docker run --rm -e PGPASSWORD="$DB_PASSWORD" -e PGSSLMODE=require postgres:16-alpine \
  pg_dump -h "$host" -p "$port" -U "$DB_USER" -Fc "$dbname" > "$BACKUP_DIR/pg/$TODAY.dump"
ls -1t "$BACKUP_DIR"/pg/*.dump | tail -n +8 | xargs -r rm --
echo "RDS $host/$dbname → $BACKUP_DIR/pg/$TODAY.dump"

# (Đề xuất) đẩy bản dump lên S3 riêng cho sao lưu: aws s3 cp "$BACKUP_DIR/pg/$TODAY.dump" s3://<bucket-sao-lưu>/pg/
