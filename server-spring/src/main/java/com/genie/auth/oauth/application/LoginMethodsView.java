package com.genie.auth.oauth.application;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * {@code GET /me/login-methods} (API_SPEC 3.3): email đăng nhập, có mật khẩu chưa, tài khoản mạng xã hội đã liên kết.
 * TLCN mỗi user tối đa một nhà cung cấp (plan D16).
 */
public record LoginMethodsView(String email, Password password, List<Identity> identities) {

    public record Password(boolean enabled) {
    }

    /** {@code provider}: {@code GOOGLE}; {@code email}: email bên Google lúc liên kết. */
    public record Identity(String provider, String email, OffsetDateTime linkedAt) {
    }
}
