# PROGRESS — Chuỗi C10 Báo cáo (UC-VND-08 · UC-ADM-06 · UC-VND-19)

| | |
|---|---|
| **Plan** | [plan_bao_cao_c10.md](./plan_bao_cao_c10.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-28 |
| **Use case** | Xem báo cáo doanh thu & Phân tích bán hàng (UC-VND-08) · Dashboard doanh thu & ký quỹ toàn sàn (UC-ADM-06) · Bật chế độ tạm nghỉ bán hàng của shop (UC-VND-19) |
| **Tổng** | 34 task + 8 commit, 8 giai đoạn |
| **Nhánh** | `feature/c10-vacation` (Q00–Q02) → `feature/c10-seller-analytics` (Q03) → `feature/c10-admin-dashboard` (Q04–Q07) |
| **Phụ trách** | _(điền kết quả `git config user.name` ở task Q00.01 — tên này dùng khi tick cột Phụ trách ở checklist / báo cáo / Excel)_ |
| **Người commit** | danh tính git đã cấu hình trên máy người làm — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Cập nhật gần nhất** | 2026-11-28 — lập plan, chưa bắt đầu (0 / 42) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục *Ghi chú*.
- Task `COMMIT` chỉ tick sau khi qua *Cổng kiểm tra* (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`. Thông điệp đã ghi sẵn — tiếng Anh, chữ thường, đúng `commitlint.config.cjs`; có mã Jira thì thêm ` [#GNE-<số>]` cuối.
- Sau mỗi buổi làm: cập nhật cột *Xong* của bảng Tổng quan, dòng *Cập nhật gần nhất*, một dòng *Nhật ký* cuối trang.
- Cột *Chạy* của use case chỉ tick khi đã chạy thật trên giao diện (Chromium điều khiển bằng Playwright, hệ thống thật) và đạt hết ca của kịch bản — ghi cách chạy + số ca vào *Ghi chú*.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | **Bật tạm nghỉ** (API 10.11) | ☐ | |
| D2 | **Cổng kiểm đơn mở** | ☐ | |
| D3 | **Khi đang nghỉ** (UC-VND-19 b3) | ☐ | |
| D4 | **Kết thúc** | ☐ | |
| D5 | **Tổng quan Seller** (API 10.2) | ☐ | |
| D6 | **Phân tích Seller** (API 10.9) | ☐ | |
| D7 | **Tổng quan Admin** (API 11.2) | ☐ | |
| D8 | **Sổ cái** (API 11.9) | ☐ | |
| D9 | **Định nghĩa "trong kỳ"** | ☐ | |
| D10 | **Hiệu năng** | ☐ | |
| D11 | **Cache** | ☐ | |
| D12 | **Quyền** | ☐ | |
| D13 | **Khi chưa có dữ liệu** | ☐ | |
| D14 | **Khối Cảm xúc** | ☐ | |
| D15 | **Cờ web** | ☐ | |
| D16 | **Ngoài phạm vi** | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| Q00 | Chuẩn bị | 5 | 0 | 0 / 5 |
| Q01 | Tài liệu trước | 2 | 1 | 0 / 3 |
| Q02 | Chế độ tạm nghỉ (UC-VND-19) | 5 | 1 | 0 / 6 |
| Q03 | Tổng quan & phân tích Seller (UC-VND-08) — nhánh `feature/c10-seller-analytics` | 4 | 1 | 0 / 5 |
| Q04 | Dashboard & Sổ cái Admin (UC-ADM-06) — nhánh `feature/c10-admin-dashboard` | 3 | 2 | 0 / 5 |
| Q05 | Nối web (cờ `vacation`, `seller-dashboard`, `seller-analytics`, `admin-dashboard`, `ledger`) | 4 | 1 | 0 / 5 |
| Q06 | Kiểm thử & chạy thật | 3 | 1 | 0 / 4 |
| Q07 | Hoàn thiện use case & tick tiến độ | 8 | 1 | 0 / 9 |
| **Tổng** | | **34** | **8** | **0 / 42** |

## Q00 — Chuẩn bị

- [ ] **Q00.01** Chạy `git config user.name` / `user.email`
- [ ] **Q00.02** Chốt D1–D16 vào progress.
- [ ] **Q00.03** Tạo nhánh `feature/c10-vacation` từ `develop`.
- [ ] **Q00.04** Đọc `PurchaseRules` / `CheckoutPricer` (đặt hàng đã chặn shop nghỉ chưa
- [ ] **Q00.05** Đọc màn mock Seller › Tổng quan, Seller › Phân tích (khoảng thời gian, KPI, biểu đồ, nguồn truy cập, top 5, kết quả đơn, sức khỏe), Admin › Tổng quan (2 tab), Admin › Sổ cái, Seller › Cài đặt › Chế độ nghỉ.

## Q01 — Tài liệu trước

- [ ] **Q01.01** API_SPEC
- [ ] **Q01.02** Usecase.md
- [ ] **Q01.C1** 🔖 COMMIT `docs(vendor): specify vacation mode rules and report metric definitions` · hash: `…`

## Q02 — Chế độ tạm nghỉ (UC-VND-19)

- [ ] **Q02.01** Cổng `VacationOrderGuard` + bản mặc định + `VacationOrderGuardImpl` (order) (D2).
- [ ] **Q02.02** `VacationService` + `SellerVacationController` (D1, D4) + evict sau commit (N6).
- [ ] **Q02.03** Chặn đặt hàng (D3a) nếu chưa có; ẩn voucher shop nghỉ (D3b).
- [ ] **Q02.04** `VacationEndJob` (D4).
- [ ] **Q02.05** Test
- [ ] **Q02.C1** 🔖 COMMIT `feat(vendor): let sellers start and end vacation mode with open order checks` · hash: `…`

## Q03 — Tổng quan & phân tích Seller (UC-VND-08) — nhánh `feature/c10-seller-analytics`

- [ ] **Q03.01** `ReportRange` (preset / custom, giờ VN, kỳ so sánh) + `ReportFixtures` (test).
- [ ] **Q03.02** `SellerReportQueries`
- [ ] **Q03.03** `SellerDashboardService` + `SellerAnalyticsService` + 2 controller (5 endpoint) + cache (D11).
- [ ] **Q03.04** Test
- [ ] **Q03.C1** 🔖 COMMIT `feat(vendor): serve seller dashboard and sales analytics from order read models` · hash: `…`

## Q04 — Dashboard & Sổ cái Admin (UC-ADM-06) — nhánh `feature/c10-admin-dashboard`

- [ ] **Q04.01** `AdminDashboardQueries` + `AdminDashboardService` + `AdminDashboardController` (5 endpoint D7, quyền D12, cache D11) + `DashboardCsv`.
- [ ] **Q04.02** `LedgerEntryQuery` + `AdminLedgerController` (payment) + `LedgerCsv` (stream) (D8).
- [ ] **Q04.03** Test
- [ ] **Q04.C1** 🔖 COMMIT `feat(admin): serve platform finance and operations dashboards` · hash: `…`
- [ ] **Q04.C2** 🔖 COMMIT `feat(payment): list and export financial ledger entries for staff` · hash: `…`

## Q05 — Nối web (cờ `vacation`, `seller-dashboard`, `seller-analytics`, `admin-dashboard`, `ledger`)

- [ ] **Q05.01** `seller-settings/settings.api.ts` (`startVacation`, `endVacation`)
- [ ] **Q05.02** `seller-dashboard` + `seller-analytics` (4 hàm; `getSentiment` đã nối ở C7)
- [ ] **Q05.03** `admin-dashboard` (4 hàm + CSV) + `admin-finance` Sổ cái (`listLedger`, `listAllLedger` → cursor, lọc, xuất).
- [ ] **Q05.04** vi / en + `error-codes.ts`; test mapper; kiểm 9 theme (biểu đồ đọc được ở theme tối), 360 px; `npm run verify`.
- [ ] **Q05.C1** 🔖 COMMIT `feat(web): connect vacation mode, seller analytics, admin dashboard and ledger to the real api` · hash: `…`

## Q06 — Kiểm thử & chạy thật

- [ ] **Q06.01** `docs/api-tests/reports.http`
- [ ] **Q06.02** `docs/kiem-thu/kich-ban-2026-11-28-c10.md`
- [ ] **Q06.03** Chạy thật
- [ ] **Q06.C1** 🔖 COMMIT `docs(vendor): add report api tests and real-run scenarios` · hash: `…`

## Q07 — Hoàn thiện use case & tick tiến độ

- [ ] **Q07.01** OpenAPI
- [ ] **Q07.02** `docs/web-backend-handoff.md`
- [ ] **Q07.03** Usecase.md
- [ ] **Q07.04** `CHECKLIST_TIEN_DO_UC.md`
- [ ] **Q07.05** `BAO_CAO_UC_DOC_LAP.md`
- [ ] **Q07.06** `Quản Lý UseCase.xlsx`
- [ ] **Q07.07** README ngày (mục 7)
- [ ] **Q07.C1** 🔖 COMMIT `docs(vendor): record vacation mode, seller analytics and platform dashboard as implemented` · hash: `…`
- [ ] **Q07.08** Mở 3 PR xếp chồng vào `develop` (`c10-vacation` → `c10-seller-analytics` → `c10-admin-dashboard`).

## Ghi chú

| Task | Ghi chú |
|---|---|
| | |

## Nhật ký

| Ngày | Việc đã làm | Commit / PR |
|---|---|---|
| 2026-11-28 | Lập plan + progress (34 task, 8 commit, 16 quyết định chờ chốt) | nhánh `feature/plan-c3-c11` |
