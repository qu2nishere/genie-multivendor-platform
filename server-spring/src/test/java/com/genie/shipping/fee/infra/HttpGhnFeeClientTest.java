package com.genie.shipping.fee.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.genie.shipping.fee.application.GhnFeeClient;
import com.genie.shipping.fee.application.Parcel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Phí GHN thật qua preview (địa giới 2 cấp) — UC-ORD-06 (plan C2 O05.06, O11.01): OK, phường lạ, tuyến không hỗ trợ, lỗi máy chủ. */
class HttpGhnFeeClientTest {

    private static final String URL = "https://ghn.test/v2/shipping-order/preview";
    private static final Parcel P = new Parcel(5L, 1000001, 884120, 1000000, "1003476", "Nguyễn Văn A", "0901234567",
            "25 Tôn Đức Thắng, Phường Đống Đa, Hà Nội", 800, 20, null, null, 7_000_000L, false);

    private MockRestServiceServer server;
    private HttpGhnFeeClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ghn.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpGhnFeeClient(builder.build(), "tok");
    }

    @Test
    void returnsTotalFee() {
        server.expect(once(), requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Token", "tok")).andExpect(header("ShopId", "884120"))
                .andExpect(jsonPath("$.service_type_id").value(2))
                .andExpect(jsonPath("$.is_new_to_address").value(true))
                .andExpect(jsonPath("$.to_ward_code").value("1003476"))
                .andExpect(jsonPath("$.to_name").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.to_address").value("25 Tôn Đức Thắng, Phường Đống Đa, Hà Nội"))
                .andExpect(jsonPath("$.to_district_id").doesNotExist())
                .andExpect(jsonPath("$.weight").value(800))
                .andExpect(jsonPath("$.insurance_value").value(5_000_000))
                .andExpect(jsonPath("$.items[0].weight").value(800))
                .andRespond(withSuccess("{\"code\":200,\"data\":{\"order_code\":\"\",\"total_fee\":63900,"
                        + "\"fee\":{\"main_service\":53900,\"insurance\":10000}}}", MediaType.APPLICATION_JSON));
        assertThat(client.fee(P)).isEqualTo(63_900);
        server.verify();
    }

    @Test
    void retriesOnceOnRandomAddressConflict() {
        String conflict = "{\"code\":400,\"message\":\"To address conflict\"}";
        server.expect(once(), requestTo(URL)).andRespond(withBadRequest().body(conflict)
                .contentType(MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(URL)).andRespond(withSuccess(
                "{\"code\":200,\"data\":{\"total_fee\":53900}}", MediaType.APPLICATION_JSON));
        assertThat(client.fee(P)).isEqualTo(53_900);
        server.verify();
    }

    @Test
    void givesUpAfterSecondAddressConflict() {
        String conflict = "{\"code\":400,\"message\":\"To address conflict\"}";
        server.expect(times(2), requestTo(URL)).andRespond(withBadRequest().body(conflict)
                .contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.fee(P)).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @Test
    void readsJsonEvenWithOctetStreamContentType() {
        server.expect(once(), requestTo(URL)).andRespond(withSuccess("{\"code\":200,\"data\":{\"total_fee\":20900}}",
                MediaType.APPLICATION_OCTET_STREAM));
        assertThat(client.fee(P)).isEqualTo(20_900);
    }

    @Test
    void sumsFeePartsWhenTotalIsMissing() {
        server.expect(once(), requestTo(URL)).andRespond(withSuccess(
                "{\"code\":200,\"data\":{\"fee\":{\"main_service\":20900,\"insurance\":0}}}",
                MediaType.APPLICATION_JSON));
        assertThat(client.fee(P)).isEqualTo(20_900);
    }

    @Test
    void unknownWardIsAnErrorForFallback() {
        server.expect(once(), requestTo(URL)).andRespond(withBadRequest().body(
                "{\"code\":400,\"message\":\"phường/xã người nhận mới không tồn tại trong hệ thống\"}")
                .contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.fee(P)).isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(GhnFeeClient.RouteNotSupportedException.class);
    }

    @Test
    void unsupportedRouteIsReported() {
        server.expect(once(), requestTo(URL)).andRespond(withBadRequest()
                .body("{\"code\":400,\"message\":\"Route not supported\"}").contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.fee(P)).isInstanceOf(GhnFeeClient.RouteNotSupportedException.class);
    }

    @Test
    void serverErrorBubblesUpForFallback() {
        server.expect(once(), requestTo(URL)).andRespond(withServerError());
        assertThatThrownBy(() -> client.fee(P)).isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(GhnFeeClient.RouteNotSupportedException.class);
    }

    @Test
    void missingTokenFailsFast() {
        assertThatThrownBy(() -> new HttpGhnFeeClient(RestClient.create(), " ").fee(P))
                .isInstanceOf(IllegalStateException.class);
    }
}
