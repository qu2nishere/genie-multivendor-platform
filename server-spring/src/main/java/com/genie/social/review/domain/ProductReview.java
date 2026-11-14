package com.genie.social.review.domain;

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
 * ProductReview — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
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
    private String videoUrl;                  // NULLABLE — Video review ngắn ≤ 15 giây (Review Shorts) trên MinIO

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
