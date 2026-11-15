#!/usr/bin/env bash
# Sao lưu — TECH_STACK mục 16.9
#   pg_dump -Fc hằng ngày 02:00, giữ 7 bản      → cron: 0 2 * * *  /home/deploy/genie/deploy/backup/backup.sh pg
#   mcli mirror Silo Chủ nhật hằng tuần, giữ 2 bản → cron: 0 3 * * 0  /home/deploy/genie/deploy/backup/backup.sh silo
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

backup_silo() {
  mkdir -p "$BACKUP_DIR/silo/$TODAY"
  docker compose run --rm -v "$BACKUP_DIR/silo/$TODAY:/backup" --entrypoint /bin/sh silo-init -c \
    "MC=\$(command -v mcli || command -v mc) && \$MC alias set genie http://silo:9000 \"\$MINIO_ROOT_USER\" \"\$MINIO_ROOT_PASSWORD\" && \$MC mirror --overwrite genie /backup"
  ls -1dt "$BACKUP_DIR"/silo/*/ | tail -n +3 | xargs -r rm -rf --
  echo "Silo → $BACKUP_DIR/silo/$TODAY"
}

case "${1:-pg}" in
  pg) backup_pg ;;
  silo) backup_silo ;;
  all) backup_pg; backup_silo ;;
  *) echo "Dùng: $0 [pg|silo|all]"; exit 1 ;;
esac

# (Đề xuất) đẩy ra ngoài VPS: rclone copy "$BACKUP_DIR" remote:genie-backup --max-age 7d
