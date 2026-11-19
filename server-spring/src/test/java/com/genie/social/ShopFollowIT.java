package com.genie.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.domain.UserRole;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** UC-SOC-06 — theo dõi / hủy theo dõi, không đếm sai (plan theo dõi gian hàng F02.05, F02.06). */
class ShopFollowIT extends ShopFollowTestSupport {

    @Test
    void followAndUnfollowAreIdempotentAndCountCorrectly() throws Exception {
        long[] s = shop();
        assertThat(s[1]).isNotEqualTo(s[0]);   // shop_id (vendor_profiles.id) khác users.id chủ shop
        long u = customer();
        mvc.perform(put(URL + "/" + s[1]).header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isNoContent());
        mvc.perform(put(URL + "/" + s[1]).header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isNoContent());
        assertThat(followers(s[0])).isEqualTo(1);
        assertThat(rows(s[0])).isEqualTo(1);
        mvc.perform(get(URL + "/shop-ids").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0]").value(s[1]));

        mvc.perform(delete(URL + "/" + s[1]).header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isNoContent());
        mvc.perform(delete(URL + "/" + s[1]).header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isNoContent());
        assertThat(followers(s[0])).isZero();   // không âm
        assertThat(rows(s[0])).isZero();
    }

    @Test
    void cannotFollowOwnShopOrUnfollowableShops() throws Exception {
        long[] s = shop();
        mvc.perform(put(URL + "/" + s[1]).header(HttpHeaders.AUTHORIZATION, auth(s[0], UserRole.VENDOR)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GNE-SOC-4095"));

        long u = customer();
        long[] pending = shop();
        jdbc.update("UPDATE vendor_profiles SET kyc_status = 'MANUAL_REVIEW' WHERE user_id = ?", pending[0]);
        long[] suspended = shop();
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE id = ?", suspended[0]);
        for (long shopId : new long[] {pending[1], suspended[1], 999_999L}) {
            mvc.perform(put(URL + "/" + shopId).header(HttpHeaders.AUTHORIZATION, auth(u)))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("GNE-SYS-4040"));
        }
        assertThat(followers(pending[0]) + followers(suspended[0])).isZero();
        mvc.perform(put(URL + "/" + s[1])).andExpect(status().isUnauthorized());
    }

    @Test
    void lockedShopCanStillBeUnfollowedAndDisappearsFromIds() throws Exception {
        long[] s = shop();
        long u = customer();
        mvc.perform(put(URL + "/" + s[1]).header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isNoContent());
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE id = ?", s[0]);
        mvc.perform(get(URL + "/shop-ids").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(delete(URL + "/" + s[1]).header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isNoContent());
        assertThat(rows(s[0])).isZero();
        assertThat(followers(s[0])).isZero();
    }

    @Test
    void concurrentFollowsNeverMiscount() throws Exception {
        long[] s = shop();
        ExecutorService pool = Executors.newFixedThreadPool(20);
        try {
            // 20 request của CÙNG một người → +1 đúng một lần
            long u = customer();
            String token = auth(u);
            runAll(pool, 20, i -> put(URL + "/" + s[1]).header(HttpHeaders.AUTHORIZATION, token));
            assertThat(followers(s[0])).isEqualTo(1);

            // 20 người khác nhau cùng theo dõi → +20
            List<String> tokens = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                tokens.add(auth(customer()));
            }
            runAll(pool, 20, i -> put(URL + "/" + s[1]).header(HttpHeaders.AUTHORIZATION, tokens.get(i)));
            assertThat(followers(s[0])).isEqualTo(21);

            // xen kẽ theo dõi / hủy của 20 người → bộ đếm luôn bằng số dòng thật
            runAll(pool, 40, i -> (i % 2 == 0 ? delete(URL + "/" + s[1]) : put(URL + "/" + s[1]))
                    .header(HttpHeaders.AUTHORIZATION, tokens.get(i / 2)));
            assertThat(followers(s[0])).isEqualTo(rows(s[0]));
        } finally {
            pool.shutdown();
        }
    }

    private interface Req {
        MockHttpServletRequestBuilder build(int i);
    }

    private void runAll(ExecutorService pool, int n, Req req) throws Exception {
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> fs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final int k = i;
            Callable<Integer> c = () -> {
                go.await();
                return mvc.perform(req.build(k)).andReturn().getResponse().getStatus();
            };
            fs.add(pool.submit(c));
        }
        go.countDown();
        for (Future<Integer> f : fs) {
            assertThat(f.get()).isEqualTo(204);
        }
    }
}
