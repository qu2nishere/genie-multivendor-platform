package com.genie.support;

import com.genie.common.mail.EmailSender;
import com.genie.common.mail.MailTemplate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thay SMTP trong test: ghi lại thư đã "gửi" để test đọc mã OTP / kiểm nội dung (plan AUTH A01.08).
 */
public class FakeEmailSender implements EmailSender {

    public record SentMail(String to, MailTemplate template, Map<String, Object> variables, Locale locale) {
    }

    private final List<SentMail> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(String to, MailTemplate template, Map<String, Object> variables, Locale locale) {
        sent.add(new SentMail(to, template, Map.copyOf(variables == null ? Map.of() : variables), locale));
    }

    public List<SentMail> sent() {
        return List.copyOf(sent);
    }

    public Optional<SentMail> last(String to, MailTemplate template) {
        for (int i = sent.size() - 1; i >= 0; i--) {
            SentMail m = sent.get(i);
            if (m.to().equals(to) && m.template() == template) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    /** Mã OTP trong thư gần nhất gửi tới {@code to}. */
    public String lastOtp(String to) {
        return last(to, MailTemplate.OTP).map(m -> String.valueOf(m.variables().get("otp")))
                .orElseThrow(() -> new AssertionError("Chưa có email OTP gửi tới " + to));
    }

    public void clear() {
        sent.clear();
    }
}
