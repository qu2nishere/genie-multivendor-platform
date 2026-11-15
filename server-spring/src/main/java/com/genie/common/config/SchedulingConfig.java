package com.genie.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật {@code @Scheduled} cho Cron (TS 10.3: {@code FOR UPDATE SKIP LOCKED}). Tắt trong test bằng
 * {@code genie.scheduling.enabled=false}.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "genie.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
