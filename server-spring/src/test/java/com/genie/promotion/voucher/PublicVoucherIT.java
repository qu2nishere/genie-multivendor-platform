// GET /public/vouchers (plan 2026-11-26 voucher V05.06; API_SPEC 5.13)
package com.genie.promotion.voucher;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.genie.common.domain.UserRole;
import com.genie.support.TestDataBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicVoucherIT extends VoucherTestSupport {

    private String fo() {
        return auth(staff("MANAGE_VOUCHER"), UserRole.STAFF);
    }

    private long platform(String code, int dayFrom, int dayTo, String extra) throws Exception {
        return post_("/api/v1/admin/vouchers", fo(), percentBody(code, now().plusDays(dayFrom), now().plusDays(dayTo),
                "\"title\":\"Voucher sàn " + code + "\"" + (extra == null ? "" : "," + extra))).data().get("id").asLong();
    }

    private long shopVoucher(TestDataBuilder.Shop s, String code, int dayFrom, int dayTo) throws Exception {
        return post_("/api/v1/seller/vouchers", auth(s.userId(), UserRole.VENDOR),
                percentBody(code, now().plusDays(dayFrom), now().plusDays(dayTo), null)).data().get("id").asLong();
    }

    private static List<String> codes(Res r) {
        return r.body().get("data").findValues("code").stream().map(JsonNode::asText).toList();
    }

    @Test
    void featuredReturnsLivePlatformVouchersOnlyWithoutCoinRewards() throws Exception {
        coinRewardVoucher();
        String live = code();
        String upcoming = code();
        String disabled = code();
        platform(live, 0, 5, null);
        platform(upcoming, 1, 5, null);
        long off = platform(disabled, 0, 5, null);
        patch_("/api/v1/admin/vouchers/" + off, fo(), "{\"is_active\":false}");

        Res r = get_("/api/v1/public/vouchers?featured=true&limit=12", null);
        assertThat(r.status()).isEqualTo(200);
        List<String> got = codes(r);
        assertThat(got).contains(live, upcoming).doesNotContain(disabled, "COIN500");
        assertThat(r.body().get("data")).allSatisfy(v -> {
            assertThat(v.get("type").asText()).isIn("PLATFORM", "FREESHIP");
            assertThat(v.get("shop").isNull()).isTrue();
            assertThat(v.has("is_saved") && !v.get("is_saved").isNull()).isFalse();
        });
        JsonNode up = r.body().get("data").findParents("code").stream().filter(n -> n.get("code").asText().equals(upcoming))
                .findFirst().orElseThrow();
        assertThat(up.get("state").asText()).isEqualTo("UPCOMING");
        assertThat(get_("/api/v1/public/vouchers?featured=true&limit=13", null).status()).isEqualTo(422);
        assertThat(get_("/api/v1/public/vouchers", null).status()).isEqualTo(422);
        assertThat(get_("/api/v1/public/vouchers?featured=true&code=ABC", null).status()).isEqualTo(422);
    }

    @Test
    void shopModeUsesTheShopProfileIdAndHidesBlockedShops() throws Exception {
        TestDataBuilder.Shop s = data.shop().create();
        long cat = category();
        jdbc.update("UPDATE categories SET name = 'Tai nghe test' WHERE id = ?", cat);
        String running = code();
        String soon = code();
        long runningId = shopVoucher(s, running, 0, 5);
        shopVoucher(s, soon, 2, 5);
        jdbc.update("INSERT INTO voucher_excluded_categories (voucher_id, category_id, created_at) VALUES (?, ?, now())",
                runningId, cat);

        Res r = get_("/api/v1/public/vouchers?shop_id=" + s.shopId(), null);
        assertThat(codes(r)).containsExactlyInAnyOrder(running, soon);
        JsonNode first = r.body().get("data").findParents("code").stream()
                .filter(n -> n.get("code").asText().equals(running)).findFirst().orElseThrow();
        assertThat(first.at("/shop/id").asLong()).isEqualTo(s.shopId());       // vendor_profiles.id, không phải users.id
        assertThat(first.at("/shop/name").asText()).isEqualTo(s.name());
        assertThat(first.get("excluded_categories").get(0).asText()).isEqualTo("Tai nghe test");
        assertThat(first.get("discount_value").asInt()).isEqualTo(15);
        if (s.userId() != s.shopId()) {   // truyền nhầm users.id thì không ra voucher của shop này
            assertThat(codes(get_("/api/v1/public/vouchers?shop_id=" + s.userId(), null))).doesNotContain(running);
        }

        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE id = ?", s.userId());
        assertThat(get_("/api/v1/public/vouchers?shop_id=" + s.shopId(), null).body().get("data")).isEmpty();
        assertThat(codes(get_("/api/v1/public/vouchers?code=" + running, null))).isEmpty();
    }

    @Test
    void codesModeAndIsSavedForTheCaller() throws Exception {
        String a = code();
        String b = code();
        long aId = platform(a, 0, 5, null);
        platform(b, 0, 5, null);
        coinRewardVoucher();
        Res r = get_("/api/v1/public/vouchers?codes=" + a.toLowerCase() + "," + b + ",COIN500,NOPE99", null);
        assertThat(codes(r)).containsExactlyInAnyOrder(a, b);
        assertThat(get_("/api/v1/public/vouchers?code=" + a, null).body().get("data")).hasSize(1);

        long buyer = customer();
        jdbc.update("INSERT INTO user_vouchers (user_id, voucher_id, created_at) VALUES (?, ?, now())", buyer, aId);
        Res withToken = get_("/api/v1/public/vouchers?codes=" + a + "," + b, auth(buyer, UserRole.CUSTOMER));
        for (JsonNode v : withToken.body().get("data")) {
            assertThat(v.get("is_saved").asBoolean()).isEqualTo(v.get("code").asText().equals(a));
        }
    }

    @Test
    void listIsCachedAndEvictedWhenAVoucherChanges() throws Exception {
        String c = code();
        long id = platform(c, 0, 5, null);
        assertThat(codes(get_("/api/v1/public/vouchers?featured=true&limit=12", null))).contains(c);
        assertThat(redis.keys("cache:promotion:public_vouchers:*")).isNotEmpty();
        patch_("/api/v1/admin/vouchers/" + id, fo(), "{\"is_active\":false}");
        assertThat(redis.keys("cache:promotion:public_vouchers:*")).isEmpty();
        assertThat(codes(get_("/api/v1/public/vouchers?featured=true&limit=12", null))).doesNotContain(c);
    }
}
