# PLAN — Chuỗi C6 Khuyến mãi khi mua (UC-PRO-04 · UC-PRO-10 · UC-PRO-05 · UC-PRO-06)

| | |
|---|---|
| **Ngày lập** | 2026-11-28 |
| **Thể loại** | server-spring (module `promotion` feature `voucher` (áp mã), `coin` (tiêu Điểm), `flashsale` (mới); sửa nhỏ cổng `order/checkout`) + nối `web/` (feature `checkout`, `promotions`, `seller-promotions`, `admin-marketing`) |
| **Use case** | Thuật toán Stacking Voucher 3 Tầng & Điểm tự động khi Checkout (UC-PRO-04) · Sử dụng Điểm Genie để giảm trừ thanh toán (UC-PRO-10) · Tạo chiến dịch Flash Sale & Cấu hình khung giờ vàng (UC-PRO-05) · Mua hàng Flash Sale chống bán vượt suất (UC-PRO-06) |
| **Tài liệu chuẩn** | UC phân hệ 07 (UC-PRO-04 dòng 1969, 05 dòng 2000, 06 dòng 2026, 10 dòng 2100) · API 5.16 (Flash Sale công khai), 6.8 (`vouchers`, `use_coins`), 6.9 (Voucher Picker), 6.10, 10.7 (đăng ký Flash Sale), 11.15 (Flash Sale sàn), 12.2 (mở / đóng 1 phút), 15 (`GNE-VCH-4221`, `GNE-INV-4091`, `GNE-PRO-4221…4224`) · ENTITY 4.1–4.8, 5.3 `OrderItem` (`shop_discount`, `platform_discount`, `prorated_discount`, `coin_discount`), 5.4 `OrderDiscount` · tham số TS: Shop + Sàn ≤ 85 %, Điểm ≤ 50 % sau voucher, 1 Điểm = 1₫ — xem [README](../README.md) |
| **Theo dõi tiến độ** | [progress_khuyen_mai_khi_mua_c6.md](progress_khuyen_mai_khi_mua_c6.md) — cùng mã task `Xxx.yy` |
| **Phụ trách** | Người làm plan — lấy theo **`git config user.name`** trên máy thực hiện (ghi vào progress ở X00.01); không ghi tên cố định |
| **Người commit** | danh tính git đã cấu hình trên máy đó — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Phụ thuộc** | C2 (`CheckoutPricer`, `CheckoutDiscountPolicy`, `CheckoutCompensation`, `ExpiredSessionSweeper`), B5 (voucher, Kho Voucher, `VoucherStatus`, `Voucher.isApplicable / calculateDiscount / consume / restore`), Điểm Genie (`CoinLedgerService` debit FEFO / credit, `UserCoin.verifyHash`). **Không** phụ thuộc C3–C5 — làm song song được; `onOrderCancelled` (C4) có thì cài luôn, chưa có thì chỉ cài `onExpired` |
| **Mục tiêu** | 4 use case ⬜ → ✅. Checkout áp được voucher shop / sàn / freeship + Điểm, gợi ý bộ mã tốt nhất, phân bổ xuống từng dòng hàng; hủy / hết hạn hoàn đúng phần; Flash Sale chạy đúng giờ, không bán vượt suất |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `1b66eae`, 2026-11-28)

| Phần | Đã có | Còn thiếu |
|---|---|---|
| **Voucher (B5)** | Quản lý shop / sàn, Kho `/me/vouchers`, `/public/vouchers`, `VoucherStatus`, `VoucherScopes` (4 bảng phạm vi), `Voucher.isApplicable / calculateDiscount / consume / restore` (entity), `VoucherUsage` UNIQUE `(voucher_id, checkout_session_id)`, `user_vouchers` | Áp mã khi mua (Picker 6.9, tính 3 tầng, phân bổ, ghi lượt dùng); `findVoucherByCode` (6.9) đang mock |
| **Checkout (C2)** | `CheckoutDiscountPolicy.apply(userId, shops, selection)` → `Discounts(shops[shopDiscount, platformDiscount, freeshipDiscount], coinDiscount)` (bản mặc định `NONE`); `CheckoutPricer` dùng chung xem trước / đặt hàng; `CheckoutCompensation.onExpired` (no-op); `order_items` có cột `shop_discount`, `platform_discount`, `prorated_discount`, `coin_discount`; `orders` có `discount_amount`, `platform_discount_amount`, `shipping_discount_amount`, `coin_used_count`, `coin_discount_amount`; bảng `order_discounts` | Cổng chưa có chỗ **ghi** (trừ lượt / Điểm trong transaction đặt hàng) và chưa trả **phân bổ theo dòng** (D3) |
| **Điểm** | `CoinLedgerService` (credit / debit theo lô FEFO, băm `coin:` HMAC), `CoinFacade` (cho order / social), `UserCoin.verifyHash` | Loại giao dịch `USED_AT_CHECKOUT` / `ORDER_REFUND` (kiểm CHECK `coin_transactions.type` — X00.04) |
| **Flash Sale** | Bảng `flash_sale_campaigns` (`vendor_id`, `title`, `starts_at`, `ends_at`, `max_products`, `status`), `flash_sale_items` (`campaign_id`, `product_id`, `flash_price`, `flash_stock`, `reserved_count`, `sold_count`, `status`, `version`) | ⚠ Không có **biến thể** (`product_variant_id`), hạn đăng ký, số SP tối đa mỗi shop, shop đăng ký, giới hạn 1 suất / người → migration (D14); toàn bộ service / API / cron |
| **Mã lỗi** | `GNE-VCH-*` (B5) | `GNE-VCH-4221` (đã có trong `ErrorCode`? kiểm — B5 thêm), `GNE-INV-4091`, `GNE-PRO-4221…4224` (API 15), **mới** `GNE-PRO-4093` (đã mua suất Flash Sale này) |
| **Web** | Checkout: ô Voucher / Điểm **đang ẩn** khi thật (C2 D2); `promotions/voucher.api.ts` `findVoucherByCode` mock; `seller-promotions/promo.api.ts` (`listFlashCampaigns`, `listEligible`, `listFlashItems`, `registerFlashItem`) mock; `admin-marketing/marketing.api.ts` (`listCampaigns`, `getCampaign`, `createCampaign`, `scheduleCampaign`, `endCampaign`, `reviewFlashItem`) mock; khu "FLASH SALE" trang chủ mock | Nối thật theo cờ (D20) |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | **`promotion → order`** (cài cổng của checkout); **`order` không import `promotion`**. Mọi thứ checkout cần (giảm giá, phân bổ, ghi lượt, hoàn, giá Flash Sale, giữ suất) đi qua **cổng trong order** | README mục 3 |
| N2 | Một **bộ tính thuần** (`StackingEngine`) dùng chung cho xem trước (6.8), Picker (6.9) và đặt hàng (6.10) — đặt hàng **tính lại từ đầu** trên dữ liệu khóa dòng, không tin số client gửi | UC-PRO-04; C2 `CheckoutPricer` |
| N3 | Trừ lượt voucher / ngân sách / Điểm / suất Flash Sale **trong transaction đặt hàng** bằng `UPDATE … WHERE còn đủ` hoặc `@Version`; hỏng điều kiện → rollback cả đơn và báo mã cụ thể | TS 10 |
| N4 | Thứ tự cố định **Shop → Sàn → Freeship → Điểm**; `grand_total ≥ 0`; giảm do sàn tài trợ **không** làm giảm tiền shop nhận (C3 D12) | UC-PRO-04 ràng buộc |
| N5 | Tiền tính bằng `long` VND trong bộ tính (làm tròn xuống từng dòng, phần dư dồn vào dòng giá trị lớn nhất — tổng phân bổ **đúng bằng** số giảm) | TS 8 |
| N6 | Hoàn khi hết hạn / hủy: hoàn **đúng phần đã phân bổ** của đơn con bị hủy; lượt voucher sàn chỉ hoàn khi **mọi** đơn con của phiên bị hủy | UC-ORD-10 ràng buộc |
| N7 | Cron Flash Sale 1 phút, `SKIP LOCKED`; mở bán kiểm lại tồn | UC-PRO-05 b4 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Cổng ghi khi đặt hàng** — `CheckoutDiscountPolicy` chỉ tính, không ghi | C2 cổng | Thêm vào cổng (sửa file của C2, ghi rõ trong PR): `default Commit commit(long sessionId, long userId, List<PlacedOrder> orders, Selection sel)` — gọi trong transaction `CheckoutService.place` **sau** khi tạo đơn: tính lại (N2) với khóa dòng voucher / Điểm, trừ lượt, ghi `voucher_usages`, `order_discounts`, `CoinTransaction`; trả số cuối để `CheckoutPricer` ghi vào đơn / dòng. Lệch với lúc xem trước → ném `CheckoutDiscountChanged` → `409 GNE-VCH-4221` `details{voucher_id, reason}` (khách bấm lại) | ☐ |
| D2 | **Đầu vào chọn mã** (API 6.8 `vouchers`) | API 6.8 | `{shop_vouchers:[{shop_id, voucher_id}], platform_voucher_id, freeship_voucher_id, use_coins, auto: true|false}` — `auto = true` (mặc định khi khách chưa chạm Picker) → server chọn bộ tốt nhất (D6) và trả `applied` để web hiện; khách bỏ chọn một mã → gửi `auto = false` + lựa chọn tay | ☐ |
| D3 | **Phân bổ xuống dòng hàng** | UC-PRO-04 b4–5; ENTITY 5.3 | `Discounts` thêm `lines: List<LineAllocation(variantId, shopDiscount, platformDiscount, freeshipShare, coinDiscount)>` (đổi record của C2 + `CheckoutPricer` ghi vào `order_items.shop_discount / platform_discount / coin_discount`, `prorated_discount = tổng`, `gross_amount = line_total − shop_discount`) — theo tỷ lệ `line_total` trong tập dòng **hợp lệ** của mã (phạm vi / loại trừ), làm tròn theo N5 | ☐ |
| D4 | **Kiểm từng mã** (UC-PRO-04 b2) | UC-PRO-04 | `VoucherEligibility.check(voucher, user, lines, now)` → `OK` hoặc lý do (`NOT_STARTED`, `EXPIRED`, `DISABLED`, `USAGE_LIMIT`, `USER_LIMIT`, `BUDGET`, `MIN_ORDER` (+ số còn thiếu), `OUT_OF_SCOPE`, `SHOP_MISMATCH`, `NOT_IN_WALLET`). Voucher sàn / freeship / shop công khai dùng được bằng mã **không cần lưu trước**; quà đổi Điểm (`COIN*`) chỉ dùng khi có trong Kho của người đó (`NOT_IN_WALLET`). Lý do có câu tiếng Việt ("Chưa đạt giá trị tối thiểu, mua thêm 35.000₫") qua khóa i18n `checkout.voucher.reason.*` | ☐ |
| D5 | **Tầng & trần** | UC-PRO-04 b3–6; TS (Shop + Sàn ≤ 85 %) | Tầng 1 Shop (≤ 1 mã / shop, trên tạm tính hàng hợp lệ của shop); tầng 2 Sàn (≤ 1, đơn tối thiểu tính trên tổng sau tầng 1, phân bổ theo shop rồi theo dòng); **trần Shop + Sàn ≤ 85 % tiền hàng** của mỗi dòng (cắt phần sàn khi vượt — ghi `details.capped`); tầng 3 Freeship (≤ 1, trừ vào tổng phí ship, phân bổ theo phí ship từng shop, không vượt phí thật); Điểm sau cùng (D9). Voucher shop loại `FREESHIP` (B5 D8) tính ở tầng 1 nhưng trừ vào phí ship của shop đó | ☐ |
| D6 | **Gợi ý bộ tốt nhất** | UC-PRO-04 b7 | Mỗi tầng lấy ≤ 5 ứng viên hợp lệ (sắp theo số giảm ước tính) → thử tổ hợp (≤ 5 × 5 × 5 với tầng shop chọn độc lập từng shop — tối ưu từng shop trước, rồi sàn × freeship) → bộ có tổng giảm lớn nhất, hòa thì ít mã sàn tài trợ hơn. Đo thời gian (mục tiêu < 50 ms với giỏ 10 shop) | ☐ |
| D7 | **Voucher Picker** (API 6.9) | API 6.9 | `POST /me/checkout/applicable-vouchers` (controller ở **promotion**, đọc dòng giỏ đã chọn qua cổng đọc của order — `CheckoutLinesReader`) → nhóm `shop[shop_id]`, `platform`, `freeship`: mỗi mã có `applicable`, `reason`, `estimated_discount`, `is_saved`, `is_best`; tham số `code` (tra mã nhập tay — thay `findVoucherByCode` mock) | ☐ |
| D8 | **Ghi lượt dùng & ngân sách** | ENTITY 4.1–4.3; N3 | Trong `commit`: `SELECT … FOR UPDATE` các voucher của bộ (thứ tự theo id — tránh deadlock) → `Voucher.consume(amount)` (tăng `used_count`, `total_discount_used`, tự tắt khi hết ngân sách — đã có) → `voucher_usages` một dòng / voucher / phiên + `order_id` của đơn đầu tiên dùng mã (voucher sàn dùng chung phiên), trạng thái "đã dùng" của Kho tính từ `voucher_usages` như B5 D16 (**không** dùng cột `user_vouchers.is_used` — đang `@Transient`); `order_discounts` mỗi đơn con một dòng / mã với `discount_amount` đã phân bổ cho đơn đó, `funded_by` theo `sponsor_type` — đồng tài trợ ghi **một** dòng `funded_by = CO_SPONSOR`, C3 tách theo `platform_share_percent` khi ghi sổ | ☐ |
| D9 | **Điểm khi mua** (UC-PRO-10) | UC-PRO-10 b1–4 | `N = min(số dư, floor(50 % × (tiền hàng − giảm voucher)))`; bật `use_coins` → phân bổ `coin_discount` theo tỷ lệ dòng (N5); `commit`: kiểm `UserCoin.verifyHash()` (lệch → `409 GNE-PRO-4225` **mới** "Điểm đang bị khóa kiểm tra" + AdminNotification), `CoinLedgerService.debit(user, N, USED_AT_CHECKOUT, sessionId)` (FEFO, khóa lạc quan `version`); ghi `orders.coin_used_count / coin_discount_amount` từng đơn con theo phần phân bổ. Sàn chịu → sổ cái `COIN_COST` khi giải ngân (C3) | ☐ |
| D10 | **Hoàn khi hết hạn / hủy** | C2 `onExpired`; C4 `onOrderCancelled`; N6 | `PromotionCompensation implements CheckoutCompensation`: `onExpired(session)` → hoàn mọi mã + Điểm của phiên; `onOrderCancelled(orderId)` → hoàn voucher shop của đơn đó (`Voucher.restore`, xóa `voucher_usages` của mã đó nếu không còn đơn nào của phiên dùng), Điểm theo `orders.coin_used_count` (`ORDER_REFUND`, **giữ hạn lô cũ** — `CoinLedgerService.creditBack(...)`), voucher sàn / freeship chỉ khi mọi đơn con của phiên `CANCELLED*` / `AUTO_CANCELLED`. Idempotent (gọi 2 lần không hoàn 2 lần): chỉ hoàn khi `voucher_usages` của mã còn tồn tại và chưa có giao dịch Điểm `ORDER_REFUND` với `reference = orderId` | ☐ |
| D11 | **Mã lỗi khi đặt hàng** | API 15 | Mã hết lượt / hết ngân sách / hết hạn lúc bấm Đặt → `409 GNE-VCH-4221` `details{voucher_id, reason}`; web tự tính lại không có mã đó (UC-PRO-04 ngoại lệ) | ☐ |
| D12 | **Chiến dịch Flash Sale của sàn** (11.15) | UC-PRO-05 b1, b3–5 | `POST /admin/flash-sales {name, starts_at, ends_at, max_items_per_shop, registration_deadline}` (`MANAGE_VOUCHER`) → `DRAFT`; `PATCH …/items/{item_id} {status: APPROVED|REJECTED, note}` chỉ khi `DRAFT`; `POST …/schedule` → `SCHEDULED` (cần ≥ 1 item `APPROVED` — `422 GNE-PRO-4224`); `POST …/end` (khi `ACTIVE`) → `ENDED`. Khung giờ ≤ 24 giờ, bắt đầu sau hạn đăng ký, không chồng khung giờ với chiến dịch sàn khác (`409`). Audit `FLASH_SALE_*` (mới) | ☐ |
| D13 | **Shop đăng ký sản phẩm** (10.7) | UC-PRO-05 b2 | `GET /seller/flash-sales?registrable=true`, `GET …/{id}/eligible-products` (sản phẩm `PUBLISHED` của shop, còn tồn), `GET …/{id}/items`, `POST …/{id}/items {variant_id, flash_price, flash_stock}`: trước hạn đăng ký (`422 GNE-PRO-4223`), ≤ `max_items_per_shop`, giá Flash **≤ 85 % giá bán hiện tại** (`GNE-PRO-4221` — TLCN chưa có lịch sử giá 30 ngày → dùng giá hiện tại và giá thấp nhất 30 ngày **không** áp được; ghi rõ ở UC), `0 < flash_stock ≤ tồn khả dụng` (`FlashSaleItem.validateAgainst`). **Chiến dịch riêng của shop** (UC-PRO-05 ràng buộc) → **ngoài phạm vi TLCN** (API không có endpoint) — ghi ở UC | ☐ |
| D14 | **Migration Flash Sale** | ENTITY 4.5–4.6 | `V30`: `flash_sale_campaigns.registration_deadline`, `max_items_per_shop`; `flash_sale_items.product_variant_id` (NOT NULL cho bản ghi mới), `vendor_id`, `review_note`, UNIQUE `(campaign_id, product_variant_id)`; bảng **`flash_sale_purchases`** (`campaign_id`, `product_variant_id`, `user_id`, `order_id`, `status` `RESERVED/SOLD/RELEASED`, UNIQUE `(campaign_id, product_variant_id, user_id) WHERE status <> 'RELEASED'`); 4 giá trị `AuditAction` | ☐ |
| D15 | **Mở / đóng đúng giờ** | UC-PRO-05 b4–5; API 12.2 (1 phút) | `FlashSaleClockJob` mỗi phút: `SCHEDULED` và `starts_at <= now` → kiểm lại tồn từng item (hạ `flash_stock` về tồn khả dụng + báo shop — UC ngoại lệ) → `activate()`; `ACTIVE` và (`ends_at <= now` hoặc mọi item hết suất) → `end()`. Khi mở: `FLASH_SALE_REMINDER` cho người theo dõi các shop có item (qua `ShopFollowFacade` — cạnh mới `promotion → social`, dùng chung với C7, đã ghi README mục 3) | ☐ |
| D16 | **Giá Flash Sale khi mua** | UC-PRO-06 b3 | Cổng mới trong order `FlashSalePricing { Map<variantId, FlashOffer(campaignItemId, price, remaining)> offersFor(variantIds, now) }` — `CheckoutPricer` dùng `unit_price = flash_price` cho **1 đơn vị** đầu (giới hạn 1 suất / người), phần còn lại giá thường; hiện ở 6.8 (`flash_sale: {ends_at, remaining}`) | ☐ |
| D17 | **Giữ suất & chống bán vượt** | UC-PRO-06 b2, b4–5; TS 14 | Cổng `FlashSaleReservations`: `reserve(sessionId, userId, offers)` trong transaction đặt hàng — `UPDATE flash_sale_items SET reserved_count = reserved_count + 1, version = version + 1 WHERE id = ? AND flash_stock - sold_count - reserved_count >= 1 AND status = 'APPROVED'` (0 dòng → `409 GNE-INV-4091`), chèn `flash_sale_purchases` `RESERVED` (trùng UNIQUE → `409 GNE-PRO-4093`); `confirm(sessionId)` khi đơn chốt (online: trong `OrderPaymentFacade.markSessionPaid` của C3 — gọi cổng; **COD: ngay khi đặt**) → `reserved → sold`; `release(sessionId)` trong `onExpired` / `onOrderCancelled` (trước khi trả tiền) | ☐ |
| D18 | **Flash Sale công khai** (5.16) | API 5.16 | `GET /public/flash-sales/current?limit=6` — chiến dịch `ACTIVE` (hoặc `SCHEDULED` gần nhất trong 24 giờ, `upcoming = true`), item + `sold_count / flash_stock`, giá gốc / giá Flash, `ends_at`; cache Redis 30 giây (API 0 bảng cache), xóa khi mở / đóng / bán | ☐ |
| D19 | **Thông báo** | UC-PRO-05 b4 | Mở bán: `FLASH_SALE_REMINDER` người theo dõi shop (tối đa 1 / người / chiến dịch); shop: kết quả duyệt item, bị hạ suất khi mở bán | ☐ |
| D20 | **Cờ web** | — | `checkout-vouchers` (Picker, nhập mã, công tắc Điểm, bảng giảm giá ở Giỏ / Thanh toán), `flash-sales` (trang chủ + nhãn trên trang sản phẩm + giá trong giỏ), `seller-flash-sales` (đăng ký), `admin-flash-sales` (quản lý chiến dịch) | ☐ |
| D21 | **Ngoài phạm vi** | — | Chiến dịch Flash Sale riêng của shop (D13), Redis Lua + hàng đợi cho tải lớn (DATN — UC-PRO-06 ràng buộc), voucher phương thức thanh toán, chia chi phí đồng tài trợ trên sổ cái (C3 làm khi giải ngân dựa `order_discounts.funded_by`) | ☐ |

---

## 3. Luồng nghiệp vụ chi tiết

### 3.1 Xem trước & Picker

```
POST /me/checkout/preview (6.8) {items, vouchers(D2), use_coins}
  └─ CheckoutPricer (order) ─► CheckoutDiscountPolicy.apply = PromotionCheckoutPolicy (promotion)
        └─ StackingEngine.solve(lines, shops, candidates, coinsBalance, auto)  [thuần, N2]
             tầng 1 Shop → tầng 2 Sàn (trần 85 %) → tầng 3 Freeship → Điểm (≤ 50 %) → lines allocation (D3)
POST /me/checkout/applicable-vouchers (6.9) {items, code?} ─► PickerService (promotion) → nhóm + reason + is_best
```

### 3.2 Đặt hàng

```
POST /me/checkout-sessions (6.10) ─► CheckoutService.place (C2, một transaction)
   ├─ FlashSalePricing.offersFor → giá dòng (D16)
   ├─ tạo đơn con + dòng
   ├─ FlashSaleReservations.reserve (D17)      → 409 GNE-INV-4091 / GNE-PRO-4093
   ├─ CheckoutDiscountPolicy.commit (D1, D8, D9) → 409 GNE-VCH-4221 / GNE-PRO-4225
   └─ COD: FlashSaleReservations.confirm ngay ; online: confirm trong OrderPaymentFacade.markSessionPaid (C3)
Hết hạn (C2 sweeper) / hủy (C4, C5) ─► PromotionCompensation.onExpired / onOrderCancelled (D10) + FlashSaleReservations.release
```

### 3.3 Flash Sale

```
Admin: POST /admin/flash-sales → DRAFT ─(shop đăng ký 10.7)─► PATCH items APPROVED ─► POST schedule → SCHEDULED
FlashSaleClockJob (1 phút): SCHEDULED → ACTIVE (kiểm tồn, báo người theo dõi) ; ACTIVE → ENDED (hết giờ / hết suất)
GET /public/flash-sales/current (cache 30 s)
```

---

## 4. Cây file sẽ tạo / sửa

```
server-spring/src/main/resources/db/migration/V30__c6_flash_sale.sql                 (mới — số tạm)
server-spring/src/main/java/com/genie/
├── common/error/ErrorCode.java                  (sửa) VCH-4221 (nếu chưa), INV-4091, PRO-4093, 4221–4225
├── common/audit/AuditAction.java                (sửa) FLASH_SALE_CREATED / SCHEDULED / ENDED / ITEM_REVIEWED
├── order/checkout/application/                  (sửa — file của C2, ghi rõ ở PR)
│     CheckoutDiscountPolicy (commit + lines) · CheckoutPricer (ghi phân bổ) · CheckoutService (gọi commit / reserve)
│     FlashSalePricing (cổng mới) · FlashSaleReservations (cổng mới) · CheckoutLinesReader (cổng đọc cho Picker) · CheckoutPorts (bản mặc định)
├── order/checkout/application/OrderPaymentFacade.java (sửa — gọi FlashSaleReservations.confirm khi trả tiền)
└── promotion/
    ├── voucher/application/  StackingEngine · VoucherEligibility · DiscountAllocator · PromotionCheckoutPolicy
    │                         PickerService · PromotionCompensation · VoucherUsageWriter
    ├── voucher/api/          CheckoutVoucherController (6.9)
    ├── coin/application/     CoinCheckoutService (D9) · CoinLedgerService (sửa — creditBack giữ hạn lô)
    └── flashsale/            domain (FlashSaleCampaign, FlashSaleItem sửa; FlashSalePurchase mới + repo)
                              application (FlashSaleAdminService, FlashSaleRegistrationService, FlashSaleClockJob,
                                           FlashSalePricingImpl, FlashSaleReservationsImpl, PublicFlashSaleService + cache)
                              api (AdminFlashSaleController, SellerFlashSaleController, PublicFlashSaleController)
server-spring/src/test/java/com/genie/promotion/…
    StackingEngineTest (bảng ca lớn) · DiscountAllocatorTest · VoucherEligibilityTest · BestComboTest (hiệu năng)
    CheckoutVoucherPreviewIT · CheckoutVoucherCommitIT · VoucherConcurrencyIT · CoinCheckoutIT · PromotionCompensationIT
    FlashSaleAdminIT · FlashSaleRegistrationIT · FlashSaleClockJobIT · FlashSalePurchaseIT · FlashSaleConcurrencyIT · PublicFlashSaleIT
web/src/features/checkout/… (Picker, công tắc Điểm, bảng giảm) · promotions/voucher.api.ts (findVoucherByCode → 6.9)
web/src/features/seller-promotions/infrastructure/promo.api.ts (phần Flash Sale) · admin-marketing/…/marketing.api.ts (Flash Sale)
web/src/features/promotions/… (khu FLASH SALE trang chủ) · product (nhãn Flash Sale) · error-codes.ts · i18n
docs/Document/… ENTITY (4.5, 4.6, 4.10 mới FlashSalePurchase, 5.3, 5.4, Phụ lục A/E) · API_SPEC (5.16, 6.8–6.10, 10.7, 11.15, 15)
                Usecase.md · CHECKLIST · BAO_CAO · xlsx
docs/api-tests/checkout-promotions.http · flash-sales.http · docs/kiem-thu/kich-ban-2026-11-28-c6.md
```

---

## 5. Bảng ánh xạ Use case → API → Dữ liệu

| Bước UC | Endpoint / tác vụ | Bảng | Quyền |
|---|---|---|---|
| UC-PRO-04 b1–8 | `POST /me/checkout/preview`, `POST /me/checkout/applicable-vouchers` | `vouchers`, 4 bảng phạm vi, `user_vouchers`, `voucher_usages` (đọc) | người mua |
| UC-PRO-04 ghi + ngoại lệ | `POST /me/checkout-sessions` (commit), hết hạn / hủy (compensation) | `vouchers`, `voucher_usages`, `order_discounts`, `order_items`, `orders` | người mua / nội bộ |
| UC-PRO-10 | `use_coins` của 6.8 / 6.10 | `user_coins`, `coin_transactions`, `orders`, `order_items` | người mua |
| UC-PRO-05 | `GET/POST /admin/flash-sales…`, `PATCH …/items/{id}`, `GET/POST /seller/flash-sales…`, `FlashSaleClockJob` | `flash_sale_campaigns`, `flash_sale_items`, `system_logs`, `notifications` | `MANAGE_VOUCHER` / VENDOR `VERIFIED` |
| UC-PRO-06 | 6.8 (giá Flash), 6.10 (giữ suất), thanh toán / hết hạn | `flash_sale_items`, `flash_sale_purchases`, `order_items` | người mua |
| UC-PRO-05 b4 hiển thị | `GET /public/flash-sales/current` | như trên | công khai |

**Mã lỗi:** `GNE-VCH-4221` (409/422 mã không còn hiệu lực lúc đặt — theo API 15), `GNE-INV-4091` (409 hết suất), `GNE-PRO-4221` (giá / suất không hợp lệ), `GNE-PRO-4223` (quá số SP / quá hạn đăng ký), `GNE-PRO-4224` (lên lịch khi chưa có SP duyệt); **mới:** `GNE-PRO-4093` (409 đã mua suất này), `GNE-PRO-4225` (409 Điểm đang bị khóa kiểm tra). Dùng lại `GNE-PRO-4222` (không đủ Điểm).

---

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — `ModuleBoundaryTest` (**`order` không import `promotion`**), C2 IT (`CheckoutPreviewIT`, `CheckoutSessionIT`, `CheckoutConcurrencyIT`) **vẫn xanh** sau khi đổi cổng.
2. Swagger: summary `UC-PRO-04/05/06/10`.
3. `docs/api-tests/checkout-promotions.http`: 3 tầng + Điểm cho giỏ 2 shop; mã hết lượt giữa xem trước và đặt → `4221`.
4. Đụng `web/`: `npm run verify`.
5. Tick progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Xxx.yy` = task; `Xxx.Cn` = task commit.

### X00 — Chuẩn bị

- **[X00.01]** Chạy `git config user.name` / `user.email` — ghi vào dòng *Phụ trách* của progress; kiểm B5 + Điểm Genie trên `develop`; C4 đã có `onOrderCancelled` chưa (chưa → chỉ cài `onExpired`, ghi *Ghi chú*).
- **[X00.02]** Chốt D1–D21 vào progress.
- **[X00.03]** Tạo nhánh `feature/c6-voucher-stacking` từ `develop`.
- **[X00.04]** Đọc `CheckoutPricer`, `CheckoutService.place`, `CheckoutView` (C2) — ghi lại đúng chỗ chèn `commit` / `reserve` / giá Flash; CHECK `coin_transactions.type` (có `USED_AT_CHECKOUT`, `ORDER_REFUND`?), `flash_sale_*.status`; `Voucher.calculateDiscount / consume / restore` hiện làm gì.
- **[X00.05]** Đọc màn mock Giỏ / Thanh toán (Picker 3 nhóm, nhập mã, lý do không áp được, bảng "Voucher Sàn phân bổ" theo shop, công tắc Điểm), khu FLASH SALE trang chủ, nhãn Flash trên trang sản phẩm, Seller › Khuyến mãi › Flash Sale, Admin › Flash Sale.

### X01 — Tài liệu trước

- **[X01.01]** ENTITY 4.5–4.6 (cột mới D14, `validateAgainst` theo biến thể), **4.10 mới `FlashSalePurchase`**, 5.3 (công thức phân bổ D3 + làm tròn N5), 5.4 (`funded_by`, một dòng / mã / đơn con — D8), 4.8 (`USED_AT_CHECKOUT`, `ORDER_REFUND` giữ hạn lô), Phụ lục A/E.
- **[X01.02]** API_SPEC — 6.8 (`vouchers` D2, `applied`, `capped`, `flash_sale`), 6.9 (D7, `code`), 6.10 (D11), 5.16 (D18), 10.7 (D13, giá ≤ 85 % giá hiện tại), 11.15 (D12, chồng khung giờ), 15 (`GNE-PRO-4093`, `4225`), lịch sử phiên bản.
- **[X01.03]** Usecase.md — UC-PRO-04 (trần 85 %, quà Điểm phải có trong Kho — D4/D5), UC-PRO-05 (chiến dịch riêng của shop ngoài phạm vi, giá so với giá hiện tại — D13), UC-PRO-06 (COD chốt suất ngay khi đặt — D17).
- **[X01.C1]** COMMIT — `docs(promotion): specify voucher stacking, coins at checkout and flash sale purchase rules`

### X02 — Bộ tính 3 tầng (thuần)

- **[X02.01]** `VoucherEligibility` (D4) — đầu vào là dữ liệu đã nạp (không gọi CSDL), trả lý do + số còn thiếu.
- **[X02.02]** `DiscountAllocator` (D3, N5) — phân bổ theo tỷ lệ, dồn phần dư, không âm.
- **[X02.03]** `StackingEngine.solve` (D5, D6, D9) — tầng 1 → 2 (trần 85 %) → 3 → Điểm; chế độ `auto` (bộ tốt nhất) và tay.
- **[X02.04]** Test — `VoucherEligibilityTest` (mỗi lý do một ca biên), `DiscountAllocatorTest` (tổng đúng bằng số giảm với 1, 3, 7 dòng lẻ tiền), `StackingEngineTest` (≥ 25 ca: 1 shop / 3 shop, % có trần, tiền cố định, freeship vượt phí ship, sàn vượt 85 %, Điểm 50 %, `grand_total` không âm, phạm vi ngành / loại trừ sản phẩm, CO_SPONSOR), `BestComboTest` (giỏ 10 shop × 5 ứng viên < 50 ms).
- **[X02.C1]** COMMIT — `feat(promotion): add three-tier voucher stacking engine with coin cap and line allocation`

### X03 — Áp mã ở xem trước & Picker (UC-PRO-04)

- **[X03.01]** Sửa cổng C2: `Discounts.lines` (D3), `Selection` theo D2 + `CheckoutPricer` ghi phân bổ; bản mặc định `NONE` vẫn chạy (C2 IT xanh).
- **[X03.02]** `PromotionCheckoutPolicy implements CheckoutDiscountPolicy` — nạp ứng viên (Kho + mã công khai + mã nhập) theo lô, gọi engine.
- **[X03.03]** Cổng đọc `CheckoutLinesReader` (order cài) + `PickerService` + `CheckoutVoucherController` `POST /me/checkout/applicable-vouchers` (D7).
- **[X03.04]** Test — `CheckoutVoucherPreviewIT` (6.8 trả `applied` + `lines` cộng đúng; mã shop khác bị `SHOP_MISMATCH`; quà Điểm chưa có trong Kho không dùng được; `auto` chọn đúng bộ tốt nhất), `PickerIT` (3 nhóm, `is_best`, `reason` tiếng Việt, tra `code` thường / hoa).
- **[X03.C1]** COMMIT — `feat(order): carry voucher selections and line allocations through checkout pricing`
- **[X03.C2]** COMMIT — `feat(promotion): apply vouchers at checkout preview and serve the voucher picker`

### X04 — Ghi lượt dùng khi đặt & hoàn (UC-PRO-04)

- **[X04.01]** `commit` (D1, D8) — khóa voucher theo id tăng dần, `consume`, `VoucherUsageWriter` (`voucher_usages`, `order_discounts`).
- **[X04.02]** `PromotionCompensation` (D10) — `onExpired`, `onOrderCancelled` (nếu cổng C4 đã có), idempotent.
- **[X04.03]** Ánh xạ lỗi D11 trong `GlobalExceptionHandler` / service đặt hàng.
- **[X04.04]** Test — `CheckoutVoucherCommitIT` (đặt 2 shop + voucher sàn → `used_count + 1` một lần, 2 dòng `order_discounts`, `order_items` cộng đúng), **`VoucherConcurrencyIT`** (voucher còn 1 lượt, 10 khách đặt song song → đúng 1 đơn có mã, 9 nhận `4221` hoặc đặt không mã theo lựa chọn), `PromotionCompensationIT` (hết hạn → hoàn lượt; hủy 1 / 2 đơn con → voucher shop của đơn đó hoàn, voucher sàn **chưa**; hủy nốt → voucher sàn hoàn; gọi 2 lần không hoàn 2 lần).
- **[X04.C1]** COMMIT — `feat(promotion): consume vouchers when orders are placed and restore them on expiry or cancellation`

### X05 — Điểm Genie khi mua (UC-PRO-10) — nhánh `feature/c6-coins-at-checkout`

- **[X05.01]** `CoinCheckoutService` (D9) — tính `N`, `commit` (verifyHash, debit FEFO), ghi `orders.coin_*`.
- **[X05.02]** `CoinLedgerService.creditBack(user, amount, ORDER_REFUND, orderId)` giữ hạn lô gốc (đọc lại các lô đã trừ theo `reference`).
- **[X05.03]** Test — `CoinCheckoutIT` (số dư 900, tiền hàng sau voucher 1.000 → dùng 500; bật / tắt; hash lệch → `4225` + AdminNotification; hai phiên song song cùng Điểm → không âm), hoàn khi hủy giữ hạn lô cũ (kiểm `expires_at`).
- **[X05.C1]** COMMIT — `feat(promotion): spend genie coins at checkout and refund them with original expiry`

### X06 — Flash Sale: chiến dịch & đăng ký (UC-PRO-05) — nhánh `feature/c6-flash-sale`

- **[X06.01]** `V30__c6_flash_sale.sql` (D14) + entity sửa / mới + `ErrorCode` + `AuditAction`.
- **[X06.02]** `FlashSaleAdminService` + `AdminFlashSaleController` (D12) — tạo, xem, duyệt item, lên lịch, kết thúc sớm; audit.
- **[X06.03]** `FlashSaleRegistrationService` + `SellerFlashSaleController` (D13) — 4 endpoint 10.7.
- **[X06.04]** `FlashSaleClockJob` (D15) + thông báo (D19) + `PublicFlashSaleService` / `PublicFlashSaleController` + cache (D18).
- **[X06.05]** Test — `FlashSaleAdminIT` (chồng khung giờ → 409; lên lịch không có item duyệt → `4224`; audit), `FlashSaleRegistrationIT` (quá hạn → `4223`; giá 90 % → `4221`; suất > tồn → `4221`; vượt số SP / shop → `4223`; shop chưa `VERIFIED` → 403), `FlashSaleClockJobIT` (tới giờ → `ACTIVE`, tồn giảm → hạ suất + báo shop; hết giờ / hết suất → `ENDED`; người theo dõi nhận 1 nhắc), `PublicFlashSaleIT` (cache 30 giây, xóa khi đổi).
- **[X06.C1]** COMMIT — `feat(db): add flash sale registration columns and purchase limits`
- **[X06.C2]** COMMIT — `feat(promotion): run flash sale campaigns with seller registration and timed activation`

### X07 — Mua Flash Sale chống bán vượt suất (UC-PRO-06)

- **[X07.01]** Cổng `FlashSalePricing`, `FlashSaleReservations` (order) + bản mặc định (không có Flash Sale) + cài ở promotion (D16, D17).
- **[X07.02]** `CheckoutPricer` / `CheckoutService` dùng giá + giữ suất; `OrderPaymentFacade` gọi `confirm` (online); COD `confirm` ngay; `onExpired` / `onOrderCancelled` gọi `release`.
- **[X07.03]** Test — `FlashSalePurchaseIT` (giá Flash cho 1 đơn vị, đơn vị thứ 2 giá thường; mua lần 2 cùng chiến dịch → `4093`; hết hạn thanh toán → suất trả lại; COD → `sold_count` tăng ngay), **`FlashSaleConcurrencyIT`** (50 suất, 200 khách đặt song song → đúng 50 đơn có giá Flash, `reserved + sold ≤ flash_stock` mọi lúc — kịch bản tiền TS 14).
- **[X07.C1]** COMMIT — `feat(order): add flash sale pricing and reservation ports to checkout`
- **[X07.C2]** COMMIT — `feat(promotion): reserve flash sale slots atomically without overselling`

### X08 — Nối web Giỏ / Thanh toán (cờ `checkout-vouchers`)

- **[X08.01]** Checkout API — gửi `vouchers` / `use_coins` theo D2; đọc `applied`, `lines`, `capped`; `findVoucherByCode` → 6.9 `code`.
- **[X08.02]** Picker 3 nhóm (đánh dấu "Tốt nhất", lý do không áp được, nhập mã), bảng giảm (Shop / Sàn / Freeship / Điểm, "Voucher Sàn phân bổ" theo shop), công tắc Điểm ("Dùng N Điểm — giảm N₫"); lỗi `4221` khi đặt → tự tính lại không mã đó + thông báo. Kiểm 9 theme, 360 px.
- **[X08.03]** vi / en (`checkout.voucher.reason.*`), `error-codes.ts`; test mapper; `npm run verify`.
- **[X08.C1]** COMMIT — `feat(web): connect voucher picker and coin usage at checkout to the real api`

### X09 — Nối web Flash Sale (cờ `flash-sales`, `seller-flash-sales`, `admin-flash-sales`)

- **[X09.01]** Trang chủ khu FLASH SALE (đếm ngược, thanh "Đã bán x/y"), nhãn + giá Flash ở trang sản phẩm / giỏ (`flash_sale` của 6.8).
- **[X09.02]** Seller › Khuyến mãi › Flash Sale (4 hàm `promo.api.ts`) — lỗi `4221` / `4223` tại ô.
- **[X09.03]** Admin › Flash Sale (6 hàm `marketing.api.ts`) — tạo, duyệt item, lên lịch, kết thúc.
- **[X09.C1]** COMMIT — `feat(web): connect flash sale storefront, seller registration and admin campaigns to the real api`

### X10 — Dữ liệu demo, kiểm thử & chạy thật

- **[X10.01]** Seed — 1 chiến dịch Flash Sale `SCHEDULED` (mở sau 10 phút khi nạp seed — tính theo `now()`), item của TechZone đã duyệt; voucher shop / sàn sẵn có (B5) đủ để thử 3 tầng.
- **[X10.02]** `docs/api-tests/checkout-promotions.http` + `flash-sales.http`.
- **[X10.03]** `docs/kiem-thu/kich-ban-2026-11-28-c6.md` — TC-C6-01…: giỏ 2 shop tự chọn bộ tốt nhất; đổi tay; mã không đủ điều kiện có lý do; trần 85 %; freeship không vượt phí ship; Điểm 50 %; đặt khi mã vừa hết lượt; hủy / hết hạn hoàn đúng; Admin tạo Flash Sale → shop đăng ký → duyệt → mở đúng giờ → mua → hết suất; 2 tab cùng mua suất cuối; 9 theme; 360 px.
- **[X10.04]** Chạy thật — web build `PUBLIC_REAL_API=…,cart,checkout,vouchers,checkout-vouchers,coins,flash-sales,seller-flash-sales,admin-flash-sales` + backend `local`; Chromium (Playwright) với khách + chủ shop + Finance Officer; mỗi lỗi một commit `fix(…)`.
- **[X10.C1]** COMMIT — `feat(db): seed a scheduled flash sale for demos`
- **[X10.C2]** COMMIT — `docs(promotion): add checkout promotion api tests and real-run scenarios`

### X11 — Hoàn thiện use case & tick tiến độ

- **[X11.01]** OpenAPI — summary `UC-PRO-04/05/06/10`.
- **[X11.02]** `docs/web-backend-handoff.md` — `checkout-vouchers`, `flash-sales`, `seller-flash-sales`, `admin-flash-sales` đã nối; bỏ ghi chú "Flash Sale còn mock (C6)" của B5.
- **[X11.03]** Usecase.md — *Hiện trạng* 4 UC.
- **[X11.04]** `CHECKLIST_TIEN_DO_UC.md` — tick 5 lớp 4 UC, Trạng thái, **cột Phụ trách = tên ở X00.01**, số mục 1 + phân hệ 07, dòng mục 5 *Nhật ký*.
- **[X11.05]** `BAO_CAO_UC_DOC_LAP.md` — mục C6 + dòng *Phụ trách*.
- **[X11.06]** `Quản Lý UseCase.xlsx` — 4 UC, cột AA.
- **[X11.07]** README ngày (mục 7) — tick dòng C6; README mục 3 thêm cạnh `promotion → social` (D15) nếu C7 chưa thêm.
- **[X11.C1]** COMMIT — `docs(promotion): record voucher stacking, coins at checkout and flash sale as implemented`
- **[X11.08]** Mở 3 PR xếp chồng vào `develop` (`c6-voucher-stacking` → `c6-coins-at-checkout` → `c6-flash-sale`); PR đầu nhờ người làm C2 duyệt thay đổi cổng checkout.

---

## 8. Ngoài phạm vi

- Chiến dịch Flash Sale riêng của shop; Redis Lua + hàng đợi; voucher phương thức thanh toán (DATN).
- Ghi sổ cái chi phí voucher sàn / freeship / Điểm (C3 làm khi giải ngân, đọc `order_discounts.funded_by`, `orders.coin_discount_amount`).

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Đổi cổng C2 làm hỏng checkout đang chạy | Bản mặc định giữ hành vi cũ; C2 IT bắt buộc xanh (cổng kiểm tra 1) |
| Lệch tiền do làm tròn phân bổ | N5 + test tổng đúng bằng số giảm |
| Bán vượt suất / dùng vượt lượt voucher khi đặt song song | UPDATE có điều kiện + UNIQUE + `FOR UPDATE` theo thứ tự id; 2 IT đồng thời |
| Tổ hợp mã bùng nổ | ≤ 5 ứng viên / tầng, tối ưu từng shop độc lập; test hiệu năng |
| Hoàn hai lần khi cả sweeper và hủy cùng chạy | Kiểm tồn tại `voucher_usages` / giao dịch Điểm theo `reference` trước khi hoàn (D10) |
