package com.genie.admin;

import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.support.IntegrationTestBase;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

/** Tiện ích chung cho test màn Nhật ký hệ thống (plan nhật ký kiểm toán L03.07, L03.08, L04.04). */
abstract class AuditLogTestSupport extends IntegrationTestBase {

    @Autowired
    protected JwtService jwtService;

    /** Token Admin seed (yen.dh — đủ 13 quyền). */
    protected String adminToken() {
        return token("yen.dh@genie.example", UserRole.ADMIN);
    }

    protected String token(String email, UserRole role) {
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        return "Bearer " + jwtService.issue(id, role, 1L).token();
    }

    /** Nhân viên mới có chức danh (admin_profiles) và các quyền cho trước. */
    protected Long createStaff(String title, String... permissions) {
        var user = createCustomer(uniqueEmail(), PASSWORD);
        jdbc.update("UPDATE users SET role = 'STAFF' WHERE id = ?", user.getId());
        jdbc.update("INSERT INTO admin_profiles (user_id, full_name, is_active_moderator, staff_title, created_at, updated_at) "
                + "VALUES (?, 'Nhân viên test', FALSE, ?, now(), now())", user.getId(), title);
        Long admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        for (String p : permissions) {
            jdbc.update("INSERT INTO staff_permissions (staff_user_id, permission, granted_by_admin_id, granted_at) "
                    + "VALUES (?, ?, ?, now())", user.getId(), p, admin);
        }
        return user.getId();
    }

    protected String staffToken(Long staffId) {
        return "Bearer " + jwtService.issue(staffId, UserRole.STAFF, 1L).token();
    }

    /** Chèn thẳng một dòng nhật ký với thời điểm cho trước (AuditLogger luôn lấy giờ hiện tại). */
    protected String insertLog(Instant at, Long userId, String email, String role, String action, String referenceId,
                               String details, String ip, String status) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO system_logs (id, created_at, user_id, user_email, user_role, action_type, reference_id, "
                        + "details, ip_address, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, Timestamp.from(at), userId, email, role, action, referenceId, details, ip, status);
        return id;
    }

    protected void clearLogs() {
        jdbc.update("DELETE FROM system_logs");
    }
}
