# PLAN — Chat Khách ↔ Shop thời gian thực (UC-SOC-01 · UC-SOC-02 · UC-VND-06)

| | |
|---|---|
| **Ngày lập** | 2026-11-26 |
| **Thể loại** | server-spring (module `social`, feature `chat`; cài cổng ở `vendor`, `catalog`, `order`) + nối `web/` (feature `chat`, `seller-settings`) |
| **Use case** | Nhắn tin trò chuyện thời gian thực Khách – Shop — Live Chat (UC-SOC-01) · Gửi thẻ sản phẩm, hình ảnh và trạng thái đơn hàng trong chat (UC-SOC-02) · Cấu hình tin nhắn trả lời tự động — Auto-reply (UC-VND-06) — 3 / 4 use case của cụm B6 (UC-NOT-01 ở [plan thông báo](plan_thong_bao_realtime.md)) |
| **Tài liệu chuẩn** | UC phân hệ 08 (UC-SOC-01 dòng 2128, UC-SOC-02 dòng 2163) + phân hệ 05 (UC-VND-06 dòng 1596) · API 8.1–8.5, 10.1 (`badges`), 10.11 (`auto_reply`), 13, 15 · ENTITY 1.4 (`VendorProfile.autoReply*`, `chatResponseRate`, `vacation*`), 11.1–11.2 · LUU_TRU 5.4 (`chat-media` riêng tư, `conversation/{id}/{uuid}`) — xem [README](README.md) |
| **Theo dõi tiến độ** | [progress_chat_khach_shop.md](progress_chat_khach_shop.md) — cùng mã task `Mxx.yy` |
| **Phụ trách** | **Nguyễn Ngọc Thiện** — lập plan và thực hiện (nhận 2026-11-26) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `--no-verify`, không `Co-Authored-By` |
| **Phụ thuộc** | Plan [1]: **N01** (cột `messages.is_auto`, index `conversations`, `NotificationType.CHAT_MESSAGE`), **N03.04** (`NotificationFacade.upsertChat`), **N05.01–N05.02** (hub WebSocket web có `send`) — README mục 2 |
| **Mục tiêu** | 3 use case ⬜ → ✅ (đủ 5 lớp) |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `5abb096`, 2026-11-26)

| Phần | Đã có | Còn thiếu |
|---|---|---|
| **Entity / bảng** (`V11__chat.sql`) | `Conversation` (`customer_id`, `vendor_id` = **`users.id` chủ shop**, UNIQUE cặp; `last_message_snippet/at`, `customer_unread_count`, `vendor_unread_count`; `onNewMessage`, `markReadBy`), `Message` (`type` ∈ `TEXT, IMAGE, PRODUCT_CARD, ORDER_CARD`, `content`, `attachment_url`, `is_read`, `product_variant_id`, `order_id`; FK SET NULL), index `(conversation_id, created_at DESC)`. Repository rỗng | ⚠ Không có `read_at` (UC-SOC-02 nhắc `readAt`) → D3. Không có `client_message_id` → D4. Không có cờ tin tự động → **`V27` thêm `is_auto`** (plan [1] N01.04). Không có index danh sách hội thoại theo phía → `V27` |
| **Package** | `social/chat/{api,application,domain}` chỉ entity | Toàn bộ service / controller / STOMP handler |
| **UC-VND-06 — phần cấu hình** | **Đã có từ B4** (UC-VND-21): `PATCH /seller/settings` khối `auto_reply` (`enabled`, `greeting`, `away_message` ≤ 500) → `VendorProfile.configureAutoReply`, `422 GNE-VND-4222` khi bật mà rỗng; `GET /seller/settings` trả `auto_reply`; web `seller-settings` đã nối (cờ `vendor`) | **Phần gửi tự động trong chat** (UC-VND-06 bước 4–5) chưa có — plan này làm |
| **Shop** | `VendorProfile.chatResponseRate` (NULL — "job hằng ngày" chưa có), `isMall`, `vacationStartsAt/EndsAt/Message` (UC-VND-19 chưa làm, cột có sẵn); cổng `SellerBadgeContributor` (vendor — chờ chat trả `unread_chats`); cổng KYC `/seller/**` (`KycGateInterceptor`) đã phủ `/seller/conversations/**` → shop chưa `VERIFIED` nhận `403 GNE-VND-4031` | Cổng tra shop cho chat; job tỉ lệ phản hồi |
| **Realtime** | `WebSocketConfig` (`/app` prefix, `/user/queue/**`), `StompAuthInterceptor` (SEND cần đã xác thực), `UserChannel.sendAfterCommit`; plan [1] thêm `OnlineUsers` | `@MessageMapping` chat; hằng `CHAT`; theo dõi kết nối / ngắt (presence) |
| **Đơn / sản phẩm** | Bảng `orders` có dữ liệu từ C2 (`TR-…`), `VariantFacade.lines` (C2), `ProductSummaryRows` (catalog) | Cổng tra thẻ sản phẩm / đơn cho chat (đặt đúng chiều phụ thuộc — README mục 3) |
| **Web** | `features/chat`: `chat.api.ts` (`listConversations`, `openConversation`, `listMessages`, `sendMessage`, `markRead`, `mockTyping`), `seller-chat.api.ts` (4 hàm `/seller/conversations/**`), `useChat` / `useSellerChat` **hỏi lại 2 giây / 15 giây** thay WebSocket, `chatSend.ts` (bong bóng "Đang gửi…"), `ChatWidget` (khung nổi), `ChatPage`, `SellerChatPage`, `ChatParts`; chạy `USE_MOCK`. Handoff dòng 61 (WebSocket, `client_message_id` làm Idempotency-Key), 75 (`mockTyping`) | Nối thật bằng cờ `chat`; thay polling bằng hub realtime (plan [1] N05) |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Package `com.genie.social.chat.{api,application,domain}`; STOMP handler ở `api` (`ChatStompController`) gọi **cùng** service với REST — một luật nghiệp vụ, hai cửa | TS 4.1; API 8 ("REST là đường dự phòng") |
| N2 | **`social` không import `vendor`, `catalog`, `order`** (vòng `order → vendor → social`). Cần dữ liệu của họ → **cổng** trong `social.chat.application` (`ChatShopDirectory`, `ChatProductLookup`, `ChatOrderLookup`), module kia cài bằng **file mới**. Được import `auth` (`UserFacade`) và `notification` (`NotificationFacade`) | README mục 3 |
| N3 | Ghi tin + cập nhật hội thoại (snippet, `last_message_at`, bộ đếm chưa đọc) **trong một transaction**, khóa dòng `conversations` (`SELECT … FOR UPDATE`) để bộ đếm đúng khi hai bên gửi cùng lúc | TS 10; UC-SOC-01 b6 |
| N4 | Đẩy WebSocket **sau commit** (`UserChannel.sendAfterCommit`) tới **cả hai** bên (người gửi nhận lại để chuyển "Đang gửi…" → "Đã nhận"); người nhận offline → thông báo in-app (`NotificationFacade.upsertChat`) | UC-SOC-01 b7–8, ngoại lệ |
| N5 | Kiểm quyền ở service theo hội thoại (IDOR): khách chỉ hội thoại có `customer_id = me`; shop chỉ hội thoại có `vendor_id = me`; id lạ hay của người khác → `404 GNE-SYS-4040` (không lộ tồn tại) | TS 9 |
| N6 | Ảnh chat nằm bucket **riêng tư** `chat-media`; API chỉ trả URL ký tạm (presigned GET 10 phút), không lưu URL công khai | LUU_TRU 5.4; API 8.3 |
| N7 | Tin tự động (lời chào / vắng mặt / tạm nghỉ) có `is_auto = true`, đứng tên shop, **không** tính vào tỉ lệ phản hồi | UC-VND-06 b4 |
| N8 | Chống spam: giới hạn tần suất gửi bằng `RateLimiter` Redis sẵn có (`429 GNE-RAT-4290`), `client_message_id` chống gửi trùng | TS 9 |
| N9 | JSON snake_case; danh sách hội thoại / tin nhắn phân trang **cursor**; `@Operation(summary = "UC-SOC-0x …")` | TS 8 |
| N10 | Mỗi task có test: unit (luật thuần, entity) + `*IT` Testcontainers (PG + Redis + Silo) + IT STOMP thật (`WebSocketStompClient`) cho luồng realtime | TS 14 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Kênh gửi tin** | API 8.4 (REST) + 13 (`/app/chat.send`) | Cả hai cùng gọi `ChatService.send(…)`. Web dùng STOMP khi hub đang kết nối, rớt kết nối thì `POST …/messages` (cùng `client_message_id` → không trùng). STOMP lỗi nghiệp vụ → gửi về `/user/queue/chat` `{type: "MESSAGE_FAILED", client_message_id, code, message}` | ☐ |
| D2 | **Redis Pub/Sub** (UC-SOC-01 b7, ràng buộc) | TS: simple broker một instance | TLCN **không** dùng Redis Pub/Sub — simple broker trong bộ nhớ (như plan B1 D18, plan [1] D20); ghi vào *Ràng buộc* của UC-SOC-01 là phần DATN khi chạy nhiều instance | ☐ |
| D3 | **Trạng thái tin** `SENT / DELIVERED / SEEN` khi bảng không có `read_at` / `delivered_at` | API 8.3; UC-SOC-02 b5 ("Đang gửi…" → "Đã nhận" = ack WebSocket → "Đã xem") | Không thêm cột: `SENT` chỉ tồn tại phía client (bong bóng chưa có ack); tin đã lưu = `DELIVERED` (server đã nhận); `is_read = true` = `SEEN`. Server chỉ trả `DELIVERED` / `SEEN`. Ghi API 8.3 + ENTITY 11.2 (bỏ nhắc `readAt`) | ☐ |
| D4 | **`client_message_id`** không có cột | API 8.3–8.4; handoff dòng 61 | Không lưu CSDL. Redis `chat:cmid:{userId}:{uuid}` → `message_id` (`SET NX`, 24 giờ): gửi lại cùng uuid → trả tin đã lưu (`200` thay `201`). Gói realtime và phản hồi 8.4 mang lại `client_message_id`; lịch sử 8.3 trả `null` (đúng API). Redis lỗi → bỏ qua chống trùng, vẫn gửi | ☐ |
| D5 | **Ai được chat** | UC-SOC-01 tiền điều kiện | Khách: vai trò `CUSTOMER` / `VENDOR` (chủ shop chat với shop **khác**); **STAFF / ADMIN bị chặn** (`@PreAuthorize`, `403 GNE-SEC-4030` — tài khoản nội bộ chỉ xem, plan B1 D16). Shop đích phải `VERIFIED` + chủ `ACTIVE` (`404 GNE-VND-4041` nếu không). Chat với shop của chính mình → **`409 GNE-SOC-4096` (mới)** "Không thể nhắn tin với shop của chính mình" — thêm API 15. Hội thoại cũ mà shop bị khóa / chủ bị đình chỉ: đọc được, gửi → `404 GNE-VND-4041` | ☐ |
| D6 | **Nội dung tin chữ** | API 8.4 (`422` tin rỗng) | Cắt khoảng trắng đầu cuối, bỏ ký tự điều khiển (giữ xuống dòng), 1–2.000 ký tự; lưu văn bản thô, web hiển thị dạng text (không HTML). Tần suất: 20 tin / 10 giây / người (`rl:chat:{userId}`) | ☐ |
| D7 | **Ảnh** | UC-SOC-02 b2; LUU_TRU 5.4 | `UploadPurpose.CHAT_IMAGE` (`chat-media`, JPEG / PNG / WebP ≤ 5 MB, riêng tư, mọi người đã đăng nhập). Gửi `{type: IMAGE, image_object_key: "tmp/chat-media/…"}` — khóa phải là `tmp/` **của chính người gửi** → `promoteTo("conversation/{id}/")`, lưu khóa vào `attachment_url`. Đọc: presigned GET 10 phút tạo lúc trả phản hồi. Một tin một ảnh | ☐ |
| D8 | **Thẻ sản phẩm** — API gửi `product_id`, bảng lưu `product_variant_id` | API 8.4 vs ENTITY 11.2 | Body nhận `product_id` + `variant_id` (tùy chọn — trang sản phẩm đang chọn SKU). Server lưu biến thể (có `variant_id` thì kiểm thuộc sản phẩm, không thì biến thể mặc định — giá thấp nhất còn bán). Sản phẩm phải thuộc **shop của hội thoại** + đang `PUBLISHED`, ngược lại `403 GNE-SOC-4031`. Khi đọc: thông tin **hiện tại** qua cổng (`id`, `name`, `image_url`, `price`, `slug`); sản phẩm đã ẩn / xóa → `product.is_available = false`, `price = null` (thẻ vẫn hiện tên). Sửa API 8.3 / 8.4 | ☐ |
| D9 | **Thẻ đơn hàng** + ô chọn đơn | UC-SOC-02 b3 (người bán chọn đơn); 14.1 dùng `GET /me/orders?shop_id=` (C5 — chưa có) | Cả hai bên gửi được `{type: ORDER_CARD, order_id}`; đơn phải có `customer_id` + `vendor_id` đúng cặp hội thoại (`403 GNE-SOC-4031`). **Thêm endpoint 8.6** `GET /me/conversations/{id}/orders?limit=10` (+ `/seller/conversations/{id}/orders`) → đơn gần nhất giữa hai bên `[{id, code, status, total, created_at, item_count, thumbnail_url}]` — không chờ C4 / C5. Thẻ hiện mã đơn, trạng thái hiện tại; `tracking_code` = `null` tới khi C4 có vận đơn | ☐ |
| D10 | **Bộ đếm chưa đọc & đánh dấu đã xem** | UC-SOC-01 b9; API 8.5 | `send` tăng bộ đếm phía người nhận (khóa dòng — N3). `read`: `markReadBy(side)` + `UPDATE messages SET is_read = true WHERE conversation_id = ? AND sender_id <> me AND is_read = false` → sự kiện `MESSAGE_SEEN {conversation_id, up_to_message_id}` cho bên kia; đọc khi đang có thông báo `CHAT_MESSAGE` chưa đọc của hội thoại → đánh dấu luôn thông báo đó. `unread_total` (badge) = `SUM` bộ đếm phía mình | ☐ |
| D11 | **Tin tự động** (UC-VND-06 b4–5) | UC-VND-06; UC-SOC-01 ngoại lệ | (a) **Lời chào**: khi hội thoại **mới tạo** bằng 8.2, hoặc mở lại mà tin cuối cách ≥ 24 giờ và chưa có lời chào trong 24 giờ — chỉ khi `auto_reply.enabled` và `greeting` khác rỗng. (b) **Vắng mặt**: khi khách gửi tin và shop "vắng" = ngoài 08:00–22:00 giờ VN **hoặc** chủ shop không có kết nối WebSocket nào — gửi `away_message`, tối đa **1 lần / hội thoại / 60 phút**. (c) **Tạm nghỉ**: shop có `vacation_starts_at <= now < vacation_ends_at` → dùng `vacation_message` thay (b), bất kể `auto_reply.enabled` (UC-VND-19 sau này chỉ bật cờ — đọc cột sẵn có). Tin tự động `is_auto = true`, `sender_id` = chủ shop, đẩy realtime như tin thường | ☐ |
| D12 | **Tỉ lệ & thời gian phản hồi** ("Phản hồi 98% · trong vài phút" — API 8.2 `shop_stats`, trang shop B3) | ENTITY 1.4 `chatResponseRate` ("trả lời trong 24h, tính trên 30 ngày, job hằng ngày") | `ChatFacade.responseStats(vendorUserIds, from, to)`: một hội thoại "được trả lời" nếu sau tin đầu tiên của khách (trong cửa sổ 30 ngày) có tin **không tự động** của shop trong 24 giờ; `avg_response_minutes` = trung vị thời gian tới tin trả lời đầu. Job `ChatResponseRateJob` ở **vendor** (03:30 hằng ngày, lô 100 shop) ghi `vendor_profiles.chat_response_rate` (NULL khi < 5 hội thoại). `avg_response_minutes` tính khi mở hội thoại, cache Redis `chat:stats:{vendorUserId}` 1 giờ | ☐ |
| D13 | **Trực tuyến** (`is_online`, `last_active_at` ở 8.1) | API 8.1 | `is_online` = `OnlineUsers.isOnline(userId)` (plan [1] N02.06); `last_active_at` = Redis `presence:last:{userId}` ghi khi `SessionDisconnectEvent` (không có → `null`). Phía khách xem shop: người online là **chủ shop** | ☐ |
| D14 | **Đang gõ** | API 13 `/app/chat.typing` | Chuyển tiếp `{type: TYPING, conversation_id}` cho bên kia, không lưu, tối đa 1 tin / 2 giây / hội thoại / người (bỏ thừa); web ẩn dấu "…" sau 4 giây không nhận thêm. Bỏ `mockTyping` khi chạy thật | ☐ |
| D15 | **Gói tin `/user/queue/chat`** | API 13 | `{type: "MESSAGE_CREATED", conversation_id, message: <8.3, sender theo góc nhìn người nhận>, client_message_id, unread_total}`, `{type: "MESSAGE_SEEN", conversation_id, up_to_message_id}`, `{type: "TYPING", conversation_id}`, `{type: "MESSAGE_FAILED", client_message_id, code, message}`. Ghi API 13 | ☐ |
| D16 | **Danh sách hội thoại** | API 8.1 | Chỉ hội thoại đã có ít nhất một tin (`last_message_at IS NOT NULL`); sắp `last_message_at DESC, id DESC`, cursor; `limit` 1–50. Phía khách `counterpart` = shop (`id` = `vendor_profiles.id`, `name`, `avatar_url` = logo, `is_mall`, `is_online`, `last_active_at`); phía shop = khách (`id` = `users.id`, `name`, `avatar_url`, `is_online`) | ☐ |
| D17 | **Badge Seller Center** | API 10.1 `badges.unread_chats`; cổng `SellerBadgeContributor` | Cài ở **vendor** (`vendor/shop/application/ChatBadgeContributor` gọi `ChatFacade.unreadForVendor`) — không để `social` import `vendor`. Badge storefront (nút chat nổi) lấy `unread_total` từ realtime / `GET /me/conversations` (`summary.unread_total`) | ☐ |
| D18 | **Tài khoản bị ẩn danh** (UC-AUTH-10) | — | Không xóa tin; tên / ảnh lấy từ `UserFacade` nên tự thành "Người dùng Genie" sau khi ẩn danh. Hội thoại với người đã ẩn danh: chỉ đọc, gửi → `404` | ☐ |
| D19 | **Cờ nối web** | `isMocked(feature)` | `chat` (storefront + Seller). Thẻ đơn hàng + ô chọn đơn dùng 8.6 (không cần cờ `orders`) | ☐ |
| D20 | **Ngoài phạm vi** | — | Email nhắc tin đầu ngày (UC-SOC-01 ngoại lệ → UC-NOT-03 / C11), trả lời theo từ khóa (DATN), thu hồi / sửa tin, chặn người dùng, gửi nhiều ảnh một lần, video | ☐ |

---

## 3. Luồng nghiệp vụ chi tiết

### 3.1 Mở hội thoại & lời chào (UC-SOC-01 b1–3, UC-VND-06 b4)

```
POST /me/conversations {shop_id}
  ├─ vai trò STAFF/ADMIN → 403 ; ChatShopDirectory.find(shopId) → không VERIFIED / chủ không ACTIVE → 404 GNE-VND-4041
  ├─ chủ shop == me → 409 GNE-SOC-4096
  ├─ INSERT conversations (customer_id, vendor_id) ON CONFLICT DO NOTHING → lấy dòng (201 mới / 200 đã có)
  ├─ AutoReplyPolicy.greetingFor(conv, shop, now) → có → gửi tin is_auto (cùng transaction) → realtime sau commit
  └─ trả {id, counterpart, shop_stats{chat_response_rate, avg_response_minutes}, unread_count}
Web: mở từ trang sản phẩm → gửi tiếp PRODUCT_CARD (8.4) ; từ theo dõi đơn → ORDER_CARD
```

### 3.2 Gửi tin (UC-SOC-01 b4–8, UC-SOC-02)

```
STOMP /app/chat.send {conversation_id, client_message_id, type, text|image_object_key|product_id(+variant_id)|order_id}
 hoặc POST /me|seller/conversations/{id}/messages
  ├─ rate limit rl:chat:{me} (20/10s) ; Redis chat:cmid:{me}:{uuid} đã có → trả tin cũ
  ├─ ChatService.send — transaction:
  │    SELECT conversations FOR UPDATE ; kiểm me là customer/vendor của hội thoại (N5) ; shop còn hoạt động (D5)
  │    TEXT: chuẩn hóa (D6) · IMAGE: promote tmp → conversation/{id}/… (D7)
  │    PRODUCT_CARD: ChatProductLookup.resolve(productId, variantId, vendorUserId) (D8) · ORDER_CARD: ChatOrderLookup.belongs(orderId, customerId, vendorUserId) (D9)
  │    INSERT messages ; conversation.onNewMessage(m, fromCustomer)
  │    khách gửi & AutoReplyPolicy.awayFor(...) (D11) → INSERT tin is_auto
  ├─ sau commit: /user/queue/chat MESSAGE_CREATED → người gửi (kèm client_message_id) + người nhận
  └─ người nhận không online (OnlineUsers) → NotificationFacade.upsertChat(recipient, convId, "Tin nhắn mới từ {tên}", snippet)
```

### 3.3 Đã xem (UC-SOC-01 b9, UC-SOC-02 b5)

```
STOMP /app/chat.read {conversation_id}  hoặc  POST …/{id}/read
  ├─ markReadBy(side) ; UPDATE messages SET is_read = true … ; đánh dấu thông báo CHAT_MESSAGE của hội thoại (nếu có)
  └─ sau commit: bên kia nhận MESSAGE_SEEN {conversation_id, up_to_message_id} → bong bóng "Đã xem"
```

---

## 4. Cây file sẽ tạo / sửa

```
server-spring/src/main/java/com/genie/
├── common/
│   ├── error/ErrorCode.java            (sửa) + CHAT_CARD_FORBIDDEN GNE-SOC-4031 · CHAT_OWN_SHOP GNE-SOC-4096
│   ├── storage/UploadPurpose.java      (sửa) + CHAT_IMAGE
│   └── realtime/UserChannel.java       (sửa) + CHAT ; realtime/PresenceListener.java (mới — SessionDisconnectEvent → presence:last)
├── social/chat/
│   ├── domain/       Conversation (sửa: openFor(), sideOf(userId)…) · Message (sửa: factory text/image/product/order/auto, statusFor(viewer))
│   │                 ConversationRepository · MessageRepository (truy vấn cursor, đánh dấu đọc, thống kê phản hồi)
│   ├── application/  ChatService · ChatQueryService · AutoReplyPolicy · ChatFacade · ChatRealtime · ChatIdempotency
│   │                 ChatShopDirectory · ChatProductLookup · ChatOrderLookup (cổng) · MessageView · ConversationView · ChatCursor
│   └── api/          MeConversationController · SellerConversationController · ChatStompController · request/response record
├── vendor/shop/application/ ChatShopDirectoryImpl · ChatBadgeContributor · ChatResponseRateJob   (mới — vendor → social)
├── catalog/product/application/ ChatProductLookupImpl                                           (mới — catalog → social)
└── order/order/application/ ChatOrderLookupImpl                                                 (mới — order → social)
server-spring/src/test/java/com/genie/social/chat/
    ConversationTest · MessageTest · AutoReplyPolicyTest · ChatCursorTest
    ConversationIT · ChatMessageIT · ChatReadIT · ChatCardsIT · ChatImageIT · ChatStompIT · ChatSecurityIT · AutoReplyIT · ChatResponseRateJobIT (vendor)
web/src/features/chat/{domain,infrastructure,application,ui}/…    (sửa — DTO 8.x, isMocked('chat'), realtime qua hub, ảnh, thẻ, ô chọn đơn)
web/src/shared/api/error-codes.ts · web/src/i18n/{vi,en}.json
docs/Document/…  API_SPEC (8.1–8.6, 13, 15, 4.1) · ENTITY (11.1, 11.2) · Usecase.md (UC-SOC-01, 02, UC-VND-06) · CHECKLIST · BAO_CAO · Quản Lý UseCase.xlsx
docs/api-tests/chat.http · docs/kiem-thu/kich-ban-2026-11-26-chat.md · docs/web-backend-handoff.md
```

---

## 5. Bảng ánh xạ Use case → API → Dữ liệu

| Bước UC | Endpoint / kênh | Bảng | Quyền |
|---|---|---|---|
| UC-SOC-01 b1–3: "Chat ngay", tạo / lấy hội thoại | `POST /me/conversations` | `conversations` (+ `messages` lời chào) | CUSTOMER / VENDOR |
| UC-SOC-01 b3: 30 tin gần nhất, cuộn lên | `GET /me|seller/conversations/{id}/messages?before&limit=30` | `messages` | thành viên hội thoại |
| UC-SOC-01 b4–8: gửi, đẩy tức thời | `/app/chat.send` · `POST …/{id}/messages` → `/user/queue/chat` | `messages`, `conversations` | thành viên |
| UC-SOC-01 b9: đã xem, xóa badge | `/app/chat.read` · `POST …/{id}/read` | `messages.is_read`, bộ đếm | thành viên |
| UC-SOC-01 b10: hộp thư (shop) | `GET /me/conversations` · `GET /seller/conversations` | `conversations` | — |
| UC-SOC-01 ngoại lệ offline | `NotificationFacade.upsertChat` | `notifications` (`CHAT_MESSAGE`) | hệ thống |
| UC-SOC-02 b1: thẻ sản phẩm | 8.4 `PRODUCT_CARD` | `messages.product_variant_id` | thành viên |
| UC-SOC-02 b2: ảnh | `POST /uploads/presigned-urls` (`CHAT_IMAGE`) → 8.4 `IMAGE` | Silo `chat-media` | thành viên |
| UC-SOC-02 b3: thẻ đơn hàng | `GET …/{id}/orders` (8.6 mới) → 8.4 `ORDER_CARD` | `messages.order_id`, `orders` (đọc) | thành viên |
| UC-SOC-02 b5: Đang gửi → Đã nhận → Đã xem | `MESSAGE_CREATED` (ack), `MESSAGE_SEEN` | — | — |
| UC-VND-06 b1–3: cấu hình | `PATCH /seller/settings` khối `auto_reply` (**đã có — B4**) | `vendor_profiles` | VENDOR `VERIFIED` |
| UC-VND-06 b4–5: gửi tự động | trong `ChatService` (D11) | `messages.is_auto` | hệ thống |
| Badge Seller | `GET /seller/me` → `badges.unread_chats` | `conversations` | VENDOR |

**Mã lỗi:** `GNE-SOC-4031` (403 — thẻ đơn / sản phẩm không thuộc hội thoại, đã có ở API 15), `GNE-SOC-4096` (409 — chat với shop của mình, **mới**). Dùng lại `GNE-VAL-4220`, `GNE-SYS-4040`, `GNE-VND-4041`, `GNE-RAT-4290`, `GNE-SEC-4030`, `GNE-VND-4031` (cổng KYC).

---

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — gồm `ModuleBoundaryTest` (**`social` không import `vendor` / `catalog` / `order`**).
2. Swagger có endpoint mới, summary `UC-SOC-01` / `UC-SOC-02`.
3. Thử tay `docs/api-tests/chat.http`: khách thuần ↔ `mai.tran@gmail.com` (chủ TechZone) và `owner.shoe@genie.example`; nhân viên `phat.nt@genie.example` → `403`.
4. Đụng `web/`: `cd web && npm run verify`.
5. Tick task ở progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Mxx.yy` = task; `Mxx.Cn` = task commit.

### M00 — Chuẩn bị

- **[M00.01]** Kiểm danh tính git; kiểm plan [1] đã merge N01 (có `messages.is_auto`, `V27`) và N03.04 / N05.02 (hoặc rebase lên nhánh plan [1] nếu chưa merge; ghi vào progress).
- **[M00.02]** Chốt D1–D20 vào progress.
- **[M00.03]** Tạo nhánh `feature/chat-live` từ `develop` mới nhất.
- **[M00.04]** Đọc màn mock — khung chat nổi (hộp thư nhiều shop, luồng tin, tin nhanh 4 câu, gửi ảnh, gửi đơn, thẻ sản phẩm, 3 trạng thái), `/chat`, Seller › Chat (danh sách, online, badge); ghi trường DTO mock để đối chiếu API 8.
- **[M00.05]** Kiểm dữ liệu seed — có khách thuần (`SELECT email FROM users WHERE role = 'CUSTOMER'`) và đơn của khách đó ở shop seed (C2 tạo đơn COD thật trên máy dev, ghi id vào progress cho ca thẻ đơn).

### M01 — Tài liệu trước

- **[M01.01]** API_SPEC 8 — 8.1 `summary.unread_total`, cursor, chỉ hội thoại có tin (D16); 8.2 `409 GNE-SOC-4096`, `403` nội bộ, `shop_stats` (D12); 8.3 `status` chỉ `DELIVERED` / `SEEN` (D3), `product.is_available`, `product.slug`, `order.status`, `is_auto`; 8.4 `variant_id`, `image_object_key` phải là `tmp/` của người gửi, giới hạn 2.000 ký tự, `429`, gửi lại cùng `client_message_id` → `200` cùng tin (D4); **thêm 8.6** ô chọn đơn (D9).
- **[M01.02]** API_SPEC 13 — gói tin `/user/queue/chat` (D15), luật `/app/chat.typing` (D14); 4.1 purpose `CHAT_IMAGE`; 15 thêm `GNE-SOC-4096`; lịch sử phiên bản.
- **[M01.03]** Usecase.md — UC-SOC-01 ràng buộc (D2 simple broker), luật vắng mặt (D11b); UC-SOC-02 b5 (D3); UC-VND-06 b3 đường dẫn thật `PATCH /seller/settings` (thay `PUT /api/v1/vendor/auto-reply`), b4 luật 24 giờ, b5 tạm nghỉ; ENTITY 11.2 bỏ `readAt`, ghi `isAuto`, cách suy trạng thái.
- **[M01.C1]** COMMIT — `docs(social): specify live chat events, cards and auto replies`

### M02 — Hằng số dùng chung & cổng

- **[M02.01]** `ErrorCode` + `GNE-SOC-4031`, `GNE-SOC-4096` (khối `// ===== Tương tác`); `UploadPurpose.CHAT_IMAGE` (D7); `UserChannel.CHAT`.
- **[M02.02]** Cổng trong `social.chat.application` — `ChatShopDirectory` (`find(shopId)` → `ShopRef{shopId, vendorUserId, name, logoUrl, isMall, active}`, `byVendorUsers(ids)`, `autoReply(vendorUserId)` → `{enabled, greeting, awayMessage, vacationMessage?, vacationActive}`, `chatResponseRate(vendorUserId)`), `ChatProductLookup` (`resolve(productId, variantId, vendorUserId)`, `cards(variantIds)`), `ChatOrderLookup` (`belongs(orderId, customerId, vendorUserId)`, `cards(orderIds)`, `recentBetween(customerId, vendorUserId, limit)`). Mỗi cổng có bản mặc định `@ConditionalOnMissingBean` (shop: không tìm thấy; sản phẩm / đơn: từ chối) để `social` test riêng được.
- **[M02.03]** `vendor/shop/application/ChatShopDirectoryImpl` — đọc `VendorProfileRepository` + `UserFacade` (trạng thái chủ); `shop_id` = `vendor_profiles.id`, hội thoại lưu `users.id` chủ shop (bẫy hai id).
- **[M02.04]** `catalog/product/application/ChatProductLookupImpl` — JdbcTemplate read-model (tên, slug, ảnh đại diện, giá biến thể, trạng thái, `vendor_id`) — **file mới**, không sửa file của C1.
- **[M02.05]** `order/order/application/ChatOrderLookupImpl` — đọc `orders` (+ số dòng, ảnh dòng đầu) — **file mới**, không sửa file của C2.
- **[M02.06]** `common/realtime/PresenceListener` (D13) — `SessionDisconnectEvent` → Redis `presence:last:{userId}` = now (TTL 30 ngày); lỗi Redis bỏ qua.
- **[M02.07]** Test — unit cho 3 cài đặt (dữ liệu dựng bằng `TestDataBuilder` / `OrderTestData`); `ModuleBoundaryTest` xanh.
- **[M02.C1]** COMMIT — `feat(common): add chat error codes, chat image purpose and presence tracking`
- **[M02.C2]** COMMIT — `feat(social): define chat ports for shops, products and orders`
- **[M02.C3]** COMMIT — `feat(vendor): expose shop chat profile to the chat module`
- **[M02.C4]** COMMIT — `feat(catalog): resolve product cards for chat messages`
- **[M02.C5]** COMMIT — `feat(order): resolve order cards between a buyer and a shop for chat`

### M03 — Hội thoại & tin chữ (UC-SOC-01)

- **[M03.01]** `Conversation` — `sideOf(userId)` (`CUSTOMER / VENDOR / NONE`), `onNewMessage` sửa: snippet theo loại ("[Hình ảnh]", "[Sản phẩm]", "[Đơn hàng]" khi `content` null; cắt 100 ký tự), không tăng chưa đọc cho chính người gửi; `markReadBy`. `ConversationTest`.
- **[M03.02]** `Message` — factory `text(…)`, `image(…)`, `productCard(…)`, `orderCard(…)`, `auto(…)`; `statusFor(viewerId)` (D3). `MessageTest`.
- **[M03.03]** `ConversationRepository` — `insertIgnore(customerId, vendorId)` (`ON CONFLICT DO NOTHING`), `findByIdForUpdate`, trang theo phía + cursor (D16), `sumUnreadForCustomer/Vendor`. `MessageRepository` — trang `before` cursor (30), `markReadFrom(conversationId, readerId)` trả id lớn nhất, `lastAutoAt(conversationId, kind)`.
- **[M03.04]** `ChatCursor` (base64url `{t, id}`); cursor hỏng → 422; `ChatCursorTest`.
- **[M03.05]** `ChatService.open(me, shopId)` (3.1, chưa gồm lời chào — M08) ; `send(me, conversationId, SendCommand)` cho `TEXT` (3.2, D6) ; `read(me, conversationId)` (3.3, D10).
- **[M03.06]** `ChatIdempotency` (D4) + rate limit (N8) — dùng `RateLimiter` sẵn có.
- **[M03.07]** `ChatQueryService` — danh sách hội thoại (counterpart theo phía qua `ChatShopDirectory` / `UserFacade` theo lô — chống N+1; `is_online`, `last_active_at`), tin nhắn (`sender` = `ME` / `THEM` theo người xem, `status`), `unread_total`.
- **[M03.08]** `ConversationIT` — mở mới `201`, mở lại `200` cùng id; hai luồng mở cùng lúc → 1 dòng; shop chưa `VERIFIED` / chủ bị khóa → `404 GNE-VND-4041`; shop của mình → `4096`; nhân viên → `403`.
- **[M03.09]** `ChatMessageIT` — gửi chữ hai chiều, bộ đếm đúng phía; 50 tin song song từ hai bên → bộ đếm = số tin của bên kia (khóa dòng); tin rỗng / > 2.000 → `422`; gửi lại cùng `client_message_id` → cùng tin, không tăng bộ đếm; 21 tin / 10 giây → `429`; người thứ ba đọc / gửi vào hội thoại → `404`; cursor 3 trang không trùng.
- **[M03.10]** `ChatReadIT` — đọc → bộ đếm phía mình 0, tin của bên kia `SEEN`, tin của mình vẫn `DELIVERED` tới khi bên kia đọc; thông báo `CHAT_MESSAGE` chưa đọc của hội thoại được đánh dấu.
- **[M03.C1]** COMMIT — `feat(social): open conversations and exchange text messages with unread counters`

### M04 — Realtime (UC-SOC-01 b5–8)

- **[M04.01]** `ChatRealtime` — sau commit gửi `MESSAGE_CREATED` cho người gửi (kèm `client_message_id`) và người nhận (góc nhìn `THEM`) + `unread_total` của từng người; `MESSAGE_SEEN`; `TYPING` (D14, D15).
- **[M04.02]** `ChatStompController` — `@MessageMapping("chat.send")`, `("chat.read")`, `("chat.typing")`; lấy người dùng từ `StompUser`; lỗi nghiệp vụ (`BusinessException`, `InvalidRequestException`) → `MESSAGE_FAILED` về chính người gửi (không làm rớt kết nối).
- **[M04.03]** Người nhận offline → `NotificationFacade.upsertChat(…)` (plan [1] N03.04) với `link.type = CONVERSATION`; online thì không tạo thông báo.
- **[M04.04]** `ChatStompIT` — hai `WebSocketStompClient` (khách, chủ shop): gửi qua `/app/chat.send` → người gửi nhận ack có `client_message_id`, người nhận nhận tin < 1 giây; `chat.read` → người gửi nhận `MESSAGE_SEEN`; `chat.typing` chuyển tiếp, gửi dồn 5 lần trong 1 giây → bên kia nhận 1; gửi vào hội thoại không phải của mình → `MESSAGE_FAILED` `GNE-SYS-4040`; chủ shop ngắt kết nối → tin mới tạo thông báo `CHAT_MESSAGE` (1 dòng dù 3 tin).
- **[M04.C1]** COMMIT — `feat(social): deliver chat messages, read receipts and typing over websocket`

### M05 — API REST hai phía + badge

- **[M05.01]** `MeConversationController` — 8.1, 8.2, 8.3, 8.4, 8.5 phía khách; `@PreAuthorize("hasAnyRole('CUSTOMER','VENDOR')")`; `@Operation(summary = "UC-SOC-01 …")`.
- **[M05.02]** `SellerConversationController` — 8.1, 8.3, 8.4, 8.5 phía shop (`/seller/conversations/**`, cổng KYC tự áp); hội thoại phải có `vendor_id = me`.
- **[M05.03]** `ChatFacade` — `unreadForVendor(vendorUserId)`, `responseStats(…)` (M08); `vendor/shop/application/ChatBadgeContributor` cài `SellerBadgeContributor` → `unread_chats` (D17).
- **[M05.04]** `ChatSecurityIT` — ma trận: khách / chủ shop / nhân viên / không token × 9 endpoint; chủ shop A không đọc được hội thoại của shop B qua `/seller/…`; shop chưa `VERIFIED` → `403 GNE-VND-4031`; `GET /seller/me` có `badges.unread_chats` đúng.
- **[M05.05]** `docs/api-tests/chat.http` — luồng khách ↔ shop đầy đủ + lỗi.
- **[M05.C1]** COMMIT — `feat(social): expose buyer and seller conversation endpoints`
- **[M05.C2]** COMMIT — `feat(vendor): show unread chat count on the seller center badge`

### M06 — Nối web chat (UC-SOC-01)

- **[M06.01]** `chat/infrastructure` — DTO theo API 8 (`counterpart`, `last_message`, `status`, `client_message_id`, `is_auto`); mapper; `isMocked('chat')` cho cả `chat.api.ts`, `seller-chat.api.ts`; `listConversations` / `listMessages` đọc cursor (cuộn lên tải thêm 30 tin); giữ nhánh mock.
- **[M06.02]** `application/chatRealtime.ts` — đăng ký `/user/queue/chat` qua hub (plan [1] N05.02): `MESSAGE_CREATED` → chèn vào cache tin + cập nhật hộp thư / badge, khớp bong bóng theo `client_message_id`; `MESSAGE_SEEN` → "Đã xem"; `TYPING` → dấu "…" 4 giây; `MESSAGE_FAILED` → bong bóng lỗi + "Gửi lại".
- **[M06.03]** `chatSend.ts` — gửi qua `realtimeHub.send('/app/chat.send', …)` khi đã kết nối, ngược lại `POST` (cùng `client_message_id`); bỏ `refetchInterval` 2 giây / 15 giây khi realtime đang kết nối (giữ hỏi lại 30 giây khi mất kết nối); `markRead` qua `/app/chat.read` khi mở hội thoại / tab được focus.
- **[M06.04]** UI — `ChatWidget`, `ChatPage`, `SellerChatPage`: trạng thái online / "Hoạt động x phút trước", badge `unread_total`, "Phản hồi 98% · trong vài phút" (ẩn khi `null`), nhãn "Tin nhắn tự động" cho `is_auto`; khách chưa đăng nhập bấm "Chat ngay" → câu UC-SOC-02 ngoại lệ + nút đăng nhập; tài khoản nội bộ ẩn nút chat. Kiểm 9 theme + khổ hẹp.
- **[M06.05]** `error-codes.ts` + vi / en — `GNE-SOC-4031`, `GNE-SOC-4096`, khóa `chat.*` mới; `check-i18n` xanh.
- **[M06.06]** Test — `chat.rules.test.ts` (khớp bong bóng theo `client_message_id`, thứ tự khi tin realtime đến trước phản hồi REST, suy trạng thái), `chat.mapper.test.ts`; `npm run verify`.
- **[M06.C1]** COMMIT — `feat(web): connect buyer and seller chat to the real api over websocket`

### M07 — Ảnh, thẻ sản phẩm, thẻ đơn (UC-SOC-02) — nhánh `feature/chat-cards-auto-reply`

- **[M07.01]** `ChatService.send` cho `IMAGE` (D7): kiểm khóa `tmp/chat-media/{me}/…`, `promoteTo`, presigned GET khi đọc (`MessageView.image_url`); xóa tệp nếu transaction rollback.
- **[M07.02]** `PRODUCT_CARD` (D8) qua `ChatProductLookup`; `ORDER_CARD` (D9) qua `ChatOrderLookup`; `MessageView` nạp thẻ theo lô (một lời gọi cổng cho cả trang tin).
- **[M07.03]** 8.6 — `GET /me/conversations/{id}/orders`, `GET /seller/conversations/{id}/orders` (`limit` 1–20).
- **[M07.04]** `ChatImageIT` — ảnh thật lên Silo → tin `IMAGE` có URL ký tải được; khóa `tmp/` của người khác → `422`; sai MIME / > 5 MB bị chặn ở bước xin link. `ChatCardsIT` — thẻ sản phẩm của shop khác → `4031`; biến thể không thuộc sản phẩm → `422`; sản phẩm bị ẩn sau khi gửi → `is_available = false`; thẻ đơn của khách khác → `4031`; 8.6 chỉ trả đơn đúng cặp, mới nhất trước.
- **[M07.05]** Web — gửi ảnh (`uploadFile(f, 'CHAT_IMAGE')`, xem trước, lỗi dung lượng / định dạng tại chỗ — UC-SOC-02 ngoại lệ), thẻ sản phẩm tự gửi khi mở từ trang sản phẩm (kèm `variant_id` đang chọn), nút "Gửi đơn" dùng 8.6, bấm thẻ mở `/p/{slug}-{id}` / `/orders/{id}` (shop: `/seller/orders`), 4 câu hỏi gợi ý nhanh (UC-SOC-02 ràng buộc).
- **[M07.C1]** COMMIT — `feat(social): send images, product cards and order cards in chat`
- **[M07.C2]** COMMIT — `feat(web): send chat images and product or order cards`

### M08 — Trả lời tự động & tỉ lệ phản hồi (UC-VND-06)

- **[M08.01]** `AutoReplyPolicy` (hàm thuần, D11) — `greetingFor(conv, shop, lastMessageAt, lastGreetingAt, now)`, `awayFor(shop, ownerOnline, lastAwayAt, now)` (khung 08:00–22:00 giờ VN, 60 phút), ưu tiên tạm nghỉ; `AutoReplyPolicyTest` bảng ca (biên 07:59 / 08:00 / 21:59 / 22:00, chủ online / offline, tắt công tắc, nội dung rỗng, tạm nghỉ đang / hết hiệu lực, 59 / 60 phút).
- **[M08.02]** Gắn vào `ChatService.open` (lời chào) và `send` (vắng mặt / tạm nghỉ khi **khách** gửi) — tin `is_auto`, đẩy realtime; tin tự động không tạo thông báo cho chủ shop.
- **[M08.03]** `ChatFacade.responseStats` (D12) — SQL một lần cho nhiều shop (cửa sổ 30 ngày, bỏ `is_auto`); `avg_response_minutes` cache 1 giờ; 8.2 `shop_stats`.
- **[M08.04]** `vendor/shop/application/ChatResponseRateJob` — 03:30 giờ VN, lô 100, ghi `chat_response_rate` (NULL khi < 5 hội thoại), `PublicShopService.evict(shopId)` sau khi đổi (cache trang shop B3).
- **[M08.05]** `AutoReplyIT` — bật lời chào → mở hội thoại mới nhận lời chào `is_auto`, mở lại ngay không nhận thêm; khách nhắn lúc 23:00 → nhận tin vắng mặt 1 lần, nhắn tiếp trong 60 phút không nhận thêm; chủ shop online lúc 10:00 → không có tin vắng mặt; tạm nghỉ → `vacation_message`; tắt công tắc → không gì cả. `ChatResponseRateJobIT` — 10 hội thoại, 8 được trả lời trong 24 giờ (tin tự động không tính) → `80.00`.
- **[M08.06]** Web — Seller › Cài đặt › Tự động trả lời (đã nối ở B4): kiểm lại lưu / lỗi `GNE-VND-4222` trên API thật; khung chat hiện nhãn "Tin nhắn tự động".
- **[M08.C1]** COMMIT — `feat(social): send shop greeting and away replies automatically`
- **[M08.C2]** COMMIT — `feat(vendor): compute daily chat response rate for shops`

### M09 — Kiểm thử & chạy thật

- **[M09.01]** `docs/kiem-thu/kich-ban-2026-11-26-chat.md` — TC-CHAT-01…: mở chat từ trang sản phẩm (thẻ sản phẩm tự gửi), từ trang shop, từ theo dõi đơn; hai trình duyệt (khách + chủ shop) gửi qua lại < 1 giây; Đang gửi → Đã nhận → Đã xem; đang gõ; ảnh hợp lệ / quá 5 MB / sai định dạng; thẻ đơn; lời chào; tin vắng mặt (chạy backend với giờ giả lập hoặc chủ shop offline); khách chưa đăng nhập; nhân viên; mất mạng rồi có lại (gửi bằng REST dự phòng, không trùng tin); badge Seller + chuông thông báo khi chủ shop offline; 9 theme; khổ hẹp.
- **[M09.02]** Chạy thật — web bản build `PUBLIC_REAL_API=…,notifications,chat` + backend `local` + Postgres + Redis + Silo; Chromium (Playwright) 2–3 ngữ cảnh song song; ghi số ca đạt; lỗi phát hiện sửa ngay (mỗi lỗi một commit `fix(…)`).
- **[M09.03]** Đo nhanh — 50 cặp hội thoại gửi đồng thời qua STOMP (script) → độ trễ p95 < 300 ms trên máy dev, không mất tin, bộ đếm khớp; ghi số vào progress.
- **[M09.C1]** COMMIT — `docs(social): add chat api tests and real-run scenarios`

### M10 — Hoàn thiện use case & tick tiến độ

- **[M10.01]** OpenAPI — đủ 12 endpoint REST, summary `UC-SOC-01` / `UC-SOC-02`; ghi chú STOMP ở API 13.
- **[M10.02]** `docs/web-backend-handoff.md` — nhóm `chat` đã nối; bỏ dòng 61, 75.
- **[M10.03]** Usecase.md — *Hiện trạng* UC-SOC-01, UC-SOC-02, UC-VND-06 → "Đã triển khai (2026-11-…)".
- **[M10.04]** `CHECKLIST_TIEN_DO_UC.md` — tick 5 lớp `UC-SOC-01`, `UC-SOC-02`, `UC-VND-06` (ô chưa đạt → `◐` + phần thiếu), Trạng thái ✅, **cột Phụ trách = Nguyễn Ngọc Thiện**, số mục 1 + phân hệ 05 và 08, dòng mục 5 *Nhật ký*; mục 3 gạch việc "badge chat" của B4 nếu có.
- **[M10.05]** `BAO_CAO_UC_DOC_LAP.md` — bảng trạng thái cụm B6 (4 dòng; UC-NOT-01 theo plan [1]), dòng *Phụ trách*, hiện trạng ở bảng *Thành viên*; mục 8 *Thứ tự đề xuất* ghi B6 xong.
- **[M10.06]** `Quản Lý UseCase.xlsx` — *Hiện trạng mã nguồn* 3 UC = "Đã triển khai", cột AA = "Nguyễn Ngọc Thiện" (giữ), sheet *Thành viên*.
- **[M10.07]** README ngày (mục 7) — tick dòng plan [2].
- **[M10.C1]** COMMIT — `docs(social): record live chat, chat cards and auto replies as implemented`
- **[M10.08]** Mở PR `feature/chat-live` (M01–M06) rồi `feature/chat-cards-auto-reply` (M07–M10) vào `develop`.

---

## 8. Ngoài phạm vi

- Email nhắc tin đầu ngày, trả lời theo từ khóa, thu hồi / sửa tin, chặn người dùng, nhiều ảnh / video — D20.
- Chế độ tạm nghỉ (bật / tắt — UC-VND-19): plan này chỉ **đọc** `vacation_*` để chọn tin tự động.
- Mã vận đơn trên thẻ đơn hàng — C4; danh sách "Đơn mua" đầy đủ — C5.

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Vòng phụ thuộc `order → vendor → social → order` | Cổng ở `social`, cài ở module kia bằng file mới (N2); `ModuleBoundaryTest` ở mọi cổng commit |
| Bộ đếm chưa đọc lệch khi hai bên gửi cùng lúc | `SELECT … FOR UPDATE` dòng hội thoại (N3); IT 50 tin song song |
| Trùng tin khi mạng chập chờn (STOMP rồi REST) | `client_message_id` + Redis `SET NX` (D4); IT gửi lại |
| Lộ ảnh chat | Bucket riêng tư + URL ký 10 phút (N6); khóa `tmp/` phải của người gửi |
| Spam tin vắng mặt | 1 lần / hội thoại / 60 phút (D11); test biên |
| Sửa nhầm file của C1 / C2 | Cài cổng bằng **file mới** (`ChatProductLookupImpl`, `ChatOrderLookupImpl`); không sửa `VariantFacade`, `OrderFacade` |
| Simple broker mất tin khi người nhận vừa rớt mạng | Tin luôn ở CSDL; web tải lại trang tin khi hub kết nối lại; thông báo `CHAT_MESSAGE` khi offline |
