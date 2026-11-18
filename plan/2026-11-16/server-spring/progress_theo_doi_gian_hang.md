# PROGRESS — Khách hàng theo dõi gian hàng (UC-SOC-06)

| | |
|---|---|
| **Plan** | [plan_theo_doi_gian_hang.md](./plan_theo_doi_gian_hang.md) — chi tiết từng task tra theo mã `Fxx.yy` |
| **Ngày lập** | 2026-11-16 |
| **Tổng** | 33 task + 5 commit, 6 giai đoạn |
| **Nhánh** | `feature/social-shop-follow` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-17 — đang làm (31 / 38) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục Ghi chú.
- Task `COMMIT` chỉ tick sau khi qua Cổng kiểm tra (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`.
- Thông điệp commit ghi sẵn bên dưới, thêm ` [#GNE-<số>]` vào cuối khi đã có mã Jira.
- Cập nhật cột "Xong" của bảng Tổng quan và dòng "Cập nhật gần nhất" sau mỗi buổi làm; ghi một dòng vào Nhật ký cuối trang.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | `shop_id` là id nào | ☑ | Như đề xuất — `shop_id` = `vendor_profiles.id`; `shop_follows.vendor_id` = `users.id` chủ shop, đổi qua cổng · 2026-11-17 |
| D2 | UC-SOC-06 bước 1: `POST /api/v1/shops/{vendorId}/follow` (bật / tắt); API 3.11: `PUT` / `DELETE /me/following/{shop_id}` + `GET /me/following`, `GET /me/following/shop-ids` | ☑ | Theo API: `PUT` / `DELETE /me/following/{shop_id}`, `GET /me/following`, `GET /me/following/shop-ids`; sửa Usecase.md ở F05.03 · 2026-11-17 |
| D3 | "Không theo dõi chính shop của mình" chưa có mã lỗi | ☑ | Như đề xuất — mã mới `GNE-SOC-4095` (409) "Không thể theo dõi shop của chính mình" · 2026-11-17 |
| D4 | Shop nào theo dõi được | ☑ | Như đề xuất — chỉ shop `VERIFIED` + chủ `ACTIVE` (không thì `404 GNE-SYS-4040`); đã theo dõi rồi mới bị khóa vẫn hủy được, không hiện trong danh sách · 2026-11-17 |
| D5 | Module `vendor` chưa có tầng application; plan Mở shop & KYC sở hữu `ShopFacade` | ☑ | Như đề xuất — `vendor/shop/application/ShopFollowDirectoryImpl` cài cổng, thêm truy vấn vào `VendorProfileRepository`; không tạo `ShopFacade` · 2026-11-17 |
| D6 | Bước 5 UC: gửi `NEW_VENDOR_VOUCHER` / `FLASH_SALE_REMINDER` tới người theo dõi | ☑ | Như đề xuất — ngoài phạm vi; có `ShopFollowFacade.followerIdsOf` · 2026-11-17 |
| D7 | `GET /me` → `counters.following_shops` | ☑ | Như đề xuất — ngoài phạm vi; có `ShopFollowFacade.countFollowing` · 2026-11-17 |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong | Mã Jira | Nhánh |
|---|---|---:|---:|---:|---|---|
| F00 | Chuẩn bị | 5 | 0 | 5 / 5 | — | `feature/social-shop-follow` |
| F01 | Cổng tra shop & truy vấn | 6 | 1 | 7 / 7 | — | `feature/social-shop-follow` |
| F02 | Theo dõi / hủy theo dõi | 6 | 1 | 7 / 7 | — | `feature/social-shop-follow` |
| F03 | Danh sách shop đang theo dõi | 5 | 1 | 6 / 6 | — | `feature/social-shop-follow` |
| F04 | Nối giao diện `web/` | 6 | 1 | 6 / 7 | — | `feature/social-shop-follow` |
| F05 | Hoàn thiện use case | 5 | 1 | 0 / 6 | — | `feature/social-shop-follow` |
| **Tổng** | | **33** | **5** | **31 / 38** | | |

## F00 — Chuẩn bị

- [x] **F00.01** Kiểm danh tính git
- [x] **F00.02** Chốt quyết định D1–D7
- [x] **F00.03** Tạo nhánh
- [x] **F00.04** Kiểm trạng thái plan Mở shop & KYC
- [x] **F00.05** Đọc màn mock

## F01 — Cổng tra shop & truy vấn

- [x] **F01.01** `ErrorCode`
- [x] **F01.02** `ShopFollowDirectory` (cổng ở `social.follow.application`)
- [x] **F01.03** `VendorProfileRepository` (scope `vendor`)
- [x] **F01.04** `ShopFollowDirectoryImpl` (ở `vendor.shop.application`)
- [x] **F01.05** `ShopFollowRepository`
- [x] **F01.06** `ModuleBoundaryTest`
- [x] **F01.C1** 🔖 COMMIT `feat(vendor): expose shop lookup and follower counter for shop follow` · hash: `58c00d4`

## F02 — Theo dõi / hủy theo dõi

- [x] **F02.01** `ShopFollowService.follow(userId, shopId)`
- [x] **F02.02** `ShopFollowService.unfollow(userId, shopId)`
- [x] **F02.03** `ShopFollowQueryService.shopIds(userId)`
- [x] **F02.04** `MeFollowingController`
- [x] **F02.05** `ShopFollowIT`
- [x] **F02.06** `ShopFollowConcurrencyIT`
- [x] **F02.C1** 🔖 COMMIT `feat(social): follow and unfollow shops without miscounting followers` · hash: `a0ba686`

## F03 — Danh sách shop đang theo dõi

- [x] **F03.01** `ShopFollowQueryService.list(userId, cursor, limit)`
- [x] **F03.02** `summary.total` = số shop đang theo dõi còn hiển thị được.
- [x] **F03.03** `MeFollowingController`
- [x] **F03.04** `ShopFollowFacade`
- [x] **F03.05** `ShopFollowQueryIT`
- [x] **F03.C1** 🔖 COMMIT `feat(social): list followed shops with rating and follower count` · hash: `c0e0aa8`

## F04 — Nối giao diện `web/`

- [x] **F04.01** `shop/infrastructure/shop.api.ts`
- [x] **F04.02** `error-codes.ts` + `vi.json` / `en.json`
- [x] **F04.03** Trạng thái & lỗi
- [x] **F04.04** Trang `/account/following` dùng `summary.total` cho tiêu đề "Shop đang theo dõi (n)".
- [-] **F04.05** Chạy thật hai phía
- [x] **F04.06** `cd web && npm run verify`.
- [ ] **F04.C1** 🔖 COMMIT `fix(web): call shop follow endpoints defined in the api spec` · hash: `…`

## F05 — Hoàn thiện use case

- [ ] **F05.01** `docs/api-tests/shop-follow.http`
- [ ] **F05.02** OpenAPI
- [ ] **F05.03** Tài liệu nguồn
- [ ] **F05.04** Tiến độ
- [ ] **F05.C1** 🔖 COMMIT `docs(social): record shop follow use case as implemented` · hash: `…`
- [ ] **F05.05** Mở PR vào `develop`

---

## Chốt plan — tick tiến độ use case

Làm **ngay khi xong plan** (hoặc xong từng use case), trong file [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../../docs/Document/CHECKLIST_TIEN_DO_UC.md) — cách tick xem mục 0 của file đó.

- [ ] `UC-SOC-06` Khách hàng theo dõi gian hàng — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [ ] Cập nhật số ở mục 1 *Tổng quan* của checklist (Đã triển khai / Đang triển khai / Chưa có backend)
- [ ] Thêm một dòng vào mục 5 *Nhật ký cập nhật* của checklist (ngày · use case · mã commit / PR)
- [ ] Đổi cột *Hiện trạng mã nguồn* của các use case trên trong `Quản Lý UseCase.xlsx`
- [ ] Tick dòng plan này ở bảng *Tiến độ các plan* trong [README](../README.md) mục 6

## Ghi chú

Ghi ở đây: lý do bỏ qua task (`[-]`), chỗ làm khác plan, số đo, việc tồn chuyển sang plan khác.

| Task | Ghi chú |
|---|---|

## Nhật ký
| F00.03 | Nhánh tách từ `feature/catalog-wishlist` (xếp nối tiếp seed → audit → banner → coin → wishlist → follow) |
| F00.04 | `ShopFacade` chưa có (plan Mở shop & KYC chưa làm) → `ShopFollowDirectoryImpl` truy vấn `VendorProfileRepository` trực tiếp; plan [7] tạo `ShopFacade` thì đổi sang gọi nó |
| F00.05 | Web: `listFollowedShops` gọi `GET /me/shop-follows`, `setFollow` `POST` / `DELETE /me/shop-follows` (lệch spec); `FollowsPanel` dùng `id, name, authentic, rating, followerCount`; tiêu đề đếm theo độ dài danh sách |
| F01.02 | Cổng đổi tên phương thức danh sách thành `findVisibleByVendorUserIds` (lọc luôn shop không hiển thị được — D4) |
| F01.04 | Kiểm chủ shop `ACTIVE` theo lô: thêm `UserFacade.activeAmong(ids)` + `UserRepository.findActiveIds` (module `auth`, chỉ bổ sung) — tránh gọi `isActive` 50 lần mỗi trang. Logo: cột lưu khóa đối tượng thì ghép URL bucket `content-assets`, đã là URL thì giữ nguyên |
| F01.06 | `ModuleBoundaryTest` xanh: `vendor` → `social.application` (cổng) và `vendor` → `auth.application` (`UserFacade`) đều qua tầng application; `social` không import `vendor` → không vòng |
| F02.06 | Ca đồng thời đặt trong `ShopFollowIT.concurrentFollowsNeverMiscount` (không tách lớp riêng): 20 request cùng một người → `follower_count` +1; 20 người khác nhau → +20; 40 theo dõi / hủy xen kẽ → `follower_count` = số dòng `shop_follows`. Test cũng khẳng định `shop_id` ≠ `users.id` chủ shop (D1) |
| F03.01 | Mã `GET /me/following` (`ShopFollowQueryService.list`, endpoint trong `MeFollowingController`) đã đi cùng commit F02 vì chung lớp; commit F03 thêm `ShopFollowFacade` + `ShopFollowQueryIT`. Thông tin shop nạp một lần / trang qua cổng (`findByUserIdIn` + `activeAmong` — 2 câu cho cả trang) |
| F03.02 | `summary.total` = số shop CÒN hiển thị được trong tối đa 1.000 shop đang theo dõi (shop bị khóa không đếm — D4); `ShopFollowFacade.countFollowing` là bộ đếm thô (đếm cả shop bị khóa) cho `GET /me` sau này |
| F04.01 | Cờ `isMocked('follow')` chỉ cho nhóm theo dõi; `getShop` / `listShops` công khai giữ `USE_MOCK`. `listFollowedShops` trả `{items, total}` (DTO `FollowedShopDto` + mapper `fromFollowedShop`, có test); thêm `fetchFollowedShopIds` + hook `useFollowedShopIds` — nút Theo dõi dùng tập ID (đúng cả khi theo dõi > 50 shop); `setFollow` → `PUT` / `DELETE /me/following/{id}` |
| F04.03 | Nút Theo dõi giữ kiểu cập nhật hiện có (tải lại tập ID sau khi đổi — không cộng / trừ lạc quan số người theo dõi vì trang shop còn mock). `404` → toast `shop.follow.gone` "Shop này hiện không còn hoạt động"; `409 GNE-SOC-4095` qua `errors.*`; chưa đăng nhập → lời nhắc đăng nhập (có sẵn) |
| F04.04 | Tiêu đề "Shop đang theo dõi (n)" và số đếm ở menu Tài khoản (`AccountNav`) dùng `total` |
| F04.05 | `[-]` Phần trình duyệt chờ người dùng (CLAUDE.md). Đã chạy thật phía API trên CSDL mới `genie_follow`: `PUT` 2 lần → `follower_count` 1; shop của chính mình → `409 GNE-SOC-4095`; shop lạ → 404; `shop-ids` `[2, 1]`; danh sách đủ `$ShopMini` + `summary.total 2`; `DELETE` 2 lần → 204, bộ đếm = số dòng `shop_follows` ở mọi shop. Cách chạy giao diện: `PUBLIC_REAL_API=auth,follow` |
| F04.06 | Cổng web: lint 0, astro check 0 lỗi, tsc 0, vitest 320/320, build OK, i18n OK (3271 khóa); E2E `guest` + `orders` 10/10 |

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-16 | Lập plan + progress (33 task, 5 commit, 6 giai đoạn) — chưa bắt đầu làm | — |
