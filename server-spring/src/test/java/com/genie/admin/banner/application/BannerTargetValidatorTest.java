package com.genie.admin.banner.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Liên kết banner chỉ trong miền sàn (plan banner B02.06). */
class BannerTargetValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"/c/dien-tu", "/p/tai-nghe-1", "/shop/15", "/flash-sale", "/discover",
        "/campaign/sieu-sale-12-12", "/#flash-sale", "/search?q=tai%20nghe", "/c/thoi-trang?sort=new"})
    void allowsInternalPaths(String target) {
        assertThat(BannerTargetValidator.isAllowed(target)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"//other.com", "/\\other.com", "javascript:alert(1)", "https://x.com", "/%2F%2Fevil.com",
        "/%5Cevil.com", "c/dien-tu", " /c/dien-tu", "/c/dien tu", "/a\nb", "/api/v1/admin/users",
        "/go?u=https://evil.com", "/%E0%A4%A"})
    void rejectsExternalOrUnsafeTargets(String target) {
        assertThat(BannerTargetValidator.isAllowed(target)).isFalse();
    }

    @org.junit.jupiter.api.Test
    void rejectsTooLong() {
        assertThat(BannerTargetValidator.isAllowed("/" + "a".repeat(500))).isFalse();
    }
}
