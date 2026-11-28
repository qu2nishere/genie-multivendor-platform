# PROGRESS — Chuỗi C9 Ví & rút tiền (UC-VND-20 · UC-ADM-04 · UC-ADM-05)

| | |
|---|---|
| **Plan** | [plan_vi_rut_tien_c9.md](./plan_vi_rut_tien_c9.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-28 |
| **Use case** | Xem ví & sổ giao dịch người bán (UC-VND-20) · Người bán gửi yêu cầu rút tiền từ Ví về Ngân hàng (UC-ADM-04) · Nhân viên sàn phê duyệt & Thực hiện lệnh chuyển khoản — Payout (UC-ADM-05) |
| **Tổng** | 34 task + 8 commit, 8 giai đoạn |
| **Nhánh** | `feature/c9-seller-wallet` (I00–I03) → `feature/c9-payouts` (I04–I07) |
| **Phụ trách** | _(điền kết quả `git config user.name` ở task I00.01 — tên này dùng khi tick cột Phụ trách ở checklist / báo cáo / Excel)_ |
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
| D1 | **Khối tổng quan ví** (API 10.10 `GET /seller/wallet`) | ☐ | |
| D2 | **Khi nào trừ số dư** — UC-ADM-04 b5 ("trừ `balance`, cộng `pending`") lệch entity (`pending` chỉ giữ chỗ; `availableBalance = balance − pending`) | ☐ | |
| D3 | **Điều kiện tạo lệnh** | ☐ | |
| D4 | **Một lệnh chờ tại một thời điểm** | ☐ | |
| D5 | **Xác thực bước cao** | ☐ | |
| D6 | **Sổ giao dịch** (API 10.10) | ☐ | |
| D7 | **Xuất CSV** | ☐ | |
| D8 | **Migration** | ☐ | |
| D9 | **Duyệt & chuyển khoản** (API 11.6) | ☐ | |
| D10 | **Khớp SePay tiền ra** | ☐ | |
| D11 | **Hoàn tất** (một transaction) | ☐ | |
| D12 | **Cài `PayoutAccountGuard`** | ☐ | |
| D13 | **Thông báo** | ☐ | |
| D14 | **Cờ web** | ☐ | |
| D15 | **Ngoài phạm vi** | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| I00 | Chuẩn bị | 5 | 0 | 0 / 5 |
| I01 | Tài liệu trước | 3 | 1 | 0 / 4 |
| I02 | Ví & sổ giao dịch (UC-VND-20) | 4 | 1 | 0 / 5 |
| I03 | Tạo lệnh rút (UC-ADM-04) | 4 | 2 | 0 / 6 |
| I04 | Duyệt & chuyển khoản (UC-ADM-05) — nhánh `feature/c9-payouts` | 4 | 1 | 0 / 5 |
| I05 | Nối web (cờ `seller-wallet`, `admin-withdrawals`) | 3 | 1 | 0 / 4 |
| I06 | Kiểm thử & chạy thật | 3 | 1 | 0 / 4 |
| I07 | Hoàn thiện use case & tick tiến độ | 8 | 1 | 0 / 9 |
| **Tổng** | | **34** | **8** | **0 / 42** |

## I00 — Chuẩn bị

- [ ] **I00.01** Chạy `git config user.name` / `user.email`
- [ ] **I00.02** Chốt D1–D15 vào progress.
- [ ] **I00.03** Tạo nhánh `feature/c9-seller-wallet` từ `develop`.
- [ ] **I00.04** Đọc cơ chế bước cao 🔐 của AUTH (`purpose`, header, thời hạn), `PayoutAccountGuard` + chỗ vendor gọi, `AuditLogCsvWriter` (có tách lên `common` được không
- [ ] **I00.05** Đọc màn mock Seller › Ví & Escrow (4 thẻ số, khối "Cách tính tiền về ví", sổ giao dịch lọc tháng / loại, CSV, hộp Rút tiền + OTP) và Admin › Xử lý rút tiền (tab, chi tiết, modal "Chờ chuyển khoản" có QR, từ chối có lý do, xác nhận tay).

## I01 — Tài liệu trước

- [ ] **I01.01** ENTITY 6.4 (luồng giữ / chốt theo D2
- [ ] **I01.02** API_SPEC
- [ ] **I01.03** Usecase.md
- [ ] **I01.C1** 🔖 COMMIT `docs(payment): specify seller wallet statements, withdrawal holds and payout confirmation` · hash: `…`

## I02 — Ví & sổ giao dịch (UC-VND-20)

- [ ] **I02.01** `SellerWalletQuery`
- [ ] **I02.02** `WalletStatementCsv` + `CsvWriter` (D7).
- [ ] **I02.03** `SellerWalletController`
- [ ] **I02.04** Test
- [ ] **I02.C1** 🔖 COMMIT `feat(payment): show seller wallet balances and a merged transaction statement` · hash: `…`

## I03 — Tạo lệnh rút (UC-ADM-04)

- [ ] **I03.01** `V33__c9_withdrawals.sql` (D8) + entity / repo + `ErrorCode`.
- [ ] **I03.02** `WithdrawalService.request` (D2–D5) + `POST /seller/withdrawals` 🔁 🔐.
- [ ] **I03.03** `PaymentPayoutAccountGuard` (D12) + thông báo (D13).
- [ ] **I03.04** Test
- [ ] **I03.C1** 🔖 COMMIT `feat(db): add withdrawal transfer codes and approval columns` · hash: `…`
- [ ] **I03.C2** 🔖 COMMIT `feat(payment): let sellers request withdrawals that hold available balance` · hash: `…`

## I04 — Duyệt & chuyển khoản (UC-ADM-05) — nhánh `feature/c9-payouts`

- [ ] **I04.01** `WithdrawalService.approve / reject / complete` (D9, D11) + `AdminWithdrawalController` (5 endpoint 11.6, `PROCESS_PAYOUT`).
- [ ] **I04.02** `WithdrawalTransferMatcher` (D10) đăng ký vào `BankTransferRouter`.
- [ ] **I04.03** Hàng đợi `UNCONFIRMED` (truy vấn `APPROVED` + `approved_at < now − 24h`), AdminNotification nhắc mỗi ngày (job 09:00 giờ VN).
- [ ] **I04.04** Test
- [ ] **I04.C1** 🔖 COMMIT `feat(payment): let finance staff approve payouts and confirm transfers through sepay` · hash: `…`

## I05 — Nối web (cờ `seller-wallet`, `admin-withdrawals`)

- [ ] **I05.01** `seller-wallet/wallet.api.ts` (3 hàm + CSV)
- [ ] **I05.02** `admin-finance/finance.api.ts` (5 hàm rút tiền)
- [ ] **I05.03** vi / en + `error-codes.ts`; test mapper; kiểm 9 theme, 360 px; `npm run verify`.
- [ ] **I05.C1** 🔖 COMMIT `feat(web): connect the seller wallet and payout processing to the real api` · hash: `…`

## I06 — Kiểm thử & chạy thật

- [ ] **I06.01** `docs/api-tests/wallet.http`
- [ ] **I06.02** `docs/kiem-thu/kich-ban-2026-11-28-c9.md`
- [ ] **I06.03** Chạy thật
- [ ] **I06.C1** 🔖 COMMIT `docs(payment): add wallet and payout api tests and real-run scenarios` · hash: `…`

## I07 — Hoàn thiện use case & tick tiến độ

- [ ] **I07.01** OpenAPI
- [ ] **I07.02** `docs/web-backend-handoff.md`
- [ ] **I07.03** Usecase.md
- [ ] **I07.04** `CHECKLIST_TIEN_DO_UC.md`
- [ ] **I07.05** `BAO_CAO_UC_DOC_LAP.md`
- [ ] **I07.06** `Quản Lý UseCase.xlsx`
- [ ] **I07.07** README ngày (mục 7)
- [ ] **I07.C1** 🔖 COMMIT `docs(payment): record seller wallet, withdrawals and payouts as implemented` · hash: `…`
- [ ] **I07.08** Mở 2 PR xếp chồng vào `develop` (`c9-seller-wallet` → `c9-payouts`).

## Ghi chú

| Task | Ghi chú |
|---|---|
| | |

## Nhật ký

| Ngày | Việc đã làm | Commit / PR |
|---|---|---|
| 2026-11-28 | Lập plan + progress (34 task, 8 commit, 15 quyết định chờ chốt) | nhánh `feature/plan-c3-c11` |
