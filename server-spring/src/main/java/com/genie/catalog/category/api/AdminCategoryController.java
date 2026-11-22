// API 11.11 — Danh mục toàn sàn (UC-CAT-14; plan B2 C04.02). MANAGE_CATALOG; đổi hoa hồng cần thêm UPDATE_COMMISSION.
package com.genie.catalog.category.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.genie.catalog.category.application.AdminCategoryView;
import com.genie.catalog.category.application.CategoryAdminService;
import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Body {@code POST}: {@code { name, slug, parent_id, icon, description, commission_rate (%), is_active,
 * requires_business_license }}; {@code PATCH}: trường bất kỳ trong số đó. Lỗi: {@code 409 GNE-CAT-4091} trùng slug / tên
 * trong cùng cha ({@code details.existing_category}) · {@code 409 GNE-CAT-4092} xóa khi còn con / sản phẩm / voucher ·
 * {@code 422 GNE-VAL-4220} sai dữ liệu, quá 3 cấp, chuyển vào con cháu, hoa hồng ngoài 3,5–8 % · {@code 403 GNE-SEC-4030}
 * đổi hoa hồng khi thiếu {@code UPDATE_COMMISSION}.
 */
@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasAuthority('MANAGE_CATALOG')")
public class AdminCategoryController {

    static final String UPDATE_COMMISSION = "UPDATE_COMMISSION";

    private final CategoryAdminService categories;
    private final ApiResponseFactory responses;

    public AdminCategoryController(CategoryAdminService categories, ApiResponseFactory responses) {
        this.categories = categories;
        this.responses = responses;
    }

    @Operation(summary = "UC-CAT-14 — Cây danh mục đầy đủ (kể cả ẩn) + số liệu")
    @GetMapping
    public ResponseEntity<ApiListResponse<AdminCategoryView>> tree() {
        CategoryAdminService.AdminTree t = categories.tree();
        List<AdminCategoryView> roots = t.roots();
        return responses.page(new PageImpl<>(roots), t.summary());
    }

    @Operation(summary = "UC-CAT-14 — Thêm danh mục")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminCategoryView>> create(@CurrentUser AuthUser me, Authentication auth,
                                                                 @RequestBody JsonNode body, ClientInfo client) {
        return responses.created(categories.create(body, can(auth, UPDATE_COMMISSION), me.id(), client));
    }

    @Operation(summary = "UC-CAT-14 — Sửa / ẩn / hiện / chuyển danh mục")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminCategoryView>> patch(@CurrentUser AuthUser me, Authentication auth,
                                                                @PathVariable long id, @RequestBody JsonNode body,
                                                                ClientInfo client) {
        return responses.ok(categories.patch(id, body, can(auth, UPDATE_COMMISSION), me.id(), client));
    }

    @Operation(summary = "UC-CAT-14 — Xóa danh mục rỗng")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser AuthUser me, @PathVariable long id) {
        categories.delete(id, me.id());
        return ResponseEntity.noContent().build();
    }

    static boolean can(Authentication auth, String authority) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
