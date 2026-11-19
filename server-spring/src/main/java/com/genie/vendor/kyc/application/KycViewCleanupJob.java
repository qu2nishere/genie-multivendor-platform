package com.genie.vendor.kyc.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Xóa bản giải mã tạm của giấy tờ KYC quá 10 phút (plan Mở shop & KYC D5, K04.04; LUU_TRU 5.7). */
@Component
public class KycViewCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(KycViewCleanupJob.class);

    private final KycDossierStorage dossiers;

    public KycViewCleanupJob(KycDossierStorage dossiers) {
        this.dossiers = dossiers;
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    public void run() {
        int n = dossiers.purgeExpiredViews();
        if (n > 0) {
            log.info("Đã xóa {} bản xem tạm giấy tờ KYC", n);
        }
    }
}
