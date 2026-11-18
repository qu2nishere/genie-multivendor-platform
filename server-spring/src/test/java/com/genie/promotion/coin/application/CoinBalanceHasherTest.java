package com.genie.promotion.coin.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.promotion.coin.domain.UserCoin;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** Băm số dư Điểm (plan Điểm Genie P01.04). Vector cố định khớp dữ liệu mẫu R__seed_demo_70 (khóa máy dev). */
class CoinBalanceHasherTest {

    private final CoinBalanceHasher hasher = new CoinBalanceHasher("local-wallet-secret");

    @Test
    void matchesFixedVectorUsedBySeed() {
        assertThat(hasher.hash(1, 1250)).isEqualTo("dfb51a70ee103bbbe1c9d25d024d4313ff28896633fc6eb3649038dc4cfcfbed");
    }

    @Test
    void sameInputSameHashDifferentInputDifferentHash() {
        assertThat(hasher.hash(7, 100)).isEqualTo(hasher.hash(7, 100));
        assertThat(hasher.hash(7, 100)).isNotEqualTo(hasher.hash(7, 101)).isNotEqualTo(hasher.hash(8, 100));
        assertThat(new CoinBalanceHasher("other").hash(7, 100)).isNotEqualTo(hasher.hash(7, 100));
    }

    @Test
    void sealAndDetectTampering() {
        UserCoin coin = UserCoin.builder().userId(9L).balance(300).lastUpdated(OffsetDateTime.now()).build();
        hasher.seal(coin);
        assertThat(hasher.matches(coin)).isTrue();
        coin.setBalance(999_999);   // sửa thẳng CSDL
        assertThat(hasher.matches(coin)).isFalse();
    }
}
