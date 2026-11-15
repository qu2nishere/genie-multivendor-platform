package com.genie.common.security;

import com.genie.common.api.ErrorResponseFactory;
import com.genie.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Chưa đăng nhập / token sai / token đã thu hồi mà gọi endpoint cần đăng nhập → {@code 401 GNE-SEC-4010}
 * (client gọi {@code POST /auth/sessions/refresh} một lần rồi thử lại — LLD 12).
 */
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    private final ErrorResponseFactory errors;

    public RestAuthEntryPoint(ErrorResponseFactory errors) {
        this.errors = errors;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        errors.write(request, response, ErrorCode.UNAUTHORIZED, null, null);
    }
}
