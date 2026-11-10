#!/usr/bin/env bash
# Sao lưu — TECH_STACK mục 16.9
#   pg_dump -Fc hằng ngày 02:00, giữ 7 bản      → cron: 0 2 * * *  /home/deploy/genie/deploy/backup/backup.sh pg
#   mc mirror MinIO Chủ nhật hằng tuần, giữ 2 bản → cron: 0 3 * * 0  /home/deploy/genie/deploy/backup/backup.sh minio
set -euo pipefail
cd "$(dirname "$0")/.."
source .env

BACKUP_DIR=${BACKUP_DIR:-/backup}
TODAY=$(date +%F)

backup_pg() {
  mkdir -p "$BACKUP_DIR/pg"
  docker compose exec -T postgres pg_dump -U "$DB_USER" -Fc genie > "$BACKUP_DIR/pg/$TODAY.dump"
  ls -1t "$BACKUP_DIR"/pg/*.dump | tail -n +8 | xargs -r rm --
  echo "PostgreSQL → $BACKUP_DIR/pg/$TODAY.dump"
}

backup_minio() {
  mkdir -p "$BACKUP_DIR/minio/$TODAY"
  docker compose run --rm -v "$BACKUP_DIR/minio/$TODAY:/backup" --entrypoint /bin/sh minio-init -c \
    "mc alias set genie http://minio:9000 \"\$MINIO_ROOT_USER\" \"\$MINIO_ROOT_PASSWORD\" && mc mirror --overwrite genie /backup"
  ls -1dt "$BACKUP_DIR"/minio/*/ | tail -n +3 | xargs -r rm -rf --
  echo "MinIO → $BACKUP_DIR/minio/$TODAY"
}

case "${1:-pg}" in
  pg) backup_pg ;;
  minio) backup_minio ;;
  all) backup_pg; backup_minio ;;
  *) echo "Dùng: $0 [pg|minio|all]"; exit 1 ;;
esac

# (Đề xuất) đẩy ra ngoài VPS: rclone copy "$BACKUP_DIR" remote:genie-backup --max-age 7d
