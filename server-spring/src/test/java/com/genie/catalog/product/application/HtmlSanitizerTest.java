package com.genie.catalog.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Lọc HTML mô tả sản phẩm (plan C1 D9). */
class HtmlSanitizerTest {

    private static final String PREFIX = "https://s3.genie.vn/product-media/";
    private final HtmlSanitizer sanitizer = new HtmlSanitizer(PREFIX);

    @Test
    void keepsFormattingAndHardensLinks() {
        HtmlSanitizer.Result r = sanitizer.sanitize(
                "<h2>Đặc điểm</h2><p><strong>Pin</strong> <em>40 giờ</em></p><ul><li>ANC</li></ul>"
                        + "<a href=\"https://genie.vn/p/x-1\">xem</a>");
        assertThat(r.html()).contains("<h2>Đặc điểm</h2>", "<strong>Pin</strong>", "<li>ANC</li>")
                .contains("rel=\"nofollow noopener\"").contains("target=\"_blank\"");
        assertThat(r.textLength()).isBetween("Đặc điểmPin 40 giờANCxem".length(), 30);   // chỉ đếm chữ, không đếm thẻ
    }

    @Test
    void stripsScriptsHandlersAndForeignImages() {
        HtmlSanitizer.Result r = sanitizer.sanitize("<p onclick=\"steal()\">a</p><script>alert(1)</script>"
                + "<a href=\"javascript:alert(1)\">x</a><iframe src=\"https://evil\"></iframe>"
                + "<img src=\"https://evil.example/track.gif\" onerror=\"x()\">"
                + "<img src=\"" + PREFIX + "vendor/5/product/6/a.webp\" alt=\"ảnh\">");
        assertThat(r.html()).doesNotContain("script", "onclick", "onerror", "javascript", "iframe", "evil.example")
                .contains(PREFIX + "vendor/5/product/6/a.webp");
    }

    @Test
    void blankInputGivesEmptyResult() {
        assertThat(sanitizer.sanitize(null).html()).isEmpty();
        assertThat(sanitizer.sanitize("   ").textLength()).isZero();
    }
}
