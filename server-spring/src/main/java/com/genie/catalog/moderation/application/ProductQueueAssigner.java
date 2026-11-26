// Giao sản phẩm vừa gửi duyệt cho nhân viên kiểm duyệt ít việc nhất — UC-CAT-13 bước 2 (plan C1 D13)
package com.genie.catalog.moderation.application;

import com.genie.catalog.product.domain.Product;
import com.genie.catalog.product.domain.ProductRepository;
import com.genie.catalog.product.event.ProductSubmittedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Chạy <b>đồng bộ trong transaction lưu sản phẩm</b> (sản phẩm là entity đang quản lý — gán qua entity để lần ghi cuối
 * không đè mất người duyệt). Khi có sàng lọc Gemini (UC-CAT-12, P06), phân công vẫn giữ ở bước này; kết quả AI chỉ đổi
 * thứ tự hàng chờ ({@code FLAGGED} lên đầu).
 */
@Component
public class ProductQueueAssigner {

    private final ProductRepository products;
    private final ModerationQueueService queue;

    public ProductQueueAssigner(ProductRepository products, ModerationQueueService queue) {
        this.products = products;
        this.queue = queue;
    }

    @EventListener
    public void onSubmitted(ProductSubmittedEvent e) {
        products.findById(e.productId()).filter(Product::isUnderReview)
                .ifPresent(p -> p.assignTo(queue.leastLoadedModerator()));
    }
}
