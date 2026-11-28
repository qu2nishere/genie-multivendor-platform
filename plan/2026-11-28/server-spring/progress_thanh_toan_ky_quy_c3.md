# PROGRESS — Chuỗi C3 Thanh toán & ký quỹ (UC-PAY-01 · UC-PAY-03 · UC-PAY-05 · UC-PAY-06 · UC-PAY-07)

| | |
|---|---|
| **Plan** | [plan_thanh_toan_ky_quy_c3.md](./plan_thanh_toan_ky_quy_c3.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-28 |
| **Use case** | Khởi tạo thanh toán trực tuyến — VNPay / chuyển khoản VietQR qua SePay (UC-PAY-01) · Xử lý IPN VNPay & webhook SePay idempotent (UC-PAY-03) · Ghi nhận ký quỹ (Escrow) & Khấu trừ hoa hồng (UC-PAY-05) · Ghi sổ cái tài chính kép bất biến (UC-PAY-06) · Tự động giải phóng tiền ký quỹ về ví người bán (UC-PAY-07) |
| **Tổng** | 57 task + 11 commit, 11 giai đoạn |
| **Nhánh** | `feature/c3-ledger-escrow` (E00–E04) → `feature/c3-vnpay` (E05) → `feature/c3-sepay-vietqr` (E06) → `feature/c3-escrow-release` (E07–E10) |
| **Phụ trách** | _(điền kết quả `git config user.name` ở task E00.01 — tên này dùng khi tick cột Phụ trách ở checklist / báo cáo / Excel)_ |
| **Người commit** | danh tính git đã cấu hình trên máy người làm — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Cập nhật gần nhất** | 2026-11-28 — lập plan, chưa bắt đầu (0 / 68) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục *Ghi chú*.
- Task `COMMIT` chỉ tick sau khi qua *Cổng kiểm tra* (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`. Thông điệp đã ghi sẵn — tiếng Anh, chữ thường, đúng `commitlint.config.cjs`; có mã Jira thì thêm ` [#GNE-<số>]` cuối.
- Sau mỗi buổi làm: cập nhật cột *Xong* của bảng Tổng quan, dòng *Cập nhật gần nhất*, một dòng *Nhật ký* cuối trang.
- Cột *Chạy* của use case chỉ tick khi đã chạy thật trên giao diện (Chromium điều khiển bằng Playwright, hệ thống thật) và đạt hết ca của kịch bản — ghi cách chạy + số ca vào *Ghi chú*.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | **Giao dịch gắn phiên hay đơn** — bảng có `order_id`, nhưng phiên nhiều shop thanh toán một lần | ☐ | |
| D2 | **Mã tham chiếu** `vnp_TxnRef` / nội dung chuyển khoản | ☐ | |
| D3 | **Chế độ cổng** | ☐ | |
| D4 | **Cổng order nhận kết quả thanh toán** | ☐ | |
| D5 | **Xử lý IPN** (thứ tự kiểm) | ☐ | |
| D6 | **Thanh toán lại / đổi phương thức** (API 6.12) | ☐ | |
| D7 | **Thanh toán thất bại có hoàn kho ngay?** | ☐ | |
| D8 | **Sổ giao dịch ngân hàng SePay** — tiền thiếu / thừa / không mã "ghi chờ Staff" nhưng không có bảng | ☐ | |
| D9 | **Khớp nội dung chuyển khoản** | ☐ | |
| D10 | **Tiền về sau khi phiên đã hết hạn** | ☐ | |
| D11 | **Ký quỹ đơn COD** | ☐ | |
| D12 | **Công thức ký quỹ** | ☐ | |
| D13 | **Danh mục tài khoản kế toán** (`financial_ledger.account` là chuỗi) | ☐ | |
| D14 | **Bất biến sổ cái** | ☐ | |
| D15 | **Giải ngân** (UC-PAY-07) | ☐ | |
| D16 | **Ví chưa tồn tại** | ☐ | |
| D17 | **Thông báo** | ☐ | |
| D18 | **Đối soát hằng đêm** | ☐ | |
| D19 | **Phí cổng** | ☐ | |
| D20 | **Cờ web** | ☐ | |
| D21 | **Ngoài phạm vi** | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| E00 | Chuẩn bị | 7 | 0 | 0 / 7 |
| E01 | Tài liệu trước | 4 | 1 | 0 / 5 |
| E02 | Migration & miền | 4 | 1 | 0 / 5 |
| E03 | Sổ cái kép (UC-PAY-06) | 4 | 1 | 0 / 5 |
| E04 | Ký quỹ & ví (UC-PAY-05, nền UC-PAY-07) | 5 | 1 | 0 / 6 |
| E05 | VNPay (UC-PAY-01 nhánh VNPay, UC-PAY-03) — nhánh `feature/c3-vnpay` | 8 | 2 | 0 / 10 |
| E06 | VietQR qua SePay (UC-PAY-01 nhánh VietQR, UC-PAY-03 nhánh SePay) — nhánh `feature/c3-sepay-vietqr` | 5 | 1 | 0 / 6 |
| E07 | Giải ngân ký quỹ (UC-PAY-07) — nhánh `feature/c3-escrow-release` | 3 | 1 | 0 / 4 |
| E08 | Nối web (cờ `payment`) | 5 | 1 | 0 / 6 |
| E09 | Dữ liệu demo, kiểm thử & chạy thật | 4 | 1 | 0 / 5 |
| E10 | Hoàn thiện use case & tick tiến độ | 8 | 1 | 0 / 9 |
| **Tổng** | | **57** | **11** | **0 / 68** |

## E00 — Chuẩn bị

- [ ] **E00.01** Chạy `git config user.name` / `user.email`
- [ ] **E00.02** Chốt D1–D21 vào progress (cột *Chốt* + ngày).
- [ ] **E00.03** Tạo nhánh `feature/c3-ledger-escrow` từ `develop`.
- [ ] **E00.04** Đọc CHECK constraint thật của `payment_transactions.status`, `escrow_ledgers.status`, `wallet_transactions.type`, `admin_notifications.type` (có `ESCROW_DISCREPANCY`, chuyển khoản lệch chưa?) trong `V6`, `V10`; đọc `CheckoutPricer` để biết `shop_shipping_subsidy` / freeship shop được lưu ở cột nào (D12); đọc `R__seed_demo_30_shops.sql` (ví đã có?
- [ ] **E00.05** Kiểm `CheckoutCreatedEvent` được publish **trong** transaction của `CheckoutService.place` (D11); kiểm `SecurityConfig` đường dẫn công khai hiện có cho `/payments/**`.
- [ ] **E00.06** Đăng ký tài khoản sandbox VNPay (`sandbox.vnpayment.vn`) + tài khoản SePay thử (nếu có)
- [ ] **E00.07** Đọc màn mock: trang Thanh toán (chọn phương thức), trang kết quả, màn VietQR (đếm ngược, QR, nội dung `TR…`), "Thanh toán lại" ở Đơn mua; `grep` mọi chỗ gọi `settleMockPayment`, `retryPayment`, `getCheckoutSession`.

## E01 — Tài liệu trước

- [ ] **E01.01** ENTITY 6.1 (`checkout_session_id`, `payment_code`, `expires_at`, D1–D2), 6.7 (`LedgerAccount`, bảng bút toán mẫu mục 3.3, trigger chỉ ghi thêm D14), **6.9 mới `BankTransaction`** (D8), Phụ lục E; `GIAI_NGHIA_ENTITY.md`; `java-code/Diagram06_*.java`.
- [ ] **E01.02** API_SPEC
- [ ] **E01.03** Usecase.md
- [ ] **E01.04** `docs/cau-hinh-moi-truong.md` + `.env.example` + `deploy/.env.example`
- [ ] **E01.C1** 🔖 COMMIT `docs(payment): specify payment sessions, sepay bank log and ledger accounts` · hash: `…`

## E02 — Migration & miền

- [ ] **E02.01** `V28__c3_payment_escrow.sql`
- [ ] **E02.02** Entity
- [ ] **E02.03** `ErrorCode`
- [ ] **E02.04** Test
- [ ] **E02.C1** 🔖 COMMIT `feat(db): add payment session link, sepay bank log and append-only ledger` · hash: `…`

## E03 — Sổ cái kép (UC-PAY-06)

- [ ] **E03.01** `LedgerGroup` (builder: `debit(account, amount)`, `credit(...)`, `reference`, `orderId`, `vendorId`, `event`)
- [ ] **E03.02** `LedgerPoster.post(group)`
- [ ] **E03.03** `LedgerReconciliationJob` (D18)
- [ ] **E03.04** Test
- [ ] **E03.C1** 🔖 COMMIT `feat(payment): add append-only double-entry ledger poster and nightly reconciliation` · hash: `…`

## E04 — Ký quỹ & ví (UC-PAY-05, nền UC-PAY-07)

- [ ] **E04.01** `EscrowAmounts.of(order, items)` (D12)
- [ ] **E04.02** `EscrowFacade`
- [ ] **E04.03** `EscrowOpenListener` (D11)
- [ ] **E04.04** `WalletHasher` (HMAC khóa `genie.wallet.hash-secret`
- [ ] **E04.05** Test
- [ ] **E04.C1** 🔖 COMMIT `feat(payment): open escrow for cod orders and add wallet poster` · hash: `…`

## E05 — VNPay (UC-PAY-01 nhánh VNPay, UC-PAY-03) — nhánh `feature/c3-vnpay`

- [ ] **E05.01** `VnpaySignature` (sắp tham số ASCII, URL-encode kiểu VNPay `+` cho khoảng trắng, HMAC-SHA512) + `VnpayUrlBuilder` (tham số API 2.1.0: `vnp_Version=2.1.0`, `vnp_Command=pay`, `vnp_Locale=vn`, `vnp_OrderType=other`, `vnp_IpAddr`, `vnp_CreateDate` / `vnp_ExpireDate` giờ VN `yyyyMMddHHmmss`).
- [ ] **E05.02** `OrderPaymentFacade` trong order (D4) + `OrderPaidEvent` phát sau khi đổi; test đơn vị cho máy trạng thái (`markPaid` chỉ từ `PENDING_PAYMENT`).
- [ ] **E05.03** `VnpayPaymentInitiator implements PaymentInitiator` (`@ConditionalOnProperty` theo D3
- [ ] **E05.04** `VnpayIpnService` (D5) + `VnpayController` (`/payments/vnpay/ipn`, `/payments/vnpay/return`
- [ ] **E05.05** `CheckoutPaymentService` (order)
- [ ] **E05.06** Thông báo `AFTER_COMMIT` (D17)
- [ ] **E05.07** `DevPaymentSimulatorController` `@Profile("local")` (D3)
- [ ] **E05.08** Test
- [ ] **E05.C1** 🔖 COMMIT `feat(order): add order payment facade and retry payment endpoint` · hash: `…`
- [ ] **E05.C2** 🔖 COMMIT `feat(payment): initiate vnpay payments and confirm them through signed ipn` · hash: `…`

## E06 — VietQR qua SePay (UC-PAY-01 nhánh VietQR, UC-PAY-03 nhánh SePay) — nhánh `feature/c3-sepay-vietqr`

- [ ] **E06.01** `VietQrBuilder`
- [ ] **E06.02** `SepayWebhookController` + `SepayWebhookService`
- [ ] **E06.03** `BankTransferRouter` + `ContentCodeParser` (D9) + interface `BankTransferMatcher { boolean supports(code); void match(bankTx) }`.
- [ ] **E06.04** `PaymentTransferMatcher`
- [ ] **E06.05** Test
- [ ] **E06.C1** 🔖 COMMIT `feat(payment): accept vietqr transfers through the sepay webhook with a bank transaction log` · hash: `…`

## E07 — Giải ngân ký quỹ (UC-PAY-07) — nhánh `feature/c3-escrow-release`

- [ ] **E07.01** `EscrowFacade.releaseNow` + `EscrowReleaseJob` (D15)
- [ ] **E07.02** Bỏ qua ký quỹ `FROZEN` / `DISPUTED` / `PENDING_FUNDING` (ràng buộc UC-ORD-12)
- [ ] **E07.03** Test
- [ ] **E07.C1** 🔖 COMMIT `feat(payment): release completed escrows to seller wallets every 15 minutes` · hash: `…`

## E08 — Nối web (cờ `payment`)

- [ ] **E08.01** `checkout.api.ts`
- [ ] **E08.02** Màn VietQR
- [ ] **E08.03** VNPay
- [ ] **E08.04** Trang giả lập cổng dev `/checkout/mock-gateway` (chỉ build có `payment` + biến `PUBLIC_DEV_TOOLS=true`)
- [ ] **E08.05** `error-codes.ts` + vi / en (5 mã PAY + khóa mới); test mapper; `npm run verify`.
- [ ] **E08.C1** 🔖 COMMIT `feat(web): connect vnpay and vietqr payment screens to the real api` · hash: `…`

## E09 — Dữ liệu demo, kiểm thử & chạy thật

- [ ] **E09.01** Seed
- [ ] **E09.02** `docs/api-tests/payments.http`
- [ ] **E09.03** `docs/kiem-thu/kich-ban-2026-11-28-c3.md`
- [ ] **E09.04** Chạy thật
- [ ] **E09.C1** 🔖 COMMIT `docs(payment): add payment api tests and real-run scenarios` · hash: `…`

## E10 — Hoàn thiện use case & tick tiến độ

- [ ] **E10.01** OpenAPI
- [ ] **E10.02** `docs/web-backend-handoff.md`
- [ ] **E10.03** Usecase.md
- [ ] **E10.04** `CHECKLIST_TIEN_DO_UC.md`
- [ ] **E10.05** `BAO_CAO_UC_DOC_LAP.md`
- [ ] **E10.06** `Quản Lý UseCase.xlsx`
- [ ] **E10.07** README ngày (mục 7)
- [ ] **E10.C1** 🔖 COMMIT `docs(payment): record payment, escrow, ledger and payout release as implemented` · hash: `…`
- [ ] **E10.08** Mở 4 PR xếp chồng vào `develop` (`c3-ledger-escrow` → `c3-vnpay` → `c3-sepay-vietqr` → `c3-escrow-release`); mỗi PR ghi nhánh nền ở dòng đầu; PR đầu nhờ người làm C2 duyệt `OrderPaymentFacade` + `CheckoutSessionController`.

## Ghi chú

| Task | Ghi chú |
|---|---|
| | |

## Nhật ký

| Ngày | Việc đã làm | Commit / PR |
|---|---|---|
| 2026-11-28 | Lập plan + progress (57 task, 11 commit, 21 quyết định chờ chốt) | nhánh `feature/plan-c3-c11` |
