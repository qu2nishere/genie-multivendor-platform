# Progress — Nhật ký banner theo hành động cụ thể & xem chi tiết

> Plan: [plan_nhat_ky_banner_chi_tiet.md](plan_nhat_ky_banner_chi_tiet.md)

## Tiến độ

| Giai đoạn | Việc | Trạng thái |
|---|---|---|
| P00 | Tài liệu ENTITY / API_SPEC | ☑ |
| P01 | Migration V24 + enum | ☑ |
| P02 | Ghi nhật ký banner | ☑ |
| P03 | API Nhật ký trả chi tiết | ☑ |
| P04 | Web | ☑ |
| P05 | Đồng bộ tiến độ | ☑ |

## Task

- [x] **P00.01** ENTITY.md Phụ lục A + Diagram10
- [x] **P00.02** API_SPEC 11.10 + 16
- [x] **P00.03** COMMIT docs
- [x] **P01.01** `V24__banner_audit_granular_actions.sql`
- [x] **P01.02** `AuditAction` + `category()`
- [x] **P01.03** COMMIT db
- [x] **P02.01** `BannerAdminService.record` ghi mã cụ thể
- [x] **P02.02** `BannerAdminIT`
- [x] **P02.03** COMMIT admin
- [x] **P03.01** `AuditLogView` + `AuditChange`
- [x] **P03.02** `toView` đọc `details`
- [x] **P03.03** Test
- [x] **P03.04** COMMIT admin
- [x] **P04.01** DTO / types / mapper / mock
- [x] **P04.02** i18n hành động + `actionLabel`
- [x] **P04.03** `AuditLogPage` + hộp chi tiết
- [x] **P04.04** Test + verify
- [x] **P04.05** COMMIT admin-ui
- [x] **P05.01** Kịch bản kiểm thử, CHECKLIST, CLAUDE.md
- [x] **P05.02** COMMIT docs

## Nhật ký

| Ngày | Việc | Commit |
|---|---|---|
| 2026-11-22 | P00 tài liệu ENTITY / Diagram10 / XMI / API_SPEC + plan | `0122007` |
| 2026-11-22 | P01 `V21` + 5 mã `AuditAction` (nhóm CONFIG) | `476cac5` |
| 2026-11-22 | P02 `BannerAdminService` ghi mã cụ thể, `BannerAdminIT` | `d1c5d78` |
| 2026-11-22 | P03 API trả `reference_id`, `user_agent`, `changes`; `AuditLogQueryIT` | `27c0674` |
| 2026-11-22 | P04 web: nhãn hành động đã dịch, hộp chi tiết, mock, test | `9b785f5` |
| 2026-11-22 | P05 CHECKLIST, CLAUDE.md, progress. Kiểm: 138 unit test (có ArchUnit) + 35 test IT liên quan xanh; web vitest / tsc / eslint / check-i18n xanh. Kịch bản kiểm thử TC-01-09 cập nhật ở nhánh `feature/local-run-test-guides` | (commit này) |
