# PROGRESS — Chuỗi C6 Khuyến mãi khi mua (UC-PRO-04 · UC-PRO-10 · UC-PRO-05 · UC-PRO-06)

| | |
|---|---|
| **Plan** | [plan_khuyen_mai_khi_mua_c6.md](./plan_khuyen_mai_khi_mua_c6.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-28 |
| **Use case** | Thuật toán Stacking Voucher 3 Tầng & Điểm tự động khi Checkout (UC-PRO-04) · Sử dụng Điểm Genie để giảm trừ thanh toán (UC-PRO-10) · Tạo chiến dịch Flash Sale & Cấu hình khung giờ vàng (UC-PRO-05) · Mua hàng Flash Sale chống bán vượt suất (UC-PRO-06) |
| **Tổng** | 49 task + 15 commit, 12 giai đoạn |
| **Nhánh** | `feature/c6-voucher-stacking` (X00–X04, X08) → `feature/c6-coins-at-checkout` (X05) → `feature/c6-flash-sale` (X06–X07, X09–X11) |
| **Phụ trách** | _(điền kết quả `git config user.name` ở task X00.01 — tên này dùng khi tick cột Phụ trách ở checklist / báo cáo / Excel)_ |
| **Người commit** | danh tính git đã cấu hình trên máy người làm — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Cập nhật gần nhất** | 2026-11-28 — lập plan, chưa bắt đầu (0 / 64) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục *Ghi chú*.
- Task `COMMIT` chỉ tick sau khi qua *Cổng kiểm tra* (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`. Thông điệp đã ghi sẵn — tiếng Anh, chữ thường, đúng `commitlint.config.cjs`; có mã Jira thì thêm ` [#GNE-<số>]` cuối.
- Sau mỗi buổi làm: cập nhật cột *Xong* của bảng Tổng quan, dòng *Cập nhật gần nhất*, một dòng *Nhật ký* cuối trang.
- Cột *Chạy* của use case chỉ tick khi đã chạy thật trên giao diện (Chromium điều khiển bằng Playwright, hệ thống thật) và đạt hết ca của kịch bản — ghi cách chạy + số ca vào *Ghi chú*.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | **Cổng ghi khi đặt hàng** — `CheckoutDiscountPolicy` chỉ tính, không ghi | ☐ | |
| D2 | **Đầu vào chọn mã** (API 6.8 `vouchers`) | ☐ | |
| D3 | **Phân bổ xuống dòng hàng** | ☐ | |
| D4 | **Kiểm từng mã** (UC-PRO-04 b2) | ☐ | |
| D5 | **Tầng & trần** | ☐ | |
| D6 | **Gợi ý bộ tốt nhất** | ☐ | |
| D7 | **Voucher Picker** (API 6.9) | ☐ | |
| D8 | **Ghi lượt dùng & ngân sách** | ☐ | |
| D9 | **Điểm khi mua** (UC-PRO-10) | ☐ | |
| D10 | **Hoàn khi hết hạn / hủy** | ☐ | |
| D11 | **Mã lỗi khi đặt hàng** | ☐ | |
| D12 | **Chiến dịch Flash Sale của sàn** (11.15) | ☐ | |
| D13 | **Shop đăng ký sản phẩm** (10.7) | ☐ | |
| D14 | **Migration Flash Sale** | ☐ | |
| D15 | **Mở / đóng đúng giờ** | ☐ | |
| D16 | **Giá Flash Sale khi mua** | ☐ | |
| D17 | **Giữ suất & chống bán vượt** | ☐ | |
| D18 | **Flash Sale công khai** (5.16) | ☐ | |
| D19 | **Thông báo** | ☐ | |
| D20 | **Cờ web** | ☐ | |
| D21 | **Ngoài phạm vi** | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| X00 | Chuẩn bị | 5 | 0 | 0 / 5 |
| X01 | Tài liệu trước | 3 | 1 | 0 / 4 |
| X02 | Bộ tính 3 tầng (thuần) | 4 | 1 | 0 / 5 |
| X03 | Áp mã ở xem trước & Picker (UC-PRO-04) | 4 | 2 | 0 / 6 |
| X04 | Ghi lượt dùng khi đặt & hoàn (UC-PRO-04) | 4 | 1 | 0 / 5 |
| X05 | Điểm Genie khi mua (UC-PRO-10) — nhánh `feature/c6-coins-at-checkout` | 3 | 1 | 0 / 4 |
| X06 | Flash Sale: chiến dịch & đăng ký (UC-PRO-05) — nhánh `feature/c6-flash-sale` | 5 | 2 | 0 / 7 |
| X07 | Mua Flash Sale chống bán vượt suất (UC-PRO-06) | 3 | 2 | 0 / 5 |
| X08 | Nối web Giỏ / Thanh toán (cờ `checkout-vouchers`) | 3 | 1 | 0 / 4 |
| X09 | Nối web Flash Sale (cờ `flash-sales`, `seller-flash-sales`, `admin-flash-sales`) | 3 | 1 | 0 / 4 |
| X10 | Dữ liệu demo, kiểm thử & chạy thật | 4 | 2 | 0 / 6 |
| X11 | Hoàn thiện use case & tick tiến độ | 8 | 1 | 0 / 9 |
| **Tổng** | | **49** | **15** | **0 / 64** |

## X00 — Chuẩn bị

- [ ] **X00.01** Chạy `git config user.name` / `user.email`
- [ ] **X00.02** Chốt D1–D21 vào progress.
- [ ] **X00.03** Tạo nhánh `feature/c6-voucher-stacking` từ `develop`.
- [ ] **X00.04** Đọc `CheckoutPricer`, `CheckoutService.place`, `CheckoutView` (C2)
- [ ] **X00.05** Đọc màn mock Giỏ / Thanh toán (Picker 3 nhóm, nhập mã, lý do không áp được, bảng "Voucher Sàn phân bổ" theo shop, công tắc Điểm), khu FLASH SALE trang chủ, nhãn Flash trên trang sản phẩm, Seller › Khuyến mãi › Flash Sale, Admin › Flash Sale.

## X01 — Tài liệu trước

- [ ] **X01.01** ENTITY 4.5–4.6 (cột mới D14, `validateAgainst` theo biến thể), **4.10 mới `FlashSalePurchase`**, 5.3 (công thức phân bổ D3 + làm tròn N5), 5.4 (`funded_by`, một dòng / mã / đơn con
- [ ] **X01.02** API_SPEC
- [ ] **X01.03** Usecase.md
- [ ] **X01.C1** 🔖 COMMIT `docs(promotion): specify voucher stacking, coins at checkout and flash sale purchase rules` · hash: `…`

## X02 — Bộ tính 3 tầng (thuần)

- [ ] **X02.01** `VoucherEligibility` (D4)
- [ ] **X02.02** `DiscountAllocator` (D3, N5)
- [ ] **X02.03** `StackingEngine.solve` (D5, D6, D9)
- [ ] **X02.04** Test
- [ ] **X02.C1** 🔖 COMMIT `feat(promotion): add three-tier voucher stacking engine with coin cap and line allocation` · hash: `…`

## X03 — Áp mã ở xem trước & Picker (UC-PRO-04)

- [ ] **X03.01** Sửa cổng C2: `Discounts.lines` (D3), `Selection` theo D2 + `CheckoutPricer` ghi phân bổ; bản mặc định `NONE` vẫn chạy (C2 IT xanh).
- [ ] **X03.02** `PromotionCheckoutPolicy implements CheckoutDiscountPolicy`
- [ ] **X03.03** Cổng đọc `CheckoutLinesReader` (order cài) + `PickerService` + `CheckoutVoucherController` `POST /me/checkout/applicable-vouchers` (D7).
- [ ] **X03.04** Test
- [ ] **X03.C1** 🔖 COMMIT `feat(order): carry voucher selections and line allocations through checkout pricing` · hash: `…`
- [ ] **X03.C2** 🔖 COMMIT `feat(promotion): apply vouchers at checkout preview and serve the voucher picker` · hash: `…`

## X04 — Ghi lượt dùng khi đặt & hoàn (UC-PRO-04)

- [ ] **X04.01** `commit` (D1, D8)
- [ ] **X04.02** `PromotionCompensation` (D10)
- [ ] **X04.03** Ánh xạ lỗi D11 trong `GlobalExceptionHandler` / service đặt hàng.
- [ ] **X04.04** Test
- [ ] **X04.C1** 🔖 COMMIT `feat(promotion): consume vouchers when orders are placed and restore them on expiry or cancellation` · hash: `…`

## X05 — Điểm Genie khi mua (UC-PRO-10) — nhánh `feature/c6-coins-at-checkout`

- [ ] **X05.01** `CoinCheckoutService` (D9)
- [ ] **X05.02** `CoinLedgerService.creditBack(user, amount, ORDER_REFUND, orderId)` giữ hạn lô gốc (đọc lại các lô đã trừ theo `reference`).
- [ ] **X05.03** Test
- [ ] **X05.C1** 🔖 COMMIT `feat(promotion): spend genie coins at checkout and refund them with original expiry` · hash: `…`

## X06 — Flash Sale: chiến dịch & đăng ký (UC-PRO-05) — nhánh `feature/c6-flash-sale`

- [ ] **X06.01** `V30__c6_flash_sale.sql` (D14) + entity sửa / mới + `ErrorCode` + `AuditAction`.
- [ ] **X06.02** `FlashSaleAdminService` + `AdminFlashSaleController` (D12)
- [ ] **X06.03** `FlashSaleRegistrationService` + `SellerFlashSaleController` (D13)
- [ ] **X06.04** `FlashSaleClockJob` (D15) + thông báo (D19) + `PublicFlashSaleService` / `PublicFlashSaleController` + cache (D18).
- [ ] **X06.05** Test
- [ ] **X06.C1** 🔖 COMMIT `feat(db): add flash sale registration columns and purchase limits` · hash: `…`
- [ ] **X06.C2** 🔖 COMMIT `feat(promotion): run flash sale campaigns with seller registration and timed activation` · hash: `…`

## X07 — Mua Flash Sale chống bán vượt suất (UC-PRO-06)

- [ ] **X07.01** Cổng `FlashSalePricing`, `FlashSaleReservations` (order) + bản mặc định (không có Flash Sale) + cài ở promotion (D16, D17).
- [ ] **X07.02** `CheckoutPricer` / `CheckoutService` dùng giá + giữ suất; `OrderPaymentFacade` gọi `confirm` (online); COD `confirm` ngay; `onExpired` / `onOrderCancelled` gọi `release`.
- [ ] **X07.03** Test
- [ ] **X07.C1** 🔖 COMMIT `feat(order): add flash sale pricing and reservation ports to checkout` · hash: `…`
- [ ] **X07.C2** 🔖 COMMIT `feat(promotion): reserve flash sale slots atomically without overselling` · hash: `…`

## X08 — Nối web Giỏ / Thanh toán (cờ `checkout-vouchers`)

- [ ] **X08.01** Checkout API
- [ ] **X08.02** Picker 3 nhóm (đánh dấu "Tốt nhất", lý do không áp được, nhập mã), bảng giảm (Shop / Sàn / Freeship / Điểm, "Voucher Sàn phân bổ" theo shop), công tắc Điểm ("Dùng N Điểm
- [ ] **X08.03** vi / en (`checkout.voucher.reason.*`), `error-codes.ts`; test mapper; `npm run verify`.
- [ ] **X08.C1** 🔖 COMMIT `feat(web): connect voucher picker and coin usage at checkout to the real api` · hash: `…`

## X09 — Nối web Flash Sale (cờ `flash-sales`, `seller-flash-sales`, `admin-flash-sales`)

- [ ] **X09.01** Trang chủ khu FLASH SALE (đếm ngược, thanh "Đã bán x/y"), nhãn + giá Flash ở trang sản phẩm / giỏ (`flash_sale` của 6.8).
- [ ] **X09.02** Seller › Khuyến mãi › Flash Sale (4 hàm `promo.api.ts`)
- [ ] **X09.03** Admin › Flash Sale (6 hàm `marketing.api.ts`)
- [ ] **X09.C1** 🔖 COMMIT `feat(web): connect flash sale storefront, seller registration and admin campaigns to the real api` · hash: `…`

## X10 — Dữ liệu demo, kiểm thử & chạy thật

- [ ] **X10.01** Seed
- [ ] **X10.02** `docs/api-tests/checkout-promotions.http` + `flash-sales.http`.
- [ ] **X10.03** `docs/kiem-thu/kich-ban-2026-11-28-c6.md`
- [ ] **X10.04** Chạy thật
- [ ] **X10.C1** 🔖 COMMIT `feat(db): seed a scheduled flash sale for demos` · hash: `…`
- [ ] **X10.C2** 🔖 COMMIT `docs(promotion): add checkout promotion api tests and real-run scenarios` · hash: `…`

## X11 — Hoàn thiện use case & tick tiến độ

- [ ] **X11.01** OpenAPI
- [ ] **X11.02** `docs/web-backend-handoff.md`
- [ ] **X11.03** Usecase.md
- [ ] **X11.04** `CHECKLIST_TIEN_DO_UC.md`
- [ ] **X11.05** `BAO_CAO_UC_DOC_LAP.md`
- [ ] **X11.06** `Quản Lý UseCase.xlsx`
- [ ] **X11.07** README ngày (mục 7)
- [ ] **X11.C1** 🔖 COMMIT `docs(promotion): record voucher stacking, coins at checkout and flash sale as implemented` · hash: `…`
- [ ] **X11.08** Mở 3 PR xếp chồng vào `develop` (`c6-voucher-stacking` → `c6-coins-at-checkout` → `c6-flash-sale`); PR đầu nhờ người làm C2 duyệt thay đổi cổng checkout.

## Ghi chú

| Task | Ghi chú |
|---|---|
| | |

## Nhật ký

| Ngày | Việc đã làm | Commit / PR |
|---|---|---|
| 2026-11-28 | Lập plan + progress (49 task, 15 commit, 21 quyết định chờ chốt) | nhánh `feature/plan-c3-c11` |
