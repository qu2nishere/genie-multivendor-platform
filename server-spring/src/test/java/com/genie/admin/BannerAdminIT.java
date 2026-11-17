package com.genie.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.domain.UserRole;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** UC-ADM-12 — quản trị banner qua API (plan banner B03.08, B03.09). */
class BannerAdminIT extends BannerTestSupport {

    private static final String URL = "/api/v1/admin/banners";
    private static final Instant FROM = NOW.minus(1, ChronoUnit.DAYS);
    private static final Instant TO = NOW.plus(10, ChronoUnit.DAYS);

    private MvcResult create(String token, String json) throws Exception {
        return mvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                .content(json)).andReturn();
    }

    private static String str(MvcResult r, String path) throws Exception {
        Object v = JsonPath.read(r.getResponse().getContentAsString(StandardCharsets.UTF_8), path);
        return v == null ? null : v.toString();
    }

    private int bannerAudits() {
        return jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'BANNER_CHANGED'", Integer.class);
    }

    @Test
    void createsBannerWithPromotedImageAndAuditLog() throws Exception {
        String token = token("linh.tm@genie.example", UserRole.STAFF);
        String tmp = uploadImage(token);
        clearLogs();
        MvcResult r = create(token, body("Siêu sale", tmp, "/flash-sale", "HOME_TOP_CAROUSEL", null, FROM, TO, true));
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        String id = str(r, "$.data.id");
        assertThat(str(r, "$.data.state")).isEqualTo("LIVE");
        assertThat(str(r, "$.data.image_url")).contains("/content-assets/banner/" + id + "/desktop-").endsWith(".png");
        String key = jdbc.queryForObject("SELECT image_url FROM home_banners WHERE id = ?", String.class, Long.parseLong(id));
        assertThat(key).startsWith("banner/" + id + "/desktop-");
        assertThat(existsInSilo(key)).isTrue();
        assertThat(existsInSilo(tmp)).isFalse();
        assertThat(bannerAudits()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT details FROM system_logs WHERE action_type = 'BANNER_CHANGED'", String.class))
                .contains("\"op\":\"CREATE\"").contains("BN-" + id);
    }

    @Test
    void rejectsMissingFieldsWithFieldErrors() throws Exception {
        String token = adminToken();
        mvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", null, "/c/dien-tu", "CATEGORY_TOP", null, TO, FROM, true)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-VAL-4220"))
                .andExpect(jsonPath("$.errors[*].field").value(
                        org.hamcrest.Matchers.containsInAnyOrder("image_object_key", "category_id", "ends_at")));
        mvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "tmp/user/1/x.png", "//evil.com", "POPUP", null, FROM, TO, true)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-BNR-4222"));
    }

    @Test
    void ninthOverlappingBannerIsRejectedButDisjointScheduleIsAllowed() throws Exception {
        for (int i = 0; i < 8; i++) {
            insertBanner("P" + i, "POPUP", null, FROM, TO, true, i);
        }
        insertBanner("off", "POPUP", null, FROM, TO, false, 9);   // banner tắt không tính
        String token = adminToken();
        assertThat(create(token, body("Thứ 9", uploadImage(token), "/discover", "POPUP", null, FROM, TO, true))
                .getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("GNE-BNR-4221");
        MvcResult later = create(token, body("Sau đó", uploadImage(token), "/discover", "POPUP", null,
                TO.plus(1, ChronoUnit.DAYS), TO.plus(5, ChronoUnit.DAYS), true));
        assertThat(later.getResponse().getStatus()).isEqualTo(201);
        assertThat(str(later, "$.data.state")).isEqualTo("SCHEDULED");
        // bật lại banner đang tắt cũng bị chặn
        long off = jdbc.queryForObject("SELECT id FROM home_banners WHERE title = 'off'", Long.class);
        mvc.perform(patch(URL + "/" + off).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"is_active\":true}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("GNE-BNR-4221"));
    }

    @Test
    void updateKeepsOrReplacesImage() throws Exception {
        String token = adminToken();
        MvcResult r = create(token, body("Gốc", uploadImage(token), "/discover", "HOME_MIDDLE", null, FROM, TO, true));
        long id = Long.parseLong(str(r, "$.data.id"));
        String firstKey = jdbc.queryForObject("SELECT image_url FROM home_banners WHERE id = ?", String.class, id);

        mvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Đổi tên", null, "/c/nha-cua", "HOME_MIDDLE", null, FROM, TO, true)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("Đổi tên"));
        assertThat(jdbc.queryForObject("SELECT image_url FROM home_banners WHERE id = ?", String.class, id)).isEqualTo(firstKey);

        mvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Đổi ảnh", uploadImage(token), "/c/nha-cua", "HOME_MIDDLE", null, FROM, TO, true)))
                .andExpect(status().isOk());
        String secondKey = jdbc.queryForObject("SELECT image_url FROM home_banners WHERE id = ?", String.class, id);
        assertThat(secondKey).isNotEqualTo(firstKey).startsWith("banner/" + id + "/desktop-");
        assertThat(existsInSilo(secondKey)).isTrue();
        assertThat(existsInSilo(firstKey)).isFalse();
        assertThat(jdbc.queryForObject("SELECT details FROM system_logs WHERE action_type = 'BANNER_CHANGED' "
                + "AND details LIKE '%UPDATE%' ORDER BY created_at DESC LIMIT 1", String.class)).contains("changes");
    }

    @Test
    void toggleAndDeleteRules() throws Exception {
        String token = adminToken();
        long live = insertBanner("live", "HOME_TOP_SIDE", null, FROM, TO, true, 1);
        long scheduled = insertBanner("sched", "HOME_TOP_SIDE", null, TO, TO.plus(3, ChronoUnit.DAYS), true, 2);
        long ended = insertBanner("ended", "HOME_TOP_SIDE", null, FROM.minus(9, ChronoUnit.DAYS), FROM, true, 3);
        clearLogs();

        mvc.perform(delete(URL + "/" + live).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GNE-BNR-4091"));
        mvc.perform(delete(URL + "/" + scheduled).header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isConflict());
        mvc.perform(delete(URL + "/" + ended).header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isNoContent());

        mvc.perform(patch(URL + "/" + live).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"is_active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.state").value("OFF"));
        mvc.perform(delete(URL + "/" + live).header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isNoContent());
        mvc.perform(delete(URL + "/999999").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isNotFound());
        // seed/… không bị xóa khỏi Silo (không có thật trong test — chỉ cần không lỗi) và mỗi thao tác 1 dòng nhật ký
        assertThat(bannerAudits()).isEqualTo(3);
    }

    @Test
    void listsWithFilterSearchAndPaging() throws Exception {
        insertBanner("Công nghệ chính hãng", "HOME_TOP_CAROUSEL", null, FROM, TO, true, 1);
        insertBanner("Làm đẹp mỗi ngày", "HOME_TOP_CAROUSEL", null, FROM, TO, false, 2);
        long cat = jdbc.queryForObject("INSERT INTO categories (name, slug, is_active, requires_business_license) "
                + "VALUES ('Điện tử test', 'dien-tu-test-' || gen_random_uuid(), TRUE, FALSE) RETURNING id", Long.class);
        insertBanner("Shop nổi bật", "CATEGORY_TOP", cat, FROM, TO, true, 1);
        String token = adminToken();
        mvc.perform(get(URL).param("position", "HOME_TOP_CAROUSEL").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].state").value("OFF"))
                .andExpect(jsonPath("$.pagination.total_items").value(2))
                .andExpect(jsonPath("$.pagination.page").value(1));
        mvc.perform(get(URL).param("q", "LÀM ĐẸP").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(get(URL).param("position", "CATEGORY_TOP").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data[0].category.id").value(cat))
                .andExpect(jsonPath("$.data[0].image_url").value(org.hamcrest.Matchers.endsWith(
                        "/content-assets/seed/banner/home-tech.jpg")));
        mvc.perform(get(URL).param("size", "1").param("page", "2").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.pagination.total_pages").value(3));
    }

    @Test
    void onlyManageVoucherHoldersCanManage() throws Exception {
        for (String t : List.of(adminToken(), token("linh.tm@genie.example", UserRole.STAFF))) {
            mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, t)).andExpect(status().isOk());
        }
        for (String t : List.of(token("phat.nt@genie.example", UserRole.STAFF),
                token("truc.ht@genie.example", UserRole.STAFF), token("mai.tran@gmail.com", UserRole.CUSTOMER))) {
            mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, t))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("GNE-SEC-4030"));
        }
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
