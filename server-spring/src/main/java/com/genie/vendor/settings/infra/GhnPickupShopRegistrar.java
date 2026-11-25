package com.genie.vendor.settings.infra;

import com.genie.vendor.settings.application.PickupShopRegistrar;
import java.util.LinkedHashMap;
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
import org.springframework.web.client.RestClientException;

/**
 * Đăng ký kho lấy hàng thật qua GHN {@code POST /v2/shop/register} (UC-VND-05; plan Mở shop & KYC D16, K07.02; TS 10.2):
 * header {@code Token}, timeout 5 giây, thử lại 2 lần khi lỗi mạng / 5xx (lỗi 4xx = dữ liệu sai, không thử lại).
 * Bật bằng {@code GHN_PICKUP_MODE=http}; địa chỉ API đọc {@code GHN_API_URL} như danh mục hành chính.
 * <p>
 * Giới hạn đã biết: API {@code shop/register} của GHN vẫn nhận {@code district_id} + {@code ward_code} theo địa giới
 * <b>cũ</b>, còn Genie lưu phường theo địa giới 2 cấp (01/07/2025) — chưa kiểm được với GHN thật. GHN không có API công
 * khai sửa địa chỉ cửa hàng nên đổi kho = đăng ký lại và nhận Shop ID mới.
 */
@Component
@ConditionalOnProperty(prefix = "genie.shipping.ghn", name = "pickup-mode", havingValue = "http")
public class GhnPickupShopRegistrar implements PickupShopRegistrar {

    private static final Logger log = LoggerFactory.getLogger(GhnPickupShopRegistrar.class);
    static final int MAX_ATTEMPTS = 3;
    static final String DEFAULT_GHN_API = "https://online-gateway.ghn.vn/shiip/public-api";

    private final RestClient http;
    private final String token;

    @Autowired
    public GhnPickupShopRegistrar(@Value("${genie.shipping.ghn.token:}") String token,
                                  @Value("${GHN_API_URL:" + DEFAULT_GHN_API + "}") String baseUrl,
                                  RestClient.Builder builder) {
        this(builder.requestFactory(timeouts()).baseUrl(baseUrl).build(), token);
        log.info("Kho lấy hàng GHN: gọi {} /v2/shop/register (GHN_PICKUP_MODE=http).", baseUrl);
    }

    GhnPickupShopRegistrar(RestClient http, String token) {
        this.http = http;
        this.token = token == null ? "" : token.trim();
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(5_000);
        return factory;
    }

    @Override
    public int sync(Integer currentGhnShopId, PickupInfo info) {
        if (token.isEmpty()) {
            throw new PickupSyncException("Thiếu GHN_TOKEN khi GHN_PICKUP_MODE=http", null);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ward_code", info.wardCode());
        body.put("name", info.shopName());
        body.put("phone", info.phone());
        body.put("address", info.fullAddress());
        RuntimeException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Map<?, ?> res = http.post().uri("/v2/shop/register").header("Token", token)
                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
                return shopId(res);
            } catch (HttpClientErrorException e) {
                log.warn("GHN từ chối đăng ký kho của shop {}: {} {}", info.vendorProfileId(), e.getStatusCode(),
                        e.getResponseBodyAsString());
                throw new PickupSyncException("GHN từ chối đăng ký kho", e);
            } catch (RestClientException | PickupSyncException e) {
                last = e;
                log.warn("GHN đăng ký kho của shop {} lỗi (lần {}/{}): {}", info.vendorProfileId(), attempt, MAX_ATTEMPTS,
                        e.getMessage());
            }
        }
        throw new PickupSyncException("GHN không phản hồi sau " + MAX_ATTEMPTS + " lần", last);
    }

    private static int shopId(Map<?, ?> res) {
        Object code = res == null ? null : res.get("code");
        if (!(code instanceof Number n) || n.intValue() != 200 || !(res.get("data") instanceof Map<?, ?> data)
                || !(data.get("shop_id") instanceof Number id)) {
            throw new PickupSyncException("GHN trả kết quả không hợp lệ: " + res, null);
        }
        return id.intValue();
    }
}
