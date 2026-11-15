package com.genie.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Bật {@code @Async} (listener AFTER_COMMIT gửi email / thông báo — TS 4.5) và {@code @Retryable} (TS 10.2).
 * Executor dùng {@code spring.task.execution.*} của Spring Boot.
 */
@Configuration
@EnableAsync
@EnableRetry
public class AsyncConfig {
}
