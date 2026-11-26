// Thông báo bảo mật từ sự kiện của module auth (UC-AUTH-02, UC-NOT-01 bước 8; plan 2026-11-26 thông báo D11a, N03.06)
package com.genie.notification.inbox.application;

import com.genie.auth.login.event.NewDeviceLoginEvent;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Cạnh {@code notification → auth} được phép (README mục 3); {@code auth} không biết module này. */
@Component
public class AuthEventNotificationListener {

    private static final DateTimeFormatter VN_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final NotificationFacade notifications;

    public AuthEventNotificationListener(NotificationFacade notifications) {
        this.notifications = notifications;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNewDevice(NewDeviceLoginEvent e) {
        if (e.userId() == null) {
            return;
        }
        String device = e.deviceName() == null || e.deviceName().isBlank() ? "thiết bị mới" : e.deviceName();
        String when = e.loginAt() == null ? "" : " lúc " + NotificationViews.vn(e.loginAt()).format(VN_TIME);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("event", "NEW_DEVICE_LOGIN");
        meta.put("device_name", device);
        if (e.ipAddress() != null) {
            meta.put("ip_address", e.ipAddress());
        }
        notifications.sendNow(NotificationCommand.of(e.userId(), NotificationKind.NEW_DEVICE_LOGIN,
                "Đăng nhập từ thiết bị mới",
                "Tài khoản của bạn vừa đăng nhập trên " + device + when + ". Nếu không phải bạn, hãy đổi mật khẩu ngay.",
                NotificationLink.of(NotificationLink.SECURITY)).withMetadata(meta));
    }
}
