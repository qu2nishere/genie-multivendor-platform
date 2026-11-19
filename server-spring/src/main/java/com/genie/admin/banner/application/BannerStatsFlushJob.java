package com.genie.admin.banner.application;

import com.genie.admin.banner.application.BannerEventService.EventType;
import com.genie.admin.banner.domain.HomeBannerRepository;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ghi gộp bộ đếm banner từ Redis vào PostgreSQL mỗi 5 phút (LLD 10; plan banner B05.04). Với mỗi banner trong
 * {@code banner:stats:dirty}: {@code GETDEL} hai bộ đếm (nguyên tử — chạy song song không cộng trùng) rồi
 * {@code addStats} trong transaction; ghi DB lỗi thì {@code INCRBY} trả lại Redis để không mất số.
 */
@Component
public class BannerStatsFlushJob {

    private static final Logger log = LoggerFactory.getLogger(BannerStatsFlushJob.class);

    private final StringRedisTemplate redis;
    private final HomeBannerRepository banners;
    private final TransactionTemplate tx;

    public BannerStatsFlushJob(StringRedisTemplate redis, HomeBannerRepository banners, TransactionTemplate tx) {
        this.redis = redis;
        this.banners = banners;
        this.tx = tx;
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void scheduled() {
        try {
            flush();
        } catch (Exception e) {
            log.warn("Ghi gộp bộ đếm banner lỗi, thử lại ở lượt sau: {}", e.getMessage());
        }
    }

    /** @return số banner đã ghi gộp */
    public int flush() {
        Set<String> dirty = redis.opsForSet().members(BannerEventService.DIRTY);
        if (dirty == null || dirty.isEmpty()) {
            return 0;
        }
        int written = 0;
        for (String raw : dirty) {
            redis.opsForSet().remove(BannerEventService.DIRTY, raw);
            long id;
            try {
                id = Long.parseLong(raw);
            } catch (NumberFormatException e) {
                continue;
            }
            String impKey = BannerEventService.counterKey(id, EventType.IMPRESSION);
            String clkKey = BannerEventService.counterKey(id, EventType.CLICK);
            long imp = parse(redis.opsForValue().getAndDelete(impKey));
            long clk = parse(redis.opsForValue().getAndDelete(clkKey));
            if (imp == 0 && clk == 0) {
                continue;
            }
            try {
                tx.executeWithoutResult(s -> banners.addStats(id, imp, clk));
                written++;
            } catch (RuntimeException e) {
                // trả số lại Redis để lượt sau ghi tiếp
                if (imp > 0) redis.opsForValue().increment(impKey, imp);
                if (clk > 0) redis.opsForValue().increment(clkKey, clk);
                redis.opsForSet().add(BannerEventService.DIRTY, raw);
                log.warn("Không ghi được bộ đếm banner {} — đã trả lại Redis: {}", id, e.getMessage());
            }
        }
        if (written > 0) {
            log.info("Đã ghi gộp bộ đếm của {} banner", written);
        }
        return written;
    }

    private static long parse(String v) {
        try {
            return v == null ? 0 : Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
