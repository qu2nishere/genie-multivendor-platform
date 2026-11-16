package com.genie.auth.deletion.application;

import com.genie.auth.deletion.event.AccountAnonymizedEvent;
import com.genie.auth.deletion.event.AccountDeletionRequestedEvent;
import com.genie.auth.profile.application.AccountSummaryService;
import com.genie.common.mail.EmailSender;
import com.genie.common.mail.MailTemplate;
import com.genie.common.storage.UploadPurpose;
import com.genie.common.storage.UploadService;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Việc phụ sau commit của UC-AUTH-10, chạy nền (TS 4.5): email báo đã nhận yêu cầu xóa; xóa ảnh đại diện trên Silo khi
 * tài khoản đã ẩn danh. Lỗi chỉ ghi log — không ảnh hưởng thao tác chính.
 */
@Component
public class AccountDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionListener.class);
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final EmailSender emailSender;
    private final AccountSummaryService summaries;
    private final UploadService uploads;

    public AccountDeletionListener(EmailSender emailSender, AccountSummaryService summaries, UploadService uploads) {
        this.emailSender = emailSender;
        this.summaries = summaries;
        this.uploads = uploads;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeletionRequested(AccountDeletionRequestedEvent event) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("name", nameOf(event.userId(), event.email()));
            vars.put("email", event.email());
            vars.put("requestedAt", format(event.requestedAt(), DATE_TIME));
            vars.put("anonymizeAt", format(event.anonymizeAt(), DATE));
            vars.put("graceDays", AccountDeletionService.GRACE_PERIOD.toDays());
            String lang = summaries.preferredLanguageOf(event.userId());
            emailSender.send(event.email(), MailTemplate.DELETION_SCHEDULED, vars, Locale.forLanguageTag(lang));
        } catch (RuntimeException e) {
            log.warn("Không gửi được email xóa tài khoản cho user {}: {}", event.userId(), e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnonymized(AccountAnonymizedEvent event) {
        if (event.avatarUrl() == null) {
            return;
        }
        uploads.keyOfPublicUrl(UploadPurpose.AVATAR, event.avatarUrl())
                .ifPresent(key -> uploads.deleteQuietly(UploadPurpose.AVATAR, key));
    }

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

    private static String format(OffsetDateTime t, DateTimeFormatter f) {
        return t == null ? "" : t.atZoneSameInstant(VIETNAM).format(f);
    }
}
