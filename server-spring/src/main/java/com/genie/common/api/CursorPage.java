package com.genie.common.api;

import java.util.List;

/**
 * Phân trang cursor / keyset: {@code ?limit=20&cursor=...} → {@code pagination{limit, has_more, next_cursor}} (TECH_STACK mục 8).
 */
public record CursorPage<T>(List<T> items, Pagination pagination) {

    public record Pagination(int limit, boolean hasMore, String nextCursor) {
    }
}
