# PROGRESS — Chuỗi C7 Đánh giá (UC-SOC-03 · UC-SOC-04 · UC-SOC-05 · UC-AI-07 phía backend)

| | |
|---|---|
| **Plan** | [plan_danh_gia_c7.md](./plan_danh_gia_c7.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-28 |
| **Use case** | Đánh giá sản phẩm đã mua & Tải ảnh thực tế (UC-SOC-03) · Người bán phản hồi đánh giá (UC-SOC-04) · Báo cáo đánh giá vi phạm / spam & Kiểm duyệt đánh giá (UC-SOC-05) · Phân tích cảm xúc đánh giá bằng PhoBERT (UC-AI-07 — phần backend; phần AI Worker ở [plan_phan_tich_cam_xuc.md](../server-python/plan_phan_tich_cam_xuc.md)) |
| **Tổng** | 45 task + 11 commit, 10 giai đoạn |
| **Nhánh** | `feature/c7-reviews` (R00–R04) → `feature/c7-review-moderation` (R05) → `feature/c7-sentiment-backend` (R06–R09) |
| **Phụ trách** | _(điền kết quả `git config user.name` ở task R00.01 — tên này dùng khi tick cột Phụ trách ở checklist / báo cáo / Excel)_ |
| **Người commit** | danh tính git đã cấu hình trên máy người làm — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Cập nhật gần nhất** | 2026-11-28 — lập plan, chưa bắt đầu (0 / 56) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục *Ghi chú*.
- Task `COMMIT` chỉ tick sau khi qua *Cổng kiểm tra* (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`. Thông điệp đã ghi sẵn — tiếng Anh, chữ thường, đúng `commitlint.config.cjs`; có mã Jira thì thêm ` [#GNE-<số>]` cuối.
- Sau mỗi buổi làm: cập nhật cột *Xong* của bảng Tổng quan, dòng *Cập nhật gần nhất*, một dòng *Nhật ký* cuối trang.
- Cột *Chạy* của use case chỉ tick khi đã chạy thật trên giao diện (Chromium điều khiển bằng Playwright, hệ thống thật) và đạt hết ca của kịch bản — ghi cách chạy + số ca vào *Ghi chú*.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | **Đầu vào đánh giá** (API 7.9) | ☐ | |
| D2 | **Điều kiện được đánh giá** | ☐ | |
| D3 | **Thời hạn đánh giá** | ☐ | |
| D4 | **Ảnh / video** | ☐ | |
| D5 | **Ẩn tên** | ☐ | |
| D6 | **Điểm sản phẩm & shop** | ☐ | |
| D7 | **Thưởng 50 Điểm** | ☐ | |
| D8 | **Đánh dấu đơn đã đánh giá** | ☐ | |
| D9 | **Trạng thái báo cáo** | ☐ | |
| D10 | **Báo cáo** (API 7.10) | ☐ | |
| D11 | **Phản hồi của shop** (API 10.8) | ☐ | |
| D12 | **Danh sách** | ☐ | |
| D13 | **Hữu ích** (API 7.10) | ☐ | |
| D14 | **Hàng chờ kiểm duyệt** (API 11.5) | ☐ | |
| D15 | **Hợp đồng AI Worker** | ☐ | |
| D16 | **Thử lại & quét lại** | ☐ | |
| D17 | **Thống kê cảm xúc của shop** (API 10.9 `sentiment`) | ☐ | |
| D18 | **Chạy lại khi đổi mô hình** | ☐ | |
| D19 | **Cờ web** | ☐ | |
| D20 | **Ngoài phạm vi** | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| R00 | Chuẩn bị | 6 | 0 | 0 / 6 |
| R01 | Tài liệu trước | 3 | 1 | 0 / 4 |
| R02 | Migration & cổng | 4 | 2 | 0 / 6 |
| R03 | Đánh giá đã mua (UC-SOC-03) | 4 | 1 | 0 / 5 |
| R04 | Phản hồi của shop (UC-SOC-04) | 3 | 1 | 0 / 4 |
| R05 | Báo cáo & kiểm duyệt (UC-SOC-05) — nhánh `feature/c7-review-moderation` | 3 | 1 | 0 / 4 |
| R06 | Cảm xúc đánh giá phía backend (UC-AI-07) — nhánh `feature/c7-sentiment-backend` | 5 | 2 | 0 / 7 |
| R07 | Nối web (cờ `reviews`, `seller-reviews`, `review-moderation`, `sentiment`) | 5 | 1 | 0 / 6 |
| R08 | Kiểm thử & chạy thật | 4 | 1 | 0 / 5 |
| R09 | Hoàn thiện use case & tick tiến độ | 8 | 1 | 0 / 9 |
| **Tổng** | | **45** | **11** | **0 / 56** |

## R00 — Chuẩn bị

- [ ] **R00.01** Chạy `git config user.name` / `user.email`
- [ ] **R00.02** Chốt D1–D20 vào progress.
- [ ] **R00.03** Tạo nhánh `feature/c7-reviews` từ `develop`.
- [ ] **R00.04** Đọc `Product` (`rating`, `review_count`?), `VendorProfile.applyRating`, `CoinLedgerService.credit`, `UploadService.promoteTo`, `InternalTokenFilter` (đường dẫn nội bộ đang cho phép), CHECK `ck_notifications_type`, `ck_system_logs_action_type` mới nhất.
- [ ] **R00.05** Chốt hợp đồng HTTP với plan [6] (Z00.03): request / callback / token / mã lỗi / thời gian chờ
- [ ] **R00.06** Đọc màn mock: modal đánh giá (sao, chữ, ảnh / video, ẩn tên), danh sách đánh giá trang sản phẩm (lọc, Hữu ích, Báo cáo), Seller › Đánh giá (3 lọc, phản hồi), Admin › Kiểm duyệt đánh giá (hàng chờ, nhãn AI), khối Cảm xúc ở Seller › Phân tích.

## R01 — Tài liệu trước

- [ ] **R01.01** ENTITY 9.1 (ẩn tên D5, không sửa / xóa D3, Điểm thưởng không thu hồi D7), 9.3 (D9), 10.1 (`REVIEW_REPLIED`), Phụ lục A (`REVIEW_RESTORED`, `ReviewReportStatus`), Phụ lục E; LUU_TRU bucket `review-media`.
- [ ] **R01.02** API_SPEC
- [ ] **R01.03** Usecase.md
- [ ] **R01.C1** 🔖 COMMIT `docs(social): specify verified reviews, seller replies, review moderation and sentiment callbacks` · hash: `…`

## R02 — Migration & cổng

- [ ] **R02.01** `V31__c7_review_moderation.sql` (D9, D11 `REVIEW_REPLIED`, `REVIEW_RESTORED`)
- [ ] **R02.02** Cổng trong social + bản mặc định (`ReviewPorts`) + 4 bản cài ở order / catalog / vendor / promotion (D2, D6, D7, D8).
- [ ] **R02.03** `UploadPurpose.REVIEW_IMAGE / REVIEW_VIDEO` (D4), `ErrorCode`, `AuditAction`, `NotificationType`.
- [ ] **R02.04** Test
- [ ] **R02.C1** 🔖 COMMIT `feat(db): add review report states and review indexes` · hash: `…`
- [ ] **R02.C2** 🔖 COMMIT `feat(social): add review ports implemented by order, catalog, vendor and promotion` · hash: `…`

## R03 — Đánh giá đã mua (UC-SOC-03)

- [ ] **R03.01** `ReviewService.create` (D1, D2, D6–D8) + `ReviewNames` (D5).
- [ ] **R03.02** `ReviewQueryService`
- [ ] **R03.03** `HelpfulVoteService` + `ReviewInteractionController` (`PUT / DELETE /reviews/{id}/helpful-vote`) (D13).
- [ ] **R03.04** Test
- [ ] **R03.C1** 🔖 COMMIT `feat(social): let verified buyers review purchased items with photos and helpful votes` · hash: `…`

## R04 — Phản hồi của shop (UC-SOC-04)

- [ ] **R04.01** `PhoneNumberGuard` (D11) + `SellerReviewService` + `SellerReviewController` (`GET /seller/reviews`, `PUT /seller/reviews/{id}/reply`).
- [ ] **R04.02** Thông báo `REVIEW_REPLIED` (AFTER_COMMIT)
- [ ] **R04.03** Test
- [ ] **R04.C1** 🔖 COMMIT `feat(social): let sellers reply once to reviews with a phone number guard` · hash: `…`

## R05 — Báo cáo & kiểm duyệt (UC-SOC-05) — nhánh `feature/c7-review-moderation`

- [ ] **R05.01** `ReviewReportService` + `POST /reviews/{id}/reports` (D10).
- [ ] **R05.02** `ReviewModerationService` + `AdminReviewModerationController` (D14)
- [ ] **R05.03** Test
- [ ] **R05.C1** 🔖 COMMIT `feat(social): add review reports and a prioritized moderation queue` · hash: `…`

## R06 — Cảm xúc đánh giá phía backend (UC-AI-07) — nhánh `feature/c7-sentiment-backend`

- [ ] **R06.01** `SentimentDispatcher` (AFTER_COMMIT + `@Async`, bỏ qua đánh giá chỉ có sao) + `SentimentClient` (`HttpSentimentClient` / `StubSentimentClient`, D15) + thử lại (D16).
- [ ] **R06.02** `InternalSentimentController` `PATCH /internal/reviews/{id}/sentiment` + `SentimentResultService` (`attachSentiment`, nhãn hợp lệ, `score` 0–1, xóa cache).
- [ ] **R06.03** `SentimentRescanJob` (D16).
- [ ] **R06.04** `SellerSentimentStats` + `SellerSentimentController` `GET /seller/analytics/sentiment` (D17) + cache.
- [ ] **R06.05** Test
- [ ] **R06.C1** 🔖 COMMIT `feat(social): send reviews to the sentiment worker and store callback results` · hash: `…`
- [ ] **R06.C2** 🔖 COMMIT `feat(social): serve seller sentiment statistics with a rescan job` · hash: `…`

## R07 — Nối web (cờ `reviews`, `seller-reviews`, `review-moderation`, `sentiment`)

- [ ] **R07.01** `reviews/review.api.ts` (4 hàm)
- [ ] **R07.02** `seller-reviews/review.api.ts`
- [ ] **R07.03** `admin-moderation/review.api.ts`
- [ ] **R07.04** `seller-analytics` `getSentiment`
- [ ] **R07.05** vi / en + `error-codes.ts`; test mapper; kiểm 9 theme, 360 px; `npm run verify`.
- [ ] **R07.C1** 🔖 COMMIT `feat(web): connect reviews, seller replies, review moderation and sentiment stats to the real api` · hash: `…`

## R08 — Kiểm thử & chạy thật

- [ ] **R08.01** Seed
- [ ] **R08.02** `docs/api-tests/reviews.http`
- [ ] **R08.03** `docs/kiem-thu/kich-ban-2026-11-28-c7.md`
- [ ] **R08.04** Chạy thật
- [ ] **R08.C1** 🔖 COMMIT `docs(social): add review api tests and real-run scenarios` · hash: `…`

## R09 — Hoàn thiện use case & tick tiến độ

- [ ] **R09.01** OpenAPI
- [ ] **R09.02** `docs/web-backend-handoff.md`
- [ ] **R09.03** Usecase.md
- [ ] **R09.04** `CHECKLIST_TIEN_DO_UC.md`
- [ ] **R09.05** `BAO_CAO_UC_DOC_LAP.md`
- [ ] **R09.06** `Quản Lý UseCase.xlsx`
- [ ] **R09.07** README ngày (mục 7)
- [ ] **R09.C1** 🔖 COMMIT `docs(social): record reviews, replies, moderation and sentiment as implemented` · hash: `…`
- [ ] **R09.08** Mở 3 PR xếp chồng vào `develop` (`c7-reviews` → `c7-review-moderation` → `c7-sentiment-backend`).

## Ghi chú

| Task | Ghi chú |
|---|---|
| | |

## Nhật ký

| Ngày | Việc đã làm | Commit / PR |
|---|---|---|
| 2026-11-28 | Lập plan + progress (45 task, 11 commit, 20 quyết định chờ chốt) | nhánh `feature/plan-c3-c11` |
