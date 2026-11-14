package com.genie.catalog.search.domain;

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
 * SearchKeywordStat — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "search_keyword_stats", uniqueConstraints = {
    @UniqueConstraint(name = "uq_keyword_day", columnNames = {"normalized_keyword", "stat_date"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class SearchKeywordStat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false, length = 100)
    private String normalizedKeyword;         // Từ khóa đã chuẩn hóa: chữ thường, bỏ dấu cách thừa (giữ dấu tiếng Việt để hiển thị)

    @Column(nullable = false)
    private LocalDate statDate;               // Ngày thống kê

    @Builder.Default
    @Column(nullable = false)
    private Integer searchCount = 0;          // Số lượt tìm trong ngày (flush từ Redis search:kw:{date})

    @Builder.Default
    @Column(nullable = false)
    private Integer resultClickCount = 0;     // Số lượt bấm vào kết quả — loại từ khóa "tìm mà không mua"

    @Builder.Default
    @Column(nullable = false)
    private Boolean isBlocked = false;        // Staff chặn từ khóa nhạy cảm khỏi danh sách phổ biến
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Cộng dồn bộ đếm từ Redis — UC-CAT-04 */
    public void accumulate(int searches, int clicks) {
        this.searchCount += searches;
        this.resultClickCount += clicks;
    }
}
