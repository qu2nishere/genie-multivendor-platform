package com.genie.common.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.stereotype.Component;

/**
 * Nối thư mục dữ liệu mẫu {@code classpath:db/seed} vào Flyway khi {@code genie.seed.demo-data=true}
 * (plan seed 2026-11-16 D1, S01.02). Giá trị đọc từ {@code GENIE_SEED_DEMO}: mặc định {@code false} ở
 * {@code application.yml}, {@code true} ở profile {@code local}, {@code false} cứng ở profile {@code test};
 * production luôn {@code false} (ADR 0010 — RDS không bao giờ có dữ liệu mẫu).
 * Không ghi cứng {@code spring.flyway.locations} theo profile để các location khác (nếu có) vẫn giữ nguyên.
 */
@Component
@ConditionalOnProperty(name = "genie.seed.demo-data", havingValue = "true")
public class SeedDataFlywayCustomizer implements FlywayConfigurationCustomizer {

    public static final String SEED_LOCATION = "classpath:db/seed";

    @Override
    public void customize(FluentConfiguration configuration) {
        List<Location> locations = new ArrayList<>(Arrays.asList(configuration.getLocations()));
        Location seed = new Location(SEED_LOCATION);
        boolean present = locations.stream().anyMatch(l -> l.getDescriptor().equals(seed.getDescriptor()));
        if (!present) {
            locations.add(seed);
            configuration.locations(locations.toArray(Location[]::new));
        }
    }
}
