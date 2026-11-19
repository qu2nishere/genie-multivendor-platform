package com.genie.catalog.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.genie.auth.profile.domain.User;
import com.genie.catalog.CatalogITSupport;
import com.genie.catalog.stats.application.ProductViewFlushJob;
import com.genie.catalog.stats.application.RecentCategoryTracker;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** {@code POST /public/products/{id}/views} + job ghi gộp (UC-CAT-06 bước 9, UC-CAT-18; API_SPEC 5.7; plan 2026-11-17 C04.06). */
class ProductViewIT extends CatalogITSupport {

    @Autowired
    ProductViewFlushJob flushJob;

    @Autowired
    RecentCategoryTracker recent;

    private MvcResult view(long id, String source, String ip, String token) throws Exception {
        MockHttpServletRequestBuilder req = post("/api/v1/public/products/" + id + "/views")
                .contentType(MediaType.APPLICATION_JSON).content("{\"source\":\"" + source + "\"}")
                .header("User-Agent", "JUnit").with(r -> {
                    r.setRemoteAddr(ip);
                    return r;
                });
        if (token != null) {
            req.header(HttpHeaders.AUTHORIZATION, bearer(token));
        }
        return mvc.perform(req).andReturn();
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    @Test
    void countsBySourceDedupesAndFlushesToStatsAndProduct() throws Exception {
        var shop = data.shop().create();
        long id = data.product(shop).sku(1000, 1).create();

        assertThat(status(view(id, "SEARCH", "10.1.0.1", null))).isEqualTo(204);
        assertThat(status(view(id, "SEARCH", "10.1.0.1", null))).isEqualTo(204);     // trùng trong 30 phút → bỏ qua
        view(id, "SEARCH", "10.1.0.2", null);
        view(id, "EXTERNAL_LINK", "10.1.0.3", null);
        assertThat(flushJob.flush()).isEqualTo(2);

        Map<String, Object> search = jdbc.queryForMap(
                "SELECT view_count, unique_visitor_count, vendor_id FROM product_view_stats WHERE product_id = ? AND stat_date = ? AND source = 'SEARCH'",
                id, today());
        assertThat(((Number) search.get("view_count")).intValue()).isEqualTo(2);
        assertThat(((Number) search.get("unique_visitor_count")).intValue()).isEqualTo(2);
        assertThat(((Number) search.get("vendor_id")).longValue()).isEqualTo(shop.userId());
        assertThat(jdbc.queryForObject("SELECT view_count FROM product_view_stats WHERE product_id = ? AND source = 'EXTERNAL_LINK'",
                Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT view_count FROM products WHERE id = ?", Long.class, id)).isEqualTo(3);

        // Lượt mới sau lần ghi đầu → cộng dồn
        view(id, "SEARCH", "10.1.0.4", null);
        flushJob.flush();
        assertThat(jdbc.queryForObject("SELECT view_count FROM product_view_stats WHERE product_id = ? AND source = 'SEARCH'",
                Integer.class, id)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT view_count FROM products WHERE id = ?", Long.class, id)).isEqualTo(4);
        assertThat(flushJob.flush()).isZero();
    }

    @Test
    void signedInViewerIsTrackedByUserAndRemembersCategory() throws Exception {
        long cat = data.category("Thời trang", null);
        var shop = data.shop().create();
        long id = data.product(shop).category(cat).sku(1000, 1).create();
        String email = uniqueEmail();
        User user = createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));

        view(id, "HOME_FEED", "10.2.0.1", token);
        view(id, "HOME_FEED", "10.2.0.2", token);    // khác IP nhưng cùng người → vẫn là lượt trùng
        flushJob.flush();
        assertThat(jdbc.queryForObject("SELECT view_count FROM product_view_stats WHERE product_id = ?", Integer.class, id)).isEqualTo(1);
        assertThat(recent.topCategories(user.getId(), 3)).containsExactly(cat);
    }

    @Test
    void rejectsUnknownSourceAndUnavailableProduct() throws Exception {
        var shop = data.shop().create();
        long id = data.product(shop).sku(1000, 1).create();
        long hidden = data.product(shop).status("HIDDEN").sku(1000, 1).create();
        assertThat(status(view(id, "FACEBOOK", "10.3.0.1", null))).isEqualTo(422);
        MvcResult gone = view(hidden, "SEARCH", "10.3.0.1", null);
        assertThat(status(gone)).isEqualTo(404);
        assertThat((String) read(gone, "$.code")).isEqualTo("GNE-CAT-4042");
    }
}
