# PLAN — Chuỗi C5 Sau giao hàng (UC-ORD-09 · UC-ORD-10 · UC-ORD-11 · UC-ORD-12)

| | |
|---|---|
| **Ngày lập** | 2026-11-28 |
| **Thể loại** | server-spring (module `order` feature `order` (đơn của khách), `checkout` (hủy nhóm đơn); cổng mới cài ở `payment`) + nối `web/` (feature `orders`, `checkout`) |
| **Use case** | Khách hàng theo dõi chi tiết hành trình đơn hàng (UC-ORD-09) · Khách hàng yêu cầu hủy đơn hàng (UC-ORD-10) · Khách hàng xác nhận "Đã nhận hàng thành công" (UC-ORD-11) · Hệ thống tự động hoàn tất đơn sau 7 ngày từ khi Delivered (UC-ORD-12) |
| **Tài liệu chuẩn** | UC phân hệ 03 (UC-ORD-09 dòng 1163, 10 dòng 1185, 11 dòng 1218, 12 dòng 1244) · API 6.13 (hủy nhóm đơn), 7.1–7.4, 12.2 (hoàn tất 1 giờ), 15 (`GNE-ORD-4092`, `4095`) · ENTITY 5.1, 5.2 (máy trạng thái), 5.5, 6.3, 7.2 · Document_Vi 3.4.2 — xem [README](../README.md) |
| **Theo dõi tiến độ** | [progress_sau_giao_hang_c5.md](progress_sau_giao_hang_c5.md) — cùng mã task `Dxx.yy` |
| **Phụ trách** | Người làm plan — lấy theo **`git config user.name`** trên máy thực hiện (ghi vào progress ở D00.01); không ghi tên cố định |
| **Người commit** | danh tính git đã cấu hình trên máy đó — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Phụ thuộc** | **C3** (`EscrowFacade.releaseNow`, ký quỹ `HELD` / `PENDING_FUNDING`) · **C4** (`OrderLifecycleService`, `CancellationRefunds`, vận đơn + `shipment_events`, cột V29 `confirmed_at / cancelled_at / ready_for_pickup_at`, đơn tới `DELIVERED`, đối soát COD đưa đơn về `PAID`) |
| **Mục tiêu** | 4 use case ⬜ → ✅. Khách xem đơn + hành trình, hủy đúng luật, bấm "Đã nhận hàng" (giải ngân ngay khi ký quỹ `HELD`), đơn tự hoàn tất sau 7 ngày; **C7 (đánh giá) và C8 (trả hàng) bắt đầu được** |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `1b66eae`, 2026-11-28)

| Phần | Đã có | Còn thiếu |
|---|---|---|
| **Order** | `Order.cancel`, `markCustomerConfirmed`, `complete`, `markPaid`; cột `customer_confirmed_at`, `completed_at`, `delivered_at`, `is_reviewed`; `OrderController`, `OrderService`, `AutoCompleteOrdersJob`, `OrderLifecycleListener` là **file rỗng**; `OrderCompletedEvent`, `OrderStatusChangedEvent` (`order.event`); `OpenOrders.TERMINAL` | API 7.1–7.4, 6.13; job hoàn tất; cổng giải ngân (D5) |
| **Sau C4** | `OrderLifecycleService` (chuyển trạng thái + audit + event), `CancellationRefunds` (bản mặc định = AdminNotification), `CheckoutCompensation.onOrderCancelled` (no-op tới C6), `shipment_trackings` / `shipment_events`, cột `confirmed_at`, `cancelled_at`, `ready_for_pickup_at`, `cancel_reason`, `cancelled_by` | — |
| **Sau C3** | `EscrowFacade.statusOf / releaseNow / scheduleRelease`, `ExpiredSessionSweeper` (C2) | — |
| **Mã lỗi** | — | `GNE-ORD-4092` (đã qua bước hủy), `GNE-ORD-4095` (chưa giao — chưa xác nhận được) — API 15; dùng lại `GNE-ORD-4041` (C4) |
| **Web** | `orders/order.api.ts` (`listOrders`, `getOrder`, `cancelOrder`, `confirmReceipt`) — `USE_MOCK`; màn Đơn mua (tab), Chi tiết đơn (dòng thời gian, vận đơn, nút Hủy / Đã nhận / Trả hàng / Đánh giá / Thanh toán ngay / Huỷ nhóm đơn) | Nối thật theo cờ `orders` |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Mọi chuyển trạng thái qua `OrderLifecycleService` (C4); thêm phương thức `cancelByCustomer`, `confirmReceipt`, `autoComplete` | README mục 4 |
| N2 | **`order` không import `payment` / `support`.** Giải ngân → cổng `OrderEscrowGateway` (order khai, payment cài); "có yêu cầu trả hàng đang mở" → **read-model SQL** trên `refund_requests` (tiền lệ read-model 2026-11-17 D6) | README mục 3 |
| N3 | Kiểm sở hữu: đơn phải có `customer_id` = người gọi → không thì `404 GNE-ORD-4041` | TS 9 |
| N4 | Giải ngân **chỉ** khi ký quỹ `HELD` (không bao giờ `PENDING_FUNDING`, `DISPUTED`, `FROZEN`) | UC-ORD-11, 12 ràng buộc |
| N5 | Hoàn tồn đúng mô hình giữ chỗ: chưa trả → nhả `reserved_count`; đã `commitSale` → cộng `stock_count` (qua `VariantFacade`) | UC-ORD-10 b3 |
| N6 | Cron hoàn tất `FOR UPDATE SKIP LOCKED LIMIT 200`, mỗi đơn một transaction | LLD 10 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Đường dẫn** | UC ghi `/customer/orders/{id}`, `/confirm-received` | Theo API: `GET /me/orders`, `GET /me/orders/{id}`, `POST /me/orders/{id}/cancel`, `POST /me/orders/{id}/confirm-receipt`, `POST /me/checkout-sessions/{id}/cancel`. Sửa câu UC-ORD-09 b2, UC-ORD-11 b2 | ☐ |
| D2 | **Danh sách đơn của khách** (API 7.1) | API 7.1; màn Đơn mua | Tab: `ALL`, `TO_PAY` (`PENDING_PAYMENT` online), `TO_SHIP` (COD `PENDING_PAYMENT`, `PAID`, `CONFIRMED`, `PREPARING`, `READY_FOR_PICKUP`), `SHIPPING` (`DELIVERING`, `DELIVERY_FAILED`), `TO_RECEIVE` (`DELIVERED`, COD `PAID` chưa xác nhận), `COMPLETED`, `CANCELLED` (`CANCELLED*`, `AUTO_CANCELLED`), `RETURNS` (`RETURN_*`, `RETURNED_TO_VENDOR`, `REFUNDED`, `PARTIALLY_REFUNDED`); tìm theo mã đơn / tên sản phẩm / tên shop; phân trang theo API 7.1; `summary.counts` theo tab; mỗi thẻ: shop (`shop_id` = `vendor_profiles.id` — read-model), 1–3 dòng hàng đầu + "và N sản phẩm", tổng tiền, cờ hành động (D3) | ☐ |
| D3 | **Cờ hành động** trên đơn (`actions`) — web không tự suy luật | API 7.2 | Server trả `can_cancel`, `can_confirm_receipt`, `can_request_return` (trong 7 ngày từ `delivered_at`, chưa có yêu cầu mở — C8 dùng), `can_review` (đơn `COMPLETED` + còn dòng chưa đánh giá — C7 dùng), `can_pay` (online `PENDING_PAYMENT`, phiên chưa hết hạn), `return_window_closed`, `return_deadline_at`. Một hàm thuần `OrderActions.of(order, now, hasOpenRefund, reviewedItems)` + test bảng ca | ☐ |
| D4 | **Dòng thời gian hành trình** (UC-ORD-09 b3–4) | UC-ORD-09; ENTITY 7.2 | Ghép 2 nguồn, sắp theo thời gian: (a) mốc của đơn — `created_at` (Đặt hàng), `payment_confirmed_at` (Đã thanh toán), `confirmed_at` (Shop xác nhận), `ready_for_pickup_at` (Đã giao cho ĐVVC), `delivered_at`, `customer_confirmed_at`, `completed_at`, `cancelled_at`; (b) `shipment_events` của vận đơn `OUTBOUND` (`event_status` chuẩn hóa + `location` + `description`). Thứ tự chuẩn Online / COD như UC-ORD-09 b3 để vẽ thanh tiến trình (`progress.steps[]`, bước hiện tại). Kèm `shipment {carrier, tracking_code, status, estimated_delivery_at}` | ☐ |
| D5 | **Cổng giải ngân** | N2; UC-ORD-11 b4 | Order khai `OrderEscrowGateway { EscrowState stateOf(orderId); void releaseNow(orderId); }`; payment cài bằng `EscrowFacade` (gọi **trong** transaction của order — `releaseNow` dùng `propagation = MANDATORY`). Không có bean (test module order) → bản mặc định ném lỗi rõ ràng | ☐ |
| D6 | **Xác nhận đã nhận hàng** (UC-ORD-11) | UC-ORD-11 b3–4 | Từ `DELIVERED` hoặc COD `PAID` (đã đối soát) + không có yêu cầu trả hàng mở; `markCustomerConfirmed(now)` (idempotent). Ký quỹ `HELD` → `Order.complete()` + `releaseNow` cùng transaction → `200 {status: COMPLETED, escrow_released: true}`; `PENDING_FUNDING` → chỉ ghi `customer_confirmed_at`, trả `{status: DELIVERED, escrow_released: false, message: "Đơn COD đang chờ đối soát…"}`; trạng thái khác → `409 GNE-ORD-4095`. Phát `OrderCompletedEvent` (mở nút đánh giá — C7) | ☐ |
| D7 | **Hoàn tất ngay sau đối soát COD** (UC-LOG-03 b4) | C4 D17 | `CodRemittedCompletionListener` (order, đồng bộ `OrderPaidEvent` nguồn COD): nếu `customer_confirmed_at ≠ null` **hoặc** `delivered_at <= now − 7 ngày` và không có trả hàng mở → `complete()` + `releaseNow` | ☐ |
| D8 | **Tự hoàn tất 7 ngày** (UC-ORD-12) | UC-ORD-12 b1–4; API 12.2 (1 giờ) | `AutoCompleteOrdersJob` `@Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")`: đúng câu SQL của UC (thêm điều kiện `customer_confirmed_at IS NULL` không cần — đơn đã xác nhận đã `COMPLETED`), ngưỡng `genie.order.auto-complete-after=P7D`; mỗi đơn: `complete()` + `releaseNow` (UC-PAY-07 "giải ngân ngay trong lượt chạy"), audit `ORDER_STATUS_CHANGED` lý do "Tự động hoàn tất do hết hạn 7 ngày" | ☐ |
| D9 | **Hủy đơn của khách** (UC-ORD-10) | UC-ORD-10 b2–6 | `POST /me/orders/{id}/cancel {reason_code, note}` — `reason_code` ∈ `CHANGE_MIND`, `CHANGE_ADDRESS`, `FOUND_CHEAPER`, `OTHER`. Cho phép: `PAID`, `CONFIRMED`, `PREPARING` **chưa có vận đơn**, COD `PENDING_PAYMENT`; online `PENDING_PAYMENT` → D10. Khác → `409 GNE-ORD-4092` + `details.hint = "RETURN_AFTER_DELIVERY"`. Trong một transaction: `Order.cancel()` (`CANCELLED`, `cancel_reason`, `cancelled_by = CUSTOMER`, `cancelled_at`), hoàn tồn (N5), COD → ký quỹ `PENDING_FUNDING` đóng (cùng cổng payment như C4 D12), online đã trả → `CancellationRefunds.refund(orderId, reason)` (C8), `CheckoutCompensation.onOrderCancelled(orderId)` (C6 — hoàn voucher shop / Điểm theo phần phân bổ; voucher sàn chỉ khi **mọi** đơn con của phiên đã hủy — luật trong C6), thông báo shop `ORDER_STATUS_CHANGED` "Khách đã hủy đơn …" | ☐ |
| D10 | **Đơn online chưa trả tiền** — hủy một đơn con làm lệch số tiền phiên | UC-ORD-10 luồng thay thế; API 6.13 | Online `PENDING_PAYMENT`: **chỉ hủy cả nhóm** (`POST /me/checkout-sessions/{id}/cancel`); gọi hủy một đơn con trong phiên nhiều đơn → `409 GNE-ORD-4092` `details.hint = "CANCEL_SESSION"`; phiên một đơn → server tự chuyển sang hủy nhóm. Hủy nhóm: mọi đơn con `PENDING_PAYMENT` → `CANCELLED`, nhả giữ chỗ, giao dịch `PENDING` → `FAILED` (`reason = CANCELLED_BY_CUSTOMER`), phiên → trạng thái hủy (kiểm giá trị `SessionStatus` ở D00.04 — không có `CANCELLED` thì dùng `EXPIRED` + ghi chú, và đề xuất thêm giá trị ở migration C3/C4 còn mở) | ☐ |
| D11 | **Hủy một phần nhiều shop** (UC-ORD-10 ràng buộc) | UC-ORD-10 | Đơn vị hủy = đơn con; đơn con khác giữ nguyên vòng đời; không bỏ từng món trong đơn con (API không nhận `item_ids`) | ☐ |
| D12 | **Thông báo** | UC-ORD-10 b6; UC-ORD-11 b5 | Shop: "Khách hủy đơn …" (lý do). Khách: "Đơn … đã hoàn tất" (khi tự hoàn tất — kèm "Đánh giá ngay"); giải ngân báo shop là việc của C3 (`WALLET_CREDITED`) | ☐ |
| D13 | **Cờ web** | — | `orders` (Đơn mua, Chi tiết đơn, Hủy, Đã nhận hàng, Huỷ nhóm đơn). Nút "Trả hàng" (C8) và "Đánh giá" (C7) chỉ đọc cờ `actions` — gọi API thì thuộc plan đó | ☐ |
| D14 | **Ngoài phạm vi** | — | Hoàn tiền thật (C8 cài `CancellationRefunds`), hoàn voucher / Điểm (C6 cài `onOrderCancelled`), yêu cầu trả hàng (C8), đánh giá (C7) | ☐ |

---

## 3. Luồng nghiệp vụ chi tiết

```
GET  /me/orders?tab&q&page&size        → BuyerOrderQuery (read-model: đơn + shop + 3 dòng đầu + actions D3) + summary.counts
GET  /me/orders/{id}                    → chi tiết + timeline (D4) + shipment + actions + tiền (tạm tính, ship, giảm, tổng, đã hoàn)
POST /me/orders/{id}/cancel             → OrderLifecycleService.cancelByCustomer (D9 / D10) → hoàn tồn → ký quỹ COD đóng /
                                          CancellationRefunds (online) → CheckoutCompensation.onOrderCancelled → AFTER_COMMIT báo shop
POST /me/checkout-sessions/{id}/cancel  → CheckoutService.cancelSession (D10)
POST /me/orders/{id}/confirm-receipt    → OrderLifecycleService.confirmReceipt (D6) → [HELD] complete + OrderEscrowGateway.releaseNow
OrderPaidEvent (COD đối soát) ─► CodRemittedCompletionListener (D7)
AutoCompleteOrdersJob (mỗi giờ)         → complete + releaseNow (D8)
```

---

## 4. Cây file sẽ tạo / sửa

```
server-spring/src/main/java/com/genie/
├── common/error/ErrorCode.java                  (sửa) ORD-4092, ORD-4095 (nếu C4 chưa thêm ORD-4092)
├── order/order/
│   ├── application/  BuyerOrderQuery · OrderActions · OrderTimeline · OrderEscrowGateway (cổng) · AutoCompleteOrdersJob (hiện thực)
│   │                 CodRemittedCompletionListener · OpenRefundLookup (read-model refund_requests) · OrderLifecycleService (sửa — 3 phương thức)
│   └── api/          OrderController (hiện thực) · BuyerOrderResponses · CancelOrderRequest
├── order/checkout/application/CheckoutService.java (sửa — cancelSession) · api/CheckoutSessionController (sửa — POST /{id}/cancel)
└── payment/escrow/application/OrderEscrowGatewayImpl.java (mới — cài cổng D5)
server-spring/src/test/java/com/genie/order/…
    OrderActionsTest · OrderTimelineTest · BuyerOrderIT · BuyerCancelOrderIT · CancelSessionIT · ConfirmReceiptIT
    AutoCompleteOrdersJobIT · CodRemittedCompletionIT
web/src/features/orders/infrastructure/order.api.ts · checkout/infrastructure/checkout.api.ts (cancelSession) · error-codes.ts · i18n
docs/Document/… API_SPEC (6.13, 7.1–7.4, 12.2) · Usecase.md · ENTITY 5.2 (luật hủy) · CHECKLIST · BAO_CAO · xlsx
docs/api-tests/buyer-orders.http · docs/kiem-thu/kich-ban-2026-11-28-c5.md
```

---

## 5. Bảng ánh xạ Use case → API → Dữ liệu

| Bước UC | Endpoint / tác vụ | Bảng | Quyền |
|---|---|---|---|
| UC-ORD-09 | `GET /me/orders`, `GET /me/orders/{id}` | `orders`, `order_items`, `shipment_trackings`, `shipment_events`, `vendor_profiles` (đọc) | CUSTOMER / VENDOR mua hàng (chủ đơn) |
| UC-ORD-10 | `POST /me/orders/{id}/cancel`, `POST /me/checkout-sessions/{id}/cancel` | `orders`, `stock_reservations`, `product_variants`, `checkout_sessions`, `payment_transactions`, `escrow_ledgers`, `system_logs` | chủ đơn |
| UC-ORD-11 | `POST /me/orders/{id}/confirm-receipt` | `orders`, `escrow_ledgers`, `vendor_wallets`, `wallet_transactions`, `financial_ledger` | chủ đơn |
| UC-ORD-12 | `AutoCompleteOrdersJob` mỗi giờ, `CodRemittedCompletionListener` | như trên + `refund_requests` (đọc) | nội bộ |

**Mã lỗi:** `GNE-ORD-4092` (409 đã qua bước được hủy — `details.hint`), `GNE-ORD-4095` (409 chưa giao / chưa xác nhận được), `GNE-ORD-4041` (404). Dùng lại `GNE-VAL-4220`, `GNE-ORD-4031`.

---

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — `ModuleBoundaryTest` (`order` không import `payment` / `support`).
2. Swagger: summary `UC-ORD-09/10/11`.
3. `docs/api-tests/buyer-orders.http`: khách A xem đơn khách B → `404`; hủy đơn `DELIVERING` → `409` `hint`; xác nhận đơn `PREPARING` → `4095`.
4. Đụng `web/`: `npm run verify`.
5. Tick progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Dxx.yy` = task; `Dxx.Cn` = task commit.

### D00 — Chuẩn bị

- **[D00.01]** Chạy `git config user.name` / `user.email` — ghi vào dòng *Phụ trách* của progress; kiểm C3 + C4 đã merge (`EscrowFacade`, `OrderLifecycleService`, cột V29).
- **[D00.02]** Chốt D1–D14 vào progress.
- **[D00.03]** Tạo nhánh `feature/c5-buyer-orders` từ `develop`.
- **[D00.04]** Đọc CHECK `checkout_sessions.status` (D10), `orders.status`; đọc `ExpiredSessionSweeper` (C2) để hủy nhóm dùng lại phần nhả giữ chỗ.
- **[D00.05]** Đọc màn mock Đơn mua (8 tab, thẻ đơn), Chi tiết đơn (thanh tiến trình, dòng thời gian, khối vận đơn, nút hành động, hộp hủy với lý do), "Huỷ nhóm đơn" ở trang kết quả thanh toán; `grep` `listOrders`, `getOrder`, `cancelOrder`, `confirmReceipt`.

### D01 — Tài liệu trước

- **[D01.01]** API_SPEC — 7.1 (tab D2, `summary.counts`), 7.2 (`actions` D3, `timeline` + `progress` D4, `shipment`), 7.3 (`reason_code`, luật D9–D10, `details.hint`), 7.4 (D6 hai nhánh), 6.13 (D10), 12.2 (ngưỡng 7 ngày cấu hình được), lịch sử phiên bản.
- **[D01.02]** Usecase.md — UC-ORD-09 b2, UC-ORD-11 b2 (đường dẫn — D1); UC-ORD-10 (online chưa trả → hủy nhóm — D10); ENTITY 5.2 ghi luật hủy theo người hủy.
- **[D01.C1]** COMMIT — `docs(order): specify buyer order tracking, cancellation and receipt confirmation`

### D02 — Đơn của khách & hành trình (UC-ORD-09)

- **[D02.01]** `OrderActions` (D3) + `OrderTimeline` (D4) — hàm thuần.
- **[D02.02]** `BuyerOrderQuery` — read-model danh sách + chi tiết; `OpenRefundLookup` (read-model `refund_requests` mở — N2).
- **[D02.03]** `OrderController` — `GET /me/orders`, `GET /me/orders/{id}`; `@Operation(summary = "UC-ORD-09 …")`; `BuyerOnlyFilter` đã phủ `/me/orders`? (kiểm — chưa thì thêm như B5).
- **[D02.04]** Test — `OrderActionsTest` (bảng ca mọi trạng thái × online / COD × trước / sau 7 ngày × có / không trả hàng mở), `OrderTimelineTest` (đơn online đủ mốc, COD, đơn hủy), `BuyerOrderIT` (tab đếm đúng; tìm theo tên sản phẩm; đơn của người khác → 404; chi tiết có `shipment_events` sắp đúng giờ; nhân viên → `403 GNE-ORD-4031`).
- **[D02.C1]** COMMIT — `feat(order): list buyer orders with tracking timeline and allowed actions`

### D03 — Hủy đơn (UC-ORD-10) — nhánh `feature/c5-cancel-complete`

- **[D03.01]** `OrderLifecycleService.cancelByCustomer` (D9, D11) — hoàn tồn (N5), ký quỹ COD đóng, `CancellationRefunds`, `onOrderCancelled`.
- **[D03.02]** `CheckoutService.cancelSession` (D10) + `POST /me/checkout-sessions/{id}/cancel`.
- **[D03.03]** `POST /me/orders/{id}/cancel` + thông báo shop (D12).
- **[D03.04]** Test — `BuyerCancelOrderIT` (COD `PENDING_PAYMENT` → nhả giữ chỗ; online `PAID` → `stock_count` cộng lại + `CancellationRefunds` được gọi đúng một lần; `PREPARING` có vận đơn → 409 `RETURN_AFTER_DELIVERY`; online chưa trả trong phiên 2 đơn → 409 `CANCEL_SESSION`; hủy 2 lần → lần 2 `409`; đơn con thứ hai của phiên không đổi), `CancelSessionIT` (mọi đơn con `CANCELLED`, giữ chỗ `RELEASED`, giao dịch `FAILED`, phiên hết hiệu lực; phiên đã trả → 409).
- **[D03.C1]** COMMIT — `feat(order): let buyers cancel orders and unpaid checkout sessions`

### D04 — Xác nhận đã nhận hàng (UC-ORD-11)

- **[D04.01]** Cổng `OrderEscrowGateway` (order) + `OrderEscrowGatewayImpl` (payment) (D5).
- **[D04.02]** `OrderLifecycleService.confirmReceipt` (D6) + `POST /me/orders/{id}/confirm-receipt`; phát `OrderCompletedEvent`.
- **[D04.03]** Test — `ConfirmReceiptIT` (VNPay `DELIVERED` + `HELD` → `COMPLETED`, ví shop +net, `ESCROW_LIABILITY` của đơn về 0; COD chưa đối soát → `DELIVERED` giữ nguyên + `customer_confirmed_at`, ví không đổi; có trả hàng mở → 409; bấm 2 lần → 200 không cộng ví lần 2; đơn `PREPARING` → `4095`).
- **[D04.C1]** COMMIT — `feat(order): confirm receipt and release escrow when funds are held`

### D05 — Tự hoàn tất (UC-ORD-12)

- **[D05.01]** `AutoCompleteOrdersJob` (D8, N6) + cấu hình `genie.order.auto-complete-after`.
- **[D05.02]** `CodRemittedCompletionListener` (D7).
- **[D05.03]** Test — `AutoCompleteOrdersJobIT` (đơn giao 8 ngày + `HELD` → `COMPLETED` + ví; 6 ngày → giữ; có trả hàng `REQUESTED` → giữ; COD `DELIVERED` + `PENDING_FUNDING` → không chọn; hai lần chạy song song → mỗi đơn hoàn tất một lần), `CodRemittedCompletionIT` (khách đã xác nhận trước → đối soát xong hoàn tất ngay; chưa xác nhận, giao 3 ngày → chờ job).
- **[D05.C1]** COMMIT — `feat(order): auto-complete delivered orders after the 7-day return window`

### D06 — Nối web (cờ `orders`)

- **[D06.01]** `order.api.ts` — 4 hàm theo `isMocked('orders')` + `checkout.api.ts` `cancelSession`; DTO / mapper API 7.1–7.4 (tab, `actions`, `timeline`, `progress`, `shipment`).
- **[D06.02]** Màn Đơn mua (8 tab + đếm, tìm), Chi tiết đơn (thanh tiến trình theo `progress`, dòng thời gian, khối vận đơn + mã chép được), hộp Hủy (lý do), "Đã nhận được hàng" (hai thông điệp theo D6), "Huỷ nhóm đơn"; nút hiện / ẩn **chỉ** theo `actions`; lỗi `4092` đọc `hint` để hướng dẫn (trả hàng / hủy nhóm). Kiểm 9 theme, 360 px.
- **[D06.03]** vi / en + `error-codes.ts`; test mapper (`order.mapper.test.ts`); `npm run verify`.
- **[D06.C1]** COMMIT — `feat(web): connect buyer orders, cancellation and receipt confirmation to the real api`

### D07 — Kiểm thử & chạy thật

- **[D07.01]** `docs/api-tests/buyer-orders.http` — xem đơn, hủy (4 trường hợp), hủy nhóm, xác nhận (HELD / COD), chạy job bằng endpoint dev (`@Profile("local")`) hoặc chỉnh `delivered_at` bằng SQL (ghi rõ).
- **[D07.02]** `docs/kiem-thu/kich-ban-2026-11-28-c5.md` — TC-C5-01…: theo dõi đơn VNPay từ đặt → giao (dùng webhook C4), đơn COD; hủy khi `PAID` / `PREPARING` / sau bàn giao (bị chặn); hủy nhóm đơn chưa trả; đã nhận hàng → ví shop tăng (kiểm ở Seller › Ví sau C9, hoặc SQL); COD xác nhận trước đối soát; tự hoàn tất (chỉnh giờ); 9 theme; 360 px.
- **[D07.03]** Chạy thật — web build `PUBLIC_REAL_API=…,payment,seller-orders,orders` + backend `local` + Postgres + Redis + Silo + Mailpit; Chromium (Playwright) với khách + chủ shop; mỗi lỗi một commit `fix(…)`.
- **[D07.C1]** COMMIT — `docs(order): add buyer order api tests and real-run scenarios`

### D08 — Hoàn thiện use case & tick tiến độ

- **[D08.01]** OpenAPI — summary `UC-ORD-09/10/11` (UC-ORD-12 là job — ghi ở mô tả).
- **[D08.02]** `docs/web-backend-handoff.md` — nhóm `orders` đã nối.
- **[D08.03]** Usecase.md — *Hiện trạng* 4 UC.
- **[D08.04]** `CHECKLIST_TIEN_DO_UC.md` — tick 5 lớp 4 UC (UC-ORD-10 ô *Nối/Chạy* `◐` nếu hoàn tiền thật chờ C8), Trạng thái, **cột Phụ trách = tên ở D00.01**, số mục 1 + phân hệ 03, dòng mục 5 *Nhật ký*.
- **[D08.05]** `BAO_CAO_UC_DOC_LAP.md` — mục C5; C7 / C8 ghi "đơn `COMPLETED` / cờ `can_review` / `can_request_return` đã có".
- **[D08.06]** `Quản Lý UseCase.xlsx` — 4 UC, cột AA.
- **[D08.07]** README ngày (mục 7) — tick dòng C5.
- **[D08.C1]** COMMIT — `docs(order): record buyer order tracking, cancellation and completion as implemented`
- **[D08.08]** Mở 2 PR xếp chồng vào `develop` (`c5-buyer-orders` → `c5-cancel-complete`).

---

## 8. Ngoài phạm vi

- Hoàn tiền đơn đã trả bị hủy (C8 cài `CancellationRefunds`); hoàn voucher / Điểm (C6 cài `onOrderCancelled`).
- Yêu cầu trả hàng (C8), đánh giá (C7) — plan này chỉ trả cờ `can_request_return`, `can_review`.

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Giải ngân đơn COD chưa có tiền thật | Chỉ khi ký quỹ `HELD` (N4); IT riêng |
| Khách bấm "Đã nhận" cùng lúc job hoàn tất chạy | Khóa dòng đơn `FOR UPDATE` trong cả hai đường + `releaseNow` kiểm `HELD`; IT song song |
| Hủy đơn con làm lệch tiền phiên online chưa trả | D10 chỉ cho hủy nhóm |
| Cờ `actions` lệch luật web | Web chỉ đọc cờ; luật ở một hàm thuần có test |
| `order` cần biết trả hàng đang mở mà không import `support` | Read-model SQL (N2) |
