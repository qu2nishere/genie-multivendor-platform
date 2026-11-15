package com.genie.common.security;

import com.genie.common.domain.UserRole;
import java.time.Instant;

/**
 * Người đang gọi API — principal do {@code JwtAuthFilter} đặt vào SecurityContext.
 * Controller lấy bằng {@code @CurrentUser AuthUser me}; service nhận {@code userId} tường minh.
 *
 * @param id        users.id
 * @param role      vai trò lúc phát token
 * @param sessionId family_id của refresh token đã sinh ra access token này (phiên đăng nhập — "Phiên này")
 * @param jti       mã access token (để đưa vào denylist khi đăng xuất)
 * @param issuedAt  iat
 * @param expiresAt exp
 */
public record AuthUser(Long id, UserRole role, Long sessionId, String jti, Instant issuedAt, Instant expiresAt) {

    public boolean isStaffOrAdmin() {
        return role == UserRole.STAFF || role == UserRole.ADMIN;
    }
}
