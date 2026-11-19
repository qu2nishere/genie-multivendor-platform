package com.genie.catalog.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.genie.catalog.CatalogITSupport;
import com.genie.catalog.search.application.SearchKeys;
import com.genie.catalog.search.application.SearchKeywordFlushJob;
import com.genie.support.TestDataBuilder;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/** Đếm từ khóa, xu hướng & job ghi gộp (UC-CAT-04; API_SPEC 5.4b; plan 2026-11-17 C03.06). */
class TrendingIT extends CatalogITSupport {

    static final String SEARCH = "/api/v1/public/products";
    static final String TRENDING = "/api/v1/public/search/trending";

    @Autowired
    SearchKeywordFlushJob flushJob;

    private void searchFrom(String ip, String q) throws Exception {
        mvc.perform(get(SEARCH).param("q", q).with(req -> {
            req.setRemoteAddr(ip);
            return req;
        })).andReturn();
    }

    private List<String> trending(int limit) throws Exception {
        MvcResult r = call(TRENDING, "limit", String.valueOf(limit));
        assertThat(status(r)).isEqualTo(200);
        return read(r, "$.data[*].keyword");
    }

    /** Ngày (giờ VN) theo đồng hồ test. */
    private LocalDate today() {
        return SearchKeys.today(clock);
    }

    @Test
    void countsOncePerIpWithinTenMinutesAndRanksByCount() throws Exception {
        String a = TestDataBuilder.uniqueWord();
        String b = TestDataBuilder.uniqueWord();
        var shop = data.shop().create();
        data.product(shop).name("Áo " + a).sku(1000, 1).create();
        data.product(shop).name("Quần " + b).sku(1000, 1).create();

        searchFrom("10.0.0.1", "Áo " + a);
        searchFrom("10.0.0.1", "áo  " + a.toUpperCase());     // cùng IP, cùng từ đã chuẩn hóa → không đếm thêm
        searchFrom("10.0.0.2", "Áo " + a);
        searchFrom("10.0.0.3", "quần " + b);

        Double score = redis.opsForZSet().score(SearchKeys.daily(today()), "áo " + a);
        assertThat(score).isEqualTo(2.0);
        List<String> top = trending(10);
        assertThat(top).contains("áo " + a, "quần " + b);
        assertThat(top.indexOf("áo " + a)).isLessThan(top.indexOf("quần " + b));
    }

    @Test
    void searchesWithoutResultsAreNotCounted() throws Exception {
        String w = TestDataBuilder.uniqueWord() + TestDataBuilder.uniqueWord();
        searchFrom("10.0.1.1", w);
        assertThat(redis.opsForZSet().score(SearchKeys.daily(today()), w)).isNull();
    }

    @Test
    void blockedKeywordsAreHidden() throws Exception {
        String w = TestDataBuilder.uniqueWord();
        var shop = data.shop().create();
        data.product(shop).name("Hàng " + w).sku(1000, 1).create();
        searchFrom("10.0.2.1", "hàng " + w);
        jdbc.update("INSERT INTO search_keyword_stats (normalized_keyword, stat_date, search_count, result_click_count, is_blocked) "
                + "VALUES (?, ?, 0, 0, TRUE)", "hàng " + w, today().minusDays(30));

        assertThat(trending(10)).doesNotContain("hàng " + w);
    }

    @Test
    void flushJobAccumulatesIntoDatabaseAndTrendingFallsBackToIt() throws Exception {
        String w = TestDataBuilder.uniqueWord();
        var shop = data.shop().create();
        data.product(shop).name("Giày " + w).sku(1000, 1).create();
        searchFrom("10.0.3.1", "giày " + w);
        searchFrom("10.0.3.2", "giày " + w);
        assertThat(flushJob.flush()).isGreaterThanOrEqualTo(1);
        searchFrom("10.0.3.3", "giày " + w);
        flushJob.flush();
        assertThat(flushJob.flush()).isZero();      // không còn gì chờ ghi

        Integer count = jdbc.queryForObject(
                "SELECT search_count FROM search_keyword_stats WHERE normalized_keyword = ? AND stat_date = ?",
                Integer.class, "giày " + w, today());
        assertThat(count).isEqualTo(3);

        // Redis mất dữ liệu (khởi động lại) → xu hướng đọc từ DB
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
        assertThat(trending(10)).contains("giày " + w);
    }

    @Test
    void invalidLimitGives422() throws Exception {
        assertThat(status(call(TRENDING, "limit", "0"))).isEqualTo(422);
        assertThat(status(call(TRENDING, "limit", "11"))).isEqualTo(422);
    }
}
