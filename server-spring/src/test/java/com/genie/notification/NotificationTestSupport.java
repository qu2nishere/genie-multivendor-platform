// Tiện ích chung cho IT thông báo (plan 2026-11-26 thông báo N03–N07)
package com.genie.notification;

import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.support.IntegrationTestBase;
import com.genie.support.TestDataBuilder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class NotificationTestSupport extends IntegrationTestBase {

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    protected TestDataBuilder data;
    protected TransactionTemplate tx;

    @BeforeEach
    void notificationSupport() {
        data = new TestDataBuilder(jdbc);
        tx = new TransactionTemplate(transactionManager);
    }

    protected long customer() {
        return createCustomer(uniqueEmail(), PASSWORD).getId();
    }

    /** Nhân viên có chức danh + quyền cho trước (giống AuditLogTestSupport). */
    protected long staff(String... permissions) {
        long id = customer();
        jdbc.update("UPDATE users SET role = 'STAFF' WHERE id = ?", id);
        jdbc.update("INSERT INTO admin_profiles (user_id, full_name, is_active_moderator, staff_title, created_at, updated_at) "
                + "VALUES (?, 'Nhân viên test', FALSE, NULL, now(), now())", id);
        Long admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        for (String p : permissions) {
            jdbc.update("INSERT INTO staff_permissions (staff_user_id, permission, granted_by_admin_id, granted_at) "
                    + "VALUES (?, ?, ?, now())", id, p, admin);
        }
        return id;
    }

    protected String auth(long userId, UserRole role) {
        return "Bearer " + jwtService.issue(userId, role, 1L).token();
    }

    protected List<Map<String, Object>> notificationsOf(long userId) {
        return jdbc.queryForList("SELECT * FROM notifications WHERE user_id = ? ORDER BY id", userId);
    }

    protected int adminRows(String type, String titleLike) {
        return jdbc.queryForObject("SELECT count(*) FROM admin_notifications WHERE type = ? AND title LIKE ?",
                Integer.class, type, titleLike);
    }
}
