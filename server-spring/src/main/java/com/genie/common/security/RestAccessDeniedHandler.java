package com.genie.common.security;

import com.genie.common.api.ErrorResponseFactory;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditActor;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Đã đăng nhập nhưng thiếu vai trò / mã quyền → {@code 403 GNE-SEC-4030} + {@code system_logs ACCESS_DENIED}
 * (API_SPEC 0.10, NFR-13). Dùng cho cả lỗi ở tầng filter (luật URL) và {@code GlobalExceptionHandler}
 * (lỗi {@code @PreAuthorize} trong controller).
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    private final ErrorResponseFactory errors;
    private final AuditLogger auditLogger;
    private final ClientInfoResolver clientInfoResolver;

    public RestAccessDeniedHandler(ErrorResponseFactory errors, AuditLogger auditLogger,
                                   ClientInfoResolver clientInfoResolver) {
        this.errors = errors;
        this.auditLogger = auditLogger;
        this.clientInfoResolver = clientInfoResolver;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        recordDenied(request);
        errors.write(request, response, ErrorCode.FORBIDDEN, null, null);
    }

    /** Ghi {@code ACCESS_DENIED}; lỗi ghi nhật ký không được làm hỏng phản hồi 403. */
    public void recordDenied(HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            AuditActor actor = auth != null && auth.getPrincipal() instanceof AuthUser user
                    ? auditLogger.resolve(user.id())
                    : AuditActor.anonymous(null);
            auditLogger.recordIndependent(AuditAction.ACCESS_DENIED, actor, null,
                    Map.of("method", request.getMethod(), "path", request.getRequestURI()),
                    clientInfoResolver.resolve(request), false);
        } catch (RuntimeException e) {
            log.warn("Không ghi được nhật ký ACCESS_DENIED: {}", e.getMessage());
        }
    }
}
