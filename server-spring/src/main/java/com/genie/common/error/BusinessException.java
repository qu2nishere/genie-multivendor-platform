package com.genie.common.error;

import java.util.Map;

/**
 * Lỗi nghiệp vụ mang {@link ErrorCode}; GlobalExceptionHandler chuyển thành ErrorResponse.
 * {@code details} (tùy chọn) là dữ liệu màn hình cần hiển thị ngay — số lần thử còn lại, thời điểm hết khóa… (API_SPEC 0.3).
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), null);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BusinessException(ErrorCode errorCode, Map<String, Object> details) {
        this(errorCode, errorCode.defaultMessage(), details);
    }

    public BusinessException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message == null ? errorCode.defaultMessage() : message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /** NULLABLE — dữ liệu kèm lỗi. Jackson KHÔNG đổi tên khóa Map → đặt khóa sẵn dạng snake_case ({@code remaining_attempts}). */
    public Map<String, Object> getDetails() {
        return details;
    }
}
