package com.genie.common.config;

import com.genie.common.security.ClientInfoResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình Spring MVC dùng chung: cho phép controller nhận {@code ClientInfo} làm tham số.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ClientInfoResolver clientInfoResolver;

    public WebConfig(ClientInfoResolver clientInfoResolver) {
        this.clientInfoResolver = clientInfoResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(clientInfoResolver);
    }
}
