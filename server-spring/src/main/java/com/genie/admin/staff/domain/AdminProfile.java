package com.genie.admin.staff.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import com.genie.admin.rbac.domain.StaffPermissionEnum;

/**
 * AdminProfile — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 * D2: bỏ trường điều hướng sang module khác: user — dùng cột Long *Id tương ứng.
 */
@Entity
@Table(name = "admin_profiles")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class AdminProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long userId;                      // FK → users.id, UNIQUE (quan hệ 1-1)

    @Column(nullable = false, length = 255)
    private String fullName;                  // Họ tên nhân viên/quản trị viên

    @Column(length = 500)
    private String avatarUrl;                 // NULLABLE — Ảnh đại diện

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActiveModerator = false; // true = đang trong danh sách nhận phân công kiểm duyệt

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo hồ sơ

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật gần nhất

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StaffTitle staffTitle;            // NULLABLE — Chức danh của STAFF (3 chức danh); NULL với ADMIN. Chỉ dùng nạp bộ quyền mặc định — quyền thật ở staff_permissions


    // ===== QUAN HỆ =====
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Bật/tắt trạng thái đang trực kiểm duyệt — UC-ADM-03 */
    public void toggleModerator(boolean active) {
        this.isActiveModerator = active;
    }

    /** Đổi chức danh — chỉ trả về bộ quyền mặc định để Admin xác nhận, không tự cấp/thu hồi quyền — UC-ADM-01, UC-ADM-02 */
    public Set<StaffPermissionEnum> changeTitle(StaffTitle newTitle) {
        this.staffTitle = newTitle;
        return newTitle == null ? Set.of() : newTitle.defaultPermissions();
    }

}
