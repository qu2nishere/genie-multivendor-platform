package com.genie.catalog.wishlist.application;

import com.genie.catalog.wishlist.domain.UserWishlistRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc danh sách yêu thích của chính người gọi — UC-CAT-08 (API_SPEC 3.10; plan wishlist W02.03, W03). */
@Service
public class WishlistQueryService {

    /** Tối đa số ID trả cho client để tô tim (D5). */
    public static final int MAX_IDS = 1000;

    private final UserWishlistRepository wishlists;

    public WishlistQueryService(UserWishlistRepository wishlists) {
        this.wishlists = wishlists;
    }

    /** {@code GET /me/wishlist/product-ids} — tối đa 1.000 ID mới nhất. */
    @Transactional(readOnly = true)
    public List<Long> productIds(long userId) {
        return wishlists.findProductIds(userId, PageRequest.of(0, MAX_IDS));
    }
}
