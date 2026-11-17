# PLAN — AI Worker: pipeline thẩm định KYC (QR, MRZ, Face Match)

| | |
|---|---|
| **Ngày lập** | 2026-11-16 |
| **Thể loại** | server-python (`server-python/app/kyc`, `app/core`) + `scripts/fetch-models.sh` + `deploy/` (biến môi trường, Dockerfile) |
| **Use case** | Thẩm định KYC tự động qua AI — QR, MRZ & Face Match (UC-VND-03, **phần AI Worker**). Phần backend (gửi hồ sơ, quy đổi tín hiệu → điểm, kết luận, hết giờ) ở [plan_mo_shop_kyc.md](../server-spring/plan_mo_shop_kyc.md) giai đoạn K05 |
| **Tài liệu chuẩn** | UC-VND-03 (luồng + ràng buộc) · ENTITY 1.6 (`KycResult`, bảng tín hiệu rủi ro) · API 12 · LLD 7.4 (SD-04), 9.6 · implement Phần 8 · LUU_TRU 5.6–5.7 · TS 2.5, 4.6, 9, 10.2, 14, 16.2 — xem [README](../README.md) |
| **Phụ thuộc** | Hợp đồng Backend ↔ AI Worker (quyết định D6 của plan Mở shop & KYC = Y-D1 ở đây) phải chốt trước. Không phụ thuộc mã Java: test bằng tệp JSON mẫu + máy chủ callback giả |
| **Theo dõi tiến độ** | [progress_kyc_ai_worker.md](./progress_kyc_ai_worker.md) — cùng mã task `Yxx.yy` |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |

---

## 0. Hiện trạng đã khảo sát

| Phần | Có gì | Thiếu |
|---|---|---|
| `app/main.py` | FastAPI + `lifespan` (TODO nạp mô hình), gắn router kèm `require_internal_token` | Nạp YuNet / SFace, đặt `models_loaded` |
| `app/core` | `config.py` (`internal_token`, `backend_url`, `minio_*`, `models_dir`), `security.py` (so khớp token hằng thời gian), `logging.py`, `health.py` | `callback.py` rỗng; chưa có cấu hình ngưỡng khuôn mặt, giới hạn tải ảnh |
| `app/kyc` | 7 tệp `router, schemas, service, qr, mrz, face, scoring` — **đều chỉ có docstring + `# TODO`** | Toàn bộ pipeline |
| `tests/` | `test_health.py`, `conftest.py` (TestClient), thư mục `tests/kyc/` rỗng | Test KYC, ảnh mẫu |
| Phụ thuộc | `opencv-python-headless`, `onnxruntime`, `numpy`, `httpx`, `minio`, `pydantic-settings` | Thư viện đọc ký tự MRZ (Y-D2); `qrcode` cho test |
| `scripts/fetch-models.sh` | Tải `face_detection_yunet_2023mar.onnx`, `face_recognition_sface_2021dec.onnx` (+ PhoBERT) | Kiểm SHA-256 (TODO) |
| `Dockerfile` | `python:3.12-slim`, 1 worker uvicorn, người dùng không phải root | Gói hệ thống cho OCR (nếu chọn Tesseract) |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | AI Worker **không kết luận**: chỉ trả kết quả từng phép kiểm + danh sách mã tín hiệu + dữ liệu trích xuất. Điểm, ngưỡng 30 / 70, lấy mẫu 5%, kiểm trùng CCCD nằm ở backend | ENTITY 1.6; plan Mở shop & KYC D6 |
| N2 | **Cộng tín hiệu thay vì chặn cứng:** một phép kiểm không chạy được (ảnh mờ, không thấy QR) → trả `null` + tín hiệu tương ứng, pipeline vẫn chạy tiếp các phép còn lại | UC-VND-03 ràng buộc |
| N3 | Chỉ nhận request có token nội bộ; chỉ gọi lại đúng `BACKEND_URL` đã cấu hình (bỏ qua host trong `callback_url` nếu khác); chỉ tải ảnh từ host lưu trữ được cấu hình (chống SSRF) | TS 9; API 12 |
| N4 | **Không lưu ảnh giấy tờ / khuôn mặt xuống đĩa**, không ghi dữ liệu cá nhân vào log (số CCCD, họ tên, URL ký sẵn, vector khuôn mặt); log chỉ có `kyc_id`, thời gian từng bước, mã tín hiệu | NFR-07; LUU_TRU 5.6 |
| N5 | **Không đồng ý sinh trắc → không chạy bước khuôn mặt** (`face_match = null`, không tạo vector) | UC-VND-02 bước 2; Nghị định 13/2023 |
| N6 | Mô hình nạp **một lần** trong `lifespan`; suy luận CPU chạy trong thread pool, tối đa 1 hồ sơ cùng lúc (ngân sách RAM VPS); `*.onnx` không commit | TS 2.2, 16.2; implement 8 |
| N7 | Trả `202` ngay, xử lý nền, gọi lại backend; cả pipeline ≤ 10 giây (backend chờ tối đa 30 giây); callback thử lại 3 lần giãn cách | LLD 9.6; TS 10.2 |
| N8 | Ngưỡng (điểm khớp mặt, độ nét, kích thước ảnh) là **cấu hình** qua biến môi trường, không ghi cứng | UC-VND-03 ràng buộc |
| N9 | Cấu trúc `app/<miền>/{router,schemas,service,…}.py`; `ruff check` + `ruff format --check` + `pytest` xanh; kiểu dữ liệu đầy đủ | TS 4.6, 5 |
| N10 | Ảnh test **không phải giấy tờ thật**: thẻ mẫu tự dựng (QR + MRZ sinh bằng mã), ảnh mặt có giấy phép dùng lại hoặc của thành viên nhóm đã đồng ý | NFR-07 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Đề xuất | Chốt |
|---|---|---|---|
| Y-D1 | Hợp đồng với backend (trùng D6 của plan Mở shop & KYC) | snake_case. **Yêu cầu** `POST /kyc/verify`: `{kyc_id, documents{id_front, id_back, selfie}` (URL ký sẵn 5 phút)`, declared{identity_number, full_name}, biometric_consent, callback_url}` → `202 {kyc_id, accepted: true}`. **Callback** `PATCH {BACKEND_URL}/api/v1/internal/kyc/{kyc_id}/result`: `{checks{qr_code, mrz, mrz_qr_match, cccd_number_valid, expiry_rule_valid, face_match, face_duplicate, national_emblem, fingerprint}` (mỗi giá trị `true / false / null`)`, face_match_score, liveness_score, signals[{code}], extracted{identity_number, full_name, date_of_birth, gender, address, issue_date, expiry_date, legacy_identity_number}, errors[], model{yunet, sface, pipeline_version}}`. Tệp JSON mẫu dùng chung đặt ở `docs/api-tests/kyc-contract/` | ☐ |
| Y-D2 | Đọc ký tự MRZ — repo chưa có thư viện OCR | **Tesseract** (`tesseract-ocr` + dữ liệu `mrz.traineddata`, gọi qua `pytesseract`, chỉ nhận `A–Z 0–9 <`) — nhẹ, đủ cho phông OCR-B; cập nhật TS 2.5 + Dockerfile. Phương án dự phòng: RapidOCR (ONNX) nếu Tesseract đọc kém trên ảnh chụp điện thoại — đo ở Y04.06 rồi quyết | ☐ |
| Y-D3 | Giải mã QR | `cv2.QRCodeDetector` (có sẵn) + tiền xử lý nhiều mức (phóng to, CLAHE, ngưỡng thích nghi, xoay 0 / 90 / 180 / 270); đo tỷ lệ đọc ở Y03.07, dưới 90% trên bộ ảnh mẫu thì thêm `zxing-cpp` | ☐ |
| Y-D4 | Tín hiệu khuôn mặt trùng shop khác (`FACE_DUPLICATE`) cần kho vector khuôn mặt — chưa có cột / nơi lưu | **Đợt này chưa làm:** luôn trả `face_duplicate = null`. Làm sau cần thêm cột vector đã mã hóa ở `kyc_results` (ENTITY trước) + backend gửi danh sách ứng viên — ghi thành việc tồn ở Ghi chú | ☐ |
| Y-D5 | Quốc huy (`national_emblem`) và vân tay (`fingerprint`) — 2 cờ của ENTITY 1.6 | So mẫu đơn giản (template matching vùng cố định sau khi nắn thẻ) → `true / false / null`; **không** sinh tín hiệu rủi ro (chỉ để nhân viên tham khảo). Nếu không kịp: trả `null` | ☐ |
| Y-D6 | Ngưỡng khớp mặt SFace (cosine) | `KYC_FACE_MATCH_THRESHOLD = 0.363` (ngưỡng công bố của SFace) → khớp; `KYC_FACE_LOW_THRESHOLD = 0.25` → dưới mức này `FACE_SCORE_LOW`; ở giữa `FACE_SCORE_BORDERLINE`. Hiệu chỉnh lại sau khi đo (Y05.08) | ☐ |
| Y-D7 | "Liveness" phía máy chủ (chỉ có 1 ảnh selfie tĩnh) | Chỉ kiểm chất lượng thụ động: độ nét (phương sai Laplacian), kích thước mặt, số khuôn mặt = 1, dấu hiệu chụp lại màn hình (moiré đơn giản) → `liveness_score` 0–1; dưới ngưỡng → `LIVENESS_SUSPECT`. Liveness chủ động (chớp mắt, quay đầu) ở trình duyệt — ngoài phạm vi | ☐ |
| Y-D8 | Cách lấy ảnh | Tải qua URL ký sẵn trong yêu cầu (không dùng khóa Silo ở worker cho luồng KYC); giới hạn 12 MB / ảnh, timeout 5 giây, chỉ `image/jpeg`, `image/png` | ☐ |

---

## 3. Cây file sẽ tạo / sửa

```
server-python/
├── pyproject.toml                 (sửa) + pytesseract; dev: qrcode, pillow, respx
├── Dockerfile                     (sửa) + tesseract-ocr, mrz.traineddata (Y-D2)
├── app/
│   ├── main.py                    (sửa) lifespan nạp YuNet + SFace, đặt models_loaded
│   ├── core/
│   │   ├── config.py              (sửa) ngưỡng KYC, giới hạn tải ảnh, storage_allowed_hosts
│   │   ├── callback.py            (viết) gọi lại backend có thử lại
│   │   └── images.py              (mới) tải ảnh an toàn + giải mã + chuẩn hóa hướng / kích thước
│   └── kyc/
│       ├── schemas.py             (viết) VerifyRequest · VerifyAccepted · KycResultPayload · Signal · Extracted
│       ├── signals.py             (mới) hằng số mã tín hiệu (khớp ENTITY 1.6)
│       ├── qr.py                  (viết) giải mã + phân tích chuỗi QR CCCD
│       ├── mrz.py                 (viết) tìm vùng MRZ, OCR, phân tích TD1, số kiểm tra
│       ├── rules.py               (mới) cấu trúc số CCCD, quy tắc hạn thẻ, so QR ↔ MRZ
│       ├── face.py                (viết) YuNet phát hiện, SFace vector, cosine, chất lượng
│       ├── scoring.py             (viết) gom kết quả từng bước → checks + signals (KHÔNG tính điểm)
│       ├── service.py             (viết) điều phối pipeline, giới hạn đồng thời, đo thời gian
│       └── router.py              (viết) POST /kyc/verify
├── tests/
│   ├── fixtures/                  (mới) build_card.py (dựng thẻ mẫu) · faces/ (ảnh được phép dùng) · contract/*.json
│   └── kyc/                       test_qr.py · test_mrz.py · test_rules.py · test_face.py · test_scoring.py
│                                  test_service.py · test_router.py · test_callback.py · test_images.py
scripts/fetch-models.sh            (sửa) kiểm SHA-256
deploy/…/.env.example, docker-compose (sửa) biến KYC_* của ai-worker
docs/api-tests/kyc-contract/       (mới — dùng chung với backend) verify-request.json · result-pass.json · result-partial.json · result-fail.json
docs/Document/…                    implement 8.1 · API_SPEC 12 · TECH_STACK 2.5 · Usecase.md (UC-VND-03) · CHECKLIST_TIEN_DO_UC.md
```

---

## 4. Bảng ánh xạ bước pipeline → phép kiểm → tín hiệu

| Bước (UC-VND-03) | Tệp | Cờ trong `checks` | Tín hiệu có thể sinh (ENTITY 1.6) |
|---|---|---|---|
| Tầng 0 — chất lượng ảnh đầu vào | `core/images.py` | — | `UNREADABLE_IMAGE` |
| Tầng 1 — đọc QR mặt trước | `kyc/qr.py` | `qr_code` | `LEGACY_CARD` (không có QR / thẻ đời cũ), `UNREADABLE_IMAGE` |
| Tầng 1 — đọc MRZ mặt sau | `kyc/mrz.py` | `mrz` | `UNREADABLE_IMAGE` |
| Tầng 1 — đối chiếu QR ↔ MRZ | `kyc/rules.py` | `mrz_qr_match` | `QR_MRZ_MISMATCH` |
| Tầng 1 — cấu trúc số CCCD (mã tỉnh, giới tính / thế kỷ, năm sinh) | `kyc/rules.py` | `cccd_number_valid` | `ID_STRUCTURE_MISMATCH` |
| Tầng 1 — quy tắc hạn thẻ (mốc 25 / 40 / 60 tuổi) | `kyc/rules.py` | `expiry_rule_valid` | `EXPIRY_RULE_MISMATCH` |
| Tầng 1 — quốc huy, vân tay (Y-D5) | `kyc/face.py` / `rules.py` | `national_emblem`, `fingerprint` | — |
| Tầng 2 — khớp mặt thẻ ↔ selfie | `kyc/face.py` | `face_match` + `face_match_score` | `FACE_SCORE_BORDERLINE`, `FACE_SCORE_LOW` |
| Tầng 2 — chất lượng selfie | `kyc/face.py` | `liveness_score` | `LIVENESS_SUSPECT` |
| Tầng 2 — trùng khuôn mặt (Y-D4) | — | `face_duplicate = null` | (`FACE_DUPLICATE` — chưa làm) |

`DUPLICATE_ID` và `DECLARED_DATA_MISMATCH` do **backend** sinh (plan Mở shop & KYC K05.07) — worker không sinh.

---

## 5. Nhánh, commit, PR

- Nhánh: **`feature/ai-worker-kyc`** tách từ `develop` (có mã Jira: `feature/GNE-<số>-ai-worker-kyc`). PR vào `develop` theo mốc: (1) Y01–Y02 khung + hợp đồng, (2) Y03–Y04 QR + MRZ + luật, (3) Y05–Y07 khuôn mặt + pipeline, (4) Y08–Y09 đóng gói + tài liệu. Mỗi PR ≤ ~400 dòng (không tính ảnh / JSON mẫu).
- Scope commit: `ai-worker`, `deploy`, `docs`.
- Quy ước chung: [README mục 5](../README.md).

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-python && ruff check . && ruff format --check .`
2. `pytest` xanh (test cần mô hình đánh dấu `@pytest.mark.models`, tự bỏ qua khi thiếu `models/*.onnx`; chạy đủ trên máy đã `scripts/fetch-models.sh`).
3. Không có ảnh giấy tờ thật, `*.onnx`, `.env` trong `git status`.
4. Soát log một lần chạy: không có số CCCD, họ tên, URL ký sẵn.
5. Tick task ở progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Yxx.yy` = task; `Yxx.Cn` = task commit.

### Y00 — Chuẩn bị & chốt hợp đồng

- **[Y00.01]** Kiểm danh tính git — `git config user.name` / `user.email`.
- **[Y00.02]** Chốt Y-D1…Y-D8 — Y-D1 chốt **cùng người làm backend** (plan Mở shop & KYC K00.02, ADR 0010); ghi vào đầu file progress.
- **[Y00.03]** Tạo nhánh — `git switch develop && git pull && git switch -c feature/ai-worker-kyc`.
- **[Y00.04]** Dựng môi trường — Python 3.12, `pip install -e ".[dev]"`, `MODELS_BASE_URL=… scripts/fetch-models.sh`; `pytest` (test health) và `ruff check` xanh; `uvicorn app.main:app --reload` lên được.
- **[Y00.05]** Đọc tài liệu theo thứ tự — UC-VND-03 → ENTITY 1.6 → LLD 7.4 → implement Phần 8; ghi chỗ implement lệch hợp đồng mới (sửa ở Y09).
- **[Y00.06]** Viết tệp hợp đồng mẫu — `docs/api-tests/kyc-contract/{verify-request,result-pass,result-partial,result-fail}.json` (dữ liệu giả); báo người làm backend dùng chung.
- **[Y00.C1]** COMMIT — `docs(ai-worker): add shared kyc contract samples for backend and ai worker`

### Y01 — Khung: cấu hình, lược đồ, nạp mô hình

- **[Y01.01]** `core/config.py` — thêm `storage_allowed_hosts`, `kyc_download_max_bytes = 12 MB`, `kyc_download_timeout_s = 5`, `kyc_face_match_threshold = 0.363`, `kyc_face_low_threshold = 0.25`, `kyc_liveness_threshold`, `kyc_min_image_side = 600`, `kyc_blur_threshold`, `kyc_max_concurrency = 1`, `callback_retries = 3`, `pipeline_version`; `.env.example` của worker.
- **[Y01.02]** `kyc/signals.py` — hằng số 9 mã tín hiệu worker được phép sinh, đúng chính tả ENTITY 1.6; test so với danh sách trong `docs/api-tests/kyc-contract`.
- **[Y01.03]** `kyc/schemas.py` — Pydantic v2: `VerifyRequest` (URL phải `http/https`, `identity_number` 12 chữ số, `kyc_id > 0`), `VerifyAccepted`, `Checks` (9 trường `bool | None`), `Signal`, `Extracted`, `ModelInfo`, `KycResultPayload`; cấm trường lạ (`extra = "forbid"`).
- **[Y01.04]** `test_schemas` — nạp 4 tệp JSON hợp đồng qua lược đồ không lỗi; thiếu trường bắt buộc → lỗi; xuất JSON giữ snake_case và `null`.
- **[Y01.05]** `main.py` `lifespan` — nạp `cv2.FaceDetectorYN` (YuNet) và `cv2.FaceRecognizerSF` (SFace) từ `models_dir` vào `app.state`; thiếu tệp → log cảnh báo, `models_loaded = False`, worker vẫn lên (bước khuôn mặt sẽ trả `null` + `errors`).
- **[Y01.06]** `health` — `/health` trả thêm `models_loaded`, `pipeline_version`; test.
- **[Y01.C1]** COMMIT — `feat(ai-worker): add kyc contract schemas, settings and model loading`

### Y02 — Tải ảnh an toàn & gọi lại backend

- **[Y02.01]** `core/images.py` `fetch_image(url)` — kiểm host thuộc `storage_allowed_hosts` (N3), tải dạng luồng có giới hạn byte + timeout, kiểm `Content-Type`, giải mã bằng OpenCV **trong bộ nhớ** (N4), sửa hướng theo EXIF, thu nhỏ cạnh dài về ≤ 2000 px.
- **[Y02.02]** `assess_quality(image)` — cạnh ngắn < `kyc_min_image_side` hoặc độ nét (phương sai Laplacian) dưới ngưỡng → đánh dấu không đạt (nguồn của `UNREADABLE_IMAGE`).
- **[Y02.03]** `test_images.py` (dùng `respx`) — host lạ bị từ chối; tệp quá lớn bị cắt và báo lỗi; không phải ảnh → lỗi; ảnh xoay EXIF được dựng thẳng; ảnh mờ bị đánh dấu.
- **[Y02.04]** `core/callback.py` `send_result(kyc_id, payload)` — `PATCH {backend_url}/api/v1/internal/kyc/{kyc_id}/result`, header `X-Internal-Token`, timeout 5 giây, thử lại 3 lần (1 s / 2 s / 4 s) với lỗi mạng và 5xx, **không** thử lại với 4xx; log kết quả không kèm nội dung.
- **[Y02.05]** `test_callback.py` — thành công lần đầu; 503 hai lần rồi 200; 401 không thử lại; hết lượt → ghi log lỗi, không ném ra ngoài luồng nền.
- **[Y02.C1]** COMMIT — `feat(ai-worker): download kyc images safely and call back the backend with retries`

### Y03 — Tầng 1: mã QR mặt trước

- **[Y03.01]** `tests/fixtures/build_card.py` — dựng ảnh thẻ mẫu (nền, khung, QR sinh từ chuỗi giả, 3 dòng MRZ phông OCR-B, ô dán ảnh mặt); tham số: xoay, làm mờ, giảm sáng, thu nhỏ (N10).
- **[Y03.02]** `qr.decode(image)` — `cv2.QRCodeDetector` + chuỗi tiền xử lý Y-D3 (dừng ngay khi đọc được); trả chuỗi thô hoặc `None`.
- **[Y03.03]** `qr.parse(raw)` — tách theo `|`: số CCCD (12 số), số CMND cũ (có thể rỗng), họ tên, ngày sinh `ddMMyyyy`, giới tính, địa chỉ, ngày cấp `ddMMyyyy`; chuỗi không đúng 7 phần / sai định dạng → `None` + ghi `errors`.
- **[Y03.04]** Phân loại kết quả — đọc được và hợp lệ → `qr_code = true`; không tìm thấy QR trên ảnh đạt chất lượng → `LEGACY_CARD`, `qr_code = false`; ảnh không đạt chất lượng → `UNREADABLE_IMAGE`, `qr_code = null`.
- **[Y03.05]** `test_qr.py` — thẻ mẫu thẳng / xoay 90° / 180° / thu nhỏ đọc được; chuỗi thiếu trường bị từ chối; ngày không tồn tại (31/02) bị từ chối; thẻ không có QR → `LEGACY_CARD`; ảnh mờ → `UNREADABLE_IMAGE`.
- **[Y03.06]** Chuẩn hóa họ tên — bỏ dấu, viết hoa, gộp khoảng trắng (cùng quy tắc `NameNormalizer` của backend; bảng ví dụ dùng chung trong test).
- **[Y03.07]** Đo tỷ lệ đọc QR — chạy trên bộ ảnh mẫu biến thể (≥ 30 ảnh sinh từ `build_card.py`); ghi tỷ lệ vào Ghi chú; < 90% thì áp phương án dự phòng Y-D3.
- **[Y03.C1]** COMMIT — `feat(ai-worker): decode and parse the citizen id qr code`

### Y04 — Tầng 1: MRZ mặt sau & các luật đối chiếu

- **[Y04.01]** Phụ thuộc OCR (Y-D2) — thêm `pytesseract` vào `pyproject.toml`; Dockerfile cài `tesseract-ocr` + chép `mrz.traineddata`; ghi cách cài trên Windows / macOS vào README của worker.
- **[Y04.02]** `mrz.locate(image)` — tìm dải MRZ ở phần dưới mặt sau (blackhat + gradient + đóng hình thái → vùng chữ dài nhất), nắn nghiêng, cắt.
- **[Y04.03]** `mrz.read(region)` — OCR chỉ nhận `A–Z`, `0–9`, `<`; ghép đúng 3 dòng × 30 ký tự (TD1); sửa nhầm thường gặp theo vị trí (`O↔0`, `I↔1`, `B↔8` ở ô số).
- **[Y04.04]** `mrz.parse(lines)` — tách số giấy tờ, ngày sinh, giới tính, ngày hết hạn, quốc tịch, họ tên; tính **số kiểm tra** (trọng số 7-3-1) cho từng trường + số kiểm tra tổng; sai số kiểm tra → `mrz = false`.
- **[Y04.05]** `test_mrz.py` — thẻ mẫu đọc đúng; sửa 1 ký tự → sai số kiểm tra; ảnh không có MRZ → `mrz = null` + `UNREADABLE_IMAGE`; bộ ví dụ số kiểm tra theo ICAO 9303.
- **[Y04.06]** Đo tỷ lệ đọc MRZ trên bộ ảnh biến thể; ghi vào Ghi chú; quyết định giữ Tesseract hay đổi (Y-D2).
- **[Y04.07]** `rules.check_id_structure(identity_number, date_of_birth, gender)` — 3 số đầu thuộc bảng mã tỉnh (`resources/province_codes.txt`), số thứ 4 khớp giới tính + thế kỷ sinh, số 5–6 khớp 2 số cuối năm sinh → `cccd_number_valid`; lệch → `ID_STRUCTURE_MISMATCH`.
- **[Y04.08]** `rules.check_expiry(date_of_birth, issue_date, expiry_date)` — hạn thẻ phải là sinh nhật mốc 25 / 40 / 60 kế tiếp sau ngày cấp (cấp trong 2 năm trước mốc thì tính mốc sau; sau 60 tuổi không thời hạn) → `expiry_rule_valid`; lệch → `EXPIRY_RULE_MISMATCH`; thiếu dữ liệu → `null`.
- **[Y04.09]** `rules.match_qr_mrz(qr, mrz)` — so số CCCD, ngày sinh, giới tính, họ tên đã chuẩn hóa → `mrz_qr_match`; lệch bất kỳ → `QR_MRZ_MISMATCH`; một trong hai không đọc được → `null` (không sinh tín hiệu lệch).
- **[Y04.10]** `test_rules.py` — bảng ca: số CCCD đúng / sai mã tỉnh / sai giới tính / sai năm; hạn thẻ ở từng mốc tuổi + ca biên 2 năm; QR ↔ MRZ khớp, lệch tên, lệch ngày sinh, thiếu một bên.
- **[Y04.C1]** COMMIT — `feat(ai-worker): read the id card mrz and validate its check digits`
- **[Y04.C2]** COMMIT — `feat(ai-worker): cross-check qr, mrz, id number structure and expiry rule`

### Y05 — Tầng 2: khuôn mặt

- **[Y05.01]** `face.detect(image)` — YuNet, trả các khuôn mặt (hộp + 5 điểm mốc + độ tin cậy) sắp theo diện tích; không có mặt / nhiều hơn 1 mặt trên selfie → ghi `errors`.
- **[Y05.02]** `face.embed(image, face)` — căn chỉnh bằng `alignCrop` của SFace → vector 128 chiều đã chuẩn hóa; vector chỉ tồn tại trong bộ nhớ của lần xử lý (N4).
- **[Y05.03]** `face.compare(card_face, selfie_face)` — cosine → `face_match_score` (làm tròn 4 chữ số); phân loại theo Y-D6: `≥ match` → `face_match = true`; giữa hai ngưỡng → `false` + `FACE_SCORE_BORDERLINE`; `< low` → `false` + `FACE_SCORE_LOW`.
- **[Y05.04]** `face.selfie_quality(image, face)` — Y-D7: độ nét vùng mặt, tỷ lệ diện tích mặt / ảnh, đúng 1 khuôn mặt, chỉ số moiré → `liveness_score` 0–1; dưới `kyc_liveness_threshold` → `LIVENESS_SUSPECT`.
- **[Y05.05]** Tôn trọng đồng ý sinh trắc (N5) — `biometric_consent = false` → bỏ cả Y05.01–Y05.04, `face_match = null`, `face_match_score = null`, `liveness_score = null`, `errors += ["BIOMETRIC_CONSENT_NOT_GIVEN"]`.
- **[Y05.06]** Thiếu mô hình / lỗi suy luận — không làm hỏng pipeline: các cờ khuôn mặt `null` + `errors += ["FACE_MODEL_UNAVAILABLE"]`.
- **[Y05.07]** `test_face.py` (`@pytest.mark.models`) — cùng một người hai ảnh → khớp; hai người khác → `FACE_SCORE_LOW`; selfie không có mặt → lỗi được ghi; selfie hai mặt; không đồng ý sinh trắc → không gọi mô hình (kiểm bằng mock); thiếu mô hình → `null`.
- **[Y05.08]** Hiệu chỉnh ngưỡng — đo điểm trên bộ ảnh mặt được phép dùng (cặp cùng người / khác người), ghi phân bố + ngưỡng chọn vào Ghi chú; cập nhật mặc định nếu khác Y-D6.
- **[Y05.09]** (Y-D5) Quốc huy / vân tay — so mẫu trên thẻ đã nắn; không kịp thì trả `null` và ghi `[-]` có lý do.
- **[Y05.C1]** COMMIT — `feat(ai-worker): match the id card face against the selfie with yunet and sface`

### Y06 — Gom kết quả & điều phối pipeline

- **[Y06.01]** `scoring.assemble(steps)` — gom kết quả các bước thành `Checks` + `signals` (không trùng mã, đúng thứ tự cố định) + `extracted` (ưu tiên QR, thiếu thì lấy MRZ) + `errors`; **không** tính điểm (N1).
- **[Y06.02]** `test_scoring.py` — hồ sơ sạch → không tín hiệu; mỗi tín hiệu một ca; nhiều tín hiệu cùng lúc; `UNREADABLE_IMAGE` chỉ xuất hiện một lần dù hai ảnh đều mờ; kết quả khớp 3 tệp JSON hợp đồng.
- **[Y06.03]** `service.verify(request)` — tải 3 ảnh song song → tầng 0 → QR → MRZ → luật → khuôn mặt; mỗi bước bọc `try` riêng (N2), đo thời gian từng bước; phần CPU chạy `run_in_threadpool`; `asyncio.Semaphore(kyc_max_concurrency)`.
- **[Y06.04]** Giới hạn thời gian — toàn pipeline tối đa 20 giây; quá hạn → gửi callback với phần đã có + `errors += ["PIPELINE_TIMEOUT"]` (backend sẽ chuyển duyệt tay).
- **[Y06.05]** `router.py` `POST /kyc/verify` — kiểm lược đồ, trả `202 VerifyAccepted`, đẩy `service.verify` + `callback.send_result` vào tác vụ nền; lỗi ngoài dự kiến trong tác vụ nền → vẫn gọi callback với `errors`.
- **[Y06.06]** `test_router.py` — thiếu / sai token → `401`; thân sai → `422`; hợp lệ → `202` và callback được gọi đúng một lần với thân hợp lệ theo lược đồ (máy chủ callback giả bằng `respx`).
- **[Y06.07]** `test_service.py` — ảnh không tải được → `UNREADABLE_IMAGE` + vẫn callback; một bước ném ngoại lệ → các bước khác vẫn chạy; hai yêu cầu đồng thời chạy tuần tự (semaphore).
- **[Y06.08]** Soát log (N4) — test bắt log của một lần chạy: không chứa số CCCD, họ tên, URL.
- **[Y06.C1]** COMMIT — `feat(ai-worker): run the kyc pipeline in the background and report signals to the backend`

### Y07 — Ghép với backend

- **[Y07.01]** Chạy hai phía ở local — backend `genie.ai-worker.mode = http`, worker `BACKEND_URL` trỏ backend, cùng `INTERNAL_TOKEN`; `storage_allowed_hosts` = host Silo local.
- **[Y07.02]** Kịch bản đạt — nộp hồ sơ bằng thẻ mẫu + selfie cùng người → backend nhận callback → `VERIFIED` (trừ khi rơi mẫu 5%).
- **[Y07.03]** Kịch bản duyệt tay — thẻ mẫu mờ / không QR → `MANUAL_REVIEW`, màn Admin hiện đúng cờ + tín hiệu.
- **[Y07.04]** Kịch bản từ chối — QR và MRZ lệch nhau + selfie khác người → `REJECTED`, lý do theo tín hiệu.
- **[Y07.05]** Kịch bản lỗi — tắt worker giữa chừng → backend chuyển `MANUAL_REVIEW` sau 30 giây; worker bật lại không gửi callback muộn làm đổi kết quả (backend idempotent — plan Mở shop & KYC K05.13).
- **[Y07.06]** Đo — thời gian pipeline (mục tiêu ≤ 10 giây) và RAM đỉnh của tiến trình worker; ghi vào Ghi chú; vượt ngân sách TS 16.2 thì giảm kích thước ảnh xử lý.
- **[Y07.C1]** COMMIT — `fix(ai-worker): align kyc pipeline with the backend after end-to-end runs`

### Y08 — Đóng gói & triển khai

- **[Y08.01]** `scripts/fetch-models.sh` — thêm kiểm SHA-256 theo `checksums.txt`, sai thì xóa tệp và dừng.
- **[Y08.02]** Dockerfile — cài gói OCR (Y-D2), bỏ JRE khỏi lớp không cần cho KYC nếu tách được, kiểm image chạy bằng người dùng `worker`; `docker build` + chạy `/health`.
- **[Y08.03]** `deploy/` — thêm biến `KYC_*`, `STORAGE_ALLOWED_HOSTS` vào `.env.example` và dịch vụ `ai-worker` của Compose; giới hạn RAM theo TS 16.2; worker không mở cổng ra ngoài mạng Compose.
- **[Y08.04]** CI — bước `ruff` + `pytest` cho `server-python` (bỏ qua test `models`); thêm nếu workflow chưa có.
- **[Y08.C1]** COMMIT — `build(ai-worker): package ocr dependencies and verify model checksums`
- **[Y08.C2]** COMMIT — `ci(ai-worker): lint and test the ai worker on pull requests`

### Y09 — Tài liệu & kết thúc

- **[Y09.01]** `server-python/README.md` — cách chạy, biến môi trường, sơ đồ pipeline, bảng tín hiệu, cách dựng ảnh mẫu, giới hạn đã biết (Y-D4, Y-D7).
- **[Y09.02]** Tài liệu nguồn — implement 8.1 (hợp đồng snake_case, worker không tính điểm), API_SPEC 12 (thân callback), TECH_STACK 2.5 (thư viện OCR), Usecase.md UC-VND-03 dòng *Hiện trạng*, LLD 7.4 nếu thứ tự bước đổi; ghi lịch sử phiên bản.
- **[Y09.03]** Tiến độ — `CHECKLIST_TIEN_DO_UC.md` (UC-VND-03, lớp AI), `Quản Lý UseCase.xlsx`; ghi việc tồn: `FACE_DUPLICATE` (Y-D4), liveness chủ động.
- **[Y09.C1]** COMMIT — `docs(ai-worker): document the kyc pipeline, signals and known limits`
- **[Y09.04]** Mở PR cuối vào `develop` — mẫu PR ghi: không đổi CSDL; thêm gói hệ thống vào image.

---

## 8. Ngoài phạm vi

- Quy đổi tín hiệu → điểm, ngưỡng, lấy mẫu, kiểm trùng CCCD, hết giờ 30 giây, email kết quả — backend (plan Mở shop & KYC K05).
- Phân tích cảm xúc đánh giá PhoBERT (`app/sentiment`) — plan riêng của cụm đánh giá.
- Trùng khuôn mặt giữa các shop (`FACE_DUPLICATE`), liveness chủ động, đọc chip NFC, đối chiếu VNeID / C06 — DATN.
- Huấn luyện / tinh chỉnh mô hình — chỉ dùng YuNet + SFace có sẵn.

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Hợp đồng lệch backend | Tệp JSON mẫu dùng chung (Y00.06) + test lược đồ hai phía; đổi hợp đồng phải sửa tệp mẫu trong cùng PR |
| Ảnh chụp điện thoại thật khó đọc hơn thẻ mẫu tự dựng | Không chặn cứng: đọc không được → tín hiệu → duyệt tay (N2); đo tỷ lệ ở Y03.07 / Y04.06 và có phương án thư viện dự phòng |
| Ngưỡng khớp mặt chưa hợp ảnh thẻ in nhỏ, mờ | Ngưỡng là cấu hình (N8); vùng "giữa" đẩy sang duyệt tay thay vì từ chối; hiệu chỉnh ở Y05.08 |
| RAM VPS thấp (worker dùng chung với PhoBERT) | 1 hồ sơ cùng lúc, thu nhỏ ảnh, 1 tiến trình uvicorn; đo ở Y07.06 |
| Rò dữ liệu cá nhân qua log / tệp tạm | Xử lý trong bộ nhớ, test soát log (Y06.08), cổng kiểm tra mục 6 |
| SSRF qua URL ảnh / `callback_url` | Danh sách host cho phép + luôn gọi lại `BACKEND_URL` cấu hình (N3), test ở Y02.03 |
| Không tải được mô hình khi demo | Worker vẫn chạy tầng 1; bước mặt trả `null` → backend chuyển duyệt tay; backend còn chế độ giả lập (`mode = stub`) |
