package com.genie.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.support.FakePickupShopRegistrar;
import com.genie.support.TestDataBuilder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID — UC-VND-05 (API_SPEC 10.11; plan Mở shop & KYC K07.05, D16). */
class PickupAddressIT extends KycTestSupport {

    private static final String URL = "/api/v1/seller/pickup-address";
    private static final String HCM_SAI_GON = "{\"phone_number\":\"0912345678\",\"province_id\":79,\"ward_code\":\"26740\","
            + "\"address_line\":\"  Kho TechZone, 88 Nguyễn Văn Linh \"}";

    private ResultActions putPickup(String token, String json) throws Exception {
        return mvc.perform(put(URL).header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    /** Shop đã VERIFIED (dữ liệu dựng sẵn) chưa có kho — trả {vendorUserId, vendorProfileId}. */
    private long[] verifiedShopWithoutPickup() {
        TestDataBuilder data = new TestDataBuilder(jdbc);
        long vendor = data.vendor();
        long shopId = data.shopIdOf(vendor);
        jdbc.update("""
                UPDATE vendor_profiles SET kyc_status = 'VERIFIED', pickup_phone = NULL, pickup_province_id = NULL,
                       pickup_province = NULL, pickup_district_id = NULL, pickup_district = NULL, pickup_ward_code = NULL,
                       pickup_ward = NULL, pickup_address_line = NULL, ghn_shop_id = NULL WHERE id = ?""", shopId);
        return new long[] {vendor, shopId};
    }

    private Map<String, Object> pickupRow(long shopId) {
        return jdbc.queryForMap("SELECT * FROM vendor_profiles WHERE id = ?", shopId);
    }

    @Test
    void savesPickupWithServerNamesAndRegistersWithGhn() throws Exception {
        long[] s = verifiedShopWithoutPickup();
        String t = vendorToken(s[0]);
        putPickup(t, HCM_SAI_GON)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.data.phone_number").value("0912345678"))
                .andExpect(jsonPath("$.data.province.id").value(79))
                .andExpect(jsonPath("$.data.province.name").value("Thành phố Hồ Chí Minh"))   // server tự điền tên
                .andExpect(jsonPath("$.data.ward.code").value("26740"))
                .andExpect(jsonPath("$.data.ward.name").value("Phường Sài Gòn"))
                .andExpect(jsonPath("$.data.address_line").value("Kho TechZone, 88 Nguyễn Văn Linh"))
                .andExpect(jsonPath("$.data.ghn_shop_id").value(770_000 + s[1]))
                .andExpect(jsonPath("$.data.district").doesNotExist());

        Map<String, Object> row = pickupRow(s[1]);
        assertThat(row.get("ghn_shop_id")).isEqualTo((int) (770_000 + s[1]));
        assertThat(row.get("pickup_ward")).isEqualTo("Phường Sài Gòn");
        assertThat(row.get("pickup_district_id")).isNull();
        List<FakePickupShopRegistrar.Call> calls = ghnPickup.callsFor(s[1]);
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).currentGhnShopId()).isNull();
        assertThat(calls.get(0).info().fullAddress())
                .isEqualTo("Kho TechZone, 88 Nguyễn Văn Linh, Phường Sài Gòn, Thành phố Hồ Chí Minh");

        // GET /seller/settings trả lại đúng khối kho, không còn quận
        mvc.perform(get("/api/v1/seller/settings").header(HttpHeaders.AUTHORIZATION, t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pickup_address.ward.name").value("Phường Sài Gòn"))
                .andExpect(jsonPath("$.data.pickup_address.ghn_shop_id").value(770_000 + s[1]))
                .andExpect(jsonPath("$.data.pickup_address.district").doesNotExist());
    }

    @Test
    void secondSaveUpdatesTheSameGhnShop() throws Exception {
        long[] s = verifiedShopWithoutPickup();
        String t = vendorToken(s[0]);
        putPickup(t, HCM_SAI_GON).andExpect(status().isOk());
        putPickup(t, "{\"phone_number\":\"0987654321\",\"province_id\":79,\"ward_code\":\"26743\","
                + "\"address_line\":\"12 Lê Lợi\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ward.name").value("Phường Bến Thành"))
                .andExpect(jsonPath("$.data.ghn_shop_id").value(770_000 + s[1]));
        List<FakePickupShopRegistrar.Call> calls = ghnPickup.callsFor(s[1]);
        assertThat(calls).hasSize(2);
        assertThat(calls.get(1).currentGhnShopId()).isEqualTo((int) (770_000 + s[1]));   // gọi cập nhật, không đăng ký mới
    }

    @Test
    void wardOutsideProvinceOrBadInputIsRejected() throws Exception {
        long[] s = verifiedShopWithoutPickup();
        String t = vendorToken(s[0]);
        // phường của An Giang nhưng chọn TP. HCM
        putPickup(t, "{\"phone_number\":\"0912345678\",\"province_id\":79,\"ward_code\":\"30985\",\"address_line\":\"1 A\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-VAL-4220"));
        putPickup(t, "{\"phone_number\":\"12345\",\"province_id\":79,\"ward_code\":\"26740\",\"address_line\":\"1 A\"}")
                .andExpect(status().isUnprocessableEntity());
        putPickup(t, "{\"phone_number\":\"0912345678\",\"province_id\":79,\"ward_code\":\"26740\",\"address_line\":\" \"}")
                .andExpect(status().isUnprocessableEntity());
        assertThat(ghnPickup.callsFor(s[1])).isEmpty();      // không gọi GHN khi dữ liệu sai
        assertThat(pickupRow(s[1]).get("ghn_shop_id")).isNull();
    }

    @Test
    void ghnFailureKeepsOldPickup() throws Exception {
        long[] s = verifiedShopWithoutPickup();
        String t = vendorToken(s[0]);
        putPickup(t, HCM_SAI_GON).andExpect(status().isOk());
        ghnPickup.failNext();
        putPickup(t, "{\"phone_number\":\"0987654321\",\"province_id\":79,\"ward_code\":\"26743\",\"address_line\":\"12 Lê Lợi\"}")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("GNE-LOG-5022"));
        Map<String, Object> row = pickupRow(s[1]);
        assertThat(row.get("pickup_ward_code")).isEqualTo("26740");
        assertThat(row.get("pickup_phone")).isEqualTo("0912345678");
    }

    @Test
    void unverifiedShopCannotConfigurePickup() throws Exception {
        long[] s = newShop();
        putPickup(vendorToken(s[0]), HCM_SAI_GON)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GNE-VND-4031"))
                .andExpect(jsonPath("$.details.kyc_status").value("NOT_SUBMITTED"));
        assertThat(ghnPickup.callsFor(s[1])).isEmpty();
    }
}
