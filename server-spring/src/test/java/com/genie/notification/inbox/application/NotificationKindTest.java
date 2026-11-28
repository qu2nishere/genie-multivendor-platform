// Enum công khai của module notification phải khớp enum domain (plan 2026-11-26 thông báo D10)
package com.genie.notification.inbox.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.notification.admin.application.StaffAlertKind;
import com.genie.notification.admin.domain.AdminNotificationType;
import com.genie.notification.inbox.domain.NotificationType;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class NotificationKindTest {

    @Test
    void notificationKindMirrorsNotificationType() {
        assertThat(Arrays.stream(NotificationKind.values()).map(Enum::name))
                .containsExactlyElementsOf(Arrays.stream(NotificationType.values()).map(Enum::name).toList());
        for (NotificationKind k : NotificationKind.values()) {
            assertThat(k.toType().name()).isEqualTo(k.name());
        }
    }

    @Test
    void staffAlertKindMirrorsAdminNotificationType() {
        assertThat(Arrays.stream(StaffAlertKind.values()).map(Enum::name))
                .containsExactlyElementsOf(Arrays.stream(AdminNotificationType.values()).map(Enum::name).toList());
        for (StaffAlertKind k : StaffAlertKind.values()) {
            assertThat(k.toType().name()).isEqualTo(k.name());
        }
    }
}
