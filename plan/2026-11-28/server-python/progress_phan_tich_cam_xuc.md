# PROGRESS — AI Worker: Phân tích cảm xúc đánh giá bằng PhoBERT (UC-AI-07 phía AI Worker)

| | |
|---|---|
| **Plan** | [plan_phan_tich_cam_xuc.md](./plan_phan_tich_cam_xuc.md) — chi tiết từng task tra theo mã |
| **Ngày lập** | 2026-11-28 |
| **Use case** | Phân tích cảm xúc đánh giá bằng PhoBERT — Sentiment Analysis (UC-AI-07, phần AI Worker). Phần backend (sự kiện, callback, thống kê, hàng chờ) ở [plan_danh_gia_c7.md](../server-spring/plan_danh_gia_c7.md) |
| **Tổng** | 27 task + 8 commit, 9 giai đoạn |
| **Nhánh** | `feature/ai-worker-sentiment` |
| **Phụ trách** | _(điền kết quả `git config user.name` ở task Z00.01 — tên này dùng khi tick cột Phụ trách ở checklist / báo cáo / Excel)_ |
| **Người commit** | danh tính git đã cấu hình trên máy người làm — không `--author`, không `--no-verify`, không `Co-Authored-By` / dấu vết AI |
| **Cập nhật gần nhất** | 2026-11-28 — lập plan, chưa bắt đầu (0 / 35) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục *Ghi chú*.
- Task `COMMIT` chỉ tick sau khi qua *Cổng kiểm tra* (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`. Thông điệp đã ghi sẵn — tiếng Anh, chữ thường, đúng `commitlint.config.cjs`; có mã Jira thì thêm ` [#GNE-<số>]` cuối.
- Sau mỗi buổi làm: cập nhật cột *Xong* của bảng Tổng quan, dòng *Cập nhật gần nhất*, một dòng *Nhật ký* cuối trang.
- Cột *Chạy* của use case chỉ tick khi đã chạy thật trên giao diện (Chromium điều khiển bằng Playwright, hệ thống thật) và đạt hết ca của kịch bản — ghi cách chạy + số ca vào *Ghi chú*.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | **Thư viện tách từ** — PhoBERT cần đầu vào đã tách từ | ☐ | |
| D2 | **Nơi để mô hình** | ☐ | |
| D3 | **Xuất ONNX & lượng tử hóa** | ☐ | |
| D4 | **Đồng bộ hay gọi lại** | ☐ | |
| D5 | **Lô & đồng thời** | ☐ | |
| D6 | **Đầu ra** | ☐ | |
| D7 | **Gọi lại lỗi** | ☐ | |
| D8 | **Đo chất lượng** | ☐ | |
| D9 | **Teencode** | ☐ | |
| D10 | **Cấu hình** | ☐ | |
| D11 | **Ngoài phạm vi** | ☐ | |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong |
|---|---|---:|---:|---:|
| Z00 | Chuẩn bị | 5 | 0 | 0 / 5 |
| Z01 | Mô hình & phát hành | 3 | 1 | 0 / 4 |
| Z02 | Tiền xử lý | 3 | 1 | 0 / 4 |
| Z03 | Mô hình & suy luận | 3 | 1 | 0 / 4 |
| Z04 | API & gọi lại backend | 4 | 1 | 0 / 5 |
| Z05 | Đo chất lượng | 2 | 1 | 0 / 3 |
| Z06 | Triển khai | 2 | 1 | 0 / 3 |
| Z07 | Chạy thật cùng backend | 2 | 1 | 0 / 3 |
| Z08 | Hoàn thiện | 3 | 1 | 0 / 4 |
| **Tổng** | | **27** | **8** | **0 / 35** |

## Z00 — Chuẩn bị

- [ ] **Z00.01** Chạy `git config user.name` / `user.email`
- [ ] **Z00.02** Chốt D1–D11 vào progress.
- [ ] **Z00.03** Chốt hợp đồng HTTP mục 5 cùng plan C7 (R00.05)
- [ ] **Z00.04** Lấy notebook / mã huấn luyện của nhóm: tên mô hình gốc (`vinai/phobert-base` / `-v2`), thứ tự nhãn, thư viện tách từ, từ điển teencode, độ dài tối đa
- [ ] **Z00.05** Tạo nhánh `feature/ai-worker-sentiment` từ `develop`; tạo venv, `pip install -e ".[dev]"`, test KYC hiện có xanh.

## Z01 — Mô hình & phát hành

- [ ] **Z01.01** `scripts/export_sentiment_onnx.py` (D3)
- [ ] **Z01.02** Đóng gói bản phát hành (D2): `.onnx`, tokenizer `.tar.gz`, `labels.json`; tải lên `MODELS_BASE_URL`; thêm SHA-256 vào `scripts/models.sha256`.
- [ ] **Z01.03** `scripts/fetch-models.sh`
- [ ] **Z01.C1** 🔖 COMMIT `build(ai-worker): add phobert sentiment export script and model fetching` · hash: `…`

## Z02 — Tiền xử lý

- [ ] **Z02.01** `preprocess.py` (N1, D9)
- [ ] **Z02.02** `resources/sentiment/teencode.tsv` chép từ lúc huấn luyện.
- [ ] **Z02.03** Test
- [ ] **Z02.C1** 🔖 COMMIT `feat(ai-worker): preprocess vietnamese reviews exactly as in phobert training` · hash: `…`

## Z03 — Mô hình & suy luận

- [ ] **Z03.01** `model.py`
- [ ] **Z03.02** `main.py` lifespan nạp mô hình (thiếu tệp → trạng thái `missing`, không làm sập KYC
- [ ] **Z03.03** Test
- [ ] **Z03.C1** 🔖 COMMIT `feat(ai-worker): load the phobert sentiment onnx model once at startup` · hash: `…`

## Z04 — API & gọi lại backend

- [ ] **Z04.01** `schemas.py`, `router.py`
- [ ] **Z04.02** `queue.py` (hàng đợi giới hạn, một luồng suy luận theo lô) + `service.py` điều phối.
- [ ] **Z04.03** `callback.py`
- [ ] **Z04.04** Test
- [ ] **Z04.C1** 🔖 COMMIT `feat(ai-worker): accept review sentiment jobs and call back the backend` · hash: `…`

## Z05 — Đo chất lượng

- [ ] **Z05.01** `scripts/eval_sentiment.py` (D8)
- [ ] **Z05.02** Ghi kết quả vào `server-python/README.md` (bảng: mô hình, F1, p95, CPU máy đo) + đoạn tóm tắt cho báo cáo TLCN.
- [ ] **Z05.C1** 🔖 COMMIT `docs(ai-worker): record sentiment model accuracy and latency` · hash: `…`

## Z06 — Triển khai

- [ ] **Z06.01** `Dockerfile` (Java nếu D1), `deploy/docker-compose.yml` (volume `models`, biến D10), `server-python/.env.example`, `deploy/.env.example`, `docs/cau-hinh-moi-truong.md`.
- [ ] **Z06.02** Thử `docker compose up ai-worker` máy dev: `/health` `sentiment: ready`.
- [ ] **Z06.C1** 🔖 COMMIT `build(deploy): configure the ai worker sentiment model volume and settings` · hash: `…`

## Z07 — Chạy thật cùng backend

- [ ] **Z07.01** Backend (`AI_WORKER_MODE=http`) + AI Worker + web
- [ ] **Z07.02** Ghi kết quả vào kịch bản `docs/kiem-thu/kich-ban-2026-11-28-c7.md` (mục AI Worker) + *Ghi chú* progress.
- [ ] **Z07.C1** 🔖 COMMIT `docs(ai-worker): add sentiment real-run results` · hash: `…`

## Z08 — Hoàn thiện

- [ ] **Z08.01** `server-python/README.md`
- [ ] **Z08.02** Báo plan C7 (R09.03–R09.04) tick UC-AI-07: ghi tên ở Z00.01 vào cột Phụ trách cùng người làm C7 nếu khác người.
- [ ] **Z08.C1** 🔖 COMMIT `docs(ai-worker): document the sentiment service` · hash: `…`
- [ ] **Z08.03** Mở PR `feature/ai-worker-sentiment` vào `develop`.

## Ghi chú

| Task | Ghi chú |
|---|---|
| | |

## Nhật ký

| Ngày | Việc đã làm | Commit / PR |
|---|---|---|
| 2026-11-28 | Lập plan + progress (27 task, 8 commit, 11 quyết định chờ chốt) | nhánh `feature/plan-c3-c11` |
