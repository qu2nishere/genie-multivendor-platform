# PLAN — Sửa đồng hồ "Tồn kho được giữ trong 15:00" ở trang Thanh toán

| Mục | Nội dung |
|---|---|
| **Ngày lập** | 2026-11-26 |
| **Liên quan** | Chuỗi C2 — UC-ORD-07 (Tạo phiên checkout), UC-ORD-08 (Cron hủy phiên quá hạn); task tiếp nối `O11.03` của [progress C2](../../2026-11-25/server-spring/progress_gio_hang_checkout_c2.md) |
| **Phạm vi** | Chỉ `web/` (feature `checkout`) — **không** đổi backend, API, CSDL |
| **Nhánh** | `bugfix/c2-checkout-hold-timer` tạo từ `develop` mới nhất (đã có PR #110) |
| **Người commit** | danh tính git đã cấu hình — không `--author`, không `--no-verify`, không dấu vết AI (CLAUDE.md mục 4) |

---

## 1. Hiện trạng & lỗi

**Lỗi người dùng thấy:** vào `/checkout` → khung tổng tiền hiện "Tồn kho được giữ trong **14:55**" và đếm ngược; bấm Back rồi vào lại → đồng hồ **về lại 15:00**. Hết giờ còn có nút "Giữ lại" đặt thêm 15 phút.

**Nguyên nhân** (`web/src/features/checkout/ui/`):

| Chỗ | Đang làm |
|---|---|
| `CheckoutPage.tsx:48` | `useState(() => holdDeadline(Date.now()))` — hạn giữ = **lúc mở trang** + 15 phút, mỗi lần vào trang tính lại |
| `CheckoutPage.tsx:253` | `onRenew={() => setDeadline(holdDeadline(Date.now()))}` — nút "Giữ lại" gia hạn ở trình duyệt |
| `CheckoutSections.tsx:230` `HoldCountdown`, `:294` trong `CheckoutSummary` | Hiện đồng hồ + nút "Giữ lại" |

Đây là phần giao diện từ thời chạy mock (plan G10.19), **không hỏi server**. Trên hệ thống thật:

- **Trước khi bấm "Đặt hàng" server CHƯA giữ hàng** → câu "Tồn kho được giữ trong …" là sai sự thật, và nút "Giữ lại" không giữ gì cả.
- Server chỉ giữ hàng khi `POST /me/checkout-sessions` thành công: `checkout_sessions.expires_at` = lúc đặt + 15 phút, **cố định** (không gia hạn — giữ hàng gắn với đơn, chống giữ hàng vô thời hạn); hết hạn → cron UC-ORD-08 hủy đơn, trả tồn.
- Sau khi đặt, trang kết quả (`CheckoutResult.tsx:127`) **đã** đếm ngược đúng theo `reservation_expires_at` của server — không phải sửa.

## 2. Quyết định

| Mã | Câu hỏi | Đề xuất | Chốt |
|---|---|---|---|
| H1 | Bỏ đồng hồ ở chế độ nào? | **Chỉ khi nối API thật** (`PUBLIC_REAL_API` có `checkout`). Chế độ mock giữ nguyên đồng hồ cũ (mô phỏng giao diện, E2E chạy mock). Phương án khác: bỏ ở cả hai chế độ cho gọn | ☑ theo đề xuất |
| H2 | Thay bằng gì ở trang Thanh toán? | Dòng thông tin tĩnh (không đếm): *"Sau khi đặt hàng, sản phẩm được giữ 15 phút để bạn thanh toán."* — dùng `HOLD_MINUTES` | ☑ theo đề xuất |
| H3 | Nút "Giữ lại"? | Bỏ ở chế độ thật (không có gì để gia hạn). Gia hạn / thanh toán lại đơn đang chờ thuộc API 6.12 — chuỗi C3 | ☑ theo đề xuất |
| H4 | Bấm Back về `/checkout` sau khi đã đặt | **Ngoài plan này** — làm cùng C3 (màn "đơn đang chờ thanh toán" + nút tới đơn). Hiện tại món đã rời giỏ nên trang không đặt lại được | ☑ theo đề xuất |

## 3. Việc cần làm

> Mã task `H0x`. Sau mỗi task: tick mục 5, cuối plan một dòng Nhật ký. Chỉ commit khi người dùng yêu cầu (CLAUDE.md mục 4).

### H01 — Chuẩn bị
- **H01.01** `git switch develop && git pull`, tạo nhánh `bugfix/c2-checkout-hold-timer`.

### H02 — Cờ chế độ (tầng application, theo luật phụ thuộc `ui → application`)
- **H02.01** Thêm hằng `HOLD_TIMER_BEFORE_ORDER` (= đang chạy mock) — đặt cạnh `DISCOUNTS_ENABLED` ở `checkout/infrastructure/shipping.api.ts` hoặc file cờ riêng `checkout/infrastructure/checkout.flags.ts` (`isMocked('checkout')`), xuất qua `checkout/index.ts` / dùng trong `ui` qua application như `useCheckout`.

### H03 — Giao diện
- **H03.01** `CheckoutSections.tsx`: thêm component `HoldNotice` (dòng tĩnh, icon `lock`, cùng kiểu khung vàng nhạt) dùng `t('checkout.hold.notice', { minutes: HOLD_MINUTES })`.
- **H03.02** `CheckoutSummary`: `deadline` / `onRenew` thành tùy chọn; có `deadline` → `HoldCountdown` (mock), không có → `HoldNotice` (thật).
- **H03.03** `CheckoutPage.tsx`: chế độ thật không tạo `deadline` / không truyền `onRenew`; chế độ mock giữ như cũ.
- **H03.04** i18n `vi.json` / `en.json`: thêm `checkout.hold.notice` (vi: "Sau khi đặt hàng, sản phẩm được giữ {minutes} phút để bạn thanh toán." · en: "After you place the order, items are held for {minutes} minutes for payment."). Giữ khóa cũ (mock vẫn dùng).
- **H03.05** Kiểm tương phản khung thông báo ở 9 theme (ADR 0001) — `/checkout?theme=…`.

### H04 — Kiểm tra
- **H04.01** Vitest: test nhỏ cho hàm chọn hiển thị (nếu tách thành hàm thuần ở `domain`), còn lại `npm run verify` xanh.
- **H04.02** E2E mock (`npm run test:e2e`) vẫn xanh — đồng hồ mock không đổi.
- **H04.03** Chạy thật (backend + `PUBLIC_REAL_API` có `checkout`):

| Mã | Thao tác | Kỳ vọng |
|---|---|---|
| TC-H-01 | Giỏ → chọn 1 món → "Mua hàng" | `/checkout` hiện dòng "Sau khi đặt hàng, sản phẩm được giữ 15 phút…", **không** có đồng hồ đếm, không có nút "Giữ lại" |
| TC-H-02 | Back về giỏ → "Mua hàng" lại | Vẫn là dòng thông tin tĩnh (không còn hiện tượng "reset 15 phút") |
| TC-H-03 | Đặt **VNPAY** (cổng giả lập) | Trang kết quả đếm ngược theo `reservation_expires_at` của server (~15:00 từ lúc đặt); tải lại trang → đồng hồ **tiếp tục**, không về 15:00 |
| TC-H-04 | Đặt **COD** | Trang thành công, không có đồng hồ |
| TC-H-05 | Đổi `web/.env` bỏ `checkout` khỏi `PUBLIC_REAL_API` (mock) | `/checkout` vẫn có đồng hồ + nút "Giữ lại" như trước |

### H06 — Lỗi phát sinh khi chạy thật: đặt lại món đã đặt (2026-11-26)
Người dùng gặp `422 GNE-VAL-4220` (`cart_item_ids` `NOT_FOUND`) khi bấm "Đặt hàng" lần hai: đơn đầu đã tạo (dòng giỏ đã rời giỏ) nhưng trang Thanh toán cũ vẫn còn món đó, giao diện chỉ báo "Dữ liệu yêu cầu không hợp lệ.". Trang cũ có thể hiện lại khi trình duyệt khôi phục từ bfcache (bấm Back), hoặc khi mở trang ở thẻ khác.
- **H06.01** `CheckoutPage`: `pageshow` với `persisted` → tải lại trang (lấy giỏ mới).
- **H06.02** Lỗi 422 `cart_item_ids` (`hasStaleCartItems` ở `checkout.rules.ts` + test) → tải lại giỏ, báo `checkout.place.stale_items`; dòng cũ tự rơi khỏi trang, hết món thì hiện màn "Chưa có sản phẩm để thanh toán".
- Không tải lại giỏ ngay khi đặt thành công: giỏ rỗng làm trang đổi sang màn trống và gỡ `PaymentFlow` (cổng VNPAY giả lập / mã QR).
- Màn "đơn đang chờ thanh toán" khi quay lại vẫn thuộc H4 (C3).

| Mã | Thao tác | Kỳ vọng |
|---|---|---|
| TC-H-06 | Mở `/checkout` có 1 món → ở thẻ khác đặt món đó (hoặc SQL xóa dòng giỏ) → quay lại thẻ đầu bấm "Đặt hàng" | Báo "Một số sản phẩm đã được đặt hoặc không còn trong giỏ hàng — giỏ hàng đã được tải lại."; trang hiện "Chưa có sản phẩm để thanh toán"; không tạo phiên mới |

### H07 — "Mua ngay" chỉ tick đúng món vừa bấm (2026-11-26)
Bấm "Mua ngay" → giỏ tick **mọi** món. Nguyên nhân: nút gửi `?buy=<khóa dòng>` = `<productId>|<nhãn phân loại ở trang sản phẩm>` (sản phẩm không phân loại → nhãn rỗng `26|`), còn dòng giỏ từ API dựng khóa bằng `variant_name` server lưu (`26|Mặc định`) → không khớp → `initialSelection` rơi về "chọn tất cả".
- **H07.01** `?buy=<variantId>` (`AddToCartButton`, `pages/cart.astro` chỉ nhận số nguyên dương), `initialSelection` khớp theo `variantId`; test `cart.rules.test.ts` sửa theo.

| Mã | Thao tác | Kỳ vọng |
|---|---|---|
| TC-H-07 | Giỏ đã có món khác → trang sản phẩm bất kỳ (có / không phân loại) → "Mua ngay" | `/cart` chỉ tick đúng món vừa bấm; các món cũ không tick |

### H05 — Tài liệu & commit
- **H05.01** Progress C2: thêm / tick `O11.03`, một dòng Nhật ký; `docs/kiem-thu/kich-ban-2026-11-26-c2.md` thêm TC-H-01…03 vào nhóm UC-ORD-07.
- **H05.02** COMMIT (khi người dùng yêu cầu): `fix(web): show hold notice instead of local countdown before placing an order`; push + PR vào `develop`.

## 4. Rủi ro

| Rủi ro | Xử lý |
|---|---|
| Người dùng hiểu nhầm "không giữ hàng" khi đang ở trang Thanh toán | Câu thông báo nói rõ giữ **sau khi đặt** |
| Trang kết quả online (phiên `PAYMENT_PENDING`) chưa hiện đồng hồ ở mọi nhánh giao diện | TC-H-03 kiểm; thiếu thì thêm task H03.06 dùng `reservationExpiresAt` |
| Đụng file chung `vi.json` / `en.json` với chuỗi C1 | Chỉ thêm 1 khóa mới trong `checkout.hold.*` |

## 5. Tiến độ

- [x] H01.01 · [x] H02.01 · [x] H03.01 · [x] H03.02 · [x] H03.03 · [x] H03.04 · [x] H03.05 · [x] H04.01 · [x] H04.02 · [ ] H04.03 (TC-H-01/02 đạt; TC-H-03/04/05 chờ chạy tay) · [x] H05.01 · [x] H06.01 · [x] H06.02 · [x] H07.01 · [ ] H05.02

**Nhật ký**

| Ngày | Task | Việc | Commit |
|---|---|---|---|
| 2026-11-26 | — | Lập plan | (chưa commit) |
| 2026-11-26 | H01–H05.01 | Nhánh `bugfix/c2-checkout-hold-timer` từ `develop` @ `76809ff`. Cờ `HOLD_TIMER_BEFORE_ORDER` ở `checkout.api.ts` (xuất qua `useCheckout`); `HoldNotice` + `deadline` / `onRenew` tùy chọn trong `CheckoutSummary`; `CheckoutPage` chỉ truyền đồng hồ khi mock; khóa `checkout.hold.notice` vi / en. H03.05: khung dùng lại đúng lớp màu của `HoldCountdown` cũ (`bg-amber-50 text-amber-600`) nên tương phản như cũ. H04.01: không tách hàm thuần (chỉ rẽ nhánh hiển thị) → `npm run verify` xanh. H04.02: E2E mock `checkout-vnpay` + `orders` 10 / 10. H04.03: chạy thật TC-H-01, TC-H-02 đạt (Playwright trên dev server, `PUBLIC_REAL_API` có `checkout`) | (chưa commit) |
| 2026-11-26 | H06 | Người dùng báo `422 cart_item_ids NOT_FOUND` khi đặt lại món vừa đặt. `CheckoutPage` tải lại trang khi khôi phục từ bfcache; lỗi 422 `cart_item_ids` → tải lại giỏ + báo `checkout.place.stale_items` (`hasStaleCartItems` + test). Kiểm: `npm run verify` xanh, E2E mock 10 / 10, chạy thật TC-H-06 đạt (Playwright, xóa dòng giỏ bằng SQL khi trang đang mở) | (chưa commit) |
| 2026-11-26 | H07 | "Mua ngay" tick mọi món: khóa `?buy=` ghép từ nhãn phân loại lệch `variant_name` server (`26|` ≠ `26|Mặc định`) → đổi sang `?buy=<variantId>`. `npm run verify` xanh (360 test); chạy thật TC-H-07 đạt (đèn ngủ #101, giỏ có 2 món Boutique → chỉ đèn ngủ được tick) | (chưa commit) |
