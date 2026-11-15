package com.genie.support;

import com.genie.auth.oauth.application.IdentityClaims;
import com.genie.auth.oauth.application.IdentityProviderClient;
import com.genie.auth.oauth.application.OAuthProvider;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Google giả cho test: không gọi mạng. Test đăng ký trước {@code code → danh tính}; {@link #authorizeUrl} trả URL giả có
 * {@code state} để test đọc lại. Đứng TRƯỚC {@code GoogleIdentityProviderClient} trong danh sách (xem {@code TestBeans}).
 */
public class FakeIdentityProviderClient implements IdentityProviderClient {

    private final Map<String, IdentityClaims> identities = new ConcurrentHashMap<>();

    public void register(String code, IdentityClaims claims) {
        identities.put(code, claims);
    }

    public void clear() {
        identities.clear();
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public boolean configured() {
        return true;
    }

    @Override
    public String authorizeUrl(String state, String nonce, String codeChallenge, String redirectUri) {
        return "https://accounts.fake/auth?state=" + state + "&code_challenge=" + codeChallenge
                + "&redirect_uri=" + redirectUri;
    }

    @Override
    public IdentityClaims exchange(String code, String codeVerifier, String redirectUri, String expectedNonce) {
        if (codeVerifier == null || codeVerifier.length() < 43 || expectedNonce == null) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID);
        }
        IdentityClaims claims = identities.get(code);
        if (claims == null) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID);
        }
        return claims;
    }
}
