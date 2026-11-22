package com.genie.catalog.search.application;

import com.genie.catalog.search.domain.KeywordNormalizer;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.Hashing;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Đếm lượt tìm theo từ khóa trên Redis (UC-CAT-04 bước 5; plan 2026-11-17 A6). <b>Không ghi DB trong request</b> (plan N6) —
 * {@link SearchKeywordFlushJob} gộp 5 phút một lần vào {@code search_keyword_stats}.
 * <p>Chống spam: một IP chỉ được đếm một lần cho cùng từ khóa trong {@value #DEDUPE_MINUTES} phút ({@code SET NX EX}).
 * Redis lỗi → bỏ qua (thống kê, không phải dữ liệu tiền — plan N7).
 */
@Component
public class SearchKeywordCounter {

    static final long DEDUPE_MINUTES = 10;
    static final int MIN_LENGTH = 2;
    static final Duration DAILY_TTL = Duration.ofDays(8);

    private static final Logger log = LoggerFactory.getLogger(SearchKeywordCounter.class);

    private final StringRedisTemplate redis;
    private final Clock clock;

    public SearchKeywordCounter(StringRedisTemplate redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    /** @param keyword từ khóa đã chuẩn hóa, còn dấu ({@link KeywordNormalizer#normalize}) — giữ dấu để hiển thị ở "Xu hướng" */
    public void record(String keyword, ClientInfo client) {
        if (keyword == null || keyword.length() < MIN_LENGTH) {
            return;
        }
        try {
            String ipHash = Hashing.sha256Hex(client.ip() == null ? "unknown" : client.ip()).substring(0, 16);
            Boolean first = redis.opsForValue().setIfAbsent(SearchKeys.dedupe(ipHash, keyword), "1",
                    Duration.ofMinutes(DEDUPE_MINUTES));
            if (!Boolean.TRUE.equals(first)) {
                return;
            }
            LocalDate day = SearchKeys.today(clock);
            String dailyKey = SearchKeys.daily(day);
            redis.opsForZSet().incrementScore(dailyKey, keyword, 1);
            redis.expire(dailyKey, DAILY_TTL);
            redis.opsForHash().increment(SearchKeys.PENDING, day + "|" + keyword, 1);
        } catch (RuntimeException e) {
            log.warn("Bỏ qua đếm từ khóa vì Redis lỗi: {}", e.getMessage());
        }
    }
}
