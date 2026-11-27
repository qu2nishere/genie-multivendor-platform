// Voucher gian hàng qua API thật (plan 2026-11-26 voucher V03.06; UC-PRO-01; API_SPEC 10.7)
package com.genie.promotion.voucher;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.genie.common.domain.UserRole;
import com.genie.promotion.voucher.application.PromotionSearchPorts;
import com.genie.support.TestDataBuilder;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SellerVoucherIT extends VoucherTestSupport {

    @Autowired
    PromotionSearchPorts searchPorts;

    private TestDataBuilder.Shop shop() {
        return data.shop().create();
    }

    private String vendor(TestDataBuilder.Shop s) {
        return auth(s.userId(), UserRole.VENDOR);
    }

    @Test
    void createsThreeDiscountTypesWithGeneratedTitles() throws Exception {
        TestDataBuilder.Shop s = shop();
        OffsetDateTime from = now().plusHours(1);
        OffsetDateTime to = now().plusDays(10);

        Res pct = post_("/api/v1/seller/vouchers", vendor(s), percentBody(code().toLowerCase(), from, to, null));
        assertThat(pct.status()).isEqualTo(201);
        JsonNode p = pct.data();
        assertThat(p.get("title").asText()).isEqualTo("Giảm 15% tối đa 50.000₫ đơn từ 200.000₫");
        assertThat(p.get("code").asText()).isUpperCase();
        assertThat(p.get("type").asText()).isEqualTo("SHOP");
        assertThat(p.get("funded_by").asText()).isEqualTo("VENDOR");
        assertThat(p.get("status").asText()).isEqualTo("SCHEDULED");
        assertThat(p.get("discount_value").asInt()).isEqualTo(15);
        assertThat(p.get("locked_fields")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT vendor_id FROM vouchers WHERE id = ?", Long.class, p.get("id").asLong()))
                .isEqualTo(s.userId());

        Res fixed = post_("/api/v1/seller/vouchers", vendor(s), "{\"code\":\"" + code() + "\",\"discount_type\":\"FIXED_AMOUNT\","
                + "\"discount_value\":30000,\"min_order_value\":399000,\"usage_limit\":100,\"usage_per_user\":2,"
                + "\"valid_from\":\"" + now() + "\",\"valid_to\":\"" + to + "\"}");
        assertThat(fixed.status()).isEqualTo(201);
        assertThat(fixed.data().get("title").asText()).isEqualTo("Giảm 30.000₫ đơn từ 399.000₫");
        assertThat(fixed.data().get("status").asText()).isEqualTo("ACTIVE");
        assertThat(fixed.data().get("max_discount").isNull()).isTrue();

        Res ship = post_("/api/v1/seller/vouchers", vendor(s), "{\"code\":\"" + code() + "\",\"discount_type\":\"FREESHIP\","
                + "\"max_discount\":30000,\"min_order_value\":99000,\"usage_limit\":100,\"usage_per_user\":1,"
                + "\"valid_from\":\"" + from + "\",\"valid_to\":\"" + to + "\"}");
        assertThat(ship.status()).isEqualTo(201);
        assertThat(ship.data().get("title").asText()).isEqualTo("Freeship tối đa 30.000₫ đơn từ 99.000₫");
        assertThat(ship.data().get("type").asText()).isEqualTo("SHOP");
        assertThat(ship.data().get("discount_value").isNull()).isTrue();
    }

    @Test
    void scopesAreStoredAndProductsMustBelongToTheShop() throws Exception {
        TestDataBuilder.Shop s = shop();
        TestDataBuilder.Shop other = shop();
        long cat = category();
        long excluded = category();
        long mine = data.product(s.userId(), "Tai nghe", "PUBLISHED", 100_000);
        long theirs = data.product(other.userId(), "Của shop khác", "PUBLISHED", 100_000);
        OffsetDateTime from = now().plusHours(1);
        OffsetDateTime to = now().plusDays(10);

        Res byCategory = post_("/api/v1/seller/vouchers", vendor(s), percentBody(code(), from, to,
                "\"scope\":\"CATEGORY\",\"category_ids\":[" + cat + "],\"excluded_category_ids\":[" + excluded + "],"
                        + "\"excluded_product_ids\":[" + mine + "]").replace("\"scope\":\"ALL\",", ""));
        assertThat(byCategory.status()).isEqualTo(201);
        assertThat(byCategory.data().get("category_ids").get(0).asLong()).isEqualTo(cat);
        assertThat(byCategory.data().get("excluded_category_ids").get(0).asLong()).isEqualTo(excluded);
        assertThat(byCategory.data().get("excluded_product_ids").get(0).asLong()).isEqualTo(mine);

        Res byProduct = post_("/api/v1/seller/vouchers", vendor(s), percentBody(code(), from, to,
                "\"product_ids\":[" + mine + "]").replace("\"scope\":\"ALL\"", "\"scope\":\"PRODUCT\""));
        assertThat(byProduct.status()).isEqualTo(201);
        assertThat(byProduct.data().get("product_ids").get(0).asLong()).isEqualTo(mine);

        Res foreign = post_("/api/v1/seller/vouchers", vendor(s), percentBody(code(), from, to,
                "\"product_ids\":[" + mine + "," + theirs + "]").replace("\"scope\":\"ALL\"", "\"scope\":\"PRODUCT\""));
        assertThat(foreign.status()).isEqualTo(422);
        assertThat(foreign.body().get("errors").get(0).get("field").asText()).isEqualTo("product_ids");

        Res unknownCat = post_("/api/v1/seller/vouchers", vendor(s), percentBody(code(), from, to,
                "\"category_ids\":[999999999]").replace("\"scope\":\"ALL\"", "\"scope\":\"CATEGORY\""));
        assertThat(unknownCat.status()).isEqualTo(422);
    }

    @Test
    void duplicateCodesAreRejectedIncludingCaseAndRaces() throws Exception {
        TestDataBuilder.Shop s = shop();
        String c = code();
        OffsetDateTime from = now().plusHours(1);
        OffsetDateTime to = now().plusDays(10);
        assertThat(post_("/api/v1/seller/vouchers", vendor(s), percentBody(c, from, to, null)).status()).isEqualTo(201);
        Res dup = post_("/api/v1/seller/vouchers", vendor(shop()), percentBody(c.toLowerCase(), from, to, null));
        assertThat(dup.status()).isEqualTo(409);
        assertThat(dup.code()).isEqualTo("GNE-VCH-4092");

        String race = code();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Res>> fs = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                fs.add(pool.submit(() -> post_("/api/v1/seller/vouchers", vendor(s), percentBody(race, from, to, null))));
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<Res> f : fs) statuses.add(f.get().status());
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void editRulesDependOnStatusAndSavers() throws Exception {
        TestDataBuilder.Shop s = shop();
        OffsetDateTime to = now().plusDays(10);
        long scheduled = post_("/api/v1/seller/vouchers", vendor(s), percentBody(code(), now().plusDays(1), to, null))
                .data().get("id").asLong();
        String newCode = code();
        Res free = put_("/api/v1/seller/vouchers/" + scheduled, vendor(s), "{\"code\":\"" + newCode + "\","
                + "\"discount_type\":\"FIXED_AMOUNT\",\"discount_value\":20000,\"min_order_value\":100000,\"usage_limit\":50,"
                + "\"usage_per_user\":1,\"valid_from\":\"" + now().plusDays(2) + "\",\"valid_to\":\"" + to + "\"}");
        assertThat(free.status()).isEqualTo(200);
        assertThat(free.data().get("code").asText()).isEqualTo(newCode);
        assertThat(free.data().get("title").asText()).isEqualTo("Giảm 20.000₫ đơn từ 100.000₫");

        String runningCode = code();
        long running = post_("/api/v1/seller/vouchers", vendor(s), percentBody(runningCode, now(), to, null))
                .data().get("id").asLong();
        JsonNode detail = get_("/api/v1/seller/vouchers/" + running, vendor(s)).data();
        assertThat(detail.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(detail.get("locked_fields")).extracting(JsonNode::asText).contains("discount_value", "code")
                .doesNotContain("valid_to", "usage_limit");

        Res lockedRes = put_("/api/v1/seller/vouchers/" + running, vendor(s),
                percentBody(runningCode, now(), to, null).replace("\"discount_value\":15", "\"discount_value\":20"));
        assertThat(lockedRes.status()).isEqualTo(409);
        assertThat(lockedRes.code()).isEqualTo("GNE-VCH-4094");
        assertThat(lockedRes.body().at("/details/locked_fields")).extracting(JsonNode::asText).containsExactly("discount_value");

        Res extended = put_("/api/v1/seller/vouchers/" + running, vendor(s),
                percentBody(runningCode, now(), to.plusDays(5), null).replace("\"usage_limit\":500", "\"usage_limit\":900"));
        assertThat(extended.status()).isEqualTo(200);
        assertThat(extended.data().get("usage").get("limit").asInt()).isEqualTo(900);

        // sắp diễn ra nhưng đã có người lưu → khóa như đang chạy
        String savedCode = code();
        long saved = post_("/api/v1/seller/vouchers", vendor(s), percentBody(savedCode, now().plusDays(1), to, null))
                .data().get("id").asLong();
        jdbc.update("INSERT INTO user_vouchers (user_id, voucher_id, created_at) VALUES (?, ?, now())", customer(), saved);
        Res savedEdit = put_("/api/v1/seller/vouchers/" + saved, vendor(s),
                percentBody(savedCode, now().plusDays(1), to, null).replace("\"min_order_value\":200000", "\"min_order_value\":100000"));
        assertThat(savedEdit.status()).isEqualTo(409);
    }

    @Test
    void disableIsOneWayAndOwnershipIsEnforced() throws Exception {
        TestDataBuilder.Shop s = shop();
        TestDataBuilder.Shop b = shop();
        String c = code();
        long id = post_("/api/v1/seller/vouchers", vendor(s), percentBody(c, now(), now().plusDays(5), null)).data().get("id").asLong();

        assertThat(get_("/api/v1/seller/vouchers/" + id, vendor(b)).status()).isEqualTo(404);
        assertThat(put_("/api/v1/seller/vouchers/" + id, vendor(b), percentBody(c, now(), now().plusDays(6), null)).status())
                .isEqualTo(404);
        assertThat(patch_("/api/v1/seller/vouchers/" + id, vendor(b), "{\"is_active\":false}").status()).isEqualTo(404);

        Res off = patch_("/api/v1/seller/vouchers/" + id, vendor(s), "{\"is_active\":false}");
        assertThat(off.status()).isEqualTo(200);
        assertThat(off.data().get("status").asText()).isEqualTo("DISABLED");
        assertThat(patch_("/api/v1/seller/vouchers/" + id, vendor(s), "{\"is_active\":false}").status()).isEqualTo(200);
        assertThat(patch_("/api/v1/seller/vouchers/" + id, vendor(s), "{\"is_active\":true}").status()).isEqualTo(422);
        Res editOff = put_("/api/v1/seller/vouchers/" + id, vendor(s), percentBody(c, now(), now().plusDays(6), null));
        assertThat(editOff.status()).isEqualTo(409);
        assertThat(editOff.body().at("/details/locked_fields/0").asText()).isEqualTo("status");
    }

    @Test
    void listFiltersByStatusAndSummaryCountsThisMonth() throws Exception {
        TestDataBuilder.Shop s = shop();
        OffsetDateTime to = now().plusDays(5);
        long active = post_("/api/v1/seller/vouchers", vendor(s), percentBody(code(), now(), to, null)).data().get("id").asLong();
        post_("/api/v1/seller/vouchers", vendor(s), percentBody(code(), now().plusDays(1), to, null));
        long off = post_("/api/v1/seller/vouchers", vendor(s), percentBody(code(), now(), to, null)).data().get("id").asLong();
        patch_("/api/v1/seller/vouchers/" + off, vendor(s), "{\"is_active\":false}");
        post_("/api/v1/seller/vouchers", vendor(shop()), percentBody(code(), now(), to, null));   // shop khác

        long buyer = customer();
        for (int i = 0; i < 3; i++) {
            usage(active, buyer, now().minusHours(i), null);
        }

        Res all = get_("/api/v1/seller/vouchers", vendor(s));
        assertThat(all.body().get("data")).hasSize(3);
        assertThat(all.body().at("/pagination/total_items").asInt()).isEqualTo(3);
        assertThat(all.body().at("/summary/active_count").asInt()).isEqualTo(1);
        assertThat(all.body().at("/summary/usage_this_month").asInt()).isEqualTo(3);
        assertThat(all.body().at("/summary/revenue_from_promotions_this_month").asLong()).isZero();
        assertThat(get_("/api/v1/seller/vouchers?status=DISABLED", vendor(s)).body().get("data")).hasSize(1);
        assertThat(get_("/api/v1/seller/vouchers?status=SCHEDULED", vendor(s)).body().get("data")).hasSize(1);
        assertThat(get_("/api/v1/seller/vouchers?status=BOGUS", vendor(s)).status()).isEqualTo(422);
        assertThat(get_("/api/v1/seller/vouchers?size=1&page=2", vendor(s)).body().at("/pagination/total_pages").asInt())
                .isEqualTo(3);
    }

    @Test
    void creatingAShopVoucherRefreshesTheShopFilterCacheAfterCommit() throws Exception {
        TestDataBuilder.Shop s = shop();
        assertThat(searchPorts.vendorIdsWithActiveVoucher().orElseThrow()).doesNotContain(s.userId());   // nạp cache
        long id = post_("/api/v1/seller/vouchers", vendor(s), percentBody(code(), now(), now().plusDays(3), null))
                .data().get("id").asLong();
        assertThat(searchPorts.vendorIdsWithActiveVoucher().orElseThrow()).contains(s.userId());
        patch_("/api/v1/seller/vouchers/" + id, vendor(s), "{\"is_active\":false}");
        assertThat(searchPorts.vendorIdsWithActiveVoucher().orElseThrow()).doesNotContain(s.userId());
        // lỗi 422 (rollback) không xóa cache — không có gì để kiểm thêm ngoài việc không ném lỗi
        clock.advance(Duration.ZERO);
    }

    @Test
    void validationErrorsComeBackTogether() throws Exception {
        Res bad = post_("/api/v1/seller/vouchers", vendor(shop()), "{\"code\":\"ab\",\"discount_type\":\"PERCENTAGE\","
                + "\"discount_value\":150,\"min_order_value\":0,\"usage_limit\":0,\"usage_per_user\":1,"
                + "\"valid_from\":\"" + now().minusDays(1) + "\",\"valid_to\":\"" + now().minusDays(2) + "\"}");
        assertThat(bad.status()).isEqualTo(422);
        assertThat(bad.code()).isEqualTo("GNE-VAL-4220");
        assertThat(bad.body().get("errors")).extracting(e -> e.get("field").asText())
                .contains("code", "discount_value", "max_discount", "usage_limit", "valid_from", "valid_to");
    }
}
