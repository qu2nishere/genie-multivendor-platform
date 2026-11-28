// Đọc / ghi 4 bảng phạm vi voucher (plan 2026-11-26 voucher D10, V02.05) — sửa = thay cả tập trong cùng transaction
package com.genie.promotion.voucher.application;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class VoucherScopes {

    /** Bốn tập phạm vi của một voucher. */
    public record Sets(Set<Long> categoryIds, Set<Long> productIds, Set<Long> excludedCategoryIds,
                       Set<Long> excludedProductIds) {

        static final Sets EMPTY = new Sets(Set.of(), Set.of(), Set.of(), Set.of());

        public static Sets of(VoucherDraft d) {
            return new Sets(d.categoryIds(), d.productIds(), d.excludedCategoryIds(), d.excludedProductIds());
        }
    }

    private enum Table {
        CATEGORIES("voucher_categories", "category_id"),
        PRODUCTS("voucher_products", "product_id"),
        EXCLUDED_CATEGORIES("voucher_excluded_categories", "category_id"),
        EXCLUDED_PRODUCTS("voucher_excluded_products", "product_id");

        final String table;
        final String column;

        Table(String table, String column) {
            this.table = table;
            this.column = column;
        }
    }

    private final NamedParameterJdbcTemplate jdbc;

    public VoucherScopes(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void replace(long voucherId, Sets sets) {
        write(voucherId, Table.CATEGORIES, sets.categoryIds());
        write(voucherId, Table.PRODUCTS, sets.productIds());
        write(voucherId, Table.EXCLUDED_CATEGORIES, sets.excludedCategoryIds());
        write(voucherId, Table.EXCLUDED_PRODUCTS, sets.excludedProductIds());
    }

    public void deleteAll(long voucherId) {
        replace(voucherId, Sets.EMPTY);
    }

    public Sets of(long voucherId) {
        return ofMany(List.of(voucherId)).getOrDefault(voucherId, Sets.EMPTY);
    }

    /** Bốn tập của nhiều voucher — 4 truy vấn cho cả trang. */
    public Map<Long, Sets> ofMany(Collection<Long> voucherIds) {
        Map<Long, Sets> out = new HashMap<>();
        if (voucherIds.isEmpty()) return out;
        Map<Table, Map<Long, Set<Long>>> byTable = new HashMap<>();
        for (Table t : Table.values()) {
            Map<Long, Set<Long>> m = new HashMap<>();
            jdbc.query("SELECT voucher_id, " + t.column + " FROM " + t.table + " WHERE voucher_id IN (:ids) ORDER BY "
                    + t.column, new MapSqlParameterSource("ids", voucherIds),
                    rs -> { m.computeIfAbsent(rs.getLong(1), k -> new LinkedHashSet<>()).add(rs.getLong(2)); });
            byTable.put(t, m);
        }
        for (Long id : voucherIds) {
            out.put(id, new Sets(get(byTable, Table.CATEGORIES, id), get(byTable, Table.PRODUCTS, id),
                    get(byTable, Table.EXCLUDED_CATEGORIES, id), get(byTable, Table.EXCLUDED_PRODUCTS, id)));
        }
        return out;
    }

    private static Set<Long> get(Map<Table, Map<Long, Set<Long>>> byTable, Table t, Long id) {
        return byTable.get(t).getOrDefault(id, Set.of());
    }

    private void write(long voucherId, Table t, Set<Long> ids) {
        jdbc.update("DELETE FROM " + t.table + " WHERE voucher_id = :vid", new MapSqlParameterSource("vid", voucherId));
        if (ids.isEmpty()) return;
        MapSqlParameterSource[] batch = ids.stream()
                .map(id -> new MapSqlParameterSource("vid", voucherId).addValue("ref", id))
                .toArray(MapSqlParameterSource[]::new);
        jdbc.batchUpdate("INSERT INTO " + t.table + " (voucher_id, " + t.column + ", created_at) VALUES (:vid, :ref, now())", batch);
    }
}
