package com.genie.promotion.coin.domain;

/**
 * Enum CoinTransactionType — ENTITY.md Phụ lục A. ← CoinTransaction.type (mở rộng: COSMETIC_REDEEM, VENDOR_REWARD)
 */
public enum CoinTransactionType { CHECK_IN, ORDER_CASHBACK, REVIEW_BONUS, USED_AT_CHECKOUT, ORDER_REFUND, VOUCHER_REDEEM, EXPIRED } // ← CoinTransaction.type (EXPIRED: lô hết hạn — V16, plan 2026-11-16 Điểm Genie D5; mở rộng: COSMETIC_REDEEM, VENDOR_REWARD)
