# PLAN — Tra cứu nhật ký kiểm toán hệ thống bất biến (UC-ADM-07)

| | |
|---|---|
| **Ngày lập** | 2026-11-16 |
| **Thể loại** | server-spring (module `admin`, feature `audit`) + nối `web/` (feature `admin-system`) |
| **Use case** | Tra cứu nhật ký kiểm toán hệ thống bất biến (UC-ADM-07) — Sprint 7 theo implement 1.2, làm sớm vì độc lập |
| **Tài liệu chuẩn** | UC phân hệ 11 (UC-ADM-07) · API 11.10, 0.3, 0.4, 0.10, 15, 16 · ENTITY 12.1 (`SystemLog`), Phụ lục A (`AuditAction`) · LLD 11.1 · TS 4, 8, 9, 14 — xem [README](../README.md) |
| **Theo dõi tiến độ** | [progress_nhat_ky_kiem_toan.md](./progress_nhat_ky_kiem_toan.md) — cùng mã task `Lxx.yy` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |

---

## 0. Hiện trạng đã khảo sát

| Phần | Có gì | Thiếu |
|---|---|---|
| `common/audit` | `SystemLog` (id UUID, `createdAt`, `userId`, `userEmail`, `userRole`, `actionType`, `referenceId`, `details` JSON, `ipAddress`, `status` SUCCESS / FAILED, `userAgent`), `SystemLogRepository` (rỗng), `AuditLogger` (đang được phân hệ AUTH gọi thật), `AuditAction` (30 giá trị), `AuditActor`, `ActorDirectory` | Truy vấn đọc có lọc + phân trang; enum `AuditCategory` (API 16) |
| CSDL | `system_logs` (`V12`), index `idx_system_logs_created_action (created_at, action_type)`; `CHECK` action đã có 30 giá trị (`V14`) | — (không cần migration) |
| `admin` | `rbac` (`StaffPermission`, `StaffPermissionEnum` có `VIEW_AUDIT_LOG`, `StaffPermissionAuthorityProvider`), `staff` (`AdminProfile`, `AdminProfileLookup`) | Feature `audit` (controller, service) |
| Bảo mật | `JwtAuthFilter` cấp authority = `ROLE_<role>` + mã quyền; `/admin/**` yêu cầu `STAFF`; `RestAccessDeniedHandler` ghi `ACCESS_DENIED` | — |
| `web/features/admin-system` | Màn Nhật ký hệ thống xong trên mock: `listAuditLogs` (`GET /admin/audit-logs`), `auditNow`; xuất CSV đang tạo ở trình duyệt từ dòng đang lọc (handoff mục 4) | Nối API thật; xuất CSV gọi `…/export` |
| Dữ liệu | Đăng nhập / đổi mật khẩu / Google… trên máy dev đã sinh `system_logs` thật | — |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Package `com.genie.admin.audit.{api,application}`; controller không mở transaction; service `@Transactional(readOnly = true)` | TS 4.1–4.2 |
| N2 | **Chỉ đọc**: không thêm bất kỳ đường `UPDATE` / `DELETE` nào cho `system_logs`; `SystemLogRepository` không được khai báo phương thức sửa / xóa (có test kiến trúc kiểm) | UC-ADM-07 ràng buộc |
| N3 | Quyền: `@PreAuthorize("hasAuthority('VIEW_AUDIT_LOG')")` trên mọi endpoint; Admin có sẵn mọi quyền; nhân viên thiếu quyền → `403 GNE-SEC-4030` + ghi `ACCESS_DENIED` (đã có ở `RestAccessDeniedHandler`) | API 0.10, 11.10 |
| N4 | JSON snake_case, vỏ `ApiResponse`, phân trang **cursor** theo (`created_at`, `id`) giảm dần; `summary.denied_count` trong `meta` theo API 0.3 | API 0.3, 0.4, 11.10 |
| N5 | `@Operation(summary = "UC-ADM-07 …")` trên mọi endpoint | TS 8 |
| N6 | Không trả `details` thô nếu chứa dữ liệu nhạy cảm: chỉ đưa ra `target` (mục 2 — D3); không bao giờ lộ mật khẩu / OTP / token (AUTH đã không ghi các giá trị này) | TS 9 |
| N7 | Thời gian lấy từ `Clock` bean; lọc `from` / `to` tính theo múi giờ `Asia/Ho_Chi_Minh` | TS 7.1 |
| N8 | Mỗi task có test: unit cho ánh xạ + `*IT` Testcontainers cho truy vấn, phân quyền | TS 14 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Nguồn lệch | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | Đường dẫn: UC-ADM-07 bước 1 ghi `GET /api/v1/admin/system-logs?from=&to=&type=&q=`; API 11.10 ghi `GET /admin/audit-logs?from&to&category&q&cursor&limit` | UC vs API | Theo **API** (web đã gọi `/admin/audit-logs`); sửa câu trong Usecase.md (task L06.03) | ☐ |
| D2 | Ánh xạ `AuditAction` → `AuditCategory` (API 16: `AUTH, KYC, FINANCE, MODERATION, SECURITY, CONFIG`) chưa có ở đâu | API 11.10 | Thêm enum `AuditCategory` ở `common/audit` và phương thức `AuditAction.category()`: **AUTH** = `LOGIN_SUCCESS, LOGIN_FAILED, PASSWORD_CHANGED, PASSWORD_RESET, EMAIL_CHANGED, OAUTH_LINKED, OAUTH_UNLINKED, SESSION_REVOKED, ACCOUNT_DELETION_REQUESTED, ACCOUNT_DELETION_CANCELLED` · **KYC** = `KYC_SUBMITTED, KYC_APPROVED, KYC_REJECTED` · **FINANCE** = `ESCROW_RELEASED, PAYOUT_APPROVED, PAYOUT_REJECTED, REFUND_PROCESSED, ORDER_STATUS_CHANGED` · **MODERATION** = `REVIEW_HIDDEN, PRODUCT_MODERATED` · **SECURITY** = `ACCOUNT_LOCKED, ACCOUNT_SUSPENDED, ACCOUNT_REACTIVATED, ACCESS_DENIED, PII_ANONYMIZED` · **CONFIG** = `COMMISSION_UPDATED, PERMISSION_GRANTED, PERMISSION_REVOKED, RBAC_ROLE_CHANGED, STAFF_INVITED`. Ghi bảng này vào API 16 / ENTITY Phụ lục A | ☐ |
| D3 | `target` ("KYC-1037 · TravelGear") lấy từ đâu — `system_logs` chỉ có `reference_id` + `details` | API 11.10 | `target` = `details.target_label` nếu module ghi nhật ký có đưa vào, ngược lại = `reference_id`, ngược lại `null`. Không JOIN sang bảng module khác (giữ bản ghi "chụp lại") | ☐ |
| D4 | `actor_role`: ví dụ API là `MODERATOR` (chức danh), còn `system_logs.user_role` chụp `STAFF` | API vs ENTITY | Trả `user_role` đã chụp; riêng `STAFF` thì thay bằng chức danh hiện tại tra qua `AdminProfileLookup` (cùng module `admin`), không tìm thấy thì giữ `STAFF` | ☐ |
| D5 | `result`: API có `SUCCESS, FAILED, DENIED`; cột `status` chỉ có `SUCCESS` / `FAILED` | API vs ENTITY | `DENIED` khi `action_type = ACCESS_DENIED`; còn lại lấy `status`. `summary.denied_count` = số dòng `ACCESS_DENIED` trong bộ lọc hiện tại | ☐ |
| D6 | Xuất CSV: giới hạn bao nhiêu dòng | API 11.10 (không ghi) | Tối đa 50.000 dòng / lần, ghi luồng (`StreamingResponseBody`), UTF-8 có BOM (mở được bằng Excel), tên tệp `nhat-ky-he-thong.csv`; vượt giới hạn → cắt và thêm header `X-Truncated: true` | ☐ |
| D7 | Tìm theo `q` (user / IP / mã tham chiếu) | UC-ADM-07 bước 2 | `ILIKE` trên `user_email`, `ip_address`, `reference_id`; không thêm index (bảng nhỏ ở quy mô TLCN, luôn kèm khoảng thời gian — mặc định 7 ngày, tối đa 90 ngày / lần truy vấn) | ☐ |

---

## 3. Cây file sẽ tạo / sửa

```
server-spring/src/main/java/com/genie/
├── common/audit/
│   ├── AuditCategory.java                      (mới — D2)
│   ├── AuditAction.java                        (sửa — thêm category())
│   └── SystemLogRepository.java                (sửa — truy vấn đọc keyset, đếm denied; KHÔNG có sửa / xóa)
└── admin/audit/
    ├── api/          AdminAuditLogController · AuditLogResponse (record) · AuditLogQuery (record tham số)
    └── application/  AuditLogQueryService · AuditLogCsvWriter · AuditLogView (record)
server-spring/src/test/java/com/genie/
├── common/audit/     AuditActionCategoryTest
├── admin/            AuditLogQueryIT · AuditLogExportIT · AuditLogPermissionIT
└── architecture/     (sửa) ModuleBoundaryTest / thêm luật "SystemLogRepository không có phương thức ghi đè dữ liệu"
web/src/features/admin-system/infrastructure/system.api.ts   (sửa — bỏ nhánh mock theo cờ, gọi export)
docs/Document/…  Usecase.md (UC-ADM-07 bước 1) · API_SPEC 16 (bảng ánh xạ category) · ENTITY Phụ lục A · CHECKLIST_TIEN_DO_UC.md · Quản Lý UseCase.xlsx
docs/web-backend-handoff.md · docs/api-tests/admin-audit-log.http
```

---

## 4. Bảng ánh xạ Use case → API → Dữ liệu

| Bước UC | Endpoint (API 11.10) | Bảng | Quyền |
|---|---|---|---|
| Mở Nhật ký hệ thống, lọc thời gian / nhóm / từ khóa, phân trang | `GET /admin/audit-logs?from&to&category&q&cursor&limit=50` | `system_logs` (đọc) | `VIEW_AUDIT_LOG` |
| Dòng tóm tắt "N lần truy cập bị từ chối" | cùng endpoint — `meta.summary.denied_count` | `system_logs` | `VIEW_AUDIT_LOG` |
| Xuất nhật ký | `GET /admin/audit-logs/export?from&to&category&q` → `text/csv` | `system_logs` | `VIEW_AUDIT_LOG` |

**Mã lỗi dùng (đã có trong `ErrorCode`, không thêm mới):** `GNE-SEC-4010`, `GNE-SEC-4030`, `GNE-VAL-4220` (sai `from` / `to` / `category` / `cursor`, khoảng thời gian > 90 ngày), `GNE-RAT-4290`.
**Không có migration, không thêm `UploadPurpose`, không thêm `AuditAction`.**

---

## 5. Nhánh, commit, PR

- Nhánh: `feature/admin-audit-log` tách từ `develop` (có mã Jira: `feature/GNE-<số>-admin-audit-log`). Một PR backend (≤ ~400 dòng) + một PR web nhỏ, hoặc gộp nếu vẫn dưới ngưỡng.
- Scope commit: `common` (enum + repository), `admin` (feature audit), `admin-ui` (nối web), `docs`.
- Quy ước chung: [README mục 5](../README.md).

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — unit + IT (Testcontainers) + ArchUnit xanh.
2. Swagger UI `http://localhost:8080/swagger-ui.html` hiện 2 endpoint, summary có `UC-ADM-07`.
3. Thử tay bằng `docs/api-tests/admin-audit-log.http`: Admin seed `yen.dh@genie.example` xem được; nhân viên `truc.ht@genie.example` (CS — không có `VIEW_AUDIT_LOG`) nhận `403`.
4. Nếu đụng `web/`: `cd web && npm run verify`.
5. Tick task ở progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Lxx.yy` = task; `Lxx.Cn` = task commit (làm sau khi qua mục 6).

### L00 — Chuẩn bị

- **[L00.01]** Kiểm danh tính git — `git config user.name` / `user.email` đúng tài khoản của mình.
- **[L00.02]** Chốt quyết định D1–D7 — ghi lựa chọn vào đầu file progress.
- **[L00.03]** Tạo nhánh — `git switch develop && git pull && git switch -c feature/admin-audit-log`.
- **[L00.04]** Dựng dữ liệu thử — chạy backend profile `local`, đăng nhập đúng / sai vài lần bằng các tài khoản seed, gọi một API `/admin/**` bằng tài khoản thiếu quyền để có dòng `ACCESS_DENIED`; `SELECT action_type, count(*) FROM system_logs GROUP BY 1` ghi vào Ghi chú.
- **[L00.05]** Đọc lại màn mock — mở `/admin/audit-logs` (mock) ghi các cột, bộ lọc, trạng thái rỗng, định dạng CSV hiện tại để API trả đủ trường (`web/src/features/admin-system`).

### L01 — Phân loại hành động (`common/audit`)

- **[L01.01]** `AuditCategory` — enum `AUTH, KYC, FINANCE, MODERATION, SECURITY, CONFIG` kèm chú thích nguồn (API 16).
- **[L01.02]** `AuditAction.category()` — ánh xạ theo bảng D2 (`switch` đủ nhánh, không `default` để thêm giá trị mới thì trình biên dịch bắt lỗi thiếu).
- **[L01.03]** `AuditCategory.actions()` — trả tập `AuditAction` thuộc nhóm (dùng cho điều kiện `action_type IN (…)`).
- **[L01.04]** `AuditActionCategoryTest` — mọi `AuditAction` có đúng một category; `actions()` của 6 nhóm hợp lại đủ 30 giá trị, không trùng.
- **[L01.C1]** COMMIT — `feat(common): classify audit actions into categories`

### L02 — Truy vấn đọc `system_logs`

- **[L02.01]** `SystemLogRepository` — truy vấn keyset: lọc `created_at` trong [`from`, `to`), `action_type IN (:actions)` (tùy chọn), `q` (`ILIKE` trên `user_email`, `ip_address`, `reference_id` — D7), điều kiện con trỏ `(created_at, id) < (:cursorTime, :cursorId)`, sắp `created_at DESC, id DESC`, `LIMIT :limit + 1` (dư 1 dòng để biết còn trang sau). Viết bằng `@Query` JPQL / Specification — không nối chuỗi SQL.
- **[L02.02]** `countDenied(from, to, actions, q)` — đếm dòng `ACCESS_DENIED` trong cùng bộ lọc (D5).
- **[L02.03]** `streamForExport(from, to, actions, q, max)` — `Stream<SystemLog>` với `@QueryHints(fetchSize)` để xuất CSV không nạp hết vào bộ nhớ (D6).
- **[L02.04]** Kiểm `EXPLAIN` trên CSDL local có vài nghìn dòng (chèn tạm bằng `generate_series`) — truy vấn dùng `idx_system_logs_created_action`; ghi kết quả vào Ghi chú. Nếu quét toàn bảng với bộ lọc mặc định → ghi nhận và đề xuất index (không tự thêm migration trong plan này).
- **[L02.05]** Luật kiến trúc — test khẳng định `SystemLogRepository` không có phương thức tên bắt đầu `delete` / `update` / có `@Modifying` (N2).
- **[L02.C1]** COMMIT — `feat(common): add read-only keyset queries for system logs`

### L03 — Dịch vụ & API tra cứu

- **[L03.01]** `AuditLogQuery` (record tham số) — `from`, `to` (ISO-8601; mặc định 7 ngày gần nhất tới hiện tại), `category` (enum, tùy chọn), `q` (cắt khoảng trắng, ≤ 100 ký tự), `cursor`, `limit` (1–100, mặc định 50). Validate: `to > from`, khoảng ≤ 90 ngày (D7) → sai trả `422 GNE-VAL-4220` có `errors[]` theo trường.
- **[L03.02]** Con trỏ — mã hóa / giải mã `cursor` = Base64URL của `createdAt|id` (dùng chung `CursorPage` của `common/api` nếu đã hỗ trợ; nếu chưa thì viết tiện ích nhỏ trong `admin/audit/application`); con trỏ hỏng → `422`.
- **[L03.03]** `AuditLogView` + ánh xạ từ `SystemLog`: `id`, `created_at`, `actor_email` (`system` khi `user_id` null — theo `system@genie.vn` mà `AuditLogger` ghi cho job), `actor_role` (D4), `action`, `category` (D2), `target` (D3), `ip_address`, `result` (D5).
- **[L03.04]** `AuditLogQueryService.search(query)` — gọi L02.01 + L02.02, dựng trang cursor (`has_more`, `next_cursor`) và `summary.denied_count`.
- **[L03.05]** Tra chức danh nhân viên (D4) — gom `user_id` của các dòng `STAFF` trong trang, tra một lần qua `AdminProfileLookup` (tránh N+1).
- **[L03.06]** `AdminAuditLogController` — `GET /api/v1/admin/audit-logs`, `@PreAuthorize("hasAuthority('VIEW_AUDIT_LOG')")`, `@Operation(summary = "UC-ADM-07 — Tra cứu nhật ký kiểm toán")`; header `Cache-Control: no-store`.
- **[L03.07]** `AuditLogQueryIT` — dữ liệu tạo bằng `AuditLogger` + `MutableClock`: lọc theo từng category; lọc thời gian biên (bao gồm `from`, loại trừ `to`); `q` theo email / IP / mã tham chiếu (không phân biệt hoa thường); phân trang 3 trang không trùng, không sót khi nhiều dòng cùng `created_at`; `denied_count` đúng; trạng thái rỗng trả mảng rỗng.
- **[L03.08]** `AuditLogPermissionIT` — Admin `200`; nhân viên có `VIEW_AUDIT_LOG` `200`; nhân viên không có quyền `403 GNE-SEC-4030` và sinh thêm đúng 1 dòng `ACCESS_DENIED`; khách hàng `403`; không token `401 GNE-SEC-4010`.
- **[L03.C1]** COMMIT — `feat(admin): search system audit logs with filters and cursor paging`

### L04 — Xuất CSV

- **[L04.01]** `AuditLogCsvWriter` — ghi luồng: BOM UTF-8, dòng tiêu đề tiếng Việt (Thời gian, User, Vai trò, Hành động, Nhóm, Đối tượng, IP, Kết quả), thoát dấu `"` / `,` / xuống dòng theo RFC 4180, **vô hiệu công thức** (ô bắt đầu bằng `= + - @` thì thêm `'` ở đầu — chống CSV injection), thời gian theo giờ Việt Nam `yyyy-MM-dd HH:mm:ss`.
- **[L04.02]** `GET /api/v1/admin/audit-logs/export` — cùng tham số lọc (không `cursor` / `limit`), `Content-Type: text/csv; charset=UTF-8`, `Content-Disposition: attachment; filename="nhat-ky-he-thong.csv"`, tối đa 50.000 dòng (D6), `@Transactional(readOnly = true)` bao quanh luồng đọc; `@PreAuthorize` như L03.06; `@Operation(summary = "UC-ADM-07 — Xuất nhật ký kiểm toán ra CSV")`.
- **[L04.03]** Giới hạn tần suất xuất — `RateLimiter` 5 lần / phút / người dùng (`rl:auditexport:{userId}`) → `429 GNE-RAT-4290`.
- **[L04.04]** `AuditLogExportIT` — nội dung khớp bộ lọc; có BOM; ô chứa dấu phẩy / nháy kép / xuống dòng được thoát đúng; ô bắt đầu bằng `=` bị vô hiệu; vượt giới hạn có header `X-Truncated`; thiếu quyền `403`.
- **[L04.C1]** COMMIT — `feat(admin): export filtered audit logs as csv`

### L05 — Nối giao diện `web/` (scope `admin-ui`)

- **[L05.01]** `web/src/features/admin-system/infrastructure/system.api.ts` — `listAuditLogs`: thay `USE_MOCK` bằng `isMocked('audit')` (cờ `PUBLIC_REAL_API=audit`); đối chiếu DTO với phản hồi thật (tên trường snake_case, `meta.pagination`, `meta.summary.denied_count`), sửa mapper nếu lệch.
- **[L05.02]** `auditNow` — khi nối thật dùng thời gian máy khách (đã có nhánh), kiểm bộ lọc "Hôm nay / 7 ngày / 30 ngày" gửi `from` / `to` đúng múi giờ.
- **[L05.03]** Xuất CSV — nhánh thật gọi `GET /admin/audit-logs/export` (tải blob kèm Bearer, đặt tên tệp theo `Content-Disposition`); giữ cách tạo CSV ở trình duyệt cho nhánh mock (handoff mục 4).
- **[L05.04]** Lỗi & trạng thái — `403` hiện trang "Không có quyền" của `RequirePermission`; `422` khoảng thời gian > 90 ngày hiện lỗi tại bộ lọc (thêm khóa i18n `admin-system.audit.range_too_long` ở `vi.json` + `en.json` nếu chưa có); `429` khi xuất liên tục.
- **[L05.05]** Chạy thật hai phía — web `PUBLIC_USE_MOCK=true` + `PUBLIC_REAL_API=auth,audit`, backend local: đăng nhập `yen.dh@genie.example`, mở `/admin/audit-logs`, lọc từng nhóm, cuộn trang, tìm theo email, xuất CSV mở bằng Excel thấy tiếng Việt đúng.
- **[L05.06]** `npm run verify` trong `web/`.
- **[L05.C1]** COMMIT — `feat(admin-ui): connect audit log screen to the real api`

### L06 — Hoàn thiện use case

- **[L06.01]** `docs/api-tests/admin-audit-log.http` — ≥ 1 kịch bản đúng (lọc + phân trang + xuất) và ≥ 1 kịch bản lỗi (`403`, `422`) (TS 14).
- **[L06.02]** OpenAPI — kiểm `/v3/api-docs` có 2 endpoint với mô tả tham số, mã lỗi; cập nhật `docs/openapi/openapi.json` nếu repo đang giữ bản xuất.
- **[L06.03]** Tài liệu nguồn `docs/Document` — Usecase.md UC-ADM-07 bước 1 sửa đường dẫn theo API (D1) và dòng *Hiện trạng*; API_SPEC 16 + ENTITY Phụ lục A thêm bảng ánh xạ `AuditAction` → `AuditCategory` (D2); API 11.10 ghi giới hạn 90 ngày / 50.000 dòng (D6, D7); ghi lịch sử phiên bản.
- **[L06.04]** Tiến độ — `CHECKLIST_TIEN_DO_UC.md`: tick GD / BE / Test / Nối / Chạy cho UC-ADM-07, đổi trạng thái, thêm dòng Nhật ký; `Quản Lý UseCase.xlsx`: cột *Hiện trạng mã nguồn* → "Đã triển khai".
- **[L06.05]** `docs/web-backend-handoff.md` — đánh dấu `listAuditLogs` đã nối; mục 4 bỏ dòng "nhật ký 11.13" khỏi danh sách xuất CSV tạm.
- **[L06.06]** `CLAUDE.md` mục 2 — ghi feature `admin/audit` đã có.
- **[L06.C1]** COMMIT — `docs(admin): record audit log use case as implemented`
- **[L06.07]** Mở PR vào `develop` — tiêu đề đúng chuẩn commit, điền mẫu PR (Ảnh hưởng CSDL: không có), đính ảnh màn Nhật ký chạy API thật.

---

## 8. Ngoài phạm vi

- Ghi nhật ký cho các phân hệ chưa làm (KYC, tài chính, kiểm duyệt…) — mỗi phân hệ tự gọi `AuditLogger` khi được hiện thực; màn này tự hiển thị thêm.
- Trigger CSDL chặn `UPDATE` / `DELETE` trên `system_logs`, phân vùng bảng theo tháng, job xóa nhật ký quá 12 tháng — DATN (ENTITY 13.2).
- Xem chi tiết `details` JSON đầy đủ của một dòng; đồng bộ nhật ký ra hệ thống ngoài.

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Thêm `AuditAction` mới ở plan khác mà quên phân loại | `category()` dùng `switch` không `default` → lỗi biên dịch; `AuditActionCategoryTest` kiểm đủ |
| Hai dòng cùng `created_at` làm phân trang sót / trùng | Keyset theo cặp (`created_at`, `id`); test có nhiều dòng trùng thời điểm (L03.07) |
| `q` dạng `ILIKE '%…%'` chậm khi bảng lớn | Bắt buộc kèm khoảng thời gian ≤ 90 ngày; ghi nhận ở L02.04, index trigram để DATN |
| CSV mở bằng Excel lỗi font / bị chèn công thức | BOM UTF-8 + vô hiệu công thức (L04.01), có test |
| `actor_role` lệch do nhân viên đổi chức danh sau khi ghi log | Chấp nhận: hiển thị chức danh **hiện tại** cho `STAFF` (D4), ghi rõ trong API 11.10 |
