package com.genie.order.cart.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.order.cart.application.CartMergeService;
import com.genie.order.cart.application.CartMergeService.PublicVariant;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Dòng hiển thị cho giỏ khách vãng lai — UC-ORD-01 (API_SPEC 5.18 {@code GET /public/variants}; plan C2 D8). */
@RestController
@RequestMapping("/api/v1/public/variants")
public class PublicVariantController {

    private final CartMergeService merge;
    private final ApiResponseFactory responses;

    public PublicVariantController(CartMergeService merge, ApiResponseFactory responses) {
        this.merge = merge;
        this.responses = responses;
    }

    @Operation(summary = "UC-ORD-01 — Tra biến thể cho giỏ khách",
            description = "ids tối đa 50; id không tồn tại bị bỏ. Không cache (tồn / giá đổi liên tục).")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicVariant>>> list(@RequestParam String ids) {
        return CartController.noStore(responses.ok(merge.publicVariants(CartController.parseIds(ids))));
    }
}
