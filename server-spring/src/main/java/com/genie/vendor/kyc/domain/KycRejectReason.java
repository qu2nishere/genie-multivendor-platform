package com.genie.vendor.kyc.domain;

/**
 * Mã lý do nhân viên từ chối hồ sơ KYC (API_SPEC 16.4; web {@code admin-kyc/domain/kyc.rules.ts REJECT_REASONS}; plan Mở shop
 * & KYC K06.01) — nhãn tiếng Việt ghép vào {@code kyc_rejection_reason} và email cho người bán. {@code OTHER} bắt buộc ghi chú.
 */
public enum KycRejectReason {
    BLURRY_DOCUMENT("Ảnh giấy tờ mờ, không đọc được"),
    INFO_MISMATCH("Thông tin kê khai không khớp với giấy tờ"),
    EXPIRED_DOCUMENT("Giấy tờ đã hết hạn"),
    SUSPECTED_FORGERY("Nghi ngờ giấy tờ bị chỉnh sửa / giả mạo"),
    MISSING_LICENSE("Thiếu giấy phép kinh doanh hợp lệ"),
    OTHER("Lý do khác");

    private final String label;

    KycRejectReason(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Lý do lưu vào hồ sơ: {@code "Nhãn: ghi chú"} (hoặc chỉ ghi chú với OTHER). */
    public String reasonWith(String note) {
        String n = note == null ? "" : note.trim();
        if (this == OTHER) {
            return n;
        }
        return n.isEmpty() ? label : label + ": " + n;
    }
}
