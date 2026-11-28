// Cổng xử lý / thu nhỏ ảnh trước khi gửi Gemini — UC-CAT-12 (update_gemini.md mục 11.2)
package com.genie.catalog.moderation.application;

import java.util.Optional;

public interface ScreeningImagePort {

    /** @return ảnh đã thu nhỏ (JPEG) / WebP gốc; {@code Optional.empty()} nếu không dùng được. */
    Optional<Image> prepare(byte[] source, String mimeType);

    record Image(String mimeType, byte[] data) {
    }
}
