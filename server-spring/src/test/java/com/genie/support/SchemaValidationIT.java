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
}
