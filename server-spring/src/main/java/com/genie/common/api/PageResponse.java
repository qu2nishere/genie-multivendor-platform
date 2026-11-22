package com.genie.common.api;

import java.util.List;

/**
 * Vỏ phản hồi danh sách có phân trang số trang (API_SPEC 0.3, 0.4):
 * {@code { code, status, message, data: [...], pagination: {page, size, total_items, total_pages}, summary, meta }}.
 * {@code summary} = {@code null} khi màn hình không cần tổng hợp (vd khối {@code facets} của tìm kiếm).
 */
public record PageResponse<T>(String code, int status, String message, List<T> data,
                              Pagination pagination, Object summary, ApiResponse.Meta meta) {

    /** {@code page} bắt đầu từ 1. */
    public record Pagination(int page, int size, long totalItems, int totalPages) {

        public static Pagination of(int page, int size, long totalItems) {
            int pages = size <= 0 ? 0 : (int) ((totalItems + size - 1) / size);
            return new Pagination(page, size, totalItems, pages);
        }
    }
}
