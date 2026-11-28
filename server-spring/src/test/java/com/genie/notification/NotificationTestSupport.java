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
        // Thông báo sàn / nhân viên không gắn người — dọn để các ca không thấy dữ liệu của nhau
        jdbc.update("DELETE FROM platform_notifications");
        jdbc.update("DELETE FROM admin_notifications");
    }

    /** Khách tạo lúc {@code createdAtIso} (thông báo sàn phát trước lúc này không hiện — D3). */
    protected long customerCreatedAt(String createdAtIso) {
        long id = customer();
        jdbc.update("UPDATE users SET created_at = CAST(? AS TIMESTAMPTZ) WHERE id = ?", createdAtIso, id);
        return id;
    }

    protected long vendorCreatedAt(String createdAtIso) {
        long id = customerCreatedAt(createdAtIso);
        jdbc.update("UPDATE users SET role = 'VENDOR' WHERE id = ?", id);
        return id;
    }

    protected long insertPersonal(long userId, String type, String title, String createdAtIso, boolean read) {
        return jdbc.queryForObject("INSERT INTO notifications (user_id, type, title, body, channel, delivery_status, "
                + "retry_count, is_read, sent_at, created_at) VALUES (?, ?, ?, 'Thân', 'IN_APP', 'SENT', 0, ?, "
                + "CAST(? AS TIMESTAMPTZ), CAST(? AS TIMESTAMPTZ)) RETURNING id", Long.class,
                userId, type, title, read, createdAtIso, createdAtIso);
    }

    /** Thông báo sàn đã phát lúc {@code dispatchedAtIso} ({@code null} = đang hẹn giờ). */
    protected long insertPlatform(String target, String recipientIdsJson, String title, String dispatchedAtIso) {
        return jdbc.queryForObject("INSERT INTO platform_notifications (title, content, target, created_by, created_at, "
                + "updated_at, reach_count, send_at, recipient_user_ids_json, dispatched_at) VALUES (?, '<p>Nội dung "
                + "<b>sàn</b></p>', ?, 1, now(), now(), 10, NULL, CAST(? AS JSONB), CAST(? AS TIMESTAMPTZ)) RETURNING id",
                Long.class, title, target, recipientIdsJson, dispatchedAtIso);
    }

    protected long insertAdmin(String type, String title, String createdAtIso) {
        return jdbc.queryForObject("INSERT INTO admin_notifications (type, title, content, is_read, created_at, updated_at) "
                + "VALUES (?, ?, 'Nội dung', FALSE, CAST(? AS TIMESTAMPTZ), now()) RETURNING id", Long.class,
                type, title, createdAtIso);
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
