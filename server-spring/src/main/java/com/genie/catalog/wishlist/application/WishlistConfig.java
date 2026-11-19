package com.genie.catalog.wishlist.application;

import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cài đặt mặc định của các cổng wishlist khi module khác chưa cài (plan wishlist W03.01). */
@Configuration
public class WishlistConfig {

    @Bean
    @ConditionalOnMissingBean(WishlistShopAvailability.class)
    WishlistShopAvailability noVacation() {
        return vendorIds -> Set.of();
    }
}
