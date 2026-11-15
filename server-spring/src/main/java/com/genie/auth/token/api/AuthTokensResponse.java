package com.genie.auth.token.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.genie.auth.profile.application.UserSummary;
import com.genie.auth.token.application.OpenedSession;

/**
 * {@code $AuthTokens} (API_SPEC 1): {@code { access_token, token_type: "Bearer", expires_in: 900, user{id, role, full_name, avatar_url} }}.
 * Làm mới phiên ({@code POST /auth/sessions/refresh}) cũng kèm {@code user} để web dựng lại phiên sau khi tải lại trang.
 */
public record AuthTokensResponse(String accessToken, String tokenType, long expiresIn,
                                 @JsonInclude(JsonInclude.Include.NON_NULL) UserSummary user) {

    public static AuthTokensResponse of(OpenedSession session, UserSummary user) {
        return new AuthTokensResponse(session.accessToken().token(), "Bearer", session.accessToken().expiresIn(), user);
    }
}
