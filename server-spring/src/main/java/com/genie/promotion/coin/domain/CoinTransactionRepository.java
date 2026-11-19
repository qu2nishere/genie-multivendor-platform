package com.genie.promotion.coin.domain;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Sổ Điểm chỉ ghi thêm (ENTITY 4.8): dương = cộng (một lô có hạn), âm = trừ. */
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, Long> {

    /** Σ amount — phải luôn bằng {@code user_coins.balance} (UC-PRO-09). */
    @Query("SELECT coalesce(sum(t.amount), 0) FROM CoinTransaction t WHERE t.userId = :userId")
    long sumAmount(@Param("userId") Long userId);

    /** Các lô cộng, sắp hạn tăng dần (FEFO), lô không hạn cuối cùng. */
    @Query("""
            SELECT t FROM CoinTransaction t WHERE t.userId = :userId AND t.amount > 0
            ORDER BY CASE WHEN t.expiresAt IS NULL THEN 1 ELSE 0 END, t.expiresAt, t.id""")
    List<CoinTransaction> findCreditLots(@Param("userId") Long userId);

    /** Tổng đã trừ (số dương). */
    @Query("SELECT coalesce(-sum(t.amount), 0) FROM CoinTransaction t WHERE t.userId = :userId AND t.amount < 0")
    long sumDebits(@Param("userId") Long userId);

    /** Trang đầu lịch sử (mới nhất trước). */
    @Query("SELECT t FROM CoinTransaction t WHERE t.userId = :userId ORDER BY t.createdAt DESC, t.id DESC")
    List<CoinTransaction> findPage(@Param("userId") Long userId, Pageable page);

    /** Trang sau con trỏ ({@code createdAt}, {@code id}). */
    @Query("""
            SELECT t FROM CoinTransaction t WHERE t.userId = :userId
              AND (t.createdAt < :createdAt OR (t.createdAt = :createdAt AND t.id < :id))
            ORDER BY t.createdAt DESC, t.id DESC""")
    List<CoinTransaction> findPageAfter(@Param("userId") Long userId, @Param("createdAt") OffsetDateTime createdAt,
                                        @Param("id") Long id, Pageable page);

    /** Người có lô cộng đã quá hạn — đầu vào job hết hạn (P06). */
    @Query("SELECT DISTINCT t.userId FROM CoinTransaction t WHERE t.amount > 0 AND t.expiresAt < :now")
    List<Long> findUsersWithExpiredLots(@Param("now") OffsetDateTime now);

    /** Lô đã có dòng hết hạn tham chiếu tới chưa (job chạy lại không trừ thêm). */
    boolean existsByUserIdAndTypeAndReferenceId(Long userId, CoinTransactionType type, String referenceId);
}
