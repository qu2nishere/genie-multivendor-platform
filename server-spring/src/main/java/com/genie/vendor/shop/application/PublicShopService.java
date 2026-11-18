package com.genie.vendor.shop.application;

import com.fasterxml.jackson.databind.JavaType;
import com.genie.common.cache.JsonCache;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.storage.ObjectStorage;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trang gian hàng công khai (UC-CAT-16; API_SPEC 5.11, 5.12; plan 2026-11-17 A12, D7).
 * <ul>
 *   <li>Chỉ shop {@code kyc_status = VERIFIED} của chủ {@code ACTIVE}; ngược lại {@code 404 GNE-VND-4041};</li>
 *   <li>chỉ số uy tín đọc cột tổng hợp của {@code vendor_profiles} (do sự kiện / job khác cập nhật — không tính mỗi lần mở);
 *       riêng {@code product_count} đếm sản phẩm {@code PUBLISHED} (chưa có sự kiện nào cập nhật {@code active_product_count});</li>
 *   <li>{@code categories} = danh mục gốc (cấp 1) của các sản phẩm đang bán — tab danh mục của shop;</li>
 *   <li>cache Redis 10 phút; tạm nghỉ tính lúc trả.</li>
 * </ul>
 * Lưới sản phẩm & "Tìm trong Shop" dùng {@code GET /public/products?shop_id=} của module catalog.
 * Đọc thẳng bảng {@code products}, {@code categories} bằng SQL thuần (read-model, quyết định D6) — không import class catalog.
 */
@Service
public class PublicShopService {

    static final Duration TTL = Duration.ofMinutes(10);
    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final String AVATAR_BUCKET = "avatar-assets";

    public record CategoryRef(long id, String name) {
    }

    public record Vacation(LocalDate until, String message) {
    }

    /** {@code GET /public/shops/{id}}. */
    public record ShopPage(long id, String name, String logoUrl, String coverUrl, String description, boolean isMall,
                           boolean isFavorite, double ratingAvg, long followerCount, long productCount,
                           Double chatResponseRate, Integer avgResponseMinutes, LocalDate joinedAt, boolean isOnline,
                           Vacation vacation, List<CategoryRef> categories) {
    }

    /** {@code GET /public/shops?featured=true} — shop đã xác minh ở trang chủ. */
    public record FeaturedShop(long id, String name, String logoUrl, double ratingAvg, long followerCount, String provinceName) {
    }

    /** Bản cache: mốc tạm nghỉ thô để tính lúc trả. */
    public record Cached(ShopPage page, OffsetDateTime vacationStartsAt, OffsetDateTime vacationEndsAt, String vacationMessage) {
    }

    private final JdbcTemplate jdbc;
    private final JsonCache cache;
    private final ObjectStorage storage;
    private final Clock clock;
    private final JavaType cachedType;

    public PublicShopService(JdbcTemplate jdbc, JsonCache cache, ObjectStorage storage, Clock clock) {
        this.jdbc = jdbc;
        this.cache = cache;
        this.storage = storage;
        this.clock = clock;
        this.cachedType = cache.type(Cached.class);
    }

    static String cacheKey(long shopId) {
        return "vendor:shop:" + shopId;
    }

    /** Gọi khi shop đổi thông tin / bị khóa (plan Mở shop & KYC — UC-VND-21). */
    public void evict(long shopId) {
        cache.evict(cacheKey(shopId));
    }

    @Transactional(readOnly = true)
    public ShopPage shop(long shopId) {
        Cached c = cache.getOrLoad(cacheKey(shopId), cachedType, TTL, () -> load(shopId));
        if (c == null) {
            throw new BusinessException(ErrorCode.SHOP_UNAVAILABLE);
        }
        ShopPage p = c.page();
        return new ShopPage(p.id(), p.name(), p.logoUrl(), p.coverUrl(), p.description(), p.isMall(), p.isFavorite(),
                p.ratingAvg(), p.followerCount(), p.productCount(), p.chatResponseRate(), p.avgResponseMinutes(),
                p.joinedAt(), false, vacation(c), p.categories());
    }

    @Transactional(readOnly = true)
    public List<FeaturedShop> featured(int limit) {
        return jdbc.query("""
                SELECT vp.id, vp.shop_name, vp.logo_url, vp.rating_avg, vp.follower_count, vp.pickup_province
                  FROM vendor_profiles vp JOIN users u ON u.id = vp.user_id
                 WHERE vp.kyc_status = 'VERIFIED' AND u.status = 'ACTIVE'
                 ORDER BY vp.is_mall DESC, vp.rating_avg DESC, vp.follower_count DESC, vp.id
                 LIMIT ?""", (rs, i) -> new FeaturedShop(rs.getLong("id"), rs.getString("shop_name"),
                url(rs.getString("logo_url")), rs.getDouble("rating_avg"), rs.getLong("follower_count"),
                rs.getString("pickup_province")), limit);
    }

    private Vacation vacation(Cached c) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (c.vacationStartsAt() == null || c.vacationStartsAt().isAfter(now)
                || (c.vacationEndsAt() != null && !c.vacationEndsAt().isAfter(now))) {
            return null;
        }
        return new Vacation(c.vacationEndsAt() == null ? null : c.vacationEndsAt().atZoneSameInstant(VN).toLocalDate(),
                c.vacationMessage());
    }

    private Cached load(long shopId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT vp.id, vp.user_id, vp.shop_name, vp.logo_url, vp.cover_url, vp.shop_description, vp.is_mall,
                       vp.is_favorite, vp.rating_avg, vp.follower_count, vp.chat_response_rate, vp.avg_response_minutes,
                       vp.created_at, vp.vacation_starts_at, vp.vacation_ends_at, vp.vacation_message
                  FROM vendor_profiles vp JOIN users u ON u.id = vp.user_id
                 WHERE vp.id = ? AND vp.kyc_status = 'VERIFIED' AND u.status = 'ACTIVE'""", shopId);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> r = rows.get(0);
        long vendorId = ((Number) r.get("user_id")).longValue();
        long productCount = jdbc.queryForObject(
                "SELECT count(*) FROM products WHERE vendor_id = ? AND status = 'PUBLISHED'", Long.class, vendorId);
        List<CategoryRef> categories = jdbc.query("""
                WITH RECURSIVE up AS (
                    SELECT c.id, c.name, c.parent_id, 0 AS depth FROM categories c
                     WHERE c.id IN (SELECT DISTINCT category_id FROM products WHERE vendor_id = ? AND status = 'PUBLISHED')
                    UNION ALL
                    SELECT p.id, p.name, p.parent_id, up.depth + 1 FROM categories p JOIN up ON p.id = up.parent_id
                     WHERE up.depth < 10)
                SELECT DISTINCT c.id, c.name, c.display_order FROM up JOIN categories c ON c.id = up.id
                 WHERE up.parent_id IS NULL AND c.is_active = TRUE
                 ORDER BY c.display_order NULLS LAST, c.id""",
                (rs, i) -> new CategoryRef(rs.getLong("id"), rs.getString("name")), vendorId);
        OffsetDateTime created = toOffset(r.get("created_at"));
        BigDecimal rate = (BigDecimal) r.get("chat_response_rate");
        ShopPage page = new ShopPage(shopId, (String) r.get("shop_name"), url((String) r.get("logo_url")),
                url((String) r.get("cover_url")), (String) r.get("shop_description"), Boolean.TRUE.equals(r.get("is_mall")),
                Boolean.TRUE.equals(r.get("is_favorite")), ((Number) r.get("rating_avg")).doubleValue(),
                ((Number) r.get("follower_count")).longValue(), productCount, rate == null ? null : rate.doubleValue(),
                r.get("avg_response_minutes") == null ? null : ((Number) r.get("avg_response_minutes")).intValue(),
                created == null ? null : created.atZoneSameInstant(VN).toLocalDate(), false, null, categories);
        return new Cached(page, toOffset(r.get("vacation_starts_at")), toOffset(r.get("vacation_ends_at")),
                (String) r.get("vacation_message"));
    }

    private String url(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        return stored.startsWith("http://") || stored.startsWith("https://") ? stored : storage.publicUrl(AVATAR_BUCKET, stored);
    }

    private static OffsetDateTime toOffset(Object v) {
        if (v instanceof Timestamp t) {
            return t.toInstant().atOffset(ZoneOffset.UTC);
        }
        return v instanceof OffsetDateTime o ? o : null;
    }
}
