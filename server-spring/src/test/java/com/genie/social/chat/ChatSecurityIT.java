// Ma trận quyền + REST hai phía + badge Seller (plan 2026-11-26 chat M05.04)
package com.genie.social.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.common.domain.UserRole;
import com.genie.support.TestDataBuilder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class ChatSecurityIT extends ChatTestSupport {

    @Autowired
    ObjectMapper mapper;

    private MvcResult call(MockHttpServletRequestBuilder req, String auth, String body) throws Exception {
        if (auth != null) {
            req.header(HttpHeaders.AUTHORIZATION, auth);
        }
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(req).andReturn();
    }

    private JsonNode jsonOf(MvcResult r) throws Exception {
        return mapper.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private long openAndSend(long buyer, TestDataBuilder.Shop shop, String text) throws Exception {
        MvcResult opened = call(post("/api/v1/me/conversations"), customerAuth(buyer),
                "{\"shop_id\":" + shop.shopId() + "}");
        long conv = jsonOf(opened).at("/data/id").asLong();
        call(post("/api/v1/me/conversations/" + conv + "/messages"), customerAuth(buyer),
                "{\"type\":\"TEXT\",\"text\":\"" + text + "\"}");
        return conv;
    }

    @Test
    void buyerFlowOverRest() throws Exception {
        TestDataBuilder.Shop shop = shop();
        long buyer = customer();
        MvcResult first = call(post("/api/v1/me/conversations"), customerAuth(buyer), "{\"shop_id\":" + shop.shopId() + "}");
        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        JsonNode opened = jsonOf(first).get("data");
        assertThat(opened.at("/counterpart/type").asText()).isEqualTo("SHOP");
        assertThat(opened.at("/counterpart/id").asLong()).isEqualTo(shop.shopId());
        assertThat(opened.has("shop_stats")).isTrue();
        assertThat(opened.at("/shop_stats/chat_response_rate").isNumber()).isTrue();
        long conv = opened.get("id").asLong();
        assertThat(call(post("/api/v1/me/conversations"), customerAuth(buyer), "{\"shop_id\":" + shop.shopId() + "}")
                .getResponse().getStatus()).isEqualTo(200);

        String cmid = UUID.randomUUID().toString();
        String body = "{\"type\":\"TEXT\",\"text\":\"Còn hàng không?\",\"client_message_id\":\"" + cmid + "\"}";
        MvcResult sent = call(post("/api/v1/me/conversations/" + conv + "/messages"), customerAuth(buyer), body);
        assertThat(sent.getResponse().getStatus()).isEqualTo(201);
        JsonNode m = jsonOf(sent).get("data");
        assertThat(m.get("sender").asText()).isEqualTo("ME");
        assertThat(m.get("status").asText()).isEqualTo("DELIVERED");
        assertThat(m.get("client_message_id").asText()).isEqualTo(cmid);
        assertThat(m.has("product") && m.get("product").isNull()).isTrue();
        assertThat(m.get("is_auto").asBoolean()).isFalse();
        MvcResult resent = call(post("/api/v1/me/conversations/" + conv + "/messages"), customerAuth(buyer), body);
        assertThat(resent.getResponse().getStatus()).isEqualTo(200);
        assertThat(jsonOf(resent).at("/data/id").asLong()).isEqualTo(m.get("id").asLong());

        JsonNode inbox = jsonOf(call(get("/api/v1/me/conversations"), customerAuth(buyer), null));
        assertThat(inbox.at("/data/0/id").asLong()).isEqualTo(conv);
        assertThat(inbox.at("/data/0/last_message/preview").asText()).isEqualTo("Còn hàng không?");
        assertThat(inbox.at("/summary/unread_total").asLong()).isZero();
        assertThat(inbox.at("/pagination/has_more").asBoolean()).isFalse();

        JsonNode messages = jsonOf(call(get("/api/v1/me/conversations/" + conv + "/messages?limit=10"),
                customerAuth(buyer), null));
        assertThat(messages.get("data")).hasSize(1);
        assertThat(call(post("/api/v1/me/conversations/" + conv + "/read"), customerAuth(buyer), null)
                .getResponse().getStatus()).isEqualTo(204);
        assertThat(call(get("/api/v1/me/conversations/" + conv + "/orders"), customerAuth(buyer), null)
                .getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void openErrors() throws Exception {
        long buyer = customer();
        TestDataBuilder.Shop mine = shop();
        TestDataBuilder.Shop pending = data.shop().kyc("PENDING").create();
        assertThat(jsonOf(call(post("/api/v1/me/conversations"), vendorAuth(mine.userId()),
                "{\"shop_id\":" + mine.shopId() + "}")).get("code").asText()).isEqualTo("GNE-SOC-4096");
        MvcResult unavailable = call(post("/api/v1/me/conversations"), customerAuth(buyer),
                "{\"shop_id\":" + pending.shopId() + "}");
        assertThat(unavailable.getResponse().getStatus()).isEqualTo(404);
        assertThat(jsonOf(unavailable).get("code").asText()).isEqualTo("GNE-VND-4041");
        assertThat(call(post("/api/v1/me/conversations"), customerAuth(buyer), "{}").getResponse().getStatus())
                .isEqualTo(422);
    }

    @Test
    void staffAndAnonymousAreRejected() throws Exception {
        TestDataBuilder.Shop shop = shop();
        long staff = customer();
        jdbc.update("UPDATE users SET role = 'STAFF' WHERE id = ?", staff);
        String staffAuth = auth(staff, UserRole.STAFF);
        MvcResult forbidden = call(post("/api/v1/me/conversations"), staffAuth, "{\"shop_id\":" + shop.shopId() + "}");
        assertThat(forbidden.getResponse().getStatus()).isEqualTo(403);
        assertThat(jsonOf(forbidden).get("code").asText()).isEqualTo("GNE-SEC-4030");
        assertThat(call(get("/api/v1/me/conversations"), staffAuth, null).getResponse().getStatus()).isEqualTo(403);
        assertThat(call(get("/api/v1/seller/conversations"), staffAuth, null).getResponse().getStatus()).isEqualTo(403);
        assertThat(call(get("/api/v1/me/conversations"), null, null).getResponse().getStatus()).isEqualTo(401);
        assertThat(call(get("/api/v1/seller/conversations"), null, null).getResponse().getStatus()).isEqualTo(401);
        // khách thuần không vào được phía shop
        assertThat(call(get("/api/v1/seller/conversations"), customerAuth(customer()), null).getResponse().getStatus())
                .isEqualTo(403);
    }

    @Test
    void sellerSideSeesOnlyItsOwnConversationsAndTheBadgeCounts() throws Exception {
        TestDataBuilder.Shop shopA = shop();
        TestDataBuilder.Shop shopB = shop();
        long buyer = customer();
        long convA = openAndSend(buyer, shopA, "Hỏi shop A");
        openAndSend(buyer, shopA, "Hỏi lại shop A");
        long convB = openAndSend(buyer, shopB, "Hỏi shop B");

        JsonNode inboxA = jsonOf(call(get("/api/v1/seller/conversations"), vendorAuth(shopA.userId()), null));
        assertThat(inboxA.get("data")).hasSize(1);
        assertThat(inboxA.at("/data/0/counterpart/type").asText()).isEqualTo("CUSTOMER");
        assertThat(inboxA.at("/data/0/counterpart/id").asLong()).isEqualTo(buyer);
        assertThat(inboxA.at("/data/0/unread_count").asInt()).isEqualTo(2);
        assertThat(inboxA.at("/summary/unread_total").asLong()).isEqualTo(2);

        JsonNode me = jsonOf(call(get("/api/v1/seller/me"), vendorAuth(shopA.userId()), null));
        assertThat(me.at("/data/badges/unread_chats").asLong()).isEqualTo(2);

        // shop A không đọc / gửi / đánh dấu hội thoại của shop B
        assertThat(call(get("/api/v1/seller/conversations/" + convB + "/messages"), vendorAuth(shopA.userId()), null)
                .getResponse().getStatus()).isEqualTo(404);
        assertThat(call(post("/api/v1/seller/conversations/" + convB + "/messages"), vendorAuth(shopA.userId()),
                "{\"type\":\"TEXT\",\"text\":\"chen\"}").getResponse().getStatus()).isEqualTo(404);
        assertThat(call(post("/api/v1/seller/conversations/" + convB + "/read"), vendorAuth(shopA.userId()), null)
                .getResponse().getStatus()).isEqualTo(404);

        MvcResult reply = call(post("/api/v1/seller/conversations/" + convA + "/messages"), vendorAuth(shopA.userId()),
                "{\"type\":\"TEXT\",\"text\":\"Dạ còn ạ\"}");
        assertThat(reply.getResponse().getStatus()).isEqualTo(201);
        assertThat(call(post("/api/v1/seller/conversations/" + convA + "/read"), vendorAuth(shopA.userId()), null)
                .getResponse().getStatus()).isEqualTo(204);
        assertThat(jsonOf(call(get("/api/v1/seller/me"), vendorAuth(shopA.userId()), null))
                .at("/data/badges/unread_chats").asLong()).isZero();
        JsonNode buyerView = jsonOf(call(get("/api/v1/me/conversations/" + convA + "/messages"), customerAuth(buyer), null));
        assertThat(buyerView.at("/data/0/sender").asText()).isEqualTo("THEM");
        assertThat(buyerView.at("/data/1/status").asText()).isEqualTo("SEEN");
    }

    @Test
    void unverifiedShopIsStoppedByTheKycGate() throws Exception {
        TestDataBuilder.Shop pending = data.shop().kyc("PENDING").create();
        MvcResult r = call(get("/api/v1/seller/conversations"), vendorAuth(pending.userId()), null);
        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(jsonOf(r).get("code").asText()).isEqualTo("GNE-VND-4031");
    }
}
