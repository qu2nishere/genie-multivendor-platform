// Mã SKU của biến thể — người bán tự nhập hoặc server sinh (API_SPEC 10.5 "sku = null → server tự sinh"; plan C1 D5)
package com.genie.catalog.product.domain;

import java.util.regex.Pattern;

/**
 * Mã người bán nhập: {@code ^[A-Za-z0-9._-]{1,100}$} (cột {@code product_variants.sku VARCHAR(100) UNIQUE}). Mã server
 * sinh: {@code G{productId}-{n}} — {@code n} tăng dần trong một sản phẩm, không trùng mẫu seed ({@code XL-…}, {@code SKU-…}).
 * Kiểm trùng toàn sàn do service làm (cần CSDL).
 */
public final class SkuGenerator {

    public static final Pattern SELLER_SKU = Pattern.compile("^[A-Za-z0-9._-]{1,100}$");

    private SkuGenerator() {
    }

    public static boolean isValid(String sku) {
        return sku != null && SELLER_SKU.matcher(sku).matches();
    }

    public static String generated(long productId, int sequence) {
        if (sequence < 1) throw new IllegalArgumentException("sequence phải ≥ 1");
        return "G" + productId + "-" + sequence;
    }

    /** Tiền tố chung của các mã server sinh cho sản phẩm {@code productId}. */
    public static String generatedPrefix(long productId) {
        return "G" + productId + "-";
    }
}
