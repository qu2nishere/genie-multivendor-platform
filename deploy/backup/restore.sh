#!/usr/bin/env bash
# Khôi phục thử bản pg_dump vào một CSDL tạm trên CÙNG RDS — chạy trước mỗi release và trước buổi bảo vệ
# (TECH_STACK mục 16.9, ADR 0010). Khôi phục thật sau sự cố: dùng "Restore to point in time" của RDS console.
# Dùng: ./restore.sh /backup/pg/2026-11-10.dump [tên_db_đích]
set -euo pipefail
cd "$(dirname "$0")/.."
# shellcheck disable=SC1091
source .env

DUMP=${1:?Cần đường dẫn file .dump}
TARGET_DB=${2:-genie_restore_test}
[ "$TARGET_DB" = "genie" ] && { echo "Không khôi phục đè lên CSDL đang chạy 'genie'." >&2; exit 1; }

url=${DB_URL#jdbc:postgresql://}
hostport=${url%%/*}
host=${hostport%%:*}
port=${hostport##*:}; [ "$port" = "$hostport" ] && port=5432

pg() { docker run --rm -i -e PGPASSWORD="$DB_PASSWORD" -e PGSSLMODE=require postgres:16-alpine "$@"; }

pg dropdb -h "$host" -p "$port" -U "$DB_USER" --if-exists "$TARGET_DB"
pg createdb -h "$host" -p "$port" -U "$DB_USER" "$TARGET_DB"
pg pg_restore -h "$host" -p "$port" -U "$DB_USER" -d "$TARGET_DB" --no-owner < "$DUMP"

echo "Đã khôi phục vào $TARGET_DB. Kiểm tra nhanh:"
pg psql -h "$host" -p "$port" -U "$DB_USER" -d "$TARGET_DB" -c \
  "SELECT count(*) AS so_bang FROM information_schema.tables WHERE table_schema = 'public';"
echo "Xong thì xóa: dropdb $TARGET_DB (để không tốn dung lượng RDS)."
