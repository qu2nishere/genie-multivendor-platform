// Lọc HTML do người dùng soạn trước khi lưu (plan 2026-11-26 thông báo D13, N02.07; API_SPEC 11.17)
package com.genie.common.html;

import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

/**
 * Giữ thẻ định dạng cơ bản ({@code p, br, b, strong, i, em, u, s, ul, ol, li, blockquote, h2, h3, a, img}); bỏ mọi thứ
 * còn lại ({@code script}, {@code iframe}, {@code style}, thuộc tính {@code on*}, {@code style=}…).
 * <ul>
 *   <li>{@code a}: chỉ {@code https:} hoặc đường dẫn tương đối bắt đầu bằng {@code /} (không {@code //}); luôn thêm
 *       {@code rel="noopener noreferrer" target="_blank"}.</li>
 *   <li>{@code img}: chỉ {@code src} bắt đầu bằng một trong {@code allowedImagePrefixes} (kho tệp công khai của sàn);
 *       ảnh khác bị bỏ.</li>
 * </ul>
 * Lớp thuần (không phụ thuộc Spring) — bean tạo ở {@link HtmlSanitizerConfig}.
 */
public class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.none()
            .addTags("p", "br", "b", "strong", "i", "em", "u", "s", "ul", "ol", "li", "blockquote", "h2", "h3", "a", "img")
            .addAttributes("a", "href")
            .addAttributes("img", "src", "alt")
            .addProtocols("a", "href", "https")
            .preserveRelativeLinks(true);

    private final List<String> allowedImagePrefixes;

    public HtmlSanitizer(List<String> allowedImagePrefixes) {
        this.allowedImagePrefixes = List.copyOf(allowedImagePrefixes);
    }

    /** HTML an toàn để lưu và hiển thị; chuỗi rỗng khi đầu vào {@code null}. */
    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        // baseUri giả để Safelist nhận đường dẫn tương đối; preserveRelativeLinks giữ nguyên dạng "/…"
        Document dirty = Jsoup.parseBodyFragment(html, "https://genie.invalid/");
        Document clean = new Cleaner(SAFELIST).clean(dirty);
        for (Element a : clean.select("a")) {
            String href = a.attr("href").trim();
            if (!(href.startsWith("https://") || (href.startsWith("/") && !href.startsWith("//")))) {
                a.unwrap();   // bỏ liên kết, giữ chữ
                continue;
            }
            a.attr("rel", "noopener noreferrer");
            a.attr("target", "_blank");
        }
        for (Element img : clean.select("img")) {
            String src = img.attr("src").trim();
            if (allowedImagePrefixes.stream().noneMatch(src::startsWith)) {
                img.remove();
            }
        }
        clean.outputSettings().prettyPrint(false);
        return clean.body().html().trim();
    }

    /** Văn bản thuần (bỏ thẻ, gộp khoảng trắng), cắt tối đa {@code max} ký tự ở ranh giới từ, thêm "…" khi bị cắt. */
    public static String plainPreview(String html, int max) {
        String text = html == null ? "" : Jsoup.parse(html).text().replaceAll("\\s+", " ").trim();
        if (text.length() <= max) {
            return text;
        }
        int cut = text.lastIndexOf(' ', max - 1);
        if (cut < max / 2) {
            cut = max - 1;
        }
        return text.substring(0, cut).trim() + "…";
    }
}
