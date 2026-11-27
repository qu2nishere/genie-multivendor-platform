// Ảnh, thẻ sản phẩm, thẻ đơn + ô chọn đơn 8.6 (plan 2026-11-26 chat M07.04; UC-SOC-02)
package com.genie.social.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.support.TestDataBuilder;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class ChatCardsIT extends ChatTestSupport {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
    private final HttpClient http = HttpClient.newHttpClient();

    @Autowired
    ObjectMapper mapper;

    private JsonNode call(String method, String path, String auth, String body, int expectedStatus) throws Exception {
        var req = "POST".equals(method) ? post(path) : get(path);
        req.header(HttpHeaders.AUTHORIZATION, auth);
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        MvcResult r = mvc.perform(req).andReturn();
        String content = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(r.getResponse().getStatus()).as(content).isEqualTo(expectedStatus);
        return content.isEmpty() ? null : mapper.readTree(content);
    }

    private long open(long buyer, TestDataBuilder.Shop shop) throws Exception {
        return call("POST", "/api/v1/me/conversations", customerAuth(buyer), "{\"shop_id\":" + shop.shopId() + "}", 201)
                .at("/data/id").asLong();
    }

    private JsonNode sendAs(long buyer, long conv, String body, int status) throws Exception {
        return call("POST", "/api/v1/me/conversations/" + conv + "/messages", customerAuth(buyer), body, status);
    }

    /** Xin link CHAT_IMAGE + PUT ảnh lên Silo như trình duyệt; trả khóa {@code tmp/…}. */
    private String uploadImage(String auth) throws Exception {
        JsonNode issued = call("POST", "/api/v1/uploads/presigned-urls", auth,
                "{\"purpose\":\"CHAT_IMAGE\",\"content_type\":\"image/png\",\"content_length\":" + PNG.length + "}", 200);
        HttpRequest put = HttpRequest.newBuilder(URI.create(issued.at("/data/upload_url").asText()))
                .header("Content-Type", "image/png").PUT(HttpRequest.BodyPublishers.ofByteArray(PNG)).build();
        assertThat(http.send(put, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(200);
        return issued.at("/data/object_key").asText();
    }

    private static boolean inSilo(String key) throws Exception {
        try {
            siloClient().statObject(StatObjectArgs.builder().bucket("chat-media").object(key).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        }
    }

    // ===== Ảnh (D7) =====

    @Test
    void imageIsPromotedIntoThePrivateBucketAndServedThroughASignedLink() throws Exception {
        TestDataBuilder.Shop shop = shop();
        long buyer = customer();
        long conv = open(buyer, shop);
        String tmp = uploadImage(customerAuth(buyer));
        assertThat(tmp).startsWith("tmp/user/" + buyer + "/");

        JsonNode m = sendAs(buyer, conv, "{\"type\":\"IMAGE\",\"image_object_key\":\"" + tmp + "\"}", 201).get("data");
        assertThat(m.get("type").asText()).isEqualTo("IMAGE");
        String url = m.get("image_url").asText();
        assertThat(url).contains("chat-media").contains("X-Amz-Signature");
        String stored = jdbc.queryForObject("SELECT attachment_url FROM messages WHERE id = ?", String.class,
                m.get("id").asLong());
        assertThat(stored).startsWith("conversation/" + conv + "/");
        assertThat(inSilo(stored)).isTrue();
        assertThat(inSilo(tmp)).isFalse();
        HttpResponse<byte[]> download = http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(download.statusCode()).isEqualTo(200);
        assertThat(download.body()).isEqualTo(PNG);

        // shop đọc lịch sử cũng nhận link ký (tạo lại mỗi lần đọc)
        JsonNode seen = call("GET", "/api/v1/seller/conversations/" + conv + "/messages", vendorAuth(shop.userId()),
                null, 200);
        assertThat(seen.at("/data/0/image_url").asText()).contains("X-Amz-Signature");
        // dòng hộp thư: ảnh không có chữ xem trước
        JsonNode inbox = call("GET", "/api/v1/seller/conversations", vendorAuth(shop.userId()), null, 200);
        assertThat(inbox.at("/data/0/last_message/type").asText()).isEqualTo("IMAGE");
        assertThat(inbox.at("/data/0/last_message/preview").isNull()).isTrue();
    }

    @Test
    void someoneElsesTmpKeyAndBadUploadsAreRejected() throws Exception {
        TestDataBuilder.Shop shop = shop();
        long buyer = customer();
        long other = customer();
        long conv = open(buyer, shop);
        String theirs = uploadImage(customerAuth(other));
        sendAs(buyer, conv, "{\"type\":\"IMAGE\",\"image_object_key\":\"" + theirs + "\"}", 422);
        sendAs(buyer, conv, "{\"type\":\"IMAGE\"}", 422);
        assertThat(inSilo(theirs)).isTrue();   // không đụng tệp của người khác

        call("POST", "/api/v1/uploads/presigned-urls", customerAuth(buyer),
                "{\"purpose\":\"CHAT_IMAGE\",\"content_type\":\"video/mp4\",\"content_length\":1000}", 422);
        call("POST", "/api/v1/uploads/presigned-urls", customerAuth(buyer),
                "{\"purpose\":\"CHAT_IMAGE\",\"content_type\":\"image/png\",\"content_length\":" + (5 * 1024 * 1024 + 1)
                        + "}", 422);
    }

    // ===== Thẻ sản phẩm (D8) =====

    @Test
    void productCardMustBelongToTheConversationShopAndShowsCurrentState() throws Exception {
        TestDataBuilder.Shop shop = shop();
        TestDataBuilder.Shop otherShop = shop();
        long buyer = customer();
        long conv = open(buyer, shop);
        long[] mine = product(shop, 389_000);
        long[] foreign = product(otherShop, 99_000);
        long extraVariant = data.variant(mine[0], 350_000, 3);

        JsonNode card = sendAs(buyer, conv, "{\"type\":\"PRODUCT_CARD\",\"product_id\":" + mine[0] + "}", 201)
                .at("/data/product");
        assertThat(card.get("id").asLong()).isEqualTo(mine[0]);
        assertThat(card.get("variant_id").asLong()).isEqualTo(extraVariant);   // biến thể còn bán giá thấp nhất
        assertThat(card.get("price").asLong()).isEqualTo(350_000);
        assertThat(card.get("is_available").asBoolean()).isTrue();
        assertThat(card.get("slug").asText()).isNotBlank();
        JsonNode chosen = sendAs(buyer, conv, "{\"type\":\"PRODUCT_CARD\",\"product_id\":" + mine[0] + ",\"variant_id\":"
                + mine[1] + "}", 201).at("/data/product");
        assertThat(chosen.get("variant_id").asLong()).isEqualTo(mine[1]);

        JsonNode forbidden = sendAs(buyer, conv, "{\"type\":\"PRODUCT_CARD\",\"product_id\":" + foreign[0] + "}", 403);
        assertThat(forbidden.get("code").asText()).isEqualTo("GNE-SOC-4031");
        sendAs(buyer, conv, "{\"type\":\"PRODUCT_CARD\",\"product_id\":999999999}", 403);
        sendAs(buyer, conv, "{\"type\":\"PRODUCT_CARD\",\"product_id\":" + mine[0] + ",\"variant_id\":" + foreign[1]
                + "}", 422);
        sendAs(buyer, conv, "{\"type\":\"PRODUCT_CARD\"}", 422);

        long hidden = data.product(shop.userId(), "Đang ẩn", "HIDDEN", 1_000);
        data.variant(hidden, 1_000, 1);
        sendAs(buyer, conv, "{\"type\":\"PRODUCT_CARD\",\"product_id\":" + hidden + "}", 403);

        // ẩn sản phẩm SAU khi gửi → thẻ vẫn hiện tên, giá null, is_available = false
        data.setStatus(mine[0], "HIDDEN");
        JsonNode history = call("GET", "/api/v1/me/conversations/" + conv + "/messages", customerAuth(buyer), null, 200);
        JsonNode latest = history.at("/data/0/product");
        assertThat(latest.get("is_available").asBoolean()).isFalse();
        assertThat(latest.get("price").isNull()).isTrue();
        assertThat(latest.get("name").asText()).isNotBlank();
    }

    // ===== Thẻ đơn + 8.6 (D9) =====

    @Test
    void orderCardsAndThePickerOnlyCoverTheExactPair() throws Exception {
        TestDataBuilder.Shop shop = shop();
        TestDataBuilder.Shop otherShop = shop();
        long buyer = customer();
        long stranger = customer();
        long conv = open(buyer, shop);
        long[] p = product(shop, 150_000);
        long[] q = product(otherShop, 90_000);
        long older = order(buyer, shop.userId(), p[1], 150_000, "2026-06-01T10:00:00Z");
        long newer = order(buyer, shop.userId(), p[1], 300_000, "2026-06-05T10:00:00Z");
        long strangers = order(stranger, shop.userId(), p[1], 150_000, "2026-06-06T10:00:00Z");
        long elsewhere = order(buyer, otherShop.userId(), q[1], 90_000, "2026-06-07T10:00:00Z");

        JsonNode picker = call("GET", "/api/v1/me/conversations/" + conv + "/orders", customerAuth(buyer), null, 200);
        assertThat(picker.get("data")).hasSize(2);
        assertThat(picker.at("/data/0/id").asLong()).isEqualTo(newer);
        assertThat(picker.at("/data/0/total").asLong()).isEqualTo(300_000);
        assertThat(picker.at("/data/0/item_count").asInt()).isEqualTo(1);
        assertThat(picker.at("/data/1/id").asLong()).isEqualTo(older);
        JsonNode sellerPicker = call("GET", "/api/v1/seller/conversations/" + conv + "/orders?limit=1",
                vendorAuth(shop.userId()), null, 200);
        assertThat(sellerPicker.get("data")).hasSize(1);
        call("GET", "/api/v1/me/conversations/" + conv + "/orders?limit=21", customerAuth(buyer), null, 422);

        JsonNode card = sendAs(buyer, conv, "{\"type\":\"ORDER_CARD\",\"order_id\":" + newer + "}", 201).at("/data/order");
        assertThat(card.get("id").asLong()).isEqualTo(newer);
        assertThat(card.get("code").asText()).startsWith("TR-");
        assertThat(card.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        // shop cũng gửi được thẻ đơn của chính cặp này
        call("POST", "/api/v1/seller/conversations/" + conv + "/messages", vendorAuth(shop.userId()),
                "{\"type\":\"ORDER_CARD\",\"order_id\":" + older + "}", 201);

        assertThat(sendAs(buyer, conv, "{\"type\":\"ORDER_CARD\",\"order_id\":" + strangers + "}", 403).get("code")
                .asText()).isEqualTo("GNE-SOC-4031");
        sendAs(buyer, conv, "{\"type\":\"ORDER_CARD\",\"order_id\":" + elsewhere + "}", 403);
        sendAs(buyer, conv, "{\"type\":\"ORDER_CARD\"}", 422);
    }
}
