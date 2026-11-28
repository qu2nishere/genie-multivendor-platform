# PLAN — Chuỗi C11 Email giao dịch (UC-NOT-03)

| | |
|---|---|
| **Ngày lập** | 2026-11-28 |
| **Thể loại** | server-spring (module `notification` feature `email` (mới: outbox, gửi bất đồng bộ, thử lại, nhật ký); listener gửi mail đặt ở module nguồn `order`, `payment`, `support`, `vendor`) + mẫu HTML `templates/email/` |
| **Use case** | Gửi email giao dịch tự động (UC-NOT-03) |
| **Tài liệu chuẩn** | UC-NOT-03 (Usecase.md dòng 2495) · API 12.2 (bảng *Email tự động* + "Gửi lại email lỗi (tối đa 3 lần) — 5 phút") · ENTITY 10.1 `Notification` (`channel` `IN_APP` / `EMAIL`, `delivery_status`, `retry_count`, `markFailed()`), 12.1 `SystemLog` · `docs/cau-hinh-moi-truong.md` (SMTP, Mailpit) — xem [README](../README.md) |
| **Theo dõi tiến độ** | [progress_email_giao_dich_c11.md](progress_email_giao_dich_c11.md) — cùng mã task `Jxx.yy` |
| **Phụ trách** | Người làm plan — lấy theo **`git config user.name`** trên máy thực hiện (ghi vào progress ở J00.01); không ghi tên cố định |
| **Người commit** | danh tính git đã cấu hình trên máy đó — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Phụ thuộc** | Hạ tầng **J01–J04 không phụ thuộc** chuỗi nào (làm ngay được). Giai đoạn J05–J08 nối email cho sự kiện của **C3** (thanh toán), **C4 / C5** (từ chối / hủy), **C8** (trả hàng / hoàn tiền), **C9** (rút tiền) — làm phần nào khi chuỗi đó đã merge |
| **Mục tiêu** | UC-NOT-03 ⬜ → ✅. Mọi email giao dịch ở bảng API 12.2 đi qua **một** outbox: ghi trong transaction nghiệp vụ, gửi sau commit trên luồng riêng 4 luồng, lỗi thử lại tối đa 3 lần (backoff), có nhật ký; mẫu HTML responsive thống nhất |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `1b66eae`, 2026-11-28)

| Phần | Đã có | Còn thiếu |
|---|---|---|
| **Gửi mail** | `common/mail/EmailSender` (cổng), `SmtpEmailSender` (Thymeleaf + `JavaMailSender`, `@Retryable` 3 lần 2 / 4 / 8 giây **trong cùng luồng gọi**), `MailTemplate` (11 mẫu: `PASSWORD_CHANGED`, `NEW_DEVICE_LOGIN`, `EMAIL_CHANGED`, `DELETION_SCHEDULED`, `KYC_APPROVED`, `KYC_REJECTED`, `KYC_SUPPLEMENT`, `PAYOUT_ACCOUNT_CHANGED`, `STAFF_INVITATION`, `ACCOUNT_REACTIVATED`, `PRODUCT_APPROVED`, …), `MailBranding`; tệp mẫu `templates/email/*.html` (có sẵn **`order-confirmed.html`**, **`refund.html`** chưa dùng); 7 nơi gọi (`OtpChallengeService`, `AccountMailListener`, `AccountDeletionListener`, `UserFacade`, `ProductModerationMailer`, `KycNotifier`, `PayoutAccountService`) | ⚠ **Không nhật ký** gửi; lỗi sau 3 lần là mất (process tắt giữa chừng cũng mất); thử lại chặn luồng của listener; không có mẫu cho đơn hàng / thanh toán / trả hàng / rút tiền |
| **Bảng** | `notifications` có `channel`, `delivery_status`, `retry_count`, `sent_at` (ENTITY 10.1 thiết kế sẵn cho email) | Cột lịch thử lại + lỗi cuối + khóa mẫu (D2) |
| **Hộp thư** | `InboxReadService` / `InboxReadModel` đọc `notifications` | ⚠ Phải **lọc `channel = IN_APP`** khi bắt đầu ghi dòng `EMAIL` (D3) |
| **Phụ thuộc module** | `order → notification`, `vendor → notification`, `promotion → notification`, `social → notification`; C3 / C8 thêm `payment → notification`, `support → notification` | **`notification` không import module nguồn** (vòng) — listener gửi mail nằm ở module nguồn (README mục 3) |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | **Outbox**: email được "xếp hàng" bằng một dòng `notifications` (`channel = EMAIL`, `delivery_status = PENDING`) **trong transaction nghiệp vụ** → rollback thì không có mail; commit xong mới gửi | UC-NOT-03 b2; TS 4 |
| N2 | Gửi trên **luồng riêng** (`ThreadPoolTaskExecutor` 4 luồng, hàng đợi có giới hạn), không chặn request / listener | UC-NOT-03 b2 |
| N3 | Lỗi → `markFailed()` (`retry_count + 1`, `next_attempt_at` theo backoff); job 5 phút gửi lại tối đa **3** lần; quá 3 → `FAILED` vĩnh viễn + log ERROR (không lặp vô hạn) | UC-NOT-03 b5; API 12.2 |
| N4 | Mẫu HTML **responsive** (một layout chung, bảng ≤ 600 px, font hệ thống, ảnh có `alt`), tiền / ngày qua cùng định dạng với web (VND, giờ VN), có bản chữ thuần (multipart) | UC-NOT-03 b3 |
| N5 | Không đưa dữ liệu nhạy cảm vào mail: số TK che giữa, không gửi mật khẩu / token đầy đủ; OTP chỉ 6 số + hạn | TS 9 |
| N6 | **OTP / bảo mật giữ đường gửi ngay** (người dùng đang chờ) nhưng vẫn **ghi nhật ký** qua outbox ở trạng thái đã gửi / lỗi (D6) | UC-NOT-03 b1 |
| N7 | `notification` chỉ cung cấp `TransactionalMailer`; **module nguồn** gọi từ listener của mình (tiền lệ `CheckoutNotificationListener`) | README mục 3 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Nơi lưu outbox / nhật ký** | ENTITY 10.1 (`channel EMAIL`, `retryCount`, `markFailed`) | Dùng chính bảng `notifications` với `channel = EMAIL` (đúng thiết kế ENTITY — không thêm bảng mới); `title` = tiêu đề mail, `body` = bản chữ thuần rút gọn (để tra cứu), `metadata_json` = biến của mẫu (không chứa dữ liệu nhạy cảm — N5), `reference_type / reference_id` = đối tượng nghiệp vụ (đơn, yêu cầu hoàn, lệnh rút) | ☐ |
| D2 | **Migration** | N3 | `V34`: `notifications.next_attempt_at`, `last_error` (≤ 500 ký tự), `template_key`, `recipient_email` (chụp lúc xếp hàng — đổi email sau không gửi nhầm), index `(channel, delivery_status, next_attempt_at)`; kiểm CHECK `delivery_status` có `PENDING / SENT / FAILED` (thêm nếu thiếu) | ☐ |
| D3 | **Hộp thư không được hiện dòng EMAIL** | UC-NOT-01 | Mọi truy vấn của `InboxReadModel` / đếm chưa đọc / đánh dấu đã đọc thêm `channel = 'IN_APP'` — task riêng + IT hồi quy (J03.04) | ☐ |
| D4 | **API của outbox** | N7 | `TransactionalMailer.queue(MailCommand{userId?, email, template, variables, locale, reference})` — `@Transactional(propagation = MANDATORY)`; trả id; `MailDispatcher` nghe `MailQueuedEvent` `AFTER_COMMIT` → executor → `EmailSender.send` → `SENT` + `sent_at` / `markFailed` + `last_error`. Bỏ `@Retryable` trong luồng (đổi `SmtpEmailSender` chỉ thử 1 lần — thử lại do outbox lo) | ☐ |
| D5 | **Backoff** | UC-NOT-03 b5 | Lần thử 1 ngay sau commit; lỗi → lần 2 sau 1 phút, lần 3 sau 5 phút, lần 4 sau 25 phút (tối đa **3 lần gửi lại**); `EmailRetryJob` 5 phút (`SKIP LOCKED`, lô 50) nhặt `PENDING` / `FAILED` có `next_attempt_at <= now` và `retry_count < 3` — cũng cứu được mail `PENDING` bị mất do tắt máy giữa chừng (quá 2 phút chưa gửi) | ☐ |
| D6 | **Email hiện có** (7 nơi gọi `EmailSender`) | N6 | OTP (`OtpChallengeService`) + cảnh báo bảo mật (`NEW_DEVICE_LOGIN`, `PASSWORD_CHANGED`, `EMAIL_CHANGED`): **gửi ngay** như cũ + ghi nhật ký (`TransactionalMailer.logSent / logFailed` — dòng `EMAIL` trạng thái cuối); còn lại (KYC, duyệt sản phẩm, xóa tài khoản, mời nhân viên, TK nhận tiền, mở khóa) **chuyển sang outbox**. Hành vi người dùng không đổi | ☐ |
| D7 | **Danh sách email giao dịch mới** (API 12.2) | UC-NOT-03 b1 | Khách: `ORDER_PLACED` (đặt thành công — mã phiên, từng đơn con, bảng hàng, tổng; COD gửi khi đặt, online gửi khi trả tiền thành công — "hóa đơn điện tử" dùng `order-confirmed.html`), `PAYMENT_FAILED` (link thanh toán lại, hạn giữ hàng), `ORDER_CANCELLED` (shop từ chối / khách hủy / tự hủy quá hạn — lý do, tiền / Điểm / voucher đã hoàn), `REFUND_REQUEST_UPDATE` (shop đồng ý / từ chối / yêu cầu bổ sung — hạn phản hồi), `REFUND_COMPLETED` (`refund.html` — số tiền, phương thức, mã giao dịch); Shop: `NEW_ORDER_FOR_VENDOR` (mã đơn, hạn xác nhận 24 giờ), `REFUND_REQUEST_NEW` (hạn 48 giờ), `DISPUTE_RESULT`, `WITHDRAWAL_APPROVED` / `WITHDRAWAL_REJECTED` (lý do) / `WITHDRAWAL_COMPLETED` (số tiền, TK che, mã giao dịch ngân hàng) | ☐ |
| D8 | **Ai gọi** (N7) | README mục 3 | `order`: `OrderMailListener` (đặt COD, hủy, từ chối, tự hủy — nghe event của chính order); `payment`: `PaymentMailListener` (trả tiền thành công / thất bại), `WithdrawalMailListener`; `support`: `RefundMailListener`; mỗi listener `@TransactionalEventListener(BEFORE_COMMIT)` hoặc gọi `queue` ngay trong service (để dòng outbox cùng transaction — N1) | ☐ |
| D9 | **Người nhận & ngôn ngữ** | UC-NOT-03 tiền điều kiện | Email lấy từ `UserFacade` lúc xếp hàng (chụp vào `recipient_email`); tài khoản không có email hợp lệ / `ANONYMIZED` → không xếp hàng (log DEBUG); ngôn ngữ theo `users.locale` (mặc định `vi`) — mẫu `vi` bắt buộc, `en` dùng chung bố cục với chuỗi dịch | ☐ |
| D10 | **Chống gửi trùng** | N1 | `UNIQUE (user_id, template_key, reference_type, reference_id) WHERE channel = 'EMAIL'` cho các mẫu "một lần" (đặt hàng, hoàn tiền xong, rút xong); mẫu có thể lặp (thanh toán thất bại nhiều lần) không đặt UNIQUE nhưng chặn trong 10 phút ở `queue` | ☐ |
| D11 | **Xem trước mẫu** | N4 | `GET /api/v1/dev/mail-preview/{template}?locale=vi` (`@Profile("local")`) dựng HTML với dữ liệu mẫu — để duyệt giao diện / kiểm 360 px mà không gửi thật | ☐ |
| D12 | **Theo dõi** | UC-NOT-03 hậu điều kiện ("ghi log gửi email") | Nhật ký = các dòng `EMAIL` (trạng thái, số lần, lỗi cuối) — tra bằng SQL; màn Admin tra cứu email **ngoài phạm vi** TLCN (API_SPEC không có); nếu kịp, thêm số mail `FAILED` vào khối "việc tồn" của dashboard C10 | ☐ |
| D13 | **Ngoài phạm vi** | UC-NOT-03 b4 | AWS SES / SendGrid (kế hoạch tương lai — giữ cổng `EmailSender` để đổi sau), email marketing, hủy đăng ký nhận tin | ☐ |

---

## 3. Luồng xử lý

```
Service nghiệp vụ (trong transaction) ──► TransactionalMailer.queue(cmd)   [MANDATORY]
     └─ INSERT notifications(channel=EMAIL, delivery_status=PENDING, template_key, recipient_email, metadata_json, next_attempt_at=now)
     └─ publish MailQueuedEvent
AFTER_COMMIT ─► MailDispatcher ─► executor "mail-" (4 luồng) ─► EmailSender.send (Thymeleaf, multipart HTML + text)
     ├─ OK  → SENT, sent_at
     └─ lỗi → markFailed (retry_count+1, next_attempt_at = backoff D5, last_error)
EmailRetryJob (5 phút, SKIP LOCKED) ─► gửi lại PENDING quá hạn / FAILED còn lượt
OTP / bảo mật: gửi ngay như cũ ─► TransactionalMailer.logSent / logFailed (nhật ký)
```

---

## 4. Cây file sẽ tạo / sửa

```
server-spring/src/main/resources/db/migration/V34__c11_email_outbox.sql               (mới — số tạm)
server-spring/src/main/resources/templates/email/
    layout/base.html (mới — bố cục chung, responsive) · fragments/order-items.html (bảng hàng)
    order-confirmed.html (sửa — dùng layout) · payment-failed.html · order-cancelled.html · new-order-vendor.html
    refund-request-new.html · refund-request-update.html · refund.html (sửa) · dispute-result.html
    withdrawal-approved.html · withdrawal-rejected.html · withdrawal-completed.html   (mới)
server-spring/src/main/resources/i18n/mail_vi.properties · mail_en.properties (chuỗi của mẫu, nếu dự án đang dùng messages)
server-spring/src/main/java/com/genie/
├── common/mail/MailTemplate.java          (sửa) + 11 mẫu mới
├── common/mail/SmtpEmailSender.java       (sửa) bỏ @Retryable trong luồng (D4)
├── notification/email/
│   ├── application/  TransactionalMailer · MailCommand · MailQueuedEvent · MailDispatcher · EmailRetryJob · MailBackoff
│   │                 MailExecutorConfig (4 luồng)
│   └── infra/        DevMailPreviewController (@Profile local)
├── notification/inbox/application/InboxReadModel.java (sửa — channel = IN_APP, D3)
├── order/order/application/OrderMailListener.java        (mới — J06)
├── payment/vnpay/application/PaymentMailListener.java    (mới — J05)
├── payment/wallet/application/WithdrawalMailListener.java (mới — J08)
├── support/refund/application/RefundMailListener.java    (mới — J07)
└── (7 nơi gọi cũ — D6)
server-spring/src/test/java/com/genie/notification/email/…
    MailBackoffTest · TransactionalMailerIT (rollback không có mail) · MailDispatcherIT (fake EmailSender lỗi 2 lần) · EmailRetryJobIT
    InboxExcludesEmailIT · MailTemplateRenderTest (mỗi mẫu render được, không còn biến ${…} thô, có bản chữ thuần)
    OrderMailIT · PaymentMailIT · RefundMailIT · WithdrawalMailIT
docs/Document/… ENTITY (10.1 cột mới + luồng outbox, Phụ lục E) · API_SPEC (12.2 bảng email đủ cột "mẫu" + "khi nào") · Usecase.md · CHECKLIST · BAO_CAO · xlsx
docs/kiem-thu/kich-ban-2026-11-28-c11.md
```

---

## 5. Bảng ánh xạ sự kiện → mẫu → module gọi

| Sự kiện (API 12.2) | Mẫu (`MailTemplate`) | Người nhận | Module gọi | Chuỗi |
|---|---|---|---|---|
| Đặt hàng COD / thanh toán online thành công | `ORDER_PLACED` (`order-confirmed.html`) | Khách | `order` (COD) / `payment` (online) | C2 / C3 |
| Thanh toán thất bại | `PAYMENT_FAILED` | Khách | `payment` | C3 |
| Có đơn mới cần xác nhận | `NEW_ORDER_FOR_VENDOR` | Shop | `order` (COD) / `payment` (online) | C3 |
| Shop từ chối / khách hủy / tự hủy quá hạn | `ORDER_CANCELLED` | Khách (+ shop khi khách hủy) | `order` | C2, C4, C5 |
| Yêu cầu trả hàng mới | `REFUND_REQUEST_NEW` | Shop | `support` | C8 |
| Shop đồng ý / từ chối / yêu cầu bổ sung | `REFUND_REQUEST_UPDATE` | Khách | `support` | C8 |
| Kết quả phân xử | `DISPUTE_RESULT` | Khách, shop | `support` | C8 |
| Đã hoàn tiền | `REFUND_COMPLETED` (`refund.html`) | Khách | `support` | C8 |
| Lệnh rút được duyệt / từ chối / hoàn tất | `WITHDRAWAL_APPROVED / REJECTED / COMPLETED` | Shop | `payment` | C9 |
| OTP, thiết bị lạ, đổi mật khẩu / email, xóa TK, mời nhân viên, KYC, duyệt SP, TK nhận tiền | (đã có) | — | đã có — D6 | AUTH, B1, B4, C1 |

---

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — `ModuleBoundaryTest` (**`notification` không import `order` / `payment` / `support`**), `MailTemplateRenderTest`.
2. Backend `local` + Mailpit (`SMTP_HOST=localhost SMTP_PORT=1025`): mail mới hiện ở `http://localhost:8025`, mở được trên khổ 360 px (Mailpit có chế độ xem điện thoại).
3. Hộp thư `GET /me/notifications` **không** có dòng `EMAIL`.
4. Tick progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Jxx.yy` = task; `Jxx.Cn` = task commit.

### J00 — Chuẩn bị

- **[J00.01]** Chạy `git config user.name` / `user.email` — ghi vào dòng *Phụ trách* của progress; liệt kê chuỗi nào đã merge (C3, C4, C5, C8, C9) để biết làm được J05–J08 tới đâu.
- **[J00.02]** Chốt D1–D13 vào progress.
- **[J00.03]** Tạo nhánh `feature/c11-email-outbox` từ `develop`.
- **[J00.04]** Đọc `SmtpEmailSender`, `MailTemplate`, `MailBranding`, 15 tệp `templates/email/*.html` (đã có layout chung chưa), 7 nơi gọi `EmailSender`; CHECK `notifications.channel`, `delivery_status`; mọi truy vấn của `InboxReadModel` (D3).

### J01 — Tài liệu trước

- **[J01.01]** ENTITY 10.1 (cột D2, luồng outbox, backoff D5, chống trùng D10), Phụ lục E; API_SPEC 12.2 (bảng email thêm cột *mẫu* + *thời điểm gửi*, chu kỳ job); Usecase.md UC-NOT-03 (outbox, OTP gửi ngay — D6).
- **[J01.C1]** COMMIT — `docs(notification): specify the transactional email outbox and retry policy`

### J02 — Migration & hộp thư

- **[J02.01]** `V34__c11_email_outbox.sql` (D2, UNIQUE D10).
- **[J02.02]** `Notification` entity (cột mới, `markFailed(error, nextAt)`, `markSent(at)`).
- **[J02.03]** `InboxReadModel` lọc `channel = IN_APP` (D3) + `InboxExcludesEmailIT`.
- **[J02.C1]** COMMIT — `feat(db): add email outbox scheduling columns to notifications`
- **[J02.C2]** COMMIT — `fix(notification): keep email rows out of the in-app inbox`

### J03 — Outbox, gửi bất đồng bộ & thử lại

- **[J03.01]** `MailExecutorConfig` (4 luồng, hàng đợi 500, từ chối → để job nhặt), `TransactionalMailer.queue / logSent / logFailed` (D4, D6, D9, D10).
- **[J03.02]** `MailDispatcher` (`AFTER_COMMIT`) + `MailBackoff` (D5) + `SmtpEmailSender` bỏ thử lại trong luồng.
- **[J03.03]** `EmailRetryJob` (D5, `SKIP LOCKED`).
- **[J03.04]** Test — `MailBackoffTest`, `TransactionalMailerIT` (gọi ngoài transaction → lỗi; rollback → không dòng nào, không gửi), `MailDispatcherIT` (fake `EmailSender` lỗi 2 lần → `retry_count` 2, lần 3 `SENT`; lỗi mãi → dừng sau 3 lần, `FAILED`, `last_error` có nội dung), `EmailRetryJobIT` (dòng `PENDING` 5 phút chưa gửi — giả lập tắt máy → được gửi), chống trùng D10.
- **[J03.C1]** COMMIT — `feat(notification): queue transactional emails in an outbox and retry failures`

### J04 — Mẫu HTML

- **[J04.01]** `layout/base.html` (N4) + `fragments/order-items.html`; chuyển 15 mẫu cũ sang layout (giữ nội dung).
- **[J04.02]** 11 mẫu mới (D7) + `MailTemplate` + chuỗi vi / en (D9).
- **[J04.03]** `DevMailPreviewController` (D11).
- **[J04.04]** Test — `MailTemplateRenderTest` (mỗi mẫu × vi / en render được, không còn `${`, có tiêu đề, bản chữ thuần không rỗng, số TK được che).
- **[J04.C1]** COMMIT — `feat(notification): add a responsive email layout and transactional templates`

### J05 — Email thanh toán & đơn mới (C3) — nhánh `feature/c11-transaction-emails`

- **[J05.01]** `PaymentMailListener` (payment) — thanh toán online thành công → `ORDER_PLACED` khách + `NEW_ORDER_FOR_VENDOR` từng shop; thất bại → `PAYMENT_FAILED`.
- **[J05.02]** `order` — đặt COD → `ORDER_PLACED` + `NEW_ORDER_FOR_VENDOR` (cạnh `CheckoutNotificationListener` hiện có).
- **[J05.03]** Test — `PaymentMailIT` (IPN thành công → 1 + N dòng `EMAIL`; IPN gửi lại → không thêm — D10).
- **[J05.C1]** COMMIT — `feat(payment): email buyers and sellers when orders are paid or payments fail`

### J06 — Email hủy / từ chối đơn (C4, C5, C2)

- **[J06.01]** `OrderMailListener` (order) — shop từ chối (C4), khách hủy (C5 — báo cả shop), tự hủy quá hạn (C2 `ExpiredSessionSweeper`) → `ORDER_CANCELLED` kèm lý do + phần đã hoàn (Điểm / voucher nếu C6 đã có).
- **[J06.02]** Test — `OrderMailIT` (3 nguồn hủy, mỗi nguồn đúng người nhận + nội dung lý do).
- **[J06.C1]** COMMIT — `feat(order): email buyers when orders are rejected, cancelled or expire`

### J07 — Email trả hàng & hoàn tiền (C8)

- **[J07.01]** `RefundMailListener` (support) — `REFUND_REQUEST_NEW` (shop), `REFUND_REQUEST_UPDATE` (khách), `DISPUTE_RESULT` (hai bên), `REFUND_COMPLETED` (khách).
- **[J07.02]** Test — `RefundMailIT`.
- **[J07.C1]** COMMIT — `feat(support): email return request updates, dispute results and completed refunds`

### J08 — Email rút tiền (C9)

- **[J08.01]** `WithdrawalMailListener` (payment) — duyệt / từ chối / hoàn tất (số TK che — N5).
- **[J08.02]** Test — `WithdrawalMailIT`.
- **[J08.C1]** COMMIT — `feat(payment): email sellers about withdrawal approval, rejection and completion`

### J09 — Chuyển email hiện có sang outbox (D6)

- **[J09.01]** 7 nơi gọi `EmailSender`: OTP + bảo mật → gửi ngay + `logSent / logFailed`; KYC, duyệt sản phẩm, xóa TK, mời nhân viên, TK nhận tiền, mở khóa → `TransactionalMailer.queue`.
- **[J09.02]** Test hồi quy các IT cũ (`RegisterFlowIT`, KYC, duyệt sản phẩm, mời nhân viên…) xanh + có dòng nhật ký.
- **[J09.C1]** COMMIT — `refactor(notification): route existing emails through the outbox log`

### J10 — Kiểm thử & chạy thật

- **[J10.01]** `docs/kiem-thu/kich-ban-2026-11-28-c11.md` — TC-C11-01…: mỗi sự kiện ở mục 5 tạo đúng một mail ở Mailpit (dùng luồng thật của C3–C9 đã có), nội dung đúng số tiền / mã; tắt Mailpit giữa chừng → mail `FAILED` rồi tự gửi lại khi bật lại (≤ 3 lần); rollback nghiệp vụ không có mail; xem trên điện thoại (Mailpit mobile) + Gmail thật một lần (`.env` Gmail — không commit); hộp thư web không có dòng email.
- **[J10.02]** Chạy thật — backend `local` + Mailpit + web build (cờ của các chuỗi đã có) — Chromium (Playwright) thao tác các luồng, kiểm Mailpit qua API `GET http://localhost:8025/api/v1/messages`; mỗi lỗi một commit `fix(…)`.
- **[J10.C1]** COMMIT — `docs(notification): add transactional email real-run scenarios`

### J11 — Hoàn thiện use case & tick tiến độ

- **[J11.01]** Usecase.md — *Hiện trạng* UC-NOT-03 (ghi rõ email nào đã nối — theo các chuỗi đã merge).
- **[J11.02]** `CHECKLIST_TIEN_DO_UC.md` — tick BE / Test / Chạy (UC chạy nền: GD / Nối `—`), Trạng thái (✅ khi mọi email mục 5 đã nối; thiếu chuỗi nào → `◐` + liệt kê), **cột Phụ trách = tên ở J00.01**, số mục 1 + phân hệ 10, dòng mục 5 *Nhật ký*.
- **[J11.03]** `BAO_CAO_UC_DOC_LAP.md` — mục C11 + dòng *Phụ trách*.
- **[J11.04]** `Quản Lý UseCase.xlsx` — UC-NOT-03, cột AA.
- **[J11.05]** README ngày (mục 7) — tick dòng C11.
- **[J11.C1]** COMMIT — `docs(notification): record transactional emails as implemented`
- **[J11.06]** Mở 2 PR xếp chồng vào `develop` (`c11-email-outbox` → `c11-transaction-emails`).

---

## 8. Ngoài phạm vi

- AWS SES / SendGrid, email marketing, hủy đăng ký, màn Admin tra cứu email (D12).

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Dòng `EMAIL` lọt vào hộp thư người dùng | D3 + `InboxExcludesEmailIT` làm trước mọi việc khác (J02) |
| Mail gửi khi transaction rollback | Outbox `MANDATORY` + gửi `AFTER_COMMIT` (N1); IT |
| Mất mail khi tắt máy / SMTP lỗi | Dòng `PENDING` bền + `EmailRetryJob` (D5) |
| Gửi trùng khi webhook / job chạy lại | UNIQUE theo đối tượng (D10) |
| Vòng phụ thuộc `notification ↔ module nguồn` | Listener ở module nguồn (N7); `ModuleBoundaryTest` |
| Chuyển email cũ làm hỏng OTP | OTP giữ đường gửi ngay (N6), chỉ thêm nhật ký; IT hồi quy |
