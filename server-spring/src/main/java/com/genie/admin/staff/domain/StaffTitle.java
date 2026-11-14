package com.genie.admin.staff.domain;

import static com.genie.admin.rbac.domain.StaffPermissionEnum.*;

import com.genie.admin.rbac.domain.StaffPermissionEnum;
import java.util.Set;

/**
 * Enum StaffTitle — ENTITY.md Phụ lục A. ← AdminProfile.staffTitle (3 chức danh, mỗi chức danh mang bộ quyền mặc định; mở rộng: KYC_OFFICER, MARKETING_OFFICER, DISPATCHER, RISK_ANALYST, COMPLIANCE_OFFICER, B2B_CREDIT_OFFICER, TECH_OPS)
 */
public enum StaffTitle {                                                                       // ← AdminProfile.staffTitle (3 chức danh, mỗi chức danh mang bộ quyền mặc định; mở rộng: KYC_OFFICER, MARKETING_OFFICER, DISPATCHER, RISK_ANALYST, COMPLIANCE_OFFICER, B2B_CREDIT_OFFICER, TECH_OPS)
    MODERATOR(VIEW_DASHBOARD, REVIEW_KYC, MANAGE_CATALOG, MODERATE_REVIEW),
    CS_SPECIALIST(VIEW_DASHBOARD, HANDLE_DISPUTE, MODERATE_REVIEW),
    FINANCE_OFFICER(VIEW_DASHBOARD, PROCESS_PAYOUT, UPDATE_COMMISSION, VIEW_FINANCE, MANAGE_VOUCHER);
    private final Set<StaffPermissionEnum> defaults;
    StaffTitle(StaffPermissionEnum... p) { this.defaults = Set.of(p); }
    public Set<StaffPermissionEnum> defaultPermissions() { return defaults; }   // MANAGE_RBAC không thuộc mẫu nào
}
