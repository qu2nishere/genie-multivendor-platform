package com.genie.auth.oauth.application;

/**
 * Dữ liệu tạm của một lần đăng nhập mạng xã hội, lưu Redis {@code oauth:state:{state}} 10 phút, dùng một lần.
 *
 * @param provider     nhà cung cấp đã chọn
 * @param nonce        chống phát lại {@code id_token}
 * @param codeVerifier PKCE — chỉ backend biết, gửi kèm khi đổi {@code code}
 * @param redirectUri  địa chỉ callback đã gửi cho nhà cung cấp (phải trùng khi đổi {@code code})
 * @param next         trang web quay về sau khi đăng nhập (đường dẫn nội bộ, có thể null)
 */
public record OAuthState(OAuthProvider provider, String nonce, String codeVerifier, String redirectUri, String next) {
}
