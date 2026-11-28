# PROGRESS — Chuỗi C5 Sau giao hàng (UC-ORD-09 · UC-ORD-10 · UC-ORD-11 · UC-ORD-12)

| | |
|---|---|
| **Plan** | [plan_sau_giao_hang_c5.md](./plan_sau_giao_hang_c5.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-28 |
| **Use case** | Khách hàng theo dõi chi tiết hành trình đơn hàng (UC-ORD-09) · Khách hàng yêu cầu hủy đơn hàng (UC-ORD-10) · Khách hàng xác nhận "Đã nhận hàng thành công" (UC-ORD-11) · Hệ thống tự động hoàn tất đơn sau 7 ngày từ khi Delivered (UC-ORD-12) |
| **Tổng** | 35 task + 8 commit, 9 giai đoạn |
| **Nhánh** | `feature/c5-buyer-orders` (D00–D02) → `feature/c5-cancel-complete` (D03–D08) |
| **Phụ trách** | _(điền kết quả `git config user.name` ở task D00.01 — tên này dùng khi tick cột Phụ trách ở checklist / báo cáo / Excel)_ |
| **Người commit** | danh tính git đã cấu hình trên máy người làm — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Cập nhật gần nhất** | 2026-11-28 — lập plan, chưa bắt đầu (0 / 43) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục *Ghi chú*.
- Task `COMMIT` chỉ tick sau khi qua *Cổng kiểm tra* (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`. Thông điệp đã ghi sẵn — tiếng Anh, chữ thường, đúng `commitlint.config.cjs`; có mã Jira thì thêm ` [#GNE-<số>]` cuối.
- Sau mỗi buổi làm: cập nhật cột *Xong* của bảng Tổng quan, dòng *Cập nhật gần nhất*, một dòng *Nhật ký* cuối trang.
- Cột *Chạy* của use case chỉ tick khi đã chạy thật trên giao diện (Chromium điều khiển bằng Playwright, hệ thống thật) và đạt hết ca của kịch bản — ghi cách chạy + số ca vào *Ghi chú*.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | **Đường dẫn** | ☐ | |
| D2 | **Danh sách đơn của khách** (API 7.1) | ☐ | |
| D3 | **Cờ hành động** trên đơn (`actions`) — web không tự suy luật | ☐ | |
| D4 | **Dòng thời gian hành trình** (UC-ORD-09 b3–4) | ☐ | |
| D5 | **Cổng giải ngân** | ☐ | |
| D6 | **Xác nhận đã nhận hàng** (UC-ORD-11) | ☐ | |
| D7 | **Hoàn tất ngay sau đối soát COD** (UC-LOG-03 b4) | ☐ | |
| D8 | **Tự hoàn tất 7 ngày** (UC-ORD-12) | ☐ | |
| D9 | **Hủy đơn của khách** (UC-ORD-10) | ☐ | |
| D10 | **Đơn online chưa trả tiền** — hủy một đơn con làm lệch số tiền phiên | ☐ | |
| D11 | **Hủy một phần nhiều shop** (UC-ORD-10 ràng buộc) | ☐ | |
| D12 | **Thông báo** | ☐ | |
| D13 | **Cờ web** | ☐ | |
| D14 | **Ngoài phạm vi** | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| D00 | Chuẩn bị | 5 | 0 | 0 / 5 |
| D01 | Tài liệu trước | 2 | 1 | 0 / 3 |
| D02 | Đơn của khách & hành trình (UC-ORD-09) | 4 | 1 | 0 / 5 |
| D03 | Hủy đơn (UC-ORD-10) — nhánh `feature/c5-cancel-complete` | 4 | 1 | 0 / 5 |
| D04 | Xác nhận đã nhận hàng (UC-ORD-11) | 3 | 1 | 0 / 4 |
| D05 | Tự hoàn tất (UC-ORD-12) | 3 | 1 | 0 / 4 |
| D06 | Nối web (cờ `orders`) | 3 | 1 | 0 / 4 |
| D07 | Kiểm thử & chạy thật | 3 | 1 | 0 / 4 |
| D08 | Hoàn thiện use case & tick tiến độ | 8 | 1 | 0 / 9 |
| **Tổng** | | **35** | **8** | **0 / 43** |

## D00 — Chuẩn bị

- [ ] **D00.01** Chạy `git config user.name` / `user.email`
- [ ] **D00.02** Chốt D1–D14 vào progress.
- [ ] **D00.03** Tạo nhánh `feature/c5-buyer-orders` từ `develop`.
- [ ] **D00.04** Đọc CHECK `checkout_sessions.status` (D10), `orders.status`; đọc `ExpiredSessionSweeper` (C2) để hủy nhóm dùng lại phần nhả giữ chỗ.
- [ ] **D00.05** Đọc màn mock Đơn mua (8 tab, thẻ đơn), Chi tiết đơn (thanh tiến trình, dòng thời gian, khối vận đơn, nút hành động, hộp hủy với lý do), "Huỷ nhóm đơn" ở trang kết quả thanh toán; `grep` `listOrders`, `getOrder`, `cancelOrder`, `confirmReceipt`.

## D01 — Tài liệu trước

- [ ] **D01.01** API_SPEC
- [ ] **D01.02** Usecase.md
- [ ] **D01.C1** 🔖 COMMIT `docs(order): specify buyer order tracking, cancellation and receipt confirmation` · hash: `…`

## D02 — Đơn của khách & hành trình (UC-ORD-09)

- [ ] **D02.01** `OrderActions` (D3) + `OrderTimeline` (D4)
- [ ] **D02.02** `BuyerOrderQuery`
- [ ] **D02.03** `OrderController`
- [ ] **D02.04** Test
- [ ] **D02.C1** 🔖 COMMIT `feat(order): list buyer orders with tracking timeline and allowed actions` · hash: `…`

## D03 — Hủy đơn (UC-ORD-10) — nhánh `feature/c5-cancel-complete`

- [ ] **D03.01** `OrderLifecycleService.cancelByCustomer` (D9, D11)
- [ ] **D03.02** `CheckoutService.cancelSession` (D10) + `POST /me/checkout-sessions/{id}/cancel`.
- [ ] **D03.03** `POST /me/orders/{id}/cancel` + thông báo shop (D12).
- [ ] **D03.04** Test
- [ ] **D03.C1** 🔖 COMMIT `feat(order): let buyers cancel orders and unpaid checkout sessions` · hash: `…`

## D04 — Xác nhận đã nhận hàng (UC-ORD-11)

- [ ] **D04.01** Cổng `OrderEscrowGateway` (order) + `OrderEscrowGatewayImpl` (payment) (D5).
- [ ] **D04.02** `OrderLifecycleService.confirmReceipt` (D6) + `POST /me/orders/{id}/confirm-receipt`; phát `OrderCompletedEvent`.
- [ ] **D04.03** Test
- [ ] **D04.C1** 🔖 COMMIT `feat(order): confirm receipt and release escrow when funds are held` · hash: `…`

## D05 — Tự hoàn tất (UC-ORD-12)

- [ ] **D05.01** `AutoCompleteOrdersJob` (D8, N6) + cấu hình `genie.order.auto-complete-after`.
- [ ] **D05.02** `CodRemittedCompletionListener` (D7).
- [ ] **D05.03** Test
- [ ] **D05.C1** 🔖 COMMIT `feat(order): auto-complete delivered orders after the 7-day return window` · hash: `…`

## D06 — Nối web (cờ `orders`)

- [ ] **D06.01** `order.api.ts`
- [ ] **D06.02** Màn Đơn mua (8 tab + đếm, tìm), Chi tiết đơn (thanh tiến trình theo `progress`, dòng thời gian, khối vận đơn + mã chép được), hộp Hủy (lý do), "Đã nhận được hàng" (hai thông điệp theo D6), "Huỷ nhóm đơn"; nút hiện / ẩn **chỉ** theo `actions`; lỗi `4092` đọc `hint` để hướng dẫn (trả hàng / hủy nhóm). Kiểm 9 theme, 360 px.
- [ ] **D06.03** vi / en + `error-codes.ts`; test mapper (`order.mapper.test.ts`); `npm run verify`.
- [ ] **D06.C1** 🔖 COMMIT `feat(web): connect buyer orders, cancellation and receipt confirmation to the real api` · hash: `…`

## D07 — Kiểm thử & chạy thật

- [ ] **D07.01** `docs/api-tests/buyer-orders.http`
- [ ] **D07.02** `docs/kiem-thu/kich-ban-2026-11-28-c5.md`
- [ ] **D07.03** Chạy thật
- [ ] **D07.C1** 🔖 COMMIT `docs(order): add buyer order api tests and real-run scenarios` · hash: `…`

## D08 — Hoàn thiện use case & tick tiến độ

- [ ] **D08.01** OpenAPI
- [ ] **D08.02** `docs/web-backend-handoff.md`
- [ ] **D08.03** Usecase.md
- [ ] **D08.04** `CHECKLIST_TIEN_DO_UC.md`
- [ ] **D08.05** `BAO_CAO_UC_DOC_LAP.md`
- [ ] **D08.06** `Quản Lý UseCase.xlsx`
- [ ] **D08.07** README ngày (mục 7)
- [ ] **D08.C1** 🔖 COMMIT `docs(order): record buyer order tracking, cancellation and completion as implemented` · hash: `…`
- [ ] **D08.08** Mở 2 PR xếp chồng vào `develop` (`c5-buyer-orders` → `c5-cancel-complete`).

## Ghi chú

| Task | Ghi chú |
|---|---|
| | |

## Nhật ký

| Ngày | Việc đã làm | Commit / PR |
|---|---|---|
| 2026-11-28 | Lập plan + progress (35 task, 8 commit, 14 quyết định chờ chốt) | nhánh `feature/plan-c3-c11` |
