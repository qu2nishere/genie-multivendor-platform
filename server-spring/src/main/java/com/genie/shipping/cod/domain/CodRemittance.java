package com.genie.shipping.cod.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import com.genie.shipping.shipment.domain.CarrierType;

/**
 * CodRemittance — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "cod_remittances")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CodRemittance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false, unique = true, length = 30)
    private String remittanceCode;            // Mã phiên hiển thị: "COD-20260612"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CarrierType carrierType;          // Hãng chuyển tiền thu hộ về sàn: GHN / GHTK / VIETTEL_POST / JNT_EXPRESS

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal receivedAmount;        // Số tiền thực nhận vào tài khoản sàn (SePay báo)

    @Column(nullable = false, unique = true, length = 100)
    private String bankReference;             // Mã giao dịch ngân hàng do SePay gửi — UNIQUE để webhook idempotent

    @Column(nullable = false)
    private OffsetDateTime receivedAt;         // Thời điểm tiền vào tài khoản sàn

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CodRemittanceStatus status = CodRemittanceStatus.WAITING; // WAITING (chờ bảng kê) / DONE (đã đối soát)

    @Column(length = 255)
    private String statementFileName;         // NULLABLE — Tên tệp bảng kê CSV đã đối chiếu

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String linesJson;                 // NULLABLE — Kết quả đối chiếu từng dòng [{trackingCode, orderId, amount, fee, result}]

    private Long reconciledByStaffId;         // NULLABLE — FK mềm → users.id, nhân viên xác nhận (không đặt FK cứng, như kycVerifiedBy)

    private OffsetDateTime reconciledAt;       // NULLABLE — Thời điểm xác nhận đối soát

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm SePay báo tiền vào, tạo phiên
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Xác nhận đối soát khi bảng kê cân với số tiền nhận; service fund các EscrowLedger khớp — UC-LOG-03 */
    public void confirm(String fileName, String lines, Long staffId, OffsetDateTime now) {
        if (this.status != CodRemittanceStatus.WAITING) throw new IllegalStateException("Phiên đã đối soát");
        this.statementFileName = fileName;
        this.linesJson = lines;
        this.reconciledByStaffId = staffId;
        this.reconciledAt = now;
        this.status = CodRemittanceStatus.DONE;
    }
}
