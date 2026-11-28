// Dạng trả về của màn quản lý voucher — Seller › Khuyến mãi (API_SPEC 10.7) và Admin › Voucher sàn (11.14)
package com.genie.promotion.voucher.application;

import com.genie.promotion.voucher.domain.DiscountType;
import com.genie.promotion.voucher.domain.Voucher;
import com.genie.promotion.voucher.domain.VoucherStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Tiền trả số nguyên VND, phần trăm là số ({@code 15}, {@code 7.5}). */
public final class VoucherAdminViews {

    /** Trường của form; khi voucher đang chạy chỉ những trường này còn sửa được (D3). */
    static final List<String> RUNNING_EDITABLE = List.of("title", "valid_to", "usage_limit", "budget", "notification_body");
    static final List<String> ALL_FIELDS = List.of("title", "code", "discount_type", "discount_value", "max_discount",
            "min_order_value", "usage_limit", "usage_per_user", "budget", "valid_from", "valid_to", "scope", "category_ids",
            "product_ids", "excluded_category_ids", "excluded_product_ids", "funded_by", "platform_share_percent",
            "notification_body");

    private VoucherAdminViews() {
    }

    public record Usage(int used, int limit) {
    }

    /** Một dòng bảng (10.7 / 11.14 — siêu tập hai bảng). */
    public record Row(long id, String code, String title, String type, String discountType, BigDecimal discountValue,
                      Long maxDiscount, long minOrderValue, Usage usage, int usagePerUser, Long budget,
                      long totalDiscountUsed, OffsetDateTime validFrom, OffsetDateTime validTo, String status,
                      String scope, String fundedBy, BigDecimal platformSharePercent, boolean isCoinReward) {
    }

    /** Form sửa / nhân bản: dòng + 4 tập phạm vi + trường đang bị khóa. */
    public record Detail(long id, String code, String title, String type, String discountType, BigDecimal discountValue,
                         Long maxDiscount, long minOrderValue, Usage usage, int usageLimit, int usagePerUser, Long budget,
                         long totalDiscountUsed, OffsetDateTime validFrom, OffsetDateTime validTo, String status,
                         String scope, List<Long> categoryIds, List<Long> productIds, List<Long> excludedCategoryIds,
                         List<Long> excludedProductIds, String fundedBy, BigDecimal platformSharePercent,
                         String notificationBody, boolean isCoinReward, List<String> lockedFields) {
    }

    public static Row row(Voucher v, OffsetDateTime now, boolean coinReward) {
        return new Row(v.getId(), v.getCode(), v.getTitle(), v.getType().name(), v.getDiscountType().name(),
                discountValue(v), money(v.getMaxDiscountCap()), v.getMinOrderValue().longValue(),
                new Usage(v.getUsedCount(), v.getUsageLimit()), v.getUsagePerUser(), money(v.getBudget()),
                v.getTotalDiscountUsed().longValue(), v.getValidFrom(), v.getValidTo(), v.status(now).name(),
                v.getScope().name(), v.getSponsorType().name(), plain(v.getPlatformSharePercent()), coinReward);
    }

    public static Detail detail(Voucher v, VoucherScopes.Sets sets, OffsetDateTime now, boolean hasSavers,
                                boolean coinReward) {
        return new Detail(v.getId(), v.getCode(), v.getTitle(), v.getType().name(), v.getDiscountType().name(),
                discountValue(v), money(v.getMaxDiscountCap()), v.getMinOrderValue().longValue(),
                new Usage(v.getUsedCount(), v.getUsageLimit()), v.getUsageLimit(), v.getUsagePerUser(),
                money(v.getBudget()), v.getTotalDiscountUsed().longValue(), v.getValidFrom(), v.getValidTo(),
                v.status(now).name(), v.getScope().name(), List.copyOf(sets.categoryIds()), List.copyOf(sets.productIds()),
                List.copyOf(sets.excludedCategoryIds()), List.copyOf(sets.excludedProductIds()), v.getSponsorType().name(),
                plain(v.getPlatformSharePercent()), v.getNotificationBody(), coinReward,
                lockedFields(v, now, hasSavers, coinReward));
    }

    /** Trường form đang khóa: [] = sửa tự do; đã kết thúc / đã tắt = tất cả; quà đổi Điểm luôn khóa mã (D12). */
    static List<String> lockedFields(Voucher v, OffsetDateTime now, boolean hasSavers, boolean coinReward) {
        VoucherStatus st = v.status(now);
        if (st == VoucherStatus.ENDED || st == VoucherStatus.DISABLED) return ALL_FIELDS;
        if (v.fullyEditable(hasSavers, now)) return coinReward ? List.of("code") : List.of();
        return ALL_FIELDS.stream().filter(f -> !RUNNING_EDITABLE.contains(f)).toList();
    }

    /** FREESHIP không có giá trị giảm riêng ({@code null}); phần trăm giữ số lẻ. */
    static BigDecimal discountValue(Voucher v) {
        return v.getDiscountType() == DiscountType.FREESHIP ? null : plain(v.getDiscountValue());
    }

    static Long money(BigDecimal amount) {
        return amount == null ? null : amount.longValue();
    }

    /** 15.00 → 15, 7.50 → 7.5, 100.00 → 100 (không ra dạng 1E+2). */
    static BigDecimal plain(BigDecimal n) {
        return n == null ? null : new BigDecimal(n.stripTrailingZeros().toPlainString());
    }
}
