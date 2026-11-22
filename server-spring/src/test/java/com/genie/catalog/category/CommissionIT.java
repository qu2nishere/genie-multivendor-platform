package com.genie.catalog.category;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.catalog.category.application.CategoryFacade;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/** API 11.8 — cấu hình hoa hồng (UC-CAT-14 bước 8–9; plan B2 C05.04, D1, D2). */
class CommissionIT extends CategoryITSupport {

    private static final String PATH = "/api/v1/admin/commission-settings";

    @Autowired
    CategoryFacade facade;

    @AfterEach
    void restoreDefault() {
        jdbc.update("UPDATE platform_settings SET value_json = '0.0500'::jsonb WHERE setting_key = 'commission.default_rate'");
    }

    @Test
    void defaultRateIsStoredInPlatformSettingsAndAudited() throws Exception {
        String finance = staffToken("UPDATE_COMMISSION", "VIEW_FINANCE");
        MvcResult r = send("PATCH", PATH, finance, "{\"default_rate\":6}");
        assertThat(status(r)).isEqualTo(200);
        assertThat(((Number) read(r, "$.data.default_rate")).doubleValue()).isEqualTo(6.0);
        assertThat(jdbc.queryForObject("SELECT value_json::text FROM platform_settings "
                + "WHERE setting_key = 'commission.default_rate'", String.class)).isEqualTo("0.0600");
        assertThat(jdbc.queryForObject("SELECT details::text FROM system_logs WHERE action_type = 'COMMISSION_UPDATED' "
                + "AND reference_id = 'DEFAULT' ORDER BY created_at DESC LIMIT 1", String.class)).contains("DEFAULT");
        assertThat(status(send("PATCH", PATH, finance, "{\"default_rate\":2}"))).isEqualTo(422);
        assertThat(status(send("PATCH", PATH, finance, "{\"default_rate\":50}"))).isEqualTo(422);
    }

    @Test
    void perCategoryRatesWithFallbackToDefault() throws Exception {
        String admin = adminToken();
        String w = word();
        long root = createCategory(admin, "Hoa hồng " + w, null);
        long child = createCategory(admin, "Con " + w, root);

        MvcResult on = send("PATCH", PATH, admin, "{\"categories\":[{\"category_id\":" + root
                + ",\"rate\":7.5,\"is_enabled\":true}]}");
        assertThat(status(on)).isEqualTo(200);
        List<Boolean> enabled = read(on, "$.data.categories[?(@.category_id == " + root + ")].is_enabled");
        assertThat(enabled).containsExactly(true);
        assertThat(facade.effectiveCommissionRate(child)).isEqualByComparingTo(new BigDecimal("0.0750"));

        send("PATCH", PATH, admin, "{\"categories\":[{\"category_id\":" + root + ",\"is_enabled\":false}]}");
        assertThat(jdbc.queryForObject("SELECT commission_rate FROM categories WHERE id = ?", BigDecimal.class, root))
                .isNull();
        assertThat(facade.effectiveCommissionRate(child)).isEqualByComparingTo(new BigDecimal("0.0500"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'COMMISSION_UPDATED' "
                + "AND reference_id = ?", Integer.class, String.valueOf(root))).isEqualTo(2);

        assertThat((String) read(send("PATCH", PATH, admin, "{\"categories\":[{\"category_id\":999999999,"
                + "\"rate\":5,\"is_enabled\":true}]}"), "$.code")).isEqualTo("GNE-CAT-4041");
        assertThat(status(send("GET", PATH, staffToken("MANAGE_CATALOG"), null))).isEqualTo(403);
    }
}
