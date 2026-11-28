# PROGRESS — Chat Khách ↔ Shop thời gian thực (UC-SOC-01 · UC-SOC-02 · UC-VND-06)

| | |
|---|---|
| **Plan** | [plan_chat_khach_shop.md](./plan_chat_khach_shop.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-26 |
| **Tổng** | 62 task + 17 commit, 11 giai đoạn |
| **Nhánh** | `feature/chat-live` (M00–M06) → `feature/chat-cards-auto-reply` (M07–M10) |
| **Phụ trách** | **Nguyễn Ngọc Thiện** — lập plan và thực hiện (nhận 2026-11-26) |
| **Người commit** | danh tính git đã cấu hình trong repo (`Thien211105`) — không `--author`, không `--no-verify`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-27 — xong toàn bộ M00–M10 (79 / 79), PR #116 + #117 (79 / 79) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục *Ghi chú*.
- Task `COMMIT` chỉ tick sau khi qua *Cổng kiểm tra* (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`. Thông điệp đã ghi sẵn — tiếng Anh, chữ thường, đúng `commitlint.config.cjs`; có mã Jira thì thêm ` [#GNE-<số>]` cuối.
- Sau mỗi buổi làm: cập nhật cột *Xong* của bảng Tổng quan, dòng *Cập nhật gần nhất*, một dòng *Nhật ký* cuối trang.
- Cột *Chạy* của use case chỉ tick khi đã chạy thật trên giao diện (Chromium điều khiển bằng Playwright, hệ thống thật) và đạt hết ca của kịch bản — ghi cách chạy + số ca vào *Ghi chú*.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | **Kênh gửi tin** | ☑ | Như đề xuất — STOMP `/app/chat.send` khi hub kết nối; không ack sau 5 giây → `POST …/messages` cùng `client_message_id`; lỗi → `MESSAGE_FAILED` / 2026-11-27 |
| D2 | **Redis Pub/Sub** (UC-SOC-01 b7, ràng buộc) | ☑ | Như đề xuất — simple broker một instance; Redis Pub/Sub là phần DATN / 2026-11-27 |
| D3 | **Trạng thái tin** `SENT / DELIVERED / SEEN` khi bảng không có `read_at` / `delivered_at` | ☑ | Như đề xuất — server chỉ trả `DELIVERED` / `SEEN` (`is_read`); `SENDING` / `FAILED` chỉ ở client / 2026-11-27 |
| D4 | **`client_message_id`** không có cột | ☑ | Như đề xuất — Redis `chat:cmid:{userId}:{uuid}` giữ chỗ `SET NX` rồi gắn `message_id` 24 giờ; gửi lại → `200` cùng tin; Redis lỗi → bỏ qua / 2026-11-27 |
| D5 | **Ai được chat** | ☑ | Như đề xuất — `@PreAuthorize(hasRole CUSTOMER)` (VENDOR kế thừa), STOMP từ chối STAFF / ADMIN; shop phải hoạt động (`404 GNE-VND-4041`), shop của mình `409 GNE-SOC-4096` / 2026-11-27 |
| D6 | **Nội dung tin chữ** | ☑ | Như đề xuất — bỏ ký tự điều khiển, cắt khoảng trắng, 1–2.000 ký tự; `rl:chat:{userId}` 20 / 10 giây / 2026-11-27 |
| D7 | **Ảnh** | ☑ | Như đề xuất — khóa `tmp/user/{me}/…` (đúng quy ước `UploadService`, không phải `tmp/chat-media/`) → `conversation/{id}/…`, xóa khi rollback; link ký 10 phút / 2026-11-27 |
| D8 | **Thẻ sản phẩm** — API gửi `product_id`, bảng lưu `product_variant_id` | ☑ | Như đề xuất — biến thể mặc định: còn hàng trước, rồi giá thấp nhất / 2026-11-27 |
| D9 | **Thẻ đơn hàng** + ô chọn đơn | ☑ | Như đề xuất — thêm 8.6 hai phía, `limit` 1–20 (mặc định 10) / 2026-11-27 |
| D10 | **Bộ đếm chưa đọc & đánh dấu đã xem** | ☑ | Như đề xuất — `UPDATE … RETURNING` lấy `up_to_message_id`; `NotificationFacade.markChatRead` / 2026-11-27 |
| D11 | **Tin tự động** (UC-VND-06 b4–5) | ☑ | Như đề xuất — mốc tin vắng mặt giữ ở Redis `chat:away:{id}` (lời chào không chặn tin vắng mặt; Redis lỗi → tin tự động gần nhất trong CSDL) / 2026-11-27 |
| D12 | **Tỉ lệ & thời gian phản hồi** ("Phản hồi 98% · trong vài phút" — API 8.2 `shop_stats`, trang shop B3) | ☑ | Như đề xuất — `ChatStats` (một SQL nhiều shop) + `ChatResponseRateJob` 03:30, `NULL` khi < 5 hội thoại; trung vị phút cache `chat:stats:{id}` 1 giờ / 2026-11-27 |
| D13 | **Trực tuyến** (`is_online`, `last_active_at` ở 8.1) | ☑ | Như đề xuất — `PresenceListener` ghi `presence:last:{userId}` 30 ngày khi ngắt kết nối / 2026-11-27 |
| D14 | **Đang gõ** | ☑ | Như đề xuất — chặn trùng bằng Redis `SET NX` 2 giây / 2026-11-27 |
| D15 | **Gói tin `/user/queue/chat`** | ☑ | Như đề xuất — 4 loại gói tin, `MESSAGE_FAILED` kèm `conversation_id` / 2026-11-27 |
| D16 | **Danh sách hội thoại** | ☑ | Như đề xuất — cursor `(last_message_at, id)`, `limit` 1–50 (mặc định 20) / 2026-11-27 |
| D17 | **Badge Seller Center** | ☑ | Như đề xuất — `vendor/shop/application/ChatBadgeContributor` / 2026-11-27 |
| D18 | **Tài khoản bị ẩn danh** (UC-AUTH-10) | ☑ | Như đề xuất — tên qua `UserFacade.findSummaries`; khách không còn ACTIVE → gửi `404` / 2026-11-27 |
| D19 | **Cờ nối web** | ☑ | Như đề xuất — cờ `chat` (`CHAT_MOCKED`), 8.6 không cần cờ `orders` / 2026-11-27 |
| D20 | **Ngoài phạm vi** | ☑ | Như đề xuất / 2026-11-27 |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| M00 | Chuẩn bị | 5 | 0 | 5 / 5 |
| M01 | Tài liệu trước | 3 | 1 | 4 / 4 |
| M02 | Hằng số dùng chung & cổng | 7 | 5 | 12 / 12 |
| M03 | Hội thoại & tin chữ (UC-SOC-01) | 10 | 1 | 11 / 11 |
| M04 | Realtime (UC-SOC-01 b5–8) | 4 | 1 | 5 / 5 |
| M05 | API REST hai phía + badge | 5 | 2 | 7 / 7 |
| M06 | Nối web chat (UC-SOC-01) | 6 | 1 | 7 / 7 |
| M07 | Ảnh, thẻ sản phẩm, thẻ đơn (UC-SOC-02) — nhánh `feature/chat-cards-auto-reply` | 5 | 2 | 7 / 7 |
| M08 | Trả lời tự động & tỉ lệ phản hồi (UC-VND-06) | 6 | 2 | 8 / 8 |
| M09 | Kiểm thử & chạy thật | 3 | 1 | 4 / 4 |
| M10 | Hoàn thiện use case & tick tiến độ | 8 | 1 | 9 / 9 |
| **Tổng** | | **62** | **17** | **79 / 79** |

## M00 — Chuẩn bị

- [x] **M00.01** Kiểm danh tính git; kiểm plan [1] đã merge N01 (có `messages.is_auto`, `V27`) và N03.04 / N05.02 (hoặc rebase lên nhánh plan [1] nếu chưa merge; ghi vào progress).
- [x] **M00.02** Chốt D1–D20 vào progress.
- [x] **M00.03** Tạo nhánh `feature/chat-live` từ `develop` mới nhất.
- [x] **M00.04** Đọc màn mock
- [x] **M00.05** Kiểm dữ liệu seed

## M01 — Tài liệu trước

- [x] **M01.01** API_SPEC 8
- [x] **M01.02** API_SPEC 13
- [x] **M01.03** Usecase.md
- [x] **M01.C1** 🔖 COMMIT `docs(social): specify live chat events, cards and auto replies` · hash: `eda5f69`

## M02 — Hằng số dùng chung & cổng

- [x] **M02.01** `ErrorCode` + `GNE-SOC-4031`, `GNE-SOC-4096` (khối `// ===== Tương tác`); `UploadPurpose.CHAT_IMAGE` (D7); `UserChannel.CHAT`.
- [x] **M02.02** Cổng trong `social.chat.application`
- [x] **M02.03** `vendor/shop/application/ChatShopDirectoryImpl`
- [x] **M02.04** `catalog/product/application/ChatProductLookupImpl`
- [x] **M02.05** `order/order/application/ChatOrderLookupImpl`
- [x] **M02.06** `common/realtime/PresenceListener` (D13)
- [x] **M02.07** Test
- [x] **M02.C1** 🔖 COMMIT `feat(common): add chat error codes, chat image purpose and presence tracking` · hash: `2877fa8`
- [x] **M02.C2** 🔖 COMMIT `feat(social): define chat ports for shops, products and orders` · hash: `2595078`
- [x] **M02.C3** 🔖 COMMIT `feat(vendor): expose shop chat profile to the chat module` · hash: `a0a641f`
- [x] **M02.C4** 🔖 COMMIT `feat(catalog): resolve product cards for chat messages` · hash: `d91ea8a`
- [x] **M02.C5** 🔖 COMMIT `feat(order): resolve order cards between a buyer and a shop for chat` · hash: `9d08a5c`

## M03 — Hội thoại & tin chữ (UC-SOC-01)

- [x] **M03.01** `Conversation`
- [x] **M03.02** `Message`
- [x] **M03.03** `ConversationRepository`
- [x] **M03.04** `ChatCursor` (base64url `{t, id}`); cursor hỏng → 422; `ChatCursorTest`.
- [x] **M03.05** `ChatService.open(me, shopId)` (3.1, chưa gồm lời chào
- [x] **M03.06** `ChatIdempotency` (D4) + rate limit (N8)
- [x] **M03.07** `ChatQueryService`
- [x] **M03.08** `ConversationIT`
- [x] **M03.09** `ChatMessageIT`
- [x] **M03.10** `ChatReadIT`
- [x] **M03.C1** 🔖 COMMIT `feat(social): open conversations and exchange text messages with unread counters` · hash: `cff77be`

## M04 — Realtime (UC-SOC-01 b5–8)

- [x] **M04.01** `ChatRealtime`
- [x] **M04.02** `ChatStompController`
- [x] **M04.03** Người nhận offline → `NotificationFacade.upsertChat(…)` (plan [1] N03.04) với `link.type = CONVERSATION`; online thì không tạo thông báo.
- [x] **M04.04** `ChatStompIT`
- [x] **M04.C1** 🔖 COMMIT `feat(social): deliver chat messages, read receipts and typing over websocket` · hash: `8d83bfd`

## M05 — API REST hai phía + badge

- [x] **M05.01** `MeConversationController`
- [x] **M05.02** `SellerConversationController`
- [x] **M05.03** `ChatFacade`
- [x] **M05.04** `ChatSecurityIT`
- [x] **M05.05** `docs/api-tests/chat.http`
- [x] **M05.C1** 🔖 COMMIT `feat(social): expose buyer and seller conversation endpoints` · hash: `51c4356`
- [x] **M05.C2** 🔖 COMMIT `feat(vendor): show unread chat count on the seller center badge` · hash: `668d083`

## M06 — Nối web chat (UC-SOC-01)

- [x] **M06.01** `chat/infrastructure`
- [x] **M06.02** `application/chatRealtime.ts`
- [x] **M06.03** `chatSend.ts`
- [x] **M06.04** UI
- [x] **M06.05** `error-codes.ts` + vi / en
- [x] **M06.06** Test
- [x] **M06.C1** 🔖 COMMIT `feat(web): connect buyer and seller chat to the real api over websocket` · hash: `0018b21`

## M07 — Ảnh, thẻ sản phẩm, thẻ đơn (UC-SOC-02) — nhánh `feature/chat-cards-auto-reply`

- [x] **M07.01** `ChatService.send` cho `IMAGE` (D7): kiểm khóa `tmp/chat-media/{me}/…`, `promoteTo`, presigned GET khi đọc (`MessageView.image_url`); xóa tệp nếu transaction rollback.
- [x] **M07.02** `PRODUCT_CARD` (D8) qua `ChatProductLookup`; `ORDER_CARD` (D9) qua `ChatOrderLookup`; `MessageView` nạp thẻ theo lô (một lời gọi cổng cho cả trang tin).
- [x] **M07.03** 8.6
- [x] **M07.04** `ChatImageIT`
- [x] **M07.05** Web
- [x] **M07.C1** 🔖 COMMIT `feat(social): send images, product cards and order cards in chat` · hash: `6e2b7cc`
- [x] **M07.C2** 🔖 COMMIT `feat(web): send chat images and product or order cards` · hash: `a421a87`

## M08 — Trả lời tự động & tỉ lệ phản hồi (UC-VND-06)

- [x] **M08.01** `AutoReplyPolicy` (hàm thuần, D11)
- [x] **M08.02** Gắn vào `ChatService.open` (lời chào) và `send` (vắng mặt / tạm nghỉ khi **khách** gửi)
- [x] **M08.03** `ChatFacade.responseStats` (D12)
- [x] **M08.04** `vendor/shop/application/ChatResponseRateJob`
- [x] **M08.05** `AutoReplyIT`
- [x] **M08.06** Web
- [x] **M08.C1** 🔖 COMMIT `feat(social): send shop greeting and away replies automatically` · hash: `898ae64`
- [x] **M08.C2** 🔖 COMMIT `feat(vendor): compute daily chat response rate for shops` · hash: `3394a4a`

## M09 — Kiểm thử & chạy thật

- [x] **M09.01** `docs/kiem-thu/kich-ban-2026-11-26-chat.md`
- [x] **M09.02** Chạy thật
- [x] **M09.03** Đo nhanh
- [x] **M09.C1** 🔖 COMMIT `docs(social): add chat api tests and real-run scenarios` · hash: `916b0c9`

## M10 — Hoàn thiện use case & tick tiến độ

- [x] **M10.01** OpenAPI
- [x] **M10.02** `docs/web-backend-handoff.md`
- [x] **M10.03** Usecase.md
- [x] **M10.04** `CHECKLIST_TIEN_DO_UC.md`
- [x] **M10.05** `BAO_CAO_UC_DOC_LAP.md`
- [x] **M10.06** `Quản Lý UseCase.xlsx`
- [x] **M10.07** README ngày (mục 7)
- [x] **M10.C1** 🔖 COMMIT `docs(social): record live chat, chat cards and auto replies as implemented` · hash: `50bedc8`
- [x] **M10.08** Mở PR `feature/chat-live` (M01–M06) rồi `feature/chat-cards-auto-reply` (M07–M10) vào `develop`.

---

## Chốt plan — tick tiến độ use case (người phụ trách: Nguyễn Ngọc Thiện)

Làm **ngay khi xong plan** (hoặc xong từng use case), trong [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../../docs/Document/CHECKLIST_TIEN_DO_UC.md) — cách tick xem mục 0 của file đó. Use case chỉ ✅ khi đủ 5 lớp GD / BE / Test / Nối / Chạy (UC chạy nền: đủ các ô không phải `—`).

- [ ] `UC-SOC-01` Nhắn tin trò chuyện thời gian thực Khách – Shop (Live Chat) — tick GD / BE / Test / Nối / Chạy, Trạng thái **✅ Đã triển khai**, cột **Phụ trách = Nguyễn Ngọc Thiện**
- [ ] `UC-SOC-02` Gửi thẻ sản phẩm, hình ảnh và trạng thái đơn hàng trong chat — tick GD / BE / Test / Nối / Chạy, Trạng thái **✅ Đã triển khai**, cột **Phụ trách = Nguyễn Ngọc Thiện**
- [ ] `UC-VND-06` Cấu hình tin nhắn trả lời tự động (Auto-reply) — tick GD / BE / Test / Nối / Chạy, Trạng thái **✅ Đã triển khai**, cột **Phụ trách = Nguyễn Ngọc Thiện**
- [ ] Cập nhật số ở mục 1 *Tổng quan* (Đã triển khai / Đang triển khai / Chưa có backend) + bảng *Tiến độ theo phân hệ* (phân hệ 05 (Gian hàng & KYC) và 08 (Chat, Đánh giá & Theo dõi shop))
- [ ] Thêm một dòng vào mục 5 *Nhật ký cập nhật* của checklist (ngày · use case · việc đã làm · mã commit / PR)
- [ ] Bảng *Thành viên & phân công* (đầu checklist **và** đầu `BAO_CAO_UC_DOC_LAP.md`) — cột *Hiện trạng* của Nguyễn Ngọc Thiện ghi các use case vừa xong
- [ ] `BAO_CAO_UC_DOC_LAP.md` — bảng trạng thái của cụm / dòng use case (✅ + *Còn lại*), dòng *Phụ trách: Nguyễn Ngọc Thiện*
- [ ] `Quản Lý UseCase.xlsx` — cột *Hiện trạng mã nguồn* = "Đã triển khai", cột AA *Người phụ trách* = "Nguyễn Ngọc Thiện", sheet *Thành viên* (sửa XML, chuỗi `&#NNNN;`)
- [ ] Câu *Hiện trạng* của từng use case trong `Usecase.md`
- [ ] Tick dòng **[2] Chat Khách – Shop** ở bảng mục 7 của [README](./README.md)

## Ghi chú

Ghi ở đây: lý do bỏ qua task (`[-]`), chỗ làm khác plan, số đo, số migration thật, cách chạy thật + số ca đạt, việc tồn chuyển sang plan khác.

| Task | Ghi chú |
|---|---|
| M10.08 | PR #116 `feature/chat-live` (M01–M06, nền `feature/notification-platform-broadcast` #115) → PR #117 `feature/chat-cards-auto-reply` (M07–M10, nền `feature/chat-live`) — xếp chồng, đổi nền sang `develop` sau mỗi lần merge |
| M08.06 | Chạy thật TC-CHAT-01: bật công tắc + lưu lời chào / tin vắng mặt qua `PATCH /seller/settings` thật → toast "Đã lưu cấu hình tự động trả lời", CSDL đúng; khung chat hiện nhãn "Tin nhắn tự động" |
| M09.02 | Chromium (Playwright) 3 ngữ cảnh, web build `PUBLIC_REAL_API=…,notifications,platform-notifications,chat` + backend `local` (`PUBLIC_S3_URL=http://localhost:9000`) + Postgres + Redis + Silo + Mailpit — **15 / 15 ca đạt**. Lỗi phát hiện và đã sửa: ảnh GIF / > 5 MB không báo lỗi tại chỗ (`a526359`); badge Chat Seller không tự đổi + nhãn "đang gõ" phía shop sai (`9241f13`). Lỗi của chính kịch bản (chữ xem trước ở hộp thư trùng với bong bóng, popup banner che nút chat) đã chỉnh trong script |
| M09.03 | 50 cặp khách ↔ 10 shop × 5 tin qua `/app/chat.send` (250 tin đồng thời): nhận 250 / 250, lưu 250, bộ đếm đúng 50 / 50; p50 161 ms, **p95 235 ms**, max 247 ms (< 300 ms). Token ký bằng khóa JWT máy dev (giới hạn đăng nhập 10 lần / phút / IP) |
| M10.01 | `/v3/api-docs`: 11 endpoint REST chat (plan ước 12 — 8.2 chỉ có phía khách), summary `UC-SOC-01` / `UC-SOC-02`; STOMP ghi ở API 13 |
| M10.02–M10.07 | Handoff: dòng chat realtime + `mockTyping` gạch (đã nối), nhóm `chat` ghi "đã nối". Usecase: *Hiện trạng* 3 UC. Checklist: 3 UC ✅ (5 lớp), Đã triển khai 26 → 29, mock 35 → 32, phân hệ 05: 8 / 0 / 3, phân hệ 08: 2 / 1 / 4, bảng Thành viên, Nhật ký. BAO_CAO: bảng cụm B6 (4 / 4 ✅), mục 8. Excel: cột S 3 dòng (50, 70, 71) = "Đã triển khai", cột AA giữ "Nguyễn Ngọc Thiện", sheet Thành viên C7. README ngày: tick plan [2]. Backend `./mvnw clean verify` 254 unit + 432 IT xanh; web `npm run verify` 78 file test xanh |
| M00.01 | Nhánh xếp chồng trên `feature/notification-platform-broadcast` (plan [1] chưa merge — PR #112 / #114 / #115); có sẵn `V27` (`messages.is_auto`), `NotificationFacade.upsertChat`, hub `send` |
| M00.04 | DTO mock của web đã khớp API 8 (`counterpart`, `last_message`, `client_message_id`); thêm `is_auto`, `product.is_available` / `variant_id`, `order.status` |
| M00.05 | Seed không có khách thuần — chạy thật dùng `owner.shoe@genie.example` (chủ shop chat với shop KHÁC — D5) làm người mua ↔ `mai.tran@gmail.com` (TechZone, shop_id 1) |
| M02–M08 | Test: `ChatPortsIT` 7, `ChatConversationIT` 13, `ChatStompIT` 7 (STOMP thật), `ChatSecurityIT` 5, `ChatCardsIT` 4 (Silo thật), `AutoReplyIT` 5, `ChatResponseRateJobIT` 1; unit `ConversationTest` 4, `ChatRulesTest` 4, `AutoReplyPolicyTest` 11; web `chat.rules.test.ts` 7, `chat.mapper.test.ts` 3 |
| M03.07 | `ChatStats` (trung vị phút cho `shop_stats`) viết luôn ở M03 vì 8.2 cần; M08 dùng lại cho job |
| M07.05 | Thẻ sản phẩm tự gửi từ trang sản phẩm dùng biến thể mặc định (link `/chat?shop&product` chưa mang `variant`); 4 câu gợi ý nhanh đã có từ G10 |

## Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-26 | Lập plan + progress (62 task, 17 commit, 11 giai đoạn) — chưa bắt đầu làm | — |
| 2026-11-27 | M00–M05: cổng shop / sản phẩm / đơn, hội thoại + tin chữ (khóa dòng, chống trùng, rate limit, cursor), STOMP `/app/chat.*` + thông báo khi offline, REST hai phía + badge Seller | `2877fa8` … `668d083` |
| 2026-11-27 | M06: web nối thật cờ `chat` qua hub WebSocket (REST dự phòng, gửi lại, tải tin cũ, đang gõ, hoạt động lần cuối) | `0018b21` |
| 2026-11-27 | M07–M08 (nhánh `feature/chat-cards-auto-reply`): ảnh riêng tư + thẻ sản phẩm / đơn + 8.6; lời chào / vắng mặt / tạm nghỉ, job tỉ lệ phản hồi 03:30 | `6e2b7cc` `a421a87` `898ae64` `3394a4a` |
| 2026-11-27 | M09: kịch bản TC-CHAT-00…15, chạy thật 15 / 15 (Playwright), đo 50 cặp p95 235 ms; sửa 2 lỗi phát hiện khi chạy | `a526359` `9241f13` `916b0c9` |
| 2026-11-27 | M10: OpenAPI, handoff, Usecase, checklist, báo cáo độc lập, Excel, README — **UC-SOC-01, UC-SOC-02, UC-VND-06 ✅ Đã triển khai** | `50bedc8` |
| 2026-11-27 | Mở PR #116, #117 — **plan [2] xong 79 / 79** | #116 #117 |
