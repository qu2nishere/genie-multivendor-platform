package com.genie.catalog.wishlist.api;

import com.genie.catalog.wishlist.application.WishlistItemView;
import com.genie.catalog.wishlist.application.WishlistQueryService;
import com.genie.catalog.wishlist.application.WishlistService;
import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sản phẩm yêu thích của chính người đang đăng nhập — UC-CAT-08 (API_SPEC 3.10). {@code userId} luôn lấy từ token (N3).
 */
@RestController
@RequestMapping("/api/v1/me/wishlist")
public class MeWishlistController {

    private final WishlistService wishlist;
    private final WishlistQueryService query;
    private final ApiResponseFactory responses;

    public MeWishlistController(WishlistService wishlist, WishlistQueryService query, ApiResponseFactory responses) {
        this.wishlist = wishlist;
        this.query = query;
        this.responses = responses;
    }

    @Operation(summary = "UC-CAT-08 — Tập ID sản phẩm đã thích (tô tim)",
            description = "Tối đa 1.000 ID mới nhất; client giữ trong bộ nhớ khi tải trang.")
    @GetMapping("/product-ids")
    public ResponseEntity<ApiResponse<List<Long>>> productIds(@CurrentUser AuthUser me) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(responses.ok(query.productIds(me.id())).getBody());
    }

    @Operation(summary = "UC-CAT-08 — Danh sách sản phẩm yêu thích",
            description = "Mới thích trước, phân trang cursor (limit 1–50, mặc định 20); summary.total cho tiêu đề; "
                    + "is_purchasable = false khi sản phẩm không còn bán / hết hàng / shop đang nghỉ.")
    @GetMapping
    public ResponseEntity<ApiListResponse<WishlistItemView>> list(@CurrentUser AuthUser me,
                                                                  @RequestParam(required = false) String cursor,
                                                                  @RequestParam(required = false) Integer limit) {
        WishlistQueryService.ListResult r = query.list(me.id(), cursor, limit);
        var body = responses.list(r.page(), Map.of("total", r.total())).getBody();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @Operation(summary = "UC-CAT-08 — Thích sản phẩm (idempotent)",
            description = "204; sản phẩm không tồn tại / không còn bán → 404 GNE-CAT-4042.")
    @PutMapping("/{productId}")
    public ResponseEntity<Void> add(@CurrentUser AuthUser me, @PathVariable @Positive long productId) {
        wishlist.add(me.id(), productId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "UC-CAT-08 — Bỏ thích sản phẩm (idempotent)", description = "Luôn 204.")
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> remove(@CurrentUser AuthUser me, @PathVariable @Positive long productId) {
        wishlist.remove(me.id(), productId);
        return ResponseEntity.noContent().build();
    }
}
