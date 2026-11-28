// Cài cổng ChatOrderLookup của module social (plan 2026-11-26 chat M02.05, N2, D9; UC-SOC-02; API_SPEC 8.6)
package com.genie.order.order.application;

import com.genie.catalog.product.application.VariantFacade;
import com.genie.social.chat.application.ChatOrderLookup;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Read-model đơn cho thẻ đơn hàng / ô "Gửi đơn" trong chat — <b>file mới</b>, không sửa {@code OrderFacade} (C2). Đọc
 * {@code orders} + số dòng + biến thể dòng đầu; ảnh dòng đầu lấy qua {@link VariantFacade#lines} (cùng luật ảnh với giỏ).
 */
@Component
public class ChatOrderLookupImpl implements ChatOrderLookup {

    private static final String SELECT = """
            SELECT o.id, o.order_code, o.status, o.customer_id, o.vendor_id, o.grand_total, o.created_at,
                   (SELECT COUNT(*) FROM order_items i WHERE i.order_id = o.id) AS item_count,
                   (SELECT i.product_variant_id FROM order_items i WHERE i.order_id = o.id ORDER BY i.id LIMIT 1)
                       AS first_variant
            FROM orders o
            """;

    private final JdbcTemplate jdbc;
    private final VariantFacade variants;

    public ChatOrderLookupImpl(JdbcTemplate jdbc, VariantFacade variants) {
        this.jdbc = jdbc;
        this.variants = variants;
    }

    private record Row(long id, String code, String status, long customerId, long vendorUserId, long total,
                       OffsetDateTime createdAt, int itemCount, Long firstVariant) {
    }

    @Override
    public Map<Long, OrderCard> cards(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        Long[] arr = orderIds.stream().filter(Objects::nonNull).distinct().toArray(Long[]::new);
        List<Row> rows = jdbc.query(SELECT + "WHERE o.id = ANY (CAST(? AS bigint[]))", ChatOrderLookupImpl::row,
                VariantFacade.pgArray(arr));
        Map<Long, OrderCard> out = new LinkedHashMap<>();
        for (OrderCard c : toCards(rows)) {
            out.put(c.id(), c);
        }
        return out;
    }

    @Override
    public List<OrderCard> recentBetween(long customerId, long vendorUserId, int limit) {
        List<Row> rows = jdbc.query(SELECT + """
                WHERE o.customer_id = ? AND o.vendor_id = ?
                ORDER BY o.created_at DESC, o.id DESC LIMIT ?""", ChatOrderLookupImpl::row,
                customerId, vendorUserId, Math.max(1, Math.min(limit, 20)));
        return toCards(rows);
    }

    private List<OrderCard> toCards(List<Row> rows) {
        var images = variants.lines(rows.stream().map(Row::firstVariant).filter(Objects::nonNull).toList());
        List<OrderCard> out = new ArrayList<>(rows.size());
        for (Row r : rows) {
            var line = r.firstVariant() == null ? null : images.get(r.firstVariant());
            out.add(new OrderCard(r.id(), r.code(), r.status(), r.customerId(), r.vendorUserId(), r.total(),
                    r.createdAt(), r.itemCount(), line == null ? null : line.imageUrl()));
        }
        return out;
    }

    private static Row row(ResultSet rs, int n) throws SQLException {
        long first = rs.getLong("first_variant");
        Long firstVariant = rs.wasNull() ? null : first;
        return new Row(rs.getLong("id"), rs.getString("order_code"), rs.getString("status"), rs.getLong("customer_id"),
                rs.getLong("vendor_id"), rs.getBigDecimal("grand_total").longValue(),
                rs.getObject("created_at", OffsetDateTime.class), rs.getInt("item_count"), firstVariant);
    }
}
