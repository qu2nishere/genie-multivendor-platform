# PROGRESS — Quản lý banner trang chủ & chiến dịch tĩnh (UC-ADM-12)

| | |
|---|---|
| **Plan** | [plan_banner_trang_chu.md](./plan_banner_trang_chu.md) — chi tiết từng task tra theo mã `Bxx.yy` |
| **Ngày lập** | 2026-11-16 |
| **Tổng** | 48 task + 9 commit, 8 giai đoạn |
| **Nhánh** | `feature/admin-home-banner` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-17 — đang làm (7 / 57) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục Ghi chú.
- Task `COMMIT` chỉ tick sau khi qua Cổng kiểm tra (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`.
- Thông điệp commit ghi sẵn bên dưới, thêm ` [#GNE-<số>]` vào cuối khi đã có mã Jira.
- Cập nhật cột "Xong" của bảng Tổng quan và dòng "Cập nhật gần nhất" sau mỗi buổi làm; ghi một dòng vào Nhật ký cuối trang.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | Hành động kiểm toán cho banner chưa có trong `AuditAction` | ☑ | Như đề xuất — `BANNER_CHANGED` (nhóm `CONFIG`), `details.op`; migration **`V15__banner_audit_action.sql`** (B00.04) · 2026-11-17 |
| D2 | "Tối đa 8 banner đang hiển thị mỗi vị trí" đếm thế nào với banner hẹn giờ | ☑ | Như đề xuất — đếm banner bật khác cùng vị trí (+ danh mục với `CATEGORY_TOP`) có lịch giao nhau và chưa kết thúc; ≥ 8 → `4221`; bật lại cũng kiểm · 2026-11-17 |
| D3 | `state` (API: `LIVE, SCHEDULED, ENDED, OFF`) | ☑ | Như đề xuất — `HomeBanner.state(now)` + enum `BannerState` · 2026-11-17 |
| D4 | Khóa đối tượng ảnh | ☑ | Như đề xuất — `banner/{id}/{desktop|mobile}-{uuid}.{ext}` qua `UploadService.promoteTo`; không xóa khóa `seed/` · 2026-11-17 |
| D5 | Khóa Redis | ☑ | Như đề xuất — `banner:live:{position}[:{categoryId}]` TTL 60 s; `banner:{id}:imp|clk`, job 5 phút `GETDEL` · 2026-11-17 |
| D6 | `POST /public/banner-events` công khai dễ bị thổi số | ☑ | Như đề xuất — ≤ 50 sự kiện, 60 request / phút / IP, bỏ qua id không `LIVE`, luôn `204` · 2026-11-17 |
| D7 | Ô chọn đích "Cửa hàng" cần `GET /admin/shops?q&limit=20` (API 11.16) — thuộc module `vendor` chưa có | ☑ | Như đề xuất — ngoài phạm vi, web giữ `shop.listShops` · 2026-11-17 |
| D8 | `target_audience_json` | ☑ | Như đề xuất — luôn `NULL` · 2026-11-17 |
| D9 | Trang chiến dịch `/campaign/{slug}`: UC bước 8 ghi cấu hình tĩnh ở frontend; API 5.18 đã thêm `GET /public/campaigns/{slug}` | ☑ | Như đề xuất — trang chiến dịch tĩnh ở frontend · 2026-11-17 |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong | Mã Jira | Nhánh |
|---|---|---:|---:|---:|---|---|
| B00 | Chuẩn bị | 5 | 0 | 4 / 5 | — | `feature/admin-home-banner` |
| B01 | Tài liệu, migration, hằng số dùng chung | 6 | 2 | 3 / 8 | — | `feature/admin-home-banner` |
| B02 | Miền & truy vấn | 6 | 1 | 0 / 7 | — | `feature/admin-home-banner` |
| B03 | API quản trị banner | 9 | 1 | 0 / 10 | — | `feature/admin-home-banner` |
| B04 | API công khai & cache | 5 | 1 | 0 / 6 | — | `feature/admin-home-banner` |
| B05 | Bộ đếm hiển thị / bấm | 5 | 1 | 0 / 6 | — | `feature/admin-home-banner` |
| B06 | Nối giao diện `web/` | 6 | 2 | 0 / 8 | — | `feature/admin-home-banner` |
| B07 | Hoàn thiện use case | 6 | 1 | 0 / 7 | — | `feature/admin-home-banner` |
| **Tổng** | | **48** | **9** | **7 / 57** | | |

## B00 — Chuẩn bị

- [x] **B00.01** Kiểm danh tính git
- [x] **B00.02** Chốt quyết định D1–D9
- [x] **B00.03** Tạo nhánh
- [x] **B00.04** Kiểm số migration
- [ ] **B00.05** Đọc màn mock

## B01 — Tài liệu, migration, hằng số dùng chung

- [x] **B01.01** ENTITY trước (TS 7.3)
- [x] **B01.02** `V16__banner_audit_action.sql`
- [x] **B01.03** `AuditAction` thêm `BANNER_CHANGED`; nếu plan Nhật ký kiểm toán đã merge thì thêm nhánh `category()` → `CONFIG`.
- [ ] **B01.04** `ErrorCode` thêm 3 mã `GNE-BNR-*` (thông điệp tiếng Việt theo API 15); `ErrorCodeTest` (nếu có khẳng định số lượng) cập nhật.
- [ ] **B01.05** `UploadPurpose.BANNER_IMAGE`
- [ ] **B01.06** Test
- [ ] **B01.C1** 🔖 COMMIT `feat(db): allow banner change action in system logs` · hash: `…`
- [ ] **B01.C2** 🔖 COMMIT `feat(common): add banner error codes and banner image upload purpose` · hash: `…`

## B02 — Miền & truy vấn

- [ ] **B02.01** `BannerState`
- [ ] **B02.02** `HomeBanner`
- [ ] **B02.03** `HomeBannerTest`
- [ ] **B02.04** `HomeBannerRepository`
- [ ] **B02.05** `BannerTargetValidator` (N4)
- [ ] **B02.06** `BannerTargetValidatorTest`
- [ ] **B02.C1** 🔖 COMMIT `feat(admin): add banner state rules and target url validation` · hash: `…`

## B03 — API quản trị banner

- [ ] **B03.01** `BannerRequest` (record + Bean Validation)
- [ ] **B03.02** `BannerAdminService.create`
- [ ] **B03.03** `update`
- [ ] **B03.04** `setActive(id, active)`
- [ ] **B03.05** `delete(id)`
- [ ] **B03.06** `BannerQueryService.search`
- [ ] **B03.07** `AdminBannerController`
- [ ] **B03.08** `BannerAdminIT` (PG + Redis + Silo)
- [ ] **B03.09** `BannerAdminIT` phân quyền
- [ ] **B03.C1** 🔖 COMMIT `feat(admin): manage home banners with schedule, slot limit and audit log` · hash: `…`

## B04 — API công khai & cache

- [ ] **B04.01** `BannerCache`
- [ ] **B04.02** `BannerQueryService.live(positions, categoryId, now)`
- [ ] **B04.03** `PublicBannerController`
- [ ] **B04.04** Listener xóa cache
- [ ] **B04.05** `PublicBannerIT`
- [ ] **B04.C1** 🔖 COMMIT `feat(admin): serve live banners by position with redis cache` · hash: `…`

## B05 — Bộ đếm hiển thị / bấm

- [ ] **B05.01** `BannerEventsRequest`
- [ ] **B05.02** `BannerEventService.record(events, clientIp)`
- [ ] **B05.03** `POST /api/v1/public/banner-events` → `204`; không yêu cầu CSRF / đăng nhập; chấp nhận `Content-Type: text/plain` của `navigator.sendBeacon` (đọc thân như JSON).
- [ ] **B05.04** `BannerStatsFlushJob`
- [ ] **B05.05** `BannerEventIT` + `BannerStatsFlushJobIT`
- [ ] **B05.C1** 🔖 COMMIT `feat(admin): count banner impressions and clicks through redis` · hash: `…`

## B06 — Nối giao diện `web/`

- [ ] **B06.01** `web/src/shared/api/error-codes.ts` + `vi.json` / `en.json`
- [ ] **B06.02** `promotions/infrastructure/banner.api.ts`
- [ ] **B06.03** `trackBanner`
- [ ] **B06.04** `admin-marketing/infrastructure/marketing.api.ts` (nhóm banner)
- [ ] **B06.05** Chạy thật hai phía
- [ ] **B06.06** E2E
- [ ] **B06.C1** 🔖 COMMIT `fix(web): send banner events in batches to the spec endpoint` · hash: `…`
- [ ] **B06.C2** 🔖 COMMIT `feat(admin-ui): connect banner management to the real api` · hash: `…`

## B07 — Hoàn thiện use case

- [ ] **B07.01** `docs/api-tests/admin-banner.http`
- [ ] **B07.02** OpenAPI
- [ ] **B07.03** Tài liệu nguồn
- [ ] **B07.04** Tiến độ
- [ ] **B07.05** `docs/web-backend-handoff.md`
- [ ] **B07.C1** 🔖 COMMIT `docs(admin): record home banner use case as implemented` · hash: `…`
- [ ] **B07.06** Mở PR vào `develop`

---

## Chốt plan — tick tiến độ use case

Làm **ngay khi xong plan** (hoặc xong từng use case), trong file [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../../docs/Document/CHECKLIST_TIEN_DO_UC.md) — cách tick xem mục 0 của file đó.

- [ ] `UC-ADM-12` Quản lý banner trang chủ & chiến dịch tĩnh — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [ ] Cập nhật số ở mục 1 *Tổng quan* của checklist (Đã triển khai / Đang triển khai / Chưa có backend)
- [ ] Thêm một dòng vào mục 5 *Nhật ký cập nhật* của checklist (ngày · use case · mã commit / PR)
- [ ] Đổi cột *Hiện trạng mã nguồn* của các use case trên trong `Quản Lý UseCase.xlsx`
- [ ] Tick dòng plan này ở bảng *Tiến độ các plan* trong [README](../README.md) mục 6

## Ghi chú

Ghi ở đây: lý do bỏ qua task (`[-]`), chỗ làm khác plan, số đo, việc tồn chuyển sang plan khác.

| Task | Ghi chú |
|---|---|

## Nhật ký
| B00.03 | Nhánh tách từ `feature/admin-audit-log` (không phải `develop`) để dùng lại `ApiListResponse`, `InvalidRequestException`, `requestBlob` và `AuditAction.category()` của plan [2] — tránh viết trùng gây conflict. PR banner mở sau khi PR nhật ký kiểm toán đã merge (rebase lên `develop` khi đó) |
| B00.04 | `develop` tới `V14`; plan [3] merge trước plan [7] → dùng **`V15__banner_audit_action.sql`**, KYC dời sang `V16` (đúng ghi chú README mục 4 — sửa bảng sổ đăng ký trong cùng PR) |
| B01.01 | ENTITY 10.4 (phương thức `create / state / activate / changeSchedule / changeContent / changeImages / isDeletable`, ảnh lưu khóa đối tượng), Phụ lục A (`BANNER_CHANGED`, `BannerState` không lưu), Phụ lục E; `java-code/Diagram10_*.java`, `GIAI_NGHIA_ENTITY.md`; API 16.2 thêm `BANNER_CHANGED` vào nhóm `CONFIG` |

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-16 | Lập plan + progress (48 task, 9 commit, 8 giai đoạn) — chưa bắt đầu làm | — |
