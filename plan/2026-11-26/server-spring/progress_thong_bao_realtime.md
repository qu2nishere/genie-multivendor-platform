# PROGRESS — Thông báo trong ứng dụng thời gian thực (UC-NOT-01) + Admin phát thông báo toàn sàn (UC-NOT-05)

| | |
|---|---|
| **Plan** | [plan_thong_bao_realtime.md](./plan_thong_bao_realtime.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-26 |
| **Tổng** | 81 task + 17 commit, 11 giai đoạn |
| **Nhánh** | `feature/b56-shared-schema` (N01) → `feature/notification-inbox` (N02–N05) → `feature/notification-platform-broadcast` (N06–N10) |
| **Phụ trách** | **Nguyễn Ngọc Thiện** — lập plan và thực hiện (nhận 2026-11-26) |
| **Người commit** | danh tính git đã cấu hình trong repo (`Thien211105`) — không `--author`, không `--no-verify`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-27 — N04 xong, tiếp N05 (web) (52 / 98) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục *Ghi chú*.
- Task `COMMIT` chỉ tick sau khi qua *Cổng kiểm tra* (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`. Thông điệp đã ghi sẵn — tiếng Anh, chữ thường, đúng `commitlint.config.cjs`; có mã Jira thì thêm ` [#GNE-<số>]` cuối.
- Sau mỗi buổi làm: cập nhật cột *Xong* của bảng Tổng quan, dòng *Cập nhật gần nhất*, một dòng *Nhật ký* cuối trang.
- Cột *Chạy* của use case chỉ tick khi đã chạy thật trên giao diện (Chromium điều khiển bằng Playwright, hệ thống thật) và đạt hết ca của kịch bản — ghi cách chạy + số ca vào *Ghi chú*.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | **Đối tượng nhận** của `target` | ☑ | Như đề xuất · 2026-11-26 |
| D2 | **Đánh dấu "đã phát"** — bảng không có cột, thông báo hẹn giờ không biết cron đã phát chưa | ☑ | Như đề xuất · 2026-11-26 |
| D3 | **`reach_count`** chốt lúc nào | ☑ | Như đề xuất · 2026-11-26 |
| D4 | **Đẩy realtime thông báo sàn** — `/topic/platform` tới mọi người kể cả người không thuộc đối tượng | ☑ | Như đề xuất · 2026-11-26 |
| D5 | **Gói tin realtime** trên `/user/queue/notifications` | ☑ | Như đề xuất · 2026-11-26 |
| D6 | **Nhóm hiển thị (`category`)** từ `NotificationType` — và loại cho tin nhắn chat | ☑ | Như đề xuất · 2026-11-26 |
| D7 | **Hộp thư nhân viên** `admin_notifications` (không có `user_id`) | ☑ | Như đề xuất · 2026-11-26 |
| D8 | **Phân trang gộp 3 nguồn** (`ntf_`, `pn_`, `an_`) | ☑ | Như đề xuất · 2026-11-26 |
| D9 | **Đánh dấu đã đọc** | ☑ | Như đề xuất · 2026-11-26 |
| D10 | **Cách module khác tạo thông báo** | ☑ | Như đề xuất · 2026-11-26 |
| D11 | **Nguồn thông báo nối ngay trong plan này** (có sự kiện thật) | ☑ | Như đề xuất — (e) làm được: `CheckoutCreatedEvent(sessionId, userId, orderIds, paymentMethod, grandTotal)` đủ dữ liệu (N00.05) · 2026-11-26 |
| D12 | **Nội dung tiêu đề / thân** đa ngôn ngữ | ☑ | Như đề xuất · 2026-11-26 |
| D13 | **Lọc HTML** `content_html` | ☑ | Như đề xuất · 2026-11-26 |
| D14 | **Ảnh banner thông báo** | ☑ | Như đề xuất · 2026-11-26 |
| D15 | **Sửa / xóa thông báo sàn** | ☑ | Như đề xuất · 2026-11-26 |
| D16 | **Cron phát hẹn giờ** | ☑ | Như đề xuất · 2026-11-26 |
| D17 | **Web: một kết nối WebSocket cho cả ứng dụng** | ☑ | Như đề xuất · 2026-11-26 |
| D18 | **Âm báo & toast & nhiều tab** | ☑ | Như đề xuất · 2026-11-26 |
| D19 | **Cờ nối web** | ☑ | Như đề xuất · 2026-11-26 |
| D20 | **Ngoài phạm vi** | ☑ | Như đề xuất · 2026-11-26 |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| N00 | Chuẩn bị | 6 | 0 | 6 / 6 |
| N01 | Migration chung `V26` (nhánh `feature/b56-shared-schema`, PR riêng — merge trước) | 9 | 2 | 11 / 11 |
| N02 | Tài liệu API & hằng số dùng chung (nhánh `feature/notification-inbox`) | 7 | 3 | 10 / 10 |
| N03 | Tạo thông báo cá nhân & hộp thư nhân viên (UC-NOT-01) | 10 | 4 | 14 / 14 |
| N04 | API hộp thư `/me/notifications` | 10 | 1 | 11 / 11 |
| N05 | Nối web: hub realtime + chuông + hộp thư (UC-NOT-01) | 8 | 2 | 0 / 10 |
| N06 | API quản trị thông báo sàn (UC-NOT-05) | 10 | 1 | 0 / 11 |
| N07 | Phát & đẩy realtime thông báo sàn | 4 | 1 | 0 / 5 |
| N08 | Nối web màn Admin "Thông báo sàn" + hiển thị trong hộp thư | 5 | 1 | 0 / 6 |
| N09 | Kiểm thử & chạy thật | 4 | 1 | 0 / 5 |
| N10 | Hoàn thiện use case & tick tiến độ | 8 | 1 | 0 / 9 |
| **Tổng** | | **81** | **17** | **52 / 98** |

## N00 — Chuẩn bị

- [x] **N00.01** Kiểm danh tính git
- [x] **N00.02** Chốt D1–D20
- [x] **N00.03** Cập nhật `develop`
- [x] **N00.04** Kiểm số migration
- [x] **N00.05** Đọc chữ ký sự kiện
- [x] **N00.06** Đọc màn mock

## N01 — Migration chung `V26` (nhánh `feature/b56-shared-schema`, PR riêng — merge trước)

- [x] **N01.01** Tài liệu trước (TS 7.3)
- [x] **N01.02** `V26__b5_b6_notification_chat_voucher.sql`
- [x] **N01.03** Khối (b) hộp thư
- [x] **N01.04** Khối (c) chat
- [x] **N01.05** Khối (d) voucher
- [x] **N01.06** Khối (e) nhật ký
- [x] **N01.07** Entity & enum
- [x] **N01.08** Test
- [x] **N01.C1** 🔖 COMMIT `docs(db): add schema changes for notifications, chat and vouchers` · hash: `51f57ac`
- [x] **N01.C2** 🔖 COMMIT `feat(db): add shared schema for notification, chat and voucher clusters` · hash: `79c110d`
- [x] **N01.09** Mở PR `feature/b56-shared-schema` → `develop` (mô tả: chỉ thêm cột / index / giá trị CHECK, tương thích ngược); sau khi merge mới tạo nhánh N02.

## N02 — Tài liệu API & hằng số dùng chung (nhánh `feature/notification-inbox`)

- [x] **N02.01** API_SPEC 3.12
- [x] **N02.02** API_SPEC 13
- [x] **N02.03** API_SPEC 11.17
- [x] **N02.04** Usecase.md
- [x] **N02.05** `ErrorCode` thêm `GNE-NOT-4091`, `GNE-NOT-4221` (khối `// ===== Thông báo`); `UploadPurpose.NOTIFICATION_IMAGE` (D14); `UserChannel.NOTIFICATIONS` + `sendToTopicAfterCommit`.
- [x] **N02.06** `common/realtime/OnlineUsers`
- [x] **N02.07** `HtmlSanitizer` + phụ thuộc `jsoup` (D13)
- [x] **N02.C1** 🔖 COMMIT `docs(notification): specify inbox merge, realtime events and broadcast rules` · hash: `5bc76c5`
- [x] **N02.C2** 🔖 COMMIT `build(deps): add jsoup for server-side html sanitizing` · hash: `88636db`
- [x] **N02.C3** 🔖 COMMIT `feat(common): add notification error codes, notice image purpose and online users` · hash: `f8bf25b`

## N03 — Tạo thông báo cá nhân & hộp thư nhân viên (UC-NOT-01)

- [x] **N03.01** `NotificationCategory` + `of(NotificationType)` (D6)
- [x] **N03.02** `NotificationCommand`, `NotificationLink` (`type` ∈ API 3.12 `link.type` + `CONVERSATION`, `id`), `NotificationView` (phần tử 3.12: `id` có tiền tố, `category`, `type`, `title`, `body`, `image_url`, `created_at`, `is_read`, `link`).
- [x] **N03.03** `NotificationFacade.send(cmd)`
- [x] **N03.04** `NotificationFacade.upsertChat(userId, conversationId, title, body)`
- [x] **N03.05** `AdminInboxFacade.raise(type, title, content, referenceUrl)`
- [x] **N03.06** `AuthEventNotificationListener` (trong notification)
- [x] **N03.07** `vendor/kyc/application/KycAdminNotifierImpl` (cài cổng có sẵn) → `AdminInboxFacade.raise(USER_APPROVAL, "Hồ sơ KYC mới: {shop}", …, "/admin/kyc")`; `KycNotificationListener` (`KycDecidedEvent` AFTER_COMMIT) → chủ shop `KYC_APPROVED` / `KYC_REJECTED` (`metadata.event` = `APPROVED / REJECTED / SUPPLEMENT_REQUESTED`), `link.type = KYC`.
- [x] **N03.08** `promotion/coin/application/CoinAlertNotifier` (cài `CoinAlertPort`) → `SYSTEM_ALERT` "Lệch số dư Điểm của người dùng #{id}" `referenceUrl = /admin/users?q={id}`.
- [x] **N03.09** (D11e
- [x] **N03.10** Test
- [x] **N03.C1** 🔖 COMMIT `feat(notification): create in-app notifications after commit and push them over websocket` · hash: `ba5b0c4`
- [x] **N03.C2** 🔖 COMMIT `feat(vendor): notify sellers and staff about kyc events` · hash: `34e27cf`
- [x] **N03.C3** 🔖 COMMIT `feat(promotion): raise staff alert when a coin balance is tampered` · hash: `4dba62b`
- [x] **N03.C4** 🔖 COMMIT `feat(order): notify buyer and shops when a cod checkout is placed` (bỏ nếu N03.09 `[-]`) · hash: `436d7ef`

## N04 — API hộp thư `/me/notifications`

- [x] **N04.01** `NotificationRepository`
- [x] **N04.02** `PlatformNotificationRepository` / `UserPlatformNotificationReadRepository`
- [x] **N04.03** `AdminInboxQueryService`
- [x] **N04.04** `InboxCursor` (D8)
- [x] **N04.05** `InboxQueryService.page(me, category, cursor, limit)`
- [x] **N04.06** `InboxReadService`
- [x] **N04.07** `MeNotificationController`
- [x] **N04.08** `MeNotificationIT`
- [x] **N04.09** `AdminInboxIT`
- [x] **N04.10** `NotificationRealtimeIT`
- [x] **N04.C1** 🔖 COMMIT `feat(notification): serve merged inbox with cursor paging and read marks` · hash: `60d7620`

## N05 — Nối web: hub realtime + chuông + hộp thư (UC-NOT-01)

- [ ] **N05.01** `shared/realtime/stomp.ts`
- [ ] **N05.02** `shared/realtime/realtimeHub.ts` (D17)
- [ ] **N05.03** `notifications/infrastructure`
- [ ] **N05.04** `notifications.api.ts`
- [ ] **N05.05** `application/useNotifications.ts`
- [ ] **N05.06** UI
- [ ] **N05.07** `error-codes.ts` + `vi.json` / `en.json`
- [ ] **N05.08** Test
- [ ] **N05.C1** 🔖 COMMIT `refactor(web): share one stomp connection across realtime channels` · hash: `…`
- [ ] **N05.C2** 🔖 COMMIT `feat(web): connect notification bell and inbox to the real api with live updates` · hash: `…`

## N06 — API quản trị thông báo sàn (UC-NOT-05)

- [ ] **N06.01** `PlatformNotification`
- [ ] **N06.02** `PlatformAudience` (D1, D3)
- [ ] **N06.03** Request / kiểm trường
- [ ] **N06.04** `PlatformNotificationAdminService.create`
- [ ] **N06.05** `update`
- [ ] **N06.06** `delete`
- [ ] **N06.07** `PlatformNotificationQueryService`
- [ ] **N06.08** `AdminPlatformNotificationController`
- [ ] **N06.09** `AdminPlatformNotificationIT`
- [ ] **N06.10** `PlatformNotificationSecurityIT`
- [ ] **N06.C1** 🔖 COMMIT `feat(notification): let admins compose, schedule and delete platform notifications` · hash: `…`

## N07 — Phát & đẩy realtime thông báo sàn

- [ ] **N07.01** `PlatformNotificationDispatcher`
- [ ] **N07.02** `PlatformNotificationDispatchJob` (D16)
- [ ] **N07.03** `PlatformNotificationDispatchJobIT`
- [ ] **N07.04** `NotificationRealtimeIT` (bổ sung)
- [ ] **N07.C1** 🔖 COMMIT `feat(notification): dispatch scheduled broadcasts and push them to online recipients` · hash: `…`

## N08 — Nối web màn Admin "Thông báo sàn" + hiển thị trong hộp thư

- [ ] **N08.01** `admin-marketing/infrastructure/marketing.api.ts` (nhóm thông báo sàn)
- [ ] **N08.02** Modal soạn
- [ ] **N08.03** Bảng
- [ ] **N08.04** Hộp thư người dùng
- [ ] **N08.05** E2E mock
- [ ] **N08.C1** 🔖 COMMIT `feat(admin-ui): connect platform notification management to the real api` · hash: `…`

## N09 — Kiểm thử & chạy thật

- [ ] **N09.01** `docs/api-tests/notifications.http`
- [ ] **N09.02** `docs/kiem-thu/kich-ban-2026-11-26-thong-bao.md`
- [ ] **N09.03** Chạy thật
- [ ] **N09.04** Đo nhanh
- [ ] **N09.C1** 🔖 COMMIT `docs(notification): add notification api tests and real-run scenarios` · hash: `…`

## N10 — Hoàn thiện use case & tick tiến độ

- [ ] **N10.01** OpenAPI
- [ ] **N10.02** `docs/web-backend-handoff.md`
- [ ] **N10.03** Usecase.md
- [ ] **N10.04** `CHECKLIST_TIEN_DO_UC.md`
- [ ] **N10.05** `BAO_CAO_UC_DOC_LAP.md`
- [ ] **N10.06** `Quản Lý UseCase.xlsx`
- [ ] **N10.07** README ngày (mục 7)
- [ ] **N10.C1** 🔖 COMMIT `docs(notification): record in-app and platform notifications as implemented` · hash: `…`
- [ ] **N10.08** Mở PR `feature/notification-inbox` (N02–N05) rồi `feature/notification-platform-broadcast` (N06–N10) vào `develop`; mô tả PR theo `.github/pull_request_template.md`, tiêu đề đúng chuẩn commit.

---

## Chốt plan — tick tiến độ use case (người phụ trách: Nguyễn Ngọc Thiện)

Làm **ngay khi xong plan** (hoặc xong từng use case), trong [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../../docs/Document/CHECKLIST_TIEN_DO_UC.md) — cách tick xem mục 0 của file đó. Use case chỉ ✅ khi đủ 5 lớp GD / BE / Test / Nối / Chạy (UC chạy nền: đủ các ô không phải `—`).

- [ ] `UC-NOT-01` Thông báo trong ứng dụng thời gian thực (WebSocket) — tick GD / BE / Test / Nối / Chạy, Trạng thái **✅ Đã triển khai**, cột **Phụ trách = Nguyễn Ngọc Thiện**
- [ ] `UC-NOT-05` Admin phát thông báo toàn sàn (Broadcast Platform Notification) — tick GD / BE / Test / Nối / Chạy, Trạng thái **✅ Đã triển khai**, cột **Phụ trách = Nguyễn Ngọc Thiện**
- [ ] Cập nhật số ở mục 1 *Tổng quan* (Đã triển khai / Đang triển khai / Chưa có backend) + bảng *Tiến độ theo phân hệ* (phân hệ 10 (Thông báo))
- [ ] Thêm một dòng vào mục 5 *Nhật ký cập nhật* của checklist (ngày · use case · việc đã làm · mã commit / PR)
- [ ] Bảng *Thành viên & phân công* (đầu checklist **và** đầu `BAO_CAO_UC_DOC_LAP.md`) — cột *Hiện trạng* của Nguyễn Ngọc Thiện ghi các use case vừa xong
- [ ] `BAO_CAO_UC_DOC_LAP.md` — bảng trạng thái của cụm / dòng use case (✅ + *Còn lại*), dòng *Phụ trách: Nguyễn Ngọc Thiện*
- [ ] `Quản Lý UseCase.xlsx` — cột *Hiện trạng mã nguồn* = "Đã triển khai", cột AA *Người phụ trách* = "Nguyễn Ngọc Thiện", sheet *Thành viên* (sửa XML, chuỗi `&#NNNN;`)
- [ ] Câu *Hiện trạng* của từng use case trong `Usecase.md`
- [ ] Tick dòng **[1] Thông báo & realtime** ở bảng mục 7 của [README](./README.md)

## Ghi chú

Ghi ở đây: lý do bỏ qua task (`[-]`), chỗ làm khác plan, số đo, số migration thật, cách chạy thật + số ca đạt, việc tồn chuyển sang plan khác.

| Task | Ghi chú |
|---|---|
| N04.01–N04.03 | **Khác plan:** truy vấn hộp thư viết bằng `NamedParameterJdbcTemplate` trong `InboxQueryService` / `InboxReadService` (trộn 3 bảng, `INSERT … SELECT … ON CONFLICT`) thay vì thêm phương thức vào 3 repository JPA — điều kiện "thông báo sàn hiển thị cho người xem" viết một lần ở `InboxReadModel.PLATFORM_VISIBLE`, dùng chung cho đếm / phân trang / đánh dấu đọc |
| N04.04–N04.05 | `InboxCursor` = Base64URL `at(micro giây):nguồn:id`, khóa sắp toàn phần giảm dần (cùng thời điểm: `an_` > `pn_` > `ntf_`); điều kiện SQL theo hạng nguồn (`cursorClause`). `body` của `pn_` = văn bản thuần 160 ký tự, `content_html` = HTML đã lọc; thời gian trả +07:00 |
| N04.08–N04.10 | `MeNotificationIT` 6 ca (trộn 5 dòng qua 3 trang không trùng / sót, lọc nhóm + 422, đối tượng `ALL/CUSTOMER/VENDOR/SPECIFIC_USERS` + người đăng ký sau không thấy + Admin không nhận `pn_`, IDOR 404, `is_read:false` 422, mark-all-read theo nhóm), `AdminInboxIT` (Moderator / Finance / Admin / khách, 404 khi thiếu quyền, đọc chung), `NotificationRealtimeIT` 3 ca STOMP thật (chỉ chủ nhận + `unread_count`, `NOTIFICATIONS_READ` đồng bộ tab 2, cảnh báo nhân viên chỉ tới người có quyền). Lớp hỗ trợ dọn `platform_notifications` / `admin_notifications` trước mỗi ca (bảng không gắn người). MockMvc trả ISO-8859-1 mặc định → đọc `getContentAsString(UTF_8)`. Cổng: **235 unit + 379 IT** xanh |
| Môi trường | Qua ngày Docker Desktop tự tắt → bật lại trước khi chạy IT |
| N03.02 | **Khác plan:** `ModuleBoundaryTest` cấm module khác chạm `notification..domain..` → thêm 2 enum công khai ở tầng `application`: `NotificationKind` (mirror `NotificationType`) và `StaffAlertKind` (mirror `AdminNotificationType`); `NotificationKindTest` bảo đảm luôn cùng tập giá trị. `NotificationCommand.type` dùng `NotificationKind` |
| N03.03 | **Khác plan:** trong listener `@TransactionalEventListener(AFTER_COMMIT)` đăng ký thêm "sau commit" sẽ không bao giờ chạy (Spring đã chụp danh sách synchronization) → facade có 2 cửa: `send` / `sendMany` / `upsertChat` (gọi TRONG transaction nghiệp vụ — ghi sau commit, rollback thì không) và `sendNow` / `sendManyNow` (gọi từ listener sau commit). Cả hai ghi trong `REQUIRES_NEW`, lỗi chỉ log. Dòng `IN_APP` lưu là đã tới hộp thư → ghi thẳng `delivery_status = SENT` (không qua `PENDING` → `markSent` như plan) |
| N03.05 | `AdminInboxFacade.raise` / `raiseNow`; người nhận realtime = `OnlineUsers.allOnline()` ∩ nhân viên có quyền (`InboxReadModel.staffAllowed` — SQL `users` + `staff_permissions`, ADMIN luôn có). Luật quyền / đích ở `AdminInboxRules` |
| N03.06–N03.09 | Listener / cài cổng: `AuthEventNotificationListener` (NEW_DEVICE_LOGIN), `vendor/KycAdminNotifierImpl` + `KycInAppNotificationListener` (KYC_APPROVED / KYC_REJECTED + `metadata.event`), `promotion/CoinAlertNotifier` (**`raiseNow`** — `CoinLedgerService` ném lỗi rồi rollback ngay sau khi báo), `order/CheckoutNotificationListener` (chỉ COD — 1 thông báo người mua + 1 / shop). Đều là file mới, không sửa file của C2 / B4 |
| N03.10 | `NotificationFacadeIT` 6 ca (ghi sau commit, rollback không ghi, không transaction ghi ngay, FK lỗi không hỏng nghiệp vụ gốc, gộp chat theo hội thoại tới khi đọc, `raiseNow` vs `raise` khi rollback), `KycNotificationIT` 2 ca, `CheckoutNotificationIT` 2 ca (COD 2 shop, VNPAY chưa báo), `NotificationCategoryTest`, `NotificationKindTest`; `ModuleBoundaryTest` xanh. Cổng: **232 unit + 369 IT** xanh |
| N02.03 | API 4.1 đã đặt tên purpose là **`NOTIFICATION_IMAGE`** (web `upload.rules.ts` + `PlatformNotificationsPage.tsx` cũng dùng tên này); LUU_TRU ghi `PLATFORM_NOTICE_IMAGE`. Theo luật ưu tiên (hình dạng request lấy theo API) dùng `NOTIFICATION_IMAGE`; đã đổi tên trong plan / README / progress. API lịch sử phiên bản 1.8 |
| N02.05 | `ErrorCode.PLATFORM_NOTIFICATION_DISPATCHED` (`GNE-NOT-4091`), `PLATFORM_NOTIFICATION_RECIPIENTS` (`GNE-NOT-4221`); `UploadPurpose.NOTIFICATION_IMAGE` (quyền `MANAGE_PLATFORM_NOTIFICATION`); `UserChannel.NOTIFICATIONS`, `PLATFORM_TOPIC`, `sendToTopicAfterCommit` (tách `runAfterCommit` dùng chung) |
| N02.07 | `common/html/HtmlSanitizer` (jsoup `Cleaner` + danh sách thẻ D13; `a` chỉ `https:` / đường dẫn tương đối, thêm `rel` + `target`; `img` chỉ từ tiền tố `content-assets`) + `HtmlSanitizerConfig` (tiền tố lấy từ `ObjectStorage.publicUrl`) + `plainPreview`. `HtmlSanitizerTest` 7 ca, `OnlineUsersTest` 1 ca. Cổng: `./mvnw clean verify` **228 unit + 359 IT** xanh |
| N01.09 | PR #112 `feature/b56-shared-schema` → `develop` (https://github.com/qu2nishere/genie-multivendor-platform/pull/112). Không chờ duyệt: nhánh `feature/notification-inbox` **xếp chồng** lên `feature/b56-shared-schema` (như C2) — PR #112 merge xong thì `git rebase develop` |
| N01.01 | ENTITY v1.6: 10.2 (`dispatchedAt`, `isDispatched/isScheduled/dispatch`), 11.2 (`isAuto`, cách suy `DELIVERED/SEEN`), Phụ lục A (`CHAT_MESSAGE` + bảng nhóm `category`, 7 `AuditAction`), bảng đếm enum (17 / 47), 13.1 (9 index), E.10, CHANGELOG; đồng bộ `GIAI_NGHIA_ENTITY.md`, `Diagram07`, `Diagram10` |
| N01.07 | `PlatformNotification.dispatch()` kiểm tham số **trước** khi gán (viết test phát hiện: `dispatch(1, null)` từng gán `reachCount` rồi mới ném lỗi) |
| N01.08 | Thêm `SchemaValidationIT.checkConstraintsAcceptEveryEnumValue`: đọc `pg_get_constraintdef` của `ck_system_logs_action_type`, `ck_notifications_type`, `ck_messages_type` và khẳng định chứa mọi giá trị enum Java — migration sau quên chép giá trị là đỏ ngay. `AuditActionCategoryTest` thêm 7 giá trị CONFIG; `PlatformNotificationTest` 5 ca. Cổng: `./mvnw clean verify` (JDK 21) **BUILD SUCCESS — 220 unit + 359 IT** (1 bỏ qua: `SearchPerformanceIT` chỉ chạy với `-Dperf`) |
| Ghi nhớ N06 / V04 | Web màn Nhật ký (`admin-system`) có nhãn + tông màu theo từng `AuditAction` (`actionTone`, khóa `admin-system.audit.actions.*`) — khi ghi `PLATFORM_NOTIFICATION_*` / `VOUCHER_*` phải thêm nhãn vi / en + tông màu |
| N00.01 | `git config user.name` = `Thien211105` (email cấu hình sẵn) — đúng |
| N00.03 | Nhánh `feature/b56-shared-schema` tách từ `develop` @ `e10bb37` (đã gồm commit plan) |
| N00.04 | `origin/develop` mới tới `V25`; không nhánh remote nào có `V26+` (quét `git ls-tree` mọi nhánh 2026-11-26) → giữ **`V26__b5_b6_notification_chat_voucher.sql`** |
| N00.05 | `CheckoutCreatedEvent(sessionId, userId, orderIds, paymentMethod, grandTotal)` đủ cho D11e (shop lấy từ `orders.vendor_id` trong module order). `KycDecidedEvent.status` ∈ `VERIFIED`, `REJECTED`, `SUPPLEMENT_REQUESTED` (phát ở `KycDecisionService`, `KycReviewService`, `KycSupplementExpiryJob`). `NewDeviceLoginEvent(userId, email, deviceName, ipAddress, loginAt)`. `OrderStatusChangedEvent` / `OrderPaidEvent` vẫn `// TODO` (C3 / C4) |
| N00.06 | Admin › Thông báo sàn: DTO mock (`marketing.dto.ts` `PlatformNotificationDto`, `…DetailDto`) **đã khớp API 11.17** (`code`, `content_preview`, `is_scheduled`, `read_count`, `reach_count`, `summary.*`, `content_html`, `recipient_emails`) → N08 chủ yếu bật cờ + xử lý lỗi. Hộp thư (`notifications.dto.ts`) lệch API 3.12: `type` chữ thường `order/promo/system/chat` + `icon/tint/bg`, `read_at` thay `is_read`, `link.action` thay `link.type`, `is_platform` → N05.03 viết lại DTO / mapper |
| Môi trường | Máy có JDK 24 (mặc định — Lombok lỗi `TypeTag :: UNKNOWN`) và JDK 21 ở `~/.jdks/ms-21.0.10` → chạy Maven với `JAVA_HOME=~/.jdks/ms-21.0.10`. Bật Docker Desktop trước (Testcontainers). Không đứng trong `server-spring/target` khi `clean` (Windows khóa thư mục) |

## Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-26 | Lập plan + progress (81 task, 17 commit, 11 giai đoạn) — chưa bắt đầu làm | — |
| 2026-11-26 | N00 (chốt D1–D20, kiểm `V26` trống, đọc sự kiện + DTO mock) và N01 (ENTITY v1.6, migration `V26`, entity / enum, test ràng buộc CHECK ↔ enum); verify xanh 220 unit + 359 IT | `51f57ac` `79c110d` |
| 2026-11-26 | N02: API_SPEC 3.12 / 4.1 / 11.17 / 13 + Usecase UC-NOT-01 / 05; jsoup; mã lỗi `GNE-NOT-*`, `NOTIFICATION_IMAGE`, `OnlineUsers`, `HtmlSanitizer`; verify xanh 228 unit + 359 IT. Nhánh `feature/notification-inbox` xếp chồng trên `feature/b56-shared-schema` | `5bc76c5` `88636db` `f8bf25b` |
| 2026-11-26 | N03: `NotificationFacade` (ghi sau commit + đẩy WebSocket), `AdminInboxFacade`, read-model đếm chưa đọc 3 nguồn, nguồn thông báo KYC / thiết bị mới / lệch Điểm / đặt COD; verify xanh 232 unit + 369 IT | `ba5b0c4` `34e27cf` `4dba62b` `436d7ef` |
| 2026-11-27 | N04: `GET /me/notifications` (gộp `ntf_` / `pn_` / `an_`, cursor), `PATCH /{id}`, `POST /mark-all-read`, gói tin `NOTIFICATIONS_READ`; verify xanh 235 unit + 379 IT | `60d7620` |
