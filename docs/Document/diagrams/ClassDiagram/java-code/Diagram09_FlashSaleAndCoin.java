/*
 * ============================================================================
 * GENIE — SƠ ĐỒ LỚP 09: Flash Sale, Điểm Genie & Điểm danh (Nhóm 4)
 * ============================================================================
 * Nguồn duy nhất : TLCN/diagrams/ClassDiagram/ENTITY.md (ENTITY.md TLCN v1.3) — sinh tự động, KHÔNG sửa tay;
 *                  sửa ENTITY.md rồi chạy lại công cụ sinh sơ đồ lớp (tools/classdiagram) để giữ đồng bộ.
 * Nhóm thực thể  : 
 * Thành phần     : 5 @Entity, 0 @IdClass, 3 enum, 35 thuộc tính, 15 phương thức nghiệp vụ
 * Entity         : FlashSaleCampaign, FlashSaleItem, UserCoin, CoinTransaction, DailyCheckin
 * Enum           : FlashSaleStatus, CoinTransactionType, FlashSaleItemStatus
 * Import vào EA  : Code > Import Source Directory / Import Source File (ngôn ngữ Java),
 *                  chọn cả thư mục java-code để EA nối được quan hệ giữa các sơ đồ.
 * Quy ước        : Getter/Setter do Lombok sinh — chỉ phương thức nghiệp vụ là ngăn Operations;
 *                  enum lấy nguyên văn từ Phụ lục A; @IdClass dựng từ các trường @Id của entity.
 */
package com.genie.platform.d09_flashsale_coin;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.hibernate.annotations.CreationTimestamp;
import java.time.*;
import java.util.*;

// ============================== ENUMS (3 — Phụ lục A) ==============================

public enum FlashSaleStatus { DRAFT, SCHEDULED, ACTIVE, ENDED }   // ← FlashSaleCampaign.status

public enum FlashSaleItemStatus { PENDING, APPROVED, REJECTED }   // ← FlashSaleItem.status (sàn duyệt từng sản phẩm đăng ký — TLCN v1.3)

public enum CoinTransactionType { CHECK_IN, ORDER_CASHBACK, REVIEW_BONUS, USED_AT_CHECKOUT, ORDER_REFUND, VOUCHER_REDEEM }   // ← CoinTransaction.type (mở rộng: COSMETIC_REDEEM, VENDOR_REWARD)

// ============================== ENTITIES (5) ==============================

// 4.5. FlashSaleCampaign — Chiến dịch Flash Sale  —  Bảng: flash_sale_campaigns
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

// 4.6. FlashSaleItem — Sản phẩm trong Flash Sale  —  Bảng: flash_sale_items
@Entity
@Table(name = "flash_sale_items")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class FlashSaleItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long campaignId;                  // FK → flash_sale_campaigns.id

    @Column(nullable = false)
    private Long productId;                   // FK → products.id — sản phẩm được giảm giá

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal flashPrice;            // GIÁ SỐC trong thời gian diễn ra chiến dịch

    @Column(nullable = false)
    private Integer flashStock;               // Số lượng GIỚI HẠN dành riêng cho flash sale

    @Builder.Default
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer reservedCount = 0;        // Số suất đã có ĐƠN đang chờ thanh toán (tăng khi tạo Order + StockReservation; giảm khi PAID hoặc giữ chỗ hết hạn)

    @Builder.Default
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer soldCount = 0;            // Số đã bán trong flash sale — dùng vẽ thanh tiến trình

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlashSaleItemStatus status = FlashSaleItemStatus.PENDING; // Kết quả sàn duyệt đăng ký: PENDING / APPROVED / REJECTED — UC-PRO-05 (TLCN v1.3)

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống race condition khi trừ tồn kho Flash Sale đồng thời
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đăng ký: 0 < flashStock ≤ tồn khả dụng của sản phẩm (Σ stockCount − reservedCount các SKU); kiểm tra lại khi chiến dịch mở bán — UC-PRO-05 */
    public void validateAgainst(int productAvailableStock) {
        if (this.flashStock <= 0 || this.flashStock > productAvailableStock)
            throw new IllegalArgumentException("Số suất Flash Sale (" + this.flashStock + ") phải > 0 và ≤ tồn khả dụng (" + productAvailableStock + ")");
    }

    /** Suất còn lại — UC-PRO-06 */
    public int remaining() {
        return this.flashStock - this.soldCount - this.reservedCount;
    }

    /** Giữ suất khi tạo Order + StockReservation trong cùng transaction — UC-PRO-06 */
    public void reserve(int qty) {
        if (remaining() < qty) throw new IllegalStateException("GNE-INV-4091");   // hết suất Flash Sale
        this.reservedCount += qty;
    }

    /** Giữ chỗ hết hạn / đơn bị hủy trước khi trả tiền → trả suất — UC-ORD-08 */
    public void releaseReservation(int qty) {
        if (this.reservedCount < qty) throw new IllegalStateException("reservedCount không đủ để nhả");
        this.reservedCount -= qty;
    }

    /** Ghi bền khi đơn Flash Sale thanh toán thành công — UC-PRO-06 */
    public void recordSale(int qty) {
        if (this.reservedCount < qty) throw new IllegalStateException("reservedCount không đủ");
        this.reservedCount -= qty;
        this.soldCount += qty;
    }
}

// 4.7. UserCoin — Số dư Điểm  —  Bảng: user_coins
@Entity
@Table(name = "user_coins")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class UserCoin {

    @Id
    private Long userId;                      // KHOÁ CHÍNH đồng thời là FK → users.id (KHÔNG tự tăng) — ví Điểm của khách hàng (1 Điểm = 1₫ khi thanh toán)

    @Column(nullable = false)
    private Integer balance;                  // Số Điểm hiện có

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống race condition khi trừ Điểm đồng thời

    @Column(name = "balance_hash", nullable = false, length = 255)
    private String balanceHash;               // HMAC-SHA256(userId + balance + secret) — chống can thiệp trực tiếp CSDL

    @Column(nullable = false)
    private OffsetDateTime lastUpdated;        // Lần biến động gần nhất
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Cộng Điểm (service cập nhật lại balanceHash) — UC-PRO-08 */
    public void earn(int amount, OffsetDateTime now) {
        this.balance += amount;
        this.lastUpdated = now;
    }

    /** Trừ Điểm (thanh toán đơn / đổi voucher), FEFO theo lô ở service — UC-PRO-10 */
    public void spend(int amount, OffsetDateTime now) {
        if (amount > this.balance) throw new IllegalStateException("Không đủ Điểm");
        this.balance -= amount;
        this.lastUpdated = now;
    }

    /** Phát hiện sửa số dư trực tiếp CSDL — UC-PRO-09 */
    public boolean verifyHash(String expectedHmac) {
        return MessageDigest.isEqual(this.balanceHash.getBytes(StandardCharsets.UTF_8), expectedHmac.getBytes(StandardCharsets.UTF_8));
    }
}

// 4.8. CoinTransaction — Lịch sử biến động Điểm  —  Bảng: coin_transactions
@Entity
@Table(name = "coin_transactions")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CoinTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — Điểm của khách hàng nào

    @Column(nullable = false)
    private Integer amount;                   // Số Điểm biến động — DƯƠNG là cộng, ÂM là trừ (cùng quy ước dấu với các sổ ví tiền)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoinTransactionType type;         // CHECK_IN / ORDER_CASHBACK / REVIEW_BONUS / USED_AT_CHECKOUT / ORDER_REFUND / VOUCHER_REDEEM

    @Column(length = 50)
    private String referenceId;               // NULLABLE — ID đối tượng liên quan (thường là order_id)

    @Column(length = 200)
    private String label;                     // NULLABLE — Mô tả hiển thị: "Hoàn Điểm đơn hàng #1234"

    private OffsetDateTime expiresAt;          // NULLABLE — Điểm HẾT HẠN sau 12 tháng kể từ ngày nhận (giao diện: "Điểm hết hạn sau 12 tháng")

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm phát sinh
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Lô Điểm đã hết hạn (12 tháng) — UC-PRO-09 */
    public boolean isExpired(OffsetDateTime now) {
        return this.expiresAt != null && this.expiresAt.isBefore(now);
    }

    /** Giao dịch cộng Điểm — UC-PRO-09 */
    public boolean isCredit() {
        return this.amount > 0;
    }
}

// 4.9. DailyCheckin — Điểm danh hằng ngày  —  Bảng: daily_checkins
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

    /** Bản ghi của khách chưa từng điểm danh (chưa lưu — gọi checkIn rồi mới lưu) — UC-PRO-08 */
    public static DailyCheckin firstFor(Long customerId) {
        DailyCheckin d = new DailyCheckin();
        d.customerId = customerId;
        d.streakCount = 0;
        return d;
    }

    /** Điểm danh, tính chuỗi & số Điểm thưởng — UC-PRO-08 */
    public int checkIn(OffsetDateTime now) {
        LocalDate today = now.toLocalDate();
        // quy mốc cũ về cùng offset với now (CSDL trả TIMESTAMPTZ theo UTC) — "ngày" là ngày Việt Nam (now đã ở +07:00)
        LocalDate last = this.lastCheckinAt == null ? null
                : this.lastCheckinAt.withOffsetSameInstant(now.getOffset()).toLocalDate();
        if (today.equals(last)) throw new IllegalStateException("Hôm nay đã điểm danh");
        this.streakCount = (last != null && last.plusDays(1).equals(today)) ? this.streakCount + 1 : 1;
        this.lastCheckinAt = now;
        return this.streakCount % 7 == 0 ? 1000 : 100;   // ngày thứ 7 liên tiếp: 1.000 Điểm
    }
}
