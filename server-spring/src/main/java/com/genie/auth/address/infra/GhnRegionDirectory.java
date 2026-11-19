package com.genie.auth.address.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.auth.address.application.RegionDirectory;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Danh mục hành chính 2 cấp từ GHN master-data <b>v3</b> — địa giới mới sau 01/07/2025 (plan AUTH A12.01, D15, D25):
 * {@code GET /v3/master-data/province/all}, {@code /v3/master-data/ward/all-by-province-id?province_id=},
 * header {@code Token} + {@code ShopId}; timeout 3 giây. Tên trả về dùng nguyên văn làm {@code to_province_name} /
 * {@code to_ward_name} khi tạo đơn GHN với {@code is_new_to_address: true}. Chỉ lấy đơn vị {@code status = 1} (đang hoạt động).
 * Kết quả cache Redis 24 giờ ({@code region:ghn:*}) — danh mục gần như không đổi, tránh gọi GHN mỗi lần mở form.
 * Redis lỗi → gọi thẳng GHN; GHN lỗi → {@code 503}.
 */
public class GhnRegionDirectory implements RegionDirectory {

    private static final Logger log = LoggerFactory.getLogger(GhnRegionDirectory.class);
    static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final String KEY = "region:ghn:";

    private final RestClient http;
    private final String token;
    private final String shopId;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public GhnRegionDirectory(String baseUrl, String token, String shopId, RestClient.Builder builder,
                              StringRedisTemplate redis, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(3_000);
        this.http = builder.requestFactory(factory).baseUrl(baseUrl).build();
        this.token = token;
        this.shopId = shopId;
        this.redis = redis;
        this.objectMapper = objectMapper;
        log.info("Danh mục hành chính: dùng GHN master-data ({}).", baseUrl);
    }

    @Override
    public List<Region> provinces() {
        return cached("v3:provinces", new TypeReference<List<Region>>() {
        }, () -> fetch("/v3/master-data/province/all?limit=200", row -> new Region(
                ((Number) row.get("_id")).intValue(), String.valueOf(row.get("name")))));
    }

    @Override
    public List<Ward> wards(int provinceId) {
        return cached("v3:wards:" + provinceId, new TypeReference<List<Ward>>() {
        }, () -> fetch("/v3/master-data/ward/all-by-province-id?limit=200&province_id=" + provinceId,
                row -> new Ward(String.valueOf(((Number) row.get("_id")).longValue()),
                        String.valueOf(row.get("name")))));
    }

    private <T> List<T> fetch(String path, Function<Map<?, ?>, T> mapper) {
        Map<?, ?> body;
        try {
            body = http.get().uri(path).header("Token", token).header("ShopId", shopId).retrieve().body(Map.class);
        } catch (RestClientException e) {
            log.warn("Không gọi được GHN {}: {}", path, e.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "Chưa tải được danh mục địa chỉ, vui lòng thử lại.");
        }
        List<T> out = new ArrayList<>();
        if (body != null && body.get("data") instanceof List<?> rows) {
            for (Object row : rows) {
                if (row instanceof Map<?, ?> m && active(m)) {
                    try {
                        out.add(mapper.apply(m));
                    } catch (RuntimeException e) {
                        log.debug("Bỏ dòng GHN không đúng định dạng: {}", m);
                    }
                }
            }
        }
        return out;
    }

    /** {@code status}: 1 đang hoạt động, 2 tạm tắt, 10 đã xóa — thiếu trường thì coi như đang hoạt động. */
    private static boolean active(Map<?, ?> row) {
        return !(row.get("status") instanceof Number n) || n.intValue() == 1;
    }

    private <T> List<T> cached(String key, TypeReference<List<T>> type, Supplier<List<T>> loader) {
        try {
            String json = redis.opsForValue().get(KEY + key);
            if (json != null) {
                return objectMapper.readValue(json, type);
            }
        } catch (DataAccessException | IOException e) {
            log.debug("Bỏ qua cache danh mục hành chính {}: {}", key, e.getMessage());
        }
        List<T> fresh = loader.get();
        if (!fresh.isEmpty()) {
            try {
                redis.opsForValue().set(KEY + key, objectMapper.writeValueAsString(fresh), CACHE_TTL);
            } catch (DataAccessException | IOException e) {
                log.debug("Không ghi được cache danh mục hành chính {}: {}", key, e.getMessage());
            }
        }
        return fresh;
    }
}
