package com.genie.order.order.application;

import com.genie.catalog.product.application.ProductOrderGuard;
import org.springframework.stereotype.Component;

/**
 * Bản thật của cổng {@link ProductOrderGuard} (plan C2 mục 8.4, O07.06): người bán không xóa được sản phẩm còn nằm trong
 * đơn / phiên checkout chưa kết thúc ({@code 409 GNE-CAT-4094} — phía C1).
 */
@Component
public class OrderProductGuard implements ProductOrderGuard {

    private final OpenOrders open;

    public OrderProductGuard(OpenOrders open) {
        this.open = open;
    }

    @Override
    public boolean hasOpenOrders(long productId) {
        return open.containsProduct(productId);
    }
}
