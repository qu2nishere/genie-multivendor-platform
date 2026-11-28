package com.genie.common.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Đẩy tin realtime tới một người dùng qua kênh riêng {@code /user/queue/...} (API_SPEC mục 13). Gọi trong transaction
 * thì chỉ gửi <b>sau khi commit</b> (rollback → không gửi tin sai). Người dùng không có kết nối nào → tin bị bỏ qua
 * (simple broker không lưu); lỗi gửi không làm hỏng nghiệp vụ.
 */
@Component
public class UserChannel {

    /** Sự kiện tài khoản: phiên bị thu hồi (khóa, xóa, đổi quyền, đổi mật khẩu…). */
    public static final String ACCOUNT = "/queue/account";

    /** Thông báo mới / đã đọc — chuông header (API_SPEC 13; UC-NOT-01, UC-NOT-05). */
    public static final String NOTIFICATIONS = "/queue/notifications";

    /** Kênh chung: thông báo sàn {@code target = ALL} (API_SPEC 13) — client nào đã đăng nhập cũng đăng ký được. */
    public static final String PLATFORM_TOPIC = "/topic/platform";

    private final SimpMessagingTemplate template;

    public UserChannel(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void sendAfterCommit(long userId, String destination, Object payload) {
        Runnable send = () -> {
            try {
                template.convertAndSendToUser(String.valueOf(userId), destination, payload);
            } catch (RuntimeException ignored) {
                // realtime chỉ là phụ — client vẫn bị chặn ở lần gọi API kế tiếp
            }
        };
        runAfterCommit(send);
    }

    /** Gửi lên kênh chung {@code /topic/**} (mọi người đang đăng ký nhận) — cùng luật sau commit / nuốt lỗi. */
    public void sendToTopicAfterCommit(String topic, Object payload) {
        runAfterCommit(() -> {
            try {
                template.convertAndSend(topic, payload);
            } catch (RuntimeException ignored) {
                // realtime chỉ là phụ — dữ liệu vẫn có ở API
            }
        });
    }

    /**
     * Gửi ngay tới một người — dùng trong listener {@code @TransactionalEventListener(AFTER_COMMIT)}: lúc đó đăng ký thêm
     * "sau commit" không còn chạy nữa (plan 2026-11-26 thông báo N07.01). Lỗi gửi bị bỏ qua.
     */
    public void sendNow(long userId, String destination, Object payload) {
        try {
            template.convertAndSendToUser(String.valueOf(userId), destination, payload);
        } catch (RuntimeException ignored) {
            // realtime chỉ là phụ
        }
    }

    /** Gửi ngay lên kênh chung — cùng lý do với {@link #sendNow}. */
    public void sendToTopicNow(String topic, Object payload) {
        try {
            template.convertAndSend(topic, payload);
        } catch (RuntimeException ignored) {
            // realtime chỉ là phụ
        }
    }

    private static void runAfterCommit(Runnable send) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
