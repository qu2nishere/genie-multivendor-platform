package com.genie.social.chat.domain;

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
 * Message — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "messages")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Message {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;              // FK → conversations.id — thuộc hội thoại nào

    @Column(nullable = false)
    private Long senderId;                    // FK → users.id — ai gửi (khách hoặc shop)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;                 // TEXT (chữ) / IMAGE (ảnh)

    @Column(columnDefinition = "TEXT")
    private String content;                   // NULLABLE — Nội dung chữ (khi type = TEXT)

    @Column(length = 500)
    private String attachmentUrl;             // NULLABLE — Link ảnh đính kèm (khi type = IMAGE)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;           // Người nhận đã đọc tin này chưa

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm gửi
    private Long productVariantId;            // NULLABLE — FK → product_variants.id (khi type = PRODUCT_CARD — UC-SOC-02)
    private Long orderId;                     // NULLABLE — FK → orders.id (khi type = ORDER_CARD — UC-SOC-02, TLCN v1.3)

    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tin đã đọc — UC-SOC-01 */
    public void markRead() {
        this.isRead = true;
    }
}
