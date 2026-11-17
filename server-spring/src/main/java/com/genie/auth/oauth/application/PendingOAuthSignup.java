package com.genie.auth.oauth.application;

/**
 * Lần đăng nhập bằng nhà cung cấp KHÔNG có email (Zalo) mà chưa có tài khoản Genie — chờ người dùng nhập email và xác
 * minh OTP (plan D23). Lưu Redis {@code oauth:signup:{token}} 15 phút, không ghi PostgreSQL trước khi xác minh xong.
 *
 * @param provider nhà cung cấp
 * @param subject  mã người dùng phía nhà cung cấp (đã đổi {@code code} thành công)
 * @param name     họ tên lấy từ nhà cung cấp (có thể null)
 * @param picture  URL ảnh đại diện (có thể null)
 * @param next     trang web quay về sau khi tạo tài khoản
 */
public record PendingOAuthSignup(OAuthProvider provider, String subject, String name, String picture, String next) {
}
