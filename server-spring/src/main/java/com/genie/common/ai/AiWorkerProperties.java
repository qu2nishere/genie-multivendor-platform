package com.genie.common.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình {@code genie.ai-worker.*} (TS 2.5, 16.6; ADR 0011 mục 6).
 *
 * @param url             địa chỉ AI Worker FastAPI ({@code AI_WORKER_URL})
 * @param mode            {@code http} = gọi AI Worker thật; {@code stub} = bản giả lập trong backend ({@code AI_WORKER_MODE})
 * @param callbackBaseUrl địa chỉ backend mà AI Worker gọi lại {@code /api/v1/internal/**} ({@code BACKEND_INTERNAL_URL})
 */
@ConfigurationProperties(prefix = "genie.ai-worker")
public record AiWorkerProperties(
        @DefaultValue("http://localhost:8000") String url,
        @DefaultValue("http") String mode,
        @DefaultValue("http://localhost:8080") String callbackBaseUrl) {
}
