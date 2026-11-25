package com.genie.vendor.settings.infra;

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

import com.genie.vendor.settings.application.PickupShopRegistrar.PickupInfo;
import com.genie.vendor.settings.application.PickupShopRegistrar.PickupSyncException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Đăng ký kho GHN thật — UC-VND-05 (plan Mở shop & KYC K07.02). */
class GhnPickupShopRegistrarTest {

    private static final String URL = "https://ghn.test/v2/shop/register";
    private static final PickupInfo INFO = new PickupInfo(7, "TechZone", "0912345678", 79, "Thành phố Hồ Chí Minh",
            "26740", "Phường Sài Gòn", "88 Nguyễn Văn Linh");

    private MockRestServiceServer server;
    private GhnPickupShopRegistrar registrar;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ghn.test");
        server = MockRestServiceServer.bindTo(builder).build();
        registrar = new GhnPickupShopRegistrar(builder.build(), "tok");
    }

    @Test
    void registersAndReturnsShopId() {
        server.expect(once(), requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Token", "tok"))
                .andExpect(jsonPath("$.ward_code").value("26740"))
                .andExpect(jsonPath("$.address").value("88 Nguyễn Văn Linh, Phường Sài Gòn, Thành phố Hồ Chí Minh"))
                .andRespond(withSuccess("{\"code\":200,\"data\":{\"shop_id\":884120}}", MediaType.APPLICATION_JSON));
        assertThat(registrar.sync(null, INFO)).isEqualTo(884120);
        server.verify();
    }

    @Test
    void retriesServerErrorsThenSucceeds() {
        server.expect(times(2), requestTo(URL)).andRespond(withServerError());
        server.expect(once(), requestTo(URL))
                .andRespond(withSuccess("{\"code\":200,\"data\":{\"shop_id\":5}}", MediaType.APPLICATION_JSON));
        assertThat(registrar.sync(3, INFO)).isEqualTo(5);
        server.verify();
    }

    @Test
    void givesUpAfterThreeAttempts() {
        server.expect(times(3), requestTo(URL)).andRespond(withServerError());
        assertThatThrownBy(() -> registrar.sync(null, INFO)).isInstanceOf(PickupSyncException.class);
        server.verify();
    }

    @Test
    void doesNotRetryRejectedData() {
        server.expect(once(), requestTo(URL)).andRespond(withBadRequest()
                .body("{\"code\":400,\"message\":\"Ward code is invalid\"}").contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> registrar.sync(null, INFO)).isInstanceOf(PickupSyncException.class);
        server.verify();
    }

    @Test
    void missingTokenFailsWithoutCallingGhn() {
        GhnPickupShopRegistrar noToken = new GhnPickupShopRegistrar(RestClient.create(), " ");
        assertThatThrownBy(() -> noToken.sync(null, INFO)).isInstanceOf(PickupSyncException.class);
    }
}
