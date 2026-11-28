// Hộp thư chung của đội vận hành — chuông Admin Console (UC-NOT-01 bước 9; plan 2026-11-26 thông báo D7, N03.05)
package com.genie.notification.admin.application;

import com.genie.common.realtime.OnlineUsers;
import com.genie.common.realtime.UserChannel;
import com.genie.notification.admin.domain.AdminNotification;
import com.genie.notification.admin.domain.AdminNotificationRepository;
import com.genie.notification.inbox.application.InboxReadModel;
import com.genie.notification.inbox.application.NotificationEvent;
import com.genie.notification.inbox.application.NotificationView;
import com.genie.notification.inbox.application.NotificationViews;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ghi {@code admin_notifications} (không gắn người — cả đội dùng chung, đọc một là đọc chung) trong transaction mới,
 * rồi đẩy {@code NEW_NOTIFICATION} tới nhân viên <b>đang trực tuyến</b> có quyền xem loại đó ({@link AdminInboxRules}).
 * Cùng hai cửa như {@code NotificationFacade}: {@link #raise} (trong transaction nghiệp vụ) và {@link #raiseNow}
 * (từ listener sau commit). Lỗi chỉ ghi log.
 */
@Component
public class AdminInboxFacade {

    private static final Logger log = LoggerFactory.getLogger(AdminInboxFacade.class);

    private final AdminNotificationRepository repository;
    private final InboxReadModel readModel;
    private final OnlineUsers onlineUsers;
    private final UserChannel channel;
    private final TransactionTemplate requiresNew;

    public AdminInboxFacade(AdminNotificationRepository repository, InboxReadModel readModel, OnlineUsers onlineUsers,
                            UserChannel channel, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.readModel = readModel;
        this.onlineUsers = onlineUsers;
        this.channel = channel;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Ghi sau commit của transaction hiện tại (không có transaction → ghi ngay). */
    public void raise(StaffAlertKind kind, String title, String content, String referenceUrl) {
        Runnable task = () -> raiseNow(kind, title, content, referenceUrl);
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

    /** Ghi ngay (transaction mới) — dùng trong listener {@code AFTER_COMMIT}. */
    public void raiseNow(StaffAlertKind kind, String title, String content, String referenceUrl) {
        Objects.requireNonNull(kind, "kind");
        var type = kind.toType();
        try {
            requiresNew.executeWithoutResult(status -> {
                AdminNotification saved = repository.save(AdminNotification.builder()
                        .type(type)
                        .title(title.length() > 255 ? title.substring(0, 255) : title)
                        .content(content == null || content.isBlank() ? title : content)
                        .referenceUrl(referenceUrl)
                        .isRead(false)
                        .build());
                NotificationView view = NotificationViews.admin(saved);
                Set<Long> recipients = readModel.staffAllowed(onlineUsers.allOnline(),
                        AdminInboxRules.requiredPermission(type));
                for (Long staffId : recipients) {
                    long unread = readModel.viewer(staffId).map(readModel::unreadCount).orElse(0L);
                    channel.sendAfterCommit(staffId, UserChannel.NOTIFICATIONS, NotificationEvent.created(view, unread));
                }
            });
        } catch (RuntimeException e) {
            log.warn("Không ghi được thông báo nhân viên {}: {}", type, e.toString());
        }
    }
}
