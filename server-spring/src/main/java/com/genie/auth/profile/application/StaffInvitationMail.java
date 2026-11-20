// Nội dung email mời nhân viên do module admin cung cấp (plan B1 mục 3.1; MailTemplate.STAFF_INVITATION)
package com.genie.auth.profile.application;

/** {@code staffTitleLabel} — nhãn chức danh đã dịch ("Nhân viên kiểm duyệt", "Tùy chỉnh"…). */
public record StaffInvitationMail(String fullName, String inviterName, String staffTitleLabel) {
}
