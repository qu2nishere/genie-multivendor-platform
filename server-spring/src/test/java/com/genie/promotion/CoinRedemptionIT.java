package com.genie.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.promotion.coin.application.CoinBalanceHasher;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** UC-PRO-09 — đổi Điểm lấy voucher (plan Điểm Genie P05.06). Quà theo cấu hình mặc định: 1 = COIN500 (500 Điểm). */
class CoinRedemptionIT extends CoinTestSupport {

    @Autowired
    CoinBalanceHasher hasher;

    @BeforeEach
    void vouchers() {
        at("2026-06-01T09:00");
        Timestamp from = Timestamp.from(clock.instant().minus(1, ChronoUnit.DAYS));
        Timestamp to = Timestamp.from(clock.instant().plus(300, ChronoUnit.DAYS));
        for (Object[] v : new Object[][] {{"COIN500", 5000, 50000}, {"COIN1000", 10000, 100000}, {"COIN2000", 20000, 200000}}) {
            jdbc.update("INSERT INTO vouchers (code, title, scope, type, discount_type, discount_value, min_order_value, "
                    + "usage_limit, usage_per_user, used_count, priority, valid_from, valid_to, is_active, version, "
                    + "total_discount_used, sponsor_type) VALUES (?, ?, 'ALL', 'PLATFORM', 'FIXED_AMOUNT', ?, ?, 100000, 1, 0, "
                    + "2, ?, ?, TRUE, 0, 0, 'PLATFORM') ON CONFLICT (code) DO UPDATE SET valid_from = EXCLUDED.valid_from, "
                    + "valid_to = EXCLUDED.valid_to, is_active = TRUE", v[0], "Quà " + v[0], v[1], v[2], from, to);
        }
    }

    private void giveCoins(long u, int amount) {
        jdbc.update("INSERT INTO coin_transactions (user_id, amount, type, expires_at, created_at) "
                + "VALUES (?, ?, 'ORDER_CASHBACK', ?, now())", u, amount, Timestamp.from(clock.instant().plus(300, ChronoUnit.DAYS)));
        jdbc.update("INSERT INTO user_coins (user_id, balance, version, balance_hash, last_updated) VALUES (?, ?, 0, ?, now())",
                u, amount, hasher.hash(u, amount));
    }

    private ResultActions redeem(long u, Object rewardId) throws Exception {
        return mvc.perform(post("/api/v1/me/coins/redemptions").header(HttpHeaders.AUTHORIZATION, auth(u))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reward_id\":" + rewardId + "}"));
    }

    @Test
    void catalogIsPublicAndCached() throws Exception {
        mvc.perform(get("/api/v1/public/coin-rewards"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=300, public"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].coin_cost").value(500))
                .andExpect(jsonPath("$.data[0].voucher_value").value(5000))
                .andExpect(jsonPath("$.data[0].min_order_value").value(50000));
    }

    @Test
    void redeemDebitsCoinsAndAddsVoucherToWallet() throws Exception {
        long u = newCustomer();
        giveCoins(u, 1250);
        redeem(u, 1).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.balance").value(750))
                .andExpect(jsonPath("$.data.voucher.code").value("COIN500"))
                .andExpect(jsonPath("$.data.voucher.type").value("PLATFORM"))
                .andExpect(jsonPath("$.data.voucher.discount_value").value(5000))
                .andExpect(jsonPath("$.data.voucher.shop").isEmpty())
                .andExpect(jsonPath("$.data.voucher.state").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.voucher.is_saved").value(true));
        assertThat(balance(u)).isEqualTo(750);
        assertThat(ledgerSum(u)).isEqualTo(750);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM coin_transactions WHERE user_id = ? AND type = 'VOUCHER_REDEEM' "
                + "AND amount = -500 AND label = 'Đổi voucher COIN500'", Integer.class, u)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_vouchers uv JOIN vouchers v ON v.id = uv.voucher_id "
                + "WHERE uv.user_id = ? AND v.code = 'COIN500'", Integer.class, u)).isEqualTo(1);

        // đổi lần 2 khi voucher còn trong kho, chưa dùng → 409 4092, không trừ Điểm
        redeem(u, 1).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GNE-PRO-4092"));
        assertThat(balance(u)).isEqualTo(750);
    }

    @Test
    void insufficientCoinsWritesNothing() throws Exception {
        long u = newCustomer();
        giveCoins(u, 900);
        redeem(u, 2).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-PRO-4222"))
                .andExpect(jsonPath("$.details.missing").value(100));
        assertThat(balance(u)).isEqualTo(900);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_vouchers WHERE user_id = ?", Integer.class, u)).isZero();
    }

    @Test
    void usedUpVoucherCannotBeRedeemedAgain() throws Exception {
        long u = newCustomer();
        giveCoins(u, 2000);
        redeem(u, 1).andExpect(status().isCreated());
        // giả lập đã dùng hết lượt (usage_per_user = 1): một lượt dùng ở voucher_usages (cần phiên checkout)
        Long voucherId = jdbc.queryForObject("SELECT id FROM vouchers WHERE code = 'COIN500'", Long.class);
        Long session = jdbc.queryForObject("INSERT INTO checkout_sessions (user_id, status, total_amount, created_at, "
                + "expires_at) VALUES (?, 'COMPLETED', 0, now(), now()) RETURNING id", Long.class, u);
        jdbc.update("INSERT INTO voucher_usages (voucher_id, user_id, checkout_session_id, used_at) VALUES (?, ?, ?, now())",
                voucherId, u, session);
        redeem(u, 1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GNE-PRO-4092"))
                .andExpect(jsonPath("$.details.reason").value("USAGE_LIMIT_REACHED"));
        assertThat(balance(u)).isEqualTo(1500);
    }

    @Test
    void unknownRewardIsNotFoundAndMissingIdIsInvalid() throws Exception {
        long u = newCustomer();
        giveCoins(u, 5000);
        redeem(u, 99).andExpect(status().isNotFound());
        redeem(u, null).andExpect(status().isUnprocessableEntity());
        assertThat(balance(u)).isEqualTo(5000);
    }

    @Test
    void concurrentRedemptionsSpendOnlyOnce() throws Exception {
        long u = newCustomer();
        giveCoins(u, 1500);   // đủ cho đúng 1 lần COIN1000
        String token = auth(u);
        ExecutorService pool = Executors.newFixedThreadPool(5);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Callable<Integer> c = () -> {
                go.await();
                return mvc.perform(post("/api/v1/me/coins/redemptions").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reward_id\":2}")).andReturn().getResponse()
                        .getStatus();
            };
            futures.add(pool.submit(c));
        }
        go.countDown();
        List<Integer> codes = new ArrayList<>();
        for (Future<Integer> f : futures) {
            codes.add(f.get());
        }
        pool.shutdown();
        assertThat(codes).filteredOn(c -> c == 201).hasSize(1);
        assertThat(balance(u)).isEqualTo(500);
        assertThat(ledgerSum(u)).isEqualTo(500);
    }
}
