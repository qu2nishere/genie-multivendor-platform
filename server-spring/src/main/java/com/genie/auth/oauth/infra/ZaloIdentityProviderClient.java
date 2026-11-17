package com.genie.auth.oauth.infra;

import com.genie.auth.oauth.application.IdentityClaims;
import com.genie.auth.oauth.application.IdentityProviderClient;
import com.genie.auth.oauth.application.OAuthProvider;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Đăng nhập Zalo — OAuth v4 (Authorization Code + PKCE S256) + Graph API (UC-AUTH-03, plan AUTH A14.10, D23).
 * <ul>
 *   <li>App ID / Secret đọc từ biến môi trường {@code ZALO_APP_ID}, {@code ZALO_APP_SECRET} (file {@code .env} ở gốc dự
 *       án — đã git-ignore; KHÔNG ghi vào file cấu hình trong git).</li>
 *   <li>Đổi {@code code}: {@code POST oauth.zaloapp.com/v4/access_token}, header {@code secret_key}, kèm
 *       {@code code_verifier}. Zalo không phát {@code id_token} → danh tính lấy bằng access token vừa nhận (chỉ backend
 *       giữ, không lưu) từ {@code GET graph.zalo.me/v2.0/me?fields=id,name,picture}.</li>
 *   <li>Zalo KHÔNG trả email → {@link IdentityClaims#email()} = null, {@code emailVerified} = false.</li>
 *   <li>Zalo không dùng {@code nonce}; chống giả mạo dựa vào {@code state} dùng một lần + PKCE.</li>
 * </ul>
 */
@Component
public class ZaloIdentityProviderClient implements IdentityProviderClient {

    private static final Logger log = LoggerFactory.getLogger(ZaloIdentityProviderClient.class);
    static final String AUTHORIZE_ENDPOINT = "https://oauth.zaloapp.com/v4/permission";
    static final String TOKEN_ENDPOINT = "https://oauth.zaloapp.com/v4/access_token";
    static final String ME_ENDPOINT = "https://graph.zalo.me/v2.0/me?fields=id,name,picture";

    private final String appId;
    private final String appSecret;
    private final RestClient http;

    public ZaloIdentityProviderClient(@Value("${ZALO_APP_ID:}") String appId,
                                      @Value("${ZALO_APP_SECRET:}") String appSecret,
                                      RestClient.Builder builder) {
        this.appId = appId == null ? "" : appId.trim();
        this.appSecret = appSecret == null ? "" : appSecret.trim();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.http = builder.requestFactory(factory).build();
        if (!configured()) {
            log.warn("Đăng nhập Zalo CHƯA cấu hình: thiếu biến môi trường {}{} — nút \"Zalo\" sẽ báo lỗi 503.",
                    this.appId.isEmpty() ? "ZALO_APP_ID " : "", this.appSecret.isEmpty() ? "ZALO_APP_SECRET" : "");
        } else {
            log.info("Đăng nhập Zalo đã cấu hình (app id {}).", this.appId);
        }
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.ZALO;
    }

    @Override
    public boolean configured() {
        return !appId.isEmpty() && !appSecret.isEmpty();
    }

    @Override
    public String authorizeUrl(String state, String nonce, String codeChallenge, String redirectUri) {
        return UriComponentsBuilder.fromHttpUrl(AUTHORIZE_ENDPOINT)
                .queryParam("app_id", appId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("state", state)
                .encode()
                .toUriString();
    }

    @Override
    public IdentityClaims exchange(String code, String codeVerifier, String redirectUri, String expectedNonce) {
        String accessToken = accessToken(code, codeVerifier);
        Map<?, ?> me;
        try {
            me = http.get().uri(ME_ENDPOINT).header("access_token", accessToken).retrieve().body(Map.class);
        } catch (RestClientResponseException e) {
            log.info("Zalo từ chối lấy thông tin người dùng: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.OAUTH_INVALID);
        } catch (ResourceAccessException e) {
            throw unavailable(e);
        }
        if (me == null || hasError(me.get("error"))) {
            log.info("Zalo /me báo lỗi: {}", me);
            throw new BusinessException(ErrorCode.OAUTH_INVALID);
        }
        Object id = me.get("id");
        String subject = id == null ? null : String.valueOf(id);
        if (subject == null || subject.isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID);
        }
        Object name = me.get("name");
        return new IdentityClaims(subject, null, false, name == null ? null : String.valueOf(name), pictureUrl(me));
    }

    private String accessToken(String code, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("app_id", appId);
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("code_verifier", codeVerifier);
        Map<?, ?> body;
        try {
            body = http.post().uri(TOKEN_ENDPOINT).header("secret_key", appSecret)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(Map.class);
        } catch (RestClientResponseException e) {
            log.info("Zalo từ chối đổi code: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.OAUTH_INVALID);
        } catch (ResourceAccessException e) {
            throw unavailable(e);
        }
        Object token = body == null ? null : body.get("access_token");
        if (!(token instanceof String s) || s.isBlank()) {
            // Zalo trả 200 kèm {error, error_name, error_description} khi code sai / hết hạn / verifier không khớp
            log.info("Zalo không cấp access token: {}", body == null ? null : body.get("error_name"));
            throw new BusinessException(ErrorCode.OAUTH_INVALID);
        }
        return s;
    }

    /** {@code picture: {data: {url}}}. */
    static String pictureUrl(Map<?, ?> me) {
        if (me.get("picture") instanceof Map<?, ?> picture && picture.get("data") instanceof Map<?, ?> data
                && data.get("url") instanceof String url && !url.isBlank()) {
            return url;
        }
        return null;
    }

    /** Graph API trả {@code error: 0} khi thành công. */
    static boolean hasError(Object error) {
        if (error == null) {
            return false;
        }
        return !"0".equals(String.valueOf(error).trim());
    }

    private static BusinessException unavailable(ResourceAccessException e) {
        log.warn("Không gọi được Zalo: {}", e.getMessage());
        return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Không kết nối được Zalo, vui lòng thử lại.");
    }
}
