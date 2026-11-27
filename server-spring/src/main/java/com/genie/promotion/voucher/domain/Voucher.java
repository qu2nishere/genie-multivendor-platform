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

    // ===== Vòng đời & quản lý (plan 2026-11-26 voucher D1–D4, D7, D8, D11, D18) =====

    /** Trạng thái quản lý tại {@code now} — {@link VoucherStatus} (D1). */
    public VoucherStatus status(OffsetDateTime now) {
        if (!Boolean.TRUE.equals(this.isActive)) return budgetExhausted() ? VoucherStatus.ENDED : VoucherStatus.DISABLED;
        if (now.isBefore(this.validFrom)) return VoucherStatus.SCHEDULED;
        if (!now.isBefore(this.validTo) || this.usedCount >= this.usageLimit || budgetExhausted()) return VoucherStatus.ENDED;
        return VoucherStatus.ACTIVE;
    }

    /** Ngân sách đã tiêu hết. */
    public boolean budgetExhausted() {
        return this.budget != null && this.totalDiscountUsed.compareTo(this.budget) >= 0;
    }

    /** Voucher shop: shop tài trợ, ưu tiên 1 (D6, D11) — UC-PRO-01 */
    public static Voucher createShop(long vendorUserId, VoucherTerms t) {
        Voucher v = fromTerms(t, VoucherType.SHOP, 1);
        v.vendorId = vendorUserId;
        v.sponsorType = FundingParty.VENDOR;
        v.validateConfig();
        return v;
    }

    /** Voucher sàn: FREESHIP khi giảm phí ship (ưu tiên 3), còn lại PLATFORM (ưu tiên 2); sàn hoặc đồng tài trợ (D7) — UC-PRO-02 */
    public static Voucher createPlatform(VoucherTerms t, FundingParty sponsor, BigDecimal platformSharePercent) {
        boolean freeship = t.discountType() == DiscountType.FREESHIP;
        Voucher v = fromTerms(t, freeship ? VoucherType.FREESHIP : VoucherType.PLATFORM, freeship ? 3 : 2);
        v.sponsorType = sponsor;
        v.platformSharePercent = sponsor == FundingParty.CO_SPONSOR ? platformSharePercent : null;
        v.validateConfig();
        return v;
    }

    private static Voucher fromTerms(VoucherTerms t, VoucherType type, int priority) {
        // FREESHIP: discount_value NOT NULL → lưu trần hỗ trợ (0 = hỗ trợ toàn bộ phí ship) — D8
        BigDecimal value = t.discountType() == DiscountType.FREESHIP
                ? (t.maxDiscountCap() == null ? BigDecimal.ZERO : t.maxDiscountCap()) : t.discountValue();
        return Voucher.builder().code(t.code()).title(t.title()).scope(t.scope()).type(type)
                .discountType(t.discountType()).discountValue(value).maxDiscountCap(t.maxDiscountCap())
                .minOrderValue(t.minOrderValue()).usageLimit(t.usageLimit()).usagePerUser(t.usagePerUser())
                .usedCount(0).priority(priority).validFrom(t.validFrom()).validTo(t.validTo()).isActive(true)
                .budget(t.budget()).totalDiscountUsed(BigDecimal.ZERO).notificationBody(t.notificationBody()).build();
    }

    /**
     * Sửa được mọi trường khi {@code SCHEDULED} và chưa ai lưu vào Kho (D3) — công bằng với người đã lưu: điều kiện họ đã
     * thấy không bị đổi.
     */
    public boolean fullyEditable(boolean hasSavers, OffsetDateTime now) {
        return status(now) == VoucherStatus.SCHEDULED && !hasSavers;
    }

    /**
     * Các trường bản nháp {@code draft} định đổi mà luật D3 khóa — đang chạy chỉ được đổi tiêu đề, nội dung thông báo,
     * KÉO DÀI {@code valid_to}, TĂNG {@code usage_limit} / {@code budget}. Đã kết thúc / đã tắt → {@code ["status"]}.
     * Rỗng = được sửa. Phạm vi (4 bảng) service tự so.
     */
    public List<String> lockedChanges(Voucher draft, boolean hasSavers, OffsetDateTime now) {
        VoucherStatus st = status(now);
        if (st == VoucherStatus.ENDED || st == VoucherStatus.DISABLED) return List.of("status");
        if (fullyEditable(hasSavers, now)) return List.of();
        List<String> locked = new ArrayList<>();
        if (!this.code.equals(draft.code)) locked.add("code");
        if (this.discountType != draft.discountType) locked.add("discount_type");
        if (!sameMoney(this.discountValue, draft.discountValue)) locked.add("discount_value");
        if (!sameMoney(this.maxDiscountCap, draft.maxDiscountCap)) locked.add("max_discount");
        if (!sameMoney(this.minOrderValue, draft.minOrderValue)) locked.add("min_order_value");
        if (!this.usagePerUser.equals(draft.usagePerUser)) locked.add("usage_per_user");
        if (!this.validFrom.isEqual(draft.validFrom)) locked.add("valid_from");
        if (draft.validTo.isBefore(this.validTo)) locked.add("valid_to");
        if (draft.usageLimit < this.usageLimit) locked.add("usage_limit");
        if (this.budget != null && (draft.budget == null ? false : draft.budget.compareTo(this.budget) < 0)) locked.add("budget");
        if (this.budget == null && draft.budget != null) locked.add("budget");
        if (this.scope != draft.scope) locked.add("scope");
        if (this.sponsorType != draft.sponsorType) locked.add("funded_by");
        if (!sameMoney(this.platformSharePercent, draft.platformSharePercent)) locked.add("platform_share_percent");
        return locked;
    }

    /** Chép bản nháp đã qua {@link #lockedChanges} (đủ trường khi {@code full}, ngược lại chỉ trường được phép) — UC-PRO-01, 02 */
    public void applyEdit(Voucher draft, boolean full) {
        this.title = draft.title;
        this.validTo = draft.validTo;
        this.usageLimit = draft.usageLimit;
        this.budget = draft.budget;
        this.notificationBody = draft.notificationBody;
        if (full) {
            this.code = draft.code;
            this.discountType = draft.discountType;
            this.discountValue = draft.discountValue;
            this.maxDiscountCap = draft.maxDiscountCap;
            this.minOrderValue = draft.minOrderValue;
            this.usagePerUser = draft.usagePerUser;
            this.validFrom = draft.validFrom;
            this.scope = draft.scope;
            this.type = draft.type;
            this.priority = draft.priority;
            this.sponsorType = draft.sponsorType;
            this.platformSharePercent = draft.platformSharePercent;
        }
        validateConfig();
    }

    /** Tắt một chiều (D4) — tắt bản SCHEDULED = huỷ lịch; tắt lại không lỗi — UC-PRO-01, 02 */
    public void disable() {
        this.isActive = false;
    }

    /** Xóa vĩnh viễn được: đã kết thúc / đã tắt và chưa có lượt dùng (D18) — UC-PRO-02 */
    public boolean isDeletable(boolean hasUsages, OffsetDateTime now) {
        VoucherStatus st = status(now);
        return (st == VoucherStatus.ENDED || st == VoucherStatus.DISABLED) && !hasUsages;
    }

    private static boolean sameMoney(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    /**
     * Kiểm tra cấu hình khi tạo/sửa: SHOP bắt buộc vendorId & shop tài trợ; PLATFORM / FREESHIP không gắn shop, sàn tài trợ
     * hoặc đồng tài trợ ({@code platformSharePercent} 1–99) — UC-PRO-01, UC-PRO-02 (plan 2026-11-26 voucher D7)
     */
    public void validateConfig() {
        if (this.type == VoucherType.SHOP && (this.vendorId == null || this.sponsorType != FundingParty.VENDOR))
            throw new IllegalArgumentException("Voucher SHOP phải có vendorId và sponsorType = VENDOR");
        if (this.type != VoucherType.SHOP) {
            if (this.vendorId != null || this.sponsorType == FundingParty.VENDOR)
                throw new IllegalArgumentException("Voucher sàn không gắn shop và sponsorType ∈ PLATFORM, CO_SPONSOR");
            boolean co = this.sponsorType == FundingParty.CO_SPONSOR;
            boolean shareOk = this.platformSharePercent != null && this.platformSharePercent.compareTo(BigDecimal.ONE) >= 0
                    && this.platformSharePercent.compareTo(BigDecimal.valueOf(99)) <= 0;
            if (co != shareOk || (!co && this.platformSharePercent != null))
                throw new IllegalArgumentException("platformSharePercent 1–99 chỉ dùng khi CO_SPONSOR");
        }
        if (!this.validTo.isAfter(this.validFrom)) throw new IllegalArgumentException("validTo phải sau validFrom");
    }
}
