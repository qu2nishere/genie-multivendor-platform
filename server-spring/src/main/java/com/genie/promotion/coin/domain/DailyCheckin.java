package com.genie.promotion.coin.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * DailyCheckin — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "daily_checkins", uniqueConstraints = {
    @UniqueConstraint(name = "uk_daily_checkin_customer", columnNames = {"customerId"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class DailyCheckin {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long customerId;                  // FK → users.id, UNIQUE — mỗi khách 1 dòng theo dõi

    @Column(nullable = false)
    private OffsetDateTime lastCheckinAt;      // Thời điểm điểm danh gần nhất

    @Column(nullable = false)
    private Integer streakCount;              // Số ngày điểm danh LIÊN TIẾP — đứt 1 ngày là reset về 1
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Bản ghi điểm danh của khách chưa từng điểm danh (chưa lưu — gọi {@link #checkIn} rồi mới lưu) — UC-PRO-08 */
    public static DailyCheckin firstFor(Long customerId) {
        DailyCheckin d = new DailyCheckin();
        d.customerId = customerId;
        d.streakCount = 0;
        return d;
    }

    /** Điểm danh, tính chuỗi & số Điểm thưởng — UC-PRO-08 */
    public int checkIn(OffsetDateTime now) {
        LocalDate today = now.toLocalDate();
        // Quy mốc cũ về cùng offset với now: giá trị đọc từ CSDL mang offset UTC (hibernate.jdbc.time_zone = UTC), so ngày
        // trực tiếp sẽ lệch "ngày Việt Nam" lúc 00:00–07:00 (plan 2026-11-16 Điểm Genie P03.01)
        LocalDate last = this.lastCheckinAt == null ? null
                : this.lastCheckinAt.withOffsetSameInstant(now.getOffset()).toLocalDate();
        if (today.equals(last)) throw new IllegalStateException("Hôm nay đã điểm danh");
        this.streakCount = (last != null && last.plusDays(1).equals(today)) ? this.streakCount + 1 : 1;
        this.lastCheckinAt = now;
        return this.streakCount % 7 == 0 ? 1000 : 100;   // ngày thứ 7 liên tiếp: 1.000 Điểm
    }
}
