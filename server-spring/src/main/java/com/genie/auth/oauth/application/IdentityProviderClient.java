package com.genie.auth.oauth.application;

/**
 * Cổng tới nhà cung cấp OpenID Connect (cài đặt: {@code infra.GoogleIdentityProviderClient}; test dùng bản giả).
 * Luồng Authorization Code + PKCE (S256) + {@code nonce}.
 */
public interface IdentityProviderClient {

    OAuthProvider provider();

    /** Đã có client id / secret chưa (thiếu → không cho bắt đầu đăng nhập). */
    boolean configured();

    /** URL trang đăng nhập của nhà cung cấp. */
    String authorizeUrl(String state, String nonce, String codeChallenge, String redirectUri);

    /**
     * Đổi {@code code} lấy {@code id_token}, kiểm chữ ký, {@code iss}, {@code aud}, hạn và {@code nonce}.
     * Mã sai / hết hạn / token không hợp lệ → {@code 400 GNE-AUTH-4001}; không gọi được nhà cung cấp → {@code 503}.
     */
    IdentityClaims exchange(String code, String codeVerifier, String redirectUri, String expectedNonce);
}
