# PROGRESS — AI Worker: pipeline thẩm định KYC (QR, MRZ, Face Match)

| | |
|---|---|
| **Plan** | [plan_kyc_ai_worker.md](./plan_kyc_ai_worker.md) — chi tiết từng task tra theo mã `Yxx.yy` |
| **Ngày lập** | 2026-11-16 |
| **Tổng** | 65 task + 12 commit, 10 giai đoạn |
| **Nhánh** | `feature/ai-worker-kyc` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-18 — đang làm (6 / 77) |

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
| Y00 | Chuẩn bị & chốt hợp đồng | 6 | 1 | 6 / 7 | — | `feature/ai-worker-kyc` |
| Y01 | Khung: cấu hình, lược đồ, nạp mô hình | 6 | 1 | 0 / 7 | — | `feature/ai-worker-kyc` |
| Y02 | Tải ảnh an toàn & gọi lại backend | 5 | 1 | 0 / 6 | — | `feature/ai-worker-kyc` |
| Y03 | Tầng 1: mã QR mặt trước | 7 | 1 | 0 / 8 | — | `feature/ai-worker-kyc` |
| Y04 | Tầng 1: MRZ mặt sau & các luật đối chiếu | 10 | 2 | 0 / 12 | — | `feature/ai-worker-kyc` |
| Y05 | Tầng 2: khuôn mặt | 9 | 1 | 0 / 10 | — | `feature/ai-worker-kyc` |
| Y06 | Gom kết quả & điều phối pipeline | 8 | 1 | 0 / 9 | — | `feature/ai-worker-kyc` |
| Y07 | Ghép với backend | 6 | 1 | 0 / 7 | — | `feature/ai-worker-kyc` |
| Y08 | Đóng gói & triển khai | 4 | 2 | 0 / 6 | — | `feature/ai-worker-kyc` |
| Y09 | Tài liệu & kết thúc | 4 | 1 | 0 / 5 | — | `feature/ai-worker-kyc` |
| **Tổng** | | **65** | **12** | **6 / 77** | | |

## Y00 — Chuẩn bị & chốt hợp đồng

- [x] **Y00.01** Kiểm danh tính git
- [x] **Y00.02** Chốt Y-D1…Y-D8
- [x] **Y00.03** Tạo nhánh
- [x] **Y00.04** Dựng môi trường
- [x] **Y00.05** Đọc tài liệu theo thứ tự
- [x] **Y00.06** Viết tệp hợp đồng mẫu
- [ ] **Y00.C1** 🔖 COMMIT `docs(ai-worker): add shared kyc contract samples for backend and ai worker` · hash: `…`

## Y01 — Khung: cấu hình, lược đồ, nạp mô hình

- [ ] **Y01.01** `core/config.py`
- [ ] **Y01.02** `kyc/signals.py`
- [ ] **Y01.03** `kyc/schemas.py`
- [ ] **Y01.04** `test_schemas`
- [ ] **Y01.05** `main.py` `lifespan`
- [ ] **Y01.06** `health`
- [ ] **Y01.C1** 🔖 COMMIT `feat(ai-worker): add kyc contract schemas, settings and model loading` · hash: `…`

## Y02 — Tải ảnh an toàn & gọi lại backend

- [ ] **Y02.01** `core/images.py` `fetch_image(url)`
- [ ] **Y02.02** `assess_quality(image)`
- [ ] **Y02.03** `test_images.py` (dùng `respx`)
- [ ] **Y02.04** `core/callback.py` `send_result(kyc_id, payload)`
- [ ] **Y02.05** `test_callback.py`
- [ ] **Y02.C1** 🔖 COMMIT `feat(ai-worker): download kyc images safely and call back the backend with retries` · hash: `…`

## Y03 — Tầng 1: mã QR mặt trước

- [ ] **Y03.01** `tests/fixtures/build_card.py`
- [ ] **Y03.02** `qr.decode(image)`
- [ ] **Y03.03** `qr.parse(raw)`
- [ ] **Y03.04** Phân loại kết quả
- [ ] **Y03.05** `test_qr.py`
- [ ] **Y03.06** Chuẩn hóa họ tên
- [ ] **Y03.07** Đo tỷ lệ đọc QR
- [ ] **Y03.C1** 🔖 COMMIT `feat(ai-worker): decode and parse the citizen id qr code` · hash: `…`

## Y04 — Tầng 1: MRZ mặt sau & các luật đối chiếu

- [ ] **Y04.01** Phụ thuộc OCR (Y-D2)
- [ ] **Y04.02** `mrz.locate(image)`
- [ ] **Y04.03** `mrz.read(region)`
- [ ] **Y04.04** `mrz.parse(lines)`
- [ ] **Y04.05** `test_mrz.py`
- [ ] **Y04.06** Đo tỷ lệ đọc MRZ trên bộ ảnh biến thể; ghi vào Ghi chú; quyết định giữ Tesseract hay đổi (Y-D2).
- [ ] **Y04.07** `rules.check_id_structure(identity_number, date_of_birth, gender)`
- [ ] **Y04.08** `rules.check_expiry(date_of_birth, issue_date, expiry_date)`
- [ ] **Y04.09** `rules.match_qr_mrz(qr, mrz)`
- [ ] **Y04.10** `test_rules.py`
- [ ] **Y04.C1** 🔖 COMMIT `feat(ai-worker): read the id card mrz and validate its check digits` · hash: `…`
- [ ] **Y04.C2** 🔖 COMMIT `feat(ai-worker): cross-check qr, mrz, id number structure and expiry rule` · hash: `…`

## Y05 — Tầng 2: khuôn mặt

- [ ] **Y05.01** `face.detect(image)`
- [ ] **Y05.02** `face.embed(image, face)`
- [ ] **Y05.03** `face.compare(card_face, selfie_face)`
- [ ] **Y05.04** `face.selfie_quality(image, face)`
- [ ] **Y05.05** Tôn trọng đồng ý sinh trắc (N5)
- [ ] **Y05.06** Thiếu mô hình / lỗi suy luận
- [ ] **Y05.07** `test_face.py` (`@pytest.mark.models`)
- [ ] **Y05.08** Hiệu chỉnh ngưỡng
- [ ] **Y05.09** (Y-D5) Quốc huy / vân tay
- [ ] **Y05.C1** 🔖 COMMIT `feat(ai-worker): match the id card face against the selfie with yunet and sface` · hash: `…`

## Y06 — Gom kết quả & điều phối pipeline

- [ ] **Y06.01** `scoring.assemble(steps)`
- [ ] **Y06.02** `test_scoring.py`
- [ ] **Y06.03** `service.verify(request)`
- [ ] **Y06.04** Giới hạn thời gian
- [ ] **Y06.05** `router.py` `POST /kyc/verify`
- [ ] **Y06.06** `test_router.py`
- [ ] **Y06.07** `test_service.py`
- [ ] **Y06.08** Soát log (N4)
- [ ] **Y06.C1** 🔖 COMMIT `feat(ai-worker): run the kyc pipeline in the background and report signals to the backend` · hash: `…`

## Y07 — Ghép với backend

- [ ] **Y07.01** Chạy hai phía ở local
- [ ] **Y07.02** Kịch bản đạt
- [ ] **Y07.03** Kịch bản duyệt tay
- [ ] **Y07.04** Kịch bản từ chối
- [ ] **Y07.05** Kịch bản lỗi
- [ ] **Y07.06** Đo
- [ ] **Y07.C1** 🔖 COMMIT `fix(ai-worker): align kyc pipeline with the backend after end-to-end runs` · hash: `…`

## Y08 — Đóng gói & triển khai

- [ ] **Y08.01** `scripts/fetch-models.sh`
- [ ] **Y08.02** Dockerfile
- [ ] **Y08.03** `deploy/`
- [ ] **Y08.04** CI
- [ ] **Y08.C1** 🔖 COMMIT `build(ai-worker): package ocr dependencies and verify model checksums` · hash: `…`
- [ ] **Y08.C2** 🔖 COMMIT `ci(ai-worker): lint and test the ai worker on pull requests` · hash: `…`

## Y09 — Tài liệu & kết thúc

- [ ] **Y09.01** `server-python/README.md`
- [ ] **Y09.02** Tài liệu nguồn
- [ ] **Y09.03** Tiến độ
- [ ] **Y09.C1** 🔖 COMMIT `docs(ai-worker): document the kyc pipeline, signals and known limits` · hash: `…`
- [ ] **Y09.04** Mở PR cuối vào `develop`

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

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-16 | Lập plan + progress (65 task, 12 commit, 10 giai đoạn) — chưa bắt đầu làm | — |
