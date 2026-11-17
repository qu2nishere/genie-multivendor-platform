package com.genie.auth.oauth.application;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.util.Arrays;

/**
 * Nhà cung cấp đăng nhập mạng xã hội (UC-AUTH-03). Giá trị {@link #id()} lưu ở {@code users.oauth_provider} và dùng trong
 * đường dẫn API ({@code /auth/oauth/google/…}, {@code /auth/oauth/zalo/…}, chữ thường). Apple thay bằng Zalo (plan D17).
 */
public enum OAuthProvider {

    /** OpenID Connect — {@code id_token} có email đã xác minh. */
    GOOGLE("google", "Google", true),
    /** OAuth v4 + Graph API — KHÔNG trả email: tài khoản mới phải nhập email và xác minh OTP (plan D23). */
    ZALO("zalo", "Zalo", false);

    private final String id;
    private final String label;
    private final boolean verifiedEmail;

    OAuthProvider(String id, String label, boolean verifiedEmail) {
        this.id = id;
        this.label = label;
        this.verifiedEmail = verifiedEmail;
    }

    public String id() {
        return id;
    }

    /** Tên hiển thị trong thông báo ("Đăng nhập bằng Zalo chưa được cấu hình."). */
    public String label() {
        return label;
    }

    /**
     * Nhà cung cấp có trả email ĐÃ XÁC MINH không. Không (Zalo) → không tự gắn theo email, tạo tài khoản mới phải qua bước
     * nhập email + OTP, liên kết thủ công phải nhập lại mật khẩu Genie.
     */
    public boolean providesVerifiedEmail() {
        return verifiedEmail;
    }

    /** {@code google} → GOOGLE, {@code zalo} → ZALO; không hỗ trợ → {@code 404}. */
    public static OAuthProvider fromPath(String value) {
        return Arrays.stream(values()).filter(p -> p.id.equalsIgnoreCase(value)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không hỗ trợ đăng nhập bằng " + value + "."));
    }
}
