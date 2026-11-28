// Lọc HTML mô tả sản phẩm chống XSS — UC-CAT-09 bước 14 (plan C1 D9; API_SPEC 10.5)
package com.genie.catalog.product.application;

import com.genie.common.storage.ObjectStorage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Whitelist cố định: đoạn, xuống dòng, đậm / nghiêng / gạch chân, tiêu đề h2–h3, danh sách, liên kết {@code http(s)}
 * (tự thêm {@code rel="nofollow noopener"}, mở tab mới), ảnh — ảnh chỉ giữ khi {@code src} là ảnh của bucket
 * {@code product-media} trên kho của sàn (không nhúng ảnh ngoài để chống theo dõi / nội dung lạ). Mọi thứ khác (script,
 * style, iframe, thuộc tính {@code on*}, {@code javascript:}) bị bỏ.
 */
@Component
public class HtmlSanitizer {

    /** Số ký tự chữ hiển thị tối đa (UC-CAT-09 bước 14). */
    public static final int MAX_TEXT_LENGTH = 10_000;
    /** Giới hạn HTML sau lọc — chặn mô tả toàn thẻ rỗng. */
    public static final int MAX_HTML_LENGTH = 50_000;

    /** Kết quả lọc: HTML an toàn + số ký tự chữ hiển thị. */
    public record Result(String html, int textLength) {
    }

    private static final Safelist SAFELIST = new Safelist()
            .addTags("p", "br", "strong", "b", "em", "i", "u", "h2", "h3", "ul", "ol", "li", "a", "img")
            .addAttributes("a", "href")
            .addAttributes("img", "src", "alt")
            .addProtocols("a", "href", "http", "https")
            .addProtocols("img", "src", "http", "https")
            .addEnforcedAttribute("a", "rel", "nofollow noopener")
            .addEnforcedAttribute("a", "target", "_blank");

    private final String imagePrefix;

    @Autowired
    public HtmlSanitizer(ObjectStorage storage) {
        this(storage.publicUrl(MediaUrls.PRODUCT_MEDIA_BUCKET, ""));
    }

    /** Dùng cho test: {@code imagePrefix} = URL công khai gốc của bucket ảnh sản phẩm. */
    HtmlSanitizer(String imagePrefix) {
        this.imagePrefix = imagePrefix;
    }

    public Result sanitize(String html) {
        if (html == null || html.isBlank()) {
            return new Result("", 0);
        }
        Document.OutputSettings out = new Document.OutputSettings().prettyPrint(false);
        String cleaned = Jsoup.clean(html, "", SAFELIST, out);
        Document doc = Jsoup.parseBodyFragment(cleaned);
        doc.outputSettings(out);
        for (Element img : doc.select("img")) {
            String src = img.attr("src");
            if (!src.startsWith(imagePrefix)) {
                img.remove();
            }
        }
        String safe = doc.body().html();
        return new Result(safe, doc.body().text().length());
    }

    /** Gốc URL ảnh hợp lệ trong mô tả (để service đổi ảnh {@code tmp/} sang khóa chính thức). */
    public String imagePrefix() {
        return imagePrefix;
    }
}
