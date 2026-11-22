package com.genie.common.security;

import java.security.Principal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Xác thực kết nối STOMP (API_SPEC mục 13): khung {@code CONNECT} phải mang {@code Authorization: Bearer <access token>}
 * hợp lệ, chưa bị thu hồi — sai thì từ chối (client nhận khung {@code ERROR}). Người dùng gắn vào phiên WebSocket là
 * {@link StompUser} (tên = {@code userId}) để {@code /user/queue/**} gửi đúng người.
 * <p>Chỉ cho đăng ký kênh riêng {@code /user/queue/**} và kênh chung {@code /topic/platform} — không nghe trộm được
 * hàng đợi của người khác.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    static final String AUTHORIZATION = "Authorization";

    private final JwtService jwtService;
    private final TokenRevocationStore revocations;

    public StompAuthInterceptor(JwtService jwtService, TokenRevocationStore revocations) {
        this.jwtService = jwtService;
        this.revocations = revocations;
    }

    /** Người dùng của một phiên WebSocket — {@code getName()} là {@code userId} (đích của {@code convertAndSendToUser}). */
    public record StompUser(AuthUser user) implements Principal {
        @Override
        public String getName() {
            return String.valueOf(user.id());
        }
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            accessor.setUser(new StompUser(authenticate(accessor.getFirstNativeHeader(AUTHORIZATION))));
        } else if (command == StompCommand.SUBSCRIBE) {
            if (!(accessor.getUser() instanceof StompUser) || !allowed(accessor.getDestination())) {
                throw new MessageDeliveryException("Không được đăng ký kênh này.");
            }
        } else if (command == StompCommand.SEND && !(accessor.getUser() instanceof StompUser)) {
            throw new MessageDeliveryException("Chưa xác thực.");
        }
        return message;
    }

    AuthUser authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessageDeliveryException("Thiếu access token.");
        }
        try {
            AuthUser user = jwtService.parse(header.substring(7).trim());
            if (revocations.isRevoked(user)) {
                throw new MessageDeliveryException("Phiên đăng nhập đã bị thu hồi.");
            }
            return user;
        } catch (JwtService.InvalidAccessTokenException e) {
            throw new MessageDeliveryException("Access token không hợp lệ.");
        }
    }

    static boolean allowed(String destination) {
        return destination != null
                && (destination.startsWith("/user/queue/") || destination.equals("/topic/platform"));
    }
}
