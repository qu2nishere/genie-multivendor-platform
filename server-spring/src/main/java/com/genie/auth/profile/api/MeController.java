package com.genie.auth.profile.api;

import com.genie.auth.profile.application.MeQueryService;
import com.genie.auth.profile.application.MeView;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ngữ cảnh phiên {@code GET /me} (API_SPEC 3.1) — header, menu người dùng, sidebar Tài khoản. */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final MeQueryService me;
    private final ApiResponseFactory responses;

    public MeController(MeQueryService me, ApiResponseFactory responses) {
        this.me = me;
        this.responses = responses;
    }

    @Operation(summary = "UC-AUTH-02 / UC-AUTH-08 — Ngữ cảnh phiên: người dùng, cách đăng nhập, shop, bộ đếm")
    @GetMapping
    public ResponseEntity<ApiResponse<MeView>> get(@CurrentUser AuthUser user) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(responses.ok(me.of(user.id())).getBody());
    }
}
