package com.genie.common.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/** Dựng thật mẫu {@code templates/email/otp.html} (cùng engine SpEL như ứng dụng) — bắt lỗi cú pháp Thymeleaf sớm. */
class OtpEmailTemplateTest {

    private final SpringTemplateEngine engine = engine();

    private static SpringTemplateEngine engine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private String render(String lang, String name, String bannerUrl) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("lang", lang);
        vars.put("name", name);
        vars.put("otp", "684468");
        vars.put("expiresMinutes", 5L);
        vars.put("action", "en".equals(lang) ? "creating your Genie Shop account" : "tạo tài khoản Genie Shop");
        vars.put("webUrl", "http://localhost:4321");
        vars.put("shopName", MailBranding.SHOP_NAME);
        vars.put("brandColor", MailBranding.BRAND_COLOR);
        vars.put("brandTint", MailBranding.BRAND_TINT);
        vars.put("bannerUrl", bannerUrl);
        vars.put("bannerWidth", MailBranding.BANNER_WIDTH);
        vars.put("bannerHeight", MailBranding.BANNER_HEIGHT);
        vars.put("lightUrl", MailBranding.LIGHT_URL);
        vars.put("lightWidth", MailBranding.LIGHT_WIDTH);
        return engine.process(MailTemplate.OTP.templateName(), new Context(Locale.forLanguageTag(lang), vars));
    }

    @Test
    void vietnameseEmailHasGreetingCodeValidityAndWishes() {
        String html = render("vi", "Nguyễn Văn A", null);

        assertThat(html).contains("Xin chào").contains("Nguyễn Văn A")
                .contains("color:#1F7356;background-color:#E8F5EF;font-weight:bold");   // tên nổi bật xanh lá
        assertThat(html).contains("684468");
        assertThat(html).contains("Mã có hiệu lực trong").contains("5 phút").doesNotContain("⏱");
        assertThat(html).contains("font-family:Arial,Helvetica,sans-serif;font-size:48px");   // mã OTP to, cùng font "5 phút"
        assertThat(html).doesNotContain("border-left");                                     // khung vàng không còn vạch trái
        assertThat(html).contains("dồi dào sức khỏe");
        assertThat(html).contains("cid:genie-light");                                      // đèn thần góc phải
        assertThat(html).contains(">Genie Shop</div>").doesNotContain("cid:genie-banner"); // không có banner → dải chữ
    }

    @Test
    void defaultBrandingUsesInlineBanner() {
        String html = render("vi", "Nguyễn Văn A", MailBranding.BANNER_URL);
        assertThat(html).contains("src=\"cid:genie-banner\"").contains("src=\"cid:genie-light\"");
    }

    @Test
    void englishEmailAndBannerImage() {
        String html = render("en", "Mai Tran", "https://s3.genie.vn/content-assets/email/banner.png");

        assertThat(html).contains("Hello").contains("Mai Tran").contains("5 minutes").contains("good health");
        assertThat(html).contains("<img").contains("https://s3.genie.vn/content-assets/email/banner.png")
                .contains("width=\"600\"").contains("height=\"200\"");
    }

    @Test
    void customerNameIsEscaped() {
        String html = render("vi", "<script>alert(1)</script>", null);
        assertThat(html).doesNotContain("<script>alert(1)</script>").contains("&lt;script&gt;");
    }
}
