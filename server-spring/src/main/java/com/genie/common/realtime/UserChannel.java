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
