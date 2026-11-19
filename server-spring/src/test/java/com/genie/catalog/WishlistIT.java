package com.genie.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** UC-CAT-08 — thích / bỏ thích / tập ID (plan wishlist W02.05, W02.06). */
class WishlistIT extends WishlistTestSupport {

    private static final String URL = "/api/v1/me/wishlist/";

    @Test
    void addIsIdempotentAndShowsInProductIds() throws Exception {
        long u = customer();
        long shop = data.vendor();
        long a = onSaleProduct(shop, "Tai nghe", 389000);
        long b = onSaleProduct(shop, "Loa", 320000);
        mvc.perform(put(URL + a).header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isNoContent());
        mvc.perform(put(URL + a).header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isNoContent());
        mvc.perform(put(URL + b).header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isNoContent());
        assertThat(rows(u)).isEqualTo(2);
        mvc.perform(get(URL + "product-ids").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0]").value(b));   // mới thích trước
    }

    @Test
    void removeIsIdempotentEvenForHiddenProducts() throws Exception {
        long u = customer();
        long p = onSaleProduct(data.vendor(), "Đèn bàn", 149000);
        mvc.perform(put(URL + p).header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isNoContent());
        data.setStatus(p, "HIDDEN");   // bị ẩn sau khi đã thích
        mvc.perform(delete(URL + p).header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isNoContent());
        mvc.perform(delete(URL + p).header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isNoContent());
        mvc.perform(delete(URL + "999999").header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isNoContent());
        assertThat(rows(u)).isZero();
    }

    @Test
    void cannotLikeMissingOrUnpublishedProducts() throws Exception {
        long u = customer();
        long shop = data.vendor();
        long draft = data.product(shop, "Nháp", "DRAFT", 1000);
        long hidden = data.product(shop, "Ẩn", "HIDDEN", 1000);
        for (long id : new long[] {draft, hidden, 999_999L}) {
            mvc.perform(put(URL + id).header(HttpHeaders.AUTHORIZATION, auth(u)))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("GNE-CAT-4042"));
        }
        mvc.perform(put(URL + "0").header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isUnprocessableEntity());
        mvc.perform(put(URL + "abc").header(HttpHeaders.AUTHORIZATION, auth(u))).andExpect(status().isUnprocessableEntity());
        assertThat(rows(u)).isZero();
    }

    @Test
    void listsAreIsolatedPerUserAndRequireLogin() throws Exception {
        long a = customer();
        long b = customer();
        long p = onSaleProduct(data.vendor(), "Balo", 189000);
        mvc.perform(put(URL + p).header(HttpHeaders.AUTHORIZATION, auth(a))).andExpect(status().isNoContent());
        mvc.perform(get(URL + "product-ids").header(HttpHeaders.AUTHORIZATION, auth(b)))
                .andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get(URL + "product-ids")).andExpect(status().isUnauthorized());
        mvc.perform(put(URL + p)).andExpect(status().isUnauthorized());
    }

    @Test
    void twentyConcurrentLikesKeepOneRowAndMixedTogglesNeverFail() throws Exception {
        long u = customer();
        long p = onSaleProduct(data.vendor(), "Cốc sứ", 59000);
        String token = auth(u);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> fs = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Callable<Integer> c = () -> {
                go.await();
                return mvc.perform(put(URL + p).header(HttpHeaders.AUTHORIZATION, token)).andReturn().getResponse().getStatus();
            };
            fs.add(pool.submit(c));
        }
        go.countDown();
        for (Future<Integer> f : fs) {
            assertThat(f.get()).isEqualTo(204);
        }
        assertThat(rows(u)).isEqualTo(1);

        // xen kẽ thích / bỏ thích: không lỗi 500, cuối cùng 0 hoặc 1 dòng
        CountDownLatch go2 = new CountDownLatch(1);
        List<Future<Integer>> mixed = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final boolean like = i % 2 == 0;
            Callable<Integer> c = () -> {
                go2.await();
                return mvc.perform((like ? put(URL + p) : delete(URL + p)).header(HttpHeaders.AUTHORIZATION, token))
                        .andReturn().getResponse().getStatus();
            };
            mixed.add(pool.submit(c));
        }
        go2.countDown();
        for (Future<Integer> f : mixed) {
            assertThat(f.get()).isEqualTo(204);
        }
        pool.shutdown();
        assertThat(rows(u)).isBetween(0, 1);
    }
}
