package com.genie.promotion;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.promotion.coin.application.CoinBalanceHasher;
import com.genie.promotion.coin.application.CoinExpiryJob;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Lô Điểm hết hạn sau 12 tháng (plan Điểm Genie P06.04). */
class CoinExpiryJobIT extends CoinTestSupport {

    @Autowired
    CoinExpiryJob job;

    @Autowired
    CoinBalanceHasher hasher;

    /** Lô cộng hết hạn lúc {@code expires} + trừ {@code spent}; ví khớp băm. */
    private void wallet(long u, Object[][] rows) {
        int balance = 0;
        for (Object[] r : rows) {
            int amount = (int) r[0];
            balance += amount;
            jdbc.update("INSERT INTO coin_transactions (user_id, amount, type, expires_at, created_at) VALUES (?, ?, ?, ?, now())",
                    u, amount, amount > 0 ? "ORDER_CASHBACK" : "USED_AT_CHECKOUT", r[1] == null ? null : Timestamp.from((Instant) r[1]));
        }
        jdbc.update("INSERT INTO user_coins (user_id, balance, version, balance_hash, last_updated) VALUES (?, ?, 0, ?, now())",
                u, balance, hasher.hash(u, balance));
    }

    @Test
    void expiresRemainingOfOverdueLotsOnlyOnce() {
        at("2027-06-02T00:15");
        Instant now = clock.instant();
        long u = newCustomer();
        long v = newCustomer();
        // u: lô 100 quá hạn 1 ngày (đã tiêu 30) + lô 50 còn hạn
        wallet(u, new Object[][] {{100, now.minus(1, ChronoUnit.DAYS)}, {50, now.plus(200, ChronoUnit.DAYS)}, {-30, null}});
        // v: lô 80 quá hạn, chưa tiêu
        wallet(v, new Object[][] {{80, now.minus(3, ChronoUnit.DAYS)}});

        assertThat(job.run()).isEqualTo(70 + 80);
        assertThat(balance(u)).isEqualTo(50);
        assertThat(ledgerSum(u)).isEqualTo(50);
        assertThat(balance(v)).isZero();
        assertThat(ledgerSum(v)).isZero();
        assertThat(jdbc.queryForObject("SELECT amount FROM coin_transactions WHERE user_id = ? AND type = 'EXPIRED'",
                Integer.class, u)).isEqualTo(-70);
        assertThat(jdbc.queryForObject("SELECT label FROM coin_transactions WHERE user_id = ? AND type = 'EXPIRED'",
                String.class, u)).isEqualTo("Điểm hết hạn");

        // chạy lại: không trừ thêm, lô còn hạn không bị đụng
        assertThat(job.run()).isZero();
        assertThat(balance(u)).isEqualTo(50);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM coin_transactions WHERE user_id = ? AND type = 'EXPIRED'",
                Integer.class, u)).isEqualTo(1);
    }

    @Test
    void fullySpentOverdueLotExpiresNothing() {
        at("2027-06-02T00:15");
        Instant now = clock.instant();
        long u = newCustomer();
        wallet(u, new Object[][] {{100, now.minus(1, ChronoUnit.DAYS)}, {-100, null}});
        job.run();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM coin_transactions WHERE user_id = ? AND type = 'EXPIRED'",
                Integer.class, u)).isZero();
        assertThat(balance(u)).isZero();
    }
}
