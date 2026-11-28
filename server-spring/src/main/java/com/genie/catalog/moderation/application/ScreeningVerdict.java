// Kết quả AI đã kiểm schema — UC-CAT-12 (plan C1 P06; c1_auto_product.md mục 4.3)
package com.genie.catalog.moderation.application;

import com.genie.catalog.moderation.domain.ModerationDecision;
import java.util.List;
import java.util.Set;

public record ScreeningVerdict(
        ModerationDecision decision,
        int riskScore,
        List<String> violations,
        String reasoning,
        String rawResponse,
        String modelName) {

    /** Bộ mã vi phạm hợp lệ theo ENTITY 2.10 — mã lạ trong phản hồi AI sẽ bị bỏ đi. */
    public static final Set<String> VIOLATION_CODES = Set.of(
            "COUNTERFEIT",
            "PROHIBITED_ITEM",
            "MISLEADING_DESCRIPTION",
            "BAD_IMAGE",
            "PRICE_ANOMALY",
            "WRONG_CATEGORY");
}
