package com.genie.support;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Dựng dữ liệu catalog cho test tích hợp bằng SQL thuần — chỉ chèn cột bắt buộc + cột test cần. Hai kiểu dùng song song:
 * builder đầy đủ (plan 2026-11-17 C01.05 — tìm kiếm / trang sản phẩm) và API ngắn ({@link #vendor()}, {@link #product(long,
 * String, String, long)} — plan 2026-11-16 wishlist W01.04).
 * CSDL test không bị xóa giữa các test → mỗi test tự tạo danh mục / shop riêng và lọc theo ID của mình;
 * {@link #uniqueWord()} sinh từ lạ để tìm kiếm không đụng dữ liệu test khác.
 * <pre>{@code
 * var data = new TestDataBuilder(jdbc);
 * long cat = data.category("Tai nghe", null);
 * var shop = data.shop().name("TechZone").mall().province(202, "TP. Hồ Chí Minh").create();
 * long p = data.product(shop).name("Tai nghe ANC").category(cat).sku(389000, 10).create();
 * }</pre>
 */
public class TestDataBuilder {

    private final JdbcTemplate jdbc;

    public TestDataBuilder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Từ 12 chữ cái thường ngẫu nhiên (đủ 26 chữ) — dùng trong tên sản phẩm để tìm kiếm chỉ ra dữ liệu của test này.
     * Lưu ý: tầng trigram (bù gõ sai) vẫn có thể bắt nhầm từ lạ của test khác — test chạy chéo shop chỉ nên khẳng định
     * "có / không có", không khẳng định đúng toàn bộ danh sách.
     */
    public static String uniqueWord() {
        java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append((char) ('a' + r.nextInt(26)));
        }
        return sb.toString();
    }

    // ===== Danh mục & thương hiệu =====

    public long category(String name, Long parentId) {
        return jdbc.queryForObject(
                "INSERT INTO categories (name, slug, parent_id, display_order, is_active) VALUES (?, ?, ?, 0, TRUE) RETURNING id",
                Long.class, name, "c-" + UUID.randomUUID().toString().substring(0, 8), parentId);
    }

    public long brand(String name, boolean verified) {
        return jdbc.queryForObject(
                "INSERT INTO brands (name, slug, is_verified, created_at) VALUES (?, ?, ?, now()) RETURNING id",
                Long.class, name, "b-" + UUID.randomUUID().toString().substring(0, 8), verified);
    }

    // ===== Shop =====

    /** Shop đã tạo: {@code userId} = users.id của chủ shop (= products.vendor_id), {@code shopId} = vendor_profiles.id. */
    public record Shop(long userId, long shopId, String name) {
    }

    public ShopBuilder shop() {
        return new ShopBuilder();
    }

    public final class ShopBuilder {
        private String name = "Shop " + uniqueWord();
        private String kycStatus = "VERIFIED";
        private String userStatus = "ACTIVE";
        private boolean mall;
        private Integer provinceId;
        private String provinceName;
        private BigDecimal freeshipThreshold;
        private OffsetDateTime vacationStart;
        private OffsetDateTime vacationEnd;
        private double ratingAvg;
        private int followers;

        public ShopBuilder name(String v) { this.name = v; return this; }
        public ShopBuilder kyc(String v) { this.kycStatus = v; return this; }
        public ShopBuilder userStatus(String v) { this.userStatus = v; return this; }
        public ShopBuilder mall() { this.mall = true; return this; }
        public ShopBuilder province(int id, String name) { this.provinceId = id; this.provinceName = name; return this; }
        public ShopBuilder freeship(long threshold) { this.freeshipThreshold = BigDecimal.valueOf(threshold); return this; }
        public ShopBuilder rating(double avg) { this.ratingAvg = avg; return this; }
        public ShopBuilder followers(int n) { this.followers = n; return this; }
        public ShopBuilder vacation(OffsetDateTime start, OffsetDateTime end) {
            this.vacationStart = start; this.vacationEnd = end; return this;
        }

        public Shop create() {
            String email = "shop-" + UUID.randomUUID().toString().substring(0, 12) + "@genie.test";
            long userId = jdbc.queryForObject("""
                    INSERT INTO users (email, role, status, email_verified_at, failed_login_attempts, version, created_at, updated_at)
                    VALUES (?, 'VENDOR', ?, now(), 0, 0, now(), now()) RETURNING id""", Long.class, email, userStatus);
            long shopId = jdbc.queryForObject("""
                    INSERT INTO vendor_profiles (user_id, shop_name, kyc_status, created_at, is_mall, rating_avg, rating_count,
                        follower_count, active_product_count, pickup_province_id, pickup_province,
                        vacation_starts_at, vacation_ends_at, shop_description, chat_response_rate, avg_response_minutes)
                    VALUES (?, ?, ?, ?, ?, ?, 10, ?, 0, ?, ?, ?, ?, 'Mô tả shop test', 95.5, 6) RETURNING id""",
                    Long.class, userId, name, kycStatus, Timestamp.from(Instant.parse("2021-03-02T03:00:00Z")),
                    mall, ratingAvg, followers, provinceId, provinceName,
                    vacationStart == null ? null : Timestamp.from(vacationStart.toInstant()),
                    vacationEnd == null ? null : Timestamp.from(vacationEnd.toInstant()));
            if (freeshipThreshold != null) {
                jdbc.update("INSERT INTO vendor_shipping_config (vendor_id, freeship_threshold) VALUES (?, ?)",
                        userId, freeshipThreshold);
            }
            return new Shop(userId, shopId, name);
        }
    }

    // ===== Voucher (plan 2026-11-24 H08 — bộ lọc "Shop có voucher") =====

    /**
     * Voucher SHOP của {@code shop}, mặc định đang dùng được quanh mốc {@code now} (đồng hồ test — cổng tìm kiếm dùng
     * {@code Clock}, không dùng {@code now()} của CSDL). Đổi {@code type("PLATFORM")} / {@code type("FREESHIP")} → không gắn shop.
     */
    public VoucherBuilder voucher(Shop shop, OffsetDateTime now) {
        return new VoucherBuilder(shop == null ? null : shop.userId(), now);
    }

    public final class VoucherBuilder {
        private String type = "SHOP";
        private Long vendorId;
        private boolean active = true;
        private OffsetDateTime validFrom;
        private OffsetDateTime validTo;
        private int usedCount;
        private int usageLimit = 100;
        private BigDecimal budget;
        private BigDecimal totalDiscountUsed = BigDecimal.ZERO;

        private VoucherBuilder(Long vendorId, OffsetDateTime now) {
            this.vendorId = vendorId;
            this.validFrom = now.minusDays(1);
            this.validTo = now.plusDays(30);
        }

        public VoucherBuilder type(String v) { this.type = v; if (!"SHOP".equals(v)) this.vendorId = null; return this; }
        public VoucherBuilder inactive() { this.active = false; return this; }
        public VoucherBuilder valid(OffsetDateTime from, OffsetDateTime to) { this.validFrom = from; this.validTo = to; return this; }
        public VoucherBuilder uses(int used, int limit) { this.usedCount = used; this.usageLimit = limit; return this; }
        public VoucherBuilder budget(long budget, long used) {
            this.budget = BigDecimal.valueOf(budget); this.totalDiscountUsed = BigDecimal.valueOf(used); return this;
        }

        public long create() {
            return jdbc.queryForObject("""
                    INSERT INTO vouchers (code, title, scope, type, discount_type, discount_value, min_order_value, usage_limit,
                        usage_per_user, used_count, priority, vendor_id, valid_from, valid_to, is_active, version, budget,
                        total_discount_used, sponsor_type)
                    VALUES (?, 'Voucher test', 'ALL', ?, 'FIXED_AMOUNT', 10000, 0, ?, 1, ?, 1, ?, ?, ?, ?, 0, ?, ?, ?)
                    RETURNING id""", Long.class,
                    "T" + uniqueWord().toUpperCase(), type, usageLimit, usedCount, vendorId,
                    Timestamp.from(validFrom.toInstant()), Timestamp.from(validTo.toInstant()), active, budget,
                    totalDiscountUsed, "SHOP".equals(type) ? "VENDOR" : "PLATFORM");
        }
    }

    // ===== Sản phẩm =====

    public ProductBuilder product(Shop shop) {
        return new ProductBuilder(shop);
    }

    private record Sku(BigDecimal price, int stock, int reserved, String attributesJson, boolean deleted) {
    }

    public final class ProductBuilder {
        private final Shop shop;
        private String name = "Sản phẩm " + uniqueWord();
        private String description = "Mô tả sản phẩm test";
        private String brandName;
        private Long brandId;
        private Long categoryId;
        private String status = "PUBLISHED";
        private BigDecimal basePrice;
        private Integer sold;
        private Double rating;
        private String specsJson;
        private String shipsFrom;
        private boolean preOrder;
        private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        private final List<Sku> skus = new ArrayList<>();
        private final List<String> images = new ArrayList<>();

        private ProductBuilder(Shop shop) {
            this.shop = shop;
        }

        public ProductBuilder name(String v) { this.name = v; return this; }
        public ProductBuilder description(String v) { this.description = v; return this; }
        public ProductBuilder brand(Long id, String name) { this.brandId = id; this.brandName = name; return this; }
        public ProductBuilder category(long id) { this.categoryId = id; return this; }
        public ProductBuilder status(String v) { this.status = v; return this; }
        public ProductBuilder basePrice(long v) { this.basePrice = BigDecimal.valueOf(v); return this; }
        public ProductBuilder sold(int v) { this.sold = v; return this; }
        public ProductBuilder rating(double v) { this.rating = v; return this; }
        public ProductBuilder specs(String json) { this.specsJson = json; return this; }
        public ProductBuilder shipsFrom(String v) { this.shipsFrom = v; return this; }
        public ProductBuilder preOrder() { this.preOrder = true; return this; }
        public ProductBuilder createdAt(OffsetDateTime v) { this.createdAt = v; return this; }
        public ProductBuilder image(String url) { this.images.add(url); return this; }

        public ProductBuilder sku(long price, int stock) {
            return sku(price, stock, 0, null);
        }

        /** {@code attributesJson} vd {@code {"Màu sắc":"Đen","Phiên bản":"Pro"}}. */
        public ProductBuilder sku(long price, int stock, int reserved, String attributesJson) {
            skus.add(new Sku(BigDecimal.valueOf(price), stock, reserved, attributesJson, false));
            return this;
        }

        public ProductBuilder deletedSku(long price, int stock) {
            skus.add(new Sku(BigDecimal.valueOf(price), stock, 0, null, true));
            return this;
        }

        public long create() {
            BigDecimal base = basePrice != null ? basePrice
                    : skus.stream().map(Sku::price).min(BigDecimal::compareTo).orElse(BigDecimal.valueOf(100000));
            long id = jdbc.queryForObject("""
                    INSERT INTO products (vendor_id, category_id, name, description, brand, brand_id, specs_json, ships_from,
                        base_price, status, is_published, version, created_at, updated_at, sold, rating, item_condition,
                        is_pre_order, pre_order_days, view_count, origin)
                    VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, 0, ?, ?, ?, ?, 'NEW', ?, ?, 0, 'Việt Nam')
                    RETURNING id""", Long.class,
                    shop.userId(), categoryId, name, description, brandName, brandId, specsJson, shipsFrom,
                    base, status, "PUBLISHED".equals(status), Timestamp.from(createdAt.toInstant()),
                    Timestamp.from(createdAt.toInstant()), sold, rating, preOrder, preOrder ? 10 : null);
            int i = 0;
            for (Sku s : skus) {
                jdbc.update("""
                        INSERT INTO product_variants (product_id, sku, variant_name, attributes_json, price, stock_count,
                            reserved_count, version, deleted_at)
                        VALUES (?, ?, ?, CAST(? AS jsonb), ?, ?, ?, 0, ?)""",
                        id, "SKU-" + UUID.randomUUID(), "Phân loại " + (++i), s.attributesJson(), s.price(), s.stock(),
                        s.reserved(), s.deleted() ? Timestamp.from(Instant.now()) : null);
            }
            int order = 0;
            for (String url : images) {
                jdbc.update("""
                        INSERT INTO product_images (product_id, image_url, is_primary, display_order)
                        VALUES (?, ?, ?, ?)""", id, url, order == 0, order);
                order++;
            }
            return id;
        }
    }

    // ===================== API ngắn gọn (plan 2026-11-16 wishlist W01.04 — dùng ở wishlist, theo dõi shop, KYC) =====================

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
