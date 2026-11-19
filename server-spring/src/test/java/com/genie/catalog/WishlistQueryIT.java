package com.genie.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.catalog.wishlist.application.WishlistFacade;
import com.genie.catalog.wishlist.application.WishlistShopAvailability;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;

/** UC-CAT-08 — danh sách yêu thích (plan wishlist W03.05, W03.06). Bật thống kê Hibernate để đếm câu SQL (chống N+1). */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(WishlistQueryIT.VacationConfig.class)
class WishlistQueryIT extends WishlistTestSupport {

    private static final String URL = "/api/v1/me/wishlist";

    /** Shop đang nghỉ — cổng D3 giả (module vendor chưa cài). */
    static final Set<Long> ON_VACATION = ConcurrentHashMap.newKeySet();

    @TestConfiguration
    static class VacationConfig {
        @Bean
        @Primary
        WishlistShopAvailability fakeVacation() {
            return ids -> ids.stream().filter(ON_VACATION::contains).collect(Collectors.toSet());
        }
    }

    @Autowired
    EntityManagerFactory emf;

    @Autowired
    WishlistFacade facade;

    private void like(long u, long productId) throws Exception {
        mvc.perform(put(URL + "/" + productId).header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isNoContent());
    }

    @Test
    void mapsPriceDefaultVariantImageAndPurchasable() throws Exception {
        long u = customer();
        long shop = data.vendor();
        long multi = data.product(shop, "Điện thoại", "PUBLISHED", 6_000_000);
        data.variant(multi, 5_290_000, 0);             // rẻ nhất nhưng hết hàng
        long inStock = data.variant(multi, 5_490_000, 4);
        data.variant(multi, 5_990_000, 9);
        data.primaryImage(multi, "seed/product/phone.jpg");
        long soldOut = data.product(shop, "Áo thun", "PUBLISHED", 89_000);
        long firstSku = data.variant(soldOut, 89_000, 0);
        long hidden = onSaleProduct(shop, "Bị ẩn", 100_000);
        long vacationShop = data.vendor();
        long resting = onSaleProduct(vacationShop, "Balo du lịch", 189_000);
        for (long p : new long[] {multi, soldOut, hidden, resting}) {
            like(u, p);
        }
        data.setStatus(hidden, "HIDDEN");
        ON_VACATION.add(vacationShop);
        try {
            String body = mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, auth(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.summary.total").value(4))
                    .andExpect(jsonPath("$.data.length()").value(4))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            // mới thích trước: resting, hidden, soldOut, multi
            assertThat(JsonPath.<List<Integer>>read(body, "$.data[*].product_id"))
                    .containsExactly((int) resting, (int) hidden, (int) soldOut, (int) multi);
            assertThat(JsonPath.<Integer>read(body, "$.data[3].price")).isEqualTo(5_290_000);
            assertThat(JsonPath.<Integer>read(body, "$.data[3].default_variant_id")).isEqualTo((int) inStock);
            assertThat(JsonPath.<Boolean>read(body, "$.data[3].is_purchasable")).isTrue();
            assertThat(JsonPath.<String>read(body, "$.data[3].image_url")).endsWith("/product-media/seed/product/phone.jpg");
            assertThat(JsonPath.<String>read(body, "$.data[3].product_slug")).isEqualTo("dien-thoai");
            assertThat(JsonPath.<Boolean>read(body, "$.data[2].is_purchasable")).isFalse();             // hết hàng
            assertThat(JsonPath.<Integer>read(body, "$.data[2].default_variant_id")).isEqualTo((int) firstSku);
            assertThat(JsonPath.<Boolean>read(body, "$.data[1].is_purchasable")).isFalse();             // bị ẩn
            assertThat(JsonPath.<Boolean>read(body, "$.data[0].is_purchasable")).isFalse();             // shop nghỉ
        } finally {
            ON_VACATION.clear();
        }
    }

    @Test
    void pagesWithoutDuplicatesAndFixedQueryCount() throws Exception {
        long u = customer();
        long shop = data.vendor();
        List<Long> liked = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            long p = onSaleProduct(shop, "SP " + i, 10_000 + i);
            data.variant(p, 9_000 + i, 3);   // 2 SKU mỗi sản phẩm
            like(u, p);
            liked.add(p);
        }
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        String first = mvc.perform(get(URL).param("limit", "20").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(jsonPath("$.data.length()").value(20))
                .andExpect(jsonPath("$.pagination.has_more").value(true))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        // 1 trang 20 dòng: user_wishlists + products + product_variants + product_images + count — không phụ thuộc số dòng
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(6);

        String cursor = JsonPath.read(first, "$.pagination.next_cursor");
        String second = mvc.perform(get(URL).param("limit", "20").param("cursor", cursor)
                        .header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.pagination.next_cursor").isEmpty())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Integer> all = new ArrayList<>(JsonPath.<List<Integer>>read(first, "$.data[*].product_id"));
        all.addAll(JsonPath.read(second, "$.data[*].product_id"));
        assertThat(all).doesNotHaveDuplicates().hasSize(25);

        mvc.perform(get(URL).param("limit", "51").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(status().isUnprocessableEntity());
        assertThat(facade.userIdsWishing(liked.get(0))).containsExactly(u);
    }

    @Test
    void emptyListHasZeroTotal() throws Exception {
        long u = customer();
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.summary.total").value(0));
    }
}
