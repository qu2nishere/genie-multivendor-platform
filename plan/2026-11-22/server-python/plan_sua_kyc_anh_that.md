# Plan — Sửa KYC với ảnh chụp thật (AI Worker + web Mở shop / KYC)

> Người làm: Nguyễn Ngọc Thiện · Ngày: 2026-11-22 · UC: UC-VND-01, UC-VND-02, UC-VND-03, UC-VND-04
> Tiến độ: [progress_sua_kyc_anh_that.md](progress_sua_kyc_anh_that.md) · Nhánh: `bugfix/kyc-real-photo-pipeline`

## 1. Hiện trạng (chạy AI thật với CCCD + selfie thật, chụp điện thoại 1442×2560, thẻ xoay 90°)

Hồ sơ bị **từ chối tự động** dù ảnh rõ: QR ✗, MRZ ✗, khớp mặt ✗ (0%), lỗi `SELFIE_FACE_NOT_FOUND`, tín hiệu "ảnh mờ".
Chạy từng bước pipeline trên chính bộ ảnh đó (script debug, không ghi dữ liệu cá nhân) cho thấy:

| # | Bước | Kết quả | Nguyên nhân gốc |
|---|---|---|---|
| L1 | Độ nét (`assess_quality`) | mặt trước 29, mặt sau 37 < ngưỡng 60 → "mờ" → `UNREADABLE_IMAGE` | Phương sai Laplacian đo trên **cả khung 2000 px** (nền bàn, bao nhựa, vùng trống kéo điểm xuống) — không phản ánh độ nét vùng chữ |
| L2 | QR (`cv2.QRCodeDetector`) | không đọc được ở cả 4 hướng | Bộ đọc OpenCV yếu với QR nhỏ / qua bao nhựa. **zxing-cpp** đọc được ở mọi hướng, `parse` hợp lệ |
| L3 | MRZ (Tesseract) | OCR ra đủ 3 dòng × 30 ký tự nhưng dòng 1 thành `1DVNM…` | `_rows` bắt buộc bắt đầu bằng `ID`; không sửa nhầm chữ ↔ số ở phần chữ (vị trí 0–4 dòng 1, dòng 3) |
| L4 | Dò mặt selfie (YuNet) | ảnh 2000 px: 0 mặt; thu về ≤ 960 px: 1 mặt (0,92) | YuNet huấn luyện ở ~320 px — mặt quá lớn so với khung lưới anchor |
| L5 | Dò mặt trên thẻ | đứng thẳng: 0 mặt; xoay 90°: 1 mặt (0,93) | Pipeline không dựng thẳng ảnh thẻ trước khi dò mặt |
| L6 | Web admin: "Quốc huy", "Vân tay" ✗ | Kiểm tra **chưa làm** (`null`) bị hiển thị thành "không đạt" | Mapper `=== true` biến `null` thành `false` |
| L7 | Web người bán: không có nút "Yêu cầu nhân viên xem xét" | `useRequestManualReview` có sẵn nhưng không màn nào dùng | Thiếu UI |
| L8 | Form mở shop: email shop sửa tự do | Người dùng muốn **email shop = email tài khoản**, ô xám, không sửa | Form chỉ điền sẵn; backend nhận email bất kỳ |

## 2. Quyết định

| Mã | Câu hỏi | Chốt |
|---|---|---|
| F-D1 | Thư viện QR | Thêm `zxing-cpp` (Apache-2.0, wheel sẵn cho Python 3.12) làm bộ đọc chính; OpenCV giữ làm dự phòng. Không đổi hợp đồng API |
| F-D2 | Độ nét | Đo ở độ phân giải chuẩn hóa (cạnh dài 1000 px) theo **ô lưới**, lấy phân vị cao (vùng chữ nét nhất) — ảnh mờ toàn phần vẫn thấp. Ngưỡng hiệu chỉnh lại bằng ảnh mẫu + ảnh thật |
| F-D3 | Hướng thẻ | Dựng thẳng ảnh mặt trước bằng cách thử 4 hướng, chọn hướng dò được mặt tin cậy nhất (dùng cho dò mặt thẻ). QR / MRZ đã tự thử nhiều hướng |
| F-D4 | Dò mặt | Dò + căn mặt trên bản thu nhỏ (cạnh dài ≤ 960 px) cho cả selfie và thẻ |
| F-D5 | MRZ | Sửa nhầm chữ ↔ số theo vị trí cho phần **chữ** (dòng 1 vị trí 0–4, dòng 2 vị trí 7 và 15–17, dòng 3); nhận dòng đầu dạng `1D…`; thêm lượt OCR ở bản thu nhỏ 50% |
| F-D6 | Email shop | **Email shop = email tài khoản**: web hiện ô xám chỉ đọc; backend bỏ qua email client gửi, luôn dùng email tài khoản (một nguồn sự thật) — API_SPEC 9.1 ghi chú |
| F-D7 | Không lưu ảnh thật | Thư mục `test-kyc/` vào `.gitignore`; test tự động chỉ dùng thẻ giả `tests/fixtures/build_card.py` |

## 3. Task

### F00 — Chuẩn bị
- **[F00.01]** `.gitignore` thêm `test-kyc/`; plan + progress này.
- **[F00.02]** COMMIT `chore: ignore local kyc test photos and add fix plan`

### F01 — AI Worker: QR
- **[F01.01]** `pyproject.toml` thêm `zxing-cpp`; `qr.decode` thử zxing-cpp trước (ảnh gốc + xám), rồi các biến thể OpenCV cũ.
- **[F01.02]** Test: QR thẻ giả xoay 90 / 180 / 270 + thu nhỏ đọc được; ảnh không có QR vẫn `LEGACY_CARD`.
- **[F01.03]** COMMIT `fix(ai-worker): read cccd qr codes with zxing-cpp`

### F02 — AI Worker: MRZ
- **[F02.01]** `_fix_line` sửa vị trí chữ; `_rows` nhận `1D`/`lD`; `read` thêm bản thu nhỏ 50%.
- **[F02.02]** Test: dòng OCR `1DVNM…` / `NGUYEN…` có số lẫn chữ vẫn parse hợp lệ.
- **[F02.03]** COMMIT `fix(ai-worker): tolerate letter and digit mix-ups in mrz lines`

### F03 — AI Worker: khuôn mặt + độ nét
- **[F03.01]** `face.detect` dò trên bản thu nhỏ (≤ 960 px) và trả kèm ảnh đã thu; `face.run` dựng thẳng ảnh thẻ (4 hướng, chọn mặt tin cậy nhất).
- **[F03.02]** `images.sharpness` đo theo ô lưới ở cạnh dài 1000 px, phân vị cao; hiệu chỉnh `kyc_blur_threshold`.
- **[F03.03]** Test: selfie mặt lớn, thẻ xoay 90° vẫn dò được; ảnh nét / mờ / nhỏ phân loại đúng.
- **[F03.04]** Chạy pipeline trên ảnh thật (`test-kyc/`, ngoài git) → QR ✓, MRZ ✓, khớp mặt ✓, không `UNREADABLE_IMAGE`.
- **[F03.05]** COMMIT `fix(ai-worker): detect faces on large and rotated photos and measure sharpness per tile`

### F04 — Web
- **[F04.01]** Admin KYC: kiểm tra `null` hiển thị "Chưa kiểm" (xám), không phải ✗.
- **[F04.02]** `/seller/kyc`: hồ sơ `REJECTED` chưa xin → nút **Yêu cầu nhân viên xem xét** (xác nhận → gọi API → thông báo); đã xin → dòng chữ "Đã gửi yêu cầu".
- **[F04.03]** `/become-seller`: ô email = email tài khoản, chỉ đọc (xám) + dòng gợi ý.
- **[F04.04]** i18n vi / en, test rules, `npm run verify` phần liên quan.
- **[F04.05]** COMMIT `fix(seller-ui): …` / `fix(admin-ui): …` (tách theo màn)

### F05 — Backend email shop
- **[F05.01]** `ShopRegistrationService` dùng email tài khoản, bỏ qua email client; `ShopRegistrationIT` cập nhật; API_SPEC 9.1 ghi chú.
- **[F05.02]** COMMIT `fix(vendor): use the account email as the shop email on registration`

### F06 — Chạy lại thật
- **[F06.01]** Build lại image worker, nộp KYC bằng ảnh thật qua web → kết quả mong đợi: VERIFIED hoặc MANUAL_REVIEW có lý do đúng; ghi kết quả vào progress.
- **[F06.02]** COMMIT `docs: record kyc real photo fix results`
