// Áp kết quả sàng lọc AI vào sản phẩm trong transaction riêng — UC-CAT-12 (plan C1 P06; c1_auto_product.md mục 4.7)
package com.genie.catalog.moderation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.catalog.moderation.domain.ModerationDecision;
import com.genie.catalog.moderation.domain.ProductModerationResult;
import com.genie.catalog.moderation.domain.ProductModerationResultRepository;
import com.genie.catalog.product.domain.Product;
import com.genie.catalog.product.domain.ProductRepository;
import com.genie.catalog.product.domain.ProductStatus;
import com.genie.catalog.product.event.ProductModeratedEvent;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Luôn lưu bản ghi {@link ProductModerationResult} (giữ lịch sử — A8). Chỉ đổi trạng thái sản phẩm khi sản phẩm vẫn ở
 * {@code PENDING_APPROVAL} và {@code submitted_at} khớp tới <b>micro giây</b> (PostgreSQL {@code TIMESTAMPTZ} không giữ
 * nano giây). AI không bao giờ tự đăng sản phẩm lên sàn (D13).
 */
@Component
public class ScreeningResultApplier {

    private static final int MAX_MODEL_NAME = 60;
    private static final int MAX_REASON_TEXT = 500;
    private static final int MAX_FALLBACK_ERROR = 2_000;

    private static final Map<String, String> VIOLATION_LABELS = Map.of(
            "COUNTERFEIT", "hàng giả / nhái",
            "PROHIBITED_ITEM", "hàng cấm / hạn chế",
            "MISLEADING_DESCRIPTION", "mô tả sai / gây hiểu lầm",
            "BAD_IMAGE", "ảnh không đạt",
            "PRICE_ANOMALY", "giá bất thường",
            "WRONG_CATEGORY", "sai ngành hàng");

    private final ProductRepository products;
    private final ProductModerationResultRepository results;
    private final AiScreeningSettings settings;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final ObjectMapper json;
    private final Clock clock;

    public ScreeningResultApplier(ProductRepository products, ProductModerationResultRepository results,
                                  AiScreeningSettings settings, AuditLogger audit,
                                  ApplicationEventPublisher events, ObjectMapper json, Clock clock) {
        this.products = products;
        this.results = results;
        this.settings = settings;
        this.audit = audit;
        this.events = events;
        this.json = json;
        this.clock = clock;
    }

    @Transactional
    public void apply(long productId, OffsetDateTime submittedAt, ScreeningVerdict verdict) {
        Product p = products.findByIdForUpdate(productId).orElse(null);
        if (p == null) return;

        String modelName = truncate(verdict.modelName(), MAX_MODEL_NAME);
        results.save(ProductModerationResult.builder()
                .productId(productId)
                .modelName(modelName)
                .decision(verdict.decision())
                .riskScore(verdict.riskScore())
                .violationsJson(serializeViolations(verdict.violations()))
                .reasoning(verdict.reasoning())
                .rawResponse(verdict.rawResponse())
                .fallbackToManual(false)
                .build());

        if (!isCurrentSubmission(p, submittedAt)) return;

        if (verdict.decision() == ModerationDecision.REJECT && verdict.riskScore() >= settings.rejectThreshold()) {
            String detail = verdict.reasoning() == null || verdict.reasoning().isBlank()
                    ? "Không đạt kiểm duyệt tự động."
                    : truncate("Không đạt kiểm duyệt tự động: " + verdict.reasoning().trim(), MAX_REASON_TEXT);
            p.rejectByAi(detail);
            audit.record(AuditAction.PRODUCT_MODERATED, (Long) null, String.valueOf(productId),
                    Map.of(
                            "action", "AI_REJECT",
                            "from_status", ProductStatus.PENDING_APPROVAL.name(),
                            "to_status", ProductStatus.REJECTED.name(),
                            "risk_score", verdict.riskScore(),
                            "model", modelName),
                    null, true);
            events.publishEvent(new ProductModeratedEvent(
                    p.getId(), p.getVendorId(), p.getName(), "REJECTED", "AI_AUTO", verdict.reasoning()));
        } else if (verdict.decision() == ModerationDecision.REJECT
                || verdict.decision() == ModerationDecision.NEED_REVIEW) {
            p.flag("AI nghi ngờ: " + firstViolationLabel(verdict.violations()));
        }
        // APPROVE → giữ PENDING_APPROVAL cho người duyệt (D13)
        products.save(p);
    }

    @Transactional
    public void applyFailure(long productId, OffsetDateTime submittedAt, String modelName, String error) {
        Product p = products.findByIdForUpdate(productId).orElse(null);
        if (p == null) return;
        results.save(ProductModerationResult.fallback(
                productId,
                truncate(modelName == null || modelName.isBlank() ? "unknown" : modelName, MAX_MODEL_NAME),
                truncate(error, MAX_FALLBACK_ERROR),
                OffsetDateTime.now(clock)));
        // A6: lỗi kỹ thuật không phải dấu hiệu vi phạm → giữ nguyên PENDING_APPROVAL
    }

    private static boolean isCurrentSubmission(Product p, OffsetDateTime submittedAt) {
        return p.getStatus() == ProductStatus.PENDING_APPROVAL
                && p.getSubmittedAt() != null
                && submittedAt != null
                && p.getSubmittedAt().toInstant().truncatedTo(ChronoUnit.MICROS)
                        .equals(submittedAt.toInstant().truncatedTo(ChronoUnit.MICROS));
    }

    private String serializeViolations(List<String> violations) {
        try {
            return json.writeValueAsString(violations == null ? List.of() : violations);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private static String firstViolationLabel(List<String> violations) {
        if (violations == null || violations.isEmpty()) return "cần kiểm tra";
        return VIOLATION_LABELS.getOrDefault(violations.get(0), "cần kiểm tra");
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
