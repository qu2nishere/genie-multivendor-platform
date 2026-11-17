package com.genie.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Nạp tệp {@code .env} của máy dev (khóa bí mật: SMTP_*, MAIL_FROM, GOOGLE_CLIENT_*…) để không phải nhập tay vào Run
 * Configuration của IntelliJ và không ghi vào {@code application*.yml}.
 * <ul>
 *   <li>Tìm {@code .env} ở thư mục chạy rồi đi ngược lên tối đa 3 cấp — chạy từ {@code server-spring/} (IntelliJ,
 *       {@code ./mvnw}) sẽ thấy {@code .env} ở thư mục gốc dự án (cạnh {@code .env.example}).</li>
 *   <li>Độ ưu tiên thấp: biến môi trường thật, tham số dòng lệnh, Run Configuration đều ghi đè được.</li>
 *   <li>Chạy <b>trước</b> {@link ConfigDataEnvironmentPostProcessor} (nơi Spring đọc {@code application*.yml} và chọn
 *       profile) nên {@code SPRING_PROFILES_ACTIVE=local} trong {@code .env} có tác dụng — không cần đặt profile
 *       trong IntelliJ. Profile đặt bằng dòng lệnh / {@code @ActiveProfiles} của test vẫn được ưu tiên.</li>
 *   <li>Không có tệp → bỏ qua (CI, Docker, VPS dùng biến môi trường như cũ). Tệp {@code .env} bị {@code .gitignore}
 *       chặn — không bao giờ lên git.</li>
 * </ul>
 * Cú pháp: {@code KHOA=gia_tri} mỗi dòng; bỏ dòng trống, dòng {@code #}; bỏ tiền tố {@code export}; giá trị có thể bọc
 * {@code "…"} hoặc {@code '…'}.
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String SOURCE_NAME = "dotenv";
    private static final int MAX_PARENT_LEVELS = 3;
    /** Tên biến kiểu môi trường → khóa Spring cần có trước khi chọn profile (MapPropertySource không tự đổi tên). */
    private static final Map<String, String> SPRING_ALIASES = Map.of(
            "SPRING_PROFILES_ACTIVE", "spring.profiles.active");

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER - 1;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path file = find(Path.of("").toAbsolutePath());
        if (file == null) {
            return;
        }
        try {
            Map<String, Object> values = withSpringAliases(parse(Files.readAllLines(file, StandardCharsets.UTF_8)));
            if (!values.isEmpty()) {
                environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, values));
            }
        } catch (IOException e) {
            // không đọc được .env → chạy tiếp với biến môi trường sẵn có
        }
    }

    static Path find(Path start) {
        Path dir = start;
        for (int i = 0; i <= MAX_PARENT_LEVELS && dir != null; i++) {
            Path candidate = dir.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }

    static Map<String, Object> withSpringAliases(Map<String, Object> values) {
        SPRING_ALIASES.forEach((envKey, springKey) -> {
            Object value = values.get(envKey);
            if (value != null) {
                values.putIfAbsent(springKey, value);
            }
        });
        return values;
    }

    static Map<String, Object> parse(List<String> lines) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).strip();
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).strip();
            String value = line.substring(eq + 1).strip();
            if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'")))) {
                value = value.substring(1, value.length() - 1);
            }
            if (!key.isEmpty() && !value.isEmpty()) {
                values.put(key, value);
            }
        }
        return values;
    }
}
