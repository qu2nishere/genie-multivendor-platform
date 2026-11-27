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

    /** Người dùng đứng ở phía nào của hội thoại (kiểm IDOR — plan chat N5). */
    public enum Side { CUSTOMER, VENDOR, NONE }

    public Side sideOf(long userId) {
        if (customerId != null && customerId == userId) return Side.CUSTOMER;
        if (vendorId != null && vendorId == userId) return Side.VENDOR;
        return Side.NONE;
    }

    /** id người ở phía bên kia so với {@code userId} (đã là thành viên). */
    public long otherThan(long userId) {
        return sideOf(userId) == Side.CUSTOMER ? vendorId : customerId;
    }

    /** Số tin chưa đọc phía {@code side}. */
    public int unreadFor(Side side) {
        return side == Side.CUSTOMER ? customerUnread : side == Side.VENDOR ? vendorUnread : 0;
    }

    /**
     * Cập nhật tóm tắt & bộ đếm chưa đọc — UC-SOC-01 (plan chat M03.01): trích đoạn theo loại khi không có chữ ("[Hình ảnh]",
     * "[Sản phẩm]", "[Đơn hàng]"), cắt 100 ký tự; chỉ tăng bộ đếm của <b>bên nhận</b>. Gọi khi đang khóa dòng (N3).
     */
    public void onNewMessage(Message m, boolean fromCustomer) {
        this.lastMessageSnippet = snippetOf(m);
        this.lastMessageAt = m.getCreatedAt();
        if (fromCustomer) this.vendorUnread += 1; else this.customerUnread += 1;
    }

    static String snippetOf(Message m) {
        String text = m.getContent();
        if (m.getType() == MessageType.TEXT && text != null && !text.isBlank()) {
            String flat = text.replaceAll("\\s+", " ").trim();
            return flat.length() <= 100 ? flat : flat.substring(0, 100);
        }
        return switch (m.getType()) {
            case IMAGE -> "[Hình ảnh]";
            case PRODUCT_CARD -> "[Sản phẩm]";
            case ORDER_CARD -> "[Đơn hàng]";
            case TEXT -> "";
        };
    }

    /** Đặt lại badge chưa đọc — UC-SOC-01 */
    public void markReadBy(boolean customer) {
        if (customer) this.customerUnread = 0; else this.vendorUnread = 0;
    }
}
