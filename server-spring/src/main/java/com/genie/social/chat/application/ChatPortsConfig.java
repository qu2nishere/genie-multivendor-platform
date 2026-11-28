// Bản mặc định của các cổng chat (plan 2026-11-26 chat M02.02) — module kia cài bean cùng kiểu thì bản dưới không tạo
package com.genie.social.chat.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Không có module cài → shop không tìm thấy, sản phẩm / đơn bị từ chối (để {@code social} chạy / test riêng được). */
@Configuration(proxyBeanMethods = false)
public class ChatPortsConfig {

    @Bean
    @ConditionalOnMissingBean(ChatShopDirectory.class)
    ChatShopDirectory noChatShops() {
        return new ChatShopDirectory() {
            @Override
            public Optional<ChatShop> find(long shopId) {
                return Optional.empty();
            }

            @Override
            public Map<Long, ChatShop> byVendorUsers(Collection<Long> vendorUserIds) {
                return Map.of();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(ChatProductLookup.class)
    ChatProductLookup noChatProducts() {
        return new ChatProductLookup() {
            @Override
            public Optional<ProductRef> resolve(long productId, Long variantId) {
                return Optional.empty();
            }

            @Override
            public Map<Long, ProductCard> cards(Collection<Long> variantIds) {
                return Map.of();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(ChatOrderLookup.class)
    ChatOrderLookup noChatOrders() {
        return new ChatOrderLookup() {
            @Override
            public Map<Long, OrderCard> cards(Collection<Long> orderIds) {
                return Map.of();
            }

            @Override
            public List<OrderCard> recentBetween(long customerId, long vendorUserId, int limit) {
                return List.of();
            }
        };
    }
}
