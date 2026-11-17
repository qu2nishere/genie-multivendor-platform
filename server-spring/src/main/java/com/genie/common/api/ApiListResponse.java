package com.genie.common.api;

import java.util.List;
import java.util.Map;

/**
 * Vỏ phản hồi danh sách (API_SPEC 0.3): {@code data} là mảng, {@code pagination} và {@code summary} ở cấp gốc.
 * {@code pagination} là {@link CursorPage.Pagination} ({@code limit, has_more, next_cursor}) hoặc phân trang số;
 * {@code summary} để {@code null} khi màn hình không cần — khóa Map đặt sẵn snake_case. Dựng qua {@link ApiResponseFactory#list}.
 */
public record ApiListResponse<T>(String code, int status, String message, List<T> data, Object pagination,
                                 Map<String, Object> summary, ApiResponse.Meta meta) {
}
