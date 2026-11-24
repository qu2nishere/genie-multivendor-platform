package com.genie.auth.profile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.genie.common.domain.UserRole;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-08T07:00:00Z");

    private static User customer() {
        return User.newCustomer("  Mai.Tran@Gmail.com ", "hash", NOW);
    }

    @Test
    void newCustomerIsActiveVerifiedAndNormalized() {
        User u = customer();
        assertThat(u.getEmail()).isEqualTo("mai.tran@gmail.com");
        assertThat(u.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(u.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(u.getEmailVerifiedAt()).isEqualTo(NOW);
        assertThat(u.hasPassword()).isTrue();
    }

    @Test
    void fiveFailedLoginsLockForFifteenMinutes() {
        User u = customer();
        for (int i = 0; i < 4; i++) {
            u.recordFailedLogin(NOW);
        }
        assertThat(u.isLocked(NOW)).isFalse();
        assertThat(u.getFailedLoginAttempts()).isEqualTo(4);

        u.recordFailedLogin(NOW);

        assertThat(u.isLocked(NOW)).isTrue();
        assertThat(u.isLocked(NOW.plusMinutes(15))).isFalse();
        assertThat(u.getFailedLoginAttempts()).isZero();
    }

    @Test
    void lockForAndClearLock() {
        User u = customer();
        u.lockFor(Duration.ofMinutes(15), NOW);
        assertThat(u.isLocked(NOW.plusMinutes(10))).isTrue();
        u.clearLock();
        assertThat(u.isLocked(NOW)).isFalse();
    }

    @Test
    void suspensionExpiresAutomatically() {
        User u = customer();
        u.suspend("Spam", NOW.plusDays(3));
        assertThat(u.isActive()).isFalse();
        assertThat(u.reactivateIfSuspensionExpired(NOW.plusDays(1))).isFalse();
        assertThat(u.reactivateIfSuspensionExpired(NOW.plusDays(3))).isTrue();
        assertThat(u.isActive()).isTrue();
    }

    /** Biên của cron hết hạn đình chỉ (plan 2026-11-24 H03.04): đúng bằng hạn là mở, xóa lý do / hạn. */
    @Test
    void suspensionEndsExactlyAtItsDeadlineAndClearsReason() {
        User u = customer();
        u.suspend("ABNORMAL_RETURNS", NOW);
        assertThat(u.reactivateIfSuspensionExpired(NOW.minusNanos(1))).isFalse();
        assertThat(u.reactivateIfSuspensionExpired(NOW)).isTrue();
        assertThat(u.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(u.getSuspensionReason()).isNull();
        assertThat(u.getSuspendedUntil()).isNull();
        assertThat(u.reactivateIfSuspensionExpired(NOW.plusDays(1))).isFalse();   // đã mở thì không mở lần nữa
    }

    @Test
    void indefiniteSuspensionAndLockedAccountsNeverReopenByThemselves() {
        User forever = customer();
        forever.suspend("COUNTERFEIT_SELLER", null);
        assertThat(forever.reactivateIfSuspensionExpired(NOW.plusYears(10))).isFalse();
        assertThat(forever.getStatus()).isEqualTo(AccountStatus.SUSPENDED);

        User locked = customer();
        locked.setStatus(AccountStatus.LOCKED);
        locked.setSuspendedUntil(NOW.minusDays(1));
        assertThat(locked.reactivateIfSuspensionExpired(NOW)).isFalse();
        assertThat(locked.getStatus()).isEqualTo(AccountStatus.LOCKED);
    }

    @Test
    void cannotUnlinkOnlyLoginMethod() {
        User googleOnly = User.newCustomer("g@x.com", null, NOW);
        googleOnly.linkIdentity("google", "sub-1");

        assertThatThrownBy(googleOnly::unlinkIdentity).isInstanceOf(IllegalStateException.class);

        googleOnly.changePassword("hash");
        googleOnly.unlinkIdentity();
        assertThat(googleOnly.getOauthProvider()).isNull();
    }

    @Test
    void cannotLinkTwice() {
        User u = customer();
        u.linkIdentity("google", "sub-1");
        assertThatThrownBy(() -> u.linkIdentity("apple", "sub-2")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deletionRequestLifecycle() {
        User u = customer();
        u.requestDeletion(NOW);
        assertThat(u.isPendingDeletion()).isTrue();
        assertThatThrownBy(() -> u.requestDeletion(NOW)).isInstanceOf(IllegalStateException.class);
        u.cancelDeletion();
        assertThat(u.isPendingDeletion()).isFalse();
        assertThatThrownBy(u::cancelDeletion).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anonymizeRemovesCredentialsAndIdentity() {
        User u = customer();
        u.setId(42L);
        u.linkIdentity("google", "sub-1");

        u.anonymize();

        assertThat(u.getStatus()).isEqualTo(AccountStatus.ANONYMIZED);
        assertThat(u.getEmail()).isEqualTo("deleted_user_42@anonymized.local");
        assertThat(u.getPassword()).isNull();
        assertThat(u.getOauthProvider()).isNull();
        assertThat(u.getOauthProviderId()).isNull();
    }

    @Test
    void changeEmailNormalizesAndVerifies() {
        User u = customer();
        u.changeEmail(" New@Mail.com", NOW.plusDays(1));
        assertThat(u.getEmail()).isEqualTo("new@mail.com");
        assertThat(u.getEmailVerifiedAt()).isEqualTo(NOW.plusDays(1));
    }
}
