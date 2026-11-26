# PROGRESS — Chuỗi C2 Giỏ → Checkout

> Plan: [plan_gio_hang_checkout_c2.md](plan_gio_hang_checkout_c2.md) · Ngày lập: 2026-11-25 · Phụ trách: _(chưa phân công)_
> Chạy song song với C1 Đăng bán — ranh giới ở **plan mục 8** (migration chung `V25` đã tạo, file sở hữu, luật ghi `product_variants`, cổng `ProductOrderGuard`).
> Cách dùng: xong task nào thì đổi `- [ ]` → `- [x]`, điền cột **Commit / PR** ở mục 2 và ghi **một dòng Nhật ký** ở mục 6 (ngày, mã task, việc, mã commit / nhánh). Kịch bản chạy thật ghi **Đạt / Không đạt** vào mục 5. Task không cần làm ghi `- [x] ~~…~~ (không cần: lý do)`. Đồng thời cập nhật `docs/Document/CHECKLIST_TIEN_DO_UC.md` khi xong một lớp của UC (CLAUDE.md mục 4).

## 1. Tổng quan

| UC | GD | BE | Test | Nối | Chạy | Trạng thái hiện tại | Mục tiêu |
|---|:---:|:---:|:---:|:---:|:---:|---|---|
| UC-ORD-01 Giỏ hàng | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai | ✅ |
| UC-ORD-06 Phí vận chuyển GHN | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai | ✅ |
| UC-ORD-04 Xem trước & tách giỏ | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai | ✅ |
| UC-ORD-07 Phiên checkout & đơn con | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 Đang triển khai | ✅ |
| UC-ORD-08 Cron hủy đơn quá hạn | — | ☑ | ☑ | — | ☐ | 🟡 Đang triển khai | ✅ |

**Tổng quan dự án (CHECKLIST 2026-11-25):** Đã triển khai 24 / 87 → mục tiêu **29** / 87 khi đóng plan (đếm lại từ các dòng UC lúc đóng — C1 có thể đã đổi số).

## 2. Tiến độ theo giai đoạn

| Giai đoạn | Nhánh | Việc | Số task | Trạng thái | Commit / PR |
|---|---|---|---:|:---:|---|
| O00 | `develop` | Chuẩn bị, chốt quyết định | 5 | ◐ | gốc `develop` @ `c108096`; D1–D16 áp theo đề xuất, chờ nhóm xác nhận |
| O01 | `feature/c2-cart` | ORD-01: tài liệu trước | 5 | ☑ | `07f49f5` |
| O02 | `feature/c2-cart` | ORD-01: backend + test | 6 | ☑ | `845b880` |
| O03 | `feature/c2-cart` | ORD-01: nối web | 4 | ☑ | `0c108ff` |
| O04 | `feature/c2-cart` | ORD-01: kiểm tra & PR | 3 | ☐ | |
| O05 | `feature/c2-shipping-fee` | ORD-06: phí GHN + dự phòng | 8 | ◐ | `06a24fc`, `0a72e65` (chưa push / PR) |
| O06 | `feature/c2-checkout-preview` | ORD-04: xem trước & tách giỏ | 7 | ◐ | `e656097`, `8f6c0e4`, `de696ca` (chưa push / PR) |
| O07 | `feature/c2-checkout-session` | ORD-07: đặt hàng | 10 | ◐ | `c32ab06`, `c4467f8`, `df1fb03`, `d81c428` (chưa push / PR) |
| O08 | `feature/c2-order-sweeper` | ORD-08: cron hủy quá hạn | 6 | ◐ | `d703a78`, `d9c6d19` (chưa push / PR) |
| O09 | nhánh cuối | Chạy thật trên giao diện | 6 | ◐ | kịch bản đã viết — chờ người dùng tự chạy |
| O10 | nhánh cuối | Hồi quy & đóng plan | 4 | ◐ | O10.01 phần backend + web xong; E2E, ✅ UC chờ chạy thật |

**Tổng:** 64 task · 13 kịch bản chạy thật.

**Sơ đồ nhánh (D1 — xếp chồng, PR vào `develop` đúng thứ tự):**
```
develop ─┬─ feature/c2-cart ─── feature/c2-shipping-fee ─── feature/c2-checkout-preview ─── feature/c2-checkout-session ─── feature/c2-order-sweeper
         │   (PR #1)              (PR #2)                      (PR #3)                          (PR #4)                          (PR #5)
         └─ gộp PR #n xong → git switch <nhánh n+1> && git rebase develop → mở PR #n+1
```

| Nhánh | Tạo từ | Ngày tạo | PR | Ngày gộp |
|---|---|---|---|---|
| `feature/c2-cart` | `develop` @ `c108096` | 2026-11-26 | | |
| `feature/c2-shipping-fee` | `feature/c2-cart` | 2026-11-26 | | |
| `feature/c2-checkout-preview` | `feature/c2-shipping-fee` | 2026-11-26 | | |
| `feature/c2-checkout-session` | `feature/c2-checkout-preview` | 2026-11-26 | | |
| `feature/c2-order-sweeper` | `feature/c2-checkout-session` | 2026-11-26 | | |

## 3. Quyết định (chép kết quả từ plan mục 2)

| Mã | Chủ đề | Chốt | Ngày | Ai chốt |
|---|---|:---:|---|---|
| D1 | Nhánh & thứ tự PR | ◐ Làm đúng 5 nhánh xếp chồng; chưa push / PR (chờ người dùng) | 2026-11-26 | Claude (áp đề xuất — chờ nhóm xác nhận) |
| D2 | Voucher & Điểm trong C2 (cổng mặc định 0) | ◐ Cổng `CheckoutDiscountPolicy` mặc định 0; web ẩn Voucher / Điểm khi `checkout` thật mà chưa có `voucher` | 2026-11-26 | Claude (áp đề xuất — chờ nhóm xác nhận) |
| D3 | Thanh toán trong C2 (COD trọn, online stub) | ◐ COD → phiên `COMPLETED` + chốt tồn; online → `PAYMENT_PENDING`, cổng `PaymentInitiator` giả lập | 2026-11-26 | Claude (áp đề xuất — chờ nhóm xác nhận) |
| D4 | GHN với địa giới 2 cấp (spike) | ◐ Chưa spike được (không có `GHN_TOKEN`) → nhánh dự phòng: GHN chỉ khi hai đầu có mã quận | 2026-11-26 | Claude (áp đề xuất — chờ nhóm xác nhận) |
| D5 | Shop chưa cấu hình kho lấy hàng (VND-05 ✅ — không sửa seed, phí dự phòng cho sản phẩm cũ) | ◐ Không sửa seed (đã đủ kho); shop chưa có kho → 30.000đ tạm tính | 2026-11-26 | Claude (áp đề xuất — chờ nhóm xác nhận) |
| D6 | Chế độ GHN `stub\|http`, `SHIPPING_UNAVAILABLE` | ◐ `GHN_MODE=stub|http` đọc thẳng biến môi trường (không sửa `application.yml`); thêm `SHIPPING_UNAVAILABLE` ở 6.8 | 2026-11-26 | Claude (áp đề xuất — chờ nhóm xác nhận) |
| D7 | Khối lượng (gram, mặc định 500 g chỉ cho dữ liệu cũ) / freeship chỉ cấp shop | ◐ Theo đề xuất; mặc định 500 g là hằng `VariantFacade.DEFAULT_WEIGHT_GRAMS` (không thêm khóa yml) | 2026-11-26 | Claude (áp đề xuất — chờ nhóm xác nhận) |
| D8 | Giỏ vãng lai & kết quả gộp giỏ | ◐ Theo đề xuất (API 6.7 thêm `skipped`, `adjusted`) | 2026-11-26 | Claude (áp đề xuất — chờ nhóm xác nhận) |
| D9 | Giới hạn giỏ (100 dòng, 999 / dòng) | ◐ Theo đề xuất (100 dòng / 999) | 2026-11-26 | Claude (áp đề xuất — chờ nhóm xác nhận) |
| D10 | Mã đơn `TR-yyyyMMdd-######` | ◐ Theo đề xuất + nối tiếp số lớn nhất trong CSDL khi Redis mất khóa đếm; `CS-` suy từ id (không thêm cột) | 2026-11-26 | Claude (áp đề xuất — chờ nhóm xác nhận) |
| D11 | Idempotency đặt hàng | ◐ `IdempotencyFilter` sẵn có chỉ là khung rỗng → hiện thực `IdempotencyStore` (Redis `SET NX`), gọi trong `CheckoutService` | 2026-11-26 | Claude (áp đề xuất — chờ nhóm xác nhận) |
| D12 | Hoa hồng chụp vào `order_items` | ◐ Theo đề xuất | 2026-11-26 | Claude (áp đề xuất — chờ nhóm xác nhận) |
| D13 | Cron hủy phiên quá hạn | ◐ Theo đề xuất (mỗi phiên một transaction, lô 50 id) | 2026-11-26 | Claude (áp đề xuất — chờ nhóm xác nhận) |
| D14 | Chặn xóa tài khoản + cài `ProductOrderGuard` của C1 | ◐ Theo đề xuất; `ProductOrderGuard` tạo nguyên văn mục 8.4 (C1 chưa lên `develop`) | 2026-11-26 | Claude (áp đề xuất — chờ nhóm xác nhận) |
| D15 | Migration — đã có 2 index trong `V25` chung với C1 | ☑ | 2026-11-26 | chủ dự án |
| D16 | Ghi `product_variants` (tăng `version`, lọc trạng thái) & luật "mua được" — plan mục 8.3, 8.5 | ◐ Theo đề xuất (`VariantFacade` + test 4090 biến thể đã bỏ / sản phẩm ẩn) | 2026-11-26 | Claude (áp đề xuất — chờ nhóm xác nhận) |

## 4. Task

### O00 — Chuẩn bị (`develop`)
- [ ] **O00.01** Đọc plan mục 8, xác nhận với người làm C1; chốt D1–D16, ghi mục 3
- [x] **O00.02** `git switch develop && git pull`, ghi mã commit gốc vào mục 2
- [x] **O00.03** Duyệt PR schema `V25` (plan C1 P00.06); vào `develop` rồi mới tạo nhánh — `V25` đã có trên `develop` (`36c7872`)
- [ ] **O00.04** Dựng môi trường + `./mvnw verify` + `npm run verify` mốc xanh, ghi mục 7
- [ ] **O00.05** Ghi người phụ trách vào BAO_CAO / CHECKLIST / Excel

### O01 — `feature/c2-cart`: tài liệu trước
- [x] **O01.01** Tạo nhánh `feature/c2-cart` từ `develop`
- [x] **O01.02** `API_SPEC.md` 6.3 (D9), 6.7 (D8)
- [x] **O01.03** `web-backend-handoff.md` mục `cart`
- [x] **O01.04** Thêm `GNE-INV-4093/4094`, `GNE-ORD-4093/4094` (ErrorCode khối đơn hàng + web + i18n)
- [x] **O01.05** COMMIT `docs(order): specify cart limits and merge result`

### O02 — `feature/c2-cart`: backend + test
- [x] **O02.01** `VariantFacade` (file mới, JdbcTemplate, luật D16) + `ShopFacade` (vendor) cho dòng giỏ
- [x] **O02.02** Repository + `CartService`
- [x] **O02.03** `CartMergeService` + `GET /public/variants`
- [x] **O02.04** `CartController` 6.1–6.7
- [x] **O02.05** `CartIT`, `CartMergeIT`, `PublicVariantsIT`, `OrderSecurityIT`
- [x] **O02.06** COMMIT `feat(order): implement shopping cart api`

### O03 — `feature/c2-cart`: nối web
- [x] **O03.01** `cart.mapper.ts` / `cart.api.ts` nhánh thật, cảnh báo đổi giá
- [x] **O03.02** Toast gộp giỏ theo `skipped` / `adjusted`
- [x] **O03.03** Cờ `cart` trong `PUBLIC_REAL_API` + docs cấu hình
- [x] **O03.04** COMMIT `feat(web): connect cart to real api`

### O04 — `feature/c2-cart`: kiểm tra & PR
- [ ] **O04.01** `docs/api-tests/order-cart.http` — API đúng (file đã viết; IT đã phủ các ca, chưa gọi trên backend chạy thật)
- [x] **O04.02** CHECKLIST / Usecase / Excel UC-ORD-01 + COMMIT `docs: record c2 cart progress` (Excel dồn vào O10.03 — tệp nhị phân, plan mục 8.2.3)
- [ ] **O04.03** Push + PR vào `develop` (chờ người dùng)

### O05 — `feature/c2-shipping-fee` (UC-ORD-06)
- [x] **O05.01** Tạo nhánh từ `feature/c2-cart` + spike GHN 2 cấp (D4), ghi mục 7
- [x] **O05.02** Tài liệu 6.8 / UC-ORD-06 / `.env.example` + COMMIT `docs(shipping): specify shipping fee quote and fallback`
- [x] **O05.03** `FlatFeeTable`, `Parcel`, `ShippingFeeQuoter`
- [x] **O05.04** `GhnFeeClient` + cache 10 phút
- [x] **O05.05** Kiểm seed shop demo đã đủ kho lấy hàng + `ghn_shop_id` (không sửa file có sẵn)
- [x] **O05.06** `FlatFeeTableTest`, `ShippingFeeQuoterTest`, `GhnFeeClientTest`
- [x] **O05.07** COMMIT `feat(shipping): quote ghn shipping fee with flat fallback` (+ seed file mới nếu O05.05 cần)
- [ ] **O05.08** CHECKLIST UC-ORD-06, push + PR

### O06 — `feature/c2-checkout-preview` (UC-ORD-04)
- [x] **O06.01** Tạo nhánh từ `feature/c2-shipping-fee`
- [x] **O06.02** Tài liệu 6.8 (D2) + handoff dòng 44 + COMMIT `docs(order): clarify checkout preview before vouchers`
- [x] **O06.03** `CheckoutDiscountPolicy` + `CheckoutPricer`
- [x] **O06.04** `POST /me/checkout/preview`
- [x] **O06.05** `CheckoutPricerTest`, `CheckoutPreviewIT`
- [x] **O06.06** Web preview thật + COMMIT `feat(order): add checkout preview with per-shop split` + `feat(web): use real checkout preview`
- [ ] **O06.07** CHECKLIST UC-ORD-04 (+ Nối ORD-06), push + PR

### O07 — `feature/c2-checkout-session` (UC-ORD-07)
- [x] **O07.01** Tạo nhánh từ `feature/c2-checkout-preview`
- [x] **O07.02** Tài liệu 6.10 / LLD 6.1 (D3), migration nếu D15 + COMMIT `docs(order): specify checkout session before payment gateway`
- [x] **O07.03** `OrderCodeGenerator`, `ReservationService`
- [x] **O07.04** `CheckoutService.place`, `PaymentInitiator` stub, `CheckoutCreatedEvent`
- [x] **O07.05** `POST /me/checkout-sessions` (Idempotency), `GET /me/checkout-sessions/{id}`
- [x] **O07.06** `OrderAccountDeletionBlocker` + `OrderProductGuard` (cổng C1)
- [x] **O07.07** `CheckoutSessionIT`, `CheckoutConcurrencyIT` (+ biến thể đã bỏ / sản phẩm ẩn → 4090), `OrderProductGuardIT`, `OrderCodeGeneratorTest`, `AccountDeletionGuardIT`
- [x] **O07.08** Web đặt hàng thật + trang kết quả + xử lý `4090`, cờ `checkout`
- [x] **O07.09** COMMIT `feat(order): persist checkout session and per-shop orders` + `feat(order): block account and product deletion with open orders` + `feat(web): place orders through real checkout api`
- [ ] **O07.10** CHECKLIST UC-ORD-07, push + PR

### O08 — `feature/c2-order-sweeper` (UC-ORD-08)
- [x] **O08.01** Tạo nhánh từ `feature/c2-checkout-session`
- [x] **O08.02** Tài liệu LLD 10 / UC-ORD-08 + COMMIT `docs(order): specify expired checkout sweeper`
- [x] **O08.03** `ExpiredSessionSweeper` + `CheckoutCompensation` + audit
- [x] **O08.04** `ExpiredSessionSweeperIT` (MutableClock, đua IPN, chạy song song)
- [x] **O08.05** COMMIT `feat(order): cancel expired checkout sessions and release stock`
- [ ] **O08.06** CHECKLIST UC-ORD-08, push + PR

### O09 — Chạy thật trên giao diện (người dùng tự bấm)
- [x] **O09.01** Viết `docs/kiem-thu/kich-ban-<ngày>-c2.md`
- [ ] **O09.02** TC-C2-01 … 06 (giỏ)
- [ ] **O09.03** TC-C2-07 … 09 (phí ship, xem trước)
- [ ] **O09.04** TC-C2-10 … 12 (đặt hàng)
- [ ] **O09.05** TC-C2-13 (cron hủy)
- [ ] **O09.06** Ghi kết quả mục 5; lỗi → task `O11.xx`

### O11 — Sửa sau spike / chạy thật
- [x] **O11.01** `HttpGhnFeeClient` đổi sang `POST /v2/shipping-order/preview` (địa giới 2 cấp, `is_new_to_address`), `ShippingFeeQuoter` chỉ cần `ghnShopId` + mã phường; thử lại 1 lần khi GHN trả "To address conflict" ngẫu nhiên; tự parse JSON khi GHN trả `Content-Type: application/octet-stream` — nhánh `feature/c2-ghn-preview-fee`
- [ ] **O11.02** Hai hệ mã địa giới cùng tồn tại: thiếu `GHN_TOKEN` / `GHN_SHOP_ID` → sổ địa chỉ dùng `regions/regions.json` (**mã nhà nước**, vd TP.HCM `79`, Phường Sài Gòn `26740`); có đủ → GHN master-data v3 (**mã GHN**, vd `1000001`, `1003506`). GHN preview từ chối mã nhà nước ("phường/xã người nhận mới không tồn tại") → địa chỉ seed / tạo khi chưa có token chỉ được phí tạm tính. Cần chốt một hệ mã (hoặc bảng đối chiếu mã nhà nước → mã GHN) — liên quan plan AUTH D25, báo người giữ UC-AUTH-09. Phát hiện 2026-11-26 khi gán `ghn_shop_id = 241931` cho shop seed *Boutique Sài Gòn* (CSDL máy dev, không commit)

### O10 — Hồi quy & đóng plan
- [ ] **O10.01** `./mvnw clean verify` + `npm run verify` + `npm run test:e2e`, ghi mục 7
- [ ] **O10.02** CHECKLIST: 5 UC → ✅, tổng quan, nhật ký
- [ ] **O10.03** Usecase.md / Excel (sau `git merge develop`) / BAO_CAO / CLAUDE.md mục 2; nếu C1 đã merge → kịch bản nối chuỗi (plan mục 8.9)
- [ ] **O10.04** COMMIT `docs: close c2 cart and checkout plan`

## 5. Kết quả chạy thật

| Mã | Kịch bản | Ngày | Người chạy | Kết quả | Ghi chú |
|---|---|---|---|:---:|---|
| TC-C2-01 | Thêm 1 SKU 2 lần → cộng dồn | | | | |
| TC-C2-02 | Thêm vượt tồn | | | | |
| TC-C2-03 | Chủ shop mua hàng shop mình | | | | |
| TC-C2-04 | Tài khoản Admin vào giỏ | | | | |
| TC-C2-05 | Vãng lai → đăng nhập gộp giỏ | | | | |
| TC-C2-06 | Đổi giá khi món đang trong giỏ | | | | |
| TC-C2-07 | Giỏ 2 shop khác tỉnh → checkout | | | | |
| TC-C2-08 | Freeship của shop | | | | |
| TC-C2-09 | GHN lỗi → phí tạm tính | | | | |
| TC-C2-10 | Đặt COD 2 shop | | | | |
| TC-C2-11 | Bấm đặt hàng 2 lần | | | | |
| TC-C2-12 | Hai người tranh SKU còn 1 | | | | |
| TC-C2-13 | Đơn online bỏ dở → tự hủy, trả tồn | | | | |

## 6. Nhật ký

> Mỗi dòng = một việc đã xong. Mẫu: `| 2026-11-26 | O02.02 | CartService thêm / xem / đổi / xóa | feature/c2-cart | a1b2c3d |`

| Ngày | Task | Việc đã làm | Nhánh | Commit / PR |
|---|---|---|---|---|
| 2026-11-25 | — | Lập plan + progress chuỗi C2 (5 UC, 64 task, 15 quyết định chờ chốt) | `develop` @ `0e34dce` | (chưa commit) |
| 2026-11-26 | — | Rà chéo với plan C1: cập nhật hiện trạng (UC-VND-05 ✅, mã lỗi `GNE-ORD-4093/4094` chưa có trong code, seed đã có kho lấy hàng), migration `V25` → `V26`, thêm D16 (ghi `product_variants`, luật "mua được"), D14 cài `ProductOrderGuard`, D5 / D7 theo ranh giới với C1 | `feature/c1` @ `609a0c4` | (chưa commit) |
| 2026-11-26 | O01–O04 | Giỏ hàng UC-ORD-01: tài liệu (API 6.3 giới hạn, 6.7 `skipped` / `adjusted`, handoff), mã lỗi `GNE-INV-4093/4094`, `GNE-ORD-4093/4094`; `VariantFacade` + `ShopFacade.buyInfo`; `CartService` / `CartMergeService` / `CartController` / `PublicVariantController`; `CartIT`, `CartMergeIT`; web nối `cart` + toast gộp giỏ; `order-cart.http` | `feature/c2-cart` | `07f49f5`, `845b880`, `0c108ff` |
| 2026-11-26 | O05.01–O05.07 | Phí vận chuyển UC-ORD-06: tài liệu (6.8 `is_estimated_fee`, `SHIPPING_UNAVAILABLE`, `GHN_MODE`), `shipping/fee` (`Parcel`, `ShippingQuote`, `FlatFeeTable`, `ShippingFeeQuoter`, `GhnFeeClient`, `HttpGhnFeeClient`), seed shop demo đã đủ `pickup_*` + `ghn_shop_id` (không sửa); test 11 ca; spike D4 chưa chạy (thiếu token) | `feature/c2-shipping-fee` | `06a24fc`, `0a72e65` |
| 2026-11-26 | O06.01–O06.06 | Xem trước UC-ORD-04: tài liệu 6.8 (giảm giá = 0 / `null` trước C6, `base_shipping_fee`, `free_ship_threshold`; gạch `shipping-policies` ở handoff); `CheckoutDiscountPolicy` + `CheckoutPorts` + `CheckoutPricer`; `POST /me/checkout/preview`; `CheckoutPreviewIT` (5), `CheckoutPricerTest` (2); web phí ship từ 6.8, ẩn Voucher / Điểm, nhãn "phí tạm tính" | `feature/c2-checkout-preview` | `e656097`, `8f6c0e4`, `de696ca` |
| 2026-11-26 | O07.01–O07.09 | Đặt hàng UC-ORD-07: tài liệu 6.10 / LLD 6.1 (D3, D10, D11); `IdempotencyStore` (Redis `SET NX` — plan ghi `IdempotencyFilter` sẵn có nhưng thực tế chỉ là khung rỗng, đã thay), `OrderCodeGenerator` (ngày giờ VN; Redis mất khóa đếm → nối tiếp số lớn nhất trong CSDL), `ReservationService`, `CheckoutService.place` / `get`, `PaymentInitiator` giả lập, `CheckoutCreatedEvent`, `CheckoutSessionController`; `ProductOrderGuard` (nguyên văn mục 8.4) + `OrderProductGuard`, `OrderAccountDeletionBlocker`; test `CheckoutSessionIT` (6), `CheckoutConcurrencyIT` (2 — 8 người tranh tồn 3), `OrderGuardsIT`, `CheckoutServiceTest` (3); web đặt hàng thật, cổng giả lập → sang trang kết quả | `feature/c2-checkout-session` | `c32ab06`, `c4467f8`, `df1fb03`, `d81c428` |
| 2026-11-26 | O08.01–O08.05 | Cron UC-ORD-08: tài liệu LLD 10 (D13); `CheckoutCompensation` (mặc định không làm gì), `ExpiredSessionSweeper`, `CheckoutSessionRepository.findExpiredIds / lockIfExpired`; `ExpiredSessionSweeperIT` (5: quá hạn → EXPIRED / AUTO_CANCELLED / nhả tồn + audit, chưa tới hạn không đụng, IPN đã chốt → bỏ qua, COD không quét, 2 cron song song không nhả hai lần, phiên lỗi không chặn phiên khác) | `feature/c2-order-sweeper` | `d703a78`, `d9c6d19` |
| 2026-11-26 | O09.01, O10.01–O10.03 | Kịch bản `docs/kiem-thu/kich-ban-2026-11-26-c2.md` (TC-C2-01…13); hồi quy backend + web xanh; cập nhật CHECKLIST / Usecase / Excel (5 UC → Đang triển khai) / BAO_CAO / CLAUDE.md | `feature/c2-order-sweeper` | (commit đóng plan) |
| 2026-11-26 | O05.01 (spike D4) | Tạo cửa hàng mẫu GHN thử `241931` (kho Quận 1), điền `GHN_SHOP_ID` vào `.env`; spike: `/v2/shipping-order/fee` không nhận phường mới, `/v2/shipping-order/preview` + `is_new_to_address` + mã phường v3 tính đúng phí — kết quả ở mục 7 | `feature/c2-order-sweeper` | (commit này) |
| 2026-11-26 | O11.01 | Phí GHN theo địa giới 2 cấp: `Parcel` bỏ mã quận, thêm tên / SĐT / địa chỉ người nhận; `HttpGhnFeeClient` gọi `/v2/shipping-order/preview` (đọc `total_fee`), thử lại 1 lần khi "To address conflict", tự parse JSON (GHN có lúc trả `application/octet-stream`); `ShippingFeeQuoter` chỉ cần `ghnShopId` + mã phường; test `HttpGhnFeeClientTest` (9), `ShippingFeeQuoterTest` (8). Gọi thật lớp đã biên dịch trên GHN thử: 12 / 12 lần đúng (Q1 → Đống Đa 53.900đ, → Sài Gòn 20.900đ) | `feature/c2-ghn-preview-fee` | (commit này) |

## 7. Số liệu kiểm tra & ghi chú kỹ thuật

| Ngày | Nhánh | `./mvnw verify` | `npm run verify` | E2E | Ghi chú |
|---|---|---|---|---|---|
| 2026-11-26 | `feature/c2-order-sweeper` | 209 unit + 358 IT xanh (1 skip) | xanh — 76 file / 359 test | chưa chạy | mốc cuối (O10.01) |
| 2026-11-26 | `feature/c2-checkout-session` | 209 unit + 353 IT xanh (1 skip) | xanh — 76 file | — | mốc sau O07 |
| 2026-11-26 | `feature/c2-cart` | 339 IT xanh (1 skip) | xanh — 76 file / 359 test | — | mốc sau O02 (thêm `CartIT` 10, `CartMergeIT` 3) |

**Kết quả spike GHN 2 cấp (O05.01 — D4):** chạy 2026-11-26 trên môi trường thử GHN (`dev-online-gateway`), kho gửi = cửa hàng mẫu `241931` "Genie Demo - Kho Quan 1" (tạo bằng `POST /v2/shop/register`, mã cũ quận 1442 / phường 20108), điểm nhận dùng **đúng định danh app đang lưu** (master-data v3: tỉnh `_id` vd Hà Nội `1000000`, phường `_id` dạng chuỗi vd Phường Đống Đa `"1003476"`).

| Ca | Endpoint | Tham số điểm nhận | Kết quả |
|---|---|---|---|
| A0 đối chứng | `POST /v2/shipping-order/fee` | `to_district_id` + `to_ward_code` cũ | `200`, 53.900đ |
| B, C | `/v2/shipping-order/fee` | chỉ `to_ward_code` mới (± `to_province_id`) | `400` "to ward not found" |
| D, E, G1–G5 | `/v2/shipping-order/fee` | `is_new_to_address: true` + mã / tên phường mới (thử `to_ward_id`, `to_new_ward_id`, `to_new_ward_code`…) | `400` "mã phường mới không tồn tại: 0" — API phí v2 có nhánh địa chỉ mới nhưng không nhận được mã qua các tên trường đã thử |
| F | `POST /v3/shipping-order/fee` | — | `404` (không có API phí v3) |
| **P2 ✅** | **`POST /v2/shipping-order/preview`** (xem trước, **không tạo đơn** — `order_code` rỗng) | **`is_new_to_address: true` + `to_ward_code` = mã phường mới** + `service_type_id: 2`, `payment_type_id`, `required_note`, `to_name`, `to_phone`, `to_address`, `weight`, `items` | **`200`, `data.fee.main_service`**: Q1 → Đống Đa 500 g **53.900đ** (= phí tính theo mã cũ ở A0), 3 kg 119.900đ; Q1 → Phường Sài Gòn 500 g / 3 kg 20.900đ, 10 kg 67.100đ; mã phường sai → `400` "phường/xã người nhận mới không tồn tại" |
| P1 | `/v2/shipping-order/preview` | `is_new_to_address` + chỉ **tên** tỉnh / phường | `400` "Địa chỉ nhận không hợp lệ" — phải gửi mã |

**Kết luận:** tính phí theo địa chỉ 2 cấp **được**, nhưng qua `POST /v2/shipping-order/preview` thay cho `/v2/shipping-order/fee`. Kho gửi lấy theo header `ShopId` (không cần `from_district_id`), nên kho của shop cũng không cần mã quận — chỉ cần shop có `ghn_shop_id` thật. Môi trường thử chập chờn (vài lần trả "To address conflict" / "Chưa truyền serviceID" rồi gọi lại thì được) → giữ bảng phí dự phòng + `is_estimated_fee` khi lỗi. **Việc cần làm (task mới O11.01):** đổi `HttpGhnFeeClient` sang `preview` (gửi tên / SĐT / địa chỉ người nhận từ `AddressSnapshot`, `to_ward_code` mới, `is_new_to_address: true`), bỏ điều kiện "hai đầu phải có mã quận" trong `ShippingFeeQuoter` (chỉ cần `ghnShopId`), sửa `HttpGhnFeeClientTest`, API 6.8 / `Usecase.md` UC-ORD-06.
| 2026-11-26 | — | Gộp migration với C1: `V25__listing_and_checkout_schema.sql` (có `uk_cart_items_cart_variant`, `idx_checkout_sessions_open_expires`) → D15 ☑; sửa tên cột `reservation_expires_at` → `expires_at` trong luồng 3.4, 3.5, D13; phần phối hợp chuyển vào plan **mục 8** (bỏ file ranh giới riêng); O00.03 = chờ PR schema | `feature/c1` | (chưa commit) |
