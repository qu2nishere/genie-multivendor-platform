// Unit test nhãn AiCheck — UC-CAT-12/13 (update_gemini.md mục 2.1)
package com.genie.catalog.moderation.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiCheckTest {

    @Test
    void nullDecisionReturnsNotScreened() {
        assertThat(AiCheck.of(null, null, null, 85)).isEqualTo(AiCheck.NOT_SCREENED);
    }

    @Test
    void fallbackTakesPrecedenceOverNeedReview() {
        assertThat(AiCheck.of("NEED_REVIEW", 50, true, 85)).isEqualTo(AiCheck.FAILED);
    }

    @Test
    void approveReturnsPassed() {
        assertThat(AiCheck.of("APPROVE", 10, false, 85)).isEqualTo(AiCheck.PASSED);
    }

    @Test
    void needReviewReturnsSuspected() {
        assertThat(AiCheck.of("NEED_REVIEW", 60, false, 85)).isEqualTo(AiCheck.SUSPECTED);
    }

    @Test
    void rejectBelowThresholdReturnsSuspected() {
        assertThat(AiCheck.of("REJECT", 84, false, 85)).isEqualTo(AiCheck.SUSPECTED);
    }

    @Test
    void rejectAtOrAboveThresholdReturnsRejected() {
        assertThat(AiCheck.of("REJECT", 85, false, 85)).isEqualTo(AiCheck.REJECTED);
    }

    @Test
    void rejectWithNullRiskScoreReturnsSuspected() {
        assertThat(AiCheck.of("REJECT", null, false, 85)).isEqualTo(AiCheck.SUSPECTED);
    }
}
