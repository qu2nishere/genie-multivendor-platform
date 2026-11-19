package com.genie.vendor.shop.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorProfileRepository extends JpaRepository<VendorProfile, Long> {

    Optional<VendorProfile> findByUserId(Long userId);

    List<VendorProfile> findByUserIdIn(Collection<Long> userIds);

    /** Cộng / trừ người theo dõi nguyên tử, không âm (UC-SOC-06; plan theo dõi gian hàng F01.03). */
    @Modifying
    @Query(value = "UPDATE vendor_profiles SET follower_count = GREATEST(follower_count + :delta, 0) WHERE user_id = :userId",
            nativeQuery = true)
    int adjustFollowerCount(@Param("userId") long userId, @Param("delta") int delta);
}
