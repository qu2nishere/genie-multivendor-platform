package com.genie.common.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * PlatformSetting — cấu hình chung toàn sàn dạng khóa → giá trị JSON (ENTITY 13.x; plan B2 D2, migration V23). Ví dụ
 * {@code commission.default_rate = 0.0500}. Đọc / ghi qua {@link PlatformSettings}.
 */
@Entity
@Table(name = "platform_settings")
@Getter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class PlatformSetting {

    @Id
    @Column(name = "setting_key", length = 100)
    private String settingKey;                // Khóa cấu hình, vd "commission.default_rate"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_json", nullable = false, columnDefinition = "jsonb")
    private String valueJson;                 // Giá trị JSON (số, chuỗi, đối tượng)

    @Column(name = "updated_by_admin_id")
    private Long updatedByAdminId;            // NULLABLE — FK → users.id, người đổi gần nhất (NULL = giá trị khởi tạo)

    @Column(nullable = false)
    private OffsetDateTime updatedAt;         // Lần đổi gần nhất

    // ===== Phương thức nghiệp vụ =====

    /** Đổi giá trị (JSON đã kiểm ở service) — UC-CAT-14 bước 8 */
    public void change(String valueJson, Long adminId, OffsetDateTime now) {
        this.valueJson = valueJson;
        this.updatedByAdminId = adminId;
        this.updatedAt = now;
    }
}
