package com.genie.catalog.wishlist.application;

import com.genie.catalog.product.domain.ProductImageRepository;
import com.genie.catalog.product.domain.ProductRepository;
import com.genie.catalog.product.domain.ProductStatus;
import com.genie.catalog.product.domain.ProductSummaryRows.ImageRow;
import com.genie.catalog.product.domain.ProductSummaryRows.ProductRow;
import com.genie.catalog.product.domain.ProductSummaryRows.VariantRow;
import com.genie.catalog.product.domain.ProductVariantRepository;
import com.genie.catalog.wishlist.domain.UserWishlist;
import com.genie.catalog.wishlist.domain.UserWishlistRepository;
import com.genie.common.api.CursorPage;
import com.genie.catalog.search.domain.SlugUtil;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.storage.ObjectStorage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc danh sách yêu thích của chính người gọi — UC-CAT-08 (API_SPEC 3.10; plan wishlist W02.03, W03). */
@Service
public class WishlistQueryService {

    /** Tối đa số ID trả cho client để tô tim (D5). */
    public static final int MAX_IDS = 1000;
    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;
    /** Bucket ảnh sản phẩm (LUU_TRU 5) — CSDL lưu khóa đối tượng, API ghép URL công khai (N5). */
    static final String PRODUCT_MEDIA = "product-media";

    private final UserWishlistRepository wishlists;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final ProductImageRepository images;
    private final WishlistShopAvailability shops;
    private final ObjectStorage storage;

    public WishlistQueryService(UserWishlistRepository wishlists, ProductRepository products,
                                ProductVariantRepository variants, ProductImageRepository images,
                                WishlistShopAvailability shops, ObjectStorage storage) {
        this.wishlists = wishlists;
        this.products = products;
        this.variants = variants;
        this.images = images;
        this.shops = shops;
        this.storage = storage;
    }

    /** Trang danh sách + {@code summary.total}. */
    public record ListResult(CursorPage<WishlistItemView> page, long total) {
    }

    /** {@code GET /me/wishlist/product-ids} — tối đa 1.000 ID mới nhất. */
    @Transactional(readOnly = true)
    public List<Long> productIds(long userId) {
        return wishlists.findProductIds(userId, PageRequest.of(0, MAX_IDS));
    }

    /**
     * {@code GET /me/wishlist?cursor&limit} — mới thích trước. Sản phẩm / SKU / ảnh nạp THEO LÔ (một truy vấn mỗi bảng cho cả
     * trang, dạng projection không nạp entity) — chống N+1.
     */
    @Transactional(readOnly = true)
    public ListResult list(long userId, String cursor, Integer limit) {
        int size = limit == null ? DEFAULT_LIMIT : limit;
        if (size < 1 || size > MAX_LIMIT) {
            throw InvalidRequestException.of("limit", "OUT_OF_RANGE", limit, "limit phải từ 1 đến 50.");
        }
        PageRequest page = PageRequest.of(0, size + 1);
        List<UserWishlist> rows;
        if (cursor == null || cursor.isBlank()) {
            rows = wishlists.findPage(userId, page);
        } else {
            Cursor c = Cursor.decode(cursor.trim());
            rows = wishlists.findPageAfter(userId, c.createdAt(), c.id(), page);
        }
        boolean hasMore = rows.size() > size;
        List<UserWishlist> pageRows = hasMore ? rows.subList(0, size) : rows;
        String next = hasMore ? new Cursor(pageRows.get(size - 1).getCreatedAt(), pageRows.get(size - 1).getId()).encode()
                : null;

        List<Long> ids = pageRows.stream().map(UserWishlist::getProductId).distinct().toList();
        List<WishlistItemView> items = ids.isEmpty() ? List.of() : buildItems(ids);
        return new ListResult(new CursorPage<>(items, new CursorPage.Pagination(size, hasMore, next)),
                wishlists.countByUserId(userId));
    }

    private List<WishlistItemView> buildItems(List<Long> ids) {
        Map<Long, ProductRow> byId = products.findRowsByIds(ids).stream()
                .collect(Collectors.toMap(ProductRow::getId, Function.identity()));
        Map<Long, List<VariantRow>> skus = variants.findActiveRows(ids).stream()
                .collect(Collectors.groupingBy(VariantRow::getProductId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, String> image = new LinkedHashMap<>();
        for (ImageRow i : images.findGalleryRows(ids)) {
            image.putIfAbsent(i.getProductId(), i.getImageUrl());   // phần tử đầu = ảnh chính
        }
        Set<Long> onVacation = shops.vendorIdsOnVacation(byId.values().stream().map(ProductRow::getVendorId)
                .collect(Collectors.toSet()));

        List<WishlistItemView> out = new ArrayList<>();
        for (Long id : ids) {
            ProductRow p = byId.get(id);
            if (p == null) {
                continue;   // sản phẩm đã bị xóa cứng — bỏ dòng
            }
            List<VariantRow> list = skus.getOrDefault(id, List.of());
            long price = list.stream().map(v -> v.getPrice().longValue()).min(Long::compare)
                    .orElse(p.getBasePrice().longValue());
            Comparator<VariantRow> cheapest = Comparator.comparing(VariantRow::getPrice).thenComparing(VariantRow::getId);
            Long defaultVariant = list.stream().filter(v -> available(v) > 0).min(cheapest).map(VariantRow::getId)
                    .orElse(list.isEmpty() ? null : list.get(0).getId());
            boolean inStock = list.stream().anyMatch(v -> available(v) > 0);
            boolean purchasable = p.getStatus() == ProductStatus.PUBLISHED && inStock
                    && !onVacation.contains(p.getVendorId());
            out.add(new WishlistItemView(id, SlugUtil.slugify(p.getName()), p.getName(), url(image.get(id)), price,
                    defaultVariant, purchasable));
        }
        return out;
    }

    private static int available(VariantRow v) {
        return Math.max(0, v.getStockCount() - (v.getReservedCount() == null ? 0 : v.getReservedCount()));
    }

    private String url(String key) {
        if (key == null) {
            return null;
        }
        return key.startsWith("http://") || key.startsWith("https://") ? key : storage.publicUrl(PRODUCT_MEDIA, key);
    }

    /** Con trỏ = Base64URL({@code <created_at ISO>|<id>}). Hỏng → 422 trường {@code cursor}. */
    record Cursor(OffsetDateTime createdAt, long id) {

        String encode() {
            String raw = createdAt.toInstant() + "|" + id;
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }

        static Cursor decode(String value) {
            try {
                String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
                int sep = raw.indexOf('|');
                return new Cursor(Instant.parse(raw.substring(0, sep)).atOffset(ZoneOffset.UTC),
                        Long.parseLong(raw.substring(sep + 1)));
            } catch (RuntimeException e) {
                throw InvalidRequestException.of("cursor", "INVALID_FORMAT", value, "Con trỏ phân trang không hợp lệ.");
            }
        }
    }
}
