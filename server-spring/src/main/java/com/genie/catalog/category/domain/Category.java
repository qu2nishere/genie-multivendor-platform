package com.genie.catalog.category.domain;

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
 * Category — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "categories")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Category {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false, length = 255)
    private String name;                      // Tên danh mục hiển thị: "Điện thoại", "Thời trang nam"

    @Column(length = 255)
    private String slug;                      // NULLABLE — Chuỗi thân thiện URL: "dien-thoai" → /danh-muc/dien-thoai

    @Column(name = "parent_id")   // D8: trùng tên logic với @JoinColumn chỉ-đọc
    private Long parentId;                    // NULLABLE — FK → chính bảng này. NULL = danh mục gốc (cấp 1)

    private Integer displayOrder;             // NULLABLE — Thứ tự sắp xếp khi hiển thị (số nhỏ lên trước)

    @Column(length = 50)
    private String icon;                      // NULLABLE — Tên/mã icon hiển thị trên giao diện

    @Column(length = 500)
    private String description;               // NULLABLE — Mô tả ngắn hiển thị ở đầu trang danh mục — UC-CAT-01 (TLCN v1.3)

    @Column(precision = 5, scale = 4)
    private BigDecimal commissionRate;        // NULLABLE — Hoa hồng mặc định theo ngành hàng, cùng kiểu với VendorProfile/OrderItem (vd 0.0350 = 3.5%, khung 0.0350–0.0800)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;          // true = đang hiển thị; false = ẩn khỏi menu nhưng giữ dữ liệu

    @Builder.Default
    @Column(nullable = false)
    private Boolean requiresBusinessLicense = false; // true = ngành hàng có điều kiện (thực phẩm, mỹ phẩm, thiết bị điện…) — sản phẩm phải kèm giấy tờ chứng thực (product_documents) mới được gửi duyệt

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua Category.parentId
    private List<Category> subCategories;     // Các danh mục con trực tiếp (đệ quy)
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Danh mục gốc — UC-CAT-01 */
    public boolean isRoot() {
        return this.parentId == null;
    }

    /** Đổi tỷ lệ hoa hồng ngành (chỉ áp dụng đơn mới) — UC-CAT-14 */
    public void changeCommissionRate(BigDecimal rate) {
        if (rate.compareTo(new BigDecimal("0.0350")) < 0 || rate.compareTo(new BigDecimal("0.0800")) > 0)
            throw new IllegalArgumentException("Hoa hồng phải trong khung 3.5% – 8%");
        this.commissionRate = rate;
    }
}
