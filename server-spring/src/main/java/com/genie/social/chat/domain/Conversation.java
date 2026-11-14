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
 * Conversation — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "conversations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "vendor_id"}))
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Conversation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long customerId;                  // FK → users.id — khách hàng (UNIQUE ghép với vendorId)

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — shop

    @Column(length = 255)
    private String lastMessageSnippet;        // NULLABLE — Trích đoạn tin nhắn cuối, hiển thị ở danh sách chat

    private OffsetDateTime lastMessageAt;      // NULLABLE — Thời điểm tin cuối — dùng để SẮP XẾP danh sách

    @Builder.Default   // sửa lệch ENTITY.md theo Quy ước 12 (implement-entity.md mục 6)
    @Column(name = "customer_unread_count", nullable = false)
    private Integer customerUnread = 0;           // Số tin khách chưa đọc — badge đỏ phía khách

    @Builder.Default
    @Column(name = "vendor_unread_count", nullable = false)
    private Integer vendorUnread = 0;             // Số tin shop chưa đọc — badge đỏ phía shop

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm mở hội thoại

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật gần nhất

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "conversation_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua Message.conversationId
    private List<Message> messages;           // Các tin nhắn trong hội thoại
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Cập nhật tóm tắt & bộ đếm chưa đọc — UC-SOC-01 */
    public void onNewMessage(Message m, boolean fromCustomer) {
        this.lastMessageSnippet = m.getContent() == null ? "[" + m.getType() + "]" : m.getContent().substring(0, Math.min(100, m.getContent().length()));
        this.lastMessageAt = m.getCreatedAt();
        if (fromCustomer) this.vendorUnread += 1; else this.customerUnread += 1;
    }

    /** Đặt lại badge chưa đọc — UC-SOC-01 */
    public void markReadBy(boolean customer) {
        if (customer) this.customerUnread = 0; else this.vendorUnread = 0;
    }
}
