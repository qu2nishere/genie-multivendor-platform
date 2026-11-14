package com.genie.common.audit;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * SystemLog — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "system_logs")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class SystemLog {

    @Id
    @Column(length = 36)
    private String id;                        // Khoá chính kiểu UUID (36 ký tự), KHÔNG tự tăng

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm xảy ra hành động

    private Long userId;                      // NULLABLE — Ai thực hiện. NULL = hành động của hệ thống

    @Column(nullable = false, length = 255)
    private String userEmail;                 // Email người thực hiện — CHỤP LẠI, không JOIN

    @Column(nullable = false, length = 50)
    private String userRole;                  // Vai trò lúc thực hiện — chụp lại

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private AuditAction actionType;           // Loại hành động: LOGIN_SUCCESS, KYC_APPROVED, PAYOUT_APPROVED...

    @Column(length = 100)
    private String referenceId;               // NULLABLE — ID đối tượng bị tác động

    @Column(columnDefinition = "TEXT")
    private String details;                   // NULLABLE — Chi tiết dạng JSON: giá trị trước/sau khi sửa

    @Column(length = 45)
    private String ipAddress;                 // NULLABLE — Địa chỉ IP thực hiện hành động (NULL với tác vụ hệ thống / Cron)

    @Column(nullable = false, length = 20)
    private String status;                    // Kết quả: "SUCCESS" / "FAILED"

    @Column(length = 500)
    private String userAgent;                 // NULLABLE — Trình duyệt/thiết bị thực hiện

    // Sinh UUID và mốc thời gian trước khi lưu thông qua hook @PrePersist của JPA (createdAt còn có @CreationTimestamp — Quy ước 12)
    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.createdAt == null) this.createdAt = OffsetDateTime.now();
    }
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tạo bản ghi kiểm toán bất biến — UC-ADM-03 */
    public static SystemLog of(Long userId, String userEmail, String userRole, AuditAction action, String referenceId, String details, String ip, boolean success) {
        SystemLog l = new SystemLog();
        l.userId = userId; l.userEmail = userEmail; l.userRole = userRole;   // NOT NULL — chụp lại lúc thực hiện ("system@genie.vn" / "SYSTEM" với job nền)
        l.actionType = action; l.referenceId = referenceId; l.details = details; l.ipAddress = ip;
        l.status = success ? "SUCCESS" : "FAILED";
        return l;   // id & createdAt gán ở prePersist() — không vi phạm NOT NULL dù factory không truyền thời gian
    }
}
