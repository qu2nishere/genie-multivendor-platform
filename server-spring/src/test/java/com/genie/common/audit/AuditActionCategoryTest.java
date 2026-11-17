package com.genie.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
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
    }
}
