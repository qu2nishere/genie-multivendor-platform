package com.genie.notification.inbox.domain;

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
 * Notification — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "notifications")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — gửi cho ai

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;            // 15 loại (Phụ lục A): ORDER_STATUS_CHANGED, KYC_APPROVED, PAYMENT_SUCCESS (người mua), NEW_ORDER_FOR_VENDOR (shop)... — sự kiện con ghi trong metadataJson.event

    @Column(nullable = false, length = 255)
    private String title;                     // Tiêu đề thông báo

    @Column(columnDefinition = "TEXT")
    private String body;                      // NULLABLE — Nội dung chi tiết

    @Column(length = 500)
    private String imageUrl;                  // NULLABLE — Ảnh minh hoạ (ảnh sản phẩm, banner)

    @Column(length = 255)
    private String referenceId;               // NULLABLE — ID đối tượng liên quan, để bấm vào là điều hướng tới

    @Column(length = 50)
    private String referenceType;             // NULLABLE — Loại đối tượng: "ORDER", "PRODUCT", "VOUCHER"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadataJson;              // NULLABLE — Dữ liệu bổ sung dạng JSON tuỳ loại thông báo

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;      // Kênh phát thông báo MVP: IN_APP (lưu bảng + đẩy WebSocket) / EMAIL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationDeliveryStatus deliveryStatus; // PENDING, SENT, DELIVERED, FAILED (A.14)

    private Integer retryCount;               // NULLABLE — Số lần đã thử gửi lại khi thất bại

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;           // Đã đọc hay chưa — dùng cho badge số đỏ

    private OffsetDateTime sentAt;             // NULLABLE — Thời điểm gửi đi thành công

    private OffsetDateTime readAt;             // NULLABLE — Thời điểm user mở đọc

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo thông báo
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đã gửi qua kênh — UC-NOT-01 */
    public void markSent(OffsetDateTime now) {
        this.deliveryStatus = NotificationDeliveryStatus.SENT;
        this.sentAt = now;
    }

    /** Gửi lỗi — tăng retryCount, @Scheduled thử lại tối đa 3 lần — UC-NOT-03 */
    public void markFailed() {
        this.retryCount += 1;
        this.deliveryStatus = NotificationDeliveryStatus.FAILED;
    }

    /** Người dùng đã đọc — UC-NOT-01 */
    public void markRead(OffsetDateTime now) {
        this.isRead = true;
        this.readAt = now;
    }
}
