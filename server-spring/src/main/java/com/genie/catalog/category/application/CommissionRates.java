package com.genie.catalog.category.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Hoa hồng: API dùng <b>phần trăm</b> ({@code 5.0}), CSDL dùng <b>phân số</b> ({@code 0.0500}) — đổi qua lại duy nhất ở
 * đây (plan B2 N4; cấm {@code double}). Khung hợp lệ 3,5 %–8 % cho cả mặc định và theo ngành (D1).
 */
public final class CommissionRates {

    public static final BigDecimal MIN = new BigDecimal("0.0350");
    public static final BigDecimal MAX = new BigDecimal("0.0800");
    /** Mặc định toàn sàn khi {@code platform_settings} chưa có khóa (UC-CAT-14: 5 %). */
    public static final BigDecimal FALLBACK_DEFAULT = new BigDecimal("0.0500");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private CommissionRates() {
    }

    public static BigDecimal toPercent(BigDecimal fraction) {
        return fraction == null ? null : fraction.multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal toFraction(BigDecimal percent) {
        return percent == null ? null : percent.divide(HUNDRED, 4, RoundingMode.HALF_UP);
    }

    public static boolean inRange(BigDecimal fraction) {
        return fraction != null && fraction.compareTo(MIN) >= 0 && fraction.compareTo(MAX) <= 0;
    }
}
