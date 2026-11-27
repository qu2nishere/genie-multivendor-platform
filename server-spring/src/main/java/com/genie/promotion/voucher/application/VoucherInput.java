// Thân $ShopVoucherInput / $PlatformVoucherInput (API_SPEC 10.7, 11.14) — snake_case qua JacksonConfig
package com.genie.promotion.voucher.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Chưa kiểm — {@link VoucherInputValidator} chuẩn hóa thành {@link VoucherDraft}. {@code title}, {@code fundedBy},
 * {@code platformSharePercent}, {@code notificationBody} chỉ dùng cho voucher sàn (shop: server tự đặt).
 */
public record VoucherInput(String title, String code, String discountType, BigDecimal discountValue,
                           BigDecimal maxDiscount, BigDecimal minOrderValue, Integer usageLimit, Integer usagePerUser,
                           BigDecimal budget, OffsetDateTime validFrom, OffsetDateTime validTo, String scope,
                           List<Long> categoryIds, List<Long> productIds, List<Long> excludedCategoryIds,
                           List<Long> excludedProductIds, String fundedBy, BigDecimal platformSharePercent,
                           String notificationBody) {
}
