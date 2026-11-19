package com.genie.catalog.search.application;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Job 5 phút: ghi bộ đếm từ khóa từ Redis xuống {@code search_keyword_stats} (UC-CAT-04 bước 5; LLD 10; plan 2026-11-17 A6).
 * <ol>
 *   <li>khóa job {@code SET NX} — nhiều instance chỉ một bản chạy;</li>
 *   <li>{@code RENAME search:kw:pending → search:kw:flushing} (nguyên tử: lượt tìm mới ghi vào pending mới);
 *       nếu {@code flushing} còn từ lần lỗi trước thì ghi nó trước, không đổi tên;</li>
 *   <li>UPSERT cộng dồn — đúng {@code SearchKeywordStat.accumulate()} nhưng một câu SQL cho cả lô;</li>
 *   <li>ghi xong mới {@code DEL flushing}; DB lỗi thì giữ để lần sau ghi lại (không mất, không đếm đôi).</li>
 * </ol>
 */
@Component
public class SearchKeywordFlushJob {

    private static final Logger log = LoggerFactory.getLogger(SearchKeywordFlushJob.class);
    static final int MAX_KEYWORD_LENGTH = 100;

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbc;

    public SearchKeywordFlushJob(StringRedisTemplate redis, JdbcTemplate jdbc) {
        this.redis = redis;
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void run() {
        try {
            Boolean locked = redis.opsForValue().setIfAbsent(SearchKeys.FLUSH_LOCK, "1", Duration.ofMinutes(4));
            if (!Boolean.TRUE.equals(locked)) {
                return;
            }
            try {
                flush();
            } finally {
                redis.delete(SearchKeys.FLUSH_LOCK);
            }
        } catch (RuntimeException e) {
            log.warn("Ghi thống kê từ khóa thất bại, thử lại lần sau: {}", e.getMessage());
        }
    }

    /** Ghi một lô; trả số dòng (ngày, từ khóa) đã ghi. Public để test gọi trực tiếp (cron tắt trong test). */
    @Transactional
    public int flush() {
        if (!Boolean.TRUE.equals(redis.hasKey(SearchKeys.FLUSHING))) {
            if (!Boolean.TRUE.equals(redis.hasKey(SearchKeys.PENDING))) {
                return 0;
            }
            redis.rename(SearchKeys.PENDING, SearchKeys.FLUSHING);
        }
        Map<Object, Object> entries = redis.opsForHash().entries(SearchKeys.FLUSHING);
        List<Object[]> rows = new ArrayList<>();
        entries.forEach((field, value) -> {
            String f = field.toString();
            int sep = f.indexOf('|');
            if (sep <= 0) {
                return;
            }
            String keyword = f.substring(sep + 1);
            if (keyword.isEmpty() || keyword.length() > MAX_KEYWORD_LENGTH) {
                return;
            }
            rows.add(new Object[] {keyword, LocalDate.parse(f.substring(0, sep)), Integer.parseInt(value.toString())});
        });
        if (!rows.isEmpty()) {
            jdbc.batchUpdate("""
                    INSERT INTO search_keyword_stats (normalized_keyword, stat_date, search_count, result_click_count, is_blocked)
                    VALUES (?, ?, ?, 0, FALSE)
                    ON CONFLICT (normalized_keyword, stat_date)
                    DO UPDATE SET search_count = search_keyword_stats.search_count + EXCLUDED.search_count""", rows);
        }
        // Chỉ xóa bản flushing SAU KHI commit — rollback thì giữ lại để lần sau ghi lại
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redis.delete(SearchKeys.FLUSHING);
                }
            });
        } else {
            redis.delete(SearchKeys.FLUSHING);
        }
        return rows.size();
    }
}
