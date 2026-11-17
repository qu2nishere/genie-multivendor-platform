# PROGRESS — Khách hàng theo dõi gian hàng (UC-SOC-06)

| | |
|---|---|
| **Plan** | [plan_theo_doi_gian_hang.md](./plan_theo_doi_gian_hang.md) — chi tiết từng task tra theo mã `Fxx.yy` |
| **Ngày lập** | 2026-11-16 |
| **Tổng** | 33 task + 5 commit, 6 giai đoạn |
| **Nhánh** | `feature/social-shop-follow` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
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
| D1 | `shop_id` là id nào | ☐ | |
| D2 | UC-SOC-06 bước 1: `POST /api/v1/shops/{vendorId}/follow` (bật / tắt); API 3.11: `PUT` / `DELETE /me/following/{shop_id}` + `GET /me/following`, `GET /me/following/shop-ids` | ☐ | |
| D3 | "Không theo dõi chính shop của mình" chưa có mã lỗi | ☐ | |
| D4 | Shop nào theo dõi được | ☐ | |
| D5 | Module `vendor` chưa có tầng application; plan Mở shop & KYC sở hữu `ShopFacade` | ☐ | |
| D6 | Bước 5 UC: gửi `NEW_VENDOR_VOUCHER` / `FLASH_SALE_REMINDER` tới người theo dõi | ☐ | |
| D7 | `GET /me` → `counters.following_shops` | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong | Mã Jira | Nhánh |
|---|---|---:|---:|---:|---|---|
| F00 | Chuẩn bị | 5 | 0 | 0 / 5 | — | `feature/social-shop-follow` |
| F01 | Cổng tra shop & truy vấn | 6 | 1 | 0 / 7 | — | `feature/social-shop-follow` |
| F02 | Theo dõi / hủy theo dõi | 6 | 1 | 0 / 7 | — | `feature/social-shop-follow` |
| F03 | Danh sách shop đang theo dõi | 5 | 1 | 0 / 6 | — | `feature/social-shop-follow` |
| F04 | Nối giao diện `web/` | 6 | 1 | 0 / 7 | — | `feature/social-shop-follow` |
| F05 | Hoàn thiện use case | 5 | 1 | 0 / 6 | — | `feature/social-shop-follow` |
| **Tổng** | | **33** | **5** | **0 / 38** | | |

## F00 — Chuẩn bị

- [ ] **F00.01** Kiểm danh tính git
- [ ] **F00.02** Chốt quyết định D1–D7
- [ ] **F00.03** Tạo nhánh
- [ ] **F00.04** Kiểm trạng thái plan Mở shop & KYC
- [ ] **F00.05** Đọc màn mock

## F01 — Cổng tra shop & truy vấn

- [ ] **F01.01** `ErrorCode`
- [ ] **F01.02** `ShopFollowDirectory` (cổng ở `social.follow.application`)
- [ ] **F01.03** `VendorProfileRepository` (scope `vendor`)
- [ ] **F01.04** `ShopFollowDirectoryImpl` (ở `vendor.shop.application`)
- [ ] **F01.05** `ShopFollowRepository`
- [ ] **F01.06** `ModuleBoundaryTest`
- [ ] **F01.C1** 🔖 COMMIT `feat(vendor): expose shop lookup and follower counter for shop follow` · hash: `…`

## F02 — Theo dõi / hủy theo dõi

- [ ] **F02.01** `ShopFollowService.follow(userId, shopId)`
- [ ] **F02.02** `ShopFollowService.unfollow(userId, shopId)`
- [ ] **F02.03** `ShopFollowQueryService.shopIds(userId)`
- [ ] **F02.04** `MeFollowingController`
- [ ] **F02.05** `ShopFollowIT`
- [ ] **F02.06** `ShopFollowConcurrencyIT`
- [ ] **F02.C1** 🔖 COMMIT `feat(social): follow and unfollow shops without miscounting followers` · hash: `…`

## F03 — Danh sách shop đang theo dõi

- [ ] **F03.01** `ShopFollowQueryService.list(userId, cursor, limit)`
- [ ] **F03.02** `summary.total` = số shop đang theo dõi còn hiển thị được.
- [ ] **F03.03** `MeFollowingController`
- [ ] **F03.04** `ShopFollowFacade`
- [ ] **F03.05** `ShopFollowQueryIT`
- [ ] **F03.C1** 🔖 COMMIT `feat(social): list followed shops with rating and follower count` · hash: `…`

## F04 — Nối giao diện `web/`

- [ ] **F04.01** `shop/infrastructure/shop.api.ts`
- [ ] **F04.02** `error-codes.ts` + `vi.json` / `en.json`
- [ ] **F04.03** Trạng thái & lỗi
- [ ] **F04.04** Trang `/account/following` dùng `summary.total` cho tiêu đề "Shop đang theo dõi (n)".
- [ ] **F04.05** Chạy thật hai phía
- [ ] **F04.06** `cd web && npm run verify`.
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
| | |

## Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-16 | Lập plan + progress (33 task, 5 commit, 6 giai đoạn) — chưa bắt đầu làm | — |
