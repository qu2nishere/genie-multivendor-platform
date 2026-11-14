package com.genie.auth.profile.domain;

/**
 * Giới tính — CustomerProfile.gender (ENTITY.md Phụ lục A, nhóm 1).
 * PREFER_NOT_TO_SAY khác NULL: người dùng đã chủ động chọn không tiết lộ.
 */
public enum Gender {
    MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
}
