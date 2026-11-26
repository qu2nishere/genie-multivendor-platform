package com.genie.order.cart.application;

import com.genie.catalog.product.application.VariantFacade;
import com.genie.catalog.product.application.VariantFacade.VariantLine;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.order.cart.application.PurchaseRules.Reason;
import com.genie.order.cart.domain.CartItem;
import com.genie.order.cart.domain.CartItemRepository;
import com.genie.order.cart.domain.CartRepository;
import com.genie.vendor.shop.application.ShopFacade;
import com.genie.vendor.shop.application.ShopFacade.ShopBuyInfo;
import com.genie.vendor.shop.application.ShopFacade.ShopMiniView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giỏ hàng của người mua đã đăng nhập — UC-ORD-01 (API_SPEC 6.1–6.5; plan C2 3.1, O02.02). Mọi thao tác chỉ chạm giỏ
 * của chính người gọi ({@code userId} từ token): id dòng của người khác coi như không tồn tại.
 */
@Service
public class CartService {

    /** Giới hạn giỏ (plan C2 D9). */
    public static final int MAX_LINES = 100;
    public static final int MAX_QTY = 999;

    private final CartRepository carts;
    private final CartItemRepository items;
    private final VariantFacade variants;
    private final ShopFacade shops;
    private final Clock clock;

    public CartService(CartRepository carts, CartItemRepository items, VariantFacade variants, ShopFacade shops,
                       Clock clock) {
        this.carts = carts;
        this.items = items;
        this.variants = variants;
        this.shops = shops;
        this.clock = clock;
    }

    /** Một nhóm shop của giỏ (6.1 {@code shops[]}). */
    public record ShopGroup(ShopMiniView shop, List<CartLineView> items) {
    }

    /** {@code GET /me/cart}. {@code itemCount} = số dòng (badge). */
    public record CartView(List<ShopGroup> shops, int itemCount) {
    }

    /** Dòng mini-cart (6.2). */
    public record MiniItem(long id, long productId, String productName, String imageUrl, String variantName,
                           long unitPrice) {
    }

    public record MiniCart(List<MiniItem> items, long total) {
    }

    /** {@code created} = dòng mới (201), ngược lại cộng dồn (200). */
    public record AddResult(CartLineView item, boolean created, long cartItemCount) {
    }

    public record UpdateResult(CartLineView item, Long mergedFromId) {
    }

    // ===================== đọc =====================

    @Transactional(readOnly = true)
    public CartView view(long userId) {
        Long cartId = carts.findIdByUserId(userId).orElse(null);
        if (cartId == null) {
            return new CartView(List.of(), 0);
        }
        List<CartItem> rows = items.findByCart(cartId);
        Map<Long, VariantLine> lines = variants.lines(rows.stream().map(CartItem::getProductVariantId).toList());
        Map<Long, ShopBuyInfo> shopInfo = shopsOf(lines.values());

        Map<Long, List<CartLineView>> byVendor = new LinkedHashMap<>();
        for (CartItem i : rows) {
            VariantLine line = lines.get(i.getProductVariantId());
            if (line == null) {
                continue;   // biến thể đã bị xóa cứng — không hiển thị được
            }
            byVendor.computeIfAbsent(line.vendorUserId(), k -> new ArrayList<>())
                    .add(toView(i, line, shopInfo.get(line.vendorUserId()), userId));
        }
        List<ShopGroup> groups = new ArrayList<>();
        byVendor.forEach((vendor, list) -> {
            ShopBuyInfo s = shopInfo.get(vendor);
            groups.add(new ShopGroup(s == null ? new ShopMiniView(0, "", null, false) : s.mini(), list));
        });
        return new CartView(groups, rows.size());
    }

    /** {@code GET /me/cart/items?limit=5&sort=-added_at} — mới thêm trước + tổng số dòng. */
    @Transactional(readOnly = true)
    public MiniCart latest(long userId, int limit) {
        if (limit < 1 || limit > 50) {
            throw InvalidRequestException.of("limit", "OUT_OF_RANGE", limit, "limit phải từ 1 đến 50.");
        }
        Long cartId = carts.findIdByUserId(userId).orElse(null);
        if (cartId == null) {
            return new MiniCart(List.of(), 0);
        }
        List<CartItem> rows = items.findLatest(cartId, PageRequest.of(0, limit));
        Map<Long, VariantLine> lines = variants.lines(rows.stream().map(CartItem::getProductVariantId).toList());
        List<MiniItem> out = rows.stream().filter(r -> lines.containsKey(r.getProductVariantId())).map(r -> {
            VariantLine l = lines.get(r.getProductVariantId());
            return new MiniItem(r.getId(), l.productId(), l.productName(), l.imageUrl(), l.variantName(), l.unitPrice());
        }).toList();
        return new MiniCart(out, items.countByCartId(cartId));
    }

    @Transactional(readOnly = true)
    public long count(long userId) {
        return carts.findIdByUserId(userId).map(items::countByCartId).orElse(0L);
    }

    // ===================== ghi =====================

    /** {@code POST /me/cart/items} — cùng biến thể cộng dồn; mọi vi phạm trả lỗi (khác batch / merge). */
    @Transactional
    public AddResult add(long userId, long variantId, int qty) {
        requireQty(qty);
        VariantLine line = variants.lines(List.of(variantId)).get(variantId);
        ShopBuyInfo shop = line == null ? null : shopsOf(List.of(line)).get(line.vendorUserId());
        Reason reason = PurchaseRules.reasonFor(line, shop, userId);
        if (reason != null && reason != Reason.OUT_OF_STOCK) {
            throw error(reason);
        }
        long cartId = cartIdFor(userId);
        CartItem existing = items.findByCartIdAndProductVariantId(cartId, variantId).orElse(null);
        int total = qty + (existing == null ? 0 : existing.getQuantity());
        if (total > line.available()) {
            throw stockExceeded(line.available());
        }
        if (total > MAX_QTY) {
            throw InvalidRequestException.of("quantity", "MAX_QUANTITY", total, "Mỗi sản phẩm tối đa 999 cái.");
        }
        if (existing == null && items.countByCartId(cartId) >= MAX_LINES) {
            throw InvalidRequestException.of("variant_id", "CART_FULL", variantId, "Giỏ hàng đã đủ 100 sản phẩm.");
        }
        CartItem saved;
        if (existing != null) {
            existing.setQuantity(total);
            saved = items.save(existing);
        } else {
            // dòng mới: upsert để hai yêu cầu đồng thời cùng SKU không đụng uk_cart_items_cart_variant
            long id = items.upsertAdd(cartId, variantId, qty, BigDecimal.valueOf(line.unitPrice()), now());
            saved = items.findById(id).orElseThrow();
        }
        carts.touch(cartId, now());
        return new AddResult(toView(saved, line, shop, userId), existing == null, items.countByCartId(cartId));
    }

    /**
     * {@code PATCH /me/cart/items/{id}} — đúng một trong {@code quantity} / {@code variantId}. Đổi phân loại sang biến thể
     * khác của CÙNG sản phẩm; biến thể đó đã có dòng → gộp vào dòng đó (giới hạn theo tồn / 999), xóa dòng cũ và trả
     * {@code merged_from_id} = id dòng cũ (6.4).
     */
    @Transactional
    public UpdateResult update(long userId, long itemId, Integer quantity, Long variantId) {
        if ((quantity == null) == (variantId == null)) {
            throw InvalidRequestException.of("quantity", "ONE_OF_REQUIRED", null,
                    "Gửi đúng một trong hai trường quantity hoặc variant_id.");
        }
        long cartId = carts.findIdByUserId(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        CartItem row = items.findByIdAndCartId(itemId, cartId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (quantity != null) {
            requireQty(quantity);
            VariantLine line = variants.lines(List.of(row.getProductVariantId())).get(row.getProductVariantId());
            ShopBuyInfo shop = line == null ? null : shopsOf(List.of(line)).get(line.vendorUserId());
            int available = line == null ? 0 : line.available();
            // Giảm số lượng luôn được (kể cả khi món đã hết hàng / ngừng bán); tăng thì không vượt tồn khả dụng
            if (quantity > row.getQuantity() && quantity > available) {
                throw stockExceeded(available);
            }
            row.setQuantity(quantity);
            items.save(row);
            carts.touch(cartId, now());
            return new UpdateResult(line == null ? null : toView(row, line, shop, userId), null);
        }

        if (variantId == row.getProductVariantId()) {
            VariantLine same = variants.lines(List.of(variantId)).get(variantId);
            return new UpdateResult(toView(row, same, shopsOf(List.of(same)).get(same.vendorUserId()), userId), null);
        }
        Map<Long, VariantLine> lines = variants.lines(List.of(row.getProductVariantId(), variantId));
        VariantLine current = lines.get(row.getProductVariantId());
        VariantLine target = lines.get(variantId);
        if (target == null || current == null || target.productId() != current.productId()) {
            throw InvalidRequestException.of("variant_id", "NOT_SAME_PRODUCT", variantId,
                    "Chỉ đổi được sang phân loại khác của cùng sản phẩm.");
        }
        ShopBuyInfo shop = shopsOf(List.of(target)).get(target.vendorUserId());
        Reason reason = PurchaseRules.reasonFor(target, shop, userId);
        if (reason != null && reason != Reason.OUT_OF_STOCK) {
            throw error(reason);
        }
        CartItem twin = items.findByCartIdAndProductVariantId(cartId, variantId).orElse(null);
        if (twin != null) {
            int merged = Math.min(Math.min(twin.getQuantity() + row.getQuantity(), MAX_QTY),
                    Math.max(target.available(), twin.getQuantity()));
            twin.setQuantity(merged);
            items.save(twin);
            items.delete(row);
            carts.touch(cartId, now());
            return new UpdateResult(toView(twin, target, shop, userId), row.getId());
        }
        if (row.getQuantity() > target.available()) {
            throw stockExceeded(target.available());
        }
        row.setProductVariantId(variantId);
        row.setPriceSnapshot(BigDecimal.valueOf(target.unitPrice()));
        items.save(row);
        carts.touch(cartId, now());
        return new UpdateResult(toView(row, target, shop, userId), null);
    }

    /** {@code DELETE /me/cart/items?ids=} — id không thuộc giỏ của người gọi bị bỏ qua. */
    @Transactional
    public void remove(long userId, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        carts.findIdByUserId(userId).ifPresent(cartId -> {
            if (items.deleteOwned(cartId, ids) > 0) {
                carts.touch(cartId, now());
            }
        });
    }

    // ===================== dùng chung cho batch / merge / checkout =====================

    /** Kết quả thêm "mềm" một món (6.6, 6.7): bỏ qua kèm lý do, hoặc thêm với số lượng cuối cùng trong giỏ. */
    record SoftAdd(Reason skipped, int finalQuantity, boolean adjusted) {
    }

    /**
     * Thêm không ném lỗi nghiệp vụ: không mua được → {@code skipped}; vượt tồn khả dụng / 999 → hạ số lượng
     * ({@code adjusted}). Dùng cho "Mua lại" và gộp giỏ khách.
     */
    SoftAdd softAdd(long cartId, long userId, long variantId, int qty, VariantLine line, ShopBuyInfo shop) {
        Reason reason = PurchaseRules.reasonFor(line, shop, userId);
        if (reason != null) {
            return new SoftAdd(reason, 0, false);
        }
        CartItem existing = items.findByCartIdAndProductVariantId(cartId, variantId).orElse(null);
        int had = existing == null ? 0 : existing.getQuantity();
        if (existing == null && items.countByCartId(cartId) >= MAX_LINES) {
            return new SoftAdd(Reason.CART_FULL, 0, false);
        }
        int wanted = had + Math.max(1, qty);
        int cap = Math.min(MAX_QTY, Math.max(line.available(), had));
        int target = Math.min(wanted, cap);
        if (target <= had) {
            // không thêm được gì nữa (đã chạm tồn); dòng cũ giữ nguyên
            return had > 0 ? new SoftAdd(null, had, true) : new SoftAdd(Reason.OUT_OF_STOCK, 0, false);
        }
        if (existing != null) {
            existing.setQuantity(target);
            items.save(existing);
        } else {
            items.upsertAdd(cartId, variantId, target, BigDecimal.valueOf(line.unitPrice()), now());
        }
        return new SoftAdd(null, target, target < wanted);
    }

    /** Id giỏ của người dùng — tạo nếu chưa có (an toàn khi gọi đồng thời). */
    long cartIdFor(long userId) {
        return carts.findIdByUserId(userId).orElseGet(() -> {
            carts.insertIfAbsent(userId, now());
            return carts.findIdByUserId(userId).orElseThrow();
        });
    }

    Map<Long, ShopBuyInfo> shopsOf(Collection<VariantLine> lines) {
        Set<Long> vendors = lines.stream().filter(Objects::nonNull).map(VariantLine::vendorUserId)
                .collect(Collectors.toSet());
        return shops.buyInfo(vendors, now());
    }

    void touch(long cartId) {
        carts.touch(cartId, now());
    }

    long countLines(long cartId) {
        return items.countByCartId(cartId);
    }

    CartLineView toView(CartItem i, VariantLine line, ShopBuyInfo shop, long userId) {
        Reason reason = PurchaseRules.reasonFor(line, shop, userId);
        Long priceAtAdd = i.getPriceSnapshot() == null ? null
                : i.getPriceSnapshot().setScale(0, RoundingMode.HALF_UP).longValueExact();
        return new CartLineView(i.getId(), line.productId(), line.productSlug(), line.variantId(), line.productName(),
                line.categoryName(), line.imageUrl(), line.variantName(), line.unitPrice(), line.originalPrice(),
                priceAtAdd, i.getQuantity(), line.available(), reason == null, PurchaseRules.publicReason(reason));
    }

    private static void requireQty(int qty) {
        if (qty < 1 || qty > MAX_QTY) {
            throw InvalidRequestException.of("quantity", "OUT_OF_RANGE", qty, "Số lượng phải từ 1 đến 999.");
        }
    }

    static BusinessException error(Reason r) {
        return switch (r) {
            case OWN_PRODUCT -> new BusinessException(ErrorCode.OWN_SHOP_PRODUCT);
            case SHOP_ON_VACATION -> new BusinessException(ErrorCode.SHOP_ON_VACATION);
            case OUT_OF_STOCK -> stockExceeded(0);
            default -> new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE);
        };
    }

    private static BusinessException stockExceeded(int available) {
        return new BusinessException(ErrorCode.CART_QUANTITY_EXCEEDS_STOCK,
                "Sản phẩm này chỉ còn " + available + " sản phẩm trong kho.", Map.of("stock", available));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
