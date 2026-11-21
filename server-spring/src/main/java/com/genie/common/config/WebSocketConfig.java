package com.genie.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.common.security.SecurityProperties;
import com.genie.common.security.StompAuthInterceptor;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Realtime — WebSocket STOMP (API_SPEC mục 13; TECH_STACK: simple broker trong bộ nhớ, một instance). Endpoint
 * {@code /ws} (WebSocket thuần, không SockJS), origin theo {@code genie.security.cors-origins}. Kênh riêng
 * {@code /user/queue/**}, kênh chung {@code /topic/**}; client gửi lên {@code /app/**}. Xác thực ở khung CONNECT
 * ({@link StompAuthInterceptor}). Tin nhắn JSON dùng {@link ObjectMapper} của ứng dụng (snake_case, thời gian ISO).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final SecurityProperties security;
    private final StompAuthInterceptor auth;
    private final ObjectMapper objectMapper;

    public WebSocketConfig(SecurityProperties security, StompAuthInterceptor auth, ObjectMapper objectMapper) {
        this.security = security;
        this.auth = auth;
        this.objectMapper = objectMapper;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(security.corsOrigins().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(auth);
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> converters) {
        MappingJackson2MessageConverter json = new MappingJackson2MessageConverter();
        json.setObjectMapper(objectMapper);
        converters.add(json);
        converters.add(new StringMessageConverter());
        converters.add(new ByteArrayMessageConverter());
        return false;
    }
}
