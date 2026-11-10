#!/usr/bin/env bash
# Nạp dữ liệu demo vào PostgreSQL local (R__seed_demo.sql: 1 Admin, 2 Staff, 5 shop đã KYC, ~200 sản phẩm, voucher mẫu)
# Flyway đã tự chạy R__seed_demo.sql khi backend khởi động; script này dùng để nạp lại nhanh.
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
COMPOSE="docker compose -f $ROOT/deploy/docker-compose.local.yml"
SEED="$ROOT/server-spring/src/main/resources/db/migration/R__seed_demo.sql"

$COMPOSE exec -T postgres psql -U "${DB_USER:-genie}" -d genie -v ON_ERROR_STOP=1 < "$SEED"
echo "Đã nạp dữ liệu demo."
