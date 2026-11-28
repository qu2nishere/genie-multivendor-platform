// Tiện ích chung cho IT voucher (plan 2026-11-26 voucher V03–V05)
package com.genie.promotion.voucher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.support.IntegrationTestBase;
import com.genie.support.TestDataBuilder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public abstract class VoucherTestSupport extends IntegrationTestBase {

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected ObjectMapper mapper;

    protected TestDataBuilder data;

    /** Kết quả gọi API: mã HTTP + thân JSON. */
    protected record Res(int status, JsonNode body) {
        public JsonNode data() {
            return body.get("data");
        }

        public String code() {
            return body.path("code").asText();
        }
    }

    @BeforeEach
    void voucherSupport() {
        data = new TestDataBuilder(jdbc);
    }

    protected OffsetDateTime now() {
        return OffsetDateTime.parse(START);
    }

    protected static String code() {
        return "V" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
    }

    protected String auth(long userId, UserRole role) {
        return "Bearer " + jwtService.issue(userId, role, 1L).token();
    }

    protected long customer() {
        return createCustomer(uniqueEmail(), PASSWORD).getId();
    }

    /** Nhân viên có quyền cho trước. */
    protected long staff(String... permissions) {
        long id = customer();
        jdbc.update("UPDATE users SET role = 'STAFF' WHERE id = ?", id);
        jdbc.update("INSERT INTO admin_profiles (user_id, full_name, is_active_moderator, staff_title, created_at, updated_at) "
                + "VALUES (?, 'Nhân viên test', FALSE, NULL, now(), now())", id);
        Long admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        for (String p : permissions) {
            jdbc.update("INSERT INTO staff_permissions (staff_user_id, permission, granted_by_admin_id, granted_at) "
                    + "VALUES (?, ?, ?, now())", id, p, admin);
        }
        return id;
    }

    protected Res call(MockHttpServletRequestBuilder req, String auth, String body) throws Exception {
        if (auth != null) req.header(HttpHeaders.AUTHORIZATION, auth);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(body);
        MvcResult r = mvc.perform(req).andReturn();
        String s = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new Res(r.getResponse().getStatus(), s.isEmpty() ? mapper.createObjectNode() : mapper.readTree(s));
    }

    protected Res get_(String path, String auth) throws Exception {
        return call(get(path), auth, null);
    }

    protected Res post_(String path, String auth, String body) throws Exception {
        return call(post(path), auth, body);
    }

    protected Res put_(String path, String auth, String body) throws Exception {
        return call(put(path), auth, body);
    }

    protected Res patch_(String path, String auth, String body) throws Exception {
        return call(patch(path), auth, body);
    }

    protected Res delete_(String path, String auth) throws Exception {
        return call(delete(path), auth, null);
    }

    /** Thân voucher % (15% tối đa 50k, đơn từ 200k) — {@code from}/{@code to} theo đồng hồ test. */
    protected String percentBody(String code, OffsetDateTime from, OffsetDateTime to, String extra) {
        return "{\"code\":\"" + code + "\",\"discount_type\":\"PERCENTAGE\",\"discount_value\":15,\"max_discount\":50000,"
                + "\"min_order_value\":200000,\"usage_limit\":500,\"usage_per_user\":1,\"valid_from\":\"" + from
                + "\",\"valid_to\":\"" + to + "\",\"scope\":\"ALL\"" + (extra == null ? "" : "," + extra) + "}";
    }

    /** Một lượt dùng voucher thật (có phiên thanh toán — FK); {@code orderId} có thể {@code null}. */
    protected void usage(long voucherId, long userId, OffsetDateTime at, Long orderId) {
        long session = jdbc.queryForObject("INSERT INTO checkout_sessions (user_id, total_amount, status, expires_at, "
                + "created_at) VALUES (?, 100000, 'COMPLETED', now(), now()) RETURNING id", Long.class, userId);
        jdbc.update("INSERT INTO voucher_usages (voucher_id, user_id, checkout_session_id, order_id, used_at) "
                + "VALUES (?, ?, ?, ?, ?)", voucherId, userId, session, orderId, at);
        jdbc.update("UPDATE vouchers SET used_count = used_count + 1 WHERE id = ?", voucherId);
    }

    /** Voucher quà đổi Điểm {@code COIN500} như seed (CSDL test không nạp seed) — chạy lại được. */
    protected long coinRewardVoucher() {
        jdbc.update("""
                INSERT INTO vouchers (code, title, scope, type, discount_type, discount_value, min_order_value, usage_limit,
                    usage_per_user, used_count, priority, valid_from, valid_to, is_active, version, total_discount_used,
                    sponsor_type)
                VALUES ('COIN500', 'Quà đổi Điểm: giảm 5.000₫', 'ALL', 'PLATFORM', 'FIXED_AMOUNT', 5000, 50000, 100000, 10, 0, 2,
                    now() - interval '1 day', now() + interval '365 days', TRUE, 0, 0, 'PLATFORM')
                ON CONFLICT (code) DO NOTHING""");
        return jdbc.queryForObject("SELECT id FROM vouchers WHERE code = 'COIN500'", Long.class);
    }

    protected long category() {
        return data.category();
    }
}
