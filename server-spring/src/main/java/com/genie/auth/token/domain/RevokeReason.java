package com.genie.auth.token.domain;

/**
 * Giá trị {@code refresh_tokens.revoked_reason} (cột VARCHAR(100) tự do — ENTITY.md 1.7 liệt kê USER_LOGOUT,
 * PASSWORD_CHANGED, ROTATED, SUSPICIOUS_REUSE, SUSPICIOUS; bổ sung theo plan AUTH A03.05).
 */
public final class RevokeReason {

    public static final String USER_LOGOUT = "USER_LOGOUT";
    public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String ROTATED = "ROTATED";
    /** Token đã xoay vòng bị dùng lại → nghi bị đánh cắp, thu hồi mọi phiên (LLD 11.2). */
    public static final String REUSE_DETECTED = "SUSPICIOUS_REUSE";
    public static final String SUSPENDED = "SUSPENDED";
    public static final String ACCOUNT_DELETED = "ACCOUNT_DELETED";
    public static final String ADMIN_REVOKED = "ADMIN_REVOKED";
    public static final String ROLE_CHANGED = "ROLE_CHANGED";

    private RevokeReason() {
    }
}
