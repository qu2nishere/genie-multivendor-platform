package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.genie.auth.profile.domain.User;
import com.genie.support.IntegrationTestBase;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-AUTH-08 đổi ảnh đại diện qua MinIO thật (plan AUTH A11.01, A11.03, A11.05): xin link → PUT lên MinIO →
 * {@code PATCH /me/profile} lưu URL vào {@code customer_profiles.avatar_url}.
 */
class ProfileAvatarIT extends IntegrationTestBase {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
    private final HttpClient http = HttpClient.newHttpClient();

    private MvcResult presign(String token, String contentType, long size) throws Exception {
        return mvc.perform(post("/api/v1/uploads/presigned-urls").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\":\"AVATAR\",\"file_name\":\"me.png\",\"content_type\":\"" + contentType
                        + "\",\"content_length\":" + size + "}"))
                .andReturn();
    }

    /** Giống trình duyệt: PUT thẳng lên MinIO bằng link đã ký. */
    private int putToMinio(MvcResult presigned, byte[] body) throws Exception {
        HttpRequest put = HttpRequest.newBuilder(URI.create(json(presigned, "$.data.upload_url")))
                .header("Content-Type", json(presigned, "$.data.headers['Content-Type']"))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        return http.send(put, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private MvcResult patchAvatar(String token, String objectKey) throws Exception {
        return mvc.perform(patch("/api/v1/me/profile").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"avatar_object_key\":\"" + objectKey + "\"}"))
                .andReturn();
    }

    private static boolean existsInMinio(String key) throws Exception {
        try {
            minioClient().statObject(StatObjectArgs.builder().bucket("avatar-assets").object(key).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        }
    }

    @Test
    void uploadAvatarAndSaveUrl() throws Exception {
        String email = uniqueEmail();
        User user = createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));

        MvcResult presigned = presign(token, "image/png", PNG.length);
        assertThat(presigned.getResponse().getStatus()).isEqualTo(200);
        String tmpKey = json(presigned, "$.data.object_key");
        assertThat(tmpKey).startsWith("tmp/user/" + user.getId() + "/").endsWith(".png");
        assertThat(json(presigned, "$.data.method")).isEqualTo("PUT");
        assertThat(putToMinio(presigned, PNG)).isEqualTo(200);

        MvcResult updated = patchAvatar(token, tmpKey);
        assertThat(updated.getResponse().getStatus()).isEqualTo(200);
        String finalKey = tmpKey.substring("tmp/".length());
        String expectedUrl = MINIO.getS3URL() + "/avatar-assets/" + finalKey;
        assertThat(json(updated, "$.data.avatar_url")).isEqualTo(expectedUrl);
        assertThat(jdbc.queryForObject("SELECT avatar_url FROM customer_profiles WHERE user_id = ?", String.class,
                user.getId())).isEqualTo(expectedUrl);
        assertThat(existsInMinio(tmpKey)).isFalse();
        assertThat(existsInMinio(finalKey)).isTrue();

        // Ảnh mới thay ảnh cũ → ảnh cũ bị xóa sau khi lưu xong
        MvcResult second = presign(token, "image/jpeg", PNG.length);
        assertThat(putToMinio(second, PNG)).isEqualTo(200);
        assertThat(patchAvatar(token, json(second, "$.data.object_key")).getResponse().getStatus()).isEqualTo(200);
        assertThat(existsInMinio(finalKey)).isFalse();

        MvcResult profile = mvc.perform(get("/api/v1/me/profile").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn();
        assertThat(json(profile, "$.data.avatar_url")).endsWith(".jpg");
        assertThat(json(profile, "$.data.email")).isEqualTo(email);
    }

    @Test
    void rejectsWrongTypeAndOversizedFile() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));

        MvcResult gif = presign(token, "image/gif", 100);
        assertThat(gif.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(gif, "$.details.field")).isEqualTo("content_type");

        MvcResult big = presign(token, "image/png", 2L * 1024 * 1024);
        assertThat(big.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(big, "$.details.max_bytes")).isEqualTo(String.valueOf(1024 * 1024));
    }

    @Test
    void cannotUseAnotherUsersFileOrMissingFile() throws Exception {
        String a = uniqueEmail();
        String b = uniqueEmail();
        createCustomer(a, PASSWORD);
        createCustomer(b, PASSWORD);
        String tokenA = accessTokenOf(login(a, PASSWORD));
        String tokenB = accessTokenOf(login(b, PASSWORD));

        MvcResult presignedA = presign(tokenA, "image/png", PNG.length);
        assertThat(putToMinio(presignedA, PNG)).isEqualTo(200);
        String keyOfA = json(presignedA, "$.data.object_key");

        assertThat(patchAvatar(tokenB, keyOfA).getResponse().getStatus()).isEqualTo(422);      // tệp của người khác
        MvcResult notUploaded = presign(tokenA, "image/png", PNG.length);                      // xin link nhưng không PUT
        assertThat(patchAvatar(tokenA, json(notUploaded, "$.data.object_key")).getResponse().getStatus())
                .isEqualTo(422);
        assertThat(patchAvatar(tokenA, "../../etc/passwd").getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    void presignRequiresLogin() throws Exception {
        assertThat(mvc.perform(post("/api/v1/uploads/presigned-urls").contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\":\"AVATAR\",\"content_type\":\"image/png\",\"content_length\":10}"))
                .andReturn().getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void updateProfileFields() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));

        MvcResult ok = mvc.perform(patch("/api/v1/me/profile").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"full_name\":\"Trần Văn Bình\",\"gender\":\"MALE\",\"date_of_birth\":\"2000-01-02\","
                        + "\"phone_number\":\"\",\"preferred_language\":\"en\"}"))
                .andReturn();
        assertThat(ok.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(ok, "$.data.full_name")).isEqualTo("Trần Văn Bình");
        assertThat(json(ok, "$.data.phone_number")).isNull();
        assertThat(json(ok, "$.data.date_of_birth")).isEqualTo("2000-01-02");

        MvcResult blank = mvc.perform(patch("/api/v1/me/profile").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"full_name\":\"   \"}")).andReturn();
        assertThat(blank.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(blank, "$.code")).isEqualTo("GNE-VAL-4220");
    }
}
