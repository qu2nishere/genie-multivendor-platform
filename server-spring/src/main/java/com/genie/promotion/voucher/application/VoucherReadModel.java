// Read-model SQL cho voucher (plan 2026-11-26 voucher N2, V03.01, D10, D13, D17) — promotion KHÔNG import vendor / order
package com.genie.promotion.voucher.application;

import com.genie.promotion.voucher.domain.VoucherStatus;
import com.genie.promotion.voucher.domain.VoucherStatusSql;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Đọc thẳng {@code vendor_profiles}, {@code users}, {@code products}, {@code categories}, {@code orders},
 * {@code order_discounts}, {@code voucher_usages}, {@code user_vouchers} (tiền lệ plan 2026-11-17 D6): đặt cổng ở đây
 * sẽ tạo vòng {@code vendor → promotion → … → vendor} khi chuỗi C6 cài cổng order trong promotion.
 * <p>{@code shopId} = {@code vendor_profiles.id}; {@code vendorUserId} = {@code users.id} chủ shop ({@code vouchers.vendor_id}).
 */
@Component
public class VoucherReadModel {

    private final NamedParameterJdbcTemplate jdbc;

    public VoucherReadModel(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** {@code $Voucher.shop}. */
    public record ShopRef(long id, String name) {
    }

    /** Shop theo chủ (mọi trạng thái), một truy vấn. */
    public Map<Long, ShopRef> shopRefs(Collection<Long> vendorUserIds) {
        Map<Long, ShopRef> out = new HashMap<>();
        if (vendorUserIds.isEmpty()) return out;
        jdbc.query("SELECT user_id, id, shop_name FROM vendor_profiles WHERE user_id IN (:ids)",
                new MapSqlParameterSource("ids", vendorUserIds),
                rs -> { out.put(rs.getLong("user_id"), new ShopRef(rs.getLong("id"), rs.getString("shop_name"))); });
        return out;
    }

    /** Chủ shop của {@code shop_id} nếu shop đang hiển thị được: {@code VERIFIED} + chủ {@code ACTIVE}. */
    public Optional<Long> visibleShopOwner(long shopId) {
        List<Long> r = jdbc.queryForList("""
                SELECT vp.user_id FROM vendor_profiles vp JOIN users u ON u.id = vp.user_id
                WHERE vp.id = :id AND vp.kyc_status = 'VERIFIED' AND u.status = 'ACTIVE'""",
                new MapSqlParameterSource("id", shopId), Long.class);
        return r.stream().findFirst();
    }

    /** Chủ shop nào trong tập đang hiển thị được (lọc voucher shop của shop bị khóa). */
    public Set<Long> visibleOwners(Collection<Long> vendorUserIds) {
        if (vendorUserIds.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.queryForList("""
                SELECT vp.user_id FROM vendor_profiles vp JOIN users u ON u.id = vp.user_id
                WHERE vp.user_id IN (:ids) AND vp.kyc_status = 'VERIFIED' AND u.status = 'ACTIVE'""",
                new MapSqlParameterSource("ids", vendorUserIds), Long.class));
    }

    /** Sản phẩm (chưa xóa) thuộc chủ shop trong tập đưa vào. */
    public Set<Long> productsOwnedBy(long vendorUserId, Collection<Long> productIds) {
        if (productIds.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.queryForList("""
                SELECT id FROM products WHERE id IN (:ids) AND vendor_id = :vid AND status <> 'DELETED'""",
                new MapSqlParameterSource("ids", productIds).addValue("vid", vendorUserId), Long.class));
    }

    /** Sản phẩm (chưa xóa) tồn tại — loại trừ sản phẩm ở voucher sàn. */
    public Set<Long> existingProducts(Collection<Long> productIds) {
        if (productIds.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.queryForList("SELECT id FROM products WHERE id IN (:ids) AND status <> 'DELETED'",
                new MapSqlParameterSource("ids", productIds), Long.class));
    }

    /** Ngành đang bật trong tập đưa vào. */
    public Set<Long> activeCategories(Collection<Long> categoryIds) {
        if (categoryIds.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.queryForList("SELECT id FROM categories WHERE id IN (:ids) AND is_active",
                new MapSqlParameterSource("ids", categoryIds), Long.class));
    }

    /** Tên ngành theo id (giữ thứ tự đầu vào). */
    public List<String> categoryNames(Collection<Long> categoryIds) {
        if (categoryIds.isEmpty()) return List.of();
        Map<Long, String> names = new LinkedHashMap<>();
        jdbc.query("SELECT id, name FROM categories WHERE id IN (:ids)", new MapSqlParameterSource("ids", categoryIds),
                rs -> { names.put(rs.getLong("id"), rs.getString("name")); });
        return categoryIds.stream().map(names::get).filter(java.util.Objects::nonNull).toList();
    }

    public boolean hasSavers(long voucherId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM user_vouchers WHERE voucher_id = :id)",
                new MapSqlParameterSource("id", voucherId), Boolean.class));
    }

    public boolean hasUsages(long voucherId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM voucher_usages WHERE voucher_id = :id)",
                new MapSqlParameterSource("id", voucherId), Boolean.class));
    }

    /** Gỡ voucher khỏi Kho của mọi người (xóa vĩnh viễn voucher sàn — D18). */
    public void deleteSavers(long voucherId) {
        jdbc.update("DELETE FROM user_vouchers WHERE voucher_id = :id", new MapSqlParameterSource("id", voucherId));
    }

    /** Một trang id voucher quản lý (lọc theo chủ shop hoặc voucher sàn, trạng thái) + tổng. */
    public record IdPage(List<Long> ids, long total) {
    }

    /**
     * @param vendorUserId chủ shop; {@code null} = voucher sàn (PLATFORM + FREESHIP)
     */
    public IdPage managementPage(Long vendorUserId, VoucherStatus status, OffsetDateTime now, int page, int size) {
        String where = vendorUserId != null ? "v.type = 'SHOP' AND v.vendor_id = :vid" : "v.type IN ('PLATFORM', 'FREESHIP')";
        if (status != null) where += " AND " + VoucherStatusSql.of(status);
        MapSqlParameterSource p = new MapSqlParameterSource("now", ts(now)).addValue("vid", vendorUserId)
                .addValue("limit", size).addValue("offset", (long) page * size);
        Long total = jdbc.queryForObject("SELECT count(*) FROM vouchers v WHERE " + where, p, Long.class);
        List<Long> ids = jdbc.queryForList("SELECT v.id FROM vouchers v WHERE " + where + " ORDER BY "
                + VoucherStatusSql.orderBy(status) + " LIMIT :limit OFFSET :offset", p, Long.class);
        return new IdPage(ids, total == null ? 0 : total);
    }

    /** Số voucher của chủ shop / sàn ở một trạng thái. */
    public long count(Long vendorUserId, VoucherStatus status, OffsetDateTime now) {
        String where = vendorUserId != null ? "v.type = 'SHOP' AND v.vendor_id = :vid" : "v.type IN ('PLATFORM', 'FREESHIP')";
        Long n = jdbc.queryForObject("SELECT count(*) FROM vouchers v WHERE " + where + " AND " + VoucherStatusSql.of(status),
                new MapSqlParameterSource("now", ts(now)).addValue("vid", vendorUserId), Long.class);
        return n == null ? 0 : n;
    }

    /** Thẻ thống kê Seller (D17): lượt dùng tháng này + doanh thu đơn hoàn tất có voucher của shop trong tháng. */
    public record ShopMonthStats(long usage, long revenue) {
    }

    public ShopMonthStats shopMonthStats(long vendorUserId, OffsetDateTime from, OffsetDateTime to) {
        MapSqlParameterSource p = new MapSqlParameterSource("vid", vendorUserId).addValue("from", ts(from)).addValue("to", ts(to));
        Long usage = jdbc.queryForObject("""
                SELECT count(*) FROM voucher_usages u JOIN vouchers v ON v.id = u.voucher_id
                WHERE v.type = 'SHOP' AND v.vendor_id = :vid AND u.used_at >= :from AND u.used_at < :to""", p, Long.class);
        BigDecimal revenue = jdbc.queryForObject("""
                SELECT COALESCE(SUM(o.grand_total), 0) FROM orders o
                WHERE o.status = 'COMPLETED' AND o.completed_at >= :from AND o.completed_at < :to
                  AND EXISTS (SELECT 1 FROM order_discounts d JOIN vouchers v ON v.id = d.voucher_id
                              WHERE d.order_id = o.id AND v.type = 'SHOP' AND v.vendor_id = :vid)""", p, BigDecimal.class);
        return new ShopMonthStats(usage == null ? 0 : usage, revenue == null ? 0 : revenue.longValue());
    }

    /** Thẻ thống kê Admin (D17). */
    public record PlatformStats(long totalUsage, long totalDiscountUsed, long remainingBudget) {
    }

    public PlatformStats platformStats(OffsetDateTime now) {
        MapSqlParameterSource p = new MapSqlParameterSource("now", ts(now));
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(v.used_count), 0) AS used, COALESCE(SUM(v.total_discount_used), 0) AS discount,
                       COALESCE(SUM(CASE WHEN v.budget IS NOT NULL AND (%s OR %s)
                                         THEN GREATEST(v.budget - v.total_discount_used, 0) ELSE 0 END), 0) AS remaining
                FROM vouchers v WHERE v.type IN ('PLATFORM', 'FREESHIP')""".formatted(VoucherStatusSql.ACTIVE,
                VoucherStatusSql.SCHEDULED), p, (rs, n) -> new PlatformStats(rs.getLong("used"),
                rs.getBigDecimal("discount").longValue(), rs.getBigDecimal("remaining").longValue()));
    }

    /** Lượt người dùng đã dùng từng voucher + mã đơn gần nhất (Kho — {@code USED}). */
    public record Usage(int count, String lastOrderCode) {
    }

    public Map<Long, Usage> usagesOf(long userId, Collection<Long> voucherIds) {
        Map<Long, Usage> out = new HashMap<>();
        if (voucherIds.isEmpty()) return out;
        jdbc.query("""
                SELECT u.voucher_id, count(*) AS n,
                       (SELECT o.order_code FROM voucher_usages u2 LEFT JOIN orders o ON o.id = u2.order_id
                        WHERE u2.voucher_id = u.voucher_id AND u2.user_id = :uid ORDER BY u2.used_at DESC, u2.id DESC LIMIT 1)
                           AS last_code
                FROM voucher_usages u WHERE u.user_id = :uid AND u.voucher_id IN (:ids) GROUP BY u.voucher_id""",
                new MapSqlParameterSource("uid", userId).addValue("ids", voucherIds),
                rs -> { out.put(rs.getLong("voucher_id"), new Usage(rs.getInt("n"), rs.getString("last_code"))); });
        return out;
    }

    /** Voucher nào người dùng đã lưu ({@code is_saved}). */
    public Set<Long> savedAmong(long userId, Collection<Long> voucherIds) {
        if (voucherIds.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.queryForList("SELECT voucher_id FROM user_vouchers WHERE user_id = :uid AND voucher_id IN (:ids)",
                new MapSqlParameterSource("uid", userId).addValue("ids", voucherIds), Long.class));
    }

    static Timestamp ts(OffsetDateTime t) {
        return Timestamp.from(t.toInstant());
    }
}
