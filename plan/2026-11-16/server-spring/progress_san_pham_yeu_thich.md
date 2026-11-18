# PROGRESS — Quản lý danh sách sản phẩm yêu thích (UC-CAT-08)

| | |
|---|---|
| **Plan** | [plan_san_pham_yeu_thich.md](./plan_san_pham_yeu_thich.md) — chi tiết từng task tra theo mã `Wxx.yy` |
| **Ngày lập** | 2026-11-16 |
| **Tổng** | 32 task + 6 commit, 6 giai đoạn |
| **Nhánh** | `feature/catalog-wishlist` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-17 — đang làm (16 / 38) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục Ghi chú.
- Task `COMMIT` chỉ tick sau khi qua Cổng kiểm tra (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`.
- Thông điệp commit ghi sẵn bên dưới, thêm ` [#GNE-<số>]` vào cuối khi đã có mã Jira.
- Cập nhật cột "Xong" của bảng Tổng quan và dòng "Cập nhật gần nhất" sau mỗi buổi làm; ghi một dòng vào Nhật ký cuối trang.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | UC-CAT-08 bước 2–3: `POST /api/v1/customer/wishlist` kiểu bật / tắt (toggle); API 3.10: `PUT` / `DELETE /me/wishlist/{product_id}` idempotent + `GET /me/wishlist/product-ids` | ☑ | Theo API: `PUT` / `DELETE /me/wishlist/{product_id}` idempotent + `GET /me/wishlist/product-ids`; sửa Usecase.md ở W05.03 · 2026-11-17 |
| D2 | Thích sản phẩm không còn bán / không tồn tại | ☑ | Như đề xuất — thích sản phẩm không tồn tại / không `PUBLISHED` → `404 GNE-CAT-4042`; đã thích rồi mới bị ẩn → vẫn hiện, `is_purchasable = false`, bỏ thích bình thường · 2026-11-17 |
| D3 | `is_purchasable = false` "khi shop đang nghỉ / sản phẩm hết bán" — trạng thái nghỉ của shop ở module `vendor` | ☑ | Như đề xuất — cổng `WishlistShopAvailability` (mặc định không shop nào nghỉ); `is_purchasable` = `PUBLISHED` + còn SKU có tồn + shop không nghỉ · 2026-11-17 |
| D4 | `price`, `default_variant_id`, `image_url` lấy thế nào | ☑ | Như đề xuất — `price` = giá thấp nhất của SKU chưa xóa (không có SKU → `base_price`); `default_variant_id` = SKU còn tồn rẻ nhất (hết hàng → SKU đầu tiên); `image_url` = ảnh chính (không có cờ chính → ảnh `display_order` nhỏ nhất) · 2026-11-17 |
| D5 | Giới hạn số sản phẩm yêu thích | ☑ | Như đề xuất — không giới hạn; `product-ids` trả tối đa 1.000 ID mới nhất · 2026-11-17 |
| D6 | UC mục đích có "nhận thông báo khi sản phẩm giảm giá" | ☑ | Như đề xuất — ngoài phạm vi; có sẵn `WishlistFacade.userIdsWishing(productId)` · 2026-11-17 |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong | Mã Jira | Nhánh |
|---|---|---:|---:|---:|---|---|
| W00 | Chuẩn bị | 5 | 0 | 5 / 5 | — | `feature/catalog-wishlist` |
| W01 | Miền & truy vấn | 4 | 1 | 5 / 5 | — | `feature/catalog-wishlist` |
| W02 | Thích / bỏ thích & tập ID | 6 | 1 | 6 / 7 | — | `feature/catalog-wishlist` |
| W03 | Danh sách yêu thích | 6 | 1 | 0 / 7 | — | `feature/catalog-wishlist` |
| W04 | Nối giao diện `web/` | 6 | 2 | 0 / 8 | — | `feature/catalog-wishlist` |
| W05 | Hoàn thiện use case | 5 | 1 | 0 / 6 | — | `feature/catalog-wishlist` |
| **Tổng** | | **32** | **6** | **16 / 38** | | |

## W00 — Chuẩn bị

- [x] **W00.01** Kiểm danh tính git
- [x] **W00.02** Chốt quyết định D1–D6
- [x] **W00.03** Tạo nhánh
- [-] **W00.04** Kiểm dữ liệu thử
- [x] **W00.05** Đọc màn mock

## W01 — Miền & truy vấn

- [x] **W01.01** `ErrorCode`
- [x] **W01.02** `UserWishlist.of(userId, productId, now)`
- [x] **W01.03** `UserWishlistRepository`
- [x] **W01.04** `TestDataBuilder`
- [x] **W01.C1** 🔖 COMMIT `feat(catalog): add wishlist queries and product test fixtures` · hash: `d74ed73`

## W02 — Thích / bỏ thích & tập ID

- [x] **W02.01** `WishlistService.add(userId, productId)`
- [x] **W02.02** `WishlistService.remove(userId, productId)`
- [x] **W02.03** `WishlistQueryService.productIds(userId)`
- [x] **W02.04** `MeWishlistController`
- [x] **W02.05** `WishlistIT`
- [x] **W02.06** `WishlistConcurrencyIT`
- [ ] **W02.C1** 🔖 COMMIT `feat(catalog): add and remove wishlist products idempotently` · hash: `…`

## W03 — Danh sách yêu thích

- [ ] **W03.01** `WishlistShopAvailability`
- [ ] **W03.02** `WishlistQueryService.list(userId, cursor, limit)`
- [ ] **W03.03** `summary.total` = `countByUserId` (API 3.10); phân trang cursor theo API 0.4.
- [ ] **W03.04** `MeWishlistController`
- [ ] **W03.05** `WishlistQueryIT`
- [ ] **W03.06** `WishlistFacade.userIdsWishing(productId)` (D6)
- [ ] **W03.C1** 🔖 COMMIT `feat(catalog): list wishlist products with price and purchasable flag` · hash: `…`

## W04 — Nối giao diện `web/`

- [ ] **W04.01** `product/infrastructure/wishlist.api.ts`
- [ ] **W04.02** Thêm `listWishlist(cursor)` (DTO snake_case + mapper + nhánh mock dùng dữ liệu hiện có) gọi `GET /me/wishlist`; trang `/account/wishlist` dùng hàm này thay cho ghép ID + `listProductsByIds`; tiêu đề lấy `summary.total`.
- [ ] **W04.03** Trạng thái & lỗi
- [ ] **W04.04** Test Vitest cho mapper + quy tắc cập nhật lạc quan (nếu đặt ở `domain` / `application`).
- [ ] **W04.05** Chạy thật hai phía
- [ ] **W04.06** `cd web && npm run verify`.
- [ ] **W04.C1** 🔖 COMMIT `fix(web): call wishlist endpoints defined in the api spec` · hash: `…`
- [ ] **W04.C2** 🔖 COMMIT `feat(web): load wishlist page from the real api` · hash: `…`

## W05 — Hoàn thiện use case

- [ ] **W05.01** `docs/api-tests/wishlist.http`
- [ ] **W05.02** OpenAPI
- [ ] **W05.03** Tài liệu nguồn
- [ ] **W05.04** Tiến độ
- [ ] **W05.C1** 🔖 COMMIT `docs(catalog): record wishlist use case as implemented` · hash: `…`
- [ ] **W05.05** Mở PR vào `develop`

---

## Chốt plan — tick tiến độ use case

Làm **ngay khi xong plan** (hoặc xong từng use case), trong file [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../../docs/Document/CHECKLIST_TIEN_DO_UC.md) — cách tick xem mục 0 của file đó.

- [ ] `UC-CAT-08` Quản lý danh sách sản phẩm yêu thích — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [ ] Cập nhật số ở mục 1 *Tổng quan* của checklist (Đã triển khai / Đang triển khai / Chưa có backend)
- [ ] Thêm một dòng vào mục 5 *Nhật ký cập nhật* của checklist (ngày · use case · mã commit / PR)
- [ ] Đổi cột *Hiện trạng mã nguồn* của các use case trên trong `Quản Lý UseCase.xlsx`
- [ ] Tick dòng plan này ở bảng *Tiến độ các plan* trong [README](../README.md) mục 6

## Ghi chú

Ghi ở đây: lý do bỏ qua task (`[-]`), chỗ làm khác plan, số đo, việc tồn chuyển sang plan khác.

| Task | Ghi chú |
|---|---|

## Nhật ký
| W00.03 | Nhánh tách từ `feature/promotion-coin-checkin` (xếp nối tiếp seed → audit → banner → coin → wishlist) |
| W00.04 | `[-]` Không phụ thuộc: test tự dựng dữ liệu bằng `TestDataBuilder` (W01.04); lần chạy API thật (W04.05) tự chèn sản phẩm vào CSDL mới (dữ liệu mẫu nằm ở nhánh seed) |
| W00.05 | Web: `wishlist.api.ts` gọi `GET /me/wishlist/ids` + `POST /me/wishlist` / `DELETE` (lệch spec); trang `/account/wishlist` ghép ID + `listProductsByIds` — sẽ đổi sang `GET /me/wishlist` |
| W01.04 | `TestDataBuilder` (trước là khung rỗng) dựng bằng `JdbcTemplate`: `category`, `vendor` (users VENDOR + vendor_profiles VERIFIED), `shopIdOf`, `product`, `variant`, `primaryImage`, `setStatus` — khóa tự nhiên ngẫu nhiên, test không đụng nhau. `ProductVariant` không có `@SQLRestriction` thật → truy vấn tự lọc `deleted_at IS NULL` |
| W02.04 | `product_id` kiểm bằng `@Positive` trên `@PathVariable` (Spring 6.1 tự kiểm tham số) — `0` hoặc chữ → `422 GNE-VAL-4220` |
| W02.06 | Ca đồng thời đặt luôn trong `WishlistIT` (`twentyConcurrentLikesKeepOneRowAndMixedTogglesNeverFail`) thay vì lớp `WishlistConcurrencyIT` riêng — dùng chung dữ liệu dựng sẵn; 20 `PUT` song song → 20 lần `204`, 1 dòng; 20 `PUT` / `DELETE` xen kẽ → toàn `204` |

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-16 | Lập plan + progress (32 task, 6 commit, 6 giai đoạn) — chưa bắt đầu làm | — |
