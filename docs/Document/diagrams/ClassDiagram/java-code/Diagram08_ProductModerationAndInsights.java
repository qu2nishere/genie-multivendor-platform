/*
 * ============================================================================
 * GENIE — SƠ ĐỒ LỚP 08: Kiểm duyệt sản phẩm, Giấy tờ chứng thực, Đề xuất danh mục & Thống kê truy cập (Nhóm 2)
 * ============================================================================
 * Nguồn duy nhất : TLCN/diagrams/ClassDiagram/ENTITY.md (ENTITY.md TLCN v1.2) — sinh tự động, KHÔNG sửa tay;
 *                  sửa ENTITY.md rồi chạy lại công cụ sinh sơ đồ lớp (tools/classdiagram) để giữ đồng bộ.
 * Nhóm thực thể  : 
 * Thành phần     : 5 @Entity, 0 @IdClass, 4 enum, 46 thuộc tính, 7 phương thức nghiệp vụ
 * Entity         : ProductDocument, CategoryProposal, ProductModerationResult, SearchKeywordStat, ProductViewStat
 * Enum           : ProductDocumentType, CategoryProposalStatus, ModerationDecision, TrafficSource
 * Import vào EA  : Code > Import Source Directory / Import Source File (ngôn ngữ Java),
 *                  chọn cả thư mục java-code để EA nối được quan hệ giữa các sơ đồ.
 * Quy ước        : Getter/Setter do Lombok sinh — chỉ phương thức nghiệp vụ là ngăn Operations;
 *                  enum lấy nguyên văn từ Phụ lục A; @IdClass dựng từ các trường @Id của entity.
 */
package com.genie.platform.d08_moderation_insights;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.*;
import com.genie.platform.d02_catalog_cart.Product;

// ============================== ENUMS (4 — Phụ lục A) ==============================

public enum ProductDocumentType { BUSINESS_LICENSE, QUALITY_CERTIFICATE, IMPORT_DOCUMENT, BRAND_AUTHORIZATION, OTHER }   // ← ProductDocument.documentType

public enum CategoryProposalStatus { PENDING, APPROVED, REJECTED }   // ← CategoryProposal.status

public enum ModerationDecision { APPROVE, REJECT, NEED_REVIEW }   // ← ProductModerationResult.decision

public enum TrafficSource { SEARCH, HOME_FEED, PROMOTION, SHOP_PAGE, EXTERNAL_LINK }   // ← ProductViewStat.source

// ============================== ENTITIES (5) ==============================

// 2.8. ProductDocument — Giấy tờ chứng thực sản phẩm  —  Bảng: product_documents
@Entity
@Table(name = "product_documents", indexes = {
    @Index(name = "idx_product_document_product", columnList = "product_id")
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductDocument {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long productId;                   // FK → products.id (ON DELETE CASCADE)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductDocumentType documentType; // BUSINESS_LICENSE / QUALITY_CERTIFICATE / IMPORT_DOCUMENT / BRAND_AUTHORIZATION / OTHER

    @Column(nullable = false, length = 500)
    private String fileUrl;                   // Đường dẫn Silo (bucket riêng tư, đọc qua Presigned URL)

    @Column(nullable = false, length = 255)
    private String fileName;                  // Tên tệp gốc hiển thị cho Staff

    @Column(nullable = false, length = 20)
    private String fileExtension;             // pdf / doc / docx / xls / xlsx — KHÔNG nhận ảnh (giao diện chặn)

    @Column(nullable = false)
    private Long fileSizeBytes;               // ≤ 10 MB / tệp

    private LocalDate expiresAt;              // NULLABLE — Ngày hết hạn của giấy tờ (giấy phép có thời hạn); NULL = không thời hạn

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime uploadedAt;

    // ===== QUAN HỆ =====
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;                  // Sản phẩm sở hữu giấy tờ
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Giấy tờ còn hiệu lực tại thời điểm duyệt — UC-CAT-13 */
    public boolean isValidOn(LocalDate day) {
        return this.expiresAt == null || !this.expiresAt.isBefore(day);
    }
}

// 2.9. CategoryProposal — Đề xuất danh mục mới của người bán  —  Bảng: category_proposals
@Entity
@Table(name = "category_proposals", indexes = {
    @Index(name = "idx_category_proposal_status", columnList = "status, created_at")
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CategoryProposal {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — shop đề xuất

    private Long parentCategoryId;            // NULLABLE — FK → categories.id — danh mục cha đề xuất; NULL = đề xuất danh mục gốc

    @Column(nullable = false, length = 100)
    private String proposedName;              // Tên danh mục đề xuất: "Thú cưng (Pet)"

    @Column(columnDefinition = "TEXT")
    private String reason;                    // NULLABLE — Shop giải thích vì sao cần danh mục này

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private CategoryProposalStatus status = CategoryProposalStatus.PENDING; // PENDING / APPROVED / REJECTED

    private Long createdCategoryId;           // NULLABLE — FK → categories.id — danh mục đã tạo khi duyệt

    private Long reviewedByStaffId;           // NULLABLE — FK mềm → users.id — Staff / Admin xử lý

    @Column(length = 500)
    private String rejectionReason;           // NULLABLE — Lý do từ chối

    private OffsetDateTime reviewedAt;         // NULLABLE

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Duyệt đề xuất — Service tạo Category mới rồi gọi hàm này — UC-CAT-17 */
    public void approve(Long categoryId, Long staffId, OffsetDateTime now) {
        if (this.status != CategoryProposalStatus.PENDING) throw new IllegalStateException("Đề xuất đã được xử lý");
        this.status = CategoryProposalStatus.APPROVED;
        this.createdCategoryId = categoryId;
        this.reviewedByStaffId = staffId;
        this.reviewedAt = now;
    }

    /** Từ chối đề xuất kèm lý do — UC-CAT-17 */
    public void reject(String reason, Long staffId, OffsetDateTime now) {
        if (this.status != CategoryProposalStatus.PENDING) throw new IllegalStateException("Đề xuất đã được xử lý");
        this.status = CategoryProposalStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewedByStaffId = staffId;
        this.reviewedAt = now;
    }
}

// 2.10. ProductModerationResult — Kết quả kiểm duyệt sản phẩm tự động  —  Bảng: product_moderation_results
@Entity
@Table(name = "product_moderation_results", indexes = {
    @Index(name = "idx_moderation_product_time", columnList = "product_id, created_at")
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductModerationResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long productId;                   // FK → products.id

    @Column(nullable = false, length = 60)
    private String modelName;                 // Mô hình đã dùng: "gemini-2.5-flash" (TLCN) / tên model AI Worker (DATN)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModerationDecision decision;      // APPROVE / REJECT / NEED_REVIEW

    @Column(nullable = false)
    private Integer riskScore;                // 0–100 — điểm rủi ro do mô hình trả về

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String violationsJson;            // NULLABLE — Mã vi phạm: ["COUNTERFEIT","PROHIBITED_ITEM","MISLEADING_DESCRIPTION","BAD_IMAGE","PRICE_ANOMALY","WRONG_CATEGORY"]

    @Column(columnDefinition = "TEXT")
    private String reasoning;                 // NULLABLE — Giải thích tiếng Việt hiển thị ở ô "Phân tích từ AI"

    @Column(columnDefinition = "TEXT")
    private String rawResponse;               // NULLABLE — JSON gốc mô hình trả về (phục vụ điều tra; không hiển thị)

    @Builder.Default
    @Column(nullable = false)
    private Boolean fallbackToManual = false; // true = gọi mô hình lỗi / quá hạn / JSON sai → chuyển thẳng hàng chờ duyệt thủ công

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ===== QUAN HỆ =====
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;                  // Sản phẩm được kiểm duyệt
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Kết quả khi gọi mô hình thất bại — luôn chuyển duyệt thủ công — UC-CAT-12 */
    public static ProductModerationResult fallback(Long productId, String modelName, String error, OffsetDateTime now) {
        ProductModerationResult r = new ProductModerationResult();
        r.productId = productId; r.modelName = modelName; r.decision = ModerationDecision.NEED_REVIEW;
        r.riskScore = 50; r.fallbackToManual = true; r.rawResponse = error; r.createdAt = now;
        return r;
    }

    /** Có tự từ chối được không (REJECT và điểm rủi ro ≥ ngưỡng cấu hình) — UC-CAT-12 */
    public boolean isAutoReject(int rejectThreshold) {
        return this.decision == ModerationDecision.REJECT && this.riskScore >= rejectThreshold;
    }
}

// 2.11. SearchKeywordStat — Thống kê từ khóa tìm kiếm theo ngày  —  Bảng: search_keyword_stats
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

// 2.12. ProductViewStat — Lượt xem sản phẩm theo ngày & nguồn truy cập  —  Bảng: product_view_stats
@Entity
@Table(name = "product_view_stats", uniqueConstraints = {
    @UniqueConstraint(name = "uq_product_view_day_source", columnNames = {"product_id", "stat_date", "source"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductViewStat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long productId;                   // FK → products.id

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — sao chép từ sản phẩm để tổng hợp theo shop không cần JOIN

    @Column(nullable = false)
    private LocalDate statDate;               // Ngày thống kê

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrafficSource source;             // SEARCH / HOME_FEED / PROMOTION / SHOP_PAGE / EXTERNAL_LINK

    @Builder.Default
    @Column(nullable = false)
    private Integer viewCount = 0;            // Lượt xem trang chi tiết

    @Builder.Default
    @Column(nullable = false)
    private Integer uniqueVisitorCount = 0;   // Khách khác nhau (HyperLogLog trên Redis, ghi số ước lượng)

    @Builder.Default
    @Column(nullable = false)
    private Integer addToCartCount = 0;       // Lượt thêm vào giỏ từ trang này
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Cộng dồn bộ đếm từ Redis (job 5 phút) — UC-CAT-06 */
    public void accumulate(int views, int visitors, int carts) {
        this.viewCount += views;
        this.uniqueVisitorCount = Math.max(this.uniqueVisitorCount, visitors);
        this.addToCartCount += carts;
    }
}
