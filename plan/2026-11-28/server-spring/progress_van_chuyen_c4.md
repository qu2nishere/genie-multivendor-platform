# PROGRESS — Chuỗi C4 Vận chuyển (UC-LOG-16 · UC-LOG-01 · UC-LOG-02 · UC-LOG-15 · UC-LOG-03)

| | |
|---|---|
| **Plan** | [plan_van_chuyen_c4.md](./plan_van_chuyen_c4.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-28 |
| **Use case** | Người bán xác nhận / từ chối đơn hàng mới (UC-LOG-16) · Tiếp nhận đơn hàng & Đóng gói tại kho shop (UC-LOG-01) · Đẩy vận đơn sang GHN & In phiếu gửi hàng PDF (UC-LOG-02) · Điều phối đa hãng vận chuyển (UC-LOG-15) · Đồng bộ hành trình giao nhận qua Webhook GHN (UC-LOG-03, gồm luồng phụ đối soát COD và hoàn hàng) |
| **Tổng** | 54 task + 12 commit, 12 giai đoạn |
| **Nhánh** | `feature/c4-seller-orders` (H00–H03, H08) → `feature/c4-ghn-shipment` (H04–H05) → `feature/c4-carrier-webhook` (H06) → `feature/c4-cod-reconciliation` (H07, H09–H11) |
| **Phụ trách** | _(điền kết quả `git config user.name` ở task H00.01 — tên này dùng khi tick cột Phụ trách ở checklist / báo cáo / Excel)_ |
| **Người commit** | danh tính git đã cấu hình trên máy người làm — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Cập nhật gần nhất** | 2026-11-28 — lập plan, chưa bắt đầu (0 / 66) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục *Ghi chú*.
- Task `COMMIT` chỉ tick sau khi qua *Cổng kiểm tra* (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`. Thông điệp đã ghi sẵn — tiếng Anh, chữ thường, đúng `commitlint.config.cjs`; có mã Jira thì thêm ` [#GNE-<số>]` cuối.
- Sau mỗi buổi làm: cập nhật cột *Xong* của bảng Tổng quan, dòng *Cập nhật gần nhất*, một dòng *Nhật ký* cuối trang.
- Cột *Chạy* của use case chỉ tick khi đã chạy thật trên giao diện (Chromium điều khiển bằng Playwright, hệ thống thật) và đạt hết ca của kịch bản — ghi cách chạy + số ca vào *Ghi chú*.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | **Đơn nào shop được xác nhận** | ☐ | |
| D2 | **Từ chối đơn** (UC-LOG-16 3a) | ☐ | |
| D3 | **Tự xác nhận sau 24 giờ** (UC-LOG-16 3b) | ☐ | |
| D4 | **Chuẩn bị hàng** (UC-LOG-01) | ☐ | |
| D5 | **Hãng hỗ trợ trong TLCN** | ☐ | |
| D6 | **Chọn hãng** (UC-LOG-15 b2–3) | ☐ | |
| D7 | **Chênh lệch phí ship** | ☐ | |
| D8 | **Tạo vận đơn** (UC-LOG-02) | ☐ | |
| D9 | **Phiếu gửi hàng PDF** | ☐ | |
| D10 | **Bảng chuẩn hóa trạng thái** (UC-LOG-03 b7) | ☐ | |
| D11 | **Webhook** — một đường cho mọi hãng | ☐ | |
| D12 | **Hoàn hàng về shop** (UC-LOG-03 luồng phụ) | ☐ | |
| D13 | **Đặt hạn giải ngân khi giao xong** | ☐ | |
| D14 | **Đồng bộ khi lỡ webhook** | ☐ | |
| D15 | **Phiên đối soát COD** (11.7) | ☐ | |
| D16 | **Đối chiếu bảng kê** | ☐ | |
| D17 | **Hoàn tất sau đối soát** | ☐ | |
| D18 | **Thông báo** | ☐ | |
| D19 | **Cờ web** | ☐ | |
| D20 | **Ngoài phạm vi** | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| H00 | Chuẩn bị | 6 | 0 | 0 / 6 |
| H01 | Tài liệu trước | 4 | 1 | 0 / 5 |
| H02 | Migration & vòng đời đơn | 5 | 1 | 0 / 6 |
| H03 | Shop xác nhận / từ chối / chuẩn bị (UC-LOG-16, LOG-01) | 4 | 1 | 0 / 5 |
| H04 | Điều phối đa hãng (UC-LOG-15) — nhánh `feature/c4-ghn-shipment` | 5 | 1 | 0 / 6 |
| H05 | Tạo vận đơn & phiếu PDF (UC-LOG-02) | 4 | 1 | 0 / 5 |
| H06 | Webhook & đồng bộ hành trình (UC-LOG-03) — nhánh `feature/c4-carrier-webhook` | 6 | 2 | 0 / 8 |
| H07 | Đối soát COD (UC-LOG-03 luồng phụ) — nhánh `feature/c4-cod-reconciliation` | 4 | 1 | 0 / 5 |
| H08 | Nối web Seller › Đơn hàng (cờ `seller-orders`) | 3 | 1 | 0 / 4 |
| H09 | Nối web Admin › Đối soát COD (cờ `cod-remittances`) | 1 | 1 | 0 / 2 |
| H10 | Dữ liệu demo, kiểm thử & chạy thật | 4 | 1 | 0 / 5 |
| H11 | Hoàn thiện use case & tick tiến độ | 8 | 1 | 0 / 9 |
| **Tổng** | | **54** | **12** | **0 / 66** |

## H00 — Chuẩn bị

- [ ] **H00.01** Chạy `git config user.name` / `user.email`
- [ ] **H00.02** Chốt D1–D20 vào progress.
- [ ] **H00.03** Tạo nhánh `feature/c4-seller-orders` từ `develop`.
- [ ] **H00.04** Đọc máy trạng thái `Order` (ENTITY 5.2 + entity)
- [ ] **H00.05** Tài khoản GHN sandbox (`GHN_TOKEN`, `GHN_SHOP_ID` của shop thử đã đăng ký kho
- [ ] **H00.06** Đọc màn mock Seller › Đơn hàng (tab, thẻ đơn, chi tiết, hộp Từ chối, báo giá hãng, in phiếu), Admin › Đối soát COD (danh sách phiên, tải CSV, bảng đối chiếu); `grep` các hàm `fulfillment.api.ts`, `finance.api.ts` (COD).

## H01 — Tài liệu trước

- [ ] **H01.01** ENTITY 5.2 (cột mới D1–D4, bảng chuyển trạng thái do shop / hãng), 7.1 (`label`, `shipping_fee_actual`, UNIQUE), 7.2 (`carrier_event_id`), 6.8 (luồng phiên đối soát D15–D16), Phụ lục A (`AuditAction.COD_RECONCILED`), Phụ lục E.
- [ ] **H01.02** API_SPEC
- [ ] **H01.03** Usecase.md
- [ ] **H01.04** `docs/cau-hinh-moi-truong.md`, `.env.example`, `deploy/.env.example`
- [ ] **H01.C1** 🔖 COMMIT `docs(shipping): specify seller fulfillment, carrier routing, webhooks and cod reconciliation` · hash: `…`

## H02 — Migration & vòng đời đơn

- [ ] **H02.01** `V29__c4_fulfillment_shipping.sql`
- [ ] **H02.02** `OrderLifecycleService` (N1)
- [ ] **H02.03** Cổng `CancellationRefunds` + bản mặc định `OrderPorts` (AdminNotification "cần hoàn tiền"); `CheckoutCompensation.onOrderCancelled` default no-op; hoàn tồn qua `VariantFacade` (nhả giữ chỗ / cộng tồn).
- [ ] **H02.04** `ErrorCode` + `AuditAction`.
- [ ] **H02.05** Test
- [ ] **H02.C1** 🔖 COMMIT `feat(order): add order lifecycle service with vendor cancellation hooks` · hash: `…`

## H03 — Shop xác nhận / từ chối / chuẩn bị (UC-LOG-16, LOG-01)

- [ ] **H03.01** `SellerOrderQuery`
- [ ] **H03.02** `SellerOrderController`
- [ ] **H03.03** `AutoConfirmOrdersJob` (D3) + thông báo (D18).
- [ ] **H03.04** Test
- [ ] **H03.C1** 🔖 COMMIT `feat(order): let sellers confirm, reject and prepare new orders` · hash: `…`

## H04 — Điều phối đa hãng (UC-LOG-15) — nhánh `feature/c4-ghn-shipment`

- [ ] **H04.01** `CarrierGateway` (`quote`, `createOrder`, `detail`, `parseWebhook`) + `CarrierQuote(carrier, fee, eta, serviceName)`; `StubCarrierGateway` theo hãng (bảng phí vùng, ETA cố định)
- [ ] **H04.02** `GhnCarrierGateway` (`http`)
- [ ] **H04.03** `CarrierRouter` (D6)
- [ ] **H04.04** `ShipmentFacade.quotes(orderId…)` + `GET /seller/orders/{id}/shipping-quotes`.
- [ ] **H04.05** Test
- [ ] **H04.C1** 🔖 COMMIT `feat(shipping): quote enabled carriers in parallel and recommend the best one` · hash: `…`

## H05 — Tạo vận đơn & phiếu PDF (UC-LOG-02)

- [ ] **H05.01** `ShipmentService.create(order, carrier)` (D8, N4) + `ShipmentFacade.createReturn(...)` (chữ ký cho C8, thân dùng chung).
- [ ] **H05.02** `POST /seller/orders/{id}/shipments` 🔁 (Idempotency-Key) → `READY_FOR_PICKUP`, thông báo khách "Đã giao cho ĐVVC".
- [ ] **H05.03** `WaybillPdfRenderer` (D9)
- [ ] **H05.04** Test
- [ ] **H05.C1** 🔖 COMMIT `feat(shipping): create carrier waybills and render a6 shipping labels` · hash: `…`

## H06 — Webhook & đồng bộ hành trình (UC-LOG-03) — nhánh `feature/c4-carrier-webhook`

- [ ] **H06.01** `CarrierStatusMapper` (D10)
- [ ] **H06.02** `CarrierWebhookController` + `CarrierWebhookService` (D11, N5) → `ShipmentStatusChangedEvent`.
- [ ] **H06.03** `OrderShipmentListener` (order, đồng bộ)
- [ ] **H06.04** `EscrowOrderStatusListener` (payment)
- [ ] **H06.05** `ShipmentSyncJob` (D14).
- [ ] **H06.06** Test
- [ ] **H06.C1** 🔖 COMMIT `feat(shipping): sync shipment journeys from carrier webhooks into order states` · hash: `…`
- [ ] **H06.C2** 🔖 COMMIT `feat(payment): schedule escrow release on delivery and close cod escrow on return` · hash: `…`

## H07 — Đối soát COD (UC-LOG-03 luồng phụ) — nhánh `feature/c4-cod-reconciliation`

- [ ] **H07.01** `CodRemittanceMatcher` (D15) đăng ký vào `BankTransferRouter`.
- [ ] **H07.02** `CodStatementParser` (CSV UTF-8 / BOM, cột D16, số tiền có dấu chấm ngăn cách) + `CodReconciliationService.preview / confirm` (D16, N7) + `OrderPaymentFacade.markCodRemitted`.
- [ ] **H07.03** `AdminCodRemittanceController`
- [ ] **H07.04** Test
- [ ] **H07.C1** 🔖 COMMIT `feat(payment): reconcile carrier cod remittances against uploaded statements` · hash: `…`

## H08 — Nối web Seller › Đơn hàng (cờ `seller-orders`)

- [ ] **H08.01** `order.api.ts` + `fulfillment.api.ts`
- [ ] **H08.02** Màn danh sách + chi tiết: tab đếm, tìm, xác nhận, hộp từ chối (mã lý do + ghi chú), chuẩn bị, báo giá (đánh dấu "Đề xuất"), bàn giao (Idempotency-Key), in phiếu (mở PDF tab mới); lỗi `4092` / `4091` / `5021` hiện đúng chỗ; badge "Đơn mới" ở menu theo `summary.counts.NEW`. Kiểm 9 theme, khổ 360 px.
- [ ] **H08.03** vi / en + `error-codes.ts`; test mapper; `npm run verify`.
- [ ] **H08.C1** 🔖 COMMIT `feat(seller-ui): connect seller order fulfillment to the real api` · hash: `…`

## H09 — Nối web Admin › Đối soát COD (cờ `cod-remittances`)

- [ ] **H09.01** `finance.api.ts` (4 hàm COD) theo `isMocked('cod-remittances')`; tải CSV (multipart), bảng đối chiếu 4 trạng thái, tổng lệch (`4221`) hiện cảnh báo, nút xác nhận chỉ bật khi cân.
- [ ] **H09.C1** 🔖 COMMIT `feat(admin-ui): connect cod remittance reconciliation to the real api` · hash: `…`

## H10 — Dữ liệu demo, kiểm thử & chạy thật

- [ ] **H10.01** Seed
- [ ] **H10.02** `docs/api-tests/shipping.http`
- [ ] **H10.03** `docs/kiem-thu/kich-ban-2026-11-28-c4.md`
- [ ] **H10.04** Chạy thật
- [ ] **H10.C1** 🔖 COMMIT `docs(shipping): add shipping api tests and real-run scenarios` · hash: `…`

## H11 — Hoàn thiện use case & tick tiến độ

- [ ] **H11.01** OpenAPI
- [ ] **H11.02** `docs/web-backend-handoff.md`
- [ ] **H11.03** Usecase.md
- [ ] **H11.04** `CHECKLIST_TIEN_DO_UC.md`
- [ ] **H11.05** `BAO_CAO_UC_DOC_LAP.md`
- [ ] **H11.06** `Quản Lý UseCase.xlsx`
- [ ] **H11.07** README ngày (mục 7)
- [ ] **H11.C1** 🔖 COMMIT `docs(shipping): record seller fulfillment, waybills and carrier sync as implemented` · hash: `…`
- [ ] **H11.08** Mở 4 PR xếp chồng vào `develop` (`c4-seller-orders` → `c4-ghn-shipment` → `c4-carrier-webhook` → `c4-cod-reconciliation`).

## Ghi chú

| Task | Ghi chú |
|---|---|
| | |

## Nhật ký

| Ngày | Việc đã làm | Commit / PR |
|---|---|---|
| 2026-11-28 | Lập plan + progress (54 task, 12 commit, 20 quyết định chờ chốt) | nhánh `feature/plan-c3-c11` |
