// Thông báo KYC: chủ shop nhận kết quả, đội vận hành nhận hồ sơ mới (plan 2026-11-26 thông báo D11b–c, N03.07, N03.10)
package com.genie.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.support.TestDataBuilder;
import com.genie.vendor.kyc.event.KycDecidedEvent;
import com.genie.vendor.kyc.event.KycSubmittedEvent;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

class KycNotificationIT extends NotificationTestSupport {

    @Autowired
    ApplicationEventPublisher events;

    private void publish(Object event) {
        tx.executeWithoutResult(s -> events.publishEvent(event));
    }

    @Test
    void sellerGetsApprovedRejectedAndSupplementNotifications() {
        TestDataBuilder.Shop shop = data.shop().name("Shop KYC " + System.nanoTime()).create();
        publish(new KycDecidedEvent(shop.shopId(), shop.userId(), "VERIFIED", null, List.of(), null));
        publish(new KycDecidedEvent(shop.shopId(), shop.userId(), "REJECTED", "Ảnh mờ", List.of(), null));
        publish(new KycDecidedEvent(shop.shopId(), shop.userId(), "SUPPLEMENT_REQUESTED", "Bổ sung giấy phép",
                List.of("BUSINESS_LICENSE"), OffsetDateTime.parse("2026-12-03T10:00:00Z")));

        List<Map<String, Object>> rows = notificationsOf(shop.userId());
        assertThat(rows).extracting(r -> r.get("type"))
                .containsExactly("KYC_APPROVED", "KYC_REJECTED", "KYC_REJECTED");
        assertThat(rows.get(0).get("title").toString()).contains(shop.name()).contains("đã được duyệt");
        assertThat(rows.get(1).get("body")).isEqualTo("Ảnh mờ");
        assertThat(rows.get(2).get("body").toString()).contains("Bổ sung giấy phép").contains("17:00 03/12/2026");
        assertThat(rows.get(2).get("metadata_json").toString()).contains("SUPPLEMENT_REQUESTED").contains("BUSINESS_LICENSE");
        assertThat(rows).allSatisfy(r -> assertThat(r.get("reference_type")).isEqualTo("KYC"));
    }

    @Test
    void staffInboxGetsUserApprovalWhenAShopSubmitsKyc() {
        TestDataBuilder.Shop shop = data.shop().name("Shop nộp " + System.nanoTime()).kyc("PENDING").create();
        publish(new KycSubmittedEvent(shop.shopId(), shop.userId()));
        assertThat(adminRows("USER_APPROVAL", "Hồ sơ KYC mới: " + shop.name())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT reference_url FROM admin_notifications WHERE title = ?", String.class,
                "Hồ sơ KYC mới: " + shop.name())).isEqualTo("/admin/kyc");
    }
}
