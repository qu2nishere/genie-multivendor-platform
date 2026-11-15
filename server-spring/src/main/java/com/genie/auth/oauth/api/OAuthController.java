package com.genie.auth.oauth.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.genie.auth.oauth.application.OAuthLoginResult;
import com.genie.auth.oauth.application.OAuthLoginService;
import com.genie.auth.oauth.application.OAuthProvider;
import com.genie.auth.token.api.AuthTokensResponse;
import com.genie.auth.token.api.RefreshCookies;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.error.BusinessException;
import com.genie.common.security.ClientInfo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Đăng nhập mạng xã hội (API_SPEC 2.9, 2.10; UC-AUTH-03).
 * <ol>
 *   <li>Web chuyển trình duyệt tới {@code GET /auth/oauth/google/authorize?next=/cart} → {@code 302} sang Google.</li>
 *   <li>Google trả về web {@code /auth/oauth-callback?code&state} → web gọi {@code POST /auth/oauth/google/token}
 *       → {@code 200 {result, tokens, next}} + cookie {@code genie_rt}.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/auth/oauth")
public class OAuthController {

    private final OAuthLoginService oauth;
    private final RefreshCookies cookies;
    private final ApiResponseFactory responses;

    public OAuthController(OAuthLoginService oauth, RefreshCookies cookies, ApiResponseFactory responses) {
        this.oauth = oauth;
        this.cookies = cookies;
        this.responses = responses;
    }

    public record TokenRequest(@NotBlank @Size(max = 2048) String code,
                               @NotBlank @Size(max = 200) String state,
                               @Size(max = 2048) String redirectUri) {
    }

    /** {@code result}: LOGGED_IN | LINKED | CREATED; {@code next}: trang quay về (null nếu không có). */
    public record OAuthTokenResponse(String result, AuthTokensResponse tokens,
                                     @JsonInclude(JsonInclude.Include.NON_NULL) String next) {
    }

    @Operation(summary = "UC-AUTH-03 — Bắt đầu đăng nhập Google (302 sang trang đăng nhập Google)")
    @GetMapping("/{provider}/authorize")
    public ResponseEntity<Void> authorize(@PathVariable String provider,
                                          @RequestParam(name = "next", required = false) String next) {
        String location;
        try {
            location = oauth.start(OAuthProvider.fromPath(provider), next);
        } catch (BusinessException e) {
            // trình duyệt đang được chuyển hướng — đưa về trang callback của web để hiện thông báo lỗi
            location = oauth.errorRedirect(e.getErrorCode().code());
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location))
                .header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }

    @Operation(summary = "UC-AUTH-03 — Hoàn tất đăng nhập Google: đổi code, đăng nhập / liên kết / tạo tài khoản")
    @PostMapping("/{provider}/token")
    public ResponseEntity<ApiResponse<OAuthTokenResponse>> token(@PathVariable String provider,
                                                                @Valid @RequestBody TokenRequest body,
                                                                ClientInfo client) {
        OAuthLoginResult result = oauth.complete(OAuthProvider.fromPath(provider), body.code(), body.state(), client);
        var auth = result.auth();
        ResponseEntity<ApiResponse<OAuthTokenResponse>> base = responses.ok(new OAuthTokenResponse(
                result.result().name(), AuthTokensResponse.of(auth.session(), auth.user()), result.next()));
        return ResponseEntity.status(base.getStatusCode())
                .header(HttpHeaders.SET_COOKIE, cookies.issue(auth.session()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(base.getBody());
    }
}
