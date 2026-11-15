package com.genie.auth.oauth.application;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.util.Arrays;

/**
 * Nhà cung cấp đăng nhập mạng xã hội (UC-AUTH-03). Giá trị {@link #id()} lưu ở {@code users.oauth_provider} và dùng trong
 * đường dẫn API ({@code /auth/oauth/google/…}, chữ thường). Apple để sau (plan D17).
 */
public enum OAuthProvider {

    GOOGLE("google");

    private final String id;

    OAuthProvider(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** {@code google} → GOOGLE; không hỗ trợ → {@code 404}. */
    public static OAuthProvider fromPath(String value) {
        return Arrays.stream(values()).filter(p -> p.id.equalsIgnoreCase(value)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không hỗ trợ đăng nhập bằng " + value + "."));
    }
}
