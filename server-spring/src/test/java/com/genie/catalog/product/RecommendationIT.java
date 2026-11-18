package com.genie.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.genie.auth.profile.domain.User;
import com.genie.catalog.CatalogITSupport;
import com.genie.catalog.stats.application.RecentCategoryTracker;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

/** {@code GET /public/recommendations} (UC-CAT-07 bước 5; API_SPEC 5.5; plan 2026-11-17 C05.04). */
class RecommendationIT extends CatalogITSupport {

    static final String URL = "/api/v1/public/recommendations";

    @Autowired
    RecentCategoryTracker recent;

    @Test
    void sameDayGivesSameStablePagesAndNextDayReshuffles() throws Exception {
        var shop = data.shop().create();
        for (int i = 0; i < 12; i++) {
            data.product(shop).sold(i).sku(1000, 1).create();
        }
        MvcResult p1 = call(URL, "page", "1", "size", "5");
        assertThat(status(p1)).isEqualTo(200);
        List<Long> first = ids(p1);
        List<Long> again = ids(call(URL, "page", "1", "size", "5"));
        List<Long> second = ids(call(URL, "page", "2", "size", "5"));
        assertThat(first).hasSize(5).isEqualTo(again);
        assertThat(second).doesNotContainAnyElementsOf(first);
        assertThat(((Number) read(p1, "$.pagination.total_items")).intValue()).isBetween(12, 240);
        assertThat(p1.getResponse().getHeader("Cache-Control")).contains("public");

        // Sang ngày khác → thứ tự đổi (xác suất trùng nguyên trang đầu với ≥ 12 sản phẩm là không đáng kể)
        List<Long> fullToday = ids(call(URL, "page", "1", "size", "60"));
        clock.advance(Duration.ofDays(1));
        List<Long> fullTomorrow = ids(call(URL, "page", "1", "size", "60"));
        assertThat(fullTomorrow).isNotEqualTo(fullToday);   // tập nguồn dùng chung CSDL test nên chỉ so thứ tự
    }

    @Test
    void signedInBuyerGetsRecentlyViewedCategoryFirst() throws Exception {
        long cat = data.category("Đồ gia dụng", null);
        var shop = data.shop().create();
        List<Long> inCat = List.of(
                data.product(shop).category(cat).sku(1000, 1).create(),
                data.product(shop).category(cat).sku(1000, 1).create(),
                data.product(shop).category(cat).sku(1000, 1).create());
        String email = uniqueEmail();
        User user = createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));
        recent.viewed(user.getId(), cat);

        MvcResult r = mvc.perform(get(URL).param("size", "3").header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn();
        assertThat(ids(r)).containsExactlyInAnyOrderElementsOf(inCat);
        assertThat(r.getResponse().getHeader("Cache-Control")).contains("private");
    }

    @Test
    void invalidPagingGives422() throws Exception {
        assertThat(status(call(URL, "size", "61"))).isEqualTo(422);
        assertThat(status(call(URL, "page", "0"))).isEqualTo(422);
    }
}
