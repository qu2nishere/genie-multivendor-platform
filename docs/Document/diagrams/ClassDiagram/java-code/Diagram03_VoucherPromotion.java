/*
 * ============================================================================
 * GENIE — SƠ ĐỒ LỚP 03: Voucher 3 tầng Shop / Sàn / Freeship & Phạm vi áp dụng (Nhóm 4)
 * ============================================================================
 * Nguồn duy nhất : TLCN/diagrams/ClassDiagram/ENTITY.md (ENTITY.md TLCN v1.2) — sinh tự động, KHÔNG sửa tay;
 *                  sửa ENTITY.md rồi chạy lại công cụ sinh sơ đồ lớp (tools/classdiagram) để giữ đồng bộ.
 * Nhóm thực thể  : 04. NHÓM KHUYẾN MÃI, VOUCHER, FLASH SALE & ĐIỂM
 * Thành phần     : 7 @Entity, 4 @IdClass, 4 enum, 57 thuộc tính, 11 phương thức nghiệp vụ
 * Entity         : Voucher, VoucherUsage, UserVoucher, VoucherCategory, VoucherProduct, VoucherExcludedCategory, VoucherExcludedProduct
 * Enum           : VoucherScope, VoucherType, DiscountType, FundingParty
 * @IdClass       : VoucherCategoryId, VoucherProductId, VoucherExcludedCategoryId, VoucherExcludedProductId
 * Import vào EA  : Code > Import Source Directory / Import Source File (ngôn ngữ Java),
 *                  chọn cả thư mục java-code để EA nối được quan hệ giữa các sơ đồ.
 * Quy ước        : Getter/Setter do Lombok sinh — chỉ phương thức nghiệp vụ là ngăn Operations;
 *                  enum lấy nguyên văn từ Phụ lục A; @IdClass dựng từ các trường @Id của entity.
 */
package com.genie.platform.d03_voucher;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.hibernate.annotations.CreationTimestamp;
import java.time.*;
import java.util.*;
import com.genie.platform.d01_identity.User;
import com.genie.platform.d02_catalog_cart.Category;
import com.genie.platform.d02_catalog_cart.Product;

// ============================== ENUMS (4 — Phụ lục A) ==============================

public enum VoucherScope { ALL, CATEGORY, PRODUCT }   // ← Voucher.scope (CATEGORY / PRODUCT dùng 4 bảng nối mục 4.4)

public enum VoucherType { SHOP, PLATFORM, FREESHIP }   // ← Voucher.type, OrderDiscount.voucherType (3 tầng; mở rộng: PAYMENT — tầng 4)

public enum DiscountType { PERCENTAGE, FIXED_AMOUNT, FREESHIP }   // ← Voucher.discountType

public enum FundingParty { PLATFORM, VENDOR, CO_SPONSOR }   // ← Voucher.sponsorType, OrderDiscount.fundedBy (CO_SPONSOR: sàn + shop cùng chịu theo Voucher.platformSharePercent)

// ============================== ENTITIES (7) ==============================

// 4.1. Voucher — Mã giảm giá  —  Bảng: vouchers
@Entity
@Table(name = "vouchers")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Voucher {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false, length = 50)
    private String code;                      // Mã khách nhập: "SALE50K" — UNIQUE toàn hệ thống

    @Column(nullable = false, length = 255)
    private String title;                     // Tên chương trình hiển thị: "Giảm 50K cho đơn từ 300K"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoucherScope scope;               // Phạm vi MVP: ALL (mọi sản phẩm của shop / của đơn). Giữ cột để mở rộng CATEGORY / PRODUCT ở bản đầy đủ

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoucherType type;                 // MVP 2 tầng: SHOP (shop tài trợ, Tầng 1) / PLATFORM (sàn tài trợ, Tầng 2)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;        // PERCENTAGE (theo %) / FIXED_AMOUNT (số tiền cố định)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountValue;         // Giá trị giảm: 10 (nếu là %) hoặc 50000 (nếu là số tiền)

    @Column(precision = 19, scale = 2)
    private BigDecimal maxDiscountCap;        // NULLABLE — Trần giảm tối đa. VD giảm 50% nhưng không quá 100.000đ

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal minOrderValue;         // Giá trị đơn tối thiểu mới được dùng mã

    @Column(nullable = false)
    private Integer usageLimit;               // Tổng số lượt được dùng của TOÀN BỘ mã này

    @Column(nullable = false)
    private Integer usagePerUser;             // Số lượt tối đa MỖI NGƯỜI được dùng

    @Builder.Default
    @Column(nullable = false)
    private Integer usedCount = 0;            // Số lượt đã dùng thực tế (bộ đếm)

    @Column(nullable = false)
    private Integer priority;                 // Độ ưu tiên khi khách dùng nhiều mã cùng lúc (số lớn áp trước)

    private Long vendorId;                    // NULLABLE — FK → users.id. Có giá trị khi type = SHOP; NULL = mã của sàn

    @Column(nullable = false)
    private OffsetDateTime validFrom;          // Thời điểm bắt đầu hiệu lực

    @Column(nullable = false)
    private OffsetDateTime validTo;            // Thời điểm hết hiệu lực

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;          // Công tắc bật/tắt thủ công (vòng đời logic: SCHEDULED nếu now < validFrom; ACTIVE nếu isActive=true && validFrom <= now <= validTo; EXPIRED nếu now > validTo)

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống phát vượt số lượt khi nhiều người cùng dùng

    @Column(precision = 19, scale = 2)
    private BigDecimal budget;                // NULLABLE — Trần NGÂN SÁCH tính bằng tiền cho cả chiến dịch

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDiscountUsed = BigDecimal.ZERO; // Tổng số tiền đã giảm tích luỹ — đối chiếu với budget

    @Column(columnDefinition = "TEXT")
    private String notificationBody;          // NULLABLE — Nội dung thông báo đẩy khi tung mã mới

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FundingParty sponsorType;         // Ai chi trả khoản giảm: PLATFORM (sàn) hay VENDOR (shop)

    @Column(precision = 5, scale = 2)
    private BigDecimal platformSharePercent;  // NULLABLE — Chỉ khi sponsorType = CO_SPONSOR: % sàn chịu, phần còn lại shop chịu

    // ===== Phạm vi áp dụng (scope = CATEGORY / PRODUCT) và danh sách loại trừ =====
    @OneToMany
    @JoinColumn(name = "voucher_id", insertable = false, updatable = false)
    private List<VoucherCategory> categories;          // Danh mục ĐƯỢC áp dụng

    @OneToMany
    @JoinColumn(name = "voucher_id", insertable = false, updatable = false)
    private List<VoucherProduct> products;             // Sản phẩm ĐƯỢC áp dụng

    @OneToMany
    @JoinColumn(name = "voucher_id", insertable = false, updatable = false)
    private List<VoucherExcludedCategory> excludedCategories; // Danh mục BỊ LOẠI TRỪ

    @OneToMany
    @JoinColumn(name = "voucher_id", insertable = false, updatable = false)
    private List<VoucherExcludedProduct> excludedProducts;    // Sản phẩm BỊ LOẠI TRỪ

    // ===== QUAN HỆ =====

    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Kiểm tra điều kiện áp dụng (thời gian, lượt, đơn tối thiểu) — UC-PRO-03 */
    public boolean isApplicable(BigDecimal orderValue, OffsetDateTime now) {
        return Boolean.TRUE.equals(this.isActive) && !now.isBefore(this.validFrom) && now.isBefore(this.validTo)
            && (this.usageLimit == null || this.usedCount < this.usageLimit)
            && orderValue.compareTo(this.minOrderValue) >= 0;
    }

    /** Tính số tiền giảm (có trần, không vượt giá trị) — UC-PRO-03 */
    public BigDecimal calculateDiscount(BigDecimal base) {
        BigDecimal d = this.discountType == DiscountType.PERCENTAGE
            ? base.multiply(this.discountValue).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
            : this.discountValue;
        return this.maxDiscountCap == null ? d.min(base) : d.min(this.maxDiscountCap).min(base);
    }

    /** Ghi nhận một lượt dùng (service dùng @Version), hết ngân sách tự tắt — UC-PRO-03 */
    public void consume(BigDecimal discount) {
        if (this.usageLimit != null && this.usedCount >= this.usageLimit) throw new IllegalStateException("GNE-VCH-4221");
        this.usedCount += 1;
        this.totalDiscountUsed = this.totalDiscountUsed.add(discount);
        if (this.budget != null && this.totalDiscountUsed.compareTo(this.budget) >= 0) this.isActive = false;
    }

    /** Hoàn lượt khi đơn bị hủy / hết hạn thanh toán — UC-ORD-08 */
    public void restore(BigDecimal discount) {
        this.usedCount = Math.max(0, this.usedCount - 1);
        this.totalDiscountUsed = this.totalDiscountUsed.subtract(discount).max(BigDecimal.ZERO);
    }

    /** Kiểm tra cấu hình khi tạo/sửa: SHOP bắt buộc vendorId & shop tài trợ; PLATFORM không gắn shop & sàn tài trợ — UC-PRO-01, UC-PRO-02 */
    public void validateConfig() {
        if (this.type == VoucherType.SHOP && (this.vendorId == null || this.sponsorType != FundingParty.VENDOR))
            throw new IllegalArgumentException("Voucher SHOP phải có vendorId và sponsorType = VENDOR");
        if (this.type == VoucherType.PLATFORM && (this.vendorId != null || this.sponsorType != FundingParty.PLATFORM))
            throw new IllegalArgumentException("Voucher PLATFORM không gắn shop và sponsorType = PLATFORM");
        if (this.validTo.isBefore(this.validFrom)) throw new IllegalArgumentException("validTo phải sau validFrom");
    }
}

// 4.2. VoucherUsage — Lịch sử sử dụng voucher  —  Bảng: voucher_usages
@Entity
@Table(name = "voucher_usages", uniqueConstraints = {
    @UniqueConstraint(name = "uq_voucher_usage_session", columnNames = {"voucher_id", "checkout_session_id"})   // 1 voucher chỉ trừ 1 lượt / phiên thanh toán
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VoucherUsage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long voucherId;                   // FK → vouchers.id — mã nào đã được dùng

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — ai dùng

    @Column(nullable = false)
    private Long checkoutSessionId;           // FK → checkout_sessions.id — phiên thanh toán đã dùng mã (đơn vị tính lượt dùng)

    private Long orderId;                     // NULLABLE — FK → orders.id. CÓ giá trị với voucher SHOP (đúng 1 đơn con của shop đó); NULL với voucher PLATFORM / FREESHIP / PAYMENT áp toàn phiên (nhiều đơn con)

    @Column(nullable = false)
    private OffsetDateTime usedAt;             // Thời điểm sử dụng

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "voucher_id", insertable = false, updatable = false)
    private Voucher voucher;                  // Voucher được dùng
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tạo bản ghi lượt dùng — 1 bản ghi / voucher / phiên (UNIQUE voucher+session); shopOrderId chỉ truyền với voucher SHOP — UC-PRO-03 */
    public static VoucherUsage of(Long voucherId, Long userId, Long checkoutSessionId, Long shopOrderId, OffsetDateTime now) {
        VoucherUsage u = new VoucherUsage();
        u.voucherId = voucherId; u.userId = userId; u.checkoutSessionId = checkoutSessionId; u.orderId = shopOrderId; u.usedAt = now;
        return u;
    }
}

// 4.3. UserVoucher — Ví voucher của người dùng  —  Bảng: user_vouchers
@Entity
@Table(name = "user_vouchers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "voucher_id"}))
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class UserVoucher {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — chủ ví

    @Column(nullable = false)
    private Long voucherId;                   // FK → vouchers.id — mã đã lưu vào ví

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm user bấm "Lưu mã"

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false) @JoinColumn(name = "voucher_id", insertable = false, updatable = false)
    private Voucher voucher;                  // Chi tiết voucher

    @ManyToOne(optional = false) @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;                        // Chủ sở hữu

    // ---- Trường tính toán, KHÔNG lưu DB ----
    @Transient
    private Boolean isUsed;                   // true = user đã dùng hết lượt cho phép của mã này
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đánh dấu voucher trong ví đã dùng — UC-PRO-03 */
    public void markUsed() {
        if (Boolean.TRUE.equals(this.isUsed)) throw new IllegalStateException("Voucher đã dùng");
        this.isUsed = true;
    }
}

// 4.4. VoucherCategory / VoucherProduct / VoucherExcludedCategory / VoucherExcludedProduct  —  Bảng: voucher_categories
@Entity
@Table(name = "voucher_categories")
@IdClass(VoucherCategoryId.class)             // Khoá chính GỘP (composite key) 2 cột
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VoucherCategory {

    @Id
    private Long voucherId;                   // FK → vouchers.id — phần 1 của khoá chính

    @Id
    private Long categoryId;                  // FK → categories.id — phần 2 của khoá chính

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm gắn danh mục ĐƯỢC áp dụng voucher (@CreationTimestamp)

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;                // Danh mục ĐƯỢC áp dụng voucher
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Phạm vi áp dụng của voucher — UC-PRO-01 */
    public boolean matches(Long categoryId) {
        return this.categoryId.equals(categoryId);
    }
}

// 4.4. VoucherCategory / VoucherProduct / VoucherExcludedCategory / VoucherExcludedProduct  —  Bảng: voucher_products
@Entity
@Table(name = "voucher_products")
@IdClass(VoucherProductId.class)              // Khoá chính GỘP (composite key) 2 cột
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VoucherProduct {

    @Id
    private Long voucherId;                   // FK → vouchers.id — phần 1 của khoá chính

    @Id
    private Long productId;                   // FK → products.id — phần 2 của khoá chính

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm gắn sản phẩm ĐƯỢC áp dụng voucher (@CreationTimestamp)

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;                  // Sản phẩm ĐƯỢC áp dụng voucher
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Phạm vi áp dụng của voucher — UC-PRO-01 */
    public boolean matches(Long productId) {
        return this.productId.equals(productId);
    }
}

// 4.4. VoucherCategory / VoucherProduct / VoucherExcludedCategory / VoucherExcludedProduct  —  Bảng: voucher_excluded_categories
@Entity
@Table(name = "voucher_excluded_categories")
@IdClass(VoucherExcludedCategoryId.class)     // Khoá chính GỘP (composite key) 2 cột
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VoucherExcludedCategory {

    @Id
    private Long voucherId;                   // FK → vouchers.id — phần 1 của khoá chính

    @Id
    private Long categoryId;                  // FK → categories.id — phần 2 của khoá chính

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm gắn danh mục BỊ LOẠI TRỪ khỏi voucher (@CreationTimestamp)

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;                // Danh mục BỊ LOẠI TRỪ khỏi voucher
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Phạm vi loại trừ của voucher — UC-PRO-01 */
    public boolean matches(Long categoryId) {
        return this.categoryId.equals(categoryId);
    }
}

// 4.4. VoucherCategory / VoucherProduct / VoucherExcludedCategory / VoucherExcludedProduct  —  Bảng: voucher_excluded_products
@Entity
@Table(name = "voucher_excluded_products")
@IdClass(VoucherExcludedProductId.class)      // Khoá chính GỘP (composite key) 2 cột
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VoucherExcludedProduct {

    @Id
    private Long voucherId;                   // FK → vouchers.id — phần 1 của khoá chính

    @Id
    private Long productId;                   // FK → products.id — phần 2 của khoá chính

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm gắn sản phẩm BỊ LOẠI TRỪ khỏi voucher (@CreationTimestamp)

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;                  // Sản phẩm BỊ LOẠI TRỪ khỏi voucher
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Phạm vi loại trừ của voucher — UC-PRO-01 */
    public boolean matches(Long productId) {
        return this.productId.equals(productId);
    }
}

// ============================== KHÓA CHÍNH GỘP @IdClass (4) ==============================

/** Khóa chính gộp (@IdClass) của VoucherCategory — voucherId + categoryId (ENTITY.md 4.4) */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class VoucherCategoryId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long voucherId;  // FK → vouchers.id — phần 1 của khoá chính
    private Long categoryId;  // FK → categories.id — phần 2 của khoá chính
}

/** Khóa chính gộp (@IdClass) của VoucherProduct — voucherId + productId (ENTITY.md 4.4) */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class VoucherProductId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long voucherId;  // FK → vouchers.id — phần 1 của khoá chính
    private Long productId;  // FK → products.id — phần 2 của khoá chính
}

/** Khóa chính gộp (@IdClass) của VoucherExcludedCategory — voucherId + categoryId (ENTITY.md 4.4) */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class VoucherExcludedCategoryId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long voucherId;  // FK → vouchers.id — phần 1 của khoá chính
    private Long categoryId;  // FK → categories.id — phần 2 của khoá chính
}

/** Khóa chính gộp (@IdClass) của VoucherExcludedProduct — voucherId + productId (ENTITY.md 4.4) */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class VoucherExcludedProductId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long voucherId;  // FK → vouchers.id — phần 1 của khoá chính
    private Long productId;  // FK → products.id — phần 2 của khoá chính
}
