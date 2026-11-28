// Nhóm hiển thị của hộp thư (plan 2026-11-26 thông báo D6, N03.01)
package com.genie.notification.inbox.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NotificationCategoryTest {

    @Test
    void everyTypeBelongsToExactlyOneCategory() {
        Set<NotificationType> union = EnumSet.noneOf(NotificationType.class);
        int total = 0;
        for (NotificationCategory c : NotificationCategory.values()) {
            assertThat(c.types()).as(c.name()).isNotEmpty();
            total += c.types().size();
            union.addAll(c.types());
        }
        assertThat(union).containsExactlyInAnyOrder(NotificationType.values());
        assertThat(total).isEqualTo(NotificationType.values().length);
    }

    @Test
    void mapsLikeApiSpec() {
        assertThat(NotificationCategory.of(NotificationType.ORDER_STATUS_CHANGED)).isEqualTo(NotificationCategory.ORDER);
        assertThat(NotificationCategory.of(NotificationType.NEW_ORDER_FOR_VENDOR)).isEqualTo(NotificationCategory.ORDER);
        assertThat(NotificationCategory.of(NotificationType.WALLET_CREDITED)).isEqualTo(NotificationCategory.ORDER);
        assertThat(NotificationCategory.of(NotificationType.NEW_PLATFORM_VOUCHER)).isEqualTo(NotificationCategory.PROMOTION);
        assertThat(NotificationCategory.of(NotificationType.NEW_DEVICE_LOGIN)).isEqualTo(NotificationCategory.SYSTEM);
        assertThat(NotificationCategory.of(NotificationType.KYC_REJECTED)).isEqualTo(NotificationCategory.SYSTEM);
        assertThat(NotificationCategory.of(NotificationType.CHAT_MESSAGE)).isEqualTo(NotificationCategory.CHAT);
        assertThat(NotificationCategory.includesSystemSources(null)).isTrue();
        assertThat(NotificationCategory.includesSystemSources(NotificationCategory.SYSTEM)).isTrue();
        assertThat(NotificationCategory.includesSystemSources(NotificationCategory.ORDER)).isFalse();
    }
}
