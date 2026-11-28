# PLAN — Chuỗi C7 Đánh giá (UC-SOC-03 · UC-SOC-04 · UC-SOC-05 · UC-AI-07 phía backend)

| | |
|---|---|
| **Ngày lập** | 2026-11-28 |
| **Thể loại** | server-spring (module `social` feature `review`, `sentiment`; cổng cài ở `order`, `catalog`, `vendor`, `promotion`) + nối `web/` (feature `reviews`, `product`, `seller-reviews`, `admin-moderation` phần đánh giá, `seller-analytics` khối cảm xúc) |
| **Use case** | Đánh giá sản phẩm đã mua & Tải ảnh thực tế (UC-SOC-03) · Người bán phản hồi đánh giá (UC-SOC-04) · Báo cáo đánh giá vi phạm / spam & Kiểm duyệt đánh giá (UC-SOC-05) · Phân tích cảm xúc đánh giá bằng PhoBERT (UC-AI-07 — phần backend; phần AI Worker ở [plan_phan_tich_cam_xuc.md](../server-python/plan_phan_tich_cam_xuc.md)) |
| **Tài liệu chuẩn** | UC phân hệ 08 (UC-SOC-03 dòng 2192, 04 dòng 2225, 05 dòng 2249, UC-AI-07 dòng 2296) · API 5.8 (đánh giá công khai), 7.9, 7.10, 10.8, 10.9 (`/seller/analytics/sentiment`), 11.5, 12.1 (`PATCH /internal/reviews/{id}/sentiment`), 12.2 (quét lại 15 phút), 15 (`GNE-SOC-409x`, `4221`) · ENTITY 9.1 `ProductReview`, 9.3 `ReviewReport`, 9.4 `ReviewHelpfulVote`, 2.2 `Product.rating`, 1.4 `VendorProfile.applyRating`, 4.8 `CoinTransaction` (`REVIEW_BONUS`) · LUU_TRU (bucket ảnh đánh giá) — xem [README](../README.md) |
| **Theo dõi tiến độ** | [progress_danh_gia_c7.md](progress_danh_gia_c7.md) — cùng mã task `Rxx.yy` |
| **Phụ trách** | Người làm plan — lấy theo **`git config user.name`** trên máy thực hiện (ghi vào progress ở R00.01); không ghi tên cố định |
| **Người commit** | danh tính git đã cấu hình trên máy đó — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Phụ thuộc** | **C5** (đơn `COMPLETED`, `OrderCompletedEvent`, cờ `can_review`). Plan [6] AI Worker cho phần chạy thật UC-AI-07 (backend có `AI_WORKER_MODE=stub` nên không phải chờ). Hợp đồng HTTP chốt ở R00.05 ↔ Z00.03 |
| **Mục tiêu** | 4 use case ⬜ → ✅. Khách đánh giá dòng hàng đã nhận (ảnh / video, ẩn tên, +50 Điểm khi có ảnh), Hữu ích, báo cáo; shop phản hồi một lần; Moderator ẩn / hiện; mỗi đánh giá có chữ được chấm cảm xúc, shop xem thống kê, hàng chờ ưu tiên đánh giá tiêu cực |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `1b66eae`, 2026-11-28)

| Phần | Đã có | Còn thiếu |
|---|---|---|
| **Bảng / entity** (`V9__review.sql`) | `product_reviews` (`order_item_id` UNIQUE, `product_id`, `customer_id`, `vendor_id`, `rating`, `comment`, `image_urls_json`, `video_url`, `vendor_reply`, `vendor_replied_at`, `is_visible`, `helpful_count`, `is_anonymous`, `aspect_sentiment_json`); `review_reports` (`review_id`, `reporter_id`, `reason`, `note`, `created_at`); `review_helpful_votes`; entity `ProductReview.reply / hide / adjustHelpful / attachSentiment`, `ReviewReport.of`, `ReviewHelpfulVote.of` | ⚠ `review_reports` **không có trạng thái** xử lý (Admin 11.5 lọc `state=PENDING`) và không UNIQUE người báo cáo (API 7.10 `409 GNE-SOC-4093`) → migration (D9); index theo shop / sản phẩm |
| **Catalog** | `Product.rating` (+ `review_count`? kiểm R00.04), trang sản phẩm đọc điểm từ bảng `products` | Cổng cập nhật điểm khi đánh giá đổi (D6) |
| **Điểm** | `CoinTransactionType.REVIEW_BONUS` đã có, `CoinFacade` | Cổng thưởng (D7) |
| **Upload** | `UploadPurpose` có `PRODUCT_IMAGE`, `CHAT_IMAGE`… | `REVIEW_IMAGE`, `REVIEW_VIDEO` (D4) |
| **Thông báo** | `NotificationType` không có loại "shop phản hồi đánh giá" | `REVIEW_REPLIED` (D11) |
| **AI** | `social/sentiment/*` chỉ có `.gitkeep`; `common/ai` + `InternalTokenFilter` (đã dùng cho KYC `PATCH /internal/kyc/{id}/result`), `AI_WORKER_URL`, `AI_WORKER_MODE=stub|http` | `SentimentClient`, endpoint nội bộ, job quét lại, thống kê |
| **Phụ thuộc module** | `catalog → social`, `vendor → social`, `order → social` (đã có) | **`social` không import `order` / `catalog` / `vendor` / `promotion`** — mọi thông tin ngoài qua cổng do module kia cài (README mục 3) |
| **Mã lỗi** | `GNE-SOC-4031`, `4096` (B6) | `GNE-SOC-4091…4095`, `GNE-SOC-4221` (API 15) |
| **Web** | `reviews/review.api.ts` (`listReviews`, `toggleHelpful`, `reportReview`, `createReview`), `seller-reviews/review.api.ts` (`listShopReviews`, `replyReview`), `admin-moderation/review.api.ts` (`listReviewReports`, `setReviewVisibility`), `seller-analytics/analytics.api.ts` (`getSentiment`) — tất cả mock | Nối theo cờ (D14) |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | **`social` chỉ khai cổng**, module khác cài: `ReviewEligibility` (order), `ReviewProductStats` (catalog), `ReviewShopStats` (vendor), `ReviewCoinBonus` (promotion — cạnh mới `promotion → social`). `ModuleBoundaryTest` phải xanh | README mục 3 |
| N2 | Chống đánh giá ảo: mỗi đánh giá gắn đúng một `order_item_id` của **chính người gọi**, đơn `COMPLETED`; UNIQUE `order_item_id` | UC-SOC-03 ràng buộc |
| N3 | Điểm sản phẩm / shop chỉ tính đánh giá **đang hiện** (`is_visible = true`); tính lại trong cùng transaction với thay đổi (tạo, ẩn, hiện) | UC-SOC-05 b4 |
| N4 | Kết quả AI chỉ **tham khảo**: không tự ẩn, không đổi số sao / điểm shop; không gửi tên / email / SĐT khách sang AI Worker | UC-AI-07 ràng buộc |
| N5 | Gọi AI `@Async @TransactionalEventListener(AFTER_COMMIT)`; lỗi không ảnh hưởng việc lưu đánh giá | TS 4 |
| N6 | Nhân viên kiểm duyệt cần `MODERATE_REVIEW`; mọi ẩn / hiện ghi `system_logs` | UC-SOC-05 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Đầu vào đánh giá** (API 7.9) | UC-SOC-03 b2–5 | `POST /me/reviews {order_item_id, rating 1–5, comment ≤ 1000 ký tự (lọc HTML → chữ thuần), image_keys ≤ 5, video_key ≤ 1, is_anonymous}`; ảnh / video là `object_key` đã tải qua presigned (D4) — server `promoteTo` sang khóa chính thức; `201 $Review` | ☐ |
| D2 | **Điều kiện được đánh giá** | UC-SOC-03 b7; API 15 | Cổng `ReviewEligibility.of(orderItemId, userId)` (order cài) → `{orderId, productId, vendorUserId, variantLabel, completed}`; không phải của người gọi → `404 GNE-ORD-4041`; đơn chưa `COMPLETED` → `409 GNE-SOC-4092`; đã đánh giá → `409 GNE-SOC-4091` (kiểm trước + bắt UNIQUE khi hai request đua) | ☐ |
| D3 | **Thời hạn đánh giá** | UC không ghi | Không giới hạn trong TLCN (UC không có) — ghi rõ "đánh giá được bất cứ lúc nào sau khi hoàn tất" ở UC; đánh giá **không sửa / xóa** được (UC không có luồng) | ☐ |
| D4 | **Ảnh / video** | UC-SOC-03 b4; LUU_TRU 5.x | `UploadPurpose.REVIEW_IMAGE` (jpeg/png/webp ≤ 5 MB, bucket công khai `review-media`), `REVIEW_VIDEO` (mp4/webm ≤ 50 MB, ≤ 60 giây — kiểm thời lượng phía web, server kiểm dung lượng + MIME); `image_urls_json` lưu khóa, API trả URL công khai | ☐ |
| D5 | **Ẩn tên** | UC-SOC-03 b5 | `is_anonymous = true` → tên hiển thị "N***h" (ký tự đầu + cuối), ảnh đại diện mặc định; chỉ áp ở đầu ra công khai / shop (Moderator xem tên thật) | ☐ |
| D6 | **Điểm sản phẩm & shop** | UC-SOC-03 b8; UC-SOC-05 b4 | `ReviewProductStats.recompute(productId)` (catalog cài: `rating = AVG`, `review_count = COUNT` các đánh giá hiện) + xóa cache chi tiết sản phẩm (`ProductDetailService.evict`); `ReviewShopStats.recompute(vendorUserId)` (vendor cài: `VendorProfile.applyRating`) + `PublicShopService.evict`. Gọi trong cùng transaction (N3) | ☐ |
| D7 | **Thưởng 50 Điểm** | UC-SOC-03 b10 | Có ≥ 1 ảnh **hoặc** video → `ReviewCoinBonus.grant(userId, reviewId, 50)` (promotion cài: `CoinLedgerService.credit(REVIEW_BONUS, reference = reviewId)`, hết hạn 12 tháng; idempotent theo `reference`); gọi trong transaction lưu đánh giá. Đánh giá bị ẩn sau đó **không** thu hồi Điểm (ghi UC) | ☐ |
| D8 | **Đánh dấu đơn đã đánh giá** | `orders.is_reviewed` | `ReviewEligibility.markReviewed(orderItemId)` (order cài: `is_reviewed = true` khi mọi dòng của đơn đã có đánh giá) — để `can_review` của C5 tắt | ☐ |
| D9 | **Trạng thái báo cáo** | API 11.5 (`state=PENDING`) | `V31`: `review_reports.status` (`PENDING`, `RESOLVED_HIDDEN`, `RESOLVED_KEPT`), `resolved_by_staff_id`, `resolved_at`, UNIQUE `(review_id, reporter_id)`; index `product_reviews (vendor_id, created_at DESC)`, `(product_id, is_visible, helpful_count DESC)` | ☐ |
| D10 | **Báo cáo** (API 7.10) | UC-SOC-05 b1–2 | `POST /reviews/{id}/reports {reason ∈ SPAM, OFFENSIVE, FAKE_PURCHASE, OTHER, note ≤ 300}` — khách hoặc shop (shop chỉ báo đánh giá của shop mình); không tự báo đánh giá của mình; trùng → `409 GNE-SOC-4093` | ☐ |
| D11 | **Phản hồi của shop** (API 10.8) | UC-SOC-04 b3–6 | `PUT /seller/reviews/{id}/reply {text ≤ 500}` — đánh giá thuộc shop (`404` nếu không), chưa phản hồi (`409 GNE-SOC-4094`), chặn SĐT (regex `(\+84|0)(\d[\s.-]?){8,10}` sau khi bỏ khoảng trắng / dấu chấm, chữ số viết liền ≥ 9) → `422 GNE-SOC-4221`; `ProductReview.reply(text, now)`; thông báo khách loại **`REVIEW_REPLIED`** (mới — sửa `ck_notifications_type` trong `V31`) | ☐ |
| D12 | **Danh sách** | API 5.8, 10.8 | Công khai `GET /public/products/{id}/reviews?filter=ALL|WITH_MEDIA|STAR_5…STAR_1&sort=HELPFUL|NEWEST&cursor` + `summary {rating_avg, count, by_star[], with_media_count}` (chỉ đánh giá hiện; `is_helpful_by_me` khi có token); shop `GET /seller/reviews?filter=ALL|UNREPLIED|ATTENTION(≤3★)&page&size` + phân loại đã mua + `sentiment` (nếu có, chỉ shop / Moderator xem — UC-AI-07 "không hiện cho khách") | ☐ |
| D13 | **Hữu ích** (API 7.10) | UC-SOC-03 b11 | `PUT / DELETE /reviews/{id}/helpful-vote` idempotent → `helpful_count`; không bấm cho đánh giá của mình → `422 GNE-VAL-4220` field `review_id` "Không thể bấm Hữu ích cho đánh giá của bạn" (không dùng `GNE-SOC-4095` — mã đó dành cho theo dõi shop của chính mình); `ProductReview.adjustHelpful(±1)` bằng `UPDATE … SET helpful_count = helpful_count ± 1` có điều kiện chèn / xóa dòng thật | ☐ |
| D14 | **Hàng chờ kiểm duyệt** (API 11.5) | UC-SOC-05 b2–5; UC-AI-07 b6 | `GET /admin/review-reports?state=PENDING&page&size` — **nhóm theo đánh giá**: số báo cáo, lý do, người báo cáo, nội dung, ảnh, `sentiment`; sắp: (1) ≥ 3 báo cáo, (2) `NEGATIVE` với `score ≥ 0,85` (nhãn "AI: Tiêu cực"), (3) báo cáo mới nhất. `PATCH /admin/reviews/{id} {is_visible, note}`: ẩn → `hide()`, mọi báo cáo `PENDING` → `RESOLVED_HIDDEN`, audit `REVIEW_HIDDEN`; giữ → `RESOLVED_KEPT` (không đổi `is_visible`); hiện lại đánh giá đã ẩn → audit **`REVIEW_RESTORED`** (mới); mọi nhánh tính lại điểm (D6) | ☐ |
| D15 | **Hợp đồng AI Worker** | API 12.1; UC-AI-07 b1–4 | Backend → worker `POST {AI_WORKER_URL}/sentiment {review_id, text}` (header `X-Internal-Token`), worker trả `202` rồi gọi lại `PATCH /api/v1/internal/reviews/{id}/sentiment {label, score, model}` (chung token). Backend lưu `aspect_sentiment_json = {"overall", "score", "model", "analyzedAt"}` qua `attachSentiment`. `AI_WORKER_MODE=stub` → `StubSentimentClient` chấm theo từ khóa (tệ / lỗi / chậm → NEGATIVE…) rồi gọi đúng service nội bộ (cùng đường như callback). Chốt cùng plan [6] Z00.03 | ☐ |
| D16 | **Thử lại & quét lại** | UC-AI-07 ngoại lệ; API 12.2 | Gọi worker lỗi / quá 10 giây → thử lại 2 / 8 / 30 giây (`@Retryable` hoặc vòng lặp có `Thread.sleep` trong luồng async — không giữ transaction); `SentimentRescanJob` 15 phút: đánh giá có chữ, `aspect_sentiment_json IS NULL`, tạo trong 24 giờ → gửi lại (lô 50). Chỉ số sao → không gọi | ☐ |
| D17 | **Thống kê cảm xúc của shop** (API 10.9 `sentiment`) | UC-AI-07 b5; UC-VND-08 b8 | `GET /seller/analytics/sentiment?range=7d|30d|90d` — tỉ lệ 3 nhãn (chỉ `score ≥ 0,6`), xu hướng theo tuần (giờ VN), top 5 sản phẩm nhiều đánh giá tiêu cực; cache Redis `sentiment:vendor:{id}:{range}` 10 phút, xóa khi có kết quả mới của shop. **Endpoint thuộc plan này** (lõi UC-AI-07); C10 chỉ hiển thị cùng màn Phân tích | ☐ |
| D18 | **Chạy lại khi đổi mô hình** | UC-AI-07 ngoại lệ | Endpoint `POST /admin/sentiment/reanalyze {model_before}` chỉ cho vai trò `ADMIN` (không có quyền nhân viên tương ứng trong 13 quyền) chạy nền theo lô — **tùy chọn**, làm nếu còn thời gian (đánh dấu `[-]` nếu bỏ) | ☐ |
| D19 | **Cờ web** | — | `reviews` (modal đánh giá ở Đơn mua, danh sách đánh giá trang sản phẩm, Hữu ích, Báo cáo), `seller-reviews` (Seller › Đánh giá), `review-moderation` (Admin › Kiểm duyệt đánh giá), `sentiment` (khối Cảm xúc ở Seller › Phân tích) | ☐ |
| D20 | **Ngoài phạm vi** | — | Phân tích theo khía cạnh (DATN — chỉ thêm khóa vào cùng cột), sửa / xóa đánh giá, hiển thị nhãn cảm xúc cho khách | ☐ |

---

## 3. Luồng nghiệp vụ chi tiết

```
POST /uploads/presigned-urls {purpose: REVIEW_IMAGE|REVIEW_VIDEO} → PUT Silo
POST /me/reviews (D1) ─► ReviewService.create (một transaction):
     ReviewEligibility.of (D2) → ProductReview.save → ReviewProductStats.recompute + ReviewShopStats.recompute (D6)
     → ReviewCoinBonus.grant nếu có media (D7) → ReviewEligibility.markReviewed (D8) → publish ReviewCreatedEvent
     AFTER_COMMIT @Async: SentimentDispatcher → SentimentClient (stub | http) (D15, D16)
AI Worker ─► PATCH /internal/reviews/{id}/sentiment (X-Internal-Token) ─► attachSentiment → evict sentiment:vendor:{id}
GET  /public/products/{id}/reviews (D12) · PUT/DELETE /reviews/{id}/helpful-vote (D13) · POST /reviews/{id}/reports (D10)
GET  /seller/reviews (D12) · PUT /seller/reviews/{id}/reply (D11) → AFTER_COMMIT thông báo REVIEW_REPLIED
GET  /admin/review-reports (D14) · PATCH /admin/reviews/{id} (D14) → hide/restore → recompute → audit
GET  /seller/analytics/sentiment (D17)        SentimentRescanJob 15 phút (D16)
```

---

## 4. Cây file sẽ tạo / sửa

```
server-spring/src/main/resources/db/migration/V31__c7_review_moderation.sql                 (mới — số tạm)
server-spring/src/main/java/com/genie/
├── common/error/ErrorCode.java          (sửa) SOC-4091…4094, SOC-4221
├── common/audit/AuditAction.java        (sửa) REVIEW_RESTORED
├── common/storage/UploadPurpose.java    (sửa) REVIEW_IMAGE, REVIEW_VIDEO
├── notification/inbox/domain/NotificationType.java (sửa) REVIEW_REPLIED
├── social/review/
│   ├── domain/       ReviewReport (sửa: status, resolve) · ReviewReportStatus (mới) · repo (sửa)
│   ├── application/  ReviewService · ReviewQueryService · HelpfulVoteService · ReviewReportService · ReviewModerationService
│   │                 SellerReviewService · PhoneNumberGuard · ReviewNames (ẩn tên) · ReviewCreatedEvent
│   │                 cổng: ReviewEligibility · ReviewProductStats · ReviewShopStats · ReviewCoinBonus · ReviewPorts (mặc định)
│   └── api/          MeReviewController · PublicReviewController · ReviewInteractionController · SellerReviewController
│                     AdminReviewModerationController
├── social/sentiment/ application (SentimentDispatcher, SentimentClient, HttpSentimentClient, StubSentimentClient,
│                     SentimentResultService, SentimentRescanJob, SellerSentimentStats) · api (InternalSentimentController,
│                     SellerSentimentController)
├── order/order/application/ReviewEligibilityImpl.java        (mới — cài cổng)
├── catalog/product/application/ReviewProductStatsImpl.java   (mới)
├── vendor/shop/application/ReviewShopStatsImpl.java          (mới)
└── promotion/coin/application/ReviewCoinBonusImpl.java       (mới — cạnh promotion → social)
server-spring/src/test/java/com/genie/social/review/…
    PhoneNumberGuardTest · ReviewNamesTest · ReviewCreateIT · ReviewConcurrencyIT · PublicReviewIT · HelpfulVoteIT
    ReviewReportIT · SellerReviewIT · ReviewModerationIT · SentimentCallbackIT · SentimentRescanJobIT · SellerSentimentIT
web/src/features/reviews/… · product/… (danh sách đánh giá) · seller-reviews/… · admin-moderation/infrastructure/review.api.ts
web/src/features/seller-analytics/infrastructure/analytics.api.ts (getSentiment) · error-codes.ts · i18n
docs/Document/… ENTITY (9.1, 9.3, 10.1, Phụ lục A/E) · API_SPEC (5.8, 7.9, 7.10, 10.8, 10.9, 11.5, 12.1, 15) · LUU_TRU (bucket)
                Usecase.md · CHECKLIST · BAO_CAO · xlsx
docs/api-tests/reviews.http · docs/kiem-thu/kich-ban-2026-11-28-c7.md
```

---

## 5. Bảng ánh xạ Use case → API → Dữ liệu

| Bước UC | Endpoint / tác vụ | Bảng | Quyền |
|---|---|---|---|
| UC-SOC-03 b1–10 | `POST /uploads/presigned-urls`, `POST /me/reviews` | `product_reviews`, `products`, `vendor_profiles`, `coin_transactions`, `user_coins`, `orders.is_reviewed` | chủ dòng hàng |
| UC-SOC-03 b11 | `GET /public/products/{id}/reviews`, `PUT/DELETE /reviews/{id}/helpful-vote` | `product_reviews`, `review_helpful_votes` | công khai / đăng nhập |
| UC-SOC-04 | `GET /seller/reviews`, `PUT /seller/reviews/{id}/reply` | `product_reviews`, `notifications` | VENDOR |
| UC-SOC-05 | `POST /reviews/{id}/reports`, `GET /admin/review-reports`, `PATCH /admin/reviews/{id}` | `review_reports`, `product_reviews`, `products`, `vendor_profiles`, `system_logs` | đăng nhập / `MODERATE_REVIEW` |
| UC-AI-07 | sự kiện → AI Worker, `PATCH /internal/reviews/{id}/sentiment`, `SentimentRescanJob`, `GET /seller/analytics/sentiment` | `product_reviews.aspect_sentiment_json` | `X-Internal-Token` / VENDOR |

**Mã lỗi:** `GNE-SOC-4091` (đã đánh giá), `4092` (đơn chưa hoàn tất), `4093` (đã báo cáo), `4094` (shop đã phản hồi), `GNE-SOC-4221` (phản hồi chứa SĐT); dùng lại `GNE-ORD-4041`, `GNE-VAL-4220`, `GNE-SEC-4030`.

---

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — `ModuleBoundaryTest` (**`social` không import `order` / `catalog` / `vendor` / `promotion`**).
2. Swagger: summary `UC-SOC-03/04/05`, `UC-AI-07`; endpoint nội bộ ẩn khỏi tài liệu công khai nếu tiền lệ KYC làm vậy.
3. `docs/api-tests/reviews.http`: đánh giá dòng hàng của người khác → 404; đơn chưa hoàn tất → 4092; lần 2 → 4091; phản hồi có SĐT → 4221.
4. Đụng `web/`: `npm run verify`.
5. Tick progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Rxx.yy` = task; `Rxx.Cn` = task commit.

### R00 — Chuẩn bị

- **[R00.01]** Chạy `git config user.name` / `user.email` — ghi vào dòng *Phụ trách* của progress; kiểm C5 đã merge (đơn `COMPLETED`, `OrderCompletedEvent`).
- **[R00.02]** Chốt D1–D20 vào progress.
- **[R00.03]** Tạo nhánh `feature/c7-reviews` từ `develop`.
- **[R00.04]** Đọc `Product` (`rating`, `review_count`?), `VendorProfile.applyRating`, `CoinLedgerService.credit`, `UploadService.promoteTo`, `InternalTokenFilter` (đường dẫn nội bộ đang cho phép), CHECK `ck_notifications_type`, `ck_system_logs_action_type` mới nhất.
- **[R00.05]** Chốt hợp đồng HTTP với plan [6] (Z00.03): request / callback / token / mã lỗi / thời gian chờ — ghi vào cả hai progress.
- **[R00.06]** Đọc màn mock: modal đánh giá (sao, chữ, ảnh / video, ẩn tên), danh sách đánh giá trang sản phẩm (lọc, Hữu ích, Báo cáo), Seller › Đánh giá (3 lọc, phản hồi), Admin › Kiểm duyệt đánh giá (hàng chờ, nhãn AI), khối Cảm xúc ở Seller › Phân tích.

### R01 — Tài liệu trước

- **[R01.01]** ENTITY 9.1 (ẩn tên D5, không sửa / xóa D3, Điểm thưởng không thu hồi D7), 9.3 (D9), 10.1 (`REVIEW_REPLIED`), Phụ lục A (`REVIEW_RESTORED`, `ReviewReportStatus`), Phụ lục E; LUU_TRU bucket `review-media`.
- **[R01.02]** API_SPEC — 5.8 (lọc / sắp / `summary` D12), 7.9 (D1–D2), 7.10 (D10, D13), 10.8 (D11, D12), 10.9 `sentiment` (D17), 11.5 (D14), 12.1 (D15), 15, lịch sử phiên bản.
- **[R01.03]** Usecase.md — UC-SOC-03 (D3, D7), UC-SOC-05 (trạng thái báo cáo), UC-AI-07 (stub khi không có worker, endpoint thống kê thuộc C7).
- **[R01.C1]** COMMIT — `docs(social): specify verified reviews, seller replies, review moderation and sentiment callbacks`

### R02 — Migration & cổng

- **[R02.01]** `V31__c7_review_moderation.sql` (D9, D11 `REVIEW_REPLIED`, `REVIEW_RESTORED`) — chép đủ danh sách CHECK của migration mới nhất.
- **[R02.02]** Cổng trong social + bản mặc định (`ReviewPorts`) + 4 bản cài ở order / catalog / vendor / promotion (D2, D6, D7, D8).
- **[R02.03]** `UploadPurpose.REVIEW_IMAGE / REVIEW_VIDEO` (D4), `ErrorCode`, `AuditAction`, `NotificationType`.
- **[R02.04]** Test — `SchemaValidationIT`, `ModuleBoundaryTest` (cạnh mới `promotion → social`), test từng bản cài cổng (IT nhỏ).
- **[R02.C1]** COMMIT — `feat(db): add review report states and review indexes`
- **[R02.C2]** COMMIT — `feat(social): add review ports implemented by order, catalog, vendor and promotion`

### R03 — Đánh giá đã mua (UC-SOC-03)

- **[R03.01]** `ReviewService.create` (D1, D2, D6–D8) + `ReviewNames` (D5).
- **[R03.02]** `ReviewQueryService` — công khai (D12) + `PublicReviewController` `GET /public/products/{id}/reviews`; `MeReviewController` `POST /me/reviews`.
- **[R03.03]** `HelpfulVoteService` + `ReviewInteractionController` (`PUT / DELETE /reviews/{id}/helpful-vote`) (D13).
- **[R03.04]** Test — `ReviewNamesTest`, `ReviewCreateIT` (đơn `COMPLETED` → 201, `products.rating` / shop cập nhật, +50 Điểm khi có ảnh, không ảnh không thưởng, `is_reviewed` bật khi đủ dòng; đơn `DELIVERED` → 4092; người khác → 404; lần 2 → 4091), `ReviewConcurrencyIT` (2 request cùng dòng → 1 đánh giá, Điểm thưởng 1 lần), `PublicReviewIT` (lọc có ảnh, sắp Hữu ích, ẩn tên, đánh giá ẩn không hiện + không tính điểm), `HelpfulVoteIT` (bấm 2 lần idempotent, bỏ, tự bấm của mình → 422).
- **[R03.C1]** COMMIT — `feat(social): let verified buyers review purchased items with photos and helpful votes`

### R04 — Phản hồi của shop (UC-SOC-04)

- **[R04.01]** `PhoneNumberGuard` (D11) + `SellerReviewService` + `SellerReviewController` (`GET /seller/reviews`, `PUT /seller/reviews/{id}/reply`).
- **[R04.02]** Thông báo `REVIEW_REPLIED` (AFTER_COMMIT) — "Người bán [Tên Shop] vừa phản hồi đánh giá của bạn".
- **[R04.03]** Test — `PhoneNumberGuardTest` (0912 345 678, +84.912.345.678, "không chín một hai…" không chặn, số đơn hàng TR-… không chặn), `SellerReviewIT` (3 lọc đúng; phản hồi lần 2 → 4094; đánh giá shop khác → 404; khách nhận thông báo).
- **[R04.C1]** COMMIT — `feat(social): let sellers reply once to reviews with a phone number guard`

### R05 — Báo cáo & kiểm duyệt (UC-SOC-05) — nhánh `feature/c7-review-moderation`

- **[R05.01]** `ReviewReportService` + `POST /reviews/{id}/reports` (D10).
- **[R05.02]** `ReviewModerationService` + `AdminReviewModerationController` (D14) — hàng chờ nhóm theo đánh giá + thứ tự ưu tiên, ẩn / giữ / hiện lại, audit, tính lại điểm.
- **[R05.03]** Test — `ReviewReportIT` (trùng → 4093; shop báo đánh giá shop khác → 403/404; tự báo của mình → 422), `ReviewModerationIT` (đánh giá 3 báo cáo đứng đầu, kế đến NEGATIVE 0,9; ẩn → điểm sản phẩm giảm đúng, báo cáo `RESOLVED_HIDDEN`, audit; giữ → `RESOLVED_KEPT`; hiện lại → `REVIEW_RESTORED`; Moderator thiếu quyền → 403).
- **[R05.C1]** COMMIT — `feat(social): add review reports and a prioritized moderation queue`

### R06 — Cảm xúc đánh giá phía backend (UC-AI-07) — nhánh `feature/c7-sentiment-backend`

- **[R06.01]** `SentimentDispatcher` (AFTER_COMMIT + `@Async`, bỏ qua đánh giá chỉ có sao) + `SentimentClient` (`HttpSentimentClient` / `StubSentimentClient`, D15) + thử lại (D16).
- **[R06.02]** `InternalSentimentController` `PATCH /internal/reviews/{id}/sentiment` + `SentimentResultService` (`attachSentiment`, nhãn hợp lệ, `score` 0–1, xóa cache).
- **[R06.03]** `SentimentRescanJob` (D16).
- **[R06.04]** `SellerSentimentStats` + `SellerSentimentController` `GET /seller/analytics/sentiment` (D17) + cache.
- **[R06.05]** Test — `SentimentCallbackIT` (token sai → 401; nhãn lạ → 422; đúng → JSON lưu đủ 4 khóa; callback trùng ghi đè cùng giá trị), `SentimentDispatchIT` (MockWebServer: worker lỗi 2 lần rồi thành công → gửi 3 lần; chỉ số sao → không gọi; stub chạy được đầu–cuối), `SentimentRescanJobIT`, `SellerSentimentIT` (bỏ `score < 0,6`; tuần theo giờ VN; top 5 tiêu cực; cache xóa khi có kết quả mới).
- **[R06.C1]** COMMIT — `feat(social): send reviews to the sentiment worker and store callback results`
- **[R06.C2]** COMMIT — `feat(social): serve seller sentiment statistics with a rescan job`

### R07 — Nối web (cờ `reviews`, `seller-reviews`, `review-moderation`, `sentiment`)

- **[R07.01]** `reviews/review.api.ts` (4 hàm) — modal đánh giá (tải ảnh / video qua presigned, đếm ký tự, ẩn tên), danh sách ở trang sản phẩm (lọc, sắp, Hữu ích, Báo cáo), nút "Đánh giá" ở Đơn mua theo `actions.can_review`.
- **[R07.02]** `seller-reviews/review.api.ts` — 3 lọc, phản hồi (lỗi `4221` tại ô, `4094`).
- **[R07.03]** `admin-moderation/review.api.ts` — hàng chờ (nhãn "AI: Tiêu cực" + độ tin cậy, số báo cáo), Ẩn / Giữ / Hiện lại kèm ghi chú.
- **[R07.04]** `seller-analytics` `getSentiment` — khối Cảm xúc (tỉ lệ, xu hướng tuần, top 5).
- **[R07.05]** vi / en + `error-codes.ts`; test mapper; kiểm 9 theme, 360 px; `npm run verify`.
- **[R07.C1]** COMMIT — `feat(web): connect reviews, seller replies, review moderation and sentiment stats to the real api`

### R08 — Kiểm thử & chạy thật

- **[R08.01]** Seed — **không** seed đánh giá giả (mỗi đánh giá phải gắn dòng hàng của đơn `COMPLETED` thật — N2); dữ liệu demo tạo bằng luồng thật ở R08.04 (đặt → giao → hoàn tất → đánh giá); chỉ seed báo cáo mẫu nếu đã có đánh giá thật.
- **[R08.02]** `docs/api-tests/reviews.http` — đánh giá, Hữu ích, báo cáo, phản hồi, kiểm duyệt, callback cảm xúc (stub + token).
- **[R08.03]** `docs/kiem-thu/kich-ban-2026-11-28-c7.md` — TC-C7-01…: hoàn tất đơn (C5) → đánh giá có ảnh → +50 Điểm → điểm sản phẩm đổi; ẩn tên; Hữu ích; báo cáo 3 người → lên đầu hàng chờ; Moderator ẩn → điểm giảm; shop phản hồi (chặn SĐT) → khách nhận thông báo; cảm xúc stub + AI Worker thật (khi plan [6] xong); khối Cảm xúc ở Phân tích; 9 theme; 360 px.
- **[R08.04]** Chạy thật — web build `PUBLIC_REAL_API=…,orders,reviews,seller-reviews,review-moderation,sentiment` + backend `local` (`AI_WORKER_MODE=stub`, rồi `http` + AI Worker thật khi plan [6] xong) + Postgres + Redis + Silo + Mailpit; Chromium (Playwright) với khách, chủ shop, Moderator; mỗi lỗi một commit `fix(…)`.
- **[R08.C1]** COMMIT — `docs(social): add review api tests and real-run scenarios`

### R09 — Hoàn thiện use case & tick tiến độ

- **[R09.01]** OpenAPI — summary `UC-SOC-03/04/05`, `UC-AI-07`.
- **[R09.02]** `docs/web-backend-handoff.md` — 4 cờ đã nối.
- **[R09.03]** Usecase.md — *Hiện trạng* 4 UC (UC-AI-07 ghi rõ backend + worker; nếu worker chưa xong → "backend xong, chạy thật với stub" và ô *Chạy* `◐`).
- **[R09.04]** `CHECKLIST_TIEN_DO_UC.md` — tick 5 lớp 4 UC, Trạng thái, **cột Phụ trách = tên ở R00.01** (UC-AI-07 ghi cả người làm plan [6] nếu khác), số mục 1 + phân hệ 08, dòng mục 5 *Nhật ký*.
- **[R09.05]** `BAO_CAO_UC_DOC_LAP.md` — mục C7 + dòng *Phụ trách*.
- **[R09.06]** `Quản Lý UseCase.xlsx` — 4 UC, cột AA.
- **[R09.07]** README ngày (mục 7) — tick dòng C7 khi cả plan [6] xong.
- **[R09.C1]** COMMIT — `docs(social): record reviews, replies, moderation and sentiment as implemented`
- **[R09.08]** Mở 3 PR xếp chồng vào `develop` (`c7-reviews` → `c7-review-moderation` → `c7-sentiment-backend`).

---

## 8. Ngoài phạm vi

- Phần AI Worker (tiền xử lý, PhoBERT ONNX) — plan [6].
- Khía cạnh cảm xúc (DATN), sửa / xóa đánh giá, nhãn cảm xúc cho khách.

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Vòng phụ thuộc khi social cần dữ liệu order / catalog / vendor | 4 cổng do module kia cài (N1); `ModuleBoundaryTest` |
| Điểm sản phẩm lệch khi ẩn / hiện đồng thời | Tính lại bằng `AVG` trong cùng transaction (không cộng trừ dồn) |
| AI Worker chậm làm chậm luồng đánh giá | AFTER_COMMIT + async + thử lại; job quét lại |
| Lộ thông tin khách sang AI | Chỉ gửi `review_id` + `text` (N4); test kiểm body request |
| Chặn SĐT nhầm số đơn / giá tiền | Regex có test ca biên; chỉ chặn chuỗi giống SĐT Việt Nam |
