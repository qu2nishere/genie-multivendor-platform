// Cổng đảo ngược: module order cho catalog biết sản phẩm còn nằm trong đơn chưa kết thúc (plan C1 mục 10.4 = plan C2 mục 8.4).
package com.genie.catalog.product.application;

public interface ProductOrderGuard {

    /**
     * true = còn phiên checkout INITIATED / PAYMENT_PENDING hoặc đơn con chưa ở trạng thái kết thúc
     * (COMPLETED / CANCELLED / AUTO_CANCELLED / REFUNDED…) có chứa biến thể của sản phẩm này.
     */
    boolean hasOpenOrders(long productId);
}
