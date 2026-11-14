package com.genie.common.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Chuẩn hóa họ tên để so khớp chính chủ (VendorProfile.verifyPayoutAccount — ENTITY.md 1.4, UC-VND-07):
 * bỏ dấu tiếng Việt (kể cả đ/Đ), viết hoa, gộp khoảng trắng.
 */
public final class NameNormalizer {

    private NameNormalizer() {
    }

    public static String normalize(String name) {
        if (name == null) return "";
        String noMarks = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return noMarks.toUpperCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
