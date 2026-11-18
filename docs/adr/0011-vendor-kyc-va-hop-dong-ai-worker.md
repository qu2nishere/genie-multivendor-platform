# 0011 — Mở shop & KYC người bán: mã hóa hồ sơ, hợp đồng Backend ↔ AI Worker

- **Trạng thái:** Chấp nhận (plan `plan/2026-11-16/server-spring/plan_mo_shop_kyc.md` D1–D15;
  `plan/2026-11-16/server-python/plan_kyc_ai_worker.md` Y-D1)
- **Phạm vi:** `server-spring/` — `vendor`, `common` (crypto, storage, ai), `auth` (OTP); `server-python/` — `kyc`

## Bối cảnh

Tài liệu nguồn lệch nhau ở chỗ quan trọng: implement 8.1 dùng camelCase và để AI Worker tự tính `riskScore`, còn
API_SPEC 12 dùng `overall`, `checks{5}`, `face_match_score`; UC-VND-03 lại yêu cầu "cộng điểm rủi ro thay vì chặn
cứng" với ngưỡng và bảng điểm cấu hình được (ENTITY 1.6). Ảnh CCCD / selfie là dữ liệu cá nhân đặc biệt nhạy cảm
(LUU_TRU 5.6–5.7, NFR-07) nhưng Presigned GET của ảnh đã mã hóa thì không xem được. `vendor_profiles` cũng thiếu cột
cho các màn hình Mở shop / KYC / Cài đặt.

## Quyết định

1. **`shop_id` trên API = `vendor_profiles.id`.** Bảng nghiệp vụ khác vẫn lưu `vendor_id = users.id`; đổi qua
   `ShopFacade` (module `vendor`).
2. **Schema:** `V17__vendor_kyc_additions.sql` (số theo sổ đăng ký README plan 2026-11-16) chỉ **thêm** 9 cột
   (`contact_phone`, `contact_email`, `pickup_phone`, `kyc_biometric_consent`, `kyc_documents_meta_json`,
   `kyc_supplement_documents_json`, `kyc_supplement_deadline_at`, `kyc_manual_review_requested_at`,
   `shop_name_changed_at`), 3 index và 4 `AuditAction` (`KYC_SUPPLEMENT_REQUESTED`, `KYC_REQUEUED`,
   `KYC_DOCUMENT_VIEWED`, `PAYOUT_ACCOUNT_CHANGED`).
3. **Tải tệp theo API 4.1:** một purpose `KYC_DOCUMENT` (bucket riêng tư `kyc-dossiers`, pdf / jpeg / png, ≤ 10 MB, ảnh
   ≥ 20 KB, link 10 phút, chỉ VENDOR); `SHOP_LOGO` (≤ 1 MB) / `SHOP_COVER` (≤ 2 MB) ở `avatar-assets`.
4. **Mã hóa tầng ứng dụng AES-256-GCM** (`common.crypto.AesGcmCipher`, khóa `KYC_AES_KEY` Base64 32 byte): khi nộp hồ sơ
   backend đọc tệp `tmp/`, mã hóa (IV 12 byte ngẫu nhiên đặt đầu tệp, AAD = `vendorProfileId:docType`), ghi
   `kyc-dossiers/vendor/{vendorProfileId}/{front|back|selfie|license}-{uuid}.enc`, xóa bản `tmp/`. Người bán **không bao
   giờ** nhận lại URL ảnh. Nhân viên / AI Worker xem qua **bản giải mã tạm** `kyc-dossiers/tmp/view/{uuid}.{ext}` +
   Presigned GET 5 phút; job dọn bản tạm sau 10 phút (lifecycle `tmp/` 1 ngày là lớp dự phòng). Mỗi lần nhân viên mở
   hồ sơ ghi `KYC_DOCUMENT_VIEWED`.
5. **Một hợp đồng snake_case giữa Backend và AI Worker; AI Worker không kết luận.**
   - Yêu cầu `POST {AI_WORKER_URL}/kyc/verify` (header `X-Internal-Token`):
     `{kyc_id, documents{id_front, id_back, selfie}, declared{identity_number, full_name}, biometric_consent,
     callback_url}` — `documents.*` là URL bản xem tạm 5 phút → `202 {kyc_id, accepted: true}`.
   - Callback `PATCH /api/v1/internal/kyc/{kyc_id}/result` (header `X-Internal-Token`):
     `{checks{qr_code, mrz, mrz_qr_match, cccd_number_valid, expiry_rule_valid, face_match, face_duplicate,
     national_emblem, fingerprint}` (mỗi giá trị `true / false / null`)`, face_match_score, liveness_score,
     signals[{code}], extracted{identity_number, full_name, date_of_birth, gender, address, issue_date, expiry_date,
     legacy_identity_number}, errors[], model{yunet, sface, pipeline_version}}` → `200 {kyc_status}`.
   - **Backend** quy đổi tín hiệu → điểm theo bảng cấu hình `genie.kyc.risk.points.*` (11 mã của ENTITY 1.6), tự thêm
     `DUPLICATE_ID` / `DECLARED_DATA_MISMATCH`, lấy mẫu 5 %, chốt `PASS` (< 30) / `PARTIAL` (30–69 hoặc bị lấy mẫu) /
     `FAIL` (≥ 70). Callback idempotent: hồ sơ không còn `AI_PROCESSING` thì bỏ qua.
   - Tệp JSON mẫu dùng chung cho test hai phía: `docs/api-tests/kyc-contract/`.
6. **Chạy được khi chưa có AI Worker:** `genie.ai-worker.mode = ${AI_WORKER_MODE}` (`stub` mặc định ở profile local,
   `http` ở prod). Bản giả lập sinh tín hiệu theo 2 số cuối CCCD kê khai (`00–29` không tín hiệu → tự duyệt,
   `30–69` → `FACE_SCORE_BORDERLINE` + `UNREADABLE_IMAGE` → duyệt tay, `70–99` → `QR_MRZ_MISMATCH` + `FACE_SCORE_LOW`
   → từ chối) và áp kết quả sau 2 giây. Gọi AI Worker lỗi hoặc quá 30 giây không có callback → `MANUAL_REVIEW`.
7. **Máy trạng thái KYC trong entity** (LLD 6.3) bổ sung: `requestManualReview` (`REJECTED → MANUAL_REVIEW`, 1 lần / hồ
   sơ; hồ sơ đó nộp lại mà AI kết luận `FAIL` thì vào `MANUAL_REVIEW` thay vì `REJECTED`), `requeueForReview`
   (`VERIFIED / REJECTED / SUPPLEMENT_REQUESTED → MANUAL_REVIEW`, không chạy lại AI), `expireSupplement`
   (`SUPPLEMENT_REQUESTED` quá 7 ngày → `REJECTED`). Chuyển sai → `409 GNE-VND-4095`.
8. **Duyệt tay:** body `approve` thêm `is_mall`; `shop_commission_rate` chỉ nhận khi người duyệt có thêm quyền
   `UPDATE_COMMISSION` (ghi `COMMISSION_UPDATED`).
9. **Sau khi mở shop** `UserFacade.promoteToVendor` chỉ đổi vai trò, **không** thu hồi phiên; quyền `/seller` có ngay sau
   `POST /auth/sessions/refresh` (access token mới đọc vai trò từ CSDL).
10. **Cổng đảo ngược** do `vendor` khai, module khác cài khi được hiện thực: `ShopShippingSettings` (shipping),
    `PayoutAccountGuard` (payment), `KycAdminNotifier` / thông báo in-app (notification), `SellerBadgeContributor`
    (order, chat), `ShopCategoryLookup` (catalog), `ShopBootstrapContributor` (payment, shipping). GHN `shop/register`
    và tra tên chủ tài khoản ngân hàng đi qua cổng `PickupShopRegistrar` / `BankAccountLookup` có bản giả lập.

## Hệ quả

- Mất `KYC_AES_KEY` = mất toàn bộ hồ sơ đã mã hóa: khóa production nằm trong `deploy/.env`, sao lưu riêng, không xoay
  khi còn hồ sơ (xoay khóa để DATN).
- Hai bên (Java, Python) test hợp đồng bằng cùng tệp JSON mẫu; đổi hợp đồng phải sửa ADR này + API_SPEC 12 +
  implement 8.1.
- Backend giữ RAM cho mã hóa tệp ≤ 10 MB, xử lý tuần tự từng tệp.
