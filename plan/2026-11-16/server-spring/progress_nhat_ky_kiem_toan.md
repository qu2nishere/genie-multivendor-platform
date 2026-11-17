# PROGRESS — Tra cứu nhật ký kiểm toán hệ thống bất biến (UC-ADM-07)

| | |
|---|---|
| **Plan** | [plan_nhat_ky_kiem_toan.md](./plan_nhat_ky_kiem_toan.md) — chi tiết từng task tra theo mã `Lxx.yy` |
| **Ngày lập** | 2026-11-16 |
| **Tổng** | 39 task + 6 commit, 7 giai đoạn |
| **Nhánh** | `feature/admin-audit-log` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-16 — mới lập plan, chưa bắt đầu (0 / 45) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục Ghi chú.
- Task `COMMIT` chỉ tick sau khi qua Cổng kiểm tra (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`.
- Thông điệp commit ghi sẵn bên dưới, thêm ` [#GNE-<số>]` vào cuối khi đã có mã Jira.
- Cập nhật cột "Xong" của bảng Tổng quan và dòng "Cập nhật gần nhất" sau mỗi buổi làm; ghi một dòng vào Nhật ký cuối trang.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | Đường dẫn: UC-ADM-07 bước 1 ghi `GET /api/v1/admin/system-logs?from=&to=&type=&q=`; API 11.10 ghi `GET /admin/audit-logs?from&to&category&q&cursor&limit` | ☐ | |
| D2 | Ánh xạ `AuditAction` → `AuditCategory` (API 16: `AUTH, KYC, FINANCE, MODERATION, SECURITY, CONFIG`) chưa có ở đâu | ☐ | |
| D3 | `target` ("KYC-1037 · TravelGear") lấy từ đâu — `system_logs` chỉ có `reference_id` + `details` | ☐ | |
| D4 | `actor_role`: ví dụ API là `MODERATOR` (chức danh), còn `system_logs.user_role` chụp `STAFF` | ☐ | |
| D5 | `result`: API có `SUCCESS, FAILED, DENIED`; cột `status` chỉ có `SUCCESS` / `FAILED` | ☐ | |
| D6 | Xuất CSV: giới hạn bao nhiêu dòng | ☐ | |
| D7 | Tìm theo `q` (user / IP / mã tham chiếu) | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong | Mã Jira | Nhánh |
|---|---|---:|---:|---:|---|---|
| L00 | Chuẩn bị | 5 | 0 | 0 / 5 | — | `feature/admin-audit-log` |
| L01 | Phân loại hành động (`common/audit`) | 4 | 1 | 0 / 5 | — | `feature/admin-audit-log` |
| L02 | Truy vấn đọc `system_logs` | 5 | 1 | 0 / 6 | — | `feature/admin-audit-log` |
| L03 | Dịch vụ & API tra cứu | 8 | 1 | 0 / 9 | — | `feature/admin-audit-log` |
| L04 | Xuất CSV | 4 | 1 | 0 / 5 | — | `feature/admin-audit-log` |
| L05 | Nối giao diện `web/` (scope `admin-ui`) | 6 | 1 | 0 / 7 | — | `feature/admin-audit-log` |
| L06 | Hoàn thiện use case | 7 | 1 | 0 / 8 | — | `feature/admin-audit-log` |
| **Tổng** | | **39** | **6** | **0 / 45** | | |

## L00 — Chuẩn bị

- [ ] **L00.01** Kiểm danh tính git
- [ ] **L00.02** Chốt quyết định D1–D7
- [ ] **L00.03** Tạo nhánh
- [ ] **L00.04** Dựng dữ liệu thử
- [ ] **L00.05** Đọc lại màn mock

## L01 — Phân loại hành động (`common/audit`)

- [ ] **L01.01** `AuditCategory`
- [ ] **L01.02** `AuditAction.category()`
- [ ] **L01.03** `AuditCategory.actions()`
- [ ] **L01.04** `AuditActionCategoryTest`
- [ ] **L01.C1** 🔖 COMMIT `feat(common): classify audit actions into categories` · hash: `…`

## L02 — Truy vấn đọc `system_logs`

- [ ] **L02.01** `SystemLogRepository`
- [ ] **L02.02** `countDenied(from, to, actions, q)`
- [ ] **L02.03** `streamForExport(from, to, actions, q, max)`
- [ ] **L02.04** Kiểm `EXPLAIN` trên CSDL local có vài nghìn dòng (chèn tạm bằng `generate_series`)
- [ ] **L02.05** Luật kiến trúc
- [ ] **L02.C1** 🔖 COMMIT `feat(common): add read-only keyset queries for system logs` · hash: `…`

## L03 — Dịch vụ & API tra cứu

- [ ] **L03.01** `AuditLogQuery` (record tham số)
- [ ] **L03.02** Con trỏ
- [ ] **L03.03** `AuditLogView` + ánh xạ từ `SystemLog`: `id`, `created_at`, `actor_email` (`system` khi `user_id` null
- [ ] **L03.04** `AuditLogQueryService.search(query)`
- [ ] **L03.05** Tra chức danh nhân viên (D4)
- [ ] **L03.06** `AdminAuditLogController`
- [ ] **L03.07** `AuditLogQueryIT`
- [ ] **L03.08** `AuditLogPermissionIT`
- [ ] **L03.C1** 🔖 COMMIT `feat(admin): search system audit logs with filters and cursor paging` · hash: `…`

## L04 — Xuất CSV

- [ ] **L04.01** `AuditLogCsvWriter`
- [ ] **L04.02** `GET /api/v1/admin/audit-logs/export`
- [ ] **L04.03** Giới hạn tần suất xuất
- [ ] **L04.04** `AuditLogExportIT`
- [ ] **L04.C1** 🔖 COMMIT `feat(admin): export filtered audit logs as csv` · hash: `…`

## L05 — Nối giao diện `web/` (scope `admin-ui`)

- [ ] **L05.01** `web/src/features/admin-system/infrastructure/system.api.ts`
- [ ] **L05.02** `auditNow`
- [ ] **L05.03** Xuất CSV
- [ ] **L05.04** Lỗi & trạng thái
- [ ] **L05.05** Chạy thật hai phía
- [ ] **L05.06** `npm run verify` trong `web/`.
- [ ] **L05.C1** 🔖 COMMIT `feat(admin-ui): connect audit log screen to the real api` · hash: `…`

## L06 — Hoàn thiện use case

- [ ] **L06.01** `docs/api-tests/admin-audit-log.http`
- [ ] **L06.02** OpenAPI
- [ ] **L06.03** Tài liệu nguồn `docs/Document`
- [ ] **L06.04** Tiến độ
- [ ] **L06.05** `docs/web-backend-handoff.md`
- [ ] **L06.06** `CLAUDE.md` mục 2
- [ ] **L06.C1** 🔖 COMMIT `docs(admin): record audit log use case as implemented` · hash: `…`
- [ ] **L06.07** Mở PR vào `develop`

---

## Chốt plan — tick tiến độ use case

Làm **ngay khi xong plan** (hoặc xong từng use case), trong file [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../../docs/Document/CHECKLIST_TIEN_DO_UC.md) — cách tick xem mục 0 của file đó.

- [ ] `UC-ADM-07` Tra cứu nhật ký kiểm toán hệ thống bất biến — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [ ] Cập nhật số ở mục 1 *Tổng quan* của checklist (Đã triển khai / Đang triển khai / Chưa có backend)
- [ ] Thêm một dòng vào mục 5 *Nhật ký cập nhật* của checklist (ngày · use case · mã commit / PR)
- [ ] Đổi cột *Hiện trạng mã nguồn* của các use case trên trong `Quản Lý UseCase.xlsx`
- [ ] Tick dòng plan này ở bảng *Tiến độ các plan* trong [README](../README.md) mục 6

## Ghi chú

Ghi ở đây: lý do bỏ qua task (`[-]`), chỗ làm khác plan, số đo, việc tồn chuyển sang plan khác.

| Task | Ghi chú |
|---|---|
| | |

## Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-16 | Lập plan + progress (39 task, 6 commit, 7 giai đoạn) — chưa bắt đầu làm | — |
