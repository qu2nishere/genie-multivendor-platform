package com.genie.shipping.fee.infra;

import com.genie.shipping.fee.application.GhnFeeClient;
import com.genie.shipping.fee.application.Parcel;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Phí GHN thật {@code POST /v2/shipping-order/fee} gói "Chuẩn" ({@code service_type_id = 2}) — UC-ORD-06 (plan C2 D6,
 * O05.04): header {@code Token} (token sàn) + {@code ShopId} (shop GHN của người bán), timeout 3 giây, không thử lại
 * (quoter chuyển bảng dự phòng). Bật bằng {@code GHN_MODE=http}; địa chỉ API {@code GHN_API_URL} như danh mục hành chính.
 * <p>Giới hạn đã biết (D4): API v2 còn đòi {@code from_district_id} / {@code to_district_id} theo địa giới cũ — quoter chỉ
 * gọi khi cả hai đầu có mã quận; chưa kiểm được với GHN sandbox (máy dev chưa có {@code GHN_TOKEN}).
 */
@Component
@ConditionalOnProperty(name = "GHN_MODE", havingValue = "http")
public class HttpGhnFeeClient implements GhnFeeClient {

    private static final Logger log = LoggerFactory.getLogger(HttpGhnFeeClient.class);
    static final String DEFAULT_GHN_API = "https://online-gateway.ghn.vn/shiip/public-api";
    static final int STANDARD_SERVICE = 2;

    private final RestClient http;
    private final String token;

    @Autowired
    public HttpGhnFeeClient(@Value("${genie.shipping.ghn.token:}") String token,
                            @Value("${GHN_API_URL:" + DEFAULT_GHN_API + "}") String baseUrl,
                            RestClient.Builder builder) {
        this(builder.requestFactory(timeouts()).baseUrl(baseUrl).build(), token);
        log.info("Phí vận chuyển: gọi GHN {} /v2/shipping-order/fee (GHN_MODE=http).", baseUrl);
    }

    HttpGhnFeeClient(RestClient http, String token) {
        this.http = http;
        this.token = token == null ? "" : token.trim();
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(3_000);
        return factory;
    }

    @Override
    public long fee(Parcel p) {
        if (token.isEmpty()) {
            throw new IllegalStateException("Thiếu GHN_TOKEN khi GHN_MODE=http");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service_type_id", STANDARD_SERVICE);
        body.put("from_district_id", p.fromDistrictId());
        body.put("from_ward_code", p.fromWardCode());
        body.put("to_district_id", p.toDistrictId());
        body.put("to_ward_code", p.toWardCode());
        body.put("weight", p.weightGrams());
        if (p.lengthCm() != null) body.put("length", p.lengthCm());
        if (p.widthCm() != null) body.put("width", p.widthCm());
        if (p.heightCm() != null) body.put("height", p.heightCm());
        body.put("insurance_value", Math.min(p.subtotal(), 5_000_000L));   // GHN giới hạn khai giá 5 triệu
        try {
            Map<?, ?> res = http.post().uri("/v2/shipping-order/fee").header("Token", token)
                    .header("ShopId", String.valueOf(p.ghnShopId()))
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
            return total(res);
        } catch (HttpClientErrorException e) {
            String msg = e.getResponseBodyAsString().toLowerCase(Locale.ROOT);
            if (msg.contains("route") || msg.contains("không hỗ trợ") || msg.contains("not support")) {
                throw new RouteNotSupportedException(e.getResponseBodyAsString());
            }
            throw e;
        }
    }

    private static long total(Map<?, ?> res) {
        Object code = res == null ? null : res.get("code");
        if (!(code instanceof Number n) || n.intValue() != 200 || !(res.get("data") instanceof Map<?, ?> data)
                || !(data.get("total") instanceof Number total)) {
            throw new IllegalStateException("GHN trả kết quả phí không hợp lệ: " + res);
        }
        return total.longValue();
    }
}
