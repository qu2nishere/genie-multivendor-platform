package com.genie.admin.banner.application;

import com.genie.admin.banner.domain.BannerPosition;
import com.genie.admin.banner.domain.HomeBanner;
import com.genie.admin.banner.domain.HomeBannerRepository;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.storage.UploadPurpose;
import com.genie.common.storage.UploadService;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc banner — màn quản lý của Admin (API_SPEC 11.16; plan banner B03.06). Ánh xạ entity → {@link BannerView} dùng chung
 * cho các API quản trị.
 */
@Service
public class BannerQueryService {

    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 60;
    private static final Sort ADMIN_ORDER = Sort.by(Sort.Order.asc("position"), Sort.Order.asc("sortOrder"),
            Sort.Order.desc("id"));

    private final HomeBannerRepository banners;
    private final UploadService uploads;
    private final ObjectProvider<BannerCategoryLookup> categories;
    private final BannerCache cache;
    private final Clock clock;

    public BannerQueryService(HomeBannerRepository banners, UploadService uploads,
                              ObjectProvider<BannerCategoryLookup> categories, BannerCache cache, Clock clock) {
        this.banners = banners;
        this.uploads = uploads;
        this.categories = categories;
        this.cache = cache;
        this.clock = clock;
    }

    /** {@code GET /admin/banners?position&q&page&size} — {@code page} từ 1, {@code size} 1–60 (mặc định 20). */
    @Transactional(readOnly = true)
    public Page<BannerView> search(BannerPosition position, String q, Integer page, Integer size) {
        int p = page == null ? 1 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        List<InvalidRequestException.Violation> errors = new ArrayList<>();
        if (p < 1) {
            errors.add(new InvalidRequestException.Violation("page", "OUT_OF_RANGE", page, "page phải từ 1."));
        }
        if (s < 1 || s > MAX_SIZE) {
            errors.add(new InvalidRequestException.Violation("size", "OUT_OF_RANGE", size, "size phải từ 1 đến 60."));
        }
        if (!errors.isEmpty()) {
            throw new InvalidRequestException(errors);
        }
        String term = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        Specification<HomeBanner> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (position != null) {
                preds.add(cb.equal(root.get("position"), position));
            }
            if (term != null) {
                String pattern = "%" + term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                preds.add(cb.like(cb.lower(root.get("title")), pattern, '\\'));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
        Page<HomeBanner> rows = banners.findAll(spec, PageRequest.of(p - 1, s, ADMIN_ORDER));
        Map<Long, String> names = categoryNames(rows.getContent().stream().map(HomeBanner::getCategoryId)
                .filter(Objects::nonNull).toList());
        OffsetDateTime now = OffsetDateTime.now(clock);
        return rows.map(b -> toView(b, names, now));
    }

    /**
     * {@code GET /public/banners} (API_SPEC 5.14): theo từng vị trí, banner {@code LIVE} sắp {@code sort_order} — đọc
     * cache trước, trượt thì đọc PostgreSQL rồi ghi cache (cache-aside, LUU_TRU 4.5). Vị trí không có banner → mảng rỗng.
     */
    @Transactional(readOnly = true)
    public Map<BannerPosition, List<PublicBannerView>> live(Collection<BannerPosition> positions, Long categoryId) {
        Map<BannerPosition, List<PublicBannerView>> out = new java.util.LinkedHashMap<>();
        OffsetDateTime now = OffsetDateTime.now(clock);
        for (BannerPosition position : positions) {
            String key = BannerCache.key(position, categoryId);
            List<PublicBannerView> list = cache.get(key).orElseGet(() -> {
                List<HomeBanner> rows = position == BannerPosition.CATEGORY_TOP
                        ? banners.findLiveInCategory(position, categoryId, now)
                        : banners.findLive(position, now);
                List<PublicBannerView> views = rows.stream().map(b -> new PublicBannerView(b.getId(), b.getTitle(),
                        imageUrl(b.getImageUrl()), imageUrl(b.getMobileImageUrl()), b.getTargetUrl())).toList();
                cache.put(key, views);
                return views;
            });
            out.put(position, list);
        }
        return out;
    }

    /** Ánh xạ một banner (tra tên danh mục riêng lẻ). */
    public BannerView view(HomeBanner b) {
        Map<Long, String> names = b.getCategoryId() == null ? Map.of() : categoryNames(List.of(b.getCategoryId()));
        return toView(b, names, OffsetDateTime.now(clock));
    }

    /** URL công khai của khóa ảnh trong {@code content-assets}; {@code null} giữ nguyên. */
    public String imageUrl(String objectKey) {
        return objectKey == null ? null : uploads.publicUrl(UploadPurpose.BANNER_IMAGE, objectKey);
    }

    private BannerView toView(HomeBanner b, Map<Long, String> names, OffsetDateTime now) {
        BannerView.CategoryRef category = b.getCategoryId() == null ? null
                : new BannerView.CategoryRef(b.getCategoryId(), names.get(b.getCategoryId()));
        return new BannerView(b.getId(), b.getTitle(), imageUrl(b.getImageUrl()), imageUrl(b.getMobileImageUrl()),
                b.getTargetUrl(), b.getPosition(), category, b.getSortOrder(),
                b.getStartsAt().atZoneSameInstant(VN).toOffsetDateTime(),
                b.getEndsAt().atZoneSameInstant(VN).toOffsetDateTime(), Boolean.TRUE.equals(b.getIsActive()),
                b.state(now), b.getImpressionCount(), b.getClickCount());
    }

    private Map<Long, String> categoryNames(Collection<Long> ids) {
        BannerCategoryLookup lookup = categories.getIfAvailable();
        return lookup == null || ids.isEmpty() ? Map.of() : lookup.namesOf(ids);
    }
}
