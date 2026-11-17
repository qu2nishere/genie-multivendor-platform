package com.genie.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** Tiện ích chung cho test banner (plan banner B03–B05). Mỗi test bắt đầu với bảng {@code home_banners} trống. */
abstract class BannerTestSupport extends AuditLogTestSupport {

    protected static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
    protected static final Instant NOW = Instant.parse(START);
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void cleanBanners() {
        jdbc.update("DELETE FROM home_banners");
    }

    /** Xin link + PUT ảnh lên Silo như trình duyệt; trả khóa {@code tmp/…}. */
    protected String uploadImage(String token) throws Exception {
        var r = mvc.perform(post("/api/v1/uploads/presigned-urls").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\":\"BANNER_IMAGE\",\"content_type\":\"image/png\",\"content_length\":" + PNG.length
                        + "}")).andReturn();
        String body = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String url = JsonPath.read(body, "$.data.upload_url");
        HttpRequest put = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "image/png")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(PNG)).build();
        http.send(put, HttpResponse.BodyHandlers.discarding());
        return JsonPath.read(body, "$.data.object_key");
    }

    protected static boolean existsInSilo(String key) throws Exception {
        try {
            siloClient().statObject(StatObjectArgs.builder().bucket("content-assets").object(key).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        }
    }

    /** Thân JSON tạo / sửa banner. {@code image} null = bỏ trường. */
    protected static String body(String title, String image, String target, String position, Long categoryId,
                                 Instant startsAt, Instant endsAt, boolean active) {
        return "{\"title\":\"" + title + "\""
                + (image == null ? "" : ",\"image_object_key\":\"" + image + "\"")
                + ",\"target_url\":\"" + target + "\",\"position\":\"" + position + "\""
                + ",\"category_id\":" + categoryId + ",\"sort_order\":1"
                + ",\"starts_at\":\"" + startsAt + "\",\"ends_at\":\"" + endsAt + "\",\"is_active\":" + active + "}";
    }

    /** Chèn thẳng banner (dựng dữ liệu nhanh) — trả id. */
    protected long insertBanner(String title, String position, Long categoryId, Instant startsAt, Instant endsAt,
                                boolean active, int sortOrder) {
        Long admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        return jdbc.queryForObject("INSERT INTO home_banners (title, image_url, target_url, position, category_id, "
                        + "sort_order, starts_at, ends_at, is_active, created_by_admin_id, impression_count, click_count, "
                        + "created_at, updated_at) VALUES (?, 'seed/banner/home-tech.jpg', '/discover', ?, ?, ?, ?, ?, ?, ?, "
                        + "0, 0, now(), now()) RETURNING id", Long.class,
                title, position, categoryId, sortOrder, Timestamp.from(startsAt), Timestamp.from(endsAt), active, admin);
    }
}
