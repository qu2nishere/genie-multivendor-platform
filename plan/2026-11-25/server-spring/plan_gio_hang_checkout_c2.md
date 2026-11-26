# PLAN — Chuỗi C2 Giỏ → Checkout (UC-ORD-01 · UC-ORD-04 · UC-ORD-06 · UC-ORD-07 · UC-ORD-08)

| Mục | Nội dung |
|---|---|
| **Ngày lập** | 2026-11-25 |
| **Phụ trách** | _(chưa phân công — điền tên người nhận, rồi sửa dòng C2 ở `BAO_CAO_UC_DOC_LAP.md`, cột Phụ trách ở `CHECKLIST_TIEN_DO_UC.md` và `Quản Lý UseCase.xlsx`)_ |
| **Tiến độ** | [progress_gio_hang_checkout_c2.md](progress_gio_hang_checkout_c2.md) |
| **Phối hợp với C1** | Chạy **song song, độc lập** với chuỗi C1 Đăng bán ([plan C1](../../2026-11-26/c1.md)). Mọi điểm chạm — số migration, sở hữu file, luật ghi `product_variants`, cổng `ProductOrderGuard`, luật "mua được", cân nặng / freeship — chốt ở **mục 8** cuối file (cùng nội dung với mục 10 của plan C1 — bắt buộc đọc trước O00; đổi gì phải sửa cả hai plan và báo bên C1). C2 test trên sản phẩm `PUBLISHED` của seed + `TestDataBuilder`, **không chờ** C1 |
| **Mục tiêu** | 5 UC của chuỗi C2 từ ⬜ → ✅ (đủ 5 lớp GD / BE / Test / Nối / Chạy; UC-ORD-08 không có GD / Nối). Tổng quan dự kiến: Đã triển khai 24 → **29** / 87 (số CHECKLIST 2026-11-25; đếm lại từ các dòng UC lúc đóng plan — mục 8.2.3) |
| **Nguồn** | `BAO_CAO_UC_DOC_LAP.md` mục 6 (C2) · `Usecase.md` UC-ORD-01 (dòng 1004), 04 (1041), 06 (1074), 07 (1100), 08 (1138) · `API_SPEC.md` 5 (`GET /public/variants`, dòng 1128), 6.1–6.8, 6.10, 6.11, 15 · `LLD.md` 6.1 (SessionStatus), 7.2 (SD-02), 9.1 (giữ chỗ tồn kho), 10 (Cron) · `ENTITY.md` 3.1–3.2, 5.1–5.5, 7.3 |
| **Thứ tự ưu tiên khi mâu thuẫn** | TECH_STACK mục 12 → Usecase.md / ENTITY.md → Document_Vi → implement (CLAUDE.md mục 8) |
| **Người commit** | danh tính git đã cấu hình — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI (CLAUDE.md mục 4) |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `0e34dce`, 2026-11-25 — cập nhật @ `609a0c4`, 2026-11-26: B4 / UC-VND-05 ✅, có plan C1)

| Phần | Đã có | Còn thiếu |
|---|---|---|
| **Entity / bảng** | `Cart`, `CartItem` (`price_snapshot`, `added_at`), `CheckoutSession`, `Order` (304 dòng, có máy trạng thái, `order_code` UNIQUE), `OrderItem`, `OrderDiscount`, `StockReservation`; enum `SessionStatus` (5), `OrderStatus` (19), `StockReservationStatus`; migration `V3__cart`, `V5__order` + **`V25` chung với C1** (2026-11-26: `uk_cart_items_cart_variant`, `idx_checkout_sessions_open_expires`; cột hạn phiên tên `expires_at`). `ProductVariant.reserve / release / confirm` đã có | Repository rỗng (chỉ khai báo) |
| **Service / controller `order`** | Khung 7 dòng có `// TODO`: `CartController`, `CartService`, `CartMergeService`, `CheckoutController`, `CheckoutService`, `CheckoutFacade`, `ShippingFeeQuoter`, `ReservationService`, `ExpiredSessionSweeper` | Toàn bộ nghiệp vụ |
| **Dùng chung** | `ErrorCode` đã có `GNE-INV-4090/4091`, `GNE-ORD-4031/4092`, `GNE-USR-4091` (kiểm lại 2026-11-26 — `GNE-ORD-4093/4094` **chưa** có trong code, chỉ có ở API 15); `common/idempotency/IdempotencyFilter` + `IdempotencyStore`; `AuditAction.ORDER_STATUS_CHANGED`; `JsonCache`; cổng `auth.deletion.application.AccountDeletionBlocker` (chờ module order cài) | Thêm `GNE-INV-4093`, `GNE-INV-4094`, `GNE-ORD-4093`, `GNE-ORD-4094` (đã có ở API 15 — O01.04; chỉ thêm trong khối `// ===== Đơn hàng / tồn kho`, mục 8.2.1, 8) |
| **Vận chuyển** | `VendorShippingConfig` + `VendorShippingSettingsAdapter` (ngưỡng freeship, hãng bật); UC-VND-05 ✅ (2026-11-25): kho lấy hàng **2 cấp** `pickup_province_id` + `pickup_ward_code` (`pickup_district_id = NULL` với kho mới), `VendorProfile.isPickupConfigured()`, `ghn_shop_id` (đăng ký GHN đang giả lập `GHN_PICKUP_MODE=stub`); seed `R__seed_demo_30_shops.sql` **đã có** `pickup_*` + `ghn_shop_id` cho shop demo; `auth/address/infra/GhnRegionDirectory` (gọi GHN danh mục hành chính); `.env.example` có `GHN_TOKEN`, `GHN_SHOP_ID` (đang tắt) | Client tính phí GHN, bảng phí dự phòng, cache |
| **Địa chỉ** | UC-AUTH-09 ✅ — **địa giới 2 cấp**: `provinceId` + `wardCode`; `districtId` chỉ còn ở địa chỉ cũ (NULL với địa chỉ mới) | ⚠ GHN fee v2 cần `to_district_id` — xem D4 |
| **Web** | `features/cart` (`fetchCart`, `addCartItem`, `updateCartItem`, `removeCartItems`, `addCartItemsBatch`, `mergeCart`, `resolveGuestItems`, `getVariantChoice`), `features/checkout` (`createCheckout`, `getCheckoutSession`, `retryPayment`, `settleMockPayment`, `getShipPolicies`) — tất cả chạy trên mock; handoff dòng 44 (`/public/shops/shipping-policies` không có trong spec), 257–299 | Nối API thật qua cờ `PUBLIC_REAL_API` (`cart`, `checkout`) |

**Phụ thuộc theo báo cáo (BAO_CAO mục 6 C2):** *Tính phí vận chuyển* cần *Sổ địa chỉ* (✅) + *Cấu hình kho lấy hàng* UC-VND-05 (✅ 2026-11-25). *Tạo phiên Checkout* «include» *Xem trước đơn hàng* «include» *Tính phí vận chuyển* → làm đúng thứ tự **ORD-01 → ORD-06 → ORD-04 → ORD-07 → ORD-08**.

**Ngoài phạm vi (ghi lại để không quên):**
- Áp voucher / Điểm khi mua (UC-PRO-04, PRO-10), `POST /me/checkout/applicable-vouchers` (6.9), Flash Sale (PRO-06) → **C6**. C2 chỉ để sẵn cổng (D2).
- Khởi tạo thanh toán VNPay / VietQR thật, IPN, ký quỹ (6.12, UC-PAY-*) → **C3**. C2 dùng cổng thanh toán giả lập (D3).
- Hủy nhóm đơn do khách (6.13, UC-ORD-10), danh sách / chi tiết đơn mua (UC-ORD-09) → **C5**.
- Điều phối đa hãng (UC-LOG-15) → **C4**. C2 chỉ tính phí GHN.
- Cấu hình kho lấy hàng (UC-VND-05) → đã xong ở B4. C2 chỉ **đọc** `vendor_profiles.pickup_*` qua `ShopFacade` (D5).
- Đăng bán / sửa / ẩn / khóa / xóa / duyệt sản phẩm (UC-CAT-09…13, 17) → **C1** (chạy song song). C2 chỉ **đọc** trạng thái sản phẩm theo luật "mua được" (mục 8.5) và cài cổng `ProductOrderGuard` của C1 (D14).

---

## 1. Nguyên tắc bắt buộc

1. **Tài liệu sửa trước, code sau** (TECH_STACK 7.3): thay đổi hành vi / hợp đồng API thì sửa `Usecase.md` / `API_SPEC.md` / `LLD.md` trước, commit `docs(order): …` riêng ở đầu mỗi nhánh.
2. **Ranh giới module** (TS 4.4, `ModuleBoundaryTest`): `order` chỉ gọi module khác qua `*Facade` / cổng ở tầng `application` (`ProductFacade`/`VariantFacade` của `catalog`, `ShopFacade` của `vendor`, `AddressFacade` của `auth`, `ShopShippingSettings`); không import `domain` / `infra` module khác; không tạo vòng phụ thuộc. Cổng mà module **sau** (promotion, payment) sẽ cài thì đặt ở `order.*.application` với bản mặc định `@ConditionalOnMissingBean` (giống `ShopVoucherAvailability`).
3. **Tiền** dùng `BigDecimal` (làm tròn đồng, `HALF_UP`); **server luôn tính lại giá**, không tin giá client.
4. **Giữ chỗ tồn kho** đúng LLD 9.1: `UPDATE … WHERE stock_count - reserved_count >= :qty` (0 dòng → `GNE-INV-4090`), không đọc-rồi-ghi. Sắp xếp `variant_id` tăng dần trước khi giữ chỗ để tránh deadlock. Mọi câu ghi `product_variants` của C2 **tăng `version`** và theo đúng mục 8.3 (giữ chỗ lọc `products.status = 'PUBLISHED'` + `deleted_at IS NULL`; không dùng `ProductVariant.reserve/commitSale` trên entity được quản lý).
5. **Cron** (TS 10.3): `@Scheduled` + `FOR UPDATE SKIP LOCKED`, theo lô, mỗi phiên một transaction, lỗi một phiên không dừng phiên khác, log kết quả (mẫu: `SuspensionExpiryJob`, `KycSupplementExpiryJob`).
6. **Sự kiện / email / cache chạy sau commit** (`@TransactionalEventListener(AFTER_COMMIT)`).
7. **Không đổi tên bảng / cột / enum / mã UC** (NFR-12). Migration: `V25__listing_and_checkout_schema.sql` **chung với C1 đã tạo** (2026-11-26) — có sẵn 2 index của C2 (D15); cần thêm → `V26+` theo thứ tự merge (mục 8.1). ENTITY.md sửa trước.
8. **Mã lỗi** chỉ dùng mã có trong API_SPEC 15; cần mã mới thì thêm vào API_SPEC 15 + `ErrorCode` + `web/src/shared/api/error-codes.ts` + khóa `errors.GNE-*` (vi, en) cùng một commit.
9. **Web** (CLAUDE.md mục 5): sửa theo 4 tầng, giữ nhánh mock chạy được; chuỗi qua `t()`; màn đổi giao diện kiểm 9 theme.
10. **Cổng kiểm tra trước mỗi COMMIT**: `cd server-spring && ./mvnw verify` (JDK 21); đụng `web/` thì `cd web && npm run verify`.
11. **Chỉ tick Chạy khi người dùng tự thao tác trên giao diện với hệ thống thật** (CLAUDE.md mục 4.4). Claude gọi API thì ghi "API đúng".
12. **Commit / PR** theo CLAUDE.md mục 4: `<type>(<scope>): <subject>` tiếng Anh, chữ thường; scope `order` (backend), `shipping`, `web`, `db`, `docs`; `git add` đúng từng file; chỉ đưa lệnh commit khi người dùng yêu cầu.
13. **Ranh giới với C1** (mục 8): chỉ sửa file C2 sở hữu (mục 8.2) — **không** sửa `Product`, `ProductVariant`, `ProductVariantRepository`, `VendorShopPorts`; truy vấn catalog viết trong `VariantFacade` (file mới của C2, JdbcTemplate); không sửa file seed có sẵn; hàm dựng test đặt ở `support/OrderTestData`.

---

## 2. Quyết định cần chốt trước khi làm

| Mã | Câu hỏi | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Nhánh & thứ tự PR** | CLAUDE.md mục 4; PR ≤ ~400 dòng | 5 nhánh **xếp chồng** (cùng sửa file progress), tạo từ `develop` mới nhất: `feature/c2-cart` (ORD-01) → `feature/c2-shipping-fee` (ORD-06) → `feature/c2-checkout-preview` (ORD-04) → `feature/c2-checkout-session` (ORD-07) → `feature/c2-order-sweeper` (ORD-08). Mỗi nhánh tạo từ nhánh trước; PR vào `develop` đúng thứ tự, gộp xong nhánh trước thì `git rebase develop` nhánh sau. Nhánh nào vượt ~400 dòng thì tách PR backend / web | ☐ |
| D2 | **Voucher & Điểm trong C2** | 6.8 trả `vouchers_applied`, `best_selection`, `coins`; UC-PRO-04/10 thuộc C6 | C2 **không áp giảm giá**: cổng `order.checkout.application.CheckoutDiscountPolicy` bản mặc định trả 0 → `vouchers_applied = null`, `best_selection = null`, `coins = null`, các khoản giảm = 0. Request vẫn nhận `vouchers`, `use_coins` (bỏ qua). Web khi `PUBLIC_REAL_API` có `checkout` mà chưa có `voucher` thì ẩn ô Voucher / Điểm. C6 cài cổng này | ☐ |
| D3 | **Thanh toán trong C2** (C3 chưa có) | 6.10 khối `payment`; LLD 6.1; `OrderStatus.PENDING_PAYMENT` = "COD: chờ shop xác nhận" | **COD làm trọn**: trong cùng transaction phiên → `COMPLETED`, giữ chỗ → `CONFIRMED` (`stock_count -= q`, `reserved_count -= q`), đơn con giữ `PENDING_PAYMENT` (chờ shop xác nhận — C4). **VNPAY / VIETQR**: phiên → `PAYMENT_PENDING`, khối `payment` lấy từ cổng `order.checkout.application.PaymentInitiator`; bản mặc định (`PAYMENT_MODE=stub`) trả `{ method, payment_url: null }` / `vietqr: null` → đủ để kiểm cron ORD-08 bằng đơn online bỏ dở. C3 cài cổng thật | ☐ |
| D4 | **GHN với địa giới 2 cấp** — fee v2 cần `to_district_id`, địa chỉ mới không có `districtId` | UC-ORD-06 bước 3; AUTH D25 (2 cấp, GHN v3) | **Spike O05.01** trước khi code: kiểm API phí GHN nhận `to_ward_code` / `from_ward_code` không cần quận (bản v3 / tham số mới). Lưu ý: từ 2026-11-25 **cả** sổ địa chỉ lẫn kho lấy hàng (UC-VND-05) đều 2 cấp (`district_id = NULL`) nên gần như mọi tuyến không có quận — kết quả spike quyết định gần toàn bộ phí. Nếu được → dùng. Nếu không → dùng `districtId` khi địa chỉ có, còn lại tính **bảng phí dự phòng** (cùng tỉnh 15.000đ / khác tỉnh 30.000đ) + cờ `is_estimated_fee = true`. Ghi kết quả spike vào progress mục 7 | ☐ |
| D5 | **Shop chưa cấu hình kho lấy hàng** (UC-VND-05 ✅ 2026-11-25) | UC-ORD-06 tiền điều kiện; mục 8.6 | (a) **Không sửa seed** — `R__seed_demo_30_shops.sql` đã có `pickup_*` + `ghn_shop_id` cho mọi shop demo (O05.05 chỉ kiểm). (b) Từ C1, sản phẩm mới không gửi duyệt được khi shop chưa có kho (`422 GNE-CAT-4224`) ⇒ chỉ còn sản phẩm cũ của shop chưa có kho → **phí dự phòng liên tỉnh 30.000đ**, `is_estimated_fee = true` (không chặn mua, không thêm enum). Đọc kho qua `ShopFacade` (C2 thêm phương thức nếu thiếu — C2 sở hữu file này) | ☐ |
| D6 | **Chế độ GHN** | mẫu `AI_WORKER_MODE=stub\|http` | `GHN_MODE=stub\|http` (mặc định `stub` ở profile local / test). `http`: `RestClient` timeout 3 giây, gói "Chuẩn", cache Redis `ship:fee:{sha256(tham số)}` 10 phút (`JsonCache`; Redis lỗi → gọi thẳng); lỗi / timeout → bảng dự phòng. GHN báo không phục vụ tuyến → `unavailable_reason = "ROUTE_NOT_SUPPORTED"`? → **cần chốt**: thêm giá trị mới vào API 6.1 hay dùng `PRODUCT_UNAVAILABLE`. Đề xuất thêm `SHIPPING_UNAVAILABLE` vào API 6.8 (chỉ ở preview, không phải enum CSDL) | ☐ |
| D7 | **Khối lượng / freeship** | `Product.weight` NULLABLE; UC-ORD-06 bước 5; `Product.shippingPayer`; mục 8.6 | `products.weight` là **gram**; C1 bắt buộc cân nặng khi gửi duyệt nên NULL chỉ còn ở dữ liệu cũ → 500 g / sản phẩm (`genie.shipping.default-weight-grams`). Kích thước NULL → bỏ qua. Ngưỡng freeship **chỉ ở cấp shop** (`VendorShippingConfig.freeshipThreshold` qua `VendorShippingSettingsAdapter`) — C1 đã bỏ `free_shipping_threshold` theo sản phẩm. Tạm tính shop ≥ ngưỡng hoặc mọi sản phẩm `shippingPayer = SELLER` → `shipping_fee` khách trả = 0, `is_free_shipping_by_shop = true` (ghi trợ giá vào ký quỹ để C3). Không dùng `products.ships_from` để tính phí | ☐ |
| D8 | **Giỏ khách vãng lai & gộp giỏ** | UC-ORD-01 luồng thay thế; API 6.7 chỉ trả `{merged, cart_item_count}` nhưng Usecase bước 4 yêu cầu trả dòng bị điều chỉnh | Giỏ tạm giữ ở `localStorage` (web đã có); `GET /public/variants?ids=` (tối đa 50) làm trong nhánh `c2-cart`. 6.7 trả thêm `skipped: [{variant_id, reason}]` và `adjusted: [{variant_id, quantity}]` (giống 6.6) — **sửa API_SPEC trước** | ☐ |
| D9 | **Giới hạn giỏ** | không có trong spec | Tối đa **100 dòng / giỏ**, **999 / dòng** → vượt trả `422` có `errors[]` (`InvalidRequestException`). Ghi vào API 6.3 | ☐ |
| D10 | **Mã đơn `TR-yyyyMMdd-######`** | UC-ORD-07 bước 4 (Redis `INCR order:seq:{yyyyMMdd}`) | Redis `INCR` + `EXPIRE` 48 giờ, ngày theo giờ VN; Redis lỗi → `503 GNE-SYS-5030` (client thử lại cùng `Idempotency-Key`). Mã phiên `CS-xxxxx` (base32 5 ký tự, UNIQUE) | ☐ |
| D11 | **Idempotency đặt hàng** | API 6.10 🔁 | Dùng `common/idempotency/IdempotencyFilter` sẵn có cho `POST /me/checkout-sessions`; IT gửi 2 lần cùng key → 1 phiên | ☐ |
| D12 | **Hoa hồng chụp vào `order_items`** | UC-ORD-07 bước 4 | `VendorProfile.effectiveCommissionRate(categoryRate)` qua `ShopFacade` + `CategoryFacade`: shop → ngành → 5% | ☐ |
| D13 | **Cron hủy phiên (ORD-08)** | Usecase: 60 giây, `created_at < now-15'`; LLD 10 | `@Scheduled(fixedDelayString = "PT60S", initialDelayString = "PT30S")`; chọn phiên `INITIATED / PAYMENT_PENDING` có `expires_at <= now` (cột CSDL; API gọi `reservation_expires_at`) (tương đương created_at+15', khớp hạn hiện ở web), lô 50, `SKIP LOCKED`, mỗi phiên 1 transaction. Nhả giữ chỗ **có điều kiện** (`WHERE status = 'ACTIVE'`) để không đua với IPN (C3). Hoàn voucher / Điểm / suất Flash Sale qua cổng `CheckoutCompensation` — bản mặc định không làm gì (C6 cài). Ghi `ORDER_STATUS_CHANGED` `details.reason = PAYMENT_TIMEOUT`, actor = hệ thống | ☐ |
| D14 | **Chặn xóa tài khoản** | BAO_CAO mục 3 (UC-AUTH-10) | Module order cài `AccountDeletionBlocker`: còn phiên `INITIATED / PAYMENT_PENDING` hoặc đơn chưa ở trạng thái kết thúc → chặn (`GNE-USR-4091`). **Cùng truy vấn đó** cài luôn cổng `catalog.product.application.ProductOrderGuard.hasOpenOrders(productId)` của C1 (chữ ký nguyên văn ở mục 8.4 — nếu C1 chưa đưa lên `develop` thì C2 tạo đúng nguyên văn) để người bán không xóa được sản phẩm còn trong đơn (`409 GNE-CAT-4094`) | ☐ |
| D15 | **Migration** | mục 8.1 | **Đã làm 2026-11-26** trong `V25__listing_and_checkout_schema.sql` chung với C1: `uk_cart_items_cart_variant (cart_id, product_variant_id)` (thêm cùng SKU → `INSERT … ON CONFLICT` cộng dồn) và `idx_checkout_sessions_open_expires (status, expires_at) WHERE status IN ('INITIATED', 'PAYMENT_PENDING')` (cron ORD-08). Lưu ý tên cột CSDL là **`expires_at`** (API 6.10 gọi là `reservation_expires_at`). Cần thêm index / cột → `V26+` theo thứ tự merge | ☑ 2026-11-26 |
| D16 | **Ghi `product_variants` & luật "mua được"** (chạm C1) | mục 8.3, 5 | Cột C2 được ghi: chỉ `reserved_count` (±) và `stock_count` (trừ khi chốt bán), luôn `version = version + 1`. Giữ chỗ lọc `p.status = 'PUBLISHED' AND v.deleted_at IS NULL`; nhả / chốt **không** lọc (vẫn phải nhả khi người bán đã ẩn / bỏ biến thể). Dòng giỏ mua được ⇔ `PUBLISHED` + `deleted_at IS NULL` + shop không nghỉ + không tự mua + còn tồn; `HIDDEN` / `PENDING_APPROVAL` / `FLAGGED` / `REJECTED` / `DELETED` / biến thể đã bỏ → `PRODUCT_UNAVAILABLE`. `VariantFacade` đọc thẳng CSDL, **không** qua cache `ProductDetailService` | ☐ |

---

## 3. Luồng nghiệp vụ chi tiết

### 3.1 Giỏ hàng (UC-ORD-01)

```
POST /me/cart/items {variant_id, quantity}
  ├─ chặn STAFF / ADMIN (đã có ở tầng bảo mật theo đường dẫn — GNE-ORD-4031)
  ├─ VariantFacade.lineInfo(variantId): product PUBLISHED & variant deleted_at IS NULL? → không: 404 / PRODUCT_UNAVAILABLE (D16)
  ├─ ShopFacade: chủ shop == người mua → 409 GNE-ORD-4094 · shop tạm nghỉ → 409 GNE-ORD-4093
  ├─ Cart.getOrCreate(userId)   (carts.user_id UNIQUE; tạo bằng INSERT … ON CONFLICT DO NOTHING)
  ├─ dòng trùng variant → quantity += Q (200) · mới → INSERT với price_snapshot = giá hiện tại (201)
  └─ tổng > available (stock_count - reserved_count) → 409 GNE-INV-4094, details.stock
GET /me/cart        → nhóm theo shop, mỗi dòng: unit_price hiện tại, price_at_add = price_snapshot, stock, is_available, unavailable_reason
GET /me/cart/items?limit=5&sort=-added_at  → mini-cart + summary.total
PATCH /me/cart/items/{id}  quantity | variant_id (đổi sang variant đã có → gộp, trả merged_from_id)
DELETE /me/cart/items?ids=…  → 204 (chỉ xóa dòng của chính mình)
POST /me/cart/items/batch  → {added, skipped[], cart_item_count}
POST /me/cart/merge        → {merged, skipped[], adjusted[], cart_item_count}   (D8)
GET /public/variants?ids=  → dòng hiển thị cho giỏ vãng lai (≤ 50 id)
```

### 3.2 Tính phí vận chuyển (UC-ORD-06)

```
ShippingFeeQuoter.quote(Parcel{vendorId, from(pickup), to(address), weightGrams, maxDims, subtotal, allSellerPays})
  ├─ freeship shop (D7) → 0, freeByShop = true (vẫn tính phí thật để C3 ghi trợ giá)
  ├─ thiếu kho lấy hàng (D5) / thiếu mã GHN (D4) → FlatFeeTable (15k cùng tỉnh / 30k khác tỉnh), estimated = true
  ├─ GHN_MODE=stub → FlatFeeTable, estimated = false (giả lập "đúng")
  └─ GHN_MODE=http → JsonCache "ship:fee:{hash}" (10') → GhnFeeClient (timeout 3s)
                       lỗi / timeout → FlatFeeTable, estimated = true · không phục vụ tuyến → SHIPPING_UNAVAILABLE
```

### 3.3 Xem trước & tách giỏ (UC-ORD-04)

```
POST /me/checkout/preview {cart_item_ids, address_id?}
  ├─ address_id rỗng → địa chỉ mặc định (AddressFacade) ; không có địa chỉ → address = null, shipping_fee = null
  ├─ nạp dòng giỏ thuộc người gọi (id lạ → bỏ qua, không lộ dữ liệu người khác)
  ├─ nhóm theo vendor_id (thứ tự = thứ tự shop trong giỏ) → mỗi shop: subtotal, weight, quote phí ship
  ├─ CheckoutDiscountPolicy.apply(...)   (mặc định 0 — D2)
  └─ summary: subtotal, shipping_total, các khoản giảm = 0, grand_total, order_count, item_count
```
Một engine `CheckoutPricer` dùng chung cho preview (6.8) và đặt hàng (6.10) — không tính hai nơi.

### 3.4 Đặt hàng (UC-ORD-07) — một transaction

```
POST /me/checkout-sessions  (Idempotency-Key)
  ├─ CheckoutPricer.price(...)  (tính lại toàn bộ; dòng không còn bán / shop nghỉ / tự mua → 409 tương ứng)
  ├─ BEGIN
  │   ├─ INSERT checkout_sessions (INITIATED, code CS-…, grand_total, payment_method, expires_at = now+15')
  │   ├─ mỗi shop: order_code (D10) → INSERT orders (PENDING_PAYMENT, shipping_address_snapshot_json, buyer_note, sub_total, shipping_fee…)
  │   │            INSERT order_items (snapshot tên, SKU, unit_price, commission_rate — D12)
  │   ├─ variant_id tăng dần: UPDATE product_variants … reserved_count += q, version += 1 WHERE status sản phẩm PUBLISHED AND deleted_at IS NULL AND stock_count - reserved_count >= q (D16)
  │   │            0 dòng → ROLLBACK, 409 GNE-INV-4090 {out_of_stock_cart_item_ids}
  │   │            INSERT stock_reservations (ACTIVE, expires_at = now+15')
  │   ├─ DELETE cart_items đã đặt
  │   └─ COD → session COMPLETED + reservations CONFIRMED · online → session PAYMENT_PENDING (D3)
  ├─ COMMIT → CheckoutCreatedEvent (AFTER_COMMIT; C11 gửi email sau)
  └─ 201 $CheckoutSession + payment (PaymentInitiator — D3)
GET /me/checkout-sessions/{id}  → chỉ chủ phiên; người khác → 404
```

### 3.5 Cron hủy phiên quá hạn (UC-ORD-08)

```
ExpiredSessionSweeper (60 giây)
  loop lô 50: tx {
    sessions = SELECT … WHERE status IN ('INITIATED','PAYMENT_PENDING') AND expires_at <= now
               ORDER BY expires_at LIMIT 50 FOR UPDATE SKIP LOCKED
    mỗi phiên (try/catch riêng):
      UPDATE stock_reservations SET status='EXPIRED', released_at=now WHERE id=? AND status='ACTIVE'
         1 dòng → UPDATE product_variants SET reserved_count = reserved_count - q, version = version + 1 WHERE id=? AND reserved_count >= q
         0 dòng → IPN đã CONFIRMED trước → bỏ qua phiên
      session → EXPIRED ; orders → AUTO_CANCELLED (Order.transitionTo) ; audit ORDER_STATUS_CHANGED {reason: PAYMENT_TIMEOUT}
      CheckoutCompensation.onExpired(session)   (mặc định no-op — C6)
  }
  log "Đã hủy {} phiên quá hạn thanh toán" khi > 0
```

---

## 4. Cây file sẽ tạo / sửa (dự kiến)

```
server-spring/src/main/java/com/genie/order/
├── cart/api/{CartController, PublicVariantController?, CartMapper, CartResponse, AddCartItemRequest, UpdateCartItemRequest, BatchAddRequest, MergeCartRequest}.java
├── cart/application/{CartService, CartMergeService, CartLineView}.java
├── cart/domain/{CartRepository, CartItemRepository}.java                     (truy vấn)
├── checkout/api/{CheckoutController, PreviewRequest, PreviewResponse, CreateCheckoutRequest, CheckoutResponse}.java
├── checkout/application/{CheckoutPricer, CheckoutService, CheckoutFacade, CheckoutDiscountPolicy, PaymentInitiator, CheckoutCompensation, OrderCodeGenerator}.java
├── checkout/domain/CheckoutSessionRepository.java
├── reservation/application/{ReservationService, ExpiredSessionSweeper}.java
├── reservation/domain/StockReservationRepository.java
├── order/application/OrderProductGuard.java                                (D14 — cài cổng ProductOrderGuard của C1)
└── order/application/OrderAccountDeletionBlocker.java                       (D14)
server-spring/src/main/java/com/genie/shipping/fee/
├── application/{ShippingFeeQuoter → chuyển từ order.checkout hoặc giữ cổng ở order, FlatFeeTable, Parcel}.java
└── infra/{GhnFeeClient, ShippingFeeConfig}.java                            (GHN_MODE)
server-spring/src/main/java/com/genie/catalog/product/application/VariantFacade.java     (mới — C2 sở hữu: dòng giỏ, giữ chỗ / nhả / chốt bằng JdbcTemplate; KHÔNG sửa ProductVariant / ProductVariantRepository — mục 8.2, 3)
server-spring/src/main/java/com/genie/vendor/shop/application/ShopFacade.java           (sửa — vacation, owner, pickup, commission)
server-spring/src/main/resources/
├── application.yml                       (⚠ cần chủ dự án đồng ý — chỉ khối genie.shipping.*, genie.checkout.*, GHN_MODE; khối genie.gemini.* là của C1)
└── (không sửa seed — R__seed_demo_30_shops.sql đã có pickup + ghn_shop_id; cần thêm thì file mới R__seed_demo_86_…)
server-spring/src/test/java/com/genie/order/
├── cart/{CartIT, CartMergeIT, PublicVariantsIT}.java
├── checkout/{CheckoutPreviewIT, CheckoutSessionIT, CheckoutConcurrencyIT, CheckoutPricerTest}.java
├── reservation/ExpiredSessionSweeperIT.java
├── OrderSecurityIT.java                  (STAFF/ADMIN 403, truy cập chéo 404)
└── ../support/OrderTestData.java         (hàm dựng giỏ / phiên — hạn chế sửa TestDataBuilder)
server-spring/src/test/java/com/genie/shipping/fee/{FlatFeeTableTest, GhnFeeClientTest (WireMock / MockRestServiceServer)}.java
web/src/features/cart/…, web/src/features/checkout/… (mapper / api / ui theo D2, D6, D8) · .env.example (GHN_MODE, PAYMENT_MODE)
docs/: API_SPEC 6.x, Usecase.md (Hiện trạng 5 UC), LLD 10, web-backend-handoff.md, kiem-thu/kich-ban-<ngày>-c2.md, CHECKLIST, BAO_CAO, Excel
```

---

## 5. Task

> Mã task `Oxx.yy`. Mỗi task `COMMIT` chỉ làm sau khi `./mvnw verify` (+ `npm run verify` nếu đụng web) xanh. Sau **mỗi** task: tick ở progress mục 4 + một dòng **Nhật ký** (mục 6 của progress).

### O00 — Chuẩn bị (nhánh `develop`)
- **O00.01** Đọc mục 8, xác nhận với người làm C1 (ghi Nhật ký cả hai plan). Chốt D1–D16 với nhóm (đặc biệt D3 với người làm C3, D5 với Thiện, D2 với người làm C6), ghi vào progress mục 3.
- **O00.02** `git switch develop && git pull` — xác nhận sạch (`git status`), ghi mã commit gốc vào progress.
- **O00.03** Chờ **PR schema `V25`** (mục 8.1, plan C1 P00.06) vào `develop` — duyệt PR đó; xong mới tạo `feature/c2-cart` (O01.01).
- **O00.04** Dựng môi trường: `docker compose … up -d`, JDK 21, `./mvnw verify` + `npm run verify` trên `develop` để có mốc xanh; ghi số test vào progress mục 7.
- **O00.05** Ghi tên người phụ trách vào BAO_CAO (dòng C2), CHECKLIST (cột Phụ trách 5 UC), Excel. COMMIT `docs: assign c2 cart and checkout chain` (có thể gộp vào O01.05).

### O01 — Nhánh `feature/c2-cart`: tài liệu trước (UC-ORD-01)
- **O01.01** `git switch -c feature/c2-cart` (từ `develop`).
- **O01.02** `API_SPEC.md` 6.3 (giới hạn D9), 6.7 (`skipped`, `adjusted` — D8).
- **O01.03** `web-backend-handoff.md`: ghi rõ các hàm `cart` sẽ nối.
- **O01.04** Thêm `GNE-INV-4093`, `GNE-INV-4094`, `GNE-ORD-4093`, `GNE-ORD-4094` vào `ErrorCode` (chỉ trong khối đơn hàng / tồn kho) + `error-codes.ts` + khóa vi / en (chỉ thêm khóa mới — mục 8.2).
- **O01.05** COMMIT `docs(order): specify cart limits and merge result`.

### O02 — `feature/c2-cart`: backend + test
- **O02.01** Cổng đọc từ `catalog` — file **mới** `VariantFacade` (C2 sở hữu, JdbcTemplate, đọc thẳng CSDL, luật D16 / mục 8.5) (`VariantFacade.lineInfos(ids)` nạp theo lô: product status, giá, giá gốc, ảnh, slug, tồn khả dụng, vendorId) + `vendor` (`ShopFacade`: mini shop, vacation, owner).
- **O02.02** Repository + `CartService` (thêm, xem, mini-cart, đổi số lượng / phân loại + gộp, xóa nhiều, batch).
- **O02.03** `CartMergeService` + `GET /public/variants`.
- **O02.04** `CartController` (6.1–6.7) + mapper đúng snake_case API.
- **O02.05** Test: `CartIT` (sản phẩm `HIDDEN` / `PENDING_APPROVAL` / `DELETED` / biến thể `deleted_at` → `PRODUCT_UNAVAILABLE`, cộng dồn, vượt tồn `4094`, tự mua `4094`, shop nghỉ `4093`, sản phẩm ẩn, đổi phân loại gộp dòng, xóa dòng người khác bị bỏ qua, giới hạn D9), `CartMergeIT`, `PublicVariantsIT`, `OrderSecurityIT` (STAFF / ADMIN `403 GNE-ORD-4031`, khách chưa đăng nhập `401`).
- **O02.06** COMMIT `feat(order): implement shopping cart api`.

### O03 — `feature/c2-cart`: nối web
- **O03.01** `cart.mapper.ts` / `cart.api.ts` nhánh thật đúng DTO; `price_at_add` ≠ `unit_price` → cảnh báo đổi giá; `unavailable_reason` → nhãn.
- **O03.02** Gộp giỏ khi đăng nhập hiển thị toast theo `skipped` / `adjusted`.
- **O03.03** Bật cờ `cart` trong `PUBLIC_REAL_API` (docs `cau-hinh-moi-truong.md`).
- **O03.04** COMMIT `feat(web): connect cart to real api`.

### O04 — `feature/c2-cart`: kiểm tra & PR
- **O04.01** Chạy API bằng `docs/api-tests/order-cart.http` (mới) — ghi "API đúng".
- **O04.02** Cập nhật CHECKLIST UC-ORD-01 (BE ☑, Test ☑, Nối ☑) + `Usecase.md` *Hiện trạng* + Excel. COMMIT `docs: record c2 cart progress`.
- **O04.03** Push + PR `feat(order): implement shopping cart api` vào `develop` (chờ người dùng cho push).

### O05 — Nhánh `feature/c2-shipping-fee` (UC-ORD-06)
- **O05.01** `git switch -c feature/c2-shipping-fee` (từ `feature/c2-cart`). **Spike D4**: thử API phí GHN sandbox với `to_ward_code` 2 cấp; ghi kết quả progress mục 7.
- **O05.02** Tài liệu: `API_SPEC.md` 6.8 thêm `is_estimated_fee`, `SHIPPING_UNAVAILABLE` (D6); `Usecase.md` UC-ORD-06 ghi chế độ stub / http; `.env.example` `GHN_MODE`. COMMIT `docs(shipping): specify shipping fee quote and fallback`.
- **O05.03** `FlatFeeTable`, `Parcel`, `ShippingFeeQuoter` (freeship D7, thiếu kho D5).
- **O05.04** `GhnFeeClient` (`RestClient`, timeout 3s, token sàn, `shop_id` = `ghnShopId`) + cache `JsonCache` 10 phút.
- **O05.05** Kiểm seed `R__seed_demo_30_shops.sql` đã đủ `pickup_province_id`, `pickup_ward_code`, `ghn_shop_id` cho mọi shop demo (D5) — **không sửa** file; thiếu thì thêm file mới `R__seed_demo_86_…` và báo người giữ seed.
- **O05.06** Test: `FlatFeeTableTest`, `ShippingFeeQuoterTest` (freeship, thiếu kho, khối lượng NULL), `GhnFeeClientTest` (giả GHN: OK, timeout → dự phòng, tuyến không hỗ trợ, cache trúng).
- **O05.07** COMMIT `feat(shipping): quote ghn shipping fee with flat fallback` (+ `chore(db): …` chỉ khi O05.05 phải thêm file seed mới).
- **O05.08** CHECKLIST UC-ORD-06 (BE, Test), push + PR (chờ người dùng).

### O06 — Nhánh `feature/c2-checkout-preview` (UC-ORD-04)
- **O06.01** `git switch -c feature/c2-checkout-preview` (từ `feature/c2-shipping-fee`).
- **O06.02** Tài liệu: `API_SPEC.md` 6.8 ghi rõ C2 trả giảm giá = 0 / `null` (D2); gạch dòng 44 handoff (`shipping-policies` → dùng 6.8). COMMIT `docs(order): clarify checkout preview before vouchers`.
- **O06.03** `CheckoutDiscountPolicy` (mặc định 0) + `CheckoutPricer` (nhóm theo shop, subtotal, phí ship, tổng).
- **O06.04** `POST /me/checkout/preview`.
- **O06.05** Test: `CheckoutPricerTest` (2 shop, làm tròn, freeship), `CheckoutPreviewIT` (địa chỉ mặc định, không có địa chỉ, dòng của người khác, sản phẩm hết hàng → `is_available=false`).
- **O06.06** Web: `checkout` mapper / api preview thật, ẩn Voucher / Điểm (D2), hiện "phí tạm tính" khi `is_estimated_fee`, bỏ `getShipPolicies` (tổng giỏ lấy từ preview). COMMIT `feat(order): add checkout preview with per-shop split`; COMMIT `feat(web): use real checkout preview`.
- **O06.07** CHECKLIST UC-ORD-04 (+ Nối UC-ORD-06), push + PR.

### O07 — Nhánh `feature/c2-checkout-session` (UC-ORD-07)
- **O07.01** `git switch -c feature/c2-checkout-session` (từ `feature/c2-checkout-preview`).
- **O07.02** Tài liệu: `API_SPEC.md` 6.10 ghi D3 (COD hoàn tất ngay, online `PAYMENT_PENDING` với cổng giả lập), `LLD.md` 6.1 khớp; migration đã có ở `V25` — không tạo thêm (D15). COMMIT `docs(order): specify checkout session before payment gateway`.
- **O07.03** `OrderCodeGenerator` (D10), `ReservationService.reserveAll / confirmAll` (LLD 9.1) — câu SQL giữ chỗ / nhả / chốt nằm trong `VariantFacade` theo mục 8.3 (tăng `version`).
- **O07.04** `CheckoutService.place` (một transaction, snapshot, hoa hồng D12, xóa giỏ, COD / online D3), `PaymentInitiator` stub, `CheckoutCreatedEvent`.
- **O07.05** `POST /me/checkout-sessions` (Idempotency D11), `GET /me/checkout-sessions/{id}`.
- **O07.06** `OrderAccountDeletionBlocker` + `OrderProductGuard` (cài `ProductOrderGuard` — D14, mục 8.4).
- **O07.07** Test: `CheckoutSessionIT` (2 shop → 2 đơn, snapshot không đổi khi sửa giá sau đó, giỏ bị xóa đúng dòng, COD → `COMPLETED` + tồn giảm, VNPAY → `PAYMENT_PENDING` + `reserved_count` tăng, hết hàng → `4090` + rollback toàn bộ, shop nghỉ `4093`, tự mua `4094`, Idempotency trùng key), `CheckoutConcurrencyIT` (2 luồng tranh 1 sản phẩm tồn 1 → chỉ 1 thành công; biến thể `deleted_at` / sản phẩm `HIDDEN` → `4090`; mỗi lần giữ chỗ `version` tăng), `OrderProductGuardIT` (phiên chờ thanh toán / đơn chưa kết thúc → `true`, đơn đã hủy → `false`), `OrderCodeGeneratorTest`, `AccountDeletionGuardIT` bổ sung.
- **O07.08** Web: `createCheckout` / `getCheckoutSession` thật; trang kết quả COD / online-stub; xử lý `4090` (gỡ dòng + gọi lại preview). Bật cờ `checkout`.
- **O07.09** COMMIT `feat(order): persist checkout session and per-shop orders`; COMMIT `feat(order): block account and product deletion with open orders`; COMMIT `feat(web): place orders through real checkout api`.
- **O07.10** CHECKLIST UC-ORD-07, push + PR.

### O08 — Nhánh `feature/c2-order-sweeper` (UC-ORD-08)
- **O08.01** `git switch -c feature/c2-order-sweeper` (từ `feature/c2-checkout-session`).
- **O08.02** Tài liệu: `LLD.md` 10 dòng ORD-08 ghi chu kỳ / lô / điều kiện (D13); `Usecase.md` UC-ORD-08. COMMIT `docs(order): specify expired checkout sweeper`.
- **O08.03** `ExpiredSessionSweeper` + `CheckoutCompensation` (no-op) + audit.
- **O08.04** Test: `ExpiredSessionSweeperIT` dùng `MutableClock` (quá hạn → `EXPIRED` / `AUTO_CANCELLED` / `reserved_count` về cũ; chưa quá hạn không đụng; giữ chỗ đã `CONFIRMED` → bỏ qua; COD không bị quét; 2 tiến trình chạy song song không trừ 2 lần; một phiên lỗi không chặn phiên khác).
- **O08.05** COMMIT `feat(order): cancel expired checkout sessions and release stock`.
- **O08.06** CHECKLIST UC-ORD-08 (BE, Test), push + PR.

### O09 — Chạy thật trên giao diện (người dùng tự bấm — kịch bản `docs/kiem-thu/kich-ban-<ngày>-c2.md`)
- **O09.01** Viết kịch bản TC-C2 (mục 6) vào `docs/kiem-thu/`.
- **O09.02** TC-C2-CART: thêm / đổi / xóa / mini-cart / vãng lai → đăng nhập gộp giỏ / Mua lại.
- **O09.03** TC-C2-SHIP + PREVIEW: 2 shop khác tỉnh, đổi địa chỉ, freeship, GHN lỗi (tắt mạng / `GHN_MODE=http` token sai) → phí tạm tính.
- **O09.04** TC-C2-ORDER: đặt COD 2 shop; đặt VNPAY (stub) rồi bỏ; hết hàng giữa chừng; nhấn đặt 2 lần.
- **O09.05** TC-C2-SWEEP: đợi 15 phút (hoặc chỉnh `genie.checkout.reservation-ttl=PT2M` ở local) → đơn tự hủy hiện ở `/orders`, tồn trả lại.
- **O09.06** Ghi Đạt / Không đạt vào progress mục 5; lỗi phát sinh → thêm task `O11.xx` (nhánh `bugfix/c2-…`).

### O10 — Hồi quy & đóng plan
- **O10.01** `./mvnw clean verify` + `npm run verify` + `npm run test:e2e` trên nhánh cuối; ghi số vào progress mục 7.
- **O10.02** CHECKLIST: 5 UC đủ lớp → ✅, mục 1 Tổng quan, bảng theo phân hệ, mục 3, mục 5 Nhật ký.
- **O10.03** `Usecase.md` *Hiện trạng* 5 UC + Excel cột *Hiện trạng mã nguồn* (sửa ngay sau `git merge develop` — tệp nhị phân) + BAO_CAO dòng C2 (tiến độ) + `CLAUDE.md` mục 2 (đoạn "Plan 2026-11-25"). Nếu C1 đã lên `develop`: chạy kịch bản nối chuỗi ở mục 8.9, ghi Nhật ký cả hai plan.
- **O10.04** COMMIT `docs: close c2 cart and checkout plan`.

---

## 6. Kịch bản chạy thật (tóm tắt — chi tiết ở `docs/kiem-thu/`)

| Mã | UC | Kịch bản | Kỳ vọng |
|---|---|---|---|
| TC-C2-01 | ORD-01 | Thêm 1 SKU 2 lần | 1 dòng, số lượng cộng dồn, badge tăng |
| TC-C2-02 | ORD-01 | Thêm vượt tồn | "Sản phẩm này chỉ còn N…" (`GNE-INV-4094`) |
| TC-C2-03 | ORD-01 | Chủ shop TechZone thêm sản phẩm của TechZone | Bị chặn `GNE-ORD-4094` |
| TC-C2-04 | ORD-01 | Tài khoản Admin vào giỏ | Không thấy nút mua / API `403 GNE-ORD-4031` |
| TC-C2-05 | ORD-01 | Vãng lai thêm 2 món → đăng nhập | Toast gộp giỏ, `localStorage` xóa |
| TC-C2-06 | ORD-01 | Seller đổi giá khi món đang trong giỏ | Cảnh báo "giá đã thay đổi" |
| TC-C2-07 | ORD-04/06 | Giỏ 2 shop khác tỉnh → checkout | 2 kiện, 2 phí ship, tổng đúng |
| TC-C2-08 | ORD-06 | Shop đạt ngưỡng freeship | "Shop miễn phí vận chuyển", phí 0 |
| TC-C2-09 | ORD-06 | GHN lỗi / timeout | Phí 15k / 30k, nhãn "phí tạm tính" |
| TC-C2-10 | ORD-07 | Đặt COD 2 shop | Trang thành công, 2 mã `TR-…`, giỏ mất đúng các món, tồn giảm |
| TC-C2-11 | ORD-07 | Bấm "Đặt hàng" 2 lần nhanh | Chỉ 1 phiên |
| TC-C2-12 | ORD-07 | Hai trình duyệt cùng mua SKU còn 1 | 1 thành công, 1 thấy "Sản phẩm vừa hết hàng — đã gỡ khỏi đơn" |
| TC-C2-13 | ORD-08 | Đặt VNPAY (stub) rồi bỏ, chờ hết hạn | Đơn "Đã hủy tự động", tồn khả dụng trở lại |

---

## 7. Rủi ro

| Mã | Rủi ro | Cách xử lý |
|---|---|---|
| R1 | GHN fee không nhận địa giới 2 cấp (D4) | Spike O05.01 trước; dự phòng bảng phí cố định |
| R2 | Kho lấy hàng 2 cấp (UC-VND-05 ✅) không có quận → GHN fee v2 không dùng được | Spike O05.01 (D4); dự phòng bảng phí cố định |
| R3 | C3 / C6 làm song song đụng `CheckoutService` | Cổng `PaymentInitiator`, `CheckoutDiscountPolicy`, `CheckoutCompensation` chốt chữ ký ngay O06 / O07 và báo nhóm |
| R4 | Deadlock khi giữ chỗ nhiều SKU | Sắp `variant_id` tăng dần; `CheckoutConcurrencyIT` |
| R5 | Trùng số migration với nhánh khác | `V25` chung đã tạo; thêm mới `V26+` theo thứ tự merge — mục 8.1 |
| R7 | Làm song song C1: ghi đè `reserved_count`, xung đột file chung (`ErrorCode`, `application.yml`, i18n, `.env.example`, CHECKLIST, Excel) | Tuân mục 8; `git merge origin/develop` trước mỗi PR; IT hai bên có ca chống ghi đè (mục 8.3) |
| R6 | PR vượt ~400 dòng | Tách PR backend / web trong cùng nhánh |

---

## 8. Phối hợp với C1 (Đăng bán) — làm song song, độc lập

> Phần này **cùng nội dung** (chỉ khác số mục) với mục phối hợp trong plan bên kia ([plan C1 mục 10](../../2026-11-26/c1.md)) — sửa một bên thì sửa cả bên kia, báo người làm và ghi Nhật ký cả hai file. Nguyên tắc: (1) không bên nào phải chờ bên kia để code / test — C2 test trên sản phẩm `PUBLISHED` của seed + `TestDataBuilder`, C1 không cần giỏ / đơn; (2) không sửa file bên kia sở hữu (8.2); (3) điểm chạm duy nhất lúc chạy là bảng `product_variants` (8.3) và cổng `ProductOrderGuard` (8.4).

### 8.1 Migration chung `V25` (đã tạo 2026-11-26)

- `server-spring/src/main/resources/db/migration/V25__listing_and_checkout_schema.sql` gồm **cả hai chuỗi**: C1 — `products.submitted_at`, `is_requeued`, `is_locked`, `video_url` + `idx_products_moderation_queue`; C2 — `uk_cart_items_cart_variant (cart_id, product_variant_id)`, `idx_checkout_sessions_open_expires (status, expires_at)`. `Product.java` đã có 4 trường mới; `ENTITY.md` 2.2 + 13.1 đã cập nhật. Cột mới đều NULL được / có DEFAULT → code, seed, `TestDataBuilder` cũ không phải đổi.
- **PR schema đi trước:** chỉ gồm 3 file trên (`V25`, `Product.java`, `ENTITY.md`) — COMMIT `feat(db): add shared listing and checkout schema` + `docs(db): document the shared listing and checkout schema`, PR vào `develop`, người bên kia duyệt. C2 tạo nhánh `feature/c2-cart` **sau** khi PR này vào `develop`; C1 `git merge develop` lại vào `feature/c1`.
- Sau khi merge **không sửa `V25`**. Bên nào phát sinh thêm cột / index → `V26`, `V27`… theo **thứ tự merge**: trước khi mở PR có migration, `git fetch && git log origin/develop -- server-spring/src/main/resources/db/migration` để lấy số kế tiếp; trùng số thì người merge sau đổi tên.

### 8.2 Sở hữu file — chỉ bên sở hữu được sửa

#### 8.2.1 Backend `server-spring`

| File / thư mục | Chủ | Bên kia được làm gì |
|---|---|---|
| `catalog/product/domain/Product.java`, `ProductVariant.java`, `ProductImage.java`, `ProductDocument.java`, `ProductRepository.java`, `ProductVariantRepository.java`, `ProductImageRepository.java` | **C1** | C2 **không sửa**. Cần truy vấn → viết SQL trong `VariantFacade` (JdbcTemplate) |
| `catalog/product/application/*` hiện có + file mới của C1 (`SellerProductService`, `ProductWriteService`, `ProductLifecycleService`, `ProductMediaService`, `ProductCacheEvictor`, `SellerShopPort`, …) | **C1** | — |
| `catalog/product/application/ProductOrderGuard.java` (cổng, mục 8.4) | **chung** — nội dung chốt nguyên văn ở mục 8.4 | Ai cần trước thì tạo đúng nguyên văn |
| `catalog/product/application/VariantFacade.java` (**file mới**) | **C2** | C1 không tạo lớp trùng tên; C1 không gọi lớp này |
| `catalog/brand/**`, `catalog/moderation/**`, `catalog/proposal/**`, `catalog/category/application/CategoryFacade.java` (+ `attributesFor`) | **C1** | C2 chỉ gọi `CategoryFacade.effectiveCommissionRate` (đã có, không sửa) |
| `vendor/shop/application/VendorShopPorts.java` | **C1** (thêm `implements SellerShopPort`) | C2 không sửa |
| `vendor/shop/application/ShopFacade.java` | **C2** (thêm phương thức đọc kho lấy hàng / chủ shop nếu thiếu) | C1 không sửa — C1 đọc shop qua `SellerShopPort` |
| `admin/staff/**` (moderator) | **C1** | — |
| `order/**`, `shipping/fee/**` | **C2** | C1 không sửa |
| `common/storage/UploadPurpose.java`, `common/mail/MailTemplate.java` + `templates/mail/product-*` | **C1** | — |
| `common/error/ErrorCode.java` | **chung** | C1 chỉ thêm dòng trong khối `GNE-CAT-*`; C2 chỉ thêm trong khối `// ===== Đơn hàng / tồn kho` (`GNE-INV-*`, `GNE-ORD-*`). Không sắp xếp lại / đổi dòng của nhau |
| `src/main/resources/application.yml` | **chung** — ⚠ mọi thay đổi cần chủ dự án đồng ý | C1 chỉ trong khối `genie.gemini.*`; C2 chỉ trong `genie.shipping.*` (thêm `mode`, `default-weight-grams`…) và khối mới `genie.checkout.*` |
| `db/seed/*` | C1: file **mới** `R__seed_demo_85_moderation_queue.sql`. C2: **không sửa seed** (`R__seed_demo_30_shops.sql` đã có `pickup_*` + `ghn_shop_id` cho mọi shop demo) — cần thêm thì tạo file mới `R__seed_demo_86_…` | Không sửa file seed có sẵn của nhau |
| `src/test/java/com/genie/support/TestDataBuilder.java` | **chung — hạn chế sửa** | Hàm dựng mới: C1 đặt ở `support/ListingTestData.java`, C2 ở `support/OrderTestData.java` (gọi `TestDataBuilder` bên trong). Chỉ sửa `TestDataBuilder` khi buộc phải, và báo bên kia |
| Fake mới | C1: `FakeProductScreeningClient`; C2: `FakeGhnFeeClient`, `FakePaymentInitiator` (nếu cần) | — |

#### 8.2.2 Web `web/`

| Phần | Chủ |
|---|---|
| `features/seller-products`, `features/admin-moderation`, `features/admin-catalog` | **C1** |
| `features/cart`, `features/checkout`, nút "Thêm vào giỏ / Mua ngay" ở `features/product` | **C2** |
| `shared/api/error-codes.ts`, `i18n/vi.json`, `i18n/en.json` | **chung** — mỗi bên chỉ thêm khóa mới (`errors.GNE-CAT-*` / `errors.GNE-INV-*`, `GNE-ORD-*`, khóa của feature mình), không sửa khóa của nhau |
| `web/.env.example` dòng `PUBLIC_REAL_API` | **chung** — người merge sau gộp hợp hai danh sách nhóm (C1: `seller-products,catalog-proposals,product-moderation,catalog-products`; C2: `cart,checkout`) |

#### 8.2.3 Tài liệu

| File | Luật |
|---|---|
| `API_SPEC.md` | C1 sửa 4.1, 10.5, 10.6, 11.11 (*Sản phẩm trong danh mục con*, *Đề xuất*), 11.12, dòng `GNE-CAT-*` ở 15, 16 (`reason_code`). C2 sửa 5 (`/public/variants`), 6.x, dòng `GNE-INV-*` / `GNE-ORD-*` ở 15. Dòng *Lịch sử* đầu file: mỗi bên thêm một mục phiên bản riêng, người merge sau đánh lại số |
| `Usecase.md`, `ENTITY.md`, `LLD.md` | Mỗi bên chỉ sửa mục UC / entity của mình (C1: UC-CAT-09…13, 17, ENTITY 2.x; C2: UC-ORD-*, ENTITY 3.x, 5.x, LLD 6.1, 9.1, 10) |
| `CHECKLIST_TIEN_DO_UC.md` | Mỗi bên chỉ sửa dòng UC của mình + một dòng Nhật ký. **Mục 1 Tổng quan & bảng theo phân hệ**: đếm lại từ các dòng UC ngay trước khi merge (không cộng dồn số của nhánh mình) |
| `BAO_CAO_UC_DOC_LAP.md` | C1 sửa mục 6 khối C1; C2 sửa khối C2; bảng *Thành viên* mỗi người sửa dòng của mình |
| `Quản Lý UseCase.xlsx` (tệp nhị phân — **git không gộp được**) | Chỉ sửa ở commit tài liệu **cuối** của plan, ngay sau `git merge develop`; nếu vẫn xung đột → lấy bản `develop` rồi sửa lại dòng của mình |
| `CLAUDE.md` mục 2, 9 | Mỗi bên thêm đoạn riêng (`Plan 2026-11-26 C1` / `Plan 2026-11-25 C2`); dòng "Migration mới tiếp theo từ …" người merge sau sửa |
| `docs/web-backend-handoff.md` | Mỗi bên chỉ gạch dòng của feature mình |

### 8.3 Bảng `product_variants` — ai ghi cột nào (chống ghi đè tồn kho)

**Vấn đề:** `ProductVariant` không có `@DynamicUpdate`; nếu C1 nạp entity rồi lưu bằng JPA, câu UPDATE ghi lại **mọi cột**, kể cả `reserved_count` mà C2 vừa tăng bằng SQL → mất giữ chỗ, cron ORD-08 trừ âm.

| Cột | C1 (người bán sửa) | C2 (giỏ / đặt hàng / cron) |
|---|---|---|
| `price`, `sku`, `variant_name`, `attributes_json`, `image_url` | ghi | chỉ đọc |
| `stock_count` | ghi (giá trị tuyệt đối người bán nhập), **chỉ khi** `mới >= reserved_count` | trừ khi chốt bán (COD / thanh toán xong): `stock_count - q` |
| `reserved_count` | **không bao giờ ghi** (dòng mới: `0`) | `+ q` khi giữ chỗ, `- q` khi nhả / chốt |
| `deleted_at` | ghi (bỏ biến thể khi sửa, xóa sản phẩm) | chỉ đọc |
| `version` | `version + 1` mỗi lần ghi | `version + 1` mỗi lần ghi |

**Luật bắt buộc cho C1:**
- Biến thể đã có: **không** sửa qua entity JPA được quản lý. Dùng UPDATE có chọn cột (`@Modifying` query hoặc JdbcTemplate) trong `ProductVariantRepository`:
  `UPDATE product_variants SET price = :p, stock_count = :s, sku = :sku, variant_name = :n, attributes_json = :a, image_url = :img, version = version + 1 WHERE id = :id AND product_id = :pid AND deleted_at IS NULL AND :s >= reserved_count` → 0 dòng ⇒ đọc lại; `reserved_count > :s` ⇒ `422 GNE-VAL-4220` (`errors[].field = variants[i].stock`, `code = BELOW_RESERVED`, `details.reserved`).
- Biến thể mới: `persist` entity mới (`reserved_count = 0`) — an toàn.
- Bỏ biến thể khi sửa: `reserved_count > 0` ⇒ `409 GNE-CAT-4094` (`details.variant_ids`); còn lại `UPDATE … SET stock_count = 0, deleted_at = now(), version = version + 1 WHERE id = ? AND reserved_count = 0` (đúng luật `ProductVariant.softDelete`: người bán bỏ phân loại = ngừng bán, đưa tồn về 0).
- Xóa sản phẩm: có biến thể `reserved_count > 0` **hoặc** `ProductOrderGuard.hasOpenOrders(productId)` ⇒ `409 GNE-CAT-4094`. Phần kiểm `reserved_count` C1 tự làm được ngay, không chờ C2.

**Luật bắt buộc cho C2 (trong `VariantFacade`, JdbcTemplate):**
- Giữ chỗ: `UPDATE product_variants v SET reserved_count = v.reserved_count + :q, version = v.version + 1 FROM products p WHERE v.id = :id AND p.id = v.product_id AND p.status = 'PUBLISHED' AND v.deleted_at IS NULL AND v.stock_count - v.reserved_count >= :q` (0 dòng ⇒ `409 GNE-INV-4090`). Sắp `variant_id` tăng dần.
- Nhả: `… SET reserved_count = reserved_count - :q, version = version + 1 WHERE id = :id AND reserved_count >= :q` (không lọc `deleted_at` / trạng thái — vẫn phải nhả được khi người bán đã ẩn / bỏ biến thể).
- Chốt bán: `… SET stock_count = stock_count - :q, reserved_count = reserved_count - :q, version = version + 1 WHERE id = :id AND reserved_count >= :q AND stock_count >= :q`.
- Không dùng `ProductVariant.reserve/releaseReservation/commitSale` trên entity được quản lý (cùng lý do ghi đè).

**Test hai bên phải có:** C1 — `SellerProductWriteIT` ca "đang có giữ chỗ (đặt `reserved_count = 2` bằng SQL) → người bán sửa giá → `reserved_count` vẫn 2" và ca `BELOW_RESERVED`. C2 — `CheckoutConcurrencyIT` ca "giữ chỗ trên biến thể đã `deleted_at` / sản phẩm `HIDDEN` → 4090".

### 8.4 Cổng `ProductOrderGuard` (C1 khai, C2 cài)

Nội dung **nguyên văn** — ai cần trước thì tạo, người sau dùng bản trên `develop`, không sửa chữ ký:

```java
// Cổng đảo ngược: module order cho catalog biết sản phẩm còn nằm trong đơn chưa kết thúc (plan C1 mục 10.4 = plan C2 mục 8.4).
package com.genie.catalog.product.application;

public interface ProductOrderGuard {

    /**
     * true = còn phiên checkout INITIATED / PAYMENT_PENDING hoặc đơn con chưa ở trạng thái kết thúc
     * (COMPLETED / CANCELLED / AUTO_CANCELLED / REFUNDED…) có chứa biến thể của sản phẩm này.
     */
    boolean hasOpenOrders(long productId);
}
```

- Bản mặc định (C1, khi chưa có module order): bean `@ConditionalOnMissingBean` trả `false`, đặt trong `catalog/product/application/ProductPortsConfig.java` (C1 sở hữu).
- Bản thật (C2, task O07.06): `order/order/application/OrderProductGuard.java` — truy vấn `order_items` + `orders` / `checkout_sessions` theo `variant_id IN (biến thể của sản phẩm)`.

### 8.5 Luật "mua được" — C1 ghi trạng thái, C2 đọc

Một dòng giỏ / đặt hàng **mua được** khi và chỉ khi: `products.status = 'PUBLISHED'` **và** `product_variants.deleted_at IS NULL` **và** shop không tạm nghỉ **và** người mua không phải chủ shop **và** tồn khả dụng > 0.

| C1 làm | Trạng thái C2 thấy | `unavailable_reason` C2 trả |
|---|---|---|
| Shop tự ẩn / sàn khóa (`is_locked`) | `HIDDEN` | `PRODUCT_UNAVAILABLE` |
| Sửa trường quan trọng sản phẩm đang bán | `PENDING_APPROVAL` (chờ duyệt lại) | `PRODUCT_UNAVAILABLE` |
| AI / Staff từ chối, gắn cờ | `REJECTED` / `FLAGGED` | `PRODUCT_UNAVAILABLE` |
| Xóa sản phẩm | `DELETED` | `PRODUCT_UNAVAILABLE` |
| Bỏ một biến thể | `deleted_at` khác NULL | `PRODUCT_UNAVAILABLE` |
| Hết tồn | `stock_count - reserved_count = 0` | `OUT_OF_STOCK` |

- C1 **không** thêm giá trị `ProductStatus` mới; cờ `is_locked` (C1 D1) luôn đi kèm `HIDDEN`.
- C2 đọc thẳng CSDL trong `VariantFacade` — **không** dùng cache `ProductDetailService` (cache 5 phút) để giá / trạng thái mới của C1 thấy ngay ở giỏ. C1 vẫn `evict` cache chi tiết như plan.
- Giá: C1 đổi giá có hiệu lực ngay; C2 so `price_snapshot` để cảnh báo "giá đã đổi", luôn tính lại giá ở preview / đặt hàng.

### 8.6 Vận chuyển: cân nặng, kích thước, freeship, kho lấy hàng

| Điểm | C1 (ghi) | C2 (đọc) |
|---|---|---|
| Cân nặng | `submit = true` bắt buộc `weight_grams` 1–50.000 (UC-CAT-09 bước 8); lưu `products.weight` **đơn vị gram** | Đọc `products.weight` (gram); NULL (chỉ còn ở dữ liệu cũ) → 500 g |
| Kích thước | Tùy chọn, mỗi chiều 1–200 cm (`length`, `width`, `height`) | NULL → bỏ qua khi tính phí |
| Ai trả phí ship | `shipping_payer` ∈ `BUYER`, `SELLER` | `SELLER` → sản phẩm đó miễn phí ship cho khách (C2 D7) |
| Ngưỡng freeship | **Chỉ cấp shop** — `vendor_shipping_config.freeship_threshold` (màn Cài đặt vận chuyển). C1 **bỏ** `free_shipping_threshold` theo sản phẩm khỏi API 10.5, form web và migration | Đọc ngưỡng shop qua `VendorShippingSettingsAdapter` |
| `ships_from` | C1 ghi tên tỉnh kho lấy hàng lúc lưu (chỉ để hiển thị) | **Không** dùng tính phí — tính từ `vendor_profiles.pickup_*` |
| Kho lấy hàng | UC-VND-05 ✅ (2026-11-25): kho 2 cấp (`pickup_province_id`, `pickup_ward_code`, `pickup_district_id = NULL`), `isPickupConfigured()`. C1 chặn gửi duyệt khi chưa cấu hình (`422 GNE-CAT-4224`) | Sản phẩm cũ của shop chưa có kho (chỉ còn ở dữ liệu cũ) → phí dự phòng (C2 D5) |

### 8.7 Hoa hồng
Chỉ C2 dùng: `ShopFacade.effectiveCommissionRate(vendorUserId, CategoryFacade.effectiveCommissionRate(categoryId))` (đã có) chụp vào `order_items`. C1 không sửa hai hàm này. C1 đổi danh mục sản phẩm → đi duyệt lại; đơn cũ giữ snapshot.

### 8.8 Mã lỗi mỗi bên thêm (không trùng)

| C1 | C2 |
|---|---|
| `GNE-CAT-4093`, `4094`, `4096` (mới), `4097` (mới), `4221`, `4222`, `4223`, `4224` (mới) | `GNE-INV-4093`, `GNE-INV-4094`, `GNE-ORD-4093`, `GNE-ORD-4094` (đã có trong API 15, **chưa** có trong `ErrorCode.java`) |

`GNE-CAT-4094` (C1 — sản phẩm còn đơn đang xử lý) và `GNE-ORD-4094` (C2 — mua hàng của chính shop mình) khác miền, không đụng nhau.

### 8.9 Thứ tự merge & kiểm tra chéo

- Hai chuỗi merge vào `develop` theo bất kỳ thứ tự nào. Trước mỗi PR: `git merge origin/develop` (hoặc rebase), chạy `./mvnw clean verify` + `npm run verify` — `ModuleBoundaryTest` phải xanh (catalog không import order / vendor).
- Người merge sau kiểm lại: số migration (mục 8.1), dòng `PUBLIC_REAL_API`, tổng quan CHECKLIST, `CLAUDE.md` dòng migration tiếp theo.
- Khi **cả hai** đã lên `develop`: chạy một kịch bản nối chuỗi — người bán đăng sản phẩm → Moderator duyệt → khách thêm giỏ → người bán sửa giá (giỏ báo đổi giá) → khách đặt VNPAY stub → người bán thử xóa sản phẩm (`409 GNE-CAT-4094`) → cron hủy phiên → xóa được. Ai merge sau chạy và ghi kết quả vào Nhật ký của plan mình.
