package com.genie.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.promotion.coin.application.CoinBalanceHasher;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

/** UC-PRO-09 — số dư, sắp hết hạn, trạng thái điểm danh, lịch sử (plan Điểm Genie P04.05). */
class CoinQueryIT extends CoinTestSupport {

    @Autowired
    CoinBalanceHasher hasher;

    /** Dựng ví + các dòng sổ trực tiếp (số dư = Σ, băm đúng khóa test). {@code lots}: {amount, ngày hết hạn tính từ now}. */
    private void ledger(long u, int[][] rows) {
        Instant now = clock.instant();
        int balance = 0;
        for (int[] r : rows) {
            balance += r[0];
            Timestamp exp = r[0] > 0 ? Timestamp.from(now.plus(r[1], ChronoUnit.DAYS)) : null;
            jdbc.update("INSERT INTO coin_transactions (user_id, amount, type, label, expires_at, created_at) "
                    + "VALUES (?, ?, ?, NULL, ?, ?)", u, r[0], r[0] > 0 ? "ORDER_CASHBACK" : "USED_AT_CHECKOUT", exp,
                    Timestamp.from(now.minus(rows.length, ChronoUnit.MINUTES)));
        }
        jdbc.update("INSERT INTO user_coins (user_id, balance, version, balance_hash, last_updated) VALUES (?, ?, 0, ?, now()) "
                + "ON CONFLICT (user_id) DO UPDATE SET balance = EXCLUDED.balance, balance_hash = EXCLUDED.balance_hash",
                u, balance, hasher.hash(u, balance));
    }

    @Test
    void newAccountHasZeroEverything() throws Exception {
        long u = newCustomer();
        at("2026-06-01T09:00");
        mvc.perform(get("/api/v1/me/coins").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.data.balance").value(0))
                .andExpect(jsonPath("$.data.expiring_soon").isEmpty())
                .andExpect(jsonPath("$.data.check_in.streak_days").value(0))
                .andExpect(jsonPath("$.data.check_in.checked_in_today").value(false))
                .andExpect(jsonPath("$.data.check_in.rewards.length()").value(7))
                .andExpect(jsonPath("$.data.check_in.rewards[6]").value(1000));
        mvc.perform(get("/api/v1/me/coins/transactions").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.pagination.has_more").value(false));
    }

    @Test
    void afterCheckInShowsTodayDoneAndNextMidnightVietnam() throws Exception {
        long u = newCustomer();
        at("2026-06-01T23:10");
        mvc.perform(post("/api/v1/me/coins/check-ins").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/me/coins").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(jsonPath("$.data.balance").value(100))
                .andExpect(jsonPath("$.data.check_in.checked_in_today").value(true))
                .andExpect(jsonPath("$.data.check_in.streak_days").value(1))
                .andExpect(jsonPath("$.data.check_in.next_check_in_at").value("2026-06-02T00:00:00+07:00"));
        mvc.perform(get("/api/v1/me/coins/transactions").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(jsonPath("$.data[0].type").value("CHECK_IN"))
                .andExpect(jsonPath("$.data[0].description").value("Điểm danh ngày 1"))
                .andExpect(jsonPath("$.data[0].amount").value(100));
    }

    @Test
    void expiringSoonCountsOnlyRemainingOfLotsWithinThirtyDays() throws Exception {
        long u = newCustomer();
        at("2026-06-01T09:00");
        ledger(u, new int[][] {{150, 20}, {500, 40}});
        mvc.perform(get("/api/v1/me/coins").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(jsonPath("$.data.balance").value(650))
                .andExpect(jsonPath("$.data.expiring_soon.amount").value(150))
                .andExpect(jsonPath("$.data.expiring_soon.within_days").value(30));

        long v = newCustomer();
        ledger(v, new int[][] {{150, 20}, {500, 40}, {-150, 0}});   // đã tiêu ăn hết lô sắp hết hạn (FEFO)
        mvc.perform(get("/api/v1/me/coins").header(HttpHeaders.AUTHORIZATION, auth(v)))
                .andExpect(jsonPath("$.data.balance").value(500))
                .andExpect(jsonPath("$.data.expiring_soon").isEmpty());
    }

    @Test
    void historyPagesWithoutDuplicatesAndOnlyShowsOwnRows() throws Exception {
        long u = newCustomer();
        long other = newCustomer();
        at("2026-06-01T09:00");
        ledger(u, new int[][] {{10, 300}, {20, 300}, {30, 300}, {40, 300}, {50, 300}});
        ledger(other, new int[][] {{999, 300}});
        List<Integer> ids = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            var req = get("/api/v1/me/coins/transactions").param("limit", "2").param("user_id", String.valueOf(other))
                    .header(HttpHeaders.AUTHORIZATION, auth(u));
            if (cursor != null) {
                req = req.param("cursor", cursor);
            }
            String body = mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse()
                    .getContentAsString(StandardCharsets.UTF_8);
            ids.addAll(JsonPath.<List<Integer>>read(body, "$.data[*].id"));
            List<Integer> amounts = JsonPath.read(body, "$.data[*].amount");
            assertThat(amounts).doesNotContain(999);   // không lộ dữ liệu người khác dù gửi user_id
            cursor = JsonPath.read(body, "$.pagination.next_cursor");
            pages++;
        } while (cursor != null);
        assertThat(pages).isEqualTo(3);
        assertThat(ids).hasSize(5).doesNotHaveDuplicates();
        mvc.perform(get("/api/v1/me/coins/transactions").param("limit", "51").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/me/coins/transactions").param("cursor", "%%%").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void tamperedBalanceIsStillReadableButBlocksWrites() throws Exception {
        long u = newCustomer();
        at("2026-06-01T09:00");
        ledger(u, new int[][] {{300, 300}});
        jdbc.update("UPDATE user_coins SET balance = 999999 WHERE user_id = ?", u);   // sửa thẳng CSDL
        mvc.perform(get("/api/v1/me/coins").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.balance").value(999999));
        mvc.perform(post("/api/v1/me/coins/check-ins").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("GNE-SYS-5000"));
        assertThat(ledgerSum(u)).isEqualTo(300);
    }
}
