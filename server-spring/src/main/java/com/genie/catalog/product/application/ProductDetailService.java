package com.genie.catalog.product.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.catalog.product.domain.VariantAttributes;
import com.genie.catalog.product.domain.VariantMatrix;
import com.genie.catalog.search.domain.SlugUtil;
import com.genie.common.cache.JsonCache;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chi tiết sản phẩm, ma trận biến thể SKU & tồn kho (UC-CAT-06; API_SPEC 5.6, 5.10; plan 2026-11-17 A7, D11).
 * <ul>
 *   <li>Phần tĩnh ({@link ProductSnapshot}) nạp bằng vài câu SQL theo sản phẩm (không N+1) và cache Redis 5 phút;</li>
 *   <li>tồn khả dụng ({@code stock_count − reserved_count}) đọc lại mỗi request bằng một câu SQL nhẹ — giỏ / đặt hàng
 *       vẫn kiểm tồn trong transaction của chúng;</li>
 *   <li>sản phẩm không {@code PUBLISHED} / shop chưa {@code VERIFIED} / chủ shop bị khóa → {@code 404 GNE-CAT-4042}.</li>
 * </ul>
 * Xóa cache: {@link #evict(long)} — gọi khi plan sản phẩm của người bán (UC-CAT-10/11/13) sửa / ẩn / duyệt sản phẩm.
 */
@Service
public class ProductDetailService {

    static final Duration TTL = Duration.ofMinutes(5);
    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final JdbcTemplate jdbc;
    private final JsonCache cache;
    private final MediaUrls media;
    private final ObjectMapper json;
    private final Clock clock;
    private final JavaType snapshotType;

    public ProductDetailService(JdbcTemplate jdbc, JsonCache cache, MediaUrls media, ObjectMapper json, Clock clock) {
        this.jdbc = jdbc;
        this.cache = cache;
        this.media = media;
        this.json = json;
        this.clock = clock;
        this.snapshotType = cache.type(ProductSnapshot.class);
    }

    static String cacheKey(long id) {
        return "catalog:product:" + id;
    }

    public void evict(long productId) {
        cache.evict(cacheKey(productId));
    }

    @Transactional(readOnly = true)
    public ProductDetailView detail(long id) {
        ProductSnapshot s = snapshot(id);
        Map<Long, Integer> stock = stock(id);
        List<ProductDetailView.Variant> variants = s.skus().stream()
                .map(v -> new ProductDetailView.Variant(v.id(), v.options(), v.price(), stock.getOrDefault(v.id(), 0)))
                .toList();
        ProductSnapshot.Shop sh = s.shop();
        ProductDetailView.ShopInfo shop = new ProductDetailView.ShopInfo(sh.id(), sh.name(), sh.logoUrl(), sh.isMall(),
                sh.isFavorite(), sh.ratingAvg(), sh.followerCount(), sh.provinceName(), sh.joinedYear(), false, vacation(sh));
        return new ProductDetailView(s.id(), s.slug(), s.name(), s.categoryPath(), s.brand(), s.images(), null, s.price(),
                s.originalPrice(), s.discountPercent(), s.ratingAvg(), s.ratingCount(), s.soldCount(), s.isPreOrder(),
                s.preOrderDays(), null, s.optionGroups(), variants, s.specs(), s.condition(), s.origin(), s.shipsFrom(),
                s.descriptionHtml(), shop);
    }

    @Transactional(readOnly = true)
    public VariantChoices variants(long id) {
        ProductSnapshot s = snapshot(id);
        Map<Long, Integer> stock = stock(id);
        List<VariantChoices.Group> groups = s.optionGroups().stream()
                .map(g -> new VariantChoices.Group(g.name(), g.values().stream().map(ProductDetailView.OptionValue::value).toList()))
                .toList();
        List<VariantChoices.Choice> choices = s.skus().stream()
                .map(v -> new VariantChoices.Choice(v.id(), v.options(), v.price(), stock.getOrDefault(v.id(), 0), v.imageUrl()))
                .toList();
        return new VariantChoices(groups, choices, s.images().isEmpty() ? null : s.images().get(0));
    }

    /** Phần tĩnh (cache 5 phút). Dùng chung cho sản phẩm tương tự (giá, danh mục, tên). */
    public ProductSnapshot snapshot(long id) {
        ProductSnapshot s = cache.getOrLoad(cacheKey(id), snapshotType, TTL, () -> load(id));
        if (s == null) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE);
        }
        return s;
    }

    /** Shop đang tạm nghỉ ngay lúc này → {@code {until, message}}. */
    ProductDetailView.Vacation vacation(ProductSnapshot.Shop sh) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (sh.vacationStartsAt() == null || sh.vacationStartsAt().isAfter(now)
                || (sh.vacationEndsAt() != null && !sh.vacationEndsAt().isAfter(now))) {
            return null;
        }
        return new ProductDetailView.Vacation(
                sh.vacationEndsAt() == null ? null : sh.vacationEndsAt().atZoneSameInstant(VN).toLocalDate(), sh.vacationMessage());
    }

    private Map<Long, Integer> stock(long productId) {
        Map<Long, Integer> out = new HashMap<>();
        jdbc.query("""
                SELECT id, GREATEST(stock_count - reserved_count, 0) AS available FROM product_variants
                 WHERE product_id = ? AND deleted_at IS NULL""",
                rs -> { out.put(rs.getLong("id"), rs.getInt("available")); }, productId);
        return out;
    }

    // ===================== nạp phần tĩnh =====================

    private ProductSnapshot load(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.id, p.name, p.description, p.brand, p.brand_id, p.category_id, p.specs_json::text AS specs_json,
                       p.base_price, p.sold, p.rating, p.is_pre_order, p.pre_order_days, p.item_condition, p.origin,
                       p.ships_from, p.variant_image_attribute, p.option_images_json::text AS option_images_json,
                       vp.id AS shop_id, vp.user_id AS vendor_id, vp.shop_name, vp.logo_url, vp.is_mall, vp.is_favorite,
                       vp.rating_avg AS shop_rating, vp.follower_count, vp.pickup_province, vp.created_at AS shop_created_at,
                       vp.vacation_starts_at, vp.vacation_ends_at, vp.vacation_message,
                       b.name AS brand_name, b.is_verified AS brand_verified
                  FROM products p
                  JOIN users u ON u.id = p.vendor_id
                  JOIN vendor_profiles vp ON vp.user_id = p.vendor_id
                  LEFT JOIN brands b ON b.id = p.brand_id
                 WHERE p.id = ? AND p.status = 'PUBLISHED' AND u.status = 'ACTIVE' AND vp.kyc_status = 'VERIFIED'""", id);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> r = rows.get(0);
        Long categoryId = r.get("category_id") == null ? null : ((Number) r.get("category_id")).longValue();
        String name = (String) r.get("name");

        // ---- SKU + ma trận phân loại ----
        List<VariantMatrix.Sku> skus = new ArrayList<>();
        Map<Long, String> variantImages = new HashMap<>();
        jdbc.query("""
                SELECT v.id, v.attributes_json::text AS attrs, v.variant_name, v.price,
                       COALESCE(v.image_url, (SELECT i.image_url FROM product_images i WHERE i.variant_id = v.id
                                               ORDER BY i.display_order, i.id LIMIT 1)) AS image_url
                  FROM product_variants v WHERE v.product_id = ? AND v.deleted_at IS NULL ORDER BY v.id""", rs -> {
            long vid = rs.getLong("id");
            String img = media.product(rs.getString("image_url"));
            variantImages.put(vid, img);
            // thứ tự nhóm theo variant_name — JSONB tự sắp khóa (vd "Màu sắc" lên trước "Phiên bản")
            skus.add(new VariantMatrix.Sku(vid, VariantAttributes.ordered(attributes(rs.getString("attrs")),
                    rs.getString("variant_name")),
                    Pricing.price(rs.getBigDecimal("price"), null), img));
        }, id);
        VariantMatrix.Result matrix = VariantMatrix.build(skus, (String) r.get("variant_image_attribute"));
        // ảnh của giá trị: option_images_json (mọi nhóm có ảnh — V26), sản phẩm cũ dùng ảnh SKU của nhóm ảnh chính
        Map<String, Map<String, String>> optionImages =
                OptionImages.parse(json, (String) r.get("option_images_json"));
        List<ProductDetailView.OptionGroup> groups = matrix.groups().stream()
                .map(g -> new ProductDetailView.OptionGroup(g.name(), g.values().stream().map(v -> {
                    String key = OptionImages.keyOf(optionImages, g.name(), v.value());
                    return new ProductDetailView.OptionValue(v.value(), key != null ? media.product(key) : v.imageUrl());
                }).toList()))
                .toList();
        List<ProductSnapshot.SkuView> skuViews = matrix.variants().stream()
                .map(v -> new ProductSnapshot.SkuView(v.id(), v.options(), v.price(), variantImages.get(v.id())))
                .toList();

        // ---- giá ----
        BigDecimal base = (BigDecimal) r.get("base_price");
        BigDecimal minVariant = skus.isEmpty() ? null
                : BigDecimal.valueOf(skus.stream().mapToLong(VariantMatrix.Sku::price).min().orElseThrow());
        long price = Pricing.price(minVariant, base);
        Long original = Pricing.originalPrice(price, base);

        // ---- ảnh gallery (không gồm ảnh riêng của SKU) ----
        List<String> images = jdbc.queryForList("""
                SELECT image_url FROM product_images WHERE product_id = ? AND variant_id IS NULL
                 ORDER BY is_primary DESC NULLS LAST, display_order, id""", String.class, id)
                .stream().map(media::product).toList();

        long ratingCount = jdbc.queryForObject(
                "SELECT count(*) FROM product_reviews WHERE product_id = ? AND is_visible = TRUE", Long.class, id);

        Long brandId = r.get("brand_id") == null ? null : ((Number) r.get("brand_id")).longValue();
        String brandName = r.get("brand_name") != null ? (String) r.get("brand_name") : (String) r.get("brand");
        ProductDetailView.BrandRef brand = brandName == null || brandName.isBlank() ? null
                : new ProductDetailView.BrandRef(brandId, brandName, Boolean.TRUE.equals(r.get("brand_verified")));

        OffsetDateTime shopCreated = toOffset(r.get("shop_created_at"));
        ProductSnapshot.Shop shop = new ProductSnapshot.Shop(
                ((Number) r.get("shop_id")).longValue(), ((Number) r.get("vendor_id")).longValue(), (String) r.get("shop_name"),
                media.resolve("avatar-assets", (String) r.get("logo_url")), Boolean.TRUE.equals(r.get("is_mall")),
                Boolean.TRUE.equals(r.get("is_favorite")), number(r.get("shop_rating")), longOf(r.get("follower_count")),
                (String) r.get("pickup_province"), shopCreated == null ? 0 : shopCreated.atZoneSameInstant(VN).getYear(),
                toOffset(r.get("vacation_starts_at")), toOffset(r.get("vacation_ends_at")), (String) r.get("vacation_message"));

        String shipsFrom = r.get("ships_from") != null ? (String) r.get("ships_from") : (String) r.get("pickup_province");
        return new ProductSnapshot(id, SlugUtil.slugify(name), name, categoryId, categoryPath(categoryId), brand, images,
                price, original, Pricing.discountPercent(price, original), Math.round(number(r.get("rating")) * 10) / 10.0,
                ratingCount, longOf(r.get("sold")), Boolean.TRUE.equals(r.get("is_pre_order")),
                r.get("pre_order_days") == null ? null : ((Number) r.get("pre_order_days")).intValue(),
                groups, skuViews, specs((String) r.get("specs_json"), categoryId), (String) r.get("item_condition"),
                (String) r.get("origin"), shipsFrom, (String) r.get("description"), shop);
    }

    /** Đường dẫn danh mục từ gốc tới lá (CTE đệ quy đi lên theo parent_id). */
    private List<ProductDetailView.CategoryRef> categoryPath(Long categoryId) {
        if (categoryId == null) {
            return List.of();
        }
        return jdbc.query("""
                WITH RECURSIVE up AS (
                    SELECT id, name, slug, parent_id, 0 AS depth FROM categories WHERE id = ?
                    UNION ALL
                    SELECT c.id, c.name, c.slug, c.parent_id, up.depth + 1 FROM categories c JOIN up ON c.id = up.parent_id
                     WHERE up.depth < 10)
                SELECT id, name, slug FROM up ORDER BY depth DESC""",
                (rs, i) -> new ProductDetailView.CategoryRef(rs.getLong("id"), rs.getString("name"), rs.getString("slug")),
                categoryId);
    }

    /**
     * {@code specs_json} dạng {@code {"warranty":"12 tháng"}} → {@code [{name: "Bảo hành", value: "12 tháng"}]}:
     * tên lấy từ {@code category_attributes} của danh mục (theo {@code display_order}), khóa lạ giữ nguyên tên khóa.
     */
    private List<ProductDetailView.Spec> specs(String specsJson, Long categoryId) {
        if (specsJson == null || specsJson.isBlank()) {
            return List.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        JsonNode sellerOrder = null;
        try {
            JsonNode node = json.readTree(specsJson);
            if (node.isObject()) {
                for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> e = it.next();
                    if (!e.getValue().isNull() && !SpecCodec.ORDER_KEY.equals(e.getKey())) {
                        values.put(e.getKey(), e.getValue().isTextual() ? e.getValue().asText() : e.getValue().toString());
                    }
                }
                sellerOrder = node.get(SpecCodec.ORDER_KEY);
            } else if (node.isArray()) {     // dạng cũ [{name, value}]
                for (JsonNode n : node) {
                    if (n.hasNonNull("name") && n.hasNonNull("value")) {
                        values.put(n.get("name").asText(), n.get("value").asText());
                    }
                }
            }
        } catch (Exception e) {
            return List.of();
        }
        List<ProductDetailView.Spec> out = new ArrayList<>();
        // tên hiển thị của khóa là mã thuộc tính ngành
        Map<String, String> names = new LinkedHashMap<>();
        if (categoryId != null) {
            jdbc.query("""
                    SELECT code, name FROM category_attributes WHERE category_id = ? AND is_active = TRUE
                     ORDER BY display_order, id""", rs -> {
                names.put(rs.getString("code"), rs.getString("name"));
            }, categoryId);
        }
        if (sellerOrder != null && sellerOrder.isArray()) {
            // người bán đã tự sắp thứ tự dòng (plan C1, 2026-11-27) → hiện đúng thứ tự đó
            SpecCodec.ordered(values, sellerOrder)
                    .forEach((k, v) -> out.add(new ProductDetailView.Spec(names.getOrDefault(k, k), v)));
            return out;
        }
        // dữ liệu cũ: thuộc tính ngành theo display_order trước, khóa tự do sau
        names.forEach((code, name) -> {
            String v = values.remove(code);
            if (v != null) out.add(new ProductDetailView.Spec(name, v));
        });
        values.forEach((k, v) -> out.add(new ProductDetailView.Spec(k, v)));
        return out;
    }

    private Map<String, String> attributes(String attrsJson) {
        if (attrsJson == null || attrsJson.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> raw = json.readValue(attrsJson, new TypeReference<LinkedHashMap<String, Object>>() { });
            Map<String, String> out = new LinkedHashMap<>();
            raw.forEach((k, v) -> out.put(k, v == null ? null : v.toString()));
            return out;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static OffsetDateTime toOffset(Object v) {
        if (v instanceof Timestamp t) {
            return t.toInstant().atOffset(ZoneOffset.UTC);
        }
        if (v instanceof OffsetDateTime o) {
            return o;
        }
        return null;
    }

    private static double number(Object v) {
        return v == null ? 0 : ((Number) v).doubleValue();
    }

    private static long longOf(Object v) {
        return v == null ? 0 : ((Number) v).longValue();
    }
}
