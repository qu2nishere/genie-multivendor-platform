package com.genie.order.checkout.application;

import com.genie.auth.address.application.AddressFacade;
import com.genie.auth.address.application.AddressSnapshot;
import com.genie.catalog.product.application.VariantFacade;
import com.genie.catalog.product.application.VariantFacade.VariantLine;
import com.genie.common.error.InvalidRequestException;
import com.genie.order.cart.application.PurchaseRules;
import com.genie.order.cart.application.PurchaseRules.Reason;
import com.genie.order.cart.domain.CartItem;
import com.genie.order.cart.domain.CartItemRepository;
import com.genie.order.cart.domain.CartRepository;
import com.genie.order.checkout.application.CheckoutDiscountPolicy.Discounts;
import com.genie.order.checkout.application.CheckoutDiscountPolicy.Selection;
import com.genie.order.checkout.application.CheckoutDiscountPolicy.ShopAmount;
import com.genie.order.checkout.application.CheckoutDiscountPolicy.ShopDiscount;
import com.genie.shipping.fee.application.Parcel;
import com.genie.shipping.fee.application.ShippingFeeQuoter;
import com.genie.shipping.fee.application.ShippingQuote;
import com.genie.vendor.shop.application.ShopFacade;
import com.genie.vendor.shop.application.ShopFacade.ShopBuyInfo;
import com.genie.vendor.shop.application.ShopFacade.ShopMiniView;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Engine tính giá DÙNG CHUNG cho xem trước (API 6.8 — UC-ORD-04) và đặt hàng (6.10 — UC-ORD-07): nạp dòng giỏ của người
 * gọi, tách theo shop, tính tạm tính, phí ship (UC-ORD-06), giảm giá (cổng {@link CheckoutDiscountPolicy}) và tổng
 * (plan C2 3.3). Server luôn tính lại giá hiện tại — không tin giá client.
 */
@Component
public class CheckoutPricer {

    /** Tối đa số dòng một lần xem trước / đặt hàng (= giới hạn dòng giỏ, plan C2 D9). */
    public static final int MAX_LINES = 100;
    /** {@code unavailable_reason} riêng của 6.8 khi GHN không phục vụ tuyến (plan C2 D6). */
    public static final String SHIPPING_UNAVAILABLE = "SHIPPING_UNAVAILABLE";

    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final VariantFacade variants;
    private final ShopFacade shops;
    private final AddressFacade addresses;
    private final ShippingFeeQuoter shipping;
    private final CheckoutDiscountPolicy discounts;
    private final Clock clock;

    public CheckoutPricer(CartRepository carts, CartItemRepository cartItems, VariantFacade variants, ShopFacade shops,
                          AddressFacade addresses, ShippingFeeQuoter shipping, CheckoutDiscountPolicy discounts,
                          Clock clock) {
        this.carts = carts;
        this.cartItems = cartItems;
        this.variants = variants;
        this.shops = shops;
        this.addresses = addresses;
        this.shipping = shipping;
        this.discounts = discounts;
        this.clock = clock;
    }

    /** Một dòng đã tính giá. {@code line} = dữ liệu catalog lúc tính (snapshot đơn dùng lại). */
    public record PricedItem(long cartItemId, long variantId, String productName, String imageUrl, String variantName,
                             long unitPrice, int quantity, boolean isAvailable, String unavailableReason,
                             VariantLine line) {

        public long lineTotal() {
            return unitPrice * quantity;
        }
    }

    /**
     * Một đơn con (một shop). {@code shippingFee} NULL khi chưa có địa chỉ. {@code baseShippingFee} = phí trước freeship,
     * {@code freeShipThreshold} = ngưỡng freeship của shop (0 khi mọi món người bán trả ship) — web tính lại khi bỏ chọn dòng.
     */
    public record PricedShop(long vendorUserId, ShopBuyInfo shop, List<PricedItem> items, long subtotal,
                             Long shippingFee, Long baseShippingFee, Long freeShipThreshold, boolean freeByShop,
                             boolean estimatedFee, long shopDiscount, long platformDiscount, long freeshipDiscount,
                             long total) {

        public List<PricedItem> available() {
            return items.stream().filter(PricedItem::isAvailable).toList();
        }
    }

    public record Summary(long subtotal, long shippingTotal, long platformDiscount, long freeshipDiscount,
                          long shopDiscount, long coinDiscount, long grandTotal, int orderCount, int itemCount) {
    }

    public record Priced(AddressSnapshot address, List<PricedShop> shops, Discounts discounts, Summary summary) {

        /** Dòng không mua được (để đặt hàng trả lỗi đúng loại). */
        public List<PricedItem> unavailable() {
            return shops.stream().flatMap(s -> s.items().stream()).filter(i -> !i.isAvailable()).toList();
        }
    }

    /**
     * @param addressId NULL → địa chỉ mặc định; không có địa chỉ nào → {@code address = null}, phí ship = null
     * @throws InvalidRequestException {@code address_id} không thuộc người gọi / đã xóa; quá {@value #MAX_LINES} dòng
     */
    @Transactional(readOnly = true)
    public Priced price(long userId, Collection<Long> cartItemIds, Long addressId, Selection selection) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw InvalidRequestException.of("cart_item_ids", "REQUIRED", null, "Chọn ít nhất một sản phẩm.");
        }
        if (cartItemIds.size() > MAX_LINES) {
            throw InvalidRequestException.of("cart_item_ids", "TOO_MANY", cartItemIds.size(),
                    "Tối đa 100 sản phẩm mỗi lần.");
        }
        AddressSnapshot address = addressId == null ? addresses.defaultFor(userId).orElse(null)
                : addresses.snapshotForCheckout(userId, addressId).orElseThrow(() -> InvalidRequestException.of(
                        "address_id", "NOT_FOUND", addressId, "Địa chỉ giao hàng không hợp lệ."));

        // Chỉ dòng thuộc giỏ của người gọi; giữ thứ tự giỏ (mới thêm trước) — id lạ bị bỏ, không lộ dữ liệu người khác
        Set<Long> wanted = new LinkedHashSet<>(cartItemIds);
        List<CartItem> rows = carts.findIdByUserId(userId).map(cartItems::findByCart).orElse(List.of()).stream()
                .filter(r -> wanted.contains(r.getId())).toList();
        Map<Long, VariantLine> lines = variants.lines(rows.stream().map(CartItem::getProductVariantId).toList());
        Set<Long> vendorIds = new LinkedHashSet<>();
        lines.values().forEach(l -> vendorIds.add(l.vendorUserId()));
        Map<Long, ShopBuyInfo> shopInfo = shops.buyInfo(vendorIds, OffsetDateTime.now(clock));

        Map<Long, List<PricedItem>> byVendor = new LinkedHashMap<>();
        for (CartItem r : rows) {
            VariantLine l = lines.get(r.getProductVariantId());
            if (l == null) {
                continue;
            }
            Reason reason = PurchaseRules.reasonFor(l, shopInfo.get(l.vendorUserId()), userId);
            if (reason == null && r.getQuantity() > l.available()) {
                reason = Reason.OUT_OF_STOCK;
            }
            byVendor.computeIfAbsent(l.vendorUserId(), k -> new ArrayList<>()).add(new PricedItem(r.getId(),
                    l.variantId(), l.productName(), l.imageUrl(), l.variantName(), l.unitPrice(), r.getQuantity(),
                    reason == null, PurchaseRules.publicReason(reason), l));
        }

        // Phí ship từng shop trên các dòng mua được
        List<PricedShop> draft = new ArrayList<>();
        for (Map.Entry<Long, List<PricedItem>> e : byVendor.entrySet()) {
            ShopBuyInfo s = shopInfo.get(e.getKey());
            List<PricedItem> items = e.getValue();
            List<PricedItem> ok = items.stream().filter(PricedItem::isAvailable).toList();
            long subtotal = ok.stream().mapToLong(PricedItem::lineTotal).sum();
            Long fee = null;
            Long base = null;
            Long threshold = null;
            boolean free = false;
            boolean estimated = false;
            if (address != null && !ok.isEmpty()) {
                ShippingQuote q = shipping.quote(parcel(e.getKey(), s, address, ok, subtotal));
                if (q.unavailable()) {
                    items = items.stream().map(i -> i.isAvailable() ? new PricedItem(i.cartItemId(), i.variantId(),
                            i.productName(), i.imageUrl(), i.variantName(), i.unitPrice(), i.quantity(), false,
                            SHIPPING_UNAVAILABLE, i.line()) : i).toList();
                    subtotal = 0;
                    fee = 0L;
                } else {
                    fee = q.customerFee();
                    base = q.carrierFee();
                    threshold = q.freeShipThreshold();
                    free = q.freeByShop();
                    estimated = q.estimated();
                }
            } else if (address != null) {
                fee = 0L;
            }
            draft.add(new PricedShop(e.getKey(), s, items, subtotal, fee, base, threshold, free, estimated, 0, 0, 0, 0));
        }

        Discounts applied = discounts.apply(userId, draft.stream().map(p -> new ShopAmount(p.vendorUserId(),
                p.subtotal(), p.shippingFee() == null ? 0 : p.shippingFee())).toList(),
                selection == null ? Selection.EMPTY : selection);
        List<PricedShop> priced = new ArrayList<>();
        long sub = 0, ship = 0, platform = 0, freeship = 0, shopDisc = 0;
        int orders = 0, itemCount = 0;
        for (PricedShop p : draft) {
            ShopDiscount d = applied.forShop(p.vendorUserId());
            long fee = p.shippingFee() == null ? 0 : p.shippingFee();
            long total = Math.max(0, p.subtotal() + fee - d.shopDiscount() - d.platformDiscount() - d.freeshipDiscount());
            priced.add(new PricedShop(p.vendorUserId(), p.shop(), p.items(), p.subtotal(), p.shippingFee(),
                    p.baseShippingFee(), p.freeShipThreshold(), p.freeByShop(), p.estimatedFee(), d.shopDiscount(),
                    d.platformDiscount(), d.freeshipDiscount(), total));
            int n = p.available().size();
            if (n > 0) {
                orders++;
                itemCount += n;
            }
            sub += p.subtotal();
            ship += fee;
            platform += d.platformDiscount();
            freeship += d.freeshipDiscount();
            shopDisc += d.shopDiscount();
        }
        long coin = applied.coinDiscount();
        long grand = Math.max(0, sub + ship - platform - freeship - shopDisc - coin);
        return new Priced(address, priced, applied,
                new Summary(sub, ship, platform, freeship, shopDisc, coin, grand, orders, itemCount));
    }

    /** Kiện của một shop: tổng gram, kích thước lớn nhất, kho lấy hàng, người bán trả ship cho mọi món? (D7). */
    public static Parcel parcel(long vendorUserId, ShopBuyInfo s, AddressSnapshot to, List<PricedItem> items, long subtotal) {
        int grams = items.stream().mapToInt(i -> i.line().weightGrams() * i.quantity()).sum();
        Integer length = max(items.stream().map(i -> i.line().lengthCm()).toList());
        Integer width = max(items.stream().map(i -> i.line().widthCm()).toList());
        Integer height = max(items.stream().map(i -> i.line().heightCm()).toList());
        boolean sellerPays = items.stream().allMatch(i -> i.line().sellerPaysShipping());
        boolean hasPickup = s != null && s.pickupProvinceId() != null;
        return new Parcel(vendorUserId, hasPickup ? s.pickupProvinceId() : null,
                hasPickup ? s.pickupDistrictId() : null, hasPickup ? s.pickupWardCode() : null,
                s == null ? null : s.ghnShopId(), to.provinceId(), null, to.wardCode(), grams, length, width, height,
                subtotal, sellerPays);
    }

    private static Integer max(List<Integer> values) {
        return values.stream().filter(Objects::nonNull).max(Integer::compare).orElse(null);
    }

    /** Shop mini cho phản hồi (shop không còn hồ sơ — dữ liệu hỏng — thì trả khung rỗng). */
    public static ShopMiniView mini(PricedShop p) {
        return p.shop() == null ? new ShopMiniView(0, "", null, false) : p.shop().mini();
    }
}
