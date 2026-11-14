package com.genie.payment.wallet.domain;

/**
 * Enum WalletTransactionType — ENTITY.md Phụ lục A. ← WalletTransaction.type (mở rộng: CREDIT_TOPUP, DEBIT_PURCHASE, DEBIT_AD_SPEND, DEBIT_SUBSCRIPTION_FEE, DEBIT_TAX_WITHHOLDING, DEBIT_COSMETIC_PURCHASE)
 */
public enum WalletTransactionType {                                                            // ← WalletTransaction.type (mở rộng: CREDIT_TOPUP, DEBIT_PURCHASE, DEBIT_AD_SPEND, DEBIT_SUBSCRIPTION_FEE, DEBIT_TAX_WITHHOLDING, DEBIT_COSMETIC_PURCHASE)
    CREDIT_ESCROW_RELEASE, DEBIT_REFUND, DEBIT_WITHDRAWAL, CREDIT_ADJUSTMENT, DEBIT_ADJUSTMENT
}
