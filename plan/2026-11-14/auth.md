# PLAN — Phân hệ 01 AUTH: Xác thực & Tài khoản người dùng

> **Lập:** 2026-11-14 · **Phạm vi:** backend `server-spring` module `auth` + phần nền `common` mà AUTH cần + nối `web/` (feature `auth`, `account`) với API thật.
> **Use case (9):** UC-AUTH-01, 02, 03, 04, 06, 08, 09, 10, 11 (+ chuẩn bị móc cho UC-ADM-01 kích hoạt nhân viên).
> **Nguồn:** `"HOÀN CHỈNH "/diagrams/UseCaseDiagram/Document/Usecase.md` (Phân hệ 01) · `document/API_SPEC.md` mục 0, 2, 3.1–3.9, 4, 15 · `diagrams/ClassDiagram/ENTITY.md` 1.1–1.8, Phụ lục A · `document/LLD.md` 3, 7 (sequence đăng nhập), 11 · `kien-thuc/TECH_STACK_VA_QUY_UOC.md` (TS) 4, 7, 8, 9, 10 · `kien-thuc/LUU_TRU_DU_LIEU.md` 4.2 · `kien-thuc/implement.md` Phần 1.2, 4 · `docs/web-backend-handoff.md`.
> **Cập nhật:** 2026-11-14 (lần 2) — sau khi gộp loạt entity `feature/entity-p00…p12` (PR #28–#51): 65 entity + `V1…V12` đã có thật; A02, A03 viết lại thành "bổ sung", bỏ D1, thêm D20–D22. Chi tiết ở Nhật ký.
> **Cách dùng:** làm theo thứ tự giai đoạn ở mục 7; mỗi task có mã `Axx.yy`, tick `[x]` và ghi mã commit vào **Nhật ký** (mục 10) ngay khi xong. Quyết định ở mục 2 phải chốt **trước** A01.

---

## 0. Hiện trạng đã khảo sát

| Phần | Có gì | Thiếu gì cho AUTH |
|---|---|---|
| `server-spring/common` | `ApiResponse`, `ErrorResponse`, `GlobalExceptionHandler`, `ErrorCode` (6 mã), `BusinessException`, `BaseEntity`, `JacksonConfig` (snake_case), `ClockConfig`, `ModuleBoundaryTest` | `SecurityConfig`, `JwtService`, `JwtAuthFilter`, `CurrentUser`, `RateLimiter`, `InternalTokenFilter`, `AuditLogger`, `ObjectStorage`, `MinioObjectStorage`, `UploadController` vẫn là **stub 7 dòng** — đây là phần **chặn** việc làm AUTH; `ErrorResponse` chưa có `details`; `request_id` chưa lấy từ traceId; chưa có lớp gửi mail |
| `server-spring/auth` — entity | **Đã có, khớp 1-1 ENTITY.md** (plan `plan/2026-11-14/server-spring/implement-entity.md`): `auth/profile/domain/` `User` (20 trường, 7 PT, `@Version`, `uk_users_oauth`), `CustomerProfile`, `AccountStatus` (có `ANONYMIZED`), `Gender`; `auth/address/domain/` `Address`, `AddressLabel`; `auth/token/domain/` `RefreshToken`; mỗi entity một repository **rỗng** (chưa có query). `UserRole` ở `common/domain`. Liên quan: `admin/staff/domain` `AdminProfile`, `StaffTitle.defaultPermissions()`; `admin/rbac/domain` `StaffPermission`, `StaffPermissionEnum`; `common/audit` `SystemLog` (+ `SystemLog.of(...)`), `AuditAction` | Phương thức nghiệp vụ ngoài ENTITY (đổi mật khẩu, đổi email, link/unlink, yêu cầu xóa…); query repository; `refresh_tokens.family_id`, `last_used_at` (D6); `users.deletion_requested_at` (D7); `anonymize()` chưa xóa `oauthProvider` (D19) |
| `server-spring/auth` — còn lại | Thư mục feature `register/ login/ oauth/ password/ deletion/` + `api/`, `application/` của `profile/ address/ token/` chỉ có `.gitkeep` | Toàn bộ service / controller |
| Migration | `V1__identity.sql` đủ 8 bảng nhóm 1 (CHECK enum, unique 1-1, FK `ON DELETE CASCADE`); `V12__audit.sql` có `system_logs` + `ck_system_logs_action_type` + `idx_system_logs_created_action`; `V2…V11` đủ phần còn lại. `SchemaValidationIT` (Flyway + Hibernate `validate` trên PG 16 thật) xanh. **Đã merge vào `develop` → không sửa nữa** | `V13__bootstrap_admin.sql`, `R__seed_demo.sql` vẫn `TODO`; chưa có index `refresh_tokens(user_id)`, `addresses(user_id)` (D22); email `UNIQUE` phân biệt hoa thường (D21); `CHECK` của `system_logs.action_type` thiếu hành động AUTH (D20) → tất cả đi vào **`V14__auth_additions.sql`** |
| `pom.xml` | Spring Security, oauth2-resource-server (có Nimbus JOSE), oauth2-client, data-redis, mail, validation, Testcontainers 1.21.4 (PG), ArchUnit | `bcprov-jdk18on` (Argon2), `spring-boot-starter-thymeleaf` (mẫu email), Testcontainers Redis (dùng `GenericContainer`) |
| `templates/email/` | `otp.html`, `order-confirmed.html`, `refund.html` (khung) | `password-changed.html`, `new-device.html`, `deletion-scheduled.html`, `email-changed.html` |
| `web/features/auth` | Đủ màn: đăng nhập (mật khẩu / OTP), đăng ký + OTP, quên mật khẩu, màn tạm khóa, OAuth callback, `auth.api.ts` 7 hàm + `oauthAuthorizeUrl`, `completeOAuth` — **đã có nhánh gọi thật** | Sau đăng nhập / refresh chưa gọi `GET /me` để dựng `SessionUser` đầy đủ (vai trò, shop, quyền, `has_password`) |
| `web/features/account` | Hồ sơ, đổi email, liên kết Google, đổi mật khẩu + step-up OTP, thiết bị, xóa tài khoản, sổ địa chỉ + RegionPicker — 21 hàm có nhánh thật | `POST/DELETE /me/identities/GOOGLE` phải đổi thành `google` (handoff mục 2) |

---

## 1. Nguyên tắc bắt buộc (cho mọi task)

| # | Nguyên tắc | Nguồn |
|---|---|---|
| N1 | Package `com.genie.auth.<feature>.{api,application,domain,infra,event}`; controller không mở transaction; một use case = một method `@Transactional` ở `application` | TS 4.1–4.5 |
| N2 | Module khác **chỉ** dùng `UserFacade`, `AddressFacade`, `OtpFacade` và record trong `event/`. AUTH **không** gọi facade của module "phía sau" (order, payment, vendor, notification…) — dùng **cổng đảo ngược** (interface đặt trong `auth.*.application`, module kia cài đặt) để không tạo vòng phụ thuộc | TS 4.4, ArchUnit |
| N3 | Entity theo ENTITY.md: Lombok không `@Data`, `@Version` trên `User`, `TIMESTAMPTZ`, enum `STRING`; phương thức nghiệp vụ nằm trong entity, sai trạng thái → `IllegalStateException` | TS 7.1–7.2 |
| N4 | Đổi schema: **ENTITY.md trước → migration → entity Java**. Tên `idx_/uk_/fk_` | TS 7.3 |
| N5 | JSON snake_case, vỏ `ApiResponse`, lỗi `GNE-*` đúng API_SPEC 15, có `details` khi spec ghi; `@Operation(summary = "UC-AUTH-xx …")` trên mọi endpoint | TS 8 |
| N6 | Không bao giờ log / trả mật khẩu, OTP, refresh token, reset token. OTP & token chỉ lưu **bản băm** (SHA-256 + pepper) | TS 9, LUU_TRU 4.2 |
| N7 | Chống dò email: đăng nhập sai email và sai mật khẩu trả **cùng** `401 GNE-AUTH-4011`, thời gian xử lý đồng đều (băm giả khi không có user); quên mật khẩu luôn `202` | UC-AUTH-02, 04 |
| N8 | Mọi thao tác nhạy cảm ghi `system_logs` qua `AuditLogger` (đăng nhập, khóa, đổi / đặt lại mật khẩu, đổi email, liên kết / gỡ OAuth, yêu cầu xóa, ẩn danh) | NFR-08 |
| N9 | Mỗi task có test: unit cho entity + service (fake port), `*IT` Testcontainers cho luồng có Redis / DB | TS 14 |
| N10 | Thời gian lấy từ `Clock` bean (đã có `ClockConfig`) — không `OffsetDateTime.now()` trực tiếp, để test khóa 15 phút / 30 ngày | — |

---

## 2. Quyết định cần chốt trước khi làm

Tài liệu có chỗ lệch hoặc thiếu; dưới đây là đề xuất (cột **Chốt** để chủ dự án đánh dấu). Quyết định nào đổi schema / quy ước → ghi ADR `docs/adr/0007…` và sửa tài liệu nguồn trong `"HOÀN CHỈNH "`.

| # | Vấn đề | Nguồn lệch | Đề xuất | Chốt |
|---|---|---|---|---|
| D1 | ~~Điền nội dung migration placeholder~~ | — | **Không còn hiệu lực**: `V1…V12` đã được viết thật và merge (PR #28–#51). Mọi thay đổi schema của AUTH đi vào `V14__auth_additions.sql`; `V13`, `R__seed_demo` vẫn là placeholder chưa chạy ở đâu nên được điền (A02) | ✔ |
| D2 | Đăng ký: API 2.1 ghi "tài khoản `UNVERIFIED` tới khi xác minh"; UC-AUTH-01 + LUU_TRU ghi dữ liệu tạm ở Redis, chỉ tạo `users` khi OTP đúng | API vs UC | Theo **UC + LUU_TRU**: `register:temp:{email}` (Redis), không ghi rác vào PG. `UNVERIFIED` để dành cho lời mời nhân viên. Sửa câu trong API_SPEC 2.1 | ☐ |
| D3 | TTL `register:temp`: UC 300 s, LUU_TRU 15 phút | UC vs LUU_TRU | **15 phút** (cho phép gửi lại OTP nhiều lần trong một lượt đăng ký); OTP vẫn 5 phút | ☐ |
| D4 | Khóa OTP trong Redis: LUU_TRU đặt theo email (`otp:email_verify:{email}`), API dùng `challenge_id` | LUU_TRU vs API | Theo **API**: `otp:challenge:{challengeId}` (hash: purpose, email, user_id, code_hash, attempts) + con trỏ `otp:active:{purpose}:{email}` (mỗi email / mục đích chỉ một challenge sống). Cập nhật bảng khóa ở LUU_TRU 4.2 | ☐ |
| D5 | Đăng xuất: UC-AUTH-11 ghi `POST /auth/logout`; API ghi `DELETE /auth/sessions/current` | UC vs API | Theo **API** (API_SPEC 17 đã giải thích); sửa UC-AUTH-11 bước 2 | ☐ |
| D6 | "Thiết bị đăng nhập" cần `id` ổn định, `device_type`, `location`, `last_active_at`, `is_current`; `refresh_tokens` xoay vòng mỗi 15 phút nên `id` đổi liên tục; không có cột cho các trường trên | API 3.6 vs ENTITY 1.7 | Thêm `refresh_tokens.family_id BIGINT NOT NULL` (= id token gốc của phiên, cũng là `id` trả ra ở `/me/sessions`) và `last_used_at TIMESTAMPTZ`. `device_name` / `device_type` suy từ `deviceInfo` (User-Agent) lúc đọc; `location` trả `null` (chưa có GeoIP — DATN). JWT mang claim `sid = family_id` để biết phiên hiện tại. Thêm bằng `V14` (cột `NULL` được trước → backfill `family_id = id` → `SET NOT NULL`), sửa ENTITY 1.7 + entity `RefreshToken` | ☐ |
| D7 | Yêu cầu xóa tài khoản (30 ngày) không có cột nào lưu | API 3.7 vs ENTITY | Thêm `users.deletion_requested_at TIMESTAMPTZ NULL` + index một phần `WHERE deletion_requested_at IS NOT NULL`; `User.requestDeletion(now)`, `cancelDeletion()`. Thêm bằng `V14` (hoặc tách `V15` khi tới S7) | ☐ |
| D8 | Vô hiệu access token: LUU_TRU chỉ có `auth:denylist:{jti}` — không liệt kê được mọi jti khi "đăng xuất mọi thiết bị", đổi mật khẩu, bị đình chỉ | LUU_TRU | Giữ `auth:denylist:{jti}` cho đăng xuất phiên hiện tại + thêm `auth:revoked_before:{userId}` (epoch giây, TTL 15 phút): `JwtAuthFilter` từ chối token có `iat` < giá trị này | ☐ |
| D9 | Khóa tạm do sai mật khẩu: UC nhắc `status = LOCKED`, còn `User.recordFailedLogin()` chỉ đặt `lockedUntil` | UC vs ENTITY | Theo **ENTITY**: trạng thái vẫn `ACTIVE`, khóa = `lockedUntil > now`. `LOCKED` không dùng trong TLCN. `locked_by` (`PASSWORD`/`OTP`) lưu Redis `auth:locked_by:{userId}` cùng TTL | ☐ |
| D10 | Argon2: UC ghi "cost factor = 12" (là tham số bcrypt) | UC | Argon2id theo OWASP 2024: `m = 19456 KiB, t = 2, p = 1`, salt 16 B, hash 32 B (`new Argon2PasswordEncoder(16, 32, 1, 19456, 2)`) | ☐ |
| D11 | Ảnh đại diện: UC ghi chỗ 5 MB JPG/PNG/WEBP, chỗ 1 MB JPG/PNG; API 4.1 ghi 1 MB jpeg/png | UC vs API | **1 MB, JPG/PNG** (API) | ☐ |
| D12 | Gửi email: LLD đặt port `MailSender` ở cả `auth` và `notification`; nếu `auth` gọi `notification` còn `notification` nghe sự kiện của `auth` → vòng phụ thuộc | LLD 3 | Đặt **`common/mail`**: port `MailSender` + `SmtpMailSender` (Thymeleaf) — hạ tầng kỹ thuật, không nghiệp vụ. Email OTP gửi đồng bộ; email cảnh báo (đổi mật khẩu, thiết bị lạ) gửi trong listener `AFTER_COMMIT @Async`. ADR 0008 | ☐ |
| D13 | `GET /me` cần dữ liệu module khác (shop, số giỏ, thông báo, voucher, shop theo dõi); xóa tài khoản cần kiểm đơn đang chạy, trả hàng mở, số dư ví | API 3.1, 3.5 | **Cổng đảo ngược** đặt ở `auth`: `AccountShopProvider` (vendor cài), `AccountCounterContributor` (order / notification / promotion / social cài), `AccountDeletionGuard` (order / support / payment cài). Khi module kia chưa có: Spring inject `List<…>` rỗng → bộ đếm = 0, không chặn xóa. (Entity của các module kia **đã có** — cài cổng chỉ cần thêm query, không chờ entity) | ☐ |
| D14 | Quyền STAFF nạp từ `staff_permissions` (module `admin` chưa có) | implement 4.5 | Cổng `AuthorityProvider` ở `auth`. Entity `StaffPermission` (+ `isActive()`) đã có ở `admin/rbac/domain` nhưng `auth` **không** được import `admin..domain` (ArchUnit) → cài đặt `StaffPermissionAuthorityProvider` đặt ở `admin/rbac/application` (đọc quyền còn hiệu lực), làm ngay trong A04 vì entity sẵn. Mặc định khi chưa có bean: `ADMIN` = 13 quyền, `STAFF` = rỗng | ☐ |
| D15 | Danh mục hành chính GHN (`/public/regions/**`) và reverse geocode (nút GPS) | API 3.9 | Cổng `RegionDirectory` (auth.address) → `GhnRegionDirectory` (GHN master-data API, cache Redis 24 h). Thiếu `GHN_TOKEN` ở local → đọc ảnh chụp JSON `resources/regions/*.json`. Reverse geocode: Nominatim (OSM, miễn phí, ≤ 1 req/s) rồi khớp tên sang mã GHN bằng `NameNormalizer`; làm cuối (A13.08), cắt được | ☐ |
| D16 | Một user chỉ có **một** cặp `oauth_provider` / `oauth_provider_id` (ENTITY) — không gắn cùng lúc Google và Apple | ENTITY 1.1 | Chấp nhận cho TLCN (giao diện hiện chỉ có liên kết Google); ghi rõ ở `GET /me/login-methods`. DATN tách bảng `user_identities` | ☐ |
| D17 | Apple Sign In cần Apple Developer Program (trả phí), domain HTTPS, `client_secret` ký ES256 | Chi phí | **Google trước**; Apple sau cờ `genie.oauth.apple.enabled=false`, làm nếu còn thời gian S7 | ☐ |
| D18 | `$AuthTokens.user` chỉ có `id, role, full_name, avatar_url`; web cần `SessionUser` đầy đủ | API 1 vs web | Web gọi `GET /me` ngay sau đăng nhập / refresh để dựng `SessionUser` (task A15). Không đổi API | ☐ |
| D19 | `User.anonymize()` chỉ xóa `oauthProviderId`, giữ `oauthProvider` | ENTITY 1.1 | Xóa cả hai; sửa ENTITY | ☐ |
| D20 | `AuditAction` (Phụ lục A) chỉ có `LOGIN_SUCCESS, LOGIN_FAILED, ACCOUNT_LOCKED, ACCOUNT_SUSPENDED, ACCOUNT_REACTIVATED, PII_ANONYMIZED, ACCESS_DENIED` cho AUTH; UC-AUTH-04/06/10 đòi ghi `system_logs` cho đổi / đặt lại mật khẩu, yêu cầu xóa | ENTITY Phụ lục A vs UC | Thêm `PASSWORD_CHANGED, PASSWORD_RESET, EMAIL_CHANGED, OAUTH_LINKED, OAUTH_UNLINKED, SESSION_REVOKED, ACCOUNT_DELETION_REQUESTED, ACCOUNT_DELETION_CANCELLED` vào Phụ lục A → enum `AuditAction` → `V14` thay `ck_system_logs_action_type` (DROP + ADD CONSTRAINT đủ giá trị cũ + mới) | ☐ |
| D21 | `users.email` là `UNIQUE` thường → `Mai@x.com` và `mai@x.com` thành 2 tài khoản | V1 | Service luôn `trim().toLowerCase(Locale.ROOT)` trước khi ghi / tìm; `V14` thêm `CREATE UNIQUE INDEX uk_users_email_lower ON users (lower(email))` (giữ unique cũ để `validate` khớp `@Column(unique = true)`) | ☐ |
| D22 | ENTITY 13.1 không có index cho `refresh_tokens(user_id)`, `addresses(user_id)` dù là đường truy vấn của đăng nhập / thiết bị / sổ địa chỉ; PostgreSQL **không** tự tạo index cho cột FK | V1 | `V14`: `idx_refresh_tokens_user_active (user_id) WHERE revoked_at IS NULL`, `idx_refresh_tokens_family (family_id)`, `idx_addresses_user (user_id) WHERE is_deleted = false`; bổ sung vào ENTITY 13.1 | ☐ |

---

## 3. Cây thư mục sẽ tạo

```
server-spring/src/main/java/com/genie/
├── common/
│   ├── api/          ErrorResponse(+details) · GlobalExceptionHandler(+request_id, 401/403/429 chuẩn) · ApiResponse.created()
│   ├── error/        ErrorCode(+ ~30 mã AUTH/SEC/USR) · BusinessException(+details) · TooManyRequestsException(Retry-After)
│   ├── security/     SecurityConfig · JwtService · JwtAuthFilter · AuthUser(record) · CurrentUser · RateLimiter
│   │                 RestAuthEntryPoint · RestAccessDeniedHandler · ClientInfo (IP thật, User-Agent)
│   ├── mail/         MailSender (port) · SmtpMailSender · MailTemplate (enum)                     ← D12
│   ├── audit/        (ĐÃ CÓ) SystemLog · SystemLogRepository · AuditAction — (CẦN VIẾT) AuditLogger
│   └── storage/      ObjectStorage · MinioObjectStorage · UploadController (POST /uploads/presigned-urls) · UploadPurpose
└── auth/
    ├── profile/      domain/ (ĐÃ CÓ) User · CustomerProfile · AccountStatus · Gender · UserRepository · CustomerProfileRepository
    │                 application/ UserFacade · AuthorityProvider(port) · DefaultAuthorityProvider · PasswordHasher · PasswordPolicy
    │                 (cùng feature: GET /me, hồ sơ, đổi email — xem dòng profile/ bên dưới)
    ├── otp/          (feature mới, D4)
    │   ├── api/          MeOtpController (/me/otp-challenges) · OtpChallengeResponse
    │   ├── application/  OtpChallengeService · OtpFacade · RequireStepUp(annotation) · StepUpInterceptor · OtpPurpose
    │   └── infra/        RedisOtpStore
    ├── register/     api/ RegistrationController · application/ RegistrationService · event/ UserRegisteredEvent
    ├── login/        api/ SessionController, AuthOtpController · application/ LoginService, LoginOtpService, AccountUnlockService, NewDeviceDetector
    │                 event/ AccountLockedEvent, NewDeviceLoginEvent
    ├── token/        domain/ (ĐÃ CÓ) RefreshToken, RefreshTokenRepository · application/ SessionService (cấp, xoay vòng, thu hồi), RefreshCookie
    │                 api/ MeSessionController (/me/sessions) · application/ UserAgentParser, RefreshTokenCleanupJob
    ├── password/     api/ PasswordResetController, MePasswordController · application/ PasswordResetService, PasswordChangeService
    ├── oauth/        api/ OAuthController, MeIdentityController · application/ OAuthLoginService, IdentityProviderVerifier(port)
    │                 infra/ GoogleIdentityProvider, AppleIdentityProvider, RedisOAuthStateStore
    ├── profile/      (tiếp)
    │                 api/ MeController (GET /me), MeProfileController, EmailChangeController
    │                 application/ MeQueryService, ProfileService, EmailChangeService, AccountShopProvider(port), AccountCounterContributor(port)
    ├── address/      domain/ (ĐÃ CÓ) Address, AddressLabel, AddressRepository · api/ MeAddressController, RegionController
    │                 application/ AddressService, AddressFacade, RegionDirectory(port) · infra/ GhnRegionDirectory, StaticRegionDirectory, NominatimGeocoder
    └── deletion/     api/ DeletionRequestController · application/ AccountDeletionService, AccountDeletionGuard(port), AnonymizeAccountsJob
                      event/ AccountDeletionRequestedEvent, AccountAnonymizedEvent

server-spring/src/main/resources/
├── db/migration/ V13__bootstrap_admin.sql · R__seed_demo.sql (điền) · V14__auth_additions.sql (mới)
├── templates/email/ otp.html · password-changed.html · new-device.html · email-changed.html · deletion-scheduled.html
└── regions/ provinces.json · districts.json · wards.json      (ảnh chụp GHN cho local, D15)

server-spring/src/test/java/com/genie/
├── support/ IntegrationTestBase (PG + Redis, theo mẫu SchemaValidationIT) · TestDataBuilder · FixedClock · FakeMailSender
└── auth/    *Test (unit) · RegisterFlowIT · LoginLockoutIT · RefreshRotationIT · PasswordResetIT · StepUpOtpIT
             AddressDefaultIT · OAuthLinkingIT · AnonymizeAccountsJobIT
```

Entity `User` nằm ở `auth/profile/domain` (đã chốt khi dựng entity) — service dùng chung về tài khoản (`UserFacade`, `PasswordHasher`, `AuthorityProvider`) đặt ở `auth/profile/application`. Feature `otp/` là phần thêm so với cây TS 2.3 (`register/ login/ token/ oauth/ password/ profile/ address/ deletion/`): OTP dùng chung đăng ký / đăng nhập / quên mật khẩu / step-up (cả cho `payment`, `vendor`). Ghi vào ADR 0007.

---

## 4. Bảng ánh xạ Use case → API → Dữ liệu

| UC | Endpoint (API_SPEC) | Bảng | Redis | Sprint |
|---|---|---|---|---|
| AUTH-01 Đăng ký | `POST /auth/registrations` (2.1) · `POST /auth/otp-challenges/{id}/resend` (2.3) · `POST /auth/otp-challenges/{id}/verify` purpose `REGISTER` (2.4) | `users`, `customer_profiles` | `register:temp:{email}`, `otp:challenge:*`, `rl:otp:{email}` | S1 |
| AUTH-02 Đăng nhập | `POST /auth/sessions` (2.5) · `POST /auth/otp-challenges` purpose `LOGIN`, `ACCOUNT_UNLOCK` (2.2) · verify (2.4) · `POST /auth/sessions/refresh` (2.6) | `users`, `refresh_tokens`, `system_logs` | `rl:login:{ip}`, `auth:locked_by:{userId}` | S1 |
| AUTH-11 Đăng xuất & thiết bị | `DELETE /auth/sessions/current` (2.7) · `GET /me/sessions` · `DELETE /me/sessions/{id}` · `DELETE /me/sessions?scope=others` (3.6) | `refresh_tokens` | `auth:denylist:{jti}`, `auth:revoked_before:{userId}` | S1 |
| AUTH-04 Quên mật khẩu | `POST /auth/otp-challenges` purpose `PASSWORD_RESET` · verify → `reset_token` · `POST /auth/password-resets` (2.8) | `users`, `refresh_tokens` | `auth:reset:{tokenHash}` (10 phút), `auth:pwreset_lock:{email}` (30 phút) | S1 |
| AUTH-06 Đổi / thiết lập mật khẩu | `POST /me/password/verify` · `PUT /me/password` 🔐 `PASSWORD_CHANGE` (3.4) · `POST /me/otp-challenges` (+resend) (3.5) | `users`, `refresh_tokens` | `rl:pwverify:{userId}` | S1 |
| AUTH-08 Hồ sơ | `GET /me` (3.1) · `GET/PATCH /me/profile` (3.2) · `POST /me/email-change-requests` (+`/{id}/confirm`) · `POST /uploads/presigned-urls` purpose `AVATAR` (4.1) | `customer_profiles`, `users` | `otp:challenge:*` | S2 |
| AUTH-09 Sổ địa chỉ | `GET/POST /me/addresses` · `PUT/PATCH/DELETE /me/addresses/{id}` (3.8) · `GET /public/regions/**` (3.9) | `addresses`, `customer_profiles` | `region:provinces`, `region:districts:{pid}`, `region:wards:{did}` (24 h) | S2 |
| AUTH-03 Google / Apple | `GET /auth/oauth/{provider}/authorize` (2.9) · `POST /auth/oauth/{provider}/token` (2.10) · `GET /me/login-methods` · `POST/DELETE /me/identities/{provider}` (3.3) | `users`, `customer_profiles`, `refresh_tokens` | `oauth:state:{state}` (10 phút) | S7 |
| AUTH-10 Xóa tài khoản | `POST /me/otp-challenges` `ACCOUNT_DELETION` (kiểm điều kiện trước) · `PUT /me/deletion-request` 🔐 · `DELETE /me/deletion-request` (3.7) · Cron ẩn danh hằng ngày (LLD 10) | `users`, `customer_profiles`, `addresses`, `refresh_tokens`, `system_logs` | — | S7 |
| (ADM-01) Kích hoạt nhân viên | `POST /auth/staff-activations` (2.11) | `users`, `admin_profiles` | — | theo plan ADMIN |

**Mã lỗi cần có trong `ErrorCode`** (API_SPEC 15): `GNE-SEC-4010, 4030, 4031` · `GNE-AUTH-4001, 4011, 4031, 4032, 4091, 4092, 4093, 4094, 4095, 4096, 4097, 4101, 4102, 4103, 4221, 4222, 4223, 4224, 4230` · `GNE-USR-4091, 4092` · `GNE-MINIO-5003` · đã có `GNE-VAL-4220`, `GNE-RAT-4290`, `GNE-SYS-5030`. Đối chiếu với `web/src/shared/api/error-codes.ts` và khóa `errors.GNE-*` ở `vi.json` / `en.json`.

---

## 5. Nhánh, commit, PR

- Nhánh tích hợp của phân hệ: **`feature/auth`** (đã có, tách từ `develop`, chứa plan này). Mỗi nhóm task một nhánh con tách từ `feature/auth`, PR vào `feature/auth` (≤ ~400 dòng / PR); hết mỗi sprint mở PR `feature/auth` → `develop` (squash). Trước mỗi nhánh con: `git switch feature/auth && git pull && git merge develop`.
  - `feature/auth-a01-common` — A01 · `feature/auth-a02-migration` — A02 · `feature/auth-a03-domain` — A03 · `feature/auth-a04-security` — A04
  - `feature/auth-a05-otp` … `feature/auth-a10-facade` — A05–A10 (S1)
  - `feature/auth-a11-profile`, `feature/auth-a12-address` — A11–A13 (S2)
  - `feature/auth-a14-oauth-deletion` — A14 (S7)
  - `feature/web-auth-integration` (tách từ `develop` sau khi S1 merge) — A15
- Chưa có mã Jira → tên nhánh và commit **bỏ** `GNE-<số>` (giống tiền lệ `implement-entity.md`); khi có mã thì thêm `` vào cuối commit.
- Commit: `<type>(<scope>): <subject>`, scope dùng: `common`, `db`, `auth`, `admin`, `web`, `deploy`; tài liệu dùng `docs(auth): …`. Migration đi cùng commit dùng nó; chỉ migration → `feat(db): …`.
- Mỗi nhóm kết thúc bằng task `COMMIT` sau khi qua cổng mục 6. Không `--no-verify`.

---

## 6. Cổng kiểm tra trước mỗi task `COMMIT`

1. `cd server-spring && ./mvnw verify` — unit + IT (Testcontainers) + ArchUnit xanh.
2. Không có lớp nào ngoài `auth` import `com.genie.auth..domain|infra|api` (ArchUnit tự kiểm).
3. Swagger UI `http://localhost:8080/swagger-ui.html` hiện endpoint mới, có `UC-AUTH-xx` trong summary.
4. Thử tay bằng `scripts/http/auth.http` (tạo ở A07) với Mailpit `http://localhost:8025` để lấy OTP.
5. Nếu đụng `web/`: `cd web && npm run verify`.
6. Tick task ở file này, ghi mã commit vào Nhật ký (mục 10).

---

## 7. Danh sách task theo giai đoạn

### A00 — Chuẩn bị & chốt quyết định
- [ ] A00.01 Chủ dự án chốt D2–D22 (mục 2; D1 đã đóng); quyết định nào khác đề xuất thì sửa lại plan trước khi làm.
- [ ] A00.02 `feature/auth` cập nhật từ `develop` (đã có loạt entity); `docker compose -f deploy/docker-compose.local.yml --env-file deploy/.env up -d`; `./mvnw verify` xanh (gồm `SchemaValidationIT`, `ModuleBoundaryTest`) trước khi viết dòng nào.
- [ ] A00.03 Viết ADR `docs/adr/0007-auth-migration-va-cau-truc-module.md` (D2, D4, D6–D9, D20–D22, `V14`, feature `otp/`, `User` ở `auth/profile`) và `0008-common-mail-va-cong-dao-nguoc.md` (D12–D15); cập nhật `docs/adr/README.md`.
- [ ] A00.04 Sửa tài liệu nguồn trong `"HOÀN CHỈNH "`: ENTITY.md 1.1 (`deletionRequestedAt`, `anonymize()` — D7, D19), 1.7 (`familyId`, `lastUsedAt` — D6), Phụ lục A `AuditAction` (D20), 13.1 index (D22); ghi vào mục 6 `implement-entity.md` là các lệch được chủ động bổ sung sau Phase 12; LUU_TRU 4.2 (khóa Redis D4, D8, `auth:locked_by`, `auth:reset`, `auth:pwreset_lock`, `region:*`); UC-AUTH-11 bước 2 (D5); API_SPEC 2.1 câu `UNVERIFIED` (D2). Ghi lịch sử phiên bản ở mỗi file.
- [ ] A00.04b **COMMIT** `docs(auth): record auth decisions in adr 0007 and 0008`

### A01 — Nền `common` mà AUTH cần
- [ ] A01.01 `pom.xml`: thêm `org.bouncycastle:bcprov-jdk18on`, `spring-boot-starter-thymeleaf`; test: `org.testcontainers:testcontainers` (Redis qua `GenericContainer("redis:7-alpine")`).
- [ ] A01.02 `ErrorResponse` thêm `details` (`Map<String,Object>`, `null` khi không có); `BusinessException(ErrorCode, message, details)`; `GlobalExceptionHandler`: `request_id` lấy từ traceId Micrometer (`Tracer.currentSpan()`), map `HttpMessageNotReadableException` → 422, `ConstraintViolationException` → 422, `ObjectOptimisticLockingFailureException` → 409, ngoại lệ lạ → 500 `GNE-SYS-5000` (không lộ stack trace).
- [ ] A01.03 `ErrorCode`: thêm toàn bộ mã ở mục 4. Message mặc định tiếng Việt; client dịch qua `errors.GNE-*`.
- [ ] A01.04 `RateLimiter` (Redis `INCR` + `EXPIRE` — LLD 11.2): `check(key, limit, window)` ném `TooManyRequestsException` → `429 GNE-RAT-4290` + header `Retry-After` (TTL còn lại). Redis lỗi ở rate limit → **cho qua** + log WARN (khác idempotency); ghi rõ trong Javadoc.
- [ ] A01.05 `ClientInfo` (resolver): IP thật từ `X-Forwarded-For` / `CF-Connecting-IP` chỉ khi request đi qua proxy tin cậy (cấu hình `genie.security.trusted-proxies`), User-Agent cắt 500 ký tự.
- [ ] A01.06 `common/mail` (D12): `MailSender.send(to, MailTemplate, Map<String,Object> vars, Locale)`; `SmtpMailSender` dùng Thymeleaf + `JavaMailSender`, timeout 10 s, `@Retryable` 3 lần (TS 10.2); `MailTemplate` = `OTP, PASSWORD_CHANGED, NEW_DEVICE_LOGIN, EMAIL_CHANGED, DELETION_SCHEDULED`. Viết 4 mẫu HTML còn thiếu (vi + en, chung khung `otp.html`). Test: `FakeMailSender` ghi lại thư để IT đọc OTP.
- [ ] A01.07 `AuditLogger` (bảng `system_logs`, entity `SystemLog`, `SystemLogRepository` **đã có**): `log(AuditAction, AuditActor actor, String referenceId, Map<String,Object> details, ClientInfo client, boolean success)` → `SystemLog.of(...)` (chụp `userEmail`, `userRole`; job nền dùng `system@genie.vn` / `SYSTEM`), `details` serialize JSON bằng `ObjectMapper`, ghi `userAgent`. Ghi trong **cùng transaction** của use case (REQUIRED); riêng `LOGIN_FAILED` / `ACCESS_DENIED` dùng `REQUIRES_NEW` để không mất khi transaction chính rollback. Giá trị enum mới của D20 thêm ở A02.
- [ ] A01.08 `support/`: `IntegrationTestBase` — `@SpringBootTest` + Testcontainers theo mẫu `SchemaValidationIT` (`TimeZone UTC`, `@ServiceConnection` PG 16; Redis `GenericContainer("redis:7-alpine")` + `@DynamicPropertySource`), container dùng chung cả lớp test (static), `TRUNCATE … RESTART IDENTITY CASCADE` các bảng nhóm 1 + `system_logs` và `FLUSHALL` Redis giữa test; `FixedClock` (`MutableClock` để tua thời gian), `FakeMailSender`, `TestDataBuilder.user()…`.
- [ ] A01.09 Test: `GlobalExceptionHandlerTest`, `RateLimiterIT`, `SmtpMailSenderTest` (render đủ biến).
- [ ] A01.10 **COMMIT** `feat(common): add error details, rate limiter, mail sender and audit logger`

### A02 — Bổ sung CSDL cho AUTH (`V14`, bootstrap, seed)
> `V1` (8 bảng nhóm 1) và `V12` (`system_logs`) **đã có và đã merge** — không sửa. Chỉ thêm `V14__auth_additions.sql`, điền `V13`, `R__seed_demo`.
- [ ] A02.01 `V14__auth_additions.sql` — thứ tự an toàn (migration chỉ tiến, tương thích ngược ≥ 1 phiên bản — TS 15):
  1. `refresh_tokens`: `ADD COLUMN family_id BIGINT`, `ADD COLUMN last_used_at TIMESTAMPTZ`; `UPDATE refresh_tokens SET family_id = id WHERE family_id IS NULL`; `ALTER COLUMN family_id SET NOT NULL` (D6).
  2. `users`: `ADD COLUMN deletion_requested_at TIMESTAMPTZ` (D7) — hoặc để `V15` ở S7 nếu muốn giữ `V14` nhỏ.
  3. Index (D21, D22): `uk_users_email_lower` (UNIQUE `lower(email)`), `idx_refresh_tokens_user_active`, `idx_refresh_tokens_family`, `idx_addresses_user`, `idx_users_deletion_requested_at (deletion_requested_at) WHERE deletion_requested_at IS NOT NULL`.
  4. `system_logs`: `DROP CONSTRAINT ck_system_logs_action_type` → `ADD CONSTRAINT` cùng tên với **toàn bộ** giá trị cũ (chép từ `V12`) + 8 giá trị D20.
- [ ] A02.02 Đồng bộ Java với `V14`: `RefreshToken` thêm `familyId` (`@Column(name = "family_id", nullable = false)`), `lastUsedAt`; `User` thêm `deletionRequestedAt`; `AuditAction` thêm 8 giá trị D20. Chạy `SchemaValidationIT` — phải xanh.
- [ ] A02.03 `V13__bootstrap_admin.sql`: `users` (ADMIN, ACTIVE, `email_verified_at = now()`, `created_at/updated_at = now()`, `version = 0`) + `admin_profiles` (đúng cột của `V1` — tra entity `AdminProfile`) từ placeholder `${bootstrap_admin_email}`, `${bootstrap_admin_password_hash}`; bỏ qua khi hash rỗng hoặc email đã tồn tại (`DO $$ … $$`). Ghi cách sinh hash vào `deploy/.env.example` (A02.05).
- [ ] A02.04 `R__seed_demo.sql` (chỉ profile `local`/`demo`): tài khoản trùng mock web — `mai.tran@gmail.com` / `Genie@123` (CUSTOMER, có `customer_profiles` + 2 địa chỉ HCM có mã GHN thật), admin `yen.dh@genie.example`, 3 nhân viên (MODERATOR, FINANCE_OFFICER, CS_SPECIALIST — `admin_profiles` + `staff_permissions` theo `StaffTitle.defaultPermissions()`). Dùng `ON CONFLICT DO NOTHING` (file `R__` chạy lại khi checksum đổi). Hash Argon2 sinh sẵn, ghi mật khẩu trong comment. Chọn một cách giới hạn theo profile: `spring.flyway.locations` của profile `local` thêm `classpath:db/seed` (chuyển file sang đó) **hoặc** placeholder `${seed_demo}` — ghi vào README. Shop TechZone của `mai.tran` (role VENDOR, `vendor_profiles` VERIFIED) để plan VENDOR thêm — ghi chú trong file.
- [ ] A02.05 `scripts/hash-password.sh` (gọi lớp `main` trong test / `jshell` với `Argon2PasswordEncoder` theo D10) sinh hash cho bootstrap admin / seed.
- [ ] A02.06 Test: `SchemaValidationIT` xanh sau `V14`; `BootstrapAdminIT` (placeholder có hash → có 1 ADMIN; rỗng → không có).
- [ ] A02.07 **COMMIT** `feat(db): add auth session columns, indexes and audit actions` · `feat(db): seed bootstrap admin and demo accounts`

### A03 — Bổ sung domain AUTH (entity đã có)
> Entity, enum, repository đã sinh 1-1 từ ENTITY.md. A03 chỉ **thêm** phương thức nghiệp vụ và query; mỗi phương thức mới ghi vào ENTITY.md Phụ lục E (hoặc mục 6 `implement-entity.md`) để tài liệu không lệch code.
- [ ] A03.01 `User` (`auth/profile/domain`) — giữ nguyên 7 phương thức ENTITY; sửa `anonymize()` xóa cả `oauthProvider` (D19); thêm `hasPassword()`, `changePassword(hash)`, `verifyEmail(now)`, `changeEmail(newEmail, now)`, `linkIdentity(provider, sub)` (đã gắn provider → `IllegalStateException`), `unlinkIdentity()` (không có mật khẩu → `IllegalStateException`, service map `GNE-AUTH-4093`), `lockFor(Duration, now)` (khóa do sai OTP), `clearLock()`, `requestDeletion(now)`, `cancelDeletion()`, `isPendingDeletion()`. Email luôn chuẩn hóa chữ thường trong `changeEmail` / factory `User.newCustomer(...)` (D21).
- [ ] A03.02 `User` có `@OneToMany addresses`, `refreshTokens` (chỉ đọc, LAZY mặc định) — **không** dùng trong service (tránh tải cả danh sách); truy vấn qua repository. Ghi Javadoc.
- [ ] A03.03 `CustomerProfile`: thêm `updateProfile(fullName, phone, gender, dob, bio, language)`, `changeAvatar(url)`; factory `CustomerProfile.create(User user, fullName, phone, language)` gán **cả** `userId` và `user` (quan hệ `@OneToOne(optional = false)` chỉ đọc) — có test lưu cùng transaction với `User` mới.
- [ ] A03.04 `Address`: thêm `update(...)`, `unmarkDefault()`; giữ `markAsDefault`, `softDelete`.
- [ ] A03.05 `RefreshToken`: thêm `touch(now)` (cập nhật `lastUsedAt`), factory `issue(userId, familyId, hash, deviceInfo, ip, expiresAt)` — **cắt `deviceInfo` còn 255 ký tự** (cột `VARCHAR(255) NOT NULL`, User-Agent thường dài hơn), `ip` không null (thiếu → `"0.0.0.0"`); token gốc của phiên: lưu xong rồi gán `familyId = id`. Hằng `RevokeReason`: `USER_LOGOUT, PASSWORD_CHANGED, ROTATED, REUSE_DETECTED, SUSPENDED, ACCOUNT_DELETED, ADMIN_REVOKED` (cột `revoked_reason` là `VARCHAR(100)` tự do).
- [ ] A03.06 Query repository (đang rỗng): `UserRepository.findByEmail` (email đã chuẩn hóa), `existsByEmail`, `findByOauthProviderAndOauthProviderId`, `findDueForAnonymization(cutoff, limit)` (native `FOR UPDATE SKIP LOCKED`); `CustomerProfileRepository.findByUserId`, `findByUserIdForUpdate` (`@Lock(PESSIMISTIC_WRITE)`); `RefreshTokenRepository.findByTokenHash`, `findByTokenHashForUpdate`, `revokeAllOfUser(userId, reason, now)`, `revokeFamily(familyId, reason, now)`, `revokeOtherFamilies(userId, keepFamilyId, reason, now)` (`@Modifying` JPQL), `findActiveSessions(userId, now)`; `AddressRepository.findByUserIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc`, `findByIdAndUserId`, `clearDefault(userId)`.
- [ ] A03.07 Unit test phương thức entity cũ + mới (khóa 5 lần → 15 phút, hết khóa, đình chỉ hết hạn, ẩn danh xóa cả provider, link / unlink, cắt `deviceInfo`); `AuthRepositoriesIT` cho các query `@Modifying` / `FOR UPDATE`.
- [ ] A03.08 **COMMIT** `feat(auth): add account domain methods and repository queries`

### A04 — Bảo mật lõi
- [ ] A04.01 `PasswordHasher` (Argon2id D10) + `PasswordPolicy` (≥ 8, hoa, thường, số, ký tự đặc biệt; ≤ 72 ký tự) + annotation Bean Validation `@StrongPassword` (lỗi `422 GNE-VAL-4220`, `errors[].code = PASSWORD_TOO_WEAK`).
- [ ] A04.02 `JwtService` (Nimbus từ `oauth2-resource-server`, HS256, khóa `genie.security.jwt-secret` ≥ 256 bit — kiểm khi khởi động): claim `sub` (userId), `role`, `sid` (family_id), `jti`, `iat`, `exp` (15 phút). Không đưa email / quyền vào token (quyền nạp lại mỗi request qua `AuthorityProvider`, cache 60 s theo userId trong bộ nhớ).
- [ ] A04.03 `JwtAuthFilter`: đọc Bearer → kiểm chữ ký, hạn, `auth:denylist:{jti}`, `auth:revoked_before:{userId}` (D8) → `AuthUser(id, role, sid, jti)` + authorities (`ROLE_<role>` + mã quyền). Lỗi → để `RestAuthEntryPoint` trả `401 GNE-SEC-4010`.
- [ ] A04.04 `SecurityConfig` (implement 4.1): stateless, CSRF tắt (refresh cookie `SameSite=Strict`), CORS `genie.security.cors-origins` (`allowCredentials=true`), `RoleHierarchy` `ROLE_ADMIN > ROLE_STAFF`, `ROLE_VENDOR > ROLE_CUSTOMER`, `@EnableMethodSecurity`; ma trận: `/auth/**` permitAll **trừ** `DELETE /auth/sessions/current` (authenticated); `/public/**`, webhook permitAll; `/internal/**` qua `InternalTokenFilter`; `/seller/**` `VENDOR`; `/admin/**` `STAFF`; còn lại authenticated. `RestAccessDeniedHandler` → `403 GNE-SEC-4030` + `AuditLogger ACCESS_DENIED`.
- [ ] A04.05 `CurrentUser` — `@AuthenticationPrincipal AuthUser` dùng ở controller; service nhận `userId` tường minh (không đọc SecurityContext trong domain).
- [ ] A04.06 `AuthorityProvider` port (`auth/profile/application`) + `DefaultAuthorityProvider` (`@ConditionalOnMissingBean`) + `StaffPermissionAuthorityProvider` ở `admin/rbac/application` đọc `StaffPermission` còn hiệu lực (`StaffPermissionRepository.findByStaffUserId…` + `isActive()`); ADMIN = mọi `StaffPermissionEnum` (D14). Scope commit `admin` cho phần này.
- [ ] A04.07 `RefreshCookie`: tạo / xóa `genie_rt` — `HttpOnly; SameSite=Strict; Path=/api/v1/auth; Max-Age=604800`; `Secure` theo `genie.security.cookie-secure` (false ở local http, true ở prod).
- [ ] A04.08 Cấu hình: `application.yml` thêm `genie.security.{jwt-secret, cookie-secure, cors-origins, trusted-proxies, otp-pepper}`; `.env.example` gốc + `deploy/.env.example` thêm `OTP_PEPPER`, `CORS_ALLOWED_ORIGINS`, `AUTH_COOKIE_SECURE`.
- [ ] A04.09 Test: `JwtServiceTest` (hết hạn, sai chữ ký, khóa ngắn bị từ chối khởi động), `SecurityConfigIT` (401/403 đúng định dạng, CORS preflight có credentials), `PasswordPolicyTest`.
- [ ] A04.10 **COMMIT** `feat(common): add jwt security, role hierarchy and argon2 password hashing`

### A05 — OTP dùng chung & step-up (nhánh `feature/auth-a05-otp` …)
- [ ] A05.01 `OtpPurpose` = 11 giá trị `$OtpChallenge.purpose` (API 1). `OtpChallengeService.issue(purpose, email, userId?)`: rate limit `rl:otp:{email}` 1/60 s + `rl:otp:hour:{email}` 5/giờ; hủy challenge cũ cùng `otp:active:{purpose}:{email}`; mã 6 số `SecureRandom`; lưu `otp:challenge:{id}` = `{purpose, email, user_id, code_hash = SHA-256(code + pepper), attempts: 0, resend_count}` TTL 300 s; gửi `MailTemplate.OTP` (đồng bộ — gửi lỗi → `503`, xóa challenge); trả `$OtpChallenge` (`email_masked` dạng `tr****@gmail.com`, `expires_in`, `resend_after = 60`).
- [ ] A05.02 `verify(challengeId, code, expectedPurpose)`: không có khóa → `410 GNE-AUTH-4101`; sai purpose → 410 (coi như không tồn tại); sai mã → `attempts++`, `422 GNE-AUTH-4222` `details.remaining_attempts`; lần thứ 5 → xóa challenge, `422 GNE-AUTH-4221` + callback theo purpose (khóa đăng nhập / khóa khôi phục / hủy đăng ký); đúng → xóa challenge (dùng một lần) trả `OtpContext`. So sánh bằng `MessageDigest.isEqual`. Thao tác đếm dùng `HINCRBY` để an toàn khi 2 request song song.
- [ ] A05.03 `resend(challengeId)`: kiểm 60 s + 5/giờ → mã mới, `expires_in` đặt lại 300.
- [ ] A05.04 Step-up: `@RequireStepUp(OtpPurpose)` + `StepUpInterceptor` (HandlerInterceptor) đọc `X-OTP-Challenge-Id`, `X-OTP-Code`, kiểm challenge thuộc `userId` hiện tại + đúng purpose; thiếu header → `422 GNE-AUTH-4222`. `OtpFacade.verifyStepUp(userId, purpose, id, code)` cho `payment` / `vendor` dùng khi không đi qua controller.
- [ ] A05.05 `MeOtpController`: `POST /me/otp-challenges` (gọi `AccountDeletionGuard` khi purpose `ACCOUNT_DELETION` — A14), `POST /me/otp-challenges/{id}/resend`. Purpose `WITHDRAWAL`, `PAYOUT_ACCOUNT_*` cho phép nhưng điều kiện trước khi gửi do module `payment` / `vendor` cài qua cổng `StepUpPrecondition` (rỗng = cho qua).
- [ ] A05.06 Test: `OtpChallengeServiceTest` (FixedClock), `StepUpOtpIT` (sai 5 lần, hết hạn, gửi lại sớm → 429 có `Retry-After`, challenge của user khác → từ chối, hai request đúng đồng thời chỉ một qua).
- [ ] A05.07 **COMMIT** `feat(auth): add shared otp challenges and step-up verification`

### A06 — UC-AUTH-01 Đăng ký
- [ ] A06.01 `POST /auth/registrations` (`RegistrationRequest` record: `full_name`, `email` (chuẩn hóa trim + lowercase), `phone_number` `^0\d{9,10}$`, `password` `@StrongPassword`, `accept_terms` `@AssertTrue`). Email đã có user (mọi trạng thái trừ `ANONYMIZED`) → `409 GNE-AUTH-4091`. Lưu `register:temp:{email}` = `{full_name, phone, password_hash}` TTL 15 phút (D3) — băm **trước** khi lưu Redis; ghi đè nếu đăng ký lại. Phát OTP `REGISTER` → `202 $OtpChallenge`.
- [ ] A06.02 Verify `REGISTER` (trong `AuthOtpController` chung `POST /auth/otp-challenges/{id}/verify`): đọc temp (mất → `410 GNE-AUTH-4101`) → transaction: kiểm lại email chưa tồn tại (đua nhau → 409), tạo `User` (CUSTOMER, ACTIVE, `email_verified_at`), `CustomerProfile` (full_name, phone, `preferred_language` theo `Accept-Language`) → cấp phiên (A07.04) → `201 $AuthTokens` + cookie; xóa temp; `UserRegisteredEvent` (AFTER_COMMIT: sau này `promotion` tặng voucher chào mừng).
- [ ] A06.03 Sai 5 lần → xóa `register:temp` (bắt đầu lại).
- [ ] A06.04 `scripts/http/auth.http` (REST Client / IntelliJ HTTP): kịch bản đăng ký → lấy OTP từ Mailpit → verify.
- [ ] A06.05 Test `RegisterFlowIT`: thành công (có user + profile + refresh token, cookie đúng thuộc tính), email trùng, mật khẩu yếu (422 có `errors[]`), OTP hết hạn, sai 5 lần hủy, hai lần verify song song chỉ tạo 1 user.
- [ ] A06.06 **COMMIT** `feat(auth): register customer with email otp verification`

### A07 — UC-AUTH-02 Đăng nhập + phiên + UC-AUTH-11
- [ ] A07.01 `POST /auth/sessions` (`LoginService`, LLD 7 sequence): rate limit `rl:login:{ip}` 10/phút → tìm user (không có → băm giả để thời gian đồng đều, `401 GNE-AUTH-4011` không kèm `remaining_attempts`) → `reactivateIfSuspensionExpired(now)` → `SUSPENDED` → `403 GNE-AUTH-4031` `{reason, until}` → `ANONYMIZED/DELETED` → 401 như sai mật khẩu → `isLocked` → `423 GNE-AUTH-4230` `{locked_until, locked_by}` → tài khoản chỉ có Google (`password == null`) → 401 (giao diện gợi ý "Đăng nhập bằng Google") → sai mật khẩu: `recordFailedLogin`; vừa khóa → ghi `auth:locked_by = PASSWORD`, `AuditLogger ACCOUNT_LOCKED`, `AccountLockedEvent`, trả 423; chưa khóa → `401 {remaining_attempts}` + `LOGIN_FAILED`.
- [ ] A07.02 Thành công: `recordSuccessfulLogin` → cấp phiên → `201 $AuthTokens` + cookie; `AuditLogger LOGIN_SUCCESS` (ip, ua). Băm lại mật khẩu nếu tham số Argon2 cũ (`upgradeEncoding`).
- [ ] A07.03 `NewDeviceDetector`: nếu user chưa có refresh token (kể cả đã thu hồi, 90 ngày gần nhất) cùng "trình duyệt + hệ điều hành" (từ UA) **và** cùng /24 IP → `NewDeviceLoginEvent` → listener gửi email `NEW_DEVICE_LOGIN` (in-app khi có module notification). Lần đăng nhập đầu tiên sau đăng ký không báo.
- [ ] A07.04 `SessionService.open(user, clientInfo)`: tạo `RefreshToken` (raw = 48 byte URL-safe, lưu SHA-256; `family_id = id` của chính nó; `expires_at = +7 ngày`), phát access JWT `sid = family_id`. Dùng chung cho đăng ký, đăng nhập mật khẩu / OTP / Google, mở khóa.
- [ ] A07.05 `POST /auth/sessions/refresh` (LLD 11.2): đọc cookie (thiếu → 401 `GNE-SEC-4010`) → token không dùng được (đã thu hồi / hết hạn) → nếu `revoked_reason = ROTATED` (bị dùng lại) và đã quá 10 giây ân hạn (mục 9) thì `revokeAllOfUser(REUSE_DETECTED)` + `revoked_before` + log → 401 + xóa cookie; user không còn ACTIVE / bị khóa đình chỉ → thu hồi, 401 → hợp lệ: thu hồi `ROTATED`, tạo token mới **cùng `family_id`**, `last_used_at = now` → `200 $AuthTokens` **không** kèm `user` + cookie mới. Khóa dòng `FOR UPDATE` để hai tab refresh cùng lúc không bị coi là dùng lại (tab thứ hai nhận 401 → web đã có cơ chế chỉ refresh một lần / tab; ghi rủi ro mục 9).
- [ ] A07.06 `DELETE /auth/sessions/current`: thu hồi cả family theo `sid` (`USER_LOGOUT`), `auth:denylist:{jti}` TTL = thời gian còn lại của token, xóa cookie → `204`. Access token hết hạn nhưng còn cookie → vẫn thu hồi theo cookie (permit cho đường này, đọc cookie).
- [ ] A07.07 Đăng nhập OTP: `POST /auth/otp-challenges` purpose `LOGIN` — email không tồn tại / không ACTIVE vẫn `202` nhưng không gửi thư (chống dò; ghi quyết định); verify `LOGIN` → các kiểm như A07.01 (đình chỉ, khóa) → cấp phiên `200`. Sai 5 lần → `lockFor(15 phút)`, `locked_by = OTP`, `422 GNE-AUTH-4221 {locked_until}`.
- [ ] A07.08 Mở khóa qua email: purpose `ACCOUNT_UNLOCK` chỉ khi `isLocked` (không → `409 GNE-AUTH-4096`; SUSPENDED → `403 GNE-AUTH-4031`); verify → `clearLock`, xóa `auth:locked_by`, `ACCOUNT_REACTIVATED`, cấp phiên `200`.
- [ ] A07.09 Thiết bị (`MeSessionController`): `GET /me/sessions` — mỗi family còn hiệu lực lấy token mới nhất; `device_name` = "Windows PC · Chrome 126" từ `UserAgentParser` (regex đơn giản cho Chrome/Edge/Firefox/Safari/Opera + Windows/macOS/iOS/Android/Linux, không thêm thư viện), `device_type`, `location = null` (D6), `ip_address`, `last_active_at = coalesce(last_used_at, created_at)`, `is_current = (family == sid)`; sắp `is_current` trước rồi `last_active_at` giảm dần. `DELETE /me/sessions/{id}` (id là family, không thuộc user → 404; là phiên hiện tại → `409 GNE-AUTH-4097`) → thu hồi `USER_LOGOUT` + `SESSION_REVOKED`. `DELETE /me/sessions?scope=others` → chỉ thu hồi refresh token của mọi family khác (không đặt `revoked_before` vì sẽ đá luôn phiên hiện tại); access token của máy khác tự hết trong ≤ 15 phút — khớp LUU_TRU "Denylist… tối đa 15 phút".
- [ ] A07.10 `RefreshTokenCleanupJob` (`@Scheduled` 03:00 hằng ngày): xóa token hết hạn / thu hồi quá 30 ngày (giữ lịch sử đủ cho `NewDeviceDetector` thì để 90 ngày — chọn 90).
- [ ] A07.11 Test: `LoginLockoutIT` (4 lần sai còn 1, lần 5 → 423, hết 15 phút đăng nhập lại được, sai email và sai mật khẩu cùng mã, đình chỉ hết hạn tự mở), `RefreshRotationIT` (xoay vòng giữ family, dùng lại token cũ → thu hồi tất cả + access token cũ bị chặn nhờ `revoked_before`), `SessionDevicesIT`, `LoginOtpIT`, `AccountUnlockIT`, rate limit 11 lần/phút → 429.
- [ ] A07.12 **COMMIT** (tách 2 nếu > 400 dòng) `feat(auth): login with password or otp and rotate refresh tokens` · `feat(auth): list and revoke login sessions`

### A08 — UC-AUTH-04 Quên mật khẩu
- [ ] A08.01 `POST /auth/otp-challenges` purpose `PASSWORD_RESET`: đang bị khóa khôi phục (`auth:pwreset_lock:{email}`) → vẫn `202` không gửi; email không tồn tại / tài khoản chỉ Google → `202` không gửi (tài khoản chỉ Google dùng "Thiết lập mật khẩu" sau khi đăng nhập Google).
- [ ] A08.02 Verify `PASSWORD_RESET` → `reset_token` = `rst_` + 32 byte ngẫu nhiên, lưu `auth:reset:{sha256}` = `{user_id}` TTL 600 s → `200 {reset_token, expires_in: 600}`. Sai 5 lần → `auth:pwreset_lock:{email}` 30 phút, `422 GNE-AUTH-4221 {locked_until}`.
- [ ] A08.03 `POST /auth/password-resets`: token không có → `410 GNE-AUTH-4102`; mật khẩu mới trùng hiện tại (`matches`) → `422 GNE-AUTH-4223`; đổi mật khẩu, `clearLock()` (đổi mật khẩu mở khóa — ENTITY 1.1 c), thu hồi mọi refresh token `PASSWORD_CHANGED`, đặt `revoked_before`, xóa token reset (dùng một lần), `PASSWORD_RESET` audit, email `PASSWORD_CHANGED` (AFTER_COMMIT) → `204`.
- [ ] A08.04 Test `PasswordResetIT`: email lạ vẫn 202 và không có thư, luồng đầy đủ, token dùng lần 2 → 410, mật khẩu trùng, phiên cũ bị đá (refresh cũ → 401).
- [ ] A08.05 **COMMIT** `feat(auth): reset forgotten password with email otp`

### A09 — UC-AUTH-06 Đổi / thiết lập mật khẩu
- [ ] A09.01 `POST /me/password/verify` — rate limit `rl:pwverify:{userId}` 10/phút; trả `{valid}`; tài khoản không có mật khẩu → `{valid: false}`.
- [ ] A09.02 `PUT /me/password` `@RequireStepUp(PASSWORD_CHANGE)`: có mật khẩu → `current_password` bắt buộc, sai → `401 GNE-AUTH-4011`; không có mật khẩu (tài khoản Google) → bỏ qua `current_password` (thiết lập lần đầu); trùng → `422 GNE-AUTH-4223`; yếu → 422. Đổi, thu hồi mọi family **khác** `sid` (`PASSWORD_CHANGED`) — không đặt `revoked_before` để giữ phiên hiện tại; audit; email → `204`. Áp dụng mọi vai trò (Seller › Bảo mật, Admin › Tài khoản dùng chung endpoint).
- [ ] A09.03 Test `PasswordChangeIT`: thiếu OTP header → 422, OTP của purpose khác → từ chối, đổi xong phiên hiện tại còn sống còn phiên khác bị thu hồi, thiết lập lần đầu cho tài khoản `password = NULL`.
- [ ] A09.04 **COMMIT** `feat(auth): change or set password with step-up otp`

### A10 — Đóng S1
- [ ] A10.01 `UserFacade` cho module khác: `findSummary(userId)` (id, role, email, full_name, avatar), `isActive(userId)`, `promoteToVendor(userId)` (UC-VND-01 sẽ gọi; đổi role + thu hồi để token mới mang role mới), `suspend/reactivate` (UC-ADM-03 sẽ gọi; suspend → thu hồi mọi phiên `SUSPENDED` + `revoked_before`). Chỉ trả record DTO, không trả entity.
- [ ] A10.02 Kiểm `/v3/api-docs`: mọi endpoint AUTH có `@Operation(summary="UC-AUTH-xx …")`, mô tả lỗi; xuất `docs/openapi/openapi.json` (CI hoặc tay) để đối chiếu web.
- [ ] A10.03 Chạy thử với web (`PUBLIC_USE_MOCK=false`) cho đăng ký / đăng nhập / quên mật khẩu / đổi mật khẩu / thiết bị — ghi lỗi tích hợp vào A15.
- [ ] A10.04 Cập nhật `Quản Lý UseCase.xlsx`: AUTH-01, 02, 04, 06, 11 "Đã triển khai" (sau khi web nối xong A15.01–A15.04).
- [ ] A10.05 **COMMIT** `feat(auth): expose user facade for other modules` → mở PR, merge vào `develop`, tag theo kế hoạch `v0.1.0` khi đủ S1–S2.

### A11 — UC-AUTH-08 Hồ sơ (S2, nhánh `feature/auth-a11-profile`)
- [ ] A11.01 Tải tệp tối thiểu (common/storage, API 4.1): `ObjectStorage` port + `MinioObjectStorage` (presigned PUT 15 phút ký theo `PUBLIC_S3_URL`, `statObject` kiểm MIME / kích thước, `promote(tmpKey, finalKey)`); `POST /uploads/presigned-urls` — làm trước purpose `AVATAR` (avatar-assets, jpeg/png, 1 MB), các purpose khác để module sau bổ sung trong `UploadPurpose`. Khóa: `tmp/avatar-assets/user/{id}/{uuid}.{ext}` → `avatar-assets/user/{id}/{uuid}.{ext}`.
- [ ] A11.02 `GET /me` (`MeQueryService`, D13): user + profile + `login_methods {password, google}` + `shop` từ `AccountShopProvider` (null nếu không có cài đặt / chưa mở shop) + `counters` gộp từ mọi `AccountCounterContributor` (mặc định 0). STAFF/ADMIN: `full_name`, `avatar_url` lấy từ `admin_profiles` qua cổng `StaffProfileProvider` (admin cài; tạm đọc `users.email`).
- [ ] A11.03 `GET /me/profile`, `PATCH /me/profile` (chỉ cập nhật trường có gửi — dùng `JsonNullable` hoặc `Map` + whitelist; `full_name` rỗng → 422; `date_of_birth` ≤ hôm nay; `preferred_language` ∈ `vi, en`; `avatar_object_key` → `promote` + xóa ảnh cũ AFTER_COMMIT) → hồ sơ đầy đủ; `deletion_request` từ D7.
- [ ] A11.04 Đổi email: `POST /me/email-change-requests` — trùng email hiện tại → `422 GNE-AUTH-4224`; đã thuộc người khác → `409 GNE-AUTH-4094`; chưa có mật khẩu → `409 GNE-AUTH-4095`; OTP `EMAIL_CHANGE` gửi tới **email mới** (lưu `new_email` trong challenge). `POST /me/email-change-requests/{id}/confirm` → kiểm lại trùng (đua) → `changeEmail`, `email_verified_at = now`, audit `EMAIL_CHANGED`, email báo tới **địa chỉ cũ** → `200 {email, email_verified: true}`. Liên kết Google giữ nguyên.
- [ ] A11.05 Test `ProfileIT`, `EmailChangeIT`, `PresignedUploadIT` (MinIO Testcontainers `minio/minio` hoặc fake `ObjectStorage`).
- [ ] A11.06 **COMMIT** `feat(common): issue presigned upload urls for avatars` · `feat(auth): view and update profile and change login email`

### A12 — UC-AUTH-09 Sổ địa chỉ
- [ ] A12.01 `RegionDirectory` port (`provinces()`, `districts(provinceId)`, `wards(districtId)`, `resolve(provinceId, districtId, wardCode)` → tên); `GhnRegionDirectory` (GHN master-data `…/master-data/province|district|ward`, header `Token`, timeout 3 s) + cache Redis 24 h; `StaticRegionDirectory` đọc `resources/regions/*.json` khi thiếu `GHN_TOKEN` (`@ConditionalOnProperty`). Chụp JSON một lần bằng `scripts/fetch-ghn-regions.sh`.
- [ ] A12.02 `GET /public/regions/provinces`, `/provinces/{id}/districts`, `/districts/{id}/wards` — `Cache-Control: public, max-age=86400` + `ETag`.
- [ ] A12.03 `AddressService`: `GET /me/addresses` (mặc định đứng đầu, bỏ đã xóa), `POST` (đủ 3 cấp hợp lệ qua `resolve` — server tự điền tên tỉnh / quận / phường, không tin client; địa chỉ đầu tiên tự mặc định), `PUT` (sửa toàn bộ, không đổi được của người khác → 404), `PATCH {is_default:true}` (transaction: `clearDefault(userId)` → `markAsDefault` → `CustomerProfile.setDefaultAddress`), `DELETE` (mặc định → `409 GNE-USR-4092`; còn lại `softDelete`) → `204`. Khóa `SELECT … FOR UPDATE` trên profile khi đổi mặc định để hai request song song không ra hai địa chỉ mặc định.
- [ ] A12.04 `AddressFacade.snapshotForCheckout(userId, addressId)` → record đầy đủ tên + mã GHN (module `order` dùng khi tạo phiên checkout).
- [ ] A12.05 Test `AddressDefaultIT` (song song đặt mặc định, xóa mặc định, IDOR), `RegionControllerIT` (static directory).
- [ ] A12.06 (Có thể cắt) `GET /public/regions/reverse-geocode?lat&lng` — `NominatimGeocoder` (User-Agent riêng, ≤ 1 req/s, cache theo tọa độ làm tròn 4 số) → khớp tên với `RegionDirectory` bằng `NameNormalizer` (bỏ dấu, bỏ tiền tố "Phường/Quận/Tỉnh/TP."). Không khớp → trả tỉnh / quận khớp được, `ward = null`.
- [ ] A12.07 **COMMIT** `feat(auth): manage ghn-standard shipping addresses` · `feat(auth): serve administrative regions from ghn with cache`

### A13 — Cổng đảo ngược cho module khác (làm cùng A11)
- [ ] A13.01 Khai báo trong `auth`: `AccountShopProvider`, `AccountCounterContributor`, `AccountDeletionGuard`, `StepUpPrecondition`, `StaffProfileProvider`, `AuthorityProvider` — Javadoc ghi module nào phải cài khi làm (vendor, order, notification, promotion, social, support, payment, admin).
- [ ] A13.02 Thêm vào `CLAUDE.md` mục 9 (bẫy): "module X khi làm phải cài cổng Y của auth".
- [ ] A13.03 **COMMIT** gộp vào commit A11 / A12.

### A14 — UC-AUTH-03 Google / Apple + UC-AUTH-10 Xóa tài khoản (S7, nhánh `feature/auth-a14-oauth-deletion`)
- [ ] A14.01 `IdentityProviderVerifier` port: `authorizeUrl(state, nonce, codeChallenge, redirectUri)`, `exchange(code, codeVerifier, redirectUri) → IdTokenClaims {sub, email, email_verified, name, picture}`. `GoogleIdentityProvider`: endpoint từ discovery `accounts.google.com/.well-known/openid-configuration`, `NimbusJwtDecoder.withJwkSetUri` (cache JWKS), kiểm `iss`, `aud = GOOGLE_CLIENT_ID`, `exp`, `nonce`. `AppleIdentityProvider` sau cờ (D17): `client_secret` JWT ES256 tự ký, `response_mode=form_post` → web callback nhận POST (kiểm lại trang `oauth-callback.astro`).
- [ ] A14.02 `GET /auth/oauth/{provider}/authorize?redirect_uri=`: `redirect_uri` phải thuộc `genie.oauth.allowed-redirects` (chống open redirect) → sinh `state`, `nonce`, `code_verifier` (PKCE S256) → `oauth:state:{state}` = `{provider, nonce, code_verifier, redirect_uri, mode: LOGIN|LINK, user_id?}` TTL 10 phút → `302`.
- [ ] A14.03 `POST /auth/oauth/{provider}/token` (`OAuthLoginService`, UC-AUTH-03 bước 5): `state` không có / khác provider / khác `redirect_uri` → `400 GNE-AUTH-4001` (xóa state sau khi đọc — dùng một lần) → đổi `code` → tìm theo `(provider, sub)`: có → kiểm trạng thái như A07.01 → `LOGGED_IN`; không có, email trùng user: `email_verified = false` → `403 GNE-AUTH-4032`; user bị khóa / đình chỉ → `403 GNE-AUTH-4031`; còn lại `linkIdentity` → `LINKED` (audit `OAUTH_LINKED`); không có user → tạo `User` (password NULL, CUSTOMER, ACTIVE, verified) + `CustomerProfile` (tên, `avatar_url` = URL ảnh Google — lưu thẳng URL, ghi chú) → `CREATED`. Cấp phiên → `200 {result, tokens}` + cookie.
- [ ] A14.04 `GET /me/login-methods` (D16), `POST /me/identities/{provider}` (state `mode = LINK` + `user_id` khớp người gọi; `sub` đã thuộc user khác → `409 GNE-AUTH-4092`; email chưa xác minh → `403 GNE-AUTH-4032`; user đã gắn provider khác → 409 4092 kèm `details.reason`) → `201` + login-methods. `DELETE /me/identities/{provider}` — chưa có mật khẩu → `409 GNE-AUTH-4093`; gỡ → audit `OAUTH_UNLINKED` → `204`. Provider trong path chữ thường (`google`, `apple`).
- [ ] A14.05 Test `OAuthLinkingIT` với `FakeIdentityProviderVerifier` (không gọi Google thật): 3 kết quả, 4001 / 4031 / 4032 / 4092 / 4093, state dùng lại → 4001.
- [ ] A14.06 Xóa tài khoản (cần cột `deletion_requested_at` — nếu chưa thêm ở `V14` thì tạo `V15__account_deletion.sql` ở đây): `AccountDeletionGuard` gọi khi `POST /me/otp-challenges {purpose: ACCOUNT_DELETION}` — gộp lý do chặn từ mọi cài đặt → `409 GNE-USR-4091 {active_order_codes, …}` **trước khi** gửi mã. `PUT /me/deletion-request` `@RequireStepUp(ACCOUNT_DELETION)` → kiểm guard lần nữa → `requestDeletion(now)`, thu hồi mọi phiên (`ACCOUNT_DELETED`) + `revoked_before`, audit, email `DELETION_SCHEDULED` (hạn = +30 ngày) → `200 {requested_at, anonymize_at}`. Đăng nhập vẫn được trong 30 ngày (hồ sơ hiện băng đỏ). `DELETE /me/deletion-request` → `cancelDeletion` + audit → `204`.
- [ ] A14.07 `AnonymizeAccountsJob` (`@Scheduled` 02:30 hằng ngày, LLD 10): `findDueForAnonymization(now − 30 ngày)` theo lô 100 `FOR UPDATE SKIP LOCKED`, mỗi user một transaction: `User.anonymize()` (đã sửa ở A03.01), `CustomerProfile.anonymize(now)`, `softDelete` mọi địa chỉ, thu hồi token, audit `PII_ANONYMIZED`, `AccountAnonymizedEvent` (AFTER_COMMIT: xóa ảnh đại diện MinIO; `social` hiển thị "Người dùng đã xóa"). Không đụng `orders`, `payment_*`, `financial_ledger`.
- [ ] A14.08 Test `AccountDeletionIT` (guard chặn, OTP, hủy trong hạn) + `AnonymizeAccountsJobIT` (FixedClock tua 31 ngày, email đổi thành `deleted_user_{id}@anonymized.local`, đăng nhập lại → 401, email cũ đăng ký mới được).
- [ ] A14.09 **COMMIT** `feat(auth): sign in and link accounts with google` · `feat(auth): schedule account deletion and anonymize after 30 days`

### A15 — Nối web với API thật (nhánh `feature/web-auth-integration`, scope `web`)
- [ ] A15.01 `features/auth/infrastructure`: thêm `getMe()` (`GET /me`, DTO + mapper → `SessionUser`: `roles` theo hierarchy — `VENDOR` → `['CUSTOMER','VENDOR']`; `shop_id`, `shop_name`, `kyc_status` từ `shop`; `has_password` từ `login_methods.password`). Sau `login`, `verifyOtp` (phiên), `completeOAuth`: gọi `getMe()` rồi `setSession` (D18).
- [ ] A15.02 `shared/auth/session.ts` `refreshAccessToken()`: response refresh không có `user` → gọi `GET /me` bằng token mới; STAFF/ADMIN lấy quyền từ `GET /admin/me/permissions` khi module admin có, tạm thời ADMIN = 13 quyền (giữ hành vi mock). Sửa kiểu `RefreshResponse`.
- [ ] A15.03 `account.api.ts`: `POST/DELETE /me/identities/GOOGLE` → `google` (handoff mục 2); kiểm `confirmEmailChange` có trong bảng handoff.
- [ ] A15.04 Đối chiếu mã lỗi: `shared/api/error-codes.ts` + `errors.GNE-*` (vi, en) có đủ mã mục 4; màn tạm khóa đọc `details.locked_until`, `locked_by`; form đăng ký hiển thị `errors[]` theo trường.
- [ ] A15.05 Chạy local hai phía (web 4321, API 8080, cùng `localhost` nên cookie `SameSite=Strict` vẫn gửi; `AUTH_COOKIE_SECURE=false`); `CORS_ALLOWED_ORIGINS=http://localhost:4321`.
- [ ] A15.06 E2E: `tests/e2e/auth-real.spec.ts` chỉ chạy khi `E2E_REAL_API=1` (đăng ký → OTP lấy qua API Mailpit `GET /api/v1/message/…` → đăng nhập → đổi mật khẩu → đăng xuất); spec mock hiện có giữ nguyên.
- [ ] A15.07 Cập nhật `docs/web-backend-handoff.md` (đánh dấu nhóm `auth`, `account` đã nối, bỏ khỏi bảng mục 2 các dòng đã sửa).
- [ ] A15.08 `npm run verify` + **COMMIT** `feat(web): load session context from me endpoint after login` · `fix(web): use lowercase provider in identity endpoints`

### A16 — Hoàn thiện phân hệ
- [ ] A16.01 Bộ kiểm thử API Apidog / Postman cho AUTH: mỗi UC ≥ 1 kịch bản đúng + 1 lỗi (TS 14); lưu `docs/api-tests/auth.*.json`.
- [ ] A16.02 Bảo mật nhanh: không log body `/auth/**` (`logback` mask `password`, `otp`, `reset_token`); header `Cache-Control: no-store` cho mọi response `/auth/**` và `/me/**`; kiểm OWASP ZAP baseline trên `/auth/**` (không cảnh báo High).
- [ ] A16.03 k6 nhỏ: 50 VU đăng nhập + refresh — P95 < 500 ms (Argon2 tốn CPU: đo, chỉnh `m` nếu vượt ngân sách 2 GB / backend).
- [ ] A16.04 Cập nhật `CLAUDE.md` mục 2 (hiện trạng backend: module auth xong) và mục 9; `Quản Lý UseCase.xlsx` đủ 9 UC AUTH.
- [ ] A16.05 **COMMIT** `test(auth): add api test collection and load check for login` · `docs: update claude context after auth module`

---

## 8. Ngoài phạm vi plan này

- UC-ADM-01/02/03 (mời nhân viên, phân quyền, khóa người dùng): plan ADMIN — AUTH chỉ chuẩn bị `UserFacade.suspend/reactivate`, `AuthorityProvider`, `StaffProfileProvider`; endpoint `POST /auth/staff-activations` (API 2.11) làm cùng plan ADMIN vì cần bảng lời mời / `admin_profiles`.
- Thông báo in-app `NEW_DEVICE_LOGIN` (cần module notification) — AUTH chỉ phát sự kiện + gửi email.
- `GET /public/banks` (LLD 706 xếp chung nhóm) — thuộc tài khoản nhận tiền của shop (plan VENDOR / PAYMENT).
- GeoIP cho cột `location` thiết bị, MFA, đăng nhập bằng số điện thoại, nhiều identity / user (DATN).
- Cổng KYC `/seller/**` (`GNE-VND-4031`) — plan VENDOR.

## 9. Rủi ro & cách xử lý

| Rủi ro | Xử lý |
|---|---|
| Hai tab cùng refresh một lúc → tab sau dùng token vừa bị xoay → bị coi là đánh cắp, đăng xuất mọi nơi | Web đã gom refresh một lần / tab; thêm **ân hạn 10 giây**: token `ROTATED` dùng lại trong 10 s sau khi xoay thì trả 401 nhưng **không** thu hồi cả họ (ghi ADR 0007) |
| Argon2 m = 19 MiB × nhiều request đăng nhập đồng thời làm đầy RAM backend | Giới hạn song song bằng `Semaphore` (vd 8) quanh `matches`; rate limit 10/phút/IP; đo ở A16.03 |
| Email OTP vào spam / SMTP chậm → người dùng không nhận mã | Mailpit ở local; demo dùng Gmail SMTP có SPF; timeout 10 s + thử lại; giao diện có "Gửi lại" sau 60 s |
| `V14` đổi `CHECK` / thêm cột `NOT NULL` trên bảng đã có dữ liệu local | Thêm cột cho phép NULL → backfill → `SET NOT NULL` trong cùng file; `CHECK` mới chứa đủ giá trị cũ; chạy `SchemaValidationIT` + chạy thử trên DB local đã có dữ liệu seed trước khi merge |
| Lệch giữa entity Java và ENTITY.md sau khi thêm phương thức / cột cho AUTH | Mỗi bổ sung ghi vào ENTITY.md (A00.04) và mục 6 `implement-entity.md`; `SchemaValidationIT` chặn lệch cột |
| Dữ liệu tạm đăng ký ở Redis mất khi Redis khởi động lại | AOF `everysec` (đã cấu hình); mất thì người dùng đăng ký lại — chấp nhận |
| Google OAuth cần `redirect_uri` HTTPS khi demo | Dùng `http://localhost` khi dev (Google cho phép), Cloudflare Tunnel / domain thật khi demo |
| Module khác chưa có nên `GET /me` / xóa tài khoản thiếu dữ liệu | Cổng đảo ngược trả mặc định an toàn (bộ đếm 0); khi xóa tài khoản trước khi có module order → ghi rõ trong test và checklist demo |
| PR quá 400 dòng | Mỗi nhóm A0x có commit riêng → tách PR theo nhóm |

## 10. Nhật ký

| Ngày | Việc đã làm | Commit |
|---|---|---|
| 2026-11-14 | Lập plan AUTH | `aeeb2ae` |
| 2026-11-14 | Cập nhật plan theo loạt entity đã gộp (PR #28–#51): mục 0 hiện trạng; D1 đóng; thêm D20 (`AuditAction`), D21 (email chữ thường), D22 (index); A02 → `V14__auth_additions.sql` + điền `V13` / seed; A03 → chỉ bổ sung phương thức & query; `User` ở `auth/profile/domain`; `AuthorityProvider` đọc `StaffPermission` qua `admin/rbac/application`; nhánh con từ `feature/auth`, bỏ `GNE-<số>` khi chưa có Jira | |
