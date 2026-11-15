package com.genie.common.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/** Người gửi hiện tên "Genie Shop" (ẩn địa chỉ trong danh sách thư) và ảnh banner / đèn thần được đính kèm inline. */
class SmtpEmailSenderTest {

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

    private MimeMessage send(String from) throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        new SmtpEmailSender(mailSender, engine(), from, "http://localhost:4321").send("khach@gmail.com",
                MailTemplate.OTP, Map.of("name", "Khoa", "otp", "123456", "expiresMinutes", 5L, "action", "đăng nhập"),
                Locale.forLanguageTag("vi"));
        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        MimeMessage message = sent.getValue();
        message.saveChanges();
        return message;
    }

    @Test
    void senderShowsShopNameAndInlineImagesAreAttached() throws Exception {
        MimeMessage message = send("shop@gmail.com");

        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertThat(from.getPersonal()).isEqualTo("Genie Shop");
        assertThat(from.getAddress()).isEqualTo("shop@gmail.com");
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        message.writeTo(raw);
        assertThat(raw.toString()).contains("Content-ID: <genie-banner>").contains("Content-ID: <genie-light>");
    }

    @Test
    void explicitDisplayNameInMailFromIsKept() throws Exception {
        InternetAddress from = (InternetAddress) send("Genie CSKH <shop@gmail.com>").getFrom()[0];
        assertThat(from.getPersonal()).isEqualTo("Genie CSKH");
    }
}
