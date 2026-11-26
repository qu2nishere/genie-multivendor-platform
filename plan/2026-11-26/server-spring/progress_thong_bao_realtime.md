# PROGRESS — Thông báo trong ứng dụng thời gian thực (UC-NOT-01) + Admin phát thông báo toàn sàn (UC-NOT-05)

| | |
|---|---|
| **Plan** | [plan_thong_bao_realtime.md](./plan_thong_bao_realtime.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-26 |
| **Tổng** | 81 task + 17 commit, 11 giai đoạn |
| **Nhánh** | `feature/b56-shared-schema` (N01) → `feature/notification-inbox` (N02–N05) → `feature/notification-platform-broadcast` (N06–N10) |
| **Phụ trách** | **Nguyễn Ngọc Thiện** — lập plan và thực hiện (nhận 2026-11-26) |
| **Người commit** | danh tính git đã cấu hình trong repo (`Thien211105`) — không `--author`, không `--no-verify`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-26 — mới lập plan (0 / 98) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục *Ghi chú*.
- Task `COMMIT` chỉ tick sau khi qua *Cổng kiểm tra* (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`. Thông điệp đã ghi sẵn — tiếng Anh, chữ thường, đúng `commitlint.config.cjs`; có mã Jira thì thêm ` [#GNE-<số>]` cuối.
- Sau mỗi buổi làm: cập nhật cột *Xong* của bảng Tổng quan, dòng *Cập nhật gần nhất*, một dòng *Nhật ký* cuối trang.
- Cột *Chạy* của use case chỉ tick khi đã chạy thật trên giao diện (Chromium điều khiển bằng Playwright, hệ thống thật) và đạt hết ca của kịch bản — ghi cách chạy + số ca vào *Ghi chú*.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | **Đối tượng nhận** của `target` | ☐ | |
| D2 | **Đánh dấu "đã phát"** — bảng không có cột, thông báo hẹn giờ không biết cron đã phát chưa | ☐ | |
| D3 | **`reach_count`** chốt lúc nào | ☐ | |
| D4 | **Đẩy realtime thông báo sàn** — `/topic/platform` tới mọi người kể cả người không thuộc đối tượng | ☐ | |
| D5 | **Gói tin realtime** trên `/user/queue/notifications` | ☐ | |
| D6 | **Nhóm hiển thị (`category`)** từ `NotificationType` — và loại cho tin nhắn chat | ☐ | |
| D7 | **Hộp thư nhân viên** `admin_notifications` (không có `user_id`) | ☐ | |
| D8 | **Phân trang gộp 3 nguồn** (`ntf_`, `pn_`, `an_`) | ☐ | |
| D9 | **Đánh dấu đã đọc** | ☐ | |
| D10 | **Cách module khác tạo thông báo** | ☐ | |
| D11 | **Nguồn thông báo nối ngay trong plan này** (có sự kiện thật) | ☐ | |
| D12 | **Nội dung tiêu đề / thân** đa ngôn ngữ | ☐ | |
| D13 | **Lọc HTML** `content_html` | ☐ | |
| D14 | **Ảnh banner thông báo** | ☐ | |
| D15 | **Sửa / xóa thông báo sàn** | ☐ | |
| D16 | **Cron phát hẹn giờ** | ☐ | |
| D17 | **Web: một kết nối WebSocket cho cả ứng dụng** | ☐ | |
| D18 | **Âm báo & toast & nhiều tab** | ☐ | |
| D19 | **Cờ nối web** | ☐ | |
| D20 | **Ngoài phạm vi** | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| N00 | Chuẩn bị | 6 | 0 | 0 / 6 |
| N01 | Migration chung `V26` (nhánh `feature/b56-shared-schema`, PR riêng — merge trước) | 9 | 2 | 0 / 11 |
| N02 | Tài liệu API & hằng số dùng chung (nhánh `feature/notification-inbox`) | 7 | 3 | 0 / 10 |
| N03 | Tạo thông báo cá nhân & hộp thư nhân viên (UC-NOT-01) | 10 | 4 | 0 / 14 |
| N04 | API hộp thư `/me/notifications` | 10 | 1 | 0 / 11 |
| N05 | Nối web: hub realtime + chuông + hộp thư (UC-NOT-01) | 8 | 2 | 0 / 10 |
| N06 | API quản trị thông báo sàn (UC-NOT-05) | 10 | 1 | 0 / 11 |
| N07 | Phát & đẩy realtime thông báo sàn | 4 | 1 | 0 / 5 |
| N08 | Nối web màn Admin "Thông báo sàn" + hiển thị trong hộp thư | 5 | 1 | 0 / 6 |
| N09 | Kiểm thử & chạy thật | 4 | 1 | 0 / 5 |
| N10 | Hoàn thiện use case & tick tiến độ | 8 | 1 | 0 / 9 |
| **Tổng** | | **81** | **17** | **0 / 98** |

## N00 — Chuẩn bị

- [ ] **N00.01** Kiểm danh tính git
- [ ] **N00.02** Chốt D1–D20
- [ ] **N00.03** Cập nhật `develop`
- [ ] **N00.04** Kiểm số migration
- [ ] **N00.05** Đọc chữ ký sự kiện
- [ ] **N00.06** Đọc màn mock

## N01 — Migration chung `V26` (nhánh `feature/b56-shared-schema`, PR riêng — merge trước)

- [ ] **N01.01** Tài liệu trước (TS 7.3)
- [ ] **N01.02** `V26__b5_b6_notification_chat_voucher.sql`
- [ ] **N01.03** Khối (b) hộp thư
- [ ] **N01.04** Khối (c) chat
- [ ] **N01.05** Khối (d) voucher
- [ ] **N01.06** Khối (e) nhật ký
- [ ] **N01.07** Entity & enum
- [ ] **N01.08** Test
- [ ] **N01.C1** 🔖 COMMIT `docs(db): add schema changes for notifications, chat and vouchers` · hash: `…`
- [ ] **N01.C2** 🔖 COMMIT `feat(db): add shared schema for notification, chat and voucher clusters` · hash: `…`
- [ ] **N01.09** Mở PR `feature/b56-shared-schema` → `develop` (mô tả: chỉ thêm cột / index / giá trị CHECK, tương thích ngược); sau khi merge mới tạo nhánh N02.

## N02 — Tài liệu API & hằng số dùng chung (nhánh `feature/notification-inbox`)

- [ ] **N02.01** API_SPEC 3.12
- [ ] **N02.02** API_SPEC 13
- [ ] **N02.03** API_SPEC 11.17
- [ ] **N02.04** Usecase.md
- [ ] **N02.05** `ErrorCode` thêm `GNE-NOT-4091`, `GNE-NOT-4221` (khối `// ===== Thông báo`); `UploadPurpose.PLATFORM_NOTICE_IMAGE` (D14); `UserChannel.NOTIFICATIONS` + `sendToTopicAfterCommit`.
- [ ] **N02.06** `common/realtime/OnlineUsers`
- [ ] **N02.07** `HtmlSanitizer` + phụ thuộc `jsoup` (D13)
- [ ] **N02.C1** 🔖 COMMIT `docs(notification): specify inbox merge, realtime events and broadcast rules` · hash: `…`
- [ ] **N02.C2** 🔖 COMMIT `build(deps): add jsoup for server-side html sanitizing` · hash: `…`
- [ ] **N02.C3** 🔖 COMMIT `feat(common): add notification error codes, notice image purpose and online users` · hash: `…`

## N03 — Tạo thông báo cá nhân & hộp thư nhân viên (UC-NOT-01)

- [ ] **N03.01** `NotificationCategory` + `of(NotificationType)` (D6)
- [ ] **N03.02** `NotificationCommand`, `NotificationLink` (`type` ∈ API 3.12 `link.type` + `CONVERSATION`, `id`), `NotificationView` (phần tử 3.12: `id` có tiền tố, `category`, `type`, `title`, `body`, `image_url`, `created_at`, `is_read`, `link`).
- [ ] **N03.03** `NotificationFacade.send(cmd)`
- [ ] **N03.04** `NotificationFacade.upsertChat(userId, conversationId, title, body)`
- [ ] **N03.05** `AdminInboxFacade.raise(type, title, content, referenceUrl)`
- [ ] **N03.06** `AuthEventNotificationListener` (trong notification)
- [ ] **N03.07** `vendor/kyc/application/KycAdminNotifierImpl` (cài cổng có sẵn) → `AdminInboxFacade.raise(USER_APPROVAL, "Hồ sơ KYC mới: {shop}", …, "/admin/kyc")`; `KycNotificationListener` (`KycDecidedEvent` AFTER_COMMIT) → chủ shop `KYC_APPROVED` / `KYC_REJECTED` (`metadata.event` = `APPROVED / REJECTED / SUPPLEMENT_REQUESTED`), `link.type = KYC`.
- [ ] **N03.08** `promotion/coin/application/CoinAlertNotifier` (cài `CoinAlertPort`) → `SYSTEM_ALERT` "Lệch số dư Điểm của người dùng #{id}" `referenceUrl = /admin/users?q={id}`.
- [ ] **N03.09** (D11e
- [ ] **N03.10** Test
- [ ] **N03.C1** 🔖 COMMIT `feat(notification): create in-app notifications after commit and push them over websocket` · hash: `…`
- [ ] **N03.C2** 🔖 COMMIT `feat(vendor): notify sellers and staff about kyc events` · hash: `…`
- [ ] **N03.C3** 🔖 COMMIT `feat(promotion): raise staff alert when a coin balance is tampered` · hash: `…`
- [ ] **N03.C4** 🔖 COMMIT `feat(order): notify buyer and shops when a cod checkout is placed` (bỏ nếu N03.09 `[-]`) · hash: `…`

## N04 — API hộp thư `/me/notifications`

- [ ] **N04.01** `NotificationRepository`
- [ ] **N04.02** `PlatformNotificationRepository` / `UserPlatformNotificationReadRepository`
- [ ] **N04.03** `AdminInboxQueryService`
- [ ] **N04.04** `InboxCursor` (D8)
- [ ] **N04.05** `InboxQueryService.page(me, category, cursor, limit)`
- [ ] **N04.06** `InboxReadService`
- [ ] **N04.07** `MeNotificationController`
- [ ] **N04.08** `MeNotificationIT`
- [ ] **N04.09** `AdminInboxIT`
- [ ] **N04.10** `NotificationRealtimeIT`
- [ ] **N04.C1** 🔖 COMMIT `feat(notification): serve merged inbox with cursor paging and read marks` · hash: `…`

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

## Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-26 | Lập plan + progress (81 task, 17 commit, 11 giai đoạn) — chưa bắt đầu làm | — |
