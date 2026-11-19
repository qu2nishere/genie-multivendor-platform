package com.genie.support;

import com.genie.auth.oauth.application.OAuthProvider;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
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
    /**
     * Kho S3 cùng image với deploy/docker-compose.local.yml: Silo — fork cộng đồng của MinIO (MinIO đã xóa image
     * minio/minio khỏi Docker Hub, ADR 0009). Silo giữ API S3, đường dẫn /minio/health/*, biến MINIO_* nên dùng được
     * {@link MinIOContainer}. Bucket tạo trong {@link #createBuckets()}.
     */
    protected static final MinIOContainer SILO = new MinIOContainer(
            DockerImageName.parse("pgsty/silo:RELEASE.2026-10-26T00-00-00Z").asCompatibleSubstituteFor("minio/minio"));

    static {
        // JVM Windows / macOS có thể mặc định "Asia/Saigon" — tên PostgreSQL 16 không nhận; dự án lưu UTC.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        POSTGRES.start();
        REDIS.start();
        SILO.start();
        createBuckets();
    }

    /** Bucket cần cho test (bản thu nhỏ của deploy/silo/init.sh). */
    private static void createBuckets() {
        try {
            MinioClient client = siloClient();
            for (String bucket : new String[] {"avatar-assets", "content-assets"}) {
                if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được bucket Silo cho test", e);
        }
    }

    /** Client S3 tới Silo để test kiểm tệp trực tiếp (quyền root của container). */
    protected static MinioClient siloClient() {
        return MinioClient.builder().endpoint(SILO.getS3URL())
                .credentials(SILO.getUserName(), SILO.getPassword()).build();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("genie.storage.endpoint", SILO::getS3URL);
        registry.add("genie.storage.public-url", SILO::getS3URL);
        registry.add("genie.storage.access-key", SILO::getUserName);
        registry.add("genie.storage.secret-key", SILO::getPassword);
        // Danh mục hành chính luôn đọc ảnh chụp JSON trong test (kể cả khi .env của máy dev có GHN_TOKEN)
        registry.add("genie.shipping.ghn.token", () -> "");
        registry.add("genie.shipping.ghn.shop-id", () -> "");
    }

    public static final String START = "2026-06-08T07:00:00Z";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected FakeEmailSender mails;

    @Autowired
    @Qualifier("fakeIdentityProviderClient")
    protected FakeIdentityProviderClient google;

    @Autowired
    @Qualifier("fakeZaloIdentityProviderClient")
    protected FakeIdentityProviderClient zalo;

    @Autowired
    protected FakeAccountDeletionBlocker deletionBlocker;

    @Autowired
    protected FakeAccountContext accountContext;

    @Autowired
    protected FakeReverseGeocoder geocoder;

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
        google.clear();
        zalo.clear();
        deletionBlocker.clear();
        accountContext.clear();
        geocoder.clear();
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

        /** Đứng đầu danh sách {@code IdentityProviderClient} → OAuthLoginService dùng bản giả thay cho Google thật. */
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        FakeIdentityProviderClient fakeIdentityProviderClient() {
            return new FakeIdentityProviderClient();
        }

        /** Zalo giả — cũng đứng trước {@code ZaloIdentityProviderClient} thật. */
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        FakeIdentityProviderClient fakeZaloIdentityProviderClient() {
            return new FakeIdentityProviderClient(OAuthProvider.ZALO);
        }

        @Bean
        @Primary
        FakeAvatarDownloader fakeAvatarDownloader() {
            return new FakeAvatarDownloader();
        }

        /** Giả lập module order (chưa có) chặn xóa tài khoản khi còn đơn — mặc định không chặn. */
        @Bean
        FakeAccountDeletionBlocker fakeAccountDeletionBlocker() {
            return new FakeAccountDeletionBlocker();
        }

        /** Giả lập shop + bộ đếm của các module chưa làm cho {@code GET /me} — mặc định trống. */
        @Bean
        FakeAccountContext fakeAccountContext() {
            return new FakeAccountContext();
        }

        /** {@code @Primary} → thay {@code NominatimGeocoder} (không gọi OpenStreetMap trong test). */
        @Bean
        @Primary
        FakeReverseGeocoder fakeReverseGeocoder() {
            return new FakeReverseGeocoder();
        }
    }
}
