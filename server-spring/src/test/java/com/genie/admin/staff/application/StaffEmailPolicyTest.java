package com.genie.admin.staff.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Luật email nhân viên (plan B1 D3, chốt 2026-11-20): mặc định mọi email; có STAFF_EMAIL_DOMAINS thì giới hạn tên miền. */
class StaffEmailPolicyTest {

    @Test
    void anyValidEmailWhenNoDomainConfigured() {
        Set<String> none = StaffService.parseDomains("");
        assertThat(none).isEmpty();
        assertThat(StaffService.isAllowedEmail("nguyenquockhoa@gmail.com", none)).isTrue();
        assertThat(StaffService.isAllowedEmail("truc.ht@genie.example", none)).isTrue();
        assertThat(StaffService.isAllowedEmail("khong-phai-email", none)).isFalse();
        assertThat(StaffService.isAllowedEmail("a@b", none)).isFalse();
        assertThat(StaffService.isAllowedEmail("a b@c.vn", none)).isFalse();
    }

    @Test
    void configuredDomainsRestrictInvitations() {
        Set<String> domains = StaffService.parseDomains(" Genie.vn, @genie.com.vn ,");
        assertThat(domains).containsExactlyInAnyOrder("genie.vn", "genie.com.vn");
        assertThat(StaffService.isAllowedEmail("an@genie.vn", domains)).isTrue();
        assertThat(StaffService.isAllowedEmail("an@genie.com.vn", domains)).isTrue();
        assertThat(StaffService.isAllowedEmail("an@gmail.com", domains)).isFalse();
        assertThat(StaffService.isAllowedEmail("an@sub.genie.vn", domains)).isFalse();
    }
}
