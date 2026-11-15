package com.genie.auth.oauth.infra;

import com.genie.auth.oauth.application.IdentityClaims;
import com.genie.auth.oauth.application.IdentityProviderClient;
import com.genie.auth.oauth.application.OAuthProvider;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Google OpenID Connect — Authorization Code + PKCE (UC-AUTH-03, plan AUTH A14.01).
 * <ul>
 *   <li>Client id / secret đọc từ biến môi trường {@code GOOGLE_CLIENT_ID}, {@code GOOGLE_CLIENT_SECRET} (đặt ở IntelliJ
 *       Run Configuration / {@code deploy/.env}; KHÔNG ghi vào file cấu hình trong git).</li>
 *   <li>{@code id_token} kiểm bằng khóa công khai của Google (JWKS, có cache): chữ ký RS256, {@code iss},
 *       {@code aud = client id}, hạn, rồi {@code nonce} khớp lần đăng nhập này.</li>
 * </ul>
 */
@Component
public class GoogleIdentityProviderClient implements IdentityProviderClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleIdentityProviderClient.class);
    static final String AUTHORIZE_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final List<String> ISSUERS = List.of("https://accounts.google.com", "accounts.google.com");

    private final String clientId;
    private final String clientSecret;
    private final RestClient http;
    private volatile NimbusJwtDecoder decoder;

    public GoogleIdentityProviderClient(@Value("${GOOGLE_CLIENT_ID:}") String clientId,
                                        @Value("${GOOGLE_CLIENT_SECRET:}") String clientSecret,
                                        RestClient.Builder builder) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.http = builder.requestFactory(factory).build();
        if (!configured()) {
            log.warn("Đăng nhập Google CHƯA cấu hình: thiếu biến môi trường {}{} — nút \"Tiếp tục với Google\" sẽ báo lỗi 503.",
                    this.clientId.isEmpty() ? "GOOGLE_CLIENT_ID " : "", this.clientSecret.isEmpty() ? "GOOGLE_CLIENT_SECRET" : "");
        } else {
            log.info("Đăng nhập Google đã cấu hình (client id …{}).",
                    this.clientId.length() > 30 ? this.clientId.substring(0, 12) : this.clientId);
        }
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public boolean configured() {
        return !clientId.isEmpty() && !clientSecret.isEmpty();
    }

    @Override
    public String authorizeUrl(String state, String nonce, String codeChallenge, String redirectUri) {
        return UriComponentsBuilder.fromHttpUrl(AUTHORIZE_ENDPOINT)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .queryParam("prompt", "select_account")
                .encode()
                .toUriString();
    }

    @Override
    public IdentityClaims exchange(String code, String codeVerifier, String redirectUri, String expectedNonce) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("code_verifier", codeVerifier);
        Map<?, ?> body;
        try {
            body = http.post().uri(TOKEN_ENDPOINT).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                    .retrieve().body(Map.class);
        } catch (RestClientResponseException e) {
            log.info("Google từ chối đổi code: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.OAUTH_INVALID);
        } catch (ResourceAccessException e) {
            log.warn("Không gọi được Google: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Không kết nối được Google, vui lòng thử lại.");
        }
        Object idToken = body == null ? null : body.get("id_token");
        if (!(idToken instanceof String token) || token.isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID);
        }
        Jwt jwt;
        try {
            jwt = decoder().decode(token);
        } catch (JwtException e) {
            log.info("id_token Google không hợp lệ: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OAUTH_INVALID);
        }
        if (expectedNonce == null || !expectedNonce.equals(jwt.getClaimAsString("nonce"))) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID);
        }
        Object verified = jwt.getClaim("email_verified");
        boolean emailVerified = Boolean.TRUE.equals(verified) || "true".equalsIgnoreCase(String.valueOf(verified));
        return new IdentityClaims(jwt.getSubject(), jwt.getClaimAsString("email"), emailVerified,
                jwt.getClaimAsString("name"), jwt.getClaimAsString("picture"));
    }

    /** Tạo lần đầu khi cần (khởi động không phụ thuộc mạng); JWKS được Nimbus cache và tự làm mới khi Google xoay khóa. */
    private NimbusJwtDecoder decoder() {
        NimbusJwtDecoder d = decoder;
        if (d == null) {
            synchronized (this) {
                if (decoder == null) {
                    NimbusJwtDecoder created = NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build();
                    created.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                            new JwtTimestampValidator(),
                            new JwtClaimValidator<Object>(JwtClaimNames.ISS, iss -> ISSUERS.contains(String.valueOf(iss))),
                            new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, aud -> aud != null && aud.contains(clientId))));
                    decoder = created;
                }
                d = decoder;
            }
        }
        return d;
    }
}
