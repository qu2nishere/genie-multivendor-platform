# PLAN — Chuỗi C2 Giỏ → Checkout (UC-ORD-01 · UC-ORD-04 · UC-ORD-06 · UC-ORD-07 · UC-ORD-08)

| Mục | Nội dung |
|---|---|
| **Ngày lập** | 2026-11-25 |
| **Phụ trách** | _(chưa phân công — điền tên người nhận, rồi sửa dòng C2 ở `BAO_CAO_UC_DOC_LAP.md`, cột Phụ trách ở `CHECKLIST_TIEN_DO_UC.md` và `Quản Lý UseCase.xlsx`)_ |
| **Tiến độ** | [progress_gio_hang_checkout_c2.md](progress_gio_hang_checkout_c2.md) |
| **Mục tiêu** | 5 UC của chuỗi C2 từ ⬜ → ✅ (đủ 5 lớp GD / BE / Test / Nối / Chạy; UC-ORD-08 không có GD / Nối). Tổng quan dự kiến: Đã triển khai 17 → **22** / 87 |
| **Nguồn** | `BAO_CAO_UC_DOC_LAP.md` mục 6 (C2) · `Usecase.md` UC-ORD-01 (dòng 1004), 04 (1041), 06 (1074), 07 (1100), 08 (1138) · `API_SPEC.md` 5 (`GET /public/variants`, dòng 1128), 6.1–6.8, 6.10, 6.11, 15 · `LLD.md` 6.1 (SessionStatus), 7.2 (SD-02), 9.1 (giữ chỗ tồn kho), 10 (Cron) · `ENTITY.md` 3.1–3.2, 5.1–5.5, 7.3 |
| **Thứ tự ưu tiên khi mâu thuẫn** | TECH_STACK mục 12 → Usecase.md / ENTITY.md → Document_Vi → implement (CLAUDE.md mục 8) |
| **Người commit** | danh tính git đã cấu hình — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI (CLAUDE.md mục 4) |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `0e34dce`, 2026-11-25)

| Phần | Đã có | Còn thiếu |
|---|---|---|
| **Entity / bảng** | `Cart`, `CartItem` (`price_snapshot`, `added_at`), `CheckoutSession`, `Order` (304 dòng, có máy trạng thái, `order_code` UNIQUE), `OrderItem`, `OrderDiscount`, `StockReservation`; enum `SessionStatus` (5), `OrderStatus` (19), `StockReservationStatus`; migration `V3__cart`, `V5__order`. `ProductVariant.reserve / release / confirm` đã có | Repository rỗng (chỉ khai báo) |
| **Service / controller `order`** | Khung 7 dòng có `// TODO`: `CartController`, `CartService`, `CartMergeService`, `CheckoutController`, `CheckoutService`, `CheckoutFacade`, `ShippingFeeQuoter`, `ReservationService`, `ExpiredSessionSweeper` | Toàn bộ nghiệp vụ |
| **Dùng chung** | `ErrorCode` đã có `GNE-INV-4090/4091`, `GNE-ORD-4031/4092/4093/4094`; `common/idempotency/IdempotencyFilter` + `IdempotencyStore`; `AuditAction.ORDER_STATUS_CHANGED`; `JsonCache`; cổng `auth.deletion.application.AccountDeletionBlocker` (chờ module order cài) | Mã `GNE-INV-4093`, `GNE-INV-4094` (kiểm lại có trong `ErrorCode` chưa — O01.04) |
| **Vận chuyển** | `VendorShippingConfig` + `VendorShippingSettingsAdapter` (ngưỡng freeship, hãng bật); `VendorProfile.pickupProvinceId…` (cột có, **UC-VND-05 chưa làm** — Thiện); `auth/address/infra/GhnRegionDirectory` (gọi GHN danh mục hành chính); `.env.example` có `GHN_TOKEN`, `GHN_SHOP_ID` (đang tắt) | Client tính phí GHN, bảng phí dự phòng, cache |
| **Địa chỉ** | UC-AUTH-09 ✅ — **địa giới 2 cấp**: `provinceId` + `wardCode`; `districtId` chỉ còn ở địa chỉ cũ (NULL với địa chỉ mới) | ⚠ GHN fee v2 cần `to_district_id` — xem D4 |
| **Web** | `features/cart` (`fetchCart`, `addCartItem`, `updateCartItem`, `removeCartItems`, `addCartItemsBatch`, `mergeCart`, `resolveGuestItems`, `getVariantChoice`), `features/checkout` (`createCheckout`, `getCheckoutSession`, `retryPayment`, `settleMockPayment`, `getShipPolicies`) — tất cả chạy trên mock; handoff dòng 44 (`/public/shops/shipping-policies` không có trong spec), 257–299 | Nối API thật qua cờ `PUBLIC_REAL_API` (`cart`, `checkout`) |

**Phụ thuộc theo báo cáo (BAO_CAO mục 6 C2):** *Tính phí vận chuyển* cần *Sổ địa chỉ* (✅) + *Cấu hình kho lấy hàng* UC-VND-05 (⬜, Thiện). *Tạo phiên Checkout* «include» *Xem trước đơn hàng* «include» *Tính phí vận chuyển* → làm đúng thứ tự **ORD-01 → ORD-06 → ORD-04 → ORD-07 → ORD-08**.

**Ngoài phạm vi (ghi lại để không quên):**
- Áp voucher / Điểm khi mua (UC-PRO-04, PRO-10), `POST /me/checkout/applicable-vouchers` (6.9), Flash Sale (PRO-06) → **C6**. C2 chỉ để sẵn cổng (D2).
- Khởi tạo thanh toán VNPay / VietQR thật, IPN, ký quỹ (6.12, UC-PAY-*) → **C3**. C2 dùng cổng thanh toán giả lập (D3).
- Hủy nhóm đơn do khách (6.13, UC-ORD-10), danh sách / chi tiết đơn mua (UC-ORD-09) → **C5**.
- Điều phối đa hãng (UC-LOG-15) → **C4**. C2 chỉ tính phí GHN.
- Cấu hình kho lấy hàng trên giao diện Seller (UC-VND-05) → plan B4 của Thiện. C2 chỉ **đọc** `vendor_profiles.pickup_*` và seed sẵn cho shop demo (D5).

---

## 1. Nguyên tắc bắt buộc

1. **Tài liệu sửa trước, code sau** (TECH_STACK 7.3): thay đổi hành vi / hợp đồng API thì sửa `Usecase.md` / `API_SPEC.md` / `LLD.md` trước, commit `docs(order): …` riêng ở đầu mỗi nhánh.
2. **Ranh giới module** (TS 4.4, `ModuleBoundaryTest`): `order` chỉ gọi module khác qua `*Facade` / cổng ở tầng `application` (`ProductFacade`/`VariantFacade` của `catalog`, `ShopFacade` của `vendor`, `AddressFacade` của `auth`, `ShopShippingSettings`); không import `domain` / `infra` module khác; không tạo vòng phụ thuộc. Cổng mà module **sau** (promotion, payment) sẽ cài thì đặt ở `order.*.application` với bản mặc định `@ConditionalOnMissingBean` (giống `ShopVoucherAvailability`).
3. **Tiền** dùng `BigDecimal` (làm tròn đồng, `HALF_UP`); **server luôn tính lại giá**, không tin giá client.
4. **Giữ chỗ tồn kho** đúng LLD 9.1: `UPDATE … WHERE stock_count - reserved_count >= :qty` (0 dòng → `GNE-INV-4090`), không đọc-rồi-ghi. Sắp xếp `variant_id` tăng dần trước khi giữ chỗ để tránh deadlock.
5. **Cron** (TS 10.3): `@Scheduled` + `FOR UPDATE SKIP LOCKED`, theo lô, mỗi phiên một transaction, lỗi một phiên không dừng phiên khác, log kết quả (mẫu: `SuspensionExpiryJob`, `KycSupplementExpiryJob`).
6. **Sự kiện / email / cache chạy sau commit** (`@TransactionalEventListener(AFTER_COMMIT)`).
7. **Không đổi tên bảng / cột / enum / mã UC** (NFR-12). Migration mới (nếu cần) bắt đầu từ **`V25`** — báo nhóm giữ số trước khi làm (BAO_CAO mục 7). ENTITY.md sửa trước.
8. **Mã lỗi** chỉ dùng mã có trong API_SPEC 15; cần mã mới thì thêm vào API_SPEC 15 + `ErrorCode` + `web/src/shared/api/error-codes.ts` + khóa `errors.GNE-*` (vi, en) cùng một commit.
9. **Web** (CLAUDE.md mục 5): sửa theo 4 tầng, giữ nhánh mock chạy được; chuỗi qua `t()`; màn đổi giao diện kiểm 9 theme.
10. **Cổng kiểm tra trước mỗi COMMIT**: `cd server-spring && ./mvnw verify` (JDK 21); đụng `web/` thì `cd web && npm run verify`.
11. **Chỉ tick Chạy khi người dùng tự thao tác trên giao diện với hệ thống thật** (CLAUDE.md mục 4.4). Claude gọi API thì ghi "API đúng".
12. **Commit / PR** theo CLAUDE.md mục 4: `<type>(<scope>): <subject>` tiếng Anh, chữ thường; scope `order` (backend), `shipping`, `web`, `db`, `docs`; `git add` đúng từng file; chỉ đưa lệnh commit khi người dùng yêu cầu.

---

## 2. Quyết định cần chốt trước khi làm

| Mã | Câu hỏi | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Nhánh & thứ tự PR** | CLAUDE.md mục 4; PR ≤ ~400 dòng | 5 nhánh **xếp chồng** (cùng sửa file progress), tạo từ `develop` mới nhất: `feature/c2-cart` (ORD-01) → `feature/c2-shipping-fee` (ORD-06) → `feature/c2-checkout-preview` (ORD-04) → `feature/c2-checkout-session` (ORD-07) → `feature/c2-order-sweeper` (ORD-08). Mỗi nhánh tạo từ nhánh trước; PR vào `develop` đúng thứ tự, gộp xong nhánh trước thì `git rebase develop` nhánh sau. Nhánh nào vượt ~400 dòng thì tách PR backend / web | ☐ |
| D2 | **Voucher & Điểm trong C2** | 6.8 trả `vouchers_applied`, `best_selection`, `coins`; UC-PRO-04/10 thuộc C6 | C2 **không áp giảm giá**: cổng `order.checkout.application.CheckoutDiscountPolicy` bản mặc định trả 0 → `vouchers_applied = null`, `best_selection = null`, `coins = null`, các khoản giảm = 0. Request vẫn nhận `vouchers`, `use_coins` (bỏ qua). Web khi `PUBLIC_REAL_API` có `checkout` mà chưa có `voucher` thì ẩn ô Voucher / Điểm. C6 cài cổng này | ☐ |
| D3 | **Thanh toán trong C2** (C3 chưa có) | 6.10 khối `payment`; LLD 6.1; `OrderStatus.PENDING_PAYMENT` = "COD: chờ shop xác nhận" | **COD làm trọn**: trong cùng transaction phiên → `COMPLETED`, giữ chỗ → `CONFIRMED` (`stock_count -= q`, `reserved_count -= q`), đơn con giữ `PENDING_PAYMENT` (chờ shop xác nhận — C4). **VNPAY / VIETQR**: phiên → `PAYMENT_PENDING`, khối `payment` lấy từ cổng `order.checkout.application.PaymentInitiator`; bản mặc định (`PAYMENT_MODE=stub`) trả `{ method, payment_url: null }` / `vietqr: null` → đủ để kiểm cron ORD-08 bằng đơn online bỏ dở. C3 cài cổng thật | ☐ |
| D4 | **GHN với địa giới 2 cấp** — fee v2 cần `to_district_id`, địa chỉ mới không có `districtId` | UC-ORD-06 bước 3; AUTH D25 (2 cấp, GHN v3) | **Spike O05.01** trước khi code: kiểm API phí GHN nhận `to_ward_code` không cần quận (bản v3 / tham số mới). Nếu được → dùng. Nếu không → dùng `districtId` khi địa chỉ có, còn lại tính **bảng phí dự phòng** (cùng tỉnh 15.000đ / khác tỉnh 30.000đ) + cờ `is_estimated_fee = true`. Ghi kết quả spike vào progress mục 7 | ☐ |
| D5 | **Shop chưa cấu hình kho lấy hàng** (UC-VND-05 chưa xong) | UC-ORD-06 tiền điều kiện | (a) Seed `pickup_*` + `ghn_shop_id` cho các shop demo trong `R__seed_demo_30_shops.sql` (báo Thiện — chủ file seed). (b) Shop thật chưa có kho → **phí dự phòng liên tỉnh 30.000đ**, `is_estimated_fee = true` (không chặn mua — không có `unavailable_reason` phù hợp, không thêm enum). Khi VND-05 xong không phải sửa C2 | ☐ |
| D6 | **Chế độ GHN** | mẫu `AI_WORKER_MODE=stub\|http` | `GHN_MODE=stub\|http` (mặc định `stub` ở profile local / test). `http`: `RestClient` timeout 3 giây, gói "Chuẩn", cache Redis `ship:fee:{sha256(tham số)}` 10 phút (`JsonCache`; Redis lỗi → gọi thẳng); lỗi / timeout → bảng dự phòng. GHN báo không phục vụ tuyến → `unavailable_reason = "ROUTE_NOT_SUPPORTED"`? → **cần chốt**: thêm giá trị mới vào API 6.1 hay dùng `PRODUCT_UNAVAILABLE`. Đề xuất thêm `SHIPPING_UNAVAILABLE` vào API 6.8 (chỉ ở preview, không phải enum CSDL) | ☐ |
| D7 | **Khối lượng / freeship** | `Product.weight` NULLABLE; UC-ORD-06 bước 5; `Product.shippingPayer` | Khối lượng NULL → 500 g / sản phẩm (`genie.shipping.default-weight-grams`). Tạm tính shop ≥ `freeshipThreshold` hoặc mọi sản phẩm `shippingPayer = SELLER` → `shipping_fee` khách trả = 0, `is_free_shipping_by_shop = true` (ghi trợ giá vào ký quỹ để C3) | ☐ |
| D8 | **Giỏ khách vãng lai & gộp giỏ** | UC-ORD-01 luồng thay thế; API 6.7 chỉ trả `{merged, cart_item_count}` nhưng Usecase bước 4 yêu cầu trả dòng bị điều chỉnh | Giỏ tạm giữ ở `localStorage` (web đã có); `GET /public/variants?ids=` (tối đa 50) làm trong nhánh `c2-cart`. 6.7 trả thêm `skipped: [{variant_id, reason}]` và `adjusted: [{variant_id, quantity}]` (giống 6.6) — **sửa API_SPEC trước** | ☐ |
| D9 | **Giới hạn giỏ** | không có trong spec | Tối đa **100 dòng / giỏ**, **999 / dòng** → vượt trả `422` có `errors[]` (`InvalidRequestException`). Ghi vào API 6.3 | ☐ |
| D10 | **Mã đơn `TR-yyyyMMdd-######`** | UC-ORD-07 bước 4 (Redis `INCR order:seq:{yyyyMMdd}`) | Redis `INCR` + `EXPIRE` 48 giờ, ngày theo giờ VN; Redis lỗi → `503 GNE-SYS-5030` (client thử lại cùng `Idempotency-Key`). Mã phiên `CS-xxxxx` (base32 5 ký tự, UNIQUE) | ☐ |
| D11 | **Idempotency đặt hàng** | API 6.10 🔁 | Dùng `common/idempotency/IdempotencyFilter` sẵn có cho `POST /me/checkout-sessions`; IT gửi 2 lần cùng key → 1 phiên | ☐ |
| D12 | **Hoa hồng chụp vào `order_items`** | UC-ORD-07 bước 4 | `VendorProfile.effectiveCommissionRate(categoryRate)` qua `ShopFacade` + `CategoryFacade`: shop → ngành → 5% | ☐ |
| D13 | **Cron hủy phiên (ORD-08)** | Usecase: 60 giây, `created_at < now-15'`; LLD 10 | `@Scheduled(fixedDelayString = "PT60S", initialDelayString = "PT30S")`; chọn phiên `INITIATED / PAYMENT_PENDING` có `reservation_expires_at <= now` (tương đương created_at+15', khớp hạn hiện ở web), lô 50, `SKIP LOCKED`, mỗi phiên 1 transaction. Nhả giữ chỗ **có điều kiện** (`WHERE status = 'ACTIVE'`) để không đua với IPN (C3). Hoàn voucher / Điểm / suất Flash Sale qua cổng `CheckoutCompensation` — bản mặc định không làm gì (C6 cài). Ghi `ORDER_STATUS_CHANGED` `details.reason = PAYMENT_TIMEOUT`, actor = hệ thống | ☐ |
| D14 | **Chặn xóa tài khoản** | BAO_CAO mục 3 (UC-AUTH-10) | Module order cài `AccountDeletionBlocker`: còn phiên `INITIATED / PAYMENT_PENDING` hoặc đơn chưa ở trạng thái kết thúc → chặn (`GNE-USR-4091`) | ☐ |
| D15 | **Migration** | — | Dự kiến **không cần** (bảng đủ cột). Nếu cần index (`checkout_sessions(status, reservation_expires_at)`, `cart_items(cart_id, product_variant_id)` UNIQUE) → `V25__order_checkout_indexes.sql`, giữ số với nhóm trước | ☐ |

---

## 3. Luồng nghiệp vụ chi tiết

### 3.1 Giỏ hàng (UC-ORD-01)

```
POST /me/cart/items {variant_id, quantity}
  ├─ chặn STAFF / ADMIN (đã có ở tầng bảo mật theo đường dẫn — GNE-ORD-4031)
  ├─ VariantFacade.lineInfo(variantId): product PUBLISHED? → không: 404 / PRODUCT_UNAVAILABLE
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
  │   ├─ INSERT checkout_sessions (INITIATED, code CS-…, grand_total, payment_method, reservation_expires_at = now+15')
  │   ├─ mỗi shop: order_code (D10) → INSERT orders (PENDING_PAYMENT, shipping_address_snapshot_json, buyer_note, sub_total, shipping_fee…)
  │   │            INSERT order_items (snapshot tên, SKU, unit_price, commission_rate — D12)
  │   ├─ variant_id tăng dần: UPDATE product_variants … reserved_count += q WHERE stock_count - reserved_count >= q
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
    sessions = SELECT … WHERE status IN ('INITIATED','PAYMENT_PENDING') AND reservation_expires_at <= now
               ORDER BY reservation_expires_at LIMIT 50 FOR UPDATE SKIP LOCKED
    mỗi phiên (try/catch riêng):
      UPDATE stock_reservations SET status='EXPIRED', released_at=now WHERE id=? AND status='ACTIVE'
         1 dòng → UPDATE product_variants SET reserved_count = reserved_count - q WHERE id=? AND reserved_count >= q
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
└── order/application/OrderAccountDeletionBlocker.java                       (D14)
server-spring/src/main/java/com/genie/shipping/fee/
├── application/{ShippingFeeQuoter → chuyển từ order.checkout hoặc giữ cổng ở order, FlatFeeTable, Parcel}.java
└── infra/{GhnFeeClient, ShippingFeeConfig}.java                            (GHN_MODE)
server-spring/src/main/java/com/genie/catalog/product/application/VariantFacade.java     (sửa / mới — thông tin dòng giỏ, giữ chỗ)
server-spring/src/main/java/com/genie/vendor/shop/application/ShopFacade.java           (sửa — vacation, owner, pickup, commission)
server-spring/src/main/resources/
├── application.yml                       (genie.shipping.*, genie.checkout.*, GHN_MODE)
└── db/seed/R__seed_demo_30_shops.sql     (pickup + ghn_shop_id — D5)
server-spring/src/test/java/com/genie/order/
├── cart/{CartIT, CartMergeIT, PublicVariantsIT}.java
├── checkout/{CheckoutPreviewIT, CheckoutSessionIT, CheckoutConcurrencyIT, CheckoutPricerTest}.java
├── reservation/ExpiredSessionSweeperIT.java
└── OrderSecurityIT.java                  (STAFF/ADMIN 403, truy cập chéo 404)
server-spring/src/test/java/com/genie/shipping/fee/{FlatFeeTableTest, GhnFeeClientTest (WireMock / MockRestServiceServer)}.java
web/src/features/cart/…, web/src/features/checkout/… (mapper / api / ui theo D2, D6, D8) · .env.example (GHN_MODE, PAYMENT_MODE)
docs/: API_SPEC 6.x, Usecase.md (Hiện trạng 5 UC), LLD 10, web-backend-handoff.md, kiem-thu/kich-ban-<ngày>-c2.md, CHECKLIST, BAO_CAO, Excel
```

---

## 5. Task

> Mã task `Oxx.yy`. Mỗi task `COMMIT` chỉ làm sau khi `./mvnw verify` (+ `npm run verify` nếu đụng web) xanh. Sau **mỗi** task: tick ở progress mục 4 + một dòng **Nhật ký** (mục 6 của progress).

### O00 — Chuẩn bị (nhánh `develop`)
- **O00.01** Chốt D1–D15 với nhóm (đặc biệt D3 với người làm C3, D5 với Thiện, D2 với người làm C6), ghi vào progress mục 3.
- **O00.02** `git switch develop && git pull` — xác nhận sạch (`git status`), ghi mã commit gốc vào progress.
- **O00.03** Giữ số migration `V25` (nếu D15 cần) — báo nhóm.
- **O00.04** Dựng môi trường: `docker compose … up -d`, JDK 21, `./mvnw verify` + `npm run verify` trên `develop` để có mốc xanh; ghi số test vào progress mục 7.
- **O00.05** Ghi tên người phụ trách vào BAO_CAO (dòng C2), CHECKLIST (cột Phụ trách 5 UC), Excel. COMMIT `docs: assign c2 cart and checkout chain` (có thể gộp vào O01.05).

### O01 — Nhánh `feature/c2-cart`: tài liệu trước (UC-ORD-01)
- **O01.01** `git switch -c feature/c2-cart` (từ `develop`).
- **O01.02** `API_SPEC.md` 6.3 (giới hạn D9), 6.7 (`skipped`, `adjusted` — D8).
- **O01.03** `web-backend-handoff.md`: ghi rõ các hàm `cart` sẽ nối.
- **O01.04** Kiểm `ErrorCode` có `GNE-INV-4093`, `GNE-INV-4094` chưa; thiếu thì thêm (kèm `error-codes.ts`, khóa vi / en).
- **O01.05** COMMIT `docs(order): specify cart limits and merge result`.

### O02 — `feature/c2-cart`: backend + test
- **O02.01** Cổng đọc từ `catalog` (`VariantFacade.lineInfos(ids)` nạp theo lô: product status, giá, giá gốc, ảnh, slug, tồn khả dụng, vendorId) + `vendor` (`ShopFacade`: mini shop, vacation, owner).
- **O02.02** Repository + `CartService` (thêm, xem, mini-cart, đổi số lượng / phân loại + gộp, xóa nhiều, batch).
- **O02.03** `CartMergeService` + `GET /public/variants`.
- **O02.04** `CartController` (6.1–6.7) + mapper đúng snake_case API.
- **O02.05** Test: `CartIT` (cộng dồn, vượt tồn `4094`, tự mua `4094`, shop nghỉ `4093`, sản phẩm ẩn, đổi phân loại gộp dòng, xóa dòng người khác bị bỏ qua, giới hạn D9), `CartMergeIT`, `PublicVariantsIT`, `OrderSecurityIT` (STAFF / ADMIN `403 GNE-ORD-4031`, khách chưa đăng nhập `401`).
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
- **O05.05** Seed `pickup_*` + `ghn_shop_id` shop demo (D5) — gửi Thiện xem.
- **O05.06** Test: `FlatFeeTableTest`, `ShippingFeeQuoterTest` (freeship, thiếu kho, khối lượng NULL), `GhnFeeClientTest` (giả GHN: OK, timeout → dự phòng, tuyến không hỗ trợ, cache trúng).
- **O05.07** COMMIT `feat(shipping): quote ghn shipping fee with flat fallback`; COMMIT `chore(db): seed pickup addresses for demo shops`.
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
- **O07.02** Tài liệu: `API_SPEC.md` 6.10 ghi D3 (COD hoàn tất ngay, online `PAYMENT_PENDING` với cổng giả lập), `LLD.md` 6.1 khớp; migration `V25` nếu D15. COMMIT `docs(order): specify checkout session before payment gateway`.
- **O07.03** `OrderCodeGenerator` (D10), `ReservationService.reserveAll / confirmAll` (LLD 9.1).
- **O07.04** `CheckoutService.place` (một transaction, snapshot, hoa hồng D12, xóa giỏ, COD / online D3), `PaymentInitiator` stub, `CheckoutCreatedEvent`.
- **O07.05** `POST /me/checkout-sessions` (Idempotency D11), `GET /me/checkout-sessions/{id}`.
- **O07.06** `OrderAccountDeletionBlocker` (D14).
- **O07.07** Test: `CheckoutSessionIT` (2 shop → 2 đơn, snapshot không đổi khi sửa giá sau đó, giỏ bị xóa đúng dòng, COD → `COMPLETED` + tồn giảm, VNPAY → `PAYMENT_PENDING` + `reserved_count` tăng, hết hàng → `4090` + rollback toàn bộ, shop nghỉ `4093`, tự mua `4094`, Idempotency trùng key), `CheckoutConcurrencyIT` (2 luồng tranh 1 sản phẩm tồn 1 → chỉ 1 thành công), `OrderCodeGeneratorTest`, `AccountDeletionGuardIT` bổ sung.
- **O07.08** Web: `createCheckout` / `getCheckoutSession` thật; trang kết quả COD / online-stub; xử lý `4090` (gỡ dòng + gọi lại preview). Bật cờ `checkout`.
- **O07.09** COMMIT `feat(order): persist checkout session and per-shop orders`; COMMIT `feat(order): block account deletion with open orders`; COMMIT `feat(web): place orders through real checkout api`.
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
- **O10.03** `Usecase.md` *Hiện trạng* 5 UC + Excel cột *Hiện trạng mã nguồn* + BAO_CAO dòng C2 (tiến độ) + `CLAUDE.md` mục 2 (đoạn "Plan 2026-11-25").
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
| R2 | UC-VND-05 (Thiện) đổi cấu trúc `pickup_*` | C2 chỉ đọc qua `ShopFacade`; báo Thiện trước khi merge O05 |
| R3 | C3 / C6 làm song song đụng `CheckoutService` | Cổng `PaymentInitiator`, `CheckoutDiscountPolicy`, `CheckoutCompensation` chốt chữ ký ngay O06 / O07 và báo nhóm |
| R4 | Deadlock khi giữ chỗ nhiều SKU | Sắp `variant_id` tăng dần; `CheckoutConcurrencyIT` |
| R5 | Trùng số migration với nhánh khác | O00.03 giữ số `V25` |
| R6 | PR vượt ~400 dòng | Tách PR backend / web trong cùng nhánh |
