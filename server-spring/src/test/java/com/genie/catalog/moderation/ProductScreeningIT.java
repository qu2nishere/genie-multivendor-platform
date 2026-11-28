// Test tích hợp kiểm duyệt sản phẩm tự động bằng AI + nút bật/tắt Admin — UC-CAT-12 (plan C1 P06; c1_auto_product.md mục 4.13)
package com.genie.catalog.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.catalog.category.application.CategoryCatalog;
import com.genie.catalog.moderation.application.ProductScreeningSweepJob;
import com.genie.catalog.moderation.application.ScreeningRequest;
import com.genie.catalog.moderation.application.ScreeningRequestBuilder;
import com.genie.catalog.moderation.application.ScreeningResultApplier;
import com.genie.catalog.moderation.application.ScreeningVerdict;
import com.genie.catalog.moderation.domain.ModerationDecision;
import com.genie.common.domain.UserRole;
import com.genie.common.mail.MailTemplate;
import com.genie.common.security.JwtService;
import com.genie.common.settings.PlatformSettings;
import com.genie.common.storage.ObjectStorage;
import com.genie.support.IntegrationTestBase;
import com.genie.support.TestDataBuilder;
import com.jayway.jsonpath.JsonPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class ProductScreeningIT extends IntegrationTestBase {

    @Autowired
    JwtService jwtService;
    @Autowired
    ObjectStorage storage;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    CategoryCatalog categoryCatalog;
    @Autowired
    ScreeningResultApplier applier;
    @Autowired
    ProductScreeningSweepJob sweepJob;
    @Autowired
    ScreeningRequestBuilder requestBuilder;

    TestDataBuilder data;
    long vendor;
    String vendorToken;
    String shopWord;
    long category;

    @BeforeEach
    void setUp() {
        data = new TestDataBuilder(jdbc);
        vendor = data.vendor();
        shopWord = TestDataBuilder.uniqueWord();
        jdbc.update("""
                UPDATE vendor_profiles SET shop_name = ?, pickup_province_id = 79, pickup_province = 'Thành phố Hồ Chí Minh',
                       pickup_ward_code = '26740', pickup_ward = 'Phường Sài Gòn', pickup_address_line = '1 Lê Lợi',
                       ghn_shop_id = 990003 WHERE user_id = ?""", "Shop AI " + shopWord, vendor);
        vendorToken = bearer(jwtService.issue(vendor, UserRole.VENDOR, 1L).token());
        category = data.category();
        categoryCatalog.evictAfterCommit();
        setAiEnabled(true);
    }

    @AfterEach
    void tearDown() {
        setAiEnabled(false);
    }

    // ===== Tiện ích =====

    private void setAiEnabled(boolean on) {
        jdbc.update("""
                INSERT INTO platform_settings (setting_key, value_json, updated_at)
                VALUES (?, CAST(? AS jsonb), now())
                ON CONFLICT (setting_key) DO UPDATE SET value_json = EXCLUDED.value_json, updated_at = now()""",
                PlatformSettings.AI_SCREENING_ENABLED, Boolean.toString(on));
    }

    private String adminToken() {
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        return bearer(jwtService.issue(id, UserRole.ADMIN, 1L).token());
    }

    private Object[] staff(String name, String... permissions) {
        var user = createCustomer(uniqueEmail(), PASSWORD);
        jdbc.update("UPDATE users SET role = 'STAFF' WHERE id = ?", user.getId());
        jdbc.update("""
                INSERT INTO admin_profiles (user_id, full_name, is_active_moderator, staff_title, created_at, updated_at)
                VALUES (?, ?, TRUE, 'MODERATOR', now(), now())""", user.getId(), name);
        Long admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        for (String p : permissions) {
            jdbc.update("INSERT INTO staff_permissions (staff_user_id, permission, granted_by_admin_id, granted_at) "
                    + "VALUES (?, ?, ?, now())", user.getId(), p, admin);
        }
        return new Object[] {user.getId(), bearer(jwtService.issue(user.getId(), UserRole.STAFF, 1L).token())};
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

    private long submitViaApi(String productName) throws Exception {
        String img = "tmp/user/" + vendor + "/" + UUID.randomUUID() + ".jpg";
        storage.put("product-media", img, new byte[] {1, 2, 3}, "image/jpeg");
        Map<String, Object> body = Map.of("submit", true, "name", productName,
                "category_id", category, "image_object_keys", List.of(img),
                "variants", List.of(Map.of("options", List.of(), "price", 150_000, "stock", 3)), "weight_grams", 300);
        MvcResult r = call("POST", "/api/v1/seller/products", vendorToken, body)
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(r.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id")).longValue();
    }

    private long resultCount(long productId) {
        Long n = jdbc.queryForObject("SELECT count(*) FROM product_moderation_results WHERE product_id = ?", Long.class, productId);
        return n == null ? 0L : n;
    }

    private String statusOf(long productId) {
        return jdbc.queryForObject("SELECT status FROM products WHERE id = ?", String.class, productId);
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 100 && !condition.getAsBoolean(); i++) {
            Thread.sleep(100);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    // ===== 10 ca tích hợp theo c1_auto_product.md mục 4.13 =====

    @Test
    void case1_whenDisabled_doesNotCallAiOrCreateResult() throws Exception {
        setAiEnabled(false);
        long id = submitViaApi("SP tắt AI " + TestDataBuilder.uniqueWord());
        Thread.sleep(300);

        assertThat(aiScreening.requests()).isEmpty();
        assertThat(resultCount(id)).isZero();
        assertThat(statusOf(id)).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void case2_approveVerdict_keepsPendingApprovalForHumanReview() throws Exception {
        aiScreening.willReturn(new ScreeningVerdict(ModerationDecision.APPROVE, 10, List.of(), "Hợp lệ.", null, "stub"));
        long id = submitViaApi("Tai nghe sạch " + TestDataBuilder.uniqueWord());

        await(() -> resultCount(id) == 1);
        assertThat(statusOf(id)).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void case3_needReview_flagsProductAndPlacesItAtTopOfPendingQueue() throws Exception {
        // Tạo trước 1 sản phẩm PENDING_APPROVAL cũ hơn để kiểm thứ tự FLAGGED lên đầu
        long normalId = data.product(vendor, "SP thường " + TestDataBuilder.uniqueWord(), "PENDING_APPROVAL", 100_000);
        jdbc.update("UPDATE products SET submitted_at = now() - interval '1 hour', category_id = ? WHERE id = ?",
                category, normalId);
        data.variant(normalId, 100_000, 5);

        aiScreening.willReturn(new ScreeningVerdict(
                ModerationDecision.NEED_REVIEW, 60, List.of("PROHIBITED_ITEM"), "Có thể là hàng hạn chế.", null, "stub"));
        long flaggedId = submitViaApi("SP nghi ngờ " + TestDataBuilder.uniqueWord());
        Long assignedMod = jdbc.queryForObject("SELECT moderator_id FROM products WHERE id = ?", Long.class, flaggedId);

        await(() -> "FLAGGED".equals(statusOf(flaggedId)));
        assertThat(jdbc.queryForObject("SELECT moderation_reason FROM products WHERE id = ?", String.class, flaggedId))
                .startsWith("AI nghi ngờ");
        assertThat(jdbc.queryForObject("SELECT moderator_id FROM products WHERE id = ?", Long.class, flaggedId))
                .isEqualTo(assignedMod);

        MvcResult r = call("GET", "/api/v1/admin/product-moderation?tab=PENDING&q=" + shopWord, adminToken(), null)
                .andExpect(status().isOk()).andReturn();
        List<Number> ids = JsonPath.read(r.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data[*].product_id");
        assertThat(ids.get(0).longValue()).isEqualTo(flaggedId);
    }

    @Test
    void case4_rejectAtOrAboveThreshold_autoRejectsAuditsAndEmailsSeller() throws Exception {
        aiScreening.willReturn(new ScreeningVerdict(
                ModerationDecision.REJECT, 90, List.of("COUNTERFEIT"), "Có dấu hiệu hàng giả.", null, "gemini-2.5-flash"));
        long id = submitViaApi("Túi hàng nhái " + TestDataBuilder.uniqueWord());

        await(() -> "REJECTED".equals(statusOf(id)));
        assertThat(jdbc.queryForObject("SELECT moderator_id FROM products WHERE id = ?", Long.class, id)).isNull();
        assertThat(jdbc.queryForObject("SELECT moderation_reason FROM products WHERE id = ?", String.class, id))
                .startsWith("Không đạt kiểm duyệt tự động:");

        call("GET", "/api/v1/admin/product-moderation?tab=REJECTED&q=" + shopWord, adminToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].product_id").value(id))
                .andExpect(jsonPath("$.data[0].rejected_by_ai").value(true));

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM system_logs
                 WHERE action_type = 'PRODUCT_MODERATED' AND reference_id = ?
                   AND details::text LIKE '%AI_REJECT%'""", Long.class, String.valueOf(id))).isEqualTo(1);

        String email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, vendor);
        await(() -> mails.last(email, MailTemplate.PRODUCT_REJECTED).isPresent());
        var sent = mails.last(email, MailTemplate.PRODUCT_REJECTED).orElseThrow();
        assertThat(String.valueOf(sent.variables().get("reason"))).contains("Không đạt kiểm duyệt tự động");
    }

    @Test
    void case5_rejectBelowThreshold_flagsInsteadOfAutoRejecting() throws Exception {
        aiScreening.willReturn(new ScreeningVerdict(
                ModerationDecision.REJECT, 70, List.of("PRICE_ANOMALY"), "Giá thấp bất thường.", null, "stub"));
        long id = submitViaApi("SP giá sốc " + TestDataBuilder.uniqueWord());

        await(() -> "FLAGGED".equals(statusOf(id)));
        assertThat(jdbc.queryForObject("SELECT moderation_reason FROM products WHERE id = ?", String.class, id))
                .isEqualTo("AI nghi ngờ: giá bất thường");
    }

    @Test
    void case6_clientFailure_recordsFallbackAndKeepsPendingApproval() throws Exception {
        aiScreening.willFail();
        long id = submitViaApi("SP lỗi AI " + TestDataBuilder.uniqueWord());

        await(() -> resultCount(id) == 1);
        assertThat(statusOf(id)).isEqualTo("PENDING_APPROVAL");
        assertThat(jdbc.queryForObject(
                "SELECT fallback_to_manual FROM product_moderation_results WHERE product_id = ?",
                Boolean.class, id)).isTrue();
    }

    @Test
    void case7_staleResult_savesHistoryWithoutChangingProductStatus() throws Exception {
        aiScreening.willReturn(new ScreeningVerdict(ModerationDecision.APPROVE, 10, List.of(), "Ổn.", null, "stub"));
        long id = submitViaApi("SP về muộn " + TestDataBuilder.uniqueWord());
        await(() -> resultCount(id) == 1);

        Timestamp oldTs = jdbc.queryForObject("SELECT submitted_at FROM products WHERE id = ?", Timestamp.class, id);
        OffsetDateTime oldSubmittedAt = oldTs.toInstant().atOffset(ZoneOffset.UTC);
        // Đổi submitted_at sang thời điểm mới (như thể shop đã sửa & gửi lại)
        jdbc.update("UPDATE products SET submitted_at = submitted_at + interval '5 minutes' WHERE id = ?", id);

        applier.apply(id, oldSubmittedAt, new ScreeningVerdict(
                ModerationDecision.REJECT, 95, List.of("COUNTERFEIT"), "Kết quả cũ về muộn.", null, "stub"));

        assertThat(resultCount(id)).isEqualTo(2);
        assertThat(statusOf(id)).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void case8_sweepJob_screensMissedRecentSubmissionsAndSkipsOlderThan24Hours() throws Exception {
        long recentId = data.product(vendor, "SP sót gần " + TestDataBuilder.uniqueWord(), "PENDING_APPROVAL", 120_000);
        jdbc.update("UPDATE products SET submitted_at = now() - interval '10 minutes', category_id = ? WHERE id = ?",
                category, recentId);
        data.variant(recentId, 120_000, 5);

        long oldId = data.product(vendor, "SP quá 24h " + TestDataBuilder.uniqueWord(), "PENDING_APPROVAL", 120_000);
        jdbc.update("UPDATE products SET submitted_at = now() - interval '2 days', category_id = ? WHERE id = ?",
                category, oldId);
        data.variant(oldId, 120_000, 5);

        aiScreening.willReturn(new ScreeningVerdict(ModerationDecision.APPROVE, 12, List.of(), "OK", null, "stub"));
        sweepJob.sweep();

        // job giao việc cho luồng @Async → chờ kết quả
        await(() -> resultCount(recentId) == 1);
        assertThat(resultCount(oldId)).isZero();
    }

    @Test
    void case9_aiSettingsApi_enforcesAdminRoleAuditsAndValidatesBody() throws Exception {
        Object[] mod = staff("Nhân viên duyệt", "MANAGE_CATALOG");
        String staffToken = (String) mod[1];
        String admin = adminToken();

        call("GET", "/api/v1/admin/product-moderation/ai-settings", staffToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.mode", org.hamcrest.Matchers.oneOf("stub", "http")))
                .andExpect(jsonPath("$.data.reject_threshold").value(85));

        call("PUT", "/api/v1/admin/product-moderation/ai-settings", staffToken, Map.of("enabled", false))
                .andExpect(status().isForbidden());

        call("PUT", "/api/v1/admin/product-moderation/ai-settings", admin, Map.of("enabled", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.updated_by").isNotEmpty());

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM system_logs
                 WHERE action_type = 'PRODUCT_MODERATED' AND reference_id = 'ai_screening'
                   AND details::text LIKE '%AI_SCREENING_OFF%'""", Long.class)).isEqualTo(1);

        call("PUT", "/api/v1/admin/product-moderation/ai-settings", admin, Map.of())
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void case10_rollbackOnValidationError_neverCallsAi() throws Exception {
        // Thiếu ảnh và cân nặng khi submit = true → 422, transaction không commit
        Map<String, Object> invalidBody = Map.of("submit", true, "name", "SP lỗi " + TestDataBuilder.uniqueWord(),
                "category_id", category, "image_object_keys", List.of(),
                "variants", List.of(Map.of("options", List.of(), "price", 150_000, "stock", 3)));
        call("POST", "/api/v1/seller/products", vendorToken, invalidBody)
                .andExpect(status().isUnprocessableEntity());

        Thread.sleep(300);
        assertThat(aiScreening.requests()).isEmpty();
    }

    // ===== Phần A: Nhãn ai_check + bộ lọc ?ai= (update_gemini.md mục 2.5, ca 11–18) =====

    @Test
    void case11_whenDisabled_labelsNotScreenedInListAndDetail() throws Exception {
        setAiEnabled(false);
        String name = "SP chua cham " + TestDataBuilder.uniqueWord();
        long id = submitViaApi(name);
        Thread.sleep(200);

        call("GET", "/api/v1/admin/product-moderation?tab=PENDING&q=" + id, adminToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ai_check").value("NOT_SCREENED"));

        call("GET", "/api/v1/admin/product-moderation/" + id, adminToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ai_check").value("NOT_SCREENED"))
                .andExpect(jsonPath("$.data.ai_result").isEmpty());
    }

    @Test
    void case12_approveVerdict_labelsPassed() throws Exception {
        aiScreening.willReturn(new ScreeningVerdict(ModerationDecision.APPROVE, 10, List.of(), "Đạt.", null, "stub"));
        long id = submitViaApi("SP dat AI " + TestDataBuilder.uniqueWord());
        await(() -> resultCount(id) == 1);

        call("GET", "/api/v1/admin/product-moderation?tab=PENDING&q=" + id, adminToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ai_check").value("PASSED"));
    }

    @Test
    void case13_needReview_labelsSuspected() throws Exception {
        aiScreening.willReturn(new ScreeningVerdict(
                ModerationDecision.NEED_REVIEW, 60, List.of("PROHIBITED_ITEM"), "Nghi ngờ.", null, "stub"));
        long id = submitViaApi("SP nghi ngo " + TestDataBuilder.uniqueWord());
        await(() -> "FLAGGED".equals(statusOf(id)));

        call("GET", "/api/v1/admin/product-moderation?tab=PENDING&q=" + id, adminToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ai_check").value("SUSPECTED"));
    }

    @Test
    void case14_rejectBelowThreshold_labelsSuspected() throws Exception {
        aiScreening.willReturn(new ScreeningVerdict(
                ModerationDecision.REJECT, 70, List.of("PRICE_ANOMALY"), "Giá thấp.", null, "stub"));
        long id = submitViaApi("SP reject thap " + TestDataBuilder.uniqueWord());
        await(() -> "FLAGGED".equals(statusOf(id)));

        call("GET", "/api/v1/admin/product-moderation?tab=PENDING&q=" + id, adminToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ai_check").value("SUSPECTED"));
    }

    @Test
    void case15_rejectAtThreshold_labelsRejected() throws Exception {
        aiScreening.willReturn(new ScreeningVerdict(
                ModerationDecision.REJECT, 90, List.of("COUNTERFEIT"), "Hàng giả.", null, "stub"));
        long id = submitViaApi("SP reject cao " + TestDataBuilder.uniqueWord());
        await(() -> "REJECTED".equals(statusOf(id)));

        call("GET", "/api/v1/admin/product-moderation?tab=REJECTED&q=" + id, adminToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ai_check").value("REJECTED"));
    }

    @Test
    void case16_clientFailure_labelsFailedAndDetailShowsFallbackTrue() throws Exception {
        aiScreening.willFail();
        long id = submitViaApi("SP ai loi " + TestDataBuilder.uniqueWord());
        await(() -> resultCount(id) == 1);

        call("GET", "/api/v1/admin/product-moderation?tab=PENDING&q=" + id, adminToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ai_check").value("FAILED"));

        call("GET", "/api/v1/admin/product-moderation/" + id, adminToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ai_check").value("FAILED"))
                .andExpect(jsonPath("$.data.ai_result.fallback").value(true))
                .andExpect(jsonPath("$.data.ai_result.created_at").isNotEmpty());
    }

    @Test
    void case17_resubmissionIgnoresStaleAiResult() throws Exception {
        aiScreening.willReturn(new ScreeningVerdict(ModerationDecision.APPROVE, 10, List.of(), "Đạt cũ.", null, "stub"));
        long id = submitViaApi("SP gui lai " + TestDataBuilder.uniqueWord());
        await(() -> resultCount(id) == 1);

        jdbc.update("UPDATE products SET submitted_at = now() + interval '1 second' WHERE id = ?", id);

        call("GET", "/api/v1/admin/product-moderation?tab=PENDING&q=" + id, adminToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ai_check").value("NOT_SCREENED"));

        call("GET", "/api/v1/admin/product-moderation/" + id, adminToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ai_check").value("NOT_SCREENED"))
                .andExpect(jsonPath("$.data.ai_result").isEmpty());
    }

    @Test
    void case18_filterByAiCheckAndRejectInvalidParam() throws Exception {
        String tag = TestDataBuilder.uniqueWord();
        aiScreening.willReturn(new ScreeningVerdict(ModerationDecision.APPROVE, 10, List.of(), "OK", null, "stub"));
        long passedId = submitViaApi("SP loc " + tag + " A");
        await(() -> resultCount(passedId) == 1);

        aiScreening.willReturn(new ScreeningVerdict(
                ModerationDecision.NEED_REVIEW, 60, List.of("PROHIBITED_ITEM"), "Nghi", null, "stub"));
        long suspectedId = submitViaApi("SP loc " + tag + " B");
        await(() -> "FLAGGED".equals(statusOf(suspectedId)));

        call("GET", "/api/v1/admin/product-moderation?tab=PENDING&ai=PASSED&q=" + tag, adminToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].product_id").value(passedId))
                .andExpect(jsonPath("$.pagination.total_items").value(1))
                .andExpect(jsonPath("$.summary.counts.PENDING").value(2));

        call("GET", "/api/v1/admin/product-moderation?tab=PENDING&ai=NOPE", adminToken(), null)
                .andExpect(status().isUnprocessableEntity());
    }

    // ===== Phần B: Gom ảnh phân loại / mô tả & thu nhỏ ảnh (update_gemini.md mục 11.5, ca 19–21) =====

    private static byte[] smallPng() throws Exception {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static byte[] noisyLargePng(int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random rnd = new Random(42L);
        // Tô nhiễu ~25% diện tích để PNG 3000×2000 nặng ~4,5 MB (> 4 MB giới hạn cũ, < 8 MB trần resizer)
        int noisyHeight = height / 4;
        int[] pixels = new int[width * noisyHeight];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = rnd.nextInt();
        }
        img.setRGB(0, 0, width, noisyHeight, pixels, 0, width);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Test
    void case19_collectsGalleryOptionAndDescriptionImagesWithSkippedCount() throws Exception {
        byte[] png = smallPng();
        long id = data.product(vendor, "SP 6 anh " + TestDataBuilder.uniqueWord(), "PENDING_APPROVAL", 120_000);
        data.variant(id, 120_000, 5);

        for (int i = 0; i < 6; i++) {
            String key = "vendor/" + vendor + "/product/" + id + "/g" + i + ".png";
            storage.put("product-media", key, png, "image/png");
            jdbc.update("""
                    INSERT INTO product_images (product_id, image_url, is_primary, display_order)
                    VALUES (?, ?, ?, ?)""", id, key, i == 0, i);
        }
        String optKey = "vendor/" + vendor + "/product/" + id + "/opt-red.png";
        storage.put("product-media", optKey, png, "image/png");
        String descKey = "vendor/" + vendor + "/product/" + id + "/desc.png";
        storage.put("product-media", descKey, png, "image/png");
        String descUrl = storage.publicUrl("product-media", descKey);

        jdbc.update("""
                UPDATE products SET category_id = ?, option_images_json = CAST(? AS jsonb), description = ?
                 WHERE id = ?""",
                category, "{\"Màu sắc\":{\"Đỏ\":\"" + optKey + "\"}}",
                "<p>Mô tả có ảnh <img src=\"" + descUrl + "\"/></p>", id);

        ScreeningRequest req = requestBuilder.build(id);
        assertThat(req).isNotNull();
        assertThat(req.images()).hasSize(6);
        assertThat(req.images().get(0).label()).isEqualTo("ảnh bìa");
        assertThat(req.images().get(4).label()).isEqualTo("ảnh phân loại \"Màu sắc: Đỏ\"");
        assertThat(req.images().get(5).label()).isEqualTo("ảnh trong mô tả");
        assertThat(req.skippedImages()).isEqualTo(2);
    }

    @Test
    void case20_downscalesLarge3000x2000PngTo1024Jpeg() throws Exception {
        byte[] bigPng = noisyLargePng(3000, 2000);
        long id = data.product(vendor, "SP anh lon " + TestDataBuilder.uniqueWord(), "PENDING_APPROVAL", 120_000);
        data.variant(id, 120_000, 5);
        String key = "vendor/" + vendor + "/product/" + id + "/big.png";
        storage.put("product-media", key, bigPng, "image/png");
        jdbc.update("INSERT INTO product_images (product_id, image_url, is_primary, display_order) VALUES (?, ?, TRUE, 0)",
                id, key);
        jdbc.update("UPDATE products SET category_id = ? WHERE id = ?", category, id);

        ScreeningRequest req = requestBuilder.build(id);
        assertThat(req).isNotNull();
        assertThat(req.images()).hasSize(1);
        assertThat(req.images().get(0).mimeType()).isEqualTo("image/jpeg");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(req.images().get(0).data()));
        assertThat(decoded.getWidth()).isEqualTo(1024);
    }

    @Test
    void case21_skipsExternalImageUrlsInDescription() {
        long id = data.product(vendor, "SP anh ngoai " + TestDataBuilder.uniqueWord(), "PENDING_APPROVAL", 120_000);
        data.variant(id, 120_000, 5);
        jdbc.update("UPDATE products SET category_id = ?, description = ? WHERE id = ?",
                category, "<p>Ảnh lạ <img src=\"https://example.com/x.jpg\"/></p>", id);

        ScreeningRequest req = requestBuilder.build(id);
        assertThat(req).isNotNull();
        assertThat(req.images()).isEmpty();
        assertThat(req.skippedImages()).isGreaterThanOrEqualTo(1);
    }
}
