package com.genie.catalog.brand.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    /** Thương hiệu theo khóa chuẩn hóa (bỏ dấu, chữ thường) — chống trùng "Logitech" / "LOGITECH" (plan C1 D10). */
    Optional<Brand> findBySlug(String slug);
}
