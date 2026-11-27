# Kịch bản kiểm thử — Chat Khách ↔ Shop thời gian thực (UC-SOC-01) · Thẻ sản phẩm / ảnh / đơn trong chat (UC-SOC-02) · Tự động trả lời (UC-VND-06)

> Plan / progress: [../../plan/2026-11-26/server-spring/plan_chat_khach_shop.md](../../plan/2026-11-26/server-spring/plan_chat_khach_shop.md) · [progress](../../plan/2026-11-26/server-spring/progress_chat_khach_shop.md). Phụ trách: **Nguyễn Ngọc Thiện**.
> Chuẩn bị môi trường: [../huong-dan-chay-local.md](../huong-dan-chay-local.md) — Docker (postgres, redis, silo, mailpit), backend JDK 21 profile `local` + dữ liệu mẫu, web bản build cổng 4321.
> Mật khẩu tài khoản seed: `Genie@123`. API thử tay: [../api-tests/chat.http](../api-tests/chat.http).

## Cấu hình bắt buộc

| Nơi | Giá trị |
|---|---|
| `web/.env` | `PUBLIC_USE_MOCK=true` và `PUBLIC_REAL_API` có thêm `chat` (cùng `notifications`, `platform-notifications`, `product`, `upload`… đã có) |
| Backend | `PUBLIC_S3_URL=http://localhost:9000` (địa chỉ trình duyệt mở được — link tải ảnh lên Silo và link ký xem ảnh chat) |
| Silo | bucket riêng tư `chat-media` (`deploy/silo/init.sh` đã tạo) |
| Migration | `V26__b5_b6_notification_chat_voucher.sql` (`messages.is_auto`, index hội thoại, `NotificationType.CHAT_MESSAGE`) |

**Tài khoản:** khách thuần = tài khoản **mới** đăng ký (OTP qua Mailpit `http://localhost:8025`); chủ shop TechZone (`shop_id = 1`) = `mai.tran@gmail.com`; nhân viên = `phat.nt@genie.example`. Đơn giữa khách mới và TechZone (cho thẻ đơn) tạo bằng SQL ở bước chuẩn bị (luồng đặt hàng C2 cũng tạo được).

## Ca kiểm thử

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC-CHAT-00 | Đăng ký khách mới; tạo một đơn COD giữa khách và TechZone | Có khách, có đơn `TR-…` |
| TC-CHAT-01 | Chủ shop › Cài đặt › Tuỳ chọn vận hành: bật "Tự động trả lời chat", nhập lời chào + tin vắng mặt → "Lưu tin nhắn" | Toast "Đã lưu cấu hình tự động trả lời"; `vendor_profiles.auto_reply_*` đúng (UC-VND-06 b1–3) |
| TC-CHAT-02 | Khách mở `/chat?shop=1&product=2` (nút "Chat ngay" ở trang sản phẩm) | Hội thoại mới: lời chào hiện ngay với nhãn **Tin nhắn tự động**, thẻ sản phẩm tự gửi (ảnh, tên, giá, "Xem sản phẩm"); URL bỏ tham số |
| TC-CHAT-03 | Hai trình duyệt: chủ shop mở Seller › Chat, chọn khách; khách gửi chữ | Shop nhận **< 1 giây** không tải lại; bong bóng khách "Đang gửi…" → **Đã nhận** |
| TC-CHAT-04 | (Shop đang mở hội thoại) | Tin của khách chuyển **Đã xem**; bộ đếm chưa đọc phía shop = 0 |
| TC-CHAT-05 | Shop gõ chữ trong ô soạn rồi gửi | Khách thấy "Shop đang nhập…", tắt khi tin tới; tin của shop chuyển "Đã xem" phía shop (khách đang mở) |
| TC-CHAT-06 | Khách gửi ảnh PNG | Ảnh hiện hai phía qua link ký (`X-Amz-Signature`); lưu `chat-media/conversation/{id}/…`; mở thẳng không ký → 403 |
| TC-CHAT-07 | Khách chọn ảnh GIF, rồi ảnh > 5 MB | Báo "Ảnh phải là JPG / PNG / WEBP, tối đa 5 MB." ngay tại chỗ, không có tin mới (UC-SOC-02 ngoại lệ) |
| TC-CHAT-08 | Khách bấm "Gửi đơn hàng" → chọn đơn; shop mở menu "Đơn hàng" | Ô chọn đơn chỉ có đơn giữa đúng hai bên (API 8.6); shop thấy thẻ đơn có mã |
| TC-CHAT-09 | Chủ shop đóng trình duyệt; khách nhắn 2 tin; chủ shop đăng nhập lại | Khách nhận **một** tin vắng mặt `is_auto` (tối đa 1 / 60 phút); chủ shop có **một** thông báo `CHAT_MESSAGE` chưa đọc (gộp theo hội thoại); nav Chat có badge |
| TC-CHAT-10 | Chủ shop mở hội thoại | Thông báo chat thành đã đọc; badge nav Chat ẩn |
| TC-CHAT-11 | Khách mất mạng, gửi một tin, có mạng lại (bấm "Gửi lại" nếu bong bóng "Gửi lỗi") | Đúng **một** tin trong CSDL và trên màn (gửi lại cùng `client_message_id`) |
| TC-CHAT-12 | Khách chưa đăng nhập bấm nút chat nổi | Hộp nhắc đăng nhập |
| TC-CHAT-13 | Nhân viên đăng nhập, xem storefront | Không có nút chat; API `/me/conversations` với token nhân viên → `403 GNE-SEC-4030` (IT) |
| TC-CHAT-14 | Khổ 360 px, `/chat` mở hội thoại với 9 theme | Không tràn ngang, bong bóng / thẻ / ô soạn đủ |
| TC-CHAT-15 | Gọi API theo `chat.http` (lỗi 4096, 4041, 4031, 422, 429) | Đúng mã lỗi — đã phủ bằng IT (`ChatSecurityIT`, `ChatCardsIT`, `ChatConversationIT`) |

## Kết quả chạy thật (2026-11-27)

Chromium điều khiển bằng Playwright, 3 ngữ cảnh song song (khách, chủ shop, khách vãng lai / nhân viên / điện thoại 360 px), hệ thống thật: web bản build `PUBLIC_REAL_API=…,notifications,platform-notifications,chat` + backend `local` (JDK 21, `PUBLIC_S3_URL=http://localhost:9000`) + Postgres + Redis + Silo + Mailpit.

| Mã | Kết quả | Ghi chú |
|---|:---:|---|
| TC-CHAT-00 | Đạt | Đăng ký thật qua OTP Mailpit |
| TC-CHAT-01 | Đạt | Lưu qua `PATCH /seller/settings` thật |
| TC-CHAT-02 | Đạt | CSDL: `TEXT:is_auto` rồi `PRODUCT_CARD` |
| TC-CHAT-03 | Đạt | Shop nhận sau 93–175 ms (3 lượt) |
| TC-CHAT-04 | Đạt | |
| TC-CHAT-05 | Đạt | |
| TC-CHAT-06 | Đạt | Ảnh nén JPEG phía client; mở không ký → 403 |
| TC-CHAT-07 | Đạt | Sau bản sửa `a526359` |
| TC-CHAT-08 | Đạt | |
| TC-CHAT-09 | Đạt | 1 tin vắng mặt, 1 thông báo, badge Chat = 2 |
| TC-CHAT-10 | Đạt | Badge ẩn ngay (sau bản sửa `9241f13`) |
| TC-CHAT-11 | Đạt | Chế độ offline của Chromium không đóng WebSocket đang mở: khung SEND tới server khi có mạng lại → ack tự chuyển "Đã nhận"; không trùng tin |
| TC-CHAT-12 | Đạt | Popup banner khuyến mãi (modal) phải đóng trước — đúng hành vi |
| TC-CHAT-13 | Đạt | |
| TC-CHAT-14 | Đạt | 9 / 9 theme, `scrollWidth` ≤ 360 |
| TC-CHAT-15 | Đạt | IT |

**15 / 15 ca chạy trên trình duyệt đạt** (+ TC-CHAT-15 bằng IT). Lỗi phát hiện trong lúc chạy và đã sửa: (1) client cho chọn ảnh GIF và nén ảnh > 5 MB trước khi kiểm → không báo lỗi tại chỗ (`a526359`); (2) badge Chat ở Seller Center chỉ đổi khi tải lại trang, phía shop hiện "Shop đang nhập…" cho khách (`9241f13`).

**Số đo (máy dev, M09.03):** 50 cặp khách ↔ 10 shop, mỗi khách 5 tin gửi đồng thời qua `/app/chat.send` (250 tin): nhận đủ 250 / 250, lưu 250, bộ đếm chưa đọc đúng 50 / 50 hội thoại; độ trễ tới người nhận p50 161 ms, **p95 235 ms**, max 247 ms (mục tiêu < 300 ms). Tài khoản đo tải tạo bằng SQL, token ký bằng khóa JWT máy dev (đăng nhập bị giới hạn 10 lần / phút / IP).
