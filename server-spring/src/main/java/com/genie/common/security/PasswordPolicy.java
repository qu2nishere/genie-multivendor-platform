package com.genie.common.security;

/**
 * Độ mạnh mật khẩu (UC-AUTH-01 bước 2, FR-AUTH-01): 8–128 ký tự, có chữ hoa, chữ thường, chữ số và ký tự đặc biệt.
 * Dùng ở Bean Validation ({@link StrongPassword}) và ở service khi nhận mật khẩu ngoài DTO.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    public static boolean isStrong(String password) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return false;
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else if (!Character.isWhitespace(c)) {
                special = true;
            }
        }
        return upper && lower && digit && special;
    }
}
