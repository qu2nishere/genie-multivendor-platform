package com.genie.auth.token.application;

import com.genie.common.security.JwtService;
import java.time.Duration;

/**
 * Phiên vừa cấp / làm mới: access token (trả trong body) + refresh token gốc (chỉ đặt vào cookie {@code genie_rt}).
 *
 * @param accessToken  JWT 15 phút
 * @param refreshToken giá trị gốc — KHÔNG log, KHÔNG trả trong body
 * @param refreshTtl   Max-Age của cookie
 * @param sessionId    family_id (id ở {@code GET /me/sessions})
 * @param userId       chủ phiên — để dựng {@code user} trong phản hồi làm mới
 */
public record OpenedSession(JwtService.IssuedAccessToken accessToken, String refreshToken, Duration refreshTtl,
                            long sessionId, long userId) {
}
