// Cổng tra thẻ sản phẩm cho chat (plan 2026-11-26 chat M02.02, N2, D8; UC-SOC-02)
package com.genie.social.chat.application;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/** Cổng đảo ngược — module {@code catalog} cài ({@code ChatProductLookupImpl}). */
public interface ChatProductLookup {

    /**
     * Biến thể sẽ lưu cho thẻ sản phẩm. {@code variantId} có → phải thuộc sản phẩm, chưa xóa ({@link ProductRef#variantId()}
     * {@code null} nếu không); không có → biến thể còn bán giá thấp nhất. Rỗng khi sản phẩm không tồn tại.
     */
    Optional<ProductRef> resolve(long productId, Long variantId);

    /** Thông tin <b>hiện tại</b> của thẻ theo {@code product_variant_id} (một truy vấn cho cả trang tin). */
    Map<Long, ProductCard> cards(Collection<Long> variantIds);

    /**
     * @param variantId    {@code null} khi biến thể yêu cầu không thuộc sản phẩm / sản phẩm hết biến thể
     * @param published    sản phẩm đang {@code PUBLISHED}
     * @param vendorUserId {@code users.id} chủ shop của sản phẩm
     */
    record ProductRef(long productId, Long variantId, long vendorUserId, boolean published) {
    }

    /** @param available sản phẩm còn bán và biến thể chưa xóa; {@code false} → thẻ hiện tên, ẩn giá. */
    record ProductCard(long productId, long variantId, String slug, String name, String imageUrl, long price,
                       boolean available, long vendorUserId) {
    }
}
