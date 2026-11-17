package com.genie.support;

import com.genie.auth.profile.application.AccountCounterContributor;
import com.genie.auth.profile.application.AccountShopProvider;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thay cho module {@code vendor} / {@code order} / {@code notification}… (chưa làm) trong test {@code GET /me}: đặt shop
 * và bộ đếm giả cho một user. Mặc định không có gì → shop = null, đếm = 0.
 */
public class FakeAccountContext implements AccountShopProvider, AccountCounterContributor {

    private final Map<Long, ShopSummary> shops = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Long>> counters = new ConcurrentHashMap<>();

    public void giveShop(long userId, ShopSummary shop) {
        shops.put(userId, shop);
    }

    public void giveCounters(long userId, Map<String, Long> values) {
        counters.put(userId, values);
    }

    public void clear() {
        shops.clear();
        counters.clear();
    }

    @Override
    public Optional<ShopSummary> shopOf(long userId) {
        return Optional.ofNullable(shops.get(userId));
    }

    @Override
    public Map<String, Long> countersOf(long userId) {
        return counters.getOrDefault(userId, Map.of());
    }
}
