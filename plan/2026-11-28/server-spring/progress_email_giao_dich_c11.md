# PROGRESS — Chuỗi C11 Email giao dịch (UC-NOT-03)

| | |
|---|---|
| **Plan** | [plan_email_giao_dich_c11.md](./plan_email_giao_dich_c11.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-28 |
| **Use case** | Gửi email giao dịch tự động (UC-NOT-03) |
| **Tổng** | 35 task + 12 commit, 12 giai đoạn |
| **Nhánh** | `feature/c11-email-outbox` (J00–J04) → `feature/c11-transaction-emails` (J05–J11) |
| **Phụ trách** | _(điền kết quả `git config user.name` ở task J00.01 — tên này dùng khi tick cột Phụ trách ở checklist / báo cáo / Excel)_ |
| **Người commit** | danh tính git đã cấu hình trên máy người làm — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Cập nhật gần nhất** | 2026-11-28 — lập plan, chưa bắt đầu (0 / 47) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục *Ghi chú*.
- Task `COMMIT` chỉ tick sau khi qua *Cổng kiểm tra* (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`. Thông điệp đã ghi sẵn — tiếng Anh, chữ thường, đúng `commitlint.config.cjs`; có mã Jira thì thêm ` [#GNE-<số>]` cuối.
- Sau mỗi buổi làm: cập nhật cột *Xong* của bảng Tổng quan, dòng *Cập nhật gần nhất*, một dòng *Nhật ký* cuối trang.
- Cột *Chạy* của use case chỉ tick khi đã chạy thật trên giao diện (Chromium điều khiển bằng Playwright, hệ thống thật) và đạt hết ca của kịch bản — ghi cách chạy + số ca vào *Ghi chú*.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | **Nơi lưu outbox / nhật ký** | ☐ | |
| D2 | **Migration** | ☐ | |
| D3 | **Hộp thư không được hiện dòng EMAIL** | ☐ | |
| D4 | **API của outbox** | ☐ | |
| D5 | **Backoff** | ☐ | |
| D6 | **Email hiện có** (7 nơi gọi `EmailSender`) | ☐ | |
| D7 | **Danh sách email giao dịch mới** (API 12.2) | ☐ | |
| D8 | **Ai gọi** (N7) | ☐ | |
| D9 | **Người nhận & ngôn ngữ** | ☐ | |
| D10 | **Chống gửi trùng** | ☐ | |
| D11 | **Xem trước mẫu** | ☐ | |
| D12 | **Theo dõi** | ☐ | |
| D13 | **Ngoài phạm vi** | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| J00 | Chuẩn bị | 4 | 0 | 0 / 4 |
| J01 | Tài liệu trước | 1 | 1 | 0 / 2 |
| J02 | Migration & hộp thư | 3 | 2 | 0 / 5 |
| J03 | Outbox, gửi bất đồng bộ & thử lại | 4 | 1 | 0 / 5 |
| J04 | Mẫu HTML | 4 | 1 | 0 / 5 |
| J05 | Email thanh toán & đơn mới (C3) — nhánh `feature/c11-transaction-emails` | 3 | 1 | 0 / 4 |
| J06 | Email hủy / từ chối đơn (C4, C5, C2) | 2 | 1 | 0 / 3 |
| J07 | Email trả hàng & hoàn tiền (C8) | 2 | 1 | 0 / 3 |
| J08 | Email rút tiền (C9) | 2 | 1 | 0 / 3 |
| J09 | Chuyển email hiện có sang outbox (D6) | 2 | 1 | 0 / 3 |
| J10 | Kiểm thử & chạy thật | 2 | 1 | 0 / 3 |
| J11 | Hoàn thiện use case & tick tiến độ | 6 | 1 | 0 / 7 |
| **Tổng** | | **35** | **12** | **0 / 47** |

## J00 — Chuẩn bị

- [ ] **J00.01** Chạy `git config user.name` / `user.email`
- [ ] **J00.02** Chốt D1–D13 vào progress.
- [ ] **J00.03** Tạo nhánh `feature/c11-email-outbox` từ `develop`.
- [ ] **J00.04** Đọc `SmtpEmailSender`, `MailTemplate`, `MailBranding`, 15 tệp `templates/email/*.html` (đã có layout chung chưa), 7 nơi gọi `EmailSender`; CHECK `notifications.channel`, `delivery_status`; mọi truy vấn của `InboxReadModel` (D3).

## J01 — Tài liệu trước

- [ ] **J01.01** ENTITY 10.1 (cột D2, luồng outbox, backoff D5, chống trùng D10), Phụ lục E; API_SPEC 12.2 (bảng email thêm cột *mẫu* + *thời điểm gửi*, chu kỳ job); Usecase.md UC-NOT-03 (outbox, OTP gửi ngay
- [ ] **J01.C1** 🔖 COMMIT `docs(notification): specify the transactional email outbox and retry policy` · hash: `…`

## J02 — Migration & hộp thư

- [ ] **J02.01** `V34__c11_email_outbox.sql` (D2, UNIQUE D10).
- [ ] **J02.02** `Notification` entity (cột mới, `markFailed(error, nextAt)`, `markSent(at)`).
- [ ] **J02.03** `InboxReadModel` lọc `channel = IN_APP` (D3) + `InboxExcludesEmailIT`.
- [ ] **J02.C1** 🔖 COMMIT `feat(db): add email outbox scheduling columns to notifications` · hash: `…`
- [ ] **J02.C2** 🔖 COMMIT `fix(notification): keep email rows out of the in-app inbox` · hash: `…`

## J03 — Outbox, gửi bất đồng bộ & thử lại

- [ ] **J03.01** `MailExecutorConfig` (4 luồng, hàng đợi 500, từ chối → để job nhặt), `TransactionalMailer.queue / logSent / logFailed` (D4, D6, D9, D10).
- [ ] **J03.02** `MailDispatcher` (`AFTER_COMMIT`) + `MailBackoff` (D5) + `SmtpEmailSender` bỏ thử lại trong luồng.
- [ ] **J03.03** `EmailRetryJob` (D5, `SKIP LOCKED`).
- [ ] **J03.04** Test
- [ ] **J03.C1** 🔖 COMMIT `feat(notification): queue transactional emails in an outbox and retry failures` · hash: `…`

## J04 — Mẫu HTML

- [ ] **J04.01** `layout/base.html` (N4) + `fragments/order-items.html`; chuyển 15 mẫu cũ sang layout (giữ nội dung).
- [ ] **J04.02** 11 mẫu mới (D7) + `MailTemplate` + chuỗi vi / en (D9).
- [ ] **J04.03** `DevMailPreviewController` (D11).
- [ ] **J04.04** Test
- [ ] **J04.C1** 🔖 COMMIT `feat(notification): add a responsive email layout and transactional templates` · hash: `…`

## J05 — Email thanh toán & đơn mới (C3) — nhánh `feature/c11-transaction-emails`

- [ ] **J05.01** `PaymentMailListener` (payment)
- [ ] **J05.02** `order`
- [ ] **J05.03** Test
- [ ] **J05.C1** 🔖 COMMIT `feat(payment): email buyers and sellers when orders are paid or payments fail` · hash: `…`

## J06 — Email hủy / từ chối đơn (C4, C5, C2)

- [ ] **J06.01** `OrderMailListener` (order)
- [ ] **J06.02** Test
- [ ] **J06.C1** 🔖 COMMIT `feat(order): email buyers when orders are rejected, cancelled or expire` · hash: `…`

## J07 — Email trả hàng & hoàn tiền (C8)

- [ ] **J07.01** `RefundMailListener` (support)
- [ ] **J07.02** Test
- [ ] **J07.C1** 🔖 COMMIT `feat(support): email return request updates, dispute results and completed refunds` · hash: `…`

## J08 — Email rút tiền (C9)

- [ ] **J08.01** `WithdrawalMailListener` (payment)
- [ ] **J08.02** Test
- [ ] **J08.C1** 🔖 COMMIT `feat(payment): email sellers about withdrawal approval, rejection and completion` · hash: `…`

## J09 — Chuyển email hiện có sang outbox (D6)

- [ ] **J09.01** 7 nơi gọi `EmailSender`: OTP + bảo mật → gửi ngay + `logSent / logFailed`; KYC, duyệt sản phẩm, xóa TK, mời nhân viên, TK nhận tiền, mở khóa → `TransactionalMailer.queue`.
- [ ] **J09.02** Test hồi quy các IT cũ (`RegisterFlowIT`, KYC, duyệt sản phẩm, mời nhân viên…) xanh + có dòng nhật ký.
- [ ] **J09.C1** 🔖 COMMIT `refactor(notification): route existing emails through the outbox log` · hash: `…`

## J10 — Kiểm thử & chạy thật

- [ ] **J10.01** `docs/kiem-thu/kich-ban-2026-11-28-c11.md`
- [ ] **J10.02** Chạy thật
- [ ] **J10.C1** 🔖 COMMIT `docs(notification): add transactional email real-run scenarios` · hash: `…`

## J11 — Hoàn thiện use case & tick tiến độ

- [ ] **J11.01** Usecase.md
- [ ] **J11.02** `CHECKLIST_TIEN_DO_UC.md`
- [ ] **J11.03** `BAO_CAO_UC_DOC_LAP.md`
- [ ] **J11.04** `Quản Lý UseCase.xlsx`
- [ ] **J11.05** README ngày (mục 7)
- [ ] **J11.C1** 🔖 COMMIT `docs(notification): record transactional emails as implemented` · hash: `…`
- [ ] **J11.06** Mở 2 PR xếp chồng vào `develop` (`c11-email-outbox` → `c11-transaction-emails`).

## Ghi chú

| Task | Ghi chú |
|---|---|
| | |

## Nhật ký

| Ngày | Việc đã làm | Commit / PR |
|---|---|---|
| 2026-11-28 | Lập plan + progress (35 task, 12 commit, 13 quyết định chờ chốt) | nhánh `feature/plan-c3-c11` |
