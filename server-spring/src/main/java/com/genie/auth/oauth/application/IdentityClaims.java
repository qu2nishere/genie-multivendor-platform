package com.genie.auth.oauth.application;

/**
 * Thông tin danh tính nhà cung cấp trả về (từ {@code id_token} đã kiểm chữ ký).
 *
 * @param subject       {@code sub} — mã người dùng cố định phía Google (không đổi kể cả khi họ đổi email)
 * @param email         email tài khoản Google
 * @param emailVerified Google đã xác minh email chưa ({@code email_verified})
 * @param name          họ tên hiển thị (có thể null)
 * @param picture       URL ảnh đại diện (có thể null)
 */
public record IdentityClaims(String subject, String email, boolean emailVerified, String name, String picture) {
}
