# PLAN — Cụm B5 Voucher: quản lý, chưa áp vào giá (UC-PRO-01 · UC-PRO-02 · UC-PRO-03)

| | |
|---|---|
| **Ngày lập** | 2026-11-26 |
| **Thể loại** | server-spring (module `promotion`, feature `voucher`) + nối `web/` (feature `seller-promotions`, `admin-marketing`, `promotions`, `account`) |
| **Use case** | Tạo & Quản lý Voucher Gian hàng — Vendor Shop Vouchers (UC-PRO-01) · Tạo & Quản lý Voucher Toàn sàn (UC-PRO-02) · Khách hàng thu thập Voucher vào Kho Voucher cá nhân (UC-PRO-03) |
| **Tài liệu chuẩn** | UC phân hệ 07 (UC-PRO-01 dòng 1889, 02 dòng 1919, 03 dòng 1944) · API mục 1 (`$Voucher`), 3.14, 5.13, 10.7, 11.14, 15 · ENTITY 4.1–4.4 (`Voucher`, `VoucherUsage`, `UserVoucher`, 4 bảng phạm vi), Phụ lục A (`VoucherType`, `DiscountType`, `VoucherScope`, `FundingParty`) · BAO_CAO dòng UC-CAT-05 (xóa cache `promotion:shop_voucher_vendors`) — xem [README](README.md) |
| **Theo dõi tiến độ** | [progress_voucher_b5.md](progress_voucher_b5.md) — cùng mã task `Vxx.yy` |
| **Phụ trách** | **Nguyễn Ngọc Thiện** — lập plan và thực hiện (nhận 2026-11-26) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `--no-verify`, không `Co-Authored-By` |
| **Phụ thuộc** | Plan [1] **N01** (`V27`: 2 index `vouchers`, 4 giá trị `AuditAction.VOUCHER_*`). Không phụ thuộc chat / thông báo — làm song song được |
| **Mục tiêu** | 3 use case ⬜ → ✅ (đủ 5 lớp). **Không** áp voucher vào giá (UC-PRO-04 — chuỗi C6); plan này chỉ tạo / sửa / tắt / xóa / lưu voucher và hiển thị đúng |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `5abb096`, 2026-11-26)

| Phần | Đã có | Còn thiếu |
|---|---|---|
| **Entity / bảng** (`V4__promotion.sql`) | `Voucher` (`code` UNIQUE, `title`, `scope`, `type` ∈ `SHOP, PLATFORM, FREESHIP`, `discount_type` ∈ `PERCENTAGE, FIXED_AMOUNT, FREESHIP`, `discount_value` NOT NULL, `max_discount_cap`, `min_order_value`, `usage_limit`, `usage_per_user`, `used_count`, `priority` NOT NULL, `vendor_id` = `users.id` chủ shop, `valid_from/to`, `is_active`, `@Version`, `budget`, `total_discount_used`, `notification_body`, `sponsor_type` ∈ `PLATFORM, VENDOR, CO_SPONSOR`, `platform_share_percent`; phương thức `isApplicable`, `calculateDiscount`, `consume`, `restore`, `validateConfig`), `UserVoucher` (UNIQUE `(user_id, voucher_id)`, `is_used` cột có nhưng entity để `@Transient`), `VoucherUsage` (UNIQUE `(voucher_id, checkout_session_id)`), 4 bảng phạm vi `voucher_categories / products / excluded_categories / excluded_products` | ⚠ `validateConfig` **chặn** `type = PLATFORM` + `sponsor = CO_SPONSOR` (mâu thuẫn API 11.14) → D7. Không có phương thức trạng thái `ACTIVE / SCHEDULED / ENDED / DISABLED`, luật sửa khi đang chạy. Không có index theo shop / loại → `V27` |
| **Repository / service đã có** | `VoucherRepository.findByCode`, `findByCodeIn`, `findVendorIdsWithActiveShopVoucher` (bộ lọc "Shop có voucher" — B3 / plan 2026-11-24); `UserVoucherRepository.findByUserIdAndVoucherId`; `VoucherUsageRepository.countByUserIdAndVoucherId`; `PromotionSearchPorts` (cache 60 giây `promotion:shop_voucher_vendors`, có `evictCache()`); `VoucherView` (`$Voucher` — dùng ở đổi Điểm) — ⚠ `shop.id` đang gán **`vendor_id` (users.id)** thay vì `vendor_profiles.id`, `shop.name = null` | API voucher shop / sàn / công khai / kho; sửa `VoucherView` (D13) |
| **Quà đổi Điểm** | `genie.coin.rewards[]` trỏ tới mã `COIN500 / COIN1000 / COIN2000` (seed `R__seed_demo_50_vouchers.sql`) — chỉ nhận qua `POST /me/coins/redemptions` | Danh sách công khai / lưu bằng mã **phải loại** các mã này (ghi trong seed) — D12 |
| **Seed** | 12 voucher: 2 sàn, 1 freeship, 3 quà Điểm, 6 shop (TechZone ×2, Shoe Republic, Home & Living đã tắt, Beauty Box hết lượt) | Voucher sắp diễn ra / phạm vi ngành để demo tab (V09.01) |
| **Quyền / cổng** | `MANAGE_VOUCHER` (Admin + Finance Officer); cổng KYC `/seller/**` phủ `/seller/vouchers` (`403 GNE-VND-4031` khi shop chưa `VERIFIED`); `BuyerOnlyFilter` chặn tài khoản nội bộ ở `/me/cart`, `/me/checkout` | `/me/vouchers` cho tài khoản nội bộ (D14) |
| **Mã lỗi** | — (chưa có mã `GNE-VCH-*` trong `ErrorCode`) | `GNE-VCH-4041, 4091, 4092, 4093, 4221` (đã có ở API 15) + `GNE-VCH-4094` (**mới** — D3) |
| **Web** | `seller-promotions/promo.api.ts` (`listVouchers`, `getVoucher`, `saveVoucher`, `disableVoucher`, `listVoucherTargets` → `/public/categories` + **`/seller/products`** (C1 — chưa có)), `admin-marketing/marketing.api.ts` (5 hàm voucher sàn), `promotions/voucher.api.ts` (`listMyVouchers`, `getVouchersByCodes` → `/public/vouchers?codes=` (**lệch** — spec chỉ có `code`), `saveVoucher`, `listShopVouchers` → `/public/shops/{id}/vouchers` (**lệch** — handoff dòng 38), `findVoucherByCode` → 6.9 (C6), `removeVoucher`); tất cả `USE_MOCK`. Flash Sale nằm cùng file nhưng thuộc UC-PRO-05 (C6) | Nối thật theo cờ, **giữ mock cho Flash Sale** |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Package `com.genie.promotion.voucher.{api,application,domain}`; luật nghiệp vụ trong entity `Voucher` (trạng thái, luật sửa, kiểm cấu hình); service ánh xạ `IllegalStateException` / `IllegalArgumentException` sang mã `GNE-VCH-*` | TS 4, 7.2 |
| N2 | **`promotion` không import `vendor` / `order`** (vòng `vendor → promotion → order → vendor` khi C6 cài cổng order trong promotion). Tên shop, `shop_id`, trạng thái shop, mã đơn đã dùng mã: **SQL read-model JOIN** bảng `vendor_profiles`, `users`, `orders`, `voucher_usages` (tiền lệ plan 2026-11-17 D6). Danh mục: `promotion → catalog` (đã có) hoặc read-model `categories` | README mục 3 |
| N3 | Tiền `BigDecimal` trong CSDL, API trả **số nguyên VND**; `%` là số (`15`, `7.5`) | TS 8; API 0 |
| N4 | Voucher shop luôn gắn `vendor_id = users.id` của người gọi (lấy từ token, **không** nhận từ body); kiểm sở hữu ở service (IDOR) — voucher của shop khác → `404` | TS 9 |
| N5 | Mọi thay đổi voucher **SHOP** → `PromotionSearchPorts.evictCache()` **sau commit**; mọi thay đổi voucher công khai → xóa cache danh sách công khai (D15) sau commit | BAO_CAO UC-CAT-05; TS 4.5 |
| N6 | Thay đổi voucher **sàn** ghi `system_logs` (`VOUCHER_CREATED / UPDATED / DISABLED / DELETED`, kèm `details.changes`) trong cùng transaction | NFR-08 |
| N7 | Không đổi tên bảng / cột / enum (NFR-12); chỉ thêm index (đã ở `V27`) | TS 7.3 |
| N8 | JSON snake_case, vỏ `ApiResponse`; danh sách Seller / Admin / Kho phân trang `page / size` (API 10.7, 11.14, 3.14); `@Operation(summary = "UC-PRO-0x …")` | TS 8 |
| N9 | Mỗi task có test: unit (`VoucherTest`, bộ kiểm input) + `*IT` Testcontainers | TS 14 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Trạng thái quản lý** `ACTIVE, SCHEDULED, ENDED, DISABLED` (10.7, 11.14) | ENTITY 4.1 ghi vòng đời trong chú thích `isActive` | `Voucher.status(now)`: `!is_active` → `DISABLED` (trừ khi tự tắt vì hết ngân sách → `ENDED`); `now < valid_from` → `SCHEDULED`; `now >= valid_to` **hoặc** `used_count >= usage_limit` **hoặc** `budget` đã hết → `ENDED`; còn lại `ACTIVE`. Phía khách (`$Voucher.state`) giữ `VoucherView` (`AVAILABLE / UPCOMING / USED / EXPIRED`) nhưng dùng chung `status()` (`ENDED`/`DISABLED` → `EXPIRED`). Ghi ENTITY 4.1 + Phụ lục E | ☐ |
| D2 | **Lọc theo `status` trong SQL** | 10.7, 11.14 `?status=` | Điều kiện SQL tương đương D1 (viết một lần trong `VoucherStatusSql`, test chéo với `status()` bằng bảng ca); sắp `ACTIVE`/`SCHEDULED`: `valid_from` gần nhất; `ENDED`/`DISABLED`: `valid_to` mới nhất | ☐ |
| D3 | **Sửa voucher đang chạy** (công bằng với người đã lưu) | UC-PRO-01 b10, UC-PRO-02 b9 ("Sửa") | `SCHEDULED` **và chưa ai lưu** → sửa mọi trường (trừ `code` nếu đã có người lưu). `ACTIVE` hoặc `SCHEDULED` đã có người lưu → **chỉ** được: đổi `title`, kéo dài `valid_to`, tăng `usage_limit`, tăng `budget`, (sàn) `notification_body`; trường khác → **`409 GNE-VCH-4094` (mới)** `details.locked_fields`. `ENDED` / `DISABLED` → `409 GNE-VCH-4094` (không sửa được; dùng *Nhân bản*). Thêm mã vào API 15 | ☐ |
| D4 | **Tắt / bật lại / huỷ lịch** | 10.7, 11.14 `PATCH {is_active: false}` | Chỉ **tắt một chiều** (`is_active: true` → `422`): tránh tái phát mã đã báo dừng. Tắt bản `SCHEDULED` = "Huỷ lịch". Tắt lại bản đã tắt → `200` (idempotent) | ☐ |
| D5 | **Mã voucher** | UC-PRO-01 b2; API 10.7 | Chuẩn hóa chữ HOA, `^[A-Z0-9]{6,12}$`; duy nhất **toàn bảng** (cả voucher đã kết thúc) → `409 GNE-VCH-4092`; kiểm trước + bắt `DataIntegrityViolationException` của `UNIQUE` khi hai request đua. Nút "Tự tạo" sinh phía client (API) | ☐ |
| D6 | **Tiêu đề voucher shop** — `title` NOT NULL nhưng `$ShopVoucherInput` không có | API 10.7 vs ENTITY 4.1 | Server **tự sinh** khi tạo / sửa từ điều kiện: "Giảm 15% tối đa 50.000₫ đơn từ 200.000₫", "Giảm 30.000₫ đơn từ 399.000₫", "Freeship tối đa 30.000₫ đơn từ 99.000₫" (`VoucherTitles`, hàm thuần + test). Voucher sàn: `title` bắt buộc 5–100 ký tự (API 11.14) | ☐ |
| D7 | **Nhà tài trợ & đồng tài trợ** | UC-PRO-02 b4, b8; API 11.14 (`PLATFORM, CO_SPONSOR`); `validateConfig` chặn CO_SPONSOR | Voucher shop: server đặt `type = SHOP`, `sponsor = VENDOR`. Voucher sàn: `funded_by` ∈ `PLATFORM`, `CO_SPONSOR` (`platform_share_percent` 1–99 bắt buộc khi `CO_SPONSOR`, `null` khi `PLATFORM`), `vendor_id = null`. **Sửa `validateConfig`** cho phép `PLATFORM/FREESHIP` + `CO_SPONSOR` (ENTITY 4.1 sửa trước). Chia chi phí khi áp → C6 | ☐ |
| D8 | **FREESHIP** — `discount_value` NOT NULL, API bảo "bỏ `discount_value`, `max_discount`" | API 10.7, 11.14; seed `FREESHIP30K` (30000 / 30000) | `discount_type = FREESHIP`: nhận `max_discount` (tùy chọn — trần hỗ trợ phí ship; `null` = hỗ trợ toàn bộ phí), CSDL lưu `discount_value = max_discount` (hoặc `0` khi không trần) + `max_discount_cap = max_discount`. Shop: `type = SHOP` + `discount_type = FREESHIP` (shop tài trợ ship — UC-PRO-01 b9). Sàn: `type = FREESHIP`. Sửa câu API 10.7 / 11.14 | ☐ |
| D9 | **Giới hạn giá trị** | UC-PRO-01 b3–5, UC-PRO-02 b7 | `PERCENTAGE` 1–100 (API) **bắt buộc** `max_discount` ≥ 1.000; `FIXED_AMOUNT` 1.000–50.000.000 và `min_order_value ≥ discount_value` (không ra đơn 0₫); `min_order_value` 0–1.000.000.000; `usage_limit` 1–1.000.000; `usage_per_user` 1–min(`usage_limit`, 100); `budget` (tùy chọn) ≥ mức giảm tối đa một lượt; `valid_from` ≥ now − 5 phút khi tạo; `valid_to > valid_from`, thời lượng ≤ 180 ngày. Lỗi gom `errors[]` một lần (`422 GNE-VAL-4220`) | ☐ |
| D10 | **Phạm vi áp dụng & loại trừ** | UC-PRO-01 b6, b9 (4 bảng); API 10.7 (thiếu loại trừ sản phẩm); UC-PRO-02 b8 | Shop: `scope` ∈ `ALL, CATEGORY, PRODUCT`; `category_ids` (cần khi `CATEGORY`, ≤ 20, ngành tồn tại + đang hiện), `product_ids` (cần khi `PRODUCT`, ≤ 100, sản phẩm **của chính shop**, chưa xóa), `excluded_category_ids` và **`excluded_product_ids` (thêm vào API)** chỉ khi `scope ≠ PRODUCT`. Sàn: `scope` ∈ `ALL, CATEGORY` + loại trừ ngành / sản phẩm. Sửa = thay cả tập (xóa rồi chèn trong cùng transaction). `$Voucher.included_categories / excluded_categories` = tên ngành | ☐ |
| D11 | **Ưu tiên** (`priority` NOT NULL, không có trong input) | ENTITY 4.1 | Server đặt mặc định theo loại như seed: `SHOP` 1, `PLATFORM` 2, `FREESHIP` 3; dùng để sắp "Mã giảm giá hot" (`featured`) | ☐ |
| D12 | **Quà đổi Điểm** (`COIN500…`) | seed 50 dòng 6–7; plan Điểm Genie D3 | Loại khỏi `GET /public/vouchers` (mọi chế độ) và `POST /me/vouchers` theo mã / id (→ `404 GNE-VCH-4041` — không lộ); vẫn hiện trong Kho của người đã đổi. Màn Admin hiện kèm cờ `is_coin_reward = true`, **không cho đổi `code`** và không xóa (cấu hình trỏ theo mã) | ☐ |
| D13 | **`$Voucher.shop`** — `VoucherView` đang trả `users.id` làm `shop.id` | API mục 1 (`shop.id` = shop); bẫy hai id | Sửa `VoucherView.of(…)` nhận `ShopRef` từ read-model (`vendor_profiles.id`, `shop_name`) nạp theo lô; cập nhật chỗ gọi ở `CoinRedemptionService` (voucher sàn → `shop = null`, không đổi hành vi) | ☐ |
| D14 | **Lưu voucher** (UC-PRO-03 b4) | API 3.14 | `POST /me/vouchers {code}` hoặc `{voucher_id}`: voucher phải tồn tại, không phải quà Điểm, trạng thái `ACTIVE` hoặc `SCHEDULED` (lưu trước được — API 5.13), shop của voucher còn `VERIFIED` + chủ `ACTIVE` → ngược lại `404 GNE-VCH-4041`; đã có trong Kho → `409 GNE-VCH-4091`; đã dùng hết lượt / người → `409 GNE-VCH-4091` `details.reason = USAGE_LIMIT_REACHED`. Chèn `ON CONFLICT DO NOTHING` (bấm 2 lần). **Tài khoản nội bộ** STAFF / ADMIN: thêm `/api/v1/me/vouchers` vào `BuyerOnlyFilter.PURCHASE_PREFIXES` → `403 GNE-ORD-4031` (file của plan B1 — báo Nguyễn Quốc Khoa trong PR). `DELETE /me/vouchers/{voucher_id}` idempotent `204` | ☐ |
| D15 | **Danh sách công khai** (API 5.13) + cache | 5.13; LUU_TRU 4 | `shop_id` (= `vendor_profiles.id`) → voucher SHOP `ACTIVE` + `SCHEDULED` của shop (shop phải `VERIFIED`, chủ `ACTIVE`); `code` **hoặc `codes` (thêm, ≤ 20, cách dấu phẩy — trang chiến dịch, web đang dùng)**; `featured=true&limit=1..12` → voucher `PLATFORM` / `FREESHIP` `ACTIVE`/`SCHEDULED` sắp `priority DESC, valid_to ASC`. Không trả `ENDED`/`DISABLED`. Cache Redis 60 giây `promotion:public_vouchers:featured`, `…:shop:{shopId}` (qua `JsonCache`, Redis lỗi → đọc CSDL); `is_saved` ghép **sau** cache theo người gọi (một câu `user_vouchers` theo lô). Xóa cache sau commit khi voucher thay đổi | ☐ |
| D16 | **Kho voucher** `GET /me/vouchers` | API 3.14; UC-PRO-03 b7–8 | `state=AVAILABLE` (gồm `UPCOMING`) / `USED` / `EXPIRED`, `type` ∈ `PLATFORM, SHOP, FREESHIP` (FREESHIP gồm cả voucher shop `discount_type = FREESHIP`), `page / size` 20; `USED` = số `voucher_usages` của người ≥ `usage_per_user` (kèm `used_order_code` — read-model JOIN `orders`); `summary.counts` theo loại cho tab đang xem; sắp: dùng được trước, sắp hết hạn trước | ☐ |
| D17 | **Thống kê** | UC-PRO-01 b11; UC-PRO-02 b9; API 10.7, 11.14 `summary` | Shop: `active_count`, `usage_this_month` (`voucher_usages` của voucher shop trong tháng giờ VN), `revenue_from_promotions_this_month` (Σ tổng đơn `COMPLETED` có `order_discounts` của voucher shop trong tháng — read-model; **= 0 tới khi C5 / C6 có đơn hoàn tất**). Sàn: `active_count`, `total_usage` (Σ `used_count`), `total_discount_used`, `scheduled_count`, `remaining_budget` (Σ `budget − total_discount_used` của voucher còn chạy có ngân sách) | ☐ |
| D18 | **Xóa vĩnh viễn** (chỉ voucher sàn) | API 11.14; UC-PRO-02 b9 | Chỉ khi `status ∈ {ENDED, DISABLED}` **và** không có dòng `voucher_usages` → xóa voucher + 4 bảng phạm vi + `user_vouchers` trong một transaction; đã có lượt dùng → `409 GNE-VCH-4093`; đang `ACTIVE`/`SCHEDULED` → `409 GNE-VCH-4094` ("tắt trước"). Quà Điểm → `409 GNE-VCH-4094`. Shop không xóa (API không có) | ☐ |
| D19 | **Đụng độ ghi** (`@Version`) | ENTITY 4.1 | Sửa / tắt cùng lúc với C6 trừ lượt → `ObjectOptimisticLockingFailureException`: thử lại 1 lần trong service; vẫn lỗi → `409 GNE-VCH-4094` "Voucher vừa thay đổi, hãy tải lại" | ☐ |
| D20 | **Chọn sản phẩm cho phạm vi `PRODUCT`** (web) | `listVoucherTargets` gọi `/seller/products` (C1 — chưa có) | Khi `seller-vouchers` thật mà C1 chưa có `/seller/products`: ô chọn dùng `GET /public/products?shop_id={shop của mình}&q=` (B3 — đã có, chỉ sản phẩm đang bán); C1 xong thì đổi lại | ☐ |
| D21 | **Cờ nối web** | `isMocked(feature)` | `seller-vouchers` (Seller › Khuyến mãi — phần voucher), `admin-vouchers` (Admin › Voucher sàn), `vouchers` (Storefront: trang chủ / shop / chiến dịch + Tài khoản › Kho Voucher). Flash Sale (seller / admin) **giữ mock** (C6) | ☐ |
| D22 | **Ngoài phạm vi** | — | Áp voucher khi mua, Voucher Picker, `applicable-vouchers` (UC-PRO-04 — C6), Flash Sale (UC-PRO-05, 06), thông báo "shop có voucher mới" cho người theo dõi / `NEW_PLATFORM_VOUCHER` hàng loạt (Admin dùng UC-NOT-05), voucher phương thức thanh toán | ☐ |

---

## 3. Luồng nghiệp vụ chi tiết

### 3.1 Tạo / sửa voucher shop (UC-PRO-01)

```
POST /seller/vouchers $ShopVoucherInput            (cổng KYC: shop phải VERIFIED)
  ├─ VoucherInputValidator (D5, D8, D9, D10) → 422 errors[] gom một lần
  ├─ phạm vi: category_ids tồn tại (catalog) ; product_ids thuộc vendor_id = me (read-model products)
  ├─ code trùng → 409 GNE-VCH-4092
  ├─ Voucher.createShop(me, input, titles.of(input), priority 1) → validateConfig() → save → lưu 4 bảng phạm vi
  └─ sau commit: PromotionSearchPorts.evictCache() + PublicVoucherCache.evictShop(shopId)
PUT /seller/vouchers/{id}  → voucher.vendor_id = me (404 nếu không) → Voucher.applyEdit(input, hasSavers, now) (D3) → như trên
PATCH /seller/vouchers/{id} {is_active:false} → Voucher.disable() (D4) → evict
GET /seller/vouchers?status&page&size → danh sách + summary (D17) ; GET /{id} → $ShopVoucherInput + id, status, usage
```

### 3.2 Voucher sàn (UC-PRO-02)

```
POST /admin/vouchers $PlatformVoucherInput        (MANAGE_VOUCHER)
  ├─ như 3.1 + title bắt buộc, funded_by / platform_share_percent (D7), scope ALL|CATEGORY
  ├─ type = FREESHIP nếu discount_type = FREESHIP, còn lại PLATFORM ; priority 2 / 3
  ├─ audit VOUCHER_CREATED {code, type, budget}
  └─ sau commit: PublicVoucherCache.evictFeatured()
PUT /{id} (D3, audit VOUCHER_UPDATED + changes) · PATCH /{id} (D4, VOUCHER_DISABLED) · DELETE /{id} (D18, VOUCHER_DELETED)
GET /admin/vouchers?status&page&size → + summary (D17), is_coin_reward (D12)
```

### 3.3 Lưu vào Kho & hiển thị (UC-PRO-03)

```
GET /public/vouchers?shop_id | code | codes | featured&limit   (cache D15, is_saved ghép theo người gọi)
POST /me/vouchers {code | voucher_id} (D14) → INSERT user_vouchers ON CONFLICT DO NOTHING → 201 $Voucher (is_saved = true)
GET /me/vouchers?state&type&page&size (D16) → $Voucher[] + summary.counts
DELETE /me/vouchers/{voucher_id} → 204
```

---

## 4. Cây file sẽ tạo / sửa

```
server-spring/src/main/java/com/genie/
├── common/error/ErrorCode.java                   (sửa) + 6 mã GNE-VCH-* (khối // ===== Voucher)
├── common/security/BuyerOnlyFilter.java          (sửa — một dòng, D14; báo Khoa)
└── promotion/voucher/
    ├── domain/       Voucher (sửa: status(), createShop/createPlatform, applyEdit, disable, isDeletable, validateConfig nới D7)
    │                 VoucherStatus (mới) · VoucherRepository (sửa: truy vấn quản lý / công khai) · repo 4 bảng phạm vi (sửa: deleteByVoucherId)
    ├── application/  ShopVoucherService · PlatformVoucherService · VoucherQueryService · VoucherWalletService
    │                 PublicVoucherService · PublicVoucherCache · VoucherInputValidator · VoucherTitles · VoucherScopeWriter
    │                 VoucherReadModel (SQL: shop ref, tên ngành, mã đơn đã dùng, thống kê) · VoucherView (sửa — D13)
    │                 VoucherChangedEvent + listener xóa cache (AFTER_COMMIT)
    └── api/          SellerVoucherController · AdminVoucherController · PublicVoucherController · MeVoucherController · request/response record
server-spring/src/main/resources/db/seed/R__seed_demo_50_vouchers.sql   (sửa — thêm voucher sắp diễn ra / theo ngành, V09.01)
server-spring/src/test/java/com/genie/promotion/voucher/
    VoucherTest · VoucherStatusSqlTest · VoucherInputValidatorTest · VoucherTitlesTest
    SellerVoucherIT · AdminVoucherIT · PublicVoucherIT · VoucherWalletIT · VoucherSecurityIT · VoucherCacheEvictionIT
web/src/features/seller-promotions/infrastructure/promo.api.ts       (sửa — phần voucher)
web/src/features/admin-marketing/infrastructure/marketing.api.ts      (sửa — phần voucher sàn)
web/src/features/promotions/infrastructure/voucher.api.ts             (sửa — 2 endpoint lệch spec, cờ)
web/src/shared/api/error-codes.ts · web/src/i18n/{vi,en}.json
docs/Document/…  ENTITY (4.1, Phụ lục E) · API_SPEC (mục 1, 3.14, 5.13, 10.7, 11.14, 15) · Usecase.md · CHECKLIST · BAO_CAO · Quản Lý UseCase.xlsx
docs/api-tests/vouchers.http · docs/kiem-thu/kich-ban-2026-11-26-voucher.md · docs/web-backend-handoff.md
```

---

## 5. Bảng ánh xạ Use case → API → Dữ liệu

| Bước UC | Endpoint | Bảng | Quyền |
|---|---|---|---|
| UC-PRO-01 b1–8: tạo voucher shop | `POST /seller/vouchers` | `vouchers`, 4 bảng phạm vi | VENDOR `VERIFIED` |
| UC-PRO-01 b10: danh sách 3 trạng thái, Sửa / Nhân bản / Tắt | `GET /seller/vouchers`, `GET/PUT/PATCH /seller/vouchers/{id}` | như trên | như trên |
| UC-PRO-01 b11: thẻ thống kê | `summary` của `GET /seller/vouchers` | `vouchers`, `voucher_usages`, `order_discounts`, `orders` (đọc) | như trên |
| UC-PRO-02 b1–7: tạo voucher sàn, ngân sách | `POST /admin/vouchers` | `vouchers`, phạm vi, `system_logs` | `MANAGE_VOUCHER` |
| UC-PRO-02 b9: Nhân bản / Sửa / Tắt / Xóa, 5 chỉ số | `GET/PUT/PATCH/DELETE /admin/vouchers/{id}`, `summary` | như trên | `MANAGE_VOUCHER` |
| UC-PRO-02 b6: "Mã giảm giá hot" trang chủ | `GET /public/vouchers?featured=true&limit=4` | `vouchers` | công khai |
| UC-PRO-03 b1–2: thẻ voucher ở trang chủ / shop / sản phẩm / chiến dịch | `GET /public/vouchers?shop_id | code(s) | featured` | `vouchers`, `user_vouchers` (`is_saved`) | công khai (+ token) |
| UC-PRO-03 b3–6: "Lưu mã" | `POST /me/vouchers` | `user_vouchers` | CUSTOMER / VENDOR |
| UC-PRO-03 b7–8: Kho Voucher, lịch sử, xóa | `GET /me/vouchers`, `DELETE /me/vouchers/{voucher_id}` | `user_vouchers`, `voucher_usages`, `orders` (đọc) | như trên |

**Mã lỗi** (API 15): `GNE-VCH-4041` (404), `GNE-VCH-4091` (409 đã lưu / hết lượt), `GNE-VCH-4092` (409 trùng mã), `GNE-VCH-4093` (409 đã có lượt dùng), `GNE-VCH-4221` (422 — thêm vào `ErrorCode` cho C6 dùng), **`GNE-VCH-4094` (409 — không sửa / xóa được ở trạng thái hiện tại, mới)**. Dùng lại `GNE-VAL-4220`, `GNE-VND-4031`, `GNE-ORD-4031`, `GNE-SEC-4030`.

---

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — gồm `ModuleBoundaryTest` (**`promotion` không import `vendor` / `order`**), `SeedDataIT`.
2. Swagger có endpoint mới, summary `UC-PRO-01/02/03`.
3. Thử tay `docs/api-tests/vouchers.http`: `mai.tran@gmail.com` (chủ TechZone) tạo voucher shop; `owner.shoe@genie.example` không sửa được voucher của TechZone (`404`); `linh.tm@genie.example` (Finance Officer) quản lý voucher sàn; `phat.nt@genie.example` (Moderator) → `403`; khách thuần lưu voucher.
4. Đụng `web/`: `cd web && npm run verify`.
5. Tick task ở progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Vxx.yy` = task; `Vxx.Cn` = task commit.

### V00 — Chuẩn bị

- **[V00.01]** Kiểm danh tính git; kiểm `V27` (plan [1] N01) đã có trên `develop` — chưa thì chờ / rebase.
- **[V00.02]** Chốt D1–D22 vào progress.
- **[V00.03]** Tạo nhánh `feature/voucher-management` từ `develop` mới nhất.
- **[V00.04]** Đọc màn mock — Seller › Khuyến mãi (3 chỉ số, bảng Mã / Loại / Điều kiện / Đã dùng / Trạng thái / Kết thúc, modal "Tạo Voucher Shop", Nhân bản), Admin › Voucher sàn (5 chỉ số, tab trạng thái, modal), trang chủ (4 vé), trang shop (hàng voucher), trang chiến dịch, Tài khoản › Kho Voucher (4 tab, lịch sử, nhập mã, modal chi tiết); tìm mọi chỗ gọi `getVouchersByCodes`, `listShopVouchers`, `findVoucherByCode` (`grep`) để biết màn nào đổi.
- **[V00.05]** Kiểm `GlobalExceptionHandler` đã ánh xạ `ObjectOptimisticLockingFailureException` chưa (D19); kiểm `CategoryFacade` có tra tên / tồn tại theo lô không (D10) — không có thì dùng read-model `categories`.

### V01 — Tài liệu trước

- **[V01.01]** ENTITY 4.1 — `status(now)` + `VoucherStatus`, `createShop`, `createPlatform`, `applyEdit` (luật D3), `disable`, `isDeletable`, `validateConfig` cho phép `CO_SPONSOR` (D7), cách lưu FREESHIP (D8), `priority` mặc định (D11); Phụ lục E; `java-code/Diagram04_*.java`, `GIAI_NGHIA_ENTITY.md`.
- **[V01.02]** API_SPEC — mục 1 `$Voucher` (`shop.id` = `vendor_profiles.id`); 3.14 (D14: nội bộ `403`, `USAGE_LIMIT_REACHED`, quà Điểm `404`, `DELETE` idempotent; D16); 5.13 (`codes`, `limit` 1–12, không trả `ENDED`, cache 60 giây — D15); 10.7 (tiêu đề tự sinh D6, FREESHIP D8, giới hạn D9, `excluded_product_ids` D10, luật sửa D3, tắt một chiều D4, `summary` D17); 11.14 (như trên + D7, D18, `is_coin_reward`); 15 thêm `GNE-VCH-4094`; lịch sử phiên bản.
- **[V01.03]** Usecase.md — UC-PRO-01 b8 (tiêu đề tự sinh), b10 (luật sửa khi đang chạy), b11 ("Doanh thu từ khuyến mãi" = 0 tới khi có đơn hoàn tất); UC-PRO-02 b4 / b8 (đồng tài trợ không gắn shop cụ thể — chia khi áp ở C6), b9 (Xóa khi đã kết thúc / đã tắt + chưa dùng); UC-PRO-03 b4 (luật lưu D14).
- **[V01.C1]** COMMIT — `docs(promotion): specify voucher lifecycle, edit rules and wallet saving`

### V02 — Miền & kiểm dữ liệu vào

- **[V02.01]** `ErrorCode` — 6 mã `GNE-VCH-*` (khối `// ===== Voucher`).
- **[V02.02]** `VoucherStatus` + `Voucher.status(now)` (D1); `VoucherStatusSql` (D2).
- **[V02.03]** `Voucher.createShop(vendorUserId, …)`, `createPlatform(…)`, `applyEdit(input, hasSavers, now)` → trả danh sách trường bị khóa (D3), `disable()` (D4), `isDeletable(hasUsages)` (D18), `validateConfig` nới (D7).
- **[V02.04]** `VoucherInputValidator` (D5, D8, D9, D10) — hàm thuần nhận input + `now` → `List<FieldError>`; `VoucherTitles` (D6).
- **[V02.05]** `VoucherRepository` — trang quản lý theo `vendor_id` / loại + `status` (D2); `existsByCode`; đếm `user_vouchers` của một voucher (`hasSavers`); `voucher_usages` có dòng nào; 4 repo phạm vi `deleteByVoucherId` + chèn lô (`VoucherScopeWriter`).
- **[V02.06]** Test — `VoucherTest` (bảng ca `status` ở mọi biên thời gian / lượt / ngân sách / tắt; `applyEdit` khi `SCHEDULED` chưa ai lưu / đã lưu / `ACTIVE` / `ENDED`; `validateConfig` 6 tổ hợp loại × nhà tài trợ), `VoucherStatusSqlTest` (IT nhỏ: SQL ra đúng tập với `status()` trên 12 voucher dựng sẵn), `VoucherInputValidatorTest` (mỗi luật D9 một ca biên), `VoucherTitlesTest`.
- **[V02.C1]** COMMIT — `feat(promotion): add voucher lifecycle status and edit rules`

### V03 — Voucher gian hàng (UC-PRO-01)

- **[V03.01]** `VoucherReadModel` — `shopOfVendor(userId)` / `shopRefs(vendorIds)` (JOIN `vendor_profiles`), `productsOwnedBy(vendorUserId, productIds)` (JOIN `products`, chưa xóa), `categoryNames(ids)`, `shopMonthStats(vendorUserId, monthStart, monthEnd)` (D17).
- **[V03.02]** `ShopVoucherService` — `create`, `update`, `disable`, `get` (kiểm sở hữu N4; trùng mã D5; phạm vi D10); phát `VoucherChangedEvent(type, vendorUserId, shopId)`.
- **[V03.03]** `VoucherQueryService.shopPage(me, status, page, size)` + `summary` (D17); `toShopInput(voucher)` cho form sửa / nhân bản (kèm `category_ids`, `product_ids`, loại trừ).
- **[V03.04]** Listener `AFTER_COMMIT` — `PromotionSearchPorts.evictCache()` (voucher SHOP) + `PublicVoucherCache.evictShop(shopId)` (N5).
- **[V03.05]** `SellerVoucherController` — `GET /seller/vouchers`, `GET /seller/vouchers/{id}`, `POST`, `PUT /{id}`, `PATCH /{id}`; `@Operation(summary = "UC-PRO-01 …")`; `POST` → `201`.
- **[V03.06]** `SellerVoucherIT` — tạo 3 loại (%, tiền, freeship) + 3 phạm vi; tiêu đề tự sinh đúng; trùng mã (kể cả khác hoa thường) → `4092`; hai request cùng mã song song → một `201` một `409`; sản phẩm của shop khác trong `product_ids` → `422` `errors[].field = product_ids`; sửa voucher `SCHEDULED` chưa ai lưu đổi mọi trường; sửa `ACTIVE` đổi `discount_value` → `4094` `locked_fields`, kéo dài `valid_to` → `200`; tắt → `DISABLED`, bật lại → `422`; shop B sửa voucher shop A → `404`; tạo voucher xong `GET /public/products?has_shop_voucher=true` thấy shop ngay (cache đã xóa); `summary.usage_this_month` đếm đúng `voucher_usages` dựng tay.
- **[V03.C1]** COMMIT — `feat(promotion): let sellers create, edit and disable shop vouchers`

### V04 — Voucher toàn sàn (UC-PRO-02)

- **[V04.01]** `PlatformVoucherService` — `create`, `update`, `disable`, `delete`, `get` (D7, D8, D18, D19); audit 4 loại kèm `details.changes`; quà Điểm (D12) không đổi `code`, không xóa.
- **[V04.02]** `VoucherQueryService.platformPage(status, page, size)` + `summary` (D17) + `is_coin_reward` (đọc `genie.coin.rewards`).
- **[V04.03]** `AdminVoucherController` — 6 endpoint API 11.14, `@PreAuthorize("hasAuthority('MANAGE_VOUCHER')")`, `@Operation(summary = "UC-PRO-02 …")`.
- **[V04.04]** `AdminVoucherIT` — tạo `PLATFORM` / `FREESHIP` / `CO_SPONSOR` (thiếu `platform_share_percent` → `422`); `valid_from` quá khứ → `422`; ngân sách: dựng `total_discount_used = budget` → `status = ENDED`, `remaining_budget` không âm; xóa voucher `ENDED` chưa dùng → `204` (mất cả phạm vi + Kho); đã dùng → `4093`; đang chạy → `4094`; quà Điểm đổi mã → `4094`; mỗi thao tác 1 dòng `system_logs` đúng mã + `changes`; sửa cùng lúc với một transaction giữ khóa dòng (giả lập C6 `consume`) → thử lại thành công hoặc `4094`.
- **[V04.05]** `VoucherSecurityIT` — Admin / Finance Officer `200`; Moderator / CS / khách `403`; shop chưa `VERIFIED` gọi `/seller/vouchers` → `403 GNE-VND-4031`; khách gọi `/seller/vouchers` → `403`.
- **[V04.C1]** COMMIT — `feat(promotion): let staff manage platform vouchers with budget and audit log`

### V05 — Voucher công khai & Kho Voucher (UC-PRO-03) — nhánh `feature/voucher-wallet`

- **[V05.01]** `VoucherView` sửa (D13) — `ShopRef` từ read-model, tên ngành áp dụng / loại trừ, `used_order_code`; cập nhật `CoinRedemptionService` + test của plan Điểm Genie vẫn xanh.
- **[V05.02]** `PublicVoucherService` + `PublicVoucherCache` (D15) — 3 chế độ, loại quà Điểm (D12), shop ẩn khi không `VERIFIED` / chủ bị khóa, `is_saved` ghép sau cache.
- **[V05.03]** `VoucherWalletService` — `save(me, code | voucherId)` (D14), `remove(me, voucherId)`, `page(me, state, type, page, size)` + `summary.counts` (D16).
- **[V05.04]** `BuyerOnlyFilter` thêm `/api/v1/me/vouchers` (D14) + ca trong test sẵn có của filter.
- **[V05.05]** `PublicVoucherController` (`GET /public/vouchers`, `@Operation(summary = "UC-PRO-03 …")`), `MeVoucherController` (`GET`, `POST`, `DELETE /me/vouchers…`).
- **[V05.06]** `PublicVoucherIT` — `featured` chỉ sàn / freeship, đúng thứ tự `priority`, không có `COIN*`; `shop_id` = `vendor_profiles.id` (không phải users.id) trả voucher của đúng shop, kể cả `SCHEDULED`; shop bị khóa → rỗng; `codes=GENIE10,TECHZONE5` trả 2; có token → `is_saved` đúng; lần 2 lấy từ cache, Admin tắt voucher → lần sau không còn (cache đã xóa).
- **[V05.07]** `VoucherWalletIT` — lưu bằng mã (chữ thường vẫn nhận) và bằng id; lưu 2 lần → `4091`; lưu quà Điểm bằng mã → `4041`; lưu voucher `ENDED` / `DISABLED` → `4041`; dùng hết lượt (dựng `voucher_usages`) → `4091` `USAGE_LIMIT_REACHED`; hai request lưu song song → 1 dòng; `GET` 3 trạng thái + `summary.counts`; `USED` có `used_order_code`; xóa 2 lần → `204`; STAFF → `403 GNE-ORD-4031`.
- **[V05.08]** `VoucherCacheEvictionIT` — tạo / sửa / tắt voucher SHOP → cache `promotion:shop_voucher_vendors` và `…:shop:{id}` bị xóa sau commit, rollback thì không.
- **[V05.C1]** COMMIT — `fix(promotion): return shop id and name in voucher view`
- **[V05.C2]** COMMIT — `feat(promotion): list public vouchers and let buyers save them to their wallet`
- **[V05.C3]** COMMIT — `feat(common): block internal accounts from the voucher wallet`

### V06 — Nối web Seller › Khuyến mãi (UC-PRO-01)

- **[V06.01]** `promo.api.ts` — `listVouchers`, `getVoucher`, `saveVoucher`, `disableVoucher` theo `isMocked('seller-vouchers')`; DTO / mapper theo API 10.7 (bỏ `title` khỏi body, `excluded_product_ids`, FREESHIP gửi `max_discount`); **Flash Sale giữ mock** (cờ riêng, không đổi).
- **[V06.02]** `listVoucherTargets` — ngành từ `/public/categories` (đã có), sản phẩm theo D20 (`/public/products?shop_id=&q=`).
- **[V06.03]** Modal "Tạo Voucher Shop" — ánh xạ `errors[].field` về ô; `4092` báo tại ô mã; `4094` hiện các trường bị khóa (`locked_fields`) và khóa ô tương ứng khi mở form voucher đang chạy; Nhân bản = mở form từ `GET /{id}`, xóa mã; Tắt có hộp xác nhận; 3 chỉ số từ `summary`. Kiểm 9 theme.
- **[V06.04]** `error-codes.ts` + vi / en — 6 mã `GNE-VCH-*`, khóa mới; `check-i18n` xanh.
- **[V06.C1]** COMMIT — `feat(seller-ui): connect shop voucher management to the real api`

### V07 — Nối web Admin › Voucher sàn (UC-PRO-02)

- **[V07.01]** `marketing.api.ts` (nhóm voucher sàn) — 5 hàm theo `isMocked('admin-vouchers')`; DTO / mapper API 11.14 (`summary` 5 chỉ số, `funded_by`, `platform_share_percent`, `is_coin_reward`); Flash Sale giữ mock.
- **[V07.02]** Modal "Tạo Voucher Sàn" + bảng — lỗi tại ô, `4093` / `4094` khi xóa, nhãn "Quà đổi Điểm" (ẩn nút Xóa, khóa ô mã), tab trạng thái theo `status`.
- **[V07.C1]** COMMIT — `feat(admin-ui): connect platform voucher management to the real api`

### V08 — Nối web Storefront & Kho Voucher (UC-PRO-03)

- **[V08.01]** `voucher.api.ts` — `isMocked('vouchers')`; sửa 2 endpoint lệch spec: `listShopVouchers` → `GET /public/vouchers?shop_id=` (handoff dòng 38), `getVouchersByCodes` → `codes` (D15); thêm `listFeaturedVouchers(limit)` nếu trang chủ đang đọc mock khác; `findVoucherByCode` (6.9) **giữ mock** (C6); ô "nhập mã để lưu" ở Kho gọi thẳng `saveVoucher(code)`.
- **[V08.02]** `listMyVouchers(state, type, page)` theo API 3.14 + `summary.counts` cho 4 tab; lịch sử (Đã sử dụng / Hết hiệu lực) với `used_order_code`; xóa khỏi kho.
- **[V08.03]** Vé "Lưu" ở trang chủ / trang shop / trang chiến dịch / trang sản phẩm — nút chuyển "Đã lưu vào ví" theo `is_saved`; chưa đăng nhập → về đăng nhập kèm `next`; `4091` hiện "Đã lưu" (không báo lỗi đỏ); `GNE-ORD-4031` ẩn nút với tài khoản nội bộ. SSR trang shop / chiến dịch gọi qua `server-http.ts` (không token → không có `is_saved`, island tải lại khi có phiên).
- **[V08.04]** Test — `voucher.mapper.test.ts` (`shop` null / có, FREESHIP, `state`), `voucher.rules.test.ts` (nhãn "Chưa tới giờ" / "Dùng ngay"); `npm run verify`.
- **[V08.C1]** COMMIT — `feat(web): connect public vouchers and the voucher wallet to the real api`

### V09 — Dữ liệu demo, kiểm thử & chạy thật

- **[V09.01]** Seed — `R__seed_demo_50_vouchers.sql` thêm (chạy lại được, `ON CONFLICT (code) DO NOTHING`): 1 voucher sàn **sắp diễn ra** (`valid_from` +2 ngày), 1 voucher shop TechZone phạm vi **ngành** + loại trừ 1 ngành (chèn `voucher_categories` / `voucher_excluded_categories` tra theo slug), 1 voucher shop freeship; `SeedDataIT` xanh.
- **[V09.02]** `docs/api-tests/vouchers.http` — luồng shop (tạo → sửa → tắt), sàn (tạo → sửa → tắt → xóa), khách (xem công khai → lưu → kho → xóa) + lỗi (`4092`, `4094`, `4093`, `4091`, `4041`, `403`, `422`).
- **[V09.03]** `docs/kiem-thu/kich-ban-2026-11-26-voucher.md` — TC-VCH-01…: 3 loại × 3 phạm vi ở Seller; sửa khi đang chạy (ô bị khóa); Nhân bản; Tắt; voucher shop vừa tạo hiện ở trang shop và bộ lọc "Shop có voucher" (`/search`) ngay; Admin tạo / ngân sách / đồng tài trợ / xóa; "Mã giảm giá hot" trang chủ; lưu từ trang chủ / shop / chiến dịch; Kho 4 tab + lịch sử + xóa + nhập mã; quà Điểm không lưu được bằng mã; nhân viên không thấy nút Lưu; 9 theme; khổ hẹp; tắt JS (trang shop SSR vẫn hiện voucher).
- **[V09.04]** Chạy thật — web bản build `PUBLIC_REAL_API=…,seller-vouchers,admin-vouchers,vouchers` + backend `local` + Postgres + Redis + Silo; Chromium (Playwright) với 3 tài khoản (chủ shop, Finance Officer, khách); ghi số ca đạt; lỗi phát hiện sửa ngay (mỗi lỗi một commit `fix(…)`).
- **[V09.C1]** COMMIT — `feat(db): seed upcoming, category-scoped and free shipping vouchers`
- **[V09.C2]** COMMIT — `docs(promotion): add voucher api tests and real-run scenarios`

### V10 — Hoàn thiện use case & tick tiến độ

- **[V10.01]** OpenAPI — đủ 14 endpoint, summary `UC-PRO-01/02/03`.
- **[V10.02]** `docs/web-backend-handoff.md` — nhóm `seller-vouchers`, `admin-vouchers`, `vouchers` đã nối; bỏ dòng 38; ghi `findVoucherByCode` + Flash Sale còn mock (C6).
- **[V10.03]** Usecase.md — *Hiện trạng* UC-PRO-01, 02, 03 → "Đã triển khai (2026-11-…)" + phần để C6.
- **[V10.04]** `CHECKLIST_TIEN_DO_UC.md` — tick 5 lớp 3 UC (ô chưa đạt → `◐` + phần thiếu), Trạng thái ✅, **cột Phụ trách = Nguyễn Ngọc Thiện**, số mục 1 + phân hệ 07, dòng mục 5 *Nhật ký*; mục 3 cụm tìm kiếm: ghi "B5 đã xóa cache `promotion:shop_voucher_vendors` khi đổi voucher shop".
- **[V10.05]** `BAO_CAO_UC_DOC_LAP.md` — bảng trạng thái cụm B5 (3 dòng), dòng *Phụ trách*, dòng UC-CAT-05 (bỏ ghi chú "cụm B5 phải xóa cache" → "đã xóa"), C6 ghi "B5 đã có voucher", hiện trạng ở bảng *Thành viên*.
- **[V10.06]** `Quản Lý UseCase.xlsx` — *Hiện trạng mã nguồn* 3 UC = "Đã triển khai", cột AA = "Nguyễn Ngọc Thiện" (giữ), sheet *Thành viên*.
- **[V10.07]** README ngày (mục 7) — tick dòng plan [3]; khi cả 3 plan xong ghi tổng kết + số tổng quan mới.
- **[V10.C1]** COMMIT — `docs(promotion): record shop vouchers, platform vouchers and voucher wallet as implemented`
- **[V10.08]** Mở PR `feature/voucher-management` (V01–V04, V06–V07) rồi `feature/voucher-wallet` (V05, V08–V10) vào `develop`; PR thứ nhất ghi rõ `BuyerOnlyFilter` không đổi (đổi ở PR hai) và nhờ Nguyễn Quốc Khoa duyệt dòng đó.

---

## 8. Ngoài phạm vi

- Áp voucher / Voucher Picker / Stacking 3 tầng / Điểm khi mua, hoàn lượt khi hủy đơn (UC-PRO-04, 10 — C6): `Voucher.consume` / `restore` có sẵn, plan này không gọi.
- Flash Sale (UC-PRO-05, 06): màn seller / admin giữ mock.
- Thông báo voucher mới tới người theo dõi shop / toàn sàn — Admin dùng *Phát thông báo toàn sàn* (plan [1]).

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Trộn `vendor_profiles.id` với `users.id` | `shop_id` trên API luôn `vendor_profiles.id`; `vouchers.vendor_id` luôn `users.id`; chuyển đổi chỉ trong `VoucherReadModel`; IT kiểm bằng hai shop có id lệch nhau |
| Vòng phụ thuộc khi C6 cài cổng order trong promotion | `promotion` không import `vendor` / `order` (N2); `ModuleBoundaryTest` |
| Bộ lọc "Shop có voucher" cũ 60 giây sau khi đổi voucher | `evictCache()` sau commit (N5); `VoucherCacheEvictionIT` |
| Quà đổi Điểm bị lộ / bị lưu tự do | Loại theo `genie.coin.rewards` ở mọi đường công khai (D12); IT |
| Sửa điều kiện voucher người dùng đã lưu | Luật khóa trường D3 trong entity + test |
| `BuyerOnlyFilter` là file của plan B1 | Sửa một dòng trong PR riêng (V05.C3), nhờ Khoa duyệt |
| Web gọi `/seller/products` (C1) chưa có | D20 dùng `/public/products?shop_id=` tạm thời |
