package com.genie.promotion.flashsale.domain;

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
 * FlashSaleCampaign — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "flash_sale_campaigns")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class FlashSaleCampaign {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    private Long vendorId;                    // NULLABLE — FK → users.id. NULL = chiến dịch TOÀN SÀN

    @Column(nullable = false, length = 200)
    private String title;                     // Tên chiến dịch: "Flash Sale 12.12"

    @Column(nullable = false)
    private OffsetDateTime startsAt;           // Thời điểm bắt đầu

    @Column(nullable = false)
    private OffsetDateTime endsAt;             // Thời điểm kết thúc

    @Builder.Default
    @Column(nullable = false)
    private Integer maxProducts = 20;         // Số sản phẩm tối đa được tham gia (mặc định 20)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlashSaleStatus status;           // DRAFT / SCHEDULED / ACTIVE / ENDED

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo chiến dịch

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "campaign_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua FlashSaleItem.campaignId
    private List<FlashSaleItem> items;        // Danh sách sản phẩm trong chiến dịch
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Lên lịch (sau khi duyệt) — UC-PRO-05 */
    public void schedule() {
        if (this.status != FlashSaleStatus.DRAFT) throw new IllegalStateException("Chỉ lên lịch bản nháp");
        if (this.items.size() > this.maxProducts) throw new IllegalStateException("Vượt số sản phẩm tối đa");
        this.status = FlashSaleStatus.SCHEDULED;
    }

    /** Cron mở bán khi now ≥ startsAt — UC-PRO-05 */
    public void activate(OffsetDateTime now) {
        if (this.status != FlashSaleStatus.SCHEDULED || now.isBefore(this.startsAt)) throw new IllegalStateException("Chưa tới giờ mở bán");
        this.status = FlashSaleStatus.ACTIVE;
    }

    /** Đang trong khung giờ vàng — UC-PRO-06 */
    public boolean isRunning(OffsetDateTime now) {
        return this.status == FlashSaleStatus.ACTIVE && !now.isBefore(this.startsAt) && now.isBefore(this.endsAt);
    }

    /** Kết thúc chiến dịch (Cron khi now ≥ endsAt, hoặc Admin dừng) — UC-PRO-05 */
    public void end() {
        if (this.status != FlashSaleStatus.ACTIVE && this.status != FlashSaleStatus.SCHEDULED) throw new IllegalStateException("Chiến dịch không chạy");
        this.status = FlashSaleStatus.ENDED;
    }
}
