# PLAN — Chuỗi C4 Vận chuyển (UC-LOG-16 · UC-LOG-01 · UC-LOG-02 · UC-LOG-15 · UC-LOG-03)

| | |
|---|---|
| **Ngày lập** | 2026-11-28 |
| **Thể loại** | server-spring (module `order` feature `fulfillment` (mới), module `shipping` feature `shipment`, `carrier` (mới), `cod`; module `payment` feature `cod`) + nối `web/` (feature `seller-orders`, `admin-finance` phần đối soát COD) |
| **Use case** | Người bán xác nhận / từ chối đơn hàng mới (UC-LOG-16) · Tiếp nhận đơn hàng & Đóng gói tại kho shop (UC-LOG-01) · Đẩy vận đơn sang GHN & In phiếu gửi hàng PDF (UC-LOG-02) · Điều phối đa hãng vận chuyển (UC-LOG-15) · Đồng bộ hành trình giao nhận qua Webhook GHN (UC-LOG-03, gồm luồng phụ đối soát COD và hoàn hàng) |
| **Tài liệu chuẩn** | UC phân hệ 06 (UC-LOG-01 dòng 1753, 02 dòng 1776, 03 dòng 1801, 15 dòng 1832, 16 dòng 1858) · API 10.3 (đơn của shop), 11.7 (đối soát COD), 12.1 (`/shipping/{carrier}/webhook/{secret}`), 12.2 (tự xác nhận 24 giờ, đồng bộ 3PL 30 phút), 15 (`GNE-ORD-*`, `GNE-LOG-*`) · ENTITY 5.2 `Order` (máy trạng thái), 5.5 `StockReservation`, 6.3 `EscrowLedger`, 6.8 `CodRemittance`, 7.1 `ShipmentTracking`, 7.2 `ShipmentEvent`, 7.3 `VendorShippingConfig` · Document_Vi 3.4.2 (19 trạng thái đơn) — xem [README](../README.md) |
| **Theo dõi tiến độ** | [progress_van_chuyen_c4.md](progress_van_chuyen_c4.md) — cùng mã task `Hxx.yy` |
| **Phụ trách** | Người làm plan — lấy theo **`git config user.name`** trên máy thực hiện (ghi vào progress ở H00.01); không ghi tên cố định |
| **Người commit** | danh tính git đã cấu hình trên máy đó — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Phụ thuộc** | **C3** (đơn `PAID`, `EscrowFacade`, `LedgerPoster`, sổ ngân hàng `bank_transactions` + `BankTransferMatcher`). UC-VND-05 (kho lấy hàng GHN — đã có), UC-VND-21 (`vendor_shipping_config` — đã có), `shipping/fee` (C2: `ShippingFeeQuoter`, `HttpGhnFeeClient`) |
| **Mục tiêu** | 5 use case ⬜ → ✅. Shop xác nhận / từ chối / chuẩn bị / bàn giao đơn, có vận đơn GHN (sandbox) + phiếu PDF A6; webhook đưa đơn tới `DELIVERED` (đặt hạn giải ngân 7 ngày); Staff đối soát COD đưa ký quỹ COD về `HELD` |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `1b66eae`, 2026-11-28)

| Phần | Đã có | Còn thiếu |
|---|---|---|
| **Order** | `Order` có `confirmByVendor`, `startPreparing`, `readyForPickup`, `startDelivering`, `markDelivered`, `markDeliveryFailed`, `retryDelivery`, `startReturning`, `markReturnedToVendor`, `cancel`, `markPaid`; cột `ghn_order_code`, `delivered_at`, `shipping_expected_at`, `buyer_note`; `SellerOrderController` / `OrderService` / `OrderFacade` là **file rỗng (TODO)**; `OpenOrders` (đếm đơn mở) | API 10.3; cột `confirmed_at`, `auto_confirmed` (chỉ số "sàn tự xác nhận" — UC-VND-08), `cancel_reason`, `cancelled_by`, `cancelled_at`, `ready_for_pickup_at` (C5 dựng dòng thời gian từ các mốc này); job tự xác nhận 24 giờ |
| **Shipping** | `ShipmentTracking` (`direction`, `refund_request_id`, `tracking_code`, `carrier_type`, `carrier_order_id`, `status`, `estimated_delivery_at`, `actual_delivered_at`; `applyCarrierStatus`, `forReturn`, `isFinal`), `ShipmentEvent` (`isDuplicateOf`), `VendorShippingConfig` (`supports`, `isFreeshipFor`), `CodRemittance` (`remittance_code`, `received_amount`, `bank_reference`, `status`, `lines_json`; `confirm`); `shipping/fee` (`ShippingFeeQuoter`, `FlatFeeTable`, `GhnFeeClient` + `HttpGhnFeeClient` qua `/v2/shipping-order/preview`, `GHN_MODE`); kho lấy hàng `PickupShopRegistrar` (`GHN_PICKUP_MODE`) | Tạo vận đơn GHN, adapter đa hãng, webhook, phiếu PDF, đối soát COD; UNIQUE chống trùng sự kiện hãng |
| **Phụ thuộc module** | `order → shipping` (đã có), `shipping → vendor` | **`shipping` không được import `order`** (vòng) — webhook cập nhật đơn bằng sự kiện đồng bộ (README mục 3) |
| **Mã lỗi** | — | `GNE-ORD-4041`, `GNE-ORD-4092`, `GNE-LOG-4221`, `GNE-LOG-5021` (API 15) + mới `GNE-LOG-4091` (đơn đã có vận đơn), `GNE-LOG-4092` (shop không còn hãng nào bật / không hãng nào phục vụ tuyến) |
| **Web** | `seller-orders/fulfillment.api.ts` (`getSellerOrder`, `confirmOrder`, `rejectOrder`, `prepareOrder`, `getShippingQuotes`, `createShipment`, `getShippingLabel`), `seller-orders/order.api.ts` (`listSellerOrders`), `admin-finance/finance.api.ts` (`listCodRemittances`, `getCodRemittance`, `previewReconciliation`, `confirmReconciliation`) — tất cả `USE_MOCK` | Nối thật theo cờ `seller-orders`, `cod-remittances` |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Mọi chuyển trạng thái đơn đi qua **`OrderLifecycleService`** (order, plan này tạo — C5 / C8 dùng lại) gọi phương thức của `Order`; ghi `system_logs` `ORDER_STATUS_CHANGED` + phát `OrderStatusChangedEvent` (`order.event`, đã có); không `UPDATE orders SET status` bằng SQL tay | README mục 4; ENTITY 5.2 |
| N2 | **`shipping` không import `order`.** `order → shipping` (`ShipmentFacade.create`, `quotes`, `label`); webhook hãng → `ShipmentStatusChangedEvent` (`shipping.event`) → `OrderShipmentListener` (order) chạy **đồng bộ cùng transaction** | README mục 3 |
| N3 | Kiểm sở hữu ở service: đơn phải có `vendor_id = users.id` của shop gọi → không thì `404 GNE-ORD-4041` (không lộ đơn shop khác) | TS 9 |
| N4 | Gọi hãng (tạo vận đơn, tính phí) **ngoài** transaction CSDL dài: tính phí song song timeout 3 giây; tạo vận đơn xong mới mở transaction ghi `shipment_trackings` + đổi đơn (lỗi hãng → không đổi gì — UC-LOG-02 2a) | TS 10 |
| N5 | Webhook hãng: xác thực `secret` trên URL (so sánh hằng thời gian) + (tùy chọn) IP allowlist; chống trùng sự kiện bằng UNIQUE `(shipment_id, carrier_event_id)`; trạng thái chỉ **tiến** theo thứ tự hợp lệ (sự kiện cũ tới muộn không kéo lùi) | API 12.1 |
| N6 | Mỗi hãng một **adapter** (`CarrierGateway`), chế độ `stub` mặc định, `http` thật chỉ GHN trong TLCN; trạng thái mọi hãng chuẩn hóa về `ShipmentTrackingStatus` | UC-LOG-15 b5 |
| N7 | Tiền COD: chỉ ký quỹ `PENDING_FUNDING` → `HELD` qua `EscrowFacade` (C3); bút toán qua `LedgerPoster`; mỗi dòng bảng kê khớp đúng một đơn | README mục 4 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Đơn nào shop được xác nhận** | UC-LOG-16 tiền điều kiện | Online: `PAID`; COD: `PENDING_PAYMENT` + `payment_method = COD`. `confirmByVendor()` → `CONFIRMED`, ghi `confirmed_at`; gọi 2 lần → `200` (idempotent). Trạng thái khác → `409 GNE-ORD-4092` | ☐ |
| D2 | **Từ chối đơn** (UC-LOG-16 3a) | UC-ORD-10 b2 (shop hủy được từ `PENDING_PAYMENT`) | `POST /seller/orders/{id}/reject {reason_code, note}` chỉ khi `PAID` / `PENDING_PAYMENT` (COD) / `CONFIRMED` / `PREPARING` chưa có vận đơn → `CANCELLED_BY_VENDOR`, `cancel_reason`, `cancelled_by = VENDOR`, `cancelled_at`; hoàn tồn: COD `PENDING_PAYMENT` → nhả giữ chỗ (`StockReservation.release`), đã `commitSale` → cộng `stock_count` (qua `VariantFacade`); online đã trả → cổng **`CancellationRefunds.refund(orderId, reason)`** (order khai — C8 cài thật; **bản mặc định** của plan này: AdminNotification "Đơn … cần hoàn tiền" + log, ký quỹ giữ nguyên `HELD`); voucher / Điểm → `CheckoutCompensation` thêm phương thức `default onOrderCancelled(orderId)` (C6 cài). Mã lý do cố định: `OUT_OF_STOCK`, `WRONG_PRICE`, `CANNOT_DELIVER_AREA`, `OTHER` | ☐ |
| D3 | **Tự xác nhận sau 24 giờ** (UC-LOG-16 3b) | API 12.2 (15 phút) | `AutoConfirmOrdersJob` 15 phút: đơn `PAID` / COD `PENDING_PAYMENT` có `created_at`(COD) hoặc `payment_confirmed_at`(online) `<= now − 24h` → `confirmByVendor()`, `auto_confirmed = true` (đếm ở UC-VND-08), `SKIP LOCKED` lô 100; thông báo shop "Sàn đã tự xác nhận đơn …" | ☐ |
| D4 | **Chuẩn bị hàng** (UC-LOG-01) | UC-LOG-01 b4–5 | `POST /seller/orders/{id}/prepare` từ `CONFIRMED` → `PREPARING`; `shipping_expected_at = now + 24h` (SLA). Danh sách 10.3: tab `ALL, NEW (PAID + COD chờ), TO_PREPARE (CONFIRMED + PREPARING), SHIPPING (READY_FOR_PICKUP + DELIVERING), DELIVERED, RETURNS (RETURN_* + RETURNED_TO_VENDOR), CANCELLED`, tìm theo mã đơn / tên khách (bỏ dấu), sắp mới / cũ, `page / size`; `summary.counts` theo tab | ☐ |
| D5 | **Hãng hỗ trợ trong TLCN** | UC-LOG-15; `CarrierType` | Adapter: `GHN` (`stub` \| `http` — tạo đơn `/v2/shipping-order/create`, chi tiết `/v2/shipping-order/detail`, phí đã có), `GHTK`, `VIETTEL_POST`, `JNT_EXPRESS` (**chỉ `stub`** — bảng phí cố định theo vùng + mã vận đơn giả `GHTK-…`), `GENIE_EXPRESS` (stub, nội thành ≤ 15 km & ≤ 30 kg — bỏ qua khi thiếu tọa độ). Biến `CARRIER_MODE=stub|http` (http chỉ đổi GHN; hãng khác luôn stub) — ghi rõ "TLCN tích hợp thật GHN" ở UC-LOG-15 | ☐ |
| D6 | **Chọn hãng** (UC-LOG-15 b2–3) | UC-LOG-15 | `GET /seller/orders/{id}/shipping-quotes` gọi song song các hãng shop bật (`CompletableFuture` + timeout 3 giây, lỗi → bỏ hãng đó); `recommended` = phí thấp nhất trong các hãng có ETA ≤ ETA nhanh nhất + 1 ngày. Shop được chọn hãng khác trong danh sách (`POST …/shipments {carrier}`); không gửi `carrier` → dùng `recommended`; mọi hãng lỗi → GHN với phí đã tính ở checkout (UC-LOG-15 ngoại lệ) | ☐ |
| D7 | **Chênh lệch phí ship** | UC-LOG-15 ràng buộc | Phí khách trả giữ nguyên (`orders.shipping_fee`); `shipment_trackings.shipping_fee_actual` lưu phí thật; khi giải ngân (C3) chênh lệch âm ghi `FREESHIP_COST`, dương ghi doanh thu ship — **plan này chỉ lưu số**, C3 `EscrowReleaseJob` đọc (thêm task E07 vào C3 nếu chưa có → ghi chú ở H11) | ☐ |
| D8 | **Tạo vận đơn** (UC-LOG-02) | API 10.3 `POST /shipments` 🔁 | Chỉ từ `PREPARING`; Idempotency-Key bắt buộc; đã có vận đơn `OUTBOUND` → `409 GNE-LOG-4091`; payload GHN: địa chỉ người nhận từ `shipping_address_snapshot_json` (2 cấp + mã phường v3 — như phí), cân nặng / kích thước tổng từ biến thể (thiếu → mặc định 500 g, 20×15×10), `cod_amount = grand_total` khi COD, `required_note = CHOXEMHANGKHONGTHU`, `items` (tên, SL, **không** giá). Thành công → `ShipmentTracking` (`OUTBOUND`, `AWAITING_PICKUP`), `orders.ghn_order_code`, `readyForPickup()` → `READY_FOR_PICKUP`. Lỗi / quá hạn → `502 GNE-LOG-5021`, đơn giữ `PREPARING` | ☐ |
| D9 | **Phiếu gửi hàng PDF** | UC-LOG-02 b5 (A6 100×150 mm, Code128) | Tự sinh bằng **Apache PDFBox** + **ZXing** (Code128) — thêm 2 dependency (`pom.xml`, ghi lý do ở PR); nội dung: người gửi (tên shop, SĐT, kho), người nhận (tên, SĐT che giữa, địa chỉ), mã vận đơn + mã vạch, mã đơn `TR-…`, danh sách hàng **không giá**, COD (nếu có) số tiền thu. Sinh mỗi lần gọi (không lưu Silo); `GET …/shipping-label` trả `application/pdf`; `?format=html` cho trình duyệt in trực tiếp (tùy chọn). Font Unicode tiếng Việt nhúng (Noto Sans — thêm vào `resources/fonts/`, kiểm giấy phép OFL) | ☐ |
| D10 | **Bảng chuẩn hóa trạng thái** (UC-LOG-03 b7) | UC-LOG-03; ENTITY 7.1 | GHN: `ready_to_pick, picking` → `AWAITING_PICKUP`; `picked, storing, transporting, sorting` → `PICKED_UP` / `IN_TRANSIT`; `delivering, money_collect_delivering` → `OUT_FOR_DELIVERY`; `delivered` → `DELIVERED`; `delivery_fail` → `FAILED`; `waiting_to_return, return, return_transporting, returning` → `RETURNING`; `returned` → `RETURNED`; `lost, damage` → `LOST`; `cancel` → giữ + cảnh báo. Ảnh hưởng đơn: `PICKED_UP` → đơn `DELIVERING` (`startDelivering`); `DELIVERED` → `markDelivered(at)`; `FAILED` → `markDeliveryFailed`; giao lại → `retryDelivery`; `RETURNING` → `startReturning`; `RETURNED` → `markReturnedToVendor` + hoàn tồn + D12 | ☐ |
| D11 | **Webhook** — một đường cho mọi hãng | API 12.1 | `POST /api/v1/shipping/{carrier}/webhook/{secret}`; `secret` theo hãng (`GHN_WEBHOOK_SECRET`…); sai → `401`; hãng không hỗ trợ → `404`; luôn `200` khi đã ghi nhận (kể cả trùng / không tìm thấy vận đơn — log WARN). Payload GHN: `OrderCode`, `Status`, `Time`, `Warehouse`, `Reason` — `carrier_event_id = OrderCode + Status + Time` | ☐ |
| D12 | **Hoàn hàng về shop** (UC-LOG-03 luồng phụ) | UC-LOG-03 | `RETURNED_TO_VENDOR`: cộng lại `stock_count`; online → `CancellationRefunds.refund(orderId, "DELIVERY_FAILED_RETURNED")`; COD → payment đóng ký quỹ `PENDING_FUNDING` (listener `OrderStatusChangedEvent` trong payment → `EscrowLedger.markRefunded()` với số 0 — trạng thái `REFUNDED`, ghi chú "COD không thu được") + bút toán phí ship 2 chiều `FREESHIP_COST` (sàn chịu) | ☐ |
| D13 | **Đặt hạn giải ngân khi giao xong** | UC-LOG-03 b5; UC-PAY-05 b4 | payment lắng nghe `OrderStatusChangedEvent(DELIVERED)` (đồng bộ) → `EscrowFacade.scheduleRelease(orderId, delivered_at + 7 ngày)` (ký quỹ `HELD` / `PENDING_FUNDING` đều đặt, giải ngân chỉ khi `HELD` + đơn `COMPLETED` — C5) | ☐ |
| D14 | **Đồng bộ khi lỡ webhook** | API 12.2 (30 phút) | `ShipmentSyncJob` 30 phút: vận đơn chưa `isFinal()` không có sự kiện > 6 giờ → hỏi `CarrierGateway.detail(trackingCode)` → đi cùng đường xử lý webhook (cùng chống trùng) | ☐ |
| D15 | **Phiên đối soát COD** (11.7) | UC-LOG-03 luồng phụ b1 | `CodRemittanceMatcher` (đăng ký vào `BankTransferRouter` của C3): tiền **vào** từ số tài khoản / nội dung có mẫu của hãng (`GHN`, cấu hình `genie.cod.carrier-patterns`) → tạo `cod_remittances` `WAITING` (`received_amount`, `bank_reference`, `carrier_type`); không khớp mẫu → để `UNMATCHED` như C3 | ☐ |
| D16 | **Đối chiếu bảng kê** | UC-LOG-03 b2–3; API 11.7 | `POST /admin/cod-remittances/{id}/reconciliation-preview` (multipart CSV: `tracking_code, cod_amount, fee`) → mỗi dòng `MATCHED` (vận đơn có, đơn COD `DELIVERED`, `cod_amount = grand_total`), `AMOUNT_MISMATCH`, `NOT_FOUND`, `ALREADY_RECONCILED`; tổng `Σ cod − Σ fee` phải = `received_amount` → lệch `422 GNE-LOG-4221` (vẫn trả bảng để xem). `POST …/confirm` 🔁: chỉ dòng `MATCHED` → `EscrowFacade.fundCod(orderId, amount, now)` (`HELD`), `OrderPaymentFacade.markCodRemitted(orderId)` (đơn `DELIVERED` → `PAID`, `funded_at`), bút toán (Nợ `BANK` thực nhận + Nợ `GATEWAY_FEE_EXPENSE` phí thu hộ / Có `ESCROW_LIABILITY` + `SHIPPING_PAYABLE`), `CodRemittance.confirm(staffId, lines)`, audit `COD_RECONCILED` (mới). Quyền `PROCESS_PAYOUT` (Finance Officer — `StaffPermissionEnum` đã có) | ☐ |
| D17 | **Hoàn tất sau đối soát** | UC-LOG-03 b4 | Plan này chỉ đưa đơn về `PAID` + ký quỹ `HELD`; việc "khách đã xác nhận / quá 7 ngày → hoàn tất ngay" thuộc **C5** (listener `OrderPaidEvent` COD — C5 D7) — ghi chú để không làm hai lần | ☐ |
| D18 | **Thông báo** | UC-LOG-16 b3; UC-LOG-03 b6 | Khách: "Shop đã xác nhận", "Đã giao cho ĐVVC (mã …)", "Đang giao", "Giao thành công — vui lòng xác nhận", "Giao thất bại — giữ liên lạc", "Shop từ chối đơn (lý do)"; shop: "Sàn tự xác nhận đơn". Loại `ORDER_STATUS_CHANGED` (đã có), `AFTER_COMMIT` | ☐ |
| D19 | **Cờ web** | — | `seller-orders` (Seller › Đơn hàng: danh sách, chi tiết, xác nhận / từ chối / chuẩn bị / báo giá / bàn giao / in phiếu), `cod-remittances` (Admin › Đối soát COD). Phần "theo dõi đơn" phía khách là C5 | ☐ |
| D20 | **Ngoài phạm vi** | — | Khách hủy / nhận hàng / theo dõi (C5), vận đơn chiều về của trả hàng (C8 — dùng `ShipmentFacade.createReturn` plan này để sẵn chữ ký), tích hợp thật GHTK / ViettelPost / J&T (DATN), bảo hiểm hàng hóa | ☐ |

---

## 3. Luồng nghiệp vụ chi tiết

### 3.1 Shop xử lý đơn (UC-LOG-16, LOG-01)

```
GET /seller/orders?tab&q&sort&page&size  → danh sách + summary.counts (D4)   GET /seller/orders/{id} → chi tiết + buyer_note + shipment
POST /seller/orders/{id}/confirm   → OrderLifecycleService.confirm (D1) → ORDER_STATUS_CHANGED → thông báo khách (AFTER_COMMIT)
POST /seller/orders/{id}/reject    → OrderLifecycleService.rejectByVendor (D2) → hoàn tồn → CancellationRefunds / CheckoutCompensation
POST /seller/orders/{id}/prepare   → PREPARING, shipping_expected_at = now + 24h
AutoConfirmOrdersJob (15 phút)     → confirm + auto_confirmed = true (D3)
```

### 3.2 Bàn giao hãng (UC-LOG-15, LOG-02)

```
GET  /seller/orders/{id}/shipping-quotes → ShipmentFacade.quotes(order parcel) → CarrierRouter (song song, 3 s, D6) → quotes[] + recommended
POST /seller/orders/{id}/shipments {carrier?} 🔁 → (ngoài tx) CarrierGateway.createOrder(...) → (tx) ShipmentTracking OUTBOUND
                                            + orders.ghn_order_code + readyForPickup() → READY_FOR_PICKUP
GET  /seller/orders/{id}/shipping-label → WaybillPdfRenderer (PDFBox + ZXing Code128, A6) → application/pdf
```

### 3.3 Hành trình & đối soát (UC-LOG-03)

```
POST /shipping/{carrier}/webhook/{secret} ─► CarrierWebhookService: secret (N5) → parse theo adapter → ShipmentEvent (UNIQUE) →
   ShipmentTracking.applyCarrierStatus (chỉ tiến) → publish ShipmentStatusChangedEvent (đồng bộ)
   └─► OrderShipmentListener (order): đổi đơn theo D10 → OrderStatusChangedEvent ─► payment: DELIVERED → scheduleRelease (D13)
                                                                                  └► RETURNED_TO_VENDOR → D12
ShipmentSyncJob (30 phút) → CarrierGateway.detail → cùng đường trên (D14)
SePay tiền vào từ hãng ─► CodRemittanceMatcher → cod_remittances WAITING (D15)
Admin › Đối soát COD: preview CSV (D16) → confirm → ký quỹ HELD + đơn PAID + bút toán + COD_RECONCILED
```

---

## 4. Cây file sẽ tạo / sửa

```
server-spring/src/main/resources/db/migration/V29__c4_fulfillment_shipping.sql      (mới — số tạm)
server-spring/src/main/resources/fonts/NotoSans-Regular.ttf, NotoSans-Bold.ttf      (mới — OFL)
server-spring/pom.xml                                                               (sửa — pdfbox, zxing core)
server-spring/src/main/java/com/genie/
├── common/error/ErrorCode.java          (sửa) ORD-4041, 4092; LOG-4091, 4092, 4221, 5021
├── common/audit/AuditAction.java        (sửa) COD_RECONCILED (+ category FINANCE)
├── order/
│   ├── order/application/  OrderLifecycleService (mới, N1) · CancellationRefunds (cổng, mới) · OrderPorts (bản mặc định)
│   │                       AutoConfirmOrdersJob (mới) · OrderShipmentListener (mới) · SellerOrderQuery (mới — read-model danh sách)
│   ├── checkout/application/CheckoutCompensation.java (sửa — default onOrderCancelled)
│   │                        OrderPaymentFacade.java (sửa — markCodRemitted)
│   └── order/api/          SellerOrderController (hiện thực) · SellerOrderRequests / Responses
├── shipping/
│   ├── carrier/  application (CarrierGateway, CarrierRouter, CarrierQuote, CarrierMode) · infra (GhnCarrierGateway — http,
│   │             StubCarrierGateway — GHTK / VIETTEL_POST / JNT_EXPRESS / GENIE_EXPRESS / GHN stub)
│   ├── shipment/ application (ShipmentFacade, ShipmentService, CarrierWebhookService, ShipmentSyncJob, WaybillPdfRenderer,
│   │             CarrierStatusMapper) · api (CarrierWebhookController) · event (ShipmentStatusChangedEvent)
│   └── cod/      domain (CodRemittance sửa nếu cần)
└── payment/cod/  application (CodRemittanceMatcher, CodReconciliationService, CodStatementParser) · api (AdminCodRemittanceController)
                  · EscrowOrderStatusListener (DELIVERED → scheduleRelease, RETURNED_TO_VENDOR COD → đóng ký quỹ)
server-spring/src/test/java/com/genie/…
    OrderLifecycleTest · CarrierStatusMapperTest · CarrierRouterTest · WaybillPdfRendererTest · CodStatementParserTest
    SellerOrderIT · SellerOrderRejectIT · AutoConfirmOrdersJobIT · ShipmentCreateIT · GhnCarrierGatewayTest (MockWebServer)
    CarrierWebhookIT · ShipmentSyncJobIT · CodReconciliationIT · ShippingModuleBoundaryTest (ArchUnit: shipping ↛ order)
web/src/features/seller-orders/infrastructure/{fulfillment,order}.api.ts · admin-finance/infrastructure/finance.api.ts (phần COD)
web/src/shared/api/error-codes.ts · web/src/i18n/{vi,en}.json
docs/Document/… ENTITY (5.2, 7.1, 7.2, 6.8, Phụ lục A/E) · API_SPEC (10.3, 11.7, 12.1, 12.2, 15) · Usecase.md · CHECKLIST · BAO_CAO · xlsx
docs/api-tests/shipping.http · docs/kiem-thu/kich-ban-2026-11-28-c4.md · docs/cau-hinh-moi-truong.md · .env.example · deploy/.env.example
```

---

## 5. Bảng ánh xạ Use case → API → Dữ liệu

| Bước UC | Endpoint / tác vụ | Bảng | Quyền |
|---|---|---|---|
| UC-LOG-16 b1–4, 3a | `GET /seller/orders`, `GET /seller/orders/{id}`, `POST …/confirm`, `POST …/reject` | `orders`, `order_items`, `stock_reservations`, `product_variants`, `system_logs` | VENDOR `VERIFIED` (cổng KYC) |
| UC-LOG-16 3b | `AutoConfirmOrdersJob` 15 phút | `orders` | nội bộ |
| UC-LOG-01 | `POST …/prepare`, tab / tìm kiếm của `GET /seller/orders` | `orders` | VENDOR |
| UC-LOG-15 | `GET …/shipping-quotes` | `vendor_shipping_config`, `vendor_profiles` (kho), `addresses` snapshot | VENDOR |
| UC-LOG-02 | `POST …/shipments` 🔁, `GET …/shipping-label` | `shipment_trackings`, `orders.ghn_order_code` | VENDOR |
| UC-LOG-03 | `POST /shipping/{carrier}/webhook/{secret}`, `ShipmentSyncJob` | `shipment_events`, `shipment_trackings`, `orders`, `escrow_ledgers` | `secret` |
| UC-LOG-03 COD | `GET /admin/cod-remittances`, `GET …/{id}`, `POST …/reconciliation-preview`, `POST …/confirm` 🔁 | `cod_remittances`, `bank_transactions`, `escrow_ledgers`, `orders`, `financial_ledger`, `system_logs` | `PROCESS_PAYOUT` |

**Mã lỗi:** `GNE-ORD-4041` (404), `GNE-ORD-4092` (409 sai bước), `GNE-LOG-4221` (422 bảng kê không cân), `GNE-LOG-5021` (502 hãng lỗi); **mới:** `GNE-LOG-4091` (409 đã có vận đơn), `GNE-LOG-4092` (409 không có hãng phục vụ). Dùng lại `GNE-VND-4031`, `GNE-SEC-4030`, `GNE-VAL-4220`.

---

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — `ModuleBoundaryTest` (**`shipping` không import `order`**, `order` không import `payment`), `SchemaValidationIT`.
2. Swagger: summary `UC-LOG-xx`; webhook không cần JWT (chỉ đúng đường dẫn).
3. `docs/api-tests/shipping.http`: shop A xác nhận đơn shop B → `404`; bàn giao 2 lần → `409 GNE-LOG-4091`; webhook sai secret → `401`; webhook trùng → không thêm sự kiện.
4. Đụng `web/`: `npm run verify`.
5. Tick progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Hxx.yy` = task; `Hxx.Cn` = task commit.

### H00 — Chuẩn bị

- **[H00.01]** Chạy `git config user.name` / `user.email` — ghi vào dòng *Phụ trách* của progress; kiểm C3 đã merge (`EscrowFacade`, `LedgerPoster`, `BankTransferRouter`) — chưa thì làm H01–H05 trước (không cần C3) và để H06–H07 sau.
- **[H00.02]** Chốt D1–D20 vào progress.
- **[H00.03]** Tạo nhánh `feature/c4-seller-orders` từ `develop`.
- **[H00.04]** Đọc máy trạng thái `Order` (ENTITY 5.2 + entity) — liệt kê chuyển hợp lệ, so với D1, D2, D4, D10; đọc CHECK `orders.status`, `shipment_trackings.status`, `carrier_type`.
- **[H00.05]** Tài khoản GHN sandbox (`GHN_TOKEN`, `GHN_SHOP_ID` của shop thử đã đăng ký kho — UC-VND-05) + đăng ký URL webhook ở trang GHN; thiếu thì chạy `stub` và ghi vào *Ghi chú*.
- **[H00.06]** Đọc màn mock Seller › Đơn hàng (tab, thẻ đơn, chi tiết, hộp Từ chối, báo giá hãng, in phiếu), Admin › Đối soát COD (danh sách phiên, tải CSV, bảng đối chiếu); `grep` các hàm `fulfillment.api.ts`, `finance.api.ts` (COD).

### H01 — Tài liệu trước

- **[H01.01]** ENTITY 5.2 (cột mới D1–D4, bảng chuyển trạng thái do shop / hãng), 7.1 (`label`, `shipping_fee_actual`, UNIQUE), 7.2 (`carrier_event_id`), 6.8 (luồng phiên đối soát D15–D16), Phụ lục A (`AuditAction.COD_RECONCILED`), Phụ lục E.
- **[H01.02]** API_SPEC — 10.3 (tab D4, lý do từ chối D2, báo giá D6, bàn giao D8, phiếu D9), 11.7 (CSV D16, quyền), 12.1 (bảng chuẩn hóa D10, D11), 12.2, 15 (`GNE-LOG-4091`, `4092`), lịch sử phiên bản.
- **[H01.03]** Usecase.md — UC-LOG-15 (chỉ GHN tích hợp thật — D5), UC-LOG-02 b5 (PDFBox + Code128, không giá), UC-LOG-03 (bảng D10, hoàn hàng D12).
- **[H01.04]** `docs/cau-hinh-moi-truong.md`, `.env.example`, `deploy/.env.example` — `CARRIER_MODE`, `GHN_WEBHOOK_SECRET`, `genie.cod.carrier-patterns`.
- **[H01.C1]** COMMIT — `docs(shipping): specify seller fulfillment, carrier routing, webhooks and cod reconciliation`

### H02 — Migration & vòng đời đơn

- **[H02.01]** `V29__c4_fulfillment_shipping.sql` — cột `orders` (D1–D4), `shipment_trackings` (`label_object_key` để trống cho DATN, `shipping_fee_actual`, UNIQUE `(carrier_type, tracking_code)`), `shipment_events.carrier_event_id` + UNIQUE, `AuditAction.COD_RECONCILED` (chép đủ danh sách `ck_system_logs_action_type` của `V27`/`V28`).
- **[H02.02]** `OrderLifecycleService` (N1) — `confirm`, `rejectByVendor`, `prepare`, `markReadyForPickup`, `applyShipmentStatus`; audit + `OrderStatusChangedEvent`.
- **[H02.03]** Cổng `CancellationRefunds` + bản mặc định `OrderPorts` (AdminNotification "cần hoàn tiền"); `CheckoutCompensation.onOrderCancelled` default no-op; hoàn tồn qua `VariantFacade` (nhả giữ chỗ / cộng tồn).
- **[H02.04]** `ErrorCode` + `AuditAction`.
- **[H02.05]** Test — `OrderLifecycleTest` (bảng chuyển hợp lệ / không hợp lệ cho mọi phương thức), `SchemaValidationIT`.
- **[H02.C1]** COMMIT — `feat(order): add order lifecycle service with vendor cancellation hooks`

### H03 — Shop xác nhận / từ chối / chuẩn bị (UC-LOG-16, LOG-01)

- **[H03.01]** `SellerOrderQuery` — read-model danh sách theo tab / tìm (`unaccent` hoặc `NameNormalizer`) / sắp / trang + `summary.counts`; chi tiết (dòng hàng, phân loại, `buyer_note`, phương thức, mốc thời gian, vận đơn nếu có).
- **[H03.02]** `SellerOrderController` — `GET /seller/orders`, `GET /{id}`, `POST /{id}/confirm`, `/reject`, `/prepare`; `@Operation(summary = "UC-LOG-16 …" / "UC-LOG-01 …")`.
- **[H03.03]** `AutoConfirmOrdersJob` (D3) + thông báo (D18).
- **[H03.04]** Test — `SellerOrderIT` (tab đếm đúng; tìm theo tên khách có dấu / không dấu; xác nhận online `PAID` → `CONFIRMED`; COD `PENDING_PAYMENT` → `CONFIRMED`; xác nhận 2 lần → 200; shop khác → 404; chuẩn bị đặt `shipping_expected_at`), `SellerOrderRejectIT` (COD chưa trả → nhả giữ chỗ, `reserved_count` giảm; online đã trả → `stock_count` cộng lại + AdminNotification hoàn tiền; từ `PREPARING` có vận đơn → 409), `AutoConfirmOrdersJobIT` (đơn 25 giờ → `auto_confirmed`; 23 giờ → giữ).
- **[H03.C1]** COMMIT — `feat(order): let sellers confirm, reject and prepare new orders`

### H04 — Điều phối đa hãng (UC-LOG-15) — nhánh `feature/c4-ghn-shipment`

- **[H04.01]** `CarrierGateway` (`quote`, `createOrder`, `detail`, `parseWebhook`) + `CarrierQuote(carrier, fee, eta, serviceName)`; `StubCarrierGateway` theo hãng (bảng phí vùng, ETA cố định) — D5.
- **[H04.02]** `GhnCarrierGateway` (`http`) — phí qua client đã có (`HttpGhnFeeClient`), tạo đơn `/v2/shipping-order/create` (header `Token`, `ShopId`), chi tiết `/v2/shipping-order/detail`, timeout 3 / 10 giây, ánh xạ lỗi.
- **[H04.03]** `CarrierRouter` (D6) — song song, bỏ hãng lỗi, chọn `recommended`, dự phòng GHN.
- **[H04.04]** `ShipmentFacade.quotes(orderId…)` + `GET /seller/orders/{id}/shipping-quotes`.
- **[H04.05]** Test — `CarrierRouterTest` (1 hãng quá hạn bị bỏ; luật ETA + 1 ngày; mọi hãng lỗi → GHN), `GhnCarrierGatewayTest` (MockWebServer: thành công, 4xx, quá hạn).
- **[H04.C1]** COMMIT — `feat(shipping): quote enabled carriers in parallel and recommend the best one`

### H05 — Tạo vận đơn & phiếu PDF (UC-LOG-02)

- **[H05.01]** `ShipmentService.create(order, carrier)` (D8, N4) + `ShipmentFacade.createReturn(...)` (chữ ký cho C8, thân dùng chung).
- **[H05.02]** `POST /seller/orders/{id}/shipments` 🔁 (Idempotency-Key) → `READY_FOR_PICKUP`, thông báo khách "Đã giao cho ĐVVC".
- **[H05.03]** `WaybillPdfRenderer` (D9) — PDFBox + ZXing, font nhúng; `GET …/shipping-label` (`application/pdf`, tên tệp `phieu-<mã đơn>.pdf`).
- **[H05.04]** Test — `ShipmentCreateIT` (stub: tạo → mã vận đơn + `READY_FOR_PICKUP`; lần 2 → `409 GNE-LOG-4091`; hãng lỗi → `502`, đơn giữ `PREPARING`, không có dòng vận đơn; cùng Idempotency-Key → cùng kết quả), `WaybillPdfRendererTest` (PDF mở được bằng PDFBox, kích thước trang 100×150 mm, có chữ tiếng Việt, không có giá).
- **[H05.C1]** COMMIT — `feat(shipping): create carrier waybills and render a6 shipping labels`

### H06 — Webhook & đồng bộ hành trình (UC-LOG-03) — nhánh `feature/c4-carrier-webhook`

- **[H06.01]** `CarrierStatusMapper` (D10) — bảng GHN + bảng stub các hãng.
- **[H06.02]** `CarrierWebhookController` + `CarrierWebhookService` (D11, N5) → `ShipmentStatusChangedEvent`.
- **[H06.03]** `OrderShipmentListener` (order, đồng bộ) — đổi đơn theo D10, hoàn hàng D12; thông báo khách (D18).
- **[H06.04]** `EscrowOrderStatusListener` (payment) — `DELIVERED` → `scheduleRelease` (D13); `RETURNED_TO_VENDOR` COD → đóng ký quỹ + bút toán (D12).
- **[H06.05]** `ShipmentSyncJob` (D14).
- **[H06.06]** Test — `CarrierStatusMapperTest`, `CarrierWebhookIT` (chuỗi picked → delivering → delivered: đơn `DELIVERING` → `DELIVERED`, `delivered_at`, ký quỹ `release_eligible_at = +7 ngày`; sự kiện trùng → 1 dòng; sự kiện cũ tới sau không kéo lùi; `delivery_fail` → `DELIVERY_FAILED` rồi giao lại; `returned` online → AdminNotification hoàn tiền + tồn cộng lại; COD → ký quỹ đóng; sai secret → 401), `ShipmentSyncJobIT` (MockWebServer trả `delivered`), `ShippingModuleBoundaryTest`.
- **[H06.C1]** COMMIT — `feat(shipping): sync shipment journeys from carrier webhooks into order states`
- **[H06.C2]** COMMIT — `feat(payment): schedule escrow release on delivery and close cod escrow on return`

### H07 — Đối soát COD (UC-LOG-03 luồng phụ) — nhánh `feature/c4-cod-reconciliation`

- **[H07.01]** `CodRemittanceMatcher` (D15) đăng ký vào `BankTransferRouter`.
- **[H07.02]** `CodStatementParser` (CSV UTF-8 / BOM, cột D16, số tiền có dấu chấm ngăn cách) + `CodReconciliationService.preview / confirm` (D16, N7) + `OrderPaymentFacade.markCodRemitted`.
- **[H07.03]** `AdminCodRemittanceController` — 4 endpoint API 11.7, `@PreAuthorize("hasAuthority('PROCESS_PAYOUT')")`, `POST …/confirm` 🔁.
- **[H07.04]** Test — `CodStatementParserTest`, `CodReconciliationIT` (webhook SePay tiền vào mẫu GHN → phiên `WAITING`; preview đủ 4 loại dòng; tổng lệch → 422 kèm bảng; confirm → ký quỹ `HELD`, đơn `PAID`, bút toán cân, `COD_RECONCILED`; confirm lại → không đổi gì; dòng đã đối soát ở phiên khác → `ALREADY_RECONCILED`).
- **[H07.C1]** COMMIT — `feat(payment): reconcile carrier cod remittances against uploaded statements`

### H08 — Nối web Seller › Đơn hàng (cờ `seller-orders`)

- **[H08.01]** `order.api.ts` + `fulfillment.api.ts` — 7 hàm theo `isMocked('seller-orders')`; DTO / mapper API 10.3 (tab, `summary.counts`, mốc thời gian, vận đơn, `auto_confirmed`).
- **[H08.02]** Màn danh sách + chi tiết: tab đếm, tìm, xác nhận, hộp từ chối (mã lý do + ghi chú), chuẩn bị, báo giá (đánh dấu "Đề xuất"), bàn giao (Idempotency-Key), in phiếu (mở PDF tab mới); lỗi `4092` / `4091` / `5021` hiện đúng chỗ; badge "Đơn mới" ở menu theo `summary.counts.NEW`. Kiểm 9 theme, khổ 360 px.
- **[H08.03]** vi / en + `error-codes.ts`; test mapper; `npm run verify`.
- **[H08.C1]** COMMIT — `feat(seller-ui): connect seller order fulfillment to the real api`

### H09 — Nối web Admin › Đối soát COD (cờ `cod-remittances`)

- **[H09.01]** `finance.api.ts` (4 hàm COD) theo `isMocked('cod-remittances')`; tải CSV (multipart), bảng đối chiếu 4 trạng thái, tổng lệch (`4221`) hiện cảnh báo, nút xác nhận chỉ bật khi cân.
- **[H09.C1]** COMMIT — `feat(admin-ui): connect cod remittance reconciliation to the real api`

### H10 — Dữ liệu demo, kiểm thử & chạy thật

- **[H10.01]** Seed — bật hãng cho shop seed (`vendor_shipping_config`: GHN + 1 hãng stub), kho lấy hàng có `ghn_shop_id` thử; tệp CSV mẫu bảng kê `docs/api-tests/data/cod-statement-sample.csv`.
- **[H10.02]** `docs/api-tests/shipping.http` — shop: danh sách, xác nhận, từ chối, chuẩn bị, báo giá, bàn giao, phiếu; webhook GHN (chuỗi 8 trạng thái, trùng, sai secret); COD: giả lập SePay tiền vào → preview → confirm.
- **[H10.03]** `docs/kiem-thu/kich-ban-2026-11-28-c4.md` — TC-C4-01…: đơn mới (online / COD) → xác nhận → chuẩn bị → báo giá → bàn giao → in phiếu → webhook giao thành công; từ chối (COD / online); tự xác nhận 24 giờ (chỉnh giờ bằng SQL); giao thất bại → giao lại; hoàn hàng; đối soát COD đủ / lệch; shop khác không xem được đơn; 9 theme; 360 px.
- **[H10.04]** Chạy thật — web build `PUBLIC_REAL_API=…,payment,seller-orders,cod-remittances` + backend `local` (`CARRIER_MODE=http` + GHN sandbox nếu có, webhook qua đường hầm tạm; không thì stub + gọi webhook bằng `.http`) + Postgres + Redis + Silo + Mailpit; Chromium (Playwright); mỗi lỗi một commit `fix(…)`.
- **[H10.C1]** COMMIT — `docs(shipping): add shipping api tests and real-run scenarios`

### H11 — Hoàn thiện use case & tick tiến độ

- **[H11.01]** OpenAPI — đủ endpoint, summary `UC-LOG-01/02/03/15/16`.
- **[H11.02]** `docs/web-backend-handoff.md` — nhóm `seller-orders`, `cod-remittances` đã nối; nếu C3 chưa đọc `shipping_fee_actual` khi giải ngân (D7) → ghi việc còn lại cho C3 / C10.
- **[H11.03]** Usecase.md — *Hiện trạng* 5 UC.
- **[H11.04]** `CHECKLIST_TIEN_DO_UC.md` — tick 5 lớp 5 UC (ô chưa đạt → `◐`, vd "GHTK / ViettelPost / J&T chỉ giả lập"), Trạng thái, **cột Phụ trách = tên ở H00.01**, số mục 1 + phân hệ 06, dòng mục 5 *Nhật ký*.
- **[H11.05]** `BAO_CAO_UC_DOC_LAP.md` — mục C4 (*Tiến độ* + *Phụ trách*); C5, C8 ghi "vận đơn / webhook / `OrderLifecycleService` đã có".
- **[H11.06]** `Quản Lý UseCase.xlsx` — 5 UC, cột AA.
- **[H11.07]** README ngày (mục 7) — tick dòng C4.
- **[H11.C1]** COMMIT — `docs(shipping): record seller fulfillment, waybills and carrier sync as implemented`
- **[H11.08]** Mở 4 PR xếp chồng vào `develop` (`c4-seller-orders` → `c4-ghn-shipment` → `c4-carrier-webhook` → `c4-cod-reconciliation`).

---

## 8. Ngoài phạm vi

- Khách hủy đơn / xác nhận nhận hàng / theo dõi hành trình (C5 — dùng `OrderLifecycleService`, `shipment_events`).
- Vận đơn chiều về cho trả hàng (C8 — gọi `ShipmentFacade.createReturn`).
- Tích hợp thật GHTK / ViettelPost / J&T / Genie Express, bảo hiểm hàng (DATN).

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Vòng `order ↔ shipping` | Sự kiện đồng bộ `ShipmentStatusChangedEvent`; `ShippingModuleBoundaryTest` |
| Webhook trùng / tới sai thứ tự kéo lùi trạng thái | UNIQUE `carrier_event_id`; chỉ chuyển tiến (N5); IT |
| GHN lỗi giữa lúc tạo vận đơn → mất dấu đơn đã tạo ở GHN | Gọi hãng trước, ghi CSDL sau; GHN trả mã nhưng ghi CSDL lỗi → log ERROR kèm mã + `ShipmentSyncJob` không cứu được → Staff hủy tay trên GHN (ghi trong kịch bản); Idempotency-Key chặn bấm lại |
| Font tiếng Việt trong PDF lỗi | Nhúng Noto Sans; test có chữ "Đơn hàng" |
| Bảng kê COD định dạng khác nhau giữa hãng | Parser theo cột tên cố định + tệp mẫu; dòng không đọc được → `NOT_FOUND` kèm số dòng |
