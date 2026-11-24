package com.genie.catalog.category;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** API 11.11 — quản lý cây danh mục (UC-CAT-14; plan B2 C04.04, D3–D7, D17). */
class AdminCategoryIT extends CategoryITSupport {

    private static final String BASE = "/api/v1/admin/categories";

    @Test
    void createThreeLevelsWithGeneratedSlugAndRejectTheFourth() throws Exception {
        String admin = adminToken();
        String w = word();
        MvcResult root = send("POST", BASE, admin, "{\"name\":\"Thú cưng " + w + "\",\"icon\":\"heart\","
                + "\"commission_rate\":6.5,\"requires_business_license\":true}");
        assertThat(status(root)).isEqualTo(201);
        assertThat((String) read(root, "$.data.slug")).isEqualTo("thu-cung-" + w);
        assertThat(((Number) read(root, "$.data.commission_rate")).doubleValue()).isEqualTo(6.5);
        assertThat((Boolean) read(root, "$.data.requires_business_license")).isTrue();
        assertThat((Integer) read(root, "$.data.level")).isEqualTo(1);
        long rootId = ((Number) read(root, "$.data.id")).longValue();

        long child = createCategory(admin, "Chó " + w, rootId);
        long leaf = createCategory(admin, "Thức ăn cho chó " + w, child);
        MvcResult tooDeep = send("POST", BASE, admin, "{\"name\":\"Hạt " + w + "\",\"parent_id\":" + leaf + "}");
        assertThat(status(tooDeep)).isEqualTo(422);
        assertThat((String) read(tooDeep, "$.errors[0].field")).isEqualTo("parent_id");

        // con thừa hưởng hoa hồng của cha (effective) nhưng không có tỷ lệ riêng
        MvcResult tree = send("GET", BASE, admin, null);
        List<Object> childRate = read(tree, "$.data[?(@.id == " + rootId + ")].children[0].commission_rate");
        List<Number> childEffective = read(tree, "$.data[?(@.id == " + rootId + ")].children[0].effective_commission_rate");
        assertThat(childRate.get(0)).isNull();
        assertThat(childEffective.get(0).doubleValue()).isEqualTo(6.5);
        assertThat(((Number) read(tree, "$.summary.root_count")).longValue()).isPositive();
        // hoa hồng đổi → nhật ký
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'COMMISSION_UPDATED' "
                + "AND reference_id = ?", Integer.class, String.valueOf(rootId))).isEqualTo(1);
    }

    @Test
    void duplicateSlugOrNameUnderTheSameParentIsRejected() throws Exception {
        String admin = adminToken();
        String w = word();
        long first = createCategory(admin, "Đồ cổ " + w, null);
        MvcResult sameSlug = send("POST", BASE, admin, "{\"name\":\"Khác " + w + "\",\"slug\":\"do-co-" + w + "\"}");
        assertThat(status(sameSlug)).isEqualTo(409);
        assertThat((String) read(sameSlug, "$.code")).isEqualTo("GNE-CAT-4091");
        assertThat(((Number) read(sameSlug, "$.details.existing_category.id")).longValue()).isEqualTo(first);
        MvcResult sameName = send("POST", BASE, admin, "{\"name\":\"ĐỒ CỔ " + w.toUpperCase() + "\",\"slug\":\"x-" + w + "\"}");
        assertThat((String) read(sameName, "$.code")).isEqualTo("GNE-CAT-4091");
        // cùng tên nhưng khác cha thì được
        long other = createCategory(admin, "Gốc " + w, null);
        assertThat(status(send("POST", BASE, admin, "{\"name\":\"Đồ cổ " + w + "\",\"slug\":\"do-co-con-" + w
                + "\",\"parent_id\":" + other + "}"))).isEqualTo(201);
        assertThat(status(send("POST", BASE, admin, "{\"name\":\"Sai\",\"slug\":\"Có Dấu\"}"))).isEqualTo(422);
    }

    @Test
    void moveRulesAndHideShow() throws Exception {
        String admin = adminToken();
        String w = word();
        long a = createCategory(admin, "A " + w, null);
        long a1 = createCategory(admin, "A1 " + w, a);
        long a11 = createCategory(admin, "A11 " + w, a1);
        long b = createCategory(admin, "B " + w, null);
        // vào chính con cháu của nó
        assertThat((String) read(send("PATCH", BASE + "/" + a, admin, "{\"parent_id\":" + a11 + "}"), "$.errors[0].code"))
                .isEqualTo("CYCLE");
        // nhánh A1 (2 cấp) chuyển xuống dưới B1 → 4 cấp
        long b1 = createCategory(admin, "B1 " + w, b);
        assertThat(status(send("PATCH", BASE + "/" + a1, admin, "{\"parent_id\":" + b1 + "}"))).isEqualTo(422);
        // chuyển A1 sang dưới B → được
        MvcResult moved = send("PATCH", BASE + "/" + a1, admin, "{\"parent_id\":" + b + "}");
        assertThat(status(moved)).isEqualTo(200);
        assertThat(((Number) read(moved, "$.data.parent_id")).longValue()).isEqualTo(b);
        // ẩn / hiện
        assertThat((Boolean) read(send("PATCH", BASE + "/" + b, admin, "{\"is_active\":false}"), "$.data.is_active"))
                .isFalse();
        assertThat(jdbc.queryForObject("SELECT is_active FROM categories WHERE id = ?", Boolean.class, a11)).isTrue();
    }

    @Test
    void deleteOnlyWhenEmpty() throws Exception {
        String admin = adminToken();
        String w = word();
        long parent = createCategory(admin, "Xóa " + w, null);
        long child = createCategory(admin, "Con " + w, parent);
        MvcResult hasChildren = send("DELETE", BASE + "/" + parent, admin, null);
        assertThat((String) read(hasChildren, "$.code")).isEqualTo("GNE-CAT-4092");
        assertThat((String) read(hasChildren, "$.details.reason")).isEqualTo("HAS_CHILDREN");

        var shop = data.shop().create();
        data.product(shop).category(child).status("DRAFT").create();   // mọi trạng thái trừ DELETED đều chặn xóa
        assertThat((String) read(send("DELETE", BASE + "/" + child, admin, null), "$.details.reason"))
                .isEqualTo("HAS_PRODUCTS");

        long empty = createCategory(admin, "Rỗng " + w, parent);
        assertThat(status(send("DELETE", BASE + "/" + empty, admin, null))).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM categories WHERE id = ?", Integer.class, empty)).isZero();
        assertThat(status(send("DELETE", BASE + "/" + empty, admin, null))).isEqualTo(404);
    }

    @Test
    void commissionFieldNeedsUpdateCommission() throws Exception {
        String moderator = staffToken("MANAGE_CATALOG");
        String w = word();
        assertThat(status(send("POST", BASE, moderator, "{\"name\":\"Có hoa hồng " + w + "\",\"commission_rate\":5}")))
                .isEqualTo(403);
        long id = createCategory(moderator, "Không hoa hồng " + w, null);
        // đổi tên + gửi lại đúng tỷ lệ đang có (null) → không cần quyền hoa hồng
        assertThat(status(send("PATCH", BASE + "/" + id, moderator, "{\"name\":\"Đổi tên " + w
                + "\",\"commission_rate\":null}"))).isEqualTo(200);
        assertThat(status(send("PATCH", BASE + "/" + id, moderator, "{\"commission_rate\":5}"))).isEqualTo(403);
        assertThat(status(send("PATCH", BASE + "/" + id, adminToken(), "{\"commission_rate\":9}"))).isEqualTo(422);
        assertThat(status(send("GET", BASE, staffToken("UPDATE_COMMISSION"), null))).isEqualTo(403);
        assertThat(status(send("GET", BASE, null, null))).isEqualTo(401);
    }

    /** API 11.11: ngành đang được voucher tham chiếu → {@code 4092 IN_USE}; gỡ tham chiếu thì xóa được (plan 2026-11-24 H06.02). */
    @Test
    void deleteRejectedWhileAVoucherReferencesTheCategory() throws Exception {
        String admin = adminToken();
        long cat = createCategory(admin, "Có voucher " + word(), null);
        Long voucher = jdbc.queryForObject("""
                INSERT INTO vouchers (code, title, scope, type, discount_type, discount_value, min_order_value, usage_limit,
                                      usage_per_user, used_count, priority, valid_from, valid_to, is_active, version,
                                      total_discount_used, sponsor_type)
                VALUES (?, 'Voucher theo ngành', 'CATEGORY', 'PLATFORM', 'FIXED_AMOUNT', 10000, 0, 10, 1, 0, 1,
                        now(), now() + interval '7 days', TRUE, 0, 0, 'PLATFORM') RETURNING id""",
                Long.class, "CAT" + word().toUpperCase());
        jdbc.update("INSERT INTO voucher_categories (voucher_id, category_id, created_at) VALUES (?, ?, now())", voucher, cat);
        try {
            MvcResult inUse = send("DELETE", BASE + "/" + cat, admin, null);
            assertThat(status(inUse)).isEqualTo(409);
            assertThat((String) read(inUse, "$.code")).isEqualTo("GNE-CAT-4092");
            assertThat((String) read(inUse, "$.details.reason")).isEqualTo("IN_USE");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM categories WHERE id = ?", Integer.class, cat)).isEqualTo(1);
        } finally {
            jdbc.update("DELETE FROM voucher_categories WHERE category_id = ?", cat);
        }
        assertThat(status(send("DELETE", BASE + "/" + cat, admin, null))).isEqualTo(204);
    }
}
