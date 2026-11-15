package com.genie.auth.token.api;

import com.genie.auth.token.application.OpenedSession;
import com.genie.common.security.SecurityProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie refresh token {@code genie_rt} (API_SPEC 0.2, LUU_TRU 6.4): {@code HttpOnly; SameSite=Strict;
 * Path=/api/v1/auth}, 7 ngày, xoay vòng mỗi lần làm mới; {@code Secure} theo {@code genie.security.cookie-secure}.
 */
@Component
public class RefreshCookies {

    public static final String NAME = "genie_rt";
    public static final String PATH = "/api/v1/auth";

    private final boolean secure;

    public RefreshCookies(SecurityProperties props) {
        this.secure = props.cookieSecure();
    }

    public ResponseCookie issue(OpenedSession session) {
        return build(session.refreshToken(), session.refreshTtl());
    }

    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
