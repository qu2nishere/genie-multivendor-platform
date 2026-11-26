package com.genie.shipping.fee.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
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

/** Phí GHN thật — UC-ORD-06 (plan C2 O05.06): OK, tuyến không hỗ trợ, lỗi máy chủ. */
class HttpGhnFeeClientTest {

    private static final String URL = "https://ghn.test/v2/shipping-order/fee";
    private static final Parcel P = new Parcel(5L, 79, 1442, "20101", 884120, 1, 1450, "W1", 800, 20, null, null,
            7_000_000L, false);

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
                .andExpect(jsonPath("$.to_ward_code").value("W1"))
                .andExpect(jsonPath("$.weight").value(800))
                .andExpect(jsonPath("$.insurance_value").value(5_000_000))
                .andRespond(withSuccess("{\"code\":200,\"data\":{\"total\":36300}}", MediaType.APPLICATION_JSON));
        assertThat(client.fee(P)).isEqualTo(36_300);
        server.verify();
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
