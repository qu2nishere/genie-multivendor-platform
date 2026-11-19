package com.genie.common.api;

import org.springframework.data.domain.Page;

/**
 * Khối {@code pagination} kiểu trang số (API_SPEC 0.4): {@code {page, size, total_items, total_pages}} — {@code page} từ 1.
 */
public record PagePagination(int page, int size, long totalItems, int totalPages) {

    public static PagePagination of(Page<?> page) {
        return new PagePagination(page.getNumber() + 1, page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
