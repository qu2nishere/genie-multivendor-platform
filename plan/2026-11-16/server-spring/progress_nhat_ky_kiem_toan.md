# PROGRESS — Tra cứu nhật ký kiểm toán hệ thống bất biến (UC-ADM-07)

| | |
|---|---|
| **Plan** | [plan_nhat_ky_kiem_toan.md](./plan_nhat_ky_kiem_toan.md) — chi tiết từng task tra theo mã `Lxx.yy` |
| **Ngày lập** | 2026-11-16 |
| **Tổng** | 39 task + 6 commit, 7 giai đoạn |
| **Nhánh** | `feature/admin-audit-log` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-17 — đang làm (28 / 45) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục Ghi chú.
- Task `COMMIT` chỉ tick sau khi qua Cổng kiểm tra (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`.
- Thông điệp commit ghi sẵn bên dưới, thêm ` [#GNE-<số>]` vào cuối khi đã có mã Jira.
- Cập nhật cột "Xong" của bảng Tổng quan và dòng "Cập nhật gần nhất" sau mỗi buổi làm; ghi một dòng vào Nhật ký cuối trang.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | Đường dẫn: UC-ADM-07 bước 1 ghi `GET /api/v1/admin/system-logs?from=&to=&type=&q=`; API 11.10 ghi `GET /admin/audit-logs?from&to&category&q&cursor&limit` | ☑ | Theo API: `GET /admin/audit-logs`, `/admin/audit-logs/export`; sửa Usecase.md ở L06.03 · 2026-11-17 |
| D2 | Ánh xạ `AuditAction` → `AuditCategory` (API 16: `AUTH, KYC, FINANCE, MODERATION, SECURITY, CONFIG`) chưa có ở đâu | ☑ | Như đề xuất — `AuditCategory` + `AuditAction.category()` (switch không default) · 2026-11-17 |
| D3 | `target` ("KYC-1037 · TravelGear") lấy từ đâu — `system_logs` chỉ có `reference_id` + `details` | ☑ | Như đề xuất — `details.target_label` → `reference_id` → `null` · 2026-11-17 |
| D4 | `actor_role`: ví dụ API là `MODERATOR` (chức danh), còn `system_logs.user_role` chụp `STAFF` | ☑ | Như đề xuất — `STAFF` thay bằng chức danh hiện tại (tra `admin_profiles` một lần / trang) · 2026-11-17 |
| D5 | `result`: API có `SUCCESS, FAILED, DENIED`; cột `status` chỉ có `SUCCESS` / `FAILED` | ☑ | Như đề xuất — `DENIED` khi `ACCESS_DENIED`; `summary.denied_count` trong bộ lọc · 2026-11-17 |
| D6 | Xuất CSV: giới hạn bao nhiêu dòng | ☑ | Như đề xuất — 50.000 dòng, BOM UTF-8, `X-Truncated: true` khi cắt · 2026-11-17 |
| D7 | Tìm theo `q` (user / IP / mã tham chiếu) | ☑ | Như đề xuất — `ILIKE` email / IP / mã tham chiếu; mặc định 7 ngày, tối đa 90 ngày · 2026-11-17 |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong | Mã Jira | Nhánh |
|---|---|---:|---:|---:|---|---|
| L00 | Chuẩn bị | 5 | 0 | 4 / 5 | — | `feature/admin-audit-log` |
| L01 | Phân loại hành động (`common/audit`) | 4 | 1 | 5 / 5 | — | `feature/admin-audit-log` |
| L02 | Truy vấn đọc `system_logs` | 5 | 1 | 6 / 6 | — | `feature/admin-audit-log` |
| L03 | Dịch vụ & API tra cứu | 8 | 1 | 9 / 9 | — | `feature/admin-audit-log` |
| L04 | Xuất CSV | 4 | 1 | 4 / 5 | — | `feature/admin-audit-log` |
| L05 | Nối giao diện `web/` (scope `admin-ui`) | 6 | 1 | 0 / 7 | — | `feature/admin-audit-log` |
| L06 | Hoàn thiện use case | 7 | 1 | 0 / 8 | — | `feature/admin-audit-log` |
| **Tổng** | | **39** | **6** | **28 / 45** | | |

## L00 — Chuẩn bị

- [x] **L00.01** Kiểm danh tính git
- [x] **L00.02** Chốt quyết định D1–D7
- [x] **L00.03** Tạo nhánh
- [ ] **L00.04** Dựng dữ liệu thử
- [x] **L00.05** Đọc lại màn mock

## L01 — Phân loại hành động (`common/audit`)

- [x] **L01.01** `AuditCategory`
- [x] **L01.02** `AuditAction.category()`
- [x] **L01.03** `AuditCategory.actions()`
- [x] **L01.04** `AuditActionCategoryTest`
- [x] **L01.C1** 🔖 COMMIT `feat(common): classify audit actions into categories` · hash: `7ca8ef4`

## L02 — Truy vấn đọc `system_logs`

- [x] **L02.01** `SystemLogRepository`
- [x] **L02.02** `countDenied(from, to, actions, q)`
- [x] **L02.03** `streamForExport(from, to, actions, q, max)`
- [x] **L02.04** Kiểm `EXPLAIN` trên CSDL local có vài nghìn dòng (chèn tạm bằng `generate_series`)
- [x] **L02.05** Luật kiến trúc
- [x] **L02.C1** 🔖 COMMIT `feat(common): add read-only keyset queries for system logs` · hash: `8a3e67f`

## L03 — Dịch vụ & API tra cứu

- [x] **L03.01** `AuditLogQuery` (record tham số)
- [x] **L03.02** Con trỏ
- [x] **L03.03** `AuditLogView` + ánh xạ từ `SystemLog`: `id`, `created_at`, `actor_email` (`system` khi `user_id` null
- [x] **L03.04** `AuditLogQueryService.search(query)`
- [x] **L03.05** Tra chức danh nhân viên (D4)
- [x] **L03.06** `AdminAuditLogController`
- [x] **L03.07** `AuditLogQueryIT`
- [x] **L03.08** `AuditLogPermissionIT`
- [x] **L03.C1** 🔖 COMMIT `feat(admin): search system audit logs with filters and cursor paging` · hash: `43a9504`

## L04 — Xuất CSV

- [x] **L04.01** `AuditLogCsvWriter`
- [x] **L04.02** `GET /api/v1/admin/audit-logs/export`
- [x] **L04.03** Giới hạn tần suất xuất
- [x] **L04.04** `AuditLogExportIT`
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

## Nhật ký
| L00.05 | Web gửi `from` / `to` dạng ngày `YYYY-MM-DD` (to tính cả ngày) và đọc `pagination`, `summary` ở cấp gốc của phản hồi; `AuditLogDto.id` khai `number` nhưng `system_logs.id` là UUID chuỗi → sửa DTO web ở L05 |
| L02.01 | Làm bằng `JpaSpecificationExecutor` + `SystemLogSpecs` (`filter`, `after`, `denied`) thay vì `@Query`: tham số tùy chọn (nhóm, `q`) trong JPQL dễ lỗi suy kiểu `NULL` trên PostgreSQL. `countDenied` = `count(filter.and(denied))`; xuất CSV dùng `findBy(spec, q -> q.sortBy(…).limit(max).stream())` trong transaction chỉ đọc |
| L02.04 | Chèn tạm 5.000 dòng rồi rollback: trang 51 dòng dùng `Index Scan Backward` trên `idx_system_logs_created_action`, chạy 0,08 ms; đếm `ACCESS_DENIED` chạy Seq Scan 0,5 ms (bảng nhỏ). Không thêm index |
| L03.01 | `AuditLogQuery` đặt ở `application` (dùng chung cho tra cứu và xuất CSV). `from` / `to` nhận cả ISO-8601 lẫn ngày `YYYY-MM-DD` (giờ VN, `to` tính hết ngày — khớp web). Thêm dùng chung: `common/error/InvalidRequestException` (422 có `errors[]` theo trường) và `common/api/ApiListResponse` + `ApiResponseFactory.list()` (`pagination`, `summary` ở cấp gốc — API 0.3) |
| L03.07 | Lần chạy đầu lỗi môi trường: Byte Buddy không tự gắn agent vào JVM (Mockito inline) — chạy lại thì 9/9 xanh, không phải lỗi mã |
| L04.02 | Ghi thẳng ra `HttpServletResponse` (đồng bộ, vẫn theo luồng: `Stream` + `detach` từng dòng) thay vì `StreamingResponseBody` — tránh xử lý bất đồng bộ làm mất transaction / security context, MockMvc test được. Giới hạn đọc từ `genie.audit.export-max` (mặc định 50.000); test hạ xuống 3 để thử `X-Truncated`. Mở `Access-Control-Expose-Headers: Content-Disposition, X-Truncated` cho web |

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-16 | Lập plan + progress (39 task, 6 commit, 7 giai đoạn) — chưa bắt đầu làm | — |
