package com.genie.catalog.category;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** API 11.11 "Bộ thuộc tính" (UC-CAT-15; plan B2 C06.04, D8–D12). */
class CategoryAttributeIT extends CategoryITSupport {

    private static String attrs(long id) {
        return "/api/v1/admin/categories/" + id + "/attributes";
    }

    @Test
    void createListInheritAndRejectDuplicatesInTheFamily() throws Exception {
        String admin = adminToken();
        String w = word();
        long root = createCategory(admin, "Điện tử " + w, null);
        long child = createCategory(admin, "Điện thoại " + w, root);

        MvcResult created = send("POST", attrs(root), admin, "{\"code\":\"warranty\",\"name\":\"Bảo hành\","
                + "\"data_type\":\"ENUM\",\"values\":[\"6 tháng\",\"12 tháng\"],\"is_required\":true,"
                + "\"is_filterable\":true}");
        assertThat(status(created)).isEqualTo(201);
        assertThat((List<String>) read(created, "$.data.values")).containsExactly("6 tháng", "12 tháng");
        assertThat(created.getResponse().getContentAsString()).doesNotContain("grace_period_days");   // chưa có SP

        MvcResult childList = send("GET", attrs(child), admin, null);
        assertThat((List<Object>) read(childList, "$.data")).isEmpty();
        assertThat((List<String>) read(childList, "$.summary.inherited[*].code")).containsExactly("warranty");

        MvcResult dupChild = send("POST", attrs(child), admin, "{\"code\":\"warranty\",\"name\":\"BH\",\"data_type\":\"TEXT\"}");
        assertThat((String) read(dupChild, "$.code")).isEqualTo("GNE-CAT-4095");
        assertThat(((Number) read(dupChild, "$.details.category_id")).longValue()).isEqualTo(root);
        // thêm ở con trước rồi thêm cùng mã ở cha → cũng chặn
        send("POST", attrs(child), admin, "{\"code\":\"screen_size\",\"name\":\"Màn hình\",\"data_type\":\"NUMBER\",\"unit\":\"inch\"}");
        assertThat((String) read(send("POST", attrs(root), admin,
                "{\"code\":\"screen_size\",\"name\":\"Màn hình\",\"data_type\":\"NUMBER\"}"), "$.code")).isEqualTo("GNE-CAT-4095");
    }

    @Test
    void fieldRules() throws Exception {
        String admin = adminToken();
        long id = createCategory(admin, "Quy tắc " + word(), null);
        assertThat(status(send("POST", attrs(id), admin, "{\"code\":\"Bad-Code\",\"name\":\"X\",\"data_type\":\"TEXT\"}")))
                .isEqualTo(422);
        assertThat(status(send("POST", attrs(id), admin, "{\"code\":\"color\",\"name\":\"Màu\",\"data_type\":\"ENUM\"}")))
                .isEqualTo(422);   // ENUM thiếu values
        assertThat(status(send("POST", attrs(id), admin, "{\"code\":\"weight\",\"name\":\"Nặng\",\"data_type\":\"TEXT\","
                + "\"unit\":\"kg\"}"))).isEqualTo(422);   // đơn vị chỉ cho NUMBER
        assertThat(status(send("POST", attrs(id), admin, "{\"code\":\"note\",\"name\":\"Ghi chú\",\"data_type\":\"TEXT\","
                + "\"is_variant_axis\":true}"))).isEqualTo(422);
        send("POST", attrs(id), admin, "{\"code\":\"origin\",\"name\":\"Xuất xứ\",\"data_type\":\"TEXT\"}");
        MvcResult changeType = send("PUT", attrs(id) + "/origin", admin, "{\"name\":\"Xuất xứ\",\"data_type\":\"ENUM\","
                + "\"values\":[\"VN\"]}");
        assertThat((String) read(changeType, "$.errors[0].field")).isEqualTo("data_type");
        assertThat(status(send("PUT", attrs(id) + "/khong_co", admin, "{\"name\":\"X\"}"))).isEqualTo(404);
        assertThat(status(send("GET", attrs(999999999L), admin, null))).isEqualTo(404);
    }

    @Test
    void requiredOnBranchWithProductsGivesGracePeriodAndDeactivateDropsRequired() throws Exception {
        String admin = adminToken();
        String w = word();
        long root = createCategory(admin, "Mỹ phẩm " + w, null);
        long child = createCategory(admin, "Son " + w, root);
        data.product(data.shop().create()).category(child).create();

        MvcResult created = send("POST", attrs(root), admin, "{\"code\":\"expiry_date\",\"name\":\"Hạn dùng\","
                + "\"data_type\":\"DATE\",\"is_required\":true}");
        assertThat((Integer) read(created, "$.data.grace_period_days")).isEqualTo(14);

        send("POST", attrs(root), admin, "{\"code\":\"ingredients\",\"name\":\"Thành phần\",\"data_type\":\"TEXT\"}");
        MvcResult nowRequired = send("PUT", attrs(root) + "/ingredients", admin, "{\"name\":\"Thành phần\","
                + "\"data_type\":\"TEXT\",\"is_required\":true}");
        assertThat((Integer) read(nowRequired, "$.data.grace_period_days")).isEqualTo(14);

        MvcResult off = send("PATCH", attrs(root) + "/expiry_date", admin, "{\"is_active\":false}");
        assertThat((Boolean) read(off, "$.data.is_active")).isFalse();
        assertThat((Boolean) read(off, "$.data.is_required")).isFalse();
        MvcResult on = send("PATCH", attrs(root) + "/expiry_date", admin, "{\"is_active\":true}");
        assertThat((Boolean) read(on, "$.data.is_active")).isTrue();
        assertThat((Boolean) read(on, "$.data.is_required")).isFalse();   // không tự bật lại (D12)

        assertThat(status(send("GET", attrs(root), staffToken("UPDATE_COMMISSION"), null))).isEqualTo(403);
        assertThat(status(send("GET", attrs(root), staffToken("MANAGE_CATALOG"), null))).isEqualTo(200);
    }
}
