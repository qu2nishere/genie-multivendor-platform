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

    /**
     * {@code provider}: {@code GOOGLE} | {@code ZALO}; {@code email}: email bên Google lúc liên kết (Zalo: null);
     * {@code displayName}: tên hiển thị bên nhà cung cấp lúc liên kết (có thể null).
     */
    public record Identity(String provider, String email, String displayName, OffsetDateTime linkedAt) {
    }
}
