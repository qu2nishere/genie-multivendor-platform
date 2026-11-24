package com.genie.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.genie.auth.profile.application.SuspensionExpiryJob;
import com.genie.auth.profile.application.UserFacade;
import com.genie.common.mail.MailTemplate;
import com.genie.common.security.ClientInfo;
import com.genie.support.FakeEmailSender;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cron hết hạn đình chỉ — UC-ADM-03 luồng thay thế, LLD 10, plan 2026-11-24 H03.05 (D1–D4).
 * CSDL dùng chung giữa các test → mọi khẳng định theo id của tài khoản do test tạo, không theo tổng số toàn bảng.
 */
class SuspensionExpiryJobIT extends AuditLogTestSupport {

    @Autowired
    SuspensionExpiryJob job;

    @Autowired
    UserFacade userFacade;

    @Autowired
    PlatformTransactionManager transactionManager;

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    /** Khách mới bị đình chỉ tới {@code until} (NULL = vô thời hạn) — dựng thẳng bằng SQL như UserFacade.suspend lưu. */
    private long suspendedCustomer(String email, OffsetDateTime until) {
        long id = createCustomer(email, PASSWORD).getId();
        jdbc.update("UPDATE users SET status = 'SUSPENDED', suspension_reason = 'ABNORMAL_RETURNS', suspended_until = ? "
                + "WHERE id = ?", until, id);
        return id;
    }

    private String statusOf(long id) {
        return jdbc.queryForObject("SELECT status FROM users WHERE id = ?", String.class, id);
    }

    /** Dòng {@code ACCOUNT_REACTIVATED} của tài khoản: cron / Admin ghi {@code reference_id}, đăng nhập ghi {@code user_id}. */
    private int reactivatedLogs(long id) {
        return jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'ACCOUNT_REACTIVATED' "
                + "AND (reference_id = ? OR user_id = ?)", Integer.class, String.valueOf(id), id);
    }

    /** Email gửi nền sau commit ({@code @Async}) — chờ tối đa 5 giây. */
    private FakeEmailSender.SentMail waitForMail(String to) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            var mail = mails.last(to, MailTemplate.ACCOUNT_REACTIVATED);
            if (mail.isPresent()) {
                return mail.get();
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Chưa có email mở lại tài khoản gửi tới " + to);
    }

    private long mailsTo(String to) throws InterruptedException {
        Thread.sleep(500);   // cho listener @Async kịp chạy nếu (sai) có gửi
        return mails.sent().stream().filter(m -> m.to().equals(to) && m.template() == MailTemplate.ACCOUNT_REACTIVATED)
                .count();
    }

    @Test
    void expiredSuspensionIsLiftedLoggedAndEmailed() throws Exception {
        String email = uniqueEmail();
        OffsetDateTime until = now().minusMinutes(1);
        long id = suspendedCustomer(email, until);

        assertThat(job.run()).isPositive();

        assertThat(statusOf(id)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT suspension_reason IS NULL AND suspended_until IS NULL FROM users WHERE id = ?",
                Boolean.class, id)).isTrue();
        assertThat(reactivatedLogs(id)).isEqualTo(1);
        var log = jdbc.queryForMap("SELECT user_id, details::jsonb ->> 'reason' AS reason, details::jsonb ->> 'source' AS source, "
                + "details::jsonb ->> 'suspended_until' AS until FROM system_logs "
                + "WHERE action_type = 'ACCOUNT_REACTIVATED' AND reference_id = ?", String.valueOf(id));
        assertThat(log.get("user_id")).isNull();                         // người làm = hệ thống
        assertThat(log.get("reason")).isEqualTo("SUSPENSION_EXPIRED");
        assertThat(log.get("source")).isEqualTo("CRON");
        assertThat(log.get("until")).isNotNull();

        var mail = waitForMail(email);
        assertThat(mail.variables().get("reason")).isEqualTo("SUSPENSION_EXPIRED");
        assertThat(mail.variables().get("reactivatedAt")).isNotNull();

        // Đăng nhập lại được ngay (phiên cũ đã bị thu hồi lúc khóa — chỉ cần đăng nhập mới)
        assertThat(login(email, PASSWORD).getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    void stillRunningSuspensionIndefiniteLockedAndDeletedAccountsAreUntouched() throws Exception {
        String running = uniqueEmail();
        long runningId = suspendedCustomer(running, now().plusMinutes(1));
        long foreverId = suspendedCustomer(uniqueEmail(), null);
        long lockedId = createCustomer(uniqueEmail(), PASSWORD).getId();
        jdbc.update("UPDATE users SET status = 'LOCKED', suspended_until = ? WHERE id = ?", now().minusDays(1), lockedId);
        long deletedId = createCustomer(uniqueEmail(), PASSWORD).getId();
        jdbc.update("UPDATE users SET status = 'DELETED', suspended_until = ? WHERE id = ?", now().minusDays(1), deletedId);

        job.run();

        assertThat(statusOf(runningId)).isEqualTo("SUSPENDED");
        assertThat(statusOf(foreverId)).isEqualTo("SUSPENDED");
        assertThat(statusOf(lockedId)).isEqualTo("LOCKED");
        assertThat(statusOf(deletedId)).isEqualTo("DELETED");
        for (long id : List.of(runningId, foreverId, lockedId, deletedId)) {
            assertThat(reactivatedLogs(id)).isZero();
        }
        // Còn hạn → đăng nhập vẫn bị chặn 403 GNE-AUTH-4031
        MvcResult blocked = login(running, PASSWORD);
        assertThat(blocked.getResponse().getStatus()).isEqualTo(403);
        assertThat(json(blocked, "$.code")).isEqualTo("GNE-AUTH-4031");

        // Qua hạn (tua đồng hồ 2 phút) → lượt sau mới mở
        clock.advance(Duration.ofMinutes(2));
        job.run();
        assertThat(statusOf(runningId)).isEqualTo("ACTIVE");
        assertThat(statusOf(foreverId)).isEqualTo("SUSPENDED");
    }

    @Test
    void manyExpiredAccountsAreOpenedAcrossBatchesOnce() throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            ids.add(suspendedCustomer(uniqueEmail(), now().minusMinutes(5)));
        }

        assertThat(job.run()).isGreaterThanOrEqualTo(150);   // 2 lô (100 + 50)
        assertThat(job.run()).isZero();                       // chạy lại không mở / ghi thêm

        for (long id : ids) {
            assertThat(statusOf(id)).isEqualTo("ACTIVE");
            assertThat(reactivatedLogs(id)).isEqualTo(1);
        }
    }

    @Test
    void twoRunsAtTheSameTimeOpenEachAccountOnce() throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            ids.add(suspendedCustomer(uniqueEmail(), now().minusMinutes(5)));
        }

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> task = job::run;
            List<Future<Integer>> runs = pool.invokeAll(List.of(task, task));
            int total = runs.get(0).get() + runs.get(1).get();
            assertThat(total).isGreaterThanOrEqualTo(40);
        } finally {
            pool.shutdownNow();
        }

        for (long id : ids) {
            assertThat(statusOf(id)).isEqualTo("ACTIVE");
            assertThat(reactivatedLogs(id)).as("tài khoản %d chỉ được mở một lần", id).isEqualTo(1);
        }
    }

    /** D4: đăng nhập khi đã hết hạn tự mở trước — cron sau đó không ghi thêm. */
    @Test
    void loginBeforeTheJobReopensTheAccountAndTheJobSkipsIt() throws Exception {
        String email = uniqueEmail();
        long id = suspendedCustomer(email, now().minusMinutes(1));

        assertThat(login(email, PASSWORD).getResponse().getStatus()).isEqualTo(201);
        assertThat(statusOf(id)).isEqualTo("ACTIVE");
        assertThat(reactivatedLogs(id)).isEqualTo(1);

        job.run();

        assertThat(reactivatedLogs(id)).isEqualTo(1);
        var log = jdbc.queryForMap("SELECT details::jsonb ->> 'reason' AS reason, details::jsonb ->> 'source' AS source "
                + "FROM system_logs WHERE action_type = 'ACCOUNT_REACTIVATED' AND user_id = ?", id);
        assertThat(log.get("reason")).isEqualTo("SUSPENSION_EXPIRED");   // cùng mã lý do với cron (D1)
        assertThat(log.get("source")).isNull();                          // không phải do cron
    }

    /** D2: Admin mở khóa thủ công cũng gửi email; lý do chữ của Admin không nằm trong thư. */
    @Test
    void manualReactivationByAdminSendsTheSameEmail() throws Exception {
        String email = uniqueEmail();
        long id = suspendedCustomer(email, now().plusDays(30));

        MvcResult r = mvc.perform(post("/api/v1/admin/users/" + id + "/reactivate")
                .header(HttpHeaders.AUTHORIZATION, adminToken())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Đã xác minh khiếu nại\"}")).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);

        var mail = waitForMail(email);
        assertThat(mail.variables().get("reason")).isEqualTo("ADMIN");
        assertThat(mail.variables().values()).doesNotContain("Đã xác minh khiếu nại");

        job.run();
        assertThat(reactivatedLogs(id)).isEqualTo(1);   // cron không đụng tài khoản đã mở
    }

    /** Email chỉ gửi SAU commit: transaction mở khóa bị rollback → không có thư, tài khoản vẫn khóa. */
    @Test
    void noEmailWhenTheReactivationIsRolledBack() throws Exception {
        String email = uniqueEmail();
        long id = suspendedCustomer(email, now().plusDays(7));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            userFacade.reactivate(id, null, ClientInfo.system(), "ADMIN");
            status.setRollbackOnly();
        });

        assertThat(statusOf(id)).isEqualTo("SUSPENDED");
        assertThat(mailsTo(email)).isZero();
    }

    /** Khóa tạm vì sai mật khẩu (không phải đình chỉ) được Admin gỡ → không gửi thư mở lại tài khoản. */
    @Test
    void clearingAPasswordLockDoesNotSendTheReactivationEmail() throws Exception {
        String email = uniqueEmail();
        long id = createCustomer(email, PASSWORD).getId();
        jdbc.update("UPDATE users SET locked_until = ? WHERE id = ?", now().plusMinutes(15), id);

        userFacade.reactivate(id, null, ClientInfo.system(), "ADMIN");

        assertThat(statusOf(id)).isEqualTo("ACTIVE");
        assertThat(mailsTo(email)).isZero();
    }
}
