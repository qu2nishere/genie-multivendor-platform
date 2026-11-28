// Dữ liệu trả cho màn Sản phẩm của Seller Center — API_SPEC 10.5 (UC-CAT-09, 10, 11; plan C1 D21)
package com.genie.catalog.product.application;

import java.util.List;

/** Gom các record trả về / nhận vào của nhóm {@code /seller/products} (JSON snake_case do Jackson đổi tên). */
public final class SellerProductViews {

    private SellerProductViews() {
    }

    /**
     * Một dòng danh sách. {@code status}: {@code FLAGGED} (AI nghi ngờ) trả {@code PENDING_APPROVAL} — shop không thấy kết
     * quả sàng lọc. {@code rejectReason}: lý do từ chối / lý do sàn khóa.
     */
    public record Row(long id, String name, String imageUrl, int variantCount, long priceMin, long stockTotal,
                      long soldCount, String status, String rejectReason, boolean isLocked) {
    }

    public record MediaRef(String objectKey, String url) {
    }

    public record VideoRef(String objectKey, String fileName, String url) {
    }

    /** {@code url}: link ký 5 phút (bucket riêng tư {@code product-docs}). */
    public record DocumentRef(long id, String fileName, String objectKey, String url) {
    }

    public record IdName(long id, String name) {
    }

    public record Spec(String name, String value) {
    }

    public record OptionValue(String value, String imageObjectKey, String imageUrl) {
    }

    public record OptionGroup(String name, List<OptionValue> values) {
    }

    /** {@code reserved}: số đang giữ chỗ cho đơn chưa thanh toán — tồn không hạ được dưới số này. */
    public record Variant(Long id, List<String> options, String sku, long price, int stock, int reserved) {
    }

    public record Dimensions(Integer length, Integer width, Integer height) {
    }

    /** {@code GET /seller/products/{id}} — mở form sửa / xem. {@code version}: gửi lại khi {@code PUT} (plan C1 D24). */
    public record Detail(long id, long version, String status, String rejectReason, boolean isLocked, boolean isRequeued,
                         List<MediaRef> images, VideoRef video, List<DocumentRef> documents, String name,
                         List<IdName> categoryPath, IdName brand, String condition, String origin, List<Spec> specs,
                         String descriptionHtml, List<OptionGroup> optionGroups, String variantImageGroup,
                         List<Variant> variants, Integer weightGrams, Dimensions dimensionsCm, String shippingPayer,
                         boolean isPreOrder, Integer preOrderDays) {
    }

    /** Kết quả {@code POST} / {@code PUT}: {@code requiresReview = true} khi sản phẩm vào hàng chờ duyệt. */
    public record Saved(long id, String status, boolean requiresReview) {
    }

    // ===== Đầu vào POST / PUT ($ProductInput — API 10.5) =====

    public record OptionValueInput(String value, String imageObjectKey) {
    }

    public record OptionGroupInput(String name, List<OptionValueInput> values) {
    }

    /** {@code id = null} → biến thể mới; {@code sku = null} → server tự sinh. Giá / tồn được để trống khi lưu nháp. */
    public record VariantInput(Long id, List<String> options, String sku, Long price, Integer stock) {
    }

    public record DimensionsInput(Integer length, Integer width, Integer height) {
    }

    /**
     * {@code submit = true}: gửi duyệt; {@code false}: lưu nháp (sản phẩm đang bán thì là "Lưu thay đổi"). {@code version}:
     * lấy từ {@code GET} chi tiết để phát hiện sửa trên dữ liệu cũ. {@code documentFileNames}: tên gốc, song song
     * {@code documentObjectKeys}.
     */
    public record ProductInput(boolean submit, String name, Long categoryId, Long brandId, String condition, String origin,
                               List<String> imageObjectKeys, String videoObjectKey, List<String> documentObjectKeys,
                               List<String> documentFileNames, List<Spec> specs, String descriptionHtml,
                               List<OptionGroupInput> optionGroups, String variantImageGroup,
                               List<VariantInput> variants, Integer weightGrams, DimensionsInput dimensionsCm,
                               String shippingPayer, Boolean isPreOrder, Integer preOrderDays, Long version) {
    }
}
