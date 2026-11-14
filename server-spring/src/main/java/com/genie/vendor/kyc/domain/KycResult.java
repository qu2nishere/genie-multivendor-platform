package com.genie.vendor.kyc.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import com.genie.vendor.shop.domain.VendorProfile;

/**
 * KycResult — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "kyc_results")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class KycResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "vendor_profile_id", unique = true, nullable = false)
    private Long vendorProfileId;             // FK → vendor_profiles.id, UNIQUE (mỗi shop 1 bản kết quả; TLCN chỉ KYC người bán)

    // ===== Dữ liệu bóc tách: QR mặt trước (nguồn chính) + MRZ mặt sau (đối chiếu) =====
    @Column(length = 20)
    private String extractedIdNumber;         // NULLABLE — Số CCCD 12 số (từ QR; MRZ chứa số này ở trường tùy chọn dòng 1)

    @Column(length = 255)
    private String extractedFullName;         // NULLABLE — Họ tên có dấu (từ QR; MRZ dòng 3 là bản không dấu để đối chiếu)

    @Column(length = 20)
    private String extractedDob;              // NULLABLE — Ngày sinh (từ QR, dạng ddMMyyyy; lưu String để giữ dữ liệu thô)

    @Column(length = 10)
    private String extractedGender;           // NULLABLE — Giới tính "Nam"/"Nữ" (từ QR; MRZ ghi M/F)

    @Column(length = 100)
    private String extractedNationality;      // NULLABLE — Quốc tịch (từ MRZ, mã VNM)

    @Column(columnDefinition = "TEXT")
    private String extractedAddress;          // NULLABLE — Nơi thường trú (từ QR)

    @Column(length = 20)
    private String extractedIssueDate;        // NULLABLE — Ngày cấp (từ QR)

    @Column(length = 20)
    private String extractedExpiryDate;       // NULLABLE — Ngày hết hạn (từ MRZ dòng 2, YYMMDD)

    @Column(length = 255)
    private String extractedIssuer;           // NULLABLE — Nơi cấp; QR/MRZ không chứa, chỉ có khi người duyệt nhập tay

    // ===== Tầng 0 — kiểm tra dữ liệu (vài chục ms, không cần GPU) =====
    private Boolean qrCodeDetected;           // NULLABLE — Giải mã được QR mặt trước (thẻ gắn chip / thẻ căn cước mới đều có)

    private Boolean mrzValid;                 // NULLABLE — Đọc được 3 dòng MRZ mặt sau và đúng toàn bộ check digit ICAO 9303 (trọng số 7-3-1)

    private Boolean mrzQrMatch;               // NULLABLE — QR khớp MRZ: số thẻ, ngày sinh, giới tính, họ tên (bỏ dấu, viết hoa)

    private Boolean cccdNumberValid;          // NULLABLE — Số CCCD đúng cấu trúc (mã tỉnh 3 số + thế kỷ/giới tính + 2 số năm sinh) và khớp ngày sinh, giới tính

    private Boolean expiryRuleValid;          // NULLABLE — Ngày hết hạn khớp quy tắc theo tuổi đổi thẻ (14 / 25 / 40 / 60 tuổi hoặc không thời hạn — Luật Căn cước 2023)

    private Boolean duplicateIdFound;         // NULLABLE — Số CCCD đã gắn với một shop khác đang hoạt động

    // ===== Tầng 1 — sinh trắc (~1 giây, CPU ONNX) =====
    private Boolean faceMatchResult;          // NULLABLE — Khuôn mặt selfie khớp ảnh chân dung cắt từ mặt trước thẻ

    @Column(precision = 5, scale = 4)
    private BigDecimal faceMatchScore;        // NULLABLE — Điểm tương đồng cosine 0.0000 → 1.0000 (YuNet + SFace)

    @Column(precision = 5, scale = 4)
    private BigDecimal livenessScore;         // NULLABLE — Điểm người thật 0.0000 → 1.0000 (thử thách nháy mắt/quay đầu phía client + anti-spoof thụ động)

    private Boolean faceDuplicateFound;       // NULLABLE — Khuôn mặt trùng hồ sơ KYC của CCCD khác (MVP: so cosine với embedding đã lưu trong AI Worker — không dùng Vector DB)

    // ===== Điểm rủi ro tổng hợp =====
    private Integer riskScore;                // NULLABLE — Tổng điểm rủi ro 0..∞; mỗi tín hiệu cộng điểm thay vì chặn cứng

    @Column(columnDefinition = "TEXT")
    private String riskSignalsJson;           // NULLABLE — Mảng JSON các tín hiệu đã cộng điểm, vd [{"code":"FACE_SCORE_BORDERLINE","points":25}]

    @Builder.Default
    @Column(nullable = false)
    private Boolean sampledForReview = false; // true = hồ sơ điểm thấp bị lấy mẫu ngẫu nhiên (~5%) để người duyệt hậu kiểm

    // ===== Metadata =====
    @Column(columnDefinition = "TEXT")
    private String errors;                    // NULLABLE — Mảng JSON chứa thông báo lỗi của từng bước xử lý

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KycOverallResult overallStatus;   // Kết luận tổng: PASS / PARTIAL / FAIL (VARCHAR + CHECK constraint trên PostgreSQL 16)

    private OffsetDateTime processedAt;        // NULLABLE — Thời điểm AI xử lý xong

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo bản ghi

    // ===== QUAN HỆ =====
    @OneToOne(optional = false)
    @JoinColumn(name = "vendor_profile_id", insertable = false, updatable = false)
    private VendorProfile vendorProfile;      // Hồ sơ shop tương ứng

    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Kết quả cần nhân viên duyệt thủ công — UC-VND-03 */
    public boolean requiresManualReview() {
        return this.overallStatus == KycOverallResult.PARTIAL;
    }

    /** AI kết luận đạt — UC-VND-03 */
    public boolean isPassed() {
        return this.overallStatus == KycOverallResult.PASS;
    }

    /** Chốt điểm rủi ro và kết luận: ≥ rejectFrom → FAIL; ≥ autoApproveBelow hoặc bị lấy mẫu → PARTIAL; còn lại PASS (ngưỡng mặc định 30 / 70, cấu hình genie.kyc.risk.*) — UC-VND-03 */
    public KycOverallResult applyRiskScore(int score, String signalsJson, boolean sampled, int autoApproveBelow, int rejectFrom, OffsetDateTime now) {
        this.riskScore = score;
        this.riskSignalsJson = signalsJson;
        this.sampledForReview = sampled && score < autoApproveBelow;
        if (score >= rejectFrom) this.overallStatus = KycOverallResult.FAIL;
        else if (score >= autoApproveBelow || this.sampledForReview) this.overallStatus = KycOverallResult.PARTIAL;
        else this.overallStatus = KycOverallResult.PASS;
        this.processedAt = now;
        return this.overallStatus;
    }
}
