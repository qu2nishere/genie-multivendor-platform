package com.genie.support;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Dựng nhanh dữ liệu danh mục / shop / sản phẩm cho test tích hợp (plan wishlist W01.04 — dùng lại cho các plan catalog,
 * social sau). Chỉ chèn cột bắt buộc, mọi khóa tự nhiên sinh ngẫu nhiên nên các test không đụng nhau.
 * <pre>{@code
 * var data = new TestDataBuilder(jdbc);
 * long shop = data.vendor();
 * long p = data.product(shop, "Tai nghe", "PUBLISHED", 389000);
 * data.variant(p, 389000, 10);
 * data.primaryImage(p, "seed/product/headphones.jpg");
 * }</pre>
 */
public class TestDataBuilder {

    private final JdbcTemplate jdbc;

    public TestDataBuilder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static String rnd() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /** Danh mục gốc đang bật. */
    public long category() {
        return jdbc.queryForObject("INSERT INTO categories (name, slug, is_active, requires_business_license) "
                + "VALUES (?, ?, TRUE, FALSE) RETURNING id", Long.class, "Danh mục " + rnd(), "dm-" + rnd());
    }

    /** Chủ shop (users VENDOR) + vendor_profiles VERIFIED — trả {@code users.id} (= vendor_id của sản phẩm). */
    public long vendor() {
        String tag = rnd();
        long userId = jdbc.queryForObject("INSERT INTO users (email, password, role, status, email_verified_at, "
                + "failed_login_attempts, version, created_at, updated_at) VALUES (?, NULL, 'VENDOR', 'ACTIVE', now(), 0, 0, "
                + "now(), now()) RETURNING id", Long.class, "shop-" + tag + "@genie.test");
        jdbc.update("INSERT INTO vendor_profiles (user_id, shop_name, kyc_status, created_at, is_mall, rating_avg, rating_count, "
                + "follower_count, active_product_count, pre_order_enabled, is_favorite, auto_reply_enabled) "
                + "VALUES (?, ?, 'VERIFIED', now(), FALSE, 0, 0, 0, 0, FALSE, FALSE, FALSE)", userId, "Shop " + tag);
        return userId;
    }

    /** {@code vendor_profiles.id} của chủ shop ({@code shop_id} trên API). */
    public long shopIdOf(long vendorUserId) {
        return jdbc.queryForObject("SELECT id FROM vendor_profiles WHERE user_id = ?", Long.class, vendorUserId);
    }

    /** Sản phẩm với trạng thái cho trước (PUBLISHED, DRAFT, HIDDEN…). */
    public long product(long vendorUserId, String name, String status, long basePrice) {
        return jdbc.queryForObject("INSERT INTO products (vendor_id, name, base_price, status, item_condition, version, "
                        + "created_at, updated_at) VALUES (?, ?, ?, ?, 'NEW', 0, now(), now()) RETURNING id", Long.class,
                vendorUserId, name, BigDecimal.valueOf(basePrice), status);
    }

    /** Biến thể SKU (tồn {@code stock}, chưa giữ chỗ). */
    public long variant(long productId, long price, int stock) {
        return jdbc.queryForObject("INSERT INTO product_variants (product_id, sku, price, stock_count, reserved_count, version) "
                + "VALUES (?, ?, ?, ?, 0, 0) RETURNING id", Long.class, productId, "SKU-" + rnd(), BigDecimal.valueOf(price), stock);
    }

    /** Ảnh chính — {@code objectKey} là khóa đối tượng trong bucket product-media. */
    public long primaryImage(long productId, String objectKey) {
        return jdbc.queryForObject("INSERT INTO product_images (product_id, image_url, is_primary, display_order) "
                + "VALUES (?, ?, TRUE, 0) RETURNING id", Long.class, productId, objectKey);
    }

    public void setStatus(long productId, String status) {
        jdbc.update("UPDATE products SET status = ? WHERE id = ?", status, productId);
    }
}
