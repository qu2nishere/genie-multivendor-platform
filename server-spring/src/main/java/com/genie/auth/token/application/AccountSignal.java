package com.genie.auth.token.application;

/**
 * Tin trên kênh {@code /user/queue/account} (API_SPEC mục 13): {@code {"type": "SESSION_REVOKED", "reason": "SUSPENDED"}}.
 * {@code reason} là {@code RevokeReason} — {@code SUSPENDED} (bị khóa), {@code ACCOUNT_DELETED}, {@code PASSWORD_CHANGED},
 * {@code SUSPICIOUS_REUSE}… Client đăng xuất và báo theo lý do. Đổi quyền nhân viên không thu hồi phiên — module
 * {@code admin} gửi {@code {"type": "PERMISSIONS_CHANGED"}} trên cùng kênh (plan B1 D19).
 */
public record AccountSignal(String type, String reason) {

    public static final String SESSION_REVOKED = "SESSION_REVOKED";
}
