package com.genie.common.api;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.NotFoundException;
import com.genie.common.error.TooManyRequestsException;
import com.genie.common.security.RestAccessDeniedHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Chuyển ngoại lệ thành {@link ErrorResponse} (RFC 7807 mở rộng, mã GNE-* — TS 8, API_SPEC 0.3).
 * Không bao giờ lộ stack trace / thông điệp kỹ thuật ra client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorResponseFactory errors;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public GlobalExceptionHandler(ErrorResponseFactory errors, RestAccessDeniedHandler accessDeniedHandler) {
        this.errors = errors;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @ExceptionHandler(TooManyRequestsException.class)
    ResponseEntity<ErrorResponse> handleRateLimit(TooManyRequestsException ex, HttpServletRequest req) {
        return ResponseEntity.status(ex.getErrorCode().status())
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.getRetryAfterSeconds()))
                .body(errors.build(ex.getErrorCode(), ex.getMessage(), ex.getDetails(), req.getRequestURI()));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        ErrorCode ec = ex.getErrorCode();
        return ResponseEntity.status(ec.status())
                .body(errors.build(ec, ex.getMessage(), ex.getDetails(), req.getRequestURI()));
    }

    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ErrorResponse> handleNotFound(Exception ex, HttpServletRequest req) {
        String message = ex instanceof NotFoundException ? ex.getMessage() : null;
        return of(ErrorCode.NOT_FOUND, message, req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return of(ErrorCode.METHOD_NOT_ALLOWED, null, req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(toSnakeCase(fe.getField()), fe.getCode(),
                        isSensitive(fe.getField()) ? null : fe.getRejectedValue(), fe.getDefaultMessage()))
                .toList();
        List<ErrorResponse.FieldError> globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(ge -> new ErrorResponse.FieldError(null, ge.getCode(), null, ge.getDefaultMessage()))
                .toList();
        List<ErrorResponse.FieldError> all = new ArrayList<>(fieldErrors);
        all.addAll(globalErrors);
        ErrorCode ec = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(ec.status()).body(
                errors.build(ec.code(), ec.status().value(), ec.defaultMessage(), all, null, req.getRequestURI()));
    }

    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingRequestHeaderException.class,
            MissingRequestCookieException.class})
    ResponseEntity<ErrorResponse> handleBadInput(Exception ex, HttpServletRequest req) {
        log.debug("Yêu cầu không hợp lệ {}: {}", req.getRequestURI(), ex.getMessage());
        return of(ErrorCode.VALIDATION_FAILED, null, req);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class,
            CannotAcquireLockException.class})
    ResponseEntity<ErrorResponse> handleLock(Exception ex, HttpServletRequest req) {
        log.warn("Xung đột cập nhật đồng thời {}: {}", req.getRequestURI(), ex.getMessage());
        return of(ErrorCode.DB_CONFLICT_RETRY, null, req);
    }

    /** {@code @PreAuthorize} thất bại trong controller (luật URL thì {@code RestAccessDeniedHandler} xử lý ở filter). */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        accessDeniedHandler.recordDenied(req);
        return of(ErrorCode.FORBIDDEN, null, req);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        return of(ErrorCode.UNAUTHORIZED, null, req);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Lỗi không lường trước tại {} {}", req.getMethod(), req.getRequestURI(), ex);
        return of(ErrorCode.INTERNAL_ERROR, null, req);
    }

    private ResponseEntity<ErrorResponse> of(ErrorCode ec, String message, HttpServletRequest req) {
        return ResponseEntity.status(ec.status()).body(errors.build(ec, message, null, req.getRequestURI()));
    }

    /** Tên trường Java (camelCase) → tên trong JSON (snake_case) cho {@code errors[].field}. */
    static String toSnakeCase(String field) {
        if (field == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(field.length() + 4);
        for (char c : field.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Không trả lại giá trị đã nhập của trường nhạy cảm (mật khẩu, OTP, token). */
    private static boolean isSensitive(String field) {
        String f = field == null ? "" : field.toLowerCase();
        return f.contains("password") || f.contains("otp") || f.contains("token") || f.contains("code");
    }
}
