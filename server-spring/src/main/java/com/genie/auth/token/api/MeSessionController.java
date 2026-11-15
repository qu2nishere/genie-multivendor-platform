package com.genie.auth.token.api;

import com.genie.auth.token.application.SessionService;
import com.genie.auth.token.application.SessionView;
import com.genie.auth.token.domain.RevokeReason;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thiết bị đăng nhập (API_SPEC 3.6; UC-AUTH-11): Tài khoản › Thiết bị đăng nhập, Admin › Tài khoản › Phiên đăng nhập.
 */
@RestController
@RequestMapping("/api/v1/me/sessions")
public class MeSessionController {

    private final SessionService sessions;
    private final ApiResponseFactory responses;

    public MeSessionController(SessionService sessions, ApiResponseFactory responses) {
        this.sessions = sessions;
        this.responses = responses;
    }

    @Operation(summary = "UC-AUTH-11 — Danh sách thiết bị đang đăng nhập")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionView>>> list(@CurrentUser AuthUser me) {
        return responses.ok(sessions.listSessions(me.id(), me.sessionId()));
    }

    @Operation(summary = "UC-AUTH-11 — Đăng xuất một thiết bị khác")
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> revokeOne(@CurrentUser AuthUser me, @PathVariable long sessionId, ClientInfo client) {
        sessions.revokeSession(me.id(), sessionId, me.sessionId(), client);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "UC-AUTH-11 — Đăng xuất tất cả thiết bị khác (scope=others)")
    @DeleteMapping
    public ResponseEntity<Void> revokeOthers(@CurrentUser AuthUser me, @RequestParam("scope") String scope,
                                             ClientInfo client) {
        if (!"others".equals(scope)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Chỉ hỗ trợ scope=others.");
        }
        sessions.revokeOtherSessions(me.id(), me.sessionId(), RevokeReason.USER_LOGOUT, client);
        return ResponseEntity.noContent().build();
    }
}
