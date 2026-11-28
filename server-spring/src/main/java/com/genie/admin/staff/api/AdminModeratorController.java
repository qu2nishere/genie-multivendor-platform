// API_SPEC 11.12 "Cấu hình nhân viên duyệt" — UC-CAT-13 bước 7 (plan C1 P07.01, D15). Quyền MANAGE_CATALOG.
package com.genie.admin.staff.api;

import com.genie.admin.staff.application.ModeratorService;
import com.genie.catalog.moderation.application.ModeratorPool;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /admin/moderators/redistribute} ("Phân phối lại") nằm ở catalog — nó sửa {@code products}. */
@RestController
@RequestMapping("/api/v1/admin/moderators")
@PreAuthorize("hasAuthority('MANAGE_CATALOG')")
public class AdminModeratorController {

    private final ModeratorService moderators;
    private final ApiResponseFactory responses;

    public AdminModeratorController(ModeratorService moderators, ApiResponseFactory responses) {
        this.moderators = moderators;
        this.responses = responses;
    }

    public record AvailabilityRequest(List<ModeratorService.AvailabilityItem> items) {
    }

    /** Một dòng của bảng "Cấu hình nhân viên" (API 11.12). */
    public record ModeratorView(long staffId, String name, boolean isAccepting) {
        static ModeratorView of(ModeratorPool.Moderator m) {
            return new ModeratorView(m.staffId(), m.name(), m.accepting());
        }
    }

    @Operation(summary = "UC-CAT-13 — Danh sách nhân viên kiểm duyệt sản phẩm và trạng thái nhận việc")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ModeratorView>>> list() {
        return responses.ok(moderators.moderators().stream().map(ModeratorView::of).toList());
    }

    @Operation(summary = "UC-CAT-13 — Lưu cấu hình nhận việc của nhân viên kiểm duyệt")
    @PutMapping("/availability")
    public ResponseEntity<ApiResponse<List<ModeratorView>>> availability(@RequestBody AvailabilityRequest body) {
        return responses.ok(moderators.updateAvailability(body == null ? null : body.items()).stream()
                .map(ModeratorView::of).toList());
    }
}
