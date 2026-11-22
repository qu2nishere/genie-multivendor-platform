package com.genie.catalog.search.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Slug sinh từ tên (plan 2026-11-17 A11, D2) — cùng bộ mẫu với {@code web/src/shared/lib/slug.test.ts}. */
class SlugUtilTest {

    static final Path SAMPLES = Path.of("..", "docs", "api-tests", "slug-samples.json");

    @Test
    void matchesSharedSamples() throws Exception {
        List<Map<String, String>> samples = new ObjectMapper().readValue(Files.readString(SAMPLES),
                new TypeReference<>() { });
        assertThat(samples).hasSizeGreaterThanOrEqualTo(10);
        for (Map<String, String> s : samples) {
            assertThat(SlugUtil.slugify(s.get("name"))).as(s.get("name")).isEqualTo(s.get("slug"));
        }
    }

    @Test
    void neverExceedsMaxLengthOrEndsWithDash() {
        String slug = SlugUtil.slugify("a ".repeat(200));
        assertThat(slug.length()).isLessThanOrEqualTo(SlugUtil.MAX_LENGTH);
        assertThat(slug).doesNotEndWith("-").doesNotStartWith("-");
    }

    @Test
    void nullBecomesDefault() {
        assertThat(SlugUtil.slugify(null)).isEqualTo("san-pham");
    }
}
