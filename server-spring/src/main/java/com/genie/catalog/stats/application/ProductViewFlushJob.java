package com.genie.catalog.stats.application;

import com.genie.catalog.stats.domain.TrafficSource;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
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
 * Job 5 phút: ghi lượt xem từ Redis xuống {@code product_view_stats} + {@code products.view_count}
 * (UC-CAT-06 bước 9; LLD 10; plan 2026-11-17 A10). Cùng khuôn với {@code SearchKeywordFlushJob}:
 * khóa job → {@code RENAME pending → flushing} → UPSERT → xóa {@code flushing} sau commit.
 * {@code unique_visitor_count} = {@code PFCOUNT} của HyperLogLog trong ngày (lấy max với giá trị đã ghi).
 */
@Component
public class ProductViewFlushJob {

    private static final Logger log = LoggerFactory.getLogger(ProductViewFlushJob.class);
    static final String LOCK = "job:pv-flush";

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbc;

    public ProductViewFlushJob(StringRedisTemplate redis, JdbcTemplate jdbc) {
        this.redis = redis;
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT2M")
    public void run() {
        try {
            Boolean locked = redis.opsForValue().setIfAbsent(LOCK, "1", Duration.ofMinutes(4));
            if (!Boolean.TRUE.equals(locked)) {
                return;
            }
            try {
                flush();
            } finally {
                redis.delete(LOCK);
            }
        } catch (RuntimeException e) {
            log.warn("Ghi lượt xem thất bại, thử lại lần sau: {}", e.getMessage());
        }
    }

    private record Row(LocalDate day, long productId, TrafficSource source, int views) {
    }

    /** Ghi một lô; trả số dòng (ngày, sản phẩm, nguồn). Public để test gọi trực tiếp. */
    @Transactional
    public int flush() {
        if (!Boolean.TRUE.equals(redis.hasKey(ProductViewCounter.FLUSHING))) {
            if (!Boolean.TRUE.equals(redis.hasKey(ProductViewCounter.PENDING))) {
                return 0;
            }
            redis.rename(ProductViewCounter.PENDING, ProductViewCounter.FLUSHING);
        }
        List<Row> rows = new ArrayList<>();
        redis.opsForHash().entries(ProductViewCounter.FLUSHING).forEach((field, value) -> {
            String[] parts = field.toString().split("\\|");
            if (parts.length != 3) {
                return;
            }
            try {
                rows.add(new Row(LocalDate.parse(parts[0]), Long.parseLong(parts[1]), TrafficSource.valueOf(parts[2]),
                        Integer.parseInt(value.toString())));
            } catch (IllegalArgumentException ignored) {
                // dòng hỏng — bỏ qua
            }
        });

        List<Object[]> stats = new ArrayList<>();
        Map<Long, Long> perProduct = new HashMap<>();
        for (Row r : rows) {
            Long unique = redis.opsForHyperLogLog().size(ProductViewCounter.uniqueKey(r.day(), r.productId(), r.source()));
            stats.add(new Object[] {r.productId(), r.day(), r.source().name(), r.views(), unique == null ? 0 : unique.intValue(),
                    r.productId()});
            perProduct.merge(r.productId(), (long) r.views(), Long::sum);
        }
        if (!stats.isEmpty()) {
            // vendor_id lấy từ products; sản phẩm đã bị xóa cứng thì SELECT rỗng → bỏ qua
            jdbc.batchUpdate("""
                    INSERT INTO product_view_stats (product_id, vendor_id, stat_date, source, view_count, unique_visitor_count,
                                                    add_to_cart_count)
                    SELECT ?, p.vendor_id, ?, ?, ?, ?, 0 FROM products p WHERE p.id = ?
                    ON CONFLICT (product_id, stat_date, source) DO UPDATE
                       SET view_count = product_view_stats.view_count + EXCLUDED.view_count,
                           unique_visitor_count = GREATEST(product_view_stats.unique_visitor_count, EXCLUDED.unique_visitor_count)""",
                    stats);
            List<Object[]> totals = perProduct.entrySet().stream().map(e -> new Object[] {e.getValue(), e.getKey()}).toList();
            jdbc.batchUpdate("UPDATE products SET view_count = view_count + ? WHERE id = ?", totals);
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redis.delete(ProductViewCounter.FLUSHING);
                }
            });
        } else {
            redis.delete(ProductViewCounter.FLUSHING);
        }
        return rows.size();
    }
}
