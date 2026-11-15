package com.genie.common.audit;

/**
 * Người thực hiện được chụp lại vào {@code system_logs} (email / vai trò lúc đó — không JOIN về sau).
 *
 * @param userId NULLABLE — null với job nền hoặc người chưa đăng nhập
 * @param email  NOT NULL ở bảng — job nền dùng {@code system@genie.vn}; người chưa đăng nhập dùng email đã nhập
 * @param role   NOT NULL ở bảng — {@code SYSTEM}, {@code ANONYMOUS} hoặc tên {@code UserRole}
 */
public record AuditActor(Long userId, String email, String role) {

    public static final AuditActor SYSTEM = new AuditActor(null, "system@genie.vn", "SYSTEM");

    /** Người chưa đăng nhập / không xác định (ví dụ đăng nhập sai email). */
    public static AuditActor anonymous(String email) {
        return new AuditActor(null, email == null || email.isBlank() ? "anonymous" : email, "ANONYMOUS");
    }
}
