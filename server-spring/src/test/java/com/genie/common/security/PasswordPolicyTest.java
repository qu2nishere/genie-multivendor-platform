package com.genie.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"Genie@123", "Mật@Khẩu2026", "Abcdef1!"})
    void acceptsStrongPasswords(String password) {
        assertThat(PasswordPolicy.isStrong(password)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"genie@123", "GENIE@123", "Genie@abc", "Genie1234", "Ge@1", ""})
    void rejectsWeakPasswords(String password) {
        assertThat(PasswordPolicy.isStrong(password)).isFalse();
    }

    @Test
    void rejectsNullAndTooLong() {
        assertThat(PasswordPolicy.isStrong(null)).isFalse();
        assertThat(PasswordPolicy.isStrong("Aa1!" + "x".repeat(200))).isFalse();
    }

    @Test
    void validatorTreatsNullAsValid() {
        assertThat(new StrongPassword.Validator().isValid(null, null)).isTrue();
        assertThat(new StrongPassword.Validator().isValid("weak", null)).isFalse();
    }
}
