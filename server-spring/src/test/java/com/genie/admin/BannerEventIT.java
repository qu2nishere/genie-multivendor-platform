package com.genie.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.admin.banner.application.BannerStatsFlushJob;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** UC-ADM-12 — lượt hiển thị / bấm qua Redis và job ghi gộp (plan banner B05.05). */
class BannerEventIT extends BannerTestSupport {

    private static final String URL = "/api/v1/public/banner-events";

    @Autowired
    BannerStatsFlushJob job;

    private Map<String, Object> stats(long id) {
        return jdbc.queryForMap("SELECT impression_count, click_count FROM home_banners WHERE id = ?", id);
    }

    @Test
    void countsEventsAndFlushesOnce() throws Exception {
        long id = insertBanner("đếm", "HOME_MIDDLE", null, NOW.minus(1, ChronoUnit.DAYS), NOW.plus(5, ChronoUnit.DAYS),
                true, 1);
        long ended = insertBanner("cũ", "HOME_MIDDLE", null, NOW.minus(9, ChronoUnit.DAYS), NOW.minus(1, ChronoUnit.DAYS),
                true, 2);
        String batch = "{\"events\":[{\"banner_id\":" + id + ",\"type\":\"IMPRESSION\"},{\"banner_id\":" + id
                + ",\"type\":\"IMPRESSION\"},{\"banner_id\":" + id + ",\"type\":\"CLICK\"},{\"banner_id\":999999,"
                + "\"type\":\"CLICK\"},{\"banner_id\":" + ended + ",\"type\":\"IMPRESSION\"},{\"banner_id\":" + id
                + ",\"type\":\"HOVER\"}]}";
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(batch)).andExpect(status().isNoContent());
        // navigator.sendBeacon gửi text/plain
        mvc.perform(post(URL).contentType(MediaType.TEXT_PLAIN)
                        .content("{\"events\":[{\"banner_id\":" + id + ",\"type\":\"IMPRESSION\"}]}"))
                .andExpect(status().isNoContent());

        assertThat(redis.opsForValue().get("banner:" + id + ":imp")).isEqualTo("3");
        assertThat(job.flush()).isEqualTo(1);
        assertThat(stats(id)).containsEntry("impression_count", 3L).containsEntry("click_count", 1L);
        assertThat(stats(ended)).containsEntry("impression_count", 0L);
        assertThat(redis.hasKey("banner:" + id + ":imp")).isFalse();

        assertThat(job.flush()).isZero();
        assertThat(stats(id)).containsEntry("impression_count", 3L);
    }

    @Test
    void rejectsTooManyEventsAndRateLimitsPerIp() throws Exception {
        StringBuilder big = new StringBuilder("{\"events\":[");
        for (int i = 0; i < 51; i++) {
            big.append(i == 0 ? "" : ",").append("{\"banner_id\":1,\"type\":\"CLICK\"}");
        }
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(big.append("]}").toString()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("GNE-VAL-4220"));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("không phải json"))
                .andExpect(status().isUnprocessableEntity());

        String one = "{\"events\":[{\"banner_id\":1,\"type\":\"CLICK\"}]}";
        for (int i = 0; i < 60; i++) {
            mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(one)).andExpect(status().isNoContent());
        }
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(one))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("GNE-RAT-4290"));
    }
}
