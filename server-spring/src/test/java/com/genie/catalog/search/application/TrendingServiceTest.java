package com.genie.catalog.search.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Xếp hạng từ khóa xu hướng (plan 2026-11-17 A6) — hồi quy 2026-11-19: "áo thun" và "ao thun" hiện 2 dòng. */
class TrendingServiceTest {

    private static Map<String, Double> scores(Object... kv) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put((String) kv[i], ((Number) kv[i + 1]).doubleValue());
        }
        return m;
    }

    @Test
    void mergesAccentVariantsAndShowsAccentedForm() {
        var out = TrendingService.rank(scores("ao thun", 3, "áo thun", 2, "tai nghe", 4), Set.of(), 5);
        assertThat(out).containsExactly("áo thun", "tai nghe");   // 3 + 2 = 5 > 4
    }

    @Test
    void picksMostSearchedAccentedVariant() {
        var out = TrendingService.rank(scores("đồng hồ", 1, "dong ho", 5, "đông hồ", 3), Set.of(), 5);
        assertThat(out).containsExactly("đông hồ");
    }

    @Test
    void blockedVariantBlocksTheWholeGroup() {
        var out = TrendingService.rank(scores("áo thun", 9, "ao thun", 1, "serum", 2), Set.of("ao thun"), 5);
        assertThat(out).containsExactly("serum");
    }

    @Test
    void respectsLimitAndStableOrder() {
        var out = TrendingService.rank(scores("b", 1, "a", 1, "c", 7), Set.of(), 2);
        assertThat(out).containsExactly("c", "a");
    }
}
