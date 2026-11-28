package com.genie.promotion.voucher.application;

/**
 * Voucher vừa tạo / sửa / tắt / xóa (plan 2026-11-26 voucher N5, V03.04) — listener xóa cache SAU commit.
 *
 * @param shopVoucher voucher SHOP (ảnh hưởng bộ lọc "Shop có voucher" của tìm kiếm)
 */
public record VoucherChangedEvent(long voucherId, boolean shopVoucher) {
}
