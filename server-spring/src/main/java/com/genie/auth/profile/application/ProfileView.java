package com.genie.auth.profile.application;

import com.genie.auth.profile.domain.Gender;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Hồ sơ người mua (API_SPEC 3.1 — {@code GET /me/profile}), khớp {@code ProfileDto} của web
 * ({@code features/account/infrastructure/account.api.ts}).
 *
 * @param avatarUrl       URL công khai của ảnh đại diện (MinIO {@code avatar-assets/user/{id}/…} hoặc ảnh Google)
 * @param deletionRequest yêu cầu xóa tài khoản đang chờ (null nếu không có) — ẩn danh sau 30 ngày
 */
public record ProfileView(String fullName, String email, boolean emailVerified, String phoneNumber, Gender gender,
                          LocalDate dateOfBirth, String bio, String preferredLanguage, String avatarUrl,
                          DeletionRequest deletionRequest) {

    public record DeletionRequest(OffsetDateTime requestedAt, OffsetDateTime anonymizeAt) {
    }
}
