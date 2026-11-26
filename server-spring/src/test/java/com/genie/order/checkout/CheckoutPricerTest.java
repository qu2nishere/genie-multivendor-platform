package com.genie.order.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.auth.address.application.AddressSnapshot;
import com.genie.catalog.product.application.VariantFacade.VariantLine;
import com.genie.order.checkout.application.CheckoutPricer;
import com.genie.order.checkout.application.CheckoutPricer.PricedItem;
import com.genie.shipping.fee.application.Parcel;
import com.genie.vendor.shop.application.ShopFacade.ShopBuyInfo;
import com.genie.vendor.shop.application.ShopFacade.ShopMiniView;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Kiện hàng của một shop: khối lượng × số lượng, kích thước lớn nhất, người bán trả ship (plan C2 D7, O06.05). */
class CheckoutPricerTest {

    private static final AddressSnapshot TO = new AddressSnapshot(1, "A", "0901", "25 Nguyễn Huệ", "26740", "P",
            79, "HCM", null);
    private static final ShopBuyInfo SHOP = new ShopBuyInfo(9, 5, new ShopMiniView(9, "S", null, false), false, 1, 1442,
            "20101", 99, true);

    private static PricedItem item(int grams, Integer len, boolean sellerPays, int qty) {
        VariantLine l = new VariantLine(1, 1, "sp", "SP", "SKU", "Đen", null, 100_000, null, 10, true, 5, null, null,
                grams, len, null, 3, sellerPays);
        return new PricedItem(1, 1, "SP", null, "Đen", 100_000, qty, true, null, l);
    }

    @Test
    void parcelSumsWeightAndTakesLargestDimensions() {
        Parcel p = CheckoutPricer.parcel(5, SHOP, TO, List.of(item(300, 20, false, 2), item(500, 35, true, 1)), 300_000);
        assertThat(p.weightGrams()).isEqualTo(1100);
        assertThat(p.lengthCm()).isEqualTo(35);
        assertThat(p.widthCm()).isNull();
        assertThat(p.heightCm()).isEqualTo(3);
        assertThat(p.allSellerPays()).isFalse();
        assertThat(p.fromProvinceId()).isEqualTo(1);
        assertThat(p.toProvinceId()).isEqualTo(79);
        assertThat(p.toWardCode()).isEqualTo("26740");   // mã phường mới, không cần quận (D4, O11.01)
        assertThat(p.toName()).isEqualTo("A");
        assertThat(p.toAddress()).isEqualTo("25 Nguyễn Huệ, P, HCM");
    }

    @Test
    void sellerPaysOnlyWhenEveryItemIsSellerPaid() {
        assertThat(CheckoutPricer.parcel(5, SHOP, TO, List.of(item(100, null, true, 1)), 1).allSellerPays()).isTrue();
        Parcel noShop = CheckoutPricer.parcel(5, null, TO, List.of(item(100, null, true, 1)), 1);
        assertThat(noShop.fromProvinceId()).isNull();
        assertThat(noShop.ghnShopId()).isNull();
    }
}
