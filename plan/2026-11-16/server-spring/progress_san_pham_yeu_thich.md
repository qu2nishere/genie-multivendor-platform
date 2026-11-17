# PROGRESS — Quản lý danh sách sản phẩm yêu thích (UC-CAT-08)

| | |
|---|---|
| **Plan** | [plan_san_pham_yeu_thich.md](./plan_san_pham_yeu_thich.md) — chi tiết từng task tra theo mã `Wxx.yy` |
| **Ngày lập** | 2026-11-16 |
| **Tổng** | 32 task + 6 commit, 6 giai đoạn |
| **Nhánh** | `feature/catalog-wishlist` |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-16 — mới lập plan, chưa bắt đầu (0 / 38) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục Ghi chú.
- Task `COMMIT` chỉ tick sau khi qua Cổng kiểm tra (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`.
- Thông điệp commit ghi sẵn bên dưới, thêm ` [#GNE-<số>]` vào cuối khi đã có mã Jira.
- Cập nhật cột "Xong" của bảng Tổng quan và dòng "Cập nhật gần nhất" sau mỗi buổi làm; ghi một dòng vào Nhật ký cuối trang.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | UC-CAT-08 bước 2–3: `POST /api/v1/customer/wishlist` kiểu bật / tắt (toggle); API 3.10: `PUT` / `DELETE /me/wishlist/{product_id}` idempotent + `GET /me/wishlist/product-ids` | ☐ | |
| D2 | Thích sản phẩm không còn bán / không tồn tại | ☐ | |
| D3 | `is_purchasable = false` "khi shop đang nghỉ / sản phẩm hết bán" — trạng thái nghỉ của shop ở module `vendor` | ☐ | |
| D4 | `price`, `default_variant_id`, `image_url` lấy thế nào | ☐ | |
| D5 | Giới hạn số sản phẩm yêu thích | ☐ | |
| D6 | UC mục đích có "nhận thông báo khi sản phẩm giảm giá" | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong | Mã Jira | Nhánh |
|---|---|---:|---:|---:|---|---|
| W00 | Chuẩn bị | 5 | 0 | 0 / 5 | — | `feature/catalog-wishlist` |
| W01 | Miền & truy vấn | 4 | 1 | 0 / 5 | — | `feature/catalog-wishlist` |
| W02 | Thích / bỏ thích & tập ID | 6 | 1 | 0 / 7 | — | `feature/catalog-wishlist` |
| W03 | Danh sách yêu thích | 6 | 1 | 0 / 7 | — | `feature/catalog-wishlist` |
| W04 | Nối giao diện `web/` | 6 | 2 | 0 / 8 | — | `feature/catalog-wishlist` |
| W05 | Hoàn thiện use case | 5 | 1 | 0 / 6 | — | `feature/catalog-wishlist` |
| **Tổng** | | **32** | **6** | **0 / 38** | | |

## W00 — Chuẩn bị

- [ ] **W00.01** Kiểm danh tính git
- [ ] **W00.02** Chốt quyết định D1–D6
- [ ] **W00.03** Tạo nhánh
- [ ] **W00.04** Kiểm dữ liệu thử
- [ ] **W00.05** Đọc màn mock

## W01 — Miền & truy vấn

- [ ] **W01.01** `ErrorCode`
- [ ] **W01.02** `UserWishlist.of(userId, productId, now)`
- [ ] **W01.03** `UserWishlistRepository`
- [ ] **W01.04** `TestDataBuilder`
- [ ] **W01.C1** 🔖 COMMIT `feat(catalog): add wishlist queries and product test fixtures` · hash: `…`

## W02 — Thích / bỏ thích & tập ID

- [ ] **W02.01** `WishlistService.add(userId, productId)`
- [ ] **W02.02** `WishlistService.remove(userId, productId)`
- [ ] **W02.03** `WishlistQueryService.productIds(userId)`
- [ ] **W02.04** `MeWishlistController`
- [ ] **W02.05** `WishlistIT`
- [ ] **W02.06** `WishlistConcurrencyIT`
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
| | |

## Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-16 | Lập plan + progress (32 task, 6 commit, 6 giai đoạn) — chưa bắt đầu làm | — |
