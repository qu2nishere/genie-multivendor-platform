package com.genie.common.api;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Dựng {@link ApiResponse} có {@code meta} đầy đủ cho controller:
 * {@code return responses.ok(data);}, {@code responses.created(data)}, {@code responses.accepted(data)}.
 */
@Component
public class ApiResponseFactory {

    private final RequestIds requestIds;
    private final Clock clock;
    private final String serverVersion;

    public ApiResponseFactory(RequestIds requestIds, Clock clock,
                              @Value("${genie.version:dev}") String serverVersion) {
        this.requestIds = requestIds;
        this.clock = clock;
        this.serverVersion = serverVersion;
    }

    public ApiResponse.Meta meta() {
        return new ApiResponse.Meta(requestIds.current(), clock.instant(), serverVersion);
    }

    public <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return of(HttpStatus.OK, data);
    }

    public <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return of(HttpStatus.CREATED, data);
    }

    public <T> ResponseEntity<ApiResponse<T>> accepted(T data) {
        return of(HttpStatus.ACCEPTED, data);
    }

    public <T> ResponseEntity<ApiResponse<T>> of(HttpStatus status, T data) {
        return ResponseEntity.status(status).body(ApiResponse.of(status.value(), data, meta()));
    }

    /** Danh sách trang số (API_SPEC 0.4) — controller tự thêm header cache nếu cần qua {@link #pageBody}. */
    public <T> ResponseEntity<PageResponse<T>> page(java.util.List<T> items, PageResponse.Pagination pagination,
                                                    Object summary) {
        return ResponseEntity.ok(pageBody(items, pagination, summary));
    }

    public <T> PageResponse<T> pageBody(java.util.List<T> items, PageResponse.Pagination pagination, Object summary) {
        return new PageResponse<>("SUCCESS", 200, "OK", items, pagination, summary, meta());
    }

    /** Thân {@link ApiResponse} 200 — để controller tự dựng {@code ResponseEntity} kèm {@code Cache-Control}. */
    public <T> ApiResponse<T> body(T data) {
        return ApiResponse.ok(data, meta());
    }
}
