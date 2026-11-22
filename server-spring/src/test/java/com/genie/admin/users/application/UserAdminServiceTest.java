package com.genie.admin.users.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.auth.profile.application.MemberDirectory.Member;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** Lưu / tách lý do khóa (plan B1 D11) và ánh xạ trạng thái API 11.13. */
class UserAdminServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-08T07:00:00Z");

    private static Member member(String status, OffsetDateTime lockedUntil, OffsetDateTime suspendedUntil) {
        return new Member(1, "a@genie.test", "CUSTOMER", status, lockedUntil, suspendedUntil, null, "A", null);
    }

    @Test
    void reasonRoundTrip() {
        assertThat(UserAdminService.formatReason("FRAUD", null)).isEqualTo("FRAUD");
        assertThat(UserAdminService.formatReason("OTHER", "Lừa đảo")).isEqualTo("OTHER: Lừa đảo");
        assertThat(UserAdminService.parseReason("FRAUD")).containsExactly("FRAUD", null);
        assertThat(UserAdminService.parseReason("OTHER: Lừa đảo: qua chat")).containsExactly("OTHER", "Lừa đảo: qua chat");
        // lý do chữ tự do từ trước (vd khóa nhân viên) → OTHER + nguyên văn
        assertThat(UserAdminService.parseReason("Vi phạm chính sách")).containsExactly("OTHER", "Vi phạm chính sách");
        assertThat(UserAdminService.parseReason(null)).containsExactly("OTHER", null);
    }

    @Test
    void statusMapping() {
        assertThat(UserAdminService.statusOf(member("ACTIVE", null, null), NOW)).isEqualTo("ACTIVE");
        assertThat(UserAdminService.statusOf(member("SUSPENDED", null, NOW.plusDays(7)), NOW)).isEqualTo("SUSPENDED");
        assertThat(UserAdminService.statusOf(member("SUSPENDED", null, null), NOW)).isEqualTo("LOCKED");
        assertThat(UserAdminService.statusOf(member("LOCKED", null, null), NOW)).isEqualTo("LOCKED");
        assertThat(UserAdminService.statusOf(member("ACTIVE", NOW.plusMinutes(5), null), NOW)).isEqualTo("LOCKED");
        assertThat(UserAdminService.statusOf(member("ACTIVE", NOW.minusMinutes(5), null), NOW)).isEqualTo("ACTIVE");
    }
}
