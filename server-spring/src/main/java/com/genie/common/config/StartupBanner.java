package com.genie.common.config;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * In một khối log dễ thấy khi backend đã sẵn sàng nhận request (sau {@link ApplicationReadyEvent}): địa chỉ API,
 * Swagger, WebSocket, giao diện web và Mailpit (profile {@code local}) — đỡ phải dò log khởi động để biết lúc nào thử được.
 */
@Component
public class StartupBanner {

    private static final Logger log = LoggerFactory.getLogger(StartupBanner.class);

    private final Environment env;
    private final String webUrl;

    public StartupBanner(Environment env, @Value("${genie.web-url:http://localhost:4321}") String webUrl) {
        this.env = env;
        this.webUrl = webUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ready(ApplicationReadyEvent event) {
        if (!(event.getApplicationContext() instanceof WebServerApplicationContext web)) return;   // test MockMvc
        int port = web.getWebServer().getPort();
        if (port <= 0) return;
        String base = "http://localhost:" + port;
        String profiles = String.join(", ", env.getActiveProfiles().length == 0
                ? env.getDefaultProfiles() : env.getActiveProfiles());
        StringBuilder sb = new StringBuilder()
                .append("\n\n  ============================================================\n")
                .append("   Genie backend đã sẵn sàng (profile: ").append(profiles).append(")\n")
                .append("   Giao diện web : ").append(webUrl).append('\n')
                .append("   API           : ").append(base).append("/api/v1\n")
                .append("   Swagger UI    : ").append(base)
                .append(env.getProperty("springdoc.swagger-ui.path", "/swagger-ui.html")).append('\n')
                .append("   WebSocket     : ws://localhost:").append(port).append("/ws\n")
                .append("   Health        : ").append(base).append("/actuator/health\n");
        if (Arrays.asList(env.getActiveProfiles()).contains("local")) {
            sb.append("   Mailpit (OTP) : http://localhost:8025\n");
        }
        sb.append("  ============================================================\n");
        log.info(sb.toString());
    }
}
