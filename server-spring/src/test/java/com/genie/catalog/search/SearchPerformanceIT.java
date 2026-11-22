package com.genie.catalog.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.catalog.CatalogITSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Hiệu năng tìm kiếm trên 10.000 sản phẩm (plan 2026-11-17 C02.10, mục tiêu p95 &lt; 300 ms cho tìm kiếm + facet).
 * Chỉ chạy khi bật: {@code ./mvnw verify -Dit.test=SearchPerformanceIT -Dperf=true} — không chạy trong CI vì chèn
 * 10.000 dòng vào CSDL test dùng chung. In p50 / p95 / max ra log và kế hoạch truy vấn để dán vào Ghi chú progress.
 */
@Tag("perf")
@EnabledIfSystemProperty(named = "perf", matches = "true")
class SearchPerformanceIT extends CatalogITSupport {

    static final String URL = "/api/v1/public/products";
    static final String[] WORDS = {"tai nghe", "áo thun", "bàn phím", "serum", "giày", "điện thoại", "nồi chiên", "ốp lưng",
            "đồng hồ", "balo", "son môi", "sạc dự phòng", "loa", "chuột", "kem chống nắng", "váy", "quần jean", "mũ", "gối", "bình"};

    @Test
    void searchWithFacetsStaysUnder300msAtP95() throws Exception {
        long cat = data.category("Hiệu năng", null);
        long brand = data.brand("PerfBrand", true);
        List<Long> vendors = new ArrayList<>();
        for (int s = 0; s < 20; s++) {
            vendors.add(data.shop().province(200 + (s % 5), "Tỉnh " + (s % 5)).create().userId());
        }
        String words = "ARRAY['" + String.join("','", WORDS) + "']";
        jdbc.update("""
                INSERT INTO products (vendor_id, category_id, name, description, brand, brand_id, base_price, status, is_published,
                    version, created_at, updated_at, sold, rating, item_condition, is_pre_order, view_count)
                SELECT (?::bigint[])[1 + g % 20], ?, (WORDS_ARRAY)[1 + g % 20] || ' mẫu ' || g, 'Mô tả sản phẩm số ' || g, 'PerfBrand', ?,
                       100000 + (g % 50) * 10000, 'PUBLISHED', TRUE, 0, now() - (g || ' minutes')::interval, now(),
                       g % 500, 3 + (g % 20) / 10.0, 'NEW', FALSE, 0
                  FROM generate_series(1, 10000) g""".replace("WORDS_ARRAY", words),
                vendors.toArray(new Long[0]), cat, brand);
        jdbc.update("""
                INSERT INTO product_variants (product_id, sku, price, stock_count, reserved_count, version)
                SELECT p.id, 'PERF-' || p.id, p.base_price, 10, 0, 0 FROM products p WHERE p.category_id = ?""", cat);
        // Thống kê mới cho planner (production do autovacuum lo) — thiếu users / vendor_profiles thì planner ước 1 dòng
        // và chọn vòng lặp lồng chạy lại cả bảng products (đo được ~500 ms ở facet danh mục)
        jdbc.execute("ANALYZE products; ANALYZE product_variants; ANALYZE users; ANALYZE vendor_profiles; "
                + "ANALYZE vendor_shipping_config; ANALYZE brands; ANALYZE categories");

        String catId = String.valueOf(cat);
        String[][] queries = {
                {"q", "tai nghe", "include_facets", "true"},
                {"q", "ao thun", "category_id", catId, "include_facets", "true", "sort", "PRICE_ASC"},
                {"q", "ban phim", "price_min", "150000", "price_max", "400000", "include_facets", "true"},
                {"q", "dien thoai", "rating_min", "4", "sort", "BEST_SELLING"},
                {"category_id", catId, "include_facets", "true", "page", "20"},
                {"q", "tai nge", "include_facets", "true"},     // gõ sai → tầng trigram
        };
        List<Long> timings = new ArrayList<>();
        long[][] perQuery = new long[queries.length][6];
        for (int round = 0; round < 6; round++) {
            for (int qi = 0; qi < queries.length; qi++) {
                String[] q = queries[qi];
                redis.getConnectionFactory().getConnection().serverCommands().flushAll();   // bỏ rate limit giữa các lượt đo
                long t0 = System.nanoTime();
                assertThat(status(call(URL, q))).isEqualTo(200);
                long ms = (System.nanoTime() - t0) / 1_000_000;
                timings.add(ms);
                perQuery[qi][round] = ms;
            }
        }
        for (int qi = 0; qi < queries.length; qi++) {
            System.out.printf("SearchPerformanceIT query %d %s: %s%n", qi, Arrays.toString(queries[qi]), Arrays.toString(perQuery[qi]));
        }
        long[] sorted = timings.stream().mapToLong(Long::longValue).sorted().toArray();
        long p50 = sorted[sorted.length / 2];
        long p95 = sorted[(int) Math.ceil(sorted.length * 0.95) - 1];
        System.out.printf("SearchPerformanceIT: n=%d p50=%dms p95=%dms max=%dms all=%s%n", sorted.length, p50, p95,
                sorted[sorted.length - 1], Arrays.toString(sorted));
        // 10.000 dòng thì planner có thể chọn quét tuần tự (rẻ hơn); tắt seq scan để chứng minh index DÙNG ĐƯỢC
        List<String> plan = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<List<String>>) con -> {
            List<String> lines = new ArrayList<>();
            try (var st = con.createStatement()) {
                st.execute("SET enable_seqscan = off");
                try (var rs = st.executeQuery("""
                        EXPLAIN (ANALYZE, BUFFERS) SELECT p.id FROM products p
                         WHERE p.search_vector @@ to_tsquery('simple', 'tai & nghe:*')
                         ORDER BY ts_rank_cd(p.search_vector, to_tsquery('simple', 'tai & nghe:*')) DESC LIMIT 20""")) {
                    while (rs.next()) {
                        lines.add(rs.getString(1));
                    }
                }
                try (var rs = st.executeQuery("""
                        EXPLAIN SELECT p.id FROM products p
                         WHERE 'tai nge' <% immutable_unaccent(lower(p.name))""")) {
                    while (rs.next()) {
                        lines.add(rs.getString(1));
                    }
                }
                st.execute("RESET enable_seqscan");
            }
            return lines;
        });
        System.out.println("SearchPerformanceIT plan:\n" + String.join("\n", plan));

        assertThat(String.join("\n", plan)).contains("idx_products_search_vector", "idx_products_name_unaccent_trgm");
        assertThat(p95).isLessThan(300);
    }
}
