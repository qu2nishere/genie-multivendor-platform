package com.genie.auth.oauth.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.genie.auth.oauth.application.OAuthLoginResult;
import com.genie.auth.oauth.application.OAuthLoginService;
import com.genie.auth.oauth.application.OAuthProvider;
import com.genie.auth.oauth.application.OAuthSignupService;
import com.genie.auth.otp.api.OtpChallengeResponse;
import com.genie.auth.otp.application.IssuedOtp;
import com.genie.auth.token.api.AuthTokensResponse;
import com.genie.auth.token.api.RefreshCookies;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.error.BusinessException;
import com.genie.common.security.ClientInfo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
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
 * Đăng nhập mạng xã hội Google / Zalo (API_SPEC 2.9, 2.10; UC-AUTH-03).
 * <ol>
 *   <li>Web chuyển trình duyệt tới {@code GET /auth/oauth/{google|zalo}/authorize?next=/cart} → {@code 302} sang nhà
 *       cung cấp.</li>
 *   <li>Nhà cung cấp trả về web {@code /auth/oauth-callback?code&state} → web gọi {@code POST /auth/oauth/{p}/token}
 *       → {@code 200 {result, tokens, next}} + cookie {@code genie_rt}.</li>
 *   <li>Zalo chưa có tài khoản → {@code 200 {result: SIGNUP_REQUIRED, signup: {token, name, expires_in}}} (chưa cấp
 *       phiên) → web hỏi email → {@code POST /auth/oauth/zalo/signup} → {@code 202 $OtpChallenge} → xác minh ở
 *       {@code POST /auth/otp-challenges/{id}/verify} → {@code 201 $AuthTokens} (plan D23).</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/auth/oauth")
public class OAuthController {

    private final OAuthLoginService oauth;
    private final OAuthSignupService signups;
    private final RefreshCookies cookies;
    private final ApiResponseFactory responses;

    public OAuthController(OAuthLoginService oauth, OAuthSignupService signups, RefreshCookies cookies,
                           ApiResponseFactory responses) {
        this.oauth = oauth;
        this.signups = signups;
        this.cookies = cookies;
        this.responses = responses;
    }

    public record TokenRequest(@NotBlank @Size(max = 2048) String code,
                               @NotBlank @Size(max = 200) String state,
                               @Size(max = 2048) String redirectUri) {
    }

    /**
     * {@code result}: LOGGED_IN | LINKED | CREATED | SIGNUP_REQUIRED; {@code tokens} null khi SIGNUP_REQUIRED;
     * {@code next}: trang quay về (null nếu không có); {@code signup}: chỉ có khi SIGNUP_REQUIRED.
     */
    public record OAuthTokenResponse(String result, @JsonInclude(JsonInclude.Include.NON_NULL) AuthTokensResponse tokens,
                                     @JsonInclude(JsonInclude.Include.NON_NULL) String next,
                                     @JsonInclude(JsonInclude.Include.NON_NULL) OAuthLoginResult.SignupStep signup) {
    }

    public record SignupRequest(@NotBlank @Size(max = 200) String signupToken,
                                @NotBlank @Email @Size(max = 255) String email) {
    }

    @Operation(summary = "UC-AUTH-03 — Bắt đầu đăng nhập Google / Zalo (302 sang trang đăng nhập của nhà cung cấp)")
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

    @Operation(summary = "UC-AUTH-03 — Hoàn tất đăng nhập Google / Zalo: đổi code, đăng nhập / liên kết / tạo tài khoản")
    @PostMapping("/{provider}/token")
    public ResponseEntity<ApiResponse<OAuthTokenResponse>> token(@PathVariable String provider,
                                                                @Valid @RequestBody TokenRequest body,
                                                                ClientInfo client) {
        OAuthLoginResult result = oauth.complete(OAuthProvider.fromPath(provider), body.code(), body.state(), client);
        var auth = result.auth();
        if (auth == null) {   // SIGNUP_REQUIRED — chưa có phiên
            ResponseEntity<ApiResponse<OAuthTokenResponse>> base = responses.ok(new OAuthTokenResponse(
                    result.result().name(), null, result.next(), result.signup()));
            return ResponseEntity.status(base.getStatusCode()).header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(base.getBody());
        }
        ResponseEntity<ApiResponse<OAuthTokenResponse>> base = responses.ok(new OAuthTokenResponse(
                result.result().name(), AuthTokensResponse.of(auth.session(), auth.user()), result.next(), null));
        return ResponseEntity.status(base.getStatusCode())
                .header(HttpHeaders.SET_COOKIE, cookies.issue(auth.session()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(base.getBody());
    }

    @Operation(summary = "UC-AUTH-03 — Đăng ký bằng Zalo: nhập email cho phiên đăng ký tạm → gửi OTP xác minh email")
    @PostMapping("/{provider}/signup")
    public ResponseEntity<ApiResponse<OtpChallengeResponse>> signup(@PathVariable String provider,
                                                                   @Valid @RequestBody SignupRequest body) {
        IssuedOtp otp = signups.requestOtp(OAuthProvider.fromPath(provider), body.signupToken(), body.email());
        return responses.accepted(OtpChallengeResponse.from(otp));
    }
}
