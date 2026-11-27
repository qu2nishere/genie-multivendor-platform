package com.genie.support;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Kiểm schema: Flyway chạy toàn bộ migration trên PostgreSQL 16 thật, rồi Hibernate {@code ddl-auto: validate}
 * so mọi {@code @Entity} với bảng — lệch tên / kiểu cột là context không khởi động được (implement-entity.md Phase 12).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class SchemaValidationIT {

    static {
        // JVM Windows có thể mặc định "Asia/Saigon" — tên PostgreSQL 16 không nhận khi mở kết nối; dự án lưu thời gian theo UTC.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    EntityManager entityManager;

    @Test
    void flywayMigrationsMatchEntities() {
        assertThat(entityManager.getMetamodel().getEntities()).isNotEmpty();
    }

    /**
     * Mỗi giá trị enum Java phải nằm trong {@code CHECK} của cột tương ứng — migration sửa ràng buộc mà quên chép
     * giá trị cũ / mới thì đỏ ở đây thay vì lỗi lúc ghi thật (plan 2026-11-26 N01.08, V26).
     */
    @Test
    void checkConstraintsAcceptEveryEnumValue() {
        assertCheckContainsAll("system_logs", "ck_system_logs_action_type", com.genie.common.audit.AuditAction.values());
        assertCheckContainsAll("notifications", "ck_notifications_type",
                com.genie.notification.inbox.domain.NotificationType.values());
        assertCheckContainsAll("messages", "ck_messages_type", com.genie.social.chat.domain.MessageType.values());
    }

    private void assertCheckContainsAll(String table, String constraint, Enum<?>[] values) {
        String def = (String) entityManager.createNativeQuery(
                        "SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c JOIN pg_class t ON t.oid = c.conrelid "
                                + "WHERE t.relname = :table AND c.conname = :name")
                .setParameter("table", table)
                .setParameter("name", constraint)
                .getSingleResult();
        for (Enum<?> v : values) {
            assertThat(def).as(table + "." + constraint + " thiếu " + v.name()).contains("'" + v.name() + "'");
        }
    }
}
