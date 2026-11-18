package com.genie.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Slug khớp quy tắc của web ({@code shared/lib/slug.ts}). */
class SlugsTest {

    @Test
    void matchesWebSlugify() {
        assertThat(Slugs.slugify("Điện thoại & Phụ kiện")).isEqualTo("dien-thoai-phu-kien");
        assertThat(Slugs.slugify("Tai nghe Bluetooth chống ồn ANC Pro 5 — Pin 40 giờ"))
                .isEqualTo("tai-nghe-bluetooth-chong-on-anc-pro-5-pin-40-gio");
        assertThat(Slugs.slugify("Laptop UltraBook 14\" Core i5, 16GB RAM"))
                .isEqualTo("laptop-ultrabook-14-core-i5-16gb-ram");
        assertThat(Slugs.slugify("  --Đồng hồ--  ")).isEqualTo("dong-ho");
        assertThat(Slugs.slugify(null)).isEmpty();
    }
}
