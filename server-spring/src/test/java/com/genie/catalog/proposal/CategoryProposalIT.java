// UC-CAT-17 — Đề xuất danh mục & duyệt đề xuất (API 10.6, 11.11; plan C1 P08.03, D20)
package com.genie.catalog.proposal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.catalog.category.application.CategoryCatalog;
import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.support.IntegrationTestBase;
import com.genie.support.TestDataBuilder;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class CategoryProposalIT extends IntegrationTestBase {

    private static final String SELLER = "/api/v1/seller/category-proposals";
    private static final String ADMIN = "/api/v1/admin/category-proposals";

    @Autowired
    JwtService jwtService;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    CategoryCatalog categoryCatalog;

    TestDataBuilder data;
    String vendorToken;
    String adminToken;
    long root;

    @BeforeEach
    void setUp() {
        data = new TestDataBuilder(jdbc);
        long vendor = data.vendor();
        vendorToken = "Bearer " + jwtService.issue(vendor, UserRole.VENDOR, 1L).token();
        Long admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        adminToken = "Bearer " + jwtService.issue(admin, UserRole.ADMIN, 1L).token();
        root = data.category("Gốc " + TestDataBuilder.uniqueWord(), null);
        categoryCatalog.evictAfterCommit();
    }

    private ResultActions send(String url, String token, Object body) throws Exception {
        var req = post(url).header(HttpHeaders.AUTHORIZATION, token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        return mvc.perform(req);
    }

    private long propose(String name, Long parentId) throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("name", name);
        b.put("parent_id", parentId);
        b.put("reason", "Chưa có ngành phù hợp");
        String body = send(SELLER, vendorToken, b).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    @Test
    void sellerProposesAndAdminApprovesUnderTheRightParent() throws Exception {
        String name = "Thiết bị đeo " + TestDataBuilder.uniqueWord();
        long id = propose(name, root);

        mvc.perform(get(SELLER).header(HttpHeaders.AUTHORIZATION, vendorToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(id))
                .andExpect(jsonPath("$.data[0].code").value(String.format("CP-%03d", id)))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].parent.id").value(root));

        String queue = mvc.perform(get(ADMIN).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(queue).contains(name);

        String approved = send(ADMIN + "/" + id + "/approve", adminToken, Map.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.category.name").value(name))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long categoryId = ((Number) JsonPath.read(approved, "$.data.category.id")).longValue();
        assertThat(jdbc.queryForObject("SELECT parent_id FROM categories WHERE id = ?", Long.class, categoryId))
                .isEqualTo(root);
        // hoa hồng kế thừa (không đặt riêng)
        assertThat(jdbc.queryForObject("SELECT commission_rate FROM categories WHERE id = ?", java.math.BigDecimal.class,
                categoryId)).isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM category_proposals WHERE id = ?", String.class, id))
                .isEqualTo("APPROVED");

        // đề xuất đã xử lý → 4097
        send(ADMIN + "/" + id + "/approve", adminToken, Map.of()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GNE-CAT-4097"));
    }

    @Test
    void duplicatesLimitsAndDepthAreRejected() throws Exception {
        String existing = "Đồng hồ " + TestDataBuilder.uniqueWord();
        long child = data.category(existing, root);
        categoryCatalog.evictAfterCommit();

        // trùng danh mục cùng cha (không dấu, chữ hoa) → 4091 + existing_category
        Map<String, Object> dup = new HashMap<>(Map.of("name", existing.toUpperCase().replace("Đ", "D").replace("ồ", "o")));
        dup.put("parent_id", root);
        send(SELLER, vendorToken, dup).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GNE-CAT-4091"))
                .andExpect(jsonPath("$.details.existing_category.id").value(child));

        // cha đã ở cấp 3 → 422
        long level3 = data.category("Cấp ba " + TestDataBuilder.uniqueWord(), child);
        categoryCatalog.evictAfterCommit();
        Map<String, Object> deep = new HashMap<>(Map.of("name", "Quá sâu " + TestDataBuilder.uniqueWord()));
        deep.put("parent_id", level3);
        send(SELLER, vendorToken, deep).andExpect(status().isUnprocessableEntity());

        // trùng đề xuất đang chờ của chính shop
        String mine = "Thú cưng " + TestDataBuilder.uniqueWord();
        propose(mine, root);
        Map<String, Object> again = new HashMap<>(Map.of("name", mine));
        again.put("parent_id", root);
        send(SELLER, vendorToken, again).andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.field").value("proposal"));

        // tối đa 5 đề xuất đang chờ
        for (int i = 0; i < 4; i++) propose("Ngành " + TestDataBuilder.uniqueWord(), root);
        Map<String, Object> sixth = new HashMap<>(Map.of("name", "Thứ sáu " + TestDataBuilder.uniqueWord()));
        sixth.put("parent_id", root);
        send(SELLER, vendorToken, sixth).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-CAT-4221"));
    }

    @Test
    void rejectNeedsReasonAndCommissionNeedsPermission() throws Exception {
        long id = propose("Đồ cổ " + TestDataBuilder.uniqueWord(), root);
        send(ADMIN + "/" + id + "/reject", adminToken, Map.of("reason", " ")).andExpect(status().isUnprocessableEntity());

        // nhân viên chỉ có MANAGE_CATALOG gửi hoa hồng riêng → 403
        var user = createCustomer(uniqueEmail(), PASSWORD);
        jdbc.update("UPDATE users SET role = 'STAFF' WHERE id = ?", user.getId());
        jdbc.update("INSERT INTO admin_profiles (user_id, full_name, is_active_moderator, staff_title, created_at, updated_at) "
                + "VALUES (?, 'NV danh mục', FALSE, 'MODERATOR', now(), now())", user.getId());
        Long admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        jdbc.update("INSERT INTO staff_permissions (staff_user_id, permission, granted_by_admin_id, granted_at) "
                + "VALUES (?, 'MANAGE_CATALOG', ?, now())", user.getId(), admin);
        String staff = "Bearer " + jwtService.issue(user.getId(), UserRole.STAFF, 1L).token();
        send(ADMIN + "/" + id + "/approve", staff, Map.of("commission_rate", 6.0)).andExpect(status().isForbidden());

        send(ADMIN + "/" + id + "/reject", staff, Map.of("reason", "Đã có danh mục tương tự")).andExpect(status().isOk());
        mvc.perform(get(SELLER).header(HttpHeaders.AUTHORIZATION, vendorToken))
                .andExpect(jsonPath("$.data[0].status").value("REJECTED"))
                .andExpect(jsonPath("$.data[0].reject_reason").value("Đã có danh mục tương tự"));
    }
}
