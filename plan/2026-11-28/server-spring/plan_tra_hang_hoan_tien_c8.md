# PLAN — Chuỗi C8 Trả hàng & hoàn tiền (UC-SUP-01 · UC-SUP-02 · UC-SUP-03 · UC-SUP-04)

| | |
|---|---|
| **Ngày lập** | 2026-11-28 |
| **Thể loại** | server-spring (module `support` feature `refund` (+ `dispute` mới); hoàn tiền cổng ở `payment/vnpay`, khớp chuyển khoản ở `payment/sepay`) + nối `web/` (feature `refunds`, `seller-orders` phần trả hàng, `admin-disputes`) |
| **Use case** | Tạo yêu cầu Trả hàng / Hoàn tiền (UC-SUP-01) · Người bán tiếp nhận và xử lý yêu cầu trả hàng (UC-SUP-02) · Nhân viên sàn phân xử tranh chấp khiếu nại (UC-SUP-03) · Thực hiện hoàn tiền — VNPay / VietQR / COD (UC-SUP-04) |
| **Tài liệu chuẩn** | UC phân hệ 09 (UC-SUP-01 dòng 2336, 02 dòng 2371, 03 dòng 2400, 04 dòng 2427) · API 7.5–7.8, 10.4, 11.4, 12.2 (hạn phản hồi 15 phút), 15 (`GNE-SUP-*`, `GNE-PAY-4093`) · ENTITY 8.1 `RefundRequest` (**bảng đồng bộ RefundRequest ↔ Order ↔ EscrowLedger**, ví dụ số), 8.2, 8.3, 6.2 `PaymentRefund`, 6.3 `EscrowLedger`, 7.1 `ShipmentTracking.forReturn` · TS 14 (hoàn một phần nhiều lần) — xem [README](../README.md) |
| **Theo dõi tiến độ** | [progress_tra_hang_hoan_tien_c8.md](progress_tra_hang_hoan_tien_c8.md) — cùng mã task `Uxx.yy` |
| **Phụ trách** | Người làm plan — lấy theo **`git config user.name`** trên máy thực hiện (ghi vào progress ở U00.01); không ghi tên cố định |
| **Người commit** | danh tính git đã cấu hình trên máy đó — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Phụ thuộc** | **C3** (`EscrowFacade`, `LedgerPoster`, `WalletPoster`, `PaymentTransaction.recordRefund`, `bank_transactions` + `BankTransferRouter`), **C4** (`ShipmentFacade.createReturn`, webhook vận đơn `RETURN`, cổng `CancellationRefunds`, `OrderLifecycleService`), **C5** (đơn `DELIVERED`, cờ `can_request_return`, hủy đơn đã trả gọi `CancellationRefunds`) |
| **Mục tiêu** | 4 use case ⬜ → ✅. Khách gửi yêu cầu có bằng chứng, ký quỹ bị phong tỏa; shop có 48 giờ (tự đồng ý khi im lặng); sàn phân xử; hoàn tiền VNPay tự động / VietQR + COD chuyển khoản tay có SePay xác nhận; khách + shop + sàn luôn bằng tiền gốc của đơn |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `1b66eae`, 2026-11-28)

| Phần | Đã có | Còn thiếu |
|---|---|---|
| **Bảng / entity** (`V8__refund.sql`) | `refund_requests` (`order_id`, `requested_by_user_id`, `refund_amount`, `reason`, `request_type`, `resolution_mode`, `status`, `resolved_by_staff_id`, `resolution_note`, `requested_at`, `shop_response_due_at`, `evidence_requested_at`, `evidence_request_note`, `resolved_at`, `shop_rejected_at`, `return_due_at`, `return_shipped_at`, `return_received_at`); `refund_request_items` (`order_item_id`, `quantity`, `item_refund_amount`); `refund_evidences` (`media_url`, `media_type`, `display_order`, `submission_round`); `payment_refunds` (`payment_transaction_id`, `refund_request_id`, `gateway_refund_id`, `amount`, `status`). `RefundRequest` có đủ phương thức máy trạng thái (`approve`, `approveByShop`, `autoApproveIfShopSilent`, `closeIfCustomerSilent`, `closeIfEvidenceNotProvided`, `confirmReturnReceived`, `escalateToDispute`, `failReturnIfOverdue`, `markRefunded`, `markReturnShipped`, `reject`, `requestMoreEvidence`, `shopReject`, `submitAdditionalEvidence`); `EscrowLedger.dispute / freeze / releaseDisputeHold / applyPartialRefund / markRefunded`; `Order.requestReturn / approveReturn / rejectReturn / markRefunded`; `PaymentRefund.markSuccess / markFailed` | ⚠ Không lưu **tài khoản nhận hoàn** của khách (VietQR / COD) → cột snapshot (D9); không có mã chuyển khoản `TRRF{id}` riêng (dùng id được nhưng cần cột để khớp nhanh — D9); ký quỹ không nhớ trạng thái trước khi phong tỏa (D13). Toàn bộ service / API / job |
| **Module** | `support/refund/domain` chỉ có entity + repo | `support → order, payment, shipping, notification, promotion` (cạnh mới — README mục 3; `support → promotion` để hoàn Điểm — thêm vào README) |
| **Upload** | `UploadPurpose` chưa có bằng chứng | `REFUND_EVIDENCE` (bucket **riêng tư** `refund-evidence`, ảnh ≤ 10 MB, video ≤ 100 MB — D4) |
| **Mã lỗi** | — | `GNE-SUP-4091`, `4092`, `4221` (API 15), `GNE-PAY-4093` (C3 đã thêm); **mới** `GNE-SUP-4093` (yêu cầu không ở bước cho phép thao tác), `GNE-SUP-4222` (số lượng / số tiền trả vượt phần đã mua) |
| **Web** | `refunds/refund.api.ts` (`listBanks`, `createRefund`, `addEvidence`, `escalateRefund`, `acceptRejection`), `seller-orders/refund.api.ts` (`listSellerRefunds`, `actOnRefund`), `admin-disputes/dispute.api.ts` (`listDisputes`, `resolveDispute`, `confirmReturnReceived`, `reopenDispute`, `freezeEscrow`, `unfreezeEscrow`, `syncRefund`, `retryRefund`, `markRefundTransferred`) — mock | Nối theo cờ (D21) |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | **Ba máy trạng thái đi cùng nhau** — mọi chuyển của `RefundRequest` cập nhật `Order` (qua `OrderLifecycleService` — C4) và `EscrowLedger` (qua `EscrowFacade` — C3) **trong cùng transaction**, đúng bảng đồng bộ ENTITY 8.1; một lớp `RefundStateSync` duy nhất làm việc này (test theo bảng) | ENTITY 8.1 |
| N2 | **Tiền hoàn ≤ tiền khách đã trả cho đúng các món đó**: `item_refund_amount ≤ (line_total − prorated_discount) × qty_trả / qty_mua`; tổng mọi lần hoàn của đơn ≤ `grand_total` (COD) / `PaymentTransaction.refundableAmount()` (VNPay); khách + shop + sàn = tiền gốc | UC-SUP-01, 04 ràng buộc; TS 14 |
| N3 | Một đơn có **tối đa một** yêu cầu đang mở; trả tiếp món khác chỉ khi yêu cầu trước đã đóng (`PARTIALLY_REFUNDED` cho phép tạo mới) | API 15 `GNE-SUP-4092` |
| N4 | Gọi cổng hoàn tiền **ngoài** transaction dài: tạo `PaymentRefund PENDING` (tx 1) → gọi VNPay → ghi kết quả + đổi trạng thái (tx 2); lỗi → `FAILED`, **không** giải ngân cho shop khi chưa hoàn xong | UC-SUP-04 1a |
| N5 | Cron hạn chót `FOR UPDATE SKIP LOCKED`, mỗi yêu cầu một transaction, gọi đúng phương thức entity | LLD 10 |
| N6 | Bằng chứng ở bucket **riêng tư**; xem qua URL ký tạm 10 phút, chỉ khách đó, shop của đơn, nhân viên có quyền | LUU_TRU; TS 9 |
| N7 | Sổ cái mọi lần hoàn qua `LedgerPoster` (C3); thu hồi ví shop qua `WalletPoster.debit` (`DEBIT_REFUND`) | README mục 4 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Điều kiện tạo yêu cầu** | UC-SUP-01 tiền điều kiện; API 7.5 | Đơn của người gọi, `status ∈ {DELIVERED, PAID (COD đã đối soát), PARTIALLY_REFUNDED}`, `now ≤ delivered_at + 7 ngày` (`genie.refund.window=P7D`), không có yêu cầu mở (N3) → ngược lại `409 GNE-SUP-4092`. Đơn `COMPLETED` (đã xác nhận / tự hoàn tất) **không** tạo được (cửa sổ khiếu nại đã đóng — khớp C5 `return_window_closed`) | ☐ |
| D2 | **Đầu vào** (API 7.5 🔁) | UC-SUP-01 b2–5 | `{order_id, request_type ∈ REFUND_ONLY, RETURN_AND_REFUND, items:[{order_item_id, quantity}], reason_code ∈ DAMAGED_IN_TRANSIT, WRONG_ITEM, COUNTERFEIT, MISSING_PARTS, NOT_RECEIVED, note ≤ 500, evidence_keys:[…], refund_account?: {bank_code, account_number, account_name}}`; Idempotency-Key bắt buộc. Bằng chứng **≥ 1 ảnh + ≥ 1 video** (`422 GNE-SUP-4221`) — trừ `NOT_RECEIVED` (chỉ cần ghi chú). `refund_account` bắt buộc khi đơn VietQR / COD (D9) | ☐ |
| D3 | **Số tiền từng món** | UC-SUP-01 b7; N2 | `item_refund_amount = round_down((line_total − prorated_discount) × q / quantity_mua)`; phần ship: **không** hoàn phí ship trừ khi trả **toàn bộ** đơn với lý do lỗi shop / vận chuyển (`DAMAGED_IN_TRANSIT`, `WRONG_ITEM`, `COUNTERFEIT`, `MISSING_PARTS`, `NOT_RECEIVED`) → cộng `shipping_fee − shipping_discount_amount`; số lượng vượt phần còn lại (trừ phần đã hoàn trước) → `422 GNE-SUP-4222`. Ghi công thức vào ENTITY 8.2 | ☐ |
| D4 | **Bằng chứng** | UC-SUP-01 b5; N6 | `UploadPurpose.REFUND_EVIDENCE` (riêng tư, `image/jpeg|png|webp ≤ 10 MB`, `video/mp4|webm ≤ 100 MB`); `refund_evidences.submission_round` 1 (lúc tạo), 2 (bổ sung); shop gửi bằng chứng khi phân xử (UC-SUP-03 b2) → **thêm** `POST /seller/refund-requests/{id}/evidence` (round 1 của shop — cột `uploaded_by_role` mới: `CUSTOMER`, `VENDOR`) — bổ sung vào API 10.4 | ☐ |
| D5 | **Khi tạo** (UC-SUP-01 b7) | ENTITY 8.1 | Một transaction: `RefundRequest REQUESTED`, `shop_response_due_at = now + 48h`, items, evidences; `Order.requestReturn()` → `RETURN_REQUESTED`; `EscrowFacade.dispute(orderId)` → `DISPUTED` (ký quỹ `PENDING_FUNDING` của COD chưa đối soát → giữ trạng thái + cờ chặn hoàn tất — C5 job đã loại đơn có yêu cầu mở); thông báo shop "Yêu cầu trả hàng mới — hạn 48 giờ" | ☐ |
| D6 | **Shop xử lý** (API 10.4) | UC-SUP-02 b3 | `GET /seller/refund-requests?status&page&size` + chi tiết (bằng chứng URL ký); `POST …/request-evidence {message}` (một lần — `409 GNE-SUP-4091`; đồng hồ shop dừng, khách có 48 giờ); `POST …/approve {resolution: APPROVE_REFUND_ONLY | APPROVE_WITH_RETURN}` (`WITH_RETURN` chỉ khi khách chọn `RETURN_AND_REFUND`); `POST …/reject {reason}` → `SHOP_REJECTED`, `shop_rejected_at`; `POST …/escalate` (shop nhờ sàn) → `DISPUTED`; `POST …/confirm-return-received` (hàng về kho — khi webhook không về). Sai bước → `409 GNE-SUP-4093` | ☐ |
| D7 | **Vận đơn chiều về** | UC-SUP-02 phương án 2 | `approve(WITH_RETURN)` → `AWAITING_RETURN_SHIPMENT`, `return_due_at = now + 7 ngày`, `ShipmentFacade.createReturn(orderId, refundRequestId, carrier của đơn gốc)` (C4 — người gửi là khách, người nhận là kho shop, không COD); lỗi hãng → vẫn chuyển trạng thái, báo Staff tạo tay (không chặn quyết định của shop). `ReturnShipmentListener` (support, nghe `ShipmentStatusChangedEvent` hướng `RETURN`): `PICKED_UP` → `markReturnShipped()`; `DELIVERED` → `confirmReturnReceived()` → `APPROVED` → hoàn tiền (D10) | ☐ |
| D8 | **Hạn chót** (cron 15 phút) | UC-SUP-01, 02; API 12.2 | `RefundDeadlineJob`: `REQUESTED` quá `shop_response_due_at` → `autoApproveIfShopSilent()` (theo phương án khách chọn; `RETURN_AND_REFUND` → tạo vận đơn chiều về như D7); `AWAITING_EVIDENCE` quá `evidence_requested_at + 48h` → `closeIfEvidenceNotProvided()` → `REJECTED`; `SHOP_REJECTED` quá `shop_rejected_at + 48h` → `closeIfCustomerSilent()` → `REJECTED`; `AWAITING_RETURN_SHIPMENT` quá `return_due_at` chưa gửi → `failReturnIfOverdue()` → `RETURN_FAILED`. Nhánh đóng → đơn `RETURN_REJECTED → COMPLETED`, ký quỹ `releaseDisputeHold()` → `HELD` (rồi C3 giải ngân); thông báo hai bên | ☐ |
| D9 | **Tài khoản nhận hoàn & mã chuyển khoản** | UC-SUP-04 b1 (VietQR / COD) | `V32`: `refund_requests.refund_bank_snapshot_json` (ngân hàng, số TK, tên — **mã hóa** bằng `AesGcmCipher` như giấy tờ KYC, chỉ Staff có quyền xem bản rõ), `refund_code` (`TRRF{id}`, UNIQUE). Kiểm tên chủ TK không bắt buộc khớp tên khách (khách có thể nhờ người thân) — ghi UC | ☐ |
| D10 | **Thực hiện hoàn — VNPay** (UC-SUP-04 b1) | VNPay API refund | `RefundExecutionService.execute(refundRequestId)`: (tx 1) `PaymentRefund PENDING`; gọi `VnpayRefundClient` (`vnp_Command=refund`, `vnp_TransactionType` `02` toàn phần / `03` một phần, `vnp_TxnRef` gốc, `vnp_Amount`, `vnp_TransactionNo`, `vnp_TransactionDate`, `vnp_CreateBy`, `vnp_RequestId` duy nhất, chữ ký HMAC-SHA512 theo thứ tự trường của API refund — khác thứ tự lúc thanh toán; `PAYMENT_MODE=stub` → thành công giả lập); (tx 2) thành công → `markSuccess(gateway_refund_id)`, `PaymentTransaction.recordRefund(amount)`, D12; lỗi → `markFailed()`, AdminNotification, yêu cầu giữ `APPROVED`. Admin `POST /admin/refunds/{id}/sync` (hỏi `querydr`), `POST /admin/refunds/{id}/retry` 🔁 | ☐ |
| D11 | **Thực hiện hoàn — VietQR / COD** | UC-SUP-04 b1 | Yêu cầu `APPROVED` → `PaymentRefund` `PENDING` loại thủ công (`payment_transaction_id` = giao dịch gốc nếu VietQR, `NULL` nếu COD), hiện trong Admin "Chờ chuyển khoản thủ công" kèm VietQR tới TK khách, nội dung `TRRF{id}`; `RefundTransferMatcher` (đăng ký vào `BankTransferRouter` của C3): tiền **ra** có `TRRF{id}` + số tiền khớp → D12, `referenceCode` ghi `resolution_note`; lệch → `UNMATCHED` + cảnh báo; `POST /admin/refunds/{id}/mark-transferred {bank_reference?}` xác nhận tay (sau 24 giờ không khớp hoặc khi cần). COD chưa đối soát (`PENDING_FUNDING`) vẫn hoàn và **trừ vào lần đối soát COD kế tiếp** (ghi `COD_RECEIVABLE` — C4 đọc khi confirm) | ☐ |
| D12 | **Ghi nhận hoàn xong** (một transaction) | UC-SUP-04 b2, b5; N1, N7 | `RefundRequest.markRefunded()`; `Order.markRefunded(amount)` → `REFUNDED` (hoàn hết) / `PARTIALLY_REFUNDED`; ký quỹ: hoàn hết → `markRefunded()`; một phần → `applyPartialRefund(refundGross, refundCommission)` + `releaseDisputeHold()` → `HELD`; ký quỹ **đã giải ngân** (khiếu nại muộn được sàn chấp nhận) → `WalletPoster.debit(vendor, phần của shop, DEBIT_REFUND)` (ví **không được âm**: không đủ → chặn bước thu hồi, ghi AdminNotification để Staff thu hồi tay — ghi UC); hoàn tồn các món đã trả về (`RETURN_AND_REFUND` sau khi nhận hàng); hoàn Điểm theo phần phân bổ (`CoinFacade.refundForOrderItems` — cạnh `support → promotion`); sổ cái `REFUND` (Nợ `ESCROW_LIABILITY`/`VENDOR_PAYABLE` phần shop + Nợ `COMMISSION_REVENUE` phần hoa hồng đảo + Nợ `MARKETING_EXPENSE` phần sàn đã tài trợ / Có `GATEWAY_CLEARING` hoặc `BANK`); audit `REFUND_PROCESSED`; thông báo + email khách (C11) | ☐ |
| D13 | **Phong tỏa ký quỹ** (API 11.4) | API 11.4 `freeze` / `unfreeze` | `V32`: `escrow_ledgers.status_before_freeze`; `POST /admin/escrows/{id}/freeze {reason}` (bắt buộc lý do) → `FROZEN` (không giải ngân, không hoàn); đã `RELEASED` / `REFUNDED` → `409 GNE-PAY-4093`; `unfreeze` → về `status_before_freeze`. Audit **`ESCROW_FROZEN` / `ESCROW_UNFROZEN`** (mới). Quyền `HANDLE_DISPUTE` (`StaffPermissionEnum` đã có) | ☐ |
| D14 | **Phân xử** (API 11.4) | UC-SUP-03 b1–4 | `GET /admin/disputes?status=OPEN|RESOLVED&page&size` (OPEN = `DISPUTED`; kèm bằng chứng hai bên, lịch sử, vận đơn); `POST /admin/disputes/{id}/resolve {decision, note (bắt buộc), refund_amount?}` — `decision` ∈ `BUYER_REFUND_ONLY` (→ `approve(APPROVE_REFUND_ONLY)` → hoàn), `BUYER_WITH_RETURN` (→ `AWAITING_RETURN_SHIPMENT` + vận đơn về), `SELLER_WINS` (→ `reject()` → `REJECTED`, đơn → `COMPLETED`, ký quỹ `HELD`, cảnh báo khách), `CARRIER_FAULT` (hoàn khách 100 % do **sàn ứng** + ký quỹ giải phóng cho shop như bình thường; sổ cái Nợ `CARRIER_CLAIM_RECEIVABLE` (tài khoản mới — thêm vào `LedgerAccount` của C3) / Có `CUSTOMER_REFUND_PAYABLE`; sàn tự đòi hãng ngoài hệ thống). Lý do `COUNTERFEIT` + người mua thắng → trả `suggested_actions: [LOCK_PRODUCT]` để web gọi API khóa sản phẩm sẵn có (UC-CAT-11) — **không** thêm cạnh `support → catalog`. `POST …/confirm-return-received`, `POST …/reopen` (trong 24 giờ sau phán quyết nếu chưa hoàn tiền) → `DISPUTED`. Audit **`DISPUTE_RESOLVED`** (mới) | ☐ |
| D15 | **Khách sau khi shop từ chối** (API 7.7, 7.8) | UC-SUP-02 3a | `POST /me/refund-requests/{id}/escalate` (trong 48 giờ từ `shop_rejected_at`, hoặc khi `AWAITING_EVIDENCE` cho là vô lý) → `DISPUTED`; `POST …/accept-rejection` → `REJECTED` + đóng như D8 | ☐ |
| D16 | **Bổ sung bằng chứng** (API 7.6) | UC-SUP-01 luồng thay thế | `POST /me/refund-requests/{id}/evidence {evidence_keys}` chỉ khi `AWAITING_EVIDENCE` → `submitAdditionalEvidence()` → `REQUESTED`, `shop_response_due_at = now + 48h`, round 2 | ☐ |
| D17 | **Cài cổng `CancellationRefunds`** (C4) | UC-ORD-10 b4; UC-LOG-03 hoàn hàng | `SupportCancellationRefunds` (support): tạo `RefundRequest` hệ thống (`REFUND_ONLY`, `APPROVED`, `requested_by_user_id = customer`, lý do `ORDER_CANCELLED` / `DELIVERY_FAILED_RETURNED`, toàn bộ đơn kể cả ship) rồi `execute` (D10 / D11) — thay bản mặc định AdminNotification của C4 | ☐ |
| D18 | **Hoàn chuyển khoản muộn** (C3 D10 `LATE_PAYMENT_REFUND`) | C3 | Admin tạo lệnh hoàn tay từ dòng `bank_transactions` `UNMATCHED` (`POST /admin/bank-transactions/{id}/refund {account}`) → `PaymentRefund` thủ công + `TRRF` — **tùy chọn**, làm nếu kịp (bỏ thì đánh `[-]`, Staff xử lý ngoài hệ thống) | ☐ |
| D19 | **Danh sách của khách** | API 7.5 | `GET /me/refund-requests?page&size` + chi tiết `GET /me/refund-requests/{id}` (dòng thời gian, hạn chót, nút hợp lệ) — nếu API chưa có thì **thêm** vào 7.5 (màn "Trả hàng / Hoàn tiền" của Tài khoản đang dùng) | ☐ |
| D20 | **Thông báo** | UC-SUP-01 b8; UC-SUP-04 b3 | Shop: yêu cầu mới (hạn 48 giờ), khách bổ sung / khiếu nại, phán quyết; khách: shop yêu cầu bổ sung / đồng ý / từ chối (hạn 48 giờ khiếu nại), vận đơn chiều về, đã hoàn tiền (`REFUND_PROCESSED`); Staff: hoàn VNPay lỗi, chờ chuyển khoản | ☐ |
| D21 | **Cờ web** | — | `refunds` (khách: tạo, bổ sung, khiếu nại, chấp nhận, danh sách), `seller-refunds` (Seller › Trả hàng), `admin-disputes` (Admin › Trả hàng & Hoàn tiền: phân xử, hoàn tiền, phong tỏa) | ☐ |
| D22 | **Ngoài phạm vi** | — | Bồi thường từ hãng vận chuyển (ngoài hệ thống), gỡ nhãn Chính hãng tự động, thu hồi khi ví shop không đủ (Staff tay), MoMo / ZaloPay | ☐ |

---

## 3. Luồng nghiệp vụ chi tiết

```
Khách: POST /uploads/presigned-urls (REFUND_EVIDENCE) → POST /me/refund-requests 🔁 (D1–D5)
        → RefundStateSync: RR REQUESTED · Order RETURN_REQUESTED · Escrow DISPUTED → báo shop (48 giờ)
Shop:  request-evidence (D6) ─► khách POST …/evidence (D16) ─► REQUESTED
       approve REFUND_ONLY ─► APPROVED ─► RefundExecutionService (D10 / D11) ─► REFUNDED (D12)
       approve WITH_RETURN ─► AWAITING_RETURN_SHIPMENT + vận đơn RETURN (D7) ─► webhook DELIVERED ─► APPROVED ─► hoàn
       reject ─► SHOP_REJECTED ─► khách escalate (D15) ─► DISPUTED  |  accept-rejection / quá 48 giờ ─► REJECTED → COMPLETED
RefundDeadlineJob 15 phút (D8)
Admin: GET /admin/disputes → resolve (D14) ─► hoàn / trả hàng / bác / lỗi hãng
       refunds/{id}/sync · retry · mark-transferred ; escrows/{id}/freeze · unfreeze (D13)
SePay tiền ra TRRF{id} ─► RefundTransferMatcher ─► D12
Hủy đơn đã trả (C5) / GHN hoàn hàng (C4) ─► CancellationRefunds (D17) ─► RR hệ thống APPROVED ─► hoàn
```

---

## 4. Cây file sẽ tạo / sửa

```
server-spring/src/main/resources/db/migration/V32__c8_refund_dispute.sql               (mới — số tạm)
server-spring/src/main/java/com/genie/
├── common/error/ErrorCode.java          (sửa) SUP-4091, 4092, 4093, 4221, 4222
├── common/audit/AuditAction.java        (sửa) ESCROW_FROZEN, ESCROW_UNFROZEN, DISPUTE_RESOLVED
├── common/storage/UploadPurpose.java    (sửa) REFUND_EVIDENCE
├── support/refund/
│   ├── domain/       RefundRequest (sửa: refundCode, bankSnapshot), RefundEvidence (uploadedByRole), RefundReasonCode (mới)
│   ├── application/  RefundRequestService (khách) · SellerRefundService · RefundStateSync · RefundAmounts · RefundDeadlineJob
│   │                 ReturnShipmentListener · RefundExecutionService · RefundTransferMatcher · SupportCancellationRefunds
│   │                 RefundEvidenceViewer (URL ký) · RefundNotifications
│   └── api/          MeRefundController · SellerRefundController · AdminDisputeController · AdminRefundController
├── support/dispute/application/DisputeService.java · EscrowFreezeService.java
├── payment/vnpay/application/VnpayRefundClient.java · VnpayRefundSignature.java   (mới — C8 thêm vào module payment)
├── payment/ledger/domain/LedgerAccount.java (sửa) CARRIER_CLAIM_RECEIVABLE
└── promotion/coin/application/CoinFacade.java (sửa — refundForOrderItems)
server-spring/src/test/java/com/genie/support/…
    RefundAmountsTest · RefundStateSyncTest (bảng ENTITY 8.1) · VnpayRefundSignatureTest
    RefundRequestCreateIT · RefundEvidenceIT · SellerRefundIT · ReturnShipmentIT · RefundDeadlineJobIT
    VnpayRefundIT · ManualRefundTransferIT · PartialRefundConcurrencyIT · DisputeResolveIT · EscrowFreezeIT · CancellationRefundIT
web/src/features/refunds/… · seller-orders/infrastructure/refund.api.ts · admin-disputes/infrastructure/dispute.api.ts · error-codes.ts · i18n
docs/Document/… ENTITY (8.1–8.3, 6.2, 6.3, 6.7, Phụ lục A/E) · API_SPEC (7.5–7.8, 10.4, 11.4, 12.2, 15) · LUU_TRU · Usecase.md · CHECKLIST · BAO_CAO · xlsx
docs/api-tests/refunds.http · docs/kiem-thu/kich-ban-2026-11-28-c8.md
```

---

## 5. Bảng ánh xạ Use case → API → Dữ liệu

| Bước UC | Endpoint / tác vụ | Bảng | Quyền |
|---|---|---|---|
| UC-SUP-01 | `POST /me/refund-requests` 🔁, `POST …/{id}/evidence`, `…/escalate`, `…/accept-rejection`, `GET /me/refund-requests…` | `refund_requests`, `refund_request_items`, `refund_evidences`, `orders`, `escrow_ledgers` | chủ đơn |
| UC-SUP-02 | `GET /seller/refund-requests`, `POST …/request-evidence | approve | reject | confirm-return-received | escalate | evidence`, `RefundDeadlineJob`, webhook vận đơn RETURN | như trên + `shipment_trackings` | VENDOR |
| UC-SUP-03 | `GET /admin/disputes`, `POST …/resolve | confirm-return-received | reopen`, `POST /admin/escrows/{id}/freeze | unfreeze` | như trên + `system_logs` | `HANDLE_DISPUTE` |
| UC-SUP-04 | `RefundExecutionService`, `POST /admin/refunds/{id}/sync | retry | mark-transferred`, SePay tiền ra `TRRF` | `payment_refunds`, `payment_transactions`, `escrow_ledgers`, `vendor_wallets`, `wallet_transactions`, `financial_ledger`, `product_variants`, `coin_transactions` | nội bộ / Staff |

**Mã lỗi:** `GNE-SUP-4091` (đã yêu cầu bổ sung 1 lần), `4092` (quá hạn / đã có yêu cầu mở), `4221` (thiếu bằng chứng), `GNE-PAY-4093` (ký quỹ đã giải ngân / hoàn — không phong tỏa được); **mới:** `GNE-SUP-4093` (sai bước), `GNE-SUP-4222` (vượt phần đã mua / đã hoàn).

---

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — `ModuleBoundaryTest` (không module nào import `support`; `support` chỉ import các module ở README mục 3).
2. Swagger: summary `UC-SUP-01…04`.
3. `docs/api-tests/refunds.http`: tạo yêu cầu thiếu video → 4221; lần 2 khi đang mở → 4092; shop yêu cầu bổ sung 2 lần → 4091; hoàn một phần 2 lần vượt tổng → 4222.
4. Đụng `web/`: `npm run verify`.
5. Tick progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Uxx.yy` = task; `Uxx.Cn` = task commit.

### U00 — Chuẩn bị

- **[U00.01]** Chạy `git config user.name` / `user.email` — ghi vào dòng *Phụ trách* của progress; kiểm C3, C4, C5 đã merge (`EscrowFacade`, `ShipmentFacade.createReturn`, `CancellationRefunds`, cờ `can_request_return`).
- **[U00.02]** Chốt D1–D22 vào progress.
- **[U00.03]** Tạo nhánh `feature/c8-refund-requests` từ `develop`.
- **[U00.04]** Đọc bảng đồng bộ ENTITY 8.1 + ví dụ số (chép thành bảng ca test), CHECK `refund_requests.status`, `resolution_mode`, `payment_refunds.status`; quyền `HANDLE_DISPUTE` (CS Specialist) có trong preset nào; `AesGcmCipher` (KYC) để mã hóa TK nhận hoàn.
- **[U00.05]** Tài liệu API refund VNPay sandbox (thứ tự trường băm, `vnp_RequestId`, mã phản hồi) — ghi tóm tắt vào *Ghi chú*.
- **[U00.06]** Đọc màn mock: Chi tiết đơn → "Yêu cầu Trả hàng / Hoàn tiền" (chọn món, lý do, tải ảnh / video, TK nhận hoàn), Tài khoản › Trả hàng, Seller › Trả hàng (4 phương án + hạn), Admin › Trả hàng & Hoàn tiền (bằng chứng hai bên, phán quyết, trạng thái hoàn Gateway, phong tỏa).

### U01 — Tài liệu trước

- **[U01.01]** ENTITY 8.1 (bảng đồng bộ cập nhật D5, D8, D12, D14; cột D9), 8.2 (công thức D3), 8.3 (`uploaded_by_role` D4), 6.3 (`status_before_freeze` D13), 6.7 (`CARRIER_CLAIM_RECEIVABLE`), Phụ lục A (3 `AuditAction`, `RefundReasonCode`), Phụ lục E.
- **[U01.02]** API_SPEC — 7.5 (D1–D3, `refund_account`, danh sách D19), 7.6–7.8 (D15, D16), 10.4 (D6 + `evidence` của shop D4), 11.4 (D10, D11, D13, D14), 12.2, 15 (`GNE-SUP-4093`, `4222`), lịch sử phiên bản.
- **[U01.03]** Usecase.md — UC-SUP-01 (không hoàn ship trừ trả cả đơn — D3; tên TK không cần khớp — D9), UC-SUP-03 (CARRIER_FAULT sàn ứng — D14), UC-SUP-04 (ví shop không đủ khi khiếu nại muộn — D12).
- **[U01.C1]** COMMIT — `docs(support): specify return requests, seller handling, disputes and refund execution`

### U02 — Migration & đồng bộ trạng thái

- **[U02.01]** `V32__c8_refund_dispute.sql` (D9, D4 `uploaded_by_role`, D13, 3 `AuditAction`).
- **[U02.02]** `RefundAmounts` (D3, N2) — hàm thuần + `RefundStateSync` (N1) dùng `OrderLifecycleService` + `EscrowFacade` (thêm thân `dispute`, `releaseDisputeHold`, `refundFull`, `refundPartial` vào `EscrowFacade` của C3 — file của C3, ghi ở PR).
- **[U02.03]** `ErrorCode`, `AuditAction`, `UploadPurpose.REFUND_EVIDENCE`, `RefundReasonCode`.
- **[U02.04]** Test — `RefundAmountsTest` (chia lẻ, đã hoàn một phần trước, có / không ship), `RefundStateSyncTest` (mọi dòng bảng ENTITY 8.1 — trạng thái RR × Order × Escrow sau mỗi chuyển).
- **[U02.C1]** COMMIT — `feat(db): add refund account snapshot, refund codes and escrow freeze memory`
- **[U02.C2]** COMMIT — `feat(support): keep refund, order and escrow states in sync`

### U03 — Khách tạo yêu cầu (UC-SUP-01)

- **[U03.01]** `RefundRequestService.create` (D1–D5, D9) + `MeRefundController` `POST /me/refund-requests` 🔁, `GET` danh sách / chi tiết (D19).
- **[U03.02]** `…/evidence` (D16), `…/escalate`, `…/accept-rejection` (D15).
- **[U03.03]** `RefundEvidenceViewer` (N6) — URL ký 10 phút theo quyền xem.
- **[U03.04]** Test — `RefundRequestCreateIT` (đơn `DELIVERED` 3 ngày → 201, đơn `RETURN_REQUESTED`, ký quỹ `DISPUTED`; 8 ngày → 4092; `COMPLETED` → 4092; thiếu video → 4221; `NOT_RECEIVED` không cần ảnh; số lượng vượt → 4222; yêu cầu thứ 2 khi đang mở → 4092; cùng Idempotency-Key → cùng kết quả; COD chưa đối soát → ký quỹ giữ `PENDING_FUNDING`), `RefundEvidenceIT` (URL ký cho khách / shop đúng, shop khác → 404).
- **[U03.C1]** COMMIT — `feat(support): let buyers open return and refund requests with evidence`

### U04 — Shop xử lý & hạn chót (UC-SUP-02)

- **[U04.01]** `SellerRefundService` + `SellerRefundController` (D6, D4 bằng chứng của shop).
- **[U04.02]** Vận đơn chiều về + `ReturnShipmentListener` (D7).
- **[U04.03]** `RefundDeadlineJob` (D8, N5).
- **[U04.04]** Thông báo (D20).
- **[U04.05]** Test — `SellerRefundIT` (4 phương án; `WITH_RETURN` cho `REFUND_ONLY` → 409; bổ sung lần 2 → 4091; shop khác → 404), `ReturnShipmentIT` (webhook RETURN `PICKED_UP` → `return_shipped_at`; `DELIVERED` → `APPROVED` → gọi hoàn), `RefundDeadlineJobIT` (4 nhánh hạn chót; mỗi nhánh đúng trạng thái đơn / ký quỹ theo bảng; chạy 2 lần song song không xử lý trùng).
- **[U04.C1]** COMMIT — `feat(support): let sellers handle return requests with deadlines and return shipments`

### U05 — Thực hiện hoàn tiền (UC-SUP-04) — nhánh `feature/c8-refund-execution`

- **[U05.01]** `VnpayRefundSignature` + `VnpayRefundClient` (D10; `stub` / `http`).
- **[U05.02]** `RefundExecutionService.execute` (N4, D10, D11) + D12 trong một transaction (tồn, Điểm, sổ cái, ví nếu đã giải ngân, audit).
- **[U05.03]** `RefundTransferMatcher` (D11) đăng ký vào `BankTransferRouter`; `AdminRefundController` (`sync`, `retry` 🔁, `mark-transferred`).
- **[U05.04]** `SupportCancellationRefunds` (D17) thay bản mặc định của C4.
- **[U05.05]** Test — `VnpayRefundSignatureTest` (vector mẫu), `VnpayRefundIT` (MockWebServer: toàn phần → `REFUNDED`, ký quỹ `REFUNDED`, sổ cái cân; một phần → `PARTIALLY_REFUNDED`, ký quỹ `HELD` với `net` giảm đúng; lỗi → `FAILED` + ký quỹ không giải ngân; retry → thành công), `ManualRefundTransferIT` (SePay tiền ra `TRRF` đúng số → hoàn xong; lệch → `UNMATCHED`; xác nhận tay), **`PartialRefundConcurrencyIT`** (2 yêu cầu hoàn một phần liên tiếp + gửi lại cùng lúc → tổng hoàn ≤ đã trả — TS 14), `CancellationRefundIT` (hủy đơn VNPay đã trả → RR hệ thống → hoàn tự động; GHN hoàn hàng online → hoàn).
- **[U05.C1]** COMMIT — `feat(payment): refund vnpay transactions through the refund api`
- **[U05.C2]** COMMIT — `feat(support): execute approved refunds and confirm manual transfers through sepay`

### U06 — Sàn phân xử (UC-SUP-03) — nhánh `feature/c8-disputes`

- **[U06.01]** `DisputeService` + `AdminDisputeController` (D14) — danh sách, chi tiết (bằng chứng hai bên, vận đơn, lịch sử), phán quyết 4 loại, nhận hàng hoàn, mở lại; audit `DISPUTE_RESOLVED`.
- **[U06.02]** `EscrowFreezeService` (D13) — `POST /admin/escrows/{id}/freeze | unfreeze`, audit.
- **[U06.03]** Test — `DisputeResolveIT` (khách khiếu nại sau khi shop từ chối → `DISPUTED`; `BUYER_REFUND_ONLY` → hoàn; `SELLER_WINS` → đơn `COMPLETED`, ký quỹ `HELD`; `CARRIER_FAULT` → khách được hoàn + shop vẫn được giải ngân + bút toán `CARRIER_CLAIM_RECEIVABLE`; mở lại sau khi đã hoàn → 409; thiếu quyền → 403), `EscrowFreezeIT` (phong tỏa → job giải ngân bỏ qua; gỡ → về trạng thái cũ; đã giải ngân → 4093).
- **[U06.C1]** COMMIT — `feat(support): let staff arbitrate disputes and freeze escrows`

### U07 — Nối web phía khách (cờ `refunds`)

- **[U07.01]** `refund.api.ts` (5 hàm + danh sách / chi tiết) — form chọn món + số lượng (web hiện số tiền **ước tính**, số cuối lấy từ phản hồi của server — `RefundAmounts`), lý do, tải ảnh / video (presigned riêng tư), TK nhận hoàn khi VietQR / COD (`listBanks` đã có — B4), bổ sung bằng chứng, khiếu nại, chấp nhận; nút ở Chi tiết đơn theo `actions.can_request_return` (C5).
- **[U07.C1]** COMMIT — `feat(web): connect buyer return and refund requests to the real api`

### U08 — Nối web Seller › Trả hàng (cờ `seller-refunds`)

- **[U08.01]** `seller-orders/refund.api.ts` (`listSellerRefunds`, `actOnRefund` + bằng chứng của shop) — 4 phương án, đồng hồ 48 giờ, xem ảnh / video, lỗi `4091` / `4093`.
- **[U08.C1]** COMMIT — `feat(seller-ui): connect seller return handling to the real api`

### U09 — Nối web Admin › Trả hàng & Hoàn tiền (cờ `admin-disputes`)

- **[U09.01]** `admin-disputes/dispute.api.ts` (9 hàm) — hàng chờ, bằng chứng hai bên, phán quyết (ghi chú bắt buộc), "Trạng thái hoàn tiền Gateway" (đồng bộ / thử lại), "Chờ chuyển khoản thủ công" (VietQR tới TK khách, nội dung `TRRF…`, "Đã chuyển khoản"), phong tỏa / gỡ.
- **[U09.02]** vi / en + `error-codes.ts` cho U07–U09; test mapper; kiểm 9 theme, 360 px; `npm run verify`.
- **[U09.C1]** COMMIT — `feat(admin-ui): connect dispute arbitration and refund operations to the real api`

### U10 — Kiểm thử & chạy thật

- **[U10.01]** `docs/api-tests/refunds.http` — mọi luồng mục 3 + webhook RETURN + SePay `TRRF`.
- **[U10.02]** `docs/kiem-thu/kich-ban-2026-11-28-c8.md` — TC-C8-01…: hàng vỡ → shop đồng ý hoàn ngay (VNPay sandbox hoàn thật nếu có khóa); trả hàng có vận đơn về; shop yêu cầu bổ sung → khách nộp; shop im lặng 48 giờ (chỉnh giờ) → tự đồng ý; shop từ chối → khách khiếu nại → sàn xử 4 kiểu; hoàn một phần rồi trả tiếp món khác; COD hoàn tay qua VietQR + webhook SePay; phong tỏa ký quỹ; 9 theme; 360 px.
- **[U10.03]** Chạy thật — web build `PUBLIC_REAL_API=…,orders,refunds,seller-refunds,admin-disputes` + backend `local` + Postgres + Redis + Silo + Mailpit; Chromium (Playwright) với khách, chủ shop, CS Specialist; mỗi lỗi một commit `fix(…)`.
- **[U10.C1]** COMMIT — `docs(support): add refund api tests and real-run scenarios`

### U11 — Hoàn thiện use case & tick tiến độ

- **[U11.01]** OpenAPI — summary `UC-SUP-01…04`.
- **[U11.02]** `docs/web-backend-handoff.md` — 3 cờ đã nối.
- **[U11.03]** Usecase.md — *Hiện trạng* 4 UC; UC-ORD-10 (hoàn tiền tự động đã có — cập nhật câu của C5).
- **[U11.04]** `CHECKLIST_TIEN_DO_UC.md` — tick 5 lớp 4 UC (+ đổi ô `◐` của UC-ORD-10 nếu C5 để lại), Trạng thái, **cột Phụ trách = tên ở U00.01**, số mục 1 + phân hệ 09, dòng mục 5 *Nhật ký*.
- **[U11.05]** `BAO_CAO_UC_DOC_LAP.md` — mục C8 + dòng *Phụ trách*.
- **[U11.06]** `Quản Lý UseCase.xlsx` — 4 UC, cột AA.
- **[U11.07]** README ngày (mục 7) — tick dòng C8; mục 3 thêm cạnh `support → promotion`.
- **[U11.C1]** COMMIT — `docs(support): record returns, disputes and refunds as implemented`
- **[U11.08]** Mở 3 PR xếp chồng vào `develop` (`c8-refund-requests` → `c8-refund-execution` → `c8-disputes`).

---

## 8. Ngoài phạm vi

- Đòi bồi thường hãng vận chuyển, gỡ nhãn Chính hãng tự động, thu hồi tay khi ví shop không đủ, hoàn chuyển khoản muộn (D18 tùy chọn).

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Ba máy trạng thái lệch nhau | Một lớp `RefundStateSync` + test theo đúng bảng ENTITY 8.1 |
| Hoàn vượt số đã trả khi hoàn nhiều lần / gửi lại | `RefundAmounts` trừ phần đã hoàn; khóa dòng đơn + giao dịch; `PartialRefundConcurrencyIT` |
| VNPay hoàn lỗi nhưng shop đã được giải ngân | Ký quỹ giữ `DISPUTED` tới khi `REFUNDED` (N4); job giải ngân bỏ qua |
| Lộ bằng chứng / TK ngân hàng khách | Bucket riêng tư + URL ký; TK mã hóa AES-GCM, chỉ Staff có quyền xem |
| Cron và thao tác tay cùng lúc trên một yêu cầu | `SKIP LOCKED` + khóa dòng ở thao tác tay; kiểm trạng thái trong entity |
