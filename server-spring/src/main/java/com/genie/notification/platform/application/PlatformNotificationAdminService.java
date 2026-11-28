// Soạn / phát / hẹn giờ / sửa / xóa thông báo toàn sàn — UC-NOT-05 (API_SPEC 11.17; plan 2026-11-26 thông báo D1–D3, D13–D16, N06.03–N06.06)
package com.genie.notification.platform.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.error.InvalidRequestException.Violation;
import com.genie.common.html.HtmlSanitizer;
import com.genie.common.security.ClientInfo;
import com.genie.common.storage.UploadPurpose;
import com.genie.common.storage.UploadService;
import com.genie.notification.platform.application.PlatformNotificationViews.Command;
import com.genie.notification.platform.domain.PlatformNotification;
import com.genie.notification.platform.domain.PlatformNotificationRepository;
import com.genie.notification.platform.domain.PlatformNotificationTarget;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Luật (D13–D16): tiêu đề 1–100; nội dung lọc HTML rồi 1–20.000 ký tự; ảnh là khóa {@code tmp/} của chính người gửi
 * ({@code NOTIFICATION_IMAGE}, chuyển sang {@code notice/{id}/…}); {@code SPECIFIC_USERS} cần 1–500 email đều là
 * khách / shop đang hoạt động ({@code GNE-NOT-4221} + {@code unknown_emails}); {@code send_at} = {@code null} (phát ngay)
 * hoặc now + 1 phút … now + 90 ngày. Sửa chỉ khi chưa phát ({@code GNE-NOT-4091}); xóa được mọi lúc. Mỗi thao tác ghi
 * {@code system_logs}.
 */
@Service
public class PlatformNotificationAdminService {

    static final int TITLE_MAX = 100;
    static final int CONTENT_MAX = 20_000;
    static final int RECIPIENTS_MAX = 500;
    static final Duration MIN_LEAD = Duration.ofMinutes(1);
    static final Duration MAX_LEAD = Duration.ofDays(90);
    static final String IMAGE_PREFIX = "notice/";

    private final PlatformNotificationRepository notifications;
    private final PlatformAudience audience;
    private final PlatformNotificationQueryService query;
    private final HtmlSanitizer sanitizer;
    private final UploadService uploads;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PlatformNotificationAdminService(PlatformNotificationRepository notifications, PlatformAudience audience,
                                            PlatformNotificationQueryService query, HtmlSanitizer sanitizer,
                                            UploadService uploads, AuditLogger audit, ApplicationEventPublisher events,
                                            ObjectMapper objectMapper, Clock clock) {
        this.notifications = notifications;
        this.audience = audience;
        this.query = query;
        this.sanitizer = sanitizer;
        this.uploads = uploads;
        this.audit = audit;
        this.events = events;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** Nội dung đã kiểm: HTML sạch, đối tượng, id người nhận. */
    private record Checked(String title, String html, PlatformNotificationTarget target, List<Long> recipientIds,
                           OffsetDateTime sendAt) {
    }

    @Transactional
    public PlatformNotificationViews.Detail create(long adminId, Command cmd, ClientInfo client) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        Checked c = check(cmd, now, true);
        PlatformNotification n = notifications.save(PlatformNotification.builder()
                .title(c.title())
                .content(c.html())
                .target(c.target())
                .recipientUserIdsJson(c.target() == PlatformNotificationTarget.SPECIFIC_USERS ? json(c.recipientIds()) : null)
                .createdBy(adminId)
                .sendAt(c.sendAt())
                .reachCount(0L)
                .build());
        if (cmd.imageObjectKey() != null && !cmd.imageObjectKey().isBlank()) {
            n.setImageUrl(promote(adminId, n.getId(), cmd.imageObjectKey().trim()));
        }
        if (c.sendAt() == null) {
            dispatch(n, c.recipientIds(), now);
        }
        Map<String, Object> details = base(n);
        details.put("scheduled", c.sendAt() != null);
        details.put("reach_count", n.getReachCount());
        audit.record(AuditAction.PLATFORM_NOTIFICATION_PUBLISHED, adminId, String.valueOf(n.getId()), details, client, true);
        notifications.flush();   // chi tiết đọc bằng SQL — đẩy ảnh / dispatched_at xuống trước
        return query.detail(n);
    }

    @Transactional
    public PlatformNotificationViews.Detail update(long adminId, long id, Command cmd, ClientInfo client) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        PlatformNotification n = find(id);
        if (!n.isEditable()) {
            throw new BusinessException(ErrorCode.PLATFORM_NOTIFICATION_DISPATCHED);
        }
        Checked c = check(cmd, now, false);
        Map<String, Object> before = snapshot(n);
        n.changeContent(c.title(), c.html(), c.target(),
                c.target() == PlatformNotificationTarget.SPECIFIC_USERS ? json(c.recipientIds()) : null, c.sendAt());
        String newKey = cmd.imageObjectKey();
        if (newKey != null) {
            String old = n.getImageUrl();
            if (newKey.isBlank()) {
                n.setImageUrl(null);
                deleteAfterCommit(old);
            } else if (!newKey.trim().equals(old)) {
                n.setImageUrl(promote(adminId, n.getId(), newKey.trim()));
                deleteAfterCommit(old);
            }
        }
        if (c.sendAt() == null) {
            dispatch(n, c.recipientIds(), now);
        }
        Map<String, Object> details = base(n);
        details.put("changes", changes(before, snapshot(n)));
        details.put("dispatched", n.isDispatched());
        audit.record(AuditAction.PLATFORM_NOTIFICATION_UPDATED, adminId, String.valueOf(n.getId()), details, client, true);
        notifications.flush();
        return query.detail(n);
    }

    @Transactional
    public void delete(long adminId, long id, ClientInfo client) {
        PlatformNotification n = find(id);
        Map<String, Object> details = base(n);
        details.put("was_dispatched", n.isDispatched());
        details.put("reach_count", n.getReachCount());
        details.put("read_count", n.getReadCount() == null ? 0 : n.getReadCount());
        String image = n.getImageUrl();
        notifications.delete(n);   // lượt đọc xóa theo ON DELETE CASCADE
        deleteAfterCommit(image);
        audit.record(AuditAction.PLATFORM_NOTIFICATION_DELETED, adminId, String.valueOf(id), details, client, true);
    }

    /** Phát: chốt số người nhận lúc {@code at}, đẩy realtime sau commit (dùng chung với cron hẹn giờ — N07). */
    void dispatch(PlatformNotification n, List<Long> recipientIds, OffsetDateTime at) {
        n.dispatch(audience.count(n.getTarget(), recipientIds, at), at);
        events.publishEvent(new PlatformNotificationDispatchedEvent(n.getId()));
    }

    PlatformNotification find(long id) {
        return notifications.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                "Không tìm thấy thông báo."));
    }

    // ---------- kiểm dữ liệu ----------

    private Checked check(Command cmd, OffsetDateTime now, boolean creating) {
        List<Violation> errors = new ArrayList<>();
        String title = cmd.title() == null ? "" : cmd.title().trim();
        if (title.isEmpty()) {
            errors.add(new Violation("title", "REQUIRED", null, "Vui lòng nhập tiêu đề."));
        } else if (title.length() > TITLE_MAX) {
            errors.add(new Violation("title", "TOO_LONG", null, "Tiêu đề tối đa 100 ký tự."));
        }
        String html = sanitizer.sanitize(cmd.contentHtml());
        String text = HtmlSanitizer.plainPreview(html, Integer.MAX_VALUE);
        if (text.isBlank() && !html.contains("<img")) {
            errors.add(new Violation("content_html", "REQUIRED", null, "Vui lòng nhập nội dung."));
        } else if (html.length() > CONTENT_MAX) {
            errors.add(new Violation("content_html", "TOO_LONG", null, "Nội dung tối đa 20.000 ký tự."));
        }
        PlatformNotificationTarget target = null;
        if (cmd.target() == null || cmd.target().isBlank()) {
            errors.add(new Violation("target", "REQUIRED", null, "Vui lòng chọn đối tượng nhận."));
        } else {
            try {
                target = PlatformNotificationTarget.valueOf(cmd.target().trim());
            } catch (IllegalArgumentException e) {
                errors.add(new Violation("target", "INVALID_VALUE", cmd.target(), "Đối tượng nhận không hợp lệ."));
            }
        }
        List<String> emails = cmd.recipientEmails() == null ? List.of() : cmd.recipientEmails();
        if (target == PlatformNotificationTarget.SPECIFIC_USERS && emails.size() > RECIPIENTS_MAX) {
            errors.add(new Violation("recipient_emails", "TOO_MANY", emails.size(), "Tối đa 500 người nhận."));
        }
        if (cmd.sendAt() != null) {
            if (cmd.sendAt().isBefore(now.plus(MIN_LEAD))) {
                errors.add(new Violation("send_at", "TOO_SOON", null, "Giờ hẹn phải sau thời điểm hiện tại ít nhất 1 phút."));
            } else if (cmd.sendAt().isAfter(now.plus(MAX_LEAD))) {
                errors.add(new Violation("send_at", "TOO_LATE", null, "Chỉ hẹn giờ trong vòng 90 ngày."));
            }
        }
        if (cmd.imageObjectKey() != null && !cmd.imageObjectKey().isBlank() && creating
                && !cmd.imageObjectKey().trim().startsWith("tmp/")) {
            errors.add(new Violation("image_object_key", "INVALID_FORMAT", null, "Ảnh không hợp lệ, vui lòng tải lại."));
        }
        if (!errors.isEmpty()) {
            throw new InvalidRequestException(errors);
        }
        List<Long> ids = List.of();
        if (target == PlatformNotificationTarget.SPECIFIC_USERS) {
            PlatformAudience.Resolved r = audience.resolveEmails(emails);
            if (r.ids().isEmpty() || !r.unknown().isEmpty()) {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("unknown_emails", r.unknown());
                throw new BusinessException(ErrorCode.PLATFORM_NOTIFICATION_RECIPIENTS, details);
            }
            ids = r.ids();
        }
        return new Checked(title, html, target, ids, cmd.sendAt());
    }

    // ---------- ảnh ----------

    private String promote(long adminId, long id, String tmpKey) {
        if (!tmpKey.startsWith("tmp/")) {
            throw InvalidRequestException.of("image_object_key", "INVALID_FORMAT", tmpKey, "Ảnh không hợp lệ, vui lòng tải lại.");
        }
        String key = uploads.promoteTo(adminId, UploadPurpose.NOTIFICATION_IMAGE, tmpKey, IMAGE_PREFIX + id + "/").objectKey();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    uploads.deleteQuietly(UploadPurpose.NOTIFICATION_IMAGE, key);
                }
            }
        });
        return key;
    }

    private void deleteAfterCommit(String key) {
        if (key == null || key.isBlank() || !key.startsWith(IMAGE_PREFIX)) {
            return;   // ảnh seed / khóa lạ không xóa
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                uploads.deleteQuietly(UploadPurpose.NOTIFICATION_IMAGE, key);
            }
        });
    }

    // ---------- nhật ký ----------

    private static Map<String, Object> base(PlatformNotification n) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("target_label", PlatformNotificationViews.code(n.getId()) + " · " + n.getTitle());
        d.put("target", n.getTarget().name());
        return d;
    }

    private static Map<String, Object> snapshot(PlatformNotification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", n.getTitle());
        m.put("target", n.getTarget().name());
        m.put("recipients", n.getRecipientUserIdsJson());
        m.put("send_at", n.getSendAt() == null ? null : n.getSendAt().toString());
        m.put("image", n.getImageUrl());
        m.put("content_length", n.getContent() == null ? 0 : n.getContent().length());
        return m;
    }

    private static Map<String, Object> changes(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String k : after.keySet()) {
            if (!Objects.equals(before.get(k), after.get(k))) {
                out.put(k, Map.of("from", String.valueOf(before.get(k)), "to", String.valueOf(after.get(k))));
            }
        }
        return out;
    }

    private String json(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    List<Long> recipientIds(PlatformNotification n) {
        if (n.getRecipientUserIdsJson() == null || n.getRecipientUserIdsJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(n.getRecipientUserIdsJson(), new TypeReference<List<Long>>() { });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
