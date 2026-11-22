package com.genie.auth.otp.application;

/**
 * Mục đích của một mã OTP — đúng danh sách {@code $OtpChallenge.purpose} (API_SPEC 1).
 * <ul>
 *   <li>Khách vãng lai ({@code /auth/otp-challenges}): REGISTER, LOGIN, PASSWORD_RESET, ACCOUNT_UNLOCK;</li>
 *   <li>Step-up khi đã đăng nhập ({@code /me/otp-challenges}, header {@code X-OTP-*} — API_SPEC 0.7): PASSWORD_CHANGE,
 *       ACCOUNT_DELETION, WITHDRAWAL, PAYOUT_ACCOUNT_UPDATE, PAYOUT_ACCOUNT_DELETE;</li>
 *   <li>Luồng riêng: EMAIL_CHANGE ({@code /me/email-change-requests}), SHOP_REGISTRATION (mục 9 — plan VENDOR),
 *       OAUTH_SIGNUP ({@code /auth/oauth/{provider}/signup} — đăng ký bằng Zalo, xác minh email; plan D23).</li>
 * </ul>
 */
public enum OtpPurpose {
    REGISTER, LOGIN, PASSWORD_RESET, PASSWORD_CHANGE, ACCOUNT_DELETION, WITHDRAWAL,
    PAYOUT_ACCOUNT_UPDATE, PAYOUT_ACCOUNT_DELETE, SHOP_REGISTRATION, EMAIL_CHANGE, ACCOUNT_UNLOCK, OAUTH_SIGNUP,
    /** Nhân viên kích hoạt lời mời (UC-ADM-01, plan B1 D2) — chỉ xin qua {@code POST /auth/staff-activations/otp-challenges}. */
    STAFF_ACTIVATION;

    /** Được xin qua {@code POST /auth/otp-challenges} (không cần đăng nhập). REGISTER đi qua {@code /auth/registrations}. */
    public boolean isGuestRequestable() {
        return this == LOGIN || this == PASSWORD_RESET || this == ACCOUNT_UNLOCK;
    }

    /** Được xin qua {@code POST /me/otp-challenges} để xác thực bước 2 (header X-OTP-*). */
    public boolean isStepUp() {
        return this == PASSWORD_CHANGE || this == ACCOUNT_DELETION || this == WITHDRAWAL
                || this == PAYOUT_ACCOUNT_UPDATE || this == PAYOUT_ACCOUNT_DELETE;
    }

    /** Việc khách đang làm, chèn vào email OTP: "Bạn đang {action}." / "You are {action}." */
    public String action(boolean english) {
        return switch (this) {
            case REGISTER -> english ? "creating your Genie Shop account" : "tạo tài khoản Genie Shop";
            case LOGIN -> english ? "signing in to Genie Shop" : "đăng nhập vào Genie Shop";
            case PASSWORD_RESET -> english ? "resetting your password" : "đặt lại mật khẩu";
            case PASSWORD_CHANGE -> english ? "changing your password" : "đổi mật khẩu";
            case ACCOUNT_DELETION -> english ? "requesting to delete your account" : "yêu cầu xóa tài khoản";
            case WITHDRAWAL -> english ? "withdrawing money from your shop wallet" : "rút tiền từ ví shop";
            case PAYOUT_ACCOUNT_UPDATE -> english ? "updating your payout bank account" : "cập nhật tài khoản nhận tiền";
            case PAYOUT_ACCOUNT_DELETE -> english ? "removing your payout bank account" : "xóa tài khoản nhận tiền";
            case SHOP_REGISTRATION -> english ? "registering your shop on Genie" : "đăng ký mở shop trên Genie";
            case EMAIL_CHANGE -> english ? "changing your sign-in email" : "đổi email đăng nhập";
            case ACCOUNT_UNLOCK -> english ? "unlocking your account" : "mở khóa tài khoản";
            case OAUTH_SIGNUP -> english ? "creating your Genie Shop account with Zalo"
                    : "tạo tài khoản Genie Shop bằng Zalo";
            case STAFF_ACTIVATION -> english ? "activating your Genie Shop staff account"
                    : "kích hoạt tài khoản nhân viên Genie Shop";
        };
    }
}
