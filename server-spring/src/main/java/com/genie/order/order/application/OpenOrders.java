package com.genie.order.order.application;

import com.genie.catalog.product.application.VariantFacade;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Truy vấn "còn đơn chưa kết thúc" dùng chung cho hai cổng chặn xóa (plan C2 D14): tài khoản (UC-AUTH-10) và sản phẩm
 * (UC-CAT-12 của C1). Đơn kết thúc = {@link #TERMINAL}; phiên checkout còn mở luôn có đơn con {@code PENDING_PAYMENT} nên
 * kiểm đơn là đủ.
 */
@Component
public class OpenOrders {

    /** Trạng thái kết thúc của đơn con (Document_Vi 3.4.2). */
    static final String TERMINAL = "'COMPLETED', 'CANCELLED', 'CANCELLED_BY_VENDOR', 'AUTO_CANCELLED', 'REFUNDED'";

    private final JdbcTemplate jdbc;
    private final VariantFacade variants;

    public OpenOrders(JdbcTemplate jdbc, VariantFacade variants) {
        this.jdbc = jdbc;
        this.variants = variants;
    }

    /** Mã đơn còn mở mà người dùng là người mua hoặc chủ shop (tối đa 20, mới trước). */
    public List<String> codesOf(long userId) {
        return jdbc.queryForList("SELECT order_code FROM orders WHERE (customer_id = ? OR vendor_id = ?) AND status NOT IN ("
                + TERMINAL + ") ORDER BY id DESC LIMIT 20", String.class, userId, userId);
    }

    /** Còn đơn mở chứa biến thể (kể cả đã bỏ) của sản phẩm. */
    public boolean containsProduct(long productId) {
        List<Long> ids = variants.variantIdsOf(productId);
        if (ids.isEmpty()) {
            return false;
        }
        Boolean open = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM order_items oi JOIN orders o ON o.id = oi.order_id "
                + "WHERE oi.product_variant_id = ANY (CAST(? AS bigint[])) AND o.status NOT IN (" + TERMINAL + "))",
                Boolean.class, VariantFacade.pgArray(ids.toArray(Long[]::new)));
        return Boolean.TRUE.equals(open);
    }
}
