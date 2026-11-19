package com.genie.catalog.search.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Chuẩn hóa từ khóa (plan 2026-11-17 A1). */
class KeywordNormalizerTest {

    @Test
    void lowercasesCollapsesSpacesAndKeepsVietnameseMarks() {
        assertThat(KeywordNormalizer.normalize("  Áo   THUN  Nam ")).isEqualTo("áo thun nam");
    }

    @Test
    void specialCharactersBecomeSpaces() {
        assertThat(KeywordNormalizer.normalize("tai-nghe, (chống ồn)!!")).isEqualTo("tai nghe chống ồn");
        assertThat(KeywordNormalizer.normalize("!!!@@@")).isEmpty();
        assertThat(KeywordNormalizer.normalize(null)).isEmpty();
    }

    @Test
    void cutsToMaxLength() {
        assertThat(KeywordNormalizer.normalize("x".repeat(500))).hasSize(KeywordNormalizer.MAX_LENGTH);
    }

    @Test
    void unaccentHandlesDStroke() {
        assertThat(KeywordNormalizer.unaccent("Đồng hồ đẹp")).isEqualTo("Dong ho dep");
        assertThat(KeywordNormalizer.normalizeUnaccented("Đồng HỒ  đẹp")).isEqualTo("dong ho dep");
    }

    @Test
    void decomposedInputGivesSameResultAsComposed() {
        String nfd = java.text.Normalizer.normalize("áo thun", java.text.Normalizer.Form.NFD);
        assertThat(KeywordNormalizer.normalize(nfd)).isEqualTo("áo thun");
    }

    @Test
    void prefixQueryMarksOnlyLastToken() {
        assertThat(KeywordNormalizer.prefixTsQuery("tai nghe chong")).isEqualTo("tai & nghe & chong:*");
        assertThat(KeywordNormalizer.prefixTsQuery("tai")).isEqualTo("tai:*");
        assertThat(KeywordNormalizer.prefixTsQuery("")).isEmpty();
    }
}
