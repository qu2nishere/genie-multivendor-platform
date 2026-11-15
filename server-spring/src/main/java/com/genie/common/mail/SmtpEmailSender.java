package com.genie.common.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Gửi email HTML qua SMTP (Mailpit ở local, Gmail SMTP / Mailtrap khi demo — TS 3.2).
 * Timeout 10 s (cấu hình {@code spring.mail.properties}), thử lại 3 lần 2 s / 4 s (TS 10.2).
 */
@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final String from;
    private final String webUrl;

    public SmtpEmailSender(JavaMailSender mailSender, ITemplateEngine templateEngine,
                           @Value("${genie.mail-from:no-reply@genie.local}") String from,
                           @Value("${genie.web-url:http://localhost:4321}") String webUrl) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.from = from;
        this.webUrl = webUrl;
    }

    @Override
    @Retryable(retryFor = MailException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void send(String to, MailTemplate template, Map<String, Object> variables, Locale locale) {
        Locale loc = locale == null ? Locale.forLanguageTag("vi") : locale;
        Map<String, Object> vars = new HashMap<>(variables == null ? Map.of() : variables);
        vars.putIfAbsent("lang", "en".equals(loc.getLanguage()) ? "en" : "vi");
        vars.putIfAbsent("appName", "Genie");
        vars.putIfAbsent("webUrl", webUrl);
        vars.putIfAbsent("shopName", MailBranding.SHOP_NAME);
        vars.putIfAbsent("brandColor", MailBranding.BRAND_COLOR);
        vars.putIfAbsent("brandTint", MailBranding.BRAND_TINT);
        vars.putIfAbsent("bannerUrl", MailBranding.BANNER_URL);
        vars.putIfAbsent("bannerWidth", MailBranding.BANNER_WIDTH);
        vars.putIfAbsent("bannerHeight", MailBranding.BANNER_HEIGHT);
        vars.putIfAbsent("lightUrl", MailBranding.LIGHT_URL);
        vars.putIfAbsent("lightWidth", MailBranding.LIGHT_WIDTH);
        String html = templateEngine.process(template.templateName(), new Context(loc, vars));
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");   // multipart: có ảnh inline
            // Hộp thư hiện tên "Genie Shop" thay cho địa chỉ; MAIL_FROM dạng "Tên <địa chỉ>" thì giữ nguyên tên đó
            if (from.contains("<")) {
                helper.setFrom(from);
            } else {
                helper.setFrom(from, MailBranding.SHOP_NAME);
            }
            helper.setTo(to);
            helper.setSubject(template.subject(loc));
            helper.setText(html, true);
            // Chỉ đính kèm ảnh mẫu thật sự dùng — ảnh thừa sẽ hiện thành tệp đính kèm trong Gmail
            if (html.contains("cid:" + MailBranding.BANNER_CID)) {
                helper.addInline(MailBranding.BANNER_CID, new ClassPathResource(MailBranding.BANNER_FILE), "image/jpeg");
            }
            if (html.contains("cid:" + MailBranding.LIGHT_CID)) {
                helper.addInline(MailBranding.LIGHT_CID, new ClassPathResource(MailBranding.LIGHT_FILE), "image/png");
            }
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("Không dựng được email " + template, e);
        }
        mailSender.send(message);
    }
}
