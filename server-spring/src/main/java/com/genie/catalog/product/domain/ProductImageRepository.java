package com.genie.catalog.product.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /** Ảnh gallery chung của nhiều sản phẩm: ảnh chính trước, rồi theo thứ tự hiển thị — phần tử đầu mỗi sản phẩm là ảnh đại diện. */
    @Query("""
            SELECT i.productId AS productId, i.imageUrl AS imageUrl FROM ProductImage i
            WHERE i.productId IN :productIds AND i.variantId IS NULL
            ORDER BY i.productId, CASE WHEN i.isPrimary = true THEN 0 ELSE 1 END, i.displayOrder, i.id""")
    List<ProductSummaryRows.ImageRow> findGalleryRows(@Param("productIds") Collection<Long> productIds);
}
