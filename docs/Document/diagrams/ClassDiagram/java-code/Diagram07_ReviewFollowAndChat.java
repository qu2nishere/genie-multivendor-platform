/*
 * ============================================================================
 * GENIE — SƠ ĐỒ LỚP 07: Đánh giá, Hữu ích, Báo cáo vi phạm, Theo dõi shop & Chat (Nhóm 9, 11)
 * ============================================================================
 * Nguồn duy nhất : TLCN/diagrams/ClassDiagram/ENTITY.md (ENTITY.md TLCN v1.3) — sinh tự động, KHÔNG sửa tay;
 *                  sửa ENTITY.md rồi chạy lại công cụ sinh sơ đồ lớp (tools/classdiagram) để giữ đồng bộ.
 * Nhóm thực thể  : 09. NHÓM ĐÁNH GIÁ & TƯƠNG TÁC SHOP, 11. NHÓM NHẮN TIN (CHAT)
 * Thành phần     : 6 @Entity, 0 @IdClass, 2 enum, 49 thuộc tính, 9 phương thức nghiệp vụ
 * Entity         : ProductReview, ShopFollow, ReviewReport, ReviewHelpfulVote, Conversation, Message
 * Enum           : ReviewReportReason, MessageType
 * Import vào EA  : Code > Import Source Directory / Import Source File (ngôn ngữ Java),
 *                  chọn cả thư mục java-code để EA nối được quan hệ giữa các sơ đồ.
 * Quy ước        : Getter/Setter do Lombok sinh — chỉ phương thức nghiệp vụ là ngăn Operations;
 *                  enum lấy nguyên văn từ Phụ lục A; @IdClass dựng từ các trường @Id của entity.
 */
package com.genie.platform.d07_review_follow_chat;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.*;
import java.util.*;

// ============================== ENUMS (2 — Phụ lục A) ==============================

public enum ReviewReportReason { SPAM, OFFENSIVE, FAKE_PURCHASE }   // ← ReviewReport.reason

public enum MessageType { TEXT, IMAGE, PRODUCT_CARD, ORDER_CARD }   // ← Message.type (thẻ sản phẩm / thẻ đơn hàng — UC-SOC-02)

// ============================== ENTITIES (6) ==============================

// 9.1. ProductReview — Đánh giá sản phẩm  —  Bảng: product_reviews
@Entity
@Table(name = "product_reviews")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductReview {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long orderItemId;                 // FK → order_items.id, UNIQUE — ĐẢM BẢO ĐÁNH GIÁ THẬT

    @Column(nullable = false)
    private Long productId;                   // FK → products.id — đánh giá sản phẩm nào (denormalize để query nhanh)

    @Column(nullable = false)
    private Long customerId;                  // FK → users.id — ai đánh giá

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — shop nào bị/được đánh giá

    @Column(nullable = false)
    private Integer rating;                   // Số sao 1–5

    @Column(columnDefinition = "TEXT")
    private String comment;                   // NULLABLE — Nội dung nhận xét

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String imageUrlsJson;             // NULLABLE — Mảng JSON các link ảnh khách đính kèm

    @Column(length = 500)
    private String videoUrl;                  // NULLABLE — Video review ngắn ≤ 15 giây (Review Shorts) trên Silo

    @Column(columnDefinition = "TEXT")
    private String vendorReply;               // NULLABLE — Phản hồi của shop

    private OffsetDateTime vendorRepliedAt;    // NULLABLE — Thời điểm shop phản hồi

    @Builder.Default
    @Column(nullable = false)
    private Boolean isVisible = true;         // false = bị staff ẩn do vi phạm, nhưng vẫn giữ dữ liệu

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm đánh giá
    @Builder.Default
    @Column(nullable = false)
    private Integer helpfulCount = 0;         // Số lượt bấm "Hữu ích" (đếm sẵn, đồng bộ review_helpful_votes) — sắp xếp đánh giá hữu ích lên đầu (UC-SOC-03)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isAnonymous = false;      // Đánh giá ẩn tên — hiển thị dạng "L***g" — UC-SOC-03 (TLCN v1.3)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String aspectSentimentJson;       // NULLABLE — Kết quả PhoBERT (UC-AI-07). TLCN lưu cảm xúc tổng thể: {"overall":"NEGATIVE","score":0.93,"model":"phobert-sentiment-v1","analyzedAt":"..."}; DATN thêm khóa theo khía cạnh (quality, delivery, packaging, service). NULL = chưa phân tích / đánh giá không có chữ

    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Shop phản hồi đánh giá — UC-SOC-04 */
    public void reply(String text, OffsetDateTime now) {
        if (this.vendorReply != null) throw new IllegalStateException("Shop chỉ phản hồi 1 lần");
        this.vendorReply = text;
        this.vendorRepliedAt = now;
    }

    /** Ẩn đánh giá vi phạm (do con người quyết định) — UC-SOC-03 */
    public void hide() {
        this.isVisible = false;
    }

    /** Cộng / trừ bộ đếm "Hữu ích" khi thêm / xóa ReviewHelpfulVote — UC-SOC-03 */
    public void adjustHelpful(int delta) {
        this.helpfulCount = Math.max(0, this.helpfulCount + delta);
    }

    /** Gắn kết quả phân tích cảm xúc PhoBERT do AI Worker gọi lại — UC-AI-07 */
    public void attachSentiment(String aspectJson) {
        this.aspectSentimentJson = aspectJson;
    }

}

// 9.2. ShopFollow — Theo dõi shop  —  Bảng: shop_follows
@Entity
@Table(name = "shop_follows", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"customerId", "vendorId"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ShopFollow {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long customerId;                  // FK → users.id — người theo dõi (UNIQUE ghép với vendorId)

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — shop được theo dõi

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm bấm theo dõi
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Theo dõi shop (UNIQUE customer+vendor) — UC-SOC-06 */
    public static ShopFollow of(Long customerId, Long vendorId, OffsetDateTime now) {
        ShopFollow f = new ShopFollow();
        f.customerId = customerId; f.vendorId = vendorId; f.createdAt = now;
        return f;
    }
}

// 9.3. ReviewReport — Báo cáo đánh giá vi phạm  —  Bảng: review_reports
@Entity
@Table(name = "review_reports")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ReviewReport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long reviewId;                    // FK → product_reviews.id — báo cáo review nào

    @Column(nullable = false)
    private Long reporterId;                  // FK → users.id — ai báo cáo (thường là shop bị đánh giá)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewReportReason reason;        // SPAM / OFFENSIVE (xúc phạm) / FAKE_PURCHASE (mua giả)

    @Column(columnDefinition = "TEXT")
    private String note;                      // NULLABLE — Giải thích thêm của người báo cáo

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm báo cáo
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Báo cáo đánh giá vi phạm — UC-SOC-05 */
    public static ReviewReport of(Long reviewId, Long reporterId, ReviewReportReason reason, String note, OffsetDateTime now) {
        ReviewReport r = new ReviewReport();
        r.reviewId = reviewId; r.reporterId = reporterId; r.reason = reason; r.note = note; r.createdAt = now;
        return r;
    }
}

// 9.4. ReviewHelpfulVote — Lượt bấm "Hữu ích" cho đánh giá  —  Bảng: review_helpful_votes
@Entity
@Table(name = "review_helpful_votes", uniqueConstraints = {
    @UniqueConstraint(name = "uq_review_helpful_vote", columnNames = {"review_id", "user_id"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ReviewHelpfulVote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long reviewId;                    // FK → product_reviews.id (ON DELETE CASCADE)

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — người bấm (không được là tác giả đánh giá)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Bấm "Hữu ích" (bấm lần 2 = xóa dòng và trừ helpfulCount) — UC-SOC-03 */
    public static ReviewHelpfulVote of(Long reviewId, Long userId, Long reviewAuthorId, OffsetDateTime now) {
        if (userId.equals(reviewAuthorId)) throw new IllegalArgumentException("Không tự bấm Hữu ích cho đánh giá của mình");
        ReviewHelpfulVote v = new ReviewHelpfulVote();
        v.reviewId = reviewId; v.userId = userId; v.createdAt = now;
        return v;
    }
}

// 11.1. Conversation — Cuộc hội thoại  —  Bảng: conversations
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

    @Column(name = "customer_unread_count", nullable = false)
    private Integer customerUnread;           // Số tin khách chưa đọc — badge đỏ phía khách

    @Column(name = "vendor_unread_count", nullable = false)
    private Integer vendorUnread;             // Số tin shop chưa đọc — badge đỏ phía shop

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

// 11.2. Message — Tin nhắn  —  Bảng: messages
@Entity
@Table(name = "messages")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Message {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
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

    @Builder.Default
    @Column(nullable = false)
    private Boolean isAuto = false;           // Tin tự động của shop (UC-VND-06), không tính tỉ lệ phản hồi (V26)

    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tin đã đọc — UC-SOC-01 */
    public void markRead() {
        this.isRead = true;
    }
}
