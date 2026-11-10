#!/usr/bin/env bash
# Pull image theo TAG, up -d, kiểm health, rollback — TECH_STACK mục 15
# Dùng: ./scripts/deploy.sh vX.Y.Z
set -euo pipefail
cd "$(dirname "$0")/.."

NEW_TAG=${1:?Cần phiên bản, ví dụ v0.2.0}
PREV_TAG=$(grep -E '^TAG=' .env | cut -d= -f2 || true)

set_tag() { sed -i "s/^TAG=.*/TAG=$1/" .env; }

echo "Triển khai $NEW_TAG (bản trước: ${PREV_TAG:-không có})"
set_tag "$NEW_TAG"
docker compose pull
docker compose up -d

echo "Chờ /actuator/health = UP (tối đa 120 giây)..."
for _ in $(seq 1 24); do
  if docker compose exec -T backend wget -qO- http://localhost:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    echo "OK — $NEW_TAG đang chạy."
    exit 0
  fi
  sleep 5
done

echo "Health check thất bại — rollback về $PREV_TAG" >&2
if [ -n "${PREV_TAG:-}" ]; then
  set_tag "$PREV_TAG"
  docker compose up -d
fi
# Lưu ý: migration chỉ tiến — rollback image không lùi CSDL.
exit 1
