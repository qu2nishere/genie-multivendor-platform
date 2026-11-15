package com.genie.common.api;

import java.time.Instant;

/**
 * Vỏ phản hồi thành công (TECH_STACK mục 8):
 * {@code { code: "SUCCESS", status, message, data, meta{request_id, timestamp, server_version} }}.
 * JSON là snake_case nhờ JacksonConfig. Controller dựng qua {@link ApiResponseFactory}.
 */
public record ApiResponse<T>(String code, int status, String message, T data, Meta meta) {

    public record Meta(String requestId, Instant timestamp, String serverVersion) {
    }

    public static <T> ApiResponse<T> ok(T data, Meta meta) {
        return of(200, data, meta);
    }

    public static <T> ApiResponse<T> of(int status, T data, Meta meta) {
        return new ApiResponse<>("SUCCESS", status, "OK", data, meta);
    }
}
