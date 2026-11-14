package com.genie.payment.escrow.domain;

/**
 * Enum FundingSource — ENTITY.md Phụ lục A. ← EscrowLedger.fundingSource (ONLINE_GATEWAY gồm VNPay và VietQR qua SePay; mở rộng: WALLET, SPLIT)
 */
public enum FundingSource { ONLINE_GATEWAY, COD_CASH }                                         // ← EscrowLedger.fundingSource (ONLINE_GATEWAY gồm VNPay và VietQR qua SePay; mở rộng: WALLET, SPLIT)
