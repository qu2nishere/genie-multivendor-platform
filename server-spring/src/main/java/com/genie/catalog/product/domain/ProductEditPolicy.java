// Sửa trường nào thì sản phẩm đang bán phải duyệt lại — UC-CAT-10 bước 5–7 (plan C1 D4; API_SPEC 10.5 "requires_review")
package com.genie.catalog.product.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Luật thuần, không phụ thuộc Spring: so ảnh chụp "trường quan trọng" trước và sau khi sửa. Đổi tên, danh sách ảnh (thêm /
 * bớt / đổi thứ tự — ảnh đầu là ảnh chính), video, mô tả, danh mục, thương hiệu, giấy tờ hoặc thông số → phải duyệt lại.
 * Giá, tồn, thêm / bỏ biến thể, ảnh phân loại, vận chuyển, đặt trước → cập nhật ngay, không duyệt lại.
 */
public final class ProductEditPolicy {

    private ProductEditPolicy() {
    }

    /**
     * @param imageKeys    khóa ảnh theo thứ tự hiển thị
     * @param description  mô tả ĐÃ lọc HTML (so đúng thứ sẽ lưu)
     * @param documentKeys khóa giấy tờ (thứ tự không quan trọng)
     * @param specs        thông số đã chuẩn hóa {@code mã / tên → giá trị}
     */
    public record ReviewFields(String name, List<String> imageKeys, String videoKey, String description, Long categoryId,
                               Long brandId, Set<String> documentKeys, Map<String, String> specs) {
    }

    public static boolean requiresReview(ReviewFields before, ReviewFields after) {
        return !Objects.equals(trim(before.name()), trim(after.name()))
                || !Objects.equals(nullToEmpty(before.imageKeys()), nullToEmpty(after.imageKeys()))
                || !Objects.equals(blankToNull(before.videoKey()), blankToNull(after.videoKey()))
                || !Objects.equals(blankToNull(before.description()), blankToNull(after.description()))
                || !Objects.equals(before.categoryId(), after.categoryId())
                || !Objects.equals(before.brandId(), after.brandId())
                || !Objects.equals(before.documentKeys() == null ? Set.of() : before.documentKeys(),
                        after.documentKeys() == null ? Set.of() : after.documentKeys())
                || !Objects.equals(before.specs() == null ? Map.of() : before.specs(),
                        after.specs() == null ? Map.of() : after.specs());
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static List<String> nullToEmpty(List<String> l) {
        return l == null ? List.of() : l;
    }
}
