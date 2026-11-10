package com.genie.common.api;

import java.time.Instant;
import java.util.List;

/**
 * Phản hồi lỗi — RFC 7807 mở rộng (TECH_STACK mục 8):
 * {@code { code: "GNE-<MIỀN>-<HTTP><STT>", status, message, errors[{field, code, rejected_value, message}], meta{request_id, timestamp, path} }}.
 */
public record ErrorResponse(String code, int status, String message, List<FieldError> errors, Meta meta) {

    public record FieldError(String field, String code, Object rejectedValue, String message) {
    }

    public record Meta(String requestId, Instant timestamp, String path) {
    }
}
