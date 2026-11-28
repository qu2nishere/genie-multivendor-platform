package com.genie.catalog.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Luật thuần của đăng bán: gửi duyệt, "sửa trường nào thì duyệt lại", mã SKU (plan C1 P02 — D4, D5). */
class ProductListingRulesTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-26T10:00:00+07:00");

    private static Product product(ProductStatus status) {
        return Product.builder().vendorId(1L).name("Tai nghe").basePrice(BigDecimal.ONE).status(status)
                .moderatorId(9L).moderationReason("cũ").isPublished(status == ProductStatus.PUBLISHED).build();
    }

    // ===== Product.submit =====

    @Test
    void draftAndRejectedGoToQueueWithoutRequeueFlag() {
        for (ProductStatus s : List.of(ProductStatus.DRAFT, ProductStatus.REJECTED)) {
            Product p = product(s);
            p.submit(NOW);
            assertThat(p.getStatus()).isEqualTo(ProductStatus.PENDING_APPROVAL);
            assertThat(p.getIsRequeued()).isFalse();
            assertThat(p.getSubmittedAt()).isEqualTo(NOW);
            assertThat(p.getModeratorId()).isNull();
            assertThat(p.getModerationReason()).isNull();
        }
    }

    @Test
    void liveProductsAreRequeuedAndLeaveTheStorefront() {
        for (ProductStatus s : List.of(ProductStatus.PUBLISHED, ProductStatus.HIDDEN)) {
            Product p = product(s);
            p.submit(NOW);
            assertThat(p.getStatus()).isEqualTo(ProductStatus.PENDING_APPROVAL);
            assertThat(p.getIsRequeued()).isTrue();
            assertThat(p.getIsPublished()).isFalse();
        }
    }

    @Test
    void underReviewOrDeletedCannotBeSubmitted() {
        for (ProductStatus s : List.of(ProductStatus.PENDING_APPROVAL, ProductStatus.FLAGGED, ProductStatus.DELETED)) {
            assertThatThrownBy(() -> product(s).submit(NOW)).isInstanceOf(IllegalStateException.class);
        }
        assertThat(product(ProductStatus.FLAGGED).isUnderReview()).isTrue();
        assertThat(product(ProductStatus.DRAFT).isUnderReview()).isFalse();
    }

    @Test
    void lockedProductStaysLockedUntilApproved() {
        Product p = product(ProductStatus.HIDDEN);
        p.setIsLocked(true);
        p.submit(NOW);
        assertThat(p.getIsLocked()).isTrue();
        p.approve(5L);
        assertThat(p.getIsLocked()).isFalse();
        assertThat(p.getIsRequeued()).isFalse();
        assertThat(p.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
    }

    // ===== ProductEditPolicy =====

    private static ProductEditPolicy.ReviewFields fields(String name, List<String> images, Map<String, String> specs) {
        return new ProductEditPolicy.ReviewFields(name, images, null, "<p>Mô tả</p>", 11L, 9L, Set.of("docs/a.pdf"), specs);
    }

    @Test
    void priceStockAndSameFieldsDoNotNeedReview() {
        var before = fields("Tai nghe ANC", List.of("a.webp", "b.webp"), Map.of("warranty", "12 tháng"));
        var after = fields(" Tai nghe ANC ", List.of("a.webp", "b.webp"), Map.of("warranty", "12 tháng"));
        assertThat(ProductEditPolicy.requiresReview(before, after)).isFalse();
    }

    @Test
    void importantFieldsNeedReview() {
        var base = fields("Tai nghe ANC", List.of("a.webp", "b.webp"), Map.of("warranty", "12 tháng"));
        assertThat(ProductEditPolicy.requiresReview(base, fields("Tai nghe ANC Pro", base.imageKeys(), base.specs())))
                .isTrue();
        // đổi thứ tự ảnh = đổi ảnh chính
        assertThat(ProductEditPolicy.requiresReview(base, fields(base.name(), List.of("b.webp", "a.webp"), base.specs())))
                .isTrue();
        assertThat(ProductEditPolicy.requiresReview(base, fields(base.name(), base.imageKeys(), Map.of("warranty", "24 tháng"))))
                .isTrue();
        var otherCategory = new ProductEditPolicy.ReviewFields(base.name(), base.imageKeys(), null, base.description(), 12L,
                9L, base.documentKeys(), base.specs());
        assertThat(ProductEditPolicy.requiresReview(base, otherCategory)).isTrue();
        var newDocs = new ProductEditPolicy.ReviewFields(base.name(), base.imageKeys(), null, base.description(), 11L,
                9L, Set.of("docs/a.pdf", "docs/b.pdf"), base.specs());
        assertThat(ProductEditPolicy.requiresReview(base, newDocs)).isTrue();
    }

    // ===== SkuGenerator =====

    @Test
    void skuRules() {
        assertThat(SkuGenerator.isValid("KB-BLK_BLU.01")).isTrue();
        assertThat(SkuGenerator.isValid("có dấu")).isFalse();
        assertThat(SkuGenerator.isValid("")).isFalse();
        assertThat(SkuGenerator.isValid("x".repeat(101))).isFalse();
        assertThat(SkuGenerator.generated(6, 3)).isEqualTo("G6-3");
        assertThat(SkuGenerator.generated(6, 3)).startsWith(SkuGenerator.generatedPrefix(6));
        assertThatThrownBy(() -> SkuGenerator.generated(6, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
