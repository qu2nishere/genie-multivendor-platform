// Ba cài đặt cổng chat ở vendor / catalog / order (plan 2026-11-26 chat M02.03–M02.05, M02.07)
package com.genie.social.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.social.chat.application.ChatOrderLookup;
import com.genie.social.chat.application.ChatProductLookup;
import com.genie.social.chat.application.ChatShopDirectory;
import com.genie.support.TestDataBuilder;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ChatPortsIT extends ChatTestSupport {

    @Autowired
    ChatShopDirectory shops;

    @Autowired
    ChatProductLookup products;

    @Autowired
    ChatOrderLookup orders;

    @Test
    void realImplementationsReplaceTheDefaults() {
        assertThat(shops.getClass().getName()).contains("vendor.shop.application.ChatShopDirectoryImpl");
        assertThat(products.getClass().getName()).contains("catalog.product.application.ChatProductLookupImpl");
        assertThat(orders.getClass().getName()).contains("order.order.application.ChatOrderLookupImpl");
    }

    @Test
    void shopDirectoryMapsBothIdsAutoReplyAndActiveFlag() {
        TestDataBuilder.Shop shop = shop();
        jdbc.update("UPDATE vendor_profiles SET auto_reply_enabled = TRUE, auto_reply_greeting = 'Xin chào!', "
                + "auto_reply_offline = 'Shop sẽ trả lời sớm', vacation_message = 'Nghỉ Tết' WHERE id = ?", shop.shopId());

        var found = shops.find(shop.shopId()).orElseThrow();
        assertThat(found.shopId()).isEqualTo(shop.shopId());
        assertThat(found.vendorUserId()).isEqualTo(shop.userId());
        assertThat(found.name()).isEqualTo(shop.name());
        assertThat(found.active()).isTrue();
        assertThat(found.autoReplyEnabled()).isTrue();
        assertThat(found.greeting()).isEqualTo("Xin chào!");
        assertThat(found.awayMessage()).isEqualTo("Shop sẽ trả lời sớm");
        assertThat(found.vacationMessage()).isEqualTo("Nghỉ Tết");
        assertThat(found.chatResponseRate()).isNotNull();
        assertThat(found.onVacation(OffsetDateTime.parse(START))).isFalse();

        assertThat(shops.byVendorUsers(List.of(shop.userId(), -1L))).containsOnlyKeys(shop.userId());
        assertThat(shops.byVendorUser(shop.userId())).map(ChatShopDirectory.ChatShop::shopId).contains(shop.shopId());
        assertThat(shops.find(-1L)).isEmpty();
    }

    @Test
    void shopIsInactiveWhenNotVerifiedOrOwnerSuspended() {
        TestDataBuilder.Shop pending = data.shop().kyc("PENDING").create();
        TestDataBuilder.Shop suspended = data.shop().userStatus("SUSPENDED").create();
        assertThat(shops.find(pending.shopId()).orElseThrow().active()).isFalse();
        assertThat(shops.find(suspended.shopId()).orElseThrow().active()).isFalse();
        assertThat(shops.byVendorUsers(List.of(suspended.userId())).get(suspended.userId()).active()).isFalse();
    }

    @Test
    void vacationWindowIsReadFromTheProfile() {
        OffsetDateTime now = OffsetDateTime.parse(START);
        TestDataBuilder.Shop shop = data.shop().vacation(now.minusDays(1), now.plusDays(2)).create();
        var found = shops.find(shop.shopId()).orElseThrow();
        assertThat(found.onVacation(now)).isTrue();
        assertThat(found.onVacation(now.plusDays(3))).isFalse();
    }

    @Test
    void productResolveDefaultsToCheapestSellableVariantAndChecksOwnership() {
        TestDataBuilder.Shop shop = shop();
        long productId = data.product(shop.userId(), "Tai nghe " + TestDataBuilder.uniqueWord(), "PUBLISHED", 300_000);
        long expensive = data.variant(productId, 300_000, 5);
        long cheapOut = data.variant(productId, 100_000, 0);
        long cheap = data.variant(productId, 200_000, 5);

        var ref = products.resolve(productId, null).orElseThrow();
        assertThat(ref.variantId()).isEqualTo(cheap);   // còn hàng trước, rồi giá thấp
        assertThat(ref.vendorUserId()).isEqualTo(shop.userId());
        assertThat(ref.published()).isTrue();
        assertThat(products.resolve(productId, expensive).orElseThrow().variantId()).isEqualTo(expensive);
        assertThat(products.resolve(productId, cheapOut).orElseThrow().variantId()).isEqualTo(cheapOut);

        long other = data.product(shop.userId(), "Khác", "PUBLISHED", 1_000);
        long otherVariant = data.variant(other, 1_000, 1);
        assertThat(products.resolve(productId, otherVariant).orElseThrow().variantId()).isNull();
        assertThat(products.resolve(-5L, null)).isEmpty();

        long hidden = data.product(shop.userId(), "Ẩn", "HIDDEN", 1_000);
        data.variant(hidden, 1_000, 1);
        assertThat(products.resolve(hidden, null).orElseThrow().published()).isFalse();
    }

    @Test
    void productCardsReflectCurrentState() {
        TestDataBuilder.Shop shop = shop();
        long[] p = product(shop, 199_000);
        var card = products.cards(List.of(p[1])).get(p[1]);
        assertThat(card.productId()).isEqualTo(p[0]);
        assertThat(card.price()).isEqualTo(199_000);
        assertThat(card.available()).isTrue();
        assertThat(card.slug()).isNotBlank();
        assertThat(card.vendorUserId()).isEqualTo(shop.userId());

        data.setStatus(p[0], "HIDDEN");
        assertThat(products.cards(List.of(p[1])).get(p[1]).available()).isFalse();
        assertThat(products.cards(List.of())).isEmpty();
    }

    @Test
    void orderLookupReturnsOnlyTheExactPairNewestFirst() {
        TestDataBuilder.Shop shop = shop();
        TestDataBuilder.Shop otherShop = shop();
        long buyer = customer();
        long stranger = customer();
        long[] p = product(shop, 150_000);
        long[] q = product(otherShop, 90_000);
        long older = order(buyer, shop.userId(), p[1], 150_000, "2026-06-01T10:00:00Z");
        long newer = order(buyer, shop.userId(), p[1], 300_000, "2026-06-05T10:00:00Z");
        order(stranger, shop.userId(), p[1], 150_000, "2026-06-06T10:00:00Z");
        order(buyer, otherShop.userId(), q[1], 90_000, "2026-06-07T10:00:00Z");

        List<ChatOrderLookup.OrderCard> recent = orders.recentBetween(buyer, shop.userId(), 10);
        assertThat(recent).extracting(ChatOrderLookup.OrderCard::id).containsExactly(newer, older);
        ChatOrderLookup.OrderCard first = recent.get(0);
        assertThat(first.total()).isEqualTo(300_000);
        assertThat(first.itemCount()).isEqualTo(1);
        assertThat(first.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(first.code()).startsWith("TR-");
        assertThat(orders.recentBetween(buyer, shop.userId(), 1)).hasSize(1);

        assertThat(orders.belongs(older, buyer, shop.userId())).isTrue();
        assertThat(orders.belongs(older, stranger, shop.userId())).isFalse();
        assertThat(orders.belongs(older, buyer, otherShop.userId())).isFalse();
        assertThat(orders.belongs(-1L, buyer, shop.userId())).isFalse();
        assertThat(orders.cards(List.of(older, newer))).containsOnlyKeys(older, newer);
    }
}
