package com.genie.common.api;

import java.time.Clock;
import java.util.Map;
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

    /** Danh sách phân trang cursor kèm {@code summary} (có thể {@code null}) — API_SPEC 0.3, 0.4. */
    public <T> ResponseEntity<ApiListResponse<T>> list(CursorPage<T> page, Map<String, Object> summary) {
        return ResponseEntity.ok(new ApiListResponse<>("SUCCESS", 200, "OK", page.items(), page.pagination(),
                summary, meta()));
    }

    /** Danh sách phân trang số (API_SPEC 0.4) — {@code page} của Spring Data đã ánh xạ sang DTO. */
    public <T> ResponseEntity<ApiListResponse<T>> page(org.springframework.data.domain.Page<T> page,
                                                       Map<String, Object> summary) {
        return ResponseEntity.ok(new ApiListResponse<>("SUCCESS", 200, "OK", page.getContent(), PagePagination.of(page),
                summary, meta()));
    }

    public <T> ResponseEntity<ApiResponse<T>> of(HttpStatus status, T data) {
        return ResponseEntity.status(status).body(ApiResponse.of(status.value(), data, meta()));
    }
}
