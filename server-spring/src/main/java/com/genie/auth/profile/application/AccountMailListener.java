package com.genie.auth.profile.application;

import com.genie.auth.login.event.NewDeviceLoginEvent;
import com.genie.auth.password.event.PasswordChangedEvent;
import com.genie.auth.token.application.UserAgentParser;
import com.genie.common.mail.EmailSender;
import com.genie.common.mail.MailTemplate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Email cảnh báo bảo mật gửi SAU khi transaction chính commit, chạy nền (TS 4.5): đổi / đặt lại mật khẩu (UC-AUTH-04, 06),
 * đăng nhập từ thiết bị lạ (UC-AUTH-02 luồng 8a). Gửi lỗi chỉ ghi log — không ảnh hưởng thao tác của người dùng.
 */
@Component
public class AccountMailListener {

    private static final Logger log = LoggerFactory.getLogger(AccountMailListener.class);
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final EmailSender emailSender;
    private final AccountSummaryService summaries;

    public AccountMailListener(EmailSender emailSender, AccountSummaryService summaries) {
        this.emailSender = emailSender;
        this.summaries = summaries;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordChanged(PasswordChangedEvent event) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", nameOf(event.userId(), event.email()));
        vars.put("email", event.email());
        vars.put("method", event.method() == null ? "CHANGE" : event.method());
        vars.put("changedAt", format(event.changedAt()));
        vars.put("device", event.userAgent() == null ? "" : UserAgentParser.parse(event.userAgent()).name());
        vars.put("ip", event.ipAddress() == null ? "" : event.ipAddress());
        send(event.userId(), event.email(), MailTemplate.PASSWORD_CHANGED, vars);
    }

    /** Tên hiển thị trong lời chào: họ tên hồ sơ, không có thì phần trước @ của email. */
    private String nameOf(Long userId, String email) {
        try {
            String name = summaries.summaryOf(userId).fullName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        } catch (RuntimeException e) {
            log.debug("Không đọc được tên user {}: {}", userId, e.getMessage());
        }
        int at = email == null ? -1 : email.indexOf('@');
        return at > 0 ? email.substring(0, at) : "";
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNewDevice(NewDeviceLoginEvent event) {
        send(event.userId(), event.email(), MailTemplate.NEW_DEVICE_LOGIN, Map.of(
                "device", event.deviceName() == null ? "" : event.deviceName(),
                "ip", event.ipAddress() == null ? "" : event.ipAddress(),
                "loginAt", format(event.loginAt())));
    }

    private void send(Long userId, String email, MailTemplate template, Map<String, Object> vars) {
        try {
            String lang = summaries.preferredLanguageOf(userId);
            emailSender.send(email, template, vars, Locale.forLanguageTag(lang));
        } catch (MailException | IllegalStateException e) {
            log.warn("Không gửi được email {} cho user {}: {}", template, userId, e.getMessage());
        }
    }

    private static String format(OffsetDateTime t) {
        return t == null ? "" : t.atZoneSameInstant(VIETNAM).format(FORMAT);
    }
}
