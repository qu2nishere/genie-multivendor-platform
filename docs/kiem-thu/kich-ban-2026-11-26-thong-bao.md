# Kịch bản kiểm thử — Thông báo thời gian thực (UC-NOT-01) + Admin phát thông báo toàn sàn (UC-NOT-05)

> Plan / progress: [../../plan/2026-11-26/server-spring/plan_thong_bao_realtime.md](../../plan/2026-11-26/server-spring/plan_thong_bao_realtime.md) · [progress](../../plan/2026-11-26/server-spring/progress_thong_bao_realtime.md). Phụ trách: **Nguyễn Ngọc Thiện**.
> Chuẩn bị môi trường: [../huong-dan-chay-local.md](../huong-dan-chay-local.md) — Docker (postgres, redis, silo, mailpit), backend JDK 21 profile `local` + dữ liệu mẫu, web bản build cổng 4321.
> Mật khẩu tài khoản seed: `Genie@123`. API thử tay: [../api-tests/notifications.http](../api-tests/notifications.http).

## Cấu hình bắt buộc

| Nơi | Giá trị |
|---|---|
| `web/.env` | `PUBLIC_USE_MOCK=true` và `PUBLIC_REAL_API` có thêm `notifications,platform-notifications` (cùng các nhóm đã có) |
| Backend | `PUBLIC_S3_URL` phải là địa chỉ **trình duyệt mở được** (máy dev: `http://localhost:9000`). Nếu `.env` gốc đặt `http://host.docker.internal:9000` (cho AI Worker trong Docker) thì link tải ảnh lên Silo không mở được từ trình duyệt → chạy backend với `PUBLIC_S3_URL=http://localhost:9000` |
| Migration | `V26__b5_b6_notification_chat_voucher.sql` đã áp (backend tự chạy Flyway) |

**Tài khoản:** khách thuần = tài khoản **mới** đăng ký (OTP qua Mailpit `http://localhost:8025`); chủ shop = `mai.tran@gmail.com`; Admin = `yen.dh@genie.example`; nhân viên Moderator = `phat.nt@genie.example`; Finance Officer = `linh.tm@genie.example`.

## Ca kiểm thử

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC-NOT-00 | Đăng ký khách mới (OTP Mailpit); đăng nhập khách (2 tab), chủ shop, Admin, Moderator | Cả 4 vai vào đúng màn; WebSocket kết nối (DevTools › Network › WS `/ws` CONNECTED) |
| TC-NOT-01 | Khách đang mở trang chủ; đăng nhập cùng tài khoản trên **thiết bị khác** (trình duyệt điện thoại) | Tab đang mở hiện toast "Đăng nhập từ thiết bị mới", chuông +1 không cần tải lại |
| TC-NOT-02 | Admin › Thông báo sàn › Tạo thông báo: đối tượng **Tất cả**, tiêu đề, nội dung, ảnh → Phát → Xác nhận | Toast "Đã phát thông báo."; khách và chủ shop nhận toast + âm "ting" ngay; chuông +1 ở mọi tab; ảnh lưu `content-assets/notice/{id}/…` |
| TC-NOT-03 | Khách › Tài khoản › Thông báo › tab Hệ thống → bấm thông báo sàn | Modal chi tiết có ảnh + nội dung; thông báo thành đã đọc; Admin tải lại bảng thấy "Đã đọc" = 1 |
| TC-NOT-04 | Admin phát đối tượng **Chỉ Vendor** | Chủ shop nhận; khách thuần **không** nhận (cả realtime lẫn hộp thư) |
| TC-NOT-05 | Admin phát **Người dùng cụ thể** với 1 email đúng + 1 email không tồn tại | Ô email báo lỗi liệt kê đúng email lạ (`GNE-NOT-4221`); sửa còn email đúng → chỉ người đó nhận |
| TC-NOT-06 | Khách bấm "Đánh dấu tất cả đã đọc" ở tab 1 | Chuông ở tab 2 về 0 ngay (`NOTIFICATIONS_READ`) |
| TC-NOT-07 | Chèn thẳng CSDL một thông báo sàn có `<img onerror>`, `javascript:`, `<script>` (vượt lớp lọc server) → khách mở modal | Không mã nào chạy; DOM modal không còn `onerror` / `javascript:` / `<script>` (lớp lọc phía client) |
| TC-NOT-08 | Chèn `admin_notifications` loại `USER_APPROVAL` + `WITHDRAW_REQUEST` → Moderator mở chuông Admin Console | Thấy hồ sơ KYC mới, **không** thấy yêu cầu rút tiền (thiếu `PROCESS_PAYOUT`) |
| TC-NOT-09 | Khách mất mạng 3 giây rồi có lại → Admin phát **Chỉ Customer** | Kết nối tự nối lại (2 → 4 giây); khách vẫn nhận toast |
| TC-NOT-10 | Admin tạo thông báo **hẹn giờ** +2 phút | Bảng hiện "Đã hẹn giờ", `reach_count` = 0; tới giờ cron (30 giây / lần) phát → khách nhận toast |
| TC-NOT-11 | Admin xóa thông báo **đã phát** | Hộp xác nhận cảnh báo "gỡ khỏi hộp thư của mọi người nhận"; khách tải lại không còn thấy |
| TC-NOT-12 | Khổ 360 px, trang Tài khoản › Thông báo với 9 theme (`?theme=genie…sleeve`) | Hiển thị đủ tab / danh sách, không lỗi console |
| TC-NOT-13 | Gọi API theo `notifications.http` (bước 9, 15–19) | `422` / `409 GNE-NOT-4091` / `422 GNE-NOT-4221` / `403` / `404` đúng như ghi chú |

## Kết quả chạy thật (2026-11-27)

Chromium điều khiển bằng Playwright, 6 ngữ cảnh trình duyệt song song (khách 2 tab + thiết bị thứ hai giả UA iPhone, chủ shop, Admin, Moderator), hệ thống thật: web bản build `PUBLIC_REAL_API=…,notifications,platform-notifications` + backend `local` (JDK 21, `PUBLIC_S3_URL=http://localhost:9000`) + Postgres + Redis + Silo + Mailpit.

| Mã | Kết quả | Ghi chú |
|---|:---:|---|
| TC-NOT-00 | Đạt | Đăng ký thật qua OTP Mailpit |
| TC-NOT-01 | Đạt | Chuông 1 → 2 ngay khi đăng nhập trên "iPhone" |
| TC-NOT-02 | Đạt | Ảnh `notice/64/…webp`; khách + shop nhận toast; tab 2 cũng tăng |
| TC-NOT-03 | Đạt | Bảng Admin: đã đọc 1 |
| TC-NOT-04 | Đạt | |
| TC-NOT-05 | Đạt | |
| TC-NOT-06 | Đạt | |
| TC-NOT-07 | Đạt | `window.__xss` không đổi, DOM sạch |
| TC-NOT-08 | Đạt | |
| TC-NOT-09 | Đạt | |
| TC-NOT-10 | Đạt | Cron phát đúng giờ hẹn |
| TC-NOT-11 | Đạt | |
| TC-NOT-12 | Đạt | 9 theme, 0 lỗi console |
| TC-NOT-13 | Đạt | Đã kiểm bằng IT (`MeNotificationIT`, `AdminPlatformNotificationIT`, `PlatformNotificationSecurityIT`) |

**13 / 13 ca chạy trên trình duyệt đạt.** Lỗi phát hiện trong lúc chạy và đã sửa: (1) form soạn đặt lại giá trị **sau** lần vẽ đầu → chữ gõ ngay khi vừa mở hộp thoại bị xóa — chuyển sang `useLayoutEffect` (`e856c14`); (2) môi trường: `PUBLIC_S3_URL` của `.env` máy dev không mở được từ trình duyệt (ghi ở *Cấu hình bắt buộc*).

**Số đo (máy dev):** `GET /me/notifications?limit=20` p50 10 ms / p95 35 ms (55 lượt); phát `ALL` tới 8 kết nối STOMP đang mở: nhận đủ 8 trong ≤ 17 ms tính từ lúc gửi POST (đăng nhập bị giới hạn 10 lần / phút / IP nên không mở được 200 kết nối trong một lượt như plan dự kiến).
