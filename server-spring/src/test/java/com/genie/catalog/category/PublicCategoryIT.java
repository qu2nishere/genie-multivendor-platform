package com.genie.catalog.category;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** API 5.1, 5.2 — cây danh mục công khai (UC-CAT-01; plan B2 C03.03, D5). */
class PublicCategoryIT extends CategoryITSupport {

    @Test
    void treeHeaderAndHiddenBranches() throws Exception {
        String admin = adminToken();
        String w = word();
        long root = createCategory(admin, "Điện gia dụng " + w, null);
        long child = createCategory(admin, "Bếp " + w, root);
        long leaf = createCategory(admin, "Nồi chiên " + w, child);
        var shop = data.shop().create();
        data.product(shop).category(leaf).create();                       // PUBLISHED
        data.product(shop).category(child).status("DRAFT").create();      // không đếm ở trang công khai

        MvcResult tree = call("/api/v1/public/categories");
        assertThat(status(tree)).isEqualTo(200);
        assertThat(tree.getResponse().getHeader("Cache-Control")).contains("max-age=300");
        List<String> leafSlugs = read(tree, "$.data[?(@.id == " + root + ")].children[0].children[0].slug");
        assertThat(leafSlugs).containsExactly("noi-chien-" + w);

        MvcResult header = call("/api/v1/public/categories/bep-" + w);
        assertThat(status(header)).isEqualTo(200);
        assertThat((List<String>) read(header, "$.data.breadcrumb[*].slug"))
                .containsExactly("dien-gia-dung-" + w, "bep-" + w);
        assertThat((List<String>) read(header, "$.data.children[*].slug")).containsExactly("noi-chien-" + w);
        assertThat(((Number) read(header, "$.data.product_count")).longValue()).isEqualTo(1);
        assertThat(((Number) read(header, "$.data.parent_id")).longValue()).isEqualTo(root);

        // ẩn cha → cả nhánh biến mất (cờ của con không đổi); cache bị xóa sau commit
        send("PATCH", "/api/v1/admin/categories/" + root, admin, "{\"is_active\":false}");
        assertThat((List<Object>) read(call("/api/v1/public/categories"), "$.data[?(@.id == " + root + ")]")).isEmpty();
        MvcResult hidden = call("/api/v1/public/categories/noi-chien-" + w);
        assertThat(status(hidden)).isEqualTo(404);
        assertThat((String) read(hidden, "$.code")).isEqualTo("GNE-CAT-4041");
        assertThat(status(call("/api/v1/public/categories/khong-co-" + w))).isEqualTo(404);
    }

    @Test
    void renameIsVisibleRightAfterCommit() throws Exception {
        String admin = adminToken();
        String w = word();
        long id = createCategory(admin, "Cũ " + w, null);
        call("/api/v1/public/categories");   // nạp cache
        send("PATCH", "/api/v1/admin/categories/" + id, admin, "{\"name\":\"Mới " + w + "\",\"slug\":\"moi-" + w + "\"}");
        List<String> names = read(call("/api/v1/public/categories"), "$.data[?(@.id == " + id + ")].name");
        assertThat(names).containsExactly("Mới " + w);
    }
}
