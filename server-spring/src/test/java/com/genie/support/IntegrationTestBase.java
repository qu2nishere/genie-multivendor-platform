package com.genie.support;

import com.genie.auth.profile.domain.CustomerProfile;
import com.genie.auth.profile.domain.CustomerProfileRepository;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Cơ sở test tích hợp (*IT): toàn bộ ứng dụng + PostgreSQL 16 + Redis 7 thật (Testcontainers), MockMvc
 * (TS 14, plan AUTH A01.08). Container khởi động MỘT lần cho mọi lớp test (singleton) — nhanh hơn @Container mỗi lớp.
 * <ul>
 *   <li>{@link #clock} tua được thời gian; {@link #mails} ghi lại email thay cho SMTP;</li>
 *   <li>trước mỗi test: xóa Redis, xóa phiên đăng nhập / nhật ký, đặt lại đồng hồ (dữ liệu mẫu R__seed_demo giữ nguyên).</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestBase.TestBeans.class)
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        // JVM Windows / macOS có thể mặc định "Asia/Saigon" — tên PostgreSQL 16 không nhận; dự án lưu UTC.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
    }

    public static final String START = "2026-06-08T07:00:00Z";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected FakeEmailSender mails;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected StringRedisTemplate redis;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected CustomerProfileRepository customerProfileRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    /** Mật khẩu dùng cho tài khoản tạo trong test. */
    protected static final String PASSWORD = "Genie@123";

    /** Email ngẫu nhiên — mỗi test một tài khoản, không ảnh hưởng nhau (dữ liệu CSDL không bị xóa giữa các test). */
    protected static String uniqueEmail() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@genie.test";
    }

    /** Tạo người mua đã xác minh email, có hồ sơ. */
    protected User createCustomer(String email, String password) {
        User user = userRepository.saveAndFlush(User.newCustomer(email,
                password == null ? null : passwordEncoder.encode(password), OffsetDateTime.now(clock)));
        customerProfileRepository.saveAndFlush(CustomerProfile.create(user, "Người dùng test", "0901234567", "vi"));
        return user;
    }

    /** {@code POST /api/v1/auth/sessions} — trả kết quả để test đọc body / cookie. */
    protected MvcResult login(String email, String password) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn();
    }

    protected static String accessTokenOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.access_token");
    }

    protected static Cookie refreshCookieOf(MvcResult result) {
        return result.getResponse().getCookie("genie_rt");
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected static String json(MvcResult result, String path) throws Exception {
        Object value = JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), path);
        return value == null ? null : value.toString();
    }

    @BeforeEach
    void resetState() {
        clock.set(Instant.parse(START));
        mails.clear();
        var connectionFactory = redis.getConnectionFactory();
        if (connectionFactory != null) {
            connectionFactory.getConnection().serverCommands().flushAll();
        }
        jdbc.update("DELETE FROM refresh_tokens");
        jdbc.update("DELETE FROM system_logs");
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class TestBeans {

        /** {@code @Primary} → mọi nơi inject {@link Clock} nhận đồng hồ này thay cho {@code ClockConfig#clock}. */
        @Bean
        @Primary
        MutableClock testClock() {
            return MutableClock.startingAt(START);
        }

        @Bean
        @Primary
        FakeEmailSender fakeEmailSender() {
            return new FakeEmailSender();
        }
    }
}
