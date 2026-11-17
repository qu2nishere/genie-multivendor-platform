#!/usr/bin/env bash
# Nạp dữ liệu mẫu vào PostgreSQL của MÁY DEV (plan seed 2026-11-16 S01.03; ENTITY 13.2; ADR 0010).
#   R__seed_demo.sql (tài khoản) + server-spring/src/main/resources/db/seed/R__seed_demo_*.sql (danh mục, shop, sản phẩm…)
# Dùng CHUNG biến CSDL với backend nên cắm CSDL nào thì seed vào đúng CSDL đó:
#   DATABASE_URL (postgresql://user:pass@host:port/db) > DB_URL (jdbc:postgresql://…) + DB_USER + DB_PASSWORD
#   thứ tự lấy: biến môi trường → tệp .env gốc repo → mặc định Docker local (genie / changeme).
# CHỈ chạy với host local (localhost, 127.0.0.1, ::1, tên dịch vụ Docker "postgres") — production (AWS RDS) bị từ chối.
#
#   scripts/seed/seed.sh               # dùng psql trên máy
#   scripts/seed/seed.sh --docker      # dùng psql trong container postgres của deploy/docker-compose.local.yml
#   scripts/seed/seed.sh --only 30     # chỉ chạy R__seed_demo_30_*.sql (00 = R__seed_demo.sql)
#   scripts/seed/seed.sh --dry-run     # chỉ in danh sách file sẽ chạy
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
MIGRATION_DIR="$ROOT/server-spring/src/main/resources/db/migration"
SEED_DIR="$ROOT/server-spring/src/main/resources/db/seed"
COMPOSE_FILE="$ROOT/deploy/docker-compose.local.yml"

USE_DOCKER=false
ONLY=""
DRY_RUN=false
while [ $# -gt 0 ]; do
  case "$1" in
    --docker) USE_DOCKER=true ;;
    --only) ONLY="${2:?--only cần số thứ tự file, vd 30}"; shift ;;
    --dry-run) DRY_RUN=true ;;
    -h|--help) sed -n '2,14p' "$0"; exit 0 ;;
    *) echo "Tham số không hợp lệ: $1 (xem --help)" >&2; exit 2 ;;
  esac
  shift
done

# ---------- đọc biến: môi trường → .env → mặc định ----------
env_from_file() {   # env_from_file KEY → in giá trị trong .env (dòng viết SAU thắng), bỏ comment / ngoặc kép
  [ -f "$ROOT/.env" ] || return 0
  grep -E "^[[:space:]]*(export[[:space:]]+)?$1=" "$ROOT/.env" | tail -n 1 \
    | sed -E "s/^[[:space:]]*(export[[:space:]]+)?$1=//; s/[[:space:]]+#.*$//; s/^\"(.*)\"$/\1/; s/^'(.*)'$/\1/" | tr -d '\r'
}
pick() { local v="${!1:-}"; [ -n "$v" ] || v=$(env_from_file "$1"); [ -n "$v" ] || v="$2"; printf '%s' "$v"; }

DATABASE_URL=$(pick DATABASE_URL "")
if [ -n "$DATABASE_URL" ]; then
  # postgresql://user:pass@host:port/db?params
  rest="${DATABASE_URL#*://}"
  creds="${rest%%@*}"; hostpart="${rest#*@}"
  DB_USER="${creds%%:*}"; DB_PASSWORD="${creds#*:}"; [ "$DB_PASSWORD" = "$creds" ] && DB_PASSWORD=""
else
  DB_URL=$(pick DB_URL "jdbc:postgresql://localhost:5432/genie")
  DB_USER=$(pick DB_USER "genie")
  DB_PASSWORD=$(pick DB_PASSWORD "changeme")
  hostpart="${DB_URL#jdbc:postgresql://}"
fi
# hostpart = host[:port]/db[?params]
hostport="${hostpart%%/*}"
dbandparams="${hostpart#*/}"
DB_NAME="${dbandparams%%\?*}"
DB_PARAMS=""; [ "$dbandparams" != "$DB_NAME" ] && DB_PARAMS="${dbandparams#*\?}"
DB_HOST="${hostport%%:*}"; DB_PORT="${hostport#*:}"; [ "$DB_PORT" = "$hostport" ] && DB_PORT=5432

# ---------- bộ chặn môi trường (N10): chỉ host local ----------
case "$DB_HOST" in
  localhost|127.0.0.1|::1|\[::1\]|postgres) ;;
  *)
    echo "DỪNG: host CSDL '$DB_HOST' không phải máy dev (chỉ chấp nhận localhost, 127.0.0.1, ::1, postgres)." >&2
    echo "Dữ liệu mẫu chỉ nạp vào PostgreSQL local; production (AWS RDS) không bao giờ seed — ADR 0010." >&2
    exit 3 ;;
esac

if $USE_DOCKER; then
  echo "CSDL: container postgres (deploy/docker-compose.local.yml) · db=$DB_NAME · user=$DB_USER"
else
  echo "CSDL: $DB_HOST:$DB_PORT · db=$DB_NAME · user=$DB_USER · mật khẩu=****${DB_PARAMS:+ · $DB_PARAMS}"
fi

# ---------- danh sách file ----------
FILES=()
if [ -z "$ONLY" ] || [ "$ONLY" = "00" ]; then FILES+=("$MIGRATION_DIR/R__seed_demo.sql"); fi
for f in "$SEED_DIR"/R__seed_demo_*.sql; do
  [ -e "$f" ] || continue
  if [ -n "$ONLY" ]; then case "$(basename "$f")" in "R__seed_demo_${ONLY}_"*) ;; *) continue ;; esac; fi
  FILES+=("$f")
done
[ ${#FILES[@]} -gt 0 ] || { echo "Không có file seed nào khớp '--only $ONLY'." >&2; exit 2; }

if $DRY_RUN; then
  printf 'Sẽ chạy (theo thứ tự):\n'; for f in "${FILES[@]}"; do printf '  %s\n' "${f#$ROOT/}"; done; exit 0
fi

# ---------- psql ----------
run_psql() {   # run_psql [tham số psql…] — đọc SQL từ stdin
  if $USE_DOCKER; then
    docker compose -f "$COMPOSE_FILE" exec -T -e PGPASSWORD="$DB_PASSWORD" postgres \
      psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -q "$@"
  else
    command -v psql >/dev/null || { echo "Không tìm thấy psql — cài PostgreSQL client hoặc dùng --docker." >&2; exit 4; }
    local conn="host=$DB_HOST port=$DB_PORT dbname=$DB_NAME user=$DB_USER"
    [ -n "$DB_PARAMS" ] && conn="$conn ${DB_PARAMS//&/ }"
    PGPASSWORD="$DB_PASSWORD" psql "$conn" -v ON_ERROR_STOP=1 -q "$@"
  fi
}

COUNT_SQL="SELECT t.name AS bang, t.n AS so_dong FROM (VALUES
  ('users', (SELECT count(*) FROM users)), ('categories', (SELECT count(*) FROM categories)),
  ('brands', (SELECT count(*) FROM brands)), ('category_attributes', (SELECT count(*) FROM category_attributes)),
  ('vendor_profiles', (SELECT count(*) FROM vendor_profiles)), ('products', (SELECT count(*) FROM products)),
  ('product_variants', (SELECT count(*) FROM product_variants)), ('product_images', (SELECT count(*) FROM product_images)),
  ('vouchers', (SELECT count(*) FROM vouchers)), ('home_banners', (SELECT count(*) FROM home_banners)),
  ('coin_transactions', (SELECT count(*) FROM coin_transactions)), ('user_wishlists', (SELECT count(*) FROM user_wishlists)),
  ('shop_follows', (SELECT count(*) FROM shop_follows)), ('kyc_results', (SELECT count(*) FROM kyc_results))
) AS t(name, n);"

# kết nối + đã migrate tới V14 chưa
if ! echo "SELECT 1;" | run_psql -t >/dev/null; then
  echo "Không kết nối được CSDL. Kiểm tra container / biến DB_* (docs/cau-hinh-moi-truong.md)." >&2; exit 5
fi
migrated=$(echo "SELECT CASE WHEN to_regclass('public.flyway_schema_history') IS NULL THEN 0
  ELSE (SELECT coalesce(max(installed_rank), 0) FROM flyway_schema_history WHERE version IS NOT NULL AND split_part(version, '.', 1)::int >= 14 AND success) END;" \
  | run_psql -t -A | tr -d '[:space:]')
if [ "${migrated:-0}" = "0" ]; then
  echo "CSDL chưa có schema V14 — chạy backend một lần (./mvnw spring-boot:run trong server-spring) để Flyway migrate rồi seed lại." >&2
  exit 6
fi

echo "Trước khi seed:"; echo "$COUNT_SQL" | run_psql
for f in "${FILES[@]}"; do
  echo "→ ${f#$ROOT/}"
  if [ "$(basename "$f")" = "R__seed_demo.sql" ]; then
    # D3: psql không thay placeholder Flyway → tự thay ${seed_demo} = true
    sed 's/\${seed_demo}/true/g' "$f" | run_psql --single-transaction
  else
    run_psql --single-transaction < "$f"
  fi
done
echo "Sau khi seed:"; echo "$COUNT_SQL" | run_psql
echo "Xong. Ảnh mẫu: scripts/seed/upload-assets.sh (đẩy lên Silo)."
