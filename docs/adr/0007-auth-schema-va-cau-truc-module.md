# 0007 — AUTH: bổ sung schema bằng V14 và cấu trúc module

- **Trạng thái:** Chấp nhận (plan `plan/2026-11-14/auth.md` D2–D9, D16–D22 — chủ dự án cho tiến hành theo đề xuất 2026-11-14)
- **Phạm vi:** `server-spring/` module `auth`, `common`

## Bối cảnh

Entity nhóm 1 đã dựng 1-1 theo ENTITY.md và merge (`V1…V12`). Khi lập plan AUTH thấy thiếu chỗ lưu cho màn
"Thiết bị đăng nhập", yêu cầu xóa tài khoản, hành động kiểm toán của AUTH; email `UNIQUE` phân biệt hoa thường;
không có index cho `refresh_tokens(user_id)`, `addresses(user_id)`.

## Quyết định

- Không sửa `V1…V12`. Thêm `V14__auth_additions.sql`:
  `refresh_tokens.family_id` (sequence `refresh_token_family_seq`, = `id` phiên ở `GET /me/sessions`, = claim `sid`),
  `refresh_tokens.last_used_at`, `users.deletion_requested_at`, `uk_users_email_lower`, 5 index, `CHECK` mới của
  `system_logs.action_type` (+8 giá trị: `PASSWORD_CHANGED, PASSWORD_RESET, EMAIL_CHANGED, OAUTH_LINKED, OAUTH_UNLINKED,
  SESSION_REVOKED, ACCOUNT_DELETION_REQUESTED, ACCOUNT_DELETION_CANCELLED`).
- `V13__bootstrap_admin.sql` và `R__seed_demo.sql` (đang là placeholder, chưa chạy ở môi trường chung) được điền nội dung.
  Seed chỉ chạy khi placeholder Flyway `seed_demo = true` (profile `local`, `test`).
- Hash Argon2id cho seed / bootstrap sinh bằng `scripts/hash-password.py` (Python thuần, RFC 9106, đã kiểm test vector).
- Đăng ký giữ dữ liệu tạm ở Redis (`register:temp:{email}`, 15 phút), chỉ tạo `users` khi OTP đúng; `UNVERIFIED` để dành.
- OTP lưu theo `otp:challenge:{challengeId}` (khớp API_SPEC), không theo email như LUU_TRU 4.2.
- Khóa tạm do sai mật khẩu / OTP dùng `users.locked_until`; trạng thái vẫn `ACTIVE` (không dùng `LOCKED`).
- Vô hiệu access token: `auth:denylist:{jti}` + `auth:revoked_before:{userId}`; Redis lỗi thì cho qua.
- `User.anonymize()` xóa cả `oauthProvider`. Entity `User` ở `auth/profile/domain`; feature mới `auth/otp`.

## Hệ quả

- ENTITY.md (1.1, 1.7, 13.1, Phụ lục A), LUU_TRU_DU_LIEU.md 4.2, API_SPEC 2.1, Usecase UC-AUTH-11 cần cập nhật theo (plan A00.04).
- Máy dev đã chạy Flyway với `V13` / `R__seed_demo` bản placeholder: chạy `flyway repair` hoặc xóa volume Postgres local.
- Một user chỉ gắn được một nhà cung cấp OAuth (Google **hoặc** Apple) — DATN tách bảng `user_identities`.
