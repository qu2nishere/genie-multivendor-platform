// Lọc HTML thông báo sàn (plan 2026-11-26 thông báo D13, N02.07)
package com.genie.common.html;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

    private static final String CDN = "https://s3.genie.vn/content-assets/";
    private final HtmlSanitizer sanitizer = new HtmlSanitizer(List.of(CDN));

    @Test
    void keepsBasicFormatting() {
        String html = "<h2>Siêu sale</h2><p><b>Giảm</b> <i>tới</i> <u>50%</u></p><ul><li>A</li><li>B</li></ul>";
        assertThat(sanitizer.sanitize(html)).isEqualTo(html);
    }

    @Test
    void removesScriptsHandlersStylesAndFrames() {
        String out = sanitizer.sanitize("<p onclick=\"x()\" style=\"color:red\">Hi</p><script>alert(1)</script>"
                + "<iframe src=\"https://evil.com\"></iframe><style>p{}</style>");
        assertThat(out).isEqualTo("<p>Hi</p>");
    }

    @Test
    void neutralisesDangerousLinksButKeepsText() {
        String out = sanitizer.sanitize("<a href=\"javascript:alert(1)\">bấm</a> <a href=\"//evil.com\">x</a>"
                + " <a href=\"http://plain.com\">y</a>");
        assertThat(out).doesNotContain("<a").contains("bấm").contains("x").contains("y");
    }

    @Test
    void safeLinksGetRelAndTarget() {
        String out = sanitizer.sanitize("<a href=\"https://genie.vn/flash-sale\">FS</a><a href=\"/c/dien-tu\">Điện tử</a>");
        assertThat(out).contains("href=\"https://genie.vn/flash-sale\"").contains("href=\"/c/dien-tu\"");
        assertThat(out.split("rel=\"noopener noreferrer\"", -1)).hasSize(3);
        assertThat(out.split("target=\"_blank\"", -1)).hasSize(3);
    }

    @Test
    void keepsOnlyPlatformImages() {
        String out = sanitizer.sanitize("<p><img src=\"" + CDN + "notice/1/a.webp\" alt=\"banner\">"
                + "<img src=\"https://evil.com/x.png\" onerror=\"x()\"></p>");
        assertThat(out).contains(CDN + "notice/1/a.webp").contains("alt=\"banner\"")
                .doesNotContain("evil.com").doesNotContain("onerror");
    }

    @Test
    void nullOrBlankBecomesEmpty() {
        assertThat(sanitizer.sanitize(null)).isEmpty();
        assertThat(sanitizer.sanitize("   ")).isEmpty();
    }

    @Test
    void plainPreviewStripsTagsAndCutsAtWordBoundary() {
        assertThat(HtmlSanitizer.plainPreview("<p>Genie <b>nâng cấp</b>\n hệ thống</p>", 160))
                .isEqualTo("Genie nâng cấp hệ thống");
        String longText = "<p>" + "chữ ".repeat(60) + "</p>";
        String preview = HtmlSanitizer.plainPreview(longText, 30);
        assertThat(preview).endsWith("…").hasSizeLessThanOrEqualTo(30);
        assertThat(preview).doesNotContain("ch…");
    }
}
