package com.genie.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Công tắc dữ liệu mẫu (plan seed S01.02): bật → Flyway có thêm db/seed, tắt → không có bean. */
class SeedDataFlywayCustomizerTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SeedDataFlywayCustomizer.class);

    @Test
    void registersCustomizerOnlyWhenDemoDataEnabled() {
        runner.withPropertyValues("genie.seed.demo-data=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(SeedDataFlywayCustomizer.class));
        runner.withPropertyValues("genie.seed.demo-data=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(SeedDataFlywayCustomizer.class));
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(SeedDataFlywayCustomizer.class));
    }

    @Test
    void appendsSeedLocationOnceAndKeepsMigrations() {
        FluentConfiguration config = Flyway.configure().locations("classpath:db/migration");
        SeedDataFlywayCustomizer customizer = new SeedDataFlywayCustomizer();

        customizer.customize(config);
        customizer.customize(config);

        assertThat(Arrays.stream(config.getLocations()).map(Location::getDescriptor))
                .containsExactly("classpath:db/migration", "classpath:db/seed");
    }
}
