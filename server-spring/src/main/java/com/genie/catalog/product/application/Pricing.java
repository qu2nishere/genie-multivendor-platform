package com.genie.catalog.product.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Giá hiển thị của sản phẩm (plan 2026-11-17 D8; API_SPEC {@code $ProductCard}).
 * <ul>
 *   <li>{@code price} = giá thấp nhất của SKU còn bán; không có SKU → {@code base_price};</li>
 *   <li>{@code original_price} = {@code base_price} khi lớn hơn {@code price}, ngược lại {@code null};</li>
 *   <li>{@code discount_percent} = làm tròn xuống {@code (original − price) / original × 100}.</li>
 * </ul>
 */
public final class Pricing {

    private Pricing() {
    }

    public static long price(BigDecimal minVariantPrice, BigDecimal basePrice) {
        BigDecimal p = minVariantPrice != null ? minVariantPrice : basePrice;
        return p == null ? 0L : p.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static Long originalPrice(long price, BigDecimal basePrice) {
        if (basePrice == null) {
            return null;
        }
        long base = basePrice.setScale(0, RoundingMode.HALF_UP).longValueExact();
        return base > price ? base : null;
    }

    public static Integer discountPercent(long price, Long originalPrice) {
        if (originalPrice == null || originalPrice <= 0) {
            return null;
        }
        return (int) ((originalPrice - price) * 100 / originalPrice);
    }
}
