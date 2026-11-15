package com.genie.auth.address.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<Address, Long> {

    /** Sổ địa chỉ: mặc định đứng đầu, rồi mới nhất (is_default có thể NULL nên không dùng ORDER BY is_default DESC). */
    @Query("select a from Address a where a.userId = :userId and a.isDeleted = false "
            + "order by case when a.isDefault = true then 0 else 1 end, a.createdAt desc, a.id desc")
    List<Address> findActiveByUserId(@Param("userId") Long userId);

    @Query("select a from Address a where a.id = :id and a.userId = :userId and a.isDeleted = false")
    Optional<Address> findActiveByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("select count(a) from Address a where a.userId = :userId and a.isDeleted = false")
    long countActiveByUserId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Address a set a.isDefault = false where a.userId = :userId and a.isDefault = true")
    int clearDefault(@Param("userId") Long userId);
}
