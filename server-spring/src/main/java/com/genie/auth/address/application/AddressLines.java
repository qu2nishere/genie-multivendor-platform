package com.genie.auth.address.application;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Chuẩn hóa địa chỉ cụ thể (số nhà, tên đường) để so trùng — plan AUTH A12.07, D26: chữ thường, bỏ dấu
 * ("đ" → "d"), dấu câu thành khoảng trắng, gộp khoảng trắng. "25 Nguyễn Huệ, Căn hộ 12A" và "25  nguyen hue can ho 12a"
 * cùng thành "25 nguyen hue can ho 12a". Không bỏ tiền tố như {@link RegionNames} (tránh "P 5" thành "5").
 */
public final class AddressLines {

    private AddressLines() {
    }

    public static String normalize(String line) {
        if (line == null) {
            return "";
        }
        String s = line.toLowerCase(Locale.ROOT).replace('đ', 'd');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return s.replaceAll("[^a-z0-9]+", " ").trim();
    }
}
