# 0008 — `common`: gửi email, nhật ký, bảo mật và cổng đảo ngược giữa module

- **Trạng thái:** Chấp nhận (plan `plan/2026-11-14/auth.md` D12–D15)
- **Phạm vi:** `server-spring/` — `common`, `auth`, `admin`

## Bối cảnh

ArchUnit cấm vòng phụ thuộc giữa các slice `com.genie.(*)` (gồm cả `common`). Filter JWT ở `common.security` cần quyền
STAFF (bảng của `admin`); `AuditLogger` cần email / vai trò (bảng của `auth`); `auth` gửi email còn `notification` lại
nghe sự kiện của `auth`.

## Quyết định

- Gửi email: cổng `common.mail.EmailSender` + `SmtpEmailSender` (Thymeleaf, `@Retryable` 3 lần, timeout 10 s).
  Đặt tên `EmailSender` để không trùng `org.springframework.mail.MailSender`.
- Cổng đảo ngược khai báo ở `common`, module nghiệp vụ cài đặt:
  `common.security.AuthorityProvider` ← `admin.rbac.application.StaffPermissionAuthorityProvider` (cache 60 s);
  `common.audit.ActorDirectory` ← `auth.profile.application.UserActorDirectory`.
- Cổng đảo ngược của `auth` cho module khác (GET /me, xóa tài khoản) khai báo trong `auth` khi làm A11 / A14.
- Kế thừa vai trò (VENDOR ⊃ CUSTOMER, ADMIN ⊃ STAFF) do `JwtAuthFilter` cấp sẵn authority thay cho `RoleHierarchy`.
- Filter bảo mật không đăng ký là bean (tránh Spring Boot tự thêm vào chuỗi servlet lần hai) — `SecurityConfig` tạo.

## Hệ quả

- `common` không import module nghiệp vụ nào; mọi module dùng `common.*` tự do.
- Đổi quyền nhân viên phải gọi `StaffPermissionAuthorityProvider.evict(userId)` (plan ADMIN).
