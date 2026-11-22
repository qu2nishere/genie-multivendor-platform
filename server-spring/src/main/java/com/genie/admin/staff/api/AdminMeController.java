// API 11.1 — Khung Admin: GET / PATCH /admin/me (không cần mã quyền; mọi STAFF / ADMIN). Plan B1 — nối khung Admin.
package com.genie.admin.staff.api;

import com.genie.admin.staff.application.AdminMeView;
import com.genie.admin.staff.application.StaffService;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/me")
public class AdminMeController {

    private final StaffService staff;
    private final ApiResponseFactory responses;

    public AdminMeController(StaffService staff, ApiResponseFactory responses) {
        this.staff = staff;
        this.responses = responses;
    }

    public record UpdateMeRequest(@Size(max = 255) String fullName, @Size(max = 500) String avatarObjectKey) {
    }

    @Operation(summary = "UC-ADM-02 — Khung Admin: tài khoản đang đăng nhập + mã quyền (nav ẩn / hiện theo quyền)")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminMeView>> me(@CurrentUser AuthUser me) {
        return responses.ok(staff.me(me.id()));
    }

    @Operation(summary = "Khung Admin — Sửa họ tên, ảnh đại diện của nhân viên đang đăng nhập")
    @PatchMapping
    public ResponseEntity<ApiResponse<AdminMeView>> update(@CurrentUser AuthUser me,
                                                           @Valid @RequestBody UpdateMeRequest body) {
        return responses.ok(staff.updateMe(me.id(), body.fullName(), body.avatarObjectKey()));
    }
}
