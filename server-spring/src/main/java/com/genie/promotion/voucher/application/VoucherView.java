package com.genie.promotion.voucher.application;

import com.genie.common.domain.FundingParty;
import com.genie.common.domain.VoucherType;
import com.genie.promotion.voucher.domain.DiscountType;
import com.genie.promotion.voucher.domain.Voucher;
import com.genie.promotion.voucher.domain.VoucherScope;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * {@code $Voucher} — voucher phía khách (API_SPEC mục 1; plan Điểm Genie P05.04). Đặt ở feature {@code voucher} để plan Kho
 * Voucher (UC-PRO-03) dùng lại. Tiền là số nguyên VND; {@code discount_value} của voucher % là số %.
 *
 * @param shop                {@code null} với voucher sàn; voucher shop có {@code id} = id chủ shop, {@code name} do module
 *                            vendor điền sau (chưa có cổng — {@code null})
 * @param includedCategories  tên danh mục áp dụng — để trống tới khi module catalog có cổng tra tên (voucher sàn {@code ALL})
 * @param state               {@code AVAILABLE, UPCOMING, USED, EXPIRED}
 */
public record VoucherView(long id, String code, String title, VoucherType type, DiscountType discountType,
                          Number discountValue, Long maxDiscount, long minOrderValue, ShopRef shop, VoucherScope scope,
                          List<String> includedCategories, List<String> excludedCategories, OffsetDateTime validFrom,
                          OffsetDateTime validTo, String state, Usage usage, FundingParty fundedBy, String description,
                          Boolean isSaved) {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    public record ShopRef(long id, String name) {
    }

    public record Usage(int used, Integer limit) {
    }

    /**
     * @param usedUp người xem đã dùng hết lượt cho phép của mã này
     * @param saved  có trong Kho Voucher của người xem ({@code null} khi không có ngữ cảnh người dùng)
     */
    public static VoucherView of(Voucher v, OffsetDateTime now, boolean usedUp, Boolean saved) {
        String state;
        if (usedUp) {
            state = "USED";
        } else if (!Boolean.TRUE.equals(v.getIsActive()) || !now.isBefore(v.getValidTo())) {
            state = "EXPIRED";
        } else if (now.isBefore(v.getValidFrom())) {
            state = "UPCOMING";
        } else {
            state = "AVAILABLE";
        }
        return new VoucherView(v.getId(), v.getCode(), v.getTitle(), v.getType(), v.getDiscountType(),
                number(v.getDiscountValue()), v.getMaxDiscountCap() == null ? null : v.getMaxDiscountCap().longValue(),
                v.getMinOrderValue().longValue(), v.getVendorId() == null ? null : new ShopRef(v.getVendorId(), null),
                v.getScope(), List.of(), List.of(), v.getValidFrom().atZoneSameInstant(VN).toOffsetDateTime(),
                v.getValidTo().atZoneSameInstant(VN).toOffsetDateTime(), state,
                new Usage(v.getUsedCount() == null ? 0 : v.getUsedCount(), v.getUsageLimit()), v.getSponsorType(),
                v.getNotificationBody(), saved);
    }

    /** 10.00 → 10 (số nguyên); 7.50 → 7.5. */
    private static Number number(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal v = value.stripTrailingZeros();
        return v.scale() <= 0 ? (Number) v.longValueExact() : (Number) v.doubleValue();
    }
}
