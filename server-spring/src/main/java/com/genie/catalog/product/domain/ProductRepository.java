package com.genie.catalog.product.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Cột tóm tắt của nhiều sản phẩm (không nạp entity — tránh quan hệ EAGER) cho màn danh sách. */
    @Query("SELECT p.id AS id, p.name AS name, p.status AS status, p.basePrice AS basePrice, p.vendorId AS vendorId "
            + "FROM Product p WHERE p.id IN :ids")
    List<ProductSummaryRows.ProductRow> findRowsByIds(@Param("ids") Collection<Long> ids);

    /** Trạng thái một sản phẩm (kiểm "đang bán" khi thả tim) — không nạp entity. */
    @Query("SELECT p.status FROM Product p WHERE p.id = :id")
    java.util.Optional<ProductStatus> findStatus(@Param("id") Long id);
}
