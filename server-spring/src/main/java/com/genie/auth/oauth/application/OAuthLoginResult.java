package com.genie.auth.oauth.application;

import com.genie.auth.login.application.AuthResult;

/**
 * Kết quả đăng nhập mạng xã hội (API_SPEC 2.10).
 *
 * @param result {@code LOGGED_IN} (đã gắn từ trước), {@code LINKED} (gắn vào tài khoản cùng email), {@code CREATED} (tài khoản mới)
 * @param auth   phiên vừa cấp + thông tin người dùng
 * @param next   trang web quay về (đã kiểm là đường dẫn nội bộ)
 */
public record OAuthLoginResult(Outcome result, AuthResult auth, String next) {

    public enum Outcome { LOGGED_IN, LINKED, CREATED }
}
