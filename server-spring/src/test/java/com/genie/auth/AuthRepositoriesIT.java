package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.auth.address.domain.Address;
import com.genie.auth.address.domain.AddressLabel;
import com.genie.auth.address.domain.AddressRepository;
import com.genie.auth.profile.domain.CustomerProfile;
import com.genie.auth.profile.domain.CustomerProfileRepository;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.auth.token.domain.RefreshToken;
import com.genie.auth.token.domain.RefreshTokenRepository;
import com.genie.auth.token.domain.RevokeReason;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Query repository của AUTH (plan AUTH A03.06–A03.07) trên PostgreSQL thật sau V14.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AuthRepositoriesIT {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-08T07:00:00Z");

    @Autowired UserRepository users;
    @Autowired CustomerProfileRepository profiles;
    @Autowired AddressRepository addresses;
    @Autowired RefreshTokenRepository tokens;
    @Autowired EntityManager em;

    private User newUser(String email) {
        return users.saveAndFlush(User.newCustomer(email, "hash", NOW));
    }

    @Test
    void userAndProfileAreSavedTogether() {
        User u = newUser("Mai@Example.com");
        profiles.saveAndFlush(CustomerProfile.create(u, "Mai", "0901234567", "vi"));
        em.clear();

        assertThat(users.findByEmail("mai@example.com")).isPresent();
        assertThat(users.existsByEmail("mai@example.com")).isTrue();
        assertThat(profiles.findByUserId(u.getId())).get().extracting(CustomerProfile::getFullName).isEqualTo("Mai");
    }

    @Test
    void refreshTokenFamiliesRevokeIndependently() {
        User u = newUser("tokens@example.com");
        long familyA = tokens.nextFamilyId();
        long familyB = tokens.nextFamilyId();
        assertThat(familyB).isGreaterThan(familyA);

        tokens.saveAndFlush(RefreshToken.issue(u.getId(), familyA, "hash-a", "Chrome", "1.1.1.1", NOW.plusDays(7)));
        tokens.saveAndFlush(RefreshToken.issue(u.getId(), familyB, "hash-b", "Safari", "1.1.1.2", NOW.plusDays(7)));

        assertThat(tokens.revokeOtherFamilies(u.getId(), familyA, RevokeReason.PASSWORD_CHANGED, NOW)).isEqualTo(1);
        assertThat(tokens.findActiveTokens(u.getId(), NOW)).extracting(RefreshToken::getFamilyId).containsExactly(familyA);
        assertThat(tokens.findByTokenHash("hash-b")).get().extracting(RefreshToken::getRevokedReason)
                .isEqualTo(RevokeReason.PASSWORD_CHANGED);

        assertThat(tokens.revokeAllOfUser(u.getId(), RevokeReason.USER_LOGOUT, NOW)).isEqualTo(1);
        assertThat(tokens.findActiveTokens(u.getId(), NOW)).isEmpty();
    }

    @Test
    void addressesListDefaultFirstAndHideDeleted() {
        User u = newUser("addr@example.com");
        Address a = addresses.saveAndFlush(address(u.getId(), "Nhà", false));
        Address b = addresses.saveAndFlush(address(u.getId(), "Công ty", true));
        Address c = addresses.saveAndFlush(address(u.getId(), "Cũ", false));
        c.softDelete();
        addresses.saveAndFlush(c);

        assertThat(addresses.findActiveByUserId(u.getId())).extracting(Address::getId).containsExactly(b.getId(), a.getId());
        assertThat(addresses.countActiveByUserId(u.getId())).isEqualTo(2);

        assertThat(addresses.clearDefault(u.getId())).isEqualTo(1);
        assertThat(addresses.findActiveByIdAndUserId(c.getId(), u.getId())).isEmpty();
    }

    @Test
    void dueForAnonymizationOnlyReturnsOldRequests() {
        User old = newUser("old@example.com");
        old.requestDeletion(NOW.minusDays(31));
        User recent = newUser("recent@example.com");
        recent.requestDeletion(NOW.minusDays(5));
        users.saveAllAndFlush(java.util.List.of(old, recent));

        assertThat(users.findDueForAnonymization(NOW.minusDays(30), 10)).extracting(User::getEmail)
                .containsExactly("old@example.com");
    }

    private static Address address(Long userId, String recipient, boolean isDefault) {
        return Address.builder()
                .userId(userId).recipientName(recipient).phoneNumber("0901234567").addressLine("25 Nguyễn Huệ")
                .ward("P. Bến Nghé").wardCode("20308").district("Quận 1").districtId(1442)
                .province("TP. Hồ Chí Minh").provinceId(202).isDefault(isDefault).label(AddressLabel.HOME)
                .build();
    }
}
