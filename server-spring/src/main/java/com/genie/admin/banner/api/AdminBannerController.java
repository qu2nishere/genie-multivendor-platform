package com.genie.admin.banner.api;

import com.genie.admin.banner.application.BannerAdminService;
import com.genie.admin.banner.application.BannerCommand;
import com.genie.admin.banner.application.BannerQueryService;
import com.genie.admin.banner.application.BannerView;
import com.genie.admin.banner.domain.BannerPosition;
import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Banner trang chủ của Admin Portal — UC-ADM-12 (API_SPEC 11.16). Quyền {@code MANAGE_VOUCHER} (Admin, Finance Officer).
 * Lỗi: {@code 422 GNE-VAL-4220} (thiếu trường — {@code errors[]}), {@code 422 GNE-BNR-4221} (đủ 8 banner),
 * {@code 422 GNE-BNR-4222} (liên kết ngoài sàn), {@code 409 GNE-BNR-4091} (xóa banner đang / sắp hiển thị), {@code 404}.
 */
@RestController
@RequestMapping("/api/v1/admin/banners")
@PreAuthorize("hasAuthority('MANAGE_VOUCHER')")
public class AdminBannerController {

    private final BannerAdminService admin;
    private final BannerQueryService query;
    private final ApiResponseFactory responses;

    public AdminBannerController(BannerAdminService admin, BannerQueryService query, ApiResponseFactory responses) {
        this.admin = admin;
        this.query = query;
        this.responses = responses;
    }

    /** Thân {@code POST} / {@code PUT} — kiểm chi tiết ở service để trả {@code errors[]} đúng tên trường. */
    public record BannerRequest(String title, String imageObjectKey, String mobileImageObjectKey, String targetUrl,
                                BannerPosition position, Long categoryId, Integer sortOrder, OffsetDateTime startsAt,
                                OffsetDateTime endsAt, Boolean isActive) {

        BannerCommand toCommand() {
            return new BannerCommand(title, imageObjectKey, mobileImageObjectKey, targetUrl, position, categoryId, sortOrder,
                    startsAt, endsAt, isActive);
        }
    }

    public record ActiveRequest(@NotNull Boolean isActive) {
    }

    @Operation(summary = "UC-ADM-12 — Danh sách banner (lọc vị trí, tìm theo tiêu đề)")
    @GetMapping
    public ResponseEntity<ApiListResponse<BannerView>> list(@RequestParam(required = false) BannerPosition position,
                                                            @RequestParam(required = false) String q,
                                                            @RequestParam(required = false) Integer page,
                                                            @RequestParam(required = false) Integer size) {
        return responses.page(query.search(position, q, page, size), null);
    }

    @Operation(summary = "UC-ADM-12 — Tạo banner")
    @PostMapping
    public ResponseEntity<ApiResponse<BannerView>> create(@CurrentUser AuthUser me, @RequestBody BannerRequest body,
                                                          ClientInfo client) {
        return responses.created(admin.create(me.id(), body.toCommand(), client));
    }

    @Operation(summary = "UC-ADM-12 — Sửa banner (bỏ image_object_key = giữ ảnh)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerView>> update(@CurrentUser AuthUser me, @PathVariable long id,
                                                          @RequestBody BannerRequest body, ClientInfo client) {
        return responses.ok(admin.update(me.id(), id, body.toCommand(), client));
    }

    @Operation(summary = "UC-ADM-12 — Tắt ngay / Bật lại banner")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerView>> setActive(@CurrentUser AuthUser me, @PathVariable long id,
                                                             @Valid @RequestBody ActiveRequest body, ClientInfo client) {
        return responses.ok(admin.setActive(me.id(), id, body.isActive(), client));
    }

    @Operation(summary = "UC-ADM-12 — Xóa banner đã tắt / đã kết thúc")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser AuthUser me, @PathVariable long id, ClientInfo client) {
        admin.delete(me.id(), id, client);
        return ResponseEntity.noContent().build();
    }
}
