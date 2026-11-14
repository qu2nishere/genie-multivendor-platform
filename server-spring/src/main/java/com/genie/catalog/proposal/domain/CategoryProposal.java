package com.genie.catalog.proposal.domain;

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
 * CategoryProposal — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "category_proposals", indexes = {
    @Index(name = "idx_category_proposal_status", columnList = "status, created_at")
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CategoryProposal {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — shop đề xuất

    private Long parentCategoryId;            // NULLABLE — FK → categories.id — danh mục cha đề xuất; NULL = đề xuất danh mục gốc

    @Column(nullable = false, length = 100)
    private String proposedName;              // Tên danh mục đề xuất: "Thú cưng (Pet)"

    @Column(columnDefinition = "TEXT")
    private String reason;                    // NULLABLE — Shop giải thích vì sao cần danh mục này

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private CategoryProposalStatus status = CategoryProposalStatus.PENDING; // PENDING / APPROVED / REJECTED

    private Long createdCategoryId;           // NULLABLE — FK → categories.id — danh mục đã tạo khi duyệt

    private Long reviewedByStaffId;           // NULLABLE — FK mềm → users.id — Staff / Admin xử lý

    @Column(length = 500)
    private String rejectionReason;           // NULLABLE — Lý do từ chối

    private OffsetDateTime reviewedAt;         // NULLABLE

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Duyệt đề xuất — Service tạo Category mới rồi gọi hàm này — UC-CAT-17 */
    public void approve(Long categoryId, Long staffId, OffsetDateTime now) {
        if (this.status != CategoryProposalStatus.PENDING) throw new IllegalStateException("Đề xuất đã được xử lý");
        this.status = CategoryProposalStatus.APPROVED;
        this.createdCategoryId = categoryId;
        this.reviewedByStaffId = staffId;
        this.reviewedAt = now;
    }

    /** Từ chối đề xuất kèm lý do — UC-CAT-17 */
    public void reject(String reason, Long staffId, OffsetDateTime now) {
        if (this.status != CategoryProposalStatus.PENDING) throw new IllegalStateException("Đề xuất đã được xử lý");
        this.status = CategoryProposalStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewedByStaffId = staffId;
        this.reviewedAt = now;
    }
}
