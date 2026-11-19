package com.genie.catalog.wishlist.application;

import com.genie.catalog.wishlist.domain.UserWishlistRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cửa công khai của wishlist cho module khác (TS 4.4; plan wishlist D6, W03.06) — chưa có nơi gọi. Dành cho phân hệ
 * thông báo sau này ("sản phẩm yêu thích giảm giá") để biết ai cần được báo.
 */
@Component
public class WishlistFacade {

    private final UserWishlistRepository wishlists;

    public WishlistFacade(UserWishlistRepository wishlists) {
        this.wishlists = wishlists;
    }

    /** {@code users.id} của những người đang thích sản phẩm. */
    @Transactional(readOnly = true)
    public List<Long> userIdsWishing(long productId) {
        return wishlists.findUserIdsByProductId(productId);
    }
}
