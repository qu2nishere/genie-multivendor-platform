// Kho Voucher /me/vouchers (plan 2026-11-26 voucher V05.07; API_SPEC 3.14; UC-PRO-03)
package com.genie.promotion.voucher;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.genie.common.domain.UserRole;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class VoucherWalletIT extends VoucherTestSupport {

    private String fo() {
        return auth(staff("MANAGE_VOUCHER"), UserRole.STAFF);
    }

    private long platform(String code, int dayFrom, int dayTo, String perUserLimit) throws Exception {
        String body = percentBody(code, now().plusDays(dayFrom), now().plusDays(dayTo), "\"title\":\"Voucher " + code + "\"");
        if (perUserLimit != null) body = body.replace("\"usage_per_user\":1", "\"usage_per_user\":" + perUserLimit);
        return post_("/api/v1/admin/vouchers", fo(), body).data().get("id").asLong();
    }

    @Test
    void saveByCodeOrIdOnceAndRefuseHiddenVouchers() throws Exception {
        long buyer = customer();
        String me = auth(buyer, UserRole.CUSTOMER);
        String c = code();
        long id = platform(c, 0, 5, null);

        Res saved = post_("/api/v1/me/vouchers", me, "{\"code\":\"" + c.toLowerCase() + "\"}");
        assertThat(saved.status()).isEqualTo(201);
        assertThat(saved.data().get("is_saved").asBoolean()).isTrue();
        assertThat(saved.data().get("state").asText()).isEqualTo("AVAILABLE");
        Res again = post_("/api/v1/me/vouchers", me, "{\"voucher_id\":" + id + "}");
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.code()).isEqualTo("GNE-VCH-4091");

        long upcoming = platform(code(), 2, 5, null);
        assertThat(post_("/api/v1/me/vouchers", me, "{\"voucher_id\":" + upcoming + "}").status()).isEqualTo(201);

        coinRewardVoucher();
        Res coin = post_("/api/v1/me/vouchers", me, "{\"code\":\"COIN500\"}");
        assertThat(coin.status()).isEqualTo(404);
        assertThat(coin.code()).isEqualTo("GNE-VCH-4041");

        long off = platform(code(), 0, 5, null);
        patch_("/api/v1/admin/vouchers/" + off, fo(), "{\"is_active\":false}");
        assertThat(post_("/api/v1/me/vouchers", me, "{\"voucher_id\":" + off + "}").code()).isEqualTo("GNE-VCH-4041");
        assertThat(post_("/api/v1/me/vouchers", me, "{\"code\":\"KHONGCO99\"}").status()).isEqualTo(404);
        assertThat(post_("/api/v1/me/vouchers", me, "{}").status()).isEqualTo(422);
    }

    @Test
    void usageLimitReachedIsReported() throws Exception {
        long buyer = customer();
        long id = platform(code(), 0, 5, null);
        usage(id, buyer, now(), null);
        Res r = post_("/api/v1/me/vouchers", auth(buyer, UserRole.CUSTOMER), "{\"voucher_id\":" + id + "}");
        assertThat(r.status()).isEqualTo(409);
        assertThat(r.body().at("/details/reason").asText()).isEqualTo("USAGE_LIMIT_REACHED");
    }

    @Test
    void concurrentSavesCreateOneRow() throws Exception {
        long buyer = customer();
        String me = auth(buyer, UserRole.CUSTOMER);
        long id = platform(code(), 0, 5, null);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Res>> fs = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                fs.add(pool.submit(() -> post_("/api/v1/me/vouchers", me, "{\"voucher_id\":" + id + "}")));
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<Res> f : fs) statuses.add(f.get().status());
            assertThat(statuses).containsOnlyOnce(201);
            assertThat(statuses).allMatch(s -> s == 201 || s == 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_vouchers WHERE user_id = ? AND voucher_id = ?",
                Integer.class, buyer, id)).isEqualTo(1);
    }

    @Test
    void walletTabsHistoryAndRemove() throws Exception {
        long buyer = customer();
        String me0 = auth(buyer, UserRole.CUSTOMER);
        long avail = platform(code(), 0, 5, null);
        long soon = platform(code(), 1, 5, null);
        long usedUp = platform(code(), 0, 5, null);
        long expired = platform(code(), 0, 1, null);
        for (long id : List.of(avail, soon, usedUp, expired)) {
            assertThat(post_("/api/v1/me/vouchers", me0, "{\"voucher_id\":" + id + "}").status()).isEqualTo(201);
        }
        long shop = post_("/api/v1/seller/vouchers", auth(data.shop().create().userId(), UserRole.VENDOR),
                "{\"code\":\"" + code() + "\",\"discount_type\":\"FREESHIP\",\"max_discount\":30000,\"min_order_value\":0,"
                        + "\"usage_limit\":10,\"usage_per_user\":1,\"valid_from\":\"" + now() + "\",\"valid_to\":\""
                        + now().plusDays(3) + "\"}").data().get("id").asLong();
        post_("/api/v1/me/vouchers", me0, "{\"voucher_id\":" + shop + "}");
        // đơn đã dùng mã
        long order = jdbc.queryForObject("""
                INSERT INTO orders (order_code, customer_id, vendor_id, shipping_address_snapshot_json, sub_total, shipping_fee,
                    grand_total, funding_status, status, created_at, updated_at)
                VALUES (?, ?, ?, '{}'::jsonb, 100000, 0, 100000, 'UNFUNDED', 'COMPLETED', now(), now()) RETURNING id""",
                Long.class, "TR-W" + code(), buyer, data.shop().create().userId());
        usage(usedUp, buyer, now(), order);
        clock.advance(java.time.Duration.ofDays(2));   // expired hết hạn (valid_to = +1 ngày)
        String me = auth(buyer, UserRole.CUSTOMER);      // token cũ hết hạn theo đồng hồ test

        JsonNode available = get_("/api/v1/me/vouchers", me).body();
        assertThat(available.get("data")).extracting(v -> v.get("id").asLong()).containsExactly(shop, soon, avail);   // sau 2 ngày cả hai đều đang chạy, cùng hạn → id mới trước
        assertThat(available.at("/summary/counts/ALL").asInt()).isEqualTo(3);
        assertThat(available.at("/summary/counts/FREESHIP").asInt()).isEqualTo(1);
        assertThat(available.at("/summary/counts/SHOP").asInt()).isEqualTo(1);
        assertThat(get_("/api/v1/me/vouchers?type=FREESHIP", me).body().get("data")).hasSize(1);
        assertThat(get_("/api/v1/me/vouchers?type=PLATFORM", me).body().at("/pagination/total_items").asInt()).isEqualTo(2);

        JsonNode used = get_("/api/v1/me/vouchers?state=USED", me).body().get("data");
        assertThat(used).hasSize(1);
        assertThat(used.get(0).get("state").asText()).isEqualTo("USED");
        assertThat(used.get(0).get("used_order_code").asText()).startsWith("TR-W");
        JsonNode exp = get_("/api/v1/me/vouchers?state=EXPIRED", me).body().get("data");
        assertThat(exp).extracting(v -> v.get("id").asLong()).containsExactly(expired);
        assertThat(exp.get(0).get("state").asText()).isEqualTo("EXPIRED");
        assertThat(get_("/api/v1/me/vouchers?state=BAD", me).status()).isEqualTo(422);

        assertThat(delete_("/api/v1/me/vouchers/" + avail, me).status()).isEqualTo(204);
        assertThat(delete_("/api/v1/me/vouchers/" + avail, me).status()).isEqualTo(204);
        assertThat(get_("/api/v1/me/vouchers", me).body().at("/summary/counts/ALL").asInt()).isEqualTo(2);
    }

    @Test
    void internalAccountsHaveNoWallet() throws Exception {
        String staffAuth = auth(staff("MANAGE_VOUCHER"), UserRole.STAFF);
        Res r = get_("/api/v1/me/vouchers", staffAuth);
        assertThat(r.status()).isEqualTo(403);
        assertThat(r.code()).isEqualTo("GNE-ORD-4031");
        assertThat(post_("/api/v1/me/vouchers", staffAuth, "{\"code\":\"ABCDEF\"}").code()).isEqualTo("GNE-ORD-4031");
        assertThat(get_("/api/v1/me/vouchers", null).status()).isEqualTo(401);
        assertThat(get_("/api/v1/public/vouchers?featured=true", staffAuth).status()).isEqualTo(200);
    }
}
