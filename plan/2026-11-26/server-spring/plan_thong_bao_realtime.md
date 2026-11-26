# PLAN — Thông báo trong ứng dụng thời gian thực (UC-NOT-01) + Admin phát thông báo toàn sàn (UC-NOT-05)

| | |
|---|---|
| **Ngày lập** | 2026-11-26 |
| **Thể loại** | server-spring (module `notification`: feature `inbox`, `platform`, `admin`) + nối `web/` (feature `notifications`, `admin-marketing`, `shared/realtime`) |
| **Use case** | Thông báo trong ứng dụng thời gian thực — WebSocket (UC-NOT-01, cụm B6) · Admin phát thông báo toàn sàn — Broadcast Platform Notification (UC-NOT-05, nhóm A) |
| **Tài liệu chuẩn** | UC phân hệ 10 (UC-NOT-01 dòng 2463, UC-NOT-05 dòng 2509) · API 3.12, 4.1, 11.17, 13, 15 · ENTITY 10.1–10.3, 10.5, Phụ lục A (`NotificationType`, `PlatformNotificationTarget`, `AdminNotificationType`) · LUU_TRU 5.4 (`content-assets`, `notice/{noticeId}/…`) · TS 4, 8, 9, 10, 14 — xem [README](README.md) |
| **Theo dõi tiến độ** | [progress_thong_bao_realtime.md](progress_thong_bao_realtime.md) — cùng mã task `Nxx.yy` |
| **Phụ trách** | **Nguyễn Ngọc Thiện** — lập plan và thực hiện (nhận 2026-11-26) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `--no-verify`, không `Co-Authored-By` |
| **Mục tiêu** | 2 use case ⬜ → ✅ (đủ 5 lớp). Làm xong plan này thì module khác (chat, đơn hàng, thanh toán, KYC…) chỉ cần gọi `NotificationFacade` là có thông báo thời gian thực |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `5abb096`, 2026-11-26)

| Phần | Đã có | Còn thiếu |
|---|---|---|
| **Entity / bảng** (`V10__notification.sql`) | `Notification` (`notifications`: `user_id`, `type` 16 giá trị, `title`, `body`, `image_url`, `reference_id/type`, `metadata_json`, `channel`, `delivery_status`, `is_read`, `read_at`…; index `(user_id, is_read, created_at DESC)`), `PlatformNotification` (`target`, `send_at`, `recipient_user_ids_json`, `reach_count`, `read_count` là `@Formula COUNT(*)`), `UserPlatformNotificationRead` (PK `(user_id, platform_notification_id)`), `AdminNotification` (hộp thư chung đội vận hành, không `user_id`). Repository rỗng | ⚠ **Không có cột "đã phát"**: thông báo hẹn giờ không biết cron đã phát chưa (D2). Không có index đếm lượt đọc theo thông báo. `ck_notifications_type` chưa có loại cho tin nhắn chat (D6) |
| **Package** | `notification/{inbox,platform,admin,email}/{api,application,domain}` chỉ có `.gitkeep` | Toàn bộ service / controller |
| **Realtime backend** | `common/config/WebSocketConfig` (STOMP `/ws`, simple broker `/queue` `/topic`, `/app` prefix), `common/security/StompAuthInterceptor` (CONNECT cần Bearer; chỉ cho SUBSCRIBE `/user/queue/**` + `/topic/platform`), `common/realtime/UserChannel.sendAfterCommit` (mới có hằng `ACCOUNT`) — plan B1 D18 | Hằng `NOTIFICATIONS`; gửi `/topic/platform`; tra người đang trực tuyến (`SimpUserRegistry`) |
| **Quyền** | `StaffPermissionEnum.MANAGE_PLATFORM_NOTIFICATION` (Admin có đủ; preset chức danh không có) | — |
| **Dùng chung** | `UploadService.promoteTo`, `publicUrl` (banner); `AuditLogger`; `ApiResponseFactory.page()` / `list()`; `InvalidRequestException` (422 `errors[]`) | `UploadPurpose.NOTIFICATION_IMAGE`; `ErrorCode` `GNE-NOT-4091`, `GNE-NOT-4221` (đã có ở API 15); thư viện lọc HTML (backend chưa có `jsoup`) |
| **Cổng đảo ngược đang chờ module notification** | `vendor/kyc/application/KycAdminNotifier` (báo nhân viên hồ sơ KYC mới — `USER_APPROVAL`), `promotion/coin/application/CoinAlertPort` (lệch số dư Điểm — `SYSTEM_ALERT`) | Cài đặt (đặt trong **vendor** / **promotion**, gọi `NotificationFacade` — README mục 3) |
| **Sự kiện đang phát thật** | `NewDeviceLoginEvent` (auth), `KycDecidedEvent` (vendor: duyệt / từ chối / bổ sung), `KycSubmittedEvent`, `ShopVerifiedEvent`, `CheckoutCreatedEvent` (C2). `OrderStatusChangedEvent`, `OrderPaidEvent` còn `// TODO` (C3 / C4 làm) | Listener tạo thông báo |
| **Web** | `features/notifications` (mock): `listNotifications`, `markAllNotificationsRead` (**lệch spec**: `POST /me/notifications/read-all`), `markNotificationRead` (**lệch**: `POST /me/notifications/{id}/read`) — handoff dòng 35; DTO mock khác hẳn API 3.12 (`type` chữ thường kèm `icon/tint/bg`, `link.action`, `read_at`, `is_platform`); `NotifPanel`, `NotificationsPanel`; `shared/realtime/stomp.ts` (chưa có `send`), `accountChannel.ts` (một `StompConnection` riêng, khởi ở `Providers.tsx`); `admin-marketing` màn Thông báo sàn (`listPlatformNotifications`, `get…`, `save…`, `delete…`) chạy `USE_MOCK` | Mapper theo API 3.12; nối thật bằng cờ; hub realtime dùng chung; toast + âm báo; đồng bộ nhiều tab |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Package `com.genie.notification.<feature>.{api,application,domain}`; controller không mở transaction; một thao tác = một method `@Transactional` ở `application` | TS 4.1–4.2 |
| N2 | **`notification` là module "lá"**: chỉ được import `common` và `auth` (`UserFacade`). Module khác gọi vào qua `NotificationFacade` / `AdminInboxFacade`. Không cài cổng đảo ngược của module khác trong `notification` | README mục 3; `ModuleBoundaryTest` |
| N3 | Ghi thông báo **sau commit** của nghiệp vụ gốc: listener `@TransactionalEventListener(AFTER_COMMIT)` + transaction mới (`REQUIRES_NEW`); nghiệp vụ gốc không bao giờ hỏng vì thông báo lỗi | TS 4.5 |
| N4 | Đẩy WebSocket **sau commit** của chính dòng thông báo (`UserChannel.sendAfterCommit`); mất kết nối thì người dùng vẫn thấy ở `GET /me/notifications` (đẩy realtime chỉ là phụ) | UC-NOT-01 hậu điều kiện; UC-NOT-05 thay thế |
| N5 | Thông báo sàn là **fan-out on read**: một dòng `platform_notifications`, không chép ra từng người; trạng thái đọc ghi `user_platform_notification_reads` khi người dùng mở | UC-NOT-05 hậu điều kiện; ENTITY 10.2–10.3 |
| N6 | Mỗi người chỉ thấy thông báo của chính mình (IDOR): đọc / đánh dấu theo `id` luôn lọc thêm `user_id` (hoặc kiểm đối tượng nhận với `pn_`) | TS 9 |
| N7 | `content_html` của thông báo sàn **lọc ở server khi lưu** (danh sách thẻ cho phép), ảnh chỉ nhận khóa đối tượng `tmp/…` của chính người gửi | TS 9; API 11.17 |
| N8 | Mọi thao tác Admin trên thông báo sàn ghi `system_logs` (`PLATFORM_NOTIFICATION_*`) trong cùng transaction | NFR-08 |
| N9 | JSON snake_case, vỏ `ApiResponse`; hộp thư phân trang **cursor**, màn Admin phân trang `page / size` (API 11.17); `@Operation(summary = "UC-NOT-0x …")` | TS 8 |
| N10 | Mỗi task có test: unit (luật thuần) + `*IT` Testcontainers (PG + Redis + Silo); IT WebSocket dùng `WebSocketStompClient` thật | TS 14 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Đối tượng nhận** của `target` | ENTITY Phụ lục A; `User.role` ∈ `CUSTOMER, VENDOR, STAFF, ADMIN` | `CUSTOMER` = tài khoản vai trò `CUSTOMER`; `VENDOR` = vai trò `VENDOR` (chủ shop — vẫn mua hàng được nhưng nhận theo nhóm shop); `ALL` = `CUSTOMER ∪ VENDOR`; `SPECIFIC_USERS` = danh sách id trong `recipient_user_ids_json` (Admin nhập email → server đổi ra id, email lạ → `422 GNE-NOT-4221` `details.unknown_emails`, tối đa 500 email). Luôn chỉ tài khoản `status = ACTIVE`. **STAFF / ADMIN không nhận** thông báo sàn (họ dùng hộp `admin_notifications`) | ☐ |
| D2 | **Đánh dấu "đã phát"** — bảng không có cột, thông báo hẹn giờ không biết cron đã phát chưa | UC-NOT-05 thay thế "hẹn giờ"; API 11.17 `is_scheduled` | `V26` thêm `platform_notifications.dispatched_at TIMESTAMPTZ NULL` (ENTITY 10.2 sửa trước; NFR-12 chỉ thêm cột). Phát ngay: `dispatched_at = now` trong transaction tạo. Hẹn giờ: `dispatched_at = NULL`, cron đặt khi phát. Hộp thư người dùng **chỉ** đọc dòng `dispatched_at IS NOT NULL`. `is_scheduled = dispatched_at IS NULL`. Index `idx_platform_notifications_dispatched (dispatched_at DESC) WHERE dispatched_at IS NOT NULL` và `idx_platform_notifications_pending (send_at) WHERE dispatched_at IS NULL` | ☐ |
| D3 | **`reach_count`** chốt lúc nào | UC-NOT-05 bước 2 | Đếm **lúc phát** (không phải lúc tạo bản hẹn giờ) bằng một câu `COUNT(*)` theo D1, ghi cùng transaction đặt `dispatched_at`. Người đăng ký **sau** lúc phát không thấy thông báo cũ: điều kiện hiển thị thêm `users.created_at <= dispatched_at` (để tỉ lệ đọc `read_count / reach_count` không vượt 100 %) | ☐ |
| D4 | **Đẩy realtime thông báo sàn** — `/topic/platform` tới mọi người kể cả người không thuộc đối tượng | API 13 (`/topic/platform`); UC-NOT-05 bước 3 "tới người dùng đang trực tuyến **thuộc đối tượng**" | `ALL` → gửi một tin tới `/topic/platform`. `CUSTOMER` / `VENDOR` / `SPECIFIC_USERS` → lấy danh sách người đang kết nối từ `SimpUserRegistry` (một instance — TS), giao với đối tượng (một câu `SELECT id FROM users WHERE id = ANY(?) AND …`), gửi từng người qua `/user/queue/notifications` — không lộ nội dung `SPECIFIC_USERS` cho người khác. Ghi rõ vào API 13 | ☐ |
| D5 | **Gói tin realtime** trên `/user/queue/notifications` | API 13 ("thông báo mới (dạng 3.12) + `unread_count`") | `{ "type": "NEW_NOTIFICATION" \| "PLATFORM_NOTIFICATION", "notification": <phần tử 3.12>, "unread_count": 4 }` và `{ "type": "NOTIFICATIONS_READ", "ids": ["ntf_1"] \| null, "category": null, "unread_count": 0 }` (gửi lại cho **chính người đó** sau khi đánh dấu đọc → các tab / thiết bị khác cập nhật chuông). `/topic/platform` mang `{ "type": "PLATFORM_NOTIFICATION", "notification": … }` không có `unread_count` (client tự +1) | ☐ |
| D6 | **Nhóm hiển thị (`category`)** từ `NotificationType` — và loại cho tin nhắn chat | API 3.12 có tab `CHAT`; enum chưa có loại chat; UC-SOC-01 "người nhận offline → tạo thông báo in-app" | Ánh xạ cố định trong `NotificationCategory.of(type)`: **ORDER** = `ORDER_STATUS_CHANGED, PAYMENT_SUCCESS, PAYMENT_FAILED, REFUND_PROCESSED, NEW_ORDER_FOR_VENDOR, ESCROW_RELEASED, WALLET_CREDITED` · **PROMOTION** = `NEW_PLATFORM_VOUCHER, NEW_VENDOR_VOUCHER, FLASH_SALE_REMINDER` · **SYSTEM** = `SYSTEM_ALERT, KYC_APPROVED, KYC_REJECTED, NEW_DEVICE_LOGIN, PRODUCT_APPROVED, PRODUCT_REJECTED` + mọi dòng `pn_` / `an_` · **CHAT** = `CHAT_MESSAGE` (**giá trị mới**, `V26` sửa `ck_notifications_type`; ENTITY Phụ lục A sửa trước). `switch` không `default` để thêm loại mà quên nhóm thì không biên dịch | ☐ |
| D7 | **Hộp thư nhân viên** `admin_notifications` (không có `user_id`) | UC-NOT-01 bước 9; API 3.12 (`an_`) | Một endpoint `GET /me/notifications` cho mọi vai trò: STAFF / ADMIN thấy dòng `ntf_` của mình + `an_` **lọc theo quyền**: `USER_APPROVAL` ↔ `REVIEW_KYC`, `WITHDRAW_REQUEST` ↔ `PROCESS_PAYOUT`, `NEW_ORDER` / `ESCROW_DISCREPANCY` ↔ `VIEW_FINANCE`, `SYSTEM_ALERT` ↔ mọi nhân viên. `is_read` của `an_` là **dùng chung** (một người xử lý thì cả đội thấy đã đọc — đúng nghĩa "hộp thư chung"). Đẩy realtime `an_` mới: tới nhân viên đang trực tuyến có quyền tương ứng | ☐ |
| D8 | **Phân trang gộp 3 nguồn** (`ntf_`, `pn_`, `an_`) | API 3.12 (cursor) | Khóa sắp xếp `(sort_at DESC, source, id DESC)` với `sort_at` = `created_at` (ntf / an) hoặc `dispatched_at` (pn). Mỗi nguồn lấy `limit + 1` dòng nhỏ hơn cursor, trộn trong bộ nhớ, cắt `limit`; cursor = base64url của `{t, s, id}` (dùng lại kiểu cursor của `CoinQueryService` nếu có). `limit` 1–50 (mặc định 20, chuông gọi 5). `summary.unread_count` = tổng chưa đọc ba nguồn **không** phụ thuộc `category` | ☐ |
| D9 | **Đánh dấu đã đọc** | API 3.12 | `PATCH /me/notifications/{id}` `{is_read: true}` → `204`: `ntf_` → `markRead(now)` (lọc `user_id`, id lạ → `404 GNE-SYS-4040`); `pn_` → `INSERT … ON CONFLICT DO NOTHING` vào `user_platform_notification_reads` (chỉ khi người đó thuộc đối tượng, ngược lại 404); `an_` → cần quyền như D7. `{is_read: false}` → `422` (không hỗ trợ đánh dấu chưa đọc — ghi API). `POST /me/notifications/mark-all-read` `{category}` → một `UPDATE` cho `ntf_` + một `INSERT … SELECT` cho `pn_` còn chưa đọc + `an_` nếu nhân viên | ☐ |
| D10 | **Cách module khác tạo thông báo** | TS 4.4; N2, N3 | `NotificationFacade.send(NotificationCommand)` (record: `userId`, `type`, `title`, `body`, `imageUrl`, `link` = `{type, id}`, `metadata`) — ghi sau commit của người gọi, trả ngay. `AdminInboxFacade.raise(AdminNotificationType, title, content, referenceUrl)`. Listener **trong module notification** cho sự kiện công khai của `auth` (cạnh `notification → auth` được phép): `NewDeviceLoginEvent` → `NEW_DEVICE_LOGIN`. Sự kiện của `vendor` / `order` / `promotion`: **module đó** gọi facade (vd `vendor` thêm `KycNotificationListener` cho `KycDecidedEvent`) — không để `notification` import `vendor` | ☐ |
| D11 | **Nguồn thông báo nối ngay trong plan này** (có sự kiện thật) | mục 0 | (a) `NEW_DEVICE_LOGIN` (auth); (b) `KYC_APPROVED` / `KYC_REJECTED` (+ yêu cầu bổ sung → `KYC_REJECTED` `metadata.event = SUPPLEMENT_REQUESTED`) cho chủ shop; (c) `USER_APPROVAL` cho nhân viên (cài `KycAdminNotifier` trong vendor); (d) `SYSTEM_ALERT` lệch số dư Điểm (cài `CoinAlertPort` trong promotion); (e) `CheckoutCreatedEvent` → người mua `ORDER_STATUS_CHANGED` "Đặt hàng thành công" + mỗi shop `NEW_ORDER_FOR_VENDOR` (đặt ở module **order**, chỉ khi COD hoặc đã thanh toán — đọc chữ ký event ở N00.05; nếu event không đủ dữ liệu thì để C3 / C4). Đơn đang giao / tiền về ví / thanh toán: để chuỗi C3, C4, C9 gọi facade | ☐ |
| D12 | **Nội dung tiêu đề / thân** đa ngôn ngữ | i18n web | Server lưu **chuỗi tiếng Việt đã ghép sẵn** (snapshot, như email) + `metadata_json` (mã đơn, tên shop…) để sau này dịch lại phía client. Không thêm cột ngôn ngữ (DATN) | ☐ |
| D13 | **Lọc HTML** `content_html` | API 11.17 "rich-text"; N7 | Thêm phụ thuộc `org.jsoup:jsoup` (commit `build(deps)`); `Safelist.basic()` + `img` (chỉ `src` thuộc `PUBLIC_S3_URL` / `content-assets`) + `h2, h3`; mọi `a` thêm `rel="noopener noreferrer" target="_blank"`, chỉ `https:` hoặc đường dẫn tương đối. Tiêu đề ≤ 100 ký tự (API 11.17), nội dung sau lọc 1–20.000 ký tự. Cột `content` lưu HTML đã lọc; `content_preview` = văn bản thuần cắt 160 ký tự | ☐ |
| D14 | **Ảnh banner thông báo** | LUU_TRU 5.4 | `UploadPurpose.NOTIFICATION_IMAGE` (`content-assets`, JPEG / PNG / WebP ≤ 5 MB, công khai, quyền `MANAGE_PLATFORM_NOTIFICATION`); khóa chính thức `notice/{id}/{uuid}.{ext}` qua `UploadService.promoteTo`; đổi ảnh / xóa thông báo → xóa ảnh cũ `AFTER_COMMIT` | ☐ |
| D15 | **Sửa / xóa thông báo sàn** | API 11.17 | `PUT` chỉ khi `dispatched_at IS NULL` (chưa phát), ngược lại `409 GNE-NOT-4091`; được đổi cả `send_at` (đặt `null` = phát ngay khi lưu). `DELETE`: chưa phát → xóa; **đã phát cũng xóa được** (gỡ khỏi hộp thư mọi người — dùng khi phát nhầm; lượt đọc xóa theo `ON DELETE CASCADE`). Mỗi thao tác ghi `PLATFORM_NOTIFICATION_PUBLISHED / UPDATED / DELETED` | ☐ |
| D16 | **Cron phát hẹn giờ** | UC-NOT-05 thay thế | `PlatformNotificationDispatchJob` `@Scheduled(fixedDelay = 30 s)`: `SELECT … WHERE dispatched_at IS NULL AND send_at <= now ORDER BY send_at LIMIT 20 FOR UPDATE SKIP LOCKED`, mỗi dòng một transaction (đếm `reach_count`, đặt `dispatched_at`) rồi đẩy realtime sau commit; `send_at` phải ≥ now + 1 phút khi tạo / sửa (`422`), tối đa 90 ngày tới. Chạy khi `genie.scheduling.enabled` | ☐ |
| D17 | **Web: một kết nối WebSocket cho cả ứng dụng** | `accountChannel.ts` đang giữ `StompConnection` riêng | Tách `shared/realtime/realtimeHub.ts` (một `StompConnection`, `subscribe(dest, handler)`, `send(dest, body)` — thêm khung `SEND` vào `stomp.ts`); `accountChannel` chuyển sang dùng hub (không đổi hành vi B1). Chat (plan [2]) dùng lại hub | ☐ |
| D18 | **Âm báo & toast & nhiều tab** | UC-NOT-01 bước 5, 7 | Toast góc phải (`shared/ui` Toast sẵn có) + âm "ting" tổng hợp bằng Web Audio (không thêm tệp âm thanh; tắt khi tab ẩn / người dùng chưa tương tác trang). Đồng bộ tab bằng `BroadcastChannel('genie.notifications')` với tin `NOTIFICATION_SYNC`, `READ_ONE`, `READ_ALL` (tab nhận realtime phát lại cho tab khác) — kết hợp D5 `NOTIFICATIONS_READ` | ☐ |
| D19 | **Cờ nối web** | `isMocked(feature)` | `notifications` (chuông + Tài khoản › Thông báo, cả Seller / Admin), `platform-notifications` (màn Admin). Realtime bật khi `notifications` thật | ☐ |
| D20 | **Ngoài phạm vi** | — | Email kèm thông báo (UC-NOT-03 — chuỗi C11), push điện thoại / Zalo ZNS, Redis Pub/Sub nhiều instance (UC-NOT-01 bước 3 — TLCN một instance, simple broker; ghi DATN), kênh `/user/queue/counters` và `/user/queue/admin-queues` (badge — để plan của từng màn), xóa / dọn thông báo cũ | ☐ |

---

## 3. Luồng nghiệp vụ chi tiết

### 3.1 Tạo & đẩy thông báo cá nhân (UC-NOT-01)

```
Nghiệp vụ gốc (vd KycDecisionService) ──commit──► KycDecidedEvent
  └─ vendor/kyc/application/KycNotificationListener (AFTER_COMMIT)
        └─ NotificationFacade.send(cmd)                      [notification.inbox.application]
              ├─ transaction REQUIRES_NEW: INSERT notifications (channel IN_APP, delivery_status PENDING, is_read false)
              ├─ sau commit: UserChannel.sendAfterCommit(userId, "/queue/notifications",
              │                {type: NEW_NOTIFICATION, notification: NotificationView, unread_count})
              └─ markSent(now) → delivery_status SENT (cập nhật nhẹ, lỗi thì bỏ qua)
Trình duyệt: realtimeHub nhận → Toast + "ting" → tăng badge → BroadcastChannel NOTIFICATION_SYNC cho tab khác
```

### 3.2 Hộp thư gộp (API 3.12)

```
GET /me/notifications?category&cursor&limit
  ├─ ntf_: SELECT … FROM notifications WHERE user_id = :me [AND type IN (:types của category)] AND (sort, id) < cursor  LIMIT n+1
  ├─ pn_ : (bỏ khi category ∉ {null, SYSTEM} hoặc người gọi là STAFF/ADMIN)
  │        SELECT p.*, r.read_at FROM platform_notifications p LEFT JOIN user_platform_notification_reads r ON (r.user_id = :me …)
  │        WHERE p.dispatched_at IS NOT NULL AND p.dispatched_at >= :userCreatedAt
  │          AND (p.target = 'ALL' AND :role IN (CUSTOMER, VENDOR) OR p.target = :role
  │               OR p.target = 'SPECIFIC_USERS' AND p.recipient_user_ids_json @> to_jsonb(:me))  LIMIT n+1
  ├─ an_ : chỉ STAFF/ADMIN, type theo quyền (D7)
  └─ trộn theo (sort_at DESC, source, id DESC) → cắt n → next_cursor ; summary.unread_count (3 câu COUNT)
```

### 3.3 Phát thông báo toàn sàn (UC-NOT-05)

```
POST /admin/platform-notifications {title, content_html, image_object_key?, target, recipient_emails?, send_at?}
  ├─ @PreAuthorize MANAGE_PLATFORM_NOTIFICATION
  ├─ kiểm trường → 422 errors[] ; SPECIFIC_USERS: email → id (UserFacade.idsByEmails) ; thiếu / lạ → 422 GNE-NOT-4221 {unknown_emails}
  ├─ lọc HTML (D13) ; lưu dòng → lấy id → promote ảnh "notice/{id}/…" (D14)
  ├─ send_at = null → dispatch(now): reach_count = COUNT(đối tượng) ; dispatched_at = now
  ├─ audit PLATFORM_NOTIFICATION_PUBLISHED {target, scheduled: send_at != null}
  └─ sau commit (khi đã phát): ALL → /topic/platform ; còn lại → người đang online ∩ đối tượng → /user/queue/notifications (D4)
PlatformNotificationDispatchJob (30 s): dòng hẹn giờ tới giờ → dispatch(send_at) → đẩy như trên
GET /admin/platform-notifications?target&q&page&size → read_count (COUNT lượt đọc), reach_count, is_scheduled ; summary tháng này
```

---

## 4. Cây file sẽ tạo / sửa

```
server-spring/src/main/resources/db/migration/V26__b5_b6_notification_chat_voucher.sql     (mới — N01, dùng chung 3 plan)
server-spring/pom.xml                                                                       (sửa — org.jsoup:jsoup, D13)
server-spring/src/main/java/com/genie/
├── common/
│   ├── error/ErrorCode.java            (sửa) + PLATFORM_NOTIFICATION_DISPATCHED 4091 · PLATFORM_NOTIFICATION_RECIPIENTS 4221
│   ├── storage/UploadPurpose.java      (sửa) + NOTIFICATION_IMAGE
│   ├── audit/AuditAction.java          (sửa) + 7 giá trị (V26) + nhánh category() → CONFIG
│   ├── realtime/UserChannel.java       (sửa) + NOTIFICATIONS ; + sendToTopicAfterCommit(dest, payload)
│   ├── realtime/OnlineUsers.java       (mới) — bọc SimpUserRegistry: isOnline(userId), onlineAmong(ids), allOnline()
│   └── html/HtmlSanitizer.java         (mới) — D13 (dùng lại cho mô tả sản phẩm C1 nếu cần)
├── notification/
│   ├── inbox/domain/       NotificationType (+CHAT_MESSAGE) · NotificationCategory (mới) · NotificationRepository (truy vấn)
│   ├── inbox/application/  NotificationFacade · NotificationCommand · NotificationLink · NotificationView · InboxQueryService
│   │                       InboxReadService · InboxCursor · AuthEventNotificationListener (NEW_DEVICE_LOGIN)
│   ├── inbox/api/          MeNotificationController · NotificationResponse (record)
│   ├── platform/domain/    PlatformNotification (+dispatchedAt, dispatch(), isDispatched(), …) · repositories (truy vấn)
│   ├── platform/application/ PlatformNotificationAdminService · PlatformAudience · PlatformNotificationDispatcher
│   │                       PlatformNotificationDispatchJob · PlatformNotificationQueryService
│   ├── platform/api/       AdminPlatformNotificationController · request / response record
│   └── admin/application/  AdminInboxFacade · AdminInboxQueryService (an_ theo quyền)
├── vendor/kyc/application/ KycAdminNotifierImpl (mới) · KycNotificationListener (mới)
├── promotion/coin/application/CoinAlertNotifier (mới — cài CoinAlertPort)
└── order/checkout/application/CheckoutNotificationListener (mới — D11e, nếu event đủ dữ liệu)
server-spring/src/test/java/com/genie/notification/
    NotificationCategoryTest · InboxCursorTest · PlatformAudienceTest · HtmlSanitizerTest · PlatformNotificationTest
    MeNotificationIT · NotificationRealtimeIT (STOMP thật) · AdminPlatformNotificationIT · PlatformNotificationDispatchJobIT
    PlatformNotificationSecurityIT · AdminInboxIT · KycNotificationIT (vendor) · NotificationBoundaryIT (nghiệp vụ gốc không hỏng khi notification lỗi)
web/src/shared/realtime/{stomp.ts (sửa — SEND), realtimeHub.ts (mới), accountChannel.ts (sửa), realtime.test.ts (sửa)}
web/src/features/notifications/{domain,infrastructure,application,ui}/…          (sửa — DTO / mapper theo 3.12, isMocked, realtime, toast)
web/src/features/admin-marketing/infrastructure/marketing.api.ts                   (sửa — nhóm platform-notifications)
web/src/shared/api/error-codes.ts · web/src/i18n/{vi,en}.json                         (sửa)
docs/Document/…  ENTITY (10.1, 10.2, Phụ lục A, E) · API_SPEC (3.12, 4.1, 11.17, 13, 15, lịch sử) · Usecase.md · CHECKLIST · BAO_CAO · Quản Lý UseCase.xlsx
docs/api-tests/notifications.http · docs/kiem-thu/kich-ban-2026-11-26-thong-bao.md · docs/web-backend-handoff.md
```

---

## 5. Bảng ánh xạ Use case → API → Dữ liệu

| Bước UC | Endpoint / kênh | Bảng | Quyền |
|---|---|---|---|
| UC-NOT-01 b1–2: sự kiện nghiệp vụ → tạo thông báo | `NotificationFacade.send` (nội bộ) | `notifications` | hệ thống |
| UC-NOT-01 b3–5: đẩy tức thời, toast, chuông +1 | WS `/user/queue/notifications` (`NEW_NOTIFICATION`) | — | chủ kết nối |
| UC-NOT-01 b6: trung tâm thông báo, tab, chi tiết | `GET /me/notifications?category&cursor&limit` | `notifications`, `platform_notifications`, `user_platform_notification_reads`, `admin_notifications` | đã đăng nhập |
| UC-NOT-01 b6: đánh dấu một / tất cả | `PATCH /me/notifications/{id}` · `POST /me/notifications/mark-all-read` | như trên | đã đăng nhập |
| UC-NOT-01 b7: đồng bộ nhiều tab | WS `NOTIFICATIONS_READ` + `BroadcastChannel` | — | — |
| UC-NOT-01 b9: chuông Admin Console | `GET /me/notifications` (dòng `an_`) + WS | `admin_notifications` | STAFF / ADMIN theo quyền (D7) |
| UC-NOT-05 b1: tải ảnh banner | `POST /uploads/presigned-urls` (`NOTIFICATION_IMAGE`) | Silo `content-assets` | `MANAGE_PLATFORM_NOTIFICATION` |
| UC-NOT-05 b1–2: soạn, phát / hẹn giờ | `POST /admin/platform-notifications` · `PUT …/{id}` | `platform_notifications`, `system_logs` | `MANAGE_PLATFORM_NOTIFICATION` |
| UC-NOT-05 b3: đẩy tới người đang trực tuyến | WS `/topic/platform` hoặc `/user/queue/notifications` (`PLATFORM_NOTIFICATION`) | — | — |
| UC-NOT-05 b4: người dùng mở | `PATCH /me/notifications/pn_{id}` | `user_platform_notification_reads` | đối tượng nhận |
| UC-NOT-05 b5: thống kê | `GET /admin/platform-notifications` (+ `/{id}`) | như trên | `MANAGE_PLATFORM_NOTIFICATION` |
| UC-NOT-05 thay thế: hẹn giờ | `PlatformNotificationDispatchJob` | `platform_notifications` | hệ thống |
| Xóa | `DELETE /admin/platform-notifications/{id}` | như trên + `system_logs` | `MANAGE_PLATFORM_NOTIFICATION` |

**Mã lỗi:** `GNE-NOT-4091` (409 — đã phát, không sửa được), `GNE-NOT-4221` (422 — người nhận rỗng / email lạ). Dùng lại `GNE-VAL-4220`, `GNE-SEC-4030`, `GNE-SYS-4040`.

---

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — unit + IT + ArchUnit (`ModuleBoundaryTest` — **không vòng**) + `SchemaValidationIT` (sau `V26`).
2. Swagger UI có endpoint mới, summary `UC-NOT-01` / `UC-NOT-05`.
3. Thử tay `docs/api-tests/notifications.http`: Admin (`BOOTSTRAP_ADMIN_EMAIL` trong `.env`) phát được; `linh.tm@genie.example` (Finance Officer — không có `MANAGE_PLATFORM_NOTIFICATION`) → `403`; `mai.tran@gmail.com` đọc hộp thư.
4. Đụng `web/`: `cd web && npm run verify` (lint → astro check + tsc → vitest → build → check-i18n).
5. Tick task ở progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Nxx.yy` = task; `Nxx.Cn` = task commit.

### N00 — Chuẩn bị

- **[N00.01]** Kiểm danh tính git — `git config user.name` / `user.email` (phải là `Thien211105`).
- **[N00.02]** Chốt D1–D20 — ghi kết quả vào bảng *Quyết định* đầu file progress (mặc định "như đề xuất").
- **[N00.03]** Cập nhật `develop` — `git switch develop && git pull`; ghi mã commit `develop` vào progress.
- **[N00.04]** Kiểm số migration — `git log origin/develop --oneline -- server-spring/src/main/resources/db/migration`; C1 / C2 đã chiếm `V26` chưa → chốt số thật của migration chung, sửa README mục 5.
- **[N00.05]** Đọc chữ ký sự kiện — `CheckoutCreatedEvent` (có `userId`, danh sách đơn con / `vendorId`, phương thức thanh toán?) để chốt D11e; `KycDecidedEvent.status` các giá trị thật; `NewDeviceLoginEvent`.
- **[N00.06]** Đọc màn mock — chuông Storefront / Seller / Admin, `/account/notifications` (5 tab, "Xem thêm", modal chi tiết), Admin › Thông báo sàn (3 chỉ số, bảng 5 dòng / trang, modal soạn, xác nhận phát): ghi các trường đang dùng để đối chiếu DTO mock với API 3.12 / 11.17.

### N01 — Migration chung `V26` (nhánh `feature/b56-shared-schema`, PR riêng — merge trước)

- **[N01.01]** Tài liệu trước (TS 7.3) — ENTITY: 10.2 thêm `dispatchedAt` + phương thức `dispatch(reach, at)`, `isDispatched()`, `isScheduled()`; Phụ lục A thêm `NotificationType.CHAT_MESSAGE` và 7 giá trị `AuditAction`; 11.2 (`Message`) thêm `isAuto`; ghi index mới ở mục 13.1; đồng bộ `GIAI_NGHIA_ENTITY.md`, `java-code/Diagram10_*.java`, `Diagram11_*.java`, `Diagram12_*.java`; ghi CHANGELOG ENTITY.
- **[N01.02]** `V26__b5_b6_notification_chat_voucher.sql` — đầu file ghi nguồn 3 plan; khối (a) thông báo sàn: `ALTER TABLE platform_notifications ADD COLUMN dispatched_at TIMESTAMPTZ`; `UPDATE platform_notifications SET dispatched_at = COALESCE(send_at, created_at) WHERE send_at IS NULL OR send_at <= now()` (dữ liệu cũ nếu có); 2 index D2; `CREATE INDEX idx_user_platform_notification_reads_notification ON user_platform_notification_reads (platform_notification_id)`.
- **[N01.03]** Khối (b) hộp thư — `DROP/ADD CONSTRAINT ck_notifications_type` (16 giá trị cũ + `CHAT_MESSAGE`); `CREATE UNIQUE INDEX uk_notifications_chat_unread ON notifications (user_id, reference_id) WHERE type = 'CHAT_MESSAGE' AND is_read = FALSE` (chat plan [2] gộp thông báo theo hội thoại bằng `ON CONFLICT`); `CREATE INDEX idx_admin_notifications_read_created ON admin_notifications (is_read, created_at DESC)`.
- **[N01.04]** Khối (c) chat — `ALTER TABLE messages ADD COLUMN is_auto BOOLEAN NOT NULL DEFAULT FALSE` (tin tự động không tính tỉ lệ phản hồi — plan [2] D11); `CREATE INDEX idx_conversations_customer_last ON conversations (customer_id, last_message_at DESC)`, `idx_conversations_vendor_last ON conversations (vendor_id, last_message_at DESC)`.
- **[N01.05]** Khối (d) voucher — `CREATE INDEX idx_vouchers_vendor_valid_to ON vouchers (vendor_id, valid_to DESC) WHERE vendor_id IS NOT NULL`; `idx_vouchers_type_valid_to ON vouchers (type, valid_to DESC)` (plan [3] D16).
- **[N01.06]** Khối (e) nhật ký — `DROP/ADD CONSTRAINT ck_system_logs_action_type` chép **đủ** danh sách của migration mới nhất trên `develop` (`V24` hoặc của C1 nếu đã thêm) + `PLATFORM_NOTIFICATION_PUBLISHED`, `PLATFORM_NOTIFICATION_UPDATED`, `PLATFORM_NOTIFICATION_DELETED`, `VOUCHER_CREATED`, `VOUCHER_UPDATED`, `VOUCHER_DISABLED`, `VOUCHER_DELETED`.
- **[N01.07]** Entity & enum — `PlatformNotification.dispatchedAt` + phương thức (N01.01); `NotificationType.CHAT_MESSAGE`; `Message.isAuto` (`@Builder.Default false`); `AuditAction` 7 giá trị + nhánh `category()` → `CONFIG` (switch không `default` sẽ báo lỗi nếu quên).
- **[N01.08]** Test — `SchemaValidationIT`, `SeedDataIT` xanh; `PlatformNotificationTest` (dispatch đặt `reachCount` + `dispatchedAt`, dispatch lần 2 → `IllegalStateException`); `AuditActionTest` (nếu có kiểm đếm) cập nhật.
- **[N01.C1]** COMMIT — `docs(db): add schema changes for notifications, chat and vouchers`
- **[N01.C2]** COMMIT — `feat(db): add shared schema for notification, chat and voucher clusters`
- **[N01.09]** Mở PR `feature/b56-shared-schema` → `develop` (mô tả: chỉ thêm cột / index / giá trị CHECK, tương thích ngược); sau khi merge mới tạo nhánh N02.

### N02 — Tài liệu API & hằng số dùng chung (nhánh `feature/notification-inbox`)

- **[N02.01]** API_SPEC 3.12 — ghi D6 (bảng `type → category`), D8 (cursor gộp, `limit` 1–50, `summary.unread_count`), D9 (`is_read: false` → 422; `pn_` của người không thuộc đối tượng → 404), D7 (dòng `an_` theo quyền, đọc chung). Thêm `CHAT_MESSAGE` và `link.type = CONVERSATION` (plan [2]).
- **[N02.02]** API_SPEC 13 — bảng kênh: gói tin `/user/queue/notifications` (D5: `NEW_NOTIFICATION`, `PLATFORM_NOTIFICATION`, `NOTIFICATIONS_READ`), `/topic/platform` chỉ khi `target = ALL` (D4); bỏ câu "các kênh khác chờ plan Notification".
- **[N02.03]** API_SPEC 11.17 — `title` ≤ 100, nội dung lọc HTML (D13), `send_at` ≥ now + 1 phút và ≤ 90 ngày, tối đa 500 email, `DELETE` được cả khi đã phát (D15), `is_scheduled` = chưa phát (D2), `reach_count` chốt lúc phát (D3); 4.1 thêm purpose `NOTIFICATION_IMAGE`; mục lịch sử phiên bản 1.8.
- **[N02.04]** Usecase.md — UC-NOT-01 *Ràng buộc*: TLCN dùng simple broker một instance thay Redis Pub/Sub (bước 3 — DATN); UC-NOT-05 bước 2: `reach_count` lúc phát, người đăng ký sau không nhận.
- **[N02.05]** `ErrorCode` thêm `GNE-NOT-4091`, `GNE-NOT-4221` (khối `// ===== Thông báo`); `UploadPurpose.NOTIFICATION_IMAGE` (D14); `UserChannel.NOTIFICATIONS` + `sendToTopicAfterCommit`.
- **[N02.06]** `common/realtime/OnlineUsers` — bọc `SimpUserRegistry`: `isOnline(userId)`, `onlineAmong(Collection<Long>)`, `allOnline()`; unit test với registry giả.
- **[N02.07]** `HtmlSanitizer` + phụ thuộc `jsoup` (D13) — `HtmlSanitizerTest`: bỏ `<script>`, `onerror=`, `javascript:` trong `href`, `<iframe>`, `style`; giữ `<p><b><i><ul><li><a><img><h2>`; `img` ngoài `content-assets` bị bỏ; liên kết có `rel` đúng; văn bản thuần cắt 160 ký tự đúng ranh giới từ.
- **[N02.C1]** COMMIT — `docs(notification): specify inbox merge, realtime events and broadcast rules`
- **[N02.C2]** COMMIT — `build(deps): add jsoup for server-side html sanitizing`
- **[N02.C3]** COMMIT — `feat(common): add notification error codes, notice image purpose and online users`

### N03 — Tạo thông báo cá nhân & hộp thư nhân viên (UC-NOT-01)

- **[N03.01]** `NotificationCategory` + `of(NotificationType)` (D6) — `NotificationCategoryTest` (mọi giá trị enum đều có nhóm).
- **[N03.02]** `NotificationCommand`, `NotificationLink` (`type` ∈ API 3.12 `link.type` + `CONVERSATION`, `id`), `NotificationView` (phần tử 3.12: `id` có tiền tố, `category`, `type`, `title`, `body`, `image_url`, `created_at`, `is_read`, `link`).
- **[N03.03]** `NotificationFacade.send(cmd)` — đăng ký `afterCommit` (hoặc chạy ngay khi không có transaction) → `TransactionTemplate(REQUIRES_NEW)` lưu dòng (`IN_APP`, `PENDING`) → `UserChannel.sendAfterCommit(userId, NOTIFICATIONS, {NEW_NOTIFICATION, view, unread_count})` → `markSent`. Lỗi bất kỳ chỉ log `WARN` (N3). Thêm `sendMany(List<cmd>)` (một transaction, dùng cho nhiều shop của một đơn).
- **[N03.04]** `NotificationFacade.upsertChat(userId, conversationId, title, body)` — cho plan [2]: `INSERT … ON CONFLICT (user_id, reference_id) WHERE type = 'CHAT_MESSAGE' AND is_read = FALSE DO UPDATE SET body, created_at = now()` (một thông báo chưa đọc / hội thoại), đẩy realtime như N03.03.
- **[N03.05]** `AdminInboxFacade.raise(type, title, content, referenceUrl)` — lưu `admin_notifications`, sau commit đẩy `/user/queue/notifications` tới nhân viên đang trực tuyến có quyền (D7; `UserFacade` lấy quyền / vai trò theo lô).
- **[N03.06]** `AuthEventNotificationListener` (trong notification) — `NewDeviceLoginEvent` → `NEW_DEVICE_LOGIN`, `link.type = SECURITY`, thân "Đăng nhập mới từ {thiết bị} lúc {giờ VN}".
- **[N03.07]** `vendor/kyc/application/KycAdminNotifierImpl` (cài cổng có sẵn) → `AdminInboxFacade.raise(USER_APPROVAL, "Hồ sơ KYC mới: {shop}", …, "/admin/kyc")`; `KycNotificationListener` (`KycDecidedEvent` AFTER_COMMIT) → chủ shop `KYC_APPROVED` / `KYC_REJECTED` (`metadata.event` = `APPROVED / REJECTED / SUPPLEMENT_REQUESTED`), `link.type = KYC`.
- **[N03.08]** `promotion/coin/application/CoinAlertNotifier` (cài `CoinAlertPort`) → `SYSTEM_ALERT` "Lệch số dư Điểm của người dùng #{id}" `referenceUrl = /admin/users?q={id}`.
- **[N03.09]** (D11e — chỉ khi N00.05 xác nhận event đủ dữ liệu) `order/checkout/application/CheckoutNotificationListener` — COD: người mua "Đặt hàng thành công" (`ORDER_STATUS_CHANGED`, `link ORDER`), mỗi shop `NEW_ORDER_FOR_VENDOR`; đơn online chờ thanh toán → để C3. Không đủ dữ liệu → `[-]`, ghi chuyển C3.
- **[N03.10]** Test — `NotificationFacadeIT`: gọi trong transaction rồi rollback → **không** có dòng / không có tin WS; commit → có dòng `SENT`; notification lỗi (repository ném) → nghiệp vụ gốc vẫn commit (`NotificationBoundaryIT`); `upsertChat` gọi 3 lần → 1 dòng chưa đọc; đọc xong gọi lại → dòng mới. `KycNotificationIT`: duyệt / từ chối / bổ sung sinh đúng thông báo + `an_` khi nộp hồ sơ. `ModuleBoundaryTest` xanh (không vòng).
- **[N03.C1]** COMMIT — `feat(notification): create in-app notifications after commit and push them over websocket`
- **[N03.C2]** COMMIT — `feat(vendor): notify sellers and staff about kyc events`
- **[N03.C3]** COMMIT — `feat(promotion): raise staff alert when a coin balance is tampered`
- **[N03.C4]** COMMIT — `feat(order): notify buyer and shops when a cod checkout is placed` (bỏ nếu N03.09 `[-]`)

### N04 — API hộp thư `/me/notifications`

- **[N04.01]** `NotificationRepository` — truy vấn trang theo cursor + `category` (danh sách type), đếm chưa đọc, `markRead(id, userId)`, `markAllRead(userId, types)` (`@Modifying`).
- **[N04.02]** `PlatformNotificationRepository` / `UserPlatformNotificationReadRepository` — truy vấn native dòng `pn_` hiển thị cho một người (3.2), đếm chưa đọc, `insertRead(userId, id, now)` `ON CONFLICT DO NOTHING`, `insertAllUnread(userId, now)` (`INSERT … SELECT`), `isVisibleTo(id, userId)`.
- **[N04.03]** `AdminInboxQueryService` — trang `an_` theo type cho phép (D7), đếm chưa đọc, đánh dấu đọc.
- **[N04.04]** `InboxCursor` (D8) — mã hóa / giải mã, cursor hỏng → `422 GNE-VAL-4220`; `InboxCursorTest`.
- **[N04.05]** `InboxQueryService.page(me, category, cursor, limit)` — trộn ba nguồn theo khóa D8; `summary.unread_count`; người STAFF / ADMIN không nhận `pn_`.
- **[N04.06]** `InboxReadService` — `markRead(me, prefixedId)` theo tiền tố (D9; id sai định dạng → 404), `markAllRead(me, category)`; sau commit gửi `NOTIFICATIONS_READ` cho chính người đó (D5).
- **[N04.07]** `MeNotificationController` — `GET /me/notifications`, `PATCH /me/notifications/{id}`, `POST /me/notifications/mark-all-read`; `@Operation(summary = "UC-NOT-01 …")`; vỏ `ApiListResponse` có `pagination.next_cursor` + `summary`.
- **[N04.08]** `MeNotificationIT` — trộn đúng thứ tự giữa `ntf_` / `pn_` qua 3 trang; lọc `category` (CHAT chỉ ra `CHAT_MESSAGE`, SYSTEM gồm `pn_`); người A không đọc / đánh dấu được thông báo của B (404); `pn_` `VENDOR` không hiện với khách thuần; `SPECIFIC_USERS` chỉ người trong danh sách; người đăng ký sau lúc phát không thấy; `mark-all-read` theo nhóm chỉ đổi nhóm đó; đánh dấu `pn_` 2 lần không lỗi, `read_count` +1; `unread_count` đúng sau mỗi bước.
- **[N04.09]** `AdminInboxIT` — Moderator thấy `USER_APPROVAL` nhưng không thấy `WITHDRAW_REQUEST`; một nhân viên đọc thì nhân viên khác thấy đã đọc; khách hàng không bao giờ thấy `an_`.
- **[N04.10]** `NotificationRealtimeIT` — `WebSocketStompClient` kết nối `/ws` với token thật, `SUBSCRIBE /user/queue/notifications`: `send` → nhận `NEW_NOTIFICATION` + `unread_count`; tab thứ hai của cùng người nhận `NOTIFICATIONS_READ` sau `mark-all-read`; người khác **không** nhận.
- **[N04.C1]** COMMIT — `feat(notification): serve merged inbox with cursor paging and read marks`

### N05 — Nối web: hub realtime + chuông + hộp thư (UC-NOT-01)

- **[N05.01]** `shared/realtime/stomp.ts` — thêm `send(destination, body, headers?)` (khung `SEND`, `content-type:application/json`), xếp hàng khi chưa `CONNECTED`; test mã hóa khung `SEND`.
- **[N05.02]** `shared/realtime/realtimeHub.ts` (D17) — một `StompConnection` duy nhất (`startRealtime()`, `subscribe`, `send`, `onStatus` cho chat biết đang mất kết nối); `accountChannel.ts` chuyển sang hub (giữ nguyên `SESSION_REVOKED`, `PERMISSIONS_CHANGED`); `Providers.tsx` khởi hub; `realtime.test.ts` cập nhật — hành vi B1 không đổi.
- **[N05.03]** `notifications/infrastructure` — `notifications.dto.ts` theo API 3.12 (giữ DTO mock cũ ở `notifications.mock.ts` để nhánh mock vẫn chạy); mapper `toNotification` (category → icon / màu ở **domain/ui**, không ở DTO); `link.type` → route (`ORDER` → `/orders/{id}`, `VOUCHER_WALLET` → `/account/vouchers`, `SECURITY` → `/account/devices`, `KYC` → `/seller/kyc`, `PLATFORM_NOTIFICATION` → mở modal, `CONVERSATION` → mở khung chat, `ADMIN_KYC_QUEUE` → `/admin/kyc`…).
- **[N05.04]** `notifications.api.ts` — `isMocked('notifications')`; sửa 2 endpoint lệch spec (`PATCH /me/notifications/{id}` `{is_read: true}`, `POST /me/notifications/mark-all-read` `{category}`); `listNotifications(category, cursor, limit)` đọc `pagination.next_cursor`, `summary.unread_count`.
- **[N05.05]** `application/useNotifications.ts` — `unread` lấy từ `summary.unread_count` (không tự đếm trang đầu); `useNotificationStream()` đăng ký `/user/queue/notifications` + `/topic/platform` qua hub: chèn vào đầu cache TanStack, cập nhật badge, toast + "ting" (D18), `NOTIFICATIONS_READ` → cập nhật cache; `BroadcastChannel('genie.notifications')`.
- **[N05.06]** UI — `NotifPanel` (chuông 5 tin), `NotificationsPanel` (5 tab, "Xem thêm" theo cursor, "Đánh dấu tất cả đã đọc" theo tab, modal chi tiết; `pn_` hiện HTML đã lọc bằng `dangerouslySetInnerHTML` **chỉ** sau khi server lọc — thêm lọc phòng thủ phía client bằng danh sách thẻ như D13); chuông Seller Center + Admin Console dùng cùng hook. Kiểm 9 theme + khổ hẹp.
- **[N05.07]** `error-codes.ts` + `vi.json` / `en.json` — `GNE-NOT-4091`, `GNE-NOT-4221`; khóa mới `notifications.*` (toast, trống theo tab, chưa kết nối); `node ../scripts/check-i18n.mjs` xanh.
- **[N05.08]** Test — `notifications.rules.test.ts` (route theo `link`, nhãn nhóm), `notifications.mapper.test.ts` (3 tiền tố id); `npm run verify`.
- **[N05.C1]** COMMIT — `refactor(web): share one stomp connection across realtime channels`
- **[N05.C2]** COMMIT — `feat(web): connect notification bell and inbox to the real api with live updates`

### N06 — API quản trị thông báo sàn (UC-NOT-05)

- **[N06.01]** `PlatformNotification` — `create(…)`, `reschedule(sendAt, now)`, `changeContent(…)`, `dispatch(reach, at)`, `isDispatched()`, `isScheduled()`, `isEditable()`; ném `IllegalStateException` khi sửa bản đã phát.
- **[N06.02]** `PlatformAudience` (D1, D3) — `count(target, recipientIds, at)` (một câu SQL), `contains(notification, userId, role, userCreatedAt)`; `PlatformAudienceTest` + phần SQL kiểm trong IT.
- **[N06.03]** Request / kiểm trường — `title` 1–100, `content_html` (lọc → 1–20.000), `image_object_key` (tùy chọn, `tmp/…` của chính người gửi), `target` enum, `recipient_emails` (bắt buộc + 1–500 khi `SPECIFIC_USERS`; bỏ qua với target khác), `send_at` (null hoặc now + 1 phút … now + 90 ngày) → gom `errors[]` một lần (`InvalidRequestException`).
- **[N06.04]** `PlatformNotificationAdminService.create` — email → id qua `UserFacade.idsByEmails` (thêm phương thức nếu thiếu — module auth, thay đổi nhỏ, báo Khoa trong PR), email lạ → `GNE-NOT-4221` `details.unknown_emails`; lưu → `promoteTo(notice/{id}/…)`; phát ngay thì `dispatch(audience.count(…), now)`; audit `PLATFORM_NOTIFICATION_PUBLISHED`; sự kiện nội bộ `PlatformNotificationDispatchedEvent` (đẩy realtime sau commit).
- **[N06.05]** `update` — chỉ khi chưa phát (`409 GNE-NOT-4091`); đổi ảnh → ảnh cũ xóa sau commit; `send_at = null` → phát ngay; audit `PLATFORM_NOTIFICATION_UPDATED` với `details.changes`.
- **[N06.06]** `delete` — mọi trạng thái (D15); xóa ảnh sau commit; audit `PLATFORM_NOTIFICATION_DELETED {was_dispatched, reach_count, read_count}`; 404 khi không có.
- **[N06.07]** `PlatformNotificationQueryService` — danh sách `target`, `q` (tiêu đề / nội dung, không phân biệt hoa thường, bỏ dấu nếu có hàm `unaccent`), `page / size` (mặc định 5); mỗi dòng `code = "PN-{id}"`, `content_preview`, `image_url`, `created_by` ("{tên} ({id})" qua `UserFacade`), `send_at` (= `dispatched_at` hoặc giờ hẹn), `is_scheduled`, `read_count`, `reach_count`; `summary` tháng này theo giờ VN (`sent_this_month`, `total_reach`, `total_reads`); chi tiết thêm `content_html`, `recipient_emails`.
- **[N06.08]** `AdminPlatformNotificationController` — 5 endpoint API 11.17, `@PreAuthorize("hasAuthority('MANAGE_PLATFORM_NOTIFICATION')")`, `POST` → `201`, `DELETE` → `204`.
- **[N06.09]** `AdminPlatformNotificationIT` — tạo phát ngay (`reach_count` đúng theo từng target trên dữ liệu dựng bằng `TestDataBuilder`), tạo hẹn giờ (`is_scheduled = true`, `reach_count = 0`), HTML độc bị lọc, thiếu email / email lạ → `4221` kèm `unknown_emails`, sửa bản đã phát → `4091`, sửa bản hẹn giờ đổi giờ được, xóa bản đã phát → biến mất khỏi hộp thư người dùng, ảnh thật lên Silo rồi bị xóa khi xóa; mỗi thao tác 1 dòng `system_logs`.
- **[N06.10]** `PlatformNotificationSecurityIT` — Admin `200`; nhân viên được cấp `MANAGE_PLATFORM_NOTIFICATION` `200`; Finance Officer / Moderator / khách `403`; không token `401`; xin link ảnh `NOTIFICATION_IMAGE` thiếu quyền `403`.
- **[N06.C1]** COMMIT — `feat(notification): let admins compose, schedule and delete platform notifications`

### N07 — Phát & đẩy realtime thông báo sàn

- **[N07.01]** `PlatformNotificationDispatcher` — nhận `PlatformNotificationDispatchedEvent` sau commit: `ALL` → `UserChannel.sendToTopicAfterCommit("/topic/platform", …)`; còn lại → `OnlineUsers.allOnline()` ∩ đối tượng (một câu SQL theo lô 500) → `/user/queue/notifications` kèm `unread_count` của từng người (D4, D5).
- **[N07.02]** `PlatformNotificationDispatchJob` (D16) — lô 20, `SKIP LOCKED`, mỗi dòng một transaction, lỗi một dòng không dừng dòng khác, log số đã phát; `@ConditionalOnProperty genie.scheduling.enabled`.
- **[N07.03]** `PlatformNotificationDispatchJobIT` — bản hẹn giờ quá giờ được phát đúng 1 lần (chạy job 2 lần), `reach_count` đếm lúc phát (người tạo giữa lúc hẹn và lúc phát vẫn được tính), hai luồng chạy song song không phát trùng; dùng `MutableClock`.
- **[N07.04]** `NotificationRealtimeIT` (bổ sung) — `ALL` → người kết nối `/topic/platform` nhận; `SPECIFIC_USERS` → chỉ người trong danh sách nhận ở `/user/queue/notifications`, người ngoài danh sách không nhận gì; `VENDOR` → khách thuần không nhận.
- **[N07.C1]** COMMIT — `feat(notification): dispatch scheduled broadcasts and push them to online recipients`

### N08 — Nối web màn Admin "Thông báo sàn" + hiển thị trong hộp thư

- **[N08.01]** `admin-marketing/infrastructure/marketing.api.ts` (nhóm thông báo sàn) — `isMocked('platform-notifications')`; DTO / mapper theo API 11.17 (`code`, `content_preview`, `is_scheduled`, `read_count`, `reach_count`, `summary`); `savePlatformNotification` gửi `content_html`, `image_object_key` (ảnh qua `uploadFile(f, 'NOTIFICATION_IMAGE')`), `recipient_emails`, `send_at`.
- **[N08.02]** Modal soạn — ánh xạ `errors[].field` của `422` về ô; `GNE-NOT-4221` tô đỏ đúng các email trong `unknown_emails`; `GNE-NOT-4091` báo "đã phát — không sửa được" và tải lại; hộp xác nhận ghi rõ số người sẽ nhận (lấy `reach_count` sau khi tạo) và "phát ngay / hẹn giờ".
- **[N08.03]** Bảng — tỉ lệ đọc `read_count / reach_count` (0 khi `reach_count = 0`), nhãn "Đã hẹn giờ", phân trang 5 dòng; 3 chỉ số từ `summary`; modal chi tiết Sửa (ẩn khi đã phát) / Xóa (cảnh báo "gỡ khỏi hộp thư mọi người" khi đã phát).
- **[N08.04]** Hộp thư người dùng — dòng `pn_` mở modal chi tiết (ảnh banner + HTML) và gọi `PATCH` đánh dấu đọc.
- **[N08.05]** E2E mock — `web/tests/e2e/admin.spec.ts` thêm kịch bản soạn → xác nhận → thấy trong bảng (mock) nếu chưa có; `npm run verify`.
- **[N08.C1]** COMMIT — `feat(admin-ui): connect platform notification management to the real api`

### N09 — Kiểm thử & chạy thật

- **[N09.01]** `docs/api-tests/notifications.http` — luồng đúng (đăng nhập Admin → tải ảnh → phát `ALL` → khách đọc hộp thư → đánh dấu → Admin thấy `read_count` 1 → hẹn giờ → sửa → xóa) và lỗi (`4221`, `4091`, `403`, cursor hỏng `422`, id người khác `404`).
- **[N09.02]** `docs/kiem-thu/kich-ban-2026-11-26-thong-bao.md` — bảng ca TC-NOT-01…: chuông nhận ngay khi KYC được duyệt (2 tab cùng mở — cả hai cập nhật), đăng nhập thiết bị mới, nhân viên nhận "Hồ sơ KYC mới", phát `ALL` / `CUSTOMER` / `VENDOR` / `SPECIFIC_USERS` với 3 trình duyệt (khách, chủ shop, khách ngoài danh sách), hẹn giờ 2 phút, mất mạng rồi có lại (hub tự kết nối, hộp thư vẫn đủ), 9 theme, khổ hẹp, tắt JS (trang Tài khoản › Thông báo vẫn có khung SSR).
- **[N09.03]** Chạy thật — web bản build `PUBLIC_REAL_API=auth,upload,notifications,platform-notifications,…` (giữ các cờ đã nối) + backend `local` + Postgres + Redis + Silo + Mailpit; Chromium điều khiển bằng Playwright, 3 ngữ cảnh trình duyệt song song; ghi số ca đạt / lỗi, lỗi phát hiện sửa ngay (mỗi lỗi một commit `fix(…)`).
- **[N09.04]** Đo nhanh — phát `ALL` tới 200 người dùng seed đang kết nối (script STOMP) → thời gian tới người cuối < 1 giây; `GET /me/notifications` p95 < 150 ms trên seed (ghi số vào progress).
- **[N09.C1]** COMMIT — `docs(notification): add notification api tests and real-run scenarios`

### N10 — Hoàn thiện use case & tick tiến độ

- **[N10.01]** OpenAPI — `/v3/api-docs` có đủ 8 endpoint, summary `UC-NOT-01` / `UC-NOT-05`.
- **[N10.02]** `docs/web-backend-handoff.md` — nhóm `notifications`, `platform-notifications` đã nối; bỏ dòng 35 (endpoint lệch spec); ghi kênh realtime đã có.
- **[N10.03]** Usecase.md — câu *Hiện trạng* của UC-NOT-01, UC-NOT-05 → "Đã triển khai (2026-11-…)" + nguồn đã nối (D11).
- **[N10.04]** `CHECKLIST_TIEN_DO_UC.md` — tick 5 lớp của `UC-NOT-01`, `UC-NOT-05` (ô nào chưa đạt thì `◐` + ghi phần thiếu), Trạng thái ✅, **cột Phụ trách = Nguyễn Ngọc Thiện**, số mục 1 *Tổng quan* + phân hệ 10, dòng mục 5 *Nhật ký* (ngày · UC · việc · mã commit / PR); bảng *Thành viên & phân công* cập nhật hiện trạng.
- **[N10.05]** `BAO_CAO_UC_DOC_LAP.md` — dòng UC-NOT-05 ở mục 4 (Phụ trách Nguyễn Ngọc Thiện, "✅ Đã triển khai"), UC-NOT-01 trong bảng cụm B6 (thêm bảng trạng thái như B4), dòng *Hiện trạng* của Nguyễn Ngọc Thiện ở bảng *Thành viên*.
- **[N10.06]** `Quản Lý UseCase.xlsx` — cột *Hiện trạng mã nguồn* = "Đã triển khai" cho 2 UC, cột AA *Người phụ trách* = "Nguyễn Ngọc Thiện" (sửa XML, chuỗi dạng `&#NNNN;` — không mở bằng Excel để giữ định dạng); sheet *Thành viên* dòng Thiện.
- **[N10.07]** README ngày (mục 7) — tick dòng plan [1].
- **[N10.C1]** COMMIT — `docs(notification): record in-app and platform notifications as implemented`
- **[N10.08]** Mở PR `feature/notification-inbox` (N02–N05) rồi `feature/notification-platform-broadcast` (N06–N10) vào `develop`; mô tả PR theo `.github/pull_request_template.md`, tiêu đề đúng chuẩn commit.

---

## 8. Ngoài phạm vi

- Email giao dịch (UC-NOT-03), push điện thoại / ZNS, Redis Pub/Sub nhiều instance, dọn thông báo cũ — D20.
- Thông báo của đơn hàng đang giao / thanh toán / ví / hoàn tiền / duyệt sản phẩm: chuỗi C1, C3, C4, C8, C9 gọi `NotificationFacade` khi làm (facade đã sẵn).
- Kênh `/user/queue/counters`, `/user/queue/admin-queues` (badge giỏ / đơn / hàng chờ Admin) — plan của từng màn.
- Nhắm đối tượng theo hạng / tỉnh / hành vi, A/B thông báo — DATN.

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Vòng phụ thuộc module khi cài cổng `KycAdminNotifier`, `CoinAlertPort` | Cài trong module **nguồn** gọi `NotificationFacade` (README mục 3); `ModuleBoundaryTest` chạy ở mọi cổng commit |
| Trùng số migration với C1 / C2 | N00.04 + README mục 5; nhánh merge sau đổi số và chép đủ `CHECK` |
| Thông báo lỗi làm hỏng nghiệp vụ gốc | Ghi sau commit, transaction riêng, nuốt lỗi + log (N3); test `NotificationBoundaryIT` |
| Thông báo sàn lộ cho người ngoài đối tượng | Không dùng `/topic/platform` trừ `ALL` (D4); hộp thư lọc đối tượng ở SQL (N6); IT kiểm người ngoài không nhận |
| XSS qua `content_html` | Lọc server khi lưu (D13) + lọc phòng thủ phía client; test chuỗi tấn công phổ biến |
| Gộp 3 nguồn làm phân trang sai / trùng | Khóa sắp xếp toàn phần `(sort_at, source, id)`; IT đi 3 trang kiểm không trùng, không sót |
| Phát hẹn giờ trùng khi job chạy chồng | `FOR UPDATE SKIP LOCKED` + `dispatch()` ném khi đã phát; IT chạy song song |
| Web mở nhiều kết nối WebSocket | Hub dùng chung (D17); test đếm `new WebSocket` = 1 |
