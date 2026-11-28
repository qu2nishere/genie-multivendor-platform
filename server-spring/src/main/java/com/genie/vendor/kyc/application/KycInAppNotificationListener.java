// Thông báo in-app kết quả KYC cho chủ shop (UC-VND-03, UC-VND-04; plan 2026-11-26 thông báo D11b, N03.07)
package com.genie.vendor.kyc.application;

import com.genie.notification.inbox.application.NotificationCommand;
import com.genie.notification.inbox.application.NotificationFacade;
import com.genie.notification.inbox.application.NotificationLink;
import com.genie.notification.inbox.application.NotificationKind;
import com.genie.vendor.kyc.event.KycDecidedEvent;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Song song với email kết quả ({@code KycNotifier}): duyệt → {@code KYC_APPROVED}; từ chối / yêu cầu bổ sung →
 * {@code KYC_REJECTED} ({@code metadata.event} = {@code REJECTED} / {@code SUPPLEMENT_REQUESTED}). Đích bấm vào: Hồ sơ KYC.
 */
@Component
public class KycInAppNotificationListener {

    private static final DateTimeFormatter DEADLINE = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final NotificationFacade notifications;
    private final VendorProfileRepository profiles;

    public KycInAppNotificationListener(NotificationFacade notifications, VendorProfileRepository profiles) {
        this.notifications = notifications;
        this.profiles = profiles;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDecided(KycDecidedEvent e) {
        String shop = profiles.findById(e.vendorProfileId()).map(p -> p.getShopName()).orElse("của bạn");
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("event", e.status());
        meta.put("shop_name", shop);
        NotificationKind type;
        String title;
        String body;
        switch (e.status()) {
            case "VERIFIED" -> {
                type = NotificationKind.KYC_APPROVED;
                title = "Gian hàng " + shop + " đã được duyệt";
                body = "Hồ sơ KYC đã được duyệt. Bạn có thể bắt đầu đăng bán sản phẩm.";
            }
            case "SUPPLEMENT_REQUESTED" -> {
                type = NotificationKind.KYC_REJECTED;
                title = "Cần bổ sung hồ sơ KYC";
                String deadline = e.deadlineAt() == null ? ""
                        : " Hạn bổ sung: " + e.deadlineAt().atZoneSameInstant(VN).format(DEADLINE) + ".";
                body = (e.reason() == null || e.reason().isBlank() ? "Hồ sơ cần bổ sung giấy tờ." : e.reason()) + deadline;
                meta.put("supplement_documents", e.supplementDocuments());
            }
            case "REJECTED" -> {
                type = NotificationKind.KYC_REJECTED;
                title = "Hồ sơ KYC bị từ chối";
                body = e.reason() == null || e.reason().isBlank() ? "Hồ sơ KYC của gian hàng chưa đạt." : e.reason();
            }
            default -> {
                return;
            }
        }
        notifications.sendNow(new NotificationCommand(e.vendorUserId(), type, title, body, null,
                NotificationLink.of(NotificationLink.KYC), meta));
    }
}
