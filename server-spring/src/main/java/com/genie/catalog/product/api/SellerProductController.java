// API_SPEC 10.5 Sản phẩm của shop — UC-CAT-09, UC-CAT-10 (plan C1 P03.05, P04.04). Cổng KYC: chỉ shop VERIFIED (API 0.9).
package com.genie.catalog.product.api;

import com.genie.catalog.product.application.ProductWriteService;
import com.genie.catalog.product.application.SellerProductQueryService;
import com.genie.catalog.product.application.SellerProductViews;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.api.PageResponse;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/products")
public class SellerProductController {

    private final SellerProductQueryService queries;
    private final ProductWriteService writes;
    private final ApiResponseFactory responses;

    public SellerProductController(SellerProductQueryService queries, ProductWriteService writes,
                                   ApiResponseFactory responses) {
        this.queries = queries;
        this.writes = writes;
        this.responses = responses;
    }

    @Operation(summary = "UC-CAT-10 — Danh sách sản phẩm của shop theo tab trạng thái (summary.counts)")
    @GetMapping
    public ResponseEntity<PageResponse<SellerProductViews.Row>> list(@CurrentUser AuthUser me,
                                                                    @RequestParam(required = false) String status,
                                                                    @RequestParam(required = false) String q,
                                                                    @RequestParam(required = false) Integer page,
                                                                    @RequestParam(required = false) Integer size) {
        SellerProductQueryService.Page result = queries.list(me.id(), status, q, page, size);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(responses.pageBody(result.items(), result.pagination(), Map.of("counts", result.counts())));
    }

    @Operation(summary = "UC-CAT-10 — Mở form sửa / xem một sản phẩm của shop")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SellerProductViews.Detail>> detail(@CurrentUser AuthUser me, @PathVariable long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(responses.body(queries.detail(me.id(), id)));
    }

    @Operation(summary = "UC-CAT-09 — Thêm mới sản phẩm & biến thể SKU (lưu nháp hoặc gửi duyệt)")
    @PostMapping
    public ResponseEntity<ApiResponse<SellerProductViews.Saved>> create(@CurrentUser AuthUser me,
                                                                       @RequestBody SellerProductViews.ProductInput body) {
        return responses.of(HttpStatus.CREATED, writes.create(me.id(), body));
    }

    @Operation(summary = "UC-CAT-10 — Cập nhật thông tin sản phẩm, giá bán & hình ảnh")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SellerProductViews.Saved>> update(@CurrentUser AuthUser me, @PathVariable long id,
                                                                       @RequestBody SellerProductViews.ProductInput body) {
        return responses.ok(writes.update(me.id(), id, body));
    }
}
