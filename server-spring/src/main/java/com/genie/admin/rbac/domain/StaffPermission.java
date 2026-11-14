package com.genie.admin.rbac.domain;

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
 * StaffPermission — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "staff_permissions")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class StaffPermission {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long staffUserId;                 // FK → users.id — nhân viên được cấp quyền

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StaffPermissionEnum permission;   // 13 quyền TLCN: VIEW_DASHBOARD, REVIEW_KYC, MANAGE_CATALOG, MODERATE_REVIEW, HANDLE_DISPUTE, PROCESS_PAYOUT, UPDATE_COMMISSION, VIEW_FINANCE, MANAGE_VOUCHER, MANAGE_PLATFORM_NOTIFICATION, VIEW_AUDIT_LOG, MANAGE_USER, MANAGE_RBAC

    @Column(nullable = false)
    private Long grantedByAdminId;            // FK → users.id — Admin nào đã cấp quyền này

    @Column(nullable = false)
    private OffsetDateTime grantedAt;          // Thời điểm cấp quyền

    private OffsetDateTime revokedAt;          // NULLABLE — Thời điểm thu hồi. NULL = quyền vẫn còn hiệu lực
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Quyền còn hiệu lực — UC-ADM-02 */
    public boolean isActive() {
        return this.revokedAt == null;
    }

    /** Thu hồi quyền — UC-ADM-02 */
    public void revoke(OffsetDateTime now) {
        this.revokedAt = now;
    }
}
