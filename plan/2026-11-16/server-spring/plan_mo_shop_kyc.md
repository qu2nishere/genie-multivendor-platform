# PLAN — Cụm B4: Mở shop & KYC người bán (7 use case)

| | |
|---|---|
| **Ngày lập** | 2026-11-16 |
| **Thể loại** | server-spring (module `vendor` + phần nền `common` / `auth` / `shipping` mà cụm cần) + nối `web/` (feature `seller-onboarding`, `seller-settings`, `admin-kyc`) |
| **Use case (7)** | Đăng ký mở gian hàng — Become a Seller (UC-VND-01) · Tải hồ sơ pháp lý & Giấy tờ định danh KYC (UC-VND-02) · Thẩm định KYC tự động qua AI — QR, MRZ & Face Match (UC-VND-03, **phần backend**; phần AI Worker ở [plan_kyc_ai_worker.md](../server-python/plan_kyc_ai_worker.md)) · Kiểm duyệt thủ công hồ sơ KYC (UC-VND-04) · Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID (UC-VND-05) · Đăng ký tài khoản nhận giải ngân (UC-VND-07) · Cập nhật thông tin gian hàng & cài đặt vận hành (UC-VND-21) |
| **Tài liệu chuẩn** | UC phân hệ 05 · API 0.7, 0.8, 0.9, 4.1, 9, 10.1, 10.11, 10.12, 11.3, 12, 15, 16 · ENTITY 1.4 (`VendorProfile`), 1.6 (`KycResult` + bảng tín hiệu rủi ro), 7.3 (`VendorShippingConfig`), Phụ lục A · LLD 6.3 (máy trạng thái KYC), 7.4 (SD-04), 9.6, 10 · LUU_TRU 5.2–5.7 · implement Phần 8 · TS 4, 7, 8, 9, 10, 14 — xem [README](../README.md) |
| **Phụ thuộc** | Phân hệ AUTH (đã có: OTP, `UserFacade`, Silo, email, audit) · plan [seed](../db/plan_seed_du_lieu_mau.md) S06 (hàng chờ KYC mẫu) · plan AUTH A12.01–A12.02 (danh mục hành chính GHN) cho giai đoạn K07 · [AI Worker](../server-python/plan_kyc_ai_worker.md) cho bản thật của K05 (có chế độ giả lập nên không phải chờ) |
| **Theo dõi tiến độ** | [progress_mo_shop_kyc.md](./progress_mo_shop_kyc.md) — cùng mã task `Kxx.yy` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |

---

## 0. Hiện trạng đã khảo sát

| Phần | Có gì | Thiếu |
|---|---|---|
| `vendor/shop/domain` | `VendorProfile` (≈ 50 trường: thông tin shop, 4 khóa ảnh giấy tờ, `kycStatus` 7 trạng thái, `kycRejectionReason`, `kycVerificationMethod`, `kycVerifiedBy`, `commissionRate`, `pickup*` + `ghnShopId`, `payout*` + `payoutAccountVerifiedAt`, `isMall`, bộ đếm, `preOrderEnabled`, `autoReply*`, `vacation*`); phương thức `submitKyc`, `startAiProcessing`, `routeToManualReview`, `requestSupplement`, `approveKyc`, `rejectKyc`, `isPickupConfigured`, `effectiveCommissionRate`, `verifyPayoutAccount`, `isPayoutAccountVerified`, `startVacation`…; enum `KycStatus`, `KycVerificationMethod`, `BusinessType`; repository rỗng | Toàn bộ `api`, `application`, `infra`. **Thiếu cột** cho: SĐT / email liên hệ của shop, SĐT kho, đồng ý sinh trắc, danh sách giấy tờ cần bổ sung + hạn 7 ngày, "đã yêu cầu nhân viên xem xét", thời điểm đổi tên shop, tên tệp + dung lượng giấy tờ (API 9.1, 10.11, 10.12 cần) |
| `vendor/kyc/domain` | `KycResult` (dữ liệu trích xuất, 8 cờ kiểm tra, `faceMatchScore`, `livenessScore`, `riskScore`, `riskSignalsJson`, `sampledForReview`, `overallStatus`; `applyRiskScore(score, signals, sampled, 30, 70, now)`, `requiresManualReview`, `isPassed`), `KycOverallResult` | Service chấm điểm, callback |
| `shipping/shipment/domain` | `VendorShippingConfig` + repository (module **khác**) | Cửa cho `vendor` đọc / ghi cấu hình hãng vận chuyển |
| `common` | `UploadService` / `S3ObjectStorage` (mới purpose `AVATAR`), `EmailSender`, `AuditLogger`, `InternalTokenFilter` (đã có khung 50 dòng), `AiWorkerHttpClient` (**rỗng — TODO**), `NameNormalizer`, cấu hình `genie.security.kyc-aes-key` (biến `KYC_AES_KEY`; mặc định dev ở `application-local.yml` là khóa base64 32 byte hợp lệ), `genie.ai-worker.url` (`AI_WORKER_URL`), `genie.storage.presigned-put-kyc-ttl = 10m`, `genie.shipping.ghn.*` | Mã hóa AES-256-GCM; purpose KYC / logo / bìa; client AI Worker |
| `auth` | `OtpChallengeService.issue(purpose, email, userId, payload)` (nội bộ), `OtpFacade.verifyStepUp(…)`, `OtpPurpose` có `SHOP_REGISTRATION`, `PAYOUT_ACCOUNT_UPDATE`, `PAYOUT_ACCOUNT_DELETE`; `UserFacade.promoteToVendor`, `isActive`, `findSummary`; `StepUpPrecondition` (cổng) | `OtpFacade` chưa cho module khác **phát** / **xác minh** OTP có dữ liệu kèm theo |
| CSDL | `vendor_profiles`, `kyc_results` (`V1`), `vendor_shipping_config` (`V7`) | Migration bổ sung cột (mục 2 — D2) |
| `web` | `seller-onboarding` (`registerShop`, `confirmShopRegistration`, `getKyc`, `saveKycDraft`, `submitKyc`, `requestManualReview`), `seller-settings` (`getSettings`, `patchSettings`, `savePickupAddress`, `listBanks`, `savePayoutAccount`, `deletePayoutAccount`, `getSellerMe`, `getPayoutAccount`…), `admin-kyc` (6 hàm) — đều xong trên mock; `KycGate` ở Seller Center; E2E `kyc-onboarding.spec.ts` (mock) | Nối API thật |
| `server-python` | Khung: `kyc/{router,schemas,service,qr,mrz,face,scoring}.py` là `# TODO` | Toàn bộ pipeline — plan riêng |

---

## 1. Nguyên tắc bắt buộc

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Package `com.genie.vendor.<feature>.{api,application,domain,infra,event}` với feature `shop` (mở shop, khung Seller, cổng KYC), `kyc` (hồ sơ, AI, duyệt), `settings` (cài đặt, kho, tài khoản nhận tiền); controller không mở transaction; một use case = một method `@Transactional` | TS 4.1–4.3 |
| N2 | Module khác chỉ dùng `ShopFacade` + record trong `event/`; `vendor` gọi module khác chỉ qua `*Facade` của họ (`UserFacade`, `OtpFacade`) hoặc **cổng đảo ngược** do `vendor` khai, module kia cài (`shipping`, `notification`, `payment`) | TS 4.4; ADR 0008 |
| N3 | **Máy trạng thái KYC nằm trong entity** (LLD 6.3): `NOT_SUBMITTED → PENDING → AI_PROCESSING → VERIFIED / MANUAL_REVIEW / REJECTED`; `MANUAL_REVIEW → VERIFIED / REJECTED / SUPPLEMENT_REQUESTED`; chuyển sai → `IllegalStateException`, service ánh xạ `409 GNE-VND-4095` | TS 7.2; LLD 6.3 |
| N4 | **Ảnh giấy tờ là dữ liệu cá nhân đặc biệt nhạy cảm:** mã hóa AES-256-GCM ở tầng ứng dụng trước khi lưu `kyc-dossiers`; không bao giờ trả URL ảnh cho chính người bán; nhân viên xem qua liên kết 5 phút của **bản giải mã tạm**, mỗi lần mở ghi `system_logs`; che số CCCD / số tài khoản khi hiển thị | UC-VND-02 ràng buộc; LUU_TRU 5.6–5.7; NFR-07 |
| N5 | **KYC theo tầng, cộng điểm rủi ro thay vì chặn cứng**; ngưỡng (30 / 70), bảng điểm tín hiệu, tỷ lệ lấy mẫu 5% nằm trong **cấu hình** (`genie.kyc.risk.*`), không ghi cứng trong mã | UC-VND-03 ràng buộc; ENTITY 1.6 c–d |
| N6 | **Không để quyết định bất lợi hoàn toàn tự động:** hồ sơ `FAIL` phải hiện lý do theo tín hiệu và có nút "Yêu cầu nhân viên xem xét" (1 lần / hồ sơ) | UC-VND-03 ràng buộc |
| N7 | Gọi AI Worker **bất đồng bộ sau commit** (`@Async @TransactionalEventListener(AFTER_COMMIT)`), timeout 30 giây; lỗi / quá hạn → `MANUAL_REVIEW` (không kẹt hồ sơ) | TS 4.5, 10.2; LLD 9.6 |
| N8 | Callback nội bộ chỉ nhận header token nội bộ (`InternalTokenFilter`), idempotent (gọi lại không đổi kết quả đã chốt) | API 12; TS 9 |
| N9 | **Cổng KYC:** mọi `/seller/**` trả `403 GNE-VND-4031` khi `kyc_status ≠ VERIFIED`, **trừ** `GET /seller/me`, `/seller/kyc/**`, `GET|PATCH /seller/settings`, `PUT /seller/pickup-address`, `/seller/payout-account/**`, nhóm vacation | API 0.9 |
| N10 | Kiểm sở hữu ở service: người bán chỉ thao tác trên shop của `userId` hiện tại — không nhận `shop_id` từ request ở nhóm `/seller/**` | TS 9 (IDOR) |
| N11 | Thao tác nhạy cảm (đổi / xóa tài khoản nhận tiền) bắt buộc OTP bước 2; mọi quyết định KYC, xem giấy tờ, đổi tài khoản nhận tiền, đổi hoa hồng ghi `system_logs` | API 0.7; NFR-08 |
| N12 | Đổi schema: ENTITY trước → migration → entity Java; không sửa `V1…V14` | TS 7.3 |
| N13 | JSON snake_case, vỏ `ApiResponse`, mã lỗi đúng API 15 có `details` khi spec ghi; `@Operation(summary = "UC-VND-xx …")` | TS 8 |
| N14 | Mỗi task có test: unit (entity, chấm điểm, mã hóa, chuẩn hóa tên) + `*IT` Testcontainers (PG + Redis + Silo); E2E mở shop + KYC là 1 trong 9 kịch bản bắt buộc | TS 14 |

---

## 2. Quyết định cần chốt trước khi làm

| # | Vấn đề | Nguồn lệch | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | `shop_id` trên API là id nào | API không ghi; ENTITY lưu `vendor_id = users.id` ở bảng nghiệp vụ | `shop_id` = `vendor_profiles.id`; `ShopFacade` cung cấp chuyển đổi hai chiều. Ghi vào API mục 0 | ☐ |
| D2 | `vendor_profiles` thiếu cột cho màn hình (API 9.1, 10.11, 10.12, 11.3) | API vs ENTITY 1.4 | `V15__vendor_kyc_additions.sql` thêm (đều cho phép `NULL` / có mặc định — chỉ **thêm**): `contact_phone VARCHAR(20)`, `contact_email VARCHAR(255)`, `pickup_phone VARCHAR(20)`, `kyc_biometric_consent BOOLEAN NOT NULL DEFAULT FALSE`, `kyc_documents_meta_json JSONB` (tên tệp + dung lượng từng giấy tờ), `kyc_supplement_documents_json JSONB`, `kyc_supplement_deadline_at TIMESTAMPTZ`, `kyc_manual_review_requested_at TIMESTAMPTZ`, `shop_name_changed_at TIMESTAMPTZ`; index `uk_vendor_profiles_shop_name_lower` (UNIQUE `lower(shop_name)`), `idx_vendor_profiles_kyc_status (kyc_status, kyc_submitted_at)`, `idx_vendor_profiles_supplement_deadline` (một phần). Sửa ENTITY 1.4 + 13.1 trước | ☐ |
| D3 | Hành động kiểm toán của vendor | ENTITY Phụ lục A (chỉ có `KYC_SUBMITTED / APPROVED / REJECTED`) vs API 11.3, NFR-07 | Thêm 4 giá trị trong cùng `V15`: `KYC_SUPPLEMENT_REQUESTED`, `KYC_REQUEUED`, `KYC_DOCUMENT_VIEWED`, `PAYOUT_ACCOUNT_CHANGED` (nhóm `KYC` ×3, `FINANCE` ×1). Mở shop, đổi cài đặt **không** ghi nhật ký kiểm toán | ☐ |
| D4 | Purpose tải tệp: LUU_TRU 5.4 liệt kê 4 purpose KYC riêng; API 4.1 dùng **một** `KYC_DOCUMENT` | LUU_TRU vs API | Theo **API**: `KYC_DOCUMENT` (bucket `kyc-dossiers`, pdf / jpeg / png, ≤ 10 MB, ảnh ≥ 20 KB, URL 10 phút, chỉ VENDOR), `SHOP_LOGO` (≤ 1 MB), `SHOP_COVER` (≤ 2 MB) ở `avatar-assets` (jpeg / png). Loại giấy tờ (`ID_FRONT`…) nằm ở khóa của `document_object_keys`. Sửa LUU_TRU 5.4 | ☐ |
| D5 | Mã hóa & xem ảnh KYC: ảnh đã mã hóa thì Presigned GET trực tiếp không xem được | LUU_TRU 5.6–5.7; API 11.3 | Nộp hồ sơ: backend đọc tệp `tmp/`, mã hóa AES-256-GCM (IV ngẫu nhiên 12 byte đặt đầu tệp, AAD = `vendorProfileId:docType`), ghi `kyc-dossiers/vendor/{vendorId}/{front\|back\|selfie\|license}-{uuid}.enc`, xóa `tmp/`. Xem (nhân viên / AI Worker): giải mã ra đối tượng tạm `kyc-dossiers/tmp/view/{uuid}` → Presigned GET 5 phút → job dọn sau 10 phút (lifecycle `tmp/` 1 ngày là lớp dự phòng) | ☐ |
| D6 | Hợp đồng Backend ↔ AI Worker: implement 8.1 (camelCase, worker trả `riskScore`) khác API 12 (`overall`, `checks{5}`, `face_match_score`…) | implement vs API vs UC-VND-03 | **Một hợp đồng snake_case** (chốt chung với plan AI Worker — Y00.02): yêu cầu `POST {AI_WORKER_URL}/kyc/verify` `{kyc_id, documents{id_front, id_back, selfie}: URL 5 phút, declared{identity_number, full_name}, biometric_consent, callback_url}` → `202`. Callback `PATCH /api/v1/internal/kyc/{id}/result` `{checks{qr_code, mrz, mrz_qr_match, cccd_number_valid, expiry_rule_valid, face_match, face_duplicate, national_emblem, fingerprint}, face_match_score, liveness_score, signals[{code}], extracted{…}, errors[], model}`. **Backend** là nơi quy đổi tín hiệu → điểm (bảng điểm cấu hình), kiểm trùng CCCD (`DUPLICATE_ID`), lấy mẫu 5% và kết luận — worker không quyết định. Sửa API 12, implement 8.1 | ☐ |
| D7 | Chạy được khi chưa có AI Worker | — | `genie.ai-worker.mode: ${AI_WORKER_MODE}` = `http \| stub` — mặc định `stub` ở `application-local.yml`, `http` ở `application-prod.yml`; máy dev muốn chạy cùng AI Worker thật thì đặt `AI_WORKER_MODE=http` trong `.env` (không sửa yml): bản giả lập sinh tín hiệu theo 2 số cuối CCCD kê khai (`…00–29` không tín hiệu, `…30–69` → `FACE_SCORE_BORDERLINE` + `UNREADABLE_IMAGE`, `…70–99` → `QR_MRZ_MISMATCH` + `FACE_SCORE_LOW`) và gọi thẳng dịch vụ chấm điểm sau 2 giây | ☐ |
| D8 | "Yêu cầu nhân viên xem xét" (API 10.12: chỉ khi `REJECTED`, `202`, lần 2 → `409 GNE-VND-4096`) cần chuyển `REJECTED → MANUAL_REVIEW` mà entity chưa có | API vs ENTITY | Thêm phương thức `VendorProfile.requestManualReview(now)` (chỉ từ `REJECTED`, chỉ 1 lần — ghi `kycManualReviewRequestedAt`) → `MANUAL_REVIEW`. Hồ sơ nộp lại sau đó mà AI kết luận `FAIL` thì vào `MANUAL_REVIEW` thay vì `REJECTED` (UC-VND-03 ràng buộc) | ☐ |
| D9 | "Đưa lại vào hàng chờ" (`POST …/requeue` → "Về `PENDING`") | API 11.3 vs máy trạng thái | Thêm `VendorProfile.requeueForReview()` : từ `VERIFIED / REJECTED / SUPPLEMENT_REQUESTED` → `MANUAL_REVIEW` (nhóm lọc `PENDING` của API gồm `PENDING, AI_PROCESSING, MANUAL_REVIEW`); không chạy lại AI | ☐ |
| D10 | Gắn nhãn Chính hãng khi duyệt (UC-VND-04 bước 8) — body `approve` của API 11.3 chỉ có `shop_commission_rate` | UC vs API | Thêm trường tùy chọn `is_mall` vào body `approve`; `shop_commission_rate` chỉ nhận khi người duyệt có thêm quyền `UPDATE_COMMISSION` (không có → `403 GNE-SEC-4030`), ghi `COMMISSION_UPDATED`. Sửa API 11.3 | ☐ |
| D11 | Sau khi mở shop: `UserFacade.promoteToVendor` (plan AUTH A10.01) "đổi role + thu hồi phiên", còn API 9.2 bảo client **làm mới token** để có quyền `/seller` | AUTH vs API | Giữ phiên hiện tại: `promoteToVendor` chỉ đổi `role` và đặt cờ buộc làm mới (không thu hồi refresh token); quyền `/seller` có ngay sau `POST /auth/sessions/refresh`. Kiểm và sửa ở K03.05 (scope `auth`) | ☐ |
| D12 | Đăng ký GHN Shop & danh mục hành chính | UC-VND-05; TS 10.1 | Cổng `PickupShopRegistrar` khai ở `vendor.settings.application`; cài đặt HTTP gọi GHN `shop/register` đặt ở `vendor/settings/infra` (chuyển về adapter của module `shipping` khi module đó được hiện thực); thiếu `GHN_TOKEN` → bản giả lập trả `ghn_shop_id` giả. Tên tỉnh / quận / phường lấy qua `AddressFacade` / `RegionDirectory` của AUTH (A12) — chưa có thì K07 chờ | ☐ |
| D13 | Xác minh chính chủ tài khoản ngân hàng | UC-VND-07 bước 4; implement 8.3 | Cổng `BankAccountLookup` + bản giả lập (`/mock/bank/lookup` nội bộ): trả tên chủ tài khoản = tên người dùng nhập đã bỏ dấu viết hoa, **trừ** số tài khoản kết thúc `000` → trả tên khác (để test không khớp). So với `kyc_results.extracted_full_name` (không có thì `vendor_profiles.representative`) sau `NameNormalizer` | ☐ |
| D14 | `PATCH /seller/settings` chứa khối `auto_reply` (UC-VND-06) và `GET` trả `vacation` (UC-VND-19) — hai UC ngoài cụm | API 10.11 | Cụm này **lưu / trả** các trường đó cho đủ hình dạng API (kiểm `GNE-VND-4222`), nhưng hành vi tự trả lời chat và `PUT / DELETE /seller/vacation` để plan của cụm chat / báo cáo | ☐ |
| D15 | Số điện thoại / email liên hệ shop và "Họ tên chủ shop" của form mở shop | API 9.1 vs ENTITY | `owner_name` → `representative`; `phone_number` → `contact_phone`; `email` → `contact_email` (cột mới D2); OTP mở shop gửi tới `email` này | ☐ |

---

## 3. Cây file sẽ tạo / sửa

```
server-spring/src/main/java/com/genie/
├── common/
│   ├── error/ErrorCode.java            (sửa) + VND-4031 · 4091 · 4093 · 4094 · 4095 · 4096 · 4221 · 4222 · 4223 · WAL-4222 · WAL-4091 · LOG-5022
│   ├── storage/UploadPurpose.java      (sửa) + KYC_DOCUMENT · SHOP_LOGO · SHOP_COVER
│   ├── storage/ObjectStorage.java      (sửa) + đọc / ghi nội dung tệp (getObject, putObject) nếu chưa có
│   ├── crypto/AesGcmCipher.java        (mới) mã hóa / giải mã AES-256-GCM dùng chung
│   ├── audit/AuditAction.java          (sửa) + 4 giá trị (D3)
│   └── ai/AiWorkerHttpClient.java      (sửa) RestClient có header token nội bộ, timeout
├── auth/otp/application/OtpFacade.java (sửa) + issue / verify cho module khác (K02)
├── auth/profile/application/UserFacade.java (sửa nếu cần — D11)
├── shipping/shipment/application/VendorShippingSettingsAdapter.java   (mới — cài cổng của vendor, K09)
└── vendor/
    ├── shop/
    │   ├── domain/       VendorProfile (sửa: trường + phương thức mới) · VendorProfileRepository (sửa)
    │   ├── application/  ShopFacade · ShopRegistrationService · SellerContext (tra shop của người gọi) · SellerShellService
    │   │                 ShopNamePolicy (chuẩn hóa, trùng, từ cấm) · SellerBadgeContributor (cổng)
    │   ├── api/          ShopRegistrationController · SellerMeController · AdminShopController (K10.05)
    │   │                 KycGateInterceptor + VendorWebConfig
    │   └── event/        ShopRegisteredEvent · ShopVerifiedEvent
    ├── kyc/
    │   ├── domain/       (đã có) KycResult · KycOverallResult · KycResultRepository · KycDocumentType (mới) · KycRejectReason (mới)
    │   ├── application/  KycSubmissionService · KycDossierStorage (mã hóa / bản xem tạm) · KycRiskPolicy (bảng điểm, ngưỡng, lấy mẫu)
    │   │                 KycDecisionService · KycVerificationClient (cổng) · KycAiTimeoutJob · KycSupplementExpiryJob
    │   │                 KycReviewService (duyệt tay) · KycQueryService · KycNotifier (email) · KycProperties
    │   ├── infra/        HttpKycVerificationClient · StubKycVerificationClient (D7)
    │   ├── api/          SellerKycController · AdminKycController · InternalKycController
    │   └── event/        KycSubmittedEvent · KycDecidedEvent
    └── settings/
        ├── application/  ShopSettingsService · PickupAddressService · PickupShopRegistrar (cổng) · PayoutAccountService
        │                 BankAccountLookup (cổng) · BankCatalog · ShopShippingSettings (cổng) · PayoutAccountGuard (cổng)
        ├── infra/        GhnPickupShopRegistrar · StubPickupShopRegistrar · MockBankAccountLookup
        └── api/          SellerSettingsController · SellerPickupAddressController · SellerPayoutAccountController · BankController
server-spring/src/main/resources/
├── application.yml                         (sửa) genie.kyc.* (hằng số nghiệp vụ) · genie.ai-worker.mode: ${AI_WORKER_MODE} · genie.vendor.*
├── application-local.yml / -prod.yml      (sửa) mặc định AI_WORKER_MODE = stub / http
├── banks/banks.json                        (mới) danh mục ngân hàng
├── templates/email/                        (mới) kyc-approved.html · kyc-rejected.html · kyc-supplement.html · payout-account-changed.html
└── db/migration/V15__vendor_kyc_additions.sql   (mới — D2, D3)
server-spring/src/test/java/com/genie/vendor/   *Test (unit) · ShopRegistrationIT · KycGateIT · KycSubmissionIT · KycDecisionIT
                                                 KycCallbackIT · KycReviewIT · PickupAddressIT · PayoutAccountIT · ShopSettingsIT
web/src/features/{seller-onboarding,seller-settings,admin-kyc}/infrastructure/*.api.ts   (sửa)
web/tests/e2e/kyc-onboarding-real.spec.ts                                                 (mới — chạy khi E2E_REAL_API=1)
docs/Document/…  ENTITY (1.4, 1.6, 13.1, Phụ lục A, E) · GIAI_NGHIA_ENTITY · java-code/Diagram01 · Class XMI · API_SPEC (0, 4.1, 9, 10.11, 10.12, 11.3, 12, 15)
                 LUU_TRU 5.4–5.7 · implement 8.1 · Usecase.md (7 UC) · CHECKLIST_TIEN_DO_UC.md · Quản Lý UseCase.xlsx
docs/adr/0011-vendor-kyc-va-hop-dong-ai-worker.md · docs/web-backend-handoff.md · docs/api-tests/vendor-*.http
```

---

## 4. Bảng ánh xạ Use case → API → Dữ liệu

| Use case | Endpoint (API) | Bảng | Redis / Silo | Giai đoạn |
|---|---|---|---|---|
| Đăng ký mở gian hàng (UC-VND-01) | `POST /me/shop-registrations` (9.1) · `POST /me/shop-registrations/{challenge_id}/confirm` (9.2) · `GET /seller/me` (10.1) | `vendor_profiles`, `users.role`, `vendor_wallets`\*, `vendor_shipping_config`\* | `otp:challenge:*` | K03 |
| Tải hồ sơ pháp lý & Giấy tờ định danh KYC (UC-VND-02) | `POST /uploads/presigned-urls` `KYC_DOCUMENT` (4.1) · `GET /seller/kyc` · `PUT /seller/kyc/draft` · `POST /seller/kyc/submissions` · `POST /seller/kyc/manual-review-requests` (10.12) | `vendor_profiles`, `system_logs` | Silo `kyc-dossiers` (`.enc`) | K04 |
| Thẩm định KYC tự động qua AI (UC-VND-03) — backend | gọi `POST {AI_WORKER_URL}/kyc/verify` · nhận `PATCH /internal/kyc/{id}/result` (12) | `kyc_results`, `vendor_profiles` | Silo bản xem tạm | K05 |
| Kiểm duyệt thủ công hồ sơ KYC (UC-VND-04) | `GET /admin/kyc-submissions` · `GET /admin/kyc-submissions/{id}` · `POST …/approve` · `…/reject` · `…/request-supplement` · `…/requeue` (11.3) | `vendor_profiles`, `kyc_results`, `system_logs` | Silo bản xem tạm | K06 |
| Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID (UC-VND-05) | `PUT /seller/pickup-address` (10.11) | `vendor_profiles.pickup*`, `ghn_shop_id` | — | K07 |
| Đăng ký tài khoản nhận giải ngân (UC-VND-07) | `GET /public/banks` · `POST /seller/payout-account/verification` · `PUT /seller/payout-account` 🔐 · `DELETE /seller/payout-account` 🔐 (10.11) | `vendor_profiles.payout*`, `system_logs` | `otp:challenge:*` | K08 |
| Cập nhật thông tin gian hàng & cài đặt vận hành (UC-VND-21) | `GET /seller/settings` · `PATCH /seller/settings` (10.11) · `POST /uploads/presigned-urls` `SHOP_LOGO` / `SHOP_COVER` | `vendor_profiles`, `vendor_shipping_config` | Silo `avatar-assets` | K09 |

\* Ví shop và cấu hình vận chuyển mặc định được tạo khi mở shop qua cổng (`payment`, `shipping`) — module chưa có cài đặt thì bỏ qua, module đó tự tạo khi được hiện thực (ghi ở K03.06).

**Mã lỗi thêm vào `ErrorCode` (đều đã có ở API 15 và web):** `GNE-VND-4031, 4091, 4093, 4094, 4095, 4096, 4221, 4222, 4223` · `GNE-WAL-4222, 4091` · `GNE-LOG-5022`. **Không** thêm `GNE-VND-4092`, `4224` (chế độ nghỉ — ngoài cụm).
**Migration:** `V15__vendor_kyc_additions.sql`. **UploadPurpose:** `KYC_DOCUMENT`, `SHOP_LOGO`, `SHOP_COVER`. **AuditAction:** 4 giá trị D3.

---

## 5. Nhánh, commit, PR

- Nhánh tích hợp: **`feature/vendor-shop-kyc`** tách từ `develop` (có mã Jira: `feature/GNE-<số>-vendor-shop-kyc`). Mỗi giai đoạn một nhánh con tách từ nhánh tích hợp, PR vào nhánh tích hợp (≤ ~400 dòng); xong mỗi mốc (K03, K06, K09, K12) mở PR nhánh tích hợp → `develop` (squash). Tên nhánh con: `feature/vendor-k01-schema`, `feature/vendor-k02-foundation`, `feature/vendor-k03-shop-registration`, `feature/vendor-k04-kyc-submission`, `feature/vendor-k05-kyc-ai`, `feature/vendor-k06-kyc-review`, `feature/vendor-k07-pickup`, `feature/vendor-k08-payout-account`, `feature/vendor-k09-settings`, `feature/web-vendor-integration` (K11).
- Scope commit: `db`, `common`, `auth`, `vendor`, `shipping`, `web`, `seller-ui`, `admin-ui`, `docs`.
- Quy ước chung: [README mục 5](../README.md).

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — unit + IT (PG + Redis + Silo) + ArchUnit + `SchemaValidationIT`.
2. Không lớp nào ngoài `vendor` import `com.genie.vendor..domain|infra|api` (ArchUnit); `vendor` không import `domain` của module khác.
3. Swagger UI hiện endpoint mới, summary có `UC-VND-xx`.
4. Thử tay bằng `docs/api-tests/vendor-*.http`, Mailpit `http://localhost:8025` để lấy OTP / xem email kết quả KYC, Silo console `http://localhost:9001` để xác nhận tệp trong `kyc-dossiers` **không mở được** (đã mã hóa).
5. Nếu đụng `web/`: `cd web && npm run verify`.
6. Tick task ở progress, ghi mã commit vào Nhật ký.

---

## 7. Danh sách task theo giai đoạn

Ký hiệu: `Kxx.yy` = task; `Kxx.Cn` = task commit.

### K00 — Chuẩn bị & chốt quyết định

- **[K00.01]** Kiểm danh tính git — `git config user.name` / `user.email`.
- **[K00.02]** Chốt quyết định D1–D15 — ghi vào đầu file progress; D6 chốt **cùng người làm AI Worker** (plan AI Worker Y00.02) và ghi thành ADR `docs/adr/0011-vendor-kyc-va-hop-dong-ai-worker.md`.
- **[K00.03]** Tạo nhánh tích hợp — `git switch develop && git pull && git switch -c feature/vendor-shop-kyc && git push -u origin feature/vendor-shop-kyc`.
- **[K00.04]** Dựng môi trường — `.env` gốc (`cp .env.example .env`, khối CSDL [1] Docker; profile `local` lấy từ `.env` — [docs/cau-hinh-moi-truong.md](../../../docs/cau-hinh-moi-truong.md)) + Docker local (postgres, redis, silo, mailpit) + backend + seed (plan seed S01–S06); `./mvnw verify` xanh trước khi viết dòng nào.
- **[K00.05]** Đọc màn mock theo thứ tự luồng — `/become-seller` → modal OTP → `/seller/kyc` (5 trạng thái băng thông báo, form, tải ảnh, selfie, đồng ý sinh trắc, lưu nháp) → `/seller/settings` → `/admin/kyc` (hàng chờ, chi tiết, trình xem ảnh, 4 hành động); ghi trường giao diện cần và chỗ web đang lệch API.
- **[K00.06]** Kiểm số migration — `V15` còn trống trên `develop` ([README mục 4](../README.md)); ghi vào progress.
- **[K00.C1]** COMMIT — `docs(vendor): record kyc and ai worker contract decisions in adr 0011`

### K01 — Tài liệu, migration `V15`, hằng số dùng chung (nhánh `feature/vendor-k01-schema`)

- **[K01.01]** ENTITY trước (N12) — `docs/Document/…/ENTITY.md` 1.4: 9 cột mới (D2) + phương thức mới (`requestManualReview`, `requeueForReview`, `saveKycDraft`, `attachKycDocuments`, `renameShop`, `updatePickup`, `setPayoutAccount`, `clearPayoutAccount`, `expireSupplement`); 13.1 thêm 3 index; Phụ lục A thêm 4 `AuditAction` (D3); Phụ lục E; CHANGELOG.
- **[K01.02]** Đồng bộ `GIAI_NGHIA_ENTITY.md`, `java-code/Diagram01_IdentityAndRbac.java`, `Diagram10_*.java`, `Genie_ClassDiagram_UML25.xmi` (thuộc tính + phương thức + literal mới).
- **[K01.03]** `V15__vendor_kyc_additions.sql` — theo thứ tự an toàn: thêm cột cho phép `NULL` / có mặc định; tạo index (`uk_vendor_profiles_shop_name_lower` — kiểm dữ liệu seed không trùng trước); thay `ck_system_logs_action_type` với **toàn bộ** giá trị hiện có + 4 giá trị D3.
- **[K01.04]** Entity Java — `VendorProfile` thêm 9 trường khớp cột (JSONB dạng `String *Json`), `AuditAction` thêm 4 giá trị (+ `category()` nếu plan Nhật ký kiểm toán đã merge); `SchemaValidationIT` xanh.
- **[K01.05]** `ErrorCode` — 12 mã ở mục 4 (thông điệp tiếng Việt theo API 15); cập nhật test đếm mã.
- **[K01.06]** `UploadPurpose` — `KYC_DOCUMENT`, `SHOP_LOGO`, `SHOP_COVER` (D4): bucket, MIME, dung lượng tối đa, **dung lượng tối thiểu 20 KB cho ảnh KYC**, TTL URL 10 phút cho KYC (`genie.storage.presigned-put-kyc-ttl`), vai trò được xin (VENDOR).
- **[K01.07]** Test — `PresignedUploadIT` thêm ca: người bán xin `KYC_DOCUMENT` được, khách hàng chưa mở shop `403`; ảnh 5 KB bị từ chối; PDF 11 MB bị từ chối; URL KYC hết hạn sau 10 phút.
- **[K01.08]** Seed — cập nhật `R__seed_demo_80_kyc_queue.sql` điền cột mới (plan seed S06.07) nếu file đã có trên `develop`.
- **[K01.C1]** COMMIT — `feat(db): add vendor kyc columns, indexes and kyc audit actions`
- **[K01.C2]** COMMIT — `feat(common): add vendor error codes and kyc upload purposes`

### K02 — Nền module `vendor` (nhánh `feature/vendor-k02-foundation`)

- **[K02.01]** `AesGcmCipher` (`common/crypto`) — `encrypt(plaintext, aad)` → `IV(12) ‖ ciphertext ‖ tag`, `decrypt(blob, aad)`; khóa 256 bit đọc từ `genie.security.kyc-aes-key` (biến `KYC_AES_KEY`, Base64), kiểm độ dài khi khởi động (sai độ dài → backend dừng với thông báo rõ); mặc định dev đã hợp lệ, VPS sinh bằng `openssl rand -base64 32` ghi vào `deploy/.env`; sai khóa / sai AAD / dữ liệu bị sửa → ngoại lệ rõ ràng.
- **[K02.02]** `AesGcmCipherTest` — mã hóa → giải mã ra đúng dữ liệu; hai lần mã hóa cùng dữ liệu cho bản mã khác nhau (IV ngẫu nhiên); sửa 1 byte → giải mã lỗi; sai AAD → lỗi; tệp 10 MB chạy trong giới hạn bộ nhớ.
- **[K02.03]** `ObjectStorage` (cài đặt `S3ObjectStorage` — Silo ở máy dev, AWS S3 ở production, ADR 0010; bucket `kyc-dossiers` trên S3 chặn công khai + SSE + versioning — `deploy/aws/s3-init.sh`) — bổ sung (nếu chưa có) `get(bucket, key)` trả luồng, `put(bucket, key, bytes, contentType)`, `presignGet(bucket, key, ttl)`, `stat`, `remove`; `S3ObjectStorage` cài đặt; test với Silo Testcontainers.
- **[K02.04]** `OtpFacade` (scope `auth`) — thêm `issue(purpose, email, userId, payload)` trả `$OtpChallenge` và `verify(challengeId, code, expectedPurpose, userId)` trả payload; chỉ cho phép purpose của module khác (`SHOP_REGISTRATION`); giữ nguyên luật 60 giây / 5 lần / 5 phút; test trong `auth`.
- **[K02.05]** `SellerContext` — tra `VendorProfile` theo `userId` hiện tại (`findByUserId`), không có → `403 GNE-SEC-4030`; trả `vendorProfileId`, `vendorUserId`, `kycStatus` (N10).
- **[K02.06]** `KycGateInterceptor` + `VendorWebConfig` — áp cho `/api/v1/seller/**`; danh sách ngoại lệ đúng API 0.9 (khai thành hằng số có chú thích nguồn); chưa `VERIFIED` → `403 GNE-VND-4031` kèm `details.kyc_status`.
- **[K02.07]** `ShopFacade` (cửa công khai — README mục 3) — `findMini(shopId)`, `findByVendorUserId(userId)`, `vendorUserIdOf(shopId)`, `shopIdOf(vendorUserId)`, `isVerified(vendorUserId)`, `isOnVacation(vendorUserId, now)`, `effectiveCommissionRate(vendorUserId, categoryRate)`; chỉ trả record (`ShopMiniView`…), không trả entity.
- **[K02.08]** `VendorProfileRepository` — `findByUserId`, `findByUserIdForUpdate`, `existsByShopNameNormalized`, tra theo trạng thái KYC có phân trang, `findStuckAiProcessing(before)`, `findSupplementExpired(now)`.
- **[K02.09]** `KycGateIT` — tài khoản VENDOR chưa `VERIFIED`: `GET /seller/me`, `/seller/kyc`, `GET /seller/settings` qua được; một endpoint `/seller/**` khác (dùng controller thử trong test) → `403 GNE-VND-4031`; khách hàng (không phải VENDOR) → `403 GNE-SEC-4030`.
- **[K02.C1]** COMMIT — `feat(common): add aes-gcm cipher and object read access for kyc files`
- **[K02.C2]** COMMIT — `feat(auth): let other modules issue and verify otp challenges`
- **[K02.C3]** COMMIT — `feat(vendor): add shop facade, seller context and kyc gate`

### K03 — Đăng ký mở gian hàng (UC-VND-01) (nhánh `feature/vendor-k03-shop-registration`)

- **[K03.01]** `ShopNamePolicy` — chuẩn hóa (`trim`, gộp khoảng trắng), độ dài 3–100, danh sách từ cấm trong `resources/vendor/banned-words.txt` (so sau khi bỏ dấu bằng `NameNormalizer`), trùng tên không phân biệt hoa thường → `409 GNE-VND-4094`; unit test.
- **[K03.02]** `POST /me/shop-registrations` — `ShopRegistrationRequest` (`shop_name`, `owner_name`, `phone_number` `^0\d{9,10}$`, `email`, `main_category_id`, `business_type` ∈ `INDIVIDUAL, HOUSEHOLD, BUSINESS`, `accept_terms` phải `true`); đã có shop → `409 GNE-VND-4093`; tên trùng / cấm → `409 GNE-VND-4094`; phát OTP `SHOP_REGISTRATION` tới `email` của shop qua `OtpFacade.issue` với payload là dữ liệu form → `202 $OtpChallenge`. Danh mục: kiểm tồn tại qua cổng `ShopCategoryLookup` (module `catalog` cài sau; mặc định chỉ kiểm khác null).
- **[K03.03]** `POST /me/shop-registrations/{challenge_id}/confirm` — `OtpFacade.verify` (sai mã / hết hạn theo luật OTP chung); transaction: kiểm lại chưa có shop + tên chưa trùng (hai request song song chỉ tạo 1 shop — dựa `UNIQUE user_id` và `uk_vendor_profiles_shop_name_lower`), tạo `VendorProfile` (`kycStatus = NOT_SUBMITTED`, `commissionRate = NULL`, `pickup*` / `ghnShopId` = `NULL`, `representative`, `contactPhone`, `contactEmail` — D15), `UserFacade.promoteToVendor(userId)`; phát `ShopRegisteredEvent`; trả `201 {shop_id, kyc_status}`.
- **[K03.04]** Factory `VendorProfile.register(…)` + unit test (trạng thái ban đầu đúng UC-VND-01 bước 4).
- **[K03.05]** Vai trò sau khi mở shop (D11, scope `auth`) — đọc `UserFacade.promoteToVendor`: bảo đảm phiên hiện tại **không** bị thu hồi và `POST /auth/sessions/refresh` trả access token mang `VENDOR`; nếu đang thu hồi phiên thì sửa + cập nhật test của `auth`.
- **[K03.06]** Sau commit (`ShopRegisteredEvent`, `AFTER_COMMIT`) — tạo ví shop và cấu hình vận chuyển mặc định qua cổng `ShopBootstrapContributor` (danh sách bean; `payment` / `shipping` cài khi có) — hiện tại `shipping` cài luôn ở K09.02 (GHN bật mặc định).
- **[K03.07]** `GET /seller/me` (`SellerShellService`) — `shop` (`$ShopMini`), `kyc_status`, `badges {orders_to_confirm, unread_chats}` gộp từ các `SellerBadgeContributor` (mặc định 0).
- **[K03.08]** `ShopRegistrationIT` — luồng đầy đủ (OTP lấy từ `FakeEmailSender`): có `vendor_profiles`, `users.role = VENDOR`, refresh xong gọi được `GET /seller/me`; đã có shop → `4093`; tên trùng (khác hoa thường / dấu cách) → `4094`; từ cấm → `4094`; sai OTP 5 lần → hủy, không tạo shop; hai lần confirm song song → 1 shop; `accept_terms = false` → `422`; tài khoản vẫn mua hàng được (gọi một API `/me/**`).
- **[K03.C1]** COMMIT — `feat(vendor): register a shop with email otp and promote the account to vendor`
- **[K03.C2]** COMMIT — `feat(vendor): return seller shell context with kyc status`
- **[K03.09]** Mở PR `feature/vendor-shop-kyc` → `develop` (mốc 1: K01–K03).

### K04 — Tải hồ sơ pháp lý & Giấy tờ định danh KYC (UC-VND-02) (nhánh `feature/vendor-k04-kyc-submission`)

- **[K04.01]** `KycDocumentType` — `ID_FRONT, ID_BACK, BUSINESS_LICENSE, SELFIE` (API 16) ↔ cột `idFrontUrl`, `idBackUrl`, `licenseUrl`, `selfieUrl`.
- **[K04.02]** Entity — `VendorProfile.saveKycDraft(identityCode, taxCode, representative, shopName, businessType, consent)` (chỉ khi `NOT_SUBMITTED / REJECTED / SUPPLEMENT_REQUESTED`), `attachKycDocument(type, encKey, fileName, size)`, siết `submitKyc(now)`: chỉ từ `NOT_SUBMITTED / REJECTED / SUPPLEMENT_REQUESTED` (đang xử lý / đã duyệt → `IllegalStateException` → `409 GNE-VND-4095`), xóa `kycRejectionReason` + thông tin bổ sung khi nộp lại; unit test bảng chuyển trạng thái.
- **[K04.03]** `KycDossierStorage.seal(vendorProfileId, type, tmpObjectKey)` — kiểm `tmpObjectKey` thuộc chính người gọi + đã tải lên + đúng MIME / dung lượng (`statObject`); đọc tệp, mã hóa (K02.01, AAD = `vendorProfileId:type`), ghi `kyc-dossiers/vendor/{vendorId}/{front|back|selfie|license}-{uuid}.enc`, xóa bản `tmp/`; trả khóa + tên tệp + dung lượng gốc (D5).
- **[K04.04]** `KycDossierStorage.openTemporaryView(encKey, aad)` — giải mã ra `kyc-dossiers/tmp/view/{uuid}.{ext}` + Presigned GET 5 phút (dùng cho AI Worker ở K05 và nhân viên ở K06); `KycViewCleanupJob` xóa bản xem quá 10 phút.
- **[K04.05]** `GET /seller/kyc` — đúng hình dạng API 10.12: `status`, `shop_name`, `business_type`, `identity_number`, `tax_code`, `representative`, `documents` (mỗi loại `{file_name, size_bytes}` hoặc `null` — **không** có URL, N4), `biometric_consent`, `rejection_reason`, `supplement {documents, note, deadline_at}`, `manual_review_requested`, `submitted_at`.
- **[K04.06]** `PUT /seller/kyc/draft` — mọi trường tùy chọn; có `document_object_keys` thì niêm phong ngay từng tệp (K04.03); không đổi `kycStatus`.
- **[K04.07]** `POST /seller/kyc/submissions` — kiểm: `identity_number` đúng 12 chữ số; `tax_code` + `BUSINESS_LICENSE` bắt buộc với `HOUSEHOLD` / `BUSINESS`; `ID_FRONT`, `ID_BACK`, `SELFIE` bắt buộc (giấy tờ đã có từ lần trước được bỏ qua); thiếu → `422 GNE-VND-4223 {missing: […]}`; trạng thái không cho nộp → `409 GNE-VND-4095`; transaction: niêm phong tệp mới, lưu dữ liệu kê khai + `kycBiometricConsent`, `submitKyc(now)`, `AuditLogger KYC_SUBMITTED {biometric_consent, consent_at}` (UC-VND-02 bước 2), phát `KycSubmittedEvent` → `202 {status: "PENDING"}`.
- **[K04.08]** `POST /seller/kyc/manual-review-requests` (D8) — chỉ khi `REJECTED` (khác → `409 GNE-VND-4095`); đã yêu cầu → `409 GNE-VND-4096`; `requestManualReview(now)` → `MANUAL_REVIEW`; `202`.
- **[K04.09]** Thông báo nội bộ — sau commit hồ sơ mới: `AdminNotification` loại `USER_APPROVAL` qua cổng `KycAdminNotifier` (module `notification` cài sau; mặc định ghi log).
- **[K04.10]** `KycSubmissionIT` (PG + Redis + Silo) — nộp đủ giấy tờ → `PENDING`, 3–4 tệp `.enc` trong `kyc-dossiers`, tệp `tmp/` đã xóa, tải tệp `.enc` về **không** phải ảnh hợp lệ nhưng giải mã ra đúng tệp gốc; thiếu selfie → `4223 {missing:["SELFIE"]}`; hộ kinh doanh thiếu MST / GPKD → lỗi đúng; nộp khi đang `PENDING` → `4095`; dùng `object_key` của người khác → từ chối; lưu nháp rồi `GET` thấy lại dữ liệu, không lộ URL; nộp lại sau `REJECTED` giữ giấy tờ cũ khi không gửi tệp mới; `KYC_SUBMITTED` có thời điểm đồng ý.
- **[K04.C1]** COMMIT — `feat(vendor): encrypt kyc documents with aes-gcm before storing them`
- **[K04.C2]** COMMIT — `feat(vendor): save kyc draft and submit kyc dossier for verification`

### K05 — Thẩm định KYC tự động qua AI — phần backend (UC-VND-03) (nhánh `feature/vendor-k05-kyc-ai`)

- **[K05.01]** `KycProperties` (`genie.kyc`) — `risk.auto-approve-below = 30`, `risk.reject-from = 70`, `risk.sample-rate = 0.05`, `risk.points.<CODE>` cho đủ 11 tín hiệu của ENTITY 1.6 (`QR_MRZ_MISMATCH 60`, `ID_STRUCTURE_MISMATCH 40`, `EXPIRY_RULE_MISMATCH 30`, `DUPLICATE_ID 50`, `FACE_DUPLICATE 70`, `FACE_SCORE_BORDERLINE 25`, `FACE_SCORE_LOW 60`, `LIVENESS_SUSPECT 40`, `LEGACY_CARD 30`, `DECLARED_DATA_MISMATCH 20`, `UNREADABLE_IMAGE 30`), `ai-timeout = 30s`, `supplement-days = 7`; khai trong `application.yml` kèm chú thích nguồn (N5) — đây là **hằng số nghiệp vụ** nên để trong yml, **không** đưa ra `.env` ([docs/cau-hinh-moi-truong.md](../../../docs/cau-hinh-moi-truong.md) mục 1).
- **[K05.02]** `KycRiskPolicy` — hàm thuần: nhận danh sách mã tín hiệu → `riskScore` + `riskSignalsJson` (`[{code, points}]`); tín hiệu lạ → bỏ qua + log cảnh báo; quyết định lấy mẫu bằng nguồn ngẫu nhiên tiêm được (test cố định được).
- **[K05.03]** `KycRiskPolicyTest` — không tín hiệu → 0; cộng dồn đúng bảng; `FACE_SCORE_BORDERLINE` đơn lẻ (25) vẫn dưới ngưỡng 30; `UNREADABLE_IMAGE` (30) rơi đúng vùng duyệt tay; đổi cấu hình điểm thì kết quả đổi; mẫu 5% ≈ 5% trên 10.000 lần với hạt giống cố định.
- **[K05.04]** `KycVerificationClient` (cổng) + `HttpKycVerificationClient` — `AiWorkerHttpClient` (RestClient, header `X-Internal-Token`, timeout kết nối 3 giây / đọc 10 giây — chỉ chờ `202`); yêu cầu theo hợp đồng D6: URL bản xem tạm của 3 ảnh (K04.04), dữ liệu kê khai, `biometric_consent`, `callback_url`.
- **[K05.05]** `StubKycVerificationClient` (D7) — bật khi `genie.ai-worker.mode = stub` (`AI_WORKER_MODE`, `@ConditionalOnProperty`); thêm `AI_WORKER_MODE` vào `.env.example`, `deploy/.env.example` (`http`) và bảng biến của [docs/cau-hinh-moi-truong.md](../../../docs/cau-hinh-moi-truong.md); sinh tín hiệu theo 2 số cuối CCCD kê khai, gọi `KycDecisionService` sau 2 giây bằng `TaskScheduler`.
- **[K05.06]** Listener gửi thẩm định — `@Async @TransactionalEventListener(AFTER_COMMIT)` nhận `KycSubmittedEvent`: transaction mới `startAiProcessing()` (`PENDING → AI_PROCESSING`) → gọi client; gọi lỗi (AI Worker tắt, 5xx) → `routeToManualReview()` ngay + ghi `errors` (N7).
- **[K05.07]** `KycDecisionService.apply(vendorProfileId, result)` — transaction, khóa dòng hồ sơ (`FOR UPDATE`); không ở `AI_PROCESSING` → bỏ qua (idempotent — N8); bổ sung tín hiệu phía backend: `DUPLICATE_ID` (số CCCD đã gắn shop khác đang `VERIFIED` — `duplicateIdFound`), `DECLARED_DATA_MISMATCH` (số CCCD / họ tên kê khai khác dữ liệu QR sau chuẩn hóa); không đồng ý sinh trắc → không tính tín hiệu khuôn mặt và **luôn** chuyển duyệt tay (UC-VND-02 bước 2); tính điểm (K05.02); lưu / cập nhật `KycResult` (1 dòng / shop: dữ liệu trích xuất, cờ kiểm tra, điểm khớp mặt, liveness, `errors`), gọi `applyRiskScore(score, signalsJson, sampled, 30, 70, now)`.
- **[K05.08]** Chuyển trạng thái theo kết luận — `PASS` → `approveKyc(null, OCR_AUTO, now)` + `ShopVerifiedEvent`; `PARTIAL` → `routeToManualReview()`; `FAIL` → `rejectKyc(lý do ghép từ tín hiệu, tiếng Việt)` — **trừ** khi hồ sơ đã từng yêu cầu nhân viên xem xét (`kycManualReviewRequestedAt != null`) thì `routeToManualReview()` (D8, N6); ghi `KYC_APPROVED` / `KYC_REJECTED` (actor hệ thống) + `KycDecidedEvent`.
- **[K05.09]** `InternalKycController` — `PATCH /api/v1/internal/kyc/{id}/result` (D6), chỉ qua `InternalTokenFilter` (thiếu / sai token → `401`); Bean Validation thân yêu cầu; trả `200 {kyc_status}`; `@Operation(summary = "UC-VND-03 — Nhận kết quả thẩm định KYC từ AI Worker")`.
- **[K05.10]** `KycAiTimeoutJob` — `@Scheduled` mỗi 15 giây: hồ sơ `AI_PROCESSING` quá `genie.kyc.ai-timeout` (tính từ lúc chuyển `AI_PROCESSING` — lưu ở `kyc_results.created_at` hoặc trường thời điểm thích hợp, ghi cách chọn vào Ghi chú) → `routeToManualReview()` + `errors = ["AI_TIMEOUT"]`; `FOR UPDATE SKIP LOCKED`.
- **[K05.11]** `KycNotifier` — sau commit quyết định: email `kyc-approved.html` / `kyc-rejected.html` (lý do theo tín hiệu + nút "Yêu cầu nhân viên xem xét" trỏ `/seller/kyc`) tới email tài khoản chủ shop (`UserFacade.emailOf`); thông báo in-app qua cổng (module `notification` cài sau).
- **[K05.12]** `KycDecisionIT` — dùng client giả trong test: không tín hiệu → `VERIFIED`, `kyc_verification_method = OCR_AUTO`, có `kyc_results` `PASS`; bị lấy mẫu → `MANUAL_REVIEW` + `sampled_for_review`; tổng 45 → `MANUAL_REVIEW` `PARTIAL`; tổng 120 → `REJECTED` có lý do; CCCD trùng shop đã duyệt → +50; không đồng ý sinh trắc → luôn `MANUAL_REVIEW`; hồ sơ đã yêu cầu xem xét + `FAIL` → `MANUAL_REVIEW`; email đúng mẫu.
- **[K05.13]** `KycCallbackIT` — sai / thiếu token nội bộ → `401`; callback 2 lần → lần 2 không đổi gì; callback cho hồ sơ không ở `AI_PROCESSING` → `200` không đổi trạng thái; AI Worker không phản hồi 30 giây (tua `MutableClock`) → job chuyển `MANUAL_REVIEW`; gọi worker lỗi ngay → `MANUAL_REVIEW`.
- **[K05.14]** Chạy thử chế độ giả lập — backend `local` (`mode = stub`): nộp 3 hồ sơ với CCCD đuôi `10`, `45`, `90` → `VERIFIED`, `MANUAL_REVIEW`, `REJECTED`; ghi kết quả vào Ghi chú.
- **[K05.C1]** COMMIT — `feat(vendor): score kyc risk signals with configurable points and thresholds`
- **[K05.C2]** COMMIT — `feat(vendor): send kyc dossiers to the ai worker and apply its result`
- **[K05.C3]** COMMIT — `feat(vendor): route stuck or failed ai checks to manual review`

### K06 — Kiểm duyệt thủ công hồ sơ KYC (UC-VND-04) (nhánh `feature/vendor-k06-kyc-review`)

- **[K06.01]** `KycRejectReason` — danh mục lý do (API 16 `KycRejectReason`, vd `BLURRY_DOCUMENT`, `INFO_MISMATCH`, `EXPIRED_DOCUMENT`, `SUSPECTED_FRAUD`, `OTHER`…) kèm nhãn tiếng Việt cho email.
- **[K06.02]** Entity — `requestSupplement(documents, note, deadline)` (mở rộng phương thức cũ: lưu `kycSupplementDocumentsJson`, `kycSupplementDeadlineAt = now + 7 ngày`), `requeueForReview()` (D9), `expireSupplement()` (`SUPPLEMENT_REQUESTED → REJECTED`), `markAsMall(boolean)`, `setNegotiatedCommission(rate)`; unit test chuyển trạng thái hợp lệ / không hợp lệ.
- **[K06.03]** `KycQueryService.search(status, page, size)` — `GET /admin/kyc-submissions`: nhóm `PENDING` = `PENDING + AI_PROCESSING + MANUAL_REVIEW`; sắp theo `riskScore` giảm dần rồi `kycSubmittedAt` (UC-VND-04 bước 2); mỗi dòng `id`, `code` (`KYC-{id}`), `shop`, `owner_name`, `business_type`, `status`, `submitted_at`; `summary.counts` theo trạng thái.
- **[K06.04]** `KycQueryService.detail(id, reviewer)` — `GET /admin/kyc-submissions/{id}` đúng API 11.3: `risk_level` (`LOW < 30`, `MEDIUM 30–69`, `HIGH ≥ 70`), `risk_score`, `is_random_audit`, `documents[]` (URL bản xem tạm 5 phút — K04.04), `ai_result` (`overall`, `checks`, `face_match_score`, `errors`; `null` khi AI chưa xong), `declared` (số CCCD **che** `0790•••••041`, người đại diện, MST), `shop_commission_rate`, `rejection_reason`, `supplement`. Mỗi lần mở ghi `KYC_DOCUMENT_VIEWED` (N4).
- **[K06.05]** `KycReviewService.approve(id, staffId, commissionRate?, isMall?)` — chỉ từ `MANUAL_REVIEW` (khác → `409 GNE-VND-4095`); `approveKyc(staffId, MANUAL_STAFF, now)`; `shop_commission_rate` chỉ khi người duyệt có `UPDATE_COMMISSION` (D10) và trong khoảng hợp lệ 0–100% → ghi `COMMISSION_UPDATED`; `is_mall` → `markAsMall`; `KYC_APPROVED`; `ShopVerifiedEvent`.
- **[K06.06]** `reject(id, staffId, reasonCode, note)` — `note` bắt buộc khi `OTHER` (`422`); `rejectKyc(nhãn lý do + ghi chú)`; `KYC_REJECTED`.
- **[K06.07]** `requestSupplement(id, staffId, documents[], note)` — ≥ 1 giấy tờ (`422`); hạn 7 ngày; `KYC_SUPPLEMENT_REQUESTED`.
- **[K06.08]** `requeue(id, staffId)` — D9; `KYC_REQUEUED`.
- **[K06.09]** `AdminKycController` — 6 endpoint API 11.3, `@PreAuthorize("hasAuthority('REVIEW_KYC')")`, mỗi hành động trả `200` kèm chi tiết đã cập nhật; `@Operation(summary = "UC-VND-04 …")`.
- **[K06.10]** `KycSupplementExpiryJob` — `@Scheduled` hằng ngày 01:00 (LLD 10): hồ sơ `SUPPLEMENT_REQUESTED` quá hạn → `expireSupplement()` (lý do "Quá hạn bổ sung hồ sơ"), email; `FOR UPDATE SKIP LOCKED`.
- **[K06.11]** Email — `kyc-supplement.html` (danh sách giấy tờ cần bổ sung, ghi chú, hạn), dùng lại `kyc-approved` / `kyc-rejected`; gửi `AFTER_COMMIT`.
- **[K06.12]** `KycReviewIT` — hàng chờ sắp đúng thứ tự, đếm theo trạng thái; chi tiết có URL ảnh mở được trong 5 phút và sinh `KYC_DOCUMENT_VIEWED`; số CCCD bị che; duyệt → `VERIFIED` + `kyc_verified_by` + email; duyệt kèm hoa hồng khi thiếu `UPDATE_COMMISSION` → `403`; từ chối `OTHER` thiếu ghi chú → `422`; yêu cầu bổ sung → hạn 7 ngày, người bán `GET /seller/kyc` thấy `supplement`; quá hạn (tua đồng hồ) → `REJECTED`; requeue; Moderator `200`, Finance Officer / CS `403`; hành động trên hồ sơ sai trạng thái → `409`.
- **[K06.C1]** COMMIT — `feat(vendor): review kyc dossiers manually with approve, reject and supplement`
- **[K06.C2]** COMMIT — `feat(vendor): expire kyc supplement requests after seven days`
- **[K06.13]** Mở PR `feature/vendor-shop-kyc` → `develop` (mốc 2: K04–K06).

### K07 — Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID (UC-VND-05) (nhánh `feature/vendor-k07-pickup`)

- **[K07.01]** Kiểm điều kiện — danh mục hành chính GHN (`RegionDirectory` / `AddressFacade.resolveRegion` — plan AUTH A12) đã có trên `develop`; chưa có thì dừng giai đoạn này, ghi vào Ghi chú (D12).
- **[K07.02]** `PickupShopRegistrar` (cổng) — `register(PickupInfo) → ghnShopId`, `update(ghnShopId, PickupInfo)`; `GhnPickupShopRegistrar` (GHN `…/v2/shop/register`, header `Token`, timeout 5 giây, thử lại 2 lần — TS 10.2), `StubPickupShopRegistrar` khi thiếu `GHN_TOKEN` (trả số giả ổn định theo `vendorProfileId`).
- **[K07.03]** Entity — `VendorProfile.updatePickup(phone, provinceId, provinceName, districtId, districtName, wardCode, wardName, addressLine, ghnShopId)`; unit test `isPickupConfigured()` đổi thành `true`.
- **[K07.04]** `PUT /seller/pickup-address` — body API 10.11 (`phone_number`, `province_id`, `district_id`, `ward_code`, `address_line`); tên tỉnh / quận / phường do **server tự điền** từ mã (không tin client); gọi GHN **trước**, thành công mới lưu; đã có `ghnShopId` thì gọi cập nhật; GHN lỗi → `502 GNE-LOG-5022` (không lưu); chỉ shop `VERIFIED` (UC-VND-05 tiền điều kiện — endpoint nằm trong danh sách ngoại lệ cổng KYC nên kiểm ở service) → `403 GNE-VND-4031`; trả `200 {…pickup_address, ghn_shop_id}`.
- **[K07.05]** `PickupAddressIT` — lưu thành công với registrar giả; mã phường không thuộc quận → `422`; GHN lỗi → `502`, dữ liệu cũ giữ nguyên; shop chưa duyệt → `403`; lưu lần 2 gọi cập nhật thay vì đăng ký mới.
- **[K07.C1]** COMMIT — `feat(vendor): save pickup address and register the shop with ghn`

### K08 — Đăng ký tài khoản nhận giải ngân (UC-VND-07) (nhánh `feature/vendor-k08-payout-account`)

- **[K08.01]** `BankCatalog` + `GET /public/banks` — đọc `resources/banks/banks.json` (`code`, `name` — các ngân hàng Việt Nam phổ biến), `Cache-Control: public, max-age=86400`; `BankController`.
- **[K08.02]** `BankAccountLookup` (cổng) + `MockBankAccountLookup` (D13) — có độ trễ giả 300 ms, timeout 5 giây.
- **[K08.03]** `POST /seller/payout-account/verification` — `{bank_code, account_number, account_holder}`; tra tên chủ tài khoản; so với họ tên KYC (`kyc_results.extracted_full_name`, không có thì `representative`) sau `NameNormalizer` (bỏ dấu, viết hoa, gộp khoảng trắng); khớp → `200 {matched: true, normalized_holder}`; không khớp → `422 GNE-WAL-4222 {kyc_name}`; rate limit 5 lần / phút / shop.
- **[K08.04]** Entity — `setPayoutAccount(bankName, accountNumber, holderName, bankReturnedName, kycFullName, now)` (dùng `verifyPayoutAccount` có sẵn; không khớp → không lưu), `clearPayoutAccount()` (đặt lại `payoutAccountVerifiedAt = NULL`); unit test.
- **[K08.05]** `PUT /seller/payout-account` 🔐 — OTP bước 2 `PAYOUT_ACCOUNT_UPDATE` (header `X-OTP-Challenge-Id`, `X-OTP-Code` qua `OtpFacade.verifyStepUp`); xác minh lại chính chủ **phía server** (không tin kết quả bước verification); lưu; `PAYOUT_ACCOUNT_CHANGED` (số tài khoản che trong `details`); email `payout-account-changed.html`; trả `payout_account` (`account_number_masked`, `is_verified`).
- **[K08.06]** `DELETE /seller/payout-account` 🔐 — OTP bước 2 `PAYOUT_ACCOUNT_DELETE`; còn lệnh rút đang chờ → `409 GNE-WAL-4091` qua cổng `PayoutAccountGuard` (module `payment` cài sau; mặc định không chặn) — cổng cũng được nối vào `StepUpPrecondition` để chặn **trước khi gửi mã** (API 3.5); `clearPayoutAccount()`; `204`.
- **[K08.07]** Điều kiện chung — chỉ shop `VERIFIED` (`403 GNE-VND-4031`); che số tài khoản ở mọi phản hồi (`•••• •••• 6712`).
- **[K08.08]** `PayoutAccountIT` — verification khớp / không khớp (`…000`); `PUT` thiếu OTP → `422 GNE-AUTH-4222`; OTP sai mục đích bị từ chối; lưu thành công có `payout_account_verified_at`, có nhật ký + email; đổi tài khoản không khớp tên → không lưu, tài khoản cũ giữ nguyên; xóa; shop chưa duyệt → `403`.
- **[K08.C1]** COMMIT — `feat(vendor): verify and save the shop payout bank account with step-up otp`

### K09 — Cập nhật thông tin gian hàng & cài đặt vận hành (UC-VND-21) (nhánh `feature/vendor-k09-settings`)

- **[K09.01]** `ShopShippingSettings` (cổng ở `vendor.settings.application`) — `get(vendorUserId)`, `save(vendorUserId, carriers, freeShippingEnabled, threshold)`, `createDefault(vendorUserId)`.
- **[K09.02]** `VendorShippingSettingsAdapter` (scope `shipping`, ở `shipping/shipment/application`) — cài cổng trên `VendorShippingConfig` + repository; tạo mặc định GHN bật khi mở shop (K03.06); test trong `shipping`.
- **[K09.03]** Entity — `renameShop(newName, now)` (tối đa 1 lần / 30 ngày: `shopNameChangedAt + 30 ngày > now` → `IllegalStateException` → `409 GNE-VND-4091 {next_rename_allowed_at}`), `updateBranding(description, logoKey, coverKey)`, `setPreOrderEnabled`, `configureAutoReply(enabled, greeting, awayMessage)` (bật mà cả hai rỗng → `422 GNE-VND-4222` — D14); unit test.
- **[K09.04]** `GET /seller/settings` — một lời gọi trả đủ khối API 10.11: `profile` (+ `next_rename_allowed_at`), `shipping`, `pickup_address` (+ `ghn_shop_id`), `payout_account` (che số), `auto_reply`, `pre_order_enabled`, `vacation` (chỉ đọc — D14).
- **[K09.05]** `PATCH /seller/settings` — gửi khối nào cập nhật khối đó; `profile`: tên (qua `ShopNamePolicy` + luật 30 ngày), mô tả, `logo_object_key` (`SHOP_LOGO`) / `cover_object_key` (`SHOP_COVER`) → `promote` sang `avatar-assets/vendor/{vendorId}/{logo|cover}/{uuid}.{ext}`, xóa ảnh cũ `AFTER_COMMIT`; `shipping`: tắt hết hãng → `422 GNE-VND-4221`, ngưỡng freeship ≥ 0; `auto_reply`; `pre_order_enabled`; trả `200` như `GET`.
- **[K09.06]** Người bán chưa `VERIFIED` vẫn vào được Cài đặt (UC-VND-21 tiền điều kiện) — kiểm trong IT.
- **[K09.07]** `ShopSettingsIT` — đổi tên lần 1 được, lần 2 trong 30 ngày → `4091` có `next_rename_allowed_at`, sau 30 ngày được; tên trùng shop khác → `4094`; đổi logo xóa ảnh cũ trên Silo; ảnh bìa > 2 MB → `422`; tắt hết hãng → `4221`; bật tự trả lời không nội dung → `4222`; chỉ gửi một khối thì khối khác không đổi; shop A không sửa được shop B (N10).
- **[K09.C1]** COMMIT — `feat(shipping): expose shop carrier settings to the vendor module`
- **[K09.C2]** COMMIT — `feat(vendor): view and update shop profile and operating settings`
- **[K09.08]** Mở PR `feature/vendor-shop-kyc` → `develop` (mốc 3: K07–K09).

### K10 — Cổng cho module khác & API phụ

- **[K10.01]** Cài cổng của plan khác bằng `ShopFacade` — `WishlistShopAvailability` (plan Sản phẩm yêu thích D3) và, nếu plan Theo dõi gian hàng đã merge, cho `ShopFollowDirectoryImpl` gọi lại `ShopFacade` (bỏ truy vấn trùng).
- **[K10.02]** Cổng của AUTH — nếu `GET /me` (plan AUTH A11.02) đã có: cài `AccountShopProvider` trả `shop {id, name, logo_url, kyc_status}`; chưa có thì ghi việc này vào CLAUDE.md mục 9.
- **[K10.03]** Sự kiện công khai — `ShopVerifiedEvent(shopId, vendorUserId)`, `ShopRegisteredEvent` có Javadoc ghi module nào nghe (catalog: mở đăng bán; notification: thông báo).
- **[K10.04]** `ShopFacade` bổ sung `searchVerified(q, limit)` cho ô chọn shop.
- **[K10.05]** `GET /admin/shops?q&limit=20` (API 11.16 — ô chọn đích banner) — `AdminShopController`, quyền `MANAGE_VOUCHER`, trả `[{id, name}]`; IT nhỏ.
- **[K10.C1]** COMMIT — `feat(vendor): provide shop lookups for other modules and admin pickers`

### K11 — Nối giao diện `web/` (nhánh `feature/web-vendor-integration`)

- **[K11.01]** `seller-onboarding/infrastructure/registration.api.ts` — `registerShop`, `confirmShopRegistration` theo `isMocked('vendor')`; sau confirm gọi làm mới token (`shared/auth/session.ts`) rồi chuyển `/seller/kyc`; ánh xạ `4093`, `4094`, lỗi OTP.
- **[K11.02]** `seller-onboarding/infrastructure/kyc.api.ts` — `getKyc`, `saveKycDraft`, `submitKyc`, `requestManualReview`; tải giấy tờ qua `shared/api/upload.ts` với `purpose = KYC_DOCUMENT` (ảnh KYC **không** nén WebP — LUU_TRU 5.4); `4223` → tô ô giấy tờ thiếu; `4095`, `4096`.
- **[K11.03]** `seller-settings/infrastructure/shop.api.ts` — `getSellerMe`; `KycGate` đọc `kyc_status` thật; xử lý chung `403 GNE-VND-4031` ở `shared/api/http.ts` hoặc lớp bọc của Seller (chuyển về màn KYC).
- **[K11.04]** `seller-settings/infrastructure/settings.api.ts` — `getSettings`, `patchSettings`, `savePickupAddress`, `listBanks`, `savePayoutAccount` (kèm OTP bước 2), `deletePayoutAccount`, `getPayoutAccount`; logo / bìa qua `SHOP_LOGO` / `SHOP_COVER`; `startVacation` / `endVacation` **giữ mock** (D14).
- **[K11.05]** `admin-kyc/infrastructure/kyc.api.ts` — 6 hàm; trình xem tài liệu dùng URL 5 phút (hết hạn thì gọi lại chi tiết); `approve` gửi `is_mall` (D10).
- **[K11.06]** Kiểm DTO — đối chiếu từng DTO web với phản hồi thật (snake_case, kiểu số / ngày); sửa mapper; cập nhật mock cho cùng hình dạng để `PUBLIC_USE_MOCK` vẫn chạy.
- **[K11.07]** i18n — rà khóa `errors.GNE-VND-*`, `errors.GNE-WAL-4222`, `errors.GNE-WAL-4091`, `errors.GNE-LOG-5022` đủ `vi.json` / `en.json`.
- **[K11.08]** Chạy thật hai phía — `PUBLIC_REAL_API=auth,upload,profile,security,vendor` (trong `web/.env`), backend `AI_WORKER_MODE=stub` (mặc định): tài khoản mới → mở shop (OTP Mailpit) → nộp KYC (CCCD đuôi `45`) → Moderator `phat.nt@genie.example` duyệt → người bán lưu kho lấy hàng, tài khoản nhận tiền (OTP), đổi logo; lặp lại với đuôi `10` (tự duyệt) và `90` (từ chối → yêu cầu nhân viên xem xét).
- **[K11.09]** E2E — `web/tests/e2e/kyc-onboarding-real.spec.ts` chạy khi `E2E_REAL_API=1` (OTP lấy qua API Mailpit): kịch bản "mở shop + KYC" (1 trong 9 kịch bản bắt buộc — TS 14); spec mock hiện có giữ nguyên.
- **[K11.10]** `cd web && npm run verify`.
- **[K11.C1]** COMMIT — `feat(seller-ui): connect shop registration and kyc screens to the real api`
- **[K11.C2]** COMMIT — `feat(seller-ui): connect shop settings, pickup address and payout account`
- **[K11.C3]** COMMIT — `feat(admin-ui): connect kyc review screens to the real api`

### K12 — Hoàn thiện cụm

- **[K12.01]** Bộ kiểm thử API — `docs/api-tests/vendor-registration.http`, `vendor-kyc.http`, `vendor-settings.http`, `admin-kyc.http`: mỗi UC ≥ 1 kịch bản đúng + 1 lỗi; ma trận quyền `REVIEW_KYC` / `UPDATE_COMMISSION` (TS 14).
- **[K12.02]** Bảo mật nhanh — xác nhận log không chứa số CCCD đầy đủ, số tài khoản, URL bản xem tạm; `Cache-Control: no-store` cho `/seller/kyc`, `/admin/kyc-submissions/**`; tệp trong `kyc-dossiers` không công khai (thử `curl` không ký → `403`).
- **[K12.03]** OpenAPI — kiểm `/v3/api-docs` đủ endpoint, mô tả lỗi; cập nhật bản xuất nếu repo đang giữ.
- **[K12.04]** Tài liệu nguồn — Usecase.md 7 UC: đường dẫn theo API (`/vendor/send-otp`, `/internal/vendor/{id}/kyc-result`, `genie-kyc-encrypted`…), dòng *Hiện trạng*; API_SPEC: mục 0 (`shop_id`), 4.1, 10.12, 11.3 (`is_mall`), 12 (hợp đồng callback D6), 15; LUU_TRU 5.4–5.7 (purpose, bản xem tạm); implement 8.1; LLD 9.6, 10 (giờ chạy job); `docs/cau-hinh-moi-truong.md` (`AI_WORKER_MODE`, `KYC_AES_KEY`, `GHN_TOKEN`); ghi lịch sử phiên bản.
- **[K12.05]** Tiến độ — `CHECKLIST_TIEN_DO_UC.md` (7 UC, tick từng lớp, Nhật ký), `Quản Lý UseCase.xlsx`; `docs/web-backend-handoff.md` (nhóm `seller-onboarding`, `seller-settings`, `admin-kyc`); `CLAUDE.md` mục 2 + mục 9 (cổng cần module khác cài: `PayoutAccountGuard`, `SellerBadgeContributor`, `ShopCategoryLookup`, `KycAdminNotifier`, `ShopBootstrapContributor`).
- **[K12.06]** Hợp đồng Swagger `genie-api-spec` — ghi việc cần đồng bộ (body callback, `is_mall`, `GET /admin/shops`) thành issue / dòng trong Ghi chú để làm ở đợt đồng bộ API_SPEC kế tiếp.
- **[K12.C1]** COMMIT — `test(vendor): add api test collections for shop registration and kyc`
- **[K12.C2]** COMMIT — `docs(vendor): record shop and kyc use cases as implemented`
- **[K12.07]** Mở PR `feature/vendor-shop-kyc` → `develop` (mốc cuối); mẫu PR ghi migration `V15` (chỉ thêm cột / index / giá trị `CHECK` — tương thích ngược).

---

## 8. Ngoài phạm vi

- **Pipeline AI** (giải mã QR, đọc MRZ, YuNet + SFace, liveness, trùng khuôn mặt) — [plan_kyc_ai_worker.md](../server-python/plan_kyc_ai_worker.md).
- Cấu hình tin nhắn trả lời tự động (UC-VND-06) — chỉ lưu trường; hành vi gửi tin thuộc cụm Chat & realtime. Bật chế độ tạm nghỉ (UC-VND-19) — `PUT / DELETE /seller/vacation` thuộc nhóm báo cáo / đơn hàng. Xem ví (UC-VND-20), rút tiền (UC-ADM-04/05).
- Đăng bán sản phẩm sau khi `VERIFIED` (chuỗi C1); tạo ví shop (module `payment`).
- Kiểm tra liveness / độ nét / giải mã thử QR phía trình duyệt — đã có ở giao diện (mock), tinh chỉnh khi ghép AI Worker.
- Đọc chip NFC, xác thực VNeID / C06; đối chiếu MST với cơ quan thuế (đồ án dùng giả lập); đa kho lấy hàng (`vendor_warehouses`) — DATN.

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Hợp đồng Backend ↔ AI Worker lệch nhau khi hai người làm song song | Chốt D6 thành ADR 0011 trước khi viết mã (K00.02 / Y00.02); test hợp đồng hai phía dùng **cùng** tệp JSON mẫu (`docs/api-tests/kyc-contract/*.json`) |
| Mất khóa `KYC_AES_KEY` → không giải mã được hồ sơ | Khóa nằm trong `deploy/.env` của máy chủ production (VPS / EC2 — CSDL RDS, tệp S3), sao lưu riêng; README ghi rõ; không đổi khóa khi còn hồ sơ (xoay khóa để DATN) |
| Tệp lớn (10 MB) mã hóa trong bộ nhớ làm tốn RAM | Giới hạn 10 MB + xử lý tuần tự từng tệp; test đo (K02.02); backend có ngân sách RAM riêng (TS 16.2) |
| Hồ sơ kẹt `AI_PROCESSING` khi AI Worker chết | Job timeout 30 giây + chuyển duyệt tay ngay khi gọi lỗi (K05.06, K05.10) |
| Bản xem tạm (ảnh đã giải mã) tồn tại lâu trên Silo | URL 5 phút + job dọn 10 phút + lifecycle `tmp/` 1 ngày; bucket riêng tư |
| `promoteToVendor` thu hồi phiên làm người dùng bị đăng xuất giữa luồng mở shop | Kiểm và sửa ở K03.05 (D11), có test |
| `V15` đụng `V16` / `V17` của plan khác (cùng sửa `CHECK`) | Sổ đăng ký ở README; nhánh merge sau chép đủ giá trị |
| GHN sandbox không ổn định khi demo | Bản giả lập `StubPickupShopRegistrar`; lỗi GHN không làm mất dữ liệu cũ (K07.04) |
| PR quá 400 dòng | Mỗi giai đoạn một nhánh con + nhiều commit — tách PR theo commit |
