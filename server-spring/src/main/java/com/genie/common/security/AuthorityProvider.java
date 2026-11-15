package com.genie.common.security;

import com.genie.common.domain.UserRole;
import java.util.Set;

/**
 * Cổng nạp mã quyền (13 mã {@code StaffPermissionEnum}) cho người dùng STAFF / ADMIN — plan AUTH D14.
 * Đặt ở {@code common} để {@code JwtAuthFilter} dùng được mà không phụ thuộc module {@code admin}
 * (tránh vòng phụ thuộc — ArchUnit {@code modules_free_of_cycles}). Module {@code admin} cài đặt.
 * Không có bean nào → chỉ có authority vai trò ({@code ROLE_*}).
 */
public interface AuthorityProvider {

    /** Mã quyền còn hiệu lực, ví dụ {@code REVIEW_KYC}; rỗng với CUSTOMER / VENDOR. */
    Set<String> permissionsOf(long userId, UserRole role);
}
