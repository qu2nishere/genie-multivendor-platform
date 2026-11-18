package com.genie.catalog.product.domain;

import java.math.BigDecimal;

/**
 * Projection chỉ đọc cho các màn danh sách (sản phẩm yêu thích…) — lấy đúng cột cần, KHÔNG nạp entity (tránh các quan hệ
 * {@code @ManyToOne} EAGER sinh truy vấn phụ / N+1). Plan wishlist W03.02.
 */
public final class ProductSummaryRows {

    private ProductSummaryRows() {
    }

    public interface ProductRow {
        Long getId();

        String getName();

        ProductStatus getStatus();

        BigDecimal getBasePrice();

        Long getVendorId();
    }

    public interface VariantRow {
        Long getProductId();

        Long getId();

        BigDecimal getPrice();

        Integer getStockCount();

        Integer getReservedCount();
    }

    public interface ImageRow {
        Long getProductId();

        String getImageUrl();
    }
}
