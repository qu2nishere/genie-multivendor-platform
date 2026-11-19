package com.genie.social.follow.api;

import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.social.follow.application.ShopFollowQueryService;
import com.genie.social.follow.application.ShopFollowService;
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
 * Shop đang theo dõi của chính người đang đăng nhập — UC-SOC-06 (API_SPEC 3.11). {@code shop_id} = {@code vendor_profiles.id}
 * (D1); {@code userId} luôn lấy từ token (N5).
 */
@RestController
@RequestMapping("/api/v1/me/following")
public class MeFollowingController {

    private final ShopFollowService follows;
    private final ShopFollowQueryService query;
    private final ApiResponseFactory responses;

    public MeFollowingController(ShopFollowService follows, ShopFollowQueryService query, ApiResponseFactory responses) {
        this.follows = follows;
        this.query = query;
        this.responses = responses;
    }

    @Operation(summary = "UC-SOC-06 — Tập shop_id đang theo dõi (tô nút Theo dõi)")
    @GetMapping("/shop-ids")
    public ResponseEntity<ApiResponse<List<Long>>> shopIds(@CurrentUser AuthUser me) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(responses.ok(query.shopIds(me.id())).getBody());
    }

    @Operation(summary = "UC-SOC-06 — Danh sách shop đang theo dõi",
            description = "Mới theo dõi trước, phân trang cursor (limit 1–50, mặc định 20); summary.total cho tiêu đề. "
                    + "Shop bị khóa / chưa xác minh không hiện.")
    @GetMapping
    public ResponseEntity<ApiListResponse<ShopFollowQueryService.FollowedShop>> list(
            @CurrentUser AuthUser me, @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        ShopFollowQueryService.ListResult r = query.list(me.id(), cursor, limit);
        var body = responses.list(r.page(), Map.of("total", r.total())).getBody();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @Operation(summary = "UC-SOC-06 — Theo dõi shop (idempotent)",
            description = "204; shop không tồn tại / chưa xác minh / chủ shop bị khóa → 404; shop của chính mình → 409 GNE-SOC-4095.")
    @PutMapping("/{shopId}")
    public ResponseEntity<Void> follow(@CurrentUser AuthUser me, @PathVariable @Positive long shopId) {
        follows.follow(me.id(), shopId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "UC-SOC-06 — Hủy theo dõi shop (idempotent)", description = "Luôn 204.")
    @DeleteMapping("/{shopId}")
    public ResponseEntity<Void> unfollow(@CurrentUser AuthUser me, @PathVariable @Positive long shopId) {
        follows.unfollow(me.id(), shopId);
        return ResponseEntity.noContent().build();
    }
}
