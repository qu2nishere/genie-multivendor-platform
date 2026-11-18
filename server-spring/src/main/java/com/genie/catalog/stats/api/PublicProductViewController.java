package com.genie.catalog.stats.api;

import com.genie.catalog.stats.application.ProductViewCounter;
import com.genie.catalog.stats.application.RecentCategoryTracker;
import com.genie.catalog.stats.domain.TrafficSource;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/v1/public/products/{id}/views} — ghi lượt xem theo nguồn (UC-CAT-06 bước 9, UC-CAT-18 bước 5;
 * API_SPEC 5.7; plan 2026-11-17 C04.05). Luôn {@code 204} (kể cả lượt trùng bị bỏ qua) để client gửi bằng
 * {@code navigator.sendBeacon} không cần đọc kết quả. Sản phẩm không còn bán → {@code 404 GNE-CAT-4042}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/public/products/{id}/views")
@Tag(name = "Catalog — sản phẩm")
public class PublicProductViewController {

    public record ViewRequest(@NotNull TrafficSource source) {
    }

    private final ProductViewCounter counter;
    private final RecentCategoryTracker recent;
    private final JdbcTemplate jdbc;

    public PublicProductViewController(ProductViewCounter counter, RecentCategoryTracker recent, JdbcTemplate jdbc) {
        this.counter = counter;
        this.recent = recent;
        this.jdbc = jdbc;
    }

    @PostMapping
    @Operation(summary = "UC-CAT-06, UC-CAT-18 Ghi lượt xem sản phẩm theo nguồn truy cập (src=share → EXTERNAL_LINK)")
    public ResponseEntity<Void> record(@PathVariable @Positive long id, @RequestBody @jakarta.validation.Valid ViewRequest body,
                                       @CurrentUser AuthUser me, ClientInfo client) {
        List<Long> category = jdbc.queryForList(
                "SELECT COALESCE(category_id, 0) FROM products WHERE id = ? AND status = 'PUBLISHED'", Long.class, id);
        if (category.isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE);
        }
        Long userId = me == null ? null : me.id();
        counter.record(id, body.source(), ProductViewCounter.visitorKey(userId, client));
        if (userId != null && category.get(0) > 0) {
            recent.viewed(userId, category.get(0));
        }
        return ResponseEntity.noContent().build();
    }
}
