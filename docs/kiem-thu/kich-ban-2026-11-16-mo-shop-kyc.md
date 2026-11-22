# Kịch bản kiểm thử — Plan 2026-11-16 · Phần 2: Mở shop & KYC (từ khách hàng → người bán → khách hàng)

> Phạm vi: **UC-VND-01** Mở shop · **UC-VND-02** Nộp hồ sơ KYC · **UC-VND-03** Thẩm định tự động (AI) · **UC-VND-04** Nhân viên duyệt
> KYC · **UC-VND-07** Tài khoản nhận tiền · **UC-VND-21** Cài đặt shop · và phần khách hàng thấy shop sau khi được duyệt.
> Chưa hiểu KYC là gì / dữ liệu đi đâu: đọc trước [../kyc-giai-thich-cho-nguoi-moi.md](../kyc-giai-thich-cho-nguoi-moi.md).
> Chức năng KYC **chưa làm** (không test ở đây): [kyc-viec-chua-lam.md](kyc-viec-chua-lam.md).
> Chức năng chung (nhật ký, banner, Điểm, yêu thích, theo dõi): [kich-ban-2026-11-16-chuc-nang-chung.md](kich-ban-2026-11-16-chuc-nang-chung.md).

---

## 0. Bức tranh luồng

```
 KHÁCH HÀNG                    NGƯỜI BÁN (cùng tài khoản)                                  NHÂN VIÊN SÀN              KHÁCH HÀNG KHÁC
 ──────────                    ─────────────────────────                                  ─────────────              ───────────────
 [A] Đăng ký tài khoản  ──►  [B] /become-seller ──► [C] /seller/kyc nộp hồ sơ ──► [D] AI (stub) chấm điểm
     (CUSTOMER, OTP)             OTP email TK          CCCD + 3 ảnh, mã hóa            │
                                 → VENDOR,              → PENDING → AI_PROCESSING        ├─ < 30  → VERIFIED ───────────────────────────────┐
                                   NOT_SUBMITTED                                         ├─ 30–69 → MANUAL_REVIEW ──► [E] /admin/kyc         │
                                                                                         └─ ≥ 70  → REJECTED ──► [F] xin xem xét (1 lần)     │
                                                                                                        Duyệt · Từ chối · Bổ sung · Đưa lại  │
                                                                                                                   │                          ▼
                               [H] tài khoản nhận tiền (OTP) ◄── [I] cài đặt shop ◄──────── VERIFIED ◄─────────────┘     [J] theo dõi shop mới
```

| Trạng thái `kyc_status` | Ai đưa tới | Người bán bán hàng được? |
|---|---|---|
| `NOT_SUBMITTED` | Mở shop xong | ❌ (Seller Center bị chặn, chỉ vào được `/seller/kyc`) |
| `PENDING` → `AI_PROCESSING` | Nộp hồ sơ | ❌ |
| `MANUAL_REVIEW` | AI 30–69 điểm / AI lỗi / không đồng ý sinh trắc / lấy mẫu 5% / người bán xin xem xét / nhân viên "đưa lại hàng chờ" | ❌ |
| `SUPPLEMENT_REQUESTED` | Nhân viên yêu cầu bổ sung (hạn 7 ngày) | ❌ |
| `REJECTED` | AI ≥ 70 / nhân viên từ chối / quá hạn bổ sung | ❌ |
| `VERIFIED` | AI < 30 / nhân viên duyệt | ✅ |

**Luật chuyển trạng thái quan trọng (theo code):**
- Người bán **sửa / nộp** được khi: `NOT_SUBMITTED`, `REJECTED`, `SUPPLEMENT_REQUESTED`. Trạng thái khác → **409 GNE-VND-4095**.
- Nhân viên **Duyệt / Từ chối** được khi: `AI_PROCESSING`, `MANUAL_REVIEW`.
- Nhân viên **Yêu cầu bổ sung** chỉ khi: `MANUAL_REVIEW`.
- Nhân viên **Đưa lại hàng chờ** khi: `VERIFIED`, `REJECTED`, `SUPPLEMENT_REQUESTED` → về `MANUAL_REVIEW`.
- Người bán **xin xem xét thủ công** chỉ khi `REJECTED`, **1 lần duy nhất** (lần 2 → **409 GNE-VND-4096**).

---

## 1. Chuẩn bị

### 1.1. Môi trường

- Docker (`postgres`, `redis`, `silo`, `mailpit` đang chạy) + backend + web — [../huong-dan-chay-local.md](../huong-dan-chay-local.md).
- `web/.env` có `vendor` trong `PUBLIC_REAL_API`.
- `.env` **không** đặt `AI_WORKER_MODE` (mặc định **stub**) — trừ giai đoạn K.
- Mở sẵn 4 tab: web (http://localhost:4321), **Mailpit** (http://localhost:8025), **Silo** (http://localhost:9001), **DataGrip / psql**.
- Nên dùng **2 trình duyệt** (hoặc 1 thường + 1 ẩn danh): một bên người bán, một bên nhân viên — khỏi đăng xuất / đăng nhập liên tục.
- Trang `/seller/kyc` **tự làm mới mỗi 3 giây** khi hồ sơ đang chờ AI (`PENDING` / `AI_PROCESSING`) — có kết quả là băng trạng thái + nút đổi ngay, không cần F5 (PR `bugfix/kyc-real-photo-pipeline`; trước đó phải F5). Khung Seller Center tự cập nhật mỗi 60 giây.

### 1.2. Tài khoản dùng trong kịch bản

Mỗi vòng test dùng **tài khoản mới** (không cần reset dữ liệu). Đăng ký ở `/auth/register`, mật khẩu đặt `Genie@123` cho dễ nhớ, OTP xem Mailpit.

| Tài khoản | Vai trò trong kịch bản | CCCD dùng (stub theo **2 số cuối**) |
|---|---|---|
| `seller1@genie.test` | Luồng duyệt tay đầy đủ: bổ sung → nộp lại → duyệt | `079203000145` (đuôi 45 → 55 điểm → MANUAL_REVIEW) |
| `seller2@genie.test` | Tự duyệt (HOUSEHOLD, có MST) | `079203000112` (đuôi 12 → 0 điểm → VERIFIED) |
| `seller3@genie.test` | Tự từ chối → xin xem xét → từ chối lần 2 | `079203000188` (đuôi 88 → 120 điểm → REJECTED) |
| `seller4@genie.test` | Trùng CCCD với shop đã VERIFIED | `079203000112` (trùng seller2) |
| `seller5@genie.test` | Không đồng ý sinh trắc | `079203000120` (đuôi 20) |
| `buyer1@genie.test` | Khách hàng cuối luồng (theo dõi shop) | — |
| `phat.nt@genie.example` | Moderator — duyệt KYC, **không** sửa hoa hồng | — |
| `yen.dh@genie.example` | Admin — duyệt KYC + hoa hồng riêng + nhãn Chính hãng | — |
| `linh.tm@genie.example`, `truc.ht@genie.example` | Tài chính, CSKH — **không** có quyền KYC | — |

Seed sẵn có 5 hồ sơ ở hàng chờ: *Nhà Sách Mộc*, *Gốm Bát Tràng Xưa*, *Mẹ & Bé Happy* (MANUAL_REVIEW), *TravelGear Outdoor* (SUPPLEMENT_REQUESTED), *Cheap Electronics* (REJECTED) — chủ là `kyc.<tên>@genie.example`.

### 1.3. Ảnh giấy tờ

| Loại | Bắt buộc khi | Yêu cầu |
|---|---|---|
| `ID_FRONT` — CCCD mặt trước | luôn | JPEG / PNG / PDF, **≥ 20 KB**, **≤ 10 MB** |
| `ID_BACK` — CCCD mặt sau | luôn | như trên |
| `SELFIE` — ảnh chân dung | luôn | như trên |
| `BUSINESS_LICENSE` — giấy phép kinh doanh | `HOUSEHOLD`, `BUSINESS` | như trên |

Chế độ stub **không đọc ảnh** → dùng ảnh bất kỳ đủ dung lượng (ảnh phong cảnh 100–500 KB là được). Chuẩn bị thêm 1 ảnh **< 20 KB** và 1 tệp **> 10 MB** / `.gif` để thử lỗi. **Không** dùng giấy tờ thật của người khác.

### 1.4. Bảng điểm stub (nhắc lại)

| 2 số cuối CCCD | Tín hiệu giả | Điểm | Kết quả |
|---|---|---:|---|
| `00`–`29` | — | 0 | VERIFIED *(5% bị rút ngẫu nhiên sang MANUAL_REVIEW để hậu kiểm — nếu gặp thì không phải lỗi, xem `kyc_results.sampled_for_review`)* |
| `30`–`69` | `FACE_SCORE_BORDERLINE` + `UNREADABLE_IMAGE` | 55 | MANUAL_REVIEW |
| `70`–`99` | `QR_MRZ_MISMATCH` + `FACE_SCORE_LOW` | 120 | REJECTED |

Backend cộng thêm tín hiệu của nó: `DUPLICATE_ID` (+50) nếu CCCD đã gắn shop khác đang VERIFIED.

### 1.5. SQL kiểm tra dùng xuyên suốt

```sql
-- trạng thái hồ sơ theo email chủ shop
SELECT v.id AS shop_id, v.shop_name, u.email, u.role, v.business_type, v.kyc_status, v.kyc_verification_method,
       v.kyc_rejection_reason, v.kyc_supplement_documents_json, v.kyc_supplement_deadline_at,
       v.kyc_manual_review_requested_at, v.kyc_biometric_consent, v.commission_rate, v.is_mall, v.kyc_documents_meta_json
FROM vendor_profiles v JOIN users u ON u.id = v.user_id
WHERE u.email LIKE 'seller%@genie.test' ORDER BY v.id;

-- kết quả chấm điểm (1 dòng / shop)
SELECT vendor_profile_id, risk_score, risk_signals_json, sampled_for_review, face_match_result, duplicate_id_found
FROM kyc_results WHERE vendor_profile_id = <shop_id>;

-- nhật ký liên quan
SELECT created_at AT TIME ZONE 'Asia/Ho_Chi_Minh' AS luc, user_email, action_type, reference_id, details
FROM system_logs WHERE action_type LIKE 'KYC_%' OR action_type IN ('COMMISSION_UPDATED','PAYOUT_ACCOUNT_CHANGED')
ORDER BY created_at DESC LIMIT 20;
```

---

## A. Khách hàng đăng ký tài khoản (tiền đề)

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-A-01 | Cửa sổ ẩn danh → `/auth/register` → nhập `seller1@genie.test`, họ tên, SĐT `0901234567`, mật khẩu `Genie@123` → Gửi | Chuyển màn nhập OTP; Mailpit có thư OTP tới `seller1@genie.test` |
| TC-A-02 | Nhập OTP đúng | Đăng nhập luôn, về trang chủ; `SELECT role FROM users WHERE email='seller1@genie.test'` → `CUSTOMER` |
| TC-A-03 | Lặp lại cho `seller2` … `seller5`, `buyer1` | Mỗi tài khoản là `CUSTOMER` |

---

## B. UC-VND-01 — Mở shop (`/become-seller`)

Đăng nhập `seller1@genie.test`. Form gồm: **Tên shop** (3–100 ký tự), **Tên chủ shop**, **SĐT** (`0` + 9–10 số), **Email** (= email tài khoản, ô xám chỉ đọc — OTP mở shop gửi về đây), **Ngành chính**, **Loại hình** (`INDIVIDUAL` cá nhân / `HOUSEHOLD` hộ kinh doanh / `BUSINESS` doanh nghiệp), **Đồng ý điều khoản**.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-B-01 | Chưa đăng nhập, vào `/become-seller` | Yêu cầu đăng nhập |
| TC-B-02 | Đăng nhập `seller1`, vào `/become-seller` | Form đủ trường trên |
| TC-B-03 | Để trống tên shop / tên 2 ký tự / SĐT `12345` / không tick điều khoản → Gửi | Báo lỗi đúng từng trường, không gửi OTP (API: **422 GNE-VAL-4220** kèm `errors[]`) |
| TC-B-04 | Tên shop `TechZone Official` (đã có) | **409 GNE-VND-4094** "Tên shop đã được dùng." |
| TC-B-05 | Tên `Shop Thử Nghiệm 1`, loại hình `INDIVIDUAL`; ô **Email** đã điền sẵn `seller1@genie.test`, xám, **không sửa được** → Gửi | Báo đã gửi OTP; Mailpit có thư OTP gửi tới **email tài khoản** `seller1@genie.test`. Gọi API kèm email khác (Swagger) → vẫn gửi về email tài khoản, `contact_email` = email tài khoản |
| TC-B-06 | Nhập OTP sai | Báo sai, còn N lần thử; sai đủ 5 lần thì phải gửi lại mã |
| TC-B-07 | Nhập OTP đúng | Tạo shop, chuyển vào Seller Center; banner nhắc **nộp hồ sơ xác minh**. DB: `users.role = 'VENDOR'`, `vendor_profiles.kyc_status = 'NOT_SUBMITTED'` |
| TC-B-08 | Vẫn phiên đăng nhập cũ, F5 | Không bị đăng xuất (giữ phiên khi nâng lên VENDOR) |
| TC-B-09 | Quay lại `/become-seller`, đăng ký shop thứ 2 | **409 GNE-VND-4093** "Tài khoản đã có shop." |
| TC-B-10 | Làm B-05 → B-07 cho `seller2` (`HOUSEHOLD`), `seller3`, `seller4`, `seller5` (`INDIVIDUAL`), tên shop khác nhau | Mỗi người có 1 shop `NOT_SUBMITTED` |

---

## C. UC-VND-02 — Nộp hồ sơ KYC (`/seller/kyc`)

Form gồm: **Số CCCD** (12 số), **Họ tên người đại diện**, **Loại hình**, **Mã số thuế** (hộ / doanh nghiệp), 3–4 ô tải ảnh, ô **Đồng ý xử lý dữ liệu sinh trắc học**, nút **Lưu nháp** và **Gửi duyệt**.

### C.1. Cổng KYC chặn Seller Center

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-C-01 | `seller1` (NOT_SUBMITTED) bấm menu Sản phẩm / Đơn hàng / Ví… | `KycGate` chặn, dẫn về `/seller/kyc` |
| TC-C-02 | Gọi thẳng API chỉ dành cho shop đã xác minh (vd `GET /api/v1/seller/payout-account` trong Swagger với token seller1) | **403 GNE-VND-4031** "Shop chưa được xác minh (KYC)." (`/seller/kyc`, `/seller/me`, `/seller/settings` vẫn mở để người bán nộp hồ sơ) |

### C.2. Tải ảnh

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-C-03 | Tải ảnh **< 20 KB** vào ô CCCD mặt trước | Bị chặn: ảnh quá nhỏ (không đủ để đọc QR / MRZ) |
| TC-C-04 | Tải tệp **> 10 MB** hoặc `.gif` | Bị chặn: dung lượng / định dạng |
| TC-C-05 | Tải ảnh hợp lệ cho đủ 3 ô | Hiện tên tệp / xem trước; Network thấy **PUT thẳng lên `localhost:9000`** (Silo), không qua backend |
| TC-C-06 | Silo console → bucket `kyc-dossiers` → thư mục `tmp/` | Có tệp tạm vừa tải (chưa mã hóa — sẽ bị xóa khi nộp) |

### C.3. Lưu nháp

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-C-07 | Chỉ nhập họ tên đại diện → **Lưu nháp** | Lưu được (nháp cho phép thiếu trường); F5 vẫn còn |
| TC-C-08 | Lưu nháp với CCCD `0792` (chưa đủ) rồi với `07a2` (có chữ) | `0792` **lưu được** (nháp cho phép nhập dở, tối đa 12 số); `07a2` → **422** "Số CCCD gồm 12 chữ số." |
| TC-C-09 | `GET /api/v1/seller/kyc` (Swagger) | Trả dữ liệu đã lưu, **không có URL ảnh** (chỉ tên tệp) |

### C.4. Kiểm lỗi khi gửi duyệt

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-C-10 | CCCD 11 số / có chữ → Gửi duyệt | **422** "Số CCCD gồm đúng 12 chữ số." (trường `identity_number`) |
| TC-C-11 | Bỏ trống họ tên đại diện | **422** "Nhập họ tên người đại diện." |
| TC-C-12 | Bỏ trống ảnh SELFIE | **422 GNE-VND-4223** "Hồ sơ KYC còn thiếu giấy tờ." — chi tiết `missing: [SELFIE]` |
| TC-C-13 | (seller2, `HOUSEHOLD`) không nhập mã số thuế | **422** "Mã số thuế gồm 10 hoặc 13 chữ số (dạng 0312345678-001)." |
| TC-C-14 | (seller2) mã số thuế `12345` | Như trên |
| TC-C-15 | (seller2) MST hợp lệ `0312345678` nhưng thiếu giấy phép kinh doanh | **422 GNE-VND-4223**, `missing: [BUSINESS_LICENSE]` |

### C.5. Gửi duyệt thành công (seller1)

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-C-16 | `seller1`: CCCD `079203000145`, họ tên `Nguyễn Văn Một`, `INDIVIDUAL`, đủ 3 ảnh, **tick đồng ý sinh trắc** → Gửi duyệt | Báo đã gửi, trạng thái **Đang xác minh** |
| TC-C-17 | Ngay lập tức chạy SQL 1.5 | `kyc_status` = `PENDING` hoặc `AI_PROCESSING`; `kyc_biometric_consent = true` |
| TC-C-18 | Silo → `kyc-dossiers/vendor/<shop_id>/` | Có 3 tệp **`.enc`**; tệp trong `tmp/` của lần tải đã **biến mất** |
| TC-C-19 | Tải một tệp `.enc` về, đổi đuôi `.jpg`, mở | **Không mở được** (đã mã hóa AES-256-GCM) |
| TC-C-20 | Trong lúc đang chờ, sửa và Gửi duyệt lần nữa | **409 GNE-VND-4095** "Hồ sơ KYC đang được xử lý hoặc đã được duyệt." |
| TC-C-21 | Nhật ký (SQL 1.5 hoặc `/admin/audit-logs` nhóm KYC) | Có `KYC_SUBMITTED` của seller1 |

---

## D. UC-VND-03 — Thẩm định tự động (chế độ stub)

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-D-01 | (tiếp C-16) đợi ~3 giây trên `/seller/kyc` (tự làm mới) | **Đang chờ nhân viên duyệt** (`MANUAL_REVIEW`). `kyc_results`: `risk_score = 55`, tín hiệu `FACE_SCORE_BORDERLINE`, `UNREADABLE_IMAGE` |
| TC-D-02 | `seller2` (`HOUSEHOLD`, MST `0312345678`, đủ 4 ảnh, CCCD `079203000112`, đồng ý sinh trắc) → Gửi → đợi vài giây | **VERIFIED** tự động; `kyc_verification_method = 'OCR_AUTO'`; Seller Center mở khóa; `risk_score = 0`. *(5% khả năng vào MANUAL_REVIEW vì lấy mẫu — xem `sampled_for_review`)* |
| TC-D-03 | Mailpit | seller2 nhận thư **KYC được duyệt** |
| TC-D-04 | `seller3` (CCCD `079203000188`) → Gửi → đợi vài giây (không F5) | **REJECTED**, có lý do; `risk_score = 120`; nút **Yêu cầu nhân viên xem xét**; Mailpit có thư **bị từ chối** |
| TC-D-05 | `seller4` (CCCD `079203000112` — trùng seller2 đã VERIFIED) → Gửi | Không tự duyệt: tín hiệu **`DUPLICATE_ID`** (+50) → **MANUAL_REVIEW**; `duplicate_id_found = true` |
| TC-D-06 | `seller5` (CCCD `079203000120`), **không tick** đồng ý sinh trắc → Gửi | Dù đuôi 20 (0 điểm) vẫn **MANUAL_REVIEW** (không đồng ý sinh trắc → luôn duyệt tay); `kyc_biometric_consent = false` |
| TC-D-07 | Nhật ký nhóm KYC | Có `KYC_APPROVED` (seller2, người làm = hệ thống) và `KYC_REJECTED` (seller3) |

---

## E. UC-VND-04 — Nhân viên duyệt KYC (`/admin/kyc`)

### E.1. Quyền

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-E-01 | `linh.tm` (Tài chính) và `truc.ht` (CSKH) vào `/admin/kyc` | Không có menu; URL trực tiếp → không có quyền; API **403**; nhật ký có `ACCESS_DENIED` |
| TC-E-02 | `phat.nt` (Moderator) vào `/admin/kyc` | Vào được |

### E.2. Hàng chờ & xem hồ sơ (Moderator)

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-E-03 | Xem danh sách | Có seller1, seller4, seller5 (MANUAL_REVIEW) + hồ sơ seed; **rủi ro cao xếp trước**; lọc theo trạng thái (MANUAL_REVIEW / SUPPLEMENT_REQUESTED / REJECTED…) |
| TC-E-04 | Mở hồ sơ seller1 | Thấy: thông tin khai báo, loại hình, **điểm rủi ro 55**, danh sách tín hiệu, ảnh CCCD 2 mặt + selfie |
| TC-E-05 | Chuột phải ảnh → mở trong tab mới, xem URL | URL dạng `…/kyc-dossiers/tmp/view/…?X-Amz-…&X-Amz-Expires=300` (link **5 phút**) |
| TC-E-06 | Đợi > 5 phút, tải lại đúng URL ảnh đó | Lỗi hết hạn (`AccessDenied` / `Request has expired`); mở lại hồ sơ thì có link mới |
| TC-E-07 | Nhật ký | Mỗi lần mở hồ sơ thêm 1 dòng **`KYC_DOCUMENT_VIEWED`** (người xem = phat.nt) |

### E.3. Yêu cầu bổ sung → người bán nộp lại

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-E-08 | seller1 → **Yêu cầu bổ sung**, không chọn giấy tờ nào | Báo phải chọn ≥ 1 giấy tờ (API 422, trường `documents`) |
| TC-E-09 | Chọn **SELFIE**, ghi chú "Ảnh chân dung bị lóa" → Gửi | Trạng thái **SUPPLEMENT_REQUESTED**; `kyc_supplement_deadline_at` = bây giờ + 7 ngày; nhật ký `KYC_SUPPLEMENT_REQUESTED`; Mailpit: thư yêu cầu bổ sung tới seller1 |
| TC-E-10 | Thử Yêu cầu bổ sung cho hồ sơ **seller2 đã VERIFIED** (Swagger `POST /admin/kyc-submissions/{id}/request-supplement`, body `{"documents":["SELFIE"]}`) | Bị từ chối — chỉ yêu cầu bổ sung khi hồ sơ đang `MANUAL_REVIEW`. Giấy tờ được chọn: `ID_FRONT`, `ID_BACK`, `SELFIE`, `BUSINESS_LICENSE`, `BUSINESS_REGISTRATION`, `TAX_NOTICE` |
| TC-E-11 | seller1 F5 `/seller/kyc` | Thấy danh sách giấy tờ cần bổ sung (SELFIE), ghi chú, **hạn 7 ngày**; các trường khác vẫn còn |
| TC-E-12 | seller1 tải selfie mới → Gửi duyệt (giữ CCCD đuôi 45) | Về `PENDING` → `AI_PROCESSING` → (2 giây) **MANUAL_REVIEW** |

### E.4. Từ chối

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-E-13 | Hồ sơ seed *Gốm Bát Tràng Xưa* → **Từ chối**, lý do **Khác (OTHER)**, để trống ghi chú | **422** "Nhập ghi chú cho lý do khác." |
| TC-E-14 | Lý do **Ảnh giấy tờ mờ, không đọc được** (`BLURRY_DOCUMENT`) → Từ chối | **REJECTED**; `kyc_rejection_reason` có câu lý do; nhật ký `KYC_REJECTED`; email từ chối tới `kyc.gombattrang@genie.example` |
| TC-E-15 | Thử lần lượt các lý do khác trên hồ sơ khác (nếu còn): `INFO_MISMATCH`, `EXPIRED_DOCUMENT`, `SUSPECTED_FORGERY`, `MISSING_LICENSE` | Lý do hiển thị đúng câu tiếng Việt ở phía người bán |

### E.5. Duyệt & hoa hồng

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-E-16 | Moderator duyệt seller1 **kèm hoa hồng riêng 6%** | **403** "Cần quyền UPDATE_COMMISSION để đặt hoa hồng riêng." (Moderator không có quyền này) |
| TC-E-17 | Đăng nhập Admin `yen.dh` → seller1 → Duyệt, hoa hồng **60%** | **422** "Hoa hồng riêng phải trong khoảng 0–50%." |
| TC-E-18 | Admin duyệt seller1, hoa hồng **6%**, bật **Chính hãng** | **VERIFIED** (`MANUAL_STAFF`), `commission_rate = 0.0600`, `is_mall = true`; nhật ký `KYC_APPROVED` + `COMMISSION_UPDATED`; Mailpit thư duyệt tới seller1 |
| TC-E-19 | seller1 F5 | Seller Center **mở khóa toàn bộ**; khung hiện trạng thái đã xác minh, nhãn Chính hãng |
| TC-E-20 | Moderator duyệt seller5 **không** đặt hoa hồng | Duyệt được (hoa hồng giữ mặc định theo ngành) |

### E.6. Đưa lại hàng chờ (requeue)

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-E-21 | Admin → hồ sơ seed *Cheap Electronics* (REJECTED) → **Đưa lại hàng chờ** | Về **MANUAL_REVIEW**, xóa lý do từ chối cũ; nhật ký `KYC_REQUEUED` |
| TC-E-22 | Đưa lại hàng chờ hồ sơ đang MANUAL_REVIEW (vd seller4) | Bị từ chối — hồ sơ đang ở hàng chờ |
| TC-E-23 | Duyệt seller4 (trùng CCCD) **hoặc** từ chối lý do `INFO_MISMATCH` | Nhân viên quyết; kiểm điểm rủi ro hiển thị có `DUPLICATE_ID` để làm căn cứ |

---

## F. Sau khi bị từ chối — người bán xin xem xét / nộp lại

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-F-01 | `seller3` (REJECTED ở D-04) bấm **Yêu cầu nhân viên xem xét** | Về **MANUAL_REVIEW**; `kyc_manual_review_requested_at` có giá trị; hồ sơ xuất hiện ở `/admin/kyc` |
| TC-F-02 | Moderator **Từ chối** seller3 lần nữa (lý do `SUSPECTED_FORGERY`) | REJECTED; email từ chối **không còn** nút xin xem xét |
| TC-F-03 | seller3 bấm xin xem xét lần 2 (hoặc `POST /seller/kyc/manual-review-requests`) | **409 GNE-VND-4096** "Bạn đã gửi yêu cầu xem xét hồ sơ KYC." |
| TC-F-04 | seller3 sửa hồ sơ: đổi CCCD thành `079203000133` (đuôi 33) → Gửi duyệt | Được nộp lại (REJECTED cho sửa) → AI → **MANUAL_REVIEW** (55 điểm) |
| TC-F-05 | seller (đang REJECTED do AI, **chưa** xin xem xét) → xin xem xét | Vào MANUAL_REVIEW; lần AI chấm sau dù ≥ 70 điểm cũng **vào MANUAL_REVIEW** chứ không tự từ chối (đã từng xin xem xét) |

---

## G. Hết hạn bổ sung 7 ngày (job 01:00)

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-G-01 | Dùng hồ sơ seed *TravelGear Outdoor* (SUPPLEMENT_REQUESTED). Đăng nhập `kyc.travelgear@genie.example` → `/seller/kyc` | Thấy yêu cầu bổ sung "Ảnh mặt sau CCCD bị lóa…" + hạn |
| TC-G-02 | SQL đẩy hạn về quá khứ (bên dưới), để backend chạy qua **01:00** sáng | Sáng hôm sau: **REJECTED** "Quá hạn bổ sung hồ sơ" + email. Kiểm nhanh không cần chờ: `mvnw verify -Dit.test=KycReviewIT` (ca `supplementExpiresAfterSevenDaysAndRequeue`) |

```sql
UPDATE vendor_profiles SET kyc_supplement_deadline_at = now() - interval '1 hour'
WHERE id = (SELECT v.id FROM vendor_profiles v JOIN users u ON u.id = v.user_id WHERE u.email = 'kyc.travelgear@genie.example');
```

---

## H. UC-VND-07 — Tài khoản nhận tiền (Seller Center → Cài đặt → Tài khoản nhận tiền)

Đăng nhập `seller1` (đã VERIFIED, họ tên KYC "Nguyễn Văn Một"). Ngân hàng **giả lập**: tên chủ tài khoản trả về = tên bạn nhập (bỏ dấu, viết hoa), riêng số tài khoản **kết thúc `000`** trả tên khác.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-H-01 | Mở màn tài khoản nhận tiền | Chưa có tài khoản; danh sách ngân hàng tải từ `GET /public/banks` |
| TC-H-02 | Số tài khoản `12ab` / `123` (dưới 6 số) | Báo số tài khoản 6–20 chữ số |
| TC-H-03 | Ngân hàng bất kỳ, số `1234567000`, chủ TK `Nguyen Van Mot` → Kiểm tra | **422 GNE-WAL-4222** "Tên chủ tài khoản không khớp với CCCD." (ngân hàng giả trả `NGUYEN VAN KHAC`) |
| TC-H-04 | Số `1234567890`, chủ TK `Tran Thi B` (khác tên KYC) | **422 GNE-WAL-4222** |
| TC-H-05 | Số `1234567890`, chủ TK `Nguyễn Văn Một` → Lưu | Yêu cầu **OTP bước 2**; Mailpit có OTP mục đích `PAYOUT_ACCOUNT_UPDATE` |
| TC-H-06 | Nhập OTP sai | Không lưu, báo sai |
| TC-H-07 | Nhập OTP đúng | Lưu thành công; số tài khoản hiển thị **đã che** (chỉ lộ vài số cuối); nhật ký `PAYOUT_ACCOUNT_CHANGED` |
| TC-H-08 | Xóa tài khoản nhận tiền | Yêu cầu OTP `PAYOUT_ACCOUNT_DELETE` → nhập đúng → xóa; nhật ký `PAYOUT_ACCOUNT_CHANGED` |
| TC-H-09 | `seller3` (REJECTED) gọi `GET /seller/payout-account` | **403 GNE-VND-4031** |

---

## I. UC-VND-21 — Cài đặt shop (`/seller/settings`)

Đăng nhập `seller1`.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-I-01 | Mở `/seller/settings` | Tải đủ: thông tin shop, logo, ảnh bìa, mô tả, hãng vận chuyển, tự trả lời |
| TC-I-02 | Đổi logo (JPEG/PNG ≤ 1 MB), ảnh bìa (≤ 2 MB), mô tả → Lưu | Lưu; F5 vẫn còn; ảnh nằm ở bucket công khai `avatar-assets` |
| TC-I-03 | Logo > 1 MB / ảnh bìa > 2 MB | Bị chặn dung lượng |
| TC-I-04 | Đổi tên shop thành `Shop Thử Nghiệm Một` | Lưu được |
| TC-I-05 | Đổi tên lần 2 ngay | **409 GNE-VND-4091** "Chỉ được đổi tên shop 1 lần trong 30 ngày." |
| TC-I-06 | Đổi tên trùng shop khác (`Beauty Box`) | **409 GNE-VND-4094** |
| TC-I-07 | Tắt **tất cả** hãng vận chuyển → Lưu | **422 GNE-VND-4221** "Phải bật ít nhất 1 hãng vận chuyển." |
| TC-I-08 | Bật tự trả lời, nội dung trống | **422 GNE-VND-4222** |
| TC-I-09 | Bật tự trả lời có nội dung | Lưu được |

> Không test ở đây: **kho lấy hàng GHN (UC-VND-05)** và **chế độ tạm nghỉ** — backend chưa có API, màn vẫn chạy mock ([kyc-viec-chua-lam.md](kyc-viec-chua-lam.md)).

---

## J. Phía khách hàng — shop vừa được duyệt

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-J-01 | **Trước** khi seller1 được duyệt (làm trước E-18): `buyer1` gọi `PUT /me/following/<shop_id seller1>` | **404** — shop chưa xác minh không theo dõi được |
| TC-J-02 | **Sau** E-18: `buyer1` theo dõi shop seller1 | `204`; `follower_count` +1; `/account/following` có shop **Shop Thử Nghiệm Một**, nhãn Chính hãng, logo mới |
| TC-J-03 | seller1 (chủ shop) tự theo dõi shop mình | **409 GNE-SOC-4095** |
| TC-J-04 | Admin xem `GET /admin/shops` (Swagger, hoặc ô chọn shop khi tạo banner liên kết tới shop) | Shop seller1 có trong danh sách shop đã xác minh |
| TC-J-05 | Admin `/admin/audit-logs` nhóm KYC, tìm `Shop Thử Nghiệm` | Đủ chuỗi: `KYC_SUBMITTED` → `KYC_DOCUMENT_VIEWED` → `KYC_SUPPLEMENT_REQUESTED` → `KYC_SUBMITTED` → `KYC_APPROVED` (+ `COMMISSION_UPDATED`) |

> Trang shop công khai `/shop/{id}` và sản phẩm storefront **vẫn chạy mock** (chưa có API catalog) — nên phần khách hàng kiểm qua theo dõi shop / API.

---

## K. (Tùy chọn) AI Worker thật — chế độ `http`

Bật theo [../huong-dan-chay-local.md](../huong-dan-chay-local.md) mục **6.2** (build image, chạy container, `.env` → `AI_WORKER_MODE=http`, restart backend). Dùng tài khoản người bán mới.

| Mã | Bước làm | Kết quả mong đợi |
|---|---|---|
| TC-K-01 | http://localhost:8000/health | `status: UP`, `models_loaded: true`, có `pipeline_version` |
| TC-K-02 | `POST http://localhost:8000/kyc/verify` **không** header `X-Internal-Token` | 401 / 403 |
| TC-K-03 | Nộp KYC bằng ảnh **không phải CCCD** (phong cảnh) | Log worker nhận yêu cầu, gọi lại backend; tín hiệu **`UNREADABLE_IMAGE`** → **MANUAL_REVIEW**; không kẹt `AI_PROCESSING` |
| TC-K-04 | Nộp KYC bằng CCCD **của chính bạn** + selfie của bạn (rõ nét, cạnh ngắn ≥ 600 px) | Admin thấy cờ QR / MRZ / khớp mặt, điểm khớp mặt; `kyc_results.extracted_*` có dữ liệu đọc từ thẻ. Khai họ tên / số khác thẻ → thêm `DECLARED_DATA_MISMATCH` |
| TC-K-05 | Selfie người khác | `FACE_SCORE_LOW` / `face_match_result = false`, điểm cao |
| TC-K-06 | **Tắt** container worker rồi nộp KYC | Không lỗi 500; hồ sơ chuyển **MANUAL_REVIEW** ngay (gọi lỗi) — `kyc_results.errors` ghi lỗi |
| TC-K-07 | Worker chạy nhưng `INTERNAL_TOKEN` hai bên khác nhau | Backend gọi worker bị 401 → hồ sơ chuyển **MANUAL_REVIEW** ngay. (Nếu worker nhận được nhưng không gọi lại kịp → sau **30 giây** `KycAiTimeoutJob` chuyển MANUAL_REVIEW) |

> Xong nhớ xóa ảnh giấy tờ thật khỏi máy và đưa `.env` về stub (mục 6.3).

---

## L. Bảng tổng hợp

| Giai đoạn | Use case | Mã | Kết quả | Ghi chú |
|---|---|---|---|---|
| A | (AUTH) Đăng ký khách | TC-A-01 → 03 | ☐ | |
| B | UC-VND-01 Mở shop | TC-B-01 → 10 | ☐ | |
| C | UC-VND-02 Nộp hồ sơ | TC-C-01 → 21 | ☐ | |
| D | UC-VND-03 Thẩm định tự động (stub) | TC-D-01 → 07 | ☐ | |
| E | UC-VND-04 Nhân viên duyệt | TC-E-01 → 23 | ☐ | |
| F | UC-VND-02/04 Xin xem xét / nộp lại | TC-F-01 → 05 | ☐ | |
| G | UC-VND-04 Hết hạn bổ sung | TC-G-01 → 02 | ☐ | |
| H | UC-VND-07 Tài khoản nhận tiền | TC-H-01 → 09 | ☐ | |
| I | UC-VND-21 Cài đặt shop | TC-I-01 → 09 | ☐ | |
| J | Phía khách hàng | TC-J-01 → 05 | ☐ | |
| K | UC-VND-03 AI thật (tùy chọn) | TC-K-01 → 07 | ☐ | |

Đạt đủ trên giao diện → cập nhật cột **Chạy** của UC tương ứng trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`.
