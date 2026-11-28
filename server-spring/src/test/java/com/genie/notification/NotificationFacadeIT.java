// Ghi thông báo sau commit, không làm hỏng nghiệp vụ gốc, gộp thông báo chat (plan 2026-11-26 thông báo N3, N03.10)
package com.genie.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.notification.admin.application.AdminInboxFacade;
import com.genie.notification.admin.application.StaffAlertKind;
import com.genie.notification.inbox.application.NotificationCommand;
import com.genie.notification.inbox.application.NotificationFacade;
import com.genie.notification.inbox.application.NotificationKind;
import com.genie.notification.inbox.application.NotificationLink;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotificationFacadeIT extends NotificationTestSupport {

    @Autowired
    NotificationFacade facade;

    @Autowired
    AdminInboxFacade adminInbox;

    private static NotificationCommand cmd(long user, String title) {
        return new NotificationCommand(user, NotificationKind.ORDER_STATUS_CHANGED, title, "Thân", null,
                NotificationLink.of(NotificationLink.ORDER, 44L), Map.of("order_code", "TR-1"));
    }

    @Test
    void writesOnlyAfterTheBusinessTransactionCommits() {
        long u = customer();
        tx.executeWithoutResult(s -> {
            facade.send(cmd(u, "Đơn đang giao"));
            // chưa commit → chưa có dòng nào
            assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE user_id = ?", Integer.class, u))
                    .isZero();
        });
        List<Map<String, Object>> rows = notificationsOf(u);
        assertThat(rows).hasSize(1);
        Map<String, Object> row = rows.get(0);
        assertThat(row.get("type")).isEqualTo("ORDER_STATUS_CHANGED");
        assertThat(row.get("channel")).isEqualTo("IN_APP");
        assertThat(row.get("delivery_status")).isEqualTo("SENT");
        assertThat(row.get("is_read")).isEqualTo(false);
        assertThat(row.get("reference_type")).isEqualTo("ORDER");
        assertThat(row.get("reference_id")).isEqualTo("44");
        assertThat(row.get("metadata_json").toString()).contains("TR-1");
    }

    @Test
    void rollbackLeavesNoNotification() {
        long u = customer();
        tx.executeWithoutResult(s -> {
            facade.send(cmd(u, "Sẽ rollback"));
            s.setRollbackOnly();
        });
        assertThat(notificationsOf(u)).isEmpty();
    }

    @Test
    void withoutTransactionWritesImmediately() {
        long u = customer();
        facade.send(cmd(u, "Ngay"));
        facade.sendNow(cmd(u, "Ngay 2"));
        facade.sendManyNow(List.of(cmd(u, "A"), cmd(u, "B")));
        assertThat(notificationsOf(u)).hasSize(4);
    }

    @Test
    void notificationFailureNeverBreaksTheBusinessTransaction() {
        long u = customer();
        long missingUser = 9_000_000_000L;   // vi phạm FK users → ghi thông báo lỗi
        tx.executeWithoutResult(s -> {
            jdbc.update("UPDATE users SET email_verified_at = TIMESTAMPTZ '2001-01-02 03:04:05+00' WHERE id = ?", u);
            facade.send(cmd(missingUser, "Không ghi được"));
        });
        assertThat(jdbc.queryForObject("SELECT extract(year FROM email_verified_at)::int FROM users WHERE id = ?",
                Integer.class, u)).isEqualTo(2001);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE user_id = ?", Integer.class, missingUser))
                .isZero();
    }

    @Test
    void chatNotificationsAreMergedPerConversationUntilRead() {
        long u = customer();
        facade.upsertChat(u, 3301L, "Tin nhắn mới từ TechZone", "Dạ còn hàng");
        facade.upsertChat(u, 3301L, "Tin nhắn mới từ TechZone", "Bạn cần màu nào?");
        facade.upsertChat(u, 3302L, "Tin nhắn mới từ Shoe", "Chào bạn");
        List<Map<String, Object>> rows = notificationsOf(u);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("body")).isEqualTo("Bạn cần màu nào?");
        assertThat(rows.get(0).get("type")).isEqualTo("CHAT_MESSAGE");
        assertThat(rows.get(0).get("reference_type")).isEqualTo("CONVERSATION");

        jdbc.update("UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND reference_id = '3301'", u);
        facade.upsertChat(u, 3301L, "Tin nhắn mới từ TechZone", "Shop gửi tiếp");
        assertThat(notificationsOf(u)).hasSize(3);
    }

    @Test
    void staffInboxRowIsWrittenEvenInsideARolledBackTransactionWhenRaisedNow() {
        String title = "Cảnh báo " + System.nanoTime();
        tx.executeWithoutResult(s -> {
            adminInbox.raiseNow(StaffAlertKind.SYSTEM_ALERT, title, "Chi tiết", "/admin/users");
            s.setRollbackOnly();
        });
        assertThat(adminRows("SYSTEM_ALERT", title)).isEqualTo(1);

        String later = "Sau commit " + System.nanoTime();
        tx.executeWithoutResult(s -> {
            adminInbox.raise(StaffAlertKind.WITHDRAW_REQUEST, later, "Rút tiền", "/admin/withdrawals");
            s.setRollbackOnly();
        });
        assertThat(adminRows("WITHDRAW_REQUEST", later)).isZero();
    }
}
