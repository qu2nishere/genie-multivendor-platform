package com.genie.promotion.voucher.application;

import com.genie.common.domain.FundingParty;
import com.genie.promotion.voucher.domain.VoucherTerms;
import java.math.BigDecimal;
import java.util.Set;

/** Voucher đã kiểm + chuẩn hóa (plan 2026-11-26 voucher V02.04) — điều kiện + 4 tập phạm vi + nhà tài trợ (sàn). */
public record VoucherDraft(VoucherTerms terms, Set<Long> categoryIds, Set<Long> productIds,
                           Set<Long> excludedCategoryIds, Set<Long> excludedProductIds, FundingParty sponsor,
                           BigDecimal platformSharePercent) {
}
