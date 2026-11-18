package com.genie.common.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Slug tiếng Việt không dấu cho URL — CÙNG quy tắc với {@code web/src/shared/lib/slug.ts} ({@code slugify}) để link
 * {@code /p/{slug}-{id}} do backend trả khớp trang web: "Điện thoại & Phụ kiện" → "dien-thoai-phu-kien".
 * Bảng {@code products} chưa có cột slug nên slug sản phẩm sinh từ tên (handoff mục 3 — trường {@code product_slug}).
 */
public final class Slugs {

    private Slugs() {
    }

    public static String slugify(String input) {
        if (input == null) {
            return "";
        }
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[\\u0300-\\u036f]", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-{2,}", "-");
    }
}
