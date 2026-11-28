// Điều phối sàng lọc AI sản phẩm ngoài transaction — UC-CAT-12 (plan C1 P06; c1_auto_product.md mục 4.8)
package com.genie.catalog.moderation.application;

import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * KHÔNG đặt {@code @Transactional} ở lớp này: bước dựng yêu cầu ({@link ScreeningRequestBuilder}) và bước áp kết quả
 * ({@link ScreeningResultApplier}) ở 2 bean riêng để không giữ kết nối CSDL trong lúc gọi mạng sang Gemini (N6, mục 4.8).
 */
@Service
public class ProductScreeningService {

    private static final Logger log = LoggerFactory.getLogger(ProductScreeningService.class);

    private final AiScreeningSettings settings;
    private final ScreeningRequestBuilder builder;
    private final ProductScreeningClient client;
    private final ScreeningResultApplier applier;

    public ProductScreeningService(AiScreeningSettings settings, ScreeningRequestBuilder builder,
                                   ProductScreeningClient client, ScreeningResultApplier applier) {
        this.settings = settings;
        this.builder = builder;
        this.client = client;
        this.applier = applier;
    }

    public void screen(long productId, OffsetDateTime submittedAt) {
        if (!settings.enabled()) return;
        ScreeningRequest req = builder.build(productId);
        if (req == null) return;
        try {
            ScreeningVerdict verdict = client.screen(req);
            applier.apply(productId, submittedAt, verdict);
        } catch (ScreeningFailedException e) {
            log.warn("Sàng lọc AI sản phẩm #{} lỗi: {}", productId, e.getMessage());
            applier.applyFailure(productId, submittedAt, settings.model(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("Sàng lọc AI sản phẩm #{} lỗi bất ngờ", productId, e);
            applier.applyFailure(productId, submittedAt, settings.model(), e.getClass().getSimpleName());
        }
    }
}
