// API 11.13 — Người dùng vi phạm (UC-ADM-03; plan B1 R05). Admin hoặc nhân viên có MANAGE_USER.
package com.genie.admin.users.api;

import com.genie.admin.users.application.PlatformUserView;
import com.genie.admin.users.application.UserAdminService;
import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chỉ khách hàng / chủ shop — đích là nhân viên, Admin hoặc tài khoản đã xóa → {@code 404 GNE-SYS-4040}. Lỗi nhập
 * ({@code reason_code} ngoài danh sách, thiếu ghi chú khi {@code OTHER}, {@code duration} sai) → {@code 422 GNE-VAL-4220}.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasAuthority('MANAGE_USER')")
public class AdminUserController {

    private final UserAdminService service;
    private final ApiResponseFactory responses;

    public AdminUserController(UserAdminService service, ApiResponseFactory responses) {
        this.service = service;
        this.responses = responses;
    }

    /** {@code duration}: {@code P7D}, {@code P30D} hoặc {@code null} (vô thời hạn). */
    public record SuspendRequest(@NotBlank String reasonCode, @Size(max = 500) String note, String duration) {
    }

    public record ReactivateRequest(@NotBlank @Size(max = 500) String reason) {
    }

    @Operation(summary = "UC-ADM-03 — Danh sách người dùng (tab ALL / CUSTOMER / VENDOR / LOCKED, tìm email / SĐT / tên / tên shop)")
    @GetMapping
    public ResponseEntity<ApiListResponse<PlatformUserView>> list(@RequestParam(required = false) String tab,
                                                                 @RequestParam(required = false) String q,
                                                                 @RequestParam(required = false) Integer page,
                                                                 @RequestParam(required = false) Integer size) {
        UserAdminService.UserList result = service.list(tab, q, page, size);
        return responses.page(result.page(), Map.<String, Object>of("counts", result.counts()));
    }

    @Operation(summary = "UC-ADM-03 — Khóa người dùng (7 ngày / 30 ngày / vô thời hạn), thu hồi mọi phiên")
    @PostMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<PlatformUserView>> suspend(@CurrentUser AuthUser me, @PathVariable long id,
                                                                 @Valid @RequestBody SuspendRequest body,
                                                                 ClientInfo client) {
        return responses.ok(service.suspend(me.id(), id, body.reasonCode(), body.note(), body.duration(), client));
    }

    @Operation(summary = "UC-ADM-03 — Mở khóa người dùng (lý do bắt buộc)")
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<PlatformUserView>> reactivate(@CurrentUser AuthUser me, @PathVariable long id,
                                                                    @Valid @RequestBody ReactivateRequest body,
                                                                    ClientInfo client) {
        return responses.ok(service.reactivate(me.id(), id, body.reason(), client));
    }
}
