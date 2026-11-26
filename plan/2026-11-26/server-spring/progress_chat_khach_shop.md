# PROGRESS — Chat Khách ↔ Shop thời gian thực (UC-SOC-01 · UC-SOC-02 · UC-VND-06)

| | |
|---|---|
| **Plan** | [plan_chat_khach_shop.md](./plan_chat_khach_shop.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-26 |
| **Tổng** | 62 task + 17 commit, 11 giai đoạn |
| **Nhánh** | `feature/chat-live` (M00–M06) → `feature/chat-cards-auto-reply` (M07–M10) |
| **Phụ trách** | **Nguyễn Ngọc Thiện** — lập plan và thực hiện (nhận 2026-11-26) |
| **Người commit** | danh tính git đã cấu hình trong repo (`Thien211105`) — không `--author`, không `--no-verify`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-26 — mới lập plan (0 / 79) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục *Ghi chú*.
- Task `COMMIT` chỉ tick sau khi qua *Cổng kiểm tra* (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`. Thông điệp đã ghi sẵn — tiếng Anh, chữ thường, đúng `commitlint.config.cjs`; có mã Jira thì thêm ` [#GNE-<số>]` cuối.
- Sau mỗi buổi làm: cập nhật cột *Xong* của bảng Tổng quan, dòng *Cập nhật gần nhất*, một dòng *Nhật ký* cuối trang.
- Cột *Chạy* của use case chỉ tick khi đã chạy thật trên giao diện (Chromium điều khiển bằng Playwright, hệ thống thật) và đạt hết ca của kịch bản — ghi cách chạy + số ca vào *Ghi chú*.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | **Kênh gửi tin** | ☐ | |
| D2 | **Redis Pub/Sub** (UC-SOC-01 b7, ràng buộc) | ☐ | |
| D3 | **Trạng thái tin** `SENT / DELIVERED / SEEN` khi bảng không có `read_at` / `delivered_at` | ☐ | |
| D4 | **`client_message_id`** không có cột | ☐ | |
| D5 | **Ai được chat** | ☐ | |
| D6 | **Nội dung tin chữ** | ☐ | |
| D7 | **Ảnh** | ☐ | |
| D8 | **Thẻ sản phẩm** — API gửi `product_id`, bảng lưu `product_variant_id` | ☐ | |
| D9 | **Thẻ đơn hàng** + ô chọn đơn | ☐ | |
| D10 | **Bộ đếm chưa đọc & đánh dấu đã xem** | ☐ | |
| D11 | **Tin tự động** (UC-VND-06 b4–5) | ☐ | |
| D12 | **Tỉ lệ & thời gian phản hồi** ("Phản hồi 98% · trong vài phút" — API 8.2 `shop_stats`, trang shop B3) | ☐ | |
| D13 | **Trực tuyến** (`is_online`, `last_active_at` ở 8.1) | ☐ | |
| D14 | **Đang gõ** | ☐ | |
| D15 | **Gói tin `/user/queue/chat`** | ☐ | |
| D16 | **Danh sách hội thoại** | ☐ | |
| D17 | **Badge Seller Center** | ☐ | |
| D18 | **Tài khoản bị ẩn danh** (UC-AUTH-10) | ☐ | |
| D19 | **Cờ nối web** | ☐ | |
| D20 | **Ngoài phạm vi** | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| M00 | Chuẩn bị | 5 | 0 | 0 / 5 |
| M01 | Tài liệu trước | 3 | 1 | 0 / 4 |
| M02 | Hằng số dùng chung & cổng | 7 | 5 | 0 / 12 |
| M03 | Hội thoại & tin chữ (UC-SOC-01) | 10 | 1 | 0 / 11 |
| M04 | Realtime (UC-SOC-01 b5–8) | 4 | 1 | 0 / 5 |
| M05 | API REST hai phía + badge | 5 | 2 | 0 / 7 |
| M06 | Nối web chat (UC-SOC-01) | 6 | 1 | 0 / 7 |
| M07 | Ảnh, thẻ sản phẩm, thẻ đơn (UC-SOC-02) — nhánh `feature/chat-cards-auto-reply` | 5 | 2 | 0 / 7 |
| M08 | Trả lời tự động & tỉ lệ phản hồi (UC-VND-06) | 6 | 2 | 0 / 8 |
| M09 | Kiểm thử & chạy thật | 3 | 1 | 0 / 4 |
| M10 | Hoàn thiện use case & tick tiến độ | 8 | 1 | 0 / 9 |
| **Tổng** | | **62** | **17** | **0 / 79** |

## M00 — Chuẩn bị

- [ ] **M00.01** Kiểm danh tính git; kiểm plan [1] đã merge N01 (có `messages.is_auto`, `V26`) và N03.04 / N05.02 (hoặc rebase lên nhánh plan [1] nếu chưa merge; ghi vào progress).
- [ ] **M00.02** Chốt D1–D20 vào progress.
- [ ] **M00.03** Tạo nhánh `feature/chat-live` từ `develop` mới nhất.
- [ ] **M00.04** Đọc màn mock
- [ ] **M00.05** Kiểm dữ liệu seed

## M01 — Tài liệu trước

- [ ] **M01.01** API_SPEC 8
- [ ] **M01.02** API_SPEC 13
- [ ] **M01.03** Usecase.md
- [ ] **M01.C1** 🔖 COMMIT `docs(social): specify live chat events, cards and auto replies` · hash: `…`

## M02 — Hằng số dùng chung & cổng

- [ ] **M02.01** `ErrorCode` + `GNE-SOC-4031`, `GNE-SOC-4096` (khối `// ===== Tương tác`); `UploadPurpose.CHAT_IMAGE` (D7); `UserChannel.CHAT`.
- [ ] **M02.02** Cổng trong `social.chat.application`
- [ ] **M02.03** `vendor/shop/application/ChatShopDirectoryImpl`
- [ ] **M02.04** `catalog/product/application/ChatProductLookupImpl`
- [ ] **M02.05** `order/order/application/ChatOrderLookupImpl`
- [ ] **M02.06** `common/realtime/PresenceListener` (D13)
- [ ] **M02.07** Test
- [ ] **M02.C1** 🔖 COMMIT `feat(common): add chat error codes, chat image purpose and presence tracking` · hash: `…`
- [ ] **M02.C2** 🔖 COMMIT `feat(social): define chat ports for shops, products and orders` · hash: `…`
- [ ] **M02.C3** 🔖 COMMIT `feat(vendor): expose shop chat profile to the chat module` · hash: `…`
- [ ] **M02.C4** 🔖 COMMIT `feat(catalog): resolve product cards for chat messages` · hash: `…`
- [ ] **M02.C5** 🔖 COMMIT `feat(order): resolve order cards between a buyer and a shop for chat` · hash: `…`

## M03 — Hội thoại & tin chữ (UC-SOC-01)

- [ ] **M03.01** `Conversation`
- [ ] **M03.02** `Message`
- [ ] **M03.03** `ConversationRepository`
- [ ] **M03.04** `ChatCursor` (base64url `{t, id}`); cursor hỏng → 422; `ChatCursorTest`.
- [ ] **M03.05** `ChatService.open(me, shopId)` (3.1, chưa gồm lời chào
- [ ] **M03.06** `ChatIdempotency` (D4) + rate limit (N8)
- [ ] **M03.07** `ChatQueryService`
- [ ] **M03.08** `ConversationIT`
- [ ] **M03.09** `ChatMessageIT`
- [ ] **M03.10** `ChatReadIT`
- [ ] **M03.C1** 🔖 COMMIT `feat(social): open conversations and exchange text messages with unread counters` · hash: `…`

## M04 — Realtime (UC-SOC-01 b5–8)

- [ ] **M04.01** `ChatRealtime`
- [ ] **M04.02** `ChatStompController`
- [ ] **M04.03** Người nhận offline → `NotificationFacade.upsertChat(…)` (plan [1] N03.04) với `link.type = CONVERSATION`; online thì không tạo thông báo.
- [ ] **M04.04** `ChatStompIT`
- [ ] **M04.C1** 🔖 COMMIT `feat(social): deliver chat messages, read receipts and typing over websocket` · hash: `…`

## M05 — API REST hai phía + badge

- [ ] **M05.01** `MeConversationController`
- [ ] **M05.02** `SellerConversationController`
- [ ] **M05.03** `ChatFacade`
- [ ] **M05.04** `ChatSecurityIT`
- [ ] **M05.05** `docs/api-tests/chat.http`
- [ ] **M05.C1** 🔖 COMMIT `feat(social): expose buyer and seller conversation endpoints` · hash: `…`
- [ ] **M05.C2** 🔖 COMMIT `feat(vendor): show unread chat count on the seller center badge` · hash: `…`

## M06 — Nối web chat (UC-SOC-01)

- [ ] **M06.01** `chat/infrastructure`
- [ ] **M06.02** `application/chatRealtime.ts`
- [ ] **M06.03** `chatSend.ts`
- [ ] **M06.04** UI
- [ ] **M06.05** `error-codes.ts` + vi / en
- [ ] **M06.06** Test
- [ ] **M06.C1** 🔖 COMMIT `feat(web): connect buyer and seller chat to the real api over websocket` · hash: `…`

## M07 — Ảnh, thẻ sản phẩm, thẻ đơn (UC-SOC-02) — nhánh `feature/chat-cards-auto-reply`

- [ ] **M07.01** `ChatService.send` cho `IMAGE` (D7): kiểm khóa `tmp/chat-media/{me}/…`, `promoteTo`, presigned GET khi đọc (`MessageView.image_url`); xóa tệp nếu transaction rollback.
- [ ] **M07.02** `PRODUCT_CARD` (D8) qua `ChatProductLookup`; `ORDER_CARD` (D9) qua `ChatOrderLookup`; `MessageView` nạp thẻ theo lô (một lời gọi cổng cho cả trang tin).
- [ ] **M07.03** 8.6
- [ ] **M07.04** `ChatImageIT`
- [ ] **M07.05** Web
- [ ] **M07.C1** 🔖 COMMIT `feat(social): send images, product cards and order cards in chat` · hash: `…`
- [ ] **M07.C2** 🔖 COMMIT `feat(web): send chat images and product or order cards` · hash: `…`

## M08 — Trả lời tự động & tỉ lệ phản hồi (UC-VND-06)

- [ ] **M08.01** `AutoReplyPolicy` (hàm thuần, D11)
- [ ] **M08.02** Gắn vào `ChatService.open` (lời chào) và `send` (vắng mặt / tạm nghỉ khi **khách** gửi)
- [ ] **M08.03** `ChatFacade.responseStats` (D12)
- [ ] **M08.04** `vendor/shop/application/ChatResponseRateJob`
- [ ] **M08.05** `AutoReplyIT`
- [ ] **M08.06** Web
- [ ] **M08.C1** 🔖 COMMIT `feat(social): send shop greeting and away replies automatically` · hash: `…`
- [ ] **M08.C2** 🔖 COMMIT `feat(vendor): compute daily chat response rate for shops` · hash: `…`

## M09 — Kiểm thử & chạy thật

- [ ] **M09.01** `docs/kiem-thu/kich-ban-2026-11-26-chat.md`
- [ ] **M09.02** Chạy thật
- [ ] **M09.03** Đo nhanh
- [ ] **M09.C1** 🔖 COMMIT `docs(social): add chat api tests and real-run scenarios` · hash: `…`

## M10 — Hoàn thiện use case & tick tiến độ

- [ ] **M10.01** OpenAPI
- [ ] **M10.02** `docs/web-backend-handoff.md`
- [ ] **M10.03** Usecase.md
- [ ] **M10.04** `CHECKLIST_TIEN_DO_UC.md`
- [ ] **M10.05** `BAO_CAO_UC_DOC_LAP.md`
- [ ] **M10.06** `Quản Lý UseCase.xlsx`
- [ ] **M10.07** README ngày (mục 7)
- [ ] **M10.C1** 🔖 COMMIT `docs(social): record live chat, chat cards and auto replies as implemented` · hash: `…`
- [ ] **M10.08** Mở PR `feature/chat-live` (M01–M06) rồi `feature/chat-cards-auto-reply` (M07–M10) vào `develop`.

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

## Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-26 | Lập plan + progress (62 task, 17 commit, 11 giai đoạn) — chưa bắt đầu làm | — |
