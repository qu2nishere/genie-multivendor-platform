package com.genie.common.mail;

import java.util.Locale;

/**
 * Mẫu email (Thymeleaf, {@code resources/templates/email/<file>.html}) và tiêu đề theo ngôn ngữ.
 * Biến dùng chung mọi mẫu: {@code lang} ({@code vi} / {@code en}), {@code appName}, {@code webUrl}.
 */
public enum MailTemplate {

    /** Biến: {@code otp}, {@code expiresMinutes}, {@code purpose} (mã OtpPurpose). */
    OTP("otp", "Mã xác thực Genie", "Your Genie verification code"),
    /** Biến: {@code changedAt}. */
    PASSWORD_CHANGED("password-changed", "Mật khẩu Genie Shop của bạn vừa được thay đổi", "Your Genie Shop password was changed"),
    /** Biến: {@code device}, {@code ip}, {@code loginAt}. */
    NEW_DEVICE_LOGIN("new-device", "Đăng nhập Genie từ thiết bị mới", "New sign-in to your Genie account"),
    /** Biến: {@code newEmail}. */
    EMAIL_CHANGED("email-changed", "Email đăng nhập Genie đã thay đổi", "Your Genie sign-in email was changed"),
    /** Biến: {@code anonymizeAt}. */
    DELETION_SCHEDULED("deletion-scheduled", "Yêu cầu xóa tài khoản Genie", "Your Genie account deletion request");

    private final String file;
    private final String subjectVi;
    private final String subjectEn;

    MailTemplate(String file, String subjectVi, String subjectEn) {
        this.file = file;
        this.subjectVi = subjectVi;
        this.subjectEn = subjectEn;
    }

    /** Tên mẫu cho Thymeleaf, ví dụ {@code email/otp}. */
    public String templateName() {
        return "email/" + file;
    }

    public String subject(Locale locale) {
        return locale != null && "en".equals(locale.getLanguage()) ? subjectEn : subjectVi;
    }
}
