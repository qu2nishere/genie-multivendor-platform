// API 11.18 — Nhân viên & phân quyền (UC-ADM-01, 02, 03; plan B1 R04.05). Chỉ Admin (MANAGE_RBAC).
package com.genie.admin.staff.api;

import com.genie.admin.rbac.domain.StaffPermissionEnum;
import com.genie.admin.staff.application.StaffEmailCheckView;
import com.genie.admin.staff.application.StaffService;
import com.genie.admin.staff.application.StaffView;
import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lỗi: {@code 409 GNE-ADM-4091} email đã dùng ({@code details.taken_by} = vai trò tài khoản đang giữ email) · {@code 422 GNE-ADM-4221} tên miền không thuộc {@code STAFF_EMAIL_DOMAINS} ·
 * {@code 403 GNE-SEC-4031} cấp {@code MANAGE_RBAC} / sửa quyền Admin · {@code 409 GNE-ADM-4092} thao tác trên chính mình
 * hoặc Admin · {@code 409 GNE-ADM-4093} gửi lại lời mời cho người đã kích hoạt · {@code 429} quá 3 lần gửi lại / giờ.
 */
@RestController
@RequestMapping("/api/v1/admin/staff")
@PreAuthorize("hasAuthority('MANAGE_RBAC')")
public class AdminStaffController {

    private final StaffService staff;
    private final ApiResponseFactory responses;

    public AdminStaffController(StaffService staff, ApiResponseFactory responses) {
        this.staff = staff;
        this.responses = responses;
    }

    public record InviteRequest(@NotBlank @Size(max = 255) String fullName,
                                @NotBlank @Size(max = 255) String email,
                                @NotBlank String staffTitle,
                                @NotNull List<StaffPermissionEnum> permissions) {
    }

    public record PermissionsRequest(@NotBlank String staffTitle, @NotNull List<StaffPermissionEnum> permissions) {
    }

    public record LockRequest(@NotBlank @Size(max = 255) String reason) {
    }

    @Operation(summary = "UC-ADM-01 — Danh sách nhân viên (kể cả lời mời đang chờ / đã hết hạn)")
    @GetMapping
    public ResponseEntity<ApiListResponse<StaffView>> list(@CurrentUser AuthUser me,
                                                          @RequestParam(required = false) String q,
                                                          @RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size) {
        return responses.page(staff.list(q, page, size, me.id()), null);
    }

    @Operation(summary = "UC-ADM-01 — Kiểm tra email trước khi mời (đã thuộc khách hàng / người bán / nhân viên?)")
    @GetMapping("/email-check")
    public ResponseEntity<ApiResponse<StaffEmailCheckView>> emailCheck(@RequestParam String email) {
        return responses.ok(staff.checkEmail(email));
    }

    @Operation(summary = "UC-ADM-01 — Mời nhân viên mới (gửi email kích hoạt, hạn 48 giờ)")
    @PostMapping("/invitations")
    public ResponseEntity<ApiResponse<StaffView>> invite(@CurrentUser AuthUser me,
                                                         @Valid @RequestBody InviteRequest body, ClientInfo client) {
        return responses.created(staff.invite(me.id(), body.fullName(), body.email(), body.staffTitle(),
                body.permissions(), client));
    }

    @Operation(summary = "UC-ADM-01 — Gửi lại lời mời (vô hiệu liên kết cũ, hạn 48 giờ tính lại)")
    @PostMapping("/{id}/invitations/resend")
    public ResponseEntity<ApiResponse<Void>> resend(@CurrentUser AuthUser me, @PathVariable long id,
                                                    ClientInfo client) {
        staff.resend(me.id(), id, client);
        return responses.accepted(null);
    }

    @Operation(summary = "UC-ADM-02 — Đổi chức danh và cấp / thu hồi mã quyền")
    @PutMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<StaffView>> permissions(@CurrentUser AuthUser me, @PathVariable long id,
                                                              @Valid @RequestBody PermissionsRequest body,
                                                              ClientInfo client) {
        return responses.ok(staff.updatePermissions(me.id(), id, body.staffTitle(), body.permissions(), client));
    }

    @Operation(summary = "UC-ADM-03 — Khóa nhân viên (vô thời hạn, thu hồi mọi phiên)")
    @PostMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<StaffView>> lock(@CurrentUser AuthUser me, @PathVariable long id,
                                                       @Valid @RequestBody LockRequest body, ClientInfo client) {
        return responses.ok(staff.lock(me.id(), id, body.reason(), client));
    }

    @Operation(summary = "UC-ADM-03 — Mở khóa nhân viên")
    @PostMapping("/{id}/unlock")
    public ResponseEntity<ApiResponse<StaffView>> unlock(@CurrentUser AuthUser me, @PathVariable long id,
                                                         ClientInfo client) {
        return responses.ok(staff.unlock(me.id(), id, client));
    }

    @Operation(summary = "UC-ADM-01 — Xóa nhân viên (thu hồi quyền + phiên, ẩn danh hóa tài khoản)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser AuthUser me, @PathVariable long id, ClientInfo client) {
        staff.delete(me.id(), id, client);
        return ResponseEntity.noContent().build();
    }
}
