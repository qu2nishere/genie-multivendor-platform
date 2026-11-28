// Cổng đảo ngược: module vendor cho catalog biết thông tin shop cần khi đăng bán (plan C1 N3, D2, D23)
package com.genie.catalog.product.application;

import java.util.Optional;

/**
 * {@code vendor} đã phụ thuộc {@code catalog} nên catalog không gọi thẳng {@code ShopFacade} được (không vòng phụ thuộc —
 * {@code ModuleBoundaryTest}); {@code vendor/shop/application/VendorShopPorts} cài cổng này.
 */
public interface SellerShopPort {

    /**
     * @param shopId             {@code vendor_profiles.id} ({@code shop_id} trên API)
     * @param pickupConfigured   đã cấu hình kho lấy hàng & đăng ký GHN (UC-VND-05) — điều kiện gửi duyệt (D2)
     * @param pickupProvinceName tên tỉnh kho lấy hàng — ghi vào {@code products.ships_from} (chỉ hiển thị)
     * @param preOrderEnabled    shop cho phép đăng sản phẩm đặt trước (D23)
     */
    record ShopFacts(long shopId, String shopName, boolean pickupConfigured, String pickupProvinceName,
                     boolean preOrderEnabled) {
    }

    /** Shop của chủ shop {@code vendorUserId} (users.id); rỗng nếu tài khoản chưa có shop. */
    Optional<ShopFacts> factsOf(long vendorUserId);

    /** Xóa cache trang shop công khai (số sản phẩm, lưới sản phẩm) — gọi sau commit khi sản phẩm đổi trạng thái bán. */
    void evictShopPage(long shopId);
}
