# PLAN — Chuỗi C3 Thanh toán & ký quỹ (UC-PAY-01 · UC-PAY-03 · UC-PAY-05 · UC-PAY-06 · UC-PAY-07)

| | |
|---|---|
| **Ngày lập** | 2026-11-28 |
| **Thể loại** | server-spring (module `payment`: feature `vnpay`, `sepay` (mới), `escrow`, `ledger`, `wallet`; cổng mới trong `order`) + nối `web/` (feature `checkout`, `payment`) |
| **Use case** | Khởi tạo thanh toán trực tuyến — VNPay / chuyển khoản VietQR qua SePay (UC-PAY-01) · Xử lý IPN VNPay & webhook SePay idempotent (UC-PAY-03) · Ghi nhận ký quỹ (Escrow) & Khấu trừ hoa hồng (UC-PAY-05) · Ghi sổ cái tài chính kép bất biến (UC-PAY-06) · Tự động giải phóng tiền ký quỹ về ví người bán (UC-PAY-07) |
| **Tài liệu chuẩn** | UC phân hệ 04 (UC-PAY-01 dòng 1279, 03 dòng 1309, 05 dòng 1346, 06 dòng 1376, 07 dòng 1400) · API 6.10–6.12, 12.1 (VNPay return / IPN, SePay webhook), 12.2 (tác vụ nền), 15 (`GNE-PAY-*`) · ENTITY 5.1 `CheckoutSession`, 5.2 `Order`, 5.3 `OrderItem`, 6.1 `PaymentTransaction`, 6.3 `EscrowLedger`, 6.4 `VendorWallet`, 6.5 `WalletTransaction`, 6.7 `FinancialLedger` · LLD 6 (máy trạng thái), 9 (thuật toán), 10 (cron) · TS 10 (đồng thời), 14 (5 kịch bản tiền) — xem [README](../README.md) |
| **Theo dõi tiến độ** | [progress_thanh_toan_ky_quy_c3.md](progress_thanh_toan_ky_quy_c3.md) — cùng mã task `Exx.yy` |
| **Phụ trách** | Người làm plan — lấy theo **`git config user.name`** trên máy thực hiện (ghi vào progress ở E00.01); không ghi tên cố định |
| **Người commit** | danh tính git đã cấu hình trên máy đó — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Phụ thuộc** | C2 trên `develop` (`CheckoutService`, `PaymentInitiator`, `ExpiredSessionSweeper`, `VariantFacade.reserve/release/commitSale`). Chuỗi **C4, C5, C8, C9 phụ thuộc plan này** (sổ cái, ký quỹ, ví, sổ ngân hàng SePay) |
| **Mục tiêu** | 5 use case ⬜ → ✅. Đơn VNPay / VietQR thanh toán thật (sandbox), đơn `PAID`, ký quỹ `HELD`, đơn COD có ký quỹ `PENDING_FUNDING`; mọi dòng tiền có bút toán kép; đơn `COMPLETED` được giải ngân về ví shop |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `1b66eae`, 2026-11-28)

| Phần | Đã có | Còn thiếu |
|---|---|---|
| **Bảng / entity** (`V6__payment.sql`) | `payment_transactions` (`order_id`, `idempotency_key`, `amount`, `refunded_amount`, `gateway_transaction_id`, `payment_method`, `status`, `webhook_payload`, `@Version`); `escrow_ledgers` (gross / commission / shipping / net / refunded, `funding_source`, `gateway_funded_amount`, `cod_funded_amount`, `status`, `funded_at`, `release_eligible_at`, `released_at`); `vendor_wallets` (`balance`, `pending_withdrawal`, `balance_hash`, `@Version`); `wallet_transactions`; `financial_ledger` (`seq`, `event_type`, `reference_id`, `order_id`, `vendor_id`, `account`, `debit`, `credit`, `balance_after`). Entity có phương thức: `PaymentTransaction.markSuccess/markFailed/recordRefund/refundableAmount`, `EscrowLedger.fund/freeze/dispute/release/scheduleRelease/isReleasable/applyPartialRefund/markRefunded/releaseDisputeHold`, `VendorWallet.credit/debit/holdForWithdrawal/settleWithdrawal/verifyHash`, `FinancialLedger.entry` | ⚠ `payment_transactions` gắn **`order_id`** nhưng một phiên checkout nhiều shop chỉ thanh toán **một lần** → cần `checkout_session_id` (D1). Không có mã chuyển khoản `TR{id}`, hạn, UNIQUE mã giao dịch cổng. **Không có** chỗ ghi webhook SePay chưa khớp (D8). `FinancialEventType` đủ 10 giá trị; **chưa có danh mục tài khoản** kế toán (D13) |
| **Service / API** | Mọi thư mục `payment/*` chỉ có `domain/` (entity + repository rỗng) | Toàn bộ service, controller, job |
| **Cổng của C2** | `PaymentInitiator` (bản mặc định `CheckoutPorts.stubPaymentInitiator` trả khối `payment` rỗng), `CheckoutCreatedEvent` (đơn COD báo ngay — `CheckoutNotificationListener`), `ExpiredSessionSweeper` (60 giây, nhả giữ chỗ, đơn `AUTO_CANCELLED`), `OrderCodeGenerator`, `CheckoutPricer` chụp hoa hồng vào `order_items.commission_rate / commission_amount / vendor_receivable` | Cài bản thật `PaymentInitiator`; cổng order nhận "đã thanh toán" (D4); listener tạo ký quỹ COD (D11) |
| **Cấu hình** | `.env.example`: `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_URL`, `VNPAY_RETURN_URL`, `SEPAY_API_KEY`, `SEPAY_ACCOUNT_NUMBER` (chú thích, chưa dùng); khóa băm ví máy dev `local-wallet-secret` (seed) | `PAYMENT_MODE`, `SEPAY_BANK_CODE`, `SEPAY_ACCOUNT_NAME`, `VNPAY_IPN_URL` (ghi chú — đăng ký ở cổng VNPay); bảng `docs/cau-hinh-moi-truong.md` |
| **Mã lỗi** | — | `GNE-PAY-4021`, `GNE-PAY-4092`, `GNE-PAY-4093` (API 15), thêm `GNE-PAY-4094` (đổi phương thức khi phiên không còn chờ — D6), `GNE-PAY-4041` (phiên không thuộc người gọi) |
| **Web** | `checkout/checkout.api.ts`: `createCheckout`, `getCheckoutSession`, `retryPayment`, **`settleMockPayment`** (giả lập trả tiền trên mock); trang kết quả / màn VietQR / thanh toán lại đã dựng (cờ `checkout` đang thật cho đặt hàng) | Nối 6.11 / 6.12 thật, bỏ `settleMockPayment` khi cờ `payment` bật, chuyển hướng VNPay thật |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Package `com.genie.payment.{vnpay,sepay,escrow,ledger,wallet}.{api,application,domain,infra}`; luật tiền trong entity; service mở `@Transactional` | TS 4 |
| N2 | **`order` không import `payment`.** `payment → order` (cài `PaymentInitiator`, gọi `OrderPaymentFacade`); order cần gì của payment thì khai cổng trong order | README mục 3 |
| N3 | Tiền / tồn / trạng thái đơn đổi **trong cùng transaction** với xác nhận thanh toán; thông báo / email `AFTER_COMMIT` | TS 4, 10 |
| N4 | **Return URL chỉ hiển thị** — không đổi trạng thái; chỉ IPN / webhook (đã kiểm chữ ký / Apikey) mới đổi | API 12.1; TS 9 |
| N5 | Webhook **idempotent**: VNPay theo `payment_code` + trạng thái giao dịch (khóa dòng `FOR UPDATE`); SePay theo `id` SePay (UNIQUE `bank_transactions.sepay_id`). Luôn trả mã cổng mong đợi để cổng không gửi lại vô hạn | UC-PAY-03 |
| N6 | **Sổ cái chỉ ghi thêm**: không có `UPDATE` / `DELETE` trên `financial_ledger` (trigger chặn ở CSDL + không có phương thức sửa); sai → bút toán đảo cùng `reference_id`; mỗi nhóm bút toán Nợ = Có trước khi commit | UC-PAY-06; TS 7 |
| N7 | Ví shop: `SELECT … FOR UPDATE` + `@Version` + `balance_hash` HMAC; mọi thay đổi số dư qua `WalletPoster` và luôn có một dòng `wallet_transactions` (`balance_before / after`) | TS 10; ENTITY 6.4 |
| N8 | Cron `FOR UPDATE SKIP LOCKED`, lô nhỏ, mỗi bản ghi một transaction (lỗi một bản ghi không làm hỏng cả lô) | LLD 10 |
| N9 | Bí mật cổng (`VNPAY_HASH_SECRET`, `SEPAY_API_KEY`) chỉ ở `.env`; log **không** in chữ ký / khóa; `webhook_payload` lưu nguyên văn để đối chiếu (không có dữ liệu thẻ) | TS 9 |
| N10 | 2 kịch bản tiền bắt buộc của TS 14 thuộc plan này: **IPN + Return / IPN gửi lại cùng lúc chỉ ghi nhận một lần**; **hai lượt giải ngân song song chỉ cộng ví một lần** | TS 14 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Giao dịch gắn phiên hay đơn** — bảng có `order_id`, nhưng phiên nhiều shop thanh toán một lần | UC-PAY-01 b3 (`checkoutSessionId`); API 6.10 | Thêm `payment_transactions.checkout_session_id` (FK, NOT NULL cho bản ghi mới), giữ `order_id` NULL (dành cho DATN thanh toán từng đơn). Một phiên có thể có nhiều giao dịch (thanh toán lại / đổi phương thức — D6), nhiều nhất **một** `SUCCESS`. ENTITY 6.1 + Phụ lục E sửa trước | ☐ |
| D2 | **Mã tham chiếu** `vnp_TxnRef` / nội dung chuyển khoản | UC-PAY-01 (`vnp_TxnRef = checkoutSessionId`, VietQR `TR{paymentTransactionId}`) | Một mã cho cả hai cổng: `payment_code = "TR" + payment_transactions.id` (cột mới, UNIQUE), dùng làm `vnp_TxnRef`, `idempotency_key` và nội dung chuyển khoản VietQR. VNPay bắt buộc `TxnRef` không trùng trong ngày → mỗi lần thanh toán lại phải là giao dịch mới. Sửa câu UC-PAY-01 b2 / b3 | ☐ |
| D3 | **Chế độ cổng** | TS "bên thứ ba có stub" | `PAYMENT_MODE=stub` (mặc định `local` / test) \| `http`. Stub: VNPay trả `payment_url` tới trang giả lập của web `/checkout/mock-gateway?code=TR…` (chỉ khi `PUBLIC_REAL_API` có `payment` + backend profile `local`), trang này gọi `POST /api/v1/dev/payments/{code}/simulate` (**chỉ bean ở profile `local`**) dựng IPN có chữ ký đúng → chạy đúng code thật của IPN. VietQR stub: trả QR thật (ảnh `qr.sepay.vn`) + nút "Giả lập đã chuyển" gọi webhook SePay giả lập tương tự | ☐ |
| D4 | **Cổng order nhận kết quả thanh toán** | N2 | Order khai `OrderPaymentFacade` (cài ngay trong order — payment gọi): `markSessionPaid(sessionId, paymentCode, paidAt)` → mỗi đơn con `Order.markPaid()`, `StockReservation.confirm()` + `VariantFacade.commitSale(qty)`, `CheckoutSession.complete()`; `markSessionPaymentFailed(sessionId)` → phiên vẫn `PAYMENT_PENDING` (D7); `sessionForPayment(sessionId, userId)` → tổng tiền, trạng thái, hạn, phương thức. Phát `OrderPaidEvent` (đã có trong `order.event`) sau khi đổi | ☐ |
| D5 | **Xử lý IPN** (thứ tự kiểm) | UC-PAY-03 b2–8; API 12.1 | (1) kiểm chữ ký → sai `97`; (2) tìm giao dịch theo `vnp_TxnRef` **`FOR UPDATE`** → không có `01`; (3) `vnp_Amount ≠ amount × 100` → `04`; (4) giao dịch đã `SUCCESS` / `FAILED` → `02`; (5) `vnp_ResponseCode = 00` **và** `vnp_TransactionStatus = 00` → `markSuccess(vnp_TransactionNo)` + D4 + ký quỹ `HELD` (D12) + sổ cái `ESCROW_IN`; khác → `markFailed`; (6) trả `00`. Mọi nhánh lưu `webhook_payload`. Lỗi bất ngờ → rollback, trả `99` (VNPay gửi lại) | ☐ |
| D6 | **Thanh toán lại / đổi phương thức** (API 6.12) | UC-PAY-01 luồng thay thế | `POST /me/checkout-sessions/{id}/payments {method, return_url}` 🔁 (Idempotency-Key): phiên phải của người gọi (`404 GNE-PAY-4041`), `PAYMENT_PENDING` / `INITIATED`, chưa quá `expires_at` (`409 GNE-INV-4093`); giao dịch `PENDING` cũ → `FAILED` với `webhook_payload = {"reason":"superseded"}` (nếu CHECK `payment_transactions.status` có sẵn `CANCELLED` thì dùng `CANCELLED` — kiểm ở E00.04); tạo giao dịch mới; đổi sang **COD** → đơn con chuyển nhánh COD (ký quỹ `PENDING_FUNDING`, báo shop như đặt COD), phiên `COMPLETED`. Phiên đã `COMPLETED` → `409 GNE-PAY-4094` | ☐ |
| D7 | **Thanh toán thất bại có hoàn kho ngay?** | UC-PAY-03 b7 ("FAILED, hoàn kho") ↔ UC-PAY-01 ("thanh toán lại khi phiên còn giữ chỗ 15 phút") | **Không** nhả giữ chỗ khi một lần thanh toán thất bại — khách còn thanh toán lại được tới `expires_at`; `ExpiredSessionSweeper` (C2) nhả khi hết hạn. Sửa câu UC-PAY-03 b7 cho khớp | ☐ |
| D8 | **Sổ giao dịch ngân hàng SePay** — tiền thiếu / thừa / không mã "ghi chờ Staff" nhưng không có bảng | UC-PAY-03 luồng SePay; README mục 4 | Bảng mới **`bank_transactions`** (ENTITY 6.9 mới): `sepay_id` UNIQUE, `transfer_type` (`IN`/`OUT`), `amount`, `content`, `reference_code`, `account_number`, `transaction_date`, `matched_type` (`PAYMENT`, `COD_REMITTANCE`, `REFUND`, `WITHDRAWAL`, NULL), `matched_id`, `status` (`MATCHED`, `UNMATCHED`, `IGNORED`), `note`, `raw_json`, `created_at`. Webhook ghi một dòng **trước** khi khớp (chống trùng bằng UNIQUE), rồi gọi bộ khớp đã đăng ký (`BankTransferMatcher` — C3 cài `TR`, C4 `COD`, C8 `TRRF`, C9 `TRWD`) | ☐ |
| D9 | **Khớp nội dung chuyển khoản** | UC-PAY-03 | Chuẩn hóa `content` (bỏ khoảng trắng / dấu chấm, chữ hoa); thứ tự thử: `TRRF\d+` → `TRWD\d+` → `TR\d+` (tránh `TRRF12` bị hiểu là `TR`). Tiền vào `TR{id}`: khớp số tiền → thành công như IPN; thiếu / thừa → `UNMATCHED` + `AdminNotification` "Chuyển khoản lệch" (Staff xử lý tay — ngoài phạm vi màn hình, chỉ thông báo + log); không có mã → `UNMATCHED` | ☐ |
| D10 | **Tiền về sau khi phiên đã hết hạn** | UC-PAY-03 luồng SePay | Máy trạng thái đơn không cho `AUTO_CANCELLED` quay lại, nên: (a) tiền tới khi đơn **còn** `PENDING_PAYMENT` (sweeper chưa chạy tới) → thử giữ chỗ lại bằng `VariantFacade.reserve` cho mọi dòng trong **một** transaction; đủ hàng → xác nhận như thanh toán thành công, thiếu → như (b); (b) đơn đã `AUTO_CANCELLED` → `bank_transactions.status = UNMATCHED`, `note = "LATE_PAYMENT_REFUND"` + AdminNotification để Staff hoàn tay (hàng đợi hoàn của C8). Sửa câu UC-PAY-03 luồng SePay cho khớp | ☐ |
| D11 | **Ký quỹ đơn COD** | UC-PAY-05 tác nhân ("với COD kích hoạt khi tạo đơn") | `payment` lắng nghe `CheckoutCreatedEvent` **đồng bộ** (`@EventListener`, cùng transaction đặt hàng — kiểm ở E00.05 event được publish trong transaction): COD → mỗi đơn con một `escrow_ledgers` `PENDING_FUNDING`, `funding_source = COD_CASH`, `cod_funded_amount = grand_total`; online → chưa tạo (tạo khi IPN thành công) | ☐ |
| D12 | **Công thức ký quỹ** | UC-PAY-05 b2–4; ENTITY 6.3 | Đọc từ `order_items` đã chụp ở C2 (không tính lại tỷ lệ): `gross = Σ gross_amount` (= `line_total − shop_discount`), `commission_fee = Σ commission_amount`, `shop_shipping_subsidy` = phần ship shop chịu (freeship của shop — C2 đã tính vào `orders.shipping_discount_amount`? kiểm E00.04), `platform_shipping_fee` = phí ship khách trả (sàn thu hộ trả hãng), `net = gross − commission − shop_shipping_subsidy`. Voucher sàn / Điểm do sàn chịu **không** trừ vào `net` (UC-PRO-04 ràng buộc). `gateway_funded_amount = grand_total` (online) | ☐ |
| D13 | **Danh mục tài khoản kế toán** (`financial_ledger.account` là chuỗi) | UC-PAY-06 b2; ENTITY 6.7 (ví dụ `CASH`, `ESCROW_LIABILITY`) | Enum `LedgerAccount` (lưu tên): `GATEWAY_CLEARING` (tiền chờ cổng chuyển), `BANK` (tài khoản ngân hàng sàn — VietQR / COD đã nhận), `COD_RECEIVABLE` (hãng đang giữ tiền thu hộ), `ESCROW_LIABILITY`, `VENDOR_PAYABLE` (ví shop), `COMMISSION_REVENUE`, `SHIPPING_PAYABLE` (phí ship phải trả hãng), `MARKETING_EXPENSE` (voucher sàn / freeship / Điểm), `GATEWAY_FEE_EXPENSE`, `CUSTOMER_REFUND_PAYABLE`. Bảng bút toán mẫu cho từng `FinancialEventType` ghi vào ENTITY 6.7 (mục 3.3 của plan) | ☐ |
| D14 | **Bất biến sổ cái** | UC-PAY-06 ràng buộc ("ứng dụng chỉ INSERT, SELECT") | TLCN dùng một tài khoản CSDL → migration thêm trigger `trg_financial_ledger_append_only` (`BEFORE UPDATE OR DELETE … RAISE EXCEPTION`); entity không có setter; `LedgerPoster` kiểm Σ Nợ = Σ Có theo nhóm, lệch → `IllegalStateException` (rollback). `balance_after` = số dư lũy kế **của tài khoản đó**, tính bằng `SELECT … ORDER BY seq DESC LIMIT 1` trong cùng transaction sau khi lấy khóa tư vấn `pg_advisory_xact_lock(hash(account))` (hai nhóm song song không lệch) | ☐ |
| D15 | **Giải ngân** (UC-PAY-07) | API 12.2 (15 phút); UC-ORD-12 b3 ("giải ngân ngay trong lượt chạy") | `EscrowReleaseJob` 15 phút: `escrow.status = HELD AND release_eligible_at <= now AND orders.status = COMPLETED` (`FOR UPDATE SKIP LOCKED LIMIT 100`), mỗi ký quỹ một transaction: `EscrowLedger.release(now)` → `WalletPoster.credit(vendor, net, CREDIT_ESCROW_RELEASE, orderId)` → sổ cái `ESCROW_RELEASE`, `PAYOUT_ACCRUE`, chi phí sàn của đơn (`PLATFORM_VOUCHER`, `FREESHIP_COST`, `COIN_COST` theo `order_discounts` / `coin_discount_amount`) → audit `ESCROW_RELEASED`. `EscrowFacade.releaseNow(orderId)` dùng chung cho C5 (khách bấm "Đã nhận hàng") | ☐ |
| D16 | **Ví chưa tồn tại** | ENTITY 6.4 (một shop một ví) | Cài `ShopBootstrapContributor` (cổng của vendor) tạo ví khi shop `VERIFIED`; `WalletPoster` tự tạo nếu thiếu (shop cũ trước plan); seed: kiểm `R__seed_demo_30_shops.sql` đã có ví (dòng `balance_hash` dùng `local-wallet-secret`) | ☐ |
| D17 | **Thông báo** | UC-LOG-01 b1; UC-PAY-07 b3 | Online thành công: người mua `PAYMENT_SUCCESS` + từng shop `NEW_ORDER_FOR_VENDOR` (giống `CheckoutNotificationListener` của COD); thất bại: `PAYMENT_FAILED` (kèm nút thanh toán lại); giải ngân: shop `WALLET_CREDITED`. Đẩy trạng thái phiên realtime tới người mua qua `UserChannel` (`/user/queue/payments`) để màn VietQR tự chuyển (web vẫn hỏi lại mỗi 3 giây phòng mất kết nối) | ☐ |
| D18 | **Đối soát hằng đêm** | API 12.2 ("Đối soát ví / sổ cái, báo `ESCROW_DISCREPANCY`") | `LedgerReconciliationJob` 02:00 giờ VN: (a) mỗi ví `verifyHash()`; (b) Σ Nợ = Σ Có toàn sổ và theo từng `reference_id` trong ngày; (c) ví = Σ `wallet_transactions`; lệch → `AdminNotification` `ESCROW_DISCREPANCY` (loại thông báo Admin đã có? kiểm E00.04) + log ERROR — không tự sửa | ☐ |
| D19 | **Phí cổng** | `FinancialEventType.GATEWAY_FEE` | `genie.payment.vnpay.fee-percent` (mặc định `0`) — `> 0` mới ghi `GATEWAY_FEE`; VietQR không phí | ☐ |
| D20 | **Cờ web** | `isMocked(feature)` | `payment` — thanh toán VNPay / VietQR / thanh toán lại / trang kết quả. Cờ `checkout` (C2) giữ nguyên | ☐ |
| D21 | **Ngoài phạm vi** | — | Hoàn tiền (C8 — `PaymentRefund`), hủy đơn (C5), đối soát COD với hãng (C4 — dùng `bank_transactions` của plan này), rút tiền (C9), màn Sổ cái Admin (C10 đọc), MoMo / ZaloPay / thẻ quốc tế (DATN) | ☐ |

---

## 3. Luồng nghiệp vụ chi tiết

### 3.1 Khởi tạo & thanh toán VNPay (UC-PAY-01, PAY-03)

```
POST /me/checkout-sessions (C2) ──► CheckoutService.place ──► PaymentInitiator.initiate(req)  [bean thật của payment]
   VNPAY: tạo payment_transactions{session, amount, PENDING, payment_code=TR{id}} ──► VnpayUrlBuilder (sắp tham số ASCII,
          HMAC-SHA512 VNPAY_HASH_SECRET, vnp_TxnRef=TR{id}, vnp_Amount=amount*100, vnp_ExpireDate=expires_at giờ VN) ──► payment_url
   VIETQR: tạo giao dịch như trên ──► khối vietqr{bank, account_number, account_name, amount, content=TR{id}, qr_url, expires_at}
   COD:    không tạo giao dịch (Payment.cod())
Trình duyệt ──► VNPay ──► GET /payments/vnpay/return (chỉ kiểm chữ ký, 302 {return_url}?session_id=…&result=…)   [N4]
VNPay ──► GET /payments/vnpay/ipn ──► VnpayIpnService (D5) ──► OrderPaymentFacade.markSessionPaid (D4)
          ──► EscrowFacade.holdOnline(orders) (D12) ──► LedgerPoster ESCROW_IN (+ GATEWAY_FEE) ──► {"RspCode":"00"}
          AFTER_COMMIT: PAYMENT_SUCCESS (khách) + NEW_ORDER_FOR_VENDOR (shop) + đẩy /user/queue/payments
GET /me/checkout-sessions/{id} (6.11) — web hỏi mỗi 3 giây: payment.status PENDING | SUCCESS | FAILED, đơn con
POST /me/checkout-sessions/{id}/payments (6.12) — thanh toán lại / đổi phương thức (D6)
```

### 3.2 Webhook SePay (UC-PAY-03 nhánh VietQR)

```
POST /payments/sepay/webhook  (Authorization: Apikey {SEPAY_API_KEY} — sai → 401, không ghi gì)
  ├─ INSERT bank_transactions(sepay_id UNIQUE, …) ON CONFLICT DO NOTHING → đã có → 200 {"success":true}   (N5)
  ├─ BankTransferRouter: transfer_type IN/OUT + mã trong content (D9) → BankTransferMatcher đã đăng ký
  │     IN  + TR{id}   → PaymentTransferMatcher (C3): đủ tiền → như IPN thành công; lệch / hết hạn → UNMATCHED (D9, D10)
  │     IN  + không mã → COD matcher (C4, khi có) hoặc UNMATCHED
  │     OUT + TRRF{id} → C8 ; OUT + TRWD{id} → C9 (chưa có bean → UNMATCHED, giữ nguyên dòng để khớp lại sau)
  └─ luôn 200 {"success": true}
```

### 3.3 Bút toán mẫu (ghi vào ENTITY 6.7 — số liệu ví dụ: đơn 1.000.000₫ hàng + 30.000₫ ship, hoa hồng 5%, voucher sàn 50.000₫)

| Sự kiện | Nợ | Có |
|---|---|---|
| `ESCROW_IN` (VNPay báo thành công, khách trả 980.000₫) | `GATEWAY_CLEARING` 980.000 | `ESCROW_LIABILITY` 950.000 · `SHIPPING_PAYABLE` 30.000 |
| `ESCROW_IN` (COD — khi C4 đối soát) | `BANK` (thực nhận) | `COD_RECEIVABLE` |
| `COMMISSION` (khi giải ngân) | `ESCROW_LIABILITY` 50.000 | `COMMISSION_REVENUE` 50.000 |
| `PLATFORM_VOUCHER` (khi giải ngân) | `MARKETING_EXPENSE` 50.000 | `ESCROW_LIABILITY` 50.000 |
| `ESCROW_RELEASE` + `PAYOUT_ACCRUE` | `ESCROW_LIABILITY` 950.000 | `VENDOR_PAYABLE` 950.000 |
| `GATEWAY_FEE` (nếu cấu hình) | `GATEWAY_FEE_EXPENSE` | `GATEWAY_CLEARING` |

Tổng Nợ = tổng Có trong từng nhóm; `ESCROW_LIABILITY` của một đơn về 0 sau giải ngân (test kiểm điều này).

### 3.4 Giải ngân (UC-PAY-07)

```
EscrowReleaseJob (15 phút, SKIP LOCKED, lô 100)  /  EscrowFacade.releaseNow(orderId) (C5 gọi khi khách xác nhận)
  └─ mỗi ký quỹ một transaction: escrow.isReleasable(now)? → release(now) → WalletPoster.credit (FOR UPDATE ví, hash)
     → LedgerPoster nhóm {COMMISSION, PLATFORM_VOUCHER/FREESHIP_COST/COIN_COST, ESCROW_RELEASE, PAYOUT_ACCRUE}
     → AuditLogger ESCROW_RELEASED → AFTER_COMMIT: WALLET_CREDITED cho shop
```

---

## 4. Cây file sẽ tạo / sửa

```
server-spring/src/main/resources/db/migration/V28__c3_payment_escrow.sql         (mới — số tạm)
server-spring/src/main/java/com/genie/
├── common/error/ErrorCode.java                         (sửa) khối // ===== Payment: PAY-4021, 4041, 4092, 4093, 4094
├── order/checkout/application/OrderPaymentFacade.java  (mới — cổng order, cài trong order, D4)
├── order/checkout/application/CheckoutPaymentService.java (mới — 6.11 khối payment, 6.12 thanh toán lại)
├── order/checkout/api/CheckoutSessionController.java   (sửa — POST /{id}/payments)
└── payment/
    ├── vnpay/   domain (PaymentTransaction sửa: session, paymentCode, expiresAt) · application (VnpayPaymentInitiator,
    │            VnpayUrlBuilder, VnpaySignature, VnpayIpnService, PaymentModeConfig) · api (VnpayController: return, ipn)
    │            · infra (DevPaymentSimulatorController — @Profile("local"))
    ├── sepay/   domain (BankTransaction, BankTransactionRepository, TransferType, BankTransactionStatus)
    │            application (SepayWebhookService, BankTransferRouter, BankTransferMatcher, PaymentTransferMatcher, VietQrBuilder)
    │            api (SepayWebhookController)
    ├── escrow/  application (EscrowFacade, EscrowOpenListener (COD), EscrowReleaseJob, EscrowAmounts)
    ├── ledger/  domain (LedgerAccount) · application (LedgerPoster, LedgerGroup, LedgerReconciliationJob)
    └── wallet/  application (WalletPoster, WalletHasher, WalletBootstrap — cài ShopBootstrapContributor)
server-spring/src/test/java/com/genie/payment/
    VnpaySignatureTest · VnpayUrlBuilderTest · LedgerGroupTest · EscrowAmountsTest · ContentCodeParserTest
    VnpayPaymentIT · VnpayIpnIT · VnpayIpnConcurrencyIT · CheckoutRetryPaymentIT · SepayWebhookIT · LateTransferIT
    CodEscrowIT · LedgerAppendOnlyIT · EscrowReleaseIT · EscrowReleaseConcurrencyIT · LedgerReconciliationIT
web/src/features/checkout/infrastructure/checkout.api.ts       (sửa — 6.11, 6.12, bỏ settleMockPayment khi cờ bật)
web/src/features/payment/…                                     (sửa — màn VietQR, trang giả lập cổng dev, kết quả)
web/src/shared/api/error-codes.ts · web/src/i18n/{vi,en}.json
docs/Document/…  ENTITY (6.1, 6.7, 6.9 mới, Phụ lục E) · API_SPEC (6.10–6.12, 12.1, 15) · Usecase.md · CHECKLIST · BAO_CAO · xlsx
docs/api-tests/payments.http · docs/kiem-thu/kich-ban-2026-11-28-c3.md · docs/cau-hinh-moi-truong.md · .env.example · deploy/.env.example
```

---

## 5. Bảng ánh xạ Use case → API → Dữ liệu

| Bước UC | Endpoint / tác vụ | Bảng | Quyền |
|---|---|---|---|
| UC-PAY-01 b1–4: tạo URL VNPay / khối VietQR | `POST /me/checkout-sessions` (C2 gọi `PaymentInitiator`), `POST /me/checkout-sessions/{id}/payments` | `payment_transactions` | CUSTOMER / VENDOR mua hàng |
| UC-PAY-01 màn kết quả / VietQR | `GET /me/checkout-sessions/{id}`, WS `/user/queue/payments` | `checkout_sessions`, `payment_transactions`, `orders` | chủ phiên |
| UC-PAY-03 IPN | `GET /payments/vnpay/ipn` · `GET /payments/vnpay/return` | `payment_transactions`, `orders`, `stock_reservations`, `product_variants`, `escrow_ledgers`, `financial_ledger` | chữ ký HMAC-SHA512 |
| UC-PAY-03 SePay | `POST /payments/sepay/webhook` | `bank_transactions` + như trên | `Authorization: Apikey` |
| UC-PAY-05 | listener `CheckoutCreatedEvent` (COD), IPN / webhook (online) | `escrow_ledgers`, `order_items` (đọc) | nội bộ |
| UC-PAY-06 | `LedgerPoster` (mọi chuỗi gọi), `LedgerReconciliationJob` 02:00 | `financial_ledger` | nội bộ |
| UC-PAY-07 | `EscrowReleaseJob` 15 phút, `EscrowFacade.releaseNow` | `escrow_ledgers`, `vendor_wallets`, `wallet_transactions`, `financial_ledger`, `system_logs` | nội bộ |

**Mã lỗi** (API 15): `GNE-PAY-4021` (402 cổng từ chối — trả ở 6.11 khi giao dịch `FAILED`), `GNE-PAY-4092` (409 IPN trùng — nội bộ, cổng vẫn nhận 200), `GNE-PAY-4093` (409 ký quỹ đã giải ngân / hoàn — C8 dùng), **mới:** `GNE-PAY-4094` (409 phiên không còn chờ thanh toán), `GNE-PAY-4041` (404 phiên không thuộc người gọi). Dùng lại `GNE-INV-4093` (giữ chỗ hết hạn), `GNE-ORD-4031`.

---

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — `ModuleBoundaryTest` (**`order` không import `payment`**), `SchemaValidationIT`, `SeedDataIT`.
2. Swagger: endpoint mới có `@Operation(summary = "UC-PAY-0x …")`; webhook / IPN **không** yêu cầu JWT (đăng ký ở `SecurityConfig` đúng đường dẫn, không mở rộng hơn).
3. `docs/api-tests/payments.http`: IPN chữ ký sai (`97`), đúng (`00`), gửi lại (`02`), sai tiền (`04`); webhook SePay thiếu Apikey (`401`), trùng `id` (200, không ghi lần 2).
4. Đụng `web/`: `cd web && npm run verify`.
5. Tick progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Exx.yy` = task; `Exx.Cn` = task commit.

### E00 — Chuẩn bị

- **[E00.01]** Chạy `git config user.name` / `user.email` — ghi vào dòng *Phụ trách* của progress (đây là tên dùng khi tick checklist ở E10); kiểm `develop` mới nhất, đọc lại README ngày (mục 3 đồ thị, mục 4 điểm chung, mục 5 migration).
- **[E00.02]** Chốt D1–D21 vào progress (cột *Chốt* + ngày).
- **[E00.03]** Tạo nhánh `feature/c3-ledger-escrow` từ `develop`.
- **[E00.04]** Đọc CHECK constraint thật của `payment_transactions.status`, `escrow_ledgers.status`, `wallet_transactions.type`, `admin_notifications.type` (có `ESCROW_DISCREPANCY`, chuyển khoản lệch chưa?) trong `V6`, `V10`; đọc `CheckoutPricer` để biết `shop_shipping_subsidy` / freeship shop được lưu ở cột nào (D12); đọc `R__seed_demo_30_shops.sql` (ví đã có? — D16).
- **[E00.05]** Kiểm `CheckoutCreatedEvent` được publish **trong** transaction của `CheckoutService.place` (D11); kiểm `SecurityConfig` đường dẫn công khai hiện có cho `/payments/**`.
- **[E00.06]** Đăng ký tài khoản sandbox VNPay (`sandbox.vnpayment.vn`) + tài khoản SePay thử (nếu có) — ghi biến vào `.env` máy dev (không commit); chưa có thì làm theo D3 (stub) và ghi "chạy thật cổng thật chờ khóa" vào *Ghi chú*.
- **[E00.07]** Đọc màn mock: trang Thanh toán (chọn phương thức), trang kết quả, màn VietQR (đếm ngược, QR, nội dung `TR…`), "Thanh toán lại" ở Đơn mua; `grep` mọi chỗ gọi `settleMockPayment`, `retryPayment`, `getCheckoutSession`.

### E01 — Tài liệu trước

- **[E01.01]** ENTITY 6.1 (`checkout_session_id`, `payment_code`, `expires_at`, D1–D2), 6.7 (`LedgerAccount`, bảng bút toán mẫu mục 3.3, trigger chỉ ghi thêm D14), **6.9 mới `BankTransaction`** (D8), Phụ lục E; `GIAI_NGHIA_ENTITY.md`; `java-code/Diagram06_*.java`.
- **[E01.02]** API_SPEC — 6.10 (khối `payment` VNPay / VietQR đủ trường), 6.11 (`payment.status`, `failure_code`), 6.12 (D6, mã lỗi), 12.1 (thứ tự kiểm IPN D5, mã `RspCode`, Return URL chỉ hiển thị, SePay D8–D10), 13 (kênh `/user/queue/payments`), 15 (`GNE-PAY-4041`, `4094`), lịch sử phiên bản.
- **[E01.03]** Usecase.md — UC-PAY-01 b2–3 (mã `TR{id}` dùng chung — D2), UC-PAY-03 b7 (thất bại không nhả giữ chỗ — D7), luồng SePay (tiền về muộn — D10), UC-PAY-06 b2 (danh mục tài khoản — D13).
- **[E01.04]** `docs/cau-hinh-moi-truong.md` + `.env.example` + `deploy/.env.example` — `PAYMENT_MODE`, `VNPAY_*`, `SEPAY_*` (`SEPAY_BANK_CODE`, `SEPAY_ACCOUNT_NAME` mới), ghi chú đăng ký IPN URL ở cổng VNPay.
- **[E01.C1]** COMMIT — `docs(payment): specify payment sessions, sepay bank log and ledger accounts`

### E02 — Migration & miền

- **[E02.01]** `V28__c3_payment_escrow.sql` — cột mới `payment_transactions` + UNIQUE `payment_code`, UNIQUE `(payment_method, gateway_transaction_id)` partial `WHERE gateway_transaction_id IS NOT NULL`; bảng `bank_transactions` + CHECK + index `(status, created_at)`; trigger chỉ ghi thêm `financial_ledger`; index `escrow_ledgers (status, release_eligible_at)`.
- **[E02.02]** Entity — `PaymentTransaction` (`checkoutSessionId`, `paymentCode`, `expiresAt`, `static forSession(...)`), `BankTransaction` + repo, `LedgerAccount`, `FinancialLedger.entry(...)` giữ nguyên chữ ký (thêm `account` kiểu enum → lưu tên).
- **[E02.03]** `ErrorCode` — 5 mã khối Payment.
- **[E02.04]** Test — `SchemaValidationIT`, `SeedDataIT` xanh; `LedgerAppendOnlyIT` (UPDATE / DELETE `financial_ledger` → lỗi CSDL).
- **[E02.C1]** COMMIT — `feat(db): add payment session link, sepay bank log and append-only ledger`

### E03 — Sổ cái kép (UC-PAY-06)

- **[E03.01]** `LedgerGroup` (builder: `debit(account, amount)`, `credit(...)`, `reference`, `orderId`, `vendorId`, `event`) — hàm thuần kiểm cân, không âm, ≥ 2 dòng.
- **[E03.02]** `LedgerPoster.post(group)` — `@Transactional(propagation = MANDATORY)` (bắt buộc chạy trong transaction nghiệp vụ), khóa tư vấn theo tài khoản (D14), tính `balance_after`, chèn lô.
- **[E03.03]** `LedgerReconciliationJob` (D18) — 02:00 giờ VN, `@SchedulerLock` theo tiền lệ job khác (kiểm ShedLock có chưa; không có thì `SKIP LOCKED` trên bảng khóa / chạy một instance), báo AdminNotification.
- **[E03.04]** Test — `LedgerGroupTest` (lệch 1₫ → lỗi; nhóm 6 dòng cân), `LedgerPosterIT` (gọi ngoài transaction → lỗi; 50 nhóm song song cùng tài khoản → `balance_after` tăng đúng thứ tự `seq`), `LedgerReconciliationIT` (sửa tay hash ví bằng SQL → có AdminNotification).
- **[E03.C1]** COMMIT — `feat(payment): add append-only double-entry ledger poster and nightly reconciliation`

### E04 — Ký quỹ & ví (UC-PAY-05, nền UC-PAY-07)

- **[E04.01]** `EscrowAmounts.of(order, items)` (D12) — hàm thuần + test bảng ca (nhiều ngành hoa hồng khác nhau, freeship shop, voucher sàn không trừ shop).
- **[E04.02]** `EscrowFacade` — `openCod(orderId)`, `holdOnline(orderIds, paymentCode, now)`, `fundCod(orderId, amount, now)` (C4 đối soát gọi), `scheduleRelease(orderId, at)`, `releaseNow(orderId)`, `statusOf(orderId)` (cho C5 / C8 / C10 đọc); `freeze/dispute/refund` để trống chữ ký (C8 cài thân).
- **[E04.03]** `EscrowOpenListener` (D11) — `@EventListener` đồng bộ `CheckoutCreatedEvent`, COD → `openCod` từng đơn.
- **[E04.04]** `WalletHasher` (HMAC khóa `genie.wallet.hash-secret` — kiểm tên khóa seed đang dùng), `WalletPoster.credit/debit(vendorUserId, amount, type, reference)` (N7) + `WalletBootstrap` cài `ShopBootstrapContributor` (D16).
- **[E04.05]** Test — `EscrowAmountsTest`, `CodEscrowIT` (đặt COD 2 shop → 2 ký quỹ `PENDING_FUNDING` đúng số; rollback đặt hàng → không có ký quỹ), `WalletPosterIT` (credit / debit, hash đổi đúng, debit quá số dư → lỗi, song song 20 credit → số dư đúng).
- **[E04.C1]** COMMIT — `feat(payment): open escrow for cod orders and add wallet poster`

### E05 — VNPay (UC-PAY-01 nhánh VNPay, UC-PAY-03) — nhánh `feature/c3-vnpay`

- **[E05.01]** `VnpaySignature` (sắp tham số ASCII, URL-encode kiểu VNPay `+` cho khoảng trắng, HMAC-SHA512) + `VnpayUrlBuilder` (tham số API 2.1.0: `vnp_Version=2.1.0`, `vnp_Command=pay`, `vnp_Locale=vn`, `vnp_OrderType=other`, `vnp_IpAddr`, `vnp_CreateDate` / `vnp_ExpireDate` giờ VN `yyyyMMddHHmmss`).
- **[E05.02]** `OrderPaymentFacade` trong order (D4) + `OrderPaidEvent` phát sau khi đổi; test đơn vị cho máy trạng thái (`markPaid` chỉ từ `PENDING_PAYMENT`).
- **[E05.03]** `VnpayPaymentInitiator implements PaymentInitiator` (`@ConditionalOnProperty` theo D3 — bean thật thay `stubPaymentInitiator`) — VNPAY / VIETQR (khối `vietqr` qua `VietQrBuilder` — dùng lại ở E06) / COD.
- **[E05.04]** `VnpayIpnService` (D5) + `VnpayController` (`/payments/vnpay/ipn`, `/payments/vnpay/return` — D-N4) + `SecurityConfig` mở đúng 2 đường dẫn.
- **[E05.05]** `CheckoutPaymentService` (order) — 6.11 khối `payment`, 6.12 thanh toán lại / đổi phương thức (D6) + `CheckoutSessionController` `POST /{id}/payments` (Idempotency-Key bắt buộc — `IdempotencyStore` của C2).
- **[E05.06]** Thông báo `AFTER_COMMIT` (D17) — `PaymentNotificationListener` trong **payment** (gọi `NotificationFacade`) + đẩy `/user/queue/payments` (thêm hằng `UserChannel.PAYMENTS`).
- **[E05.07]** `DevPaymentSimulatorController` `@Profile("local")` (D3) — dựng IPN có chữ ký đúng từ `payment_code` rồi gọi `VnpayIpnService` (không đi đường tắt).
- **[E05.08]** Test — `VnpaySignatureTest` (bộ vector mẫu từ tài liệu VNPay), `VnpayPaymentIT` (đặt VNPAY → `payment_url` hợp lệ, giao dịch `PENDING`), `VnpayIpnIT` (6 nhánh D5; thành công → mọi đơn con `PAID`, tồn cứng giảm, giữ chỗ `CONFIRMED`, ký quỹ `HELD`, bút toán cân), **`VnpayIpnConcurrencyIT`** (IPN gửi 5 lần song song + Return → đúng một lần `PAID`, một nhóm bút toán — N10), `CheckoutRetryPaymentIT` (thất bại → thanh toán lại → thành công; đổi sang COD; phiên hết hạn → `GNE-INV-4093`).
- **[E05.C1]** COMMIT — `feat(order): add order payment facade and retry payment endpoint`
- **[E05.C2]** COMMIT — `feat(payment): initiate vnpay payments and confirm them through signed ipn`

### E06 — VietQR qua SePay (UC-PAY-01 nhánh VietQR, UC-PAY-03 nhánh SePay) — nhánh `feature/c3-sepay-vietqr`

- **[E06.01]** `VietQrBuilder` — `qr_url = https://qr.sepay.vn/img?acc=…&bank=…&amount=…&des=TR{id}`, tên tài khoản, hạn = `expires_at`.
- **[E06.02]** `SepayWebhookController` + `SepayWebhookService` — kiểm `Authorization: Apikey` (so sánh hằng thời gian), ghi `bank_transactions` trước (N5), luôn `200 {"success": true}`.
- **[E06.03]** `BankTransferRouter` + `ContentCodeParser` (D9) + interface `BankTransferMatcher { boolean supports(code); void match(bankTx) }`.
- **[E06.04]** `PaymentTransferMatcher` — đủ tiền → cùng đường thành công với IPN (tách `PaymentSuccessHandler` dùng chung VNPay / SePay để không lặp code); lệch → `UNMATCHED` + AdminNotification; muộn (D10).
- **[E06.05]** Test — `ContentCodeParserTest` (`TR 123`, `tr123`, `TRRF12`, `TRWD9`, nội dung ngân hàng thêm chữ), `SepayWebhookIT` (sai Apikey 401; đủ tiền → đơn `PAID`; gửi lại cùng `id` → không đổi gì; thiếu tiền → `UNMATCHED`; tiền ra không có matcher → `UNMATCHED`), `LateTransferIT` (phiên hết hạn đơn `AUTO_CANCELLED` → `UNMATCHED` `LATE_PAYMENT_REFUND`).
- **[E06.C1]** COMMIT — `feat(payment): accept vietqr transfers through the sepay webhook with a bank transaction log`

### E07 — Giải ngân ký quỹ (UC-PAY-07) — nhánh `feature/c3-escrow-release`

- **[E07.01]** `EscrowFacade.releaseNow` + `EscrowReleaseJob` (D15) — bút toán đủ nhóm theo mục 3.3 (+ chênh lệch phí ship thật `shipment_trackings.shipping_fee_actual` khi C4 đã có — âm ghi `FREESHIP_COST`), audit `ESCROW_RELEASED`, thông báo `WALLET_CREDITED`.
- **[E07.02]** Bỏ qua ký quỹ `FROZEN` / `DISPUTED` / `PENDING_FUNDING` (ràng buộc UC-ORD-12) — test riêng từng trạng thái.
- **[E07.03]** Test — `EscrowReleaseIT` (dựng đơn `COMPLETED` + ký quỹ `HELD` quá hạn → ví +net, `wallet_transactions` đúng `balance_before / after`, `ESCROW_LIABILITY` của đơn về 0, hash ví hợp lệ), **`EscrowReleaseConcurrencyIT`** (job + `releaseNow` cùng lúc trên một ký quỹ → ví cộng đúng một lần — N10), ký quỹ `DISPUTED` không bị chọn.
- **[E07.C1]** COMMIT — `feat(payment): release completed escrows to seller wallets every 15 minutes`

### E08 — Nối web (cờ `payment`)

- **[E08.01]** `checkout.api.ts` — `getCheckoutSession` (6.11 khối `payment`), `retryPayment` (6.12, Idempotency-Key), `settleMockPayment` chỉ còn ở nhánh mock; DTO / mapper `payment` + `vietqr`.
- **[E08.02]** Màn VietQR — QR thật, số tài khoản / tên / nội dung `TR…` có nút chép, đếm ngược tới `expires_at`, hỏi 6.11 mỗi 3 giây + nghe `/user/queue/payments` (hub realtime đã có), tự chuyển trang thành công; hết giờ → màn "Hết thời gian giữ hàng".
- **[E08.03]** VNPay — chuyển hướng `payment_url`; trang kết quả đọc `?session_id` rồi hỏi 6.11 (không tin tham số URL — N4); thất bại → nút "Thanh toán lại" / "Đổi phương thức" (6.12).
- **[E08.04]** Trang giả lập cổng dev `/checkout/mock-gateway` (chỉ build có `payment` + biến `PUBLIC_DEV_TOOLS=true`) — nút "Thanh toán thành công" / "Hủy" gọi endpoint giả lập (D3).
- **[E08.05]** `error-codes.ts` + vi / en (5 mã PAY + khóa mới); test mapper; `npm run verify`.
- **[E08.C1]** COMMIT — `feat(web): connect vnpay and vietqr payment screens to the real api`

### E09 — Dữ liệu demo, kiểm thử & chạy thật

- **[E09.01]** Seed — bảo đảm mọi shop seed có ví (`R__seed_demo_30_shops.sql`, hash bằng khóa máy dev); không seed giao dịch thanh toán (tạo bằng luồng thật).
- **[E09.02]** `docs/api-tests/payments.http` — tạo phiên VNPay / VietQR / COD, IPN 6 nhánh (kèm script tính chữ ký — `scripts/dev/vnpay-sign.mjs` hoặc ghi công thức), SePay webhook (đủ / thiếu / trùng / sai Apikey), thanh toán lại, đổi COD.
- **[E09.03]** `docs/kiem-thu/kich-ban-2026-11-28-c3.md` — TC-C3-01…: VNPay sandbox thành công / hủy / thanh toán lại; VietQR (giả lập webhook + nếu có tài khoản SePay thì chuyển khoản thật số tiền nhỏ); COD tạo ký quỹ chờ; IPN trùng; đơn 2 shop một lần thanh toán; giải ngân (dựng đơn `COMPLETED` bằng SQL vì C5 chưa có — ghi rõ); sổ cái cân; khổ 360 px; 9 theme màn VietQR.
- **[E09.04]** Chạy thật — web build `PUBLIC_REAL_API=…,cart,checkout,payment` + backend `local` (`PAYMENT_MODE=http` nếu có khóa sandbox, không thì `stub` + trang giả lập) + Postgres + Redis + Silo + Mailpit; Chromium (Playwright); IPN sandbox cần URL công khai → dùng `cloudflared` / `ngrok` tạm thời (ghi vào kịch bản); lỗi phát hiện sửa ngay (mỗi lỗi một commit `fix(…)`).
- **[E09.C1]** COMMIT — `docs(payment): add payment api tests and real-run scenarios`

### E10 — Hoàn thiện use case & tick tiến độ

- **[E10.01]** OpenAPI — đủ endpoint, summary `UC-PAY-01/03`; webhook ghi chú xác thực.
- **[E10.02]** `docs/web-backend-handoff.md` — nhóm `payment` đã nối; ghi `settleMockPayment` chỉ còn ở mock.
- **[E10.03]** Usecase.md — *Hiện trạng* 5 UC → "Đã triển khai (ngày …)" + phần để chuỗi sau (hoàn tiền C8, đối soát COD C4).
- **[E10.04]** `CHECKLIST_TIEN_DO_UC.md` — tick 5 lớp 5 UC (ô chưa đạt → `◐` + phần thiếu, vd "IPN chưa kiểm với VNPay thật"), Trạng thái, **cột Phụ trách = `git config user.name` đã ghi ở E00.01**, số mục 1 + phân hệ 04 (đếm lại từ dòng UC), dòng mục 5 *Nhật ký*.
- **[E10.05]** `BAO_CAO_UC_DOC_LAP.md` — mục C3: dòng *Tiến độ* + *Phụ trách*; C4, C5, C8, C9 ghi "sổ cái / ký quỹ / ví / sổ ngân hàng SePay đã có (C3)".
- **[E10.06]** `Quản Lý UseCase.xlsx` — *Hiện trạng mã nguồn* 5 UC, cột AA = tên ở E00.01.
- **[E10.07]** README ngày (mục 7) — tick dòng C3, điền Phụ trách.
- **[E10.C1]** COMMIT — `docs(payment): record payment, escrow, ledger and payout release as implemented`
- **[E10.08]** Mở 4 PR xếp chồng vào `develop` (`c3-ledger-escrow` → `c3-vnpay` → `c3-sepay-vietqr` → `c3-escrow-release`); mỗi PR ghi nhánh nền ở dòng đầu; PR đầu nhờ người làm C2 duyệt `OrderPaymentFacade` + `CheckoutSessionController`.

---

## 8. Ngoài phạm vi

- Hoàn tiền qua cổng / chuyển khoản tay (C8 — dùng `PaymentTransaction.recordRefund`, `bank_transactions` OUT `TRRF`).
- Đối soát tiền thu hộ COD với hãng (C4 — `BankTransferMatcher` cho tiền vào không mã + bảng kê).
- Hủy đơn / hoàn tất đơn (C5 — gọi `EscrowFacade.releaseNow`), rút tiền (C9 — `WalletPoster.debit`), màn Sổ cái / Dashboard (C10).

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| IPN và Return URL / IPN gửi lại chạy song song → cộng tiền 2 lần | Khóa dòng giao dịch `FOR UPDATE` + kiểm trạng thái trước khi đổi; Return không đổi gì (N4); `VnpayIpnConcurrencyIT` |
| Lệch chữ ký VNPay do mã hóa URL | Bộ vector mẫu chính thức trong `VnpaySignatureTest`; log chuỗi băm (không log khóa) khi lệch ở `local` |
| Webhook SePay gửi lại / tấn công giả | Apikey so sánh hằng thời gian; UNIQUE `sepay_id`; luôn 200 |
| Vòng phụ thuộc `order ↔ payment` | Cổng `OrderPaymentFacade` nằm trong order; `ModuleBoundaryTest` |
| Sổ cái lệch khi nhiều nhóm ghi song song | Khóa tư vấn theo tài khoản; job đối soát hằng đêm |
| IPN sandbox không gọi được máy dev | Đường hầm tạm (`cloudflared`), hoặc trang giả lập D3 chạy đúng code IPN |
| Shop cũ chưa có ví | `WalletPoster` tự tạo + seed bổ sung (D16) |
