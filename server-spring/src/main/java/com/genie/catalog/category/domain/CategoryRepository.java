package com.genie.catalog.category.domain;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Category c WHERE c.id = :id")
    Optional<Category> findByIdForUpdate(@Param("id") Long id);

    /** Khóa nhiều dòng theo thứ tự id (cấu hình hoa hồng hàng loạt — tránh deadlock). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Category c WHERE c.id IN :ids ORDER BY c.id")
    List<Category> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    Optional<Category> findBySlug(String slug);

    boolean existsByParentId(Long parentId);

    @Query("SELECT COALESCE(MAX(c.displayOrder), 0) FROM Category c WHERE c.parentId IS NULL")
    int maxRootDisplayOrder();

    @Query("SELECT COALESCE(MAX(c.displayOrder), 0) FROM Category c WHERE c.parentId = :parentId")
    int maxDisplayOrderUnder(@Param("parentId") Long parentId);

    /** Thứ tự kế tiếp trong nhóm anh em ({@code null} = danh mục gốc). */
    default int nextDisplayOrder(Long parentId) {
        return (parentId == null ? maxRootDisplayOrder() : maxDisplayOrderUnder(parentId)) + 1;
    }
}
