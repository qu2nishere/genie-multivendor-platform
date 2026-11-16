# BÁO CÁO DỰ TOÁN CHI PHÍ TRIỂN KHAI & GIẢI PHÁP AI — BẢN TLCN
## Sàn TMĐT Đa người bán Genie — Phạm vi Tiểu luận chuyên ngành (TLCN v1.1)

> **Phạm vi:** chi phí để **xây, demo và chạy thử** bản TLCN (87 use case, 64 bảng, 1 VPS) và các phương án AI: **KYC người bán** và **phân tích cảm xúc đánh giá bằng PhoBERT** (cùng AI Worker tự host) và **kiểm duyệt sản phẩm bằng Google Gemini API** (UC-CAT-12).
> **Nguồn số liệu:** đơn giá và giả định lấy từ bản DATN `DATN/kien-thuc/BAO_CAO_DU_TOAN_CHI_PHI_VA_GIAI_PHAP_AI_v1.0.md` để hai bản so sánh được; các giai đoạn T2–T3 của DATN không lặp lại ở đây.
> **Lưu ý:** mọi giá là **giả định để lập dự toán**, chưa gồm VAT; cần lấy báo giá thật trước khi chi.

---

## 📑 MỤC LỤC
- [PHẦN 0 — Tóm tắt](#p0) · [PHẦN 1 — Tham số](#p1) · [PHẦN 2 — Hạ tầng](#p2) · [PHẦN 3 — Dịch vụ bên thứ ba](#p3) · [PHẦN 4 — Tổng hợp](#p4)
- [PHẦN 5 — Chi phí một lần & chi phí ẩn](#p5) · [PHẦN 6 — Giải pháp AI KYC](#p6) · [PHẦN 6B — Kiểm duyệt sản phẩm bằng Gemini](#p6b) · [PHẦN 7 — Các dịch vụ AI để DATN](#p7) · [PHẦN 8 — Đòn bẩy tiết kiệm](#p8) · [PHẦN 9 — Việc cần xác nhận](#p9)

---

<a id="p0"></a>
## PHẦN 0 — TÓM TẮT

| Giai đoạn | Quy mô | Hạ tầng | Dịch vụ ngoài | **Tổng / tháng** |
|---|---|--:|--:|--:|
| **M0 — Phát triển & demo đồ án** (16 tuần) | Nhóm 4 người + hội đồng | 0 – 1.400k | ~125k | **≈ 0,13 – 1,5 triệu** |
| **M1 — Chạy thử có người dùng thật** (sau bảo vệ, 3–6 tháng) | ≤ 1.000 user, ≤ 50 shop, ~500 đơn/tháng, ~1.000 lượt kiểm duyệt sản phẩm/tháng | 2.240k | ~325 – 625k | **≈ 2,6 – 2,9 triệu** |

**Năm kết luận chính:**
1. **Bỏ Elasticsearch, Qdrant, RabbitMQ, Kafka, OSRM giúp MVP chạy trên 1 máy** — so với T1 của DATN (14,0 triệu/tháng hạ tầng, 7 máy) chi phí hạ tầng M1 giảm ~84%.
2. **Không dùng OTP SMS / Zalo ZNS** (khoản tốn nhất của bản đầy đủ): OTP và thông báo đi qua **email** (miễn phí ở quy mô MVP) + in-app.
3. **KYC tự host bản rút gọn (QR + MRZ + ONNX)** chạy CPU ~1–2 giây / hồ sơ, RAM < 1,5 GB — không cần GPU, không trả phí theo hồ sơ.
4. **Không làm ví khách hàng** → không phát sinh chi phí giấy phép trung gian thanh toán (vốn tối thiểu 50 tỷ đồng theo NĐ 52/2024 — xem DATN Phần 0.3).
5. **Kiểm duyệt sản phẩm bằng Gemini API** thay vì tự dựng pipeline CLIP: M0 nằm trong hạn mức miễn phí; M1 ước ~40đ / lượt → **~40–50k / tháng** cho ~1.000 lượt — rẻ hơn nhiều so với thuê thêm máy GPU / CPU cho mô hình tự host.

---

<a id="p1"></a>
## PHẦN 1 — THAM SỐ & GIẢ ĐỊNH

| Tham số | Giá trị | Nguồn |
|---|---|---|
| Đơn giá máy chủ | vCPU 130k + RAM 70k / GB + SSD 4k / GB mỗi tháng | DATN Phần 2 |
| AOV | 350.000đ | DATN BAO_CAO_KINH_DOANH 0.2 |
| Đơn hàng M1 | ~500 đơn / tháng → GMV ≈ 175 triệu / tháng | Giả định chạy thử |
| Hồ sơ KYC M1 | ~50 / tháng | Giả định |
| Lượt kiểm duyệt sản phẩm M1 | ~1.000 / tháng (sản phẩm mới + sửa tên / mô tả / ảnh) | Giả định |
| Đơn giá Gemini (bản Flash, trả phí) | ~0,30 USD / 1 triệu token vào (chữ + ảnh), ~2,50 USD / 1 triệu token ra | Bảng giá Google AI tham chiếu — **kiểm lại trước khi chạy M1** |
| Email | ~6 email / đơn + 3 email / đăng ký | Ước tính |
| Tỷ giá | 1 USD ≈ 26.500đ | Giả định |

---

<a id="p2"></a>
## PHẦN 2 — CHI PHÍ HẠ TẦNG

### 2.1 M0 — Phát triển & demo

| Phương án | Cấu hình | Chi phí / tháng |
|---|---|--:|
| **A. 0 đồng** | Chạy Docker Compose trên laptop / PC của nhóm + **Cloudflare Tunnel** (miễn phí) để hội đồng và VNPay / GHN gọi được IPN, webhook qua HTTPS | 0 |
| **B. VPS demo** (khuyến nghị 1 tháng trước bảo vệ) | 4 vCPU / 8 GB / 80 GB SSD = 4×130 + 8×70 + 80×4 | **1.400k** |

> Phương án B tránh rủi ro mạng hội trường / máy cá nhân; chỉ cần thuê 1–2 tháng. VPS giá rẻ trên thị trường có thể thấp hơn đơn giá chuẩn này.

**Phân bổ RAM trên máy 8 GB:**

| Container | RAM | Ghi chú |
|---|--:|---|
| backend (Spring Boot) | 1,5 GB | `-Xmx1536m` |
| ai-worker (ONNX, 1 worker) | 1,5 GB | Nạp mô hình 1 lần |
| postgres | 2,0 GB | `shared_buffers = 512MB` khi RAM 8 GB dùng chung |
| frontend (Astro SSR) | 0,4 GB | |
| redis, silo (kho tệp S3 — `pgsty/silo`, miễn phí, AGPL-3.0; thay MinIO từ 2026-11-15), nginx, uptime-kuma | 1,0 GB | |
| Hệ điều hành + dự phòng | 1,6 GB | |

### 2.2 M1 — Chạy thử có người dùng thật

| Thành phần | Cấu hình | Chi phí |
|---|---|--:|
| VPS ứng dụng (toàn bộ Compose) | 6 vCPU / 12 GB / 150 GB | 6×130 + 12×70 + 150×4 = **2.220k** |
| Sao lưu ngoài máy (object storage 50 GB) | | ~20k |
| **Tổng** | | **2.240k** |

> So với T1 của DATN (7 máy, 14.032k): bỏ máy Search/MQ (2.440k), OSRM (1.060k), Observability riêng (1.420k), tách DB (2.440k), Silo riêng (1.740k), staging trả tiền (592k).

---

<a id="p3"></a>
## PHẦN 3 — DỊCH VỤ BÊN THỨ BA

| Dịch vụ | M0 | M1 | Ghi chú |
|---|--:|--:|---|
| Tên miền `.com` / `.vn` (chia 12 tháng) | 125k | 125k | ~1,5 triệu / năm |
| SSL | 0 | 0 | Let's Encrypt / Cloudflare |
| Email giao dịch | 0 | 0 – 300k | Gmail SMTP (≤ 500 / ngày) khi demo; M1 dùng dịch vụ SMTP gói thấp nếu vượt |
| VNPay | 0 | phí giao dịch | Sandbox miễn phí; production cần hồ sơ doanh nghiệp, phí ~1–2%/giao dịch (tính vào chi phí biến đổi, không vào cố định) |
| SePay (theo dõi tài khoản ngân hàng, webhook VietQR) | 0 | 0 – 200k | Gói miễn phí / gói thấp theo số giao dịch — **cần lấy báo giá**; chuyển khoản liên ngân hàng NAPAS 247 thường miễn phí cho người chuyển |
| GHN (mặc định) + GHTK / ViettelPost / J&T | 0 | thu từ khách | Môi trường dev miễn phí; cước giao hàng khách trả; hãng phụ dùng adapter giả lập nếu chưa cấp tài khoản |
| Google Gemini API (kiểm duyệt sản phẩm) | 0 | ~50k | M0 dùng hạn mức miễn phí của Google AI Studio; M1 trả phí theo token (Phần 6B) |
| Đăng nhập Google (OAuth2) | 0 | 0 | Miễn phí |
| Đăng nhập Apple (Sign in with Apple) | 0 (tùy chọn) | ~220k | Cần Apple Developer Program ~99 USD / năm — **chỉ bật khi có tài khoản**, không bắt buộc cho demo |
| GitHub (repo + Actions) | 0 | 0 | Gói Free đủ cho nhóm |
| Jira / Apidog / SonarCloud | 0 | 0 | Gói miễn phí |
| Uptime Kuma | 0 | 0 | Tự host |
| Dự phòng (tra cứu tên tài khoản ngân hàng thật thay API giả lập) | 0 | ~150k | Theo báo giá đối tác chi hộ |
| **Tổng** | **~125k** | **~325 – 625k** (chưa tính Apple tùy chọn) | |

---

<a id="p4"></a>
## PHẦN 4 — TỔNG HỢP

| Khoản | M0 (phương án A) | M0 (phương án B) | M1 |
|---|--:|--:|--:|
| Hạ tầng | 0 | 1.400k | 2.240k |
| Dịch vụ ngoài | 125k | 125k | 325 – 625k |
| **Tổng / tháng** | **125k** | **1.525k** | **2,6 – 2,9 triệu** |
| Cả giai đoạn | 4 tháng ≈ 0,5 triệu | 2 tháng VPS + 4 tháng domain ≈ 3,3 triệu | 6 tháng ≈ 15,5–17,5 triệu |

**Đối chiếu doanh thu M1 (minh họa):** GMV 175 triệu × hoa hồng bình quân ~5,5% ≈ 9,6 triệu / tháng doanh thu hoa hồng; trừ chi phí biến đổi (phí cổng do sàn chịu, voucher sàn) — vẫn đủ trang trải hạ tầng M1. Chi phí nhân sự **chưa** tính.

---

<a id="p5"></a>
## PHẦN 5 — CHI PHÍ MỘT LẦN & CHI PHÍ ẨN

| Khoản | Ước tính | Ghi chú |
|---|--:|---|
| Thiết kế logo, nhận diện | 0 – 1 triệu | Tự làm / Canva |
| Hồ sơ đăng ký sàn TMĐT với Bộ Công Thương | 0 (tự làm) | Chỉ cần khi vận hành thương mại (xem báo cáo pháp lý MVP) |
| Hồ sơ mở cổng VNPay production | 0 | Cần pháp nhân, tài khoản ngân hàng doanh nghiệp |
| Tư vấn pháp lý quy chế & chính sách | 3 – 10 triệu | Khuyến nghị trước khi mở công khai |
| **Chi phí ẩn lớn nhất:** thời gian của nhóm cho kiểm thử luồng tiền (voucher 3 tầng + Điểm, Flash Sale, COD + Escrow) & sửa lỗi tích hợp VNPay / SePay / 3PL / Gemini | ~25% thời lượng dự án | Đã dành cuối Sprint 6 và Sprint 8 trong implement.md |

---

<a id="p6"></a>
## PHẦN 6 — GIẢI PHÁP AI: KYC NGƯỜI BÁN

### 6.1 Vì sao không dùng OCR toàn ảnh + DeepFace
Hướng "OCR toàn ảnh CCCD + DeepFace" (PyTorch + TensorFlow) mà DATN từng phân tích mất 5–30 giây / hồ sơ và nạp nhiều GB thư viện; chạy nhiều worker thì mỗi worker nạp lại mô hình và **hết bộ nhớ**. Đồ án xây mới nên chọn ngay hướng nhẹ: đọc QR + MRZ (không cần OCR tự do) và mô hình ONNX nhỏ.

### 6.2 Kiến trúc đề xuất cho MVP (đã đặc tả ở UC-VND-03)

| Tầng | Kỹ thuật | Thời gian | Tài nguyên |
|---|---|--:|---|
| 0 — Tài liệu | Giải mã **QR** mặt trước CCCD gắn chip (số CCCD, họ tên, ngày sinh, giới tính, địa chỉ, ngày cấp) bằng `zxing-cpp` / OpenCV; đọc 3 dòng **MRZ** mặt sau, kiểm tra check digit; đối chiếu QR ↔ MRZ ↔ thông tin khai | < 300 ms | CPU |
| 1 — Sinh trắc | **YuNet** (phát hiện mặt) + **SFace** (embedding 128 chiều) ONNX; cosine ≥ 0,85; liveness chủ động phía client (nháy mắt / quay đầu) | ~1 s | CPU, ~200 MB mô hình |
| 2 — Tài chính | Chỉ rút tiền về tài khoản ngân hàng **đúng tên** trên CCCD (UC-VND-07) | — | Không cần AI |

- **Điểm rủi ro** cộng dồn tín hiệu; < 30 tự duyệt, 30–69 người duyệt, ≥ 70 từ chối; lấy mẫu 5% hồ sơ tự duyệt để hậu kiểm.
- **Đóng gói:** 1 worker Uvicorn, nạp mô hình khi khởi động, xử lý nền (`BackgroundTasks`), giới hạn 2 GB RAM (dùng chung với mô hình PhoBERT phân tích cảm xúc — UC-AI-07). Ở M1 (~50 hồ sơ / tháng) tổng thời gian CPU < 5 phút / tháng.
- **Giới hạn phải nêu khi bảo vệ:** không đọc chip NFC / không xác thực qua C06–VNeID → thẻ giả in dữ liệu nhất quán vẫn có thể qua Tầng 0–1; rủi ro được khống chế ở Tầng 2.

### 6.3 So sánh với API eKYC bên thứ ba

| Phương án | Chi phí M1 (50 hồ sơ) | Ưu | Nhược |
|---|--:|---|---|
| **Tự host (chọn)** | ~0 (nằm trong VPS) | Không phí theo hồ sơ, thể hiện năng lực kỹ thuật trong đồ án | Không xác thực chip / C06 |
| API eKYC nội địa (VNPT, FPT.AI…) | ~400k (≈ 8.000đ / hồ sơ — giá tham chiếu) | Chính xác cao, có đọc NFC (app native) | Phụ thuộc đối tác, cần hợp đồng xử lý dữ liệu |

> Khuyến nghị: MVP tự host; khi vận hành thương mại quy mô lớn cân nhắc chuyển sang eKYC nội địa được cấp phép (dữ liệu ở lại Việt Nam).

---

<a id="p6b"></a>
## PHẦN 6B — KIỂM DUYỆT SẢN PHẨM BẰNG GOOGLE GEMINI API (UC-CAT-12)

### 6B.1 Cách dùng
Gọi `generateContent` của mô hình Gemini bản Flash với `responseMimeType = application/json` + `responseSchema` → nhận `decision` (APPROVE / REJECT / NEED_REVIEW), `riskScore` 0–100, `violations[]`, `reasoning` tiếng Việt. Gemini chỉ **sàng lọc**: `REJECT` và `riskScore` ≥ 85 bị từ chối tự động, mọi sản phẩm còn lại vẫn qua Moderator duyệt cuối (UC-CAT-13). Đặc tả: Usecase.md UC-CAT-12, implement.md 5.3.

### 6B.2 Ước tính chi phí một lượt

| Thành phần | Token | Ghi chú |
|---|--:|---|
| Chỉ dẫn hệ thống (chính sách cấm bán) | ~800 | Cố định |
| Tên, mô tả (bỏ HTML, cắt 2.000 ký tự), danh mục, thương hiệu, giá, tên giấy tờ | ~700 | |
| Tối đa 4 ảnh (ảnh ≤ 384 px mỗi chiều ≈ 258 token / ảnh) | ~1.000 | Resize trước khi gửi |
| **Vào** | **~2.500** | ≈ 0,00075 USD |
| **Ra** (JSON + giải thích ngắn) | **~300** | ≈ 0,00075 USD |
| **Tổng / lượt** | | **≈ 0,0015 USD ≈ 40đ** |

M1 ~1.000 lượt / tháng ≈ **40k đ / tháng** (làm tròn 50k cho lượt thử lại). M0 (demo, vài trăm lượt) nằm trong hạn mức miễn phí — giới hạn số lượt / phút của gói miễn phí đủ cho demo nhưng không đủ cho tải thật.

### 6B.3 So sánh phương án

| Phương án | Chi phí M1 | Ưu | Nhược |
|---|--:|---|---|
| **Gemini API (chọn cho TLCN)** | ~50k / tháng | Hiểu chữ + ảnh, không cần dữ liệu huấn luyện, làm được trong 1 sprint | Phụ thuộc dịch vụ ngoài, dữ liệu sản phẩm ra nước ngoài (không chứa dữ liệu cá nhân), cần người duyệt cuối |
| Tự host CLIP + bộ phân loại (hướng DATN) | +1–2 triệu / tháng (thêm CPU / RAM hoặc GPU) | Dữ liệu ở lại hệ thống, không phí theo lượt | Cần dữ liệu gán nhãn, tốn thời gian dựng |
| Duyệt tay 100% | 0 tiền máy | Đơn giản | Không theo kịp khi số sản phẩm tăng |

**Biện pháp giữ chi phí và an toàn:** chỉ gọi khi tạo / sửa tên, mô tả, ảnh; resize ảnh; không gửi CCCD, số tài khoản, nội dung giấy tờ; thử lại tối đa 3 lần rồi chuyển duyệt tay; đặt hạn mức chi tiêu (budget alert) trên Google Cloud.

---

<a id="p7"></a>
## PHẦN 7 — CÁC DỊCH VỤ AI CỦA DATN CHƯA LÀM Ở TLCN

| Dịch vụ (DATN) | Quyết định TLCN | Lý do |
|---|---|---|
| Kiểm duyệt ảnh sản phẩm bằng CLIP tự host | **Thay bằng Gemini API** + Moderator duyệt cuối | Phần 6B |
| Tìm kiếm bằng hình ảnh (CLIP + Qdrant) | **Bỏ** | Cần Vector DB + GPU / CPU lớn |
| Phân tích cảm xúc đánh giá (PhoBERT) | **Có — bản rút gọn** (v1.2, UC-AI-07): cảm xúc tổng thể 3 nhãn; theo khía cạnh vẫn ở DATN | Nhóm đã tự fine-tune mô hình; chạy ONNX CPU trong AI Worker có sẵn — 0 đồng / lượt, chỉ tăng giới hạn RAM AI Worker từ 1,5 lên 2 GB, VPS 8 GB vẫn đủ |
| Trợ lý mua sắm, sinh mô tả sản phẩm (LLM) | **Bỏ** | Chi phí API + rủi ro nội dung |
| Gợi ý cá nhân hóa (vector), dự báo rời bỏ | **Bỏ** → sản phẩm tương tự bằng SQL + `pg_trgm` (UC-CAT-07) | Không đủ dữ liệu hành vi |
| Chatbot CSKH | **Bỏ** | Không có ticket CSKH trong TLCN |

---

<a id="p8"></a>
## PHẦN 8 — ĐÒN BẨY TIẾT KIỆM (THEO THỨ TỰ ƯU TIÊN)

1. Dùng phương án 0 đồng (laptop + Cloudflare Tunnel) trong 10 tuần đầu; chỉ thuê VPS 1–2 tháng cuối.
2. OTP và thông báo qua email thay SMS / ZNS (OTP **chỉ** qua email).
3. Khuyến khích chuyển khoản VietQR (SePay) — không mất phí % như cổng thẻ.
4. Ảnh nén WebP phía client, giới hạn 8 ảnh / sản phẩm → ổ đĩa Silo < 10 GB cho 200 sản phẩm.
5. Một VPS duy nhất, không tách DB / search / MQ.
6. KYC tự host, 1 worker.
7. Gemini chỉ gọi khi nội dung sản phẩm thay đổi, ảnh resize ≤ 384 px, đặt budget alert.

---

<a id="p9"></a>
## PHẦN 9 — VIỆC CẦN LẤY BÁO GIÁ / XÁC NHẬN TRƯỚC KHI CHẠY THỬ M1

- [ ] Báo giá VPS thật (so với đơn giá chuẩn 130k / vCPU, 70k / GB RAM).
- [ ] Hợp đồng & phí VNPay production; hồ sơ pháp nhân.
- [ ] Gói SePay phù hợp số giao dịch / tháng và ngân hàng hỗ trợ (tài khoản doanh nghiệp của sàn).
- [ ] Tài khoản GHN production, chu kỳ đối soát COD, phí thu hộ.
- [ ] Dịch vụ tra cứu tên chủ tài khoản ngân hàng (thay API giả lập).
- [ ] Bảng giá Gemini hiện hành, hạn mức gói miễn phí / trả phí, điều khoản dùng dữ liệu của gói trả phí.
- [ ] Tài khoản API GHTK / ViettelPost / J&T (nếu muốn bật đa hãng thật).
- [ ] Pháp chế xác nhận nghĩa vụ khấu trừ thuế thay cho hộ / cá nhân kinh doanh khi sàn có chức năng thanh toán (NĐ 117/2025) — xem báo cáo pháp lý MVP.

---

## LỊCH SỬ PHIÊN BẢN
- **TLCN v1.4 (2026-11-16):** kho tệp MinIO → Silo (cùng giấy phép, cùng chi phí 0 đồng tự host; MinIO ngừng phát hành image cộng đồng — ADR 0009); SePay giữ nguyên dòng chi phí "cần lấy báo giá".
- **MVP v1.0 (2026-10-28):** viết lại từ bản đầy đủ v1.3 cho phạm vi MVP: 2 giai đoạn M0 / M1 thay T0–T3; bỏ chi phí Search / MQ / OSRM / OTP SMS / ZNS / LLM; giữ phương án KYC tự host theo tầng.
- **TLCN v1.1 (2026-10-29):** phạm vi 86 use case / 64 bảng / 16 tuần; thêm Phần 6B kiểm duyệt sản phẩm bằng Gemini API (~40đ / lượt), chi phí OAuth Google / Apple, đa hãng 3PL; tổng M1 ≈ 2,6–2,9 triệu / tháng.
