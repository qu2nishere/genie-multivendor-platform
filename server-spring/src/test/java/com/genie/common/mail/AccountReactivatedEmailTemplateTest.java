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

/** Dựng thật mẫu {@code templates/email/account-reactivated.html} (UC-ADM-03, plan 2026-11-24 H03.06). */
class AccountReactivatedEmailTemplateTest {

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

    private String render(String lang, String reason) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("lang", lang);
        vars.put("name", "Nguyễn Văn A");
        vars.put("email", "an.nguyen@gmail.com");
        vars.put("reason", reason);
        vars.put("reactivatedAt", "14:05 01/12/2026");
        vars.put("webUrl", "http://localhost:4321");
        vars.put("shopName", MailBranding.SHOP_NAME);
        vars.put("brandColor", MailBranding.BRAND_COLOR);
        vars.put("brandTint", MailBranding.BRAND_TINT);
        vars.put("bannerUrl", MailBranding.BANNER_URL);
        vars.put("bannerWidth", MailBranding.BANNER_WIDTH);
        vars.put("bannerHeight", MailBranding.BANNER_HEIGHT);
        vars.put("lightUrl", MailBranding.LIGHT_URL);
        vars.put("lightWidth", MailBranding.LIGHT_WIDTH);
        return engine.process(MailTemplate.ACCOUNT_REACTIVATED.templateName(), new Context(Locale.forLanguageTag(lang), vars));
    }

    @Test
    void expiredSuspensionEmailHasGreetingTimeAccountAndSignInButton() {
        String html = render("vi", "SUSPENSION_EXPIRED");

        assertThat(html).contains("Xin chào").contains("Nguyễn Văn A");
        assertThat(html).contains("Thời hạn đình chỉ").contains("<b>hoạt động lại</b>").doesNotContain("xem xét lại");
        assertThat(html).contains("14:05 01/12/2026").contains("an.nguyen@gmail.com");
        assertThat(html).contains("href=\"http://localhost:4321/auth/login\"").contains(">Đăng nhập<");
        assertThat(html).contains("có thể bị đình chỉ lại").contains("src=\"cid:genie-banner\"");
    }

    @Test
    void manualReactivationUsesReviewWording() {
        String html = render("vi", "ADMIN");
        assertThat(html).contains("xem xét lại").contains("<b>mở khóa</b>").doesNotContain("Thời hạn đình chỉ");
    }

    @Test
    void englishEmail() {
        String html = render("en", "SUSPENSION_EXPIRED");
        assertThat(html).contains("Hello").contains("<b>active again</b>").contains(">Sign in<")
                .contains("Your Genie Shop account is active again since").doesNotContain("Xin chào");
        assertThat(MailTemplate.ACCOUNT_REACTIVATED.subject(Locale.ENGLISH)).isEqualTo("Your Genie Shop account has been reactivated");
        assertThat(MailTemplate.ACCOUNT_REACTIVATED.subject(Locale.forLanguageTag("vi")))
                .isEqualTo("Tài khoản Genie Shop của bạn đã được mở lại");
    }
}
