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
 * ProductImage — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "product_images")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "product_id", nullable = false)
    private Long productId;                   // FK → products.id — ảnh thuộc sản phẩm nào

    private Long variantId;                   // NULLABLE — FK → product_variants.id. NULL = ảnh trong gallery chung của sản phẩm; khác NULL = ảnh RIÊNG của biến thể (chỉ hiện khi khách chọn biến thể đó, KHÔNG xuất hiện trong gallery chung)

    @Column(nullable = false, length = 500)
    private String imageUrl;                  // Link ảnh gốc trên storage (S3/MinIO)

    @Column(length = 500)
    private String cdnUrl;                    // NULLABLE — Link ảnh qua CDN, tải nhanh hơn cho người dùng cuối

    private Integer width;                    // NULLABLE — Chiều rộng ảnh (pixel)

    private Integer height;                   // NULLABLE — Chiều cao ảnh (pixel)

    private Long fileSize;                    // NULLABLE — Dung lượng file (byte)

    @Column(length = 500)
    private String altText;                   // NULLABLE — Văn bản thay thế cho SEO & người khiếm thị

    private Boolean isPrimary;                // NULLABLE — true = ảnh đại diện hiển thị ở danh sách sản phẩm

    @Column(length = 50)
    private String mimeType;                  // NULLABLE — Loại file: "image/jpeg", "image/webp"

    @Column(nullable = false)
    private Integer displayOrder;             // Thứ tự hiển thị trong thư viện ảnh (số nhỏ lên trước)
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đặt làm ảnh đại diện — UC-CAT-10 */
    public void markPrimary() {
        this.isPrimary = true;
        this.displayOrder = 0;
    }
}
