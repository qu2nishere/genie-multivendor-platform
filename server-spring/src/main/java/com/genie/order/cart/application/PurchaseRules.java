package com.genie.order.cart.application;

import com.genie.catalog.product.application.VariantFacade.VariantLine;
import com.genie.vendor.shop.application.ShopFacade.ShopBuyInfo;

/**
 * Luật "mua được" của một dòng (plan C2 mục 8.5, D16) — dùng chung cho giỏ (6.1), xem trước (6.8) và đặt hàng (6.10):
 * sản phẩm {@code PUBLISHED} + biến thể chưa bỏ + shop không tạm nghỉ + người mua không phải chủ shop + còn tồn.
 */
public final class PurchaseRules {

    /** {@code unavailable_reason} (API 6.1) + {@code OWN_PRODUCT}, {@code CART_FULL} chỉ dùng ở {@code skipped} (6.6, 6.7). */
    public enum Reason { OUT_OF_STOCK, SHOP_ON_VACATION, PRODUCT_UNAVAILABLE, OWN_PRODUCT, CART_FULL }

    private PurchaseRules() {
    }

    /**
     * Lý do không mua được; {@code null} = mua được ít nhất 1 cái. Chủ shop xem dòng của shop mình (dữ liệu cũ) →
     * {@link Reason#OWN_PRODUCT}; ở phản hồi giỏ đổi thành {@code PRODUCT_UNAVAILABLE} ({@link #publicReason}).
     */
    public static Reason reasonFor(VariantLine line, ShopBuyInfo shop, long buyerId) {
        if (line == null || !line.purchasable()) {
            return Reason.PRODUCT_UNAVAILABLE;
        }
        if (line.vendorUserId() == buyerId) {
            return Reason.OWN_PRODUCT;
        }
        if (shop != null && shop.onVacation()) {
            return Reason.SHOP_ON_VACATION;
        }
        if (line.available() <= 0) {
            return Reason.OUT_OF_STOCK;
        }
        return null;
    }

    /** Giá trị trả ở {@code unavailable_reason} của giỏ / xem trước (enum API 6.1 không có OWN_PRODUCT). */
    public static String publicReason(Reason r) {
        if (r == null) {
            return null;
        }
        return r == Reason.OWN_PRODUCT || r == Reason.CART_FULL ? Reason.PRODUCT_UNAVAILABLE.name() : r.name();
    }
}
