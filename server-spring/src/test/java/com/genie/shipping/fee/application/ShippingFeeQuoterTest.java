package com.genie.shipping.fee.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.common.cache.JsonCache;
import com.genie.shipping.shipment.domain.VendorShippingConfig;
import com.genie.shipping.shipment.domain.VendorShippingConfigRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** UC-ORD-06 — freeship, thiếu kho, chế độ stub / http, GHN lỗi, tuyến không hỗ trợ, cache (plan C2 O05.06). */
class ShippingFeeQuoterTest {

    private final VendorShippingConfigRepository configs = mock(VendorShippingConfigRepository.class);
    private final GhnFeeClient ghn = mock(GhnFeeClient.class);
    private final Map<String, String> redisData = new HashMap<>();
    private JsonCache cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenAnswer(i -> redisData.get(i.<String>getArgument(0)));
        org.mockito.Mockito.doAnswer(i -> redisData.put(i.getArgument(0), i.getArgument(1)))
                .when(ops).set(anyString(), anyString(), any(Duration.class));
        cache = new JsonCache(redis, new ObjectMapper());
        when(configs.findByVendorId(anyLong())).thenReturn(Optional.empty());
    }

    @SuppressWarnings("unchecked")
    private ShippingFeeQuoter quoter(GhnFeeClient client) {
        ObjectProvider<GhnFeeClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(client);
        return new ShippingFeeQuoter(configs, provider, cache);
    }

    /** Kho ở tỉnh 79 có shop GHN {@code 99}; giao tới tỉnh {@code to}, phường {@code ward} (mã v3; NULL = thiếu). */
    private static Parcel parcel(int to, String ward, long subtotal, boolean sellerPays) {
        return parcel(to, ward, subtotal, sellerPays, 99);
    }

    private static Parcel parcel(int to, String ward, long subtotal, boolean sellerPays, Integer ghnShopId) {
        return new Parcel(5L, 79, ghnShopId, to, ward, "Người nhận", "0901234567", "25 Nguyễn Huệ, Phường A, Tỉnh B",
                800, 20, 10, 5, subtotal, sellerPays);
    }

    @Test
    void stubModeUsesFlatTableAsExactFee() {
        ShippingQuote same = quoter(null).quote(parcel(79, null, 100_000, false));
        assertThat(same.customerFee()).isEqualTo(15_000);
        assertThat(same.estimated()).isFalse();
        assertThat(quoter(null).quote(parcel(1, null, 100_000, false)).customerFee()).isEqualTo(30_000);
    }

    @Test
    void shopWithoutPickupGetsEstimatedInterProvinceFee() {
        Parcel noPickup = new Parcel(5L, null, null, 79, "1003506", "A", "0901", "addr", 500, null, null, null, 10_000,
                false);
        ShippingQuote q = quoter(ghn).quote(noPickup);
        assertThat(q.customerFee()).isEqualTo(30_000);
        assertThat(q.estimated()).isTrue();
        verify(ghn, never()).fee(any());
    }

    @Test
    void freeshipThresholdOrSellerPaysMakesCustomerFeeZero() {
        when(configs.findByVendorId(5L)).thenReturn(Optional.of(
                VendorShippingConfig.builder().vendorId(5L).freeshipThreshold(new BigDecimal("299000")).build()));
        ShippingQuote reached = quoter(null).quote(parcel(1, null, 299_000, false));
        assertThat(reached.freeByShop()).isTrue();
        assertThat(reached.customerFee()).isZero();
        assertThat(reached.carrierFee()).isEqualTo(30_000);
        assertThat(quoter(null).quote(parcel(1, null, 298_999, false)).freeByShop()).isFalse();
        assertThat(quoter(null).quote(parcel(1, null, 1_000, true)).customerFee()).isZero();
    }

    @Test
    void httpModeWithoutGhnShopOrWardFallsBackToEstimatedFee() {
        ShippingQuote noShop = quoter(ghn).quote(parcel(1, "1003476", 100_000, false, null));
        assertThat(noShop.estimated()).isTrue();
        assertThat(noShop.customerFee()).isEqualTo(30_000);
        assertThat(quoter(ghn).quote(parcel(1, null, 100_000, false)).estimated()).isTrue();
        verify(ghn, never()).fee(any());
    }

    @Test
    void twoLevelAddressIsQuotedByGhnWithoutDistrict() {
        when(ghn.fee(any())).thenReturn(53_900L);
        ShippingQuote q = quoter(ghn).quote(parcel(1000000, "1003476", 100_000, false));
        assertThat(q.customerFee()).isEqualTo(53_900);
        assertThat(q.estimated()).isFalse();
    }

    @Test
    void httpModeAsksGhnOnceThenServesFromCache() {
        when(ghn.fee(any())).thenReturn(27_500L);
        ShippingQuote first = quoter(ghn).quote(parcel(1, "1003476", 100_000, false));
        ShippingQuote second = quoter(ghn).quote(parcel(1, "1003476", 100_000, false));
        assertThat(first.customerFee()).isEqualTo(27_500);
        assertThat(first.estimated()).isFalse();
        assertThat(second.customerFee()).isEqualTo(27_500);
        verify(ghn, times(1)).fee(any());
    }

    @Test
    void ghnErrorFallsBackAndUnsupportedRouteIsUnavailable() {
        when(ghn.fee(any())).thenThrow(new IllegalStateException("timeout"));
        ShippingQuote err = quoter(ghn).quote(parcel(79, "1003506", 100_000, false));
        assertThat(err.estimated()).isTrue();
        assertThat(err.customerFee()).isEqualTo(15_000);

        GhnFeeClient noRoute = mock(GhnFeeClient.class);
        when(noRoute.fee(any())).thenThrow(new GhnFeeClient.RouteNotSupportedException("route"));
        assertThat(quoter(noRoute).quote(parcel(1, "1003477", 100_000, false)).unavailable()).isTrue();
    }

    @Test
    void flatTable() {
        assertThat(FlatFeeTable.fee(79, 79)).isEqualTo(15_000);
        assertThat(FlatFeeTable.fee(79, 1)).isEqualTo(30_000);
        assertThat(FlatFeeTable.fee(null, 1)).isEqualTo(30_000);
    }
}
