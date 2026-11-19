package com.genie.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.ResultActions;

/** UC-PRO-08 — điểm danh qua API (plan Điểm Genie P03.04, P03.05). */
class CheckInIT extends CoinTestSupport {

    private static final String URL = "/api/v1/me/coins/check-ins";

    private ResultActions checkIn(long userId) throws Exception {
        return mvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, auth(userId)));
    }

    @Test
    void sevenConsecutiveDaysPaySixHundredPlusOneThousand() throws Exception {
        long u = newCustomer();
        for (int day = 1; day <= 7; day++) {
            at("2026-06-0" + day + "T09:00");
            checkIn(u).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.reward").value(day == 7 ? 1000 : 100))
                    .andExpect(jsonPath("$.data.streak_days").value(day));
        }
        assertThat(balance(u)).isEqualTo(1600);
        assertThat(ledgerSum(u)).isEqualTo(1600);
        // mỗi lô hết hạn đúng 12 tháng sau lúc điểm danh (09:00 giờ VN ngày 1…7/6/2026 → 1…7/6/2027)
        Integer withExpiry = jdbc.queryForObject("SELECT count(*) FROM coin_transactions WHERE user_id = ? AND type = "
                + "'CHECK_IN' AND expires_at >= '2027-06-01T09:00:00+07:00' AND expires_at <= '2027-06-07T09:00:00+07:00'",
                Integer.class, u);
        assertThat(withExpiry).isEqualTo(7);
        // ngày 8 bắt đầu lại ô 1
        at("2026-06-08T09:00");
        checkIn(u).andExpect(jsonPath("$.data.reward").value(100)).andExpect(jsonPath("$.data.streak_days").value(1));
    }

    @Test
    void secondCheckInSameVietnamDayIsRejected() throws Exception {
        long u = newCustomer();
        at("2026-06-01T08:00");
        checkIn(u).andExpect(status().isCreated());
        at("2026-06-01T22:00");
        checkIn(u).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GNE-PRO-4091"));
        assertThat(balance(u)).isEqualTo(100);
    }

    @Test
    void skippingADayResetsTheStreak() throws Exception {
        long u = newCustomer();
        at("2026-06-01T08:00");
        checkIn(u);
        at("2026-06-02T08:00");
        checkIn(u).andExpect(jsonPath("$.data.streak_days").value(2));
        at("2026-06-04T08:00");
        checkIn(u).andExpect(jsonPath("$.data.streak_days").value(1));
    }

    @Test
    void lateNightAndEarlyMorningAreDifferentVietnamDaysOnTheSameUtcDay() throws Exception {
        long u = newCustomer();
        at("2026-06-01T23:30");   // 16:30Z ngày 01
        checkIn(u).andExpect(status().isCreated());
        at("2026-06-02T00:30");   // 17:30Z ngày 01 — cùng ngày UTC
        checkIn(u).andExpect(status().isCreated()).andExpect(jsonPath("$.data.streak_days").value(2));
        assertThat(balance(u)).isEqualTo(200);
    }

    @Test
    void tenConcurrentRequestsCreditOnlyOnce() throws Exception {
        long u = newCustomer();
        at("2026-06-01T10:00");
        String token = auth(u);
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Callable<Integer> call = () -> {
                start.await();
                return mvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, token)).andReturn().getResponse().getStatus();
            };
            results.add(pool.submit(call));
        }
        start.countDown();
        List<Integer> codes = new ArrayList<>();
        for (Future<Integer> f : results) {
            codes.add(f.get());
        }
        pool.shutdown();
        assertThat(codes).filteredOn(c -> c == 201).hasSize(1);
        assertThat(codes).filteredOn(c -> c == 409).hasSize(9);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM coin_transactions WHERE user_id = ? AND type = 'CHECK_IN'",
                Integer.class, u)).isEqualTo(1);
        assertThat(ledgerSum(u)).isEqualTo(balance(u)).isEqualTo(100);
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mvc.perform(post(URL)).andExpect(status().isUnauthorized());
    }
}
