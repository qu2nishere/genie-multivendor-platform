// Tiêu đề voucher shop tự sinh từ điều kiện (plan 2026-11-26 voucher D6; API_SPEC 10.7)
package com.genie.promotion.voucher.application;

import com.genie.promotion.voucher.domain.DiscountType;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Hàm thuần: "Giảm 15% tối đa 50.000₫ đơn từ 200.000₫", "Giảm 30.000₫ đơn từ 399.000₫", "Freeship tối đa 30.000₫". */
public final class VoucherTitles {

    private VoucherTitles() {
    }

    public static String of(DiscountType type, BigDecimal value, BigDecimal maxDiscount, BigDecimal minOrder) {
        StringBuilder sb = new StringBuilder();
        switch (type) {
            case PERCENTAGE -> {
                sb.append("Giảm ").append(number(value)).append('%');
                if (maxDiscount != null) sb.append(" tối đa ").append(vnd(maxDiscount));
            }
            case FIXED_AMOUNT -> sb.append("Giảm ").append(vnd(value));
            case FREESHIP -> {
                sb.append("Freeship");
                if (maxDiscount != null && maxDiscount.signum() > 0) sb.append(" tối đa ").append(vnd(maxDiscount));
            }
        }
        if (minOrder != null && minOrder.signum() > 0) sb.append(" đơn từ ").append(vnd(minOrder));
        return sb.toString();
    }

    /** 50000 → "50.000₫". */
    static String vnd(BigDecimal amount) {
        DecimalFormat f = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN")));
        return f.format(amount) + "₫";
    }

    /** 15 → "15", 7.5 → "7,5". */
    static String number(BigDecimal n) {
        return n.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
