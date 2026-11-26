package com.genie.order.cart.application;

import com.genie.catalog.product.application.VariantFacade;
import com.genie.catalog.product.application.VariantFacade.VariantLine;
import com.genie.common.error.InvalidRequestException;
import com.genie.order.cart.application.CartService.SoftAdd;
import com.genie.order.cart.application.PurchaseRules.Reason;
import com.genie.vendor.shop.application.ShopFacade.ShopBuyInfo;
import com.genie.vendor.shop.application.ShopFacade.ShopMiniView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thêm nhiều món không ném lỗi nghiệp vụ — "Mua lại" (API 6.6) và gộp giỏ khách vãng lai sau đăng nhập (6.7; plan C2 D8,
 * O02.03); cùng với tra dòng hiển thị cho giỏ khách ({@code GET /public/variants}, API 5.18).
 */
@Service
public class CartMergeService {

    /** Tối đa số món một lần gửi (6.6, 6.7) và số id của {@code /public/variants}. */
    public static final int MAX_ITEMS = 50;

    private final CartService cart;
    private final VariantFacade variants;

    public CartMergeService(CartService cart, VariantFacade variants) {
        this.cart = cart;
        this.variants = variants;
    }

    public record Item(long variantId, int quantity) {
    }

    public record Skipped(long variantId, String reason) {
    }

    public record Adjusted(long variantId, int quantity) {
    }

    public record BatchResult(int added, List<Skipped> skipped, long cartItemCount) {
    }

    public record MergeResult(int merged, List<Skipped> skipped, List<Adjusted> adjusted, long cartItemCount) {
    }

    /** Dòng hiển thị của giỏ khách (API 5.18 {@code GET /public/variants}). */
    public record PublicVariant(long variantId, long productId, String productSlug, String productName,
                                String categoryName, String variantName, String imageUrl, long unitPrice,
                                Long originalPrice, int stock, boolean isAvailable, String unavailableReason,
                                ShopMiniView shop) {
    }

    @Transactional
    public BatchResult batch(long userId, List<Item> input) {
        Outcome o = apply(userId, input);
        return new BatchResult(o.added, o.skipped, o.count);
    }

    @Transactional
    public MergeResult merge(long userId, List<Item> input) {
        Outcome o = apply(userId, input);
        return new MergeResult(o.added, o.skipped, o.adjusted, o.count);
    }

    /** Khách chưa đăng nhập không có "chủ shop" để so — chỉ xét sản phẩm, shop nghỉ, tồn. */
    @Transactional(readOnly = true)
    public List<PublicVariant> publicVariants(List<Long> ids) {
        if (ids.size() > MAX_ITEMS) {
            throw InvalidRequestException.of("ids", "TOO_MANY", ids.size(), "Tối đa 50 id mỗi lần.");
        }
        Map<Long, VariantLine> lines = variants.lines(ids);
        Map<Long, ShopBuyInfo> shops = cart.shopsOf(lines.values());
        List<PublicVariant> out = new ArrayList<>();
        for (VariantLine l : lines.values()) {
            ShopBuyInfo s = shops.get(l.vendorUserId());
            Reason r = PurchaseRules.reasonFor(l, s, -1L);
            out.add(new PublicVariant(l.variantId(), l.productId(), l.productSlug(), l.productName(), l.categoryName(),
                    l.variantName(), l.imageUrl(), l.unitPrice(), l.originalPrice(), l.available(), r == null,
                    PurchaseRules.publicReason(r), s == null ? null : s.mini()));
        }
        return out;
    }

    private record Outcome(int added, List<Skipped> skipped, List<Adjusted> adjusted, long count) {
    }

    private Outcome apply(long userId, List<Item> input) {
        if (input == null || input.isEmpty()) {
            throw InvalidRequestException.of("items", "REQUIRED", null, "Cần ít nhất một sản phẩm.");
        }
        if (input.size() > MAX_ITEMS) {
            throw InvalidRequestException.of("items", "TOO_MANY", input.size(), "Tối đa 50 sản phẩm mỗi lần.");
        }
        // Gộp trùng biến thể trong cùng yêu cầu (giỏ khách có thể lưu một SKU hai lần)
        Map<Long, Integer> wanted = new LinkedHashMap<>();
        for (Item i : input) {
            wanted.merge(i.variantId(), Math.max(1, i.quantity()), Integer::sum);
        }
        Map<Long, VariantLine> lines = variants.lines(wanted.keySet());
        Map<Long, ShopBuyInfo> shops = cart.shopsOf(lines.values());
        long cartId = cart.cartIdFor(userId);

        int added = 0;
        List<Skipped> skipped = new ArrayList<>();
        List<Adjusted> adjusted = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
            VariantLine line = lines.get(e.getKey());
            SoftAdd r = cart.softAdd(cartId, userId, e.getKey(), e.getValue(), line,
                    line == null ? null : shops.get(line.vendorUserId()));
            if (r.skipped() != null) {
                skipped.add(new Skipped(e.getKey(), r.skipped().name()));
                continue;
            }
            added++;
            if (r.adjusted()) {
                adjusted.add(new Adjusted(e.getKey(), r.finalQuantity()));
            }
        }
        if (added > 0) {
            cart.touch(cartId);
        }
        return new Outcome(added, skipped, adjusted, cart.countLines(cartId));
    }
}
