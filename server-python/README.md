# Genie AI Worker (`server-python`)

FastAPI chạy nội bộ (chỉ backend gọi tới, header `X-Internal-Token`). Hiện có **pipeline thẩm định KYC** (UC-VND-03 — QR,
MRZ, khớp mặt); phân tích cảm xúc đánh giá (`app/sentiment`) làm ở plan khác. Hợp đồng với backend: ADR 0011 mục 5 + tệp mẫu
`docs/api-tests/kyc-contract/`.

## Chạy

```bash
# Python 3.12 (dự án khóa >=3.12,<3.13) + Tesseract (đọc MRZ)
#   Debian/Ubuntu: sudo apt-get install tesseract-ocr tesseract-ocr-eng
#   macOS:         brew install tesseract
#   Windows:       bộ cài UB Mannheim (github.com/UB-Mannheim/tesseract) — thêm thư mục cài vào PATH
pip install -e ".[dev]"
SKIP_PHOBERT=1 ../scripts/fetch-models.sh     # YuNet + SFace từ opencv_zoo, kiểm SHA-256 (scripts/models.sha256)
cp .env.example .env                          # INTERNAL_TOKEN TRÙNG backend; BACKEND_URL; STORAGE_ALLOWED_HOSTS
uvicorn app.main:app --reload --port 8000
```

Không cài được Python 3.12 / Tesseract trên máy: dựng image và chạy test trong container, ví dụ
`docker build -t genie-ai-worker:local . && docker run --rm -p 8000:8000 --env-file .env -v "$PWD/models:/app/models" genie-ai-worker:local`.

Backend gọi worker thật: đặt `AI_WORKER_MODE=http` trong `.env` gốc (mặc định máy dev là `stub` — giả lập trong backend).

Kiểm tra trước commit: `ruff check . && ruff format --check . && pytest` (test `@pytest.mark.models` tự bỏ qua khi thiếu
`models/*.onnx`; CI chạy `pytest -m "not models"`).

## Pipeline (`app/kyc/service.py`)

```
POST /kyc/verify ──202──► nền: tải 3 ảnh song song (core/images: host cho phép, ≤ 12 MB, jpeg/png, trong bộ nhớ)
  → chất lượng (cạnh ngắn ≥ 600 px, độ nét Laplacian)       → UNREADABLE_IMAGE
  → QR mặt trước (cv2.QRCodeDetector, nhiều mức tiền xử lý)   → qr_code · LEGACY_CARD
  → MRZ mặt sau (tesseract stdin, TD1, số kiểm tra 7-3-1)     → mrz
  → luật: QR ↔ MRZ · cấu trúc số CCCD · hạn thẻ 25/40/60      → QR_MRZ_MISMATCH · ID_STRUCTURE_MISMATCH · EXPIRY_RULE_MISMATCH
  → khuôn mặt (YuNet + SFace, chỉ khi biometric_consent)      → face_match · FACE_SCORE_BORDERLINE / LOW · LIVENESS_SUSPECT
  → scoring.assemble (KHÔNG tính điểm) → PATCH {BACKEND_URL}/api/v1/internal/kyc/{id}/result (thử lại 1/2/4 s)
```

- Mỗi bước bọc `try` riêng; quá `KYC_PIPELINE_TIMEOUT_S` (20 s) → gửi phần đã có + `PIPELINE_TIMEOUT`.
- Tối đa `KYC_MAX_CONCURRENCY` (1) hồ sơ cùng lúc; phần CPU chạy trong thread pool. Đo trên thẻ mẫu: ≈ 0.4–0.8 s / hồ sơ,
  RAM ≈ 250 MiB sau khi nạp mô hình.
- **Dữ liệu cá nhân:** ảnh không ghi xuống đĩa (OCR qua `stdin`), không log số CCCD / họ tên / URL ký sẵn / vector mặt
  (`httpx` bị hạ log về WARNING vì tự ghi URL). Test `test_service.py::test_logs_contain_no_personal_data` canh việc này.

## Tín hiệu worker sinh (`app/kyc/signals.py`, ENTITY 1.6)

| Mã | Khi nào | Điểm (backend, mặc định) |
|---|---|---:|
| `UNREADABLE_IMAGE` | ảnh tải / giải mã hỏng, quá nhỏ, quá mờ, không đọc được MRZ | 30 |
| `LEGACY_CARD` | ảnh đạt chất lượng nhưng không có QR | 30 |
| `QR_MRZ_MISMATCH` | số / ngày sinh / giới tính / họ tên QR khác MRZ | 60 |
| `ID_STRUCTURE_MISMATCH` | mã tỉnh, số giới tính – thế kỷ, 2 số năm sinh sai | 40 |
| `EXPIRY_RULE_MISMATCH` | hạn thẻ không đúng mốc 25 / 40 / 60 tuổi | 30 |
| `FACE_SCORE_BORDERLINE` | 0.25 ≤ cosine < 0.363 | 25 |
| `FACE_SCORE_LOW` | cosine < 0.25 | 60 |
| `LIVENESS_SUSPECT` | selfie không có / nhiều mặt, mờ, mặt quá nhỏ, vân moiré | 40 |

`DUPLICATE_ID`, `DECLARED_DATA_MISMATCH` do backend sinh. Worker không quyết định — backend cộng điểm (< 30 tự duyệt,
30–69 duyệt tay, ≥ 70 từ chối, lấy mẫu 5%).

## Ảnh mẫu cho test

`tests/fixtures/build_card.py` dựng thẻ CCCD **giả** (QR sinh từ chuỗi giả, 3 dòng MRZ có số kiểm tra đúng, ô ảnh xám) và
các biến thể xoay / thu nhỏ / tối / mờ. Không đưa giấy tờ thật hay ảnh mặt người thật chưa được đồng ý vào repo.

## Giới hạn đã biết

- `face_duplicate` luôn `null` (cần kho vector mặt — Y-D4); `national_emblem`, `fingerprint` luôn `null` (Y-D5).
- Liveness chỉ thụ động trên 1 ảnh selfie (Y-D7) — chớp mắt / quay đầu làm ở trình duyệt.
- Tỷ lệ đọc QR / MRZ 100% đo trên **thẻ mẫu tự dựng** (MRZ vẽ bằng DejaVu Sans Mono, không phải OCR-B); ảnh chụp điện thoại
  thật cần đo lại. Ngưỡng mặt SFace chưa hiệu chỉnh vì chưa có bộ ảnh mặt được phép dùng — chỉnh qua `KYC_FACE_*`.
