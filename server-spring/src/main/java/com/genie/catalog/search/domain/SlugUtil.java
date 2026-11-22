package com.genie.catalog.search.domain;

import com.genie.common.domain.Slugs;

/**
 * Slug của sản phẩm sinh từ tên khi đọc — bảng {@code products} không có cột slug (plan 2026-11-17 D2, thuật toán A11).
 * URL chuẩn {@code /p/{slug}-{id}}; web dùng cùng thuật toán ở {@code web/src/shared/lib/slug.ts} (test chung bộ mẫu).
 * <p>{@link Slugs#slugify} (bỏ dấu → chữ thường → ký tự ngoài {@code [a-z0-9]} thành {@code -}) → cắt {@value #MAX_LENGTH} ký tự.
 * Mọi link sản phẩm (tìm kiếm, chi tiết, yêu thích) dùng hàm này để slug khớp nhau.
 * Tên không còn ký tự hợp lệ → {@code "san-pham"}.
 */
public final class SlugUtil {

    public static final int MAX_LENGTH = 80;

    private SlugUtil() {
    }

    public static String slugify(String name) {
        // Quy tắc chung với web slugify (common/domain/Slugs) + giới hạn độ dài / mặc định riêng của slug sản phẩm
        String s = Slugs.slugify(name);
        if (s.length() > MAX_LENGTH) {
            s = trimDashes(s.substring(0, MAX_LENGTH));
        }
        return s.isEmpty() ? "san-pham" : s;
    }

    private static String trimDashes(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && s.charAt(start) == '-') start++;
        while (end > start && s.charAt(end - 1) == '-') end--;
        return s.substring(start, end);
    }
}
