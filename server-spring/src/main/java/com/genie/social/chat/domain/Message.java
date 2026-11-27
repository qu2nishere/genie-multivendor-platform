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

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm gửi — đặt từ Clock lúc tạo (plan chat M03.02; khung giờ / cursor dùng cùng đồng hồ)
    private Long productVariantId;            // NULLABLE — FK → product_variants.id (khi type = PRODUCT_CARD — UC-SOC-02)
    private Long orderId;                     // NULLABLE — FK → orders.id (khi type = ORDER_CARD — UC-SOC-02, TLCN v1.3)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isAuto = false;           // Tin tự động của shop (lời chào / vắng mặt / tạm nghỉ — UC-VND-06), không tính tỉ lệ phản hồi (V26)

    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tin đã đọc — UC-SOC-01 */
    public void markRead() {
        this.isRead = true;
    }

    // ===== Factory (plan chat M03.02, M07, M08) =====

    public static Message text(long conversationId, long senderId, String text, OffsetDateTime at) {
        return base(conversationId, senderId, MessageType.TEXT, at).content(text).build();
    }

    /** {@code objectKey} = khóa chính thức trong bucket riêng tư {@code chat-media} (không lưu URL — N6). */
    public static Message image(long conversationId, long senderId, String objectKey, OffsetDateTime at) {
        return base(conversationId, senderId, MessageType.IMAGE, at).attachmentUrl(objectKey).build();
    }

    public static Message productCard(long conversationId, long senderId, long variantId, OffsetDateTime at) {
        return base(conversationId, senderId, MessageType.PRODUCT_CARD, at).productVariantId(variantId).build();
    }

    public static Message orderCard(long conversationId, long senderId, long orderId, OffsetDateTime at) {
        return base(conversationId, senderId, MessageType.ORDER_CARD, at).orderId(orderId).build();
    }

    /** Tin tự động của shop (lời chào / vắng mặt / tạm nghỉ — UC-VND-06): đứng tên chủ shop, {@code is_auto}. */
    public static Message auto(long conversationId, long vendorUserId, String text, OffsetDateTime at) {
        return base(conversationId, vendorUserId, MessageType.TEXT, at).content(text).isAuto(true).build();
    }

    private static MessageBuilder base(long conversationId, long senderId, MessageType type, OffsetDateTime at) {
        return Message.builder().conversationId(conversationId).senderId(senderId).type(type).createdAt(at)
                .isRead(false).isAuto(false);
    }

    /** Trạng thái hiển thị (D3): đã lưu = {@code DELIVERED}; bên nhận đã mở hội thoại = {@code SEEN}. */
    public String status() {
        return Boolean.TRUE.equals(isRead) ? "SEEN" : "DELIVERED";
    }
}
