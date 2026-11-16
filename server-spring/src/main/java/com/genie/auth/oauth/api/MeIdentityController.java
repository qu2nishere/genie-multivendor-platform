package com.genie.auth.oauth.api;

import com.genie.auth.oauth.application.LoginMethodsService;
import com.genie.auth.oauth.application.LoginMethodsView;
import com.genie.auth.oauth.application.OAuthLoginService;
import com.genie.auth.oauth.application.OAuthProvider;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tài khoản › Liên kết đăng nhập (API_SPEC 3.3, UC-AUTH-03, plan A14.04).
 * Liên kết Google: {@code POST /me/identities/google/authorize} lấy URL Google (state gắn với người đang đăng nhập) →
 * trình duyệt sang Google → quay về {@code /auth/oauth-callback} → web gọi {@code POST /me/identities/google}
 * với {@code code}, {@code state}.
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeIdentityController {

    private final OAuthLoginService oauth;
    private final LoginMethodsService methods;
    private final ApiResponseFactory responses;

    public MeIdentityController(OAuthLoginService oauth, LoginMethodsService methods, ApiResponseFactory responses) {
        this.oauth = oauth;
        this.methods = methods;
        this.responses = responses;
    }

    public record AuthorizeResponse(String authorizeUrl) {
    }

    public record LinkRequest(@NotBlank @Size(max = 2048) String code, @NotBlank @Size(max = 200) String state) {
    }

    public record UnlinkRequest(@NotBlank @Size(max = 128) String currentPassword) {
    }

    @Operation(summary = "UC-AUTH-03 — Cách đăng nhập của tài khoản: mật khẩu, Google đã liên kết")
    @GetMapping("/login-methods")
    public ResponseEntity<ApiResponse<LoginMethodsView>> loginMethods(@CurrentUser AuthUser me) {
        return responses.ok(methods.of(me.id()));
    }

    @Operation(summary = "UC-AUTH-03 — Bắt đầu liên kết Google: trả URL trang đăng nhập Google")
    @PostMapping("/identities/{provider}/authorize")
    public ResponseEntity<ApiResponse<AuthorizeResponse>> authorize(@CurrentUser AuthUser me,
                                                                   @PathVariable String provider) {
        String url = oauth.startLink(OAuthProvider.fromPath(provider), me.id());
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(responses.ok(new AuthorizeResponse(url)).getBody());
    }

    @Operation(summary = "UC-AUTH-03 — Hoàn tất liên kết Google vào tài khoản đang đăng nhập")
    @PostMapping("/identities/{provider}")
    public ResponseEntity<ApiResponse<LoginMethodsView>> link(@CurrentUser AuthUser me, @PathVariable String provider,
                                                             @Valid @RequestBody LinkRequest body, ClientInfo client) {
        oauth.link(OAuthProvider.fromPath(provider), me.id(), body.code(), body.state(), client);
        return responses.created(methods.of(me.id()));
    }

    @Operation(summary = "UC-AUTH-03 — Gỡ liên kết Google: nhập lại mật khẩu hiện tại (tài khoản phải có mật khẩu)")
    @DeleteMapping("/identities/{provider}")
    public ResponseEntity<Void> unlink(@CurrentUser AuthUser me, @PathVariable String provider,
                                       @Valid @RequestBody UnlinkRequest body, ClientInfo client) {
        oauth.unlink(OAuthProvider.fromPath(provider), me.id(), body.currentPassword(), client);
        return ResponseEntity.noContent().build();
    }
}
