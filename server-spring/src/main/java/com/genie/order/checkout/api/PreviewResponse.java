package com.genie.order.checkout.api;

import com.genie.auth.address.application.AddressSnapshot;
import com.genie.order.checkout.application.CheckoutPricer;
import com.genie.order.checkout.application.CheckoutPricer.Priced;
import com.genie.order.checkout.application.CheckoutPricer.PricedItem;
import com.genie.order.checkout.application.CheckoutPricer.PricedShop;
import com.genie.vendor.shop.application.ShopFacade.ShopMiniView;
import java.util.List;

/**
 * Phản hồi {@code POST /me/checkout/preview} (API_SPEC 6.8; plan C2 O06.04). Trước chuỗi C6: các khoản giảm = 0,
 * {@code vouchers_applied} / {@code best_selection} / {@code coins} = {@code null} (D2).
 */
public record PreviewResponse(Address address, List<Shop> shops, Object vouchersApplied, Object bestSelection,
                              Object coins, Summary summary) {

    /** {@code $Address} rút gọn từ ảnh chụp sổ địa chỉ (địa giới 2 cấp). */
    public record Address(long id, String recipientName, String phoneNumber, Region province, Ward ward,
                          String addressLine, String deliveryNote, String fullAddress) {
    }

    public record Region(int id, String name) {
    }

    public record Ward(String code, String name) {
    }

    public record Item(long cartItemId, long variantId, String productName, String imageUrl, String variantName,
                       long unitPrice, int quantity, long platformDiscount, boolean isAvailable,
                       String unavailableReason) {
    }

    public record Shop(ShopMiniView shop, List<Item> items, long subtotal, Long shippingFee, Long baseShippingFee,
                       Long freeShipThreshold, boolean isFreeShippingByShop, boolean isEstimatedFee,
                       Object shopVoucher, long freeshipDiscount, long platformDiscount, long total) {
    }

    public record Summary(long subtotal, long shippingTotal, long platformDiscount, long freeshipDiscount,
                          long shopDiscount, long coinDiscount, long grandTotal, int orderCount, int itemCount) {
    }

    static PreviewResponse of(Priced p) {
        List<Shop> shops = p.shops().stream().map(PreviewResponse::shop).toList();
        CheckoutPricer.Summary s = p.summary();
        return new PreviewResponse(address(p.address()), shops, null, null, null,
                new Summary(s.subtotal(), s.shippingTotal(), s.platformDiscount(), s.freeshipDiscount(),
                        s.shopDiscount(), s.coinDiscount(), s.grandTotal(), s.orderCount(), s.itemCount()));
    }

    static Address address(AddressSnapshot a) {
        if (a == null) {
            return null;
        }
        return new Address(a.addressId(), a.recipientName(), a.phoneNumber(), new Region(a.provinceId(), a.province()),
                new Ward(a.wardCode(), a.ward()), a.addressLine(), a.deliveryNote(), a.fullAddress());
    }

    private static Shop shop(PricedShop s) {
        List<Item> items = s.items().stream().map(PreviewResponse::item).toList();
        return new Shop(CheckoutPricer.mini(s), items, s.subtotal(), s.shippingFee(), s.baseShippingFee(),
                s.freeShipThreshold(), s.freeByShop(), s.estimatedFee(), null, s.freeshipDiscount(),
                s.platformDiscount(), s.total());
    }

    private static Item item(PricedItem i) {
        return new Item(i.cartItemId(), i.variantId(), i.productName(), i.imageUrl(), i.variantName(), i.unitPrice(),
                i.quantity(), 0, i.isAvailable(), i.unavailableReason());
    }
}
