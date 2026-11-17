package com.genie.auth.oauth.application;

import com.genie.auth.login.application.AuthResult;

/**
 * Kết quả đăng nhập mạng xã hội (API_SPEC 2.10).
 *
 * @param result {@code LOGGED_IN} (đã gắn từ trước), {@code LINKED} (gắn vào tài khoản cùng email), {@code CREATED}
 *               (tài khoản mới), {@code SIGNUP_REQUIRED} (Zalo chưa có tài khoản — phải nhập email + OTP, chưa đăng nhập)
 * @param auth   phiên vừa cấp + thông tin người dùng ({@code null} khi {@code SIGNUP_REQUIRED})
 * @param next   trang web quay về (đã kiểm là đường dẫn nội bộ)
 * @param signup bước hoàn tất đăng ký (chỉ có khi {@code SIGNUP_REQUIRED})
 */
public record OAuthLoginResult(Outcome result, AuthResult auth, String next, SignupStep signup) {

    public enum Outcome { LOGGED_IN, LINKED, CREATED, SIGNUP_REQUIRED }

    /**
     * @param token     mã phiên đăng ký tạm — gửi kèm khi nhập email
     * @param provider  {@code ZALO}
     * @param name      tên lấy từ nhà cung cấp (hiện lời chào)
     * @param expiresIn số giây còn hiệu lực
     */
    public record SignupStep(String token, String provider, String name, long expiresIn) {
    }

    public static OAuthLoginResult signedIn(Outcome outcome, AuthResult auth, String next) {
        return new OAuthLoginResult(outcome, auth, next, null);
    }
}
