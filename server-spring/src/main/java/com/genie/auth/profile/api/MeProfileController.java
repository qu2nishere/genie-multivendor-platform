package com.genie.auth.profile.api;

import com.genie.auth.profile.application.ProfileChanges;
import com.genie.auth.profile.application.ProfileService;
import com.genie.auth.profile.application.ProfileView;
import com.genie.auth.profile.domain.Gender;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hồ sơ của tôi (API_SPEC 3.1, UC-AUTH-08): xem / sửa thông tin và đổi ảnh đại diện.
 * Đổi ảnh: {@code POST /uploads/presigned-urls {purpose: AVATAR}} → PUT lên MinIO →
 * {@code PATCH /me/profile {avatar_object_key}}.
 */
@RestController
@RequestMapping("/api/v1/me/profile")
public class MeProfileController {

    private final ProfileService profiles;
    private final ApiResponseFactory responses;

    public MeProfileController(ProfileService profiles, ApiResponseFactory responses) {
        this.profiles = profiles;
        this.responses = responses;
    }

    /** Trường không gửi (null) giữ nguyên; {@code phone_number} / {@code bio} gửi chuỗi rỗng để xóa. */
    public record UpdateProfileRequest(
            @Size(max = 255) @Pattern(regexp = ".*\\S.*", message = "Họ tên không được để trống.") String fullName,
            @Pattern(regexp = "^$|^0\\d{9,10}$", message = "Số điện thoại phải gồm 10–11 chữ số, bắt đầu bằng 0.")
            String phoneNumber,
            Gender gender,
            @PastOrPresent LocalDate dateOfBirth,
            @Size(max = 1000) String bio,
            @Pattern(regexp = "vi|en", message = "Ngôn ngữ chỉ nhận vi hoặc en.") String preferredLanguage,
            @Size(max = 200) String avatarObjectKey) {
    }

    @Operation(summary = "UC-AUTH-08 — Xem hồ sơ của tôi")
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileView>> get(@CurrentUser AuthUser me) {
        return responses.ok(profiles.get(me.id()));
    }

    @Operation(summary = "UC-AUTH-08 — Sửa hồ sơ / đổi ảnh đại diện")
    @PatchMapping
    public ResponseEntity<ApiResponse<ProfileView>> update(@CurrentUser AuthUser me,
                                                          @Valid @RequestBody UpdateProfileRequest body) {
        return responses.ok(profiles.update(me.id(), new ProfileChanges(body.fullName(), body.phoneNumber(),
                body.gender(), body.dateOfBirth(), body.bio(), body.preferredLanguage(), body.avatarObjectKey())));
    }
}
