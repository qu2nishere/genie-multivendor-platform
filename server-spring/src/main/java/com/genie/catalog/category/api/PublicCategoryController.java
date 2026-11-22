// API 5.1, 5.2 — Cây danh mục công khai (UC-CAT-01; plan B2 C03.02)
package com.genie.catalog.category.api;

import com.genie.catalog.category.application.CategoryTreeService;
import com.genie.catalog.category.application.PublicCategoryHeader;
import com.genie.catalog.category.application.PublicCategoryNode;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Công khai, không cần đăng nhập. {@code Cache-Control: public, max-age=300} — trang SSR của web gọi ở mọi trang. */
@RestController
@RequestMapping("/api/v1/public/categories")
public class PublicCategoryController {

    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final CategoryTreeService tree;
    private final ApiResponseFactory responses;

    public PublicCategoryController(CategoryTreeService tree, ApiResponseFactory responses) {
        this.tree = tree;
        this.responses = responses;
    }

    @Operation(summary = "UC-CAT-01 — Cây danh mục đang hiển thị (3 cấp)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicCategoryNode>>> list() {
        return ResponseEntity.ok().cacheControl(CACHE).body(responses.body(tree.publicTree()));
    }

    @Operation(summary = "UC-CAT-01 — Đầu trang danh mục theo slug (breadcrumb, danh mục con, số sản phẩm)")
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<PublicCategoryHeader>> header(@PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(CACHE).body(responses.body(tree.header(slug)));
    }
}
