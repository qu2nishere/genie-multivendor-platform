package com.genie.common.audit;

/**
 * Enum AuditAction — ENTITY.md Phụ lục A. ← SystemLog.actionType (mở rộng: MFA_ENABLED, MFA_DISABLED, MFA_CHALLENGE_FAILED, PAYOUT_REVIEWED)
 * Từ {@code PASSWORD_CHANGED} trở đi: bổ sung cho phân hệ AUTH (plan AUTH D20, V14__auth_additions.sql) — giữ đồng bộ với
 * {@code ck_system_logs_action_type}.
 */
public enum AuditAction {                                                                      // ← SystemLog.actionType (mở rộng: MFA_ENABLED, MFA_DISABLED, MFA_CHALLENGE_FAILED, PAYOUT_REVIEWED)
    LOGIN_SUCCESS, LOGIN_FAILED, ACCOUNT_LOCKED, ACCOUNT_SUSPENDED, ACCOUNT_REACTIVATED,
    KYC_SUBMITTED, KYC_APPROVED, KYC_REJECTED, ORDER_STATUS_CHANGED, ESCROW_RELEASED,
    PAYOUT_APPROVED, PAYOUT_REJECTED, REFUND_PROCESSED, REVIEW_HIDDEN, COMMISSION_UPDATED,
    PERMISSION_GRANTED, PERMISSION_REVOKED, RBAC_ROLE_CHANGED, STAFF_INVITED, PII_ANONYMIZED,
    ACCESS_DENIED, PRODUCT_MODERATED,                 // lần truy cập bị chặn quyền (màn Nhật ký), kết quả duyệt sản phẩm
    // ===== AUTH (plan AUTH D20) =====
    PASSWORD_CHANGED, PASSWORD_RESET, EMAIL_CHANGED, OAUTH_LINKED, OAUTH_UNLINKED, SESSION_REVOKED,
    ACCOUNT_DELETION_REQUESTED, ACCOUNT_DELETION_CANCELLED,
    // ===== Banner trang chủ (plan 2026-11-16 banner D1, V18) — CHỈ CÒN cho bản ghi cũ (details.op); code mới ghi 5 mã bên dưới =====
    BANNER_CHANGED,
    // ===== Mở shop & KYC (plan 2026-11-16 Mở shop & KYC D3, V20) =====
    KYC_SUPPLEMENT_REQUESTED, KYC_REQUEUED, KYC_DOCUMENT_VIEWED, PAYOUT_ACCOUNT_CHANGED,
    // ===== Banner theo động từ cụ thể (plan 2026-11-22 nhật ký banner N1, V21) — sửa kèm details.changes {trường: {from, to}} =====
    BANNER_CREATED, BANNER_UPDATED, BANNER_ACTIVATED, BANNER_DEACTIVATED, BANNER_DELETED;

    /**
     * Nhóm hiển thị trên màn Nhật ký hệ thống (API_SPEC 16; plan nhật ký kiểm toán D2). {@code switch} không có
     * {@code default}: thêm giá trị mới mà quên phân nhóm thì trình biên dịch báo lỗi.
     */
    public AuditCategory category() {
        return switch (this) {
            case LOGIN_SUCCESS, LOGIN_FAILED, PASSWORD_CHANGED, PASSWORD_RESET, EMAIL_CHANGED, OAUTH_LINKED,
                 OAUTH_UNLINKED, SESSION_REVOKED, ACCOUNT_DELETION_REQUESTED, ACCOUNT_DELETION_CANCELLED -> AuditCategory.AUTH;
            case KYC_SUBMITTED, KYC_APPROVED, KYC_REJECTED, KYC_SUPPLEMENT_REQUESTED, KYC_REQUEUED,
                 KYC_DOCUMENT_VIEWED -> AuditCategory.KYC;
            case ESCROW_RELEASED, PAYOUT_APPROVED, PAYOUT_REJECTED, REFUND_PROCESSED, ORDER_STATUS_CHANGED,
                 PAYOUT_ACCOUNT_CHANGED ->
                    AuditCategory.FINANCE;
            case REVIEW_HIDDEN, PRODUCT_MODERATED -> AuditCategory.MODERATION;
            case ACCOUNT_LOCKED, ACCOUNT_SUSPENDED, ACCOUNT_REACTIVATED, ACCESS_DENIED, PII_ANONYMIZED ->
                    AuditCategory.SECURITY;
            case COMMISSION_UPDATED, PERMISSION_GRANTED, PERMISSION_REVOKED, RBAC_ROLE_CHANGED, STAFF_INVITED,
                 BANNER_CHANGED, BANNER_CREATED, BANNER_UPDATED, BANNER_ACTIVATED, BANNER_DEACTIVATED, BANNER_DELETED ->
                    AuditCategory.CONFIG;
        };
    }
}
