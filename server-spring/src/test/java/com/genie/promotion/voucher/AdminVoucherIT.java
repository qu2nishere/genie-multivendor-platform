// Voucher sàn qua API thật (plan 2026-11-26 voucher V04.04; UC-PRO-02; API_SPEC 11.14)
package com.genie.promotion.voucher;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.genie.common.domain.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class AdminVoucherIT extends VoucherTestSupport {

    @Autowired
    PlatformTransactionManager transactionManager;

    private String finance() {
        return auth(staff("MANAGE_VOUCHER"), UserRole.STAFF);
    }

    private String platformBody(String code, OffsetDateTime from, OffsetDateTime to, String extra) {
        return percentBody(code, from, to, "\"title\":\"Siêu Sale Giữa Tháng\"" + (extra == null ? "" : "," + extra));
    }

    private List<Map<String, Object>> logs(long voucherId) {
        return jdbc.queryForList("SELECT action_type, details FROM system_logs WHERE reference_id = ? ORDER BY created_at, id",
                String.valueOf(voucherId));
    }

    @Test
    void createsPlatformFreeshipAndCoSponsoredVouchersWithAudit() throws Exception {
        String fo = finance();
        OffsetDateTime from = now().plusHours(1);
        OffsetDateTime to = now().plusDays(10);
        Res p = post_("/api/v1/admin/vouchers", fo, platformBody(code(), from, to, "\"budget\":5000000"));
        assertThat(p.status()).isEqualTo(201);
        assertThat(p.data().get("type").asText()).isEqualTo("PLATFORM");
        assertThat(p.data().get("funded_by").asText()).isEqualTo("PLATFORM");
        assertThat(p.data().get("title").asText()).isEqualTo("Siêu Sale Giữa Tháng");
        assertThat(p.data().get("budget").asLong()).isEqualTo(5_000_000);
        long id = p.data().get("id").asLong();
        assertThat(jdbc.queryForObject("SELECT vendor_id FROM vouchers WHERE id = ?", Long.class, id)).isNull();
        assertThat(logs(id)).extracting(m -> m.get("action_type")).containsExactly("VOUCHER_CREATED");

        Res ship = post_("/api/v1/admin/vouchers", fo, "{\"title\":\"Freeship toàn sàn\",\"code\":\"" + code() + "\","
                + "\"discount_type\":\"FREESHIP\",\"max_discount\":30000,\"min_order_value\":99000,\"usage_limit\":100,"
                + "\"usage_per_user\":1,\"valid_from\":\"" + from + "\",\"valid_to\":\"" + to + "\"}");
        assertThat(ship.status()).isEqualTo(201);
        assertThat(ship.data().get("type").asText()).isEqualTo("FREESHIP");

        Res noShare = post_("/api/v1/admin/vouchers", fo, platformBody(code(), from, to, "\"funded_by\":\"CO_SPONSOR\""));
        assertThat(noShare.status()).isEqualTo(422);
        assertThat(noShare.body().at("/errors/0/field").asText()).isEqualTo("platform_share_percent");
        Res co = post_("/api/v1/admin/vouchers", fo, platformBody(code(), from, to,
                "\"funded_by\":\"CO_SPONSOR\",\"platform_share_percent\":60"));
        assertThat(co.status()).isEqualTo(201);
        assertThat(co.data().get("platform_share_percent").asInt()).isEqualTo(60);

        assertThat(post_("/api/v1/admin/vouchers", fo, platformBody(code(), now().minusDays(1), to, null)).status())
                .isEqualTo(422);
        assertThat(post_("/api/v1/admin/vouchers", fo, percentBody(code(), from, to, null)).status())
                .isEqualTo(422);   // thiếu tên chương trình
        Res product = post_("/api/v1/admin/vouchers", fo, platformBody(code(), from, to, null)
                .replace("\"scope\":\"ALL\"", "\"scope\":\"PRODUCT\""));
        assertThat(product.status()).isEqualTo(422);
    }

    @Test
    void updateIsAuditedWithChangesAndDisableIsOneWay() throws Exception {
        String fo = finance();
        String c = code();
        long id = post_("/api/v1/admin/vouchers", fo, platformBody(c, now(), now().plusDays(5), null)).data().get("id").asLong();
        Res upd = put_("/api/v1/admin/vouchers/" + id, fo, platformBody(c, now(), now().plusDays(8), null)
                .replace("Siêu Sale Giữa Tháng", "Siêu Sale kéo dài"));
        assertThat(upd.status()).isEqualTo(200);
        Res off = patch_("/api/v1/admin/vouchers/" + id, fo, "{\"is_active\":false}");
        assertThat(off.data().get("status").asText()).isEqualTo("DISABLED");
        List<Map<String, Object>> logs = logs(id);
        assertThat(logs).extracting(m -> m.get("action_type"))
                .containsExactly("VOUCHER_CREATED", "VOUCHER_UPDATED", "VOUCHER_DISABLED");
        JsonNode changes = mapper.readTree((String) logs.get(1).get("details")).get("changes");
        assertThat(changes.has("title")).isTrue();
        assertThat(changes.has("valid_to")).isTrue();
        assertThat(changes.has("discount_value")).isFalse();
    }

    @Test
    void budgetExhaustionEndsTheVoucherAndRemainingBudgetNeverGoesNegative() throws Exception {
        String fo = finance();
        long id = post_("/api/v1/admin/vouchers", fo, platformBody(code(), now(), now().plusDays(5), "\"budget\":1000000"))
                .data().get("id").asLong();
        long before = get_("/api/v1/admin/vouchers", fo).body().at("/summary/remaining_budget").asLong();
        jdbc.update("UPDATE vouchers SET total_discount_used = 1200000, is_active = FALSE WHERE id = ?", id);
        JsonNode d = get_("/api/v1/admin/vouchers/" + id, fo).data();
        assertThat(d.get("status").asText()).isEqualTo("ENDED");
        JsonNode list = get_("/api/v1/admin/vouchers?status=ENDED", fo).body();
        assertThat(list.get("data")).extracting(r -> r.get("id").asLong()).contains(id);
        long after = list.at("/summary/remaining_budget").asLong();
        assertThat(after).isGreaterThanOrEqualTo(0).isEqualTo(before - 1_000_000);
        assertThat(list.at("/summary/total_discount_used").asLong()).isGreaterThanOrEqualTo(1_200_000);
    }

    @Test
    void deleteRules() throws Exception {
        String fo = finance();
        long cat = category();
        // đã kết thúc, chưa dùng → xóa sạch cả phạm vi và Kho
        long ended = post_("/api/v1/admin/vouchers", fo, platformBody(code(), now(), now().plusDays(1),
                "\"category_ids\":[" + cat + "]").replace("\"scope\":\"ALL\"", "\"scope\":\"CATEGORY\"")).data().get("id").asLong();
        jdbc.update("INSERT INTO user_vouchers (user_id, voucher_id, created_at) VALUES (?, ?, now())", customer(), ended);
        jdbc.update("UPDATE vouchers SET valid_to = ? WHERE id = ?", now().minusMinutes(1), ended);
        assertThat(delete_("/api/v1/admin/vouchers/" + ended, fo).status()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vouchers WHERE id = ?", Integer.class, ended)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM voucher_categories WHERE voucher_id = ?", Integer.class, ended)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_vouchers WHERE voucher_id = ?", Integer.class, ended)).isZero();
        assertThat(logs(ended)).extracting(m -> m.get("action_type")).contains("VOUCHER_DELETED");

        long used = post_("/api/v1/admin/vouchers", fo, platformBody(code(), now(), now().plusDays(1), null)).data().get("id").asLong();
        usage(used, customer(), now(), null);
        patch_("/api/v1/admin/vouchers/" + used, fo, "{\"is_active\":false}");
        Res usedRes = delete_("/api/v1/admin/vouchers/" + used, fo);
        assertThat(usedRes.status()).isEqualTo(409);
        assertThat(usedRes.code()).isEqualTo("GNE-VCH-4093");

        long running = post_("/api/v1/admin/vouchers", fo, platformBody(code(), now(), now().plusDays(1), null)).data().get("id").asLong();
        assertThat(delete_("/api/v1/admin/vouchers/" + running, fo).code()).isEqualTo("GNE-VCH-4094");

        long coin = coinRewardVoucher();
        JsonNode coinRow = get_("/api/v1/admin/vouchers/" + coin, fo).data();
        assertThat(coinRow.get("is_coin_reward").asBoolean()).isTrue();
        assertThat(delete_("/api/v1/admin/vouchers/" + coin, fo).code()).isEqualTo("GNE-VCH-4094");
        assertThat(get_("/api/v1/admin/vouchers?size=50", fo).body().get("data"))
                .anySatisfy(r -> assertThat(r.get("is_coin_reward").asBoolean()).isTrue());
    }

    @Test
    void concurrentConsumeIsRetriedOnce() throws Exception {
        String fo = finance();
        String c = code();
        long id = post_("/api/v1/admin/vouchers", fo, platformBody(c, now(), now().plusDays(5), null)).data().get("id").asLong();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CountDownLatch locked = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // giả lập C6 trừ lượt: giữ khóa dòng 800 ms rồi tăng version — PUT đang chờ sẽ đụng độ phiên bản
            Future<?> consume = pool.submit(() -> tx.executeWithoutResult(s -> {
                jdbc.queryForList("SELECT id FROM vouchers WHERE id = ? FOR UPDATE", id);
                locked.countDown();
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                jdbc.update("UPDATE vouchers SET used_count = used_count + 1, version = version + 1 WHERE id = ?", id);
            }));
            locked.await();
            Res upd = put_("/api/v1/admin/vouchers/" + id, fo, platformBody(c, now(), now().plusDays(9), null));
            consume.get();
            assertThat(upd.status()).isEqualTo(200);
            assertThat(jdbc.queryForMap("SELECT used_count, valid_to FROM vouchers WHERE id = ?", id).get("used_count"))
                    .isEqualTo(1);
            assertThat(get_("/api/v1/admin/vouchers/" + id, fo).data().get("valid_to").asText())
                    .startsWith(now().plusDays(9).toLocalDate().toString());
        } finally {
            pool.shutdownNow();
        }
    }
}
