# Architecture Decision Records

Mỗi quyết định kiến trúc một tệp `NNNN-<ten-ngan>.md` theo mẫu: **Trạng thái · Bối cảnh · Quyết định · Hệ quả**.
Quyết định đã thay thế thì đổi trạng thái thành `Thay bằng NNNN`, không xóa tệp.

| # | Quyết định | Trạng thái | Ngày |
|---|---|---|---|
| [0001](0001-web-chin-theme-storefront.md) | Storefront mang đủ 9 theme của prototype | Chấp nhận | 2026-11-11 |
| [0002](0002-web-css-lai-tailwind-va-lop-tr.md) | CSS lai: utility Tailwind + giữ lớp `tr-*` / theme trong `styles/` | Chấp nhận | 2026-11-11 |
| [0003](0003-web-du-lieu-mock-truoc.md) | Dựng giao diện trên lớp mock trước khi có backend | Chấp nhận | 2026-11-11 |
| [0004](0004-web-bang-icon-prototype.md) | Port bảng icon `ICONS` của prototype, không thêm thư viện icon | Chấp nhận | 2026-11-11 |
| [0005](0005-web-bo-phan-trinh-dien-prototype.md) | Không chuyển phần chỉ để trình diễn của prototype | Chấp nhận | 2026-11-11 |
| [0006](0006-web-thu-muc-bo-sung.md) | Thư mục / tệp bổ sung so với cây TECH_STACK 2.4 | Chấp nhận | 2026-11-14 |
| [0007](0007-auth-schema-va-cau-truc-module.md) | AUTH: bổ sung schema bằng V14 và cấu trúc module | Chấp nhận | 2026-11-14 |
| [0008](0008-common-mail-audit-va-cong-dao-nguoc.md) | `common`: gửi email, nhật ký, bảo mật và cổng đảo ngược giữa module | Chấp nhận | 2026-11-14 |
| [0009](0009-kho-tep-silo-thay-image-minio.md) | Kho tệp: thay MinIO (ngừng phát hành) bằng Silo (máy dev / test; production xem 0010) | Chấp nhận | 2026-11-15 |
| [0010](0010-production-aws-rds-s3.md) | Production cố định AWS RDS + S3; dev / test dùng PostgreSQL (Docker hoặc cài máy) + Silo | Chấp nhận | 2026-11-17 |

Nguồn: plan chuyển giao diện `plan/2026-11-11/web/plan_chuyen_giao_dien.md` (mục 2, 3) và nhật ký
`progress_chuyen_giao_dien.md`.
