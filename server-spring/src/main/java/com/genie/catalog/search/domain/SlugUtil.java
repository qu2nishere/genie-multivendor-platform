package com.genie.catalog.search.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Slug của sản phẩm sinh từ tên khi đọc — bảng {@code products} không có cột slug (plan 2026-11-17 D2, thuật toán A11).
 * URL chuẩn {@code /p/{slug}-{id}}; web dùng cùng thuật toán ở {@code web/src/shared/lib/slug.ts} (test chung bộ mẫu).
 * <p>Bỏ dấu → chữ thường → ký tự ngoài {@code [a-z0-9]} thành {@code -} → gộp {@code -} → cắt {@value #MAX_LENGTH} ký tự.
 * Tên không còn ký tự hợp lệ → {@code "san-pham"}.
 */
public final class SlugUtil {

    public static final int MAX_LENGTH = 80;
    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

    private SlugUtil() {
    }

    public static String slugify(String name) {
        String s = KeywordNormalizer.unaccent(name == null ? "" : name).toLowerCase(Locale.ROOT);
        s = NON_SLUG.matcher(s).replaceAll("-");
        s = trimDashes(s);
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
