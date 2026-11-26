package com.genie.order.cart.api;

import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.api.CursorPage;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.order.cart.application.CartLineView;
import com.genie.order.cart.application.CartMergeService;
import com.genie.order.cart.application.CartMergeService.BatchResult;
import com.genie.order.cart.application.CartMergeService.Item;
import com.genie.order.cart.application.CartMergeService.MergeResult;
import com.genie.order.cart.application.CartService;
import com.genie.order.cart.application.CartService.AddResult;
import com.genie.order.cart.application.CartService.CartView;
import com.genie.order.cart.application.CartService.MiniItem;
import com.genie.order.cart.application.CartService.UpdateResult;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Giỏ hàng — UC-ORD-01 (API_SPEC 6.1–6.7; plan C2 O02.04). Tài khoản nội bộ bị {@code BuyerOnlyFilter} chặn trước
 * ({@code 403 GNE-ORD-4031}); {@code userId} luôn lấy từ token.
 */
@RestController
@RequestMapping("/api/v1/me/cart")
public class CartController {

    private final CartService cart;
    private final CartMergeService merge;
    private final ApiResponseFactory responses;

    public CartController(CartService cart, CartMergeService merge, ApiResponseFactory responses) {
        this.cart = cart;
        this.merge = merge;
        this.responses = responses;
    }

    /** {@code POST /me/cart/items} và {@code PATCH} trả cùng kiểu (6.3, 6.4). */
    public record ItemResponse(CartLineView item, Long mergedFromId, long cartItemCount) {
    }

    @Operation(summary = "UC-ORD-01 — Giỏ hàng nhiều shop", description = "Nhóm theo shop (shop có món mới thêm lên đầu); "
            + "price_at_add = giá lúc thêm, unit_price = giá hiện tại; unavailable_reason khi không mua được.")
    @GetMapping
    public ResponseEntity<ApiResponse<CartView>> view(@CurrentUser AuthUser me) {
        return noStore(responses.ok(cart.view(me.id())));
    }

    @Operation(summary = "UC-ORD-01 — Mini cart ở header", description = "Mới thêm trước; summary.total = tổng số dòng.")
    @GetMapping("/items")
    public ResponseEntity<ApiListResponse<MiniItem>> latest(@CurrentUser AuthUser me,
                                                            @RequestParam(defaultValue = "5") int limit,
                                                            @RequestParam(defaultValue = "-added_at") String sort) {
        if (!"-added_at".equals(sort)) {
            throw InvalidRequestException.of("sort", "UNSUPPORTED", sort, "Chỉ hỗ trợ sort=-added_at.");
        }
        CartService.MiniCart m = cart.latest(me.id(), limit);
        var page = new CursorPage<>(m.items(), new CursorPage.Pagination(limit, m.total() > m.items().size(), null));
        return noStore(responses.list(page, Map.of("total", m.total())));
    }

    @Operation(summary = "UC-ORD-01 — Thêm vào giỏ / Mua ngay", description = "201 dòng mới, 200 khi cộng dồn cùng biến thể. "
            + "409 GNE-INV-4094 (details.stock) · GNE-ORD-4093 shop tạm nghỉ · GNE-ORD-4094 sản phẩm của shop mình · "
            + "404 GNE-CAT-4042 không còn bán · 422 số lượng 1–999 / giỏ đủ 100 dòng.")
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<ItemResponse>> add(@CurrentUser AuthUser me,
                                                         @Valid @RequestBody CartRequests.AddItem req) {
        AddResult r = cart.add(me.id(), req.variantId(), req.quantity());
        ItemResponse body = new ItemResponse(r.item(), null, r.cartItemCount());
        return noStore(r.created() ? responses.created(body) : responses.ok(body));
    }

    @Operation(summary = "UC-ORD-01 — Đổi số lượng / đổi phân loại",
            description = "Đổi sang biến thể đã có trong giỏ → gộp, merged_from_id = id dòng bị gộp. 409 GNE-INV-4094 vượt tồn.")
    @PatchMapping("/items/{id}")
    public ResponseEntity<ApiResponse<ItemResponse>> update(@CurrentUser AuthUser me, @PathVariable long id,
                                                            @Valid @RequestBody CartRequests.UpdateItem req) {
        UpdateResult r = cart.update(me.id(), id, req.quantity(), req.variantId());
        return noStore(responses.ok(new ItemResponse(r.item(), r.mergedFromId(), cart.count(me.id()))));
    }

    @Operation(summary = "UC-ORD-01 — Xóa một hoặc nhiều món", description = "204; id không thuộc giỏ của bạn bị bỏ qua.")
    @DeleteMapping("/items")
    public ResponseEntity<Void> remove(@CurrentUser AuthUser me, @RequestParam String ids) {
        cart.remove(me.id(), parseIds(ids));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "UC-ORD-01 — Thêm nhiều món (Mua lại)",
            description = "Món không mua được nằm ở skipped[] kèm reason; vượt tồn thì thêm tối đa có thể.")
    @PostMapping("/items/batch")
    public ResponseEntity<ApiResponse<BatchResult>> batch(@CurrentUser AuthUser me,
                                                          @Valid @RequestBody CartRequests.Lines req) {
        return noStore(responses.ok(merge.batch(me.id(), items(req))));
    }

    @Operation(summary = "UC-ORD-01 — Gộp giỏ khách vãng lai sau đăng nhập",
            description = "Cùng SKU cộng dồn, chặn ở tồn kho; skipped[] (không mua được), adjusted[] (đã hạ số lượng).")
    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<MergeResult>> merge(@CurrentUser AuthUser me,
                                                          @Valid @RequestBody CartRequests.Lines req) {
        return noStore(responses.ok(merge.merge(me.id(), items(req))));
    }

    private static List<Item> items(CartRequests.Lines req) {
        return req.items().stream()
                .map(l -> new Item(l.variantId(), l.quantity() == null ? 1 : l.quantity())).toList();
    }

    /** {@code ids=1,2,3} → danh sách id (bỏ trùng); sai định dạng / rỗng / quá 200 → 422 trường {@code ids}. */
    static List<Long> parseIds(String ids) {
        try {
            List<Long> out = Arrays.stream(ids.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                    .map(Long::parseLong).distinct().toList();
            if (out.isEmpty() || out.size() > 200) {
                throw new NumberFormatException();
            }
            return out;
        } catch (NumberFormatException e) {
            throw InvalidRequestException.of("ids", "INVALID_FORMAT", ids, "ids là danh sách số, cách nhau bởi dấu phẩy.");
        }
    }

    static <T> ResponseEntity<T> noStore(ResponseEntity<T> r) {
        return ResponseEntity.status(r.getStatusCode()).cacheControl(CacheControl.noStore()).body(r.getBody());
    }
}
