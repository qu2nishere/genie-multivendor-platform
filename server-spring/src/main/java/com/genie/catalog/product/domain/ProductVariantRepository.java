package com.genie.catalog.product.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /** SKU chưa xóa mềm của nhiều sản phẩm — một truy vấn cho cả trang (tránh N+1). */
    @Query("""
            SELECT v.productId AS productId, v.id AS id, v.price AS price, v.stockCount AS stockCount,
                   v.reservedCount AS reservedCount
            FROM ProductVariant v WHERE v.productId IN :productIds AND v.deletedAt IS NULL ORDER BY v.productId, v.id""")
    List<ProductSummaryRows.VariantRow> findActiveRows(@Param("productIds") Collection<Long> productIds);
}
