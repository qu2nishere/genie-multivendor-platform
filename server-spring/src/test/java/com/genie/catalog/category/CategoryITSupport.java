package com.genie.catalog.category;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.genie.catalog.CatalogITSupport;
import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.support.TestDataBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** Nền chung cho IT danh mục sàn (plan B2): token Admin / nhân viên theo mã quyền, gọi API có thân JSON. */
abstract class CategoryITSupport extends CatalogITSupport {

    @Autowired
    protected JwtService jwtService;

    protected String adminToken() {
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        return bearer(jwtService.issue(id, UserRole.ADMIN, 1L).token());
    }

    /** Nhân viên mới chỉ có các mã quyền cho trước. */
    protected String staffToken(String... permissions) {
        var user = createCustomer(uniqueEmail(), PASSWORD);
        jdbc.update("UPDATE users SET role = 'STAFF' WHERE id = ?", user.getId());
        Long admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        for (String p : permissions) {
            jdbc.update("INSERT INTO staff_permissions (staff_user_id, permission, granted_by_admin_id, granted_at) "
                    + "VALUES (?, ?, ?, now())", user.getId(), p, admin);
        }
        return bearer(jwtService.issue(user.getId(), UserRole.STAFF, 1L).token());
    }

    protected MvcResult send(String method, String path, String token, String json) throws Exception {
        var req = switch (method) {
            case "POST" -> post(path);
            case "PATCH" -> patch(path);
            case "PUT" -> put(path);
            case "DELETE" -> delete(path);
            default -> get(path);
        };
        if (token != null) req.header(HttpHeaders.AUTHORIZATION, token);
        if (json != null) req.contentType(MediaType.APPLICATION_JSON).content(json);
        return mvc.perform(req).andReturn();
    }

    protected static String word() {
        return TestDataBuilder.uniqueWord();
    }

    /** Tạo danh mục qua API Admin, trả id. */
    protected long createCategory(String token, String name, Long parentId) throws Exception {
        MvcResult r = send("POST", "/api/v1/admin/categories", token,
                "{\"name\":\"" + name + "\",\"parent_id\":" + parentId + "}");
        if (status(r) != 201) throw new AssertionError("Tạo danh mục lỗi " + status(r) + ": "
                + r.getResponse().getContentAsString());
        return ((Number) read(r, "$.data.id")).longValue();
    }
}
