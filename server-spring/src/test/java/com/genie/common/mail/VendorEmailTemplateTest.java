package com.genie.common.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/** Dựng thật 4 mẫu email của cụm Mở shop & KYC (plan K05.11, K06.11, K08.05) — bắt lỗi cú pháp Thymeleaf. */
class VendorEmailTemplateTest {

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

    private String render(MailTemplate t, String lang, Map<String, Object> extra) {
        Map<String, Object> vars = new HashMap<>(extra);
        vars.put("lang", lang);
        vars.put("name", "Đặng Quốc Huy");
        vars.put("storeName", "TechZone Official");
        vars.put("webUrl", "http://localhost:4321");
        vars.put("shopName", MailBranding.SHOP_NAME);
        vars.put("brandColor", MailBranding.BRAND_COLOR);
        vars.put("brandTint", MailBranding.BRAND_TINT);
        vars.put("bannerUrl", MailBranding.BANNER_URL);
        vars.put("bannerWidth", MailBranding.BANNER_WIDTH);
        vars.put("bannerHeight", MailBranding.BANNER_HEIGHT);
        vars.put("lightUrl", MailBranding.LIGHT_URL);
        vars.put("lightWidth", MailBranding.LIGHT_WIDTH);
        return engine.process(t.templateName(), new Context(Locale.forLanguageTag(lang), vars));
    }

    @Test
    void approvedAndRejected() {
        assertThat(render(MailTemplate.KYC_APPROVED, "vi", Map.of()))
                .contains("TechZone Official").contains("đã được <b>duyệt</b>").contains("http://localhost:4321/seller");
        String rejected = render(MailTemplate.KYC_REJECTED, "vi",
                Map.of("reason", "ảnh giấy tờ mờ, không đọc được", "canRequestReview", true));
        assertThat(rejected).contains("ảnh giấy tờ mờ").contains("Yêu cầu nhân viên xem xét").contains("/seller/kyc");
        assertThat(render(MailTemplate.KYC_REJECTED, "en", Map.of("reason", "x", "canRequestReview", false)))
                .contains("not approved").doesNotContain("ask our staff");
    }

    @Test
    void supplementAndPayout() {
        assertThat(render(MailTemplate.KYC_SUPPLEMENT, "vi", Map.of("documents", List.of("CCCD mặt trước"),
                "note", "Ảnh bị lóa", "deadline", "15/06/2026 10:00")))
                .contains("CCCD mặt trước").contains("Ảnh bị lóa").contains("15/06/2026 10:00");
        Map<String, Object> payout = new HashMap<>();
        payout.put("action", "UPDATED");
        payout.put("bankName", "Vietcombank");
        payout.put("accountMasked", "•••• •••• 6712");
        payout.put("changedAt", "22:30 15/11/2026");
        assertThat(render(MailTemplate.PAYOUT_ACCOUNT_CHANGED, "vi", payout))
                .contains("Vietcombank").contains("6712").contains("cập nhật");
    }
}
