package com.genie.catalog.category;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.common.domain.UserRole;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Ma trận vai trò × mọi endpoint của cụm B2 (UC-CAT-01, 14, 15) — TECH_STACK 14 ("ma trận 13 mã quyền"),
 * LLD 11.1, API 11.8 / 11.11; plan 2026-11-24 H06.01.
 * Vai trò: khách (không token), CUSTOMER, VENDOR, CS (không có quyền danh mục), Moderator ({@code MANAGE_CATALOG}),
 * Finance ({@code UPDATE_COMMISSION}), ADMIN. Mỗi ca thay đổi dữ liệu dùng danh mục riêng của nó.
 */
class CatalogSecurityIT extends CategoryITSupport {

    private static final String ADMIN_BASE = "/api/v1/admin/categories";
    private static final String COMMISSION = "/api/v1/admin/commission-settings";

    private record Role(String name, String token) {
    }

    private String roleToken(UserRole role) {
        var user = createCustomer(uniqueEmail(), PASSWORD);
        if (role != UserRole.CUSTOMER) {
            jdbc.update("UPDATE users SET role = ? WHERE id = ?", role.name(), user.getId());
        }
        return bearer(jwtService.issue(user.getId(), role, 1L).token());
    }

    private List<Role> roles() {
        List<Role> all = new ArrayList<>();
        all.add(new Role("GUEST", null));
        all.add(new Role("CUSTOMER", roleToken(UserRole.CUSTOMER)));
        all.add(new Role("VENDOR", roleToken(UserRole.VENDOR)));
        all.add(new Role("CS", staffToken("VIEW_DASHBOARD", "MODERATE_REVIEW", "HANDLE_DISPUTE")));
        all.add(new Role("MODERATOR", staffToken("VIEW_DASHBOARD", "REVIEW_KYC", "MANAGE_CATALOG", "MODERATE_REVIEW")));
        all.add(new Role("FINANCE", staffToken("VIEW_DASHBOARD", "UPDATE_COMMISSION", "VIEW_FINANCE", "PROCESS_PAYOUT")));
        all.add(new Role("ADMIN", adminToken()));
        return all;
    }

    /** Kỳ vọng theo vai trò: GUEST, CUSTOMER, VENDOR, CS, MODERATOR, FINANCE, ADMIN. */
    private static Map<String, Integer> expect(int guest, int customer, int vendor, int cs, int mod, int fin, int admin) {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("GUEST", guest);
        m.put("CUSTOMER", customer);
        m.put("VENDOR", vendor);
        m.put("CS", cs);
        m.put("MODERATOR", mod);
        m.put("FINANCE", fin);
        m.put("ADMIN", admin);
        return m;
    }

    private void assertCode(MvcResult r, int status) throws Exception {
        if (status == 401) assertThat((String) read(r, "$.code")).isEqualTo("GNE-SEC-4010");
        if (status == 403) assertThat((String) read(r, "$.code")).isEqualTo("GNE-SEC-4030");
    }

    @Test
    void publicTreeIsOpenToEveryone() throws Exception {
        String w = word();
        createCategory(adminToken(), "Công khai " + w, null);
        for (Role role : roles()) {
            assertThat(status(send("GET", "/api/v1/public/categories", role.token(), null))).as(role.name()).isEqualTo(200);
            assertThat(status(send("GET", "/api/v1/public/categories/cong-khai-" + w, role.token(), null))).as(role.name())
                    .isEqualTo(200);
        }
    }

    @Test
    void categoryManagementMatrix() throws Exception {
        String admin = adminToken();
        Map<String, Integer> readOnly = expect(401, 403, 403, 403, 200, 403, 200);
        Map<String, Integer> create = expect(401, 403, 403, 403, 201, 403, 201);
        Map<String, Integer> createWithRate = expect(401, 403, 403, 403, 403, 403, 201);
        Map<String, Integer> rename = expect(401, 403, 403, 403, 200, 403, 200);
        Map<String, Integer> changeRate = expect(401, 403, 403, 403, 403, 403, 200);
        Map<String, Integer> remove = expect(401, 403, 403, 403, 204, 403, 204);

        for (Role role : roles()) {
            String w = word();
            String who = role.name();

            MvcResult list = send("GET", ADMIN_BASE, role.token(), null);
            assertThat(status(list)).as(who + " GET").isEqualTo(readOnly.get(who));
            assertCode(list, readOnly.get(who));

            MvcResult created = send("POST", ADMIN_BASE, role.token(), "{\"name\":\"Bảo mật " + w + "\"}");
            assertThat(status(created)).as(who + " POST").isEqualTo(create.get(who));
            assertCode(created, create.get(who));

            MvcResult withRate = send("POST", ADMIN_BASE, role.token(),
                    "{\"name\":\"Có hoa hồng " + w + "\",\"commission_rate\":5}");
            assertThat(status(withRate)).as(who + " POST commission_rate").isEqualTo(createWithRate.get(who));
            assertCode(withRate, createWithRate.get(who));

            long target = createCategory(admin, "Đích " + w, null);
            MvcResult renamed = send("PATCH", ADMIN_BASE + "/" + target, role.token(), "{\"name\":\"Đổi tên " + w + "\"}");
            assertThat(status(renamed)).as(who + " PATCH name").isEqualTo(rename.get(who));
            assertCode(renamed, rename.get(who));

            MvcResult rate = send("PATCH", ADMIN_BASE + "/" + target, role.token(), "{\"commission_rate\":6}");
            assertThat(status(rate)).as(who + " PATCH commission_rate").isEqualTo(changeRate.get(who));
            assertCode(rate, changeRate.get(who));

            long doomed = createCategory(admin, "Xóa " + w, null);
            MvcResult deleted = send("DELETE", ADMIN_BASE + "/" + doomed, role.token(), null);
            assertThat(status(deleted)).as(who + " DELETE").isEqualTo(remove.get(who));
            assertCode(deleted, remove.get(who));
        }
    }

    /** API 11.11: chỉ kiểm {@code UPDATE_COMMISSION} khi giá trị hoa hồng thật sự đổi. */
    @Test
    void moderatorMaySendTheUnchangedCommissionRate() throws Exception {
        String w = word();
        MvcResult created = send("POST", ADMIN_BASE, adminToken(), "{\"name\":\"Giữ hoa hồng " + w + "\",\"commission_rate\":6.5}");
        long id = ((Number) read(created, "$.data.id")).longValue();
        String moderator = staffToken("MANAGE_CATALOG");

        assertThat(status(send("PATCH", ADMIN_BASE + "/" + id, moderator,
                "{\"name\":\"Giữ hoa hồng mới " + w + "\",\"commission_rate\":6.5}"))).isEqualTo(200);
        assertThat(status(send("PATCH", ADMIN_BASE + "/" + id, moderator, "{\"commission_rate\":7}"))).isEqualTo(403);
        assertThat(status(send("PATCH", ADMIN_BASE + "/" + id, moderator, "{\"commission_rate\":null}"))).isEqualTo(403);
    }

    @Test
    void attributeMatrix() throws Exception {
        String admin = adminToken();
        Map<String, Integer> read = expect(401, 403, 403, 403, 200, 403, 200);
        Map<String, Integer> write = expect(401, 403, 403, 403, 201, 403, 201);
        Map<String, Integer> update = expect(401, 403, 403, 403, 200, 403, 200);

        for (Role role : roles()) {
            String who = role.name();
            long cat = createCategory(admin, "Thuộc tính " + word(), null);
            String base = ADMIN_BASE + "/" + cat + "/attributes";
            String code = "sec_" + word().toLowerCase();

            assertThat(status(send("GET", base, role.token(), null))).as(who + " GET").isEqualTo(read.get(who));
            MvcResult created = send("POST", base, role.token(),
                    "{\"code\":\"" + code + "\",\"name\":\"Bảo hành\",\"data_type\":\"TEXT\"}");
            assertThat(status(created)).as(who + " POST").isEqualTo(write.get(who));
            assertCode(created, write.get(who));

            // PUT / PATCH trên thuộc tính do Admin tạo sẵn
            String own = "own_" + word().toLowerCase();
            assertThat(status(send("POST", base, admin, "{\"code\":\"" + own + "\",\"name\":\"Xuất xứ\",\"data_type\":\"TEXT\"}")))
                    .isEqualTo(201);
            MvcResult put = send("PUT", base + "/" + own, role.token(), "{\"name\":\"Xuất xứ mới\",\"data_type\":\"TEXT\"}");
            assertThat(status(put)).as(who + " PUT").isEqualTo(update.get(who));
            MvcResult patch = send("PATCH", base + "/" + own, role.token(), "{\"is_active\":false}");
            assertThat(status(patch)).as(who + " PATCH").isEqualTo(update.get(who));
        }
    }

    @Test
    void commissionSettingsMatrix() throws Exception {
        Map<String, Integer> read = expect(401, 403, 403, 403, 403, 200, 200);
        Map<String, Integer> write = expect(401, 403, 403, 403, 403, 200, 200);
        try {
            for (Role role : roles()) {
                String who = role.name();
                MvcResult get = send("GET", COMMISSION, role.token(), null);
                assertThat(status(get)).as(who + " GET").isEqualTo(read.get(who));
                assertCode(get, read.get(who));
                MvcResult patch = send("PATCH", COMMISSION, role.token(), "{\"default_rate\":5}");
                assertThat(status(patch)).as(who + " PATCH").isEqualTo(write.get(who));
                assertCode(patch, write.get(who));
            }
        } finally {
            send("PATCH", COMMISSION, adminToken(), "{\"default_rate\":5}");
        }
    }

    /** Bị chặn 403 → nhật ký {@code ACCESS_DENIED} kèm đường dẫn (plan B1 D14); 401 không ghi (chưa biết là ai). */
    @Test
    void forbiddenRequestsAreAudited() throws Exception {
        String cs = staffToken("HANDLE_DISPUTE");
        assertThat(status(send("GET", COMMISSION, cs, null))).isEqualTo(403);
        assertThat(status(send("GET", ADMIN_BASE, null, null))).isEqualTo(401);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'ACCESS_DENIED' "
                + "AND details LIKE '%/api/v1/admin/commission-settings%'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'ACCESS_DENIED' "
                + "AND details LIKE '%\"/api/v1/admin/categories\"%'", Integer.class)).isZero();
    }

    /** Thu hồi {@code MANAGE_CATALOG} qua API RBAC → request kế tiếp của nhân viên bị chặn ngay (plan B1 D19). */
    @Test
    void revokedPermissionTakesEffectOnTheNextRequest() throws Exception {
        var user = createCustomer(uniqueEmail(), PASSWORD);
        jdbc.update("UPDATE users SET role = 'STAFF' WHERE id = ?", user.getId());
        jdbc.update("INSERT INTO admin_profiles (user_id, full_name, is_active_moderator, staff_title, created_at, updated_at) "
                + "VALUES (?, 'Kiểm duyệt test', FALSE, 'MODERATOR', now(), now())", user.getId());
        String staff = bearer(jwtService.issue(user.getId(), UserRole.STAFF, 1L).token());
        String admin = adminToken();

        assertThat(status(send("PUT", "/api/v1/admin/staff/" + user.getId() + "/permissions", admin,
                "{\"staff_title\":\"MODERATOR\",\"permissions\":[\"VIEW_DASHBOARD\",\"MANAGE_CATALOG\"]}"))).isEqualTo(200);
        assertThat(status(send("GET", ADMIN_BASE, staff, null))).isEqualTo(200);

        assertThat(status(send("PUT", "/api/v1/admin/staff/" + user.getId() + "/permissions", admin,
                "{\"staff_title\":\"MODERATOR\",\"permissions\":[\"VIEW_DASHBOARD\"]}"))).isEqualTo(200);
        assertThat(status(send("GET", ADMIN_BASE, staff, null))).isEqualTo(403);
    }
}
