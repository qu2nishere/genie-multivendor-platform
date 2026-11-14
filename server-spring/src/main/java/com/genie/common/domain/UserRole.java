package com.genie.common.domain;

/**
 * Vai trò tài khoản — User.role (ENTITY.md Phụ lục A, nhóm 1).
 * Dùng chung nhiều module nên đặt ở shared kernel.
 */
public enum UserRole {
    CUSTOMER, VENDOR, STAFF, ADMIN   // mở rộng: VENDOR_STAFF, SHIPPER, B2B_BUYER
}
