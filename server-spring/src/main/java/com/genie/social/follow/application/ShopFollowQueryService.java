package com.genie.social.follow.application;

import com.genie.common.api.CursorPage;
import com.genie.common.error.InvalidRequestException;
import com.genie.social.follow.application.ShopFollowDirectory.FollowedShopView;
import com.genie.social.follow.domain.ShopFollow;
import com.genie.social.follow.domain.ShopFollowRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc danh sách shop đang theo dõi của chính người gọi — UC-SOC-06 (API_SPEC 3.11; plan theo dõi gian hàng F02.03, F03). */
@Service
public class ShopFollowQueryService {

    static final int MAX_IDS = 1000;
    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;

    private final ShopFollowRepository follows;
    private final ShopFollowDirectory shops;

    public ShopFollowQueryService(ShopFollowRepository follows, ShopFollowDirectory shops) {
        this.follows = follows;
        this.shops = shops;
    }

    /** {@code $ShopMini} (API mục 1). */
    public record ShopMini(long id, String name, String logoUrl, boolean isMall) {
    }

    /** Một dòng "Shop đang theo dõi". */
    public record FollowedShop(ShopMini shop, double ratingAvg, int followerCount) {
    }

    public record ListResult(CursorPage<FollowedShop> page, long total) {
    }

    /** {@code GET /me/following/shop-ids} — {@code shop_id} (= vendor_profiles.id) các shop còn theo dõi được, mới nhất trước. */
    @Transactional(readOnly = true)
    public List<Long> shopIds(long userId) {
        List<Long> vendorIds = follows.findVendorIdsByCustomerId(userId, PageRequest.of(0, MAX_IDS));
        Map<Long, FollowedShopView> visible = shops.findVisibleByVendorUserIds(vendorIds);
        return vendorIds.stream().map(visible::get).filter(v -> v != null).map(FollowedShopView::shopId).toList();
    }

    /**
     * {@code GET /me/following?cursor&limit} — mới theo dõi trước; thông tin shop nạp MỘT lần cho cả trang. Shop không còn hiển
     * thị được (bị khóa / chưa xác minh) bị bỏ qua (D4); {@code total} chỉ đếm shop còn hiển thị.
     */
    @Transactional(readOnly = true)
    public ListResult list(long userId, String cursor, Integer limit) {
        int size = limit == null ? DEFAULT_LIMIT : limit;
        if (size < 1 || size > MAX_LIMIT) {
            throw InvalidRequestException.of("limit", "OUT_OF_RANGE", limit, "limit phải từ 1 đến 50.");
        }
        PageRequest page = PageRequest.of(0, size + 1);
        List<ShopFollow> rows;
        if (cursor == null || cursor.isBlank()) {
            rows = follows.findPage(userId, page);
        } else {
            Cursor c = Cursor.decode(cursor.trim());
            rows = follows.findPageAfter(userId, c.createdAt(), c.id(), page);
        }
        boolean hasMore = rows.size() > size;
        List<ShopFollow> pageRows = hasMore ? rows.subList(0, size) : rows;
        String next = hasMore ? new Cursor(pageRows.get(size - 1).getCreatedAt(), pageRows.get(size - 1).getId()).encode()
                : null;

        Map<Long, FollowedShopView> visible = shops.findVisibleByVendorUserIds(
                pageRows.stream().map(ShopFollow::getVendorId).toList());
        List<FollowedShop> items = new ArrayList<>();
        for (ShopFollow f : pageRows) {
            FollowedShopView v = visible.get(f.getVendorId());
            if (v != null) {
                items.add(new FollowedShop(new ShopMini(v.shopId(), v.name(), v.logoUrl(), v.isMall()), v.ratingAvg(),
                        v.followerCount()));
            }
        }
        long total = shops.findVisibleByVendorUserIds(
                follows.findVendorIdsByCustomerId(userId, PageRequest.of(0, MAX_IDS))).size();
        return new ListResult(new CursorPage<>(items, new CursorPage.Pagination(size, hasMore, next)), total);
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
