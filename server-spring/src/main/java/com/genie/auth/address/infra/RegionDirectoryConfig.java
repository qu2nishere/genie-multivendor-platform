package com.genie.auth.address.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.auth.address.application.RegionDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestClient;

/**
 * Chọn nguồn danh mục hành chính 2 cấp (plan AUTH D15, D25): có cả {@code GHN_TOKEN} và {@code GHN_SHOP_ID}
 * ({@code genie.shipping.ghn.token}, {@code genie.shipping.ghn.shop-id}) → GHN thật (API v3 bắt buộc header
 * {@code ShopId}), thiếu một trong hai → danh mục JSON trong repo. Địa chỉ API GHN đọc từ biến môi trường {@code GHN_API_URL} (mặc định production; môi trường
 * thử của GHN: {@code https://dev-online-gateway.ghn.vn/shiip/public-api}) — không thêm khóa vào application.yml.
 */
@Configuration
public class RegionDirectoryConfig {

    static final String DEFAULT_GHN_API = "https://online-gateway.ghn.vn/shiip/public-api";

    @Bean
    RegionDirectory regionDirectory(@Value("${genie.shipping.ghn.token:}") String token,
                                    @Value("${genie.shipping.ghn.shop-id:}") String shopId,
                                    @Value("${GHN_API_URL:" + DEFAULT_GHN_API + "}") String baseUrl,
                                    RestClient.Builder builder, StringRedisTemplate redis, ObjectMapper objectMapper) {
        if (token == null || token.isBlank() || shopId == null || shopId.isBlank()) {
            return new StaticRegionDirectory(objectMapper);
        }
        return new GhnRegionDirectory(baseUrl, token.trim(), shopId.trim(), builder, redis, objectMapper);
    }
}
