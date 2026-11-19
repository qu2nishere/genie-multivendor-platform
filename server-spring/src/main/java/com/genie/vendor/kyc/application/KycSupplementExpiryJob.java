package com.genie.vendor.kyc.application;

import com.genie.vendor.kyc.event.KycDecidedEvent;
import com.genie.vendor.shop.domain.KycStatus;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hết hạn yêu cầu bổ sung KYC 7 ngày → {@code REJECTED} "Quá hạn bổ sung hồ sơ" + email (UC-VND-04; API 12.2; LLD 10;
 * plan Mở shop & KYC K06.10). Chạy 01:00 hằng ngày giờ Việt Nam, {@code FOR UPDATE SKIP LOCKED}, mỗi lượt 100 hồ sơ.
 */
@Component
public class KycSupplementExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(KycSupplementExpiryJob.class);
    static final String REASON = "Quá hạn bổ sung hồ sơ";

    private final VendorProfileRepository profiles;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public KycSupplementExpiryJob(VendorProfileRepository profiles, ApplicationEventPublisher events, Clock clock) {
        this.profiles = profiles;
        this.events = events;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public int run() {
        List<VendorProfile> expired = profiles.findSupplementExpired(OffsetDateTime.now(clock));
        for (VendorProfile p : expired) {
            p.expireSupplement(REASON);
            events.publishEvent(new KycDecidedEvent(p.getId(), p.getUserId(), KycStatus.REJECTED.name(), REASON, List.of(), null));
        }
        if (!expired.isEmpty()) {
            log.info("{} hồ sơ KYC quá hạn bổ sung → REJECTED", expired.size());
        }
        return expired.size();
    }
}
