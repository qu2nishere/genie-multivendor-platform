package com.genie.common.mail;

import java.util.Locale;
import java.util.Map;

/**
 * Cổng gửi email (plan AUTH D12 — đặt ở {@code common} để {@code auth} và {@code notification} cùng dùng mà không phụ thuộc
 * nhau). Đặt tên {@code EmailSender} để không trùng {@code org.springframework.mail.MailSender}.
 * Gửi lỗi sau khi đã thử lại → ném {@link org.springframework.mail.MailException}.
 */
public interface EmailSender {

    void send(String to, MailTemplate template, Map<String, Object> variables, Locale locale);
}
