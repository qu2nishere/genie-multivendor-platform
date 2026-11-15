package com.genie.auth.token.application;

import com.genie.auth.token.domain.RefreshTokenRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cron 03:00 hằng ngày: xóa refresh token đã hết hạn / đã thu hồi quá 90 ngày (giữ 90 ngày để nhận diện thiết bị lạ —
 * plan AUTH A07.10).
 */
@Component
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);
    private static final int KEEP_DAYS = 90;

    private final RefreshTokenRepository tokens;
    private final Clock clock;

    public RefreshTokenCleanupJob(RefreshTokenRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void purge() {
        int deleted = tokens.deleteStale(OffsetDateTime.now(clock).minusDays(KEEP_DAYS));
        log.info("Đã xóa {} refresh token cũ", deleted);
    }
}
