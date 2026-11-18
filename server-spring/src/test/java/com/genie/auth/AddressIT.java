package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.genie.auth.address.application.AddressFacade;
import com.genie.auth.profile.domain.User;
import com.genie.support.IntegrationTestBase;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Sổ địa chỉ + danh mục hành chính 2 cấp (API_SPEC 3.8–3.9, UC-AUTH-09; plan AUTH A12.05, D25). Danh mục đọc
 * {@code regions/regions.json} (34 tỉnh, mã ĐVHC QĐ 19/2025): Thành phố Hồ Chí Minh 79 › Phường Sài Gòn 26740,
 * Phường Bến Thành 26743; Thành phố Hà Nội 1 › Phường Hoàn Kiếm 00070; Thành phố Đồng Nai 75 › Phường Trấn Biên 26041.
 */
class AddressIT extends IntegrationTestBase {

    @Autowired
    private AddressFacade addressFacade;

    private static String body(String name, int provinceId, String wardCode, Boolean isDefault) {
        return """
                {"recipient_name":"%s","phone_number":"0901234567","province_id":%d,
                 "ward_code":"%s","address_line":"25 Nguyễn Huệ","label":"HOME"%s}"""
                .formatted(name, provinceId, wardCode,
                        isDefault == null ? "" : ",\"is_default\":" + isDefault);
    }

    private MvcResult create(String token, String json) throws Exception {
        return mvc.perform(post("/api/v1/me/addresses").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json)).andReturn();
    }

    private MvcResult list(String token) throws Exception {
        return mvc.perform(get("/api/v1/me/addresses").header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn();
    }

    private String signIn() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        return accessTokenOf(login(email, PASSWORD));
    }

    @Test
    void firstAddressBecomesDefaultAndNamesComeFromTheDirectory() throws Exception {
        String token = signIn();
        // client gửi mã đúng nhưng server tự điền tên (không nhận tên từ client)
        MvcResult r = create(token, body("Trần Thị Mai", 79, "26740", false));

        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        assertThat(json(r, "$.data.is_default")).isEqualTo("true");
        assertThat(json(r, "$.data.province.name")).isEqualTo("Thành phố Hồ Chí Minh");
        assertThat(json(r, "$.data.ward.code")).isEqualTo("26740");
        assertThat(json(r, "$.data.ward.name")).isEqualTo("Phường Sài Gòn");
        assertThat(r.getResponse().getContentAsString()).doesNotContain("district");
        assertThat(json(r, "$.data.label")).isEqualTo("HOME");
        long id = Long.parseLong(json(r, "$.data.id"));

        // địa giới mới không còn cấp huyện → cột quận để NULL (V16)
        assertThat(jdbc.queryForObject("SELECT district_id FROM addresses WHERE id = ?", Integer.class, id)).isNull();
        Long defaultId = jdbc.queryForObject(
                "SELECT p.default_address_id FROM customer_profiles p JOIN addresses a ON a.user_id = p.user_id "
                        + "WHERE a.id = ?", Long.class, id);
        assertThat(defaultId).isEqualTo(id);
    }

    @Test
    void settingAnotherDefaultMovesTheFlagAndDefaultCannotBeDeleted() throws Exception {
        String token = signIn();
        long first = Long.parseLong(json(create(token, body("A", 79, "26740", null)), "$.data.id"));
        long second = Long.parseLong(json(create(token, body("B", 79, "26743", null)), "$.data.id"));

        MvcResult list = list(token);
        assertThat(json(list, "$.data.length()")).isEqualTo("2");
        assertThat(json(list, "$.data[0].id")).isEqualTo(String.valueOf(first));          // mặc định đứng đầu

        // địa chỉ mặc định không xóa được
        MvcResult blocked = mvc.perform(delete("/api/v1/me/addresses/" + first)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn();
        assertThat(blocked.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(blocked, "$.code")).isEqualTo("GNE-USR-4092");

        MvcResult patched = mvc.perform(patch("/api/v1/me/addresses/" + second)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"is_default\":true}")).andReturn();
        assertThat(patched.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(patched, "$.data.is_default")).isEqualTo("true");

        MvcResult after = list(token);
        assertThat(json(after, "$.data[0].id")).isEqualTo(String.valueOf(second));
        assertThat(json(after, "$.data[1].is_default")).isEqualTo("false");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM addresses WHERE id IN (?, ?) AND is_default = true",
                Integer.class, first, second)).isEqualTo(1);

        // địa chỉ cũ giờ xóa được (xóa mềm) và biến mất khỏi danh sách
        assertThat(mvc.perform(delete("/api/v1/me/addresses/" + first)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn().getResponse().getStatus())
                .isEqualTo(204);
        assertThat(json(list(token), "$.data.length()")).isEqualTo("1");
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM addresses WHERE id = ?", Boolean.class, first))
                .isTrue();
    }

    @Test
    void wrongRegionHierarchyIsRejected() throws Exception {
        String token = signIn();
        // phường của Hà Nội gắn với TP. HCM
        MvcResult r = create(token, body("A", 79, "00070", null));
        assertThat(r.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(r, "$.code")).isEqualTo("GNE-VAL-4220");
        // mã tỉnh / mã phường cũ (trước sáp nhập) không còn hợp lệ
        assertThat(create(token, body("A", 202, "20308", null)).getResponse().getStatus()).isEqualTo(422);
        // số điện thoại sai định dạng
        String badPhone = body("A", 79, "26740", null).replace("0901234567", "12345");
        assertThat(create(token, badPhone).getResponse().getStatus()).isEqualTo(422);
        assertThat(json(list(token), "$.data.length()")).isEqualTo("0");
    }

    @Test
    void cannotTouchAnotherUsersAddress() throws Exception {
        String owner = signIn();
        long id = Long.parseLong(json(create(owner, body("Chủ", 79, "26740", null)), "$.data.id"));
        String other = signIn();

        assertThat(mvc.perform(put("/api/v1/me/addresses/" + id).header(HttpHeaders.AUTHORIZATION, bearer(other))
                .contentType(MediaType.APPLICATION_JSON).content(body("Kẻ lạ", 79, "26740", null)))
                .andReturn().getResponse().getStatus()).isEqualTo(404);
        assertThat(mvc.perform(patch("/api/v1/me/addresses/" + id).header(HttpHeaders.AUTHORIZATION, bearer(other))
                .contentType(MediaType.APPLICATION_JSON).content("{\"is_default\":true}"))
                .andReturn().getResponse().getStatus()).isEqualTo(404);
        assertThat(mvc.perform(delete("/api/v1/me/addresses/" + id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andReturn().getResponse().getStatus()).isEqualTo(404);
        assertThat(json(list(owner), "$.data[0].recipient_name")).isEqualTo("Chủ");
    }

    @Test
    void updateChangesEverythingAndSnapshotIsReadyForCheckout() throws Exception {
        String email = uniqueEmail();
        User user = createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));
        long id = Long.parseLong(json(create(token, body("A", 79, "26740", null)), "$.data.id"));

        MvcResult updated = mvc.perform(put("/api/v1/me/addresses/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content(body("Nguyễn Văn B", 1, "00070", null).replace("\"HOME\"", "\"OFFICE\"")))
                .andReturn();
        assertThat(updated.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(updated, "$.data.province.name")).isEqualTo("Thành phố Hà Nội");
        assertThat(json(updated, "$.data.ward.name")).isEqualTo("Phường Hoàn Kiếm");
        assertThat(json(updated, "$.data.label")).isEqualTo("OFFICE");
        assertThat(json(updated, "$.data.is_default")).isEqualTo("true");                  // PUT không bỏ cờ

        var snapshot = addressFacade.snapshotForCheckout(user.getId(), id).orElseThrow();
        assertThat(snapshot.fullAddress()).isEqualTo("25 Nguyễn Huệ, Phường Hoàn Kiếm, Thành phố Hà Nội");
        assertThat(snapshot.provinceId()).isEqualTo(1);
        assertThat(snapshot.wardCode()).isEqualTo("00070");
        assertThat(addressFacade.snapshotForCheckout(user.getId() + 999, id)).isEmpty();
        assertThat(addressFacade.defaultFor(user.getId())).isPresent();
    }

    @Test
    void concurrentDefaultChangesKeepExactlyOneDefault() throws Exception {
        String token = signIn();
        long a = Long.parseLong(json(create(token, body("A", 79, "26740", null)), "$.data.id"));
        long b = Long.parseLong(json(create(token, body("B", 79, "26743", null)), "$.data.id"));
        long c = Long.parseLong(json(create(token, body("C", 1, "00070", null)), "$.data.id"));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> setB = () -> mvc.perform(patch("/api/v1/me/addresses/" + b)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"is_default\":true}")).andReturn().getResponse().getStatus();
            Callable<Integer> setC = () -> mvc.perform(patch("/api/v1/me/addresses/" + c)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"is_default\":true}")).andReturn().getResponse().getStatus();
            Future<Integer> f1 = pool.submit(setB);
            Future<Integer> f2 = pool.submit(setC);
            assertThat(f1.get()).isEqualTo(200);
            assertThat(f2.get()).isEqualTo(200);
        } finally {
            pool.shutdown();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM addresses WHERE id IN (?, ?, ?) AND is_default = true",
                Integer.class, a, b, c)).isEqualTo(1);
    }

    @Test
    void regionsArePublicAndCacheable() throws Exception {
        MvcResult provinces = mvc.perform(get("/api/v1/public/regions/provinces")).andReturn();
        assertThat(provinces.getResponse().getStatus()).isEqualTo(200);
        assertThat(provinces.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("max-age=86400")
                .contains("public");
        String etag = provinces.getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(etag).isNotBlank();
        assertThat(json(provinces, "$.data.length()")).isEqualTo("34");

        assertThat(mvc.perform(get("/api/v1/public/regions/provinces").header(HttpHeaders.IF_NONE_MATCH, etag))
                .andReturn().getResponse().getStatus()).isEqualTo(304);

        MvcResult wards = mvc.perform(get("/api/v1/public/regions/provinces/79/wards")).andReturn();
        assertThat(json(wards, "$.data.length()")).isEqualTo("168");                       // TP. HCM sau sáp nhập
        assertThat(wards.getResponse().getContentAsString()).contains("\"code\":\"26740\"");
        assertThat(json(mvc.perform(get("/api/v1/public/regions/provinces/123456/wards")).andReturn(),
                "$.data.length()")).isEqualTo("0");
        // cấp quận / huyện đã bỏ
        assertThat(mvc.perform(get("/api/v1/public/regions/provinces/79/districts")).andReturn().getResponse()
                .getStatus()).isEqualTo(404);
    }

    @Test
    void reverseGeocodeMatchesNamesToTwoLevelRegions() throws Exception {
        // tên kiểu OpenStreetMap theo địa giới mới → tỉnh + phường
        geocoder.answer(new com.genie.auth.address.application.ReverseGeocoder.GeoPlace(
                java.util.List.of("Phường Sài Gòn", "Thành phố Hồ Chí Minh"), "72 Lê Thánh Tôn"));
        MvcResult r = mvc.perform(get("/api/v1/public/regions/reverse-geocode")
                .param("lat", "10.7769").param("lng", "106.7009")).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(r, "$.data.province.id")).isEqualTo("79");
        assertThat(json(r, "$.data.ward.code")).isEqualTo("26740");
        assertThat(json(r, "$.data.address_line")).isEqualTo("72 Lê Thánh Tôn");
        assertThat(r.getResponse().getContentAsString()).doesNotContain("district");

        // OSM còn tên cũ ("Tỉnh Đồng Nai", "Thành phố Biên Hòa") nhưng phường đã là tên mới → vẫn khớp
        geocoder.answer(new com.genie.auth.address.application.ReverseGeocoder.GeoPlace(
                java.util.List.of("Phường Trấn Biên", "Thành phố Biên Hòa", "Tỉnh Đồng Nai"), ""));
        MvcResult dn = mvc.perform(get("/api/v1/public/regions/reverse-geocode")
                .param("lat", "10.95").param("lng", "106.82")).andReturn();
        assertThat(json(dn, "$.data.province.id")).isEqualTo("75");
        assertThat(json(dn, "$.data.ward.code")).isEqualTo("26041");

        // OSM còn tên phường cũ (P. Bến Nghé, Quận 1) → khớp được tỉnh, phường để người dùng tự chọn
        geocoder.answer(new com.genie.auth.address.application.ReverseGeocoder.GeoPlace(
                java.util.List.of("Phường Bến Nghé", "Quận 1", "Thành phố Hồ Chí Minh"), "1 Lê Lợi"));
        MvcResult partial = mvc.perform(get("/api/v1/public/regions/reverse-geocode")
                .param("lat", "10.77").param("lng", "106.70")).andReturn();
        assertThat(json(partial, "$.data.province.id")).isEqualTo("79");
        assertThat(partial.getResponse().getContentAsString()).doesNotContain("\"ward\":{");

        // tọa độ sai
        assertThat(mvc.perform(get("/api/v1/public/regions/reverse-geocode").param("lat", "200").param("lng", "0"))
                .andReturn().getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    void allAddressLabelsAreStored() throws Exception {
        String token = signIn();
        for (String label : java.util.List.of("HOME", "OFFICE", "SCHOOL", "APARTMENT", "OTHER")) {
            // mỗi loại một số nhà khác nhau — cùng địa chỉ thì bị chặn trùng (A12.07)
            String json = body("A", 79, "26740", null).replace("\"HOME\"", "\"" + label + "\"")
                    .replace("25 Nguyễn Huệ", label + " Nguyễn Huệ");
            MvcResult r = create(token, json);
            assertThat(r.getResponse().getStatus()).isEqualTo(201);
            assertThat(json(r, "$.data.label")).isEqualTo(label);
            assertThat(jdbc.queryForObject("SELECT label FROM addresses WHERE id = ?", String.class,
                    Long.parseLong(json(r, "$.data.id")))).isEqualTo(label);
        }
        // loại không hỗ trợ → bị từ chối, không ghi gì
        MvcResult bad = create(token, body("A", 79, "26740", null).replace("\"HOME\"", "\"GARDEN\""));
        assertThat(bad.getResponse().getStatus()).isBetween(400, 422);
        assertThat(json(list(token), "$.data.length()")).isEqualTo("5");
    }

    @Test
    void duplicateAddressIsRejected() throws Exception {
        String token = signIn();
        long first = Long.parseLong(json(create(token, body("Mai", 79, "26740", null)), "$.data.id"));

        // cùng tỉnh + phường + SĐT, địa chỉ cụ thể chỉ khác dấu / hoa thường / dấu câu / khoảng trắng; khác tên + loại
        String same = body("Người khác", 79, "26740", null)
                .replace("25 Nguyễn Huệ", "25  nguyen hue,").replace("\"HOME\"", "\"OFFICE\"");
        MvcResult dup = create(token, same);
        assertThat(dup.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(dup, "$.code")).isEqualTo("GNE-USR-4093");
        assertThat(json(dup, "$.details.existing_address_id")).isEqualTo(String.valueOf(first));
        assertThat(json(list(token), "$.data.length()")).isEqualTo("1");

        // khác SĐT hoặc khác phường → không phải trùng
        long second = Long.parseLong(json(create(token, body("Mai", 79, "26743", null)), "$.data.id"));
        assertThat(create(token, body("Mai", 79, "26740", null).replace("0901234567", "0911111111"))
                .getResponse().getStatus()).isEqualTo(201);

        // sửa giữ nguyên chính nó → lưu được; sửa thành bản sao của địa chỉ khác → 409
        assertThat(mvc.perform(put("/api/v1/me/addresses/" + first).header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(body("Mai Trần", 79, "26740", null)))
                .andReturn().getResponse().getStatus()).isEqualTo(200);
        MvcResult dupEdit = mvc.perform(put("/api/v1/me/addresses/" + second)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content(body("Mai", 79, "26740", null))).andReturn();
        assertThat(dupEdit.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(dupEdit, "$.details.existing_address_id")).isEqualTo(String.valueOf(first));

        // địa chỉ đã xóa không tính: xóa bản không mặc định rồi thêm lại được
        assertThat(mvc.perform(delete("/api/v1/me/addresses/" + second)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn().getResponse().getStatus())
                .isEqualTo(204);
        assertThat(create(token, body("Mai", 79, "26743", null)).getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    void concurrentDuplicateCreatesKeepOneAddress() throws Exception {
        String token = signIn();
        create(token, body("Khác", 1, "00070", null));                     // đã có hồ sơ + 1 địa chỉ
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> add = () -> create(token, body("Mai", 79, "26740", null)).getResponse().getStatus();
            Future<Integer> f1 = pool.submit(add);
            Future<Integer> f2 = pool.submit(add);
            assertThat(java.util.List.of(f1.get(), f2.get())).containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdown();
        }
        assertThat(json(list(token), "$.data.length()")).isEqualTo("2");
    }

    @Test
    void deliveryNoteIsStoredTrimmedAndCopiedToSnapshot() throws Exception {
        String email = uniqueEmail();
        User user = createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));
        String withNote = body("Mai", 79, "26740", null)
                .replace("}", ",\"delivery_note\":\"  Gần cổng ĐH NEU,   gửi phòng bảo vệ  \"}");
        MvcResult r = create(token, withNote);
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        assertThat(json(r, "$.data.delivery_note")).isEqualTo("Gần cổng ĐH NEU, gửi phòng bảo vệ");
        long id = Long.parseLong(json(r, "$.data.id"));
        assertThat(addressFacade.snapshotForCheckout(user.getId(), id).orElseThrow().deliveryNote())
                .isEqualTo("Gần cổng ĐH NEU, gửi phòng bảo vệ");

        // sửa với ghi chú rỗng → xóa ghi chú; khác ghi chú không làm địa chỉ thành "khác" (vẫn bị chặn trùng)
        MvcResult cleared = mvc.perform(put("/api/v1/me/addresses/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content(body("Mai", 79, "26740", null).replace("}", ",\"delivery_note\":\"   \"}")))
                .andReturn();
        assertThat(cleared.getResponse().getStatus()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT delivery_note FROM addresses WHERE id = ?", String.class, id))
                .isNull();
        assertThat(create(token, withNote).getResponse().getStatus()).isEqualTo(409);

        // quá 200 ký tự → 422
        String tooLong = body("Mai", 79, "26743", null)
                .replace("}", ",\"delivery_note\":\"" + "a".repeat(201) + "\"}");
        assertThat(create(token, tooLong).getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    void addressesRequireSignIn() throws Exception {
        assertThat(mvc.perform(get("/api/v1/me/addresses")).andReturn().getResponse().getStatus()).isEqualTo(401);
    }
}
