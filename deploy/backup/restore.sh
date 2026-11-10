#!/usr/bin/env bash
# Khôi phục thử vào CSDL tạm — chạy trước mỗi release và trước buổi bảo vệ (TECH_STACK mục 16.9)
# Dùng: ./restore.sh /backup/pg/2026-11-10.dump [tên_db_đích]
set -euo pipefail
cd "$(dirname "$0")/.."
source .env

DUMP=${1:?Cần đường dẫn file .dump}
TARGET_DB=${2:-genie_restore_test}

docker compose exec -T postgres dropdb -U "$DB_USER" --if-exists "$TARGET_DB"
docker compose exec -T postgres createdb -U "$DB_USER" "$TARGET_DB"
docker compose exec -T postgres pg_restore -U "$DB_USER" -d "$TARGET_DB" --no-owner < "$DUMP"

echo "Đã khôi phục vào $TARGET_DB. Kiểm tra nhanh:"
docker compose exec -T postgres psql -U "$DB_USER" -d "$TARGET_DB" -c \
  "SELECT count(*) AS so_bang FROM information_schema.tables WHERE table_schema = 'public';"
