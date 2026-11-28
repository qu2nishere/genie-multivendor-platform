package com.genie.promotion.voucher.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Điều kiện một voucher (đã kiểm hợp lệ ở tầng application) — dùng cho {@link Voucher#createShop}, {@link Voucher#createPlatform}
 * và bản nháp khi sửa (plan 2026-11-26 voucher V02.03). Tiền là {@code BigDecimal} VND.
 *
 * @param maxDiscountCap trần giảm; FREESHIP = trần hỗ trợ phí ship ({@code null} = hỗ trợ toàn bộ)
 * @param budget         ngân sách cả chiến dịch, {@code null} = không giới hạn
 */
public record VoucherTerms(String code, String title, DiscountType discountType, BigDecimal discountValue,
                           BigDecimal maxDiscountCap, BigDecimal minOrderValue, int usageLimit, int usagePerUser,
                           BigDecimal budget, OffsetDateTime validFrom, OffsetDateTime validTo, VoucherScope scope,
                           String notificationBody) {
}
