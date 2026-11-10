package com.genie.common.api;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Chuyển ngoại lệ thành {@link ErrorResponse} (RFC 7807 mở rộng, mã GNE-*).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        ErrorCode ec = ex.getErrorCode();
        return build(ec.status(), ec.code(), ex.getMessage(), List.of(), req);
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "GNE-SYS-4040", ex.getMessage(), List.of(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getCode(), fe.getRejectedValue(), fe.getDefaultMessage()))
                .toList();
        ErrorCode ec = ErrorCode.VALIDATION_FAILED;
        return build(ec.status(), ec.code(), "Dữ liệu không hợp lệ", errors, req);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                List<ErrorResponse.FieldError> errors, HttpServletRequest req) {
        // TODO: lấy request_id từ traceId (Micrometer Tracing)
        var meta = new ErrorResponse.Meta(null, Instant.now(), req.getRequestURI());
        return ResponseEntity.status(status).body(new ErrorResponse(code, status.value(), message, errors, meta));
    }
}
