package com.genie.common.error;

import java.util.List;

/**
 * Tham số không hợp lệ phát hiện trong service / controller (ngoài Bean Validation) → {@code 422 GNE-VAL-4220} kèm
 * {@code errors[]} theo trường (API_SPEC 0.3). Tên trường viết sẵn dạng snake_case như trong JSON / query.
 */
public class InvalidRequestException extends RuntimeException {

    /** Một lỗi của một trường: {@code code} dạng UPPER_SNAKE ({@code INVALID_FORMAT}, {@code OUT_OF_RANGE}…). */
    public record Violation(String field, String code, Object rejectedValue, String message) {
    }

    private final List<Violation> violations;

    public InvalidRequestException(List<Violation> violations) {
        super(ErrorCode.VALIDATION_FAILED.defaultMessage());
        this.violations = List.copyOf(violations);
    }

    public static InvalidRequestException of(String field, String code, Object rejectedValue, String message) {
        return new InvalidRequestException(List.of(new Violation(field, code, rejectedValue, message)));
    }

    public List<Violation> getViolations() {
        return violations;
    }
}
