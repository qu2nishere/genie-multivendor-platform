package com.genie.catalog.attribute.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import com.genie.catalog.category.domain.Category;

/**
 * CategoryAttribute — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "category_attributes", uniqueConstraints = {
    @UniqueConstraint(name = "uq_category_attribute_code", columnNames = {"category_id", "code"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CategoryAttribute {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "category_id", nullable = false)
    private Long categoryId;                  // FK → categories.id — thuộc tính khai ở ngành cha được ngành con kế thừa (Service duyệt cây danh mục)

    @Column(nullable = false, length = 50)
    private String code;                      // Khóa máy đọc, dùng làm key trong Product.specsJson (vd: "origin", "expiry_date", "storage_gb")

    @Column(nullable = false, length = 100)
    private String name;                      // Tên hiển thị: "Xuất xứ", "Hạn sử dụng"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttributeDataType dataType;       // TEXT / NUMBER / BOOLEAN / ENUM / MULTI_ENUM / DATE

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String allowedValuesJson;         // NULLABLE — Danh sách giá trị hợp lệ khi ENUM / MULTI_ENUM: ["Việt Nam","Nhật Bản",...]

    @Column(length = 20)
    private String unit;                      // NULLABLE — Đơn vị cho NUMBER: "GB", "ml", "kg"

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRequired = false;       // Bắt buộc nhập trước khi gửi duyệt (thông tin pháp lý: xuất xứ, hạn dùng, thành phần…)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isFilterable = false;     // Dùng làm bộ lọc trên trang tìm kiếm (truy vấn JSONB Product.specsJson, index GIN)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isVariantAxis = false;    // Là trục tạo biến thể SKU (Màu, Size) — giá trị nằm ở ProductVariant.attributesJson

    @Builder.Default
    @Column(nullable = false)
    private Integer displayOrder = 0;         // Thứ tự hiển thị trên form đăng bán & trang chi tiết

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;          // Ngừng dùng thuộc tính mà không làm hỏng dữ liệu cũ

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    // ===== QUAN HỆ =====
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;                // Ngành hàng sở hữu thuộc tính
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Kiểm tra giá trị người bán nhập (null hợp lệ khi không bắt buộc; ENUM phải thuộc danh sách — Service parse allowedValuesJson) — UC-CAT-09 */
    public boolean accepts(String rawValue, java.util.Set<String> allowedValues) {
        if (rawValue == null || rawValue.isBlank()) return !Boolean.TRUE.equals(this.isRequired);
        return switch (this.dataType) {
            case NUMBER -> rawValue.matches("-?\\d+(\\.\\d+)?");
            case BOOLEAN -> rawValue.equals("true") || rawValue.equals("false");
            case DATE -> rawValue.matches("\\d{4}-\\d{2}-\\d{2}");
            case ENUM -> allowedValues.contains(rawValue);
            case MULTI_ENUM -> java.util.Arrays.stream(rawValue.split(",")).map(String::trim).allMatch(allowedValues::contains);
            case TEXT -> rawValue.length() <= 500;
        };
    }

    /** Ngừng dùng thuộc tính (dữ liệu cũ trong specsJson giữ nguyên) — UC-CAT-15 */
    public void deactivate() {
        this.isActive = false;
        this.isRequired = false;
    }
}
