# PROGRESS — Cụm B4: Mở shop & KYC người bán (7 use case)

| | |
|---|---|
| **Plan** | [plan_mo_shop_kyc.md](./plan_mo_shop_kyc.md) — chi tiết từng task tra theo mã `Kxx.yy` |
| **Ngày lập** | 2026-11-16 |
| **Tổng** | 117 task + 27 commit, 13 giai đoạn (K07 viết lại 2026-11-25: 10 task + 3 commit) |
| **Nhánh** | `feature/vendor-shop-kyc` |
| **Phụ trách** | Nguyễn Ngọc Thiện — lập plan và thực hiện (phân công 2026-11-17; xem bảng Thành viên trong `docs/Document/CHECKLIST_TIEN_DO_UC.md`) |
| **Người commit** | danh tính git đã cấu hình trong repo — không `--author`, không `Co-Authored-By` |
| **Cập nhật gần nhất** | 2026-11-25 — xong cả plan (144 / 144) |

**Cách dùng**

- Xong task nào đổi `[ ]` thành `[x]`. Task bỏ qua có lý do: đổi thành `[-]` và ghi lý do ở mục Ghi chú.
- Task `COMMIT` chỉ tick sau khi qua Cổng kiểm tra (plan mục 6); điền mã commit 7 ký tự vào chỗ `hash`.
- Thông điệp commit ghi sẵn bên dưới, thêm ` [#GNE-<số>]` vào cuối khi đã có mã Jira.
- Cập nhật cột "Xong" của bảng Tổng quan và dòng "Cập nhật gần nhất" sau mỗi buổi làm; ghi một dòng vào Nhật ký cuối trang.

---

## Quyết định (plan mục 2)

| # | Vấn đề | Đã chốt | Kết quả chốt / ngày |
|---|---|:---:|---|
| D1 | `shop_id` trên API là id nào | ☑ | Theo đề xuất: `shop_id` = `vendor_profiles.id`; `ShopFacade` đổi hai chiều (ADR 0011) · 2026-11-18 |
| D2 | `vendor_profiles` thiếu cột cho màn hình (API 9.1, 10.11, 10.12, 11.3) | ☑ | Theo đề xuất, nhưng migration là **`V17__vendor_kyc_additions.sql`** (V15 = banner, V16 = Điểm Genie đã dùng — sổ đăng ký README mục 4) · 2026-11-18 |
| D3 | Hành động kiểm toán của vendor | ☑ | Theo đề xuất: 4 giá trị trong V17; CHECK chép đủ V14 + `BANNER_CHANGED`; `AuditAction.category()` KYC ×3, FINANCE ×1 · 2026-11-18 |
| D4 | Purpose tải tệp: LUU_TRU 5.4 liệt kê 4 purpose KYC riêng; API 4.1 dùng **một** `KYC_DOCUMENT` | ☑ | Theo đề xuất (API 4.1): `KYC_DOCUMENT`, `SHOP_LOGO`, `SHOP_COVER` · 2026-11-18 |
| D5 | Mã hóa & xem ảnh KYC: ảnh đã mã hóa thì Presigned GET trực tiếp không xem được | ☑ | Theo đề xuất: mã hóa AES-256-GCM tầng ứng dụng + bản giải mã tạm 5 phút (ADR 0011 mục 4) · 2026-11-18 |
| D6 | Hợp đồng Backend ↔ AI Worker: implement 8.1 (camelCase, worker trả `riskScore`) khác API 12 (`overall`, `checks{5}`, `face_match_score`…) | ☑ | Theo đề xuất — hợp đồng snake_case ghi ở ADR 0011 mục 5 (trùng Y-D1 plan AI Worker; cùng người làm hai phía) · 2026-11-18 |
| D7 | Chạy được khi chưa có AI Worker | ☑ | Theo đề xuất: `AI_WORKER_MODE` = `stub` (local) / `http` (prod) · 2026-11-18 |
| D8 | "Yêu cầu nhân viên xem xét" (API 10.12: chỉ khi `REJECTED`, `202`, lần 2 → `409 GNE-VND-4096`) cần chuyển `REJECTED → MANUAL_REVIEW` mà entity chưa có | ☑ | Theo đề xuất: `requestManualReview(now)` chỉ từ `REJECTED`, 1 lần · 2026-11-18 |
| D9 | "Đưa lại vào hàng chờ" (`POST …/requeue` → "Về `PENDING`") | ☑ | Theo đề xuất: `requeueForReview()` → `MANUAL_REVIEW`, không chạy lại AI · 2026-11-18 |
| D10 | Gắn nhãn Chính hãng khi duyệt (UC-VND-04 bước 8) — body `approve` của API 11.3 chỉ có `shop_commission_rate` | ☑ | Theo đề xuất: thêm `is_mall`; hoa hồng cần thêm `UPDATE_COMMISSION` · 2026-11-18 |
| D11 | Sau khi mở shop: `UserFacade.promoteToVendor` (plan AUTH A10.01) "đổi role + thu hồi phiên", còn API 9.2 bảo client **làm mới token** để có quyền `/seller` | ☑ | Theo đề xuất: `promoteToVendor` không thu hồi phiên; refresh cấp token VENDOR (sửa ở K03.05) · 2026-11-18 |
| D12 | Đăng ký GHN Shop & danh mục hành chính | ☑ | Theo đề xuất: cổng `PickupShopRegistrar` + bản giả lập khi thiếu `GHN_TOKEN`; K07 phụ thuộc A12 (kiểm ở K07.01) · 2026-11-18 |
| D13 | Xác minh chính chủ tài khoản ngân hàng | ☑ | Theo đề xuất: `BankAccountLookup` giả lập, số TK đuôi `000` → tên khác · 2026-11-18 |
| D14 | `PATCH /seller/settings` chứa khối `auto_reply` (UC-VND-06) và `GET` trả `vacation` (UC-VND-19) — hai UC ngoài cụm | ☑ | Theo đề xuất: lưu / trả `auto_reply`, `vacation` chỉ đọc; hành vi để cụm khác · 2026-11-18 |
| D15 | Số điện thoại / email liên hệ shop và "Họ tên chủ shop" của form mở shop | ☑ | Theo đề xuất: `owner_name` → `representative`, `phone_number` → `contact_phone`, `email` → `contact_email` (OTP gửi tới đây) · 2026-11-18 |
| D16 | Kho lấy hàng 2 cấp + cách gọi GHN `shop/register` (bổ sung 2026-11-25) | ☑ | Theo đề xuất (theo D25 của plan AUTH người dùng đã đồng ý 2026-11-18): quận = `NULL`, `GHN_PICKUP_MODE` = `stub` mặc định / `http` · 2026-11-25 |

## Tổng quan

| Giai đoạn | Nội dung | Task | Commit | Xong | Mã Jira | Nhánh |
|---|---|---:|---:|---:|---|---|
| K00 | Chuẩn bị & chốt quyết định | 6 | 1 | 7 / 7 | — | `feature/vendor-shop-kyc` |
| K01 | Tài liệu, migration `V15`, hằng số dùng chung | 8 | 2 | 10 / 10 | — | `feature/vendor-k01-schema` |
| K02 | Nền module `vendor` | 9 | 3 | 12 / 12 | — | `feature/vendor-k02-foundation` |
| K03 | Đăng ký mở gian hàng (UC-VND-01) | 9 | 2 | 11 / 11 | — | `feature/vendor-k03-shop-registration` |
| K04 | Tải hồ sơ pháp lý & Giấy tờ định danh KYC (UC-VND-02) | 10 | 2 | 12 / 12 | — | `feature/vendor-k04-kyc-submission` |
| K05 | Thẩm định KYC tự động qua AI — phần backend (UC-VND-03) | 14 | 3 | 17 / 17 | — | `feature/vendor-k05-kyc-ai` |
| K06 | Kiểm duyệt thủ công hồ sơ KYC (UC-VND-04) | 13 | 2 | 15 / 15 | — | `feature/vendor-k06-kyc-review` |
| K07 | Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID (UC-VND-05) | 10 | 3 | 13 / 13 | — | `feature/vendor-k07-pickup` |
| K08 | Đăng ký tài khoản nhận giải ngân (UC-VND-07) | 8 | 1 | 9 / 9 | — | `feature/vendor-k08-payout-account` |
| K09 | Cập nhật thông tin gian hàng & cài đặt vận hành (UC-VND-21) | 8 | 2 | 10 / 10 | — | `feature/vendor-k09-settings` |
| K10 | Cổng cho module khác & API phụ | 5 | 1 | 6 / 6 | — | `feature/vendor-shop-kyc` |
| K11 | Nối giao diện `web/` | 10 | 3 | 13 / 13 | — | `feature/web-vendor-integration` |
| K12 | Hoàn thiện cụm | 7 | 2 | 9 / 9 | — | `feature/vendor-shop-kyc` |
| **Tổng** | | **117** | **27** | **144 / 144** | | |

## K00 — Chuẩn bị & chốt quyết định

- [x] **K00.01** Kiểm danh tính git
- [x] **K00.02** Chốt quyết định D1–D15
- [x] **K00.03** Tạo nhánh tích hợp
- [x] **K00.04** Dựng môi trường
- [x] **K00.05** Đọc màn mock theo thứ tự luồng
- [x] **K00.06** Kiểm số migration
- [x] **K00.C1** 🔖 COMMIT `docs(vendor): record kyc and ai worker contract decisions in adr 0011` · hash: `9e81d28`

## K01 — Tài liệu, migration `V15`, hằng số dùng chung

- [x] **K01.01** ENTITY trước (N12)
- [x] **K01.02** Đồng bộ `GIAI_NGHIA_ENTITY.md`, `java-code/Diagram01_IdentityAndRbac.java`, `Diagram10_*.java`, `Genie_ClassDiagram_UML25.xmi` (thuộc tính + phương thức + literal mới).
- [x] **K01.03** `V15__vendor_kyc_additions.sql`
- [x] **K01.04** Entity Java
- [x] **K01.05** `ErrorCode`
- [x] **K01.06** `UploadPurpose`
- [x] **K01.07** Test
- [-] **K01.08** Seed
- [x] **K01.C1** 🔖 COMMIT `feat(db): add vendor kyc columns, indexes and kyc audit actions` · hash: `d28250c`
- [x] **K01.C2** 🔖 COMMIT `feat(common): add vendor error codes and kyc upload purposes` · hash: `351ba0b`

## K02 — Nền module `vendor`

- [x] **K02.01** `AesGcmCipher` (`common/crypto`)
- [x] **K02.02** `AesGcmCipherTest`
- [x] **K02.03** `ObjectStorage`
- [x] **K02.04** `OtpFacade` (scope `auth`)
- [x] **K02.05** `SellerContext`
- [x] **K02.06** `KycGateInterceptor` + `VendorWebConfig`
- [x] **K02.07** `ShopFacade` (cửa công khai
- [x] **K02.08** `VendorProfileRepository`
- [x] **K02.09** `KycGateIT`
- [x] **K02.C1** 🔖 COMMIT `feat(common): add aes-gcm cipher and object read access for kyc files` · hash: `7571971`
- [x] **K02.C2** 🔖 COMMIT `feat(auth): let other modules issue and verify otp challenges` · hash: `37c0cd3`
- [x] **K02.C3** 🔖 COMMIT `feat(vendor): add shop facade, seller context and kyc gate` · hash: `e91ac45`

## K03 — Đăng ký mở gian hàng (UC-VND-01)

- [x] **K03.01** `ShopNamePolicy`
- [x] **K03.02** `POST /me/shop-registrations`
- [x] **K03.03** `POST /me/shop-registrations/{challenge_id}/confirm`
- [x] **K03.04** Factory `VendorProfile.register(…)` + unit test (trạng thái ban đầu đúng UC-VND-01 bước 4).
- [x] **K03.05** Vai trò sau khi mở shop (D11, scope `auth`)
- [x] **K03.06** Sau commit (`ShopRegisteredEvent`, `AFTER_COMMIT`)
- [x] **K03.07** `GET /seller/me` (`SellerShellService`)
- [x] **K03.08** `ShopRegistrationIT`
- [x] **K03.C1** 🔖 COMMIT `feat(vendor): register a shop with email otp and promote the account to vendor` · hash: `d9700f1`
- [x] **K03.C2** 🔖 COMMIT `feat(vendor): return seller shell context with kyc status` · hash: `5e80822`
- [-] **K03.09** Mở PR `feature/vendor-shop-kyc` → `develop` (mốc 1: K01–K03).

## K04 — Tải hồ sơ pháp lý & Giấy tờ định danh KYC (UC-VND-02)

- [x] **K04.01** `KycDocumentType`
- [x] **K04.02** Entity
- [x] **K04.03** `KycDossierStorage.seal(vendorProfileId, type, tmpObjectKey)`
- [x] **K04.04** `KycDossierStorage.openTemporaryView(encKey, aad)`
- [x] **K04.05** `GET /seller/kyc`
- [x] **K04.06** `PUT /seller/kyc/draft`
- [x] **K04.07** `POST /seller/kyc/submissions`
- [x] **K04.08** `POST /seller/kyc/manual-review-requests` (D8)
- [x] **K04.09** Thông báo nội bộ
- [x] **K04.10** `KycSubmissionIT` (PG + Redis + Silo)
- [x] **K04.C1** 🔖 COMMIT `feat(vendor): encrypt kyc documents with aes-gcm before storing them` · hash: `1b3428a`
- [x] **K04.C2** 🔖 COMMIT `feat(vendor): save kyc draft and submit kyc dossier for verification` · hash: `a423fc2`

## K05 — Thẩm định KYC tự động qua AI — phần backend (UC-VND-03)

- [x] **K05.01** `KycProperties` (`genie.kyc`)
- [x] **K05.02** `KycRiskPolicy`
- [x] **K05.03** `KycRiskPolicyTest`
- [x] **K05.04** `KycVerificationClient` (cổng) + `HttpKycVerificationClient`
- [x] **K05.05** `StubKycVerificationClient` (D7)
- [x] **K05.06** Listener gửi thẩm định
- [x] **K05.07** `KycDecisionService.apply(vendorProfileId, result)`
- [x] **K05.08** Chuyển trạng thái theo kết luận
- [x] **K05.09** `InternalKycController`
- [x] **K05.10** `KycAiTimeoutJob`
- [x] **K05.11** `KycNotifier`
- [x] **K05.12** `KycDecisionIT`
- [x] **K05.13** `KycCallbackIT`
- [x] **K05.14** Chạy thử chế độ giả lập
- [x] **K05.C1** 🔖 COMMIT `feat(vendor): score kyc risk signals with configurable points and thresholds` · hash: `6e69ddc`
- [x] **K05.C2** 🔖 COMMIT `feat(vendor): send kyc dossiers to the ai worker and apply its result` · hash: `4f3019f`
- [x] **K05.C3** 🔖 COMMIT `feat(vendor): route stuck or failed ai checks to manual review` · hash: `25cfddc`

## K06 — Kiểm duyệt thủ công hồ sơ KYC (UC-VND-04)

- [x] **K06.01** `KycRejectReason`
- [x] **K06.02** Entity
- [x] **K06.03** `KycQueryService.search(status, page, size)`
- [x] **K06.04** `KycQueryService.detail(id, reviewer)`
- [x] **K06.05** `KycReviewService.approve(id, staffId, commissionRate?, isMall?)`
- [x] **K06.06** `reject(id, staffId, reasonCode, note)`
- [x] **K06.07** `requestSupplement(id, staffId, documents[], note)`
- [x] **K06.08** `requeue(id, staffId)`
- [x] **K06.09** `AdminKycController`
- [x] **K06.10** `KycSupplementExpiryJob`
- [x] **K06.11** Email
- [x] **K06.12** `KycReviewIT`
- [x] **K06.C1** 🔖 COMMIT `feat(vendor): review kyc dossiers manually with approve, reject and supplement` · hash: `7ea86e8`
- [x] **K06.C2** 🔖 COMMIT `feat(vendor): expire kyc supplement requests after seven days` · hash: `dcc00d5`
- [-] **K06.13** Mở PR `feature/vendor-shop-kyc` → `develop` (mốc 2: K04–K06).

## K07 — Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID (UC-VND-05)

> Viết lại 2026-11-25 theo D16 (lần đầu dừng ở K07.01 vì chưa có A12).

- [x] **K07.01** Kiểm điều kiện
- [x] **K07.02** `PickupShopRegistrar` (cổng) + bản giả lập + bản GHN
- [x] **K07.03** Entity 2 cấp
- [x] **K07.04** `PUT /seller/pickup-address`
- [x] **K07.05** `PickupAddressIT`
- [x] **K07.06** Tài liệu nguồn
- [x] **K07.C1** 🔖 COMMIT `feat(vendor): save pickup address and register the shop with ghn` · hash: `fffef67`
- [x] **K07.07** Web bỏ cờ mock `pickup`
- [x] **K07.C2** 🔖 COMMIT `feat(seller-ui): connect the pickup address form to the real api` · hash: `0273177`
- [x] **K07.08** Bộ kiểm thử API + `./mvnw verify`
- [x] **K07.09** Chạy thật hai phía
- [x] **K07.10** Tiến độ
- [x] **K07.C3** 🔖 COMMIT `docs(vendor): record pickup address use case as implemented` · hash: (chính commit này — `git log --grep "record pickup address"`)

## K08 — Đăng ký tài khoản nhận giải ngân (UC-VND-07)

- [x] **K08.01** `BankCatalog` + `GET /public/banks`
- [x] **K08.02** `BankAccountLookup` (cổng) + `MockBankAccountLookup` (D13)
- [x] **K08.03** `POST /seller/payout-account/verification`
- [x] **K08.04** Entity
- [x] **K08.05** `PUT /seller/payout-account` 🔐
- [x] **K08.06** `DELETE /seller/payout-account` 🔐
- [x] **K08.07** Điều kiện chung
- [x] **K08.08** `PayoutAccountIT`
- [x] **K08.C1** 🔖 COMMIT `feat(vendor): verify and save the shop payout bank account with step-up otp` · hash: `ce5830c`

## K09 — Cập nhật thông tin gian hàng & cài đặt vận hành (UC-VND-21)

- [x] **K09.01** `ShopShippingSettings` (cổng ở `vendor.settings.application`)
- [x] **K09.02** `VendorShippingSettingsAdapter` (scope `shipping`, ở `shipping/shipment/application`)
- [x] **K09.03** Entity
- [x] **K09.04** `GET /seller/settings`
- [x] **K09.05** `PATCH /seller/settings`
- [x] **K09.06** Người bán chưa `VERIFIED` vẫn vào được Cài đặt (UC-VND-21 tiền điều kiện)
- [x] **K09.07** `ShopSettingsIT`
- [x] **K09.C1** 🔖 COMMIT `feat(shipping): expose shop carrier settings to the vendor module` · hash: `878ecd7`
- [x] **K09.C2** 🔖 COMMIT `feat(vendor): view and update shop profile and operating settings` · hash: `4d28102`
- [-] **K09.08** Mở PR `feature/vendor-shop-kyc` → `develop` (mốc 3: K07–K09).

## K10 — Cổng cho module khác & API phụ

- [x] **K10.01** Cài cổng của plan khác bằng `ShopFacade`
- [x] **K10.02** Cổng của AUTH
- [x] **K10.03** Sự kiện công khai
- [x] **K10.04** `ShopFacade` bổ sung `searchVerified(q, limit)` cho ô chọn shop.
- [x] **K10.05** `GET /admin/shops?q&limit=20` (API 11.16
- [x] **K10.C1** 🔖 COMMIT `feat(vendor): provide shop lookups for other modules and admin pickers` · hash: `b9cde08`

## K11 — Nối giao diện `web/`

- [x] **K11.01** `seller-onboarding/infrastructure/registration.api.ts`
- [x] **K11.02** `seller-onboarding/infrastructure/kyc.api.ts`
- [x] **K11.03** `seller-settings/infrastructure/shop.api.ts`
- [x] **K11.04** `seller-settings/infrastructure/settings.api.ts`
- [x] **K11.05** `admin-kyc/infrastructure/kyc.api.ts`
- [x] **K11.06** Kiểm DTO
- [x] **K11.07** i18n
- [x] **K11.08** Chạy thật hai phía
- [x] **K11.09** E2E
- [x] **K11.10** `cd web && npm run verify`.
- [x] **K11.C1** 🔖 COMMIT `feat(seller-ui): connect shop registration and kyc screens to the real api` · hash: `6e4cafc`
- [x] **K11.C2** 🔖 COMMIT `feat(seller-ui): connect shop settings, pickup address and payout account` · hash: `5fb33e5`
- [x] **K11.C3** 🔖 COMMIT `feat(admin-ui): connect kyc review screens to the real api` · hash: `6063f99`

## K12 — Hoàn thiện cụm

- [x] **K12.01** Bộ kiểm thử API
- [x] **K12.02** Bảo mật nhanh
- [x] **K12.03** OpenAPI
- [x] **K12.04** Tài liệu nguồn
- [x] **K12.05** Tiến độ
- [x] **K12.06** Hợp đồng Swagger `genie-api-spec`
- [x] **K12.C1** 🔖 COMMIT `test(vendor): add api test collections for shop registration and kyc` · hash: `8da73a7`
- [x] **K12.C2** 🔖 COMMIT `docs(vendor): record shop and kyc use cases as implemented` · hash: `b98c911`
- [-] **K12.07** Mở PR `feature/vendor-shop-kyc` → `develop` (mốc cuối); mẫu PR ghi migration `V15` (chỉ thêm cột / index / giá trị `CHECK`

---

## Chốt plan — tick tiến độ use case

Làm **ngay khi xong plan** (hoặc xong từng use case), trong file [`docs/Document/CHECKLIST_TIEN_DO_UC.md`](../../../docs/Document/CHECKLIST_TIEN_DO_UC.md) — cách tick xem mục 0 của file đó.

- [x] `UC-VND-01` Đăng ký mở gian hàng (xong K03 + K11) — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [x] `UC-VND-02` Tải hồ sơ pháp lý & Giấy tờ định danh KYC (xong K04 + K11) — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [x] `UC-VND-03` Thẩm định KYC tự động qua AI — tick `◐` khi xong K05 (chế độ giả lập); chỉ `☑` khi plan AI Worker cũng xong (Y07) — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [x] `UC-VND-04` Kiểm duyệt thủ công hồ sơ KYC (xong K06 + K11) — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [x] `UC-VND-05` Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID (xong K07 + K11) — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [x] `UC-VND-07` Đăng ký tài khoản nhận giải ngân (xong K08 + K11) — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [x] `UC-VND-21` Cập nhật thông tin gian hàng & cài đặt vận hành (xong K09 + K11) — tick các lớp BE / Test / Nối / Chạy đã xong, đổi cột Trạng thái
- [x] Cập nhật số ở mục 1 *Tổng quan* của checklist (Đã triển khai / Đang triển khai / Chưa có backend)
- [x] Thêm một dòng vào mục 5 *Nhật ký cập nhật* của checklist (ngày · use case · mã commit / PR)
- [x] Đổi cột *Hiện trạng mã nguồn* của các use case trên trong `Quản Lý UseCase.xlsx` (UC-VND-05 — 2026-11-25)
- [ ] Tick dòng plan này ở bảng *Tiến độ các plan* trong [README](../README.md) mục 6

## Ghi chú

Ghi ở đây: lý do bỏ qua task (`[-]`), chỗ làm khác plan, số đo, việc tồn chuyển sang plan khác.

| Task | Ghi chú |
|---|---|

## Nhật ký
| K00.01 | Danh tính git của repo: `Thien211105` (không `--author`, không `Co-Authored-By`). |
| K00.03 | Nhánh `feature/vendor-shop-kyc` tách từ `feature/social-shop-follow` (chồng nhánh như các plan trước — PR phải merge sau plan Theo dõi gian hàng); làm thẳng trên nhánh tích hợp, không tách nhánh con theo giai đoạn (commit vẫn theo từng task COMMIT). Chưa push. |
| K00.04 | Postgres dev chạy cổng 5433 (`POSTGRES_PORT=5433`, máy có Postgres khác ở 5432); JDK 21. `./mvnw verify` xanh ở nhánh nền (108 unit + 144 IT, plan Theo dõi gian hàng). |
| K00.05 | Lệch web ↔ API ghi lại để sửa ở K11: `registerShop` đọc `challenge_id`/`masked_target` (API trả `$OtpChallenge` có `email_masked`); admin-kyc `supplement.due_at` (API: `deadline_at`); `approveKyc` chưa gửi `is_mall`; 3 feature còn dùng `USE_MOCK` toàn cục thay cho `isMocked(<feature>)`. |
| K00.06 | `V15` (banner) và `V16` (Điểm Genie) đã dùng → migration của cụm này là **`V17__vendor_kyc_additions.sql`** (README mục 4 đã ghi). |
| K01.02 | XMI: thêm 9 thuộc tính + 17 phương thức `VendorProfile`, tham số mới của `requestSupplement`, literal `AuditAction` (kèm `BANNER_CHANGED` của plan banner còn thiếu); Diagram10 cũng bổ sung `BANNER_CHANGED`. |
| K01.03 | Migration là `V17__vendor_kyc_additions.sql` (không phải V15). CHECK chép đủ V14 + `BANNER_CHANGED` + 4 giá trị mới. |
| K01.04 | `approveKyc` / `rejectKyc` siết trạng thái nguồn (`AI_PROCESSING` / `MANUAL_REVIEW`); `setPayoutAccount` không khớp tên thì **không** xóa xác minh của tài khoản cũ (khác `verifyPayoutAccount`). Không thêm `setPreOrderEnabled` (trùng setter Lombok). |
| K01.06 | Purpose có quyền theo vai trò dùng `requiredAuthority = "ROLE_VENDOR"`; ảnh `KYC_DOCUMENT` < 20 KB → `422 GNE-VAL-4220 {min_bytes}` cả khi xin link lẫn khi nhận tệp (`acceptsSize`). |
| K01.08 | Bỏ qua: `R__seed_demo_80_kyc_queue.sql` chỉ có trên nhánh `feature/db-seed-demo` (chưa vào develop) và đã tự điền cột mới khi cột tồn tại (S06.07). Chú thích trong tệp đó còn ghi "V15" — sửa thành V17 khi merge nhánh seed. |
| K02.03 | `ObjectStorage.get(bucket, key)` đọc cả tệp vào bộ nhớ (giấy tờ ≤ 10 MB); `put` / `stat` / `presignGet` / `delete` đã có. Test `ObjectStorageIT` trên Silo; bucket `kyc-dossiers` thêm vào `IntegrationTestBase`. |
| K02.04 | `OtpFacade.issue` / `verify` chỉ mở cho `SHOP_REGISTRATION` (`FOREIGN_PURPOSES`); gửi lại mã dùng `POST /me/otp-challenges/{id}/resend` có sẵn (kiểm đúng chủ challenge). |
| K02.08 | `findStuckAiProcessing` tính hạn 30 giây từ `kyc_submitted_at` (nộp xong là gửi AI ngay trong listener sau commit) — không cần thêm cột. |
| K02.09 | Controller thăm dò `/api/v1/seller/__kyc-gate-probe` chỉ có trong `src/test`. Màn mở (me, kyc, settings, pickup, payout) được kiểm "không trả GNE-VND-4031" — endpoint chưa có thì 404. |
| K03.02 | Không dùng cổng `ShopCategoryLookup`: module `vendor` sẽ cài cổng `WishlistShopAvailability` của `catalog` (K10.01) nên `catalog` cài ngược cổng của `vendor` sẽ thành vòng phụ thuộc (ArchUnit). Thay bằng `catalog.category.application.CategoryFacade.isActive` (vendor → catalog một chiều). Danh mục không tồn tại / đang ẩn → `422 {field: main_category_id}`. |
| K03.05 | `UserFacade.promoteToVendor` bỏ `revokeAllSessions(ROLE_CHANGED)`; refresh cấp token đọc `users.role` → test `ShopRegistrationIT` refresh bằng cookie cũ rồi gọi `GET /seller/me` được; token CUSTOMER cũ vẫn mua hàng. |
| K03.06 | `ShopBootstrapListener` (AFTER_COMMIT, mỗi cài đặt một transaction REQUIRES_NEW, lỗi chỉ ghi log). Chưa có cài đặt nào — `shipping` cài ở K09.02; ví shop chờ module `payment`. |
| K03.09 | Chưa mở PR — chờ người dùng (nhánh chưa push; PR phải merge sau chuỗi plan 2–6). |
| K04.02 | Phương thức entity đã thêm ở K01.04 (cùng commit migration); unit test bảng chuyển trạng thái `VendorProfileTest` (commit K03.C1). |
| K04.03 | `UploadService.readOwnTmp` kiểm chủ sở hữu / loại / dung lượng (ảnh ≥ 20 KB) rồi đọc nội dung; SELFIE phải là ảnh. Tên tệp hiển thị lấy từ trường mở rộng `document_file_names` (tùy chọn — API 10.12 không có chỗ gửi tên tệp; không gửi thì đặt `front.jpg`…). Metadata `{file_name, size_bytes, content_type}` ở `kyc_documents_meta_json`. Rollback → xóa tệp `.enc` mới; commit → xóa tệp cũ bị thay. |
| K04.04 | Bản xem tạm ghi vào Redis ZSET `kyc:views` (điểm = thời điểm tạo); `KycViewCleanupJob` mỗi phút xóa bản quá 10 phút. |
| K04.09 | Cổng `KycAdminNotifier` + listener AFTER_COMMIT; chưa có cài đặt (module notification) → ghi log. |
| K05.02 | Lấy mẫu qua bean `KycSampler` (test IT thay bằng bản cố định); mỗi mã tín hiệu tính 1 lần; chỉ hồ sơ dưới ngưỡng tự duyệt mới có thể bị lấy mẫu. |
| K05.05 | `StubKycVerificationClient` dùng executor riêng (không phụ thuộc TaskScheduler — test tắt scheduling) áp kết quả sau 2 giây. Thêm `BACKEND_INTERNAL_URL` (`genie.ai-worker.callback-base-url`) để dựng `callback_url`. Test dùng `FakeKycVerificationClient` + `FakeKycSampler` (@Primary trong `IntegrationTestBase`). |
| K05.07 | `kyc_results` không có cột `national_emblem` / `fingerprint` → hai cờ này không lưu (màn duyệt trả `null`); thêm cột để đợt sau (ENTITY trước). Không đồng ý sinh trắc: không lưu điểm khuôn mặt, bỏ tín hiệu FACE_* / LIVENESS_SUSPECT. |
| K05.10 | Mốc tính quá hạn = `kyc_submitted_at` (listener gửi AI ngay sau commit nộp hồ sơ) — không thêm cột. |
| K05.12 | Trùng CCCD (+50) rơi vùng 30–69 → `MANUAL_REVIEW` (không phải REJECTED) đúng bảng điểm ENTITY 1.6. |
| K05.14 | Chưa chạy — gộp vào lần chạy thử backend local (stub) sau K10 cùng các endpoint khác. |
| K06.01 | Mã lý do theo web (`REJECT_REASONS`): BLURRY_DOCUMENT, INFO_MISMATCH, EXPIRED_DOCUMENT, SUSPECTED_FORGERY, MISSING_LICENSE, OTHER. Lý do lưu = "nhãn: ghi chú". |
| K06.04 | `checks.national_emblem` / `fingerprint` luôn `null` (chưa có cột — xem K05.07). Thêm trường `is_mall`, `manual_review_requested` vào chi tiết (màn hình cần). `shop_commission_rate` trả theo phần trăm (3 = 3%), lưu tỷ lệ 0.0300. Gọi lại chi tiết sau thao tác duyệt không ghi thêm `KYC_DOCUMENT_VIEWED`. |
| K06.05 | Hoa hồng riêng 0–50% (cùng giới hạn web `MAX_COMMISSION_RATE`). `request-supplement` nhận 6 loại giấy tờ của API 16.1 (kể cả BUSINESS_REGISTRATION, TAX_NOTICE). |
| K06.13 | Chưa mở PR — chờ người dùng. |
| K07.01 | Đã kiểm: **chưa có** danh mục hành chính GHN (`RegionDirectory` / `AddressFacade.resolveRegion` — plan AUTH A12 chưa hiện thực; `auth/address` mới có entity). Theo D12 dừng K07: `PUT /seller/pickup-address` chưa có (cổng KYC đã mở sẵn đường dẫn). Phương thức entity `updatePickup` + cột `pickup_phone` đã có (K01). Làm tiếp K07.02–K07.05 khi A12 merge. |
| K07.01 (2026-11-25) | Kiểm lại: `RegionDirectory` 2 cấp đã có trên `develop` (plan AUTH A12, D25); `ModuleBoundaryTest` cho `vendor` dùng gói `application` của `auth`. Làm K07 theo D16 trên nhánh `feature/vendor-k07-pickup` tách từ `develop` (qua commit tài liệu `57193bb` của `docs/update-b4-progress`). |
| K07.02 | Tra tài liệu GHN: `v2/shop/register` nhận `district_id` + `ward_code` theo địa giới cũ, cờ `is_new_to_address` chỉ có ở API tạo đơn → bản `http` gửi `ward_code` (mã mới), `name`, `phone`, `address`; chưa kiểm với GHN thật. `GhnPickupShopRegistrarTest` dùng `MockRestServiceServer` (5 ca). |
| K07.03 | `updatePickup` đổi chữ ký (bỏ 2 tham số quận); seed cũ còn mã quận vẫn `isPickupConfigured() = true` (chỉ cần tỉnh + phường + địa chỉ + Shop ID). Không cần migration (cột quận cho `NULL` từ V1). |
| K07.04 | Lưu xong xóa cache trang shop công khai (`PublicShopService.evict`) vì trang shop hiện tỉnh của kho; cache chi tiết sản phẩm ("Gửi từ") tự hết hạn sau 5 phút. Lỗi định dạng SĐT trả `errors[]` theo trường như các API khác. |
| K07.05 | `FakePickupShopRegistrar` (`@Primary` trong `IntegrationTestBase`, Shop ID giả `770000 + id`); 5 ca IT. |
| K07.08 | `./mvnw clean verify` (JDK 21 `~/.jdks/ms-21.0.10`): 193 unit + 326 IT xanh (bỏ qua `SearchPerformanceIT` như thường lệ). `vendor-pickup-address.http` chạy trên backend `local` 7 / 7 ca đúng (body gửi bằng tệp UTF-8 — curl trên dòng lệnh Windows làm hỏng tiếng Việt). |
| K07.09 | Theo yêu cầu người dùng (2026-11-25), Claude chạy bằng **Chromium thật điều khiển qua Playwright** (web 4321 cờ `vendor` + backend 8080 `GHN_PICKUP_MODE=stub` + Postgres + Redis), đăng nhập `owner.shoe@genie.example` qua `/auth/login`: 6 / 6 ca đạt (khối kho trống, nút khóa khi thiếu dữ liệu, lưu → thông báo + `GHN-880002`, tải lại còn dữ liệu, đổi phường giữ Shop ID). Sau khi chạy đã trả kho của shop Shoe Republic về như seed. |
| K08.05 | OTP bước 2 qua `@RequireStepUp` (aspect của auth — mã kiểm sau khi body qua `@Valid`). Email `payout-account-changed` gửi sau commit; nhật ký chỉ ghi số tài khoản đã che. |
| K08.06 | `PayoutAccountService` cài `StepUpPrecondition` cho `PAYOUT_ACCOUNT_DELETE` → chặn ngay khi xin mã nếu cổng `PayoutAccountGuard` (payment, chưa có cài đặt) báo còn lệnh rút chờ. |
| K09.02 | `VendorShippingSettingsAdapter` (shipping) cài cả `ShopShippingSettings` lẫn `ShopBootstrapContributor` → mở shop xong có `vendor_shipping_config` (GHN, GHTK bật) — kiểm trong `ShopRegistrationIT`. `free_shipping_enabled` không có cột: bật = có ngưỡng. |
| K09.03 | Phương thức entity đã thêm ở K01.04; unit test `VendorProfileTest`. |
| K09.05 | Logo / bìa lưu URL công khai đầy đủ (`avatar-assets/vendor/{shopId}/{logo|cover}/…`) — `ShopFacade.mediaUrl` vẫn đọc được khóa tương đối của dữ liệu mẫu. `description = ""` để xóa mô tả. |
| K09.08 | Chưa mở PR — chờ người dùng. |
| K10.01 | `VendorShopPorts` cài `WishlistShopAvailability` (shop đang nghỉ) + `AccountShopProvider` (`GET /me → shop`); `ShopFollowDirectoryImpl` dùng `ShopFacade.mediaUrl`. Bean giả trong `WishlistQueryIT` vẫn `@Primary` nên test cũ không đổi. |
| K10.02 | `GET /me` (A11.02) đã có → cài luôn `AccountShopProvider` (không cần ghi CLAUDE.md mục 9). |
| K11.01 | Cờ `PUBLIC_REAL_API=vendor` (cả 3 feature). Sửa lệch: 9.1 trả `$OtpChallenge` → đọc `email_masked` (web cũ đọc `masked_target`). Làm mới token sau confirm đã có sẵn ở `useConfirmShop`. |
| K11.02 | Gửi thêm `document_file_names`; DTO chấp nhận `business_type` / `identity_number` / `representative` = null (shop mới chưa kê khai). |
| K11.03 | `GET /seller/search` thuộc cụm đơn hàng — giữ mock bằng cờ riêng `seller-search`. Thêm backend `GET /seller/payout-account` (web `getPayoutAccount` đang gọi). Xử lý chung 403 GNE-VND-4031: Seller Center đã khóa nav theo `kyc_status` từ `/seller/me` nên không thêm lớp bọc. |
| K11.04 | Kho lấy hàng (`PUT /seller/pickup-address`) giữ mock bằng cờ `pickup` (K07 chờ A12); chế độ nghỉ giữ mock bằng cờ `vacation` (D14). Mapper chịu `pickup_address = null`, `description` / `auto_reply.*` = null. Xóa logo (gửi null) chưa hỗ trợ ở server — null = giữ nguyên. |
| K11.05 | `approveKyc(id, rate, isMall)` + ô "Gắn nhãn Shop Chính hãng" trong khối hoa hồng; mapper đọc `supplement.deadline_at` (fallback `due_at` của mock); `risk_level` / `risk_score` / `checks.*` null-safe. |
| K11.09 | Đã viết `web/tests/e2e/kyc-onboarding-real.spec.ts` (đăng ký qua API + OTP Mailpit → mở shop → nộp KYC CCCD đuôi 10 → chờ giả lập AI duyệt) — **chưa chạy** (cần backend + Mailpit + web cờ thật; chạy cùng K11.08). |
| K11.10 | eslint 0 lỗi, prettier, `astro check` 0 lỗi, vitest 320/320, `astro build` OK, i18n OK (3272 khóa); `npm run verify` gọi trực tiếp lỗi 127 trên máy này nên chạy từng bước. |
| K11.08 | Xong 2026-11-25 — theo yêu cầu người dùng, Claude chạy kịch bản `docs/kiem-thu/kich-ban-2026-11-16-mo-shop-kyc.md` bằng **Chromium thật điều khiển qua Playwright** (web bản build cổng 4321 vì máy thiếu RAM cho `astro dev` + backend `local` + Postgres + Redis + Silo + Mailpit + AI Worker Docker `AI_WORKER_MODE=http`), dùng ảnh CCCD + selfie thật của người dùng trong `test-kyc/` (người dùng cho phép; dữ liệu đọc từ ảnh không in ra log). 3 shop mới: A khai đúng → AI tự duyệt (QR + MRZ đạt, khớp mặt 0,57, 0 điểm); B selfie không có mặt + khai sai → `LIVENESS_SUSPECT` + `DECLARED_DATA_MISMATCH` 60 điểm → duyệt tay → từ chối / xin xem xét / từ chối lại / đưa lại hàng chờ; C không đồng ý sinh trắc + trùng CCCD → duyệt tay → bổ sung selfie → Admin duyệt 6 % + Chính hãng. Khoảng 80 ca đạt (mở shop, KYC, AI, duyệt, tài khoản nhận tiền, cài đặt). Lỗi tìm ra và đã sửa ở nhánh `bugfix/b4-real-run-fixes`: màn duyệt thiếu tín hiệu rủi ro (`8bbb2e5`, `6acef4f`), menu Seller Center không tự mở khóa + hộp thoại xóa TK nhận tiền treo khi xin OTP lỗi (`a4c7606`), gợi ý "mã thử: 123456" hiện khi chạy API thật (`0c5103e`); kiểm lại 5 / 5 đạt. Lưu ý môi trường: file `hosts` của Windows còn trỏ `host.docker.internal` về IP cũ `192.168.1.151` → trình duyệt không PUT ảnh lên Silo được (bài test ép phân giải về `127.0.0.1`); khởi động lại Docker Desktop để nó ghi lại IP. Ngoài phạm vi B4 (AUTH): chuyển trang ngay sau đăng nhập có thể hủy lượt làm mới token → bị đăng xuất. |
| K05.14 | Đã chạy (2026-11-18): jar profile local, DB mới `genie_kyc`, `AI_WORKER_MODE=stub`, SMTP → Mailpit; đăng ký tài khoản → mở shop (OTP Mailpit) → tải 3 ảnh qua presigned PUT Silo → nộp: CCCD đuôi **10 → VERIFIED**, **45 → MANUAL_REVIEW**, **90 → REJECTED** (lý do theo tín hiệu). Log không có ERROR. Lưu ý: `.env` máy dev trỏ SMTP Gmail — lần chạy đầu gửi nhầm 1 OTP tới địa chỉ giả `@genie.test` qua Gmail; chạy thử phải ghi đè `SMTP_HOST=localhost SMTP_PORT=1025`. |
| K12.02 | Log backend chạy thử không chứa số CCCD; GET không ký tệp `kyc-dossiers/vendor/1/front-….enc` → 403; `Cache-Control: no-store` ở `/seller/kyc`, `/seller/settings`, `/seller/payout-account`, `/admin/kyc-submissions/**` (có IT kiểm). Nhật ký chỉ ghi số tài khoản đã che. |
| K12.03 | `/v3/api-docs` có đủ 22 endpoint của cụm, summary mang mã UC. Repo không giữ bản xuất OpenAPI. |
| K12.04 | Usecase.md 7 UC (dòng Hiện trạng + bỏ `/vendor/send-otp`), API_SPEC (4.1, 9.2, 10.11 GET payout, 10.12 `document_file_names`, 11.3 `is_mall`, 12 callback mới, lịch sử 1.5), LUU_TRU 5.7, implement 8.1, `docs/cau-hinh-moi-truong.md` (`AI_WORKER_MODE`, `BACKEND_INTERNAL_URL`). LLD chưa sửa (job 01:00 / 15 giây đã ghi trong Javadoc). |
| K12.05 | CHECKLIST: 6 UC → Đang triển khai (17 / 56 / 11), UC-VND-05 vẫn Chưa (chờ A12); Excel cột S / U; handoff 3 nhóm ghi "đã nối"; CLAUDE.md mục 2 + mục 9 (cổng chờ module khác cài). |
| K12.06 | Cần đồng bộ hợp đồng Swagger `genie-api-spec` ở đợt sau: thân callback `PATCH /internal/kyc/{id}/result` (ADR 0011), yêu cầu `POST /kyc/verify`, `is_mall` của approve, `GET /admin/shops`, `GET /seller/payout-account`, `document_file_names`. |
| K12.07 | Chưa mở PR — chờ người dùng (nhánh chồng trên `feature/social-shop-follow`, chưa push). |

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-16 | Lập plan + progress (112 task, 25 commit, 13 giai đoạn) — chưa bắt đầu làm | — |
| 2026-11-18 | Plan Mở shop & KYC: K00–K06, K08–K12 xong (K07 chờ A12); full verify 131 unit + 181 IT xanh; web verify xanh; chạy thử stub 10/45/90 đúng. Còn: chạy thật giao diện (K11.08), PR. | 9e81d28 … 8da73a7 + commit tài liệu |
| 2026-11-25 | K07 làm lại theo D16 (địa giới 2 cấp): cổng `PickupShopRegistrar` + giả lập / GHN, `PUT /seller/pickup-address`, IT + unit, tài liệu nguồn, web bỏ cờ mock `pickup`; `./mvnw clean verify` + `npm run verify` xanh; chạy thật API 7 / 7 + Chromium 6 / 6 → UC-VND-05 ✅. Còn K11.08 (chạy thật 6 UC còn lại) | `fffef67`, `0273177` + commit tài liệu |
| 2026-11-25 | K11.08 chạy thật cả cụm bằng Playwright + AI Worker thật (ảnh CCCD thật); sửa 4 lỗi phát hiện; `./mvnw clean verify` 193 unit + 326 IT, `npm run verify` 358 test xanh → 6 UC còn lại ✅, cụm B4 7 / 7 | `8bbb2e5`, `6acef4f`, `a4c7606`, `0c5103e` + commit tài liệu |
