package com.genie.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.support.TestDataBuilder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Cài đặt gian hàng — UC-VND-21 (API_SPEC 10.11; plan Mở shop & KYC K09.06–K09.07). */
class ShopSettingsIT extends KycTestSupport {

    private static final String URL = "/api/v1/seller/settings";

    private ResultActions patchSettings(String token, String json) throws Exception {
        return mvc.perform(patch(URL).header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String tmpAvatar(long userId, int size) {
        String key = "tmp/user/" + userId + "/" + UUID.randomUUID() + ".png";
        storage.put("avatar-assets", key, new byte[size], "image/png");
        return key;
    }

    @Test
    void renameAtMostOncePerThirtyDaysAndNameMustBeFree() throws Exception {
        TestDataBuilder data = new TestDataBuilder(jdbc);
        long a = data.vendor();
        long b = data.vendor();
        String bName = jdbc.queryForObject("SELECT shop_name FROM vendor_profiles WHERE user_id = ?", String.class, b);
        String t = vendorToken(a);
        patchSettings(t, "{\"profile\":{\"shop_name\":\"" + bName.toLowerCase() + "\"}}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GNE-VND-4094"));
        String first = "Đổi Tên " + UUID.randomUUID().toString().substring(0, 6);
        patchSettings(t, "{\"profile\":{\"shop_name\":\"" + first + "\"}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.shop_name").value(first))
                .andExpect(jsonPath("$.data.profile.next_rename_allowed_at").isNotEmpty());
        patchSettings(t, "{\"profile\":{\"shop_name\":\"Lần Hai " + UUID.randomUUID().toString().substring(0, 6) + "\"}}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GNE-VND-4091"))
                .andExpect(jsonPath("$.details.next_rename_allowed_at").exists());
        clock.advance(Duration.ofDays(30));
        t = vendorToken(a);                       // token cũ đã hết hạn sau 30 ngày
        patchSettings(t, "{\"profile\":{\"shop_name\":\"Lần Ba " + UUID.randomUUID().toString().substring(0, 6) + "\"}}")
                .andExpect(status().isOk());
        // shop B không bị đổi (người gọi chỉ sửa được shop của mình)
        assertThat(jdbc.queryForObject("SELECT shop_name FROM vendor_profiles WHERE user_id = ?", String.class, b))
                .isEqualTo(bName);
    }

    @Test
    void logoReplacementDeletesOldImageAndCoverSizeIsChecked() throws Exception {
        long v = new TestDataBuilder(jdbc).vendor();
        long shopId = new TestDataBuilder(jdbc).shopIdOf(v);
        String t = vendorToken(v);
        String body = patchSettings(t, "{\"profile\":{\"logo_object_key\":\"" + tmpAvatar(v, 2_000) + "\"}}")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String firstUrl = com.jayway.jsonpath.JsonPath.read(body, "$.data.profile.logo_url");
        assertThat(firstUrl).contains("/avatar-assets/vendor/" + shopId + "/logo/");
        String firstKey = firstUrl.substring(firstUrl.indexOf("vendor/"));
        assertThat(storage.stat("avatar-assets", firstKey)).isPresent();

        patchSettings(t, "{\"profile\":{\"logo_object_key\":\"" + tmpAvatar(v, 3_000) + "\"}}").andExpect(status().isOk());
        assertThat(storage.stat("avatar-assets", firstKey)).isEmpty();   // ảnh cũ đã xóa sau commit

        patchSettings(t, "{\"profile\":{\"cover_object_key\":\"" + tmpAvatar(v, 2 * 1024 * 1024 + 1) + "\"}}")
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shippingAutoReplyAndPartialPatch() throws Exception {
        long v = new TestDataBuilder(jdbc).vendor();
        String t = vendorToken(v);
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shipping.carriers.GHN").value(true))
                .andExpect(jsonPath("$.data.pickup_address").isEmpty())
                .andExpect(jsonPath("$.data.payout_account").isEmpty())
                .andExpect(jsonPath("$.data.vacation").isEmpty());
        patchSettings(t, "{\"shipping\":{\"carriers\":{\"GHN\":false,\"GHTK\":false,\"VIETTEL_POST\":false,"
                + "\"JNT_EXPRESS\":false,\"GENIE_EXPRESS\":false},\"free_shipping_enabled\":false}}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("GNE-VND-4221"));
        patchSettings(t, "{\"shipping\":{\"carriers\":{\"GHN\":true,\"GHTK\":false,\"VIETTEL_POST\":true,"
                + "\"JNT_EXPRESS\":false,\"GENIE_EXPRESS\":false},\"free_shipping_enabled\":true,\"free_shipping_threshold\":99000}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shipping.carriers.VIETTEL_POST").value(true))
                .andExpect(jsonPath("$.data.shipping.free_shipping_threshold").value(99000));
        patchSettings(t, "{\"auto_reply\":{\"enabled\":true,\"greeting\":\" \",\"away_message\":null}}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("GNE-VND-4222"));
        // chỉ gửi pre_order_enabled → các khối khác giữ nguyên
        patchSettings(t, "{\"pre_order_enabled\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pre_order_enabled").value(true))
                .andExpect(jsonPath("$.data.shipping.free_shipping_threshold").value(99000))
                .andExpect(jsonPath("$.data.shipping.carriers.GHTK").value(false));
    }

    @Test
    void unverifiedSellerCanStillOpenSettings() throws Exception {
        long[] s = newShop();
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, vendorToken(s[0]))).andExpect(status().isOk());
        patchSettings(vendorToken(s[0]), "{\"auto_reply\":{\"enabled\":true,\"greeting\":\"Chào bạn\"}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auto_reply.greeting").value("Chào bạn"));
    }
}
