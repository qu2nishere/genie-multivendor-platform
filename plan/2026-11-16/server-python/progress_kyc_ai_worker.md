# PROGRESS — AI Worker: pipeline thẩm định KYC (QR, MRZ, Face Match)

| | |
|---|---|
| **Plan** | [plan_kyc_ai_worker.md](./plan_kyc_ai_worker.md) — chi tiết từng task tra theo mã `Yxx.yy` |
| **Ngày lập** | 2026-11-16 |
| **Tổng** | 65 task + 12 commit, 10 giai đoạn |
| **Nhánh** | `feature/ai-worker-kyc` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-18 — đang làm (76 / 77) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục Ghi chú.
- Task `COMMIT` chỉ tick sau khi qua Cổng kiểm tra (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`.
- Thông điệp commit ghi sẵn bên dưới, thêm ` [#GNE-<số>]` vào cuối khi đã có mã Jira.
- Cập nhật cột "Xong" của bảng Tổng quan và dòng "Cập nhật gần nhất" sau mỗi buổi làm; ghi một dòng vào Nhật ký cuối trang.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| Y-D1 | Hợp đồng với backend (trùng D6 của plan Mở shop & KYC) | ☑ | Theo đề xuất — chốt cùng backend ở ADR 0011 mục 5; mẫu `docs/api-tests/kyc-contract/` (backend có `KycContractIT` đọc trực tiếp) · 2026-11-18 |
| Y-D2 | Đọc ký tự MRZ — repo chưa có thư viện OCR | ☑ | Tesseract 5 + `pytesseract`, dữ liệu **`eng`** (gói Debian `tesseract-ocr-eng`) với whitelist `A–Z0–9<` thay cho `mrz.traineddata` (không có trong gói hệ thống); đo ở Y04.06 · 2026-11-18 |
| Y-D3 | Giải mã QR | ☑ | Theo đề xuất: `cv2.QRCodeDetector` + tiền xử lý nhiều mức; đo ở Y03.07 · 2026-11-18 |
| Y-D4 | Tín hiệu khuôn mặt trùng shop khác (`FACE_DUPLICATE`) cần kho vector khuôn mặt — chưa có cột / nơi lưu | ☑ | Theo đề xuất: đợt này `face_duplicate = null` · 2026-11-18 |
| Y-D5 | Quốc huy (`national_emblem`) và vân tay (`fingerprint`) — 2 cờ của ENTITY 1.6 | ☑ | Đợt này trả `null` cho `national_emblem` / `fingerprint` (backend cũng chưa có cột lưu — plan Mở shop & KYC K05.07) · 2026-11-18 |
| Y-D6 | Ngưỡng khớp mặt SFace (cosine) | ☑ | Theo đề xuất: 0.363 / 0.25 (biến môi trường) · 2026-11-18 |
| Y-D7 | "Liveness" phía máy chủ (chỉ có 1 ảnh selfie tĩnh) | ☑ | Theo đề xuất: chất lượng thụ động (độ nét, cỡ mặt, đúng 1 mặt, moiré) · 2026-11-18 |
| Y-D8 | Cách lấy ảnh | ☑ | Theo đề xuất: tải qua URL ký sẵn, 12 MB, 5 giây, jpeg / png, host thuộc `STORAGE_ALLOWED_HOSTS` · 2026-11-18 |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong | Mã Jira | Nhánh |
|---|---|---:|---:|---:|---|---|
| Y00 | Chuẩn bị & chốt hợp đồng | 6 | 1 | 7 / 7 | — | `feature/ai-worker-kyc` |
| Y01 | Khung: cấu hình, lược đồ, nạp mô hình | 6 | 1 | 7 / 7 | — | `feature/ai-worker-kyc` |
| Y02 | Tải ảnh an toàn & gọi lại backend | 5 | 1 | 6 / 6 | — | `feature/ai-worker-kyc` |
| Y03 | Tầng 1: mã QR mặt trước | 7 | 1 | 8 / 8 | — | `feature/ai-worker-kyc` |
| Y04 | Tầng 1: MRZ mặt sau & các luật đối chiếu | 10 | 2 | 12 / 12 | — | `feature/ai-worker-kyc` |
| Y05 | Tầng 2: khuôn mặt | 9 | 1 | 10 / 10 | — | `feature/ai-worker-kyc` |
| Y06 | Gom kết quả & điều phối pipeline | 8 | 1 | 9 / 9 | — | `feature/ai-worker-kyc` |
| Y07 | Ghép với backend | 6 | 1 | 7 / 7 | — | `feature/ai-worker-kyc` |
| Y08 | Đóng gói & triển khai | 4 | 2 | 6 / 6 | — | `feature/ai-worker-kyc` |
| Y09 | Tài liệu & kết thúc | 4 | 1 | 4 / 5 | — | `feature/ai-worker-kyc` |
| **Tổng** | | **65** | **12** | **76 / 77** | | |

## Y00 — Chuẩn bị & chốt hợp đồng

- [x] **Y00.01** Kiểm danh tính git
- [x] **Y00.02** Chốt Y-D1…Y-D8
- [x] **Y00.03** Tạo nhánh
- [x] **Y00.04** Dựng môi trường
- [x] **Y00.05** Đọc tài liệu theo thứ tự
- [x] **Y00.06** Viết tệp hợp đồng mẫu
- [x] **Y00.C1** 🔖 COMMIT `docs(ai-worker): add shared kyc contract samples for backend and ai worker` · hash: `03510c4`

## Y01 — Khung: cấu hình, lược đồ, nạp mô hình

- [x] **Y01.01** `core/config.py`
- [x] **Y01.02** `kyc/signals.py`
- [x] **Y01.03** `kyc/schemas.py`
- [x] **Y01.04** `test_schemas`
- [x] **Y01.05** `main.py` `lifespan`
- [x] **Y01.06** `health`
- [x] **Y01.C1** 🔖 COMMIT `feat(ai-worker): add kyc contract schemas, settings and model loading` · hash: `2908fe5`

## Y02 — Tải ảnh an toàn & gọi lại backend

- [x] **Y02.01** `core/images.py` `fetch_image(url)`
- [x] **Y02.02** `assess_quality(image)`
- [x] **Y02.03** `test_images.py` (dùng `respx`)
- [x] **Y02.04** `core/callback.py` `send_result(kyc_id, payload)`
- [x] **Y02.05** `test_callback.py`
- [x] **Y02.C1** 🔖 COMMIT `feat(ai-worker): download kyc images safely and call back the backend with retries` · hash: `42dd2a9`

## Y03 — Tầng 1: mã QR mặt trước

- [x] **Y03.01** `tests/fixtures/build_card.py`
- [x] **Y03.02** `qr.decode(image)`
- [x] **Y03.03** `qr.parse(raw)`
- [x] **Y03.04** Phân loại kết quả
- [x] **Y03.05** `test_qr.py`
- [x] **Y03.06** Chuẩn hóa họ tên
- [x] **Y03.07** Đo tỷ lệ đọc QR
- [x] **Y03.C1** 🔖 COMMIT `feat(ai-worker): decode and parse the citizen id qr code` · hash: `f554ea4`

## Y04 — Tầng 1: MRZ mặt sau & các luật đối chiếu

- [x] **Y04.01** Phụ thuộc OCR (Y-D2)
- [x] **Y04.02** `mrz.locate(image)`
- [x] **Y04.03** `mrz.read(region)`
- [x] **Y04.04** `mrz.parse(lines)`
- [x] **Y04.05** `test_mrz.py`
- [x] **Y04.06** Đo tỷ lệ đọc MRZ trên bộ ảnh biến thể; ghi vào Ghi chú; quyết định giữ Tesseract hay đổi (Y-D2).
- [x] **Y04.07** `rules.check_id_structure(identity_number, date_of_birth, gender)`
- [x] **Y04.08** `rules.check_expiry(date_of_birth, issue_date, expiry_date)`
- [x] **Y04.09** `rules.match_qr_mrz(qr, mrz)`
- [x] **Y04.10** `test_rules.py`
- [x] **Y04.C1** 🔖 COMMIT `feat(ai-worker): read the id card mrz and validate its check digits` · hash: `a066346`
- [x] **Y04.C2** 🔖 COMMIT `feat(ai-worker): cross-check qr, mrz, id number structure and expiry rule` · hash: `f91b701`

## Y05 — Tầng 2: khuôn mặt

- [x] **Y05.01** `face.detect(image)`
- [x] **Y05.02** `face.embed(image, face)`
- [x] **Y05.03** `face.compare(card_face, selfie_face)`
- [x] **Y05.04** `face.selfie_quality(image, face)`
- [x] **Y05.05** Tôn trọng đồng ý sinh trắc (N5)
- [x] **Y05.06** Thiếu mô hình / lỗi suy luận
- [x] **Y05.07** `test_face.py` (`@pytest.mark.models`)
- [-] **Y05.08** Hiệu chỉnh ngưỡng
- [-] **Y05.09** (Y-D5) Quốc huy / vân tay
- [x] **Y05.C1** 🔖 COMMIT `feat(ai-worker): match the id card face against the selfie with yunet and sface` · hash: `9123dfb`

## Y06 — Gom kết quả & điều phối pipeline

- [x] **Y06.01** `scoring.assemble(steps)`
- [x] **Y06.02** `test_scoring.py`
- [x] **Y06.03** `service.verify(request)`
- [x] **Y06.04** Giới hạn thời gian
- [x] **Y06.05** `router.py` `POST /kyc/verify`
- [x] **Y06.06** `test_router.py`
- [x] **Y06.07** `test_service.py`
- [x] **Y06.08** Soát log (N4)
- [x] **Y06.C1** 🔖 COMMIT `feat(ai-worker): run the kyc pipeline in the background and report signals to the backend` · hash: `711c396`

## Y07 — Ghép với backend

- [x] **Y07.01** Chạy hai phía ở local
- [-] **Y07.02** Kịch bản đạt
- [x] **Y07.03** Kịch bản duyệt tay
- [x] **Y07.04** Kịch bản từ chối
- [x] **Y07.05** Kịch bản lỗi
- [x] **Y07.06** Đo
- [-] **Y07.C1** 🔖 COMMIT `fix(ai-worker): align kyc pipeline with the backend after end-to-end runs` · hash: `…`

## Y08 — Đóng gói & triển khai

- [x] **Y08.01** `scripts/fetch-models.sh`
- [x] **Y08.02** Dockerfile
- [x] **Y08.03** `deploy/`
- [x] **Y08.04** CI
- [x] **Y08.C1** 🔖 COMMIT `build(ai-worker): package ocr dependencies and verify model checksums` · hash: `10b29b9`
- [x] **Y08.C2** 🔖 COMMIT `ci(ai-worker): lint and test the ai worker on pull requests` · hash: `832bf60`

## Y09 — Tài liệu & kết thúc

- [x] **Y09.01** `server-python/README.md`
- [x] **Y09.02** Tài liệu nguồn
- [x] **Y09.03** Tiến độ
- [ ] **Y09.C1** 🔖 COMMIT `docs(ai-worker): document the kyc pipeline, signals and known limits` · hash: `…`
- [-] **Y09.04** Mở PR cuối vào `develop`

---

## Chốt plan — tick tiến độ use case

Làm **ngay khi xong plan** (hoặc xong từng use case), trong file [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../../docs/Document/CHECKLIST_TIEN_DO_UC.md) — cách tick xem mục 0 của file đó.

- [ ] `UC-VND-03` Thẩm định KYC tự động qua AI — BE / Test / Chạy chỉ `☑` khi cả plan Mở shop & KYC (K05) và plan này (Y07) cùng xong — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [ ] Cập nhật số ở mục 1 *Tổng quan* của checklist (Đã triển khai / Đang triển khai / Chưa có backend)
- [ ] Thêm một dòng vào mục 5 *Nhật ký cập nhật* của checklist (ngày · use case · mã commit / PR)
- [ ] Đổi cột *Hiện trạng mã nguồn* của các use case trên trong `Quản Lý UseCase.xlsx`
- [ ] Tick dòng plan này ở bảng *Tiến độ các plan* trong [README](../README.md) mục 6

## Ghi chú

Ghi ở đây: lý do bỏ qua task (`[-]`), chỗ làm khác plan, số đo, việc tồn chuyển sang plan khác.

| Task | Ghi chú |
|---|---|

## Nhật ký
| Y00.03 | Nhánh `feature/ai-worker-kyc` tách từ `feature/vendor-shop-kyc` (dùng chung ADR 0011 + hợp đồng) — PR merge sau nhánh đó. |
| Y00.04 | Máy chỉ có Python 3.13 (dự án `>=3.12,<3.13`) và không có Tesseract → chạy test trong image dev `python:3.12-slim` + `tesseract-ocr` (Tesseract 5.5.0), mount mã nguồn — không cài thêm lên máy. Mô hình YuNet / SFace tải từ opencv_zoo vào `server-python/models/` (bị `.gitignore` chặn). SHA-256: yunet `8f2383e4…2552fa4`, sface `0ba9fbfa…7c34e79`. |
| Y00.05 | implement 8.1 cũ (camelCase, worker trả `riskScore`) đã đánh dấu thay thế ở plan Mở shop & KYC K12.04. |
| Y01.05 | Mô hình nạp ở `app/kyc/models.py` vào `app.state.face_models`; `/health` giờ trả `UP` sau lifespan kèm `models_loaded` (thiếu mô hình vẫn UP — tầng 1 chạy được), `LOADING` 503 trước khi khởi động xong. |
| Y02.01 | Host so khớp kiểu glob (`*.s3.ap-southeast-1.amazonaws.com`), cả dạng `host:port`; giải mã bằng Pillow (sửa hướng EXIF) rồi chuyển OpenCV BGR, cạnh dài ≤ 2000 px — tất cả trong bộ nhớ. |
| Y03.07 | Tỷ lệ đọc QR trên 32 biến thể thẻ mẫu (xoay 0/90/180/270, thu 75%, tối 70%, mờ nhẹ): **32/32 = 100%** → giữ `cv2.QRCodeDetector`, chưa cần `zxing-cpp`. Lưu ý: thẻ mẫu tự dựng sạch hơn ảnh chụp điện thoại thật — đo lại khi có ảnh thật được phép dùng. |
| Y04.02 | Định vị dải MRZ (blackhat + gradient) dễ cắt thiếu dòng → `read_image` thử: vùng định vị → nửa dưới ảnh, ở 4 hướng; chọn 3 dòng bắt đầu từ dòng `ID`. OCR trên ảnh **xám** (nhị phân Otsu làm mất ký tự `<`). |
| Y04.06 | Tỷ lệ đọc MRZ hợp lệ (đúng mọi số kiểm tra) trên 36 biến thể (lật 180°, thu 80/60%, tối 70%, mờ 3/5): **36/36 = 100%** với Tesseract 5.5 dữ liệu `eng` + whitelist → giữ Tesseract. Thẻ mẫu vẽ bằng DejaVu Sans Mono (không có phông OCR-B); ảnh thật cần đo lại. |
| Y04.08 | Mốc hạn thẻ: sinh nhật 25 / 40 / 60 kế tiếp; cấp từ (mốc − 2 năm) thì sang mốc sau; cấp từ 58 tuổi → không thời hạn (MRZ `<<<<<<` hoặc ngày ≥ 2090 coi là đúng). |
| Y04.01 | Dockerfile cài `tesseract-ocr tesseract-ocr-eng` ở commit bổ sung (lệnh sửa trong commit Y04.C1 bị lỗi cú pháp, không áp dụng). Cài trên máy dev: Windows — bộ cài UB Mannheim + thêm vào PATH; macOS — `brew install tesseract` (ghi trong README worker ở Y09). |
| Y05.04 | Liveness thụ động = 0.45 × độ nét vùng mặt + 0.3 × tỷ lệ diện tích mặt + 0.25 × (1 − moiré); khác đúng 1 mặt → 0. Moiré = đỉnh phổ tần cao / trung bình (ảnh tự nhiên ≈ 0, vân lưới → 1). |
| Y05.07 | Repo **không có ảnh khuôn mặt người thật được phép dùng** (N10) → ca cùng người / khác người kiểm bằng mô hình giả lập vector (cosine 1.0 / 0.0); test `@pytest.mark.models` dùng YuNet + SFace thật chỉ kiểm nạp mô hình và selfie không có mặt. Cần bổ sung khi nhóm có ảnh đồng ý sử dụng. |
| Y05.08 | Bỏ qua: chưa có bộ ảnh mặt được phép dùng để đo phân bố điểm — giữ ngưỡng công bố của SFace 0.363 / 0.25 (cấu hình được qua `KYC_FACE_*`). |
| Y05.09 | Bỏ qua (Y-D5): `national_emblem` / `fingerprint` trả `null`; backend cũng chưa có cột lưu. |
| Y06.03 | Thời gian một hồ sơ thẻ mẫu (không mô hình mặt): tải 6 ms, QR 79 ms, MRZ 289 ms, tổng ≈ 0.4 giây trong container. Semaphore tạo theo từng event loop. |
| Y06.08 | Test soát log bắt được 2 chỗ rò: (1) `httpx` tự ghi URL ảnh ký sẵn ở mức INFO → hạ `httpx` / `httpcore` xuống WARNING (`quiet_third_party`); (2) `pytesseract` ghi ảnh ra tệp tạm `/tmp` (trái N4) → bỏ `pytesseract`, gọi thẳng `tesseract stdin stdout` (ảnh qua bộ nhớ). Y-D2 vẫn là Tesseract. |
| Y07.01 | Chạy 2026-11-18: backend jar (DB mới `genie_ai`, `AI_WORKER_MODE=http`, `BACKEND_INTERNAL_URL=http://host.docker.internal:8080`, `PUBLIC_S3_URL=http://host.docker.internal:9000` để link ký đúng host worker thấy) ↔ worker container (`STORAGE_ALLOWED_HOSTS=host.docker.internal:9000`, mô hình thật). Không sửa `application*.yml`. |
| Y07.02 | Bỏ qua: cần cặp ảnh thẻ + selfie cùng một người thật (được phép dùng) — repo không có. Thẻ mẫu tự dựng có ô ảnh xám nên không thể ra VERIFIED qua đường thật; tầng 1 của thẻ chuẩn ra đủ 5 cờ `true` (QR, MRZ, QR↔MRZ, cấu trúc số, hạn thẻ). |
| Y07.03 | Thẻ chuẩn + selfie không có mặt → `LIVENESS_SUSPECT` 40 → `MANUAL_REVIEW`; ảnh hỏng (byte rác) → `NOT_AN_IMAGE` → `UNREADABLE_IMAGE` 30 → `MANUAL_REVIEW`. Thẻ mờ không QR + selfie không mặt → 30 + 40 = 70 → `REJECTED` (lý do theo tín hiệu). |
| Y07.04 | Mặt sau MRZ ghi tên khác QR → `QR_MRZ_MISMATCH` 60 + `LIVENESS_SUSPECT` 40 = 100 → `REJECTED`; nhân viên vẫn thấy cờ `mrz_qr_match = false`. |
| Y07.05 | Tắt worker → backend gọi lỗi ngay → `MANUAL_REVIEW` + `errors = ["AI_WORKER_UNAVAILABLE"]` (không phải chờ 30 giây). Ca callback muộn / quá 30 giây đã có IT backend (`KycCallbackIT`). |
| Y07.06 | Pipeline ≈ 0.6–1.8 giây từ lúc nộp tới khi backend có kết quả (MRZ ≈ 0.3–0.6 s là bước nặng nhất); RAM worker 250 MiB khi đã nạp YuNet + SFace — trong ngân sách TS 16.2. |
| Y07.C1 | Không có thay đổi mã sau khi chạy hai phía (không có lỗi lệch hợp đồng) → không tạo commit sửa. |
| Y08.01 | `scripts/models.sha256` (YuNet, SFace); sai → xóa tệp, thoát 1 (đã thử bằng tệp bị sửa). Không đặt `MODELS_BASE_URL` thì YuNet / SFace tải từ opencv_zoo; `SKIP_PHOBERT=1` bỏ PhoBERT (chưa có bản phát hành). |
| Y08.03 | Compose prod `ai-worker`: `STORAGE_ALLOWED_HOSTS` (mặc định `*.s3.ap-southeast-1.amazonaws.com`) + 4 biến `KYC_*`; vẫn không mở cổng ra ngoài mạng Compose; giữ giới hạn RAM 2g (đo thực tế 250 MiB). |
| Y08.04 | Job `ai-worker` trong `ci.yml` đã có → thêm cài `tesseract-ocr tesseract-ocr-eng fonts-dejavu-core`, `ruff format --check`, `pytest -m "not models"`. |
| Y08.02 | `docker build` image thật (1.7 GB — `transformers` + JRE của plan PhoBERT; JRE chưa bỏ vì cụm cảm xúc còn cần) → `/health` = `UP`, `models_loaded: true`, tiến trình chạy bằng user `worker`, Tesseract 5.5.0. |
| Y09.02 | TECH_STACK 2.5 (Tesseract qua stdin, opencv_zoo + SHA-256), Usecase.md UC-VND-03 Hiện trạng; implement 8.1 + API_SPEC 12 đã sửa ở plan Mở shop & KYC K12.04 (cùng hợp đồng). |
| Y09.03 | CHECKLIST: UC-VND-03 BE ☑ / Test ☑ (Chạy ☐), dòng Nhật ký; Excel cột U. Việc tồn: `FACE_DUPLICATE` (Y-D4), quốc huy / vân tay (Y-D5), liveness chủ động, bộ ảnh mặt được phép dùng để hiệu chỉnh ngưỡng + ca VERIFIED thật. |
| Y09.04 | Chưa mở PR — chờ người dùng (nhánh chồng trên `feature/vendor-shop-kyc`, chưa push). |

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-16 | Lập plan + progress (65 task, 12 commit, 10 giai đoạn) — chưa bắt đầu làm | — |
| 2026-11-18 | Plan AI Worker: Y00–Y09 xong (trừ ca cần ảnh mặt thật); 81 test worker xanh, chạy hai phía với backend http: lệch QR/MRZ → REJECTED, selfie không mặt → MANUAL_REVIEW, worker tắt → MANUAL_REVIEW. | 03510c4 … + commit tài liệu |
