package com.genie.auth.login.api;

import com.genie.auth.login.application.AuthResult;
import com.genie.auth.login.application.LoginService;
import com.genie.auth.profile.application.AccountSummaryService;
import com.genie.auth.token.api.AuthTokensResponse;
import com.genie.auth.token.api.RefreshCookies;
import com.genie.auth.token.application.OpenedSession;
import com.genie.auth.token.application.RefreshRejectedException;
import com.genie.auth.token.application.SessionService;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.api.ErrorResponse;
import com.genie.common.api.ErrorResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phiên đăng nhập là tài nguyên (API_SPEC 2.5–2.7, mục 17): đăng nhập, làm mới, đăng xuất.
 * Refresh token chỉ đi trong cookie {@code genie_rt}, không bao giờ nằm trong body.
 */
@RestController
@RequestMapping("/api/v1/auth/sessions")
public class SessionController {

    private final LoginService loginService;
    private final SessionService sessions;
    private final RefreshCookies cookies;
    private final ApiResponseFactory responses;
    private final ErrorResponseFactory errors;
    private final AccountSummaryService summaries;

    public SessionController(LoginService loginService, SessionService sessions, RefreshCookies cookies,
                             ApiResponseFactory responses, ErrorResponseFactory errors,
                             AccountSummaryService summaries) {
        this.summaries = summaries;
        this.loginService = loginService;
        this.sessions = sessions;
        this.cookies = cookies;
        this.responses = responses;
        this.errors = errors;
    }

    public record LoginRequest(@NotBlank @Email @Size(max = 255) String email,
                               @NotBlank @Size(max = 128) String password) {
    }

    @Operation(summary = "UC-AUTH-02 — Đăng nhập bằng email + mật khẩu")
    @PostMapping
    public ResponseEntity<ApiResponse<AuthTokensResponse>> login(@Valid @RequestBody LoginRequest body, ClientInfo client) {
        AuthResult result = loginService.login(body.email(), body.password(), client);
        return withCookie(HttpStatus.CREATED, result.session(), AuthTokensResponse.of(result.session(), result.user()));
    }

    @Operation(summary = "UC-AUTH-02 — Làm mới access token bằng cookie genie_rt (xoay vòng)")
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = RefreshCookies.NAME, required = false) String refreshToken,
                                     ClientInfo client, HttpServletRequest request) {
        try {
            OpenedSession session = sessions.refresh(refreshToken, client);
            // kèm user: web tải lại trang chỉ có cookie, cần biết ai đang đăng nhập (chưa có GET /me)
            return withCookie(HttpStatus.OK, session, AuthTokensResponse.of(session, summaries.summaryOf(session.userId())));
        } catch (RefreshRejectedException e) {
            ErrorResponse body = errors.build(e.getErrorCode(), e.getMessage(), null, request.getRequestURI());
            ResponseEntity.BodyBuilder builder = ResponseEntity.status(e.getErrorCode().status());
            if (e.isClearCookie()) {
                builder.header(HttpHeaders.SET_COOKIE, cookies.clear().toString());
            }
            return builder.body(body);
        }
    }

    @Operation(summary = "UC-AUTH-11 — Đăng xuất phiên hiện tại")
    @DeleteMapping("/current")
    public ResponseEntity<Void> logout(@CurrentUser AuthUser me,
                                       @CookieValue(name = RefreshCookies.NAME, required = false) String refreshToken) {
        sessions.logoutCurrent(me, refreshToken);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.clear().toString()).build();
    }

    private <T> ResponseEntity<ApiResponse<T>> withCookie(HttpStatus status, OpenedSession session, T data) {
        ResponseEntity<ApiResponse<T>> base = responses.of(status, data);
        return ResponseEntity.status(base.getStatusCode())
                .header(HttpHeaders.SET_COOKIE, cookies.issue(session).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(base.getBody());
    }
}
