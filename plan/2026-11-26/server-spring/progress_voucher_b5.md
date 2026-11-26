# PROGRESS — Cụm B5 Voucher: quản lý, chưa áp vào giá (UC-PRO-01 · UC-PRO-02 · UC-PRO-03)

| | |
|---|---|
| **Plan** | [plan_voucher_b5.md](./plan_voucher_b5.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-26 |
| **Tổng** | 55 task + 13 commit, 11 giai đoạn |
| **Nhánh** | `feature/voucher-management` (V01–V04, V06–V07) → `feature/voucher-wallet` (V05, V08–V10) |
| **Phụ trách** | **Nguyễn Ngọc Thiện** — lập plan và thực hiện (nhận 2026-11-26) |
| **Người commit** | danh tính git đã cấu hình trong repo (`Thien211105`) — không `--author`, không `--no-verify`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-26 — mới lập plan (0 / 68) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục *Ghi chú*.
- Task `COMMIT` chỉ tick sau khi qua *Cổng kiểm tra* (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`. Thông điệp đã ghi sẵn — tiếng Anh, chữ thường, đúng `commitlint.config.cjs`; có mã Jira thì thêm ` [#GNE-<số>]` cuối.
- Sau mỗi buổi làm: cập nhật cột *Xong* của bảng Tổng quan, dòng *Cập nhật gần nhất*, một dòng *Nhật ký* cuối trang.
- Cột *Chạy* của use case chỉ tick khi đã chạy thật trên giao diện (Chromium điều khiển bằng Playwright, hệ thống thật) và đạt hết ca của kịch bản — ghi cách chạy + số ca vào *Ghi chú*.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | **Trạng thái quản lý** `ACTIVE, SCHEDULED, ENDED, DISABLED` (10.7, 11.14) | ☐ | |
| D2 | **Lọc theo `status` trong SQL** | ☐ | |
| D3 | **Sửa voucher đang chạy** (công bằng với người đã lưu) | ☐ | |
| D4 | **Tắt / bật lại / huỷ lịch** | ☐ | |
| D5 | **Mã voucher** | ☐ | |
| D6 | **Tiêu đề voucher shop** — `title` NOT NULL nhưng `$ShopVoucherInput` không có | ☐ | |
| D7 | **Nhà tài trợ & đồng tài trợ** | ☐ | |
| D8 | **FREESHIP** — `discount_value` NOT NULL, API bảo "bỏ `discount_value`, `max_discount`" | ☐ | |
| D9 | **Giới hạn giá trị** | ☐ | |
| D10 | **Phạm vi áp dụng & loại trừ** | ☐ | |
| D11 | **Ưu tiên** (`priority` NOT NULL, không có trong input) | ☐ | |
| D12 | **Quà đổi Điểm** (`COIN500…`) | ☐ | |
| D13 | **`$Voucher.shop`** — `VoucherView` đang trả `users.id` làm `shop.id` | ☐ | |
| D14 | **Lưu voucher** (UC-PRO-03 b4) | ☐ | |
| D15 | **Danh sách công khai** (API 5.13) + cache | ☐ | |
| D16 | **Kho voucher** `GET /me/vouchers` | ☐ | |
| D17 | **Thống kê** | ☐ | |
| D18 | **Xóa vĩnh viễn** (chỉ voucher sàn) | ☐ | |
| D19 | **Đụng độ ghi** (`@Version`) | ☐ | |
| D20 | **Chọn sản phẩm cho phạm vi `PRODUCT`** (web) | ☐ | |
| D21 | **Cờ nối web** | ☐ | |
| D22 | **Ngoài phạm vi** | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| V00 | Chuẩn bị | 5 | 0 | 0 / 5 |
| V01 | Tài liệu trước | 3 | 1 | 0 / 4 |
| V02 | Miền & kiểm dữ liệu vào | 6 | 1 | 0 / 7 |
| V03 | Voucher gian hàng (UC-PRO-01) | 6 | 1 | 0 / 7 |
| V04 | Voucher toàn sàn (UC-PRO-02) | 5 | 1 | 0 / 6 |
| V05 | Voucher công khai & Kho Voucher (UC-PRO-03) — nhánh `feature/voucher-wallet` | 8 | 3 | 0 / 11 |
| V06 | Nối web Seller › Khuyến mãi (UC-PRO-01) | 4 | 1 | 0 / 5 |
| V07 | Nối web Admin › Voucher sàn (UC-PRO-02) | 2 | 1 | 0 / 3 |
| V08 | Nối web Storefront & Kho Voucher (UC-PRO-03) | 4 | 1 | 0 / 5 |
| V09 | Dữ liệu demo, kiểm thử & chạy thật | 4 | 2 | 0 / 6 |
| V10 | Hoàn thiện use case & tick tiến độ | 8 | 1 | 0 / 9 |
| **Tổng** | | **55** | **13** | **0 / 68** |

## V00 — Chuẩn bị

- [ ] **V00.01** Kiểm danh tính git; kiểm `V26` (plan [1] N01) đã có trên `develop`
- [ ] **V00.02** Chốt D1–D22 vào progress.
- [ ] **V00.03** Tạo nhánh `feature/voucher-management` từ `develop` mới nhất.
- [ ] **V00.04** Đọc màn mock
- [ ] **V00.05** Kiểm `GlobalExceptionHandler` đã ánh xạ `ObjectOptimisticLockingFailureException` chưa (D19); kiểm `CategoryFacade` có tra tên / tồn tại theo lô không (D10)

## V01 — Tài liệu trước

- [ ] **V01.01** ENTITY 4.1
- [ ] **V01.02** API_SPEC
- [ ] **V01.03** Usecase.md
- [ ] **V01.C1** 🔖 COMMIT `docs(promotion): specify voucher lifecycle, edit rules and wallet saving` · hash: `…`

## V02 — Miền & kiểm dữ liệu vào

- [ ] **V02.01** `ErrorCode`
- [ ] **V02.02** `VoucherStatus` + `Voucher.status(now)` (D1); `VoucherStatusSql` (D2).
- [ ] **V02.03** `Voucher.createShop(vendorUserId, …)`, `createPlatform(…)`, `applyEdit(input, hasSavers, now)` → trả danh sách trường bị khóa (D3), `disable()` (D4), `isDeletable(hasUsages)` (D18), `validateConfig` nới (D7).
- [ ] **V02.04** `VoucherInputValidator` (D5, D8, D9, D10)
- [ ] **V02.05** `VoucherRepository`
- [ ] **V02.06** Test
- [ ] **V02.C1** 🔖 COMMIT `feat(promotion): add voucher lifecycle status and edit rules` · hash: `…`

## V03 — Voucher gian hàng (UC-PRO-01)

- [ ] **V03.01** `VoucherReadModel`
- [ ] **V03.02** `ShopVoucherService`
- [ ] **V03.03** `VoucherQueryService.shopPage(me, status, page, size)` + `summary` (D17); `toShopInput(voucher)` cho form sửa / nhân bản (kèm `category_ids`, `product_ids`, loại trừ).
- [ ] **V03.04** Listener `AFTER_COMMIT`
- [ ] **V03.05** `SellerVoucherController`
- [ ] **V03.06** `SellerVoucherIT`
- [ ] **V03.C1** 🔖 COMMIT `feat(promotion): let sellers create, edit and disable shop vouchers` · hash: `…`

## V04 — Voucher toàn sàn (UC-PRO-02)

- [ ] **V04.01** `PlatformVoucherService`
- [ ] **V04.02** `VoucherQueryService.platformPage(status, page, size)` + `summary` (D17) + `is_coin_reward` (đọc `genie.coin.rewards`).
- [ ] **V04.03** `AdminVoucherController`
- [ ] **V04.04** `AdminVoucherIT`
- [ ] **V04.05** `VoucherSecurityIT`
- [ ] **V04.C1** 🔖 COMMIT `feat(promotion): let staff manage platform vouchers with budget and audit log` · hash: `…`

## V05 — Voucher công khai & Kho Voucher (UC-PRO-03) — nhánh `feature/voucher-wallet`

- [ ] **V05.01** `VoucherView` sửa (D13)
- [ ] **V05.02** `PublicVoucherService` + `PublicVoucherCache` (D15)
- [ ] **V05.03** `VoucherWalletService`
- [ ] **V05.04** `BuyerOnlyFilter` thêm `/api/v1/me/vouchers` (D14) + ca trong test sẵn có của filter.
- [ ] **V05.05** `PublicVoucherController` (`GET /public/vouchers`, `@Operation(summary = "UC-PRO-03 …")`), `MeVoucherController` (`GET`, `POST`, `DELETE /me/vouchers…`).
- [ ] **V05.06** `PublicVoucherIT`
- [ ] **V05.07** `VoucherWalletIT`
- [ ] **V05.08** `VoucherCacheEvictionIT`
- [ ] **V05.C1** 🔖 COMMIT `fix(promotion): return shop id and name in voucher view` · hash: `…`
- [ ] **V05.C2** 🔖 COMMIT `feat(promotion): list public vouchers and let buyers save them to their wallet` · hash: `…`
- [ ] **V05.C3** 🔖 COMMIT `feat(common): block internal accounts from the voucher wallet` · hash: `…`

## V06 — Nối web Seller › Khuyến mãi (UC-PRO-01)

- [ ] **V06.01** `promo.api.ts`
- [ ] **V06.02** `listVoucherTargets`
- [ ] **V06.03** Modal "Tạo Voucher Shop"
- [ ] **V06.04** `error-codes.ts` + vi / en
- [ ] **V06.C1** 🔖 COMMIT `feat(seller-ui): connect shop voucher management to the real api` · hash: `…`

## V07 — Nối web Admin › Voucher sàn (UC-PRO-02)

- [ ] **V07.01** `marketing.api.ts` (nhóm voucher sàn)
- [ ] **V07.02** Modal "Tạo Voucher Sàn" + bảng
- [ ] **V07.C1** 🔖 COMMIT `feat(admin-ui): connect platform voucher management to the real api` · hash: `…`

## V08 — Nối web Storefront & Kho Voucher (UC-PRO-03)

- [ ] **V08.01** `voucher.api.ts`
- [ ] **V08.02** `listMyVouchers(state, type, page)` theo API 3.14 + `summary.counts` cho 4 tab; lịch sử (Đã sử dụng / Hết hiệu lực) với `used_order_code`; xóa khỏi kho.
- [ ] **V08.03** Vé "Lưu" ở trang chủ / trang shop / trang chiến dịch / trang sản phẩm
- [ ] **V08.04** Test
- [ ] **V08.C1** 🔖 COMMIT `feat(web): connect public vouchers and the voucher wallet to the real api` · hash: `…`

## V09 — Dữ liệu demo, kiểm thử & chạy thật

- [ ] **V09.01** Seed
- [ ] **V09.02** `docs/api-tests/vouchers.http`
- [ ] **V09.03** `docs/kiem-thu/kich-ban-2026-11-26-voucher.md`
- [ ] **V09.04** Chạy thật
- [ ] **V09.C1** 🔖 COMMIT `feat(db): seed upcoming, category-scoped and free shipping vouchers` · hash: `…`
- [ ] **V09.C2** 🔖 COMMIT `docs(promotion): add voucher api tests and real-run scenarios` · hash: `…`

## V10 — Hoàn thiện use case & tick tiến độ

- [ ] **V10.01** OpenAPI
- [ ] **V10.02** `docs/web-backend-handoff.md`
- [ ] **V10.03** Usecase.md
- [ ] **V10.04** `CHECKLIST_TIEN_DO_UC.md`
- [ ] **V10.05** `BAO_CAO_UC_DOC_LAP.md`
- [ ] **V10.06** `Quản Lý UseCase.xlsx`
- [ ] **V10.07** README ngày (mục 7)
- [ ] **V10.C1** 🔖 COMMIT `docs(promotion): record shop vouchers, platform vouchers and voucher wallet as implemented` · hash: `…`
- [ ] **V10.08** Mở PR `feature/voucher-management` (V01–V04, V06–V07) rồi `feature/voucher-wallet` (V05, V08–V10) vào `develop`; PR thứ nhất ghi rõ `BuyerOnlyFilter` không đổi (đổi ở PR hai) và nhờ Nguyễn Quốc Khoa duyệt dòng đó.

---

## Chốt plan — tick tiến độ use case (người phụ trách: Nguyễn Ngọc Thiện)

Làm **ngay khi xong plan** (hoặc xong từng use case), trong [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../../docs/Document/CHECKLIST_TIEN_DO_UC.md) — cách tick xem mục 0 của file đó. Use case chỉ ✅ khi đủ 5 lớp GD / BE / Test / Nối / Chạy (UC chạy nền: đủ các ô không phải `—`).

- [ ] `UC-PRO-01` Tạo & Quản lý Voucher Gian hàng (Vendor Shop Vouchers) — tick GD / BE / Test / Nối / Chạy, Trạng thái **✅ Đã triển khai**, cột **Phụ trách = Nguyễn Ngọc Thiện**
- [ ] `UC-PRO-02` Tạo & Quản lý Voucher Toàn sàn — tick GD / BE / Test / Nối / Chạy, Trạng thái **✅ Đã triển khai**, cột **Phụ trách = Nguyễn Ngọc Thiện**
- [ ] `UC-PRO-03` Khách hàng thu thập Voucher vào Kho Voucher cá nhân — tick GD / BE / Test / Nối / Chạy, Trạng thái **✅ Đã triển khai**, cột **Phụ trách = Nguyễn Ngọc Thiện**
- [ ] Cập nhật số ở mục 1 *Tổng quan* (Đã triển khai / Đang triển khai / Chưa có backend) + bảng *Tiến độ theo phân hệ* (phân hệ 07 (Khuyến mãi, Voucher, Flash Sale & Điểm))
- [ ] Thêm một dòng vào mục 5 *Nhật ký cập nhật* của checklist (ngày · use case · việc đã làm · mã commit / PR)
- [ ] Bảng *Thành viên & phân công* (đầu checklist **và** đầu `BAO_CAO_UC_DOC_LAP.md`) — cột *Hiện trạng* của Nguyễn Ngọc Thiện ghi các use case vừa xong
- [ ] `BAO_CAO_UC_DOC_LAP.md` — bảng trạng thái của cụm / dòng use case (✅ + *Còn lại*), dòng *Phụ trách: Nguyễn Ngọc Thiện*
- [ ] `Quản Lý UseCase.xlsx` — cột *Hiện trạng mã nguồn* = "Đã triển khai", cột AA *Người phụ trách* = "Nguyễn Ngọc Thiện", sheet *Thành viên* (sửa XML, chuỗi `&#NNNN;`)
- [ ] Câu *Hiện trạng* của từng use case trong `Usecase.md`
- [ ] Tick dòng **[3] Voucher B5** ở bảng mục 7 của [README](./README.md)

## Ghi chú

Ghi ở đây: lý do bỏ qua task (`[-]`), chỗ làm khác plan, số đo, số migration thật, cách chạy thật + số ca đạt, việc tồn chuyển sang plan khác.

| Task | Ghi chú |
|---|---|

## Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-26 | Lập plan + progress (55 task, 13 commit, 11 giai đoạn) — chưa bắt đầu làm | — |
