package com.genie.common.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Dựng {@link ErrorResponse} dùng chung cho {@code GlobalExceptionHandler} (lỗi trong controller) và các filter /
 * entry point của Spring Security (lỗi trước khi tới controller — phải tự ghi JSON).
 */
@Component
public class ErrorResponseFactory {

    private final RequestIds requestIds;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public ErrorResponseFactory(RequestIds requestIds, Clock clock, ObjectMapper objectMapper) {
        this.requestIds = requestIds;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    public ErrorResponse build(String code, int status, String message, List<ErrorResponse.FieldError> errors,
                               Map<String, Object> details, String path) {
        var meta = new ErrorResponse.Meta(requestIds.current(), clock.instant(), path);
        return new ErrorResponse(code, status, message, errors == null ? List.of() : errors, details, meta);
    }

    public ErrorResponse build(ErrorCode ec, String message, Map<String, Object> details, String path) {
        return build(ec.code(), ec.status().value(), message == null ? ec.defaultMessage() : message, List.of(), details, path);
    }

    /** Ghi thẳng ra response (dùng ở filter / entry point, nơi không có @ExceptionHandler). */
    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode ec, String message,
                      Map<String, Object> details) throws IOException {
        ErrorResponse body = build(ec, message, details, request.getRequestURI());
        response.setStatus(ec.status().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
