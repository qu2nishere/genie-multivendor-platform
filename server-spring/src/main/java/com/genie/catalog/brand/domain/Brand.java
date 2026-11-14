package com.genie.catalog.brand.domain;

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
 * Brand — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "brands", uniqueConstraints = {
    @UniqueConstraint(name = "uq_brand_slug", columnNames = "slug")
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Brand {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false, length = 100)
    private String name;                      // Tên hiển thị: "Samsung", "Logitech"

    @Column(nullable = false, length = 120)
    private String slug;                      // Khóa chuẩn hóa (bỏ dấu, chữ thường) — chống trùng "samsung" / "SAMSUNG"

    @Column(length = 500)
    private String logoUrl;                   // NULLABLE — Logo thương hiệu

    @Builder.Default
    @Column(nullable = false)
    private Boolean isVerified = false;       // true = Staff đã xác nhận (thương hiệu do shop tự thêm mặc định chưa xác minh)

    private Long createdByUserId;             // NULLABLE — FK mềm → users.id — shop đã thêm thương hiệu (NULL = do sàn tạo)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Shop thêm thương hiệu mới ngay trong form đăng bán (chưa xác minh) — UC-CAT-09 */
    public static Brand proposedBy(String name, String slug, Long vendorUserId, OffsetDateTime now) {
        Brand b = new Brand();
        b.name = name; b.slug = slug; b.createdByUserId = vendorUserId; b.isVerified = false; b.createdAt = now;
        return b;
    }

    /** Staff xác nhận thương hiệu (khi duyệt sản phẩm đầu tiên dùng thương hiệu này) — UC-CAT-13 */
    public void verify() {
        this.isVerified = true;
    }
}
