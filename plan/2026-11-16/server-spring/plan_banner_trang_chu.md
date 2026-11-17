# PLAN — Quản lý banner trang chủ & chiến dịch tĩnh (UC-ADM-12)

| | |
|---|---|
| **Ngày lập** | 2026-11-16 |
| **Thể loại** | server-spring (module `admin`, feature `banner`) + nối `web/` (feature `promotions`, `admin-marketing`) |
| **Use case** | Quản lý banner trang chủ & chiến dịch tĩnh (UC-ADM-12) — Sprint 8 theo implement 1.2, làm sớm vì độc lập |
| **Tài liệu chuẩn** | UC phân hệ 11 (UC-ADM-12) · API 5.14, 5.15, 5.17, 5.18, 11.16, 4.1, 0.12, 15 · ENTITY 10.4 (`HomeBanner`), Phụ lục A (`BannerPosition`) · LUU_TRU 4.3–4.4 (cache, bộ đếm), 5.4–5.6 · LLD 10 (job ghi gộp bộ đếm) · TS 4, 8, 9, 10, 14 — xem [README](../README.md) |
| **Theo dõi tiến độ** | [progress_banner_trang_chu.md](./progress_banner_trang_chu.md) — cùng mã task `Bxx.yy` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |

---

## 0. Hiện trạng đã khảo sát

| Phần | Có gì | Thiếu |
|---|---|---|
| `admin/banner/domain` | `HomeBanner` (17 trường: `title`, `imageUrl`, `mobileImageUrl`, `targetUrl`, `position`, `categoryId`, `sortOrder`, `startsAt`, `endsAt`, `isActive`, `targetAudienceJson`, `createdByAdminId`, `impressionCount`, `clickCount`…; phương thức `isLive(now)`, `deactivate()`, `recordStats(imp, clk)`, `ctr()`), `BannerPosition` (6 giá trị), `HomeBannerRepository` (rỗng) | Toàn bộ `api`, `application`; truy vấn repository |
| CSDL | `home_banners` (`V10__notification.sql`) có index theo vị trí / thời gian | `CHECK` của `system_logs.action_type` chưa có hành động cho banner (UC: "mọi thay đổi ghi `system_logs`") |
| `common/storage` | `UploadPurpose` mới có `AVATAR`; `UploadService` (xin URL, `promote`, xóa), `S3ObjectStorage` | Purpose `BANNER_IMAGE` (bucket `content-assets`, jpeg / png / webp, 5 MB, STAFF có `MANAGE_VOUCHER` — API 4.1) |
| `common/error/ErrorCode` | Chưa có `GNE-BNR-4091`, `GNE-BNR-4221`, `GNE-BNR-4222` | Thêm 3 mã (đã có trong API 15) |
| `web/features/promotions` | `banner.api.ts`: `listLiveBanners` (`GET /public/banners`), `trackBanner` (`POST /public/banners/{id}/events` — **lệch spec**: phải là `POST /public/banner-events` gộp lô) | Sửa đường dẫn + gom lô 5 giây |
| `web/features/admin-marketing` | Màn Banner xong trên mock: `listBanners`, `saveBanner` (`POST` / `PUT`), `setBannerActive` (`PATCH`), `deleteBanner` | Nối API thật; ô chọn shop đích đang dùng `shop.listShops` công khai (handoff mục 4) |
| Seed | Plan [seed](../db/plan_seed_du_lieu_mau.md) S05.03 tạo banner mẫu + ảnh `content-assets/seed/banner/…` | — (không bắt buộc để bắt đầu) |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Package `com.genie.admin.banner.{api,application,domain}`; controller không mở transaction; một thao tác = một method `@Transactional` ở `application` | TS 4.1–4.2 |
| N2 | Luật nghiệp vụ nằm trong entity (`isLive`, `deactivate`, phương thức mới cho trạng thái / kiểm lịch); sai trạng thái → `IllegalStateException`, service ánh xạ sang mã `GNE-BNR-*` | TS 7.2 |
| N3 | Quyền quản trị: `@PreAuthorize("hasAuthority('MANAGE_VOUCHER')")` (Admin + Finance Officer mặc định); endpoint `/public/**` không cần đăng nhập | API 0.10, 11.16 |
| N4 | **Liên kết đích chỉ trong miền sàn** — chống chuyển hướng mở: chỉ nhận đường dẫn tương đối bắt đầu bằng một dấu `/`; từ chối `//`, `/\`, có `scheme`, ký tự điều khiển → `422 GNE-BNR-4222` | UC-ADM-12 ngoại lệ |
| N5 | Ảnh: xin Presigned PUT (`purpose = BANNER_IMAGE`) → PUT thẳng Silo → gửi `image_object_key` trong chính request tạo / sửa (không upload qua Spring, không bước confirm riêng); CSDL lưu **khóa đối tượng**, API trả URL ghép từ `PUBLIC_S3_URL` | API 0.8, 4.1; LUU_TRU 5.5–5.6 |
| N6 | Cache & bộ đếm ở Redis chỉ để tăng tốc / gom số liệu — mất Redis không sai dữ liệu: đọc banner rơi về PostgreSQL, bộ đếm mất tối đa 5 phút | LUU_TRU 4.7–4.8 |
| N7 | Xóa cache `AFTER_COMMIT` khi tạo / sửa / tắt / bật / xóa banner | TS 4.5 |
| N8 | Mọi thay đổi ghi `system_logs` qua `AuditLogger` trong cùng transaction | UC-ADM-12 ràng buộc; NFR-08 |
| N9 | JSON snake_case, vỏ `ApiResponse`; danh sách Admin phân trang `page` / `size` (API 11.16); `@Operation(summary = "UC-ADM-12 …")` | TS 8 |
| N10 | Mỗi task có test: unit entity / validator + `*IT` Testcontainers (PG + Redis + Silo) | TS 14 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Nguồn lệch | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | Hành động kiểm toán cho banner chưa có trong `AuditAction` | ENTITY Phụ lục A vs UC | Thêm **một** giá trị `BANNER_CHANGED` (chi tiết `op` = `CREATE / UPDATE / ACTIVATE / DEACTIVATE / DELETE` trong `details`), nhóm `CONFIG`; migration `V16__banner_audit_action.sql` thay `ck_system_logs_action_type` (đăng ký ở [README mục 4](../README.md)); sửa ENTITY Phụ lục A trước | ☐ |
| D2 | "Tối đa 8 banner đang hiển thị mỗi vị trí" đếm thế nào với banner hẹn giờ | UC bước 3 | Khi lưu banner **đang bật**: đếm banner bật khác ở cùng `position` (với `CATEGORY_TOP`: cùng `category_id`) có khoảng [`starts_at`, `ends_at`] **giao** với banner đang lưu và chưa kết thúc; ≥ 8 → `422 GNE-BNR-4221`. Bật lại (`PATCH`) cũng kiểm | ☐ |
| D3 | `state` (API: `LIVE, SCHEDULED, ENDED, OFF`) | API 11.16 | Server tính: `!is_active` → `OFF`; `now < starts_at` → `SCHEDULED`; `now > ends_at` → `ENDED`; còn lại `LIVE`. Thêm phương thức `HomeBanner.state(now)` (ghi vào ENTITY Phụ lục E) | ☐ |
| D4 | Khóa đối tượng ảnh | LUU_TRU 5.5 | `content-assets/banner/{bannerId}/{desktop\|mobile}-{uuid}.{ext}`; tạo mới: lưu banner lấy `id` rồi `promote` trong cùng transaction; đổi ảnh: xóa ảnh cũ `AFTER_COMMIT`. Seed dùng tiền tố `seed/banner/` (không bị xóa khi sửa — kiểm tiền tố trước khi xóa) | ☐ |
| D5 | Khóa Redis | LUU_TRU 4.3–4.4 | Cache: `banner:live:{position}` và `banner:live:CATEGORY_TOP:{categoryId}` TTL 60 giây (JSON danh sách đã sắp). Bộ đếm: `banner:{id}:imp`, `banner:{id}:clk` (`INCR`), job 5 phút đọc-và-xóa nguyên tử rồi cộng vào PostgreSQL | ☐ |
| D6 | `POST /public/banner-events` công khai dễ bị thổi số | API 5.15 | Tối đa 50 sự kiện / request, rate limit 60 request / phút / IP (`rl:bannerevt:{ip}`), bỏ qua `banner_id` không tồn tại trong cache `LIVE`; luôn trả `204` (không lộ banner nào hợp lệ) | ☐ |
| D7 | Ô chọn đích "Cửa hàng" cần `GET /admin/shops?q&limit=20` (API 11.16) — thuộc module `vendor` chưa có | handoff mục 4 | **Ngoài phạm vi plan này**: giữ cách web đang dùng (`shop.listShops` công khai) cho tới khi plan [Mở shop & KYC](./plan_mo_shop_kyc.md) có `ShopFacade` (task K10.05 thêm endpoint) | ☐ |
| D8 | `target_audience_json` | ENTITY 10.4 | TLCN không dùng: luôn `NULL`, không nhận từ API | ☐ |
| D9 | Trang chiến dịch `/campaign/{slug}`: UC bước 8 ghi cấu hình tĩnh ở frontend; API 5.18 đã thêm `GET /public/campaigns/{slug}` | UC vs API 1.4 | Giữ **tĩnh ở frontend** trong plan này (UC bước 8); `GET /public/campaigns/*` để plan khuyến mãi sau — banner chỉ cần `target_url` trỏ `/campaign/<slug>` | ☐ |

---

## 3. Cây file sẽ tạo / sửa

```
server-spring/src/main/java/com/genie/
├── common/
│   ├── error/ErrorCode.java                    (sửa) + BANNER_NOT_DELETABLE 4091 · BANNER_SLOT_FULL 4221 · BANNER_TARGET_FORBIDDEN 4222
│   ├── storage/UploadPurpose.java              (sửa) + BANNER_IMAGE
│   └── audit/AuditAction.java                  (sửa) + BANNER_CHANGED (D1)
└── admin/banner/
    ├── domain/       HomeBanner (sửa: state(), activate(), reschedule…) · BannerState (mới) · HomeBannerRepository (sửa: truy vấn)
    ├── application/  BannerAdminService · BannerQueryService · BannerTargetValidator · BannerCache · BannerEventService
    │                 BannerStatsFlushJob · BannerView / BannerCommand (record)
    └── api/          AdminBannerController · PublicBannerController · BannerRequest / BannerResponse / BannerEventsRequest (record)
server-spring/src/main/resources/db/migration/V16__banner_audit_action.sql      (mới — D1)
server-spring/src/test/java/com/genie/admin/
    HomeBannerTest · BannerTargetValidatorTest · BannerAdminIT · PublicBannerIT · BannerEventIT · BannerStatsFlushJobIT
web/src/features/promotions/infrastructure/banner.api.ts            (sửa)
web/src/features/promotions/application/…                           (sửa — gom lô sự kiện)
web/src/features/admin-marketing/infrastructure/marketing.api.ts    (sửa — nhóm banner)
web/src/shared/api/error-codes.ts · web/src/i18n/{vi,en}.json       (sửa — 3 mã GNE-BNR-*)
docs/Document/…  ENTITY (Phụ lục A, E, 10.4) · API_SPEC (11.16, 15) · Usecase.md · CHECKLIST_TIEN_DO_UC.md · Quản Lý UseCase.xlsx
docs/web-backend-handoff.md · docs/api-tests/admin-banner.http
```

---

## 4. Bảng ánh xạ Use case → API → Dữ liệu

| Bước UC | Endpoint | Bảng | Redis | Quyền |
|---|---|---|---|---|
| Tải ảnh desktop / mobile | `POST /uploads/presigned-urls` (`purpose = BANNER_IMAGE`) — API 4.1 | — (Silo `content-assets`) | — | `MANAGE_VOUCHER` |
| Danh sách, lọc vị trí, tìm | `GET /admin/banners?position&q&page&size` — API 11.16 | `home_banners`, `categories` (tên) | — | `MANAGE_VOUCHER` |
| Tạo / sửa | `POST /admin/banners` · `PUT /admin/banners/{id}` | `home_banners`, `system_logs` | xóa `banner:live:*` | `MANAGE_VOUCHER` |
| Tắt ngay / Bật lại | `PATCH /admin/banners/{id}` `{is_active}` | như trên | như trên | `MANAGE_VOUCHER` |
| Xóa (chỉ `OFF` / `ENDED`) | `DELETE /admin/banners/{id}` | như trên | như trên | `MANAGE_VOUCHER` |
| Trang chủ / danh mục đọc banner | `GET /public/banners?positions=…&category_id=…` — API 5.14 | `home_banners` (đọc) | `banner:live:{position}` 60 giây | công khai |
| Đếm hiển thị / bấm | `POST /public/banner-events` — API 5.15 | — | `banner:{id}:imp`, `banner:{id}:clk` | công khai |
| Ghi gộp số liệu, hiển thị CTR | (job 5 phút — LLD 10) | `home_banners.impression_count`, `click_count` | đọc-và-xóa bộ đếm | hệ thống |

**Mã lỗi thêm vào `ErrorCode` (đều đã có ở API 15):** `GNE-BNR-4091` (409 — không xóa được banner đang hiển thị / đã lên lịch), `GNE-BNR-4221` (422 — vị trí đủ 8 banner), `GNE-BNR-4222` (422 — liên kết ngoài miền sàn). Dùng lại: `GNE-VAL-4220`, `GNE-SEC-4030`, `GNE-SYS-4040`, `GNE-MINIO-5003`.
**Migration:** `V16__banner_audit_action.sql`. **UploadPurpose:** `BANNER_IMAGE`. **AuditAction:** `BANNER_CHANGED`.

---

## 5. Nhánh, commit, PR

- Nhánh: `feature/admin-home-banner` tách từ `develop` (có mã Jira: `feature/GNE-<số>-admin-home-banner`). Tách 3 PR: (1) B01–B03 quản trị, (2) B04–B05 công khai + bộ đếm, (3) B06–B07 nối web + tài liệu.
- Scope commit: `common`, `db`, `admin`, `web` (storefront), `admin-ui`, `docs`.
- Quy ước chung: [README mục 5](../README.md).

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — unit + IT + ArchUnit + `SchemaValidationIT` (sau `V16`).
2. Swagger UI hiện endpoint mới, summary có `UC-ADM-12`.
3. Thử tay `docs/api-tests/admin-banner.http` với `linh.tm@genie.example` (Finance Officer — có `MANAGE_VOUCHER`) và `phat.nt@genie.example` (Moderator — phải `403`).
4. Nếu đụng `web/`: `cd web && npm run verify`.
5. Tick task ở progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Bxx.yy` = task; `Bxx.Cn` = task commit.

### B00 — Chuẩn bị

- **[B00.01]** Kiểm danh tính git — `git config user.name` / `user.email`.
- **[B00.02]** Chốt quyết định D1–D9 — ghi vào đầu file progress.
- **[B00.03]** Tạo nhánh — `git switch develop && git pull && git switch -c feature/admin-home-banner`.
- **[B00.04]** Kiểm số migration — xem `db/migration` trên `develop` và [README mục 4](../README.md): nếu `V15` chưa bị chiếm khi sắp merge thì đổi tên file theo thứ tự thật; ghi số dùng vào progress.
- **[B00.05]** Đọc màn mock — mở `/admin/banners` và trang chủ (mock) ghi lại: các cột, 4 trạng thái, modal (trường, kiểm lỗi tại ô), cách 6 vị trí hiển thị (UC bước 6) để đối chiếu khi nối thật.

### B01 — Tài liệu, migration, hằng số dùng chung

- **[B01.01]** ENTITY trước (TS 7.3) — `docs/Document/…/ENTITY.md`: Phụ lục A thêm `AuditAction.BANNER_CHANGED`; mục 10.4 + Phụ lục E thêm phương thức `state(now)`, `activate()`, kiểu `BannerState`; ghi CHANGELOG. Đồng bộ `GIAI_NGHIA_ENTITY.md`, `java-code/Diagram10_*.java`.
- **[B01.02]** `V16__banner_audit_action.sql` — `DROP CONSTRAINT ck_system_logs_action_type` + `ADD CONSTRAINT` với **toàn bộ** giá trị hiện có trên `develop` (chép từ migration mới nhất đã merge) + `BANNER_CHANGED`.
- **[B01.03]** `AuditAction` thêm `BANNER_CHANGED`; nếu plan Nhật ký kiểm toán đã merge thì thêm nhánh `category()` → `CONFIG`.
- **[B01.04]** `ErrorCode` thêm 3 mã `GNE-BNR-*` (thông điệp tiếng Việt theo API 15); `ErrorCodeTest` (nếu có khẳng định số lượng) cập nhật.
- **[B01.05]** `UploadPurpose.BANNER_IMAGE` — bucket `content-assets`, `image/jpeg, image/png, image/webp`, 5 MB, công khai, không mã hóa; `UploadController` chỉ cấp purpose này cho người có `MANAGE_VOUCHER` (thêm kiểm quyền theo purpose nếu `UploadService` chưa có cơ chế — viết gọn dạng `purpose.requiredAuthority()`).
- **[B01.06]** Test — `SchemaValidationIT` xanh; `PresignedUploadIT` thêm ca `BANNER_IMAGE`: Finance Officer xin được, khách hàng `403`, sai MIME / quá 5 MB `422`.
- **[B01.C1]** COMMIT — `feat(db): allow banner change action in system logs`
- **[B01.C2]** COMMIT — `feat(common): add banner error codes and banner image upload purpose`

### B02 — Miền & truy vấn

- **[B02.01]** `BannerState` — enum `LIVE, SCHEDULED, ENDED, OFF`.
- **[B02.02]** `HomeBanner` — thêm `state(now)` (D3), `activate()` (bật lại), `changeSchedule(startsAt, endsAt)` (ném `IllegalArgumentException` khi `endsAt <= startsAt`), `changeContent(title, targetUrl, position, categoryId, sortOrder)` (`CATEGORY_TOP` bắt buộc `categoryId`, vị trí khác xóa `categoryId`), `changeImages(desktopKey, mobileKey)`, `isDeletable(now)` (chỉ `OFF` / `ENDED`); factory `HomeBanner.create(…)`.
- **[B02.03]** `HomeBannerTest` — bảng ca cho `state` (4 trạng thái, biên thời gian), `isLive`, `isDeletable`, `CATEGORY_TOP` thiếu danh mục, lịch sai, `recordStats` cộng dồn, `ctr()` khi 0 lượt hiển thị.
- **[B02.04]** `HomeBannerRepository` — `findLive(position, categoryId, now)` sắp `sort_order, id`; `countOverlapping(position, categoryId, startsAt, endsAt, now, excludeId)` (D2); tìm kiếm Admin `search(position, q, pageable)` (`q` theo `title` không phân biệt hoa thường); `addStats(id, imp, clk)` (`@Modifying UPDATE … SET impression_count = impression_count + :imp`).
- **[B02.05]** `BannerTargetValidator` (N4) — hàm thuần: nhận chuỗi, trả hợp lệ / không; quy tắc: không rỗng, ≤ độ dài cột, bắt đầu bằng đúng một `/`, không chứa `\`, không có `//` ở đầu, không có ký tự điều khiển / khoảng trắng đầu cuối, không chứa `://`, không bắt đầu `/api/`.
- **[B02.06]** `BannerTargetValidatorTest` — ca hợp lệ (`/c/dien-tu`, `/p/tai-nghe-1`, `/shop/15`, `/flash-sale`, `/discover`, `/campaign/sieu-sale-12-12`) và ca bị chặn (`//other.com`, `/\\other.com`, `javascript:alert(1)`, `https://x.com`, `/%2F%2Fevil.com` sau khi giải mã, chuỗi rỗng).
- **[B02.C1]** COMMIT — `feat(admin): add banner state rules and target url validation`

### B03 — API quản trị banner

- **[B03.01]** `BannerRequest` (record + Bean Validation) — `title` (bắt buộc, ≤ độ dài cột), `image_object_key` (bắt buộc khi tạo; bỏ khi sửa = giữ ảnh), `mobile_image_object_key` (tùy chọn; chuỗi rỗng = xóa ảnh mobile), `target_url`, `position`, `category_id`, `sort_order` (≥ 0), `starts_at`, `ends_at`, `is_active`. Thiếu ảnh / thời gian / `category_id` khi `CATEGORY_TOP` → `422 GNE-VAL-4220` có `errors[]` đúng tên trường (giao diện báo lỗi tại ô).
- **[B03.02]** `BannerAdminService.create` — kiểm `target_url` (N4 → `4222`), kiểm danh mục tồn tại khi `CATEGORY_TOP` (qua `CategoryRepository` cùng… — **khác module**: dùng cổng `BannerCategoryLookup` khai trong `admin.banner.application`, module `catalog` cài sau; chưa có cài đặt thì chỉ kiểm `category_id` khác null), kiểm đủ 8 (D2 → `4221`), lưu lấy `id`, `promote` ảnh sang khóa chính thức (D4), ghi `AuditLogger BANNER_CHANGED {op: CREATE}`, phát sự kiện xóa cache `AFTER_COMMIT`.
- **[B03.03]** `update` — như tạo; đổi ảnh thì `promote` ảnh mới + xóa ảnh cũ `AFTER_COMMIT` (không xóa khóa có tiền tố `seed/`); không gửi `image_object_key` thì giữ ảnh; ghi `details` giá trị trước / sau của các trường đổi.
- **[B03.04]** `setActive(id, active)` — tắt: `deactivate()`; bật lại: `activate()` + kiểm đủ 8 (D2); ghi audit `DEACTIVATE` / `ACTIVATE`.
- **[B03.05]** `delete(id)` — chỉ khi `isDeletable(now)`, ngược lại `409 GNE-BNR-4091`; xóa dòng + xóa ảnh `AFTER_COMMIT`; audit `DELETE`. Không tìm thấy → `404 GNE-SYS-4040`.
- **[B03.06]** `BannerQueryService.search` — trả `BannerView`: mọi trường API 11.16 gồm `image_url` / `mobile_image_url` (ghép từ khóa đối tượng), `category` (`{id, name}` hoặc `null` — tên qua cổng `BannerCategoryLookup`, không có thì chỉ `id`), `state` (D3), `impressions`, `clicks`; phân trang `page` / `size` theo API 0.4.
- **[B03.07]** `AdminBannerController` — 5 endpoint API 11.16, `@PreAuthorize("hasAuthority('MANAGE_VOUCHER')")`, `@Operation(summary = "UC-ADM-12 …")`, `POST` trả `201`, `PATCH` trả `200` kèm banner, `DELETE` trả `204`.
- **[B03.08]** `BannerAdminIT` (PG + Redis + Silo) — tạo đủ trường; thiếu ảnh / lịch sai / `CATEGORY_TOP` thiếu danh mục → `422` đúng `errors[].field`; `target_url` xấu → `4222`; banner thứ 9 cùng vị trí giao lịch → `4221`, khác lịch thì được; sửa không đổi ảnh giữ nguyên khóa; đổi ảnh thì ảnh cũ bị xóa khỏi Silo; tắt → `OFF`, bật lại; xóa `LIVE` / `SCHEDULED` → `4091`, xóa `OFF` / `ENDED` → `204`; mỗi thao tác sinh 1 dòng `BANNER_CHANGED`.
- **[B03.09]** `BannerAdminIT` phân quyền — Admin / Finance Officer `200`; Moderator, CS, khách hàng `403`; không token `401`.
- **[B03.C1]** COMMIT — `feat(admin): manage home banners with schedule, slot limit and audit log`

### B04 — API công khai & cache

- **[B04.01]** `BannerCache` — đọc / ghi / xóa `banner:live:{position}[:{categoryId}]` TTL 60 giây (JSON); Redis lỗi → bỏ qua cache, đọc PostgreSQL (N6); `evictAll()` xóa theo danh sách khóa cố định (6 vị trí) + mẫu `CATEGORY_TOP:*` bằng `SCAN` (không `KEYS`).
- **[B04.02]** `BannerQueryService.live(positions, categoryId, now)` — theo từng vị trí: cache-aside (LUU_TRU 4.5); trả `Map<position, List<…>>` gồm cả vị trí không có banner (mảng rỗng); mỗi phần tử: `id`, `title`, `image_url`, `mobile_image_url`, `target_url`.
- **[B04.03]** `PublicBannerController` — `GET /api/v1/public/banners?positions=…&category_id=…`: `positions` bắt buộc (danh sách enum, sai → `422`), `category_id` bắt buộc khi có `CATEGORY_TOP`; header `Cache-Control: public, max-age=60` (API 0.12); `@Operation(summary = "UC-ADM-12 — Banner đang hiển thị theo vị trí")`.
- **[B04.04]** Listener xóa cache — `@TransactionalEventListener(AFTER_COMMIT)` nhận `BannerChangedEvent` (record trong `admin/banner/event`) → `BannerCache.evictAll()` (N7).
- **[B04.05]** `PublicBannerIT` — chỉ trả banner `LIVE` đúng thứ tự `sort_order`; banner hẹn giờ / hết hạn / tắt không xuất hiện; `CATEGORY_TOP` lọc theo danh mục; lần gọi 2 lấy từ cache (đếm truy vấn hoặc sửa thẳng DB rồi xác nhận dữ liệu cũ trong 60 giây); sau khi Admin tắt banner thì cache bị xóa và banner biến mất ngay; Redis tắt (dừng container trong test) vẫn trả đúng.
- **[B04.C1]** COMMIT — `feat(admin): serve live banners by position with redis cache`

### B05 — Bộ đếm hiển thị / bấm

- **[B05.01]** `BannerEventsRequest` — `events[]` 1–50 phần tử `{banner_id, type}` với `type` ∈ `IMPRESSION, CLICK` (D6).
- **[B05.02]** `BannerEventService.record(events, clientIp)` — rate limit `rl:bannerevt:{ip}` 60 / phút; chỉ nhận `banner_id` đang có trong tập banner `LIVE` (lấy từ cache / DB); `INCR banner:{id}:imp` / `:clk` bằng pipeline; Redis lỗi → bỏ qua, vẫn `204`.
- **[B05.03]** `POST /api/v1/public/banner-events` → `204`; không yêu cầu CSRF / đăng nhập; chấp nhận `Content-Type: text/plain` của `navigator.sendBeacon` (đọc thân như JSON).
- **[B05.04]** `BannerStatsFlushJob` — `@Scheduled(fixedDelay = 5 phút)`: với từng banner có khóa đếm, đọc-và-xóa nguyên tử (`GETDEL`), gọi `HomeBannerRepository.addStats` trong transaction; ghi DB lỗi thì cộng trả lại Redis (`INCRBY`) để không mất số; ghi log số banner đã ghi gộp (LLD 10).
- **[B05.05]** `BannerEventIT` + `BannerStatsFlushJobIT` — gửi 3 hiển thị + 1 bấm → sau khi chạy job `impression_count = 3`, `click_count = 1`, khóa Redis đã xóa; chạy job lần 2 không cộng thêm; `banner_id` lạ bị bỏ qua; quá 50 sự kiện `422`; quá tần suất `429`.
- **[B05.C1]** COMMIT — `feat(admin): count banner impressions and clicks through redis`

### B06 — Nối giao diện `web/`

- **[B06.01]** `web/src/shared/api/error-codes.ts` + `vi.json` / `en.json` — thêm `GNE-BNR-4091`, `GNE-BNR-4221`, `GNE-BNR-4222` (nếu chưa có) — `node ../scripts/check-i18n.mjs` xanh.
- **[B06.02]** `promotions/infrastructure/banner.api.ts` — `listLiveBanners`: `isMocked('banner')`; đối chiếu DTO (map theo vị trí) + mapper; SSR gọi qua `server-http.ts` với `INTERNAL_API_URL`.
- **[B06.03]** `trackBanner` — đổi sang `POST /public/banner-events` (sửa endpoint lệch spec — handoff mục 2): gom sự kiện vào hàng đợi, gửi mỗi 5 giây và khi rời trang bằng `navigator.sendBeacon`; mỗi banner chỉ đếm 1 hiển thị / lần xem trang.
- **[B06.04]** `admin-marketing/infrastructure/marketing.api.ts` (nhóm banner) — `listBanners`, `saveBanner`, `setBannerActive`, `deleteBanner` theo `isMocked('banner')`; tải ảnh qua `shared/api/upload.ts` với `purpose = BANNER_IMAGE` (cờ `upload`); ánh xạ `errors[]` của `422` về đúng ô trong modal.
- **[B06.05]** Chạy thật hai phía — `PUBLIC_REAL_API=auth,upload,banner`: Finance Officer tạo banner (ảnh thật lên Silo) → trang chủ hiện trong ≤ 60 giây; "Tắt ngay" → biến mất ngay; kiểm 6 vị trí hiển thị đúng UC bước 6; bấm banner → sau ≤ 5 phút màn quản lý tăng lượt bấm, CTR đúng; nút "Xem" mở đúng trang.
- **[B06.06]** E2E — thêm kịch bản vào `web/tests/e2e/admin.spec.ts` (chạy mock) cho luồng tạo / tắt / xóa banner nếu chưa có; `npm run verify`.
- **[B06.C1]** COMMIT — `fix(web): send banner events in batches to the spec endpoint`
- **[B06.C2]** COMMIT — `feat(admin-ui): connect banner management to the real api`

### B07 — Hoàn thiện use case

- **[B07.01]** `docs/api-tests/admin-banner.http` — kịch bản đúng (tạo → xem công khai → sự kiện → tắt → xóa) + lỗi (`4221`, `4222`, `4091`, `403`).
- **[B07.02]** OpenAPI — kiểm `/v3/api-docs`; cập nhật bản xuất nếu repo đang giữ.
- **[B07.03]** Tài liệu nguồn — Usecase.md UC-ADM-12: dòng *Hiện trạng*; API 11.16 ghi quy tắc đếm 8 banner (D2), 5.15 ghi giới hạn 50 sự kiện (D6); ENTITY đã sửa ở B01.01; ghi lịch sử phiên bản.
- **[B07.04]** Tiến độ — `CHECKLIST_TIEN_DO_UC.md` (tick 5 lớp, Nhật ký), `Quản Lý UseCase.xlsx` → "Đã triển khai".
- **[B07.05]** `docs/web-backend-handoff.md` — đánh dấu nhóm banner đã nối, bỏ dòng `POST /public/banners/{id}/events` khỏi bảng lệch spec; `CLAUDE.md` mục 2.
- **[B07.C1]** COMMIT — `docs(admin): record home banner use case as implemented`
- **[B07.06]** Mở PR vào `develop` — mẫu PR ghi rõ migration `V16` (tương thích ngược: chỉ nới `CHECK`).

---

## 8. Ngoài phạm vi

- `GET /admin/shops` cho ô chọn shop đích (D7) và `GET /public/campaigns/*` (D9).
- Nhắm đối tượng (`target_audience_json`), A/B banner, thống kê theo ngày — DATN.
- Thông báo toàn sàn (UC-NOT-05) dùng chung bucket `content-assets` — plan riêng.

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Plan khác cũng sửa `ck_system_logs_action_type` (vd `V15` của Mở shop & KYC) | Sổ đăng ký migration ở README; nhánh merge sau chép đủ giá trị của nhánh trước (B00.04, B01.02) |
| Ảnh đã `promote` nhưng transaction rollback → tệp mồ côi | `promote` là bước cuối trong transaction; lỗi sau đó thì xóa tệp trong `catch`; tệp `tmp/` tự hết hạn 1 ngày |
| Bộ đếm bị thổi bằng script | Giới hạn 50 sự kiện + rate limit theo IP (D6); số liệu chỉ để tham khảo CTR |
| Job ghi gộp chạy trên nhiều instance | `GETDEL` nguyên tử nên không cộng trùng; TLCN một instance |
| Cache 60 giây làm "Tắt ngay" chậm | Xóa cache `AFTER_COMMIT` (B04.04); web SSR không cache thêm ở tầng Astro |
