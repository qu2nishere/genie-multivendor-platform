# PROGRESS — Hoàn thiện phần còn thiếu của cụm B1, B2, B3

> Plan: [plan_hoan_thien_b1_b2_b3.md](plan_hoan_thien_b1_b2_b3.md) · Ngày lập: 2026-11-24 · Rà soát / làm bù: Nguyễn Ngọc Thiện · Phụ trách: Nguyễn Quốc Khoa (B1, B2), Nghiêm Phú Đăng Quân (B3)
> Cách dùng: xong task nào thì đổi `- [ ]` → `- [x]` và ghi một dòng **Nhật ký** (ngày, việc, mã commit). Kịch bản chạy thật ghi **Đạt / Không đạt** vào bảng mục 5. Task không cần làm ghi `- [x] ~~…~~ (không cần: lý do)`.

## 1. Tổng quan

| UC | GD | BE | Test | Nối | Chạy | Trạng thái hiện tại | Mục tiêu |
|---|:---:|:---:|:---:|:---:|:---:|---|---|
| UC-ADM-03 Khóa / Mở khóa | ☑ | ◐ | ◐ | ☑ | ◐ | 🟡 thiếu cron hết hạn đình chỉ | ✅ |
| UC-CAT-01 Cây danh mục | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 chưa chạy thật giao diện | ✅ |
| UC-CAT-14 Danh mục & hoa hồng | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 chưa chạy thật giao diện | ✅ |
| UC-CAT-15 Bộ thuộc tính | ☑ | ☑ | ☑ | ☑ | ☐ | 🟡 chưa chạy thật giao diện | ✅ |
| UC-CAT-05 Lọc & sắp xếp | ☑ | ◐ | ☑ | ◐ | ◐ | 🟡 thiếu lọc "Shop có voucher", bộ lọc thuộc tính còn mock | ✅ |

**Tổng quan dự án khi xong:** Đã triển khai 12 → 17 / 87 · Đang triển khai 22 → 17.

## 2. Tiến độ theo giai đoạn

| Giai đoạn | Việc | Số task | Trạng thái | Commit / PR |
|---|---|---:|:---:|---|
| H00 | Chuẩn bị (làm trước tiên) | 7 | ◐ (còn H00.06 push / PR) | `6d1a54e`, `0aca1bd` |
| H01 | B1: chuẩn bị dữ liệu thử | 2 | ☑ | — |
| H02 | B1: tài liệu trước | 5 | ☑ | `ec65c06` |
| H03 | B1: backend + test | 9 | ☑ | `8e0e81e`, `a2e02d2` |
| H04 | B1: chạy thật (kịch bản TC-B1 ở mục 6.1) | 2 | ☐ | |
| H05 | B2: chuẩn bị | 2 | ☑ | — |
| H06 | B2: test tự động còn thiếu | 8 | ☑ | `ca20841` |
| H07 | B2: tài liệu trước (chỉ khi H06 lộ lệch spec) | 2 | ☑ (không cần) | — |
| H08 | B3: cổng "shop có voucher" (backend) | 7 | ☑ | `b9ffd83` |
| H09 | B3: seed (D10) | 3 | ☑ | `2b10dac` |
| H10 | B3: tài liệu | 5 | ☑ | `d4793b6` |
| H11 | B3: web | 9 | ◐ (còn H11.07 kiểm 9 theme bằng mắt) | `9559019` |
| H12 | B2 + B3: chạy thật trên giao diện (kịch bản TC-B2, TC-B3 ở mục 6.2, 6.3) | 3 | ☐ | |
| H13 | Hồi quy toàn bộ | 4 | ☐ | |
| H14 | Đồng bộ tiến độ & đóng plan | 6 | ☐ | |

**Tổng:** 74 task · 60 kịch bản chạy thật.

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

- [ ] **H04.01** Chạy TC-B1-01 → TC-B1-12 (người dùng tự bấm), ghi mục 5
- [ ] **H04.02** Lỗi phát sinh → sửa + test hồi quy + chạy lại TC, commit `fix(auth): …` riêng

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
- [ ] **H11.07** Kiểm 9 theme + khổ hẹp ở `/search`, `/c/dien-tu` (người dùng nhìn bằng mắt — gộp vào TC-B3-14, TC-B3-16)
- [x] **H11.08** `npm run verify`
- [x] **H11.09** COMMIT `feat(web): drive voucher and attribute filters from search facets` (tách nếu > 400 dòng)

### H12 — B2 + B3: chạy thật trên giao diện (kịch bản TC-B2, TC-B3 ở mục 6.2, 6.3)

- [ ] **H12.01** Chạy TC-B2-01 → TC-B2-32 (người dùng tự bấm), ghi mục 5
- [ ] **H12.02** Chạy TC-B3-01 → TC-B3-16, ghi mục 5
- [ ] **H12.03** Lỗi phát sinh → sửa + test hồi quy + chạy lại TC, commit `fix(...)` riêng

### H13 — Hồi quy toàn bộ

- [ ] **H13.01** `./mvnw clean verify` (JDK 21), ghi số mục 7
- [ ] **H13.02** `SearchPerformanceIT -Dperf=true` có lọc voucher (p95 không tệ hơn 20 %)
- [ ] **H13.03** `npm run verify` + `npm run test:e2e`
- [ ] **H13.04** Chạy lại `catalog-category.http`, `catalog-search.http` (thêm ca `has_shop_voucher`)

### H14 — Đồng bộ tiến độ & đóng plan

- [ ] **H14.01** CHECKLIST: tick 5 UC → ✅, Tổng quan 12 → 17, bảng phân hệ, mục 3, nhật ký
- [ ] **H14.02** Usecase.md, BAO_CAO_UC_DOC_LAP, Excel (sửa XML giữ định dạng)
- [ ] **H14.03** `b1.md` R06, `b2.md` chạy thật + `CatalogSecurityIT`, progress B3
- [ ] **H14.04** Chép kịch bản sang `docs/kiem-thu/kich-ban-2026-11-24-b1-b2-b3.md`
- [ ] **H14.05** `CLAUDE.md` mục 2: dòng *Plan 2026-11-24*
- [ ] **H14.06** COMMIT `docs: update progress after completing b1, b2 and b3 use cases` + PR từng nhánh

## 5. Kết quả chạy thật

### B1 · UC-ADM-03

| Mã | Thao tác (tóm tắt) | Kết quả | Ngày | Ghi chú / lỗi |
|---|---|:---:|---|---|
| TC-B1-01 | `/admin/users` → tìm `b1.cron7@example.com` → Khóa, lý do `ABNORMAL_RETURNS`, thời hạn 7 ngày | ☐ | | |
| TC-B1-02 | Đăng nhập `b1.cron7@example.com` | ☐ | | |
| TC-B1-03 | SQL (a) dời hạn về 1 phút trước; không đăng nhập; chờ tối đa 5 phút (xem log backend) | ☐ | | |
| TC-B1-04 | `/admin/audit-logs`, lọc nhóm AUTH / tìm email | ☐ | | |
| TC-B1-05 | Mailpit http://localhost:8025 | ☐ | | |
| TC-B1-06 | Đăng nhập `b1.cron7@example.com` | ☐ | | |
| TC-B1-07 | Khóa `b1.forever@example.com` vô thời hạn → SQL (b) kiểm `suspended_until` NULL → chờ ≥ 6 phút | ☐ | | |
| TC-B1-08 | Khóa `b1.manual@example.com` 30 ngày → Mở khóa thủ công, bỏ trống lý do | ☐ | | |
| TC-B1-09 | Mở khóa thủ công có lý do "Đã xác minh khiếu nại" | ☐ | | |
| TC-B1-10 | Khóa lại `b1.cron7` 7 ngày → SQL (a) dời hạn → đăng nhập ngay (trước khi cron chạy) | ☐ | | |
| TC-B1-11 | Khóa một nhân viên thử ở `/admin/staff` (không khóa nhân viên seed) → chờ ≥ 6 phút | ☐ | | |
| TC-B1-12 | Đăng nhập `phat.nt@genie.example` (Moderator, không có `MANAGE_USER`) → `/admin/users` | ☐ | | |

### B2 · UC-CAT-01 / 14 / 15

| Mã | Thao tác (tóm tắt) | Kết quả | Ngày | Ghi chú / lỗi |
|---|---|:---:|---|---|
| TC-B2-01 | Trang chủ → menu Danh mục ở header | ☐ | | |
| TC-B2-02 | Bấm một ngành gốc (`/c/dien-tu`) | ☐ | | |
| TC-B2-03 | Mở `/c/thiet-bi-am-thanh` (ngành con) | ☐ | | |
| TC-B2-04 | Mở `/c/khong-ton-tai` | ☐ | | |
| TC-B2-05 | View-source `/c/dien-tu` | ☐ | | |
| TC-B2-06 | `/admin/catalog` — 4 chỉ số đầu màn | ☐ | | |
| TC-B2-07 | Thêm danh mục "Thú cưng thử", slug để trống, icon, mô tả, hoa hồng 6 → Lưu | ☐ | | |
| TC-B2-08 | Thêm con "Thức ăn cho chó thử" (cha = Thú cưng thử), bỏ trống hoa hồng | ☐ | | |
| TC-B2-09 | Thêm cấp 3 dưới "Thức ăn cho chó thử" → rồi thử thêm cấp 4 | ☐ | | |
| TC-B2-10 | Thêm ngành slug `dien-tu` | ☐ | | |
| TC-B2-11 | Thêm ngành con tên "Thức ăn cho chó thử" lần 2 cùng cha / cùng tên nhưng khác cha | ☐ | | |
| TC-B2-12 | Sửa hoa hồng = 12, rồi 3 | ☐ | | |
| TC-B2-13 | Sửa "Thú cưng thử": chọn cha = chính ngành con của nó | ☐ | | |
| TC-B2-14 | Bật công tắc "Ngành cần giấy tờ" → Lưu → mở lại | ☐ | | |
| TC-B2-15 | Ẩn "Thú cưng thử" → tải lại storefront | ☐ | | |
| TC-B2-16 | Xóa "Thú cưng thử" (còn con) | ☐ | | |
| TC-B2-17 | SQL (d) cho voucher tham chiếu ngành cấp 3 → xóa ngành đó | ☐ | | |
| TC-B2-18 | Xóa "Điện tử" (có sản phẩm) — chỉ bấm, ngành sẽ không bị xóa | ☐ | | |
| TC-B2-19 | Ngành "Thú cưng thử": thêm `pet_size` "Kích cỡ thú cưng", ENUM [Nhỏ, Vừa, Lớn], lọc được, trục biến thể | ☐ | | |
| TC-B2-20 | Thêm `weight` NUMBER đơn vị `kg`; thêm `flavor` ENUM không có giá trị; thêm `note` TEXT có đơn vị | ☐ | | |
| TC-B2-21 | Mở bộ thuộc tính của ngành con (bảng kế thừa) | ☐ | | |
| TC-B2-22 | Ngành con thêm mã `pet_size` | ☐ | | |
| TC-B2-23 | Sửa `pet_size`: thử đổi kiểu dữ liệu | ☐ | | |
| TC-B2-24 | Bật bắt buộc cho `pet_size` (ngành chưa có sản phẩm) | ☐ | | |
| TC-B2-25 | Ngành `dien-tu` (có sản phẩm): thêm thuộc tính thử `b2_test` TEXT bắt buộc | ☐ | | |
| TC-B2-26 | Ngừng dùng `b2_test` | ☐ | | |
| TC-B2-27 | Đổi mặc định toàn sàn 5 → 5,5; tắt hoa hồng riêng "Thú cưng thử"; đổi 2 ngành gốc khác → Lưu một lần | ☐ | | |
| TC-B2-28 | Đăng nhập Moderator `phat.nt` | ☐ | | |
| TC-B2-29 | Đăng nhập Finance `linh.tm` | ☐ | | |
| TC-B2-30 | Đăng nhập CS `truc.ht` → gõ cả 2 URL | ☐ | | |
| TC-B2-31 | `/seller/products/new` → ô chọn danh mục | ☐ | | |
| TC-B2-32 | Dọn dữ liệu: xóa cấp 3 → cấp 2 → "Thú cưng thử" | ☐ | | |

### B3 · UC-CAT-05

| Mã | Thao tác (tóm tắt) | Kết quả | Ngày | Ghi chú / lỗi |
|---|---|:---:|---|---|
| TC-B3-01 | `/search?q=tai nghe` → tick Shop có voucher | ☐ | | |
| TC-B3-02 | Bỏ tick / bấm × trên chip | ☐ | | |
| TC-B3-03 | Tick voucher + khoảng giá + 4★ + sắp xếp Giá tăng dần | ☐ | | |
| TC-B3-04 | Tick voucher + chọn danh mục | ☐ | | |
| TC-B3-05 | Gõ sai "tai nge" + voucher | ☐ | | |
| TC-B3-06 | Facet thương hiệu / nơi bán khi đang tick voucher | ☐ | | |
| TC-B3-07 | SQL (g) tắt voucher của shop D10 → chờ ≤ 60 giây → tải lại TC-B3-01 | ☐ | | |
| TC-B3-08 | Shop có voucher đã tắt / hết lượt (seed D10) | ☐ | | |
| TC-B3-09 | Trang shop `/shop/<id>` → "Tìm trong shop" | ☐ | | |
| TC-B3-10 | `/c/dien-tu` (D8 = Có) | ☐ | | |
| TC-B3-11 | `/c/dien-tu` → nhóm thuộc tính (vd. Bảo hành) | ☐ | | |
| TC-B3-12 | Ngành không có thuộc tính `is_filterable` | ☐ | | |
| TC-B3-13 | Thuộc tính `is_filterable` tạm ở `dien-tu` (+ giá trị trong `specs_json`) hiện trong bộ lọc | ☐ | | |
| TC-B3-14 | Khổ hẹp (≤ 768 px): mở ngăn kéo bộ lọc, tick voucher → Áp dụng | ☐ | | |
| TC-B3-15 | Tắt JS (DevTools) → tick voucher → bấm Áp dụng | ☐ | | |
| TC-B3-16 | 9 theme tại `/search` và `/c/dien-tu` | ☐ | | |

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
| 2026-11-24 | H05–H07 (nhánh `feature/b2-catalog-acceptance`): `CatalogSecurityIT` ma trận 7 vai trò × mọi endpoint B2 — **khớp spec**, H07 không cần; `AdminCategoryIT` xóa ngành bị voucher tham chiếu → `IN_USE`; `PublicCategoryIT` ETag → `304`, đổi tên → ETag mới; `CommissionIT` lưu hàng loạt chỉ ghi ngành đổi; `JsonCacheTest` Redis lỗi đọc / ghi / JSON hỏng → đọc nguồn; `catalog-category.http` thêm bước Moderator / Finance / `IN_USE`. Phát hiện cho Khoa: `ACCESS_DENIED` ghi **mọi** lần 403, chưa có giới hạn 1 dòng / người / phút như B1 D14 đề xuất | `ca20841` |
| 2026-11-24 | H10 (tài liệu trước): API 5.3 quy tắc D5 + cache 60 giây + `has_shop_voucher_supported = true`, mục 14 trang danh mục dùng `facets.attribute`; handoff gạch `attribute-filters`; CLAUDE.md mục 9 (cổng đã cài, khóa cache B5 phải xóa, sửa câu "catalog chưa có backend") | `d4793b6` |
| 2026-11-24 | H08: `VoucherRepository.findVendorIdsWithActiveShopVoucher`, `PromotionSearchPorts` (cache `JsonCache` 60 giây, Redis lỗi đọc CSDL), `TestDataBuilder.voucher(...)`; `PromotionSearchPortsIT` 7 test (11 ca plan; ca "Redis lỗi" do `JsonCacheTest` của nhánh B2 phủ), `FacetIT` 4 ca voucher (thay test "bị bỏ qua") | `b9ffd83` |
| 2026-11-24 | H09: seed `SHOEREP30K` / `HOMEOFF10` (tắt) / `BEAUTY20K` (hết lượt); cột `used_count`, `is_active` lấy từ `VALUES` | `2b10dac` |
| 2026-11-24 | H11: facets có `attribute` (mã + giá trị) và `voucherFilterSupported`; `FilterPanel` ẩn ô voucher khi không hỗ trợ, thêm ô ở trang danh mục (D8); 2 trang bỏ `listAttributeFilters`, dùng facets; xóa mock `attribute-filters` + DTO / mapper / type; `.env.example`, `huong-dan-chay-local.md` bỏ nhóm này | `9559019` |
| 2026-11-24 | Kiểm thật B3 (Claude, API + SSR — **chưa phải TC người dùng**): 12 / 12 đạt — cờ hỗ trợ `true`; lọc voucher chỉ còn TechZone + Shoe Republic (99 sp), không có Home & Living / Beauty Box; voucher + từ khóa + giá tăng dần; `/search`, `/c/dien-tu` có ô voucher, tìm trong shop không có; `/c/dien-tu` nhóm "Bảo hành" lấy từ facets, `attr.warranty=12 tháng` = 43 sp khớp facet | — |
