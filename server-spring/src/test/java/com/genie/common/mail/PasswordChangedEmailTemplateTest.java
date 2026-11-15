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

/** Dựng thật mẫu {@code templates/email/password-changed.html} — bắt lỗi cú pháp Thymeleaf và kiểm nội dung theo method. */
class PasswordChangedEmailTemplateTest {

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

    private String render(String lang, String method, String device, String ip) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("lang", lang);
        vars.put("name", "Nguyễn Văn A");
        vars.put("email", "an.nguyen+test@gmail.com");
        vars.put("method", method);
        vars.put("changedAt", "22:30 15/11/2026");
        vars.put("device", device);
        vars.put("ip", ip);
        vars.put("webUrl", "http://localhost:4321");
        vars.put("shopName", MailBranding.SHOP_NAME);
        vars.put("brandColor", MailBranding.BRAND_COLOR);
        vars.put("brandTint", MailBranding.BRAND_TINT);
        vars.put("bannerUrl", MailBranding.BANNER_URL);
        vars.put("bannerWidth", MailBranding.BANNER_WIDTH);
        vars.put("bannerHeight", MailBranding.BANNER_HEIGHT);
        vars.put("lightUrl", MailBranding.LIGHT_URL);
        vars.put("lightWidth", MailBranding.LIGHT_WIDTH);
        return engine.process(MailTemplate.PASSWORD_CHANGED.templateName(), new Context(Locale.forLanguageTag(lang), vars));
    }

    @Test
    void resetEmailHasGreetingDetailsWarningAndResetButton() {
        String html = render("vi", "RESET", "Mac · Safari 18", "203.0.113.7");

        assertThat(html).contains("Xin chào").contains("Nguyễn Văn A")
                .contains("color:#1F7356;background-color:#E8F5EF;font-weight:bold");
        assertThat(html).contains("<b>đặt lại</b>").contains("mọi thiết bị đang đăng nhập đã được đăng xuất");
        assertThat(html).contains("22:30 15/11/2026").contains("Mac · Safari 18").contains("203.0.113.7")
                .contains("an.nguyen+test@gmail.com");
        assertThat(html).contains("Không phải bạn thực hiện?").contains("Đặt lại mật khẩu ngay")
                .contains("href=\"http://localhost:4321/auth/forgot?email=an.nguyen");
        assertThat(html).contains("dồi dào sức khỏe").contains("src=\"cid:genie-banner\"").contains("src=\"cid:genie-light\"");
    }

    @Test
    void changeAndSetupUseTheirOwnWording() {
        assertThat(render("vi", "CHANGE", "", "")).contains("<b>thay đổi</b>").doesNotContain("<b>đặt lại</b>")
                .doesNotContain("Thiết bị:").doesNotContain("Địa chỉ IP:");
        assertThat(render("vi", "SETUP", null, null)).contains("<b>thiết lập mật khẩu</b>");
    }

    @Test
    void englishEmail() {
        String html = render("en", "RESET", "iPhone · Safari 17", "198.51.100.2");
        assertThat(html).contains("Hello").contains("was <b>reset</b>").contains("Not you?")
                .contains("Reset my password").contains("good health").doesNotContain("Xin chào");
    }
}
