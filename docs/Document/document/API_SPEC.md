# GENIE — ĐẶC TẢ API REST (API Specification)

| Thuộc tính | Nội dung |
|---|---|
| Mã tài liệu | GENIE-TLCN-API |
| Phiên bản | **1.4 (2026-11-16)** — đồng bộ với mã nguồn `genie-multivendor-platform` (AUTH đã hiện thực, giao diện `web/` đã dựng trên mock, kho tệp Silo); bản trước: 1.3 — khớp hợp đồng OpenAPI `genie-api-spec` v1.1.0 (Swagger UI); khớp bộ tài liệu TLCN **v1.3** (ENTITY.md 65 bảng, LLD 8.5 & 12.1, Document_Vi 3.5 & 3.7.1 đã đồng bộ theo file này) và giao diện `genie/ui_kits/` |
| Ngày lập | 2026-11-08 |
| Phạm vi | Toàn bộ API mà các màn hình **Storefront, Auth, Seller Center, Admin Console** cần — không thêm trường/endpoint mà màn hình không dùng |
| Base URL | `https://api.genie.vn/api/v1` (staging: `https://api.staging.genie.vn/api/v1`) |
| Định dạng | JSON `snake_case`, UTF-8 |
| Tài liệu gốc | LLD.md mục 8 & 12, TECH_STACK_VA_QUY_UOC.md mục 8–9, ENTITY.md, Usecase.md, BAO_CAO_DOI_CHIEU_GIAO_DIEN.md |
| Lịch sử | 1.4 (2026-11-16): mục 2–3 chỉnh theo backend AUTH thật (đăng ký chưa tạo `users` tới khi OTP đúng, `GNE-AUTH-4012`, refresh trả kèm `user`, callback OAuth cố định + `next`, thêm `POST /me/identities/{provider}/authorize`, liên kết Google phải trùng email `GNE-AUTH-4098`, gỡ liên kết cần `current_password`, xóa tài khoản giữ phiên hiện tại); thêm mã `GNE-SYS-4040/4050/5000`; mục 5.18 bổ sung 6 endpoint công khai giao diện đang dùng; trường `product_slug` ở giỏ / `$OrderItem`; mục 17.5 đối chiếu 21 endpoint lệch giữa mã web và spec; kho tệp MinIO → Silo (ADR 0009 — mã `GNE-MINIO-5003` giữ nguyên) · 1.3 (2026-11-08): đồng bộ với hợp đồng Swagger `genie-api-spec` — đổi `GNE-WAL-4002/4033/4034` thành `GNE-WAL-4221/4222/4092` cho khớp định dạng mã, thêm 7 mã lỗi còn thiếu (`GNE-AUTH-4097`, `GNE-ORD-4041`, `GNE-CAT-4095`, `GNE-VND-4096`, `GNE-VND-4224`, `GNE-NOT-4221`, `GNE-ADM-4092`), chốt cấu trúc phản hồi 6.3 / 7.6 / 7.8, `client_message_id`, payload webhook SePay / hãng vận chuyển / KYC nội bộ (mục 17.4) · 1.2 (2026-11-08): rà phủ toàn bộ — thêm mở khóa tài khoản qua email, tìm nhanh ở Seller Center, mục 12.2 tác vụ nền & email tự động (UC-ORD-08, UC-ORD-12, UC-PAY-07, UC-NOT-03) · 1.1 (2026-11-08): bổ sung đổi email, từ khóa xu hướng, chi tiết đơn phía shop, cảm xúc đánh giá (PhoBERT), phong tỏa ký quỹ, xóa banner; mọi chênh lệch ở mục 17 đã xử lý · 1.0 (2026-11-08): bản đầu |

> **Cách đọc:** mỗi endpoint ghi *màn hình dùng nó*, *quyền*, *mã UC*, rồi tới request/response mẫu. Đối tượng dùng lại nhiều lần được định nghĩa một lần ở **mục 1 (Schema dùng chung)** và gọi bằng tên `$TênSchema` — khi chuyển sang OpenAPI, mỗi `$TênSchema` là một `components/schemas`.
>
> **Nguyên tắc thiết kế xuyên suốt:**
> 1. **Màn hình trả gì thì API trả nấy.** Danh sách trả DTO rút gọn, trang chi tiết trả DTO đầy đủ; không có trường "để dành".
> 2. **REST theo tài nguyên:** CRUD dùng đúng `GET / POST / PUT / PATCH / DELETE`; chuyển trạng thái có luật nghiệp vụ (hủy đơn, duyệt KYC…) dùng *custom method* `POST /{tài-nguyên}/{id}/{động-từ}` theo quy ước dự án (TECH_STACK mục 8).
> 3. **Ít round-trip:** một trang = 1–3 lời gọi; dữ liệu dùng chung nhiều nơi (tim yêu thích, shop đã theo dõi) lấy một lần bằng endpoint trả *danh sách ID*, để danh sách sản phẩm công khai cache được (không chứa dữ liệu riêng từng người).
> 4. **Server tính, client hiển thị:** giá giỏ hàng, voucher, Điểm, nút hành động của đơn (`allowed_actions`) do server tính — client không tự dựng lại máy trạng thái.

---

## MỤC LỤC

0. [Quy ước chung](#0-quy-ước-chung)
1. [Schema dùng chung](#1-schema-dùng-chung)
2. [Xác thực — Auth (khách vãng lai)](#2-xác-thực--auth)
3. [Tài khoản của tôi — `/me`](#3-tài-khoản-của-tôi--me)
4. [Tải tệp — Uploads](#4-tải-tệp--uploads)
5. [Danh mục, sản phẩm, shop, trang chủ — `/public`](#5-danh-mục-sản-phẩm-shop-trang-chủ--public)
6. [Giỏ hàng, thanh toán — Cart & Checkout](#6-giỏ-hàng-và-thanh-toán)
7. [Đơn mua, trả hàng, đánh giá (khách)](#7-đơn-mua-trả-hàng-đánh-giá-khách)
8. [Chat Khách ↔ Shop](#8-chat-khách--shop)
9. [Mở shop — Become a Seller](#9-mở-shop)
10. [Seller Center — `/seller`](#10-seller-center--seller)
11. [Admin Console — `/admin`](#11-admin-console--admin)
12. [Webhook & API nội bộ](#12-webhook--api-nội-bộ)
13. [Realtime — WebSocket (STOMP)](#13-realtime--websocket-stomp)
14. [Bản đồ Màn hình → API](#14-bản-đồ-màn-hình--api)
15. [Danh mục mã lỗi](#15-danh-mục-mã-lỗi)
16. [Enum & mã lý do](#16-enum--mã-lý-do)
17. [Chênh lệch với ENTITY/LLD & quyết định thiết kế](#17-chênh-lệch-với-entitylld--quyết-định-thiết-kế)
18. [Chức năng bổ sung & độ phủ](#18-chức-năng-bổ-sung--độ-phủ)

---

## 0. Quy ước chung

### 0.1 Nhóm đường dẫn theo đối tượng gọi

| Tiền tố | Ai gọi | Xác thực |
|---|---|---|
| `/auth/**` | Khách vãng lai (đăng ký, đăng nhập, quên mật khẩu, OAuth) | Không (trừ `refresh`, `logout`) |
| `/public/**` | Mọi người — dữ liệu công khai, **cache được** ở CDN/Nginx | Không bắt buộc; nếu gửi token thì một số trường cá nhân xuất hiện (ghi rõ ở từng endpoint) |
| `/me/**` | Mọi người dùng đã đăng nhập (khách, chủ shop, nhân viên) — dữ liệu *của chính mình* | `Bearer` |
| `/reviews/**`, `/uploads/**` | Người dùng đã đăng nhập, tài nguyên dùng chung giữa vai trò | `Bearer` |
| `/seller/**` | Chủ shop (`role = VENDOR`) | `Bearer` + **cổng KYC** (mục 0.9) |
| `/admin/**` | Nhân viên (`STAFF`) và `ADMIN` | `Bearer` + **mã quyền** (mục 0.10) |
| `/payments/**`, `/shipping/**` | VNPay, SePay, hãng vận chuyển (webhook) | Chữ ký / API key |
| `/internal/**` | AI Worker (KYC, PhoBERT) | Header `X-Internal-Token` |

Tên tài nguyên: danh từ số nhiều, `kebab-case` (`/category-proposals`). Hành động đặc biệt: động từ ở cuối (`/orders/{id}/cancel`). Tham số đường dẫn luôn là **`id` số (int64)**; mã hiển thị như `TR-20260608-000141`, `RFN-0231`, `PAYOUT-0012` trả trong trường `code`.

### 0.2 Header

| Header | Bắt buộc | Mô tả |
|---|---|---|
| `Authorization: Bearer <access_token>` | Với nhóm cần đăng nhập | JWT HS256, sống **15 phút** |
| `Accept-Language: vi \| en` | Không (mặc định `vi`) | Ngôn ngữ của `message`, nhãn trạng thái từ hãng vận chuyển, tên lý do |
| `Idempotency-Key: <UUIDv4>` | **Có** với API tiền / tồn kho (đánh dấu 🔁) | Redis `SET NX` 24 giờ; gửi lại cùng key → nhận lại đúng response cũ. Redis lỗi → `503 GNE-SYS-5030` |
| `X-OTP-Challenge-Id` + `X-OTP-Code` | **Có** với thao tác nhạy cảm (đánh dấu 🔐) | Xác thực bước 2 bằng OTP email — xem mục 0.7 |
| `X-Request-Id` | Không | Client tự sinh để dò log; server luôn trả lại trong `meta.request_id` |

Refresh token **không** đi trong body: server đặt cookie `genie_rt` (`HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`, 7 ngày, xoay vòng mỗi lần dùng).

### 0.3 Cấu trúc phản hồi

**Thành công — một đối tượng (`200`, `201`)**

```json
{
  "code": "SUCCESS",
  "status": 200,
  "message": "OK",
  "data": { "id": 141, "code": "TR-20260608-000141", "status": "PAID" },
  "meta": { "request_id": "req_01HP8S9W5Q8Y9K1M3B00000001", "timestamp": "2026-06-08T14:22:00.125+07:00", "server_version": "mvp-1.0" }
}
```

**Thành công — danh sách**: `data` là mảng, kèm `pagination` và (nếu màn hình cần đếm theo tab / tổng hợp) `summary`.

```json
{
  "code": "SUCCESS", "status": 200, "message": "OK",
  "data": [ { "...": "..." } ],
  "pagination": { "page": 1, "size": 20, "total_items": 240, "total_pages": 12 },
  "summary": { "counts": { "ALL": 48, "TO_PAY": 2 } },
  "meta": { "request_id": "...", "timestamp": "...", "server_version": "mvp-1.0" }
}
```

**`204 No Content`**: không có body (bật/tắt, xóa).

**Lỗi — RFC 7807 mở rộng (`4xx`, `5xx`)**

```json
{
  "code": "GNE-VAL-4220",
  "status": 422,
  "message": "Dữ liệu yêu cầu không hợp lệ.",
  "errors": [
    { "field": "phone_number", "code": "PATTERN_MISMATCH", "rejected_value": "012345", "message": "Số điện thoại phải gồm 10 chữ số." }
  ],
  "details": { "remaining_attempts": 3 },
  "meta": { "request_id": "...", "timestamp": "...", "path": "/api/v1/auth/registrations" }
}
```

`details` (tùy chọn) mang dữ liệu để màn hình hiển thị ngay mà không gọi thêm: số lần thử còn lại, thời điểm hết khóa, danh sách đơn đang chặn thao tác… Danh mục mã lỗi ở mục 15.

### 0.4 Phân trang

| Kiểu | Dùng khi | Tham số | Khối `pagination` |
|---|---|---|---|
| **Trang số** | Màn hình có nút số trang / "Trang 2/8" / tổng số kết quả (danh mục, tìm kiếm, Gợi ý hôm nay, bảng quản trị) | `page` (từ 1), `size` (mặc định 20, tối đa 60; tìm kiếm tối đa trang 100) | `{ page, size, total_items, total_pages }` |
| **Con trỏ (cursor)** | Danh sách dài nối tiếp "Xem thêm" / cuộn (thông báo, chat, lịch sử Điểm, sổ cái, nhật ký) | `limit` (mặc định 20, tối đa 100), `cursor` | `{ limit, has_more, next_cursor }` |

### 0.5 Kiểu dữ liệu

| Loại | Quy ước | Ví dụ |
|---|---|---|
| Tiền | Số nguyên VND, không phần lẻ | `389000` |
| Tỷ lệ % | Số thập phân 1 chữ số, đơn vị % | `5.0` (= 5%) |
| Thời điểm | ISO-8601 có múi giờ | `2026-06-08T14:22:00+07:00` |
| Ngày | `YYYY-MM-DD` | `1996-08-12` |
| Tháng báo cáo | `YYYY-MM` | `2026-06` |
| Khoảng thời gian | ISO-8601 duration | `P7D` |
| Enum | `UPPER_SNAKE_CASE`, đúng Phụ lục A ENTITY.md | `PENDING_PAYMENT` |
| URL ảnh | URL tuyệt đối CDN/Silo; ảnh riêng tư là Presigned GET 5 phút | `https://cdn.genie.vn/product-media/...webp` |
| Trường không có giá trị | Trả `null`, **không** bỏ khóa | `"vacation": null` |

### 0.6 Định danh giá trị hiển thị

Server **không** trả chuỗi đã định dạng kiểu `"-30k"`, `"Đơn từ 199.000₫"`, `"128k người theo dõi"`. Client tự định dạng từ số (`Intl.NumberFormat`) theo ngôn ngữ đang chọn. Ngoại lệ: nhãn trạng thái thô của hãng vận chuyển (`carrier_status_text`) và lý do do người dùng tự nhập.

### 0.7 OTP bước 2 (step-up) cho thao tác nhạy cảm 🔐

Các thao tác: đổi/thiết lập mật khẩu, xóa tài khoản, rút tiền, thêm/đổi/xóa tài khoản nhận tiền. (Đăng ký mở shop cũng xác minh email bằng OTP nhưng theo luồng riêng ở mục 9 — không dùng header `X-OTP-*`.)

1. Gửi mã: `POST /me/otp-challenges` `{ "purpose": "WITHDRAWAL" }` → `202` `$OtpChallenge`.
2. Gọi thao tác chính kèm header `X-OTP-Challenge-Id` và `X-OTP-Code`.
3. Sai mã → `422 GNE-AUTH-4222` (`details.remaining_attempts`); sai 5 lần → `422 GNE-AUTH-4221` (challenge bị hủy); hết 5 phút → `410 GNE-AUTH-4101`. Gửi lại: `POST /me/otp-challenges/{id}/resend` (sau 60 giây, tối đa 5 lần/giờ).

Luồng khách vãng lai (đăng ký, đăng nhập OTP, quên mật khẩu) dùng `/auth/otp-challenges` ở mục 2 với cùng luật.

### 0.8 Tải tệp

Không upload qua Spring. Client xin Presigned PUT (`POST /uploads/presigned-urls`) → `PUT` thẳng lên Silo → gửi `object_key` trong chính request nghiệp vụ (tạo sản phẩm, gửi đánh giá…). Server kiểm tra MIME/kích thước bằng `statObject` và chuyển tệp từ `tmp/` sang khóa chính thức **trong cùng request đó** — không có bước `confirm` riêng (bớt 1 round-trip so với LLD 8.4).

### 0.9 Cổng KYC của Seller Center

Mọi endpoint `/seller/**` trả `403 GNE-VND-4031` khi `kyc_status ≠ VERIFIED`, **trừ**: `GET /seller/me`, nhóm `/seller/kyc/**`, `GET|PATCH /seller/settings`, `PUT /seller/pickup-address`, nhóm `/seller/payout-account/**`, nhóm vacation. Khớp luật nav: "chưa VERIFIED thì khóa mọi mục trừ KYC và Cài đặt".

### 0.10 Phân quyền Admin

Mỗi endpoint `/admin/**` ghi mã quyền cần có (13 mã của `StaffPermissionEnum`). `ADMIN` có toàn quyền. Thiếu quyền → `403 GNE-SEC-4030` và ghi `system_logs ACCESS_DENIED`. Trường tiền trên Tổng quan chỉ trả khi có `VIEW_FINANCE`.

### 0.11 Giới hạn tần suất

| Endpoint | Giới hạn |
|---|---|
| `POST /auth/sessions` | 10 / phút / IP |
| Gửi / gửi lại OTP | 1 / 60 giây / email, 5 / giờ / email |
| `GET /public/products`, `GET /public/search/suggestions` | 60 / phút / IP |
| `POST /me/password/verify` | 10 / phút / tài khoản |

Vượt → `429 GNE-RAT-4290`, header `Retry-After`.

### 0.12 Cache phía máy chủ / CDN

| Endpoint | Cache |
|---|---|
| `GET /public/categories` | 1 giờ |
| `GET /public/banners` | 60 giây |
| `GET /public/flash-sales/current` | 30 giây |
| `GET /public/products/{id}` | 5 phút (giá, tồn kho đọc thẳng DB khi thêm giỏ / đặt hàng) |
| `GET /public/regions/**` | 24 giờ |

`ETag` + `If-None-Match` → `304` cho mọi `GET /public/**`.

---

## 1. Schema dùng chung

### `$OtpChallenge`
```json
{ "challenge_id": "otp_9f2c1a", "purpose": "WITHDRAWAL", "email_masked": "tr****@gmail.com", "expires_in": 300, "resend_after": 60 }
```
`purpose` ∈ `REGISTER, LOGIN, PASSWORD_RESET, PASSWORD_CHANGE, ACCOUNT_DELETION, WITHDRAWAL, PAYOUT_ACCOUNT_UPDATE, PAYOUT_ACCOUNT_DELETE, SHOP_REGISTRATION, EMAIL_CHANGE, ACCOUNT_UNLOCK`.

### `$AuthTokens`
```json
{
  "access_token": "eyJhbGciOi...",
  "token_type": "Bearer",
  "expires_in": 900,
  "user": { "id": 20411, "role": "CUSTOMER", "full_name": "Trần Thị Mai", "avatar_url": null }
}
```
`user.role` để client chuyển hướng: `CUSTOMER/VENDOR` → Storefront, `STAFF/ADMIN` → Admin Console.

### `$ShopMini`
```json
{ "id": 15, "name": "TechZone Official", "logo_url": "https://cdn.genie.vn/avatar-assets/shop/15/logo.webp", "is_mall": true }
```

### `$ProductCard` — thẻ sản phẩm ở mọi lưới (trang chủ, danh mục, tìm kiếm, shop, tương tự, Flash Sale)
```json
{
  "id": 1,
  "slug": "tai-nghe-bluetooth-chong-on-anc-pro-5",
  "name": "Tai nghe Bluetooth chống ồn ANC Pro 5 — Pin 40 giờ",
  "image_url": "https://cdn.genie.vn/product-media/vendor/15/product/1/main.webp",
  "price": 389000,
  "original_price": 799000,
  "discount_percent": 51,
  "rating_avg": 4.8,
  "sold_count": 2100,
  "shop": { "id": 15, "name": "TechZone Official" },
  "is_free_shipping": true,
  "is_pre_order": false,
  "is_shop_on_vacation": false,
  "flash_sale": { "sold_count": 45, "total_count": 50 }
}
```
- `price` = giá thấp nhất trong các biến thể đang bán (hoặc giá Flash Sale khi đang chạy); `original_price` có thể `null`.
- `flash_sale` chỉ khác `null` khi sản phẩm ở trong chiến dịch đang `ACTIVE`. Thanh "Đã bán 45/50" / "Đã hết suất" vẽ từ hai số này.
- **Không** có cờ "đã thích" — client so với `GET /me/wishlist/product-ids` (mục 3.10).

### `$Region`
```json
{ "id": 202, "name": "TP. Hồ Chí Minh" }
```
Phường/xã dùng `code` (chuỗi, chuẩn GHN) thay cho `id`: `{ "code": "20308", "name": "P. Bến Nghé" }`.

### `$Address`
```json
{
  "id": 1,
  "recipient_name": "Trần Thị Mai",
  "phone_number": "0901234567",
  "province": { "id": 202, "name": "TP. Hồ Chí Minh" },
  "district": { "id": 1442, "name": "Quận 1" },
  "ward": { "code": "20308", "name": "P. Bến Nghé" },
  "address_line": "25 Nguyễn Huệ, Căn hộ 12A",
  "label": "HOME",
  "is_default": true
}
```
`label` ∈ `HOME` (Nhà riêng), `OFFICE` (Văn phòng).

### `$AddressSnapshot` — địa chỉ đã chụp trên đơn (chỉ hiển thị)
```json
{ "recipient_name": "Trần Thị Mai", "phone_number": "0901234567", "full_address": "25 Nguyễn Huệ, Căn hộ 12A, P. Bến Nghé, Quận 1, TP. Hồ Chí Minh" }
```

### `$Voucher` — voucher phía khách (kho voucher, picker, vé "Lưu" ở trang chủ / shop / chiến dịch)
```json
{
  "id": 3,
  "code": "TECH15",
  "title": "Giảm 15% tối đa 50k",
  "type": "SHOP",
  "discount_type": "PERCENTAGE",
  "discount_value": 15,
  "max_discount": 50000,
  "min_order_value": 250000,
  "shop": { "id": 15, "name": "TechZone Official" },
  "scope": "ALL",
  "included_categories": [],
  "excluded_categories": [],
  "valid_from": "2026-06-01T00:00:00+07:00",
  "valid_to": "2026-06-20T23:59:59+07:00",
  "state": "AVAILABLE",
  "usage": { "used": 412, "limit": 800 },
  "funded_by": "VENDOR",
  "description": "Chỉ áp dụng cho sản phẩm của TechZone Official.",
  "is_saved": true
}
```
- `type` ∈ `PLATFORM, SHOP, FREESHIP`; `discount_type` ∈ `PERCENTAGE, FIXED_AMOUNT, FREESHIP`.
- `shop` = `null` với voucher sàn. `included_categories` / `excluded_categories` là mảng **tên** danh mục (để hiện "Chỉ áp dụng: Điện tử" / "Ngoại trừ: Điện tử").
- `state` ∈ `AVAILABLE` (dùng được), `UPCOMING` (chưa tới giờ), `USED`, `EXPIRED`.
- `is_saved` chỉ có khi gọi kèm token; `used_order_code` có thêm khi `state = USED`.

### `$OrderItem` — dòng hàng trên đơn
```json
{
  "id": 9001,
  "product_id": 1,
  "product_slug": "tai-nghe-bluetooth-chong-on-anc-pro-5",
  "variant_id": 101,
  "product_name": "Tai nghe Bluetooth chống ồn ANC Pro 5 — Pin 40 giờ",
  "image_url": "https://cdn.genie.vn/.../main.webp",
  "variant_name": "Đen",
  "quantity": 1,
  "unit_price": 389000
}
```

### `$MoneySummary` — khối tổng tiền
```json
{ "subtotal": 848000, "shipping_fee": 25000, "discount_total": 60000, "coin_discount": 0, "grand_total": 813000 }
```

### `$Media`
```json
{ "type": "IMAGE", "url": "https://cdn.genie.vn/review-media/.../1.webp" }
```
`type` ∈ `IMAGE, VIDEO`.

---

## 2. Xác thực — Auth

**Màn hình:** `auth/index.html` — Đăng nhập (tab *Mật khẩu* / *Mã OTP*), Đăng ký, Nhập OTP, Quên mật khẩu (email → OTP → đặt mật khẩu → xong / tạm khóa), Tài khoản tạm khóa (đếm ngược), Đăng nhập Google/Apple.

### 2.1 `POST /auth/registrations` — Đăng ký người mua
UC-AUTH-01 · Không cần đăng nhập

```json
{
  "full_name": "Nguyễn Văn A",
  "email": "ban@email.com",
  "phone_number": "0901234567",
  "password": "Genie@123",
  "accept_terms": true
}
```
Luật: mật khẩu ≥ 8 ký tự, có hoa, thường, số, ký tự đặc biệt; `accept_terms = true`.

`202` → `$OtpChallenge` (`purpose = REGISTER`). **Chưa tạo bản ghi `users`**: dữ liệu đăng ký (mật khẩu đã băm Argon2id) giữ tạm ở Redis `register:temp:{email}` 15 phút; chỉ khi OTP đúng mới tạo `users` (`ACTIVE`) + `customer_profiles` (plan AUTH D2, D3). `UNVERIFIED` chỉ dùng cho nhân viên được mời chưa kích hoạt. Email chuẩn hóa `trim` + chữ thường; `phone_number` khớp `^0\d{9,10}$`.

| Lỗi | Khi nào |
|---|---|
| `409 GNE-AUTH-4091` | Email đã có tài khoản (mọi trạng thái trừ `ANONYMIZED`) → giao diện mở hộp thoại "Email này đã được đăng ký" + nút Đi tới đăng nhập |
| `422 GNE-VAL-4220` | Sai định dạng từng trường; mật khẩu yếu → `errors[].code = PASSWORD_TOO_WEAK` |

### 2.2 `POST /auth/otp-challenges` — Gửi OTP đăng nhập / quên mật khẩu
UC-AUTH-02 (luồng OTP), UC-AUTH-04

```json
{ "purpose": "LOGIN", "email": "ban@email.com" }
```
`purpose` ∈ `LOGIN, PASSWORD_RESET, ACCOUNT_UNLOCK`. `202` → `$OtpChallenge`.
`ACCOUNT_UNLOCK`: nút **"Xác minh qua email"** trên màn "Tài khoản tạm khoá" — chỉ gửi được khi tài khoản đang bị khóa tạm do sai mật khẩu / OTP 5 lần (`409 GNE-AUTH-4096` nếu không bị khóa; tài khoản bị Admin đình chỉ dùng "Liên hệ hỗ trợ" — trang tĩnh).
Với `PASSWORD_RESET` và `LOGIN` luôn trả `202` dù email không tồn tại / không hoạt động (không gửi thư — chống dò email; màn hình ghi "Nếu email … tồn tại, mã OTP đã được gửi"). Tài khoản chỉ có Google **vẫn nhận mã** `PASSWORD_RESET` — đó là đường "Thiết lập mật khẩu" khi chưa đăng nhập. Email đang bị khóa khôi phục (sai OTP quên mật khẩu 5 lần, 30 phút) → `202` nhưng không gửi.
Giới hạn: gửi lại sau 60 giây, tối đa 5 mã / giờ / email (`429 GNE-RAT-4290` + `Retry-After`). Mỗi email / mục đích chỉ một mã còn sống — mã mới hủy mã cũ.

### 2.3 `POST /auth/otp-challenges/{challenge_id}/resend` — Gửi lại OTP
`202` → `$OtpChallenge` mới (`expires_in` đặt lại 300). `429 GNE-RAT-4290` (kèm `Retry-After`) nếu chưa đủ 60 giây hoặc quá 5 lần / giờ.

### 2.4 `POST /auth/otp-challenges/{challenge_id}/verify` — Xác nhận OTP
```json
{ "otp": "123456" }
```
Kết quả theo `purpose` của challenge:

| `purpose` | Phản hồi | Màn hình kế tiếp |
|---|---|---|
| `REGISTER` | `201` `$AuthTokens` (kích hoạt tài khoản và đăng nhập luôn) | Storefront |
| `LOGIN` | `200` `$AuthTokens` | Storefront |
| `PASSWORD_RESET` | `200` `{ "reset_token": "rst_7c1e...", "expires_in": 600 }` | Đặt mật khẩu mới |
| `ACCOUNT_UNLOCK` | `200` `$AuthTokens` — xóa `locked_until`, đặt lại số lần sai, đăng nhập luôn; ghi `system_logs ACCOUNT_REACTIVATED` | Storefront |

| Lỗi | Khi nào | Giao diện |
|---|---|---|
| `422 GNE-AUTH-4222` | Sai mã, `details.remaining_attempts` | "Mã không đúng. Còn 3 lần thử" |
| `422 GNE-AUTH-4221` | Sai lần thứ 5. `REGISTER` → hủy đăng ký; `LOGIN` → khóa 15 phút (`details.locked_until`); `PASSWORD_RESET` → khóa khôi phục 30 phút | "Đăng ký thất bại" / màn tạm khóa |
| `410 GNE-AUTH-4101` | Quá 5 phút | "Mã OTP đã hết hiệu lực" |

### 2.5 `POST /auth/sessions` — Đăng nhập bằng mật khẩu
UC-AUTH-02

```json
{ "email": "ban@email.com", "password": "Genie@123" }
```
`201` → `$AuthTokens` + `Set-Cookie: genie_rt=…`

| Lỗi | Khi nào | `details` |
|---|---|---|
| `401 GNE-AUTH-4011` | Sai email/mật khẩu (email không tồn tại trả cùng mã, không kèm `remaining_attempts`) | `{ "remaining_attempts": 4 }` |
| `401 GNE-AUTH-4012` | Tài khoản chỉ có Google / Apple (`password = NULL`) — **không** tính là một lần sai; giao diện gợi ý Đăng nhập bằng Google / bằng mã email / Thiết lập mật khẩu | `{ "reason": "PASSWORD_NOT_SET", "provider": "google" }` |
| `423 GNE-AUTH-4230` | Sai 5 lần hoặc đang bị khóa | `{ "locked_until": "2026-06-08T14:37:00+07:00", "locked_by": "PASSWORD" }` (`locked_by` ∈ `PASSWORD, OTP`) — màn "Tài khoản tạm khoá" đếm ngược |
| `403 GNE-AUTH-4031` | Tài khoản bị đình chỉ (đình chỉ đã hết hạn thì tự mở lại) | `{ "reason": "...", "until": "..." }` |
| `429 GNE-RAT-4290` | Quá 10 lần / phút / IP | `Retry-After` |

Cookie: `genie_rt` — `HttpOnly; SameSite=Strict; Path=/api/v1/auth; Max-Age=604800`, `Secure` ở prod. Access token JWT HS256 15 phút, claim `sub`, `role`, `sid` (= mã phiên `family_id`), `jti`. Đăng nhập từ thiết bị / mạng lạ (90 ngày chưa thấy cùng trình duyệt + hệ điều hành và cùng dải IP /24) → email cảnh báo `NEW_DEVICE_LOGIN`.

### 2.6 `POST /auth/sessions/refresh` — Làm mới access token
Dùng cookie `genie_rt`. `200` → `$AuthTokens` **kèm `user`** (web tải lại trang chỉ còn cookie, cần biết ai đang đăng nhập — đã đổi so với bản trước) + cookie mới; token mới giữ cùng `family_id`, cập nhật `last_used_at`. Thiếu cookie / token hết hạn → `401 GNE-SEC-4010`. Token đã xoay vòng bị dùng lại: trong 10 giây ân hạn (hai tab cùng làm mới) chỉ trả `401`; quá 10 giây → thu hồi **mọi** phiên của người dùng (`REUSE_DETECTED`) + chặn access token cũ (`auth:revoked_before`), xóa cookie, `401 GNE-SEC-4010`.

### 2.7 `DELETE /auth/sessions/current` — Đăng xuất
UC-AUTH-11 · `Bearer` (access token hết hạn vẫn đăng xuất được bằng cookie). Thu hồi cả phiên theo `sid` (`USER_LOGOUT`), đưa `jti` vào `auth:denylist`, `204`, xóa cookie.

### 2.8 `POST /auth/password-resets` — Đặt mật khẩu mới
UC-AUTH-04

```json
{ "reset_token": "rst_7c1e...", "new_password": "NewPass@2026" }
```
`204`. Gỡ khóa đăng nhập tạm (nếu có), thu hồi mọi phiên đăng nhập cũ và chặn ngay access token cũ; ghi `PASSWORD_RESET` (tài khoản chưa có mật khẩu: `method = SETUP`); gửi email "Mật khẩu Genie Shop của bạn vừa được thay đổi". `reset_token` dùng một lần.

| Lỗi | Khi nào |
|---|---|
| `422 GNE-AUTH-4223` | Mật khẩu mới trùng mật khẩu gần nhất |
| `410 GNE-AUTH-4102` | `reset_token` hết hạn |

### 2.9 `GET /auth/oauth/{provider}/authorize` — Bắt đầu đăng nhập Google / Apple
UC-AUTH-03 · `provider` ∈ `google, apple`

Query: `next` (trang cần quay về sau khi đăng nhập, chỉ đường dẫn nội bộ). Callback **cố định** của web: `<web>/auth/oauth-callback` (trùng "Authorized redirect URI" khai ở Google Cloud Console) — không nhận `redirect_uri` từ client (chống open redirect). `302` tới trang đăng nhập của nhà cung cấp (Authorization Code + PKCE S256, có `state`, `nonce`; `state` lưu Redis 10 phút kèm `next`). Chưa cấu hình Google → `302` về callback `?error=GNE-SYS-5030`. Apple tắt bằng cờ `genie.oauth.apple.enabled = false` cho tới khi có tài khoản Apple Developer.

### 2.10 `POST /auth/oauth/{provider}/token` — Hoàn tất đăng nhập mạng xã hội
Web nhận `code` ở trang callback rồi gọi:

```json
{ "code": "4/0AX4XfW...", "state": "st_91ab" }
```
`200` + cookie `genie_rt`:
```json
{ "result": "LINKED", "tokens": { "...$AuthTokens": "..." }, "next": "/cart" }
```
`result` theo luồng `account-linking-google-login.md`: `LOGGED_IN` (đã liên kết từ trước — tìm theo `sub`), `LINKED` (gắn vào tài khoản cùng email), `CREATED` (tạo tài khoản mới, chưa có mật khẩu). Ảnh đại diện Google được tải về lưu Silo (`avatar-assets`); tên / ảnh chỉ điền khi hồ sơ đang trống. `state` dùng một lần.

| Lỗi | Khi nào |
|---|---|
| `403 GNE-AUTH-4032` | `email_verified = false` → không liên kết, tránh chiếm tài khoản |
| `403 GNE-AUTH-4031` | Tài khoản (đã liên kết / cùng email) đang bị đình chỉ |
| `423 GNE-AUTH-4230` | Tài khoản đang bị khóa tạm |
| `409 GNE-AUTH-4092` | Tài khoản cùng email đã gắn một nhà cung cấp khác (mỗi tài khoản tối đa 1 nhà cung cấp) |
| `400 GNE-AUTH-4001` | `state` không có / hết hạn / là state liên kết; `nonce` / chữ ký `id_token` không hợp lệ |

### 2.11 `POST /auth/staff-activations` — Nhân viên kích hoạt lời mời
UC-ADM-01 · Mở từ link trong email mời (màn hình kích hoạt chưa có trong prototype nhưng bắt buộc để luồng "Mời nhân viên" chạy được)

```json
{ "invitation_token": "inv_5d2a...", "otp": "123456", "password": "Staff@2026" }
```
`201` → `$AuthTokens`. `410 GNE-AUTH-4103` lời mời hết hạn.

---

## 3. Tài khoản của tôi — `/me`

Dùng chung cho khách, chủ shop (tài khoản shop chính là tài khoản người mua của chủ shop) và nhân viên.

### 3.1 `GET /me` — Ngữ cảnh phiên (header + sidebar)
**Màn hình:** Header Storefront (avatar, tên, badge chuông, badge giỏ, nút chat nổi), menu người dùng ("Chuyển đổi tài khoản" sang shop), sidebar Tài khoản (badge Thông báo / Kho Voucher / Shop theo dõi, dòng "Email và Google").

`200`:
```json
{
  "id": 20411,
  "role": "CUSTOMER",
  "full_name": "Trần Thị Mai",
  "avatar_url": null,
  "email": "mai.tran@gmail.com",
  "login_methods": { "password": true, "google": true, "zalo": false },
  "shop": { "id": 15, "name": "TechZone Official", "logo_url": null, "kyc_status": "VERIFIED" },
  "counters": {
    "cart_items": 11,
    "unread_notifications": 3,
    "unread_chats": 3,
    "available_vouchers": 12,
    "following_shops": 3
  }
}
```
`shop` = `null` khi chưa mở shop (ẩn mục chuyển tài khoản). Gọi một lần khi tải trang và ngay sau đăng nhập / làm mới token (web dựng `SessionUser` — plan AUTH D18); các bộ đếm cập nhật qua WebSocket (mục 13). Dữ liệu `shop`, `counters` do phân hệ khác cung cấp qua cổng đảo ngược (`AccountShopProvider`, `AccountCounterContributor`) — phân hệ chưa làm thì trả `null` / `0`. *(Đã hiện thực 2026-11-17 — `MeController`; `login_methods.zalo` thêm theo plan AUTH D23.)*

### 3.2 Hồ sơ cá nhân
UC-AUTH-08 · **Màn hình:** Tài khoản › Hồ sơ

#### `GET /me/profile`
```json
{
  "full_name": "Trần Thị Mai",
  "email": "mai.tran@gmail.com",
  "email_verified": true,
  "phone_number": "0901234567",
  "gender": "FEMALE",
  "date_of_birth": "1996-08-12",
  "bio": "Thích săn deal đồ công nghệ & mỹ phẩm",
  "preferred_language": "vi",
  "avatar_url": null,
  "deletion_request": null
}
```
`gender` ∈ `MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY`. `deletion_request` khác `null` khi đang chờ xóa → hiện băng đỏ "Tài khoản sẽ được ẩn danh sau 30 ngày": `{ "requested_at": "...", "anonymize_at": "..." }`.

#### `PATCH /me/profile`
Gửi trường nào cập nhật trường đó.
```json
{
  "full_name": "Trần Thị Mai",
  "phone_number": "0901234567",
  "gender": "FEMALE",
  "date_of_birth": "1996-08-12",
  "bio": "...",
  "preferred_language": "en",
  "avatar_object_key": "tmp/user/20411/9c1f2a7e-….png"
}
```
`200` → hồ sơ đầy đủ như `GET`. `null` / không gửi = giữ nguyên; chuỗi rỗng ở `phone_number`, `bio` = xóa. Ảnh đại diện: JPG/PNG ≤ 1 MB (`422 GNE-VAL-4220` nếu vượt); `avatar_object_key` phải là khóa tạm của chính người gọi đã tải lên — server chuyển sang `user/{id}/{uuid}.{ext}` trong bucket `avatar-assets`, lưu URL công khai, xóa ảnh cũ sau khi commit. `full_name` rỗng, `date_of_birth` sau hôm nay, `preferred_language` ∉ `vi, en` → `422`.

#### `POST /me/email-change-requests` — Đổi email đăng nhập
**Màn hình:** Hồ sơ › Email › "Thay đổi" → modal "Đổi email đăng nhập" (email mới → mã gửi tới email mới → Xác nhận)
```json
{ "new_email": "mai.tran.work@gmail.com" }
```
`202` → `$OtpChallenge` (`purpose = EMAIL_CHANGE`, mã gửi tới **email mới**).

| Lỗi | Khi nào |
|---|---|
| `409 GNE-AUTH-4094` | Email mới đã thuộc tài khoản khác |
| `422 GNE-AUTH-4224` | Email mới trùng email hiện tại |
| `409 GNE-AUTH-4095` | Tài khoản chỉ có Google, chưa có mật khẩu → phải "Thiết lập mật khẩu" trước (giao diện hiện gợi ý thay vì mở modal) |

#### `POST /me/email-change-requests/{challenge_id}/confirm`
`{ "otp": "123456" }` → `200` `{ "email": "mai.tran.work@gmail.com", "email_verified": true }`. Ghi `EMAIL_CHANGED`, gửi email báo tới địa chỉ cũ; liên kết Google giữ nguyên (nhận diện bằng `sub` — `users.oauth_provider_id`). *(Chưa hiện thực — 2026-11-16.)* Sai mã theo luật mục 0.7.

### 3.3 Liên kết đăng nhập
UC-AUTH-03 · **Màn hình:** Tài khoản › Liên kết đăng nhập; Seller › Cài đặt › Bảo mật (chip "Đăng nhập bằng")

#### `GET /me/login-methods`
```json
{
  "email": "mai.tran@gmail.com",
  "password": { "enabled": true },
  "identities": [ { "provider": "google", "email": "mai.tran@gmail.com", "linked_at": "2026-03-02T10:00:00+07:00" } ]
}
```
`email`, `linked_at` của Google lấy từ bản ghi `OAUTH_LINKED` gần nhất trong `system_logs` (bảng `users` không lưu email Google). Mỗi tài khoản TLCN tối đa **một** nhà cung cấp.

#### `POST /me/identities/{provider}/authorize` — Bắt đầu liên kết *(bổ sung khi hiện thực)*
`Bearer` · gọi bằng XHR (trình duyệt chuyển hướng thẳng không mang access token). `200` `{ "authorize_url": "https://accounts.google.com/o/oauth2/v2/auth?..." }` — web chuyển trình duyệt tới URL này; `state` ghi kèm người đang đăng nhập nên chỉ đúng người đó hoàn tất được. Google quay về callback chung `/auth/oauth-callback`, web nhận ra ý định "liên kết" rồi gọi endpoint dưới.

#### `POST /me/identities/{provider}` — Liên kết Google
`provider` viết thường (`google`, `apple`).
```json
{ "code": "4/0AX4XfW...", "state": "st_91ab" }
```
`201` → `GET /me/login-methods`; web quay về `/account/logins?linked=google`. Đã gắn đúng Google này → coi như thành công.

| Lỗi | Khi nào |
|---|---|
| `409 GNE-AUTH-4098` | **Email Google khác email tài khoản Genie** — một người = một tài khoản = một email (`details.account_email`); giao diện nhắc chọn đúng tài khoản Google |
| `409 GNE-AUTH-4092` | Tài khoản Google này đã gắn với người dùng khác, hoặc tài khoản đã gắn nhà cung cấp khác (`details.reason`) |
| `403 GNE-AUTH-4032` | Google trả `email_verified = false` |
| `400 GNE-AUTH-4001` | `state` không phải state liên kết của chính người gọi / hết hạn |

#### `DELETE /me/identities/{provider}` — Gỡ liên kết
Body bắt buộc nhập lại mật khẩu hiện tại (thao tác nhạy cảm — người mượn máy đang đăng nhập không gỡ được):
```json
{ "current_password": "Genie@123" }
```
`204`, ghi `OAUTH_UNLINKED`. `409 GNE-AUTH-4093` nếu đây là cách đăng nhập duy nhất (chưa có mật khẩu) — nút "Gỡ liên kết" bị khóa trên giao diện; `401 GNE-AUTH-4011` sai mật khẩu; tối đa 10 lần / phút (dùng chung bộ đếm với `POST /me/password/verify`, `429`). Chưa gắn provider này → coi như đã gỡ.

### 3.4 Mật khẩu
UC-AUTH-06 · **Màn hình:** Tài khoản › Đổi / Thiết lập mật khẩu; Seller › Cài đặt › Bảo mật; Admin › Tài khoản

#### `POST /me/password/verify` — Kiểm tra mật khẩu hiện tại khi đang gõ
```json
{ "password": "Genie@123" }
```
`200` `{ "valid": true }` — dòng "Chính xác / Không chính xác". Giới hạn 10 lần/phút.

#### `PUT /me/password` 🔐 `purpose = PASSWORD_CHANGE`
```json
{ "current_password": "Genie@123", "new_password": "NewPass@2026" }
```
- `current_password` **bắt buộc** khi `login_methods.password = true`; bỏ trống khi tài khoản chỉ có Google (Thiết lập mật khẩu lần đầu).
- Header OTP bước 2 (`X-OTP-Challenge-Id`, `X-OTP-Code`) được kiểm **sau** khi body hợp lệ (body sai không làm tiêu mã).
- `204`. Thu hồi mọi phiên **khác** phiên hiện tại (`revoked_reason = PASSWORD_CHANGED`), giữ phiên đang dùng; ghi `PASSWORD_CHANGED` (`method = CHANGE` / `SETUP`), gửi email cảnh báo.

| Lỗi | Khi nào |
|---|---|
| `401 GNE-AUTH-4011` | Mật khẩu hiện tại sai |
| `422 GNE-AUTH-4223` | Mật khẩu mới trùng mật khẩu hiện tại |
| `422 GNE-VAL-4220` | Không đủ độ mạnh |

### 3.5 OTP bước 2
#### `POST /me/otp-challenges`
```json
{ "purpose": "ACCOUNT_DELETION" }
```
`202` → `$OtpChallenge`. Một số `purpose` kiểm tra điều kiện **trước khi gửi mã**, để màn hình báo lỗi ngay:

| `purpose` | Điều kiện | Lỗi |
|---|---|---|
| `ACCOUNT_DELETION` | Không còn đơn đang xử lý | `409 GNE-USR-4091`, `details.active_order_codes: ["TR-20260608-000044", …]` |
| `PAYOUT_ACCOUNT_DELETE` | Không có lệnh rút đang chờ | `409 GNE-WAL-4091` |
| `WITHDRAWAL` | Đã có tài khoản nhận tiền xác minh | `403 GNE-WAL-4032` |

#### `POST /me/otp-challenges/{challenge_id}/resend`
`202` → `$OtpChallenge`.

### 3.6 Thiết bị đăng nhập
UC-AUTH-11 · **Màn hình:** Tài khoản › Thiết bị đăng nhập; Admin › Tài khoản › Phiên đăng nhập

#### `GET /me/sessions`
```json
[
  { "id": 501, "device_name": "Windows PC · Chrome 126", "device_type": "DESKTOP", "location": "TP. Hồ Chí Minh, VN", "ip_address": "113.161.40.12", "last_active_at": "2026-06-22T09:40:00+07:00", "is_current": true },
  { "id": 498, "device_name": "iPhone 15 · Genie App", "device_type": "MOBILE", "location": "TP. Hồ Chí Minh, VN", "ip_address": "171.244.10.5", "last_active_at": "2026-06-22T07:40:00+07:00", "is_current": false }
]
```
`device_type` ∈ `DESKTOP, MOBILE, TABLET`. Nguồn: bảng `refresh_tokens` còn hiệu lực — mỗi phiên (`family_id`) một dòng, lấy token mới nhất; `id` = `family_id` (ổn định qua các lần xoay vòng); `device_name` / `device_type` suy từ User-Agent; `last_active_at` = `last_used_at` (hoặc `created_at`); `location` hiện trả `null` (chưa có GeoIP — DATN); phiên hiện tại đứng đầu.

#### `DELETE /me/sessions/{id}` — Đăng xuất một thiết bị
`204`. `409 GNE-AUTH-4097` nếu `id` là phiên hiện tại (dùng `DELETE /auth/sessions/current`) — giao diện không hiện nút này ở dòng "Thiết bị hiện tại".

#### `DELETE /me/sessions?scope=others` — Đăng xuất tất cả thiết bị khác
`204`. Chỉ thu hồi refresh token của các phiên khác (ghi `SESSION_REVOKED`); access token đã cấp trên máy khác tự hết hạn trong ≤ 15 phút.

### 3.7 Xóa tài khoản
UC-AUTH-10 · **Màn hình:** Tài khoản › Hồ sơ › "Xóa tài khoản"

Luồng: `POST /me/otp-challenges {purpose: ACCOUNT_DELETION}` (nếu còn đơn đang xử lý → `409` kèm danh sách đơn, modal hiện danh sách) → nhập OTP →

#### `PUT /me/deletion-request` 🔐 `purpose = ACCOUNT_DELETION`
Không body. `200`:
```json
{ "requested_at": "2026-06-22T09:45:00+07:00", "anonymize_at": "2026-07-22T09:45:00+07:00" }
```
Kiểm lại điều kiện chặn (đơn phát sinh trong lúc nhập mã vẫn bị chặn `409 GNE-USR-4091`); ghi `users.deletion_requested_at`, `ACCOUNT_DELETION_REQUESTED`; **đăng xuất các thiết bị khác, giữ phiên hiện tại** (để khách thấy băng đỏ và nút hủy); email "Tài khoản sẽ được ẩn danh vào ngày …" kèm nút Giữ lại tài khoản. Đã có yêu cầu → trả lại yêu cầu cũ (không tính lại 30 ngày). Trong 30 ngày vẫn đăng nhập được. Cron 02:30 hằng ngày ẩn danh tài khoản quá hạn (LLD 10).

#### `DELETE /me/deletion-request` — Hủy yêu cầu xóa (trong 30 ngày)
`204`, ghi `ACCOUNT_DELETION_CANCELLED`. Không có yêu cầu → vẫn `204`.

### 3.8 Sổ địa chỉ
UC-AUTH-09 · **Màn hình:** Tài khoản › Địa chỉ; modal "Địa chỉ của tôi" ở Thanh toán

| Method & Path | Mô tả | Body | Phản hồi |
|---|---|---|---|
| `GET /me/addresses` | Danh sách, mặc định đứng đầu | — | `200` `$Address[]` |
| `POST /me/addresses` | Thêm | `$AddressInput` | `201` `$Address` |
| `PUT /me/addresses/{id}` | Sửa toàn bộ | `$AddressInput` | `200` `$Address` |
| `PATCH /me/addresses/{id}` | Đặt mặc định | `{ "is_default": true }` | `200` `$Address` |
| `DELETE /me/addresses/{id}` | Xóa (mềm) | — | `204`; `409 GNE-USR-4092` nếu là địa chỉ mặc định |

`$AddressInput`:
```json
{
  "recipient_name": "Nguyễn Văn A",
  "phone_number": "0901234567",
  "province_id": 202,
  "district_id": 1442,
  "ward_code": "20308",
  "address_line": "25 Nguyễn Huệ",
  "label": "HOME",
  "is_default": false
}
```
Luật: SĐT `^0\d{9,10}$`; đủ 3 cấp hành chính; `address_line` không rỗng. Địa chỉ đầu tiên tự thành mặc định.

### 3.9 Đơn vị hành chính (GHN) & định vị
**Màn hình:** bộ chọn Tỉnh → Quận → Phường (tab + ô tìm nhanh) ở form địa chỉ khách, địa chỉ lấy hàng của shop, nút "Dùng vị trí hiện tại (GPS)".

| Method & Path | Phản hồi |
|---|---|
| `GET /public/regions/provinces` | `$Region[]` |
| `GET /public/regions/provinces/{province_id}/districts` | `$Region[]` |
| `GET /public/regions/districts/{district_id}/wards` | `[{ "code": "20308", "name": "P. Bến Nghé" }]` |
| `GET /public/regions/reverse-geocode?lat=10.7769&lng=106.7009` | `{ "province": $Region, "district": $Region, "ward": {code,name}, "address_line": "72 Lê Thánh Tôn" }` |

Ô "Tìm nhanh" lọc phía client (danh sách nhỏ, cache 24 giờ).

### 3.10 Sản phẩm yêu thích
UC-CAT-08 · **Màn hình:** nút tim trên `$ProductCard` và trang chi tiết; Tài khoản › Sản phẩm yêu thích

#### `GET /me/wishlist/product-ids` — Tập ID để tô tim
`200` `[2, 13]`. Gọi một lần khi tải trang, client giữ trong bộ nhớ.

#### `GET /me/wishlist?cursor&limit=20`
```json
[
  { "product_id": 2, "name": "Đồng hồ thông minh Watch S2 màn AMOLED, đo SpO2", "image_url": "...", "price": 690000, "default_variant_id": 201, "is_purchasable": true }
]
```
`summary.total` cho tiêu đề "Sản phẩm yêu thích (2)". `is_purchasable = false` khi shop đang nghỉ / sản phẩm hết bán → khóa nút "Thêm vào giỏ" (nút gọi `POST /me/cart/items` với `default_variant_id`).

#### `PUT /me/wishlist/{product_id}` — Thích (idempotent) · `204`
#### `DELETE /me/wishlist/{product_id}` — Bỏ thích · `204`

### 3.11 Shop đang theo dõi
UC-SOC-06 · **Màn hình:** nút Theo dõi ở trang Shop & trang chi tiết; Tài khoản › Shop đang theo dõi

#### `GET /me/following/shop-ids` → `200` `[15, 17, 12]`
#### `GET /me/following?cursor&limit=20`
```json
[ { "shop": { "id": 15, "name": "TechZone Official", "logo_url": null, "is_mall": true }, "rating_avg": 4.9, "follower_count": 128000 } ]
```
`summary.total` cho "Shop đang theo dõi (3)".

#### `PUT /me/following/{shop_id}` · `204` — cộng `follower_count`
#### `DELETE /me/following/{shop_id}` · `204`

### 3.12 Thông báo
UC-NOT-01, UC-NOT-05 · **Màn hình:** chuông + panel 5 tin ở header Storefront / Seller / Admin; Tài khoản › Thông báo (tab Tất cả / Đơn hàng / Khuyến mãi / Hệ thống / Chat, "Xem thêm"); modal "Chi tiết thông báo"

Một endpoint cho mọi vai trò: khách nhận thông báo cá nhân + thông báo sàn; chủ shop nhận thêm thông báo của shop; nhân viên nhận `admin_notifications`.

#### `GET /me/notifications?category=ORDER&cursor&limit=5`
`category` ∈ `ORDER, PROMOTION, SYSTEM, CHAT` (bỏ trống = tất cả).
```json
[
  {
    "id": "ntf_884",
    "category": "ORDER",
    "type": "ORDER_STATUS_CHANGED",
    "title": "Đơn TR-20260608-000044 đang giao",
    "body": "GHN đang trung chuyển đến kho HCM, dự kiến giao 11/06.",
    "created_at": "2026-06-22T08:00:00+07:00",
    "is_read": false,
    "link": { "type": "ORDER", "id": 44 }
  },
  {
    "id": "pn_1047",
    "category": "SYSTEM",
    "type": "PLATFORM_ANNOUNCEMENT",
    "title": "Genie nâng cấp hệ thống 02:00 – 04:00",
    "body": "Một số dịch vụ có thể gián đoạn…",
    "created_at": "2026-06-18T09:00:00+07:00",
    "is_read": true,
    "link": { "type": "PLATFORM_NOTIFICATION", "id": 1047 }
  }
]
```
- `id` có tiền tố nguồn: `ntf_` (bảng `notifications`), `pn_` (`platform_notifications`), `an_` (`admin_notifications`).
- `type` là chuỗi: dòng `ntf_` dùng `NotificationType`, dòng `pn_` là `PLATFORM_ANNOUNCEMENT`, dòng `an_` của nhân viên dùng `AdminNotificationType` (`SYSTEM_ALERT, NEW_ORDER, USER_APPROVAL, WITHDRAW_REQUEST, ESCROW_DISCREPANCY` — ENTITY.md Phụ lục A).
- `link.type` ∈ `ORDER, VOUCHER_WALLET, SECURITY, HOME, PLATFORM_NOTIFICATION, PRODUCT, KYC, WITHDRAWAL, ADMIN_KYC_QUEUE, ADMIN_PRODUCT_QUEUE, ADMIN_WITHDRAWALS` — client đổi thành route. `link` = `null` thì chỉ mở modal chi tiết.
- `summary.unread_count` cho dòng "Bạn có 3 thông báo chưa đọc".

#### `PATCH /me/notifications/{id}` — Đánh dấu đã đọc
`{ "is_read": true }` → `204`.

#### `POST /me/notifications/mark-all-read`
`{ "category": null }` → `204`.

### 3.13 Điểm Genie
UC-PRO-08, UC-PRO-09 · **Màn hình:** Tài khoản › Điểm Genie (số dư, "150 Điểm sắp hết hạn trong 30 ngày", điểm danh 7 ngày, lịch sử, "Đổi quà")

#### `GET /me/coins`
```json
{
  "balance": 1250,
  "expiring_soon": { "amount": 150, "within_days": 30 },
  "check_in": {
    "streak_days": 3,
    "checked_in_today": false,
    "next_check_in_at": "2026-06-23T00:00:00+07:00",
    "rewards": [100, 100, 100, 100, 100, 100, 1000]
  }
}
```
`rewards[i]` = số Điểm của ngày thứ `i+1` trong chuỗi 7 ngày.

#### `POST /me/coins/check-ins` — Điểm danh
`201` `{ "reward": 100, "balance": 1350, "streak_days": 4 }`. `409 GNE-PRO-4091` đã điểm danh hôm nay.

#### `GET /me/coins/transactions?cursor&limit=20`
```json
[ { "id": 7781, "type": "ORDER_CASHBACK", "description": "Hoàn điểm đơn TR-20260608-000041", "amount": 120, "created_at": "2026-06-08T10:00:00+07:00" } ]
```
`type` ∈ `CHECK_IN, ORDER_CASHBACK, REVIEW_BONUS, USED_AT_CHECKOUT, ORDER_REFUND, VOUCHER_REDEEM`; `amount` âm khi tiêu.

#### `GET /public/coin-rewards` — Danh mục quà đổi Điểm
```json
[ { "id": 1, "coin_cost": 500, "voucher_value": 5000, "min_order_value": 50000 } ]
```

#### `POST /me/coins/redemptions` — Đổi Điểm lấy voucher
`{ "reward_id": 1 }` → `201` `{ "voucher": $Voucher, "balance": 750 }`. `422 GNE-PRO-4222` không đủ Điểm.

### 3.14 Kho voucher
UC-PRO-03 · **Màn hình:** Tài khoản › Kho Voucher (tab Tất cả / Voucher Sàn / Voucher Shop / Freeship; "Xem lịch sử voucher" với tab Hết hiệu lực / Đã sử dụng; nhập mã để lưu; xóa; modal chi tiết)

#### `GET /me/vouchers?state=AVAILABLE&type=SHOP&page&size=20`
- `state` ∈ `AVAILABLE` (gồm cả `UPCOMING`), `USED`, `EXPIRED`.
- `200` `$Voucher[]` + `summary.counts: { "ALL": 13, "PLATFORM": 7, "SHOP": 5, "FREESHIP": 2 }`.
- Modal chi tiết dùng luôn dữ liệu trong danh sách (không có endpoint chi tiết riêng).

#### `POST /me/vouchers` — Lưu voucher
Từ nút "Lưu" (trang chủ, trang shop, chiến dịch) hoặc ô nhập mã:
```json
{ "code": "GENIE30K" }
```
hoặc `{ "voucher_id": 1 }`. `201` `$Voucher`.

| Lỗi | Khi nào |
|---|---|
| `404 GNE-VCH-4041` | "Mã voucher không tồn tại hoặc đã hết hạn." |
| `409 GNE-VCH-4091` | "Voucher đã được lưu trước đó" |

#### `DELETE /me/vouchers/{voucher_id}` — Xóa khỏi kho · `204`

---

## 4. Tải tệp — Uploads

### 4.1 `POST /uploads/presigned-urls`
**Dùng ở:** ảnh đại diện, ảnh/video/giấy tờ sản phẩm, ảnh phân loại, ảnh trong mô tả, logo/ảnh bìa shop, ảnh/video đánh giá, bằng chứng trả hàng, ảnh chat, hồ sơ KYC, banner, ảnh thông báo sàn.

```json
{ "purpose": "PRODUCT_IMAGE", "file_name": "tai-nghe.webp", "content_type": "image/webp", "content_length": 482113 }
```
`201`:
```json
{ "upload_url": "https://s3.genie.vn/product-media/tmp/...?X-Amz-Signature=...", "method": "PUT", "headers": { "Content-Type": "image/webp" }, "object_key": "tmp/product-media/vendor/15/2f1c.webp", "expires_at": "2026-06-08T14:37:00+07:00" }
```

| `purpose` | Bucket | MIME | Tối đa | Ai được xin |
|---|---|---|---|---|
| `AVATAR` | `avatar-assets` | jpeg, png | 1 MB | mọi người dùng |
| `SHOP_LOGO` / `SHOP_COVER` | `avatar-assets` | jpeg, png | 1 MB / 2 MB | VENDOR |
| `PRODUCT_IMAGE`, `VARIANT_IMAGE`, `DESCRIPTION_IMAGE` | `product-media` | jpeg, png, webp | 5 MB | VENDOR |
| `PRODUCT_VIDEO` | `product-media` | mp4 | 30 MB | VENDOR |
| `PRODUCT_DOCUMENT` | `product-docs` (riêng tư) | pdf, doc(x), xls(x) — **không nhận ảnh** | 10 MB | VENDOR |
| `REVIEW_IMAGE` / `REVIEW_VIDEO` | `review-media` | jpeg, png / mp4 | 5 MB / 30 MB | CUSTOMER |
| `REFUND_EVIDENCE` | `refund-evidence` (riêng tư) | jpeg, png, mp4 | 10 MB | CUSTOMER, VENDOR |
| `CHAT_IMAGE` | `chat-media` (riêng tư) | image/* | 5 MB | CUSTOMER, VENDOR |
| `KYC_DOCUMENT` | `kyc-dossiers` (mã hóa) | pdf, jpeg, png | 10 MB, ảnh ≥ 20 KB | VENDOR |
| `BANNER_IMAGE`, `NOTIFICATION_IMAGE` | `content-assets` | jpeg, png, webp | 5 MB | STAFF (`MANAGE_VOUCHER` / `MANAGE_PLATFORM_NOTIFICATION`) |

Sai MIME/kích thước → `422 GNE-VAL-4220` ngay khi xin URL; server kiểm tra lại khi nhận `object_key` (`statObject`). URL hết hạn sau 15 phút (KYC 10 phút) → `GNE-MINIO-5003` khi PUT, xin URL mới. URL được ký theo tên miền công khai `PUBLIC_S3_URL` (`https://s3.genie.vn`, Nginx → Silo).

**Hiện thực (2026-11-16):** đã có `purpose = AVATAR` (bucket `avatar-assets`, `image/jpeg`, `image/png`, 1 MB); khóa tạm thật dạng `tmp/user/{userId}/{uuid}.{ext}` → khóa chính thức `user/{userId}/{uuid}.{ext}`. Các `purpose` khác do từng phân hệ thêm vào `UploadPurpose` khi làm. Kho tệp là **Silo** (`pgsty/silo`, fork cộng đồng của MinIO, cùng API S3 — ADR 0009); tên mã lỗi `GNE-MINIO-5003` giữ nguyên để không đổi hợp đồng.

---

## 5. Danh mục, sản phẩm, shop, trang chủ — `/public`

### 5.1 `GET /public/categories` — Cây danh mục
UC-CAT-01 · **Màn hình:** hàng danh mục dưới header, bộ lọc "Theo danh mục", chip danh mục con ở trang danh mục, ô chọn ngành hàng (Mở shop), bộ chọn danh mục 3 cấp ở form sản phẩm (Seller)

```json
[
  {
    "id": 1, "name": "Điện tử", "slug": "dien-tu", "icon": "zap",
    "children": [
      { "id": 11, "name": "Máy tính & Laptop", "slug": "may-tinh-laptop", "icon": "laptop",
        "children": [ { "id": 111, "name": "Bàn phím", "slug": "ban-phim", "icon": null, "children": [] } ] }
    ]
  }
]
```
Chỉ danh mục đang hiển thị, sắp theo `display_order`. Cache 1 giờ.

### 5.2 `GET /public/categories/{slug}` — Đầu trang danh mục
**Màn hình:** trang danh mục (tiêu đề, mô tả, tổng số sản phẩm, chip danh mục con, breadcrumb)

```json
{
  "id": 1, "name": "Điện tử", "slug": "dien-tu", "icon": "zap",
  "description": "Smartphone, laptop, tai nghe, camera và phụ kiện công nghệ chính hãng.",
  "breadcrumb": [ { "id": 1, "name": "Điện tử", "slug": "dien-tu" } ],
  "children": [ { "id": 11, "name": "Máy tính & Laptop", "slug": "may-tinh-laptop" } ],
  "product_count": 1284
}
```
`404 GNE-CAT-4041` nếu slug không có / đã ẩn.

### 5.3 `GET /public/products` — Tìm kiếm & lọc sản phẩm
UC-CAT-02, UC-CAT-05, UC-CAT-16 · **Màn hình:** Kết quả tìm kiếm (toàn sàn / trong shop), lưới trang danh mục, lưới & tab trang Shop (Tất cả / Mới nhất / Bán chạy / theo danh mục), "Có thể bạn quan tâm" khi không có kết quả, sản phẩm của trang chiến dịch, ô chọn sản phẩm làm đích banner (Admin)

| Query | Kiểu | Mô tả |
|---|---|---|
| `q` | string | Từ khóa (tên, thương hiệu, mô tả — `tsvector` + `unaccent`) |
| `shop_id` | int64 | Tìm trong shop |
| `category_id` | int64 | Gồm cả danh mục con |
| `brand_ids` | int64[] | `brand_ids=3,7` |
| `province_ids` | int[] | "Nơi bán" (tỉnh kho lấy hàng của shop) |
| `price_min`, `price_max` | int | VND |
| `rating_min` | int | 3, 4, 5 |
| `is_mall` | bool | "Shop Chính Hãng" |
| `free_shipping` | bool | "Miễn phí vận chuyển" |
| `has_shop_voucher` | bool | "Shop có voucher" |
| `attr.{code}` | string[] | Lọc thuộc tính có `is_filterable` — chỉ khi đã chọn đúng 1 danh mục, vd. `attr.warranty=12 tháng` |
| `discount_min` | int | % giảm tối thiểu (trang chiến dịch cấu hình ở frontend) |
| `sort` | enum | `RELEVANCE` (mặc định khi có `q`), `NEWEST`, `BEST_SELLING`, `PRICE_ASC`, `PRICE_DESC` |
| `include_facets` | bool | Trả thêm khối `facets` cho sidebar bộ lọc |
| `page`, `size` | int | Mặc định `size=20` |

`200` → `$ProductCard[]` + `pagination` (dùng `total_items` cho "**12** sản phẩm" / "Xem 12 sản phẩm"), và khi `include_facets=true`:
```json
"summary": {
  "facets": {
    "brands": [ { "id": 3, "name": "Sony", "count": 2 } ],
    "locations": [ { "province_id": 202, "name": "TP. HCM", "count": 9 } ],
    "attribute": { "code": "warranty", "name": "Bảo hành", "values": [ { "value": "12 tháng", "count": 4 } ] }
  }
}
```
`facets.attribute` = `null` khi chưa chọn đúng một danh mục.

### 5.4 `GET /public/search/suggestions?q=tai` — Gợi ý khi gõ
UC-CAT-04 · **Màn hình:** dropdown ô tìm kiếm header (từ 2 ký tự, debounce 250 ms)

```json
{
  "categories": [ { "id": 13, "name": "Thiết bị âm thanh", "slug": "thiet-bi-am-thanh", "icon": "headphones" } ],
  "shops": [ { "id": 15, "name": "TechZone Official" } ],
  "products": [ { "id": 1, "name": "Tai nghe Bluetooth chống ồn ANC Pro 5", "image_url": "..." } ]
}
```
Tối đa 2 danh mục, 2 shop, 5 sản phẩm.

### 5.4b `GET /public/search/trending?limit=5` — Từ khóa xu hướng
UC-CAT-04 · **Màn hình:** dropdown ô tìm kiếm khi ô còn trống (dòng có icon xu hướng + nhãn "Xu hướng")
```json
[ { "keyword": "tai nghe chống ồn" }, { "keyword": "serum vitamin c" } ]
```
Top từ khóa 24 giờ qua từ `search_keyword_stats` (đã lọc từ cấm). Cache 10 phút. Bấm một dòng → `GET /public/products?q=…`.

### 5.5 `GET /public/recommendations?page&size` — Gợi ý cho bạn / Gợi ý hôm nay
UC-CAT-07 · **Màn hình:** Trang chủ "Gợi ý cho bạn" (36 thẻ, `size=36`) và kệ 6 sản phẩm ở hero (dùng lại 6 thẻ đầu), trang "Gợi ý hôm nay" (30 thẻ/trang, "Trang 2/8", "240 sản phẩm")

`200` → `$ProductCard[]` + `pagination`. Có token → ưu tiên danh mục khách xem gần đây; không có → bán chạy + mới.

### 5.6 `GET /public/products/{id}` — Chi tiết sản phẩm
UC-CAT-06 · **Màn hình:** trang sản phẩm (gallery + video, huy hiệu, giá, Flash Sale, đặt trước, shop nghỉ, chọn phân loại + tồn kho, khung shop, "Chi tiết sản phẩm", "Mô tả sản phẩm")

```json
{
  "id": 1,
  "slug": "tai-nghe-bluetooth-chong-on-anc-pro-5",
  "name": "Tai nghe Bluetooth chống ồn ANC Pro 5 — Pin 40 giờ",
  "category_path": [ { "id": 1, "name": "Điện tử", "slug": "dien-tu" }, { "id": 13, "name": "Thiết bị âm thanh", "slug": "thiet-bi-am-thanh" } ],
  "brand": { "id": 3, "name": "Sony", "is_verified": true },
  "images": [ "https://cdn.genie.vn/.../1.webp", "https://cdn.genie.vn/.../2.webp" ],
  "video_url": null,
  "price": 389000,
  "original_price": 799000,
  "discount_percent": 51,
  "rating_avg": 4.8,
  "rating_count": 2100,
  "sold_count": 2100,
  "is_pre_order": false,
  "pre_order_days": null,
  "flash_sale": { "flash_price": 389000, "sold_count": 45, "total_count": 50, "ends_at": "2026-06-08T23:59:59+07:00", "per_user_limit": 1 },
  "option_groups": [
    { "name": "Màu sắc", "values": [ { "value": "Đen", "image_url": "..." }, { "value": "Trắng", "image_url": null } ] },
    { "name": "Phiên bản", "values": [ { "value": "Tiêu chuẩn", "image_url": null }, { "value": "Bản Pro", "image_url": null } ] }
  ],
  "variants": [
    { "id": 101, "options": ["Đen", "Tiêu chuẩn"], "price": 389000, "stock": 38 },
    { "id": 102, "options": ["Đen", "Bản Pro"], "price": 539000, "stock": 20 }
  ],
  "specs": [ { "name": "Bảo hành", "value": "12 tháng chính hãng" }, { "name": "Kết nối", "value": "Bluetooth 5.3, USB-C" } ],
  "condition": "NEW",
  "origin": "Trung Quốc",
  "ships_from": "TP. Hồ Chí Minh",
  "description_html": "<p>…</p>",
  "shop": {
    "id": 15, "name": "TechZone Official", "logo_url": null, "is_mall": true, "is_favorite": false,
    "rating_avg": 4.9, "follower_count": 128000, "province_name": "TP. HCM", "joined_year": 2021,
    "is_online": true, "vacation": null
  }
}
```
- `stock` = `stock_count − reserved_count` (khả dụng). Nút phân loại `stock = 0` bị khóa; ≤ 5 hiện "Sắp hết".
- `shop.vacation` = `{ "until": "2026-07-15", "message": "…" }` → khóa nút mua, hiện băng "Shop đang tạm nghỉ".
- `shop.is_favorite` = huy hiệu "Shop Yêu Thích".
- Bảng "Chi tiết sản phẩm" ghép từ `category_path`, `brand`, `shop`, `specs`, `condition`, `origin`, `ships_from`.
- **Đã thích / đang theo dõi**: so với `GET /me/wishlist/product-ids`, `GET /me/following/shop-ids`.
- Chia sẻ: client tự dựng deeplink `https://genie.vn/p/{slug}-{id}?v={variant_id}&src=share` (trang SSR có Open Graph — UC-CAT-18), không cần API.
- `404 GNE-CAT-4042` khi sản phẩm không còn bán → màn "Sản phẩm không còn bán" + gọi 5.9.

### 5.7 `POST /public/products/{id}/views` — Ghi lượt xem
**Màn hình:** mở trang sản phẩm (nguồn dữ liệu cho "Lượt truy cập", "Nguồn truy cập" ở Phân tích của shop)

```json
{ "source": "SEARCH" }
```
`source` ∈ `SEARCH, HOME_FEED, PROMOTION, SHOP_PAGE, EXTERNAL_LINK` (`src=share` → `EXTERNAL_LINK`). `204`. Đếm trong Redis, ghi gộp 5 phút.

### 5.8 `GET /public/products/{id}/reviews` — Đánh giá sản phẩm
UC-SOC-03 · **Màn hình:** khối "Đánh giá sản phẩm" (điểm TB, chip lọc 5 Sao / 4 Sao / 3 Sao / Có hình ảnh / Đã mua hàng kèm số đếm)

Query: `rating` (1–5), `has_media` (bool), `cursor`, `limit=10`.
```json
[
  {
    "id": 5512,
    "reviewer": { "display_name": "L***g", "avatar_url": null },
    "is_anonymous": true,
    "rating": 5,
    "variant_name": "Đen / Tiêu chuẩn",
    "content": "Chống ồn tốt, đeo êm tai. Đáng tiền!",
    "media": [ { "type": "IMAGE", "url": "..." } ],
    "created_at": "2026-05-15T20:11:00+07:00",
    "helpful_count": 21,
    "is_helpful_by_me": false,
    "shop_reply": { "content": "Cảm ơn bạn đã tin tưởng TechZone!", "replied_at": "2026-05-16T09:00:00+07:00" }
  }
]
```
`summary`: `{ "rating_avg": 4.8, "rating_count": 2100, "counts": { "ALL": 2100, "5": 1600, "4": 300, "3": 120, "2": 50, "1": 30, "WITH_MEDIA": 640 } }` — chip "Đã mua hàng" dùng `ALL` (mọi đánh giá đều từ đơn đã mua). `is_helpful_by_me` chỉ có khi gửi token. Đánh giá bị ẩn không trả.

### 5.9 `GET /public/products/{id}/similar?limit=6` — Sản phẩm tương tự
UC-CAT-07 · `$ProductCard[]` (cùng danh mục lá, loại sản phẩm hiện tại).

### 5.10 `GET /public/products/{id}/variants` — Phân loại để đổi trong giỏ
**Màn hình:** modal "Chọn phân loại" ở Giỏ hàng

```json
{ "option_groups": [ { "name": "Màu sắc", "values": ["Đen", "Trắng"] } ], "variants": [ { "id": 101, "options": ["Đen"], "price": 389000, "stock": 38, "image_url": null } ], "image_url": "..." }
```
Nhẹ hơn 5.6 (không mô tả, không shop).

### 5.11 `GET /public/shops/{id}` — Trang Shop
UC-CAT-16 · **Màn hình:** ảnh bìa + 4 chỉ số (Đánh giá, Người theo dõi, Sản phẩm, Phản hồi chat), thẻ nhận diện, băng tạm nghỉ, tab danh mục của shop

```json
{
  "id": 15,
  "name": "TechZone Official",
  "logo_url": null,
  "cover_url": "https://cdn.genie.vn/avatar-assets/shop/15/cover.webp",
  "description": "Thiết bị công nghệ chính hãng — bảo hành 12 tháng.",
  "is_mall": true,
  "is_favorite": false,
  "rating_avg": 4.9,
  "follower_count": 128000,
  "product_count": 9,
  "chat_response_rate": 98.0,
  "avg_response_minutes": 4,
  "joined_at": "2021-03-02",
  "is_online": true,
  "vacation": null,
  "categories": [ { "id": 1, "name": "Điện tử" } ]
}
```
Lưới sản phẩm: `GET /public/products?shop_id=15&sort=…&category_id=…`. Voucher của shop: 5.13. Tìm trong shop: 5.3 với `shop_id`.

### 5.12 `GET /public/shops?featured=true&limit=6` — Shop đã xác minh (trang chủ)
```json
[ { "id": 15, "name": "TechZone Official", "rating_avg": 4.9, "follower_count": 128000, "province_name": "TP. HCM" } ]
```

### 5.13 `GET /public/vouchers` — Voucher công khai để "Lưu"
UC-PRO-03 · **Màn hình:** 4 vé "Lưu voucher" ở khu Flash Sale trang chủ, hàng voucher ở trang Shop, voucher của trang chiến dịch

| Query | Mô tả |
|---|---|
| `shop_id` | Voucher đang chạy của shop |
| `code` | Một mã cụ thể (trang chiến dịch, vd. `SALE1212`) |
| `featured=true&limit=4` | Voucher sàn / freeship nổi bật (theo `priority`) cho trang chủ |

`200` → `$Voucher[]` (gồm cả `UPCOMING` để lưu trước). Có token → kèm `is_saved` (nút "Lưu" thành dấu ✓).

### 5.14 `GET /public/banners?positions=…&category_id=…` — Banner
UC-ADM-12 · **Màn hình:** băng chuyền đầu trang chủ, 2 ô cạnh băng chuyền, banner giữa trang, banner khu Flash Sale, popup xếp chồng (1 lần/phiên), banner đầu trang danh mục

`positions` ∈ `HOME_TOP_CAROUSEL, HOME_TOP_SIDE, HOME_MIDDLE, FLASH_SALE_TOP, POPUP, CATEGORY_TOP` (nhiều giá trị cách bằng dấu phẩy). `category_id` bắt buộc khi có `CATEGORY_TOP`.

```json
{
  "HOME_TOP_CAROUSEL": [ { "id": 101, "title": "Nhận hàng rồi, tiền mới đến shop", "image_url": "...", "mobile_image_url": null, "target_url": "/goi-y-hom-nay" } ],
  "HOME_TOP_SIDE": [],
  "POPUP": []
}
```
Chỉ banner đang `LIVE` (bật, trong khung giờ), sắp theo `sort_order`. Cache 60 giây.

### 5.15 `POST /public/banner-events` — Ghi lượt hiển thị / bấm (gộp lô)
```json
{ "events": [ { "banner_id": 101, "type": "IMPRESSION" }, { "banner_id": 108, "type": "CLICK" } ] }
```
`type` ∈ `IMPRESSION, CLICK`. `204`. Client gom 5 giây / khi rời trang (`navigator.sendBeacon` — server nhận cả `text/plain`). Tối đa **50 sự kiện / request** (vượt → `422 GNE-VAL-4220`), **60 request / phút / IP** (`429 GNE-RAT-4290`); sự kiện của banner không còn hiển thị bị bỏ qua (vẫn `204`). Số liệu cộng vào `impressions` / `clicks` sau tối đa 5 phút.

### 5.16 `GET /public/flash-sales/current?limit=6` — Flash Sale đang chạy
UC-PRO-06 · **Màn hình:** khối Flash Sale trang chủ (đồng hồ "Kết thúc sau", lưới sản phẩm có thanh suất)

```json
{ "id": 2102, "name": "Khung giờ vàng 20h", "ends_at": "2026-06-20T22:00:00+07:00", "items": [ "$ProductCard (flash_sale khác null)" ] }
```
Không có chiến dịch `ACTIVE` → `data: null` (ẩn khối).

### 5.17 Trang tĩnh
Các trang chính sách (Trung tâm trợ giúp, Điều khoản, Quy chế sàn, Bảng hoa hồng…) là **nội dung tĩnh ở frontend** — không có API (TLCN UC-ADM-12 bước 8, Document_Vi 1.6). Trang chiến dịch `/campaign/{slug}` ở giao diện đã dựng đọc dữ liệu qua 5.18 (thay cho cấu hình tĩnh).

### 5.18 Endpoint công khai bổ sung theo giao diện `web/` *(v1.4 — `docs/web-backend-handoff.md` mục 2)*
Giao diện đã gọi các endpoint dưới đây ở nhánh gọi thật; spec 1.3 chưa có. Tất cả không cần đăng nhập, cache được (mục 0.12).

| Endpoint | Dùng ở | Phản hồi `200` |
|---|---|---|
| `GET /public/campaigns/{slug}` | Trang chiến dịch `/campaign/{slug}` (UC-ADM-12, UC-PRO-02/05) | `{ "slug", "title", "subtitle", "banner": $Media, "starts_at", "ends_at", "voucher_codes": ["SALE1212"], "product_query": { "discount_min": 30, "category_id": null } }` — lưới sản phẩm vẫn lấy bằng `GET /public/products?…` theo `product_query`, voucher bằng `GET /public/vouchers?code=` |
| `GET /public/campaigns/slugs` | `sitemap.xml` | `["sieu-sale-12-12", "…"]` |
| `GET /public/categories/{category_id}/attribute-filters` | Bộ lọc thuộc tính theo ngành ở trang danh mục / tìm kiếm (UC-CAT-05) | `[ { "code": "ram", "name": "RAM", "type": "ENUM", "options": ["8GB", "16GB"] } ]` — chỉ thuộc tính `is_filterable` của ngành (bảng `category_attributes`). Có thể trả gộp trong `include_facets=true` của 5.3 |
| `GET /public/brands/verified` | Dấu tích "thương hiệu chính hãng" ở bộ lọc Thương hiệu | `[ { "id": 3, "name": "Logitech", "logo_url": "…" } ]` — có thể thay bằng cờ `verified` trong facet thương hiệu của 5.3 |
| `GET /public/variants?ids=1801,5012` | Giỏ của khách vãng lai (chỉ lưu `variant_id` + số lượng ở trình duyệt) cần dòng hiển thị | `[ { "variant_id", "product_id", "product_slug", "product_name", "variant_name", "image_url", "unit_price", "original_price", "stock", "is_available", "unavailable_reason", "shop": $ShopMini } ]` — tối đa 50 id |
| `GET /public/shops/shipping-policies?ids=15,22` | Trang giỏ hiển thị mức miễn phí vận chuyển / phí dự kiến theo shop trước khi vào Thanh toán | `[ { "shop_id": 15, "free_ship_threshold": 299000, "carriers": ["GHN"], "estimated_fee_from": 15000 } ]` — số chính xác vẫn tính ở `POST /me/checkout/preview` (6.8) |

---

## 6. Giỏ hàng và thanh toán

### 6.1 `GET /me/cart` — Giỏ hàng nhiều shop
UC-ORD-01 · **Màn hình:** Giỏ hàng (nhóm theo shop, chọn từng món, cảnh báo "Giá đã tăng/giảm so với lúc thêm", "Phân loại: …", ô số lượng giới hạn tồn kho)

```json
{
  "shops": [
    {
      "shop": { "id": 15, "name": "TechZone Official", "logo_url": null, "is_mall": true },
      "items": [
        {
          "id": 7001,
          "product_id": 18,
          "product_slug": "ban-phim-co-rgb-switch-blue",
          "variant_id": 1801,
          "product_name": "Bàn phím cơ RGB switch blue gaming chống ồn",
          "image_url": "...",
          "variant_name": "Blue switch",
          "unit_price": 459000,
          "original_price": 890000,
          "price_at_add": 429000,
          "quantity": 1,
          "stock": 25,
          "is_available": true,
          "unavailable_reason": null
        }
      ]
    }
  ],
  "item_count": 11
}
```
`unavailable_reason` ∈ `OUT_OF_STOCK, SHOP_ON_VACATION, PRODUCT_UNAVAILABLE, FLASH_SALE_SOLD_OUT`. `product_slug` (thêm v1.4) để dựng liên kết chuẩn `/p/{slug}-{id}` mà không gọi thêm chi tiết sản phẩm. Giá, giảm giá, phí ship tính ở 6.8 (cùng một engine với trang Thanh toán).

### 6.2 `GET /me/cart/items?limit=5&sort=-added_at` — Mini cart ở header
`200` → `[{ "id", "product_id", "product_name", "image_url", "variant_name", "unit_price" }]` + `summary.total` cho dòng "Còn 6 sản phẩm trong giỏ".

### 6.3 `POST /me/cart/items` — Thêm vào giỏ / Mua ngay
UC-ORD-01 · **Màn hình:** trang sản phẩm (Thêm vào giỏ, Mua ngay, Đặt trước), Sản phẩm yêu thích (Thêm vào giỏ)

```json
{ "variant_id": 101, "quantity": 1 }
```
`201` → `{ "item": <dòng giỏ như 6.1>, "cart_item_count": 12 }` (cùng biến thể đã có → cộng dồn số lượng, trả `200`). `cart_item_count` để cập nhật badge; cấu trúc cùng kiểu với phản hồi 6.4 (`{ item, merged_from_id }`).

| Lỗi | Khi nào |
|---|---|
| `409 GNE-INV-4094` | Vượt tồn kho, `details.stock` |
| `409 GNE-INV-4091` | Hết suất Flash Sale (tối đa 1 suất / người / chiến dịch) |
| `409 GNE-ORD-4093` | Shop đang tạm nghỉ |
| `409 GNE-ORD-4094` | Sản phẩm của chính shop mình |

### 6.4 `PATCH /me/cart/items/{id}` — Đổi số lượng / đổi phân loại
```json
{ "quantity": 2 }
```
hoặc `{ "variant_id": 1802 }` (modal "Chọn phân loại"). Đổi sang biến thể đã có trong giỏ → hai dòng gộp làm một, phản hồi `{ "item": {...}, "merged_from_id": 7001 }`. `409 GNE-INV-4094` vượt tồn ("Sản phẩm này chỉ còn 25 sản phẩm trong kho").

### 6.5 `DELETE /me/cart/items?ids=7001,7002` — Xóa một hoặc nhiều món
`204`. (Đưa số lượng về 0 → client hỏi xác nhận rồi gọi endpoint này.)

### 6.6 `POST /me/cart/items/batch` — Thêm nhiều món (Mua lại / Mua lại tất cả)
**Màn hình:** "Mua lại" ở thẻ đơn & trang theo dõi đơn, "Mua lại tất cả" ở nhóm đơn đã hủy

```json
{ "items": [ { "variant_id": 101, "quantity": 1 }, { "variant_id": 1801, "quantity": 1 } ] }
```
`200`:
```json
{ "added": 1, "skipped": [ { "variant_id": 1801, "reason": "SHOP_ON_VACATION" } ], "cart_item_count": 12 }
```
Toast "Đã thêm sản phẩm còn bán, bỏ qua sản phẩm của shop đang tạm nghỉ" dựa vào `skipped`.

### 6.7 `POST /me/cart/merge` — Gộp giỏ khách vãng lai sau đăng nhập
**Màn hình:** toast "Đã gộp giỏ hàng tạm vào tài khoản của bạn" (giỏ vãng lai lưu `localStorage` 30 ngày)

```json
{ "items": [ { "variant_id": 101, "quantity": 1 } ] }
```
`200` `{ "merged": 1, "cart_item_count": 12 }`. Cùng SKU cộng dồn, chặn ở tồn kho.

### 6.8 `POST /me/checkout/preview` — Tính giá (Giỏ hàng & Thanh toán)
UC-ORD-04, UC-ORD-06, UC-PRO-04, UC-PRO-10 · **Màn hình:** thanh tổng của Giỏ hàng (tạm tính từng shop, Voucher Genie, "Dùng 1.250 Điểm", "Genie giữ … đến khi bạn nhận hàng", "2 đơn · gồm phí ship …"), toàn bộ trang Thanh toán (đơn con theo shop, phí ship, voucher shop / sàn phân bổ / freeship, "Có bộ mã tiết kiệm thêm …", Điểm, chi tiết thanh toán, đồng hồ giữ hàng)

```json
{
  "cart_item_ids": [7001, 7002, 7003],
  "address_id": 1,
  "vouchers": {
    "platform_voucher_id": 1,
    "freeship_voucher_id": 2,
    "shop_vouchers": [ { "shop_id": 15, "voucher_id": 3 } ]
  },
  "use_coins": true
}
```
- `address_id` bỏ trống → dùng địa chỉ mặc định (Giỏ hàng chưa chọn địa chỉ).
- `vouchers` bỏ trống → server **tự áp bộ mã giảm nhiều nhất** trong kho voucher của khách (giống trạng thái mặc định của giao diện).

`200`:
```json
{
  "address": "$Address",
  "shops": [
    {
      "shop": "$ShopMini",
      "items": [
        { "cart_item_id": 7001, "variant_id": 101, "product_name": "...", "image_url": "...", "variant_name": "Đen", "unit_price": 389000, "quantity": 1, "platform_discount": 22000, "is_available": true, "unavailable_reason": null }
      ],
      "subtotal": 848000,
      "shipping_fee": 25000,
      "is_free_shipping_by_shop": false,
      "shop_voucher": { "id": 3, "code": "TECH15", "discount": 50000 },
      "freeship_discount": 25000,
      "platform_discount": 38000,
      "total": 760000
    }
  ],
  "vouchers_applied": {
    "platform": { "id": 1, "code": "GENIE30K", "discount": 30000 },
    "freeship": { "id": 2, "code": "FREESHIP30", "discount": 30000 }
  },
  "best_selection": { "is_current_best": false, "extra_saving": 20000, "vouchers": { "platform_voucher_id": 5, "freeship_voucher_id": 2, "shop_vouchers": [ { "shop_id": 15, "voucher_id": 3 } ] } },
  "coins": { "balance": 1250, "max_usable": 312000, "applied": 1250 },
  "summary": { "subtotal": 1261000, "shipping_total": 41000, "platform_discount": 30000, "freeship_discount": 30000, "shop_discount": 50000, "coin_discount": 1250, "grand_total": 1190750, "order_count": 2, "item_count": 3 }
}
```
- Thứ tự tính: Voucher Shop → Voucher Sàn (phân bổ theo dòng, phần dư dồn dòng cuối) → Freeship → Điểm (≤ 50% tiền hàng còn lại, 1 Điểm = 1₫). Tổng giảm tầng 1+2 ≤ 85% tiền hàng (LLD 9.3).
- `is_free_shipping_by_shop = true` → "Shop miễn phí vận chuyển" (đạt ngưỡng freeship của shop).
- Voucher đã chọn mà không còn hợp lệ → bị bỏ và có `warnings: [{ "voucher_id": 1, "code": "GNE-VCH-4221", "message": "..." }]` trong phản hồi (không trả lỗi, để giỏ vẫn hiển thị).
- Không cung cấp mã đơn trước khi đặt (xem mục 17).

### 6.9 `POST /me/checkout/applicable-vouchers` — Danh sách cho Voucher Picker
UC-PRO-03, UC-PRO-04 · **Màn hình:** modal "Chọn Voucher Genie" (nhóm Mã Giảm Giá / Miễn Phí Vận Chuyển) và "Chọn Voucher của Shop" — voucher không dùng được vẫn hiện, mờ, kèm lý do; ô "Nhập mã voucher"

```json
{ "cart_item_ids": [7001, 7002], "scope": "PLATFORM", "shop_id": null, "current_selection": { "platform_voucher_id": 1, "freeship_voucher_id": null, "shop_vouchers": [] }, "code": null }
```
`scope` ∈ `PLATFORM` (sàn + freeship), `SHOP` (cần `shop_id`). `code` khác `null` → chỉ trả voucher có mã đó (kể cả chưa lưu) để "Áp dụng".

`200`:
```json
[ { "voucher": "$Voucher", "is_eligible": false, "ineligible_reason": "MIN_ORDER_NOT_MET", "ineligible_message": "Chưa đạt giá trị tối thiểu 1.000.000₫", "estimated_discount": 0 } ]
```
`ineligible_reason` ∈ `NO_MATCHING_ITEMS, MIN_ORDER_NOT_MET, NOT_STARTED, OUT_OF_QUOTA, BUDGET_EXHAUSTED, STACKING_LIMIT`. Nhập mã không tồn tại → `404 GNE-VCH-4041`.

### 6.10 `POST /me/checkout-sessions` 🔁 — Đặt hàng
UC-ORD-07, UC-PAY-01 · **Màn hình:** nút "Đặt hàng & thanh toán VNPAY" / "Đặt hàng & lấy mã QR" / "Đặt hàng" (COD)

```json
{
  "cart_item_ids": [7001, 7002, 7003],
  "address_id": 1,
  "vouchers": { "platform_voucher_id": 1, "freeship_voucher_id": 2, "shop_vouchers": [ { "shop_id": 15, "voucher_id": 3 } ] },
  "use_coins": true,
  "notes": [ { "shop_id": 15, "note": "Giao giờ hành chính giúp mình nhé." } ],
  "payment_method": "VNPAY",
  "return_url": "https://genie.vn/checkout/result"
}
```
`payment_method` ∈ `VNPAY, VIETQR, COD`. Lời nhắn ≤ 500 ký tự. Server tính lại giá (không tin giá client), giữ chỗ tồn kho 15 phút, tách N đơn con theo shop, xóa các dòng đã đặt khỏi giỏ.

`201` → `$CheckoutSession` (6.11) có thêm khối `payment` theo phương thức:
```json
"payment": { "method": "VNPAY", "payment_url": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?..." }
```
```json
"payment": {
  "method": "VIETQR",
  "vietqr": { "bank_name": "MB Bank", "account_number": "000088662024", "account_name": "GENIE GIU TIEN", "amount": 1190750, "transfer_content": "TR20260608000201", "qr_payload": "00020101021238...", "expires_at": "2026-06-08T14:37:00+07:00" }
}
```
```json
"payment": { "method": "COD" }
```

| Lỗi | Khi nào | `details` |
|---|---|---|
| `409 GNE-INV-4090` | Hết hàng lúc giữ chỗ | `{ "out_of_stock_cart_item_ids": [7001] }` → client gỡ dòng ("Sản phẩm vừa hết hàng — đã gỡ khỏi đơn") và gọi lại 6.8 |
| `409 GNE-INV-4091` | Hết suất Flash Sale | |
| `409 GNE-ORD-4093` | Shop tạm nghỉ | `{ "shop_ids": [19] }` |
| `422 GNE-VCH-4221` | Voucher hết hiệu lực giữa chừng | `{ "voucher_id": 1 }` |
| `503 GNE-SYS-5030` | Không kiểm được Idempotency-Key | Thử lại với **cùng** key |

### 6.11 `GET /me/checkout-sessions/{id}` — Kết quả đặt hàng / trạng thái thanh toán
UC-ORD-07, UC-PAY-03 · **Màn hình:** "Thanh toán thành công / Đặt hàng thành công" (mã giao dịch, ngày đặt, từng đơn con + "Theo dõi", tổng tiền), "Thanh toán thất bại" (mã đơn, "Tồn kho vẫn được giữ thêm 13:54"), ô VietQR tự chuyển trang khi SePay báo đã nhận (poll 3 giây hoặc WebSocket)

`$CheckoutSession`:
```json
{
  "id": 8821,
  "code": "CS-8F21A",
  "status": "COMPLETED",
  "payment_method": "VNPAY",
  "created_at": "2026-06-08T14:22:00+07:00",
  "reservation_expires_at": "2026-06-08T14:37:00+07:00",
  "orders": [ { "id": 201, "code": "TR-20260608-000201", "shop": { "id": 15, "name": "TechZone Official" }, "grand_total": 760000 } ],
  "summary": { "subtotal": 1261000, "shipping_total": 41000, "discount_total": 110000, "coin_discount": 1250, "grand_total": 1190750 }
}
```
`status` ∈ `INITIATED, PAYMENT_PENDING, COMPLETED, FAILED, EXPIRED`.

### 6.12 `POST /me/checkout-sessions/{id}/payments` 🔁 — Thanh toán lại / đổi phương thức
UC-PAY-01 · **Màn hình:** "Thử thanh toán lại", "Đổi phương thức" (trang thất bại), "Thanh toán ngay" ở đơn / nhóm đơn chờ thanh toán (modal chọn VNPAY / VietQR / COD)

```json
{ "payment_method": "VIETQR", "return_url": "https://genie.vn/checkout/result" }
```
`200` → khối `payment` như 6.10. Chọn `COD` → các đơn chuyển sang chờ shop xác nhận. `409 GNE-INV-4093` khi giữ chỗ đã hết 15 phút.

### 6.13 `POST /me/checkout-sessions/{id}/cancel` — Hủy nhóm đơn
UC-ORD-10 · **Màn hình:** "Huỷ nhóm đơn" ở nhóm đơn chờ thanh toán

```json
{ "reason_code": "CHANGE_OF_MIND", "note": null }
```
`200` `{ "cancelled_order_ids": [29, 30] }`. Nhả tồn kho, hoàn voucher và Điểm.

---

## 7. Đơn mua, trả hàng, đánh giá (khách)

### 7.1 `GET /me/orders` — Đơn mua
UC-ORD-09 · **Màn hình:** Tài khoản › Đơn mua (tab Tất cả / Chờ thanh toán / Vận chuyển / Chờ giao hàng / Hoàn thành / Trả hàng-Hoàn tiền / Đã huỷ; thẻ đơn; nhóm đơn cùng phiên), menu "Gửi đơn hàng" trong chat

| Query | Mô tả |
|---|---|
| `status_group` | `TO_PAY, SHIPPING, TO_RECEIVE, COMPLETED, RETURN, CANCELLED` (bỏ trống = tất cả) — ánh xạ ở mục 16.3 |
| `shop_id` | Lọc đơn với một shop (menu chọn đơn trong chat) |
| `cursor`, `limit` | Mặc định 10 |

```json
[
  {
    "id": 44,
    "code": "TR-20260608-000044",
    "checkout_session_id": 8811,
    "status": "DELIVERING",
    "payment_method": "VNPAY",
    "shop": "$ShopMini",
    "items": [ "$OrderItem" ],
    "subtotal": 848000,
    "shipping_fee": 25000,
    "discount_total": 60000,
    "grand_total": 813000,
    "payment_expires_at": null,
    "is_reviewed": false,
    "allowed_actions": ["TRACK", "CHAT"]
  }
]
```
- `allowed_actions` ⊆ `PAY, CANCEL, TRACK, CHAT, CONFIRM_RECEIPT, REQUEST_RETURN, VIEW_RETURN, REVIEW, BUY_AGAIN` — nút trên thẻ đơn vẽ đúng theo mảng này.
- Đơn online `PENDING_PAYMENT` / `CANCELLED` cùng `checkout_session_id` được client gộp thành "Nhóm đơn hàng" (tổng tiền nhóm cộng từ các đơn con).
- `summary.counts` theo `status_group`.

### 7.2 `GET /me/orders/{id}` — Theo dõi đơn
UC-ORD-09 · **Màn hình:** trang theo dõi đơn (stepper 7 bước, thẻ hãng vận chuyển + mã vận đơn, ghi chú giữ tiền / ngày giải ngân, hành trình, địa chỉ, sản phẩm, tổng tiền, khối yêu cầu trả hàng, các nút)

```json
{
  "id": 41,
  "code": "TR-20260608-000041",
  "status": "DELIVERED",
  "payment_method": "VIETQR",
  "placed_at": "2026-06-06T09:10:00+07:00",
  "shop": "$ShopMini",
  "items": [ { "...$OrderItem": "...", "paid_unit_price": 148000 } ],
  "money": "$MoneySummary",
  "shipping_address": "$AddressSnapshot",
  "shipment": {
    "carrier": "GHTK",
    "tracking_code": "GHTK8841200",
    "estimated_delivery_at": "2026-06-08T00:00:00+07:00",
    "delivered_at": "2026-06-08T11:32:00+07:00",
    "events": [
      { "status": "DELIVERED", "description": "Giao hàng thành công — người nhận đã ký nhận", "location": "Q.1, TP. HCM", "occurred_at": "2026-06-08T11:32:00+07:00" }
    ]
  },
  "escrow_release_at": "2026-06-15T11:32:00+07:00",
  "return_window_closed": false,
  "refund_request": null,
  "allowed_actions": ["CONFIRM_RECEIPT", "REQUEST_RETURN", "CHAT"]
}
```
- `items[].paid_unit_price` = đơn giá sau khi trừ giảm giá đã phân bổ → modal trả hàng tính "Số tiền hoàn dự kiến" ngay trên client.
- `shipment` = `null` khi chưa bàn giao ĐVVC; `carrier` ∈ `GHN, GHTK, VIETTEL_POST, JNT_EXPRESS, GENIE_EXPRESS`; `events[]` mới nhất trước, gồm cả mốc `PAID`, `PREPARING` của sàn.
- `refund_request` khi đã có yêu cầu:
```json
{ "id": 233, "code": "RFN-0233", "status": "AWAITING_EVIDENCE", "request_type": "RETURN_AND_REFUND", "reason_code": "DAMAGED_IN_TRANSIT", "refund_amount": 459000, "shop_message": "Vui lòng quay rõ phần sản phẩm bị lỗi", "deadline_at": "2026-06-10T10:00:00+07:00", "refund_bank_masked": "Vietcombank ••4471", "resolved_at": null }
```
`deadline_at` là hạn của bước hiện tại (48 giờ shop phản hồi / 48 giờ khách bổ sung hoặc khiếu nại / 7 ngày gửi hàng hoàn).

### 7.3 `POST /me/orders/{id}/cancel` — Hủy đơn
UC-ORD-10 · **Màn hình:** modal "Xác nhận huỷ đơn hàng" + chọn lý do

```json
{ "reason_code": "CHANGE_ADDRESS", "note": null }
```
`200` `{ "status": "CANCELLED", "refund_initiated": true }` — `refund_initiated = true` khi đơn đã trả tiền online ("Tiền được hoàn về phương thức đã thanh toán"). `409 GNE-ORD-4092` từ `READY_FOR_PICKUP` trở đi.

### 7.4 `POST /me/orders/{id}/confirm-receipt` — Đã nhận hàng
UC-ORD-11 · **Màn hình:** modal "Xác nhận đã nhận hàng?" (… Genie sẽ chuyển tiền cho shop)

`200` `{ "status": "COMPLETED", "completed_at": "..." }`. `409 GNE-ORD-4095` nếu đơn chưa `DELIVERED`.

### 7.5 `POST /me/refund-requests` 🔁 — Yêu cầu trả hàng / hoàn tiền
UC-SUP-01 · **Màn hình:** modal "Yêu cầu Trả hàng / Hoàn tiền" (phương án, chọn món + số lượng, lý do, bằng chứng ≥ 1 ảnh + 1 video ≤ 10 MB, tài khoản nhận tiền hoàn cho đơn VietQR/COD)

```json
{
  "order_id": 41,
  "request_type": "RETURN_AND_REFUND",
  "reason_code": "WRONG_SIZE_COLOR",
  "items": [ { "order_item_id": 9001, "quantity": 1 } ],
  "evidence": [ { "object_key": "tmp/refund-evidence/...jpg", "media_type": "IMAGE" }, { "object_key": "tmp/refund-evidence/...mp4", "media_type": "VIDEO" } ],
  "refund_bank": { "bank_code": "VCB", "account_number": "0071000006712", "account_holder": "TRAN THI MAI" }
}
```
`refund_bank` bắt buộc khi `payment_method ≠ VNPAY` (đơn VietQR / COD hoàn bằng chuyển khoản tay); danh sách ngân hàng: `GET /public/banks` (mục 10.11). `201` → khối `refund_request` như 7.2.

| Lỗi | Khi nào |
|---|---|
| `422 GNE-SUP-4221` | Thiếu ảnh hoặc video bằng chứng |
| `409 GNE-SUP-4092` | Quá 7 ngày kể từ khi giao / đơn đã có yêu cầu đang mở |

### 7.6 `POST /me/refund-requests/{id}/evidence` — Bổ sung bằng chứng
UC-SUP-01 · **Màn hình:** "Tải thêm ảnh/video" → "Gửi bổ sung" (khi `AWAITING_EVIDENCE`)

`{ "evidence": [ { "object_key": "...", "media_type": "IMAGE" } ] }` → `200` khối `refund_request` như 7.2 (trạng thái về `REQUESTED`, đồng hồ 48 giờ của shop chạy tiếp).

### 7.7 `POST /me/refund-requests/{id}/escalate` — Khiếu nại lên Sàn
UC-SUP-03 · **Màn hình:** "Khiếu nại lên Sàn" (khi shop từ chối / yêu cầu bổ sung) → `200` `{ "status": "DISPUTED" }`.

### 7.8 `POST /me/refund-requests/{id}/accept-rejection` — Chấp nhận shop từ chối
**Màn hình:** nút "Chấp nhận" khi `SHOP_REJECTED` → `200` `{ "status": "REJECTED" }`, đóng yêu cầu, đơn về `COMPLETED`.

### 7.9 `POST /me/reviews` — Đánh giá sản phẩm
UC-SOC-03 · **Màn hình:** modal "Đánh giá sản phẩm" (từng sản phẩm trong đơn, 1–5 sao, nhận xét, ≤ 5 ảnh, 1 video ≤ 30 MB, ẩn tên; "Gửi và đánh giá sản phẩm tiếp theo")

```json
{ "order_item_id": 9001, "rating": 5, "content": "Âm thanh hay…", "image_object_keys": ["tmp/review-media/...jpg"], "video_object_key": null, "is_anonymous": false }
```
`201` `{ "id": 5513, "coins_awarded": 50 }` (+50 Điểm khi có ảnh). `409 GNE-SOC-4091` đã đánh giá dòng hàng này; `409 GNE-SOC-4092` đơn chưa `COMPLETED`.

### 7.10 Hữu ích & báo cáo đánh giá
UC-SOC-03, UC-SOC-05 · **Màn hình:** nút "Hữu ích (n)" và "Báo cáo" ở từng đánh giá (trang sản phẩm); "Báo cáo" ở Seller › Đánh giá

| Method & Path | Body | Phản hồi |
|---|---|---|
| `PUT /reviews/{id}/helpful-vote` | — | `200` `{ "helpful_count": 22 }` (idempotent) |
| `DELETE /reviews/{id}/helpful-vote` | — | `200` `{ "helpful_count": 21 }` |
| `POST /reviews/{id}/reports` | `{ "reason": "SPAM", "note": "..." }` | `201`; `409 GNE-SOC-4093` đã báo cáo |

`reason` ∈ `SPAM, OFFENSIVE, FAKE_PURCHASE`. Vai trò: CUSTOMER (đánh giá bất kỳ), VENDOR (đánh giá của shop mình).

---

## 8. Chat Khách ↔ Shop

UC-SOC-01, UC-SOC-02 · **Màn hình:** khung chat nổi Storefront (hộp thư nhiều shop, luồng tin, tin nhanh, gửi ảnh, gửi đơn, thẻ sản phẩm, trạng thái Đang gửi / Đã nhận / Đã xem); Seller › Chat

Hai nhóm đường dẫn cùng DTO: khách dùng `/me/conversations/**`, shop dùng `/seller/conversations/**` với đúng các endpoint 8.1, 8.3, 8.4, 8.5 (`GET /seller/conversations`, `GET /seller/conversations/{id}/messages`, `POST /seller/conversations/{id}/messages`, `POST /seller/conversations/{id}/read`); 8.2 (mở hội thoại) chỉ có phía khách. Gửi tin thời gian thực qua WebSocket (mục 13); REST dưới đây là đường chính để tải dữ liệu và là đường dự phòng để gửi.

### 8.1 `GET /me/conversations?cursor&limit=20` (shop: `GET /seller/conversations`)
```json
[
  {
    "id": 3301,
    "counterpart": { "type": "SHOP", "id": 15, "name": "TechZone Official", "avatar_url": null, "is_mall": true, "is_online": true, "last_active_at": "2026-06-08T14:32:00+07:00" },
    "last_message": { "type": "TEXT", "preview": "Dạ còn đủ màu đen bạn nhé!", "sent_at": "2026-06-08T14:32:00+07:00" },
    "unread_count": 1
  }
]
```
Phía shop `counterpart.type = CUSTOMER`. Ảnh / thẻ sản phẩm / thẻ đơn có `preview` = `null`, client hiện "[Hình ảnh]", "[Sản phẩm]", "[Đơn hàng]".

### 8.2 `POST /me/conversations` — Mở (hoặc lấy) hội thoại với shop
**Màn hình:** "Chat ngay" (trang sản phẩm, trang shop), "Chat" (giỏ hàng, theo dõi đơn), "Liên hệ Shop"

```json
{ "shop_id": 15 }
```
`200` (đã có) / `201` (mới):
```json
{ "id": 3301, "counterpart": "...", "shop_stats": { "chat_response_rate": 98.0, "avg_response_minutes": 4 }, "unread_count": 0 }
```
`shop_stats` cho dòng "Phản hồi 98% · trong vài phút". Thẻ sản phẩm / đơn được gửi tự động khi mở từ trang sản phẩm / đơn: client gửi tiếp một tin `PRODUCT_CARD` / `ORDER_CARD` (8.4).

### 8.3 `GET /me/conversations/{id}/messages?before=<cursor>&limit=30`
```json
[
  { "id": 99012, "sender": "THEM", "type": "TEXT", "text": "Dạ còn đủ màu đen bạn nhé!", "sent_at": "2026-06-08T14:32:00+07:00", "status": "SEEN" },
  { "id": 99010, "sender": "ME", "type": "PRODUCT_CARD", "product": { "id": 1, "name": "Tai nghe Bluetooth ANC Pro 5", "image_url": "...", "price": 389000 }, "sent_at": "...", "status": "SEEN" },
  { "id": 99011, "sender": "ME", "type": "ORDER_CARD", "order": { "id": 44, "code": "TR-20260608-000044" }, "sent_at": "...", "status": "DELIVERED" },
  { "id": 99013, "sender": "THEM", "type": "IMAGE", "image_url": "https://s3.genie.vn/chat-media/...?X-Amz-...", "sent_at": "...", "status": "SEEN" }
]
```
`sender` ∈ `ME, THEM`; `status` (chỉ ý nghĩa với tin của mình) ∈ `SENT, DELIVERED, SEEN`. Mới nhất trước; cuộn lên dùng `next_cursor`.
Mỗi tin có đủ các khóa `text`, `product`, `order`, `image_url`, `client_message_id` (khóa không dùng theo `type` là `null`, §0.5); `client_message_id` là UUID client gửi ở 8.4, trả lại để khớp bong bóng "Đang gửi…" (`null` với tin cũ / tin của bên kia).

### 8.4 `POST /me/conversations/{id}/messages` — Gửi tin
```json
{ "type": "TEXT", "text": "Sản phẩm còn hàng không?" }
```
Các dạng khác: `{ "type": "IMAGE", "image_object_key": "tmp/chat-media/..." }`, `{ "type": "PRODUCT_CARD", "product_id": 1 }`, `{ "type": "ORDER_CARD", "order_id": 44 }`. Thêm `client_message_id` (UUID) để client khớp bong bóng "Đang gửi…" khi nhận lại. `201` → tin như 8.3. Shop bật tự trả lời → server tự gửi lời chào / tin vắng mặt / tin tạm nghỉ (UC-VND-06, UC-VND-19).

| Lỗi | Khi nào |
|---|---|
| `403 GNE-SOC-4031` | Đơn / sản phẩm không thuộc hội thoại này |
| `422 GNE-VAL-4220` | Tin rỗng, ảnh > 5 MB |

### 8.5 `POST /me/conversations/{id}/read` — Đánh dấu đã xem
`204`. Đặt `unread_count = 0`, bắn sự kiện "Đã xem" cho bên kia.

---

## 9. Mở shop

UC-VND-01 · **Màn hình:** Trở thành Người bán (form Tên Shop, Họ tên chủ shop, SĐT, Email, Ngành hàng, Loại hình, đồng ý Điều khoản) → modal "Xác minh Email mở Shop" → chuyển sang Seller Center màn KYC

### 9.1 `POST /me/shop-registrations` — Gửi đăng ký
```json
{ "shop_name": "TechZone Official", "owner_name": "Đặng Quốc Huy", "phone_number": "0912345678", "email": "shop@techzone.vn", "main_category_id": 1, "business_type": "BUSINESS", "accept_terms": true }
```
`business_type` ∈ `INDIVIDUAL` (Cá nhân), `HOUSEHOLD` (Hộ kinh doanh), `BUSINESS` (Doanh nghiệp). `202` → `$OtpChallenge` (`purpose = SHOP_REGISTRATION`, gửi tới `email`).

| Lỗi | Khi nào |
|---|---|
| `409 GNE-VND-4093` | Tài khoản đã có shop |
| `409 GNE-VND-4094` | Tên shop đã được dùng |

### 9.2 `POST /me/shop-registrations/{challenge_id}/confirm`
`{ "otp": "123456" }` → `201` `{ "shop_id": 15, "kyc_status": "NOT_SUBMITTED" }`. Tài khoản được nâng lên `role = VENDOR` (vẫn mua hàng bình thường — `CUSTOMER < VENDOR`); client làm mới token (`POST /auth/sessions/refresh`) để có quyền `/seller`.

---

## 10. Seller Center — `/seller`

Vai trò `VENDOR`. Mọi truy vấn tự giới hạn trong shop của người gọi (chống IDOR — không nhận `shop_id` từ client). Cổng KYC: mục 0.9.

### 10.1 `GET /seller/me` — Khung Seller Center
**Màn hình:** sidebar (tên shop, chip trạng thái KYC, badge "Đơn hàng", "Chat"), menu tài khoản, banner KYC ở Tổng quan

```json
{
  "shop": { "id": 15, "name": "TechZone Official", "logo_url": null, "is_mall": true },
  "kyc_status": "VERIFIED",
  "badges": { "orders_to_confirm": 2, "unread_chats": 5 }
}
```
`kyc_status` ∈ `NOT_SUBMITTED, PENDING, AI_PROCESSING, MANUAL_REVIEW, VERIFIED, REJECTED, SUPPLEMENT_REQUESTED` — client gom `PENDING / AI_PROCESSING / MANUAL_REVIEW` thành "KYC đang chờ". Thông báo của shop: `GET /me/notifications`.

### 10.1b `GET /seller/search?q=TR-2026&limit=5` — Tìm nhanh
**Màn hình:** ô "Tìm đơn, sản phẩm…" trên header Seller Center (dropdown 2 nhóm; bấm đơn → tab Đơn hàng lọc theo mã, bấm sản phẩm → mở form sản phẩm)
```json
{
  "orders": [ { "id": 141, "code": "TR-20260608-000141", "customer_name": "Trần Thị Mai", "status": "CONFIRMED" } ],
  "products": [ { "id": 1, "name": "Tai nghe Bluetooth ANC Pro 5", "image_url": "...", "status": "PUBLISHED" } ]
}
```
Tìm theo mã đơn, tên khách, tên sản phẩm, SKU — chỉ trong shop của người gọi. Từ 2 ký tự, debounce 250 ms. Bỏ qua cổng KYC (trả rỗng khi chưa `VERIFIED`).

### 10.2 Tổng quan
UC-VND-08 · **Màn hình:** Tổng quan (Việc cần làm, Doanh thu 7 ngày / 30 ngày / 12 tháng, Tiền của shop, Đơn hàng gần đây, Đơn 30 ngày qua, Sản phẩm)

#### `GET /seller/dashboard`
```json
{
  "as_of": "2026-06-08",
  "work_queue": {
    "orders_to_confirm": 2,
    "orders_to_prepare": 2,
    "orders_delivery_failed": 1,
    "return_requests": { "count": 2, "nearest_deadline_at": "2026-06-09T02:00:00+07:00" },
    "unread_chats": 5
  },
  "money": { "available": 12450000, "held": 1907600, "pending_withdrawal": 5000000, "cod_pending": 1950000 },
  "orders_30d": {
    "count": 100, "previous_count": 92,
    "visits": 3120, "previous_visits": 2786,
    "outcomes": { "COMPLETED": 81, "CANCELLED_BY_CUSTOMER": 4, "REJECTED_BY_SHOP": 2, "REFUNDED": 2, "IN_PROGRESS": 11 }
  },
  "products": { "PUBLISHED": 3, "PENDING_APPROVAL": 1, "DRAFT": 2, "REJECTED": 1 }
}
```
Tỉ lệ chuyển đổi = `count / visits`, client tự chia. Bảng "Đơn hàng gần đây": `GET /seller/orders?size=5`.

#### `GET /seller/analytics/revenue?range=30d`
`range` ∈ `7d, 30d, 12m` (12 tháng = các tháng đã chốt).
```json
{ "total": 46500000, "previous_total": 38680000, "points": [ { "label": "2026-05-10", "value": 1000000 } ] }
```
`previous_total` = `null` với `12m`. Doanh thu = tiền hàng trước hoa hồng.

### 10.3 Đơn hàng của shop
UC-LOG-16, UC-LOG-01, UC-LOG-02, UC-LOG-15 · **Màn hình:** Đơn hàng (tab Tất cả / Mới / Chờ chuẩn bị / Đang giao / Đã giao / Trả hàng / Đã hủy + badge; ô tìm mã đơn/khách; sắp xếp Mới nhất/Cũ nhất; thẻ đơn có lời nhắn của khách)

#### `GET /seller/orders`
| Query | Mô tả |
|---|---|
| `tab` | `NEW, TO_PREPARE, DELIVERING, DELIVERED, CANCELLED` (bỏ trống = tất cả). Ánh xạ trạng thái ở mục 16.3. Tab "Trả hàng" dùng 10.4 |
| `q` | Mã đơn hoặc tên khách |
| `customer_id` | Đơn của một khách (menu "Gửi đơn hàng" trong chat của shop) |
| `sort` | `NEWEST` (mặc định), `OLDEST` |
| `page`, `size` | |

```json
[
  {
    "id": 138,
    "code": "TR-20260608-000138",
    "status": "DELIVERING",
    "payment_method": "VNPAY",
    "customer": { "id": 20377, "name": "Lê Hoàng Phúc", "avatar_url": null },
    "ship_to": "Hải Châu, Đà Nẵng",
    "items": [ { "product_name": "Bàn phím cơ RGB", "variant_name": "Đen/Blue", "quantity": 1, "unit_price": 459000 } ],
    "grand_total": 459000,
    "buyer_note": null,
    "reject_reason": null,
    "shipment": { "carrier": "JNT_EXPRESS", "tracking_code": "VN931042771", "status": "IN_TRANSIT", "carrier_status_text": "Đang trung chuyển" },
    "created_at": "2026-06-07T10:00:00+07:00",
    "allowed_actions": ["PRINT_LABEL"]
  }
]
```
- `allowed_actions` ⊆ `CONFIRM, REJECT, PREPARE, HAND_OVER, PRINT_LABEL`.
- Badge trạng thái: `status` cho `NEW/CONFIRMED/PREPARING`, `shipment.status` + `carrier_status_text` khi đã giao ĐVVC ("Giao thất bại", "Đơn gặp sự cố"…).
- `summary.counts` theo tab (badge "Mới", "Chờ chuẩn bị").

#### `GET /seller/orders/{id}` — Chi tiết đơn
**Màn hình:** nút "Chi tiết" trên thẻ đơn → modal (trạng thái, phương thức, thời gian đặt, người nhận + địa chỉ, sản phẩm × số lượng, tổng khách trả, lời nhắn, hãng + mã vận đơn + trạng thái hãng, lý do từ chối)
```json
{
  "id": 141, "code": "TR-20260608-000141", "status": "CONFIRMED", "payment_method": "VNPAY", "created_at": "2026-06-08T14:22:00+07:00",
  "shipping_address": "$AddressSnapshot",
  "items": [ { "product_name": "Tai nghe ANC Pro 5", "variant_name": "Đen / Pro", "quantity": 1, "unit_price": 389000 } ],
  "grand_total": 778000,
  "buyer_note": "Gói kỹ giúp mình, đồ làm quà.",
  "shipment": null,
  "reject_reason": null,
  "allowed_actions": ["PREPARE"]
}
```
Địa chỉ đầy đủ chỉ trả ở chi tiết (danh sách chỉ có `ship_to` rút gọn). `404 GNE-ORD-4041` nếu đơn không tồn tại hoặc không thuộc shop.

#### `POST /seller/orders/{id}/confirm` — Xác nhận đơn
`200` `{ "status": "CONFIRMED" }`. Khách nhận thông báo "Shop đã xác nhận đơn".

#### `POST /seller/orders/{id}/reject` — Từ chối đơn
```json
{ "reason_code": "OUT_OF_STOCK", "note": null }
```
`note` bắt buộc khi `reason_code = OTHER`. `200` `{ "status": "CANCELLED_BY_VENDOR" }` — tự nhả tồn kho, hoàn Điểm, voucher; đơn online tự hoàn tiền.

#### `POST /seller/orders/{id}/prepare` — Chuẩn bị hàng
`200` `{ "status": "PREPARING" }`.

#### `GET /seller/orders/{id}/shipping-quotes` — Báo giá hãng vận chuyển
**Màn hình:** modal "Yêu cầu lấy hàng (3PL)" (các hãng shop đã bật, phí, thời gian, hãng được chọn)
```json
[ { "carrier": "GHN", "fee": 24000, "estimated_days": 2, "is_selected": false }, { "carrier": "GHTK", "fee": 22000, "estimated_days": 3, "is_selected": true } ]
```
Luật chọn ở LLD 9.5. Mọi hãng lỗi → trả GHN với phí dự phòng.

#### `POST /seller/orders/{id}/shipments` 🔁 — Bàn giao cho ĐVVC
`201`:
```json
{ "carrier": "GHTK", "tracking_code": "VN931042841", "status": "AWAITING_PICKUP" }
```
Đơn chuyển `READY_FOR_PICKUP`. Tạo vận đơn thất bại sau 3 lần thử → `502 GNE-LOG-5021` ("Tạo vận đơn thất bại, thử lại").

#### `GET /seller/orders/{id}/shipping-label` — Phiếu gửi hàng
**Màn hình:** modal "Phiếu gửi hàng" / "In lại vận đơn"
- `Accept: application/json` → `{ "carrier": "GHTK", "tracking_code": "VN931042841", "recipient_name": "Lê Hoàng Phúc", "ship_to": "Hải Châu, Đà Nẵng", "item_count": 1, "cod_amount": 0, "is_prepaid": true }`
- `Accept: application/pdf` → PDF khổ A6 từ hãng.

### 10.4 Trả hàng (phía shop)
UC-SUP-02 · **Màn hình:** Đơn hàng › tab "Trả hàng" (mã yêu cầu, mã đơn, trạng thái, hạn "còn 31 giờ", khách, sản phẩm, phương án, lý do, ảnh + video bằng chứng, số tiền; nút Yêu cầu bổ sung / Từ chối / Đồng ý nhận lại hàng / Đồng ý hoàn tiền / Đã nhận hàng hoàn / Nhờ Sàn phân xử)

#### `GET /seller/refund-requests?status&page&size`
```json
[
  {
    "id": 231, "code": "RFN-0231",
    "order": { "id": 129, "code": "TR-20260608-000129" },
    "customer_name": "Hoàng Thu Trang",
    "items": [ { "product_name": "Tai nghe ANC Pro 5", "variant_name": "Trắng", "quantity": 1 } ],
    "request_type": "RETURN_AND_REFUND",
    "reason_code": "WRONG_SIZE_COLOR",
    "evidence": [ "$Media" ],
    "refund_amount": 389000,
    "status": "REQUESTED",
    "deadline_at": "2026-06-09T21:00:00+07:00"
  }
]
```

| Method & Path | Body | Kết quả |
|---|---|---|
| `POST /seller/refund-requests/{id}/request-evidence` | `{ "message": "Vui lòng quay rõ phần lỗi" }` | `AWAITING_EVIDENCE`; chỉ 1 lần (`409 GNE-SUP-4091`) |
| `POST /seller/refund-requests/{id}/approve` | `{ "resolution": "APPROVE_REFUND_ONLY" }` hoặc `APPROVE_WITH_RETURN` | Hoàn ngay / tạo vận đơn chiều về (`AWAITING_RETURN_SHIPMENT`) |
| `POST /seller/refund-requests/{id}/reject` | `{ "reason": "..." }` | `SHOP_REJECTED`, khách có 48 giờ khiếu nại |
| `POST /seller/refund-requests/{id}/confirm-return-received` | — | Hàng hoàn về kho → hoàn tiền, `APPROVED → REFUNDED` |
| `POST /seller/refund-requests/{id}/escalate` | — | `DISPUTED` (Nhờ Sàn phân xử) |

Mọi action trả `200` với yêu cầu đã cập nhật.

### 10.5 Sản phẩm
UC-CAT-09, UC-CAT-10, UC-CAT-11 · **Màn hình:** Sản phẩm (tab Tất cả / Đang bán / Bản nháp / Chờ duyệt / Không hợp lệ / Đã ẩn + số đếm; bảng Sản phẩm, SKU, Giá, Kho, Đã bán, Trạng thái; Sửa / Xem / Ẩn / Hiện / Xóa); Form 7 khối (Hình ảnh + video, Giấy tờ chứng thực, Thông tin cơ bản, Thông số kỹ thuật, Mô tả, Phân loại & Giá, Vận chuyển)

#### `GET /seller/products?status&q&page&size`
`status` ∈ `PUBLISHED, DRAFT, PENDING_APPROVAL, REJECTED, HIDDEN` (bỏ trống = tất cả).
```json
[ { "id": 1, "name": "Tai nghe Bluetooth ANC Pro 5", "image_url": "...", "variant_count": 4, "price_min": 389000, "stock_total": 38, "sold_count": 2100, "status": "PUBLISHED", "reject_reason": null } ]
```
Sản phẩm `FLAGGED` (Gemini nghi ngờ) trả `status = PENDING_APPROVAL` cho shop — không lộ kết quả sàng lọc. `summary.counts` theo `status`.

#### `GET /seller/products/{id}` — Mở form sửa / xem
```json
{
  "id": 6,
  "status": "DRAFT",
  "reject_reason": null,
  "images": [ { "object_key": "product-media/vendor/15/product/6/1.webp", "url": "..." } ],
  "video": null,
  "documents": [ { "id": 31, "file_name": "hoa-don-nhap-hang.pdf", "url": "https://s3.genie.vn/product-docs/...?X-Amz-..." } ],
  "name": "Bàn phím cơ RGB switch blue Full-size",
  "category_path": [ { "id": 1, "name": "Điện tử" }, { "id": 11, "name": "Máy tính & Laptop" }, { "id": 111, "name": "Bàn phím" } ],
  "brand": { "id": 9, "name": "Logitech" },
  "condition": "NEW",
  "origin": "Việt Nam",
  "specs": [ { "name": "Loại switch", "value": "Cherry MX Blue" } ],
  "description_html": "<p>…</p>",
  "option_groups": [ { "name": "Màu sắc", "values": [ { "value": "Đen", "image_object_key": null, "image_url": null } ] }, { "name": "Switch", "values": [ { "value": "Blue" }, { "value": "Red" } ] } ],
  "variant_image_group": "Màu sắc",
  "variants": [ { "id": 601, "options": ["Đen", "Blue"], "sku": "KB-BLK-BLU", "price": 459000, "stock": 54 } ],
  "weight_grams": 850,
  "dimensions_cm": { "length": 30, "width": 20, "height": 10 },
  "ships_from_province": { "id": 202, "name": "TP. Hồ Chí Minh" },
  "shipping_payer": "BUYER",
  "free_shipping_threshold": null,
  "is_pre_order": false,
  "pre_order_days": null
}
```
`PENDING_APPROVAL` → chỉ xem ("Sản phẩm đang chờ duyệt. Bạn chỉ có thể xem…").

#### `POST /seller/products` — Thêm sản phẩm · `PUT /seller/products/{id}` — Sửa
Body (`$ProductInput`):
```json
{
  "submit": true,
  "name": "Bàn phím cơ RGB switch blue Full-size",
  "category_id": 111,
  "brand_id": 9,
  "condition": "NEW",
  "origin": "Việt Nam",
  "image_object_keys": ["tmp/product-media/...1.webp", "product-media/vendor/15/product/6/2.webp"],
  "video_object_key": null,
  "document_object_keys": ["tmp/product-docs/...pdf"],
  "specs": [ { "name": "Bảo hành", "value": "12 tháng chính hãng" } ],
  "description_html": "<p>…</p>",
  "option_groups": [ { "name": "Màu sắc", "values": [ { "value": "Đen", "image_object_key": "tmp/product-media/...jpg" } ] } ],
  "variant_image_group": "Màu sắc",
  "variants": [ { "id": null, "options": ["Đen", "Blue"], "sku": null, "price": 459000, "stock": 54 } ],
  "weight_grams": 850,
  "dimensions_cm": { "length": 30, "width": 20, "height": 10 },
  "shipping_payer": "SELLER",
  "free_shipping_threshold": 200000,
  "is_pre_order": false,
  "pre_order_days": null
}
```
- `submit = false` → lưu `DRAFT` (không kiểm đủ trường). `submit = true` → `PENDING_APPROVAL`, gọi Gemini sàng lọc rồi vào hàng chờ Moderator.
- Ảnh đầu tiên là ảnh chính; giữ thứ tự mảng. 1–8 ảnh, ≤ 2 nhóm phân loại, ≤ 50 biến thể, giá > 0, tồn ≥ 0, mô tả ≤ 10.000 ký tự, đặt trước 7–30 ngày (chỉ khi shop bật Pre-order). `sku = null` → server tự sinh.
- Sửa sản phẩm đang bán mà đổi tên / ảnh / mô tả / danh mục / thương hiệu / giấy tờ → về `PENDING_APPROVAL`; chỉ đổi giá, tồn → giữ `PUBLISHED`. Phản hồi có `"requires_review": true|false` để giao diện báo trước.
- `ships_from_province` không nhận từ client: lấy từ địa chỉ lấy hàng của shop.

`201` / `200` → `{ "id": 6, "status": "PENDING_APPROVAL", "requires_review": true }`.

| Lỗi | Khi nào |
|---|---|
| `403 GNE-VND-4031` | Shop chưa `VERIFIED` (nút "Đăng bán" bị khóa) |
| `422 GNE-CAT-4222` | Ngành bắt buộc giấy tờ chứng thực mà chưa có (`details.field = "document_object_keys"`) |
| `422 GNE-CAT-4223` | Thiếu thông số bắt buộc của ngành (`details.missing_specs: ["Bảo hành"]`) |
| `422 GNE-VAL-4220` | Vi phạm giới hạn ở trên |

Hộp thoại "Có 3 phân loại đang có giá 0₫ và tồn kho 0" là cảnh báo phía client, không chặn.

#### `PATCH /seller/products/{id}` — Ẩn / mở bán lại
`{ "status": "HIDDEN" }` hoặc `{ "status": "PUBLISHED" }` (chỉ từ `HIDDEN` do shop tự ẩn). `200`. `409 GNE-CAT-4093` nếu bị sàn khóa.

#### `DELETE /seller/products/{id}` — Xóa (mềm)
`204`. `409 GNE-CAT-4094` khi còn đơn đang xử lý chứa sản phẩm.

#### `GET /seller/categories/{id}/listing-rules` — Luật đăng bán theo ngành
UC-CAT-15 · **Màn hình:** chip "Gợi ý thông số", dòng "Bắt buộc với ngành này: Bảo hành", nhãn "Bắt buộc với ngành này" ở khối Giấy tờ
```json
{
  "category_id": 111,
  "commission_rate": 5.0,
  "requires_documents": false,
  "attributes": [ { "code": "warranty", "name": "Bảo hành", "data_type": "ENUM", "values": ["6 tháng", "12 tháng", "24 tháng"], "unit": null, "is_required": true, "is_variant_axis": false } ]
}
```
Gồm thuộc tính kế thừa từ ngành cha.

#### Thương hiệu
**Màn hình:** ô "Thương hiệu" (tìm + "Thêm "…" làm thương hiệu mới")

| Method & Path | Phản hồi |
|---|---|
| `GET /seller/brands?q=log&limit=10` | `[{ "id": 9, "name": "Logitech" }]` |
| `POST /seller/brands` `{ "name": "Akko" }` | `201` `{ "id": 41, "name": "Akko" }` (thương hiệu chưa xác minh); trùng tên → `200` trả bản có sẵn |

### 10.6 Danh mục toàn sàn & đề xuất danh mục
UC-CAT-17 · **Màn hình:** Danh mục toàn sàn (cây có số sản phẩm + hoa hồng; "Đề xuất của Shop" + trạng thái; modal Đề xuất danh mục mới)

#### `GET /seller/categories`
```json
[ { "id": 1, "name": "Điện tử", "commission_rate": 5.0, "product_count": 1284, "children": [ { "id": 11, "name": "Máy tính & Laptop", "commission_rate": 4.0, "product_count": 386 } ] } ]
```
2 cấp đầu (màn hình chỉ hiện 2 cấp).

#### `GET /seller/category-proposals`
```json
[ { "id": 81, "code": "CP-081", "name": "Thiết bị đeo thông minh", "parent": { "id": 1, "name": "Điện tử" }, "status": "PENDING", "reject_reason": null, "created_at": "2026-06-06T10:00:00+07:00" } ]
```

#### `POST /seller/category-proposals`
```json
{ "name": "Thiết bị đeo thông minh", "parent_id": 1, "reason": "Sản phẩm smartwatch chưa khớp danh mục nào" }
```
`parent_id = null` → đề xuất danh mục gốc. `201`.

| Lỗi | Khi nào |
|---|---|
| `409 GNE-CAT-4091` | Trùng tên danh mục có sẵn — `details.existing_category: { "id", "name" }` |
| `422 GNE-CAT-4221` | Đã có 5 đề xuất đang chờ duyệt |

### 10.7 Khuyến mãi — Voucher shop & Flash Sale
UC-PRO-01, UC-PRO-05 · **Màn hình:** Khuyến mãi (banner Flash Sale đăng ký, 3 chỉ số, bảng mã giảm giá: Mã / Loại / Điều kiện / Đã dùng / Trạng thái / Kết thúc; Chỉnh sửa / Nhân bản / Tắt), modal "Tạo Voucher Shop", modal "Đăng ký Flash Sale"

#### `GET /seller/vouchers?status&page&size`
```json
[ { "id": 51, "code": "SALE15", "discount_type": "PERCENTAGE", "discount_value": 15, "max_discount": 50000, "min_order_value": 250000, "usage": { "used": 142, "limit": 500 }, "status": "ACTIVE", "valid_to": "2026-06-20T23:59:59+07:00" } ]
```
`status` ∈ `ACTIVE, SCHEDULED, ENDED, DISABLED`. `summary`: `{ "active_count": 2, "usage_this_month": 522, "revenue_from_promotions_this_month": 8400000 }`.

#### `GET /seller/vouchers/{id}` — Mở form sửa / nhân bản
→ `$ShopVoucherInput` + `id`, `status`, `usage`.

#### `POST /seller/vouchers` · `PUT /seller/vouchers/{id}`
`$ShopVoucherInput`:
```json
{
  "code": "SUMMER2026",
  "discount_type": "PERCENTAGE",
  "discount_value": 15,
  "max_discount": 50000,
  "min_order_value": 200000,
  "usage_limit": 500,
  "usage_per_user": 1,
  "budget": 5000000,
  "valid_from": "2026-06-10T00:00:00+07:00",
  "valid_to": "2026-06-30T23:59:59+07:00",
  "scope": "CATEGORY",
  "category_ids": [13],
  "product_ids": [],
  "excluded_category_ids": [14]
}
```
- `code` 6–12 ký tự `A-Z0-9`, duy nhất toàn hệ thống (`409 GNE-VCH-4092`). Nút "Tự tạo" sinh mã phía client.
- `discount_type = FREESHIP` → bỏ `discount_value`, `max_discount`. `PERCENTAGE` ≤ 100.
- `scope` ∈ `ALL` (tất cả sản phẩm shop), `CATEGORY` (cần `category_ids`), `PRODUCT` (cần `product_ids`); `excluded_category_ids` chỉ dùng khi `scope ≠ PRODUCT`.
- Voucher shop: `type = SHOP`, `funded_by = VENDOR` (server tự đặt).
- `201` / `200` → voucher như danh sách.

#### `PATCH /seller/vouchers/{id}` — Tắt voucher
`{ "is_active": false }` → `200`.

#### `GET /seller/flash-sales?registrable=true` — Chiến dịch đang mở đăng ký
```json
[ { "id": 2103, "name": "FLASH SALE 12.12", "starts_at": "2026-12-12T00:00:00+07:00", "ends_at": "2026-12-12T02:00:00+07:00", "registration_deadline": "2026-12-10", "max_items_per_shop": 20, "registered_count": 0 } ]
```

#### `GET /seller/flash-sales/{id}/eligible-products` — Ô chọn "Sản phẩm / SKU"
```json
[ { "product_id": 1, "name": "Tai nghe ANC Pro 5", "reference_price": 389000, "max_flash_price": 330650, "stock": 38 } ]
```
`reference_price` = giá bán thấp nhất 30 ngày; `max_flash_price` = 85% (giá Flash phải thấp hơn ít nhất 15%).

#### `GET /seller/flash-sales/{id}/items` — Đã đăng ký, chờ sàn duyệt
`[{ "id": 7, "product_id": 1, "name": "Tai nghe ANC Pro 5", "flash_price": 299000, "flash_stock": 30, "status": "PENDING" }]`

#### `POST /seller/flash-sales/{id}/items` — Đăng ký sản phẩm
```json
{ "product_id": 1, "flash_price": 299000, "flash_stock": 30 }
```
Đăng ký lại cùng sản phẩm → ghi đè (upsert). `201`.

| Lỗi | Khi nào |
|---|---|
| `422 GNE-PRO-4221` | Giá Flash > `max_flash_price` hoặc số suất ngoài 1..tồn kho |
| `422 GNE-PRO-4223` | Quá `max_items_per_shop` / quá hạn đăng ký |

### 10.8 Đánh giá (phía shop)
UC-SOC-04, UC-SOC-05 · **Màn hình:** Đánh giá (khối điểm TB + phân bố sao; tab Tất cả / Chưa phản hồi / Cần chú ý ≤ 3★; thẻ đánh giá; Phản hồi ≤ 500 ký tự, không chứa SĐT; Báo cáo)

#### `GET /seller/reviews?filter&page&size`
`filter` ∈ `ALL, UNREPLIED, LOW_RATING`.
```json
[
  { "id": 5520, "reviewer": { "display_name": "Lê H. Phúc", "avatar_url": null }, "rating": 2, "product_name": "Bàn phím cơ RGB", "variant_name": "Blue switch", "content": "Một phím bị kẹt, mong shop hỗ trợ đổi.", "media": [ "$Media" ], "created_at": "2026-06-05T09:00:00+07:00", "reply": null, "is_reported_by_me": false }
]
```
`summary`: `{ "rating_avg": 4.7, "rating_count": 1109, "distribution": { "5": 842, "4": 196, "3": 48, "2": 14, "1": 9 }, "counts": { "ALL": 1109, "UNREPLIED": 2, "LOW_RATING": 71 } }`.

#### `PUT /seller/reviews/{id}/reply`
`{ "content": "Cảm ơn bạn đã ủng hộ shop" }` → `200` `{ "content": "...", "replied_at": "..." }`. `422 GNE-SOC-4221` chứa số điện thoại; `409 GNE-SOC-4094` đã phản hồi.

Báo cáo vi phạm: `POST /reviews/{id}/reports` (mục 7.10).

### 10.9 Phân tích bán hàng
UC-VND-08 · **Màn hình:** Phân tích (chọn Hôm nay / 7 ngày qua / 30 ngày qua / Tùy chọn ngày; 4 ô Doanh thu gộp, Đơn hàng, Lượt truy cập, Tỉ lệ chuyển đổi; biểu đồ doanh thu; Tiền thực về shop; Đơn kết thúc thế nào + sức khỏe shop; Nguồn truy cập; Sản phẩm bán chạy)

#### `GET /seller/analytics/overview?range=7d` (hoặc `range=custom&from=2026-06-01&to=2026-06-08`)
`range` ∈ `today, 7d, 30d, custom`.
```json
{
  "period": { "from": "2026-06-02", "to": "2026-06-08", "comparison_label": "PREVIOUS_7_DAYS" },
  "revenue": { "gross": 12600000, "previous": 10800000 },
  "orders": { "count": 29, "previous": 24 },
  "visits": { "count": 860, "previous": 760 },
  "chart": { "granularity": "DAY", "points": [ { "label": "2026-06-02", "value": 1600000 } ] },
  "net_revenue": { "net": 11674000, "commission": 630000, "commission_rate": 5.0, "shop_voucher_cost": 296000 },
  "outcomes": { "COMPLETED": 17, "CANCELLED_BY_CUSTOMER": 1, "REJECTED_BY_SHOP": 1, "REFUNDED": 0, "IN_PROGRESS": 10 },
  "health_30d": { "lost_disputes": 1, "auto_confirmed_orders": 2 }
}
```
- `chart.granularity` = `HOUR` khi `today`.
- Tỉ lệ chuyển đổi và tỉ lệ shop hủy/từ chối client tự tính.
- `net = gross − commission − shop_voucher_cost` (voucher sàn và Điểm do sàn chịu).

#### `GET /seller/analytics/traffic-sources?range=30d`
```json
[ { "source": "SEARCH", "visits": 1435, "share_percent": 46.0, "conversion_rate": 3.9 } ]
```
Nguồn: `product_view_stats`.

#### `GET /seller/analytics/top-products?range=30d&limit=5`
```json
[ { "product_id": 1, "name": "Tai nghe ANC Pro 5", "image_url": "...", "sold": 38, "revenue": 14782000, "revenue_share_percent": 31.8, "conversion_rate": 8.2 } ]
```
`summary.total_revenue` cho dòng "Đóng góp tính trên 46,5tr doanh thu gộp".

#### `GET /seller/analytics/sentiment?range=7d` — Cảm xúc đánh giá
UC-AI-07 · **Màn hình:** khối "Cảm xúc đánh giá" ở Phân tích (thanh Tích cực / Trung lập / Tiêu cực, tỉ lệ tích cực so với kỳ trước, sản phẩm bị chê nhiều nhất, "Xem đánh giá")
`range` như overview.
```json
{ "analyzed_count": 64, "positive": 47, "neutral": 9, "negative": 8, "previous_positive": 41, "most_negative_product": { "product_id": 18, "name": "Bàn phím cơ RGB", "negative": 4 } }
```
Chỉ tính đánh giá có chữ, `score ≥ 0,6` (LLD 9.8); cache Redis `sentiment:vendor:{id}` bị xóa khi AI Worker trả kết quả mới.

### 10.10 Ví & Escrow
UC-VND-20, UC-ADM-04 · **Màn hình:** Ví & Escrow (Số dư khả dụng, Đang giữ, Doanh thu 30 ngày, Chờ đối soát COD, Đang rút, "Cách tính tiền về ví", Sổ giao dịch ví có lọc tháng / loại / tìm, Xuất CSV), modal "Yêu cầu rút tiền"

#### `GET /seller/wallet`
```json
{
  "available_balance": 12450000,
  "held_balance": 1907600,
  "cod_pending": 1950000,
  "pending_withdrawal": 5000000,
  "revenue_30d": { "value": 46500000, "previous": 38680000 },
  "commission_example": { "gross": 778000, "commission_rate": 5.0, "commission": 38900, "net": 739100 },
  "payout_account": { "bank_name": "Vietcombank", "account_holder": "DANG QUOC HUY", "account_number_masked": "•••• •••• 6712", "is_verified": true },
  "min_withdrawal": 100000
}
```
`commission_example` lấy từ đơn bán gần nhất. `payout_account = null` → modal rút tiền hiện "Thêm ở Cài đặt".

#### `GET /seller/wallet/transactions?month=2026-06&type=SALE&q=TR-2026&page&size`
`type` ∈ `SALE, RELEASE, PAYOUT, REFUND`.
```json
[ { "id": 90012, "created_at": "2026-06-08T14:22:00+07:00", "reference_code": "TR-20260608-000141", "type": "SALE", "description": "Bán hàng — vào escrow", "gross_amount": 778000, "commission_amount": 38900, "net_amount": 739100, "state": "HELD" } ]
```
`state` ∈ `HELD` (Escrow), `AVAILABLE` (Khả dụng), `PAYOUT_PENDING` (Chờ duyệt), `PAYOUT_DONE` (Đã rút), `REFUNDED` (Hoàn). `summary.available_months: ["2026-06", "2026-05"]` cho ô chọn tháng. Đây là **sổ hợp nhất** từ `escrow_ledgers` + `wallet_transactions` (xem mục 17).

#### `GET /seller/wallet/transactions/export?month=2026-06&type=` → `text/csv`
Cột: Thời gian, Mã tham chiếu, Loại, Diễn giải, Gross, Hoa hồng, Net. Tên tệp `so-giao-dich-vi-06-2026.csv`.

#### `POST /seller/withdrawals` 🔁 🔐 `purpose = WITHDRAWAL`
```json
{ "amount": 5000000 }
```
`201`:
```json
{ "id": 13, "code": "PAYOUT-0013", "amount": 5000000, "status": "PENDING", "bank_account_masked": "Vietcombank ••6712", "requested_at": "2026-06-08T09:00:00+07:00" }
```
Tiền chuyển từ khả dụng sang "Đang rút"; kế toán duyệt trong 24 giờ làm việc.

| Lỗi | Khi nào |
|---|---|
| `422 GNE-WAL-4221` | Dưới 100.000₫ |
| `400 GNE-WAL-4001` | Vượt số dư khả dụng |
| `403 GNE-WAL-4032` | Chưa có tài khoản nhận tiền đã xác minh |

### 10.11 Cài đặt Shop
UC-VND-05, UC-VND-06, UC-VND-07, UC-VND-19, UC-VND-21 · **Màn hình:** Cài đặt (Thông tin gian hàng: ảnh bìa, logo, tên — đổi 1 lần/30 ngày, mô tả; Vận chuyển: 5 hãng + freeship từ X₫; Địa chỉ lấy hàng GHN + GHN Shop ID; Tài khoản nhận tiền; Tuỳ chọn vận hành: tự trả lời chat, Pre-order, Chế độ nghỉ; Bảo mật)

#### `GET /seller/settings` — Tải toàn bộ trang (1 lời gọi)
```json
{
  "profile": { "shop_name": "TechZone Official", "description": "Thiết bị công nghệ chính hãng — bảo hành 12 tháng.", "logo_url": null, "cover_url": "...", "next_rename_allowed_at": null },
  "shipping": { "carriers": { "GHN": true, "GHTK": true, "VIETTEL_POST": false, "JNT_EXPRESS": false, "GENIE_EXPRESS": false }, "free_shipping_enabled": true, "free_shipping_threshold": 99000 },
  "pickup_address": { "phone_number": "0912345678", "province": "$Region", "district": "$Region", "ward": { "code": "20710", "name": "P. Tân Phong" }, "address_line": "Kho TechZone, 88 Nguyễn Văn Linh", "ghn_shop_id": 884120 },
  "payout_account": { "bank_name": "Vietcombank", "account_holder": "DANG QUOC HUY", "account_number_masked": "•••• •••• 6712", "is_verified": true },
  "auto_reply": { "enabled": true, "greeting": "Chào bạn, shop có thể giúp gì cho bạn?", "away_message": "Hiện tại shop đang bận…" },
  "pre_order_enabled": false,
  "vacation": null
}
```
`vacation` khi đang nghỉ: `{ "ends_at": "2026-07-15T00:00:00+07:00", "message": "Shop đang tạm nghỉ…" }`.

#### `PATCH /seller/settings` — "Lưu cài đặt" / "Lưu tin nhắn"
Gửi khối nào cập nhật khối đó:
```json
{
  "profile": { "shop_name": "TechZone Official", "description": "…", "logo_object_key": "tmp/avatar-assets/...png", "cover_object_key": null },
  "shipping": { "carriers": { "GHN": true, "GHTK": true, "VIETTEL_POST": false, "JNT_EXPRESS": false, "GENIE_EXPRESS": false }, "free_shipping_enabled": true, "free_shipping_threshold": 99000 },
  "auto_reply": { "enabled": true, "greeting": "…", "away_message": "…" },
  "pre_order_enabled": false
}
```
`200` → như `GET`.

| Lỗi | Khi nào |
|---|---|
| `409 GNE-VND-4091` | Đổi tên shop lần 2 trong 30 ngày (`details.next_rename_allowed_at`) |
| `422 GNE-VND-4221` | Tắt hết hãng vận chuyển |
| `422 GNE-VND-4222` | Bật tự trả lời mà cả 2 nội dung đều rỗng |

#### `PUT /seller/pickup-address` — "Lưu và Đồng bộ với GHN"
UC-VND-05
```json
{ "phone_number": "0912345678", "province_id": 202, "district_id": 1449, "ward_code": "20710", "address_line": "Kho TechZone, 88 Nguyễn Văn Linh" }
```
`200` `{ "...pickup_address", "ghn_shop_id": 884120 }`. GHN lỗi → `502 GNE-LOG-5022`.

#### Tài khoản nhận tiền
UC-VND-07 · **Màn hình:** modal "Cập nhật tài khoản nhận tiền" (Ngân hàng, Tên chủ TK, Số TK → "Xác minh chính chủ" → OTP → "Xác nhận & lưu"), modal "Xóa tài khoản" (OTP)

| Method & Path | Body | Phản hồi |
|---|---|---|
| `POST /seller/payout-account/verification` | `{ "bank_code": "VCB", "account_number": "0071000006712", "account_holder": "Đặng Quốc Huy" }` | `200` `{ "matched": true, "normalized_holder": "DANG QUOC HUY" }`; `422 GNE-WAL-4222` tên không khớp CCCD (`details.kyc_name`) |
| `PUT /seller/payout-account` 🔐 `PAYOUT_ACCOUNT_UPDATE` | như trên | `200` `payout_account` |
| `DELETE /seller/payout-account` 🔐 `PAYOUT_ACCOUNT_DELETE` | — | `204`; `409 GNE-WAL-4091` còn lệnh rút đang chờ |

Danh sách ngân hàng: `GET /public/banks` → `[{ "code": "VCB", "name": "Vietcombank" }]` (dùng chung cho modal trả hàng của khách).

#### Chế độ nghỉ
UC-VND-19 · **Màn hình:** công tắc "Chế độ nghỉ (Vacation)" → modal "Bật chế độ nghỉ" (mở lại ngày ≤ 30 ngày, tin nhắn tự động; nếu còn đơn chưa bàn giao thì liệt kê)

| Method & Path | Body | Phản hồi |
|---|---|---|
| `PUT /seller/vacation` | `{ "ends_at": "2026-07-15T00:00:00+07:00", "message": "Shop đang tạm nghỉ…" }` | `200` `vacation`; `409 GNE-VND-4092` còn đơn chưa bàn giao (`details.pending_order_codes`); `422 GNE-VND-4224` quá 30 ngày / ngày trong quá khứ |
| `DELETE /seller/vacation` | — | `204` (mở bán lại) |

#### Bảo mật
Dùng chung `/me/login-methods`, `/me/password/verify`, `PUT /me/password` (mục 3.3–3.4) — tài khoản shop là tài khoản người mua của chủ shop.

### 10.12 Hồ sơ KYC
UC-VND-02, UC-VND-03, UC-VND-04 · **Màn hình:** Hồ sơ KYC (băng trạng thái Chưa nộp / Chờ duyệt / Đã duyệt / Từ chối + "Yêu cầu nhân viên xem xét" / Cần bổ sung + hạn 7 ngày; Thông tin người bán: Tên shop, Loại hình, Số CCCD, MST, Người đại diện; Giấy tờ: CCCD mặt trước, mặt sau, GPKD (Hộ KD/DN); selfie tải lên hoặc chụp camera có kiểm tra người thật; đồng ý sinh trắc; Lưu nháp; Gửi duyệt KYC / Gửi lại hồ sơ)

#### `GET /seller/kyc`
```json
{
  "status": "SUPPLEMENT_REQUESTED",
  "shop_name": "TechZone Official",
  "business_type": "BUSINESS",
  "identity_number": "079096001234",
  "tax_code": "0312345678",
  "representative": "Đặng Quốc Huy",
  "documents": {
    "ID_FRONT": { "file_name": "cccd-truoc.jpg", "size_bytes": 2202009 },
    "ID_BACK": { "file_name": "cccd-sau.jpg", "size_bytes": 1992294 },
    "BUSINESS_LICENSE": { "file_name": "gpkd.pdf", "size_bytes": 3565158 },
    "SELFIE": { "file_name": "selfie.jpg", "size_bytes": 812004 }
  },
  "biometric_consent": true,
  "rejection_reason": null,
  "supplement": { "documents": ["BUSINESS_LICENSE"], "note": "Vui lòng cung cấp GPKD còn hạn", "deadline_at": "2026-06-15T10:00:00+07:00" },
  "manual_review_requested": false,
  "submitted_at": "2026-06-08T10:24:00+07:00"
}
```
Giấy tờ chưa tải = `null`. Không trả URL ảnh CCCD về lại cho shop (giảm rò rỉ — màn hình chỉ hiện tên tệp và dung lượng).

#### `PUT /seller/kyc/draft` — Lưu nháp
Body như 10.12 submit nhưng mọi trường tùy chọn. `200`.

#### `POST /seller/kyc/submissions` — Gửi duyệt / Gửi lại
```json
{
  "shop_name": "TechZone Official",
  "business_type": "BUSINESS",
  "identity_number": "079096001234",
  "tax_code": "0312345678",
  "representative": "Đặng Quốc Huy",
  "document_object_keys": { "ID_FRONT": "tmp/kyc-dossiers/...jpg", "ID_BACK": "tmp/kyc-dossiers/...jpg", "BUSINESS_LICENSE": "tmp/kyc-dossiers/...pdf", "SELFIE": "tmp/kyc-dossiers/...jpg" },
  "biometric_consent": true
}
```
- `identity_number` đúng 12 chữ số; `tax_code` bắt buộc với `HOUSEHOLD`, `BUSINESS`; `BUSINESS_LICENSE` bắt buộc với `HOUSEHOLD`, `BUSINESS`; `SELFIE` luôn bắt buộc. Giấy tờ đã có từ lần trước có thể bỏ (giữ bản cũ).
- `biometric_consent = false` → bỏ qua so khớp khuôn mặt tự động, chuyển nhân viên đối chiếu.
- `202` `{ "status": "PENDING" }` — gọi AI Worker bất đồng bộ (QR → MRZ → khuôn mặt + liveness → điểm rủi ro).
- `422 GNE-VND-4223` thiếu giấy tờ (`details.missing: ["SELFIE"]`); `409 GNE-VND-4095` hồ sơ đang `PENDING / AI_PROCESSING / MANUAL_REVIEW` hoặc đã `VERIFIED`.

#### `POST /seller/kyc/manual-review-requests` — "Yêu cầu nhân viên xem xét"
Chỉ khi `REJECTED`. `202`. `409 GNE-VND-4096` nếu đã gửi (nút chuyển thành "Đã yêu cầu nhân viên xem xét").

---

## 11. Admin Console — `/admin`

Vai trò `STAFF` / `ADMIN`. Mỗi endpoint ghi mã quyền. Bảng quản trị dùng phân trang số (`page`, `size`).

### 11.1 Khung Admin
**Màn hình:** sidebar (nav ẩn/hiện theo quyền, badge hàng chờ), chuông, Tài khoản

#### `GET /admin/me` — không cần mã quyền
```json
{ "id": 3, "full_name": "Đặng Hải Yến", "email": "yen.dh@genie.example", "avatar_url": null, "role": "ADMIN", "staff_title": null, "permissions": ["VIEW_DASHBOARD", "REVIEW_KYC", "..."], "status": "ACTIVE" }
```
Nav chỉ hiện mục có mã quyền trong `permissions`. Truy cập trái quyền → màn "Bạn không có quyền truy cập".

#### `PATCH /admin/me` — Sửa tên, ảnh đại diện
`{ "full_name": "Đặng Hải Yến", "avatar_object_key": "tmp/avatar-assets/..." }` → `200`. Đổi mật khẩu (bắt buộc OTP) và phiên đăng nhập dùng `/me/password`, `/me/sessions`.

#### `GET /admin/queues` — Badge nav + khối "Việc cần xử lý"
Chỉ trả hàng chờ mà người gọi có quyền.
```json
{ "kyc_pending": 3, "products_pending": 3, "disputes_open": 2, "withdrawals_pending": 4, "withdrawals_blocked": 1, "cod_remittances_waiting": 2 }
```
Khóa vắng mặt khi thiếu quyền (vd. CS Specialist không có `withdrawals_*`). Cập nhật đẩy qua WebSocket `/user/queue/admin-queues`.

### 11.2 Tổng quan
UC-ADM-06 · `VIEW_DASHBOARD` (+ `VIEW_FINANCE` cho khối tiền) · **Màn hình:** Việc cần xử lý, Kỳ báo cáo + Xuất CSV, 4 ô GMV / Hoa hồng / Chi phí voucher và freeship / Lợi nhuận ròng, "Hoa hồng đi về đâu", biểu đồ GMV (7 ngày / Theo tuần / 6 tháng), Tiền sàn đang giữ cho shop, Tiền đang chờ xử lý, Đơn đang chạy, Shop có GMV cao nhất, Sản phẩm bán chạy, Shop tăng trưởng nhanh

#### `GET /admin/dashboard/finance?month=2026-06` — `VIEW_FINANCE`
```json
{
  "month": "2026-06",
  "available_months": ["2026-06", "2026-05", "2026-04"],
  "gmv": { "value": 1842650000, "previous": 1687600000 },
  "commission": { "value": 92132500, "previous": 84380000 },
  "voucher_cost": { "value": 45680000, "previous": 39820000 },
  "coin_cost": 3840000,
  "gateway_fee": 5180000,
  "net_profit": 37432500,
  "holdings": { "escrow_held": 326480000, "wallet_balances": 118730000 },
  "pending_money": { "payable_withdrawals": 8150000, "blocked_withdrawals": 1200000, "cod_remittances": 1895000, "refunds_pending": 498000, "open_disputes": 2 },
  "top_shops_by_gmv": [ { "shop_id": 15, "name": "TechZone Official", "gmv": 45800000 } ]
}
```
`net_profit = commission − voucher_cost − coin_cost − gateway_fee`. `holdings` là số dư hiện tại (không theo kỳ). `top_shops_by_gmv` tối đa 8.

#### `GET /admin/dashboard/gmv-chart?range=6m` — `VIEW_FINANCE`
`range` ∈ `7d, weekly, 6m` (7 ngày cuối kỳ / theo tuần trong tháng / 6 tháng).
```json
{ "title_key": "LAST_6_MONTHS", "points": [ { "label": "2026-01", "value": 1290000000 } ] }
```

#### `GET /admin/dashboard/operations` — `VIEW_DASHBOARD`
```json
{
  "orders_in_progress": [ { "group": "AWAITING_PAYMENT", "count": 28 }, { "group": "PAID", "count": 142 }, { "group": "PREPARING", "count": 206 }, { "group": "DELIVERING", "count": 442 }, { "group": "DELIVERED", "count": 263 } ],
  "orders_finished": [ { "group": "COMPLETED", "count": 2806 }, { "group": "CANCELLED", "count": 99 }, { "group": "RETURNED_REFUNDED", "count": 18 } ],
  "top_products": [ { "product_id": 16, "name": "Áo thun cotton basic", "sold": 150 } ]
}
```
`top_products` tối đa 8.

#### `GET /admin/dashboard/growing-shops?category_id&q&page&size=5` — `VIEW_DASHBOARD`
```json
[ { "shop": { "id": 12, "name": "Beauty Box", "logo_url": null }, "category_name": "Làm đẹp", "sold_count": 263, "rating_avg": 4.9, "growth_percent": 38.0, "gmv": 31890000 } ]
```
Sắp theo `growth_percent` giảm dần. `gmv` chỉ có khi có `VIEW_FINANCE`.

#### `GET /admin/dashboard/export?month=2026-06` — `VIEW_FINANCE` → `text/csv`
Tệp `bao-cao-06-2026.csv`: chỉ số kỳ, điểm biểu đồ, bảng shop, trạng thái đơn, sản phẩm bán chạy.

### 11.3 Duyệt KYC
UC-VND-04 · `REVIEW_KYC` · **Màn hình:** hàng chờ (tab Chờ duyệt / Chờ bổ sung / Đã duyệt / Từ chối / Tất cả), chi tiết (mức rủi ro, "Hậu kiểm ngẫu nhiên", hồ sơ đã tải + trình xem phóng to/xoay/lật, Kết quả xác thực tự động: QR Code / Quốc huy / MRZ / Vân tay / Khớp khuôn mặt, so sánh khuôn mặt, dữ liệu kê khai, hoa hồng riêng của shop, lỗi phát hiện), Từ chối (lý do), Phê duyệt, Yêu cầu bổ sung (chọn giấy tờ + ghi chú, 7 ngày), Đưa lại vào hàng chờ

#### `GET /admin/kyc-submissions?status=PENDING&page&size`
`status` ∈ `PENDING` (gồm `AI_PROCESSING`, `MANUAL_REVIEW`), `SUPPLEMENT_REQUESTED`, `VERIFIED`, `REJECTED` (bỏ trống = tất cả).
```json
[ { "id": 1042, "code": "KYC-1042", "shop": { "id": 31, "name": "Nhà Sách Mộc", "logo_url": null }, "owner_name": "Phạm Thị Lan", "business_type": "HOUSEHOLD", "status": "PENDING", "submitted_at": "2026-06-08T10:24:00+07:00" } ]
```
`summary.counts` theo `status`.

#### `GET /admin/kyc-submissions/{id}`
```json
{
  "id": 1041, "code": "KYC-1041",
  "shop": { "id": 32, "name": "Gốm Bát Tràng Xưa" },
  "owner_name": "Trần Quốc Bảo", "business_type": "BUSINESS", "status": "PENDING", "submitted_at": "...",
  "risk_level": "MEDIUM", "risk_score": 42, "is_random_audit": true,
  "documents": [ { "type": "ID_FRONT", "url": "https://s3.genie.vn/kyc-dossiers/...?X-Amz-Expires=300", "content_type": "image/jpeg" } ],
  "ai_result": {
    "overall": "PARTIAL",
    "checks": { "qr_code": true, "national_emblem": true, "mrz": false, "fingerprint": true, "face_match": true },
    "face_match_score": 0.91,
    "errors": ["MRZ mờ, không đọc được 2 ký tự cuối."]
  },
  "declared": { "identity_number_masked": "0790•••••041", "representative": "Trần Quốc Bảo", "tax_code": "0312345678" },
  "shop_commission_rate": null,
  "rejection_reason": null,
  "supplement": null
}
```
- `risk_level` ∈ `LOW` (< 30), `MEDIUM` (30–69), `HIGH` (≥ 70). `ai_result.overall` ∈ `PASS, PARTIAL, FAIL`; `ai_result` = `null` khi AI chưa chạy xong / lỗi.
- URL tài liệu là Presigned GET 5 phút; mỗi lần mở ghi `system_logs` (NFR-07).

| Method & Path | Body | Kết quả |
|---|---|---|
| `POST /admin/kyc-submissions/{id}/approve` | `{ "shop_commission_rate": 3.0 }` (bỏ trống = theo ngành) | `VERIFIED`, `kyc_verification_method = MANUAL_STAFF` |
| `POST /admin/kyc-submissions/{id}/reject` | `{ "reason_code": "BLURRY_DOCUMENT", "note": "…" }` (`note` bắt buộc khi `OTHER`) | `REJECTED`, thông báo + email shop |
| `POST /admin/kyc-submissions/{id}/request-supplement` | `{ "documents": ["ID_FRONT", "BUSINESS_LICENSE"], "note": "Ảnh CCCD mặt trước bị mờ…" }` (≥ 1 giấy tờ) | `SUPPLEMENT_REQUESTED`, hạn 7 ngày |
| `POST /admin/kyc-submissions/{id}/requeue` | — | Về `PENDING` |

Mỗi action trả `200` với chi tiết đã cập nhật; ghi `system_logs KYC_APPROVED / KYC_REJECTED`.

### 11.4 Trả hàng & Hoàn tiền (phân xử)
UC-SUP-03, UC-SUP-04 · `HANDLE_DISPUTE` · **Màn hình:** đếm Đang mở / Đã hoàn / Từ chối; thẻ tranh chấp (mã yêu cầu, mã đơn, người mua, người bán, lý do, bằng chứng khách + shop, số tiền); modal "Phân xử" (4 phán quyết + biên bản); "Hàng đã về kho shop"; "Mở lại"; khối "Trạng thái hoàn tiền Gateway" (Đồng bộ trạng thái, Thử lại hoàn tiền, Đã chuyển khoản thủ công + nội dung `TRRF…`)

#### `GET /admin/disputes?status=OPEN&page&size`
`status` ∈ `OPEN` (`DISPUTED`), `AWAITING_RETURN`, `REFUNDED`, `DENIED`.
```json
[
  {
    "id": 231, "code": "RFN-0231",
    "order": { "id": 135, "code": "TR-20260608-000135", "payment_method": "VIETQR" },
    "customer": { "id": 20502, "name": "Vũ Minh Quân" },
    "shop": { "id": 12, "name": "Beauty Box" },
    "reason_code": "NOT_AS_DESCRIBED",
    "amount": 178000,
    "status": "OPEN",
    "opened_at": "2026-06-08T12:00:00+07:00",
    "customer_evidence": [ "$Media" ],
    "shop_evidence": [ "$Media" ],
    "escrow": { "id": 5512, "status": "DISPUTED" },
    "refund": null
  }
]
```
`escrow.status = FROZEN` → nhãn "Ký quỹ đang phong tỏa".
Khi đã hoàn, `refund`:
```json
{ "id": 77, "gateway": "VNPAY", "original_transaction_id": "TXN-8847", "gateway_refund_id": "VNP-RF-20260620-001", "amount": 389000, "status": "SUCCESS", "created_at": "2026-06-20T14:32:10+07:00", "manual_transfer": null }
```
Đơn VietQR / COD: `gateway_refund_id = null`, `manual_transfer = { "to_account": "Techcombank ••4471 · VU MINH QUAN", "transfer_content": "TRRF0231" }`, `status = PENDING` → nhãn "Chờ chuyển khoản thủ công". `summary.counts` theo `status`.

#### `POST /admin/disputes/{id}/resolve` — Thực thi phán quyết
```json
{ "outcome": "REFUND_NOW", "verdict": "Ảnh mở hộp cho thấy sai màu so với mô tả…" }
```
| `outcome` | Hệ quả |
|---|---|
| `REFUND_NOW` | A. Người mua đúng — hoàn từ escrow, tính khiếu nại thua cho shop |
| `RETURN_THEN_REFUND` | A. Người mua đúng — tạo vận đơn chiều về, khách có 7 ngày giao hàng → `AWAITING_RETURN` |
| `DENY` | B. Người bán đúng — bác yêu cầu, mở khóa ký quỹ, cảnh báo khách |
| `CARRIER_FAULT` | C. Lỗi vận chuyển — hoàn khách, đền bù shop theo giá khai |

`verdict` bắt buộc. `200` → tranh chấp đã cập nhật.

| Method & Path | Mô tả |
|---|---|
| `POST /admin/disputes/{id}/confirm-return-received` | "Hàng đã về kho shop" → hoàn tiền |
| `POST /admin/disputes/{id}/reopen` | "Mở lại" → về `OPEN` |
| `POST /admin/refunds/{id}/sync` | Hỏi lại VNPay trạng thái hoàn → `200` khối `refund` |
| `POST /admin/refunds/{id}/retry` 🔁 | Gửi lại lệnh hoàn VNPay khi `FAILED` |
| `POST /admin/escrows/{id}/freeze` | "Phong tỏa ký quỹ" — body `{ "reason": "Nghi gian lận…" }` (bắt buộc). Ký quỹ → `FROZEN`, không giải ngân kể cả quá 7 ngày; ghi `system_logs`. `409 GNE-PAY-4093` khi ký quỹ đã `RELEASED / REFUNDED` |
| `POST /admin/escrows/{id}/unfreeze` | "Gỡ phong tỏa" → về trạng thái trước (`HELD` / `DISPUTED`) |
| `POST /admin/refunds/{id}/mark-transferred` | Xác nhận đã chuyển khoản tay (VietQR/COD). Body `{ "bank_reference": "FT26170123456" }` (tùy chọn — webhook SePay `TRRF…` cũng tự đánh dấu) |

### 11.5 Kiểm duyệt đánh giá
UC-SOC-05 · `MODERATE_REVIEW` · **Màn hình:** danh sách đánh giá bị báo cáo (sắp theo số báo cáo; người dùng che tên, số sao, sản phẩm, mã, nội dung, "Cờ: …", số báo cáo; Giữ hiển thị / Ẩn đánh giá / Hiện lại)

#### `GET /admin/review-reports?state=PENDING&page&size`
`state` ∈ `PENDING, HIDDEN, RESOLVED`.
```json
[ { "review_id": 9921, "code": "RV-9921", "product_name": "Tai nghe ANC Pro 5", "reviewer_masked": "kh***92", "rating": 1, "content": "…", "report_count": 3, "top_reason": "SPAM", "reported_by_vendor": false, "state": "PENDING", "sentiment": { "label": "NEGATIVE", "score": 0.96 } } ]
```
`sentiment` (PhoBERT, UC-AI-07) = `null` khi chưa phân tích → nhãn "AI: Tiêu cực" khi `label = NEGATIVE`. Sắp xếp: `NEGATIVE` với `score ≥ 0,85` lên đầu, sau đó theo `report_count` — AI chỉ ưu tiên, không tự ẩn.

#### `PATCH /admin/reviews/{id}`
`{ "is_visible": false, "reason": "OFFENSIVE" }` ("Ẩn đánh giá") hoặc `{ "is_visible": true }` ("Giữ hiển thị" — đóng các báo cáo; "Hiện lại"). `200`. Ghi `system_logs REVIEW_HIDDEN`.

### 11.6 Xử lý rút tiền
UC-ADM-05 · `PROCESS_PAYOUT` · **Màn hình:** 3 chỉ số (Chờ xử lý, Có thể chi ngay, Bị chặn chờ KYC); bảng Mã / Người bán / Tài khoản nhận / Số tiền / KYC / Yêu cầu; Từ chối (lý do, hoàn ví) / Duyệt chi → modal chuyển khoản (VietQR, nội dung `TRWD…`, "Xác nhận tay" sau 24 giờ)

#### `GET /admin/withdrawals?status=PENDING&page&size`
```json
[ { "id": 12, "code": "PAYOUT-0012", "shop": { "id": 15, "name": "TechZone Official", "logo_url": null }, "bank_account_masked": "Vietcombank ••6712", "is_account_verified": true, "amount": 5000000, "shop_kyc_status": "VERIFIED", "requested_at": "2026-06-08T09:00:00+07:00", "status": "PENDING" } ]
```
`status` ∈ `PENDING, APPROVED, PROCESSING, COMPLETED, REJECTED`. Lệnh của shop `kyc ≠ VERIFIED` hiện "Chặn, hồ sơ KYC cần bổ sung" (không có nút duyệt). `summary`: `{ "pending_count": 4, "payable_amount": 8150000, "blocked_amount": 1200000 }`.

#### `POST /admin/withdrawals/{id}/approve` — Duyệt chi
`200`:
```json
{ "status": "APPROVED", "transfer": { "bank_name": "Vietcombank", "account_number": "0071000006712", "account_holder": "DANG QUOC HUY", "amount": 5000000, "transfer_content": "TRWD0012", "qr_payload": "00020101021238…" } }
```
`409 GNE-WAL-4092` khi shop chưa `VERIFIED`. Webhook SePay báo tiền ra với nội dung `TRWD0012` → tự `COMPLETED`.

#### `GET /admin/withdrawals/{id}/transfer` — Mở lại modal "Chờ chuyển khoản"
`200` khối `transfer`.

#### `POST /admin/withdrawals/{id}/reject`
`{ "reason_code": "ACCOUNT_MISMATCH", "note": null }` → `200` `{ "status": "REJECTED" }` — tiền trả về số dư khả dụng của shop.

#### `POST /admin/withdrawals/{id}/complete` — Xác nhận tay
`{ "bank_reference": "FT26170123456" }` → `200` `{ "status": "COMPLETED" }`. Chỉ khi `APPROVED/PROCESSING`.

### 11.7 Đối soát COD
UC-LOG-03, UC-PAY-05 · `PROCESS_PAYOUT` · **Màn hình:** danh sách phiên (mã, hãng, số tiền đã nhận, thời điểm, mã GD ngân hàng, trạng thái); chi tiết: "Tải bảng kê CSV" (3 cột mã vận đơn, số tiền, phí) / "Dùng bảng kê mẫu"; bảng đối chiếu Mã vận đơn / Đơn hàng / Bảng kê / Trên đơn / Phí / Kết quả (Khớp / Lệch số tiền / Không tìm thấy vận đơn); Xuất CSV; "Xác nhận đối soát"

#### `GET /admin/cod-remittances?status=WAITING&page&size`
```json
[ { "id": 20260612, "code": "COD-20260612", "carrier": "GHN", "received_amount": 1075000, "received_at": "2026-06-12T09:12:00+07:00", "bank_reference": "FT26163881021", "status": "WAITING" } ]
```
Một phiên = một khoản tiền thu hộ hãng vận chuyển chuyển về tài khoản sàn (SePay báo). `status` ∈ `WAITING, DONE`.

#### `GET /admin/cod-remittances/{id}` — Phiên đã xác nhận
`200` → phiên + `statement_file_name` + `lines` (như phản hồi preview bên dưới).

#### `POST /admin/cod-remittances/{id}/reconciliation-preview` — Đối chiếu bảng kê
Client đọc CSV (nhỏ, 3 cột) thành JSON rồi gửi — không upload tệp.
```json
{ "statement_file_name": "bang-ke-0612.csv", "lines": [ { "tracking_code": "VN931042698", "amount": 640000, "fee": 28000 } ] }
```
`200`:
```json
{
  "lines": [ { "tracking_code": "VN931042698", "order_code": "TR-20260608-000136", "statement_amount": 640000, "order_amount": 640000, "fee": 28000, "result": "MATCH" } ],
  "totals": { "statement_net": 1075000, "received_amount": 1075000, "matched_count": 2, "is_balanced": true }
}
```
`result` ∈ `MATCH, AMOUNT_MISMATCH, NOT_FOUND`. "Xác nhận đối soát" chỉ bật khi `is_balanced` và có ≥ 1 dòng khớp.

#### `POST /admin/cod-remittances/{id}/confirm` 🔁
Body như preview. `200` `{ "status": "DONE", "funded_order_count": 2 }` — các đơn COD khớp: ký quỹ `PENDING_FUNDING → HELD`, đơn `DELIVERED → PAID`, ghi `financial_ledger`. `422 GNE-LOG-4221` nếu không cân.

### 11.8 Cấu hình hoa hồng
UC-CAT-14 · `UPDATE_COMMISSION` · **Màn hình:** Phí hoa hồng mặc định (Lưu mặc định), Hoa hồng theo ngành hàng (tỷ lệ + công tắc, Lưu cấu hình). Thứ tự áp dụng: riêng của shop → theo ngành → mặc định.

#### `GET /admin/commission-settings`
```json
{ "default_rate": 5.0, "categories": [ { "category_id": 1, "name": "Điện tử", "rate": 5.0, "is_enabled": true } ] }
```
`is_enabled = false` → ngành dùng tỷ lệ mặc định.

#### `PATCH /admin/commission-settings`
`{ "default_rate": 5.0 }` hoặc `{ "categories": [ { "category_id": 3, "rate": 10.0, "is_enabled": true } ] }` → `200` như `GET`. Ghi `system_logs COMMISSION_UPDATED` (cũ → mới). Tỷ lệ 0–50.

### 11.9 Sổ cái tài chính
UC-PAY-06 · `VIEW_FINANCE` · **Màn hình:** "Append-only · bất biến", tìm mã tham chiếu / tài khoản, lọc sự kiện, bảng Seq / Thời gian / Sự kiện / Tham chiếu / Tài khoản / Nợ / Có, Xuất sổ cái

#### `GET /admin/ledger-entries?event_type&q&cursor&limit=50`
```json
[ { "seq": 100482, "created_at": "2026-06-08T14:22:07+07:00", "event_type": "ESCROW_IN", "reference": "TR-20260608-000141", "account": "ESCROW_LIABILITY", "debit": null, "credit": 778000 } ]
```
`event_type` ∈ `ESCROW_IN, COMMISSION, PLATFORM_VOUCHER, FREESHIP_COST, COIN_COST, ESCROW_RELEASE, PAYOUT_ACCRUE, PAYOUT_SETTLE, REFUND, GATEWAY_FEE`. `account` ∈ `ESCROW_LIABILITY, PLATFORM_REVENUE, SELLER_PAYABLE, PLATFORM_EXPENSE, CASH`.

#### `GET /admin/ledger-entries/export?event_type&q` → `text/csv` (`so-cai-tai-chinh.csv`)

### 11.10 Nhật ký hệ thống
UC-ADM-07 · `VIEW_AUDIT_LOG` · **Màn hình:** lọc thời gian (Hôm nay / 7 ngày / 30 ngày / Tuỳ chọn), nhóm (Đăng nhập / KYC / Tài chính / Kiểm duyệt / Bảo mật / Cấu hình), tìm User / IP / mã; bảng Thời gian / User / Vai trò / Hành động / Chi tiết / IP / Kết quả; Xuất CSV

#### `GET /admin/audit-logs?from&to&category&q&cursor&limit=50`
`category` ∈ `AUTH, KYC, FINANCE, MODERATION, SECURITY, CONFIG`.
```json
[ { "id": 55120, "created_at": "2026-06-22T09:42:11+07:00", "actor_email": "phat.nt@genie.example", "actor_role": "MODERATOR", "action": "KYC_APPROVED", "category": "KYC", "target": "KYC-1037 · TravelGear", "ip_address": "113.161.40.12", "result": "SUCCESS" } ]
```
`result` ∈ `SUCCESS, FAILED, DENIED` (`DENIED` = dòng `ACCESS_DENIED`); `actor_email = "system"` cho tác vụ định kỳ. `summary.denied_count` = số dòng `ACCESS_DENIED` trong bộ lọc. `id` là UUID (chuỗi).
- `from` / `to`: ISO-8601 hoặc ngày `YYYY-MM-DD` (giờ Việt Nam; `to` dạng ngày tính hết ngày đó, dạng thời điểm thì loại trừ). Mặc định 7 ngày gần nhất; **tối đa 90 ngày / lần** → vượt `422 GNE-VAL-4220` (`errors[].code = RANGE_TOO_LONG`). `q` ≤ 100 ký tự, tìm không phân biệt hoa thường trong email / IP / mã tham chiếu. `limit` 1–100 (mặc định 50).
- `actor_role`: vai trò chụp lúc ghi; riêng `STAFF` hiển thị **chức danh hiện tại** (`MODERATOR`…). `target` = `details.target_label` nếu có, không thì `reference_id`.

#### `GET /admin/audit-logs/export?…` → `text/csv` (`nhat-ky-he-thong.csv`)
Cùng bộ lọc (không `cursor` / `limit`). UTF-8 có BOM, ô bắt đầu bằng `= + - @` được thêm `'` (chống chèn công thức), giờ Việt Nam. **Tối đa 50.000 dòng** mới nhất — bị cắt thì có header `X-Truncated: true`. Giới hạn 5 lần / phút / người (`429 GNE-RAT-4290`).

### 11.11 Danh mục toàn sàn & bộ thuộc tính
UC-CAT-14, UC-CAT-15, UC-CAT-17 · `MANAGE_CATALOG` · **Màn hình:** 4 chỉ số (Danh mục gốc, Danh mục con, Tổng sản phẩm, Chờ duyệt), cây danh mục gốc, chi tiết (hiển thị, Bộ thuộc tính, Sửa; bảng danh mục con Sản phẩm / Hoa hồng / Xem SP), "Đề xuất danh mục từ người bán" (Duyệt / Từ chối có lý do), modal Thêm/Sửa danh mục (tên, slug + Auto, danh mục cha 3 cấp, icon, hoa hồng mặc định, Xóa khi rỗng), Bộ thuộc tính (Mã, Tên, Kiểu, Giá trị/Đơn vị, Bắt buộc, Lọc được, Trục biến thể, Ngừng dùng)

#### `GET /admin/categories`
```json
[
  {
    "id": 1, "name": "Điện tử", "slug": "dien-tu", "icon": "zap", "is_active": true, "commission_rate": 5.0, "product_count": 12840, "shop_count": 312,
    "children": [ { "id": 11, "name": "Điện thoại & Phụ kiện", "commission_rate": 4.0, "product_count": 4210, "children": [ { "id": 111, "name": "Điện thoại" } ] } ]
  }
]
```
`summary`: `{ "root_count": 6, "child_count": 18, "product_count": 72730, "pending_proposals": 2 }`.

| Method & Path | Body | Phản hồi |
|---|---|---|
| `POST /admin/categories` | `{ "name": "Thú cưng", "slug": "thu-cung", "parent_id": null, "icon": "heart", "commission_rate": 5.0, "is_active": true }` | `201`; `409 GNE-CAT-4091` trùng slug |
| `PATCH /admin/categories/{id}` | trường bất kỳ ở trên (công tắc "Hiển thị" = `{ "is_active": false }`) | `200` |
| `DELETE /admin/categories/{id}` | — | `204`; `409 GNE-CAT-4092` còn sản phẩm hoặc danh mục con |

#### Bộ thuộc tính
| Method & Path | Body | Phản hồi |
|---|---|---|
| `GET /admin/categories/{id}/attributes` | — | `[{ "code": "warranty", "name": "Bảo hành", "data_type": "ENUM", "values": ["6 tháng", "12 tháng"], "unit": null, "is_required": true, "is_filterable": true, "is_variant_axis": false, "is_active": true }]` |
| `POST /admin/categories/{id}/attributes` | `{ "code": "warranty", "name": "Bảo hành", "data_type": "ENUM", "values": [...], "unit": null, "is_required": true, "is_filterable": true, "is_variant_axis": false }` | `201`; `409 GNE-CAT-4095` trùng `code` trong ngành; `422 GNE-VAL-4220` `code` sai mẫu `^[a-z][a-z0-9_]*$` |
| `PUT /admin/categories/{id}/attributes/{code}` | như POST (trừ `code`) | `200`; thêm thuộc tính bắt buộc → phản hồi có `"grace_period_days": 14` (shop có 14 ngày bổ sung cho sản phẩm cũ) |
| `PATCH /admin/categories/{id}/attributes/{code}` | `{ "is_active": false }` ("Ngừng dùng" / "Dùng lại") | `200` |

`data_type` ∈ `TEXT, NUMBER, BOOLEAN, ENUM, MULTI_ENUM, DATE`.

#### Sản phẩm trong danh mục con (từ "Xem SP")
`GET /admin/products?category_id=11&tab=ALL&page&size` — `tab` ∈ `ALL, PENDING, PUBLISHED, HIDDEN_OR_LOCKED`.
```json
[ { "id": 1, "name": "Tai nghe Bluetooth ANC", "image_url": "...", "shop_name": "TechZone", "price": 389000, "stock": 38, "status": "PUBLISHED", "is_locked": false, "reason": null } ]
```
Duyệt / từ chối dùng 11.12. Khóa / mở khóa:

| Method & Path | Body |
|---|---|
| `POST /admin/products/{id}/lock` | `{ "reason_code": "POLICY_VIOLATION" }` → `HIDDEN` + khóa (shop không tự mở được) |
| `POST /admin/products/{id}/unlock` | — → `PUBLISHED` |

#### Đề xuất danh mục
| Method & Path | Body | Phản hồi |
|---|---|---|
| `GET /admin/category-proposals?status=PENDING` | — | `[{ "id": 81, "name": "Thú cưng (Pet)", "shop": { "id": 40, "name": "PetWorld VN" }, "parent": null, "reason": "…", "created_at": "..." }]` |
| `POST /admin/category-proposals/{id}/approve` | `{ "commission_rate": 5.0 }` (tùy chọn) | `200` `{ "category": { "id": 7, "name": "Thú cưng (Pet)" } }` — tạo danh mục dưới `parent` |
| `POST /admin/category-proposals/{id}/reject` | `{ "reason": "Đã có danh mục tương tự" }` (bắt buộc) | `200`; lý do gửi tới shop |

### 11.12 Duyệt sản phẩm
UC-CAT-12, UC-CAT-13 · `MANAGE_CATALOG` · **Màn hình:** tab Chờ duyệt / Đã duyệt / Không duyệt / Cấu hình nhân viên; ô tìm sản phẩm hoặc nhà bán; bảng Sản phẩm / Nhà bán / Trạng thái (Chờ duyệt, AI nghi ngờ, Chờ duyệt lại, Đã duyệt, Từ chối) / Người duyệt (chọn để giao lại; "AI Hệ thống") / Kiểm duyệt. Trang chi tiết: dòng Gemini (quyết định, điểm rủi ro, vi phạm), lý do AI, ảnh, Hồ sơ pháp lý sản phẩm (Xem trực tiếp), thông tin cơ bản, mô tả, vận chuyển & kích thước, bảng SKU; thanh quyết định (lý do từ chối + ghi chú, Từ chối, Duyệt lên sàn, Chờ duyệt lại)

#### `GET /admin/product-moderation?tab=PENDING&q&page&size`
`tab` ∈ `PENDING` (`PENDING_APPROVAL` + `FLAGGED`, `FLAGGED` lên đầu), `PUBLISHED`, `REJECTED`.
```json
[ { "product_id": 1283, "name": "Nước hoa mini chính hãng 10ml", "image_url": "...", "shop": { "id": 12, "name": "Beauty Box" }, "status": "FLAGGED", "is_requeued": false, "assignee": { "id": 8, "name": "Nguyễn Thảo Vy" }, "rejected_by_ai": false, "submitted_at": "2026-06-08T11:05:00+07:00" } ]
```
`summary.counts` theo tab.

#### `GET /admin/product-moderation/{product_id}`
```json
{
  "product_id": 1283, "name": "Nước hoa mini chính hãng 10ml", "status": "FLAGGED", "is_requeued": false, "submitted_at": "...",
  "shop": { "id": 12, "name": "Beauty Box" },
  "ai_result": { "decision": "NEED_REVIEW", "risk_score": 62, "violations": ["COUNTERFEIT"], "reasoning": "Ảnh sản phẩm trùng logo thương hiệu lớn nhưng giấy ủy quyền chưa khớp tên shop." },
  "decision_reason": null,
  "images": ["..."],
  "documents": [ { "id": 52, "file_name": "giay-uy-quyen.pdf", "url": "https://s3.genie.vn/product-docs/...?X-Amz-Expires=300" } ],
  "category_path": "Làm đẹp > Nước hoa",
  "brand": "Eau de Parfum", "condition": "NEW", "origin": "Pháp",
  "description_html": "<p>Nước hoa mini 10ml…</p>",
  "weight_grams": 80, "dimensions_cm": { "length": 4, "width": 4, "height": 10 }, "ships_from": "TP. HCM", "shipping_payer": "SELLER",
  "variants": [ { "sku": "PF-10", "name": "10ml", "price": 129000, "stock": 300 } ]
}
```
`ai_result.decision` ∈ `APPROVE, REJECT, NEED_REVIEW`. `decision_reason` = lý do từ chối đã lưu (AI hoặc người).

| Method & Path | Body | Kết quả |
|---|---|---|
| `POST /admin/products/{id}/approve` | — | `PUBLISHED`, thông báo shop `PRODUCT_APPROVED` |
| `POST /admin/products/{id}/reject` | `{ "reason_code": "MISSING_SAFETY_CERT", "note": "Bổ sung giấy hợp quy" }` | `REJECTED`, thông báo shop kèm lý do |
| `POST /admin/products/{id}/requeue` | — | `PENDING_APPROVAL` + `is_requeued = true` ("Chờ duyệt lại") |
| `PUT /admin/products/{id}/assignee` | `{ "staff_id": 8 }` | Giao lại người duyệt; `200` |

Ghi `system_logs PRODUCT_MODERATED`.

#### Cấu hình nhân viên duyệt
| Method & Path | Body | Phản hồi |
|---|---|---|
| `GET /admin/moderators` | — | `[{ "staff_id": 7, "name": "Lê Minh Tú", "is_accepting": true }]` |
| `PUT /admin/moderators/availability` | `{ "items": [ { "staff_id": 9, "is_accepting": false } ] }` ("Lưu cấu hình") | `200` |
| `POST /admin/moderators/redistribute` | — ("Phân phối lại" — chia đều hàng chờ cho người đang nhận việc) | `200` `{ "reassigned_count": 3 }` |

### 11.13 Người dùng
UC-ADM-03 · `MANAGE_USER` · **Màn hình:** tab Tất cả / Khách hàng / Shop / Đang khóa; tìm email, SĐT, tên shop; bảng Tài khoản / Loại / SĐT / Số đơn / Cảnh báo / Trạng thái (+ lý do · thời hạn); Khóa tài khoản (lý do, thời hạn 7 ngày / 30 ngày / Vô thời hạn), Mở khóa (lý do bắt buộc)

#### `GET /admin/users?tab=ALL&q&page&size`
`tab` ∈ `ALL, CUSTOMER, VENDOR, LOCKED`.
```json
[ { "id": 20377, "code": "U-20377", "full_name": "Lê Hoàng Phúc", "shop_name": null, "email": "phuc.le@yahoo.com", "phone_number": "0978112340", "role": "CUSTOMER", "order_count": 31, "warning_count": 3, "status": "SUSPENDED", "suspension": { "reason": "Hoàn hàng bất thường nhiều lần", "until": "2026-06-29T00:00:00+07:00" } } ]
```
`status` ∈ `ACTIVE, LOCKED, SUSPENDED`; `suspension.until = null` = vô thời hạn. Không liệt kê `STAFF/ADMIN` (quản lý ở 11.18). `summary.counts` theo tab.

#### `POST /admin/users/{id}/suspend`
`{ "reason_code": "ABNORMAL_RETURNS", "note": null, "duration": "P7D" }` — `duration` ∈ `P7D, P30D, null` (vô thời hạn). `200`. Thu hồi mọi phiên; ghi `system_logs ACCOUNT_SUSPENDED`.

#### `POST /admin/users/{id}/reactivate`
`{ "reason": "Đã xác minh khiếu nại" }` → `200`. `ACCOUNT_REACTIVATED`.

### 11.14 Voucher sàn
UC-PRO-02 · `MANAGE_VOUCHER` · **Màn hình:** 5 chỉ số (Đang chạy, Lượt dùng hệ thống, Tổng giảm giá đã dùng, Sắp ra mắt, Ngân sách còn lại); tab trạng thái; bảng Mã / Loại / Giá trị / Đơn tối thiểu / Giảm tối đa / Sử dụng / Thời gian / Nhà tài trợ / Trạng thái; Nhân bản / Sửa / Tắt / Huỷ / Xoá vĩnh viễn; modal "Tạo Voucher Sàn"

#### `GET /admin/vouchers?status&page&size`
`status` ∈ `ACTIVE, SCHEDULED, ENDED, DISABLED`.
```json
[ { "id": 1, "title": "Siêu Sale Giữa Tháng", "code": "GENIE50", "discount_type": "PERCENTAGE", "discount_value": 50, "min_order_value": 500000, "max_discount": 100000, "usage": { "used": 3420, "limit": 10000 }, "usage_per_user": 1, "valid_from": "...", "valid_to": "...", "funded_by": "PLATFORM", "status": "ACTIVE" } ]
```
`summary`: `{ "active_count": 2, "total_usage": 27320, "total_discount_used": 456800000, "scheduled_count": 1, "remaining_budget": 543200000 }`.

#### `GET /admin/vouchers/{id}` → `$PlatformVoucherInput` + `id`, `status`, `usage`

#### `POST /admin/vouchers` · `PUT /admin/vouchers/{id}`
`$PlatformVoucherInput`:
```json
{
  "title": "Siêu Sale Giữa Tháng",
  "code": "GENIE50",
  "discount_type": "PERCENTAGE",
  "discount_value": 50,
  "max_discount": 100000,
  "min_order_value": 500000,
  "usage_limit": 10000,
  "usage_per_user": 1,
  "budget": 500000000,
  "valid_from": "2026-06-15T00:00:00+07:00",
  "valid_to": "2026-06-30T23:59:59+07:00",
  "scope": "ALL",
  "category_ids": [],
  "excluded_category_ids": [1],
  "funded_by": "CO_SPONSOR",
  "platform_share_percent": 50
}
```
- `discount_type = FREESHIP` → voucher `type = FREESHIP`; còn lại `type = PLATFORM`.
- `scope` ∈ `ALL, CATEGORY`. `funded_by` ∈ `PLATFORM, CO_SPONSOR` (`platform_share_percent` bắt buộc khi `CO_SPONSOR`).
- `budget` hết → voucher tự tắt.
- `valid_from` không ở quá khứ khi tạo mới; `valid_to > valid_from`. `409 GNE-VCH-4092` trùng mã.

#### `PATCH /admin/vouchers/{id}` — Tắt / Huỷ lịch
`{ "is_active": false }` → `200`.

#### `DELETE /admin/vouchers/{id}` — Xoá vĩnh viễn
Chỉ khi `ENDED` và `usage.used = 0`. `204`; `409 GNE-VCH-4093` nếu đã có lượt dùng.

### 11.15 Flash Sale
UC-PRO-05 · `MANAGE_VOUCHER` · **Màn hình:** danh sách chiến dịch + trạng thái (Nháp / Đã lên lịch / Đang chạy / Đã kết thúc); chi tiết chiến dịch (khung giờ, tối đa SP/shop, hạn đăng ký; bảng sản phẩm Shop / Giá gốc 30 ngày / Giá Flash / Suất / Duyệt – Từ chối); Lên lịch (cần ≥ 1 SP đã duyệt) / Kết thúc sớm; modal "Tạo chiến dịch"

| Method & Path | Body | Phản hồi |
|---|---|---|
| `GET /admin/flash-sales` | — | `[{ "id": 2103, "name": "FLASH SALE 12.12", "starts_at": "...", "ends_at": "...", "status": "DRAFT", "item_count": 3 }]` |
| `GET /admin/flash-sales/{id}` | — | chiến dịch + `max_items_per_shop`, `registration_deadline`, `items[]` (dưới) |
| `POST /admin/flash-sales` | `{ "name": "FLASH SALE 12.12", "starts_at": "...", "ends_at": "...", "max_items_per_shop": 20, "registration_deadline": "2026-12-10" }` | `201` (`DRAFT`) |
| `POST /admin/flash-sales/{id}/schedule` | — | `SCHEDULED`; `422 GNE-PRO-4224` chưa có SP duyệt |
| `POST /admin/flash-sales/{id}/end` | — | `ENDED` (kết thúc sớm khi `ACTIVE`) |
| `PATCH /admin/flash-sales/{id}/items/{item_id}` | `{ "status": "APPROVED" }` / `REJECTED` | `200`; chỉ khi chiến dịch `DRAFT`; `422 GNE-PRO-4221` khi giá Flash không thấp hơn ≥ 15% |

`items[]`:
```json
{ "id": 7, "product_id": 1, "product_name": "Tai nghe ANC Pro 5", "shop_name": "TechZone Official", "reference_price": 389000, "flash_price": 299000, "flash_stock": 50, "status": "PENDING", "meets_discount_rule": true }
```
`meets_discount_rule = false` → nút "Duyệt" bị khóa. Mở/đóng đúng giờ do cron (1 phút).

### 11.16 Banner trang chủ
UC-ADM-12 · `MANAGE_VOUCHER` · **Màn hình:** lọc vị trí, tìm; bảng Banner / Vị trí / Thời gian / Hiển thị – Bấm / CTR / Trạng thái (Đang hiển thị, Đã lên lịch, Đã kết thúc, Đã tắt); Xem trên storefront / Sửa / Tắt ngay / Bật lại; modal banner (tiêu đề, ảnh desktop + mobile, vị trí, danh mục khi "Đầu trang danh mục", thứ tự, thời gian, liên kết đích chọn từ dữ liệu sàn: danh mục / sản phẩm / cửa hàng / Flash Sale / Gợi ý hôm nay / trang chiến dịch)

#### `GET /admin/banners?position&q&page&size`
```json
[ { "id": 115, "title": "Siêu sale 12.12", "image_url": "...", "mobile_image_url": null, "target_url": "/sieu-sale-12-12", "position": "HOME_TOP_CAROUSEL", "category": null, "sort_order": 5, "starts_at": "2026-11-01T00:00:00+07:00", "ends_at": "2026-12-12T23:59:59+07:00", "is_active": true, "state": "LIVE", "impressions": 12800, "clicks": 940 } ]
```
`state` ∈ `LIVE, SCHEDULED, ENDED, OFF` (server tính). CTR = `clicks / impressions`, client chia.

#### `POST /admin/banners` · `PUT /admin/banners/{id}`
```json
{ "title": "Siêu sale 12.12", "image_object_key": "tmp/content-assets/...jpg", "mobile_image_object_key": null, "target_url": "/sieu-sale-12-12", "position": "CATEGORY_TOP", "category_id": 1, "sort_order": 1, "starts_at": "...", "ends_at": "...", "is_active": true }
```
Sửa mà không đổi ảnh → bỏ `image_object_key`; `mobile_image_object_key`: bỏ / `null` = giữ, `""` = bỏ ảnh mobile. `image_object_key` là khóa `tmp/…` do `POST /uploads/presigned-urls` (`purpose = BANNER_IMAGE`, quyền `MANAGE_VOUCHER`) cấp; server chuyển ảnh sang `content-assets/banner/{id}/…`, phản hồi trả URL. Lỗi thiếu trường: `422 GNE-VAL-4220` với `errors[].field` (`title`, `image_object_key`, `target_url`, `position`, `category_id`, `sort_order`, `starts_at`, `ends_at`). Danh sách phân trang số (`page` từ 1, `size` ≤ 60); `category` = `{id, name}`. Mọi thay đổi ghi `system_logs` `BANNER_CHANGED`.

| Lỗi | Khi nào |
|---|---|
| `422 GNE-BNR-4221` | Vị trí đã đủ 8 banner đang hiển thị — đếm banner **bật** khác cùng vị trí (cùng `category_id` với `CATEGORY_TOP`) có khung giờ **giao** với banner đang lưu và chưa kết thúc; bật lại (`PATCH`) cũng kiểm |
| `422 GNE-BNR-4222` | `target_url` ngoài miền sàn |
| `422 GNE-VAL-4220` | Thiếu ảnh / thời gian / `category_id` khi `CATEGORY_TOP` |

#### `PATCH /admin/banners/{id}` — Tắt ngay / Bật lại
`{ "is_active": false }` → `200`.

#### `DELETE /admin/banners/{id}` — Xóa
Chỉ khi banner `OFF` hoặc `ENDED` (nút "Xóa" chỉ hiện ở hai trạng thái này). `204`; `409 GNE-BNR-4091` khi đang `LIVE` / `SCHEDULED`.

Ô chọn đích: danh mục `GET /public/categories`, sản phẩm `GET /public/products?q&shop_id`, cửa hàng `GET /admin/shops?q&limit=20` → `[{ "id": 15, "name": "TechZone Official" }]` (quyền `MANAGE_VOUCHER`).

### 11.17 Thông báo sàn
UC-NOT-05 · `MANAGE_PLATFORM_NOTIFICATION` · **Màn hình:** 3 chỉ số (Đã gửi tháng này, Lượt tiếp cận, Tổng lượt đọc); tìm tiêu đề/nội dung, lọc đối tượng; bảng Mã TB / Thông báo / Ảnh / Đối tượng / Thời gian / Đã đọc (%) hoặc "Đã hẹn giờ" (5 dòng/trang); modal chi tiết (Sửa / Xóa); modal soạn (đối tượng, email người nhận cụ thể, tiêu đề ≤ 100, nội dung rich-text, ảnh, hẹn giờ) → xác nhận phát

#### `GET /admin/platform-notifications?target&q&page&size=5`
```json
[ { "id": 1048, "code": "PN-1048", "title": "Siêu sale 12.12 giảm tới 50%", "content_preview": "Chương trình Siêu sale 12.12 bắt đầu từ 00:00…", "image_url": "...", "target": "CUSTOMER", "created_by": "Admin (1)", "send_at": "2026-06-08T09:00:00+07:00", "is_scheduled": false, "read_count": 48210, "reach_count": 120400 } ]
```
`target` ∈ `ALL, CUSTOMER, VENDOR, SPECIFIC_USERS`. `summary`: `{ "sent_this_month": 6, "total_reach": 834320, "total_reads": 222390 }`.

#### `GET /admin/platform-notifications/{id}` → như trên + `content_html`, `recipient_emails` (khi `SPECIFIC_USERS`)

#### `POST /admin/platform-notifications` · `PUT /admin/platform-notifications/{id}`
```json
{ "title": "Nâng cấp hệ thống 02:00 – 04:00", "content_html": "<p>Một số dịch vụ…</p>", "image_object_key": null, "target": "SPECIFIC_USERS", "recipient_emails": ["mai.tran@gmail.com"], "send_at": "2026-06-10T02:00:00+07:00" }
```
`send_at = null` → phát ngay (WebSocket `/topic/platform` + hộp thông báo). `201` / `200`. `PUT` chỉ khi chưa phát (`409 GNE-NOT-4091` nếu đã phát). `422 GNE-NOT-4221` khi `recipient_emails` rỗng với `SPECIFIC_USERS` hoặc email không tồn tại (`details.unknown_emails`).

#### `DELETE /admin/platform-notifications/{id}` → `204`

### 11.18 Nhân viên & phân quyền
UC-ADM-01, UC-ADM-02, UC-ADM-03 · `MANAGE_RBAC` (chỉ Admin) · **Màn hình:** "5 nhân viên · 3 chức danh + Admin · 13 mã quyền"; tìm tên/email; bảng Nhân viên / Chức danh / Quyền (di chuột xem mô tả) / Trạng thái (Hoạt động, Tạm khoá, Chờ kích hoạt); Gửi lại lời mời / Sửa quyền / Khóa (lý do) / Mở khóa / Xóa; modal "Mời nhân viên mới" (họ tên, chức danh, email `@genie.example`, quyền theo chức danh có thể chỉnh); modal "Chỉnh quyền" (chức danh nạp bộ mặc định, 13 quyền, cảnh báo tách biệt nhiệm vụ PROCESS_PAYOUT + UPDATE_COMMISSION, tạm khoá / mở khoá)

#### `GET /admin/permission-catalog`
```json
{
  "permissions": [ { "code": "VIEW_DASHBOARD", "description": "Xem trang Tổng quan" } ],
  "title_presets": { "MODERATOR": ["VIEW_DASHBOARD", "REVIEW_KYC", "MANAGE_CATALOG", "MODERATE_REVIEW"], "CS_SPECIALIST": ["VIEW_DASHBOARD", "MODERATE_REVIEW", "HANDLE_DISPUTE"], "FINANCE_OFFICER": ["VIEW_DASHBOARD", "PROCESS_PAYOUT", "UPDATE_COMMISSION", "VIEW_FINANCE", "MANAGE_VOUCHER"] }
}
```

#### `GET /admin/staff?q&page&size`
```json
[ { "id": 7, "full_name": "Ngô Tấn Phát", "email": "phat.nt@genie.example", "role": "STAFF", "staff_title": "MODERATOR", "permissions": ["VIEW_DASHBOARD", "REVIEW_KYC", "MANAGE_CATALOG", "MODERATE_REVIEW"], "status": "ACTIVE", "is_self": false } ]
```
`staff_title` ∈ `MODERATOR, CS_SPECIALIST, FINANCE_OFFICER, CUSTOM` (`null` với Admin — giao diện hiện "Toàn quyền"). `status` ∈ `ACTIVE, LOCKED, PENDING_ACTIVATION`.

| Method & Path | Body | Phản hồi |
|---|---|---|
| `POST /admin/staff/invitations` | `{ "full_name": "Hồ Thanh Trúc", "email": "truc.ht@genie.example", "staff_title": "CS_SPECIALIST", "permissions": [...] }` | `201` nhân viên `PENDING_ACTIVATION`; `409 GNE-ADM-4091` email đã dùng; `422 GNE-ADM-4221` sai đuôi `@genie.example` |
| `POST /admin/staff/{id}/invitations/resend` | — | `202` |
| `PUT /admin/staff/{id}/permissions` | `{ "staff_title": "CUSTOM", "permissions": [...] }` | `200` + `warnings: ["SEGREGATION_OF_DUTIES"]` khi có cả `PROCESS_PAYOUT` và `UPDATE_COMMISSION`; `403 GNE-SEC-4031` khi cấp `MANAGE_RBAC`. Thu hồi refresh token của nhân viên (`PermissionsChangedEvent`) |
| `POST /admin/staff/{id}/lock` | `{ "reason": "Nhân viên đã chấm dứt hợp đồng" }` | `200` |
| `POST /admin/staff/{id}/unlock` | — | `200` |
| `DELETE /admin/staff/{id}` | — | `204`; `409 GNE-ADM-4092` khi xóa chính mình |

Ghi `system_logs STAFF_INVITED / PERMISSION_GRANTED / PERMISSION_REVOKED / RBAC_ROLE_CHANGED / ACCOUNT_LOCKED`.

---

## 12. Webhook & API nội bộ

Không có màn hình nhưng bắt buộc để các màn hình thanh toán, giao hàng, KYC, cảm xúc đánh giá chạy. Mọi webhook **idempotent** theo mã giao dịch, luôn trả 2xx cho bên gọi khi đã xử lý (kể cả trùng).

| Method & Path | Bên gọi | Xác thực | Việc làm |
|---|---|---|---|
| `GET /payments/vnpay/return` | Trình duyệt khách (VNPay chuyển về) | `vnp_SecureHash` HMAC-SHA512 | Chỉ kiểm chữ ký rồi `302` về `{return_url}?session_id=…&result=success|failed` (trạng thái thật lấy từ IPN) |
| `GET /payments/vnpay/ipn` | VNPay | HMAC-SHA512 | Xác nhận thanh toán → đơn `PAID`, ký quỹ `HELD`. Trả `{"RspCode":"00"}`; sai chữ ký `97`; đã xử lý `02`; lệch tiền `04` (LLD SD-03) |
| `POST /payments/sepay/webhook` | SePay | Header `Authorization: Apikey …` | Tiền vào nội dung `TR…` → thanh toán VietQR; tiền vào từ hãng vận chuyển → tạo phiên `cod-remittances`; tiền ra `TRWD…` → lệnh rút `COMPLETED`; tiền ra `TRRF…` → hoàn tiền `SUCCESS`. Body theo định dạng SePay (camelCase: `id`, `gateway`, `transactionDate`, `accountNumber`, `code`, `content`, `transferType` `in/out`, `transferAmount`, `accumulated`, `subAccount`, `referenceCode`, `description`), idempotent theo `id`; trả `200 { "success": true }` |
| `POST /shipping/{carrier}/webhook/{secret}` | GHN, GHTK, ViettelPost, J&T | `secret` trên URL + IP allowlist | `carrier` ∈ `ghn, ghtk, viettel_post, jnt_express`; body là payload gốc của hãng (adapter chuẩn hóa). Ghi `shipment_events`, cập nhật đơn (`DELIVERING`, `DELIVERED`, `DELIVERY_FAILED`, `RETURNING`…); trả `200` `{ "received": true }` |
| `PATCH /internal/kyc/{id}/result` | AI Worker | `X-Internal-Token` | Body `{ overall: PASS\|PARTIAL\|FAIL, checks: {qr_code, national_emblem, mrz, fingerprint, face_match}, face_match_score, liveness_passed, risk_score, errors[], model }` (cùng cấu trúc `ai_result` ở 11.3) → tự duyệt, chuyển duyệt tay hoặc từ chối; trả `200 { "kyc_status" }` |
| `PATCH /internal/reviews/{id}/sentiment` | AI Worker | `X-Internal-Token` | `{ "label": "NEGATIVE", "score": 0.91, "model": "phobert-genie-v1" }` → `aspect_sentiment_json.overall` |
| `GET /public/health` | Uptime Kuma | — | `{ "status": "UP" }` — trả nguyên văn, **không** bọc phong bì §0.3 |

### 12.2 Tác vụ nền & email tự động (không có endpoint)

Các use case hệ thống không có API gọi từ client nhưng làm thay đổi dữ liệu mà màn hình đọc qua các endpoint ở trên. Chạy bằng `@Scheduled` + `SELECT … FOR UPDATE SKIP LOCKED` (LLD mục 10).

| Tác vụ | Chu kỳ | UC | Kết quả thấy ở API / màn hình |
|---|---|---|---|
| Hủy phiên / đơn quá hạn thanh toán, nhả tồn, hoàn voucher & Điểm | 60 giây | **UC-ORD-08** | Đơn `AUTO_CANCELLED` ("Tự huỷ do quá hạn") ở `GET /me/orders`; phiên `EXPIRED` ở `GET /me/checkout-sessions/{id}`; ô VietQR "Mã QR đã hết hạn" |
| Tự hoàn tất đơn `DELIVERED` quá 7 ngày không khiếu nại | 1 giờ | **UC-ORD-12** | Đơn `COMPLETED`, `return_window_closed = true` |
| Giải ngân ký quỹ đơn `COMPLETED` (bỏ qua ký quỹ `FROZEN` / `DISPUTED`) | 15 phút | **UC-PAY-07** | Dòng `RELEASE` ở `GET /seller/wallet/transactions`, `available_balance` tăng; bút toán `ESCROW_RELEASE` + `PAYOUT_ACCRUE` ở `GET /admin/ledger-entries` |
| Tự xác nhận đơn shop chưa xử lý sau 24 giờ | 15 phút | UC-LOG-16 | `health_30d.auto_confirmed_orders` ở Phân tích |
| Hạn phản hồi trả hàng (shop 48 giờ → tự đồng ý; khách quá hạn → đóng) | 15 phút | UC-SUP-02, 03 | `status` của `refund_request` |
| Mở / đóng Flash Sale đúng giờ | 1 phút | UC-PRO-05 | `GET /public/flash-sales/current`, trạng thái chiến dịch Admin |
| Phát thông báo sàn hẹn giờ (`send_at`) | 1 phút | UC-NOT-05 | Thông báo xuất hiện ở `GET /me/notifications` + WS `/topic/platform` |
| Hết hạn yêu cầu bổ sung KYC 7 ngày → `REJECTED` | Hằng ngày | UC-VND-04 | `GET /seller/kyc` |
| Hết hạn Điểm 12 tháng (FEFO) | Hằng ngày | UC-PRO-09 | `GET /me/coins` (`expiring_soon`), dòng âm ở lịch sử Điểm |
| Kết thúc chế độ nghỉ tại `ends_at` | Hằng giờ | UC-VND-19 | `vacation = null` ở trang Shop / Cài đặt |
| Ẩn danh hóa tài khoản sau 30 ngày (`AnonymizeAccountsJob` — đã hiện thực) | 02:30 hằng ngày (giờ VN) | UC-AUTH-10 | Tài khoản `ANONYMIZED`, không đăng nhập được; email cũ đăng ký lại được; ảnh đại diện xóa khỏi Silo |
| Tính chỉ số shop (đánh giá, tỉ lệ & thời gian phản hồi chat, "Shop Yêu Thích") | Hằng giờ | UC-CAT-16 | `GET /public/shops/{id}` |
| Ghi gộp lượt xem / từ khóa / hiển thị–bấm banner từ Redis | 5 phút | UC-CAT-04, 06, ADM-12 | `GET /seller/analytics/*`, `GET /public/search/trending`, cột Hiển thị / Bấm ở Banner |
| Chạy lại phân tích cảm xúc chưa có kết quả | 15 phút | UC-AI-07 | `GET /seller/analytics/sentiment`, `sentiment` ở Kiểm duyệt đánh giá |
| Đồng bộ trạng thái 3PL khi lỡ webhook | 30 phút | UC-LOG-03 | `shipment.events` ở `GET /me/orders/{id}` |
| Đối soát ví / sổ cái, báo `ESCROW_DISCREPANCY` | Hằng đêm | UC-PAY-06 | Thông báo Admin ở `GET /me/notifications` |
| Gửi lại email lỗi (tối đa 3 lần) | 5 phút | UC-NOT-03 | — |

**Email tự động (UC-NOT-03)** — gửi bất đồng bộ qua SMTP sau khi transaction commit, không có endpoint:

| Sự kiện | Người nhận | Nội dung |
|---|---|---|
| Mã OTP (đăng ký, đăng nhập, quên mật khẩu, mở khóa, đổi email, thao tác 🔐) | Người dùng | Mã 6 số, hạn 5 phút |
| Đặt hàng thành công / thanh toán thành công / thất bại | Khách | Mã phiên, từng đơn con, tổng tiền |
| Shop từ chối / hủy đơn, đơn tự hủy quá hạn | Khách | Lý do, tiền / Điểm / voucher đã hoàn |
| Có đơn mới cần xác nhận | Shop | Mã đơn, hạn 24 giờ |
| Kết quả KYC (duyệt / từ chối / yêu cầu bổ sung) | Shop | Lý do, hạn 7 ngày |
| Kết quả duyệt sản phẩm | Shop | Lý do từ chối |
| Yêu cầu trả hàng mới / kết quả phân xử / đã hoàn tiền | Khách, shop | Mã yêu cầu, số tiền, hạn phản hồi |
| Lệnh rút tiền được duyệt / từ chối / hoàn tất | Shop | Số tiền, tài khoản nhận (che số) |
| Đăng nhập thiết bị lạ, đổi mật khẩu, đổi email (gửi về email cũ), yêu cầu xóa tài khoản | Người dùng | Thời điểm, thiết bị, link hủy |
| Lời mời nhân viên | Nhân viên mới | Link kích hoạt (`POST /auth/staff-activations`) |

---

## 13. Realtime — WebSocket (STOMP)

Endpoint `wss://api.genie.vn/ws`, header `Authorization: Bearer …` khi `CONNECT`. Swagger không mô tả được STOMP — Apidog hỗ trợ tab WebSocket, hoặc ghi chú như dưới.

| Kênh | Hướng | Dữ liệu | Màn hình cập nhật |
|---|---|---|---|
| `/user/queue/notifications` | server → client | thông báo mới (dạng 3.12) + `unread_count` | Chuông header (Storefront, Seller, Admin) |
| `/user/queue/chat` | server → client | `{ "conversation_id", "message": {…8.3}, "unread_total" }`; sự kiện `MESSAGE_DELIVERED`, `MESSAGE_SEEN`, `TYPING` | Khung chat, badge nút chat, badge nav Chat (Seller) |
| `/user/queue/payments` | server → client | `{ "checkout_session_id", "status": "COMPLETED" }` | Ô VietQR tự chuyển trang "Thanh toán thành công" |
| `/user/queue/counters` | server → client | `{ "cart_items", "unread_notifications", "orders_to_confirm" }` | Badge giỏ, badge "Đơn hàng" Seller |
| `/user/queue/admin-queues` | server → client | như `GET /admin/queues` | Badge nav Admin, "Việc cần xử lý" |
| `/topic/platform` | server → client | thông báo sàn vừa phát | Chuông mọi người dùng |
| `/app/chat.send` | client → server | như body 8.4 + `conversation_id`, `client_message_id` | — |
| `/app/chat.read` | client → server | `{ "conversation_id" }` | — |
| `/app/chat.typing` | client → server | `{ "conversation_id" }` | Dấu "…" đang gõ |

---

## 14. Bản đồ Màn hình → API

### 14.1 Storefront

| Màn hình | API (theo thứ tự gọi) |
|---|---|
| Khung chung (header, chuông, mini cart, chat nổi) | `GET /me`, `GET /me/wishlist/product-ids`, `GET /me/following/shop-ids`, `GET /public/categories`; hover: `GET /me/notifications?limit=5`, `GET /me/cart/items?limit=5`; bấm vào ô tìm: `GET /public/search/trending`; gõ tìm: `GET /public/search/suggestions`; WS |
| Trang chủ | `GET /public/banners?positions=HOME_TOP_CAROUSEL,HOME_TOP_SIDE,HOME_MIDDLE,FLASH_SALE_TOP,POPUP`, `GET /public/recommendations?size=36`, `GET /public/flash-sales/current?limit=6`, `GET /public/vouchers?featured=true&limit=4`, `GET /public/shops?featured=true&limit=6`, `POST /public/banner-events` |
| Gợi ý hôm nay | `GET /public/recommendations?page=2&size=30` |
| Trang danh mục | `GET /public/categories/{slug}`, `GET /public/banners?positions=CATEGORY_TOP&category_id=`, `GET /public/products?category_id=&include_facets=true` |
| Tìm kiếm (toàn sàn / trong shop) | `GET /public/products?q=&include_facets=true[&shop_id=]`; rỗng: `GET /public/products?sort=BEST_SELLING&size=5` |
| Trang Shop | `GET /public/shops/{id}`, `GET /public/vouchers?shop_id=`, `GET /public/products?shop_id=&sort=`, `PUT/DELETE /me/following/{id}`, `POST /me/vouchers`, `POST /me/conversations` |
| Chi tiết sản phẩm | `GET /public/products/{id}`, `POST /public/products/{id}/views`, `GET /public/products/{id}/reviews`, `GET /public/products/{id}/similar`, `POST /me/cart/items`, `PUT/DELETE /me/wishlist/{id}`, `PUT/DELETE /me/following/{shop_id}`, `PUT/DELETE /reviews/{id}/helpful-vote`, `POST /reviews/{id}/reports` |
| Trang chiến dịch | `GET /public/vouchers?code=SALE1212`, `GET /public/products?discount_min=45` |
| Giỏ hàng | `GET /me/cart`, `POST /me/checkout/preview`, `PATCH /me/cart/items/{id}`, `DELETE /me/cart/items?ids=`, `GET /public/products/{id}/variants`, `POST /me/checkout/applicable-vouchers` |
| Thanh toán | `POST /me/checkout/preview`, `GET /me/addresses`, `POST /me/checkout/applicable-vouchers`, `POST /me/checkout-sessions` |
| VNPay / VietQR / kết quả | `GET /me/checkout-sessions/{id}` (poll hoặc WS `/user/queue/payments`), `POST /me/checkout-sessions/{id}/payments` |
| Đặt hàng thành công / thất bại | `GET /me/checkout-sessions/{id}` |
| Tài khoản › Đơn mua | `GET /me/orders?status_group=`, `POST /me/orders/{id}/cancel`, `POST /me/checkout-sessions/{id}/cancel`, `POST /me/checkout-sessions/{id}/payments`, `POST /me/orders/{id}/confirm-receipt`, `POST /me/cart/items/batch` |
| Theo dõi đơn | `GET /me/orders/{id}`, `POST /me/orders/{id}/confirm-receipt`, `POST /me/refund-requests`, `POST /me/refund-requests/{id}/evidence|escalate|accept-rejection`, `GET /public/banks`, `POST /uploads/presigned-urls` |
| Đánh giá sản phẩm (modal) | `POST /uploads/presigned-urls`, `POST /me/reviews` |
| Tài khoản › Hồ sơ | `GET /me/profile`, `PATCH /me/profile`, `POST /me/email-change-requests`, `POST /me/email-change-requests/{id}/confirm`, `POST /me/otp-challenges {ACCOUNT_DELETION}`, `PUT/DELETE /me/deletion-request` |
| Tài khoản › Địa chỉ | `GET/POST/PUT/PATCH/DELETE /me/addresses`, `GET /public/regions/**` |
| Tài khoản › Mật khẩu | `POST /me/password/verify`, `POST /me/otp-challenges {PASSWORD_CHANGE}`, `PUT /me/password` |
| Tài khoản › Liên kết đăng nhập | `GET /me/login-methods`, `POST /me/identities/google/authorize` → Google → `/auth/oauth-callback` → `POST /me/identities/google`; gỡ: `DELETE /me/identities/google {current_password}` |
| Tài khoản › Thiết bị | `GET /me/sessions`, `DELETE /me/sessions/{id}`, `DELETE /me/sessions?scope=others` |
| Tài khoản › Thông báo | `GET /me/notifications`, `PATCH /me/notifications/{id}`, `POST /me/notifications/mark-all-read` |
| Tài khoản › Kho Voucher | `GET /me/vouchers`, `POST /me/vouchers`, `DELETE /me/vouchers/{id}` |
| Tài khoản › Điểm Genie | `GET /me/coins`, `POST /me/coins/check-ins`, `GET /me/coins/transactions`, `GET /public/coin-rewards`, `POST /me/coins/redemptions` |
| Tài khoản › Yêu thích | `GET /me/wishlist`, `DELETE /me/wishlist/{id}`, `POST /me/cart/items` |
| Tài khoản › Shop theo dõi | `GET /me/following`, `DELETE /me/following/{id}` |
| Chat nổi | `GET /me/conversations`, `POST /me/conversations`, `GET /me/conversations/{id}/messages`, WS `/app/chat.send`, `POST /me/conversations/{id}/read`, `GET /me/orders?shop_id=` |
| Trở thành Người bán | `GET /public/categories`, `POST /me/shop-registrations`, `POST /me/shop-registrations/{id}/confirm` |

### 14.2 Auth

| Màn hình | API |
|---|---|
| Đăng nhập (mật khẩu) | `POST /auth/sessions` |
| Đăng nhập (OTP) | `POST /auth/otp-challenges {LOGIN}` → `POST /auth/otp-challenges/{id}/verify` |
| Tài khoản tạm khoá | "Xác minh qua email": `POST /auth/otp-challenges {ACCOUNT_UNLOCK}` → `…/verify`; "Liên hệ hỗ trợ": trang tĩnh Trung tâm trợ giúp |
| Đăng ký | `POST /auth/registrations` → `POST /auth/otp-challenges/{id}/verify` |
| Quên mật khẩu | `POST /auth/otp-challenges {PASSWORD_RESET}` → `…/verify` → `POST /auth/password-resets` |
| Gửi lại mã | `POST /auth/otp-challenges/{id}/resend` |
| Google / Apple | `GET /auth/oauth/{provider}/authorize?next=` → Google → `/auth/oauth-callback` (cố định) → `POST /auth/oauth/{provider}/token` (Apple tắt bằng cờ) |

### 14.3 Seller Center

| Màn hình | API |
|---|---|
| Khung (sidebar, chuông, ô tìm nhanh) | `GET /seller/me`, `GET /me/notifications`, `GET /seller/search?q=`, WS |
| Tổng quan | `GET /seller/dashboard`, `GET /seller/analytics/revenue?range=`, `GET /seller/orders?size=5` |
| Đơn hàng | `GET /seller/orders?tab=`, `GET /seller/orders/{id}`, `POST /seller/orders/{id}/confirm|reject|prepare`, `GET /seller/orders/{id}/shipping-quotes`, `POST /seller/orders/{id}/shipments`, `GET /seller/orders/{id}/shipping-label` |
| Đơn hàng › Trả hàng | `GET /seller/refund-requests`, `POST /seller/refund-requests/{id}/request-evidence|approve|reject|confirm-return-received|escalate` |
| Sản phẩm (danh sách) | `GET /seller/products?status=`, `PATCH /seller/products/{id}`, `DELETE /seller/products/{id}` |
| Sản phẩm (form) | `GET /seller/products/{id}`, `GET /public/categories`, `GET /seller/categories/{id}/listing-rules`, `GET/POST /seller/brands`, `POST /uploads/presigned-urls`, `POST /seller/products` / `PUT /seller/products/{id}` |
| Ví & Escrow | `GET /seller/wallet`, `GET /seller/wallet/transactions`, `GET /seller/wallet/transactions/export`, `POST /me/otp-challenges {WITHDRAWAL}`, `POST /seller/withdrawals` |
| Hồ sơ KYC | `GET /seller/kyc`, `POST /uploads/presigned-urls {KYC_DOCUMENT}`, `PUT /seller/kyc/draft`, `POST /seller/kyc/submissions`, `POST /seller/kyc/manual-review-requests` |
| Danh mục toàn sàn | `GET /seller/categories`, `GET/POST /seller/category-proposals` |
| Khuyến mãi | `GET /seller/vouchers`, `GET/POST/PUT/PATCH /seller/vouchers/{id}`, `GET /seller/flash-sales?registrable=true`, `GET /seller/flash-sales/{id}/eligible-products|items`, `POST /seller/flash-sales/{id}/items` |
| Chat | `GET /seller/conversations`, `GET /seller/conversations/{id}/messages`, WS, `POST /seller/conversations/{id}/read`, `GET /seller/orders?customer_id=` |
| Đánh giá | `GET /seller/reviews?filter=`, `PUT /seller/reviews/{id}/reply`, `POST /reviews/{id}/reports` |
| Phân tích | `GET /seller/analytics/overview`, `GET /seller/analytics/traffic-sources`, `GET /seller/analytics/top-products`, `GET /seller/analytics/sentiment` |
| Cài đặt | `GET /seller/settings`, `PATCH /seller/settings`, `PUT /seller/pickup-address`, `GET /public/regions/**`, `GET /public/banks`, `POST /seller/payout-account/verification`, `PUT/DELETE /seller/payout-account`, `PUT/DELETE /seller/vacation`, `GET /me/login-methods`, `PUT /me/password` |

### 14.4 Admin Console

| Màn hình | Quyền | API |
|---|---|---|
| Khung (nav, chuông, badge) | — | `GET /admin/me`, `GET /admin/queues`, `GET /me/notifications`, WS |
| Tổng quan | `VIEW_DASHBOARD` (+`VIEW_FINANCE`) | `GET /admin/queues`, `GET /admin/dashboard/finance`, `GET /admin/dashboard/gmv-chart`, `GET /admin/dashboard/operations`, `GET /admin/dashboard/growing-shops`, `GET /admin/dashboard/export` |
| Duyệt KYC | `REVIEW_KYC` | `GET /admin/kyc-submissions`, `GET /admin/kyc-submissions/{id}`, `POST …/approve|reject|request-supplement|requeue` |
| Trả hàng & Hoàn tiền | `HANDLE_DISPUTE` | `GET /admin/disputes`, `POST /admin/disputes/{id}/resolve|confirm-return-received|reopen`, `POST /admin/refunds/{id}/sync|retry|mark-transferred`, `POST /admin/escrows/{id}/freeze|unfreeze` |
| Kiểm duyệt đánh giá | `MODERATE_REVIEW` | `GET /admin/review-reports`, `PATCH /admin/reviews/{id}` |
| Xử lý rút tiền | `PROCESS_PAYOUT` | `GET /admin/withdrawals`, `POST …/approve|reject|complete`, `GET …/transfer` |
| Đối soát COD | `PROCESS_PAYOUT` | `GET /admin/cod-remittances`, `GET /admin/cod-remittances/{id}`, `POST …/reconciliation-preview`, `POST …/confirm` |
| Cấu hình hoa hồng | `UPDATE_COMMISSION` | `GET /admin/commission-settings`, `PATCH /admin/commission-settings` |
| Sổ cái tài chính | `VIEW_FINANCE` | `GET /admin/ledger-entries`, `GET /admin/ledger-entries/export` |
| Nhật ký hệ thống | `VIEW_AUDIT_LOG` | `GET /admin/audit-logs`, `GET /admin/audit-logs/export` |
| Danh mục toàn sàn | `MANAGE_CATALOG` | `GET/POST/PATCH/DELETE /admin/categories`, `…/attributes`, `GET /admin/products?category_id=`, `POST /admin/products/{id}/lock|unlock`, `GET /admin/category-proposals`, `POST …/approve|reject` |
| Duyệt sản phẩm | `MANAGE_CATALOG` | `GET /admin/product-moderation`, `GET /admin/product-moderation/{id}`, `POST /admin/products/{id}/approve|reject|requeue`, `PUT /admin/products/{id}/assignee`, `GET /admin/moderators`, `PUT /admin/moderators/availability`, `POST /admin/moderators/redistribute` |
| Người dùng | `MANAGE_USER` | `GET /admin/users`, `POST /admin/users/{id}/suspend|reactivate` |
| Voucher sàn | `MANAGE_VOUCHER` | `GET/POST/PUT/PATCH/DELETE /admin/vouchers` |
| Flash Sale | `MANAGE_VOUCHER` | `GET/POST /admin/flash-sales`, `POST …/schedule|end`, `PATCH …/items/{item_id}` |
| Banner trang chủ | `MANAGE_VOUCHER` | `GET/POST/PUT/PATCH/DELETE /admin/banners`, `GET /admin/shops`, `POST /uploads/presigned-urls` |
| Thông báo sàn | `MANAGE_PLATFORM_NOTIFICATION` | `GET/POST/PUT/DELETE /admin/platform-notifications` |
| Phân quyền nhân viên | `MANAGE_RBAC` | `GET /admin/permission-catalog`, `GET /admin/staff`, `POST /admin/staff/invitations`, `POST …/invitations/resend`, `PUT …/permissions`, `POST …/lock|unlock`, `DELETE /admin/staff/{id}` |
| Tài khoản | — | `GET/PATCH /admin/me`, `PUT /me/password`, `GET /me/sessions`, `DELETE /me/sessions?scope=others` |

---

## 15. Danh mục mã lỗi

Định dạng `GNE-<MIỀN>-<HTTP><STT>` — phần `<HTTP>` luôn trùng mã HTTP trả về (từ v1.3). Cột **Nguồn**: `LLD` = có từ LLD 1.0; `mới` = do API này thêm — **đã bổ sung vào LLD 12.1 và Document_Vi 3.7.1 (TLCN v1.3)**.

| Mã | HTTP | Ý nghĩa | Nguồn |
|---|---|---|---|
| `GNE-VAL-4220` | 422 | Sai định dạng / Bean Validation (chi tiết trong `errors[]`) | LLD |
| `GNE-SEC-4010` | 401 | Access token hết hạn / sai chữ ký; refresh token bị dùng lại | LLD |
| `GNE-SEC-4030` | 403 | Thiếu quyền trên tài nguyên | LLD |
| `GNE-SEC-4031` | 403 | Phân quyền không hợp lệ (cấp `MANAGE_RBAC` cho STAFF) | LLD |
| `GNE-RAT-4290` | 429 | Vượt giới hạn tần suất (`Retry-After`) | LLD |
| `GNE-AUTH-4001` | 400 | OAuth `state` / `nonce` / `id_token` không hợp lệ | mới |
| `GNE-AUTH-4011` | 401 | Sai email / mật khẩu (`remaining_attempts`) | LLD |
| `GNE-AUTH-4012` | 401 | Tài khoản chỉ có Google / Apple, chưa thiết lập mật khẩu — không tính lần sai (`reason = PASSWORD_NOT_SET`, `provider`) | mới (v1.4 — có trong `ErrorCode.java`) |
| `GNE-AUTH-4031` | 403 | Tài khoản bị đình chỉ (khóa tạm dùng `4230`) | LLD |
| `GNE-AUTH-4032` | 403 | Email phía Google/Apple chưa xác minh — không liên kết | mới |
| `GNE-AUTH-4091` | 409 | Email đã được đăng ký | LLD |
| `GNE-AUTH-4092` | 409 | Tài khoản Google đã gắn với người dùng khác | mới |
| `GNE-AUTH-4093` | 409 | Không gỡ được cách đăng nhập duy nhất | mới |
| `GNE-AUTH-4094` | 409 | Email mới đã thuộc tài khoản khác | mới |
| `GNE-AUTH-4096` | 409 | Tài khoản không ở trạng thái khóa tạm, không cần mở khóa | mới |
| `GNE-AUTH-4097` | 409 | Không thu hồi được phiên hiện tại bằng `/me/sessions/{id}` (dùng `DELETE /auth/sessions/current`) | mới (v1.3) |
| `GNE-AUTH-4098` | 409 | Liên kết Google ở trang tài khoản: email Google khác email tài khoản Genie (`account_email`) | mới (v1.4 — có trong `ErrorCode.java`) |
| `GNE-AUTH-4095` | 409 | Tài khoản chỉ có Google, cần thiết lập mật khẩu trước khi đổi email | mới |
| `GNE-AUTH-4101` | 410 | OTP quá hạn 5 phút | LLD |
| `GNE-AUTH-4102` | 410 | `reset_token` hết hạn | mới |
| `GNE-AUTH-4103` | 410 | Lời mời nhân viên hết hạn | mới |
| `GNE-AUTH-4221` | 422 | Sai OTP lần thứ 5, challenge bị hủy | LLD |
| `GNE-AUTH-4222` | 422 | Sai OTP (`remaining_attempts`) | mới |
| `GNE-AUTH-4223` | 422 | Mật khẩu mới trùng mật khẩu hiện tại / gần nhất | mới |
| `GNE-AUTH-4224` | 422 | Email mới trùng email hiện tại | mới |
| `GNE-AUTH-4230` | 423 | Khóa đăng nhập 15 phút (`locked_until`, `locked_by`) | LLD |
| `GNE-USR-4091` | 409 | Còn đơn đang xử lý, chưa xóa được tài khoản (`active_order_codes`) | mới |
| `GNE-USR-4092` | 409 | Không xóa được địa chỉ mặc định | mới |
| `GNE-CAT-4041` | 404 | Danh mục không tồn tại / đã ẩn | mới |
| `GNE-CAT-4042` | 404 | Sản phẩm không còn bán | mới |
| `GNE-CAT-4091` | 409 | Trùng tên / slug danh mục (`existing_category`) | mới |
| `GNE-CAT-4092` | 409 | Danh mục còn sản phẩm / danh mục con | mới |
| `GNE-CAT-4093` | 409 | Sản phẩm đang bị sàn khóa | mới |
| `GNE-CAT-4094` | 409 | Sản phẩm còn đơn đang xử lý | mới |
| `GNE-CAT-4095` | 409 | Trùng mã thuộc tính trong ngành | mới (v1.3) |
| `GNE-CAT-4221` | 422 | Đã có 5 đề xuất danh mục đang chờ | mới |
| `GNE-CAT-4222` | 422 | Ngành bắt buộc giấy tờ chứng thực | mới |
| `GNE-CAT-4223` | 422 | Thiếu thông số bắt buộc của ngành (`missing_specs`) | mới |
| `GNE-INV-4090` | 409 | Hết tồn kho lúc giữ chỗ | LLD |
| `GNE-INV-4091` | 409 | Hết suất Flash Sale | LLD |
| `GNE-INV-4093` | 409 | Giữ chỗ đã hết hạn 15 phút | LLD |
| `GNE-INV-4094` | 409 | Số lượng vượt tồn khả dụng trong giỏ (`stock`) | mới |
| `GNE-ORD-4092` | 409 | Đơn đã qua bước được hủy | LLD |
| `GNE-ORD-4093` | 409 | Shop tạm nghỉ / đã từ chối đơn | LLD |
| `GNE-ORD-4094` | 409 | Mua sản phẩm của chính shop mình | LLD |
| `GNE-ORD-4095` | 409 | Đơn chưa giao, chưa xác nhận nhận hàng được | mới |
| `GNE-ORD-4041` | 404 | Đơn không tồn tại hoặc không thuộc shop của người gọi | mới (v1.3) |
| `GNE-PAY-4021` | 402 | Cổng thanh toán từ chối giao dịch | LLD |
| `GNE-PAY-4092` | 409 | IPN / webhook trùng (trả 200 cho cổng) | LLD |
| `GNE-PAY-4093` | 409 | Ký quỹ đã giải ngân / hoàn, không phong tỏa được | mới |
| `GNE-VCH-4041` | 404 | Mã voucher không tồn tại / đã hết hạn | mới |
| `GNE-VCH-4091` | 409 | Voucher đã được lưu | mới |
| `GNE-VCH-4092` | 409 | Trùng mã voucher | mới |
| `GNE-VCH-4093` | 409 | Voucher đã có lượt dùng, không xóa được | mới |
| `GNE-VCH-4221` | 422 | Voucher không áp dụng được (hết hạn, chưa đạt tối thiểu, vượt 85%…) | LLD |
| `GNE-PRO-4091` | 409 | Đã điểm danh hôm nay | mới |
| `GNE-PRO-4221` | 422 | Giá Flash không thấp hơn ≥ 15% / số suất không hợp lệ | mới |
| `GNE-PRO-4222` | 422 | Không đủ Điểm | mới |
| `GNE-PRO-4223` | 422 | Quá số sản phẩm tối đa / quá hạn đăng ký Flash Sale | mới |
| `GNE-PRO-4224` | 422 | Lên lịch Flash Sale khi chưa có sản phẩm được duyệt | mới |
| `GNE-SUP-4091` | 409 | Đã yêu cầu bổ sung bằng chứng một lần | mới |
| `GNE-SUP-4092` | 409 | Quá hạn 7 ngày / đơn đã có yêu cầu trả hàng đang mở | mới |
| `GNE-SUP-4221` | 422 | Thiếu ảnh hoặc video bằng chứng | mới |
| `GNE-SOC-4031` | 403 | Thẻ đơn / sản phẩm không thuộc hội thoại | mới |
| `GNE-SOC-4091` | 409 | Đã đánh giá dòng hàng này | mới |
| `GNE-SOC-4092` | 409 | Đơn chưa hoàn thành, chưa đánh giá được | mới |
| `GNE-SOC-4093` | 409 | Đã báo cáo đánh giá này | mới |
| `GNE-SOC-4094` | 409 | Shop đã phản hồi đánh giá | mới |
| `GNE-SOC-4221` | 422 | Phản hồi chứa số điện thoại | mới |
| `GNE-VND-4031` | 403 | Shop chưa `VERIFIED` (cổng KYC) | mới |
| `GNE-VND-4091` | 409 | Đổi tên shop quá 1 lần / 30 ngày | mới |
| `GNE-VND-4092` | 409 | Còn đơn chưa bàn giao, chưa bật chế độ nghỉ được | mới |
| `GNE-VND-4093` | 409 | Tài khoản đã có shop | mới |
| `GNE-VND-4094` | 409 | Tên shop đã được dùng | mới |
| `GNE-VND-4095` | 409 | Hồ sơ KYC đang được xử lý / đã duyệt | mới |
| `GNE-VND-4096` | 409 | Đã gửi yêu cầu nhân viên xem xét hồ sơ KYC | mới (v1.3) |
| `GNE-VND-4221` | 422 | Phải bật ít nhất 1 hãng vận chuyển | mới |
| `GNE-VND-4222` | 422 | Bật tự trả lời mà không có nội dung | mới |
| `GNE-VND-4223` | 422 | Hồ sơ KYC thiếu giấy tờ (`missing`) | mới |
| `GNE-VND-4224` | 422 | Ngày mở lại của chế độ nghỉ phải trong 30 ngày tới | mới (v1.3) |
| `GNE-WAL-4001` | 400 | Số dư khả dụng không đủ | LLD |
| `GNE-WAL-4221` | 422 | Dưới mức rút tối thiểu 100.000₫ | mới |
| `GNE-WAL-4032` | 403 | Tài khoản nhận tiền chưa xác minh chính chủ | LLD |
| `GNE-WAL-4222` | 422 | Tên chủ tài khoản không khớp CCCD | mới |
| `GNE-WAL-4092` | 409 | Duyệt chi cho shop chưa `VERIFIED` | mới |
| `GNE-WAL-4091` | 409 | Còn lệnh rút đang chờ, chưa xóa tài khoản nhận tiền | mới |
| `GNE-LOG-4221` | 422 | Bảng kê COD không cân | mới |
| `GNE-LOG-5021` | 502 | Hãng vận chuyển lỗi khi tạo vận đơn | mới |
| `GNE-LOG-5022` | 502 | GHN lỗi khi đồng bộ kho lấy hàng | mới |
| `GNE-BNR-4091` | 409 | Xóa banner đang hiển thị / đã lên lịch | mới |
| `GNE-BNR-4221` | 422 | Vị trí banner đã đủ 8 banner đang hiển thị | mới |
| `GNE-BNR-4222` | 422 | Liên kết banner ngoài miền sàn | mới |
| `GNE-NOT-4091` | 409 | Thông báo sàn đã phát, không sửa được | mới |
| `GNE-NOT-4221` | 422 | Danh sách người nhận rỗng hoặc có email không tồn tại (`unknown_emails`) | mới (v1.3) |
| `GNE-ADM-4091` | 409 | Email nhân viên đã được dùng | mới |
| `GNE-ADM-4092` | 409 | Không xóa được chính mình | mới (v1.3) |
| `GNE-ADM-4221` | 422 | Email nhân viên không có đuôi `@genie.example` | mới |
| `GNE-SYS-4040` | 404 | Không tìm thấy tài nguyên (đường dẫn / id không tồn tại hoặc không thuộc người gọi) | mới (v1.4 — `ErrorCode.java`) |
| `GNE-SYS-4050` | 405 | Phương thức HTTP không được hỗ trợ | mới (v1.4) |
| `GNE-SYS-5000` | 500 | Lỗi hệ thống không lường trước (không lộ stack trace; có `request_id`) | mới (v1.4) |
| `GNE-SYS-5030` | 503 | Không kiểm được Idempotency-Key / Redis không phản hồi (OTP, đăng ký tạm, `state` OAuth) / dịch vụ ngoài chưa cấu hình | LLD |
| `GNE-DB-5002` | 500 | Deadlock giao dịch tài chính (đã tự thử lại) | LLD |
| `GNE-MINIO-5003` | 500 | Presigned URL hết hạn khi tải tệp (kho tệp nay là Silo — giữ tên mã) | LLD |

---

## 16. Enum & mã lý do

### 16.1 Enum dùng trong API (đúng Phụ lục A ENTITY.md)

| Enum | Giá trị |
|---|---|
| `UserRole` | `CUSTOMER, VENDOR, STAFF, ADMIN` |
| `AccountStatus` | `UNVERIFIED, ACTIVE, LOCKED, SUSPENDED, ANONYMIZED, DELETED` |
| `Gender` | `MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY` |
| `BusinessType` | `INDIVIDUAL, HOUSEHOLD, BUSINESS` |
| `KycStatus` | `NOT_SUBMITTED, PENDING, AI_PROCESSING, MANUAL_REVIEW, VERIFIED, REJECTED, SUPPLEMENT_REQUESTED` |
| `ProductStatus` | `DRAFT, PENDING_APPROVAL, PUBLISHED, HIDDEN, REJECTED, FLAGGED, DELETED` |
| `ProductCondition` | `NEW, USED, REFURBISHED` |
| `ShippingPayer` | `BUYER, SELLER` |
| `AttributeDataType` | `TEXT, NUMBER, BOOLEAN, ENUM, MULTI_ENUM, DATE` |
| `ModerationDecision` | `APPROVE, REJECT, NEED_REVIEW` |
| `OrderStatus` (19) | `PENDING_PAYMENT, PAID, CONFIRMED, PREPARING, READY_FOR_PICKUP, DELIVERING, DELIVERED, DELIVERY_FAILED, RETURNING, RETURNED_TO_VENDOR, COMPLETED, RETURN_REQUESTED, RETURN_APPROVED, RETURN_REJECTED, PARTIALLY_REFUNDED, REFUNDED, CANCELLED, CANCELLED_BY_VENDOR, AUTO_CANCELLED` |
| `SessionStatus` | `INITIATED, PAYMENT_PENDING, COMPLETED, FAILED, EXPIRED` |
| `PaymentMethod` | `VNPAY, VIETQR, COD` |
| `PaymentRefundStatus` | `PENDING, SUCCESS, FAILED` |
| `CarrierType` | `GHN, GHTK, VIETTEL_POST, JNT_EXPRESS, GENIE_EXPRESS` |
| `ShipmentTrackingStatus` | `AWAITING_PICKUP, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, FAILED, RETURNING, RETURNED, LOST` |
| `RefundRequestType` | `RETURN_AND_REFUND, REFUND_ONLY` |
| `RefundResolution` | `APPROVE_WITH_RETURN, APPROVE_REFUND_ONLY` |
| `RefundStatus` | `REQUESTED, AWAITING_EVIDENCE, SHOP_REJECTED, DISPUTED, AWAITING_RETURN_SHIPMENT, RETURN_FAILED, APPROVED, REJECTED, REFUNDED` |
| `VoucherType` | `SHOP, PLATFORM, FREESHIP` |
| `DiscountType` | `PERCENTAGE, FIXED_AMOUNT, FREESHIP` |
| `VoucherScope` | `ALL, CATEGORY, PRODUCT` |
| `FundingParty` | `PLATFORM, VENDOR, CO_SPONSOR` |
| `FlashSaleStatus` | `DRAFT, SCHEDULED, ACTIVE, ENDED` |
| `CoinTransactionType` | `CHECK_IN, ORDER_CASHBACK, REVIEW_BONUS, USED_AT_CHECKOUT, ORDER_REFUND, VOUCHER_REDEEM` |
| `MessageType` | `TEXT, IMAGE, PRODUCT_CARD, ORDER_CARD` |
| `ReviewReportReason` | `SPAM, OFFENSIVE, FAKE_PURCHASE` |
| `TrafficSource` | `SEARCH, HOME_FEED, PROMOTION, SHOP_PAGE, EXTERNAL_LINK` |
| `BannerPosition` | `HOME_TOP_CAROUSEL, HOME_TOP_SIDE, HOME_MIDDLE, CATEGORY_TOP, FLASH_SALE_TOP, POPUP` |
| `PlatformNotificationTarget` | `ALL, CUSTOMER, VENDOR, SPECIFIC_USERS` |
| `NotificationType` | `ORDER_STATUS_CHANGED, KYC_APPROVED, KYC_REJECTED, PAYMENT_SUCCESS, PAYMENT_FAILED, ESCROW_RELEASED, WALLET_CREDITED, SYSTEM_ALERT, REFUND_PROCESSED, NEW_PLATFORM_VOUCHER, NEW_VENDOR_VOUCHER, NEW_ORDER_FOR_VENDOR, FLASH_SALE_REMINDER, NEW_DEVICE_LOGIN, PRODUCT_APPROVED, PRODUCT_REJECTED` (+ `PLATFORM_ANNOUNCEMENT` cho dòng từ `platform_notifications`) |
| `WithdrawalStatus` | `PENDING, APPROVED, PROCESSING, COMPLETED, REJECTED` |
| `FinancialEventType` | `ESCROW_IN, COMMISSION, PLATFORM_VOUCHER, FREESHIP_COST, COIN_COST, ESCROW_RELEASE, PAYOUT_ACCRUE, PAYOUT_SETTLE, REFUND, GATEWAY_FEE` |
| `AuditAction` | `LOGIN_SUCCESS, LOGIN_FAILED, ACCOUNT_LOCKED, ACCOUNT_SUSPENDED, ACCOUNT_REACTIVATED, KYC_SUBMITTED, KYC_APPROVED, KYC_REJECTED, ORDER_STATUS_CHANGED, ESCROW_RELEASED, PAYOUT_APPROVED, PAYOUT_REJECTED, REFUND_PROCESSED, REVIEW_HIDDEN, COMMISSION_UPDATED, PERMISSION_GRANTED, PERMISSION_REVOKED, RBAC_ROLE_CHANGED, STAFF_INVITED, PII_ANONYMIZED, ACCESS_DENIED, PRODUCT_MODERATED` |
| `StaffPermission` (13) | `VIEW_DASHBOARD, REVIEW_KYC, MANAGE_CATALOG, MODERATE_REVIEW, HANDLE_DISPUTE, PROCESS_PAYOUT, UPDATE_COMMISSION, VIEW_FINANCE, MANAGE_VOUCHER, MANAGE_PLATFORM_NOTIFICATION, VIEW_AUDIT_LOG, MANAGE_USER, MANAGE_RBAC` |
| `StaffTitle` | `MODERATOR, CS_SPECIALIST, FINANCE_OFFICER` (+ `CUSTOM` ở API khi quyền đã chỉnh khác bộ mặc định) |

### 16.2 Enum chỉ có ở API (gom / hiển thị, không lưu DB)

| Enum | Giá trị | Dùng ở |
|---|---|---|
| `OtpPurpose` | `REGISTER, LOGIN, PASSWORD_RESET, PASSWORD_CHANGE, ACCOUNT_DELETION, WITHDRAWAL, PAYOUT_ACCOUNT_UPDATE, PAYOUT_ACCOUNT_DELETE, SHOP_REGISTRATION, EMAIL_CHANGE, ACCOUNT_UNLOCK` | 0.7 |
| `CustomerOrderAction` | `PAY, CANCEL, TRACK, CHAT, CONFIRM_RECEIPT, REQUEST_RETURN, VIEW_RETURN, REVIEW, BUY_AGAIN` | 7.1, 7.2 |
| `VendorOrderAction` | `CONFIRM, REJECT, PREPARE, HAND_OVER, PRINT_LABEL` | 10.3 |
| `VoucherState` | `AVAILABLE, UPCOMING, USED, EXPIRED` | `$Voucher` |
| `ManagedVoucherStatus` | `ACTIVE, SCHEDULED, ENDED, DISABLED` | 10.7, 11.14 |
| `BannerState` | `LIVE, SCHEDULED, ENDED, OFF` | 11.16 |
| `WalletEntryState` | `HELD, AVAILABLE, PAYOUT_PENDING, PAYOUT_DONE, REFUNDED` | 10.10 |
| `DisputeOutcome` | `REFUND_NOW, RETURN_THEN_REFUND, DENY, CARRIER_FAULT` | 11.4 |
| `KycDocumentType` | `ID_FRONT, ID_BACK, BUSINESS_LICENSE, BUSINESS_REGISTRATION, SELFIE, TAX_NOTICE` | 10.12, 11.3 |
| `RiskLevel` | `LOW, MEDIUM, HIGH` | 11.3 |
| `NotificationCategory` | `ORDER, PROMOTION, SYSTEM, CHAT` | 3.12 |
| `AuditCategory` | `AUTH, KYC, FINANCE, MODERATION, SECURITY, CONFIG` — ánh xạ từ `AuditAction`: **AUTH** = `LOGIN_SUCCESS, LOGIN_FAILED, PASSWORD_CHANGED, PASSWORD_RESET, EMAIL_CHANGED, OAUTH_LINKED, OAUTH_UNLINKED, SESSION_REVOKED, ACCOUNT_DELETION_REQUESTED, ACCOUNT_DELETION_CANCELLED` · **KYC** = `KYC_SUBMITTED, KYC_APPROVED, KYC_REJECTED` · **FINANCE** = `ESCROW_RELEASED, PAYOUT_APPROVED, PAYOUT_REJECTED, REFUND_PROCESSED, ORDER_STATUS_CHANGED` · **MODERATION** = `REVIEW_HIDDEN, PRODUCT_MODERATED` · **SECURITY** = `ACCOUNT_LOCKED, ACCOUNT_SUSPENDED, ACCOUNT_REACTIVATED, ACCESS_DENIED, PII_ANONYMIZED` · **CONFIG** = `COMMISSION_UPDATED, PERMISSION_GRANTED, PERMISSION_REVOKED, RBAC_ROLE_CHANGED, STAFF_INVITED, BANNER_CHANGED` | 11.10 |

### 16.3 Ánh xạ trạng thái đơn → tab

| Tab khách (`status_group`) | `OrderStatus` |
|---|---|
| `TO_PAY` — Chờ thanh toán | `PENDING_PAYMENT` |
| `SHIPPING` — Vận chuyển | `PAID` (chưa giao), `CONFIRMED`, `PREPARING`, `READY_FOR_PICKUP`, `DELIVERING`, `DELIVERY_FAILED` |
| `TO_RECEIVE` — Chờ giao hàng | `DELIVERED`; `PAID` của đơn COD đã giao (`delivered_at ≠ null`) |
| `COMPLETED` — Hoàn thành | `COMPLETED` |
| `RETURN` — Trả hàng / Hoàn tiền | `RETURN_REQUESTED, RETURN_APPROVED, RETURN_REJECTED, RETURNING, RETURNED_TO_VENDOR, PARTIALLY_REFUNDED, REFUNDED` |
| `CANCELLED` — Đã huỷ | `CANCELLED, CANCELLED_BY_VENDOR, AUTO_CANCELLED` |

| Tab shop (`tab`) | `OrderStatus` |
|---|---|
| `NEW` — Mới | `PENDING_PAYMENT` của đơn COD; `PAID` của đơn online chưa xác nhận |
| `TO_PREPARE` — Chờ chuẩn bị | `CONFIRMED, PREPARING` |
| `DELIVERING` — Đang giao | `READY_FOR_PICKUP, DELIVERING, DELIVERY_FAILED, RETURNING` |
| `DELIVERED` — Đã giao | `DELIVERED, COMPLETED`; `PAID` của đơn COD đã giao |
| `CANCELLED` — Đã hủy | `CANCELLED, CANCELLED_BY_VENDOR, AUTO_CANCELLED` |
| Trả hàng | dùng `GET /seller/refund-requests` |

### 16.4 Mã lý do (client dịch theo ngôn ngữ)

| Nhóm | Mã → nhãn tiếng Việt trên giao diện |
|---|---|
| Hủy đơn (khách) | `CHANGE_OF_MIND` Đổi ý · `CHANGE_ADDRESS` Muốn đổi địa chỉ · `CHANGE_VARIANT` Muốn đổi sản phẩm / phân loại · `FOUND_BETTER_PRICE` Tìm được giá tốt hơn · `OTHER` Lý do khác |
| Từ chối đơn (shop) | `OUT_OF_STOCK` Hết hàng thực tế · `WRONG_PRICE` Sai giá niêm yết · `UNDELIVERABLE_AREA` Không giao tới khu vực · `OTHER` |
| Trả hàng | `DAMAGED_IN_TRANSIT` Bể vỡ do vận chuyển · `WRONG_SIZE_COLOR` Sai kích thước/màu · `COUNTERFEIT` Hàng giả/nhái · `MISSING_ACCESSORIES` Thiếu phụ kiện · `NOT_RECEIVED` Không nhận được hàng · `NOT_AS_DESCRIBED` Không đúng mô tả · `DEFECTIVE` Sản phẩm lỗi |
| Từ chối KYC | `BLURRY_DOCUMENT` Ảnh mờ · `INFO_MISMATCH` Thông tin không khớp · `EXPIRED_DOCUMENT` Giấy tờ hết hạn · `SUSPECTED_FORGERY` Nghi giả mạo · `MISSING_LICENSE` Thiếu GPKD · `OTHER` |
| Từ chối / khóa sản phẩm | `MISSING_SAFETY_CERT` Thiếu chứng nhận an toàn · `POLICY_VIOLATION` Vi phạm chính sách · `BAD_IMAGE` Ảnh không đạt · `MISLEADING_DESCRIPTION` Mô tả sai · `UNREASONABLE_PRICE` Giá bất hợp lý · `PROHIBITED_ITEM` Hàng cấm |
| Từ chối rút tiền | `ACCOUNT_MISMATCH` Tài khoản không khớp hồ sơ · `OPEN_DISPUTE` Có khiếu nại đang mở · `SUSPICIOUS` Giao dịch bất thường · `OTHER` |
| Đình chỉ người dùng | `COUNTERFEIT_SELLER` Bán hàng giả · `ABNORMAL_RETURNS` Hoàn hàng bất thường · `REVIEW_SPAM` Spam đánh giá · `FRAUD` Lừa đảo · `OTHER` |

---

## 17. Chênh lệch với ENTITY/LLD & quyết định thiết kế

### 17.1 Trường màn hình cần mà ENTITY.md v1.2 chưa có — **đã bổ sung ở ENTITY.md v1.3** (cả 10 file `java-code/`, GIAI_NGHIA_ENTITY.md, LLD mục 5; migration: đã nằm sẵn trong `V1…V12` của repo — viết 1-1 từ ENTITY.md v1.3)

| Màn hình cần | Entity | Đã thêm |
|---|---|---|
| Mô tả ở đầu trang danh mục | `Category` | `Category.description` (NULLABLE, ≤ 500) |
| Nhãn địa chỉ "Nhà riêng / Văn phòng" | `Address` | `Address.label` + enum `AddressLabel` |
| "Đánh giá ẩn tên" | `ProductReview` | `ProductReview.isAnonymous` |
| Duyệt / từ chối từng sản phẩm Flash Sale | `FlashSaleItem` | `FlashSaleItem.status` + enum `FlashSaleItemStatus` |
| Hẹn giờ phát thông báo sàn (LLD đã có cron "Phát thông báo hẹn giờ") | `PlatformNotification` | `PlatformNotification.sendAt`, `recipientUserIdsJson` |
| Thẻ đơn hàng trong chat | `Message` | `Message.orderId` (+ quan hệ #101 `Message → Order`) |
| Phiên đối soát COD | (chưa có) | Entity `CodRemittance` — bảng `cod_remittances` (ENTITY.md mục 6.8) + enum `CodRemittanceStatus` |
| Huy hiệu "Shop Yêu Thích", "Shop đã xác minh" nổi bật ở trang chủ | `VendorProfile` | `VendorProfile.isFavorite` (sàn gắn); "Shop đã xác minh" ở trang chủ tính từ `ratingAvg` + `isMall` |
| Voucher nổi bật ở trang chủ | `Voucher` | Dùng `priority` sẵn có (không thêm trường) |

### 17.2 Điểm API khác LLD 1.0 mục 8.5 — có chủ đích (**LLD 1.1 mục 8.5, Document_Vi, implement.md Phần 17, sơ đồ tuần tự, Usecase.md đã đổi theo**)

| LLD | API này | Lý do |
|---|---|---|
| `POST /auth/login`, `/auth/refresh`, `/auth/logout` | `POST /auth/sessions`, `POST /auth/sessions/refresh`, `DELETE /auth/sessions/current` | Phiên đăng nhập là tài nguyên; khớp `GET/DELETE /me/sessions` của màn Thiết bị |
| `POST /auth/register` + `/register/verify` | `POST /auth/registrations` + `POST /auth/otp-challenges/{id}/verify` | Một cơ chế OTP chung cho đăng ký / đăng nhập OTP / quên mật khẩu (màn OTP dùng chung) |
| OTP gửi kèm từng API (đổi mật khẩu, rút tiền…) | `POST /me/otp-challenges` + header `X-OTP-Challenge-Id`, `X-OTP-Code` | Một luật step-up cho mọi thao tác nhạy cảm, dùng được cả với `DELETE` |
| `POST /storage/upload` + `POST /…/media/confirm` | `POST /uploads/presigned-urls` + `object_key` trong request nghiệp vụ | Bỏ bước confirm riêng, bớt 1 round-trip |
| `GET /public/search` | `GET /public/products?q=` | Tìm kiếm và lọc danh mục / shop là cùng một tài nguyên với bộ lọc khác nhau |
| `POST /shops/{vendorId}/follow`, `POST /me/wishlist` | `PUT/DELETE /me/following/{shop_id}`, `PUT/DELETE /me/wishlist/{product_id}` | `PUT` idempotent — bấm tim 2 lần không lỗi |
| `POST /checkout` | `POST /me/checkout-sessions` | Tài nguyên là phiên thanh toán (CheckoutSession) |
| `POST /admin/kyc/{id}/{approve,reject,supplement}` | `/admin/kyc-submissions/{id}/approve|reject|request-supplement|requeue` | Thêm "Đưa lại vào hàng chờ" mà màn hình có |
| `GET /admin/dashboard` | 4 endpoint `dashboard/finance|gmv-chart|operations|growing-shops` | Mỗi khối có bộ lọc riêng (kỳ báo cáo, khoảng biểu đồ, ngành + phân trang) và quyền riêng (`VIEW_FINANCE`) |
| `POST /admin/cod/reconcile` (CSV) | `cod-remittances/{id}/reconciliation-preview` + `/confirm` | Màn hình xem trước kết quả đối chiếu rồi mới xác nhận |

### 17.3 Quyết định thiết kế khác

1. **Mã đơn trên trang Thanh toán:** server chỉ cấp mã khi tạo phiên (6.10) → trang Thanh toán hiện "Đơn 1/2" (đã sửa `storefront/Checkout.jsx`); mã thật hiện ở trang kết quả.
2. **Sổ giao dịch ví của shop** (10.10) là *bản chiếu hợp nhất* từ `escrow_ledgers` (dòng `SALE` đang giữ) và `wallet_transactions` (`RELEASE`, `PAYOUT`, `REFUND`) — `WalletTransactionType` không có `SALE` vì tiền chưa vào ví.
3. **Thông báo** gộp 3 bảng (`notifications`, `platform_notifications` + `user_platform_notification_reads`, `admin_notifications`) vào một luồng `GET /me/notifications`, phân biệt bằng tiền tố `id`.
4. **Đếm giỏ, thông báo, chat** trả trong `GET /me` một lần, sau đó chỉ cập nhật qua WebSocket — không poll.
5. **Sản phẩm `FLAGGED`** hiện là "Chờ duyệt" với shop (10.5); chỉ Admin thấy kết quả Gemini.
6. **Giá Flash Sale** gắn theo `product_id` (đúng `FlashSaleItem.productId`); ô chọn trên giao diện đã đổi thành "Sản phẩm".
7. **Tài khoản nhận tiền khi rút**: giao diện TLCN cũ cho nhập lại tài khoản mỗi lần rút; API dùng tài khoản đã xác minh ở Cài đặt (UC-VND-07) — đúng màn Ví hiện tại.
8. **Trang chính sách & trang chiến dịch** là nội dung tĩnh ở frontend, không có API.

### 17.4 Chốt khi dựng hợp đồng Swagger (v1.3)

Khi sinh OpenAPI (`genie-api-spec`) từ tài liệu này, các chỗ còn hở được chốt như sau — tài liệu và Swagger UI giờ trùng nhau:

| Chỗ | Chốt |
|---|---|
| 3 mã ví lệch định dạng | `GNE-WAL-4002` → `GNE-WAL-4221`, `GNE-WAL-4033` → `GNE-WAL-4222`, `GNE-WAL-4034` → `GNE-WAL-4092` (đã đổi cả LLD 12.1, Document_Vi 3.7.1) |
| Lỗi chưa có mã | Thêm `GNE-AUTH-4097` (3.6), `GNE-ORD-4041` (10.3), `GNE-VND-4224` (10.11), `GNE-VND-4096` (10.12), `GNE-CAT-4095` (11.11), `GNE-NOT-4221` (11.17), `GNE-ADM-4092` (11.18) |
| Phản hồi 6.3, 7.6, 7.8 | `{ item, cart_item_count }`; khối `refund_request`; `{ status: REJECTED }` |
| Tin nhắn 8.3 | Có `client_message_id` |
| `type` thông báo 3.12 | Chuỗi — `NotificationType` / `PLATFORM_ANNOUNCEMENT` / `AdminNotificationType` theo nguồn |
| Webhook & nội bộ (12) | Payload SePay, `{carrier}` viết thường, body KYC nội bộ, health không bọc phong bì |
| Mở shop (0.7 ↔ 9) | Dùng luồng OTP riêng của mục 9, không dùng header `X-OTP-*` |

---


### 17.5 Đồng bộ với mã nguồn (v1.4 — 2026-11-16)

**a) Backend AUTH đã hiện thực — khác spec 1.3 (spec đã sửa theo code):**

| Chỗ | Spec 1.3 | Hiện thực (spec 1.4) | Lý do |
|---|---|---|---|
| 2.1 đăng ký | Tạo `users` `UNVERIFIED` ngay | Giữ tạm ở Redis 15 phút, tạo `users` khi OTP đúng | Không ghi rác vào PostgreSQL (plan AUTH D2, D3) |
| 2.5 đăng nhập | Tài khoản chỉ Google → `401 4011` | `401 GNE-AUTH-4012`, không tính lần sai | Không khóa oan, hướng dẫn đúng cách đăng nhập |
| 2.6 làm mới | Không kèm `user` | Kèm `user` | Web tải lại trang chỉ còn cookie |
| 2.9 / 2.10 OAuth | Client gửi `redirect_uri` | Callback cố định, `next` lưu trong `state` | Chống open redirect, trùng URI khai ở Google |
| 3.3 liên kết | Không có bước lấy URL | `POST /me/identities/{provider}/authorize` | Trình duyệt chuyển hướng không mang access token |
| 3.3 liên kết | Gắn bất kỳ Gmail nào | Email Google phải trùng email tài khoản (`4098`) | Một người = một tài khoản = một email |
| 3.3 gỡ liên kết | Không body | Body `current_password` | Thao tác nhạy cảm |
| 3.3 `provider` | `GOOGLE` | `google` (chữ thường) | Thống nhất path |
| 3.7 xóa tài khoản | Đăng xuất mọi phiên | Đăng xuất thiết bị khác, giữ phiên hiện tại | Khách cần thấy băng đỏ và nút hủy |

**b) Giao diện `web/` (đang chạy mock) gọi đường dẫn khác spec — giữ spec, sửa mã khi nối backend** (`docs/web-backend-handoff.md` mục 2):

| Mã web đang gọi | Theo spec |
|---|---|
| `GET /me/wishlist/ids` | `GET /me/wishlist/product-ids` |
| `GET/POST/DELETE /me/shop-follows…` | `GET /me/following`, `PUT/DELETE /me/following/{shop_id}`, `GET /me/following/shop-ids` |
| `GET /public/search/suggest` | `GET /public/search/suggestions` |
| `GET /public/feed` | `GET /public/recommendations` |
| `GET /public/products/{id}/related` | `GET /public/products/{id}/similar` |
| `GET /public/products/facets` | `GET /public/products?…&include_facets=true` |
| `POST /me/notifications/{id}/read`, `POST /me/notifications/read-all` | `PATCH /me/notifications/{id}`, `POST /me/notifications/mark-all-read` |
| `POST /reviews/{id}/helpful` | `PUT / DELETE /reviews/{id}/helpful-vote` |
| `POST /public/banners/{id}/events` | `POST /public/banner-events` |
| `GET /public/shops/{id}/vouchers` | `GET /public/vouchers?shop_id=` |
| `POST/DELETE /me/identities/GOOGLE` | `…/google` — **đã sửa** trong mã (2026-11-16) |

**c) Spec bổ sung theo giao diện:** 6 endpoint ở mục 5.18; trường `product_slug` ở dòng giỏ (6.1) và `$OrderItem`. Thông báo loại đơn hàng đã có `link: { "type": "ORDER", "id": … }` — web dùng `link.id` thay cho tách số từ mã `TR-…`. `category_name` ở dòng giỏ chỉ là trường của mock (tính voucher phía client) — **không** đưa vào spec.

## 18. Chức năng bổ sung & độ phủ

**Độ phủ (rà ngày 2026-11-08):** 87/87 use case có endpoint hoặc tác vụ nền tương ứng (mục 12.2); mọi nút có thao tác dữ liệu trên Storefront, Auth, Seller Center, Admin Console đều có endpoint. Không cần API: nút mô phỏng demo ("Mô phỏng: …"), chọn giao diện / theme, "Xem với vai trò" (Admin demo), chia sẻ (deeplink phía client), in phiếu (`window.print`), trang chính sách tĩnh, "Tải ứng dụng".


Các mục trước đây "chưa có màn hình" nay đã có cả giao diện lẫn API:

| Chức năng | UC | Giao diện đã thêm | Endpoint |
|---|---|---|---|
| Cảm xúc đánh giá trong Phân tích của shop | UC-AI-07 | `vendor/VendorPages.jsx` — khối "Cảm xúc đánh giá" | `GET /seller/analytics/sentiment` (10.9) |
| Nhãn "AI: Tiêu cực" + ưu tiên hàng chờ | UC-AI-07, UC-SOC-05 | `admin/Disputes.jsx` (Kiểm duyệt đánh giá) | `sentiment` trong `GET /admin/review-reports` (11.5) |
| Từ khóa xu hướng khi ô tìm kiếm trống | UC-CAT-04 | `storefront/Header.jsx` | `GET /public/search/trending` (5.4b) |
| Đổi email đăng nhập | UC-AUTH-08 | `storefront/Account.jsx` — modal "Đổi email đăng nhập" | `POST /me/email-change-requests` (+ `/confirm`) (3.2) |
| Phong tỏa / gỡ phong tỏa ký quỹ | UC-SUP-03 (`EscrowLedger.freeze()`) | `admin/Disputes.jsx` — nút trên thẻ tranh chấp | `POST /admin/escrows/{id}/freeze|unfreeze` (11.4) |
| Chi tiết đơn phía shop | UC-LOG-01 | `vendor/Fulfillment.jsx` — nút "Chi tiết" | `GET /seller/orders/{id}` (10.3) |
| Mở khóa tài khoản qua email | UC-AUTH-02 | `auth/index.html` — nút "Xác minh qua email" | `POST /auth/otp-challenges {ACCOUNT_UNLOCK}` (2.2) |
| Tìm nhanh đơn / sản phẩm | UC-LOG-16, UC-CAT-10 | `vendor/VendorShell.jsx` — dropdown ô tìm kiếm | `GET /seller/search` (10.1b) |
| Tác vụ nền & email | UC-ORD-08, 12, PAY-07, NOT-03 | — | Mục 12.2 |
| Xóa banner | UC-ADM-12 | `admin/Banners.jsx` — nút "Xóa" khi Đã tắt / Đã kết thúc | `DELETE /admin/banners/{id}` (11.16) |
