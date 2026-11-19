package com.genie.auth.address.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.auth.address.application.ReverseGeocoder;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reverse geocode bằng Nominatim — OpenStreetMap (plan AUTH A12.06, D15). Tuân thủ chính sách dùng miễn phí của
 * Nominatim: User-Agent riêng của ứng dụng, tối đa 1 yêu cầu / giây (chờ tuần tự trong tiến trình), cache kết quả theo
 * tọa độ làm tròn 4 chữ số (~11 m) trong Redis 7 ngày. Địa chỉ máy chủ đổi được bằng biến môi trường
 * {@code NOMINATIM_URL} (tự dựng Nominatim riêng nếu cần).
 */
@Component
public class NominatimGeocoder implements ReverseGeocoder {

    private static final Logger log = LoggerFactory.getLogger(NominatimGeocoder.class);
    static final String USER_AGENT = "GenieShop/1.0 (TLCN student project; reverse geocoding for address form)";
    private static final Duration CACHE_TTL = Duration.ofDays(7);
    private static final long MIN_INTERVAL_MS = 1_000;
    /** Trường địa chỉ của Nominatim, xếp từ đơn vị nhỏ đến lớn. */
    private static final List<String> AREA_FIELDS = List.of("quarter", "neighbourhood", "suburb", "village",
            "hamlet", "town", "city_district", "district", "county", "municipality", "city", "state_district",
            "state", "province", "region");

    private final RestClient http;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private long lastCallAt;

    public NominatimGeocoder(@Value("${NOMINATIM_URL:https://nominatim.openstreetmap.org}") String baseUrl,
                             RestClient.Builder builder, StringRedisTemplate redis, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(5_000);
        this.http = builder.requestFactory(factory).baseUrl(baseUrl).build();
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<GeoPlace> lookup(double lat, double lng) {
        String key = String.format(Locale.ROOT, "geo:rev:%.4f:%.4f", lat, lng);
        try {
            String json = redis.opsForValue().get(key);
            if (json != null) {
                return Optional.of(objectMapper.readValue(json, GeoPlace.class));
            }
        } catch (DataAccessException | IOException e) {
            log.debug("Bỏ qua cache reverse geocode: {}", e.getMessage());
        }
        Optional<GeoPlace> place = call(lat, lng);
        place.ifPresent(p -> {
            try {
                redis.opsForValue().set(key, objectMapper.writeValueAsString(p), CACHE_TTL);
            } catch (DataAccessException | IOException e) {
                log.debug("Không ghi được cache reverse geocode: {}", e.getMessage());
            }
        });
        return place;
    }

    private Optional<GeoPlace> call(double lat, double lng) {
        throttle();
        Map<?, ?> body;
        try {
            body = http.get()
                    .uri(u -> u.path("/reverse").queryParam("format", "jsonv2").queryParam("lat", lat)
                            .queryParam("lon", lng).queryParam("zoom", 18).queryParam("addressdetails", 1)
                            .queryParam("accept-language", "vi").build())
                    .header("User-Agent", USER_AGENT)
                    .retrieve().body(Map.class);
        } catch (RestClientException e) {
            log.warn("Không gọi được Nominatim: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "Chưa xác định được địa chỉ từ vị trí, vui lòng chọn thủ công.");
        }
        if (body == null || body.containsKey("error") || !(body.get("address") instanceof Map<?, ?> address)) {
            return Optional.empty();
        }
        return Optional.of(toPlace(address));
    }

    static GeoPlace toPlace(Map<?, ?> address) {
        List<String> names = new ArrayList<>();
        for (String field : AREA_FIELDS) {
            if (address.get(field) instanceof String v && !v.isBlank()) {
                names.add(v.trim());
            }
        }
        String house = address.get("house_number") instanceof String h ? h.trim() : "";
        String road = address.get("road") instanceof String r ? r.trim() : "";
        String line = (house + " " + road).trim();
        return new GeoPlace(names, line);
    }

    /** Nominatim cho tối đa 1 yêu cầu / giây — các lời gọi trong tiến trình xếp hàng. */
    private synchronized void throttle() {
        long wait = lastCallAt + MIN_INTERVAL_MS - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallAt = System.currentTimeMillis();
    }
}
