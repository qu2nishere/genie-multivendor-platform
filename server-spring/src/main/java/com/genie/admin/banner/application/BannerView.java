package com.genie.admin.banner.application;

import com.genie.admin.banner.domain.BannerPosition;
import com.genie.admin.banner.domain.BannerState;
import java.time.OffsetDateTime;

/**
 * Một dòng màn quản lý banner (API_SPEC 11.16). Ảnh là URL công khai ghép từ khóa đối tượng; CTR client tự chia.
 *
 * @param category {@code null} khi vị trí không phải {@code CATEGORY_TOP}
 */
public record BannerView(long id, String title, String imageUrl, String mobileImageUrl, String targetUrl,
                         BannerPosition position, CategoryRef category, int sortOrder, OffsetDateTime startsAt,
                         OffsetDateTime endsAt, boolean isActive, BannerState state, long impressions, long clicks) {

    /** Danh mục của banner; {@code name} có thể {@code null} khi module catalog chưa cài cổng tra cứu. */
    public record CategoryRef(long id, String name) {
    }
}
