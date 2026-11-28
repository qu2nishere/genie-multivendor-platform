// Dựng ScreeningRequest từ CSDL và kho ảnh (KHÔNG đọc dữ liệu cá nhân chủ shop — N10) — UC-CAT-12 (plan C1 P06; update_gemini.md mục 11.3)
package com.genie.catalog.moderation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.catalog.category.application.CategoryCatalog;
import com.genie.catalog.category.application.CategoryIndex;
import com.genie.catalog.category.application.CategoryRow;
import com.genie.catalog.product.application.OptionImages;
import com.genie.catalog.product.application.SpecCodec;
import com.genie.common.storage.ObjectStorage;
import com.genie.common.storage.UploadPurpose;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tách bean riêng để mở {@code @Transactional(readOnly = true)} trước khi gọi mạng sang Gemini (mục 4.8).
 * Gom tối đa 8 ảnh (ảnh chung ≤ 4, ảnh giá trị phân loại ≤ 2, ảnh SKU ≤ 1, ảnh trong mô tả ≤ 1), thu nhỏ qua
 * {@link ScreeningImagePort} (B-D3, B-D4), tuyệt đối không đọc {@code users} / {@code vendor_profiles}.
 */
@Component
public class ScreeningRequestBuilder {

    private static final Logger log = LoggerFactory.getLogger(ScreeningRequestBuilder.class);
    private static final int MAX_DESCRIPTION_CHARS = 4_000;
    private static final int MAX_VARIANTS = 20;
    private static final int MAX_GALLERY_IMAGES = 4;
    private static final int MAX_OPTION_IMAGES = 2;
    private static final int MAX_SKU_IMAGES = 1;
    private static final int MAX_DESC_IMAGES = 1;
    private static final int MAX_TOTAL_IMAGES = 8;
    private static final int MAX_TOTAL_IMAGE_BYTES = 9 * 1024 * 1024; // ≈ 12 MB khi mã hóa base64 (B-D3)

    private record Candidate(String key, String label) {
    }

    private record ImageBatch(List<ScreeningRequest.Image> images, int skipped) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final CategoryCatalog catalog;
    private final ObjectStorage storage;
    private final ObjectMapper json;
    private final ScreeningImagePort imagePort;

    public ScreeningRequestBuilder(NamedParameterJdbcTemplate jdbc, CategoryCatalog catalog,
                                   ObjectStorage storage, ObjectMapper json, ScreeningImagePort imagePort) {
        this.jdbc = jdbc;
        this.catalog = catalog;
        this.storage = storage;
        this.json = json;
        this.imagePort = imagePort;
    }

    /** Trả {@code null} nếu sản phẩm không tồn tại hoặc không còn ở trạng thái {@code PENDING_APPROVAL}. */
    @Transactional(readOnly = true)
    public ScreeningRequest build(long productId) {
        MapSqlParameterSource p = new MapSqlParameterSource("p", productId);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.name, p.brand, b.name AS brand_name, p.item_condition, p.description,
                       p.specs_json::text AS specs_json, p.option_images_json::text AS option_images_json,
                       p.category_id, p.status
                  FROM products p
                  LEFT JOIN brands b ON b.id = p.brand_id
                 WHERE p.id = :p""", p);
        if (rows.isEmpty()) return null;
        Map<String, Object> row = rows.get(0);
        if (!"PENDING_APPROVAL".equals(row.get("status"))) return null;

        String name = (String) row.get("name");
        String brand = (String) row.get("brand");
        if (brand == null || brand.isBlank()) {
            brand = (String) row.get("brand_name");
        }
        String condition = (String) row.get("item_condition");
        Long categoryId = row.get("category_id") == null ? null : ((Number) row.get("category_id")).longValue();
        String categoryPath = categoryPath(categoryId);
        String descriptionHtml = (String) row.get("description");
        String descriptionText = plainDescription(descriptionHtml);
        Map<String, String> specs = SpecCodec.parse(json, (String) row.get("specs_json"));

        long minPrice = 0L;
        long maxPrice = 0L;
        List<Map<String, Object>> prices = jdbc.queryForList("""
                SELECT min(price) AS min_p, max(price) AS max_p
                  FROM product_variants
                 WHERE product_id = :p AND deleted_at IS NULL""", p);
        if (!prices.isEmpty()) {
            Object minObj = prices.get(0).get("min_p");
            Object maxObj = prices.get(0).get("max_p");
            if (minObj instanceof BigDecimal bd) minPrice = bd.longValue();
            else if (minObj instanceof Number n) minPrice = n.longValue();
            if (maxObj instanceof BigDecimal bd) maxPrice = bd.longValue();
            else if (maxObj instanceof Number n) maxPrice = n.longValue();
        }

        List<String> variantNames = jdbc.queryForList("""
                SELECT variant_name FROM product_variants
                 WHERE product_id = :p AND deleted_at IS NULL AND variant_name IS NOT NULL AND variant_name <> ''
                 ORDER BY id
                 LIMIT :lim""",
                new MapSqlParameterSource("p", productId).addValue("lim", MAX_VARIANTS), String.class);

        List<String> documentNames = jdbc.queryForList("""
                SELECT file_name FROM product_documents WHERE product_id = :p ORDER BY id""", p, String.class);

        ImageBatch batch = collectImages(productId, descriptionHtml, (String) row.get("option_images_json"));

        return new ScreeningRequest(productId, name, categoryPath, brand, condition, minPrice, maxPrice,
                descriptionText, specs, variantNames, documentNames, batch.images(), batch.skipped());
    }

    private ImageBatch collectImages(long productId, String descriptionHtml, String optionImagesJson) {
        MapSqlParameterSource p = new MapSqlParameterSource("p", productId);
        String bucket = UploadPurpose.PRODUCT_IMAGE.bucket();
        List<Candidate> candidates = new ArrayList<>();
        Set<String> seenKeys = new LinkedHashSet<>();
        int skipped = 0;

        // 1. Ảnh chung (≤ 4, ảnh đầu = "ảnh bìa", thừa → skipped)
        List<String> gallery = jdbc.queryForList("""
                SELECT image_url FROM product_images
                 WHERE product_id = :p AND variant_id IS NULL
                 ORDER BY is_primary DESC NULLS LAST, display_order, id""", p, String.class);
        int takenGallery = 0;
        for (String key : gallery) {
            if (key == null || key.isBlank()) continue;
            if (!seenKeys.add(key)) continue;
            if (takenGallery < MAX_GALLERY_IMAGES) {
                candidates.add(new Candidate(key, takenGallery == 0 ? "ảnh bìa" : "ảnh sản phẩm"));
                takenGallery++;
            } else {
                skipped++;
            }
        }

        // 2. Ảnh giá trị phân loại từ option_images_json (≤ 2)
        Map<String, Map<String, String>> optImages = OptionImages.parse(json, optionImagesJson);
        int takenOption = 0;
        for (Map.Entry<String, Map<String, String>> group : optImages.entrySet()) {
            for (Map.Entry<String, String> val : group.getValue().entrySet()) {
                String key = val.getValue();
                if (key == null || key.isBlank()) continue;
                if (!seenKeys.add(key)) continue;
                if (takenOption < MAX_OPTION_IMAGES) {
                    candidates.add(new Candidate(key,
                            "ảnh phân loại \"" + group.getKey() + ": " + val.getKey() + "\""));
                    takenOption++;
                } else {
                    skipped++;
                }
            }
        }

        // 3. Ảnh SKU (product_images.variant_id IS NOT NULL, ≤ 1)
        List<Map<String, Object>> skuRows = jdbc.queryForList("""
                SELECT pi.image_url, pv.variant_name
                  FROM product_images pi
                  JOIN product_variants pv ON pv.id = pi.variant_id
                 WHERE pi.product_id = :p AND pv.deleted_at IS NULL
                 ORDER BY pi.id""", p);
        int takenSku = 0;
        for (Map<String, Object> r : skuRows) {
            String key = (String) r.get("image_url");
            if (key == null || key.isBlank()) continue;
            if (!seenKeys.add(key)) continue;
            if (takenSku < MAX_SKU_IMAGES) {
                String vname = (String) r.get("variant_name");
                String label = vname == null || vname.isBlank()
                        ? "ảnh phân loại"
                        : "ảnh phân loại \"" + vname + "\"";
                candidates.add(new Candidate(key, label));
                takenSku++;
            } else {
                skipped++;
            }
        }

        // 4. Ảnh chèn trong mô tả (<img src> thuộc bucket product-media, ≤ 1; URL lạ không tải → skipped)
        if (descriptionHtml != null && !descriptionHtml.isBlank()) {
            String prefix = storage.publicUrl(bucket, "");
            int takenDesc = 0;
            for (Element img : Jsoup.parse(descriptionHtml).select("img[src]")) {
                String src = img.attr("src").trim();
                if (src.isEmpty()) continue;
                if (!src.startsWith(prefix)) {
                    skipped++;
                    continue;
                }
                String key = src.substring(prefix.length());
                if (key.isBlank()) {
                    skipped++;
                    continue;
                }
                if (!seenKeys.add(key)) continue;
                if (takenDesc < MAX_DESC_IMAGES) {
                    candidates.add(new Candidate(key, "ảnh trong mô tả"));
                    takenDesc++;
                } else {
                    skipped++;
                }
            }
        }

        // 5. Đọc từ kho và thu nhỏ, dừng khi đủ 8 ảnh hoặc tổng > 9 MB thô
        List<ScreeningRequest.Image> out = new ArrayList<>();
        int totalBytes = 0;
        for (int i = 0; i < candidates.size(); i++) {
            if (out.size() >= MAX_TOTAL_IMAGES) {
                skipped += candidates.size() - i;
                break;
            }
            Candidate c = candidates.get(i);
            String mime = mimeFromKey(c.key());
            if (mime == null) {
                skipped++;
                continue;
            }
            try {
                Optional<byte[]> raw = storage.get(bucket, c.key());
                if (raw.isEmpty()) {
                    log.debug("Bỏ ảnh {} khi sàng lọc AI: không tìm thấy trong kho {}", c.key(), bucket);
                    skipped++;
                    continue;
                }
                Optional<ScreeningImagePort.Image> prepared = imagePort.prepare(raw.get(), mime);
                if (prepared.isEmpty()) {
                    skipped++;
                    continue;
                }
                ScreeningImagePort.Image ready = prepared.get();
                if (totalBytes + ready.data().length > MAX_TOTAL_IMAGE_BYTES) {
                    skipped += candidates.size() - i;
                    break;
                }
                totalBytes += ready.data().length;
                out.add(new ScreeningRequest.Image(ready.mimeType(), ready.data(), c.label()));
            } catch (RuntimeException e) {
                log.debug("Bỏ ảnh {} khi sàng lọc AI do lỗi đọc kho: {}", c.key(), e.getMessage());
                skipped++;
            }
        }
        return new ImageBatch(out, skipped);
    }

    private String categoryPath(Long categoryId) {
        if (categoryId == null) return null;
        CategoryIndex idx = catalog.index();
        List<String> names = new ArrayList<>();
        idx.ancestors(categoryId).forEach(r -> names.add(r.name()));
        idx.find(categoryId).map(CategoryRow::name).ifPresent(names::add);
        return names.isEmpty() ? null : String.join(" › ", names);
    }

    private static String plainDescription(String html) {
        if (html == null || html.isBlank()) return "";
        String text = Jsoup.parse(html).text().replaceAll("\\s+", " ").trim();
        return text.length() <= MAX_DESCRIPTION_CHARS ? text : text.substring(0, MAX_DESCRIPTION_CHARS);
    }

    private static String mimeFromKey(String key) {
        int dot = key.lastIndexOf('.');
        if (dot < 0 || dot == key.length() - 1) return null;
        String ext = key.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> null;
        };
    }
}
