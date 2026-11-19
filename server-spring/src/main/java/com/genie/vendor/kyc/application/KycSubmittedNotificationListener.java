package com.genie.vendor.kyc.application;

import com.genie.vendor.kyc.event.KycSubmittedEvent;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Sau commit hồ sơ KYC mới: báo nhân viên duyệt qua {@link KycAdminNotifier} (plan Mở shop & KYC K04.09). */
@Component
public class KycSubmittedNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(KycSubmittedNotificationListener.class);

    private final List<KycAdminNotifier> notifiers;
    private final VendorProfileRepository profiles;

    public KycSubmittedNotificationListener(List<KycAdminNotifier> notifiers, VendorProfileRepository profiles) {
        this.notifiers = notifiers;
        this.profiles = profiles;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmitted(KycSubmittedEvent event) {
        if (notifiers.isEmpty()) {
            log.info("Hồ sơ KYC mới của shop {} chờ thẩm định (chưa có module thông báo cho nhân viên)", event.vendorProfileId());
            return;
        }
        String shopName = profiles.findById(event.vendorProfileId()).map(p -> p.getShopName()).orElse("");
        for (KycAdminNotifier n : notifiers) {
            try {
                n.kycSubmitted(event.vendorProfileId(), shopName);
            } catch (RuntimeException e) {
                log.warn("Không gửi được thông báo KYC mới: {}", e.getMessage());
            }
        }
    }
}
