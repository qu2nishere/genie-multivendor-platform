package com.genie.common.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Redis dùng chung (TECH_STACK 16.5, LUU_TRU 4): OTP, đăng ký tạm, rate limit, denylist JWT, khóa tài khoản.
 * <p>
 * Kết nối (host / port / password) lấy từ {@code spring.data.redis.*} trong {@code application*.yml}; lớp này chỉ đặt
 * các giá trị mà file cấu hình chưa có, bằng hằng số trong code:
 * <ul>
 *   <li>timeout lệnh 2 giây (mặc định của Lettuce là 60 giây — Redis chết thì request treo cả phút);</li>
 *   <li>timeout mở kết nối 2 giây;</li>
 *   <li>mất kết nối → từ chối lệnh ngay thay vì xếp hàng chờ, để service trả {@code 503 GNE-SYS-5030} sớm;
 *       Lettuce vẫn tự kết nối lại khi Redis sống lại.</li>
 * </ul>
 * Mọi khóa dùng {@code StringRedisTemplate} (Spring Boot tự tạo) — giá trị là chuỗi / hash chuỗi, JSON tự ghi khi cần,
 * để đọc được bằng {@code redis-cli} khi gỡ lỗi.
 */
@Configuration(proxyBeanMethods = false)
public class RedisConfig {

    static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(2);
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    static final Duration SHUTDOWN_TIMEOUT = Duration.ofMillis(100);

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Bean
    LettuceClientConfigurationBuilderCustomizer genieLettuceCustomizer() {
        return builder -> builder
                .commandTimeout(COMMAND_TIMEOUT)
                .shutdownTimeout(SHUTDOWN_TIMEOUT)
                .clientOptions(ClientOptions.builder()
                        .autoReconnect(true)
                        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                        .socketOptions(SocketOptions.builder().connectTimeout(CONNECT_TIMEOUT).build())
                        .build());
    }

    /**
     * Kiểm tra Redis lúc khởi động: không chặn ứng dụng (health {@code /actuator/health} vẫn báo DOWN),
     * chỉ ghi log rõ ràng để dev biết vì sao đăng ký / OTP trả 503.
     */
    @Bean
    ApplicationRunner redisStartupCheck(RedisConnectionFactory connectionFactory) {
        return args -> {
            try (RedisConnection connection = connectionFactory.getConnection()) {
                log.info("Redis OK ({})", connection.ping());
            } catch (RuntimeException e) {
                log.warn("Không kết nối được Redis — đăng ký, OTP, đăng nhập sẽ trả 503 cho tới khi Redis chạy. "
                        + "Kiểm tra REDIS_HOST / REDIS_PORT / REDIS_PASSWORD. Lỗi: {}", e.getMessage());
            }
        };
    }
}
