package com.genie.promotion.coin.application;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình Điểm Genie {@code genie.coin} (plan 2026-11-16 Điểm Genie P01.02, D3).
 *
 * @param expiryMonths     số tháng một lô Điểm còn hạn (LLD 9.9 — 12)
 * @param expiringSoonDays cửa sổ "sắp hết hạn" hiển thị (30)
 * @param checkIn          7 ô thưởng điểm danh (chỉ để hiển thị — luật thật ở {@code DailyCheckin.checkIn})
 * @param rewards          danh mục quà đổi Điểm → voucher sàn theo mã
 */
@ConfigurationProperties(prefix = "genie.coin")
public record CoinProperties(@DefaultValue("12") int expiryMonths, @DefaultValue("30") int expiringSoonDays,
                             CheckIn checkIn, List<Reward> rewards) {

    public CoinProperties {
        checkIn = checkIn == null ? new CheckIn(List.of(100, 100, 100, 100, 100, 100, 1000)) : checkIn;
        rewards = rewards == null ? List.of() : List.copyOf(rewards);
    }

    public record CheckIn(List<Integer> rewards) {
    }

    /** Một quà: {@code id} hiển thị cho client, giá Điểm, mã voucher sàn được cấp. */
    public record Reward(long id, int coinCost, String voucherCode) {
    }
}
