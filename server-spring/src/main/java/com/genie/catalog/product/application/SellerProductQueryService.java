// Danh sách & chi tiết sản phẩm của shop — API_SPEC 10.5 GET (UC-CAT-09, 10, 11; plan C1 P03.05, D21)
package com.genie.catalog.product.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.catalog.attribute.application.AttributeView;
import com.genie.catalog.brand.application.BrandService;
import com.genie.catalog.category.application.CategoryCatalog;
import com.genie.catalog.category.application.CategoryFacade;
import com.genie.catalog.category.application.CategoryIndex;
import com.genie.catalog.product.domain.Product;
import com.genie.catalog.product.domain.ProductRepository;
import com.genie.catalog.product.domain.ProductStatus;
import com.genie.common.api.PageResponse;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.error.NotFoundException;
import com.genie.common.storage.ObjectStorage;
import com.genie.common.storage.UploadPurpose;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ sản phẩm của chính shop ({@code products.vendor_id = users.id} người gọi); của shop khác hoặc đã xóa →
 * {@code 404} (không lộ sự tồn tại — plan C1 N8). Danh sách đọc bằng SQL (không nạp entity hàng loạt: {@code Product.category}
 * là EAGER — CLAUDE.md mục 9).
 */
@Service
public class SellerProductQueryService {

    /** Tab của màn Sản phẩm; {@code ALL} = mọi trạng thái trừ {@code DELETED}. */
    public static final List<String> TABS = List.of("ALL", "PUBLISHED", "DRAFT", "PENDING_APPROVAL", "REJECTED", "HIDDEN");
    static final Duration DOCUMENT_LINK_TTL = Duration.ofMinutes(5);

    public record Page(List<SellerProductViews.Row> items, PageResponse.Pagination pagination,
                       Map<String, Long> counts) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final ProductRepository products;
    private final ProductListingStore store;
    private final CategoryCatalog catalog;
    private final CategoryFacade categories;
    private final BrandService brands;
    private final MediaUrls media;
    private final ObjectStorage storage;
    private final ObjectMapper json;

    public SellerProductQueryService(NamedParameterJdbcTemplate jdbc, ProductRepository products, ProductListingStore store,
                                     CategoryCatalog catalog, CategoryFacade categories, BrandService brands,
                                     MediaUrls media, ObjectStorage storage, ObjectMapper json) {
        this.jdbc = jdbc;
        this.products = products;
        this.store = store;
        this.catalog = catalog;
        this.categories = categories;
        this.brands = brands;
        this.media = media;
        this.storage = storage;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public Page list(long vendorUserId, String status, String q, Integer page, Integer size) {
        String tab = status == null || status.isBlank() ? "ALL" : status.trim().toUpperCase();
        if (!TABS.contains(tab)) {
            throw InvalidRequestException.of("status", "INVALID_VALUE", status, "Trạng thái không hợp lệ.");
        }
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null ? 20 : Math.max(1, Math.min(size, 100));
        String needle = q == null || q.isBlank() ? null : q.trim();

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("vid", vendorUserId)
                .addValue("q", needle).addValue("limit", s).addValue("offset", (p - 1) * s);
        String search = needle == null ? "" : """
                 AND (immutable_unaccent(lower(p.name)) LIKE '%' || immutable_unaccent(lower(:q)) || '%'
                      OR EXISTS (SELECT 1 FROM product_variants sv WHERE sv.product_id = p.id AND sv.deleted_at IS NULL
                                 AND lower(sv.sku) LIKE '%' || lower(:q) || '%'))""";

        Map<String, Long> counts = new LinkedHashMap<>();
        TABS.forEach(t -> counts.put(t, 0L));
        jdbc.query("""
                SELECT CASE WHEN p.status = 'FLAGGED' THEN 'PENDING_APPROVAL' ELSE p.status END AS tab, count(*) AS n
                  FROM products p WHERE p.vendor_id = :vid AND p.status <> 'DELETED'""" + search + " GROUP BY 1",
                params, rs -> {
                    long n = rs.getLong("n");
                    counts.merge(rs.getString("tab"), n, Long::sum);
                    counts.merge("ALL", n, Long::sum);
                });

        String filter = switch (tab) {
            case "ALL" -> " AND p.status <> 'DELETED'";
            case "PENDING_APPROVAL" -> " AND p.status IN ('PENDING_APPROVAL', 'FLAGGED')";
            default -> " AND p.status = '" + tab + "'";     // tab đã kiểm trong danh sách cố định
        };
        List<SellerProductViews.Row> rows = jdbc.query("""
                SELECT p.id, p.name, p.status, p.moderation_reason, p.is_locked, COALESCE(p.sold, 0) AS sold,
                       (SELECT i.image_url FROM product_images i WHERE i.product_id = p.id AND i.variant_id IS NULL
                         ORDER BY i.is_primary DESC NULLS LAST, i.display_order, i.id LIMIT 1) AS image,
                       v.variant_count, v.price_min, v.stock_total
                  FROM products p
                  LEFT JOIN LATERAL (SELECT count(*) AS variant_count, MIN(pv.price) AS price_min,
                                            COALESCE(SUM(pv.stock_count), 0) AS stock_total
                                       FROM product_variants pv WHERE pv.product_id = p.id AND pv.deleted_at IS NULL) v ON TRUE
                 WHERE p.vendor_id = :vid""" + filter + search + """
                 ORDER BY p.updated_at DESC, p.id DESC LIMIT :limit OFFSET :offset""", params, (rs, i) -> {
            String st = rs.getString("status");
            boolean locked = rs.getBoolean("is_locked");
            String reason = "REJECTED".equals(st) || locked ? rs.getString("moderation_reason") : null;
            return new SellerProductViews.Row(rs.getLong("id"), rs.getString("name"), media.product(rs.getString("image")),
                    rs.getInt("variant_count"), rs.getBigDecimal("price_min") == null ? 0 : rs.getBigDecimal("price_min").longValue(),
                    rs.getLong("stock_total"), rs.getLong("sold"), shopStatus(st), reason, locked);
        });
        return new Page(rows, PageResponse.Pagination.of(p, s, counts.getOrDefault(tab, 0L)), counts);
    }

    @Transactional(readOnly = true)
    public SellerProductViews.Detail detail(long vendorUserId, long productId) {
        Product p = owned(vendorUserId, productId);
        List<AttributeView> attrs = p.getCategoryId() == null ? List.of() : categories.attributesFor(p.getCategoryId());

        List<SellerProductViews.MediaRef> images = store.galleryKeys(productId).stream()
                .map(k -> new SellerProductViews.MediaRef(k, media.product(k))).toList();
        SellerProductViews.VideoRef video = p.getVideoUrl() == null ? null
                : new SellerProductViews.VideoRef(p.getVideoUrl(), fileName(p.getVideoUrl()), media.product(p.getVideoUrl()));
        List<SellerProductViews.DocumentRef> docs = store.documents(productId).stream()
                .map(d -> new SellerProductViews.DocumentRef(d.id(), d.fileName(), d.fileUrl(),
                        storage.presignGet(UploadPurpose.PRODUCT_DOCUMENT.bucket(), d.fileUrl(), DOCUMENT_LINK_TTL)))
                .toList();

        List<SellerProductViews.IdName> path = new ArrayList<>();
        if (p.getCategoryId() != null) {
            CategoryIndex idx = catalog.index();
            idx.ancestors(p.getCategoryId()).forEach(r -> path.add(new SellerProductViews.IdName(r.id(), r.name())));
            idx.find(p.getCategoryId()).ifPresent(r -> path.add(new SellerProductViews.IdName(r.id(), r.name())));
        }
        SellerProductViews.IdName brand = p.getBrandId() == null ? null : brands.find(p.getBrandId())
                .map(b -> new SellerProductViews.IdName(b.id(), b.name())).orElse(null);

        Matrix matrix = matrix(store.activeVariants(productId), p.getVariantImageAttribute());
        String st = p.getStatus().name();
        boolean locked = Boolean.TRUE.equals(p.getIsLocked());
        return new SellerProductViews.Detail(p.getId(), p.getVersion() == null ? 0 : p.getVersion(), shopStatus(st),
                "REJECTED".equals(st) || locked ? p.getModerationReason() : null, locked,
                Boolean.TRUE.equals(p.getIsRequeued()), images, video, docs, p.getName(), path, brand,
                p.getItemCondition() == null ? "NEW" : p.getItemCondition().name(), p.getOrigin(),
                SpecCodec.toViews(SpecCodec.parse(json, p.getSpecsJson()), attrs),
                p.getDescription(), matrix.groups(), p.getVariantImageAttribute(), matrix.variants(),
                toInt(p.getWeight()), new SellerProductViews.Dimensions(toInt(p.getLength()), toInt(p.getWidth()),
                        toInt(p.getHeight())),
                p.getShippingPayer() == null ? "BUYER" : p.getShippingPayer().name(),
                Boolean.TRUE.equals(p.getIsPreOrder()), p.getPreOrderDays());
    }

    /** Sản phẩm của chính shop, chưa xóa — khác → 404 (cả khi là sản phẩm của shop khác). */
    public Product owned(long vendorUserId, long productId) {
        return products.findById(productId)
                .filter(p -> p.getVendorId() != null && p.getVendorId() == vendorUserId)
                .filter(p -> p.getStatus() != ProductStatus.DELETED)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm."));
    }

    /** FLAGGED (AI nghi ngờ) hiện cho shop như đang chờ duyệt (API 10.5). */
    static String shopStatus(String status) {
        return "FLAGGED".equals(status) ? "PENDING_APPROVAL" : status;
    }

    record Matrix(List<SellerProductViews.OptionGroup> groups, List<SellerProductViews.Variant> variants) {
    }

    /** Dựng nhóm phân loại + bảng SKU từ {@code attributes_json} các biến thể (thứ tự giá trị = thứ tự biến thể). */
    Matrix matrix(List<ProductListingStore.VariantRow> rows, String imageGroup) {
        Map<String, Map<String, String>> groups = new LinkedHashMap<>();   // nhóm → (giá trị → khóa ảnh)
        for (ProductListingStore.VariantRow r : rows) {
            r.attributes().forEach((g, v) -> {
                Map<String, String> values = groups.computeIfAbsent(g, k -> new LinkedHashMap<>());
                String img = g.equals(imageGroup) ? r.imageUrl() : null;
                values.merge(v, img == null ? "" : img, (old, neu) -> old.isEmpty() ? neu : old);
            });
        }
        List<SellerProductViews.OptionGroup> outGroups = new ArrayList<>();
        groups.forEach((name, values) -> {
            List<SellerProductViews.OptionValue> vals = new ArrayList<>();
            values.forEach((v, img) -> vals.add(new SellerProductViews.OptionValue(v, img.isEmpty() ? null : img,
                    img.isEmpty() ? null : media.product(img))));
            outGroups.add(new SellerProductViews.OptionGroup(name, vals));
        });
        Set<String> order = groups.keySet();
        List<SellerProductViews.Variant> variants = rows.stream().map(r -> new SellerProductViews.Variant(r.id(),
                order.stream().map(g -> r.attributes().getOrDefault(g, "")).toList(), r.sku(),
                r.price() == null ? 0 : r.price().longValue(), r.stock(), r.reserved())).toList();
        return new Matrix(outGroups, variants);
    }

    static String fileName(String key) {
        return key == null ? null : key.substring(key.lastIndexOf('/') + 1);
    }

    private static Integer toInt(Double d) {
        return d == null ? null : (int) Math.round(d);
    }
}
