# PROGRESS — Chuỗi C2 Giỏ → Checkout

> Plan: [plan_gio_hang_checkout_c2.md](plan_gio_hang_checkout_c2.md) · Ngày lập: 2026-11-25 · Phụ trách: _(chưa phân công)_
> Cách dùng: xong task nào thì đổi `- [ ]` → `- [x]`, điền cột **Commit / PR** ở mục 2 và ghi **một dòng Nhật ký** ở mục 6 (ngày, mã task, việc, mã commit / nhánh). Kịch bản chạy thật ghi **Đạt / Không đạt** vào mục 5. Task không cần làm ghi `- [x] ~~…~~ (không cần: lý do)`. Đồng thời cập nhật `docs/Document/CHECKLIST_TIEN_DO_UC.md` khi xong một lớp của UC (CLAUDE.md mục 4).

## 1. Tổng quan

| UC | GD | BE | Test | Nối | Chạy | Trạng thái hiện tại | Mục tiêu |
|---|:---:|:---:|:---:|:---:|:---:|---|---|
| UC-ORD-01 Giỏ hàng | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai | ✅ |
| UC-ORD-06 Phí vận chuyển GHN | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai | ✅ |
| UC-ORD-04 Xem trước & tách giỏ | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai | ✅ |
| UC-ORD-07 Phiên checkout & đơn con | ☑ | ☐ | ☐ | ☐ | ☐ | ⬜ Chưa triển khai | ✅ |
| UC-ORD-08 Cron hủy đơn quá hạn | — | ☐ | ☐ | — | ☐ | ⬜ Chưa triển khai | ✅ |

**Tổng quan dự án (CHECKLIST 2026-11-24):** Đã triển khai 17 / 87 → mục tiêu **22** / 87 khi đóng plan.

## 2. Tiến độ theo giai đoạn

| Giai đoạn | Nhánh | Việc | Số task | Trạng thái | Commit / PR |
|---|---|---|---:|:---:|---|
| O00 | `develop` | Chuẩn bị, chốt quyết định | 5 | ☐ | |
| O01 | `feature/c2-cart` | ORD-01: tài liệu trước | 5 | ☐ | |
| O02 | `feature/c2-cart` | ORD-01: backend + test | 6 | ☐ | |
| O03 | `feature/c2-cart` | ORD-01: nối web | 4 | ☐ | |
| O04 | `feature/c2-cart` | ORD-01: kiểm tra & PR | 3 | ☐ | |
| O05 | `feature/c2-shipping-fee` | ORD-06: phí GHN + dự phòng | 8 | ☐ | |
| O06 | `feature/c2-checkout-preview` | ORD-04: xem trước & tách giỏ | 7 | ☐ | |
| O07 | `feature/c2-checkout-session` | ORD-07: đặt hàng | 10 | ☐ | |
| O08 | `feature/c2-order-sweeper` | ORD-08: cron hủy quá hạn | 6 | ☐ | |
| O09 | nhánh cuối | Chạy thật trên giao diện | 6 | ☐ | |
| O10 | nhánh cuối | Hồi quy & đóng plan | 4 | ☐ | |

**Tổng:** 64 task · 13 kịch bản chạy thật.

**Sơ đồ nhánh (D1 — xếp chồng, PR vào `develop` đúng thứ tự):**
```
develop ─┬─ feature/c2-cart ─── feature/c2-shipping-fee ─── feature/c2-checkout-preview ─── feature/c2-checkout-session ─── feature/c2-order-sweeper
         │   (PR #1)              (PR #2)                      (PR #3)                          (PR #4)                          (PR #5)
         └─ gộp PR #n xong → git switch <nhánh n+1> && git rebase develop → mở PR #n+1
```

| Nhánh | Tạo từ | Ngày tạo | PR | Ngày gộp |
|---|---|---|---|---|
| `feature/c2-cart` | `develop` @ | | | |
| `feature/c2-shipping-fee` | `feature/c2-cart` | | | |
| `feature/c2-checkout-preview` | `feature/c2-shipping-fee` | | | |
| `feature/c2-checkout-session` | `feature/c2-checkout-preview` | | | |
| `feature/c2-order-sweeper` | `feature/c2-checkout-session` | | | |

## 3. Quyết định (chép kết quả từ plan mục 2)

| Mã | Chủ đề | Chốt | Ngày | Ai chốt |
|---|---|:---:|---|---|
| D1 | Nhánh & thứ tự PR | ☐ | | |
| D2 | Voucher & Điểm trong C2 (cổng mặc định 0) | ☐ | | |
| D3 | Thanh toán trong C2 (COD trọn, online stub) | ☐ | | |
| D4 | GHN với địa giới 2 cấp (spike) | ☐ | | |
| D5 | Shop chưa cấu hình kho lấy hàng | ☐ | | |
| D6 | Chế độ GHN `stub\|http`, `SHIPPING_UNAVAILABLE` | ☐ | | |
| D7 | Khối lượng mặc định / freeship shop | ☐ | | |
| D8 | Giỏ vãng lai & kết quả gộp giỏ | ☐ | | |
| D9 | Giới hạn giỏ (100 dòng, 999 / dòng) | ☐ | | |
| D10 | Mã đơn `TR-yyyyMMdd-######` | ☐ | | |
| D11 | Idempotency đặt hàng | ☐ | | |
| D12 | Hoa hồng chụp vào `order_items` | ☐ | | |
| D13 | Cron hủy phiên quá hạn | ☐ | | |
| D14 | Chặn xóa tài khoản khi còn đơn | ☐ | | |
| D15 | Migration (`V25` nếu cần) | ☐ | | |

## 4. Task

### O00 — Chuẩn bị (`develop`)
- [ ] **O00.01** Chốt D1–D15, ghi mục 3
- [ ] **O00.02** `git switch develop && git pull`, ghi mã commit gốc vào mục 2
- [ ] **O00.03** Giữ số migration `V25` với nhóm (nếu D15 cần)
- [ ] **O00.04** Dựng môi trường + `./mvnw verify` + `npm run verify` mốc xanh, ghi mục 7
- [ ] **O00.05** Ghi người phụ trách vào BAO_CAO / CHECKLIST / Excel

### O01 — `feature/c2-cart`: tài liệu trước
- [ ] **O01.01** Tạo nhánh `feature/c2-cart` từ `develop`
- [ ] **O01.02** `API_SPEC.md` 6.3 (D9), 6.7 (D8)
- [ ] **O01.03** `web-backend-handoff.md` mục `cart`
- [ ] **O01.04** Kiểm / thêm `GNE-INV-4093`, `GNE-INV-4094` (ErrorCode + web + i18n)
- [ ] **O01.05** COMMIT `docs(order): specify cart limits and merge result`

### O02 — `feature/c2-cart`: backend + test
- [ ] **O02.01** Cổng `VariantFacade` (catalog) + `ShopFacade` (vendor) cho dòng giỏ
- [ ] **O02.02** Repository + `CartService`
- [ ] **O02.03** `CartMergeService` + `GET /public/variants`
- [ ] **O02.04** `CartController` 6.1–6.7
- [ ] **O02.05** `CartIT`, `CartMergeIT`, `PublicVariantsIT`, `OrderSecurityIT`
- [ ] **O02.06** COMMIT `feat(order): implement shopping cart api`

### O03 — `feature/c2-cart`: nối web
- [ ] **O03.01** `cart.mapper.ts` / `cart.api.ts` nhánh thật, cảnh báo đổi giá
- [ ] **O03.02** Toast gộp giỏ theo `skipped` / `adjusted`
- [ ] **O03.03** Cờ `cart` trong `PUBLIC_REAL_API` + docs cấu hình
- [ ] **O03.04** COMMIT `feat(web): connect cart to real api`

### O04 — `feature/c2-cart`: kiểm tra & PR
- [ ] **O04.01** `docs/api-tests/order-cart.http` — API đúng
- [ ] **O04.02** CHECKLIST / Usecase / Excel UC-ORD-01 + COMMIT `docs: record c2 cart progress`
- [ ] **O04.03** Push + PR vào `develop` (chờ người dùng)

### O05 — `feature/c2-shipping-fee` (UC-ORD-06)
- [ ] **O05.01** Tạo nhánh từ `feature/c2-cart` + spike GHN 2 cấp (D4), ghi mục 7
- [ ] **O05.02** Tài liệu 6.8 / UC-ORD-06 / `.env.example` + COMMIT `docs(shipping): specify shipping fee quote and fallback`
- [ ] **O05.03** `FlatFeeTable`, `Parcel`, `ShippingFeeQuoter`
- [ ] **O05.04** `GhnFeeClient` + cache 10 phút
- [ ] **O05.05** Seed kho lấy hàng shop demo (báo Thiện)
- [ ] **O05.06** `FlatFeeTableTest`, `ShippingFeeQuoterTest`, `GhnFeeClientTest`
- [ ] **O05.07** COMMIT `feat(shipping): quote ghn shipping fee with flat fallback` + `chore(db): seed pickup addresses for demo shops`
- [ ] **O05.08** CHECKLIST UC-ORD-06, push + PR

### O06 — `feature/c2-checkout-preview` (UC-ORD-04)
- [ ] **O06.01** Tạo nhánh từ `feature/c2-shipping-fee`
- [ ] **O06.02** Tài liệu 6.8 (D2) + handoff dòng 44 + COMMIT `docs(order): clarify checkout preview before vouchers`
- [ ] **O06.03** `CheckoutDiscountPolicy` + `CheckoutPricer`
- [ ] **O06.04** `POST /me/checkout/preview`
- [ ] **O06.05** `CheckoutPricerTest`, `CheckoutPreviewIT`
- [ ] **O06.06** Web preview thật + COMMIT `feat(order): add checkout preview with per-shop split` + `feat(web): use real checkout preview`
- [ ] **O06.07** CHECKLIST UC-ORD-04 (+ Nối ORD-06), push + PR

### O07 — `feature/c2-checkout-session` (UC-ORD-07)
- [ ] **O07.01** Tạo nhánh từ `feature/c2-checkout-preview`
- [ ] **O07.02** Tài liệu 6.10 / LLD 6.1 (D3), migration nếu D15 + COMMIT `docs(order): specify checkout session before payment gateway`
- [ ] **O07.03** `OrderCodeGenerator`, `ReservationService`
- [ ] **O07.04** `CheckoutService.place`, `PaymentInitiator` stub, `CheckoutCreatedEvent`
- [ ] **O07.05** `POST /me/checkout-sessions` (Idempotency), `GET /me/checkout-sessions/{id}`
- [ ] **O07.06** `OrderAccountDeletionBlocker`
- [ ] **O07.07** `CheckoutSessionIT`, `CheckoutConcurrencyIT`, `OrderCodeGeneratorTest`, `AccountDeletionGuardIT`
- [ ] **O07.08** Web đặt hàng thật + trang kết quả + xử lý `4090`, cờ `checkout`
- [ ] **O07.09** COMMIT `feat(order): persist checkout session and per-shop orders` + `feat(order): block account deletion with open orders` + `feat(web): place orders through real checkout api`
- [ ] **O07.10** CHECKLIST UC-ORD-07, push + PR

### O08 — `feature/c2-order-sweeper` (UC-ORD-08)
- [ ] **O08.01** Tạo nhánh từ `feature/c2-checkout-session`
- [ ] **O08.02** Tài liệu LLD 10 / UC-ORD-08 + COMMIT `docs(order): specify expired checkout sweeper`
- [ ] **O08.03** `ExpiredSessionSweeper` + `CheckoutCompensation` + audit
- [ ] **O08.04** `ExpiredSessionSweeperIT` (MutableClock, đua IPN, chạy song song)
- [ ] **O08.05** COMMIT `feat(order): cancel expired checkout sessions and release stock`
- [ ] **O08.06** CHECKLIST UC-ORD-08, push + PR

### O09 — Chạy thật trên giao diện (người dùng tự bấm)
- [ ] **O09.01** Viết `docs/kiem-thu/kich-ban-<ngày>-c2.md`
- [ ] **O09.02** TC-C2-01 … 06 (giỏ)
- [ ] **O09.03** TC-C2-07 … 09 (phí ship, xem trước)
- [ ] **O09.04** TC-C2-10 … 12 (đặt hàng)
- [ ] **O09.05** TC-C2-13 (cron hủy)
- [ ] **O09.06** Ghi kết quả mục 5; lỗi → task `O11.xx`

### O10 — Hồi quy & đóng plan
- [ ] **O10.01** `./mvnw clean verify` + `npm run verify` + `npm run test:e2e`, ghi mục 7
- [ ] **O10.02** CHECKLIST: 5 UC → ✅, tổng quan, nhật ký
- [ ] **O10.03** Usecase.md / Excel / BAO_CAO / CLAUDE.md mục 2
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

## 7. Số liệu kiểm tra & ghi chú kỹ thuật

| Ngày | Nhánh | `./mvnw verify` | `npm run verify` | E2E | Ghi chú |
|---|---|---|---|---|---|
| | `develop` (mốc O00.04) | | | | |

**Kết quả spike GHN 2 cấp (O05.01 — D4):** _(điền: endpoint đã thử, tham số, phản hồi, kết luận dùng `to_ward_code` hay dự phòng)_
