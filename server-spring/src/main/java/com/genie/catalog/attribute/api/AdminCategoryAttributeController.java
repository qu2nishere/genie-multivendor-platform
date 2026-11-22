// API 11.11 "Bộ thuộc tính" — UC-CAT-15 (plan B2 C06.03). MANAGE_CATALOG.
package com.genie.catalog.attribute.api;

import com.genie.catalog.attribute.application.AttributeCommand;
import com.genie.catalog.attribute.application.AttributeView;
import com.genie.catalog.attribute.application.CategoryAttributeService;
import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET} trả thuộc tính của chính ngành; {@code summary.inherited} là thuộc tính kế thừa từ ngành tổ tiên (chỉ
 * đọc). Lỗi: {@code 404 GNE-CAT-4041} ngành không tồn tại · {@code 404 GNE-SYS-4040} mã thuộc tính không có ·
 * {@code 409 GNE-CAT-4095} trùng {@code code} trong ngành / tổ tiên / con cháu · {@code 422 GNE-VAL-4220} sai dữ liệu
 * (mã sai mẫu, thiếu giá trị ENUM, đổi kiểu dữ liệu…).
 */
@RestController
@RequestMapping("/api/v1/admin/categories/{categoryId}/attributes")
@PreAuthorize("hasAuthority('MANAGE_CATALOG')")
public class AdminCategoryAttributeController {

    private final CategoryAttributeService attributes;
    private final ApiResponseFactory responses;

    public AdminCategoryAttributeController(CategoryAttributeService attributes, ApiResponseFactory responses) {
        this.attributes = attributes;
        this.responses = responses;
    }

    public record ActiveRequest(@NotNull Boolean isActive) {
    }

    @Operation(summary = "UC-CAT-15 — Bộ thuộc tính của ngành (+ thuộc tính kế thừa)")
    @GetMapping
    public ResponseEntity<ApiListResponse<AttributeView>> list(@PathVariable long categoryId) {
        CategoryAttributeService.AttributeList result = attributes.list(categoryId);
        return responses.page(new PageImpl<>(result.own()), Map.<String, Object>of("inherited", result.inherited()));
    }

    @Operation(summary = "UC-CAT-15 — Thêm thuộc tính chuẩn cho ngành")
    @PostMapping
    public ResponseEntity<ApiResponse<AttributeView>> create(@PathVariable long categoryId,
                                                             @RequestBody AttributeCommand body) {
        return responses.created(attributes.create(categoryId, body));
    }

    @Operation(summary = "UC-CAT-15 — Sửa thuộc tính (không đổi mã / kiểu dữ liệu)")
    @PutMapping("/{code}")
    public ResponseEntity<ApiResponse<AttributeView>> update(@PathVariable long categoryId, @PathVariable String code,
                                                             @RequestBody AttributeCommand body) {
        return responses.ok(attributes.update(categoryId, code, body));
    }

    @Operation(summary = "UC-CAT-15 — Ngừng dùng / dùng lại thuộc tính")
    @PatchMapping("/{code}")
    public ResponseEntity<ApiResponse<AttributeView>> setActive(@PathVariable long categoryId, @PathVariable String code,
                                                                @Valid @RequestBody ActiveRequest body) {
        return responses.ok(attributes.setActive(categoryId, code, body.isActive()));
    }
}
