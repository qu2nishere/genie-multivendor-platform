# PROGRESS — Cụm B5 Voucher: quản lý, chưa áp vào giá (UC-PRO-01 · UC-PRO-02 · UC-PRO-03)

| | |
|---|---|
| **Plan** | [plan_voucher_b5.md](./plan_voucher_b5.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-26 |
| **Tổng** | 55 task + 13 commit, 11 giai đoạn |
| **Nhánh** | `feature/voucher-management` (V01–V04, V06–V07) → `feature/voucher-wallet` (V05, V08–V10) |
| **Phụ trách** | **Nguyễn Ngọc Thiện** — lập plan và thực hiện (nhận 2026-11-26) |
| **Người commit** | danh tính git đã cấu hình trong repo (`Thien211105`) — không `--author`, không `--no-verify`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-28 — xong V00–V10 (chỉ còn mở PR V10.08) (67 / 68) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục *Ghi chú*.
- Task `COMMIT` chỉ tick sau khi qua *Cổng kiểm tra* (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`. Thông điệp đã ghi sẵn — tiếng Anh, chữ thường, đúng `commitlint.config.cjs`; có mã Jira thì thêm ` [#GNE-<số>]` cuối.
- Sau mỗi buổi làm: cập nhật cột *Xong* của bảng Tổng quan, dòng *Cập nhật gần nhất*, một dòng *Nhật ký* cuối trang.
- Cột *Chạy* của use case chỉ tick khi đã chạy thật trên giao diện (Chromium điều khiển bằng Playwright, hệ thống thật) và đạt hết ca của kịch bản — ghi cách chạy + số ca vào *Ghi chú*.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | **Trạng thái quản lý** `ACTIVE, SCHEDULED, ENDED, DISABLED` (10.7, 11.14) | ☑ | Như đề xuất — `Voucher.status(now)`; hết ngân sách (kể cả khi `consume` đã tự tắt) → `ENDED` / 2026-11-27 |
| D2 | **Lọc theo `status` trong SQL** | ☑ | Như đề xuất — `VoucherStatusSql` (điều kiện + `CASE`), `VoucherStatusSqlIT` so từng ca với `status()` / 2026-11-27 |
| D3 | **Sửa voucher đang chạy** (công bằng với người đã lưu) | ☑ | Như đề xuất — `fullyEditable` + `lockedChanges` (đổi phạm vi cũng khóa); `409 GNE-VCH-4094` `details.locked_fields`; chi tiết trả `locked_fields` để web khóa ô / 2026-11-27 |
| D4 | **Tắt / bật lại / huỷ lịch** | ☑ | Như đề xuất / 2026-11-27 |
| D5 | **Mã voucher** | ☑ | Như đề xuất — kiểm trước + bắt `DataIntegrityViolationException` (IT hai request song song) / 2026-11-27 |
| D6 | **Tiêu đề voucher shop** — `title` NOT NULL nhưng `$ShopVoucherInput` không có | ☑ | Như đề xuất — `VoucherTitles` / 2026-11-27 |
| D7 | **Nhà tài trợ & đồng tài trợ** | ☑ | Như đề xuất — `validateConfig` nới cho `PLATFORM / FREESHIP` + `CO_SPONSOR` (`platformSharePercent` 1–99) / 2026-11-27 |
| D8 | **FREESHIP** — `discount_value` NOT NULL, API bảo "bỏ `discount_value`, `max_discount`" | ☑ | Như đề xuất — FREESHIP lưu trần vào `discount_value` (0 = toàn bộ), API trả `discount_value = null` / 2026-11-27 |
| D9 | **Giới hạn giá trị** | ☑ | Như đề xuất — `VoucherInputValidator` gom lỗi một lần; % cho phép 2 chữ số lẻ / 2026-11-27 |
| D10 | **Phạm vi áp dụng & loại trừ** | ☑ | Như đề xuất — `VoucherScopes` thay cả 4 tập trong cùng transaction; sản phẩm loại trừ ở voucher sàn chỉ cần tồn tại / 2026-11-27 |
| D11 | **Ưu tiên** (`priority` NOT NULL, không có trong input) | ☑ | Như đề xuất / 2026-11-27 |
| D12 | **Quà đổi Điểm** (`COIN500…`) | ☑ | Như đề xuất — mã đọc từ `genie.coin.rewards`; ẩn ở 5.13 + 3.14, Admin có `is_coin_reward` / 2026-11-27 |
| D13 | **`$Voucher.shop`** — `VoucherView` đang trả `users.id` làm `shop.id` | ☑ | Như đề xuất — `VoucherViews` nạp theo lô; `CoinRedemptionService` dùng bản không shop (voucher sàn) / 2026-11-28 |
| D14 | **Lưu voucher** (UC-PRO-03 b4) | ☑ | Như đề xuất / 2026-11-28 |
| D15 | **Danh sách công khai** (API 5.13) + cache | ☑ | Như đề xuất — cache lưu danh sách id (thứ tự), `state` / `is_saved` dựng lại mỗi lần; xóa cả nhóm khi voucher đổi / 2026-11-28 |
| D16 | **Kho voucher** `GET /me/vouchers` | ☑ | Như đề xuất — tab SHOP = mọi voucher shop, FREESHIP = sàn freeship + shop freeship / 2026-11-28 |
| D17 | **Thống kê** | ☑ | Như đề xuất — doanh thu tháng tính đơn `COMPLETED` theo `completed_at` giờ VN (= 0 tới C5 / C6) / 2026-11-27 |
| D18 | **Xóa vĩnh viễn** (chỉ voucher sàn) | ☑ | Như đề xuất / 2026-11-27 |
| D19 | **Đụng độ ghi** (`@Version`) | ☑ | Như đề xuất — `TransactionTemplate` thử lại một lần; IT giữ khóa dòng 800 ms → sửa vẫn thành công / 2026-11-27 |
| D20 | **Chọn sản phẩm cho phạm vi `PRODUCT`** (web) | ☑ | Đổi: C1 đã merge (`#113`) nên dùng thẳng `GET /seller/products?status=PUBLISHED` thay `/public/products?shop_id=` / 2026-11-28 |
| D21 | **Cờ nối web** | ☑ | Như đề xuất / 2026-11-28 |
| D22 | **Ngoài phạm vi** | ☑ | Như đề xuất / 2026-11-26 |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| V00 | Chuẩn bị | 5 | 0 | 5 / 5 |
| V01 | Tài liệu trước | 3 | 1 | 4 / 4 |
| V02 | Miền & kiểm dữ liệu vào | 6 | 1 | 7 / 7 |
| V03 | Voucher gian hàng (UC-PRO-01) | 6 | 1 | 7 / 7 |
| V04 | Voucher toàn sàn (UC-PRO-02) | 5 | 1 | 6 / 6 |
| V05 | Voucher công khai & Kho Voucher (UC-PRO-03) — nhánh `feature/voucher-wallet` | 8 | 3 | 11 / 11 |
| V06 | Nối web Seller › Khuyến mãi (UC-PRO-01) | 4 | 1 | 5 / 5 |
| V07 | Nối web Admin › Voucher sàn (UC-PRO-02) | 2 | 1 | 3 / 3 |
| V08 | Nối web Storefront & Kho Voucher (UC-PRO-03) | 4 | 1 | 5 / 5 |
| V09 | Dữ liệu demo, kiểm thử & chạy thật | 4 | 2 | 6 / 6 |
| V10 | Hoàn thiện use case & tick tiến độ | 8 | 1 | 8 / 9 |
| **Tổng** | | **55** | **13** | **67 / 68** |

## V00 — Chuẩn bị

- [x] **V00.01** Kiểm danh tính git; kiểm `V26` (plan [1] N01) đã có trên `develop`
- [x] **V00.02** Chốt D1–D22 vào progress.
- [x] **V00.03** Tạo nhánh `feature/voucher-management` từ `develop` mới nhất.
- [x] **V00.04** Đọc màn mock
- [x] **V00.05** Kiểm `GlobalExceptionHandler` đã ánh xạ `ObjectOptimisticLockingFailureException` chưa (D19); kiểm `CategoryFacade` có tra tên / tồn tại theo lô không (D10)

## V01 — Tài liệu trước

- [x] **V01.01** ENTITY 4.1
- [x] **V01.02** API_SPEC
- [x] **V01.03** Usecase.md
- [x] **V01.C1** 🔖 COMMIT `docs(promotion): specify voucher lifecycle, edit rules and wallet saving` · hash: `5afe663`

## V02 — Miền & kiểm dữ liệu vào

- [x] **V02.01** `ErrorCode`
- [x] **V02.02** `VoucherStatus` + `Voucher.status(now)` (D1); `VoucherStatusSql` (D2).
- [x] **V02.03** `Voucher.createShop(vendorUserId, …)`, `createPlatform(…)`, `applyEdit(input, hasSavers, now)` → trả danh sách trường bị khóa (D3), `disable()` (D4), `isDeletable(hasUsages)` (D18), `validateConfig` nới (D7).
- [x] **V02.04** `VoucherInputValidator` (D5, D8, D9, D10)
- [x] **V02.05** `VoucherRepository`
- [x] **V02.06** Test
- [x] **V02.C1** 🔖 COMMIT `feat(promotion): add voucher lifecycle status and edit rules` · hash: `49fbcfc`

## V03 — Voucher gian hàng (UC-PRO-01)

- [x] **V03.01** `VoucherReadModel`
- [x] **V03.02** `ShopVoucherService`
- [x] **V03.03** `VoucherQueryService.shopPage(me, status, page, size)` + `summary` (D17); `toShopInput(voucher)` cho form sửa / nhân bản (kèm `category_ids`, `product_ids`, loại trừ).
- [x] **V03.04** Listener `AFTER_COMMIT`
- [x] **V03.05** `SellerVoucherController`
- [x] **V03.06** `SellerVoucherIT`
- [x] **V03.C1** 🔖 COMMIT `feat(promotion): let sellers create, edit and disable shop vouchers` · hash: `940d021`

## V04 — Voucher toàn sàn (UC-PRO-02)

- [x] **V04.01** `PlatformVoucherService`
- [x] **V04.02** `VoucherQueryService.platformPage(status, page, size)` + `summary` (D17) + `is_coin_reward` (đọc `genie.coin.rewards`).
- [x] **V04.03** `AdminVoucherController`
- [x] **V04.04** `AdminVoucherIT`
- [x] **V04.05** `VoucherSecurityIT`
- [x] **V04.C1** 🔖 COMMIT `feat(promotion): let staff manage platform vouchers with budget and audit log` · hash: `c5a7bd8`

## V05 — Voucher công khai & Kho Voucher (UC-PRO-03) — nhánh `feature/voucher-wallet`

- [x] **V05.01** `VoucherView` sửa (D13)
- [x] **V05.02** `PublicVoucherService` + `PublicVoucherCache` (D15)
- [x] **V05.03** `VoucherWalletService`
- [x] **V05.04** `BuyerOnlyFilter` thêm `/api/v1/me/vouchers` (D14) + ca trong test sẵn có của filter.
- [x] **V05.05** `PublicVoucherController` (`GET /public/vouchers`, `@Operation(summary = "UC-PRO-03 …")`), `MeVoucherController` (`GET`, `POST`, `DELETE /me/vouchers…`).
- [x] **V05.06** `PublicVoucherIT`
- [x] **V05.07** `VoucherWalletIT`
- [x] **V05.08** `VoucherCacheEvictionIT`
- [x] **V05.C1** 🔖 COMMIT `fix(promotion): return shop id and name in voucher view` · hash: `dde4f8f`
- [x] **V05.C2** 🔖 COMMIT `feat(promotion): list public vouchers and let buyers save them to their wallet` · hash: `ef02932`
- [x] **V05.C3** 🔖 COMMIT `feat(common): block internal accounts from the voucher wallet` · hash: `1fe9574`

## V06 — Nối web Seller › Khuyến mãi (UC-PRO-01)

- [x] **V06.01** `promo.api.ts`
- [x] **V06.02** `listVoucherTargets`
- [x] **V06.03** Modal "Tạo Voucher Shop"
- [x] **V06.04** `error-codes.ts` + vi / en
- [x] **V06.C1** 🔖 COMMIT `feat(seller-ui): connect shop voucher management to the real api` · hash: `b2c496e`

## V07 — Nối web Admin › Voucher sàn (UC-PRO-02)

- [x] **V07.01** `marketing.api.ts` (nhóm voucher sàn)
- [x] **V07.02** Modal "Tạo Voucher Sàn" + bảng
- [x] **V07.C1** 🔖 COMMIT `feat(admin-ui): connect platform voucher management to the real api` · hash: `3333092`

## V08 — Nối web Storefront & Kho Voucher (UC-PRO-03)

- [x] **V08.01** `voucher.api.ts`
- [x] **V08.02** `listMyVouchers(state, type, page)` theo API 3.14 + `summary.counts` cho 4 tab; lịch sử (Đã sử dụng / Hết hiệu lực) với `used_order_code`; xóa khỏi kho.
- [x] **V08.03** Vé "Lưu" ở trang chủ / trang shop / trang chiến dịch / trang sản phẩm
- [x] **V08.04** Test
- [x] **V08.C1** 🔖 COMMIT `feat(web): connect public vouchers and the voucher wallet to the real api` · hash: `d24ed35`

## V09 — Dữ liệu demo, kiểm thử & chạy thật

- [x] **V09.01** Seed
- [x] **V09.02** `docs/api-tests/vouchers.http`
- [x] **V09.03** `docs/kiem-thu/kich-ban-2026-11-26-voucher.md`
- [x] **V09.04** Chạy thật
- [x] **V09.C1** 🔖 COMMIT `feat(db): seed upcoming, category-scoped and free shipping vouchers` · hash: `65fbdbb`
- [x] **V09.C2** 🔖 COMMIT `docs(promotion): add voucher api tests and real-run scenarios` · hash: `770eccc`

## V10 — Hoàn thiện use case & tick tiến độ

- [x] **V10.01** OpenAPI
- [x] **V10.02** `docs/web-backend-handoff.md`
- [x] **V10.03** Usecase.md
- [x] **V10.04** `CHECKLIST_TIEN_DO_UC.md`
- [x] **V10.05** `BAO_CAO_UC_DOC_LAP.md`
- [x] **V10.06** `Quản Lý UseCase.xlsx`
- [x] **V10.07** README ngày (mục 7)
- [x] **V10.C1** 🔖 COMMIT `docs(promotion): record shop vouchers, platform vouchers and voucher wallet as implemented` · hash: `228757e`
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
| V00.01 | Nhánh `feature/voucher-management` tạo từ `origin/develop` (đã có C1 `#113`) + gộp `feature/b56-shared-schema` (`V26` — PR #112 chưa merge); `feature/voucher-wallet` xếp chồng trên nhánh này |
| V03–V04 | Một service `VoucherManagementService` + `VoucherManagementQuery` cho cả voucher shop và sàn (cùng luật, khác quyền / nhật ký) thay cho 2 service riêng như cây file dự kiến |
| V02–V05 | Test: `VoucherTest` 9, `VoucherInputValidatorTest` 11, `VoucherStatusSqlIT` 1, `SellerVoucherIT` 8, `AdminVoucherIT` 5, `VoucherSecurityIT` 2, `PublicVoucherIT` 4, `VoucherWalletIT` 5, `VoucherCacheEvictionIT` 2, `BuyerOnlyFilterTest` (thêm 3 ca); toàn nhánh `./mvnw clean verify` 250 unit + 401 IT xanh |
| V08.01 | Thêm `listFeaturedVouchers` — trang chủ trước đó ghi cứng 4 mã mẫu không có trong dữ liệu thật (`c37f7e0`) |
| V09.04 | Chromium (Playwright), web build `PUBLIC_REAL_API=…,seller-vouchers,admin-vouchers,vouchers` + backend `local` — **17 / 17 ca đạt**. Lỗi thật phát hiện: sửa voucher đang chạy gửi lại giờ khác (form chỉ giữ ngày) → 4094 — sửa `a0ae85f`. Lỗi của kịch bản: checkbox / Select tùy biến, regex rỗng khiến script bấm nhầm "Huỷ" `GENIESOON` lượt đầu (đã bật lại trong CSDL máy dev, thêm chặn mã rỗng) |
| V10.01 | `/v3/api-docs` có 15 endpoint voucher (plan ước 14), summary `UC-PRO-01` (5) / `UC-PRO-02` (6) / `UC-PRO-03` (4) |
| V10.04–V10.06 | Số liệu checklist tính trên nhánh này (chưa gồm plan [1], [2]): Đã triển khai 24 → 27, mock 34 → 31, phân hệ 07: 3 / 2 / 4. Khi merge chung với #114–#117 phải cộng lại (dự kiến 32 đã triển khai) |

## Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-26 | Lập plan + progress (55 task, 13 commit, 11 giai đoạn) — chưa bắt đầu làm | — |
| 2026-11-27 | V00–V04: tài liệu API / ENTITY / Usecase, miền voucher (trạng thái, luật sửa, đồng tài trợ), `/seller/vouchers`, `/admin/vouchers` (ngân sách, nhật ký, xóa) | `5afe663` … `c5a7bd8` |
| 2026-11-27 | V06–V07: web Seller › Khuyến mãi + Admin › Voucher sàn nối thật (khóa ô theo `locked_fields`) | `b2c496e` `3333092` |
| 2026-11-28 | V05, V08 (nhánh `feature/voucher-wallet`): `$Voucher` đúng `shop_id`, `/public/vouchers`, Kho `/me/vouchers`, chặn tài khoản nội bộ; web trang chủ / shop / chiến dịch / Kho | `dde4f8f` … `c37f7e0` |
| 2026-11-28 | V09–V10: seed demo, `vouchers.http`, chạy thật 17 / 17, sửa lỗi giờ khi sửa voucher; tài liệu — **UC-PRO-01, 02, 03 ✅** | `65fbdbb` `a0ae85f` `770eccc` `228757e` |
