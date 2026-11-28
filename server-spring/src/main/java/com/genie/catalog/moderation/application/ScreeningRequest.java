// Dữ liệu tin đăng gửi AI sàng lọc (KHÔNG có dữ liệu cá nhân — N10) — UC-CAT-12 (plan C1 P06; update_gemini.md mục 11.1)
package com.genie.catalog.moderation.application;

import java.util.List;
import java.util.Map;

public record ScreeningRequest(
        long productId,
        String name,
        String categoryPath,
        String brand,
        String condition,
        long minPrice,
        long maxPrice,
        String descriptionText,
        Map<String, String> specs,
        List<String> variantNames,
        List<String> documentNames,
        List<Image> images,
        int skippedImages) {

    /** {@code label} hiển thị cho Gemini, vd "ảnh bìa", "ảnh phân loại \"Màu sắc: Đỏ\"", "ảnh trong mô tả". */
    public record Image(String mimeType, byte[] data, String label) {
    }
}
