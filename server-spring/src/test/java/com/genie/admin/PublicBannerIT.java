package com.genie.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** UC-ADM-12 — banner công khai trên storefront + cache Redis (plan banner B04.05). */
class PublicBannerIT extends BannerTestSupport {

    private static final String URL = "/api/v1/public/banners";
    private static final Instant FROM = NOW.minus(1, ChronoUnit.DAYS);
    private static final Instant TO = NOW.plus(10, ChronoUnit.DAYS);

    private long category() {
        return jdbc.queryForObject("INSERT INTO categories (name, slug, is_active, requires_business_license) "
                + "VALUES ('Ngành test', 'nganh-test-' || gen_random_uuid(), TRUE, FALSE) RETURNING id", Long.class);
    }

    @Test
    void returnsOnlyLiveBannersInSortOrderPerPosition() throws Exception {
        insertBanner("thứ hai", "HOME_TOP_CAROUSEL", null, FROM, TO, true, 2);
        insertBanner("thứ nhất", "HOME_TOP_CAROUSEL", null, FROM, TO, true, 1);
        insertBanner("hẹn giờ", "HOME_TOP_CAROUSEL", null, TO, TO.plus(1, ChronoUnit.DAYS), true, 0);
        insertBanner("hết hạn", "HOME_TOP_CAROUSEL", null, FROM.minus(5, ChronoUnit.DAYS), FROM, true, 0);
        insertBanner("tắt", "HOME_TOP_CAROUSEL", null, FROM, TO, false, 0);
        mvc.perform(get(URL).param("positions", "HOME_TOP_CAROUSEL,POPUP"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=60, public"))
                .andExpect(jsonPath("$.data.HOME_TOP_CAROUSEL.length()").value(2))
                .andExpect(jsonPath("$.data.HOME_TOP_CAROUSEL[0].title").value("thứ nhất"))
                .andExpect(jsonPath("$.data.HOME_TOP_CAROUSEL[0].image_url").value(
                        org.hamcrest.Matchers.endsWith("/content-assets/seed/banner/home-tech.jpg")))
                .andExpect(jsonPath("$.data.POPUP.length()").value(0));
    }

    @Test
    void categoryTopFiltersByCategoryAndRequiresIt() throws Exception {
        long a = category();
        long b = category();
        insertBanner("ngành A", "CATEGORY_TOP", a, FROM, TO, true, 1);
        insertBanner("ngành B", "CATEGORY_TOP", b, FROM, TO, true, 1);
        mvc.perform(get(URL).param("positions", "CATEGORY_TOP").param("category_id", String.valueOf(a)))
                .andExpect(jsonPath("$.data.CATEGORY_TOP.length()").value(1))
                .andExpect(jsonPath("$.data.CATEGORY_TOP[0].title").value("ngành A"));
        mvc.perform(get(URL).param("positions", "CATEGORY_TOP"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].field").value("category_id"));
        mvc.perform(get(URL).param("positions", "BOGUS")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void servesFromCacheUntilAdminChangeEvictsIt() throws Exception {
        long id = insertBanner("giữa trang", "HOME_MIDDLE", null, FROM, TO, true, 1);
        mvc.perform(get(URL).param("positions", "HOME_MIDDLE")).andExpect(jsonPath("$.data.HOME_MIDDLE.length()").value(1));

        // sửa thẳng CSDL: trong 60 giây vẫn đọc từ cache
        jdbc.update("UPDATE home_banners SET title = 'đổi ngầm' WHERE id = ?", id);
        mvc.perform(get(URL).param("positions", "HOME_MIDDLE"))
                .andExpect(jsonPath("$.data.HOME_MIDDLE[0].title").value("giữa trang"));

        // Admin "Tắt ngay" → cache bị xóa sau commit → banner biến mất ngay
        mvc.perform(patch("/api/v1/admin/banners/" + id).header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"is_active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get(URL).param("positions", "HOME_MIDDLE")).andExpect(jsonPath("$.data.HOME_MIDDLE.length()").value(0));
    }
}
