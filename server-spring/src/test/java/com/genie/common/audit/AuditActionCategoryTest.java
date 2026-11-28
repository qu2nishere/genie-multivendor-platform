package com.genie.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Mỗi hành động thuộc đúng một nhóm; 6 nhóm hợp lại đủ mọi giá trị (plan nhật ký kiểm toán L01.04). */
class AuditActionCategoryTest {

    @Test
    void everyActionBelongsToExactlyOneCategory() {
        Set<AuditAction> union = EnumSet.noneOf(AuditAction.class);
        int total = 0;
        for (AuditCategory c : AuditCategory.values()) {
            assertThat(c.actions()).as(c.name()).isNotEmpty();
            total += c.actions().size();
            union.addAll(c.actions());
        }
        assertThat(union).containsExactlyInAnyOrder(AuditAction.values());
        assertThat(total).isEqualTo(AuditAction.values().length);
    }

    @Test
    void mapsSampleActionsLikeApiSpec() {
        assertThat(AuditAction.LOGIN_FAILED.category()).isEqualTo(AuditCategory.AUTH);
        assertThat(AuditAction.KYC_APPROVED.category()).isEqualTo(AuditCategory.KYC);
        assertThat(AuditAction.PAYOUT_APPROVED.category()).isEqualTo(AuditCategory.FINANCE);
        assertThat(AuditAction.PRODUCT_MODERATED.category()).isEqualTo(AuditCategory.MODERATION);
        assertThat(AuditAction.ACCESS_DENIED.category()).isEqualTo(AuditCategory.SECURITY);
        assertThat(AuditAction.PERMISSION_GRANTED.category()).isEqualTo(AuditCategory.CONFIG);
        assertThat(AuditAction.KYC_DOCUMENT_VIEWED.category()).isEqualTo(AuditCategory.KYC);
        assertThat(AuditAction.KYC_SUPPLEMENT_REQUESTED.category()).isEqualTo(AuditCategory.KYC);
        assertThat(AuditAction.KYC_REQUEUED.category()).isEqualTo(AuditCategory.KYC);
        assertThat(AuditAction.PAYOUT_ACCOUNT_CHANGED.category()).isEqualTo(AuditCategory.FINANCE);
        for (AuditAction banner : List.of(AuditAction.BANNER_CHANGED, AuditAction.BANNER_CREATED,
                AuditAction.BANNER_UPDATED, AuditAction.BANNER_ACTIVATED, AuditAction.BANNER_DEACTIVATED,
                AuditAction.BANNER_DELETED)) {
            assertThat(banner.category()).as(banner.name()).isEqualTo(AuditCategory.CONFIG);
        }
        // Thông báo sàn (UC-NOT-05) + voucher sàn (UC-PRO-02) — V27, plan 2026-11-26
        for (AuditAction config : List.of(AuditAction.PLATFORM_NOTIFICATION_PUBLISHED,
                AuditAction.PLATFORM_NOTIFICATION_UPDATED, AuditAction.PLATFORM_NOTIFICATION_DELETED,
                AuditAction.VOUCHER_CREATED, AuditAction.VOUCHER_UPDATED, AuditAction.VOUCHER_DISABLED,
                AuditAction.VOUCHER_DELETED)) {
            assertThat(config.category()).as(config.name()).isEqualTo(AuditCategory.CONFIG);
        }
    }
}
