# PLAN — Quản lý danh sách sản phẩm yêu thích (UC-CAT-08)

| | |
|---|---|
| **Ngày lập** | 2026-11-16 |
| **Thể loại** | server-spring (module `catalog`, feature `wishlist`) + nối `web/` (feature `product`, `account`) |
| **Use case** | Quản lý danh sách sản phẩm yêu thích — Wishlist (UC-CAT-08) — Sprint 6 theo implement 1.2, làm sớm vì độc lập |
| **Tài liệu chuẩn** | UC phân hệ 02 (UC-CAT-08) · API 3.10, 0.3, 0.4, 15, 17.5 b · ENTITY 2.5 (`UserWishlist`), 2.2 (`Product`), 2.3 (`ProductVariant`), 2.4 (`ProductImage`) · LUU_TRU 5.6 · TS 4, 8, 9, 14 — xem [README](../README.md) |
| **Phụ thuộc** | Dữ liệu sản phẩm để thử tay: plan [seed](../db/plan_seed_du_lieu_mau.md) S02–S04 (test tự động **không** phụ thuộc seed) |
| **Theo dõi tiến độ** | [progress_san_pham_yeu_thich.md](./progress_san_pham_yeu_thich.md) — cùng mã task `Wxx.yy` |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |

---

## 0. Hiện trạng đã khảo sát

| Phần | Có gì | Thiếu |
|---|---|---|
| `catalog/wishlist/domain` | `UserWishlist` (`userId`, `productId`, `createdAt`; `UNIQUE (user_id, product_id)`), `UserWishlistRepository` (rỗng) | `api`, `application`; truy vấn; factory |
| `catalog/product/domain` | `Product` (`status`, `basePrice`, `vendorId`, `slug`…), `ProductVariant`, `ProductImage` + repository rỗng | Chưa có service sản phẩm nào — plan này chỉ **đọc** các bảng đó trong phạm vi module `catalog` |
| CSDL | `user_wishlists` (`V2__catalog.sql`) | — (không cần migration) |
| `common/error/ErrorCode` | Chưa có `GNE-CAT-4042` | Thêm (đã có ở API 15 + web) |
| `web/features/product` | `wishlist.api.ts`: `fetchWishlistIds` gọi `GET /me/wishlist/ids`, `setWishlisted` gọi `POST /me/wishlist` / `DELETE /me/wishlist/{id}` — **2 chỗ lệch spec** (API 17.5 b); nút tim ở thẻ sản phẩm + trang chi tiết (island `client:idle`) | Sửa đường dẫn, phương thức; nối thật |
| `web/features/account` | Mục Tài khoản › Sản phẩm yêu thích (`/account/wishlist`) dựng danh sách từ ID + `listProductsByIds` (mock) | Gọi `GET /me/wishlist` (có `summary.total`, `is_purchasable`, `default_variant_id`) |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Package `com.genie.catalog.wishlist.{api,application,domain}`; service mở `@Transactional`; controller không mở transaction | TS 4.1–4.2 |
| N2 | Thích / bỏ thích **idempotent**: `PUT` nhiều lần vẫn `204` và chỉ có một dòng; `DELETE` dòng không tồn tại vẫn `204` | API 3.10 |
| N3 | Chỉ thao tác trên danh sách của **chính người gọi** (`userId` lấy từ `@CurrentUser`, không nhận từ request) — chống IDOR | TS 9 |
| N4 | Trong module `catalog`, feature `wishlist` được đọc `Product` / `ProductVariant` / `ProductImage` qua repository; thông tin của module khác (shop đang nghỉ) chỉ qua **cổng đảo ngược** | TS 4.4; README mục 3 |
| N5 | Ảnh trả URL ghép từ `PUBLIC_S3_URL` + khóa đối tượng; giá là số nguyên VND; client tự định dạng | API 0.5, 0.6; LUU_TRU 5.6 |
| N6 | Danh sách ID để tô tim tách khỏi danh sách sản phẩm công khai (để trang công khai cache được) | API nguyên tắc 3 |
| N7 | JSON snake_case, vỏ `ApiResponse`, phân trang cursor; `@Operation(summary = "UC-CAT-08 …")` | TS 8 |
| N8 | Test: unit + `*IT` Testcontainers (PG) có ca đồng thời | TS 14 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Nguồn lệch | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | UC-CAT-08 bước 2–3: `POST /api/v1/customer/wishlist` kiểu bật / tắt (toggle); API 3.10: `PUT` / `DELETE /me/wishlist/{product_id}` idempotent + `GET /me/wishlist/product-ids` | UC vs API | Theo **API** (toggle không idempotent, bấm đúp dễ sai trạng thái); sửa câu trong Usecase.md (W05.03) | ☐ |
| D2 | Thích sản phẩm không còn bán / không tồn tại | API 15 | `PUT`: sản phẩm không tồn tại hoặc trạng thái không phải `PUBLISHED` → `404 GNE-CAT-4042`. Sản phẩm đã nằm trong danh sách rồi mới bị ẩn / khóa → vẫn hiện trong danh sách với `is_purchasable = false`, bỏ thích bình thường | ☐ |
| D3 | `is_purchasable = false` "khi shop đang nghỉ / sản phẩm hết bán" — trạng thái nghỉ của shop ở module `vendor` | API 3.10 | Cổng `WishlistShopAvailability` (khai trong `catalog.wishlist.application`): `Set<Long> vendorIdsOnVacation(Collection<Long> vendorIds)`; mặc định (chưa có cài đặt) trả rỗng. Module `vendor` cài khi plan Mở shop & KYC có `ShopFacade`. `is_purchasable` = `PUBLISHED` **và** có ít nhất 1 SKU còn tồn **và** shop không nghỉ | ☐ |
| D4 | `price`, `default_variant_id`, `image_url` lấy thế nào | API 3.10 | `price` = giá thấp nhất của các SKU đang bán (không có SKU thì `base_price`); `default_variant_id` = SKU còn tồn có giá thấp nhất (hết hàng → SKU đầu tiên); `image_url` = ảnh chính của sản phẩm | ☐ |
| D5 | Giới hạn số sản phẩm yêu thích | (không tài liệu nào ghi) | Không giới hạn ở TLCN; `GET /me/wishlist/product-ids` trả tối đa 1.000 ID mới nhất | ☐ |
| D6 | UC mục đích có "nhận thông báo khi sản phẩm giảm giá" | UC-CAT-08 | **Ngoài phạm vi** (cần phân hệ thông báo); cung cấp sẵn `WishlistFacade.userIdsWishing(productId)` để phân hệ sau dùng | ☐ |

---

## 3. Cây file sẽ tạo / sửa

```
server-spring/src/main/java/com/genie/
├── common/error/ErrorCode.java                 (sửa) + PRODUCT_UNAVAILABLE GNE-CAT-4042
└── catalog/wishlist/
    ├── domain/       UserWishlist (sửa: factory of) · UserWishlistRepository (sửa: truy vấn)
    ├── application/  WishlistService · WishlistQueryService · WishlistShopAvailability (cổng — D3) · WishlistFacade (D6)
    │                 WishlistItemView (record)
    └── api/          MeWishlistController · WishlistItemResponse (record)
server-spring/src/test/java/com/genie/catalog/
    WishlistIT · WishlistQueryIT · WishlistConcurrencyIT
server-spring/src/test/java/com/genie/support/TestDataBuilder.java   (sửa: dựng danh mục / shop / sản phẩm / SKU / ảnh cho test)
web/src/features/product/infrastructure/wishlist.api.ts              (sửa)
web/src/features/account/…                                           (sửa — trang danh sách yêu thích gọi GET /me/wishlist)
docs/Document/…  Usecase.md · API_SPEC 3.10, 17.5 · CHECKLIST_TIEN_DO_UC.md · Quản Lý UseCase.xlsx
docs/web-backend-handoff.md · docs/api-tests/wishlist.http
```

---

## 4. Bảng ánh xạ Use case → API → Dữ liệu

| Bước UC | Endpoint (API 3.10) | Bảng |
|---|---|---|
| Tô tim trên thẻ sản phẩm / trang chi tiết | `GET /me/wishlist/product-ids` → `[2, 13]` | `user_wishlists` (đọc) |
| Bấm tim — thích | `PUT /me/wishlist/{product_id}` → `204` | `user_wishlists` (ghi), `products` (đọc) |
| Bấm tim lần 2 — bỏ thích | `DELETE /me/wishlist/{product_id}` → `204` | `user_wishlists` (xóa) |
| Tài khoản › Sản phẩm yêu thích | `GET /me/wishlist?cursor&limit=20` (+ `summary.total`) | `user_wishlists`, `products`, `product_variants`, `product_images` (đọc) |
| "Thêm vào giỏ" từ danh sách | `POST /me/cart/items` với `default_variant_id` — **thuộc UC giỏ hàng, ngoài plan** | — |

**Mã lỗi thêm vào `ErrorCode`:** `GNE-CAT-4042` (404 — sản phẩm không còn bán). Dùng lại: `GNE-SEC-4010`, `GNE-VAL-4220`.
**Không có migration, không thêm `UploadPurpose` / `AuditAction`.**

---

## 5. Nhánh, commit, PR

- Nhánh: `feature/catalog-wishlist` tách từ `develop` (có mã Jira: `feature/GNE-<số>-catalog-wishlist`). Một PR backend + một PR web (hoặc gộp nếu ≤ ~400 dòng).
- Scope commit: `common`, `catalog`, `web`, `docs`.
- Quy ước chung: [README mục 5](../README.md).

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — unit + IT + ArchUnit.
2. Swagger UI hiện 4 endpoint, summary có `UC-CAT-08`.
3. Thử tay `docs/api-tests/wishlist.http` với `mai.tran@gmail.com` trên CSDL đã seed sản phẩm.
4. Nếu đụng `web/`: `cd web && npm run verify`.
5. Tick task ở progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Wxx.yy` = task; `Wxx.Cn` = task commit.

### W00 — Chuẩn bị

- **[W00.01]** Kiểm danh tính git — `git config user.name` / `user.email`.
- **[W00.02]** Chốt quyết định D1–D6 — ghi vào đầu file progress.
- **[W00.03]** Tạo nhánh — `git switch develop && git pull && git switch -c feature/catalog-wishlist`.
- **[W00.04]** Kiểm dữ liệu thử — CSDL local đã có sản phẩm seed (`SELECT count(*) FROM products WHERE status = 'PUBLISHED'`); chưa có thì chạy `scripts/seed/seed.sh` (plan seed) hoặc chèn tay 2 sản phẩm theo hướng dẫn trong Ghi chú progress.
- **[W00.05]** Đọc màn mock — nút tim ở `ProductCard`, trang `/p/…`, `/account/wishlist` (tiêu đề có số lượng, nhãn hết hàng, nút bỏ thích, nút thêm vào giỏ bị khóa khi không mua được) — ghi trường giao diện cần.

### W01 — Miền & truy vấn

- **[W01.01]** `ErrorCode` — thêm `GNE-CAT-4042` (thông điệp theo API 15).
- **[W01.02]** `UserWishlist.of(userId, productId, now)` — factory (ghi vào ENTITY Phụ lục E).
- **[W01.03]** `UserWishlistRepository` — `insertIfAbsent(userId, productId, now)` (native `INSERT … ON CONFLICT (user_id, product_id) DO NOTHING`, trả số dòng — N2); `deleteByUserIdAndProductId` (trả số dòng); `findProductIds(userId, limit)` sắp `created_at DESC`; `countByUserId`; trang keyset `(created_at DESC, id DESC)`; `findUserIdsByProductId` (D6).
- **[W01.04]** `TestDataBuilder` — thêm hàm dựng nhanh: danh mục, chủ shop (`users` VENDOR + `vendor_profiles` VERIFIED), sản phẩm `PUBLISHED`, SKU có / không tồn, ảnh chính (dùng lại cho các plan catalog sau; chỉ chèn cột bắt buộc).
- **[W01.C1]** COMMIT — `feat(catalog): add wishlist queries and product test fixtures`

### W02 — Thích / bỏ thích & tập ID

- **[W02.01]** `WishlistService.add(userId, productId)` — tra sản phẩm: không có hoặc không `PUBLISHED` → `404 GNE-CAT-4042` (D2); `insertIfAbsent`; đã có sẵn → không lỗi (N2).
- **[W02.02]** `WishlistService.remove(userId, productId)` — xóa nếu có; không có vẫn thành công (N2); **không** kiểm trạng thái sản phẩm (sản phẩm đã ẩn vẫn bỏ thích được).
- **[W02.03]** `WishlistQueryService.productIds(userId)` — tối đa 1.000 ID mới nhất (D5).
- **[W02.04]** `MeWishlistController` — `GET /api/v1/me/wishlist/product-ids`, `PUT /api/v1/me/wishlist/{product_id}` (`204`), `DELETE /api/v1/me/wishlist/{product_id}` (`204`); `product_id` là số dương (sai → `422`); `@Operation(summary = "UC-CAT-08 …")`; `Cache-Control: no-store`.
- **[W02.05]** `WishlistIT` — thích rồi `product-ids` chứa ID; `PUT` 2 lần vẫn 1 dòng; `DELETE` 2 lần đều `204`; thích sản phẩm không tồn tại / `DRAFT` / `HIDDEN` → `404 GNE-CAT-4042`; sản phẩm bị ẩn sau khi đã thích vẫn bỏ thích được; chưa đăng nhập → `401`; danh sách của user A không lẫn user B (N3).
- **[W02.06]** `WishlistConcurrencyIT` — 20 luồng cùng `PUT` một sản phẩm → 20 lần `204`, đúng 1 dòng; xen kẽ `PUT` / `DELETE` không sinh lỗi 500.
- **[W02.C1]** COMMIT — `feat(catalog): add and remove wishlist products idempotently`

### W03 — Danh sách yêu thích

- **[W03.01]** `WishlistShopAvailability` — cổng D3 + cài đặt mặc định `@ConditionalOnMissingBean` trả tập rỗng; Javadoc ghi module `vendor` sẽ cài (thêm một dòng vào CLAUDE.md mục 9 ở W05).
- **[W03.02]** `WishlistQueryService.list(userId, cursor, limit)` — lấy trang `user_wishlists`; nạp sản phẩm + SKU + ảnh chính **theo lô** (một truy vấn cho mỗi bảng theo tập `product_id` của trang — tránh N+1); tính `price`, `default_variant_id`, `image_url` (D4, N5), `is_purchasable` (D3); sản phẩm đã bị xóa cứng thì bỏ qua dòng đó.
- **[W03.03]** `summary.total` = `countByUserId` (API 3.10); phân trang cursor theo API 0.4.
- **[W03.04]** `MeWishlistController` — `GET /api/v1/me/wishlist?cursor&limit=20` (`limit` 1–50).
- **[W03.05]** `WishlistQueryIT` — thứ tự mới thích trước; `total` đúng; sản phẩm nhiều SKU lấy giá thấp nhất và SKU còn tồn làm mặc định; hết sạch tồn → `is_purchasable = false`; sản phẩm `HIDDEN` → `is_purchasable = false`; shop nghỉ (cài `WishlistShopAvailability` giả trong test) → `false`; phân trang 2 trang không trùng; số truy vấn SQL cho một trang 20 dòng không vượt ngưỡng đặt trong test (chống N+1).
- **[W03.06]** `WishlistFacade.userIdsWishing(productId)` (D6) — có test đơn giản; chưa có nơi gọi.
- **[W03.C1]** COMMIT — `feat(catalog): list wishlist products with price and purchasable flag`

### W04 — Nối giao diện `web/`

- **[W04.01]** `product/infrastructure/wishlist.api.ts` — `fetchWishlistIds`: đổi `GET /me/wishlist/ids` → `GET /me/wishlist/product-ids`; `setWishlisted`: `PUT /me/wishlist/{id}` khi thích, `DELETE` khi bỏ (API 17.5 b); thay `USE_MOCK` bằng `isMocked('wishlist')`.
- **[W04.02]** Thêm `listWishlist(cursor)` (DTO snake_case + mapper + nhánh mock dùng dữ liệu hiện có) gọi `GET /me/wishlist`; trang `/account/wishlist` dùng hàm này thay cho ghép ID + `listProductsByIds`; tiêu đề lấy `summary.total`.
- **[W04.03]** Trạng thái & lỗi — cập nhật lạc quan nút tim (đổi ngay, lỗi thì trả lại + toast); `404 GNE-CAT-4042` → thông báo "Sản phẩm không còn bán" và bỏ tim; chưa đăng nhập → mở lời nhắc đăng nhập (đã có `auth.prompt`).
- **[W04.04]** Test Vitest cho mapper + quy tắc cập nhật lạc quan (nếu đặt ở `domain` / `application`).
- **[W04.05]** Chạy thật hai phía — `PUBLIC_REAL_API=auth,wishlist`: đăng nhập `mai.tran@gmail.com`, thích từ thẻ sản phẩm và trang chi tiết, tải lại trang tim vẫn đầy; `/account/wishlist` hiện đúng số lượng, giá, nhãn hết hàng; bỏ thích; mở 2 tab kiểm đồng bộ sau tải lại.
- **[W04.06]** `cd web && npm run verify`.
- **[W04.C1]** COMMIT — `fix(web): call wishlist endpoints defined in the api spec`
- **[W04.C2]** COMMIT — `feat(web): load wishlist page from the real api`

### W05 — Hoàn thiện use case

- **[W05.01]** `docs/api-tests/wishlist.http` — kịch bản đúng (thích → ids → danh sách → bỏ thích) + lỗi (`404 4042`, `401`).
- **[W05.02]** OpenAPI — kiểm `/v3/api-docs`.
- **[W05.03]** Tài liệu nguồn — Usecase.md UC-CAT-08: bước 2–3 theo API (D1), dòng *Hiện trạng*; API 3.10 ghi quy tắc `price` / `default_variant_id` / `is_purchasable` (D3, D4), 17.5 b đánh dấu 2 dòng wishlist đã sửa; ENTITY Phụ lục E thêm `UserWishlist.of`; lịch sử phiên bản.
- **[W05.04]** Tiến độ — `CHECKLIST_TIEN_DO_UC.md` (tick 5 lớp, Nhật ký), `Quản Lý UseCase.xlsx` → "Đã triển khai"; `docs/web-backend-handoff.md` (bỏ `GET /me/wishlist/ids` khỏi bảng lệch, đánh dấu đã nối); `CLAUDE.md` mục 2 + mục 9 (cổng `WishlistShopAvailability` cần module `vendor` cài).
- **[W05.C1]** COMMIT — `docs(catalog): record wishlist use case as implemented`
- **[W05.05]** Mở PR vào `develop` — mẫu PR (Ảnh hưởng CSDL: không có).

---

## 8. Ngoài phạm vi

- Thông báo khi sản phẩm yêu thích giảm giá (D6).
- Thêm vào giỏ từ danh sách yêu thích — thuộc Quản lý giỏ hàng mua sắm cá nhân (UC-ORD-01); nút vẫn chạy mock cho tới khi giỏ hàng nối thật.
- Bộ đếm "lượt thích" trên trang sản phẩm; gợi ý theo sản phẩm yêu thích — DATN.

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Bấm tim liên tục gây lỗi trùng khóa | `INSERT … ON CONFLICT DO NOTHING` (W01.03) + test 20 luồng (W02.06) |
| N+1 khi dựng danh sách | Nạp theo lô + khẳng định số truy vấn trong test (W03.05) |
| Cột của `products` / `product_variants` khác giả định (tên cột tồn, giá) | W01.04 đọc migration `V2` trước khi viết builder; `SchemaValidationIT` bảo vệ |
| Plan catalog sau này (sản phẩm công khai) viết lại phần đọc sản phẩm | Truy vấn nằm gọn trong `wishlist/application`; khi có `ProductFacade` nội bộ thì đổi sang dùng — không đổi API |
| Trang công khai có cache mà tim sai trạng thái | Tim lấy từ `product-ids` riêng theo người dùng (N6), không nhúng vào dữ liệu công khai |
