# KYC trong Genie — giải thích từ số 0

> Dành cho người mới: chưa rõ HTTP, chưa rõ KYC, đọc code thấy rối.
> Đọc theo thứ tự từ trên xuống; mỗi phần dựa vào phần trước.
> Thử tay trên máy: [huong-dan-chay-local.md](huong-dan-chay-local.md) mục 6 · kịch bản: [kiem-thu/kich-ban-2026-11-16-mo-shop-kyc.md](kiem-thu/kich-ban-2026-11-16-mo-shop-kyc.md) · chưa làm: [kiem-thu/kyc-viec-chua-lam.md](kiem-thu/kyc-viec-chua-lam.md).

---

## Phần 1. Bài toán: Genie là gì, vì sao cần KYC

### 1.1. Genie là sàn thương mại điện tử nhiều người bán

Hình dung giống Shopee / Lazada / Tiki:

```
           ┌──────────────── Sàn Genie (bên trung gian) ────────────────┐
           │                                                             │
 Người mua ──► chọn hàng, thanh toán ──► tiền GIỮ TẠM ở sàn (escrow) ──► sau khi nhận hàng 7 ngày ──► chuyển cho Người bán
           │                                                             │
 Người bán ──► mở shop, đăng sản phẩm, giao hàng, rút tiền về ngân hàng  │
           │                                                             │
 Nhân viên sàn (Admin, Moderator, Tài chính, CSKH) ──► duyệt, kiểm soát   │
           └─────────────────────────────────────────────────────────────┘
```

Sàn **không tự bán hàng**. Sàn là nơi người lạ mua – bán với nhau, và **giữ tiền hộ** để hai bên tin nhau.

### 1.2. Rủi ro nếu ai cũng mở được shop

| Rủi ro | Ví dụ |
|---|---|
| Lừa đảo | Mở shop bằng tên giả, nhận tiền rồi không giao hàng, biến mất |
| Hàng giả / cấm bán | Không biết ai chịu trách nhiệm trước pháp luật |
| Rửa tiền / chiếm đoạt | Rút tiền về tài khoản ngân hàng **của người khác** |
| Một người mở nhiều shop | Bị khóa shop này thì mở shop khác bằng cùng giấy tờ |
| Pháp lý | Quy định TMĐT ở Việt Nam yêu cầu sàn phải thu thập và xác minh thông tin định danh người bán |

### 1.3. KYC là gì

**KYC = Know Your Customer = "Biết rõ khách hàng của mình"** — thuật ngữ ngân hàng / tài chính. Ở Genie, KYC là
quy trình **xác minh người mở shop là người thật, đúng như họ khai**, trước khi cho phép bán hàng và nhận tiền:

1. Người bán khai **số CCCD**, họ tên, loại hình kinh doanh.
2. Tải lên **ảnh CCCD mặt trước, mặt sau** và **ảnh chân dung (selfie)**; hộ / doanh nghiệp thì thêm **giấy phép kinh doanh + mã số thuế**.
3. Hệ thống (AI) + nhân viên kiểm tra: giấy tờ có thật không, có khớp lời khai không, selfie có đúng người trên CCCD không.
4. Đạt → shop được **VERIFIED** → mới mở khóa đăng sản phẩm, nhận đơn, khai tài khoản ngân hàng để rút tiền.

> Ví dụ đời thường: mở tài khoản ngân hàng online (eKYC) — chụp CCCD 2 mặt, quay mặt, chờ duyệt. Genie làm y vậy cho người bán.

---

## Phần 2. Kiến thức nền cần biết trước

### 2.1. HTTP — cách các phần mềm "nói chuyện" với nhau

Genie gồm nhiều chương trình chạy riêng: **trình duyệt (web)**, **backend Spring Boot**, **AI Worker Python**, **Postgres**,
**Silo (kho tệp)**… Chúng nói chuyện bằng **HTTP**: một bên gửi **yêu cầu (request)**, bên kia trả **phản hồi (response)**.
Giống gọi món ở quán: bạn đưa phiếu order (request) → bếp trả món hoặc báo hết món (response).

Một request gồm:

```
POST http://localhost:8080/api/v1/seller/kyc/submissions        ← PHƯƠNG THỨC + ĐỊA CHỈ
Authorization: Bearer eyJhbGciOi...                             ← HEADER: thẻ "tôi là ai" (token đăng nhập)
Content-Type: application/json
                                                                ← BODY: dữ liệu gửi kèm (JSON)
{ "identity_number": "079203000145", "full_name": "Nguyễn Văn A", ... }
```

| Phần | Ý nghĩa | Ví dụ |
|---|---|---|
| **Phương thức** | Muốn làm gì | `GET` = đọc · `POST` = tạo / gửi · `PUT` = ghi đè · `PATCH` = sửa một phần · `DELETE` = xóa |
| **Địa chỉ (URL)** | Làm với cái gì | `/api/v1/seller/kyc` = hồ sơ KYC của shop đang đăng nhập |
| **Header** | Thông tin phụ | `Authorization: Bearer <token>` — token có được sau khi đăng nhập |
| **Body** | Dữ liệu | JSON: `{ "tên_trường": giá_trị }` |

Response có **mã trạng thái** (status code):

| Mã | Nghĩa | Gặp ở KYC khi |
|---|---|---|
| `200` / `201` / `202` / `204` | Thành công (202 = "đã nhận, đang xử lý sau") | Nộp hồ sơ, duyệt |
| `401` | Chưa đăng nhập / token hết hạn | Gọi API không kèm token |
| `403` | Đăng nhập rồi nhưng **không có quyền** | Nhân viên Tài chính mở màn duyệt KYC |
| `404` | Không tìm thấy | Hồ sơ không tồn tại |
| `409` | Xung đột trạng thái | Nộp lại khi hồ sơ đang xử lý (`GNE-VND-4095`) |
| `422` | Dữ liệu sai | Thiếu ảnh selfie (`GNE-VND-4223`), CCCD không đủ 12 số |

Ngoài mã số, Genie trả thêm **mã lỗi riêng** dạng `GNE-<MIỀN>-<HTTP><STT>` (vd `GNE-VND-4223` = miền Vendor, HTTP 422, lỗi số 3)
để web hiện đúng câu tiếng Việt.

**Đường dẫn API của Genie** chia theo "ai được gọi":

| Tiền tố | Ai gọi | Ví dụ KYC |
|---|---|---|
| `/api/v1/public/…` | Ai cũng được, không cần đăng nhập | `/public/banks` (danh mục ngân hàng) |
| `/api/v1/me/…` | Người dùng đã đăng nhập, dữ liệu của chính họ | `/me/shop-registrations` (đăng ký mở shop) |
| `/api/v1/seller/…` | Chủ shop | `/seller/kyc`, `/seller/payout-account` |
| `/api/v1/admin/…` | Nhân viên sàn, theo quyền | `/admin/kyc-submissions` (hàng chờ duyệt) |
| `/api/v1/internal/…` | **Chỉ máy gọi máy** (AI Worker → backend), có mật khẩu nội bộ | `/internal/kyc/{id}/result` |

### 2.2. Đồng bộ và bất đồng bộ — vì sao nộp KYC không có kết quả ngay

- **Đồng bộ (synchronous):** gọi xong **chờ** có kết quả mới làm tiếp. Như gọi điện: nói – nghe ngay.
- **Bất đồng bộ (asynchronous):** gửi yêu cầu xong **làm việc khác**, khi nào xong bên kia **báo lại**. Như gửi đồ đi sửa: nhận phiếu hẹn, sửa xong họ gọi bạn.

AI đọc ảnh mất vài giây đến vài chục giây. Nếu bắt người bán đứng chờ, trình duyệt "treo" và dễ lỗi mạng. Nên Genie làm
**bất đồng bộ**: nộp hồ sơ → trả về ngay "đang xác minh" → AI làm xong **gọi ngược lại** (callback) báo kết quả → người bán
trang `/seller/kyc` **tự làm mới mỗi 3 giây** trong lúc chờ nên thấy trạng thái mới ngay (không cần F5).

**Callback** = "gọi lại": bên A nhờ bên B làm việc và đưa sẵn địa chỉ để B báo kết quả về. Ở đây:
backend (A) nhờ AI Worker (B); B xong thì gọi `PATCH /api/v1/internal/kyc/{id}/result` của A.

### 2.3. Mã hóa — vì sao ảnh CCCD trong Silo "mở không được"

Ảnh CCCD + selfie là **dữ liệu cá nhân nhạy cảm**. Lộ ra là bị mạo danh vay tiền, mở tài khoản. Nên Genie **mã hóa**
(AES-256-GCM) trước khi cất vào Silo:

```
Ảnh gốc (đọc được) ──[khóa bí mật KYC_AES_KEY]──► tệp .enc (một đống byte vô nghĩa) ──► cất trong bucket kyc-dossiers
```

- Không có khóa thì tải tệp `.enc` về cũng **không xem được** — kể cả người quản trị Silo / AWS.
- Khóa chỉ nằm ở backend (biến môi trường, máy dev dùng khóa mẫu trong `application-local.yml`).
- "GCM" còn kèm **mã kiểm tra**: ai sửa lén 1 byte trong tệp thì giải mã báo lỗi ngay.

### 2.4. Presigned URL — link xem ảnh tự hết hạn

Khi nhân viên cần xem ảnh, hoặc AI cần đọc ảnh, backend **giải mã ra một bản tạm** và tạo **presigned URL** — một đường
link có **chữ ký + thời hạn 5 phút** (cuối link có `?X-Amz-Signature=...&X-Amz-Expires=300`). Hết 5 phút link chết; job
`KycViewCleanupJob` xóa luôn bản tạm sau ~10 phút. Như vé xem phim có giờ chiếu — qua giờ là vô dụng.

Mỗi lần nhân viên mở ảnh, hệ thống **ghi nhật ký** `KYC_DOCUMENT_VIEWED` (ai, lúc nào) — để truy vết nếu dữ liệu bị lộ.

### 2.5. Điểm rủi ro (risk score) — AI không tự "phán", chỉ báo dấu hiệu

AI Worker **không quyết định** duyệt hay từ chối. Nó chỉ trả về **danh sách tín hiệu đáng ngờ** (signals). Backend cộng
**điểm** cho từng tín hiệu theo bảng cấu hình (`genie.kyc.risk.points` trong `application.yml`), rồi so ngưỡng:

| Tín hiệu | Nghĩa | Điểm |
|---|---|---:|
| `QR_MRZ_MISMATCH` | Dữ liệu trong mã QR (mặt trước) khác dòng MRZ (mặt sau) → nghi giấy tờ ghép / sửa | 60 |
| `ID_STRUCTURE_MISMATCH` | 12 số CCCD sai cấu trúc (mã tỉnh, mã giới tính–thế kỷ, năm sinh) | 40 |
| `EXPIRY_RULE_MISMATCH` | Ngày hết hạn không đúng mốc 25 / 40 / 60 tuổi theo luật | 30 |
| `DUPLICATE_ID` | Số CCCD này đã gắn với **shop khác** đang hoạt động *(backend tự kiểm)* | 50 |
| `FACE_DUPLICATE` | Khuôn mặt trùng người đã đăng ký *(chưa làm — DATN, xem [kiem-thu/kyc-viec-chua-lam.md](kiem-thu/kyc-viec-chua-lam.md))* | 70 |
| `FACE_SCORE_BORDERLINE` | Selfie hơi giống ảnh CCCD nhưng chưa chắc | 25 |
| `FACE_SCORE_LOW` | Selfie **không giống** ảnh trên CCCD | 60 |
| `LIVENESS_SUSPECT` | Selfie có dấu hiệu chụp lại màn hình / ảnh in, không có mặt, nhiều mặt | 40 |
| `LEGACY_CARD` | Thẻ không có QR (thẻ đời cũ) → khó kiểm tự động | 30 |
| `DECLARED_DATA_MISMATCH` | Số CCCD / họ tên **khai** khác dữ liệu **đọc từ thẻ** *(backend tự kiểm)* | 20 |
| `UNREADABLE_IMAGE` | Ảnh mờ, nhỏ, hỏng, không đọc được | 30 |

```
Tổng điểm  <  30  →  PASS     → tự duyệt VERIFIED (5% vẫn bị rút ngẫu nhiên cho nhân viên hậu kiểm)
30  ≤  điểm  < 70 →  PARTIAL  → MANUAL_REVIEW (vào hàng chờ, nhân viên quyết)
Tổng điểm  ≥  70  →  FAIL     → REJECTED (từ chối, kèm lý do)
```

Vì sao tách "AI báo – backend chấm"? Để **đổi chính sách** (ví dụ siết ngưỡng) chỉ cần sửa cấu hình backend, không phải
huấn luyện / triển khai lại AI; và quyết định cuối cùng luôn nằm ở hệ thống chính, dễ giải thích với người bán.

**Quan trọng:** người bán **không đồng ý xử lý sinh trắc học** (không cho so khuôn mặt) → bỏ phần khuôn mặt và **luôn
chuyển nhân viên duyệt**, không tự duyệt.

### 2.6. AI Worker đọc gì trên CCCD

CCCD gắn chip có 2 nguồn dữ liệu máy đọc được:

| Nguồn | Ở đâu | Chứa gì | AI dùng để |
|---|---|---|---|
| **Mã QR** | Mặt trước, góc trên phải | Số CCCD, số CMND cũ, họ tên, ngày sinh, giới tính, địa chỉ, ngày cấp | Đọc dữ liệu chuẩn, so với lời khai |
| **MRZ** (Machine Readable Zone) | 3 dòng chữ `IDVNM...<<<` ở mặt sau | Số giấy tờ, ngày sinh, giới tính, hạn thẻ, họ tên + **chữ số kiểm tra** | So chéo với QR; chữ số kiểm tra sai = nghi bị sửa |

Thêm:
- **Khuôn mặt:** YuNet tìm mặt trong ảnh, SFace biến khuôn mặt thành vector số, so độ giống (cosine) giữa selfie và ảnh trên CCCD.
- **Liveness thụ động:** đoán selfie có phải người thật trước camera (không phải chụp lại màn hình / ảnh in).
- **Luật CCCD:** 3 số đầu = mã tỉnh, số thứ 4 = giới tính + thế kỷ sinh, số 5–6 = 2 số cuối năm sinh; hạn thẻ theo mốc 25 / 40 / 60 tuổi.

Code: `server-python/app/kyc/` (`qr.py`, `mrz.py`, `face.py`, `rules…`, `scoring.py` gom tín hiệu — **không tính điểm**).

---

## Phần 3. Luồng KYC từ đầu đến cuối

### 3.1. Bức tranh tổng

```
 [1] Đăng ký mở shop        [2] Nộp hồ sơ KYC         [3] AI thẩm định        [4] Chốt kết quả           [5] Sau khi VERIFIED
 /become-seller             /seller/kyc               (bất đồng bộ)           (backend chấm điểm)       /seller/payout-account
 OTP email TK               CCCD + 3 ảnh               QR · MRZ · khuôn mặt   PASS / PARTIAL / FAIL     tài khoản nhận tiền (OTP)
 → tạo shop, KYC =          → mã hóa, lưu Silo         → trả tín hiệu         → VERIFIED / hàng chờ /   → mở khóa bán hàng
   NOT_SUBMITTED            → PENDING → AI_PROCESSING                           REJECTED
                                                                                    │
                                                              [6] Nhân viên duyệt tay (/admin/kyc): Duyệt · Từ chối · Yêu cầu bổ sung · Đưa lại hàng chờ
```

### 3.2. Các trạng thái của hồ sơ (`vendor_profiles.kyc_status`)

```
NOT_SUBMITTED ──nộp──► PENDING ──gửi AI──► AI_PROCESSING ──┬─ điểm < 30 ──────────► VERIFIED  ✅ (bán được)
   (mới mở shop,                                           ├─ 30 ≤ điểm < 70 ─────► MANUAL_REVIEW 👀
    có thể lưu nháp)                                       ├─ điểm ≥ 70 ──────────► REJECTED  ❌
                                                           └─ AI lỗi / quá 30 giây ► MANUAL_REVIEW 👀

MANUAL_REVIEW ──nhân viên──┬─ Duyệt ──────────────► VERIFIED
                           ├─ Từ chối (có lý do) ─► REJECTED
                           └─ Yêu cầu bổ sung ────► SUPPLEMENT_REQUESTED ──người bán gửi lại──► PENDING → AI_PROCESSING …
                                                         └─ quá 7 ngày không gửi ──────────────► REJECTED ("Quá hạn bổ sung")

REJECTED ──người bán bấm "Yêu cầu xem xét thủ công" (CHỈ 1 LẦN)──► MANUAL_REVIEW
REJECTED ──người bán sửa & nộp lại hồ sơ──► PENDING → …
```

| Trạng thái | Người bán thấy | Bán hàng được? |
|---|---|---|
| `NOT_SUBMITTED` | "Hãy nộp hồ sơ xác minh" | ❌ (Seller Center bị `KycGate` chặn) |
| `PENDING` / `AI_PROCESSING` | "Đang xác minh" | ❌ |
| `MANUAL_REVIEW` | "Đang chờ nhân viên duyệt" | ❌ |
| `SUPPLEMENT_REQUESTED` | Danh sách giấy tờ cần chụp lại + hạn 7 ngày | ❌ |
| `REJECTED` | Lý do từ chối + nút yêu cầu xem xét | ❌ |
| `VERIFIED` | Mở khóa toàn bộ Seller Center | ✅ |

### 3.3. Từng bước chi tiết — ai gọi gì, dữ liệu đi đâu

**Bước 1 — Đăng ký mở shop (UC-VND-01)**

```
Trình duyệt ── POST /me/shop-registrations {tên shop, ngành, loại hình…} ──► Backend
Backend: tên shop trùng? (409 GNE-VND-4094) · đã có shop? (409 GNE-VND-4093) → gửi OTP tới EMAIL TÀI KHOẢN (Mailpit)
         (email shop = email tài khoản — web hiện ô chỉ đọc, backend bỏ qua email client gửi)
Trình duyệt ── POST /me/shop-registrations/{challengeId}/confirm {otp} ──► Backend
Backend: tạo vendor_profiles (kyc_status = NOT_SUBMITTED), nâng tài khoản CUSTOMER → VENDOR
```
OTP để chứng minh người đang đăng ký đúng là chủ email tài khoản.

**Bước 2 — Nộp hồ sơ KYC (UC-VND-02)**

```
(a) Tải ảnh — làm 3 lần cho ID_FRONT, ID_BACK, SELFIE (+ BUSINESS_LICENSE nếu cần):
    Trình duyệt ── xin link tải lên ──► Backend trả presigned PUT (link ghi tạm 10 phút vào tmp/)
    Trình duyệt ── PUT ảnh thẳng lên Silo ──► Silo                          (ảnh KHÔNG đi qua backend → nhẹ backend)
    Trình duyệt giữ lại "object_key" (tên tệp tạm)

(b) Lưu nháp (tùy chọn): PUT /seller/kyc/draft — mọi trường đều được bỏ trống

(c) Gửi duyệt: POST /seller/kyc/submissions {số CCCD, họ tên, mã số thuế?, đồng ý sinh trắc, các object_key}
    Backend kiểm: CCCD đúng 12 số · đủ giấy tờ bắt buộc (thiếu → 422 GNE-VND-4223) · ảnh ≥ 20KB · đang xử lý? (409 GNE-VND-4095)
    Backend "niêm phong" từng ảnh: đọc tệp tmp/ → mã hóa AES → ghi kyc-dossiers/vendor/{shopId}/…enc → xóa tệp tạm
    kyc_status = PENDING, ghi nhật ký KYC_SUBMITTED, COMMIT vào Postgres
    Trả về ngay cho trình duyệt (không chờ AI)
```

**Bước 3 — Gửi AI thẩm định (UC-VND-03) — sau khi đã commit**

```
KycAiDispatcher (chạy nền, SAU commit):
    kyc_status = AI_PROCESSING
    giải mã 3 ảnh ra bản xem tạm + presigned GET 5 phút
    gọi KycVerificationClient.requestVerification(...)
        ├─ chế độ http: POST http://AI_WORKER/kyc/verify  (header X-Internal-Token)
        │       body: { kyc_id, documents: {id_front, id_back, selfie: <link 5 phút>}, declared: {identity_number, full_name},
        │               biometric_consent, callback_url }
        │       AI trả ngay 202 "đã nhận" rồi xử lý nền
        └─ chế độ stub: không gọi ai cả, tự bịa kết quả theo 2 số cuối CCCD, 2 giây sau tự áp kết quả
    gọi lỗi (worker tắt, 5xx, timeout) → chuyển MANUAL_REVIEW ngay, không để hồ sơ kẹt
```
Vì sao "sau commit"? Nếu gửi AI **trước** khi dữ liệu chắc chắn đã lưu, mà transaction lại rollback → AI xử lý một hồ sơ
không tồn tại. Gửi sau commit thì luôn an toàn.

**Bước 4 — AI xử lý và gọi lại**

```
AI Worker (server-python):
    tải 3 ảnh qua link 5 phút (chỉ từ host được phép — chống SSRF) → kiểm chất lượng ảnh
    → đọc QR mặt trước → đọc MRZ mặt sau → so QR ↔ MRZ → luật số CCCD, hạn thẻ
    → (nếu đồng ý sinh trắc) so khuôn mặt + liveness
    → gom thành { checks, signals, extracted, face_match_score, … }   (KHÔNG tính điểm)
    → PATCH {BACKEND_URL}/api/v1/internal/kyc/{id}/result   (header X-Internal-Token; lỗi thì thử lại sau 1 / 2 / 4 giây)
```
Không ghi ảnh xuống đĩa, không ghi số CCCD / họ tên vào log — giảm rủi ro lộ dữ liệu.

**Bước 5 — Backend chốt kết quả (`KycDecisionService`)**

```
khóa dòng hồ sơ; không còn AI_PROCESSING → bỏ qua (callback lặp / đến muộn → không xử lý 2 lần)
thêm tín hiệu phía backend: DUPLICATE_ID, DECLARED_DATA_MISMATCH
không đồng ý sinh trắc → bỏ tín hiệu khuôn mặt, ép MANUAL_REVIEW
tính điểm → lưu kyc_results → PASS / PARTIAL / FAIL → đổi kyc_status → gửi email + thông báo
Nếu AI không gọi lại trong 30 giây → KycAiTimeoutJob chuyển MANUAL_REVIEW
```

**Bước 6 — Nhân viên duyệt tay (màn `/admin/kyc`)**

| Thao tác | API | Ai được làm | Ghi chú |
|---|---|---|---|
| Xem hàng chờ | `GET /admin/kyc-submissions` | quyền `REVIEW_KYC` (Moderator, Admin) | Rủi ro cao xếp trước |
| Mở hồ sơ | `GET /admin/kyc-submissions/{id}` | như trên | Ảnh = link 5 phút; mỗi lần mở ghi `KYC_DOCUMENT_VIEWED` |
| Duyệt | `POST …/{id}/approve` | như trên | Đặt **hoa hồng riêng** cần thêm quyền `UPDATE_COMMISSION` (chỉ Admin; 0–50%) |
| Từ chối | `POST …/{id}/reject` | như trên | Chọn lý do; lý do "Khác" bắt buộc ghi chú |
| Yêu cầu bổ sung | `POST …/{id}/request-supplement` | như trên | Chọn ≥ 1 giấy tờ; hạn 7 ngày |
| Đưa lại hàng chờ | `POST …/{id}/requeue` | như trên | Mở lại hồ sơ đã chốt để xét lại |

Mọi thao tác ghi **nhật ký hệ thống** (`KYC_APPROVED`, `KYC_REJECTED`, `KYC_SUPPLEMENT_REQUESTED`, `KYC_REQUEUED`,
`COMMISSION_UPDATED`) — xem ở `/admin/audit-logs`, nhóm KYC.

**Bước 7 — Sau khi VERIFIED: tài khoản nhận tiền (UC-VND-07)**

```
GET /public/banks → chọn ngân hàng → POST /seller/payout-account/verification (tra tên chủ tài khoản)
    tên chủ tài khoản phải KHỚP họ tên trên KYC (không khớp → 422 GNE-WAL-4222)
PUT /seller/payout-account  → yêu cầu OTP bước 2 (PAYOUT_ACCOUNT_UPDATE) gửi email → nhập OTP → lưu (số TK hiển thị che ****)
DELETE /seller/payout-account → OTP PAYOUT_ACCOUNT_DELETE
Shop chưa VERIFIED → 403 GNE-VND-4031
```
Đây là chốt chặn cuối: dù ai đó chiếm được tài khoản người bán, muốn đổi ngân hàng nhận tiền vẫn phải có OTP email, và
tiền chỉ về tài khoản **đứng tên đúng người đã KYC**. (Máy dev không có ngân hàng thật: số TK kết thúc `000` giả lập "tên không khớp".)

### 3.4. Bản đồ code

| Việc | Backend `server-spring/src/main/java/com/genie/vendor/` | Web `web/src/` |
|---|---|---|
| Mở shop | `shop/api`, `shop/application` | `/become-seller` |
| Hồ sơ người bán | `kyc/api/SellerKycController`, `kyc/application/KycSubmissionService` | Seller SPA `/seller/kyc` |
| Mã hóa / bản xem tạm | `kyc/application/KycDossierStorage`, `common/crypto/AesGcmCipher` | — |
| Gửi AI | `kyc/application/KycAiDispatcher` → `kyc/infra/HttpKycVerificationClient` hoặc `StubKycVerificationClient` | — |
| Nhận kết quả AI | `kyc/api/InternalKycController` → `kyc/application/KycDecisionService`, `KycRiskPolicy` | — |
| Duyệt tay | `kyc/api/AdminKycController`, `kyc/application/KycReviewService` | Admin SPA `/admin/kyc` |
| Job nền | `KycAiTimeoutJob` (30 giây), `KycSupplementExpiryJob` (01:00), `KycViewCleanupJob` (dọn bản xem) | — |
| Tài khoản nhận tiền | `settings/…` (`/seller/payout-account`) | `/seller/…` |
| AI | `server-python/app/kyc/` | — |
| Quyết định thiết kế | `docs/adr/0011-*.md` (ADR 0011) | — |

Bảng CSDL chính: `vendor_profiles` (trạng thái, số CCCD, khóa tệp đã mã hóa…), `kyc_results` (tín hiệu, điểm, kết quả AI —
1 dòng / shop), `system_logs` (nhật ký).

---

## Phần 4. Vì sao máy dev mặc định KHÔNG chạy AI thật (chế độ `stub`)

Trong `application-local.yml`: `genie.ai-worker.mode: ${AI_WORKER_MODE:stub}`. Nghĩa là: không khai gì → dùng **stub**.

### 4.1. Stub là gì

**Stub = bản giả đóng thế.** `StubKycVerificationClient` đứng vào chỗ AI Worker, không đọc ảnh gì cả, chỉ nhìn
**2 số cuối số CCCD** để bịa tín hiệu, 2 giây sau tự áp kết quả **qua đúng `KycDecisionService` như AI thật**:

| 2 số cuối CCCD | Tín hiệu bịa | Điểm | Kết quả |
|---|---|---:|---|
| `00`–`29` | (không có) | 0 | VERIFIED *(5% vẫn bị rút sang hậu kiểm)* |
| `30`–`69` | `FACE_SCORE_BORDERLINE` + `UNREADABLE_IMAGE` | 55 | MANUAL_REVIEW |
| `70`–`99` | `QR_MRZ_MISMATCH` + `FACE_SCORE_LOW` | 120 | REJECTED |

Phần **duy nhất** bị giả là "AI đọc ảnh". Mọi thứ còn lại — mã hóa, trạng thái, chấm điểm, hàng chờ, email, nhật ký,
nhân viên duyệt — đều **chạy thật**.

### 4.2. Lý do chọn stub làm mặc định

| Lý do | Giải thích |
|---|---|
| **Nặng** | Worker cần Python **3.12** (máy bạn có 3.13 — không hợp), Tesseract OCR, OpenCV, ONNX Runtime + 2 mô hình khuôn mặt. Image Docker khoảng **1,7 GB**, build 5–10 phút, ăn thêm RAM — máy dev vốn đã chật (đã gặp hết RAM, đầy ổ C) |
| **Rắc rối mạng `localhost`** | Worker chạy trong Docker thì `localhost` là chính container → không tải được ảnh `localhost:9000`, không gọi lại được `localhost:8080`. Phải đổi sang `host.docker.internal` ở **3 chỗ** (`PUBLIC_S3_URL`, `BACKEND_URL`, `STORAGE_ALLOWED_HOSTS`) — sai một chỗ là hồ sơ rơi về duyệt tay mà không rõ vì sao |
| **Không có ảnh để test** | AI thật cần **ảnh CCCD thật, rõ nét** mới đọc được QR / MRZ. Không được đưa giấy tờ thật vào repo (dữ liệu cá nhân). Ảnh bất kỳ → AI luôn báo `UNREADABLE_IMAGE` → luôn MANUAL_REVIEW → không test được nhánh VERIFIED / REJECTED |
| **Kết quả phải đoán trước được** | Test nghiệp vụ cần biết chắc "nộp thế này thì ra thế kia". Stub cho chọn kết quả bằng 2 số cuối CCCD — test đủ 3 nhánh trong vài phút, lặp lại được |
| **Không phụ thuộc người khác** | Thành viên làm giao diện / backend không cần dựng AI mới chạy được phần mình. AI Worker có bộ test riêng (`pytest`) với **thẻ CCCD giả tự dựng** (`tests/fixtures/build_card.py`) |

### 4.3. Khi nào cần chạy AI thật

- Khi làm / sửa chính `server-python` (đọc QR, MRZ, khuôn mặt).
- Khi demo bảo vệ đề tài cần cho thấy AI thật đọc giấy tờ.
- Khi kiểm tra tích hợp thật: link 5 phút, callback, token nội bộ, timeout.

Cách bật: [huong-dan-chay-local.md](huong-dan-chay-local.md) mục **6.2** (build image, chạy container, đổi `.env` sang
`AI_WORKER_MODE=http`, khởi động lại backend). Chuyển về stub: mục **6.3**.

---

## Phần 5. Tự kiểm tra hiểu bài

1. Vì sao nộp KYC xong trang hiện "Đang xác minh" chứ không ra kết quả ngay? → *bất đồng bộ, AI xử lý nền rồi callback.*
2. Tải tệp trong bucket `kyc-dossiers` về sao không mở được? → *đã mã hóa AES-256-GCM, khóa chỉ backend giữ.*
3. AI có quyền từ chối hồ sơ không? → *không; AI chỉ trả tín hiệu, backend cộng điểm theo cấu hình rồi chốt.*
4. CCCD đuôi `45` ở chế độ stub ra gì? → *55 điểm → MANUAL_REVIEW.*
5. Người bán không tick đồng ý sinh trắc thì sao? → *không so khuôn mặt, luôn chuyển nhân viên duyệt.*
6. AI Worker bị tắt lúc nộp hồ sơ thì hồ sơ có kẹt mãi không? → *không; gọi lỗi chuyển duyệt tay ngay, quá 30 giây không gọi lại cũng chuyển.*
7. Nhân viên Tài chính (`linh.tm`) mở `/admin/kyc` được không? → *không — thiếu quyền `REVIEW_KYC` → 403.*
8. Moderator duyệt hồ sơ có đặt được hoa hồng 6% không? → *không — cần `UPDATE_COMMISSION` (chỉ Admin).*
9. Bị từ chối thì yêu cầu nhân viên xem lại được mấy lần? → *1 lần (lần 2 → `GNE-VND-4096`).*
10. Vì sao máy dev mặc định dùng stub? → *AI nặng, rắc rối mạng Docker, thiếu ảnh CCCD thật, cần kết quả đoán trước được.*

---

## Phụ lục — Từ điển nhanh

| Thuật ngữ | Nghĩa ngắn |
|---|---|
| KYC / eKYC | Xác minh danh tính khách hàng (bản điện tử) |
| CCCD | Căn cước công dân (thẻ gắn chip có QR + MRZ) |
| QR | Mã vuông mặt trước CCCD, chứa thông tin cá nhân |
| MRZ | 3 dòng chữ máy đọc ở mặt sau, có chữ số kiểm tra |
| OCR | Máy nhận dạng chữ trong ảnh (Tesseract đọc MRZ) |
| Liveness | Kiểm tra ảnh mặt là người thật, không phải ảnh chụp lại |
| Signal (tín hiệu) | Một dấu hiệu đáng ngờ do AI / backend phát hiện |
| Risk score | Tổng điểm rủi ro từ các tín hiệu |
| Stub | Bản giả đóng thế một dịch vụ thật để test |
| Callback | Bên kia làm xong thì gọi ngược lại báo kết quả |
| Presigned URL | Link có chữ ký, tự hết hạn (ở đây 5 phút) |
| AES-256-GCM | Thuật toán mã hóa đối xứng mạnh, có kiểm toàn vẹn |
| Escrow | Tiền người mua được sàn giữ tạm, chưa chuyển người bán |
| Transaction / commit | Nhóm thao tác CSDL "được cả hoặc không gì"; commit = chốt lưu |
| Idempotent | Gọi lặp lại nhiều lần kết quả vẫn như gọi 1 lần |
| SSRF | Kiểu tấn công lừa máy chủ tải dữ liệu từ địa chỉ nguy hiểm — AI chỉ tải ảnh từ host được phép |
| Token nội bộ (`X-Internal-Token`) | Mật khẩu chung giữa backend và AI Worker, người ngoài không gọi được `/internal/…` |
