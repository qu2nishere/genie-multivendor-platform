#!/usr/bin/env bash
# Đẩy ảnh mẫu lên Silo của MÁY DEV (plan seed 2026-11-16 S04.01, D6; LUU_TRU 5.5) — khớp khóa đối tượng trong db/seed:
#   web/public/img/products/*  → <tiền tố>product-media/seed/product/
#   web/public/img/banners/*   → <tiền tố>content-assets/seed/banner/
#   scripts/seed/excel/assets/*.jpg → <tiền tố>product-media/seed/excel/  (ảnh seed từ Excel — plan 2026-11-21 E05.01)
# Cuối cùng đối chiếu mọi khóa 'seed/excel/…' trong R__seed_demo_95_excel_products.sql với Silo — thiếu ảnh → thoát mã 4 (E05.02).
# Dùng client mcli có sẵn trong image Silo (service silo-init của deploy/docker-compose.local.yml) — không cần cài gì thêm.
# Đọc CÙNG biến với backend: MINIO_ENDPOINT, MINIO_APP_ACCESS_KEY, MINIO_APP_SECRET_KEY, STORAGE_BUCKET_PREFIX
# (biến môi trường → .env gốc repo → mặc định Docker local). Chạy lại không tải trùng (mirror --overwrite).
# KHÔNG đẩy lên AWS S3 production: MINIO_ENDPOINT chứa amazonaws.com → dừng (ADR 0010).
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
COMPOSE_FILE="$ROOT/deploy/docker-compose.local.yml"

env_from_file() {
  [ -f "$ROOT/.env" ] || return 0
  grep -E "^[[:space:]]*(export[[:space:]]+)?$1=" "$ROOT/.env" | tail -n 1 \
    | sed -E "s/^[[:space:]]*(export[[:space:]]+)?$1=//; s/[[:space:]]+#.*$//; s/^\"(.*)\"$/\1/; s/^'(.*)'$/\1/" | tr -d '\r'
}
pick() { local v="${!1:-}"; [ -n "$v" ] || v=$(env_from_file "$1"); [ -n "$v" ] || v="$2"; printf '%s' "$v"; }

ENDPOINT=$(pick MINIO_ENDPOINT "http://localhost:9000")
ACCESS_KEY=$(pick MINIO_APP_ACCESS_KEY "genie-app")
SECRET_KEY=$(pick MINIO_APP_SECRET_KEY "changeme")
PREFIX=$(pick STORAGE_BUCKET_PREFIX "")

case "$ENDPOINT" in
  *amazonaws.com*)
    echo "DỪNG: MINIO_ENDPOINT=$ENDPOINT là AWS S3 — ảnh mẫu chỉ đẩy lên Silo máy dev (ADR 0010)." >&2; exit 3 ;;
  http://localhost:*|http://127.0.0.1:*|http://localhost|http://127.0.0.1)
    TARGET="http://silo:9000" ;;   # Silo của docker-compose.local.yml — gọi qua mạng compose
  *)
    TARGET="$ENDPOINT" ;;
esac
echo "Silo: $TARGET (máy dev: $ENDPOINT) · bucket: ${PREFIX}product-media, ${PREFIX}content-assets"

IMG_DIR="$ROOT/web/public/img"
EXCEL_DIR="$ROOT/scripts/seed/excel/assets"
EXCEL_SQL="$ROOT/server-spring/src/main/resources/db/seed/R__seed_demo_95_excel_products.sql"
mkdir -p "$EXCEL_DIR"
# Git Bash trên Windows: đổi sang đường dẫn Windows và tắt chuyển đổi đường dẫn tự động của MSYS khi gắn volume
if command -v cygpath >/dev/null 2>&1; then
  IMG_DIR=$(cygpath -w "$IMG_DIR"); EXCEL_MOUNT=$(cygpath -w "$EXCEL_DIR"); COMPOSE_FILE=$(cygpath -w "$COMPOSE_FILE")
  export MSYS_NO_PATHCONV=1
else
  EXCEL_MOUNT="$EXCEL_DIR"
fi

docker compose -f "$COMPOSE_FILE" run --rm --no-deps -v "$IMG_DIR:/assets:ro" -v "$EXCEL_MOUNT:/excel:ro" \
  --entrypoint /bin/sh silo-init -c "
  set -e
  MC=\$(command -v mcli || command -v mc)
  \"\$MC\" alias set seed '$TARGET' '$ACCESS_KEY' '$SECRET_KEY' >/dev/null
  \"\$MC\" mirror --overwrite /assets/products 'seed/${PREFIX}product-media/seed/product'
  \"\$MC\" mirror --overwrite /assets/banners 'seed/${PREFIX}content-assets/seed/banner'
  \"\$MC\" mirror --overwrite --exclude 'manifest.json' /excel 'seed/${PREFIX}product-media/seed/excel'
"

# ---------- E05.02: ảnh nào file SQL 95 cần mà Silo chưa có? (image silo-init không có grep → so ở máy) ----------
if [ -f "$EXCEL_SQL" ]; then
  expected=$(grep -o "seed/excel/[0-9a-f]\{8\}\.jpg" "$EXCEL_SQL" | sort -u || true)
  if [ -n "$expected" ]; then
    present=$(docker compose -f "$COMPOSE_FILE" run --rm --no-deps --entrypoint /bin/sh silo-init -c "
      MC=\$(command -v mcli || command -v mc)
      \"\$MC\" alias set seed '$TARGET' '$ACCESS_KEY' '$SECRET_KEY' >/dev/null
      \"\$MC\" ls --recursive 'seed/${PREFIX}product-media/seed/excel/'" | tr -d '\r' | awk '{print "seed/excel/" $NF}' | sort -u)
    missing=$(comm -23 <(printf '%s\n' "$expected") <(printf '%s\n' "$present"))
    if [ -n "$missing" ]; then
      echo "THIẾU ảnh trên Silo (bucket ${PREFIX}product-media) mà R__seed_demo_95 cần:" >&2
      printf '  %s\n' $missing >&2
      echo "→ Kiểm folder scripts/seed/excel/assets/ đã pull đủ chưa, và STORAGE_BUCKET_PREFIX / MINIO_ENDPOINT trong .env" >&2
      echo "  phải giống cấu hình backend đang chạy." >&2
      exit 4
    fi
    echo "Ảnh seed Excel: đủ $(printf '%s\n' "$expected" | wc -l | tr -d ' ') ảnh trên Silo."
  fi
fi
echo "Xong. Thử: ${ENDPOINT%/}/${PREFIX}product-media/seed/product/headphones.jpg"
