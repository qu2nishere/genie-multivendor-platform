// Listener sau commit chạy sàng lọc AI bất đồng bộ — UC-CAT-12 (plan C1 P06; c1_auto_product.md mục 4.8)
package com.genie.catalog.moderation.application;

import com.genie.catalog.product.event.ProductSubmittedEvent;
import java.time.OffsetDateTime;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProductScreeningListener {

    private final ProductScreeningService screening;

    public ProductScreeningListener(ProductScreeningService screening) {
        this.screening = screening;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmitted(ProductSubmittedEvent e) {
        screening.screen(e.productId(), e.submittedAt());
    }

    /**
     * Chấm một sản phẩm trên luồng {@code @Async} — job quét gọi hàm này để không giữ luồng {@code @Scheduled}
     * (Spring mặc định chỉ 1 luồng lịch; gọi Gemini tuần tự 20 sản phẩm có thể chặn mọi job khác hơn 20 phút).
     */
    @Async
    public void screenLater(long productId, OffsetDateTime submittedAt) {
        screening.screen(productId, submittedAt);
    }
}
