package com.genie.admin.staff.application;

/**
 * Kết quả kiểm tra email trước khi mời nhân viên (UC-ADM-01 — cảnh báo tức thời ở form mời, API 11.18).
 *
 * @param email     email đã chuẩn hóa (chữ thường, bỏ khoảng trắng)
 * @param available {@code true} khi có thể mời bằng email này
 * @param reason    {@code null} khi hợp lệ; {@code INVALID} sai định dạng · {@code DOMAIN} ngoài {@code STAFF_EMAIL_DOMAINS}
 *                  · {@code TAKEN} đã thuộc một tài khoản
 * @param takenBy   vai trò tài khoản đang giữ email khi {@code reason = TAKEN}: CUSTOMER, VENDOR, STAFF, ADMIN hoặc OTHER
 */
public record StaffEmailCheckView(String email, boolean available, String reason, String takenBy) {
}
