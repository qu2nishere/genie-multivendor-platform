/*
 * ============================================================================
 * GENIE — SƠ ĐỒ LỚP 02: Danh mục, Thương hiệu, Sản phẩm, Biến thể SKU, Yêu thích & Giỏ hàng (Nhóm 2–3)
 * ============================================================================
 * Nguồn duy nhất : TLCN/diagrams/ClassDiagram/ENTITY.md (ENTITY.md TLCN v1.3) — sinh tự động, KHÔNG sửa tay;
 *                  sửa ENTITY.md rồi chạy lại công cụ sinh sơ đồ lớp (tools/classdiagram) để giữ đồng bộ.
 * Nhóm thực thể  : 02. NHÓM DANH MỤC & SẢN PHẨM, 03. NHÓM GIỎ HÀNG
 * Thành phần     : 9 @Entity, 0 @IdClass, 4 enum, 109 thuộc tính, 24 phương thức nghiệp vụ
 * Entity         : Category, Product, ProductVariant, ProductImage, UserWishlist, CategoryAttribute, Brand, Cart, CartItem
 * Enum           : ProductStatus, ShippingPayer, AttributeDataType, ProductCondition
 * Import vào EA  : Code > Import Source Directory / Import Source File (ngôn ngữ Java),
 *                  chọn cả thư mục java-code để EA nối được quan hệ giữa các sơ đồ.
 * Quy ước        : Getter/Setter do Lombok sinh — chỉ phương thức nghiệp vụ là ngăn Operations;
 *                  enum lấy nguyên văn từ Phụ lục A; @IdClass dựng từ các trường @Id của entity.
 */
package com.genie.platform.d02_catalog_cart;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.*;
import java.util.*;
import com.genie.platform.d01_identity.VendorProfile;

// ============================== ENUMS (4 — Phụ lục A) ==============================

public enum ProductStatus {                                                                    // ← Product.status (FLAGGED: kiểm duyệt tự động đánh dấu nghi vấn — UC-CAT-12)
    DRAFT, PENDING_APPROVAL, PUBLISHED, HIDDEN, REJECTED, FLAGGED, DELETED
}

public enum ShippingPayer { BUYER, SELLER }   // ← Product.shippingPayer

public enum AttributeDataType { TEXT, NUMBER, BOOLEAN, ENUM, MULTI_ENUM, DATE }   // ← CategoryAttribute.dataType

public enum ProductCondition { NEW, USED, REFURBISHED }   // ← Product.itemCondition (tình trạng hàng)

// ============================== ENTITIES (9) ==============================

// 2.1. Category — Danh mục ngành hàng  —  Bảng: categories
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

// 2.2. Product — Sản phẩm (SPU)  —  Bảng: products
@Entity
@Table(name = "products")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id (của vendor) — sản phẩm này thuộc shop nào

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "vendor_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private VendorProfile vendorProfile;      // Hồ sơ shop bán sản phẩm này
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

// 2.3. ProductVariant — Biến thể sản phẩm (SKU)  —  Bảng: product_variants
@Entity
@Table(name = "product_variants")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductVariant {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long productId;                   // FK → products.id — biến thể này thuộc sản phẩm nào

    @Column(unique = true, nullable = false, length = 100)
    private String sku;                       // Mã SKU duy nhất toàn hệ thống — mã kho của đơn vị bán

    @Column(length = 255)
    private String variantName;               // NULLABLE — Tên hiển thị của biến thể: "Đen - Size L"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String attributesJson;            // NULLABLE — JSON thuộc tính: {"Màu":"Đen","Size":"L"}

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;                 // GIÁ BÁN THẬT của biến thể này

    @Column(nullable = false)
    private Integer stockCount;               // Tổng tồn kho vật lý đang có

    @Builder.Default
    @Column(nullable = false)
    private Integer reservedCount = 0;        // Số lượng đang bị GIỮ CHỖ cho các đơn chưa thanh toán xong

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — lá chắn chống bán vượt tồn kho (overselling)

    @Column(length = 500)
    private String imageUrl;                  // NULLABLE — Ảnh riêng của biến thể này

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;          // NULLABLE — Xoá mềm (@SQLDelete + @SQLRestriction). NULL = chưa xoá

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;                  // Sản phẩm cha
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tồn khả dụng = tồn vật lý − đang giữ chỗ — UC-CAT-06 */
    public int availableStock() {
        return Math.max(0, this.stockCount - this.reservedCount);
    }

    /** Giữ chỗ mềm (service dùng UPDATE có điều kiện) — UC-ORD-07 */
    public void reserve(int qty) {
        if (availableStock() < qty) throw new IllegalStateException("GNE-INV-4090");   // hết tồn kho
        this.reservedCount += qty;
    }

    /** Nhả giữ chỗ (chỉ gọi sau khi StockReservation.release() thành công) — UC-ORD-08 */
    public void releaseReservation(int qty) {
        if (this.reservedCount < qty) throw new IllegalStateException("reservedCount không đủ để nhả — giữ chỗ đã được xử lý");
        this.reservedCount -= qty;
    }

    /** Trừ tồn cứng khi PAID — chỉ gọi sau StockReservation.confirm() (ACTIVE) để chống đua với Sweeper — UC-PAY-03 */
    public void commitSale(int qty) {
        if (this.reservedCount < qty || this.stockCount < qty) throw new IllegalStateException("GNE-INV-4093");
        this.reservedCount -= qty;
        this.stockCount -= qty;
    }

    /** Xóa mềm biến thể: chặn khi còn giữ chỗ của đơn đang xử lý; còn tồn thì chỉ Admin được ép xóa (force) — UC-CAT-11 */
    public void softDelete(OffsetDateTime now, boolean force) {
        if (this.reservedCount > 0) throw new IllegalStateException("Biến thể đang được giữ chỗ cho " + this.reservedCount + " sản phẩm trong đơn chưa thanh toán");
        if (this.stockCount > 0 && !force) throw new IllegalStateException("Còn " + this.stockCount + " tồn kho — đưa tồn về 0 hoặc Admin xóa cưỡng bức");
        this.deletedAt = now;
    }
}

// 2.4. ProductImage — Ảnh sản phẩm  —  Bảng: product_images
@Entity
@Table(name = "product_images")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long productId;                   // FK → products.id — ảnh thuộc sản phẩm nào

    private Long variantId;                   // NULLABLE — FK → product_variants.id. NULL = ảnh trong gallery chung của sản phẩm; khác NULL = ảnh RIÊNG của biến thể (chỉ hiện khi khách chọn biến thể đó, KHÔNG xuất hiện trong gallery chung)

    @Column(nullable = false, length = 500)
    private String imageUrl;                  // Link ảnh gốc trên storage (S3/Silo)

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

// 2.5. UserWishlist — Danh sách sản phẩm yêu thích  —  Bảng: user_wishlists
@Entity
@Table(name = "user_wishlists", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "productId"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class UserWishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // ID người dùng thả tim yêu thích

    @Column(nullable = false)
    private Long productId;                   // ID sản phẩm được lưu lại

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm thêm vào danh sách yêu thích
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.
}

// 2.6. CategoryAttribute — Bộ thuộc tính chuẩn theo ngành hàng  —  Bảng: category_attributes
@Entity
@Table(name = "category_attributes", uniqueConstraints = {
    @UniqueConstraint(name = "uq_category_attribute_code", columnNames = {"category_id", "code"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CategoryAttribute {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
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

// 2.7. Brand — Thương hiệu  —  Bảng: brands
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

// 3.1. Cart — Giỏ hàng  —  Bảng: carts
@Entity
@Table(name = "carts")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Cart {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long userId;                      // FK → users.id, UNIQUE — mỗi user có đúng 1 giỏ hàng

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Lần cuối giỏ hàng thay đổi — dùng cho email nhắc "bỏ quên giỏ hàng"

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "cart_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua CartItem.cartId
    private List<CartItem> items;             // Các dòng sản phẩm trong giỏ
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Thêm sản phẩm (gộp nếu trùng SKU) — UC-ORD-01 */
    public void addItem(CartItem item) {
        this.items.stream().filter(i -> i.getProductVariantId().equals(item.getProductVariantId())).findFirst()
            .ifPresentOrElse(i -> i.increase(item.getQuantity()), () -> this.items.add(item));
    }

    /** Xóa dòng hàng — UC-ORD-01 */
    public void removeItem(Long productVariantId) {
        this.items.removeIf(i -> i.getProductVariantId().equals(productVariantId));
    }

    /** Tạm tính theo giá snapshot — UC-ORD-04 */
    public BigDecimal subtotal() {
        return this.items.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Gộp giỏ tạm của khách vãng lai (lưu trên trình duyệt) vào giỏ tài khoản khi đăng nhập / đăng ký; service đã lọc SKU ngừng bán và giới hạn số lượng theo tồn kho trước khi gọi — UC-ORD-01 */
    public void mergeGuestItems(List<CartItem> guestItems) {
        guestItems.forEach(this::addItem);
    }
}

// 3.2. CartItem — Dòng sản phẩm trong giỏ  —  Bảng: cart_items
@Entity
@Table(name = "cart_items")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CartItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long cartId;                      // FK → carts.id — thuộc giỏ hàng nào

    @Column(nullable = false)
    private Long productVariantId;            // FK → product_variants.id — mua BIẾN THỂ nào (không phải sản phẩm chung)

    @Column(nullable = false)
    private Integer quantity;                 // Số lượng khách muốn mua

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal priceSnapshot;         // Giá tại thời điểm THÊM VÀO GIỎ — để phát hiện biến động giá

    @Column(nullable = false)
    private OffsetDateTime addedAt;            // Thời điểm thêm vào giỏ

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_variant_id", insertable = false, updatable = false)
    private ProductVariant variant;           // Biến thể sản phẩm tương ứng
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tăng số lượng — UC-ORD-01 */
    public void increase(int qty) {
        this.quantity += qty;
    }

    /** Thành tiền dòng — UC-ORD-04 */
    public BigDecimal lineTotal() {
        return this.priceSnapshot.multiply(BigDecimal.valueOf(this.quantity));
    }
}
