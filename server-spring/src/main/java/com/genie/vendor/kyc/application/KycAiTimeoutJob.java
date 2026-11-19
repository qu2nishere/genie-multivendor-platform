package com.genie.vendor.kyc.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hồ sơ {@code AI_PROCESSING} quá {@code genie.kyc.ai-timeout} (30 giây, tính từ lúc nộp) mà AI Worker chưa gọi lại →
 * chuyển duyệt tay, {@code errors = ["AI_TIMEOUT"]} (LLD 9.6; plan Mở shop & KYC N7, K05.10). Chạy mỗi 15 giây,
 * {@code FOR UPDATE SKIP LOCKED}.
 */
@Component
public class KycAiTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(KycAiTimeoutJob.class);

    private final KycDecisionService decisions;
    private final KycProperties props;

    public KycAiTimeoutJob(KycDecisionService decisions, KycProperties props) {
        this.decisions = decisions;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "PT15S", initialDelayString = "PT30S")
    public int run() {
        int n = decisions.timeoutStuck(props.aiTimeout());
        if (n > 0) {
            log.warn("{} hồ sơ KYC quá hạn chờ AI Worker → chuyển duyệt tay", n);
        }
        return n;
    }
}
