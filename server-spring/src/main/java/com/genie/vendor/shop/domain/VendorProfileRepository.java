package com.genie.vendor.shop.domain;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorProfileRepository extends JpaRepository<VendorProfile, Long> {

    Optional<VendorProfile> findByUserId(Long userId);

    List<VendorProfile> findByUserIdIn(Collection<Long> userIds);

    /** Khóa dòng shop của người bán để đổi trạng thái KYC / cài đặt không bị ghi đè đồng thời. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VendorProfile v WHERE v.userId = :userId")
    Optional<VendorProfile> findByUserIdForUpdate(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VendorProfile v WHERE v.id = :id")
    Optional<VendorProfile> findByIdForUpdate(@Param("id") Long id);

    /** Tên shop đã có (không phân biệt hoa thường — khớp {@code uk_vendor_profiles_shop_name_lower}), trừ chính shop {@code exceptId}. */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM vendor_profiles WHERE lower(shop_name) = lower(:name) "
            + "AND (CAST(:exceptId AS BIGINT) IS NULL OR id <> :exceptId))", nativeQuery = true)
    boolean existsByShopNameNormalized(@Param("name") String name, @Param("exceptId") Long exceptId);

    /** Hàng chờ KYC (UC-VND-04 bước 2): rủi ro cao trước, rồi nộp sớm trước; dùng {@code idx_vendor_profiles_kyc_status}. */
    @Query(value = "SELECT v.* FROM vendor_profiles v LEFT JOIN kyc_results r ON r.vendor_profile_id = v.id "
            + "WHERE v.kyc_status IN (:statuses) "
            + "ORDER BY r.risk_score DESC NULLS LAST, v.kyc_submitted_at ASC NULLS LAST, v.id ASC",
            countQuery = "SELECT count(*) FROM vendor_profiles v WHERE v.kyc_status IN (:statuses)",
            nativeQuery = true)
    Page<VendorProfile> findKycQueue(@Param("statuses") Collection<String> statuses, Pageable pageable);

    /** Số hồ sơ theo từng trạng thái KYC — {@code [kyc_status, count]}. */
    @Query("SELECT v.kycStatus, count(v) FROM VendorProfile v GROUP BY v.kycStatus")
    List<Object[]> countByKycStatus();

    /** Hồ sơ kẹt {@code AI_PROCESSING} quá hạn (tính từ lúc nộp — nộp xong là gửi AI ngay); khóa bỏ qua dòng đang bận. */
    @Query(value = "SELECT * FROM vendor_profiles WHERE kyc_status = 'AI_PROCESSING' AND kyc_submitted_at < :before "
            + "ORDER BY kyc_submitted_at LIMIT 100 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<VendorProfile> findStuckAiProcessing(@Param("before") OffsetDateTime before);

    /** Yêu cầu bổ sung quá hạn 7 ngày; dùng {@code idx_vendor_profiles_supplement_deadline}. */
    @Query(value = "SELECT * FROM vendor_profiles WHERE kyc_status = 'SUPPLEMENT_REQUESTED' "
            + "AND kyc_supplement_deadline_at < :now ORDER BY kyc_supplement_deadline_at LIMIT 100 "
            + "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<VendorProfile> findSupplementExpired(@Param("now") OffsetDateTime now);

    /** Shop khác đang VERIFIED gắn cùng số CCCD (tín hiệu {@code DUPLICATE_ID} — ENTITY 1.6). */
    @Query("SELECT count(v) > 0 FROM VendorProfile v WHERE v.identityCode = :identityCode AND v.id <> :exceptId "
            + "AND v.kycStatus = com.genie.vendor.shop.domain.KycStatus.VERIFIED")
    boolean existsVerifiedWithIdentity(@Param("identityCode") String identityCode, @Param("exceptId") Long exceptId);

    /** Shop đã xác minh có tên chứa {@code q} — ô chọn shop của Admin (API 11.16). */
    @Query("SELECT v FROM VendorProfile v WHERE v.kycStatus = com.genie.vendor.shop.domain.KycStatus.VERIFIED "
            + "AND lower(v.shopName) LIKE lower(concat('%', :q, '%')) ORDER BY v.shopName")
    List<VendorProfile> searchVerified(@Param("q") String q, Pageable pageable);

    /** Cộng / trừ người theo dõi nguyên tử, không âm (UC-SOC-06; plan theo dõi gian hàng F01.03). */
    @Modifying
    @Query(value = "UPDATE vendor_profiles SET follower_count = GREATEST(follower_count + :delta, 0) WHERE user_id = :userId",
            nativeQuery = true)
    int adjustFollowerCount(@Param("userId") long userId, @Param("delta") int delta);
}
