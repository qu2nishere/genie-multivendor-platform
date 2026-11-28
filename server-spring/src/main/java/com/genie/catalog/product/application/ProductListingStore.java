// Đọc / ghi bảng con của sản phẩm (ảnh, biến thể, giấy tờ) bằng SQL chọn cột — plan C1 D5, D6, D7, mục 10.3
package com.genie.catalog.product.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.catalog.product.domain.VariantAttributes;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Biến thể <b>không</b> sửa qua entity JPA được quản lý: {@code ProductVariant} không có {@code @DynamicUpdate} nên lưu
 * entity sẽ ghi đè {@code reserved_count} mà module order vừa tăng bằng SQL (giữ chỗ đơn). Mọi câu ghi ở đây chỉ chạm cột
 * của người bán, không bao giờ ghi {@code reserved_count}, và tăng {@code version} (plan C1 mục 10.3).
 */
@Component
public class ProductListingStore {

    /** Biến thể còn bán ({@code deleted_at IS NULL}); {@code attributes} giữ thứ tự khóa đọc được từ JSONB. */
    public record VariantRow(long id, String sku, BigDecimal price, int stock, int reserved, Map<String, String> attributes,
                      String imageUrl) {
    }

    public record DocumentRow(long id, String fileUrl, String fileName) {
    }

    private static final TypeReference<LinkedHashMap<String, Object>> MAP = new TypeReference<>() { };

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;

    ProductListingStore(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    // ===== Ảnh =====

    /** Khóa ảnh gallery (không gắn biến thể) theo thứ tự hiển thị — ảnh đầu là ảnh chính. */
    public List<String> galleryKeys(long productId) {
        return jdbc.queryForList("""
                SELECT image_url FROM product_images WHERE product_id = :p AND variant_id IS NULL
                 ORDER BY display_order, id""", new MapSqlParameterSource("p", productId), String.class);
    }

    /** Thay toàn bộ gallery (xóa dòng cũ, ghi lại theo thứ tự). Tệp cũ trên kho giữ nguyên (plan C1 D6). */
    void replaceGallery(long productId, List<String> keys, String altText) {
        jdbc.update("DELETE FROM product_images WHERE product_id = :p AND variant_id IS NULL",
                new MapSqlParameterSource("p", productId));
        for (int i = 0; i < keys.size(); i++) {
            jdbc.update("""
                    INSERT INTO product_images (product_id, image_url, alt_text, is_primary, display_order)
                    VALUES (:p, :url, :alt, :primary, :ord)""",
                    new MapSqlParameterSource().addValue("p", productId).addValue("url", keys.get(i))
                            .addValue("alt", altText).addValue("primary", i == 0).addValue("ord", i));
        }
    }

    // ===== Biến thể =====

    public List<VariantRow> activeVariants(long productId) {
        return jdbc.query("""
                SELECT id, sku, price, stock_count, reserved_count, attributes_json::text AS attrs, variant_name, image_url
                  FROM product_variants WHERE product_id = :p AND deleted_at IS NULL ORDER BY id""",
                new MapSqlParameterSource("p", productId),
                // thứ tự nhóm theo variant_name — JSONB tự sắp khóa (vd "Màu sắc" lên trước "Phiên bản")
                (rs, i) -> new VariantRow(rs.getLong("id"), rs.getString("sku"), rs.getBigDecimal("price"),
                        rs.getInt("stock_count"), rs.getInt("reserved_count"),
                        VariantAttributes.ordered(attributes(rs.getString("attrs")), rs.getString("variant_name")),
                        rs.getString("image_url")));
    }

    /** SKU đã có ở biến thể khác (kể cả đã xóa mềm — cột UNIQUE toàn bảng). */
    boolean skuTaken(String sku, Long exceptVariantId) {
        Integer n = jdbc.queryForObject("""
                SELECT count(*) FROM product_variants WHERE sku = :sku AND (CAST(:except AS BIGINT) IS NULL OR id <> :except)""",
                new MapSqlParameterSource().addValue("sku", sku).addValue("except", exceptVariantId), Integer.class);
        return n != null && n > 0;
    }

    /** Số thứ tự kế tiếp cho mã {@code G{productId}-n} (không tái dùng số của biến thể đã xóa mềm). */
    int nextGeneratedSequence(long productId, String prefix) {
        Integer max = jdbc.queryForObject("""
                SELECT COALESCE(MAX(CAST(substring(sku FROM :start) AS INTEGER)), 0) FROM product_variants
                 WHERE product_id = :p AND sku LIKE :like AND substring(sku FROM :start) ~ '^[0-9]+$'""",
                new MapSqlParameterSource().addValue("p", productId).addValue("like", prefix + "%")
                        .addValue("start", prefix.length() + 1), Integer.class);
        return (max == null ? 0 : max) + 1;
    }

    long insertVariant(long productId, String sku, String variantName, Map<String, String> attributes, BigDecimal price,
                       int stock, String imageUrl) {
        Long id = jdbc.queryForObject("""
                INSERT INTO product_variants (product_id, sku, variant_name, attributes_json, price, stock_count,
                                              reserved_count, version, image_url)
                VALUES (:p, :sku, :name, CAST(:attrs AS jsonb), :price, :stock, 0, 0, :img) RETURNING id""",
                new MapSqlParameterSource().addValue("p", productId).addValue("sku", sku).addValue("name", variantName)
                        .addValue("attrs", toJson(attributes)).addValue("price", price).addValue("stock", stock)
                        .addValue("img", imageUrl), Long.class);
        return id == null ? 0 : id;
    }

    /**
     * Cập nhật cột của người bán; 0 dòng khi biến thể không thuộc sản phẩm / đã xóa / tồn mới nhỏ hơn số đang giữ chỗ.
     */
    int updateVariant(long productId, long variantId, String sku, String variantName, Map<String, String> attributes,
                      BigDecimal price, int stock, String imageUrl) {
        return jdbc.update("""
                UPDATE product_variants SET sku = :sku, variant_name = :name, attributes_json = CAST(:attrs AS jsonb),
                       price = :price, stock_count = :stock, image_url = :img, version = version + 1
                 WHERE id = :id AND product_id = :p AND deleted_at IS NULL AND :stock >= reserved_count""",
                new MapSqlParameterSource().addValue("id", variantId).addValue("p", productId).addValue("sku", sku)
                        .addValue("name", variantName).addValue("attrs", toJson(attributes)).addValue("price", price)
                        .addValue("stock", stock).addValue("img", imageUrl));
    }

    /** Bỏ biến thể: tồn về 0 + xóa mềm; 0 dòng khi còn giữ chỗ của đơn (plan C1 D5). */
    int softRemoveVariant(long productId, long variantId, OffsetDateTime now) {
        return jdbc.update("""
                UPDATE product_variants SET stock_count = 0, deleted_at = :now, version = version + 1
                 WHERE id = :id AND product_id = :p AND deleted_at IS NULL AND reserved_count = 0""",
                new MapSqlParameterSource().addValue("id", variantId).addValue("p", productId)
                        .addValue("now", Timestamp.from(now.toInstant())));
    }

    // ===== Giấy tờ =====

    public List<DocumentRow> documents(long productId) {
        return jdbc.query("SELECT id, file_url, file_name FROM product_documents WHERE product_id = :p ORDER BY id",
                new MapSqlParameterSource("p", productId),
                (rs, i) -> new DocumentRow(rs.getLong("id"), rs.getString("file_url"), rs.getString("file_name")));
    }

    void deleteDocuments(long productId, Collection<Long> ids) {
        if (ids.isEmpty()) return;
        jdbc.update("DELETE FROM product_documents WHERE product_id = :p AND id IN (:ids)",
                new MapSqlParameterSource().addValue("p", productId).addValue("ids", ids));
    }

    void insertDocument(long productId, String fileUrl, String fileName, String extension, long size,
                        OffsetDateTime now) {
        jdbc.update("""
                INSERT INTO product_documents (product_id, document_type, file_url, file_name, file_extension,
                                               file_size_bytes, uploaded_at)
                VALUES (:p, 'OTHER', :url, :name, :ext, :size, :now)""",
                new MapSqlParameterSource().addValue("p", productId).addValue("url", fileUrl).addValue("name", fileName)
                        .addValue("ext", extension).addValue("size", size)
                        .addValue("now", Timestamp.from(now.toInstant())));
    }

    // ===== JSON =====

    Map<String, String> attributes(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return out;
        try {
            json.readValue(raw, MAP).forEach((k, v) -> {
                if (v != null) out.put(k, String.valueOf(v));
            });
        } catch (Exception e) {
            // dữ liệu cũ hỏng: coi như không có phân loại
        }
        return out;
    }

    String toJson(Object value) {
        try {
            return value == null ? null : json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Không ghi được JSON", e);
        }
    }
}
