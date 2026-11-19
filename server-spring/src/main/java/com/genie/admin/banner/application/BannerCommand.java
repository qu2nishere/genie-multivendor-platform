package com.genie.admin.banner.application;

import com.genie.admin.banner.domain.BannerPosition;
import java.time.OffsetDateTime;

/**
 * Dữ liệu tạo / sửa banner (API_SPEC 11.16 — {@code POST} / {@code PUT /admin/banners}).
 *
 * @param imageObjectKey       khóa {@code tmp/…} vừa tải lên; {@code null} khi sửa = giữ ảnh
 * @param mobileImageObjectKey khóa {@code tmp/…}; {@code null} = giữ (khi sửa) / không có (khi tạo); {@code ""} = bỏ ảnh mobile
 */
public record BannerCommand(String title, String imageObjectKey, String mobileImageObjectKey, String targetUrl,
                            BannerPosition position, Long categoryId, Integer sortOrder, OffsetDateTime startsAt,
                            OffsetDateTime endsAt, Boolean isActive) {
}
