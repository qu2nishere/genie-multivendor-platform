package com.genie.auth.address.application;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Chuẩn hóa tên đơn vị hành chính để so khớp giữa nguồn khác nhau (OpenStreetMap ↔ GHN) — plan AUTH A12.06:
 * bỏ dấu ("đ" → "d"), chữ thường, bỏ tiền tố cấp hành chính ("Thành phố", "TP.", "Tỉnh", "Quận", "Q.", "Huyện",
 * "Thị xã", "Phường", "P.", "Xã", "Thị trấn", "Đặc khu"), bỏ dấu câu. "Phường 1" và "P. 1" cùng thành "1"; "Thành phố Hồ Chí Minh",
 * "TP. Hồ Chí Minh" cùng thành "ho chi minh".
 */
public final class RegionNames {

    private static final List<String> PREFIXES = List.of("thanh pho", "thi xa", "thi tran", "dac khu", "tinh",
            "quan", "huyen", "phuong", "xa", "tp", "tx", "tt", "q", "p", "h");

    private RegionNames() {
    }

    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String s = name.toLowerCase(Locale.ROOT).replace('đ', 'd');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String prefix : PREFIXES) {
                if (s.startsWith(prefix + " ") && s.length() > prefix.length() + 1) {
                    s = s.substring(prefix.length() + 1).trim();
                    changed = true;
                    break;
                }
            }
        }
        // "Hồ Chí Minh City" (OSM tiếng Anh) → bỏ hậu tố "city"
        if (s.endsWith(" city")) {
            s = s.substring(0, s.length() - 5).trim();
        }
        return s;
    }

    /**
     * Hai tên cùng chỉ một đơn vị: bằng nhau sau chuẩn hóa ("1" không khớp "13") và không mâu thuẫn cấp — "Phường 5"
     * không khớp "Quận 5" dù cùng thành "5".
     */
    public static boolean same(String a, String b) {
        String na = normalize(a);
        if (na.isEmpty() || !na.equals(normalize(b))) {
            return false;
        }
        Level la = level(a);
        Level lb = level(b);
        return la == Level.UNKNOWN || lb == Level.UNKNOWN || la == lb;
    }

    /** Cấp đọc từ tiền tố; "Thành phố" (cấp tỉnh hoặc cấp huyện) và tên không tiền tố → không rõ. */
    enum Level { WARD, DISTRICT, PROVINCE, UNKNOWN }

    static Level level(String name) {
        if (name == null) {
            return Level.UNKNOWN;
        }
        String s = name.toLowerCase(Locale.ROOT).replace('đ', 'd');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        for (String p : List.of("phuong ", "p ", "xa ", "thi tran ", "tt ", "dac khu ")) {
            if (s.startsWith(p)) {
                return Level.WARD;
            }
        }
        for (String p : List.of("quan ", "q ", "huyen ", "thi xa ", "tx ", "h ")) {
            if (s.startsWith(p)) {
                return Level.DISTRICT;
            }
        }
        return s.startsWith("tinh ") ? Level.PROVINCE : Level.UNKNOWN;
    }
}
