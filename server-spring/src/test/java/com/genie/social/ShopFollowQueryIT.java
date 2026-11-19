package com.genie.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.social.follow.application.ShopFollowFacade;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

/** UC-SOC-06 — danh sách shop đang theo dõi (plan theo dõi gian hàng F03.05). */
class ShopFollowQueryIT extends ShopFollowTestSupport {

    @Autowired
    ShopFollowFacade facade;

    private void follow(long u, long shopId) throws Exception {
        mvc.perform(put(URL + "/" + shopId).header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isNoContent());
    }

    @Test
    void listsNewestFirstWithShopMiniAndHidesSuspendedShops() throws Exception {
        long u = customer();
        long other = customer();
        long[] a = shop();
        long[] b = shop();
        long[] c = shop();
        jdbc.update("UPDATE vendor_profiles SET is_mall = TRUE, rating_avg = 4.9, logo_url = 'shop/logo-a.png' WHERE user_id = ?",
                a[0]);
        follow(u, a[1]);
        follow(u, b[1]);
        follow(u, c[1]);
        follow(other, b[1]);
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE id = ?", c[0]);   // shop c bị đình chỉ

        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.total").value(2))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].shop.id").value(b[1]))
                .andExpect(jsonPath("$.data[0].follower_count").value(2))
                .andExpect(jsonPath("$.data[1].shop.id").value(a[1]))
                .andExpect(jsonPath("$.data[1].shop.is_mall").value(true))
                .andExpect(jsonPath("$.data[1].rating_avg").value(4.9))
                .andExpect(jsonPath("$.data[1].shop.logo_url").value(org.hamcrest.Matchers.endsWith("/content-assets/shop/logo-a.png")))
                .andExpect(jsonPath("$.data[1].shop.name").isNotEmpty());
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, auth(other)))
                .andExpect(jsonPath("$.data.length()").value(1));
        assertThat(facade.followerIdsOf(b[0])).containsExactlyInAnyOrder(u, other);
        assertThat(facade.countFollowing(u)).isEqualTo(3);
    }

    @Test
    void pagesWithoutDuplicates() throws Exception {
        long u = customer();
        for (int i = 0; i < 5; i++) {
            follow(u, shop()[1]);
        }
        List<Integer> ids = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            var req = get(URL).param("limit", "2").header(HttpHeaders.AUTHORIZATION, auth(u));
            if (cursor != null) {
                req = req.param("cursor", cursor);
            }
            String body = mvc.perform(req).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            ids.addAll(JsonPath.<List<Integer>>read(body, "$.data[*].shop.id"));
            cursor = JsonPath.read(body, "$.pagination.next_cursor");
            pages++;
        } while (cursor != null);
        assertThat(pages).isEqualTo(3);
        assertThat(ids).hasSize(5).doesNotHaveDuplicates();
        mvc.perform(get(URL).param("limit", "0").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(status().isUnprocessableEntity());
    }
}
