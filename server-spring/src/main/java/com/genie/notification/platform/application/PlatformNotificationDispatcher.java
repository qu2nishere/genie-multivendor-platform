// Đẩy realtime thông báo sàn vừa phát tới người đang trực tuyến thuộc đối tượng (UC-NOT-05 bước 3; API_SPEC 13; plan 2026-11-26 thông báo D4, D5, N07.01)
package com.genie.notification.platform.application;

import com.genie.common.realtime.OnlineUsers;
import com.genie.common.realtime.UserChannel;
import com.genie.notification.inbox.application.InboxQueryService;
import com.genie.notification.inbox.application.InboxReadModel;
import com.genie.notification.inbox.application.NotificationEvent;
import com.genie.notification.inbox.application.NotificationView;
import com.genie.notification.platform.domain.PlatformNotification;
import com.genie.notification.platform.domain.PlatformNotificationRepository;
import com.genie.notification.platform.domain.PlatformNotificationTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * {@code ALL} → một tin lên {@code /topic/platform}; đối tượng khác → giao người đang kết nối ({@link OnlineUsers}) với
 * đối tượng (SQL theo lô {@value #BATCH}) rồi gửi riêng từng người qua {@code /user/queue/notifications} kèm
 * {@code unread_count} — không lộ nội dung cho người ngoài đối tượng (D4). Chạy sau commit; lỗi chỉ ghi log.
 */
@Component
public class PlatformNotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PlatformNotificationDispatcher.class);
    static final int BATCH = 500;

    private final PlatformNotificationRepository notifications;
    private final PlatformAudience audience;
    private final PlatformNotificationAdminService admin;
    private final PlatformNotificationQueryService query;
    private final InboxReadModel readModel;
    private final OnlineUsers onlineUsers;
    private final UserChannel channel;

    public PlatformNotificationDispatcher(PlatformNotificationRepository notifications, PlatformAudience audience,
                                          PlatformNotificationAdminService admin, PlatformNotificationQueryService query,
                                          InboxReadModel readModel, OnlineUsers onlineUsers, UserChannel channel) {
        this.notifications = notifications;
        this.audience = audience;
        this.admin = admin;
        this.query = query;
        this.readModel = readModel;
        this.onlineUsers = onlineUsers;
        this.channel = channel;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDispatched(PlatformNotificationDispatchedEvent e) {
        try {
            notifications.findById(e.platformNotificationId()).ifPresent(this::push);
        } catch (RuntimeException ex) {
            log.warn("Không đẩy được thông báo sàn {}: {}", e.platformNotificationId(), ex.toString());
        }
    }

    void push(PlatformNotification n) {
        if (!n.isDispatched()) {
            return;
        }
        NotificationView view = InboxQueryService.platformView(n.getId(), n.getTitle(), n.getContent(),
                query.imageUrl(n.getImageUrl()), n.getDispatchedAt(), false);
        if (n.getTarget() == PlatformNotificationTarget.ALL) {
            channel.sendToTopicNow(com.genie.common.realtime.UserChannel.PLATFORM_TOPIC, NotificationEvent.platform(view, null));
            return;
        }
        List<Long> recipientIds = admin.recipientIds(n);
        List<Long> online = new ArrayList<>(onlineUsers.allOnline());
        int sent = 0;
        for (int i = 0; i < online.size(); i += BATCH) {
            Set<Long> targets = audience.among(online.subList(i, Math.min(online.size(), i + BATCH)), n.getTarget(),
                    recipientIds, n.getDispatchedAt());
            for (Long userId : targets) {
                long unread = readModel.viewer(userId).map(readModel::unreadCount).orElse(0L);
                channel.sendNow(userId, UserChannel.NOTIFICATIONS, NotificationEvent.platform(view, unread));
                sent++;
            }
        }
        log.debug("Thông báo sàn {} đã đẩy tới {} người đang trực tuyến", n.getId(), sent);
    }
}
