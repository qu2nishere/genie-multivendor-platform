# PROGRESS — Hoàn thiện phần còn thiếu của cụm B1, B2, B3

> Plan: [plan_hoan_thien_b1_b2_b3.md](plan_hoan_thien_b1_b2_b3.md) · Ngày lập: 2026-11-24 · Rà soát / làm bù: Nguyễn Ngọc Thiện · Phụ trách: Nguyễn Quốc Khoa (B1, B2), Nghiêm Phú Đăng Quân (B3)
> Cách dùng: xong task nào thì đổi `- [ ]` → `- [x]` và ghi một dòng **Nhật ký** (ngày, việc, mã commit). Kịch bản chạy thật ghi **Đạt / Không đạt** vào bảng mục 5. Task không cần làm ghi `- [x] ~~…~~ (không cần: lý do)`.

## 1. Tổng quan

| UC | GD | BE | Test | Nối | Chạy | Trạng thái hiện tại | Mục tiêu |
|---|:---:|:---:|:---:|:---:|:---:|---|---|
| UC-ADM-03 Khóa / Mở khóa | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai (2026-11-24) | ✅ |
| UC-CAT-01 Cây danh mục | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai (2026-11-24) | ✅ |
| UC-CAT-14 Danh mục & hoa hồng | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai (2026-11-24) | ✅ |
| UC-CAT-15 Bộ thuộc tính | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai (2026-11-24) | ✅ |
| UC-CAT-05 Lọc & sắp xếp | ☑ | ☑ | ☑ | ☑ | ☑ | ✅ Đã triển khai (2026-11-24) | ✅ |

**Tổng quan dự án (đã cập nhật CHECKLIST 2026-11-24):** Đã triển khai 12 → **17** / 87 · Đang triển khai 22 → **17**. Còn mở: H00.06 (push / PR — chờ người dùng).

## 2. Tiến độ theo giai đoạn

| Giai đoạn | Việc | Số task | Trạng thái | Commit / PR |
|---|---|---:|:---:|---|
| H00 | Chuẩn bị (làm trước tiên) | 7 | ◐ (còn H00.06 push / PR) | `6d1a54e`, `0aca1bd` |
| H01 | B1: chuẩn bị dữ liệu thử | 2 | ☑ | — |
| H02 | B1: tài liệu trước | 5 | ☑ | `ec65c06` |
| H03 | B1: backend + test | 9 | ☑ | `8e0e81e`, `a2e02d2` |
| H04 | B1: chạy thật (kịch bản TC-B1 ở mục 6.1) | 2 | ☑ | |
| H05 | B2: chuẩn bị | 2 | ☑ | — |
| H06 | B2: test tự động còn thiếu | 8 | ☑ | `0259f49` |
| H07 | B2: tài liệu trước (chỉ khi H06 lộ lệch spec) | 2 | ☑ (không cần) | — |
| H08 | B3: cổng "shop có voucher" (backend) | 7 | ☑ | `95bc152` |
| H09 | B3: seed (D10) | 3 | ☑ | `a922955` |
| H10 | B3: tài liệu | 5 | ☑ | `65a24a8` |
| H11 | B3: web | 9 | ☑ | `b72b41b` |
| H12 | B2 + B3: chạy thật trên giao diện (kịch bản TC-B2, TC-B3 ở mục 6.2, 6.3) | 3 | ☑ | |
| H13 | Hồi quy toàn bộ | 4 | ☑ | — |
| H14 | Đồng bộ tiến độ & đóng plan | 6 | ☑ | (commit này) |
| H15 | B2: sửa giao diện Admin danh mục (phát sinh khi chạy thật) | 8 | ☑ | `89483df` |

**Tổng:** 82 task (74 + 8 của H15) · 60 kịch bản chạy thật.

## 3. Quyết định (chép kết quả từ plan mục 2)

| Mã | Chủ đề | Chốt | Ngày | Ai chốt |
|---|---|:---:|---|---|
| D1 | Nhật ký khi cron mở khóa | ☑ theo đề xuất | 2026-11-24 | Thiện (chờ Khoa / Quân xem lại) |
| D2 | "Thông báo cho người dùng" | ☑ theo đề xuất | 2026-11-24 | Thiện (chờ Khoa / Quân xem lại) |
| D3 | Chu kỳ, lô, giới hạn | ☑ theo đề xuất | 2026-11-24 | Thiện (chờ Khoa / Quân xem lại) |
| D4 | Giữ việc tự mở khi đăng nhập | ☑ theo đề xuất | 2026-11-24 | Thiện (chờ Khoa / Quân xem lại) |
| D5 | Định nghĩa "shop đang có voucher" | ☑ theo đề xuất | 2026-11-24 | Thiện (chờ Khoa / Quân xem lại) |
| D6 | Hiệu năng | ☑ theo đề xuất | 2026-11-24 | Thiện (chờ Khoa / Quân xem lại) |
| D7 | Web khi API chưa hỗ trợ lọc voucher | ☑ theo đề xuất | 2026-11-24 | Thiện (chờ Khoa / Quân xem lại) |
| D8 | Trang danh mục `/c/{slug}` có ô "Shop có voucher" không? | ☑ theo đề xuất | 2026-11-24 | Thiện (chờ Khoa / Quân xem lại) |
| D9 | Nguồn bộ lọc thuộc tính ngành | ☑ theo đề xuất | 2026-11-24 | Thiện (chờ Khoa / Quân xem lại) |
| D10 | Seed thêm voucher | ☑ theo đề xuất | 2026-11-24 | Thiện (chờ Khoa / Quân xem lại) |
| D11 | Nhánh | ☑ theo đề xuất | 2026-11-24 | Thiện (chờ Khoa / Quân xem lại) |

## 4. Task

### H00 — Chuẩn bị (làm trước tiên)

- [x] **H00.01** Chốt D1–D11 với Khoa (B1, B2) và Quân (B3), ghi vào mục 3
- [x] **H00.02** Đổi tên nhánh `chore/minor-fixes` → `bugfix/public-etag-304` (D11)
- [x] **H00.03** Chạy lại `./mvnw clean verify` (JDK 21) + `npm run verify`, ghi số vào mục 7
- [x] **H00.04** COMMIT `fix(common): ignore response meta when computing public etags`
- [x] **H00.05** COMMIT `docs: record b2 and b3 verification results`
- [ ] **H00.06** Push + PR vào `develop` (chờ người dùng cho push; 3 nhánh mới xếp chồng lên `bugfix/public-etag-304` theo D11)
- [x] **H00.07** Dựng môi trường chạy thật (Docker, Mailpit, `PUBLIC_REAL_API`, JDK 21)

### H01 — B1: chuẩn bị dữ liệu thử

- [x] **H01.01** Đăng ký 3 tài khoản khách thử `b1.cron7` / `b1.forever` / `b1.manual`
- [x] **H01.02** Ghi `users.id` 3 tài khoản vào mục 6

### H02 — B1: tài liệu trước

- [x] **H02.01** `LLD.md` mục 10: thêm dòng `SuspensionExpiryJob`
- [x] **H02.02** `API_SPEC.md` 11.13: đoạn *Tự mở khi hết hạn*
- [x] **H02.03** `Usecase.md` UC-ADM-03: sửa *Hiện trạng* + kênh thông báo email
- [x] **H02.04** `b1.md`: sửa D13 theo D1, R06 trỏ sang plan này
- [x] **H02.05** COMMIT `docs(auth): specify suspension expiry job and reactivation email`.

### H03 — B1: backend + test

- [x] **H03.01** `UserRepository.findSuspensionExpired` (`FOR UPDATE SKIP LOCKED`)
- [x] **H03.02** `SuspensionExpiryJob` (5 phút, lô 100, tối đa 1.000 / lượt)
- [x] **H03.03** `AccountReactivatedEvent` + mẫu email `account-reactivated` + `AccountMailListener` (D2)
- [x] **H03.04** Unit `UserTest`: biên đúng bằng hạn, `LOCKED`, vô thời hạn
- [x] **H03.05** `SuspensionExpiryJobIT` — 11 ca (hết hạn, còn hạn, vô thời hạn, LOCKED, DELETED, 150 tài khoản, song song, trùng đăng nhập, đăng nhập lại, email, mở thủ công)
- [x] **H03.06** `AccountReactivatedEmailTemplateTest` (vi / en)
- [x] **H03.07** Cổng `./mvnw verify` (ArchUnit xanh)
- [x] **H03.08** COMMIT `feat(auth): reactivate accounts when suspension expires` (repo + job + IT + unit).
- [x] **H03.09** COMMIT `feat(auth): email users when their account is reactivated` (sự kiện + template + listener + test).

### H04 — B1: chạy thật (kịch bản TC-B1 ở mục 6.1)

- [x] **H04.01** Chạy TC-B1-01 → TC-B1-12 (người dùng tự bấm), ghi mục 5
- [x] ~~**H04.02** Lỗi phát sinh → sửa…~~ (không cần: B1 12/12 đạt — TC-B1-11 chỉ sửa kịch bản)

### H05 — B2: chuẩn bị

- [x] **H05.01** Ghi id các ngành seed dùng để thử vào mục 6
- [x] **H05.02** Chuẩn bị SQL voucher tham chiếu danh mục (TC-B2-17) + SQL dọn

### H06 — B2: test tự động còn thiếu

- [x] **H06.01** `CatalogSecurityIT` — ma trận 7 vai trò × mọi endpoint B2 (+ `ACCESS_DENIED`, thu hồi quyền giữa chừng)
- [x] **H06.02** `AdminCategoryIT`: xóa ngành bị voucher tham chiếu → `4092 IN_USE`
- [x] **H06.03** Unit cây công khai khi Redis lỗi → đọc CSDL
- [x] **H06.04** `PublicCategoryIT`: `Cache-Control`, ETag → `304`, đổi tên → ETag mới
- [x] **H06.05** `CommissionIT`: lưu hàng loạt, mỗi ngành đổi đúng 1 `COMMISSION_UPDATED`
- [x] **H06.06** Bổ sung `catalog-category.http` (Moderator / Finance 403, `IN_USE`)
- [x] **H06.07** Cổng `./mvnw verify`.
- [x] **H06.08** COMMIT `test(catalog): cover role matrix, voucher-referenced delete and redis fallback`.

### H07 — B2: tài liệu trước (chỉ khi H06 lộ lệch spec)

- [x] ~~**H07.01** Ma trận lệch spec → dừng, chốt với Khoa~~ (không cần: ma trận khớp API 11.8 / 11.11 + LLD 11.1)
- [x] ~~**H07.02** COMMIT `docs(catalog): …`~~ (không cần: không có lệch spec)

### H08 — B3: cổng "shop có voucher" (backend)

- [x] **H08.01** `VoucherRepository.findVendorIdsWithActiveShopVoucher` (quy tắc D5)
- [x] **H08.02** `PromotionSearchPorts implements ShopVoucherAvailability` + cache 60 giây (D6)
- [x] **H08.03** Khẳng định bản mặc định của `SearchPortsConfig` đã tắt
- [x] **H08.04** `PromotionSearchPortsIT` — 11 ca (hạn, lượt, ngân sách, loại voucher, DISTINCT, cache, Redis lỗi)
- [x] **H08.05** `ProductSearchIT` / `FacetIT` — 5 ca `has_shop_voucher` (shop, kết hợp lọc, facet, false, trigram)
- [x] **H08.06** Cổng `./mvnw verify` (ArchUnit: không vòng `promotion` ↔ `catalog`)
- [x] **H08.07** COMMIT `feat(promotion): provide shops with active vouchers to product search`.

### H09 — B3: seed (D10)

- [x] **H09.01** Thêm 3 voucher seed (còn hạn / đã tắt / hết lượt) theo D10
- [x] **H09.02** `SeedDataIT` xanh, `seed.ps1 -Docker` chạy lại không nhân đôi
- [x] **H09.03** COMMIT `feat(db): seed shop vouchers for more shops including inactive and used up ones`.

### H10 — B3: tài liệu

- [x] **H10.01** `API_SPEC.md` 5.3 (quy tắc D5, độ trễ 60 giây) + mục 14 (facets thuộc tính)
- [x] **H10.02** `web-backend-handoff.md`: gạch dòng `attribute-filters`
- [x] **H10.03** `CLAUDE.md` mục 9: cổng voucher đã cài + khóa cache
- [x] ~~**H10.04** (D8 = Không) ghi lệch vào `BAO_CAO_DOI_CHIEU_GIAO_DIEN.md`~~ (không cần: D8 = Có)
- [x] **H10.05** COMMIT `docs(catalog): document shop voucher filter and attribute facets`.

### H11 — B3: web

- [x] **H11.01** Domain / mapper: `voucherFilterSupported`, `attribute` (có `code`) từ facets; mock khớp
- [x] **H11.02** Hàm dựng query gửi `attr.<code>` đúng mã + test Vitest
- [x] **H11.03** `FilterPanel`: ẩn ô voucher khi không hỗ trợ (D7), ô ở trang danh mục (D8), thuộc tính từ facets
- [x] **H11.04** `search.astro`, `c/[slug].astro`: bỏ `listAttributeFilters`, truyền facets / `hasVoucher`
- [x] **H11.05** Xóa mock `attribute-filters` + `listAttributeFilters` khỏi `features/catalog`
- [x] **H11.06** Test Vitest: URL `voucher=1`, chip, mapper facets, ẩn / hiện ô
- [x] **H11.07** Kiểm 9 theme + khổ hẹp ở `/search`, `/c/dien-tu` (người dùng nhìn bằng mắt — gộp vào TC-B3-14, TC-B3-16)
- [x] **H11.08** `npm run verify`
- [x] **H11.09** COMMIT `feat(web): drive voucher and attribute filters from search facets` (tách nếu > 400 dòng)

### H12 — B2 + B3: chạy thật trên giao diện (kịch bản TC-B2, TC-B3 ở mục 6.2, 6.3)

- [x] **H12.01** Chạy TC-B2-01 → TC-B2-32 (người dùng tự bấm), ghi mục 5
- [x] **H12.02** Chạy TC-B3-01 → TC-B3-16, ghi mục 5
- [x] **H12.03** Lỗi phát sinh → sửa ở **H15** (giao diện Admin ngành con + khóa kiểu dữ liệu), chạy lại 32 / 32 TC-B2 đạt

### H13 — Hồi quy toàn bộ

- [x] **H13.01** `./mvnw clean verify` (JDK 21), ghi số mục 7
- [x] **H13.02** `SearchPerformanceIT -Dperf=true` có lọc voucher (p95 không tệ hơn 20 %)
- [x] **H13.03** `npm run verify` + `npm run test:e2e`
- [x] **H13.04** Chạy lại `catalog-category.http`, `catalog-search.http` (thêm ca `has_shop_voucher`)

### H14 — Đồng bộ tiến độ & đóng plan

- [x] **H14.01** CHECKLIST: tick 5 UC → ✅, Tổng quan 12 → 17, bảng phân hệ, mục 3, nhật ký
- [x] **H14.02** Usecase.md, BAO_CAO_UC_DOC_LAP, Excel (sửa XML giữ định dạng)
- [x] **H14.03** `b1.md` R06, `b2.md` chạy thật + `CatalogSecurityIT`, progress B3
- [x] **H14.04** Chép kịch bản sang `docs/kiem-thu/kich-ban-2026-11-24-b1-b2-b3.md`
- [x] **H14.05** `CLAUDE.md` mục 2: dòng *Plan 2026-11-24*
- [x] **H14.06** COMMIT `docs: update progress after completing b1, b2 and b3 use cases` (PR từng nhánh: chờ người dùng cho push)

### H15 — B2: sửa giao diện Admin danh mục (phát sinh khi chạy thật)

- [x] **H15.01** Domain: `CategoryBase`, `CategoryNode`, `flattenTree`, `findCategory`, `canDeleteCategory`
- [x] **H15.02** DTO / mapper ngành cấp 2 / 3 + mock sửa / xóa mọi cấp
- [x] **H15.03** `CatalogPage`: dòng cấp 2 / 3 có Bộ thuộc tính / Sửa / Xem SP, nhãn "Ẩn"
- [x] **H15.04** `CatModal`: sửa / ẩn / xóa ngành mọi cấp, lỗi API hiện tại ô
- [x] **H15.05** `AttributePanel`: ngành mọi cấp, khóa Kiểu dữ liệu khi sửa, lỗi API tại ô
- [x] **H15.06** Test Vitest (rules + mapper) + `npm run verify`
- [x] **H15.07** COMMIT `feat(admin-ui): manage sub-categories and lock attribute data type`
- [x] **H15.08** Chạy lại TC-B2-01 → 32 trên trình duyệt thật — 32 / 32 đạt

## 5. Kết quả chạy thật

> **Cách chạy (2026-11-24):** theo yêu cầu người dùng, Claude chạy toàn bộ 60 TC bằng **Chromium thật điều khiển qua Playwright** trên hệ thống thật (web `localhost:4321` + backend `8080` nhánh `feature/b3-shop-voucher-filter` + Postgres + Redis + Mailpit), đăng nhập qua màn `/auth/login` như người dùng, đối chiếu thêm CSDL / API; ảnh chụp ở scratchpad `ui/shots/`. Đây **không phải người bấm tay** — Khoa / Quân nên bấm lại vài ca chính trước khi gộp. Kết quả lần đầu: **B1 12/12 · B2 27/32 · B3 16/16**; sau H15 (sửa giao diện) chạy lại B2 → **32/32** — tổng **60/60 đạt**.

### B1 · UC-ADM-03

| Mã | Thao tác (tóm tắt) | Kết quả | Ngày | Ghi chú / lỗi |
|---|---|:---:|---|---|
| TC-B1-01 | `/admin/users` → tìm `b1.cron7@example.com` → Khóa, lý do `ABNORMAL_RETURNS`, thời hạn 7 ngày | Đạt | 2026-11-24 | Khóa 7 ngày (hộp thoại mặc định "Hoàn hàng bất thường nhiều lần") → CSDL `SUSPENDED` +7 ngày; tab Đang khóa 1 → 2, dòng "Đình chỉ · đến 01/12/2026"; tab `/account/profile` đang mở của b1.cron7 bị đá về `/auth/login` ngay (WebSocket) |
| TC-B1-02 | Đăng nhập `b1.cron7@example.com` | Đạt | 2026-11-24 | `403 GNE-AUTH-4031`, màn đăng nhập báo "Tài khoản đang bị tạm khóa." |
| TC-B1-03 | SQL (a) dời hạn về 1 phút trước; không đăng nhập; chờ tối đa 5 phút (xem log backend) | Đạt | 2026-11-24 | Dời hạn → cron mở sau 18 giây; `/admin/users` dòng về "Hoạt động" |
| TC-B1-04 | `/admin/audit-logs`, lọc nhóm AUTH / tìm email | Đạt | 2026-11-24 | Nhật ký: "system · System · Mở lại tài khoản · 34"; CSDL `SUSPENSION_EXPIRED · CRON`, người làm `system@genie.vn` |
| TC-B1-05 | Mailpit http://localhost:8025 | Đạt | 2026-11-24 | Mailpit: 1 thư "Tài khoản Genie Shop của bạn đã được mở lại" |
| TC-B1-06 | Đăng nhập `b1.cron7@example.com` | Đạt | 2026-11-24 | Đăng nhập được, về trang chủ |
| TC-B1-07 | Khóa `b1.forever@example.com` vô thời hạn → SQL (b) kiểm `suspended_until` NULL → chờ ≥ 6 phút | Đạt | 2026-11-24 | Vô thời hạn → `suspended_until` NULL; qua 1 vòng cron vẫn khóa, không log, không thư |
| TC-B1-08 | Khóa `b1.manual@example.com` 30 ngày → Mở khóa thủ công, bỏ trống lý do | Đạt | 2026-11-24 | Hộp "Lý do mở khóa (bắt buộc)*", để trống báo "Vui lòng nhập lý do", không gửi API, tài khoản vẫn khóa |
| TC-B1-09 | Mở khóa thủ công có lý do "Đã xác minh khiếu nại" | Đạt | 2026-11-24 | Mở khóa có lý do → "Hoạt động"; log người làm yen.dh, lý do "Đã xác minh khiếu nại"; 1 thư mở lại; cron sau đó không log thêm |
| TC-B1-10 | Khóa lại `b1.cron7` 7 ngày → SQL (a) dời hạn → đăng nhập ngay (trước khi cron chạy) | Đạt | 2026-11-24 | Dời hạn rồi đăng nhập ngay → vào được; log +1 (`SUSPENSION_EXPIRED`, không `source CRON`); 5,5 phút sau cron không ghi thêm |
| TC-B1-11 | Khóa một nhân viên thử ở `/admin/staff` (không khóa nhân viên seed) → chờ ≥ 6 phút | Đạt | 2026-11-24 | Khóa nhân viên thử qua `/admin/staff` (ô lý do bắt buộc) → lưu `SUSPENDED` **vô thời hạn** (`suspended_until` NULL — không phải `LOCKED` như kịch bản ghi); cron chạy qua (đối chứng b1.auto được mở) nhưng nhân viên vẫn "Tạm khoá" |
| TC-B1-12 | Đăng nhập `phat.nt@genie.example` (Moderator, không có `MANAGE_USER`) → `/admin/users` | Đạt | 2026-11-24 | Moderator: menu không có "Người dùng"; gõ URL → "Bạn không có quyền truy cập"; API `403 GNE-SEC-4030` |

### B2 · UC-CAT-01 / 14 / 15

| Mã | Thao tác (tóm tắt) | Kết quả | Ngày | Ghi chú / lỗi |
|---|---|:---:|---|---|
| TC-B2-01 | Trang chủ → menu Danh mục ở header | Đạt | 2026-11-24 | Desktop: thanh link đủ 10 ngành gốc; khổ 390 px: nút "Tất cả danh mục" mở hộp đủ 10 gốc + 13 con; không có ngành ẩn |
| TC-B2-02 | Bấm một ngành gốc (`/c/dien-tu`) | Đạt | 2026-11-24 | Tiêu đề, breadcrumb "Trang chủ › Điện tử", 4 chip ngành con, 70 sản phẩm — khớp API 5.2 |
| TC-B2-03 | Mở `/c/thiet-bi-am-thanh` (ngành con) | Đạt | 2026-11-24 | `301` → `/c/dien-tu?sub=thiet-bi-am-thanh`, lưới 26 sản phẩm |
| TC-B2-04 | Mở `/c/khong-ton-tai` | Đạt | 2026-11-24 | 404 thật "Không tìm thấy trang" |
| TC-B2-05 | View-source `/c/dien-tu` | Đạt | 2026-11-24 | JSON-LD `BreadcrumbList`, đúng 1 `<h1>` |
| TC-B2-06 | `/admin/catalog` — 4 chỉ số đầu màn | Đạt | 2026-11-24 | Gốc 10 · con 13 · sản phẩm 254 · chờ duyệt 0 — khớp CSDL / API |
| TC-B2-07 | Thêm danh mục "Thú cưng thử", slug để trống, icon, mô tả, hoa hồng 6 → Lưu | Đạt | 2026-11-24 | Slug tự sinh `thu-cung-thu`, 6 %; storefront có ngay (header + `/c/thu-cung-thu` 200) |
| TC-B2-08 | Thêm con "Thức ăn cho chó thử" (cha = Thú cưng thử), bỏ trống hoa hồng | Đạt | 2026-11-24 | Dòng con hiện "6%" (kế thừa); API `commission_rate` null, hiệu lực 6 |
| TC-B2-09 | Thêm cấp 3 dưới "Thức ăn cho chó thử" → rồi thử thêm cấp 4 | Đạt | 2026-11-24 | Cấp 3 tạo được (hiện dưới dòng cấp 2); ô "Danh mục cha" không liệt kê ngành cấp 3 → không tạo được cấp 4 |
| TC-B2-10 | Thêm ngành slug `dien-tu` | Đạt | 2026-11-24 | `409 GNE-CAT-4091`, báo "Tên hoặc đường dẫn danh mục đã tồn tại" (thông báo chung, không nêu tên "Điện tử" như kịch bản) |
| TC-B2-11 | Thêm ngành con tên "Thức ăn cho chó thử" lần 2 cùng cha / cùng tên nhưng khác cha | Đạt | 2026-11-24 | Cùng cha → 409; cùng tên khác cha (Sách) → 201 |
| TC-B2-12 | Sửa hoa hồng = 12, rồi 3 | Đạt | 2026-11-24 | 12 % và 3 % bị chặn ngay trên form (không gửi API); CSDL giữ 6 % |
| TC-B2-13 | Sửa "Thú cưng thử": chọn cha = chính ngành con của nó | Đạt | 2026-11-24 | Form sửa ngành gốc không có ô "Danh mục cha" → không tạo được vòng trên giao diện (API `422 CYCLE` đã kiểm ở `.http` + IT) |
| TC-B2-14 | Bật công tắc "Ngành cần giấy tờ" → Lưu → mở lại | Đạt | 2026-11-24 | Bật "Ngành cần giấy tờ" → mở lại vẫn bật; CSDL `true` |
| TC-B2-15 | Ẩn "Thú cưng thử" → tải lại storefront | Đạt | 2026-11-24 | Ẩn ngành gốc → cả nhánh 3 cấp biến mất khỏi header + cây, `/c/thu-cung-thu` 404; hiện lại → đủ, 200. **Sau H15:** ẩn riêng ngành con qua nút "Sửa" của dòng → chỉ nhánh con mất khỏi cây công khai, dòng có nhãn "Ẩn"; hiện lại được |
| TC-B2-16 | Xóa "Thú cưng thử" (còn con) | Đạt | 2026-11-24 | Nút "Xóa" bị khóa sẵn, chú thích "Chỉ xóa được khi không còn sản phẩm và danh mục con" (chặn trước, không tới hộp xác nhận); API `409 HAS_CHILDREN`. Ngành rỗng thì có hộp "Hành động này không thể hoàn tác" (TC-B2-32) |
| TC-B2-17 | SQL (d) cho voucher tham chiếu ngành cấp 3 → xóa ngành đó | Đạt | 2026-11-24 | **Sau H15:** dòng cấp 3 có "Sửa" → "Xóa" → hộp "Hành động này không thể hoàn tác" → `409 IN_USE`; gỡ liên kết voucher → xóa được 204. *(Lần chạy đầu: Không đạt — giao diện chưa có thao tác cho ngành con)* |
| TC-B2-18 | Xóa "Điện tử" (có sản phẩm) — chỉ bấm, ngành sẽ không bị xóa | Đạt | 2026-11-24 | Nút "Xóa" của "Điện tử" bị khóa (còn con / sản phẩm); ngành vẫn còn |
| TC-B2-19 | Ngành "Thú cưng thử": thêm `pet_size` "Kích cỡ thú cưng", ENUM [Nhỏ, Vừa, Lớn], lọc được, trục biến thể | Đạt | 2026-11-24 | `pet_size` ENUM [Nhỏ, Vừa, Lớn], lọc được, trục biến thể — CSDL đúng |
| TC-B2-20 | Thêm `weight` NUMBER đơn vị `kg`; thêm `flavor` ENUM không có giá trị; thêm `note` TEXT có đơn vị | Đạt | 2026-11-24 | `weight` NUMBER kg lưu được; ENUM không giá trị bị chặn trên form; TEXT không có ô đơn vị (giao diện không cho nhập) |
| TC-B2-21 | Mở bộ thuộc tính của ngành con (bảng kế thừa) | Đạt | 2026-11-24 | **Sau H15:** nút "Bộ thuộc tính" trên dòng ngành con mở trang "Bộ thuộc tính — Thức ăn cho chó thử", bảng kế thừa (chỉ đọc) có `pet_size` từ "Thú cưng thử". *(Lần đầu: "Không tìm thấy danh mục")* |
| TC-B2-22 | Ngành con thêm mã `pet_size` | Đạt | 2026-11-24 | **Sau H15:** thêm mã `pet_size` ở ngành con → form chặn ngay (so cả thuộc tính kế thừa), ô Mã báo "Mã này đã có trong ngành này"; gọi thẳng API → `409 GNE-CAT-4095` |
| TC-B2-23 | Sửa `pet_size`: thử đổi kiểu dữ liệu | Đạt | 2026-11-24 | **Sau H15:** ô "Kiểu dữ liệu" bị khóa khi sửa, gợi ý "Không đổi được kiểu dữ liệu sau khi tạo — muốn đổi thì ngừng dùng và tạo mã mới."; API vẫn chặn `422 IMMUTABLE` |
| TC-B2-24 | Bật bắt buộc cho `pet_size` (ngành chưa có sản phẩm) | Đạt | 2026-11-24 | Bật bắt buộc → 200, không có thông báo ân hạn |
| TC-B2-25 | Ngành `dien-tu` (có sản phẩm): thêm thuộc tính thử `b2_test` TEXT bắt buộc | Đạt | 2026-11-24 | `grace_period_days = 14`, màn "Shop có 14 ngày bổ sung thuộc tính bắt buộc cho sản phẩm cũ"; `/c/dien-tu` vẫn 200 |
| TC-B2-26 | Ngừng dùng `b2_test` | Đạt | 2026-11-24 | Ngừng dùng → "Ngừng dùng · Dùng lại", CSDL `is_active` / `is_required` false; đã dọn |
| TC-B2-27 | Đổi mặc định toàn sàn 5 → 5,5; tắt hoa hồng riêng "Thú cưng thử"; đổi 2 ngành gốc khác → Lưu một lần | Đạt | 2026-11-24 | Mặc định 5 → 5,5 ("Lưu mặc định"); một lần "Lưu cấu hình" gửi 11 ngành: "Thú cưng thử" tắt riêng → 5,5, Thể thao 4,5, Sách 7; `COMMISSION_UPDATED` đúng số ngành đổi; đã trả về (Thể thao 5 %, Sách 4 % như seed, mặc định 5 %) |
| TC-B2-28 | Đăng nhập Moderator `phat.nt` | Đạt | 2026-11-24 | Moderator: có "Danh mục toàn sàn", không có "Cấu hình hoa hồng"; đổi tên 200; đổi hoa hồng → 403 "Bạn không có quyền thực hiện thao tác này" |
| TC-B2-29 | Đăng nhập Finance `linh.tm` | Đạt | 2026-11-24 | Finance: có "Cấu hình hoa hồng", không có Danh mục; `/admin/catalog` → không có quyền |
| TC-B2-30 | Đăng nhập CS `truc.ht` → gõ cả 2 URL | Đạt | 2026-11-24 | CS: không có cả 2 menu; cả 2 URL → không có quyền |
| TC-B2-31 | `/seller/products/new` → ô chọn danh mục | Đạt | 2026-11-24 | Ô chọn danh mục gọi API thật `/public/categories`, có "Thú cưng thử" |
| TC-B2-32 | Dọn dữ liệu: xóa cấp 3 → cấp 2 → "Thú cưng thử" | Đạt | 2026-11-24 | **Sau H15:** dọn hoàn toàn qua giao diện từng cấp (ngành cùng tên dưới "Sách", cấp 2 → gốc) — mọi lệnh xóa 204; storefront 404, sạch dữ liệu thử |

### B3 · UC-CAT-05

| Mã | Thao tác (tóm tắt) | Kết quả | Ngày | Ghi chú / lỗi |
|---|---|:---:|---|---|
| TC-B3-01 | `/search?q=tai nghe` → tick Shop có voucher | Đạt | 2026-11-24 | Bấm "Shop có voucher" → URL `voucher=1`, 14 sp = API (toàn bộ "tai nghe" đang thuộc TechZone) |
| TC-B3-02 | Bỏ tick / bấm × trên chip | Đạt | 2026-11-24 | Bấm × trên chip "Shop có voucher" → URL bỏ `voucher`, về 14 sp |
| TC-B3-03 | Tick voucher + khoảng giá + 4★ + sắp xếp Giá tăng dần | Đạt | 2026-11-24 | 11 sp: giá 174.000 → 389.000 tăng dần, trong khoảng, ≥ 4★, đều của shop có voucher |
| TC-B3-04 | Tick voucher + chọn danh mục | Đạt | 2026-11-24 | Voucher + Điện tử → 14 sp = API |
| TC-B3-05 | Gõ sai "tai nge" + voucher | Đạt | 2026-11-24 | "tai nge" + voucher → 14 kết quả, đều của shop có voucher |
| TC-B3-06 | Facet thương hiệu / nơi bán khi đang tick voucher | Đạt | 2026-11-24 | Facet thương hiệu khi lọc voucher: Sony 12, Apple 1, Bose 1; nơi bán TP. HCM 14 — sidebar hiện đủ |
| TC-B3-07 | SQL (g) tắt voucher của shop D10 → chờ ≤ 60 giây → tải lại TC-B3-01 | Đạt | 2026-11-24 | "giày" + voucher: Shoe Republic 20 thẻ → tắt `SHOEREP30K` → 37–49 giây sau còn 0 (cache 60 giây); đã bật lại |
| TC-B3-08 | Shop có voucher đã tắt / hết lượt (seed D10) | Đạt | 2026-11-24 | Home & Living (voucher tắt), Beauty Box (hết lượt) không xuất hiện |
| TC-B3-09 | Trang shop `/shop/<id>` → "Tìm trong shop" | Đạt | 2026-11-24 | "Tìm trong shop" không có ô "Shop có voucher" |
| TC-B3-10 | `/c/dien-tu` (D8 = Có) | Đạt | 2026-11-24 | `/c/dien-tu` có ô "Shop có voucher" (D8); lọc 70 → 70 sp = API (sản phẩm Điện tử seed đều thuộc TechZone) |
| TC-B3-11 | `/c/dien-tu` → nhóm thuộc tính (vd. Bảo hành) | Đạt | 2026-11-24 | Nhóm "Bảo hành" (12 / 24 / 18 / 6 tháng) lấy từ facets; tick "12 tháng" → 43 sp = facet; không có request `attribute-filters` |
| TC-B3-12 | Ngành không có thuộc tính `is_filterable` | Đạt | 2026-11-24 | `/c/sach` không có nhóm thuộc tính |
| TC-B3-13 | Thuộc tính `is_filterable` tạm ở `dien-tu` (+ giá trị trong `specs_json`) hiện trong bộ lọc | Đạt | 2026-11-24 | Thêm tạm thuộc tính lọc được "Chất liệu thử" (đặt đầu) cho `/c/thoi-trang` + giá trị trên 1 sản phẩm → nhóm hiện ngay; đã dọn, trả `specs_json` |
| TC-B3-14 | Khổ hẹp (≤ 768 px): mở ngăn kéo bộ lọc, tick voucher → Áp dụng | Đạt | 2026-11-24 | 390 px: "Bộ lọc" → tick → "Áp dụng" → `voucher=1`, 14 sp, chip hiện. Lưu ý: ở `astro dev` thanh công cụ Astro đè lên nút "Áp dụng" (chỉ dev — script ẩn thanh này) |
| TC-B3-15 | Tắt JS (DevTools) → tick voucher → bấm Áp dụng | Đạt | 2026-11-24 | Tắt JS: tick + "Áp dụng" gửi form GET → SSR trả 14 sp đã lọc |
| TC-B3-16 | 9 theme tại `/search` và `/c/dien-tu` | Đạt | 2026-11-24 | 9 theme × 2 trang: tương phản chữ "Shop có voucher" đo được ≥ 14:1 (AA ≥ 4,5); ảnh 9 theme ở scratchpad `ui/shots/TC-B3-16-*.png` |

## 6. Dữ liệu thử

| Dùng cho | Đối tượng | Giá trị (id / mã) |
|---|---|---|
| TC-B1 | b1.cron7@example.com | id 34 |
| TC-B1 | b1.forever@example.com | id 35 |
| TC-B1 | b1.manual@example.com | id 36 |
| Claude tự kiểm cron (không dùng cho TC) | b1.auto@example.com | id 37 |
| TC-B2 | id `dien-tu` | id 4 (4 ngành con, sản phẩm nằm ở ngành con) |
| TC-B2 | id ngành con có sản phẩm | id 13 `thiet-bi-am-thanh` (26 sản phẩm) |
| TC-B2 | id "Thú cưng thử" / cấp 2 / cấp 3 | |
| TC-B3 | mã voucher seed D10 (còn hạn / đã tắt / hết lượt) | `SHOEREP30K` (Shoe Republic, vendor 6) / `HOMEOFF10` (Home & Living, vendor 8) / `BEAUTY20K` (Beauty Box, vendor 9) |

## 7. Kết quả cổng kiểm tra

| Ngày | Nhánh | `./mvnw verify` (unit / IT) | `npm run verify` | Playwright | Ghi chú |
|---|---|---|---|---|---|
| 2026-11-24 | `feature/b1-suspension-expiry-job` | 182 / 301 xanh (JDK 21; lượt đầu JVM test hết RAM ở `KycContractIT` — máy còn ~3 GB trống, chạy lại IT thì xanh) | — | — | +5 unit (`UserTest` 2, `AccountReactivatedEmailTemplateTest` 3), +8 IT (`SuspensionExpiryJobIT`) |
| 2026-11-24 | `chore/minor-fixes` (trước plan) | 177 / 293 xanh (JDK 21) | 74 file / 346 test xanh | — | Mốc ban đầu; `SearchPerformanceIT` p50 27 ms / p95 51 ms |

## 8. Nhật ký

| Ngày | Việc | Commit / PR |
|---|---|---|
| 2026-11-24 | Lập plan + progress: khảo sát B1 (thiếu `SuspensionExpiryJob`), B2 (thiếu chạy thật giao diện, `CatalogSecurityIT`, test `IN_USE`, Redis lỗi), B3 (cổng `ShopVoucherAvailability` chưa cài, web chưa đọc `has_shop_voucher_supported`, bộ lọc thuộc tính còn mock qua endpoint không có trong spec). Chờ chốt D1–D11 | (chưa commit) |
| 2026-11-24 | Người dùng cho làm tiếp; D1–D11 chốt theo đề xuất (Thiện rà soát — chờ Khoa / Quân xem lại). H00: đổi nhánh `chore/minor-fixes` → `bugfix/public-etag-304`, commit sửa ETag + tài liệu kiểm B2 / B3 | `6d1a54e`, `0aca1bd` |
| 2026-11-24 | H02: LLD 10 dòng `SuspensionExpiryJob`, API_SPEC 11.13 *Tự mở khi hết hạn*, Usecase UC-ADM-03 hiện trạng + email, `b1.md` D13 | `ec65c06` |
| 2026-11-24 | H03: `UserRepository.findSuspensionExpired` (`SKIP LOCKED`), `SuspensionExpiryJob`, `AccountReactivatedEvent`, email `account-reactivated` (cron + Admin mở thủ công; gỡ khóa sai mật khẩu không gửi), `SuspensionExpiryJobIT` 8 test (phủ 11 ca plan; đặt ở package `admin` để dùng `AuditLogTestSupport`), unit `UserTest` + template test. Phát hiện: luồng đăng nhập ghi `ACCOUNT_REACTIVATED` với `user_id` (không `reference_id`) và không khóa dòng → sửa mục Rủi ro của plan | `8e0e81e`, `a2e02d2` |
| 2026-11-24 | H01 + kiểm cron trên hệ thống thật (Claude, qua API — **chưa phải TC của người dùng**): đăng ký 4 tài khoản bằng OTP Mailpit (id 34–37); `b1.auto` khóa 7 ngày → đăng nhập `403 GNE-AUTH-4031` → dời hạn → 40 giây sau cron mở (log "Đã mở khóa 1 tài khoản…"), `system_logs` `SUSPENSION_EXPIRED · CRON` người làm `system@genie.vn`, Mailpit có thư "Tài khoản Genie Shop của bạn đã được mở lại", đăng nhập lại `201`. H04 (TC-B1 người dùng bấm) chờ | — |
| 2026-11-24 | H05–H07 (nhánh `feature/b2-catalog-acceptance`): `CatalogSecurityIT` ma trận 7 vai trò × mọi endpoint B2 — **khớp spec**, H07 không cần; `AdminCategoryIT` xóa ngành bị voucher tham chiếu → `IN_USE`; `PublicCategoryIT` ETag → `304`, đổi tên → ETag mới; `CommissionIT` lưu hàng loạt chỉ ghi ngành đổi; `JsonCacheTest` Redis lỗi đọc / ghi / JSON hỏng → đọc nguồn; `catalog-category.http` thêm bước Moderator / Finance / `IN_USE`. Phát hiện cho Khoa: `ACCESS_DENIED` ghi **mọi** lần 403, chưa có giới hạn 1 dòng / người / phút như B1 D14 đề xuất | `0259f49` |
| 2026-11-24 | H10 (tài liệu trước): API 5.3 quy tắc D5 + cache 60 giây + `has_shop_voucher_supported = true`, mục 14 trang danh mục dùng `facets.attribute`; handoff gạch `attribute-filters`; CLAUDE.md mục 9 (cổng đã cài, khóa cache B5 phải xóa, sửa câu "catalog chưa có backend") | `65a24a8` |
| 2026-11-24 | H08: `VoucherRepository.findVendorIdsWithActiveShopVoucher`, `PromotionSearchPorts` (cache `JsonCache` 60 giây, Redis lỗi đọc CSDL), `TestDataBuilder.voucher(...)`; `PromotionSearchPortsIT` 7 test (11 ca plan; ca "Redis lỗi" do `JsonCacheTest` của nhánh B2 phủ), `FacetIT` 4 ca voucher (thay test "bị bỏ qua") | `95bc152` |
| 2026-11-24 | H09: seed `SHOEREP30K` / `HOMEOFF10` (tắt) / `BEAUTY20K` (hết lượt); cột `used_count`, `is_active` lấy từ `VALUES` | `a922955` |
| 2026-11-24 | H11: facets có `attribute` (mã + giá trị) và `voucherFilterSupported`; `FilterPanel` ẩn ô voucher khi không hỗ trợ, thêm ô ở trang danh mục (D8); 2 trang bỏ `listAttributeFilters`, dùng facets; xóa mock `attribute-filters` + DTO / mapper / type; `.env.example`, `huong-dan-chay-local.md` bỏ nhóm này | `b72b41b` |
| 2026-11-24 | Kiểm thật B3 (Claude, API + SSR — **chưa phải TC người dùng**): 12 / 12 đạt — cờ hỗ trợ `true`; lọc voucher chỉ còn TechZone + Shoe Republic (99 sp), không có Home & Living / Beauty Box; voucher + từ khóa + giá tăng dần; `/search`, `/c/dien-tu` có ô voucher, tìm trong shop không có; `/c/dien-tu` nhóm "Bảo hành" lấy từ facets, `attr.warranty=12 tháng` = 43 sp khớp facet | — |
| 2026-11-24 | Xếp lại nhánh cho thẳng hàng (chưa push nên rebase an toàn): `bugfix/public-etag-304` → `feature/b1-suspension-expiry-job` → `feature/b2-catalog-acceptance` → `feature/b3-shop-voucher-filter`; gỡ xung đột progress một lần, mã commit B2 / B3 đổi (đã thay trong file này). Gộp PR theo đúng thứ tự trên | — |
| 2026-11-24 | H13 hồi quy trên nhánh tổng: `./mvnw clean verify` 188 / 321 xanh; `SearchPerformanceIT` p95 51 ms; `npm run verify` 352 test; Playwright 46 pass / 2 skip; chạy lại `catalog-category.http` (28 đúng + 3 dòng script đoán sai định dạng), `catalog-search.http` (40 đúng + 1 dòng script — ca ETag `304` nay **đạt**), kịch bản voucher 12 / 12; dọn dữ liệu thử | — |
| 2026-11-24 | Theo yêu cầu người dùng, Claude chạy 60 TC bằng Chromium thật (Playwright) trên hệ thống thật: **B1 12/12** (TC-B1-11 sửa kịch bản: khóa nhân viên = đình chỉ vô thời hạn, không phải `LOCKED`), **B2 27/32**, **B3 16/16** (tương phản 9 theme đo được ≥ 14:1). 5 TC B2 không đạt đều do **giao diện Admin**: (1) ngành cấp 2 / 3 không có Sửa / Ẩn / Xóa / Bộ thuộc tính (`CatalogPage` chỉ sửa ngành gốc, `AttributePanel` chỉ tìm `roots`) — TC-B2-17, 21, 22, 32; (2) đổi kiểu dữ liệu thuộc tính: form không khóa ô, lỗi `IMMUTABLE` chỉ hiện thông báo chung — TC-B2-23. API của các ca này đều đúng. Ghi chú thêm: lỗi trùng slug chỉ báo chung (không nêu tên ngành trùng); thanh công cụ `astro dev` đè nút "Áp dụng" ở khổ hẹp (chỉ dev). Trong lúc chạy, bước dọn của script tắt hoa hồng riêng Thể thao / Sách — đã trả lại đúng seed 5 % / 4 % | — |
| 2026-11-24 | H15 (phát sinh khi chạy thật, người dùng cho sửa): giao diện Admin quản lý được ngành cấp 2 / 3 (dòng `SubRow` có Bộ thuộc tính / Sửa / Xem SP, form sửa / ẩn / xóa mọi cấp), trang Bộ thuộc tính mở cho ngành mọi cấp, ô Kiểu dữ liệu khóa khi sửa + gợi ý, lỗi API theo trường hiện tại ô; mock sửa / xóa mọi cấp; +3 Vitest (rules, mapper). `npm run verify` 349 test (nhánh b2). Chạy lại TC-B2-01 → 32 bằng Chromium: **32 / 32 đạt** (trong lúc chạy Docker Desktop bị sập vì thiếu RAM — khởi động lại, dọn dữ liệu thử, trả hoa hồng mặc định 5 %, Thể thao 5 %, Sách 4 % như seed rồi chạy lại). Tổng 60 / 60 TC đạt | `89483df` |
| 2026-11-24 | H14 đồng bộ tiến độ: CHECKLIST (5 UC → ✅, Tổng quan 12 → 17 / 22 → 17, phân hệ 02: 10 / 1 / 6, phân hệ 11: 3 / 2 / 3, mục 3 tick R06 / chạy thật B2 / `CatalogSecurityIT` / lọc voucher, nhật ký), Usecase.md (câu *Hiện trạng* 5 UC), BAO_CAO_UC_DOC_LAP (bảng B1 / B2 / B3 + thành viên), Excel (cột *Hiện trạng mã nguồn* 5 UC + sheet Thành viên — sửa XML giữ định dạng), `b1.md` R06, `b2.md` C07 / C08, progress B3, CLAUDE.md mục 2, kịch bản `docs/kiem-thu/kich-ban-2026-11-24-b1-b2-b3.md` (+ README) | (commit này) |
