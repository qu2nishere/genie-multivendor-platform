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
 * ReviewHelpfulVote — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
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
