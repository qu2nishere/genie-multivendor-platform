# Progress — Sửa KYC với ảnh chụp thật

> Plan: [plan_sua_kyc_anh_that.md](plan_sua_kyc_anh_that.md)

## Task

- [x] **F00.01** `.gitignore` + plan / progress
- [x] **F00.02** COMMIT chore
- [x] **F01.01** zxing-cpp cho QR
- [x] **F01.02** Test QR
- [x] **F01.03** COMMIT QR
- [x] **F02.01** Sửa MRZ
- [x] **F02.02** Test MRZ
- [x] **F02.03** COMMIT MRZ
- [x] **F03.01** Dò mặt thu nhỏ + dựng thẳng thẻ
- [x] **F03.02** Độ nét theo ô lưới
- [x] **F03.03** Test khuôn mặt / độ nét
- [x] **F03.04** Chạy trên ảnh thật
- [x] **F03.05** COMMIT khuôn mặt + độ nét
- [x] **F04.01** Admin: kiểm tra `null` = "Chưa kiểm"
- [x] **F04.02** Nút "Yêu cầu nhân viên xem xét"
- [x] **F04.03** Email shop chỉ đọc
- [x] **F04.04** i18n + test + verify
- [x] **F04.05** COMMIT web
- [x] **F05.01** Backend dùng email tài khoản
- [x] **F05.02** COMMIT backend
- [x] **F06.01** Chạy lại thật qua web
- [x] **F06.02** COMMIT docs

## Nhật ký

| Ngày | Việc | Commit |
|---|---|---|
| 2026-11-22 | F00 `.gitignore` `test-kyc/` + plan | `695b390` |
| 2026-11-22 | F01 QR đọc bằng zxing-cpp (OpenCV dự phòng) | `db4d701` |
| 2026-11-22 | F02 MRZ sửa nhầm chữ ↔ số (`1DVNM` → `IDVNM`), thêm lượt OCR 50% | `3db3660` |
| 2026-11-22 | F03 dò mặt trên bản ≤ 960 px, dựng thẳng thẻ 4 hướng, độ nét theo ô lưới (p75, chuẩn 1000 px) | `c2a49e5` |
| 2026-11-22 | F03.04 pipeline trên ảnh thật (`test-kyc/`, ngoài git): độ nét 213 / 253 (trước 29 / 38), QR ✓, MRZ ✓, QR↔MRZ ✓, số CCCD ✓, hạn thẻ ✓, khớp mặt ✓ 0,569, liveness 1,0, **0 tín hiệu → 0 điểm → PASS** (trước: 120 điểm → REJECTED). `pytest` 89/89, `ruff` sạch | — |
| 2026-11-22 | F04 web: `/seller/kyc` tự làm mới 3 giây khi AI đang chấm (nút "Yêu cầu nhân viên xem xét" đã có nhưng trang không làm mới nên không hiện); admin kiểm tra `null` = "Chưa kiểm tra"; email shop chỉ đọc. vitest 13/13, tsc, eslint, check-i18n | `4d61b92` |
| 2026-11-22 | F05 backend: email shop = email tài khoản; API_SPEC 9.1. `ShopRegistrationIT` 5/5, `KycSubmissionIT` 5/5, `KycGateIT` 2/2 | `e7372d4` |
| 2026-11-25 | F06.01 chạy thật qua web (Playwright, AI Worker Docker `http`, ảnh trong `test-kyc/`): CCCD + selfie thật, khai đúng → QR ✓, MRZ ✓, QR↔MRZ ✓, khớp mặt 0,569, 0 tín hiệu → tự duyệt `OCR_AUTO`; trang `/seller/kyc` tự làm mới sang "đã xác minh". Chi tiết ở plan Mở shop & KYC K11.08 | commit tài liệu B4 |
