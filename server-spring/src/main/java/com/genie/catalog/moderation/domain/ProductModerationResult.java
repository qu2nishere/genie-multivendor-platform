package com.genie.catalog.moderation.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import com.genie.catalog.product.domain.Product;

/**
 * ProductModerationResult — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "product_moderation_results", indexes = {
    @Index(name = "idx_moderation_product_time", columnList = "product_id, created_at")
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductModerationResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "product_id", nullable = false)
    private Long productId;                   // FK → products.id

    @Column(nullable = false, length = 60)
    private String modelName;                 // Mô hình đã dùng: "gemini-2.5-flash" (TLCN) / tên model AI Worker (DATN)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModerationDecision decision;      // APPROVE / REJECT / NEED_REVIEW

    @Column(nullable = false)
    private Integer riskScore;                // 0–100 — điểm rủi ro do mô hình trả về

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String violationsJson;            // NULLABLE — Mã vi phạm: ["COUNTERFEIT","PROHIBITED_ITEM","MISLEADING_DESCRIPTION","BAD_IMAGE","PRICE_ANOMALY","WRONG_CATEGORY"]

    @Column(columnDefinition = "TEXT")
    private String reasoning;                 // NULLABLE — Giải thích tiếng Việt hiển thị ở ô "Phân tích từ AI"

    @Column(columnDefinition = "TEXT")
    private String rawResponse;               // NULLABLE — JSON gốc mô hình trả về (phục vụ điều tra; không hiển thị)

    @Builder.Default
    @Column(nullable = false)
    private Boolean fallbackToManual = false; // true = gọi mô hình lỗi / quá hạn / JSON sai → chuyển thẳng hàng chờ duyệt thủ công

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ===== QUAN HỆ =====
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;                  // Sản phẩm được kiểm duyệt
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Kết quả khi gọi mô hình thất bại — luôn chuyển duyệt thủ công — UC-CAT-12 */
    public static ProductModerationResult fallback(Long productId, String modelName, String error, OffsetDateTime now) {
        ProductModerationResult r = new ProductModerationResult();
        r.productId = productId; r.modelName = modelName; r.decision = ModerationDecision.NEED_REVIEW;
        r.riskScore = 50; r.fallbackToManual = true; r.rawResponse = error; r.createdAt = now;
        return r;
    }

    /** Có tự từ chối được không (REJECT và điểm rủi ro ≥ ngưỡng cấu hình) — UC-CAT-12 */
    public boolean isAutoReject(int rejectThreshold) {
        return this.decision == ModerationDecision.REJECT && this.riskScore >= rejectThreshold;
    }
}
