package com.genie.common.domain;

/**
 * Bên tài trợ — Voucher.sponsorType, OrderDiscount.fundedBy (ENTITY.md Phụ lục A, nhóm 4).
 * CO_SPONSOR: sàn + shop cùng chịu theo Voucher.platformSharePercent.
 */
public enum FundingParty {
    PLATFORM, VENDOR, CO_SPONSOR
}
