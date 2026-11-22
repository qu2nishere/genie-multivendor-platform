# Progress — Nhật ký banner theo hành động cụ thể & xem chi tiết

> Plan: [plan_nhat_ky_banner_chi_tiet.md](plan_nhat_ky_banner_chi_tiet.md)

## Tiến độ

| Giai đoạn | Việc | Trạng thái |
|---|---|---|
| P00 | Tài liệu ENTITY / API_SPEC | ☐ |
| P01 | Migration V21 + enum | ☐ |
| P02 | Ghi nhật ký banner | ☐ |
| P03 | API Nhật ký trả chi tiết | ☐ |
| P04 | Web | ☐ |
| P05 | Đồng bộ tiến độ | ☐ |

## Task

- [ ] **P00.01** ENTITY.md Phụ lục A + Diagram10
- [ ] **P00.02** API_SPEC 11.10 + 16
- [ ] **P00.03** COMMIT docs
- [ ] **P01.01** `V21__banner_audit_granular_actions.sql`
- [ ] **P01.02** `AuditAction` + `category()`
- [ ] **P01.03** COMMIT db
- [ ] **P02.01** `BannerAdminService.record` ghi mã cụ thể
- [ ] **P02.02** `BannerAdminIT`
- [ ] **P02.03** COMMIT admin
- [ ] **P03.01** `AuditLogView` + `AuditChange`
- [ ] **P03.02** `toView` đọc `details`
- [ ] **P03.03** Test
- [ ] **P03.04** COMMIT admin
- [ ] **P04.01** DTO / types / mapper / mock
- [ ] **P04.02** i18n hành động + `actionLabel`
- [ ] **P04.03** `AuditLogPage` + hộp chi tiết
- [ ] **P04.04** Test + verify
- [ ] **P04.05** COMMIT admin-ui
- [ ] **P05.01** Kịch bản kiểm thử, CHECKLIST, CLAUDE.md
- [ ] **P05.02** COMMIT docs

## Nhật ký

| Ngày | Việc | Commit |
|---|---|---|
