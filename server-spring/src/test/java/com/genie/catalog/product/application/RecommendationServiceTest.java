package com.genie.catalog.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

/** Trộn xen kẽ 3 : 2 của gợi ý cá nhân hóa (plan 2026-11-17 A9). */
class RecommendationServiceTest {

    @Test
    void interleavesThreePrimaryToTwoSecondary() {
        List<Long> out = RecommendationService.interleave(List.of(1L, 2L, 3L, 4L, 5L, 6L), List.of(10L, 11L, 12L, 13L));
        assertThat(out).startsWith(1L, 2L, 3L, 10L, 11L, 4L, 5L, 6L, 12L, 13L);
    }

    @Test
    void dropsDuplicatesAndFillsWhenOneSideRunsOut() {
        List<Long> out = RecommendationService.interleave(List.of(1L, 2L), List.of(2L, 9L, 8L, 7L));
        assertThat(out).containsExactly(1L, 2L, 9L, 8L, 7L);
    }

    @Test
    void capsAtMaxItems() {
        List<Long> big = LongStream.range(0, 500).boxed().toList();
        List<Long> other = LongStream.range(1000, 1500).boxed().toList();
        assertThat(RecommendationService.interleave(big, other)).hasSize(RecommendationService.MAX_ITEMS);
    }
}
