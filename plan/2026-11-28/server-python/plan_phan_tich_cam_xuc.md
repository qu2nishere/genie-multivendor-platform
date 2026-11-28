# PLAN — AI Worker: Phân tích cảm xúc đánh giá bằng PhoBERT (UC-AI-07 phía AI Worker)

| | |
|---|---|
| **Ngày lập** | 2026-11-28 |
| **Thể loại** | server-python (FastAPI, gói `app.sentiment`) + `scripts/fetch-models.sh` + `deploy/` (biến môi trường, volume mô hình) |
| **Use case** | Phân tích cảm xúc đánh giá bằng PhoBERT — Sentiment Analysis (UC-AI-07, phần AI Worker). Phần backend (sự kiện, callback, thống kê, hàng chờ) ở [plan_danh_gia_c7.md](../server-spring/plan_danh_gia_c7.md) |
| **Tài liệu chuẩn** | UC-AI-07 (Usecase.md dòng 2296) · API 12.1 (`PATCH /internal/reviews/{id}/sentiment`), 12.2 (quét lại 15 phút) · ENTITY 9.1 (`aspect_sentiment_json`) · TS mục 2.2 (mô hình không commit), mục 7 AI Worker (`CLAUDE` mục 7 của repo: "Tiền xử lý PhoBERT giống lúc huấn luyện") · tiền lệ plan `2026-11-16/server-python/plan_kyc_ai_worker.md` (token nội bộ, callback, `fetch-models.sh`) — xem [README](../README.md) |
| **Theo dõi tiến độ** | [progress_phan_tich_cam_xuc.md](progress_phan_tich_cam_xuc.md) — cùng mã task `Zxx.yy` |
| **Phụ trách** | Người làm plan — lấy theo **`git config user.name`** trên máy thực hiện (ghi vào progress ở Z00.01); không ghi tên cố định |
| **Người commit** | danh tính git đã cấu hình trên máy đó — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Phụ thuộc** | Mô hình PhoBERT phân loại cảm xúc nhóm đã fine-tune (UC-AI-07 *Hiện trạng*) + notebook / mã tiền xử lý lúc huấn luyện. Hợp đồng HTTP chốt ở Z00.03 ↔ plan C7 R00.05. **Không** phụ thuộc chuỗi C khác |
| **Mục tiêu** | `POST /sentiment` nhận đánh giá, chạy PhoBERT ONNX trên CPU, gọi lại backend với `{label, score, model}`; đo macro-F1 + thời gian suy luận trên tập kiểm thử của nhóm; chạy được trong Docker Compose |

---

## 0. Hiện trạng đã khảo sát (`develop` @ `1b66eae`, 2026-11-28)

| Phần | Đã có | Còn thiếu |
|---|---|---|
| **Khung** | `app/main.py`, `health.py`, `core/config.py` (`pydantic-settings`), `core/security.py` (`require_internal_token` — so sánh hằng thời gian), gói `kyc` hoàn chỉnh (callback về backend, `httpx`) | — |
| **Gói `sentiment`** | `model.py`, `preprocess.py`, `router.py`, `schemas.py`, `service.py` — **chỉ có docstring + `# TODO`** | Toàn bộ |
| **Phụ thuộc** | `onnxruntime`, `transformers` (tokenizer PhoBERT), `httpx`, `numpy` đã có trong `pyproject.toml` | Thư viện tách từ tiếng Việt (D1) |
| **Mô hình** | `scripts/fetch-models.sh` đã dự kiến tệp `phobert-sentiment-$MODEL_VERSION-int8.onnx` + `SKIP_PHOBERT=1`; `scripts/models.sha256` | Bản phát hành mô hình + tokenizer, dòng SHA-256 |
| **Triển khai** | `server-python/Dockerfile`, dịch vụ `ai-worker` trong `deploy/docker-compose.yml` | Volume / biến cho mô hình cảm xúc, `SENTIMENT_*` |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | **Tiền xử lý giống hệt lúc huấn luyện** (NFC → viết thường → teencode → bỏ URL → tách từ → cắt 256 token); bộ ca kiểm lấy từ dữ liệu huấn luyện — lệch một bước là độ chính xác giảm mà không báo lỗi | UC-AI-07 b2; CLAUDE mục 7 |
| N2 | Chỉ nhận request có `X-Internal-Token` đúng; không công khai cổng ra ngoài mạng Compose | API 12.1; plan KYC |
| N3 | **Không** log nội dung đánh giá ở mức INFO (chỉ `review_id`, độ dài, nhãn, thời gian); không gửi dữ liệu ra dịch vụ ngoài | UC-AI-07 ràng buộc |
| N4 | Mô hình nạp **một lần** khi khởi động (lifespan), `GET /health` báo `sentiment: ready | missing`; thiếu tệp mô hình → worker vẫn chạy KYC, `/sentiment` trả `503` | TS 2.2 |
| N5 | `*.onnx` và tokenizer **không commit**; tải bằng `fetch-models.sh` có kiểm SHA-256 | TS 2.2 |
| N6 | Test chạy được **không cần** mô hình thật (phiên ONNX giả) — CI không tải mô hình | tiền lệ KYC |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Căn cứ | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | **Thư viện tách từ** — PhoBERT cần đầu vào đã tách từ | UC-AI-07 b2 (RDRSegmenter / VnCoreNLP) | Dùng **đúng thư viện lúc huấn luyện** (đọc notebook ở Z00.04). Nếu nhóm dùng VnCoreNLP: `py_vncorenlp` (cần Java 8+ trong image — thêm `openjdk-17-jre-headless` vào Dockerfile, tải `VnCoreNLP-1.2.jar` + `models/wordsegmenter` qua `fetch-models.sh`). Nếu dùng `underthesea` / `pyvi` thì dùng thư viện đó. **Không** đổi thư viện so với huấn luyện | ☐ |
| D2 | **Nơi để mô hình** | TS 2.2; `fetch-models.sh` | Bản phát hành nội bộ (`MODELS_BASE_URL` — GitHub Release của repo hoặc bucket Silo `ml-models`): `phobert-sentiment-v1-int8.onnx`, thư mục tokenizer `phobert-sentiment-v1-tokenizer/` (đóng gói `.tar.gz`), `labels.json` (`["NEGATIVE","NEUTRAL","POSITIVE"]` theo đúng thứ tự đầu ra); thêm SHA-256 vào `scripts/models.sha256` | ☐ |
| D3 | **Xuất ONNX & lượng tử hóa** | ONNX Runtime CPU | Xuất bằng `optimum.exporters.onnx` (hoặc `torch.onnx.export`, opset 17, trục động `batch`, `sequence`), lượng tử hóa động INT8 (`onnxruntime.quantization.quantize_dynamic`); **so sánh** nhãn ONNX INT8 với mô hình gốc trên tập kiểm thử — lệch > 1 % nhãn thì dùng FP32. Script xuất để ở `server-python/scripts/export_sentiment_onnx.py` (chạy tay, không chạy trong CI) | ☐ |
| D4 | **Đồng bộ hay gọi lại** | API 12.1; UC-AI-07 b1, b4 | Gọi lại (như KYC): `POST /sentiment {review_id, text}` → `202 {accepted: true}` ngay, xử lý nền (`BackgroundTasks` / hàng đợi trong tiến trình có giới hạn), xong gọi `PATCH {BACKEND_URL}/api/v1/internal/reviews/{id}/sentiment {label, score, model}` với `X-Internal-Token`. Thêm `POST /sentiment/sync` (trả kết quả ngay — chỉ dùng để thử / đo, cùng token) | ☐ |
| D5 | **Lô & đồng thời** | UC-AI-07 ngoại lệ (chạy lại theo lô) | `POST /sentiment/batch {items:[{review_id, text}] ≤ 64}` → `202`, suy luận theo lô 16; hàng đợi nội bộ tối đa 1.000 việc (đầy → `429`); một luồng suy luận (ONNX `intra_op_num_threads` = số lõi / 2) | ☐ |
| D6 | **Đầu ra** | UC-AI-07 b3; ENTITY 9.1 | softmax → `label` = nhãn xác suất lớn nhất, `score` = xác suất đó (làm tròn 4 chữ số), `model = "phobert-genie-v1"` (đọc từ `SENTIMENT_MODEL_NAME`); văn bản rỗng sau tiền xử lý → không gọi lại (backend giữ `NULL`) | ☐ |
| D7 | **Gọi lại lỗi** | UC-AI-07 ngoại lệ | Thử lại 3 lần (1 / 4 / 16 giây) với `httpx` timeout 5 giây; vẫn lỗi → log WARN + bỏ (backend có `SentimentRescanJob` quét lại 15 phút) | ☐ |
| D8 | **Đo chất lượng** | UC-AI-07 ràng buộc ("macro-F1 đo lại trên tập kiểm thử của nhóm") | `server-python/scripts/eval_sentiment.py` chạy trên tập kiểm thử (CSV `text,label` — **không commit** nếu có dữ liệu thật của người dùng; dùng tập công khai / tập nhóm tự gán nhãn) → macro-F1, ma trận nhầm lẫn, p50 / p95 thời gian một câu trên CPU; ghi kết quả vào `server-python/README.md` + báo cáo | ☐ |
| D9 | **Teencode** | UC-AI-07 b2 | Từ điển `resources/sentiment/teencode.tsv` (chép từ lúc huấn luyện: `ko → không`, `sp → sản phẩm`, `dc/đc → được`, `k → không`, `j → gì`…); thay theo **token nguyên từ** (không thay giữa chữ) | ☐ |
| D10 | **Cấu hình** | `core/config.py` | `SENTIMENT_MODEL_PATH`, `SENTIMENT_TOKENIZER_PATH`, `SENTIMENT_LABELS_PATH`, `SENTIMENT_MODEL_NAME`, `SENTIMENT_MAX_TOKENS=256`, `BACKEND_URL` (đã có từ KYC?), `INTERNAL_TOKEN` (đã có); khai `server-python/.env.example`, `deploy/.env.example`, `docs/cau-hinh-moi-truong.md` | ☐ |
| D11 | **Ngoài phạm vi** | — | Cảm xúc theo khía cạnh (DATN), huấn luyện lại mô hình (ngoài repo), GPU | ☐ |

---

## 3. Luồng xử lý

```
Backend (C7 SentimentDispatcher) ─POST /sentiment {review_id, text} (X-Internal-Token)─► router → 202
   └─ hàng đợi nội bộ ─► SentimentService.analyze(text)
         preprocess: NFC → lower → bỏ URL / email → teencode (D9) → tách từ (D1) → tokenizer (max 256, cắt)
         ONNX Runtime (INT8) → logits → softmax → (label, score)
      ─► CallbackClient PATCH {BACKEND_URL}/api/v1/internal/reviews/{id}/sentiment {label, score, model} (D7)
GET /health → {kyc: ready, sentiment: ready|missing, model: phobert-genie-v1}
```

---

## 4. Cây file sẽ tạo / sửa

```
server-python/
├── app/sentiment/  preprocess.py · model.py (SentimentModel: nạp ONNX + tokenizer + labels) · service.py
│                   schemas.py (SentimentRequest, BatchRequest, SentimentResult) · router.py · callback.py · queue.py
├── app/main.py                (sửa — lifespan nạp mô hình, gắn router)
├── app/health.py              (sửa — trạng thái sentiment)
├── app/core/config.py         (sửa — biến D10)
├── resources/sentiment/teencode.tsv
├── scripts/export_sentiment_onnx.py · scripts/eval_sentiment.py
├── tests/sentiment/  test_preprocess.py (ca lấy từ lúc huấn luyện) · test_model.py (phiên ONNX giả) · test_router.py
│                     test_callback.py (respx) · test_queue.py
├── Dockerfile                 (sửa — Java nếu D1 chọn VnCoreNLP)
├── .env.example · README.md   (sửa)
scripts/fetch-models.sh · scripts/models.sha256       (sửa — tokenizer, labels, VnCoreNLP nếu có)
deploy/docker-compose.yml · deploy/.env.example        (sửa — volume models, biến SENTIMENT_*)
docs/cau-hinh-moi-truong.md                            (sửa)
docs/kiem-thu/kich-ban-2026-11-28-c7.md                (thêm mục AI Worker — chung với plan C7)
```

---

## 5. Hợp đồng HTTP (chốt cùng C7 R00.05)

| Hướng | Endpoint | Body | Trả |
|---|---|---|---|
| Backend → Worker | `POST /sentiment` | `{ "review_id": 9001, "text": "Hàng đẹp, giao nhanh" }` | `202 { "accepted": true }`; token sai `401`; hàng đợi đầy `429`; mô hình thiếu `503` |
| Backend → Worker | `POST /sentiment/batch` | `{ "items": [ { "review_id": 1, "text": "…" } ] }` (≤ 64) | `202 { "accepted": n }` |
| Thử / đo | `POST /sentiment/sync` | như `/sentiment` | `200 { "label": "POSITIVE", "score": 0.9731, "model": "phobert-genie-v1" }` |
| Worker → Backend | `PATCH /api/v1/internal/reviews/{id}/sentiment` | `{ "label": "NEGATIVE", "score": 0.91, "model": "phobert-genie-v1" }` | `200` (backend) |

---

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-python && .venv/bin/pytest -q && .venv/bin/ruff check .` — test không cần mô hình thật (N6).
2. Nếu có mô hình máy dev: `POST /sentiment/sync` với 5 câu mẫu ra nhãn hợp lý; `GET /health` `sentiment: ready`.
3. `docker compose build ai-worker` chạy được (khi đụng Dockerfile).
4. Tick progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Zxx.yy` = task; `Zxx.Cn` = task commit.

### Z00 — Chuẩn bị

- **[Z00.01]** Chạy `git config user.name` / `user.email` — ghi vào dòng *Phụ trách* của progress.
- **[Z00.02]** Chốt D1–D11 vào progress.
- **[Z00.03]** Chốt hợp đồng HTTP mục 5 cùng plan C7 (R00.05) — ghi vào cả hai progress.
- **[Z00.04]** Lấy notebook / mã huấn luyện của nhóm: tên mô hình gốc (`vinai/phobert-base` / `-v2`), thứ tự nhãn, thư viện tách từ, từ điển teencode, độ dài tối đa — ghi vào *Ghi chú*.
- **[Z00.05]** Tạo nhánh `feature/ai-worker-sentiment` từ `develop`; tạo venv, `pip install -e ".[dev]"`, test KYC hiện có xanh.

### Z01 — Mô hình & phát hành

- **[Z01.01]** `scripts/export_sentiment_onnx.py` (D3) — xuất ONNX + lượng tử hóa INT8, in độ lệch nhãn so với mô hình gốc.
- **[Z01.02]** Đóng gói bản phát hành (D2): `.onnx`, tokenizer `.tar.gz`, `labels.json`; tải lên `MODELS_BASE_URL`; thêm SHA-256 vào `scripts/models.sha256`.
- **[Z01.03]** `scripts/fetch-models.sh` — tải + giải nén tokenizer, labels (và VnCoreNLP nếu D1), kiểm SHA-256; bỏ ghi chú "chưa có bản phát hành" của `SKIP_PHOBERT`.
- **[Z01.C1]** COMMIT — `build(ai-worker): add phobert sentiment export script and model fetching`

### Z02 — Tiền xử lý

- **[Z02.01]** `preprocess.py` (N1, D9) — `normalize(text)`, `segment(text)`, `prepare(text) -> str | None` (rỗng → `None`).
- **[Z02.02]** `resources/sentiment/teencode.tsv` chép từ lúc huấn luyện.
- **[Z02.03]** Test — `test_preprocess.py`: ≥ 20 cặp (đầu vào thô → chuỗi đã tách từ) **lấy từ đầu ra của mã huấn luyện**; URL / email bị bỏ; teencode không thay giữa chữ; emoji giữ / bỏ đúng như lúc huấn luyện.
- **[Z02.C1]** COMMIT — `feat(ai-worker): preprocess vietnamese reviews exactly as in phobert training`

### Z03 — Mô hình & suy luận

- **[Z03.01]** `model.py` — `SentimentModel.load(paths)` (ONNX Runtime `CPUExecutionProvider`, `intra_op_num_threads`), `predict(texts) -> list[(label, score)]` (softmax ổn định số học), cắt 256 token.
- **[Z03.02]** `main.py` lifespan nạp mô hình (thiếu tệp → trạng thái `missing`, không làm sập KYC — N4); `health.py` thêm `sentiment`.
- **[Z03.03]** Test — `test_model.py` (phiên ONNX giả trả logits cố định → nhãn / score đúng; lô 3 câu; câu > 256 token bị cắt không lỗi).
- **[Z03.C1]** COMMIT — `feat(ai-worker): load the phobert sentiment onnx model once at startup`

### Z04 — API & gọi lại backend

- **[Z04.01]** `schemas.py`, `router.py` — `POST /sentiment`, `/sentiment/batch`, `/sentiment/sync` (token N2, D4, D5).
- **[Z04.02]** `queue.py` (hàng đợi giới hạn, một luồng suy luận theo lô) + `service.py` điều phối.
- **[Z04.03]** `callback.py` — `httpx` PATCH với thử lại D7; log không chứa nội dung (N3).
- **[Z04.04]** Test — `test_router.py` (token sai 401; mô hình thiếu 503; `/sync` trả đúng; batch > 64 → 422; hàng đợi đầy 429), `test_callback.py` (respx: backend lỗi 2 lần rồi 200 → gửi 3 lần; lỗi mãi → dừng sau 3 lần), `test_queue.py`.
- **[Z04.C1]** COMMIT — `feat(ai-worker): accept review sentiment jobs and call back the backend`

### Z05 — Đo chất lượng

- **[Z05.01]** `scripts/eval_sentiment.py` (D8) — macro-F1, ma trận nhầm lẫn, p50 / p95 thời gian; chạy trên tập kiểm thử của nhóm.
- **[Z05.02]** Ghi kết quả vào `server-python/README.md` (bảng: mô hình, F1, p95, CPU máy đo) + đoạn tóm tắt cho báo cáo TLCN.
- **[Z05.C1]** COMMIT — `docs(ai-worker): record sentiment model accuracy and latency`

### Z06 — Triển khai

- **[Z06.01]** `Dockerfile` (Java nếu D1), `deploy/docker-compose.yml` (volume `models`, biến D10), `server-python/.env.example`, `deploy/.env.example`, `docs/cau-hinh-moi-truong.md`.
- **[Z06.02]** Thử `docker compose up ai-worker` máy dev: `/health` `sentiment: ready`.
- **[Z06.C1]** COMMIT — `build(deploy): configure the ai worker sentiment model volume and settings`

### Z07 — Chạy thật cùng backend

- **[Z07.01]** Backend (`AI_WORKER_MODE=http`) + AI Worker + web — tạo đánh giá có chữ (C7) → trong vài giây `aspect_sentiment_json` có kết quả; đánh giá tiêu cực rõ ràng lên đầu hàng chờ Admin; khối Cảm xúc ở Seller › Phân tích đổi số; tắt worker → backend quét lại sau 15 phút.
- **[Z07.02]** Ghi kết quả vào kịch bản `docs/kiem-thu/kich-ban-2026-11-28-c7.md` (mục AI Worker) + *Ghi chú* progress.
- **[Z07.C1]** COMMIT — `docs(ai-worker): add sentiment real-run results`

### Z08 — Hoàn thiện

- **[Z08.01]** `server-python/README.md` — cách tải mô hình, chạy, đo, biến môi trường.
- **[Z08.02]** Báo plan C7 (R09.03–R09.04) tick UC-AI-07: ghi tên ở Z00.01 vào cột Phụ trách cùng người làm C7 nếu khác người.
- **[Z08.C1]** COMMIT — `docs(ai-worker): document the sentiment service`
- **[Z08.03]** Mở PR `feature/ai-worker-sentiment` vào `develop`.

---

## 8. Ngoài phạm vi

- Huấn luyện / fine-tune lại mô hình (làm ngoài repo); cảm xúc theo khía cạnh (DATN); GPU.

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Tiền xử lý lệch lúc huấn luyện → độ chính xác giảm âm thầm | Ca kiểm lấy từ đầu ra của mã huấn luyện (Z02.03); đo F1 (Z05) |
| INT8 làm đổi nhãn | So sánh với mô hình gốc (D3), lệch > 1 % → FP32 |
| Image nặng do Java (VnCoreNLP) | Chỉ thêm khi D1 bắt buộc; JRE headless |
| Worker chậm khi nhiều đánh giá | Hàng đợi giới hạn + lô; backend thử lại + quét lại |
| Mô hình chưa có bản phát hành | `SKIP_PHOBERT=1` + backend `AI_WORKER_MODE=stub` — C7 vẫn xong phần backend |
