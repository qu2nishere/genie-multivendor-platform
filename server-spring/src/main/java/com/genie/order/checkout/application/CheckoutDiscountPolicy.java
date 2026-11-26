package com.genie.order.checkout.application;

import java.util.List;

/**
 * Cổng giảm giá của checkout — voucher Shop / Sàn / Freeship và Điểm Genie (API 6.8; plan C2 D2). Chuỗi C2 chưa áp giảm
 * giá: bản mặc định ({@link CheckoutPorts}) trả {@link Discounts#NONE}; chuỗi C6 (UC-PRO-04, PRO-10) cài bản thật.
 * Cùng một cổng dùng cho xem trước (6.8) và đặt hàng (6.10) qua {@link CheckoutPricer}.
 */
public interface CheckoutDiscountPolicy {

    /** Tiền hàng của một shop trước giảm giá. */
    record ShopAmount(long vendorUserId, long subtotal, long shippingFee) {
    }

    /** Lựa chọn voucher của khách (API 6.8 {@code vouchers}); C2 nhận nhưng bỏ qua. */
    record Selection(Long platformVoucherId, Long freeshipVoucherId, List<ShopVoucher> shopVouchers, boolean useCoins) {

        public static final Selection EMPTY = new Selection(null, null, List.of(), false);
    }

    record ShopVoucher(long shopId, long voucherId) {
    }

    /** Khoản giảm theo shop (cùng thứ tự với đầu vào) + tổng Điểm dùng. {@code applied = false} → trả {@code null} ở API. */
    record Discounts(boolean applied, List<ShopDiscount> shops, long coinDiscount) {

        public static final Discounts NONE = new Discounts(false, List.of(), 0);

        public ShopDiscount forShop(long vendorUserId) {
            return shops.stream().filter(s -> s.vendorUserId() == vendorUserId).findFirst()
                    .orElse(new ShopDiscount(vendorUserId, 0, 0, 0));
        }
    }

    record ShopDiscount(long vendorUserId, long shopDiscount, long platformDiscount, long freeshipDiscount) {
    }

    Discounts apply(long userId, List<ShopAmount> shops, Selection selection);
}
