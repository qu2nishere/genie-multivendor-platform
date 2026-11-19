#!/usr/bin/env bash
# Nạp dữ liệu mẫu vào PostgreSQL Docker local (deploy/docker-compose.local.yml): tài khoản (R__seed_demo.sql) +
# db/seed (danh mục, 6 shop đã KYC, 20 sản phẩm, voucher, banner, hoạt động khách mẫu, hàng chờ KYC).
# Backend profile local đã tự nạp khi khởi động (GENIE_SEED_DEMO=true); script này để nạp lại nhanh.
# Giữ tên cũ cho tài liệu cũ — mọi việc do scripts/seed/seed.sh làm (xem scripts/seed/README.md).
set -euo pipefail
exec "$(dirname "$0")/seed/seed.sh" --docker "$@"
