package com.genie.catalog.product.application;

import com.genie.catalog.search.domain.SlugUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Cửa của {@code catalog} cho module {@code order} (plan C2 O02.01, mục 8.2 – 8.5): đọc dòng giỏ theo lô và ghi
 * {@code product_variants.reserved_count / stock_count} bằng câu UPDATE có điều kiện (LLD 9.1) — KHÔNG qua entity
 * {@code ProductVariant} được quản lý (chống ghi đè cột của nhau với C1, plan mục 8.3). Đọc thẳng CSDL, không qua cache
 * chi tiết sản phẩm, để giá / trạng thái mới thấy ngay ở giỏ (mục 8.5).
 */
@Component
public class VariantFacade {

    /** Khối lượng mặc định khi {@code products.weight} NULL (dữ liệu cũ — plan C2 D7). */
    public static final int DEFAULT_WEIGHT_GRAMS = 500;

    private final JdbcTemplate jdbc;
    private final MediaUrls media;

    public VariantFacade(JdbcTemplate jdbc, MediaUrls media) {
        this.jdbc = jdbc;
        this.media = media;
    }

    /**
     * Một biến thể kèm thông tin sản phẩm cần cho giỏ / xem trước / đặt hàng.
     *
     * @param vendorUserId    {@code products.vendor_id} = {@code users.id} của chủ shop
     * @param available       {@code stock_count - reserved_count} (không âm)
     * @param purchasable     sản phẩm {@code PUBLISHED} và biến thể chưa bỏ (luật "mua được" — mục 8.5, chưa xét shop / tồn)
     * @param weightGrams     gram; NULL ở CSDL → {@value #DEFAULT_WEIGHT_GRAMS}
     * @param sellerPaysShipping {@code shipping_payer = SELLER}
     */
    public record VariantLine(long variantId, long productId, String productSlug, String productName, String sku,
                              String variantName, String imageUrl, long unitPrice, Long originalPrice, int available,
                              boolean purchasable, long vendorUserId, Long categoryId, String categoryName,
                              int weightGrams, Integer lengthCm, Integer widthCm, Integer heightCm,
                              boolean sellerPaysShipping) {
    }

    /** Nạp theo lô; id không tồn tại không có trong kết quả. Thứ tự = thứ tự {@code ids}. */
    public Map<Long, VariantLine> lines(Collection<Long> ids) {
        Map<Long, VariantLine> out = new LinkedHashMap<>();
        if (ids == null || ids.isEmpty()) {
            return out;
        }
        Long[] arr = ids.stream().distinct().toArray(Long[]::new);
        List<VariantLine> rows = jdbc.query("""
                SELECT v.id, v.product_id, v.sku, v.variant_name, v.price, v.stock_count, v.reserved_count,
                       v.deleted_at IS NOT NULL AS deleted, v.image_url AS variant_image,
                       (SELECT string_agg(e.value, ' / ') FROM jsonb_each_text(v.attributes_json) e) AS attr_label,
                       p.name, p.status, p.vendor_id, p.category_id, p.base_price, p.weight, p.length, p.width, p.height,
                       p.shipping_payer, c.name AS category_name,
                       (SELECT i.image_url FROM product_images i WHERE i.product_id = p.id
                         ORDER BY i.is_primary DESC NULLS LAST, i.display_order NULLS LAST, i.id LIMIT 1) AS product_image
                FROM product_variants v
                JOIN products p ON p.id = v.product_id
                LEFT JOIN categories c ON c.id = p.category_id
                WHERE v.id = ANY (CAST(? AS bigint[]))""", (rs, n) -> map(rs), pgArray(arr));
        Map<Long, VariantLine> byId = new LinkedHashMap<>();
        rows.forEach(r -> byId.put(r.variantId(), r));
        for (Long id : arr) {
            VariantLine r = byId.get(id);
            if (r != null) {
                out.put(id, r);
            }
        }
        return out;
    }

    /** Id biến thể (kể cả đã bỏ) của một sản phẩm — cổng {@code ProductOrderGuard} của module order. */
    public List<Long> variantIdsOf(long productId) {
        return jdbc.queryForList("SELECT id FROM product_variants WHERE product_id = ?", Long.class, productId);
    }

    /**
     * Giữ chỗ {@code qty} (LLD 9.1; mục 8.3): chỉ khi sản phẩm {@code PUBLISHED}, biến thể chưa bỏ và còn đủ tồn khả dụng.
     * Không đọc-rồi-ghi. {@code false} = hết hàng / không còn bán → caller trả {@code 409 GNE-INV-4090}.
     */
    public boolean reserve(long variantId, int qty) {
        return jdbc.update("""
                UPDATE product_variants v SET reserved_count = v.reserved_count + ?, version = v.version + 1
                FROM products p
                WHERE v.id = ? AND p.id = v.product_id AND p.status = 'PUBLISHED' AND v.deleted_at IS NULL
                  AND v.stock_count - v.reserved_count >= ?""", qty, variantId, qty) == 1;
    }

    /** Nhả giữ chỗ — không lọc trạng thái sản phẩm / biến thể (vẫn phải nhả khi người bán đã ẩn / bỏ). */
    public boolean release(long variantId, int qty) {
        return jdbc.update("""
                UPDATE product_variants SET reserved_count = reserved_count - ?, version = version + 1
                WHERE id = ? AND reserved_count >= ?""", qty, variantId, qty) == 1;
    }

    /** Chốt bán (COD / thanh toán xong): trừ tồn thật và bỏ giữ chỗ trong cùng một câu. */
    public boolean commitSale(long variantId, int qty) {
        return jdbc.update("""
                UPDATE product_variants SET stock_count = stock_count - ?, reserved_count = reserved_count - ?,
                    version = version + 1
                WHERE id = ? AND reserved_count >= ? AND stock_count >= ?""", qty, qty, variantId, qty, qty) == 1;
    }

    /** Mảng PostgreSQL dạng chữ {@code {1,2,3}} — dùng với {@code CAST(? AS bigint[])}. */
    public static String pgArray(Long[] ids) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < ids.length; i++) {
            sb.append(i == 0 ? "" : ",").append(ids[i]);
        }
        return sb.append('}').toString();
    }

    private VariantLine map(ResultSet rs) throws SQLException {
        String status = rs.getString("status");
        boolean deleted = rs.getBoolean("deleted");
        long price = money(rs.getBigDecimal("price"));
        Long original = Pricing.originalPrice(price, rs.getBigDecimal("base_price"));
        int stock = rs.getInt("stock_count");
        int reserved = rs.getInt("reserved_count");
        String variantName = rs.getString("variant_name");
        if (variantName == null || variantName.isBlank()) {
            variantName = rs.getString("attr_label");
        }
        String image = rs.getString("variant_image");
        if (image == null || image.isBlank()) {
            image = rs.getString("product_image");
        }
        double weight = rs.getDouble("weight");
        int grams = rs.wasNull() || weight <= 0 ? DEFAULT_WEIGHT_GRAMS : (int) Math.round(weight);
        long categoryId = rs.getLong("category_id");
        boolean noCategory = rs.wasNull();
        String name = rs.getString("name");
        return new VariantLine(rs.getLong("id"), rs.getLong("product_id"), SlugUtil.slugify(name), name,
                rs.getString("sku"), variantName, media.product(image), price, original, Math.max(0, stock - reserved),
                "PUBLISHED".equals(status) && !deleted, rs.getLong("vendor_id"), noCategory ? null : categoryId,
                rs.getString("category_name"), grams, dim(rs, "length"), dim(rs, "width"), dim(rs, "height"),
                "SELLER".equals(rs.getString("shipping_payer")));
    }

    private static Integer dim(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() || v <= 0 ? null : (int) Math.ceil(v);
    }

    private static long money(BigDecimal v) {
        return v == null ? 0L : v.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
