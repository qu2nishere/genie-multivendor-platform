package com.genie.catalog.search.application;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cài đặt mặc định của các cổng đảo ngược cụm tìm kiếm — module khác cài bean riêng thì bản này tự tắt. */
@Configuration(proxyBeanMethods = false)
public class SearchPortsConfig {

    @Bean
    @ConditionalOnMissingBean(ShopVoucherAvailability.class)
    ShopVoucherAvailability unsupportedShopVoucherAvailability() {
        return Optional::empty;
    }
}
