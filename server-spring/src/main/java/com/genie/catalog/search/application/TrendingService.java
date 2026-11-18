package com.genie.catalog.search.application;

import com.fasterxml.jackson.databind.JavaType;
import com.genie.common.cache.JsonCache;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.zset.Aggregate;
import org.springframework.data.redis.connection.zset.Weights;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Từ khóa xu hướng — {@code GET /public/search/trending} (UC-CAT-04 bước 1; API_SPEC 5.4b; plan 2026-11-17 A6, D5).
 * <p><b>Thuật toán:</b> top-K theo tần suất trong cửa sổ trượt ~24 giờ bằng Redis Sorted Set:
 * {@code ZUNION search:kw:{hôm nay} search:kw:{hôm qua} WEIGHTS 1 0.5} — hôm qua tính nửa trọng số để từ khóa cũ nhạt dần.
 * Bỏ từ bị Staff chặn ({@code search_keyword_stats.is_blocked}). Kết quả cache 10 phút.
 * Redis trống / lỗi → dự phòng SQL cộng {@code search_count} hai ngày gần nhất.
 */
@Service
public class TrendingService {

    static final Duration TTL = Duration.ofMinutes(10);
    static final double YESTERDAY_WEIGHT = 0.5;

    private static final Logger log = LoggerFactory.getLogger(TrendingService.class);

    public record TrendingKeyword(String keyword) {
    }

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbc;
    private final JsonCache cache;
    private final Clock clock;
    private final JavaType listType;

    public TrendingService(StringRedisTemplate redis, JdbcTemplate jdbc, JsonCache cache, Clock clock) {
        this.redis = redis;
        this.jdbc = jdbc;
        this.cache = cache;
        this.clock = clock;
        this.listType = cache.listOf(TrendingKeyword.class);
    }

    public List<TrendingKeyword> top(int limit) {
        return cache.getOrLoad("catalog:trending:" + limit, listType, TTL, () -> compute(limit));
    }

    List<TrendingKeyword> compute(int limit) {
        Set<String> blocked = new HashSet<>(jdbc.queryForList(
                "SELECT DISTINCT normalized_keyword FROM search_keyword_stats WHERE is_blocked = TRUE", String.class));
        List<String> ranked = fromRedis(limit * 3);
        if (ranked.isEmpty()) {
            ranked = fromDatabase(limit * 3);
        }
        List<TrendingKeyword> out = new ArrayList<>(limit);
        for (String kw : ranked) {
            if (!blocked.contains(kw)) {
                out.add(new TrendingKeyword(kw));
                if (out.size() == limit) {
                    break;
                }
            }
        }
        return out;
    }

    private List<String> fromRedis(int n) {
        try {
            LocalDate today = SearchKeys.today(clock);
            Set<ZSetOperations.TypedTuple<String>> union = redis.opsForZSet().unionWithScores(
                    SearchKeys.daily(today), List.of(SearchKeys.daily(today.minusDays(1))), Aggregate.SUM,
                    Weights.of(1, YESTERDAY_WEIGHT));
            if (union == null || union.isEmpty()) {
                return List.of();
            }
            return union.stream()
                    .filter(t -> t.getValue() != null && t.getScore() != null)
                    .sorted(Comparator.comparingDouble((ZSetOperations.TypedTuple<String> t) -> t.getScore()).reversed()
                            .thenComparing(ZSetOperations.TypedTuple::getValue))
                    .limit(n)
                    .map(ZSetOperations.TypedTuple::getValue)
                    .toList();
        } catch (RuntimeException e) {
            log.warn("Xu hướng đọc từ DB vì Redis lỗi: {}", e.getMessage());
            return List.of();
        }
    }

    private List<String> fromDatabase(int n) {
        LocalDate today = SearchKeys.today(clock);
        Map<String, Double> score = new LinkedHashMap<>();
        jdbc.query("""
                SELECT normalized_keyword, stat_date, search_count FROM search_keyword_stats
                 WHERE stat_date >= ? AND is_blocked = FALSE""", rs -> {
            double w = rs.getObject("stat_date", LocalDate.class).isBefore(today) ? YESTERDAY_WEIGHT : 1.0;
            score.merge(rs.getString("normalized_keyword"), w * rs.getInt("search_count"), Double::sum);
        }, today.minusDays(1));
        return score.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(n).map(Map.Entry::getKey).toList();
    }
}
