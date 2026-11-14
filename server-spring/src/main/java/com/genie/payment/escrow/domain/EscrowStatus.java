package com.genie.payment.escrow.domain;

/**
 * Enum EscrowStatus — ENTITY.md Phụ lục A. ← EscrowLedger.status
 */
public enum EscrowStatus {                                                                     // ← EscrowLedger.status
    PENDING_FUNDING,  // Đã tạo record nhưng chưa nhận đủ tiền (COD: GHN chưa đối soát)
    HELD,             // Đã fund đầy đủ, đang giữ an toàn
    FROZEN,           // Admin chủ động đóng băng (nghi vấn gian lận, vi phạm sàn)
    RELEASED,         // Đã giải ngân cho vendor
    REFUNDED,         // Đã hoàn tiền cho khách
    DISPUTED          // Tự động khóa do phát sinh yêu cầu đổi trả / khiếu nại (RefundRequest)
}
