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
 * {@code $Voucher} — voucher phía khách (API_SPEC mục 1; plan Điểm Genie P05.04, plan 2026-11-26 voucher D13). Tiền là số
 * nguyên VND; {@code discount_value} của voucher % là số % ({@code null} với FREESHIP).
 *
 * @param shop                {@code null} với voucher sàn; voucher shop: {@code id} = {@code shop_id} ({@code vendor_profiles.id}),
 *                            tên shop — nạp theo lô qua {@link VoucherViews} (không còn trả {@code users.id})
 * @param includedCategories  tên ngành áp dụng ({@code scope = CATEGORY})
 * @param excludedCategories  tên ngành loại trừ
 * @param state               {@code AVAILABLE, UPCOMING, USED, EXPIRED} — suy từ {@link Voucher#status}
 * @param usedOrderCode       mã đơn lần dùng gần nhất khi {@code state = USED}
 */
public record VoucherView(long id, String code, String title, VoucherType type, DiscountType discountType,
                          Number discountValue, Long maxDiscount, long minOrderValue, ShopRef shop, VoucherScope scope,
                          List<String> includedCategories, List<String> excludedCategories, OffsetDateTime validFrom,
                          OffsetDateTime validTo, String state, Usage usage, FundingParty fundedBy, String description,
                          Boolean isSaved, String usedOrderCode) {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    public record ShopRef(long id, String name) {
    }

    public record Usage(int used, Integer limit) {
    }

    /**
     * Voucher sàn không cần tra shop / ngành (đổi Điểm — plan Điểm Genie). Voucher shop phải đi qua {@link VoucherViews}.
     *
     * @param usedUp người xem đã dùng hết lượt cho phép của mã này
     * @param saved  có trong Kho Voucher của người xem ({@code null} khi không có ngữ cảnh người dùng)
     */
    public static VoucherView of(Voucher v, OffsetDateTime now, boolean usedUp, Boolean saved) {
        return of(v, now, usedUp, saved, null, List.of(), List.of(), null);
    }

    public static VoucherView of(Voucher v, OffsetDateTime now, boolean usedUp, Boolean saved, ShopRef shop,
                                 List<String> included, List<String> excluded, String usedOrderCode) {
        String state = usedUp ? "USED" : switch (v.status(now)) {
            case ACTIVE -> "AVAILABLE";
            case SCHEDULED -> "UPCOMING";
            case ENDED, DISABLED -> "EXPIRED";
        };
        return new VoucherView(v.getId(), v.getCode(), v.getTitle(), v.getType(), v.getDiscountType(),
                v.getDiscountType() == DiscountType.FREESHIP ? null : number(v.getDiscountValue()),
                v.getMaxDiscountCap() == null ? null : v.getMaxDiscountCap().longValue(),
                v.getMinOrderValue().longValue(), shop, v.getScope(), included, excluded,
                v.getValidFrom().atZoneSameInstant(VN).toOffsetDateTime(),
                v.getValidTo().atZoneSameInstant(VN).toOffsetDateTime(), state,
                new Usage(v.getUsedCount() == null ? 0 : v.getUsedCount(), v.getUsageLimit()), v.getSponsorType(),
                v.getNotificationBody(), saved, usedUp ? usedOrderCode : null);
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
