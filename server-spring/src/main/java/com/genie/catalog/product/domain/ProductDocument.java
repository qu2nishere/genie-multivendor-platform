package com.genie.catalog.product.domain;

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
 * ProductDocument — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "product_documents", indexes = {
    @Index(name = "idx_product_document_product", columnList = "product_id")
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductDocument {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "product_id", nullable = false)
    private Long productId;                   // FK → products.id (ON DELETE CASCADE)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductDocumentType documentType; // BUSINESS_LICENSE / QUALITY_CERTIFICATE / IMPORT_DOCUMENT / BRAND_AUTHORIZATION / OTHER

    @Column(nullable = false, length = 500)
    private String fileUrl;                   // Đường dẫn MinIO (bucket riêng tư, đọc qua Presigned URL)

    @Column(nullable = false, length = 255)
    private String fileName;                  // Tên tệp gốc hiển thị cho Staff

    @Column(nullable = false, length = 20)
    private String fileExtension;             // pdf / doc / docx / xls / xlsx — KHÔNG nhận ảnh (giao diện chặn)

    @Column(nullable = false)
    private Long fileSizeBytes;               // ≤ 10 MB / tệp

    private LocalDate expiresAt;              // NULLABLE — Ngày hết hạn của giấy tờ (giấy phép có thời hạn); NULL = không thời hạn

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime uploadedAt;

    // ===== QUAN HỆ =====
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;                  // Sản phẩm sở hữu giấy tờ
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Giấy tờ còn hiệu lực tại thời điểm duyệt — UC-CAT-13 */
    public boolean isValidOn(LocalDate day) {
        return this.expiresAt == null || !this.expiresAt.isBefore(day);
    }
}
