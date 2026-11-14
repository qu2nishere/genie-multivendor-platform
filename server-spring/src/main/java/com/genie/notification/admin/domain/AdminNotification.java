package com.genie.notification.admin.domain;

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
 * AdminNotification — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "admin_notifications")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class AdminNotification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminNotificationType type;       // SYSTEM_ALERT / NEW_ORDER / USER_APPROVAL / WITHDRAW_REQUEST / ESCROW_DISCREPANCY (5 loại TLCN)

    @Column(nullable = false, length = 255)
    private String title;                     // Tiêu đề

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;                   // Nội dung chi tiết

    @Column(length = 500)
    private String referenceUrl;              // NULLABLE — Link tới trang xử lý trong admin panel

    @Column(length = 500)
    private String imageUrl;                  // NULLABLE — Ảnh minh hoạ

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;           // Đã xử lý/đã xem chưa

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm phát sinh

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Nhân viên / Admin đã xem cảnh báo — UC-NOT-01 */
    public void markRead() {
        this.isRead = true;
    }
}
