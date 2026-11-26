package com.genie.support;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Hàm dựng dữ liệu cho test chuỗi C2 giỏ → checkout (plan C2 mục 8.2.1 — không sửa {@link TestDataBuilder}, chỉ gọi nó).
 */
public class OrderTestData {

    private final JdbcTemplate jdbc;
    private final TestDataBuilder data;

    public OrderTestData(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.data = new TestDataBuilder(jdbc);
    }

    public TestDataBuilder base() {
        return data;
    }

    /** Một SKU đang bán: sản phẩm + biến thể. */
    public record Sku(long vendorUserId, long productId, long variantId) {
    }

    /** Shop đã xác minh mới (chưa có kho lấy hàng). */
    public long shop() {
        return data.vendor();
    }

    /** SKU đang bán của shop {@code vendor} — giá {@code price}, tồn {@code stock}, có ảnh chính. */
    public Sku sku(long vendor, long price, int stock) {
        long p = data.product(vendor, "SP " + TestDataBuilder.uniqueWord(), "PUBLISHED", price);
        long v = data.variant(p, price, stock);
        data.primaryImage(p, "seed/product/" + p + ".jpg");
        return new Sku(vendor, p, v);
    }

    /** Thêm biến thể cho sản phẩm có sẵn. */
    public long extraVariant(long productId, long price, int stock) {
        return data.variant(productId, price, stock);
    }

    public void setProductStatus(long productId, String status) {
        data.setStatus(productId, status);
    }

    public void deleteVariant(long variantId) {
        jdbc.update("UPDATE product_variants SET deleted_at = now() WHERE id = ?", variantId);
    }

    public void setPrice(long variantId, long price) {
        jdbc.update("UPDATE product_variants SET price = ? WHERE id = ?", price, variantId);
    }

    public void setStock(long variantId, int stock, int reserved) {
        jdbc.update("UPDATE product_variants SET stock_count = ?, reserved_count = ? WHERE id = ?", stock, reserved,
                variantId);
    }

    public int reserved(long variantId) {
        return jdbc.queryForObject("SELECT reserved_count FROM product_variants WHERE id = ?", Integer.class, variantId);
    }

    public int stock(long variantId) {
        return jdbc.queryForObject("SELECT stock_count FROM product_variants WHERE id = ?", Integer.class, variantId);
    }

    public long version(long variantId) {
        return jdbc.queryForObject("SELECT version FROM product_variants WHERE id = ?", Long.class, variantId);
    }

    /** Shop tạm nghỉ từ hôm qua tới tuần sau (theo giờ CSDL của test: đồng hồ test quanh 2026-06-08). */
    public void vacation(long vendor, String fromIso, String toIso) {
        jdbc.update("UPDATE vendor_profiles SET vacation_starts_at = CAST(? AS timestamptz), "
                + "vacation_ends_at = CAST(? AS timestamptz) WHERE user_id = ?", fromIso, toIso, vendor);
    }

    /** Kho lấy hàng 2 cấp đã đăng ký GHN (UC-VND-05). */
    public void pickup(long vendor, int provinceId, String wardCode) {
        jdbc.update("UPDATE vendor_profiles SET pickup_province_id = ?, pickup_province = 'Tỉnh test', "
                + "pickup_ward_code = ?, pickup_ward = 'Phường test', pickup_address_line = '1 Đường test', "
                + "pickup_phone = '0901234567', ghn_shop_id = 99 WHERE user_id = ?", provinceId, wardCode, vendor);
    }

    public void freeship(long vendor, long threshold) {
        jdbc.update("INSERT INTO vendor_shipping_config (vendor_id, freeship_threshold) VALUES (?, ?) "
                + "ON CONFLICT (vendor_id) DO UPDATE SET freeship_threshold = EXCLUDED.freeship_threshold", vendor,
                threshold);
    }

    /** Địa chỉ của người mua (2 cấp) — trả id. */
    public long address(long userId, int provinceId, String wardCode, boolean isDefault) {
        return jdbc.queryForObject("""
                INSERT INTO addresses (user_id, recipient_name, phone_number, address_line, ward_code, ward, province_id,
                    province, is_default, created_at, updated_at)
                VALUES (?, 'Người nhận', '0901234567', '25 Nguyễn Huệ', ?, 'Phường test', ?, 'Tỉnh test', ?, now(), now())
                RETURNING id""", Long.class, userId, wardCode, provinceId, isDefault);
    }

    /** Số dòng trong giỏ của người dùng. */
    public int cartLines(long userId) {
        return jdbc.queryForObject("SELECT count(*) FROM cart_items i JOIN carts c ON c.id = i.cart_id "
                + "WHERE c.user_id = ?", Integer.class, userId);
    }

    public static String key() {
        return "k-" + UUID.randomUUID();
    }
}
