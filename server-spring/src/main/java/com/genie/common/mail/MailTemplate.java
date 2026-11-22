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
    DELETION_SCHEDULED("deletion-scheduled", "Genie Shop đã nhận yêu cầu xóa tài khoản của bạn", "We received your Genie Shop account deletion request"),
    /** KYC đã duyệt (UC-VND-03/04). Biến: {@code name}, {@code storeName}. */
    KYC_APPROVED("kyc-approved", "Hồ sơ KYC của shop đã được duyệt", "Your shop KYC has been approved"),
    /** KYC bị từ chối. Biến: {@code name}, {@code storeName}, {@code reason}, {@code canRequestReview}. */
    KYC_REJECTED("kyc-rejected", "Hồ sơ KYC của shop chưa được duyệt", "Your shop KYC was not approved"),
    /** Yêu cầu bổ sung KYC. Biến: {@code name}, {@code storeName}, {@code documents}, {@code note}, {@code deadline}. */
    KYC_SUPPLEMENT("kyc-supplement", "Hồ sơ KYC cần bổ sung giấy tờ", "Your shop KYC needs more documents"),
    /** Đổi / xóa tài khoản nhận tiền (UC-VND-07). Biến: {@code name}, {@code storeName}, {@code action}, {@code bankName}, {@code accountMasked}, {@code changedAt}. */
    PAYOUT_ACCOUNT_CHANGED("payout-account-changed", "Tài khoản nhận tiền của shop vừa thay đổi", "Your shop payout account was changed"),
    /** Mời nhân viên kích hoạt tài khoản (UC-ADM-01, plan B1 R01.03). Biến: {@code name}, {@code inviterName}, {@code staffTitle}, {@code activationUrl}, {@code expiresHours}. */
    STAFF_INVITATION("staff-invitation", "Lời mời tham gia quản trị Genie Shop", "You are invited to Genie Shop admin");

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
