package com.genie.admin.banner.domain;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Truy vấn banner trang chủ (plan banner B02.04; UC-ADM-12). Tìm kiếm Admin dùng Specification ({@code BannerSpecs}). */
public interface HomeBannerRepository extends JpaRepository<HomeBanner, Long>, JpaSpecificationExecutor<HomeBanner> {

    /** Banner đang hiển thị ở một vị trí (không theo danh mục), sắp {@code sort_order, id}. */
    @Query("""
            SELECT b FROM HomeBanner b
            WHERE b.position = :position AND b.isActive = true AND b.startsAt <= :now AND b.endsAt > :now
            ORDER BY b.sortOrder, b.id""")
    List<HomeBanner> findLive(@Param("position") BannerPosition position, @Param("now") OffsetDateTime now);

    /** Banner đang hiển thị ở đầu trang một danh mục ({@code CATEGORY_TOP}). */
    @Query("""
            SELECT b FROM HomeBanner b
            WHERE b.position = :position AND b.categoryId = :categoryId
              AND b.isActive = true AND b.startsAt <= :now AND b.endsAt > :now
            ORDER BY b.sortOrder, b.id""")
    List<HomeBanner> findLiveInCategory(@Param("position") BannerPosition position,
                                        @Param("categoryId") Long categoryId, @Param("now") OffsetDateTime now);

    /**
     * Số banner BẬT khác (bỏ {@code excludeId}) cùng vị trí có lịch giao [{@code startsAt}, {@code endsAt}) và chưa kết
     * thúc — giới hạn 8 banner / vị trí (plan banner D2). {@code categoryId} chỉ so khi khác {@code null} (CATEGORY_TOP).
     */
    @Query("""
            SELECT count(b) FROM HomeBanner b
            WHERE b.position = :position AND b.isActive = true AND b.id <> :excludeId
              AND (:categoryId IS NULL OR b.categoryId = :categoryId)
              AND b.startsAt < :endsAt AND b.endsAt > :startsAt AND b.endsAt > :now""")
    long countOverlapping(@Param("position") BannerPosition position, @Param("categoryId") Long categoryId,
                          @Param("startsAt") OffsetDateTime startsAt, @Param("endsAt") OffsetDateTime endsAt,
                          @Param("now") OffsetDateTime now, @Param("excludeId") long excludeId);

    /** Cộng dồn bộ đếm đã gom từ Redis (job 5 phút — LLD 10). Không đụng {@code updated_at}. */
    @Modifying
    @Query("UPDATE HomeBanner b SET b.impressionCount = b.impressionCount + :imp, b.clickCount = b.clickCount + :clk "
            + "WHERE b.id = :id")
    int addStats(@Param("id") long id, @Param("imp") long impressions, @Param("clk") long clicks);
}
