package com.genie.common.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Phản hồi lỗi — RFC 7807 mở rộng (TECH_STACK mục 8, API_SPEC 0.3):
 * {@code { code: "GNE-<MIỀN>-<HTTP><STT>", status, message, errors[{field, code, rejected_value, message}], details, meta{request_id, timestamp, path} }}.
 * {@code details} NULLABLE — dữ liệu để màn hình hiển thị ngay (số lần thử còn lại, thời điểm hết khóa…).
 */
public record ErrorResponse(String code, int status, String message, List<FieldError> errors,
                            Map<String, Object> details, Meta meta) {

    public record FieldError(String field, String code, Object rejectedValue, String message) {
    }

    public record Meta(String requestId, Instant timestamp, String path) {
    }
}
