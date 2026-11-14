package com.genie.promotion.voucher.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import com.genie.common.domain.FundingParty;
import com.genie.common.domain.VoucherType;

/**
 * Voucher — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
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
