package com.genie.auth.profile.application;

import com.genie.common.domain.UserRole;

/**
 * Thông tin tối thiểu của người dùng — {@code $AuthTokens.user} (API_SPEC 1) và DTO công khai của {@link UserFacade}.
 * {@code role} để client chuyển hướng: CUSTOMER / VENDOR → Storefront, STAFF / ADMIN → Admin Console.
 */
public record UserSummary(Long id, UserRole role, String fullName, String avatarUrl) {
}
