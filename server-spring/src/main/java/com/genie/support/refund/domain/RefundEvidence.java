package com.genie.support.refund.domain;

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
 * RefundEvidence — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "refund_evidences")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class RefundEvidence {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "refund_request_id", nullable = false)
    private Long refundRequestId;             // FK → refund_requests.id

    @Column(nullable = false, length = 500)
    private String mediaUrl;                  // Link ảnh/video bằng chứng khách upload

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;              // IMAGE hoặc VIDEO

    @Column(nullable = false)
    private Integer displayOrder;             // Thứ tự hiển thị

    @Column(nullable = false)
    private Integer submissionRound;          // 1 = nộp khi tạo yêu cầu; 2 = nộp bổ sung theo yêu cầu của shop

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm upload
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Bằng chứng là video — UC-SUP-01 */
    public boolean isVideo() {
        return this.mediaType == MediaType.VIDEO;
    }

    /** Bằng chứng nộp bổ sung theo yêu cầu của shop — UC-SUP-01 */
    public boolean isAdditional() {
        return this.submissionRound != null && this.submissionRound > 1;
    }
}
