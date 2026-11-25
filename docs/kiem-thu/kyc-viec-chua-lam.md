# KYC — những gì CHƯA làm / chưa kiểm thật (rà ngày 2026-11-22)

> Rà bằng cách đối chiếu [../kyc-giai-thich-cho-nguoi-moi.md](../kyc-giai-thich-cho-nguoi-moi.md) với code thật (backend `vendor/`, AI Worker
> `server-python/`, web `features/seller-onboarding`, `admin-kyc`, `seller-settings`).
> Kịch bản kiểm thử phần **đã làm**: [kich-ban-2026-11-16-mo-shop-kyc.md](kich-ban-2026-11-16-mo-shop-kyc.md).

## 1. Đã làm đủ (để đối chiếu)

Mở shop có OTP (email shop = email tài khoản) · nộp hồ sơ / lưu nháp · kiểm CCCD, MST, giấy tờ bắt buộc · mã hóa AES-256-GCM · link xem 5 phút + nhật ký
`KYC_DOCUMENT_VIEWED` · gửi AI **sau commit** · chế độ stub · chấm điểm 11 tín hiệu + 3 ngưỡng · lấy mẫu hậu kiểm 5% · `DUPLICATE_ID`,
`DECLARED_DATA_MISMATCH` phía backend · không đồng ý sinh trắc → luôn duyệt tay · AI lỗi → duyệt tay ngay · job AI quá 30 giây · job hết hạn
bổ sung 7 ngày (01:00) · job dọn bản xem tạm · nhân viên Duyệt / Từ chối (6 lý do) / Yêu cầu bổ sung / Đưa lại hàng chờ · hoa hồng riêng
(quyền `UPDATE_COMMISSION`, 0–50%) · nhãn Chính hãng · xin xem xét 1 lần · email kết quả (duyệt / từ chối / bổ sung) · tài khoản nhận tiền
có OTP bước 2 · cài đặt shop. Web gọi đủ 6 API admin + 4 API người bán + API cài đặt / tài khoản nhận tiền.

UC-VND-01, 02, 03, 04, 07, 21 **đã chạy thật 2026-11-25** (Chromium / Playwright + AI Worker thật với ảnh CCCD thật) → ✅ trong `CHECKLIST_TIEN_DO_UC.md`; UC-VND-05 (kho lấy hàng) cũng xong cùng ngày.

## 2. Chưa làm — code mới có "cổng chờ" (port) hoặc trả `null`

| # | Chức năng | Hiện trạng trong code | Lý do / chờ ai | Ảnh hưởng khi test |
|---|---|---|---|---|
| 1 | **`FACE_DUPLICATE`** — khuôn mặt trùng người đã có shop khác | AI luôn trả `face_duplicate = null` (`server-python/app/kyc/scoring.py`), không bao giờ sinh tín hiệu | Quyết định **Y-D4**: cần kho vector khuôn mặt (dữ liệu sinh trắc — Nghị định 13/2023), xếp cho **DATN**. Tài liệu còn mâu thuẫn chỗ lưu: ENTITY.md (dòng ~1156) nói "lưu trong AI Worker", plan Y-D4 nói "cột vector mã hóa ở `kyc_results`" — phải chốt trước khi làm | Một người dùng 2 CCCD khác nhau nhưng cùng khuôn mặt **không bị phát hiện** |
| 2 | **Báo nhân viên khi có hồ sơ mới** (chuông `/admin`) | Cổng `KycAdminNotifier` **không có class cài đặt** → chỉ ghi log | Chờ module `notification` (`AdminNotification` loại `USER_APPROVAL`) | Nhân viên phải tự mở `/admin/kyc` mới biết |
| 3 | **Thông báo in-app cho người bán** | `KycNotifier` chỉ gửi **email** | Chờ module `notification` | Chuông thông báo người bán không có tin KYC |
| 4 | **Tra tên chủ tài khoản ngân hàng thật** | `MockBankAccountLookup` — trả lại tên người dùng nhập (bỏ dấu, viết hoa); số TK đuôi `000` → tên khác | Chưa tích hợp đối tác (VietQR / NAPAS…) | Kiểm "chính chủ" trên máy dev là giả lập |
| 5 | **Chặn đổi / xóa tài khoản nhận tiền khi còn lệnh rút chờ** (`GNE-WAL-4091`) | Cổng `PayoutAccountGuard` **chưa có cài đặt** → không chặn | Chờ module `payment` (rút tiền) | Không test được `GNE-WAL-4091` |
| 6 | **Tạo ví shop khi mở shop** | `ShopBootstrapContributor`: shipping đã cài, **payment chưa** | Chờ module `payment` | Shop mới chưa có ví |
| 7 | **Badge số việc chờ ở Seller Center** | `SellerBadgeContributor` **chưa có cài đặt** | Chờ module order / chat | Badge trống |
| 8 | ~~Kho lấy hàng GHN (UC-VND-05)~~ **đã làm 2026-11-25** (`PUT /seller/pickup-address`, GHN giả lập mặc định); còn **chế độ tạm nghỉ** | Backend **chưa có** `/seller/pickup-address`, `/seller/vacation` (cổng KYC đã chừa đường); web chạy mock (`pickup`, `vacation`) | Chờ danh mục hành chính GHN (A12) | Không test trên dữ liệu thật |
| 9 | **Kiểm quốc huy, vân tay** trên CCCD | AI luôn trả `national_emblem = null`, `fingerprint = null` | **Y-D5**, DATN | — |
| 10 | **Liveness chủ động** (chớp mắt / quay đầu), **đọc chip NFC**, **đối chiếu VNeID / C06** | Chỉ có liveness **thụ động** trên 1 ảnh selfie | DATN | Ảnh in / chụp màn hình tinh vi có thể lọt |

## 3. Có rồi nhưng chưa kiểm thật / còn thiếu sót nhỏ

| # | Việc | Chi tiết | Đề xuất |
|---|---|---|---|
| A | ~~AI Worker chế độ `http` chưa chạy thật cùng backend~~ **Đã chạy 2026-11-25** | Mới có `pytest` của worker (thẻ CCCD giả tự dựng) và test backend dùng stub / client giả. Chưa ai chạy cả vòng: link 5 phút → worker tải ảnh → callback → token nội bộ | Chạy giai đoạn **K** của kịch bản ([hướng dẫn mục 6.2](../huong-dan-chay-local.md)) |
| B | ~~Trang `/seller/kyc` không tự làm mới~~ **Đã sửa** | Nút "Yêu cầu nhân viên xem xét" không hiện vì trang không làm mới sau khi AI chấm | PR `bugfix/kyc-real-photo-pipeline`: tự làm mới 3 giây khi `PENDING` / `AI_PROCESSING` |
| F | ~~AI thật đọc sai ảnh điện thoại thật~~ **Đã sửa** | Ảnh CCCD rõ vẫn bị từ chối 120 điểm: QR (OpenCV), MRZ (`1DVNM`), dò mặt ảnh lớn / thẻ xoay, độ nét đo cả khung | PR `bugfix/kyc-real-photo-pipeline` (plan `2026-11-22/server-python/plan_sua_kyc_anh_that.md`): cùng bộ ảnh → 0 điểm, tự duyệt |
| G | **Ảnh nhòe chỉ biết sau khi gửi** | Web chỉ chặn ảnh < 20 KB lúc tải; ảnh nhòe dung lượng lớn vẫn tải lên, AI mới bắt (`UNREADABLE_IMAGE`) | Đo độ nét ngay trên trình duyệt (canvas, cùng công thức AI) → cảnh báo chụp lại, vẫn cho gửi |
| C | **Shop có chủ bị khóa** vẫn `is_purchasable = true` trong wishlist | Đúng spec hiện tại (spec chỉ nêu không còn bán / hết hàng / shop nghỉ) nhưng về nghiệp vụ shop bị khóa không nên bán | Bàn với nhóm → bổ sung spec rồi sửa |
| D | **Bộ ảnh CCCD mẫu để demo AI thật** | Không được commit giấy tờ thật; ảnh bất kỳ luôn ra `UNREADABLE_IMAGE` | Dùng `tests/fixtures/build_card.py` sinh thẻ giả có QR + MRZ đúng để demo |
| E | **Hồ sơ có thể kẹt mãi ở `PENDING`** | Nộp hồ sơ: transaction commit `PENDING` → sự kiện **trong bộ nhớ** (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`) mới chuyển `AI_PROCESSING` và gọi AI (`KycAiDispatcher`). Backend sập / khởi động lại **đúng khoảng giữa** → sự kiện mất, hồ sơ nằm `PENDING`: `KycAiTimeoutJob` chỉ quét `AI_PROCESSING`; "Đưa lại hàng chờ" chỉ nhận `VERIFIED / REJECTED / SUPPLEMENT_REQUESTED`; người bán nộp lại bị `409 GNE-VND-4095`. Xác suất thấp nhưng xảy ra thì phải sửa tay trong DB | Cho `KycAiTimeoutJob` quét thêm hồ sơ **`PENDING` quá ~1 phút** (theo `kyc_submitted_at`) → gửi lại AI (hoặc chuyển `MANUAL_REVIEW`) — bản thu gọn của mẫu *outbox*, không cần thêm hạ tầng. Kèm test IT giả lập "đã commit nhưng chưa dispatch" |

### Ghi chú thiết kế: vì sao gọi AI bằng HTTP + callback, không dùng RabbitMQ

Genie chủ đích **không** dùng hàng đợi tin nhắn ở TLCN (TECH_STACK / ADR-03: bất đồng bộ = sự kiện nội bộ Spring + `@Async` + `@Scheduled`;
Kafka / RabbitMQ nằm trong danh sách "ngoài phạm vi TLCN" — DATN chỉ được **thêm**).

```
Hiện tại:  Backend ──commit──► sự kiện trong RAM ──► POST /kyc/verify ──► AI Worker (202) … xong ──► PATCH /internal/kyc/{id}/result
           job 30 giây: AI không gọi lại → nhân viên duyệt tay
RabbitMQ:  Backend ──► tin "kyc.verify" (broker lưu đĩa) ──► Worker tự lấy, xử lý, ack ──► tin "kyc.result" ──► Backend
           worker chết giữa chừng → tin chưa ack được giao lại
```

| Tiêu chí | HTTP + callback (đang dùng) | RabbitMQ |
|---|---|---|
| Tốc độ 1 hồ sơ | Ngang nhau — chênh vài ms, AI mất 0,5–3 giây | Ngang nhau |
| Nhiều hồ sơ dồn cùng lúc | Worker xếp hàng trong RAM (1 hồ sơ / lần); quá 30 giây → duyệt tay | Tốt hơn: tin nằm trong broker, thêm worker để tăng tốc |
| Sập giữa chừng | Việc dở trong RAM mất → hồ sơ về duyệt tay (không mất hồ sơ, mất kết quả AI); khoảng commit → dispatch có thể kẹt `PENDING` (mục **E**) | Tốt hơn: giao lại tin chưa ack; dùng kèm *outbox* thì không kẹt |
| Thử lại | Tự viết (worker thử 1 / 2 / 4 giây; backend lỗi → duyệt tay) | Có sẵn: requeue, dead-letter queue |
| Độ phức tạp, tài nguyên | Thấp, không thêm dịch vụ, debug bằng log / Swagger | Thêm broker phải chạy, giám sát, cấu hình; thêm ~150–300 MB RAM máy dev |

**Kết luận:** quy mô TLCN (ít hồ sơ, 1 worker) dùng HTTP + callback **tiện hơn, không chậm hơn**; nguyên tắc bù rủi ro là "AI lỗi → người duyệt".
Cần sửa mục **E** để không kẹt `PENDING`. Lên DATN, lượng hồ sơ lớn / cần không mất việc → cân nhắc thêm RabbitMQ + outbox.

## 4. Gợi ý thứ tự làm tiếp (trong phạm vi TLCN)

1. **E** — job quét hồ sơ kẹt `PENDING` (lỗi tiềm ẩn, sửa nhỏ, có test).
2. **G** — cảnh báo ảnh nhòe ngay khi tải lên.
3. **A** — chạy thử AI thật một vòng (giai đoạn K), ghi lại kết quả.
4. Bấm đủ kịch bản Mở shop & KYC trên giao diện → tick cột **Chạy** cho UC-VND-01/02/04/07/21.
5. Các mục 2–8 làm khi module notification / payment / shipping có người hiện thực; mục 1, 9, 10 để DATN.
