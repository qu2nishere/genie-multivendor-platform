package com.genie.catalog.product.application;

import com.fasterxml.jackson.databind.JavaType;
import com.genie.catalog.stats.application.RecentCategoryTracker;
import com.genie.common.cache.JsonCache;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Gợi ý cho bạn / Gợi ý hôm nay" — {@code GET /public/recommendations?page&size} (UC-CAT-07 bước 5; API_SPEC 5.5;
 * plan 2026-11-17 thuật toán A9).
 * <ul>
 *   <li><b>Dòng chung</b> (khách vãng lai): tập nguồn = 300 sản phẩm bán chạy ∪ 300 mới nhất, xếp
 *       <b>ngẫu nhiên có seed theo ngày</b> {@code ORDER BY md5(id || yyyyMMdd)} — cùng ngày cùng thứ tự nên phân trang ổn định,
 *       sang ngày mới đổi; tối đa {@value #MAX_ITEMS} sản phẩm (8 trang × 30). Cache 30 phút theo ngày.</li>
 *   <li><b>Cá nhân hóa</b> (có token): lấy 3 danh mục xem nhiều nhất gần đây ({@link RecentCategoryTracker}); trộn
 *       <b>xen kẽ 3 : 2</b> (~60% sản phẩm của các danh mục đó, ~40% dòng chung), danh sách nào hết thì lấy tiếp danh sách kia.</li>
 * </ul>
 */
@Service
public class RecommendationService {

    public static final int MAX_ITEMS = 240;
    static final int POOL_EACH = 300;
    static final Duration TTL = Duration.ofMinutes(30);
    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    public record Page(List<ProductCard> items, long totalItems) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final ProductCardAssembler cards;
    private final RecentCategoryTracker recent;
    private final JsonCache cache;
    private final Clock clock;
    private final JavaType idsType;

    public RecommendationService(NamedParameterJdbcTemplate jdbc, ProductCardAssembler cards, RecentCategoryTracker recent,
                                 JsonCache cache, Clock clock) {
        this.jdbc = jdbc;
        this.cards = cards;
        this.recent = recent;
        this.cache = cache;
        this.clock = clock;
        this.idsType = cache.listOf(Long.class);
    }

    @Transactional(readOnly = true)
    public Page page(Long userId, int page, int size) {
        String seed = DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now(clock.withZone(VN)));
        List<Long> general = cache.getOrLoad("catalog:reco:" + seed, idsType, TTL, () -> generalFeed(seed));
        List<Long> ordered = general;
        if (userId != null) {
            List<Long> topCats = recent.topCategories(userId, 3);
            if (!topCats.isEmpty()) {
                ordered = interleave(categoryFeed(topCats, seed), general);
            }
        }
        int from = (page - 1) * size;
        List<Long> slice = from >= ordered.size() ? List.of() : ordered.subList(from, Math.min(ordered.size(), from + size));
        return new Page(cards.load(slice), ordered.size());
    }

    /** Xen kẽ 3 phần tử {@code primary} : 2 phần tử {@code secondary}, bỏ trùng, tối đa {@value #MAX_ITEMS}. */
    static List<Long> interleave(List<Long> primary, List<Long> secondary) {
        List<Long> out = new ArrayList<>(MAX_ITEMS);
        Set<Long> seen = new HashSet<>();
        int i = 0;
        int j = 0;
        int slot = 0;
        while (out.size() < MAX_ITEMS && (i < primary.size() || j < secondary.size())) {
            boolean takePrimary = (slot % 5 < 3 && i < primary.size()) || j >= secondary.size();
            Long next = takePrimary ? primary.get(i++) : secondary.get(j++);
            if (seen.add(next)) {
                out.add(next);
            }
            slot++;
        }
        return out;
    }

    private static final String VISIBLE = """
             FROM products p
             JOIN users u ON u.id = p.vendor_id
             JOIN vendor_profiles vp ON vp.user_id = p.vendor_id
            WHERE p.status = 'PUBLISHED' AND u.status = 'ACTIVE' AND vp.kyc_status = 'VERIFIED'
              AND NOT (vp.vacation_starts_at IS NOT NULL AND vp.vacation_starts_at <= :now
                       AND (vp.vacation_ends_at IS NULL OR vp.vacation_ends_at > :now))
            """;

    private List<Long> generalFeed(String seed) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("seed", seed).addValue("each", POOL_EACH)
                .addValue("max", MAX_ITEMS).addValue("now", Timestamp.from(clock.instant()));
        return jdbc.queryForList("""
                WITH pool AS (
                    (SELECT p.id""" + VISIBLE + " ORDER BY p.sold DESC NULLS LAST, p.id DESC LIMIT :each)\n"
                + "    UNION\n    (SELECT p.id" + VISIBLE + " ORDER BY p.created_at DESC, p.id DESC LIMIT :each))\n"
                + "SELECT id FROM pool ORDER BY md5(id::text || :seed), id LIMIT :max", p, Long.class);
    }

    private List<Long> categoryFeed(List<Long> categoryIds, String seed) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("seed", seed).addValue("cats", categoryIds)
                .addValue("max", MAX_ITEMS).addValue("now", Timestamp.from(clock.instant()));
        return jdbc.queryForList("SELECT p.id" + VISIBLE + """
                  AND p.category_id IN (:cats)
                ORDER BY md5(p.id::text || :seed), p.id LIMIT :max""", p, Long.class);
    }
}
