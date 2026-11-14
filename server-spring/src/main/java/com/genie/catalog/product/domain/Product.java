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
import com.genie.catalog.category.domain.Category;

/**
 * Product — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 * D2: bỏ trường điều hướng sang module khác: vendorProfile — dùng cột Long *Id tương ứng.
 */
@Entity
@Table(name = "products")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id (của vendor) — sản phẩm này thuộc shop nào

    @Column(name = "category_id")   // D8: trùng tên logic với @JoinColumn chỉ-đọc
    private Long categoryId;                  // NULLABLE — FK → categories.id, danh mục của sản phẩm

    @Column(nullable = false, length = 255)
    private String name;                      // Tên sản phẩm hiển thị

    @Column(columnDefinition = "TEXT")
    private String description;               // NULLABLE — Mô tả chi tiết, có thể chứa HTML

    @Column(length = 255)
    private String brand;                     // NULLABLE — Thương hiệu: "Samsung", "Nike"

    @Column(length = 50)
    private String condition;                 // NULLABLE — Tình trạng: "NEW" (mới) / "USED" (đã qua sử dụng)

    @Column(length = 100)
    private String origin;                    // NULLABLE — Xuất xứ: "Việt Nam", "Trung Quốc"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String specsJson;                 // NULLABLE — Mảng JSON chứa thông số kỹ thuật (RAM, màn hình, chất liệu...)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String categoryJson;              // NULLABLE — Mảng JSON lưu ĐƯỜNG DẪN danh mục đầy đủ (breadcrumb)

    // ---- Thông tin phục vụ vận chuyển ----
    private Double weight;                    // NULLABLE — Khối lượng (gram) — bắt buộc để GHN tính cước

    private Double length;                    // NULLABLE — Chiều dài kiện hàng (cm)

    private Double width;                     // NULLABLE — Chiều rộng kiện hàng (cm)

    private Double height;                    // NULLABLE — Chiều cao kiện hàng (cm)

    private String shipsFrom;                 // NULLABLE — Nơi gửi hàng hiển thị cho khách

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ShippingPayer shippingPayer;      // NULLABLE — Ai trả phí ship: BUYER (khách) / SELLER (shop)

    @Column(precision = 15, scale = 2)
    private BigDecimal shippingFeeSupport;    // NULLABLE — Số tiền shop hỗ trợ phí ship (chuẩn hóa DECIMAL thay cho Double theo database-schema-designer)

    // ---- Giá & trạng thái ----
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal basePrice;             // Giá gốc/giá hiển thị của sản phẩm (giá thật nằm ở từng variant)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;             // DRAFT / PENDING_APPROVAL / PUBLISHED / HIDDEN / REJECTED / FLAGGED / DELETED

    @Column(columnDefinition = "TEXT")
    private String moderationReason;          // NULLABLE — Lý do bị từ chối/gắn cờ, để vendor biết đường sửa

    private Long moderatorId;                 // NULLABLE — FK mềm → users.id, staff được phân công duyệt sản phẩm này

    private Boolean isPublished;              // NULLABLE — Cờ phụ đánh dấu đã từng được đăng bán

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống ghi đè khi vendor và staff cùng sửa

    @Column(length = 255)
    private String variantImageAttribute;     // NULLABLE — Tên thuộc tính quyết định ảnh biến thể, VD "Màu sắc"

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo sản phẩm

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật gần nhất

    // ---- Trường TÍNH TOÁN, KHÔNG lưu trong DB ----
    @Transient
    private Integer sold;                     // Số lượng đã bán — tính từ bảng order_items

    @Transient
    private Double rating;                    // Điểm đánh giá trung bình — tính từ bảng product_reviews

    // ---- Thông tin bổ sung theo form đăng bán (đối chiếu giao diện 2026-10-29) ----
    private Long brandId;                     // NULLABLE — FK → brands.id; NULL = "No Brand" (UC-CAT-09)

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private ProductCondition itemCondition = ProductCondition.NEW; // Tình trạng: NEW / USED / REFURBISHED

    @Builder.Default
    @Column(nullable = false)
    private Boolean isPreOrder = false;       // Sản phẩm đặt trước (chỉ khi VendorProfile.preOrderEnabled)

    private Integer preOrderDays;             // NULLABLE — Số ngày chuẩn bị hàng của sản phẩm đặt trước (7–30); NULL khi không đặt trước

    @Builder.Default
    @Column(nullable = false)
    private Long viewCount = 0L;              // Lượt xem tích lũy (flush từ Redis product:{id}:views; chi tiết theo ngày ở product_view_stats)


    // ===== QUAN HỆ =====
    @ManyToOne
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;                // NULLABLE — Danh mục chứa sản phẩm (theo categoryId)

    @OneToMany(mappedBy = "product")
    private List<ProductVariant> variants;    // Các biến thể (SKU) — nơi chứa giá và tồn kho thật

    @OneToMany
    @JoinColumn(name = "product_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua ProductImage.productId
    private List<ProductImage> images;        // Bộ ảnh của sản phẩm

    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Gửi duyệt sản phẩm (shop đã cấu hình kho lấy hàng — Service kiểm tra isPickupConfigured()) — UC-CAT-09 */
    public void submitForReview() {
        if (this.status != ProductStatus.DRAFT && this.status != ProductStatus.REJECTED)
            throw new IllegalStateException("Chỉ gửi duyệt sản phẩm ở trạng thái DRAFT / REJECTED");
        this.status = ProductStatus.PENDING_APPROVAL;
    }

    /** Duyệt và đăng bán — UC-CAT-13 */
    public void approve(Long moderatorId) {
        this.status = ProductStatus.PUBLISHED;
        this.isPublished = true;
        this.moderatorId = moderatorId;
        this.moderationReason = null;
    }

    /** Từ chối kèm lý do — UC-CAT-13 */
    public void reject(Long moderatorId, String reason) {
        this.status = ProductStatus.REJECTED;
        this.isPublished = false;
        this.moderatorId = moderatorId;
        this.moderationReason = reason;
    }

    /** Shop tự ẩn sản phẩm — UC-CAT-11 */
    public void hide() {
        this.status = ProductStatus.HIDDEN;
        this.isPublished = false;
    }

    /** Bật đặt trước: shop phải cho phép và số ngày chuẩn bị 7–30 — UC-CAT-09 */
    public void markPreOrder(int days, boolean shopAllows) {
        if (!shopAllows) throw new IllegalStateException("Shop chưa bật chế độ đặt trước");
        if (days < 7 || days > 30) throw new IllegalArgumentException("Số ngày chuẩn bị phải từ 7 đến 30");
        this.isPreOrder = true;
        this.preOrderDays = days;
    }


    /** Kiểm duyệt tự động đánh dấu nghi vấn → chờ Staff duyệt thủ công — UC-CAT-12 */
    public void flag(String reason) {
        this.status = ProductStatus.FLAGGED;
        this.isPublished = false;
        this.moderationReason = reason;
    }

}
