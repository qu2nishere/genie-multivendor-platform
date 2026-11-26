package com.genie.shipping.fee.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.shipping.fee.application.GhnFeeClient;
import com.genie.shipping.fee.application.Parcel;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Phí GHN thật theo địa giới 2 cấp — UC-ORD-06 (plan C2 D6, O11.01). Gọi {@code POST /v2/shipping-order/preview} (xem
 * trước đơn, KHÔNG tạo đơn) với {@code is_new_to_address: true} + {@code to_ward_code} = mã phường mới (master-data v3)
 * — API {@code /v2/shipping-order/fee} không nhận mã phường mới (spike D4, progress C2 mục 7). Kho gửi lấy theo header
 * {@code ShopId} của shop. Gói "Chuẩn" ({@code service_type_id = 2}), header {@code Token} (token sàn), timeout 3 giây,
 * không thử lại khi lỗi mạng / 5xx (quoter chuyển bảng dự phòng). Riêng {@code 400 "To address conflict"} — GHN trả
 * ngẫu nhiên cho cùng một yêu cầu hợp lệ (spike D4) — thử lại đúng 1 lần. Bật bằng {@code GHN_MODE=http}; địa chỉ API
 * {@code GHN_API_URL}.
 */
@Component
@ConditionalOnProperty(name = "GHN_MODE", havingValue = "http")
public class HttpGhnFeeClient implements GhnFeeClient {

    private static final Logger log = LoggerFactory.getLogger(HttpGhnFeeClient.class);
    static final String DEFAULT_GHN_API = "https://online-gateway.ghn.vn/shiip/public-api";
    static final int STANDARD_SERVICE = 2;
    /** Người bán trả phí cho GHN (khách đã trả sàn ở checkout) — chỉ để xem trước, không tạo đơn. */
    static final int SELLER_PAYS = 1;
    /** GHN giới hạn khai giá bảo hiểm 5 triệu. */
    static final long MAX_INSURANCE = 5_000_000L;
    /** Tổng số lần gọi khi GHN trả "To address conflict" ngẫu nhiên. */
    static final int MAX_ATTEMPTS = 2;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient http;
    private final String token;

    @Autowired
    public HttpGhnFeeClient(@Value("${genie.shipping.ghn.token:}") String token,
                            @Value("${GHN_API_URL:" + DEFAULT_GHN_API + "}") String baseUrl,
                            RestClient.Builder builder) {
        this(builder.requestFactory(timeouts()).baseUrl(baseUrl).build(), token);
        log.info("Phí vận chuyển: gọi GHN {} /v2/shipping-order/preview (GHN_MODE=http).", baseUrl);
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
        body.put("payment_type_id", SELLER_PAYS);
        body.put("required_note", "KHONGCHOXEMHANG");
        body.put("is_new_to_address", true);
        body.put("to_ward_code", p.toWardCode());
        body.put("to_name", p.toName());
        body.put("to_phone", p.toPhone());
        body.put("to_address", p.toAddress());
        body.put("weight", p.weightGrams());
        if (p.lengthCm() != null) body.put("length", p.lengthCm());
        if (p.widthCm() != null) body.put("width", p.widthCm());
        if (p.heightCm() != null) body.put("height", p.heightCm());
        body.put("insurance_value", Math.min(p.subtotal(), MAX_INSURANCE));
        body.put("cod_amount", 0);
        body.put("items", List.of(Map.of("name", "Kiện hàng Genie", "quantity", 1, "weight", p.weightGrams())));
        for (int attempt = 1; ; attempt++) {
            try {
                // Đọc chuỗi rồi tự parse: GHN thỉnh thoảng trả JSON với Content-Type application/octet-stream
                String raw = http.post().uri("/v2/shipping-order/preview").header("Token", token)
                        .header("ShopId", String.valueOf(p.ghnShopId()))
                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);
                return total(parse(raw));
            } catch (HttpClientErrorException e) {
                String msg = e.getResponseBodyAsString().toLowerCase(Locale.ROOT);
                if (msg.contains("address conflict") && attempt < MAX_ATTEMPTS) {
                    continue;
                }
                if (msg.contains("route") || msg.contains("không hỗ trợ") || msg.contains("not support")) {
                    throw new RouteNotSupportedException(e.getResponseBodyAsString());
                }
                throw e;
            }
        }
    }

    private static Map<?, ?> parse(String raw) {
        try {
            return raw == null || raw.isBlank() ? null : JSON.readValue(raw, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("GHN trả nội dung không phải JSON", e);
        }
    }

    /** {@code data.total_fee}; bản cũ không có thì cộng {@code fee.main_service + fee.insurance}. */
    private static long total(Map<?, ?> res) {
        Object code = res == null ? null : res.get("code");
        if (!(code instanceof Number n) || n.intValue() != 200 || !(res.get("data") instanceof Map<?, ?> data)) {
            throw new IllegalStateException("GHN trả kết quả phí không hợp lệ: " + res);
        }
        if (data.get("total_fee") instanceof Number total) {
            return total.longValue();
        }
        if (data.get("fee") instanceof Map<?, ?> fee && fee.get("main_service") instanceof Number main) {
            long insurance = fee.get("insurance") instanceof Number i ? i.longValue() : 0;
            return main.longValue() + insurance;
        }
        throw new IllegalStateException("GHN không trả phí: " + res);
    }
}
