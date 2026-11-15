package com.genie.auth.otp.application;

/**
 * Mục đích của một mã OTP — đúng danh sách {@code $OtpChallenge.purpose} (API_SPEC 1).
 * <ul>
 *   <li>Khách vãng lai ({@code /auth/otp-challenges}): REGISTER, LOGIN, PASSWORD_RESET, ACCOUNT_UNLOCK;</li>
 *   <li>Step-up khi đã đăng nhập ({@code /me/otp-challenges}, header {@code X-OTP-*} — API_SPEC 0.7): PASSWORD_CHANGE,
 *       ACCOUNT_DELETION, WITHDRAWAL, PAYOUT_ACCOUNT_UPDATE, PAYOUT_ACCOUNT_DELETE;</li>
 *   <li>Luồng riêng: EMAIL_CHANGE ({@code /me/email-change-requests}), SHOP_REGISTRATION (mục 9 — plan VENDOR).</li>
 * </ul>
 */
public enum OtpPurpose {
    REGISTER, LOGIN, PASSWORD_RESET, PASSWORD_CHANGE, ACCOUNT_DELETION, WITHDRAWAL,
    PAYOUT_ACCOUNT_UPDATE, PAYOUT_ACCOUNT_DELETE, SHOP_REGISTRATION, EMAIL_CHANGE, ACCOUNT_UNLOCK;

    /** Được xin qua {@code POST /auth/otp-challenges} (không cần đăng nhập). REGISTER đi qua {@code /auth/registrations}. */
    public boolean isGuestRequestable() {
        return this == LOGIN || this == PASSWORD_RESET || this == ACCOUNT_UNLOCK;
    }

    /** Được xin qua {@code POST /me/otp-challenges} để xác thực bước 2 (header X-OTP-*). */
    public boolean isStepUp() {
        return this == PASSWORD_CHANGE || this == ACCOUNT_DELETION || this == WITHDRAWAL
                || this == PAYOUT_ACCOUNT_UPDATE || this == PAYOUT_ACCOUNT_DELETE;
    }
}
