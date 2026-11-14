package com.genie.auth.profile.domain;

/**
 * Trạng thái tài khoản — User.status (ENTITY.md Phụ lục A, nhóm 1).
 */
public enum AccountStatus {
    UNVERIFIED, ACTIVE, LOCKED, SUSPENDED, ANONYMIZED, DELETED   // ANONYMIZED: xóa tài khoản — UC-AUTH-10
}
