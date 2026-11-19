package com.genie.catalog.search.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Chuẩn hóa từ khóa tìm kiếm (plan 2026-11-17 thuật toán A1; UC-CAT-02, UC-CAT-04).
 * <ol>
 *   <li>cắt khoảng trắng hai đầu, tối đa {@value #MAX_LENGTH} ký tự;</li>
 *   <li>Unicode NFC → chữ thường;</li>
 *   <li>ký tự không phải chữ / số → khoảng trắng (giữ chữ có dấu tiếng Việt);</li>
 *   <li>gộp khoảng trắng.</li>
 * </ol>
 * {@link #unaccent(String)} bỏ dấu giống hàm {@code unaccent()} của PostgreSQL (kể cả đ → d) — kiểm bằng
 * {@code KeywordNormalizerIT} so với CSDL thật.
 */
public final class KeywordNormalizer {

    public static final int MAX_LENGTH = 100;

    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private KeywordNormalizer() {
    }

    /** Từ khóa đã chuẩn hóa (còn dấu) — {@code ""} khi rỗng / chỉ có ký tự đặc biệt. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.strip();
        if (s.length() > MAX_LENGTH) {
            s = s.substring(0, MAX_LENGTH);
        }
        s = Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        s = NON_WORD.matcher(s).replaceAll(" ");
        return SPACES.matcher(s).replaceAll(" ").strip();
    }

    /** Bỏ dấu tiếng Việt: "Áo thun đẹp" → "Ao thun dep". */
    public static String unaccent(String s) {
        if (s == null) {
            return "";
        }
        String noMarks = MARKS.matcher(Normalizer.normalize(s, Normalizer.Form.NFD)).replaceAll("");
        return noMarks.replace('đ', 'd').replace('Đ', 'D');
    }

    /** Chuẩn hóa + bỏ dấu — dạng dùng để so khớp / thống kê ({@code search_keyword_stats.normalized_keyword}). */
    public static String normalizeUnaccented(String raw) {
        return unaccent(normalize(raw));
    }

    /** Các từ của từ khóa đã chuẩn hóa (bỏ dấu) — để dựng tsquery tiền tố. */
    public static String[] tokens(String normalizedUnaccented) {
        if (normalizedUnaccented == null || normalizedUnaccented.isBlank()) {
            return new String[0];
        }
        return normalizedUnaccented.split(" ");
    }

    /**
     * tsquery tiền tố cho {@code to_tsquery('simple', …)}: mọi từ AND với nhau, từ cuối có {@code :*}
     * ("tai ngh" → {@code tai & ngh:*}). Token chỉ còn chữ / số nên không cần escape ký tự đặc biệt của tsquery.
     */
    public static String prefixTsQuery(String normalizedUnaccented) {
        String[] tokens = tokens(normalizedUnaccented);
        if (tokens.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                sb.append(" & ");
            }
            sb.append(tokens[i]);
            if (i == tokens.length - 1) {
                sb.append(":*");
            }
        }
        return sb.toString();
    }
}
