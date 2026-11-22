package com.genie.catalog.attribute.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, Long> {

    List<CategoryAttribute> findByCategoryIdOrderByDisplayOrderAscIdAsc(Long categoryId);

    List<CategoryAttribute> findByCategoryIdInOrderByDisplayOrderAscIdAsc(Collection<Long> categoryIds);

    Optional<CategoryAttribute> findByCategoryIdAndCode(Long categoryId, String code);

    /** Thuộc tính trùng {@code code} trong các ngành cho trước (ngành, tổ tiên, con cháu — plan B2 D8). */
    List<CategoryAttribute> findByCodeAndCategoryIdIn(String code, Collection<Long> categoryIds);

    @Query("SELECT COALESCE(MAX(a.displayOrder), 0) FROM CategoryAttribute a WHERE a.categoryId = :categoryId")
    int maxDisplayOrder(@Param("categoryId") Long categoryId);
}
