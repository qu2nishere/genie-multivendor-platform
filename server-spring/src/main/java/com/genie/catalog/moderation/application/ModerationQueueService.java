// Hàng chờ duyệt sản phẩm của Staff — API_SPEC 11.12 (UC-CAT-13 bước 1–3, 7; plan C1 P07.02, D13, D15)
package com.genie.catalog.moderation.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.catalog.attribute.application.AttributeView;
import com.genie.catalog.brand.application.BrandService;
import com.genie.catalog.category.application.CategoryCatalog;
import com.genie.catalog.category.application.CategoryFacade;
import com.genie.catalog.category.application.CategoryIndex;
import com.genie.catalog.category.application.CategoryRow;
import com.genie.catalog.product.application.MediaUrls;
import com.genie.catalog.product.application.ProductListingStore;
import com.genie.catalog.product.application.SellerProductViews;
import com.genie.catalog.product.application.SpecCodec;
import com.genie.catalog.product.domain.Product;
import com.genie.catalog.product.domain.ProductRepository;
import com.genie.catalog.product.domain.ProductStatus;
import com.genie.common.api.PageResponse;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.error.NotFoundException;
import com.genie.common.storage.ObjectStorage;
import com.genie.common.storage.UploadPurpose;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tab {@code PENDING} = {@code PENDING_APPROVAL} + {@code FLAGGED} (AI nghi ngờ lên đầu, rồi theo lúc gửi duyệt), {@code
 * PUBLISHED}, {@code REJECTED} (mới quyết trước). Phân công: người <b>đang nhận việc</b> có ít sản phẩm chờ nhất, hòa thì
 * {@code id} nhỏ; không ai nhận việc → để trống (ai có {@code MANAGE_CATALOG} cũng duyệt được — D13). Đọc tên shop thẳng từ
 * {@code vendor_profiles} (read-model SQL như tìm kiếm — plan 2026-11-17 D6).
 */
@Service
public class ModerationQueueService {

    public static final List<String> TABS = List.of("PENDING", "PUBLISHED", "REJECTED");
    static final Duration DOCUMENT_LINK_TTL = Duration.ofMinutes(5);

    /** Kết quả AI của lần gửi duyệt hiện tại (mới nhất, tạo sau submitted_at) — update_gemini.md mục 1. */
    private static final String AI_JOIN = " " + """
            LEFT JOIN LATERAL (
                SELECT r.decision, r.risk_score, r.fallback_to_manual
                  FROM product_moderation_results r
                 WHERE p.submitted_at IS NOT NULL AND r.product_id = p.id AND r.created_at >= p.submitted_at
                 ORDER BY r.created_at DESC, r.id DESC
                 LIMIT 1
            ) ai ON TRUE""";

    public record IdName(long id, String name) {
    }

    /** Một dòng hàng chờ (API 11.12). {@code rejectedByAi}: bị từ chối tự động (không có người duyệt). */
    public record Row(long productId, String name, String imageUrl, IdName shop, String status, boolean isRequeued,
                      IdName assignee, boolean rejectedByAi, OffsetDateTime submittedAt, String aiCheck) {
    }

    public record Page(List<Row> items, PageResponse.Pagination pagination, Map<String, Long> counts) {
    }

    public record AiResult(String decision, int riskScore, List<String> violations, String reasoning,
                           boolean fallback, OffsetDateTime createdAt) {
    }

    public record DocumentLink(long id, String fileName, String url) {
    }

    public record Dimensions(Integer length, Integer width, Integer height) {
    }

    public record VariantLine(String sku, String name, long price, int stock) {
    }

    /** Trang chi tiết duyệt (API 11.12). {@code decisionReason}: lý do từ chối / khóa đã lưu (AI hoặc người). */
    public record Detail(long productId, String name, String status, boolean isRequeued, OffsetDateTime submittedAt,
                         IdName shop, IdName assignee, boolean rejectedByAi, AiResult aiResult, String aiCheck,
                         String decisionReason, List<String> images, String videoUrl, List<DocumentLink> documents,
                         String categoryPath, String brand, String condition, String origin, String descriptionHtml,
                         List<SellerProductViews.Spec> specs, Integer weightGrams, Dimensions dimensionsCm,
                         String shipsFrom, String shippingPayer, List<VariantLine> variants) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final ProductRepository products;
    private final ProductListingStore store;
    private final ModeratorPool pool;
    private final CategoryCatalog catalog;
    private final CategoryFacade categories;
    private final BrandService brands;
    private final MediaUrls media;
    private final ObjectStorage storage;
    private final ObjectMapper json;
    private final AiScreeningSettings aiSettings;

    public ModerationQueueService(NamedParameterJdbcTemplate jdbc, ProductRepository products, ProductListingStore store,
                                  ModeratorPool pool, CategoryCatalog catalog, CategoryFacade categories,
                                  BrandService brands, MediaUrls media, ObjectStorage storage, ObjectMapper json,
                                  AiScreeningSettings aiSettings) {
        this.jdbc = jdbc;
        this.products = products;
        this.store = store;
        this.pool = pool;
        this.catalog = catalog;
        this.categories = categories;
        this.brands = brands;
        this.media = media;
        this.storage = storage;
        this.json = json;
        this.aiSettings = aiSettings;
    }

    // ===== Danh sách =====

    @Transactional(readOnly = true)
    public Page list(String tab, String q, String ai, Integer page, Integer size) {
        String t = tab == null || tab.isBlank() ? "PENDING" : tab.trim().toUpperCase();
        if (!TABS.contains(t)) {
            throw InvalidRequestException.of("tab", "INVALID_VALUE", tab, "Tab: PENDING, PUBLISHED hoặc REJECTED.");
        }
        AiCheck aiCheckFilter = parseAiFilter(ai);
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null ? 20 : Math.max(1, Math.min(size, 100));
        int threshold = aiSettings.rejectThreshold();
        String needle = q == null || q.isBlank() ? null : q.trim();
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("q", needle).addValue("limit", s)
                .addValue("offset", (p - 1) * s).addValue("th", threshold);
        // Khối văn bản """ cắt khoảng trắng đầu dòng → phải tự thêm dấu cách, nếu không thành "WHERE TRUEAND" (lỗi SQL)
        String search = needle == null ? "" : " " + """
                AND (immutable_unaccent(lower(p.name)) LIKE '%' || immutable_unaccent(lower(:q)) || '%'
                      OR immutable_unaccent(lower(vp.shop_name)) LIKE '%' || immutable_unaccent(lower(:q)) || '%'
                      OR CAST(p.id AS TEXT) = :q)""";

        Map<String, Long> counts = new LinkedHashMap<>();
        jdbc.query("""
                SELECT count(*) FILTER (WHERE p.status IN ('PENDING_APPROVAL', 'FLAGGED')) AS pending,
                       count(*) FILTER (WHERE p.status = 'PUBLISHED') AS published,
                       count(*) FILTER (WHERE p.status = 'REJECTED') AS rejected
                  FROM products p JOIN vendor_profiles vp ON vp.user_id = p.vendor_id WHERE TRUE""" + search,
                params, rs -> {
                    counts.put("PENDING", rs.getLong("pending"));
                    counts.put("PUBLISHED", rs.getLong("published"));
                    counts.put("REJECTED", rs.getLong("rejected"));
                });
        TABS.forEach(x -> counts.putIfAbsent(x, 0L));

        String filterAndOrder = switch (t) {
            case "PENDING" -> " AND p.status IN ('PENDING_APPROVAL', 'FLAGGED')"
                    + " ORDER BY (p.status = 'FLAGGED') DESC, p.submitted_at ASC NULLS LAST, p.id ASC";
            case "PUBLISHED" -> " AND p.status = 'PUBLISHED' ORDER BY p.updated_at DESC, p.id DESC";
            default -> " AND p.status = 'REJECTED' ORDER BY p.updated_at DESC, p.id DESC";
        };
        int orderAt = filterAndOrder.indexOf(" ORDER BY");
        String tabFilter = filterAndOrder.substring(0, orderAt);
        String aiFilter = aiFilterSql(aiCheckFilter);

        long total;
        if (aiCheckFilter == null) {
            total = counts.get(t);
        } else {
            Long c = jdbc.queryForObject("""
                    SELECT count(*) FROM products p JOIN vendor_profiles vp ON vp.user_id = p.vendor_id"""
                    + AI_JOIN + " WHERE TRUE" + tabFilter + search + aiFilter, params, Long.class);
            total = c == null ? 0L : c;
        }

        String sql = """
                SELECT p.id, p.name, p.status, p.is_requeued, p.moderator_id, p.submitted_at, vp.id AS shop_id,
                       vp.shop_name,
                       ai.decision AS ai_decision, ai.risk_score AS ai_risk, ai.fallback_to_manual AS ai_fallback,
                       (SELECT i.image_url FROM product_images i WHERE i.product_id = p.id AND i.variant_id IS NULL
                         ORDER BY i.is_primary DESC NULLS LAST, i.display_order, i.id LIMIT 1) AS image
                  FROM products p JOIN vendor_profiles vp ON vp.user_id = p.vendor_id"""
                + AI_JOIN + " WHERE TRUE" + tabFilter + search + aiFilter + filterAndOrder.substring(orderAt)
                + " LIMIT :limit OFFSET :offset";
        List<Map<String, Object>> raw = jdbc.queryForList(sql, params);
        Set<Long> staff = raw.stream().map(r -> (Number) r.get("moderator_id")).filter(java.util.Objects::nonNull)
                .map(Number::longValue).collect(Collectors.toSet());
        Map<Long, String> names = pool.names(staff);
        List<Row> rows = raw.stream().map(r -> {
            Long mod = r.get("moderator_id") == null ? null : ((Number) r.get("moderator_id")).longValue();
            String status = (String) r.get("status");
            String check = AiCheck.of(
                    (String) r.get("ai_decision"),
                    r.get("ai_risk") == null ? null : ((Number) r.get("ai_risk")).intValue(),
                    (Boolean) r.get("ai_fallback"),
                    threshold).name();
            return new Row(((Number) r.get("id")).longValue(), (String) r.get("name"), media.product((String) r.get("image")),
                    new IdName(((Number) r.get("shop_id")).longValue(), (String) r.get("shop_name")), status,
                    Boolean.TRUE.equals(r.get("is_requeued")),
                    mod == null ? null : new IdName(mod, names.getOrDefault(mod, "#" + mod)),
                    "REJECTED".equals(status) && mod == null, toOffset(r.get("submitted_at")), check);
        }).toList();
        return new Page(rows, PageResponse.Pagination.of(p, s, total), counts);
    }

    private static AiCheck parseAiFilter(String ai) {
        if (ai == null || ai.isBlank()) return null;
        try {
            return AiCheck.valueOf(ai.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw InvalidRequestException.of("ai", "INVALID_VALUE", ai,
                    "Nhãn AI: NOT_SCREENED, FAILED, PASSED, SUSPECTED, REJECTED.");
        }
    }

    private static String aiFilterSql(AiCheck check) {
        if (check == null) return "";
        return switch (check) {
            case NOT_SCREENED -> " AND ai.decision IS NULL";
            case FAILED -> " AND ai.fallback_to_manual = TRUE";
            case PASSED -> " AND ai.fallback_to_manual = FALSE AND ai.decision = 'APPROVE'";
            case SUSPECTED -> " AND ai.fallback_to_manual = FALSE"
                    + " AND (ai.decision = 'NEED_REVIEW' OR (ai.decision = 'REJECT' AND ai.risk_score < :th))";
            case REJECTED -> " AND ai.fallback_to_manual = FALSE AND ai.decision = 'REJECT' AND ai.risk_score >= :th";
        };
    }

    // ===== Chi tiết =====

    @Transactional(readOnly = true)
    public Detail detail(long productId) {
        Product p = products.findById(productId).filter(x -> x.getStatus() != ProductStatus.DELETED
                        && x.getStatus() != ProductStatus.DRAFT)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm."));
        Map<String, Object> shop = jdbc.queryForMap("SELECT id, shop_name FROM vendor_profiles WHERE user_id = :v",
                new MapSqlParameterSource("v", p.getVendorId()));
        IdName assignee = null;
        if (p.getModeratorId() != null) {
            String n = pool.names(List.of(p.getModeratorId())).getOrDefault(p.getModeratorId(), "#" + p.getModeratorId());
            assignee = new IdName(p.getModeratorId(), n);
        }
        List<AttributeView> attrs = p.getCategoryId() == null ? List.of() : categories.attributesFor(p.getCategoryId());
        List<DocumentLink> docs = store.documents(productId).stream()
                .map(d -> new DocumentLink(d.id(), d.fileName(),
                        storage.presignGet(UploadPurpose.PRODUCT_DOCUMENT.bucket(), d.fileUrl(), DOCUMENT_LINK_TTL)))
                .toList();
        List<VariantLine> variants = store.activeVariants(productId).stream()
                .map(v -> new VariantLine(v.sku(), v.attributes().isEmpty() ? p.getName()
                        : String.join(" / ", v.attributes().values()), v.price() == null ? 0 : v.price().longValue(),
                        v.stock()))
                .toList();
        String brand = p.getBrandId() == null ? p.getBrand()
                : brands.find(p.getBrandId()).map(BrandService.BrandView::name).orElse(p.getBrand());
        String status = p.getStatus().name();
        AiResult ai = currentAiResult(productId, p.getSubmittedAt());
        String aiCheck = AiCheck.of(
                ai == null ? null : ai.decision(),
                ai == null ? null : ai.riskScore(),
                ai == null ? null : ai.fallback(),
                aiSettings.rejectThreshold()).name();
        return new Detail(p.getId(), p.getName(), status, Boolean.TRUE.equals(p.getIsRequeued()), p.getSubmittedAt(),
                new IdName(((Number) shop.get("id")).longValue(), (String) shop.get("shop_name")), assignee,
                "REJECTED".equals(status) && p.getModeratorId() == null, ai, aiCheck,
                p.getModerationReason(), store.galleryKeys(productId).stream().map(media::product).toList(),
                media.product(p.getVideoUrl()), docs, categoryPath(p.getCategoryId()), brand,
                p.getItemCondition() == null ? "NEW" : p.getItemCondition().name(), p.getOrigin(), p.getDescription(),
                SpecCodec.toViews(SpecCodec.parse(json, p.getSpecsJson()), attrs), toInt(p.getWeight()),
                new Dimensions(toInt(p.getLength()), toInt(p.getWidth()), toInt(p.getHeight())), p.getShipsFrom(),
                p.getShippingPayer() == null ? "BUYER" : p.getShippingPayer().name(), variants);
    }

    // ===== Phân công =====

    /** Nhân viên đang nhận việc có ít sản phẩm chờ duyệt nhất (hòa → id nhỏ); {@code null} khi không ai nhận việc. */
    @Transactional(readOnly = true)
    public Long leastLoadedModerator() {
        List<Long> accepting = pool.moderators().stream().filter(ModeratorPool.Moderator::accepting)
                .map(ModeratorPool.Moderator::staffId).sorted().toList();
        if (accepting.isEmpty()) return null;
        Map<Long, Long> load = pendingLoad(accepting);
        return accepting.stream().min(Comparator.comparing((Long id) -> load.getOrDefault(id, 0L))
                .thenComparing(id -> id)).orElse(null);
    }

    /**
     * "Phân phối lại": chia đều mọi sản phẩm đang chờ cho người đang nhận việc theo vòng (AI nghi ngờ trước, rồi theo lúc
     * gửi duyệt). Không ai nhận việc → {@code 422}. Trả số sản phẩm đã đổi người duyệt.
     */
    @Transactional
    public int redistribute() {
        List<Long> accepting = pool.moderators().stream().filter(ModeratorPool.Moderator::accepting)
                .map(ModeratorPool.Moderator::staffId).sorted().toList();
        if (accepting.isEmpty()) {
            throw InvalidRequestException.of("moderators", "NONE_ACCEPTING", null,
                    "Chưa có nhân viên nào đang nhận việc — bật \"Đang nhận việc\" trước khi phân phối lại.");
        }
        List<Map<String, Object>> queue = jdbc.queryForList("""
                SELECT id, moderator_id FROM products WHERE status IN ('PENDING_APPROVAL', 'FLAGGED')
                 ORDER BY (status = 'FLAGGED') DESC, submitted_at ASC NULLS LAST, id ASC FOR UPDATE SKIP LOCKED""",
                new MapSqlParameterSource());
        int changed = 0;
        for (int i = 0; i < queue.size(); i++) {
            long target = accepting.get(i % accepting.size());
            Object current = queue.get(i).get("moderator_id");
            if (current != null && ((Number) current).longValue() == target) continue;
            changed += jdbc.update("UPDATE products SET moderator_id = :m, version = version + 1 WHERE id = :id",
                    new MapSqlParameterSource().addValue("m", target).addValue("id", queue.get(i).get("id")));
        }
        return changed;
    }

    private Map<Long, Long> pendingLoad(List<Long> staff) {
        Map<Long, Long> load = new HashMap<>();
        jdbc.query("""
                SELECT moderator_id, count(*) AS n FROM products
                 WHERE status IN ('PENDING_APPROVAL', 'FLAGGED') AND moderator_id IN (:ids) GROUP BY moderator_id""",
                new MapSqlParameterSource("ids", staff),
                rs -> {
                    load.put(rs.getLong("moderator_id"), rs.getLong("n"));
                });
        return load;
    }

    // ===== Tiện ích =====

    private AiResult currentAiResult(long productId, OffsetDateTime submittedAt) {
        if (submittedAt == null) return null;
        List<AiResult> rows = jdbc.query("""
                SELECT decision, risk_score, violations_json::text AS v, reasoning, fallback_to_manual, created_at
                  FROM product_moderation_results
                 WHERE product_id = :p AND created_at >= :since
                 ORDER BY created_at DESC, id DESC LIMIT 1""",
                new MapSqlParameterSource("p", productId).addValue("since", submittedAt),
                (rs, i) -> new AiResult(rs.getString("decision"), rs.getInt("risk_score"), violations(rs.getString("v")),
                        rs.getString("reasoning"), rs.getBoolean("fallback_to_manual"),
                        toOffset(rs.getTimestamp("created_at"))));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<String> violations(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            return json.readValue(raw, new TypeReference<List<String>>() { });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String categoryPath(Long categoryId) {
        if (categoryId == null) return null;
        CategoryIndex idx = catalog.index();
        List<String> names = new ArrayList<>();
        idx.ancestors(categoryId).forEach(r -> names.add(r.name()));
        idx.find(categoryId).map(CategoryRow::name).ifPresent(names::add);
        return String.join(" > ", names);
    }

    private static OffsetDateTime toOffset(Object v) {
        if (v == null) return null;
        if (v instanceof OffsetDateTime o) return o;
        if (v instanceof java.sql.Timestamp ts) return ts.toInstant().atOffset(ZoneOffset.UTC);
        return null;
    }

    private static Integer toInt(Double d) {
        return d == null ? null : (int) Math.round(d);
    }

}
