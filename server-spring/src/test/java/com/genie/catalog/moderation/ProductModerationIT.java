package com.genie.catalog.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.catalog.category.application.CategoryCatalog;
import com.genie.common.domain.UserRole;
import com.genie.common.mail.MailTemplate;
import com.genie.common.security.JwtService;
import com.genie.common.storage.ObjectStorage;
import com.genie.support.IntegrationTestBase;
import com.genie.support.TestDataBuilder;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Kiểm duyệt sản phẩm thủ công theo hàng đợi — UC-CAT-13 (plan C1 P07.06; API 11.12). Hàng chờ, chi tiết, duyệt / từ chối /
 * đưa lại / giao lại, phân công người ít việc nhất, cấu hình nhận việc, phân phối lại, email người bán, quyền.
 */
class ProductModerationIT extends IntegrationTestBase {

    @Autowired
    JwtService jwtService;
    @Autowired
    ObjectStorage storage;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    CategoryCatalog categoryCatalog;

    TestDataBuilder data;
    long vendor;
    String vendorToken;
    String shopName;
    String shopWord;
    long category;

    @BeforeEach
    void setUp() {
        data = new TestDataBuilder(jdbc);
        vendor = data.vendor();
        shopWord = TestDataBuilder.uniqueWord();
        shopName = "Shop Duyet " + shopWord;
        jdbc.update("""
                UPDATE vendor_profiles SET shop_name = ?, pickup_province_id = 79, pickup_province = 'Thành phố Hồ Chí Minh',
                       pickup_ward_code = '26740', pickup_ward = 'Phường Sài Gòn', pickup_address_line = '1 Lê Lợi',
                       ghn_shop_id = 990002 WHERE user_id = ?""", shopName, vendor);
        vendorToken = bearer(jwtService.issue(vendor, UserRole.VENDOR, 1L).token());
        category = data.category();
        categoryCatalog.evictAfterCommit();
        // test tự kiểm soát ai đang nhận việc (seed có sẵn một Moderator)
        jdbc.update("UPDATE admin_profiles SET is_active_moderator = FALSE");
        mails.clear();
    }

    @AfterEach
    void restoreSeedModerator() {
        jdbc.update("""
                UPDATE admin_profiles ap SET is_active_moderator = TRUE FROM users u
                 WHERE u.id = ap.user_id AND u.email = 'phat.nt@genie.example'""");
    }

    // ===== Tiện ích =====

    private String adminToken() {
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        return bearer(jwtService.issue(id, UserRole.ADMIN, 1L).token());
    }

    /** Nhân viên STAFF có hồ sơ quản trị + các quyền cho trước; trả {id, token}. */
    private Object[] staff(String name, boolean accepting, String... permissions) {
        var user = createCustomer(uniqueEmail(), PASSWORD);
        jdbc.update("UPDATE users SET role = 'STAFF' WHERE id = ?", user.getId());
        jdbc.update("""
                INSERT INTO admin_profiles (user_id, full_name, is_active_moderator, staff_title, created_at, updated_at)
                VALUES (?, ?, ?, 'MODERATOR', now(), now())""", user.getId(), name, accepting);
        Long admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        for (String p : permissions) {
            jdbc.update("INSERT INTO staff_permissions (staff_user_id, permission, granted_by_admin_id, granted_at) "
                    + "VALUES (?, ?, ?, now())", user.getId(), p, admin);
        }
        return new Object[] {user.getId(), bearer(jwtService.issue(user.getId(), UserRole.STAFF, 1L).token())};
    }

    /** Sản phẩm đang chờ duyệt dựng thẳng CSDL (không qua API). */
    private long pending(String status, String submittedAt, Long moderatorId) {
        long id = data.product(vendor, "Sản phẩm " + TestDataBuilder.uniqueWord(), status, 120_000);
        jdbc.update("UPDATE products SET submitted_at = CAST(? AS timestamptz), moderator_id = ?, category_id = ? WHERE id = ?",
                submittedAt, moderatorId, category, id);
        data.variant(id, 120_000, 5);
        data.primaryImage(id, "seed/product/x.jpg");
        return id;
    }

    private ResultActions call(String method, String url, String token, Object body) throws Exception {
        var req = switch (method) {
            case "GET" -> get(url);
            case "POST" -> post(url);
            case "PUT" -> put(url);
            default -> throw new IllegalArgumentException(method);
        };
        req.header(HttpHeaders.AUTHORIZATION, token);
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        return mvc.perform(req);
    }

    /** Người bán gửi duyệt một sản phẩm đầy đủ qua API — trả id. */
    private long submitViaApi() throws Exception {
        String img = "tmp/user/" + vendor + "/" + UUID.randomUUID() + ".jpg";
        storage.put("product-media", img, new byte[] {1, 2, 3}, "image/jpeg");
        Map<String, Object> body = Map.of("submit", true, "name", "Gửi duyệt " + TestDataBuilder.uniqueWord(),
                "category_id", category, "image_object_keys", List.of(img),
                "variants", List.of(Map.of("options", List.of(), "price", 150_000, "stock", 3)), "weight_grams", 300);
        MvcResult r = call("POST", "/api/v1/seller/products", vendorToken, body).andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(r.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id")).longValue();
    }

    private Long moderatorOf(long productId) {
        return jdbc.queryForObject("SELECT moderator_id FROM products WHERE id = ?", Long.class, productId);
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 100 && !condition.getAsBoolean(); i++) {
            Thread.sleep(100);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    // ===== Hàng chờ & chi tiết =====

    @Test
    void flaggedFirstThenOldestAndDetailShowsAiResult() throws Exception {
        long newer = pending("PENDING_APPROVAL", "2026-06-08T10:00:00Z", null);
        long older = pending("PENDING_APPROVAL", "2026-06-08T08:00:00Z", null);
        long flagged = pending("FLAGGED", "2026-06-08T11:00:00Z", null);
        jdbc.update("""
                INSERT INTO product_moderation_results (product_id, model_name, decision, risk_score, violations_json,
                                                        reasoning, fallback_to_manual, created_at)
                VALUES (?, 'gemini-2.5-flash', 'NEED_REVIEW', 62, '["COUNTERFEIT"]', 'Nghi hàng nhái.', FALSE, now())""",
                flagged);

        String admin = adminToken();
        MvcResult r = call("GET", "/api/v1/admin/product-moderation?tab=PENDING&q=" + shopWord, admin, null)
                .andExpect(status().isOk()).andReturn();
        String body = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Number> ids = JsonPath.read(body, "$.data[*].product_id");
        assertThat(ids.stream().map(Number::longValue).toList()).containsExactly(flagged, older, newer);
        assertThat(((Number) JsonPath.read(body, "$.summary.counts.PENDING")).intValue()).isEqualTo(3);
        assertThat((String) JsonPath.read(body, "$.data[0].shop.name")).isEqualTo(shopName);

        call("GET", "/api/v1/admin/product-moderation/" + flagged, admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FLAGGED"))
                .andExpect(jsonPath("$.data.ai_result.decision").value("NEED_REVIEW"))
                .andExpect(jsonPath("$.data.ai_result.risk_score").value(62))
                .andExpect(jsonPath("$.data.ai_result.violations[0]").value("COUNTERFEIT"))
                .andExpect(jsonPath("$.data.variants[0].price").value(120_000))
                .andExpect(jsonPath("$.data.images[0]").exists());
        call("GET", "/api/v1/admin/product-moderation?tab=BAD", admin, null).andExpect(status().isUnprocessableEntity());
    }

    // ===== Quyết định =====

    @Test
    void approvePublishesVerifiesBrandLogsAndEmailsSeller() throws Exception {
        long brand = data.brand("Hiệu mới " + TestDataBuilder.uniqueWord(), false);
        long id = pending("PENDING_APPROVAL", "2026-06-08T08:00:00Z", null);
        jdbc.update("UPDATE products SET brand_id = ? WHERE id = ?", brand, id);
        Object[] mod = staff("Lê Duyệt", true, "MANAGE_CATALOG");

        call("POST", "/api/v1/admin/products/" + id + "/approve", (String) mod[1], null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
        assertThat(jdbc.queryForObject("SELECT status FROM products WHERE id = ?", String.class, id)).isEqualTo("PUBLISHED");
        assertThat(moderatorOf(id)).isEqualTo((Long) mod[0]);
        assertThat(jdbc.queryForObject("SELECT is_verified FROM brands WHERE id = ?", Boolean.class, brand)).isTrue();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM system_logs WHERE action_type = 'PRODUCT_MODERATED' AND reference_id = ?
                   AND details::text LIKE '%APPROVE%'""", Long.class, String.valueOf(id))).isEqualTo(1);
        mvc.perform(get("/api/v1/public/products/" + id)).andExpect(status().isOk());
        String email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, vendor);
        await(() -> mails.last(email, MailTemplate.PRODUCT_APPROVED).isPresent());

        // duyệt lần nữa (người khác vừa duyệt) → 4097
        call("POST", "/api/v1/admin/products/" + id + "/approve", adminToken(), null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GNE-CAT-4097"));
    }

    @Test
    void rejectNeedsAStandardReasonAndSellerSeesIt() throws Exception {
        long id = pending("PENDING_APPROVAL", "2026-06-08T08:00:00Z", null);
        String admin = adminToken();
        call("POST", "/api/v1/admin/products/" + id + "/reject", admin, Map.of("reason_code", "WHATEVER"))
                .andExpect(status().isUnprocessableEntity());
        call("POST", "/api/v1/admin/products/" + id + "/reject", admin, Map.of("reason_code", "OTHER"))
                .andExpect(status().isUnprocessableEntity());
        call("POST", "/api/v1/admin/products/" + id + "/reject", admin,
                Map.of("reason_code", "BAD_IMAGE", "note", "Ảnh mờ, có chữ chèn"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("REJECTED"));

        call("GET", "/api/v1/seller/products/" + id, vendorToken, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.reject_reason").value("Ảnh sản phẩm không đạt: Ảnh mờ, có chữ chèn"));
        String email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, vendor);
        await(() -> mails.last(email, MailTemplate.PRODUCT_REJECTED).isPresent());
        assertThat((String) mails.last(email, MailTemplate.PRODUCT_REJECTED).get().variables().get("reason"))
                .contains("Ảnh sản phẩm không đạt").contains("Ảnh mờ");

        // từ chối → đưa lại hàng chờ → "Chờ duyệt lại"
        call("POST", "/api/v1/admin/products/" + id + "/requeue", admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));
        assertThat(jdbc.queryForObject("SELECT is_requeued FROM products WHERE id = ?", Boolean.class, id)).isTrue();
        // đang chờ thì không đưa lại được nữa
        call("POST", "/api/v1/admin/products/" + id + "/requeue", admin, null).andExpect(status().isConflict());
    }

    // ===== Phân công =====

    @Test
    void newSubmissionGoesToTheLeastLoadedAcceptingModerator() throws Exception {
        Object[] a = staff("A " + TestDataBuilder.uniqueWord(), true, "MANAGE_CATALOG");
        Object[] b = staff("B " + TestDataBuilder.uniqueWord(), true, "MANAGE_CATALOG");
        pending("PENDING_APPROVAL", "2026-06-08T08:00:00Z", (Long) a[0]);
        pending("PENDING_APPROVAL", "2026-06-08T08:01:00Z", (Long) a[0]);

        long first = submitViaApi();
        assertThat(moderatorOf(first)).isEqualTo((Long) b[0]);

        // B tắt nhận việc → giao A
        call("PUT", "/api/v1/admin/moderators/availability", adminToken(),
                Map.of("items", List.of(Map.of("staff_id", b[0], "is_accepting", false))))
                .andExpect(status().isOk());
        assertThat(moderatorOf(submitViaApi())).isEqualTo((Long) a[0]);

        // không ai nhận việc → để trống
        jdbc.update("UPDATE admin_profiles SET is_active_moderator = FALSE");
        assertThat(moderatorOf(submitViaApi())).isNull();

        // giao lại cho B (đang tắt nhận việc vẫn giao được); người không phải moderator → 422
        call("PUT", "/api/v1/admin/products/" + first + "/assignee", adminToken(), Map.of("staff_id", b[0]))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.assignee.id").value(((Long) b[0]).intValue()));
        Object[] cs = staff("CS " + TestDataBuilder.uniqueWord(), false, "HANDLE_DISPUTE");
        call("PUT", "/api/v1/admin/products/" + first + "/assignee", adminToken(), Map.of("staff_id", cs[0]))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void redistributeSpreadsTheQueueAcrossAcceptingModerators() throws Exception {
        Object[] a = staff("A " + TestDataBuilder.uniqueWord(), true, "MANAGE_CATALOG");
        Object[] b = staff("B " + TestDataBuilder.uniqueWord(), true, "MANAGE_CATALOG");
        long p1 = pending("PENDING_APPROVAL", "2026-06-08T08:00:00Z", null);
        long p2 = pending("PENDING_APPROVAL", "2026-06-08T08:01:00Z", null);
        call("POST", "/api/v1/admin/moderators/redistribute", adminToken(), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reassigned_count").exists());
        assertThat(List.of(moderatorOf(p1), moderatorOf(p2))).allMatch(m -> m != null
                && (m.equals(a[0]) || m.equals(b[0])));

        jdbc.update("UPDATE admin_profiles SET is_active_moderator = FALSE");
        call("POST", "/api/v1/admin/moderators/redistribute", adminToken(), null)
                .andExpect(status().isUnprocessableEntity());
    }

    // ===== Quyền & cấu hình =====

    @Test
    void onlyManageCatalogCanModerate() throws Exception {
        long id = pending("PENDING_APPROVAL", "2026-06-08T08:00:00Z", null);
        Object[] cs = staff("CS " + TestDataBuilder.uniqueWord(), false, "MODERATE_REVIEW");
        for (String token : List.of(vendorToken, (String) cs[1])) {
            call("GET", "/api/v1/admin/product-moderation", token, null).andExpect(status().isForbidden());
            call("POST", "/api/v1/admin/products/" + id + "/approve", token, null).andExpect(status().isForbidden());
            call("GET", "/api/v1/admin/moderators", token, null).andExpect(status().isForbidden());
        }
        Object[] mod = staff("M " + TestDataBuilder.uniqueWord(), true, "MANAGE_CATALOG");
        call("GET", "/api/v1/admin/moderators", (String) mod[1], null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.staff_id == " + mod[0] + ")].is_accepting").value(true))
                .andExpect(jsonPath("$.data[?(@.staff_id == " + cs[0] + ")]").isEmpty());
        call("PUT", "/api/v1/admin/moderators/availability", (String) mod[1],
                Map.of("items", List.of(Map.of("staff_id", cs[0], "is_accepting", true))))
                .andExpect(status().isUnprocessableEntity());
    }
}
