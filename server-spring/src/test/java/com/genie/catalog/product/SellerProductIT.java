package com.genie.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.catalog.category.application.CategoryCatalog;
import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.common.storage.ObjectStorage;
import com.genie.support.IntegrationTestBase;
import com.genie.support.TestDataBuilder;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Thêm mới / cập nhật sản phẩm của người bán + dữ liệu cho form — UC-CAT-09, UC-CAT-10 (plan C1 P03.06, P04.05; API 10.5,
 * 10.6). Kèm ca chống ghi đè giữ chỗ tồn kho của đơn (plan C1 mục 10.3).
 */
class SellerProductIT extends IntegrationTestBase {

    private static final String URL = "/api/v1/seller/products";

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
    String token;
    long category;

    @BeforeEach
    void setUp() {
        data = new TestDataBuilder(jdbc);
        vendor = data.vendor();
        jdbc.update("""
                UPDATE vendor_profiles SET pickup_province_id = 79, pickup_province = 'Thành phố Hồ Chí Minh',
                       pickup_ward_code = '26740', pickup_ward = 'Phường Sài Gòn', pickup_address_line = '1 Lê Lợi',
                       ghn_shop_id = 990001 WHERE user_id = ?""", vendor);
        token = "Bearer " + jwtService.issue(vendor, UserRole.VENDOR, 1L).token();
        category = data.category();
        categoryCatalog.evictAfterCommit();     // danh mục chèn thẳng CSDL — bỏ cây cũ trong cache
    }

    // ===== Tiện ích =====

    private String tmp(String contentType, String ext) {
        String key = "tmp/user/" + vendor + "/" + UUID.randomUUID() + "." + ext;
        storage.put("product-media", key, new byte[] {1, 2, 3, 4}, contentType);
        return key;
    }

    private String tmpImage() {
        return tmp("image/jpeg", "jpg");
    }

    private String tmpDoc() {
        String key = "tmp/user/" + vendor + "/" + UUID.randomUUID() + ".pdf";
        storage.put("product-docs", key, new byte[] {5, 6, 7}, "application/pdf");
        return key;
    }

    /** Sản phẩm 1 nhóm "Màu sắc" (Đen có ảnh, Trắng), 2 biến thể, đủ điều kiện gửi duyệt. */
    private Map<String, Object> fullBody(boolean submit) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("submit", submit);
        b.put("name", "Tai nghe Bluetooth ANC " + TestDataBuilder.uniqueWord());
        b.put("category_id", category);
        b.put("condition", "NEW");
        b.put("origin", "Việt Nam");
        b.put("image_object_keys", List.of(tmpImage(), tmpImage()));
        b.put("specs", List.of(Map.of("name", "Chất liệu", "value", "Nhựa ABS")));
        b.put("description_html", "<p>Pin <strong>40 giờ</strong></p><script>alert(1)</script>");
        b.put("option_groups", List.of(Map.of("name", "Màu sắc", "values", List.of(
                Map.of("value", "Đen", "image_object_key", tmpImage()), Map.of("value", "Trắng")))));
        b.put("variant_image_group", "Màu sắc");
        b.put("variants", new ArrayList<>(List.of(
                variant(null, "Đen", null, 389_000, 10), variant(null, "Trắng", null, 359_000, 5))));
        b.put("weight_grams", 350);
        b.put("dimensions_cm", Map.of("length", 20, "width", 15, "height", 8));
        b.put("shipping_payer", "BUYER");
        b.put("is_pre_order", false);
        return b;
    }

    private static Map<String, Object> variant(Object id, String option, String sku, long price, int stock) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", id);
        v.put("options", List.of(option));
        v.put("sku", sku);
        v.put("price", price);
        v.put("stock", stock);
        return v;
    }

    private ResultActions send(String method, String url, Object body) throws Exception {
        var req = switch (method) {
            case "POST" -> post(url);
            case "PUT" -> put(url);
            default -> throw new IllegalArgumentException(method);
        };
        return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private long created(ResultActions r) throws Exception {
        Number id = JsonPath.read(r.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        return id.longValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> detail(long id) throws Exception {
        String body = mvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return (Map<String, Object>) objectMapper.readValue(body, Map.class).get("data");
    }

    // ===== Thêm mới (UC-CAT-09) =====

    @Test
    void draftNeedsOnlyAName() throws Exception {
        long id = created(send("POST", URL, Map.of("submit", false, "name", "Bàn phím nháp"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.requires_review").value(false)));
        Map<String, Object> d = detail(id);
        assertThat(d.get("name")).isEqualTo("Bàn phím nháp");
        assertThat((List<?>) d.get("variants")).isEmpty();
        assertThat(d.get("status")).isEqualTo("DRAFT");
    }

    @Test
    void submitPromotesMediaGeneratesSkusAndQueuesProduct() throws Exception {
        Map<String, Object> body = fullBody(true);
        @SuppressWarnings("unchecked")
        String firstTmp = ((List<String>) body.get("image_object_keys")).get(0);
        long id = created(send("POST", URL, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.requires_review").value(true)));

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM products WHERE id = ?", id);
        assertThat(row.get("vendor_id")).isEqualTo(vendor);
        assertThat(row.get("submitted_at")).isNotNull();
        assertThat(row.get("ships_from")).isEqualTo("Thành phố Hồ Chí Minh");
        assertThat(((java.math.BigDecimal) row.get("base_price")).longValue()).isEqualTo(359_000);
        assertThat((String) row.get("description")).contains("<strong>40 giờ</strong>").doesNotContain("script");

        List<String> images = jdbc.queryForList(
                "SELECT image_url FROM product_images WHERE product_id = ? ORDER BY display_order", String.class, id);
        assertThat(images).hasSize(2).allMatch(k -> k.startsWith("vendor/" + vendor + "/product/" + id + "/"));
        assertThat(storage.stat("product-media", firstTmp)).isEmpty();          // tệp tạm đã được chuyển đi

        List<Map<String, Object>> variants = jdbc.queryForList(
                "SELECT sku, image_url, attributes_json::text AS a FROM product_variants WHERE product_id = ? ORDER BY id", id);
        assertThat(variants).extracting(v -> v.get("sku")).containsExactly("G" + id + "-1", "G" + id + "-2");
        assertThat((String) variants.get(0).get("image_url")).startsWith("vendor/" + vendor + "/product/" + id + "/");
        assertThat(variants.get(1).get("image_url")).isNull();
        assertThat((String) variants.get(0).get("a")).contains("Màu sắc").contains("Đen");

        mvc.perform(get(URL).param("status", "PENDING_APPROVAL").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem((int) id)))
                .andExpect(jsonPath("$.summary.counts.PENDING_APPROVAL").value(1))
                .andExpect(jsonPath("$.data[0].variant_count").value(2))
                .andExpect(jsonPath("$.data[0].price_min").value(359_000))
                .andExpect(jsonPath("$.data[0].stock_total").value(15));

        Map<String, Object> d = detail(id);
        assertThat((List<?>) d.get("option_groups")).hasSize(1);
        assertThat((List<?>) d.get("variants")).hasSize(2);
        assertThat(d.get("weight_grams")).isEqualTo(350);
    }

    @Test
    void invalidFieldsAreReportedPerField() throws Exception {
        Map<String, Object> body = fullBody(true);
        List<String> nine = new ArrayList<>();
        for (int i = 0; i < 9; i++) nine.add(tmpImage());
        body.put("image_object_keys", nine);
        body.put("variants", List.of(variant(null, "Đen", null, 0, 1), variant(null, "Đen", "có dấu", 5_000, -1)));
        body.remove("weight_grams");
        send("POST", URL, body)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-VAL-4220"))
                .andExpect(jsonPath("$.errors[*].field", hasItems("image_object_keys", "variants[0].price",
                        "variants[1].options", "variants[1].sku", "variants[1].stock", "weight_grams")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM products WHERE vendor_id = ?", Long.class, vendor)).isZero();
    }

    @Test
    void submissionGates() throws Exception {
        // kho lấy hàng chưa cấu hình → 4224 (nháp vẫn lưu được)
        jdbc.update("UPDATE vendor_profiles SET ghn_shop_id = NULL WHERE user_id = ?", vendor);
        send("POST", URL, fullBody(true)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-CAT-4224"));
        send("POST", URL, fullBody(false)).andExpect(status().isCreated());
        jdbc.update("UPDATE vendor_profiles SET ghn_shop_id = 990001 WHERE user_id = ?", vendor);

        // ngành cha "kinh doanh có điều kiện" → thiếu giấy tờ 4222
        long parent = data.category("Dược phẩm " + TestDataBuilder.uniqueWord(), null);
        jdbc.update("UPDATE categories SET requires_business_license = TRUE WHERE id = ?", parent);
        jdbc.update("UPDATE categories SET parent_id = ? WHERE id = ?", parent, category);
        // thuộc tính bắt buộc khai ở ngành cha (kế thừa)
        jdbc.update("""
                INSERT INTO category_attributes (category_id, code, name, data_type, is_required, display_order, is_active,
                                                 created_at, updated_at)
                VALUES (?, 'warranty', 'Bảo hành', 'TEXT', TRUE, 1, TRUE, now(), now())""", parent);
        categoryCatalog.evictAfterCommit();

        send("POST", URL, fullBody(true)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-CAT-4222"));

        Map<String, Object> withDocs = fullBody(true);
        withDocs.put("document_object_keys", List.of(tmpDoc()));
        withDocs.put("document_file_names", List.of("giay-phep.pdf"));
        send("POST", URL, withDocs).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-CAT-4223"))
                .andExpect(jsonPath("$.details.missing_specs[0]").value("Bảo hành"));

        Map<String, Object> ok = fullBody(true);
        ok.put("document_object_keys", List.of(tmpDoc()));
        ok.put("document_file_names", List.of("giay-phep.pdf"));
        ok.put("specs", List.of(Map.of("name", "bao hanh", "value", "12 tháng")));      // khớp tên không dấu
        long id = created(send("POST", URL, ok).andExpect(status().isCreated()));
        assertThat(jdbc.queryForObject("SELECT specs_json::text FROM products WHERE id = ?", String.class, id))
                .contains("\"warranty\"");
        assertThat(jdbc.queryForObject("SELECT file_name FROM product_documents WHERE product_id = ?", String.class, id))
                .isEqualTo("giay-phep.pdf");

        mvc.perform(get("/api/v1/seller/categories/" + category + "/listing-rules").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requires_documents").value(true))
                .andExpect(jsonPath("$.data.attributes[0].code").value("warranty"))
                .andExpect(jsonPath("$.data.attributes[0].is_required").value(true));
    }

    // ===== Cập nhật (UC-CAT-10) =====

    private long published() throws Exception {
        long id = created(send("POST", URL, fullBody(true)).andExpect(status().isCreated()));
        jdbc.update("UPDATE products SET status = 'PUBLISHED', is_published = TRUE WHERE id = ?", id);
        return id;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> editBody(long id) throws Exception {
        Map<String, Object> d = detail(id);
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("submit", false);
        b.put("version", d.get("version"));
        b.put("name", d.get("name"));
        b.put("category_id", ((List<Map<String, Object>>) d.get("category_path")).get(0).get("id"));
        b.put("condition", d.get("condition"));
        b.put("origin", d.get("origin"));
        b.put("image_object_keys", ((List<Map<String, Object>>) d.get("images")).stream().map(m -> m.get("object_key")).toList());
        b.put("specs", d.get("specs"));
        b.put("description_html", d.get("description_html"));
        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map<String, Object> g : (List<Map<String, Object>>) d.get("option_groups")) {
            groups.add(Map.of("name", g.get("name"), "values", ((List<Map<String, Object>>) g.get("values")).stream()
                    .map(v -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("value", v.get("value"));
                        m.put("image_object_key", v.get("image_object_key"));
                        return m;
                    }).toList()));
        }
        b.put("option_groups", groups);
        b.put("variant_image_group", d.get("variant_image_group"));
        List<Map<String, Object>> vars = new ArrayList<>();
        for (Map<String, Object> v : (List<Map<String, Object>>) d.get("variants")) {
            Map<String, Object> m = new LinkedHashMap<>(v);
            m.remove("reserved");
            vars.add(m);
        }
        b.put("variants", vars);
        b.put("weight_grams", d.get("weight_grams"));
        b.put("dimensions_cm", d.get("dimensions_cm"));
        b.put("shipping_payer", d.get("shipping_payer"));
        b.put("is_pre_order", d.get("is_pre_order"));
        return b;
    }

    @Test
    @SuppressWarnings("unchecked")
    void priceChangeStaysLiveButNameChangeIsRequeued() throws Exception {
        long id = published();
        Map<String, Object> b = editBody(id);
        ((Map<String, Object>) ((List<?>) b.get("variants")).get(0)).put("price", 399_000);
        send("PUT", URL + "/" + id, b).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.requires_review").value(false));
        assertThat(jdbc.queryForObject("SELECT min(price) FROM product_variants WHERE product_id = ?",
                java.math.BigDecimal.class, id).longValue()).isEqualTo(359_000);

        Map<String, Object> renamed = editBody(id);
        renamed.put("name", "Tên mới " + TestDataBuilder.uniqueWord());
        send("PUT", URL + "/" + id, renamed).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.requires_review").value(true));
        assertThat(jdbc.queryForObject("SELECT is_requeued FROM products WHERE id = ?", Boolean.class, id)).isTrue();

        // đang chờ duyệt → chỉ xem
        send("PUT", URL + "/" + id, editBody(id)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GNE-CAT-4096"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sellerEditsNeverOverwriteOrderReservations() throws Exception {
        long id = published();
        long variantId = jdbc.queryForObject("SELECT min(id) FROM product_variants WHERE product_id = ?", Long.class, id);
        jdbc.update("UPDATE product_variants SET reserved_count = 2 WHERE id = ?", variantId);   // đơn đang giữ chỗ

        Map<String, Object> b = editBody(id);
        ((Map<String, Object>) ((List<?>) b.get("variants")).get(0)).put("price", 379_000);
        send("PUT", URL + "/" + id, b).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT reserved_count FROM product_variants WHERE id = ?", Integer.class, variantId))
                .isEqualTo(2);

        Map<String, Object> low = editBody(id);
        ((Map<String, Object>) ((List<?>) low.get("variants")).get(0)).put("stock", 1);
        send("PUT", URL + "/" + id, low).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].code").value("BELOW_RESERVED"));

        Map<String, Object> dropped = editBody(id);
        List<Map<String, Object>> vars = (List<Map<String, Object>>) dropped.get("variants");
        dropped.put("variants", vars.subList(1, vars.size()));
        ((List<Map<String, Object>>) dropped.get("option_groups")).set(0, Map.of("name", "Màu sắc",
                "values", List.of(Map.of("value", "Trắng"))));
        send("PUT", URL + "/" + id, dropped).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GNE-CAT-4094"));
        assertThat(jdbc.queryForObject("SELECT deleted_at FROM product_variants WHERE id = ?", Object.class, variantId))
                .isNull();
    }

    @Test
    void ownershipAndStaleVersion() throws Exception {
        long id = published();
        long other = data.vendor();
        String otherToken = "Bearer " + jwtService.issue(other, UserRole.VENDOR, 1L).token();
        mvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, otherToken)).andExpect(status().isNotFound());

        Map<String, Object> b = editBody(id);
        b.put("version", -1);
        send("PUT", URL + "/" + id, b).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GNE-CAT-4097"))
                .andExpect(jsonPath("$.details.reason").value("STALE_VERSION"));

        // khóa ảnh không thuộc sản phẩm này
        Map<String, Object> foreign = editBody(id);
        foreign.put("image_object_keys", List.of("vendor/" + other + "/product/1/x.jpg"));
        send("PUT", URL + "/" + id, foreign).andExpect(status().isUnprocessableEntity());
    }

    @Test
    @SuppressWarnings("unchecked")
    void imagesForTwoOptionGroupsAreStoredAndReturned() throws Exception {
        Map<String, Object> body = fullBody(false);
        body.put("option_groups", List.of(
                Map.of("name", "Màu sắc", "values", List.of(
                        Map.of("value", "Đen", "image_object_key", tmpImage()), Map.of("value", "Trắng"))),
                Map.of("name", "Dung lượng", "values", List.of(
                        Map.of("value", "128 GB", "image_object_key", tmpImage()), Map.of("value", "256 GB")))));
        body.put("variant_image_group", "Màu sắc");
        List<Map<String, Object>> variants = new ArrayList<>();
        for (String c : List.of("Đen", "Trắng")) {
            for (String g : List.of("128 GB", "256 GB")) {
                Map<String, Object> v = variant(null, c, null, 300_000, 1);
                v.put("options", List.of(c, g));
                variants.add(v);
            }
        }
        body.put("variants", variants);
        long id = created(send("POST", URL, body).andExpect(status().isCreated()));

        String json = jdbc.queryForObject("SELECT option_images_json::text FROM products WHERE id = ?", String.class, id);
        assertThat(json).contains("Màu sắc").contains("Đen").contains("Dung lượng").contains("128 GB");
        assertThat(jdbc.queryForObject("SELECT variant_image_attribute FROM products WHERE id = ?", String.class, id))
                .isEqualTo("Màu sắc");
        // ảnh SKU theo nhóm ảnh chính (Màu sắc): 2 SKU màu Đen có ảnh
        assertThat(jdbc.queryForObject("SELECT count(*) FROM product_variants WHERE product_id = ? AND image_url IS NOT NULL",
                Integer.class, id)).isEqualTo(2);

        List<Map<String, Object>> groups = (List<Map<String, Object>>) detail(id).get("option_groups");
        assertThat(groups).extracting(g -> g.get("name")).containsExactly("Màu sắc", "Dung lượng");
        Map<String, Object> gb128 = ((List<Map<String, Object>>) groups.get(1).get("values")).get(0);
        assertThat(gb128.get("value")).isEqualTo("128 GB");
        assertThat((String) gb128.get("image_object_key")).startsWith("vendor/" + vendor + "/product/" + id + "/");
        assertThat(gb128.get("image_url")).isNotNull();
    }

    @Test
    void shopNotVerifiedIsBlockedByKycGate() throws Exception {
        jdbc.update("UPDATE vendor_profiles SET kyc_status = 'PENDING' WHERE user_id = ?", vendor);
        send("POST", URL, Map.of("submit", false, "name", "x")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GNE-VND-4031"));
    }

    // ===== Dữ liệu cho form =====

    @Test
    void brandsAreDeduplicatedByNormalizedName() throws Exception {
        String name = "Akko " + TestDataBuilder.uniqueWord();
        Number first = JsonPath.read(send("POST", "/api/v1/seller/brands", Map.of("name", name))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.data.id");
        send("POST", "/api/v1/seller/brands", Map.of("name", "  " + name.toUpperCase() + " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(first.intValue()));
        mvc.perform(get("/api/v1/seller/brands").param("q", name.substring(0, 6)).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", hasItem(name)));
        mvc.perform(get("/api/v1/seller/categories").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].commission_rate").exists());
        mvc.perform(get("/api/v1/seller/categories/999999999/listing-rules").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GNE-CAT-4041"));
    }
}
