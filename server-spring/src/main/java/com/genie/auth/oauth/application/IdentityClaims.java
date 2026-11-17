package com.genie.auth.oauth.application;

/**
 * Thông tin danh tính nhà cung cấp trả về (Google: từ {@code id_token} đã kiểm chữ ký; Zalo: từ Graph API {@code /me}).
 *
 * @param subject       mã người dùng cố định phía nhà cung cấp ({@code sub} của Google, {@code id} của Zalo) — không đổi
 *                      kể cả khi họ đổi email / tên
 * @param email         email tài khoản (Zalo luôn {@code null})
 * @param emailVerified nhà cung cấp đã xác minh email chưa (Zalo luôn {@code false})
 * @param name          họ tên hiển thị (có thể null)
 * @param picture       URL ảnh đại diện (có thể null)
 */
public record IdentityClaims(String subject, String email, boolean emailVerified, String name, String picture) {
}
