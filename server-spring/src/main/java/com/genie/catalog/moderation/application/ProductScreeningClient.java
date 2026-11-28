// Cổng gọi dịch vụ AI sàng lọc sản phẩm — UC-CAT-12 (plan C1 P06; c1_auto_product.md mục 4.3)
package com.genie.catalog.moderation.application;

public interface ProductScreeningClient {

    /**
     * Sàng lọc một tin đăng sản phẩm.
     *
     * @throws ScreeningFailedException khi hết lượt thử / phản hồi sai schema / lỗi cấu hình
     */
    ScreeningVerdict screen(ScreeningRequest request);
}
