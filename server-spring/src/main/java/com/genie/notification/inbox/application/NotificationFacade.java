// Cửa duy nhất để module khác tạo thông báo cá nhân + đẩy realtime (UC-NOT-01; plan 2026-11-26 thông báo D5, D10, N03.03–N03.04)
package com.genie.notification.inbox.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.common.realtime.UserChannel;
import com.genie.notification.inbox.domain.Notification;
import com.genie.notification.inbox.domain.NotificationChannel;
import com.genie.notification.inbox.domain.NotificationDeliveryStatus;
import com.genie.notification.inbox.domain.NotificationRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ghi thông báo vào hộp thư (kênh {@code IN_APP}) trong một <b>transaction mới</b>, rồi sau commit của chính dòng đó đẩy
 * {@code NEW_NOTIFICATION} lên {@code /user/queue/notifications} (kèm {@code unread_count}). Mọi lỗi chỉ ghi log — nghiệp
 * vụ gốc không bao giờ hỏng vì thông báo (N3).
 * <ul>
 *   <li>{@link #send} — gọi <b>bên trong</b> transaction nghiệp vụ: chỉ ghi khi transaction đó commit (rollback → không có
 *       thông báo). Không có transaction thì ghi ngay.</li>
 *   <li>{@link #sendNow} — gọi từ listener {@code @TransactionalEventListener(AFTER_COMMIT)}: lúc đó đăng ký thêm
 *       "sau commit" sẽ không chạy nữa, nên ghi ngay (transaction mới).</li>
 * </ul>
 * Dòng {@code IN_APP} lưu xong là đã tới hộp thư → {@code delivery_status = SENT}; đẩy WebSocket chỉ là phụ (N4).
 */
@Component
public class NotificationFacade {

    private static final Logger log = LoggerFactory.getLogger(NotificationFacade.class);

    private final NotificationRepository notifications;
    private final InboxReadModel readModel;
    private final UserChannel channel;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final TransactionTemplate requiresNew;

    public NotificationFacade(NotificationRepository notifications, InboxReadModel readModel, UserChannel channel,
                              NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper, Clock clock,
                              PlatformTransactionManager transactionManager) {
        this.notifications = notifications;
        this.readModel = readModel;
        this.channel = channel;
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Một thông báo, ghi sau commit của transaction hiện tại. */
    public void send(NotificationCommand cmd) {
        sendMany(List.of(cmd));
    }

    /** Nhiều thông báo (vd mỗi shop của một đơn), một transaction, ghi sau commit của transaction hiện tại. */
    public void sendMany(List<NotificationCommand> cmds) {
        if (cmds.isEmpty()) {
            return;
        }
        List<NotificationCommand> copy = List.copyOf(cmds);
        afterCurrentCommit(() -> deliver(copy));
    }

    /** Ghi ngay (transaction mới) — dùng trong listener {@code AFTER_COMMIT}. */
    public void sendNow(NotificationCommand cmd) {
        deliver(List.of(cmd));
    }

    /** Nhiều thông báo, ghi ngay trong một transaction mới — dùng trong listener {@code AFTER_COMMIT}. */
    public void sendManyNow(List<NotificationCommand> cmds) {
        if (!cmds.isEmpty()) {
            deliver(List.copyOf(cmds));
        }
    }

    /**
     * Tin nhắn chat mới khi người nhận không trực tuyến (UC-SOC-01; plan chat M04.03): giữ <b>một</b> thông báo
     * {@code CHAT_MESSAGE} chưa đọc cho mỗi (người nhận, hội thoại) — có rồi thì cập nhật nội dung + đưa lên đầu
     * ({@code uk_notifications_chat_unread}, V27). Ghi sau commit của transaction hiện tại.
     */
    public void upsertChat(long recipientId, long conversationId, String title, String body) {
        afterCurrentCommit(() -> deliverChat(recipientId, conversationId, title, body));
    }

    void deliver(List<NotificationCommand> cmds) {
        try {
            requiresNew.executeWithoutResult(status -> {
                OffsetDateTime now = OffsetDateTime.now(clock);
                for (NotificationCommand c : cmds) {
                    Notification n = notifications.save(build(c, now));
                    push(c.userId(), NotificationViews.personal(n));
                }
            });
        } catch (RuntimeException e) {
            log.warn("Không ghi được thông báo ({} dòng, loại {}): {}", cmds.size(), cmds.get(0).type(), e.toString());
        }
    }

    void deliverChat(long recipientId, long conversationId, String title, String body) {
        try {
            requiresNew.executeWithoutResult(status -> {
                Long id = jdbc.queryForObject("""
                        INSERT INTO notifications (user_id, type, title, body, reference_id, reference_type, channel,
                                                   delivery_status, retry_count, is_read, sent_at, created_at)
                        VALUES (:uid, 'CHAT_MESSAGE', :title, :body, :ref, 'CONVERSATION', 'IN_APP', 'SENT', 0, FALSE,
                                now(), now())
                        ON CONFLICT (user_id, reference_id) WHERE type = 'CHAT_MESSAGE' AND is_read = FALSE
                        DO UPDATE SET title = EXCLUDED.title, body = EXCLUDED.body, created_at = EXCLUDED.created_at,
                                      sent_at = EXCLUDED.sent_at
                        RETURNING id""",
                        new MapSqlParameterSource("uid", recipientId).addValue("title", truncate(title, 255))
                                .addValue("body", body).addValue("ref", String.valueOf(conversationId)), Long.class);
                notifications.findById(id).ifPresent(n -> push(recipientId, NotificationViews.personal(n)));
            });
        } catch (RuntimeException e) {
            log.warn("Không ghi được thông báo chat cho người dùng {}: {}", recipientId, e.toString());
        }
    }

    private void push(long userId, NotificationView view) {
        long unread = readModel.viewer(userId).map(readModel::unreadCount).orElse(0L);
        channel.sendAfterCommit(userId, UserChannel.NOTIFICATIONS, NotificationEvent.created(view, unread));
    }

    private Notification build(NotificationCommand c, OffsetDateTime now) {
        NotificationLink link = c.link();
        return Notification.builder()
                .userId(c.userId())
                .type(c.type().toType())
                .title(truncate(c.title(), 255))
                .body(c.body())
                .imageUrl(c.imageUrl())
                .referenceType(link == null ? null : link.type())
                .referenceId(link == null || link.id() == null ? null : String.valueOf(link.id()))
                .metadataJson(json(c.metadata()))
                .channel(NotificationChannel.IN_APP)
                .deliveryStatus(NotificationDeliveryStatus.SENT)
                .retryCount(0)
                .isRead(false)
                .sentAt(now)
                .build();
    }

    private String json(Map<String, ?> metadata) {
        if (metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    /** Chạy {@code task} sau commit của transaction hiện tại; không có transaction → chạy ngay. */
    static void afterCurrentCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
