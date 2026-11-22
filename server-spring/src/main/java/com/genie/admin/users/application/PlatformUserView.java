// Dòng người dùng của API 11.13 GET /admin/users (UC-ADM-03 — plan B1 R05)
package com.genie.admin.users.application;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * {@code code = "U-" + id}. {@code role} ∈ {@code CUSTOMER, VENDOR}. {@code status} ∈ {@code ACTIVE, LOCKED,
 * SUSPENDED}: {@code SUSPENDED} = khóa có thời hạn, {@code LOCKED} = khóa vô thời hạn hoặc đang khóa tạm vì sai mật
 * khẩu. {@code suspension} chỉ có khi bị khóa bởi quản trị.
 */
public record PlatformUserView(long id, String code, String fullName,
                               @JsonInclude(JsonInclude.Include.ALWAYS) String shopName,
                               String email,
                               @JsonInclude(JsonInclude.Include.ALWAYS) String phoneNumber,
                               String role, int orderCount, int warningCount, String status,
                               @JsonInclude(JsonInclude.Include.ALWAYS) UserSuspensionView suspension) {
}
