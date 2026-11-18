package com.genie.catalog.wishlist.application;

import com.genie.catalog.product.domain.ProductRepository;
import com.genie.catalog.product.domain.ProductStatus;
import com.genie.catalog.wishlist.domain.UserWishlistRepository;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thích / bỏ thích sản phẩm — UC-CAT-08 (API_SPEC 3.10; plan wishlist W02). Cả hai idempotent (N2): thích lại không tạo
 * dòng thứ hai, bỏ thích thứ không có vẫn thành công.
 */
@Service
public class WishlistService {

    private final UserWishlistRepository wishlists;
    private final ProductRepository products;
    private final Clock clock;

    public WishlistService(UserWishlistRepository wishlists, ProductRepository products, Clock clock) {
        this.wishlists = wishlists;
        this.products = products;
        this.clock = clock;
    }

    /** Chỉ thích được sản phẩm đang bán ({@code PUBLISHED}); không có / không bán → {@code 404 GNE-CAT-4042} (D2). */
    @Transactional
    public void add(long userId, long productId) {
        boolean onSale = products.findStatus(productId).map(st -> st == ProductStatus.PUBLISHED).orElse(false);
        if (!onSale) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE);
        }
        wishlists.insertIfAbsent(userId, productId, OffsetDateTime.now(clock));
    }

    /** Bỏ thích không kiểm trạng thái sản phẩm — sản phẩm đã ẩn vẫn bỏ được (D2). */
    @Transactional
    public void remove(long userId, long productId) {
        wishlists.deleteByUserIdAndProductId(userId, productId);
    }
}
