// Cài cổng ChatProductLookup của module social (plan 2026-11-26 chat M02.04, N2, D8; UC-SOC-02)
package com.genie.catalog.product.application;

import com.genie.social.chat.application.ChatProductLookup;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Read-model cho thẻ sản phẩm trong chat — <b>file mới</b>, không sửa {@link VariantFacade} (C2 sở hữu) mà chỉ đọc qua
 * {@link VariantFacade#lines} (tên, slug, ảnh biến thể → ảnh đại diện, giá, luật "còn bán").
 */
@Component
public class ChatProductLookupImpl implements ChatProductLookup {

    private final JdbcTemplate jdbc;
    private final VariantFacade variants;

    public ChatProductLookupImpl(JdbcTemplate jdbc, VariantFacade variants) {
        this.jdbc = jdbc;
        this.variants = variants;
    }

    @Override
    public Optional<ProductRef> resolve(long productId, Long variantId) {
        List<Object[]> product = jdbc.query("SELECT vendor_id, status FROM products WHERE id = ?",
                (rs, n) -> new Object[] {rs.getLong("vendor_id"), rs.getString("status")}, productId);
        if (product.isEmpty()) {
            return Optional.empty();
        }
        long vendorUserId = (Long) product.get(0)[0];
        boolean published = "PUBLISHED".equals(product.get(0)[1]);
        List<Long> chosen = variantId != null
                ? jdbc.queryForList("""
                        SELECT id FROM product_variants WHERE id = ? AND product_id = ? AND deleted_at IS NULL""",
                        Long.class, variantId, productId)
                // mặc định: biến thể còn bán giá thấp nhất (D8) — còn hàng trước, rồi giá, rồi id
                : jdbc.queryForList("""
                        SELECT id FROM product_variants WHERE product_id = ? AND deleted_at IS NULL
                        ORDER BY (stock_count - reserved_count > 0) DESC, price, id LIMIT 1""", Long.class, productId);
        return Optional.of(new ProductRef(productId, chosen.isEmpty() ? null : chosen.get(0), vendorUserId, published));
    }

    @Override
    public Map<Long, ProductCard> cards(Collection<Long> variantIds) {
        Map<Long, ProductCard> out = new LinkedHashMap<>();
        variants.lines(variantIds).forEach((id, l) -> out.put(id, new ProductCard(l.productId(), l.variantId(),
                l.productSlug(), l.productName(), l.imageUrl(), l.unitPrice(), l.purchasable(), l.vendorUserId())));
        return out;
    }
}
