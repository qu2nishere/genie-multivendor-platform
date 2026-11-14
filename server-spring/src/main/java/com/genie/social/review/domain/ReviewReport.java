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
 * ReviewReport — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
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
