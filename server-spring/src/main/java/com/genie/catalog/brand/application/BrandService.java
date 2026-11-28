// Ô "Thương hiệu" của form đăng bán: tìm + "Thêm … làm thương hiệu mới" — API_SPEC 10.5 "Thương hiệu" (UC-CAT-09 bước 12; plan C1 D10)
package com.genie.catalog.brand.application;

import com.genie.catalog.brand.domain.Brand;
import com.genie.catalog.brand.domain.BrandRepository;
import com.genie.common.domain.Slugs;
import com.genie.common.error.InvalidRequestException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tìm theo tiền tố của khóa chuẩn hóa {@code brands.slug} (gõ "log" ra "Logitech", không phân biệt dấu / hoa thường), thương
 * hiệu đã xác minh lên trước. Shop thêm mới → {@link Brand#proposedBy} (chưa xác minh; Staff xác nhận khi duyệt sản phẩm
 * đầu tiên dùng nó — UC-CAT-13). Trùng khóa chuẩn hóa → trả bản có sẵn.
 */
@Service
public class BrandService {

    static final int MAX_NAME = 100;
    static final int MAX_SLUG = 120;

    public record BrandView(long id, String name) {
    }

    /** {@code created = false} khi tên đã có (API trả {@code 200} thay vì {@code 201}). */
    public record Saved(BrandView brand, boolean created) {
    }

    private final BrandRepository brands;
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public BrandService(BrandRepository brands, NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.brands = brands;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public List<BrandView> search(String q, Integer limit) {
        int size = limit == null ? 10 : Math.max(1, Math.min(limit, 20));
        String slug = q == null ? "" : truncate(Slugs.slugify(q), MAX_SLUG);
        return jdbc.query("""
                SELECT id, name FROM brands WHERE slug LIKE :prefix
                 ORDER BY is_verified DESC, (slug = :slug) DESC, name, id LIMIT :size""",
                new MapSqlParameterSource().addValue("prefix", slug.replace("%", "").replace("_", "\\_") + "%")
                        .addValue("slug", slug).addValue("size", size),
                (rs, i) -> new BrandView(rs.getLong("id"), rs.getString("name")));
    }

    @Transactional
    public Saved create(long vendorUserId, String rawName) {
        String name = rawName == null ? "" : rawName.trim().replaceAll("\\s+", " ");
        if (name.isEmpty() || name.length() > MAX_NAME) {
            throw InvalidRequestException.of("name", name.isEmpty() ? "REQUIRED" : "TOO_LONG", rawName,
                    "Tên thương hiệu từ 1 đến " + MAX_NAME + " ký tự.");
        }
        String slug = truncate(Slugs.slugify(name), MAX_SLUG);
        if (slug.isEmpty()) {
            throw InvalidRequestException.of("name", "INVALID_FORMAT", rawName, "Tên thương hiệu phải có chữ hoặc số.");
        }
        Optional<Brand> existing = brands.findBySlug(slug);
        if (existing.isPresent()) {
            return new Saved(view(existing.get()), false);
        }
        // hai shop thêm cùng một tên đúng cùng lúc: UNIQUE uq_brand_slug chặn bản thứ hai (lỗi xung đột, người dùng thử lại)
        Brand saved = brands.saveAndFlush(Brand.proposedBy(name, slug, vendorUserId, OffsetDateTime.now(clock)));
        return new Saved(view(saved), true);
    }

    /** Tên thương hiệu theo id (form sửa, lưu cột {@code products.brand} cho tìm kiếm); rỗng nếu không có. */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Optional<BrandView> find(long id) {
        return brands.findById(id).map(BrandService::view);
    }

    /** Thương hiệu shop tự thêm chưa xác minh → xác minh (Staff duyệt sản phẩm dùng nó — UC-CAT-13 bước 6). */
    @Transactional
    public void verifyIfProposed(long id) {
        brands.findById(id).filter(b -> !Boolean.TRUE.equals(b.getIsVerified())).ifPresent(Brand::verify);
    }

    private static BrandView view(Brand b) {
        return new BrandView(b.getId(), b.getName());
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max).replaceAll("-+$", "");
    }
}
