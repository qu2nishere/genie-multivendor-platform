package com.genie.auth.address.domain;

/**
 * Loại địa chỉ — Address.label (ENTITY.md Phụ lục A, nhóm 1). TLCN v1.3: Nhà riêng / Văn phòng; plan AUTH D24
 * (2026-11-18, migration {@code V15}): thêm Trường học, Chung cư, Khác.
 */
public enum AddressLabel {
    /** Nhà riêng */
    HOME,
    /** Văn phòng */
    OFFICE,
    /** Trường học */
    SCHOOL,
    /** Chung cư */
    APARTMENT,
    /** Khác */
    OTHER
}
